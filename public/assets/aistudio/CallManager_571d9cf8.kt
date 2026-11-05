package com.iddet.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * v2 — corrige les causes de "l'application s'est arrêtée" :
 *  1. TOUTE coroutine passe par un CoroutineExceptionHandler (avant : une 409/404 ou une coupure
 *     réseau dans startCall/accept plantait l'app).
 *  2. Réponses HTTP lues sans `!!` ni JSON aveugle (une page d'erreur HTML ne fait plus planter).
 *  3. Callbacks du Listener TOUJOURS sur le thread principal.
 *  4. Micro : permission vérifiée, AudioRecord/AudioTrack protégés et libérés une seule fois.
 */
class CallManager(
    context: Context,
    private val token: String,
    private val myUserId: String,
    private val listener: Listener,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://hoosthubs-g.onrender.com"
        private const val TAG = "CallManager"
        private const val RATE = 16_000
    }

    interface Listener {
        fun onIncoming(callId: String, callerId: String, username: String, avatar: String?)
        fun onAccepted(callId: String)
        fun onFinished(callId: String, status: String)   // declined | missed | ended | failed
        fun onError(message: String) {}
    }

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private fun ui(block: () -> Unit) = main.post { runCatching(block).onFailure { Log.e(TAG, "listener", it) } }

    // Filet de sécurité : aucune exception ne peut remonter jusqu'au runtime Android.
    private val handler = CoroutineExceptionHandler { _, t -> Log.e(TAG, "coroutine", t); ui { listener.onError(t.message ?: "Erreur") } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val wsHttp = http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()

    @Volatile private var signalWs: WebSocket? = null
    @Volatile private var audioWs: WebSocket? = null
    @Volatile private var currentCall: String? = null
    private val seenInvites = ConcurrentHashMap.newKeySet<String>()
    private var audioJob: Job? = null
    @Volatile private var player: AudioTrack? = null
    @Volatile private var started = false

    // ───────────────────────── Cycle de vie ─────────────────────────
    fun start() {
        if (started) return
        started = true
        scope.launch { signalLoop() }
        scope.launch { incomingPollLoop() }
    }

    fun stop() {
        started = false
        releaseAudio()
        signalWs?.close(1000, null); signalWs = null
        scope.cancel()
    }

    // ───────────────────────── Signalisation ─────────────────────────
    private suspend fun signalLoop() {
        var backoff = 1_000L
        while (currentCoroutineContext().isActive) {
            val closed = CompletableDeferred<Unit>()
            val req = Request.Builder().url(wsUrl("/ws/$myUserId")).build()
            signalWs = wsHttp.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, r: Response) { backoff = 1_000L }
                override fun onMessage(ws: WebSocket, text: String) {
                    runCatching { handleSignal(JSONObject(text)) }.onFailure { Log.e(TAG, "signal", it) }
                }
                override fun onClosed(ws: WebSocket, c: Int, r: String) { closed.complete(Unit) }
                override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) { closed.complete(Unit) }
            })
            closed.await()
            delay(backoff); backoff = (backoff * 2).coerceAtMost(15_000L)
        }
    }

    private fun handleSignal(j: JSONObject) {
        val id = j.optString("call_id")
        when (j.optString("type")) {
            "call_invite" -> onInvite(id, j.optString("caller_id"), j.optString("caller_username"), j.optString("caller_avatar").ifBlank { null })
            "call_accepted" -> if (id == currentCall) onRemoteAccepted(id)
            "call_declined" -> finish(id, "declined")
            "call_ended" -> finish(id, j.optString("status", "ended"))
            "call_missed" -> finish(id, "missed")
        }
    }

    private fun onInvite(id: String, callerId: String, name: String, avatar: String?) {
        if (id.isBlank() || !seenInvites.add(id)) return     // WS + polling + FCM : on ne sonne qu'une fois
        ui { listener.onIncoming(id, callerId, name, avatar) }
    }

    /** Filet de sécurité : rattrape un push perdu. */
    private suspend fun incomingPollLoop() {
        while (currentCoroutineContext().isActive) {
            if (currentCall == null) {
                api("GET", "/api/calls/incoming").json?.optJSONArray("calls")?.let { arr ->
                    for (i in 0 until arr.length()) arr.optJSONObject(i)?.let {
                        onInvite(it.optString("call_id"), it.optString("caller_id"),
                            it.optString("caller_username"), it.optString("caller_avatar").ifBlank { null })
                    }
                }
            }
            delay(2_500)
        }
    }

    // ───────────────────────── Appelant ─────────────────────────
    fun startCall(receiverId: String) {
        scope.launch {
            if (currentCall != null) return@launch
            val r = api("POST", "/api/calls/start", mapOf("receiver_id" to receiverId))
            val id = r.json?.optString("call_id").orEmpty()
            if (!r.ok || id.isBlank()) {
                val msg = r.json?.optString("detail").orEmpty().ifBlank { "Impossible de lancer l'appel (${r.code})" }
                ui { listener.onError(msg) }
                return@launch
            }
            currentCall = id
            seenInvites.add(id)
            val deadline = System.currentTimeMillis() + 50_000     // > timeout serveur (45 s)
            while (isActive && currentCall == id && System.currentTimeMillis() < deadline) {
                when (api("GET", "/api/calls/$id/status").json?.optString("status")) {
                    "accepted" -> { onRemoteAccepted(id); return@launch }
                    "declined", "missed", "ended" -> { finish(id, api("GET", "/api/calls/$id/status").json?.optString("status") ?: "ended"); return@launch }
                }
                delay(1_000)
            }
            if (currentCall == id) { hangUp(); }
        }
    }

    private fun onRemoteAccepted(id: String) {
        if (currentCall != id) return
        ui { listener.onAccepted(id) }
        openAudio(id)
    }

    // ───────────────────────── Appelé ─────────────────────────
    fun accept(callId: String) {
        scope.launch {
            val r = api("POST", "/api/calls/$callId/accept", emptyMap())
            if (!r.ok) {   // 410 = l'appelant a déjà raccroché
                finish(callId, "missed"); return@launch
            }
            currentCall = callId
            openAudio(callId)
        }
    }

    fun decline(callId: String) {
        scope.launch { api("POST", "/api/calls/$callId/decline", emptyMap()) }
        finish(callId, "declined")
    }

    fun hangUp() {
        val id = currentCall ?: return
        scope.launch { api("POST", "/api/calls/$id/end", emptyMap()) }
        finish(id, "ended")
    }

    /** Appelé aussi quand un push FCM "call_ended" arrive alors que l'app est vivante. */
    fun finish(id: String, status: String) {
        if (id.isBlank()) return
        val mine = currentCall
        if (mine != null && mine != id) return
        currentCall = null
        releaseAudio()
        ui { listener.onFinished(id, status) }
    }

    // ───────────────────────── Audio ─────────────────────────
    private fun hasMic() =
        ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun openAudio(callId: String) {
        if (!hasMic()) {                                  // avant : SecurityException => crash
            ui { listener.onError("Autorise le micro pour passer des appels") }
            hangUp(); return
        }
        releaseAudio()
        runCatching {
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_IN_COMMUNICATION

            val minOut = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(minOut * 2).setTransferMode(AudioTrack.MODE_STREAM).build()
            track.play(); player = track

            audioWs = wsHttp.newWebSocket(Request.Builder().url(wsUrl("/ws/call-audio/$callId/$myUserId")).build(),
                object : WebSocketListener() {
                    override fun onMessage(ws: WebSocket, bytes: ByteString) {
                        val t = player ?: return
                        runCatching { t.write(bytes.toByteArray(), 0, bytes.size) }
                    }
                    override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) {
                        if (currentCall == callId) hangUp()      // audio coupé => on termine proprement
                    }
                })

            audioJob = scope.launch { recordLoop() }
        }.onFailure { Log.e(TAG, "openAudio", it); ui { listener.onError("Audio indisponible") }; hangUp() }
    }

    private suspend fun recordLoop() {
        val chunk = RATE * 2 * 40 / 1000                         // 40 ms
        val minIn = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        @Suppress("MissingPermission")
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minIn, chunk * 4))
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); ui { listener.onError("Micro occupé") }; return }
        try {
            rec.startRecording()
            val buf = ByteArray(chunk)
            while (currentCoroutineContext().isActive) {
                val n = rec.read(buf, 0, chunk)
                if (n > 0) audioWs?.send(buf.toByteString(0, n)) else if (n < 0) break
            }
        } finally {
            runCatching { rec.stop() }; rec.release()
        }
    }

    private fun releaseAudio() {
        audioJob?.cancel(); audioJob = null
        audioWs?.close(1000, null); audioWs = null
        player?.let { p -> player = null; runCatching { p.stop() }; runCatching { p.release() } }
        runCatching { (app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode = AudioManager.MODE_NORMAL }
    }

    fun setSpeaker(on: Boolean) {
        runCatching { (app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn = on }
    }

    // ───────────────────────── HTTP sûr ─────────────────────────
    private class Api(val code: Int, val json: JSONObject?) { val ok get() = code in 200..299 }

    private fun wsUrl(path: String) = baseUrl.replaceFirst("http", "ws") + path

    /** Ne lève JAMAIS : réseau coupé => code -1, corps non-JSON => json = null. */
    private suspend fun api(method: String, path: String, form: Map<String, String>? = null): Api =
        withContext(Dispatchers.IO) {
            try {
                val b = Request.Builder().url(baseUrl + path).header("Authorization", "Bearer $token")
                if (method == "POST") {
                    val fb = FormBody.Builder(); form?.forEach { (k, v) -> fb.add(k, v) }; b.post(fb.build())
                }
                http.newCall(b.build()).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    Api(resp.code, runCatching { JSONObject(body) }.getOrNull())
                }
            } catch (e: Exception) {
                Log.w(TAG, "api $method $path : ${e.message}")
                Api(-1, null)
            }
        }
}
