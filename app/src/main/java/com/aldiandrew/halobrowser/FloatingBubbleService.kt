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
import android.animation.ValueAnimator
import android.view.VelocityTracker
import android.view.animation.DecelerateInterpolator
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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.hypot
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

    private val bubbleAnimators =
        mutableMapOf<Int, ValueAnimator>()

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
                    open = false
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

        bubbleAnimators.remove(id)?.cancel()
        val bubble = bubbles.remove(id) ?: return

        if (activeId == id) {
            closeBrowserWindow()
            expanded = false
        }

        bubble.savedX = bubble.params?.x
        bubble.savedY = bubble.params?.y
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

                val savedX =
                    prefs.getInt(
                        "x_" + id,
                        Int.MIN_VALUE
                    )

                val savedY =
                    prefs.getInt(
                        "y_" + id,
                        Int.MIN_VALUE
                    )

                bubbles[id] =
                    BubbleState(
                        id = id,
                        url = url,
                        kind = BubbleKind.TAB,
                        savedX = savedX.takeUnless {
                            it == Int.MIN_VALUE
                        },
                        savedY = savedY.takeUnless {
                            it == Int.MIN_VALUE
                        }
                    )

                nextId =
                    max(
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
            .forEach { bubble ->
                editor.putString(
                    "url_" + bubble.id,
                    bubble.url
                )

                bubble.params?.let { params ->
                    editor.putInt(
                        "x_" + bubble.id,
                        params.x
                    )
                    editor.putInt(
                        "y_" + bubble.id,
                        params.y
                    )
                }
            }

        editor.apply()
    }

    private fun createBubbleView(
        bubble: BubbleState
    ) {
        if (bubble.view != null) {
            return
        }

        val size = dp(56)

        val root =
            FrameLayout(this).apply {
                contentDescription =
                    "Halo Browser tab " + bubble.id

                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFFF7F2FA.toInt())
                        setStroke(
                            dp(2),
                            0xFF6750A4.toInt()
                        )
                    }

                elevation = dp(10).toFloat()
            }

        val icon =
            TextView(this).apply {
                text = "🌐"
                textSize = 20f
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

                x =
                    bubble.savedX
                        ?: dp(8)

                y =
                    bubble.savedY
                        ?: dp(180)
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
        var captured = false
        var velocityTracker: VelocityTracker? = null

        return View.OnTouchListener { _, event ->
            val tracker =
                velocityTracker
                    ?: VelocityTracker.obtain().also {
                        velocityTracker = it
                    }

            tracker.addMovement(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bubbleAnimators
                        .remove(bubble.id)
                        ?.cancel()

                    startRawX = event.rawX
                    startRawY = event.rawY
                    startX =
                        bubble.params?.x ?: 0
                    startY =
                        bubble.params?.y ?: 0

                    moved = false
                    captured = false

                    bubble.view?.animate()
                        ?.scaleX(0.90f)
                        ?.scaleY(0.90f)
                        ?.setDuration(80)
                        ?.start()

                    showCloseTarget()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx =
                        (event.rawX - startRawX).toInt()
                    val dy =
                        (event.rawY - startRawY).toInt()

                    if (
                        abs(dx) > dp(5) ||
                        abs(dy) > dp(5)
                    ) {
                        moved = true
                    }

                    if (!moved) {
                        return@OnTouchListener true
                    }

                    val params =
                        bubble.params
                            ?: return@OnTouchListener true

                    params.x = startX + dx
                    params.y = startY + dy

                    clamp(
                        params,
                        size
                    )

                    val closeCenter =
                        closeTargetCenter()

                    captured =
                        closeCenter?.let { center ->
                            hypot(
                                event.rawX - center.first,
                                event.rawY - center.second
                            ) < dp(110)
                        } == true

                    if (captured) {
                        closeTarget?.animate()
                            ?.scaleX(1.08f)
                            ?.scaleY(1.08f)
                            ?.setDuration(80)
                            ?.start()

                        closeCenter?.let { center ->
                            val tx =
                                (
                                    center.first -
                                        size / 2f
                                ).toInt()

                            val ty =
                                (
                                    center.second -
                                        size / 2f
                                ).toInt()

                            params.x =
                                lerpInt(
                                    params.x,
                                    tx,
                                    0.32f
                                )

                            params.y =
                                lerpInt(
                                    params.y,
                                    ty,
                                    0.32f
                                )
                        }
                    }

                    try {
                        wm.updateViewLayout(
                            bubble.view!!,
                            params
                        )
                    } catch (_: Exception) {
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    tracker.computeCurrentVelocity(1000)

                    val vx =
                        tracker.xVelocity

                    val vy =
                        tracker.yVelocity

                    tracker.recycle()
                    velocityTracker = null

                    hideCloseTarget()

                    bubble.view?.animate()
                        ?.scaleX(1f)
                        ?.scaleY(1f)
                        ?.setDuration(120)
                        ?.start()

                    when {
                        moved && captured ->
                            closeTab(bubble.id)

                        moved ->
                            settleBubble(
                                bubble,
                                size,
                                vx,
                                vy
                            )

                        else ->
                            openBubble(bubble.id)
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun settleBubble(
        bubble: BubbleState,
        size: Int,
        velocityX: Float,
        velocityY: Float
    ) {
        val params =
            bubble.params
                ?: return

        val maxX =
            max(
                0,
                screenWidth() - size
            )

        val targetX =
            if (abs(velocityX) >= dp(50)) {
                if (velocityX > 0f) {
                    maxX - dp(6)
                } else {
                    dp(6)
                }
            } else {
                if (
                    params.x + size / 2 <
                    screenWidth() / 2
                ) {
                    dp(6)
                } else {
                    maxX - dp(6)
                }
            }.coerceIn(
                0,
                maxX
            )

        val targetY =
            (
                params.y +
                    velocityY * 0.06f
            ).toInt().coerceIn(
                dp(8),
                screenHeight() -
                    size -
                    dp(8)
            )

        val startX = params.x
        val startY = params.y

        val distance =
            hypot(
                (targetX - startX).toFloat(),
                (targetY - startY).toFloat()
            )

        val speed =
            max(
                220f,
                hypot(
                    velocityX,
                    velocityY
                )
            )

        val duration =
            (
                170L +
                    distance / speed * 280L
            )
                .toLong()
                .coerceIn(
                    170L,
                    460L
                )

        bubbleAnimators
            .remove(bubble.id)
            ?.cancel()

        val animator =
            ValueAnimator.ofFloat(
                0f,
                1f
            ).apply {
                this.duration = duration
                interpolator =
                    DecelerateInterpolator(1.7f)

                addUpdateListener { animation ->
                    val paramsNow =
                        bubble.params
                            ?: return@addUpdateListener

                    val fraction =
                        animation.animatedFraction

                    paramsNow.x =
                        lerpInt(
                            startX,
                            targetX,
                            fraction
                        )

                    paramsNow.y =
                        lerpInt(
                            startY,
                            targetY,
                            fraction
                        )

                    try {
                        wm.updateViewLayout(
                            bubble.view!!,
                            paramsNow
                        )
                    } catch (_: Exception) {
                    }
                }

                addListener(
                    object :
                        android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(
                            animation:
                                android.animation.Animator
                        ) {
                            bubble.savedX =
                                bubble.params?.x

                            bubble.savedY =
                                bubble.params?.y

                            persistTabs()
                            arrangeBubbleStack(bubble)
                            bubbleAnimators.remove(
                                bubble.id
                            )
                        }

                        override fun onAnimationCancel(
                            animation:
                                android.animation.Animator
                        ) {
                            bubbleAnimators.remove(
                                bubble.id
                            )
                        }
                    }
                )
            }

        bubbleAnimators[bubble.id] =
            animator

        animator.start()
    }

    private fun arrangeBubbleStack(
        hero: BubbleState
    ) {
        val size = dp(56)
        val gap = dp(8)
        val heroParams =
            hero.params ?: return

        val isLeft =
            heroParams.x <
                screenWidth() / 2

        bubbles.values
            .filter {
                it.kind == BubbleKind.TAB &&
                    it.id != hero.id
            }
            .forEachIndexed { index, bubble ->
                val params =
                    bubble.params
                        ?: return@forEachIndexed

                val offset =
                    minOf(
                        dp(12) +
                            index *
                            dp(12),
                        dp(72)
                    )

                params.x =
                    if (isLeft) {
                        (
                            heroParams.x +
                                offset
                        ).coerceIn(
                            0,
                            screenWidth() - size
                        )
                    } else {
                        (
                            heroParams.x -
                                offset
                        ).coerceIn(
                            0,
                            screenWidth() - size
                        )
                    }

                params.y =
                    (
                        heroParams.y +
                            index *
                            dp(3)
                    ).coerceIn(
                        dp(8),
                        screenHeight() -
                            size -
                            dp(8)
                    )

                try {
                    wm.updateViewLayout(
                        bubble.view!!,
                        params
                    )
                } catch (_: Exception) {
                }
            }

        persistTabs()
    }

    private fun lerpInt(
        start: Int,
        end: Int,
        fraction: Float
    ): Int =
        (
            start +
                (end - start) *
                fraction
        ).toInt()

    private fun closeTargetCenter():
        Pair<Float, Float>? {
        val target =
            closeTarget ?: return null

        val location =
            IntArray(2)

        target.getLocationOnScreen(location)

        return Pair(
            location[0] +
                target.width / 2f,
            location[1] +
                target.height / 2f
        )
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

        val hero =
            tabs.firstOrNull {
                it.id == activeId
            } ?: tabs.first()

        snapSingle(
            hero,
            dp(56)
        )
    }

    private fun snapSingle(
        bubble: BubbleState,
        size: Int
    ) {
        val params =
            bubble.params ?: return

        val targetX =
            if (
                params.x + size / 2 <
                screenWidth() / 2
            ) {
                dp(6)
            } else {
                screenWidth() -
                    size -
                    dp(6)
            }

        settleBubble(
            bubble,
            size,
            (targetX - params.x).toFloat(),
            0f
        )
    }

    private fun showCloseTarget() {
        if (closeTarget != null) {
            return
        }

        val size = dp(62)

        val target =
            FrameLayout(this).apply {
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFF2B2930.toInt())
                        setStroke(
                            dp(2),
                            Color.WHITE
                        )
                    }

                val label =
                    TextView(
                        this@FloatingBubbleService
                    ).apply {
                        text = "×"
                        textSize = 28f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                    }

                addView(
                    label,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
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
                y = dp(40)
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
        var webView: WebView? = null,
        var savedX: Int? = null,
        var savedY: Int? = null
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
