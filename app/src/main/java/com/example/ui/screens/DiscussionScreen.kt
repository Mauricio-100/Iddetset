package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.ActfileComment
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.MarkdownActfile
import com.example.ui.components.VerificationBadge
import com.example.ui.components.CopyableUserId
import com.example.ui.components.VoiceRecorderUI
import com.example.ui.components.VoiceMessagePlayer
import com.example.ui.components.isVoiceMessage
import com.example.utils.AudioMessageHelper
import com.example.ui.components.ActfileCard
import com.example.ui.components.getRelativeTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(
    viewModel: IddetViewModel,
    navController: NavController,
    actfileId: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actfile by viewModel.getActfile(actfileId).collectAsStateWithLifecycle(initialValue = null)
    val comments by viewModel.getComments(actfileId).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    var replyText by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }

    var translatedContent by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var currentTranslatedTo by remember { mutableStateOf<String?>(null) }
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var smartReplies by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(actfile?.id) {
        val currentActfile = actfile
        if (currentActfile != null && com.example.utils.LocalAiManager.state.value == com.example.utils.AiModelState.READY) {
            smartReplies = com.example.utils.LocalAiManager.suggestReplies(currentActfile.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    val currentAct = actfile
                    if (currentAct != null) {
                        IconButton(
                            onClick = {
                                com.example.utils.ShareHelper.shareActfile(
                                    context = context,
                                    actfileId = currentAct.id,
                                    authorUsername = currentAct.username,
                                    content = currentAct.content,
                                    category = currentAct.category
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager la publication"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Post content at the top
                actfile?.let { post ->
                    item {
                        ActfileCard(
                            actfile = post,
                            isDetailView = true,
                            targetLanguageName = targetLanguage,
                            onLike = { viewModel.likeActfile(post.id) },
                            onView = { viewModel.incrementView(post.id) },
                            onUserClick = { userId ->
                                val myId = currentUser?.id
                                if (userId == myId) {
                                    navController.navigate("profile")
                                } else {
                                    navController.navigate("profile/$userId")
                                }
                            },
                            onComment = { /* already on discussion screen */ },
                            onDelete = if (post.userId == currentUser?.id) { { viewModel.deleteActfile(post.id); navController.popBackStack() } } else null,
                            onCategoryClick = { channelSlug ->
                                navController.navigate("community/$channelSlug")
                            },
                            onCommunityClick = { slug ->
                                navController.navigate("community/$slug")
                            },
                            onMentionClick = { mention ->
                                if (mention.startsWith("@")) {
                                    coroutineScope.launch {
                                        val u = viewModel.getUserByUsername(mention.removePrefix("@"))
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                } else if (mention.startsWith("#")) {
                                    navController.navigate("search?query=${java.net.URLEncoder.encode(mention, "UTF-8")}")
                                }
                            },
                        )
                    }
                }
                
                // Section Title: Discussions (Count)
                item {
                    Text(
                        text = "Discussions (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                // Comments list
                if (comments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune discussion pour le moment. Lancez la conversation !",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Commenter Header: Avatar + Username + VerificationBadge + Timestamp relative
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val myId = viewModel.currentUser.value?.id
                                        if (comment.userId == myId) {
                                            navController.navigate("profile")
                                        } else {
                                            navController.navigate("profile/${comment.userId}")
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initial = comment.username.firstOrNull()?.toString()?.uppercase() ?: "?"
                                        Text(
                                            text = initial,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        val commentAvatar = comment.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) } ?: comment.avatarUrl
                                        if (!commentAvatar.isNullOrBlank()) {
                                            AsyncImage(
                                                model = commentAvatar,
                                                contentDescription = "Avatar de ${comment.username}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = comment.username,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerificationBadge(
                                                userName = comment.username,
                                                isVerified = comment.isVerified,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = getRelativeTimeString(comment.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        CopyableUserId(
                                            id = comment.userId,
                                            isBot = comment.username.contains("bot", ignoreCase = true)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Comment Body (Supporting dedicated Voice Message presentation and Markdown)
                                val scope = rememberCoroutineScope()
                                val isVoice = isVoiceMessage(comment.content) || AudioMessageHelper.isAudioContent(comment.content)
                                val isMine = comment.userId == currentUser?.id

                                if (isVoice) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Réponse vocale",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            VoiceMessagePlayer(
                                                content = comment.content,
                                                isMine = isMine
                                            )
                                        }
                                    }
                                } else {
                                    MarkdownActfile(
                                        content = comment.content,
                                        onMentionClick = { username ->
                                            scope.launch {
                                                val u = viewModel.getUserByUsername(username)
                                                if (u != null) {
                                                    navController.navigate("profile/${u.id}")
                                                }
                                            }
                                        },
                                        onLinkClick = { url ->
                                            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                            navController.navigate("browser/$encodedUrl")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Sticky reply composer at the bottom with Markdown toggle, preview and voice recorder!
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                var isRecordingMode by remember { mutableStateOf(false) }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        isRecordingMode = true
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Autorisez le micro pour enregistrer un message vocal",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val onStartRecording = {
                    val permission = android.Manifest.permission.RECORD_AUDIO
                    val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        isRecordingMode = true
                    } else {
                        micPermissionLauncher.launch(permission)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isRecordingMode) {
                        VoiceRecorderUI(
                            onCancel = { isRecordingMode = false },
                            onSendVoiceFile = { file ->
                                val bytes = file.readBytes()
                                val audioB64 = "base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                viewModel.commentActfile(actfileId, audioB64)
                                isRecordingMode = false
                            },
                            onSendVoice = { voiceContent ->
                                viewModel.commentActfile(actfileId, voiceContent)
                                isRecordingMode = false
                            }
                        )
                    } else {
                        if (smartReplies.isNotEmpty() && replyText.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                smartReplies.forEach { reply ->
                                    SuggestionChip(
                                        onClick = { replyText = reply },
                                        label = { Text(reply, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                        if (isPreviewMode) {
                            // Compact Markdown Preview Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Aperçu de votre réponse",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { isPreviewMode = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Éditer",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (replyText.isBlank()) {
                                        Text(
                                            text = "Rien à prévisualiser...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        MarkdownActfile(
                                            content = replyText,
                                            onLinkClick = { url ->
                                                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                                navController.navigate("browser/$encodedUrl")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Compact input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle preview mode icon button
                            IconButton(
                                onClick = { isPreviewMode = !isPreviewMode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = "Aperçu",
                                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Rounded text input field
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 100.dp),
                                placeholder = { Text("Votre réponse...") },
                                shape = RoundedCornerShape(20.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                singleLine = false,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Action buttons: Mic and Send
                            if (replyText.isBlank()) {
                                IconButton(
                                    onClick = { onStartRecording() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Message vocal",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { onStartRecording() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Message vocal",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        viewModel.commentActfile(actfileId, replyText)
                                        replyText = ""
                                        isPreviewMode = false
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Envoyer",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
