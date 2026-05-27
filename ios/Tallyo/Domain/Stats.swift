import Foundation

private class Accumulator {
    let playerId: String
    let name: String
    var totalPoints: Int = 0
    var roundsPlayed: Int = 0
    var wins: Int = 0
    var losses: Int = 0
    var bestRound: Int = Int.min
    var worstRound: Int = Int.max

    init(playerId: String, name: String) {
        self.playerId = playerId
        self.name = name
    }

    func toStats() -> PlayerStats {
        PlayerStats(
            playerId: playerId,
            name: name,
            totalPoints: totalPoints,
            roundsPlayed: roundsPlayed,
            wins: wins,
            losses: losses,
            bestRound: roundsPlayed == 0 ? 0 : bestRound,
            worstRound: roundsPlayed == 0 ? 0 : worstRound,
            averagePerRound: roundsPlayed == 0 ? 0.0 : Double(totalPoints) / Double(roundsPlayed)
        )
    }
}

func computePlayerStats(session: Session) -> [PlayerStats] {
    var map: [String: Accumulator] = [:]
    
    // Sort players by their order index to keep deterministic order
    let sortedPlayers = session.players.sorted { $0.orderIndex < $1.orderIndex }
    for p in sortedPlayers {
        map[p.id] = Accumulator(playerId: p.id, name: p.name)
    }

    for round in session.rounds {
        if round.scores.isEmpty { continue }
        
        let pointsArray = round.scores.map { $0.points }
        guard let max = pointsArray.max(), let min = pointsArray.min() else { continue }
        
        for s in round.scores {
            guard let acc = map[s.playerId] else { continue }
            acc.totalPoints += s.points
            acc.roundsPlayed += 1
            if s.points > acc.bestRound { acc.bestRound = s.points }
            if s.points < acc.worstRound { acc.worstRound = s.points }
            if s.points == max && max != min { acc.wins += 1 }
            if s.points == min && max != min { acc.losses += 1 }
        }
        
        for e in round.events {
            guard let acc = map[e.playerId] else { continue }
            acc.totalPoints += e.points
        }
    }

    // Pending events are events in session with roundId == nil
    let pendingEvents = session.events.filter { $0.roundId == nil }
    for pe in pendingEvents {
        guard let acc = map[pe.playerId] else { continue }
        acc.totalPoints += pe.points
    }

    return sortedPlayers.map { map[$0.id]?.toStats() ?? PlayerStats(playerId: $0.id, name: $0.name, totalPoints: 0, roundsPlayed: 0, wins: 0, losses: 0, bestRound: 0, worstRound: 0, averagePerRound: 0.0) }
}

private func roundSpread(_ round: Round) -> Int {
    if round.scores.isEmpty { return 0 }
    let points = round.scores.map { $0.points }
    return (points.max() ?? 0) - (points.min() ?? 0)
}

func computeInsights(session: Session) -> SessionInsights {
    let stats = computePlayerStats(session: session)
    let played = stats.filter { $0.roundsPlayed > 0 }
    let sorted = played.sorted { $0.totalPoints > $1.totalPoints }

    var leaders: [PlayerStats] = []
    var trailers: [PlayerStats] = []
    
    if let first = sorted.first {
        let top = first.totalPoints
        let bottom = sorted.last?.totalPoints ?? top
        
        leaders = sorted.filter { $0.totalPoints == top }
        if top != bottom {
            trailers = sorted.filter { $0.totalPoints == bottom }
        }
    }

    var biggestBlowoutRoundIndex: Int? = nil
    var closestRoundIndex: Int? = nil
    var biggestSpread = -1
    var smallestSpread = Int.max
    
    // Sort rounds by orderIndex to inspect them sequentially
    let sortedRounds = session.rounds.sorted { $0.orderIndex < $1.orderIndex }
    for (i, r) in sortedRounds.enumerated() {
        if r.scores.count < 2 { continue }
        let spread = roundSpread(r)
        if spread > biggestSpread {
            biggestSpread = spread
            biggestBlowoutRoundIndex = i
        }
        if spread < smallestSpread {
            smallestSpread = spread
            closestRoundIndex = i
        }
    }

    let sweepPlayer = session.rounds.count >= 2 ? stats.first(where: { $0.wins == session.rounds.count }) : nil

    let totalPointsExchanged = session.rounds.reduce(0) { sum, round in
        sum + round.scores.reduce(0) { rSum, score in rSum + abs(score.points) }
    }

    return SessionInsights(
        totalRounds: session.rounds.count,
        totalPointsExchanged: totalPointsExchanged,
        leaders: leaders,
        trailers: trailers,
        biggestBlowoutRoundIndex: biggestBlowoutRoundIndex,
        closestRoundIndex: closestRoundIndex,
        sweepPlayer: sweepPlayer
    )
}
