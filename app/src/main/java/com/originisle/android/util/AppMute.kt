package com.originisle.android.util

/**
 * Temporary per-app mute (plan task T5.3): "don't cast this app for the next N hours", like
 * OriginOS 7's own temporary notification mute. Stored as an epoch-millis deadline per package.
 */
object AppMute {
    private const val HOUR_MS = 3_600_000L

    fun key(pkg: String): String = "mute_until_$pkg"

    fun isMuted(
        muteUntil: Long,
        now: Long,
    ): Boolean = now < muteUntil

    fun until(
        now: Long,
        hours: Int,
    ): Long = now + hours * HOUR_MS
}
