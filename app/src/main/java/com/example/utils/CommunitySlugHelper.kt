package com.example.utils

import java.util.regex.Pattern

object CommunitySlugHelper {

    // Regex matching c/slug, /c/slug, /communities/slug, communities/slug, and full URLs
    private val SLUG_PATTERNS = listOf(
        // iddet://community/slug or iddet://c/slug
        Pattern.compile("""(?i)iddet://(?:community|c)/([a-zA-Z0-9_-]{2,})"""),
        // https://hoosthubs-g.onrender.com/c/slug or hoosthubs-g.onrender.com/c/slug
        Pattern.compile("""(?i)(?:https?://)?(?:www\.)?hoosthubs-g\.onrender\.com/c/([a-zA-Z0-9_-]{2,})"""),
        // https://hoosthubs-g.onrender.com/slug
        Pattern.compile("""(?i)(?:https?://)?(?:www\.)?hoosthubs-g\.onrender\.com/([a-zA-Z0-9_-]{2,})"""),
        // https://iddet.app/c/slug or http://... or iddet.app/c/slug
        Pattern.compile("""(?i)(?:https?://)?(?:www\.)?iddet\.app/c/([a-zA-Z0-9_-]{2,})"""),
        // https://iddet.app/communities/slug or iddet.app/communities/slug
        Pattern.compile("""(?i)(?:https?://)?(?:www\.)?iddet\.app/communities/([a-zA-Z0-9_-]{2,})"""),
        // /communities/slug or communities/slug (at boundary)
        Pattern.compile("""(?i)(?:^|[\s(\[{<])/?communities/([a-zA-Z0-9_-]{2,})"""),
        // /c/slug or c/slug (at boundary)
        Pattern.compile("""(?i)(?:^|[\s(\[{<])/?c/([a-zA-Z0-9_-]{2,})"""),
        // @c/slug
        Pattern.compile("""(?i)(?:^|[\s(\[{<])@c/([a-zA-Z0-9_-]{2,})""")
    )

    /**
     * Extracts all unique community slugs from a text.
     * E.g. "Rejoins c/tech et aussi https://iddet.app/c/gaming" -> ["tech", "gaming"]
     */
    fun extractCommunitySlugs(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val foundSlugs = mutableListOf<String>()

        for (pattern in SLUG_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val rawSlug = matcher.group(1) ?: continue
                val cleanSlug = sanitizeSlug(rawSlug)
                if (isValidSlug(cleanSlug) && !foundSlugs.contains(cleanSlug)) {
                    foundSlugs.add(cleanSlug)
                }
            }
        }

        return foundSlugs
    }

    /**
     * Checks if a URL or text string points directly to a community.
     */
    fun isCommunityUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.trim().lowercase()
        return lower.contains("hoosthubs-g.onrender.com/c/") ||
                lower.startsWith("iddet://community/") ||
                lower.startsWith("iddet://c/") ||
                lower.contains("iddet.app/c/") ||
                lower.contains("iddet.app/communities/") ||
                lower.startsWith("c/") ||
                lower.startsWith("/c/") ||
                lower.startsWith("communities/") ||
                lower.startsWith("/communities/")
    }

    /**
     * Extracts a slug from a community link or null if not a community URL.
     */
    fun extractSlugFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return extractCommunitySlugs(url).firstOrNull()
    }

    /**
     * Formats a direct web URL for a community slug (OpenGraph preview + App launch).
     */
    fun formatDirectUrl(slug: String): String {
        return "https://hoosthubs-g.onrender.com/c/${slug.trim().lowercase()}"
    }

    /**
     * Formats the deep link scheme to open the app directly.
     */
    fun formatDeepLink(slug: String): String {
        return "iddet://community/${slug.trim().lowercase()}"
    }

    /**
     * Formats the standard slug tag: c/slug
     */
    fun formatSlugTag(slug: String): String {
        return "c/${slug.trim().lowercase()}"
    }

    private fun sanitizeSlug(raw: String): String {
        var slug = raw.trim()
        while (slug.endsWith(".") || slug.endsWith(",") || slug.endsWith("!") ||
            slug.endsWith("?") || slug.endsWith(")") || slug.endsWith("]") ||
            slug.endsWith(">") || slug.endsWith("/") || slug.endsWith("\"") ||
            slug.endsWith("'")
        ) {
            slug = slug.dropLast(1)
        }
        return slug.lowercase()
    }

    private fun isValidSlug(slug: String): Boolean {
        if (slug.length < 2) return false
        // Exclude generic system paths
        val reserved = setOf("explore", "home", "search", "settings", "notifications", "chat", "direct", "api")
        return !reserved.contains(slug)
    }
}
