package io.github.manhvu1212.tallyo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class TallyoPalette(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryDim: Color,
    val accent: Color,
    val warning: Color,
    val danger: Color,
    val win: Color,
    val loss: Color,
    val primaryTintBg: Color,
)

private val DarkPalette = TallyoPalette(
    bg = Color(0xFF0F1115),
    surface = Color(0xFF1A1D24),
    surfaceAlt = Color(0xFF22262F),
    border = Color(0xFF2D323C),
    text = Color(0xFFF5F6F8),
    textMuted = Color(0xFF9AA1AC),
    primary = Color(0xFF7C5CFF),
    primaryDim = Color(0xFF5A45B8),
    accent = Color(0xFF4ED2A8),
    warning = Color(0xFFF5A623),
    danger = Color(0xFFE5484D),
    win = Color(0xFF4ED2A8),
    loss = Color(0xFFE5484D),
    primaryTintBg = Color(0xFF231D3D),
)

private val LightPalette = TallyoPalette(
    bg = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEFF1F5),
    border = Color(0xFFE2E5EB),
    text = Color(0xFF101218),
    textMuted = Color(0xFF6B7280),
    primary = Color(0xFF6B47FF),
    primaryDim = Color(0xFFB7A8FF),
    accent = Color(0xFF1FAA84),
    warning = Color(0xFFD9881C),
    danger = Color(0xFFD92D32),
    win = Color(0xFF1FAA84),
    loss = Color(0xFFD92D32),
    primaryTintBg = Color(0xFFEDE8FF),
)

val LocalTallyoPalette = staticCompositionLocalOf { DarkPalette }

/**
 * Public color accessor. Properties are @Composable so they resolve against
 * the active palette in the composition (light vs dark).
 */
object TallyoColors {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.surface
    val SurfaceAlt: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.surfaceAlt
    val Border: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.border
    val Text: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.text
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.textMuted
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.primary
    val PrimaryDim: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.primaryDim
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.accent
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.warning
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.danger
    val Win: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.win
    val Loss: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.loss
    val PrimaryTintBg: Color @Composable @ReadOnlyComposable get() = LocalTallyoPalette.current.primaryTintBg
}

private fun TallyoPalette.toColorScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = accent,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = textMuted,
        outline = border,
        error = danger,
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = accent,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = textMuted,
        outline = border,
        error = danger,
    )
}

private val TallyoTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun TallyoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalTallyoPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = TallyoTypography,
            content = content,
        )
    }
}
