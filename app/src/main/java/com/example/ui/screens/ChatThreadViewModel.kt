package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.IddetRepository
import com.example.data.Message
import com.example.data.RetrofitClient
import com.example.ui.components.ChatMessageUiModel
import com.example.ui.components.ReactionUiGroup
import com.example.utils.ChatSocketClient
import com.example.utils.SocketConnectionState
import com.example.utils.WebSocketEvent
import com.example.utils.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

data class ChatPartnerUiModel(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isVerified: Boolean = false,
    val isIddetPlus: Boolean = false,
    val cardStyle: String? = null,
    val lastSeen: String? = null
)

sealed interface ChatThreadUiState {
    object Loading : ChatThreadUiState
    data class Success(
        val messages: List<ChatMessageUiModel>,
        val partner: ChatPartnerUiModel,
        val connectionState: SocketConnectionState = SocketConnectionState.CONNECTED
    ) : ChatThreadUiState
    object Empty : ChatThreadUiState
    data class Error(val message: String) : ChatThreadUiState
}

class ChatThreadViewModel(
    val partnerUserId: String,
    val repository: IddetRepository,
    private val socketClient: ChatSocketClient = WebSocketManager
) : ViewModel() {

    private val _partnerInfo = MutableStateFlow(
        ChatPartnerUiModel(
            id = partnerUserId,
            username = "Chargement…",
            avatarUrl = null,
            isOnline = false
        )
    )
    val partnerInfo: StateFlow<ChatPartnerUiModel> = _partnerInfo.asStateFlow()

    private val _optimisticMessages = MutableStateFlow<List<ChatMessageUiModel>>(emptyList())
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private var partnerTypingTimeoutJob: Job? = null
    private var myTypingTimeoutJob: Job? = null
    private var lastTypingSentTime = 0L

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _disappearingTimer = MutableStateFlow(0L)
    val disappearingTimer: StateFlow<Long> = _disappearingTimer.asStateFlow()

    fun setDisappearingTimer(durationMs: Long) {
        _disappearingTimer.value = durationMs
    }

    fun addReactionToMessage(messageId: String, emoji: String) {
        val updated = _directServerMessages.value.map { msg ->
            if (msg.id == messageId) {
                val currentReactions = msg.reactions.toMutableList()
                val existing = currentReactions.find { it.emoji == emoji }
                if (existing != null) {
                    currentReactions.remove(existing)
                    currentReactions.add(existing.copy(count = existing.count + 1, hasReacted = true))
                } else {
                    currentReactions.add(ReactionUiGroup(emoji = emoji, count = 1, hasReacted = true))
                }
                msg.copy(reactions = currentReactions)
            } else {
                msg
            }
        }
        _directServerMessages.value = updated
    }

    val socketConnectionState: StateFlow<SocketConnectionState> = socketClient.connectionState

    private val _directServerMessages = MutableStateFlow<List<ChatMessageUiModel>>(emptyList())
    private val _isLoadingMessages = MutableStateFlow(true)
    private var resolvedPartnerId: String = partnerUserId

    // Live messages from repository Flow as offline fallback
    private val roomMessagesFlow = repository.getMessagesWith(partnerUserId)

    private val _flow1 = combine(_directServerMessages, roomMessagesFlow, _optimisticMessages) { s, r, p -> Triple(s, r, p) }
    private val _flow2 = combine(_partnerInfo, _isLoadingMessages, socketConnectionState) { p, l, c -> Triple(p, l, c) }

    val uiState: StateFlow<ChatThreadUiState> = combine(_flow1, _flow2) { (serverList, roomList, pendingList), (partner, isLoading, connState) ->
        val currentUserId = repository.currentUser.value?.id ?: ""
        val currentUsername = repository.currentUser.value?.username ?: ""

        // Merge messages: Combine local Room cache with fresh server messages by ID
        val messageMap = LinkedHashMap<String, ChatMessageUiModel>()

        // 1. Immediate offline/cached Room messages
        roomList.filterNot { it.id.startsWith("conv_") }.forEach { m ->
            val isMine = (currentUserId.isNotBlank() && m.senderId == currentUserId) ||
                    (m.senderId != partnerUserId && m.senderId != resolvedPartnerId)
            val reactions = parseReactions(m.reaction)
            messageMap[m.id] = ChatMessageUiModel(
                id = m.id,
                senderId = m.senderId,
                receiverId = m.receiverId,
                content = m.content,
                type = m.type,
                isMine = isMine,
                isRead = m.isRead,
                createdAt = m.createdAt,
                reactions = reactions,
                isSending = false,
                isFailed = false
            )
        }

        // 2. Overwrite and enrich with direct server messages
        serverList.forEach { s ->
            messageMap[s.id] = s
        }

        val combined = messageMap.values.toMutableList()

        // Add pending/optimistic messages if not already present
        pendingList.forEach { pending ->
            if (combined.none { it.id == pending.id || (it.content == pending.content && it.isMine && Math.abs(it.createdAt - pending.createdAt) < 5000) }) {
                combined.add(pending)
            }
        }

        // Sort by timestamp ASC
        combined.sortBy { it.createdAt }

        // Compute grouping, avatars, and last in group
        val grouped = groupMessages(combined)

        if (isLoading && grouped.isEmpty()) {
            ChatThreadUiState.Loading
        } else if (grouped.isEmpty()) {
            ChatThreadUiState.Empty
        } else {
            ChatThreadUiState.Success(
                messages = grouped,
                partner = partner,
                connectionState = connState
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatThreadUiState.Loading
    )

    init {
        val cachedConv = repository.conversations.value.find { it.user_id == partnerUserId || it.id == partnerUserId }
        if (cachedConv != null) {
            _partnerInfo.value = _partnerInfo.value.copy(
                username = cachedConv.username,
                avatarUrl = cachedConv.avatar_url,
                isOnline = cachedConv.is_online,
                isVerified = cachedConv.is_verified
            )
        }
        loadPartnerDetails()
        loadServerMessages()
        observeWebSocketEvents()
        connectSocketIfLoggedIn()
        markMessagesRead()
        startPeriodicSync()

        // Safety timeout: Never stay stuck in loading spinner if server has no messages
        viewModelScope.launch {
            delay(1200)
            _isLoadingMessages.value = false
        }
    }

    private fun loadServerMessages() {
        viewModelScope.launch {
            try {
                val remote = repository.getDirectMessages(partnerUserId)
                if (remote.isNotEmpty()) {
                    updateServerMessages(remote)
                }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to fetch direct messages: ${e.message}")
            } finally {
                _isLoadingMessages.value = false
            }
        }
    }

    private fun updateServerMessages(remote: List<com.example.data.MessageNetwork>) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val currentUsername = repository.currentUser.value?.username ?: ""

        // Resolve real partner ID if routed by username
        remote.forEach { m ->
            if (m.sender_id.isNotBlank() && m.sender_id != currentUserId) {
                resolvedPartnerId = m.sender_id
            } else if (m.receiver_id.isNotBlank() && m.receiver_id != currentUserId) {
                resolvedPartnerId = m.receiver_id
            }
        }

        val mapped = remote.map { net ->
            val isMine = (currentUserId.isNotBlank() && net.sender_id == currentUserId) ||
                    (currentUsername.isNotBlank() && !net.sender_username.isNullOrBlank() && net.sender_username.equals(currentUsername, ignoreCase = true)) ||
                    (net.sender_id != partnerUserId && net.sender_id != resolvedPartnerId && !net.sender_username.equals(_partnerInfo.value.username, ignoreCase = true) && !net.sender_username.equals(partnerUserId, ignoreCase = true))

            val parsedTime = try {
                val cleanDate = net.created_at.replace("Z", "+0000")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", java.util.Locale.US)
                format.parse(cleanDate)?.time ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).parse(cleanDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val reactions = parseReactions(net.reaction)

            ChatMessageUiModel(
                id = net.id.ifBlank { UUID.randomUUID().toString() },
                senderId = net.sender_id,
                receiverId = net.receiver_id,
                content = net.content,
                type = if (com.example.utils.AudioMessageHelper.isAudioContent(net.content, net.type)) "audio" else net.type,
                isMine = isMine,
                isRead = net.read,
                createdAt = parsedTime,
                reactions = reactions,
                isSending = false,
                isFailed = false
            )
        }
        _directServerMessages.value = mapped
        _isLoadingMessages.value = false
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (isActive) {
                delay(1500)
                try {
                    val target = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId
                    val remote = repository.getDirectMessages(target)
                    if (remote.isNotEmpty()) {
                        updateServerMessages(remote)
                    } else if (target != partnerUserId) {
                        val fallbackRemote = repository.getDirectMessages(partnerUserId)
                        if (fallbackRemote.isNotEmpty()) {
                            updateServerMessages(fallbackRemote)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
        
        // Only send typing indicator if connected and hasn't sent recently
        if (socketConnectionState.value == SocketConnectionState.CONNECTED) {
            val now = System.currentTimeMillis()
            if (now - lastTypingSentTime > 2000) {
                lastTypingSentTime = now
                socketClient.sendTypingStatus(partnerUserId, true)
            }
            
            myTypingTimeoutJob?.let { it.cancel() }
            myTypingTimeoutJob = viewModelScope.launch {
                delay(3000)
                socketClient.sendTypingStatus(partnerUserId, false)
            }
        }
    }

    private fun loadPartnerDetails() {
        viewModelScope.launch {
            // First check local Room cache
            repository.getUserFlow(partnerUserId).collectLatest { user ->
                if (user != null) {
                    _partnerInfo.value = _partnerInfo.value.copy(
                        id = user.id,
                        username = user.username,
                        avatarUrl = user.avatarUrl ?: _partnerInfo.value.avatarUrl,
                        isVerified = user.isVerified
                    )
                }
            }
        }

        // Also fetch network profile for real-time presence & Iddet Plus status
        viewModelScope.launch {
            try {
                val header = repository.userToken?.let { "Bearer $it" }
                val profile = RetrofitClient.apiService.getUserProfile(header, partnerUserId)
                val newAvatar = profile.avatar_url?.ifBlank { null } ?: _partnerInfo.value.avatarUrl
                _partnerInfo.value = ChatPartnerUiModel(
                    id = profile.id ?: partnerUserId,
                    username = profile.username,
                    avatarUrl = newAvatar,
                    isOnline = profile.is_online,
                    isVerified = profile.is_verified,
                    isIddetPlus = profile.is_iddet_plus,
                    cardStyle = profile.card_style,
                    lastSeen = profile.last_seen
                )
            } catch (e: Exception) {
                Log.w("ChatThreadVM", "Could not fetch remote profile: ${e.message}")
            }
        }
    }

    private fun connectSocketIfLoggedIn() {
        viewModelScope.launch {
            val user = repository.currentUser.value
            if (user != null) {
                socketClient.connect(user.id)
            }
        }
    }

    private fun markMessagesRead() {
        viewModelScope.launch {
            repository.markMessagesRead(partnerUserId)
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            socketClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        val currentUserId = repository.currentUser.value?.id ?: ""
                        if (event.senderId == partnerUserId || event.senderId == resolvedPartnerId || (event.senderId == currentUserId && event.messageId.isNotBlank())) {
                            val msg = Message(
                                id = event.messageId,
                                senderId = event.senderId,
                                receiverId = if (event.senderId == currentUserId) resolvedPartnerId else currentUserId,
                                content = event.content,
                                type = event.msgType,
                                isRead = false,
                                createdAt = System.currentTimeMillis()
                            )
                            repository.insertMessageLocal(msg)
                            loadServerMessages()

                            _optimisticMessages.value = _optimisticMessages.value.filter {
                                it.id != event.messageId && !(it.content == event.content && it.isMine)
                            }

                            if (event.senderId == partnerUserId || event.senderId == resolvedPartnerId) {
                                markMessagesRead()
                            }
                        }
                    }

                    is WebSocketEvent.MessageSent -> {
                        loadServerMessages()
                        _optimisticMessages.value = _optimisticMessages.value.filter {
                            it.id != event.messageId && !(it.content == event.content && it.isMine)
                        }
                    }

                    is WebSocketEvent.MessageDeleted -> {
                        repository.deleteMessageLocal(event.messageId)
                        _optimisticMessages.value = _optimisticMessages.value.filter { it.id != event.messageId }
                        loadServerMessages()
                    }
                    
                    is WebSocketEvent.TypingStatus -> {
                        if (event.senderId == partnerUserId || event.senderId == resolvedPartnerId) {
                            _isPartnerTyping.value = event.isTyping
                            partnerTypingTimeoutJob?.let { it.cancel() }
                            if (event.isTyping) {
                                partnerTypingTimeoutJob = viewModelScope.launch {
                                    delay(5000)
                                    _isPartnerTyping.value = false
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun sendTextMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        _inputText.value = ""
        val currentUserId = repository.currentUser.value?.id ?: ""
        val tempId = UUID.randomUUID().toString()
        val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = targetPeer,
            content = text,
            type = "text",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )

        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch {
            try {
                repository.sendMessage(targetPeer, text, "text")
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to send message", e)
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun sendCustomTextMessage(customText: String) {
        if (customText.isBlank()) return
        _inputText.value = ""
        val currentUserId = repository.currentUser.value?.id ?: ""
        val tempId = UUID.randomUUID().toString()
        val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = targetPeer,
            content = customText,
            type = "text",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )

        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch {
            try {
                repository.sendMessage(targetPeer, customText, "text")
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to send custom message", e)
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun sendVoiceFile(file: File) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val myUsername = repository.currentUser.value?.username ?: "Moi"
        val tempId = "voice_" + UUID.randomUUID().toString()
        val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = targetPeer,
            content = file.absolutePath,
            type = "audio",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch(Dispatchers.IO) {
            var sent = false
            try {
                if (file.exists() && file.length() > 0) {
                    val bytes = file.readBytes()
                    val audioB64 = "data:audio/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    sent = socketClient.sendVoiceMessage(targetPeer, audioB64, myUsername)
                    if (!sent) {
                        repository.sendMessage(targetPeer, audioB64, "audio")
                        sent = true
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Error sending voice file", e)
            }

            repository.updateConversationLastMessage(
                otherUserId = targetPeer,
                content = "🎤 Message vocal",
                type = "audio",
                isIncoming = false
            )

            if (sent) {
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } else {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = false) else it
                }
            }
        }
    }

    fun sendVoiceMessage(audioB64: String) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val myUsername = repository.currentUser.value?.username ?: "Moi"
        val tempId = "voice_" + UUID.randomUUID().toString()
        val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = targetPeer,
            content = audioB64,
            type = "audio",
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch(Dispatchers.IO) {
            var sent = false
            try {
                sent = socketClient.sendVoiceMessage(targetPeer, audioB64, myUsername)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!sent) {
                try {
                    repository.sendMessage(targetPeer, audioB64, "audio")
                    sent = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            repository.updateConversationLastMessage(
                otherUserId = targetPeer,
                content = "🎤 Message vocal",
                type = "audio",
                isIncoming = false
            )

            if (sent) {
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } else {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = false) else it
                }
            }
        }
    }

    fun sendMediaMessage(mediaUri: Uri, context: Context, isVideo: Boolean) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val type = if (isVideo) "video" else "image"
        val tempId = UUID.randomUUID().toString()

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = partnerUserId,
            content = mediaUri.toString(),
            type = type,
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch(Dispatchers.IO) {
            var uploadedUrl: String? = null
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(mediaUri) ?: if (isVideo) "video/mp4" else "image/jpeg"
                val extension = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("video") || mimeType.contains("mp4") -> "mp4"
                    mimeType.contains("webm") -> "webm"
                    mimeType.contains("mov") -> "mov"
                    else -> "jpg"
                }
                val prefix = if (isVideo) "chat_vid_" else "chat_img_"
                val tempFile = File.createTempFile(prefix, ".$extension", context.cacheDir)
                contentResolver.openInputStream(mediaUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val token = repository.userToken?.let { "Bearer $it" }
                val response = RetrofitClient.apiService.uploadAudio(token, part)
                uploadedUrl = response.url
                try { tempFile.delete() } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to upload media file, falling back to uri", e)
            }

            val finalUrl = uploadedUrl ?: mediaUri.toString()
            val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId
            try {
                repository.sendMessage(targetPeer, finalUrl, type)
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } catch (e: Exception) {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun sendMediaMessage(url: String, isVideo: Boolean) {
        val currentUserId = repository.currentUser.value?.id ?: ""
        val type = if (isVideo) "video" else "image"
        val tempId = UUID.randomUUID().toString()
        val targetPeer = if (resolvedPartnerId.isNotBlank()) resolvedPartnerId else partnerUserId

        val optimistic = ChatMessageUiModel(
            id = tempId,
            senderId = currentUserId,
            receiverId = targetPeer,
            content = url,
            type = type,
            isMine = true,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            isSending = true
        )
        _optimisticMessages.value = _optimisticMessages.value + optimistic

        viewModelScope.launch {
            try {
                repository.sendMessage(targetPeer, url, type)
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != tempId }
                loadServerMessages()
            } catch (e: Exception) {
                _optimisticMessages.value = _optimisticMessages.value.map {
                    if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                }
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                val currentMessage = repository.getMessagesWith(partnerUserId).first().find { it.id == messageId }
                if (currentMessage?.reaction == emoji) {
                    repository.removeMessageReaction(messageId)
                } else {
                    repository.reactToMessage(messageId, emoji)
                }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to react", e)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId)
                repository.deleteMessageLocal(messageId)
                _optimisticMessages.value = _optimisticMessages.value.filter { it.id != messageId }
            } catch (e: Exception) {
                Log.e("ChatThreadVM", "Failed to delete message", e)
            }
        }
    }

    private fun parseReactions(reactionStr: String?): List<ReactionUiGroup> {
        if (reactionStr.isNullOrBlank()) return emptyList()
        val emojis = reactionStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val counts = emojis.groupingBy { it }.eachCount()
        return counts.map { (emoji, count) ->
            ReactionUiGroup(emoji = emoji, count = count, hasReacted = true)
        }
    }

    private fun groupMessages(messages: List<ChatMessageUiModel>): List<ChatMessageUiModel> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<ChatMessageUiModel>()
        val timeThresholdMs = 3 * 60 * 1000 // 3 minutes

        for (i in messages.indices) {
            val current = messages[i]
            val prev = messages.getOrNull(i - 1)
            val next = messages.getOrNull(i + 1)

            val isSameSenderPrev = prev != null &&
                    prev.senderId == current.senderId &&
                    (current.createdAt - prev.createdAt < timeThresholdMs)

            val isSameSenderNext = next != null &&
                    next.senderId == current.senderId &&
                    (next.createdAt - current.createdAt < timeThresholdMs)

            val isFirstInGroup = !isSameSenderPrev
            val isLastInGroup = !isSameSenderNext
            val showAvatar = !current.isMine && isLastInGroup

            result.add(
                current.copy(
                    isFirstInGroup = isFirstInGroup,
                    isLastInGroup = isLastInGroup,
                    showAvatar = showAvatar
                )
            )
        }
        return result
    }

    class Factory(
        private val partnerUserId: String,
        private val repository: IddetRepository,
        private val socketClient: ChatSocketClient = WebSocketManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatThreadViewModel(partnerUserId, repository, socketClient) as T
        }
    }

    companion object {
        fun provideFactory(
            partnerUserId: String,
            repository: IddetRepository,
            socketClient: ChatSocketClient = WebSocketManager
        ): ViewModelProvider.Factory = Factory(partnerUserId, repository, socketClient)
    }
}
