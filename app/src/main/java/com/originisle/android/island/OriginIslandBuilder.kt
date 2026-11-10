package com.originisle.android.island

import android.content.Context
import android.graphics.Color
import android.os.Bundle
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

    /**
     * Whitelists this package for the SuperX scenes vivo will actually grant.
     *
     * Delegates to [SceneCapability], which probes each scene, caches the answers, and applies a
     * retry policy to the reflective lookup. Previously a single early failure was cached as
     * permanent for the life of the process, so island access could die silently until a restart.
     */
    fun grantScenes(context: Context) = SceneCapability.grant(context)

    /** @see SceneCapability.isAvailable */
    fun isSceneAvailable(context: Context, scene: String): Boolean = SceneCapability.isAvailable(context, scene)

    /** @see SceneCapability.bestScene */
    fun bestScene(context: Context, vararg candidates: String): String = SceneCapability.bestScene(context, *candidates)

    /** @see SceneCapability.sceneFor */
    fun sceneFor(cardType: String, context: Context): String = SceneCapability.sceneFor(cardType, context)

    /** @see SceneCapability.reset */
    fun resetSceneCapability() = SceneCapability.reset()

    fun parseColor(colorStr: String?, defaultColor: Int): Int = try {
        Color.parseColor(colorStr)
    } catch (_: Exception) {
        defaultColor
    }

    /** vivo's own call colours: red for decline / hang up, green for answer. */
    const val NATIVE_RED = 0xFFFF3B30.toInt()
    const val NATIVE_GREEN = 0xFF34C759.toInt()

    /** The neutral button every other action gets, like a first-party island. */
    const val NATIVE_NEUTRAL = 0xFF2C2C2E.toInt()

    private val DESTRUCTIVE_WORDS =
        listOf("decline", "reject", "hang up", "hangup", "end call", "deny", "delete", "挂断", "拒接", "拒绝", "删除")
    private val ACCEPT_WORDS = listOf("answer", "accept", "approve", "接听", "接受", "同意", "批准")

    /**
     * Native OriginOS 7 button colour for an action title: only call-style decline (red) and
     * accept (green) are coloured; everything else is the neutral pill. [defaultColor] is
     * deliberately ignored: per-card brand colours are not a vivo pattern.
     */
    @Suppress("UNUSED_PARAMETER")
    fun resolveSemanticButtonBgColor(
        title: String,
        defaultColor: Int = 0,
    ): Int {
        val t = title.lowercase()
        return when {
            DESTRUCTIVE_WORDS.any { t.contains(it) } -> NATIVE_RED
            ACCEPT_WORDS.any { t.contains(it) } -> NATIVE_GREEN
            else -> NATIVE_NEUTRAL
        }
    }

    /** Button text is always white on the three native fills. */
    @Suppress("UNUSED_PARAMETER")
    fun resolveSemanticButtonTextColor(
        title: String,
        defaultColor: Int = 0,
    ): Int = 0xFFFFFFFF.toInt()

    /** Bundle that removes/ends a previously-posted card for [scene]. */
    fun buildEndBundle(scene: String): Bundle = BundleCodec.buildEndBundle(scene)

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

}
