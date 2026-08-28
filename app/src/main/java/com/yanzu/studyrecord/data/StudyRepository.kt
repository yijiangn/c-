package com.yanzu.studyrecord.data

import androidx.room.withTransaction
import com.yanzu.studyrecord.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class StudyRepository(private val db: AppDatabase) {
    val subjects: Flow<List<SubjectEntity>> = db.subjectDao().observeAll()
    val tasks: Flow<List<StudyTaskEntity>> = db.taskDao().observeAll()
    val sessions: Flow<List<StudySessionEntity>> = db.sessionDao().observeAll()
    val settings: Flow<UserSettingsEntity> = db.settingsDao().observe().filterNotNull()
    val taskItems: Flow<List<TaskItem>> = combine(tasks, subjects) { tasks, subjects ->
        val map = subjects.associateBy { it.id }
        tasks.map { TaskItem(it, map[it.subjectId]) }
    }

    suspend fun ensureSeeded() {
        if (db.subjectDao().getAll().isEmpty()) db.subjectDao().upsertAll(BuiltInSubjects.all())
        if (db.settingsDao().get() == null) db.settingsDao().upsert(UserSettingsEntity())
    }

    suspend fun saveTask(task: StudyTaskEntity) = db.withTransaction {
        require(task.name.isNotBlank()) { "任务名称不能为空" }
        require(task.plannedMinutes >= 0 && task.actualMinutes >= 0) { "学习时长不能为负数" }
        val desiredActual = task.actualMinutes
        val timed = db.sessionDao().timedMinutesForTask(task.id)
        val finalActual = maxOf(desiredActual, timed)
        val normalized = task.copy(
            actualMinutes = finalActual,
            status = if (finalActual > 0 && task.status == TaskStatus.NOT_STARTED.name) TaskStatus.IN_PROGRESS.name else task.status,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        db.taskDao().upsert(normalized)
        db.sessionDao().deleteManualForTask(task.id)
        val manual = normalized.actualMinutes - timed
        if (manual > 0) {
            val start = DateUtils.epochDayAtNoonMillis(task.dateEpochDay)
            db.sessionDao().insert(
                StudySessionEntity(
                    id = "manual_${task.id}", taskId = task.id, subjectId = task.subjectId,
                    startEpochMillis = start, endEpochMillis = start + manual * 60_000L,
                    durationMinutes = manual, dateEpochDay = task.dateEpochDay,
                    mode = TimerMode.STOPWATCH.name, source = "MANUAL",
                )
            )
        }
    }

    suspend fun deleteTask(id: String) = db.taskDao().delete(id)

    suspend fun toggleTask(task: StudyTaskEntity) {
        val next = if (task.status == TaskStatus.COMPLETED.name) TaskStatus.NOT_STARTED else TaskStatus.COMPLETED
        db.taskDao().upsert(task.copy(status = next.name, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun saveSubject(subject: SubjectEntity) {
        require(subject.name.isNotBlank()) { "科目名称不能为空" }
        db.subjectDao().upsert(subject.copy(name = subject.name.trim()))
    }

    suspend fun subjectTaskCount(id: String) = db.taskDao().countBySubject(id)

    suspend fun deleteSubject(id: String, moveTasksToOther: Boolean) = db.withTransaction {
        val count = db.taskDao().countBySubject(id)
        if (count > 0 && !moveTasksToOther) error("该科目下仍有任务")
        if (count > 0) db.taskDao().moveToOther(id, BuiltInSubjects.OTHER)
        db.subjectDao().deleteCustom(id)
    }

    suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = db.settingsDao().get() ?: UserSettingsEntity()
        db.settingsDao().upsert(transform(current))
    }

    suspend fun startTimer(mode: TimerMode, taskId: String?, subjectId: String?) = db.withTransaction {
        val now = System.currentTimeMillis()
        val current = db.settingsDao().get() ?: UserSettingsEntity()
        require(current.timerState == TimerState.IDLE.name) { "已有正在进行的计时" }
        db.settingsDao().upsert(current.copy(
            timerState = TimerState.RUNNING.name,
            timerMode = mode.name,
            timerStartedAtEpochMillis = now,
            timerBaseElapsedSeconds = 0,
            timerTargetSeconds = if (mode == TimerMode.POMODORO) current.pomodoroMinutes * 60L else 0,
            timerTaskId = taskId,
            timerSubjectId = subjectId,
        ))
        taskId?.let { db.taskDao().markInProgress(it, now) }
    }

    suspend fun pauseTimer() {
        val current = db.settingsDao().get() ?: return
        if (current.timerState != TimerState.RUNNING.name) return
        val elapsed = timerElapsedSeconds(current)
        db.settingsDao().upsert(current.copy(
            timerState = TimerState.PAUSED.name,
            timerBaseElapsedSeconds = elapsed,
            timerStartedAtEpochMillis = 0,
        ))
    }

    suspend fun resumeTimer() {
        val current = db.settingsDao().get() ?: return
        if (current.timerState != TimerState.PAUSED.name) return
        db.settingsDao().upsert(current.copy(
            timerState = TimerState.RUNNING.name,
            timerStartedAtEpochMillis = System.currentTimeMillis(),
        ))
    }

    suspend fun finishTimer(save: Boolean): Int = db.withTransaction {
        val current = db.settingsDao().get() ?: return@withTransaction 0
        val seconds = timerElapsedSeconds(current).let {
            if (current.timerMode == TimerMode.POMODORO.name && current.timerTargetSeconds > 0) minOf(it, current.timerTargetSeconds) else it
        }
        val minutes = (seconds / 60).toInt()
        if (save && minutes > 0) {
            val end = System.currentTimeMillis()
            val task = current.timerTaskId?.let { db.taskDao().getById(it) }
            val subjectId = task?.subjectId
                ?: current.timerSubjectId?.takeIf { db.subjectDao().getById(it) != null }
                ?: BuiltInSubjects.OTHER
            val start = end - seconds * 1000
            val date = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            db.sessionDao().insert(
                StudySessionEntity(
                    id = UUID.randomUUID().toString(), taskId = task?.id, subjectId = subjectId,
                    startEpochMillis = start, endEpochMillis = end, durationMinutes = minutes,
                    dateEpochDay = date, mode = current.timerMode, source = "TIMER",
                )
            )
            task?.let { db.taskDao().addActualMinutes(it.id, minutes, end) }
        }
        db.settingsDao().upsert(current.copy(
            timerState = TimerState.IDLE.name, timerStartedAtEpochMillis = 0,
            timerBaseElapsedSeconds = 0, timerTargetSeconds = 0,
            timerTaskId = null, timerSubjectId = null,
        ))
        minutes
    }

    fun timerElapsedSeconds(settings: UserSettingsEntity, now: Long = System.currentTimeMillis()): Long {
        val runningPart = if (settings.timerState == TimerState.RUNNING.name && settings.timerStartedAtEpochMillis > 0)
            ((now - settings.timerStartedAtEpochMillis).coerceAtLeast(0)) / 1000 else 0
        return settings.timerBaseElapsedSeconds + runningPart
    }

    suspend fun addManualSession(taskId: String?, subjectId: String?, dateEpochDay: Long, minutes: Int) = db.withTransaction {
        require(minutes > 0) { "学习时长必须大于 0 分钟" }
        val now = System.currentTimeMillis()
        val task = taskId?.let { db.taskDao().getById(it) }
        db.sessionDao().insert(
            StudySessionEntity(
                id = UUID.randomUUID().toString(), taskId = task?.id, subjectId = subjectId ?: task?.subjectId,
                startEpochMillis = DateUtils.epochDayAtNoonMillis(dateEpochDay),
                endEpochMillis = DateUtils.epochDayAtNoonMillis(dateEpochDay) + minutes * 60_000L,
                durationMinutes = minutes, dateEpochDay = dateEpochDay,
                mode = TimerMode.STOPWATCH.name, source = "MANUAL",
            )
        )
        task?.let { db.taskDao().addActualMinutes(it.id, minutes, now) }
    }

    suspend fun updateSession(session: StudySessionEntity) = db.withTransaction {
        require(session.durationMinutes > 0) { "学习时长必须大于 0 分钟" }
        val old = db.sessionDao().getById(session.id) ?: error("学习记录不存在")
        if (old.taskId != null) db.taskDao().addActualMinutes(old.taskId, -old.durationMinutes, System.currentTimeMillis())
        db.sessionDao().insert(session.copy(endEpochMillis = session.startEpochMillis + session.durationMinutes * 60_000L))
        if (session.taskId != null) db.taskDao().addActualMinutes(session.taskId, session.durationMinutes, System.currentTimeMillis())
    }

    suspend fun deleteSession(id: String) = db.withTransaction {
        val old = db.sessionDao().getById(id) ?: return@withTransaction
        db.sessionDao().delete(id)
        if (old.taskId != null) db.taskDao().addActualMinutes(old.taskId, -old.durationMinutes, System.currentTimeMillis())
    }

    fun database() = db
}
