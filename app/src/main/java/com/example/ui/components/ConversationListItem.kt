package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ConversationNetwork
import java.text.SimpleDateFormat
import java.util.*

/**
 * Standard list item for 1-on-1 conversations in iDDET.
 */
@Composable
fun ConversationListItem(
    conversation: ConversationNetwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadCount = conversation.unread_count ?: 0
    val hasUnread = unreadCount > 0

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "item_press_scale"
    )

    val relativeTime = remember(conversation.last_message_time) {
        formatConversationTime(conversation.last_message_time)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("conversation_item_${conversation.user_id}"),
        color = if (hasUnread) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Coil and Presence Dot
            val avatarModel = remember(conversation.avatar_url) {
                conversation.avatar_url?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) } ?: conversation.avatar_url
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .testTag("conversation_avatar_box_${conversation.user_id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = conversation.username.firstOrNull()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    if (!avatarModel.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "Avatar de ${conversation.username}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Presence Dot at bottom-right corner
                if (conversation.is_online) {
                    PresenceDot(
                        isOnline = true,
                        size = 14.dp,
                        borderColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center details: Name + badges + last message preview
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Name + Badges row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conversation.username,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Iddet Plus Badge
                    if (conversation.is_iddet_plus) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IddetPlusBadge(size = 16.dp)
                    }

                    // Verified / Official Badge
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(
                        isVerified = conversation.is_verified,
                        userName = conversation.username,
                        showExplainingOnClick = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Last message preview with type-specific icon / label
                LastMessagePreview(
                    lastMessage = conversation.last_message,
                    hasUnread = hasUnread
                )
            }

            // Right column: Relative timestamp + Unread counter
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (relativeTime.isNotEmpty()) {
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (hasUnread) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .testTag("unread_badge_${conversation.user_id}")
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview renderer for the conversation's last message.
 * Converts multimedia payloads (photos, videos, voice) into friendly icons + labels.
 */
@Composable
private fun LastMessagePreview(
    lastMessage: String?,
    hasUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (hasUnread) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (lastMessage.isNullOrBlank()) {
        Text(
            text = "Nouvelle discussion",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    val trimmed = lastMessage.trim()
    val isStory = trimmed.startsWith("[Story:") || trimmed.startsWith("📷 Réponse à votre story:") ||
            (trimmed.startsWith("❤️") && trimmed.contains("Réaction à votre story"))
    val isVoice = com.example.utils.AudioMessageHelper.isAudioContent(trimmed) ||
            trimmed.startsWith("[Voice Message]") || trimmed.contains("voice://") ||
            trimmed.contains(".mp3") || trimmed.contains(".m4a") || trimmed.contains(".aac") ||
            trimmed.startsWith("audio:") || trimmed.contains("Message vocal")
    val isImage = trimmed.startsWith("[Photo]") || trimmed.startsWith("image:") ||
            (trimmed.startsWith("http") && (trimmed.endsWith(".jpg") || trimmed.endsWith(".png") || trimmed.endsWith(".webp") || trimmed.endsWith(".jpeg")))
    val isVideo = trimmed.startsWith("[Vidéo]") || trimmed.startsWith("video:") ||
            (trimmed.startsWith("http") && (trimmed.endsWith(".mp4") || trimmed.endsWith(".webm") || trimmed.endsWith(".mov")))

    val storyPreviewText = when {
        trimmed.startsWith("[Story:") -> {
            val afterBracket = trimmed.substringAfter("]", "").trim()
            if (afterBracket.isNotBlank()) "Story • $afterBracket" else "Réponse à la story"
        }
        trimmed.startsWith("📷 Réponse à votre story:") -> {
            val reply = trimmed.removePrefix("📷 Réponse à votre story:").trim()
            if (reply.isNotBlank()) "Story • $reply" else "Réponse à votre story"
        }
        trimmed.startsWith("❤️") && trimmed.contains("Réaction à votre story") -> {
            val emoji = trimmed.split(" ").getOrNull(1) ?: "❤️"
            "Story • $emoji"
        }
        else -> "Story"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        when {
            isStory -> {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = storyPreviewText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            isVoice -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Message vocal",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            isImage -> {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            isVideo -> {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Vidéo",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> {
                Text(
                    text = trimmed,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Helper to format ISO timestamp into relative display string ("14:32", "Hier", "Lun", "12/05").
 */
fun formatConversationTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return ""

    val timestamp = try {
        val clean = isoTime.substringBefore(".")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(clean)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        try {
            isoTime.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }

    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) {
        return "Hier"
    }

    val diffDays = (now.timeInMillis - timestamp) / (24 * 60 * 60 * 1000)
    if (diffDays in 2..6) {
        val dayFormat = SimpleDateFormat("EEE", Locale.FRENCH)
        return dayFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
    }

    return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
}
