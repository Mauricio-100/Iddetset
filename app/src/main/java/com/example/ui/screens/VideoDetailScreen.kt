package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.ShareEntityType
import com.example.ui.components.SharePayload
import com.example.ui.components.UniversalShareModal
import com.example.ui.components.VerificationBadge
import com.example.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val videos by viewModel.realVideoFeed.collectAsState()
    val video = remember(videos, videoId) {
        videos.find { it.id == videoId }
    }

    var showShareModal by remember { mutableStateOf(false) }
    var isLiked by remember(video) { mutableStateOf(video?.liked ?: false) }
    var likesCount by remember(video) { mutableIntStateOf(video?.likes ?: 0) }

    LaunchedEffect(videoId) {
        if (videos.isEmpty()) {
            viewModel.loadVideoFeed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video?.username?.let { "@$it" } ?: "Vidéo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareModal = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (video == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Video Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    ActfileVideoPlayer(
                        videoUrl = video.video_url,
                        modifier = Modifier.fillMaxSize(),
                        title = video.description,
                        autoPlay = true,
                        lazyLoad = false
                    )
                }

                // Creator & Action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = video.avatar_url ?: "https://picsum.photos/seed/${video.user_id}/100",
                        contentDescription = "Avatar de ${video.username}",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (video.user_id.isNotBlank()) {
                                    navController.navigate("profile/${video.user_id}")
                                }
                            },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = video.username.ifBlank { "Utilisateur" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (video.is_verified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadge(
                                    modifier = Modifier.size(16.dp),
                                    isVerified = true
                                )
                            }
                        }
                        video.created_at?.let { dateStr ->
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Like Button
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            likesCount = if (isLiked) likesCount + 1 else (likesCount - 1).coerceAtLeast(0)
                            viewModel.likeVideo(videoId)
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "J'aime",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = FormatUtils.formatCount(likesCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share button
                    IconButton(onClick = { showShareModal = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                // Description
                if (!video.description.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${FormatUtils.formatCount(video.views)} vues",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareModal && video != null) {
        val conversations by viewModel.conversations.collectAsState()
        UniversalShareModal(
            payload = SharePayload(
                type = ShareEntityType.VIDEO,
                idOrSlug = video.id,
                title = video.description ?: "Vidéo de ${video.username}",
                subtitle = "@${video.username}",
                imageUrl = video.thumbnail_url ?: video.avatar_url
            ),
            conversations = conversations,
            onDismiss = { showShareModal = false },
            onSendToConversation = { receiverId, msg ->
                viewModel.sendMessage(receiverId, msg)
                Toast.makeText(context, "Partagé dans la conversation !", Toast.LENGTH_SHORT).show()
                showShareModal = false
            }
        )
    }
}
