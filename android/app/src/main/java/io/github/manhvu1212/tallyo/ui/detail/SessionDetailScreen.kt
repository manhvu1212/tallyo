package io.github.manhvu1212.tallyo.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
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
    var showQuickScoreDialog by remember { mutableStateOf(false) }
    val addFocus = remember { FocusRequester() }

    LaunchedEffect(adding) {
        if (adding) addFocus.requestFocus()
    }

    SwipeRevealHostScope(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                onBack = onBack,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = session?.name ?: stringResource(R.string.detail_title_fallback),
                            color = TallyoColors.Text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val game = session?.game.orEmpty()
                        if (game.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(TallyoColors.PrimaryTintBg)
                                    .border(
                                        1.dp,
                                        TallyoColors.Primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = game,
                                    color = TallyoColors.Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
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

                    val pendingEvents = current.pendingEvents
                    if (pendingEvents.isNotEmpty()) {
                        item {
                            TallyoCard(padding = 12.dp) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TallyoColors.PrimaryTintBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "DRAFT",
                                                color = TallyoColors.Primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.detail_pending_events_title),
                                            color = TallyoColors.Text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        stringResource(R.string.detail_pending_events_desc),
                                        color = TallyoColors.TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        pendingEvents.forEach { pe ->
                                            val player = current.players.firstOrNull { it.id == pe.playerId }
                                            if (player != null) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(TallyoColors.SurfaceAlt)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        player.name,
                                                        color = TallyoColors.Text,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (!pe.note.isNullOrBlank()) {
                                                        Text(
                                                            pe.note,
                                                            color = TallyoColors.TextMuted,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                    }
                                                    Text(
                                                        signed(pe.points),
                                                        color = if (pe.points > 0) TallyoColors.Win else TallyoColors.Loss,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = { vm.deleteQuickScore(pe.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Close,
                                                            contentDescription = "Delete",
                                                            tint = TallyoColors.TextMuted,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (current.rounds.isEmpty() && pendingEvents.isEmpty()) {
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
            val activePlayers = session?.players?.filter { !it.resting } ?: emptyList()
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activePlayers.size >= 2) {
                    TallyoFab(
                        label = stringResource(R.string.detail_quick_score_fab),
                        icon = "⚡",
                        onClick = { showQuickScoreDialog = true }
                    )
                }
                TallyoFab(
                    label = stringResource(R.string.detail_add_round_fab),
                    icon = "+",
                    onClick = { onAddRound(null) }
                )
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

    if (showQuickScoreDialog) {
        val activePlayers = session?.players?.filter { !it.resting } ?: emptyList()
        QuickScoreDialog(
            players = activePlayers,
            onDismiss = { showQuickScoreDialog = false },
            onSaveIndividual = { scoresMap, noteText ->
                scoresMap.forEach { (pid, pts) ->
                    vm.addQuickScore(pid, pts, noteText)
                }
            },
            onSaveTransfer = { from, to, pts, noteText ->
                val fromPlayerName = session?.players?.firstOrNull { it.id == from }?.name.orEmpty()
                val toPlayerName = session?.players?.firstOrNull { it.id == to }?.name.orEmpty()
                val noteForTo = if (noteText.isBlank()) "<- $fromPlayerName" else "$noteText (<- $fromPlayerName)"
                val noteForFrom = if (noteText.isBlank()) "-> $toPlayerName" else "$noteText (-> $toPlayerName)"
                
                vm.addQuickScore(to, pts, noteForTo)
                vm.addQuickScore(from, -pts, noteForFrom)
            }
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
                    if (s.points == 0) return@forEach
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
            if (round.events.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TallyoColors.Border)
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    round.events.forEach { e ->
                        val player = session.players.firstOrNull { it.id == e.playerId } ?: return@forEach
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ ${player.name}",
                                color = TallyoColors.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (!e.note.isNullOrBlank()) {
                                Text(
                                    text = e.note,
                                    color = TallyoColors.TextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = signed(e.points),
                                color = if (e.points > 0) TallyoColors.Win else TallyoColors.Loss,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

private fun signed(n: Int): String = if (n > 0) "+$n" else n.toString()

private data class DialogDeltaBtn(val label: String, val value: Int?, val edit: Boolean = false)

private val DIALOG_DELTAS = listOf(
    DialogDeltaBtn("-4", -4),
    DialogDeltaBtn("-2", -2),
    DialogDeltaBtn("-1", -1),
    DialogDeltaBtn("✎", null, edit = true),
    DialogDeltaBtn("+1", 1),
    DialogDeltaBtn("+2", 2),
    DialogDeltaBtn("+4", 4),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickScoreDialog(
    players: List<io.github.manhvu1212.tallyo.domain.Player>,
    onDismiss: () -> Unit,
    onSaveIndividual: (scores: Map<String, Int>, note: String) -> Unit,
    onSaveTransfer: (fromPlayerId: String, toPlayerId: String, points: Int, note: String) -> Unit,
) {
    var isTransfer by remember { mutableStateOf(false) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var customMode by remember { mutableStateOf(false) }
    val scores = remember { mutableStateMapOf<String, String>() }
    val customFocus = remember { FocusRequester() }

    var fromPlayerId by remember { mutableStateOf<String?>(players.firstOrNull()?.id) }
    var toPlayerId by remember { mutableStateOf<String?>(players.getOrNull(1)?.id) }

    var pointsStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf(false) }
    var transferCustomMode by remember { mutableStateOf(false) }

    LaunchedEffect(activeId) { customMode = false }
    LaunchedEffect(isTransfer) { transferCustomMode = false }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_score_title), color = TallyoColors.Text) },
        containerColor = TallyoColors.Surface,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Segment selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TallyoColors.SurfaceAlt)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val typeTextIndividual = stringResource(R.string.quick_score_type_individual)
                    val typeTextTransfer = stringResource(R.string.quick_score_type_transfer)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isTransfer) TallyoColors.Surface else Color.Transparent)
                            .clickable { isTransfer = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            typeTextIndividual,
                            color = if (!isTransfer) TallyoColors.Primary else TallyoColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTransfer) TallyoColors.Surface else Color.Transparent)
                            .clickable { isTransfer = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            typeTextTransfer,
                            color = if (isTransfer) TallyoColors.Primary else TallyoColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!isTransfer) {
                    // Individual Player List - similar to Thêm Ván
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        players.forEach { p ->
                            val raw = scores[p.id] ?: ""
                            val isActive = activeId == p.id
                            val isCustom = isActive && customMode

                            val rawNum = raw.toIntOrNull()
                            val valueColor = when {
                                raw.isEmpty() -> TallyoColors.TextMuted
                                rawNum != null && rawNum > 0 -> TallyoColors.Win
                                rawNum != null && rawNum < 0 -> TallyoColors.Loss
                                else -> TallyoColors.Text
                            }

                            val displayValue = if (raw.isNotEmpty()) {
                                if (rawNum != null && rawNum > 0) "+$raw" else raw
                            } else {
                                "0"
                            }

                            Column {
                                val rowShape = RoundedCornerShape(8.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(rowShape)
                                        .background(if (isActive) TallyoColors.PrimaryTintBg else TallyoColors.SurfaceAlt)
                                        .border(
                                            1.dp,
                                            if (isActive) TallyoColors.Primary else TallyoColors.Border,
                                            rowShape,
                                        )
                                        .clickable { activeId = p.id }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        p.name,
                                        color = TallyoColors.Text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCustom) {
                                        var fieldValue by remember(p.id) {
                                            mutableStateOf(TextFieldValue(raw, TextRange(raw.length)))
                                        }
                                        BasicTextField(
                                            value = fieldValue,
                                            onValueChange = {
                                                fieldValue = it
                                                scores[p.id] = it.text
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = valueColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End,
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done,
                                            ),
                                            keyboardActions = KeyboardActions(onDone = { customMode = false }),
                                            cursorBrush = SolidColor(TallyoColors.Primary),
                                            modifier = Modifier
                                                .focusRequester(customFocus)
                                                .padding(vertical = 2.dp),
                                        )
                                        LaunchedEffect(customMode, activeId) {
                                            if (isCustom) customFocus.requestFocus()
                                        }
                                    } else {
                                        Text(
                                            displayValue,
                                            color = valueColor,
                                            fontSize = 15.sp,
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
                                        DIALOG_DELTAS.forEach { d ->
                                            val isEdit = d.edit
                                            val isNeg = d.value != null && d.value < 0
                                            val isPos = d.value != null && d.value > 0
                                            val btnShape = RoundedCornerShape(6.dp)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(btnShape)
                                                    .background(if (isEdit) TallyoColors.Primary else TallyoColors.SurfaceAlt)
                                                    .border(1.dp, if (isEdit) TallyoColors.Primary else TallyoColors.Border, btnShape)
                                                    .clickable {
                                                        if (isEdit) {
                                                            customMode = true
                                                        } else d.value?.let { delta ->
                                                            val cur = (scores[p.id] ?: "").trim().toIntOrNull() ?: 0
                                                            val newValue = cur + delta
                                                            scores[p.id] = if (newValue == 0) "" else newValue.toString()
                                                        }
                                                    }
                                                    .padding(vertical = 8.dp),
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
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Point Transfer: From -> To
                    Text(stringResource(R.string.quick_score_player_from), color = TallyoColors.TextMuted, fontSize = 12.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        players.forEach { p ->
                            val isSelected = fromPlayerId == p.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TallyoColors.PrimaryTintBg else TallyoColors.SurfaceAlt)
                                    .border(1.dp, if (isSelected) TallyoColors.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        fromPlayerId = p.id
                                        if (toPlayerId == p.id) {
                                            toPlayerId = players.firstOrNull { it.id != p.id }?.id
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    p.name,
                                    color = if (isSelected) TallyoColors.Primary else TallyoColors.Text,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Text(stringResource(R.string.quick_score_player_to), color = TallyoColors.TextMuted, fontSize = 12.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        players.filter { it.id != fromPlayerId }.forEach { p ->
                            val isSelected = toPlayerId == p.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TallyoColors.PrimaryTintBg else TallyoColors.SurfaceAlt)
                                    .border(1.dp, if (isSelected) TallyoColors.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { toPlayerId = p.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    p.name,
                                    color = if (isSelected) TallyoColors.Primary else TallyoColors.Text,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Points Input for Transfer
                    Text(stringResource(R.string.quick_score_points), color = TallyoColors.TextMuted, fontSize = 12.sp)

                    val rawTransfer = pointsStr
                    val rawTransferNum = rawTransfer.toIntOrNull()
                    val transferValueColor = when {
                        rawTransfer.isEmpty() -> TallyoColors.TextMuted
                        rawTransferNum != null && rawTransferNum > 0 -> TallyoColors.Win
                        rawTransferNum != null && rawTransferNum < 0 -> TallyoColors.Loss
                        else -> TallyoColors.Text
                    }
                    val displayTransferValue = if (rawTransfer.isNotEmpty()) {
                        if (rawTransferNum != null && rawTransferNum > 0) "+$rawTransfer" else rawTransfer
                    } else {
                        "0"
                    }

                    Column {
                        val rowShape = RoundedCornerShape(8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(TallyoColors.SurfaceAlt)
                                .border(
                                    1.dp,
                                    TallyoColors.Border,
                                    rowShape,
                                )
                                .clickable { transferCustomMode = !transferCustomMode }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.quick_score_points),
                                color = TallyoColors.Text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (transferCustomMode) {
                                var fieldValue by remember {
                                    mutableStateOf(TextFieldValue(rawTransfer, TextRange(rawTransfer.length)))
                                }
                                BasicTextField(
                                    value = fieldValue,
                                    onValueChange = {
                                        fieldValue = it
                                        pointsStr = it.text
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = transferValueColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { transferCustomMode = false }),
                                    cursorBrush = SolidColor(TallyoColors.Primary),
                                    modifier = Modifier
                                        .focusRequester(customFocus)
                                        .padding(vertical = 2.dp),
                                )
                                LaunchedEffect(transferCustomMode) {
                                    if (transferCustomMode) customFocus.requestFocus()
                                }
                            } else {
                                Text(
                                    displayTransferValue,
                                    color = transferValueColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }

                        if (!transferCustomMode) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DIALOG_DELTAS.forEach { d ->
                                    val isEdit = d.edit
                                    val isNeg = d.value != null && d.value < 0
                                    val isPos = d.value != null && d.value > 0
                                    val btnShape = RoundedCornerShape(6.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(btnShape)
                                            .background(if (isEdit) TallyoColors.Primary else TallyoColors.SurfaceAlt)
                                            .border(1.dp, if (isEdit) TallyoColors.Primary else TallyoColors.Border, btnShape)
                                            .clickable {
                                                if (isEdit) {
                                                    transferCustomMode = true
                                                } else d.value?.let { delta ->
                                                    val cur = pointsStr.trim().toIntOrNull() ?: 0
                                                    val newValue = cur + delta
                                                    pointsStr = if (newValue == 0) "" else newValue.toString()
                                                }
                                            }
                                            .padding(vertical = 8.dp),
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
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Note Input
                Text(stringResource(R.string.quick_score_note), color = TallyoColors.TextMuted, fontSize = 12.sp)
                TallyoTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "e.g. Chặt heo",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg) {
                    Text(
                        stringResource(R.string.quick_score_invalid),
                        color = TallyoColors.Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isTransfer) {
                        val scoresMap = mutableMapOf<String, Int>()
                        var hasInvalid = false
                        for ((pid, text) in scores) {
                            val trimmed = text.trim()
                            if (trimmed.isEmpty()) continue
                            val pts = trimmed.toIntOrNull()
                            if (pts == null) {
                                hasInvalid = true
                                break
                            }
                            if (pts != 0) {
                                scoresMap[pid] = pts
                            }
                        }
                        if (hasInvalid || scoresMap.isEmpty()) {
                            errorMsg = true
                            return@TextButton
                        }
                        onSaveIndividual(scoresMap, note)
                    } else {
                        val pts = pointsStr.trim().toIntOrNull()
                        val fpid = fromPlayerId
                        val tpid = toPlayerId
                        if (pts == null || pts == 0 || fpid == null || tpid == null || fpid == tpid) {
                            errorMsg = true
                            return@TextButton
                        }
                        val amount = kotlin.math.abs(pts)
                        onSaveTransfer(fpid, tpid, amount, note)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.quick_score_save), color = TallyoColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = TallyoColors.TextMuted)
            }
        }
    )
}
