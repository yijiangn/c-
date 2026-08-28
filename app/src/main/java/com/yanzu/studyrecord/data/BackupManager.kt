package com.yanzu.studyrecord.data

import androidx.room.withTransaction
import com.yanzu.studyrecord.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate

enum class RestoreMode { REPLACE, MERGE }

class BackupManager(private val db: AppDatabase) {
    suspend fun exportJson(output: OutputStream) {
        val root = JSONObject().apply {
            put("app", "学习记录")
            put("schemaVersion", 1)
            put("exportedAt", Instant.now().toString())
            put("subjects", JSONArray(db.subjectDao().getAll().map { it.toJson() }))
            put("tasks", JSONArray(db.taskDao().getAll().map { it.toJson() }))
            put("sessions", JSONArray(db.sessionDao().getAll().map { it.toJson() }))
            put("settings", (db.settingsDao().get() ?: UserSettingsEntity()).toJson())
        }
        output.bufferedWriter(Charsets.UTF_8).use { it.write(root.toString(2)) }
    }

    suspend fun exportCsv(output: OutputStream) {
        val subjects = db.subjectDao().getAll().associateBy { it.id }
        val tasks = db.taskDao().getAll().associateBy { it.id }
        val rows = buildString {
            append('\uFEFF')
            appendLine("日期,任务,科目,学习时长（分钟）,记录方式,计时模式,开始时间,结束时间")
            db.sessionDao().getAll().forEach { s ->
                val task = s.taskId?.let(tasks::get)?.name.orEmpty()
                val subject = s.subjectId?.let(subjects::get)?.name ?: "其他"
                appendLine(listOf(DateUtils.csvDate(s.dateEpochDay), task, subject, s.durationMinutes.toString(), s.source, s.mode, DateUtils.time(s.startEpochMillis), DateUtils.time(s.endEpochMillis)).joinToString(",") { csv(it) })
            }
        }
        output.write(rows.toByteArray(Charsets.UTF_8))
        output.close()
    }

    suspend fun restoreJson(input: InputStream, mode: RestoreMode) {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("文件不是有效的 JSON 备份") }
        require(root.optString("app") == "学习记录" && root.optInt("schemaVersion") == 1) { "备份文件格式或版本不受支持" }
        require(root.has("subjects") && root.has("tasks") && root.has("sessions") && root.has("settings")) { "备份文件内容不完整" }
        val subjects = root.getJSONArray("subjects").objects().map { it.toSubject() }
        val tasks = root.getJSONArray("tasks").objects().map { it.toTask() }
        val sessions = root.getJSONArray("sessions").objects().map { it.toSession() }
        val settings = root.getJSONObject("settings").toSettings()
        require(subjects.map { it.id }.toSet().containsAll(tasks.mapNotNull { it.subjectId })) { "备份中的任务引用了不存在的科目" }
        require(tasks.map { it.id }.toSet().containsAll(sessions.mapNotNull { it.taskId })) { "备份中的计时记录引用了不存在的任务" }
        require(tasks.all { it.name.isNotBlank() && it.plannedMinutes >= 0 && it.actualMinutes >= 0 && runCatching { LocalDate.ofEpochDay(it.dateEpochDay) }.isSuccess }) { "备份中包含无效任务" }
        require(tasks.all { runCatching { TaskPriority.valueOf(it.priority); TaskStatus.valueOf(it.status) }.isSuccess }) { "备份中的任务状态无效" }
        require(sessions.all { it.durationMinutes > 0 }) { "备份中包含无效学习时长" }
        require(sessions.all { runCatching { LocalDate.ofEpochDay(it.dateEpochDay); TimerMode.valueOf(it.mode) }.isSuccess }) { "备份中的计时记录无效" }
        require(settings.dailyGoalMinutes > 0 && settings.weeklyGoalMinutes > 0 && settings.pomodoroMinutes in 1..180 && runCatching { ThemeMode.valueOf(settings.themeMode) }.isSuccess) { "备份中的设置无效" }

        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                db.sessionDao().clear(); db.taskDao().clear(); db.subjectDao().clear(); db.settingsDao().clear()
            }
            db.subjectDao().upsertAll(subjects)
            db.taskDao().upsertAll(tasks)
            db.sessionDao().upsertAll(sessions)
            db.settingsDao().upsert(settings.copy(timerState = TimerState.IDLE.name, timerStartedAtEpochMillis = 0, timerBaseElapsedSeconds = 0, timerTargetSeconds = 0, timerTaskId = null, timerSubjectId = null))
            BuiltInSubjects.all().forEach { builtIn -> if (subjects.none { it.id == builtIn.id }) db.subjectDao().upsert(builtIn) }
        }
    }

    suspend fun clearAll() = db.withTransaction {
        db.sessionDao().clear(); db.taskDao().clear(); db.subjectDao().clear(); db.settingsDao().clear()
        db.subjectDao().upsertAll(BuiltInSubjects.all())
        db.settingsDao().upsert(UserSettingsEntity())
    }

    private fun SubjectEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("colorArgb", colorArgb); put("isBuiltIn", isBuiltIn); put("sortOrder", sortOrder) }
    private fun StudyTaskEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); putNullable("subjectId", subjectId); put("plannedMinutes", plannedMinutes); put("actualMinutes", actualMinutes); put("dateEpochDay", dateEpochDay); put("priority", priority); put("status", status); put("note", note); put("createdAtEpochMillis", createdAtEpochMillis); put("updatedAtEpochMillis", updatedAtEpochMillis) }
    private fun StudySessionEntity.toJson() = JSONObject().apply { put("id", id); putNullable("taskId", taskId); putNullable("subjectId", subjectId); put("startEpochMillis", startEpochMillis); put("endEpochMillis", endEpochMillis); put("durationMinutes", durationMinutes); put("dateEpochDay", dateEpochDay); put("mode", mode); put("source", source) }
    private fun UserSettingsEntity.toJson() = JSONObject().apply {
        put("id", 1); put("dailyGoalMinutes", dailyGoalMinutes); put("weeklyGoalMinutes", weeklyGoalMinutes)
        put("pomodoroMinutes", pomodoroMinutes); put("themeMode", themeMode)
        put("timerState", timerState); put("timerMode", timerMode)
        put("timerStartedAtEpochMillis", timerStartedAtEpochMillis); put("timerBaseElapsedSeconds", timerBaseElapsedSeconds)
        put("timerTargetSeconds", timerTargetSeconds); putNullable("timerTaskId", timerTaskId); putNullable("timerSubjectId", timerSubjectId)
    }
    private fun JSONObject.toSubject() = SubjectEntity(getString("id"), getString("name"), getLong("colorArgb"), getBoolean("isBuiltIn"), getInt("sortOrder"))
    private fun JSONObject.toTask() = StudyTaskEntity(
        id = getString("id"),
        name = getString("name"),
        subjectId = nullableString("subjectId"),
        plannedMinutes = getInt("plannedMinutes"),
        actualMinutes = getInt("actualMinutes"),
        dateEpochDay = getLong("dateEpochDay"),
        priority = getString("priority"),
        status = getString("status"),
        note = optString("note"),
        createdAtEpochMillis = getLong("createdAtEpochMillis"),
        updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
    )
    private fun JSONObject.toSession() = StudySessionEntity(getString("id"), nullableString("taskId"), nullableString("subjectId"), getLong("startEpochMillis"), getLong("endEpochMillis"), getInt("durationMinutes"), getLong("dateEpochDay"), getString("mode"), getString("source"))
    private fun JSONObject.toSettings() = UserSettingsEntity(
        dailyGoalMinutes = getInt("dailyGoalMinutes"), weeklyGoalMinutes = getInt("weeklyGoalMinutes"),
        pomodoroMinutes = getInt("pomodoroMinutes"), themeMode = getString("themeMode"),
        timerState = optString("timerState", TimerState.IDLE.name), timerMode = optString("timerMode", TimerMode.STOPWATCH.name),
        timerStartedAtEpochMillis = optLong("timerStartedAtEpochMillis", 0), timerBaseElapsedSeconds = optLong("timerBaseElapsedSeconds", 0),
        timerTargetSeconds = optLong("timerTargetSeconds", 0), timerTaskId = if (has("timerTaskId")) nullableString("timerTaskId") else null,
        timerSubjectId = if (has("timerSubjectId")) nullableString("timerSubjectId") else null,
    )
    private fun JSONObject.putNullable(key: String, value: String?) { put(key, value ?: JSONObject.NULL) }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
