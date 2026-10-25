package com.originisle.android.cards

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator

/**
 * Dedicated Next-Level Download & File Transfer Card for Origin Island Pro.
 * Intercepts active downloads from Chrome, Telegram, ADM, 1DM, Google Play, etc.
 * Normalizes island width: prevents awkward stretching by keeping the left pill compact
 * (download icon + speed/extension) and rendering a smooth circular progress ring on the right.
 */
object DownloadCard {

    val DOWNLOAD_APPS = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "org.telegram.messenger",
        "com.android.vending", // Play Store
        "com.dv.adm",          // Advanced Download Manager
        "idm.internet.download.manager", // 1DM
        "com.google.android.apps.docs",
    )

    private val SPEED_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(?:KB/s|MB/s|kb/s|mb/s|kB/s|mB/s)""")
    private val SIZE_REGEX = Regex("""(\d+(?:\.\d+)?\s*(?:MB|GB|KB))\s*(?:/|of)\s*(\d+(?:\.\d+)?\s*(?:MB|GB|KB))""")

    fun isDownload(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val extras = n.extras
        val progressMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        if (progressMax <= 0 || indeterminate) return false

        val pkg = sbn.packageName
        if (pkg in DOWNLOAD_APPS) return true

        val text = "${extras.getCharSequence(NotificationCompat.EXTRA_TITLE)} ${extras.getCharSequence(NotificationCompat.EXTRA_TEXT)}".lowercase()
        return listOf("downloading", "download", "transferring", "updating", "mb/s", "kb/s", "exporting").any { text.contains(it) }
    }

    fun extractSpeed(text: String): String? {
        val match = SPEED_REGEX.find(text) ?: return null
        return match.value.replace(" ", "")
    }

    fun extractCompactSpeed(text: String): String? {
        val match = SPEED_REGEX.find(text) ?: return null
        val num = match.groupValues[1]
        val unit = if (match.value.contains("M", ignoreCase = true)) "M/s" else "K/s"
        return "$num$unit"
    }

    fun post(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ) {
        val n = sbn.notification
        val extras = n.extras
        val id = IconCache.castIdFor(sbn)

        val progress = extras.getInt(NotificationCompat.EXTRA_PROGRESS, 0)
        val progressMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 100)
        val percent = if (progressMax > 0) (progress * 100 / progressMax).coerceIn(0, 100) else 0

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val combined = "$rawTitle $rawText $bigText"

        val speed = extractSpeed(combined)
        val compactSpeed = extractCompactSpeed(combined)

        // Clean file name
        val rawName = rawTitle.ifBlank { rawText }
        val cleanName = IntelligentTextSanitizer.cleanTitle(rawName, appLabel)
            .replace(Regex("""^(?:Downloading|Download)\s+"""), "")

        // Compact pill left content: compact speed (e.g. 2.4M/s) or short file extension or empty
        // to prevent wide bloated island width!
        val leftContent = compactSpeed ?: run {
            val dotExt = cleanName.substringAfterLast('.', "")
            if (dotExt.length in 2..4 && !dotExt.contains(" ")) ".$dotExt" else ""
        }

        // Expanded card details
        val details = when {
            speed != null && cleanName.isNotBlank() -> "$percent% • $cleanName • $speed"
            cleanName.isNotBlank() -> "$percent% • $cleanName"
            else -> "$percent% • $appLabel"
        }

        // Cache small icon
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull()

        val actions = n.actions.orEmpty().filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(2)

        val clickIntent = n.contentIntent ?: fallbackClickIntent(context, sbn.packageName)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_DELIVERY)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
            putExtra("show_progress", true)
            putExtra("progress", progress)
            putExtra("progress_max", progressMax)
            putExtra("title", cleanName.ifBlank { "Downloading" })
            putExtra("text", details)
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            // Normalized compact pill
            putExtra("oi_left_content", leftContent)
            putExtra("status_chip_text", "$percent%")
            putExtra("icon_res", R.drawable.ic_download)
            appIcon?.let { putExtra("oi_left_icon", it) }
            putExtra("source_content_intent", clickIntent)

            if (actions.isNotEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(actions))
            }
        }
        context.startService(intent)
    }
}
