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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated Next-Level Download & File Transfer Card for Origin Island Pro.
 * Intercepts active downloads from Chrome, Telegram, ADM, 1DM, Google Play, etc.
 * Features:
 * - Displays source app launcher icon (Play Store, Chrome, Telegram) on the compact island pill.
 * - Real-time throughput speed measured per second in Megabits (Mbps), NOT bytes.
 * - Normalized island width: prevents awkward stretching by keeping the pill compact.
 * - Clean expanded progress bar without repeating 4-icon clutter.
 */
object DownloadCard {

    val DOWNLOAD_APPS = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "org.telegram.messenger",
        "com.android.vending", // Google Play Store
        "com.dv.adm",          // Advanced Download Manager
        "idm.internet.download.manager", // 1DM
        "com.google.android.apps.docs",
    )

    private val SPEED_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(?:KB/s|MB/s|kb/s|mb/s|kB/s|mB/s|Mbps|mbps|Kbps|kbps)""")
    private val SIZE_REGEX = Regex("""(\d+(?:\.\d+)?\s*(?:MB|GB|KB))\s*(?:/|of)\s*(\d+(?:\.\d+)?\s*(?:MB|GB|KB))""")

    private data class ProgressSnapshot(val percent: Int, val timeMs: Long)
    private val lastSnapshots = ConcurrentHashMap<Int, ProgressSnapshot>()

    fun isDownload(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val extras = n.extras
        val progressMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        if (progressMax <= 0 || indeterminate) return false

        val pkg = sbn.packageName
        if (pkg in DOWNLOAD_APPS) return true

        val text = "${extras.getCharSequence(NotificationCompat.EXTRA_TITLE)} ${extras.getCharSequence(NotificationCompat.EXTRA_TEXT)}".lowercase()
        return listOf("downloading", "download", "transferring", "updating", "mb/s", "kb/s", "mbps", "exporting").any { text.contains(it) }
    }

    /**
     * Converts any raw speed string (MB/s, KB/s, Mbps) to Megabits per second (Mbps).
     * 1 MB/s = 8.0 Mbps, 1 KB/s = 0.008 Mbps.
     */
    fun convertToMbps(text: String): String? {
        val match = SPEED_REGEX.find(text) ?: return null
        val num = match.groupValues[1].toDoubleOrNull() ?: return null
        val raw = match.value.lowercase()
        val mbps = when {
            raw.contains("mbps") -> num
            raw.contains("kbps") -> num / 1000.0
            raw.contains("mb/s") || raw.contains("m/s") -> num * 8.0
            raw.contains("kb/s") || raw.contains("k/s") -> (num * 8.0) / 1000.0
            else -> num * 8.0
        }
        return formatMbps(mbps)
    }

    fun extractCompactMbps(text: String): String? {
        val match = SPEED_REGEX.find(text) ?: return null
        val num = match.groupValues[1].toDoubleOrNull() ?: return null
        val raw = match.value.lowercase()
        val mbps = when {
            raw.contains("mbps") -> num
            raw.contains("kbps") -> num / 1000.0
            raw.contains("mb/s") || raw.contains("m/s") -> num * 8.0
            raw.contains("kb/s") || raw.contains("k/s") -> (num * 8.0) / 1000.0
            else -> num * 8.0
        }
        return formatCompactMbps(mbps)
    }

    private fun formatMbps(mbps: Double): String = when {
        mbps >= 100.0 -> String.format(Locale.US, "%.0f Mbps", mbps)
        mbps >= 1.0 -> String.format(Locale.US, "%.1f Mbps", mbps)
        else -> String.format(Locale.US, "%.0f Kbps", mbps * 1000.0)
    }

    private fun formatCompactMbps(mbps: Double): String = when {
        mbps >= 100.0 -> "${mbps.toInt()}M"
        mbps >= 1.0 -> String.format(Locale.US, "%.1fM", mbps)
        else -> "${(mbps * 1000.0).toInt()}K"
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

        val mbpsSpeed = convertToMbps(combined)
        val compactSpeed = extractCompactMbps(combined)

        // Clean file name
        val rawName = rawTitle.ifBlank { rawText }
        val cleanName = IntelligentTextSanitizer.cleanTitle(rawName, appLabel)
            .replace(Regex("""^(?:Downloading|Download)\s+"""), "")

        // Source App Launcher Icon (e.g. Google Play Store icon, Chrome icon, Telegram icon)
        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull()

        // Compact pill left content: compact speed in Mbps (e.g. "19.2M" or "19.2 Mbps")
        // Kept short to prevent wide bloated island width!
        val leftContent = compactSpeed?.let { "$it/s" } ?: run {
            val dotExt = cleanName.substringAfterLast('.', "")
            if (dotExt.length in 2..4 && !dotExt.contains(" ")) ".$dotExt" else ""
        }

        // Expanded card details: Mbps speed prominently surfaced
        val details = when {
            mbpsSpeed != null && cleanName.isNotBlank() -> "$percent% • $cleanName • $mbpsSpeed"
            cleanName.isNotBlank() -> "$percent% • $cleanName"
            else -> "$percent% • $appLabel"
        }

        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        appIcon?.let {
            IconCache.activeLargeIcons[id] = it
        }

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

            // Normalized compact pill:
            // Display source app's colorful launcher icon (Play Store, Chrome, Telegram)
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            putExtra("oi_left_content", leftContent)
            putExtra("status_chip_text", "$percent%")
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("source_content_intent", clickIntent)

            if (actions.isNotEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(actions))
            }
        }
        context.startService(intent)
    }
}
