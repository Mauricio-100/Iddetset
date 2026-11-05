package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.utils.CallManager
import com.example.utils.CallState

import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun CallOverlayHost() {
    val callState by CallManager.callState.collectAsState()
    val context = LocalContext.current

    AnimatedVisibility(
        visible = callState !is CallState.Idle,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        when (val state = callState) {
            is CallState.IncomingRinging -> {
                IncomingCallDialog(
                    callerUsername = state.callerUsername,
                    callerAvatar = state.callerAvatar,
                    onAccept = { CallManager.acceptCall(state.callId) },
                    onDecline = { CallManager.declineCall(state.callId) },
                    onQuickReply = { message ->
                        CallManager.declineWithQuickReply(
                            context = context,
                            callId = state.callId,
                            callerId = state.callerId,
                            quickMessage = message,
                            callerUsername = state.callerUsername
                        )
                    }
                )
            }
            is CallState.OutgoingRinging -> {
                ActiveOrOutgoingCallDialog(
                    title = "Appel en cours…",
                    peerUsername = state.calleeUsername,
                    peerAvatar = state.calleeAvatar,
                    subtitle = "Sonnerie…",
                    isMuted = false,
                    isSpeakerOn = true,
                    onToggleMute = {},
                    onToggleSpeaker = {},
                    onEndCall = { CallManager.endCall() }
                )
            }
            is CallState.Active -> {
                val formattedDuration = String.format(
                    java.util.Locale.ROOT,
                    "%02d:%02d",
                    state.durationSeconds / 60,
                    state.durationSeconds % 60
                )
                ActiveOrOutgoingCallDialog(
                    title = "Appel vocal",
                    peerUsername = state.peerUsername,
                    peerAvatar = state.peerAvatar,
                    subtitle = if (state.isConnecting) "Connexion audio…" else formattedDuration,
                    isMuted = state.isMuted,
                    isSpeakerOn = state.isSpeakerOn,
                    onToggleMute = { CallManager.toggleMute() },
                    onToggleSpeaker = { CallManager.toggleSpeakerphone() },
                    onEndCall = { CallManager.endCall() }
                )
            }
            is CallState.Ended -> {
                ActiveOrOutgoingCallDialog(
                    title = "Appel indisponible",
                    peerUsername = state.peerUsername,
                    peerAvatar = null,
                    subtitle = state.reason,
                    isMuted = false,
                    isSpeakerOn = true,
                    onToggleMute = {},
                    onToggleSpeaker = {},
                    onEndCall = { CallManager.resetToIdle() }
                )
            }
            CallState.Idle -> {}
        }
    }
}

@Composable
private fun IncomingCallDialog(
    callerUsername: String,
    callerAvatar: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onQuickReply: (String) -> Unit
) {
    var showQuickReplyModal by remember { mutableStateOf(false) }
    var customQuickMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.96f),
                            Color(0xFF020617).copy(alpha = 0.99f)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "APPEL VOCAL ENTRANT",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing Avatar with Glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                    )

                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF22C55E), CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!callerAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = callerAvatar,
                                contentDescription = callerUsername,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = callerUsername.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "@$callerUsername",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "veut vous parler en direct sur IDDET",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Action buttons: Decline (Red), Quick Reply (Message) and Accept (Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { onDecline() }
                                .testTag("decline_call_button"),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Refuser",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Refuser", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    }

                    // WhatsApp-style Quick Reply button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF334155),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable { showQuickReplyModal = true }
                                .testTag("quick_reply_call_button"),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "Message rapide",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Message", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                    }

                    // Accept button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF22C55E),
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable { onAccept() }
                                .testTag("accept_call_button"),
                            shadowElevation = 10.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Décrocher",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Décrocher", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Quick Reply Dialog Overlay (WhatsApp style)
            if (showQuickReplyModal) {
                Dialog(
                    onDismissRequest = { showQuickReplyModal = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Répondre par message",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Fermer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .clickable { showQuickReplyModal = false }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val quickPresets = listOf(
                                "Rappelle-moi plus tard",
                                "Rappelle-moi dans quelques minutes",
                                "Je suis en réunion, je te rappelle",
                                "Je ne peux pas parler pour le moment"
                            )

                            quickPresets.forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF334155),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            showQuickReplyModal = false
                                            onQuickReply(preset)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = preset,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom reply field
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = customQuickMessage,
                                    onValueChange = { customQuickMessage = it },
                                    placeholder = { Text("Écrire un message personnalisé...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val msg = customQuickMessage.trim()
                                        if (msg.isNotEmpty()) {
                                            showQuickReplyModal = false
                                            onQuickReply(msg)
                                        }
                                    },
                                    enabled = customQuickMessage.isNotBlank(),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (customQuickMessage.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF475569))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Envoyer",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOrOutgoingCallDialog(
    title: String,
    peerUsername: String,
    peerAvatar: String?,
    subtitle: String,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.96f),
                            Color(0xFF020617).copy(alpha = 0.99f)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp)
            ) {
                // Header status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Peer Avatar with subtle border
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!peerAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = peerAvatar,
                                contentDescription = peerUsername,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = peerUsername.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "@$peerUsername",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Relais Audio Haute Fidélité IDDET",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                // Control Buttons: Mute, Speaker, End
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    Surface(
                        shape = CircleShape,
                        color = if (isMuted) Color(0xFFEF4444) else Color(0xFF1E293B),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onToggleMute() }
                            .testTag("toggle_mute_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // End Call Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable { onEndCall() }
                            .testTag("end_call_button"),
                        shadowElevation = 10.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Raccrocher",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Speaker Button
                    Surface(
                        shape = CircleShape,
                        color = if (isSpeakerOn) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onToggleSpeaker() }
                            .testTag("toggle_speaker_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Haut-parleur",
                                tint = if (isSpeakerOn) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
