package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sin
import kotlin.math.PI

object VoiceSynthPlayer {
    private const val TAG = "VoiceSynthPlayer"
    private var activeTrack: AudioTrack? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    @Volatile
    private var isCurrentlyPlaying = false

    /**
     * Synthesizes and plays a smooth, warm vocal-like speech synth based on the message amplitudes.
     */
    fun play(
        amplitudes: List<Float>, 
        durationSeconds: Int, 
        onProgress: (Float) -> Unit, 
        onFinished: () -> Unit
    ) {
        stop() // Stop any previous playback first
        isCurrentlyPlaying = true

        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 16000
                val totalDuration = durationSeconds.toFloat()
                val totalSamples = (sampleRate * totalDuration).toInt()
                val buffer = ShortArray(totalSamples)

                val numAmps = amplitudes.size
                var phase = 0.0

                for (i in 0 until totalSamples) {
                    if (!isCurrentlyPlaying) break
                    val t = i.toFloat() / sampleRate
                    val progress = t / totalDuration

                    // Determine amplitude block
                    val ampIndex = (progress * numAmps).toInt().coerceIn(0, numAmps - 1)
                    val amp = amplitudes.getOrElse(ampIndex) { 0.4f }

                    // Melodic frequency swing to simulate speech prosody/melody (intonation)
                    val intonation = sin(2.0 * PI * 1.5 * t) * 30.0
                    val vocalFrequency = 180.0 + 50.0 * sin(2.0 * PI * 0.5 * t) + intonation // Human hum range (F3 to D4)

                    // Formant/Harmonic simulation: blend fundamental + 2nd harmonic + 3rd harmonic
                    val fundamental = sin(phase)
                    val h2 = 0.35 * sin(2.0 * phase)
                    val h3 = 0.12 * sin(3.0 * phase)
                    val signal = fundamental + h2 + h3

                    // Accumulate phase safely
                    phase += 2.0 * PI * vocalFrequency / sampleRate

                    // Volume governed by amplitudes
                    val volume = amp.coerceIn(0.05f, 1.0f) * 9000.0

                    // Fade-out envelope at the very end
                    val fadeOut = if (progress > 0.95f) {
                        (1.0f - progress) / 0.05f
                    } else {
                        1.0f
                    }

                    buffer[i] = (signal * volume * fadeOut).toInt().coerceIn(-32768, 32767).toShort()
                }

                if (isCurrentlyPlaying) {
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(totalSamples * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        activeTrack = track
                        track.write(buffer, 0, totalSamples)
                        track.play()

                        val intervalMs = 50L
                        val totalIntervals = (totalDuration * 1000 / intervalMs).toLong()
                        for (step in 0..totalIntervals) {
                            if (!isCurrentlyPlaying) break
                            Thread.sleep(intervalMs)
                            val elapsedProgress = step.toFloat() / totalIntervals
                            val p = elapsedProgress.coerceIn(0f, 1f)
                            mainHandler.post { onProgress(p) }
                        }
                    } else {
                        Log.w(TAG, "AudioTrack not initialized: state=${track.state}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing voice synthesis", e)
            } finally {
                mainHandler.post { onFinished() }
                try {
                    track?.stop()
                    track?.release()
                } catch (ex: Exception) {
                    // ignore
                }
                if (activeTrack == track) {
                    activeTrack = null
                }
            }
        }.start()
    }

    /**
     * Stops the playback immediately and releases resources.
     */
    fun stop() {
        isCurrentlyPlaying = false
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        activeTrack = null
    }
}
