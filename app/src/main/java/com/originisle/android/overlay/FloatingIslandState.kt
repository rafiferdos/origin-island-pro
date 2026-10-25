package com.originisle.android.overlay

import android.app.PendingIntent
import android.graphics.Bitmap

/**
 * Visual presentation mode of the floating overlay island.
 */
enum class OverlayVisualMode {
    HIDDEN,
    COMPACT,
    EXPANDED
}

/**
 * Data model representing the active floating island card.
 */
data class OverlayCardData(
    val id: Int = 0,
    val title: String = "",
    val text: String = "",
    val subtext: String = "",
    val sourceApp: String = "",
    val iconBitmap: Bitmap? = null,
    val iconRes: Int = 0,
    val leftContent: String = "",
    val rightContent: String = "",
    val statusChip: String = "",
    val progress: Int = -1, // 0..100, or -1 if no progress
    val isOngoing: Boolean = false,
    val scene: String = "NAVIGATION",
    val template: Int = 0,
    val waveState: Int = 0,
    val waveColors: List<String> = emptyList(),
    val buttonTitles: List<String> = emptyList(),
    val buttonColors: List<Int> = emptyList(),
    val buttonTextColors: List<Int> = emptyList(),
    val buttonIntents: List<PendingIntent> = emptyList(),
    val clickIntent: PendingIntent? = null,
    val lightColor: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
)
