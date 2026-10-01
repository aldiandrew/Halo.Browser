package com.aldiandrew.halobrowser

import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class BubbleActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
        }

        setContentView(webView)

        val url = intent.getStringExtra(MainActivity.EXTRA_URL)
            ?: "https://www.google.com"

        webView.loadUrl(url)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
