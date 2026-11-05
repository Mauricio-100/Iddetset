package com.example.utils

object UrlHelper {
    fun fixCloudinaryUrl(url: String?): String? {
        if (url == null) return null
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        
        val baseUrl = if (trimmed.contains("?")) trimmed.substringBefore("?") else trimmed

        if (baseUrl.contains("res.cloudinary.com")) {
            val lower = baseUrl.lowercase()
            // Do not alter video and audio formats
            if (lower.contains("/video/upload/") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".mp3") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".wav") ||
                lower.endsWith(".ogg") ||
                lower.endsWith(".m3u8")
            ) {
                return baseUrl
            }

            val lastSlash = baseUrl.lastIndexOf('/')
            val lastDot = baseUrl.lastIndexOf('.')
            if (lastDot > lastSlash) {
                return baseUrl.substring(0, lastDot) + ".webp"
            } else {
                return "$baseUrl.webp"
            }
        }
        return baseUrl
    }
}

