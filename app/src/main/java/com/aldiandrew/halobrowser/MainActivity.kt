package com.aldiandrew.halobrowser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var waitingForOverlay = false
    private var pendingUrl: String? = null

    private val notificationLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            HaloExpressiveTheme {
                HomeScreen(
                    overlayGranted =
                        Settings.canDrawOverlays(this),
                    onLaunch = {
                        launchFloating(it)
                    },
                    onPermission = {
                        openOverlaySettings()
                    }
                )
            }
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (
            waitingForOverlay &&
            Settings.canDrawOverlays(this)
        ) {
            waitingForOverlay = false
            pendingUrl?.let {
                pendingUrl = null
                launchFloating(it)
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return

        val incoming = intent.dataString
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return

        pendingUrl = incoming

        if (Settings.canDrawOverlays(this)) {
            pendingUrl = null
            launchFloating(incoming)
        } else {
            openOverlaySettings()
        }
    }

    private fun launchFloating(url: String) {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }

        val intent =
            Intent(
                this,
                FloatingBubbleService::class.java
            ).apply {
                action =
                    FloatingBubbleService
                        .ACTION_ADD_BUBBLE
                putExtra(
                    FloatingBubbleService.EXTRA_URL,
                    url
                )
            }

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun openOverlaySettings() {
        waitingForOverlay = true
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + packageName)
            )
        )
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    overlayGranted: Boolean,
    onLaunch: (String) -> Unit,
    onPermission: () -> Unit
) {
    var address by remember {
        mutableStateOf(
            "https://www.google.com"
        )
    }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Halo Browser",
                        style =
                            MaterialTheme.typography
                                .headlineLarge
                    )
                    Text(
                        "Fast Floating Browser",
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                FilledIconButton(
                    onClick = { }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription =
                            "Settings"
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .surfaceContainerHigh
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(22.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            if (overlayGranted) "●" else "○",
                            color =
                                if (overlayGranted) {
                                    Color(0xFF19C589)
                                } else {
                                    MaterialTheme.colorScheme
                                        .error
                                },
                            style =
                                MaterialTheme.typography
                                    .headlineSmall
                        )

                        Spacer(Modifier.size(8.dp))

                        Column {
                            Text(
                                if (overlayGranted) {
                                    "Service Active"
                                } else {
                                    "Permission Required"
                                },
                                style =
                                    MaterialTheme.typography
                                        .titleLarge
                            )
                            Text(
                                if (overlayGranted) {
                                    "Floating chat heads are ready."
                                } else {
                                    "Allow Halo Browser to appear above other apps."
                                },
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.GridView,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Display over other apps"
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            onLaunch(
                                normalizeUrl(address)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Launch Floating Mode"
                        )
                    }
                }
            }

            Text(
                "Quick Actions",
                style =
                    MaterialTheme.typography
                        .headlineMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                QuickCard(
                    icon = Icons.Filled.Add,
                    label = "New Bubble",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onLaunch(
                            "https://www.google.com"
                        )
                    }
                )

                QuickCard(
                    icon = Icons.Filled.Star,
                    label = "Bookmarks",
                    modifier = Modifier.weight(1f),
                    onClick = { }
                )

                QuickCard(
                    icon = Icons.Filled.History,
                    label = "History",
                    modifier = Modifier.weight(1f),
                    onClick = { }
                )
            }

            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Website")
                }
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(28.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme.colorScheme
                        .surfaceContainer
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(label)
        }
    }
}

private fun normalizeUrl(
    value: String
): String {
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

    if (
        input.contains(" ") ||
        !input.contains(".")
    ) {
        return "https://www.google.com/search?q=" +
            Uri.encode(input)
    }

    return "https://" + input
}
