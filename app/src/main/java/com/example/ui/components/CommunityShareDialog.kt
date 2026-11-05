package com.example.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.data.ConversationNetwork
import com.example.data.getCategoryDefaultBanner
import com.example.data.getCategoryDefaultIcon
import com.example.utils.FormatUtils

@Composable
fun CommunityShareDialog(
    community: Community,
    conversations: List<ConversationNetwork>,
    onDismiss: () -> Unit,
    onSendToConversations: (List<String>, String) -> Unit // (receiverIds, messageContent)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val selectedUserIds = remember { mutableStateListOf<String>() }
    var shareSuccessMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("community_share_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Partager la communauté",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Rich Decorated Community Preview Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Banner with decoration
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                ) {
                                    val bannerModel = if (!community.bannerUrl.isNullOrBlank()) {
                                        com.example.utils.UrlHelper.fixCloudinaryUrl(community.bannerUrl)
                                    } else {
                                        getCategoryDefaultBanner(community.category)
                                    }
                                    AsyncImage(
                                        model = bannerModel,
                                        contentDescription = "Bannière",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.15f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.65f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Profile details row
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = (-28).dp)
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Decorated Profile Photo
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(2.5.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val iconModel = if (!community.iconUrl.isNullOrBlank()) {
                                                com.example.utils.UrlHelper.fixCloudinaryUrl(community.iconUrl)
                                            } else {
                                                getCategoryDefaultIcon(community.category)
                                            }
                                            AsyncImage(
                                                model = iconModel,
                                                contentDescription = "Icône",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Badge / Shortcut Pill
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "/${community.slug}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = community.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "c/${community.slug}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        val catInfo = com.example.ui.components.getCategoryById(community.category)
                                        Text(
                                            text = "• ${catInfo?.emoji ?: "🏷️"} ${community.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "• ${FormatUtils.formatCount(community.membersCount)} membres",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (!community.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = community.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 2: Direct In-App Sharing to Contacts / Friends
                    item {
                        Text(
                            text = "Envoyer directement à vos amis sur IDDET",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Partage le raccourci /${community.slug} directement dans la conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (conversations.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Aucune discussion récente trouvée pour le partage direct.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(conversations, key = { it.user_id }) { conv ->
                                    val isSelected = selectedUserIds.contains(conv.user_id)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable {
                                                if (isSelected) {
                                                    selectedUserIds.remove(conv.user_id)
                                                } else {
                                                    selectedUserIds.add(conv.user_id)
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = conv.avatar_url ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&q=80",
                                                contentDescription = conv.username,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = conv.username,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedUserIds.isNotEmpty()) {
                            item {
                                Button(
                                    onClick = {
                                        val messageText = "Rejoins la communauté ${community.name} !\n/${community.slug}"
                                        onSendToConversations(selectedUserIds.toList(), messageText)
                                        shareSuccessMessage = "Partagé avec ${selectedUserIds.size} ami(s) !"
                                        Toast.makeText(context, "Communauté partagée avec succès !", Toast.LENGTH_SHORT).show()
                                        selectedUserIds.clear()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_share_to_contacts_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Envoyer à (${selectedUserIds.size}) ami(s)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (shareSuccessMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = shareSuccessMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Item 3: Quick Copy Actions
                    item {
                        Text(
                            text = "Liens et raccourcis rapides",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    item {
                        OutlinedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Shortcut row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Raccourci IDDET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("/${community.slug}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("/${community.slug}"))
                                            Toast.makeText(context, "Raccourci /${community.slug} copié !", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copier")
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Tag row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Tag Communauté", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("c/${community.slug}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("c/${community.slug}"))
                                            Toast.makeText(context, "Tag c/${community.slug} copié !", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copier")
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Full URL link row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Lien Web & Aperçu OpenGraph", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("https://hoosthubs-g.onrender.com/c/${community.slug}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("https://hoosthubs-g.onrender.com/c/${community.slug}"))
                                            Toast.makeText(context, "Lien web copié !", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copier")
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Deep Link Scheme
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Lien Application Direct (Deep Link)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("iddet://community/${community.slug}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("iddet://community/${community.slug}"))
                                            Toast.makeText(context, "Lien iddet:// copié !", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copier")
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: External Android Share Sheet
                    item {
                        OutlinedButton(
                            onClick = {
                                com.example.utils.ShareHelper.shareCommunity(
                                    context = context,
                                    slug = community.slug,
                                    name = community.name,
                                    description = community.description,
                                    membersCount = community.membersCount,
                                    isVerified = false
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Partager via d'autres applications...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
