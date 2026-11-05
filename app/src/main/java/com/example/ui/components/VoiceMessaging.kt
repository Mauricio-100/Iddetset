package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.util.Log

class PlaybackListenerImpl(
    private val onProgressAction: (Float, Int, Int) -> Unit,
    private val onFinishedAction: () -> Unit,
    private val onErrorAction: (String) -> Unit
) : com.example.utils.RealAudioPlayer.PlaybackListener {
    override fun onProgress(p: Float, currentMs: Int, durationMs: Int) {
        onProgressAction(p, currentMs, durationMs)
    }
    override fun onFinished() {
        onFinishedAction()
    }
    override fun onError(error: String) {
        onErrorAction(error)
    }
}

/**
 * Modern, interactive voice message player with seekable waveform, playback speed controls, and transcription.
 */
@Composable
fun VoiceMessagePlayer(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val voiceData = remember(content) {
        if (isVoiceMessage(content)) {
            parseVoiceMessage(content) ?: VoiceMessageData(5, List(24) { 0.4f }, null, null)
        } else {
            val hash = content.hashCode()
            val random = java.util.Random(hash.toLong())
            val duration = 6 + random.nextInt(16)
            val generatedAmps = List(24) { idx ->
                val base = 0.25f + 0.65f * kotlin.math.abs(kotlin.math.sin((idx + hash % 10).toFloat() * 0.45f))
                base.coerceIn(0.15f, 1.0f)
            }
            VoiceMessageData(duration, generatedAmps, content, null)
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentDurationMs by remember { mutableStateOf(if (voiceData.durationSeconds > 0) voiceData.durationSeconds * 1000 else 0) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showTranscription by remember { mutableStateOf(true) }

    DisposableEffect(content) {
        onDispose {
            com.example.utils.RealAudioPlayer.stop()
            com.example.utils.VoiceSynthPlayer.stop()
        }
    }

    // Number of bars in our waveform
    val totalBars = 26
    val amplitudes = remember(voiceData) {
        val src = voiceData.amplitudes
        if (src.isEmpty()) {
            List(totalBars) { 0.35f }
        } else if (src.size == totalBars) {
            src
        } else {
            // Interpolate or repeat to fit totalBars
            List(totalBars) { i ->
                val srcIdx = ((i.toFloat() / totalBars) * src.size).toInt().coerceIn(0, src.size - 1)
                src[srcIdx].coerceIn(0.15f, 1.0f)
            }
        }
    }

    val playIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
    val primaryColor = MaterialTheme.colorScheme.primary
    val activeBarColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val inactiveBarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    val currentTotalSec = (currentDurationMs / 1000).coerceAtLeast(voiceData.durationSeconds.coerceAtLeast(1))
    val currentElapsedSec = (progress * currentTotalSec).toInt().coerceIn(0, currentTotalSec)
    
    val timeDisplay = remember(progress, currentTotalSec, isPlaying) {
        if (isPlaying && currentElapsedSec > 0) {
            val remSec = (currentTotalSec - currentElapsedSec).coerceAtLeast(0)
            String.format("-%d:%02d", remSec / 60, remSec % 60)
        } else {
            String.format("%d:%02d", currentTotalSec / 60, currentTotalSec % 60)
        }
    }

    val elapsedDisplay = remember(currentElapsedSec) {
        String.format("%d:%02d", currentElapsedSec / 60, currentElapsedSec % 60)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Main audio controls row: Play button + Waveform + Speed button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Circle Button
                IconButton(
                    onClick = {
                        val audioSource = voiceData.audioUrl ?: content
                        if (isPlaying) {
                            com.example.utils.RealAudioPlayer.pause()
                            isPlaying = false
                        } else {
                            isPlaying = true
                            com.example.utils.RealAudioPlayer.setPlaybackSpeed(playbackSpeed)
                            com.example.utils.RealAudioPlayer.play(
                                url = audioSource,
                                listener = PlaybackListenerImpl(
                                    onProgressAction = { p, currentMs, durationMs ->
                                        progress = p
                                        if (durationMs > 0) {
                                            currentDurationMs = durationMs
                                        }
                                    },
                                    onFinishedAction = {
                                        isPlaying = false
                                        progress = 0f
                                    },
                                    onErrorAction = { error ->
                                        Log.w("VoiceMessagePlayer", "RealAudioPlayer playback issue: $error, falling back to VoiceSynthPlayer")
                                        com.example.utils.VoiceSynthPlayer.play(
                                            amplitudes = amplitudes,
                                            durationSeconds = voiceData.durationSeconds.coerceAtLeast(2),
                                            onProgress = { p -> progress = p },
                                            onFinished = {
                                                isPlaying = false
                                                progress = 0f
                                            }
                                        )
                                    }
                                ),
                                context = context
                            )
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryColor, CircleShape)
                ) {
                    Icon(
                        imageVector = playIcon,
                        contentDescription = if (isPlaying) "Pause" else "Lire",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Interactive Audio Waveform
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .pointerInput(totalBars) {
                            detectTapGestures { offset ->
                                val tapRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                progress = tapRatio
                                com.example.utils.RealAudioPlayer.seekTo(tapRatio)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        amplitudes.forEachIndexed { index, amp ->
                            val barRatio = (index + 0.5f) / totalBars.toFloat()
                            val isPlayed = barRatio <= progress
                            val barColor = if (isPlayed) activeBarColor else inactiveBarColor
                            
                            // Pulse animation for currently active bar during playback
                            val isCurrentBar = isPlaying && kotlin.math.abs(barRatio - progress) < (1.5f / totalBars)
                            val barScale = if (isCurrentBar) 1.25f else 1.0f

                            val barHeight = (8.dp + (26.dp * amp)).coerceIn(6.dp, 32.dp)

                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight)
                                    .scale(scaleX = 1f, scaleY = barScale)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Playback speed toggle button (1x, 1.5x, 2x)
                Surface(
                    onClick = {
                        val nextSpeed = when (playbackSpeed) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        playbackSpeed = nextSpeed
                        com.example.utils.RealAudioPlayer.setPlaybackSpeed(nextSpeed)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (playbackSpeed) {
                                1.5f -> "1.5x"
                                2.0f -> "2.0x"
                                else -> "1.0x"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom metadata bar: mic icon + elapsed/total time readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Lecture en cours" else "Message vocal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = if (isPlaying) "$elapsedDisplay / $timeDisplay" else timeDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transcription section if present
            if (!voiceData.transcription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = voiceData.transcription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * Advanced voice recorder bar with real-time waveform, pause/preview, listen before send, and re-record capability.
 */
@Composable
fun VoiceRecorderUI(
    onCancel: () -> Unit,
    onSendVoice: ((String) -> Unit)? = null,
    onSendVoiceFile: ((java.io.File) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val recorderManager = remember { com.example.utils.AudioRecorderManager(context) }
    
    var durationSeconds by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(true) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<java.io.File?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableStateOf(0f) }
    var previewDurationMs by remember { mutableStateOf(0) }
    var isTranscribing by remember { mutableStateOf(false) }
    
    val recordAmplitudes = remember { mutableStateListOf<Float>() }

    // Start native recording on composition
    LaunchedEffect(Unit) {
        recorderManager.startRecording()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                recorderManager.cancelRecording()
            }
            com.example.utils.RealAudioPlayer.stop()
            previewFile?.delete()
        }
    }
    
    // Pulse animation for recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Sample real peak amplitude & stopwatch timer while recording (max 5 mins = 300s)
    LaunchedEffect(isRecording) {
        if (isRecording) {
            var msCounter = 0
            while (isRecording) {
                delay(100)
                msCounter += 100
                
                val realAmp = recorderManager.getMaxAmplitudeNormalized()
                recordAmplitudes.add(realAmp)
                if (recordAmplitudes.size > 22) {
                    recordAmplitudes.removeAt(0)
                }

                if (msCounter >= 1000) {
                    msCounter = 0
                    durationSeconds++
                    
                    if (durationSeconds >= 300) {
                        isRecording = false
                        val file = recorderManager.stopRecording()
                        previewFile = file
                        if (file != null) {
                            onSendVoiceFile?.invoke(file)
                        } else {
                            onCancel()
                        }
                        break
                    }
                }
            }
        }
    }

    val displayTime = remember(durationSeconds) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isPreviewMode) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isPreviewMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
        )
    ) {
        if (!isPreviewMode) {
            // STATE 1: ACTIVE RECORDING MODE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing Red Recording Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = dotAlpha))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Elapsed timer
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.width(42.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Live dynamic waveform reflecting microphone input
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeAmps = if (recordAmplitudes.isEmpty()) {
                        List(16) { 0.15f }
                    } else {
                        recordAmplitudes
                    }
                    activeAmps.forEach { amp ->
                        val barH = (28.dp * amp).coerceIn(4.dp, 28.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barH)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.75f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Discard / Cancel Button
                IconButton(
                    onClick = {
                        isRecording = false
                        recorderManager.cancelRecording()
                        onCancel()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer l'enregistrement",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Pause & Preview Button (Listen before sending)
                IconButton(
                    onClick = {
                        isRecording = false
                        val file = recorderManager.stopRecording()
                        if (file != null && file.exists() && file.length() > 0) {
                            previewFile = file
                            isPreviewMode = true
                            previewProgress = 0f
                        } else {
                            val dur = if (durationSeconds == 0) 2 else durationSeconds
                            val fallbackFile = java.io.File.createTempFile("voice_preview_", ".wav", context.cacheDir)
                            recorderManager.writeWavAudio(fallbackFile, durationSeconds = dur)
                            previewFile = fallbackFile
                            isPreviewMode = true
                            previewProgress = 0f
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Écouter avant d'envoyer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Direct Send Button
                IconButton(
                    onClick = {
                        isRecording = false
                        val file = recorderManager.stopRecording()
                        val finalAmps = if (recordAmplitudes.isEmpty()) {
                            List(22) { 0.35f }
                        } else {
                            recordAmplitudes.toList()
                        }
                        val ampsString = finalAmps.map { String.format(java.util.Locale.US, "%.2f", it) }.joinToString(",")
                        val finalDuration = if (durationSeconds == 0) 2 else durationSeconds
                        val audioPath = file?.absolutePath ?: ""
                        val encodedPath = try { java.net.URLEncoder.encode(audioPath, "UTF-8") } catch (e: Exception) { audioPath }
                        val voiceMarkdown = "[Voice Message](voice://url=$encodedPath&duration=$finalDuration&amplitudes=$ampsString)"

                        if (onSendVoiceFile != null && file != null && file.exists() && file.length() > 0) {
                            onSendVoiceFile(file)
                        } else if (onSendVoiceFile != null && file != null) {
                            onSendVoiceFile(file)
                        } else if (onSendVoice != null) {
                            onSendVoice(voiceMarkdown)
                        } else {
                            onCancel()
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Envoyer vocal",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // STATE 2: PREVIEW / REVIEW MODE (Listen back to the voice note before sending)
            val file = previewFile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause preview playback
                IconButton(
                    onClick = {
                        if (file == null) return@IconButton
                        if (isPreviewPlaying) {
                            com.example.utils.RealAudioPlayer.pause()
                            isPreviewPlaying = false
                        } else {
                            isPreviewPlaying = true
                            com.example.utils.RealAudioPlayer.play(
                                url = file.absolutePath,
                                listener = PlaybackListenerImpl(
                                    onProgressAction = { p, currentMs, durationMs ->
                                        previewProgress = p
                                        if (durationMs > 0) previewDurationMs = durationMs
                                    },
                                    onFinishedAction = {
                                        isPreviewPlaying = false
                                        previewProgress = 0f
                                    },
                                    onErrorAction = { error ->
                                        Log.w("VoiceRecorderUI", "Preview playback error, falling back to VoiceSynthPlayer")
                                        val amps = if (recordAmplitudes.isEmpty()) List(16) { 0.4f } else recordAmplitudes.toList()
                                        com.example.utils.VoiceSynthPlayer.play(
                                            amplitudes = amps,
                                            durationSeconds = if (durationSeconds == 0) 2 else durationSeconds,
                                            onProgress = { p -> previewProgress = p },
                                            onFinished = {
                                                isPreviewPlaying = false
                                                previewProgress = 0f
                                            }
                                        )
                                    }
                                ),
                                context = context
                            )
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Pause" else "Écouter l'aperçu",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Progress Slider for Preview
                Slider(
                    value = previewProgress,
                    onValueChange = { newP ->
                        previewProgress = newP
                        com.example.utils.RealAudioPlayer.seekTo(newP)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Restart / Re-record Button
                IconButton(
                    onClick = {
                        com.example.utils.RealAudioPlayer.stop()
                        previewFile?.delete()
                        previewFile = null
                        isPreviewMode = false
                        isPreviewPlaying = false
                        durationSeconds = 0
                        recordAmplitudes.clear()
                        recorderManager.startRecording()
                        isRecording = true
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Réenregistrer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Discard Button
                IconButton(
                    onClick = {
                        com.example.utils.RealAudioPlayer.stop()
                        previewFile?.delete()
                        previewFile = null
                        onCancel()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        com.example.utils.RealAudioPlayer.stop()
                        val finalAmps = if (recordAmplitudes.isEmpty()) {
                            List(22) { 0.35f }
                        } else {
                            recordAmplitudes.toList()
                        }
                        val ampsString = finalAmps.map { String.format(java.util.Locale.US, "%.2f", it) }.joinToString(",")
                        val finalDuration = if (durationSeconds == 0) 2 else durationSeconds
                        val audioPath = file?.absolutePath ?: ""
                        val encodedPath = java.net.URLEncoder.encode(audioPath, "UTF-8")
                        val voiceMarkdown = "[Voice Message](voice://url=$encodedPath&duration=$finalDuration&amplitudes=$ampsString)"

                        if (onSendVoiceFile != null && file != null && file.exists() && file.length() > 0) {
                            onSendVoiceFile(file)
                        } else if (onSendVoiceFile != null && file != null) {
                            onSendVoiceFile(file)
                        } else if (onSendVoice != null) {
                            onSendVoice(voiceMarkdown)
                        } else {
                            onCancel()
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Envoyer l'enregistrement",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Utility details
 */
fun isVoiceMessage(content: String): Boolean {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return false
    return trimmed.startsWith("[Voice Message]") ||
            trimmed.startsWith("voice://") ||
            trimmed.contains("voice://") ||
            trimmed.startsWith("data:audio") ||
            trimmed.startsWith("data:video/mp4") ||
            trimmed.contains("/voice_messages/") ||
            (trimmed.contains("voice_") && trimmed.endsWith(".m4a")) ||
            (trimmed.startsWith("http") && (trimmed.endsWith(".m4a") || trimmed.endsWith(".mp3") || trimmed.endsWith(".wav") || trimmed.endsWith(".aac") || trimmed.endsWith(".3gp") || trimmed.endsWith(".ogg"))) ||
            (trimmed.startsWith("/") && (trimmed.endsWith(".m4a") || trimmed.endsWith(".wav") || trimmed.endsWith(".mp3") || trimmed.contains("voice_notes") || trimmed.contains("voice_record"))) ||
            com.example.utils.AudioMessageHelper.isAudioContent(trimmed)
}

data class VoiceMessageData(
    val durationSeconds: Int,
    val amplitudes: List<Float>,
    val audioUrl: String? = null,
    val transcription: String? = null
)

/**
 * Parses flexible amplitude representations, including European comma decimals (0,59,0,55)
 * and standard dot decimals (0.59,0.55).
 */
fun parseAmplitudesFlexible(ampsString: String): List<Float> {
    if (ampsString.isBlank()) return emptyList()
    
    // First try standard float parse
    val direct = ampsString.split(",").mapNotNull { it.trim().toFloatOrNull() }
    if (direct.isNotEmpty() && direct.all { it <= 1.0f }) {
        return direct
    }
    
    // Check if tokens were paired due to comma decimal format e.g. ["0", "59", "0", "55"]
    val rawTokens = ampsString.split(",").map { it.trim() }
    if (rawTokens.size >= 2 && rawTokens.size % 2 == 0) {
        val paired = mutableListOf<Float>()
        for (i in 0 until rawTokens.size step 2) {
            val floatVal = "${rawTokens[i]}.${rawTokens[i + 1]}".toFloatOrNull()
            if (floatVal != null) {
                paired.add(floatVal.coerceIn(0.05f, 1.0f))
            }
        }
        if (paired.isNotEmpty()) return paired
    }
    
    return direct.map { (it / 100f).coerceIn(0.05f, 1.0f) }
}

private fun generateWaveformFromHash(seed: String): List<Float> {
    val hash = kotlin.math.abs(seed.hashCode())
    return List(24) { idx ->
        val wave = 0.25f + 0.65f * kotlin.math.abs(kotlin.math.sin((idx + hash % 11).toFloat() * 0.55f))
        wave.coerceIn(0.15f, 0.95f)
    }
}

fun parseVoiceMessage(content: String): VoiceMessageData? {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return null
    
    return try {
        if (trimmed.contains("voice://")) {
            val uri = if (trimmed.contains("voice://")) {
                trimmed.substringAfter("voice://").removeSuffix(")")
            } else {
                trimmed.removePrefix("voice://")
            }
            val params = uri.split("&").associate {
                val parts = it.split("=")
                parts[0] to parts.getOrNull(1)
            }
            val duration = params["duration"]?.toIntOrNull() ?: 5
            val ampsString = params["amplitudes"] ?: ""
            val amplitudes = parseAmplitudesFlexible(ampsString)
            val audioUrl = params["url"]?.let { 
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
            }
            val transcription = params["transcription"]?.let { 
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
            }
            VoiceMessageData(
                durationSeconds = duration,
                amplitudes = if (amplitudes.isEmpty()) generateWaveformFromHash(trimmed) else amplitudes,
                audioUrl = audioUrl,
                transcription = transcription
            )
        } else {
            // Direct audio URL (GitHub raw, Cloudinary, etc.) or local file path or base64 data
            val audioUrl = when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("/") -> trimmed
                trimmed.startsWith("data:audio") || trimmed.contains("base64,") || trimmed.length > 80 -> trimmed
                else -> null
            }
            val hash = (audioUrl ?: trimmed).hashCode()
            val pseudoDur = 4 + (kotlin.math.abs(hash) % 12)
            val amps = generateWaveformFromHash(trimmed)
            VoiceMessageData(
                durationSeconds = pseudoDur,
                amplitudes = amps,
                audioUrl = audioUrl ?: trimmed,
                transcription = null
            )
        }
    } catch (e: Exception) {
        VoiceMessageData(5, generateWaveformFromHash(trimmed), trimmed, null)
    }
}
