package com.iddet.calls

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Reçoit les push FCM (messages "data" uniquement => onMessageReceived est TOUJOURS appelé,
 * même app fermée). Un appel entrant s'affiche en plein écran, comme un vrai appel.
 */
class IddetFirebaseService : FirebaseMessagingService() {

    companion object {
        const val CH_CALLS = "calls"
        const val CH_MSG = "messages"
        const val ACTION_ACCEPT = "com.iddet.calls.ACCEPT"
        const val ACTION_DECLINE = "com.iddet.calls.DECLINE"
        const val EXTRA_CALL_ID = "call_id"
        private const val CALL_NOTIF_ID = 4242

        fun ensureChannels(ctx: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_CALLS, "Appels", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build())
                enableVibration(true)
            })
            nm.createNotificationChannel(NotificationChannel(CH_MSG, "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        }

        fun cancelCallNotification(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(CALL_NOTIF_ID)

        /** Envoie le jeton FCM au serveur. À appeler après chaque login ET dans onNewToken. */
        fun registerToken(authToken: String, fcmToken: String, baseUrl: String = CallManager.DEFAULT_BASE_URL) {
            Thread {
                runCatching {
                    val json = """{"token":"$fcmToken","platform":"android"}"""
                    val req = Request.Builder().url("$baseUrl/api/devices/register")
                        .header("Authorization", "Bearer $authToken")
                        .post(json.toRequestBody("application/json".toMediaType())).build()
                    OkHttpClient().newCall(req).execute().close()
                }
            }.start()
        }
    }

    override fun onNewToken(token: String) {
        // Récupère ton JWT depuis ton stockage (SharedPreferences / DataStore) :
        val auth = getSharedPreferences("iddet", MODE_PRIVATE).getString("access_token", null) ?: return
        registerToken(auth, token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        try {
            ensureChannels(this)
            val d = msg.data
            when (d["type"]) {
                "call_invite" -> showIncomingCall(d["call_id"] ?: return, d["caller_username"] ?: "Appel entrant")
                "call_ended"  -> cancelCallNotification(this)       // arrête la sonnerie même sans WebSocket
                "new_message" -> showMessage(d["sender_username"] ?: "Nouveau message", d["preview"] ?: "")
            }
        } catch (t: Throwable) { android.util.Log.e("IddetFcm", "onMessageReceived", t) }   // jamais de crash ici
    }

    private fun showIncomingCall(callId: String, caller: String) {
        val full = PendingIntent.getActivity(this, 1,
            Intent(this, IncomingCallActivity::class.java).putExtra(EXTRA_CALL_ID, callId).putExtra("caller", caller)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(a: String, code: Int) = PendingIntent.getBroadcast(this, code,
            Intent(a).setPackage(packageName).putExtra(EXTRA_CALL_ID, callId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val n = NotificationCompat.Builder(this, CH_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(caller).setContentText("Appel vocal entrant")
            .setCategory(NotificationCompat.CATEGORY_CALL).setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(full, true).setOngoing(true).setAutoCancel(false)
            .setTimeoutAfter(45_000)                                   // = délai serveur
            .addAction(0, "Refuser", action(ACTION_DECLINE, 2))
            .addAction(0, "Répondre", action(ACTION_ACCEPT, 3))
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(CALL_NOTIF_ID, n) }   // SecurityException si notifs refusées
    }

    private fun showMessage(from: String, text: String) {
        val open = PendingIntent.getActivity(this, 5,
            packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CH_MSG).setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(from).setContentText(text).setContentIntent(open).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(this).notify(from.hashCode(), n) }
    }
}
