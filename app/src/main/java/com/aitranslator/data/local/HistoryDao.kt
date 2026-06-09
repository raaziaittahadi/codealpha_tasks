package com.aitranslator.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.aitranslator.data.model.HistoryItem

/**
 * Room DAO for translation history CRUD operations.
 *
 * All heavy operations run on a background thread via coroutines (suspend funs)
 * or are exposed as [LiveData] for reactive UI updates.
 */
@Dao
interface HistoryDao {

    /**
     * Returns all history items ordered by most recent first as a [LiveData] stream.
     * The UI observes this and re-renders whenever rows change.
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<HistoryItem>>

    /**
     * Returns all history items as a plain list (used for one-shot reads).
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(): List<HistoryItem>

    /**
     * Inserts a new history item. On conflict the row is replaced.
     *
     * @return The auto-generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    /**
     * Deletes a specific history item.
     */
    @Delete
    suspend fun deleteHistory(item: HistoryItem)

    /**
     * Deletes every history item (clear all).
     */
    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory()

    /**
     * Returns the count of stored translations.
     */
    @Query("SELECT COUNT(*) FROM translation_history")
    suspend fun getHistoryCount(): Int
}
