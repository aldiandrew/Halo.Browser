package com.aldiandrew.halobrowser

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowserScreen(
                        initialUrl = extractUrl(intent) ?: DEFAULT_URL,
                        onOpenBubble = ::showNativeBubble
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractUrl(sourceIntent: Intent?): String? {
        val value = sourceIntent?.getStringExtra(EXTRA_URL)
            ?: sourceIntent?.data?.toString()

        return value?.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bubble_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Halo Browser native Android bubbles"
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }

        manager.createNotificationChannel(channel)
    }

    private fun showNativeBubble(url: String) {
        BubbleActivity.createBubble(this, url)
    }

    companion object {
        private const val CHANNEL_ID = "halo_browser_bubbles_v3"
        private const val DEFAULT_URL = "https://www.google.com"

        const val EXTRA_URL = "halo_browser_url"
        const val EXTRA_BUBBLE_ID = "halo_browser_bubble_id"

        fun createNotificationChannel(context: android.content.Context) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.bubble_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Halo Browser native Android bubbles"
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            manager.createNotificationChannel(channel)
        }
    }
}

@Composable
private fun BrowserScreen(
    initialUrl: String,
    onOpenBubble: (String) -> Unit
) {
    var address by remember(initialUrl) {
        mutableStateOf(initialUrl)
    }

    var currentUrl by remember(initialUrl) {
        mutableStateOf(initialUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Address") }
            )

            Button(
                onClick = {
                    currentUrl = normalizeUrl(address)
                    address = currentUrl
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Go")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onOpenBubble(currentUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Minimize to Android Bubble")
        }

        Spacer(modifier = Modifier.height(8.dp))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
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
                            request: WebResourceRequest
                        ): Boolean {
                            return false
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {
                            super.onPageFinished(view, url)
                        }
                    }

                    loadUrl(currentUrl)
                }
            },
            update = { webView ->
                if (webView.url != currentUrl) {
                    webView.loadUrl(currentUrl)
                }
            }
        )
    }
}

private fun normalizeUrl(value: String): String {
    val input = value.trim()

    if (input.isEmpty()) {
        return "https://www.google.com"
    }

    if (
        input.startsWith("http://") ||
        input.startsWith("https://")
    ) {
        return input
    }

    if (input.contains(" ") || !input.contains(".")) {
        return "https://www.google.com/search?q=" + Uri.encode(input)
    }

    return "https://$input"
}
