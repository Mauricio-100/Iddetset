package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.IddetRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

sealed class CallState {
    object Idle : CallState()

    data class OutgoingRinging(
        val callId: String,
        val calleeId: String,
        val calleeUsername: String,
        val calleeAvatar: String?
    ) : CallState()

    data class IncomingRinging(
        val callId: String,
        val callerId: String,
        val callerUsername: String,
        val callerAvatar: String?
    ) : CallState()

    data class Active(
        val callId: String,
        val peerId: String,
        val peerUsername: String,
        val peerAvatar: String?,
        val startedAt: Long = System.currentTimeMillis(),
        val durationSeconds: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = true,
        val isConnecting: Boolean = true
    ) : CallState()

    data class Ended(
        val callId: String,
        val peerUsername: String,
        val durationSeconds: Int = 0,
        val reason: String = "Appel terminé"
    ) : CallState()
}

object CallManager {
    private const val TAG = "CallManager"
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val BUFFER_SIZE = 1280 // 40ms of 16kHz 16-bit mono

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception in CallManager", throwable)
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)
    private var timerJob: Job? = null
    private var vibrationJob: Job? = null
    private var outgoingToneJob: Job? = null
    private var callStatusPollJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    private var audioWebSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false

    private val httpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket stream
        .build()

    private var repository: IddetRepository? = null
    private var currentUserId: String? = null
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var listenerJob: Job? = null

    fun initialize(context: Context, repo: IddetRepository) {
        this.appContext = context.applicationContext
        this.repository = repo
        this.audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        this.vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Listen for WebSocket Call events once
        if (listenerJob == null || listenerJob?.isCancelled == true) {
            listenerJob = scope.launch {
                WebSocketManager.events.collect { event ->
                    when (event) {
                        is WebSocketEvent.CallInvite -> {
                            if (seenInvites.add(event.callId)) {
                                handleIncomingCall(event.callId, event.callerId, event.callerUsername, event.callerAvatar)
                            }
                        }
                        is WebSocketEvent.CallAccepted -> {
                            handleCallAccepted(event.callId, event.calleeId)
                        }
                        is WebSocketEvent.CallDeclined -> {
                            handleCallDeclined(event.callId)
                        }
                        is WebSocketEvent.CallUnavailable -> {
                            handleCallUnavailable()
                        }
                        is WebSocketEvent.CallEnded -> {
                            handleCallEnded(event.callId, event.durationSeconds)
                        }
                        is WebSocketEvent.CallMissed -> {
                            handleCallMissed(event.callId, event.callerId)
                        }
                        is WebSocketEvent.CallTimeout -> {
                            handleCallTimeout(event.callId)
                        }
                        else -> {}
                    }
                }
            }
        }

        startIncomingPollLoop()
    }

    private val seenInvites = HashSet<String>()
    private var incomingPollJob: Job? = null

    private fun startIncomingPollLoop() {
        if (incomingPollJob?.isActive == true) return
        incomingPollJob = scope.launch {
            while (isActive) {
                try {
                    val token = repository?.userToken
                    if (!token.isNullOrBlank()) {
                        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                        val request = Request.Builder()
                            .url("https://hoosthubs-g.onrender.com/api/calls/incoming")
                            .header("Authorization", authHeader)
                            .build()
                        httpClient.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bodyStr = resp.body?.string() ?: ""
                                if (bodyStr.isNotBlank()) {
                                    val json = org.json.JSONObject(bodyStr)
                                    val arr = json.optJSONArray("calls")
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) {
                                            val c = arr.getJSONObject(i)
                                            val callId = c.getString("call_id")
                                            val callerId = c.getString("caller_id")
                                            val callerUsername = c.optString("caller_username", "Contact")
                                            val callerAvatar = if (c.has("caller_avatar") && !c.isNull("caller_avatar")) c.getString("caller_avatar") else null
                                            if (seenInvites.add(callId)) {
                                                handleIncomingCall(callId, callerId, callerUsername, callerAvatar)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(2500)
            }
        }
    }

    fun setCurrentUser(userId: String?) {
        this.currentUserId = userId
    }

    /**
     * Start an outgoing call to another user
     */
    fun startCall(calleeId: String, calleeUsername: String, calleeAvatar: String?, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val repo = repository ?: run {
            CoroutineScope(Dispatchers.Main).launch {
                onResult(false, "Service d'appel non initialisé")
            }
            return
        }

        if (_callState.value !is CallState.Idle) {
            CoroutineScope(Dispatchers.Main).launch {
                onResult(false, "Un appel est déjà en cours")
            }
            return
        }

        _callState.value = CallState.OutgoingRinging(
            callId = "",
            calleeId = calleeId,
            calleeUsername = calleeUsername,
            calleeAvatar = calleeAvatar
        )
        startOutgoingRingTone()

        scope.launch {
            val res = repo.startCall(calleeId)
            res.fold(
                onSuccess = { response ->
                    Log.i(TAG, "Call initiated successfully: ${response.call_id}")
                    val current = _callState.value
                    if (current is CallState.OutgoingRinging) {
                        _callState.value = current.copy(callId = response.call_id)
                        startCallStatusPolling(response.call_id, calleeId)
                    }
                    withContext(Dispatchers.Main) {
                        onResult(true, null)
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Call could not be started: ${error.message}")
                    stopRingTone()
                    val reason = error.message ?: "Impossible de joindre @$calleeUsername"
                    _callState.value = CallState.Ended(
                        callId = "",
                        peerUsername = calleeUsername,
                        reason = reason
                    )
                    withContext(Dispatchers.Main) {
                        onResult(false, reason)
                    }
                    autoResetToIdleAfterDelay()
                }
            )
        }
    }

    /**
     * Accept an incoming call
     */
    fun acceptCall(callId: String, peerId: String? = null, peerUsername: String? = null, peerAvatar: String? = null) {
        stopRingTone()
        val repo = repository ?: return
        val current = _callState.value

        val finalPeerId = (if (!peerId.isNullOrBlank()) peerId else (current as? CallState.IncomingRinging)?.callerId) ?: ""
        val finalPeerUsername = (if (!peerUsername.isNullOrBlank()) peerUsername else (current as? CallState.IncomingRinging)?.callerUsername) ?: "Contact"
        val finalPeerAvatar = if (!peerAvatar.isNullOrBlank()) peerAvatar else (current as? CallState.IncomingRinging)?.callerAvatar

        _callState.value = CallState.Active(
            callId = callId,
            peerId = finalPeerId,
            peerUsername = finalPeerUsername,
            peerAvatar = finalPeerAvatar,
            isConnecting = true
        )

        scope.launch {
            try {
                repo.acceptCall(callId)
            } catch (e: Exception) {
                Log.w(TAG, "API acceptCall failure: ${e.message}")
            }
            startAudioStream(callId, finalPeerId)
        }
    }

    /**
     * Decline an incoming call
     */
    fun declineCall(callId: String) {
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        val repo = repository
        val current = _callState.value

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = (current as? CallState.IncomingRinging)?.callerUsername ?: "Contact",
            reason = "Appel refusé"
        )

        scope.launch {
            repo?.declineCall(callId)
        }
        autoResetToIdleAfterDelay()
    }

    /**
     * End active or outgoing call
     */
    fun endCall() {
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        val current = _callState.value
        val repo = repository

        val callId = when (current) {
            is CallState.Active -> current.callId
            is CallState.OutgoingRinging -> current.callId
            is CallState.IncomingRinging -> current.callId
            else -> ""
        }
        val peerName = when (current) {
            is CallState.Active -> current.peerUsername
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.IncomingRinging -> current.callerUsername
            else -> "Contact"
        }
        val duration = (current as? CallState.Active)?.durationSeconds ?: 0

        stopAudioStream()

        val reason = when (current) {
            is CallState.OutgoingRinging -> "Appel annulé"
            is CallState.IncomingRinging -> "Appel refusé"
            else -> "Appel terminé"
        }

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            durationSeconds = duration,
            reason = reason
        )

        if (callId.isNotBlank()) {
            scope.launch {
                repo?.endCall(callId)
            }
        }
        autoResetToIdleAfterDelay()
    }

    /**
     * Toggle microphone mute state
     */
    fun toggleMute() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newMute = !current.isMuted
            _callState.value = current.copy(isMuted = newMute)
        }
    }

    /**
     * Toggle speakerphone on/off
     */
    fun toggleSpeakerphone() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newSpeaker = !current.isSpeakerOn
            audioManager?.isSpeakerphoneOn = newSpeaker
            _callState.value = current.copy(isSpeakerOn = newSpeaker)
        }
    }

    // Handle WebSocket & Polling incoming call notifications
    fun handleIncomingCall(callId: String, callerId: String, callerUsername: String, callerAvatar: String?) {
        if (_callState.value !is CallState.Idle) {
            // Already busy
            scope.launch { repository?.declineCall(callId) }
            return
        }

        _callState.value = CallState.IncomingRinging(
            callId = callId,
            callerId = callerId,
            callerUsername = callerUsername,
            callerAvatar = callerAvatar
        )
        
        appContext?.let { ctx ->
            CallRingtonePlayer.startRinging(ctx)
            scope.launch {
                NotificationHelper.showIncomingCallNotification(
                    context = ctx,
                    callId = callId,
                    callerId = callerId,
                    callerUsername = callerUsername,
                    callerAvatar = callerAvatar
                )
            }
        }
    }

    /**
     * Decline incoming call with an automated Quick Reply message (WhatsApp style)
     */
    fun declineWithQuickReply(
        context: Context?,
        callId: String,
        callerId: String,
        quickMessage: String,
        callerUsername: String = "Contact"
    ) {
        stopRingTone()
        stopAudioStream()
        val ctx = context ?: appContext
        ctx?.let { NotificationHelper.cancelIncomingCallNotification(it) }

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = callerUsername,
            reason = "Message envoyé : \"$quickMessage\""
        )

        scope.launch {
            try {
                repository?.declineCall(callId)
            } catch (_: Exception) {}
            try {
                repository?.sendMessage(callerId, quickMessage, "text")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send quick reply message: ${e.message}")
            }
        }
        autoResetToIdleAfterDelay()
    }

    private fun handleCallAccepted(callId: String, calleeId: String) {
        val current = _callState.value
        Log.i(TAG, "handleCallAccepted: callId=$callId, calleeId=$calleeId, current=$current")
        if (current is CallState.OutgoingRinging) {
            callStatusPollJob?.cancel()
            callStatusPollJob = null
            stopRingTone()

            val effectiveCallId = if (callId.isNotBlank()) callId else current.callId
            val effectivePeerId = if (calleeId.isNotBlank()) calleeId else current.calleeId

            _callState.value = CallState.Active(
                callId = effectiveCallId,
                peerId = effectivePeerId,
                peerUsername = current.calleeUsername,
                peerAvatar = current.calleeAvatar,
                isConnecting = true
            )
            startAudioStream(effectiveCallId, effectivePeerId)
        }
    }

    private fun handleCallDeclined(callId: String) {
        Log.i(TAG, "handleCallDeclined: callId=$callId")
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.Active -> current.peerUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            reason = "Appel refusé par le correspondant"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallUnavailable() {
        Log.i(TAG, "handleCallUnavailable")
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.OutgoingRinging -> current.calleeUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = "",
            peerUsername = peerName,
            reason = "@$peerName n'est pas en ligne"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallEnded(callId: String, durationSeconds: Int) {
        Log.i(TAG, "handleCallEnded: callId=$callId, duration=$durationSeconds")
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.Active -> current.peerUsername
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.IncomingRinging -> current.callerUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            durationSeconds = durationSeconds,
            reason = "Appel terminé"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallMissed(callId: String, callerId: String?) {
        Log.i(TAG, "handleCallMissed: callId=$callId, callerId=$callerId")
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        stopAudioStream()

        val current = _callState.value
        val peerId = callerId ?: (current as? CallState.IncomingRinging)?.callerId ?: ""
        val peerName = (current as? CallState.IncomingRinging)?.callerUsername ?: "Contact"
        val peerAvatar = (current as? CallState.IncomingRinging)?.callerAvatar

        // Show system notification for missed call!
        appContext?.let { ctx ->
            NotificationHelper.cancelIncomingCallNotification(ctx)
            scope.launch {
                NotificationHelper.showMissedCallNotification(
                    context = ctx,
                    callId = callId,
                    callerId = peerId,
                    callerUsername = peerName,
                    callerAvatar = peerAvatar
                )
            }
        }

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            reason = "Appel manqué"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallTimeout(callId: String) {
        Log.i(TAG, "handleCallTimeout: callId=$callId")
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        stopRingTone()
        stopAudioStream()

        val current = _callState.value
        val peerName = when (current) {
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.IncomingRinging -> current.callerUsername
            is CallState.Active -> current.peerUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            reason = "Sans réponse"
        )
        autoResetToIdleAfterDelay()
    }

    private fun startAudioStream(callId: String, peerId: String) {
        val userId = currentUserId ?: repository?.currentUser?.value?.id ?: return
        val wsUrl = "wss://hoosthubs-g.onrender.com/ws/call-audio/$callId/$userId"

        Log.i(TAG, "Connecting to audio relay WebSocket: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        audioWebSocket = httpClient.newWebSocket(request, CallAudioWebSocketListener())
    }

    private class CallAudioWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "Audio relay WebSocket opened successfully")
            scope.launch {
                val current = _callState.value
                if (current is CallState.Active) {
                    _callState.value = current.copy(isConnecting = false)
                }
                startHardwareAudio(webSocket)
                startDurationTimer()
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // Received remote audio chunk -> Play via AudioTrack
            playAudioChunk(bytes.toByteArray())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "Audio relay WebSocket error: ${t.message}")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Audio relay WebSocket closed: $code / $reason")
        }
    }

    private fun startHardwareAudio(webSocket: WebSocket) {
        try {
            try {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.isSpeakerphoneOn = true
            } catch (e: Exception) {
                Log.w(TAG, "Cannot configure AudioManager mode", e)
            }

            // Setup AudioTrack for playback
            val minPlayBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
            val playBufSize = if (minPlayBufSize > 0) minPlayBufSize.coerceAtLeast(BUFFER_SIZE * 4) else (BUFFER_SIZE * 4)
            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG_OUT)
                            .build()
                    )
                    .setBufferSizeInBytes(playBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack?.play()
                    isPlaying = true
                } else {
                    Log.w(TAG, "AudioTrack initialized with non-ready state: ${audioTrack?.state}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create AudioTrack", e)
            }

            // Setup AudioRecord for microphone capture
            val minRecBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
            val recBufSize = if (minRecBufSize > 0) minRecBufSize.coerceAtLeast(BUFFER_SIZE * 4) else (BUFFER_SIZE * 4)

            try {
                val rec = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    recBufSize
                )

                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    rec.startRecording()
                    audioRecord = rec
                    isRecording = true

                    // Capture loop
                    scope.launch(Dispatchers.IO) {
                        val buffer = ByteArray(BUFFER_SIZE)
                        try {
                            while (isRecording && isActive && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                                val read = rec.read(buffer, 0, BUFFER_SIZE)
                                if (read > 0) {
                                    val isMuted = (_callState.value as? CallState.Active)?.isMuted == true
                                    if (!isMuted) {
                                        val chunk = buffer.copyOf(read)
                                        try {
                                            webSocket.send(chunk.toByteString())
                                        } catch (_: Exception) {
                                            break
                                        }
                                    }
                                } else if (read < 0) {
                                    delay(40)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Microphone capture loop stopped: ${e.message}")
                        }
                    }
                } else {
                    Log.w(TAG, "AudioRecord state is not initialized (state=${rec.state})")
                    try { rec.release() } catch (_: Exception) {}
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission not granted for audio call", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting AudioRecord", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting hardware audio", e)
        }
    }

    private fun playAudioChunk(data: ByteArray) {
        try {
            val track = audioTrack
            if (isPlaying && track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(data, 0, data.size, AudioTrack.WRITE_NON_BLOCKING)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing audio chunk: ${e.message}")
        }
    }

    private fun stopAudioStream() {
        isRecording = false
        isPlaying = false

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    try { stop() } catch (_: Exception) {}
                }
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    try { stop() } catch (_: Exception) {}
                }
                try { release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        audioTrack = null

        try {
            audioWebSocket?.close(1000, "Call ended")
        } catch (_: Exception) {}
        audioWebSocket = null

        timerJob?.cancel()
        timerJob = null

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (isActive) {
                delay(1000)
                seconds++
                val current = _callState.value
                if (current is CallState.Active) {
                    _callState.value = current.copy(durationSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    private fun startIncomingRinging() {
        vibrationJob?.cancel()
        vibrationJob = scope.launch {
            while (isActive && _callState.value is CallState.IncomingRinging) {
                vibrator?.let { v ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 1000), 0))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 800, 1000), 0)
                    }
                }
                delay(2000)
            }
        }
    }

    private fun startOutgoingRingTone() {
        stopRingTone()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            outgoingToneJob = scope.launch {
                while (isActive && _callState.value is CallState.OutgoingRinging) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1000)
                    } catch (e: Exception) {
                        Log.w(TAG, "ToneGenerator startTone error: ${e.message}")
                    }
                    delay(3500)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator error: ${e.message}")
        }
    }

    private fun startCallStatusPolling(callId: String, calleeId: String) {
        callStatusPollJob?.cancel()
        if (callId.isBlank()) return

        callStatusPollJob = scope.launch(Dispatchers.IO) {
            val repo = repository ?: return@launch
            var elapsedMs = 0L
            val pollIntervalMs = 1000L // 1s polling per user specification
            val maxRingingDurationMs = 45_000L

            while (isActive && _callState.value is CallState.OutgoingRinging) {
                delay(pollIntervalMs)
                elapsedMs += pollIntervalMs

                if (_callState.value !is CallState.OutgoingRinging) break

                if (elapsedMs >= maxRingingDurationMs) {
                    Log.i(TAG, "Outgoing call timeout reached after ${elapsedMs}ms")
                    withContext(Dispatchers.Main) {
                        handleCallTimeout(callId)
                    }
                    try { repo.endCall(callId) } catch (_: Exception) {}
                    break
                }

                try {
                    val statusRes = repo.getCallStatus(callId)
                    statusRes.fold(
                        onSuccess = { details ->
                            Log.d(TAG, "Call status polled: ${details.status} for callId=$callId")
                            if (_callState.value !is CallState.OutgoingRinging) return@fold

                            when (details.status) {
                                "accepted" -> {
                                    withContext(Dispatchers.Main) {
                                        handleCallAccepted(callId, calleeId)
                                    }
                                }
                                "declined" -> {
                                    withContext(Dispatchers.Main) {
                                        handleCallDeclined(callId)
                                    }
                                }
                                "missed" -> {
                                    withContext(Dispatchers.Main) {
                                        handleCallTimeout(callId)
                                    }
                                }
                                "ended" -> {
                                    withContext(Dispatchers.Main) {
                                        handleCallEnded(callId, details.duration_seconds)
                                    }
                                }
                            }
                        },
                        onFailure = { e ->
                            Log.d(TAG, "Polling call status error: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "Polling exception: ${e.message}")
                }
            }
        }
    }

    private fun stopRingTone() {
        callStatusPollJob?.cancel()
        callStatusPollJob = null
        outgoingToneJob?.cancel()
        outgoingToneJob = null
        CallRingtonePlayer.stopRinging()
        appContext?.let { NotificationHelper.cancelIncomingCallNotification(it) }
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator?.cancel()
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }

    fun resetToIdle() {
        stopRingTone()
        stopAudioStream()
        _callState.value = CallState.Idle
    }

    private fun autoResetToIdleAfterDelay() {
        scope.launch {
            delay(2800)
            if (_callState.value is CallState.Ended) {
                _callState.value = CallState.Idle
            }
        }
    }
}
