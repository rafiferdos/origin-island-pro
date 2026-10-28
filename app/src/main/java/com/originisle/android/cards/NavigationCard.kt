package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.island.CutoutConfig
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator

/**
 * Turn-by-turn navigation (Maps, anything posting `CATEGORY_NAVIGATION`) cast onto vivo's own
 * driving-navigation card — template 9 — instead of the generic base card.
 *
 * Waze is not among them: it publishes no turn data to the notification system at all (see
 * `NAV_APPS` in [com.originisle.android.service.NotificationCastListener]), so there is nothing
 * here to build a card from.
 *
 * Template 9 is what OriginOS uses for its native navigation island: a highlighted maneuver line, a
 * normal-weight street line under it, a sub-text lane for the journey summary, and the maneuver
 * arrow as the card icon. [com.originisle.android.island.OriginIslandBuilder] has always been able
 * to build it; until now nothing ever asked for it.
 *
 * The default, unconditional path for any recognised navigation notification — confirmed rendering
 * correctly on hardware, so there's no toggle/fallback to [GenericCard] to opt out of it anymore.
 */
object NavigationCard {

    /**
     * A journey summary is typically "12 min · 4,5 km · 14:32" — the pill only has room for the
     * leading figure, and that's the one worth showing.
     */
    private val SUMMARY_SEPARATORS = charArrayOf('·', '•', '|')

    fun post(context: Context, sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras
        val id = IconCache.castIdFor(sbn)

        val appLabel = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0),
            ).toString()
        }.getOrDefault(sbn.packageName)

        // Maps deliberately leaves the title empty on some frames and renders its own custom view;
        // the app's name beats the literal word "Notification" as a fallback.
        val maneuver = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            .ifBlank { appLabel }
        val street = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            .ifBlank { extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty() }
        val summary = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        // 1. Pill Left Content: Clean, glanceable distance + action (never overflows camera punch-hole)
        val leftLimit = CutoutConfig.getPillLeftLimit(context)
        val pillLeft = IntelligentTextSanitizer.formatNavigationPill(maneuver, street, maxChars = leftLimit)

        // 2. Pill Chip: ETA / Duration
        val shortCritical = extras.getCharSequence("android.shortCriticalText")?.toString()?.trim().orEmpty()
        val chip = shortCritical.ifBlank { summary.leadingFigure() }.ifBlank { maneuver }

        // 3. Secondary Lane Assist advice for Template 9
        val navAssist = IntelligentTextSanitizer.extractNavigationAssist(maneuver, street)

        // 4. Maneuver Icons with fallback
        val fallbackIcon = Icon.createWithResource(context, R.drawable.ic_navigation)
        val maneuverIcon: Icon? = when (val large = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> large
            is Bitmap -> Icon.createWithBitmap(shrinkForIcon(large))
            else -> null
        }
        val pillIcon: Icon = maneuverIcon?.let { runCatching { whiteArrow(context, it) }.getOrNull() }
            ?: fallbackIcon
        val cardIcon: Icon = maneuverIcon ?: fallbackIcon
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }

        // 5. Traffic-aware dynamic badge color & ambient glow:
        // Amber for slowdowns/delays, Vibrant Electric Navigation Emerald for clear routes
        val isHeavyTraffic = summary.contains(Regex("""(?i)\b(?:delay|\+\d+\s*min|slow|heavy|congestion)\b""")) ||
            street.contains(Regex("""(?i)\b(?:slowdown|congestion|heavy traffic)\b"""))
        val navColor = if (isHeavyTraffic) "#FFFF9500" else "#FF00C853" // Electric Navigation Emerald

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_NAVIGATION)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_DRIVING_NAVI)                    // 9
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6

            // Template 9 structure:
            // title -> Main highlight text (maneuver)
            // text -> Main normal text (street)
            // oi_nav_msg -> Journey subtext (ETA, distance, arrival clock)
            // oi_nav_assist -> Secondary turn/lane assist advice
            putExtra("title", maneuver)
            putExtra("text", street.ifBlank { summary }.ifBlank { appLabel })
            putExtra("subtext", summary)
            putExtra("oi_nav_msg", summary.ifBlank { street })
            navAssist?.let { putExtra("oi_nav_assist", it) }

            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_landing_pkg", sbn.packageName)
            putExtra("oi_landing_info", sbn.packageName)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)

            // Compact Pill: Maneuver Arrow + Distance/Action on left, Emerald ETA badge on right
            putExtra("oi_left_content", pillLeft)
            putExtra("oi_left_icon", pillIcon)
            putExtra("oi_large_icon", cardIcon)
            putExtra("oi_right_content", chip)
            putExtra("status_chip_text", chip)
            putExtra("oi_bg_color", navColor)
            putExtra("oi_fg_color", "#FFFFFFFF")

            // Ambient Edge Lighting Effect around the expanded card
            putExtra("oi_light_color", navColor)
            putExtra("oi_light_mode", 1)

            putExtra("icon_res", R.drawable.ic_navigation)
            putExtra("when", n.`when`)
            // vivo requires a content intent; extract direct nested deep-link or fall back to launching the source app.
            putExtra("source_content_intent", extractDeepLinkIntent(context, sbn))
            n.actions?.let { putParcelableArrayListExtra("actions", ArrayList(it.toList())) }
        }
        context.startService(intent)
    }

    private fun whiteArrow(context: Context, icon: Icon): Icon {
        val drawable = icon.loadDrawable(context) ?: return icon
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(Canvas(bitmap))
        return Icon.createWithBitmap(shrinkForIcon(bitmap)).setTint(Color.WHITE)
    }

    /** "12 min · 4,5 km · 14:32" -> "12 min". Blank in, blank out. */
    private fun String.leadingFigure(): String =
        split(*SUMMARY_SEPARATORS).firstOrNull()?.trim().orEmpty()
}
