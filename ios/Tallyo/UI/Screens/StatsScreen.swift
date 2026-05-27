import SwiftUI
import SwiftData

enum AiUiState {
    case idle
    case loading(aiName: String?)
    case success(content: String, aiName: String?)
    case error(message: String)
}

struct ProviderMetadata: Identifiable {
    let id: String
    let name: String
    let descKey: String
    let placeholderKey: String
    let getLinkKey: String
    let url: String
}

let PROVIDERS = [
    ProviderMetadata(
        id: "gemini",
        name: "Google Gemini",
        descKey: "ai_key_dialog_desc_gemini",
        placeholderKey: "ai_key_placeholder_gemini",
        getLinkKey: "ai_key_get_free_gemini",
        url: "https://aistudio.google.com/"
    ),
    ProviderMetadata(
        id: "groq",
        name: "Groq AI",
        descKey: "ai_key_dialog_desc_groq",
        placeholderKey: "ai_key_placeholder_groq",
        getLinkKey: "ai_key_get_free_groq",
        url: "https://console.groq.com/keys"
    ),
    ProviderMetadata(
        id: "openai",
        name: "OpenAI",
        descKey: "ai_key_dialog_desc_openai",
        placeholderKey: "ai_key_placeholder_openai",
        getLinkKey: "ai_key_get_free_openai",
        url: "https://platform.openai.com/api-keys"
    )
]

struct StatsScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    
    let sessionId: String
    let onBack: () -> Void
    
    @StateObject private var prefs = PreferencesManager.shared
    
    @State private var aiState: AiUiState = .idle
    @State private var showApiKeySheet = false
    @State private var activeStreamTask: Task<Void, Never>? = nil
    
    var body: some View {
        // Fetch session
        let descriptor = FetchDescriptor<Session>(
            predicate: #Predicate<Session> { $0.id == sessionId }
        )
        let sessions = try? modelContext.fetch(descriptor)
        let session = sessions?.first
        
        let palette = TallyoTheme.palette(for: colorScheme)
        
        let navigationContent = NavigationStack {
            ZStack {
                palette.bg.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Custom Navigation Bar
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(palette.text)
                        }
                        
                        Spacer()
                        
                        Text(NSLocalizedString("stats_title", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        Color.clear.frame(width: 24, height: 24)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    
                    if let session = session {
                        if session.rounds.isEmpty {
                            VStack(spacing: 8) {
                                Spacer()
                                Text(NSLocalizedString("stats_empty_title", comment: ""))
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(palette.text)
                                Text(NSLocalizedString("stats_empty_message", comment: ""))
                                    .font(.system(size: 13))
                                    .foregroundColor(palette.textMuted)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 24)
                                Spacer()
                            }
                        } else {
                            let stats = computePlayerStats(session: session)
                            let insights = computeInsights(session: session)
                            let ranked = stats.sorted { $0.totalPoints > $1.totalPoints }
                            let totalRounds = session.rounds.count
                            let totalPlayers = session.players.count
                            
                            let hasActiveKey = !prefs.allApiKeys.isEmpty
                            let language = getLocaleLanguage()
                            
                            ScrollView {
                                VStack(alignment: .leading, spacing: 16) {
                                    
                                    // KPI Cards
                                    HStack(spacing: 12) {
                                        KpiCard(label: NSLocalizedString("stats_kpi_rounds", comment: ""), value: "\(totalRounds)")
                                        KpiCard(label: NSLocalizedString("stats_kpi_players", comment: ""), value: "\(totalPlayers)")
                                    }
                                    
                                    // AI Insights Card
                                    VStack(alignment: .leading, spacing: 12) {
                                        HStack {
                                            HStack(spacing: 6) {
                                                Text("✨")
                                                Text(NSLocalizedString("ai_tab_insights", comment: ""))
                                                    .font(.system(size: 14, weight: .bold))
                                                    .foregroundColor(palette.text)
                                            }
                                            
                                            Spacer()
                                            
                                            if hasActiveKey {
                                                Button(action: { showApiKeySheet = true }) {
                                                    Text(NSLocalizedString("ai_key_edit_tooltip", comment: ""))
                                                        .font(.system(size: 11, weight: .semibold))
                                                        .foregroundColor(palette.primary)
                                                }
                                            }
                                        }
                                        
                                        if !hasActiveKey {
                                            VStack(spacing: 8) {
                                                Text(NSLocalizedString("ai_key_missing_desc", comment: ""))
                                                    .font(.system(size: 13))
                                                    .foregroundColor(palette.textMuted)
                                                    .multilineTextAlignment(.center)
                                                    .padding(.horizontal, 16)
                                                
                                                Button(action: { showApiKeySheet = true }) {
                                                    Text(NSLocalizedString("ai_key_configure_btn", comment: ""))
                                                        .font(.system(size: 14, weight: .bold))
                                                        .padding(.horizontal, 16)
                                                        .padding(.vertical, 8)
                                                        .background(palette.primary)
                                                        .foregroundColor(.white)
                                                        .cornerRadius(8)
                                                }
                                            }
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 4)
                                        } else {
                                            switch aiState {
                                            case .idle:
                                                Button(action: {
                                                    generateInsights(session: session, language: language)
                                                }) {
                                                    Text(NSLocalizedString("ai_btn_generate", comment: ""))
                                                        .font(.system(size: 14, weight: .bold))
                                                        .frame(maxWidth: .infinity)
                                                        .padding(.vertical, 12)
                                                        .background(palette.primary)
                                                        .foregroundColor(.white)
                                                        .cornerRadius(8)
                                                }
                                                
                                            case .loading(let aiName):
                                                HStack(spacing: 12) {
                                                    Spacer()
                                                    ProgressView()
                                                    Text(aiName != nil ?
                                                         String(format: NSLocalizedString("ai_loading_msg_named", comment: ""), aiName!) :
                                                         NSLocalizedString("ai_loading_msg", comment: ""))
                                                        .font(.system(size: 13))
                                                        .foregroundColor(palette.textMuted)
                                                    Spacer()
                                                }
                                                .padding(.vertical, 12)
                                                
                                            case .success(let content, let aiName):
                                                if content.isEmpty {
                                                    HStack(spacing: 12) {
                                                        Spacer()
                                                        ProgressView()
                                                        Text(aiName != nil ?
                                                             String(format: NSLocalizedString("ai_loading_msg_named", comment: ""), aiName!) :
                                                             NSLocalizedString("ai_loading_msg", comment: ""))
                                                            .font(.system(size: 13))
                                                            .foregroundColor(palette.textMuted)
                                                        Spacer()
                                                    }
                                                    .padding(.vertical, 12)
                                                } else {
                                                    VStack(alignment: .leading, spacing: 8) {
                                                        // Render Native Markdown AttributedString (supported in iOS 15+)
                                                        if let attributedStr = try? AttributedString(markdown: content) {
                                                            Text(attributedStr)
                                                                .font(.system(size: 14))
                                                                .lineSpacing(4)
                                                                .foregroundColor(palette.text)
                                                        } else {
                                                            Text(content)
                                                                .font(.system(size: 14))
                                                                .lineSpacing(4)
                                                                .foregroundColor(palette.text)
                                                        }
                                                        
                                                        HStack {
                                                            Spacer()
                                                            Button(action: {
                                                                generateInsights(session: session, language: language)
                                                            }) {
                                                                Text(language.hasPrefix("vi") ? "Phân tích lại" : "Regenerate")
                                                                    .font(.system(size: 12, weight: .semibold))
                                                                    .foregroundColor(palette.primary)
                                                            }
                                                        }
                                                        .padding(.top, 4)
                                                    }
                                                }
                                                
                                            case .error(let message):
                                                VStack(alignment: .leading, spacing: 8) {
                                                    HStack(alignment: .top, spacing: 10) {
                                                        Text("⚠️")
                                                        VStack(alignment: .leading, spacing: 2) {
                                                            Text(NSLocalizedString("ai_error_title", comment: ""))
                                                                .font(.system(size: 13, weight: .bold))
                                                                .foregroundColor(palette.danger)
                                                            
                                                            Text(message)
                                                                .font(.system(size: 12))
                                                                .foregroundColor(palette.text)
                                                                .lineSpacing(3)
                                                        }
                                                    }
                                                    .padding(12)
                                                    .frame(maxWidth: .infinity, alignment: .leading)
                                                    .background(palette.danger.opacity(0.08))
                                                    .cornerRadius(10)
                                                    .overlay(
                                                        RoundedRectangle(cornerRadius: 10)
                                                            .stroke(palette.danger.opacity(0.25), lineWidth: 1)
                                                    )
                                                    
                                                    Button(action: {
                                                        generateInsights(session: session, language: language)
                                                    }) {
                                                        Text(language.hasPrefix("vi") ? "Thử lại" : "Retry")
                                                            .font(.system(size: 14, weight: .bold))
                                                            .frame(maxWidth: .infinity)
                                                            .padding(.vertical, 12)
                                                            .background(palette.primary)
                                                            .foregroundColor(.white)
                                                            .cornerRadius(8)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(12)
                                    .background(palette.surface)
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(palette.border, lineWidth: 1)
                                    )
                                    
                                    // Highlights Card
                                    VStack(alignment: .leading, spacing: 12) {
                                        Text(NSLocalizedString("stats_patterns_title", comment: ""))
                                            .font(.system(size: 13))
                                            .foregroundColor(palette.textMuted)
                                            .padding(.horizontal, 4)
                                        
                                        VStack(spacing: 8) {
                                            if !insights.leaders.isEmpty {
                                                let leadersText = insights.leaders.count == 1 ?
                                                    String(format: NSLocalizedString("stats_leader_single", comment: ""), insights.leaders[0].name) :
                                                    String(format: NSLocalizedString("stats_leader_multi", comment: ""), insights.leaders.map { $0.name }.joined(separator: ", "))
                                                let signedVal = signedText(insights.leaders[0].totalPoints)
                                                PatternRow(emoji: "👑", title: leadersText, desc: String(format: NSLocalizedString("stats_leader_desc", comment: ""), signedVal, totalRounds))
                                            }
                                            
                                            if let sp = insights.sweepPlayer {
                                                PatternRow(emoji: "🔥", title: String(format: NSLocalizedString("stats_sweep_title", comment: ""), sp.name, totalRounds), desc: NSLocalizedString("stats_sweep_desc", comment: ""))
                                            }
                                            
                                            if let idx = insights.biggestBlowoutRoundIndex {
                                                PatternRow(emoji: "💥", title: String(format: NSLocalizedString("stats_blowout", comment: ""), idx + 1), desc: describeRound(session: session, idx: idx))
                                            }
                                            
                                            if let closest = insights.closestRoundIndex, closest != insights.biggestBlowoutRoundIndex {
                                                PatternRow(emoji: "🤝", title: String(format: NSLocalizedString("stats_closest", comment: ""), closest + 1), desc: describeRound(session: session, idx: closest))
                                            }
                                            
                                            if !insights.trailers.isEmpty {
                                                let trailersText = insights.trailers.count == 1 ?
                                                    String(format: NSLocalizedString("stats_trailer_single", comment: ""), insights.trailers[0].name) :
                                                    String(format: NSLocalizedString("stats_trailer_multi", comment: ""), insights.trailers.map { $0.name }.joined(separator: ", "))
                                                PatternRow(emoji: "🥶", title: trailersText, desc: String(format: NSLocalizedString("stats_trailer_desc", comment: ""), insights.trailers[0].totalPoints))
                                            }
                                        }
                                    }
                                    
                                    // Details Table Card
                                    VStack(alignment: .leading, spacing: 12) {
                                        Text(NSLocalizedString("stats_table_title", comment: ""))
                                            .font(.system(size: 13))
                                            .foregroundColor(palette.textMuted)
                                            .padding(.horizontal, 4)
                                        
                                        VStack(spacing: 0) {
                                            // Table Header
                                            HStack {
                                                Text(NSLocalizedString("stats_table_player", comment: ""))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(palette.textMuted)
                                                    .frame(maxWidth: .infinity, alignment: .leading)
                                                Text(NSLocalizedString("stats_table_total", comment: ""))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(palette.textMuted)
                                                    .frame(width: 50, alignment: .trailing)
                                                Text(NSLocalizedString("stats_table_wins", comment: ""))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(palette.textMuted)
                                                    .frame(width: 50, alignment: .trailing)
                                                Text(NSLocalizedString("stats_table_losses", comment: ""))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(palette.textMuted)
                                                    .frame(width: 50, alignment: .trailing)
                                                Text(NSLocalizedString("stats_table_avg", comment: ""))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(palette.textMuted)
                                                    .frame(width: 60, alignment: .trailing)
                                            }
                                            .padding(.bottom, 8)
                                            
                                            Divider().background(palette.border)
                                            
                                            ForEach(Array(ranked.enumerated()), id: \.element.playerId) { i, p in
                                                HStack {
                                                    Text(p.name)
                                                        .font(.system(size: 13, weight: .medium))
                                                        .foregroundColor(palette.text)
                                                        .frame(maxWidth: .infinity, alignment: .leading)
                                                        .lineLimit(1)
                                                    
                                                    Text(signedText(p.totalPoints))
                                                        .font(.system(size: 13, weight: .bold))
                                                        .foregroundColor(p.totalPoints > 0 ? palette.win : (p.totalPoints < 0 ? palette.loss : palette.textMuted))
                                                        .frame(width: 50, alignment: .trailing)
                                                    
                                                    Text("\(p.wins)")
                                                        .font(.system(size: 13))
                                                        .foregroundColor(palette.textMuted)
                                                        .frame(width: 50, alignment: .trailing)
                                                    
                                                    Text("\(p.losses)")
                                                        .font(.system(size: 13))
                                                        .foregroundColor(palette.textMuted)
                                                        .frame(width: 50, alignment: .trailing)
                                                    
                                                    Text(String(format: "%.1f", p.averagePerRound))
                                                        .font(.system(size: 13))
                                                        .foregroundColor(palette.textMuted)
                                                        .frame(width: 60, alignment: .trailing)
                                                }
                                                .padding(.vertical, 8)
                                                
                                                if i < ranked.count - 1 {
                                                    Divider().background(palette.border)
                                                }
                                            }
                                        }
                                        .padding(12)
                                        .background(palette.surface)
                                        .cornerRadius(12)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12)
                                                .stroke(palette.border, lineWidth: 1)
                                        )
                                    }
                                    
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 16)
                            }
                        }
                    } else {
                        Spacer()
                        Text(NSLocalizedString("detail_not_found", comment: ""))
                            .foregroundColor(palette.textMuted)
                        Spacer()
                    }
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showApiKeySheet) {
                ApiKeySettingsSheet()
            }
        }
        
        return AnyView(navigationContent)
    }
    
    private func getLocaleLanguage() -> String {
        return Locale.current.language.languageCode?.identifier ?? "vi"
    }
    
    private func signedText(_ n: Int) -> String {
        return "\(n > 0 ? "+" : "")\(n)"
    }
    
    private func describeRound(session: Session, idx: Int) -> String {
        guard idx < session.rounds.count else { return "" }
        let sortedRounds = session.rounds.sorted { $0.orderIndex < $1.orderIndex }
        let r = sortedRounds[idx]
        if r.scores.isEmpty { return "" }
        let points = r.scores.map { $0.points }
        guard let max = points.max(), let min = points.min() else { return "" }
        
        let topName = session.players.first { p in r.scores.contains { $0.playerId == p.id && $0.points == max } }?.name ?? "?"
        let botName = session.players.first { p in r.scores.contains { $0.playerId == p.id && $0.points == min } }?.name ?? "?"
        
        return "\(topName) (\(signedText(max))) vs \(botName) (\(signedText(min)))"
    }
    
    private func generateInsights(session: Session, language: String) {
        activeStreamTask?.cancel()
        
        aiState = .loading(aiName: nil)
        
        activeStreamTask = Task {
            let service = AiStatsService()
            do {
                let stream = service.generateInsights(
                    apiKeys: prefs.allApiKeys,
                    session: session,
                    queryType: "random",
                    language: language
                )
                
                var content = ""
                var activeName: String? = nil
                
                for try await event in stream {
                    switch event {
                    case .providerSelected(let providerName, let modelName):
                        activeName = "\(providerName) (\(modelName))"
                        content = "" // Reset accumulated text on fallback change
                        aiState = .loading(aiName: activeName)
                    case .textChunk(let text):
                        content += text
                        // Strip reasoning and planning tags on-the-fly
                        let cleanText = stripThinkingProcess(text: content)
                        aiState = .success(content: cleanText, aiName: activeName)
                    }
                }
            } catch {
                aiState = .error(message: error.localizedDescription)
            }
        }
    }
    
    private func stripThinkingProcess(text: String) -> String {
        var result = text
        
        // Remove <think>...</think> tags completely
        while true {
            guard let startRange = result.range(of: "<think>") else { break }
            if let endRange = result.range(of: "</think>", range: startRange.upperBound..<result.endIndex) {
                result.removeSubrange(startRange.lowerBound..<endRange.upperBound)
            } else {
                result = String(result[..<startRange.lowerBound])
                break
            }
        }
        
        // Match specific headers to strip thoughts before them
        let headers = [
            "📝 Tóm tắt nhanh:", "📝 Quick Summary:",
            "🧠 Phân tích chiến thuật:", "🧠 Tactical Breakdown:",
            "🔥 Chế độ Cà khịa:", "🔥 Roast Mode:",
            "✍️ Áng thơ bất hủ:", "✍️ Legendary Rhymes:",
            "🎙️ Bình luận viên:", "🎙️ Live Commentator:",
            "🦉 Góc triết học:", "🦉 Philosophical Corner:",
            "👽 Thuyết âm mưu:", "👽 Conspiracy Theory:",
            "🛋️ Bác sĩ tâm lý:", "🛋️ Therapist's Couch:",
            "📊 Nhà thống kê:", "📊 Statistician's Log:",
            "🏴‍☠️ Thuyền trưởng Hải tặc:", "🏴‍☠️ Pirate Captain:",
            "📣 Cổ động viên:", "📣 Cheerleader's Hype:"
        ]
        
        var bestIndex: String.Index? = nil
        for header in headers {
            if let range = result.range(of: header, options: .backwards) {
                var start = range.lowerBound
                // Crawl back over bold asterisks if present
                while start > result.startIndex {
                    let prevCharIndex = result.index(before: start)
                    if result[prevCharIndex] == "*" {
                        start = prevCharIndex
                    } else {
                        break
                    }
                }
                if bestIndex == nil || start > bestIndex! {
                    bestIndex = start
                }
            }
        }
        
        if let idx = bestIndex {
            return String(result[idx...]).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        
        return result.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

struct KpiCard: View {
    let label: String
    let value: String
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(palette.text)
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(palette.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(palette.surface)
        .cornerRadius(14)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(palette.border, lineWidth: 1)
        )
    }
}

struct PatternRow: View {
    let emoji: String
    let title: String
    let desc: String
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        HStack(spacing: 12) {
            Text(emoji)
                .font(.system(size: 24))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(palette.text)
                Text(desc)
                    .font(.system(size: 13))
                    .foregroundColor(palette.textMuted)
            }
            Spacer()
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(palette.surfaceAlt)
        .cornerRadius(10)
    }
}

struct ApiKeySettingsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var prefs = PreferencesManager.shared
    
    @State private var keys: [String: String] = [:]
    @State private var expandedProviderId: String? = "gemini"
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        NavigationStack {
            ZStack {
                palette.bg.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Header
                    HStack {
                        Button(NSLocalizedString("common_cancel", comment: "")) {
                            dismiss()
                        }
                        .foregroundColor(palette.textMuted)
                        
                        Spacer()
                        
                        Text(NSLocalizedString("ai_key_dialog_title", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        Button(NSLocalizedString("ai_key_save", comment: "")) {
                            save()
                        }
                        .foregroundColor(palette.primary)
                        .font(.system(size: 16, weight: .bold))
                    }
                    .padding(16)
                    .background(palette.surface)
                    
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(PROVIDERS) { provider in
                                let isExpanded = expandedProviderId == provider.id
                                let currentVal = keys[provider.id] ?? ""
                                let hasKey = !currentVal.isEmpty
                                
                                VStack(alignment: .leading, spacing: 0) {
                                    // Accordion Header Row
                                    Button(action: {
                                        expandedProviderId = isExpanded ? nil : provider.id
                                    }) {
                                        HStack(spacing: 8) {
                                            Text(provider.name)
                                                .font(.system(size: 14, weight: .bold))
                                                .foregroundColor(palette.text)
                                            
                                            let statusText = hasKey ?
                                                NSLocalizedString("ai_provider_status_configured", comment: "") :
                                                NSLocalizedString("ai_provider_status_not_configured", comment: "")
                                            
                                            Text(statusText)
                                                .font(.system(size: 10, weight: .semibold))
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(hasKey ? palette.win.opacity(0.12) : palette.textMuted.opacity(0.1))
                                                .foregroundColor(hasKey ? palette.win : palette.textMuted)
                                                .cornerRadius(6)
                                            
                                            Spacer()
                                            
                                            Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                                                .font(.system(size: 12))
                                                .foregroundColor(palette.textMuted)
                                        }
                                        .padding(14)
                                    }
                                    .buttonStyle(.plain)
                                    
                                    // Expanded content
                                    if isExpanded {
                                        VStack(alignment: .leading, spacing: 12) {
                                            Text(NSLocalizedString(provider.descKey, comment: ""))
                                                .font(.system(size: 12))
                                                .foregroundColor(palette.textMuted)
                                                .lineSpacing(4)
                                            
                                            TextField(NSLocalizedString(provider.placeholderKey, comment: ""), text: Binding(
                                                get: { keys[provider.id] ?? "" },
                                                set: { keys[provider.id] = $0 }
                                            ))
                                            .padding(10)
                                            .background(palette.surface)
                                            .foregroundColor(palette.text)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(palette.border, lineWidth: 1)
                                            )
                                            
                                            HStack {
                                                Link(NSLocalizedString(provider.getLinkKey, comment: ""), destination: URL(string: provider.url)!)
                                                    .font(.system(size: 12, weight: .semibold))
                                                    .foregroundColor(palette.primary)
                                                
                                                Spacer()
                                                
                                                if hasKey {
                                                    Button(NSLocalizedString("ai_key_clear", comment: "")) {
                                                        keys[provider.id] = ""
                                                    }
                                                    .font(.system(size: 12, weight: .bold))
                                                    .foregroundColor(palette.danger)
                                                }
                                            }
                                        }
                                        .padding(.horizontal, 14)
                                        .padding(.bottom, 14)
                                    }
                                }
                                .background(palette.surfaceAlt)
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(isExpanded ? palette.primary : Color.clear, lineWidth: 1)
                                )
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationBarHidden(true)
            .onAppear {
                for provider in PROVIDERS {
                    keys[provider.id] = prefs.getApiKey(providerId: provider.id) ?? ""
                }
            }
        }
    }
    
    private func save() {
        for provider in PROVIDERS {
            if let val = keys[provider.id], !val.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                prefs.saveApiKey(providerId: provider.id, key: val.trimmingCharacters(in: .whitespacesAndNewlines))
            } else {
                prefs.clearApiKey(providerId: provider.id)
            }
        }
        dismiss()
    }
}
