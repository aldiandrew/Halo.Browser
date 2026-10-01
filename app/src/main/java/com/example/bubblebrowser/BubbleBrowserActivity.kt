package com.example.bubblebrowser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class BubbleBrowserActivity : Activity() {

    companion object {
        private const val DEFAULT_URL = "https://google.com"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_bubble)

            webView = findViewById(R.id.bubbleWebView)
            configureWebView()

            if (savedInstanceState == null) {
                val requestedUrl = intent?.data?.toString()

                if (!requestedUrl.isNullOrBlank() &&
                    isHttpUrl(Uri.parse(requestedUrl))
                ) {
                    webView.loadUrl(requestedUrl)
                } else {
                    webView.loadUrl(DEFAULT_URL)
                }
            } else {
                webView.restoreState(savedInstanceState)
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "WebView gagal dijalankan: ${e.message ?: "error tidak diketahui"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url

                return if (isHttpUrl(uri)) {
                    false
                } else {
                    openExternalUrl(uri)
                    true
                }
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                val uri = Uri.parse(url)

                return if (isHttpUrl(uri)) {
                    false
                } else {
                    openExternalUrl(uri)
                    true
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)

                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@BubbleBrowserActivity,
                        "Gagal memuat halaman.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isHttpUrl(uri: Uri): Boolean {
        return uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)
    }

    private fun openExternalUrl(uri: Uri) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Tidak ada aplikasi yang dapat membuka tautan ini.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Gagal membuka tautan: ${e.message ?: "error tidak diketahui"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        try {
            webView.saveState(outState)
        } catch (_: Exception) {
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        try {
            webView.apply {
                stopLoading()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}