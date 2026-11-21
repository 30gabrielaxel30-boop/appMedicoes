package com.keyhistory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class, SearchTokenEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KeyHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "keyhistory.db"

        fun build(context: Context, inMemory: Boolean = false): KeyHistoryDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, KeyHistoryDatabase::class.java)
            } else {
                Room.databaseBuilder(context, KeyHistoryDatabase::class.java, NAME)
            }
            return builder.build()
        }
    }
}
