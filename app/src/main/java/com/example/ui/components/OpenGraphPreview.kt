package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class UrlMetadata(
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?,
    val faviconUrl: String?,
    val isLoaded: Boolean = false
)

object UrlMetadataResolver {
    private val cache = ConcurrentHashMap<String, UrlMetadata>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun resolve(url: String): UrlMetadata {
        val cleanUrl = sanitizeUrl(url)
        
        // Return cached metadata if present
        cache[cleanUrl]?.let { return it }
        
        // Get preset/fallback metadata
        val preset = getPresetMetadata(cleanUrl)
        
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(cleanUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val tags = parseOgMeta(body)
                        
                        val title = tags["title"] ?: tags["title_tag"] ?: preset.title
                        val desc = tags["description"] ?: tags["desc_tag"] ?: preset.description
                        var image = tags["image"] ?: preset.imageUrl
                        
                        // Resolve relative URLs for image
                        if (image != null && image.startsWith("/")) {
                            val uri = URI(cleanUrl)
                            image = "${uri.scheme}://${uri.host}$image"
                        }
                        
                        var favicon = tags["favicon_tag"] ?: preset.faviconUrl
                        if (favicon != null && favicon.startsWith("/")) {
                            val uri = URI(cleanUrl)
                            favicon = "${uri.scheme}://${uri.host}$favicon"
                        } else if (favicon == null) {
                            val uri = URI(cleanUrl)
                            favicon = "${uri.scheme}://${uri.host}/favicon.ico"
                        }
                        
                        val siteName = tags["site_name"] ?: URI(cleanUrl).host ?: preset.siteName
                        
                        val resolved = UrlMetadata(
                            url = cleanUrl,
                            title = title,
                            description = desc,
                            imageUrl = image,
                            siteName = siteName,
                            faviconUrl = favicon,
                            isLoaded = true
                        )
                        cache[cleanUrl] = resolved
                        resolved
                    } else {
                        cache[cleanUrl] = preset
                        preset
                    }
                }
            } catch (e: Exception) {
                cache[cleanUrl] = preset
                preset
            }
        }
    }
    
    private fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean
    }
    
    private fun parseOgMeta(html: String): Map<String, String> {
        val metaTags = mutableMapOf<String, String>()
        
        // Match both single/double quotes, and standard property/content order or reversed
        val ogRegex = Regex("<meta\\s+[^>]*property=\"og:([^\"]+)\"\\s+[^>]*content=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
        val ogRegexAlt = Regex("<meta\\s+[^>]*content=\"([^\"]+)\"\\s+[^>]*property=\"og:([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
        
        val titleRegex = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)
        val descRegex = Regex("<meta\\s+[^>]*name=\"description\"\\s+[^>]*content=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
        val faviconRegex = Regex("<link\\s+[^>]*rel=\"(?:shortcut icon|icon)\"\\s+[^>]*href=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)

        ogRegex.findAll(html).forEach { match ->
            metaTags[match.groupValues[1].lowercase()] = match.groupValues[2]
        }
        ogRegexAlt.findAll(html).forEach { match ->
            metaTags[match.groupValues[2].lowercase()] = match.groupValues[1]
        }
        
        titleRegex.find(html)?.let { match ->
            metaTags["title_tag"] = match.groupValues[1].trim()
        }
        descRegex.find(html)?.let { match ->
            metaTags["desc_tag"] = match.groupValues[1].trim()
        }
        faviconRegex.find(html)?.let { match ->
            metaTags["favicon_tag"] = match.groupValues[1].trim()
        }
        
        return metaTags
    }
    
    fun getPresetMetadata(url: String): UrlMetadata {
        val cleanUrl = sanitizeUrl(url)
        val host = try {
            URI(cleanUrl).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
        
        return when {
            host.contains("github.com") -> UrlMetadata(
                url = cleanUrl,
                title = "GitHub · Build and collaborate on software",
                description = "GitHub is the home for developers and teams to store, manage, and collaborate on code.",
                imageUrl = "https://opengraph.githubassets.com/1/github/docs",
                siteName = "GitHub",
                faviconUrl = "https://github.githubassets.com/favicons/favicon.svg"
            )
            host.contains("youtube.com") || host.contains("youtu.be") -> UrlMetadata(
                url = cleanUrl,
                title = "YouTube",
                description = "Partagez vos vidéos avec vos amis, votre famille et le monde entier.",
                imageUrl = "https://www.youtube.com/img/desktop/yt_1200.png",
                siteName = "YouTube",
                faviconUrl = "https://www.youtube.com/s/desktop/4f5b24df/img/favicon_144x144.png"
            )
            host.contains("google.com") -> UrlMetadata(
                url = cleanUrl,
                title = "Google",
                description = "Recherchez des informations sur le Web, des images, des vidéos et bien plus encore.",
                imageUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png",
                siteName = "Google",
                faviconUrl = "https://www.google.com/favicon.ico"
            )
            host.contains("twitter.com") || host.contains("x.com") -> UrlMetadata(
                url = cleanUrl,
                title = "X (formerly Twitter)",
                description = "X is the premium platform for real-time community engagement, messages, and instant feeds.",
                imageUrl = "https://abs.twimg.com/errors/logo46x38.png",
                siteName = "X",
                faviconUrl = "https://abs.twimg.com/favicons/twitter.ico"
            )
            host.contains("wikipedia.org") -> UrlMetadata(
                url = cleanUrl,
                title = "Wikipedia, l'encyclopédie libre",
                description = "Wikipédia est un projet d’encyclopédie collective en ligne, gratuite, neutre et ouverte à tous.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/Wikipedia-logo-v2.svg/1200px-Wikipedia-logo-v2.svg.png",
                siteName = "Wikipedia",
                faviconUrl = "https://en.wikipedia.org/static/favicon/wikipedia.ico"
            )
            else -> {
                val siteName = host.removePrefix("www.").substringBefore(".")
                UrlMetadata(
                    url = cleanUrl,
                    title = siteName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " - Site Officiel",
                    description = "Découvrez le lien partagé de $siteName. Cliquez pour ouvrir la page web correspondante.",
                    imageUrl = null,
                    siteName = host.ifEmpty { "Lien externe" },
                    faviconUrl = null
                )
            }
        }
    }
}

@Composable
fun OpenGraphPreview(
    url: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    hideImage: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null
) {
    val communitySlug = remember(url) { com.example.utils.CommunitySlugHelper.extractSlugFromUrl(url) }
    if (communitySlug != null) {
        CommunityOpenGraphCard(
            slug = communitySlug,
            modifier = modifier,
            compact = compact,
            onCommunityClick = { targetSlug ->
                if (onLinkClick != null) {
                    onLinkClick("https://iddet.app/c/$targetSlug")
                }
            }
        )
        return
    }

    val uriHandler = LocalUriHandler.current
    var metadata by remember(url) { mutableStateOf(UrlMetadataResolver.getPresetMetadata(url)) }

    LaunchedEffect(url) {
        val resolved = UrlMetadataResolver.resolve(url)
        metadata = resolved
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable {
                if (onLinkClick != null) {
                    onLinkClick(metadata.url)
                } else {
                    try {
                        uriHandler.openUri(metadata.url)
                    } catch (e: Exception) {
                        // fallback
                    }
                }
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = CardDefaults.outlinedCardBorder()
    ) {
        if (compact) {
            // Compact Style: row layout for Chats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hideImage) {
                    // Left Image (Thumbnail) - If null/blank, use a cute cat image!
                    val thumbnailModel = if (!metadata.imageUrl.isNullOrBlank()) {
                        metadata.imageUrl
                    } else {
                        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=100&q=80"
                    }
                    
                    AsyncImage(
                        model = thumbnailModel,
                        contentDescription = "Miniature",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Metadata Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (metadata.faviconUrl != null) {
                            AsyncImage(
                                model = metadata.faviconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Text(
                            text = metadata.siteName ?: "Lien",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metadata.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!metadata.description.isNullOrEmpty()) {
                        Text(
                            text = metadata.description!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source : ${metadata.url}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Light),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Full Banner Style: beautiful design for feed/actfiles
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!hideImage) {
                    // Top Header Image - If null/blank, use a cute cat image!
                    val bannerModel = if (!metadata.imageUrl.isNullOrBlank()) {
                        metadata.imageUrl
                    } else {
                        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600&q=80"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = "Preview Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Decorative overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Text details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (metadata.faviconUrl != null) {
                            AsyncImage(
                                model = metadata.faviconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Text(
                            text = metadata.siteName?.uppercase() ?: "LIEN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = metadata.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!metadata.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metadata.description!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Données récupérées du site : ${metadata.url}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
