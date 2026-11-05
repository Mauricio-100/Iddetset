package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.data.User
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.CopyableUserId
import com.example.ui.components.PersistentSearchBar
import com.example.ui.components.VerificationBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE) }

    var searchHistory by remember {
        mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList())
    }

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val giants by viewModel.giants.collectAsStateWithLifecycle()
    val actfiles by viewModel.actfiles.collectAsStateWithLifecycle()
    val searchUsersResult by viewModel.searchUsersResult.collectAsStateWithLifecycle()
    val searchActfilesResult by viewModel.searchActfilesResult.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Actfiles, 1: Projects/Communities, 2: Users
    var hashtagFilter by remember { mutableStateOf("") }
    var authorFilter by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("Recent") } // Recent, Popular, Oldest

    val communities by viewModel.searchCommunitiesFlow(query, null, if (sortOrder == "Popular") "popular" else "newest")
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Function to add query to search history
    val addToHistory = { q: String ->
        if (q.isNotBlank()) {
            val newHistory = (listOf(q) + searchHistory).distinct().take(10)
            searchHistory = newHistory
            prefs.edit().putStringSet("history", newHistory.toSet()).apply()
        }
    }

    // Filter Actfiles by hashtag & author filter
    val filteredActfiles = remember(searchActfilesResult, hashtagFilter, authorFilter, sortOrder) {
        var list = searchActfilesResult
        if (hashtagFilter.isNotBlank()) {
            val cleanTag = hashtagFilter.removePrefix("#").trim()
            list = list.filter {
                it.tags?.contains(cleanTag, ignoreCase = true) == true ||
                it.content.contains("#$cleanTag", ignoreCase = true)
            }
        }
        if (authorFilter.isNotBlank()) {
            val cleanAuthor = authorFilter.removePrefix("@").trim()
            list = list.filter {
                it.username.contains(cleanAuthor, ignoreCase = true)
            }
        }
        when (sortOrder) {
            "Popular" -> list.sortedByDescending { it.likesCount + it.viewsCount }
            "Oldest" -> list.sortedBy { it.createdAt }
            else -> list.sortedByDescending { it.createdAt }
        }
    }

    val presetHashtags = listOf("#iddet", "#tech", "#ai", "#musique", "#gaming", "#projets", "#wings", "#crypto")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "S-3 Recherche Avancée",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Bar
            PersistentSearchBar(
                viewModel = viewModel,
                onSearch = { addToHistory(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Preset Hashtags Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(presetHashtags) { tag ->
                    val isSelected = hashtagFilter.equals(tag, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            hashtagFilter = if (isSelected) "" else tag
                        },
                        label = { Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sort & Advanced Filter Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sorting Selector Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { sortOrder = "Recent" },
                        label = { Text("Récents", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortOrder == "Recent") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    AssistChip(
                        onClick = { sortOrder = "Popular" },
                        label = { Text("Populaires", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortOrder == "Popular") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    AssistChip(
                        onClick = { sortOrder = "Oldest" },
                        label = { Text("Anciens", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sortOrder == "Oldest") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }

                // Active Filters Clear Button if active
                if (hashtagFilter.isNotBlank() || authorFilter.isNotBlank()) {
                    TextButton(
                        onClick = {
                            hashtagFilter = ""
                            authorFilter = ""
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Effacer filtres", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results Category Tabs (Publications, Projets/Communautés, Utilisateurs)
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
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
                    text = { Text("Projets & Comus", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Outlined.Groups, contentDescription = "Communautés", modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Utilisateurs", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Outlined.PersonSearch, contentDescription = "Utilisateurs", modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (query.isBlank() && hashtagFilter.isBlank() && authorFilter.isBlank()) {
                    // RECENT SEARCH HISTORY
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recherches récentes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = {
                                    searchHistory = emptyList()
                                    prefs.edit().remove("history").apply()
                                }) {
                                    Text("Tout effacer", fontSize = 12.sp)
                                }
                            }
                        }

                        items(searchHistory) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSearchQuery(historyItem)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = historyItem,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val newHistory = searchHistory.filter { it != historyItem }
                                        searchHistory = newHistory
                                        prefs.edit().putStringSet("history", newHistory.toSet()).apply()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    // SUGGESTIONS WHEN QUERY IS EMPTY
                    if (selectedTab == 0) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Publications tendances",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(actfiles.take(10), key = { actfile -> "sug_${actfile.id}" }) { actfile ->
                            val isMine = actfile.userId == currentUser?.id
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(actfile.id) },
                                onView = { viewModel.incrementView(actfile.id) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onUserClick = { navController.navigate("profile/${actfile.userId}") },
                                onComment = { navController.navigate("discussion/${actfile.id}") },
                                onDelete = if (isMine) { { viewModel.deleteActfile(actfile.id) } } else null,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
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
                    } else if (selectedTab == 1) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Communautés populaires",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(communities, key = { "com_${it.id}" }) { community ->
                            CommunityProjectCard(
                                community = community,
                                onClick = { navController.navigate("community/${community.slug}") }
                            )
                        }
                    } else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Comptes recommandés",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(giants, key = { it.id }) { user ->
                            val isFollowing by viewModel.isFollowing(user.id).collectAsStateWithLifecycle(initialValue = false)
                            UserCard(
                                user = user,
                                isFollowing = isFollowing,
                                onFollowClick = {
                                    if (isFollowing) viewModel.unfollowUser(user.id) else viewModel.followUser(user.id)
                                },
                                onClick = { navController.navigate("profile/${user.id}") }
                            )
                        }
                    }
                } else {
                    // SEARCH RESULTS
                    if (selectedTab == 0) {
                        item {
                            Text(
                                text = "${filteredActfiles.size} publication(s) trouvée(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(filteredActfiles, key = { it.id }) { actfile ->
                            val isMine = actfile.userId == currentUser?.id
                            ActfileCard(
                                actfile = actfile,
                                onLike = { viewModel.likeActfile(actfile.id) },
                                onView = { viewModel.incrementView(actfile.id) },
                                targetLanguageName = targetLanguage,
                                isAiReady = aiState == com.example.utils.AiModelState.READY,
                                onUserClick = { navController.navigate("profile/${actfile.userId}") },
                                onComment = { navController.navigate("discussion/${actfile.id}") },
                                onDelete = if (isMine) { { viewModel.deleteActfile(actfile.id) } } else null,
                                onLinkClick = { url ->
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("browser/$encodedUrl")
                                },
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
                        if (filteredActfiles.isEmpty()) {
                            item {
                                EmptySearchResultState("Aucune publication ne correspond à vos critères de recherche.")
                            }
                        }
                    } else if (selectedTab == 1) {
                        item {
                            Text(
                                text = "${communities.size} communauté(s) trouvée(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(communities, key = { "search_com_${it.id}" }) { community ->
                            CommunityProjectCard(
                                community = community,
                                onClick = { navController.navigate("community/${community.slug}") }
                            )
                        }
                        if (communities.isEmpty()) {
                            item {
                                EmptySearchResultState("Aucune communauté ou projet trouvé pour '$query'.")
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "${searchUsersResult.size} utilisateur(s) trouvé(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(searchUsersResult, key = { it.id }) { user ->
                            val isFollowing by viewModel.isFollowing(user.id).collectAsStateWithLifecycle(initialValue = false)
                            UserCard(
                                user = user,
                                isFollowing = isFollowing,
                                onFollowClick = {
                                    if (isFollowing) viewModel.unfollowUser(user.id) else viewModel.followUser(user.id)
                                },
                                onClick = { navController.navigate("profile/${user.id}") }
                            )
                        }
                        if (searchUsersResult.isEmpty()) {
                            item {
                                EmptySearchResultState("Aucun utilisateur trouvé pour '$query'.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchResultState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aucun résultat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CommunityProjectCard(community: Community, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!community.iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = com.example.utils.UrlHelper.fixCloudinaryUrl(community.iconUrl),
                        contentDescription = community.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = community.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (community.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "c/${community.slug}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!community.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = community.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${com.example.utils.FormatUtils.formatCount(community.membersCount)} membres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Outlined.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${com.example.utils.FormatUtils.formatCount(community.postsCount)} posts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Rejoindre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UserCard(user: User, isFollowing: Boolean, onFollowClick: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = com.example.utils.UrlHelper.fixCloudinaryUrl(user.avatarUrl),
                        contentDescription = "Photo de profil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(userName = user.username, isVerified = user.isVerified)
                }
                Spacer(modifier = Modifier.height(2.dp))
                CopyableUserId(
                    id = user.id,
                    isBot = user.username.contains("bot", ignoreCase = true),
                    fontSize = 10.sp,
                    iconSize = 10.dp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${com.example.utils.FormatUtils.formatCount(user.followersCount)} abonnés",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (user.badges.isNotBlank()) {
                    Text(
                        text = "🏅 ${user.badges}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(if (isFollowing) "Abonné" else "Suivre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
