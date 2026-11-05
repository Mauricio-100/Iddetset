package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DEFAULT_POPULAR_EMOJIS = listOf("❤️", "👍", "🔥", "😂", "😮", "😢", "👏", "🎉")

/**
 * Floating horizontal bar allowing users to quickly pick an emoji reaction.
 */
@Composable
fun ReactionPicker(
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String> = DEFAULT_POPULAR_EMOJIS
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .testTag("reaction_picker"),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 22.dp)
                        ) {
                            onSelectEmoji(emoji)
                        }
                        .testTag("reaction_emoji_$emoji")
                        .semantics {
                            contentDescription = "Réagir avec $emoji"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
