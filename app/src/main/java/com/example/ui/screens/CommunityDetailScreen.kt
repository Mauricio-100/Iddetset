package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ActfileWithUser
import com.example.data.Channel
import com.example.data.ChannelMessage
import com.example.data.Community
import com.example.data.getCategoryDefaultIcon
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import com.example.utils.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    slug: String,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }
    var showComposerDialog by remember { mutableStateOf(false) }
    
    // Fetch community details
    val communityState by remember(slug, refreshTrigger) {
        viewModel.getCommunityFlow(slug)
    }.collectAsState(initial = null)
    val community = communityState
    
    // Fetch community channels
    val channelsState by remember(slug, refreshTrigger) {
        viewModel.getCommunityChannelsFlow(slug)
    }.collectAsState(initial = emptyList())
    val channels = channelsState

    // Tabs: 0 = Feed (Reddit style), 1 = Salons (Channels), 2 = À propos, 3 = Modération
    var selectedTab by remember { mutableStateOf(0) }
    
    // Feed Sort: "hot", "new", "top"
    var feedSort by remember { mutableStateOf("hot") }
    
    // Fetch community posts
    val communityPostsState by remember(slug, refreshTrigger) {
        viewModel.getCommunityPostsFlow(slug)
    }.collectAsState(initial = emptyList())
    
    val sortedPosts = remember(communityPostsState, feedSort) {
        when (feedSort) {
            "new" -> communityPostsState.sortedByDescending { it.createdAt }
            "top" -> communityPostsState.sortedByDescending { it.likesCount }
            else -> communityPostsState.sortedByDescending { it.likesCount * 3 + it.commentsCount * 2 + (it.viewsCount / 10) }
        }
    }

    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("community_rules", android.content.Context.MODE_PRIVATE) }
    val defaultRules = "1. Respectez les autres membres de la communauté.\n2. Pas de spam, de contenu offensant ou hors-sujet.\n3. Partagez des publications constructives et de qualité.\n4. Utilisez les salons dédiés pour les discussions thématiques."
    var communityRules by remember(slug) {
        mutableStateOf(prefs.getString(slug, defaultRules) ?: defaultRules)
    }
    
    val currentUserState by viewModel.currentUser.collectAsState()
    val isAdmin = community?.myRole == "admin" || community?.creatorId == currentUserState?.id
    val isModeratorOrAdmin = isAdmin || community?.myRole == "moderator"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (community != null) "c/${community.slug}" else "Communauté",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("top_share_community_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Partager")
                    }
                    if (isModeratorOrAdmin) {
                        IconButton(
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.testTag("mod_hub_button")
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Modération",
                                tint = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isAdmin) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && community != null) {
                FloatingActionButton(
                    onClick = { showComposerDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("publish_in_community_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publier")
                }
            } else if (selectedTab == 1 && isModeratorOrAdmin) {
                FloatingActionButton(
                    onClick = { showCreateChannelDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouveau salon")
                }
            }
        }
    ) { paddingValues ->
        if (community == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val com = community
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header (Reddit Banner + Avatar + Details + Metrics)
                item {
                    CommunityDashboardHeader(
                        community = com,
                        onJoinClick = {
                            viewModel.toggleCommunityJoin(com.slug) { success ->
                                if (success) {
                                    refreshTrigger++
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (com.isMember) "Vous avez quitté c/${com.slug}" else "Vous avez rejoint c/${com.slug}"
                                        )
                                    }
                                }
                            }
                        },
                        onRulesClick = { showRulesDialog = true },
                        onShareClick = { showShareDialog = true },
                        onNewPostClick = { showComposerDialog = true },
                        onEditIconClick = if (isAdmin) { { showEditDialog = true } } else null
                    )
                }

                // Reddit Navigation Tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Publications", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Salons", fontWeight = FontWeight.Bold)
                                    if (channels.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge { Text("${channels.size}") }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("À propos & Règles", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        if (isModeratorOrAdmin) {
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("Modération", fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // TAB 0: REDDIT-STYLE FEED
                if (selectedTab == 0) {
                    // Reddit Sort Selector Bar (Hot, New, Top)
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "hot" to "🔥 Populaire",
                                        "new" to "✨ Nouveau",
                                        "top" to "🏆 Top"
                                    ).forEach { (key, label) ->
                                        val isSelected = feedSort == key
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { feedSort = key },
                                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "${sortedPosts.size} post${if (sortedPosts.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (sortedPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.Forum,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Aucune publication pour l'instant dans c/${com.slug}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Soyez le premier à lancer une discussion !",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showComposerDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Créer une publication")
                                    }
                                }
                            }
                        }
                    } else {
                        items(sortedPosts, key = { it.id }) { actfile ->
                            val isMine = actfile.userId == currentUserState?.id
                            RedditPostCard(
                                actfile = actfile,
                                communitySlug = com.slug,
                                onLike = { viewModel.likeActfile(it) },
                                onView = { viewModel.incrementView(it) },
                                onUserClick = { userId ->
                                    val currentUserId = viewModel.currentUser.value?.id
                                    if (userId == currentUserId) {
                                        navController.navigate("profile")
                                    } else {
                                        navController.navigate("profile/$userId")
                                    }
                                },
                                onComment = { actfileId ->
                                    navController.navigate("discussion/$actfileId")
                                },
                                onDelete = if (isMine || isModeratorOrAdmin) { { viewModel.deleteActfile(actfile.id) } } else null,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
                                onMentionClick = { username ->
                                    coroutineScope.launch {
                                        val u = viewModel.getUserByUsername(username)
                                        if (u != null) {
                                            navController.navigate("profile/${u.id}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // TAB 1: PRIVATE CHANNELS
                else if (selectedTab == 1) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Salons de discussion privés",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Les messages envoyés ici sont privés et réservés aux membres du salon.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (channels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aucun salon disponible",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(channels, key = { it.id }) { channel ->
                            ChannelListItem(
                                channel = channel,
                                isLocked = com.isPrivate && !com.isMember,
                                onClick = {
                                    if (com.isPrivate && !com.isMember) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Rejoignez cette communauté pour accéder à ce salon privé.")
                                        }
                                    } else {
                                        selectedChannel = channel
                                    }
                                }
                            )
                        }
                    }
                }

                // TAB 2: ABOUT & RULES (REDDIT SIDEBAR STYLE)
                else if (selectedTab == 2) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // About Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "À propos de la communauté",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                text = "c/${com.slug}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    if (!com.description.isNullOrBlank()) {
                                        Text(
                                            text = com.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Catégorie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(com.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Confidentialité", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (com.isPrivate) "Privée 🔒" else "Publique 🌍", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Mon Statut", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (com.isMember) (if (com.myRole == "admin") "Admin 👑" else "Membre ⭐") else "Non-membre",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (com.isMember) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Membres", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(FormatUtils.formatCount(com.membersCount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Column {
                                            Text("Publications", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(FormatUtils.formatCount(com.postsCount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Column {
                                            Text("Salons", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${channels.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }

                            // Rules Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Règles officielles",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (isAdmin) {
                                            TextButton(onClick = { showRulesDialog = true }) {
                                                Text("Modifier")
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Text(
                                        text = communityRules,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Moderation shortcut card
                            if (isModeratorOrAdmin) {
                                Card(
                                    onClick = { selectedTab = 3 },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Espace Modération & Bots", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Gérer les membres, bots et filtres", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: MODERATION
                else if (selectedTab == 3) {
                    item {
                        CommunityModerationTabContent(
                            community = com,
                            viewModel = viewModel,
                            onShowSnackbar = { msg: String ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Dedicated Private Channel Chat Room
        selectedChannel?.let { activeChannel ->
            ChannelChatRoomDialog(
                channel = activeChannel,
                community = community,
                viewModel = viewModel,
                onDismiss = { selectedChannel = null }
            )
        }

        // Full-featured standard Actfile Composer with community preset
        if (showComposerDialog && community != null) {
            ActfileComposerScreen(
                viewModel = viewModel,
                initialCommunity = community,
                initialCategory = community.category,
                onDismiss = { showComposerDialog = false },
                onPublish = { content, tags, category, postAsIddet, attachedSound, selectedComm ->
                    showComposerDialog = false
                    val targetComm = selectedComm ?: community
                    val taggedContent = if (!content.contains("@c/${targetComm.slug}")) {
                        "$content\n\n@c/${targetComm.slug}"
                    } else content
                    viewModel.publishActfile(
                        content = taggedContent,
                        tags = tags,
                        category = category ?: targetComm.category,
                        communityId = targetComm.slug,
                        channelId = null,
                        postAsIddet = postAsIddet,
                        soundId = attachedSound?.id,
                        soundTitle = attachedSound?.title,
                        soundAuthor = attachedSound?.artist,
                        soundAudioUrl = attachedSound?.audioUrl,
                        soundCoverUrl = attachedSound?.albumArt,
                        communityName = targetComm.name,
                        communityIconUrl = targetComm.iconUrl
                    )
                    refreshTrigger++
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Publication ajoutée à c/${targetComm.slug} !")
                    }
                }
            )
        }

        if (showRulesDialog && community != null) {
            RulesDialog(
                rules = communityRules,
                isAdmin = isAdmin,
                onDismiss = { showRulesDialog = false },
                onSaveRules = { newRules ->
                    communityRules = newRules
                    prefs.edit().putString(slug, newRules).apply()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Règles de la communauté mises à jour !")
                    }
                }
            )
        }

        if (showCreateChannelDialog && community != null) {
            CreateChannelDialog(
                onDismiss = { showCreateChannelDialog = false },
                onConfirm = { name, description ->
                    showCreateChannelDialog = false
                    viewModel.createChannel(slug, name, description) { created ->
                        if (created != null) {
                            refreshTrigger++
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Le salon #${created.name} a été créé !")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Erreur lors de la création du salon.")
                            }
                        }
                    }
                }
            )
        }

        if (showEditDialog && community != null) {
            val categories = listOf("Fun", "Amour", "Motivation", "Tech", "Sport", "Musique", "Actu", "Business", "Spiritualité", "Autres")
            EditCommunityDialog(
                community = community,
                categories = categories,
                onDismiss = { showEditDialog = false },
                onSave = { name, desc, cat, priv, presetIconUrl ->
                    viewModel.updateCommunity(
                        slug = community.slug,
                        name = name,
                        description = desc,
                        category = cat,
                        isPrivate = priv,
                        iconUrl = presetIconUrl,
                        onResult = { updated ->
                            if (updated != null) {
                                refreshTrigger++
                                showEditDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Communauté mise à jour avec succès !")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Erreur lors de la mise à jour.")
                                }
                            }
                        }
                    )
                },
                onUploadIcon = { file, onComplete ->
                    viewModel.updateCommunityIcon(community.slug, file) { success ->
                        onComplete(success)
                        if (success) {
                            refreshTrigger++
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Photo de profil mise à jour !")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Erreur lors du téléchargement.")
                            }
                        }
                    }
                }
            )
        }

        if (showShareDialog && community != null) {
            val conversations by viewModel.conversations.collectAsState()
            CommunityShareDialog(
                community = community,
                conversations = conversations,
                onDismiss = { showShareDialog = false },
                onSendToConversations = { receiverIds, messageContent ->
                    receiverIds.forEach { receiverId ->
                        viewModel.sendMessage(
                            receiverId = receiverId,
                            content = messageContent
                        )
                    }
                }
            )
        }
    }
}

/**
 * Reddit-style Post Card with karma upvotes/downvotes, flair pills, markdown rendering, and comment counts.
 */
@Composable
fun RedditPostCard(
    actfile: ActfileWithUser,
    communitySlug: String,
    onLike: (String) -> Unit,
    onView: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onComment: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onLinkClick: (String) -> Unit,
    onMentionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val relativeTime = remember(actfile.createdAt) { getRelativeTimeString(actfile.createdAt) }

    LaunchedEffect(actfile.id) {
        onView(actfile.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onComment(actfile.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Author u/username • Time • Category Flair
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onUserClick(actfile.userId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!actfile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = com.example.utils.UrlHelper.fixCloudinaryUrl(actfile.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = actfile.username.firstOrNull()?.uppercase() ?: "U",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "u/${actfile.username}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { onUserClick(actfile.userId) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(userName = actfile.username, isVerified = actfile.isVerified, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Category Flair Pill
                val catInfo = com.example.ui.components.getCategoryById(actfile.category)
                if (catInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catInfo.color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${catInfo.emoji} ${catInfo.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catInfo.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Markdown content
            MarkdownContent(
                content = actfile.content,
                onLinkClick = onLinkClick,
                onMentionClick = onMentionClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reddit Bottom Action Bar: Upvote pill + Comments + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Karma Upvote / Downvote pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (actfile.isLikedByMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { onLike(actfile.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (actfile.isLikedByMe) Icons.Default.ArrowUpward else Icons.Outlined.ArrowUpward,
                                contentDescription = "Upvote",
                                tint = if (actfile.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${actfile.likesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (actfile.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { onLike(actfile.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Downvote",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Comments button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onComment(actfile.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Commentaires",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${actfile.commentsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Share button
                IconButton(
                    onClick = {
                        com.example.utils.ShareHelper.shareActfile(
                            context = context,
                            actfileId = actfile.id,
                            authorUsername = actfile.username,
                            content = actfile.content,
                            category = actfile.category
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Partager",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated, private channel chat room dialog with real-time Room persistence, audio messages, and member avatars.
 * Isolated from the main actfile feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelChatRoomDialog(
    channel: Channel,
    community: Community?,
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Dedicated private channel messages flow from Room
    val messagesState by viewModel.getChannelMessagesFlow(channel.id).collectAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.collectAsState()

    // Auto-scroll to bottom when a new message arrives
    LaunchedEffect(messagesState.size) {
        if (messagesState.isNotEmpty()) {
            listState.animateScrollToItem(messagesState.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Channel
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("# ${channel.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Salon privé",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!channel.description.isNullOrBlank()) {
                                Text(
                                    text = channel.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Messages list
                if (messagesState.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Salon privé #${channel.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Les messages échangés ici sont réservés à ce salon et n'apparaissent pas dans le flux public.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messagesState, key = { it.id }) { msg ->
                            val isMine = msg.senderId == currentUser?.id
                            ChannelMessageBubble(
                                message = msg,
                                isMine = isMine,
                                onDelete = { viewModel.deleteChannelMessage(msg.id) }
                            )
                        }
                    }
                }

                // Chat Input or Read-Only Membership Prompt Area
                val isCommunityMember = community?.isMember == true
                if (!isCommunityMember) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lecture seule 🔒",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Vous devez être membre de c/${community?.slug ?: ""} pour écrire dans ce salon.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (community != null) {
                                        viewModel.toggleCommunityJoin(community.slug) { }
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejoindre", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Écrire dans #${channel.name}...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("channel_message_input"),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Send Button
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            val contentToSend = messageText.trim()
                                            messageText = ""
                                            viewModel.sendChannelMessage(
                                                channelId = channel.id,
                                                communitySlug = community?.slug ?: "",
                                                content = contentToSend,
                                                type = "text"
                                            )
                                        }
                                    },
                                    enabled = messageText.isNotBlank(),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .testTag("send_channel_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Envoyer",
                                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
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

@Composable
fun ChannelMessageBubble(
    message: ChannelMessage,
    isMine: Boolean,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val timeFormatted = remember(message.createdAt) {
        val date = java.util.Date(message.createdAt)
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = !showMenu },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!message.senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = com.example.utils.UrlHelper.fixCloudinaryUrl(message.senderAvatarUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.senderUsername.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMine) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderUsername,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(userName = message.senderUsername, isVerified = message.isVerified, modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                ),
                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isVoiceMessage(message.content)) {
                        VoiceMessagePlayer(content = message.content, isMine = isMine)
                    } else {
                        MarkdownActfile(
                            content = message.content,
                            isMine = isMine,
                            onLinkClick = { url ->
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            },
                            onMentionClick = { }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (isMine) {
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("channel_item_${channel.slug}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Tag,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "# ${channel.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (!channel.description.isNullOrBlank()) {
                    Text(
                        text = channel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isLocked) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesDialog(
    rules: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSaveRules: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedRules by remember { mutableStateOf(rules) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Règles de la communauté",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (isEditing) {
                    OutlinedTextField(
                        value = editedRules,
                        onValueChange = { editedRules = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Entrez les règles de la communauté...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = rules,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdmin) {
                        if (isEditing) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("Annuler")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                onSaveRules(editedRules)
                                isEditing = false
                            }) {
                                Text("Enregistrer")
                            }
                        } else {
                            TextButton(onClick = { isEditing = true }) {
                                Text("Modifier")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onDismiss) {
                                Text("Fermer")
                            }
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text("Fermer")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Créer un nouveau salon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du salon") },
                    placeholder = { Text("ex: actualites") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optionnel)") },
                    placeholder = { Text("De quoi parle ce salon...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(name, description) },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Créer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommunityDialog(
    community: Community,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, String?) -> Unit,
    onUploadIcon: (java.io.File, (Boolean) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(community.name) }
    var description by remember { mutableStateOf(community.description ?: "") }
    var selectedCategory by remember { mutableStateOf(community.category) }
    var isPrivate by remember { mutableStateOf(community.isPrivate) }
    var customIconUrl by remember { mutableStateOf(community.iconUrl ?: "") }
    var isUploading by remember { mutableStateOf(false) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val presetIcons = remember {
        listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200",
            "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=200",
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
            "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=200",
            "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200"
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = uriToTempFile(context, it)
            if (file != null) {
                isUploading = true
                onUploadIcon(file) { success ->
                    isUploading = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifier la communauté",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Community Profile Picture (Icon) Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Icône / Photo de profil",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayUrl = if (customIconUrl.isNotBlank()) customIconUrl else getCategoryDefaultIcon(selectedCategory)
                            AsyncImage(
                                model = displayUrl,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = !isUploading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isUploading) "Téléchargement..." else "Importer une photo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Preset Avatars Selection
                    Text(
                        text = "Ou choisir un avatar thématique :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        presetIcons.forEach { iconUrl ->
                            val isSelected = customIconUrl == iconUrl
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        customIconUrl = iconUrl
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Direct Image URL Field
                    OutlinedTextField(
                        value = customIconUrl,
                        onValueChange = { customIconUrl = it },
                        label = { Text("Lien direct de l'icône (URL)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("https://...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                // Community Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nom de la communauté",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }

                // Category dropdown picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Catégorie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, color = MaterialTheme.colorScheme.onSurface)
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Privacy Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Communauté privée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Seuls les membres peuvent accéder aux salons.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSave(name, description, selectedCategory, isPrivate, customIconUrl.ifBlank { null })
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun uriToTempFile(context: android.content.Context, uri: android.net.Uri): java.io.File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = java.io.File.createTempFile("community_icon_", ".jpg", context.cacheDir)
        tempFile.deleteOnExit()
        val outputStream = java.io.FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
