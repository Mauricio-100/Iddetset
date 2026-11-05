package com.example.ui.components

import androidx.compose.ui.graphics.Color

data class CategoryInfo(
    val id: String, // e.g. "@(fun)"
    val name: String, // e.g. "Fun"
    val emoji: String, // e.g. "🎭"
    val description: String,
    val color: Color,
    val serverCategory: String = name
)

val ACTFILE_SERVER_CATEGORIES = listOf(
    "Fun", "Amour", "Motivation", "Tech", "Sport",
    "Musique", "Actu", "Business", "Spiritualité", "Autres"
)

val APP_CATEGORIES = listOf(
    CategoryInfo("@(fun)", "Fun", "🎭", "Humour, mèmes, blagues et moments drôles.", Color(0xFFFBBF24), "Fun"),
    CategoryInfo("@(amour)", "Amour", "❤️", "Relations, amitié, conseils de vie et famille.", Color(0xFFEC4899), "Amour"),
    CategoryInfo("@(motivation)", "Motivation", "🔥", "Inspiration, discipline et dépassement de soi.", Color(0xFFEF4444), "Motivation"),
    CategoryInfo("@(tech)", "Tech", "💻", "Nouvelles technologies, code et innovations.", Color(0xFF34D399), "Tech"),
    CategoryInfo("@(sport)", "Sport", "⚽", "Football, fitness, musculation et athlétisme.", Color(0xFF10B981), "Sport"),
    CategoryInfo("@(musique)", "Musique", "🎵", "Partage de sons, chansons et instruments.", Color(0xFFF472B6), "Musique"),
    CategoryInfo("@(actu)", "Actu", "📰", "Actualités mondiales, informations et tendances.", Color(0xFF6B7280), "Actu"),
    CategoryInfo("@(business)", "Business", "💼", "Entrepreneuriat, finance, startups et marketing.", Color(0xFF0F172A), "Business"),
    CategoryInfo("@(spiritualite)", "Spiritualité", "🕊️", "Méditation, philosophie, foi et sagesse.", Color(0xFF8B5CF6), "Spiritualité"),
    CategoryInfo("@(autres)", "Autres", "🌐", "Discussions libres, quotidien et découvertes.", Color(0xFF3B82F6), "Autres"),
    
    // Sub-aliases for rich discovery
    CategoryInfo("@(gaming)", "Gaming", "🎮", "Jeux vidéo, gameplays, astuces et consoles.", Color(0xFF818CF8), "Tech"),
    CategoryInfo("@(cinema)", "Cinéma", "🎬", "Films, séries, critiques et bandes-annonces.", Color(0xFFF87171), "Fun"),
    CategoryInfo("@(science)", "Science", "🔬", "Découvertes, astronomie, physique et biologie.", Color(0xFF60A5FA), "Tech"),
    CategoryInfo("@(art)", "Art", "🎨", "Dessin, peinture, design graphique et sculpture.", Color(0xFFF59E0B), "Autres"),
    CategoryInfo("@(crypto)", "Crypto", "🪙", "Bitcoin, Ethereum, web3 et blockchain.", Color(0xFFF59E0B), "Business"),
    CategoryInfo("@(anime)", "Anime", "💮", "Mangas, animés japonais et culture otaku.", Color(0xFFEC4899), "Fun"),
    CategoryInfo("@(finance)", "Finance", "📈", "Économie, investissement et finances personnelles.", Color(0xFF059669), "Business"),
    CategoryInfo("@(cuisine)", "Cuisine", "🍳", "Recettes, gastronomie, restaurants et gourmandises.", Color(0xFFF97316), "Autres"),
    CategoryInfo("@(voyage)", "Voyage", "✈️", "Destinations, aventures, paysages et conseils.", Color(0xFF06B6D4), "Autres"),
    CategoryInfo("@(mode)", "Mode", "👗", "Vêtements, tendances, styles et beauté.", Color(0xFFD946EF), "Autres"),
    CategoryInfo("@(livres)", "Livres", "📚", "Romans, littérature, poésie et bibliothèques.", Color(0xFF1E3A8A), "Spiritualité"),
    CategoryInfo("@(memes)", "Mèmes", "🤪", "Mèmes d'internet, culture web et délires.", Color(0xFFEAB308), "Fun"),
    CategoryInfo("@(fitness)", "Fitness", "💪", "Musculation, cardio, yoga et nutrition.", Color(0xFFEF4444), "Sport"),
    CategoryInfo("@(animaux)", "Animaux", "🐱", "Chats, chiens, astuces de soins et vidéos mignonnes.", Color(0xFFF59E0B), "Autres"),
    CategoryInfo("@(philosophie)", "Philosophie", "🤔", "Pensées, citations, réflexion et sagesse.", Color(0xFF8B5CF6), "Spiritualité"),
    CategoryInfo("@(code)", "Code", "💻", "Programmation, Kotlin, Python et algorithmes.", Color(0xFF06B6D4), "Tech")
)

fun normalizeToActfileCategory(raw: String?): String {
    if (raw.isNullOrBlank()) return "Autres"
    if (ACTFILE_SERVER_CATEGORIES.contains(raw)) return raw
    val cleaned = raw.replace("@(", "").replace(")", "").trim()
    val match = ACTFILE_SERVER_CATEGORIES.firstOrNull { it.equals(cleaned, ignoreCase = true) }
    if (match != null) return match
    
    val catInfo = APP_CATEGORIES.firstOrNull { 
        it.id.equals(raw, ignoreCase = true) || 
        it.name.equals(raw, ignoreCase = true) ||
        it.id.equals("@($cleaned)", ignoreCase = true) ||
        it.name.equals(cleaned, ignoreCase = true)
    }
    if (catInfo != null) return catInfo.serverCategory

    return when {
        cleaned.contains("fun", ignoreCase = true) || cleaned.contains("humour", ignoreCase = true) || cleaned.contains("mème", ignoreCase = true) || cleaned.contains("meme", ignoreCase = true) -> "Fun"
        cleaned.contains("amour", ignoreCase = true) || cleaned.contains("love", ignoreCase = true) || cleaned.contains("relation", ignoreCase = true) -> "Amour"
        cleaned.contains("moti", ignoreCase = true) || cleaned.contains("inspi", ignoreCase = true) -> "Motivation"
        cleaned.contains("tech", ignoreCase = true) || cleaned.contains("cod", ignoreCase = true) || cleaned.contains("dev", ignoreCase = true) || cleaned.contains("game", ignoreCase = true) -> "Tech"
        cleaned.contains("sport", ignoreCase = true) || cleaned.contains("fit", ignoreCase = true) || cleaned.contains("foot", ignoreCase = true) -> "Sport"
        cleaned.contains("musi", ignoreCase = true) || cleaned.contains("sound", ignoreCase = true) || cleaned.contains("song", ignoreCase = true) || cleaned.contains("audio", ignoreCase = true) -> "Musique"
        cleaned.contains("actu", ignoreCase = true) || cleaned.contains("news", ignoreCase = true) || cleaned.contains("info", ignoreCase = true) -> "Actu"
        cleaned.contains("biz", ignoreCase = true) || cleaned.contains("busines", ignoreCase = true) || cleaned.contains("finan", ignoreCase = true) || cleaned.contains("crypto", ignoreCase = true) -> "Business"
        cleaned.contains("spirit", ignoreCase = true) || cleaned.contains("philo", ignoreCase = true) || cleaned.contains("foi", ignoreCase = true) || cleaned.contains("sagesse", ignoreCase = true) -> "Spiritualité"
        else -> "Autres"
    }
}

fun getCategoryById(id: String?): CategoryInfo? {
    if (id == null || id.isBlank()) return null
    return APP_CATEGORIES.firstOrNull { 
        it.id.equals(id, ignoreCase = true) || 
        it.name.equals(id, ignoreCase = true) ||
        it.serverCategory.equals(id, ignoreCase = true) ||
        id.contains(it.id, ignoreCase = true) ||
        id.contains(it.name, ignoreCase = true) ||
        it.id.contains(id, ignoreCase = true) ||
        it.name.contains(id, ignoreCase = true)
    }
}

