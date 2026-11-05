package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object CallRingtonePlayer {
    private const val TAG = "CallRingtonePlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var isRinging = false
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    @Synchronized
    fun startRinging(context: Context) {
        if (isRinging) return
        isRinging = true

        val appContext = context.applicationContext
        initVibrator(appContext)
        startVibration()

        try {
            val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedUriString = prefs.getString("notification_ringtone_uri", null)

            val ringtoneUri: Uri = if (!savedUriString.isNullOrEmpty() && savedUriString != "silent") {
                Uri.parse(savedUriString)
            } else {
                RingtoneManager.getActualDefaultRingtoneUri(appContext, RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(appContext, ringtoneUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .build()
                    )
                    isLooping = true
                    setOnPreparedListener { player ->
                        try {
                            if (isRinging) {
                                player.start()
                                Log.i(TAG, "MediaPlayer ringtone started playing")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error starting ringtone after prepare", e)
                        }
                    }
                    setOnErrorListener { player, what, extra ->
                        Log.w(TAG, "MediaPlayer ringtone error: what=$what, extra=$extra")
                        try { player.reset(); player.release() } catch (_: Exception) {}
                        mediaPlayer = null
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer failed to play ringtone, falling back to RingtoneManager", e)
                try {
                    ringtone = RingtoneManager.getRingtone(appContext, ringtoneUri)?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            isLooping = true
                        }
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        play()
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to play fallback ringtone", e2)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ringtone", e)
        }
    }

    @Synchronized
    fun stopRinging() {
        if (!isRinging) return
        isRinging = false

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            ringtone?.let { r ->
                if (r.isPlaying) {
                    r.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Ringtone", e)
        } finally {
            ringtone = null
        }

        stopVibration()
        Log.i(TAG, "Ringtone and vibration stopped")
    }

    private fun initVibrator(context: Context) {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator?.let { v ->
                isVibrating = true
                val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error starting vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            if (isVibrating) {
                vibrator?.cancel()
                isVibrating = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping vibration", e)
        }
    }
}
