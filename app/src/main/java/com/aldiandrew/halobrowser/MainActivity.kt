package com.aldiandrew.halobrowser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var waitingForOverlayPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        initialUrl = extractUrl(intent) ?: DEFAULT_URL,
                        overlayGranted = Settings.canDrawOverlays(this),
                        onLaunch = { url -> launchFloatingMode(url) },
                        onPermission = { openOverlaySettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false
            launchFloatingMode(extractUrl(intent) ?: DEFAULT_URL)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Settings.canDrawOverlays(this)) {
            launchFloatingMode(extractUrl(intent) ?: DEFAULT_URL)
        }
    }

    private fun launchFloatingMode(url: String) {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }

        val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_ADD_BUBBLE
            putExtra(FloatingBubbleService.EXTRA_URL, url)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun openOverlaySettings() {
        waitingForOverlayPermission = true
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun extractUrl(sourceIntent: Intent?): String? {
        val value = sourceIntent?.getStringExtra(EXTRA_URL) ?: sourceIntent?.data?.toString()
        return value?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    companion object {
        const val EXTRA_URL = "halo_browser_url"
        private const val DEFAULT_URL = "https://www.google.com"
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    initialUrl: String,
    overlayGranted: Boolean,
    onLaunch: (String) -> Unit,
    onPermission: () -> Unit
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }

    val background = Color(0xFF080D2A)
    val card = Color(0xFF1E2A3D)

    Column(
        modifier = Modifier.fillMaxSize().background(background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Halo Browser",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Fast Floating Browser",
            color = Color(0xFF9BA8C2),
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            color = card,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (overlayGranted) "●" else "○",
                        color = if (overlayGranted) Color(0xFF19C589) else Color(0xFFFFB74D),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Text(
                        text = if (overlayGranted) "Floating Service Ready" else "Overlay Permission Needed",
                        color = if (overlayGranted) Color(0xFF19C589) else Color(0xFFFFB74D),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = if (overlayGranted) {
                        "Bubbles can appear above other apps."
                    } else {
                        "Allow Halo Browser to display floating bubbles above other apps."
                    },
                    color = Color(0xFF8997B1),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = onPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Display over other apps")
                }

                Button(
                    onClick = { onLaunch(normalizeUrl(url)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Launch Floating Mode")
                }
            }
        }

        Text(
            text = "Quick Actions",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedButton(
            onClick = { onLaunch(DEFAULT_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+  New Bubble")
        }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            singleLine = true,
            label = { Text("Website") },
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { url = DEFAULT_URL }) {
            Text("Reset to Google")
        }
    }
}

private const val DEFAULT_URL = "https://www.google.com"

private fun normalizeUrl(value: String): String {
    val input = value.trim()
    if (input.isEmpty()) return DEFAULT_URL
    if (input.startsWith("http://") || input.startsWith("https://")) return input
    if (input.contains(" ") || !input.contains(".")) {
        return "https://www.google.com/search?q=" + Uri.encode(input)
    }
    return "https://$input"
}