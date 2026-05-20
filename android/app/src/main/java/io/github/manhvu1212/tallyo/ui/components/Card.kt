package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TallyoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .clip(shape)
        .background(TallyoColors.Surface)
        .border(1.dp, TallyoColors.Border, shape)

    val clickable = if (onClick != null || onLongClick != null) {
        base.combinedClickable(
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
    } else base

    androidx.compose.foundation.layout.Box(
        modifier = clickable.padding(padding),
    ) {
        content()
    }
}
