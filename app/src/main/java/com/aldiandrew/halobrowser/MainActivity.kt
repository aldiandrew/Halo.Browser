package com.aldiandrew.halobrowser

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat.BubbleMetadata

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        createBrowserShortcut()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowserScreen(
                        initialUrl = extractUrl(intent) ?: "https://www.google.com",
                        onOpenBubble = { showNativeBubble() }
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
        return value?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bubble_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Halo Browser native Android bubble"
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    private fun createBrowserShortcut() {
        if (Build.VERSION.SDK_INT < 25) return

        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_URL, "https://www.google.com")
        }

        val shortcut = ShortcutInfo.Builder(this, SHORTCUT_ID)
            .setShortLabel(getString(R.string.shortcut_label))
            .setLongLabel(getString(R.string.shortcut_label))
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_halo_browser))
            .setIntent(intent)
            .setLongLived(true)
            .build()

        shortcutManager.removeDynamicShortcuts(listOf(SHORTCUT_ID))
        shortcutManager.addDynamicShortcuts(listOf(shortcut))
    }

    private fun showNativeBubble() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_URL, "https://www.google.com")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bubbleMetadata = BubbleMetadata.Builder(
            pendingIntent,
            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_halo_browser)
        )
            .setDesiredHeight(640)
            .setAutoExpandBubble(false)
            .setSuppressNotification(false)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_halo_browser)
            .setContentTitle(getString(R.string.bubble_title))
            .setContentText(getString(R.string.bubble_text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setBubbleMetadata(bubbleMetadata)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(BUBBLE_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "halo_browser_bubbles"
        private const val BUBBLE_NOTIFICATION_ID = 2001
        private const val SHORTCUT_ID = "halo_browser"
        const val EXTRA_URL = "halo_browser_url"
    }
}

@androidx.compose.runtime.Composable
private fun BrowserScreen(
    initialUrl: String,
    onOpenBubble: () -> Unit
) {
    var address by remember(initialUrl) { mutableStateOf(initialUrl) }
    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }

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
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Go")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onOpenBubble,
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
    if (input.isEmpty()) return "https://www.google.com"
    if (input.startsWith("http://") || input.startsWith("https://")) return input
    if (input.contains(" ") || !input.contains(".")) {
        return "https://www.google.com/search?q=" + Uri.encode(input)
    }
    return "https://$input"
}
