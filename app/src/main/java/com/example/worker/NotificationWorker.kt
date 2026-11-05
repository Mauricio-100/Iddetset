package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Notification
import com.example.data.RetrofitClient
import com.example.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)
            val userId = prefs.getString("user_id", "") ?: ""

            if (token.isNullOrEmpty()) {
                Log.d("NotificationWorker", "No auth token saved, skipping worker execution.")
                return Result.success()
            }

            val database = AppDatabase.getDatabase(appContext)
            val notificationDao = database.notificationDao()

            val remoteNotifications = RetrofitClient.apiService.getNotifications("Bearer $token")
            val mapped = remoteNotifications.map {
                Notification(
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

            // Save in Room database
            notificationDao.insertNotifications(mapped)

            val shownNotifications = prefs.getStringSet("shown_notification_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            var updated = false
            val newUnread = mapped.filter { !it.isRead && !shownNotifications.contains(it.id) }

            // Trigger system notifications with sound and routing
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
                    context = appContext,
                    notificationId = notif.id,
                    title = title,
                    text = descriptiveText,
                    route = route,
                    avatarUrl = notif.fromAvatar,
                    senderName = notif.fromUsername
                )
            }

            newUnread.forEach { notif ->
                shownNotifications.add(notif.id)
                updated = true
            }

            if (updated) {
                prefs.edit().putStringSet("shown_notification_ids", shownNotifications).apply()
            }

            Log.d("NotificationWorker", "Successfully processed ${newUnread.size} new notifications.")
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error executing background notification worker", e)
            Result.retry()
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

    companion object {
        const val WORK_NAME = "iddet_notification_sync_worker"
    }
}
