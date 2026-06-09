package com.aitranslator.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body sent to the LibreTranslate API.
 *
 * @param q      The text to translate.
 * @param source ISO language code of the source language (e.g. "en", "fr").
 * @param target ISO language code of the target language.
 * @param apiKey Optional API key required by some LibreTranslate instances.
 */
data class TranslationRequest(
    @SerializedName("q")
    val q: String,

    @SerializedName("source")
    val source: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("api_key")
    val apiKey: String = ""
)
