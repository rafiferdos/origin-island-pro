package com.originisle.android.island

import android.content.Context
import com.originisle.android.ui.PREFS_NAME

/**
 * Universal Hardware Camera Cutout & Safe Margin Engine.
 *
 * Computes glanceable text limits and padding dynamically from user preferences:
 * - camera_cutout_position: "center" (default for vivo X / iQOO) vs "left" (left-corner punch hole)
 * - camera_cutout_gap: gap width in dp (22dp to 50dp, default 34dp)
 * - camera_cutout_margin: safety clearance margin in dp (0dp to 16dp, default 8dp)
 */
object CutoutConfig {

    const val PREF_POSITION = "camera_cutout_position"
    const val PREF_GAP = "camera_cutout_gap"
    const val PREF_MARGIN = "camera_cutout_margin"

    const val DEFAULT_GAP = 34f
    const val DEFAULT_MARGIN = 8f
    const val DEFAULT_POSITION = "center"

    /**
     * Resolves maximum character count for the collapsed pill's left content so text
     * never collides with the front punch-hole camera on Vivo / OriginOS devices.
     */
    fun getPillLeftLimit(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        val pos = prefs.getString(PREF_POSITION, DEFAULT_POSITION) ?: DEFAULT_POSITION
        val margin = prefs.getFloat(PREF_MARGIN, DEFAULT_MARGIN)
        val gap = prefs.getFloat(PREF_GAP, DEFAULT_GAP)

        // Center punch-holes have ~11-12 chars per half by default.
        // Left-corner punch-holes have less clearance on the left (~8-9 chars).
        val baseLimit = if (pos == "left") 9 else 12

        // Safe margin penalty: larger margin (> 8dp) shrinks available text room;
        // smaller margin (< 8dp) grants more characters.
        val marginPenalty = ((margin - 8f) / 4f).toInt()

        // Cutout gap penalty: larger camera gap (> 34dp) reduces room
        val gapPenalty = ((gap - 34f) / 8f).toInt()

        return (baseLimit - marginPenalty - gapPenalty).coerceIn(6, 16)
    }

    /**
     * Applies camera-aware formatting and spacing to the pill's left content.
     * On left-corner punch-hole devices, prepends safe space to keep icons and text
     * clear of the physical camera cutout boundary.
     */
    fun applyCutoutPadding(context: Context, text: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        val pos = prefs.getString(PREF_POSITION, DEFAULT_POSITION) ?: DEFAULT_POSITION
        val limit = getPillLeftLimit(context)
        val trimmed = if (text.length > limit) text.take(limit) else text
        return if (pos == "left" && trimmed.isNotEmpty() && !trimmed.startsWith(" ")) {
            " $trimmed"
        } else {
            trimmed
        }
    }
}
