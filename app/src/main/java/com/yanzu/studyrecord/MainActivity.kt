package com.yanzu.studyrecord

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yanzu.studyrecord.data.TimerState
import com.yanzu.studyrecord.service.StudyTimerService
import com.yanzu.studyrecord.ui.StudyTimerDialog
import com.yanzu.studyrecord.ui.screens.CalendarScreen
import com.yanzu.studyrecord.ui.screens.SettingsScreen
import com.yanzu.studyrecord.ui.screens.StatisticsScreen
import com.yanzu.studyrecord.ui.screens.TodayScreen
import com.yanzu.studyrecord.ui.theme.StudyRecordTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            StudyRecordTheme(state.settings.themeMode) {
                val navController = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                var timerVisible by remember { mutableStateOf(false) }
                var autoOpenedActiveTimer by remember { mutableStateOf(false) }
                var goalNotified by rememberSaveable { mutableStateOf(false) }
                var weeklyGoalNotified by rememberSaveable { mutableStateOf(false) }
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                val context = LocalContext.current
                val startTimerService = {
                    if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    ContextCompat.startForegroundService(context, Intent(context, StudyTimerService::class.java))
                }

                LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
                LaunchedEffect(state.settings.timerState) {
                    if (state.settings.timerState != TimerState.IDLE.name && !autoOpenedActiveTimer) {
                        timerVisible = true; autoOpenedActiveTimer = true
                    }
                    if (state.settings.timerState == TimerState.IDLE.name) autoOpenedActiveTimer = false
                }
                LaunchedEffect(state.stats.todayMinutes, state.settings.dailyGoalMinutes) {
                    val reached = state.stats.todayMinutes >= state.settings.dailyGoalMinutes && state.settings.dailyGoalMinutes > 0
                    if (reached && !goalNotified) { goalNotified = true; snackbar.showSnackbar("今天的学习目标完成了，做得很棒！") }
                    if (!reached) goalNotified = false
                }
                LaunchedEffect(state.stats.weekMinutes, state.settings.weeklyGoalMinutes) {
                    val reached = state.stats.weekMinutes >= state.settings.weeklyGoalMinutes && state.settings.weeklyGoalMinutes > 0
                    if (reached && !weeklyGoalNotified) { weeklyGoalNotified = true; snackbar.showSnackbar("本周学习目标也完成了，继续保持！") }
                    if (!reached) weeklyGoalNotified = false
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        val entry by navController.currentBackStackEntryAsState()
                        NavigationBar {
                            NavItem.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = entry?.destination?.route == item.route,
                                    onClick = { navController.navigate(item.route) { popUpTo("today") { saveState = true }; launchSingleTop = true; restoreState = true } },
                                    icon = { Icon(item.icon, null) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(navController, startDestination = "today") {
                        composable("today") { TodayScreen(state, viewModel, padding, onStartTimer = { timerVisible = true }) }
                        composable("calendar") { CalendarScreen(state, viewModel, padding) }
                        composable("statistics") { StatisticsScreen(state, padding) }
                        composable("settings") { SettingsScreen(state, viewModel, padding) }
                    }
                }

                if (timerVisible) StudyTimerDialog(state.settings, state.tasks, state.subjects, viewModel, onDismiss = { timerVisible = false }, onStartService = startTimerService)
            }
        }
    }
}

private enum class NavItem(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "今日", Icons.Outlined.Home),
    CALENDAR("calendar", "日历", Icons.Outlined.CalendarMonth),
    STATISTICS("statistics", "统计", Icons.Outlined.BarChart),
    SETTINGS("settings", "设置", Icons.Outlined.Settings),
}
