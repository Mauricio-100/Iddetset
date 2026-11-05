package com.example.data

data class TrendingTopic(
    val id: String,
    val title: String,
    val source: String,
    val link: String,
    val pubDateFormatted: String,
    val category: TrendingCategory,
    val snippet: String? = null,
    val tags: List<String> = emptyList(),
    val isHot: Boolean = false,
    val imageUrl: String? = null,
    val keyTakeaways: List<String> = emptyList(),
    val readTimeMin: Int = 3,
    val isBookmarked: Boolean = false,
    val fullContentPreview: String? = null
)

enum class TrendingCategory(val label: String, val emoji: String, val searchQuery: String) {
    ALL("Tous", "🔥", "Intelligence Artificielle OR Markdown OR tech OR programmation OR Android"),
    AI("IA & LLMs", "🤖", "Intelligence Artificielle OR LLM OR Gemini OR Claude OR ChatGPT OR Mistral"),
    DEV("Dev & Outils", "⚡", "programmation OR open source OR developpeur OR Kotlin OR Python"),
    MARKDOWN("Markdown & Docs", "📝", "Markdown OR documentation OR Obsidian OR Notion OR GitHub"),
    OPEN_SOURCE("Open Source", "🌐", "open source OR GitHub OR Linux OR logiciel libre"),
    CYBER("Cybersécurité", "🛡️", "cybersecurite OR piratage OR vulnerabilite OR chiffrement OR privacy"),
    SCIENCE("Sciences & Futur", "🚀", "espace OR science OR NASA OR robotique OR quantique")
}
