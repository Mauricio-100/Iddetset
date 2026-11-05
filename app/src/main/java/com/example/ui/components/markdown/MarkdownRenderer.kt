package com.example.ui.components.markdown

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.ActfileVideoPlayer
import com.example.ui.components.VideoUrlHelper
import com.example.ui.components.LocalChannelClickHandler
import com.example.ui.components.LocalCommunityClickHandler
import com.example.ui.components.OpenGraphPreview
import com.example.ui.components.extractUrlsFromMarkdown

/**
 * High-performance, modular Markdown Renderer for Jetpack Compose.
 * Uses an AST parsed by [MarkdownParser] with cached node representation via [remember].
 */
@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    compactOpenGraph: Boolean = false,
    truncateChars: Int? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onReadMoreClick: (() -> Unit)? = null
) {
    val hideOgPhoto = remember(content) { content.contains("<!--hide_og_photo-->") }
    val cleanedContent = remember(content) { content.replace("<!--hide_og_photo-->", "").trim() }

    // Cache parsed Markdown AST of the entire content to always detect all media (images, videos)
    val allNodes = remember(cleanedContent) {
        MarkdownParser.parse(cleanedContent)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = androidx.compose.material3.LocalContentColor.current
    val secondaryTextColor = textColor.copy(alpha = 0.7f)
    val communityClickHandler = LocalCommunityClickHandler.current

    val isFeedMode = truncateChars != null

    // Reddit-style post partitioning:
    // In feed mode, show text preview (with "Voir plus" if long), and place extracted media (images/videos) at the bottom.
    val (displayNodes, isTextTruncated, mediaNodes) = remember(allNodes, truncateChars) {
        if (truncateChars == null) {
            Triple(allNodes, false, emptyList<MarkdownNode>())
        } else {
            val media = allNodes.filter {
                it is MarkdownNode.ImageNode || it is MarkdownNode.CarouselNode || it is MarkdownNode.VideoNode
            }
            val textOnly = allNodes.filter {
                it !is MarkdownNode.ImageNode && it !is MarkdownNode.CarouselNode && it !is MarkdownNode.VideoNode
            }

            var accumulatedLength = 0
            val previewText = mutableListOf<MarkdownNode>()
            var truncated = false

            for (node in textOnly) {
                val nodeLength = when (node) {
                    is MarkdownNode.Heading -> node.text.length
                    is MarkdownNode.Paragraph -> node.text.length
                    is MarkdownNode.Blockquote -> node.text.length
                    is MarkdownNode.BulletList -> node.items.sumOf { it.length }
                    is MarkdownNode.NumberedList -> node.items.sumOf { it.length }
                    is MarkdownNode.Checklist -> node.items.sumOf { it.text.length }
                    is MarkdownNode.CodeBlock -> node.code.length
                    else -> 0
                }

                if (previewText.isEmpty() && nodeLength > truncateChars) {
                    val truncatedNode = when (node) {
                        is MarkdownNode.Paragraph -> MarkdownNode.Paragraph(node.text.take(truncateChars).trimEnd() + "...")
                        is MarkdownNode.Heading -> MarkdownNode.Heading(node.level, node.text.take(truncateChars).trimEnd() + "...")
                        is MarkdownNode.Blockquote -> MarkdownNode.Blockquote(node.text.take(truncateChars).trimEnd() + "...")
                        is MarkdownNode.CodeBlock -> MarkdownNode.CodeBlock(node.language, node.code.take(truncateChars).trimEnd() + "...")
                        else -> node
                    }
                    previewText.add(truncatedNode)
                    truncated = true
                    break
                } else if (accumulatedLength + nodeLength <= truncateChars) {
                    previewText.add(node)
                    accumulatedLength += nodeLength
                } else {
                    truncated = true
                    break
                }
            }

            if (previewText.size < textOnly.size) {
                truncated = true
            }

            Triple(previewText, truncated, media)
        }
    }

    Column(
        modifier = modifier.testTag("markdown_renderer_container"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Text content nodes
        displayNodes.forEach { node ->
            when (node) {
                is MarkdownNode.Heading -> MarkdownHeadingNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.Blockquote -> MarkdownBlockquoteNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    secondaryTextColor = secondaryTextColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.CodeBlock -> MarkdownCodeBlockNode(
                    node = node,
                    isMine = isMine
                )

                is MarkdownNode.BulletList -> MarkdownBulletListNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.NumberedList -> MarkdownNumberedListNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.Checklist -> MarkdownChecklistNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.ImageNode -> MarkdownImageNode(
                    node = node
                )

                is MarkdownNode.CarouselNode -> MarkdownCarouselNode(
                    node = node
                )

                is MarkdownNode.VideoNode -> MarkdownVideoNode(
                    node = node
                )

                is MarkdownNode.Paragraph -> MarkdownParagraphNode(
                    node = node,
                    isMine = isMine,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick
                )

                is MarkdownNode.HorizontalRule -> MarkdownHorizontalRuleNode(
                    isMine = isMine
                )
            }
        }

        // 2. Reddit-style "Voir plus" button when text exceeds preview length
        if (isTextTruncated) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onReadMoreClick?.invoke() }
                    .padding(vertical = 2.dp)
                    .testTag("markdown_read_more_button")
            ) {
                Text(
                    text = "Voir plus...",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 3. Reddit-style Media placement: In Feed mode, extracted media is rendered at the bottom of the card
        if (isFeedMode && mediaNodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            mediaNodes.forEach { mediaNode ->
                when (mediaNode) {
                    is MarkdownNode.ImageNode -> MarkdownImageNode(node = mediaNode)
                    is MarkdownNode.CarouselNode -> MarkdownCarouselNode(node = mediaNode)
                    is MarkdownNode.VideoNode -> MarkdownVideoNode(node = mediaNode)
                    else -> {}
                }
            }
        }

        // Extract URLs for OpenGraph cards (ignoring media already rendered)
        val urls = remember(cleanedContent) { extractUrlsFromMarkdown(cleanedContent) }
        val mediaUrls = remember(allNodes) {
            allNodes.mapNotNull {
                when (it) {
                    is MarkdownNode.ImageNode -> it.url
                    is MarkdownNode.VideoNode -> it.url
                    else -> null
                }
            }.toSet()
        }
        val hasVideo = remember(urls, mediaNodes) {
            mediaNodes.any { it is MarkdownNode.VideoNode } || urls.any { VideoUrlHelper.isVideoUrl(it) }
        }
        
        // Extract community slugs for rich WhatsApp / OpenGraph banner
        val communitySlugs = remember(cleanedContent) { 
            com.example.utils.CommunitySlugHelper.extractCommunitySlugs(cleanedContent)
        }
        val nonCommunityUrls = remember(urls, communitySlugs, mediaUrls) {
            urls.filterNot { 
                com.example.utils.CommunitySlugHelper.isCommunityUrl(it) || 
                it in mediaUrls ||
                isDirectImageUrlHelper(it) ||
                VideoUrlHelper.isVideoUrl(it)
            }
        }

        if (communitySlugs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            communitySlugs.take(2).forEach { slug ->
                com.example.ui.components.CommunityOpenGraphCard(
                    slug = slug,
                    compact = compactOpenGraph,
                    onCommunityClick = { targetSlug ->
                        if (onLinkClick != null) {
                            onLinkClick("https://iddet.app/c/$targetSlug")
                        } else {
                            communityClickHandler?.invoke(targetSlug)
                        }
                    },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        if (!hasVideo && !hideOgPhoto && nonCommunityUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            nonCommunityUrls.take(1).forEach { url ->
                OpenGraphPreview(
                    url = url,
                    compact = compactOpenGraph,
                    hideImage = hideOgPhoto,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun isDirectImageUrlHelper(url: String): Boolean {
    val clean = url.trim().lowercase()
    return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") ||
            clean.endsWith(".webp") || clean.endsWith(".gif") || clean.endsWith(".bmp") ||
            (clean.contains("cloudinary.com") && (clean.contains("/image/upload") || clean.contains("/upload/")))
}

@Composable
private fun MarkdownHeadingNode(
    node: MarkdownNode.Heading,
    isMine: Boolean,
    primaryColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val style = when (node.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp
        )
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.5.sp,
            lineHeight = 23.sp,
            letterSpacing = (-0.1).sp
        )
        3 -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            lineHeight = 21.sp
        )
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            lineHeight = 20.sp
        )
    }

    // High-contrast, natural typography color so headings are readable and consistent across themes
    val headerTextColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (node.level == 1) 4.dp else 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.level == 1) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        MarkdownRenderedText(
            annotatedString = formattedText,
            style = style,
            textColor = headerTextColor,
            onMentionClick = onMentionClick,
            onLinkClick = onLinkClick,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun MarkdownBlockquoteNode(
    node: MarkdownNode.Blockquote,
    isMine: Boolean,
    primaryColor: Color,
    secondaryTextColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val rawText = node.text
    val isDebate = remember(rawText) {
        rawText.contains("[!DEBATE]", ignoreCase = true) ||
        (rawText.contains("sujet à débattre", ignoreCase = true) && rawText.contains("google search", ignoreCase = true))
    }

    if (isDebate) {
        // Sparkling, high-end debate document card
        val cleanText = remember(rawText) {
            rawText.replace("[!DEBATE]", "").trim()
        }

        val infiniteTransition = rememberInfiniteTransition(label = "debate_shimmer")
        val shimmerTranslate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing)
            ),
            label = "shimmer_pos"
        )

        val borderGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF4285F4), // Google Blue
                Color(0xFFEA4335), // Google Red
                Color(0xFFFBBC05), // Google Yellow
                Color(0xFF34A853), // Google Green
                Color(0xFF9333EA)  // Premium Purple
            ),
            start = androidx.compose.ui.geometry.Offset(shimmerTranslate, 0f),
            end = androidx.compose.ui.geometry.Offset(shimmerTranslate + 300f, 300f)
        )

        val debateCardBg = if (isMine) {
            Color.White.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(debateCardBg)
                .border(
                    width = 1.5.dp,
                    brush = borderGradient,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            // Sparkling Top Header: Google Search First Source + Sujet à débattre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4285F4).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Recherche Google",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "source première google search",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4),
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEA4335).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Débat",
                            tint = Color(0xFFEA4335),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "sujet à débattre",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA4335)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The debate document text rendered with markdown styles
            val formattedText = rememberRichMarkdownStyles(cleanText, primaryColor, isMine)
            MarkdownRenderedText(
                annotatedString = formattedText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal
                ),
                onMentionClick = onMentionClick,
                onLinkClick = onLinkClick,
                textColor = if (isMine) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurface
            )
        }
        return
    }

    val quoteBg = if (isMine) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val barColor = if (isMine) Color.White else primaryColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(quoteBg)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .height(IntrinsicSize.Min)
                .background(barColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)
        MarkdownRenderedText(
            annotatedString = formattedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp
            ),
            onMentionClick = onMentionClick,
            onLinkClick = onLinkClick,
            textColor = secondaryTextColor
        )
    }
}

@Composable
private fun MarkdownCodeBlockNode(
    node: MarkdownNode.CodeBlock,
    isMine: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val codeBg = if (isMine) Color.Black.copy(alpha = 0.25f) else Color(0xFF1E1E1E)
    val codeTextColor = if (isMine) Color.White else Color(0xFFD4D4D4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(codeBg)
            .padding(8.dp)
            .testTag("markdown_code_block")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.language?.uppercase() ?: "CODE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = if (isMine) Color.White.copy(alpha = 0.6f) else Color.Gray
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = if (isMine) Color.White.copy(alpha = 0.7f) else Color.LightGray,
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(node.code))
                        Toast.makeText(context, "Code copié !", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("markdown_copy_code_button")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = node.code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                ),
                color = codeTextColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MarkdownBulletListNode(
    node: MarkdownNode.BulletList,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isMine) Color.White else primaryColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
                val formattedText = rememberRichMarkdownStyles(item, primaryColor, isMine)
                MarkdownRenderedText(
                    annotatedString = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownNumberedListNode(
    node: MarkdownNode.NumberedList,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = " ${index + 1}. ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isMine) Color.White else primaryColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
                val formattedText = rememberRichMarkdownStyles(item, primaryColor, isMine)
                MarkdownRenderedText(
                    annotatedString = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownChecklistNode(
    node: MarkdownNode.Checklist,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        node.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (item.isChecked) "Completed" else "Incomplete",
                    tint = if (isMine) Color.White else if (item.isChecked) primaryColor else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                )
                val formattedText = rememberRichMarkdownStyles(item.text, primaryColor, isMine)
                MarkdownRenderedText(
                    annotatedString = formattedText,
                    style = MaterialTheme.typography.bodyMedium.let {
                        if (item.isChecked) {
                            it.copy(textDecoration = TextDecoration.LineThrough)
                        } else {
                            it
                        }
                    },
                    textColor = if (item.isChecked) secondaryTextColor else textColor,
                    onMentionClick = onMentionClick,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarkdownImageNode(
    node: MarkdownNode.ImageNode
) {
    if (VideoUrlHelper.isVideoUrl(node.url)) {
        ActfileVideoPlayer(
            videoUrl = node.url,
            title = node.altText,
            lazyLoad = true,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        val context = LocalContext.current
        var showFullPreview by remember { mutableStateOf(false) }

        val imageModel = remember(node.url) {
            coil.request.ImageRequest.Builder(context)
                .data(com.example.utils.UrlHelper.fixCloudinaryUrl(node.url))
                .crossfade(true)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .clickable { showFullPreview = true }
                .testTag("markdown_image_node"),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = node.altText ?: "Markdown Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 380.dp),
                contentScale = ContentScale.Crop
            )
        }

        if (showFullPreview) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showFullPreview = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { showFullPreview = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = node.altText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownVideoNode(
    node: MarkdownNode.VideoNode
) {
    ActfileVideoPlayer(
        videoUrl = node.url,
        title = node.title,
        lazyLoad = true,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownParagraphNode(
    node: MarkdownNode.Paragraph,
    isMine: Boolean,
    primaryColor: Color,
    textColor: Color,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?
) {
    val formattedText = rememberRichMarkdownStyles(node.text, primaryColor, isMine)
    MarkdownRenderedText(
        annotatedString = formattedText,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        textColor = textColor
    )
}

@Composable
private fun MarkdownHorizontalRuleNode(
    isMine: Boolean
) {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("markdown_horizontal_rule"),
        thickness = 1.dp,
        color = if (isMine) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun MarkdownRenderedText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val communityClickHandler = LocalCommunityClickHandler.current
    val channelClickHandler = LocalChannelClickHandler.current

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = textColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    if (com.example.utils.CommunitySlugHelper.isCommunityUrl(url)) {
                        val slug = com.example.utils.CommunitySlugHelper.extractSlugFromUrl(url)
                        if (!slug.isNullOrBlank()) {
                            communityClickHandler?.invoke(slug)
                            return@ClickableText
                        }
                    } else if (url.startsWith("https://iddet.app/c/") || url.startsWith("iddet://community/")) {
                        val slug = if (url.startsWith("https://iddet.app/c/")) url.substringAfter("https://iddet.app/c/").substringBefore("/") else url.substringAfter("iddet://community/").substringBefore("/")
                        if (slug.isNotBlank()) {
                            communityClickHandler?.invoke(slug)
                            return@ClickableText
                        }
                    }
                    if (onLinkClick != null) {
                        onLinkClick(url)
                    } else {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // ignore gracefully
                        }
                    }
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "COMMUNITY", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    communityClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "CHANNEL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    channelClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }
        }
    )
}

@Composable
fun rememberRichMarkdownStyles(
    text: String,
    primaryColor: Color,
    isMine: Boolean = false
): AnnotatedString {
    val bodyFontSize = MaterialTheme.typography.bodyMedium.fontSize
    return remember(text, primaryColor, isMine, bodyFontSize) {
        buildAnnotatedString {
            var i = 0
            val linkColor = if (isMine) Color.White else primaryColor
            val codeBgColor = if (isMine) Color.White.copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.08f)
            val codeTextColor = if (isMine) Color.White else primaryColor

            while (i < text.length) {
                val isBoundary = (i == 0 || text[i - 1].isWhitespace() || "()[]{}<>,.?!:;\"'".contains(text[i - 1]))

                // 1. Mentions: @username, @c/community, @#channel
                if (text[i] == '@') {
                    val mentionRegex = Regex("^(@c/([a-zA-Z0-9_-]+)|@#([a-zA-Z0-9_-]+)|@([a-zA-Z0-9_.]+))")
                    val match = mentionRegex.find(text.substring(i))
                    if (match != null) {
                        val fullMatch = match.value

                        val mentionType = when {
                            fullMatch.startsWith("@c/") -> "COMMUNITY"
                            fullMatch.startsWith("@#") -> "CHANNEL"
                            else -> "MENTION"
                        }
                        val rawValue = when {
                            fullMatch.startsWith("@c/") -> fullMatch.substring(3)
                            fullMatch.startsWith("@#") -> fullMatch.substring(2)
                            else -> fullMatch.substring(1)
                        }

                        val mentionColor = when (mentionType) {
                            "COMMUNITY" -> Color(0xFF8B5CF6)
                            "CHANNEL" -> Color(0xFF10B981)
                            else -> linkColor
                        }

                        pushStringAnnotation(tag = mentionType, annotation = rawValue)
                        withStyle(
                            SpanStyle(
                                color = mentionColor,
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(fullMatch)
                        }
                        pop()
                        i += fullMatch.length
                        continue
                    }
                }

                // 2. Community shortcut starting with / (e.g. /c/slug or /slug)
                if (text[i] == '/' && isBoundary) {
                    val slashRegex = Regex("^(/c/([a-zA-Z0-9_-]+)|/([a-zA-Z0-9_-]{2,}))")
                    val match = slashRegex.find(text.substring(i))
                    if (match != null) {
                        val fullMatch = match.value
                        val slug = if (fullMatch.startsWith("/c/")) fullMatch.substring(3) else fullMatch.substring(1)
                        if (!fullMatch.startsWith("//") && slug.isNotEmpty() && !slug.equals("voice", ignoreCase = true)) {
                            pushStringAnnotation(tag = "COMMUNITY", annotation = slug)
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.ExtraBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(fullMatch)
                            }
                            pop()
                            i += fullMatch.length
                            continue
                        }
                    }
                }

                // 3. Community shortcut starting with c/slug (e.g. c/general, c/crypto) or communities/slug
                if (text[i] == 'c' && isBoundary) {
                    val cSlugRegex = Regex("^(c/([a-zA-Z0-9_-]+)|communities/([a-zA-Z0-9_-]+))")
                    val match = cSlugRegex.find(text.substring(i))
                    if (match != null) {
                        val fullMatch = match.value
                        val slug = if (fullMatch.startsWith("c/")) fullMatch.substring(2) else fullMatch.substring(12)
                        pushStringAnnotation(tag = "COMMUNITY", annotation = slug)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(fullMatch)
                        }
                        pop()
                        i += fullMatch.length
                        continue
                    }
                }

                // 4. Channel shortcut starting with #channel (when not heading)
                if (text[i] == '#' && isBoundary && (i + 1 < text.length && !text[i + 1].isWhitespace())) {
                    val channelRegex = Regex("^(#([a-zA-Z0-9_-]+))")
                    val match = channelRegex.find(text.substring(i))
                    if (match != null) {
                        val fullMatch = match.value
                        val channelName = fullMatch.substring(1)
                        pushStringAnnotation(tag = "CHANNEL", annotation = channelName)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(fullMatch)
                        }
                        pop()
                        i += fullMatch.length
                        continue
                    }
                }

                // Bold **
                if (i < text.length - 1 && text[i] == '*' && text[i + 1] == '*') {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                }
                // Bold __
                if (i < text.length - 1 && text[i] == '_' && text[i + 1] == '_') {
                    val end = text.indexOf("__", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                }
                // Italic *
                if (text[i] == '*') {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }
                // Italic _
                if (text[i] == '_') {
                    val end = text.indexOf("_", i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }
                // Strikethrough ~~
                if (i < text.length - 1 && text[i] == '~' && text[i + 1] == '~') {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                }
                // Inline code `
                if (text[i] == '`') {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = bodyFontSize * 0.9f
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }
                // Link [text](url)
                if (text[i] == '[') {
                    val closingBracket = text.indexOf(']', i + 1)
                    if (closingBracket != -1) {
                        if (closingBracket < text.length - 1 && text[closingBracket + 1] == '(') {
                            val closingParen = text.indexOf(')', closingBracket + 2)
                            if (closingParen != -1) {
                                val linkText = text.substring(i + 1, closingBracket)
                                val url = text.substring(closingBracket + 2, closingParen)

                                pushStringAnnotation(tag = "URL", annotation = url)
                                withStyle(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(linkText)
                                }
                                pop()
                                i = closingParen + 1
                                continue
                            }
                        }
                    }
                }

                // Raw HTTP URL check
                val rawUrlMatch = com.example.ui.components.parseRawUrlAt(text, i)
                if (rawUrlMatch != null) {
                    pushStringAnnotation(tag = "URL", annotation = rawUrlMatch)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(rawUrlMatch)
                    }
                    pop()
                    i += rawUrlMatch.length
                    continue
                }

                append(text[i].toString())
                i++
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MarkdownCarouselNode(
    node: MarkdownNode.CarouselNode
) {
    if (node.images.isEmpty()) return

    if (node.images.size == 1) {
        MarkdownImageNode(node = node.images.first())
        return
    }

    val pagerState = rememberPagerState(
        pageCount = { node.images.size }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .testTag("markdown_carousel_node")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val context = LocalContext.current
            val imageNode = node.images[page]
            val carouselImageModel = remember(imageNode.url) {
                coil.request.ImageRequest.Builder(context)
                    .data(com.example.utils.UrlHelper.fixCloudinaryUrl(imageNode.url))
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = carouselImageModel,
                    contentDescription = imageNode.altText ?: "Carousel Image ${page + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markdown_carousel_image_$page"),
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        // Image counter badge e.g. "1/3"
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${node.images.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Bottom Page Indicator Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(node.images.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                        )
                )
            }
        }
    }
}
