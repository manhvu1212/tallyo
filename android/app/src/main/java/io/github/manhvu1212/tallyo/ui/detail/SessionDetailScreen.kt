package io.github.manhvu1212.tallyo.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.manhvu1212.tallyo.R
import io.github.manhvu1212.tallyo.domain.Round
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.domain.computePlayerStats
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.components.EmptyState
import io.github.manhvu1212.tallyo.ui.components.Ime
import io.github.manhvu1212.tallyo.ui.components.SwipeAction
import io.github.manhvu1212.tallyo.ui.components.SwipeRevealHostScope
import io.github.manhvu1212.tallyo.ui.components.SwipeRevealRow
import io.github.manhvu1212.tallyo.ui.components.PrimaryButton
import io.github.manhvu1212.tallyo.ui.components.TallyoCard
import io.github.manhvu1212.tallyo.ui.components.TallyoFab
import io.github.manhvu1212.tallyo.ui.components.TallyoTextField
import io.github.manhvu1212.tallyo.ui.components.TopBar
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onAddRound: (roundId: String?) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModel.factory(container.repository, sessionId),
    )
    val session by vm.session.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var deleteRoundTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var duplicateName by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val addFocus = remember { FocusRequester() }

    LaunchedEffect(adding) {
        if (adding) addFocus.requestFocus()
    }

    SwipeRevealHostScope(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = session?.name ?: stringResource(R.string.detail_title_fallback),
                onBack = onBack,
                trailing = if (session != null) {
                    {
                        TextButton(onClick = onOpenStats) {
                            Text(stringResource(R.string.detail_stats), color = TallyoColors.Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else null,
            )

            val current = session
            if (current == null) {
                EmptyState(title = stringResource(R.string.detail_not_found))
            } else {
                val stats = remember(current) { computePlayerStats(current) }
                val ranked = remember(stats) { stats.sortedByDescending { it.totalPoints } }
                val roundsNewestFirst = remember(current) {
                    current.rounds.mapIndexed { i, r -> r to i }.reversed()
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        TallyoCard(padding = 12.dp) {
                            Column(Modifier.animateContentSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.detail_board_title),
                                        color = TallyoColors.TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (!adding) {
                                        TextButton(
                                            onClick = { adding = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        ) {
                                            Text(
                                                stringResource(R.string.detail_add_player_cta),
                                                color = TallyoColors.Primary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ranked.forEachIndexed { i, p ->
                                        val resting = current.players.firstOrNull { it.id == p.playerId }?.resting == true
                                        val lead = i == 0 && current.rounds.isNotEmpty()
                                        val toggleLabel = if (resting)
                                            stringResource(R.string.detail_menu_resume)
                                        else
                                            stringResource(R.string.detail_menu_rest)
                                        SwipeRevealRow(
                                            action = SwipeAction(
                                                icon = if (resting) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                                contentDescription = toggleLabel,
                                                onClick = { vm.setPlayerResting(p.playerId, !resting) },
                                            ),
                                        ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(TallyoColors.Surface)
                                                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(if (lead) TallyoColors.Primary else TallyoColors.SurfaceAlt),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("${i + 1}", color = TallyoColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.size(12.dp))
                                            Text(
                                                p.name,
                                                color = TallyoColors.Text,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (resting) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(TallyoColors.SurfaceAlt)
                                                        .border(1.dp, TallyoColors.Border, RoundedCornerShape(999.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        stringResource(R.string.detail_resting),
                                                        color = TallyoColors.TextMuted,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                                Spacer(Modifier.size(8.dp))
                                            }
                                            Text(
                                                signed(p.totalPoints),
                                                color = when {
                                                    p.totalPoints > 0 -> TallyoColors.Win
                                                    p.totalPoints < 0 -> TallyoColors.Loss
                                                    else -> TallyoColors.Text
                                                },
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        }
                                    }
                                }

                                if (adding) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                adding = false
                                                newName = ""
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.common_cancel),
                                                tint = TallyoColors.TextMuted,
                                            )
                                        }
                                        TallyoTextField(
                                            value = newName,
                                            onValueChange = { newName = it },
                                            placeholder = stringResource(R.string.detail_add_player_placeholder),
                                            keyboardOptions = Ime.DoneWords,
                                            keyboardActions = KeyboardActions(onDone = {
                                                submitNewPlayer(
                                                    newName,
                                                    current,
                                                    onDuplicate = { duplicateName = newName.trim() },
                                                    onSuccess = {
                                                        vm.addPlayer(newName) { duplicateName = newName.trim() }
                                                        newName = ""
                                                        adding = false
                                                    },
                                                    onEmpty = { adding = false; newName = "" },
                                                )
                                            }),
                                            focusRequester = addFocus,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        PrimaryButton(
                                            label = stringResource(R.string.detail_add_player_submit),
                                            onClick = {
                                                submitNewPlayer(
                                                    newName,
                                                    current,
                                                    onDuplicate = { duplicateName = newName.trim() },
                                                    onSuccess = {
                                                        vm.addPlayer(newName) { duplicateName = newName.trim() }
                                                        newName = ""
                                                        adding = false
                                                    },
                                                    onEmpty = { adding = false; newName = "" },
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (current.rounds.isEmpty())
                                stringResource(R.string.detail_rounds_empty)
                            else
                                pluralStringResource(
                                    R.plurals.detail_rounds_count,
                                    current.rounds.size,
                                    current.rounds.size,
                                ),
                            color = TallyoColors.TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }

                    if (current.rounds.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.detail_first_round_hint),
                                color = TallyoColors.TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(items = roundsNewestFirst, key = { it.first.id }) { (round, idx) ->
                            RoundCard(
                                round = round,
                                idx = idx,
                                session = current,
                                onOpen = { onAddRound(round.id) },
                                onRequestDelete = { deleteRoundTarget = round.id to idx },
                            )
                        }
                    }
                }
            }
        }

        if (session != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp),
            ) {
                TallyoFab(label = stringResource(R.string.detail_add_round_fab), onClick = { onAddRound(null) })
            }
        }
    }
    }

    deleteRoundTarget?.let { (rid, idx) ->
        AlertDialog(
            onDismissRequest = { deleteRoundTarget = null },
            title = { Text(stringResource(R.string.detail_round_idx, idx + 1), color = TallyoColors.Text) },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRound(rid)
                    deleteRoundTarget = null
                }) {
                    Text(stringResource(R.string.detail_menu_delete_round), color = TallyoColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRoundTarget = null }) {
                    Text(stringResource(R.string.common_cancel), color = TallyoColors.TextMuted)
                }
            },
        )
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
}

private fun submitNewPlayer(
    name: String,
    session: Session,
    onDuplicate: () -> Unit,
    onSuccess: () -> Unit,
    onEmpty: () -> Unit,
) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
        onEmpty()
        return
    }
    if (session.players.any { it.name == trimmed }) {
        onDuplicate()
        return
    }
    onSuccess()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundCard(
    round: Round,
    idx: Int,
    session: Session,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    SwipeRevealRow(
        action = SwipeAction(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.detail_menu_delete_round),
            destructive = true,
            onClick = onRequestDelete,
        ),
    ) {
    TallyoCard(onClick = onOpen, padding = 10.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.detail_round_idx, idx + 1),
                    color = TallyoColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!round.note.isNullOrBlank()) {
                    Text(
                        round.note,
                        color = TallyoColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                round.scores.forEach { s ->
                    val player = session.players.firstOrNull { it.id == s.playerId } ?: return@forEach
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TallyoColors.SurfaceAlt)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(player.name, color = TallyoColors.TextMuted, fontSize = 12.sp, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            signed(s.points),
                            color = when {
                                s.points > 0 -> TallyoColors.Win
                                s.points < 0 -> TallyoColors.Loss
                                else -> TallyoColors.Text
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
    }
}

private fun signed(n: Int): String = if (n > 0) "+$n" else n.toString()
