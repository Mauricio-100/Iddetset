package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.data.IddetRepository
import com.example.data.getCategoryDefaultBanner
import com.example.data.getCategoryDefaultIcon
import com.example.data.getComputedOnlineCount
import com.example.utils.FormatUtils

/**
 * Rich WhatsApp / OpenGraph-style preview banner for community links and slugs (c/slug, /communities/slug).
 * Shows community banner, avatar, title, category, member count, and live online presence.
 * Clicking opens the community directly.
 */
@Composable
fun CommunityOpenGraphCard(
    slug: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    initialCommunity: Community? = null,
    onCommunityClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val communityClickHandler = LocalCommunityClickHandler.current
    val effectiveClickHandler = onCommunityClick ?: { targetSlug ->
        communityClickHandler?.invoke(targetSlug)
    }

    var community by remember(slug) { mutableStateOf(initialCommunity) }
    var isLoading by remember(slug) { mutableStateOf(initialCommunity == null) }

    LaunchedEffect(slug) {
        if (community == null) {
            val repo = IddetRepository.getInstance(context)
            val resolved = repo.getCommunity(slug)
            community = resolved ?: Community(
                id = "com_$slug",
                slug = slug.lowercase(),
                name = slug.replaceFirstChar { it.uppercase() },
                category = "Général",
                description = "Rejoignez la communauté c/$slug sur IDDET pour échanger, partager et participer aux salons.",
                membersCount = 48,
                isMember = false
            )
            isLoading = false
        }
    }

    val currentCommunity = community ?: return

    val cardShape = RoundedCornerShape(16.dp)
    val cardBorder = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )

    // Pulsing animation for the "En ligne" live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_online")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { effectiveClickHandler(currentCommunity.slug) }
            .testTag("community_opengraph_banner_${currentCommunity.slug}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Banner Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 85.dp else 110.dp)
            ) {
                val bannerModel = if (!currentCommunity.bannerUrl.isNullOrBlank()) {
                    com.example.utils.UrlHelper.fixCloudinaryUrl(currentCommunity.bannerUrl)
                } else {
                    getCategoryDefaultBanner(currentCommunity.category)
                }

                AsyncImage(
                    model = bannerModel,
                    contentDescription = "Bannière ${currentCommunity.name}",
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
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glassmorphism Community Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA), // Lavender / Purple accent
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COMMUNAUTÉ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Category Pill
                    val catInfo = getCategoryById(currentCommunity.category)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "${catInfo?.emoji ?: "🏷️"} ${currentCommunity.category}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 2. Identity and Details Section (Avatar overlapping banner)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Overlapping circular avatar
                    Box(
                        modifier = Modifier
                            .size(if (compact) 48.dp else 56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconModel = if (!currentCommunity.iconUrl.isNullOrBlank()) {
                            com.example.utils.UrlHelper.fixCloudinaryUrl(currentCommunity.iconUrl)
                        } else {
                            getCategoryDefaultIcon(currentCommunity.category)
                        }
                        AsyncImage(
                            model = iconModel,
                            contentDescription = "Icône ${currentCommunity.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Quick Action Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { effectiveClickHandler(currentCommunity.slug) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentCommunity.isMember) "Ouvrir" else "Rejoindre",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Title and Slug
                Column(modifier = Modifier.offset(y = (-14).dp)) {
                    Text(
                        text = currentCommunity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Slug & Live Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Slug tag: c/slug
                        Text(
                            text = "c/${currentCommunity.slug}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF8B5CF6) // Distinctive Community Purple
                            )
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Member count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${FormatUtils.formatCount(currentCommunity.membersCount)} membres",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Online count with animated green pulsing indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${FormatUtils.formatCount(currentCommunity.getComputedOnlineCount())} en ligne",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            )
                        }
                    }

                    // Short Description (if present)
                    if (!currentCommunity.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentCommunity.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compact) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
