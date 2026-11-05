package com.iddet.calls

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** 
 * Écran d'appel entrant plein écran (s'ouvre sur l'écran verrouillé).
 * Conçu avec le style Cyberpunk IDDET (noir profond, rouge écarlate et vert néon).
 */
class IncomingCallActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val callId = intent.getStringExtra(IddetFirebaseService.EXTRA_CALL_ID) ?: run {
            finish()
            return
        }
        val caller = intent.getStringExtra("caller") ?: "Appel vocal entrant"

        // Root container with dark background
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B0F19"))
            setPadding(48, 120, 48, 64)
        }

        // Subtitle badge
        val badge = TextView(this).apply {
            text = "IDDET S-3 SECURE CALL"
            textSize = 12f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        root.addView(badge)

        // Caller avatar / icon placeholder
        val avatarView = TextView(this).apply {
            text = caller.take(1).uppercase()
            textSize = 42f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1F2937"))
                setStroke(4, Color.parseColor("#EF4444"))
            }
            layoutParams = LinearLayout.LayoutParams(220, 220).apply {
                bottomMargin = 48
            }
        }
        root.addView(avatarView)

        // Caller name
        val callerText = TextView(this).apply {
            text = caller
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        root.addView(callerText)

        // Status text
        val statusText = TextView(this).apply {
            text = "Appel audio chiffré de bout en bout..."
            textSize = 14f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }
        root.addView(statusText)

        // Spacer to push buttons to bottom
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(spacer)

        // Actions row (Decline & Accept)
        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Decline button
        val declineBtn = Button(this).apply {
            text = "Refuser"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(Color.parseColor("#DC2626"))
            }
            layoutParams = LinearLayout.LayoutParams(0, 140, 1f).apply {
                marginEnd = 24
            }
            setOnClickListener {
                sendBroadcast(
                    Intent(IddetFirebaseService.ACTION_DECLINE)
                        .setPackage(packageName)
                        .putExtra(IddetFirebaseService.EXTRA_CALL_ID, callId)
                )
                finish()
            }
        }
        buttonsRow.addView(declineBtn)

        // Accept button
        val acceptBtn = Button(this).apply {
            text = "Répondre"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(Color.parseColor("#16A34A"))
            }
            layoutParams = LinearLayout.LayoutParams(0, 140, 1f).apply {
                marginStart = 24
            }
            setOnClickListener {
                sendBroadcast(
                    Intent(IddetFirebaseService.ACTION_ACCEPT)
                        .setPackage(packageName)
                        .putExtra(IddetFirebaseService.EXTRA_CALL_ID, callId)
                )
                finish()
            }
        }
        buttonsRow.addView(acceptBtn)

        root.addView(buttonsRow)
        setContentView(root)
    }
}
