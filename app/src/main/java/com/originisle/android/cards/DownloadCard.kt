package com.originisle.android.cards

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.TrafficStats
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
 * Universal Intelligent Download & File Transfer Engine for Origin Island Pro.
 * Automatically detects downloads from ANY app in the world (Games, Browsers, Cloud, Messengers, etc.).
 * Features:
 * - Displays the authentic source app launcher icon (e.g. Genshin Impact, Chrome, Play Store, Telegram) on the compact pill.
 * - Real-time throughput speed measured per second in Megabits (Mbps), using both text parsing and live TrafficStats network accounting.
 * - Compact right-side loading ring / progress indicator.
 * - Meaningful expanded card with formatted size, percentage, live Mbps speed, and ETA.
 * - Clean uninterrupted progress bar without clutter.
 */
object DownloadCard {

    val DOWNLOAD_APPS = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.sec.android.app.sbrowser",
        "org.telegram.messenger",
        "com.android.vending", // Google Play Store
        "com.dv.adm",          // Advanced Download Manager
        "idm.internet.download.manager", // 1DM
        "com.google.android.apps.docs",
        "com.miHoYo.GenshinImpact",
        "com.cognosphere.genshinimpact.oversea",
        "com.tencent.ig",      // PUBG Mobile
        "com.pubg.imobile",
        "com.dts.freefireth",  // Free Fire
        "com.dts.freefiremax",
    )

    private val DOWNLOAD_KEYWORDS = listOf(
        "download", "downloading", "downloaded",
        "transfer", "transferring", "transferred",
        "updating", "update", "patching", "patch",
        "installing", "extracting", "unpacking", "fetching", "syncing", "exporting",
        "resources", "game data", "assets", "package", "obb",
        "mb/s", "kb/s", "gb/s", "mbps", "kbps", "m/s", "k/s",
        "ডাউনলোড", "আপডেট", "ট্রান্সফার",
    )

    private val SPEED_REGEX = Regex(
        """(?i)(\d+(?:\.\d+)?)\s*(GB/s|GiB/s|MB/s|MiB/s|KB/s|KiB/s|B/s|Gbps|Mbps|Kbps|g/s|m/s|k/s|MBps|KBps)"""
    )
    private val SIZE_REGEX = Regex("""(?i)(\d+(?:\.\d+)?\s*(?:MB|GB|KB|TB))\s*(?:/|of)\s*(\d+(?:\.\d+)?\s*(?:MB|GB|KB|TB))""")
    private val ETA_REGEX = Regex("""(?i)(\d+\s*(?:min|mins|minute|minutes|sec|secs|second|seconds|hr|hrs|hour|hours|s|m|h)\s*(?:remaining|left))|((?:about|approx\.?)\s*\d+\s*(?:min|mins|hour|hrs)\s*left)""")

    data class SpeedTrackerState(
        var baselineBytes: Long = -1L,
        var baselineTimeMs: Long = 0L,
        var smoothedSpeedMbps: Double = 0.0,
        var lastNonZeroSpeedMbps: Double = 0.0,
        var lastNonZeroTimeMs: Long = 0L,
        var isTrackingUid: Boolean = false,
    )
    private val speedTrackers = ConcurrentHashMap<Int, SpeedTrackerState>()

    /** Drop dedupe state and traffic sample for [id]. */
    fun forget(id: Int) {
        speedTrackers.remove(id)
    }

    /**
     * Universal auto-detection for ANY download, file transfer, or asset downloader notification
     * from ANY app across the globe without requiring manual hardcoded package lists.
     */
    fun isDownload(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val extras = n.extras
        val progressMax = extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        val hasProgress = progressMax > 0 || indeterminate

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val infoText = extras.getCharSequence(NotificationCompat.EXTRA_INFO_TEXT)?.toString()?.trim().orEmpty()
        val combined = "$rawTitle $rawText $bigText $subText $infoText".lowercase(Locale.ROOT)

        val hasDownloadKeyword = DOWNLOAD_KEYWORDS.any { combined.contains(it) }
        val hasSpeed = SPEED_REGEX.containsMatchIn(combined)
        val hasSize = SIZE_REGEX.containsMatchIn(combined)
        val isProgressCategory = n.category == NotificationCompat.CATEGORY_PROGRESS ||
            n.category == NotificationCompat.CATEGORY_STATUS

        val pkg = sbn.packageName
        if (pkg in DOWNLOAD_APPS && (hasProgress || hasDownloadKeyword || hasSpeed || hasSize)) {
            return true
        }

        // Universal auto-detection:
        // 1. Explicit progress accompanied by keyword, speed, size or category
        if (hasProgress && (hasDownloadKeyword || hasSpeed || hasSize || isProgressCategory)) {
            return true
        }

        // 2. Ongoing notification with explicit speed or size regex
        val isOngoing = sbn.isOngoing || (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing && (hasSpeed || hasSize)) {
            return true
        }

        // 3. Ongoing notification with download keywords and percent sign
        if (isOngoing && hasDownloadKeyword && (hasProgress || combined.contains("%"))) {
            return true
        }

        return false
    }

    /**
     * Parses a speed string from notification text into standard Megabits per second (Mbps).
     * 1 MB/s = 8.0 Mbps, 1 KB/s = 0.008 Mbps, 1 GB/s = 8000.0 Mbps.
     */
    fun parseSpeedMbps(text: String): Double? {
        val match = SPEED_REGEX.find(text) ?: return null
        val num = match.groupValues[1].toDoubleOrNull() ?: return null
        if (num <= 0.0001) return 0.0
        val unit = match.groupValues[2].lowercase(Locale.ROOT)
        return when {
            unit.startsWith("gb") || unit.startsWith("gib") || unit == "g/s" -> num * 8000.0
            unit == "mbps" -> num
            unit == "kbps" -> num / 1000.0
            unit.startsWith("mb") || unit.startsWith("mib") || unit == "m/s" -> num * 8.0
            unit.startsWith("kb") || unit.startsWith("kib") || unit == "k/s" -> (num * 8.0) / 1000.0
            unit == "b/s" -> (num * 8.0) / 1_000_000.0
            else -> num * 8.0
        }
    }

    /**
     * Extracts non-zero formatted raw speed string (e.g. "2.4 MB/s") directly from text if present.
     */
    fun extractRawSpeedText(text: String): String? {
        val match = SPEED_REGEX.find(text) ?: return null
        val num = match.groupValues[1].toDoubleOrNull() ?: return null
        if (num <= 0.0001) return null // Filter out momentary 0.00 text speeds
        return match.value.trim()
    }

    /**
     * Converts raw speed strings (MB/s, KB/s, Mbps) to standard formatted Megabits per second (Mbps).
     */
    fun convertToMbps(text: String): String? {
        val mbps = parseSpeedMbps(text) ?: return null
        if (mbps <= 0.0001) return null
        return formatMbps(mbps)
    }

    fun extractCompactMbps(text: String): String? {
        val mbps = parseSpeedMbps(text) ?: return null
        if (mbps <= 0.0001) return null
        return formatCompactMbps(mbps)
    }

    fun formatMbps(mbps: Double): String = when {
        mbps >= 100.0 -> String.format(Locale.US, "%.0f Mbps", mbps)
        mbps >= 1.0 -> String.format(Locale.US, "%.1f Mbps", mbps)
        mbps > 0.05 -> String.format(Locale.US, "%.0f Kbps", mbps * 1000.0)
        else -> "0 Kbps"
    }

    fun formatCompactMbps(mbps: Double): String = when {
        mbps >= 100.0 -> "${mbps.toInt()}M"
        mbps >= 1.0 -> String.format(Locale.US, "%.1fM", mbps)
        mbps > 0.05 -> "${(mbps * 1000.0).toInt()}K"
        else -> "0K"
    }

    /**
     * Formats speed respecting OriginOS 7 status-bar conventions:
     * - "mbs": MB/s and KB/s format (OriginOS system standard).
     * - "mbps": standard Mbps format.
     */
    fun formatSpeed(mbps: Double, unitMode: String = "auto"): String {
        if (mbps <= 0.05) {
            return if (unitMode == "mbs") "0 KB/s" else "0 Kbps"
        }
        return if (unitMode == "mbs") {
            val mbPerSec = mbps / 8.0
            val kbPerSec = (mbps * 1000.0) / 8.0
            when {
                mbPerSec >= 100.0 -> String.format(Locale.US, "%.0f MB/s", mbPerSec)
                mbPerSec >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mbPerSec)
                kbPerSec >= 1.0 -> String.format(Locale.US, "%.0f KB/s", kbPerSec)
                else -> String.format(Locale.US, "%.0f B/s", (mbps * 1_000_000.0) / 8.0)
            }
        } else {
            when {
                mbps >= 100.0 -> String.format(Locale.US, "%.0f Mbps", mbps)
                mbps >= 1.0 -> String.format(Locale.US, "%.1f Mbps", mbps)
                else -> String.format(Locale.US, "%.0f Kbps", mbps * 1000.0)
            }
        }
    }

    /**
     * Measures and computes live download speed with exponential moving average and anti-jitter hold.
     * Prevents false "0.00" dips during TCP window gaps and rapid notification bursts.
     */
    fun computeLiveSpeed(
        context: Context,
        sbn: StatusBarNotification,
        id: Int,
        combinedText: String,
    ): String {
        val now = System.currentTimeMillis()
        val tracker = speedTrackers.computeIfAbsent(id) { SpeedTrackerState() }

        // 1. Text-based speed extraction
        val rawTextSpeed = extractRawSpeedText(combinedText)
        val textSpeedMbps = parseSpeedMbps(combinedText)

        // 2. Measure device/UID traffic stats
        val appUid = runCatching { context.packageManager.getPackageUid(sbn.packageName, 0) }.getOrNull() ?: sbn.uid
        val uidRx = if (appUid > 0) TrafficStats.getUidRxBytes(appUid) else -1L
        val uidTx = if (appUid > 0) TrafficStats.getUidTxBytes(appUid).takeIf { it > 0 } ?: 0L else 0L

        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes().takeIf { it > 0 } ?: 0L

        val currentBytes = if (uidRx > 0) {
            tracker.isTrackingUid = true
            uidRx + uidTx
        } else {
            if (tracker.isTrackingUid && tracker.baselineBytes > 0) {
                tracker.isTrackingUid = false
                tracker.baselineBytes = totalRx + totalTx
                tracker.baselineTimeMs = now
            }
            if (totalRx > 0) totalRx + totalTx else -1L
        }

        // Process TrafficStats throughput
        if (currentBytes > 0) {
            if (tracker.baselineBytes <= 0 || tracker.baselineTimeMs <= 0) {
                tracker.baselineBytes = currentBytes
                tracker.baselineTimeMs = now
            } else {
                val deltaMs = now - tracker.baselineTimeMs
                // Crucial fix: Only compute when deltaMs >= 400ms.
                // When deltaMs < 400ms, DO NOT overwrite baselineTimeMs or baselineBytes!
                // Let time and bytes accumulate across frequent notification bursts.
                if (deltaMs >= 400) {
                    val deltaBytes = currentBytes - tracker.baselineBytes
                    tracker.baselineBytes = currentBytes
                    tracker.baselineTimeMs = now

                    if (deltaBytes > 0 && deltaMs in 400..30000) {
                        val bytesPerSec = (deltaBytes * 1000.0) / deltaMs
                        val instantMbps = (bytesPerSec * 8.0) / 1_000_000.0

                        if (instantMbps > 0.05) {
                            tracker.lastNonZeroTimeMs = now
                            tracker.lastNonZeroSpeedMbps = instantMbps
                            tracker.smoothedSpeedMbps = if (tracker.smoothedSpeedMbps <= 0.05) {
                                instantMbps
                            } else {
                                // Exponential Moving Average (EMA) to smooth out network bursts
                                0.45 * instantMbps + 0.55 * tracker.smoothedSpeedMbps
                            }
                        }
                    } else if (deltaBytes <= 0) {
                        // Instantaneous zero in this sample slice
                        val timeSinceActive = now - tracker.lastNonZeroTimeMs
                        if (timeSinceActive < 3500L && tracker.lastNonZeroSpeedMbps > 0.05) {
                            // Anti-flicker: gently decay instead of dropping straight to 0.00!
                            tracker.smoothedSpeedMbps = (tracker.smoothedSpeedMbps * 0.90).coerceAtLeast(0.0)
                        } else {
                            tracker.smoothedSpeedMbps = 0.0
                        }
                    }
                }
            }
        }

        val prefs = context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, 0)
        val unitPref = prefs.getString("download_speed_unit", "auto") ?: "auto"

        // Decision matrix:
        // Priority A: If text explicitly reports a non-zero speed (e.g. "3.5 MB/s" or "28 Mbps")
        if (textSpeedMbps != null && textSpeedMbps > 0.05) {
            tracker.lastNonZeroTimeMs = now
            tracker.lastNonZeroSpeedMbps = textSpeedMbps
            tracker.smoothedSpeedMbps = if (tracker.smoothedSpeedMbps <= 0.05) {
                textSpeedMbps
            } else {
                0.5 * textSpeedMbps + 0.5 * tracker.smoothedSpeedMbps
            }

            if (unitPref == "auto" && rawTextSpeed != null) {
                return rawTextSpeed
            }
            return formatSpeed(tracker.smoothedSpeedMbps, unitPref)
        }

        // Priority B: Measured TrafficStats throughput is available and active
        if (tracker.smoothedSpeedMbps > 0.05) {
            return formatSpeed(tracker.smoothedSpeedMbps, unitPref)
        }

        // Priority C: Active within the last 3.5 seconds (anti-flicker hold)
        if (now - tracker.lastNonZeroTimeMs < 3500L && tracker.lastNonZeroSpeedMbps > 0.05) {
            return formatSpeed(tracker.lastNonZeroSpeedMbps * 0.90, unitPref)
        }

        // Priority D: Connecting or idle
        return if (unitPref == "mbs") "0 KB/s" else "0 Kbps"
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
        val indeterminate = extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE, false)
        val percent = if (progressMax > 0 && !indeterminate) (progress * 100 / progressMax).coerceIn(0, 100) else 0

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val infoText = extras.getCharSequence(NotificationCompat.EXTRA_INFO_TEXT)?.toString()?.trim().orEmpty()
        val combined = "$rawTitle $rawText $bigText $subText $infoText"

        // Compute intelligent live speed with anti-flicker smoothing
        val liveSpeedText = computeLiveSpeed(context, sbn, id, combined)

        // Authentic Source App Launcher Icon (e.g. Genshin Impact, Chrome, Play Store, Telegram)
        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull() ?: n.smallIcon ?: Icon.createWithResource(context, R.drawable.ic_download)

        // Extract clean item/game/file name
        val rawName = rawTitle.ifBlank { rawText }
        val cleanName = IntelligentTextSanitizer.cleanTitle(rawName, appLabel)
            .replace(Regex("""^(?:Downloading|Download|Updating|Update)\s+"""), "")
            .trim()
        val displayTitle = if (cleanName.isNotBlank() && cleanName != "Downloading") cleanName else appLabel

        // Meaningful expanded details: size, speed, and ETA
        val sizeMatch = SIZE_REGEX.find(combined)
        val etaMatch = ETA_REGEX.find(combined)

        val sizePart = sizeMatch?.value ?: if (percent > 0) "$percent%" else ""
        val speedPart = liveSpeedText
        val etaPart = etaMatch?.value ?: ""

        val detailParts = listOf(sizePart, speedPart, etaPart).filter { it.isNotBlank() }
        val details = when {
            detailParts.isNotEmpty() -> detailParts.joinToString(" • ")
            rawText.isNotBlank() && rawText != displayTitle -> rawText
            else -> "$percent% • $appLabel"
        }

        val style = context.getSharedPreferences(com.originisle.android.ui.PREFS_NAME, 0)
            .getString("download_pill_style", "speed_ring") ?: "speed_ring"

        val pillConfig = resolvePillConfig(
            style = style,
            displayTitle = displayTitle,
            percent = percent,
            indeterminate = indeterminate,
            progressMax = progressMax,
            liveMbpsSpeed = liveSpeedText,
        )

        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        IconCache.activeLargeIcons[id] = appIcon

        val actions = n.actions.orEmpty().filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(2)

        val clickIntent = extractDeepLinkIntent(context, sbn)
        val downloadColor = "#FF007AFF" // Electric Download Blue

        val btnTitles = ArrayList<String>()
        val btnBgColors = ArrayList<Int>()
        val btnTxtColors = ArrayList<Int>()

        actions.forEach { a ->
            val title = a.title?.toString()?.trim().orEmpty()
            btnTitles.add(title)
            val lower = title.lowercase()
            val (bg, fg) = when {
                lower.contains("pause") -> 0xFFFF9500.toInt() to 0xFFFFFFFF.toInt() // Radiant Amber
                lower.contains("cancel") || lower.contains("stop") || lower.contains("delete") -> 0xFFFF3B30.toInt() to 0xFFFFFFFF.toInt() // Crimson Red
                lower.contains("resume") || lower.contains("start") -> 0xFF34C759.toInt() to 0xFFFFFFFF.toInt() // Emerald Green
                else -> 0xFF2C2C2E.toInt() to 0xFFFFFFFF.toInt()
            }
            btnBgColors.add(bg)
            btnTxtColors.add(fg)
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_DELIVERY)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)

            putExtra("oi_right_template", pillConfig.rightTemplate)
            putExtra("oi_right_content", pillConfig.rightContent)
            putExtra("status_chip_text", pillConfig.statusChip)
            if (pillConfig.showProgress) {
                putExtra("show_progress", true)
                putExtra("progress", progress)
                putExtra("progress_max", progressMax)
            }

            // Dynamic High-Contrast Capsule Badge Colors
            putExtra("oi_bg_color", downloadColor)
            putExtra("oi_fg_color", "#FFFFFFFF")

            // Ambient Edge Glow around expanded card
            putExtra("oi_light_color", downloadColor)
            putExtra("oi_light_mode", 1)

            putExtra("title", displayTitle)
            putExtra("text", details)
            putExtra("subtext", if (displayTitle != appLabel) "$appLabel • Download Manager" else "$appLabel • Live Download")
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("oi_left_content", pillConfig.leftContent)
            if (pillConfig.leftDoubleLine != null) {
                putStringArrayListExtra("oi_left_doubleline", pillConfig.leftDoubleLine)
            }
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("source_content_intent", clickIntent)

            if (actions.isNotEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(actions))
                putStringArrayListExtra("oi_button_titles", btnTitles)
                putIntegerArrayListExtra("oi_button_bg_colors", btnBgColors)
                putIntegerArrayListExtra("oi_button_text_colors", btnTxtColors)
            }
        }
        context.startService(intent)
    }

    data class PillLayoutConfig(
        val leftContent: String,
        val leftDoubleLine: ArrayList<String>?,
        val rightTemplate: Int,
        val rightContent: String,
        val showProgress: Boolean,
        val progressVal: Int,
        val statusChip: String,
    )

    fun resolvePillConfig(
        style: String,
        displayTitle: String,
        percent: Int,
        indeterminate: Boolean,
        progressMax: Int,
        liveMbpsSpeed: String,
    ): PillLayoutConfig {
        val pctText = if (percent > 0) "$percent%" else "0%"
        val hasProgress = !indeterminate && progressMax > 0

        return when (style) {
            "doubleline" -> {
                // Option 2: vivo OriginOS Native Dual-Line
                val line1 = displayTitle.take(12)
                val line2 = liveMbpsSpeed
                val rightTpl = if (hasProgress) {
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS
                } else {
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING
                }
                val chip = if (hasProgress) pctText else "..."
                PillLayoutConfig(
                    leftContent = line1,
                    leftDoubleLine = arrayListOf(line1, line2),
                    rightTemplate = rightTpl,
                    rightContent = chip,
                    showProgress = hasProgress,
                    progressVal = percent,
                    statusChip = chip,
                )
            }
            "speed_capsule" -> {
                // Option 3: Speed & % Badge
                PillLayoutConfig(
                    leftContent = liveMbpsSpeed,
                    leftDoubleLine = null,
                    rightTemplate = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT,
                    rightContent = pctText,
                    showProgress = hasProgress,
                    progressVal = percent,
                    statusChip = pctText,
                )
            }
            "percent_speed", "split" -> {
                // Option 4: % & Speed Badge
                PillLayoutConfig(
                    leftContent = pctText,
                    leftDoubleLine = null,
                    rightTemplate = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT,
                    rightContent = liveMbpsSpeed,
                    showProgress = hasProgress,
                    progressVal = percent,
                    statusChip = liveMbpsSpeed,
                )
            }
            else -> { // "speed_ring" (Default & User's favorite)
                // Option 1: Speed & Ring
                val rightTpl = if (hasProgress) {
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS
                } else {
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING
                }
                val chip = if (hasProgress) pctText else "..."
                PillLayoutConfig(
                    leftContent = liveMbpsSpeed,
                    leftDoubleLine = null,
                    rightTemplate = rightTpl,
                    rightContent = chip,
                    showProgress = hasProgress,
                    progressVal = percent,
                    statusChip = chip,
                )
            }
        }
    }
}
