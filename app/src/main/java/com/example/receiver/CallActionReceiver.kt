package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.utils.CallManager
import com.example.utils.CallRingtonePlayer
import com.example.utils.NotificationHelper

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER_CALL = "com.example.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.example.ACTION_DECLINE_CALL"
        const val ACTION_QUICK_REPLY = "com.example.ACTION_QUICK_REPLY"

        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALLER_ID = "extra_caller_id"
        const val EXTRA_CALLER_USERNAME = "extra_caller_username"
        const val EXTRA_CALLER_AVATAR = "extra_caller_avatar"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_REMOTE_INPUT_TEXT = "extra_remote_input_text"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val callerId = intent.getStringExtra(EXTRA_CALLER_ID) ?: ""
        val callerUsername = intent.getStringExtra(EXTRA_CALLER_USERNAME) ?: "Contact"
        val callerAvatar = intent.getStringExtra(EXTRA_CALLER_AVATAR)

        // Stop ringing and dismiss incoming call notification immediately
        CallRingtonePlayer.stopRinging()
        NotificationHelper.cancelIncomingCallNotification(context)

        when (action) {
            ACTION_ANSWER_CALL -> {
                CallManager.acceptCall(callId, callerId, callerUsername, callerAvatar)

                // Launch MainActivity to bring call UI to foreground
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("route", "call_active")
                    putExtra("incoming_call_id", callId)
                    putExtra("incoming_caller_id", callerId)
                    putExtra("incoming_caller_username", callerUsername)
                    putExtra("incoming_caller_avatar", callerAvatar)
                }
                context.startActivity(openAppIntent)
            }

            ACTION_DECLINE_CALL -> {
                CallManager.declineCall(callId)
            }

            ACTION_QUICK_REPLY -> {
                val bundle = RemoteInput.getResultsFromIntent(intent)
                val directInput = bundle?.getCharSequence(EXTRA_REMOTE_INPUT_TEXT)?.toString()
                val message = directInput ?: intent.getStringExtra(EXTRA_MESSAGE) ?: "Rappelle-moi plus tard"

                CallManager.declineWithQuickReply(context, callId, callerId, message, callerUsername)

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Message envoyé à @$callerUsername : \"$message\"", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
