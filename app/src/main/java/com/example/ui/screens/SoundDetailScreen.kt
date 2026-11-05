package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.IddetViewModel
import com.example.ui.components.ShareEntityType
import com.example.ui.components.SharePayload
import com.example.ui.components.UniversalShareModal
import com.example.ui.components.VerificationBadge
import com.example.utils.MusicPlayerManager
import com.example.utils.MusicTrack
import com.example.utils.UrlHelper

data class SoundCreatorInfo(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val isVerified: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundDetailScreen(
    soundId: String,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val sounds by viewModel.sounds.collectAsStateWithLifecycle()

    var detailedSound by remember { mutableStateOf<com.example.data.SoundNetwork?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var creatorInfo by remember { mutableStateOf<SoundCreatorInfo?>(null) }

    LaunchedEffect(soundId) {
        isLoadingDetails = true
        try {
            val net = viewModel.getSoundDetails(soundId)
            if (net != null) {
                detailedSound = net
                val candidateUsername = net.author_username?.takeIf { it.isNotBlank() }
                val candidateId = net.author_id?.takeIf { it.isNotBlank() }
                if (!candidateUsername.isNullOrBlank()) {
                    val u = viewModel.getUserByUsername(candidateUsername)
                    if (u != null) {
                        creatorInfo = SoundCreatorInfo(
                            id = u.id,
                            username = u.username,
                            avatarUrl = u.avatarUrl,
                            isVerified = u.isVerified
                        )
                    }
                }
                if (creatorInfo == null && !candidateId.isNullOrBlank()) {
                    val card = viewModel.getUserCard(candidateId)
                    if (card != null) {
                        creatorInfo = SoundCreatorInfo(
                            id = card.user_id,
                            username = card.username,
                            avatarUrl = card.avatar_url,
                            isVerified = card.is_verified
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingDetails = false
        }
    }

    val cachedTrack = remember(sounds, soundId) { sounds.find { it.id == soundId } }
    val track = remember(cachedTrack, detailedSound, soundId) {
        if (detailedSound != null) {
            val net = detailedSound!!
            val rawTitle = net.title.ifBlank { "Piste Audio" }
            val rawArtist = net.author_username?.ifBlank { null }
                ?: net.author_id?.ifBlank { null }
                ?: "Artiste"
            val fixedAudio = UrlHelper.fixCloudinaryUrl(net.audio_url)?.ifBlank { null }
                ?: net.audio_url?.ifBlank { null }
                ?: "https://hoosthubs-g.onrender.com/api/sounds/$soundId/short/stream"
            val fixedCover = UrlHelper.fixCloudinaryUrl(net.cover_url)?.ifBlank { null }
                ?: net.cover_url?.ifBlank { null }
                ?: ""
            val durationSec = net.duration?.toInt() ?: 0
            val dur = if (durationSec > 0) "%02d:%02d".format(durationSec / 60, durationSec % 60) else "03:15"
            MusicTrack(
                id = soundId,
                title = rawTitle,
                artist = rawArtist,
                albumArt = fixedCover,
                audioUrl = fixedAudio,
                durationFormatted = dur,
                genre = net.category.ifBlank { "Musique" },
                likesCount = net.likes_count
            )
        } else if (cachedTrack != null) {
            cachedTrack
        } else {
            MusicTrack(
                id = soundId,
                title = "Piste Audio",
                artist = "Artiste",
                albumArt = "",
                audioUrl = "https://hoosthubs-g.onrender.com/api/sounds/$soundId/short/stream",
                durationFormatted = "03:15",
                genre = "Musique",
                likesCount = 0
            )
        }
    }

    val isAuthorVerified = detailedSound?.author_is_verified == true || detailedSound?.is_verified == true

    val playerState by MusicPlayerManager.state.collectAsState()
    val isThisTrackPlaying = playerState.isPlaying && playerState.currentTrack?.id == track.id
    val isThisTrackLoaded = playerState.currentTrack?.id == track.id

    val authorUserId = track.artist
    val isFollowingAuthor by viewModel.isFollowing(authorUserId).collectAsStateWithLifecycle(initialValue = false)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSelf = currentUser?.username.equals(track.artist, ignoreCase = true) || currentUser?.id == track.artist

    var isLiked by remember(track.id) { mutableStateOf(false) }
    var likesCount by remember(track.id) { mutableStateOf(track.likesCount) }
    var showShareModal by remember { mutableStateOf(false) }

    // Vinyl animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    fun playOrToggleTrack() {
        if (isThisTrackPlaying) {
            MusicPlayerManager.togglePlayPause()
        } else {
            MusicPlayerManager.playTrack(track)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Détail du son",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Small publish button in top bar as requested
                    FilledTonalButton(
                        onClick = {
                            viewModel.setAttachedComposerSound(track)
                            viewModel.setShowComposer(true)
                            navController.navigate("home")
                            Toast.makeText(context, "Son attaché à votre Actfile !", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showShareModal = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Cover Art / Vinyl presentation
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArt.isNotBlank()) {
                    AsyncImage(
                        model = UrlHelper.fixCloudinaryUrl(track.albumArt),
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(80.dp)
                                .rotate(if (isThisTrackPlaying) rotationAngle else 0f)
                        )
                    }
                }

                // Mini play overlay icon
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { playOrToggleTrack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisTrackPlaying) "Pause" else "Lecture",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Genre Tag
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = track.genre.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mini Profile of Creator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (track.artist.isNotBlank()) {
                            navController.navigate("profile/${track.artist}")
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayCreatorUsername = creatorInfo?.username ?: track.artist.ifEmpty { "créateur" }
                    val displayCreatorAvatar = creatorInfo?.avatarUrl
                    val isDisplayCreatorVerified = creatorInfo?.isVerified == true || isAuthorVerified

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!displayCreatorAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = com.example.utils.UrlHelper.fixCloudinaryUrl(displayCreatorAvatar),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = displayCreatorUsername.take(1).uppercase().ifEmpty { "?" },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@$displayCreatorUsername",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isDisplayCreatorVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerificationBadge(userName = displayCreatorUsername, isVerified = true, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "Créateur original",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isSelf && track.artist.isNotBlank()) {
                        Button(
                            onClick = {
                                if (isFollowingAuthor) viewModel.unfollowUser(authorUserId) else viewModel.followUser(authorUserId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingAuthor) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFollowingAuthor) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFollowingAuthor) "Abonné" else "Suivre",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player Progress Bar & Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentPos = if (isThisTrackLoaded) playerState.currentPositionMs else 0
                    val totalDur = if (isThisTrackLoaded && playerState.durationMs > 0) {
                        playerState.durationMs
                    } else {
                        195000 // default ~3m15s
                    }
                    val sliderVal = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

                    Slider(
                        value = sliderVal,
                        onValueChange = { frac ->
                            if (isThisTrackLoaded) {
                                MusicPlayerManager.seekTo((frac * totalDur).toInt())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPos / 1000),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(totalDur / 1000),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isLiked = !isLiked
                                likesCount = if (isLiked) likesCount + 1 else maxOf(0, likesCount - 1)
                                viewModel.likeSound(track.id)
                            }
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Aimer",
                                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isThisTrackLoaded) {
                                    val newPos = (currentPos - 10000).coerceAtLeast(0)
                                    MusicPlayerManager.seekTo(newPos)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "Reculer de 10s",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FloatingActionButton(
                            onClick = { playOrToggleTrack() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isThisTrackPlaying) "Pause" else "Lecture",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isThisTrackLoaded) {
                                    val newPos = (currentPos + 10000).coerceAtMost(totalDur)
                                    MusicPlayerManager.seekTo(newPos)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "Avancer de 10s",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { showShareModal = true }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Partager",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Use this sound
            Button(
                onClick = {
                    viewModel.setAttachedComposerSound(track)
                    viewModel.setShowComposer(true)
                    navController.navigate("home")
                    Toast.makeText(context, "Son sélectionné pour votre Actfile !", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Utiliser ce son pour un Actfile", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showShareModal) {
        UniversalShareModal(
            payload = SharePayload(
                type = ShareEntityType.SOUND,
                idOrSlug = track.id,
                title = track.title,
                subtitle = "par @${track.artist} • ${track.genre}",
                imageUrl = track.albumArt
            ),
            onDismiss = { showShareModal = false }
        )
    }
}
