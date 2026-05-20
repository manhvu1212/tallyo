package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun TallyoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .clip(shape)
        .background(TallyoColors.Surface)
        .border(1.dp, TallyoColors.Border, shape)
    val clickableMod = if (onClick != null) base.clickable(onClick = onClick) else base

    Box(modifier = clickableMod.padding(padding)) {
        content()
    }
}
