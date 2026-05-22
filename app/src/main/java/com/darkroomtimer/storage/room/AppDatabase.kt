package com.darkroomtimer.storage.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [EnlargerProfileEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enlargerProfileDao(): EnlargerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "darkroom_timer_database"
                )
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
