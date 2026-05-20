package io.github.manhvu1212.tallyo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        ScoreEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TallyoDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: TallyoDatabase? = null

        fun get(context: Context): TallyoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TallyoDatabase::class.java,
                "tallyo.db",
            ).build().also { instance = it }
        }
    }
}
