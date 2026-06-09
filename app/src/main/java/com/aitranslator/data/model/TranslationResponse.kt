package com.aitranslator.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response returned by the LibreTranslate API after a successful translation.
 *
 * @param translatedText The translated string.
 */
data class TranslationResponse(
    @SerializedName("translatedText")
    val translatedText: String
)
