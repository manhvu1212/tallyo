package io.github.manhvu1212.tallyo.domain

fun computePlayerStats(session: Session): List<PlayerStats> {
    val map = LinkedHashMap<String, Accumulator>()
    for (p in session.players) {
        map[p.id] = Accumulator(p.id, p.name)
    }

    for (round in session.rounds) {
        if (round.scores.isEmpty()) continue
        val max = round.scores.maxOf { it.points }
        val min = round.scores.minOf { it.points }
        for (s in round.scores) {
            val acc = map[s.playerId] ?: continue
            acc.totalPoints += s.points
            acc.roundsPlayed += 1
            if (s.points > acc.bestRound) acc.bestRound = s.points
            if (s.points < acc.worstRound) acc.worstRound = s.points
            if (s.points == max && max != min) acc.wins += 1
            if (s.points == min && max != min) acc.losses += 1
        }
        for (e in round.events) {
            val acc = map[e.playerId] ?: continue
            acc.totalPoints += e.points
        }
    }

    for (pe in session.pendingEvents) {
        val acc = map[pe.playerId] ?: continue
        acc.totalPoints += pe.points
    }

    return map.values.map { it.toStats() }
}

private class Accumulator(val playerId: String, val name: String) {
    var totalPoints: Int = 0
    var roundsPlayed: Int = 0
    var wins: Int = 0
    var losses: Int = 0
    var bestRound: Int = Int.MIN_VALUE
    var worstRound: Int = Int.MAX_VALUE

    fun toStats(): PlayerStats = PlayerStats(
        playerId = playerId,
        name = name,
        totalPoints = totalPoints,
        roundsPlayed = roundsPlayed,
        wins = wins,
        losses = losses,
        bestRound = if (roundsPlayed == 0) 0 else bestRound,
        worstRound = if (roundsPlayed == 0) 0 else worstRound,
        averagePerRound = if (roundsPlayed == 0) 0.0 else totalPoints.toDouble() / roundsPlayed,
    )
}

private fun roundSpread(round: Round): Int {
    if (round.scores.isEmpty()) return 0
    return round.scores.maxOf { it.points } - round.scores.minOf { it.points }
}

fun computeInsights(session: Session): SessionInsights {
    val stats = computePlayerStats(session)
    val played = stats.filter { it.roundsPlayed > 0 }
    val sorted = played.sortedByDescending { it.totalPoints }

    val leaders = mutableListOf<PlayerStats>()
    val trailers = mutableListOf<PlayerStats>()
    if (sorted.isNotEmpty()) {
        val top = sorted.first().totalPoints
        val bottom = sorted.last().totalPoints
        sorted.filterTo(leaders) { it.totalPoints == top }
        if (top != bottom) sorted.filterTo(trailers) { it.totalPoints == bottom }
    }

    var biggestBlowoutRoundIndex: Int? = null
    var closestRoundIndex: Int? = null
    var biggestSpread = -1
    var smallestSpread = Int.MAX_VALUE
    session.rounds.forEachIndexed { i, r ->
        if (r.scores.size < 2) return@forEachIndexed
        val spread = roundSpread(r)
        if (spread > biggestSpread) {
            biggestSpread = spread
            biggestBlowoutRoundIndex = i
        }
        if (spread < smallestSpread) {
            smallestSpread = spread
            closestRoundIndex = i
        }
    }

    val sweepPlayer = if (session.rounds.size >= 2) {
        stats.firstOrNull { it.wins == session.rounds.size }
    } else null

    val totalPointsExchanged = session.rounds.sumOf { round ->
        round.scores.sumOf { kotlin.math.abs(it.points) }
    }

    return SessionInsights(
        totalRounds = session.rounds.size,
        totalPointsExchanged = totalPointsExchanged,
        leaders = leaders,
        trailers = trailers,
        biggestBlowoutRoundIndex = biggestBlowoutRoundIndex,
        closestRoundIndex = closestRoundIndex,
        sweepPlayer = sweepPlayer,
    )
}
