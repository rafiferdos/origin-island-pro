package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator

/**
 * Calendar and Meeting Reminders Card for Origin Island Pro.
 * Intercepts events from Google Calendar, Outlook, Meet, Zoom, Teams, etc.
 * Surfaces a glanceable countdown chip (e.g. "In 10m", "Now", "3:00 PM") on the island pill
 * and provides one-tap access to join or open the meeting.
 */
object MeetingCard {

    val MEETING_APPS = setOf(
        "com.google.android.calendar",
        "com.microsoft.office.outlook",
        "com.google.android.apps.meetings", // Google Meet
        "us.zoom.videomeetings",             // Zoom
        "com.microsoft.teams",               // Microsoft Teams
        "com.cisco.webex.meetings",          // Webex
        "com.larksuite.suite",               // Lark
        "notion.id",
    )

    val MEETING_KEYWORDS = listOf(
        "meeting", "starts in", "in 10 min", "in 5 min", "in 15 min",
        "starts now", "happening now", "zoom meeting", "google meet",
        "calendar event", "webinar", "team sync", "standup", "reminder for",
    )

    fun post(
        context: Context,
        id: Int,
        title: String,
        timeSnippet: String,
        details: String,
        appLabel: String,
        appIcon: Icon?,
        clickResp: PendingIntent?,
        actions: List<Notification.Action> = emptyList(),
        sourcePkg: String? = null,
    ) {
        val cleanTitle = IntelligentTextSanitizer.cleanTitle(title, appLabel)
        val cleanDetails = IntelligentTextSanitizer.cleanBody(details, cleanTitle, appLabel)
        val usable = actions.filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(2)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            sourcePkg?.let { putExtra("source_pkg", it) }
            putExtra("is_ongoing", false)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_MEETING)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template",
                if (usable.isNotEmpty()) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", cleanTitle)
            putExtra("text", cleanDetails.ifBlank { timeSnippet })
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)

            // Short/compact pill
            putExtra("oi_left_content", cleanTitle.take(12))
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            putExtra("oi_right_content", timeSnippet)
            putExtra("status_chip_text", timeSnippet)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("source_content_intent", clickResp)
            if (usable.isNotEmpty()) putParcelableArrayListExtra("actions", ArrayList(usable))
        }
        context.startService(intent)
    }
}
