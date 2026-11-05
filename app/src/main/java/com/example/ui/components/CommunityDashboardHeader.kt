package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.data.getCategoryDefaultBanner
import com.example.data.getCategoryDefaultIcon
import com.example.data.getComputedOnlineCount
import com.example.utils.FormatUtils

@Composable
fun CommunityDashboardHeader(
    community: Community,
    onJoinClick: () -> Unit,
    onRulesClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    onNewPostClick: (() -> Unit)? = null,
    onEditIconClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpandedDescription by remember { mutableStateOf(false) }
    
    // Coherent, realistic online count calculation (never exceeding membersCount)
    val onlineCount = remember(community.membersCount, community.slug, community.onlineCount) {
        community.getComputedOnlineCount()
    }

    // Animated pulsing indicator for live online presence
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_header")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Banner with Reddit styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val bannerModel = if (!community.bannerUrl.isNullOrBlank()) {
                com.example.utils.UrlHelper.fixCloudinaryUrl(community.bannerUrl)
            } else {
                getCategoryDefaultBanner(community.category)
            }
            AsyncImage(
                model = bannerModel,
                contentDescription = "Bannière de la communauté",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            ),
                            startY = 0f
                        )
                    )
            )
        }

        // Community Profile row & Join button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-36).dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Avatar with high-contrast border
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .then(
                            if (onEditIconClick != null) Modifier.clickable { onEditIconClick() } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconModel = if (!community.iconUrl.isNullOrBlank()) {
                        com.example.utils.UrlHelper.fixCloudinaryUrl(community.iconUrl)
                    } else {
                        getCategoryDefaultIcon(community.category)
                    }
                    AsyncImage(
                        model = iconModel,
                        contentDescription = "Icône ${community.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (onEditIconClick != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Changer l'icône",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onShareClick != null) {
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .testTag("header_share_button")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Partager",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (onNewPostClick != null) {
                        FilledTonalButton(
                            onClick = onNewPostClick,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (community.isMember) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (community.isMember) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("detail_join_button")
                    ) {
                        if (community.isMember) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Membre", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rejoindre", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Community Name & Reddit Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (community.isPrivate) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privé",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Privé",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "c/${community.slug}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Category Flair Pill
                val catInfo = com.example.ui.components.getCategoryById(community.category)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (catInfo?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${catInfo?.emoji ?: "🏷️"} ${community.category}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = catInfo?.color ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Description
            if (!community.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = community.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpandedDescription) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isExpandedDescription = !isExpandedDescription }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reddit Stats Bar: Members + Online indicator + Rules shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Members
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = FormatUtils.formatCount(community.membersCount),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "membres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Online active members with animated green pulse dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${FormatUtils.formatCount(onlineCount)} en ligne",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick rules link
                TextButton(
                    onClick = onRulesClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Règles",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
