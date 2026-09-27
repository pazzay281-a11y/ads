package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.data.BubbleClickAction
import com.example.util.AdResetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingAdResetService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_UPDATE_CONFIG = "com.example.service.action.UPDATE_CONFIG"
        const val ACTION_TRIGGER_RESET = "com.example.service.action.TRIGGER_RESET"

        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "ad_reset_floating_channel"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FloatingAdResetService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingAdResetService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateConfig(context: Context) {
            val intent = Intent(context, FloatingAdResetService::class.java).apply {
                action = ACTION_UPDATE_CONFIG
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: AppPreferences

    private var floatingBubbleView: View? = null
    private var floatingMenuView: View? = null
    private var removeZoneView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var menuParams: WindowManager.LayoutParams? = null

    private var isMenuVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = AppPreferences.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFloatingService()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_RESET -> {
                performResetAction()
                return START_STICKY
            }
            ACTION_UPDATE_CONFIG -> {
                applyConfigUpdates()
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                if (floatingBubbleView == null) {
                    createFloatingBubble()
                }
                _isServiceRunning.value = true
                return START_STICKY
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.floating_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.floating_service_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetAdsIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingAdResetService::class.java).apply { action = ACTION_TRIGGER_RESET },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingAdResetService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_service_running))
            .setContentText(getString(R.string.floating_service_desc))
            .setSmallIcon(R.drawable.ad_reset_icon_1790527055214)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ad_reset_icon_1790527055214, getString(R.string.action_open_ads), resetAdsIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop_floating), stopIntent)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        if (!AdResetHelper.hasOverlayPermission(this)) {
            stopSelf()
            return
        }

        val sizePx = dpToPx(prefs.bubbleSizeDp.value)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 350
        }

        val container = FrameLayout(this).apply {
            alpha = prefs.bubbleAlpha.value
        }

        // Circular background drawable
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(prefs.bubbleColor.value.hex.toInt())
            setStroke(dpToPx(2), Color.WHITE)
        }
        container.background = bgDrawable
        container.elevation = dpToPx(8).toFloat()

        // Center Icon
        val iconView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                val padding = dpToPx(10)
                setMargins(padding, padding, padding, padding)
            }
            setImageResource(R.drawable.ad_reset_icon_1790527055214)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        setupTouchListener(container)

        try {
            windowManager.addView(container, bubbleParams)
            floatingBubbleView = container
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchDownTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = System.currentTimeMillis()
                    showRemoveZone()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    bubbleParams.x = initialX + deltaX
                    bubbleParams.y = initialY + deltaY

                    updateBubblePosition()
                    checkRemoveZoneHover(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideRemoveZone()
                    val totalDuration = System.currentTimeMillis() - touchDownTime
                    val totalDistX = abs(event.rawX - initialTouchX)
                    val totalDistY = abs(event.rawY - initialTouchY)

                    // Check if dropped into remove zone
                    if (isHoveringRemoveZone(event.rawX, event.rawY)) {
                        AdResetHelper.vibrateDevice(this, 50)
                        stopFloatingService()
                        return@setOnTouchListener true
                    }

                    // Check if it's a click: moved less than 15 pixels and held for < 350ms
                    if (totalDistX < dpToPx(12) && totalDistY < dpToPx(12) && totalDuration < 350) {
                        onBubbleClicked()
                    } else if (prefs.snapEdges.value) {
                        snapBubbleToEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateBubblePosition() {
        try {
            floatingBubbleView?.let { windowManager.updateViewLayout(it, bubbleParams) }
            if (isMenuVisible) {
                dismissMenu()
            }
        } catch (_: Exception) {}
    }

    private fun snapBubbleToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleSize = bubbleParams.width
        val middle = screenWidth / 2

        val targetX = if (bubbleParams.x + bubbleSize / 2 < middle) {
            16
        } else {
            screenWidth - bubbleSize - 16
        }

        val startX = bubbleParams.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                bubbleParams.x = animation.animatedValue as Int
                try {
                    floatingBubbleView?.let { windowManager.updateViewLayout(it, bubbleParams) }
                } catch (_: Exception) {}
            }
        }
        animator.start()
    }

    private fun onBubbleClicked() {
        if (prefs.vibrateEnabled.value) {
            AdResetHelper.vibrateDevice(this, 30)
        }

        when (prefs.clickAction.value) {
            BubbleClickAction.DIRECT_RESET -> {
                performResetAction()
            }
            BubbleClickAction.SHOW_MENU -> {
                if (isMenuVisible) {
                    dismissMenu()
                } else {
                    showQuickMenu()
                }
            }
        }
    }

    private fun performResetAction() {
        serviceScope.launch {
            val result = AdResetHelper.getAdvertisingId(this@FloatingAdResetService)
            prefs.recordResetAction(note = "Reset triggered via Floating Bubble", adId = result.id)
            AdResetHelper.openGoogleAdsSettings(this@FloatingAdResetService)
        }
    }

    private fun showQuickMenu() {
        if (floatingMenuView != null) {
            dismissMenu()
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val isLeft = bubbleParams.x < screenWidth / 2
        val menuWidth = dpToPx(220)

        menuParams = WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (isLeft) {
                bubbleParams.x + bubbleParams.width + dpToPx(12)
            } else {
                bubbleParams.x - menuWidth - dpToPx(12)
            }
            y = (bubbleParams.y - dpToPx(20)).coerceAtLeast(dpToPx(50))
        }

        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(12)
            setPadding(pad, pad, pad, pad)
            val bg = GradientDrawable().apply {
                setColor(0xEE1E222B.toInt())
                cornerRadius = dpToPx(18).toFloat()
                setStroke(dpToPx(1), 0x44FFFFFF)
            }
            background = bg
            elevation = dpToPx(12).toFloat()
        }

        // Header Title
        val header = TextView(this).apply {
            text = "⚡ Ad Quick Controls"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(8))
        }
        menuLayout.addView(header)

        // Menu Item 1: Reset Google Ads
        menuLayout.addView(createMenuItem("⚡ Reset Google Ads", 0xFF1E88E5.toInt()) {
            dismissMenu()
            performResetAction()
        })

        // Menu Item 2: Privacy Sandbox
        menuLayout.addView(createMenuItem("🛡️ Privacy Sandbox", 0xFF00897B.toInt()) {
            dismissMenu()
            AdResetHelper.openPrivacySandboxSettings(this@FloatingAdResetService)
        })

        // Menu Item 3: Copy AAID
        menuLayout.addView(createMenuItem("📋 Copy Current AAID", 0xFFFFA000.toInt()) {
            dismissMenu()
            serviceScope.launch {
                val res = AdResetHelper.getAdvertisingId(this@FloatingAdResetService)
                AdResetHelper.copyToClipboard(this@FloatingAdResetService, res.id)
            }
        })

        // Menu Item 4: Open App Dashboard
        menuLayout.addView(createMenuItem("📱 Open Dashboard", 0xFF546E7A.toInt()) {
            dismissMenu()
            val intent = Intent(this@FloatingAdResetService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        })

        // Menu Item 5: Close Floating
        menuLayout.addView(createMenuItem("❌ Hide Floating Bubble", 0xFFD32F2F.toInt()) {
            dismissMenu()
            stopFloatingService()
        })

        try {
            windowManager.addView(menuLayout, menuParams)
            floatingMenuView = menuLayout
            isMenuVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createMenuItem(label: String, accentColor: Int, onClick: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            isClickable = true
            isFocusable = true
            val bg = GradientDrawable().apply {
                setColor(0x22FFFFFF)
                cornerRadius = dpToPx(10).toFloat()
            }
            background = bg
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
                bottomMargin = dpToPx(4)
            }
            layoutParams = params

            setOnClickListener {
                if (prefs.vibrateEnabled.value) {
                    AdResetHelper.vibrateDevice(this@FloatingAdResetService, 25)
                }
                onClick()
            }
        }

        val dot = View(this).apply {
            val size = dpToPx(8)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                rightMargin = dpToPx(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
        }
        item.addView(dot)

        val text = TextView(this).apply {
            this.text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }
        item.addView(text)

        return item
    }

    private fun dismissMenu() {
        if (floatingMenuView != null && isMenuVisible) {
            try {
                windowManager.removeView(floatingMenuView)
            } catch (_: Exception) {}
            floatingMenuView = null
            isMenuVisible = false
        }
    }

    // Drop to remove zone
    private fun showRemoveZone() {
        if (removeZoneView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val size = dpToPx(64)
        val params = WindowManager.LayoutParams(
            size,
            size,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dpToPx(36)
        }

        val zone = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCCB71C1C.toInt())
                setStroke(dpToPx(2), Color.WHITE)
            }
            background = bg
            alpha = 0.8f
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            val p = dpToPx(16)
            setPadding(p, p, p, p)
        }
        zone.addView(icon)

        try {
            windowManager.addView(zone, params)
            removeZoneView = zone
        } catch (_: Exception) {}
    }

    private fun checkRemoveZoneHover(rawX: Float, rawY: Float) {
        val hovering = isHoveringRemoveZone(rawX, rawY)
        removeZoneView?.scaleX = if (hovering) 1.25f else 1.0f
        removeZoneView?.scaleY = if (hovering) 1.25f else 1.0f
    }

    private fun isHoveringRemoveZone(rawX: Float, rawY: Float): Boolean {
        val view = removeZoneView ?: return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val zoneLeft = location[0]
        val zoneTop = location[1]
        val zoneRight = zoneLeft + view.width
        val zoneBottom = zoneTop + view.height

        return rawX >= zoneLeft - 30 && rawX <= zoneRight + 30 &&
                rawY >= zoneTop - 30 && rawY <= zoneBottom + 30
    }

    private fun hideRemoveZone() {
        if (removeZoneView != null) {
            try {
                windowManager.removeView(removeZoneView)
            } catch (_: Exception) {}
            removeZoneView = null
        }
    }

    private fun applyConfigUpdates() {
        floatingBubbleView?.let { bubble ->
            val sizePx = dpToPx(prefs.bubbleSizeDp.value)
            bubbleParams.width = sizePx
            bubbleParams.height = sizePx
            bubble.alpha = prefs.bubbleAlpha.value

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(prefs.bubbleColor.value.hex.toInt())
                setStroke(dpToPx(2), Color.WHITE)
            }
            bubble.background = bg

            try {
                windowManager.updateViewLayout(bubble, bubbleParams)
            } catch (_: Exception) {}
        }
    }

    private fun stopFloatingService() {
        dismissMenu()
        hideRemoveZone()
        if (floatingBubbleView != null) {
            try {
                windowManager.removeView(floatingBubbleView)
            } catch (_: Exception) {}
            floatingBubbleView = null
        }
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissMenu()
        hideRemoveZone()
        if (floatingBubbleView != null) {
            try {
                windowManager.removeView(floatingBubbleView)
            } catch (_: Exception) {}
            floatingBubbleView = null
        }
        _isServiceRunning.value = false
        serviceScope.cancel()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
