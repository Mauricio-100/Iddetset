package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.utils.CatSoundPlayer

class PushNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PushNotificationReceiver"
        private const val CHANNEL_ID = "cmo_activity_channel"
        private const val CHANNEL_NAME = "Activités CMO"
        private const val CHANNEL_DESC = "Notifications CMO (Likes, Commentaires, Messages, Abonnements)"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered with action: ${intent.action}")

        val title = intent.getStringExtra("title") ?: "IDDET Notification"
        val message = intent.getStringExtra("message") ?: intent.getStringExtra("body") ?: "Nouveau message d'IDDET"
        val deepLinkStr = intent.getStringExtra("deep_link") ?: intent.getStringExtra("route") ?: ""

        // Play the custom meow sound!
        try {
            CatSoundPlayer.playCuteMeow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play meow sound", e)
        }

        // Show system notification
        showNotification(context, title, message, deepLinkStr)
    }

    private fun showNotification(context: Context, title: String, message: String, deepLinkStr: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure notification channel is initialized
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                val newChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                }
                notificationManager.createNotificationChannel(newChannel)
            }
        }

        // Create intent and set deep link
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLinkStr.isNotEmpty()) {
                if (deepLinkStr.startsWith("http") || deepLinkStr.startsWith("https")) {
                    data = Uri.parse(deepLinkStr)
                } else {
                    putExtra("route", deepLinkStr)
                    // If it's a relative path prefix like "s/actfile/some_id", convert to full URI
                    if (deepLinkStr.startsWith("s/actfile/") || deepLinkStr.startsWith("/s/actfile/")) {
                        val cleanPath = deepLinkStr.trimStart('/')
                        data = Uri.parse("https://iddet.gopu.inc/$cleanPath")
                    } else if (deepLinkStr.startsWith("discussion/")) {
                        val actfileId = deepLinkStr.substringAfter("discussion/")
                        data = Uri.parse("https://iddet.gopu.inc/s/actfile/$actfileId")
                    }
                }
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val notificationId = System.currentTimeMillis().hashCode()
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags)

        val appLogo = com.example.utils.HideItProManager.getNotificationLargeIconBitmap(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(appLogo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#DC2626"))
            .setSubText("S-3 CMO IDDET")

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notification posted successfully with ID: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }
}
