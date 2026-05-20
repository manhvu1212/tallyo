package io.github.manhvu1212.tallyo.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val zeroSum: Boolean,
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val resting: Boolean,
    val orderIndex: Int,
)

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class RoundEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val createdAt: Long,
    val note: String?,
    val orderIndex: Int,
)

@Entity(
    tableName = "scores",
    primaryKeys = ["roundId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playerId")],
)
data class ScoreEntity(
    val roundId: String,
    val playerId: String,
    val points: Int,
)

data class RoundWithScores(
    @Embedded val round: RoundEntity,
    @Relation(parentColumn = "id", entityColumn = "roundId")
    val scores: List<ScoreEntity>,
)

data class SessionWithDetails(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val players: List<PlayerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
        entity = RoundEntity::class,
    )
    val rounds: List<RoundWithScores>,
)
