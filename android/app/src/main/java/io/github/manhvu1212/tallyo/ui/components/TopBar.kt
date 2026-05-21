package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    TopBar(
        onBack = onBack,
        title = {
            Text(
                title,
                color = TallyoColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = trailing
    )
}

@Composable
fun TopBar(
    onBack: () -> Unit,
    title: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TallyoColors.Bg)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = TallyoColors.Primary,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            title()
        }
        if (trailing != null) trailing()
    }
}
