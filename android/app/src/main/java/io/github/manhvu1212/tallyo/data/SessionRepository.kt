package io.github.manhvu1212.tallyo.data

import io.github.manhvu1212.tallyo.data.db.PlayerEntity
import io.github.manhvu1212.tallyo.data.db.RoundEntity
import io.github.manhvu1212.tallyo.data.db.ScoreEntity
import io.github.manhvu1212.tallyo.data.db.SessionDao
import io.github.manhvu1212.tallyo.data.db.SessionEntity
import io.github.manhvu1212.tallyo.data.db.SessionWithDetails
import io.github.manhvu1212.tallyo.domain.Player
import io.github.manhvu1212.tallyo.domain.Round
import io.github.manhvu1212.tallyo.domain.RoundScore
import io.github.manhvu1212.tallyo.domain.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SessionRepository(private val dao: SessionDao) {

    fun observeSessions(): Flow<List<Session>> =
        dao.observeAll().map { list -> list.map(::toDomain) }

    fun observeSession(id: String): Flow<Session?> =
        dao.observeById(id).map { it?.let(::toDomain) }

    suspend fun createSession(
        name: String,
        playerNames: List<String>,
        zeroSum: Boolean,
        defaultName: String,
    ): String {
        val now = System.currentTimeMillis()
        val sessionId = uid()
        val effectiveName = name.trim().ifEmpty { defaultName }
        val players = playerNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, n ->
                PlayerEntity(
                    id = uid(),
                    sessionId = sessionId,
                    name = n,
                    resting = false,
                    orderIndex = index,
                )
            }
        dao.createSession(
            SessionEntity(
                id = sessionId,
                name = effectiveName,
                createdAt = now,
                updatedAt = now,
                zeroSum = zeroSum,
            ),
            players,
        )
        return sessionId
    }

    suspend fun deleteSession(id: String) = dao.deleteSession(id)

    suspend fun renameSession(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.renameSession(id, trimmed, System.currentTimeMillis())
    }

    suspend fun addRound(
        session: Session,
        scores: List<RoundScore>,
        note: String?,
    ): String {
        val now = System.currentTimeMillis()
        val roundId = uid()
        val orderIndex = session.rounds.size
        dao.appendRound(
            sessionId = session.id,
            round = RoundEntity(
                id = roundId,
                sessionId = session.id,
                createdAt = now,
                note = note?.takeIf { it.isNotBlank() },
                orderIndex = orderIndex,
            ),
            scores = scores.map { ScoreEntity(roundId, it.playerId, it.points) },
            ts = now,
        )
        return roundId
    }

    suspend fun updateRound(
        sessionId: String,
        roundId: String,
        scores: List<RoundScore>,
        note: String?,
    ) {
        dao.replaceRoundScores(
            sessionId = sessionId,
            roundId = roundId,
            note = note?.takeIf { it.isNotBlank() },
            scores = scores.map { ScoreEntity(roundId, it.playerId, it.points) },
            ts = System.currentTimeMillis(),
        )
    }

    suspend fun deleteRound(sessionId: String, roundId: String) {
        dao.deleteRound(roundId)
        dao.touchSession(sessionId, System.currentTimeMillis())
    }

    suspend fun addPlayer(session: Session, name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val id = uid()
        val nextOrder = session.players.size
        dao.upsertPlayers(
            listOf(
                PlayerEntity(
                    id = id,
                    sessionId = session.id,
                    name = trimmed,
                    resting = false,
                    orderIndex = nextOrder,
                ),
            ),
        )
        dao.touchSession(session.id, System.currentTimeMillis())
        return id
    }

    suspend fun setPlayerResting(session: Session, playerId: String, resting: Boolean) {
        val current = session.players.firstOrNull { it.id == playerId } ?: return
        val orderIndex = session.players.indexOf(current).coerceAtLeast(0)
        dao.updatePlayer(
            PlayerEntity(
                id = playerId,
                sessionId = session.id,
                name = current.name,
                resting = resting,
                orderIndex = orderIndex,
            ),
        )
        dao.touchSession(session.id, System.currentTimeMillis())
    }

    suspend fun removePlayer(sessionId: String, playerId: String) {
        dao.deletePlayer(playerId)
        dao.touchSession(sessionId, System.currentTimeMillis())
    }

    private fun toDomain(details: SessionWithDetails): Session {
        val playersSorted = details.players.sortedBy { it.orderIndex }
        val roundsSorted = details.rounds.sortedBy { it.round.orderIndex }
        return Session(
            id = details.session.id,
            name = details.session.name,
            createdAt = details.session.createdAt,
            updatedAt = details.session.updatedAt,
            zeroSum = details.session.zeroSum,
            players = playersSorted.map { Player(it.id, it.name, it.resting) },
            rounds = roundsSorted.map { rws ->
                Round(
                    id = rws.round.id,
                    createdAt = rws.round.createdAt,
                    note = rws.round.note,
                    scores = rws.scores.map { RoundScore(it.playerId, it.points) },
                )
            },
        )
    }

    private fun uid(): String = UUID.randomUUID().toString()
}
