package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

enum class ButtonVariant { Primary, Secondary, Ghost, Danger }

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val (bg, fg, border) = when (variant) {
        ButtonVariant.Primary -> Triple(TallyoColors.Primary, Color.White, TallyoColors.Primary)
        ButtonVariant.Secondary -> Triple(TallyoColors.SurfaceAlt, TallyoColors.Text, TallyoColors.Border)
        ButtonVariant.Ghost -> Triple(Color.Transparent, TallyoColors.Text, TallyoColors.Border)
        ButtonVariant.Danger -> Triple(TallyoColors.Danger, Color.White, TallyoColors.Danger)
    }
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .alpha(if (enabled && !loading) 1f else 0.5f)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = fg,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(4.dp),
            )
        } else {
            Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
