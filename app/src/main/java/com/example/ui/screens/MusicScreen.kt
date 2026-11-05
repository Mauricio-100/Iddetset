package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.utils.MusicPlayerManager
import com.example.utils.MusicTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val playerState by MusicPlayerManager.state.collectAsState()
    val apiTracks by viewModel.sounds.collectAsStateWithLifecycle()
    val isSoundsLoading by viewModel.isSoundsLoading.collectAsStateWithLifecycle()
    val isUploadingSound by viewModel.isUploadingSound.collectAsStateWithLifecycle()

    val currentTrack = playerState.currentTrack
    val isPlaying = playerState.isPlaying

    var selectedGenre by remember { mutableStateOf("Tout") }
    var searchQuery by remember { mutableStateOf("") }
    var showFullPlayerModal by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }

    // Categories matching server.py pattern: Afro|Rap|Gospel|Amapiano|Rumba|Électro|Podcast|Autres
    val genres = listOf("Tout", "Afro", "Rap", "Gospel", "Amapiano", "Rumba", "Électro", "Podcast", "Autres")

    LaunchedEffect(selectedGenre) {
        viewModel.loadSounds(selectedGenre)
    }

    val displayTracks = apiTracks

    val filteredTracks = remember(displayTracks, selectedGenre, searchQuery) {
        displayTracks.filter { track ->
            (selectedGenre == "Tout" || track.genre.contains(selectedGenre, ignoreCase = true) || selectedGenre.contains(track.genre, ignoreCase = true)) &&
            (searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) || track.artist.contains(searchQuery, ignoreCase = true))
        }
    }

    // Vinyl rotation animation when music is playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Musique & Sons",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "SERVEUR",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showPublishDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publier", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { viewModel.loadSounds(selectedGenre) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPublishDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Publier un son 🎶", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        bottomBar = {
            if (currentTrack != null) {
                // Spotify-like Mini Player bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showFullPlayerModal = true },
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column {
                        // Progress bar line at top
                        if (playerState.durationMs > 0) {
                            val progress = (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Album artwork
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack.albumArt.isNotBlank()) {
                                    AsyncImage(
                                        model = currentTrack.albumArt,
                                        contentDescription = currentTrack.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentTrack.artist} • ${currentTrack.genre}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { MusicPlayerManager.togglePlayPause() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = { MusicPlayerManager.nextTrack() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Suivant",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Genre selector chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres) { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { selectedGenre = genre },
                            label = { Text(genre, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Rechercher un son, un artiste...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Featured Hero Card (when a track is playing or selected)
            if (currentTrack != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Big Vinyl Disc
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentTrack.albumArt.isNotBlank()) {
                                        AsyncImage(
                                            model = currentTrack.albumArt,
                                            contentDescription = "Pochette",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .rotate(if (isPlaying) rotationAngle else 0f),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.85f))
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPlaying) "EN COURS D'ÉCOUTE 🎧" else "EN PAUSE ⏸️",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentTrack.artist} • ${currentTrack.genre}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                MusicPlayerManager.setFavoriteTrackOnProfile(context, currentTrack)
                                                Toast.makeText(context, "« ${currentTrack.title} » défini comme votre Vibe musicale !", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Vibe Profil", style = MaterialTheme.typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = { navController.navigate("sound/${currentTrack.id}") },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Détail", style = MaterialTheme.typography.labelSmall)
                                        }

                                        IconButton(
                                            onClick = {
                                                com.example.utils.ShareHelper.shareSound(
                                                    context = context,
                                                    soundId = currentTrack.id,
                                                    title = currentTrack.title,
                                                    authorUsername = currentTrack.artist,
                                                    category = currentTrack.genre
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Partager le son",
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

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SONS DU SERVEUR (${filteredTracks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "API HoostHubs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Loading state
            if (isSoundsLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredTracks.isEmpty()) {
                // Real Server Empty State
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucun son sur le serveur",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucune piste musicale n'a encore été publiée dans la catégorie « $selectedGenre » sur l'API.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showPublishDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publier une musique 🎶")
                            }
                        }
                    }
                }
            }

            // Real Server Tracks list
            items(filteredTracks, key = { it.id }) { track ->
                val isTrackPlaying = isPlaying && currentTrack?.id == track.id
                val isTrackSelected = currentTrack?.id == track.id
                val isFavorite = playerState.favoriteTrackId == track.id

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            MusicPlayerManager.playTrack(track)
                            viewModel.recordSoundPlay(track.id)
                        },
                    color = if (isTrackSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album image with play indicator
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.albumArt.isNotBlank()) {
                                AsyncImage(
                                    model = track.albumArt,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isTrackPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isTrackSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = track.genre,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.likeSound(track.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Aimer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate("sound/${track.id}") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Détail du son",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                com.example.utils.ShareHelper.shareSound(
                                    context = context,
                                    soundId = track.id,
                                    title = track.title,
                                    authorUsername = track.artist,
                                    category = track.genre
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Partager",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = track.durationFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    MusicPlayerManager.setFavoriteTrackOnProfile(context, track)
                                    Toast.makeText(context, "Son ajouté à votre profil", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Mettre en son de profil",
                                    tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Modal BottomSheet for Publishing a Sound to the Server
    if (showPublishDialog) {
        PublishSoundBottomSheet(
            onDismiss = { showPublishDialog = false },
            isUploading = isUploadingSound,
            onUpload = { audioUri, coverUri, title, desc, category ->
                val audioFile = uriToFile(context, audioUri, "audio_upload", ".mp3")
                val coverFile = coverUri?.let { uriToFile(context, it, "cover_upload", ".jpg") }
                if (audioFile == null) {
                    Toast.makeText(context, "Impossible de lire le fichier audio sélectionné", Toast.LENGTH_LONG).show()
                    return@PublishSoundBottomSheet
                }
                viewModel.uploadSound(audioFile, coverFile, title, desc, category) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        showPublishDialog = false
                    }
                }
            }
        )
    }

    // Full Screen Player Modal Bottom Sheet
    if (showFullPlayerModal && currentTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showFullPlayerModal = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LECTEUR AUDIO IDDET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentTrack.albumArt.isNotBlank()) {
                        AsyncImage(
                            model = currentTrack.albumArt,
                            contentDescription = currentTrack.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .rotate(if (isPlaying) rotationAngle else 0f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.9f))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${currentTrack.artist} • ${currentTrack.genre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress slider
                val progress = if (playerState.durationMs > 0) {
                    (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = progress,
                    onValueChange = { newProg ->
                        val targetMs = (newProg * playerState.durationMs).toInt()
                        MusicPlayerManager.seekTo(targetMs)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val curSecs = playerState.currentPositionMs / 1000
                    val durSecs = playerState.durationMs / 1000
                    Text(
                        text = String.format("%02d:%02d", curSecs / 60, curSecs % 60),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (durSecs > 0) String.format("%02d:%02d", durSecs / 60, durSecs % 60) else currentTrack.durationFormatted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { MusicPlayerManager.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Aléatoire",
                            tint = if (playerState.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { MusicPlayerManager.previousTrack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Précédent", modifier = Modifier.size(32.dp))
                    }

                    FilledIconButton(
                        onClick = { MusicPlayerManager.togglePlayPause() },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lecture",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { MusicPlayerManager.nextTrack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Suivant", modifier = Modifier.size(32.dp))
                    }

                    IconButton(onClick = { MusicPlayerManager.toggleRepeat() }) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Répéter",
                            tint = if (playerState.isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.utils.ShareHelper.shareSound(
                                context = context,
                                soundId = currentTrack.id,
                                title = currentTrack.title,
                                authorUsername = currentTrack.artist,
                                category = currentTrack.genre
                            )
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager le son", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val link = com.example.utils.ShareHelper.getSoundWebUrl(currentTrack.id)
                            com.example.utils.ShareHelper.copyToClipboard(context, link, "Lien du son copié !")
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copier le lien", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishSoundBottomSheet(
    onDismiss: () -> Unit,
    isUploading: Boolean,
    onUpload: (audioUri: Uri, coverUri: Uri?, title: String, description: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Afro") }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf("Afro", "Rap", "Gospel", "Amapiano", "Rumba", "Électro", "Podcast", "Autres")

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAudioUri = uri
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedCoverUri = uri
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
                text = "Publier une musique sur le serveur 🎶",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Audio picker card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { audioPicker.launch("audio/*") },
                color = if (selectedAudioUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedAudioUri != null) Icons.Default.CheckCircle else Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = if (selectedAudioUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedAudioUri != null) "Fichier audio sélectionné" else "Choisir un fichier audio (MP3, WAV...)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedAudioUri != null) {
                            Text(
                                text = selectedAudioUri?.lastPathSegment ?: "Audio prêt",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cover image picker card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { coverPicker.launch("image/*") },
                color = if (selectedCoverUri != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedCoverUri != null) Icons.Default.CheckCircle else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (selectedCoverUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedCoverUri != null) "Pochette sélectionnée" else "Ajouter une image de pochette (Optionnel)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedCoverUri != null) {
                            Text(
                                text = selectedCoverUri?.lastPathSegment ?: "Image prête",
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de la chanson") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Crédits") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Catégorie musicale",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val audio = selectedAudioUri
                    if (audio == null) return@Button
                    onUpload(audio, selectedCoverUri, title.ifBlank { "Nouveau son" }, description, selectedCategory)
                },
                enabled = selectedAudioUri != null && !isUploading,
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
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier le son sur IDDET", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri, prefix: String, extension: String): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = java.io.File.createTempFile(prefix, extension, context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}
