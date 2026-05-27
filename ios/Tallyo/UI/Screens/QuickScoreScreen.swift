import SwiftUI

struct QuickScoreScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let players: [Player]
    let isZeroSum: Bool
    
    let onSaveIndividual: ([String: Int], String) -> Void
    let onSaveTransfer: (String, String, Int, String) -> Void
    
    @State private var isTransfer: Bool = false
    @State private var activeId: String? = nil
    @State private var customMode: Bool = false
    @State private var scores: [String: String] = [:] // playerId -> text points
    
    @State private var fromPlayerId: String?
    @State private var toPlayerId: String?
    @State private var pointsStr: String = ""
    @State private var note: String = ""
    @State private var showValidationError = false
    
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
                        
                        Text(NSLocalizedString("quick_score_title", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        Button(NSLocalizedString("quick_score_save", comment: "")) {
                            save()
                        }
                        .foregroundColor(palette.primary)
                        .font(.system(size: 16, weight: .bold))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .background(palette.surface)
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            
                            // Segment selector (Individual vs Transfer)
                            if !isZeroSum {
                                HStack(spacing: 4) {
                                    Button(action: { isTransfer = false }) {
                                        Text(NSLocalizedString("quick_score_type_individual", comment: ""))
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(!isTransfer ? palette.primary : palette.textMuted)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(!isTransfer ? palette.surface : Color.clear)
                                            .cornerRadius(6)
                                    }
                                    .buttonStyle(.plain)
                                    
                                    Button(action: { isTransfer = true }) {
                                        Text(NSLocalizedString("quick_score_type_transfer", comment: ""))
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(isTransfer ? palette.primary : palette.textMuted)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(isTransfer ? palette.surface : Color.clear)
                                            .cornerRadius(6)
                                    }
                                    .buttonStyle(.plain)
                                }
                                .padding(2)
                                .background(palette.surfaceAlt)
                                .cornerRadius(8)
                            }
                            
                            if !isTransfer {
                                // 1. Individual mode
                                VStack(alignment: .leading, spacing: 12) {
                                    ForEach(players) { p in
                                        let raw = scores[p.id] ?? ""
                                        let isActive = activeId == p.id
                                        let isCustom = isActive && customMode
                                        let rawNum = Int(raw)
                                        
                                        let valueColor: Color = {
                                            if raw.isEmpty { return palette.textMuted }
                                            if let num = rawNum, num > 0 { return palette.win }
                                            if let num = rawNum, num < 0 { return palette.loss }
                                            return palette.text
                                        }()
                                        
                                        let displayValue = !raw.isEmpty ? (rawNum.map { "\($0 > 0 ? "+" : "")\($0)" } ?? raw) : "0"
                                        
                                        VStack(spacing: 4) {
                                            Button(action: {
                                                activeId = p.id
                                                customMode = false
                                            }) {
                                                HStack {
                                                    Text(p.name)
                                                        .font(.system(size: 15, weight: .medium))
                                                        .foregroundColor(palette.text)
                                                    
                                                    Spacer()
                                                    
                                                    if isCustom {
                                                        TextField("", text: Binding(
                                                            get: { scores[p.id] ?? "" },
                                                            set: { scores[p.id] = $0 }
                                                        ))
                                                        .keyboardType(.numbersAndPunctuation)
                                                        .multilineTextAlignment(.trailing)
                                                        .font(.system(size: 15, weight: .bold))
                                                        .foregroundColor(valueColor)
                                                        .frame(width: 80)
                                                    } else {
                                                        Text(displayValue)
                                                            .font(.system(size: 15, weight: .bold))
                                                            .foregroundColor(valueColor)
                                                    }
                                                }
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 8)
                                                .background(isActive ? palette.primaryTintBg : palette.surfaceAlt)
                                                .cornerRadius(8)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 8)
                                                        .stroke(isActive ? palette.primary : palette.border, lineWidth: 1)
                                                )
                                            }
                                            .buttonStyle(.plain)
                                            
                                            // Deltas
                                            if isActive && !isCustom {
                                                HStack(spacing: 4) {
                                                    ForEach([-4, -2, -1, 0, 1, 2, 4], id: \.self) { dVal in
                                                        let isEdit = dVal == 0
                                                        let label = isEdit ? "✎" : "\(dVal > 0 ? "+" : "")\(dVal)"
                                                        
                                                        Button(action: {
                                                            if isEdit {
                                                                customMode = true
                                                            } else {
                                                                let cur = Int(scores[p.id] ?? "") ?? 0
                                                                let newVal = cur + dVal
                                                                scores[p.id] = newVal == 0 ? "" : "\(newVal)"
                                                            }
                                                        }) {
                                                            Text(label)
                                                                .font(.system(size: 12, weight: .bold))
                                                                .foregroundColor(isEdit ? .white : (dVal < 0 ? palette.loss : palette.win))
                                                                .frame(maxWidth: .infinity)
                                                                .padding(.vertical, 8)
                                                                .background(isEdit ? palette.primary : palette.surfaceAlt)
                                                                .cornerRadius(6)
                                                                .overlay(
                                                                    RoundedRectangle(cornerRadius: 6)
                                                                        .stroke(isEdit ? palette.primary : palette.border, lineWidth: 1)
                                                                )
                                                        }
                                                        .buttonStyle(.plain)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 2. Transfer mode
                                VStack(alignment: .leading, spacing: 12) {
                                    // From Player Selector
                                    Text(NSLocalizedString("quick_score_player_from", comment: ""))
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(palette.textMuted)
                                    
                                    FlowLayout(spacing: 8) {
                                        ForEach(players) { p in
                                            let isSel = fromPlayerId == p.id
                                            Text(p.name)
                                                .font(.system(size: 13, weight: isSel ? .semibold : .regular))
                                                .foregroundColor(isSel ? palette.primary : palette.text)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(isSel ? palette.primaryTintBg : palette.surfaceAlt)
                                                .cornerRadius(8)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 8)
                                                        .stroke(isSel ? palette.primary : Color.clear, lineWidth: 1)
                                                )
                                                .onTapGesture {
                                                    fromPlayerId = p.id
                                                    if toPlayerId == p.id {
                                                        toPlayerId = players.first { $0.id != p.id }?.id
                                                    }
                                                }
                                        }
                                    }
                                    
                                    // To Player Selector
                                    Text(NSLocalizedString("quick_score_player_to", comment: ""))
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(palette.textMuted)
                                    
                                    FlowLayout(spacing: 8) {
                                        ForEach(players.filter { $0.id != fromPlayerId }) { p in
                                            let isSel = toPlayerId == p.id
                                            Text(p.name)
                                                .font(.system(size: 13, weight: isSel ? .semibold : .regular))
                                                .foregroundColor(isSel ? palette.primary : palette.text)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(isSel ? palette.primaryTintBg : palette.surfaceAlt)
                                                .cornerRadius(8)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 8)
                                                        .stroke(isSel ? palette.primary : Color.clear, lineWidth: 1)
                                                )
                                                .onTapGesture {
                                                    toPlayerId = p.id
                                                }
                                        }
                                    }
                                    
                                    // Points Field
                                    Text(NSLocalizedString("quick_score_points", comment: ""))
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(palette.textMuted)
                                    
                                    TextField("0", text: $pointsStr)
                                        .keyboardType(.numberPad)
                                        .padding(12)
                                        .background(palette.surface)
                                        .foregroundColor(palette.text)
                                        .cornerRadius(8)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 8)
                                                .stroke(palette.border, lineWidth: 1)
                                        )
                                }
                            }
                            
                            // Note Field
                            VStack(alignment: .leading, spacing: 8) {
                                Text(NSLocalizedString("quick_score_note", comment: ""))
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
                        }
                        .padding(16)
                    }
                }
            }
            .navigationBarHidden(true)
            .alert(NSLocalizedString("quick_score_invalid", comment: ""), isPresented: $showValidationError) {
                Button("OK", role: .cancel) { }
            }
            .onAppear {
                isTransfer = isZeroSum
                fromPlayerId = players.first?.id
                toPlayerId = players.count > 1 ? players[1].id : nil
            }
        }
    }
    
    private func save() {
        if !isTransfer {
            // Save individual
            var validScores: [String: Int] = [:]
            for (pid, txt) in scores {
                let trimmed = txt.trimmingCharacters(in: .whitespacesAndNewlines)
                if trimmed.isEmpty { continue }
                guard let num = Int(trimmed) else {
                    showValidationError = true
                    return
                }
                if num != 0 {
                    validScores[pid] = num
                }
            }
            if validScores.isEmpty {
                showValidationError = true
                return
            }
            onSaveIndividual(validScores, note)
        } else {
            // Save transfer
            guard let from = fromPlayerId, let to = toPlayerId, from != to else {
                showValidationError = true
                return
            }
            let trimmed = pointsStr.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let points = Int(trimmed), points > 0 else {
                showValidationError = true
                return
            }
            onSaveTransfer(from, to, points, note)
        }
    }
}

struct EditQuickScoreScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    
    let item: EventDisplayItem
    let players: [Player]
    
    let onSaveIndividual: (String, Int, String) -> Void
    let onSaveTransfer: (String, String, Int, String) -> Void
    
    @State private var isTransfer = false
    @State private var fromPlayerId: String?
    @State private var toPlayerId: String?
    @State private var pointsStr: String = ""
    @State private var note: String = ""
    @State private var showValidationError = false
    
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
                        
                        Text(NSLocalizedString("edit_quick_score_title", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        Button(NSLocalizedString("quick_score_save", comment: "")) {
                            save()
                        }
                        .foregroundColor(palette.primary)
                        .font(.system(size: 16, weight: .bold))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .background(palette.surface)
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            
                            if !isTransfer {
                                // Individual Player Selector
                                Text(NSLocalizedString("quick_score_player_label", comment: ""))
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(palette.textMuted)
                                
                                FlowLayout(spacing: 8) {
                                    ForEach(players) { p in
                                        let isSel = fromPlayerId == p.id
                                        Text(p.name)
                                            .font(.system(size: 13, weight: isSel ? .semibold : .regular))
                                            .foregroundColor(isSel ? palette.primary : palette.text)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(isSel ? palette.primaryTintBg : palette.surfaceAlt)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(isSel ? palette.primary : Color.clear, lineWidth: 1)
                                            )
                                            .onTapGesture {
                                                fromPlayerId = p.id
                                            }
                                    }
                                }
                                
                                // Points
                                Text(NSLocalizedString("quick_score_points", comment: ""))
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(palette.textMuted)
                                
                                TextField("0", text: $pointsStr)
                                    .keyboardType(.numbersAndPunctuation)
                                    .padding(12)
                                    .background(palette.surface)
                                    .foregroundColor(palette.text)
                                    .cornerRadius(8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(palette.border, lineWidth: 1)
                                    )
                            } else {
                                // Transfer: From -> To
                                Text(NSLocalizedString("quick_score_player_from", comment: ""))
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(palette.textMuted)
                                
                                FlowLayout(spacing: 8) {
                                    ForEach(players) { p in
                                        let isSel = fromPlayerId == p.id
                                        Text(p.name)
                                            .font(.system(size: 13, weight: isSel ? .semibold : .regular))
                                            .foregroundColor(isSel ? palette.primary : palette.text)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(isSel ? palette.primaryTintBg : palette.surfaceAlt)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(isSel ? palette.primary : Color.clear, lineWidth: 1)
                                            )
                                            .onTapGesture {
                                                fromPlayerId = p.id
                                                if toPlayerId == p.id {
                                                    toPlayerId = players.first { $0.id != p.id }?.id
                                                }
                                            }
                                    }
                                }
                                
                                Text(NSLocalizedString("quick_score_player_to", comment: ""))
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(palette.textMuted)
                                
                                FlowLayout(spacing: 8) {
                                    ForEach(players.filter { $0.id != fromPlayerId }) { p in
                                        let isSel = toPlayerId == p.id
                                        Text(p.name)
                                            .font(.system(size: 13, weight: isSel ? .semibold : .regular))
                                            .foregroundColor(isSel ? palette.primary : palette.text)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(isSel ? palette.primaryTintBg : palette.surfaceAlt)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(isSel ? palette.primary : Color.clear, lineWidth: 1)
                                            )
                                            .onTapGesture {
                                                toPlayerId = p.id
                                            }
                                    }
                                }
                                
                                Text(NSLocalizedString("quick_score_points", comment: ""))
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(palette.textMuted)
                                
                                TextField("0", text: $pointsStr)
                                    .keyboardType(.numberPad)
                                    .padding(12)
                                    .background(palette.surface)
                                    .foregroundColor(palette.text)
                                    .cornerRadius(8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(palette.border, lineWidth: 1)
                                    )
                            }
                            
                            // Note Field
                            VStack(alignment: .leading, spacing: 8) {
                                Text(NSLocalizedString("quick_score_note", comment: ""))
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
                        }
                        .padding(16)
                    }
                }
            }
            .navigationBarHidden(true)
            .alert(NSLocalizedString("quick_score_invalid", comment: ""), isPresented: $showValidationError) {
                Button("OK", role: .cancel) { }
            }
            .onAppear {
                switch item {
                case .individual(let event):
                    isTransfer = false
                    fromPlayerId = event.playerId
                    pointsStr = "\(event.points)"
                    note = event.note ?? ""
                case .transfer(let fromEvent, let toEvent, let baseNote):
                    isTransfer = true
                    fromPlayerId = fromEvent.playerId
                    toPlayerId = toEvent.playerId
                    pointsStr = "\(toEvent.points)"
                    note = baseNote
                }
            }
        }
    }
    
    private func save() {
        if !isTransfer {
            guard let playerId = fromPlayerId else {
                showValidationError = true
                return
            }
            let trimmed = pointsStr.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let points = Int(trimmed), points != 0 else {
                showValidationError = true
                return
            }
            onSaveIndividual(playerId, points, note)
        } else {
            guard let from = fromPlayerId, let to = toPlayerId, from != to else {
                showValidationError = true
                return
            }
            let trimmed = pointsStr.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let points = Int(trimmed), points > 0 else {
                showValidationError = true
                return
            }
            onSaveTransfer(from, to, points, note)
        }
    }
}
