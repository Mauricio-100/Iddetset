package com.iddet.calls

import android.media.*
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Signalisation = WebSocket /ws/{userId} (push) + polling de secours.
 * Audio = WebSocket binaire /ws/call-audio/{callId}/{userId} (PCM16 mono 16 kHz).
 * Aucune dépendance WebRTC, tout passe en TCP => compatible Render.
 */
class CallManager(
    private val baseUrl: String = DEFAULT_BASE_URL,  // "https://hoosthubs-g.onrender.com"
    private val token: String,
    private val myUserId: String,
    private val listener: Listener,
) {
    interface Listener {
        fun onIncoming(callId: String, callerId: String, username: String, avatar: String?)
        fun onAccepted(callId: String)            // côté appelant : l'autre a décroché
        fun onFinished(callId: String, status: String) // declined | missed | ended
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://hoosthubs-g.onrender.com"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // garde la connexion vivante derrière le proxy Render
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var signalWs: WebSocket? = null
    private var audioWs: WebSocket? = null
    private var currentCall: String? = null
    private val seenInvites = HashSet<String>()

    // ---------- 1. Signalisation : WS avec reconnexion auto ----------
    fun start() {
        scope.launch { signalLoop() }
        scope.launch { incomingPollLoop() }   // filet de sécurité
    }

    private suspend fun signalLoop() {
        var backoff = 1000L
        while (scope.isActive) {
            val closed = CompletableDeferred<Unit>()
            val req = Request.Builder()
                .url(baseUrl.replace("http", "ws") + "/ws/$myUserId").build()
            signalWs = http.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, r: Response) { backoff = 1000L }
                override fun onMessage(ws: WebSocket, text: String) = handleSignal(JSONObject(text))
                override fun onClosed(ws: WebSocket, c: Int, r: String) { closed.complete(Unit) }
                override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) { closed.complete(Unit) }
            })
            closed.await()
            delay(backoff); backoff = (backoff * 2).coerceAtMost(15_000L)
        }
    }

    private fun handleSignal(j: JSONObject) {
        when (j.optString("type")) {
            "call_invite" -> onInvite(j.getString("call_id"), j.getString("caller_id"),
                j.optString("caller_username"), j.optString("caller_avatar", null))
            "call_accepted" -> if (j.getString("call_id") == currentCall) {
                listener.onAccepted(currentCall!!); openAudio(currentCall!!)
            }
            "call_declined" -> finish(j.getString("call_id"), "declined")
            "call_ended"    -> finish(j.getString("call_id"), j.optString("status", "ended"))
            "call_missed"   -> finish(j.getString("call_id"), "missed")
        }
    }

    private fun onInvite(id: String, callerId: String, name: String, avatar: String?) {
        if (!seenInvites.add(id)) return          // déjà sonné (WS + polling)
        listener.onIncoming(id, callerId, name, avatar)
    }

    // ---------- 2. Rattrapage : appels entrants si le WS a raté le push ----------
    private suspend fun incomingPollLoop() {
        while (scope.isActive) {
            runCatching {
                val arr = get("/api/calls/incoming").getJSONArray("calls")
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    onInvite(c.getString("call_id"), c.getString("caller_id"),
                        c.optString("caller_username"), c.optString("caller_avatar", null))
                }
            }
            delay(2500)
        }
    }

    // ---------- 3. Appelant : lance l'appel puis surveille le statut ----------
    fun startCall(receiverId: String) = scope.launch {
        val res = post("/api/calls/start", FormBody.Builder().add("receiver_id", receiverId).build())
        val id = res.getString("call_id")
        currentCall = id
        // Ne JAMAIS attendre le WS seul : on interroge /status jusqu'à décision.
        while (isActive && currentCall == id) {
            val st = runCatching { get("/api/calls/$id/status").getString("status") }.getOrNull()
            when (st) {
                "accepted" -> { listener.onAccepted(id); openAudio(id); return@launch }
                "declined", "missed", "ended" -> { finish(id, st); return@launch }
            }
            delay(1000)
        }
    }

    // ---------- 4. Appelé ----------
    fun accept(callId: String) = scope.launch {
        post("/api/calls/$callId/accept", FormBody.Builder().build())
        currentCall = callId; openAudio(callId)
    }
    fun decline(callId: String) = scope.launch { post("/api/calls/$callId/decline", FormBody.Builder().build()) }
    fun hangUp() { currentCall?.let { id -> scope.launch { runCatching { post("/api/calls/$id/end", FormBody.Builder().build()) } }; finish(id, "ended") } }

    private fun finish(id: String, status: String) {
        if (currentCall != null && currentCall != id) return
        currentCall = null
        audioWs?.close(1000, null); audioWs = null
        recordJob?.cancel(); player?.release(); player = null
        listener.onFinished(id, status)       // => arrêter la sonnerie des deux côtés
    }

    // ---------- 5. Audio PCM16 16 kHz via WebSocket binaire ----------
    private var recordJob: Job? = null
    private var player: AudioTrack? = null
    private val RATE = 16_000

    private fun openAudio(callId: String) {
        val req = Request.Builder()
            .url(baseUrl.replace("http", "ws") + "/ws/call-audio/$callId/$myUserId").build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM).build().also { it.play() }
        player = track
        audioWs = http.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(ws: WebSocket, bytes: ByteString) { track.write(bytes.toByteArray(), 0, bytes.size) }
            override fun onMessage(ws: WebSocket, text: String) { /* "peer_joined" : ignorer */ }
        })
        // Micro : paquets de 40 ms (1280 octets) envoyés dès qu'ils sont prêts.
        recordJob = scope.launch {
            val chunk = RATE * 2 * 40 / 1000
            val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, chunk * 4)
            val buf = ByteArray(chunk); rec.startRecording()
            try { while (isActive) { val n = rec.read(buf, 0, chunk); if (n > 0) audioWs?.send(buf.toByteString(0, n)) } }
            finally { rec.stop(); rec.release() }
        }
    }

    // ---------- HTTP helpers (suspend, sur Dispatchers.IO) ----------
    private fun auth(b: Request.Builder) = b.header("Authorization", "Bearer $token")
    private fun get(path: String): JSONObject =
        http.newCall(auth(Request.Builder().url(baseUrl + path)).build()).execute().use { JSONObject(it.body!!.string()) }
    private fun post(path: String, body: RequestBody): JSONObject =
        http.newCall(auth(Request.Builder().url(baseUrl + path).post(body)).build()).execute().use { JSONObject(it.body!!.string()) }

    fun stop() { scope.cancel(); signalWs?.close(1000, null); audioWs?.close(1000, null) }
}
