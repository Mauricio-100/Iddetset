package com.example.ui.components

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.utils.UrlHelper

@Composable
fun ActfileVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    autoPlay: Boolean = false,
    lazyLoad: Boolean = true
) {
    val context = LocalContext.current
    val videoInfo = remember(videoUrl) { VideoUrlHelper.parseVideoInfo(videoUrl) }
    var isActivated by remember(videoUrl, autoPlay, lazyLoad) {
        mutableStateOf(autoPlay || !lazyLoad)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag("actfile_video_player"),
        color = Color.Black,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!isActivated) {
                // High-performance Lazy Load Video Poster (minimizes RAM, CPU & data usage while scrolling)
                VideoLazyPoster(
                    videoInfo = videoInfo,
                    title = title,
                    onPlayClick = { isActivated = true }
                )
            } else {
                when (videoInfo.type) {
                    VideoUrlHelper.VideoType.DIRECT_FILE -> {
                        DirectVideoView(videoUrl = videoInfo.originalUrl, autoPlay = true)
                    }
                    else -> {
                        EmbeddedWebVideoView(videoInfo = videoInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoLazyPoster(
    videoInfo: VideoUrlHelper.VideoInfo,
    title: String?,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbnailUrl = remember(videoInfo) { VideoUrlHelper.getThumbnailUrl(videoInfo) }
    val providerName = remember(videoInfo) { VideoUrlHelper.getProviderName(videoInfo.type) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 280.dp)
            .background(Color(0xFF0F172A))
            .clickable { onPlayClick() }
            .testTag("video_lazy_poster"),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(UrlHelper.fixCloudinaryUrl(thumbnailUrl))
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = title ?: "Aperçu vidéo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark gradient overlay for contrast and controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        } else {
            // Elegant placeholder pattern for direct files without thumbnail
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                Color(0xFF0F172A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        // Top provider badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = providerName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Play Button with Frosted Glow
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Lire la vidéo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom title if available
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DirectVideoView(
    videoUrl: String,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(false) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isError by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    // Clean up VideoView & MediaPlayer on exit
    DisposableEffect(videoUrl) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
                mediaPlayerInstance?.release()
                mediaPlayerInstance = null
                videoViewInstance = null
            } catch (e: Exception) {
                // ignore cleanup errors
            }
        }
    }

    // Periodic progress ticker
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            videoViewInstance?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition
                    durationMs = vv.duration.coerceAtLeast(1)
                }
            }
            kotlinx.coroutines.delay(250)
        }
    }

    // Auto-hide controls after 3.5 seconds if playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !isSeeking) {
            kotlinx.coroutines.delay(3500)
            showControls = false
        }
    }

    fun formatTime(millis: Int): String {
        val totalSec = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewInstance = this
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(Uri.parse(videoUrl))

                    setOnPreparedListener { mp ->
                        mediaPlayerInstance = mp
                        mp.isLooping = true
                        mp.setVolume(if (isMuted) 0f else 1.0f, if (isMuted) 0f else 1.0f)
                        if (autoPlay) {
                            start()
                            isPlaying = true
                        }
                        durationMs = duration.coerceAtLeast(1)
                    }

                    setOnErrorListener { _, _, _ ->
                        isError = true
                        true
                    }
                }
            },
            update = { view ->
                videoViewInstance = view
            }
        )

        // Minimal Controls Overlay
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(androidx.compose.animation.core.tween(200)),
            exit = fadeOut(androidx.compose.animation.core.tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Top controls (Mute / Unmute)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            mediaPlayerInstance?.let { mp ->
                                if (isMuted) {
                                    mp.setVolume(0f, 0f)
                                } else {
                                    mp.setVolume(1.0f, 1.0f)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("video_mute_btn")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                            contentDescription = if (isMuted) "Activer le son" else "Couper le son",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center Play / Pause Button
                IconButton(
                    onClick = {
                        videoViewInstance?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("video_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Lire",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bottom Timeline Slider & Timestamps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val currentFraction = if (durationMs > 0) {
                        if (isSeeking) sliderPosition else (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(if (isSeeking) (sliderPosition * durationMs).toInt() else currentPositionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = currentFraction,
                        onValueChange = { newPos ->
                            isSeeking = true
                            sliderPosition = newPos
                        },
                        onValueChangeFinished = {
                            val seekToMs = (sliderPosition * durationMs).toInt()
                            videoViewInstance?.seekTo(seekToMs)
                            currentPositionMs = seekToMs
                            isSeeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("video_timeline_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        if (isError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color.Yellow,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Impossible de lire le flux vidéo.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedWebVideoView(videoInfo: VideoUrlHelper.VideoInfo) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(videoInfo) {
        onDispose {
            try {
                webViewInstance?.stopLoading()
                webViewInstance?.loadUrl("about:blank")
                (webViewInstance?.parent as? ViewGroup)?.removeView(webViewInstance)
                webViewInstance?.destroy()
                webViewInstance = null
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val htmlContent = remember(videoInfo) {
        when (videoInfo.type) {
            VideoUrlHelper.VideoType.YOUTUBE -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                  * { margin:0; padding:0; box-sizing:border-box; }
                  body, html { width:100%; height:100%; background-color:#000000; overflow:hidden; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            VideoUrlHelper.VideoType.TIKTOK -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allowfullscreen allow="autoplay; encrypted-media"></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            VideoUrlHelper.VideoType.INSTAGRAM -> {
                val embed = videoInfo.embedUrl ?: videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <iframe src="$embed" allowfullscreen allow="autoplay; encrypted-media"></iframe>
                </body>
                </html>
                """.trimIndent()
            }
            else -> {
                val targetUrl = videoInfo.originalUrl
                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                  * { margin:0; padding:0; }
                  body, html { width:100%; height:100%; background:#000; display:flex; justify-content:center; align-items:center; }
                  video { width:100%; max-height:100%; object-fit:contain; }
                  iframe { width:100%; height:100%; border:none; }
                </style>
                </head>
                <body>
                  <video src="$targetUrl" controls playsinline autoplay style="width:100%; height:100%;"></video>
                </body>
                </html>
                """.trimIndent()
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewInstance = this
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webChromeClient = WebChromeClient()
                webViewClient = VideoWebViewClient()
                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webViewInstance = webView
            val currentTag = webView.tag as? String
            if (currentTag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        }
    )
}

class VideoWebViewClient : WebViewClient() {
    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        try {
            (view?.parent as? ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}
