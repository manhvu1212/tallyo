package io.github.manhvu1212.tallyo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: String): Flow<SessionWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRound(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScores(scores: List<ScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: RoundEventEntity)

    @Query("DELETE FROM round_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("UPDATE round_events SET roundId = :roundId WHERE sessionId = :sessionId AND roundId IS NULL")
    suspend fun linkPendingEventsToRound(sessionId: String, roundId: String)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE sessions SET updatedAt = :ts WHERE id = :id")
    suspend fun touchSession(id: String, ts: Long)

    @Query("UPDATE sessions SET name = :name, updatedAt = :ts WHERE id = :id")
    suspend fun renameSession(id: String, name: String, ts: Long)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteRound(roundId: String)

    @Query("DELETE FROM scores WHERE roundId = :roundId")
    suspend fun clearRoundScores(roundId: String)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: String)

    @Query("SELECT COUNT(*) FROM players WHERE sessionId = :sessionId")
    suspend fun playerCount(sessionId: String): Int

    @Transaction
    suspend fun createSession(
        session: SessionEntity,
        players: List<PlayerEntity>,
    ) {
        upsertSession(session)
        if (players.isNotEmpty()) upsertPlayers(players)
    }

    @Transaction
    suspend fun appendRound(
        sessionId: String,
        round: RoundEntity,
        scores: List<ScoreEntity>,
        ts: Long,
    ) {
        upsertRound(round)
        if (scores.isNotEmpty()) upsertScores(scores)
        linkPendingEventsToRound(sessionId, round.id)
        touchSession(sessionId, ts)
    }

    @Transaction
    suspend fun replaceRoundScores(
        sessionId: String,
        roundId: String,
        note: String?,
        scores: List<ScoreEntity>,
        ts: Long,
    ) {
        clearRoundScores(roundId)
        // Update note via a fresh row write — read the round to get orderIndex.
        // Simpler: a dedicated UPDATE statement.
        updateRoundNote(roundId, note)
        if (scores.isNotEmpty()) upsertScores(scores)
        touchSession(sessionId, ts)
    }

    @Query("UPDATE rounds SET note = :note WHERE id = :roundId")
    suspend fun updateRoundNote(roundId: String, note: String?)

    @Query("SELECT * FROM custom_games ORDER BY name ASC")
    fun observeCustomGames(): Flow<List<CustomGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomGame(game: CustomGameEntity)

    @Query("DELETE FROM custom_games WHERE name = :name")
    suspend fun deleteCustomGame(name: String)
}
