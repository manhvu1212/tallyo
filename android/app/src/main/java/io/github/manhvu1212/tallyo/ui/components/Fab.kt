package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun TallyoFab(
    label: String?,
    onClick: () -> Unit,
    icon: String = "+",
) {
    val shape = if (label == null) CircleShape else RoundedCornerShape(999.dp)
    val mod = Modifier
        .shadow(8.dp, shape, clip = false)
        .clip(shape)
        .background(TallyoColors.Primary)
        .clickable(onClick = onClick)

    if (label == null) {
        Row(
            mod.size(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(icon, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Row(
            mod.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(icon, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
