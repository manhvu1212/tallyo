package io.github.manhvu1212.tallyo.domain

data class Player(
    val id: String,
    val name: String,
    val resting: Boolean = false,
)

data class RoundScore(
    val playerId: String,
    val points: Int,
)

data class RoundEvent(
    val id: String,
    val sessionId: String,
    val roundId: String?,
    val playerId: String,
    val points: Int,
    val note: String?,
    val createdAt: Long,
)

data class Round(
    val id: String,
    val createdAt: Long,
    val scores: List<RoundScore>,
    val events: List<RoundEvent> = emptyList(),
    val note: String?,
)

data class Session(
    val id: String,
    val name: String,
    val game: String,
    val createdAt: Long,
    val updatedAt: Long,
    val zeroSum: Boolean,
    val players: List<Player>,
    val rounds: List<Round>,
    val pendingEvents: List<RoundEvent> = emptyList(),
)

data class PlayerStats(
    val playerId: String,
    val name: String,
    val totalPoints: Int,
    val roundsPlayed: Int,
    val wins: Int,
    val losses: Int,
    val bestRound: Int,
    val worstRound: Int,
    val averagePerRound: Double,
)

data class SessionInsights(
    val totalRounds: Int,
    val totalPointsExchanged: Int,
    val leaders: List<PlayerStats>,
    val trailers: List<PlayerStats>,
    val biggestBlowoutRoundIndex: Int?,
    val closestRoundIndex: Int?,
    val sweepPlayer: PlayerStats?,
)

data class CustomGame(
    val name: String,
    val defaultZeroSum: Boolean,
)

sealed interface EventDisplayItem {
    data class Individual(val event: RoundEvent) : EventDisplayItem
    data class Transfer(
        val fromEvent: RoundEvent,
        val toEvent: RoundEvent,
        val baseNote: String
    ) : EventDisplayItem
}

fun groupEvents(
    events: List<RoundEvent>,
    players: List<Player>
): List<EventDisplayItem> {
    val result = mutableListOf<EventDisplayItem>()
    val visited = mutableSetOf<String>()

    for (i in events.indices) {
        val e1 = events[i]
        if (e1.id in visited) continue

        // Check if e1 can be the receiver in a transfer
        var paired = false
        if (e1.points > 0) {
            val toPlayer = players.firstOrNull { it.id == e1.playerId }
            if (toPlayer != null) {
                // Find a matching sender e2
                for (j in events.indices) {
                    val e2 = events[j]
                    if (e2.id in visited || e2.id == e1.id) continue

                    if (e2.points == -e1.points && e2.points < 0) {
                        val fromPlayer = players.firstOrNull { it.id == e2.playerId }
                        if (fromPlayer != null) {
                            // Check time difference
                            val timeDiff = kotlin.math.abs(e1.createdAt - e2.createdAt)
                            if (timeDiff < 5000) {
                                // Check note format
                                val n1 = e1.note.orEmpty()
                                val n2 = e2.note.orEmpty()
                                val suffixTo1 = "<- ${fromPlayer.name}"
                                val suffixTo2 = " (<- ${fromPlayer.name})"
                                val suffixFrom1 = "-> ${toPlayer.name}"
                                val suffixFrom2 = " (-> ${toPlayer.name})"

                                val match1 = (n1 == suffixTo1 && n2 == suffixFrom1)
                                val match2 = n1.endsWith(suffixTo2) && n2.endsWith(suffixFrom2) && 
                                             n1.substringBefore(suffixTo2) == n2.substringBefore(suffixFrom2)

                                if (match1 || match2) {
                                    val baseNote = if (match1) "" else n1.substringBefore(suffixTo2)
                                    result.add(EventDisplayItem.Transfer(fromEvent = e2, toEvent = e1, baseNote = baseNote))
                                    visited.add(e1.id)
                                    visited.add(e2.id)
                                    paired = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!paired) {
            result.add(EventDisplayItem.Individual(e1))
            visited.add(e1.id)
        }
    }

    return result
}
