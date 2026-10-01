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
    private var heroId: String? = null

    private enum class HeadState {
        FREE,
        CAPTURED
    }

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
            addTab(intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL, true)
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
            val existing = activeId?.let { tabs[it] } ?: tabs.values.firstOrNull()
            existing?.let {
                heroId = it.tab.id
                if (open) openBubble(it.tab.id)
                return it.tab.id
            }
        }

        val id = "browser_tab_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val tab = BrowserTab(id, normalizeUrl(url))
        tab.x = if (tabs.size % 2 == 0) dp(4) else screenWidth() - bubbleSize() - dp(4)
        tab.y = dp(90) + tabs.size * dp(62)
        val head = Head(tab)
        tabs[id] = head
        addHead(head)
        persist()
        arrange()
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
        heroId = id
        head.view?.visibility = View.INVISIBLE
        closeExpanded(false)
        openTabWindow(head)
        arrange()
    }

    fun closeTab(id: String) {
        val head = tabs.remove(id) ?: return
        if (expandedId == id) closeExpanded(false)
        removeHead(head)
        persist()
        arrange()
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
        activeId?.let { tabs[it]?.let { openTabWindow(it) } }
    }

    fun bubbleSnapshot(): List<BubbleInfo> {
        return tabs.values.map {
            BubbleInfo(it.tab.id, it.tab.url, it.tab.title, it.tab.incognito, false)
        }
    }

    private fun restore() {
        data.loadActiveTabs().forEach {
            if (it.id.isNotBlank()) {
                val head = Head(it)
                tabs[it.id] = head
                addHead(head)
            }
        }
        if (tabs.isEmpty()) addTab(DEFAULT_URL, false)
        arrange()
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
                    head.state = HeadState.CAPTURED
                    downX = event.rawX
                    downY = event.rawY
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
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    p.x = startX + dx.toInt()
                    p.y = startY + dy.toInt()
                    clamp(p)
                    if (isNearCloseTarget(event.rawX, event.rawY)) {
                        view.scaleX = 0.72f
                        view.scaleY = 0.72f
                    } else {
                        view.scaleX = 0.84f
                        view.scaleY = 0.84f
                    }
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

                    if (isOverCloseTarget(event.rawX, event.rawY)) {
                        if (!head.manager) closeTab(head.tab.id)
                        head.state = HeadState.FREE
                        dragging = null
                        return@OnTouchListener true
                    }

                    if (!moved) {
                        head.state = HeadState.FREE
                        if (head.manager) openManager() else openBubble(head.tab.id)
                    } else {
                        head.state = HeadState.FREE
                        heroId = head.tab.id
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
        arrange()
    }

    private fun arrange() {
        val list = tabs.values.filter { it !== dragging }
        val left = list.filter { it.tab.x + bubbleSize() / 2 < screenWidth() / 2 }
            .sortedWith(compareBy<Head> { if (it.tab.id == heroId) 0 else 1 }.thenBy { it.tab.y })
        val right = list.filter { it.tab.x + bubbleSize() / 2 >= screenWidth() / 2 }
            .sortedWith(compareBy<Head> { if (it.tab.id == heroId) 0 else 1 }.thenBy { it.tab.y })

        arrangeSide(left, false)
        arrangeSide(right, true)

        managerHead?.let {
            it.tab.x = screenWidth() - bubbleSize() - dp(8)
            it.tab.y = dp(16)
            updatePosition(it)
        }
    }

    private fun arrangeSide(list: List<Head>, right: Boolean) {
        var previousY = dp(84)
        list.forEachIndexed { index, head ->
            val x = if (right) screenWidth() - bubbleSize() - dp(4) else dp(4)
            val y = min(previousY + if (index == 0) 0 else dp(62), screenHeight() - bubbleSize() - dp(8))
            val chainDamping = if (index == 0) 0.78f else 0.86f
            springTo(head, x, y, 0f, 0f, chainDamping)
            head.tab.x = x
            head.tab.y = y
            previousY = y
        }
        if (list.isNotEmpty()) persist()
    }

    private fun springTo(
        head: Head,
        targetX: Int,
        targetY: Int,
        vx: Float,
        vy: Float,
        dampingRatio: Float = 0.78f
    ) {
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
                dampingRatio = dampingRatio
            }
            setStartVelocity(vx)
        }.start()

        SpringAnimation(p, py).apply {
            spring = SpringForce(targetY.toFloat()).apply {
                stiffness = 700f
                dampingRatio = dampingRatio
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

    private fun openTabWindow(head: Head) {
        closeExpanded(false)
        expandedId = head.tab.id
        activeId = head.tab.id

        val root = browserContainer()
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(8), dp(6), dp(8), dp(8))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL

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
            singleLine = true
            textSize = 14f
            setText(if (head.tab.url == NEW_TAB_URL) "" else head.tab.url)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(0x12000000, dp(24))
        }
        column.addView(address, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(6) })

        val web = webView(head)
        expandedWeb = web
        column.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = LinearLayout(this)
        footer.gravity = Gravity.CENTER_VERTICAL
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
            openTabWindow(head)
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

        val p = overlayParams(
            (screenWidth() * 0.92f).toInt().coerceAtLeast(dp(300)),
            (screenHeight() * 0.72f).toInt().coerceAtLeast(dp(420)),
            true
        )
        p.x = dp(8)
        p.y = dp(50)
        wm.addView(root, p)
        expandedRoot = root
        expandedParams = p

        if (head.tab.url == NEW_TAB_URL) showNewTab(web) else if (web.url != head.tab.url) web.loadUrl(head.tab.url)
    }

    private fun openManager() {
        closeExpanded(false)
        expandedId = MANAGER_ID
        val root = browserContainer()
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(14), dp(12), dp(14), dp(12))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
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
            text = "Active tabs"
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

        add.setOnClickListener { addTab(DEFAULT_URL, true) }
        close.setOnClickListener { closeExpanded(false) }

        val p = overlayParams(
            (screenWidth() * 0.9f).toInt().coerceAtLeast(dp(300)),
            (screenHeight() * 0.72f).toInt().coerceAtLeast(dp(420)),
            true
        )
        p.x = dp(8)
        p.y = dp(50)
        wm.addView(root, p)
        expandedRoot = root
        expandedParams = p
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
        expandedWeb?.loadUrl(target)
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
        val root = expandedRoot ?: return
        try { wm.removeView(root) } catch (_: Exception) {}
        expandedRoot = null
        expandedParams = null
        expandedWeb = null
        address = null
        title = null
        managerList = null

        if (restore) {
            val id = expandedId
            if (id == MANAGER_ID) managerHead?.view?.visibility = View.VISIBLE
            else if (id != null) tabs[id]?.view?.visibility = View.VISIBLE
        }
        expandedId = null
    }

    private fun removeHead(head: Head) {
        head.view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        head.web?.destroy()
        head.view = null
        head.web = null
    }

    private fun updatePosition(head: Head) {
        head.params?.let {
            it.x = head.tab.x
            it.y = head.tab.y
            try { head.view?.let { view -> wm.updateViewLayout(view, it) } } catch (_: Exception) {}
        }
    }

    private fun clamp(p: WindowManager.LayoutParams) {
        p.x = p.x.coerceIn(0, max(0, screenWidth() - bubbleSize()))
        p.y = p.y.coerceIn(dp(8), max(dp(8), screenHeight() - bubbleSize() - dp(8)))
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return
        val v = TextView(this).apply {
            text = "×"
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
        val p = overlayParams(dp(62), dp(62), false)
        p.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        p.y = dp(40)
        wm.addView(v, p)
    }

    private fun hideCloseTarget() {
        closeTarget?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        closeTarget = null
    }

    private fun isNearCloseTarget(x: Float, y: Float): Boolean {
        val target = closeTarget ?: return false
        val pos = IntArray(2)
        target.getLocationOnScreen(pos)
        val cx = pos[0] + target.width / 2f
        val cy = pos[1] + target.height / 2f
        val r = target.width / 2f + dp(72)
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }

    private fun isOverCloseTarget(x: Float, y: Float): Boolean {
        val target = closeTarget ?: return false
        val pos = IntArray(2)
        target.getLocationOnScreen(pos)
        val cx = pos[0] + target.width / 2f
        val cy = pos[1] + target.height / 2f
        val r = target.width / 2f + dp(18)
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= r * r
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

    private fun overlayParams(width: Int, height: Int, focusable: Boolean) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            if (focusable) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )

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
        var state: HeadState = HeadState.FREE
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
