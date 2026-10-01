package com.aldiandrew.halobrowser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

class FloatingActivity : ComponentActivity() {

    private var service: FloatingBubbleService? = null
    private var bound = false

    private var currentId =
        FloatingBubbleService.TAB_ID

    private var currentUrl =
        DEFAULT_URL

    private var webView: WebView? = null

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {
                service =
                    (binder as? FloatingBubbleService.LocalBinder)
                        ?.service()

                bound = true

                service?.urlFor(currentId)?.let {
                    currentUrl = it
                }

                service?.maximize()
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {
                service = null
                bound = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(
            window,
            true
        )

        currentId =
            intent.getIntExtra(
                FloatingBubbleService.EXTRA_BUBBLE_ID,
                FloatingBubbleService.TAB_ID
            )

        currentUrl =
            intent.getStringExtra(
                FloatingBubbleService.EXTRA_URL
            ) ?: DEFAULT_URL

        bindFloatingService()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )

        setContent {
            HaloExpressiveTheme {
                BrowserSurface()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        currentId =
            intent.getIntExtra(
                FloatingBubbleService.EXTRA_BUBBLE_ID,
                currentId
            )

        currentUrl =
            intent.getStringExtra(
                FloatingBubbleService.EXTRA_URL
            ) ?: service?.urlFor(currentId)
            ?: DEFAULT_URL

        service?.maximize()
        webView?.loadUrl(currentUrl)
    }

    override fun onDestroy() {
        webView = null

        if (bound) {
            unbindService(connection)
            bound = false
        }

        service?.minimize()
        service = null

        super.onDestroy()
    }

    private fun bindFloatingService() {
        val intent =
            Intent(
                this,
                FloatingBubbleService::class.java
            )

        bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    @androidx.compose.runtime.Composable
    private fun BrowserSurface() {
        val manager =
            currentId == FloatingBubbleService.MANAGER_ID

        Scaffold(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerLowest,
            topBar = {
                if (manager) {
                    LargeTopAppBar(
                        title = {
                            Text("Browser Manager")
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { finish() }
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close"
                                )
                            }
                        },
                        actions = {
                            FilledIconButton(
                                onClick = {
                                    service?.addTab(
                                        DEFAULT_URL,
                                        true
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "New bubble"
                                )
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                "Halo Browser",
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (
                                        webView?.canGoBack() == true
                                    ) {
                                        webView?.goBack()
                                    } else {
                                        finish()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    service?.addTab(
                                        DEFAULT_URL,
                                        true
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "New bubble"
                                )
                            }

                            IconButton(
                                onClick = {
                                    webView?.reload()
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Reload"
                                )
                            }

                            IconButton(
                                onClick = { finish() }
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Minimize"
                                )
                            }
                        }
                    )
                }
            }
        ) { padding ->
            if (manager) {
                BrowserManagerContent(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                BrowserTabContent(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun BrowserManagerContent(
        modifier: Modifier
    ) {
        var refreshKey by remember {
            mutableStateOf(0)
        }

        val tabs =
            service?.bubbleSnapshot()
                ?.filterNot { it.isManager }
                ?: emptyList()

        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "Recent",
                    style =
                        MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    tabs.size.toString() + " tabs",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                items(
                    tabs,
                    key = { it.id }
                ) { item ->
                    ElevatedCard(
                        onClick = {
                            service?.openBubble(item.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors =
                            CardDefaults.elevatedCardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceContainer
                            )
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (
                                        item.url ==
                                        DEFAULT_URL
                                    ) {
                                        "New Tab"
                                    } else {
                                        item.url
                                    },
                                    maxLines = 1
                                )
                            },
                            supportingContent = {
                                Text(
                                    item.url,
                                    maxLines = 1
                                )
                            },
                            leadingContent = {
                                Surface(
                                    modifier =
                                        Modifier.size(52.dp),
                                    shape =
                                        RoundedCornerShape(18.dp),
                                    color =
                                        MaterialTheme.colorScheme
                                            .secondaryContainer
                                ) {
                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Tab,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        service?.closeTab(item.id)
                                        refreshKey++
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription =
                                            "Close bubble"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    service?.addTab(
                        DEFAULT_URL,
                        true
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null
                )
                Spacer(Modifier.size(8.dp))
                Text("New Bubble")
            }

            if (refreshKey > -1) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun BrowserTabContent(
        modifier: Modifier
    ) {
        var address by remember(currentId) {
            mutableStateOf(currentUrl)
        }

        Column(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .padding(8.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color =
                    MaterialTheme.colorScheme
                        .surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            webView?.goBack()
                        }
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = {
                            Text("Search or enter URL")
                        }
                    )

                    IconButton(
                        onClick = {
                            val target =
                                normalizeUrl(address)

                            currentUrl = target
                            service?.updateUrl(
                                currentId,
                                target
                            )
                            webView?.loadUrl(target)
                        }
                    ) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Go"
                        )
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp)),
                factory = { context ->
                    WebView(context).apply {
                        webView = this

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false

                        webChromeClient =
                            WebChromeClient()

                        webViewClient =
                            object : WebViewClient() {
                                override fun
                                    onPageFinished(
                                    view: WebView,
                                    url: String
                                ) {
                                    super.onPageFinished(
                                        view,
                                        url
                                    )

                                    currentUrl = url
                                    address = url

                                    service?.updateUrl(
                                        currentId,
                                        url
                                    )
                                }

                                override fun
                                    shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): Boolean {
                                    return false
                                }
                            }

                        loadUrl(currentUrl)
                    }
                },
                update = { view ->
                    if (view.url != currentUrl) {
                        view.loadUrl(currentUrl)
                    }
                }
            )
        }
    }

    companion object {
        private const val DEFAULT_URL =
            "https://www.google.com"

        private fun normalizeUrl(
            value: String
        ): String {
            val input = value.trim()

            if (input.isEmpty()) {
                return DEFAULT_URL
            }

            if (
                input.startsWith("http://") ||
                input.startsWith("https://")
            ) {
                return input
            }

            if (
                input.contains(" ") ||
                !input.contains(".")
            ) {
                return "https://www.google.com/search?q=" +
                    android.net.Uri.encode(input)
            }

            return "https://" + input
        }
    }
}
