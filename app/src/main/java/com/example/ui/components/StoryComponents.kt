package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Story
import com.example.data.StoryUser
import com.example.ui.IddetViewModel
import com.example.utils.UrlHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoriesBar(
    viewModel: IddetViewModel,
    stories: List<Story>,
    modifier: Modifier = Modifier,
    onStoryClick: (Story) -> Unit,
    onCreateClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    val distinctUserStories = remember(stories) {
        stories.groupBy { it.user.id }.map { it.value.first() }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // WhatsApp Style "Mon Statut" / Create Story item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCreateClick() }
                    .testTag("create_story_card")
            ) {
                Box(
                    modifier = Modifier.size(58.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentUser?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = UrlHelper.fixCloudinaryUrl(currentUser?.avatarUrl),
                            contentDescription = "Mon statut",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser?.username?.take(1)?.uppercase() ?: "+",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Plus icon badge in bottom right (WhatsApp style)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF25D366), // WhatsApp Green
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter statut",
                            tint = Color.White,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mon statut",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // WhatsApp / Instagram style Contact Stories
        items(distinctUserStories, key = { it.user.id }) { story ->
            val storyUser = story.user
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStoryClick(story) }
                    .testTag("story_card_${story.id}")
            ) {
                // Status ring around avatar
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .border(
                            width = 2.5.dp,
                            color = Color(0xFF25D366), // WhatsApp Green ring
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!storyUser.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = UrlHelper.fixCloudinaryUrl(storyUser.avatarUrl),
                            contentDescription = "Statut de ${storyUser.username}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = storyUser.username.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = storyUser.username,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 64.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Clean, minimalist WhatsApp-style full-screen Story Viewer
 */
@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int = 0,
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories[currentIndex]
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isMyStory = currentUser?.id == currentStory.user.id || currentUser?.username == currentStory.user.username

    // 5 seconds standard story duration (or video length)
    val storyDurationMs = if (currentStory.mediaType == "video") 10000L else 5000L
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val startProgress = progress.value
            val remainingMs = ((1f - startProgress) * storyDurationMs).toLong().coerceAtLeast(100L)
            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingMs.toInt(),
                    easing = LinearEasing
                )
            )
            if (result.endReason == AnimationEndReason.Finished) {
                if (currentIndex < stories.size - 1) {
                    progress.snapTo(0f)
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    LaunchedEffect(currentStory.id) {
        viewModel.trackStoryView(currentStory.id, currentStory.views)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Left tap -> Previous story
                                if (currentIndex > 0) {
                                    coroutineScope.launch {
                                        progress.snapTo(0f)
                                        currentIndex--
                                    }
                                } else {
                                    coroutineScope.launch { progress.snapTo(0f) }
                                }
                            } else {
                                // Right tap -> Next story
                                if (currentIndex < stories.size - 1) {
                                    coroutineScope.launch {
                                        progress.snapTo(0f)
                                        currentIndex++
                                    }
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Full-screen Media Content (Video or Image)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (currentStory.mediaType == "video") {
                    ActfileVideoPlayer(
                        videoUrl = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl) ?: currentStory.mediaUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl),
                        contentDescription = "Statut",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top gradient overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )

            // Header & WhatsApp Style Progress Bars
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f),
                        )
                    }
                }

                // Header: Back Arrow, Avatar, Username, Time & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentStory.user.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = UrlHelper.fixCloudinaryUrl(currentStory.user.avatarUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentStory.user.username.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentStory.user.username,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadge(
                                    userName = currentStory.user.username,
                                    isVerified = currentStory.user.isVerified,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "Aujourd'hui",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Delete button if my story
                    if (isMyStory) {
                        IconButton(
                            onClick = {
                                isPaused = true
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier.testTag("delete_story_button")
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Supprimer",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Gradient & WhatsApp Style Reply Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick Emoji reactions (WhatsApp style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "😂", "😮", "😢", "🙏", "👏").forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable {
                                        val storyUrl = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl) ?: currentStory.mediaUrl
                                        viewModel.recordStoryReaction(currentStory.id, emoji, currentStory.reactions)
                                        viewModel.sendStoryReaction(
                                            receiverId = currentStory.user.id,
                                            storyMediaUrl = storyUrl,
                                            storyAuthorUsername = currentStory.user.username,
                                            reaction = emoji
                                        ) {
                                            Toast.makeText(context, "Réaction envoyée !", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentColor = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Reply Text Field & Send Button (WhatsApp style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { 
                                replyText = it
                                if (it.isNotEmpty()) isPaused = true
                            },
                            placeholder = {
                                Text(
                                    "Répondre à ${currentStory.user.username}...",
                                    color = Color.White.copy(alpha = 0.65f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) isPaused = true
                                }
                                .testTag("story_reply_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true
                        )

                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val textToSend = replyText.trim()
                                    replyText = ""
                                    val storyUrl = UrlHelper.fixCloudinaryUrl(currentStory.mediaUrl) ?: currentStory.mediaUrl
                                    
                                    viewModel.sendStoryReply(
                                        receiverId = currentStory.user.id,
                                        replyText = textToSend,
                                        storyMediaUrl = storyUrl,
                                        storyAuthorUsername = currentStory.user.username
                                    ) {
                                        isPaused = false
                                        Toast.makeText(context, "Réponse envoyée !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)) // WhatsApp Green
                                    .testTag("send_story_reply_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Envoyer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    isPaused = false
                },
                title = { Text("Supprimer cette mise à jour de statut ?") },
                text = { Text("Elle sera supprimée pour tous vos contacts.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteStory(currentStory.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Statut supprimé", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        isPaused = false
                    }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

/**
 * Clean WhatsApp-style Status Creator Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorDialog(
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by viewModel.isUploadingStory.collectAsState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isUploading) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ajouter à mon statut",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { if (!isUploading) onDismiss() },
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            // Media Selection & Preview Box
            Card(
                onClick = {
                    if (!isUploading) {
                        mediaPickerLauncher.launch("*/*")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("story_media_picker_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selectedMediaUri != null) {
                        AsyncImage(
                            model = selectedMediaUri,
                            contentDescription = "Aperçu du statut",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Changer", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF25D366).copy(alpha = 0.15f),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Choisir une photo ou vidéo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Partagez un instant avec vos contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Uploading progress bar
            if (isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF25D366)
                    )
                    Text(
                        text = "Envoi du statut...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF25D366),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Publish Button
            Button(
                onClick = {
                    val uri = selectedMediaUri
                    if (uri != null) {
                        viewModel.createStory(
                            context = context,
                            uri = uri,
                            effect = null
                        ) { success, errorMsg ->
                            if (success) {
                                Toast.makeText(context, "Statut partagé !", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, errorMsg ?: "Échec de l'envoi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Veuillez choisir une photo ou vidéo", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedMediaUri != null && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_story_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366), // WhatsApp Green
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publier le statut",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
