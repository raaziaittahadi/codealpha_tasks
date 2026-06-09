package com.aitranslator.data.repository

import com.aitranslator.BuildConfig
import com.aitranslator.data.local.HistoryDao
import com.aitranslator.data.model.HistoryItem
import com.aitranslator.data.model.TranslationRequest
import com.aitranslator.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that bridges the ViewModel with the network API and local Room database.
 *
 * All suspend functions switch to [Dispatchers.IO] internally so the ViewModel
 * can call them from the main scope without blocking the UI thread.
 *
 * @param historyDao Room DAO injected for testability.
 */
class TranslationRepository(private val historyDao: HistoryDao) {

    private val apiService = RetrofitClient.translationApiService

    // -------------------------------------------------------------------------
    // Translation
    // -------------------------------------------------------------------------

    /**
     * Sends [text] to the LibreTranslate API and returns the translated string.
     *
     * @param text         The text to translate.
     * @param sourceCode   ISO code of the source language (e.g. "en").
     * @param targetCode   ISO code of the target language (e.g. "fr").
     * @return             The translated text.
     * @throws Exception   On network error or non-successful HTTP response.
     */
    suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String
    ): String = withContext(Dispatchers.IO) {
        val request = TranslationRequest(
            q = text,
            source = sourceCode,
            target = targetCode,
            apiKey = BuildConfig.LIBRE_TRANSLATE_API_KEY
        )
        val response = apiService.translate(request)

        when {
            response.isSuccessful -> {
                response.body()?.translatedText
                    ?: throw Exception("Empty response body from translation API")
            }
            response.code() == 403 -> throw Exception("Invalid API key. Please check your LIBRE_TRANSLATE_API_KEY.")
            response.code() == 429 -> throw Exception("Rate limit exceeded. Please try again later.")
            response.code() == 400 -> throw Exception("Bad request: unsupported language pair or malformed input.")
            else -> throw Exception("API error ${response.code()}: ${response.message()}")
        }
    }

    // -------------------------------------------------------------------------
    // History — write operations
    // -------------------------------------------------------------------------

    /**
     * Saves a completed translation to the local history database.
     *
     * @return The auto-generated ID of the inserted row.
     */
    suspend fun saveToHistory(item: HistoryItem): Long =
        withContext(Dispatchers.IO) { historyDao.insertHistory(item) }

    /**
     * Deletes a specific [HistoryItem] from the database.
     */
    suspend fun deleteHistoryItem(item: HistoryItem) =
        withContext(Dispatchers.IO) { historyDao.deleteHistory(item) }

    /**
     * Removes all history records from the database.
     */
    suspend fun clearAllHistory() =
        withContext(Dispatchers.IO) { historyDao.clearAllHistory() }

    // -------------------------------------------------------------------------
    // History — read operations
    // -------------------------------------------------------------------------

    /**
     * LiveData stream of all history items, ordered by most recent first.
     * Observed by [com.aitranslator.ui.history.HistoryActivity].
     */
    fun getAllHistory() = historyDao.getAllHistory()
}
