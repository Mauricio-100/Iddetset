package com.iddet.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boutons Répondre / Refuser de la notification. Passe par ton CallManager (singleton) :
 * expose-le par ex. dans ton Application : (context.applicationContext as App).callManager
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getStringExtra(IddetFirebaseService.EXTRA_CALL_ID) ?: return
        Log.i("CallActionReceiver", "Received call action: ${i.action} for callId: $id")
        IddetFirebaseService.cancelCallNotification(ctx)
        
        // Stop any playing ringtone in the app
        runCatching {
            com.example.utils.CallRingtonePlayer.stopRinging()
        }

        when (i.action) {
            IddetFirebaseService.ACTION_ACCEPT -> {
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let { launchIntent ->
                    launchIntent.putExtra("accept_call", id)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    ctx.startActivity(launchIntent)
                }
                CallManagerHolder.get(ctx)?.accept(id)
                runCatching {
                    com.example.utils.CallManager.acceptCall(id)
                }
            }
            IddetFirebaseService.ACTION_DECLINE -> {
                CallManagerHolder.get(ctx)?.decline(id)
                runCatching {
                    com.example.utils.CallManager.declineCall(id)
                }
            }
        }
    }
}

/** Point d'accès unique au CallManager (créé après le login). */
object CallManagerHolder {
    @Volatile var instance: CallManager? = null
    fun get(@Suppress("UNUSED_PARAMETER") ctx: Context) = instance
}
