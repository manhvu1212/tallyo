package io.github.manhvu1212.tallyo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.manhvu1212.tallyo.R
import io.github.manhvu1212.tallyo.domain.PlayerStats
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.domain.SessionInsights
import io.github.manhvu1212.tallyo.domain.computeInsights
import io.github.manhvu1212.tallyo.domain.computePlayerStats
import io.github.manhvu1212.tallyo.ui.LocalAppContainer
import io.github.manhvu1212.tallyo.ui.components.EmptyState
import io.github.manhvu1212.tallyo.ui.components.TallyoCard
import io.github.manhvu1212.tallyo.ui.components.TopBar
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun StatsScreen(
    sessionId: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(container.repository, sessionId))
    val session by vm.session.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(TallyoColors.Bg)) {
        TopBar(title = stringResource(R.string.stats_title), onBack = onBack)
        val current = session
        when {
            current == null -> EmptyState(title = stringResource(R.string.detail_not_found))
            current.rounds.isEmpty() -> EmptyState(
                title = stringResource(R.string.stats_empty_title),
                subtitle = stringResource(R.string.stats_empty_message),
            )
            else -> Body(current)
        }
    }
}

@Composable
private fun Body(session: Session) {
    val stats = remember(session) { computePlayerStats(session) }
    val insights = remember(session) { computeInsights(session) }
    val ranked = remember(stats) { stats.sortedByDescending { it.totalPoints } }
    val totalRounds = session.rounds.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(label = stringResource(R.string.stats_kpi_rounds), value = totalRounds.toString(), modifier = Modifier.weight(1f))
            Kpi(
                label = stringResource(R.string.stats_kpi_exchanged),
                value = insights.totalPointsExchanged.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        TallyoCard {
            Column {
                Text(
                    stringResource(R.string.stats_patterns_title),
                    color = TallyoColors.TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Patterns(session, insights, totalRounds)
            }
        }

        TallyoCard {
            Column {
                Text(
                    stringResource(R.string.stats_table_title),
                    color = TallyoColors.TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Table(ranked)
            }
        }
    }
}

@Composable
private fun Patterns(session: Session, insights: SessionInsights, totalRounds: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (insights.leaders.isNotEmpty()) {
            val leadersText = if (insights.leaders.size == 1) {
                stringResource(R.string.stats_leader_single, insights.leaders[0].name)
            } else {
                stringResource(R.string.stats_leader_multi, insights.leaders.joinToString(", ") { it.name })
            }
            val signed = signedText(insights.leaders[0].totalPoints)
            Pattern("👑", leadersText, stringResource(R.string.stats_leader_desc, signed, totalRounds))
        }
        insights.sweepPlayer?.let { sp ->
            Pattern("🔥", stringResource(R.string.stats_sweep_title, sp.name, totalRounds), stringResource(R.string.stats_sweep_desc))
        }
        insights.biggestBlowoutRoundIndex?.let { idx ->
            Pattern("💥", stringResource(R.string.stats_blowout, idx + 1), describeRound(session, idx))
        }
        val closest = insights.closestRoundIndex
        if (closest != null && closest != insights.biggestBlowoutRoundIndex) {
            Pattern("🤝", stringResource(R.string.stats_closest, closest + 1), describeRound(session, closest))
        }
        if (insights.trailers.isNotEmpty()) {
            val trailersText = if (insights.trailers.size == 1) {
                stringResource(R.string.stats_trailer_single, insights.trailers[0].name)
            } else {
                stringResource(R.string.stats_trailer_multi, insights.trailers.joinToString(", ") { it.name })
            }
            Pattern("🥶", trailersText, stringResource(R.string.stats_trailer_desc, insights.trailers[0].totalPoints))
        }
    }
}

@Composable
private fun Pattern(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TallyoColors.SurfaceAlt)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(emoji, fontSize = 24.sp)
        Column(Modifier.weight(1f)) {
            Text(title, color = TallyoColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TallyoColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(TallyoColors.Surface)
            .border(1.dp, TallyoColors.Border, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = TallyoColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TallyoColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Table(rows: List<PlayerStats>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            Th(weight = 2f, key = R.string.stats_table_player)
            Th(weight = 1f, key = R.string.stats_table_total)
            Th(weight = 1f, key = R.string.stats_table_wins)
            Th(weight = 1f, key = R.string.stats_table_losses)
            Th(weight = 1f, key = R.string.stats_table_avg)
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(1.dp).background(TallyoColors.Border),
        )
        rows.forEachIndexed { i, p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.name,
                    color = TallyoColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f),
                )
                Td(
                    signedText(p.totalPoints),
                    weight = 1f,
                    color = when {
                        p.totalPoints > 0 -> TallyoColors.Win
                        p.totalPoints < 0 -> TallyoColors.Loss
                        else -> TallyoColors.TextMuted
                    },
                    bold = true,
                )
                Td(p.wins.toString(), weight = 1f)
                Td(p.losses.toString(), weight = 1f)
                Td(String.format(java.util.Locale.getDefault(), "%.1f", p.averagePerRound), weight = 1f)
            }
            if (i < rows.size - 1) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().height(1.dp).background(TallyoColors.Border),
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Th(
    weight: Float,
    key: Int,
) {
    Text(
        stringResource(key),
        color = TallyoColors.TextMuted,
        fontSize = 11.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Td(
    text: String,
    weight: Float,
    color: androidx.compose.ui.graphics.Color = TallyoColors.TextMuted,
    bold: Boolean = false,
) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        textAlign = TextAlign.End,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.weight(weight),
    )
}

private fun describeRound(session: Session, idx: Int): String {
    val r = session.rounds.getOrNull(idx) ?: return ""
    if (r.scores.isEmpty()) return ""
    val max = r.scores.maxOf { it.points }
    val min = r.scores.minOf { it.points }
    val topName = session.players.firstOrNull { p -> r.scores.any { it.playerId == p.id && it.points == max } }?.name ?: "?"
    val botName = session.players.firstOrNull { p -> r.scores.any { it.playerId == p.id && it.points == min } }?.name ?: "?"
    return "$topName (${signedText(max)}) vs $botName (${signedText(min)})"
}

private fun signedText(n: Int): String = if (n > 0) "+$n" else n.toString()
