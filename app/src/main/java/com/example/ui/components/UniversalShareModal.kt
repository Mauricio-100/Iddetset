package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ConversationNetwork
import com.example.ui.theme.LocalAppColorScheme
import com.example.utils.ShareHelper

enum class ShareEntityType {
    PROFILE,
    COMMUNITY,
    ACTFILE,
    SOUND,
    VIDEO,
    APP
}

data class SharePayload(
    val type: ShareEntityType,
    val idOrSlug: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val isVerified: Boolean = false,
    val extraStat: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalShareModal(
    payload: SharePayload,
    conversations: List<ConversationNetwork> = emptyList(),
    onDismiss: () -> Unit,
    onSendToConversation: ((receiverId: String, messageText: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val appColors = LocalAppColorScheme.current
    var sentToUserIds by remember { mutableStateOf(setOf<String>()) }

    val webUrl = remember(payload) {
        when (payload.type) {
            ShareEntityType.PROFILE -> ShareHelper.getUserWebUrl(payload.idOrSlug)
            ShareEntityType.COMMUNITY -> ShareHelper.getCommunityWebUrl(payload.idOrSlug)
            ShareEntityType.ACTFILE -> ShareHelper.getActfileWebUrl(payload.idOrSlug)
            ShareEntityType.SOUND -> ShareHelper.getSoundWebUrl(payload.idOrSlug)
            ShareEntityType.VIDEO -> ShareHelper.getVideoWebUrl(payload.idOrSlug)
            ShareEntityType.APP -> ShareHelper.getAppWebUrl()
        }
    }

    val deepLink = remember(payload) {
        when (payload.type) {
            ShareEntityType.PROFILE -> ShareHelper.getUserDeepLink(payload.idOrSlug)
            ShareEntityType.COMMUNITY -> ShareHelper.getCommunityDeepLink(payload.idOrSlug)
            ShareEntityType.ACTFILE -> ShareHelper.getActfileDeepLink(payload.idOrSlug)
            ShareEntityType.SOUND -> ShareHelper.getSoundDeepLink(payload.idOrSlug)
            ShareEntityType.VIDEO -> ShareHelper.getVideoDeepLink(payload.idOrSlug)
            ShareEntityType.APP -> ShareHelper.getAppDeepLink()
        }
    }

    val typeLabel = remember(payload.type) {
        when (payload.type) {
            ShareEntityType.PROFILE -> "Portfolio Utilisateur"
            ShareEntityType.COMMUNITY -> "Communauté c/"
            ShareEntityType.ACTFILE -> "Publication Actfile"
            ShareEntityType.SOUND -> "Son STRIP Audio"
            ShareEntityType.VIDEO -> "Clip Vidéo / Wing"
            ShareEntityType.APP -> "Application IDDET"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Partager • $typeLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Partage complet multi-plateformes & in-app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entity Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!payload.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = payload.imageUrl,
                            contentDescription = payload.title,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(if (payload.type == ShareEntityType.PROFILE) CircleShape else RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (payload.type) {
                                    ShareEntityType.PROFILE -> Icons.Default.Person
                                    ShareEntityType.COMMUNITY -> Icons.Default.Groups
                                    ShareEntityType.ACTFILE -> Icons.Default.Article
                                    ShareEntityType.SOUND -> Icons.Default.MusicNote
                                    ShareEntityType.VIDEO -> Icons.Default.Videocam
                                    ShareEntityType.APP -> Icons.Default.Share
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = payload.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (payload.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Vérifié",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!payload.subtitle.isNullOrBlank()) {
                            Text(
                                text = payload.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!payload.extraStat.isNullOrBlank()) {
                            Text(
                                text = payload.extraStat,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Send to In-App Conversations
            if (conversations.isNotEmpty() && onSendToConversation != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "ENVOI RAPIDE EN MESSAGE PRIVÉ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(conversations) { conv ->
                        val isSent = sentToUserIds.contains(conv.user_id)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable {
                                    if (!isSent) {
                                        val shareText = "Regarde ça sur IDDET : $webUrl"
                                        onSendToConversation(conv.user_id, shareText)
                                        sentToUserIds = sentToUserIds + conv.user_id
                                        Toast.makeText(context, "Envoyé à @${conv.username}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (!conv.avatar_url.isNullOrBlank()) {
                                    AsyncImage(
                                        model = conv.avatar_url,
                                        contentDescription = conv.username,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = conv.username.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (isSent) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Envoyé",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSent) "Envoyé" else "@${conv.username}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSent) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Share Action Buttons
            Button(
                onClick = {
                    onDismiss()
                    when (payload.type) {
                        ShareEntityType.PROFILE -> ShareHelper.shareUserProfile(
                            context = context,
                            username = payload.idOrSlug,
                            displayName = payload.title,
                            bio = payload.description,
                            isVerified = payload.isVerified
                        )
                        ShareEntityType.COMMUNITY -> ShareHelper.shareCommunity(
                            context = context,
                            slug = payload.idOrSlug,
                            name = payload.title,
                            description = payload.description,
                            isVerified = payload.isVerified
                        )
                        ShareEntityType.ACTFILE -> ShareHelper.shareActfile(
                            context = context,
                            actfileId = payload.idOrSlug,
                            authorUsername = payload.subtitle?.removePrefix("@") ?: "",
                            content = payload.description
                        )
                        ShareEntityType.SOUND -> ShareHelper.shareSound(
                            context = context,
                            soundId = payload.idOrSlug,
                            title = payload.title,
                            authorUsername = payload.subtitle?.removePrefix("@") ?: ""
                        )
                        ShareEntityType.VIDEO -> ShareHelper.shareVideo(
                            context = context,
                            videoId = payload.idOrSlug,
                            authorUsername = payload.subtitle?.removePrefix("@"),
                            description = payload.description
                        )
                        ShareEntityType.APP -> ShareHelper.shareApp(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partager via d'autres applications", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Copy Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        ShareHelper.copyToClipboard(context, webUrl, "Lien web copié !")
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier Web", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        ShareHelper.copyToClipboard(context, deepLink, "Lien app iddet:// copié !")
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier App", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
