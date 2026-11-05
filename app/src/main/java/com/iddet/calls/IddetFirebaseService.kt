package com.iddet.calls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
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
        private const val TAG = "IddetFirebaseService"
        const val CH_CALLS = "calls"
        const val CH_MSG = "messages"
        const val ACTION_ACCEPT = "com.iddet.calls.ACCEPT"
        const val ACTION_DECLINE = "com.iddet.calls.DECLINE"
        const val EXTRA_CALL_ID = "call_id"
        private const val CALL_NOTIF_ID = 4242

        fun ensureChannels(ctx: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            
            val callChannel = NotificationChannel(CH_CALLS, "Appels Vocaux", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications d'appels entrants et communications IDDET"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
            nm.createNotificationChannel(callChannel)

            val msgChannel = NotificationChannel(CH_MSG, "Messages & Alertes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Messages privés et notifications IDDET"
            }
            nm.createNotificationChannel(msgChannel)
        }

        fun cancelCallNotification(ctx: Context) {
            runCatching {
                NotificationManagerCompat.from(ctx).cancel(CALL_NOTIF_ID)
                com.example.utils.CallRingtonePlayer.stopRinging()
            }
        }

        /** Envoie le jeton FCM au serveur. À appeler après chaque login ET dans onNewToken. */
        fun registerToken(authToken: String, fcmToken: String, baseUrl: String = CallManager.DEFAULT_BASE_URL) {
            if (authToken.isBlank() || fcmToken.isBlank()) return
            Thread {
                runCatching {
                    val cleanAuth = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
                    val json = """{"token":"$fcmToken","platform":"android"}"""
                    val req = Request.Builder().url("$baseUrl/api/devices/register")
                        .header("Authorization", cleanAuth)
                        .post(json.toRequestBody("application/json".toMediaType())).build()
                    OkHttpClient().newCall(req).execute().use { resp ->
                        Log.i(TAG, "registerToken response code: ${resp.code}")
                    }
                }.onFailure {
                    Log.w(TAG, "registerToken failed: ${it.message}")
                }
            }.start()
        }

        /** Supprime le jeton FCM sur le serveur au moment du logout. */
        fun unregisterToken(authToken: String, fcmToken: String, baseUrl: String = CallManager.DEFAULT_BASE_URL) {
            if (authToken.isBlank() || fcmToken.isBlank()) return
            Thread {
                runCatching {
                    val cleanAuth = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
                    val json = """{"token":"$fcmToken"}"""
                    val req = Request.Builder().url("$baseUrl/api/devices/unregister")
                        .header("Authorization", cleanAuth)
                        .post(json.toRequestBody("application/json".toMediaType())).build()
                    OkHttpClient().newCall(req).execute().use { resp ->
                        Log.i(TAG, "unregisterToken response code: ${resp.code}")
                    }
                }.onFailure {
                    Log.w(TAG, "unregisterToken failed: ${it.message}")
                }
            }.start()
        }
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM Token received: $token")
        // Récupère le JWT depuis les SharedPreferences
        val auth = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("auth_token", null)
            ?: getSharedPreferences("iddet", MODE_PRIVATE).getString("access_token", null)
            ?: return
        registerToken(auth, token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        try {
            ensureChannels(this)
            val d = msg.data
            Log.i(TAG, "FCM data payload received: $d")
            when (d["type"]) {
                "call_invite" -> {
                    val callId = d["call_id"] ?: return
                    val caller = d["caller_username"] ?: "Appel entrant"
                    val callerId = d["caller_id"] ?: ""
                    val avatar = d["caller_avatar"]

                    showIncomingCall(callId, caller)

                    // Synchronize in-app CallManager if app process is alive
                    runCatching {
                        com.example.utils.CallManager.handleIncomingCall(callId, callerId, caller, avatar)
                    }
                }
                "call_ended" -> {
                    cancelCallNotification(this) // arrête la sonnerie même sans WebSocket
                    runCatching {
                        val callId = d["call_id"] ?: ""
                        com.example.utils.CallManager.endCall()
                        CallManagerHolder.get(this)?.finish(callId, "ended")
                    }
                }
                "new_message" -> {
                    showMessage(d["sender_username"] ?: "Nouveau message", d["preview"] ?: "")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onMessageReceived exception", t) // jamais de crash ici
        }
    }

    private fun showIncomingCall(callId: String, caller: String) {
        val full = PendingIntent.getActivity(
            this,
            1,
            Intent(this, IncomingCallActivity::class.java)
                .putExtra(EXTRA_CALL_ID, callId)
                .putExtra("caller", caller)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = createActionPendingIntent(ACTION_DECLINE, 2, callId)
        val acceptIntent = createActionPendingIntent(ACTION_ACCEPT, 3, callId)

        val n = NotificationCompat.Builder(this, CH_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(caller)
            .setContentText("Appel vocal IDDET entrant...")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(full, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(45_000) // = délai serveur (45s)
            .addAction(0, "Refuser", declineIntent)
            .addAction(0, "Répondre", acceptIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(CALL_NOTIF_ID, n)
            com.example.utils.CallRingtonePlayer.startRinging(this)
        }
    }

    private fun createActionPendingIntent(actionName: String, code: Int, callId: String): PendingIntent {
        return PendingIntent.getBroadcast(
            this,
            code,
            Intent(actionName).setPackage(packageName).putExtra(EXTRA_CALL_ID, callId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showMessage(from: String, text: String) {
        val open = PendingIntent.getActivity(
            this,
            5,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH_MSG)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(from)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(from.hashCode(), n)
        }
    }
}
