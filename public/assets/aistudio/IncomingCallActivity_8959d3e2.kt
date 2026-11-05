package com.iddet.calls

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Écran d'appel entrant plein écran (s'ouvre sur l'écran verrouillé). Remplace le XML par ton design. */
class IncomingCallActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        val callId = intent.getStringExtra(IddetFirebaseService.EXTRA_CALL_ID) ?: return finish()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 200, 64, 64) }
        root.addView(TextView(this).apply { text = intent.getStringExtra("caller") ?: "Appel entrant"; textSize = 28f })
        root.addView(Button(this).apply { text = "Répondre"; setOnClickListener {
            sendBroadcast(Intent(IddetFirebaseService.ACTION_ACCEPT).setPackage(packageName).putExtra(IddetFirebaseService.EXTRA_CALL_ID, callId)); finish() } })
        root.addView(Button(this).apply { text = "Refuser"; setOnClickListener {
            sendBroadcast(Intent(IddetFirebaseService.ACTION_DECLINE).setPackage(packageName).putExtra(IddetFirebaseService.EXTRA_CALL_ID, callId)); finish() } })
        setContentView(root)
    }
}

/**
 * Boutons Répondre / Refuser de la notification. Passe par ton CallManager (singleton) :
 * expose-le par ex. dans ton Application : (context.applicationContext as App).callManager
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getStringExtra(IddetFirebaseService.EXTRA_CALL_ID) ?: return
        IddetFirebaseService.cancelCallNotification(ctx)
        val cm = CallManagerHolder.get(ctx) ?: return
        when (i.action) {
            IddetFirebaseService.ACTION_ACCEPT -> {
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
                    ctx.startActivity(it.putExtra("accept_call", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                cm.accept(id)
            }
            IddetFirebaseService.ACTION_DECLINE -> cm.decline(id)
        }
    }
}

/** Point d'accès unique au CallManager (créé après le login). */
object CallManagerHolder {
    @Volatile var instance: CallManager? = null
    fun get(@Suppress("UNUSED_PARAMETER") ctx: Context) = instance
}
