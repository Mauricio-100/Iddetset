package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import com.example.utils.CallManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(viewModel: IddetViewModel, navController: NavController, userId: String) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val user by viewModel.getUserProfile(userId).collectAsStateWithLifecycle(initialValue = null)
    val effectiveUserId = user?.id ?: userId
    val userActfiles by viewModel.getUserActfiles(effectiveUserId).collectAsStateWithLifecycle(initialValue = emptyList())
    val isFollowing by viewModel.isFollowing(effectiveUserId).collectAsStateWithLifecycle(initialValue = false)
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val friendsLocations by viewModel.friendsLocations.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFullScreenAvatar by remember { mutableStateOf(false) }
    var showFollowListSheet by remember { mutableStateOf(false) }
    var followListInitialTab by remember { mutableIntStateOf(0) }
    var waveSentRecently by remember { mutableStateOf(false) }
    var userLevel by remember { mutableStateOf<com.example.data.UserLevelResponse?>(null) }
    var showLadderDialog by remember { mutableStateOf(false) }
    val levelsTable by viewModel.levelsTable.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

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

    // Media posts for Instagram 3x3 grid
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

    val micCallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val pUser = user ?: return@rememberLauncherForActivityResult
            CallManager.startCall(
                calleeId = pUser.id,
                calleeUsername = pUser.username,
                calleeAvatar = pUser.avatarUrl
            ) { success, msg ->
                if (!success && msg != null) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permission microphone requise pour les appels", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(userId) {
        viewModel.refreshUserProfile(userId)
        viewModel.refreshActfiles()
        viewModel.loadLevelsTable()
        userLevel = viewModel.fetchUserLevel(userId)
    }

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val profileUser = user!!
    val isMe = currentUser?.id == profileUser.id
    val totalLikes = userActfiles.sumOf { it.likesCount }
    val totalViews = userActfiles.sumOf { it.viewsCount }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profileUser.username,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(
                            userName = profileUser.username,
                            isVerified = profileUser.isVerified,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            com.example.utils.ShareHelper.shareUserProfile(
                                context = context,
                                username = profileUser.username,
                                displayName = profileUser.username,
                                bio = profileUser.bio,
                                isVerified = profileUser.isVerified,
                                followersCount = profileUser.followersCount,
                                videosCount = userActfiles.count { it.content.contains(".mp4") },
                                soundsCount = userActfiles.count { it.content.contains(".mp3") || it.content.contains(".m4a") || it.content.contains("voice://") }
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Partager le portfolio")
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
                .testTag("other_profile_scrollable_feed"),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Item: Cover + Avatar + Actions + Identity + Metrics + Tabs
            item(key = "profile_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Modern Gradient Cover Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        Color(0xFF6366F1),
                                        Color(0xFF0EA5E9),
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            )
                    )

                    // Avatar + Primary Action Buttons Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Floating Avatar
                        Box(
                            modifier = Modifier
                                .offset(y = (-46).dp)
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (!profileUser.avatarUrl.isNullOrBlank()) {
                                        showFullScreenAvatar = true
                                    }
                                }
                                .testTag("profile_avatar"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profileUser.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profileUser.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                    contentDescription = "Photo de profil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = profileUser.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
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
                            if (isMe) {
                                Button(
                                    onClick = { navController.navigate("profile") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mon profil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                // Follow / Unfollow Button
                                Button(
                                    onClick = {
                                        if (isFollowing) {
                                            viewModel.unfollowUser(profileUser.id)
                                        } else {
                                            viewModel.followUser(profileUser.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                                    modifier = Modifier.testTag("profile_follow_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFollowing) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFollowing) "Abonné(e)" else "Suivre",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Direct Message Button
                                FilledTonalButton(
                                    onClick = { navController.navigate("chat/${profileUser.id}") },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                    modifier = Modifier.testTag("profile_message_button")
                                ) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Message", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Message", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                // Audio Call Button
                                FilledTonalButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            CallManager.startCall(
                                                calleeId = profileUser.id,
                                                calleeUsername = profileUser.username,
                                                calleeAvatar = profileUser.avatarUrl
                                            ) { success, msg ->
                                                if (!success && msg != null) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            micCallLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                    modifier = Modifier.testTag("profile_call_button")
                                ) {
                                    Icon(Icons.Outlined.Call, contentDescription = "Appeler", modifier = Modifier.size(15.dp), tint = Color(0xFF10B981))
                                }
                            }
                        }
                    }

                    // User Identity & Info
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
                                text = profileUser.username,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            VerificationBadge(
                                modifier = Modifier.size(20.dp),
                                userName = profileUser.username,
                                isVerified = profileUser.isVerified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UserLevelBadge(
                                level = userLevel,
                                onClick = { showLadderDialog = true }
                            )
                        }

                        // Username tag & Copyable ID
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "@${profileUser.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CopyableUserId(
                                id = profileUser.id,
                                isBot = profileUser.username.contains("bot", ignoreCase = true),
                                fontSize = 11.sp,
                                iconSize = 12.dp
                            )
                        }

                        // Bio
                        if (profileUser.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            MarkdownActfile(
                                content = profileUser.bio,
                                modifier = Modifier.fillMaxWidth(),
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                }
                            )
                        }

                        // Zodiac Sign & Origin Highlight Card
                        if (!profileUser.zodiacSign.isNullOrBlank() || !profileUser.country.isNullOrBlank() || !profileUser.birthDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ZodiacProfileHighlightCard(
                                zodiacSign = profileUser.zodiacSign,
                                country = profileUser.country,
                                birthDate = profileUser.birthDate
                            )
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
                                        text = com.example.utils.FormatUtils.formatCount(profileUser.followersCount),
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
                                        text = com.example.utils.FormatUtils.formatCount(profileUser.followingCount),
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

                                // Total Views
                                val totalViews = remember(userActfiles) { userActfiles.sumOf { it.viewsCount } }
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

                        // Live Radar Card if friend is on map
                        val friendOnMap = friendsLocations.find {
                            it.id == profileUser.id || it.username.equals(profileUser.username, ignoreCase = true)
                        }

                        if (friendOnMap != null && !isMe) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(if (friendOnMap.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (friendOnMap.isOnline) "En direct sur le Radar" else "Hors ligne",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0284C7).copy(alpha = 0.3f)
                                        ) {
                                            Text(
                                                text = "${friendOnMap.distanceKm} km",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (friendOnMap.statusMessage.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = friendOnMap.statusMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.sendWaveToFriend(friendOnMap)
                                                waveSentRecently = true
                                                Toast.makeText(
                                                    context,
                                                    "Salutation envoyée à ${friendOnMap.displayName}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (waveSentRecently) Color(0xFF10B981) else Color(0xFF0284C7),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.WavingHand, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (waveSentRecently) "Envoyé !" else "Saluer", fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { navController.navigate("friends_map") },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.Map, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Carte", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Tab Selector
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
                        }
                    }
                }
            }

            // Private Account Restriction
            if (profileUser.privacySetting == "Private" && !isFollowing && !isMe) {
                item(key = "private_account_notice") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ce compte est privé",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Abonnez-vous à @${profileUser.username} pour accéder à ses publications.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                // Tab 0: Publications Feed
                if (selectedTab == 0) {
                    if (userActfiles.isEmpty()) {
                        item(key = "empty_actfiles") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aucune publication pour le moment.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

                // Tab 1: Instagram 3x3 Media Grid
                if (selectedTab == 1) {
                    if (mediaActfiles.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aucune photo ou vidéo disponible.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        item {
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
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Wings (Sounds & Vidéos)
                if (selectedTab == 2) {
                    if (wingsActfiles.isEmpty()) {
                        item(key = "empty_wings") {
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
                                    text = "Les publications contenant des sons, musiques ou vidéos de cet utilisateur apparaîtront ici.",
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

                // Tab 3: Gamified Level & Badges
                if (selectedTab == 3) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            UserLevelCard(
                                level = userLevel,
                                onOpenLadder = { showLadderDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                subtitle = "Score calculé sur ses likes et commentaires"
                            )
                        }
                    }
                }
            }
        }
    }

    // Full screen Avatar Dialog
    if (showFullScreenAvatar && !profileUser.avatarUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showFullScreenAvatar = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullScreenAvatar = false },
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFullScreenAvatar = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fermer", tint = Color.White)
                }

                AsyncImage(
                    model = profileUser.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                    contentDescription = "Photo de profil",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    if (showFollowListSheet) {
        FollowListBottomSheet(
            viewModel = viewModel,
            navController = navController,
            userId = profileUser.id,
            initialTab = followListInitialTab,
            onDismissRequest = { showFollowListSheet = false }
        )
    }

    if (showLadderDialog) {
        LevelsLadderDialog(
            currentLevel = userLevel,
            table = levelsTable,
            onDismiss = { showLadderDialog = false }
        )
    }
}
