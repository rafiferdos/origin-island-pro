package com.originisle.android.island

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.widget.RemoteViews

/**
 * A single card action as the island should render it: label, optional glyph, optional tap intent.
 *
 * This replaces the old `OriginIslandBuilder.OriginAction` (kept as a typealias so existing call
 * sites keep compiling).
 */
data class CardAction(
    val title: String,
    val icon: Icon?,
    val pendingIntent: PendingIntent?,
)

/** @see CardAction */
typealias OriginAction = CardAction

/**
 * One typed SuperX card request — everything [BundleCodec] needs to assemble the extras bundle.
 *
 * This is the seam the codebase was missing. Previously a card travelled from detection to
 * transport as ~50 stringly-typed `oi_*` Intent extras, and the bundle was built by a single
 * 63-parameter function. A typo in a key was a runtime silent no-op, not a compile error.
 *
 * Now:
 * - the request is a value type, so a card is constructed once and passed around;
 * - [BundleCodec.buildBundlePure] takes it and a [ColorDefaults] and returns a [android.os.Bundle]
 *   with no `Context` — which means the wire format can be unit-tested on the JVM.
 *
 * Field-for-field this is exactly what `OriginIslandBuilder.buildBundle` used to accept. Nothing
 * about the emitted bundle changes; only the type safety around it does.
 *
 * Not yet `Parcelable`: `kotlin-parcelize` is not on the classpath and the request is currently
 * only passed in-process. When `PlaygroundService` is converted to post a request through an
 * Intent, add the plugin and `@Parcelize` — every field here is already Parcelable.
 */
data class SuperXRequest(
    /** Cast id; the identity the card is posted and later ended under. */
    val id: Int = -1,
    val scene: String = "NAVIGATION",
    val template: Int = 0,
    val rightTemplate: Int = 0,
    val title: String = "",
    val content: String = "",
    val leftContent: String = "",
    val rightContent: String = "",
    val extra1: String = "",
    val extra2: String = "",
    val extra3: String = "",
    val extra4: String = "",
    val subText: String? = null,
    val navMsg: String? = null,
    val navAssistText: String? = null,
    val bgColor: Int = 0,
    val fgColor: Int = 0,
    val cardBgColor: Int = 0,
    val lightColor: Int = 0,
    val lightMode: Int = 0,
    val defaultIcon: Icon? = null,
    val leftIcon: Icon? = null,
    val rightIcon: Icon? = null,
    val accentIcon: Icon? = null,
    val largeIcon: Icon? = null,
    val progress: Int = 0,
    val progressState: Int = 0,
    val progressMarkers: Boolean = false,
    val progressNodeIcons: List<Icon> = emptyList(),
    val progressIndicatorIcon: Icon? = null,
    val progressColor: Int = 0,
    val clickResp: PendingIntent? = null,
    /** 0 = create/update the card, 2 = end it. See [BundleCodec.buildEndBundle]. */
    val operation: Int = 0,
    val actions: List<CardAction> = emptyList(),
    val keepDuration: Int = 0,
    val displays: Int = 0,
    val sound: Boolean = false,
    val dismissWhenKill: Boolean = true,
    val newNode: Int = 0,
    val islandShowTime: Int = 0,
    val capsuleShowTime: Int = 0,
    val forceShow: Boolean = false,
    val showRightIcon: Boolean = true,
    val showLeftIcon: Boolean = true,
    val keepScreenOn: Boolean = false,
    val disableInvertColor: Boolean = true,
    val generatingStatus: Int = 0,
    val iconStatusType: Int = -1,
    val leftDoubleLine: List<String> = emptyList(),
    val rightDoubleLine: List<String> = emptyList(),
    val buttonTitles: List<String> = emptyList(),
    val buttonBgColors: List<Int> = emptyList(),
    val buttonTextColors: List<Int> = emptyList(),
    val customTemplate: RemoteViews? = null,
    val waveState: Int = 1,
    val waveColorList: List<String>? = null,
    val changeRecord: Int = 0,
    /**
     * The native vivo music-widget card posts its SuperX notification with this false and never
     * shows in the shade. Kept per-request rather than hardcoded so the experiment stays possible.
     */
    val showNotify: Boolean = true,
    val islandNotify: Boolean = false,
    val priority: Int = 0,
    val landingPkg: String? = null,
    val landingInfo: String? = null,
)
