package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ActfileWithUser
import com.example.data.ActfileMetadataHelper
import com.example.data.AttachedSound
import com.example.data.getCategoryDefaultIcon
import com.example.utils.MusicPlayerManager
import com.example.utils.MusicTrack
import com.example.ui.components.VerificationBadge
import com.example.ui.components.CopyableUserId
import com.example.ui.components.MarkdownContent
import com.example.ui.components.TagChip
import kotlinx.coroutines.launch

// Helper to calculate relative time
fun getRelativeTimeString(timeMs: Long): String {
    return try {
        android.text.format.DateUtils.getRelativeTimeSpanString(
            timeMs,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (e: Exception) {
        val diffSec = (System.currentTimeMillis() - timeMs) / 1000
        when {
            diffSec < 60 -> "à l'instant"
            diffSec < 3600 -> "il y a ${diffSec / 60}m"
            diffSec < 86400 -> "il y a ${diffSec / 3600}h"
            else -> "il y a ${diffSec / 86400}j"
        }
    }
}

@Composable
fun ActfileCard(
    actfile: ActfileWithUser,
    onLike: (String) -> Unit,
    onView: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onComment: (String) -> Unit,
    targetLanguageName: String = "French",
    isAiReady: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    onCommunityClick: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    isDetailView: Boolean = false,
    onPromote: ((ActfileWithUser) -> Unit)? = null
) {
    val context = LocalContext.current
    val localCommunityHandler = LocalCommunityClickHandler.current
    val effectiveCommunityClick: (String) -> Unit = { target ->
        val cleanSlug = target.removePrefix("@c/").removePrefix("c/").removePrefix("/").trim()
        if (onCommunityClick != null) {
            onCommunityClick(cleanSlug)
        } else if (localCommunityHandler != null) {
            localCommunityHandler(cleanSlug)
        } else {
            onCategoryClick?.invoke(cleanSlug)
        }
    }
    var translatedContent by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val relativeTime = remember(actfile.createdAt) { getRelativeTimeString(actfile.createdAt) }
    
    // Animated like scale
    val likeScale by animateFloatAsState(
        targetValue = if (actfile.isLikedByMe) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "like_scale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (actfile.isLikedByMe) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "like_color"
    )

    // Automatically register views
    LaunchedEffect(actfile.id) {
        onView(actfile.id)
    }

    val isCommunityPost = !actfile.communityId.isNullOrBlank() || !actfile.channelSlug.isNullOrBlank()
    val avatarSize = if (isDetailView) 52.dp else 42.dp
    val miniAvatarSize = 20.dp

    val moneyDisplay = remember(actfile.adBudget, actfile.adCurrency) {
        val b = actfile.adBudget ?: 5.0
        val c = actfile.adCurrency ?: "USD"
        if (c.equals("CDF", true) || c.equals("FC", true)) {
            "${b.toInt()} FC"
        } else {
            if (b % 1.0 == 0.0) "$${b.toInt()}.00" else String.format(java.util.Locale.US, "$%.2f", b)
        }
    }
    var showSponsoredInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (actfile.isSponsored) {
                    Modifier.border(0.6.dp, Color(0xFFEAB308).copy(alpha = 0.22f))
                } else {
                    Modifier
                }
            )
            .background(
                if (actfile.isSponsored) {
                    Color(0xFFFEF3C7).copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable { onView(actfile.id) }
            .testTag("actfile_card_${actfile.id}")
    ) {
        if (actfile.isSponsored) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color(0xFFEAB308).copy(alpha = 0.0f),
                                Color(0xFFEAB308).copy(alpha = 0.7f),
                                Color(0xFFFDE047),
                                Color(0xFFEAB308).copy(alpha = 0.7f),
                                Color(0xFFEAB308).copy(alpha = 0.0f)
                            )
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Community Label & Icon Banner (if published in a community, visible in home feed)
            val parsedComm = remember(actfile.content) { ActfileMetadataHelper.parseCommunity(actfile.content) }
            val effectiveCommunityName = actfile.communityName?.ifBlank { null } ?: parsedComm?.first?.ifBlank { null } ?: actfile.channelName?.ifBlank { null }
            val effectiveCommunitySlug = actfile.communityId?.ifBlank { null } ?: actfile.channelSlug?.ifBlank { null } ?: if (actfile.content.contains("@c/")) {
                Regex("@c/([a-zA-Z0-9_-]+)").find(actfile.content)?.groupValues?.get(1)
            } else null
            val effectiveCommunityIcon = actfile.communityIconUrl?.ifBlank { null } ?: parsedComm?.second?.ifBlank { null } ?: (if (!effectiveCommunitySlug.isNullOrBlank()) getCategoryDefaultIcon(actfile.category ?: "general") else null)
            val hasCommunityInfo = !effectiveCommunityName.isNullOrBlank() || !effectiveCommunitySlug.isNullOrBlank()

            if (hasCommunityInfo) {
                val pillInteractionSource = remember { MutableInteractionSource() }
                val isPillPressed by pillInteractionSource.collectIsPressedAsState()
                val isPillHovered by pillInteractionSource.collectIsHoveredAsState()

                val pillScale by animateFloatAsState(
                    targetValue = if (isPillPressed) 0.97f else if (isPillHovered) 1.02f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "comm_pill_scale"
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .scale(pillScale)
                        .clickable(
                            interactionSource = pillInteractionSource,
                            indication = ripple(),
                            onClick = {
                                val target = effectiveCommunitySlug ?: effectiveCommunityName ?: ""
                                effectiveCommunityClick(target)
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!effectiveCommunityIcon.isNullOrBlank()) {
                                AsyncImage(
                                    model = com.example.utils.UrlHelper.fixCloudinaryUrl(effectiveCommunityIcon),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Communauté",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = effectiveCommunityName ?: "c/$effectiveCommunitySlug",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (actfile.communityIsVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(
                                userName = effectiveCommunityName ?: effectiveCommunitySlug ?: "",
                                isVerified = true,
                                isCommunity = true,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (!actfile.channelName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "#${actfile.channelName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCommunityPost) {
                    // Standard Author Avatar
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onUserClick(actfile.userId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!actfile.avatarUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val avatarModel = remember(actfile.avatarUrl) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(com.example.utils.UrlHelper.fixCloudinaryUrl(actfile.avatarUrl))
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .build()
                            }
                            AsyncImage(
                                model = avatarModel,
                                contentDescription = "Avatar de ${actfile.username}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = actfile.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Username + VerificationBadge + Timestamp relative
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUserClick(actfile.userId) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actfile.username,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(
                                userName = actfile.username,
                                isVerified = actfile.isVerified,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            text = "@${actfile.username} • $relativeTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        CopyableUserId(
                            id = actfile.userId,
                            isBot = actfile.username.contains("bot", ignoreCase = true)
                        )
                    }

                } else {
                    // CAS 2 : Publication communautaire
                    val commIconUrl = actfile.communityIconUrl?.ifBlank { null }
                        ?: remember(actfile.category) { getCategoryDefaultIcon(actfile.category ?: "general") }

                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { effectiveCommunityClick(actfile.communityId ?: actfile.channelSlug ?: "") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = com.example.utils.UrlHelper.fixCloudinaryUrl(commIconUrl),
                            contentDescription = "Communauté ${actfile.communityName ?: actfile.channelSlug}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Community Details + Author secondary line
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Line 1: Community Name or c/slug in bold + optional channel chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val displayCommName = actfile.communityName?.ifBlank { null }
                                ?: if (!actfile.communityId.isNullOrBlank()) "c/${actfile.communityId}"
                                else if (!actfile.channelSlug.isNullOrBlank()) "c/${actfile.channelSlug}"
                                else "Communauté"

                            Text(
                                text = displayCommName,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { effectiveCommunityClick(actfile.communityId ?: actfile.channelSlug ?: "") }
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            if (actfile.communityIsVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadge(
                                    userName = displayCommName,
                                    isVerified = true,
                                    isCommunity = true,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (!actfile.channelName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${actfile.channelName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Line 2: mini-avatar + Username + VerificationBadge + Timestamp
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(actfile.userId) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(miniAvatarSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!actfile.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = actfile.avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = actfile.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = actfile.username,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            VerificationBadge(
                                userName = actfile.username,
                                isVerified = actfile.isVerified,
                                modifier = Modifier.size(12.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "• $relativeTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        CopyableUserId(
                            id = actfile.userId,
                            isBot = actfile.username.contains("bot", ignoreCase = true)
                        )
                    }
                }

                // View count pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(end = if (onDelete != null) 4.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Vues",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${com.example.utils.FormatUtils.formatCount(actfile.viewsCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Promote / Ads campaign button
                if (onPromote != null) {
                    IconButton(
                        onClick = { onPromote(actfile) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = if (actfile.isSponsored) "Campagne publicitaire active" else "Sponsoriser (IDDET Ads)",
                            tint = if (actfile.isSponsored) IddetAdsGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Delete button for post owner
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete(actfile.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer la publication",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content & Markdown
            if (!showOriginal && translatedContent != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable { showOriginal = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Traduit en $targetLanguageName • Voir original",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Cleaned content without embedded metadata tags
            val cleanedRawContent = remember(actfile.content) { ActfileMetadataHelper.cleanContent(actfile.content) }

            MarkdownContent(
                content = (if (showOriginal) cleanedRawContent else translatedContent) ?: cleanedRawContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onComment(actfile.id) }
                    .testTag("actfile_body_${actfile.id}"),
                isDetailView = isDetailView,
                onReadMoreClick = { onComment(actfile.id) },
                onMentionClick = onMentionClick,
                onLinkClick = onLinkClick
            )

            // Attached Sound Player Card (with > to play, || to pause, sound title & creator name)
            val attachedSound = remember(actfile) {
                if (!actfile.soundTitle.isNullOrBlank()) {
                    AttachedSound(
                        id = actfile.soundId ?: "",
                        title = actfile.soundTitle ?: "",
                        author = actfile.soundAuthor ?: "",
                        audioUrl = actfile.soundAudioUrl ?: "",
                        coverUrl = actfile.soundCoverUrl ?: ""
                    )
                } else {
                    ActfileMetadataHelper.parseSound(actfile.content)
                }
            }

            val playerState by MusicPlayerManager.state.collectAsState()
            val isPlayingThisSound = playerState.isPlaying && attachedSound != null && (
                (attachedSound.id.isNotBlank() && playerState.currentTrack?.id == attachedSound.id) ||
                (attachedSound.title.isNotBlank() && playerState.currentTrack?.title == attachedSound.title)
            )

            if (attachedSound != null && attachedSound.title.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isPlayingThisSound) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play (>) or Pause (||)
                        FilledIconButton(
                            onClick = {
                                val soundUrl = if (attachedSound.audioUrl.isNotBlank()) {
                                    attachedSound.audioUrl
                                } else if (attachedSound.id.isNotBlank()) {
                                    "https://hoosthubs-g.onrender.com/api/sounds/${attachedSound.id}/short/stream"
                                } else ""

                                val track = MusicTrack(
                                    id = if (attachedSound.id.isNotBlank()) attachedSound.id else actfile.id,
                                    title = attachedSound.title,
                                    artist = if (attachedSound.author.isNotBlank()) attachedSound.author else "Créateur",
                                    albumArt = attachedSound.coverUrl,
                                    audioUrl = soundUrl,
                                    durationFormatted = "03:00",
                                    genre = "Actfile Sound",
                                    likesCount = 0
                                )
                                if (isPlayingThisSound) {
                                    MusicPlayerManager.togglePlayPause()
                                } else {
                                    MusicPlayerManager.playTrack(track)
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isPlayingThisSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isPlayingThisSound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingThisSound) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingThisSound) "Pause" else "Lancer le son",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isPlayingThisSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = attachedSound.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "par @${attachedSound.author.ifBlank { "créateur" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isPlayingThisSound) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "🎵 En écoute",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic tags rendering above action bar
            val tagsList = remember(actfile.content, actfile.tags) {
                val foundTags = Regex("#([a-zA-Z0-9_À-ÿ]+)")
                    .findAll(actfile.content)
                    .map { it.groupValues[1] }
                    .toList()
                val modelTags = (actfile.tags ?: "")
                    .split(",")
                    .map { it.trim().removePrefix("#") }
                    .filter { it.isNotBlank() }
                (foundTags + modelTags).distinct()
            }

            if (tagsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tagsList.forEach { tag ->
                        TagChip(
                            tagName = tag,
                            onClick = { onMentionClick?.invoke("#$tag") }
                        )
                    }
                }
            }

            // 8. Sponsored Badge - En bas à gauche (plus petit et plus propre)
            if (actfile.isSponsored || actfile.adStatus == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SponsoredBadge(
                        isPending = actfile.adStatus == "pending",
                        moneyText = moneyDisplay,
                        onClick = {
                            val msg = "Ce projet a été sponsorisé par IDDET pour $moneyDisplay"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            showSponsoredInfoDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal bottom action bar & Category badge aligned to the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Like Button with responsive glow
                    BouncyButton(
                        onClick = { onLike(actfile.id) },
                        pressedScale = 0.88f
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (actfile.isLikedByMe) Color(0xFFFF2D55).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .scale(likeScale),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (actfile.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (actfile.isLikedByMe) "Ne plus aimer" else "Aimer",
                                    tint = likeColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${com.example.utils.FormatUtils.formatCount(actfile.likesCount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = likeColor
                                )
                            }
                        }
                    }

                    // Comment Button
                    BouncyButton(
                        onClick = { onComment(actfile.id) },
                        pressedScale = 0.88f
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = "Commenter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${com.example.utils.FormatUtils.formatCount(actfile.commentsCount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Share Button
                    BouncyButton(
                        onClick = {
                            if (onShare != null) {
                                onShare(actfile.id)
                            } else {
                                com.example.utils.ShareHelper.shareActfile(
                                    context = context,
                                    actfileId = actfile.id,
                                    authorUsername = actfile.username,
                                    content = actfile.content,
                                    category = actfile.category
                                )
                            }
                        },
                        pressedScale = 0.88f
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Partager",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Translate Button
                    BouncyButton(
                        onClick = {
                            if (showOriginal) {
                                if (translatedContent == null) {
                                    isTranslating = true
                                    coroutineScope.launch {
                                        try {
                                            val res = com.example.utils.TranslationHelper.translateText(actfile.content, targetLanguageName)
                                            if (res.isNotBlank()) {
                                                translatedContent = res
                                                showOriginal = false
                                                android.widget.Toast.makeText(context, "Traduit en $targetLanguageName", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Texte original conservé", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Traduction temporairement indisponible", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isTranslating = false
                                        }
                                    }
                                } else {
                                    showOriginal = false
                                }
                            } else {
                                showOriginal = true
                            }
                        },
                        pressedScale = 0.88f
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!showOriginal) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Translate,
                                        contentDescription = "Traduire",
                                        tint = if (!showOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Badge (e.g. #Tech) aligned to the right
                val catInfo = com.example.ui.components.getCategoryById(actfile.category)
                if (catInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = catInfo.color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = catInfo.color.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${catInfo.emoji} #${catInfo.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catInfo.color
                            )
                        }
                    }
                }
            }
        }
        if (!isDetailView) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }

        // Dialogue informatif IDDET Ads au clic sur le badge sponsorisé
        if (showSponsoredInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSponsoredInfoDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "IDDET Ads • Sponsorisé",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ce projet a été sponsorisé par IDDET pour $moneyDisplay",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cette publication bénéficie d'une visibilité amplifiée dans le flux d'actualités grâce au programme publicitaire IDDET Ads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSponsoredInfoDialog = false }) {
                        Text("Compris", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = if (onPromote != null) {
                    {
                        TextButton(
                            onClick = {
                                showSponsoredInfoDialog = false
                                onPromote(actfile)
                            }
                        ) {
                            Text("Gérer l'annonce")
                        }
                    }
                } else null
            )
        }
    }
}

