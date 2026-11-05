package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.TrendingCategory
import com.example.data.TrendingTopic
import com.example.ui.IddetViewModel
import java.net.URLEncoder

enum class NewsViewMode {
    FEED,
    BOOKMARKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleNewsScreen(
    viewModel: IddetViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val topics by viewModel.trendingTopics.collectAsState()
    val isLoading by viewModel.isTrendingLoading.collectAsState()
    val selectedCategory by viewModel.selectedTrendingCategory.collectAsState()
    val bookmarkedTopics by viewModel.bookmarkedNews.collectAsState()
    val searchQuery by viewModel.newsSearchQuery.collectAsState()

    var viewMode by remember { mutableStateOf(NewsViewMode.FEED) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(searchQuery) }

    // Dialog states
    var summaryTopic by remember { mutableStateOf<TrendingTopic?>(null) }
    var readerTopic by remember { mutableStateOf<TrendingTopic?>(null) }

    // Init bookmarks once
    LaunchedEffect(Unit) {
        viewModel.initBookmarkedNews(context)
        if (topics.isEmpty()) {
            viewModel.loadTrendingTopics(selectedCategory)
        }
    }

    val displayedTopics = when (viewMode) {
        NewsViewMode.FEED -> topics
        NewsViewMode.BOOKMARKS -> bookmarkedTopics
    }

    val quickQueries = listOf(
        "Gemini", "Android 15", "Markdown", "Obsidian",
        "Open Source", "Kotlin", "Cybersécurité", "James Webb"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Google G / News colored badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFFEA4335),
                                            Color(0xFFFBBC05),
                                            Color(0xFF34A853)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google Actualités",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE53935))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = if (viewMode == NewsViewMode.BOOKMARKS) "Mes articles sauvegardés (${bookmarkedTopics.size})" else "Flux en temps réel • Tech & Innovation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("google_news_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    // Toggle live search
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("google_news_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Outlined.Search,
                            contentDescription = "Rechercher",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bookmarks View Toggle with badge
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == NewsViewMode.FEED) NewsViewMode.BOOKMARKS else NewsViewMode.FEED
                        },
                        modifier = Modifier.testTag("google_news_bookmarks_toggle")
                    ) {
                        BadgedBox(
                            badge = {
                                if (bookmarkedTopics.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(bookmarkedTopics.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == NewsViewMode.BOOKMARKS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Favoris",
                                tint = if (viewMode == NewsViewMode.BOOKMARKS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchGoogleNewsQuery(searchQuery, selectedCategory)
                            } else {
                                viewModel.refreshTrendingTopics()
                            }
                            Toast.makeText(context, "Actualisation du flux Google News...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("google_news_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser",
                            tint = MaterialTheme.colorScheme.primary
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            if (it.isBlank()) {
                                viewModel.setNewsSearchQuery("")
                                viewModel.loadTrendingTopics(selectedCategory)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_news_search_input"),
                        placeholder = { Text("Rechercher dans Google Actualités...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchInput.isNotBlank()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    viewModel.setNewsSearchQuery("")
                                    viewModel.loadTrendingTopics(selectedCategory)
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Search Suggestions
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickQueries) { query ->
                            SuggestionChip(
                                onClick = {
                                    searchInput = query
                                    viewModel.searchGoogleNewsQuery(query, selectedCategory)
                                },
                                label = { Text(query, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Categories Filter Bar (only in FEED mode)
            if (viewMode == NewsViewMode.FEED) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TrendingCategory.values()) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectTrendingCategory(category)
                            },
                            label = {
                                Text(
                                    text = "${category.emoji} ${category.label}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("news_category_chip_${category.name}")
                        )
                    }
                }
            } else {
                // Bookmarks Header Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vos articles Google News enregistrés pour lecture ultérieure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // News Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && displayedTopics.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Récupération des flux Google News...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (displayedTopics.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = if (viewMode == NewsViewMode.BOOKMARKS) Icons.Outlined.BookmarkBorder else Icons.Outlined.Article,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (viewMode == NewsViewMode.BOOKMARKS) "Aucun article sauvegardé" else "Aucune actualité trouvée",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (viewMode == NewsViewMode.BOOKMARKS)
                                    "Cliquez sur l'icône signet sur un article pour le retrouver ici facilement."
                                else
                                    "Essayez un autre mot-clé ou sélectionnez une autre catégorie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Breaking News Hero (First item when in feed mode and no search query)
                        if (viewMode == NewsViewMode.FEED && searchQuery.isBlank() && displayedTopics.isNotEmpty()) {
                            val heroTopic = displayedTopics.first()
                            val isBookmarked = viewModel.isNewsBookmarked(heroTopic.id)

                            item(key = "hero_breaking_news_${heroTopic.id}") {
                                GoogleNewsHeroCard(
                                    topic = heroTopic,
                                    isBookmarked = isBookmarked,
                                    onOpen = {
                                        openArticle(heroTopic.link, navController)
                                    },
                                    onReaderMode = {
                                        readerTopic = heroTopic
                                    },
                                    onSummary = {
                                        summaryTopic = heroTopic
                                    },
                                    onDebate = {
                                        createDebatePost(heroTopic, viewModel, navController)
                                    },
                                    onBookmark = {
                                        viewModel.toggleBookmarkNews(heroTopic, context)
                                        Toast.makeText(
                                            context,
                                            if (isBookmarked) "Article retiré des signets" else "Article sauvegardé !",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onShare = {
                                        shareArticle(context, heroTopic)
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Section title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "DERNIÈRES DÉPÊCHES (${displayedTopics.size - 1})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Temps réel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Remaining articles
                            val subList = displayedTopics.drop(1)
                            itemsIndexed(subList, key = { _, item -> item.id }) { index, topic ->
                                val isSaved = viewModel.isNewsBookmarked(topic.id)
                                GoogleNewsArticleCard(
                                    topic = topic,
                                    rank = index + 2,
                                    isBookmarked = isSaved,
                                    onOpen = { openArticle(topic.link, navController) },
                                    onReaderMode = { readerTopic = topic },
                                    onSummary = { summaryTopic = topic },
                                    onDebate = { createDebatePost(topic, viewModel, navController) },
                                    onBookmark = {
                                        viewModel.toggleBookmarkNews(topic, context)
                                        Toast.makeText(
                                            context,
                                            if (isSaved) "Article retiré des signets" else "Article sauvegardé !",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onShare = { shareArticle(context, topic) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else {
                            // Search or Bookmarks mode: list all items directly
                            itemsIndexed(displayedTopics, key = { _, item -> item.id }) { index, topic ->
                                val isSaved = viewModel.isNewsBookmarked(topic.id)
                                GoogleNewsArticleCard(
                                    topic = topic,
                                    rank = index + 1,
                                    isBookmarked = isSaved,
                                    onOpen = { openArticle(topic.link, navController) },
                                    onReaderMode = { readerTopic = topic },
                                    onSummary = { summaryTopic = topic },
                                    onDebate = { createDebatePost(topic, viewModel, navController) },
                                    onBookmark = {
                                        viewModel.toggleBookmarkNews(topic, context)
                                        Toast.makeText(
                                            context,
                                            if (isSaved) "Article retiré des signets" else "Article sauvegardé !",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onShare = { shareArticle(context, topic) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Summary Modal Dialog
    summaryTopic?.let { topic ->
        GoogleNewsAiSummaryDialog(
            topic = topic,
            onDismiss = { summaryTopic = null },
            onOpenFull = {
                summaryTopic = null
                openArticle(topic.link, navController)
            },
            onDebate = {
                summaryTopic = null
                createDebatePost(topic, viewModel, navController)
            }
        )
    }

    // Clean Reader Mode Modal Dialog
    readerTopic?.let { topic ->
        GoogleNewsReaderDialog(
            topic = topic,
            onDismiss = { readerTopic = null },
            onOpenOriginal = {
                readerTopic = null
                openArticle(topic.link, navController)
            },
            onDebate = {
                readerTopic = null
                createDebatePost(topic, viewModel, navController)
            }
        )
    }
}

@Composable
fun GoogleNewsHeroCard(
    topic: TrendingTopic,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onReaderMode: () -> Unit,
    onSummary: () -> Unit,
    onDebate: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpen() }
            .testTag("google_news_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional image header or stylized gradient banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                if (!topic.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = topic.imageUrl,
                        contentDescription = topic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark scrim for text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                }

                // Top badges row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ À LA UNE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${topic.category.emoji} ${topic.category.label}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(16.dp)) {
                // Source & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = topic.source,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${topic.pubDateFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ~${topic.readTimeMin} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp
                )

                if (!topic.snippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topic.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // AI Key Takeaways preview
                if (topic.keyTakeaways.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Points clés Google News",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            topic.keyTakeaways.take(2).forEach { point ->
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Synthèse IA button
                    FilledTonalButton(
                        onClick = onSummary,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_synth_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synthèse IA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Reader Mode button
                    OutlinedButton(
                        onClick = onReaderMode,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_reader_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lire", fontSize = 12.sp)
                    }

                    // Débattre dans un Actfile
                    FilledTonalButton(
                        onClick = onDebate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_debate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Débattre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Bookmark
                    IconButton(
                        onClick = onBookmark,
                        modifier = Modifier.size(36.dp).testTag("hero_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Sauvegarder",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp).testTag("hero_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Partager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleNewsArticleCard(
    topic: TrendingTopic,
    rank: Int,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onReaderMode: () -> Unit,
    onSummary: () -> Unit,
    onDebate: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpen() }
            .testTag("google_news_article_$rank"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Rank + Category Badge + Source + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = topic.source,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "• ${topic.pubDateFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${topic.category.emoji} ${topic.category.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )

            if (!topic.snippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = topic.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags row
            if (topic.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topic.tags.take(3).forEach { tag ->
                        Text(
                            text = if (tag.startsWith("#")) tag else "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Synthèse IA button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable { onSummary() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Synthèse IA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mode Lecture
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable { onReaderMode() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mode Lecteur",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Débattre
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .clickable { onDebate() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RateReview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Débattre",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action Icons (Bookmark & Share)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Sauvegarder",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
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
}

@Composable
fun GoogleNewsAiSummaryDialog(
    topic: TrendingTopic,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit,
    onDebate: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with AI gradient
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4285F4),
                                        Color(0xFF9C27B0),
                                        Color(0xFFE91E63)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Synthèse IA Google News",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = topic.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Article Title
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Structured Takeaways
                Text(
                    text = "3 POINTS CLÉS À RETENIR :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                topic.keyTakeaways.forEach { point ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!topic.snippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Extrait analysé : \"${topic.snippet}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Open Full Article & Debate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFull,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lire source", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDebate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Débattre", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleNewsReaderDialog(
    topic: TrendingTopic,
    onDismiss: () -> Unit,
    onOpenOriginal: () -> Unit,
    onDebate: () -> Unit
) {
    var fontSizeSp by remember { mutableIntStateOf(16) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top control bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Lecteur Épuré",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Font size controls
                        IconButton(
                            onClick = { if (fontSizeSp > 12) fontSizeSp -= 2 },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { if (fontSizeSp < 24) fontSizeSp += 2 },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Scrollable Article Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = topic.source,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = topic.pubDateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            lineHeight = (fontSizeSp + 8).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!topic.snippet.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "SOMMAIRE RAPIDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topic.snippet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp + 6).sp
                                    )
                                }
                            }
                        }

                        if (topic.keyTakeaways.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ANALYSE & POINTS ESSENTIELS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            topic.keyTakeaways.forEach { takeaway ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = takeaway,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp + 6).sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Pour consulter l'intégralité du reportage original, les infographies interactives et les commentaires des journalistes, utilisez le bouton ci-dessous pour ouvrir la page source.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenOriginal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ouvrir source ↗", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDebate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Créer Débat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helpers
private fun openArticle(url: String, navController: NavController) {
    val encodedUrl = URLEncoder.encode(url, "UTF-8")
    navController.navigate("browser/$encodedUrl")
}

private fun createDebatePost(
    topic: TrendingTopic,
    viewModel: IddetViewModel,
    navController: NavController
) {
    val snippetText = if (!topic.snippet.isNullOrBlank()) "> ${topic.snippet.replace("\n", " ").trim()}\n" else ""
    val takeawaysFormatted = if (topic.keyTakeaways.isNotEmpty()) {
        "\n### 📌 Points clés de l'actualité\n" + topic.keyTakeaways.joinToString("\n") { "- $it" } + "\n"
    } else ""
    val tagsFormatted = topic.tags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }

    val debateDocument = """
        |> [!DEBATE]
        |> 🔍 **Débat Google Actualités** • source vérifiée
        |> [${topic.title}](${topic.link})
        |> *Source : ${topic.source} • ${topic.pubDateFormatted}*
        $snippetText$takeawaysFormatted
        
        Que pensez-vous de cette annonce ? Partagez vos analyses et retours d'expérience avec la communauté !
        
        $tagsFormatted #GoogleNews #IDDET
    """.trimMargin().trim()

    viewModel.setComposerInitialContent(debateDocument)
    viewModel.setShowComposer(true)
    if (navController.currentBackStackEntry?.destination?.route != "home") {
        navController.navigate("home") {
            popUpTo("home") { inclusive = false }
        }
    }
}

private fun shareArticle(context: Context, topic: TrendingTopic) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "📰 Google Actualités : ${topic.title}\n\nVia ${topic.source}\n🔗 ${topic.link}"
        )
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Partager l'actualité")
    context.startActivity(shareIntent)
}
