package com.example.ui.components
 
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.data.getCategoryDefaultIcon

@Composable
fun CommunitySuggestionRow(
    communities: List<Community>,
    onJoinToggle: (String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Show up to 5 suggested communities as requested
    val displayedCommunities = remember(communities) {
        communities.take(5)
    }

    if (displayedCommunities.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(vertical = 16.dp)
            .testTag("community_suggestion_row")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Suggestions pour vous",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Rejoignez de nouvelles communautés actives",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayedCommunities, key = { it.id }) { community ->
                CommunitySuggestionCard(
                    community = community,
                    onJoinToggle = { onJoinToggle(community.slug) },
                    onClick = { onClick(community.slug) }
                )
            }
        }
    }
}

@Composable
fun CommunitySuggestionCard(
    community: Community,
    onJoinToggle: () -> Unit,
    onClick: () -> Unit
) {
    val categoryInfo = getCategoryById(community.category)
    val cardColor = categoryInfo?.color ?: MaterialTheme.colorScheme.secondary

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else if (isHovered) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "suggestion_card_scale"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else if (isHovered) 8.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "suggestion_card_elevation"
    )

    Card(
        modifier = Modifier
            .width(260.dp)
            .scale(animatedScale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("suggested_community_${community.slug}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Unsplash Cover Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .background(cardColor.copy(alpha = 0.2f))
            ) {
                val coverUrl = remember(community.bannerUrl, community.category) {
                    if (!community.bannerUrl.isNullOrBlank()) com.example.utils.UrlHelper.fixCloudinaryUrl(community.bannerUrl) else {
                        // Deterministic visual category banners from Unsplash
                        when (community.category) {
                            "Fun" -> "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400&q=80"
                            "Amour" -> "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=400&q=80"
                            "Motivation" -> "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&q=80"
                            "Tech" -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=400&q=80"
                            "Sport" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&q=80"
                            "Musique" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80"
                            "Actu" -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=400&q=80"
                            "Business" -> "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400&q=80"
                            "Spiritualité" -> "https://images.unsplash.com/photo-1474552226712-ac0f0961a954?w=400&q=80"
                            else -> "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=400&q=80"
                        }
                    }
                }

                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Category pill overlay
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cardColor.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = community.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Info Section: Icon + Names
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconUrl = remember(community.iconUrl, community.category) {
                            if (!community.iconUrl.isNullOrBlank()) community.iconUrl else getCategoryDefaultIcon(community.category)
                        }
                        AsyncImage(
                            model = iconUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "c/${community.slug}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CopyableUserId(
                            id = community.id,
                            isBot = false,
                            customLabel = "c_id: ${community.id.take(8)}",
                            fontSize = 9.sp,
                            iconSize = 10.dp
                        )
                    }
                }

                if (!community.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = community.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                } else {
                    Spacer(modifier = Modifier.height(38.dp)) // Maintain aspect ratio height
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Members count + Rejoindre/Quitter button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${community.membersCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dynamic "Rejoindre/Quitter" state-dependent button
                    val isMember = community.isMember
                    Button(
                        onClick = onJoinToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMember) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isMember) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isMember) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isMember) "Quitter" else "Rejoindre",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
