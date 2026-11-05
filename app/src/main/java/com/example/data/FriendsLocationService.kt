package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

object FriendsLocationService {

    // Default friendly friends & IDDET users list distributed around realistic anchor points
    private val initialFriends = listOf(
        FriendLocation(
            id = "friend_alex",
            username = "Alex_Dev",
            displayName = "Alexandre M.",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8584,
            longitude = 2.2945,
            city = "Paris",
            district = "Tour Eiffel / Champ de Mars",
            statusMessage = "En train de tester le radar IDDET en terrasse ☕💻",
            statusEmoji = "☕",
            batteryPercent = 92,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 0.35,
            activityTag = "Dev Kotlin",
            mutualFriendsCount = 7,
            streakDays = 14,
            isFavorite = true,
            isVerified = true,
            chillStatus = "Dispo pour un café & papoter dev",
            currentlyPlayingMusic = "Synthwave Boy - Neon Sunset 🎧"
        ),
        FriendLocation(
            id = "friend_sarah",
            username = "Sarah_K",
            displayName = "Sarah K.",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8606,
            longitude = 2.3376,
            city = "Paris",
            district = "Musée du Louvre",
            statusMessage = "Prise de notes & documentation sur Obsidian 📝✨",
            statusEmoji = "📝",
            batteryPercent = 68,
            isOnline = true,
            lastSeenFormatted = "Il y a 3 min",
            distanceKm = 0.85,
            activityTag = "Design & Docs",
            mutualFriendsCount = 12,
            streakDays = 23,
            isFavorite = true,
            isVerified = true,
            chillStatus = "Session chill & écriture au soleil",
            currentlyPlayingMusic = "Lofi Girl - Peaceful Evening 🎶"
        ),
        FriendLocation(
            id = "friend_lucas",
            username = "Lucas_Gamer",
            displayName = "Lucas B.",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8530,
            longitude = 2.3499,
            city = "Paris",
            district = "Quartier Latin",
            statusMessage = "En pause jeux vidéo ! Qui est chaud pour chiller ? 🎮⚡",
            statusEmoji = "🎮",
            batteryPercent = 45,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 1.25,
            activityTag = "Gaming & Chill",
            mutualFriendsCount = 5,
            streakDays = 9,
            isFavorite = false,
            isVerified = false,
            chillStatus = "Recherche team gaming ou pause chill",
            currentlyPlayingMusic = "Cyberpunk 2077 OST - Night City"
        ),
        FriendLocation(
            id = "friend_emma",
            username = "Emma_Tech",
            displayName = "Emma V.",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8738,
            longitude = 2.2950,
            city = "Paris",
            district = "Étoile / Champs-Élysées",
            statusMessage = "Dispo pour un café ou un live coding 🚀",
            statusEmoji = "⚡",
            batteryPercent = 81,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 1.7,
            activityTag = "Dispo café",
            mutualFriendsCount = 9,
            streakDays = 31,
            isFavorite = true,
            isVerified = true,
            chillStatus = "Prendre l'air au parc, ouvert aux discussions",
            currentlyPlayingMusic = "Chillhop Essentials - Spring 2026"
        ),
        FriendLocation(
            id = "friend_thomas",
            username = "Thomas_AI",
            displayName = "Thomas R.",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8661,
            longitude = 2.3553,
            city = "Paris",
            district = "République / Marais",
            statusMessage = "Expérimente avec les agents IA autonomes 🤖✨",
            statusEmoji = "🤖",
            batteryPercent = 54,
            isOnline = true,
            lastSeenFormatted = "Il y a 10 min",
            distanceKm = 2.1,
            activityTag = "IA & Agents",
            mutualFriendsCount = 3,
            streakDays = 6,
            isFavorite = false,
            isVerified = false,
            chillStatus = "Veille tech & relaxation",
            currentlyPlayingMusic = "Kavinsky - Nightcall"
        ),
        FriendLocation(
            id = "friend_lea",
            username = "Lea_Music",
            displayName = "Léa D.",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8462,
            longitude = 2.3372,
            city = "Paris",
            district = "Jardin du Luxembourg",
            statusMessage = "Écoute de la Synthwave en révisant 🎧🎶",
            statusEmoji = "🎧",
            batteryPercent = 77,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 0.65,
            activityTag = "Musique & Chill",
            mutualFriendsCount = 8,
            streakDays = 18,
            isFavorite = false,
            isVerified = true,
            chillStatus = "Casque sur les oreilles, dispo pour écouter des sons",
            currentlyPlayingMusic = "HOME - Resonance"
        ),
        FriendLocation(
            id = "friend_karim",
            username = "Karim_Chill",
            displayName = "Karim S.",
            avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8512,
            longitude = 2.3120,
            city = "Paris",
            district = "Invalides / Esplanade",
            statusMessage = "Pause smoothie & chill en plein air 🥤☀️",
            statusEmoji = "☀️",
            batteryPercent = 95,
            isOnline = true,
            lastSeenFormatted = "En ligne",
            distanceKm = 0.95,
            activityTag = "Zen & Chill",
            mutualFriendsCount = 6,
            streakDays = 11,
            isFavorite = false,
            isVerified = false,
            chillStatus = "Posé sur l'herbe au soleil",
            currentlyPlayingMusic = "Tycho - Awake"
        ),
        FriendLocation(
            id = "friend_chloe",
            username = "Chloe_Photo",
            displayName = "Chloé N.",
            avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&auto=format&fit=crop&q=80",
            latitude = 48.8680,
            longitude = 2.3210,
            city = "Paris",
            district = "Madeleine / Opéra",
            statusMessage = "Chasse aux clichés urbains & architecture 📷🏙️",
            statusEmoji = "📷",
            batteryPercent = 63,
            isOnline = true,
            lastSeenFormatted = "À l'instant",
            distanceKm = 1.45,
            activityTag = "Photo & Art",
            mutualFriendsCount = 11,
            streakDays = 27,
            isFavorite = true,
            isVerified = true,
            chillStatus = "Balade photo urbaine",
            currentlyPlayingMusic = "Daft Punk - Veridis Quo"
        )
    )

    private val initialChillSpots = listOf(
        ChillSpot(
            id = "spot_1",
            title = "Terrasse Café Lofi & Dev",
            category = "Café & Co-Working",
            emoji = "☕",
            description = "Spot calme avec prises, wifi ultra-rapide et ambiance lofi douce",
            latitude = 48.8570,
            longitude = 2.3010,
            activeUsersCount = 5,
            creatorName = "Alex_Dev",
            distanceKm = 0.4,
            musicAmbience = "Lofi Hip-Hop Chillout"
        ),
        ChillSpot(
            id = "spot_2",
            title = "Jardin Zen & Lecture IDDET",
            category = "Détente & Plein Air",
            emoji = "🌿",
            description = "Espace ombragé idéal pour lire, écouter de la musique ou méditer",
            latitude = 48.8490,
            longitude = 2.3390,
            activeUsersCount = 3,
            creatorName = "Léa D.",
            distanceKm = 0.7,
            musicAmbience = "Binaural Beats & Nature Sounds"
        ),
        ChillSpot(
            id = "spot_3",
            title = "Arcade & Lounge Gaming",
            category = "Gaming & Fun",
            emoji = "🎮",
            description = "Espace chill avec canapés, consoles rétro et tournois improvisés",
            latitude = 48.8540,
            longitude = 2.3510,
            activeUsersCount = 6,
            creatorName = "Lucas B.",
            distanceKm = 1.1,
            musicAmbience = "Chiptune & Synthwave"
        ),
        ChillSpot(
            id = "spot_4",
            title = "Rooftop Coucher de Soleil",
            category = "Vue & Chill",
            emoji = "🌅",
            description = "Superbe vue panoramique pour admirer la golden hour entre membres",
            latitude = 48.8710,
            longitude = 2.3050,
            activeUsersCount = 4,
            creatorName = "Emma V.",
            distanceKm = 1.5,
            musicAmbience = "Deep House Sunset Chill"
        )
    )

    private val _friends = MutableStateFlow<List<FriendLocation>>(initialFriends)
    val friends: StateFlow<List<FriendLocation>> = _friends.asStateFlow()

    private val _chillSpots = MutableStateFlow<List<ChillSpot>>(initialChillSpots)
    val chillSpots: StateFlow<List<ChillSpot>> = _chillSpots.asStateFlow()

    private val _isGhostMode = MutableStateFlow(false)
    val isGhostMode: StateFlow<Boolean> = _isGhostMode.asStateFlow()

    private val _isRadarScanning = MutableStateFlow(false)
    val isRadarScanning: StateFlow<Boolean> = _isRadarScanning.asStateFlow()

    private val _radarScanRadiusKm = MutableStateFlow(3.0)
    val radarScanRadiusKm: StateFlow<Double> = _radarScanRadiusKm.asStateFlow()

    private val _lastChillWaveSent = MutableStateFlow<String?>(null)
    val lastChillWaveSent: StateFlow<String?> = _lastChillWaveSent.asStateFlow()

    private val _currentUserVibe = MutableStateFlow(
        UserVibe(
            emoji = "🚀",
            text = "En ligne • Dispo pour chiller et échanger",
            activityType = "Dispo"
        )
    )
    val currentUserVibe: StateFlow<UserVibe> = _currentUserVibe.asStateFlow()

    private val _isServerSynced = MutableStateFlow(false)
    val isServerSynced: StateFlow<Boolean> = _isServerSynced.asStateFlow()

    private val _serverSpeedKmh = MutableStateFlow(0.0)
    val serverSpeedKmh: StateFlow<Double> = _serverSpeedKmh.asStateFlow()

    private val _lastServerSyncTime = MutableStateFlow<Long>(0L)
    val lastServerSyncTime: StateFlow<Long> = _lastServerSyncTime.asStateFlow()

    fun updateServerSpeed(speed: Double) {
        _serverSpeedKmh.value = speed
        _isServerSynced.value = true
        _lastServerSyncTime.value = System.currentTimeMillis()
    }

    // Ingest real friends from the server (/api/friends/nearby)
    fun syncWithServerNearbyFriends(networkFriends: List<NearbyFriendNetwork>, userLat: Double, userLng: Double) {
        if (networkFriends.isEmpty()) {
            _isServerSynced.value = true
            _lastServerSyncTime.value = System.currentTimeMillis()
            return
        }

        val serverFriendLocations = networkFriends.map { net ->
            val dist = if (net.distance_km > 0.0) {
                net.distance_km
            } else {
                RealLocationProvider.calculateDistanceKm(userLat, userLng, net.latitude, net.longitude)
            }
            FriendLocation(
                id = net.user_id,
                username = net.username,
                displayName = net.username,
                avatarUrl = net.avatar_url,
                latitude = net.latitude,
                longitude = net.longitude,
                city = "En direct",
                district = "Secteur live",
                statusMessage = "Position synchronisée avec le serveur IDDET 📡",
                statusEmoji = "🟢",
                batteryPercent = 90,
                isOnline = true,
                lastSeenFormatted = "En direct",
                distanceKm = Math.round(dist * 100.0) / 100.0,
                activityTag = "En direct",
                mutualFriendsCount = 1,
                streakDays = 5,
                isFavorite = false,
                isVerified = true,
                chillStatus = "En ligne sur le Radar",
                currentlyPlayingMusic = null,
                isIddetMember = true
            )
        }

        // Merge: prioritize server friends and append others if not duplicate
        val existingMap = _friends.value.associateBy { it.id }.toMutableMap()
        serverFriendLocations.forEach { sf ->
            existingMap[sf.id] = sf
        }
        _friends.value = existingMap.values.toList()
        _isServerSynced.value = true
        _lastServerSyncTime.value = System.currentTimeMillis()
    }

    // Toggle Ghost Mode (protects user privacy)
    fun setGhostMode(enabled: Boolean) {
        _isGhostMode.value = enabled
    }

    // Set Radar Scan Radius
    fun setRadarScanRadius(radiusKm: Double) {
        _radarScanRadiusKm.value = radiusKm
    }

    // Update user custom status
    fun updateVibe(emoji: String, text: String, activityType: String) {
        _currentUserVibe.value = UserVibe(
            emoji = emoji,
            text = text,
            activityType = activityType,
            timestamp = System.currentTimeMillis()
        )
    }

    // Toggle Favorite friend
    fun toggleFavorite(friendId: String) {
        _friends.value = _friends.value.map { friend ->
            if (friend.id == friendId) {
                friend.copy(isFavorite = !friend.isFavorite)
            } else {
                friend
            }
        }
    }

    // Drop a custom Chill Spot on the radar
    fun dropChillSpot(title: String, category: String, emoji: String, description: String, userLat: Double, userLng: Double) {
        val newSpot = ChillSpot(
            id = "spot_${System.currentTimeMillis()}",
            title = title,
            category = category,
            emoji = emoji,
            description = description,
            latitude = userLat,
            longitude = userLng,
            activeUsersCount = 1,
            creatorName = "Moi (Créateur)",
            distanceKm = 0.05,
            musicAmbience = "Lofi Vibes"
        )
        _chillSpots.value = listOf(newSpot) + _chillSpots.value
    }

    // Join / Boost a Chill Spot
    fun joinChillSpot(spotId: String) {
        _chillSpots.value = _chillSpots.value.map { spot ->
            if (spot.id == spotId) {
                spot.copy(activeUsersCount = spot.activeUsersCount + 1)
            } else {
                spot
            }
        }
    }

    // Send a Chill Wave to a friend
    fun sendChillWave(friend: FriendLocation, waveType: ChillWaveType) {
        _lastChillWaveSent.value = "${waveType.emoji} ${waveType.label} envoyé à ${friend.displayName} !"
    }

    fun clearLastChillWave() {
        _lastChillWaveSent.value = null
    }

    // Anchor friends and chill spots relative to user's real GPS coordinates on Earth
    fun anchorFriendsAroundUser(userLat: Double, userLng: Double) {
        val offsets = listOf(
            Triple(0.0028, -0.0035, "Nord-Ouest"),
            Triple(-0.0022, 0.0041, "Sud-Est"),
            Triple(0.0042, 0.0025, "Nord-Est"),
            Triple(-0.0035, -0.0030, "Sud-Ouest"),
            Triple(0.0060, 0.0012, "Est"),
            Triple(-0.0012, -0.0050, "Ouest"),
            Triple(0.0018, 0.0035, "Est-Centre"),
            Triple(-0.0040, 0.0015, "Sud-Centre")
        )

        _friends.value = _friends.value.mapIndexed { index, friend ->
            val offset = offsets.getOrElse(index) { Triple((index * 0.0015), (index * -0.0015), "Proche") }
            val newLat = userLat + offset.first
            val newLng = userLng + offset.second
            val dist = RealLocationProvider.calculateDistanceKm(userLat, userLng, newLat, newLng)
            friend.copy(
                latitude = newLat,
                longitude = newLng,
                distanceKm = dist,
                district = "Secteur ${offset.third}"
            )
        }

        val spotOffsets = listOf(
            Pair(0.0030, 0.0010),
            Pair(-0.0020, 0.0030),
            Pair(0.0015, -0.0040),
            Pair(-0.0035, -0.0020)
        )
        _chillSpots.value = _chillSpots.value.mapIndexed { index, spot ->
            val offset = spotOffsets.getOrElse(index) { Pair(0.002, 0.002) }
            val spotLat = userLat + offset.first
            val spotLng = userLng + offset.second
            val dist = RealLocationProvider.calculateDistanceKm(userLat, userLng, spotLat, spotLng)
            spot.copy(
                latitude = spotLat,
                longitude = spotLng,
                distanceKm = dist
            )
        }
    }

    // Simulate subtle real-time movement / radar updates
    fun simulateRadarPing() {
        _friends.value = _friends.value.map { friend ->
            val deltaLat = (Random.nextDouble() - 0.5) * 0.0003
            val deltaLng = (Random.nextDouble() - 0.5) * 0.0003
            val newDist = (friend.distanceKm + (Random.nextDouble() - 0.5) * 0.04).coerceIn(0.1, 10.0)
            val roundedDist = Math.round(newDist * 100.0) / 100.0
            friend.copy(
                latitude = friend.latitude + deltaLat,
                longitude = friend.longitude + deltaLng,
                distanceKm = roundedDist
            )
        }
    }
}

