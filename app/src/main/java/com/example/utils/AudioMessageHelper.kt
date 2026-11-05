package com.example.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AudioMessageHelper {

    private const val TAG = "AudioMessageHelper"

    /**
     * Determines whether the given string content or type represents a voice/audio message.
     */
    fun isAudioContent(content: String?, type: String? = null): Boolean {
        if (content.isNullOrBlank()) return false

        // Check explicit message type
        val cleanType = type?.lowercase()
        if (cleanType == "audio" || cleanType == "voice" || cleanType == "audio_sending" || cleanType == "audio_error") {
            return true
        }

        val trimmed = content.trim()

        // Synthetic voice markdown or voice schema URI
        if (trimmed.startsWith("[Voice Message]") || trimmed.startsWith("voice://") || trimmed.contains("voice://")) return true

        // Base64 Data URI or raw base64 prefix
        if (trimmed.startsWith("data:audio") ||
            trimmed.startsWith("data:application/octet-stream") ||
            trimmed.startsWith("data:video/mp4") ||
            (trimmed.contains("base64,") && (trimmed.startsWith("data:") || trimmed.length > 50))
        ) {
            return true
        }

        // Common audio file paths or remote URLs
        val lower = trimmed.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/")) {
            if (lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                lower.endsWith(".ogg") || lower.endsWith(".aac") || lower.endsWith(".3gp") || lower.endsWith(".flac")
            ) {
                return true
            }
            if (lower.contains("voice_messages") || lower.contains("/audio/") || lower.contains("recording") ||
                (lower.contains("cloudinary.com") && lower.contains("/upload/"))
            ) {
                return true
            }
        }

        // Long contiguous base64 raw string without spaces/newlines (e.g., encoded audio payload)
        if (trimmed.length > 80 && !trimmed.contains(" ") && !trimmed.contains("\n")) {
            val isBase64Pattern = trimmed.matches(Regex("^[A-Za-z0-9+/=]+$"))
            if (isBase64Pattern) return true
        }

        return false
    }

    /**
     * Returns a concise user-friendly text preview for list views (e.g. Conversation lists).
     */
    fun getPreviewText(content: String?, type: String? = null): String {
        if (content.isNullOrBlank()) return ""
        if (isAudioContent(content, type)) {
            return "🎤 Message vocal"
        }
        return content
    }
}
