import SwiftUI
import SwiftData

struct SessionsListScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    
    @Query(sort: \Session.updatedAt, order: .reverse) private var sessions: [Session]
    
    @State private var deleteTarget: Session?
    @State private var showDeleteAlert = false
    
    let onOpenSession: (String) -> Void
    let onNewSession: () -> Void
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        NavigationStack {
            ZStack {
                palette.bg.ignoresSafeArea()
                
                VStack(alignment: .leading, spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Tallyo")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(palette.text)
                        
                        Text(NSLocalizedString("app_subtitle", comment: ""))
                            .font(.system(size: 13))
                            .foregroundColor(palette.textMuted)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 16)
                    .padding(.bottom, 12)
                    
                    if sessions.isEmpty {
                        Spacer()
                        VStack(spacing: 8) {
                            Text(NSLocalizedString("sessions_empty_title", comment: ""))
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(palette.text)
                            
                            Text(NSLocalizedString("sessions_empty_hint", comment: ""))
                                .font(.system(size: 13))
                                .foregroundColor(palette.textMuted)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                        }
                        Spacer()
                    } else {
                        List {
                            ForEach(sessions) { session in
                                SessionRow(session: session) {
                                    onOpenSession(session.id)
                                } onRequestDelete: {
                                    deleteTarget = session
                                    showDeleteAlert = true
                                }
                                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                                .listRowBackground(Color.clear)
                                .listRowSeparator(.hidden)
                            }
                        }
                        .listStyle(.plain)
                        .padding(.bottom, 80) // Leave space for FAB
                    }
                }
                
                // Floating Action Button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: onNewSession) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus")
                                    .font(.system(size: 18, weight: .bold))
                                Text(NSLocalizedString("sessions_new_fab", comment: ""))
                                    .font(.system(size: 15, weight: .semibold))
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 14)
                            .background(palette.primary)
                            .foregroundColor(.white)
                            .cornerRadius(999)
                            .shadow(color: palette.primary.opacity(0.4), radius: 8, x: 0, y: 4)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 28)
                    }
                }
            }
            .navigationBarHidden(true)
            .alert(isPresented: $showDeleteAlert) {
                let targetName = deleteTarget?.name ?? ""
                return Alert(
                    title: Text(targetName),
                    message: Text(NSLocalizedString("sessions_delete_message", comment: "")),
                    primaryButton: .destructive(Text(NSLocalizedString("sessions_delete_confirm", comment: ""))) {
                        if let target = deleteTarget {
                            modelContext.delete(target)
                            try? modelContext.save()
                        }
                        deleteTarget = nil
                    },
                    secondaryButton: .cancel(Text(NSLocalizedString("common_cancel", comment: ""))) {
                        deleteTarget = nil
                    }
                )
            }
        }
    }
}

struct SessionRow: View {
    let session: Session
    let onOpen: () -> Void
    let onRequestDelete: () -> Void
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        let palette = TallyoTheme.palette(for: colorScheme)
        
        let totals: [String: Int] = {
            var map: [String: Int] = [:]
            for r in session.rounds {
                for s in r.scores {
                    map[s.playerId] = (map[s.playerId] ?? 0) + s.points
                }
            }
            return map
        }()
        
        let leader = session.players.max { p1, p2 in
            let score1 = totals[p1.id] ?? 0
            let score2 = totals[p2.id] ?? 0
            return score1 < score2
        }
        
        let leaderTotal = leader.flatMap { totals[$0.id] } ?? 0
        
        let metaPeopleStr = session.players.count == 1 ?
            NSLocalizedString("sessions_meta_people_one", comment: "") :
            NSLocalizedString("sessions_meta_people_other", comment: "")
        let metaRoundsStr = session.rounds.count == 1 ?
            NSLocalizedString("sessions_meta_rounds_one", comment: "") :
            NSLocalizedString("sessions_meta_rounds_other", comment: "")
            
        var metaParts: [String] = [
            String(format: metaPeopleStr, session.players.count),
            String(format: metaRoundsStr, session.rounds.count)
        ]
        if session.zeroSum {
            metaParts.append(NSLocalizedString("sessions_meta_zero_sum", comment: ""))
        }
        let metaString = metaParts.joined(separator: " · ")
        
        let relativeDateString: String = {
            let formatter = RelativeDateTimeFormatter()
            formatter.unitsStyle = .full
            return formatter.localizedString(for: session.updatedAt, relativeTo: Date())
        }()
        
        Button(action: onOpen) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(session.name)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(palette.text)
                            .lineLimit(1)
                        
                        if !session.game.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text(session.game)
                                .font(.system(size: 11, weight: .semibold))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(palette.primaryTintBg)
                                .foregroundColor(palette.primary)
                                .cornerRadius(999)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 999)
                                        .stroke(palette.primary.opacity(0.5), lineWidth: 1)
                                )
                        }
                    }
                    
                    Text(metaString)
                        .font(.system(size: 13))
                        .foregroundColor(palette.textMuted)
                    
                    Text(relativeDateString)
                        .font(.system(size: 11))
                        .foregroundColor(palette.textMuted)
                        .opacity(0.7)
                }
                
                Spacer()
                
                if leader != nil && !session.rounds.isEmpty {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(NSLocalizedString("sessions_leader", comment: ""))
                            .font(.system(size: 11))
                            .foregroundColor(palette.textMuted)
                        
                        Text(leader?.name ?? "")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(palette.text)
                            .lineLimit(1)
                        
                        Text("\(leaderTotal > 0 ? "+" : "")\(leaderTotal)")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(palette.accent)
                    }
                    .padding(.leading, 12)
                    .frame(minWidth: 80, alignment: .trailing)
                }
            }
            .padding(16)
            .background(palette.surface)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(palette.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                onRequestDelete()
            } label: {
                Label(NSLocalizedString("sessions_delete_confirm", comment: ""), systemImage: "trash")
            }
        }
    }
}
