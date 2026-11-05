package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.ActfileWithUser
import com.example.ui.IddetViewModel
import com.example.ui.components.ActfileCard
import com.example.ui.components.PromoteActfileDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    val likedActfiles by viewModel.likedActfiles.collectAsState()
    val commentedActfiles by viewModel.commentedActfiles.collectAsState()
    val currentUserId = viewModel.currentUser.collectAsState().value?.id

    var actfileToPromote by remember { mutableStateOf<ActfileWithUser?>(null) }
    var isLoadingAdAction by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique Actf", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Elegant tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discussions Rejointes", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Favoris / Likés", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            val listToDisplay = if (selectedTab == 0) commentedActfiles else likedActfiles

            if (listToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "Aucune discussion rejointe" else "Aucun actfile aimé",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 0) {
                                "Les actfiles que vous commentez s'afficheront ici."
                            } else {
                                "Les actfiles que vous aimez s'afficheront ici."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listToDisplay, key = { it.id }) { actfile ->
                        val isMine = actfile.userId == currentUserId
                        val targetLanguage by viewModel.targetLanguage.collectAsState()
                        val aiState by viewModel.aiState.collectAsState()
                        ActfileCard(
                            actfile = actfile,
                            onLike = { viewModel.likeActfile(it) },
                            onView = { viewModel.incrementView(it) },
                            targetLanguageName = targetLanguage,
                            isAiReady = aiState == com.example.utils.AiModelState.READY,
                            onUserClick = { userId ->
                                if (userId == currentUserId) {
                                    navController.navigate("profile")
                                } else {
                                    navController.navigate("profile/$userId")
                                }
                            },
                            onComment = { actfileId ->
                                navController.navigate("discussion/$actfileId")
                            },
                            onDelete = if (isMine) { { viewModel.deleteActfile(it) } } else null,
                            onPromote = if (isMine) { { actfileToPromote = it } } else null,
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
                            onCategoryClick = { slug -> navController.navigate("community/$slug") },
                            onCommunityClick = { slug -> navController.navigate("community/$slug") }
                        )
                    }
                }
            }
        }

        actfileToPromote?.let { targetActfile ->
            PromoteActfileDialog(
                actfile = targetActfile,
                isLoading = isLoadingAdAction,
                onDismiss = { actfileToPromote = null },
                onPromote = { budget, currency, daily, targetCountry ->
                    isLoadingAdAction = true
                    viewModel.promoteActfile(
                        actfileId = targetActfile.id,
                        budget = budget,
                        currency = currency,
                        daily = daily,
                        targetCountry = targetCountry,
                        onSuccess = { checkoutUrl ->
                            isLoadingAdAction = false
                            actfileToPromote = null
                            Toast.makeText(context, "Campagne lancée !", Toast.LENGTH_SHORT).show()
                            if (!checkoutUrl.isNullOrBlank()) {
                                val encoded = java.net.URLEncoder.encode(checkoutUrl, "UTF-8")
                                navController.navigate("browser/$encoded")
                            }
                        },
                        onError = { err ->
                            isLoadingAdAction = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onCancelAd = {
                    isLoadingAdAction = true
                    viewModel.cancelActfileAd(targetActfile.id) { success ->
                        isLoadingAdAction = false
                        actfileToPromote = null
                        if (success) {
                            Toast.makeText(context, "Sponsorisation arrêtée", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Échec de l'annulation", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
