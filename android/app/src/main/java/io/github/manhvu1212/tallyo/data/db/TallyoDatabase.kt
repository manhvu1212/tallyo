package io.github.manhvu1212.tallyo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        ScoreEntity::class,
        CustomGameEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TallyoDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: TallyoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN game TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_games` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_games ADD COLUMN defaultZeroSum INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): TallyoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TallyoDatabase::class.java,
                "tallyo.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
