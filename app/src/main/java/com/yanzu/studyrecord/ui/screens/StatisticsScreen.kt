package com.yanzu.studyrecord.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yanzu.studyrecord.AppUiState
import com.yanzu.studyrecord.ui.theme.TealPrimary
import com.yanzu.studyrecord.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatisticsScreen(state: AppUiState, outerPadding: PaddingValues) {
    val stats = state.stats
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(outerPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("统计", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("今日", DateUtils.formatMinutes(stats.todayMinutes), Icons.Default.CalendarToday, Modifier.weight(1f))
                MetricCard("本周", DateUtils.formatMinutes(stats.weekMinutes), Icons.Default.BarChart, Modifier.weight(1f))
                MetricCard("本月", DateUtils.formatMinutes(stats.monthMinutes), Icons.Default.CalendarToday, Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFFFE7DB), shape = CircleShape, modifier = Modifier.size(58.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6D00), modifier = Modifier.size(34.dp)) } }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("连续学习", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stats.streakDays} 天", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) { Text("本周完成率"); Text("${stats.weekCompletionRate}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("最近 7 天每日学习时长", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    SevenDayChart(state)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("各科目累计学习时长", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val total = stats.subjectDurations.sumOf { it.minutes }.coerceAtLeast(1)
                    if (stats.subjectDurations.isEmpty()) Text("还没有学习记录", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 28.dp).align(Alignment.CenterHorizontally))
                    stats.subjectDurations.forEach { row ->
                        val color = Color((row.subject?.colorArgb ?: 0xFF9E9E9E).toULong())
                        val percent = row.minutes * 100 / total
                        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                            Text(row.subject?.name ?: "其他", Modifier.width(60.dp).padding(start = 8.dp))
                            Text(DateUtils.formatMinutes(row.minutes), Modifier.width(82.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress = { row.minutes.toFloat() / total }, modifier = Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(10.dp)), color = color, trackColor = color.copy(alpha = .14f), strokeCap = StrokeCap.Round)
                            Text("$percent%", Modifier.width(48.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(46.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) } }
            Text(label, modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SevenDayChart(state: AppUiState) {
    val values = state.stats.last7Days
    val max = (values.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(60)
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(5) { line ->
                    val y = size.height * line / 4f
                    drawLine(grid, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                }
                val slot = size.width / 7f
                values.forEachIndexed { index, item ->
                    val h = size.height * item.minutes / max
                    val left = slot * index + slot * .24f
                    drawRoundRect(primary, topLeft = androidx.compose.ui.geometry.Offset(left, size.height - h), size = androidx.compose.ui.geometry.Size(slot * .52f, h), cornerRadius = CornerRadius(10f, 10f))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            values.forEach { item ->
                val date = LocalDate.ofEpochDay(item.epochDay)
                Text(date.format(DateTimeFormatter.ofPattern("E", Locale.CHINA)), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
