package com.yanzu.studyrecord.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanzu.studyrecord.AppUiState
import com.yanzu.studyrecord.AppViewModel
import com.yanzu.studyrecord.data.StudyTaskEntity
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.TaskItem
import com.yanzu.studyrecord.data.TaskPriority
import com.yanzu.studyrecord.data.TaskStatus
import com.yanzu.studyrecord.data.TimerState
import com.yanzu.studyrecord.ui.theme.SoftRed
import com.yanzu.studyrecord.ui.theme.TealPrimary
import com.yanzu.studyrecord.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private enum class TaskFilter(val label: String) { ALL("全部"), UNDONE("未完成"), DONE("已完成") }

@Composable
fun TodayScreen(state: AppUiState, viewModel: AppViewModel, outerPadding: PaddingValues, onStartTimer: () -> Unit) {
    val today = DateUtils.todayEpochDay()
    var editing by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var deleting by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var subjectFilter by remember { mutableStateOf<String?>(null) }
    var subjectMenu by remember { mutableStateOf(false) }
    val todayTasks = state.tasks.filter { it.task.dateEpochDay == today }
        .filter { subjectFilter == null || it.task.subjectId == subjectFilter }
        .filter { when (filter) { TaskFilter.ALL -> true; TaskFilter.DONE -> it.task.status == TaskStatus.COMPLETED.name; TaskFilter.UNDONE -> it.task.status != TaskStatus.COMPLETED.name } }
    val dailyGoal = state.settings.dailyGoalMinutes.coerceAtLeast(1)
    val progress = (state.stats.todayMinutes.toFloat() / dailyGoal).coerceIn(0f, 1f)

    Scaffold(
        modifier = Modifier.padding(outerPadding),
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "添加任务") } },
        bottomBar = {
            Button(onClick = onStartTimer, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(if (state.settings.timerState == TimerState.IDLE.name) Icons.Default.PlayArrow else Icons.Default.Timer, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.settings.timerState == TimerState.IDLE.name) "开始计时" else "查看正在进行的计时", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("今日", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(DateUtils.fullDate(today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = TealPrimary), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("◎  今日目标", color = Color.White.copy(alpha = .88f))
                            Spacer(Modifier.height(18.dp))
                            Text("${state.stats.todayMinutes} / $dailyGoal 分钟", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(RoundedCornerShape(8.dp)), color = Color.White, trackColor = Color.White.copy(alpha = .25f))
                            Text("${(progress * 100).toInt()}%", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, tint = TealPrimary); Spacer(Modifier.width(6.dp)); Text("今日学习时长", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(22.dp))
                            Text(DateUtils.formatMinutes(state.stats.todayMinutes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("完成 ${state.stats.todayCompleted} 项任务", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            item {
                val weeklyGoal = state.settings.weeklyGoalMinutes.coerceAtLeast(1)
                val weeklyProgress = (state.stats.weekMinutes.toFloat() / weeklyGoal).coerceIn(0f, 1f)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row { Text("本周目标", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("${state.stats.weekMinutes} / $weeklyGoal 分钟  ${(weeklyProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary) }
                        LinearProgressIndicator(progress = { weeklyProgress }, modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(7.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("今日学习任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${todayTasks.size} 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TaskFilter.entries.forEach { f -> AssistChip(onClick = { filter = f }, label = { Text(f.label) }, leadingIcon = if (filter == f) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null) }
                    Box {
                        AssistChip(onClick = { subjectMenu = true }, label = { Text(state.subjects.firstOrNull { it.id == subjectFilter }?.name ?: "科目") })
                        DropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                            DropdownMenuItem(text = { Text("全部科目") }, onClick = { subjectFilter = null; subjectMenu = false })
                            state.subjects.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { subjectFilter = s.id; subjectMenu = false }) }
                        }
                    }
                }
            }
            if (todayTasks.isEmpty()) item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("今天还没有符合条件的任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { adding = true }) { Text("添加第一个任务") }
                    }
                }
            }
            items(todayTasks, key = { it.task.id }) { item ->
                TaskCard(item, onToggle = { viewModel.toggleTask(item.task) }, onLongPress = { longPressed = item.task })
            }
            item { Spacer(Modifier.height(82.dp)) }
        }
    }

    if (adding || editing != null) TaskEditorDialog(editing, state.subjects, today, onDismiss = { adding = false; editing = null }, onSave = { task ->
        viewModel.saveTask(task) { adding = false; editing = null }
    })
    longPressed?.let { task ->
        AlertDialog(onDismissRequest = { longPressed = null }, title = { Text(task.name) }, text = { Text("请选择要执行的操作") }, confirmButton = {
            TextButton(onClick = { editing = task; longPressed = null }) { Icon(Icons.Default.Edit, null); Text("编辑") }
        }, dismissButton = { TextButton(onClick = { deleting = task; longPressed = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } })
    }
    deleting?.let { task ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除任务？") }, text = { Text("“${task.name}”将被删除，但已经产生的历史学习时长会保留。") }, confirmButton = {
            TextButton(onClick = { viewModel.deleteTask(task.id); deleting = null }) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
        }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(item: TaskItem, onToggle: () -> Unit, onLongPress: () -> Unit) {
    val task = item.task
    val priorityColor = when (task.priority) { TaskPriority.HIGH.name -> SoftRed; TaskPriority.MEDIUM.name -> Color(0xFFFFA726); else -> Color(0xFF43A047) }
    val completed = task.status == TaskStatus.COMPLETED.name
    val statusProgress = when (task.status) { TaskStatus.COMPLETED.name -> 1f; TaskStatus.IN_PROGRESS.name -> .68f; else -> 0f }
    val statusText = when (task.status) { TaskStatus.COMPLETED.name -> "已完成"; TaskStatus.IN_PROGRESS.name -> "进行中"; else -> "未开始" }
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPress),
        shape = RoundedCornerShape(17.dp), tonalElevation = 1.dp, shadowElevation = 2.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(104.dp).clip(RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp)).then(Modifier), contentAlignment = Alignment.Center) {
                Surface(color = priorityColor, modifier = Modifier.fillMaxSize()) {}
            }
            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.subject?.let { subject ->
                        Surface(color = Color(subject.colorArgb.toULong()).copy(alpha = .14f), shape = RoundedCornerShape(10.dp)) {
                            Text(subject.name, color = Color(subject.colorArgb.toULong()), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                Text("计划 ${task.plannedMinutes} 分钟  ·  已学 ${task.actualMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                IconButton(onClick = onToggle) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { statusProgress }, modifier = Modifier.size(42.dp), strokeWidth = 4.dp, color = TealPrimary, trackColor = MaterialTheme.colorScheme.outlineVariant)
                        if (completed) Icon(Icons.Default.Check, null, tint = TealPrimary)
                    }
                }
                Text(statusText, color = if (task.status == TaskStatus.NOT_STARTED.name) MaterialTheme.colorScheme.onSurfaceVariant else TealPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(existing: StudyTaskEntity?, subjects: List<SubjectEntity>, defaultDate: Long, onDismiss: () -> Unit, onSave: (StudyTaskEntity) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var subjectId by remember(existing?.id) { mutableStateOf(existing?.subjectId ?: subjects.firstOrNull()?.id) }
    var planned by remember(existing?.id) { mutableStateOf((existing?.plannedMinutes ?: 30).toString()) }
    var actual by remember(existing?.id) { mutableStateOf((existing?.actualMinutes ?: 0).toString()) }
    var priority by remember(existing?.id) { mutableStateOf(existing?.priority ?: TaskPriority.MEDIUM.name) }
    var status by remember(existing?.id) { mutableStateOf(existing?.status ?: TaskStatus.NOT_STARTED.name) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.dateEpochDay ?: defaultDate) }
    var subjectMenu by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val stableTaskId = remember(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "添加学习任务" else "编辑学习任务") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("任务名称 *") }, singleLine = true, isError = error != null && name.isBlank(), modifier = Modifier.fillMaxWidth()) }
                item {
                    Box { OutlinedButton(onClick = { subjectMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(subjects.firstOrNull { it.id == subjectId }?.name ?: "选择科目") }
                        DropdownMenu(subjectMenu, { subjectMenu = false }) { subjects.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { subjectId = s.id; subjectMenu = false }) } }
                    }
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(planned, { planned = it.filter(Char::isDigit) }, label = { Text("计划分钟") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(actual, { actual = it.filter(Char::isDigit) }, label = { Text("实际分钟") }, modifier = Modifier.weight(1f), singleLine = true)
                } }
                item { Text("优先级"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TaskPriority.entries.forEach { p -> AssistChip(onClick = { priority = p.name }, label = { Text(when(p) { TaskPriority.HIGH -> "高"; TaskPriority.MEDIUM -> "中"; TaskPriority.LOW -> "低" }) }, leadingIcon = if (priority == p.name) {{ Icon(Icons.Default.Check, null, Modifier.size(15.dp)) }} else null) } } }
                item { Text("状态"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TaskStatus.entries.forEach { s -> AssistChip(onClick = { status = s.name }, label = { Text(when(s) { TaskStatus.NOT_STARTED -> "未开始"; TaskStatus.IN_PROGRESS -> "进行中"; TaskStatus.COMPLETED -> "已完成" }) }) } } }
                item { OutlinedButton(onClick = { dateOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(DateUtils.fullDate(date)) } }
                item { OutlinedTextField(note, { note = it }, label = { Text("备注") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth()) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { Button(onClick = {
            val plannedValue = planned.toIntOrNull()
            val actualValue = actual.toIntOrNull()
            error = when { name.isBlank() -> "请输入任务名称"; plannedValue == null || actualValue == null -> "请输入正确的学习时长"; else -> null }
            if (error == null) onSave(StudyTaskEntity(stableTaskId, name.trim(), subjectId, plannedValue!!, actualValue!!, date, priority, status, note.trim(), existing?.createdAtEpochMillis ?: now, now))
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
    if (dateOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { dateOpen = false }, confirmButton = { TextButton(onClick = {
            picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
            dateOpen = false
        }) { Text("确定") } }, dismissButton = { TextButton(onClick = { dateOpen = false }) { Text("取消") } }) { DatePicker(picker) }
    }
}
