package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBrowserScreen(
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var hasRenderCrashed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView?.stopLoading()
                webView?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
            webView = null
        }
    }

    val terminalBg = Color(0xFF0D0D0D)
    val terminalAccent = Color(0xFF00E5FF)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(terminalBg)
                .statusBarsPadding()
        ) {
            // TUI Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IDDET_CORE // BROWSER_v2",
                    color = terminalAccent,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "[ OUVRIR DANS LE NAVIGATEUR ]",
                    color = terminalAccent.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { url }))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                        .padding(end = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "[ FERMER ]",
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { onBack() },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }

            // Browser Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MeowControlBtn(label = "[<] RETOUR", enabled = canGoBack) { webView?.goBack() }
                MeowControlBtn(label = "AVANT [>]", enabled = canGoForward) { webView?.goForward() }
                MeowControlBtn(label = "[↺] RECHARGER", enabled = true) {
                    hasRenderCrashed = false
                    reloadKey++
                    webView?.reload()
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black)
                        .border(1.dp, terminalAccent.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "> ",
                            color = terminalAccent,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = currentUrl,
                            color = terminalAccent.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(1.dp),
                            color = terminalAccent,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // WebView Container with TUI Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color.White)
                    .border(1.dp, terminalAccent.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                if (hasRenderCrashed) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Rendu Web Interrompu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Le moteur WebView a été libéré par le système.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    hasRenderCrashed = false
                                    reloadKey++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = terminalAccent)
                            ) {
                                Text("Recharger", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { url }))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            ) {
                                Text("Ouvrir dans le navigateur", color = Color.White)
                            }
                        }
                    }
                } else {
                    key(reloadKey) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                    webViewClient = BrowserWebViewClient(
                                        onPageStartedAction = { isLoading = true },
                                        onPageFinishedAction = { view, u ->
                                            isLoading = false
                                            currentUrl = u ?: ""
                                            canGoBack = view?.canGoBack() ?: false
                                            canGoForward = view?.canGoForward() ?: false
                                        },
                                        onRenderCrashedAction = {
                                            webView = null
                                            hasRenderCrashed = true
                                        }
                                    )
                                    webChromeClient = BrowserWebChromeClient { p ->
                                        progress = p
                                    }
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    loadUrl(url)
                                    webView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // CRT Scanline Effect (Overlay)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanlineSpacing = 4.dp.toPx()
            val scanlineAlpha = 0.04f
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = scanlineAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += scanlineSpacing
            }
        }
    }
}

@Composable
private fun MeowControlBtn(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .border(
                1.dp, 
                if (enabled) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.DarkGray.copy(alpha = 0.2f),
                RoundedCornerShape(2.dp)
            )
            .background(if (enabled) Color.White.copy(alpha = 0.03f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color(0xFF00E5FF) else Color.DarkGray,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        )
    }
}

class BrowserWebViewClient(
    private val onPageStartedAction: () -> Unit,
    private val onPageFinishedAction: (WebView?, String?) -> Unit,
    private val onRenderCrashedAction: () -> Unit
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStartedAction()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedAction(view, url)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        try {
            (view?.parent as? android.view.ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        onRenderCrashedAction()
        return true
    }
}

class BrowserWebChromeClient(
    private val onProgressChangedAction: (Float) -> Unit
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChangedAction(newProgress / 100f)
    }
}
