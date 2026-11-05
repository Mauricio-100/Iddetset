package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.VideoFeedItemNetwork
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.VerificationBadge
import com.example.utils.FormatUtils
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsFeedScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val realVideoFeed by viewModel.realVideoFeed.collectAsStateWithLifecycle()
    val isVideoFeedLoading by viewModel.isVideoFeedLoading.collectAsStateWithLifecycle()

    var showPublishSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadVideoFeed()
    }

    val pagerState = rememberPagerState(pageCount = { realVideoFeed.size })

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (realVideoFeed.isEmpty() && !isVideoFeedLoading) {
                // Real server empty state: no mock fallbacks
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Aucun clip vidéo sur le serveur",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Les vidéos publiées par la communauté apparaîtront ici en temps réel.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showPublishSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publier une vidéo 🎥", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadVideoFeed() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualiser le feed")
                        }
                    }
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val videoItem = realVideoFeed[page]
                    val isPageActive = pagerState.currentPage == page

                    LaunchedEffect(isPageActive) {
                        if (isPageActive) {
                            viewModel.recordVideoView(videoItem.id)
                        }
                    }

                    RealReelItemView(
                        video = videoItem,
                        isActive = isPageActive,
                        onLike = { viewModel.likeVideo(videoItem.id) },
                        onShare = {
                            com.example.utils.ShareHelper.shareVideo(
                                context = context,
                                videoId = videoItem.id,
                                authorUsername = videoItem.username,
                                description = videoItem.description
                            )
                        },
                        onUserClick = {
                            if (videoItem.user_id.isNotBlank()) {
                                navController.navigate("profile/${videoItem.user_id}")
                            }
                        },
                        onMusicClick = { navController.navigate("music") }
                    )
                }
            }

            // Top overlay bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                if (isVideoFeedLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Feed Vidéo Réel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "API",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.loadVideoFeed() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.White)
                        }

                        IconButton(
                            onClick = { showPublishSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Publier une vidéo", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet to upload real video to server
    if (showPublishSheet) {
        PublishVideoBottomSheet(
            onDismiss = { showPublishSheet = false },
            onUpload = { videoUri, desc, hasOriginalSound ->
                val videoFile = uriToVideoFile(context, videoUri)
                if (videoFile == null) {
                    Toast.makeText(context, "Fichier vidéo inaccessible", Toast.LENGTH_SHORT).show()
                    return@PublishVideoBottomSheet
                }
                viewModel.uploadVideo(
                    videoFile = videoFile,
                    description = desc,
                    isPublic = true,
                    hasOriginalSound = hasOriginalSound,
                    soundId = null
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        showPublishSheet = false
                    }
                }
            }
        )
    }
}

@Composable
fun RealReelItemView(
    video: VideoFeedItemNetwork,
    isActive: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onUserClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    var isLiked by remember(video.liked) { mutableStateOf(video.liked) }
    var likesCount by remember(video.likes) { mutableIntStateOf(video.likes) }
    var isPlaying by remember { mutableStateOf(true) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isPlaying = !isPlaying },
                    onDoubleTap = {
                        if (!isLiked) {
                            isLiked = true
                            likesCount += 1
                            onLike()
                        }
                        showDoubleTapHeart = true
                    }
                )
            }
    ) {
        // Real server video stream
        if (isActive && video.video_url.isNotBlank()) {
            ActfileVideoPlayer(
                videoUrl = video.video_url,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!video.thumbnail_url.isNullOrBlank()) {
            AsyncImage(
                model = video.thumbnail_url,
                contentDescription = video.description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Dark gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Floating Double Tap Heart Animation
        if (showDoubleTapHeart) {
            LaunchedEffect(Unit) {
                delay(800)
                showDoubleTapHeart = false
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier
                        .size(120.dp)
                        .scale(1.2f)
                )
            }
        }

        // Pause indicator overlay
        if (!isPlaying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lecture",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        // Right Action Bar (TikTok Style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Avatar
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!video.avatar_url.isNullOrBlank()) {
                    AsyncImage(
                        model = video.avatar_url,
                        contentDescription = video.username,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { onUserClick() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onUserClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = video.username.take(1).uppercase().ifBlank { "U" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Like button with real counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        likesCount += if (isLiked) 1 else -1
                        onLike()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = FormatUtils.formatCount(likesCount),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Views counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Vues",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = FormatUtils.formatCount(video.views),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Partager",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Partager",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Rotating Vinyl Sound Disc -> links to Music screen
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .rotate(if (isPlaying) rotationAngle else 0f)
                    .clickable { onMusicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Musique",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        // Bottom Left Info Overlay (Author, Caption, Sound Title)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick() }
            ) {
                Text(
                    text = "@${video.username}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                if (video.is_verified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VerificationBadge(isVerified = true, userName = video.username)
                }
            }

            // Description / Caption
            if (!video.description.isNullOrBlank()) {
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Music track pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onMusicClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Son original • Découvrir sur IDDET 🎶",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishVideoBottomSheet(
    onDismiss: () -> Unit,
    onUpload: (videoUri: Uri, description: String, hasOriginalSound: Boolean) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var hasOriginalSound by remember { mutableStateOf(true) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Publier une vidéo sur le Feed 🎥",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Video selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { videoPicker.launch("video/*") },
                color = if (selectedVideoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedVideoUri != null) Icons.Default.CheckCircle else Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = if (selectedVideoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedVideoUri != null) "Vidéo sélectionnée" else "Choisir un clip vidéo (.mp4)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedVideoUri != null) {
                            Text(
                                text = selectedVideoUri?.lastPathSegment ?: "Fichier vidéo prêt",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Légende / Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Son original",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Activer le son d'origine de la vidéo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hasOriginalSound,
                    onCheckedChange = { hasOriginalSound = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val uri = selectedVideoUri
                    if (uri == null) return@Button
                    isUploading = true
                    onUpload(uri, description, hasOriginalSound)
                },
                enabled = selectedVideoUri != null && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publication sur le serveur...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier la vidéo", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun uriToVideoFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("video_upload_", ".mp4", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}
