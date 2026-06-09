package com.aitranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aitranslator.data.local.TranslationDatabase
import com.aitranslator.data.model.HistoryItem
import com.aitranslator.data.model.Language
import com.aitranslator.data.repository.TranslationRepository
import com.aitranslator.utils.NetworkUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for [com.aitranslator.ui.main.MainActivity].
 *
 * Holds all UI state and business logic for the main translation screen.
 * Survives configuration changes (rotation) because it extends [AndroidViewModel].
 *
 * @param application Required by [AndroidViewModel] to access application context.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // -------------------------------------------------------------------------
    // Repository
    // -------------------------------------------------------------------------

    private val repository: TranslationRepository by lazy {
        val db = TranslationDatabase.getInstance(application)
        TranslationRepository(db.historyDao())
    }

    // -------------------------------------------------------------------------
    // Sealed class for UI state
    // -------------------------------------------------------------------------

    /** Represents every possible state the translation operation can be in. */
    sealed class TranslationState {
        object Idle : TranslationState()
        object Loading : TranslationState()
        data class Success(val translatedText: String) : TranslationState()
        data class Error(val message: String) : TranslationState()
    }

    // -------------------------------------------------------------------------
    // LiveData exposed to the UI
    // -------------------------------------------------------------------------

    private val _translationState = MutableLiveData<TranslationState>(TranslationState.Idle)
    /** Translation workflow state (Idle → Loading → Success | Error). */
    val translationState: LiveData<TranslationState> = _translationState

    private val _selectedSourceLanguage = MutableLiveData(Language.SUPPORTED[0]) // English
    /** Currently selected source language. */
    val selectedSourceLanguage: LiveData<Language> = _selectedSourceLanguage

    private val _selectedTargetLanguage = MutableLiveData(Language.SUPPORTED[4]) // French
    /** Currently selected target language. */
    val selectedTargetLanguage: LiveData<Language> = _selectedTargetLanguage

    private val _characterCount = MutableLiveData(0)
    /** Live character count of the input text field. */
    val characterCount: LiveData<Int> = _characterCount

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Validates input and calls the translation API.
     *
     * Updates [translationState] through Loading → Success or Error.
     * On success, the result is also persisted to the local history database.
     *
     * @param inputText The text entered by the user.
     */
    fun translate(inputText: String) {
        // Validate: no empty input
        if (inputText.isBlank()) {
            _translationState.value = TranslationState.Error("Please enter text to translate.")
            return
        }

        // Validate: check internet connectivity
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _translationState.value = TranslationState.Error(
                "No internet connection. Please check your network and try again."
            )
            return
        }

        val source = _selectedSourceLanguage.value ?: return
        val target = _selectedTargetLanguage.value ?: return

        // Validate: source and target must differ
        if (source.code == target.code) {
            _translationState.value = TranslationState.Error(
                "Source and target languages must be different."
            )
            return
        }

        viewModelScope.launch {
            _translationState.value = TranslationState.Loading

            try {
                val translated = repository.translate(inputText, source.code, target.code)
                _translationState.value = TranslationState.Success(translated)

                // Auto-save to history
                saveToHistory(inputText, translated, source, target)

            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(
                    e.message ?: "An unexpected error occurred. Please try again."
                )
            }
        }
    }

    /**
     * Swaps source and target languages.
     * Also resets translation state back to [TranslationState.Idle].
     */
    fun swapLanguages() {
        val current = _selectedSourceLanguage.value
        _selectedSourceLanguage.value = _selectedTargetLanguage.value
        _selectedTargetLanguage.value = current
        _translationState.value = TranslationState.Idle
    }

    /**
     * Updates the source language selection from the dropdown.
     *
     * @param position Index into [Language.SUPPORTED].
     */
    fun setSourceLanguage(position: Int) {
        if (position in Language.SUPPORTED.indices) {
            _selectedSourceLanguage.value = Language.SUPPORTED[position]
        }
    }

    /**
     * Updates the target language selection from the dropdown.
     *
     * @param position Index into [Language.SUPPORTED].
     */
    fun setTargetLanguage(position: Int) {
        if (position in Language.SUPPORTED.indices) {
            _selectedTargetLanguage.value = Language.SUPPORTED[position]
        }
    }

    /**
     * Updates the live character count whenever the input text changes.
     *
     * @param text Current contents of the input field.
     */
    fun updateCharacterCount(text: String) {
        _characterCount.value = text.length
    }

    /**
     * Resets the translation state to [TranslationState.Idle].
     * Called when the user taps the Clear button.
     */
    fun clearTranslation() {
        _translationState.value = TranslationState.Idle
        _characterCount.value = 0
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Persists a completed translation to Room in the background. */
    private fun saveToHistory(
        original: String,
        translated: String,
        source: Language,
        target: Language
    ) {
        viewModelScope.launch {
            try {
                repository.saveToHistory(
                    HistoryItem(
                        originalText = original,
                        translatedText = translated,
                        sourceLanguage = source.name,
                        targetLanguage = target.name,
                        sourceCode = source.code,
                        targetCode = target.code
                    )
                )
            } catch (e: Exception) {
                // History save failure is non-critical; swallow silently
            }
        }
    }
}
