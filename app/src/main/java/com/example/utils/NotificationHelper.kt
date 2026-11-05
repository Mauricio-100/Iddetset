package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.mutableStateOf

object NotificationRouter {
    val pendingRoute = mutableStateOf<String?>(null)
}

object NotificationHelper {
    private const val CHANNEL_ID = "cmo_activity_channel"
    private const val CHANNEL_NAME = "Activités CMO"
    private const val CHANNEL_DESC = "Notifications CMO (Likes, Commentaires, Messages, Abonnements)"

    const val CALL_CHANNEL_ID = "iddet_incoming_calls_channel"
    private const val CALL_CHANNEL_NAME = "Appels vocaux IDDET"
    private const val CALL_CHANNEL_DESC = "Alertes d'appels entrants WhatsApp-style avec sonnerie"
    const val INCOMING_CALL_NOTIF_ID = 998877

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedUriString = prefs.getString("notification_ringtone_uri", null)
            val vibrationEnabled = prefs.getBoolean("notification_vibration_enabled", true)
            
            val soundUri: android.net.Uri? = if (savedUriString == "silent") {
                null
            } else if (!savedUriString.isNullOrEmpty()) {
                android.net.Uri.parse(savedUriString)
            } else {
                android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.cat_law}")
            }

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(vibrationEnabled)
                    if (vibrationEnabled) {
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                    } else {
                        vibrationPattern = longArrayOf(0)
                    }
                    setSound(soundUri, audioAttributes)
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            // Dedicated High Priority Incoming Call Channel (WhatsApp Style)
            val existingCallChannel = manager.getNotificationChannel(CALL_CHANNEL_ID)
            if (existingCallChannel == null) {
                val callSoundUri = android.media.RingtoneManager.getActualDefaultRingtoneUri(context, android.media.RingtoneManager.TYPE_RINGTONE)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

                val callAudioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()

                val callChannel = NotificationChannel(
                    CALL_CHANNEL_ID,
                    CALL_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CALL_CHANNEL_DESC
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 1000, 1000, 1000)
                    setSound(callSoundUri, callAudioAttributes)
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(callChannel)
            }
        }
    }

    /**
     * Show WhatsApp-style Incoming Call Notification with heads-up display,
     * Answer / Decline actions and Quick Reply presets ("Rappelle-moi plus tard", etc.)
     */
    suspend fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerUsername: String,
        callerAvatar: String?
    ) {
        withContext(Dispatchers.IO) {
            initChannels(context)

            val pFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val mutFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // FullScreen / Content Intent: open app and show call overlay
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "call_active")
                putExtra("incoming_call_id", callId)
                putExtra("incoming_caller_id", callerId)
                putExtra("incoming_caller_username", callerUsername)
                putExtra("incoming_caller_avatar", callerAvatar)
            }
            val contentPendingIntent = PendingIntent.getActivity(context, 101, openAppIntent, pFlags)

            // 1. Answer Action Intent
            val answerIntent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
                action = com.example.receiver.CallActionReceiver.ACTION_ANSWER_CALL
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALL_ID, callId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_ID, callerId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_USERNAME, callerUsername)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_AVATAR, callerAvatar)
            }
            val answerPendingIntent = PendingIntent.getBroadcast(context, 102, answerIntent, pFlags)

            // 2. Decline Action Intent
            val declineIntent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
                action = com.example.receiver.CallActionReceiver.ACTION_DECLINE_CALL
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALL_ID, callId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_ID, callerId)
            }
            val declinePendingIntent = PendingIntent.getBroadcast(context, 103, declineIntent, pFlags)

            // 3. Quick Reply 1: "Rappelle-moi plus tard"
            val quickReply1Intent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
                action = com.example.receiver.CallActionReceiver.ACTION_QUICK_REPLY
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALL_ID, callId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_ID, callerId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_USERNAME, callerUsername)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_MESSAGE, "Rappelle-moi plus tard")
            }
            val quickReply1PendingIntent = PendingIntent.getBroadcast(context, 104, quickReply1Intent, pFlags)

            // 4. Quick Reply 2: "Rappelle-moi dans quelques minutes"
            val quickReply2Intent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
                action = com.example.receiver.CallActionReceiver.ACTION_QUICK_REPLY
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALL_ID, callId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_ID, callerId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_USERNAME, callerUsername)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_MESSAGE, "Rappelle-moi dans quelques minutes")
            }
            val quickReply2PendingIntent = PendingIntent.getBroadcast(context, 105, quickReply2Intent, pFlags)

            // 5. Custom Inline RemoteInput Quick Reply (Allows user to type anything from notification)
            val remoteInput = androidx.core.app.RemoteInput.Builder(com.example.receiver.CallActionReceiver.EXTRA_REMOTE_INPUT_TEXT)
                .setLabel("Message rapide...")
                .build()

            val customReplyIntent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
                action = com.example.receiver.CallActionReceiver.ACTION_QUICK_REPLY
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALL_ID, callId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_ID, callerId)
                putExtra(com.example.receiver.CallActionReceiver.EXTRA_CALLER_USERNAME, callerUsername)
            }
            val customReplyPendingIntent = PendingIntent.getBroadcast(context, 106, customReplyIntent, mutFlags)

            val customReplyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                "Message",
                customReplyPendingIntent
            ).addRemoteInput(remoteInput).build()

            // Avatar Bitmap
            val avatarBitmap = if (!callerAvatar.isNullOrEmpty()) {
                downloadAvatarOrPlaceholder(com.example.utils.UrlHelper.fixCloudinaryUrl(callerAvatar) ?: callerAvatar, callerUsername)
            } else {
                generatePlaceholderAvatar(callerUsername)
            }

            val callSoundUri = android.media.RingtoneManager.getActualDefaultRingtoneUri(context, android.media.RingtoneManager.TYPE_RINGTONE)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

            val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(avatarBitmap)
                .setContentTitle("Appel entrant : @$callerUsername")
                .setContentText("Appel vocal IDDET en cours...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setColor(Color.parseColor("#22C55E")) // Green
                .setContentIntent(contentPendingIntent)
                .setFullScreenIntent(contentPendingIntent, true)
                .setSound(callSoundUri)
                .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
                // Action Buttons
                .addAction(R.drawable.ic_notification, "Décrocher", answerPendingIntent)
                .addAction(R.drawable.ic_notification, "Refuser", declinePendingIntent)
                .addAction(R.drawable.ic_notification, "Plus tard", quickReply1PendingIntent)
                .addAction(customReplyAction)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(INCOMING_CALL_NOTIF_ID, notification)
        }
    }

    fun cancelIncomingCallNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(INCOMING_CALL_NOTIF_ID)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to cancel incoming call notification", e)
        }
    }

    suspend fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerUsername: String,
        callerAvatar: String? = null
    ) {
        withContext(Dispatchers.IO) {
            initChannels(context)
            cancelIncomingCallNotification(context)

            val pFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Click opens chat or profile with caller
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "chat_user")
                putExtra("user_id", callerId)
                putExtra("username", callerUsername)
            }
            val contentPendingIntent = PendingIntent.getActivity(context, callId.hashCode(), openAppIntent, pFlags)

            val avatarBitmap = if (!callerAvatar.isNullOrEmpty()) {
                downloadAvatarOrPlaceholder(com.example.utils.UrlHelper.fixCloudinaryUrl(callerAvatar) ?: callerAvatar, callerUsername)
            } else {
                generatePlaceholderAvatar(callerUsername)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(avatarBitmap)
                .setContentTitle("Appel manqué")
                .setContentText("@$callerUsername a tenté de vous appeler")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#EF4444"))
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_notification, "Message", contentPendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify((callId + "_missed").hashCode(), notification)
        }
    }

    fun updateNotificationSound(context: Context, soundUri: android.net.Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                manager.deleteNotificationChannel(CHANNEL_ID)
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Failed to delete notification channel for sound update", e)
            }
            initChannels(context)
        }
    }

    suspend fun showSystemNotification(
        context: Context,
        notificationId: String,
        title: String,
        text: String,
        route: String,
        avatarUrl: String? = null,
        senderName: String? = null
    ) {
        withContext(Dispatchers.IO) {
            initChannels(context)

            // Convert string notificationId to an integer hash for NotificationManager
            val idHash = notificationId.hashCode()

            // Intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", route)
            }

            // Create PendingIntent
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, idHash, intent, flags)

            // Generate/Load large icon (app logo on the right as requested)
            val appLogo = getAppLogoBitmap(context)

            // Generate avatar for MessagingStyle if it's a social interaction
            val avatarIcon = if (!avatarUrl.isNullOrEmpty()) {
                downloadAvatarOrPlaceholder(com.example.utils.UrlHelper.fixCloudinaryUrl(avatarUrl) ?: avatarUrl, senderName ?: "?")
            } else {
                generatePlaceholderAvatar(senderName ?: "?")
            }

            // Standardized small icon: Pure monochrome, transparent-background silhouette for the status bar (WhatsApp-style standard)
            val smallIconRes = R.drawable.ic_notification
            val brandColor = Color.parseColor("#DC2626") // Standardized brand accent

            // Use MessagingStyle for social interactions (likes, comments, follows, messages)
            val user = androidx.core.app.Person.Builder()
                .setName(senderName ?: "Quelqu'un")
                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(avatarIcon))
                .build()

            val style = NotificationCompat.MessagingStyle(user)
                .addMessage(text, System.currentTimeMillis(), user)
                .setConversationTitle(title)

            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedUriString = prefs.getString("notification_ringtone_uri", null)
            val vibrationEnabled = prefs.getBoolean("notification_vibration_enabled", true)
            val meowEnabled = prefs.getBoolean("notification_meow_enabled", true)

            val customSoundUri: android.net.Uri? = if (savedUriString == "silent") {
                null
            } else if (!savedUriString.isNullOrEmpty()) {
                android.net.Uri.parse(savedUriString)
            } else {
                android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.cat_law}")
            }

            // Build beautiful custom notification with pure transparent background status bar icon
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setLargeIcon(appLogo) // App logo badge
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(style)
                .setColor(brandColor) // Dynamic branding color
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)

            if (customSoundUri != null) {
                builder.setSound(customSoundUri)
            }
            if (vibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 250, 250, 250))
            } else {
                builder.setVibrate(longArrayOf(0))
            }

            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSubText("S-3 CMO IDDET")

            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(idHash, builder.build())
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Failed to show system notification", e)
            }

            // Play cute kitten meow sound if enabled
            if (meowEnabled) {
                CatSoundPlayer.playCuteMeow()
            }
        }
    }

    suspend fun showDiscoveryNotification(
        context: Context,
        notificationId: String,
        title: String,
        text: String,
        route: String,
        subText: String = "IDDET Découverte",
        avatarUrl: String? = null
    ) {
        withContext(Dispatchers.IO) {
            initChannels(context)

            val idHash = notificationId.hashCode()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", route)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, idHash, intent, flags)

            val appLogo = getAppLogoBitmap(context)
            val smallIconRes = R.drawable.ic_notification
            val brandColor = Color.parseColor("#DC2626")

            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedUriString = prefs.getString("notification_ringtone_uri", null)
            val vibrationEnabled = prefs.getBoolean("notification_vibration_enabled", true)
            val meowEnabled = prefs.getBoolean("notification_meow_enabled", true)

            val customSoundUri: android.net.Uri? = if (savedUriString == "silent") {
                null
            } else if (!savedUriString.isNullOrEmpty()) {
                android.net.Uri.parse(savedUriString)
            } else {
                android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.cat_law}")
            }

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(text)
                .setSummaryText(subText)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setLargeIcon(appLogo)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setColor(brandColor)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSubText(subText)

            if (customSoundUri != null) {
                builder.setSound(customSoundUri)
            }
            if (vibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 200, 150, 200))
            } else {
                builder.setVibrate(longArrayOf(0))
            }

            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(idHash, builder.build())
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Failed to show discovery notification", e)
            }

            if (meowEnabled) {
                CatSoundPlayer.playCuteMeow()
            }
        }
    }

    private fun getAppLogoBitmap(context: Context): Bitmap? {
        return HideItProManager.getNotificationLargeIconBitmap(context)
    }

    private fun downloadAvatarOrPlaceholder(avatarUrl: String, senderName: String): Bitmap {
        return try {
            val url = URL(avatarUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input: InputStream = connection.inputStream
            val original = BitmapFactory.decodeStream(input)
            if (original != null) {
                val circular = getCircularBitmap(original)
                if (circular != original) {
                    original.recycle()
                }
                circular
            } else {
                generatePlaceholderAvatar(senderName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generatePlaceholderAvatar(senderName)
        }
    }

    private fun generatePlaceholderAvatar(name: String): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a soft colored circular background based on sender's name hash
        val paint = Paint().apply {
            isAntiAlias = true
            color = getSoftColorForName(name)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw initials text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)

        return bitmap
    }

    private fun getCircularBitmap(src: Bitmap): Bitmap {
        val size = Math.min(src.width, src.height)
        val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val rect = Rect(0, 0, size, size)
        val srcRect = Rect((src.width - size) / 2, (src.height - size) / 2, (src.width + size) / 2, (src.height + size) / 2)
        canvas.drawBitmap(src, srcRect, rect, paint)
        return dst
    }

    private fun getSoftColorForName(name: String): Int {
        val colors = intArrayOf(
            Color.parseColor("#1877F2"), // Facebook Blue
            Color.parseColor("#42B72A"), // Green
            Color.parseColor("#E4405F"), // Pink
            Color.parseColor("#FF9900"), // Yellow-orange
            Color.parseColor("#7F3DFF"), // Purple
            Color.parseColor("#00C6FF")  // Cyan
        )
        val index = Math.abs(name.hashCode()) % colors.size
        return colors[index]
    }
}
