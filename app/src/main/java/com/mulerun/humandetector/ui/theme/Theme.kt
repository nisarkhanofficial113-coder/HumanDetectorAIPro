package com.mulerun.humandetector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RadarGreen = Color(0xFF22FF88)
val RadarGreenDim = Color(0xFF0FAE5D)
val Warn = Color(0xFFFF3B4E)
val Amber = Color(0xFFFFB454)
val Bg = Color(0xFF03060A)
val Panel = Color(0xFF0B1116)
val Muted = Color(0xFF6A7480)
val Line = Color(0xFF162029)

private val darkScheme = darkColorScheme(
    primary = RadarGreen,
    onPrimary = Color.Black,
    secondary = Amber,
    background = Bg,
    onBackground = Color(0xFFE6F0EA),
    surface = Panel,
    onSurface = Color(0xFFE6F0EA),
    error = Warn
)

@Composable
fun HdTheme(nightMode: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkScheme, content = content)
}
