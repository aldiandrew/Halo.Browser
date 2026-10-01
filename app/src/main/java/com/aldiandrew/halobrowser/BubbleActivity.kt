package com.aldiandrew.halobrowser

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class BubbleActivity : Activity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val browserView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String
                ): Boolean {
                    return false
                }
            }
        }

        webView = browserView
        setContentView(browserView)

        val url = intent.getStringExtra(MainActivity.EXTRA_URL)
            ?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }
            ?: "https://www.google.com"

        browserView.loadUrl(url)
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}
