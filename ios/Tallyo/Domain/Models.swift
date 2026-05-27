import Foundation

struct PlayerStats: Identifiable, Equatable {
    var id: String { playerId }
    let playerId: String
    let name: String
    let totalPoints: Int
    let roundsPlayed: Int
    let wins: Int
    let losses: Int
    let bestRound: Int
    let worstRound: Int
    let averagePerRound: Double
}

struct SessionInsights {
    let totalRounds: Int
    let totalPointsExchanged: Int
    let leaders: [PlayerStats]
    let trailers: [PlayerStats]
    let biggestBlowoutRoundIndex: Int?
    let closestRoundIndex: Int?
    let sweepPlayer: PlayerStats?
}

enum EventDisplayItem: Identifiable {
    var id: String {
        switch self {
        case .individual(let event):
            return "ind_\(event.id)"
        case .transfer(let fromEvent, let toEvent, _):
            return "trn_\(fromEvent.id)_\(toEvent.id)"
        }
    }
    
    case individual(event: RoundEvent)
    case transfer(fromEvent: RoundEvent, toEvent: RoundEvent, baseNote: String)
}

func groupEvents(events: [RoundEvent], players: [Player]) -> [EventDisplayItem] {
    var result: [EventDisplayItem] = []
    var visited: Set<String> = []
    
    // Sort events to match index access in Kotlin (which was based on DB order or creation time)
    let sortedEvents = events.sorted { $0.createdAt < $1.createdAt }
    
    for i in 0..<sortedEvents.count {
        let e1 = sortedEvents[i]
        if visited.contains(e1.id) { continue }
        
        var paired = false
        if e1.points > 0 {
            if let toPlayer = players.first(where: { $0.id == e1.playerId }) {
                for j in 0..<sortedEvents.count {
                    let e2 = sortedEvents[j]
                    if visited.contains(e2.id) || e2.id == e1.id { continue }
                    
                    if e2.points == -e1.points && e2.points < 0 {
                        if let fromPlayer = players.first(where: { $0.id == e2.playerId }) {
                            let timeDiff = abs(e1.createdAt.timeIntervalSince1970 - e2.createdAt.timeIntervalSince1970) * 1000 // in ms
                            if timeDiff < 5000 {
                                let n1 = e1.note ?? ""
                                let n2 = e2.note ?? ""
                                let suffixTo1 = "<- \(fromPlayer.name)"
                                let suffixTo2 = " (<- \(fromPlayer.name))"
                                let suffixFrom1 = "-> \(toPlayer.name)"
                                let suffixFrom2 = " (-> \(toPlayer.name))"
                                
                                let match1 = (n1 == suffixTo1 && n2 == suffixFrom1)
                                let match2 = n1.hasSuffix(suffixTo2) && n2.hasSuffix(suffixFrom2) &&
                                    n1.replacingOccurrences(of: suffixTo2, with: "") == n2.replacingOccurrences(of: suffixFrom2, with: "")
                                
                                if match1 || match2 {
                                    let baseNote = match1 ? "" : n1.replacingOccurrences(of: suffixTo2, with: "")
                                    result.append(.transfer(fromEvent: e2, toEvent: e1, baseNote: baseNote))
                                    visited.insert(e1.id)
                                    visited.insert(e2.id)
                                    paired = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if !paired {
            result.append(.individual(event: e1))
            visited.insert(e1.id)
        }
    }
    
    return result
}
