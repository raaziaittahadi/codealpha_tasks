package com.aitranslator.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Room entity that represents a single saved translation in the history database.
 *
 * @param id             Auto-generated primary key.
 * @param originalText   The text that was translated.
 * @param translatedText The result of the translation.
 * @param sourceLanguage Human-readable source language name (e.g. "English").
 * @param targetLanguage Human-readable target language name (e.g. "French").
 * @param sourceCode     ISO code for the source language (e.g. "en").
 * @param targetCode     ISO code for the target language (e.g. "fr").
 * @param timestamp      Unix epoch millis when the translation was performed.
 */
@Parcelize
@Entity(tableName = "translation_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceCode: String,
    val targetCode: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
