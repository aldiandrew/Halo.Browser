package com.aldiandrew.halobrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChatHeadService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var data: DataManager

    private val tabs = LinkedHashMap<String, Head>()
    private var managerHead: Head? = null
    private var activeId: String? = null

    private var expandedRoot: FrameLayout? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var expandedWeb: WebView? = null
    private var address: EditText? = null
    private var title: TextView? = null
    private var managerList: LinearLayout? = null
    private var expandedId: String? = null

    private var closeTarget: View? = null
    private var dragging: Head? = null
    private var lastDragX = 0f
    private var lastDragY = 0f

    inner class LocalBinder : android.os.Binder() {
        fun service(): ChatHeadService = this@ChatHeadService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        data = DataManager(this)
        createChannel()
        startAsForeground()
        restore()
        createManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ADD_BUBBLE) {
            addTab(intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL, false)
        }
        if (intent?.action == ACTION_OPEN_BUBBLE) {
            intent.getStringExtra(EXTRA_BUBBLE_ID)?.let { openBubble(it) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        closeExpanded(false)
        tabs.values.forEach { removeHead(it) }
        managerHead?.let { removeHead(it) }
        data.saveActiveTabs(tabs.values.map { it.tab })
        super.onDestroy()
    }

    fun addTab(url: String, open: Boolean): String {
        if (tabs.size >= MAX_TABS) {
            val existing = tabs.values.lastOrNull()
            if (open && existing != null) openBubble(existing.tab.id)
            return existing?.tab?.id ?: ""
        }
        val id = "browser_tab_" + System.currentTimeMillis()
        val tab = BrowserTab(id, normalizeUrl(url))
        val index = tabs.size
        tab.x = if (index % 2 == 0) dp(6) else screenWidth() - bubbleSize() - dp(6)
        tab.y = (dp(110) + (index / 2) * dp(82)).coerceAtMost(screenHeight() - bubbleSize() - dp(80))
        val head = Head(tab)
        tabs[id] = head
        addHead(head)
        persist()
        if (open) openBubble(id)
        return id
    }

    fun openBubble(id: String) {
        if (id == MANAGER_ID) {
            openManager()
            return
        }
        val head = tabs[id] ?: return
        activeId = id
        expandTab(head)
    }

    fun closeTab(id: String) {
        val head = tabs[id] ?: return
        if (expandedId == id) closeExpanded(false)
        tabs.remove(id)
        removeHead(head)
        persist()
        updateManager()
        if (tabs.isEmpty()) addTab(DEFAULT_URL, false)
    }

    fun urlFor(id: String): String? = tabs[id]?.tab?.url

    fun updateUrl(id: String, url: String) {
        tabs[id]?.tab?.url = url
        persist()
    }

    fun minimize() {
        closeExpanded(true)
    }

    fun maximize() {
        activeId?.let { tabs[it]?.let { expandTab(it) } }
    }

    fun bubbleSnapshot(): List<BubbleInfo> {
        return tabs.values.map {
            BubbleInfo(it.tab.id, it.tab.url, it.tab.title, it.tab.incognito, false)
        }
    }

    private fun restore() {
        val restored = data.loadActiveTabs().asSequence()
            .filter { it.id.isNotBlank() && it.id != MANAGER_ID }
            .take(MAX_TABS).toList()
        restored.forEach {
            val head = Head(it)
            tabs[it.id] = head
            addHead(head)
        }
        if (tabs.isEmpty()) {
            addTab(DEFAULT_URL, false)
            return
        }
        val allRight = tabs.values.all { it.tab.x > screenWidth() / 2 }
        val allLeft = tabs.values.all { it.tab.x < screenWidth() / 2 }
        val overlap = tabs.values.toList().let { list ->
            list.indices.any { i ->
                list.indices.drop(i + 1).any { j ->
                    abs(list[i].tab.x - list[j].tab.x) < dp(48) &&
                        abs(list[i].tab.y - list[j].tab.y) < dp(48)
                }
            }
        }
        if (allRight || allLeft || overlap) {
            tabs.values.forEachIndexed { index, head ->
                head.tab.x = if (index % 2 == 0) dp(6) else screenWidth() - bubbleSize() - dp(6)
                head.tab.y = (dp(110) + (index / 2) * dp(82))
                    .coerceAtMost(screenHeight() - bubbleSize() - dp(80))
                updatePosition(head)
            }
        }
        persist()
    }

    private fun persist() {
        data.saveActiveTabs(tabs.values.map { it.tab })
    }

    private fun createManager() {
        if (managerHead != null) return
        val tab = BrowserTab(MANAGER_ID, "about:manager", "Browser Manager")
        tab.x = screenWidth() - bubbleSize() - dp(8)
        tab.y = dp(16)
        managerHead = Head(tab, true)
        addHead(managerHead!!)
    }

    private fun addHead(head: Head) {
        val root = FrameLayout(this)
        root.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (head.manager) 0xFF6750A4.toInt() else if (head.tab.incognito) 0xFF29272D.toInt() else 0xFFF5F1F8.toInt())
            setStroke(dp(1), 0x33000000)
        }
        root.elevation = dp(8).toFloat()

        val icon = ImageView(this)
        icon.scaleType = ImageView.ScaleType.CENTER_CROP
        icon.setPadding(dp(8), dp(8), dp(8), dp(8))
        icon.setImageResource(R.drawable.ic_halo_browser)
        root.addView(icon, FrameLayout.LayoutParams(-1, -1))
        head.icon = icon
        head.view = root

        val p = overlayParams(bubbleSize(), bubbleSize(), false)
        p.x = head.tab.x
        p.y = head.tab.y
        head.params = p

        root.setOnTouchListener(touch(head))
        wm.addView(root, p)
        loadIcon(head)
    }

    private fun loadIcon(head: Head) {
        val encoded = head.tab.favicon ?: return
        try {
            val bytes = Base64.getDecoder().decode(encoded)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
                head.icon?.setImageBitmap(it)
            }
        } catch (_: Exception) {
        }
    }

    private fun touch(head: Head): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        var tracker: VelocityTracker? = null

        return View.OnTouchListener { view, event ->
            val p = head.params ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = head
                    downX = event.rawX
                    downY = event.rawY
                    lastDragX = event.rawX
                    lastDragY = event.rawY
                    startX = p.x
                    startY = p.y
                    moved = false
                    tracker?.recycle()
                    tracker = VelocityTracker.obtain()
                    tracker?.addMovement(event)
                    showCloseTarget()
                    springScale(view, 0.84f)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(event)
                    lastDragX = event.rawX
                    lastDragY = event.rawY
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    p.x = startX + dx.toInt()
                    p.y = startY + dy.toInt()
                    clamp(p)
                    wm.updateViewLayout(view, p)
                    head.tab.x = p.x
                    head.tab.y = p.y
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tracker?.addMovement(event)
                    tracker?.computeCurrentVelocity(1000)
                    val vx = tracker?.xVelocity ?: 0f
                    val vy = tracker?.yVelocity ?: 0f
                    tracker?.recycle()
                    tracker = null
                    hideCloseTarget()
                    springScale(view, 1f)

                    val releaseX = if (event.rawX.isNaN()) lastDragX else event.rawX
                    val releaseY = if (event.rawY.isNaN()) lastDragY else event.rawY
                    if (isOverCloseTarget(releaseX, releaseY)) {
                        if (!head.manager) closeTab(head.tab.id)
                        dragging = null
                        return@OnTouchListener true
                    }

                    if (!moved) {
                        if (head.manager) openManager() else openBubble(head.tab.id)
                    } else {
                        settle(head, vx, vy)
                    }
                    dragging = null
                    true
                }
                else -> true
            }
        }
    }

    private fun settle(head: Head, vx: Float, vy: Float) {
        val p = head.params ?: return
        val predicted = p.x + (vx * 0.12f).toInt()
        val targetX = if (predicted + bubbleSize() / 2 < screenWidth() / 2) dp(4) else screenWidth() - bubbleSize() - dp(4)
        val targetY = (p.y + (vy * 0.08f).toInt()).coerceIn(dp(8), screenHeight() - bubbleSize() - dp(8))
        springTo(head, targetX, targetY, vx, vy)
        head.tab.x = targetX
        head.tab.y = targetY
        persist()
    }

    private fun arrange() {
        // The reference ChatHead manager does not continuously re-stack every
        // bubble. Positions belong to each bubble and are changed only by
        // creation, dragging and the final edge-snap animation.
        tabs.values.forEach { head ->
            val p = head.params ?: return@forEach
            if (head === dragging) return@forEach
            val maxX = max(0, screenWidth() - bubbleSize())
            val maxY = max(dp(8), screenHeight() - bubbleSize() - dp(8))
            val x = head.tab.x.coerceIn(0, maxX)
            val y = head.tab.y.coerceIn(dp(8), maxY)
            if (x != head.tab.x || y != head.tab.y) {
                head.tab.x = x
                head.tab.y = y
                p.x = x
                p.y = y
                updatePosition(head)
            }
        }
        managerHead?.let { head ->
            if (head.tab.x !in 0..max(0, screenWidth() - bubbleSize()) ||
                head.tab.y !in dp(8)..max(dp(8), screenHeight() - bubbleSize() - dp(8))) {
                head.tab.x = screenWidth() - bubbleSize() - dp(8)
                head.tab.y = dp(16)
                updatePosition(head)
            }
        }
    }

    private fun springTo(head: Head, targetX: Int, targetY: Int, vx: Float, vy: Float) {
        val p = head.params ?: return
        val view = head.view ?: return

        val px = object : FloatPropertyCompat<WindowManager.LayoutParams>("chatHeadX") {
            override fun getValue(v: WindowManager.LayoutParams): Float = v.x.toFloat()
            override fun setValue(v: WindowManager.LayoutParams, value: Float) {
                v.x = value.toInt()
                try { wm.updateViewLayout(view, v) } catch (_: Exception) {}
            }
        }
        val py = object : FloatPropertyCompat<WindowManager.LayoutParams>("chatHeadY") {
            override fun getValue(v: WindowManager.LayoutParams): Float = v.y.toFloat()
            override fun setValue(v: WindowManager.LayoutParams, value: Float) {
                v.y = value.toInt()
                try { wm.updateViewLayout(view, v) } catch (_: Exception) {}
            }
        }

        SpringAnimation(p, px).apply {
            spring = SpringForce(targetX.toFloat()).apply {
                stiffness = 700f
                dampingRatio = 0.78f
            }
            setStartVelocity(vx)
        }.start()

        SpringAnimation(p, py).apply {
            spring = SpringForce(targetY.toFloat()).apply {
                stiffness = 700f
                dampingRatio = 0.78f
            }
            setStartVelocity(vy)
        }.start()
    }

    private fun springScale(view: View, target: Float) {
        SpringAnimation(view, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(target).apply {
                stiffness = 900f
                dampingRatio = 0.72f
            }
        }.start()
        SpringAnimation(view, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(target).apply {
                stiffness = 900f
                dampingRatio = 0.72f
            }
        }.start()
    }

    private fun expandTab(head: Head) {
        closeExpanded(false)
        val root = head.view as? FrameLayout ?: return
        activeId = head.tab.id
        expandedId = head.tab.id
        head.expanded = true
        root.setOnTouchListener(null)
        root.removeAllViews()
        root.background = rounded(0xFFFDFBFF.toInt(), dp(28))

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
        }
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title = TextView(this).apply {
            text = head.tab.title.ifBlank { "Halo Browser" }
            textSize = 15f
            setTextColor(0xFF262229.toInt())
            maxLines = 1
        }
        bar.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))

        val back = button("‹")
        val forward = button("›")
        val reload = button("↻")
        val close = button("×")
        bar.addView(back, buttonParams())
        bar.addView(forward, buttonParams())
        bar.addView(reload, buttonParams())
        bar.addView(close, buttonParams())
        column.addView(bar)

        address = EditText(this).apply {
            hint = "Search or enter URL"
            setSingleLine(true)
            textSize = 14f
            setText(if (head.tab.url == NEW_TAB_URL) "" else head.tab.url)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0x12000000, dp(24))
        }
        column.addView(address, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(6) })

        val web = webView(head)
        column.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val manager = button("☷")
        val incognito = button(if (head.tab.incognito) "●" else "○")
        val desktop = button(if (head.tab.desktopSite) "D" else "d")
        val night = button(if (head.tab.nightMode) "☾" else "☼")
        footer.addView(manager, buttonParams())
        footer.addView(incognito, buttonParams())
        footer.addView(desktop, buttonParams())
        footer.addView(night, buttonParams())
        column.addView(footer)

        back.setOnClickListener { if (web.canGoBack()) web.goBack() }
        forward.setOnClickListener { if (web.canGoForward()) web.goForward() }
        reload.setOnClickListener { web.reload() }
        close.setOnClickListener { minimize() }
        manager.setOnClickListener { openManager() }
        incognito.setOnClickListener {
            head.tab.incognito = !head.tab.incognito
            persist()
            expandTab(head)
        }
        desktop.setOnClickListener {
            head.tab.desktopSite = !head.tab.desktopSite
            applyDesktop(web, head.tab.desktopSite)
            web.reload()
            persist()
        }
        night.setOnClickListener {
            head.tab.nightMode = !head.tab.nightMode
            applyNight(web, head.tab.nightMode)
            persist()
        }
        address?.setOnEditorActionListener { _, _, _ ->
            navigate(head, address?.text?.toString().orEmpty())
            true
        }

        val p = head.params ?: overlayParams(expandedWidth(), expandedHeight(), true)
        p.width = expandedWidth()
        p.height = expandedHeight()
        p.x = (screenWidth() - p.width) / 2
        p.y = expandedTop()
        p.flags = expandedFlags()
        head.params = p
        expandedRoot = root
        expandedParams = p
        try { wm.updateViewLayout(root, p) } catch (_: Exception) {}

        if (head.tab.url == NEW_TAB_URL) showNewTab(web)
        else if (web.url != head.tab.url) web.loadUrl(head.tab.url)
    }

    private fun openManager() {
        closeExpanded(false)
        val head = managerHead ?: return
        val root = head.view as? FrameLayout ?: return
        activeId = MANAGER_ID
        expandedId = MANAGER_ID
        head.expanded = true
        root.setOnTouchListener(null)
        root.removeAllViews()
        root.background = rounded(0xFFFDFBFF.toInt(), dp(28))

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val header = TextView(this).apply {
            text = "Browser Manager"
            textSize = 22f
            setTextColor(0xFF262229.toInt())
        }
        bar.addView(header, LinearLayout.LayoutParams(0, dp(50), 1f))
        val add = button("+")
        val close = button("×")
        bar.addView(add, buttonParams())
        bar.addView(close, buttonParams())
        column.addView(bar)

        val sub = TextView(this).apply {
            text = "Active tabs • ${tabs.size}/$MAX_TABS"
            textSize = 14f
            setTextColor(0xFF706A74.toInt())
        }
        column.addView(sub)

        val scroll = ScrollView(this)
        managerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        scroll.addView(managerList)
        column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        add.setOnClickListener {
            addTab(DEFAULT_URL, false)
            updateManager()
        }
        close.setOnClickListener { minimize() }

        val p = head.params ?: overlayParams(expandedWidth(), expandedHeight(), true)
        p.width = expandedWidth()
        p.height = expandedHeight()
        p.x = (screenWidth() - p.width) / 2
        p.y = expandedTop()
        p.flags = expandedFlags()
        head.params = p
        expandedRoot = root
        expandedParams = p
        try { wm.updateViewLayout(root, p) } catch (_: Exception) {}
        updateManager()
    }

    private fun updateManager() {
        val list = managerList ?: return
        list.removeAllViews()
        tabs.values.forEach { head ->
            val row = LinearLayout(this)
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(10), dp(8), dp(6), dp(8))
            row.background = rounded(0x0A000000, dp(20))

            val icon = ImageView(this)
            icon.setImageResource(R.drawable.ic_halo_browser)
            head.tab.favicon?.let {
                try {
                    val b = Base64.getDecoder().decode(it)
                    android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size)?.let(icon::setImageBitmap)
                } catch (_: Exception) {}
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))

            val texts = LinearLayout(this)
            texts.orientation = LinearLayout.VERTICAL
            val t = TextView(this).apply {
                text = head.tab.title.ifBlank { head.tab.url }
                textSize = 15f
                setTextColor(0xFF29252D.toInt())
                maxLines = 1
            }
            val u = TextView(this).apply {
                text = head.tab.url
                textSize = 12f
                setTextColor(0xFF77717A.toInt())
                maxLines = 1
            }
            texts.addView(t)
            texts.addView(u)
            row.addView(texts, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(10) })

            val remove = button("×")
            row.addView(remove, buttonParams())
            row.setOnClickListener { openBubble(head.tab.id) }
            remove.setOnClickListener { closeTab(head.tab.id) }

            list.addView(row, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(8) })
        }
    }

    private fun webView(head: Head): WebView {
        head.web?.let { return it }
        val web = WebView(this)
        web.setBackgroundColor(if (head.tab.nightMode) 0xFF121212.toInt() else Color.WHITE)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            allowFileAccess = false
            allowContentAccess = false
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(true)
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = if (head.tab.incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        }

        if (head.tab.incognito) {
            CookieManager.getInstance().setAcceptCookie(false)
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                head.tab.url = if (url.isBlank()) NEW_TAB_URL else url
                head.tab.title = view.title.orEmpty()
                address?.setText(if (head.tab.url == NEW_TAB_URL) "" else head.tab.url)
                address?.setSelection(address?.text?.length ?: 0)
                title?.text = head.tab.title.ifBlank { "Halo Browser" }
                if (!head.tab.incognito && head.tab.url != NEW_TAB_URL) data.addHistory(head.tab.url, head.tab.title)
                persist()
                updateManager()
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!data.adBlockEnabled) return super.shouldInterceptRequest(view, request)
                val host = request.url.host?.lowercase().orEmpty()
                if (blocked(host)) return WebResourceResponse("text/plain", "UTF-8", null)
                return super.shouldInterceptRequest(view, request)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, value: String) {
                head.tab.title = value
                title?.text = value.ifBlank { "Halo Browser" }
                persist()
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                val out = ByteArrayOutputStream()
                icon.compress(Bitmap.CompressFormat.PNG, 90, out)
                head.tab.favicon = Base64.getEncoder().encodeToString(out.toByteArray())
                head.icon?.setImageBitmap(icon)
                persist()
                updateManager()
            }
        }

        applyDesktop(web, head.tab.desktopSite)
        applyNight(web, head.tab.nightMode)
        head.web = web
        return web
    }

    private fun navigate(head: Head, value: String) {
        val target = normalizeUrl(value)
        head.tab.url = target
        persist()
        head.web?.loadUrl(target)
    }

    private fun browserContainer(): FrameLayout {
        return FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(0xFFFDFBFF.toInt())
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), 0x22000000)
            }
        }
    }

    private fun showNewTab(web: WebView) {
        val html = "<html><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><body style=\"font-family:sans-serif;text-align:center;padding:60px;background:#fdfbff\"><h1>Halo Browser</h1><p>New tab</p></body></html>"
        web.loadDataWithBaseURL("https://halo.local/", html, "text/html", "UTF-8", null)
    }

    private fun applyDesktop(web: WebView, enabled: Boolean) {
        web.settings.userAgentString = if (enabled) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/140.0 Safari/537.36"
        } else WebSettings.getDefaultUserAgent(this)
    }

    private fun applyNight(web: WebView, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) web.settings.isAlgorithmicDarkeningAllowed = enabled
        web.setBackgroundColor(if (enabled) 0xFF121212.toInt() else Color.WHITE)
    }

    private fun closeExpanded(restore: Boolean) {
        val id = expandedId ?: return
        val head = if (id == MANAGER_ID) managerHead else tabs[id]
        if (head != null) {
            collapseHead(head)
            if (restore) head.view?.visibility = View.VISIBLE
        }
        expandedRoot = null
        expandedParams = null
        expandedId = null
        address = null
        title = null
        managerList = null
    }

    private fun removeHead(head: Head) {
        head.view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        head.web?.destroy()
        head.view = null
        head.web = null
        head.icon = null
    }

    private fun updatePosition(head: Head) {
        head.params?.let {
            it.x = head.tab.x
            it.y = head.tab.y
            try { head.view?.let { view -> wm.updateViewLayout(view, it) } } catch (_: Exception) {}
        }
    }

    private fun collapseHead(head: Head) {
        val root = head.view as? FrameLayout ?: return
        head.expanded = false
        root.removeAllViews()
        root.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                if (head.manager) 0xFF6750A4.toInt()
                else if (head.tab.incognito) 0xFF29272D.toInt()
                else 0xFFF5F1F8.toInt()
            )
            setStroke(dp(1), 0x33000000)
        }
        root.elevation = dp(8).toFloat()
        head.icon?.let { icon ->
            icon.setImageResource(R.drawable.ic_halo_browser)
            icon.setPadding(dp(8), dp(8), dp(8), dp(8))
            head.tab.favicon?.let {
                try {
                    val bytes = Base64.getDecoder().decode(it)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let(icon::setImageBitmap)
                } catch (_: Exception) {}
            }
            root.addView(icon, FrameLayout.LayoutParams(-1, -1))
        }
        root.setOnTouchListener(touch(head))
        val p = head.params ?: overlayParams(bubbleSize(), bubbleSize(), false)
        p.width = bubbleSize()
        p.height = bubbleSize()
        p.x = head.tab.x.coerceIn(0, max(0, screenWidth() - bubbleSize()))
        p.y = head.tab.y.coerceIn(dp(8), max(dp(8), screenHeight() - bubbleSize() - dp(8)))
        p.flags = bubbleFlags()
        head.params = p
        try { wm.updateViewLayout(root, p) } catch (_: Exception) {}
    }

    private fun bubbleFlags(): Int =
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

    private fun expandedFlags(): Int =
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

    private fun expandedWidth(): Int =
        (screenWidth() * 0.82f).toInt().coerceAtLeast(dp(300))

    private fun expandedHeight(): Int =
        (screenHeight() * 0.58f).toInt().coerceAtLeast(dp(380))

    private fun expandedTop(): Int = dp(72)

    private fun nextBubblePosition(): Pair<Int, Int> {
        val index = tabs.size
        val sideLeft = index % 2 == 0
        val row = index / 2
        val y = (dp(110) + row * dp(82))
            .coerceAtMost(screenHeight() - bubbleSize() - dp(80))
        val x = if (sideLeft) dp(6) else screenWidth() - bubbleSize() - dp(6)
        return x to y
    }

    private fun clamp(p: WindowManager.LayoutParams) {
        p.x = p.x.coerceIn(0, max(0, screenWidth() - bubbleSize()))
        p.y = p.y.coerceIn(dp(8), max(dp(8), screenHeight() - bubbleSize() - dp(8)))
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return
        val v = TextView(this).apply {
            text = "↓"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF2B2930.toInt())
                setStroke(dp(2), Color.WHITE)
            }
        }
        closeTarget = v
        val p = overlayParams(dp(76), dp(76), false)
        p.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        p.y = dp(18)
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        wm.addView(v, p)
    }

    private fun setCloseTargetArmed(armed: Boolean) {
        val v = closeTarget as? TextView ?: return
        v.text = if (armed) "×" else "↓"
        v.textSize = if (armed) 34f else 28f
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (armed) 0xFFB3261E.toInt() else 0xFF2B2930.toInt())
            setStroke(dp(if (armed) 3 else 2), Color.WHITE)
        }
    }

    private fun isBubbleOverCloseTarget(head: Head): Boolean {
        val p = head.params ?: return false
        val left = (screenWidth() - dp(76)) / 2
        val top = screenHeight() - dp(18) - dp(76)
        val right = left + dp(76)
        val bottom = top + dp(76)
        return p.x < right && p.x + bubbleSize() > left &&
            p.y < bottom && p.y + bubbleSize() > top
    }

    private fun hideCloseTarget() {
        closeTarget?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        closeTarget = null
    }

    private fun button(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFF514C55.toInt())
            background = rounded(0x00000000, dp(21))
        }
    }

    private fun buttonParams() = LinearLayout.LayoutParams(dp(42), dp(42))

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun overlayParams(width: Int, height: Int, focusable: Boolean): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            if (focusable) expandedFlags() else bubbleFlags(),
            android.graphics.PixelFormat.TRANSLUCENT
        )
    }

    private fun bubbleSize() = dp(56)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels

    private fun blocked(host: String): Boolean {
        val list = arrayOf("doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "adnxs.com", "amazon-adsystem.com", "unityads.com")
        return list.any { host == it || host.endsWith("." + it) }
    }

    private fun normalizeUrl(value: String): String {
        val input = value.trim()
        if (input.isEmpty()) return NEW_TAB_URL
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(" ") || !input.contains(".")) return data.searchEngine + android.net.Uri.encode(input)
        return "https://" + input
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Halo Browser Floating Service", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun startAsForeground() {
        val pi = PendingIntent.getActivity(this, 100, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
            startForeground(901, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(901, notification)
        }
    }

    private data class Head(
        val tab: BrowserTab,
        val manager: Boolean = false,
        var view: View? = null,
        var params: WindowManager.LayoutParams? = null,
        var web: WebView? = null,
        var icon: ImageView? = null,
        var expanded: Boolean = false
    )

    data class BubbleInfo(
        val id: String,
        val url: String,
        val title: String,
        val incognito: Boolean,
        val isManager: Boolean
    )

    companion object {
        const val ACTION_ADD_BUBBLE = "com.aldiandrew.halobrowser.action.ADD_BUBBLE"
        const val ACTION_OPEN_BUBBLE = "com.aldiandrew.halobrowser.action.OPEN_BUBBLE"
        const val EXTRA_URL = "halo_browser_url"
        const val EXTRA_BUBBLE_ID = "halo_browser_bubble_id"
        const val MANAGER_ID = "browser_manager"
        const val TAB_ID = "browser_tab_default"
        private const val DEFAULT_URL = "https://www.google.com"
        private const val NEW_TAB_URL = "about:blank"
        private const val CHANNEL_ID = "halo_floating_service"
        private const val MAX_TABS = 4
    }
}
