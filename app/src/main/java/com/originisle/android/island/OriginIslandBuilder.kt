package com.originisle.android.island

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import com.originisle.android.R

/**
 * Assembles a vivo SuperX / OriginIsland notification-extras [Bundle] and grants the app the
 * SuperX scenes it needs. Ported verbatim from the original app.
 *
 * The single interesting reflective call is [grantScenes], which invokes the hidden
 * `NotificationManager.setSuperXInfosSceneList(List, List, List, List)` to whitelist this
 * package for every scene in [OriginIslandConstants.SUPERX_SCENES]. This only succeeds on
 * vivo OriginOS builds; elsewhere it logs "unavailable" and no-ops.
 *
 * Bundle assembly itself now lives in [BundleCodec] as a pure function over [SuperXRequest];
 * [buildBundle] is a thin adapter that resolves the theme colours and the cutout padding, which
 * are the only two things that genuinely need a [Context].
 */
object OriginIslandBuilder {

    private const val TAG = "OriginIslandBuilder"

    /**
     * [grantScenes] runs on every process start AND every card post, so the reflective method
     * lookup is cached. Once the lookup has failed (non-vivo build), don't retry OR re-log —
     * previously every single post threw + caught NoSuchMethodException, which is a stack trace
     * per card for nothing.
     */
    @Volatile
    private var scenesMethod: java.lang.reflect.Method? = null
    @Volatile
    private var scenesUnavailable = false

    fun grantScenes(context: Context) {
        if (scenesUnavailable) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val method = scenesMethod ?: runCatching {
            NotificationManager::class.java.getMethod(
                "setSuperXInfosSceneList",
                List::class.java, List::class.java, List::class.java, List::class.java,
            )
        }.getOrElse {
            scenesUnavailable = true
            Log.w(TAG, "setSuperXInfosSceneList unavailable: ${it.message}")
            return
        }
        scenesMethod = method
        try {
            val scenes = ArrayList(OriginIslandConstants.SUPERX_SCENES)
            val enabled = ArrayList<String>()
            val pkgs = ArrayList<String>()
            val allowed = ArrayList<String>()
            repeat(scenes.size) {
                enabled.add("true")
                pkgs.add(context.packageName)
                allowed.add("true")
            }
            method.invoke(nm, scenes, enabled, pkgs, allowed)
            Log.d(TAG, "setSuperXInfosSceneList granted for ${context.packageName}")
        } catch (e: Exception) {
            // The method exists but the call was refused (not vivo, permission revoked, …). Don't
            // cache this as permanent — a refusal can be transient, unlike a missing method.
            Log.w(TAG, "setSuperXInfosSceneList call failed: ${e.message}")
        }
    }

    fun parseColor(colorStr: String?, defaultColor: Int): Int = try {
        Color.parseColor(colorStr)
    } catch (_: Exception) {
        defaultColor
    }

    /** Intelligently resolves background color for action buttons based on their intent/label. */
    fun resolveSemanticButtonBgColor(title: String, defaultColor: Int = 0): Int {
        val t = title.lowercase()
        return when {
            listOf("decline", "reject", "hang up", "end", "cancel", "deny", "drop", "stop", "dismiss", "reset", "delete")
                .any { t.contains(it) } -> 0xFFFF3B30.toInt() // Apple / OriginOS Crimson Red
            listOf("answer", "accept", "approve", "yes", "join", "allow", "start", "resume", "confirm", "open")
                .any { t.contains(it) } -> 0xFF34C759.toInt() // Apple / OriginOS Emerald Green
            listOf("pause", "snooze", "later", "retry")
                .any { t.contains(it) } -> 0xFFFF9500.toInt() // Radiant Amber
            !isDefaultColor(defaultColor) -> defaultColor
            else -> 0xFF2C2C2E.toInt() // Modern dark frosted pill
        }
    }

    /** Intelligently resolves text color for action buttons. */
    fun resolveSemanticButtonTextColor(title: String, defaultColor: Int = 0): Int {
        val t = title.lowercase()
        return when {
            listOf("decline", "reject", "hang up", "end", "cancel", "deny", "drop", "stop", "dismiss", "reset", "delete",
                   "answer", "accept", "approve", "yes", "join", "allow", "start", "resume", "confirm",
                   "pause", "snooze", "later", "retry").any { t.contains(it) } -> 0xFFFFFFFF.toInt()
            !isDefaultColor(defaultColor) -> defaultColor
            else -> 0xFFFFFFFF.toInt()
        }
    }

    /** Bundle that removes/ends a previously-posted card for [scene]. */
    fun buildEndBundle(scene: String): Bundle = BundleCodec.buildEndBundle(scene)

    internal val GEMINI_PALETTE = intArrayOf(
        0xFF38BDF8.toInt(), // Sky / Electric Cyan
        0xFF818CF8.toInt(), // Radiant Indigo
        0xFFC084FC.toInt(), // Celestial Purple
        0xFFF472B6.toInt(), // Sunset Rose / Neon Pink
    )

    /**
     * Creates an iridescent Google Gemini style chromatic text span.
     * Colors each character along the cyan-indigo-purple-pink spectrum with bold typeface.
     */
    fun buildGeminiTextSpan(text: String): SpannableString {
        val clean = text.trim().removePrefix("✦").removePrefix("✨").trim()
        val fullText = if (clean.isEmpty()) "✦ View" else "✦ $clean"
        val span = SpannableString(fullText)
        val len = fullText.length
        if (len <= 0) return span
        for (i in fullText.indices) {
            val colorIndex = (i * (GEMINI_PALETTE.size - 1) / maxOf(1, len - 1)).coerceIn(0, GEMINI_PALETTE.size - 1)
            span.setSpan(
                ForegroundColorSpan(GEMINI_PALETTE[colorIndex]),
                i,
                i + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        span.setSpan(StyleSpan(Typeface.BOLD), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    fun isGeminiGlowEnabled(context: Context): Boolean =
        context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, 0)
            .getBoolean("cast_gemini_view_glow", true)

    /**
     * Build the full SuperX extras bundle.
     *
     * Every parameter maps 1:1 onto a field of [SuperXRequest]; see [OriginIslandConstants] for
     * the semantics of each template. The heavy lifting is [BundleCodec.buildBundlePure], which
     * takes no [Context] and is therefore JVM-testable.
     *
     * This adapter exists so existing call sites keep compiling while they migrate to constructing
     * a [SuperXRequest] directly.
     */
    fun buildBundle(
        context: Context,
        template: Int,
        title: String,
        content: String,
        leftContent: String,
        rightContent: String,
        extra1: String = "",
        extra2: String = "",
        extra3: String = "",
        extra4: String = "",
        rightTemplate: Int = 0,
        defaultIcon: android.graphics.drawable.Icon,
        leftIcon: android.graphics.drawable.Icon,
        rightIcon: android.graphics.drawable.Icon,
        accentIcon: android.graphics.drawable.Icon,
        progress: Int = 0,
        bgColor: Int = 0,
        fgColor: Int = 0,
        scene: String,
        clickResp: android.app.PendingIntent? = null,
        // 0 = create/update the card, 2 = end it (see buildEndBundle). The original posts with 0;
        // a non-zero create operation makes vivo's interceptor silently drop the notification.
        operation: Int = 0,
        largeIcon: android.graphics.drawable.Icon? = null,
        subText: String? = null,
        navMsg: String? = null,
        navAssistText: String? = null,
        actions: List<CardAction> = emptyList(),
        keepDuration: Int = 0,
        displays: Int = 0,
        sound: Boolean = false,
        dismissWhenKill: Boolean = true,
        newNode: Int = 0,
        islandShowTime: Int = 0,
        capsuleShowTime: Int = 0,
        forceShow: Boolean = false,
        progressState: Int = 0,
        progressMarkers: Boolean = false,
        // Template 2: explicit milestone icons along the bar + the icon that rides it, and the bar
        // colour. Empty/null/0 keeps the defaults above.
        progressNodeIcons: List<android.graphics.drawable.Icon> = emptyList(),
        progressIndicatorIcon: android.graphics.drawable.Icon? = null,
        progressColor: Int = 0,
        showRightIcon: Boolean = true,
        showLeftIcon: Boolean = true,
        cardBgColor: Int = 0,
        keepScreenOn: Boolean = false,
        disableInvertColor: Boolean = true,
        lightColor: Int = 0,
        lightMode: Int = 0,
        generatingStatus: Int = 0,
        iconStatusType: Int = -1,
        leftDoubleLine: List<String> = emptyList(),
        rightDoubleLine: List<String> = emptyList(),
        buttonTitles: List<String> = emptyList(),
        buttonBgColors: List<Int> = emptyList(),
        buttonTextColors: List<Int> = emptyList(),
        customTemplate: android.widget.RemoteViews? = null,
        waveState: Int = 1,
        waveColorList: List<String>? = null,
        changeRecord: Int = 0,
        // The native vivo music-widget card (com.vivo.musicwidgetmix) posts its SuperX notification
        // with this false — confirmed via `dumpsys notification` while it was live — and never shows
        // up in the notification shade, unlike every card this app posts with it hardcoded true.
        // Testing whether flipping it is what actually gates shade visibility.
        showNotify: Boolean = true,
        islandNotify: Boolean = false,
        priority: Int = 0,
        landingPkg: String? = null,
        landingInfo: String? = null,
    ): Bundle {
        val request = SuperXRequest(
            scene = scene,
            template = template,
            rightTemplate = rightTemplate,
            title = title,
            content = content,
            // Cutout padding depends on the device's camera hole, so it is resolved here — the one
            // piece of the island payload that genuinely needs a Context.
            leftContent = CutoutConfig.applyCutoutPadding(context, leftContent),
            rightContent = rightContent,
            extra1 = extra1,
            extra2 = extra2,
            extra3 = extra3,
            extra4 = extra4,
            subText = subText,
            navMsg = navMsg,
            navAssistText = navAssistText,
            bgColor = bgColor,
            fgColor = fgColor,
            cardBgColor = cardBgColor,
            lightColor = lightColor,
            lightMode = lightMode,
            defaultIcon = defaultIcon,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            accentIcon = accentIcon,
            largeIcon = largeIcon,
            progress = progress,
            progressState = progressState,
            progressMarkers = progressMarkers,
            progressNodeIcons = progressNodeIcons,
            progressIndicatorIcon = progressIndicatorIcon,
            progressColor = progressColor,
            clickResp = clickResp,
            operation = operation,
            actions = actions,
            keepDuration = keepDuration,
            displays = displays,
            sound = sound,
            dismissWhenKill = dismissWhenKill,
            newNode = newNode,
            islandShowTime = islandShowTime,
            capsuleShowTime = capsuleShowTime,
            forceShow = forceShow,
            showRightIcon = showRightIcon,
            showLeftIcon = showLeftIcon,
            keepScreenOn = keepScreenOn,
            disableInvertColor = disableInvertColor,
            generatingStatus = generatingStatus,
            iconStatusType = iconStatusType,
            leftDoubleLine = leftDoubleLine,
            rightDoubleLine = rightDoubleLine,
            buttonTitles = buttonTitles,
            buttonBgColors = buttonBgColors,
            buttonTextColors = buttonTextColors,
            customTemplate = customTemplate,
            waveState = waveState,
            waveColorList = waveColorList,
            changeRecord = changeRecord,
            showNotify = showNotify,
            islandNotify = islandNotify,
            priority = priority,
            landingPkg = landingPkg,
            landingInfo = landingInfo,
        )
        return BundleCodec.buildBundlePure(
            request,
            ColorDefaults(
                progressBarColor = context.resources.getColor(
                    R.color.vivo_super_x_progress_bar_default_color,
                    null,
                ),
                progressBarBackgroundColor = context.resources.getColor(
                    R.color.vivo_super_x_progress_bar_bkg_default_color,
                    null,
                ),
            ),
        )
    }

    private fun isDefaultColor(color: Int): Boolean = (color ushr 24) == 0
}
