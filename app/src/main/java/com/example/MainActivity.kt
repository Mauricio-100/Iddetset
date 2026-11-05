package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.IddetRepository
import com.example.ui.IddetViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppTheme
import android.os.Build
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import coil.decode.SvgDecoder
import coil.Coil

class MainActivity : ComponentActivity() {
    private var crashError by mutableStateOf<String?>(null)

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return

        val acceptCallId = intent.getStringExtra("accept_call")
        if (!acceptCallId.isNullOrBlank()) {
            com.example.utils.CallManager.acceptCall(acceptCallId)
            com.iddet.calls.CallManagerHolder.get(this)?.accept(acceptCallId)
        }

        val route = intent.getStringExtra("route")
        if (route != null) {
            com.example.utils.NotificationRouter.pendingRoute.value = route
        } else {
            val uri = intent.data
            if (uri != null) {
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase() ?: ""
                val pathSegments = uri.pathSegments ?: emptyList()

                // 1. Custom iddet:// scheme
                if (scheme == "iddet") {
                    when (host) {
                        "profile", "u", "user" -> {
                            val username = pathSegments.firstOrNull()
                            if (!username.isNullOrBlank()) {
                                com.example.utils.NotificationRouter.pendingRoute.value = "profile/$username"
                                return
                            }
                        }
                        "community", "c" -> {
                            val slug = pathSegments.firstOrNull()
                            if (!slug.isNullOrBlank()) {
                                com.example.utils.NotificationRouter.pendingRoute.value = "community/$slug"
                                return
                            }
                        }
                        "actfile", "discussion" -> {
                            val id = pathSegments.firstOrNull()
                            if (!id.isNullOrBlank()) {
                                com.example.utils.NotificationRouter.pendingRoute.value = "discussion/$id"
                                return
                            }
                        }
                        "sounds", "sound", "music" -> {
                            com.example.utils.NotificationRouter.pendingRoute.value = "music"
                            return
                        }
                        "video", "videos", "reels", "wings" -> {
                            com.example.utils.NotificationRouter.pendingRoute.value = "reels"
                            return
                        }
                        "messages", "chat" -> {
                            val peer = pathSegments.firstOrNull()
                            if (!peer.isNullOrBlank()) {
                                com.example.utils.NotificationRouter.pendingRoute.value = "chat/$peer"
                            } else {
                                com.example.utils.NotificationRouter.pendingRoute.value = "messages"
                            }
                            return
                        }
                        "home" -> {
                            com.example.utils.NotificationRouter.pendingRoute.value = "home"
                            return
                        }
                    }
                }

                // 2. HTTP/HTTPS Web URLs (hoosthubs-g.onrender.com, iddet.app, etc.)
                if (pathSegments.isNotEmpty()) {
                    val first = pathSegments[0].lowercase()
                    when {
                        // User Profile: /u/{username}
                        first == "u" && pathSegments.size >= 2 -> {
                            val username = pathSegments[1]
                            com.example.utils.NotificationRouter.pendingRoute.value = "profile/$username"
                            return
                        }
                        // Community: /c/{slug} or /community/{slug}
                        (first == "c" || first == "community") && pathSegments.size >= 2 -> {
                            val slug = pathSegments[1]
                            com.example.utils.NotificationRouter.pendingRoute.value = "community/$slug"
                            return
                        }
                        // Actfile: /actfile/{id} or /s/actfile/{id}
                        first == "actfile" && pathSegments.size >= 2 -> {
                            val actfileId = pathSegments[1]
                            com.example.utils.NotificationRouter.pendingRoute.value = "discussion/$actfileId"
                            return
                        }
                        first == "s" && pathSegments.size >= 3 && pathSegments[1] == "actfile" -> {
                            val actfileId = pathSegments[2]
                            com.example.utils.NotificationRouter.pendingRoute.value = "discussion/$actfileId"
                            return
                        }
                        // Sound / Music: /sounds/{id} or /sounds
                        first == "sounds" || first == "sound" -> {
                            com.example.utils.NotificationRouter.pendingRoute.value = "music"
                            return
                        }
                        // Video: /video/{id} or /videos
                        first == "video" || first == "videos" || first == "reels" -> {
                            com.example.utils.NotificationRouter.pendingRoute.value = "reels"
                            return
                        }
                        // Messages: /messages/{username}
                        first == "messages" -> {
                            if (pathSegments.size >= 2) {
                                com.example.utils.NotificationRouter.pendingRoute.value = "chat/${pathSegments[1]}"
                            } else {
                                com.example.utils.NotificationRouter.pendingRoute.value = "messages"
                            }
                            return
                        }
                    }
                }

                // Check slug extractor fallback
                val communitySlug = com.example.utils.CommunitySlugHelper.extractSlugFromUrl(uri.toString())
                if (!communitySlug.isNullOrBlank()) {
                    com.example.utils.NotificationRouter.pendingRoute.value = "community/$communitySlug"
                    return
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageLoader = ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .respectCacheHeaders(false)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            android.util.Log.e("CRASH_TAG", "App crashed", e)
            runOnUiThread {
                crashError = e.javaClass.simpleName + ": " + e.message + "\n\n" + stackTrace
            }
        }

        enableEdgeToEdge()
        com.example.utils.LocalAiManager.initOnce(applicationContext)
        com.example.utils.HideItProManager.initialize(applicationContext)
        com.example.utils.NetworkDiscoveryNotifier.startMonitoring(applicationContext)
        com.example.utils.NotificationHelper.initChannels(applicationContext)
        com.iddet.calls.IddetFirebaseService.ensureChannels(applicationContext)

        val permsToRequest = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permsToRequest.add(permission)
            }
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (permsToRequest.isNotEmpty()) {
            requestPermissions(permsToRequest.toTypedArray(), 101)
        }
        
        handleIntent(intent)
        
        try {
            // Schedule WorkManager for background notifications (WhatsApp-like background polling & persistent ringtone alerts)
            try {
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                
                val periodicWork = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.NotificationWorker>(
                    15, java.util.concurrent.TimeUnit.MINUTES
                ).setConstraints(constraints).build()

                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                    com.example.worker.NotificationWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )

                val immediateWork = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.NotificationWorker>()
                    .setConstraints(constraints)
                    .build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueue(immediateWork)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to schedule NotificationWorker", e)
            }
            
            try {
                val serviceIntent = android.content.Intent(this, com.example.worker.MessageSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start MessageSyncService", e)
            }

            val db = AppDatabase.getDatabase(this)
            val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val repository = IddetRepository(
                db.userDao(),
                db.actfileDao(),
                db.messageDao(),
                db.followDao(),
                db.commentDao(),
                db.notificationDao(),
                db.savedAccountDao(),
                db.channelMessageDao(),
                prefs
            )

            try {
                com.example.utils.CallManager.initialize(applicationContext, repository)
                repository.currentUser.value?.id?.let { uid ->
                    com.example.utils.CallManager.setCurrentUser(uid)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to initialize CallManager", e)
            }

            // Register FCM token and initialize solid CallManager v2
            val jwt = prefs.getString("auth_token", null)
            val myId = repository.currentUser.value?.id ?: prefs.getString("user_id", "") ?: ""
            if (!jwt.isNullOrBlank()) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    com.iddet.calls.IddetFirebaseService.registerToken(jwt, fcmToken)
                }
                if (myId.isNotBlank()) {
                    val cm = com.iddet.calls.CallManager(
                        context = applicationContext,
                        token = jwt,
                        myUserId = myId,
                        listener = object : com.iddet.calls.CallManager.Listener {
                            override fun onIncoming(callId: String, callerId: String, username: String, avatar: String?) {
                                com.example.utils.CallManager.handleIncomingCall(callId, callerId, username, avatar)
                            }
                            override fun onAccepted(callId: String) {}
                            override fun onFinished(callId: String, status: String) {
                                com.iddet.calls.IddetFirebaseService.cancelCallNotification(applicationContext)
                                com.example.utils.CallRingtonePlayer.stopRinging()
                                com.example.utils.CallManager.endCall()
                            }
                            override fun onError(message: String) {
                                android.util.Log.e("CallManagerV2", "Error: $message")
                            }
                        }
                    )
                    com.iddet.calls.CallManagerHolder.instance = cm
                    cm.start()
                }
            }
            
            val factory = MainViewModelFactory(repository)

            setContent {
                val viewModel: IddetViewModel = viewModel(factory = factory)
                val appTheme by viewModel.selectedTheme.collectAsState()
                
                MyApplicationTheme(appTheme = appTheme) {
                    if (crashError != null) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 64.dp)) {
                            Text("APP CRASHED", color = Color.Red)
                            Text(crashError ?: "", color = Color.Black)
                        }
                    } else {
                        var showSplash by remember { mutableStateOf(true) }
                        
                        if (showSplash) {
                            SplashScreen(onTimeout = { showSplash = false })
                        } else {
                            val currentUser by viewModel.currentUser.collectAsState()
                            
                            if (currentUser == null) {
                                AuthScreen(viewModel)
                            } else {
                                MainScreen(viewModel)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            crashError = android.util.Log.getStackTraceString(e)
            setContent {
                MyApplicationTheme(appTheme = AppTheme.DEFAULT) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 64.dp)) {
                        Text("APP INITIALIZATION FAILED", color = Color.Red)
                        Text(crashError ?: "", color = Color.Black)
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            Coil.imageLoader(this).memoryCache?.trimMemory(level)
        } catch (e: Exception) {
            // Ignore cache trim errors gracefully
        }
    }
}

class MainViewModelFactory(
    private val repository: com.example.data.IddetRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IddetViewModel(repository) as T
    }
}
