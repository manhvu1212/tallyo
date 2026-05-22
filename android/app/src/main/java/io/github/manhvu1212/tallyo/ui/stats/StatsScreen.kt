package io.github.manhvu1212.tallyo.ui.stats

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import io.github.manhvu1212.tallyo.ui.components.ButtonVariant
import io.github.manhvu1212.tallyo.ui.components.EmptyState
import io.github.manhvu1212.tallyo.ui.components.PrimaryButton
import io.github.manhvu1212.tallyo.ui.components.TallyoCard
import io.github.manhvu1212.tallyo.ui.components.TallyoTextField
import io.github.manhvu1212.tallyo.ui.components.TopBar
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun StatsScreen(
    sessionId: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: StatsViewModel = viewModel(
        factory = StatsViewModel.factory(
            repository = container.repository,
            preferencesManager = container.preferencesManager,
            aiStatsService = container.aiStatsService,
            sessionId = sessionId
        )
    )
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
            else -> Body(current, vm)
        }
    }
}

@Composable
private fun Body(session: Session, vm: StatsViewModel) {
    val stats = remember(session) { computePlayerStats(session) }
    val insights = remember(session) { computeInsights(session) }
    val ranked = remember(stats) { stats.sortedByDescending { it.totalPoints } }
    val totalRounds = session.rounds.size
    val totalPlayers = session.players.size

    val allApiKeys by vm.allApiKeys.collectAsStateWithLifecycle()
    val aiUiState by vm.aiUiState.collectAsStateWithLifecycle()

    val hasActiveKey = allApiKeys.isNotEmpty()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var selectedToneId by remember { mutableStateOf("random") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLanguage = remember(context) {
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        locale?.language ?: "vi"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kpi(label = stringResource(R.string.stats_kpi_rounds), value = totalRounds.toString(), modifier = Modifier.weight(1f))
                Kpi(label = stringResource(R.string.stats_kpi_players), value = totalPlayers.toString(), modifier = Modifier.weight(1f))
            }

            // AI Insights Card
            TallyoCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header row: Title + API Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("✨", fontSize = 16.sp)
                            Text(
                                text = stringResource(R.string.ai_tab_insights),
                                color = TallyoColors.Text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (hasActiveKey) {
                            Text(
                                text = stringResource(R.string.ai_key_edit_tooltip),
                                color = TallyoColors.Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { showApiKeyDialog = true }
                            )
                        }
                    }

                    // Content based on API Key configuration and UI State
                    if (!hasActiveKey) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_key_missing_desc),
                                color = TallyoColors.TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            PrimaryButton(
                                label = stringResource(R.string.ai_key_configure_btn),
                                onClick = { showApiKeyDialog = true }
                            )
                        }
                    } else {
                        // Tone Selector Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TONES) { tone ->
                                val isSelected = selectedToneId == tone.id
                                val label = if (currentLanguage.lowercase().startsWith("vi")) tone.labelVi else tone.labelEn
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) TallyoColors.PrimaryTintBg else TallyoColors.SurfaceAlt)
                                        .border(1.dp, if (isSelected) TallyoColors.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedToneId = tone.id }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(tone.emoji, fontSize = 14.sp)
                                        Text(
                                            text = label,
                                            color = if (isSelected) TallyoColors.Primary else TallyoColors.Text,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        when (val state = aiUiState) {
                            is AiUiState.Idle -> {
                                PrimaryButton(
                                    label = stringResource(R.string.ai_btn_generate),
                                    onClick = { vm.generateAiInsights(selectedToneId, currentLanguage) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is AiUiState.Loading -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = TallyoColors.Primary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = stringResource(R.string.ai_loading_msg),
                                        color = TallyoColors.TextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            is AiUiState.Success -> {
                                if (state.content.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = TallyoColors.Primary,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Text(
                                            text = stringResource(R.string.ai_loading_msg),
                                            color = TallyoColors.TextMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = parseMarkdown(state.content),
                                            color = TallyoColors.Text,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = if (currentLanguage.lowercase().startsWith("vi")) "Phân tích lại" else "Regenerate",
                                                color = TallyoColors.Primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.clickable {
                                                    vm.generateAiInsights(selectedToneId, currentLanguage)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is AiUiState.Error -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(TallyoColors.Danger.copy(alpha = 0.08f))
                                            .border(1.dp, TallyoColors.Danger.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("⚠️", fontSize = 16.sp)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.ai_error_title),
                                                    color = TallyoColors.Danger,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = state.message,
                                                    color = TallyoColors.Text,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                    PrimaryButton(
                                        label = if (currentLanguage.lowercase().startsWith("vi")) "Thử lại" else "Retry",
                                        onClick = { vm.generateAiInsights(selectedToneId, currentLanguage) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
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

    // API Key entry dialog
    if (showApiKeyDialog) {
        val localKeys = remember(allApiKeys) {
            androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
                PROVIDERS.forEach { provider ->
                    put(provider.id, allApiKeys[provider.id] ?: "")
                }
            }
        }
        var expandedProviderId by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.ai_key_dialog_title), color = TallyoColors.Text) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PROVIDERS.forEach { provider ->
                        val isExpanded = expandedProviderId == provider.id
                        val currentKeyValue = localKeys[provider.id] ?: ""
                        val hasKey = currentKeyValue.isNotBlank()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TallyoColors.SurfaceAlt)
                                .border(
                                    width = 1.dp,
                                    color = if (isExpanded) TallyoColors.Primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedProviderId = if (isExpanded) null else provider.id
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = provider.name,
                                        color = TallyoColors.Text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val statusText = if (hasKey) {
                                        stringResource(R.string.ai_provider_status_configured)
                                    } else {
                                        stringResource(R.string.ai_provider_status_not_configured)
                                    }
                                    val statusBg = if (hasKey) TallyoColors.Win.copy(alpha = 0.12f) else TallyoColors.TextMuted.copy(alpha = 0.1f)
                                    val statusColor = if (hasKey) TallyoColors.Win else TallyoColors.TextMuted
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    color = TallyoColors.TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            // Expanded Content
                            if (isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(provider.descRes),
                                        color = TallyoColors.TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    TallyoTextField(
                                        value = currentKeyValue,
                                        onValueChange = { localKeys[provider.id] = it },
                                        placeholder = stringResource(provider.placeholderRes),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                        Text(
                                            text = stringResource(provider.getApiKeyLinkTextRes),
                                            color = TallyoColors.Primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable { uriHandler.openUri(provider.getApiKeyUrl) }
                                                .weight(1f)
                                        )

                                        if (hasKey) {
                                            Text(
                                                text = stringResource(R.string.ai_key_clear),
                                                color = TallyoColors.Danger,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    localKeys[provider.id] = ""
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    label = stringResource(R.string.ai_key_save),
                    onClick = {
                        localKeys.forEach { (id, value) ->
                            if (value.isBlank()) {
                                vm.clearApiKey(id)
                            } else {
                                vm.saveApiKey(id, value.trim())
                            }
                        }
                        showApiKeyDialog = false
                    }
                )
            },
            dismissButton = {
                PrimaryButton(
                    label = stringResource(R.string.common_cancel),
                    onClick = { showApiKeyDialog = false },
                    variant = ButtonVariant.Secondary
                )
            },
            containerColor = TallyoColors.Surface,
            shape = RoundedCornerShape(14.dp)
        )
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
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TallyoColors.Border),
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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TallyoColors.Border),
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
    color: Color = TallyoColors.TextMuted,
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

private fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                append("\n")
            }
            var trimmed = line
            var isHeader = false
            var headerLevel = 0

            // Check headers
            if (trimmed.startsWith("#")) {
                val match = Regex("^(#+)\\s+(.*)$").find(trimmed)
                if (match != null) {
                    isHeader = true
                    headerLevel = match.groupValues[1].length
                    trimmed = match.groupValues[2]
                }
            }

            // Check bullet points
            var isBullet = false
            if (!isHeader && (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• "))) {
                isBullet = true
                trimmed = "• " + trimmed.substring(2)
            }

            val spanStyle = when {
                isHeader && headerLevel == 1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                isHeader && headerLevel == 2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                isHeader -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                else -> SpanStyle()
            }

            withStyle(spanStyle) {
                // Inside the line, parse bold ** and italic *
                var i = 0
                val lineLength = trimmed.length
                while (i < lineLength) {
                    if (i + 1 < lineLength && trimmed[i] == '*' && trimmed[i + 1] == '*') {
                        val nextIndex = trimmed.indexOf("**", i + 2)
                        if (nextIndex != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(trimmed.substring(i + 2, nextIndex))
                            }
                            i = nextIndex + 2
                            continue
                        }
                    }
                    if (trimmed[i] == '*') {
                        val nextIndex = trimmed.indexOf("*", i + 1)
                        if (nextIndex != -1 && (nextIndex + 1 >= lineLength || trimmed[nextIndex + 1] != '*')) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(trimmed.substring(i + 1, nextIndex))
                            }
                            i = nextIndex + 1
                            continue
                        }
                    }
                    append(trimmed[i])
                    i++
                }
            }

        }
    }
}

private data class ToneOption(
    val id: String,
    val emoji: String,
    val labelVi: String,
    val labelEn: String
)

private data class ProviderUiMetadata(
    val id: String,
    val name: String,
    val descRes: Int,
    val placeholderRes: Int,
    val getApiKeyLinkTextRes: Int,
    val getApiKeyUrl: String
)

private val PROVIDERS = listOf(
    ProviderUiMetadata(
        id = "gemini",
        name = "Google Gemini",
        descRes = R.string.ai_key_dialog_desc_gemini,
        placeholderRes = R.string.ai_key_placeholder_gemini,
        getApiKeyLinkTextRes = R.string.ai_key_get_free_gemini,
        getApiKeyUrl = "https://aistudio.google.com/"
    ),
    ProviderUiMetadata(
        id = "groq",
        name = "Groq AI",
        descRes = R.string.ai_key_dialog_desc_groq,
        placeholderRes = R.string.ai_key_placeholder_groq,
        getApiKeyLinkTextRes = R.string.ai_key_get_free_groq,
        getApiKeyUrl = "https://console.groq.com/keys"
    ),
    ProviderUiMetadata(
        id = "openai",
        name = "OpenAI",
        descRes = R.string.ai_key_dialog_desc_openai,
        placeholderRes = R.string.ai_key_placeholder_openai,
        getApiKeyLinkTextRes = R.string.ai_key_get_free_openai,
        getApiKeyUrl = "https://platform.openai.com/api-keys"
    )
)

private val TONES = listOf(
    ToneOption("random", "🎲", "Ngẫu nhiên", "Random"),
    ToneOption("summary", "📝", "Tóm tắt", "Summary"),
    ToneOption("tactics", "🧠", "Chiến thuật", "Tactics"),
    ToneOption("roast", "🔥", "Cà khịa", "Roast"),
    ToneOption("poet", "✍️", "Thơ phú", "Rhymes"),
    ToneOption("commentator", "🎙️", "Bình luận", "Commentator"),
    ToneOption("philosopher", "🦉", "Triết học", "Philosophy"),
    ToneOption("conspiracy", "👽", "Âm mưu", "Conspiracy"),
    ToneOption("therapist", "🛋️", "Tâm lý", "Therapist"),
    ToneOption("statistician", "📊", "Thống kê", "Statistics"),
    ToneOption("pirate", "🏴‍☠️", "Hải tặc", "Pirate"),
    ToneOption("cheerleader", "📣", "Cổ vũ", "Cheerleader")
)
