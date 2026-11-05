package com.example.ui

import com.example.data.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActfileWithUser
import com.example.data.ActfileComment
import com.example.data.IddetRepository
import com.example.data.User
import com.example.data.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@OptIn(ExperimentalCoroutinesApi::class)
class IddetViewModel(val repository: IddetRepository) : ViewModel() {

    private val _myIddetPlusStatus = MutableStateFlow<IddetPlusStatusResponse?>(null)
    val myIddetPlusStatus: StateFlow<IddetPlusStatusResponse?> = _myIddetPlusStatus

    private val _myCard = MutableStateFlow<UserCardResponse?>(null)
    val myCard: StateFlow<UserCardResponse?> = _myCard

    private val _myCredits = MutableStateFlow<CreditsResponse?>(null)
    val myCredits: StateFlow<CreditsResponse?> = _myCredits

    private val _communityBots = MutableStateFlow<List<CommunityBot>>(emptyList())
    val communityBots: StateFlow<List<CommunityBot>> = _communityBots.asStateFlow()

    private val _communityModActions = MutableStateFlow<List<CommunityModAction>>(emptyList())
    val communityModActions: StateFlow<List<CommunityModAction>> = _communityModActions.asStateFlow()

    private val _isBotsLoading = MutableStateFlow(false)
    val isBotsLoading: StateFlow<Boolean> = _isBotsLoading.asStateFlow()

    private val _isModActionsLoading = MutableStateFlow(false)
    val isModActionsLoading: StateFlow<Boolean> = _isModActionsLoading.asStateFlow()

    private val _pendingPaymentReference = MutableStateFlow<String?>(null)
    val pendingPaymentReference: StateFlow<String?> = _pendingPaymentReference.asStateFlow()

    private val _pendingPaymentStatus = MutableStateFlow<String?>(null)
    val pendingPaymentStatus: StateFlow<String?> = _pendingPaymentStatus.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun setPendingPayment(reference: String?) {
        _pendingPaymentReference.value = reference
        _pendingPaymentStatus.value = if (reference != null) "pending" else null
        pollingJob?.cancel()
        if (reference != null) {
            startPollingPaymentStatus(reference)
        }
    }

    fun clearPendingPayment() {
        pollingJob?.cancel()
        _pendingPaymentReference.value = null
        _pendingPaymentStatus.value = null
    }

    fun startPollingPaymentStatus(reference: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _pendingPaymentStatus.value = "pending"
            // Interroger régulièrement le serveur (toutes les 4s pendant max 2 minutes)
            for (i in 0 until 30) {
                delay(4000)
                try {
                    val statusRes = repository.getCheckoutStatus(reference)
                    val st = statusRes.status.lowercase()
                    _pendingPaymentStatus.value = st
                    if (st == "success") {
                        loadIddetPlusData()
                        break
                    } else if (st == "failed") {
                        break
                    }
                } catch (e: Exception) {
                    // Erreur réseau passagère, continuer à scruter
                }
            }
        }
    }

    fun loadIddetPlusData() {
        viewModelScope.launch {
            try {
                _myIddetPlusStatus.value = repository.getMyIddetPlusStatus()
                _myCard.value = repository.getMyCard()
                _myCredits.value = repository.getMyCredits()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getUserCard(userId: String): UserCardResponse? {
        return try {
            repository.getUserCard(userId)
        } catch (e: Exception) {
            null
        }
    }

    fun updateCardStyle(style: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repository.updateCardStyle(style)
                // update local state
                _myCard.value = _myCard.value?.copy(card_style = res.card_style)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur de mise à jour du style")
            }
        }
    }

    private val _currentCheckoutResponse = MutableStateFlow<com.example.data.IddetPlusCheckoutResponse?>(null)
    val currentCheckoutResponse: StateFlow<com.example.data.IddetPlusCheckoutResponse?> = _currentCheckoutResponse.asStateFlow()

    fun createCheckoutSession(
        email: String? = null,
        phoneNumber: String? = null,
        countryCode: String? = "243",
        currency: String = "CDF",
        onSuccess: (com.example.data.IddetPlusCheckoutResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = currentUser.value
                val resolvedEmail = if (!email.isNullOrBlank()) email.trim() else user?.email?.trim()

                if (resolvedEmail.isNullOrBlank()) {
                    onError("Un email est requis pour payer Iddet Plus.")
                    return@launch
                }

                val cleanNumber = phoneNumber?.let { com.example.utils.PhoneUtils.cleanNumber(it) } ?: ""
                val cleanCountry = countryCode?.let { com.example.utils.PhoneUtils.cleanCountryCode(it) } ?: "243"
                val fullPhone = if (cleanNumber.isNotBlank()) "+$cleanCountry$cleanNumber" else user?.phoneNumber

                if (user != null && (user.email.isNullOrBlank() || user.email != resolvedEmail || (!fullPhone.isNullOrBlank() && user.phoneNumber != fullPhone))) {
                    try {
                        repository.updateProfile(
                            username = user.username,
                            avatarUrl = user.avatarUrl,
                            bio = user.bio,
                            privacySetting = user.privacySetting,
                            email = resolvedEmail,
                            phoneNumber = fullPhone ?: user.phoneNumber,
                            birthDate = user.birthDate,
                            zodiacSign = user.zodiacSign,
                            preferredCategory = user.preferredCategory
                        )
                    } catch (e: Exception) {
                        // Non-critical profile update
                    }
                }

                val curr = if (currency.equals("USD", ignoreCase = true)) "USD" else "CDF"
                val res = repository.createIddetPlusCheckout(currency = curr)
                _currentCheckoutResponse.value = res

                val ref = res.reference
                if (!ref.isNullOrBlank()) {
                    setPendingPayment(ref)
                }
                onSuccess(res)
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
                if (code == 400 && (errorBody.contains("email", ignoreCase = true) || e.message().contains("email", ignoreCase = true))) {
                    onError("Un email est requis pour payer Iddet Plus.")
                } else if (code == 503) {
                    onError("Passerelle Chariow momentanément indisponible. Réessayez plus tard.")
                } else {
                    onError(e.message() ?: "Erreur de création de session Chariow ($code)")
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Erreur de création du checkout"
                if (msg.contains("email", ignoreCase = true)) {
                    onError("Un email est requis pour payer Iddet Plus.")
                } else {
                    onError(msg)
                }
            }
        }
    }

    fun checkPaymentStatus(reference: String, onResult: (status: String, isSuccess: Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repository.getCheckoutStatus(reference)
                if (res.status.equals("success", ignoreCase = true)) {
                    loadIddetPlusData()
                    onResult("success", true)
                } else {
                    onResult(res.status, false)
                }
            } catch (e: Exception) {
                onResult("error", false)
            }
        }
    }

    fun simulateGooglePlayPurchase(onSuccess: () -> Unit) {
        viewModelScope.launch {
            delay(1500)
            _myIddetPlusStatus.value = com.example.data.IddetPlusStatusResponse(
                is_iddet_plus = true,
                expires_at = "2027-09-03T00:00:00Z", // 1 year mock
                credits = 500,
                card_style = "classic",
                monthly_price_usd = 1.99,
                monthly_credits = 500,
                history = emptyList()
            )
            onSuccess()
        }
    }

    val currentUser: StateFlow<User?> = repository.currentUser
    
    val selectedTheme: StateFlow<com.example.ui.theme.AppTheme> = repository.selectedTheme

    fun setSelectedTheme(theme: com.example.ui.theme.AppTheme) {
        repository.setSelectedTheme(theme)
    }

    val targetLanguage: StateFlow<String> = repository.targetLanguage

    val aiState = com.example.utils.LocalAiManager.state

    fun setTargetLanguage(language: String) {
        repository.setTargetLanguage(language)
    }

    private val _showComposer = MutableStateFlow(false)
    val showComposer: StateFlow<Boolean> = _showComposer.asStateFlow()

    fun setShowComposer(show: Boolean) {
        _showComposer.value = show
    }

    private val _feedTab = MutableStateFlow(0)
    val feedTab: StateFlow<Int> = _feedTab.asStateFlow()
    private val _isFeedLoading = MutableStateFlow(true)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    // Trending Topics powered by Google News & Search for Tech, AI & Markdown
    private val _trendingTopics = MutableStateFlow<List<com.example.data.TrendingTopic>>(emptyList())
    val trendingTopics: StateFlow<List<com.example.data.TrendingTopic>> = _trendingTopics.asStateFlow()

    private val _selectedTrendingCategory = MutableStateFlow(com.example.data.TrendingCategory.ALL)
    val selectedTrendingCategory: StateFlow<com.example.data.TrendingCategory> = _selectedTrendingCategory.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    private val _composerInitialContent = MutableStateFlow<String?>(null)
    val composerInitialContent: StateFlow<String?> = _composerInitialContent.asStateFlow()

    fun setComposerInitialContent(content: String?) {
        _composerInitialContent.value = content
    }

    private val _viewedStoryIds = mutableSetOf<String>()
    private val _realtimeStoryViews = MutableStateFlow<Map<String, Int>>(emptyMap())
    val realtimeStoryViews: StateFlow<Map<String, Int>> = _realtimeStoryViews.asStateFlow()

    private val _realtimeStoryReactions = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val realtimeStoryReactions: StateFlow<Map<String, Map<String, Int>>> = _realtimeStoryReactions.asStateFlow()

    fun trackStoryView(storyId: String, initialViews: Int) {
        val currentViews = _realtimeStoryViews.value[storyId] ?: initialViews
        if (!_viewedStoryIds.contains(storyId)) {
            _viewedStoryIds.add(storyId)
            _realtimeStoryViews.value = _realtimeStoryViews.value + (storyId to (currentViews + 1))
        } else if (!_realtimeStoryViews.value.containsKey(storyId)) {
            _realtimeStoryViews.value = _realtimeStoryViews.value + (storyId to currentViews)
        }
    }

    fun recordStoryReaction(storyId: String, emoji: String, initialReactions: Map<String, Int>) {
        val current = (_realtimeStoryReactions.value[storyId] ?: initialReactions).toMutableMap()
        current[emoji] = (current[emoji] ?: 0) + 1
        _realtimeStoryReactions.value = _realtimeStoryReactions.value + (storyId to current)
    }

    // Existing selectTrendingCategory
    fun selectTrendingCategory(category: com.example.data.TrendingCategory) {
        _selectedTrendingCategory.value = category
        loadTrendingTopics(category)
    }

    fun refreshTrendingTopics() {
        if (_newsSearchQuery.value.isNotBlank()) {
            searchGoogleNewsQuery(_newsSearchQuery.value, _selectedTrendingCategory.value)
        } else {
            loadTrendingTopics(_selectedTrendingCategory.value)
        }
    }

    fun loadTrendingTopics(category: com.example.data.TrendingCategory = _selectedTrendingCategory.value) {
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val topics = com.example.data.TrendingTopicsService.fetchTrendingTopics(category)
                _trendingTopics.value = topics
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    // Google News Bookmarks & Live Search
    private val _bookmarkedNews = MutableStateFlow<List<com.example.data.TrendingTopic>>(emptyList())
    val bookmarkedNews: StateFlow<List<com.example.data.TrendingTopic>> = _bookmarkedNews.asStateFlow()

    private val _newsSearchQuery = MutableStateFlow("")
    val newsSearchQuery: StateFlow<String> = _newsSearchQuery.asStateFlow()

    fun setNewsSearchQuery(query: String) {
        _newsSearchQuery.value = query
    }

    fun isNewsBookmarked(topicId: String): Boolean {
        return _bookmarkedNews.value.any { it.id == topicId }
    }

    fun initBookmarkedNews(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("saved_google_news", null) ?: return
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<com.example.data.TrendingTopic>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val catName = obj.optString("category", "ALL")
                val cat = try { com.example.data.TrendingCategory.valueOf(catName) } catch (e: Exception) { com.example.data.TrendingCategory.ALL }
                val takeaways = mutableListOf<String>()
                val arr = obj.optJSONArray("keyTakeaways")
                if (arr != null) {
                    for (j in 0 until arr.length()) takeaways.add(arr.getString(j))
                }
                val tagsList = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (j in 0 until tagsArr.length()) tagsList.add(tagsArr.getString(j))
                }
                list.add(
                    com.example.data.TrendingTopic(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        source = obj.optString("source", "Google Actualités"),
                        link = obj.optString("link", "https://news.google.com"),
                        pubDateFormatted = obj.optString("pubDateFormatted", "Récemment"),
                        category = cat,
                        snippet = if (obj.has("snippet")) obj.optString("snippet") else null,
                        tags = tagsList,
                        isHot = obj.optBoolean("isHot", false),
                        imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                        keyTakeaways = takeaways,
                        readTimeMin = obj.optInt("readTimeMin", 3),
                        isBookmarked = true
                    )
                )
            }
            _bookmarkedNews.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBookmarkNews(topic: com.example.data.TrendingTopic, context: android.content.Context) {
        val current = _bookmarkedNews.value.toMutableList()
        val index = current.indexOfFirst { it.id == topic.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(0, topic.copy(isBookmarked = true))
        }
        _bookmarkedNews.value = current

        try {
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val jsonArray = org.json.JSONArray()
            for (item in current) {
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("source", item.source)
                    put("link", item.link)
                    put("pubDateFormatted", item.pubDateFormatted)
                    put("category", item.category.name)
                    put("snippet", item.snippet ?: "")
                    put("imageUrl", item.imageUrl ?: "")
                    put("isHot", item.isHot)
                    put("readTimeMin", item.readTimeMin)
                    val takeawaysArr = org.json.JSONArray()
                    item.keyTakeaways.forEach { takeawaysArr.put(it) }
                    put("keyTakeaways", takeawaysArr)
                    val tagsArr = org.json.JSONArray()
                    item.tags.forEach { tagsArr.put(it) }
                    put("tags", tagsArr)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_google_news", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun searchGoogleNewsQuery(query: String, category: com.example.data.TrendingCategory = _selectedTrendingCategory.value) {
        _newsSearchQuery.value = query
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val results = com.example.data.TrendingTopicsService.searchGoogleNews(query, category)
                _trendingTopics.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    // Friends Live Radar / Real-Time Map & Real GPS Location
    private var realLocationProvider: com.example.data.RealLocationProvider? = null
    private val _realLocation = MutableStateFlow(com.example.data.UserRealLocation())
    val realLocation: StateFlow<com.example.data.UserRealLocation> = _realLocation.asStateFlow()

    val friendsLocations = com.example.data.FriendsLocationService.friends
    val chillSpots = com.example.data.FriendsLocationService.chillSpots
    val isGhostMode = com.example.data.FriendsLocationService.isGhostMode
    val currentUserVibe = com.example.data.FriendsLocationService.currentUserVibe
    val radarScanRadiusKm = com.example.data.FriendsLocationService.radarScanRadiusKm
    val lastChillWaveSent = com.example.data.FriendsLocationService.lastChillWaveSent
    val isServerSynced = com.example.data.FriendsLocationService.isServerSynced
    val serverSpeedKmh = com.example.data.FriendsLocationService.serverSpeedKmh
    val lastServerSyncTime = com.example.data.FriendsLocationService.lastServerSyncTime

    data class TrafficJamAlert(
        val id: String = java.util.UUID.randomUUID().toString(),
        val message: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _trafficJamAlerts = MutableStateFlow<List<TrafficJamAlert>>(emptyList())
    val trafficJamAlerts: StateFlow<List<TrafficJamAlert>> = _trafficJamAlerts.asStateFlow()

    fun reportTrafficJam(lat: Double, lng: Double, note: String = "Ralentissement important") {
        val alert = TrafficJamAlert(
            message = "🚗 $note signalé par vous",
            latitude = lat,
            longitude = lng
        )
        _trafficJamAlerts.value = listOf(alert) + _trafficJamAlerts.value.take(5)
    }

    fun dismissTrafficJam(id: String) {
        _trafficJamAlerts.value = _trafficJamAlerts.value.filterNot { it.id == id }
    }

    fun initLocationTracking(context: android.content.Context) {
        if (realLocationProvider == null) {
            val provider = com.example.data.RealLocationProvider(context.applicationContext)
            realLocationProvider = provider
            viewModelScope.launch {
                provider.locationFlow.collect { loc ->
                    _realLocation.value = loc
                    val token = repository.userToken
                    if (!token.isNullOrBlank() && loc.isRealGpsAcquired) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                repository.sendLocationUpdate(
                                    lat = loc.latitude,
                                    lng = loc.longitude,
                                    isSharing = !isGhostMode.value,
                                    detectCountry = true
                                )
                                val nearby = repository.fetchNearbyFriends(50)
                                com.example.data.FriendsLocationService.syncWithServerNearbyFriends(nearby, loc.latitude, loc.longitude)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            provider.startLocationUpdates()
        } else {
            realLocationProvider?.startLocationUpdates()
        }
    }

    fun stopLocationTracking() {
        realLocationProvider?.stopLocationUpdates()
    }

    private val _lastWavedFriend = MutableStateFlow<String?>(null)
    val lastWavedFriend: StateFlow<String?> = _lastWavedFriend.asStateFlow()

    fun setGhostMode(enabled: Boolean) {
        com.example.data.FriendsLocationService.setGhostMode(enabled)
        val token = repository.userToken
        if (!token.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (enabled) {
                        repository.stopLocationSharing()
                    } else {
                        val loc = _realLocation.value
                        repository.sendLocationUpdate(
                            lat = loc.latitude,
                            lng = loc.longitude,
                            isSharing = true
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun setRadarScanRadius(radiusKm: Double) {
        com.example.data.FriendsLocationService.setRadarScanRadius(radiusKm)
    }

    fun updateUserVibe(emoji: String, text: String, activityType: String) {
        com.example.data.FriendsLocationService.updateVibe(emoji, text, activityType)
    }

    fun toggleFavoriteFriend(friendId: String) {
        com.example.data.FriendsLocationService.toggleFavorite(friendId)
    }

    fun dropChillSpot(title: String, category: String, emoji: String, description: String, userLat: Double, userLng: Double) {
        com.example.data.FriendsLocationService.dropChillSpot(title, category, emoji, description, userLat, userLng)
    }

    fun joinChillSpot(spotId: String) {
        com.example.data.FriendsLocationService.joinChillSpot(spotId)
    }

    fun sendChillWave(friend: com.example.data.FriendLocation, waveType: com.example.data.ChillWaveType) {
        com.example.data.FriendsLocationService.sendChillWave(friend, waveType)
        viewModelScope.launch {
            _lastWavedFriend.value = friend.displayName
            kotlinx.coroutines.delay(3500)
            com.example.data.FriendsLocationService.clearLastChillWave()
            if (_lastWavedFriend.value == friend.displayName) {
                _lastWavedFriend.value = null
            }
        }
    }

    fun sendWaveToFriend(friend: com.example.data.FriendLocation) {
        viewModelScope.launch {
            _lastWavedFriend.value = friend.displayName
            // Trigger a simulated friendly response or notification
            kotlinx.coroutines.delay(3000)
            if (_lastWavedFriend.value == friend.displayName) {
                _lastWavedFriend.value = null
            }
        }
    }

    fun refreshFriendsRadar() {
        viewModelScope.launch {
            val loc = _realLocation.value
            val token = repository.userToken
            if (!token.isNullOrBlank()) {
                try {
                    repository.sendLocationUpdate(loc.latitude, loc.longitude, !isGhostMode.value)
                    val nearby = repository.fetchNearbyFriends(50)
                    com.example.data.FriendsLocationService.syncWithServerNearbyFriends(nearby, loc.latitude, loc.longitude)
                } catch (_: Exception) {}
            }
            com.example.data.FriendsLocationService.simulateRadarPing()
        }
    }

    fun setManualCountry(code: String) {
        viewModelScope.launch {
            repository.setManualCountry(code)
        }
    }

    fun redetectAutoCountry() {
        viewModelScope.launch {
            repository.redetectAutoCountry()
        }
    }

    fun setFeedTab(tab: Int) {
        _feedTab.value = tab
    }

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    fun setSelectedCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    val allCategories: StateFlow<List<String>> = _allCategories.asStateFlow()

    private val _preferredCategories = MutableStateFlow<List<String>>(emptyList())
    val preferredCategories: StateFlow<List<String>> = _preferredCategories.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            if (_allCategories.value.isEmpty()) {
                _allCategories.value = repository.getCategories()
            }
            _preferredCategories.value = repository.getMyPreferredCategories()
        }
    }

    fun savePreferredCategories(categoriesList: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updated = repository.updatePreferredCategories(categoriesList)
                _preferredCategories.value = updated
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Une erreur est survenue")
            }
        }
    }
    
    val actfiles = repository.getAllActfiles().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val followedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest {
        repository.getFollowedActfiles()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _stories = MutableStateFlow<List<com.example.data.Story>>(emptyList())
    val stories: StateFlow<List<com.example.data.Story>> = _stories.asStateFlow()

    private val _isStoriesLoading = MutableStateFlow(false)
    val isStoriesLoading: StateFlow<Boolean> = _isStoriesLoading.asStateFlow()

    private val _isUploadingStory = MutableStateFlow(false)
    val isUploadingStory: StateFlow<Boolean> = _isUploadingStory.asStateFlow()

    // ── MUSIQUE & STRIP SOUNDS INTEGRATION ──
    private val _sounds = MutableStateFlow<List<com.example.utils.MusicTrack>>(emptyList())
    val sounds: StateFlow<List<com.example.utils.MusicTrack>> = _sounds.asStateFlow()

    private val _isSoundsLoading = MutableStateFlow(false)
    val isSoundsLoading: StateFlow<Boolean> = _isSoundsLoading.asStateFlow()

    private val _isUploadingSound = MutableStateFlow(false)
    val isUploadingSound: StateFlow<Boolean> = _isUploadingSound.asStateFlow()

    fun loadSounds(category: String = "Tout") {
        viewModelScope.launch {
            _isSoundsLoading.value = true
            try {
                val soundNetworks = repository.getSoundsByCategory(category)
                val mappedTracks = soundNetworks.map { it.toMusicTrack() }
                _sounds.value = mappedTracks
                com.example.utils.MusicPlayerManager.updatePlaylist(mappedTracks)
            } catch (e: Exception) {
                e.printStackTrace()
                _sounds.value = emptyList()
                com.example.utils.MusicPlayerManager.updatePlaylist(emptyList())
            } finally {
                _isSoundsLoading.value = false
            }
        }
    }

    fun uploadSound(
        audioFile: java.io.File,
        coverFile: java.io.File?,
        title: String,
        description: String,
        category: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isUploadingSound.value = true
            try {
                val res = repository.uploadSound(audioFile, coverFile, title, description, category)
                if (res.isSuccess) {
                    loadSounds(category)
                    onResult(true, "Son publié avec succès !")
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Erreur de publication du son")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Erreur inconnue")
            } finally {
                _isUploadingSound.value = false
            }
        }
    }

    fun likeSound(soundId: String) {
        viewModelScope.launch {
            try {
                repository.likeSound(soundId)
                _sounds.value = _sounds.value.map { track ->
                    if (track.id == soundId) track.copy(likesCount = track.likesCount + 1) else track
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recordSoundPlay(soundId: String) {
        viewModelScope.launch {
            try {
                repository.recordSoundPlay(soundId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun SoundNetwork.toMusicTrack(): com.example.utils.MusicTrack {
        val dur = duration ?: 0.0
        val mins = (dur / 60).toInt()
        val secs = (dur % 60).toInt()
        val formattedDuration = if (dur > 0) String.format("%02d:%02d", mins, secs) else "--:--"

        val fixedAudioUrl = com.example.utils.UrlHelper.fixCloudinaryUrl(audio_url)
            ?.ifBlank { null }
            ?: audio_url?.ifBlank { null }
            ?: "https://hoosthubs-g.onrender.com/api/sounds/$id/short/stream"

        val fixedCoverUrl = com.example.utils.UrlHelper.fixCloudinaryUrl(cover_url)
            ?.ifBlank { null }
            ?: cover_url?.ifBlank { null }
            ?: ""

        return com.example.utils.MusicTrack(
            id = id,
            title = if (title.isNotBlank()) title else "Piste Audio",
            artist = if (!author_username.isNullOrBlank()) author_username else if (!author_id.isNullOrBlank()) author_id else "Artiste",
            albumArt = fixedCoverUrl,
            audioUrl = fixedAudioUrl,
            durationFormatted = formattedDuration,
            genre = if (category.isNotBlank()) category else "Autres",
            likesCount = likes_count
        )
    }

    // ── VIDEO FEED (REELS) INTEGRATION DIRECTE ──
    private val _realVideoFeed = MutableStateFlow<List<VideoFeedItemNetwork>>(emptyList())
    val realVideoFeed: StateFlow<List<VideoFeedItemNetwork>> = _realVideoFeed.asStateFlow()

    private val _isVideoFeedLoading = MutableStateFlow(false)
    val isVideoFeedLoading: StateFlow<Boolean> = _isVideoFeedLoading.asStateFlow()

    fun loadVideoFeed() {
        viewModelScope.launch {
            _isVideoFeedLoading.value = true
            try {
                val videos = repository.getVideoFeed()
                _realVideoFeed.value = videos
            } catch (e: Exception) {
                e.printStackTrace()
                _realVideoFeed.value = emptyList()
            } finally {
                _isVideoFeedLoading.value = false
            }
        }
    }

    fun likeVideo(videoId: String) {
        viewModelScope.launch {
            try {
                repository.likeVideo(videoId)
                _realVideoFeed.value = _realVideoFeed.value.map { v ->
                    if (v.id == videoId) {
                        val newLiked = !v.liked
                        v.copy(liked = newLiked, likes = if (newLiked) v.likes + 1 else maxOf(0, v.likes - 1))
                    } else v
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recordVideoView(videoId: String) {
        viewModelScope.launch {
            try {
                repository.recordVideoView(videoId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadVideo(
        videoFile: java.io.File,
        description: String,
        isPublic: Boolean = true,
        hasOriginalSound: Boolean = false,
        soundId: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = repository.uploadVideo(videoFile, description, isPublic, hasOriginalSound, soundId)
                if (res.isSuccess) {
                    loadVideoFeed()
                    onResult(true, "Vidéo publiée avec succès !")
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Erreur de publication vidéo")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Erreur inconnue")
            }
        }
    }

    fun refreshActfiles(targetUserId: String? = null) {
        viewModelScope.launch {
            _isFeedLoading.value = true
            try {
                repository.refreshActfiles(targetUserId)
            } finally {
                _isFeedLoading.value = false
            }
        }
    }

    fun refreshUserProfile(userId: String) {
        viewModelScope.launch {
            repository.refreshUserProfile(userId)
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            repository.refreshProfile()
        }
    }

    init {
        viewModelScope.launch {
            repository.refreshActfiles()
            loadStories()
            loadTrendingTopics()
            // Automatically register live session with FastMCP server on app startup
            com.example.data.McpSessionManager.registerLiveSession(
                username = currentUser.value?.username,
                authToken = repository.userToken
            )
        }
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    com.example.data.McpSessionManager.registerLiveSession(
                        username = user.username,
                        authToken = repository.userToken
                    )
                    loadConnectedApps()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                try {
                    repository.refreshNotifications()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(15000)
            }
        }
        viewModelScope.launch {
            com.example.utils.WebSocketManager.events.collect { event ->
                when (event) {
                    is com.example.utils.WebSocketEvent.NewMessage -> {
                        val parsedTime = try {
                            val cleanTimestamp = event.timestamp.substringBefore(".")
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(cleanTimestamp)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        val detectedType = if (com.example.utils.AudioMessageHelper.isAudioContent(event.content, event.msgType)) "audio" else event.msgType
                        val incomingMsg = Message(
                            id = event.messageId,
                            senderId = event.senderId,
                            receiverId = currentUser.value?.id ?: "",
                            content = event.content,
                            type = detectedType,
                            isRead = false,
                            createdAt = parsedTime
                        )
                        repository.insertMessageLocal(incomingMsg)
                        repository.updateConversationLastMessage(
                            otherUserId = event.senderId,
                            content = event.content,
                            type = detectedType,
                            isIncoming = true,
                            senderUsername = event.senderUsername
                        )
                    }
                    is com.example.utils.WebSocketEvent.MessageSent -> {
                        val myId = currentUser.value?.id ?: ""
                        val targetReceiverId = lastSendingReceiverId ?: ""
                        
                        try {
                            val currentMessages = repository.getMessagesWith(targetReceiverId).first()
                            currentMessages.forEach { msg ->
                                if (msg.type == "audio_sending" || msg.type == "audio_error") {
                                    repository.deleteMessageLocal(msg.id)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        val msgId = event.messageId
                        if (msgId.isNotBlank()) {
                            val existing = repository.getMessageById(msgId)
                            if (existing == null) {
                                val confirmedMsg = Message(
                                    id = msgId,
                                    senderId = myId,
                                    receiverId = targetReceiverId,
                                    content = event.content,
                                    type = event.msgType,
                                    isRead = false,
                                    createdAt = System.currentTimeMillis()
                                )
                                repository.insertMessageLocal(confirmedMsg)
                            }
                            repository.updateConversationLastMessage(
                                otherUserId = targetReceiverId,
                                content = event.content,
                                type = event.msgType,
                                isIncoming = false
                            )
                        }
                    }
                    is com.example.utils.WebSocketEvent.Error -> {
                        val targetReceiverId = lastSendingReceiverId ?: ""
                        try {
                            val currentMessages = repository.getMessagesWith(targetReceiverId).first()
                            currentMessages.forEach { msg ->
                                if (msg.type == "audio_sending") {
                                    repository.insertMessageLocal(msg.copy(type = "audio_error"))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        repository.updateConversationLastMessage(
                            otherUserId = targetReceiverId,
                            content = "Échec de l'envoi",
                            type = "audio_error",
                            isIncoming = false
                        )
                    }
                    is com.example.utils.WebSocketEvent.TrafficJam -> {
                        val alert = TrafficJamAlert(
                            message = event.message,
                            latitude = event.latitude,
                            longitude = event.longitude,
                            timestamp = event.timestamp
                        )
                        _trafficJamAlerts.value = listOf(alert) + _trafficJamAlerts.value.filterNot {
                            it.latitude == event.latitude && it.longitude == event.longitude
                        }.take(5)
                    }
                    else -> {}
                }
            }
        }
    }

    // Properly scoped flows that update when currentUser changes, without causing infinite loops
    val giants: StateFlow<List<User>> = currentUser.flatMapLatest {
        repository.getGiants()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val conversations: StateFlow<List<com.example.data.ConversationNetwork>> = repository.conversations

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchInContent = MutableStateFlow(true)
    val searchInContent: StateFlow<Boolean> = _searchInContent.asStateFlow()

    private val _searchInTags = MutableStateFlow(true)
    val searchInTags: StateFlow<Boolean> = _searchInTags.asStateFlow()

    private val _searchInUsers = MutableStateFlow(true)
    val searchInUsers: StateFlow<Boolean> = _searchInUsers.asStateFlow()

    private val _searchSortOrder = MutableStateFlow("Recent") // Recent, Popular
    val searchSortOrder: StateFlow<String> = _searchSortOrder.asStateFlow()

    fun setSearchOptions(content: Boolean, tags: Boolean, users: Boolean) {
        _searchInContent.value = content
        _searchInTags.value = tags
        _searchInUsers.value = users
    }

    fun setSearchSortOrder(order: String) {
        _searchSortOrder.value = order
    }

    val searchUsersResult: StateFlow<List<User>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else repository.searchUsers(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchActfilesResult: StateFlow<List<ActfileWithUser>> = combine(
        _searchQuery, _searchInContent, _searchInTags, _searchInUsers, _searchSortOrder
    ) { query, inContent, inTags, inUsers, sortOrder ->
        if (query.isBlank()) return@combine emptyList<ActfileWithUser>()
        
        repository.searchActfiles(query).first().filter { actfile ->
            val matchContent = inContent && actfile.content.contains(query, ignoreCase = true)
            val matchTags = inTags && actfile.tags?.contains(query, ignoreCase = true) == true
            val matchUsers = inUsers && actfile.username.contains(query, ignoreCase = true)
            matchContent || matchTags || matchUsers
        }.let { list ->
            if (sortOrder == "Popular") {
                list.sortedByDescending { it.likesCount + it.viewsCount }
            } else {
                list.sortedByDescending { it.createdAt }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(username: String, password: String? = null) {
        viewModelScope.launch {
            try {
                repository.login(username, password)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun signup(username: String, password: String? = null) {
        viewModelScope.launch {
            try {
                repository.signup(username, password)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun signupFull(
        username: String, 
        password: String?, 
        avatarUrl: String?,
        bio: String,
        email: String?,
        phoneNumber: String?,
        birthDate: String?,
        zodiacSign: String?
    ) {
        viewModelScope.launch {
            try {
                repository.signup(username, password)
                repository.updateProfile(
                    username = null,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    privacySetting = "public",
                    email = email,
                    phoneNumber = phoneNumber,
                    birthDate = birthDate,
                    zodiacSign = zodiacSign
                )
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    val notifications = repository.getNotifications().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val likedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest {
        repository.getLikedActfiles()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val commentedActfiles: StateFlow<List<ActfileWithUser>> = currentUser.flatMapLatest { user ->
        repository.getCommentedActfiles(user?.id ?: "")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun updateProfile(
        username: String? = null,
        avatarUrl: String? = null,
        bio: String = currentUser.value?.bio ?: "",
        privacySetting: String = currentUser.value?.privacySetting ?: "Public",
        email: String? = null,
        phoneNumber: String? = null,
        birthDate: String? = null,
        zodiacSign: String? = null,
        country: String? = null,
        preferredCategory: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(username, avatarUrl, bio, privacySetting, email, phoneNumber, birthDate, zodiacSign, country, preferredCategory)
        }
    }

    fun updateProfileWithImage(
        avatarFile: java.io.File?,
        bio: String?,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateProfileWithImage(avatarFile, bio, phoneNumber)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Erreur de mise à jour du profil")
            }
        }
    }

    val savedAccounts: StateFlow<List<com.example.data.SavedAccount>> = repository.getSavedAccounts().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun removeSavedAccount(username: String) {
        repository.removeSavedAccount(username)
    }

    fun loginWithSavedAccount(token: String) {
        viewModelScope.launch {
            try {
                _authError.value = null
                repository.loginWithToken(token)
            } catch (e: Exception) {
                _authError.value = e.message ?: "Failed to login with saved account"
            }
        }
    }

    suspend fun getUserByUsername(username: String): User? {
        return repository.getUserByUsername(username)
    }

    fun logout() {
        repository.logout()
    }

    fun publishActfile(
        content: String,
        tags: String = "",
        category: String? = null,
        communityId: String? = null,
        channelId: String? = null,
        postAsIddet: Boolean = false,
        soundId: String? = null,
        soundTitle: String? = null,
        soundAuthor: String? = null,
        soundAudioUrl: String? = null,
        soundCoverUrl: String? = null,
        communityName: String? = null,
        communityIconUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.publishActfile(
                content = content,
                tags = tags,
                category = category,
                communityId = communityId,
                channelId = channelId,
                postAsIddet = postAsIddet,
                soundId = soundId,
                soundTitle = soundTitle,
                soundAuthor = soundAuthor,
                soundAudioUrl = soundAudioUrl,
                soundCoverUrl = soundCoverUrl,
                communityName = communityName,
                communityIconUrl = communityIconUrl
            )
            repository.refreshActfiles()
        }
    }

    fun publishAndPromoteActfile(
        content: String,
        tags: String = "",
        category: String? = null,
        communityId: String? = null,
        channelId: String? = null,
        postAsIddet: Boolean = false,
        soundId: String? = null,
        soundTitle: String? = null,
        soundAuthor: String? = null,
        soundAudioUrl: String? = null,
        soundCoverUrl: String? = null,
        communityName: String? = null,
        communityIconUrl: String? = null,
        budget: Double,
        currency: String = "USD",
        daily: Boolean = true,
        targetCountry: String? = null,
        onSuccess: (checkoutUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val createdId = repository.publishActfile(
                content = content,
                tags = tags,
                category = category,
                communityId = communityId,
                channelId = channelId,
                postAsIddet = postAsIddet,
                soundId = soundId,
                soundTitle = soundTitle,
                soundAuthor = soundAuthor,
                soundAudioUrl = soundAudioUrl,
                soundCoverUrl = soundCoverUrl,
                communityName = communityName,
                communityIconUrl = communityIconUrl
            )
            if (createdId != null) {
                val res = repository.promoteActfile(createdId, budget, currency, daily, targetCountry)
                repository.refreshActfiles()
                res.fold(
                    onSuccess = { response ->
                        onSuccess(response.checkout_url)
                    },
                    onFailure = { err ->
                        onError(err.message ?: "Actfile publié mais échec de la sponsorisation")
                    }
                )
            } else {
                onError("Impossible de créer l'actfile")
            }
        }
    }

    private val _attachedComposerSound = MutableStateFlow<com.example.utils.MusicTrack?>(null)
    val attachedComposerSound: StateFlow<com.example.utils.MusicTrack?> = _attachedComposerSound.asStateFlow()

    fun setAttachedComposerSound(track: com.example.utils.MusicTrack?) {
        _attachedComposerSound.value = track
    }

    suspend fun getSoundDetails(soundId: String): SoundNetwork? {
        return repository.getSoundDetails(soundId)
    }

    fun deleteActfile(id: String) {
        viewModelScope.launch {
            repository.deleteActfile(id)
        }
    }
    
    fun incrementView(id: String) {
        viewModelScope.launch {
            repository.incrementView(id)
        }
    }
    
    fun likeActfile(id: String) {
        viewModelScope.launch {
            repository.likeActfile(id)
        }
    }
    
    fun commentActfile(id: String, content: String) {
        viewModelScope.launch {
            repository.commentActfile(id, content)
        }
    }
    
    fun getUserActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return repository.getUserActfiles(userId)
    }
    
    fun getUserProfile(userId: String): Flow<User?> {
        return repository.getUserFlow(userId)
    }

    fun getMessagesWith(otherUserId: String): Flow<List<Message>> {
        return repository.getMessagesWith(otherUserId)
    }

    fun refreshMessagesWith(otherUserId: String) {
        viewModelScope.launch {
            repository.refreshMessagesWith(otherUserId)
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            repository.refreshConversations()
        }
    }

    private var lastSendingReceiverId: String? = null

    fun sendVoiceMessage(receiverId: String, voiceMarkdown: String) {
        lastSendingReceiverId = receiverId
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val myId = currentUser.value?.id ?: return@launch
            val msgId = "voice_" + java.util.UUID.randomUUID().toString()
            val localMsg = Message(
                id = msgId,
                senderId = myId,
                receiverId = receiverId,
                content = voiceMarkdown,
                type = "voice",
                createdAt = System.currentTimeMillis()
            )
            repository.insertMessageLocal(localMsg)
            repository.updateConversationLastMessage(
                otherUserId = receiverId,
                content = voiceMarkdown,
                type = "voice",
                isIncoming = false
            )
            try {
                repository.sendMessage(receiverId, voiceMarkdown, "voice")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendVoiceMessage(receiverId: String, file: java.io.File) {
        lastSendingReceiverId = receiverId
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val myId = currentUser.value?.id ?: return@launch
            val msgId = "voice_" + java.util.UUID.randomUUID().toString()
            val voiceContent = "[Voice Message](voice://duration=5&amplitudes=0.35,0.50,0.65,0.45,0.55)"
            
            val localMsg = Message(
                id = msgId,
                senderId = myId,
                receiverId = receiverId,
                content = if (file.exists() && file.length() > 0) file.absolutePath else voiceContent,
                type = "audio",
                createdAt = System.currentTimeMillis()
            )
            repository.insertMessageLocal(localMsg)
            repository.updateConversationLastMessage(
                otherUserId = receiverId,
                content = voiceContent,
                type = "audio",
                isIncoming = false
            )
            
            try {
                if (file.exists() && file.length() > 0) {
                    val bytes = file.readBytes()
                    val audioB64 = "base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    repository.sendMessage(receiverId, audioB64, "audio")
                } else {
                    repository.sendMessage(receiverId, voiceContent, "voice")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    repository.sendMessage(receiverId, voiceContent, "voice")
                } catch (_: Exception) {}
            }
        }
    }

    fun sendMessage(receiverId: String, content: String, type: String = "text") {
        viewModelScope.launch {
            repository.sendMessage(receiverId, content, type)
        }
    }

    fun verifyCurrentUser() {
        viewModelScope.launch {
            repository.verifyCurrentUser()
        }
    }

    fun isFollowing(otherUserId: String): Flow<Boolean> {
        return repository.isFollowing(otherUserId)
    }

    fun getFollowedUsers(userId: String? = null): Flow<List<User>> {
        return repository.getFollowedUsers(userId)
    }

    fun getFollowerUsers(userId: String? = null): Flow<List<User>> {
        return repository.getFollowerUsers(userId)
    }

    fun followUser(otherUserId: String) {
        viewModelScope.launch {
            repository.followUser(otherUserId)
        }
    }

    fun unfollowUser(otherUserId: String) {
        viewModelScope.launch {
            repository.unfollowUser(otherUserId)
        }
    }

    fun getComments(actfileId: String): Flow<List<ActfileComment>> {
        return repository.getComments(actfileId)
    }

    fun getActfile(actfileId: String): Flow<ActfileWithUser?> {
        return repository.getActfile(actfileId)
    }

    // Communities and Channels Support
    fun searchCommunitiesFlow(query: String?, category: String?, sort: String = "popular"): Flow<List<com.example.data.Community>> {
        return flow {
            emit(repository.searchCommunities(query, category, sort))
        }
    }

    fun getCommunityFlow(slug: String): Flow<com.example.data.Community?> {
        return flow {
            emit(repository.getCommunity(slug))
        }
    }

    fun getCommunityChannelsFlow(slug: String): Flow<List<com.example.data.Channel>> {
        return flow {
            emit(repository.getCommunityChannels(slug))
        }
    }

    fun getCommunityPostsFlow(slug: String): Flow<List<com.example.data.ActfileWithUser>> {
        return repository.getCommunityPostsFlow(slug)
    }

    fun getChannelMessagesFlow(channelId: String): Flow<List<com.example.data.ChannelMessage>> {
        return repository.getChannelMessagesFlow(channelId)
    }

    fun sendChannelMessage(
        channelId: String,
        communitySlug: String,
        content: String,
        type: String = "text",
        onComplete: (com.example.data.ChannelMessage?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val msg = repository.sendChannelMessage(channelId, communitySlug, content, type)
            onComplete(msg)
        }
    }

    fun deleteChannelMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteChannelMessage(messageId)
        }
    }

    fun createChannel(slug: String, name: String, description: String?, onResult: (com.example.data.Channel?) -> Unit) {
        viewModelScope.launch {
            val result = repository.createChannel(slug, name, description)
            onResult(result)
        }
    }

    fun getMyCommunitiesFlow(): Flow<List<com.example.data.Community>> {
        return flow {
            emit(repository.getMyCommunities())
        }
    }

    fun toggleCommunityJoin(slug: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.joinCommunity(slug)
            onComplete(success)
        }
    }

    fun createCommunity(
        name: String,
        category: String,
        description: String,
        isPrivate: Boolean,
        onResult: (com.example.data.Community?) -> Unit
    ) {
        viewModelScope.launch {
            val com = repository.createCommunity(name, category, description, isPrivate)
            onResult(com)
        }
    }

    fun updateCommunity(
        slug: String,
        name: String?,
        description: String?,
        category: String?,
        isPrivate: Boolean?,
        iconUrl: String? = null,
        onResult: (com.example.data.Community?) -> Unit
    ) {
        viewModelScope.launch {
            val com = repository.updateCommunity(slug, name, description, category, isPrivate, iconUrl)
            onResult(com)
        }
    }

    fun updateCommunityIcon(
        slug: String,
        iconFile: java.io.File,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.updateCommunityIcon(slug, iconFile)
            onResult(success)
        }
    }

    fun updateCommunityIconUrl(
        slug: String,
        iconUrl: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.updateCommunityIconUrl(slug, iconUrl)
            onResult(success)
        }
    }

    val mcpSessionState = com.example.data.McpSessionManager.sessionState

    fun registerNewMcpSession() {
        viewModelScope.launch {
            com.example.data.McpSessionManager.registerLiveSession(
                username = currentUser.value?.username,
                authToken = repository.userToken
            )
        }
    }

    fun callMcpTool(
        toolName: String,
        arguments: Map<String, Any?>,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.example.data.McpSessionManager.callTool(
                toolName = toolName,
                arguments = arguments,
                authToken = repository.userToken
            )
            onResult(result)
        }
    }

    private val _connectedApps = MutableStateFlow<List<com.example.data.ConnectedAppItem>>(emptyList())
    val connectedApps: StateFlow<List<com.example.data.ConnectedAppItem>> = _connectedApps.asStateFlow()

    private val _isLoadingConnectedApps = MutableStateFlow(false)
    val isLoadingConnectedApps: StateFlow<Boolean> = _isLoadingConnectedApps.asStateFlow()

    fun loadConnectedApps() {
        viewModelScope.launch {
            _isLoadingConnectedApps.value = true
            try {
                val apps = repository.getConnectedApps()
                _connectedApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingConnectedApps.value = false
            }
        }
    }

    fun connectAppViaMcp(
        clientUrl: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val username = currentUser.value?.username
            if (username.isNullOrBlank()) {
                onResult(false, "Veuillez vous connecter à un compte")
                return@launch
            }
            if (password.isBlank()) {
                onResult(false, "Mot de passe requis pour autoriser l'application via MCP")
                return@launch
            }
            val res = com.example.data.McpSessionManager.loginWithMcp(username, password, clientUrl)
            res.fold(
                onSuccess = {
                    loadConnectedApps()
                    onResult(true, "Application connectée avec succès (icône & nom OpenGraph récupérés) !")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Échec de connexion de l'application")
                }
            )
        }
    }

    fun connectBatchIntegratedApps(
        urls: List<String>,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val username = currentUser.value?.username
            if (username.isNullOrBlank()) {
                onResult(false, "Veuillez vous connecter à un compte")
                return@launch
            }
            if (password.isBlank()) {
                onResult(false, "Mot de passe requis")
                return@launch
            }
            val res = com.example.data.McpSessionManager.connectIntegratedApps(username, password, urls)
            res.fold(
                onSuccess = { count ->
                    loadConnectedApps()
                    onResult(true, "$count application(s) intégrée(s) enregistrée(s) avec succès !")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Erreur d'intégration")
                }
            )
        }
    }

    fun disconnectApp(appId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.removeConnectedApp(appId)
            if (ok) {
                loadConnectedApps()
            }
            onResult(ok)
        }
    }

    fun deleteCommunity(slug: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteCommunity(slug)
            onResult(success)
        }
    }

    fun updateMemberRole(slug: String, userId: String, role: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.updateMemberRole(slug, userId, role)
            onResult(success)
        }
    }

    fun banCommunityMember(slug: String, userId: String, reason: String? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.banCommunityMember(slug, userId, reason)
            if (success) {
                loadCommunityModActions(slug)
            }
            onResult(success)
        }
    }

    fun loadCommunityBots(slug: String) {
        viewModelScope.launch {
            _isBotsLoading.value = true
            try {
                val bots = repository.getCommunityBots(slug)
                _communityBots.value = bots
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isBotsLoading.value = false
            }
        }
    }

    fun createCommunityBot(
        slug: String,
        name: String,
        bannedWords: List<String>,
        autoBanThreshold: Int,
        welcomeMessage: String?,
        onSuccess: (CommunityBot) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val bot = repository.createCommunityBot(slug, name, bannedWords, autoBanThreshold, welcomeMessage)
            if (bot != null) {
                _communityBots.value = _communityBots.value + bot
                onSuccess(bot)
            } else {
                onError("Échec de la création du bot")
            }
        }
    }

    fun updateCommunityBot(
        slug: String,
        botId: String,
        name: String? = null,
        bannedWords: List<String>? = null,
        autoBanThreshold: Int? = null,
        welcomeMessage: String? = null,
        isActive: Boolean? = null,
        onSuccess: (CommunityBot) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val updated = repository.updateCommunityBot(slug, botId, name, bannedWords, autoBanThreshold, welcomeMessage, isActive)
            if (updated != null) {
                _communityBots.value = _communityBots.value.map { if (it.id == botId) updated else it }
                onSuccess(updated)
            } else {
                if (isActive != null) {
                    _communityBots.value = _communityBots.value.map {
                        if (it.id == botId) it.copy(isActive = isActive) else it
                    }
                    val found = _communityBots.value.find { it.id == botId }
                    if (found != null) {
                        onSuccess(found)
                        return@launch
                    }
                }
                onError("Échec de la mise à jour du bot")
            }
        }
    }

    fun deleteCommunityBot(
        slug: String,
        botId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.deleteCommunityBot(slug, botId)
            if (success) {
                _communityBots.value = _communityBots.value.filter { it.id != botId }
                onSuccess()
            } else {
                onError("Échec de la suppression du bot")
            }
        }
    }

    fun performBotModAction(
        slug: String,
        botId: String,
        botToken: String?,
        actionType: String,
        targetUserId: String?,
        targetActfileId: String?,
        reason: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.performBotModAction(slug, botId, botToken, actionType, targetUserId, targetActfileId, reason)
            if (success) {
                loadCommunityModActions(slug)
                onSuccess()
            } else {
                onError("Échec de l'action de modération du bot")
            }
        }
    }

    fun loadCommunityModActions(slug: String) {
        viewModelScope.launch {
            _isModActionsLoading.value = true
            try {
                val actions = repository.getCommunityModActions(slug)
                _communityModActions.value = actions
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isModActionsLoading.value = false
            }
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            _isStoriesLoading.value = true
            try {
                val list = repository.getStories()
                _stories.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isStoriesLoading.value = false
            }
        }
    }

    fun refreshStories() {
        loadStories()
    }

    fun createStory(
        context: android.content.Context,
        uri: android.net.Uri,
        effect: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isUploadingStory.value = true
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("video") || mimeType.contains("mp4") -> "mp4"
                    mimeType.contains("audio") || mimeType.contains("m4a") -> "m4a"
                    mimeType.contains("webm") -> "webm"
                    else -> "jpg"
                }
                val tempFile = java.io.File.createTempFile("story_upload_", ".$extension", context.cacheDir)
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val success = repository.createStory(tempFile, mimeType, effect)
                try { tempFile.delete() } catch (_: Exception) {}

                if (success) {
                    loadStories()
                    onComplete(true, null)
                } else {
                    onComplete(false, "Impossible d'envoyer la story")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Erreur inconnue")
            } finally {
                _isUploadingStory.value = false
            }
        }
    }

    fun sendStoryReaction(
        receiverId: String,
        storyMediaUrl: String?,
        storyAuthorUsername: String?,
        reaction: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val formattedContent = if (!storyMediaUrl.isNullOrBlank()) {
                    val userLabel = storyAuthorUsername ?: "Story"
                    "[Story:$storyMediaUrl|$userLabel] $reaction"
                } else {
                    "❤️ $reaction Réaction à votre story"
                }
                repository.sendMessage(receiverId, formattedContent, "story_reaction")
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendStoryReaction(
        receiverId: String,
        reaction: String,
        onComplete: () -> Unit = {}
    ) {
        sendStoryReaction(receiverId, null, null, reaction, onComplete)
    }

    fun sendStoryReply(
        receiverId: String,
        replyText: String,
        storyMediaUrl: String?,
        storyAuthorUsername: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val formattedContent = if (!storyMediaUrl.isNullOrBlank()) {
                    val userLabel = storyAuthorUsername ?: "Story"
                    "[Story:$storyMediaUrl|$userLabel] $replyText"
                } else {
                    "📷 Réponse à votre story: $replyText"
                }
                repository.sendMessage(receiverId, formattedContent, "story_reply")
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendImageMessage(
        receiverId: String,
        context: android.content.Context,
        imageUri: android.net.Uri,
        caption: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val extension = if (mimeType.contains("png")) "png" else "jpg"
                val tempFile = java.io.File.createTempFile("chat_img_", ".$extension", context.cacheDir)
                contentResolver.openInputStream(imageUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Upload to cloudinary / server via Retrofit
                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val token = repository.userToken?.let { "Bearer $it" }
                val response = com.example.data.RetrofitClient.apiService.uploadAudio(token, part)
                val uploadedUrl = response.url
                try { tempFile.delete() } catch (_: Exception) {}

                val finalContent = if (caption.isNotBlank()) {
                    "![Image]($uploadedUrl)\n\n$caption"
                } else {
                    "![Image]($uploadedUrl)"
                }
                repository.sendMessage(receiverId, finalContent, "image")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: use local URI if network upload fails
                try {
                    val fallbackContent = if (caption.isNotBlank()) {
                        "![Image]($imageUri)\n\n$caption"
                    } else {
                        "![Image]($imageUri)"
                    }
                    repository.sendMessage(receiverId, fallbackContent, "image")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun deleteConversation(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(userId)
                refreshConversations()
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(messageId, emoji)
        }
    }

    fun removeMessageReaction(messageId: String) {
        viewModelScope.launch {
            repository.removeMessageReaction(messageId)
        }
    }

    fun deleteStory(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteStory(id)
            if (success) {
                _stories.value = _stories.value.filter { it.id != id }
            }
            onComplete(success)
        }
    }

    // ── NIVEAUX & RANGS ──────────────────────────────────────────
    private val _myLevel = MutableStateFlow<com.example.data.UserLevelResponse?>(null)
    val myLevel: StateFlow<com.example.data.UserLevelResponse?> = _myLevel.asStateFlow()

    private val _levelsTable = MutableStateFlow<List<com.example.data.LevelTableItem>>(emptyList())
    val levelsTable: StateFlow<List<com.example.data.LevelTableItem>> = _levelsTable.asStateFlow()

    fun refreshMyLevel() {
        viewModelScope.launch {
            val res = repository.getMyLevel()
            if (res != null) {
                _myLevel.value = res
            }
        }
    }

    fun loadLevelsTable() {
        viewModelScope.launch {
            _levelsTable.value = repository.getLevelsTable()
        }
    }

    suspend fun fetchUserLevel(userId: String): com.example.data.UserLevelResponse? {
        return repository.getUserLevel(userId)
    }

    fun getUserLevelFlow(userId: String): Flow<com.example.data.UserLevelResponse?> = flow {
        emit(repository.getUserLevel(userId))
    }

    // ── IDDET ADS (Sponsorisation d'Actfiles) ──
    fun promoteActfile(
        actfileId: String,
        budget: Double,
        currency: String = "USD",
        daily: Boolean = true,
        targetCountry: String? = null,
        onSuccess: (checkoutUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.promoteActfile(actfileId, budget, currency, daily, targetCountry)
            res.fold(
                onSuccess = { response ->
                    onSuccess(response.checkout_url)
                },
                onFailure = { err ->
                    onError(err.message ?: "Impossible de créer la campagne publicitaire")
                }
            )
        }
    }

    fun getActfileAdStatus(
        actfileId: String,
        onResult: (com.example.data.AdStatusResponse?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.getActfileAdStatus(actfileId)
            onResult(res.getOrNull())
        }
    }

    fun cancelActfileAd(
        actfileId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.cancelActfileAd(actfileId)
            onResult(res.getOrDefault(false))
        }
    }
}
