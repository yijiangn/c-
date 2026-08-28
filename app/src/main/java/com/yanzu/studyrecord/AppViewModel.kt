package com.yanzu.studyrecord

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanzu.studyrecord.data.RestoreMode
import com.yanzu.studyrecord.data.StudySessionEntity
import com.yanzu.studyrecord.data.StudyTaskEntity
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.TaskItem
import com.yanzu.studyrecord.data.TimerMode
import com.yanzu.studyrecord.data.UserSettingsEntity
import com.yanzu.studyrecord.util.StatsCalculator
import com.yanzu.studyrecord.util.StudyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val tasks: List<TaskItem> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val stats: StudyStats = StudyStats(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudyRecordApplication
    private val repository = app.repository
    private val eventsChannel = Channel<String>(Channel.BUFFERED)
    val messages = eventsChannel.receiveAsFlow()

    val uiState = combine(repository.taskItems, repository.subjects, repository.sessions, repository.settings) { items, subjects, sessions, settings ->
        AppUiState(items, subjects, sessions, settings, StatsCalculator.calculate(sessions, items.map { it.task }, subjects))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private fun runAction(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onSuccess { success?.let { eventsChannel.send(it) } }
                .onFailure { eventsChannel.send(it.message ?: "操作失败，请稍后重试") }
        }
    }

    fun saveTask(task: StudyTaskEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.saveTask(task) } }
                .onSuccess { eventsChannel.send("任务已保存"); onDone?.invoke() }
                .onFailure { eventsChannel.send(it.message ?: "任务保存失败") }
        }
    }

    fun deleteTask(id: String) = runAction("任务已删除") { repository.deleteTask(id) }
    fun toggleTask(task: StudyTaskEntity) = runAction { repository.toggleTask(task) }
    fun saveSubject(subject: SubjectEntity) = runAction("科目已保存") { repository.saveSubject(subject) }
    fun deleteSubject(id: String, move: Boolean) = runAction("科目已删除") { repository.deleteSubject(id, move) }
    fun checkSubjectTaskCount(id: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { repository.subjectTaskCount(id) }
            callback(count)
        }
    }

    fun updateGoals(daily: Int? = null, weekly: Int? = null) = runAction("目标已更新") {
        require((daily ?: 1) > 0 && (weekly ?: 1) > 0) { "目标时长必须大于 0" }
        repository.updateSettings { it.copy(dailyGoalMinutes = daily ?: it.dailyGoalMinutes, weeklyGoalMinutes = weekly ?: it.weeklyGoalMinutes) }
    }

    fun updatePomodoro(minutes: Int) = runAction("番茄钟时长已更新") {
        require(minutes in 1..180) { "番茄钟时长应为 1～180 分钟" }
        repository.updateSettings { it.copy(pomodoroMinutes = minutes) }
    }

    fun updateTheme(mode: String) = runAction { repository.updateSettings { it.copy(themeMode = mode) } }
    fun startTimer(mode: TimerMode, taskId: String?, subjectId: String?, onStarted: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.startTimer(mode, taskId, subjectId) } }
                .onSuccess { onStarted?.invoke() }
                .onFailure { eventsChannel.send(it.message ?: "计时启动失败") }
        }
    }
    fun pauseTimer() = runAction { repository.pauseTimer() }
    fun resumeTimer() = runAction { repository.resumeTimer() }
    fun finishTimer(save: Boolean, onDone: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.finishTimer(save) } }
                .onSuccess { minutes ->
                    eventsChannel.send(if (save && minutes > 0) "已保存 ${minutes} 分钟学习记录" else if (save) "不足 1 分钟，未保存记录" else "计时已取消")
                    onDone?.invoke(minutes)
                }.onFailure { eventsChannel.send(it.message ?: "结束计时失败") }
        }
    }

    fun addManualSession(taskId: String?, subjectId: String?, date: Long, minutes: Int) = runAction("学习记录已补记") {
        repository.addManualSession(taskId, subjectId, date, minutes)
    }
    fun updateSession(session: StudySessionEntity) = runAction("学习记录已修改") { repository.updateSession(session) }
    fun deleteSession(id: String) = runAction("学习记录已删除") { repository.deleteSession(id) }

    fun exportJson(uri: Uri) = runAction("JSON 备份已导出") {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.let { app.backupManager.exportJson(it) }
            ?: error("无法写入所选文件")
    }

    fun exportCsv(uri: Uri) = runAction("CSV 学习记录已导出") {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.let { app.backupManager.exportCsv(it) }
            ?: error("无法写入所选文件")
    }

    fun restoreJson(uri: Uri, mode: RestoreMode) = runAction("备份恢复完成") {
        getApplication<Application>().contentResolver.openInputStream(uri)?.let { app.backupManager.restoreJson(it, mode) }
            ?: error("无法读取所选文件")
    }

    fun clearAll() = runAction("全部数据已清除") { app.backupManager.clearAll() }
}
