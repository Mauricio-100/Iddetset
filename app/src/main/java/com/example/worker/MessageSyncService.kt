package com.example.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.AppDatabase
import com.example.data.Notification as AppNotification
import com.example.data.RetrofitClient
import com.example.utils.NotificationHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale

class MessageSyncService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        pollNotifications()
        pollIncomingCalls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private val seenCallInvites = HashSet<String>()

    private fun pollIncomingCalls() {
        serviceScope.launch {
            val httpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            while (isActive) {
                try {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)
                    if (!token.isNullOrEmpty()) {
                        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                        val request = okhttp3.Request.Builder()
                            .url("https://hoosthubs-g.onrender.com/api/calls/incoming")
                            .header("Authorization", authHeader)
                            .build()
                        httpClient.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string() ?: ""
                                if (body.isNotBlank()) {
                                    val json = org.json.JSONObject(body)
                                    val arr = json.optJSONArray("calls")
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) {
                                            val c = arr.getJSONObject(i)
                                            val callId = c.getString("call_id")
                                            val callerId = c.getString("caller_id")
                                            val callerUsername = c.optString("caller_username", "Contact")
                                            val callerAvatar = if (c.has("caller_avatar") && !c.isNull("caller_avatar")) c.getString("caller_avatar") else null
                                            if (seenCallInvites.add(callId)) {
                                                com.example.utils.CallManager.handleIncomingCall(callId, callerId, callerUsername, callerAvatar)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "iddet_sync_service"
            val channel = NotificationChannel(
                channelId,
                "IDDET Synchronisation",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("IDDET")
                .setContentText("Écoute des messages en arrière-plan...")
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFDC2626.toInt())
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(1001, notification)
        }
    }

    private fun pollNotifications() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)
                    val userId = prefs.getString("user_id", "") ?: ""

                    if (!token.isNullOrEmpty()) {
                        val database = AppDatabase.getDatabase(applicationContext)
                        val notificationDao = database.notificationDao()

                        val remoteNotifications = RetrofitClient.apiService.getNotifications("Bearer $token")
                        val mapped = remoteNotifications.map {
                            AppNotification(
                                id = it.id,
                                userId = userId,
                                type = it.type,
                                fromUserId = it.from_user_id,
                                fromUsername = it.from_username ?: "Unknown",
                                fromAvatar = it.from_avatar,
                                message = it.message,
                                targetId = it.target_id,
                                isRead = it.read,
                                createdAt = parseIso(it.created_at)
                            )
                        }

                        notificationDao.insertNotifications(mapped)

                        val shownNotifications = prefs.getStringSet("shown_notification_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                        var updated = false
                        val newUnread = mapped.filter { !it.isRead && !shownNotifications.contains(it.id) }

                        newUnread.takeLast(5).forEach { notif ->
                            val route = when (notif.type) {
                                "like", "comment", "actfile_like" -> "discussion/${notif.targetId}"
                                "message" -> "chat/${notif.fromUserId}"
                                "follow" -> "profile/${notif.fromUserId}"
                                else -> "notifications"
                            }

                            val title = when (notif.type) {
                                "like", "actfile_like" -> "💖 Actfile aimé !"
                                "comment" -> "💬 Nouveau commentaire"
                                "follow" -> "🎉 Nouvel abonné sur IDDET"
                                "message" -> "📩 Message privé reçu"
                                else -> "🔔 Notification IDDET S-3"
                            }

                            val descriptiveText = when (notif.type) {
                                "like", "actfile_like" -> "${notif.fromUsername} a aimé votre fichier d'acte."
                                "comment" -> "${notif.fromUsername} a commenté : ${notif.message}"
                                "follow" -> "${notif.fromUsername} s'est abonné à votre profil."
                                "message" -> "Nouveau message de ${notif.fromUsername}."
                                else -> notif.message
                            }

                            NotificationHelper.showSystemNotification(
                                context = applicationContext,
                                notificationId = notif.id,
                                title = title,
                                text = descriptiveText,
                                route = route,
                                avatarUrl = notif.fromAvatar,
                                senderName = notif.fromUsername
                            )
                            shownNotifications.add(notif.id)
                            updated = true
                        }

                        if (updated) {
                            prefs.edit().putStringSet("shown_notification_ids", shownNotifications).apply()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore and retry
                }
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    private fun parseIso(date: String?): Long {
        if (date == null) return System.currentTimeMillis()
        return try {
            val cleanDate = date.replace("Z", "+0000")
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            format.parse(cleanDate)?.time ?: System.currentTimeMillis()
        } catch (e1: Exception) {
            try {
                val cleanDate = date.replace("Z", "+0000")
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                format.parse(cleanDate)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
