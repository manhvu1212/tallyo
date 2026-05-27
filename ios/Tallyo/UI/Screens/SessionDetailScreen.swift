import SwiftUI
import SwiftData

struct SessionDetailScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    
    let sessionId: String
    let onBack: () -> Void
    let onOpenStats: () -> Void
    let onAddRound: (String?) -> Void
    
    @State private var addingPlayer = false
    @State private var newPlayerName = ""
    @State private var duplicateName: String? = nil
    @State private var showDuplicateAlert = false
    
    @State private var deleteRoundTarget: (id: String, index: Int)?
    @State private var showDeleteRoundAlert = false
    
    @State private var showQuickScoreSheet = false
    @State private var editingQuickScoreItem: EventDisplayItem? = nil
    
    var body: some View {
        // Query the session details
        let descriptor = FetchDescriptor<Session>(
            predicate: #Predicate<Session> { $0.id == sessionId }
        )
        let sessions = try? modelContext.fetch(descriptor)
        let session = sessions?.first
        
        let palette = TallyoTheme.palette(for: colorScheme)
        
        ZStack {
            palette.bg.ignoresSafeArea()
            
            if let session = session {
                let stats = computePlayerStats(session: session)
                let rankedStats = stats.sorted { $0.totalPoints > $1.totalPoints }
                let sortedRounds = session.rounds.sorted { $0.orderIndex < $1.orderIndex }
                let roundsNewestFirst = sortedRounds.enumerated().map { ($0.element, $0.offset) }.reversed()
                let pendingEvents = session.events.filter { $0.roundId == nil }
                let activePlayers = session.players.filter { !$0.resting }
                
                VStack(spacing: 0) {
                    // Custom navigation bar
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(palette.text)
                        }
                        
                        Spacer()
                        
                        VStack(spacing: 2) {
                            Text(session.name)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(palette.text)
                                .lineLimit(1)
                            
                            if !session.game.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                Text(session.game)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(palette.primary)
                            }
                        }
                        
                        Spacer()
                        
                        Button(action: onOpenStats) {
                            Text(NSLocalizedString("detail_stats", comment: ""))
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(palette.primary)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            
                            // 1. Scoreboard Card
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Text(NSLocalizedString("detail_board_title", comment: ""))
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(palette.textMuted)
                                    
                                    Spacer()
                                    
                                    if !addingPlayer {
                                        Button(action: { addingPlayer = true }) {
                                            Text(NSLocalizedString("detail_add_player_cta", comment: ""))
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(palette.primary)
                                        }
                                    }
                                }
                                
                                // Ranked Players List
                                VStack(spacing: 4) {
                                    ForEach(Array(rankedStats.enumerated()), id: \.element.id) { index, playerStat in
                                        let player = session.players.first { $0.id == playerStat.playerId }
                                        let resting = player?.resting ?? false
                                        let isLeader = index == 0 && !session.rounds.isEmpty
                                        
                                        HStack {
                                            // Rank circle
                                            ZStack {
                                                Circle()
                                                    .fill(isLeader ? palette.primary : palette.surfaceAlt)
                                                    .frame(width: 26, height: 26)
                                                
                                                Text("\(index + 1)")
                                                    .font(.system(size: 13, weight: .bold))
                                                    .foregroundColor(isLeader ? .white : palette.text)
                                            }
                                            
                                            Text(playerStat.name)
                                                .font(.system(size: 16, weight: .medium))
                                                .foregroundColor(palette.text)
                                                .lineLimit(1)
                                            
                                            Spacer()
                                            
                                            if resting {
                                                Text(NSLocalizedString("detail_resting", comment: ""))
                                                    .font(.system(size: 11, weight: .semibold))
                                                    .foregroundColor(palette.textMuted)
                                                    .padding(.horizontal, 8)
                                                    .padding(.vertical, 2)
                                                    .background(palette.surfaceAlt)
                                                    .cornerRadius(999)
                                                    .overlay(
                                                        RoundedRectangle(cornerRadius: 999)
                                                            .stroke(palette.border, lineWidth: 1)
                                                    )
                                            }
                                            
                                            Text("\(playerStat.totalPoints > 0 ? "+" : "")\(playerStat.totalPoints)")
                                                .font(.system(size: 18, weight: .bold))
                                                .foregroundColor(playerStat.totalPoints > 0 ? palette.win : (playerStat.totalPoints < 0 ? palette.loss : palette.text))
                                        }
                                        .padding(.vertical, 6)
                                        .padding(.horizontal, 8)
                                        .background(palette.surface)
                                        .cornerRadius(8)
                                        .contextMenu {
                                            Button {
                                                togglePlayerResting(session: session, playerId: playerStat.playerId, currentResting: resting)
                                            } label: {
                                                Label(
                                                    resting ? NSLocalizedString("detail_menu_resume", comment: "") : NSLocalizedString("detail_menu_rest", comment: ""),
                                                    systemImage: resting ? "play.fill" : "pause.fill"
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Inline Add Player Textfield
                                if addingPlayer {
                                    HStack {
                                        Button(action: {
                                            addingPlayer = false
                                            newPlayerName = ""
                                        }) {
                                            Image(systemName: "xmark")
                                                .foregroundColor(palette.textMuted)
                                                .padding(8)
                                        }
                                        
                                        TextField(NSLocalizedString("detail_add_player_placeholder", comment: ""), text: $newPlayerName)
                                            .padding(10)
                                            .background(palette.surfaceAlt)
                                            .foregroundColor(palette.text)
                                            .cornerRadius(8)
                                            .textInputAutocapitalization(.words)
                                            .onSubmit {
                                                submitPlayer(session: session)
                                            }
                                        
                                        Button(action: {
                                            submitPlayer(session: session)
                                        }) {
                                            Text(NSLocalizedString("detail_add_player_submit", comment: ""))
                                                .font(.system(size: 13, weight: .bold))
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 8)
                                                .background(palette.primary)
                                                .foregroundColor(.white)
                                                .cornerRadius(8)
                                        }
                                    }
                                    .padding(.top, 8)
                                }
                            }
                            .padding(12)
                            .background(palette.surface)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(palette.border, lineWidth: 1)
                            )
                            
                            // Rounds Header
                            Text(session.rounds.isEmpty ?
                                 NSLocalizedString("detail_rounds_empty", comment: "") :
                                 String(format: session.rounds.count == 1 ?
                                        NSLocalizedString("detail_rounds_count_one", comment: "") :
                                        NSLocalizedString("detail_rounds_count_other", comment: ""), session.rounds.count))
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(palette.textMuted)
                                .padding(.horizontal, 4)
                            
                            // 2. Draft Card / Pending Events
                            if !pendingEvents.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack(spacing: 8) {
                                        Text("DRAFT")
                                            .font(.system(size: 9, weight: .bold))
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(palette.primaryTintBg)
                                            .foregroundColor(palette.primary)
                                            .cornerRadius(4)
                                        
                                        Text(NSLocalizedString("detail_pending_events_title", comment: ""))
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(palette.text)
                                        
                                        Spacer()
                                    }
                                    
                                    Text(NSLocalizedString("detail_pending_events_desc", comment: ""))
                                        .font(.system(size: 11))
                                        .foregroundColor(palette.textMuted)
                                        .lineSpacing(2)
                                    
                                    let groupedPendingItems = groupEvents(events: pendingEvents, players: session.players)
                                    
                                    VStack(spacing: 6) {
                                        ForEach(groupedPendingItems, id: \.id) { item in
                                            GroupedEventRow(item: item, players: session.players) {
                                                editingQuickScoreItem = item
                                            } onDelete: {
                                                deleteQuickScoreItem(item: item)
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
                            }
                            
                            // 3. Rounds List / First Round Hint
                            if session.rounds.isEmpty && pendingEvents.isEmpty {
                                HStack {
                                    Spacer()
                                    Text(NSLocalizedString("detail_first_round_hint", comment: ""))
                                        .font(.system(size: 13))
                                        .foregroundColor(palette.textMuted)
                                        .multilineTextAlignment(.center)
                                        .padding(.vertical, 24)
                                    Spacer()
                                }
                            } else {
                                ForEach(Array(roundsNewestFirst), id: \.0.id) { round, idx in
                                    RoundCardView(round: round, index: idx, session: session) {
                                        onAddRound(round.id)
                                    } onDelete: {
                                        deleteRoundTarget = (round.id, idx)
                                        showDeleteRoundAlert = true
                                    }
                                }
                            }
                            
                            Spacer().frame(height: 80) // Leave space for float buttons
                        }
                        .padding(.horizontal, 16)
                    }
                }
                
                // Floating action buttons (Quick Score, Add Round)
                VStack {
                    Spacer()
                    HStack(spacing: 12) {
                        Spacer()
                        
                        if activePlayers.count >= 2 {
                            Button(action: { showQuickScoreSheet = true }) {
                                HStack(spacing: 8) {
                                    Text("⚡")
                                    Text(NSLocalizedString("detail_quick_score_fab", comment: ""))
                                        .font(.system(size: 15, weight: .semibold))
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                                .background(palette.surfaceAlt)
                                .foregroundColor(palette.text)
                                .cornerRadius(999)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 999)
                                        .stroke(palette.border, lineWidth: 1)
                                )
                                .shadow(radius: 4, y: 2)
                            }
                        }
                        
                        Button(action: { onAddRound(nil) }) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus")
                                Text(NSLocalizedString("detail_add_round_fab", comment: ""))
                                    .font(.system(size: 15, weight: .semibold))
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 14)
                            .background(palette.primary)
                            .foregroundColor(.white)
                            .cornerRadius(999)
                            .shadow(color: palette.primary.opacity(0.4), radius: 8, x: 0, y: 4)
                        }
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 28)
                }
                
            } else {
                VStack {
                    Text(NSLocalizedString("detail_not_found", comment: ""))
                        .foregroundColor(palette.textMuted)
                    Button(action: onBack) {
                        Text("Back").foregroundColor(palette.primary)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .alert(isPresented: $showDuplicateAlert) {
            Alert(
                title: Text(NSLocalizedString("new_players_duplicate_title", comment: "")),
                message: Text(String(format: NSLocalizedString("new_players_duplicate_message", comment: ""), duplicateName ?? "")),
                dismissButton: .default(Text("OK")) { duplicateName = nil }
            )
        }
        .alert(isPresented: $showDeleteRoundAlert) {
            let idx = deleteRoundTarget?.index ?? 0
            return Alert(
                title: Text(String(format: NSLocalizedString("detail_round_idx", comment: ""), idx + 1)),
                message: Text(NSLocalizedString("sessions_delete_message", comment: "")),
                primaryButton: .destructive(Text(NSLocalizedString("detail_menu_delete_round", comment: ""))) {
                    if let target = deleteRoundTarget, let session = session {
                        // Delete round
                        if let rIndex = session.rounds.firstIndex(where: { $0.id == target.id }) {
                            let round = session.rounds[rIndex]
                            modelContext.delete(round)
                            
                            // Re-calculate orderIndex
                            let remaining = session.rounds.filter { $0.id != target.id }.sorted { $0.orderIndex < $1.orderIndex }
                            for (newIdx, r) in remaining.enumerated() {
                                r.orderIndex = newIdx
                            }
                            
                            session.updatedAt = Date()
                            try? modelContext.save()
                        }
                    }
                    deleteRoundTarget = nil
                },
                secondaryButton: .cancel(Text(NSLocalizedString("common_cancel", comment: ""))) {
                    deleteRoundTarget = nil
                }
            )
        }
        .sheet(isPresented: $showQuickScoreSheet) {
            if let session = session {
                QuickScoreScreen(
                    players: session.players.filter { !$0.resting },
                    isZeroSum: session.zeroSum,
                    onSaveIndividual: { scoresMap, note in
                        for (playerId, points) in scoresMap {
                            let event = RoundEvent(sessionId: session.id, playerId: playerId, points: points, note: note)
                            session.events.append(event)
                        }
                        session.updatedAt = Date()
                        try? modelContext.save()
                        showQuickScoreSheet = false
                    },
                    onSaveTransfer: { fromId, toId, points, note in
                        let fromPlayerName = session.players.first { $0.id == fromId }?.name ?? ""
                        let toPlayerName = session.players.first { $0.id == toId }?.name ?? ""
                        
                        let noteForTo = note.isEmpty ? "<- \(fromPlayerName)" : "\(note) (<- \(fromPlayerName))"
                        let noteForFrom = note.isEmpty ? "-> \(toPlayerName)" : "\(note) (-> \(toPlayerName))"
                        
                        let toEvent = RoundEvent(sessionId: session.id, playerId: toId, points: points, note: noteForTo)
                        let fromEvent = RoundEvent(sessionId: session.id, playerId: fromId, points: -points, note: noteForFrom)
                        
                        session.events.append(toEvent)
                        session.events.append(fromEvent)
                        session.updatedAt = Date()
                        try? modelContext.save()
                        showQuickScoreSheet = false
                    }
                )
            }
        }
        .sheet(item: $editingQuickScoreItem) { item in
            if let session = session {
                EditQuickScoreScreen(
                    item: item,
                    players: session.players.filter { !$0.resting },
                    onSaveIndividual: { playerId, points, note in
                        if case .individual(let ev) = item {
                            ev.playerId = playerId
                            ev.points = points
                            ev.note = note.isEmpty ? nil : note
                            session.updatedAt = Date()
                            try? modelContext.save()
                        }
                        editingQuickScoreItem = nil
                    },
                    onSaveTransfer: { fromId, toId, points, note in
                        if case .transfer(let fromEv, let toEv, _) = item {
                            let fromPlayerName = session.players.first { $0.id == fromId }?.name ?? ""
                            let toPlayerName = session.players.first { $0.id == toId }?.name ?? ""
                            
                            let noteForTo = note.isEmpty ? "<- \(fromPlayerName)" : "\(note) (<- \(fromPlayerName))"
                            let noteForFrom = note.isEmpty ? "-> \(toPlayerName)" : "\(note) (-> \(toPlayerName))"
                            
                            toEv.playerId = toId
                            toEv.points = points
                            toEv.note = noteForTo
                            
                            fromEv.playerId = fromId
                            fromEv.points = -points
                            fromEv.note = noteForFrom
                            
                            session.updatedAt = Date()
                            try? modelContext.save()
                        }
                        editingQuickScoreItem = nil
                    }
                )
            }
        }
    }
    
    private func togglePlayerResting(session: Session, playerId: String, currentResting: Bool) {
        if let player = session.players.first(where: { $0.id == playerId }) {
            player.resting = !currentResting
            session.updatedAt = Date()
            try? modelContext.save()
        }
    }
    
    private func submitPlayer(session: Session) {
        let trimmed = newPlayerName.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            addingPlayer = false
            return
        }
        if session.players.contains(where: { $0.name.lowercased() == trimmed.lowercased() }) {
            duplicateName = trimmed
            showDuplicateAlert = true
            return
        }
        
        let newOrder = session.players.count
        let p = Player(name: trimmed, orderIndex: newOrder)
        session.players.append(p)
        session.updatedAt = Date()
        try? modelContext.save()
        
        newPlayerName = ""
        addingPlayer = false
    }
    
    private func deleteQuickScoreItem(item: EventDisplayItem) {
        guard let session = try? modelContext.fetch(FetchDescriptor<Session>(predicate: #Predicate<Session> { $0.id == sessionId })).first else { return }
        switch item {
        case .individual(let event):
            modelContext.delete(event)
        case .transfer(let fromEvent, let toEvent, _):
            modelContext.delete(fromEvent)
            modelContext.delete(toEvent)
        }
        session.updatedAt = Date()
        try? modelContext.save()
    }
}

struct GroupedEventRow: View {
    let item: EventDisplayItem
    let players: [Player]
    let onEdit: () -> Void
    let onDelete: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        Button(action: onEdit) {
            HStack(spacing: 8) {
                switch item {
                case .individual(let event):
                    let player = players.first { $0.id == event.playerId }
                    Text(player?.name ?? "")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(palette.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    if let note = event.note, !note.isEmpty {
                        Text(note)
                            .font(.system(size: 12))
                            .foregroundColor(palette.textMuted)
                    }
                    
                    Text("\(event.points > 0 ? "+" : "")\(event.points)")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(event.points > 0 ? palette.win : palette.loss)
                    
                case .transfer(let fromEvent, let toEvent, let baseNote):
                    let fromPlayer = players.first { $0.id == fromEvent.playerId }
                    let toPlayer = players.first { $0.id == toEvent.playerId }
                    
                    Text("\(fromPlayer?.name ?? "") → \(toPlayer?.name ?? "")")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(palette.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    if !baseNote.isEmpty {
                        Text(baseNote)
                            .font(.system(size: 12))
                            .foregroundColor(palette.textMuted)
                    }
                    
                    Text("\(toEvent.points > 0 ? "+" : "")\(toEvent.points)")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(palette.primary)
                }
                
                Button(action: onDelete) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(palette.textMuted)
                        .padding(4)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(palette.surfaceAlt)
            .cornerRadius(6)
        }
        .buttonStyle(.plain)
    }
}

struct RoundCardView: View {
    let round: Round
    let index: Int
    let session: Session
    let onOpen: () -> Void
    let onDelete: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(String(format: NSLocalizedString("detail_round_idx", comment: ""), index + 1))
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(palette.text)
                    
                    Spacer()
                    
                    if let note = round.note, !note.isEmpty {
                        Text(note)
                            .font(.system(size: 11))
                            .foregroundColor(palette.textMuted)
                            .lineLimit(1)
                    }
                }
                
                // Grid/Flow row of round scores
                FlowLayout(spacing: 6) {
                    ForEach(round.scores.sorted { s1, s2 in
                        let p1 = session.players.first { $0.id == s1.playerId }?.orderIndex ?? 0
                        let p2 = session.players.first { $0.id == s2.playerId }?.orderIndex ?? 0
                        return p1 < p2
                    }) { score in
                        if score.points != 0 {
                            let player = session.players.first { $0.id == score.playerId }
                            HStack(spacing: 6) {
                                Text(player?.name ?? "")
                                    .font(.system(size: 12))
                                    .foregroundColor(palette.textMuted)
                                
                                Text("\(score.points > 0 ? "+" : "")\(score.points)")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(score.points > 0 ? palette.win : palette.loss)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(palette.surfaceAlt)
                            .cornerRadius(6)
                        }
                    }
                }
                
                // Show events inside round
                if !round.events.isEmpty {
                    Divider()
                        .background(palette.border)
                        .padding(.vertical, 4)
                    
                    let groupedEvents = groupEvents(events: round.events, players: session.players)
                    
                    VStack(spacing: 4) {
                        ForEach(groupedEvents, id: \.id) { item in
                            HStack {
                                switch item {
                                case .individual(let event):
                                    let player = session.players.first { $0.id == event.playerId }
                                    Text("⚡ \(player?.name ?? "")")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(palette.textMuted)
                                    
                                    Spacer()
                                    
                                    if let note = event.note, !note.isEmpty {
                                        Text(note)
                                            .font(.system(size: 11))
                                            .foregroundColor(palette.textMuted)
                                            .padding(.trailing, 8)
                                    }
                                    
                                    Text("\(event.points > 0 ? "+" : "")\(event.points)")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(event.points > 0 ? palette.win : palette.loss)
                                    
                                case .transfer(let fromEvent, let toEvent, let baseNote):
                                    let fromPlayer = session.players.first { $0.id == fromEvent.playerId }
                                    let toPlayer = session.players.first { $0.id == toEvent.playerId }
                                    
                                    Text("⚡ \(fromPlayer?.name ?? "") → \(toPlayer?.name ?? "")")
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(palette.textMuted)
                                    
                                    Spacer()
                                    
                                    if !baseNote.isEmpty {
                                        Text(baseNote)
                                            .font(.system(size: 11))
                                            .foregroundColor(palette.textMuted)
                                            .padding(.trailing, 8)
                                    }
                                    
                                    Text("\(toEvent.points > 0 ? "+" : "")\(toEvent.points)")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(palette.primary)
                                }
                            }
                        }
                    }
                }
            }
            .padding(10)
            .background(palette.surface)
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(palette.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button(role: .destructive, action: onDelete) {
                Label(NSLocalizedString("detail_menu_delete_round", comment: ""), systemImage: "trash")
            }
        }
    }
}

extension EventDisplayItem: Identifiable {}
