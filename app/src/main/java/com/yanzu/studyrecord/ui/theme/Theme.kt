package com.yanzu.studyrecord.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yanzu.studyrecord.data.ThemeMode

val TealPrimary = Color(0xFF00897B)
val TealLight = Color(0xFF4DB6AC)
val SoftRed = Color(0xFFE57373)
val SoftBackground = Color(0xFFF7F9F9)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F2EE),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = TealLight,
    background = SoftBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF4F3),
    error = SoftRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65D8CA),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    secondary = Color(0xFF7AD6CB),
    background = Color(0xFF101413),
    surface = Color(0xFF171C1B),
    surfaceVariant = Color(0xFF25302E),
    error = Color(0xFFFFB4AB),
)

@Composable
fun StudyRecordTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT.name -> false
        ThemeMode.DARK.name -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }
    MaterialTheme(colorScheme = colors, content = content)
}
