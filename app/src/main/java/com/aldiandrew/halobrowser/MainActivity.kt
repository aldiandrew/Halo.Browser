package com.aldiandrew.halobrowser

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        createBrowserShortcut()

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
            description = "Halo Browser native Android bubble"
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }

        manager.createNotificationChannel(channel)
    }

    private fun createBrowserShortcut() {
        val intent = Intent(this, BubbleActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_URL, DEFAULT_URL)
        }

        val shortcut = ShortcutInfoCompat.Builder(this, SHORTCUT_ID)
            .setShortLabel(getString(R.string.shortcut_label))
            .setLongLabel(getString(R.string.shortcut_label))
            .setIcon(
                IconCompat.createWithResource(
                    this,
                    R.drawable.ic_halo_browser
                )
            )
            .setLongLived(true)
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }

    private fun showNativeBubble(url: String) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return
        }

        val bubbleIntent = Intent(this, BubbleActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_URL, url)
        }

        val bubblePendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val bubblePendingIntent = PendingIntent.getActivity(
            this,
            BUBBLE_REQUEST_CODE,
            bubbleIntent,
            bubblePendingIntentFlags
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_URL, url)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            CONTENT_REQUEST_CODE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val person = Person.Builder()
            .setName(getString(R.string.app_name))
            .setKey(SHORTCUT_ID)
            .setImportant(true)
            .build()

        val bubbleIcon = IconCompat.createWithResource(
            this,
            R.drawable.ic_halo_browser
        )

        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            bubbleIcon
        )
            .setDesiredHeight(640)
            .setAutoExpandBubble(true)
            .setSuppressNotification(false)
            .build()

        val style = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(getString(R.string.app_name))
            .addMessage(
                "Tap to continue browsing",
                System.currentTimeMillis(),
                person
            )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_halo_browser)
            .setContentTitle(getString(R.string.bubble_title))
            .setContentText(getString(R.string.bubble_text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setShortcutId(SHORTCUT_ID)
            .setBubbleMetadata(bubbleMetadata)
            .setStyle(style)
            .addPerson(person)
            .build()

        NotificationManagerCompat.from(this).notify(
            BUBBLE_NOTIFICATION_ID,
            notification
        )
    }

    companion object {
        private const val CHANNEL_ID = "halo_browser_bubbles_v2"
        private const val BUBBLE_NOTIFICATION_ID = 2002
        private const val BUBBLE_REQUEST_CODE = 100
        private const val CONTENT_REQUEST_CODE = 101
        private const val SHORTCUT_ID = "halo_browser"
        private const val DEFAULT_URL = "https://www.google.com"

        const val EXTRA_URL = "halo_browser_url"
    }
}

private data class BrowserTab(
    val id: Int,
    val url: String
)

@Composable
private fun BrowserScreen(
    initialUrl: String,
    onOpenBubble: (String) -> Unit
) {
    var nextTabId by remember { mutableStateOf(2) }

    var tabs by remember(initialUrl) {
        mutableStateOf(
            listOf(
                BrowserTab(1, initialUrl)
            )
        )
    }

    var selectedTabId by remember(initialUrl) {
        mutableStateOf(1)
    }

    var address by remember(initialUrl) {
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
                    val url = normalizeUrl(address)
                    tabs = tabs.map { tab ->
                        if (tab.id == selectedTabId) {
                            tab.copy(url = url)
                        } else {
                            tab
                        }
                    }
                    address = url
                }
            ) {
                Text("Go")
            }

            OutlinedButton(
                onClick = {
                    val newTab = BrowserTab(
                        id = nextTabId,
                        url = DEFAULT_URL
                    )
                    nextTabId += 1
                    tabs = tabs + newTab
                    selectedTabId = newTab.id
                    address = newTab.url
                }
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.padding(top = 4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                OutlinedButton(
                    onClick = {
                        selectedTabId = tab.id
                        address = tab.url
                    }
                ) {
                    Text("Tab ${tab.id}")
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 4.dp))

        Button(
            onClick = {
                val selected = tabs.firstOrNull { it.id == selectedTabId }
                onOpenBubble(selected?.url ?: initialUrl)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Minimize to Android Bubble")
        }

        Spacer(modifier = Modifier.padding(top = 4.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            tabs.forEach { tab ->
                key(tab.id) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(
                                if (tab.id == selectedTabId) 1f else 0f
                            )
                            .graphicsLayer {
                                alpha = if (tab.id == selectedTabId) 1f else 0f
                            },
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

                                loadUrl(tab.url)
                            }
                        },
                        update = { webView ->
                            if (webView.url != tab.url) {
                                webView.loadUrl(tab.url)
                            }
                        }
                    )
                }
            }
        }
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
