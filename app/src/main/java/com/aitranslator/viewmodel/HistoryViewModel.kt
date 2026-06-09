package com.aitranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.aitranslator.data.local.TranslationDatabase
import com.aitranslator.data.model.HistoryItem
import com.aitranslator.data.repository.TranslationRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for [com.aitranslator.ui.history.HistoryActivity].
 *
 * Exposes a LiveData list of all saved translations and provides delete operations.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TranslationRepository by lazy {
        val db = TranslationDatabase.getInstance(application)
        TranslationRepository(db.historyDao())
    }

    /** Reactive list of all history items ordered by most-recent first. */
    val allHistory: LiveData<List<HistoryItem>> = repository.getAllHistory()

    /**
     * Deletes a single [HistoryItem] from the database.
     *
     * @param item The item to remove.
     */
    fun deleteItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteHistoryItem(item)
        }
    }

    /**
     * Wipes the entire translation history.
     */
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
