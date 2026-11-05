package com.example.ui.components

import java.net.URI

object VideoUrlHelper {

    enum class VideoType {
        YOUTUBE,
        TIKTOK,
        INSTAGRAM,
        DIRECT_FILE,
        EMBED_WEB
    }

    data class VideoInfo(
        val originalUrl: String,
        val type: VideoType,
        val embedUrl: String?,
        val videoId: String? = null
    )

    fun isVideoUrl(rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (url.isBlank()) return false

        val lowerUrl = url.lowercase()

        // Direct Video File extensions or CDN patterns
        val directExtensions = listOf(".mp4", ".webm", ".m3u8", ".mov", ".mkv", ".avi", ".ogv", ".3gp", ".flv")
        if (directExtensions.any { lowerUrl.contains(it) }) return true

        // GitHub Video Attachments / Raw Assets
        if (lowerUrl.contains("github.com") && (lowerUrl.contains("/user-attachments/assets/") || lowerUrl.contains("/releases/download/") || lowerUrl.contains("raw.githubusercontent.com"))) {
            return true
        }
        if (lowerUrl.contains("github-production-user-asset") || lowerUrl.contains("github-assets")) {
            return true
        }

        // Cloudinary Video uploads
        if (lowerUrl.contains("cloudinary.com") && lowerUrl.contains("/video/upload/")) {
            return true
        }

        // Popular platform hosts
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) return true
        if (lowerUrl.contains("tiktok.com") || lowerUrl.contains("vt.tiktok.com")) return true
        if (lowerUrl.contains("instagram.com/reel/") || lowerUrl.contains("instagram.com/p/") || lowerUrl.contains("instagr.am/p/")) return true
        if (lowerUrl.contains("vimeo.com") || lowerUrl.contains("dailymotion.com") || lowerUrl.contains("streamable.com") || lowerUrl.contains("twitch.tv")) return true

        // URL hints
        if (lowerUrl.contains("type=video") || lowerUrl.contains("format=mp4") || lowerUrl.contains("video_url") || lowerUrl.contains("url-video") || lowerUrl.contains("video-url")) return true

        return false
    }

    fun parseVideoInfo(rawUrl: String): VideoInfo {
        var url = rawUrl.trim()
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // Valid prefix
        } else if (url.startsWith("//")) {
            url = "https:$url"
        } else {
            url = "https://$url"
        }

        val lowerUrl = url.lowercase()

        // YouTube
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            val videoId = extractYouTubeId(url)
            val embedUrl = if (videoId != null) "https://www.youtube.com/embed/$videoId?autoplay=0&controls=1&enablejsapi=1" else url
            return VideoInfo(url, VideoType.YOUTUBE, embedUrl, videoId)
        }

        // TikTok
        if (lowerUrl.contains("tiktok.com")) {
            val videoId = extractTikTokId(url)
            val embedUrl = if (videoId != null) "https://www.tiktok.com/embed/v2/$videoId" else url
            return VideoInfo(url, VideoType.TIKTOK, embedUrl, videoId)
        }

        // Instagram
        if (lowerUrl.contains("instagram.com/reel/") || lowerUrl.contains("instagram.com/p/") || lowerUrl.contains("instagr.am/p/")) {
            val code = extractInstagramCode(url)
            val embedUrl = if (code != null) "https://www.instagram.com/p/$code/embed/" else url
            return VideoInfo(url, VideoType.INSTAGRAM, embedUrl, code)
        }

        // Direct Video
        val directExtensions = listOf(".mp4", ".webm", ".m3u8", ".mov", ".mkv", ".avi", ".ogv", ".3gp", ".flv")
        if (directExtensions.any { lowerUrl.contains(it) } ||
            (lowerUrl.contains("github.com") && lowerUrl.contains("/user-attachments/assets/")) ||
            lowerUrl.contains("raw.githubusercontent.com") ||
            lowerUrl.contains("github-production-user-asset") ||
            (lowerUrl.contains("cloudinary.com") && lowerUrl.contains("/video/upload/"))
        ) {
            return VideoInfo(url, VideoType.DIRECT_FILE, url)
        }

        return VideoInfo(url, VideoType.EMBED_WEB, url)
    }

    fun getThumbnailUrl(videoInfo: VideoInfo): String? {
        return when (videoInfo.type) {
            VideoType.YOUTUBE -> {
                if (!videoInfo.videoId.isNullOrBlank()) {
                    "https://img.youtube.com/vi/${videoInfo.videoId}/hqdefault.jpg"
                } else null
            }
            VideoType.DIRECT_FILE -> {
                if (videoInfo.originalUrl.contains("cloudinary.com") && videoInfo.originalUrl.contains("/video/upload/")) {
                    videoInfo.originalUrl
                        .replace("/video/upload/", "/video/upload/so_0,w_720,c_scale/")
                        .substringBeforeLast(".") + ".jpg"
                } else null
            }
            else -> null
        }
    }

    fun getProviderName(type: VideoType): String {
        return when (type) {
            VideoType.YOUTUBE -> "YouTube"
            VideoType.TIKTOK -> "TikTok"
            VideoType.INSTAGRAM -> "Instagram"
            VideoType.DIRECT_FILE -> "Vidéo"
            VideoType.EMBED_WEB -> "Web Video"
        }
    }

    private fun extractYouTubeId(url: String): String? {
        return try {
            val regex = "(?i)(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?|shorts)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})".toRegex()
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTikTokId(url: String): String? {
        return try {
            val regex = "video\\/(\\d+)".toRegex()
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractInstagramCode(url: String): String? {
        return try {
            val regex = "(?:reel|p)\\/([^\\/?#]+)".toRegex()
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}
