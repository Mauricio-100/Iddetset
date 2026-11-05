package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated presence dot indicating online / active status.
 */
@Composable
fun PresenceDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    borderWidth: Dp = 2.dp,
    showOffline: Boolean = false
) {
    if (!isOnline && !showOffline) return

    val infiniteTransition = rememberInfiniteTransition(label = "presence_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnline) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val dotColor = if (isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(if (isOnline) "presence_dot_online" else "presence_dot_offline")
            .semantics {
                contentDescription = if (isOnline) "En ligne" else "Hors ligne"
            }
    ) {
        // Outer pulsing ring for active users
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.35f))
            )
        }

        // Inner solid dot with clean border cutout
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(borderColor)
                .border(borderWidth, borderColor, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
