package com.example.data

data class FriendLocation(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val district: String = "Centre-Ville",
    val statusMessage: String,
    val statusEmoji: String = "✨",
    val batteryPercent: Int = 85,
    val isOnline: Boolean = true,
    val lastSeenFormatted: String = "À l'instant",
    val distanceKm: Double = 0.8,
    val activityTag: String = "Dev & Tech",
    val mutualFriendsCount: Int = 4,
    val streakDays: Int = 12,
    val isFavorite: Boolean = false,
    val isVerified: Boolean = false,
    val chillStatus: String = "Dispo pour chiller",
    val currentlyPlayingMusic: String? = null,
    val isIddetMember: Boolean = true
)

enum class FriendActivityFilter(val label: String, val emoji: String) {
    ALL("Tous les profils", "👥"),
    NEARBY("À proximité (< 2km)", "📍"),
    AVAILABLE("Dispos pour chiller", "⚡"),
    MUSIC_LOUNGE("Musique & Chill", "🎧"),
    STUDY_DEV("Dev & Focus", "💻"),
    GAMING("En jeu / Fun", "🎮")
}

enum class RadarViewMode(val title: String, val icon: String) {
    MAP_VIEW("Carte Interactive", "🗺️"),
    SONAR_RADAR("Sonar Tactique 360°", "📡"),
    CHILL_LOUNGE("Spots & Chill Hub", "🛋️")
}

data class ChillWaveType(
    val id: String,
    val emoji: String,
    val label: String,
    val description: String
)

val PRESET_CHILL_WAVES = listOf(
    ChillWaveType("wave_hello", "👋", "Coucou Chill", "Envoie un salut amical"),
    ChillWaveType("wave_coffee", "☕", "Pause Café", "Propose une pause café en terrasse"),
    ChillWaveType("wave_pizza", "🍕", "Manger un bout", "Organise un casse-croûte"),
    ChillWaveType("wave_game", "🎮", "Session Gaming", "Invite à une partie multijoueur"),
    ChillWaveType("wave_music", "🎧", "Écoute Son", "Partage le son du moment"),
    ChillWaveType("wave_dev", "💻", "Co-Working", "Session de code & dev ensemble")
)

data class ChillSpot(
    val id: String,
    val title: String,
    val category: String,
    val emoji: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val activeUsersCount: Int = 2,
    val creatorName: String = "IDDET Member",
    val distanceKm: Double = 0.6,
    val musicAmbience: String = "Lofi Hip-Hop Chillout"
)

data class UserVibe(
    val emoji: String,
    val text: String,
    val activityType: String,
    val timestamp: Long = System.currentTimeMillis()
)

