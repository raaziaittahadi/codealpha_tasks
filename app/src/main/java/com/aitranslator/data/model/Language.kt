package com.aitranslator.data.model

/**
 * Represents a language option shown in the dropdowns.
 *
 * @param name Human-readable label (e.g. "English").
 * @param code ISO 639-1 language code used by the translation API (e.g. "en").
 */
data class Language(
    val name: String,
    val code: String
) {
    override fun toString(): String = name

    companion object {
        /** Full list of supported languages shown in the language pickers. */
        val SUPPORTED = listOf(
            Language("English",  "en"),
            Language("Urdu",     "ur"),
            Language("Hindi",    "hi"),
            Language("Arabic",   "ar"),
            Language("French",   "fr"),
            Language("Spanish",  "es"),
            Language("Chinese",  "zh"),
            Language("German",   "de")
        )

        /** Returns the [Language] matching the given ISO [code], or English as fallback. */
        fun fromCode(code: String): Language =
            SUPPORTED.firstOrNull { it.code == code } ?: SUPPORTED[0]
    }
}
