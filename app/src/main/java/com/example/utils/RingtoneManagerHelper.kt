package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

data class AppRingtoneItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: String,
    val description: String,
    val frequencies: List<Pair<Double, Int>> // Frequency (Hz) to Duration (ms)
)

typealias RingtonePreset = AppRingtoneItem

object RingtoneManagerHelper {
    private const val PREFS_NAME = "iddet_ringtone_prefs"
    private const val KEY_RINGTONE = "selected_app_ringtone_id"

    val allRingtones: List<AppRingtoneItem> get() = RINGTONES

    val RINGTONES = listOf(
        AppRingtoneItem(
            id = "iddet_neon_pulse",
            name = "iDDET Pulse Néon",
            category = "Électro / Cyber",
            icon = "⚡",
            description = "Sonnerie signature haute technologie rythmée",
            frequencies = listOf(
                Pair(587.33, 120), Pair(880.0, 120), Pair(1174.66, 180), Pair(1760.0, 240),
                Pair(880.0, 100), Pair(1174.66, 300)
            )
        ),
        AppRingtoneItem(
            id = "marimba_echo",
            name = "Marimba Écho Style",
            category = "Classique Messagerie",
            icon = "🎶",
            description = "Double tintement percutant fluide et entraînant",
            frequencies = listOf(
                Pair(659.25, 100), Pair(783.99, 100), Pair(987.77, 120), Pair(1318.51, 250),
                Pair(987.77, 100), Pair(1318.51, 350)
            )
        ),
        AppRingtoneItem(
            id = "vip_crystal_drop",
            name = "Goutte de Cristal VIP",
            category = "Luxe & Pureté",
            icon = "💎",
            description = "Résonance cristalline harmonieuse et apaisante",
            frequencies = listOf(
                Pair(1046.50, 150), Pair(1318.51, 150), Pair(1567.98, 200), Pair(2093.00, 450)
            )
        ),
        AppRingtoneItem(
            id = "retro_synthwave",
            name = "Retro 80s Synthwave",
            category = "Rétro & Vintage",
            icon = "🕹️",
            description = "Arpège analogique nostalgique des années 80",
            frequencies = listOf(
                Pair(440.0, 90), Pair(554.37, 90), Pair(659.25, 90), Pair(880.0, 120),
                Pair(783.99, 100), Pair(1046.50, 280)
            )
        ),
        AppRingtoneItem(
            id = "afro_chill_beat",
            name = "Afro Chill Mood",
            category = "Rythme & Soleil",
            icon = "🌴",
            description = "Mélodie tropicale chaleureuse et lumineuse",
            frequencies = listOf(
                Pair(523.25, 120), Pair(659.25, 120), Pair(783.99, 120), Pair(659.25, 100),
                Pair(880.0, 150), Pair(1046.50, 320)
            )
        ),
        AppRingtoneItem(
            id = "acoustic_guitar_strum",
            name = "Guitare Acoustique Zen",
            category = "Organique & Doux",
            icon = "🎸",
            description = "Accords pincés subtils pour une alerte en douceur",
            frequencies = listOf(
                Pair(329.63, 140), Pair(392.00, 140), Pair(493.88, 160), Pair(659.25, 380)
            )
        ),
        AppRingtoneItem(
            id = "cosmic_radar_ping",
            name = "Radar Spatial Cosmique",
            category = "Sci-Fi & Futuriste",
            icon = "🛰️",
            description = "Impulsion ondulatoire façon sonar de l'espace",
            frequencies = listOf(
                Pair(800.0, 80), Pair(1200.0, 80), Pair(1600.0, 120), Pair(2400.0, 350)
            )
        ),
        AppRingtoneItem(
            id = "cyber_cat_purr",
            name = "Chat Robotique iDDET",
            category = "Mascotte & Fun",
            icon = "🐱",
            description = "Tintement joueur avec ronronnement harmonique",
            frequencies = listOf(
                Pair(740.0, 100), Pair(987.77, 100), Pair(1480.0, 140), Pair(1975.53, 300),
                Pair(1480.0, 100), Pair(2217.46, 400)
            )
        )
    )

    private var activeTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentRingtone = MutableStateFlow(RINGTONES.first())
    val currentRingtone: StateFlow<AppRingtoneItem> = _currentRingtone.asStateFlow()

    fun init(context: Context) {
        val id = getSelectedRingtoneId(context)
        _currentRingtone.value = RINGTONES.find { it.id == id } ?: RINGTONES.first()
    }

    fun setRingtone(ringtone: AppRingtoneItem, context: Context) {
        _currentRingtone.value = ringtone
        setSelectedRingtoneId(context, ringtone.id)
    }

    fun playPreview(context: Context, ringtone: AppRingtoneItem) {
        scope.launch {
            playRingtonePreview(ringtone)
        }
    }

    fun getSelectedRingtoneId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RINGTONE, RINGTONES.first().id) ?: RINGTONES.first().id
    }

    fun setSelectedRingtoneId(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RINGTONE, id).apply()
    }

    suspend fun playRingtonePreview(ringtoneItem: AppRingtoneItem) = withContext(Dispatchers.IO) {
        stopPreview()
        try {
            val sampleRate = 44100
            val allSamples = mutableListOf<Short>()

            ringtoneItem.frequencies.forEach { (freq, durationMs) ->
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                    // Apply envelope to avoid clicking
                    val envelope = when {
                        i < 200 -> i / 200.0
                        i > numSamples - 300 -> (numSamples - i) / 300.0
                        else -> 1.0
                    }
                    val sample = (sin(angle) * 32767.0 * 0.7 * envelope).toInt().toShort()
                    allSamples.add(sample)
                }
                // Small gap between notes
                val gapSamples = (sampleRate * 0.015).toInt()
                for (g in 0 until gapSamples) {
                    allSamples.add(0)
                }
            }

            val shortArray = allSamples.toShortArray()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(shortArray.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                activeTrack = track
                track.write(shortArray, 0, shortArray.size)
                track.play()
            } else {
                Log.w("RingtoneManagerHelper", "AudioTrack not initialized: state=${track.state}")
            }
        } catch (e: Exception) {
            Log.e("RingtoneManagerHelper", "Error generating ringtone preview", e)
        }
    }

    fun stopPreview() {
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            activeTrack = null
        }
    }
}
