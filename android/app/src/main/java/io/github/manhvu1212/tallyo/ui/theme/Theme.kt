package io.github.manhvu1212.tallyo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TallyoColorScheme = darkColorScheme(
    primary = TallyoColors.Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = TallyoColors.Accent,
    background = TallyoColors.Bg,
    onBackground = TallyoColors.Text,
    surface = TallyoColors.Surface,
    onSurface = TallyoColors.Text,
    surfaceVariant = TallyoColors.SurfaceAlt,
    onSurfaceVariant = TallyoColors.TextMuted,
    outline = TallyoColors.Border,
    error = TallyoColors.Danger,
)

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
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TallyoColorScheme,
        typography = TallyoTypography,
        content = content,
    )
}
