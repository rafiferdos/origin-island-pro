package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.PriorityArbitrator

/**
 * Clock, Timer & Stopwatch Live Card for Origin Island Pro.
 * Intercepts active timers and countdowns from Google Clock, Samsung Clock, vivo Clock, etc.
 * Surfaces live remaining time and interactive pause/stop controls directly on the island.
 */
object TimerCard {

    const val TIMER_COLOR_HEX = "#FFFF9500" // Vibrant Tangerine / Amber
    const val TIMER_TEXT_COLOR_HEX = "#FFFFFFFF"

    val CLOCK_APPS = setOf(
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.vivo.clock",
        "com.bbk.clock",
        "com.coloros.alarmclock",
        "com.android.deskclock",
        "com.oneplus.deskclock",
        "com.xiaomi.clock",
        "com.huawei.deskclock",
        "com.motorola.blur.alarmclock",
    )

    fun post(
        context: Context,
        id: Int,
        title: String,
        timeFormatted: String,
        appLabel: String,
        clickResp: PendingIntent?,
        actions: List<Notification.Action> = emptyList(),
        sourcePkg: String? = null,
    ) {
        val cleanTitle = title.ifBlank { "Timer" }
        val usable = actions.filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(3)

        val btnTitles = ArrayList<String>()
        val btnBgColors = ArrayList<Int>()
        val btnTxtColors = ArrayList<Int>()

        usable.forEach { a ->
            val aTitle = a.title?.toString()?.trim().orEmpty()
            btnTitles.add(aTitle)
            val lower = aTitle.lowercase()
            val (bg, fg) = when {
                lower.contains("pause") || lower.contains("snooze") -> 0xFFFF9500.toInt() to 0xFFFFFFFF.toInt() // Radiant Amber
                lower.contains("resume") || lower.contains("start") -> 0xFF34C759.toInt() to 0xFFFFFFFF.toInt() // Emerald Green
                lower.contains("reset") || lower.contains("stop") || lower.contains("cancel") || lower.contains("dismiss") || lower.contains("delete") -> 0xFFFF3B30.toInt() to 0xFFFFFFFF.toInt() // Crimson Red
                lower.contains("+") || lower.contains("add") -> 0xFF1C2B4E.toInt() to 0xFF4D88FF.toInt() // Deep Blue Accent
                else -> 0xFF2C2C2E.toInt() to 0xFFFFFFFF.toInt() // Frosted Slate
            }
            btnBgColors.add(bg)
            btnTxtColors.add(fg)
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            sourcePkg?.let { putExtra("source_pkg", it) }
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_TIMER)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template",
                if (usable.isNotEmpty()) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", cleanTitle)
            putExtra("text", "$timeFormatted remaining")
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)

            // Short compact pill: Punch-hole safe left content (max 10 chars)
            val pillLeft = cleanTitle.take(10).trim()
            putExtra("oi_left_content", pillLeft)
            val timerIcon = Icon.createWithResource(context, R.drawable.ic_timer)
            putExtra("oi_left_icon", timerIcon)
            putExtra("oi_large_icon", timerIcon)
            putExtra("oi_right_content", timeFormatted)
            putExtra("status_chip_text", timeFormatted)

            // Dynamic High-Contrast Capsule Badge Colors
            putExtra("oi_bg_color", TIMER_COLOR_HEX)
            putExtra("oi_fg_color", TIMER_TEXT_COLOR_HEX)

            // Ambient Edge Glow around expanded card matching timer color
            putExtra("oi_light_color", TIMER_COLOR_HEX)
            putExtra("oi_light_mode", 1)

            putExtra("icon_res", R.drawable.ic_timer)
            putExtra("source_content_intent", clickResp)
            if (usable.isNotEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(usable))
                putStringArrayListExtra("oi_button_titles", btnTitles)
                putIntegerArrayListExtra("oi_button_bg_colors", btnBgColors)
                putIntegerArrayListExtra("oi_button_text_colors", btnTxtColors)
            }
        }
        context.startService(intent)
    }
}
