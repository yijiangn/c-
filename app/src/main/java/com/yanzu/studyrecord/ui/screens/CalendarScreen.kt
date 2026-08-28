package com.yanzu.studyrecord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yanzu.studyrecord.AppUiState
import com.yanzu.studyrecord.AppViewModel
import com.yanzu.studyrecord.data.StudyTaskEntity
import com.yanzu.studyrecord.data.StudySessionEntity
import com.yanzu.studyrecord.data.TaskStatus
import com.yanzu.studyrecord.ui.theme.TealPrimary
import com.yanzu.studyrecord.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(state: AppUiState, viewModel: AppViewModel, outerPadding: PaddingValues) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDay by remember { mutableStateOf(today.toEpochDay()) }
    var addingTask by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var addingRecord by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<StudySessionEntity?>(null) }
    val selectedTasks = state.tasks.filter { it.task.dateEpochDay == selectedDay }
    val selectedSessions = state.sessions.filter { it.dateEpochDay == selectedDay && it.durationMinutes > 0 }
    val totalMinutes = selectedSessions.sumOf { it.durationMinutes }
    val studyDays = state.sessions.filter { it.durationMinutes > 0 }.map { it.dateEpochDay }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(outerPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("日历", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(month.format(DateTimeFormatter.ofPattern("M月 yyyy", Locale.CHINA)), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { MonthCard(month, selectedDay, today.toEpochDay(), studyDays, onPrevious = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) }, onSelect = { selectedDay = it }) }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(DateUtils.shortDate(selectedDay), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("总学习时长", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Text(DateUtils.formatMinutes(totalMinutes), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { addingTask = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("补记任务") }
                        Button(onClick = { addingRecord = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.History, null); Text("补记时长") }
                    }
                    if (selectedTasks.isEmpty() && selectedSessions.isEmpty()) Text("这一天还没有学习记录", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally))
                }
            }
        }
        items(selectedTasks, key = { it.task.id }) { item ->
            Card(onClick = { editingTask = item.task }, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color((item.subject?.colorArgb ?: 0xFF9E9E9E).toULong()).copy(alpha = .14f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(item.subject?.name?.take(1) ?: "其", color = Color((item.subject?.colorArgb ?: 0xFF9E9E9E).toULong()), fontWeight = FontWeight.Bold) }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("${item.subject?.name ?: "其他"} - ${item.task.name}", style = MaterialTheme.typography.titleMedium)
                        Text("计划 ${item.task.plannedMinutes} 分钟 · 已学 ${item.task.actualMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.task.status == TaskStatus.COMPLETED.name) Icon(Icons.Default.Check, "已完成", tint = TealPrimary) else Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        if (selectedSessions.isNotEmpty()) item {
            Text("学习时段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            selectedSessions.forEach { session ->
                val taskName = state.tasks.firstOrNull { it.task.id == session.taskId }?.task?.name ?: "独立学习记录"
                Row(Modifier.fillMaxWidth().clickable { editingSession = session }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${DateUtils.time(session.startEpochMillis)}–${DateUtils.time(session.endEpochMillis)}  $taskName · ${session.durationMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, "修改记录", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (addingTask || editingTask != null) TaskEditorDialog(editingTask, state.subjects, selectedDay, onDismiss = { addingTask = false; editingTask = null }, onSave = { task -> viewModel.saveTask(task) { addingTask = false; editingTask = null } })
    if (addingRecord) ManualRecordDialog(state, selectedDay, onDismiss = { addingRecord = false }, onSave = { taskId, subjectId, minutes ->
        viewModel.addManualSession(taskId, subjectId, selectedDay, minutes); addingRecord = false
    })
    editingSession?.let { session -> SessionEditorDialog(session, onDismiss = { editingSession = null }, onSave = { minutes -> viewModel.updateSession(session.copy(durationMinutes = minutes)); editingSession = null }, onDelete = { viewModel.deleteSession(session.id); editingSession = null }) }
}

@Composable
private fun SessionEditorDialog(session: StudySessionEntity, onDismiss: () -> Unit, onSave: (Int) -> Unit, onDelete: () -> Unit) {
    var value by remember(session.id) { mutableStateOf(session.durationMinutes.toString()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("修改学习记录") }, text = {
        Column { OutlinedTextField(value, { value = it.filter(Char::isDigit) }, label = { Text("学习时长（分钟）") }, singleLine = true); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; TextButton(onClick = { confirmDelete = true }) { Text("删除这条记录", color = MaterialTheme.colorScheme.error) } }
    }, confirmButton = { Button(onClick = { val minutes = value.toIntOrNull(); if (minutes == null || minutes <= 0) error = "请输入大于 0 的分钟数" else onSave(minutes) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("删除学习记录？") }, text = { Text("删除后会从统计和关联任务的实际时长中扣除。") }, confirmButton = { TextButton(onClick = onDelete) { Text("确认删除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
}

@Composable
private fun MonthCard(month: YearMonth, selectedDay: Long, today: Long, studyDays: Set<Long>, onPrevious: () -> Unit, onNext: () -> Unit, onSelect: (Long) -> Unit) {
    val firstOffset = month.atDay(1).dayOfWeek.value % 7
    val cells = List(firstOffset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "上个月") }
                Text(month.format(DateTimeFormatter.ofPattern("M月 yyyy", Locale.CHINA)), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "下个月") }
            }
            Row(Modifier.fillMaxWidth()) { listOf("日", "一", "二", "三", "四", "五", "六").forEach { Text(it, Modifier.weight(1f).padding(vertical = 10.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (week + List(7 - week.size) { null }).forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).height(54.dp))
                        else {
                            val epoch = date.toEpochDay()
                            Column(Modifier.weight(1f).height(54.dp).clickable { onSelect(epoch) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(if (epoch == today) TealPrimary else if (epoch == selectedDay) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) { Text(date.dayOfMonth.toString(), color = if (epoch == today) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = if (epoch == selectedDay || epoch == today) FontWeight.Bold else FontWeight.Normal) }
                                if (epoch in studyDays) Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(TealPrimary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualRecordDialog(state: AppUiState, date: Long, onDismiss: () -> Unit, onSave: (String?, String?, Int) -> Unit) {
    val tasks = state.tasks.filter { it.task.dateEpochDay == date }
    var taskId by remember { mutableStateOf<String?>(null) }
    var subjectId by remember { mutableStateOf(state.subjects.firstOrNull()?.id) }
    var minutes by remember { mutableStateOf("30") }
    var taskMenu by remember { mutableStateOf(false) }
    var subjectMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("补记学习时长") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box { OutlinedButton(onClick = { taskMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(tasks.firstOrNull { it.task.id == taskId }?.task?.name ?: "选择任务（可选）") }
                DropdownMenu(taskMenu, { taskMenu = false }) {
                    DropdownMenuItem(text = { Text("不关联任务") }, onClick = { taskId = null; taskMenu = false })
                    tasks.forEach { t -> DropdownMenuItem(text = { Text(t.task.name) }, onClick = { taskId = t.task.id; subjectId = t.task.subjectId; taskMenu = false }) }
                }
            }
            Box { OutlinedButton(onClick = { subjectMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(state.subjects.firstOrNull { it.id == subjectId }?.name ?: "选择科目") }
                DropdownMenu(subjectMenu, { subjectMenu = false }) { state.subjects.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { subjectId = s.id; subjectMenu = false }) } }
            }
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("学习时长（分钟）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(onClick = { val value = minutes.toIntOrNull(); if (value == null || value <= 0) error = "请输入大于 0 的分钟数" else onSave(taskId, subjectId, value) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
