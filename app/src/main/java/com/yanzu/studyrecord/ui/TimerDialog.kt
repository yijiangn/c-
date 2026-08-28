package com.yanzu.studyrecord.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanzu.studyrecord.AppViewModel
import com.yanzu.studyrecord.data.TaskItem
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.TimerMode
import com.yanzu.studyrecord.data.TimerState
import com.yanzu.studyrecord.data.UserSettingsEntity
import com.yanzu.studyrecord.util.DateUtils
import kotlinx.coroutines.delay

@Composable
fun StudyTimerDialog(
    settings: UserSettingsEntity,
    tasks: List<TaskItem>,
    subjects: List<SubjectEntity>,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onStartService: () -> Unit,
) {
    val active = settings.timerState != TimerState.IDLE.name
    var mode by remember { mutableStateOf(TimerMode.STOPWATCH) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active, settings.timerState) {
        while (active) { now = System.currentTimeMillis(); delay(1000) }
    }
    val repository = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.yanzu.studyrecord.StudyRecordApplication).repository
    val elapsed = repository.timerElapsedSeconds(settings, now)
    val shownSeconds = if (settings.timerMode == TimerMode.POMODORO.name && settings.timerTargetSeconds > 0)
        (settings.timerTargetSeconds - elapsed).coerceAtLeast(0) else elapsed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (active) "学习计时" else "开始计时") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (active) {
                    Text(DateUtils.formatTimer(shownSeconds), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (settings.timerMode == TimerMode.POMODORO.name) "番茄钟" else "正计时", color = MaterialTheme.colorScheme.primary)
                    tasks.firstOrNull { it.task.id == settings.timerTaskId }?.let { Text(it.task.name, modifier = Modifier.padding(top = 6.dp)) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == TimerMode.STOPWATCH, onClick = { mode = TimerMode.STOPWATCH }, label = { Text("正计时") })
                        FilterChip(selected = mode == TimerMode.POMODORO, onClick = { mode = TimerMode.POMODORO }, label = { Text("${settings.pomodoroMinutes}分钟番茄钟") })
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(tasks.firstOrNull { it.task.id == selectedTaskId }?.task?.name ?: "选择对应任务（可选）")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("不关联任务") }, onClick = { selectedTaskId = null; menuOpen = false })
                        tasks.filter { it.task.status != "COMPLETED" }.forEach { item ->
                            DropdownMenuItem(text = { Text(item.task.name) }, onClick = { selectedTaskId = item.task.id; selectedSubjectId = item.task.subjectId; menuOpen = false })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { subjectMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "选择科目（可选）")
                    }
                    DropdownMenu(expanded = subjectMenuOpen, onDismissRequest = { subjectMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("不关联科目") }, onClick = { selectedSubjectId = null; subjectMenuOpen = false })
                        subjects.forEach { subject -> DropdownMenuItem(text = { Text(subject.name) }, onClick = { selectedSubjectId = subject.id; subjectMenuOpen = false }) }
                    }
                }
            }
        },
        confirmButton = {
            if (!active) Button(onClick = {
                val subjectId = tasks.firstOrNull { it.task.id == selectedTaskId }?.task?.subjectId ?: selectedSubjectId
                viewModel.startTimer(mode, selectedTaskId, subjectId) { onStartService() }
            }) { androidx.compose.material3.Icon(Icons.Default.PlayArrow, null); Text("开始") }
            else Button(onClick = { viewModel.finishTimer(true) { onDismiss() } }) { androidx.compose.material3.Icon(Icons.Default.Stop, null); Text("结束并保存") }
        },
        dismissButton = {
            Row {
                if (active) {
                    OutlinedButton(onClick = { if (settings.timerState == TimerState.RUNNING.name) viewModel.pauseTimer() else viewModel.resumeTimer() }) {
                        androidx.compose.material3.Icon(if (settings.timerState == TimerState.RUNNING.name) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Text(if (settings.timerState == TimerState.RUNNING.name) "暂停" else "继续")
                    }
                } else OutlinedButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
