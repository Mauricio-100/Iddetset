package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import com.example.utils.ChatThemeManager
import com.example.utils.ChatThemePreset
import com.example.utils.RingtoneManagerHelper
import com.example.utils.RingtonePreset
import com.example.utils.SocketConnectionState
import kotlinx.coroutines.launch

/**
 * High-craftsmanship 1-on-1 Chat Thread screen for iDDET with WhatsApp-style swipe to reply,
 * customizable visual themes, synthesized ringtones, and humorous quick reactions.
 */
@Composable
fun ChatThreadScreen(
    userId: String,
    iddetViewModel: IddetViewModel,
    navController: NavController
) {
    val chatViewModel: ChatThreadViewModel = viewModel(
        key = "chat_$userId",
        factory = ChatThreadViewModel.provideFactory(userId, iddetViewModel.repository)
    )
    ChatThreadScreen(
        userId = userId,
        viewModel = chatViewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    userId: String,
    viewModel: ChatThreadViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        ChatThemeManager.init(context)
        RingtoneManagerHelper.init(context)
    }

    val currentTheme by ChatThemeManager.currentTheme.collectAsState()
    val currentRingtone by RingtoneManagerHelper.currentRingtone.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val partnerAvatarUrl = remember(partnerInfo.avatarUrl) {
        partnerInfo.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) } ?: partnerInfo.avatarUrl
    }
    val inputText by viewModel.inputText.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val socketState by viewModel.socketConnectionState.collectAsState()

    val listState = rememberLazyListState()

    // Swipe-to-reply state
    var replyingToMessage by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var showThemeRingtoneSheet by remember { mutableStateOf(false) }
    val disappearingTimer by viewModel.disappearingTimer.collectAsState()
    var showDisappearingDialog by remember { mutableStateOf(false) }

    // Context menu / Bottom sheet state for long-pressed message
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var isVoiceRecording by remember { mutableStateOf(false) }

    // Media Picker for images & videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            val isVideo = mime.contains("video") || uri.toString().contains("video")
            viewModel.sendMediaMessage(uri, context, isVideo)
        }
    }

    val micCallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.utils.CallManager.startCall(
                calleeId = userId,
                calleeUsername = partnerInfo.username,
                calleeAvatar = partnerAvatarUrl
            ) { success, msg ->
                if (!success && msg != null) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permission microphone requise pour les appels", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll when messages change or new message is inserted
    val messagesCount = (uiState as? ChatThreadUiState.Success)?.messages?.size ?: 0
    val isNearBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 3
            }
        }
    }

    LaunchedEffect(messagesCount) {
        if (messagesCount > 0 && isNearBottom) {
            listState.animateScrollToItem(messagesCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("profile/$userId")
                            }
                            .testTag("chat_header_profile")
                    ) {
                        // Circular Avatar with Presence Dot
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = partnerInfo.username.firstOrNull()?.uppercase() ?: "?"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (!partnerAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = partnerAvatarUrl,
                                        contentDescription = "Avatar de ${partnerInfo.username}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Active presence dot
                            if (partnerInfo.isOnline) {
                                PresenceDot(
                                    isOnline = true,
                                    size = 12.dp,
                                    borderColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 1.dp, y = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = partnerInfo.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (partnerInfo.isIddetPlus) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    IddetPlusBadge(size = 15.dp)
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadge(
                                    isVerified = partnerInfo.isVerified,
                                    userName = partnerInfo.username,
                                    showExplainingOnClick = false
                                )
                            }

                            // Live presence status text
                            Text(
                                text = if (partnerInfo.isOnline) "En ligne" else "Vu récemment",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (partnerInfo.isOnline) {
                                    Color(0xFF22C55E)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    // Disappearing Messages Timer
                    IconButton(
                        onClick = { showDisappearingDialog = true },
                        modifier = Modifier.testTag("chat_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Messages éphémères",
                            tint = if (disappearingTimer > 0) Color(0xFF3A76F0) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Chat Theme & Ringtone picker
                    IconButton(
                        onClick = { showThemeRingtoneSheet = true },
                        modifier = Modifier.testTag("chat_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = "Personnaliser le thème",
                            tint = currentTheme.accentColor
                        )
                    }

                    // Voice Call
                    IconButton(
                        onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                com.example.utils.CallManager.startCall(
                                    calleeId = userId,
                                    calleeUsername = partnerInfo.username,
                                    calleeAvatar = partnerAvatarUrl
                                ) { success, msg ->
                                    if (!success && msg != null) {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                micCallLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Appel vocal",
                            tint = Color(0xFF22C55E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // WhatsApp-style Quoted Reply Composer Banner
                    AnimatedVisibility(
                        visible = replyingToMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingToMessage?.let { replyTarget ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(currentTheme.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (replyTarget.isMine) "Réponse à vous-même" else "Réponse à ${partnerInfo.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = currentTheme.accentColor
                                        )
                                        Text(
                                            text = when {
                                                replyTarget.type == "voice" -> "🎙️ Message vocal"
                                                replyTarget.type == "image" -> "📷 Photo"
                                                replyTarget.type == "video" -> "🎬 Vidéo"
                                                else -> replyTarget.content.replace("\n", " ")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyingToMessage = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Annuler la réponse",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isVoiceRecording) {
                        // Voice Recorder UI
                        VoiceRecorderUI(
                            onCancel = { isVoiceRecording = false },
                            onSendVoiceFile = { file ->
                                viewModel.sendVoiceFile(file)
                                isVoiceRecording = false
                            },
                            onSendVoice = { voiceString ->
                                viewModel.sendVoiceMessage(voiceString)
                                isVoiceRecording = false
                            }
                        )
                    } else {
                        // Standard text and media input bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attachment button (Photos/Videos)
                            IconButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier.testTag("chat_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Joindre un média",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Voice message button
                            IconButton(
                                onClick = { isVoiceRecording = true },
                                modifier = Modifier.testTag("chat_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Enregistrer un vocal",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Text Field (multi-line extensible)
                            TextField(
                                value = inputText,
                                onValueChange = { viewModel.onInputTextChange(it) },
                                placeholder = {
                                    Text(
                                        text = if (replyingToMessage != null) "Votre réponse…" else "Message…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                maxLines = 4,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .testTag("chat_input_field")
                            )

                            // Send Button (only active when not empty)
                            val canSend = inputText.trim().isNotBlank()
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        val reply = replyingToMessage
                                        if (reply != null) {
                                            val author = if (reply.isMine) "Vous" else partnerInfo.username
                                            val snippet = reply.content.take(60).replace("\n", " ")
                                            viewModel.sendCustomTextMessage("[quote:$author|$snippet]\n$inputText")
                                            replyingToMessage = null
                                        } else {
                                            viewModel.sendTextMessage()
                                        }
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier.testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Envoyer",
                                    tint = if (canSend) {
                                        currentTheme.accentColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(currentTheme.backgroundBrush)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Discrete connection state banner
                var showThreadConnectingBanner by remember { mutableStateOf(false) }
                LaunchedEffect(socketState) {
                    if (socketState == SocketConnectionState.CONNECTING) {
                        showThreadConnectingBanner = true
                        delay(3000)
                        showThreadConnectingBanner = false
                    } else {
                        showThreadConnectingBanner = false
                    }
                }

                AnimatedVisibility(
                    visible = showThreadConnectingBanner,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connexion en direct…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // IDDET End-to-End Encryption Security Banner
                Surface(
                    color = Color(0xFF1E2633).copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF3A76F0),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Messages et appels sécurisés de bout en bout IDDET",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (showDisappearingDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisappearingDialog = false },
                        title = { Text("Messages éphémères IDDET") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Choisissez la durée après laquelle les nouveaux messages disparaîtront de cette discussion :")
                                Spacer(modifier = Modifier.height(4.dp))
                                val options = listOf(
                                    "Désactivé" to 0L,
                                    "5 secondes" to 5000L,
                                    "1 minute" to 60000L,
                                    "1 heure" to 3600000L,
                                    "1 jour" to 86400000L
                                )
                                options.forEach { (label, duration) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setDisappearingTimer(duration)
                                                showDisappearingDialog = false
                                                Toast.makeText(context, "Messages éphémères: $label", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = disappearingTimer == duration,
                                            onClick = {
                                                viewModel.setDisappearingTimer(duration)
                                                showDisappearingDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDisappearingDialog = false }) {
                                Text("Fermer")
                            }
                        }
                    )
                }

                // Messages View
                when (val state = uiState) {
                    is ChatThreadUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = currentTheme.accentColor)
                        }
                    }

                    is ChatThreadUiState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Dites bonjour à ${partnerInfo.username} 👋",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Glissez un message à gauche ou à droite pour répondre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ChatThreadUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is ChatThreadUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chat_messages_list"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = state.messages,
                                key = { it.id }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    partnerAvatar = partnerAvatarUrl,
                                    partnerUsername = partnerInfo.username,
                                    onLongClick = {
                                        selectedMessageForMenu = message
                                    },
                                    onReactionClick = { emoji ->
                                        viewModel.toggleReaction(message.id, emoji)
                                    },
                                    onImageClick = { imageUrl ->
                                        fullScreenImageUrl = imageUrl
                                    },
                                    onVideoClick = { videoUrl ->
                                        fullScreenImageUrl = videoUrl
                                    },
                                    onSwipeToReply = { target ->
                                        replyingToMessage = target
                                    }
                                )
                            }

                            if (isPartnerTyping) {
                                item(key = "typing_indicator") {
                                    com.example.ui.components.TypingIndicatorBubble(
                                        partnerAvatar = partnerAvatarUrl,
                                        partnerUsername = partnerInfo.username
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating "Scroll to bottom / New message" button when user is scrolled up
            if (!isNearBottom && messagesCount > 0) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messagesCount - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(40.dp)
                        .testTag("chat_scroll_bottom_button"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Descendre"
                    )
                }
            }
        }
    }

    // Context Menu Bottom Sheet for Long-Pressed Message
    if (selectedMessageForMenu != null) {
        val targetMessage = selectedMessageForMenu!!

        ModalBottomSheet(
            onDismissRequest = { selectedMessageForMenu = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick emoji reaction bar
                ReactionPicker(
                    onSelectEmoji = { emoji ->
                        viewModel.toggleReaction(targetMessage.id, emoji)
                        selectedMessageForMenu = null
                    },
                    onDismiss = { selectedMessageForMenu = null },
                    modifier = Modifier.fillMaxWidth()
                )

                // Humorous quick reaction chips
                Column {
                    Text(
                        text = "Réactions & Humour",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val humorReactions = listOf("🤣 MDR", "🔥 Chaud", "👀 Oula", "👑 Boss", "💀 Mort", "🎉 Fête", "💯 Validé", "🚀 Fusée")
                        items(humorReactions) { item ->
                            val emoji = item.split(" ").first()
                            SuggestionChip(
                                onClick = {
                                    viewModel.toggleReaction(targetMessage.id, emoji)
                                    selectedMessageForMenu = null
                                },
                                label = { Text(item, fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Action: Swipe-to-reply trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            replyingToMessage = targetMessage
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Répondre à ce message",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action: Copy text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(targetMessage.content))
                            Toast.makeText(context, "Message copié", Toast.LENGTH_SHORT).show()
                            selectedMessageForMenu = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Copier le texte",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action: Delete message (ONLY visible on user's own messages)
                if (targetMessage.isMine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.deleteMessage(targetMessage.id)
                                selectedMessageForMenu = null
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("chat_menu_delete_message"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Supprimer le message",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Themes & Ringtone Customizer Bottom Sheet
    if (showThemeRingtoneSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                RingtoneManagerHelper.stopPreview()
                showThemeRingtoneSheet = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChatThemeAndRingtoneContent(
                currentTheme = currentTheme,
                currentRingtone = currentRingtone,
                onSelectTheme = { theme ->
                    ChatThemeManager.setTheme(theme, context)
                },
                onSelectRingtone = { ringtone ->
                    RingtoneManagerHelper.setRingtone(ringtone, context)
                },
                onPlayRingtone = { ringtone ->
                    RingtoneManagerHelper.playPreview(context, ringtone)
                },
                onStopRingtone = {
                    RingtoneManagerHelper.stopPreview()
                }
            )
        }
    }

    // Full Screen Image/Video Viewer Dialog
    if (fullScreenImageUrl != null) {
        val mediaUrl = fullScreenImageUrl!!
        val isVideo = mediaUrl.contains(".mp4") || mediaUrl.contains(".webm") || mediaUrl.contains("/video/") || mediaUrl.contains("chat_vid_")

        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (isVideo) {
                    ActfileVideoPlayer(
                        videoUrl = mediaUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Image en plein écran",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Tabbed customizer for 6 chat visual themes & 8 built-in synthesized ringtones
 */
@Composable
private fun ChatThemeAndRingtoneContent(
    currentTheme: ChatThemePreset,
    currentRingtone: RingtonePreset,
    onSelectTheme: (ChatThemePreset) -> Unit,
    onSelectRingtone: (RingtonePreset) -> Unit,
    onPlayRingtone: (RingtonePreset) -> Unit,
    onStopRingtone: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thèmes visuels (6)", "Sonneries d'appel (8)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        onStopRingtone()
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // Theme selection list
            Text(
                text = "Choisissez l'ambiance de vos discussions :",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(ChatThemeManager.allThemes) { theme ->
                    val isSelected = theme.id == currentTheme.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            2.dp,
                            if (isSelected) theme.accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTheme(theme) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.backgroundBrush)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Sélectionné",
                                            tint = theme.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = theme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                // Bubble previews
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 18.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(theme.myBubbleColor)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 18.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(theme.partnerBubbleColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Ringtone selection list
            Text(
                text = "Sonneries intégrées pour les appels audio/vidéo :",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(RingtoneManagerHelper.allRingtones) { ringtone ->
                    val isSelected = ringtone.id == currentRingtone.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectRingtone(ringtone)
                                onPlayRingtone(ringtone)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = ringtone.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = ringtone.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onPlayRingtone(ringtone) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Écouter",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Actif",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
