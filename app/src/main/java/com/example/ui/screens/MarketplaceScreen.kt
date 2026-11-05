package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketplace", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = MarketplaceWebViewClient()
                        webChromeClient = WebChromeClient()
                        
                        val htmlData = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body {
                                        margin: 0;
                                        padding: 16px;
                                        font-family: sans-serif;
                                        background-color: transparent;
                                        display: flex;
                                        flex-direction: column;
                                        align-items: center;
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="chariow-widget" data-product-id="prd_ki4kwxw0"
                                    data-store-domain="xnycggrc.mychariow.market"
                                    data-style="tap"
                                    data-border-style="rounded"
                                    data-cta-width="xs"
                                    data-cta-animation="shake_scale"
                                    data-locale="en"
                                    data-primary-color="#D35400"
                                    data-background-color="#FF6B6B"
                                    data-custom-cta-text="Profiter de l'offre"></div>
                                <script>
                                (function() {
                                  var script = document.createElement('script');
                                  script.src = 'https://js.chariowcdn.com/v1/widget.min.js';
                                  script.async = true;
                                  document.head.appendChild(script);
                                
                                  var link = document.createElement('link');
                                  link.rel = 'stylesheet';
                                  link.href = 'https://js.chariowcdn.com/v1/widget.min.css';
                                  document.head.appendChild(link);
                                })();
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        loadDataWithBaseURL("https://xnycggrc.mychariow.market", htmlData, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

class MarketplaceWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
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
