package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.example.ui.IddetViewModel
import com.example.ui.AuthViewModel
import com.example.ui.screens.CustomBrowserScreen
import com.example.ui.components.LocalCommunityClickHandler
import com.example.ui.components.LocalChannelClickHandler
import com.example.ui.components.VerificationBadge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: IddetViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingRoute by com.example.utils.NotificationRouter.pendingRoute
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            try {
                navController.navigate(route)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            com.example.utils.NotificationRouter.pendingRoute.value = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
        viewModel.refreshActfiles()
        viewModel.refreshMyLevel()
        viewModel.loadLevelsTable()
    }

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val myLevel by viewModel.myLevel.collectAsState()
    val levelsTable by viewModel.levelsTable.collectAsState()
    val iddetPlusStatus by viewModel.myIddetPlusStatus.collectAsState()
    var showLadderDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentFeedTab by viewModel.feedTab.collectAsState()
    val currentSelectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val unreadCount = notifications.count { !it.isRead }

    LaunchedEffect(currentUser?.id) {
        val uid = currentUser?.id
        com.example.utils.CallManager.initialize(context, viewModel.repository)
        com.example.utils.CallManager.setCurrentUser(uid)
        if (uid != null) {
            com.example.utils.WebSocketManager.connect(uid)
        } else {
            com.example.utils.WebSocketManager.disconnect()
        }
    }

    var communitiesExpanded by remember { mutableStateOf(currentSelectedCategory != null) }
    LaunchedEffect(currentSelectedCategory) {
        if (currentSelectedCategory != null) {
            communitiesExpanded = true
        }
    }

    val appColors = com.example.ui.theme.LocalAppColorScheme.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = appColors.sidebarBackground,
                drawerContentColor = appColors.sidebarForeground,
                drawerShape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // BRAND HEADER ROW (Like Reddit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.AppDynamicLogo(
                            size = 46.dp,
                            showGlow = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "IDDET",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "CMO • Boosted Engine",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = appColors.sidebarForeground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Profile Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = currentUser?.avatarUrl
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                        contentDescription = "My Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = currentUser?.username?.firstOrNull()?.toString()?.uppercase() ?: "?",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUser?.username ?: "Utilisateur",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerificationBadge(
                                        userName = currentUser?.username,
                                        isVerified = currentUser?.isVerified == true,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    com.example.ui.components.UserLevelBadge(
                                        level = myLevel,
                                        onClick = { showLadderDialog = true }
                                    )
                                }
                                Text(
                                    text = currentUser?.bio ?: "Bienvenue sur mon profil IDDET !",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // GAMIFIED LEVEL & RANK WIDGET
                        com.example.ui.components.UserLevelCard(
                            level = myLevel,
                            onOpenLadder = { showLadderDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = "Score calculé sur likes & commentaires"
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // IDDET PLUS PROMO BANNER IN DRAWER
                        com.example.ui.components.IddetPlusPromoBanner(
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("iddet_plus")
                            },
                            isPremium = iddetPlusStatus?.is_iddet_plus == true
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${com.example.utils.FormatUtils.formatCount(currentUser?.followingCount ?: 0)} abonnements",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${com.example.utils.FormatUtils.formatCount(currentUser?.followersCount ?: 0)} abonnés",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    
                    // HIGHLY PROMINENT PUBLISH BUTTON
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                            viewModel.setShowComposer(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Create,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Créer un Actfile",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // SECTION: FEEDS (FLUX)
                    Text(
                        text = "FLUX PRINCIPAUX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = appColors.sidebarForeground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    val feeds = listOf(
                        Triple("Pour vous", 0, Icons.Outlined.AutoAwesome),
                        Triple("Abonnements", 1, Icons.Outlined.People),
                        Triple("Populaire", 2, Icons.Outlined.TrendingUp)
                    )

                    feeds.forEach { (label, tabIndex, icon) ->
                        val isFeedSelected = currentRoute == "home" && currentFeedTab == tabIndex && currentSelectedCategory == null
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isFeedSelected) appColors.sidebarActive else appColors.sidebarForeground
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (isFeedSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isFeedSelected) appColors.sidebarActive else appColors.sidebarForeground
                                )
                            },
                            selected = isFeedSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.setFeedTab(tabIndex)
                                viewModel.setSelectedCategoryFilter(null)
                                if (currentRoute != "home") {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = if (appColors.isDark) appColors.sidebarActive.copy(alpha = 0.2f) else appColors.sidebarActive.copy(alpha = 0.08f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = appColors.sidebarActive,
                                unselectedIconColor = appColors.sidebarForeground,
                                selectedTextColor = appColors.sidebarActive,
                                unselectedTextColor = appColors.sidebarForeground
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    // Collapsible COMMUNAUTÉ section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { communitiesExpanded = !communitiesExpanded }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (communitiesExpanded) "COMMUNAUTÉ /\\" else "COMMUNAUTÉ \\/",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            imageVector = if (communitiesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (communitiesExpanded) {
                        // All categories option
                        val isAllCategoriesSelected = currentRoute == "home" && currentSelectedCategory == null
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.Outlined.AllInclusive,
                                    contentDescription = "Toutes les catégories",
                                    tint = if (isAllCategoriesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    "Toutes les catégories",
                                    fontWeight = if (isAllCategoriesSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isAllCategoriesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            selected = isAllCategoriesSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.setSelectedCategoryFilter(null)
                                if (currentRoute != "home") {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        // Individual categories
                        com.example.ui.components.APP_CATEGORIES.forEach { cat ->
                            val isCatSelected = currentRoute == "home" && currentSelectedCategory == cat.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCatSelected) cat.color.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        viewModel.setSelectedCategoryFilter(cat.id)
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(cat.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.emoji, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCatSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isCatSelected) cat.color else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    // SECTION: TRADUCTION
                    Text(
                        text = "TRADUCTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    val targetLanguage by viewModel.targetLanguage.collectAsState()
                    var languageExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        OutlinedCard(
                            onClick = { languageExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Langue cible",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        targetLanguage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    if (languageExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .heightIn(max = 400.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            com.example.utils.TranslationHelper.supportedLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        viewModel.setTargetLanguage(lang)
                                        languageExpanded = false
                                    },
                                    leadingIcon = {
                                        if (lang == targetLanguage) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                    // SECTION: NAVIGATION PRINCIPALE
                    Text(
                        text = "AUTRES PAGES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    // Drawer menu list
                    val drawerItems = listOf(
                        Triple("Reels & Vidéos 🎬", "reels", Icons.Outlined.VideoLibrary),
                        Triple("Studio Musique 🎵", "music", Icons.Outlined.MusicNote),
                        Triple("Google Actualités 📰", "google_news", Icons.Outlined.Newspaper),
                        Triple("IDDET Ads Manager 📢", "iddet_ads", Icons.Outlined.Campaign),
                        Triple("Iddet Plus VIP ✨", "iddet_plus", Icons.Outlined.Star),
                        Triple("Hide It Pro Studio 🐱", "hide_it_pro", Icons.Outlined.VisibilityOff),
                        Triple("Radar des Potes 🗺️", "friends_map", Icons.Outlined.Place),
                        Triple("Serveur MCP & Outils 🔌", "mcp_protocol", Icons.Outlined.Build),
                        Triple("Rechercher", "search", Icons.Outlined.Search),
                        Triple("Discussions", "messages", Icons.Outlined.Message),
                        Triple("Communautés", "communities", Icons.Outlined.Group),
                        Triple("Centre d'Aide AI", "helper", Icons.Outlined.HelpOutline),
                        Triple("Mon Profil", "profile", Icons.Outlined.Person),
                        Triple("Historique Actf", "history", Icons.Outlined.History),
                        Triple("Paramètres", "settings", Icons.Outlined.Settings)
                    )
                    
                    drawerItems.forEach { (label, route, icon) ->
                        val isSelected = currentRoute == route
                        NavigationDrawerItem(
                            icon = { 
                                Icon(
                                    icon, 
                                    contentDescription = label,
                                    tint = if (isSelected) appColors.sidebarActive else appColors.sidebarForeground
                                ) 
                            },
                            label = { 
                                Text(
                                    label, 
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) appColors.sidebarActive else appColors.sidebarForeground
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        if (route == "home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = if (appColors.isDark) appColors.sidebarActive.copy(alpha = 0.2f) else appColors.sidebarActive.copy(alpha = 0.08f),
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = appColors.sidebarActive,
                                unselectedIconColor = appColors.sidebarForeground,
                                selectedTextColor = appColors.sidebarActive,
                                unselectedTextColor = appColors.sidebarForeground
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    // NOTIFICATIONS WITH REAL UNREAD BADGE IN LATERAL DRAWER!
                    val isNotificationsSelected = currentRoute == "notifications"
                    NavigationDrawerItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (isNotificationsSelected) appColors.sidebarActive else appColors.sidebarForeground
                                )
                            }
                        },
                        label = {
                            Text(
                                "Notifications",
                                fontWeight = if (isNotificationsSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isNotificationsSelected) appColors.sidebarActive else appColors.sidebarForeground
                            )
                        },
                        selected = isNotificationsSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != "notifications") {
                                navController.navigate("notifications")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = if (appColors.isDark) appColors.sidebarActive.copy(alpha = 0.2f) else appColors.sidebarActive.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = appColors.sidebarActive,
                            unselectedIconColor = appColors.sidebarForeground,
                            selectedTextColor = appColors.sidebarActive,
                            unselectedTextColor = appColors.sidebarForeground
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    
                    // REFRESH MENU ITEM
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                Icons.Outlined.Refresh, 
                                contentDescription = "Rafraîchir",
                                tint = appColors.sidebarForeground
                            ) 
                        },
                        label = { 
                            Text(
                                "Rafraîchir le fil", 
                                fontWeight = FontWeight.Bold,
                                color = appColors.sidebarForeground
                            ) 
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.refreshActfiles()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = appColors.sidebarForeground,
                            unselectedTextColor = appColors.sidebarForeground
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // LOGOUT MENU ITEM
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout, 
                                contentDescription = "Se déconnecter",
                                tint = appColors.error
                            ) 
                        },
                        label = { 
                            Text(
                                "Déconnexion", 
                                fontWeight = FontWeight.Bold,
                                color = appColors.error
                            ) 
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = appColors.error,
                            unselectedTextColor = appColors.error
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "IDDET CMO v2.5 • Boosted Engine",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .padding(20.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) {
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                shape = RoundedCornerShape(8.dp),
                containerColor = appColors.surfaceVariant,
                title = {
                    Text(
                        text = "Se déconnecter",
                        fontWeight = FontWeight.Bold,
                        color = appColors.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Voulez-vous vraiment vous déconnecter de votre compte IDDET ?",
                        color = appColors.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.error,
                            contentColor = appColors.onError
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Déconnexion", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = appColors.onSurface
                        )
                    ) {
                        Text("Annuler")
                    }
                }
            )
        }

        Scaffold { innerPadding ->
            CompositionLocalProvider(
                LocalCommunityClickHandler provides { slug ->
                    navController.navigate("community/$slug")
                },
                LocalChannelClickHandler provides { channelSlug ->
                    navController.navigate("communities")
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    exitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popEnterTransition = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popExitTransition = { fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) }
                ) {
                composable("home") { 
                    HomeScreen(
                        viewModel = viewModel, 
                        navController = navController,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    ) 
                }
                composable("friends_map") { FriendsMapScreen(viewModel, navController) }
                composable("search") { SearchScreen(viewModel, navController) }
                composable("messages") { ConversationsListScreen(viewModel, navController) }
                composable("helper") { com.example.ui.screens.HelperScreen(navController) }
                composable("profile") { ProfileScreen(viewModel, navController) }
                composable(
                    "profile/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) }
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    OtherProfileScreen(viewModel, navController, userId)
                }
                composable(
                    "chat/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) }
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    ChatThreadScreen(userId, viewModel, navController)
                }
                composable(
                    "discussion/{actfileId}",
                    arguments = listOf(navArgument("actfileId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) }
                ) { backStackEntry ->
                    val actfileId = backStackEntry.arguments?.getString("actfileId") ?: return@composable
                    DiscussionScreen(viewModel, navController, actfileId)
                }
                composable("communities") { CommunitiesScreen(viewModel, navController) }
                composable(
                    "community/{slug}",
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                    CommunityDetailScreen(slug, viewModel, navController)
                }
                composable("notifications") { NotificationsScreen(viewModel, navController) }
                composable("history") { HistoryScreen(viewModel, navController) }
                composable("iddet_ads") { IddetAdsManagerScreen(viewModel, navController) }
                composable("settings") { 
                    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    SettingsScreen(viewModel, authViewModel, navController) 
                }
                composable("marketplace") { MarketplaceScreen(navController) }
                composable("iddet_plus") { IddetPlusScreen(viewModel, navController) }
                composable("hide_it_pro") { HideItProScreen(navController) }
                composable(
                    "reels",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    ReelsFeedScreen(viewModel, navController)
                }
                composable(
                    "sound/{soundId}",
                    arguments = listOf(navArgument("soundId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val soundId = backStackEntry.arguments?.getString("soundId") ?: return@composable
                    SoundDetailScreen(soundId, viewModel, navController)
                }
                composable(
                    "video/{videoId}",
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                    VideoDetailScreen(videoId, viewModel, navController)
                }
                composable(
                    "music",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    MusicScreen(viewModel, navController)
                }
                composable(
                    "mcp_protocol",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    com.example.ui.screens.McpProtocolScreen(viewModel, navController)
                }
                composable(
                    "google_news",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    GoogleNewsScreen(viewModel, navController)
                }
                composable(
                    "browser/{url}",
                    arguments = listOf(navArgument("url") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val url = backStackEntry.arguments?.getString("url") ?: ""
                    val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
                    CustomBrowserScreen(
                        url = decodedUrl,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showLadderDialog) {
        com.example.ui.components.LevelsLadderDialog(
            currentLevel = myLevel,
            table = levelsTable,
            onDismiss = { showLadderDialog = false }
        )
    }

    com.example.ui.components.CallOverlayHost()
}
}
