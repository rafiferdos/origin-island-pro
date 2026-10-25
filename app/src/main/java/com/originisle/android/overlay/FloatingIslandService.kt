package com.originisle.android.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.originisle.android.R
import com.originisle.android.service.IconCache

/**
 * Foreground WindowManager Overlay Service that renders a real, floating, interactive
 * Apple Liquid Glass & Frosted Glass Island directly over the screen cutout.
 */
class FloatingIslandService : Service() {

    companion object {
        const val TAG = "FloatingIslandService"
        const val CHANNEL_ID = "origin_floating_overlay_channel"
        const val NOTIFICATION_ID = 99991

        const val ACTION_SHOW_CARD = "com.originisle.android.overlay.SHOW_CARD"
        const val ACTION_DISMISS_CARD = "com.originisle.android.overlay.DISMISS_CARD"
        const val ACTION_STOP_OVERLAY = "com.originisle.android.overlay.STOP_OVERLAY"

        @Volatile
        var isOverlayRunning = false
            private set

        fun postCard(context: Context, intent: Intent) {
            val prefs = context.getSharedPreferences("OriginIsle_Prefs", 0)
            val engineMode = prefs.getString("island_engine_mode", "native") ?: "native"
            if (engineMode != "overlay") return
            if (!Settings.canDrawOverlays(context)) return

            val overlayIntent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_SHOW_CARD
                putExtras(intent)
            }
            context.startService(overlayIntent)
        }

        fun dismissCard(context: Context, id: Int) {
            val overlayIntent = Intent(context, FloatingIslandService::class.java).apply {
                action = ACTION_DISMISS_CARD
                putExtra("id", id)
            }
            context.startService(overlayIntent)
        }
    }

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentCard by mutableStateOf<OverlayCardData?>(null)
    private var visualMode by mutableStateOf(OverlayVisualMode.HIDDEN)
    private var glassStyle by mutableStateOf("liquid")

    private val layoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val topOffsetDp = getSharedPreferences("OriginIsle_Prefs", 0).getInt("overlay_top_offset", 14)
            y = (topOffsetDp * resources.displayMetrics.density).toInt()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isOverlayRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()

        if (Settings.canDrawOverlays(this)) {
            initOverlayView()
        }
    }

    private fun initOverlayView() {
        if (composeView != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val prefs = getSharedPreferences("OriginIsle_Prefs", 0)
        glassStyle = prefs.getString("expanded_glass_style", "liquid") ?: "liquid"

        composeView = ComposeView(this).apply {
            lifecycleOwner.attachTo(this)
            setContent {
                FloatingIslandRoot(
                    card = currentCard,
                    mode = visualMode,
                    glassStyle = glassStyle,
                    onExpand = {
                        setVisualModeExpanded(true)
                    },
                    onCollapse = {
                        setVisualModeExpanded(false)
                    },
                    onDismiss = {
                        visualMode = OverlayVisualMode.HIDDEN
                        currentCard = null
                        updateWindowParams()
                    }
                )
            }
        }

        try {
            windowManager?.addView(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun setVisualModeExpanded(expanded: Boolean) {
        visualMode = if (expanded) OverlayVisualMode.EXPANDED else OverlayVisualMode.COMPACT
        updateWindowParams()
    }

    private fun updateWindowParams() {
        val wm = windowManager ?: return
        val cv = composeView ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (visualMode == OverlayVisualMode.EXPANDED) {
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                layoutParams.blurBehindRadius = 45 // Real hardware wallpaper and app backdrop blur!
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            } else {
                layoutParams.blurBehindRadius = 0
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            }
        } else {
            layoutParams.width = if (visualMode == OverlayVisualMode.EXPANDED) {
                WindowManager.LayoutParams.MATCH_PARENT
            } else {
                WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        try {
            wm.updateViewLayout(cv, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "updateViewLayout failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_SHOW_CARD -> handleShowCard(intent)
            ACTION_DISMISS_CARD -> handleDismissCard(intent)
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    private fun handleShowCard(intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        val subtext = intent.getStringExtra("subtext").orEmpty()
        val sourceApp = intent.getStringExtra("source_app").orEmpty()
        val leftContent = intent.getStringExtra("oi_left_content").orEmpty()
        val rightContent = intent.getStringExtra("oi_right_content").orEmpty()
        val statusChip = intent.getStringExtra("status_chip_text").orEmpty()
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val lightColor = intent.getIntExtra("oi_light_color_int", 0)

        // Resolve icon bitmap from cache or resource
        val iconBitmap: Bitmap? = IconCache.activeLargeBitmaps[id]
            ?: IconCache.activeSmallIcons[id]?.let { runCatching { it.loadDrawable(this)?.toBitmap() }.getOrNull() }

        // Progress
        val showProgress = intent.getBooleanExtra("show_progress", false)
        val progressMax = intent.getIntExtra("progress_max", 0)
        val rawProgress = intent.getIntExtra("progress", 0)
        val progress = when {
            intent.hasExtra("oi_progress") -> intent.getIntExtra("oi_progress", 0)
            showProgress && progressMax > 0 -> rawProgress * 100 / progressMax
            else -> if (showProgress) rawProgress else -1
        }

        // Wave
        val waveState = intent.getIntExtra("oi_wave_state", 0)
        val waveColors = intent.getStringArrayListExtra("oi_wave_color_list") ?: emptyList()

        // Buttons
        val buttonTitles = intent.getStringArrayListExtra("oi_button_titles") ?: emptyList()
        val buttonColors = intent.getIntegerArrayListExtra("oi_button_bg_colors") ?: emptyList()
        val buttonTextColors = intent.getIntegerArrayListExtra("oi_button_text_colors") ?: emptyList()
        val actions = intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
        val buttonIntents = actions?.mapNotNull { it.actionIntent } ?: emptyList()

        val clickIntent = intent.getParcelableExtra("click_resp", PendingIntent::class.java)
            ?: intent.getParcelableExtra("source_content_intent", PendingIntent::class.java)

        val prefs = getSharedPreferences("OriginIsle_Prefs", 0)
        glassStyle = prefs.getString("expanded_glass_style", "liquid") ?: "liquid"

        currentCard = OverlayCardData(
            id = id,
            title = title,
            text = text,
            subtext = subtext,
            sourceApp = sourceApp,
            iconBitmap = iconBitmap,
            iconRes = iconRes,
            leftContent = leftContent,
            rightContent = rightContent,
            statusChip = statusChip,
            progress = progress,
            isOngoing = intent.getBooleanExtra("is_ongoing", false),
            scene = intent.getStringExtra("oi_scene") ?: "NAVIGATION",
            template = intent.getIntExtra("oi_template", 0),
            waveState = waveState,
            waveColors = waveColors,
            buttonTitles = buttonTitles,
            buttonColors = buttonColors,
            buttonTextColors = buttonTextColors,
            buttonIntents = buttonIntents,
            clickIntent = clickIntent,
            lightColor = lightColor,
        )

        visualMode = OverlayVisualMode.COMPACT
        updateWindowParams()
    }

    private fun handleDismissCard(intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        if (currentCard?.id == id) {
            visualMode = OverlayVisualMode.HIDDEN
            currentCard = null
            updateWindowParams()
        }
    }

    private fun createNotificationChannel() {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating Island Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains the floating liquid glass overlay island"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Liquid Glass Island Active")
            .setContentText("Tap cards to expand with true hardware blur")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayRunning = false
        lifecycleOwner.onDestroy()
        composeView?.let {
            runCatching { windowManager?.removeView(it) }
        }
        composeView = null
    }
}
