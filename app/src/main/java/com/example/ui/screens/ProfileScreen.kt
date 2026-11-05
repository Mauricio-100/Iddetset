package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import com.example.ui.components.PromoteActfileDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.UserProfileNetwork
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: IddetViewModel, navController: NavController) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val user = currentUser!!

    val userActfiles by viewModel.getUserActfiles(user.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val friends by viewModel.friendsLocations.collectAsStateWithLifecycle()
    val currentUserVibe by viewModel.currentUserVibe.collectAsStateWithLifecycle()
    val myLevel by viewModel.myLevel.collectAsStateWithLifecycle()
    val levelsTable by viewModel.levelsTable.collectAsStateWithLifecycle()
    val iddetPlusStatus by viewModel.myIddetPlusStatus.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    val unreadNotifsCount = notifications.count { !it.isRead }
    val totalLikes = userActfiles.sumOf { it.likesCount }
    val totalViews = userActfiles.sumOf { it.viewsCount }

    // Helper to extract first image URL from actfile content
    fun extractFirstMediaUrl(content: String): String? {
        val regex = Regex("""!\[.*?\]\((https?://[^\s)]+)\)|(https?://[^\s)]+\.(?:jpg|jpeg|png|webp|gif))""", RegexOption.IGNORE_CASE)
        val match = regex.find(content) ?: return null
        val g1 = match.groupValues.getOrNull(1)
        val g2 = match.groupValues.getOrNull(2)
        return when {
            !g1.isNullOrBlank() -> g1
            !g2.isNullOrBlank() -> g2
            else -> null
        }
    }

    // Media posts (with images) for 3x3 Instagram grid
    val mediaActfiles = remember(userActfiles) {
        userActfiles.filter { extractFirstMediaUrl(it.content) != null }
    }

    // Wings (sound and video) posts
    val wingsActfiles = remember(userActfiles) {
        userActfiles.filter { actfile ->
            !actfile.soundAudioUrl.isNullOrBlank() ||
            !actfile.soundId.isNullOrBlank() ||
            actfile.content.contains(".mp4", ignoreCase = true) ||
            actfile.content.contains(".webm", ignoreCase = true) ||
            actfile.content.contains("<video", ignoreCase = true) ||
            actfile.content.contains("youtube", ignoreCase = true) ||
            actfile.content.contains("vimeo", ignoreCase = true) ||
            actfile.category in listOf("wings", "musique", "multimedia", "sound", "video")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
        viewModel.refreshActfiles()
        viewModel.refreshMyLevel()
        viewModel.loadLevelsTable()
        viewModel.loadIddetPlusData()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var actfileToPromote by remember { mutableStateOf<com.example.data.ActfileWithUser?>(null) }
    var isPromotingLoading by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showImageOptions by remember { mutableStateOf(false) }
    var showFullScreenAvatar by remember { mutableStateOf(false) }
    var showVibeEditorDialog by remember { mutableStateOf(false) }
    var showBadgesDialog by remember { mutableStateOf(false) }
    var showLevelsLadderDialog by remember { mutableStateOf(false) }
    var showFollowListSheet by remember { mutableStateOf(false) }
    var followListInitialTab by remember { mutableIntStateOf(0) }
    var showCategorySelectorDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSharePortfolioModal by remember { mutableStateOf(false) }
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var localPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    fun compressAndResizeImage(context: Context, uri: Uri): File? {
        return try {
            val maxDimension = 800
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null

            var inSampleSize = 1
            while (originalWidth / (inSampleSize * 2) >= maxDimension || originalHeight / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val width = bitmap.width
            val height = bitmap.height
            val (newWidth, newHeight) = if (width > height) {
                val ratio = width.toFloat() / height.toFloat()
                if (width > maxDimension) Pair(maxDimension, (maxDimension / ratio).toInt()) else Pair(width, height)
            } else {
                val ratio = height.toFloat() / width.toFloat()
                if (height > maxDimension) Pair((maxDimension / ratio).toInt(), maxDimension) else Pair(width, height)
            }

            val scaledBitmap = if (newWidth != width || newHeight != height) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else bitmap

            val file = File(context.cacheDir, "compressed_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outStream ->
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outStream)
                outStream.flush()
            }
            scaledBitmap.recycle()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun onImageSelected(uri: Uri) {
        localPreviewUri = uri
        isUploading = true
        scope.launch {
            try {
                val compressedFile = compressAndResizeImage(context, uri)
                if (compressedFile == null) {
                    Toast.makeText(context, "Erreur lors de la compression de l'image", Toast.LENGTH_SHORT).show()
                    isUploading = false
                    localPreviewUri = null
                    return@launch
                }

                viewModel.updateProfileWithImage(
                    avatarFile = compressedFile,
                    bio = user.bio,
                    phoneNumber = user.phoneNumber,
                    onSuccess = {
                        isUploading = false
                        localPreviewUri = null
                        Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        isUploading = false
                        localPreviewUri = null
                        Toast.makeText(context, "Échec de l'upload : $error", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                isUploading = false
                localPreviewUri = null
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onImageSelected(uri) }
    )

    fun createImageUri(): Uri {
        val directory = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("profile_", ".jpg", directory)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success && capturedImageUri != null) onImageSelected(capturedImageUri!!) }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri()
                capturedImageUri = uri
                try {
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Caméra non disponible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.username,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(
                            userName = user.username,
                            isVerified = user.isVerified,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    // Notifications
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifsCount > 0) {
                                    Badge { Text(unreadNotifsCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }

                    // Share Profile
                    IconButton(
                        onClick = { showSharePortfolioModal = true }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Partager mon portfolio")
                    }

                    // Refresh
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.refreshProfile()
                                viewModel.refreshActfiles()
                                delay(400)
                                isRefreshing = false
                                Toast.makeText(context, "Profil actualisé", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                        }
                    }

                    // Settings
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Paramètres")
                    }

                    // Logout
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Se déconnecter",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("profile_scrollable_feed"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Item: Banner + Floating Avatar + Identity + Actions + Highlights + Tabs
            item(key = "profile_header_container") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Modern Multi-Color Gradient Cover Banner (Twitter / Facebook style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        Color(0xFF4F46E5), // Indigo
                                        Color(0xFF06B6D4), // Cyan
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            )
                    ) {
                        // Status badge on the cover
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (iddetPlusStatus?.is_iddet_plus == true) "VIP ELITE" else "CONNECTÉ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Overlapping Avatar & Primary Action Buttons Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Glowing VIP / Story Avatar
                        val isVip = iddetPlusStatus?.is_iddet_plus == true
                        val ringBrush = if (isVip) {
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFFEC4899),
                                    Color(0xFF8B5CF6),
                                    Color(0xFF06B6D4),
                                    Color(0xFFF59E0B)
                                )
                            )
                        } else {
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .offset(y = (-46).dp)
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(ringBrush)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showImageOptions = true }
                                .testTag("profile_avatar"),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageModel = localPreviewUri ?: user.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) }
                            if (imageModel != null && (imageModel is Uri || (imageModel is String && imageModel.isNotBlank()))) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Photo de profil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            } else {
                                // Camera icon badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Icon(
                                        Icons.Outlined.PhotoCamera,
                                        contentDescription = "Modifier photo",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        // Right Action Buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showEditDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modifier", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showSharePortfolioModal = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Partager", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    // User Identity & Information
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-24).dp)
                    ) {
                        // Display Name + Verified Badge + Level Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            VerificationBadge(
                                userName = user.username,
                                isVerified = user.isVerified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UserLevelBadge(
                                level = myLevel,
                                onClick = { showLevelsLadderDialog = true }
                            )
                        }

                        // Username tag & Copyable ID
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CopyableUserId(
                                id = user.id,
                                isBot = false,
                                fontSize = 11.sp,
                                iconSize = 12.dp
                            )
                        }

                        // Viber / Instagram Style Status Bubble
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            onClick = { showVibeEditorDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TagFaces,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentUserVibe.text.ifBlank { "Définir un statut ou une humeur..." },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Modifier statut",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Bio
                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            MarkdownActfile(
                                content = user.bio,
                                modifier = Modifier.fillMaxWidth(),
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                }
                            )
                        }

                        // Zodiac Sign & Origin Highlight Card
                        if (!user.zodiacSign.isNullOrBlank() || !user.country.isNullOrBlank() || !user.birthDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ZodiacProfileHighlightCard(
                                zodiacSign = user.zodiacSign,
                                country = user.country,
                                birthDate = user.birthDate
                            )
                        }

                        // Verification Prompt if unverified
                        if (!user.isVerified) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                onClick = { showVerificationDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Demander la certification officielle IDDET",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Metric Stats Row (Instagram / Twitter style)
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Posts Count
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = 0 }
                                ) {
                                    Text(
                                        text = userActfiles.size.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Publications",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Followers Count
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            followListInitialTab = 1
                                            showFollowListSheet = true
                                        }
                                ) {
                                    Text(
                                        text = com.example.utils.FormatUtils.formatCount(user.followersCount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Abonnés",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Following Count
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            followListInitialTab = 0
                                            showFollowListSheet = true
                                        }
                                ) {
                                    Text(
                                        text = com.example.utils.FormatUtils.formatCount(user.followingCount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Abonnements",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                 // Total Likes
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = com.example.utils.FormatUtils.formatCount(totalLikes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFEF4444)
                                    )
                                    Text(
                                        text = "J'aime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Total Views (Vues Totales)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = com.example.utils.FormatUtils.formatCount(totalViews),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0284C7)
                                    )
                                    Text(
                                        text = "Vues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Instagram-Style Story Highlights Bar
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Highlight 1: VIP Club
                            HighlightCircleItem(
                                icon = Icons.Outlined.WorkspacePremium,
                                label = "VIP Club",
                                color = Color(0xFFF59E0B),
                                onClick = { navController.navigate("iddet_plus") }
                            )

                            // Highlight: IDDET Ads Manager
                            HighlightCircleItem(
                                icon = Icons.Outlined.Campaign,
                                label = "IDDET Ads",
                                color = com.example.ui.components.IddetAdsGold,
                                onClick = { navController.navigate("iddet_ads") }
                            )

                            // Highlight 2: Trophies & Badges
                            HighlightCircleItem(
                                icon = Icons.Outlined.EmojiEvents,
                                label = "Trophées",
                                color = Color(0xFF8B5CF6),
                                onClick = { showBadgesDialog = true }
                            )

                            // Highlight 3: Friends Radar Map
                            HighlightCircleItem(
                                icon = Icons.Outlined.Radar,
                                label = "Radar Potes",
                                color = Color(0xFF06B6D4),
                                badgeText = "${friends.count { it.isOnline }}",
                                onClick = { navController.navigate("friends_map") }
                            )

                            // Highlight 4: Level & XP
                            HighlightCircleItem(
                                icon = Icons.Outlined.Leaderboard,
                                label = "Niveau XP",
                                color = Color(0xFF10B981),
                                onClick = { showLevelsLadderDialog = true }
                            )

                            // Highlight 5: IDDET Support
                            HighlightCircleItem(
                                icon = Icons.Outlined.SupportAgent,
                                label = "Support",
                                color = Color(0xFFEC4899),
                                onClick = { navController.navigate("chat/support") }
                            )
                        }

                        // Multi-Tab Selector (Publications, Médias, Wings, Niveau, Infos)
                        Spacer(modifier = Modifier.height(18.dp))
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Publications", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                icon = { Icon(Icons.Outlined.Feed, contentDescription = "Publications", modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Médias", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                icon = { Icon(Icons.Outlined.GridOn, contentDescription = "Médias", modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Wings 🎵", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                icon = { Icon(Icons.Outlined.GraphicEq, contentDescription = "Wings", modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("Niveau", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                icon = { Icon(Icons.Outlined.MilitaryTech, contentDescription = "Niveau", modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                text = { Text("Infos", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                icon = { Icon(Icons.Outlined.Info, contentDescription = "Infos", modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            // Tab Content 0: Vertical Feed of Publications (Actfiles)
            if (selectedTab == 0) {
                if (userActfiles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.PostAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune publication pour le moment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Partagez vos pensées et actfiles avec la communauté !",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(userActfiles, key = { it.id }) { actfile ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(it) },
                                onView = { viewModel.incrementView(it) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
                                onUserClick = {},
                                onComment = { navController.navigate("discussion/$it") },
                                onDelete = { viewModel.deleteActfile(it) },
                                onPromote = { actfileToPromote = it },
                                onMentionClick = { username ->
                                    scope.launch {
                                        val u = viewModel.getUserByUsername(username)
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                },
                                onCommunityClick = { slug ->
                                    navController.navigate("community/$slug")
                                }
                            )
                        }
                    }
                }
            }

            // Tab Content 1: Instagram-Style 3x3 Media Grid
            if (selectedTab == 1) {
                if (mediaActfiles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune photo ou vidéo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Vos publications avec images ou vidéos apparaîtront dans cette galerie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        // Chunk into rows of 3 items
                        val rows = mediaActfiles.chunked(3)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowItems.forEach { actfile ->
                                        val imgUrl = extractFirstMediaUrl(actfile.content)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable {
                                                    navController.navigate("discussion/${actfile.id}")
                                                }
                                        ) {
                                            if (!imgUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = com.example.utils.UrlHelper.fixCloudinaryUrl(imgUrl),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            // Likes overlay in bottom corner
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Favorite,
                                                        contentDescription = null,
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = actfile.likesCount.toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Fill empty slots in row
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content 2: Wings (Sounds & Vidéos)
            if (selectedTab == 2) {
                if (wingsActfiles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun Wings (Sound / Vidéo) publié",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Les publications contenant des sons, musiques ou vidéos apparaîtront dans cette section Wings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(wingsActfiles, key = { "wing_${it.id}" }) { actfile ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(it) },
                                onView = { viewModel.incrementView(it) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
                                onUserClick = {},
                                onComment = { navController.navigate("discussion/$it") },
                                onDelete = { viewModel.deleteActfile(it) },
                                onPromote = { actfileToPromote = it },
                                onMentionClick = { username ->
                                    scope.launch {
                                        val u = viewModel.getUserByUsername(username)
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                },
                                onCommunityClick = { slug ->
                                    navController.navigate("community/$slug")
                                }
                            )
                        }
                    }
                }
            }

            // Tab Content 3: Gamified Level, Streak & Trophies Showcase
            if (selectedTab == 3) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Level Progress Card
                        UserLevelCard(
                            level = myLevel,
                            onOpenLadder = { showLevelsLadderDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Badges & Trophies Showcase Card
                        Surface(
                            onClick = { showBadgesDialog = true },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Trophées & Succès",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "6 badges débloqués",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TrophyMiniItem(Icons.Outlined.Star, "Pionnier", Color(0xFFF59E0B))
                                    TrophyMiniItem(Icons.Outlined.EditNote, "Rédacteur", Color(0xFF3B82F6))
                                    TrophyMiniItem(Icons.Outlined.LocalFireDepartment, "Streak", Color(0xFFEF4444))
                                    TrophyMiniItem(Icons.Outlined.Groups, "Connecté", Color(0xFF10B981))
                                    TrophyMiniItem(Icons.Outlined.Bolt, "Super Dev", Color(0xFF8B5CF6))
                                }
                            }
                        }

                        // Iddet Plus VIP Status Card
                        IddetPlusPromoBanner(
                            onClick = { navController.navigate("iddet_plus") },
                            modifier = Modifier.fillMaxWidth(),
                            isPremium = iddetPlusStatus?.is_iddet_plus == true
                        )
                    }
                }
            }

            // Tab Content 4: About & Contact Details Card (Facebook / Viber Info style)
            if (selectedTab == 4) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Category of Interest Card
                        val currentPrefCategory = user.preferredCategory ?: "@(fun)"
                        val categoryInfo = getCategoryById(currentPrefCategory)

                        Surface(
                            onClick = { showCategorySelectorDialog = true },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, (categoryInfo?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background((categoryInfo?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Category,
                                        contentDescription = null,
                                        tint = categoryInfo?.color ?: MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Catégorie Principale",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${categoryInfo?.name ?: "Fun"} ($currentPrefCategory)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = categoryInfo?.color ?: MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Personal Contact Info Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Informations Personnelles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                InfoRowItem(
                                    icon = Icons.Outlined.Email,
                                    label = "Adresse Email",
                                    value = user.email.ifNullOrBlank("Non renseignée")
                                )

                                InfoRowItem(
                                    icon = Icons.Outlined.Phone,
                                    label = "Téléphone",
                                    value = user.phoneNumber.ifNullOrBlank("Non renseigné")
                                )

                                InfoRowItem(
                                    icon = Icons.Outlined.Cake,
                                    label = "Date de Naissance",
                                    value = user.birthDate.ifNullOrBlank("Non renseignée")
                                )

                                if (!user.zodiacSign.isNullOrBlank()) {
                                    InfoRowItem(
                                        icon = Icons.Outlined.AutoAwesome,
                                        label = "Signe Astrologique",
                                        value = user.zodiacSign!!
                                    )
                                }

                                InfoRowItem(
                                    icon = if (user.privacySetting == "Private") Icons.Outlined.Lock else Icons.Outlined.Public,
                                    label = "Visibilité du Profil",
                                    value = if (user.privacySetting == "Private") "Profil Privé" else "Profil Public"
                                )
                            }
                        }

                        // IDDET Support Concierge Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Support & Assistance Directe",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SupportButton(
                                        name = "Crislem",
                                        color = Color(0xFFFACC15),
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("chat/crislem") }
                                    )
                                    SupportButton(
                                        name = "Doffranel",
                                        color = Color(0xFF06B6D4),
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("chat/doffranel") }
                                    )
                                    SupportButton(
                                        name = "C.M.O",
                                        color = Color(0xFF8B5CF6),
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("chat/c.m.o") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs & BottomSheets ---

    // 1. Edit Profile Dialog
    if (showEditDialog) {
        var editedUsername by remember { mutableStateOf(user.username) }
        var editedBio by remember { mutableStateOf(user.bio) }
        var editedAvatarUrl by remember { mutableStateOf(user.avatarUrl ?: "") }
        var editedPrivacy by remember { mutableStateOf(user.privacySetting) }
        var editedEmail by remember { mutableStateOf(user.email ?: "") }
        var editedPhone by remember { mutableStateOf(user.phoneNumber ?: "") }
        var editedCountry by remember { mutableStateOf(user.country ?: "") }
        var editedBirthDate by remember { mutableStateOf(user.birthDate ?: "") }

        val dynamicZodiacSign = remember(editedBirthDate) {
            ZodiacHelper.parseFromDateString(editedBirthDate)
        }
        val dynamicZodiacInfo = remember(dynamicZodiacSign) {
            dynamicZodiacSign?.let { ZodiacHelper.getInfo(it) }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Modifier le Profil",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { editedUsername = it },
                        label = { Text("Nom d'utilisateur") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editedBio,
                        onValueChange = { editedBio = it },
                        label = { Text("Bio") },
                        leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Country / Pays field + Quick Suggestions
                    OutlinedTextField(
                        value = editedCountry,
                        onValueChange = { editedCountry = it },
                        label = { Text("Pays / Origine") },
                        leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                        placeholder = { Text("ex: RDC, France, Canada...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Country Chips
                    val suggestedCountries = listOf("🇨🇩 RDC", "🇨🇬 Congo", "🇫🇷 France", "🇧🇪 Belgique", "🇨🇦 Canada", "🇨🇮 Côte d'Ivoire", "🇸🇳 Sénégal", "🇨🇲 Cameroun", "🇨🇭 Suisse", "🇺🇸 USA")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedCountries.forEach { c ->
                            val cleanName = c.substringAfter(" ")
                            FilterChip(
                                selected = editedCountry.equals(cleanName, ignoreCase = true) || editedCountry.equals(c, ignoreCase = true),
                                onClick = { editedCountry = cleanName },
                                label = { Text(c, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Birth date field
                    OutlinedTextField(
                        value = editedBirthDate,
                        onValueChange = { editedBirthDate = it },
                        label = { Text("Date de naissance (JJ/MM/AAAA)") },
                        leadingIcon = { Icon(Icons.Outlined.Cake, contentDescription = null) },
                        placeholder = { Text("15/08/1998") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Live Zodiac Sign Preview Card
                    if (dynamicZodiacInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = dynamicZodiacInfo.primaryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, dynamicZodiacInfo.primaryColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(dynamicZodiacInfo.primaryColor)
                                        .padding(5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZodiacVectorIcon(
                                        sign = dynamicZodiacInfo.sign,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Signe : ${dynamicZodiacInfo.sign}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${dynamicZodiacInfo.element})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = dynamicZodiacInfo.elementColor
                                        )
                                    }
                                    Text(
                                        text = dynamicZodiacInfo.dateRange,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editedEmail,
                        onValueChange = { editedEmail = it },
                        label = { Text("Adresse Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it },
                        label = { Text("Téléphone") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Visibilité du compte",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { editedPrivacy = "Public" }
                        ) {
                            RadioButton(
                                selected = editedPrivacy == "Public",
                                onClick = { editedPrivacy = "Public" }
                            )
                            Text("Public")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { editedPrivacy = "Private" }
                        ) {
                            RadioButton(
                                selected = editedPrivacy == "Private",
                                onClick = { editedPrivacy = "Private" }
                            )
                            Text("Privé")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calculatedZodiac = dynamicZodiacSign ?: ZodiacHelper.parseFromDateString(editedBirthDate)

                        viewModel.updateProfile(
                            username = editedUsername,
                            avatarUrl = editedAvatarUrl,
                            bio = editedBio,
                            privacySetting = editedPrivacy,
                            email = editedEmail,
                            phoneNumber = editedPhone,
                            birthDate = editedBirthDate,
                            zodiacSign = calculatedZodiac,
                            country = editedCountry
                        )
                        Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 2. Official Verification Dialog
    if (showVerificationDialog) {
        var isVerifying by remember { mutableStateOf(false) }
        var verificationSuccess by remember { mutableStateOf(false) }

        val isLevelMet = user.level >= 2
        val isBioMet = user.bio.isNotBlank()
        val allCriteriaMet = isLevelMet && isBioMet

        AlertDialog(
            onDismissRequest = { if (!isVerifying) showVerificationDialog = false },
            title = {
                Text(
                    text = "Vérification Officielle IDDET",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!verificationSuccess) {
                        Text(
                            text = "Obtenez le badge d'authenticité rouge officiel IDDET pour prouver votre identité.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Criterion 1: Level 2+
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLevelMet) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isLevelMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Niveau d'activité (Niveau 2+)",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Votre niveau : ${user.level} (Requis : 2)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLevelMet) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Criterion 2: Bio completed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBioMet) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isBioMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bio de profil complétée",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isBioMet) "Votre bio est renseignée" else "Veuillez renseigner votre bio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBioMet) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(0xFFDC2626).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Félicitations !",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Votre profil est maintenant vérifié avec le badge officiel IDDET.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!verificationSuccess) {
                    Button(
                        onClick = {
                            isVerifying = true
                            scope.launch {
                                delay(1200)
                                viewModel.verifyCurrentUser()
                                isVerifying = false
                                verificationSuccess = true
                            }
                        },
                        enabled = allCriteriaMet && !isVerifying,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Activer la vérification")
                        }
                    }
                } else {
                    Button(
                        onClick = { showVerificationDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Terminé")
                    }
                }
            },
            dismissButton = {
                if (!verificationSuccess && !isVerifying) {
                    TextButton(onClick = { showVerificationDialog = false }) {
                        Text("Fermer")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 3. Photo Options Bottom Sheet
    if (showImageOptions) {
        ModalBottomSheet(
            onDismissRequest = { showImageOptions = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Photo de profil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text("Prendre une photo", fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showImageOptions = false
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = createImageUri()
                            capturedImageUri = uri
                            try {
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Caméra non disponible", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Choisir depuis la galerie", fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        try {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Galerie non disponible", Toast.LENGTH_SHORT).show()
                        }
                        showImageOptions = false
                    }
                )

                if (!user.avatarUrl.isNullOrBlank()) {
                    ListItem(
                        headlineContent = { Text("Voir en grand format", fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Outlined.Visibility, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showFullScreenAvatar = true
                            showImageOptions = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Supprimer la photo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                        leadingContent = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.updateProfile(avatarUrl = "", bio = user.bio, privacySetting = user.privacySetting)
                            showImageOptions = false
                        }
                    )
                }
            }
        }
    }

    // 4. Full Screen Avatar Dialog
    if (showFullScreenAvatar && !user.avatarUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showFullScreenAvatar = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullScreenAvatar = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = user.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                    contentDescription = "Photo de profil agrandie",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // 5. Category Selector Dialog
    if (showCategorySelectorDialog) {
        val categories = APP_CATEGORIES
        val currentPrefCategory = user.preferredCategory ?: "@(fun)"

        AlertDialog(
            onDismissRequest = { showCategorySelectorDialog = false },
            title = {
                Text(
                    text = "Choisir ma catégorie d'intérêt",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxHeight(0.65f)) {
                    Text(
                        text = "Sélectionnez votre centre d'intérêt principal pour adapter votre fil d'actualité.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat.id.equals(currentPrefCategory, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateProfile(
                                            bio = user.bio,
                                            privacySetting = user.privacySetting,
                                            preferredCategory = cat.id
                                        )
                                        showCategorySelectorDialog = false
                                        Toast.makeText(context, "Catégorie mise à jour : ${cat.name}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(cat.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Category,
                                            contentDescription = null,
                                            tint = cat.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${cat.name} (${cat.id})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = cat.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategorySelectorDialog = false }) {
                    Text("Fermer")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 6. Vibe Status Editor Dialog
    if (showVibeEditorDialog) {
        var vibeText by remember { mutableStateOf(currentUserVibe.text) }
        AlertDialog(
            onDismissRequest = { showVibeEditorDialog = false },
            title = {
                Text(
                    text = "Définir mon statut",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Exprimez votre humeur du moment ou ce que vous faites.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vibeText,
                        onValueChange = { vibeText = it },
                        label = { Text("Mon statut") },
                        placeholder = { Text("Ex: Disponible, En train de coder...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserVibe("", vibeText, currentUserVibe.activityType)
                        showVibeEditorDialog = false
                        Toast.makeText(context, "Statut mis à jour", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVibeEditorDialog = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 7. Badges & Trophies Dialog
    if (showBadgesDialog) {
        FriendlyBadgesDialog(onDismiss = { showBadgesDialog = false })
    }

    // 8. Levels Ladder Dialog
    if (showLevelsLadderDialog) {
        LevelsLadderDialog(
            currentLevel = myLevel,
            table = levelsTable,
            onDismiss = { showLevelsLadderDialog = false }
        )
    }

    // 9. Follow List Bottom Sheet
    if (showFollowListSheet) {
        FollowListBottomSheet(
            viewModel = viewModel,
            navController = navController,
            userId = user.id,
            initialTab = followListInitialTab,
            onDismissRequest = { showFollowListSheet = false }
        )
    }

    // 10. Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(8.dp),
            title = {
                Text(
                    text = "Se déconnecter",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Voulez-vous vraiment vous déconnecter de votre compte @${user.username} ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Déconnexion", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // 11. Universal Portfolio Share Modal
    if (showSharePortfolioModal) {
        val totalActs = userActfiles.size
        val totalVideos = userActfiles.count { it.content.contains(".mp4") }
        UniversalShareModal(
            payload = SharePayload(
                type = ShareEntityType.PROFILE,
                idOrSlug = user.username,
                title = user.username,
                subtitle = "@${user.username}",
                description = user.bio,
                imageUrl = user.avatarUrl,
                isVerified = user.isVerified,
                extraStat = "$totalActs publications · $totalVideos vidéos · ${user.followersCount} abonnés"
            ),
            conversations = conversations,
            onDismiss = { showSharePortfolioModal = false },
            onSendToConversation = { receiverId, msg ->
                viewModel.sendMessage(receiverId, msg)
            }
        )
    }

    actfileToPromote?.let { targetActfile ->
        PromoteActfileDialog(
            actfile = targetActfile,
            isLoading = isPromotingLoading,
            onDismiss = { actfileToPromote = null },
            onPromote = { budget, currency, daily, targetCountry ->
                isPromotingLoading = true
                viewModel.promoteActfile(
                    actfileId = targetActfile.id,
                    budget = budget,
                    currency = currency,
                    daily = daily,
                    targetCountry = targetCountry,
                    onSuccess = { checkoutUrl ->
                        isPromotingLoading = false
                        actfileToPromote = null
                        Toast.makeText(context, "Campagne publicitaire IDDET Ads créée !", Toast.LENGTH_SHORT).show()
                        if (!checkoutUrl.isNullOrBlank()) {
                            val encoded = java.net.URLEncoder.encode(checkoutUrl, "UTF-8")
                            navController.navigate("browser/$encoded")
                        }
                    },
                    onError = { err ->
                        isPromotingLoading = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onCancelAd = {
                isPromotingLoading = true
                viewModel.cancelActfileAd(targetActfile.id) { success ->
                    isPromotingLoading = false
                    actfileToPromote = null
                    if (success) {
                        Toast.makeText(context, "Sponsorisation retirée", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Impossible d'annuler la sponsorisation", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// --- High Quality Profile UI Components ---

@Composable
fun HighlightCircleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.25f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            if (badgeText != null) {
                Surface(
                    color = Color(0xFF10B981),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun TrophyMiniItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FriendlyBadgesDialog(onDismiss: () -> Unit) {
    val badgesList = listOf(
        Pair("Pionnier IDDET", "Parmi les premiers explorateurs de la plateforme IDDET."),
        Pair("Rédacteur Pro", "A rédigé plus de 10 publications et actfiles de haute qualité."),
        Pair("Streak Master", "Connexion quotidienne continue pendant plus de 5 jours consécutifs."),
        Pair("Ami Connecté", "Actif sur la carte des potes et disponible en direct."),
        Pair("Super Développeur", "Expert en code Markdown, balises enrichies et tech."),
        Pair("Maître du Débat", "Auteur de commentaires pertinents et constructifs.")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trophées & Badges", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(badgesList) { (title, desc) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Fermer")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

fun getZodiacSign(day: Int, month: Int): String {
    return when (month) {
        1 -> if (day < 20) "Capricorne" else "Verseau"
        2 -> if (day < 19) "Verseau" else "Poissons"
        3 -> if (day < 21) "Poissons" else "Bélier"
        4 -> if (day < 20) "Bélier" else "Taureau"
        5 -> if (day < 21) "Taureau" else "Gémeaux"
        6 -> if (day < 21) "Gémeaux" else "Cancer"
        7 -> if (day < 23) "Cancer" else "Lion"
        8 -> if (day < 23) "Lion" else "Vierge"
        9 -> if (day < 23) "Vierge" else "Balance"
        10 -> if (day < 23) "Balance" else "Scorpion"
        11 -> if (day < 22) "Scorpion" else "Sagittaire"
        12 -> if (day < 22) "Sagittaire" else "Capricorne"
        else -> ""
    }
}

@Composable
fun SupportButton(name: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun String?.ifNullOrBlank(default: String): String {
    return if (this.isNullOrBlank()) default else this
}
