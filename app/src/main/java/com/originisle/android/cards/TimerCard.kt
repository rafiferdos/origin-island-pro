package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    val CLOCK_APPS = setOf(
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.vivo.clock",
        "com.coloros.alarmclock",
        "com.android.deskclock",
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
        }.take(2)

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

            // Short compact pill
            putExtra("oi_left_content", cleanTitle.take(8))
            val timerIcon = Icon.createWithResource(context, R.drawable.ic_timer)
            putExtra("oi_left_icon", timerIcon)
            putExtra("oi_large_icon", timerIcon)
            putExtra("oi_right_content", timeFormatted)
            putExtra("status_chip_text", timeFormatted)
            putExtra("icon_res", R.drawable.ic_timer)
            putExtra("source_content_intent", clickResp)
            if (usable.isNotEmpty()) putParcelableArrayListExtra("actions", ArrayList(usable))
        }
        context.startService(intent)
    }
}
