package fr.mathgl.darkroomtimer.storage.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.mathgl.darkroomtimer.development.DevelopmentDao
import fr.mathgl.darkroomtimer.development.DevelopmentProfileEntity
import fr.mathgl.darkroomtimer.development.DevelopmentStepTypeConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [EnlargerProfileEntity::class, DevelopmentProfileEntity::class],
    version = 2
)
@TypeConverters(DevelopmentStepTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enlargerProfileDao(): EnlargerProfileDao
    abstract fun developmentDao(): DevelopmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `development_profiles` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`navigationModeIndex` INTEGER NOT NULL, " +
                        "`stepsJson` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "darkroom_timer_database"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.enlargerProfileDao()
                    dao.insert(
                        EnlargerProfileEntity(
                            id = 0,
                            name = "Idéal",
                            turnOnDelayMs = 0,
                            riseTimeMs = 0,
                            riseTimeEquivMs = 0,
                            turnOffDelayMs = 0,
                            fallTimeMs = 0,
                            fallTimeEquivMs = 0
                        )
                    )
                }
            }
        }
    }
}
