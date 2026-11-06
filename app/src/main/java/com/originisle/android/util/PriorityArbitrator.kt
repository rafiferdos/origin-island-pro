package com.originisle.android.util

import android.content.Context

/**
 * Semantic Island Priority Matrix for Origin Island Pro.
 * Provides hierarchical priority ranking so critical live tasks (Calls, Navigation, Timers, Raiden)
 * maintain their anchor on the dynamic island and are never displaced by transient notifications.
 *
 * Every card posts with one of these constants; the arbitration between a live card and an incoming
 * higher-priority one lives in `PlaygroundService`. [getRaidenPriority] is the one policy that is
 * not a plain constant — it depends on a user preference — and it is split into a pure function
 * plus a Context wrapper so the policy can be tested without Android.
 */
object PriorityArbitrator {

    /** The Raiden/Muse preference store. Single source for the `raiden_always_on_top` read. */
    const val PREFS_NAME = "experimental_prefs"
    const val PREF_RAIDEN_ALWAYS_ON_TOP = "raiden_always_on_top"

    const val PRIORITY_CALL = 100
    const val PRIORITY_NAVIGATION = 95
    const val PRIORITY_TIMER = 90
    const val PRIORITY_RAIDEN_LIVE = 85
    const val PRIORITY_RIDE = 80
    const val PRIORITY_DELIVERY = 75
    const val PRIORITY_AUTH_PROMPT = 72
    const val PRIORITY_OTP = 70

    /**
     * An ongoing card that is neither a call nor showing progress (a generic live task). Sits above
     * the message/payment tiers and below the explicit live-card tiers. This was the magic `65` in
     * `GenericCard` before it had a name.
     */
    const val PRIORITY_ONGOING = 65

    const val PRIORITY_SPORTS = 60
    const val PRIORITY_MEDIA = 50
    const val PRIORITY_RAIDEN_MESSAGE = 40
    const val PRIORITY_MESSAGE = 40
    const val PRIORITY_MEETING = 40
    const val PRIORITY_PAYMENT = 35
    const val PRIORITY_WEATHER = 30
    const val PRIORITY_GENERIC = 20

    /** The single read of the "raiden_always_on_top" preference. */
    fun isRaidenAlwaysOnTop(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, 0)
            .getBoolean(PREF_RAIDEN_ALWAYS_ON_TOP, false)

    /**
     * Raiden's priority: 100 when the user forced it above everything, else 85 live / 40 message.
     *
     * Pure, so the policy can be asserted without a device or Robolectric.
     */
    fun getRaidenPriority(isLive: Boolean, alwaysOnTop: Boolean): Int {
        if (alwaysOnTop) return PRIORITY_CALL
        return if (isLive) PRIORITY_RAIDEN_LIVE else PRIORITY_RAIDEN_MESSAGE
    }

    /** @see getRaidenPriority */
    fun getRaidenPriority(context: Context, isLive: Boolean): Int =
        getRaidenPriority(isLive, isRaidenAlwaysOnTop(context))
}
