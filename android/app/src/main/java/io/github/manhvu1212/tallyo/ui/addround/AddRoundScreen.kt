package io.github.manhvu1212.tallyo.ui.addround

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.manhvu1212.tallyo.R
import io.github.manhvu1212.tallyo.domain.Player
import io.github.manhvu1212.tallyo.domain.Round
import io.github.manhvu1212.tallyo.domain.RoundScore
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.components.PrimaryButton
import io.github.manhvu1212.tallyo.ui.components.TallyoTextField
import io.github.manhvu1212.tallyo.ui.components.TopBar
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

private data class DeltaBtn(val label: String, val value: Int?, val edit: Boolean = false)

private val DELTAS = listOf(
    DeltaBtn("-4", -4),
    DeltaBtn("-2", -2),
    DeltaBtn("-1", -1),
    DeltaBtn("✎", null, edit = true),
    DeltaBtn("+1", 1),
    DeltaBtn("+2", 2),
    DeltaBtn("+4", 4),
)

@Composable
fun AddRoundScreen(
    sessionId: String,
    roundId: String?,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: AddRoundViewModel = viewModel(
        factory = AddRoundViewModel.factory(container.repository, sessionId, roundId),
    )
    val session by vm.session.collectAsStateWithLifecycle()

    val current = session
    if (current == null) {
        Column(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
            TopBar(title = stringResource(R.string.round_title_new), onBack = onBack)
            Text(
                stringResource(R.string.round_not_found),
                color = TallyoColors.TextMuted,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val editing: Round? = remember(current, roundId) {
        roundId?.let { id -> current.rounds.firstOrNull { it.id == id } }
    }

    val visiblePlayers: List<Player> = remember(current, editing) {
        if (editing != null) {
            val ids = editing.scores.map { it.playerId }.toSet()
            current.players.filter { it.id in ids }
        } else {
            current.players.filter { !it.resting }
        }
    }

    val scores: SnapshotStateMap<String, String> = remember(current, editing) {
        mutableStateMapOf<String, String>().apply {
            visiblePlayers.forEach { p ->
                val s = editing?.scores?.firstOrNull { it.playerId == p.id }
                put(p.id, s?.points?.toString() ?: "")
            }
        }
    }

    var note by remember { mutableStateOf(editing?.note.orEmpty()) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var customMode by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }
    var emptyAll by remember { mutableStateOf(false) }
    var needMore by remember { mutableStateOf(false) }
    var unbalancedBy by remember { mutableStateOf<Int?>(null) }
    val customFocus = remember { FocusRequester() }

    LaunchedEffect(activeId) { customMode = false }

    val liveStatus by remember(scores, visiblePlayers) {
        derivedStateOf {
            var sum = 0
            var emptyId: String? = null
            var emptyCount = 0
            var err = false
            for (p in visiblePlayers) {
                val raw = (scores[p.id] ?: "").trim()
                if (raw.isEmpty()) {
                    if (emptyId == null) emptyId = p.id
                    emptyCount++
                    continue
                }
                val n = raw.toIntOrNull()
                if (n == null) {
                    err = true
                    continue
                }
                sum += n
            }
            LiveStatus(sum, emptyId, emptyCount, err)
        }
    }

    val zeroSum = current.zeroSum

    val onSave: () -> Unit = {
        val ls = liveStatus
        if (ls.parseError) {
            invalid = true
        } else {
            val filledCount = visiblePlayers.size - ls.emptyCount
            if (filledCount == 0) {
                emptyAll = true
            } else if (zeroSum && ls.emptyCount >= 2) {
                needMore = true
            } else if (zeroSum && ls.emptyCount == 0 && ls.sum != 0) {
                unbalancedBy = ls.sum
            } else {
                val built: List<RoundScore> = if (zeroSum) {
                    visiblePlayers.map { p ->
                        val raw = (scores[p.id] ?: "").trim()
                        if (raw.isEmpty()) RoundScore(p.id, -ls.sum)
                        else RoundScore(p.id, raw.toInt())
                    }
                } else {
                    visiblePlayers.map { p ->
                        val raw = (scores[p.id] ?: "").trim()
                        RoundScore(p.id, if (raw.isEmpty()) 0 else raw.toInt())
                    }
                }
                val noteFinal = note.trim().takeIf { it.isNotEmpty() }
                if (editing == null) vm.saveNew(built, noteFinal, onBack)
                else vm.saveEdit(built, noteFinal, onBack)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
        TopBar(
            title = if (editing != null) stringResource(R.string.round_title_edit)
            else stringResource(R.string.round_title_new),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                if (zeroSum) stringResource(R.string.round_hint_zero_sum)
                else stringResource(R.string.round_hint_free),
                color = TallyoColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visiblePlayers.forEach { p ->
                    val raw = scores[p.id] ?: ""
                    val isActive = activeId == p.id
                    val isCustom = isActive && customMode
                    val isAutoFilled = zeroSum && liveStatus.emptyCount == 1 && liveStatus.emptyId == p.id
                    val autoVal = -liveStatus.sum

                    val rawNum = raw.toIntOrNull()
                    val valueColor = when {
                        raw.isEmpty() && isAutoFilled -> TallyoColors.Primary
                        raw.isEmpty() -> TallyoColors.TextMuted
                        rawNum != null && rawNum > 0 -> TallyoColors.Win
                        rawNum != null && rawNum < 0 -> TallyoColors.Loss
                        else -> TallyoColors.Text
                    }

                    val displayValue = when {
                        raw.isNotEmpty() -> raw
                        isAutoFilled -> "= ${if (autoVal > 0) "+$autoVal" else "$autoVal"}"
                        else -> "0"
                    }

                    Column {
                        val rowShape = RoundedCornerShape(10.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(if (isActive) TallyoColors.PrimaryTintBg else TallyoColors.Surface)
                                .border(1.dp, if (isActive) TallyoColors.Primary else TallyoColors.Border, rowShape)
                                .clickable { activeId = p.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(p.name, color = TallyoColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (isCustom) {
                                BasicTextField(
                                    value = raw,
                                    onValueChange = { scores[p.id] = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = valueColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { customMode = false }),
                                    cursorBrush = SolidColor(TallyoColors.Primary),
                                    modifier = Modifier
                                        .focusRequester(customFocus)
                                        .padding(vertical = 4.dp),
                                )
                                LaunchedEffect(customMode, activeId) {
                                    if (isCustom) customFocus.requestFocus()
                                }
                            } else {
                                Text(
                                    displayValue,
                                    color = valueColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }

                        if (isActive && !isCustom) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DELTAS.forEach { d ->
                                    val isEdit = d.edit
                                    val isNeg = d.value != null && d.value < 0
                                    val isPos = d.value != null && d.value > 0
                                    val btnShape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(btnShape)
                                            .background(if (isEdit) TallyoColors.Primary else TallyoColors.SurfaceAlt)
                                            .border(1.dp, if (isEdit) TallyoColors.Primary else TallyoColors.Border, btnShape)
                                            .clickable {
                                                if (isEdit) customMode = true
                                                else d.value?.let { delta ->
                                                    val cur = (scores[p.id] ?: "").trim().toIntOrNull() ?: 0
                                                    scores[p.id] = (cur + delta).toString()
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            d.label,
                                            color = when {
                                                isEdit -> Color.White
                                                isNeg -> TallyoColors.Loss
                                                isPos -> TallyoColors.Win
                                                else -> TallyoColors.Text
                                            },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.round_note_label), color = TallyoColors.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            TallyoTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = stringResource(R.string.round_note_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                label = if (editing != null) stringResource(R.string.round_save_edit)
                else stringResource(R.string.round_save_new),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(48.dp))
        }
    }

    SimpleAlert(
        open = invalid,
        title = R.string.round_invalid_title,
        message = R.string.round_invalid_message,
        onDismiss = { invalid = false },
    )
    SimpleAlert(
        open = emptyAll,
        title = R.string.round_empty_title,
        message = R.string.round_empty_message,
        onDismiss = { emptyAll = false },
    )
    if (needMore) {
        AlertDialog(
            onDismissRequest = { needMore = false },
            title = { Text(stringResource(R.string.round_need_more_title), color = TallyoColors.Text) },
            text = {
                Text(
                    stringResource(R.string.round_need_more_message, visiblePlayers.size - 1),
                    color = TallyoColors.TextMuted,
                )
            },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = { needMore = false }) {
                    Text("OK", color = TallyoColors.Primary)
                }
            },
        )
    }
    unbalancedBy?.let { sum ->
        val display = if (sum > 0) "+$sum" else "$sum"
        AlertDialog(
            onDismissRequest = { unbalancedBy = null },
            title = { Text(stringResource(R.string.round_unbalanced_title), color = TallyoColors.Text) },
            text = {
                Text(stringResource(R.string.round_unbalanced_message, display), color = TallyoColors.TextMuted)
            },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = { unbalancedBy = null }) {
                    Text("OK", color = TallyoColors.Primary)
                }
            },
        )
    }
}

private data class LiveStatus(
    val sum: Int,
    val emptyId: String?,
    val emptyCount: Int,
    val parseError: Boolean,
)

@Composable
private fun SimpleAlert(
    open: Boolean,
    title: Int,
    message: Int,
    onDismiss: () -> Unit,
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title), color = TallyoColors.Text) },
        text = { Text(stringResource(message), color = TallyoColors.TextMuted) },
        containerColor = TallyoColors.Surface,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = TallyoColors.Primary)
            }
        },
    )
}
