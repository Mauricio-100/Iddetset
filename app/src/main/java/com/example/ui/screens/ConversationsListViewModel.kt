package com.example.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationNetwork
import com.example.data.IddetRepository
import com.example.utils.ChatSocketClient
import com.example.utils.SocketConnectionState
import com.example.utils.WebSocketEvent
import com.example.utils.WebSocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*

enum class ConversationFilter {
    ALL,
    ONLINE,
    UNREAD
}

sealed interface ConversationsUiState {
    object Loading : ConversationsUiState
    data class Success(
        val conversations: List<ConversationNetwork>,
        val isOffline: Boolean = false
    ) : ConversationsUiState
    object Empty : ConversationsUiState
    data class Error(val message: String) : ConversationsUiState
}

class ConversationsListViewModel(
    val repository: IddetRepository,
    private val socketClient: ChatSocketClient = WebSocketManager
) : ViewModel() {

    private val _rawConversations = MutableStateFlow<List<ConversationNetwork>>(repository.conversations.value)
    private val _isLoading = MutableStateFlow(repository.conversations.value.isEmpty())
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ConversationFilter.ALL)
    val selectedFilter: StateFlow<ConversationFilter> = _selectedFilter.asStateFlow()

    val socketConnectionState: StateFlow<SocketConnectionState> = socketClient.connectionState

    private val _filterParams = combine(_searchQuery, _selectedFilter) { query, filter ->
        query to filter
    }

    // Derived UI State combining raw list, search query, filter, connection, and loading
    val uiState: StateFlow<ConversationsUiState> = combine(
        _rawConversations,
        _filterParams,
        _isLoading,
        _errorMessage,
        socketConnectionState
    ) { conversations, (query, filter), loading, error, connState ->
        if (loading && conversations.isEmpty()) {
            ConversationsUiState.Loading
        } else if (error != null && conversations.isEmpty()) {
            ConversationsUiState.Error(error)
        } else {
            val filtered = conversations.filter { conv ->
                val matchesQuery = query.isBlank() || conv.username.contains(query.trim(), ignoreCase = true)
                val matchesFilter = when (filter) {
                    ConversationFilter.ALL -> true
                    ConversationFilter.ONLINE -> conv.is_online
                    ConversationFilter.UNREAD -> (conv.unread_count ?: 0) > 0
                }
                matchesQuery && matchesFilter
            }

            if (filtered.isEmpty() && query.isBlank() && filter == ConversationFilter.ALL) {
                ConversationsUiState.Empty
            } else {
                ConversationsUiState.Success(
                    conversations = filtered,
                    isOffline = connState != SocketConnectionState.CONNECTED
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.conversations.value.isNotEmpty()) {
            ConversationsUiState.Success(repository.conversations.value)
        } else {
            ConversationsUiState.Loading
        }
    )

    init {
        loadConversations()
        startPeriodicSync()
        observeRepositoryConversations()
        observeWebSocketEvents()
        connectSocketIfLoggedIn()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: ConversationFilter) {
        _selectedFilter.value = filter
    }

    fun refresh() {
        loadConversations()
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(1800)
                try {
                    val list = repository.refreshConversations()
                    if (list.isNotEmpty() || _rawConversations.value.isEmpty()) {
                        _rawConversations.value = sortConversations(list)
                    }
                    _isLoading.value = false
                } catch (_: Exception) {}
            }
        }
    }

    private fun observeRepositoryConversations() {
        viewModelScope.launch {
            repository.conversations.collect { convs ->
                if (convs.isNotEmpty() || _rawConversations.value.isEmpty()) {
                    _rawConversations.value = sortConversations(convs)
                    _isLoading.value = false
                }
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            if (_rawConversations.value.isEmpty()) {
                _isLoading.value = true
            }
            _errorMessage.value = null
            try {
                val list = repository.refreshConversations()
                // Keep conversations intact and update seamlessly
                if (list.isNotEmpty() || _rawConversations.value.isEmpty()) {
                    _rawConversations.value = sortConversations(list)
                }
            } catch (e: Exception) {
                Log.e("ConversationsListVM", "Failed to load conversations", e)
                if (_rawConversations.value.isEmpty()) {
                    _errorMessage.value = e.localizedMessage ?: "Impossible de charger les conversations"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun connectSocketIfLoggedIn() {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                if (user != null && user.id.isNotBlank()) {
                    socketClient.connect(user.id)
                }
            }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            socketClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        handleNewMessageEvent(event)
                        // Trigger background sync to capture official server state
                        viewModelScope.launch {
                            try {
                                val list = repository.refreshConversations()
                                if (list.isNotEmpty() || _rawConversations.value.isEmpty()) {
                                    _rawConversations.value = sortConversations(list)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    is WebSocketEvent.MessageDeleted -> {
                        loadConversations()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleNewMessageEvent(event: WebSocketEvent.NewMessage) {
        val currentList = _rawConversations.value.toMutableList()
        val currentUserId = repository.currentUser.value?.id ?: ""
        val isFromMe = event.senderId == currentUserId

        val partnerId = if (isFromMe) {
            // Received own message reflection
            return
        } else {
            event.senderId
        }

        val existingIndex = currentList.indexOfFirst { it.user_id == partnerId }

        val snippet = when {
            com.example.utils.AudioMessageHelper.isAudioContent(event.content, event.msgType) -> "🎤 Message vocal"
            event.msgType == "image" -> "[Photo]"
            event.msgType == "video" -> "[Vidéo]"
            else -> event.content
        }

        val updatedItem = if (existingIndex >= 0) {
            val old = currentList.removeAt(existingIndex)
            old.copy(
                last_message = snippet,
                last_message_time = event.timestamp.ifBlank { System.currentTimeMillis().toString() },
                unread_count = (old.unread_count ?: 0) + 1
            )
        } else {
            ConversationNetwork(
                id = event.messageId,
                user_id = partnerId,
                username = event.senderUsername.ifBlank { "Utilisateur" },
                avatar_url = null,
                last_message = snippet,
                last_message_time = event.timestamp.ifBlank { System.currentTimeMillis().toString() },
                unread_count = 1,
                is_online = true
            )
        }

        // Insert at index 0 to smoothly bump the conversation to top
        currentList.add(0, updatedItem)
        _rawConversations.value = currentList
    }

    fun deleteConversation(userId: String) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(userId)
                _rawConversations.value = _rawConversations.value.filter { it.user_id != userId }
            } catch (e: Exception) {
                Log.e("ConversationsListVM", "Failed to delete conversation", e)
            }
        }
    }

    private fun sortConversations(list: List<ConversationNetwork>): List<ConversationNetwork> {
        return list.sortedByDescending { it.last_message_time ?: "" }
    }

    class Factory(
        private val repository: IddetRepository,
        private val socketClient: ChatSocketClient = WebSocketManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationsListViewModel(repository, socketClient) as T
        }
    }

    companion object {
        fun provideFactory(
            repository: IddetRepository,
            socketClient: ChatSocketClient = WebSocketManager
        ): ViewModelProvider.Factory = Factory(repository, socketClient)
    }
}
