package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

data class MenuAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun MoreMenuButton(
    items: List<MenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = modifier.size(40.dp),
    ) {
        Icon(Icons.Filled.MoreVert, contentDescription = contentDescription, tint = TallyoColors.TextMuted)
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = TallyoColors.Surface,
        ) {
            items.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            action.label,
                            color = if (action.destructive) TallyoColors.Danger else TallyoColors.Text,
                        )
                    },
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
