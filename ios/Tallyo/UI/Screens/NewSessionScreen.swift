import SwiftUI
import SwiftData

struct PresetGameItem: Identifiable {
    let id: String
    let localizationKey: String
    let defaultZeroSum: Bool
}

let PRESET_GAMES = [
    PresetGameItem(id: "tien_len", localizationKey: "game_tien_len", defaultZeroSum: true),
    PresetGameItem(id: "phom", localizationKey: "game_phom", defaultZeroSum: true),
    PresetGameItem(id: "mau_binh", localizationKey: "game_mau_binh", defaultZeroSum: true),
    PresetGameItem(id: "poker", localizationKey: "game_poker", defaultZeroSum: true),
    PresetGameItem(id: "sam", localizationKey: "game_sam", defaultZeroSum: true),
    PresetGameItem(id: "ludo", localizationKey: "game_ludo", defaultZeroSum: false),
    PresetGameItem(id: "monopoly", localizationKey: "game_monopoly", defaultZeroSum: false),
    PresetGameItem(id: "custom", localizationKey: "game_custom", defaultZeroSum: false)
]

struct NewSessionScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    
    @Query private var customGames: [CustomGame]
    
    @State private var name: String = ""
    @State private var selectedGameKey: String = "tien_len"
    @State private var customGameName: String = ""
    @State private var playerInput: String = ""
    @State private var players: [String] = []
    @State private var zeroSum: Bool = true
    
    @State private var duplicateName: String? = nil
    @State private var showDuplicateAlert = false
    
    @State private var showMinPlayersAlert = false
    
    let onBack: () -> Void
    let onCreated: (String) -> Void
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        let canCreate = players.count >= 2
        
        NavigationStack {
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
                        
                        Text(NSLocalizedString("new_title", comment: ""))
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Spacer()
                        
                        // Empty spacer to center title
                        Color.clear.frame(width: 24, height: 24)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    
                    ScrollView {
                        VStack(alignment: .leading, spacing: 24) {
                            
                            // 1. Game Section
                            NewSessionSection(title: NSLocalizedString("new_game_label", comment: "")) {
                                VStack(alignment: .leading, spacing: 12) {
                                    // Flow row of games
                                    FlowLayout(spacing: 8) {
                                        // Presets (excluding custom option first)
                                        ForEach(PRESET_GAMES.filter { $0.id != "custom" }) { game in
                                            let isSelected = selectedGameKey == game.id
                                            GameChip(
                                                title: NSLocalizedString(game.localizationKey, comment: ""),
                                                isSelected: isSelected,
                                                onTap: {
                                                    selectedGameKey = game.id
                                                    zeroSum = game.defaultZeroSum
                                                }
                                            )
                                        }
                                        
                                        // Saved Custom Games
                                        ForEach(customGames) { customGame in
                                            let key = "custom_saved:\(customGame.name)"
                                            let isSelected = selectedGameKey == key
                                            HStack(spacing: 4) {
                                                Text(customGame.name)
                                                    .font(.system(size: 13, weight: .medium))
                                                    .foregroundColor(isSelected ? palette.primary : palette.text)
                                                
                                                Button(action: {
                                                    modelContext.delete(customGame)
                                                    try? modelContext.save()
                                                    if selectedGameKey == key {
                                                        selectedGameKey = "tien_len"
                                                    }
                                                }) {
                                                    Text("×")
                                                        .font(.system(size: 16, weight: .bold))
                                                        .foregroundColor(isSelected ? palette.primary : palette.textMuted)
                                                        .padding(.leading, 2)
                                                }
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(isSelected ? palette.primaryTintBg : palette.surfaceAlt)
                                            .cornerRadius(999)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 999)
                                                    .stroke(isSelected ? palette.primary : palette.border, lineWidth: 1)
                                            )
                                            .onTapGesture {
                                                selectedGameKey = key
                                                zeroSum = customGame.defaultZeroSum
                                            }
                                        }
                                        
                                        // Custom option
                                        if let customItem = PRESET_GAMES.first(where: { $0.id == "custom" }) {
                                            let isSelected = selectedGameKey == customItem.id
                                            GameChip(
                                                title: NSLocalizedString(customItem.localizationKey, comment: ""),
                                                isSelected: isSelected,
                                                onTap: {
                                                    selectedGameKey = customItem.id
                                                    zeroSum = customItem.defaultZeroSum
                                                }
                                            )
                                        }
                                    }
                                    
                                    if selectedGameKey == "custom" {
                                        TextField(NSLocalizedString("new_game_custom_placeholder", comment: ""), text: $customGameName)
                                            .padding(12)
                                            .background(palette.surface)
                                            .foregroundColor(palette.text)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(palette.border, lineWidth: 1)
                                            )
                                            .textInputAutocapitalization(.words)
                                    }
                                }
                            }
                            
                            // 2. Session Name Section
                            NewSessionSection(title: NSLocalizedString("new_name_label", comment: "")) {
                                TextField(NSLocalizedString("new_name_placeholder", comment: ""), text: $name)
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
                            
                            // 3. Players Section
                            NewSessionSection(
                                title: NSLocalizedString("new_players_label", comment: ""),
                                hint: NSLocalizedString("new_players_hint", comment: "")
                            ) {
                                VStack(alignment: .leading, spacing: 12) {
                                    HStack {
                                        TextField(NSLocalizedString("new_players_placeholder", comment: ""), text: $playerInput)
                                            .padding(12)
                                            .background(palette.surface)
                                            .foregroundColor(palette.text)
                                            .cornerRadius(8)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(palette.border, lineWidth: 1)
                                            )
                                            .textInputAutocapitalization(.words)
                                            .onSubmit {
                                                addPlayer()
                                            }
                                        
                                        Button(action: addPlayer) {
                                            Text(NSLocalizedString("new_players_add", comment: ""))
                                                .font(.system(size: 14, weight: .bold))
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 12)
                                                .background(palette.primary)
                                                .foregroundColor(.white)
                                                .cornerRadius(8)
                                        }
                                    }
                                    
                                    if players.isEmpty {
                                        Text(NSLocalizedString("new_players_empty", comment: ""))
                                            .font(.system(size: 11))
                                            .foregroundColor(palette.textMuted)
                                    } else {
                                        FlowLayout(spacing: 8) {
                                            ForEach(players, id: \.self) { p in
                                                HStack(spacing: 6) {
                                                    Text(p)
                                                        .font(.system(size: 13, weight: .medium))
                                                        .foregroundColor(palette.text)
                                                    Text("×")
                                                        .font(.system(size: 16, weight: .bold))
                                                        .foregroundColor(palette.textMuted)
                                                }
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 8)
                                                .background(palette.surfaceAlt)
                                                .cornerRadius(999)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 999)
                                                        .stroke(palette.border, lineWidth: 1)
                                                )
                                                .onTapGesture {
                                                    players.removeAll { $0 == p }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 4. Scoring Options Section
                            NewSessionSection(title: NSLocalizedString("new_options_label", comment: "")) {
                                Button(action: { zeroSum.toggle() }) {
                                    HStack(alignment: .top, spacing: 12) {
                                        // Checkbox
                                        ZStack {
                                            RoundedRectangle(cornerRadius: 4)
                                                .stroke(zeroSum ? palette.primary : palette.border, lineWidth: 2)
                                                .frame(width: 20, height: 20)
                                            
                                            if zeroSum {
                                                RoundedRectangle(cornerRadius: 4)
                                                    .fill(palette.primary)
                                                    .frame(width: 20, height: 20)
                                                
                                                Image(systemName: "checkmark")
                                                    .font(.system(size: 12, weight: .bold))
                                                    .foregroundColor(.white)
                                            }
                                        }
                                        .padding(.top, 2)
                                        
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(NSLocalizedString("new_options_zero_sum_title", comment: ""))
                                                .font(.system(size: 16, weight: .semibold))
                                                .foregroundColor(palette.text)
                                            
                                            Text(NSLocalizedString("new_options_zero_sum_desc", comment: ""))
                                                .font(.system(size: 13))
                                                .foregroundColor(palette.textMuted)
                                                .lineSpacing(4)
                                                .multilineTextAlignment(.leading)
                                        }
                                    }
                                    .padding(12)
                                    .background(zeroSum ? palette.primaryTintBg : palette.surface)
                                    .cornerRadius(10)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(zeroSum ? palette.primary : palette.border, lineWidth: 1)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                            
                            // Submit Button
                            Button(action: submitSession) {
                                Text(NSLocalizedString("new_submit", comment: ""))
                                    .font(.system(size: 16, weight: .bold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(canCreate ? palette.primary : palette.primary.opacity(0.4))
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }
                            .disabled(!canCreate)
                            .padding(.top, 16)
                            .padding(.bottom, 48)
                            
                        }
                        .padding(.horizontal, 16)
                    }
                }
            }
            .alert(isPresented: $showDuplicateAlert) {
                Alert(
                    title: Text(NSLocalizedString("new_players_duplicate_title", comment: "")),
                    message: Text(String(format: NSLocalizedString("new_players_duplicate_message", comment: ""), duplicateName ?? "")),
                    dismissButton: .default(Text("OK")) { duplicateName = nil }
                )
            }
            .alert(NSLocalizedString("new_min_players", comment: ""), isPresented: $showMinPlayersAlert) {
                Button("OK", role: .cancel) { }
            }
        }
    }
    
    private func addPlayer() {
        let trimmed = playerInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return }
        if players.contains(trimmed) {
            duplicateName = trimmed
            showDuplicateAlert = true
            return
        }
        players.append(trimmed)
        playerInput = ""
    }
    
    private func submitSession() {
        if players.count < 2 {
            showMinPlayersAlert = true
            return
        }
        
        let selectedGameItem = PRESET_GAMES.first { $0.id == selectedGameKey }
        let isCustom = selectedGameKey == "custom" || selectedGameKey.hasPrefix("custom_saved:")
        
        let gameNameToSend: String
        if selectedGameKey == "custom" {
            let customNameTrimmed = customGameName.trimmingCharacters(in: .whitespacesAndNewlines)
            gameNameToSend = customNameTrimmed.isEmpty ? NSLocalizedString("game_custom", comment: "") : customNameTrimmed
        } else if selectedGameKey.hasPrefix("custom_saved:") {
            gameNameToSend = String(selectedGameKey.dropFirst("custom_saved:".count))
        } else {
            gameNameToSend = selectedGameItem.flatMap { NSLocalizedString($0.localizationKey, comment: "") } ?? ""
        }
        
        let defaultSessionName: String
        if isCustom {
            defaultSessionName = gameNameToSend
        } else if let item = selectedGameItem {
            defaultSessionName = NSLocalizedString(item.localizationKey, comment: "")
        } else {
            defaultSessionName = NSLocalizedString("new_session_default_name", comment: "")
        }
        
        let sessionName = name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? defaultSessionName : name.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // 1. If it's a new custom game, save it to the DB so it is in the chip list next time
        if selectedGameKey == "custom" && !customGameName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let newCustomGameName = customGameName.trimmingCharacters(in: .whitespacesAndNewlines)
            if !customGames.contains(where: { $0.name == newCustomGameName }) {
                let cg = CustomGame(name: newCustomGameName, defaultZeroSum: zeroSum)
                modelContext.insert(cg)
            }
        }
        
        // 2. Create the Session
        let session = Session(name: sessionName, game: gameNameToSend, zeroSum: zeroSum)
        
        // Add players
        for (index, playerName) in players.enumerated() {
            let player = Player(name: playerName, orderIndex: index)
            session.players.append(player)
        }
        
        modelContext.insert(session)
        try? modelContext.save()
        
        onCreated(session.id)
    }
}

struct NewSessionSection<Content: View>: View {
    let title: String
    var hint: String? = nil
    let content: () -> Content
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(palette.text)
            
            if let hint = hint {
                Text(hint)
                    .font(.system(size: 11))
                    .foregroundColor(palette.textMuted)
                    .padding(.top, -6)
            }
            
            content()
        }
    }
}

struct GameChip: View {
    let title: String
    let isSelected: Bool
    let onTap: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        Text(title)
            .font(.system(size: 13, weight: .medium))
            .foregroundColor(isSelected ? palette.primary : palette.text)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(isSelected ? palette.primaryTintBg : palette.surfaceAlt)
            .cornerRadius(999)
            .overlay(
                RoundedRectangle(cornerRadius: 999)
                    .stroke(isSelected ? palette.primary : palette.border, lineWidth: 1)
            )
            .onTapGesture(perform: onTap)
    }
}

// Simple FlowLayout implementation for displaying chips nicely
struct FlowLayout: Layout {
    var spacing: CGFloat
    
    init(spacing: CGFloat = 8) {
        self.spacing = spacing
    }
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let bounds = proposal.width ?? 300
        var totalHeight: CGFloat = 0
        var currentWidth: CGFloat = 0
        var currentRowHeight: CGFloat = 0
        
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if currentWidth + size.width > bounds {
                // wrap
                totalHeight += currentRowHeight + spacing
                currentWidth = size.width + spacing
                currentRowHeight = size.height
            } else {
                currentWidth += size.width + spacing
                currentRowHeight = max(currentRowHeight, size.height)
            }
        }
        totalHeight += currentRowHeight
        return CGSize(width: bounds, height: totalHeight)
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var currentRowHeight: CGFloat = 0
        
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX {
                // wrap
                x = bounds.minX
                y += currentRowHeight + spacing
                currentRowHeight = size.height
            } else {
                currentRowHeight = max(currentRowHeight, size.height)
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
        }
    }
}
