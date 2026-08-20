package com.budgetnotes.app.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

/**
 * Copies text and clears the clipboard after [clearAfterMs] for sensitive fields.
 */
class SecureClipboard(context: Context) {
    private val clipboard =
        context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)

    fun copy(label: String, value: String, clearAfterMs: Long = DEFAULT_CLEAR_MS) {
        if (value.isBlank()) return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        val gen = generation.incrementAndGet()
        handler.postDelayed(
            {
                if (generation.get() != gen) return@postDelayed
                clearClipboard()
            },
            clearAfterMs,
        )
    }

    private fun clearClipboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {
            // Some OEMs restrict clipboard clears
        }
    }

    companion object {
        const val DEFAULT_CLEAR_MS = 45_000L
    }
}
