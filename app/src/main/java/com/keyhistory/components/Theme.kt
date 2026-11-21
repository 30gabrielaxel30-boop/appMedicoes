package com.keyhistory.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KhColors {
    val Background = Color(0xFF070B14)
    val Surface = Color(0xFF0F1624)
    val Graphite = Color(0xFF1A2230)
    val GraphiteLight = Color(0xFF263042)
    val Outline = Color(0xFF2A3444)
    val Primary = Color(0xFF3B82F6)
    val PrimarySoft = Color(0xFF1E3A8A)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)
    val Amber = Color(0xFFF59E0B)
    val Text = Color(0xFFE6EAF2)
    val TextMuted = Color(0xFF8A94A6)
    val Chart = listOf(Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFA78BFA), Color(0xFFF59E0B), Color(0xFF06B6D4), Color(0xFF94A3B8))
}

private val scheme = darkColorScheme(
    primary = KhColors.Primary,
    onPrimary = Color.White,
    primaryContainer = KhColors.PrimarySoft,
    onPrimaryContainer = KhColors.Text,
    secondary = KhColors.Green,
    onSecondary = Color.Black,
    tertiary = KhColors.Amber,
    background = KhColors.Background,
    onBackground = KhColors.Text,
    surface = KhColors.Surface,
    onSurface = KhColors.Text,
    surfaceVariant = KhColors.Graphite,
    onSurfaceVariant = KhColors.TextMuted,
    surfaceContainer = KhColors.Surface,
    surfaceContainerHigh = KhColors.Graphite,
    surfaceContainerHighest = KhColors.GraphiteLight,
    surfaceContainerLow = KhColors.Surface,
    error = KhColors.Red,
    onError = Color.White,
    outline = KhColors.Outline,
    outlineVariant = KhColors.Outline,
)

private val base = Typography()
private val typography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

@Composable
fun KeyHistoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
