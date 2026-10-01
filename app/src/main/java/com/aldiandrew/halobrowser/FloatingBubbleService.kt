package com.aldiandrew.halobrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingBubbleService : Service() {

    private lateinit var wm: WindowManager
    private val bubbles = LinkedHashMap<Int, Bubble>()
    private var nextId = 1
    private var closeTarget: View? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        if (intent?.action == ACTION_ADD_BUBBLE) {
            addBubble(intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        closeAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val pi = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_halo_browser)
            .setContentTitle("Halo Browser")
            .setContentText("Floating browser is active")
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
    }

    private fun addBubble(url: String) {
        val id = nextId++
        val size = dp(68)

        val view = ImageButton(this).apply {
            setImageResource(R.drawable.ic_halo_browser)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = circle(0xFF2196F3.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            contentDescription = "Halo Browser bubble $id"
        }

        val params = overlay(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth() - size - dp(12)
            y = dp(220) + ((bubbles.size % 5) * dp(74))
        }

        val bubble = Bubble(id, url, view, params)
        view.setOnTouchListener(dragListener(bubble, size))
        bubbles[id] = bubble
        wm.addView(view, params)
    }

    private fun dragListener(
        bubble: Bubble,
        size: Int
    ): View.OnTouchListener {
        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        var moved = false

        return View.OnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX
                    sy = e.rawY
                    ox = bubble.params.x
                    oy = bubble.params.y
                    moved = false
                    showCloseTarget()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt()
                    val dy = (e.rawY - sy).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true

                    bubble.params.x = ox + dx
                    bubble.params.y = oy + dy
                    clamp(bubble.params, size)
                    wm.updateViewLayout(bubble.view, bubble.params)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val close = overCloseTarget(e.rawX, e.rawY)
                    hideCloseTarget()

                    when {
                        !moved -> expand(bubble)
                        close -> removeBubble(bubble)
                        else -> snap(bubble, size)
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun expand(bubble: Bubble) {
        try {
            wm.removeView(bubble.view)
        } catch (_: Exception) {
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(0xFF172033.toInt(), 0xFF31435E.toInt(), dp(2), dp(20))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = tool("‹", "Back")
        val forward = tool("›", "Forward")
        val refresh = tool("↻", "Reload")
        val plus = tool("+", "New bubble")
        val minimize = tool("×", "Minimize")

        val address = EditText(this).apply {
            setSingleLine(true)
            setText(bubble.url)
            hint = "Search or enter URL"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF98A5BA.toInt())
            background = rounded(0xFF25334B.toInt(), 0, 0, dp(17))
            setPadding(dp(12), 0, dp(12), 0)
        }

        bar.addView(back)
        bar.addView(forward)
        bar.addView(
            address,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        )
        bar.addView(refresh)
        bar.addView(plus)
        bar.addView(minimize)

        val web = WebView(this).apply {
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
                override fun onPageFinished(view: WebView, url: String) {
                    bubble.url = url
                    address.setText(url)
                }
            }
            loadUrl(bubble.url)
        }

        root.addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )
        root.addView(
            web,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(6)
            }
        )

        back.setOnClickListener { if (web.canGoBack()) web.goBack() }
        forward.setOnClickListener { if (web.canGoForward()) web.goForward() }
        refresh.setOnClickListener { web.reload() }
        plus.setOnClickListener { addBubble(DEFAULT_URL) }
        minimize.setOnClickListener {
            bubble.url = web.url ?: bubble.url
            web.stopLoading()
            web.destroy()
            bubble.webView = null
            bubble.expandedView = null
            try {
                wm.removeView(root)
            } catch (_: Exception) {
            }
            wm.addView(bubble.view, bubble.params)
        }

        address.setOnEditorActionListener { _, _, _ ->
            val target = normalize(address.text?.toString().orEmpty())
            bubble.url = target
            web.loadUrl(target)
            true
        }

        bubble.webView = web
        bubble.expandedView = root

        val width = min(dp(920), screenWidth() - dp(24))
        val height = min(dp(1250), screenHeight() - dp(120))

        val params = overlay(width, height).apply {
            gravity = Gravity.CENTER
        }

        wm.addView(root, params)
    }

    private fun removeBubble(bubble: Bubble) {
        bubbles.remove(bubble.id)
        try {
            wm.removeView(bubble.view)
        } catch (_: Exception) {
        }
        bubble.expandedView?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        bubble.webView?.apply {
            stopLoading()
            destroy()
        }
        bubble.webView = null
        bubble.expandedView = null

        if (bubbles.isEmpty()) stopSelf()
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return

        val size = dp(74)
        val target = TextView(this).apply {
            text = "×"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(0xFFE8EDF7.toInt())
            background = circle(0x33000000)
        }

        val params = overlay(size, size).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(18)
        }

        closeTarget = target
        wm.addView(target, params)
    }

    private fun hideCloseTarget() {
        closeTarget?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        closeTarget = null
    }

    private fun overCloseTarget(x: Float, y: Float): Boolean {
        val target = closeTarget ?: return false
        val pos = IntArray(2)
        target.getLocationOnScreen(pos)
        val cx = pos[0] + target.width / 2f
        val cy = pos[1] + target.height / 2f
        val r = max(target.width, target.height) / 2f + dp(14)
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun snap(bubble: Bubble, size: Int) {
        bubble.params.x =
            if (bubble.params.x + size / 2 < screenWidth() / 2) dp(6)
            else screenWidth() - size - dp(6)

        clamp(bubble.params, size)
        wm.updateViewLayout(bubble.view, bubble.params)
    }

    private fun clamp(params: WindowManager.LayoutParams, size: Int) {
        params.x = params.x.coerceIn(0, max(0, screenWidth() - size))
        params.y = params.y.coerceIn(
            dp(8),
            max(dp(8), screenHeight() - size - dp(8))
        )
    }

    private fun overlay(width: Int, height: Int) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

    private fun tool(label: String, description: String) =
        TextView(this).apply {
            text = label
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            minWidth = dp(42)
            contentDescription = description
        }

    private fun rounded(
        fill: Int,
        stroke: Int,
        strokeWidth: Int,
        radius: Int
    ) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius.toFloat()
        if (stroke != 0) setStroke(strokeWidth, stroke)
    }

    private fun circle(fill: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke(dp(2), 0xFFE6ECF5.toInt())
    }

    private fun closeAll() {
        hideCloseTarget()
        val all = bubbles.values.toList()
        bubbles.clear()
        all.forEach { bubble ->
            try { wm.removeView(bubble.view) } catch (_: Exception) {}
            bubble.expandedView?.let {
                try { wm.removeView(it) } catch (_: Exception) {}
            }
            bubble.webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Halo Browser Floating Service",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun normalize(value: String): String {
        val input = value.trim()
        if (input.isEmpty()) return DEFAULT_URL
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(" ") || !input.contains(".")) {
            return "https://www.google.com/search?q=" +
                android.net.Uri.encode(input)
        }
        return "https://$input"
    }

    private data class Bubble(
        val id: Int,
        var url: String,
        val view: View,
        val params: WindowManager.LayoutParams,
        var expandedView: View? = null,
        var webView: WebView? = null
    )

    companion object {
        const val ACTION_ADD_BUBBLE =
            "com.aldiandrew.halobrowser.action.ADD_BUBBLE"
        const val EXTRA_URL = "halo_browser_url"

        private const val CHANNEL_ID = "halo_floating_service"
        private const val FOREGROUND_ID = 901
        private const val DEFAULT_URL = "https://www.google.com"
    }
}