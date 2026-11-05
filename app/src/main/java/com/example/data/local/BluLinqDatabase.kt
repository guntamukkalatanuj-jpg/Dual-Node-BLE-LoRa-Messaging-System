package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BluLinqDatabase : RoomDatabase() {
    abstract fun messageLogDao(): MessageLogDao

    companion object {
        @Volatile
        private var INSTANCE: BluLinqDatabase? = null

        fun getInstance(context: Context): BluLinqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BluLinqDatabase::class.java,
                    "blulinq_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
