package io.github.manhvu1212.tallyo.ui.newsession

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.manhvu1212.tallyo.R
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.components.Ime
import io.github.manhvu1212.tallyo.ui.components.PrimaryButton
import io.github.manhvu1212.tallyo.ui.components.TallyoTextField
import io.github.manhvu1212.tallyo.ui.components.TopBar
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewSessionScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: NewSessionViewModel = viewModel(factory = NewSessionViewModel.factory(container.repository))

    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var playerInput by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<String>()) }
    var zeroSum by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf<String?>(null) }
    var notEnough by remember { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }

    val canCreate = players.size >= 2

    val addPlayer = lambda@{
        val trimmed = playerInput.trim()
        if (trimmed.isEmpty()) return@lambda
        if (players.contains(trimmed)) {
            duplicateName = trimmed
            return@lambda
        }
        players = players + trimmed
        playerInput = ""
    }

    val submit = {
        if (!canCreate) {
            notEnough = true
        } else {
            vm.create(
                name = name,
                playerNames = players,
                zeroSum = zeroSum,
                defaultName = context.getString(R.string.new_session_default_name),
                onCreated = onCreated,
            )
        }
    }

    Column(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
        TopBar(title = stringResource(R.string.new_title), onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Section(title = stringResource(R.string.new_name_label)) {
                TallyoTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.new_name_placeholder),
                    keyboardOptions = Ime.Next,
                    keyboardActions = KeyboardActions(onNext = { playerFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section(
                title = stringResource(R.string.new_players_label),
                hint = stringResource(R.string.new_players_hint),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TallyoTextField(
                        value = playerInput,
                        onValueChange = { playerInput = it },
                        placeholder = stringResource(R.string.new_players_placeholder),
                        keyboardOptions = Ime.Done,
                        // Handle both Done and Next: some IMEs keep the previous
                        // field's Next state even after focus moves to a Done field.
                        keyboardActions = KeyboardActions(
                            onDone = { addPlayer() },
                            onNext = { addPlayer() },
                        ),
                        focusRequester = playerFocus,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    PrimaryButton(
                        label = stringResource(R.string.new_players_add),
                        onClick = addPlayer,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (players.isEmpty()) {
                    Text(
                        stringResource(R.string.new_players_empty),
                        color = TallyoColors.TextMuted,
                        fontSize = 11.sp,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        players.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(TallyoColors.SurfaceAlt)
                                    .border(1.dp, TallyoColors.Border, RoundedCornerShape(999.dp))
                                    .clickable { players = players.filterNot { it == p } }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p, color = TallyoColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("  ×", color = TallyoColors.TextMuted, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Section(title = stringResource(R.string.new_options_label)) {
                val shape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (zeroSum) TallyoColors.PrimaryTintBg else TallyoColors.Surface)
                        .border(
                            1.dp,
                            if (zeroSum) TallyoColors.Primary else TallyoColors.Border,
                            shape,
                        )
                        .clickable { zeroSum = !zeroSum }
                        .padding(12.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckBox(checked = zeroSum)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                stringResource(R.string.new_options_zero_sum_title),
                                color = TallyoColors.Text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            stringResource(R.string.new_options_zero_sum_desc),
                            color = TallyoColors.TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp, start = 28.dp),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            PrimaryButton(
                label = stringResource(R.string.new_submit),
                onClick = submit,
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Spacer(Modifier.height(48.dp))
        }
    }

    duplicateName?.let { dup ->
        AlertDialog(
            onDismissRequest = { duplicateName = null },
            title = { Text(stringResource(R.string.new_players_duplicate_title), color = TallyoColors.Text) },
            text = {
                Text(
                    stringResource(R.string.new_players_duplicate_message, dup),
                    color = TallyoColors.TextMuted,
                )
            },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = { duplicateName = null }) {
                    Text("OK", color = TallyoColors.Primary)
                }
            },
        )
    }

    if (notEnough) {
        AlertDialog(
            onDismissRequest = { notEnough = false },
            title = { Text(stringResource(R.string.new_min_players), color = TallyoColors.Text) },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = { notEnough = false }) { Text("OK", color = TallyoColors.Primary) }
            },
        )
    }
}

@Composable
private fun Section(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(title, color = TallyoColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (hint != null) Text(hint, color = TallyoColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    val color = if (checked) TallyoColors.Primary else TallyoColors.Border
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) TallyoColors.Primary else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, color, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
