package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import com.example.ui.components.PromoteActfileDialog
import com.example.ui.components.IddetAdsGold
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.sp
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.VerificationBadge
import com.example.ui.components.CopyableUserId
import com.example.ui.components.MarkdownEditor
import com.example.ui.components.CommunitySuggestionRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.itemsIndexed
import coil.compose.AsyncImage
import com.example.data.getCategoryDefaultIcon
import com.example.data.getCategoryDefaultBanner
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api

import com.example.ui.components.PersistentSearchBar
import com.example.ui.components.TrendingTopicsSection
import com.example.ui.components.StoriesBar
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.StoryCreatorDialog
import com.example.data.TrendingCategory
import com.example.data.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: IddetViewModel, navController: NavController, onOpenDrawer: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actfiles by viewModel.actfiles.collectAsStateWithLifecycle()
    val followedActfiles by viewModel.followedActfiles.collectAsStateWithLifecycle()
    val recommendedUsers by viewModel.giants.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val preferredCategory = currentUser?.preferredCategory ?: "@(fun)"
    
    val feedTab by viewModel.feedTab.collectAsStateWithLifecycle()
    val showComposer by viewModel.showComposer.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val iddetPlusStatus by viewModel.myIddetPlusStatus.collectAsStateWithLifecycle()
    val sortedStories = remember(stories) { stories.sortedBy { it.user.id } }
    val groupedStories = remember(sortedStories) {
        sortedStories.groupBy { it.user.id }.values.mapNotNull { it.firstOrNull() }
    }
    
    var showStoryViewer by remember { mutableStateOf(false) }
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var showStoryCreator by remember { mutableStateOf(false) }

    val trendingTopics by viewModel.trendingTopics.collectAsStateWithLifecycle()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsStateWithLifecycle()
    val selectedTrendingCategory by viewModel.selectedTrendingCategory.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadCategories()
        viewModel.loadStories()
        viewModel.refreshActfiles()
    }

    var discoverySeed by remember { mutableStateOf((1..100000).random()) }
    var actfileToPromote by remember { mutableStateOf<ActfileWithUser?>(null) }
    var isPromotingLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(feedTab) {
        if (feedTab == 0) {
            discoverySeed = (1..100000).random()
        }
    }

    var suggestedCommunities by remember { mutableStateOf<List<com.example.data.Community>>(emptyList()) }
    LaunchedEffect(currentUser) {
        viewModel.searchCommunitiesFlow(query = null, category = null, sort = "popular")
            .collect { list ->
                suggestedCommunities = list.filter { !it.isMember }.shuffled()
            }
    }
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchActfilesResult by viewModel.searchActfilesResult.collectAsStateWithLifecycle()
    val isFeedLoading by viewModel.isFeedLoading.collectAsStateWithLifecycle()

    val activeActfiles = remember(feedTab, actfiles, followedActfiles, preferredCategory, selectedCategoryFilter, discoverySeed, searchQuery, searchActfilesResult) {
        if (searchQuery.isNotBlank()) {
            return@remember searchActfilesResult
        }
        
        val filteredActfiles = actfiles
        val filteredFollowed = followedActfiles
        
        val baseList = when (feedTab) {
            0 -> {
                val followedIds = filteredFollowed.map { it.userId }.toSet()
                val prefCat = preferredCategory.split(",").map { it.trim().lowercase() }
                filteredActfiles.sortedByDescending { actfile ->
                    var score = (actfile.likesCount * 3 + actfile.commentsCount * 5 + actfile.viewsCount).toDouble()
                    if (actfile.isSponsored) {
                        score += 50000.0 // Priority boost for sponsored IDDET Ads
                    }
                    if (actfile.userId in followedIds) {
                        score += 10000.0 // Huge boost for followed users
                    }
                    val catInfo = com.example.ui.components.getCategoryById(actfile.category)
                    val catName = catInfo?.name?.lowercase() ?: actfile.category?.lowercase() ?: ""
                    if (catName in prefCat) {
                        score *= 1.5 // 50% boost for preferred category
                    }
                    val rnd = java.util.Random(discoverySeed.toLong() + actfile.id.hashCode()).nextDouble()
                    score * (0.5 + rnd) // Random discovery factor
                }
            } // Personnalisé (Pour Toi)
            1 -> filteredFollowed // Abonnements
            2 -> filteredActfiles.sortedByDescending { 
                (it.likesCount + it.commentsCount * 2 + it.viewsCount) + (if (it.isSponsored) 100000 else 0)
            } // Populaires
            3 -> filteredActfiles.shuffled(java.util.Random(discoverySeed.toLong())) // Découverte
            4 -> filteredActfiles.filter { it.isSponsored } // Sponsorisés
            else -> filteredActfiles
        }
        val sortedList = baseList
        
        if (selectedCategoryFilter != null) {
            sortedList.filter { actfile ->
                val actfileCatInfo = com.example.ui.components.getCategoryById(actfile.category)
                val filterCatInfo = com.example.ui.components.getCategoryById(selectedCategoryFilter)
                if (actfileCatInfo != null && filterCatInfo != null) {
                    actfileCatInfo.id == filterCatInfo.id
                } else {
                    actfile.category?.equals(selectedCategoryFilter, ignoreCase = true) == true
                }
            }
        } else {
            sortedList
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        com.example.ui.components.AppDynamicLogo(
                            size = 32.dp,
                            showGlow = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("IDDET", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onOpenDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Ouvrir le menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("iddet_ads") },
                        modifier = Modifier.testTag("ads_manager_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "IDDET Ads Manager",
                            tint = IddetAdsGold
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate("reels") },
                        modifier = Modifier.testTag("reels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = "Reels & Vidéos"
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate("music") },
                        modifier = Modifier.testTag("music_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = "Studio Musique"
                        )
                    }
                    IconButton(
                        onClick = {
                            discoverySeed = (1..100000).random()
                            viewModel.refreshActfiles()
                        },
                        modifier = Modifier.testTag("refresh_discovery_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PersistentSearchBar(viewModel = viewModel)
            
            // Feed Tabs & Active Category Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = feedTab == 0,
                            onClick = { viewModel.setFeedTab(0) },
                            label = { Text("✨ Pour Toi", fontSize = 13.sp, fontWeight = if (feedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = feedTab == 1,
                            onClick = { viewModel.setFeedTab(1) },
                            label = { Text("👥 Abonnements", fontSize = 13.sp, fontWeight = if (feedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = feedTab == 2,
                            onClick = { viewModel.setFeedTab(2) },
                            label = { Text("🔥 Tendances", fontSize = 13.sp, fontWeight = if (feedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = feedTab == 3,
                            onClick = {
                                discoverySeed = (1..100000).random()
                                viewModel.setFeedTab(3)
                            },
                            label = { Text("🎲 Découverte", fontSize = 13.sp, fontWeight = if (feedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = feedTab == 4,
                            onClick = { viewModel.setFeedTab(4) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = if (feedTab == 4) Color.Black else IddetAdsGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📢 Sponsorisés", fontSize = 13.sp, fontWeight = if (feedTab == 4) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IddetAdsGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    if (selectedCategoryFilter != null) {
                        item {
                            InputChip(
                                selected = true,
                                onClick = { viewModel.setSelectedCategoryFilter(null) },
                                label = { Text("Catégorie: $selectedCategoryFilter", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer le filtre",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (isFeedLoading && activeActfiles.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            PullToRefreshBox(
                isRefreshing = isFeedLoading,
                onRefresh = {
                    viewModel.refreshActfiles()
                    viewModel.refreshStories()
                    viewModel.refreshTrendingTopics()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("feed_pull_to_refresh")
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                // Section Stories (Statuts éphémères Markdown / Multimédia)
                item(key = "stories_section_bar") {
                    StoriesBar(
                        viewModel = viewModel,
                        stories = groupedStories,
                        onStoryClick = { story ->
                            selectedStory = story
                            showStoryViewer = true
                        },
                        onCreateClick = {
                            showStoryCreator = true
                        }
                    )
                }

                itemsIndexed(activeActfiles, key = { _, actfile -> actfile.id }) { index, actfile ->
                    val isMine = actfile.userId == currentUser?.id || (currentUser?.username != null && actfile.username.equals(currentUser?.username, ignoreCase = true))
                    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
                    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
                    Column {
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
                            onUserClick = {
                                val currentUserId = viewModel.currentUser.value?.id
                                if (it == currentUserId) {
                                    navController.navigate("profile")
                                } else {
                                    navController.navigate("profile/$it")
                                }
                            },
                            onComment = { actfileId ->
                                navController.navigate("discussion/$actfileId")
                            },
                            onDelete = if (isMine) { { viewModel.deleteActfile(it) } } else null,
                            onPromote = if (isMine || actfile.isSponsored) { { actfileToPromote = it } } else null,
                            onMentionClick = { username ->
                                scope.launch {
                                    val u = viewModel.getUserByUsername(username)
                                    if (u != null) {
                                        navController.navigate("profile/${u.id}")
                                    }
                                }
                            },
                            onCategoryClick = { categoryId ->
                                viewModel.setSelectedCategoryFilter(categoryId)
                            },
                            onCommunityClick = { slug ->
                                navController.navigate("community/$slug")
                            }
                        )

                        // Inject CommunitySuggestionRow (Horizontal scrolling list of communities) after the 2nd post (index 1)
                        if (index == 1 && suggestedCommunities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CommunitySuggestionRow(
                                communities = suggestedCommunities,
                                onJoinToggle = { slug ->
                                    viewModel.toggleCommunityJoin(slug) { success ->
                                        if (success) {
                                            suggestedCommunities = suggestedCommunities.map {
                                                if (it.slug == slug) {
                                                    it.copy(
                                                        isMember = !it.isMember,
                                                        membersCount = if (it.isMember) it.membersCount - 1 else it.membersCount + 1
                                                    )
                                                } else it
                                            }
                                        }
                                    }
                                },
                                onClick = { slug ->
                                    navController.navigate("community/$slug")
                                }
                            )
                        }

                        // Inject Iddet Plus VIP Promotional Banner in the feed after the 4th item
                        if (index == 3) {
                            Spacer(modifier = Modifier.height(16.dp))
                            com.example.ui.components.IddetPlusPromoBanner(
                                onClick = { navController.navigate("iddet_plus") },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                isPremium = iddetPlusStatus?.is_iddet_plus == true
                            )
                        }
                    }
                }

                if (isFeedLoading && activeActfiles.isEmpty()) {
                    items(4) {
                        com.example.ui.components.ShimmerActfileCard()
                    }
                } else if (activeActfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (feedTab == 1) "Aucune publication dans vos abonnements pour le moment." else "Aucune publication disponible.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = if (feedTab == 1) "Découvrez des profils à suivre ci-dessus pour animer votre fil !" else "Tirez vers le bas pour actualiser ou créez votre premier Actfile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
        
        if (showComposer) {
            ActfileComposerScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowComposer(false) },
                onPublish = { content, tags, category, postAsIddet, attachedSound, selectedComm ->
                    viewModel.publishActfile(
                        content = content,
                        tags = tags,
                        category = category,
                        communityId = selectedComm?.id ?: selectedComm?.slug,
                        channelId = null,
                        postAsIddet = postAsIddet,
                        soundId = attachedSound?.id,
                        soundTitle = attachedSound?.title,
                        soundAuthor = attachedSound?.artist,
                        soundAudioUrl = attachedSound?.audioUrl,
                        soundCoverUrl = attachedSound?.albumArt,
                        communityName = selectedComm?.name,
                        communityIconUrl = selectedComm?.iconUrl
                    )
                    viewModel.setShowComposer(false)
                }
            )
        }

        if (showStoryViewer && selectedStory != null) {
            val userStories = sortedStories.filter { it.user.id == selectedStory?.user?.id }
            val startIndex = userStories.indexOfFirst { it.id == selectedStory?.id }.coerceAtLeast(0)
            StoryViewerDialog(
                stories = userStories,
                initialIndex = startIndex,
                viewModel = viewModel,
                onDismiss = {
                    showStoryViewer = false
                    selectedStory = null
                }
            )
        }

        if (showStoryCreator) {
            StoryCreatorDialog(
                viewModel = viewModel,
                onDismiss = { showStoryCreator = false }
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
                            Toast.makeText(context, "Campagne publicitaire IDDET Ads lancée !", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Sponsorisation arrêtée avec succès", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Impossible d'annuler la sponsorisation", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActfileComposer(
    viewModel: com.example.ui.IddetViewModel,
    allCategories: List<String>,
    onDismiss: () -> Unit,
    onPublish: (String, String, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Autres") }
    
    var isAiSuggesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(content) {
        if (content.length > 50 && com.example.utils.LocalAiManager.state.value == com.example.utils.AiModelState.READY) {
            delay(1500) // Debounce
            isAiSuggesting = true
            val suggested = com.example.utils.LocalAiManager.suggestCategory(content)
            if (suggested != null && suggested in allCategories) {
                selectedCategory = suggested
            }
            isAiSuggesting = false
        }
    }
    
    val categoriesToUse = if (allCategories.isNotEmpty()) allCategories else listOf("Fun", "Amour", "Motivation", "Tech", "Sport", "Musique", "Actu", "Business", "Spiritualité", "Autres")
    
    var showCategoryPickerByPublish by remember { mutableStateOf(false) }
    var moderationError by remember { mutableStateOf<String?>(null) }
    var isCheckingModeration by remember { mutableStateOf(false) }
    
    val selectedCatInfoRaw = com.example.ui.components.getCategoryById(selectedCategory)
    val selectedCatInfo = selectedCatInfoRaw ?: com.example.ui.components.CategoryInfo(
        id = selectedCategory,
        name = selectedCategory,
        emoji = "🏷️",
        description = "Catégorie $selectedCategory",
        color = MaterialTheme.colorScheme.primary
    )
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "New Actfile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            MarkdownEditor(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = "Write your markdown actfile here...\n\nHint: Use the toolbar for bold, links, etc."
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category selector block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { showCategoryPickerByPublish = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(selectedCatInfo.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedCatInfo.emoji, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Catégorie de la publication", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedCatInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = selectedCatInfo.color
                    )
                }
                if (isAiSuggesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = "Changer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Community selector block
            val myCommunities by produceState<List<com.example.data.Community>>(initialValue = emptyList()) {
                viewModel.getMyCommunitiesFlow().collect { value = it }
            }
            var selectedCommunity by remember { mutableStateOf<com.example.data.Community?>(null) }
            var showCommunityPicker by remember { mutableStateOf(false) }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { showCommunityPicker = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Communauté", 
                         style = MaterialTheme.typography.labelSmall, 
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedCommunity?.name ?: "Aucune (Public)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = "Changer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showCommunityPicker) {
                AlertDialog(
                    onDismissRequest = { showCommunityPicker = false },
                    title = {
                        Text(
                            text = "Sélectionner une communauté",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        LazyColumn {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedCommunity = null
                                            showCommunityPicker = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aucune (Public)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(myCommunities, key = { it.id }) { com ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedCommunity = com
                                            showCommunityPicker = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = com.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "c/${com.slug}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCommunityPicker = false }) {
                            Text("Fermer")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tags (comma separated, e.g. code, design)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (moderationError != null) {
                Text(
                    text = moderationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        isCheckingModeration = true
                        moderationError = null
                        scope.launch {
                            val isSafe = com.example.utils.LocalAiManager.checkAppropriate(content)
                            if (isSafe) {
                                val finalContent = if (selectedCommunity != null) "$content\n\n@c/${selectedCommunity!!.slug}" else content
                                onPublish(finalContent, tags, selectedCategory)
                            } else {
                                moderationError = "⚠️ S3 AI a détecté que ce contenu pourrait être inapproprié. Veuillez le réviser avant de publier."
                            }
                            isCheckingModeration = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isCheckingModeration
            ) {
                if (isCheckingModeration) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Publish Actfile")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (showCategoryPickerByPublish) {
            AlertDialog(
                onDismissRequest = { showCategoryPickerByPublish = false },
                title = {
                    Text(
                        text = "🏷️ Sélectionner une catégorie",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(categoriesToUse) { catName ->
                                val catInfoRaw = com.example.ui.components.getCategoryById(catName)
                                val catInfo = catInfoRaw ?: com.example.ui.components.CategoryInfo(
                                    id = catName,
                                    name = catName,
                                    emoji = "🏷️",
                                    description = "Catégorie $catName",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val isSelected = catName == selectedCategory
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) catInfo.color.copy(alpha = 0.15f) 
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            selectedCategory = catName
                                            showCategoryPickerByPublish = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(catInfo.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(catInfo.emoji, fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = catInfo.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) catInfo.color else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = catInfo.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                            contentDescription = "Sélectionné",
                                            tint = catInfo.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryPickerByPublish = false }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}

@Composable
fun CommunityList(
    communities: List<com.example.data.Community>,
    onJoinToggle: (String) -> Unit,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Communautés suggérées",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(communities, key = { it.id }) { community ->
                CommunitySuggestionItem(
                    community = community,
                    onJoinToggle = onJoinToggle,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
fun CommunitySuggestionItem(
    community: com.example.data.Community,
    onJoinToggle: (String) -> Unit,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick(community.slug) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val bannerModel = if (!community.bannerUrl.isNullOrBlank()) {
                    com.example.utils.UrlHelper.fixCloudinaryUrl(community.bannerUrl)
                } else {
                    getCategoryDefaultBanner(community.category)
                }
                AsyncImage(
                    model = bannerModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconModel = if (!community.iconUrl.isNullOrBlank()) {
                            com.example.utils.UrlHelper.fixCloudinaryUrl(community.iconUrl)
                        } else {
                            getCategoryDefaultIcon(community.category)
                        }
                        AsyncImage(
                            model = iconModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "c/${community.slug}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (!community.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = community.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${community.membersCount} membres actifs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = { onJoinToggle(community.slug) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Rejoindre",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
