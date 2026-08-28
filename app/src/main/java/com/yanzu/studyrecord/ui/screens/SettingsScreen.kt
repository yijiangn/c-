package com.yanzu.studyrecord.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanzu.studyrecord.AppUiState
import com.yanzu.studyrecord.AppViewModel
import com.yanzu.studyrecord.BuildConfig
import com.yanzu.studyrecord.data.RestoreMode
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.ThemeMode
import com.yanzu.studyrecord.ui.theme.TealPrimary
import java.time.LocalDate
import java.util.UUID

private enum class SettingDialog { DAILY, WEEKLY, POMODORO, THEME }

@Composable
fun SettingsScreen(state: AppUiState, viewModel: AppViewModel, outerPadding: PaddingValues) {
    var dialog by remember { mutableStateOf<SettingDialog?>(null) }
    var subjectsOpen by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var clearStep by remember { mutableIntStateOf(0) }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(viewModel::exportJson) }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(viewModel::exportCsv) }
    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(outerPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) }
        item { SettingsRow(Icons.Default.MenuBook, "科目管理", "${state.subjects.size} 个科目") { subjectsOpen = true } }
        item { SettingsRow(Icons.Default.TrackChanges, "每日目标", "${state.settings.dailyGoalMinutes} 分钟") { dialog = SettingDialog.DAILY } }
        item { SettingsRow(Icons.Default.CalendarMonth, "每周目标", "${state.settings.weeklyGoalMinutes} 分钟") { dialog = SettingDialog.WEEKLY } }
        item { SettingsRow(Icons.Default.Timer, "番茄钟时长", "${state.settings.pomodoroMinutes} 分钟") { dialog = SettingDialog.POMODORO } }
        item { SettingsRow(Icons.Default.ColorLens, "主题模式", when (state.settings.themeMode) { ThemeMode.LIGHT.name -> "浅色"; ThemeMode.DARK.name -> "深色"; else -> "跟随系统" }) { dialog = SettingDialog.THEME } }
        item { SettingsRow(Icons.Default.Backup, "导出 JSON 完整备份", "") { jsonExport.launch("学习记录备份_${LocalDate.now()}.json") } }
        item { SettingsRow(Icons.Default.Restore, "从 JSON 恢复备份", "") { jsonImport.launch(arrayOf("application/json", "text/plain")) } }
        item { SettingsRow(Icons.Default.FileDownload, "导出 CSV 学习记录", "") { csvExport.launch("学习记录_${LocalDate.now()}.csv") } }
        item {
            SettingsRow(Icons.Default.DeleteForever, "清除全部数据", "", destructive = true) { clearStep = 1 }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("学习记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("版本 v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.outline)
                Text("数据仅保存在本机 · 无广告 · 不联网", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    if (subjectsOpen) SubjectManagerDialog(state, viewModel) { subjectsOpen = false }
    dialog?.let { type -> ValueSettingDialog(type, state, onDismiss = { dialog = null }, onSave = { value ->
        when (type) { SettingDialog.DAILY -> viewModel.updateGoals(daily = value); SettingDialog.WEEKLY -> viewModel.updateGoals(weekly = value); SettingDialog.POMODORO -> viewModel.updatePomodoro(value); else -> Unit }
        dialog = null
    }, onTheme = { viewModel.updateTheme(it); dialog = null }) }
    restoreUri?.let { uri ->
        AlertDialog(onDismissRequest = { restoreUri = null }, title = { Text("恢复备份") }, text = { Text("请选择恢复方式。覆盖会先清除当前数据；合并会按记录 ID 合并，并保留当前其他记录。") }, confirmButton = {
            Button(onClick = { viewModel.restoreJson(uri, RestoreMode.MERGE); restoreUri = null }) { Text("合并数据") }
        }, dismissButton = { OutlinedButton(onClick = { viewModel.restoreJson(uri, RestoreMode.REPLACE); restoreUri = null }) { Text("覆盖现有数据") } })
    }
    if (clearStep == 1) AlertDialog(onDismissRequest = { clearStep = 0 }, title = { Text("清除全部数据？") }, text = { Text("任务、计时记录、科目和目标设置都会被删除。建议先导出 JSON 备份。") }, confirmButton = { TextButton(onClick = { clearStep = 2 }) { Text("继续", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { clearStep = 0 }) { Text("取消") } })
    if (clearStep == 2) AlertDialog(onDismissRequest = { clearStep = 0 }, title = { Text("请再次确认") }, text = { Text("此操作无法撤销，确定清除本机的全部学习数据吗？") }, confirmButton = { TextButton(onClick = { viewModel.clearAll(); clearStep = 0 }) { Text("确认全部清除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { clearStep = 0 }) { Text("取消") } })
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else TealPrimary
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = if (destructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = .24f) else MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Text(title, Modifier.weight(1f).padding(start = 18.dp), style = MaterialTheme.typography.titleMedium, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (value.isNotBlank()) Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ChevronRight, null, tint = if (destructive) tint else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ValueSettingDialog(type: SettingDialog, state: AppUiState, onDismiss: () -> Unit, onSave: (Int) -> Unit, onTheme: (String) -> Unit) {
    if (type == SettingDialog.THEME) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text("主题模式") }, text = {
            Column { listOf(ThemeMode.SYSTEM.name to "跟随系统", ThemeMode.LIGHT.name to "浅色", ThemeMode.DARK.name to "深色").forEach { (mode, label) ->
                Row(Modifier.fillMaxWidth().clickable { onTheme(mode) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(state.settings.themeMode == mode, onClick = { onTheme(mode) }); Text(label) }
            } }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
        return
    }
    val initial = when (type) { SettingDialog.DAILY -> state.settings.dailyGoalMinutes; SettingDialog.WEEKLY -> state.settings.weeklyGoalMinutes; SettingDialog.POMODORO -> state.settings.pomodoroMinutes; else -> 0 }
    var value by remember(type) { mutableStateOf(initial.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val title = when(type) { SettingDialog.DAILY -> "每日目标"; SettingDialog.WEEKLY -> "每周目标"; else -> "番茄钟时长" }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column { OutlinedTextField(value, { value = it.filter(Char::isDigit) }, label = { Text("分钟") }, singleLine = true); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = { Button(onClick = { val number = value.toIntOrNull(); error = when { number == null || number <= 0 -> "请输入大于 0 的分钟数"; type == SettingDialog.POMODORO && number > 180 -> "番茄钟最长为 180 分钟"; else -> null }; if (error == null) onSave(number!!) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun SubjectManagerDialog(state: AppUiState, viewModel: AppViewModel, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf<SubjectEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<SubjectEntity?>(null) }
    var deleteCount by remember { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("科目管理") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.subjects, key = { it.id }) { subject ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).background(Color(subject.colorArgb.toULong()), CircleShape))
                        Text(subject.name, Modifier.weight(1f).padding(start = 12.dp))
                        IconButton(onClick = { editing = subject }) { Icon(Icons.Default.Edit, "修改") }
                        if (!subject.isBuiltIn) IconButton(onClick = { viewModel.checkSubjectTaskCount(subject.id) { count -> deleteCount = count; deleteCandidate = subject } }) { Icon(Icons.Default.DeleteForever, "删除", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item { OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("添加自定义科目") } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
    if (adding || editing != null) SubjectEditorDialog(editing, onDismiss = { adding = false; editing = null }, onSave = { subject -> viewModel.saveSubject(subject); adding = false; editing = null })
    deleteCandidate?.let { subject ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("删除科目“${subject.name}”？") }, text = { Text(if (deleteCount > 0) "该科目下有 $deleteCount 个任务。删除后，这些任务将移动到“其他”。" else "该科目下没有任务，可以安全删除。") }, confirmButton = { TextButton(onClick = { viewModel.deleteSubject(subject.id, move = deleteCount > 0); deleteCandidate = null }) { Text(if (deleteCount > 0) "移动并删除" else "删除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("取消") } })
    }
}

@Composable
private fun SubjectEditorDialog(existing: SubjectEntity?, onDismiss: () -> Unit, onSave: (SubjectEntity) -> Unit) {
    val palette = listOf(0xFFE57373, 0xFFFFA726, 0xFF43A047, 0xFF42A5F5, 0xFF7E57C2, 0xFF26A69A, 0xFF9E9E9E)
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember(existing?.id) { mutableStateOf(existing?.colorArgb ?: palette[5]) }
    var error by remember { mutableStateOf<String?>(null) }
    val stableId = remember(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "添加科目" else "修改科目") }, text = {
        Column { OutlinedTextField(name, { name = it }, label = { Text("科目名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Text("科目颜色", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { palette.forEach { value -> Box(Modifier.size(if (color == value) 34.dp else 28.dp).background(Color(value.toULong()), CircleShape).clickable { color = value }) } }; error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
    }, confirmButton = { Button(onClick = { if (name.isBlank()) error = "科目名称不能为空" else onSave(SubjectEntity(stableId, name.trim(), color, existing?.isBuiltIn ?: false, existing?.sortOrder ?: 1000)) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
