package com.example.bubblebrowser

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ShortcutInfo
import android.app.ShortcutManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val CHANNEL_ID = "halo_browser_bubble"
        private const val NOTIFICATION_ID = 1001
        private const val SHORTCUT_ID = "halo_browser_bubble"
        private const val REQUEST_POST_NOTIFICATIONS = 2001
        private const val DEFAULT_URL = "https://google.com"
        private const val CONVERSATION_SHORTCUT_CATEGORY =
            "android.shortcut.conversation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.openBubbleButton).setOnClickListener {
            openBubbleBrowser()
        }
    }

    private fun openBubbleBrowser() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
                return
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Toast.makeText(
                    this,
                    "Bubble API membutuhkan Android 10 atau lebih baru.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            createNotificationChannel()

            val shortcut = createBrowserShortcut()

            val bubbleIntent = Intent(
                this,
                BubbleBrowserActivity::class.java
            ).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(DEFAULT_URL)
            }

            val bubblePendingIntent = PendingIntent.getActivity(
                this,
                0,
                bubbleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val bubbleIcon = Icon.createWithResource(
                this,
                R.drawable.ic_halo_browser
            )

            val bubbleMetadata = Notification.BubbleMetadata.Builder(
                bubblePendingIntent,
                bubbleIcon
            )
                .setDesiredHeight(700)
                .setAutoExpandBubble(true)
                .setSuppressNotification(false)
                .build()

            val notification = Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_halo_browser)
                .setContentTitle(getString(R.string.bubble_title))
                .setContentText(getString(R.string.bubble_content))
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShortcutId(shortcut.id)
                .setBubbleMetadata(bubbleMetadata)
                .build()

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, notification)

            if (!notificationManager.areBubblesAllowed()) {
                Toast.makeText(
                    this,
                    "Bubble dinonaktifkan oleh pengaturan notifikasi.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (securityException: SecurityException) {
            Toast.makeText(
                this,
                "Notifikasi tidak diizinkan.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Gagal membuka Bubble Browser: ${e.message ?: "error tidak diketahui"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bubble_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.bubble_channel_description)
            setShowBadge(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(true)
            }
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun createBrowserShortcut(): ShortcutInfo {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
            ?: throw IllegalStateException("ShortcutManager tidak tersedia")

        val shortcutIntent = Intent(
            this,
            BubbleBrowserActivity::class.java
        ).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(DEFAULT_URL)
        }

        val shortcut = ShortcutInfo.Builder(
            this,
            SHORTCUT_ID
        )
            .setShortLabel("Halo Browser")
            .setLongLabel("Halo Browser Bubble")
            .setIcon(
                Icon.createWithResource(
                    this,
                    R.drawable.ic_halo_browser
                )
            )
            .setIntent(shortcutIntent)
            .setCategories(
                setOf(
                    CONVERSATION_SHORTCUT_CATEGORY
                )
            )
            .setLongLived(true)
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut)

        return shortcut
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_POST_NOTIFICATIONS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            openBubbleBrowser()
        }
    }
}
