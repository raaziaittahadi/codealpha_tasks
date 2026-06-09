package com.aitranslator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aitranslator.data.model.HistoryItem

/**
 * Room database that persists translation history locally on the device.
 *
 * Singleton access via [getInstance].
 */
@Database(
    entities = [HistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class TranslationDatabase : RoomDatabase() {

    /** DAO for reading and writing [HistoryItem] records. */
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TranslationDatabase? = null

        /**
         * Returns the singleton [TranslationDatabase], creating it if needed.
         *
         * @param context Application context.
         */
        fun getInstance(context: Context): TranslationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TranslationDatabase::class.java,
                    "translation_history_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
