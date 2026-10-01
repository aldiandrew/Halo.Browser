package com.aldiandrew.halobrowser

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
    private var closeTarget: View? = null

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
        ensureManager()
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
                    intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL,
                    open = true
                )
            }

            ACTION_OPEN_BUBBLE -> {
                openBubble(
                    intent.getIntExtra(EXTRA_BUBBLE_ID, TAB_ID)
                )
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        closeAll()
        super.onDestroy()
    }

    fun maximize() {
        expanded = true
        arrangeBubbles()
    }

    fun minimize() {
        expanded = false
        arrangeBubbles()
    }

    fun addTab(
        url: String,
        open: Boolean
    ): Int {
        val id = nextId++
        val state = BubbleState(
            id = id,
            url = url.ifBlank { DEFAULT_URL },
            kind = BubbleKind.TAB
        )

        bubbles[id] = state
        persistTabs()
        createBubbleView(state)

        if (open) {
            activeId = id
            maximize()
            launchFloatingActivity(id)
        } else {
            arrangeBubbles()
        }

        return id
    }

    fun openBubble(id: Int) {
        val bubble = bubbles[id] ?: return
        activeId = bubble.id
        maximize()
        launchFloatingActivity(bubble.id)
    }

    fun closeTab(id: Int) {
        if (id == MANAGER_ID) return

        val bubble = bubbles.remove(id) ?: return

        bubble.view?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }

        if (activeId == id) {
            activeId = bubbles.keys.firstOrNull { it != MANAGER_ID }
                ?: MANAGER_ID
        }

        persistTabs()

        if (bubbles.keys.none { it != MANAGER_ID }) {
            addTab(DEFAULT_URL, open = false)
        }

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

    private fun ensureManager() {
        if (bubbles[MANAGER_ID] != null) return

        val state = BubbleState(
            id = MANAGER_ID,
            url = "about:manager",
            kind = BubbleKind.MANAGER
        )
        bubbles[MANAGER_ID] = state
        createBubbleView(state)
    }

    private fun ensureFirstTab() {
        if (bubbles.keys.any { it != MANAGER_ID }) return
        addTab(DEFAULT_URL, open = false)
    }

    private fun restoreTabs() {
        val ids = prefs
            .getStringSet(KEY_TAB_IDS, emptySet())
            ?: emptySet()

        ids.mapNotNull { it.toIntOrNull() }
            .sorted()
            .forEach { id ->
                val url =
                    prefs.getString("url_" + id, DEFAULT_URL)
                        ?: DEFAULT_URL

                bubbles[id] = BubbleState(
                    id = id,
                    url = url,
                    kind = BubbleKind.TAB
                )

                nextId = max(nextId, id + 1)
            }
    }

    private fun persistTabs() {
        val ids = bubbles.values
            .filter { it.kind == BubbleKind.TAB }
            .map { it.id.toString() }
            .toSet()

        val editor = prefs.edit()
            .putStringSet(KEY_TAB_IDS, ids)
            .putInt(KEY_NEXT_ID, nextId)

        bubbles.values
            .filter { it.kind == BubbleKind.TAB }
            .forEach {
                editor.putString("url_" + it.id, it.url)
            }

        editor.apply()
    }

    private fun createBubbleView(bubble: BubbleState) {
        if (bubble.view != null) return

        val size = dp(68)

        val root = FrameLayout(this).apply {
            contentDescription =
                if (bubble.kind == BubbleKind.MANAGER) {
                    "Halo Browser manager"
                } else {
                    "Halo Browser tab " + bubble.id
                }
        }

        val compose = ComposeView(this).apply {
            isClickable = false
            isFocusable = false

            setContent {
                HaloExpressiveTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (bubble.kind == BubbleKind.MANAGER) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                if (bubble.kind == BubbleKind.MANAGER) {
                                    Icons.Filled.GridView
                                } else {
                                    Icons.Filled.Language
                                },
                            contentDescription = null,
                            tint =
                                if (bubble.kind == BubbleKind.MANAGER) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                        )
                    }
                }
            }
        }

        root.addView(
            compose,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        bubble.view = root
        bubble.params = overlayParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        root.setOnTouchListener(
            makeDragListener(bubble, size)
        )

        wm.addView(root, bubble.params)
    }

    private fun makeDragListener(
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
                    startX = bubble.params?.x ?: 0
                    startY = bubble.params?.y ?: 0
                    moved = false
                    showCloseTarget()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()

                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) {
                        moved = true
                    }

                    bubble.params?.let { params ->
                        params.x = startX + dx
                        params.y = startY + dy
                        clamp(params, size)
                        wm.updateViewLayout(
                            bubble.view,
                            params
                        )
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val overClose =
                        isOverCloseTarget(event.rawX, event.rawY)

                    hideCloseTarget()

                    when {
                        !moved -> openBubble(bubble.id)

                        overClose && bubble.kind == BubbleKind.TAB ->
                            closeTab(bubble.id)

                        else ->
                            snapSingle(bubble, size)
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun arrangeBubbles() {
        if (bubbles.isEmpty()) return

        val size = dp(68)
        val gap = dp(12)
        val all = bubbles.values.toList()

        if (expanded) {
            val totalWidth =
                all.size * size +
                    max(0, all.size - 1) * gap

            val startX =
                max(
                    dp(10),
                    (screenWidth() - totalWidth) / 2
                )

            val y = dp(72)

            all.forEachIndexed { index, bubble ->
                animateBubbleTo(
                    bubble,
                    startX + index * (size + gap),
                    y
                )
            }
        } else {
            all.forEachIndexed { index, bubble ->
                animateBubbleTo(
                    bubble,
                    screenWidth() - size - dp(12),
                    dp(260) + index * dp(76)
                )
            }
        }
    }

    private fun animateBubbleTo(
        bubble: BubbleState,
        x: Int,
        y: Int
    ) {
        val params = bubble.params ?: return
        val fromX = params.x
        val fromY = params.y

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator =
                android.view.animation.OvershootInterpolator(1.1f)

            addUpdateListener {
                val t = it.animatedValue as Float
                params.x =
                    (fromX + (x - fromX) * t).toInt()
                params.y =
                    (fromY + (y - fromY) * t).toInt()

                try {
                    wm.updateViewLayout(
                        bubble.view,
                        params
                    )
                } catch (_: Exception) {
                }
            }
            start()
        }
    }

    private fun snapSingle(
        bubble: BubbleState,
        size: Int
    ) {
        val params = bubble.params ?: return

        params.x =
            if (params.x + size / 2 < screenWidth() / 2) {
                dp(6)
            } else {
                screenWidth() - size - dp(6)
            }

        clamp(params, size)
        wm.updateViewLayout(
            bubble.view,
            params
        )
    }

    private fun launchFloatingActivity(id: Int) {
        val intent = Intent(
            this,
            FloatingActivity::class.java
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(
                EXTRA_BUBBLE_ID,
                id
            )
            putExtra(
                EXTRA_URL,
                bubbles[id]?.url ?: DEFAULT_URL
            )
        }

        startActivity(intent)
    }

    private fun showCloseTarget() {
        if (closeTarget != null) return

        val size = dp(76)

        val target = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x33000000)
                setStroke(
                    dp(2),
                    0xFFE1E7F0.toInt()
                )
            }
        }

        val params = overlayParams(size, size).apply {
            gravity =
                Gravity.BOTTOM or
                    Gravity.CENTER_HORIZONTAL
            y = dp(20)
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

    private fun isOverCloseTarget(
        x: Float,
        y: Float
    ): Boolean {
        val target = closeTarget ?: return false
        val location = IntArray(2)
        target.getLocationOnScreen(location)

        val centerX =
            location[0] + target.width / 2f
        val centerY =
            location[1] + target.height / 2f

        val radius =
            max(target.width, target.height) / 2f +
                dp(18)

        val dx = x - centerX
        val dy = y - centerY

        return dx * dx + dy * dy <= radius * radius
    }

    private fun overlayParams(
        width: Int,
        height: Int
    ) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun clamp(
        params: WindowManager.LayoutParams,
        size: Int
    ) {
        params.x =
            params.x.coerceIn(
                0,
                max(0, screenWidth() - size)
            )

        params.y =
            params.y.coerceIn(
                dp(8),
                max(
                    dp(8),
                    screenHeight() - size - dp(8)
                )
            )
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Halo Browser Floating Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        "Keeps Halo Browser chat heads available"
                    setShowBadge(false)
                }
            )
    }

    private fun startAsForeground() {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                100,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_halo_browser)
                .setContentTitle("Halo Browser")
                .setContentText(
                    "Floating browser is active"
                )
                .setContentIntent(pendingIntent)
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                FOREGROUND_ID,
                notification
            )
        }
    }

    private fun closeAll() {
        hideCloseTarget()

        val all = bubbles.values.toList()
        bubbles.clear()

        all.forEach { bubble ->
            bubble.view?.let {
                try {
                    wm.removeView(it)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun screenWidth(): Int =
        resources.displayMetrics.widthPixels

    private fun screenHeight(): Int =
        resources.displayMetrics.heightPixels

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private data class BubbleState(
        val id: Int,
        var url: String,
        val kind: BubbleKind,
        var view: View? = null,
        var params: WindowManager.LayoutParams? = null
    )

    private enum class BubbleKind {
        MANAGER,
        TAB
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

        const val EXTRA_URL = "halo_browser_url"
        const val EXTRA_BUBBLE_ID =
            "halo_browser_bubble_id"

        const val MANAGER_ID = 0
        const val TAB_ID = 1

        private const val DEFAULT_URL =
            "https://www.google.com"

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
