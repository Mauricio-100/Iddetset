package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChariowWidgetConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChariowWidgetDialog(
    widgetConfig: ChariowWidgetConfig,
    reference: String?,
    amount: Double?,
    currency: String,
    onDismiss: () -> Unit,
    onVerifyStatus: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    val context = LocalContext.current

    val directUrl = remember(widgetConfig) {
        val domain = widgetConfig.store_domain.ifBlank { "xnycggrc.mychariow.market" }
        val prodId = widgetConfig.product_id.ifBlank { "prd_zs6iyq84" }
        "https://$domain/$prodId"
    }

    val amountString = remember(amount, currency) {
        if (amount != null) {
            if (currency.equals("USD", ignoreCase = true)) "$amount USD" else "${amount.toInt()} CDF"
        } else {
            if (currency.equals("USD", ignoreCase = true)) "1.25 USD" else "2800 CDF"
        }
    }

    val htmlContent = remember(widgetConfig, reference, amountString) {
        val productId = widgetConfig.product_id.ifBlank { "prd_zs6iyq84" }
        val storeDomain = widgetConfig.store_domain.ifBlank { "xnycggrc.mychariow.market" }
        val email = widgetConfig.customer_email ?: ""
        val ref = reference ?: "IDPLUS-EN-COURS"

        """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <link rel="stylesheet" href="https://js.chariow.com/v1/widget.min.css">
          <style>
            * { box-sizing: border-box; }
            body {
              margin: 0;
              padding: 16px;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              background: #F8FAFC;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            }
            .card {
              width: 100%;
              max-width: 400px;
              background: #FFFFFF;
              border-radius: 16px;
              padding: 20px;
              box-shadow: 0 4px 16px rgba(0,0,0,0.06);
              text-align: center;
            }
            .header-badge {
              display: inline-block;
              padding: 4px 10px;
              border-radius: 9999px;
              background: #FEF3C7;
              color: #92400E;
              font-size: 11px;
              font-weight: 700;
              text-transform: uppercase;
              letter-spacing: 0.5px;
              margin-bottom: 10px;
            }
            h2 {
              margin: 0 0 6px 0;
              color: #0F172A;
              font-size: 18px;
              font-weight: 800;
            }
            p.desc {
              margin: 0 0 16px 0;
              color: #64748B;
              font-size: 12px;
              line-height: 1.4;
            }
            .price-box {
              background: #FFFBEB;
              border: 1.5px dashed #F59E0B;
              border-radius: 12px;
              padding: 12px;
              margin-bottom: 20px;
            }
            .price-amount {
              font-size: 24px;
              font-weight: 900;
              color: #D97706;
            }
            .price-info {
              font-size: 11px;
              color: #B45309;
              font-weight: 600;
              margin-top: 2px;
            }
            #widget-holder {
              margin: 12px 0 20px 0;
              display: flex;
              justify-content: center;
              align-items: center;
              min-height: 48px;
            }
            .footnote {
              font-size: 11px;
              color: #94A3B8;
              margin-top: 14px;
              line-height: 1.4;
            }
            .footnote a {
              color: #D97706;
              font-weight: 600;
              text-decoration: none;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="header-badge">Chariow Checkout</div>
            <h2>Abonnement Iddet Plus</h2>
            <p class="desc">Cliquez sur le bouton ci-dessous pour ouvrir la fenêtre de paiement sécurisé (Mobile Money / Carte).</p>

            <div class="price-box">
              <div class="price-amount">$amountString</div>
              <div class="price-info">30 jours d'accès VIP • Réf: $ref</div>
            </div>

            <div id="widget-holder">
              <div id="chariow-widget"
                   data-product-id="$productId"
                   data-store-domain="$storeDomain"
                   data-customer-email="$email"
                   data-style="tap"
                   data-border-style="rounded"
                   data-cta-width="full"
                   data-background-color="#FFFFFF"
                   data-cta-animation="shine"
                   data-locale="fr"
                   data-primary-color="#ffcc00">
              </div>
            </div>

            <div class="footnote">
              Paiement 100% sécurisé géré par Chariow.<br>
              <a href="$directUrl" target="_blank">Ouvrir dans le navigateur externe ↗</a>
            </div>
          </div>

          <script src="https://js.chariow.com/v1/widget.min.js"></script>
        </body>
        </html>
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Paiement Chariow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (reference != null) {
                                Text(
                                    text = "Réf : $reference",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // WebView containing the official Chariow widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(true)

                                webChromeClient = ChariowWebChromeClient(context)
                                webViewClient = ChariowWebViewClient(context)

                                loadDataWithBaseURL(
                                    "https://${widgetConfig.store_domain.ifBlank { "xnycggrc.mychariow.market" }}",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                HorizontalDivider()

                // Action controls at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onOpenExternal(directUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigateur", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onVerifyStatus,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("J'ai payé / Vérifier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

class ChariowWebChromeClient(private val context: Context) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val newWebView = WebView(view?.context ?: return false)
        newWebView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        newWebView.settings.javaScriptEnabled = true
        newWebView.settings.domStorageEnabled = true
        newWebView.webChromeClient = this
        newWebView.webViewClient = ChariowPopupWebViewClient(context)
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()
        return true
    }
}

class ChariowPopupWebViewClient(private val context: Context) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("http://") || url.startsWith("https://")) {
            view?.loadUrl(url)
            return true
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        try {
            (view?.parent as? android.view.ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}

class ChariowWebViewClient(private val context: Context) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        try {
            (view?.parent as? android.view.ViewGroup)?.removeView(view)
            view?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}
