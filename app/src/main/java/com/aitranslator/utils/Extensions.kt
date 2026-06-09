package com.aitranslator.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Copies [text] to the system clipboard and shows a brief Toast confirmation.
 *
 * @param text  The string to copy.
 * @param label Optional clipboard label (visible in clipboard managers).
 */
fun Context.copyToClipboard(text: String, label: String = "Translated Text") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

/**
 * Shows a Snackbar anchored to [this] view.
 *
 * @param message      The message to display.
 * @param duration     Snackbar duration constant (default [Snackbar.LENGTH_SHORT]).
 * @param actionLabel  Optional action button label.
 * @param action       Optional action button click handler.
 */
fun View.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
) {
    val snackbar = Snackbar.make(this, message, duration)
    if (actionLabel != null && action != null) {
        snackbar.setAction(actionLabel) { action() }
    }
    snackbar.show()
}

/**
 * Converts a Unix epoch millisecond timestamp to a human-readable date/time string.
 *
 * @param pattern SimpleDateFormat pattern (default "MMM dd, yyyy • HH:mm").
 */
fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy • HH:mm"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

/** Makes this view [View.VISIBLE]. */
fun View.show() { visibility = View.VISIBLE }

/** Makes this view [View.GONE]. */
fun View.hide() { visibility = View.GONE }
