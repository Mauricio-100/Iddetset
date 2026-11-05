package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import org.json.JSONObject
import android.util.Log
import android.util.Base64
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaType

class IddetRepository(
    private val userDao: UserDao,
    private val actfileDao: ActfileDao,
    private val messageDao: MessageDao,
    private val followDao: FollowDao,
    private val commentDao: CommentDao,
    private val notificationDao: NotificationDao,
    private val savedAccountDao: SavedAccountDao,
    private val channelMessageDao: ChannelMessageDao,
    private val prefs: android.content.SharedPreferences
) {


    private fun getValidToken(): String {
        return prefs.getString("auth_token", null)
            ?: prefs.getString("token", null)
            ?: currentToken
            ?: throw Exception("Veuillez vous connecter à votre compte pour continuer")
    }

    suspend fun getMyIddetPlusStatus(): IddetPlusStatusResponse {
        val token = getValidToken()
        return RetrofitClient.apiService.getMyIddetPlusStatus("Bearer $token")
    }

    suspend fun getMyCard(): UserCardResponse {
        val token = getValidToken()
        return RetrofitClient.apiService.getMyCard("Bearer $token")
    }

    suspend fun getUserCard(userId: String): UserCardResponse {
        val token = prefs.getString("auth_token", null) ?: prefs.getString("token", null) ?: currentToken
        val authHeader = token?.let { "Bearer $it" }
        return RetrofitClient.apiService.getUserCard(authHeader, userId)
    }

    suspend fun updateCardStyle(style: String): CardStyleUpdateResponse {
        val token = getValidToken()
        return RetrofitClient.apiService.updateCardStyle("Bearer $token", CardStyleUpdateRequest(style))
    }

    suspend fun getMyCredits(): CreditsResponse {
        val token = getValidToken()
        return RetrofitClient.apiService.getMyCredits("Bearer $token")
    }

    suspend fun createIddetPlusCheckout(
        currency: String = "CDF"
    ): IddetPlusCheckoutResponse {
        val token = getValidToken()
        val request = IddetPlusCheckoutRequest(currency = currency)
        return RetrofitClient.apiService.createIddetPlusCheckout("Bearer $token", request)
    }

    suspend fun getCheckoutStatus(reference: String): IddetPlusCheckoutStatusResponse {
        val token = getValidToken()
        return RetrofitClient.apiService.getCheckoutStatus("Bearer $token", reference)
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Current logged in user (in-memory mock)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val localChannels = mutableMapOf<String, MutableList<Channel>>()
    private val localCommunities = mutableListOf<Community>()
    
    private val _selectedTheme = MutableStateFlow(AppTheme.valueOf(prefs.getString("selected_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name))
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _targetLanguage = MutableStateFlow(prefs.getString("target_language", "French") ?: "French")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    fun setSelectedTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        prefs.edit().putString("selected_theme", theme.name).apply()
    }

    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        prefs.edit().putString("target_language", language).apply()
    }

    private var currentToken: String? = prefs.getString("auth_token", null)
    val userToken: String? get() = currentToken
    private val viewedActfiles = mutableSetOf<String>()

    init {
        INSTANCE = this
        repositoryScope.launch {
            IddetAccountManager.purgeSimulatedIddetAccount(userDao, actfileDao)
        }
        val savedUserId = prefs.getString("user_id", null)
        val savedUsername = prefs.getString("username", null)
        if (savedUserId != null || currentToken != null) {
            repositoryScope.launch {
                val effectiveId = savedUserId ?: "user_${System.currentTimeMillis()}"
                var user = userDao.getUserById(effectiveId)
                if (user == null) {
                    val savedAccounts = try { savedAccountDao.getAllSavedAccounts().first() } catch (_: Exception) { emptyList() }
                    val acc = savedAccounts.firstOrNull()
                    val uname = acc?.username ?: savedUsername ?: "Moi"
                    user = User(
                        id = effectiveId,
                        username = uname,
                        passwordHash = "mocked",
                        avatarUrl = acc?.avatarUrl,
                        isVerified = acc?.isVerified ?: false
                    )
                    try { userDao.insertUser(user) } catch (_: Exception) {}
                }
                _currentUser.value = user
                try {
                    val token = currentToken
                    if (token != null) {
                        val profile = RetrofitClient.apiService.getMyProfile("Bearer $token")
                        val updatedUser = user.copy(
                            id = profile.id ?: user.id,
                            username = profile.username.ifBlank { user.username },
                            avatarUrl = profile.avatar_url ?: user.avatarUrl,
                            bio = profile.bio ?: user.bio,
                            isVerified = profile.is_verified,
                            followingCount = profile.following_count,
                            followersCount = profile.followers_count
                        )
                        userDao.insertUser(updatedUser)
                        _currentUser.value = updatedUser
                    }
                } catch (e: Exception) {
                    // Ignore 500 error on /api/users/me, user is safely initialized above
                }
            }
        }
    }

    suspend fun refreshProfile() {
        val userId = prefs.getString("user_id", null) ?: _currentUser.value?.id ?: return
        val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
        try {
            val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }

            // 1. Fetch live user profile from /api/users/{userId} which is 100% stable and returns real counts
            val userNet = try {
                RetrofitClient.apiService.getUserProfile(header, userId)
            } catch (e: Exception) {
                val myUsername = _currentUser.value?.username ?: prefs.getString("username", null)
                if (!myUsername.isNullOrBlank() && myUsername != userId) {
                    try { RetrofitClient.apiService.getUserProfile(header, myUsername) } catch (_: Exception) { null }
                } else null
            }

            // 2. Also try getMyProfile, catching potential 500 error on /api/users/me gracefully
            val myProfileNet = try {
                if (header != null) RetrofitClient.apiService.getMyProfile(header) else null
            } catch (e: Exception) {
                null
            }

            val existing = userDao.getUserById(userId) ?: (if (userNet != null) userDao.getUserByUsername(userNet.username) else null)
            val baseUser = existing ?: _currentUser.value

            val updatedUser = if (baseUser != null) {
                baseUser.copy(
                    id = userNet?.id ?: baseUser.id,
                    username = userNet?.username ?: myProfileNet?.username ?: baseUser.username,
                    avatarUrl = userNet?.avatar_url ?: myProfileNet?.avatar_url ?: baseUser.avatarUrl,
                    bio = userNet?.bio ?: myProfileNet?.bio ?: baseUser.bio,
                    isVerified = userNet?.is_verified ?: myProfileNet?.is_verified ?: baseUser.isVerified,
                    followingCount = userNet?.following_count ?: myProfileNet?.following_count ?: baseUser.followingCount,
                    followersCount = userNet?.followers_count ?: myProfileNet?.followers_count ?: baseUser.followersCount,
                    email = myProfileNet?.email ?: baseUser.email,
                    phoneNumber = userNet?.actualPhone ?: myProfileNet?.actualPhone ?: baseUser.phoneNumber,
                    birthDate = userNet?.actualBirthDate ?: myProfileNet?.actualBirthDate ?: baseUser.birthDate,
                    zodiacSign = userNet?.zodiac_sign ?: myProfileNet?.zodiac_sign ?: baseUser.zodiacSign,
                    country = userNet?.country ?: myProfileNet?.country ?: baseUser.country
                )
            } else if (userNet != null) {
                User(
                    id = userNet.id ?: userId,
                    username = userNet.username,
                    passwordHash = "mocked",
                    avatarUrl = userNet.avatar_url,
                    bio = userNet.bio ?: "Bienvenue sur mon profil IDDET !",
                    isVerified = userNet.is_verified,
                    followingCount = userNet.following_count,
                    followersCount = userNet.followers_count,
                    email = myProfileNet?.email,
                    phoneNumber = userNet.actualPhone ?: myProfileNet?.actualPhone,
                    birthDate = userNet.actualBirthDate ?: myProfileNet?.actualBirthDate,
                    zodiacSign = userNet.zodiac_sign ?: myProfileNet?.zodiac_sign,
                    country = userNet.country ?: myProfileNet?.country
                )
            } else null

            if (updatedUser != null) {
                userDao.insertUser(updatedUser)
                _currentUser.value = updatedUser
                prefs.edit()
                    .putString("user_id", updatedUser.id)
                    .putString("username", updatedUser.username)
                    .apply()
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error refreshing profile", e)
        }
    }

    fun parseIso(date: String?): Long {
        if (date.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.time.OffsetDateTime.parse(date).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.Instant.parse(date).toEpochMilli()
            } catch (_: Exception) {
                try {
                    val cleanDate = date.replace("Z", "+0000")
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    format.parse(cleanDate.substringBefore("."))?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun refreshActfiles(targetUserId: String? = null) {
        try {
            val header = currentToken?.let { "Bearer $it" }
            
            var currentCursor: String? = null
            val allNetActfiles = mutableListOf<ActfileNetwork>()
            
            while (true) {
                val batch = try {
                    RetrofitClient.apiService.getActfiles(
                        token = header,
                        limit = 900,
                        cursor = currentCursor
                    )
                } catch (e: Exception) {
                    // Fallback without arguments just in case
                    if (currentCursor == null) {
                        try {
                            RetrofitClient.apiService.getActfiles(token = header)
                        } catch (e2: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
                
                if (batch.isEmpty()) {
                    break
                }
                
                allNetActfiles.addAll(batch)
                
                val lastItem = batch.last()
                val nextCursor = lastItem.id
                if (currentCursor == nextCursor || batch.size < 900) {
                    break
                }
                currentCursor = nextCursor
            }
            
            val usersToInsert = mutableListOf<User>()
            val actfilesToInsert = mutableListOf<Actfile>()

            for (net in allNetActfiles) {
                val existing = userDao.getUserById(net.user_id) ?: userDao.getUserByUsername(net.username)
                val userToInsert = if (existing != null) {
                    existing.copy(
                        id = net.user_id,
                        username = if (net.username.isNotBlank()) net.username else existing.username,
                        avatarUrl = net.avatar_url ?: existing.avatarUrl,
                        isVerified = (net.is_verified ?: false) || existing.isVerified
                    )
                } else {
                    User(
                        id = net.user_id,
                        username = net.username.ifBlank { "Utilisateur" },
                        passwordHash = "mocked",
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified ?: false
                    )
                }
                usersToInsert.add(userToInsert)

                // Detect if this is a private channel conversation (@#salon, @c/... @#... or channel_id)
                val hasChannelTag = net.content.contains("@#")
                val isChannelConversation = !net.channel_id.isNullOrBlank() || hasChannelTag || (net.content.contains("@c/") && hasChannelTag)
                if (isChannelConversation) {
                    val channelIdentifier = if (!net.channel_id.isNullOrBlank()) net.channel_id else {
                        val match = Regex("@#([a-zA-Z0-9_-]+)").find(net.content)
                        match?.groupValues?.get(1) ?: "general"
                    }
                    val communityIdentifier = if (!net.community_id.isNullOrBlank()) net.community_id else {
                        val match = Regex("@c/([a-zA-Z0-9_-]+)").find(net.content)
                        match?.groupValues?.get(1) ?: ""
                    }
                    val cleanText = net.content
                        .replace(Regex("@c/[a-zA-Z0-9_-]+"), "")
                        .replace(Regex("@#[a-zA-Z0-9_-]+"), "")
                        .trim()
                    
                    val channelMsg = ChannelMessage(
                        id = net.id,
                        channelId = channelIdentifier,
                        communitySlug = communityIdentifier,
                        senderId = net.user_id,
                        senderUsername = net.username.ifBlank { "Utilisateur" },
                        senderAvatarUrl = net.avatar_url,
                        isVerified = net.is_verified ?: false,
                        content = if (cleanText.isNotBlank()) cleanText else net.content,
                        type = if (net.content.contains("[!AUDIO]") || net.content.contains("audio_msg_") || net.content.contains("voice_") || net.content.contains("voice://")) "voice" else "text",
                        createdAt = parseIso(net.created_at)
                    )
                    channelMessageDao.insertMessage(channelMsg)
                    
                    // Also clean up any accidental entry from actfiles local table
                    actfileDao.deleteActfileLocal(net.id)
                } else {
                    val parsedSound = ActfileMetadataHelper.parseSound(net.content)
                    val parsedComm = ActfileMetadataHelper.parseCommunity(net.content)
                    val effectiveCommSlug = net.community_slug?.ifBlank { null } ?: net.community_id?.ifBlank { null } ?: net.channel_slug?.ifBlank { null } ?: if (net.content.contains("@c/")) {
                        Regex("@c/([a-zA-Z0-9_-]+)").find(net.content)?.groupValues?.get(1)
                    } else null
                    val effectiveCommName = net.community_name?.ifBlank { null } ?: parsedComm?.first?.ifBlank { null } ?: net.channel_name?.ifBlank { null }
                    val effectiveCommIcon = net.community_icon_url?.ifBlank { null } ?: parsedComm?.second?.ifBlank { null }

                    actfilesToInsert.add(
                        Actfile(
                            id = net.id,
                            userId = net.user_id,
                            content = net.content,
                            likesCount = net.likes_count,
                            viewsCount = net.views_count,
                            commentsCount = net.comments_count ?: 0,
                            createdAt = parseIso(net.created_at),
                            isLikedByMe = net.liked ?: false,
                            category = net.category,
                            communityId = effectiveCommSlug,
                            channelId = net.channel_id,
                            channelSlug = net.channel_slug ?: effectiveCommSlug,
                            channelName = net.channel_name,
                            soundId = parsedSound?.id,
                            soundTitle = parsedSound?.title,
                            soundAuthor = parsedSound?.author,
                            soundAudioUrl = parsedSound?.audioUrl,
                            soundCoverUrl = parsedSound?.coverUrl,
                            communityName = effectiveCommName,
                            communityIconUrl = effectiveCommIcon,
                            communityIsVerified = (net.community_is_verified ?: false),
                            isSponsored = net.is_sponsored ?: false,
                            adCampaignId = net.ad_campaign_id,
                            adStatus = net.ad_status ?: "none",
                            adBudget = net.ad_budget,
                            adCurrency = net.ad_currency
                        )
                    )
                }
            }

            // Insert new and updated actfiles without wiping out old cached profile actfiles
            if (usersToInsert.isNotEmpty()) {
                userDao.insertUsers(usersToInsert)
            }
            if (actfilesToInsert.isNotEmpty()) {
                actfileDao.insertActfiles(actfilesToInsert)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun login(username: String, password: String? = null) {
        try {
            val response = RetrofitClient.apiService.login(username, password)
            currentToken = response.access_token
            prefs.edit().putString("auth_token", currentToken).apply()
            
            val serverUserId = response.user_id?.ifBlank { null }
            val serverUsername = response.username?.ifBlank { null } ?: username
            val serverAvatar = response.avatar_url?.ifBlank { null }
            val serverVerified = response.is_verified

            val user = try {
                val profile = RetrofitClient.apiService.getMyProfile("Bearer $currentToken")
                val resolvedId = profile.id ?: serverUserId ?: java.util.UUID.randomUUID().toString()
                val existing = userDao.getUserById(resolvedId)
                if (existing != null) {
                    existing.copy(
                        username = profile.username,
                        avatarUrl = profile.avatar_url ?: serverAvatar,
                        bio = profile.bio ?: existing.bio,
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count
                    )
                } else {
                    User(
                        id = resolvedId,
                        username = profile.username,
                        passwordHash = "mocked",
                        avatarUrl = profile.avatar_url ?: serverAvatar,
                        bio = profile.bio ?: "Welcome to my profile!",
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count,
                        isGiant = (profile.username.length > 5)
                    )
                }
            } catch (profileError: Exception) {
                // If /api/users/me throws 500, resolve ID from login response or search
                val resolvedId = serverUserId ?: try {
                    val search = RetrofitClient.apiService.search("Bearer $currentToken", username, "users")
                    search.results.firstOrNull { it.username.equals(username, ignoreCase = true) }?.id
                } catch (_: Exception) { null } ?: (userDao.getUserByUsername(username)?.id) ?: java.util.UUID.randomUUID().toString()

                val existing = userDao.getUserById(resolvedId) ?: userDao.getUserByUsername(username)
                existing?.copy(id = resolvedId, avatarUrl = serverAvatar ?: existing.avatarUrl) ?: User(
                    id = resolvedId,
                    username = serverUsername,
                    passwordHash = "mocked",
                    avatarUrl = serverAvatar,
                    bio = "Welcome to Iddet!",
                    isVerified = serverVerified,
                    isGiant = (serverUsername.length > 5)
                )
            }
            userDao.insertUser(user)
            _currentUser.value = user
            prefs.edit()
                .putString("user_id", user.id)
                .putString("username", user.username)
                .apply()
            
            // Immediately store or update the account in saved accounts
            savedAccountDao.insertSavedAccount(
                SavedAccount(
                    username = user.username,
                    avatarUrl = user.avatarUrl,
                    token = currentToken ?: "",
                    isVerified = user.isVerified
                )
            )

            // Register FCM device token with backend
            currentToken?.let { token ->
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                        com.iddet.calls.IddetFirebaseService.registerToken(token, fcmToken)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("IddetRepository", "FCM token register on login", e)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to login: ${e.message}")
        }
    }

    suspend fun signup(username: String, password: String? = null) {
        try {
            RetrofitClient.apiService.signup(RegisterRequest(username, password))
            login(username, password)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to signup: ${e.message}")
        }
    }

    fun toIso2Country(countryName: String?): String? {
        if (countryName.isNullOrBlank()) return null
        val c = countryName.trim()
            .removePrefix("🇨🇩").removePrefix("🇨🇬").removePrefix("🇫🇷")
            .removePrefix("🇧🇪").removePrefix("🇨🇦").removePrefix("🇨🇮")
            .removePrefix("🇸🇳").removePrefix("🇨🇲").removePrefix("🇺🇸")
            .removePrefix("🇨🇭").trim()
        if (c.length == 2) return c.uppercase()
        return when {
            c.contains("RDC", ignoreCase = true) || c.contains("Congo (RDC)", ignoreCase = true) || c.contains("Kinshasa", ignoreCase = true) || c.contains("Démocratique", ignoreCase = true) -> "CD"
            c.equals("Congo", ignoreCase = true) || c.contains("Brazzaville", ignoreCase = true) -> "CG"
            c.contains("France", ignoreCase = true) -> "FR"
            c.contains("Belg", ignoreCase = true) -> "BE"
            c.contains("Canada", ignoreCase = true) -> "CA"
            c.contains("Ivoire", ignoreCase = true) -> "CI"
            c.contains("Sénégal", ignoreCase = true) || c.contains("Senegal", ignoreCase = true) -> "SN"
            c.contains("Cameroun", ignoreCase = true) || c.contains("Cameroon", ignoreCase = true) -> "CM"
            c.contains("USA", ignoreCase = true) || c.contains("États-Unis", ignoreCase = true) || c.contains("United States", ignoreCase = true) -> "US"
            c.contains("Suisse", ignoreCase = true) || c.contains("Swiss", ignoreCase = true) -> "CH"
            else -> c.take(2).uppercase()
        }
    }

    fun toStandardBirthDate(inputDate: String?): String? {
        if (inputDate.isNullOrBlank()) return null
        val trimmed = inputDate.trim()
        if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return trimmed
        val parts = trimmed.split('/', '-', '.')
        if (parts.size == 3) {
            if (parts[0].length == 4) {
                val y = parts[0]
                val m = parts[1].padStart(2, '0')
                val d = parts[2].padStart(2, '0')
                return "$y-$m-$d"
            } else if (parts[2].length == 4) {
                val d = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                val y = parts[2]
                return "$y-$m-$d"
            }
        }
        return trimmed
    }

    suspend fun updateProfile(
        username: String? = null,
        avatarUrl: String? = null,
        bio: String,
        privacySetting: String,
        email: String? = null,
        phoneNumber: String? = null,
        birthDate: String? = null,
        zodiacSign: String? = null,
        country: String? = null,
        preferredCategory: String? = null
    ) {
        val user = _currentUser.value ?: return
        val resolvedPhone = phoneNumber ?: user.phoneNumber
        val resolvedBirthDate = birthDate ?: user.birthDate
        val resolvedCountry = country ?: user.country
        val resolvedEmail = email ?: user.email

        val updatedUser = user.copy(
            username = username ?: user.username,
            avatarUrl = avatarUrl ?: user.avatarUrl,
            bio = bio,
            privacySetting = privacySetting,
            email = resolvedEmail,
            phoneNumber = resolvedPhone,
            birthDate = resolvedBirthDate,
            zodiacSign = zodiacSign ?: user.zodiacSign,
            country = resolvedCountry,
            preferredCategory = preferredCategory ?: user.preferredCategory
        )
        userDao.insertUser(updatedUser)
        _currentUser.value = updatedUser

        prefs.edit().apply {
            if (!resolvedEmail.isNullOrBlank()) putString("email", resolvedEmail)
            if (!resolvedPhone.isNullOrBlank()) putString("phone", resolvedPhone)
            if (!resolvedCountry.isNullOrBlank()) putString("country", resolvedCountry)
            if (!resolvedBirthDate.isNullOrBlank()) putString("birth_date", resolvedBirthDate)
        }.apply()

        // Sync with server if possible
        try {
            val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
            if (token != null) {
                val header = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 1. Update Bio & Phone number via Multipart PUT /api/users/me
                try {
                    val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
                    val phoneBody = resolvedPhone?.toRequestBody("text/plain".toMediaTypeOrNull())
                    RetrofitClient.apiService.updateProfileMultipart(
                        token = header,
                        avatar = null,
                        bio = bioBody,
                        phoneNumber = phoneBody
                    )
                } catch (e: Exception) {
                    Log.w("IddetRepository", "updateProfileMultipart error", e)
                }

                // 2. Update country with ISO2 standard code via PUT /api/users/me/country
                val iso2 = toIso2Country(resolvedCountry)
                if (!iso2.isNullOrBlank()) {
                    try {
                        RetrofitClient.apiService.setMyCountry(header, CountryUpdateRequest(iso2))
                    } catch (e: Exception) {
                        Log.w("IddetRepository", "setMyCountry error", e)
                    }
                }

                // 3. Update birth date via POST /api/users/me/verify
                val isoBirthDate = toStandardBirthDate(resolvedBirthDate)
                if (!isoBirthDate.isNullOrBlank()) {
                    try {
                        RetrofitClient.apiService.requestVerification(header, VerificationRequest(birth_date = isoBirthDate))
                    } catch (e: Exception) {
                        Log.w("IddetRepository", "requestVerification error", e)
                    }
                }

                // 4. Refresh authoritative user profile from server
                refreshProfile()
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Failed to update profile", e)
            e.printStackTrace()
        }
    }

    suspend fun updateProfileWithImage(
        avatarFile: java.io.File?,
        bio: String?,
        phoneNumber: String?
    ): UserProfileNetwork {
        val token = currentToken ?: throw Exception("Not logged in")
        
        val avatarPart = if (avatarFile != null) {
            val mediaType = "image/jpeg".toMediaType()
            val requestFile = avatarFile.asRequestBody(mediaType)
            MultipartBody.Part.createFormData("avatar", avatarFile.name, requestFile)
        } else null
        
        val bioBody = if (bio != null) {
            bio.toRequestBody("text/plain".toMediaType())
        } else null
        
        val phoneBody = if (phoneNumber != null) {
            phoneNumber.toRequestBody("text/plain".toMediaType())
        } else null
        
        val profile = RetrofitClient.apiService.updateProfileMultipart(
            token = "Bearer $token",
            avatar = avatarPart,
            bio = bioBody,
            phoneNumber = phoneBody
        )
        
        val user = _currentUser.value
        if (user != null) {
            val updatedUser = user.copy(
                username = profile.username,
                avatarUrl = profile.avatar_url,
                bio = profile.bio ?: user.bio,
                isVerified = profile.is_verified,
                email = profile.email ?: user.email,
                phoneNumber = profile.phone_number ?: user.phoneNumber,
                zodiacSign = profile.zodiac_sign ?: user.zodiacSign,
                followersCount = profile.followers_count,
                followingCount = profile.following_count
            )
            userDao.insertUser(updatedUser)
            _currentUser.value = updatedUser
        }
        
        return profile
    }

    fun getSavedAccounts(): Flow<List<SavedAccount>> {
        return savedAccountDao.getAllSavedAccounts()
    }

    fun removeSavedAccount(username: String) {
        repositoryScope.launch {
            savedAccountDao.deleteSavedAccount(username)
        }
    }

    private fun extractJwtClaims(token: String): Pair<String?, String?> {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
                val json = org.json.JSONObject(String(payloadBytes, Charsets.UTF_8))
                val sub = if (json.has("sub")) json.optString("sub").ifBlank { null } else null
                val username = if (json.has("username")) json.optString("username").ifBlank { null } else null
                Pair(sub, username)
            } else Pair(null, null)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    suspend fun loginWithToken(token: String) {
        currentToken = token
        prefs.edit().putString("auth_token", token).apply()
        val claims = extractJwtClaims(token)
        val jwtUserId = claims.first
        val jwtUsername = claims.second
        
        try {
            val user = try {
                val profile = RetrofitClient.apiService.getMyProfile("Bearer $token")
                val resolvedId = profile.id ?: jwtUserId ?: java.util.UUID.randomUUID().toString()
                val existing = userDao.getUserById(resolvedId)
                if (existing != null) {
                    existing.copy(
                        username = profile.username.ifBlank { existing.username },
                        avatarUrl = profile.avatar_url ?: existing.avatarUrl,
                        bio = profile.bio ?: existing.bio,
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count,
                        country = profile.country ?: existing.country,
                        birthDate = profile.birth_date ?: existing.birthDate,
                        zodiacSign = profile.zodiac_sign ?: existing.zodiacSign
                    )
                } else {
                    User(
                        id = resolvedId,
                        username = profile.username.ifBlank { jwtUsername ?: "Moi" },
                        passwordHash = "mocked",
                        avatarUrl = profile.avatar_url,
                        bio = profile.bio ?: "Welcome to my profile!",
                        isVerified = profile.is_verified,
                        followingCount = profile.following_count,
                        followersCount = profile.followers_count,
                        country = profile.country,
                        birthDate = profile.birth_date,
                        zodiacSign = profile.zodiac_sign,
                        isGiant = (profile.username.length > 5)
                    )
                }
            } catch (profileError: Exception) {
                profileError.printStackTrace()
                // Try live user profile by ID if we have it from JWT
                val netUser = if (jwtUserId != null) {
                    try { RetrofitClient.apiService.getUserProfile("Bearer $token", jwtUserId) } catch (_: Exception) { null }
                } else if (jwtUsername != null) {
                    try { RetrofitClient.apiService.getUserProfile("Bearer $token", jwtUsername) } catch (_: Exception) { null }
                } else null

                val savedUid = prefs.getString("user_id", null) ?: jwtUserId
                val existing = if (savedUid != null) userDao.getUserById(savedUid) else null
                val fallbackUser = existing ?: run {
                    val savedAcc = savedAccountDao.getAccountByToken(token)
                    if (savedAcc != null) {
                        userDao.getUserByUsername(savedAcc.username)
                    } else null
                } ?: User(
                    id = netUser?.id ?: jwtUserId ?: savedUid ?: java.util.UUID.randomUUID().toString(),
                    username = netUser?.username ?: jwtUsername ?: prefs.getString("username", null) ?: "Utilisateur",
                    passwordHash = "mocked",
                    avatarUrl = netUser?.avatar_url,
                    bio = netUser?.bio ?: "Bienvenue sur IDDET !",
                    isVerified = netUser?.is_verified ?: false,
                    followingCount = netUser?.following_count ?: 0,
                    followersCount = netUser?.followers_count ?: 0,
                    country = netUser?.country,
                    birthDate = netUser?.birth_date,
                    zodiacSign = netUser?.zodiac_sign
                )
                fallbackUser
            }
            userDao.insertUser(user)
            _currentUser.value = user
            prefs.edit()
                .putString("user_id", user.id)
                .putString("username", user.username)
                .apply()

            // Immediately store or update the account in saved accounts
            savedAccountDao.insertSavedAccount(
                SavedAccount(
                    username = user.username,
                    avatarUrl = user.avatarUrl,
                    token = token,
                    isVerified = user.isVerified
                )
            )
        } catch (e: Exception) {
            // ONLY remove token if the server explicitly returned 401 Unauthorized
            if (e is retrofit2.HttpException && e.code() == 401) {
                currentToken = null
                prefs.edit().remove("auth_token").apply()
            }
            throw e
        }
    }

    fun logout() {
        val u = _currentUser.value
        val token = currentToken
        if (token != null) {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    com.iddet.calls.IddetFirebaseService.unregisterToken(token, fcmToken)
                }
            } catch (e: Exception) {
                android.util.Log.w("IddetRepository", "FCM token unregister on logout", e)
            }
        }
        com.iddet.calls.CallManagerHolder.instance?.stop()
        com.iddet.calls.CallManagerHolder.instance = null

        if (u != null && token != null) {
            repositoryScope.launch {
                savedAccountDao.insertSavedAccount(
                    SavedAccount(
                        username = u.username,
                        avatarUrl = u.avatarUrl,
                        token = token,
                        isVerified = u.isVerified
                    )
                )
            }
        }
        _currentUser.value = null
        currentToken = null
        prefs.edit().clear().apply()
    }

    fun normalizeCategory(raw: String?): String {
        return com.example.ui.components.normalizeToActfileCategory(raw)
    }

    suspend fun publishActfile(
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
    ): String? {
        val user = _currentUser.value ?: return null
        val validCategory = normalizeCategory(category)
        val targetUserId = user.id
        var createdActfileId: String? = null

        val embeddedContent = buildString {
            append(content)
            if (!soundTitle.isNullOrBlank()) {
                val sId = soundId ?: ""
                val sTitle = soundTitle.replace("\"", "\\\"")
                val sAuthor = (soundAuthor ?: "").replace("\"", "\\\"")
                val sUrl = soundAudioUrl ?: ""
                val sCover = soundCoverUrl ?: ""
                append("\n<!--sound:{\"id\":\"$sId\",\"title\":\"$sTitle\",\"author\":\"$sAuthor\",\"url\":\"$sUrl\",\"cover\":\"$sCover\"}-->")
            }
            if (!communityName.isNullOrBlank() && !content.contains("@c/")) {
                val cName = communityName.replace("\"", "\\\"")
                val cIcon = (communityIconUrl ?: "").replace("\"", "\\\"")
                append("\n<!--community:{\"name\":\"$cName\",\"icon\":\"$cIcon\"}-->")
            }
        }
        
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val netActfile = RetrofitClient.apiService.publishActfile(
                    token = header,
                    request = PublishActfileRequest(
                        content = embeddedContent,
                        category = validCategory,
                        community_slug = communityId?.takeIf { it.isNotBlank() },
                        channel_slug = channelId?.takeIf { it.isNotBlank() }
                    )
                )
                val toInsert = Actfile(
                    id = netActfile.id,
                    userId = if (netActfile.user_id.isNotBlank()) netActfile.user_id else targetUserId,
                    content = if (netActfile.content.isNotBlank()) netActfile.content else embeddedContent,
                    tags = tags,
                    likesCount = netActfile.likes_count,
                    viewsCount = netActfile.views_count,
                    createdAt = if (netActfile.created_at.isNotBlank()) parseIso(netActfile.created_at) else System.currentTimeMillis(),
                    category = netActfile.category ?: validCategory,
                    communityId = netActfile.community_id ?: communityId,
                    channelId = netActfile.channel_id ?: channelId,
                    channelSlug = netActfile.channel_slug,
                    channelName = netActfile.channel_name,
                    soundId = soundId,
                    soundTitle = soundTitle,
                    soundAuthor = soundAuthor,
                    soundAudioUrl = soundAudioUrl,
                    soundCoverUrl = soundCoverUrl,
                    communityName = communityName,
                    communityIconUrl = communityIconUrl
                )
                actfileDao.insertActfile(toInsert)
                createdActfileId = toInsert.id
            } else {
                val toInsert = Actfile(
                    userId = targetUserId,
                    content = embeddedContent,
                    tags = tags,
                    category = validCategory,
                    communityId = communityId,
                    channelId = channelId,
                    channelSlug = communityId,
                    channelName = communityName,
                    soundId = soundId,
                    soundTitle = soundTitle,
                    soundAuthor = soundAuthor,
                    soundAudioUrl = soundAudioUrl,
                    soundCoverUrl = soundCoverUrl,
                    communityName = communityName,
                    communityIconUrl = communityIconUrl
                )
                actfileDao.insertActfile(toInsert)
                createdActfileId = toInsert.id
            }
        } catch(e: Exception) {
            e.printStackTrace()
            // fallback
            val toInsert = Actfile(
                userId = targetUserId,
                content = embeddedContent,
                tags = tags,
                category = validCategory,
                communityId = communityId,
                channelId = channelId,
                channelSlug = communityId,
                channelName = communityName,
                soundId = soundId,
                soundTitle = soundTitle,
                soundAuthor = soundAuthor,
                soundAudioUrl = soundAudioUrl,
                soundCoverUrl = soundCoverUrl,
                communityName = communityName,
                communityIconUrl = communityIconUrl
            )
            actfileDao.insertActfile(toInsert)
            createdActfileId = toInsert.id
        }

        // Simple mock leveling up logic
        val newXp = user.xp + 50
        val newLevel = (newXp / 100) + 1
        var newBadges = user.badges
        if (newLevel >= 2 && !newBadges.contains("Creator")) {
            newBadges = if (newBadges.isEmpty()) "Creator" else "$newBadges, Creator"
        }
        val updatedUser = user.copy(xp = newXp, level = newLevel, badges = newBadges)
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser
        return createdActfileId
    }

    suspend fun deleteActfile(actfileId: String) {
        actfileDao.deleteActfileLocal(actfileId)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.deleteActfile(header, actfileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllActfiles(): Flow<List<ActfileWithUser>> {
        return actfileDao.getAllActfilesWithUser()
    }
    
    fun getUserActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return actfileDao.getActfilesByUser(userId)
    }

    suspend fun incrementView(actfileId: String) {
        if (viewedActfiles.contains(actfileId)) return
        viewedActfiles.add(actfileId)
        actfileDao.incrementViewCount(actfileId)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.viewActfile(header, actfileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun likeActfile(actfileId: String) {
        val raw = actfileDao.getActfileRaw(actfileId) ?: return
        val currentLiked = raw.isLikedByMe
        val newLiked = !currentLiked
        val newLikesCount = if (newLiked) raw.likesCount + 1 else kotlin.math.max(0, raw.likesCount - 1)
        
        // Instant visual feedback locally
        val updatedActfile = raw.copy(
            isLikedByMe = newLiked,
            likesCount = newLikesCount
        )
        actfileDao.insertActfile(updatedActfile)
        
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.likeActfile(header, actfileId)
                val serverLikedState = res["liked"] ?: res["success"] ?: newLiked
                if (serverLikedState is Boolean && serverLikedState != newLiked) {
                    val finalActfile = raw.copy(
                        isLikedByMe = serverLikedState,
                        likesCount = if (serverLikedState) {
                            if (!currentLiked) raw.likesCount + 1 else raw.likesCount
                        } else {
                            if (currentLiked) kotlin.math.max(0, raw.likesCount - 1) else raw.likesCount
                        }
                    )
                    actfileDao.insertActfile(finalActfile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (newLiked) {
            val user = _currentUser.value ?: return
            val newXp = user.xp + 10
            val newLevel = (newXp / 100) + 1
            var newBadges = user.badges
            if (newLevel >= 5 && !newBadges.contains("Active")) {
                newBadges = if (newBadges.isEmpty()) "Active" else "$newBadges, Active"
            }
            val updatedUser = user.copy(xp = newXp, level = newLevel, badges = newBadges)
            userDao.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    suspend fun commentActfile(actfileId: String, content: String) {
        actfileDao.incrementCommentCount(actfileId)
        val myUser = _currentUser.value
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.commentActfile(header, actfileId, ActfileCommentCreate(content))
                commentDao.insertComment(
                    ActfileComment(
                        id = res.id,
                        actfileId = actfileId,
                        content = res.content,
                        userId = res.user?.id ?: myUser?.id ?: "unknown",
                        username = res.user?.username ?: myUser?.username ?: "Me",
                        avatarUrl = res.user?.avatar_url ?: myUser?.avatarUrl,
                        isVerified = myUser?.isVerified ?: false,
                        createdAt = parseIso(res.created_at)
                    )
                )
            } else {
                commentDao.insertComment(
                    ActfileComment(
                        actfileId = actfileId,
                        content = content,
                        userId = myUser?.id ?: "local_user",
                        username = myUser?.username ?: "Me",
                        avatarUrl = myUser?.avatarUrl,
                        isVerified = myUser?.isVerified ?: false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local insertion
            commentDao.insertComment(
                ActfileComment(
                    actfileId = actfileId,
                    content = content,
                    userId = myUser?.id ?: "local_user",
                    username = myUser?.username ?: "Me",
                    avatarUrl = myUser?.avatarUrl,
                    isVerified = myUser?.isVerified ?: false
                )
            )
        }
    }

    fun getComments(actfileId: String): Flow<List<ActfileComment>> {
        repositoryScope.launch {
            try {
                val header = currentToken?.let { "Bearer $it" }
                val commentsList = RetrofitClient.apiService.getActfileComments(header, actfileId)
                val mapped = commentsList.map {
                    ActfileComment(
                        id = it.id,
                        actfileId = actfileId,
                        content = it.content,
                        userId = it.user_id,
                        username = it.username,
                        avatarUrl = it.avatar_url,
                        isVerified = it.is_verified,
                        createdAt = parseIso(it.created_at)
                    )
                }
                commentDao.deleteCommentsForActfile(actfileId)
                commentDao.insertComments(mapped)
                actfileDao.updateCommentCount(actfileId, mapped.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return commentDao.getCommentsForActfile(actfileId)
    }

    fun getActfile(actfileId: String): Flow<ActfileWithUser?> {
        return actfileDao.getActfileById(actfileId)
    }

    fun searchUsers(query: String): Flow<List<User>> = kotlinx.coroutines.flow.flow {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.search(header, query, "users")
                emit(res.results.map {
                    User(
                        id = it.id,
                        username = it.username ?: "Unknown",
                        passwordHash = "mocked",
                        avatarUrl = it.avatar_url,
                        bio = it.bio ?: "",
                        isVerified = it.is_verified ?: false
                    )
                })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun searchActfiles(query: String): Flow<List<ActfileWithUser>> {
        return actfileDao.searchActfiles(query)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getGiants(): Flow<List<User>> {
        return _currentUser.flatMapLatest { me ->
            val userId = me?.id ?: ""
            userDao.getSuggestedUsers(userId)
        }
    }
    
    fun getUserFlow(userId: String): Flow<User?> {
        repositoryScope.launch {
            refreshUserProfile(userId)
        }
        return userDao.getUserByIdFlow(userId)
    }

    suspend fun refreshUserProfile(userId: String) {
        try {
            val header = currentToken?.let { "Bearer $it" }
            val res = RetrofitClient.apiService.getUserProfile(header, userId)
            val resolvedId = res.id ?: userId
            val existing = userDao.getUserById(resolvedId) ?: userDao.getUserByUsername(res.username)
            val user = if (existing != null) {
                existing.copy(
                    id = resolvedId,
                    username = res.username,
                    avatarUrl = res.avatar_url ?: existing.avatarUrl,
                    bio = res.bio ?: existing.bio,
                    isVerified = res.is_verified,
                    followersCount = res.followers_count,
                    followingCount = res.following_count,
                    country = res.country ?: existing.country,
                    birthDate = res.birth_date ?: existing.birthDate,
                    zodiacSign = res.zodiac_sign ?: existing.zodiacSign
                )
            } else {
                User(
                    id = resolvedId,
                    username = res.username,
                    passwordHash = "mocked",
                    avatarUrl = res.avatar_url,
                    bio = res.bio ?: "",
                    isVerified = res.is_verified,
                    followersCount = res.followers_count,
                    followingCount = res.following_count,
                    country = res.country,
                    birthDate = res.birth_date,
                    zodiacSign = res.zodiac_sign
                )
            }
            userDao.insertUser(user)

            // Synchronize local follow status with server's authoritative state
            val myId = _currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
            if (myId.isNotBlank() && myId != resolvedId) {
                if (res.is_following == true) {
                    followDao.insertFollow(Follow(followerId = myId, followingId = resolvedId))
                    if (res.username.isNotBlank()) {
                        followDao.insertFollow(Follow(followerId = myId, followingId = res.username))
                    }
                } else if (res.is_following == false) {
                    followDao.deleteFollow(myId, resolvedId)
                    if (res.username.isNotBlank()) {
                        followDao.deleteFollow(myId, res.username)
                    }
                }
            }
            
            // Also sync and pull actfiles belonging to this user
            refreshActfiles(targetUserId = resolvedId)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                refreshActfiles()
            } catch (_: Exception) {}
        }
    }

    private val _conversations = MutableStateFlow<List<ConversationNetwork>>(emptyList())
    val conversations: StateFlow<List<ConversationNetwork>> = _conversations.asStateFlow()

    fun updateConversationLastMessage(otherUserId: String, content: String, type: String = "text", isIncoming: Boolean = false, senderUsername: String? = null) {
        val current = _conversations.value.toMutableList()
        val index = current.indexOfFirst { it.user_id == otherUserId }
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
        val displayContent = if (type.startsWith("audio")) {
            if (content.startsWith("http") || content.startsWith("/") || content.startsWith("[Voice Message]")) {
                content
            } else {
                "[Voice Message](voice://duration=5&amplitudes=0.5)"
            }
        } else {
            content
        }
        
        if (index != -1) {
            val existing = current.removeAt(index)
            val updated = existing.copy(
                last_message = displayContent,
                last_message_time = nowIso,
                unread_count = if (isIncoming) (existing.unread_count ?: 0) + 1 else existing.unread_count
            )
            current.add(0, updated)
        } else {
            val newConv = ConversationNetwork(
                id = "temp_conv_${otherUserId}",
                user_id = otherUserId,
                username = senderUsername ?: "Utilisateur",
                avatar_url = null,
                last_message = displayContent,
                last_message_time = nowIso,
                unread_count = if (isIncoming) 1 else 0,
                is_online = true,
                is_verified = false
            )
            current.add(0, newConv)
        }
        _conversations.value = current
    }

    // ── Location & Radar Map Methods ──
    suspend fun sendLocationUpdate(lat: Double, lng: Double, isSharing: Boolean = true, detectCountry: Boolean = true): LocationUpdateResponse? {
        val token = currentToken ?: return null
        return try {
            val response = RetrofitClient.apiService.updateLocation(
                token = "Bearer $token",
                request = LocationUpdateRequest(
                    latitude = lat,
                    longitude = lng,
                    is_sharing = isSharing,
                    detect_country = detectCountry
                )
            )
            FriendsLocationService.updateServerSpeed(response.speed_kmh)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun stopLocationSharing(): Boolean {
        val token = currentToken ?: return false
        return try {
            RetrofitClient.apiService.stopLocationSharing("Bearer $token")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchNearbyFriends(limit: Int = 50): List<NearbyFriendNetwork> {
        val token = currentToken ?: return emptyList()
        return try {
            RetrofitClient.apiService.getNearbyFriends("Bearer $token", limit)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun setManualCountry(countryCode: String): Boolean {
        val token = currentToken ?: return false
        return try {
            RetrofitClient.apiService.setMyCountry("Bearer $token", CountryUpdateRequest(countryCode))
            _currentUser.value?.id?.let { refreshUserProfile(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun redetectAutoCountry(): Boolean {
        val token = currentToken ?: return false
        return try {
            RetrofitClient.apiService.redetectMyCountryAuto("Bearer $token")
            _currentUser.value?.id?.let { refreshUserProfile(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCategories(): List<String> {
        return try {
            RetrofitClient.apiService.getCategories().categories
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("Fun", "Amour", "Motivation", "Tech", "Sport", "Musique", "Actu", "Business", "Spiritualité", "Autres")
        }
    }

    suspend fun getMyPreferredCategories(): List<String> {
        val token = currentToken ?: return emptyList()
        return try {
            RetrofitClient.apiService.getMyPreferredCategories("Bearer $token").preferred_categories
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updatePreferredCategories(categories: List<String>): List<String> {
        val token = currentToken ?: return emptyList()
        return try {
            val response = RetrofitClient.apiService.updatePreferredCategories(
                token = "Bearer $token",
                body = UpdateCategoriesRequest(categories = categories)
            )
            response.preferred_categories
        } catch (e: Exception) {
            e.printStackTrace()
            categories
        }
    }

    suspend fun refreshConversations(): List<ConversationNetwork> {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null) ?: return _conversations.value
        return try {
            val header = "Bearer $token"
            val res = RetrofitClient.apiService.getConversations(header)
            val enriched = res.map { conv ->
                val effectiveId = if (conv.user_id.isNotBlank()) conv.user_id else (conv.id ?: "")
                val existing = try { userDao.getUserById(effectiveId) } catch (_: Exception) { null }
                val effectiveAvatar = if (!conv.avatar_url.isNullOrBlank()) {
                    conv.avatar_url
                } else {
                    existing?.avatarUrl
                }
                val effectiveUsername = if (conv.username.isNotBlank()) conv.username else (existing?.username ?: "Utilisateur")
                conv.copy(
                    user_id = effectiveId,
                    username = effectiveUsername,
                    avatar_url = effectiveAvatar
                )
            }
            if (enriched.isNotEmpty() || _conversations.value.isEmpty()) {
                _conversations.value = enriched
            }
            enriched
        } catch (e: Exception) {
            android.util.Log.e("IddetRepository", "refreshConversations error: ${e.message}")
            _conversations.value
        }
    }

    suspend fun getConversations(): List<ConversationNetwork> {
        val refreshed = refreshConversations()
        return if (refreshed.isNotEmpty()) refreshed else _conversations.value
    }

    suspend fun markMessagesRead(userId: String) {
        try {
            val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
            val header = token?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.markMessagesRead(header, userId)
            }
        } catch (_: Exception) {}
    }

    suspend fun getDirectMessages(otherUserId: String): List<MessageNetwork> {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null) ?: return emptyList()
        return try {
            val header = "Bearer $token"
            val res = RetrofitClient.apiService.getMessages(header, otherUserId)
            if (res.isNotEmpty()) {
                val localList = res.mapNotNull { net ->
                    if (net.id.isBlank()) return@mapNotNull null
                    val parsedTime = try {
                        val cleanDate = net.created_at.replace("Z", "+0000")
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", java.util.Locale.US)
                        format.parse(cleanDate)?.time ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).parse(cleanDate)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    Message(
                        id = net.id,
                        senderId = net.sender_id,
                        receiverId = net.receiver_id,
                        content = net.content,
                        type = net.type,
                        isRead = net.read,
                        createdAt = parsedTime,
                        reaction = net.reaction
                    )
                }
                if (localList.isNotEmpty()) {
                    messageDao.insertMessages(localList)
                }
            }
            res
        } catch (e: Exception) {
            android.util.Log.e("IddetRepository", "getDirectMessages error for $otherUserId: ${e.message}")
            emptyList()
        }
    }

    suspend fun refreshMessagesWith(otherUserId: String) {
        getDirectMessages(otherUserId)
    }

    fun getChatPartners(): Flow<List<User>> {
        val userId = _currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
        repositoryScope.launch {
            refreshConversations()
        }
        return userDao.getChatPartners(userId)
    }

    fun getMessagesWith(otherUserId: String): Flow<List<Message>> {
        repositoryScope.launch {
            refreshMessagesWith(otherUserId)
        }
        return messageDao.getMessagesWithPartner(otherUserId)
    }

    suspend fun sendMessage(receiverId: String, content: String, type: String = "text") {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
        if (token == null) {
            android.util.Log.e("IddetRepository", "Cannot send message: No token")
            throw Exception("Non authentifié")
        }
        val header = "Bearer $token"
        val res = RetrofitClient.apiService.sendMessage(header, SendMessageRequest(content = content, receiver_id = receiverId, type = type))
        val myId = _currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
        val sentMessage = Message(
            id = res.id.ifBlank { java.util.UUID.randomUUID().toString() },
            senderId = if (res.sender_id.isNotBlank()) res.sender_id else myId,
            receiverId = if (res.receiver_id.isNotBlank()) res.receiver_id else receiverId,
            content = if (res.content.isNotBlank()) res.content else content,
            type = if (res.type.isNotBlank()) res.type else type,
            isRead = res.read,
            createdAt = System.currentTimeMillis()
        )
        messageDao.insertMessage(sentMessage)
        updateConversationLastMessage(
            otherUserId = receiverId,
            content = content,
            type = type,
            isIncoming = false
        )
    }

    // Removed sendAudioMessageMultipart

    suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessageById(id)
    }

    private fun extractCallErrorMessage(e: Throwable, fallback: String): String {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val json = org.json.JSONObject(errorBody)
                    val detail = json.optString("detail", json.optString("message", json.optString("error", "")))
                    if (detail.isNotBlank()) return detail
                }
            } catch (_: Exception) {}

            return when (code) {
                400 -> "Demande d'appel invalide"
                401 -> "Session expirée, veuillez vous reconnecter"
                403 -> "Accès refusé pour cet appel"
                404 -> "Utilisateur ou appel introuvable"
                409 -> "Le correspondant est déjà en ligne ou indisponible"
                500, 502, 503 -> "Erreur serveur temporaire"
                else -> fallback
            }
        }
        return e.message ?: fallback
    }

    suspend fun startCall(receiverId: String): Result<CallStartResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.startCall(header, receiverId)
            Result.success(response)
        } catch (e: Exception) {
            val friendlyMsg = extractCallErrorMessage(e, "Impossible de joindre le correspondant")
            android.util.Log.w("IddetRepository", "Call start notice: $friendlyMsg")
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun acceptCall(callId: String): Result<CallStatusResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.acceptCall(header, callId)
            Result.success(response)
        } catch (e: Exception) {
            val friendlyMsg = extractCallErrorMessage(e, "Impossible d'accepter l'appel")
            android.util.Log.w("IddetRepository", "Call accept notice: $friendlyMsg")
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun declineCall(callId: String): Result<CallStatusResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.declineCall(header, callId)
            Result.success(response)
        } catch (e: Exception) {
            val friendlyMsg = extractCallErrorMessage(e, "Impossible de refuser l'appel")
            android.util.Log.w("IddetRepository", "Call decline notice: $friendlyMsg")
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun endCall(callId: String): Result<CallEndResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.endCall(header, callId)
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("IddetRepository", "Error ending call: ${e.message}", e)
            val friendlyMsg = extractCallErrorMessage(e, "Impossible de terminer l'appel")
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun getCallStatus(callId: String): Result<CallStatusDetailsResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.getCallStatus(header, callId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMissedCalls(): Result<MissedCallsResponse> {
        return try {
            val header = currentToken?.let { "Bearer $it" } ?: return Result.failure(Exception("Non authentifié"))
            val response = RetrofitClient.apiService.getMissedCalls(header)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertMessageLocal(message: Message) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.deleteMessage(header, id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteConversation(otherUserId: String) {
        val myId = _currentUser.value?.id
        if (myId != null) {
            messageDao.deleteConversation(myId, otherUserId)
        }
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.deleteConversation(header, otherUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reactToMessage(messageId: String, emoji: String) {
        // Update local message reaction
        messageDao.updateReaction(messageId, emoji)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.reactToMessage(header, messageId, MessageReactionRequest(emoji))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeMessageReaction(messageId: String) {
        messageDao.updateReaction(messageId, null)
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.removeMessageReaction(header, messageId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getMessageReactions(messageId: String): List<MessageReactionGroup> {
        return try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                RetrofitClient.apiService.getMessageReactions(header, messageId).reactions
            } else {
                val local = messageDao.getMessageById(messageId)
                if (local?.reaction != null) {
                    listOf(MessageReactionGroup(emoji = local.reaction, count = 1, has_reacted = true))
                } else emptyList()
            }
        } catch (e: Exception) {
            val local = messageDao.getMessageById(messageId)
            if (local?.reaction != null) {
                listOf(MessageReactionGroup(emoji = local.reaction, count = 1, has_reacted = true))
            } else emptyList()
        }
    }

    suspend fun deleteMessageLocal(id: String) {
        messageDao.deleteMessage(id)
    }

    suspend fun verifyCurrentUser() {
        val user = _currentUser.value ?: return
        val updatedUser = user.copy(isVerified = true)
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFollowedActfiles(): Flow<List<ActfileWithUser>> {
        return _currentUser.flatMapLatest { me ->
            val myId = me?.id ?: prefs.getString("user_id", "") ?: ""
            if (myId.isEmpty()) {
                flowOf(emptyList())
            } else {
                followDao.getFollowedActfiles(myId)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isFollowing(otherUserId: String): Flow<Boolean> {
        return _currentUser.flatMapLatest { me ->
            val myId = me?.id ?: prefs.getString("user_id", "") ?: ""
            if (myId.isEmpty() || otherUserId.isEmpty()) {
                flowOf(false)
            } else {
                followDao.isFollowingFlow(myId, otherUserId)
            }
        }
    }

    fun getFollowedUsers(userId: String? = null): Flow<List<User>> {
        val targetId = userId ?: _currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
        if (targetId.isEmpty()) return flowOf(emptyList())
        return followDao.getFollowedUsersFlow(targetId)
    }

    fun getFollowerUsers(userId: String? = null): Flow<List<User>> {
        val targetId = userId ?: _currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
        if (targetId.isEmpty()) return flowOf(emptyList())
        return followDao.getFollowerUsersFlow(targetId)
    }

    suspend fun followUser(otherUserId: String) {
        val myId = _currentUser.value?.id ?: prefs.getString("user_id", null) ?: return
        val targetUser = userDao.getUserById(otherUserId) ?: userDao.getUserByUsername(otherUserId)
        val resolvedOtherId = targetUser?.id ?: otherUserId
        if (myId == resolvedOtherId || resolvedOtherId.isEmpty()) return
        
        // 1. Optimistic local follow records
        followDao.insertFollow(Follow(followerId = myId, followingId = resolvedOtherId))
        if (resolvedOtherId != otherUserId && otherUserId.isNotBlank()) {
            followDao.insertFollow(Follow(followerId = myId, followingId = otherUserId))
        }

        // 2. Optimistic local count updates
        val me = userDao.getUserById(myId) ?: _currentUser.value
        if (me != null) {
            val updatedMe = me.copy(followingCount = me.followingCount + 1)
            userDao.insertUser(updatedMe)
            _currentUser.value = updatedMe
        }
        if (targetUser != null) {
            val updatedOther = targetUser.copy(followersCount = targetUser.followersCount + 1)
            userDao.insertUser(updatedOther)
        }

        // 3. Network call: Server endpoint POST /api/users/{id}/follow is an idempotent toggle
        try {
            val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
            if (token != null) {
                val header = if (token.startsWith("Bearer ")) token else "Bearer $token"
                Log.d("IddetRepository", "Following user on server: $resolvedOtherId")
                
                val res = RetrofitClient.apiService.followUser(header, resolvedOtherId)
                Log.d("IddetRepository", "Server follow response: $res")

                if (res.following) {
                    followDao.insertFollow(Follow(followerId = myId, followingId = resolvedOtherId))
                } else {
                    // Server returned following=false (was already followed and toggled off) -> re-enable follow
                    val res2 = RetrofitClient.apiService.followUser(header, resolvedOtherId)
                    if (res2.following) {
                        followDao.insertFollow(Follow(followerId = myId, followingId = resolvedOtherId))
                    }
                }

                // Refresh target profile and current user
                refreshUserProfile(resolvedOtherId)
                refreshProfile()
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Network error in followUser", e)
        }
    }

    suspend fun unfollowUser(otherUserId: String) {
        val myId = _currentUser.value?.id ?: prefs.getString("user_id", null) ?: return
        val targetUser = userDao.getUserById(otherUserId) ?: userDao.getUserByUsername(otherUserId)
        val resolvedOtherId = targetUser?.id ?: otherUserId
        if (myId == resolvedOtherId || resolvedOtherId.isEmpty()) return

        // 1. Optimistic local deletion
        followDao.deleteFollow(myId, resolvedOtherId)
        if (resolvedOtherId != otherUserId && otherUserId.isNotBlank()) {
            followDao.deleteFollow(myId, otherUserId)
        }

        // 2. Optimistic local count updates
        val me = userDao.getUserById(myId) ?: _currentUser.value
        if (me != null) {
            val updatedMe = me.copy(followingCount = (me.followingCount - 1).coerceAtLeast(0))
            userDao.insertUser(updatedMe)
            _currentUser.value = updatedMe
        }
        if (targetUser != null) {
            val updatedOther = targetUser.copy(followersCount = (targetUser.followersCount - 1).coerceAtLeast(0))
            userDao.insertUser(updatedOther)
        }

        // 3. Network call: On this server, unfollow is toggled via POST /api/users/{id}/follow
        try {
            val token = currentToken ?: prefs.getString("auth_token", null) ?: prefs.getString("token", null)
            if (token != null) {
                val header = if (token.startsWith("Bearer ")) token else "Bearer $token"
                Log.d("IddetRepository", "Unfollowing user on server: $resolvedOtherId")
                
                val userNet = try { RetrofitClient.apiService.getUserProfile(header, resolvedOtherId) } catch (_: Exception) { null }
                if (userNet?.is_following != false) {
                    val res = RetrofitClient.apiService.followUser(header, resolvedOtherId)
                    Log.d("IddetRepository", "Unfollow toggle response: $res")
                    if (res.following) {
                        // Toggled into follow instead of unfollow, toggle once more to unfollow
                        RetrofitClient.apiService.followUser(header, resolvedOtherId)
                    }
                }

                refreshUserProfile(resolvedOtherId)
                refreshProfile()
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Network error in unfollowUser", e)
        }
    }



    fun getLikedActfiles(): Flow<List<ActfileWithUser>> {
        return actfileDao.getLikedActfiles()
    }

    fun getCommentedActfiles(userId: String): Flow<List<ActfileWithUser>> {
        return actfileDao.getCommentedActfiles(userId)
    }

    fun getNotifications(): Flow<List<Notification>> {
        val userId = _currentUser.value?.id ?: ""
        repositoryScope.launch {
            refreshNotifications()
        }
        return notificationDao.getNotificationsForUser(userId)
    }

    suspend fun refreshNotifications() {
        try {
            val header = currentToken?.let { "Bearer $it" }
            if (header != null) {
                val res = RetrofitClient.apiService.getNotifications(header)
                val mapped = res.map {
                    Notification(
                        id = it.id,
                        userId = _currentUser.value?.id ?: "",
                        type = it.type,
                        fromUserId = it.from_user_id,
                        fromUsername = it.from_username ?: "Unknown",
                        fromAvatar = it.from_avatar,
                        message = it.message,
                        targetId = it.target_id,
                        isRead = it.read,
                        createdAt = parseIso(it.created_at)
                    )
                }
                notificationDao.insertNotifications(mapped)

                // Real Android system notifications integration
                val context = AppDatabase.appContext
                if (context != null) {
                    val shownNotifications = prefs.getStringSet("shown_notification_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                    var updated = false
                    val newUnread = mapped.filter { !it.isRead && !shownNotifications.contains(it.id) }
                    
                    // Only show up to 5 most recent system notifications to prevent system limit error
                    newUnread.takeLast(5).forEach { notif ->
                        val route = when (notif.type) {
                            "like", "comment" -> "discussion/${notif.targetId}"
                            "message" -> "chat/${notif.fromUserId}"
                            "follow" -> "profile/${notif.fromUserId}"
                            else -> "notifications"
                        }
                        
                        val title = when (notif.type) {
                            "like" -> "💖 Utilité partagée !"
                            "comment" -> "💬 Nouvelle interaction"
                            "follow" -> "🎉 Nouveau membre dans votre réseau"
                            "message" -> "📩 Message privé reçu"
                            else -> "🔔 Notification S-3 CMO"
                        }

                        val descriptiveText = when (notif.type) {
                            "like" -> "${notif.fromUsername} a aimé votre fichier d'acte."
                            "comment" -> "${notif.fromUsername} a commenté : ${notif.message}"
                            "follow" -> "${notif.fromUsername} s'est abonné à votre profil."
                            "message" -> "Nouveau message de ${notif.fromUsername}."
                            else -> notif.message
                        }

                        com.example.utils.NotificationHelper.showSystemNotification(
                            context = context,
                            notificationId = notif.id,
                            title = title,
                            text = descriptiveText,
                            route = route,
                            avatarUrl = notif.fromAvatar,
                            senderName = notif.fromUsername
                        )
                    }

                    newUnread.forEach { notif ->
                        shownNotifications.add(notif.id)
                        updated = true
                    }

                    if (updated) {
                        prefs.edit().putStringSet("shown_notification_ids", shownNotifications).apply()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markNotificationAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        val userId = _currentUser.value?.id ?: return
        notificationDao.markAllAsRead(userId)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun searchCommunities(query: String?, category: String?, sort: String): List<Community> {
        val header = currentToken?.let { "Bearer $it" }
        val netList = try {
            RetrofitClient.apiService.searchCommunities(header, query, category, sort)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        var combined = (netList + localCommunities).distinctBy { it.slug }
        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            combined = combined.filter { 
                it.name.lowercase().contains(q) || 
                it.slug.lowercase().contains(q) || 
                (it.description?.lowercase()?.contains(q) == true) 
            }
        }
        if (category != null && category != "Tous" && category != "Toutes") {
            combined = combined.filter { it.category.equals(category, ignoreCase = true) }
        }
        combined = when (sort) {
            "recent" -> combined.sortedByDescending { it.createdAt }
            "alpha" -> combined.sortedBy { it.name.lowercase() }
            else -> combined.sortedWith(compareByDescending<Community> { it.membersCount }.thenByDescending { it.postsCount })
        }
        return combined
    }

    suspend fun getCommunity(slug: String): Community? {
        val cleanSlug = slug.trim().lowercase().removePrefix("c/").removePrefix("/")
        val local = localCommunities.find { it.slug.equals(cleanSlug, ignoreCase = true) }
        val token = currentToken ?: prefs.getString("auth_token", null)
        val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            val net = RetrofitClient.apiService.getCommunity(header, cleanSlug)
            if (net != null) {
                // Keep local membership state if toggled locally
                val isMem = local?.isMember ?: net.isMember
                val count = local?.membersCount ?: net.membersCount
                val merged = net.copy(isMember = isMem, membersCount = count)
                localCommunities.removeAll { it.slug.equals(cleanSlug, ignoreCase = true) }
                localCommunities.add(merged)
                merged
            } else (local ?: createFallbackCommunity(cleanSlug))
        } catch (e: Exception) {
            e.printStackTrace()
            local ?: createFallbackCommunity(cleanSlug)
        }
    }

    suspend fun joinCommunity(slug: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val local = localCommunities.find { it.slug == slug }
        val target = local ?: try {
            val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            RetrofitClient.apiService.getCommunity(header, slug)
        } catch (_: Exception) { null }

        if (target != null) {
            val updated = target.copy(
                isMember = !target.isMember,
                membersCount = if (target.isMember) (target.membersCount - 1).coerceAtLeast(1) else target.membersCount + 1
            )
            localCommunities.removeAll { it.slug == slug }
            localCommunities.add(updated)
        } else {
            val fallback = Community(
                id = slug,
                slug = slug,
                name = slug.replaceFirstChar { it.uppercase() },
                category = "General",
                description = "Communauté $slug",
                isMember = true,
                membersCount = 1
            )
            localCommunities.add(fallback)
        }

        if (token == null) return true
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            RetrofitClient.apiService.joinCommunity(authHeader, slug)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    suspend fun getCommunityChannels(slug: String): List<Channel> {
        val header = currentToken?.let { "Bearer $it" }
        val netList = try {
            RetrofitClient.apiService.getCommunityChannels(header, slug)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        val localList = localChannels[slug] ?: emptyList()
        val combined = (netList + localList).distinctBy { it.id }
        if (combined.isEmpty()) {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            val defaultChan = Channel(
                id = "chan_gen_$slug",
                communityId = slug,
                slug = "general",
                name = "Général",
                description = "Salon principal de la communauté",
                isDefault = true,
                createdAt = now
            )
            localChannels.getOrPut(slug) { mutableListOf() }.add(defaultChan)
            return listOf(defaultChan)
        }
        return combined
    }

    suspend fun createChannel(slug: String, name: String, description: String?): Channel? {
        val token = currentToken ?: prefs.getString("auth_token", null)
        
        var chanSlug = name.lowercase().trim().replace(Regex("[^a-z0-9_]"), "_").replace(Regex("_+"), "_").trim('_')
        if (chanSlug.isBlank()) chanSlug = "chan_" + (100..999).random()

        if (token != null) {
            try {
                val body = mapOf(
                    "name" to name,
                    "slug" to chanSlug,
                    "description" to (description ?: "")
                )
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val channel = RetrofitClient.apiService.createChannel(authHeader, slug, body)
                return channel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val community = getCommunity(slug)
        val newChannel = Channel(
            id = "local_chan_" + java.util.UUID.randomUUID().toString(),
            communityId = community?.id ?: slug,
            slug = chanSlug,
            name = name,
            description = description,
            isDefault = false,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
        )
        val list = localChannels.getOrPut(slug) { mutableListOf() }
        list.add(newChannel)
        return newChannel
    }

    fun getChannelMessagesFlow(channelId: String): Flow<List<ChannelMessage>> {
        return channelMessageDao.getMessagesForChannel(channelId)
    }

    suspend fun sendChannelMessage(
        channelId: String,
        communitySlug: String,
        content: String,
        type: String = "text"
    ): ChannelMessage? {
        val user = _currentUser.value ?: return null
        val message = ChannelMessage(
            id = "cmsg_" + java.util.UUID.randomUUID().toString(),
            channelId = channelId,
            communitySlug = communitySlug,
            senderId = user.id,
            senderUsername = user.username,
            senderAvatarUrl = user.avatarUrl,
            isVerified = user.isVerified,
            content = content,
            type = type,
            createdAt = System.currentTimeMillis()
        )
        channelMessageDao.insertMessage(message)
        return message
    }

    suspend fun deleteChannelMessage(id: String) {
        channelMessageDao.deleteMessage(id)
    }

    fun getCommunityPostsFlow(slug: String): Flow<List<ActfileWithUser>> = kotlinx.coroutines.flow.flow {
        val localList = try {
            val community = getCommunity(slug)
            actfileDao.getCommunityActfilesList(community?.id ?: slug, slug)
        } catch (e: Exception) {
            emptyList()
        }
        if (localList.isNotEmpty()) {
            emit(localList)
        }

        try {
            val header = currentToken?.let { "Bearer $it" }
            val netActfiles = RetrofitClient.apiService.getCommunityPosts(header, slug)
            val mapped = netActfiles.filter { it.channel_id.isNullOrBlank() }.map { net ->
                val existing = userDao.getUserById(net.user_id)
                val user = if (existing != null) {
                    existing.copy(
                        username = net.username,
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified ?: false
                    )
                } else {
                    User(
                        id = net.user_id,
                        username = net.username,
                        passwordHash = "mocked",
                        avatarUrl = net.avatar_url,
                        isVerified = net.is_verified ?: false
                    )
                }
                userDao.insertUser(user)
                
                ActfileWithUser(
                    id = net.id,
                    userId = net.user_id,
                    username = net.username,
                    avatarUrl = net.avatar_url,
                    isVerified = net.is_verified ?: false,
                    content = net.content,
                    tags = "",
                    likesCount = net.likes_count,
                    viewsCount = net.views_count,
                    commentsCount = net.comments_count ?: 0,
                    createdAt = parseIso(net.created_at),
                    isLikedByMe = net.liked ?: false,
                    category = net.category,
                    communityId = net.community_id,
                    channelId = net.channel_id,
                    channelSlug = net.channel_slug,
                    channelName = net.channel_name,
                    communityName = net.community_name,
                    communityIconUrl = net.community_icon_url,
                    communityIsVerified = net.community_is_verified ?: false,
                    isSponsored = net.is_sponsored ?: false,
                    adCampaignId = net.ad_campaign_id,
                    adStatus = net.ad_status ?: "none",
                    adBudget = net.ad_budget,
                    adCurrency = net.ad_currency
                )
            }
            val combined = (mapped + localList).distinctBy { it.id }.sortedByDescending { it.createdAt }
            emit(combined)
        } catch (e: Exception) {
            e.printStackTrace()
            if (localList.isNotEmpty()) {
                emit(localList)
            } else {
                emit(emptyList())
            }
        }
    }

    suspend fun getMyCommunities(role: String? = null): List<Community> {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val netList = if (token != null) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.apiService.getMyCommunities(authHeader, role)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else emptyList()
        val combined = (localCommunities.filter { it.isMember } + netList).distinctBy { it.slug }
        return if (role != null) combined.filter { it.myRole == role } else combined
    }

    suspend fun deleteCommunity(slug: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null)
        localCommunities.removeAll { it.slug == slug }
        if (token != null) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.apiService.deleteCommunity(authHeader, slug)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    suspend fun updateMemberRole(slug: String, userId: String, role: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            RetrofitClient.apiService.updateMemberRole(authHeader, slug, userId, mapOf("role" to role))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun banCommunityMember(slug: String, userId: String, reason: String? = null): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            RetrofitClient.apiService.banCommunityMember(authHeader, slug, mapOf("user_id" to userId, "reason" to (reason ?: "")))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCommunityBots(slug: String): List<CommunityBot> {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return emptyList()
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = RetrofitClient.apiService.getCommunityBots(authHeader, slug)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: return emptyList()
                parseCommunityBotsList(raw)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createCommunityBot(
        slug: String,
        name: String,
        bannedWords: List<String>,
        autoBanThreshold: Int,
        welcomeMessage: String?
    ): CommunityBot? {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return null
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val body = mutableMapOf<String, Any>(
                "name" to name,
                "banned_words" to bannedWords,
                "auto_ban_threshold" to autoBanThreshold
            )
            if (!welcomeMessage.isNullOrBlank()) {
                body["welcome_message"] = welcomeMessage
            }
            val response = RetrofitClient.apiService.createCommunityBot(authHeader, slug, body)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: return null
                parseSingleCommunityBot(raw)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCommunityBot(
        slug: String,
        botId: String,
        name: String?,
        bannedWords: List<String>?,
        autoBanThreshold: Int?,
        welcomeMessage: String?,
        isActive: Boolean?
    ): CommunityBot? {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return null
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val body = mutableMapOf<String, Any>()
            if (name != null) body["name"] = name
            if (bannedWords != null) body["banned_words"] = bannedWords
            if (autoBanThreshold != null) body["auto_ban_threshold"] = autoBanThreshold
            if (welcomeMessage != null) body["welcome_message"] = welcomeMessage
            if (isActive != null) body["is_active"] = isActive

            val response = RetrofitClient.apiService.updateCommunityBot(authHeader, slug, botId, body)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: return null
                parseSingleCommunityBot(raw)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteCommunityBot(slug: String, botId: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = RetrofitClient.apiService.deleteCommunityBot(authHeader, slug, botId)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performBotModAction(
        slug: String,
        botId: String,
        botToken: String?,
        actionType: String,
        targetUserId: String?,
        targetActfileId: String?,
        reason: String?
    ): Boolean {
        return try {
            val body = mutableMapOf<String, Any>("action_type" to actionType)
            if (targetUserId != null) body["target_user_id"] = targetUserId
            if (targetActfileId != null) body["target_actfile_id"] = targetActfileId
            if (reason != null) body["reason"] = reason

            val response = RetrofitClient.apiService.performBotModAction(botToken, slug, botId, body)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCommunityModActions(slug: String, limit: Int = 50): List<CommunityModAction> {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return emptyList()
        return try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = RetrofitClient.apiService.getCommunityModActions(authHeader, slug, limit)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: return emptyList()
                parseCommunityModActionsList(raw)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseCommunityBotsList(raw: String): List<CommunityBot> {
        val list = mutableListOf<CommunityBot>()
        try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) {
                val arr = org.json.JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    list.add(parseCommunityBotJsonObj(arr.getJSONObject(i)))
                }
            } else if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                val arr = obj.optJSONArray("bots") ?: obj.optJSONArray("items")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(parseCommunityBotJsonObj(arr.getJSONObject(i)))
                    }
                } else if (obj.has("id")) {
                    list.add(parseCommunityBotJsonObj(obj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSingleCommunityBot(raw: String): CommunityBot? {
        return try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                parseCommunityBotJsonObj(org.json.JSONObject(trimmed))
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseCommunityBotJsonObj(obj: org.json.JSONObject): CommunityBot {
        val words = mutableListOf<String>()
        val bw = obj.optJSONArray("banned_words")
        if (bw != null) {
            for (i in 0 until bw.length()) {
                words.add(bw.optString(i))
            }
        }
        return CommunityBot(
            id = obj.optString("id", ""),
            communityId = if (obj.has("community_id")) obj.optString("community_id") else null,
            name = obj.optString("name", "Bot"),
            bannedWords = words,
            autoBanThreshold = obj.optInt("auto_ban_threshold", 0),
            welcomeMessage = if (obj.has("welcome_message") && !obj.isNull("welcome_message")) obj.optString("welcome_message") else null,
            isActive = obj.optBoolean("is_active", true),
            createdAt = if (obj.has("created_at")) obj.optString("created_at") else null,
            token = if (obj.has("token") && !obj.isNull("token")) obj.optString("token") else null
        )
    }

    private fun parseCommunityModActionsList(raw: String): List<CommunityModAction> {
        val list = mutableListOf<CommunityModAction>()
        try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) {
                val arr = org.json.JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    list.add(parseCommunityModActionJsonObj(arr.getJSONObject(i)))
                }
            } else if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                val arr = obj.optJSONArray("actions") ?: obj.optJSONArray("items") ?: obj.optJSONArray("mod_actions")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(parseCommunityModActionJsonObj(arr.getJSONObject(i)))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseCommunityModActionJsonObj(obj: org.json.JSONObject): CommunityModAction {
        return CommunityModAction(
            id = obj.optString("id", ""),
            communityId = if (obj.has("community_id")) obj.optString("community_id") else null,
            actionType = obj.optString("action_type", ""),
            actorType = if (obj.has("actor_type")) obj.optString("actor_type") else null,
            actorId = if (obj.has("actor_id")) obj.optString("actor_id") else null,
            targetUserId = if (obj.has("target_user_id") && !obj.isNull("target_user_id")) obj.optString("target_user_id") else null,
            targetActfileId = if (obj.has("target_actfile_id") && !obj.isNull("target_actfile_id")) obj.optString("target_actfile_id") else null,
            reason = if (obj.has("reason") && !obj.isNull("reason")) obj.optString("reason") else null,
            createdAt = if (obj.has("created_at")) obj.optString("created_at") else null
        )
    }

    private fun parseCommunityJson(
        jsonStr: String,
        fallbackSlug: String,
        fallbackName: String,
        fallbackCategory: String,
        fallbackDescription: String,
        fallbackIsPrivate: Boolean
    ): Community {
        return try {
            val root = org.json.JSONObject(jsonStr)
            val obj = if (root.has("community") && !root.isNull("community")) {
                root.getJSONObject("community")
            } else if (root.has("data") && !root.isNull("data")) {
                root.getJSONObject("data")
            } else {
                root
            }

            val id = obj.optString("id", java.util.UUID.randomUUID().toString())
            val slug = obj.optString("slug", fallbackSlug)
            val name = obj.optString("name", fallbackName)
            val description = if (obj.has("description") && !obj.isNull("description")) obj.optString("description") else fallbackDescription
            val iconUrl = if (obj.has("icon_url") && !obj.isNull("icon_url")) com.example.utils.UrlHelper.fixCloudinaryUrl(obj.optString("icon_url")) else null
            val bannerUrl = if (obj.has("banner_url") && !obj.isNull("banner_url")) com.example.utils.UrlHelper.fixCloudinaryUrl(obj.optString("banner_url")) else null
            val category = obj.optString("category", fallbackCategory)
            val creatorId = if (obj.has("creator_id") && !obj.isNull("creator_id")) obj.optString("creator_id") else _currentUser.value?.id
            val isPrivate = if (obj.has("is_private")) obj.optBoolean("is_private", fallbackIsPrivate) else obj.optBoolean("isPrivate", fallbackIsPrivate)
            val membersCount = obj.optInt("members_count", 1)
            val postsCount = obj.optInt("posts_count", 0)
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            val createdAt = obj.optString("created_at", now)
            val isMember = if (obj.has("is_member")) obj.optBoolean("is_member", true) else true
            val myRole = if (obj.has("my_role") && !obj.isNull("my_role")) obj.optString("my_role") else "admin"

            Community(
                id = id,
                slug = slug,
                name = name,
                description = description,
                iconUrl = iconUrl,
                bannerUrl = bannerUrl,
                category = category,
                creatorId = creatorId,
                isPrivate = isPrivate,
                membersCount = membersCount,
                postsCount = postsCount,
                createdAt = createdAt,
                isMember = isMember,
                myRole = myRole
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            Community(
                id = java.util.UUID.randomUUID().toString(),
                slug = fallbackSlug,
                name = fallbackName,
                description = fallbackDescription,
                category = fallbackCategory,
                isPrivate = fallbackIsPrivate,
                membersCount = 1,
                postsCount = 0,
                createdAt = now,
                isMember = true,
                myRole = "admin"
            )
        }
    }

    private fun createDefaultChannelForCommunity(communitySlug: String, communityId: String) {
        val list = localChannels.getOrPut(communitySlug) { mutableListOf() }
        if (list.isEmpty()) {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            list.add(
                Channel(
                    id = "chan_gen_" + java.util.UUID.randomUUID().toString(),
                    communityId = communityId,
                    slug = "general",
                    name = "Général",
                    description = "Salon principal de la communauté",
                    isDefault = true,
                    createdAt = now
                )
            )
        }
    }

    suspend fun createCommunity(name: String, category: String, description: String, isPrivate: Boolean): Community? {
        val token = currentToken ?: prefs.getString("auth_token", null)
        
        // Generate a robust, server-compliant slug
        var slug = name.lowercase().trim()
        val replacements = mapOf(
            'à' to 'a', 'â' to 'a', 'ä' to 'a',
            'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
            'î' to 'i', 'ï' to 'i',
            'ô' to 'o', 'ö' to 'o',
            'û' to 'u', 'ü' to 'u',
            'ç' to 'c', ' ' to '_'
        )
        val sb = java.lang.StringBuilder()
        for (char in slug) {
            sb.append(replacements[char] ?: char)
        }
        slug = sb.toString()
        slug = slug.replace(Regex("[^a-z0-9_]"), "_")
        slug = slug.replace(Regex("_+"), "_")
        slug = slug.trim('_')
        
        if (slug.length < 3) {
            val randomSuffix = (100..999).random()
            slug = "com_${slug}_$randomSuffix".replace(Regex("_+"), "_").trim('_')
            if (slug.length < 3) {
                slug = "com_$randomSuffix"
            }
        }
        if (slug.length > 50) {
            slug = slug.substring(0, 50).trim('_')
        }

        var createdCommunity: Community? = null

        if (token != null) {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
            try {
                val body = mutableMapOf<String, Any>(
                    "slug" to slug,
                    "name" to name.trim(),
                    "category" to category,
                    "description" to description.trim(),
                    "is_private" to isPrivate,
                    "isPrivate" to isPrivate
                )
                val response = RetrofitClient.apiService.createCommunity(authHeader, body)
                if (response.isSuccessful) {
                    val rawJson = response.body()?.string()
                    if (!rawJson.isNullOrBlank()) {
                        createdCommunity = parseCommunityJson(rawJson, slug, name, category, description, isPrivate)
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    android.util.Log.e("IddetRepository", "createCommunity error ${response.code()}: $errBody")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Guaranteed fallback creation so creation never fails and the user is never stuck
        if (createdCommunity == null) {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            createdCommunity = Community(
                id = "com_" + java.util.UUID.randomUUID().toString(),
                slug = slug,
                name = name.trim(),
                description = description.trim(),
                iconUrl = null,
                bannerUrl = null,
                category = category,
                creatorId = _currentUser.value?.id,
                isPrivate = isPrivate,
                membersCount = 1,
                postsCount = 0,
                createdAt = now,
                isMember = true,
                myRole = "admin"
            )
        }

        // Add to local cached communities list
        localCommunities.removeAll { it.slug == createdCommunity!!.slug }
        localCommunities.add(0, createdCommunity)

        // Ensure default channel exists for immediate chat & posting
        createDefaultChannelForCommunity(createdCommunity.slug, createdCommunity.id)

        return createdCommunity
    }

    suspend fun updateCommunity(
        slug: String,
        name: String?,
        description: String?,
        category: String?,
        isPrivate: Boolean?,
        iconUrl: String? = null
    ): Community? {
        val token = currentToken ?: return null
        return try {
            val body = mutableMapOf<String, Any>()
            if (name != null) body["name"] = name
            if (description != null) body["description"] = description
            if (category != null) body["category"] = category
            if (isPrivate != null) body["is_private"] = isPrivate
            if (iconUrl != null) {
                body["icon_url"] = iconUrl
                body["iconUrl"] = iconUrl
            }

            RetrofitClient.apiService.updateCommunity("Bearer $token", slug, body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun uploadCommunityIconImage(iconFile: java.io.File): String? = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestFile = iconFile.asRequestBody(mediaType)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", iconFile.name, requestFile)
                .build()

            val request = Request.Builder()
                .url("https://tmpfiles.org/api/v1/upload")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (response.isSuccessful && resStr.contains("\"url\"")) {
                val json = JSONObject(resStr)
                val data = json.optJSONObject("data")
                val rawUrl = data?.optString("url")
                if (!rawUrl.isNullOrBlank()) {
                    val directUrl = rawUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/")
                    Log.d("IddetRepository", "Community icon uploaded successfully: $directUrl")
                    return@withContext directUrl
                }
            }
            // Fallback: encode as Base64 data URI
            val bytes = iconFile.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e("IddetRepository", "Failed to upload image file to tmpfiles, using fallback", e)
            try {
                val bytes = iconFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            } catch (err: Exception) {
                null
            }
        }
    }

    suspend fun updateCommunityIcon(slug: String, iconFile: java.io.File): Boolean {
        val token = currentToken ?: return false
        return try {
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestFile = iconFile.asRequestBody(mediaType)
            val iconPart = MultipartBody.Part.createFormData("icon", iconFile.name, requestFile)
            val slugPart = slug.toRequestBody("text/plain".toMediaTypeOrNull())

            // 1. PUT /api/communities/{slug}/icon
            try {
                val resp = RetrofitClient.apiService.updateCommunityIcon("Bearer $token", slug, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "PUT /api/communities/$slug/icon failed: ${e.message}")
            }

            // 2. POST /api/communities/{slug}/icon
            try {
                val resp = RetrofitClient.apiService.updateCommunityIconPost("Bearer $token", slug, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "POST /api/communities/$slug/icon failed: ${e.message}")
            }

            // 3. PUT /api/community/{slug}/icon
            try {
                val resp = RetrofitClient.apiService.updateCommunitySlugIconPut("Bearer $token", slug, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "PUT /api/community/$slug/icon failed: ${e.message}")
            }

            // 4. POST /api/community/{slug}/icon
            try {
                val resp = RetrofitClient.apiService.updateCommunitySlugIconPost("Bearer $token", slug, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "POST /api/community/$slug/icon failed: ${e.message}")
            }

            // 5. POST /api/community/icon (with slug part)
            try {
                val resp = RetrofitClient.apiService.uploadCommunityIconDirectPost("Bearer $token", slugPart, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "POST /api/community/icon failed: ${e.message}")
            }

            // 6. PUT /api/community/icon (with slug part)
            try {
                val resp = RetrofitClient.apiService.uploadCommunityIconDirectPut("Bearer $token", slugPart, iconPart)
                val url = extractCommunityIconUrl(resp)
                if (!url.isNullOrBlank()) return applyIconSuccess(slug, url)
            } catch (e: Exception) {
                Log.w("IddetRepository", "PUT /api/community/icon failed: ${e.message}")
            }

            // 7. Fallback: upload image and update via MCP / REST
            val iconUrl = uploadCommunityIconImage(iconFile) ?: return false
            updateCommunityIconUrl(slug, iconUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractCommunityIconUrl(resp: Map<String, Any>?): String? {
        if (resp == null) return null
        return (resp["icon_url"] ?: resp["iconUrl"] ?: resp["icon"] ?: resp["url"])?.toString()
    }

    private fun applyIconSuccess(slug: String, serverUrl: String): Boolean {
        val index = localCommunities.indexOfFirst { it.slug == slug }
        if (index != -1) {
            val old = localCommunities[index]
            localCommunities[index] = old.copy(iconUrl = serverUrl)
        }
        Log.i("IddetRepository", "Community icon updated directly on server: $serverUrl")
        return true
    }

    suspend fun getConnectedApps(): List<ConnectedAppItem> {
        val token = currentToken ?: return emptyList()
        return try {
            RetrofitClient.apiService.getConnectedApps("Bearer $token")
        } catch (e: Exception) {
            Log.w("IddetRepository", "Failed to get connected apps: ${e.message}")
            emptyList()
        }
    }

    suspend fun removeConnectedApp(appId: String): Boolean {
        val token = currentToken ?: return false
        return try {
            RetrofitClient.apiService.removeConnectedApp("Bearer $token", appId)
            true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Failed to remove connected app: ${e.message}")
            false
        }
    }

    suspend fun updateCommunityIconUrl(slug: String, iconUrl: String): Boolean {
        val token = currentToken ?: return false
        return try {
            val existingCommunity = localCommunities.find { it.slug == slug }
            val communityId = existingCommunity?.id ?: slug

            // 1. Update via FastMCP server mcp_update_community tool
            try {
                McpSessionManager.callTool(
                    toolName = "mcp_update_community",
                    arguments = mapOf(
                        "auth_token" to token,
                        "community_id" to communityId,
                        "icon_url" to iconUrl
                    ),
                    authToken = token
                )
            } catch (e: Exception) {
                Log.w("IddetRepository", "MCP tool mcp_update_community note: ${e.message}")
            }

            // 2. Update via REST PUT /api/communities/{slug}
            try {
                RetrofitClient.apiService.updateCommunity(
                    "Bearer $token",
                    slug,
                    mapOf(
                        "icon_url" to iconUrl,
                        "iconUrl" to iconUrl,
                        "icon" to iconUrl
                    )
                )
            } catch (e: Exception) {
                Log.w("IddetRepository", "REST updateCommunity note: ${e.message}")
            }

            // 3. Update local cache immediately
            val index = localCommunities.indexOfFirst { it.slug == slug }
            if (index != -1) {
                val old = localCommunities[index]
                localCommunities[index] = old.copy(iconUrl = iconUrl)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getStories(): List<Story> {
        val token = currentToken
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            val responses = RetrofitClient.apiService.getStories(authHeader)
            responses.map { res ->
                val realViews = res.views ?: res.view_count ?: 0
                val realReactions = res.reactions ?: emptyMap()
                Story(
                    id = res.id,
                    mediaUrl = com.example.utils.UrlHelper.fixCloudinaryUrl(res.media_url) ?: res.media_url,
                    mediaType = res.media_type,
                    effect = res.effect,
                    createdAt = res.created_at,
                    user = StoryUser(
                        id = res.user.id,
                        username = res.user.username,
                        avatarUrl = com.example.utils.UrlHelper.fixCloudinaryUrl(res.user.avatar_url),
                        isVerified = res.user.is_verified ?: false
                    ),
                    views = realViews,
                    reactions = realReactions
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createStory(file: java.io.File, mimeType: String, effect: String?): Boolean {
        val token = currentToken
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            val mediaType = mimeType.toMediaType()
            val requestFile = file.asRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val effectBody = if (!effect.isNullOrBlank() && effect != "none" && effect != "aucun") {
                effect.toRequestBody("text/plain".toMediaType())
            } else null
            val response = RetrofitClient.apiService.createStory(authHeader, filePart, effectBody)
            response.status == "success" || response.story_id != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteStory(id: String): Boolean {
        val token = currentToken
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            RetrofitClient.apiService.deleteStory(authHeader, id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            true // Allow local deletion fallback
        }
    }

    suspend fun getMyLevel(): UserLevelResponse? {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            RetrofitClient.apiService.getMyLevel(authHeader)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback calculation based on current user stats
            val user = _currentUser.value
            val xp = user?.xp ?: 0
            calculateFallbackLevel(user?.id ?: "", xp)
        }
    }

    suspend fun getUserLevel(userId: String): UserLevelResponse? {
        return try {
            RetrofitClient.apiService.getUserLevel(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            val user = userDao.getUserById(userId)
            val xp = user?.xp ?: 0
            calculateFallbackLevel(userId, xp)
        }
    }

    suspend fun getLevelsTable(): List<LevelTableItem> {
        return try {
            val res = RetrofitClient.apiService.getLevelsTable()
            if (res.levels.isNotEmpty()) {
                res.levels
            } else {
                fallbackLevelsTable()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackLevelsTable()
        }
    }

    private fun fallbackLevelsTable(): List<LevelTableItem> = listOf(
        LevelTableItem("Débutant", 0),
        LevelTableItem("Bronze", 100),
        LevelTableItem("Argent", 500),
        LevelTableItem("Or", 2000),
        LevelTableItem("Platine", 10000),
        LevelTableItem("Diamant", 50000),
        LevelTableItem("Légende", 200000)
    )

    private fun calculateFallbackLevel(userId: String, score: Int): UserLevelResponse {
        val tiers = listOf(
            "Débutant" to 0,
            "Bronze" to 100,
            "Argent" to 500,
            "Or" to 2000,
            "Platine" to 10000,
            "Diamant" to 50000,
            "Légende" to 200000
        )
        var currentIndex = 0
        for (i in tiers.indices) {
            if (score >= tiers[i].second) {
                currentIndex = i
            } else {
                break
            }
        }
        val currentName = tiers[currentIndex].first
        val currentThreshold = tiers[currentIndex].second
        val isMax = currentIndex == tiers.size - 1
        val nextName = if (!isMax) tiers[currentIndex + 1].first else null
        val nextThreshold = if (!isMax) tiers[currentIndex + 1].second else null
        val span = if (nextThreshold != null) nextThreshold - currentThreshold else 1
        val progress = if (isMax) 1.0f else ((score - currentThreshold).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val pointsToNext = if (nextThreshold != null) (nextThreshold - score).coerceAtLeast(0) else 0

        return UserLevelResponse(
            user_id = userId,
            score = score,
            level_index = currentIndex,
            level_name = currentName,
            next_level_name = nextName,
            next_level_score = nextThreshold,
            points_to_next = pointsToNext,
            progress = progress,
            is_max_level = isMax
        )
    }

    private fun createFallbackCommunity(slug: String): Community {
        val cleanSlug = slug.trim().lowercase().removePrefix("c/").removePrefix("/")
        val fallback = Community(
            id = "com_$cleanSlug",
            slug = cleanSlug,
            name = cleanSlug.replaceFirstChar { it.uppercase() },
            category = "Général",
            description = "Bienvenue dans la communauté c/$cleanSlug sur IDDET. Partagez, échangez et suivez les salons thématiques.",
            membersCount = 0,
            isMember = false
        )
        localCommunities.removeAll { it.slug.equals(cleanSlug, ignoreCase = true) }
        localCommunities.add(fallback)
        return fallback
    }

    // ── MUSIQUE & STRIP SOUNDS API INTEGRATION ──
    suspend fun getRecommendedSounds(): List<SoundNetwork> {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            RetrofitClient.apiService.getRecommendedSounds(authHeader, limit = 50)
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error fetching recommended sounds from API", e)
            emptyList()
        }
    }

    suspend fun getSoundsByCategory(category: String): List<SoundNetwork> {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            if (category.equals("Tout", ignoreCase = true) || category.isBlank()) {
                getRecommendedSounds()
            } else {
                RetrofitClient.apiService.getSoundsByCategory(authHeader, category, limit = 50)
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error fetching sounds by category from API", e)
            emptyList()
        }
    }

    suspend fun getSoundDetails(soundId: String): SoundNetwork? {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            val res = RetrofitClient.apiService.getSoundDetails(authHeader, soundId)
            res.sound
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error fetching sound details for $soundId", e)
            null
        }
    }

    suspend fun uploadSound(
        audioFile: java.io.File,
        coverFile: java.io.File?,
        title: String,
        description: String,
        category: String
    ): Result<String> {
        val token = currentToken ?: prefs.getString("auth_token", null)
            ?: return Result.failure(IllegalStateException("Authentification requise pour publier un son"))
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        return try {
            val audioMediaType = "audio/*".toMediaTypeOrNull()
            val audioReq = audioFile.asRequestBody(audioMediaType)
            val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.name, audioReq)

            val coverPart = coverFile?.let {
                val coverMediaType = "image/*".toMediaTypeOrNull()
                val coverReq = it.asRequestBody(coverMediaType)
                MultipartBody.Part.createFormData("cover_file", it.name, coverReq)
            }

            val titleReq = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descReq = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val catReq = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = RetrofitClient.apiService.uploadSound(
                token = authHeader,
                audio_file = audioPart,
                cover_file = coverPart,
                title = titleReq,
                description = descReq,
                category = catReq
            )
            Result.success(res.sound_id)
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error uploading sound to server", e)
            Result.failure(e)
        }
    }

    suspend fun likeSound(soundId: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return try {
            val res = RetrofitClient.apiService.likeSound(authHeader, soundId)
            res["liked"] == true || res["success"] == true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error liking sound on API", e)
            false
        }
    }

    suspend fun recordSoundPlay(soundId: String): Boolean {
        return try {
            RetrofitClient.apiService.recordSoundPlay(soundId)
            true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error recording sound play", e)
            false
        }
    }

    suspend fun commentSound(soundId: String, content: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return try {
            RetrofitClient.apiService.commentSound(authHeader, soundId, mapOf("content" to content))
            true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error commenting sound", e)
            false
        }
    }

    // ── VIDEO FEED (REELS) API INTEGRATION DIRECTE DU SERVEUR ──
    suspend fun getVideoFeed(cursor: String? = null, limit: Int = 20): List<VideoFeedItemNetwork> {
        val token = currentToken ?: prefs.getString("auth_token", null)
        val authHeader = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        return try {
            val feed = RetrofitClient.apiService.getVideoFeed(authHeader, cursor = cursor, limit = limit)
            if (feed.isNotEmpty()) {
                feed
            } else {
                // Fallback to random videos on server
                RetrofitClient.apiService.getRandomVideos(authHeader, limit = limit)
            }
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error fetching video feed from server", e)
            try {
                RetrofitClient.apiService.getRandomVideos(authHeader, limit = limit)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun likeVideo(videoId: String): Boolean {
        val token = currentToken ?: prefs.getString("auth_token", null) ?: return false
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return try {
            val res = RetrofitClient.apiService.likeVideo(authHeader, videoId)
            res["liked"] == true || res["success"] == true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error liking video on API", e)
            false
        }
    }

    suspend fun recordVideoView(videoId: String): Boolean {
        return try {
            RetrofitClient.apiService.recordVideoView(videoId)
            true
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error recording video view", e)
            false
        }
    }

    suspend fun uploadVideo(
        videoFile: java.io.File,
        description: String,
        isPublic: Boolean = true,
        hasOriginalSound: Boolean = false,
        soundId: String? = null
    ): Result<VideoUploadResponse> {
        val token = currentToken ?: prefs.getString("auth_token", null)
            ?: return Result.failure(IllegalStateException("Authentification requise pour publier une vidéo"))
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        return try {
            val videoMediaType = "video/*".toMediaTypeOrNull()
            val videoReq = videoFile.asRequestBody(videoMediaType)
            val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, videoReq)

            val descReq = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val isPublicReq = isPublic.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val hasSoundReq = hasOriginalSound.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val soundIdReq = soundId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = RetrofitClient.apiService.uploadVideo(
                token = authHeader,
                video = videoPart,
                description = descReq,
                isPublic = isPublicReq,
                hasOriginalSound = hasSoundReq,
                soundId = soundIdReq
            )
            Result.success(res)
        } catch (e: Exception) {
            Log.e("IddetRepository", "Error uploading video to server", e)
            Result.failure(e)
        }
    }

    suspend fun promoteActfile(
        actfileId: String,
        budget: Double,
        currency: String = "USD",
        daily: Boolean = true,
        targetCountry: String? = null
    ): Result<AdCampaignResponse> = withContext(Dispatchers.IO) {
        try {
            val token = currentToken ?: prefs.getString("auth_token", null)
            val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            val request = AdCampaignRequest(
                budget_amount = budget,
                currency = currency,
                daily = daily,
                target_country = targetCountry
            )
            val resp = RetrofitClient.apiService.createActfileAd(header, actfileId, request)
            
            val existing = actfileDao.getActfileRaw(actfileId)
            if (existing != null) {
                val status = resp.status ?: "active"
                actfileDao.insertActfile(
                    existing.copy(
                        isSponsored = status == "active" || existing.isSponsored,
                        adCampaignId = resp.campaign_id ?: existing.adCampaignId,
                        adStatus = status,
                        adBudget = budget,
                        adCurrency = currency
                    )
                )
            }
            Result.success(resp)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getActfileAdStatus(actfileId: String): Result<AdStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val token = currentToken ?: prefs.getString("auth_token", null)
            val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            val resp = RetrofitClient.apiService.getActfileAdStatus(header, actfileId)
            
            val existing = actfileDao.getActfileRaw(actfileId)
            if (existing != null) {
                actfileDao.insertActfile(
                    existing.copy(
                        isSponsored = resp.is_sponsored,
                        adCampaignId = resp.campaign_id ?: existing.adCampaignId,
                        adStatus = resp.ad_status ?: existing.adStatus
                    )
                )
            }
            Result.success(resp)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun cancelActfileAd(actfileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = currentToken ?: prefs.getString("auth_token", null)
            val header = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
            RetrofitClient.apiService.cancelActfileAd(header, actfileId)
            
            val existing = actfileDao.getActfileRaw(actfileId)
            if (existing != null) {
                actfileDao.insertActfile(
                    existing.copy(
                        isSponsored = false,
                        adStatus = "cancelled"
                    )
                )
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: IddetRepository? = null

        fun getInstance(context: android.content.Context): IddetRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = com.example.data.AppDatabase.getDatabase(context.applicationContext)
                    val prefs = context.applicationContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    val instance = IddetRepository(
                        db.userDao(),
                        db.actfileDao(),
                        db.messageDao(),
                        db.followDao(),
                        db.commentDao(),
                        db.notificationDao(),
                        db.savedAccountDao(),
                        db.channelMessageDao(),
                        prefs
                    )
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
