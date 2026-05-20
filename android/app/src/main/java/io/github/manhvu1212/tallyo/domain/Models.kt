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

data class Round(
    val id: String,
    val createdAt: Long,
    val scores: List<RoundScore>,
    val note: String?,
)

data class Session(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val zeroSum: Boolean,
    val players: List<Player>,
    val rounds: List<Round>,
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
