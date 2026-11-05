package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.ActfileMetadataHelper
import com.example.data.IddetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Monitors Wi-Fi and Mobile Data connectivity in real time.
 * When network becomes available, it intelligently notifies the user about
 * pending notifications or interesting Actfile posts / communities,
 * with direct deep-linking on click.
 */
object NetworkDiscoveryNotifier {
    private const val TAG = "NetworkDiscovery"
    private const val PREFS_NAME = "network_discovery_prefs"
    private const val KEY_LAST_NOTIF_TIME = "last_discovery_notif_time"
    private const val KEY_LAST_POST_ID = "last_notified_post_id"
    private const val NOTIF_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes cooldown

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false
    private var wasConnected = false

    fun startMonitoring(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val callback = DiscoveryNetworkCallback(appContext, connectivityManager)
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private class DiscoveryNetworkCallback(
        private val appContext: Context,
        private val connectivityManager: ConnectivityManager
    ) : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network became available")
            
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            if (hasInternet && (isWifi || isCellular)) {
                val networkType = if (isWifi) "Wi-Fi" else "Données mobiles"
                Log.d(TAG, "Connected via $networkType")

                scope.launch {
                    kotlinx.coroutines.delay(2000)
                    checkAndSendDiscoveryNotification(appContext, isWifi = isWifi, isCellular = isCellular)
                }
                wasConnected = true
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network lost")
            wasConnected = false
        }
    }

    suspend fun checkAndSendDiscoveryNotification(context: Context, isWifi: Boolean, isCellular: Boolean) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastNotifTime = prefs.getLong(KEY_LAST_NOTIF_TIME, 0L)

            // Respect cooldown to prevent spamming
            if (now - lastNotifTime < NOTIF_COOLDOWN_MS) {
                Log.d(TAG, "Skipping notification: cooldown active")
                return@withContext
            }

            val repository = IddetRepository.getInstance(context)
            val currentUserId = repository.currentUser.value?.id
            val netTypeName = if (isWifi) "Wi-Fi" else "Données mobiles"

            // 1. Check for unread notifications first
            try {
                val notifications = repository.getNotifications().firstOrNull() ?: emptyList()
                val unreadCount = notifications.count { !it.isRead }
                if (unreadCount > 0) {
                    val firstUnread = notifications.firstOrNull { !it.isRead }
                    val notifText = firstUnread?.message?.ifBlank { null }
                        ?: "Vous avez $unreadCount notification(s) en attente sur IDDET."
                    
                    NotificationHelper.showDiscoveryNotification(
                        context = context,
                        notificationId = "net_disc_unread_${System.currentTimeMillis()}",
                        title = "🔔 $unreadCount notification(s) en attente ($netTypeName)",
                        text = notifText,
                        route = "notifications",
                        subText = "Connecté en $netTypeName"
                    )

                    prefs.edit().putLong(KEY_LAST_NOTIF_TIME, now).apply()
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notifications", e)
            }

            // 2. Check for interesting / trending Actfiles (posts)
            try {
                val actfiles = repository.getAllActfiles().firstOrNull() ?: emptyList()
                val lastPostId = prefs.getString(KEY_LAST_POST_ID, null)

                // Pick an interesting post from someone else
                val interestingPost = actfiles
                    .filter { it.userId != currentUserId && it.id != lastPostId }
                    .maxByOrNull { (it.likesCount * 2) + it.viewsCount }
                    ?: actfiles.firstOrNull { it.id != lastPostId }

                if (interestingPost != null) {
                    val cleanBody = ActfileMetadataHelper.cleanContent(interestingPost.content)
                        .replace("\n", " ")
                        .trim()
                    val snippet = if (cleanBody.length > 75) cleanBody.take(72) + "..." else cleanBody

                    val notifTitle = "⚡ Post qui pourrait vous intéresser ($netTypeName)"
                    val notifText = "« $snippet » par @${interestingPost.username}"

                    NotificationHelper.showDiscoveryNotification(
                        context = context,
                        notificationId = "net_disc_post_${interestingPost.id}",
                        title = notifTitle,
                        text = notifText,
                        route = "discussion/${interestingPost.id}",
                        subText = "Connecté en $netTypeName"
                    )

                    prefs.edit()
                        .putLong(KEY_LAST_NOTIF_TIME, now)
                        .putString(KEY_LAST_POST_ID, interestingPost.id)
                        .apply()
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking actfiles", e)
            }

            // 3. Fallback: Suggest a popular community
            try {
                val suggestedCommunities = repository.searchCommunities(query = null, category = null, sort = "trending")
                val randomComm = suggestedCommunities.randomOrNull()

                if (randomComm != null) {
                    NotificationHelper.showDiscoveryNotification(
                        context = context,
                        notificationId = "net_disc_comm_${randomComm.slug}",
                        title = "🌐 Découvrez la communauté c/${randomComm.slug}",
                        text = "${randomComm.name} : ${randomComm.description?.take(70) ?: "Rejoignez la discussion !"}",
                        route = "community/${randomComm.slug}",
                        subText = "Connecté en $netTypeName"
                    )

                    prefs.edit().putLong(KEY_LAST_NOTIF_TIME, now).apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking suggested communities", e)
            }
        }
    }
}
