package com.aldiandrew.halobrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.max

class FloatingBubbleService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences

    private val bubbles = LinkedHashMap<Int, BubbleState>()
    private var nextId = 1
    private var activeId = TAB_ID
    private var expanded = false

    private var browserRoot: FrameLayout? = null
    private var browserParams: WindowManager.LayoutParams? = null
    private var closeTarget: View? = null
    private var addressField: EditText? = null
    private var titleView: TextView? = null
    private var activeWebView: WebView? = null

    private val binder = LocalBinder()

    inner class LocalBinder : android.os.Binder() {
        fun service(): FloatingBubbleService = this@FloatingBubbleService
    }

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WindowManager::class.java)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        nextId = prefs.getInt(KEY_NEXT_ID, 1)
        restoreTabs()

        bubbles.values.forEach {
            createBubbleView(it)
            val web = getOrCreateWebView(it)
            if (it.url != NEW_TAB_URL && it.url.isNotBlank()) {
                web.loadUrl(it.url)
            } else {
                showNewTabPage(web)
            }
        }
        ensureFirstTab()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        when (intent?.action) {
            ACTION_ADD_BUBBLE -> {
                addTab(
                    intent.getStringExtra(EXTRA_URL)
                        ?: DEFAULT_URL,
                    open = true
                )
            }

            ACTION_OPEN_BUBBLE -> {
                openBubble(
                    intent.getIntExtra(
                        EXTRA_BUBBLE_ID,
                        TAB_ID
                    )
                )
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        closeBrowserWindow()
        hideCloseTarget()

        bubbles.values.toList().forEach { bubble ->
            bubble.webView?.stopLoading()
            bubble.webView?.destroy()
            bubble.view?.let {
                try {
                    wm.removeView(it)
                } catch (_: Exception) {
                }
            }
        }

        bubbles.clear()
        super.onDestroy()
    }

    fun maximize() {
        if (activeId == MANAGER_ID) {
            activeId = bubbles.keys.firstOrNull { it != MANAGER_ID }
                ?: TAB_ID
        }

        expanded = true
        showBrowserWindow(activeId)
    }

    fun minimize() {
        expanded = false
        closeBrowserWindow()
        arrangeBubbles()
    }

    fun addTab(
        url: String,
        open: Boolean
    ): Int {
        val id = nextId++

        val state = BubbleState(
            id = id,
            url = url.ifBlank { NEW_TAB_URL },
            kind = BubbleKind.TAB
        )

        bubbles[id] = state
        persistTabs()
        createBubbleView(state)

        val web = getOrCreateWebView(state)
        if (state.url != NEW_TAB_URL && state.url.isNotBlank()) {
            web.loadUrl(state.url)
        } else {
            showNewTabPage(web)
        }

        if (open) {
            activeId = id
            showBrowserWindow(id)
        } else {
            arrangeBubbles()
        }

        return id
    }

    fun openBubble(id: Int) {
        val bubble = bubbles[id] ?: return

        activeId = bubble.id
        expanded = true
        showBrowserWindow(bubble.id)
    }

    fun closeTab(id: Int) {
        if (id == MANAGER_ID) return

        val bubble = bubbles.remove(id) ?: return

        if (activeId == id) {
            closeBrowserWindow()
            expanded = false
        }

        bubble.webView?.stopLoading()
        bubble.webView?.destroy()

        bubble.view?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }

        persistTabs()

        if (bubbles.keys.none { it != MANAGER_ID }) {
            addTab(NEW_TAB_URL, open = false)
        }

        activeId =
            bubbles.keys.firstOrNull { it != MANAGER_ID }
                ?: TAB_ID

        arrangeBubbles()
    }

    fun bubbleSnapshot(): List<BubbleInfo> =
        bubbles.values.map {
            BubbleInfo(
                id = it.id,
                url = it.url,
                isManager = it.kind == BubbleKind.MANAGER
            )
        }

    fun urlFor(id: Int): String? = bubbles[id]?.url

    fun updateUrl(
        id: Int,
        url: String
    ) {
        bubbles[id]?.url = url
        persistTabs()
    }

    private fun ensureFirstTab() {
        if (bubbles.keys.any { it != MANAGER_ID }) {
            return
        }

        addTab(
            NEW_TAB_URL,
            open = false
        )
    }

    private fun restoreTabs() {
        val ids =
            prefs.getStringSet(
                KEY_TAB_IDS,
                emptySet()
            ) ?: emptySet()

        ids.mapNotNull { it.toIntOrNull() }
            .sorted()
            .forEach { id ->
                val url =
                    prefs.getString(
                        "url_" + id,
                        NEW_TAB_URL
                    ) ?: NEW_TAB_URL

                bubbles[id] =
                    BubbleState(
                        id = id,
                        url = url,
                        kind = BubbleKind.TAB
                    )

                nextId = max(
                    nextId,
                    id + 1
                )
            }
    }

    private fun persistTabs() {
        val ids =
            bubbles.values
                .filter {
                    it.kind == BubbleKind.TAB
                }
                .map {
                    it.id.toString()
                }
                .toSet()

        val editor =
            prefs.edit()
                .putStringSet(
                    KEY_TAB_IDS,
                    ids
                )
                .putInt(
                    KEY_NEXT_ID,
                    nextId
                )

        bubbles.values
            .filter {
                it.kind == BubbleKind.TAB
            }
            .forEach {
                editor.putString(
                    "url_" + it.id,
                    it.url
                )
            }

        editor.apply()
    }

    private fun createBubbleView(
        bubble: BubbleState
    ) {
        if (bubble.view != null) {
            return
        }

        val size = dp(64)

        val root =
            FrameLayout(this).apply {
                contentDescription =
                    "Halo Browser tab " + bubble.id

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.OVAL
                        setColor(
                            0xFFF1EFFF.toInt()
                        )
                        setStroke(
                            dp(2),
                            0xFF6650A4.toInt()
                        )
                    }

                elevation = dp(8).toFloat()
            }

        val icon =
            TextView(this).apply {
                text = "●"
                textSize = 25f
                setTextColor(
                    0xFF6650A4.toInt()
                )
                gravity = Gravity.CENTER
            }

        root.addView(
            icon,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        bubble.view = root

        bubble.params =
            overlayParams(
                size,
                size,
                focusable = false
            ).apply {
                gravity =
                    Gravity.TOP or Gravity.START
            }

        root.setOnTouchListener(
            makeBubbleDragListener(
                bubble,
                size
            )
        )

        wm.addView(
            root,
            bubble.params
        )
    }

    private fun makeBubbleDragListener(
        bubble: BubbleState,
        size: Int
    ): View.OnTouchListener {
        var startRawX = 0f
        var startRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        return View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startX =
                        bubble.params?.x ?: 0
                    startY =
                        bubble.params?.y ?: 0
                    moved = false
                    showCloseTarget()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx =
                        (event.rawX - startRawX)
                            .toInt()
                    val dy =
                        (event.rawY - startRawY)
                            .toInt()

                    if (
                        abs(dx) > dp(6) ||
                        abs(dy) > dp(6)
                    ) {
                        moved = true
                    }

                    bubble.params?.let { params ->
                        params.x =
                            startX + dx
                        params.y =
                            startY + dy

                        clamp(
                            params,
                            size
                        )

                        try {
                            wm.updateViewLayout(
                                bubble.view!!,
                                params
                            )
                        } catch (_: Exception) {
                        }
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val overClose =
                        isOverCloseTarget(
                            event.rawX,
                            event.rawY
                        )

                    hideCloseTarget()

                    when {
                        !moved -> {
                            openBubble(
                                bubble.id
                            )
                        }

                        overClose -> {
                            closeTab(
                                bubble.id
                            )
                        }

                        else -> {
                            snapSingle(
                                bubble,
                                size
                            )
                        }
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun showBrowserWindow(
        id: Int
    ) {
        val bubble =
            bubbles[id] ?: return

        closeBrowserWindow()

        activeId = id

        val root =
            FrameLayout(this).apply {
                background =
                    roundedBackground(
                        0xFFFDFBFF.toInt(),
                        dp(24)
                    )
                elevation = dp(18).toFloat()
            }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(10),
                    dp(8),
                    dp(8),
                    dp(8)
                )
                background =
                    roundedBackground(
                        0xFFF1EFFF.toInt(),
                        dp(24)
                    )
            }

        val title =
            TextView(this).apply {
                text =
                    "Halo Browser"
                textSize = 15f
                setTextColor(
                    0xFF26242A.toInt()
                )
                maxLines = 1
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
                gravity =
                    Gravity.CENTER_VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1f
                    )
            }

        titleView = title

        val minimize =
            createHeaderButton(
                "−"
            )

        val newTab =
            createHeaderButton(
                "+"
            )

        val close =
            createHeaderButton(
                "×"
            )

        val back = createHeaderButton("‹")
        val forward = createHeaderButton("›")
        val refresh = createHeaderButton("↻")

        header.addView(back, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(forward, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(refresh, LinearLayout.LayoutParams(dp(42), dp(42)))

        header.addView(title)

        header.addView(
            newTab,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        header.addView(
            minimize,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        header.addView(
            close,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        root.addView(
            header,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                gravity = Gravity.TOP
            }
        )

        val address =
            EditText(this).apply {
                setSingleLine(true)
                textSize = 14f
                setText(
                    if (
                        bubble.url == NEW_TAB_URL
                    ) {
                        ""
                    } else {
                        bubble.url
                    }
                )
                hint =
                    "Search or enter URL"
                setPadding(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
                background =
                    roundedBackground(
                        0xFFF1EFFF.toInt(),
                        dp(18)
                    )
                layoutParams =
                    FrameLayout.LayoutParams(
                        0,
                        dp(46)
                    ).apply {
                        leftMargin = dp(10)
                        rightMargin = dp(10)
                        topMargin = dp(66)
                    }
            }

        addressField = address
        root.addView(address)

        val web =
            getOrCreateWebView(
                bubble
            )

        activeWebView = web

        val webParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                leftMargin = dp(8)
                rightMargin = dp(8)
                topMargin = dp(120)
                bottomMargin = dp(10)
            }

        root.addView(
            web,
            webParams
        )

        val resize =
            TextView(this).apply {
                text = "⌟"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(
                    0xFF6650A4.toInt()
                )
                background =
                    roundedBackground(
                        0x22FFFFFF,
                        dp(12)
                    )
            }

        root.addView(
            resize,
            FrameLayout.LayoutParams(
                dp(42),
                dp(42)
            ).apply {
                gravity =
                    Gravity.BOTTOM or
                        Gravity.END
                rightMargin = dp(6)
                bottomMargin = dp(6)
            }
        )

        back.setOnClickListener {
            if (web.canGoBack()) web.goBack()
        }

        forward.setOnClickListener {
            if (web.canGoForward()) web.goForward()
        }

        refresh.setOnClickListener {
            web.reload()
        }

        newTab.setOnClickListener {
            addTab(
                NEW_TAB_URL,
                open = true
            )
        }

        minimize.setOnClickListener {
            minimize()
        }

        close.setOnClickListener {
            closeTab(id)
        }

        address.setOnEditorActionListener { _, _, _ ->
            navigate(
                bubble,
                address.text.toString()
            )
            true
        }

        header.setOnTouchListener(
            makeWindowDragListener(
                root
            )
        )

        resize.setOnTouchListener(
            makeResizeListener(
                root
            )
        )

        configureWebView(
            web,
            bubble
        )

        val width =
            (screenWidth() * 0.92f)
                .toInt()
                .coerceAtLeast(
                    dp(280)
                )

        val height =
            (screenHeight() * 0.70f)
                .toInt()
                .coerceAtLeast(
                    dp(360)
                )

        val params =
            overlayParams(
                width,
                height,
                focusable = true
            ).apply {
                gravity =
                    Gravity.TOP or
                        Gravity.START
                x =
                    (screenWidth() - width) / 2
                y =
                    dp(100)
            }

        browserRoot = root
        browserParams = params

        wm.addView(
            root,
            params
        )

        expanded = true

        if (
            bubble.url != NEW_TAB_URL &&
            bubble.url.isNotBlank()
        ) {
            if (
                web.url != bubble.url
            ) {
                web.loadUrl(
                    bubble.url
                )
            }
        } else {
            showNewTabPage(web)
        }
    }

    private fun configureWebView(
        web: WebView,
        bubble: BubbleState
    ) {
        web.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String
                ) {
                    super.onPageFinished(view, url)

                    bubble.url =
                        if (url.isBlank()) NEW_TAB_URL else url

                    addressField?.let { field ->
                        if (activeWebView === view) {
                            field.setText(
                                if (bubble.url == NEW_TAB_URL) "" else bubble.url
                            )
                            field.setSelection(field.text.length)
                        }
                    }

                    titleView?.let { title ->
                        if (activeWebView === view) {
                            title.text =
                                view.title?.takeIf { it.isNotBlank() }
                                    ?: "Halo Browser"
                        }
                    }

                    persistTabs()
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val host = request.url.host?.lowercase() ?: ""
                    if (isBlockedHost(host)) {
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            null
                        )
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false
            }
    }

    private fun isBlockedHost(host: String): Boolean {
        if (host.isBlank()) return false

        val blocked = arrayOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adsystem.com",
            "adnxs.com",
            "amazon-adsystem.com",
            "adcolony.com",
            "unityads.com",
            "appsflyer.com",
            "facebook.net"
        )

        return blocked.any {
            host == it || host.endsWith("." + it)
        }
    }

    private fun getOrCreateWebView(
        bubble: BubbleState
    ): WebView {
        bubble.webView?.let {
            return it
        }

        val web =
            WebView(this).apply {
                setBackgroundColor(
                    Color.WHITE
                )

                settings.javaScriptEnabled =
                    true
                settings.domStorageEnabled =
                    true
                settings.loadsImagesAutomatically =
                    true
                settings.allowFileAccess =
                    false
                settings.allowContentAccess =
                    false
                settings.setSupportZoom(
                    true
                )
                settings.builtInZoomControls =
                    false
                settings.displayZoomControls =
                    false
                settings.javaScriptCanOpenWindowsAutomatically =
                    true
                settings.mediaPlaybackRequiresUserGesture =
                    false
                settings.cacheMode =
                    WebSettings.LOAD_DEFAULT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webChromeClient =
                    WebChromeClient()
            }

        CookieManager.getInstance().setAcceptCookie(true)

        configureWebView(web, bubble)

        bubble.webView = web
        return web
    }

    private fun showNewTabPage(
        web: WebView
    ) {
        val html =
            """
            <!doctype html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
            body{font-family:sans-serif;background:#fdfbff;color:#26242a;margin:0;padding:32px;text-align:center}
            h1{font-size:28px;margin-top:55px}
            p{color:#6f6a75}
            </style>
            </head>
            <body>
            <h1>Halo Browser</h1>
            <p>New tab</p>
            </body>
            </html>
            """.trimIndent()

        web.loadDataWithBaseURL(
            "https://halo.local/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun navigate(
        bubble: BubbleState,
        value: String
    ) {
        val target =
            normalizeUrl(
                value
            )

        bubble.url = target
        persistTabs()

        activeWebView?.loadUrl(
            target
        )
    }

    private fun makeWindowDragListener(
        root: View
    ): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        return View.OnTouchListener { _, event ->
            val params =
                browserParams
                    ?: return@OnTouchListener false

            when (
                event.actionMasked
            ) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx =
                        (event.rawX - downX)
                            .toInt()
                    val dy =
                        (event.rawY - downY)
                            .toInt()

                    if (
                        abs(dx) > dp(4) ||
                        abs(dy) > dp(4)
                    ) {
                        moved = true
                    }

                    params.x =
                        startX + dx
                    params.y =
                        startY + dy

                    clampWindow(
                        params
                    )

                    try {
                        wm.updateViewLayout(
                            root,
                            params
                        )
                    } catch (_: Exception) {
                    }

                    true
                }

                MotionEvent.ACTION_UP -> moved

                else -> true
            }
        }
    }

    private fun makeResizeListener(
        root: View
    ): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startWidth = 0
        var startHeight = 0

        return View.OnTouchListener { _, event ->
            val params =
                browserParams
                    ?: return@OnTouchListener false

            when (
                event.actionMasked
            ) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startWidth = params.width
                    startHeight = params.height
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.width =
                        (
                            startWidth +
                                event.rawX -
                                downX
                            ).toInt().coerceIn(
                                dp(280),
                                screenWidth() -
                                    dp(16)
                            )

                    params.height =
                        (
                            startHeight +
                                event.rawY -
                                downY
                            ).toInt().coerceIn(
                                dp(360),
                                screenHeight() -
                                    dp(24)
                            )

                    try {
                        wm.updateViewLayout(
                            root,
                            params
                        )
                    } catch (_: Exception) {
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> true

                else -> true
            }
        }
    }

    private fun closeBrowserWindow() {
        val root = browserRoot
        val web = activeWebView

        if (root != null && web != null && web.parent === root) {
            try {
                root.removeView(web)
            } catch (_: Exception) {
            }
        }

        root?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }

        browserRoot = null
        browserParams = null
        addressField = null
        titleView = null
        activeWebView = null
    }

    private fun arrangeBubbles() {
        val tabs =
            bubbles.values
                .filter {
                    it.kind == BubbleKind.TAB
                }

        if (tabs.isEmpty()) {
            return
        }

        val size = dp(64)
        val gap = dp(10)

        tabs.forEachIndexed { index, bubble ->
            val x =
                if (
                    index % 2 == 0
                ) {
                    screenWidth() -
                        size -
                        dp(10)
                } else {
                    dp(10)
                }

            val y =
                dp(150) +
                    (index / 2) *
                    (size + gap)

            animateBubbleTo(
                bubble,
                x,
                y
            )
        }
    }

    private fun animateBubbleTo(
        bubble: BubbleState,
        x: Int,
        y: Int
    ) {
        val params =
            bubble.params
                ?: return

        params.x = x
        params.y = y

        try {
            wm.updateViewLayout(
                bubble.view!!,
                params
            )
        } catch (_: Exception) {
        }
    }

    private fun snapSingle(
        bubble: BubbleState,
        size: Int
    ) {
        val params =
            bubble.params
                ?: return

        params.x =
            if (
                params.x +
                    size / 2 <
                screenWidth() / 2
            ) {
                dp(6)
            } else {
                screenWidth() -
                    size -
                    dp(6)
            }

        clamp(
            params,
            size
        )

        try {
            wm.updateViewLayout(
                bubble.view!!,
                params
            )
        } catch (_: Exception) {
        }
    }

    private fun showCloseTarget() {
        if (closeTarget != null) {
            return
        }

        val size = dp(72)

        val target =
            FrameLayout(this).apply {
                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.OVAL
                        setColor(
                            0xFF3A3540.toInt()
                        )
                        setStroke(
                            dp(2),
                            0xFFFFFFFF.toInt()
                        )
                    }
            }

        val params =
            overlayParams(
                size,
                size,
                focusable = false
            ).apply {
                gravity =
                    Gravity.BOTTOM or
                        Gravity.CENTER_HORIZONTAL
                y = dp(28)
            }

        closeTarget = target

        wm.addView(
            target,
            params
        )
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

    private fun isOverCloseTarget(
        x: Float,
        y: Float
    ): Boolean {
        val target =
            closeTarget
                ?: return false

        val location =
            IntArray(2)

        target.getLocationOnScreen(
            location
        )

        val centerX =
            location[0] +
                target.width / 2f

        val centerY =
            location[1] +
                target.height / 2f

        val radius =
            max(
                target.width,
                target.height
            ) / 2f +
                dp(18)

        val dx =
            x - centerX

        val dy =
            y - centerY

        return dx * dx +
            dy * dy <=
            radius * radius
    }

    private fun clamp(
        params: WindowManager.LayoutParams,
        size: Int
    ) {
        params.x =
            params.x.coerceIn(
                0,
                max(
                    0,
                    screenWidth() - size
                )
            )

        params.y =
            params.y.coerceIn(
                dp(8),
                max(
                    dp(8),
                    screenHeight() -
                        size -
                        dp(8)
                )
            )
    }

    private fun clampWindow(
        params: WindowManager.LayoutParams
    ) {
        params.x =
            params.x.coerceIn(
                0,
                max(
                    0,
                    screenWidth() -
                        params.width
                )
            )

        params.y =
            params.y.coerceIn(
                dp(8),
                max(
                    dp(8),
                    screenHeight() -
                        params.height
                )
            )
    }

    private fun overlayParams(
        width: Int,
        height: Int,
        focusable: Boolean
    ) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            if (focusable) {
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            } else {
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            },
            PixelFormat.TRANSLUCENT
        )

    private fun createHeaderButton(
        text: String
    ): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(
                0xFF514C55.toInt()
            )
            background =
                roundedBackground(
                    0x00FFFFFF,
                    dp(21)
                )
            isClickable = true
        }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius =
                radius.toFloat()
        }

    private fun screenWidth(): Int =
        resources.displayMetrics.widthPixels

    private fun screenHeight(): Int =
        resources.displayMetrics.heightPixels

    private fun dp(value: Int): Int =
        (
            value *
                resources.displayMetrics.density
        ).toInt()

    private fun createNotificationChannel() {
        getSystemService(
            NotificationManager::class.java
        ).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Halo Browser Floating Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "Keeps Halo Browser bubbles available"
                setShowBadge(false)
            }
        )
    }

    private fun startAsForeground() {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                100,
                Intent(
                    this,
                    MainActivity::class.java
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_halo_browser
                )
                .setContentTitle(
                    "Halo Browser"
                )
                .setContentText(
                    "Floating browser is active"
                )
                .setContentIntent(
                    pendingIntent
                )
                .setOngoing(true)
                .setSilent(true)
                .setCategory(
                    Notification.CATEGORY_SERVICE
                )
                .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                FOREGROUND_ID,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                FOREGROUND_ID,
                notification
            )
        }
    }

    private fun normalizeUrl(
        value: String
    ): String {
        val input =
            value.trim()

        if (input.isEmpty()) {
            return NEW_TAB_URL
        }

        if (
            input.startsWith(
                "http://"
            ) ||
            input.startsWith(
                "https://"
            )
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

    private data class BubbleState(
        val id: Int,
        var url: String,
        val kind: BubbleKind,
        var view: View? = null,
        var params:
            WindowManager.LayoutParams? = null,
        var webView: WebView? = null
    )

    private enum class BubbleKind {
        TAB,
        MANAGER
    }

    data class BubbleInfo(
        val id: Int,
        val url: String,
        val isManager: Boolean
    )

    companion object {
        const val ACTION_ADD_BUBBLE =
            "com.aldiandrew.halobrowser.action.ADD_BUBBLE"

        const val ACTION_OPEN_BUBBLE =
            "com.aldiandrew.halobrowser.action.OPEN_BUBBLE"

        const val EXTRA_URL =
            "halo_browser_url"

        const val EXTRA_BUBBLE_ID =
            "halo_browser_bubble_id"

        const val MANAGER_ID = 0
        const val TAB_ID = 1

        private const val DEFAULT_URL =
            "https://www.google.com"

        private const val NEW_TAB_URL =
            "about:blank"

        private const val PREFS =
            "halo_bubbles"

        private const val KEY_TAB_IDS =
            "tab_ids"

        private const val KEY_NEXT_ID =
            "next_id"

        private const val CHANNEL_ID =
            "halo_floating_service"

        private const val FOREGROUND_ID = 901
    }
}
