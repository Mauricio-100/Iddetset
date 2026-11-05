package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.Message
import com.example.ui.IddetViewModel
import com.example.utils.UrlHelper
import com.example.ui.components.VerificationBadge
import com.example.ui.components.MarkdownActfile
import com.example.ui.components.VoiceMessagePlayer
import com.example.ui.components.VoiceRecorderUI
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(userId: String, viewModel: IddetViewModel, navController: NavController) {
    ChatThreadScreen(userId = userId, iddetViewModel = viewModel, navController = navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyChatScreen(userId: String, viewModel: IddetViewModel, navController: NavController) {
    val myUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val partnerConversation = conversations.find { it.user_id == userId }
    val partner = viewModel.searchUsersResult.collectAsStateWithLifecycle().value.find { it.id == userId }

    val partnerName = partnerConversation?.username ?: partner?.username ?: "Utilisateur"
    val partnerAvatar = partnerConversation?.avatar_url ?: partner?.avatarUrl
    val isOnline = partnerConversation?.is_online ?: false

    val messages by viewModel.getMessagesWith(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Poll for new messages every 3 seconds while on this screen
    LaunchedEffect(userId) {
        while (true) {
            viewModel.refreshMessagesWith(userId)
            kotlinx.coroutines.delay(3000)
        }
    }
    
    if (myUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("profile/$userId")
                            }
                            .padding(vertical = 4.dp)
                            .testTag("chat_partner_header")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!partnerAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = partnerAvatar?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = partnerName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = partnerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadge(userName = partnerName, isVerified = partnerConversation?.is_verified ?: partner?.isVerified ?: false)
                            }
                            if (isOnline) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En ligne", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Hors ligne", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            MessageInputField(
                onSendMessage = { text ->
                    val finalContent = if (replyingToMessage != null) {
                        val snippet = if (com.example.utils.AudioMessageHelper.isAudioContent(replyingToMessage!!.content, replyingToMessage!!.type)) "🎤 Message vocal" else replyingToMessage!!.content.take(50).replace("\n", " ")
                        "[ReplyTo:${replyingToMessage!!.id}|$snippet]\n$text"
                    } else text
                    viewModel.sendMessage(userId, finalContent)
                    replyingToMessage = null
                },
                onSendVoice = { voiceString ->
                    viewModel.sendVoiceMessage(userId, voiceString)
                    replyingToMessage = null
                },
                onSendVoiceFile = { file ->
                    viewModel.sendVoiceMessage(userId, file)
                    replyingToMessage = null
                },
                replyingToMessage = replyingToMessage,
                onCancelReply = { replyingToMessage = null }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableConversationView(
                messages = messages,
                currentUserId = myUser?.id ?: "",
                listState = listState,
                onLinkClick = { url ->
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("browser/$encodedUrl")
                },
                onReact = { msgId, emoji ->
                    if (emoji != null) {
                        viewModel.reactToMessage(msgId, emoji)
                    } else {
                        viewModel.removeMessageReaction(msgId)
                    }
                },
                onDeleteMessage = { msgId ->
                    viewModel.deleteMessage(msgId)
                },
                onReplyMessage = { msg -> replyingToMessage = msg },
                modifier = Modifier.fillMaxSize()
            )

            // Scroll to bottom floating button when scrolled up
            val showScrollToBottom by remember {
                derivedStateOf {
                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    messages.isNotEmpty() && lastVisibleItem < messages.size - 2
                }
            }

            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("scroll_to_bottom_btn")
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bas de la conversation",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scrollable conversation list displaying all message bubbles and date separators.
 */
@Composable
fun ScrollableConversationView(
    messages: List<Message>,
    currentUserId: String,
    listState: LazyListState,
    onLinkClick: (String) -> Unit,
    onReact: (String, String?) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onReplyMessage: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val dotPatternColor = if (isLight) Color.DarkGray.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.03f)

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                val spacing = 28.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(
                            color = dotPatternColor,
                            radius = 1.5f,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("conversation_messages_list"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 1.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = "🔒 Les messages sont chiffrés de bout en bout.",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
            
            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                val isMine = msg.senderId == currentUserId
                
                // Date separator logic
                val showDateHeader = if (index == 0) {
                    true
                } else {
                    val prevMsg = messages[index - 1]
                    val formatDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    formatDay.format(Date(msg.createdAt)) != formatDay.format(Date(prevMsg.createdAt))
                }

                if (showDateHeader) {
                    DateSeparator(timestamp = msg.createdAt)
                }

                MessageBubbleItem(
                    message = msg,
                    isMine = isMine,
                    onLinkClick = onLinkClick,
                    onReact = { emoji -> onReact(msg.id, emoji) },
                    onDelete = { onDeleteMessage(msg.id) },
                    onReply = { onReplyMessage(msg) }
                )
            }
        }
    }
}

/**
 * Individual message bubble supporting text, markdown, links, audio voice notes, quick reactions, and deletion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: Message,
    isMine: Boolean,
    onLinkClick: (String) -> Unit,
    onReact: (String?) -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isAudio = com.example.utils.AudioMessageHelper.isAudioContent(message.content, message.type) || 
                  message.type == "audio_sending" || 
                  message.type == "audio_error"

    var showActionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val quickEmojis = listOf("❤️", "👍", "🔥", "😂", "😮", "😢", "👏", "💯")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isMine) 20.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 20.dp
                ),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMine) 20.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 20.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {
                            if (message.type != "audio" && message.type != "story_reaction") {
                                showActionDialog = true
                            }
                        },
                        onDoubleClick = {
                            if (message.reaction == "❤️") {
                                onReact(null)
                            } else {
                                onReact("❤️")
                            }
                        },
                        onLongClick = {
                            showActionDialog = true
                        }
                    )
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    if (isAudio) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            when (message.type) {
                                "audio_sending" -> {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Envoi...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                "audio_error" -> {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Erreur",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "L'envoi a échoué",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                else -> {
                                    VoiceMessagePlayer(content = message.content, isMine = isMine)
                                }
                            }
                        }
                    } else {
                        val storyRegex = remember { Regex("""^\[Story:(.*?)\|(.*?)\]\s*([\s\S]*)""") }
                        val storyMatch = if (message.content.startsWith("[Story:")) storyRegex.find(message.content) else null
                        
                        val replyToRegex = remember { Regex("""^\[ReplyTo:(.*?)\|(.*?)\]\s*([\s\S]*)""") }
                        val replyToMatch = if (message.content.startsWith("[ReplyTo:")) replyToRegex.find(message.content) else null

                        if (storyMatch != null) {
                            val storyMediaUrl = storyMatch.groupValues[1]
                            val storyAuthor = storyMatch.groupValues[2]
                            val replyBody = storyMatch.groupValues[3].trim()

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. Main reply content (Message / Sticker / Emoji Reaction) shown first
                                if (replyBody.isNotBlank()) {
                                    val isSingleEmoji = replyBody.length <= 4 && replyBody.any { Character.isSurrogate(it) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }
                                    if (isSingleEmoji) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                                        ) {
                                            Text(text = replyBody, fontSize = 32.sp)
                                        }
                                    } else {
                                        MarkdownActfile(
                                            content = replyBody,
                                            isMine = isMine,
                                            compactOpenGraph = true,
                                            onLinkClick = onLinkClick,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // 2. Story Miniature Thumbnail shown underneath the sticker/message
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (storyMediaUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = UrlHelper.fixCloudinaryUrl(storyMediaUrl) ?: storyMediaUrl,
                                                contentDescription = "Story miniature",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.FlashOn,
                                                    contentDescription = null,
                                                    tint = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Story de $storyAuthor",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = if (message.type == "story_reaction") "Réaction envoyée" else "Réponse à la story",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (replyToMatch != null) {
                            val replyToSnippet = replyToMatch.groupValues[2]
                            val actualMessage = replyToMatch.groupValues[3].trim()

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(28.dp)
                                                .background(if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(1.5.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "En réponse à", 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = replyToSnippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (actualMessage.isNotBlank()) {
                                    MarkdownActfile(
                                        content = actualMessage,
                                        isMine = isMine,
                                        compactOpenGraph = true,
                                        onLinkClick = onLinkClick,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else {
                            MarkdownActfile(
                                content = message.content,
                                isMine = isMine,
                                compactOpenGraph = true,
                                onLinkClick = onLinkClick,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bubble footer containing Time + Status icon
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 8.dp, bottom = 4.dp, start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMessageTime(message.createdAt),
                            fontSize = 9.sp,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (isMine) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = if (message.isRead) "Lu" else "Envoyé",
                                tint = if (message.isRead) Color(0xFF818CF8) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // Attached reaction pill badge
            if (!message.reaction.isNullOrBlank()) {
                Surface(
                    onClick = { showActionDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .padding(top = 2.dp, start = if (!isMine) 8.dp else 0.dp, end = if (isMine) 8.dp else 0.dp)
                ) {
                    Text(
                        text = message.reaction,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Reaction and Message Options Modal Dialog
    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = {
                Text(
                    text = "Réagir au message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Emoji Reaction Bar
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        items(quickEmojis.size) { idx ->
                            val emoji = quickEmojis[idx]
                            Surface(
                                onClick = {
                                    if (message.reaction == emoji) {
                                        onReact(null)
                                    } else {
                                        onReact(emoji)
                                    }
                                    showActionDialog = false
                                },
                                shape = CircleShape,
                                color = if (message.reaction == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Répondre") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showActionDialog = false
                            onReply()
                        }
                    )

                    if (!isAudio) {
                        ListItem(
                            headlineContent = { Text("Copier le texte") },
                            leadingContent = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(message.content))
                                showActionDialog = false
                            }
                        )
                    }

                    if (isMine) {
                        ListItem(
                            headlineContent = { Text("Supprimer le message", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                showActionDialog = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer le message ?") },
            text = { Text("Ce message sera définitivement supprimé.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Message input bar with quick reply chips, text input field, voice note recording, and send button.
 */
@Composable
fun MessageInputField(
    onSendMessage: (String) -> Unit,
    onSendVoiceFile: (java.io.File) -> Unit,
    onSendVoice: ((String) -> Unit)? = null,
    replyingToMessage: Message? = null,
    onCancelReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var isRecordingMode by remember { mutableStateOf(false) }

    val quickReplies = listOf(
        "🐾 Salut !",
        "🐱 Meow !",
        "Comment ça va ?",
        "Trop cool ! 👍",
        "À plus tard !"
    )

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (replyingToMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("En réponse à", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (com.example.utils.AudioMessageHelper.isAudioContent(replyingToMessage.content, replyingToMessage.type)) "🎤 Message vocal" else replyingToMessage.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Annuler", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Quick Replies Row (only shown when not recording)
        if (!isRecordingMode) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(quickReplies) { _, reply ->
                    SuggestionChip(
                        onClick = {
                            onSendMessage(reply)
                        },
                        label = { Text(reply, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            if (isRecordingMode) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    VoiceRecorderUI(
                        onCancel = { isRecordingMode = false },
                        onSendVoice = { voiceString ->
                            onSendVoice?.invoke(voiceString)
                            isRecordingMode = false
                        },
                        onSendVoiceFile = { file ->
                            onSendVoiceFile(file)
                            isRecordingMode = false
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    placeholder = { Text("Écrire un message...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (messageText.isBlank()) {
                    IconButton(
                        onClick = { isRecordingMode = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("chat_record_button")
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Vocal",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            val textToSend = messageText.trim()
                            if (textToSend.isNotBlank()) {
                                onSendMessage(textToSend)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(x = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = formatMessageDate(timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

fun formatMessageTime(timestampMillis: Long): String {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(Date(timestampMillis))
    } catch (e: Exception) {
        ""
    }
}

fun formatMessageDate(timestampMillis: Long): String {
    val date = Date(timestampMillis)
    val now = Date()
    val formatDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    if (formatDay.format(date) == formatDay.format(now)) {
        return "Aujourd'hui"
    }
    val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    if (formatDay.format(date) == formatDay.format(yesterday)) {
        return "Hier"
    }
    val outFormat = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH)
    return outFormat.format(date)
}

