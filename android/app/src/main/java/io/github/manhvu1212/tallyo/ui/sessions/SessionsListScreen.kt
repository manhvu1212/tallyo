package io.github.manhvu1212.tallyo.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.manhvu1212.tallyo.R
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.components.SwipeAction
import io.github.manhvu1212.tallyo.ui.components.SwipeRevealHostScope
import io.github.manhvu1212.tallyo.ui.components.SwipeRevealRow
import io.github.manhvu1212.tallyo.ui.components.TallyoCard
import io.github.manhvu1212.tallyo.ui.components.TallyoFab
import io.github.manhvu1212.tallyo.ui.components.rememberDateFormatter
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun SessionsListScreen(
    onOpenSession: (String) -> Unit,
    onNewSession: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: SessionsListViewModel = viewModel(factory = SessionsListViewModel.factory(container.repository))
    val sessions by vm.sessions.collectAsStateWithLifecycle()

    var deleteTarget by remember { mutableStateOf<Session?>(null) }

    SwipeRevealHostScope(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            ) {
                Text("Tallyo", color = TallyoColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.app_subtitle),
                    color = TallyoColors.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.sessions_empty_title),
                        color = TallyoColors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.sessions_empty_hint),
                        color = TallyoColors.TextMuted,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            onOpen = { onOpenSession(session.id) },
                            onRequestDelete = { deleteTarget = session },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
        ) {
            TallyoFab(label = stringResource(R.string.sessions_new_fab), onClick = onNewSession)
        }
    }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(target.name, color = TallyoColors.Text) },
            text = { Text(stringResource(R.string.sessions_delete_message), color = TallyoColors.TextMuted) },
            containerColor = TallyoColors.Surface,
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target.id)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.sessions_delete_confirm), color = TallyoColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel), color = TallyoColors.TextMuted)
                }
            },
        )
    }

}

@Composable
private fun SessionRow(
    session: Session,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val totals = remember(session) {
        val map = HashMap<String, Int>()
        for (r in session.rounds) for (s in r.scores) {
            map[s.playerId] = (map[s.playerId] ?: 0) + s.points
        }
        map
    }
    val leader = remember(session, totals) {
        session.players.maxByOrNull { totals[it.id] ?: 0 }
    }
    val leaderTotal = leader?.let { totals[it.id] ?: 0 } ?: 0
    val format = rememberDateFormatter()

    val metaParts = buildList {
        add(pluralStringResource(R.plurals.sessions_meta_people, session.players.size, session.players.size))
        add(pluralStringResource(R.plurals.sessions_meta_rounds, session.rounds.size, session.rounds.size))
        if (session.zeroSum) add(stringResource(R.string.sessions_meta_zero_sum))
    }

    SwipeRevealRow(
        action = SwipeAction(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.sessions_delete_confirm),
            destructive = true,
            onClick = onRequestDelete,
        ),
    ) {
    TallyoCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    session.name,
                    color = TallyoColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    metaParts.joinToString(" · "),
                    color = TallyoColors.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    format(session.updatedAt),
                    color = TallyoColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp).alpha(0.7f),
                )
            }
            if (leader != null && session.rounds.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 12.dp).widthIn(min = 80.dp),
                ) {
                    Text(stringResource(R.string.sessions_leader), color = TallyoColors.TextMuted, fontSize = 11.sp)
                    Text(
                        leader.name,
                        color = TallyoColors.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        (if (leaderTotal > 0) "+" else "") + leaderTotal,
                        color = TallyoColors.Accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    }
}

