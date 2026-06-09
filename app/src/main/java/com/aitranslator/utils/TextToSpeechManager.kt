package com.aitranslator.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Wrapper around Android's [TextToSpeech] engine.
 *
 * Call [initialize] once (e.g. in `onCreate`) and [shutdown] in `onDestroy`.
 */
class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                Log.d(TAG, "TTS engine initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    /**
     * Speaks [text] aloud using the TTS engine.
     *
     * @param text       The string to speak.
     * @param languageCode ISO language code (e.g. "fr", "ar", "zh").
     *                   Falls back to the device default if the locale is unavailable.
     */
    fun speak(text: String, languageCode: String) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }
        if (text.isBlank()) return

        val locale = mapCodeToLocale(languageCode)
        val result = tts!!.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $languageCode not supported by TTS, using default")
            tts!!.language = Locale.getDefault()
        }

        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /**
     * Stops any ongoing speech synthesis.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Releases TTS resources. Call this from the hosting Activity's `onDestroy`.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    /** Returns `true` if the TTS engine is initialized and ready. */
    fun isAvailable(): Boolean = isReady

    // Maps ISO 639-1 codes to the closest supported [Locale].
    private fun mapCodeToLocale(code: String): Locale = when (code) {
        "en" -> Locale.ENGLISH
        "fr" -> Locale.FRENCH
        "de" -> Locale.GERMAN
        "zh" -> Locale.CHINESE
        "es" -> Locale("es")
        "ar" -> Locale("ar")
        "hi" -> Locale("hi")
        "ur" -> Locale("ur")
        else -> Locale.getDefault()
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val UTTERANCE_ID = "ai_translator_utterance"
    }
}
