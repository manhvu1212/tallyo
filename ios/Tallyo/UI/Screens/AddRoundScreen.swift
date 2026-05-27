import SwiftUI
import SwiftData

private struct DeltaBtn: Identifiable {
    let id = UUID()
    let label: String
    let value: Int?
    let edit: Bool
}

private let DELTAS = [
    DeltaBtn(label: "-4", value: -4, edit: false),
    DeltaBtn(label: "-2", value: -2, edit: false),
    DeltaBtn(label: "-1", value: -1, edit: false),
    DeltaBtn(label: "✎", value: nil, edit: true),
    DeltaBtn(label: "+1", value: 1, edit: false),
    DeltaBtn(label: "+2", value: 2, edit: false),
    DeltaBtn(label: "+4", value: 4, edit: false)
]

struct AddRoundScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    
    let sessionId: String
    let roundId: String?
    let onBack: () -> Void
    
    @State private var scores: [String: String] = [:] // playerId -> text
    @State private var note: String = ""
    @State private var activeId: String? = nil
    @State private var customMode: Bool = false
    
    @State private var showInvalidAlert = false
    @State private var showEmptyAlert = false
    @State private var showNeedMoreAlert = false
    @State private var showUnbalancedAlert = false
    @State private var unbalancedSum = 0
    
    var body: some View {
        // Query the session details
        let descriptor = FetchDescriptor<Session>(
            predicate: #Predicate<Session> { $0.id == sessionId }
        )
        let sessions = try? modelContext.fetch(descriptor)
        guard let session = sessions?.first else {
            return AnyView(
                VStack {
                    Text(NSLocalizedString("round_not_found", comment: ""))
                    Button("Back", action: onBack)
                }
            )
        }
        
        let editing = roundId.flatMap { rid in session.rounds.first { $0.id == rid } }
        
        let visiblePlayers = editing != nil ?
            session.players.filter { p in editing!.scores.contains { $0.playerId == p.id } } :
            session.players.filter { !$0.resting }
            
        let pendingEvents = session.events.filter { $0.roundId == nil }
        let zeroSum = session.zeroSum
        let palette = TallyoTheme.palette(for: colorScheme)
        
        // Compute live values
        var sum = 0
        var emptyId: String? = nil
        var emptyCount = 0
        var parseError = false
        
        for p in visiblePlayers {
            let pendingPoints = editing == nil ? pendingEvents.filter { $0.playerId == p.id }.reduce(0) { $0 + $1.points } : 0
            let raw = (scores[p.id] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if raw.isEmpty {
                if emptyId == nil { emptyId = p.id }
                emptyCount += 1
                sum += pendingPoints
                continue
            }
            if let n = Int(raw) {
                sum += (n + pendingPoints)
            } else {
                parseError = true
            }
        }
        
        let liveSum = sum
        let liveEmptyId = emptyId
        let liveEmptyCount = emptyCount
        let liveParseError = parseError
        
        let unbalanced = zeroSum && liveEmptyCount == 0 && liveSum != 0 && !liveParseError
        
        let mainContent = NavigationStack {
            ZStack {
                palette.bg.ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Top navigation bar
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(palette.text)
                        }
                        
                        Spacer()
                        
                        Text(editing != nil ? NSLocalizedString("round_title_edit", comment: "") : NSLocalizedString("round_title_new", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        Color.clear.frame(width: 24, height: 24)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            
                            // Hint
                            Text(zeroSum ? NSLocalizedString("round_hint_zero_sum", comment: "") : NSLocalizedString("round_hint_free", comment: ""))
                                .font(.system(size: 13))
                                .foregroundColor(palette.textMuted)
                                .lineSpacing(4)
                                .padding(.horizontal, 4)
                            
                            // Pending Quick Events Banner
                            if editing == nil && !pendingEvents.isEmpty {
                                let pendingSummary = buildPendingSummary(pendingEvents: pendingEvents, players: session.players)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(String(format: NSLocalizedString("add_round_pending_banner", comment: ""), pendingEvents.count, pendingSummary))
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(palette.primary)
                                        .lineSpacing(3)
                                }
                                .padding(12)
                                .background(palette.primaryTintBg)
                                .cornerRadius(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(palette.primary.opacity(0.5), lineWidth: 1)
                                )
                            }
                            
                            // Player Input Cards
                            VStack(spacing: 12) {
                                ForEach(visiblePlayers) { p in
                                    let raw = scores[p.id] ?? ""
                                    let isActive = activeId == p.id
                                    let isCustom = isActive && customMode
                                    let isAutoFilled = zeroSum && liveEmptyCount == 1 && liveEmptyId == p.id
                                    let autoVal = -liveSum
                                    let showUnbalanced = unbalanced && isActive
                                    
                                    let rawNum = Int(raw)
                                    
                                    let valueColor: Color = {
                                        if raw.isEmpty && isAutoFilled { return palette.primary }
                                        if raw.isEmpty { return palette.textMuted }
                                        if let num = rawNum, num > 0 { return palette.win }
                                        if let num = rawNum, num < 0 { return palette.loss }
                                        return palette.text
                                    }()
                                    
                                    let displayValue: String = {
                                        if !raw.isEmpty { return raw }
                                        if isAutoFilled { return "= \(autoVal > 0 ? "+" : "")\(autoVal)" }
                                        return "0"
                                    }()
                                    
                                    let pendingPoints = editing == nil ? pendingEvents.filter { $0.playerId == p.id }.reduce(0) { $0 + $1.points } : 0
                                    let baseScore = !raw.isEmpty ? (Int(raw) ?? 0) : (isAutoFilled ? autoVal : 0)
                                    let totalScore = baseScore + pendingPoints
                                    
                                    VStack(spacing: 4) {
                                        Button(action: {
                                            activeId = p.id
                                            customMode = false
                                        }) {
                                            HStack {
                                                VStack(alignment: .leading, spacing: 4) {
                                                    HStack {
                                                        Text(p.name)
                                                            .font(.system(size: 16, weight: .medium))
                                                            .foregroundColor(palette.text)
                                                        
                                                        if editing == nil && pendingPoints != 0 {
                                                            Text("⚡ \(pendingPoints > 0 ? "+" : "")\(pendingPoints)")
                                                                .font(.system(size: 11, weight: .bold))
                                                                .padding(.horizontal, 6)
                                                                .padding(.vertical, 2)
                                                                .background((pendingPoints > 0 ? palette.win : palette.loss).opacity(0.15))
                                                                .foregroundColor(pendingPoints > 0 ? palette.win : palette.loss)
                                                                .cornerRadius(4)
                                                        }
                                                    }
                                                }
                                                
                                                Spacer()
                                                
                                                VStack(alignment: .trailing, spacing: 2) {
                                                    if isCustom {
                                                        TextField("", text: Binding(
                                                            get: { scores[p.id] ?? "" },
                                                            set: { scores[p.id] = $0 }
                                                        ))
                                                        .keyboardType(.numbersAndPunctuation)
                                                        .multilineTextAlignment(.trailing)
                                                        .font(.system(size: 18, weight: .bold))
                                                        .foregroundColor(valueColor)
                                                        .frame(width: 80)
                                                    } else {
                                                        Text(displayValue)
                                                            .font(.system(size: 18, weight: .bold))
                                                            .foregroundColor(valueColor)
                                                    }
                                                    
                                                    if editing == nil && pendingPoints != 0 {
                                                        Text("\(NSLocalizedString("stats_table_total", comment: "")): \(totalScore > 0 ? "+" : "")\(totalScore)")
                                                            .font(.system(size: 12))
                                                            .foregroundColor(palette.textMuted)
                                                    }
                                                }
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 10)
                                            .background(isActive ? palette.primaryTintBg : palette.surface)
                                            .cornerRadius(10)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(showUnbalanced ? palette.danger : (isActive ? palette.primary : palette.border), lineWidth: showUnbalanced ? 2 : 1)
                                            )
                                        }
                                        .buttonStyle(.plain)
                                        
                                        // Delta controls if active
                                        if isActive && !isCustom {
                                            HStack(spacing: 4) {
                                                ForEach(DELTAS) { d in
                                                    Button(action: {
                                                        if d.edit {
                                                            customMode = true
                                                        } else if let delta = d.value {
                                                            let cur = Int(scores[p.id] ?? "") ?? 0
                                                            scores[p.id] = "\(cur + delta)"
                                                        }
                                                    }) {
                                                        Text(d.label)
                                                            .font(.system(size: 15, weight: .bold))
                                                            .foregroundColor(d.edit ? .white : (d.value != nil && d.value! < 0 ? palette.loss : (d.value != nil && d.value! > 0 ? palette.win : palette.text)))
                                                            .frame(maxWidth: .infinity)
                                                            .padding(.vertical, 10)
                                                            .background(d.edit ? palette.primary : palette.surfaceAlt)
                                                            .cornerRadius(8)
                                                            .overlay(
                                                                RoundedRectangle(cornerRadius: 8)
                                                                    .stroke(d.edit ? palette.primary : palette.border, lineWidth: 1)
                                                            )
                                                    }
                                                    .buttonStyle(.plain)
                                                }
                                            }
                                            .padding(.top, 2)
                                        }
                                    }
                                }
                            }
                            
                            // Note
                            VStack(alignment: .leading, spacing: 6) {
                                Text(NSLocalizedString("round_note_label", comment: ""))
                                    .font(.system(size: 13))
                                    .foregroundColor(palette.textMuted)
                                
                                TextField(NSLocalizedString("round_note_placeholder", comment: ""), text: $note)
                                    .padding(12)
                                    .background(palette.surface)
                                    .foregroundColor(palette.text)
                                    .cornerRadius(8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(palette.border, lineWidth: 1)
                                    )
                                    .textInputAutocapitalization(.sentences)
                            }
                            
                            // Save Button
                            Button(action: {
                                saveRound(session: session, visiblePlayers: visiblePlayers, liveSum: liveSum, liveEmptyId: liveEmptyId, liveEmptyCount: liveEmptyCount, liveParseError: liveParseError)
                            }) {
                                Text(editing != nil ? NSLocalizedString("round_save_edit", comment: "") : NSLocalizedString("round_save_new", comment: ""))
                                    .font(.system(size: 16, weight: .bold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(palette.primary)
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }
                            .padding(.top, 16)
                            .padding(.bottom, 48)
                        }
                        .padding(.horizontal, 16)
                    }
                }
            }
            .navigationBarHidden(true)
            .alert(NSLocalizedString("round_invalid_title", comment: ""), isPresented: $showInvalidAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(NSLocalizedString("round_invalid_message", comment: ""))
            }
            .alert(NSLocalizedString("round_empty_title", comment: ""), isPresented: $showEmptyAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(NSLocalizedString("round_empty_message", comment: ""))
            }
            .alert(NSLocalizedString("round_need_more_title", comment: ""), isPresented: $showNeedMoreAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(String(format: NSLocalizedString("round_need_more_message", comment: ""), visiblePlayers.count - 1))
            }
            .alert(NSLocalizedString("round_unbalanced_title", comment: ""), isPresented: $showUnbalancedAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                let display = unbalancedSum > 0 ? "+\(unbalancedSum)" : "\(unbalancedSum)"
                Text(String(format: NSLocalizedString("round_unbalanced_message", comment: ""), display))
            }
            .onAppear {
                if let editing = editing {
                    for score in editing.scores {
                        scores[score.playerId] = "\(score.points)"
                    }
                    note = editing.note ?? ""
                }
            }
        }
        
        return AnyView(mainContent)
    }
    
    private func buildPendingSummary(pendingEvents: [RoundEvent], players: [Player]) -> String {
        let grouped = groupEvents(events: pendingEvents, players: players)
        return grouped.map { item in
            switch item {
            case .individual(let pe):
                let playerName = players.first { $0.id == pe.playerId }?.name ?? ""
                let pointsFormatted = "\(pe.points > 0 ? "+" : "")\(pe.points)"
                let noteStr = pe.note.flatMap { !$0.isEmpty ? " (\($0))" : "" } ?? ""
                return "\(playerName) \(pointsFormatted)\(noteStr)"
            case .transfer(let fromEvent, let toEvent, let baseNote):
                let fromPlayerName = players.first { $0.id == fromEvent.playerId }?.name ?? ""
                let toPlayerName = players.first { $0.id == toEvent.playerId }?.name ?? ""
                let pointsFormatted = "\(toEvent.points > 0 ? "+" : "")\(toEvent.points)"
                let noteStr = !baseNote.isEmpty ? " (\(baseNote))" : ""
                return "\(fromPlayerName) → \(toPlayerName) \(pointsFormatted)\(noteStr)"
            }
        }.joined(separator: ", ")
    }
    
    private func saveRound(session: Session, visiblePlayers: [Player], liveSum: Int, liveEmptyId: String?, liveEmptyCount: Int, liveParseError: Bool) {
        if liveParseError {
            showInvalidAlert = true
            return
        }
        
        let filledCount = visiblePlayers.count - liveEmptyCount
        if filledCount == 0 {
            showEmptyAlert = true
            return
        }
        
        let zeroSum = session.zeroSum
        
        if zeroSum && liveEmptyCount >= 2 {
            showNeedMoreAlert = true
            return
        }
        
        if zeroSum && liveEmptyCount == 0 && liveSum != 0 {
            unbalancedSum = liveSum
            showUnbalancedAlert = true
            return
        }
        
        // Save
        let finalScores: [Score]
        if zeroSum {
            finalScores = visiblePlayers.map { p in
                let raw = (scores[p.id] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let points = raw.isEmpty ? -liveSum : (Int(raw) ?? 0)
                return Score(roundId: roundId ?? UUID().uuidString, playerId: p.id, points: points)
            }
        } else {
            finalScores = visiblePlayers.map { p in
                let raw = (scores[p.id] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let points = raw.isEmpty ? 0 : (Int(raw) ?? 0)
                return Score(roundId: roundId ?? UUID().uuidString, playerId: p.id, points: points)
            }
        }
        
        let finalNote = note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : note.trimmingCharacters(in: .whitespacesAndNewlines)
        
        if let rid = roundId {
            // Update round
            if let round = session.rounds.first(where: { $0.id == rid }) {
                round.note = finalNote
                
                // Clear old scores and add new
                for score in round.scores {
                    modelContext.delete(score)
                }
                round.scores.removeAll()
                
                for fs in finalScores {
                    fs.round = round
                    round.scores.append(fs)
                }
            }
        } else {
            // New round
            let newRound = Round(note: finalNote, orderIndex: session.rounds.count)
            modelContext.insert(newRound)
            newRound.session = session
            session.rounds.append(newRound)
            
            for fs in finalScores {
                fs.round = newRound
                newRound.scores.append(fs)
            }
            
            // Move pending events into the round
            let pendingEvents = session.events.filter { $0.roundId == nil }
            for pe in pendingEvents {
                pe.round = newRound
                newRound.events.append(pe)
            }
        }
        
        session.updatedAt = Date()
        try? modelContext.save()
        onBack()
    }
}
