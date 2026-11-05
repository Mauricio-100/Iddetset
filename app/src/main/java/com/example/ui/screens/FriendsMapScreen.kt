package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.VerificationBadge
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.IddetViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class MapTileTheme(val label: String, val icon: String, val url: String, val attribution: String, val isDark: Boolean = false) {
    SNAP_DARK(
        "Snap Dark",
        "🌙",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "&copy; OpenStreetMap contributors",
        isDark = true
    ),
    STREETS(
        "Rues Claires",
        "🗺️",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "&copy; OpenStreetMap contributors",
        isDark = false
    ),
    SATELLITE(
        "Hybride",
        "🛰️",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        "&copy; Esri, Maxar, Earthstar Geographics",
        isDark = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsMapScreen(
    viewModel: IddetViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val friends by viewModel.friendsLocations.collectAsStateWithLifecycle()
    val chillSpots by viewModel.chillSpots.collectAsStateWithLifecycle()
    val isGhostMode by viewModel.isGhostMode.collectAsStateWithLifecycle()
    val currentUserVibe by viewModel.currentUserVibe.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val realLocation by viewModel.realLocation.collectAsStateWithLifecycle()
    val lastWavedFriend by viewModel.lastWavedFriend.collectAsStateWithLifecycle()
    val radarScanRadiusKm by viewModel.radarScanRadiusKm.collectAsStateWithLifecycle()
    val lastChillWaveSent by viewModel.lastChillWaveSent.collectAsStateWithLifecycle()
    val trafficJamAlerts by viewModel.trafficJamAlerts.collectAsStateWithLifecycle()
    val isServerSynced by viewModel.isServerSynced.collectAsStateWithLifecycle()
    val serverSpeedKmh by viewModel.serverSpeedKmh.collectAsStateWithLifecycle()

    var radarViewMode by remember { mutableStateOf(RadarViewMode.MAP_VIEW) }
    var selectedFilter by remember { mutableStateOf(FriendActivityFilter.ALL) }
    var selectedFriend by remember { mutableStateOf<FriendLocation?>(null) }
    var currentMapTheme by remember { mutableStateOf(MapTileTheme.SNAP_DARK) }
    var showVibeEditorDialog by remember { mutableStateOf(false) }
    var showWaveCelebration by remember { mutableStateOf(false) }
    var wavedFriendName by remember { mutableStateOf("") }
    var showReportTrafficDialog by remember { mutableStateOf(false) }
    var trafficReportNote by remember { mutableStateOf("Embouteillage important") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
            } catch (_: Exception) {}
            webViewRef = null
        }
    }

    var showChillWaveDialogFor by remember { mutableStateOf<FriendLocation?>(null) }
    var showDropChillSpotDialog by remember { mutableStateOf(false) }
    var randomMatchedFriend by remember { mutableStateOf<FriendLocation?>(null) }

    // Check location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            viewModel.initLocationTracking(context)
            Toast.makeText(context, "📍 GPS réel activé !", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Autorisation localisation requise pour la position GPS en direct", Toast.LENGTH_LONG).show()
        }
    }

    // Auto initialize location tracking on mount
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.initLocationTracking(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Filtered friends list
    val filteredFriends = remember(friends, selectedFilter) {
        when (selectedFilter) {
            FriendActivityFilter.ALL -> friends
            FriendActivityFilter.NEARBY -> friends.filter { it.distanceKm <= 2.0 }
            FriendActivityFilter.AVAILABLE -> friends.filter { it.activityTag.contains("Dispo", ignoreCase = true) || it.statusEmoji == "⚡" }
            FriendActivityFilter.MUSIC_LOUNGE -> friends.filter { it.activityTag.contains("Musique", ignoreCase = true) || it.currentlyPlayingMusic != null }
            FriendActivityFilter.STUDY_DEV -> friends.filter { it.activityTag.contains("Dev", ignoreCase = true) || it.activityTag.contains("Docs", ignoreCase = true) }
            FriendActivityFilter.GAMING -> friends.filter { it.activityTag.contains("Game", ignoreCase = true) }
        }
    }

    // Push updated coordinates or theme to Leaflet map
    LaunchedEffect(realLocation, filteredFriends, isGhostMode, currentMapTheme, isMapLoaded, trafficJamAlerts) {
        if (isMapLoaded && webViewRef != null) {
            val friendsJson = JSONArray().apply {
                filteredFriends.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("username", f.username)
                        put("displayName", f.displayName)
                        put("avatarUrl", f.avatarUrl ?: "")
                        put("lat", f.latitude)
                        put("lng", f.longitude)
                        put("statusEmoji", f.statusEmoji)
                        put("statusMessage", f.statusMessage)
                        put("activityTag", f.activityTag)
                        put("isOnline", f.isOnline)
                        put("battery", f.batteryPercent)
                        put("distanceKm", f.distanceKm)
                        put("isFavorite", f.isFavorite)
                    })
                }
            }.toString()

            val trafficJamsJson = JSONArray().apply {
                trafficJamAlerts.forEach { jam ->
                    put(JSONObject().apply {
                        put("id", jam.id)
                        put("lat", jam.latitude)
                        put("lng", jam.longitude)
                        put("message", jam.message)
                    })
                }
            }.toString()

            val userAvatar = currentUser?.avatarUrl ?: ""
            val username = currentUser?.username ?: "Moi"
            val jsCall = """
                if (window.updateMapData) {
                    window.updateMapData(
                        ${realLocation.latitude},
                        ${realLocation.longitude},
                        ${realLocation.accuracy},
                        $isGhostMode,
                        '${currentUserVibe.emoji}',
                        '$username',
                        '$userAvatar',
                        $friendsJson,
                        '${currentMapTheme.url}',
                        ${currentMapTheme.isDark}
                    );
                }
                if (window.updateTrafficJams) {
                    window.updateTrafficJams($trafficJamsJson);
                }
            """.trimIndent()
            webViewRef?.evaluateJavascript(jsCall, null)
        }
    }

    LaunchedEffect(lastWavedFriend) {
        if (lastWavedFriend != null) {
            wavedFriendName = lastWavedFriend!!
            showWaveCelebration = true
            delay(2800)
            showWaveCelebration = false
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Radar & Chill IDDET",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (realLocation.isRealGpsAcquired) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (realLocation.isRealGpsAcquired) Color(0xFF10B981) else Color(0xFFF59E0B))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (realLocation.isRealGpsAcquired) "GPS Fixé" else "Recherche GPS...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (realLocation.isRealGpsAcquired) Color(0xFF059669) else Color(0xFFD97706),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isGhostMode) "👻 Mode Fantôme (Masqué)" else "📍 ${realLocation.address}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGhostMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        // Map Theme Selector Button (only when on Map View)
                        if (radarViewMode == RadarViewMode.MAP_VIEW) {
                            IconButton(
                                onClick = {
                                    val nextTheme = when (currentMapTheme) {
                                        MapTileTheme.SNAP_DARK -> MapTileTheme.STREETS
                                        MapTileTheme.STREETS -> MapTileTheme.SATELLITE
                                        MapTileTheme.SATELLITE -> MapTileTheme.SNAP_DARK
                                    }
                                    currentMapTheme = nextTheme
                                    Toast.makeText(context, "Carte : ${nextTheme.label}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(currentMapTheme.icon, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        // Quick Status Vibe Button
                        IconButton(
                            onClick = { showVibeEditorDialog = true },
                            modifier = Modifier.testTag("status_vibe_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(currentUserVibe.emoji, fontSize = 16.sp)
                                }
                            }
                        }

                        // Ghost Mode Button
                        IconButton(
                            onClick = {
                                val newMode = !isGhostMode
                                viewModel.setGhostMode(newMode)
                                Toast.makeText(
                                    context,
                                    if (newMode) "👻 Mode Fantôme : Position masquée aux amis !" else "✨ Tu es de nouveau visible sur la carte !",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("ghost_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isGhostMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mode Fantôme",
                                tint = if (isGhostMode) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Mode Selector Segmented Tabs
                TabRow(
                    selectedTabIndex = radarViewMode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    RadarViewMode.values().forEach { mode ->
                        Tab(
                            selected = radarViewMode == mode,
                            onClick = { radarViewMode = mode },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mode.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.title,
                                        fontWeight = if (radarViewMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            when (radarViewMode) {
                RadarViewMode.MAP_VIEW -> {
                    // Real Interactive Leaflet / OpenStreetMap / CartoDB Map
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    allowFileAccess = true
                                    setGeolocationEnabled(true)
                                }
                                 webViewClient = FriendsMapWebViewClient(
                                     onPageFinishedAction = { isMapLoaded = true },
                                     onRenderCrashedAction = {
                                         webViewRef = null
                                         isMapLoaded = false
                                     }
                                 )
                                webChromeClient = WebChromeClient()

                                class FriendsMapJSBridge {
                                    @JavascriptInterface
                                    fun onFriendClicked(friendId: String) {
                                        scope.launch {
                                            val f = friends.find { it.id == friendId }
                                            if (f != null) {
                                                selectedFriend = f
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onTrafficJamClicked(jamId: String) {
                                        scope.launch {
                                            val jam = trafficJamAlerts.find { it.id == jamId }
                                            if (jam != null) {
                                                Toast.makeText(context, "🚗 " + jam.message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onMapClicked() {
                                        scope.launch {
                                            selectedFriend = null
                                        }
                                    }
                                }

                                addJavascriptInterface(FriendsMapJSBridge(), "AndroidBridge")

                                loadDataWithBaseURL(
                                    "https://iddet.local/",
                                    generateLeafletSnapMapHtml(
                                        userLat = realLocation.latitude,
                                        userLng = realLocation.longitude,
                                        tileUrl = currentMapTheme.url,
                                        tileAttribution = currentMapTheme.attribution,
                                        isDark = currentMapTheme.isDark
                                    ),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                        }
                    )

                    // Permission Request Prompt if not granted
                    if (!hasLocationPermission) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Localisation requise",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Active le GPS pour te voir en direct sur la carte avec tes amis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Activer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Top Filter Chips & Traffic Jam Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = if (!hasLocationPermission) 80.dp else 12.dp)
                    ) {
                        // Traffic Jam Alert Notification Banner
                        if (trafficJamAlerts.isNotEmpty()) {
                            val activeJam = trafficJamAlerts.first()
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFDC2626),
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🚗", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Radar Trafic : Ralentissement",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            activeJam.message,
                                            color = Color.White.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            webViewRef?.evaluateJavascript(
                                                "if(window.centerOnFriend){ window.centerOnFriend(${activeJam.latitude}, ${activeJam.longitude}); }",
                                                null
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = "Localiser", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissTrafficJam(activeJam.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(FriendActivityFilter.values()) { filter ->
                                val isSelected = filter == selectedFilter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            "${filter.emoji} ${filter.label}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                                        labelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) Color.Transparent else Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }

                    // Right-Side Map Action Buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Center on Real User GPS
                        FloatingActionButton(
                            onClick = {
                                webViewRef?.evaluateJavascript(
                                    "if(window.centerOnUser){ window.centerOnUser(${realLocation.latitude}, ${realLocation.longitude}); }",
                                    null
                                )
                                Toast.makeText(context, "Centré sur ma position GPS 📍", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color(0xFF38BDF8),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Ma position", modifier = Modifier.size(22.dp))
                        }

                        // Zoom In (+)
                        FloatingActionButton(
                            onClick = {
                                webViewRef?.evaluateJavascript("if(window.map){ window.map.zoomIn(); }", null)
                            },
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom +", modifier = Modifier.size(22.dp))
                        }

                        // Zoom Out (-)
                        FloatingActionButton(
                            onClick = {
                                webViewRef?.evaluateJavascript("if(window.map){ window.map.zoomOut(); }", null)
                            },
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom -", modifier = Modifier.size(22.dp))
                        }

                        // Real-time GPS Pulse Refresh
                        FloatingActionButton(
                            onClick = {
                                viewModel.refreshFriendsRadar()
                                viewModel.initLocationTracking(context)
                                Toast.makeText(context, "📡 Actualisation du radar en direct...", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color(0xFF10B981),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", modifier = Modifier.size(22.dp))
                        }

                        // Report Traffic Jam FAB
                        FloatingActionButton(
                            onClick = {
                                showReportTrafficDialog = true
                            },
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Signaler trafic", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Bottom Friends Carousel / Selected Friend Bottom Sheet Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedFriend,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) + slideInVertically { it / 2 } togetherWith
                                        fadeOut(animationSpec = tween(180)) + slideOutVertically { it / 2 }
                            },
                            label = "SelectedFriendCard"
                        ) { friend ->
                            if (friend != null) {
                                SelectedFriendDetailCard(
                                    friend = friend,
                                    onClose = {
                                        selectedFriend = null
                                    },
                                    onWave = {
                                        viewModel.sendWaveToFriend(friend)
                                    },
                                    onChillWave = {
                                        showChillWaveDialogFor = friend
                                    },
                                    onChat = {
                                        navController.navigate("chat/${friend.username}")
                                    },
                                    onViewProfile = {
                                        navController.navigate("profile/${friend.username}")
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavoriteFriend(friend.id)
                                    }
                                )
                            } else {
                                // Quick Carousel of buddies
                                FriendsQuickCarousel(
                                    friends = filteredFriends,
                                    onSelectFriend = { f ->
                                        selectedFriend = f
                                        webViewRef?.evaluateJavascript(
                                            "if(window.centerOnFriend){ window.centerOnFriend(${f.latitude}, ${f.longitude}); }",
                                            null
                                        )
                                    },
                                    onWaveFriend = { f ->
                                        showChillWaveDialogFor = f
                                    }
                                )
                            }
                        }
                    }
                }

                RadarViewMode.SONAR_RADAR -> {
                    // Tactical 360 Sonar Radar View
                    TacticalSonarRadarView(
                        friends = friends,
                        chillSpots = chillSpots,
                        userLat = realLocation.latitude,
                        userLng = realLocation.longitude,
                        selectedFriend = selectedFriend,
                        onSelectFriend = { friend ->
                            selectedFriend = friend
                        },
                        onSelectSpot = { spot ->
                            Toast.makeText(context, "${spot.emoji} ${spot.title} • ${spot.distanceKm} km", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerScan = {
                            viewModel.refreshFriendsRadar()
                            Toast.makeText(context, "📡 Scan Sonar 360° en direct synchronisé avec le serveur !", Toast.LENGTH_SHORT).show()
                        },
                        onRandomMatch = {
                            if (friends.isNotEmpty()) {
                                val random = friends.random()
                                randomMatchedFriend = random
                            }
                        },
                        scanRadiusKm = radarScanRadiusKm,
                        onRadiusChange = { radius ->
                            viewModel.setRadarScanRadius(radius)
                        },
                        serverSpeedKmh = serverSpeedKmh,
                        isServerSynced = isServerSynced,
                        trafficJamAlerts = trafficJamAlerts,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom Selected Card if blip is clicked
                    if (selectedFriend != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            SelectedFriendDetailCard(
                                friend = selectedFriend!!,
                                onClose = { selectedFriend = null },
                                onWave = { viewModel.sendWaveToFriend(selectedFriend!!) },
                                onChillWave = { showChillWaveDialogFor = selectedFriend },
                                onChat = { navController.navigate("chat/${selectedFriend!!.username}") },
                                onViewProfile = { navController.navigate("profile/${selectedFriend!!.username}") },
                                onToggleFavorite = { viewModel.toggleFavoriteFriend(selectedFriend!!.id) }
                            )
                        }
                    }
                }

                RadarViewMode.CHILL_LOUNGE -> {
                    // Chill Spots and Lounges Hub
                    ChillSpotsLoungeView(
                        spots = chillSpots,
                        onJoinSpot = { spot ->
                            viewModel.joinChillSpot(spot.id)
                            Toast.makeText(context, "✨ Tu as rejoint ${spot.title} !", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToSpot = { spot ->
                            radarViewMode = RadarViewMode.MAP_VIEW
                            webViewRef?.evaluateJavascript(
                                "if(window.centerOnFriend){ window.centerOnFriend(${spot.latitude}, ${spot.longitude}); }",
                                null
                            )
                        },
                        onDropSpotClick = {
                            showDropChillSpotDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top Floating Banner for sent Chill Waves
            if (lastChillWaveSent != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = lastChillWaveSent!!,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Wave celebration overlay
            if (showWaveCelebration) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👋✨", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Coucou envoyé !",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tu as fait une onde amicale à $wavedFriendName sur le radar IDDET.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to customize user vibe / status
    if (showVibeEditorDialog) {
        VibeStatusEditorDialog(
            currentVibe = currentUserVibe,
            onDismiss = { showVibeEditorDialog = false },
            onSave = { emoji, text, type ->
                viewModel.updateUserVibe(emoji, text, type)
                showVibeEditorDialog = false
                Toast.makeText(context, "Statut du jour mis à jour ! $emoji", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Chill Wave Selector Dialog
    if (showChillWaveDialogFor != null) {
        ChillWaveSelectorDialog(
            friend = showChillWaveDialogFor!!,
            onSelectWave = { waveType ->
                viewModel.sendChillWave(showChillWaveDialogFor!!, waveType)
                showChillWaveDialogFor = null
            },
            onDismiss = {
                showChillWaveDialogFor = null
            }
        )
    }

    // Drop Chill Spot Dialog
    if (showDropChillSpotDialog) {
        DropChillSpotDialog(
            userLat = realLocation.latitude,
            userLng = realLocation.longitude,
            onConfirm = { title, category, emoji, description ->
                viewModel.dropChillSpot(title, category, emoji, description, realLocation.latitude, realLocation.longitude)
                showDropChillSpotDialog = false
                Toast.makeText(context, "📍 Nouveau Chill Spot créé avec succès !", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showDropChillSpotDialog = false
            }
        )
    }

    // Roulette Match Random Dialog
    if (randomMatchedFriend != null) {
        val matched = randomMatchedFriend!!
        AlertDialog(
            onDismissRequest = { randomMatchedFriend = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎲", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Match Spontané Trouvé !", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!matched.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = matched.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(matched.displayName.take(1), fontSize = 24.sp, color = Color.White)
                            }
                        }
                    }

                    Text(
                        text = matched.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📍 ${matched.distanceKm} km • ${matched.district}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "« ${matched.chillStatus} »",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = matched
                        randomMatchedFriend = null
                        showChillWaveDialogFor = target
                    }
                ) {
                    Text("✨ Envoyer Onde Chill")
                }
            },
            dismissButton = {
                TextButton(onClick = { randomMatchedFriend = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Dialog: Signaler un embouteillage / accident
    if (showReportTrafficDialog) {
        AlertDialog(
            onDismissRequest = { showReportTrafficDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚗", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signaler un Ralentissement")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Partagez un incident ou un bouchon à vos amis et aux usagers proches sur le radar IDDET :",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val quickTypes = listOf(
                        "🚗 Ralentissement dense (< 8 km/h)",
                        "🚧 Travaux / Voie bloquée",
                        "⚠️ Accident / Danger",
                        "🛑 Circulation à l'arrêt"
                    )

                    quickTypes.forEach { typeOption ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (trafficReportNote == typeOption) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (trafficReportNote == typeOption) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { trafficReportNote = typeOption }
                        ) {
                            Text(
                                text = typeOption,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (trafficReportNote == typeOption) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportTrafficDialog = false
                        viewModel.reportTrafficJam(
                            lat = realLocation.latitude,
                            lng = realLocation.longitude,
                            note = trafficReportNote
                        )
                        Toast.makeText(context, "Alerte trafic diffusée sur le radar ! 🚗", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Diffuser l'alerte")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportTrafficDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// Generate high-performance Leaflet HTML for real Snap Map
private fun generateLeafletSnapMapHtml(
    userLat: Double,
    userLng: Double,
    tileUrl: String,
    tileAttribution: String,
    isDark: Boolean = true
): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body, #map { width: 100%; height: 100%; background: #0b132b; overflow: hidden; }
        
        /* Dark night tiles filter for Snap Dark mode (No API Key Required!) */
        .dark-tiles .leaflet-tile {
            filter: brightness(0.65) invert(1) contrast(2.2) hue-rotate(195deg) saturate(0.4) brightness(0.75);
        }
        
        /* Snap Map marker styles */
        .snap-user-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            transform: translate(-50%, -50%);
        }
        .snap-user-bubble {
            background: linear-gradient(135deg, #0284c7, #38bdf8);
            color: #ffffff;
            font-size: 11px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(2, 132, 199, 0.5);
            white-space: nowrap;
            margin-bottom: 4px;
            border: 1.5px solid #ffffff;
        }
        .snap-user-avatar {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            border: 3px solid #38bdf8;
            box-shadow: 0 0 20px rgba(56, 189, 248, 0.8);
            background: #0284c7;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            color: #fff;
            overflow: hidden;
            animation: pulse-ring 2.5s infinite;
        }
        .snap-user-avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .snap-friend-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transform: translate(-50%, -50%);
            transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        .snap-friend-marker:active {
            transform: translate(-50%, -50%) scale(1.15);
        }
        .snap-friend-bubble {
            background: rgba(15, 23, 42, 0.95);
            color: #fff;
            font-size: 11px;
            font-weight: bold;
            padding: 2px 7px;
            border-radius: 10px;
            border: 1px solid #38bdf8;
            box-shadow: 0 4px 10px rgba(0,0,0,0.5);
            white-space: nowrap;
            margin-bottom: 3px;
            display: flex;
            align-items: center;
            gap: 4px;
        }
        .snap-friend-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: 2.5px solid #10b981;
            background: #1e293b;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            color: #fff;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(0,0,0,0.6);
        }
        .snap-friend-avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .snap-traffic-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transform: translate(-50%, -50%);
            animation: traffic-bounce 2s infinite ease-in-out;
        }
        .snap-traffic-bubble {
            background: #dc2626;
            color: #ffffff;
            font-size: 11px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 12px;
            border: 1.5px solid #ffffff;
            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.6);
            white-space: nowrap;
            margin-bottom: 3px;
            display: flex;
            align-items: center;
            gap: 4px;
        }
        .snap-traffic-avatar {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            border: 2.5px solid #ffffff;
            background: #ef4444;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            box-shadow: 0 0 16px rgba(239, 68, 68, 0.9);
        }
        @keyframes traffic-bounce {
            0%, 100% { transform: translate(-50%, -50%) translateY(0); }
            50% { transform: translate(-50%, -50%) translateY(-5px); }
        }

        @keyframes pulse-ring {
            0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.7); }
            70% { box-shadow: 0 0 0 15px rgba(56, 189, 248, 0); }
            100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0); }
        }
    </style>
</head>
<body>
    <div id="map" class="${if (isDark) "dark-tiles" else ""}"></div>
    <script>
        var map = L.map('map', {
            center: [$userLat, $userLng],
            zoom: 15,
            zoomControl: false,
            attributionControl: false
        });

        var currentTileLayer = L.tileLayer('$tileUrl', {
            maxZoom: 19,
            subdomains: 'abc'
        }).addTo(map);

        var userMarker = null;
        var friendMarkers = {};

        function updateMapData(userLat, userLng, accuracy, isGhostMode, vibeEmoji, username, avatarUrl, friends, tileUrl, isDark) {
            var mapEl = document.getElementById('map');
            if (mapEl) {
                if (isDark) {
                    mapEl.classList.add('dark-tiles');
                } else {
                    mapEl.classList.remove('dark-tiles');
                }
            }

            if (currentTileLayer && tileUrl && currentTileLayer._url !== tileUrl) {
                map.removeLayer(currentTileLayer);
                currentTileLayer = L.tileLayer(tileUrl, { maxZoom: 19, subdomains: 'abc' }).addTo(map);
            }

            // Update user marker
            if (!isGhostMode) {
                var userIconHtml = '<div class="snap-user-marker">' +
                    '<div class="snap-user-bubble">' + vibeEmoji + ' ' + username + ' (Moi)</div>' +
                    '<div class="snap-user-avatar">' +
                    (avatarUrl ? '<img src="' + avatarUrl + '" />' : vibeEmoji) +
                    '</div></div>';

                var userIcon = L.divIcon({
                    html: userIconHtml,
                    className: '',
                    iconSize: [44, 70],
                    iconAnchor: [22, 50]
                });

                if (!userMarker) {
                    userMarker = L.marker([userLat, userLng], { icon: userIcon, zIndexOffset: 1000 }).addTo(map);
                } else {
                    userMarker.setLatLng([userLat, userLng]);
                    userMarker.setIcon(userIcon);
                }
            } else if (userMarker) {
                map.removeLayer(userMarker);
                userMarker = null;
            }

            // Update friends markers
            var existingIds = {};
            if (friends && friends.length) {
                friends.forEach(function(f) {
                    existingIds[f.id] = true;
                    var friendIconHtml = '<div class="snap-friend-marker" onclick="AndroidBridge.onFriendClicked(\'' + f.id + '\')">' +
                        '<div class="snap-friend-bubble">' + f.statusEmoji + ' ' + f.displayName + '</div>' +
                        '<div class="snap-friend-avatar" style="border-color: ' + (f.isOnline ? '#10b981' : '#64748b') + '">' +
                        (f.avatarUrl ? '<img src="' + f.avatarUrl + '" />' : f.displayName.charAt(0)) +
                        '</div></div>';

                    var friendIcon = L.divIcon({
                        html: friendIconHtml,
                        className: '',
                        iconSize: [40, 65],
                        iconAnchor: [20, 48]
                    });

                    if (friendMarkers[f.id]) {
                        friendMarkers[f.id].setLatLng([f.lat, f.lng]);
                        friendMarkers[f.id].setIcon(friendIcon);
                    } else {
                        var marker = L.marker([f.lat, f.lng], { icon: friendIcon }).addTo(map);
                        marker.on('click', function() {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onFriendClicked(f.id);
                            }
                        });
                        friendMarkers[f.id] = marker;
                    }
                });
            }

            // Cleanup removed markers
            Object.keys(friendMarkers).forEach(function(id) {
                if (!existingIds[id]) {
                    map.removeLayer(friendMarkers[id]);
                    delete friendMarkers[id];
                }
            });
        }

        var trafficMarkers = [];
        var trafficCircles = [];

        function updateTrafficJams(jams) {
            trafficMarkers.forEach(function(m) { map.removeLayer(m); });
            trafficCircles.forEach(function(c) { map.removeLayer(c); });
            trafficMarkers = [];
            trafficCircles = [];

            if (!jams || !jams.length) return;

            jams.forEach(function(j) {
                var iconHtml = '<div class="snap-traffic-marker" onclick="if(window.AndroidBridge) window.AndroidBridge.onTrafficJamClicked(\'' + j.id + '\')">' +
                    '<div class="snap-traffic-bubble">🚗 Ralentissement</div>' +
                    '<div class="snap-traffic-avatar">⚠️</div></div>';

                var icon = L.divIcon({
                    html: iconHtml,
                    className: '',
                    iconSize: [40, 65],
                    iconAnchor: [20, 48]
                });

                var marker = L.marker([j.lat, j.lng], { icon: icon, zIndexOffset: 900 }).addTo(map);
                marker.on('click', function() {
                    if (window.AndroidBridge) window.AndroidBridge.onTrafficJamClicked(j.id);
                });
                trafficMarkers.push(marker);

                var circle = L.circle([j.lat, j.lng], {
                    color: '#ef4444',
                    fillColor: '#ef4444',
                    fillOpacity: 0.22,
                    radius: 300,
                    weight: 2
                }).addTo(map);
                trafficCircles.push(circle);
            });
        }

        map.on('click', function() {
            if (window.AndroidBridge) {
                window.AndroidBridge.onMapClicked();
            }
        });

        function centerOnUser(lat, lng) {
            map.flyTo([lat, lng], 16, { animate: true, duration: 1.2 });
        }

        function centerOnFriend(lat, lng) {
            map.flyTo([lat, lng], 17, { animate: true, duration: 1.0 });
        }
    </script>
</body>
</html>
    """.trimIndent()
}

@Composable
fun SelectedFriendDetailCard(
    friend: FriendLocation,
    onClose: () -> Unit,
    onWave: () -> Unit,
    onChillWave: () -> Unit,
    onChat: () -> Unit,
    onViewProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Avatar, Name, Distance & District
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!friend.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = friend.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = friend.displayName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(
                            userName = friend.username,
                            isVerified = friend.isVerified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(friend.statusEmoji, fontSize = 16.sp)
                    }
                    Text(
                        text = "@${friend.username} • 📍 ${friend.distanceKm} km (${friend.district})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Favorite button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (friend.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favori",
                        tint = if (friend.isFavorite) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                    )
                }

                // Close button
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chill status bubble
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = friend.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    if (friend.currentlyPlayingMusic != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎶", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = friend.currentlyPlayingMusic,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA78BFA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats badges: Battery, Streak, Online status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔋", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${friend.batteryPercent}%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔥", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Série ${friend.streakDays}j", fontSize = 12.sp, color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            friend.lastSeenFormatted,
                            fontSize = 12.sp,
                            color = if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Onde Chill ✨, Chat 💬, Profile 👤
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChillWave,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("✨ Onde Chill", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Button(
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("💬 Chat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("👤 Profil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FriendsQuickCarousel(
    friends: List<FriendLocation>,
    onSelectFriend: (FriendLocation) -> Unit,
    onWaveFriend: (FriendLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(friends) { friend ->
            Surface(
                onClick = { onSelectFriend(friend) },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 8.dp,
                modifier = Modifier.width(170.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, if (friend.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!friend.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = friend.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = friend.displayName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📍 ${friend.distanceKm} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(friend.statusEmoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    friend.activityTag,
                                    fontSize = 9.sp,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onWaveFriend(friend) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("✨", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VibeStatusEditorDialog(
    currentVibe: com.example.data.UserVibe,
    onDismiss: () -> Unit,
    onSave: (emoji: String, text: String, type: String) -> Unit
) {
    var selectedEmoji by remember { mutableStateOf(currentVibe.emoji) }
    var statusText by remember { mutableStateOf(currentVibe.text) }
    var selectedType by remember { mutableStateOf(currentVibe.activityType) }

    val presetEmojis = listOf("🚀", "☕", "💻", "🎮", "📚", "🎧", "🔥", "✨", "🍕", "😴")
    val presetTypes = listOf("Dispo", "Dev", "Chill", "Musique", "Gaming", "Focus")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✨ Ma Vibe sur le Radar IDDET", fontWeight = FontWeight.Black)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Choisis ton emoji et ton statut d'activité visible par tes amis à proximité.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Emoji picker row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetEmojis) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            onClick = { selectedEmoji = emoji },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Activity tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetTypes) { type ->
                        val isSelected = type == selectedType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                if (statusText.isBlank() || statusText.startsWith("En mode")) {
                                    statusText = "En mode $type • Venez parler !"
                                }
                            },
                            label = { Text(type, fontSize = 12.sp) }
                        )
                    }
                }

                // Text field
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("Message de statut") },
                    placeholder = { Text("ex. En train de tester le nouveau code...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedEmoji, statusText, selectedType) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

class FriendsMapWebViewClient(
    private val onPageFinishedAction: () -> Unit,
    private val onRenderCrashedAction: () -> Unit
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedAction()
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        try {
            (view?.parent as? android.view.ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        onRenderCrashedAction()
        return true
    }
}
