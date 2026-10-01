package com.aldiandrew.halobrowser

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import java.util.UUID

class BubbleActivity : Activity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val newBubbleButton = Button(this).apply {
            text = "+"
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                createBubble(this@BubbleActivity, DEFAULT_URL)
            }
        }

        toolbar.addView(
            newBubbleButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

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

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        root.addView(
            browserView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        val url = intent.getStringExtra(MainActivity.EXTRA_URL)
            ?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }
            ?: DEFAULT_URL

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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {

        private const val DEFAULT_URL = "https://www.google.com"
        private const val CHANNEL_ID = "halo_browser_bubbles_v3"
        private const val BASE_NOTIFICATION_ID = 3000

        fun createBubble(context: Context, url: String) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return
            }

            ensureChannel(context)

            val bubbleId = "halo_browser_" + UUID.randomUUID().toString()
            val notificationId =
                BASE_NOTIFICATION_ID + (System.currentTimeMillis() % 100000000).toInt()

            val bubbleIntent = Intent(context, BubbleActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_URL, url)
                putExtra(MainActivity.EXTRA_BUBBLE_ID, bubbleId)
            }

            val pendingIntentFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

            val bubblePendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                bubbleIntent,
                pendingIntentFlags
            )

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_URL, url)
            }

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val shortcut = ShortcutInfoCompat.Builder(context, bubbleId)
                .setShortLabel("Tab")
                .setLongLabel("Halo Browser tab")
                .setIcon(
                    IconCompat.createWithResource(
                        context,
                        com.aldiandrew.halobrowser.R.drawable.ic_halo_browser
                    )
                )
                .setLongLived(true)
                .setIntent(bubbleIntent)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

            val person = Person.Builder()
                .setName("Halo Browser")
                .setKey(bubbleId)
                .setImportant(true)
                .build()

            val bubbleIcon = IconCompat.createWithResource(
                context,
                com.aldiandrew.halobrowser.R.drawable.ic_halo_browser
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
                .setConversationTitle("Halo Browser")
                .addMessage(
                    "New browser tab",
                    System.currentTimeMillis(),
                    person
                )

            val notification = NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(com.aldiandrew.halobrowser.R.drawable.ic_halo_browser)
                .setContentTitle("Halo Browser")
                .setContentText("New browser tab")
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentIntent(contentPendingIntent)
                .setShortcutId(bubbleId)
                .setBubbleMetadata(bubbleMetadata)
                .setStyle(style)
                .addPerson(person)
                .build()

            NotificationManagerCompat.from(context).notify(
                notificationId,
                notification
            )
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(com.aldiandrew.halobrowser.R.string.bubble_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
