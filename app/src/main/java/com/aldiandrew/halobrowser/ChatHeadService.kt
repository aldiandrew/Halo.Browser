package com.aldiandrew.halobrowser

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.core.app.NotificationCompat
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Floating browser controller rebuilt around the interaction model of the supplied
 * Bubble/Fast Floating Browser reference APK, without its advertising components.
 *
 * Core model:
 * - one manager head + up to four browser-tab heads
 * - FREE/CAPTURED drag state
 * - edge snap after release
 * - close target attraction
 * - active/hero tab
 * - opening a head hides that head while the browser window is visible
 * - manager restores/arranges the remaining heads
 */
class ChatHeadService : Service() {

    companion object {
        const val ACTION_ADD_BUBBLE = "com.aldiandrew.halobrowser.ADD_BUBBLE"
        const val EXTRA_URL = "url"
        private const val CHANNEL_ID = "halo_floating"
        private const val NOTIFICATION_ID = 1101
        private const val MAX_TABS = 4
        private const val HEAD_DP = 58
        private const val EDGE_DP = 10
        private const val CLOSE_DP = 112
    }

    private enum class HeadState { FREE, CAPTURED }

    private data class Head(
        val tab: BrowserTab?,
        val view: TextView,
        var state: HeadState = HeadState.FREE,
        var downX: Float = 0f,
        var downY: Float = 0f,
        var startX: Int = 0,
        var startY: Int = 0,
        var hidden: Boolean = false
    )

    private lateinit var wm: WindowManager
    private lateinit var data: DataManager

    private val heads = LinkedHashMap<String, Head>()
    private var managerHead: Head? = null
    private var activeId: String? = null

    private var browserContainer: FrameLayout? = null
    private var browserParams: WindowManager.LayoutParams? = null
    private var currentWebView: WebView? = null
    private var expandedId: String? = null
    private var managerContainer: FrameLayout? = null
    private var closeView: TextView? = null
    private var draggingHead: Head? = null

    private val density get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        data = DataManager(this)
        createNotificationChannel()
        startAsForeground()
        restoreTabs()
        if (heads.isEmpty()) addTab("https://www.google.com")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ADD_BUBBLE) {
            val url = intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
            if (url != null) addTab(url)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        saveTabs()
        closeBrowser(false)
        closeManager(false)
        closeCloseTarget()
        heads.values.forEach { removeHead(it) }
        managerHead?.let { removeHead(it) }
        heads.clear()
        managerHead = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating browser",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Halo Browser floating mode available."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Halo Browser")
            .setContentText("Floating browser is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun overlayParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun restoreTabs() {
        val saved = data.loadActiveTabs().take(MAX_TABS)
        if (saved.isEmpty()) return

        saved.forEach { tab ->
            addTab(tab.url, tab, persist = false)
        }
        arrangeHeads(animated = false)
    }

    private fun newId() = "browser_tab_" + System.currentTimeMillis()

    private fun addTab(
        url: String,
        saved: BrowserTab? = null,
        persist: Boolean = true
    ) {
        if (heads.size >= MAX_TABS) {
            managerHead?.let { openManager() }
            return
        }

        val tab = saved ?: BrowserTab(
            id = newId(),
            url = normalizeUrl(url)
        )

        val bubble = createHeadView(tab)
        val head = Head(tab, bubble)
        heads[tab.id] = head
        wm.addView(bubble, headParams(positionForIndex(heads.size - 1)))
        bubble.setOnTouchListener { _, event -> handleTouch(head, event) }

        if (activeId == null) activeId = tab.id
        ensureManager()
        arrangeHeads(animated = false)
        if (persist) saveTabs()
    }

    private fun ensureManager() {
        if (managerHead != null) return

        val view = createManagerView()
        val head = Head(null, view)
        managerHead = head
        wm.addView(view, managerParams())
        view.setOnTouchListener { _, event -> handleTouch(head, event) }
    }

    private fun createHeadView(tab: BrowserTab): TextView {
        return TextView(this).apply {
            gravity = Gravity.CENTER
            text = "•"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            background = bubbleDrawable(tab.incognito)
            elevation = dp(8).toFloat()
            contentDescription = tab.title.ifBlank { tab.url }
        }
    }

    private fun createManagerView(): TextView {
        return TextView(this).apply {
            gravity = Gravity.CENTER
            text = "▦"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            background = bubbleDrawable(false, manager = true)
            elevation = dp(8).toFloat()
            contentDescription = "Browser manager"
        }
    }

    private fun bubbleDrawable(incognito: Boolean, manager: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                when {
                    manager -> Color.rgb(55, 55, 62)
                    incognito -> Color.rgb(40, 40, 45)
                    else -> Color.rgb(45, 115, 230)
                }
            )
            setStroke(dp(2), Color.WHITE)
        }
    }

    private fun headParams(index: Int): WindowManager.LayoutParams {
        val p = overlayParams(dp(HEAD_DP), dp(HEAD_DP))
        p.x = if (index % 2 == 0) dp(EDGE_DP) else screenWidth() - dp(HEAD_DP + EDGE_DP)
        p.y = dp(130 + index * 72)
        return p
    }

    private fun managerParams(): WindowManager.LayoutParams {
        val p = overlayParams(dp(HEAD_DP), dp(HEAD_DP))
        p.x = screenWidth() - dp(HEAD_DP + EDGE_DP)
        p.y = screenHeight() - dp(HEAD_DP + 100)
        return p
    }

    private fun screenWidth() = wm.currentWindowMetrics.bounds.width()
    private fun screenHeight() = wm.currentWindowMetrics.bounds.height()

    private fun handleTouch(head: Head, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggingHead = head
                head.state = HeadState.CAPTURED
                head.downX = event.rawX
                head.downY = event.rawY
                val lp = head.view.layoutParams as WindowManager.LayoutParams
                head.startX = lp.x
                head.startY = lp.y
                showCloseTarget()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggingHead !== head) return true
                val dx = (event.rawX - head.downX).toInt()
                val dy = (event.rawY - head.downY).toInt()
                moveHead(head, head.startX + dx, head.startY + dy)
                updateCloseTarget(head)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingHead === head) {
                    val close = isNearCloseTarget(head)
                    draggingHead = null
                    head.state = HeadState.FREE
                    closeCloseTarget()
                    if (close) {
                        if (head.tab == null) {
                            stopSelf()
                        } else {
                            removeTab(head.tab.id)
                        }
                    } else {
                        snapHead(head)
                    }
                }
                return true
            }
        }
        return true
    }

    private fun moveHead(head: Head, x: Int, y: Int) {
        val lp = head.view.layoutParams as WindowManager.LayoutParams
        lp.x = x.coerceIn(0, screenWidth() - dp(HEAD_DP))
        lp.y = y.coerceIn(dp(24), screenHeight() - dp(HEAD_DP))
        try { wm.updateViewLayout(head.view, lp) } catch (_: Exception) {}
    }

    private fun snapHead(head: Head) {
        val lp = head.view.layoutParams as WindowManager.LayoutParams
        val targetX =
            if (lp.x + dp(HEAD_DP / 2) < screenWidth() / 2)
                dp(EDGE_DP)
            else
                screenWidth() - dp(HEAD_DP + EDGE_DP)

        animateHead(head, lp.x, lp.y, targetX, lp.y)
    }

    private fun animateHead(head: Head, sx: Int, sy: Int, tx: Int, ty: Int) {
        val start = System.currentTimeMillis()
        val duration = 220L
        val runnable = object : Runnable {
            override fun run() {
                val t = ((System.currentTimeMillis() - start).toFloat() / duration)
                    .coerceIn(0f, 1f)
                val eased = 1f - (1f - t) * (1f - t)
                moveHead(
                    head,
                    (sx + (tx - sx) * eased).toInt(),
                    (sy + (ty - sy) * eased).toInt()
                )
                if (t < 1f) head.view.post(this)
            }
        }
        head.view.post(runnable)
    }

    private fun arrangeHeads(animated: Boolean) {
        val visible = heads.values.filter { !it.hidden && it !== draggingHead }
        visible.forEachIndexed { index, head ->
            val targetX =
                if (index % 2 == 0) dp(EDGE_DP)
                else screenWidth() - dp(HEAD_DP + EDGE_DP)
            val targetY = dp(130 + index * 72)
            val lp = head.view.layoutParams as WindowManager.LayoutParams
            if (animated) animateHead(head, lp.x, lp.y, targetX, targetY)
            else moveHead(head, targetX, targetY)
        }

        managerHead?.let {
            if (draggingHead !== it) {
                val lp = it.view.layoutParams as WindowManager.LayoutParams
                val x = screenWidth() - dp(HEAD_DP + EDGE_DP)
                val y = screenHeight() - dp(HEAD_DP + 100)
                if (animated) animateHead(it, lp.x, lp.y, x, y)
                else moveHead(it, x, y)
            }
        }
    }

    private fun showCloseTarget() {
        if (closeView != null) return
        val v = TextView(this).apply {
            gravity = Gravity.CENTER
            text = "×"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(80, 80, 88))
                setStroke(dp(2), Color.WHITE)
            }
            elevation = dp(10).toFloat()
        }
        closeView = v
        val p = overlayParams(dp(72), dp(72)).apply {
            x = (screenWidth() - dp(72)) / 2
            y = screenHeight() - dp(96)
        }
        try { wm.addView(v, p) } catch (_: Exception) {}
    }

    private fun updateCloseTarget(head: Head) {
        val lp = head.view.layoutParams as WindowManager.LayoutParams
        val cx = lp.x + dp(HEAD_DP / 2)
        val cy = lp.y + dp(HEAD_DP / 2)
        val tx = screenWidth() / 2
        val ty = screenHeight() - dp(60)
        val near = distance(cx, cy, tx, ty) < dp(CLOSE_DP)
        closeView?.alpha = if (near) 1f else 0.72f
    }

    private fun isNearCloseTarget(head: Head): Boolean {
        val lp = head.view.layoutParams as WindowManager.LayoutParams
        return distance(
            lp.x + dp(HEAD_DP / 2),
            lp.y + dp(HEAD_DP / 2),
            screenWidth() / 2,
            screenHeight() - dp(60)
        ) < dp(CLOSE_DP)
    }

    private fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        return kotlin.math.sqrt(
            ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble()
        ).toInt()
    }

    private fun closeCloseTarget() {
        closeView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        closeView = null
    }

    private fun removeTab(id: String) {
        val head = heads.remove(id) ?: return
        removeHead(head)
        if (activeId == id) activeId = heads.keys.firstOrNull()
        if (expandedId == id) closeBrowser(false)
        saveTabs()
        arrangeHeads(animated = true)
        if (heads.isEmpty()) {
            stopSelf()
        }
    }

    private fun removeHead(head: Head) {
        try { wm.removeView(head.view) } catch (_: Exception) {}
    }

    private fun openBubble(id: String) {
        val head = heads[id] ?: return
        activeId = id
        expandedId = id
        head.hidden = true
        head.view.visibility = View.GONE
        closeManager(false)
        openBrowser(head.tab!!)
        arrangeHeads(animated = false)
    }

    private fun openManager() {
        if (managerContainer != null) return

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.argb(245, 250, 250, 252))
            }
        }

        val title = TextView(this).apply {
            text = "Browser manager"
            setTextColor(Color.rgb(30, 30, 35))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(title, LinearLayout.LayoutParams(-1, -2))

        heads.values.forEach { head ->
            val tab = head.tab ?: return@forEach
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(8), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(Color.rgb(238, 238, 242))
                }
                setOnClickListener {
                    closeManager(false)
                    openBubble(tab.id)
                }
            }
            val label = TextView(this).apply {
                text = tab.title.ifBlank { tab.url }
                setTextColor(Color.rgb(25, 25, 30))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 2
            }
            row.addView(label, LinearLayout.LayoutParams(0, dp(58), 1f))
            val close = TextView(this).apply {
                text = "×"
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setOnClickListener {
                    removeTab(tab.id)
                    closeManager(true)
                }
            }
            row.addView(close, LinearLayout.LayoutParams(dp(48), dp(58)))
            val lp = LinearLayout.LayoutParams(-1, dp(66))
            lp.bottomMargin = dp(8)
            card.addView(row, lp)
        }

        val add = Button(this).apply {
            text = "New bubble"
            setOnClickListener {
                closeManager(false)
                addTab("https://www.google.com")
            }
        }
        card.addView(add, LinearLayout.LayoutParams(-1, dp(52)))

        val closeAll = Button(this).apply {
            text = "Close manager"
            setOnClickListener { closeManager(false) }
        }
        card.addView(closeAll, LinearLayout.LayoutParams(-1, dp(52)))

        root.addView(card, FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.CENTER
            leftMargin = dp(18)
            rightMargin = dp(18)
        })

        managerContainer = root
        val p = overlayParams((screenWidth() * 0.78f).toInt(), (screenHeight() * 0.64f).toInt()).apply {
            x = ((screenWidth() - width) / 2).coerceAtLeast(0)
            y = ((screenHeight() - height) / 2).coerceAtLeast(0)
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try { wm.addView(root, p) } catch (_: Exception) { managerContainer = null }
    }

    private fun openBrowser(tab: BrowserTab) {
        closeBrowser(false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.LTGRAY)
            }
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun action(text: String, click: () -> Unit): TextView =
            TextView(this).apply {
                this.text = text
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setOnClickListener { click() }
            }

        val back = action("‹") { currentWebView?.goBack() }
        val forward = action("›") { currentWebView?.goForward() }
        val reload = action("↻") { currentWebView?.reload() }
        val manager = action("▦") {
            closeBrowser(true)
            openManager()
        }
        val close = action("×") { minimizeBrowser() }

        listOf(back, forward, reload, manager, close).forEach {
            toolbar.addView(it, LinearLayout.LayoutParams(dp(42), dp(42)))
        }

        val address = EditText(this).apply {
            setSingleLine(true)
            setText(tab.url)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(12), 0, dp(12), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.rgb(242, 242, 245))
            }
            setOnEditorActionListener { _, _, _ ->
                currentWebView?.loadUrl(normalizeUrl(text.toString()))
                true
            }
        }
        toolbar.addView(address, LinearLayout.LayoutParams(0, dp(42), 1f))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))

        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.userAgentString =
                if (tab.desktopSite) {
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/140 Safari/537.36"
                } else {
                    settings.userAgentString
                }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val u = url ?: return
                    tab.url = u
                    tab.title = view?.title.orEmpty()
                    address.setText(u)
                    data.addHistory(u, tab.title)
                    saveTabs()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val u = request?.url?.toString() ?: return false
                    if (u.startsWith("http://") || u.startsWith("https://")) {
                        view?.loadUrl(u)
                        return true
                    }
                    return false
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(normalizeUrl(tab.url))
        }

        if (tab.nightMode) {
            web.settings.forceDark = WebSettings.FORCE_DARK_ON
        }

        currentWebView = web
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        browserContainer = FrameLayout(this).apply { addView(root) }
        val p = overlayParams((screenWidth() * 0.78f).toInt(), (screenHeight() * 0.64f).toInt()).apply {
            x = ((screenWidth() - width) / 2).coerceAtLeast(0)
            y = ((screenHeight() - height) / 2).coerceAtLeast(0)
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        browserParams = p
        try { wm.addView(browserContainer, p) } catch (_: Exception) {
            browserContainer = null
            currentWebView = null
        }
    }

    private fun minimizeBrowser() {
        closeBrowser(true)
        expandedId?.let { id ->
            heads[id]?.let {
                it.hidden = false
                it.view.visibility = View.VISIBLE
                snapHead(it)
            }
        }
        expandedId = null
        arrangeHeads(animated = true)
    }

    private fun closeBrowser(restore: Boolean) {
        currentWebView?.apply {
            stopLoading()
            destroy()
        }
        currentWebView = null
        browserContainer?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        browserContainer = null
        browserParams = null

        if (restore) {
            expandedId?.let { id ->
                heads[id]?.let {
                    it.hidden = false
                    it.view.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun closeManager(refresh: Boolean) {
        managerContainer?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        managerContainer = null
        if (refresh) arrangeHeads(animated = true)
    }

    private fun saveTabs() {
        data.saveActiveTabs(heads.values.mapNotNull { it.tab })
    }

    private fun normalizeUrl(value: String): String {
        val input = value.trim()
        if (input.isEmpty()) return "https://www.google.com"
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(" ") || !input.contains(".")) {
            return data.searchEngine + android.net.Uri.encode(input)
        }
        return "https://$input"
    }
}
