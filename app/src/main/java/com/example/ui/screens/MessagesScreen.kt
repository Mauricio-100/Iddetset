package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ConversationNetwork
import com.example.data.Story
import com.example.ui.IddetViewModel
import com.example.ui.components.StoriesBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.StoryCreatorDialog
import com.example.ui.components.VerificationBadge
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(viewModel: IddetViewModel, navController: NavController) {
    ConversationsListScreen(viewModel, navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyMessagesScreen(viewModel: IddetViewModel, navController: NavController) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val groupedAndSortedStories = remember(stories) {
        stories.groupBy { it.user.id }.values.flatten()
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "online", "unread"

    var selectedStoryForViewer by remember { mutableStateOf<Pair<List<Story>, Int>?>(null) }
    var showStoryCreatorSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshConversations()
        viewModel.loadStories()
        while (true) {
            kotlinx.coroutines.delay(8000)
            viewModel.refreshConversations()
        }
    }

    // Filtered conversations
    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchesSearch = conv.username.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "online" -> conv.is_online
                "unread" -> (conv.unread_count ?: 0) > 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Rechercher un utilisateur...", fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("conversations_search_input"),
                                    shape = RoundedCornerShape(24.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            isSearchActive = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            } else {
                                Text(
                                    text = "Discussions",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(
                                    onClick = { isSearchActive = true },
                                    modifier = Modifier.testTag("conversations_search_button")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" },
                            label = { Text("Toutes", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        FilterChip(
                            selected = selectedFilter == "online",
                            onClick = { selectedFilter = "online" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("En ligne", fontSize = 13.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        FilterChip(
                            selected = selectedFilter == "unread",
                            onClick = { selectedFilter = "unread" },
                            label = { Text("Non lues", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                StoriesBar(
                    viewModel = viewModel,
                    stories = groupedAndSortedStories,
                    onStoryClick = { clickedStory ->
                        val index = groupedAndSortedStories.indexOfFirst { it.id == clickedStory.id }.coerceAtLeast(0)
                        selectedStoryForViewer = Pair(groupedAndSortedStories, index)
                    },
                    onCreateClick = {
                        showStoryCreatorSheet = true
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredConversations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Aucun utilisateur trouvé" else "Aucune discussion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) 
                                    "Essayez de modifier votre recherche pour trouver un autre membre de la communauté."
                                else 
                                    "Lancez une discussion en visitant le profil d'un membre de la communauté ou en cherchant un utilisateur !",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredConversations, key = { it.id ?: it.user_id }) { conv ->
                    ConversationItem(
                        conv = conv,
                        onClick = { navController.navigate("chat/${conv.user_id}") }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                }
            }
        }

        // Story Viewer Dialog
        selectedStoryForViewer?.let { (storyList, index) ->
            StoryViewerDialog(
                stories = storyList,
                initialIndex = index,
                viewModel = viewModel,
                onDismiss = { selectedStoryForViewer = null }
            )
        }

        // Story Creator Bottom Sheet
        if (showStoryCreatorSheet) {
            StoryCreatorDialog(
                viewModel = viewModel,
                onDismiss = { showStoryCreatorSheet = false }
            )
        }
    }
}

fun formatTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = format.parse(isoString) ?: return ""
        val outFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun ConversationItem(conv: ConversationNetwork, onClick: () -> Unit) {
    val hasUnread = (conv.unread_count ?: 0) > 0
    
    val isTyping = conv.user_id.hashCode() % 5 == 0 && conv.is_online
    val isRecordingVoice = conv.user_id.hashCode() % 8 == 0 && conv.is_online && !isTyping
    val isRead = conv.user_id.hashCode() % 4 == 0 && !hasUnread

    val infiniteTransition = rememberInfiniteTransition(label = "simulated_states")
    val typingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_alpha"
    )
    val micScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("conversation_item_${conv.user_id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Online status
        Box(
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!conv.avatar_url.isNullOrBlank()) {
                    AsyncImage(
                        model = conv.avatar_url?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                        contentDescription = "Photo de profil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = conv.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            if (conv.is_online) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)) // Beautiful emerald green
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Username and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = conv.username,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(userName = conv.username, isVerified = conv.is_verified)
                    Spacer(modifier = Modifier.width(4.dp))
                    if (conv.is_online) {
                        Text("!!", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Text("!", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(conv.last_message_time),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Last Message and Unread badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRead) {
                        Text("✔✔ ", color = Color(0xFF3B82F6), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                    
                    if (isTyping) {
                        Text(
                            text = "en train d'écrire...",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = typingAlpha),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (isRecordingVoice) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Enregistrement",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).scale(micScale)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "enregistre un audio...",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val lastMsgText = com.example.utils.AudioMessageHelper.getPreviewText(conv.last_message)
                        Text(
                            text = lastMsgText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (conv.unread_count ?: 0).toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
