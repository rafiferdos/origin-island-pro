package com.originisle.android.util

/**
 * Semantic Island Priority Matrix for Origin Island Pro.
 * Provides hierarchical priority ranking so critical live tasks (Calls, Navigation, Timers, Rides)
 * maintain their anchor on the dynamic island and are never displaced by transient notifications.
 */
object PriorityArbitrator {

    const val PRIORITY_CALL = 100
    const val PRIORITY_NAVIGATION = 95
    const val PRIORITY_TIMER = 90
    const val PRIORITY_RIDE = 80
    const val PRIORITY_DELIVERY = 75
    const val PRIORITY_AUTH_PROMPT = 72
    const val PRIORITY_OTP = 70
    const val PRIORITY_SPORTS = 60
    const val PRIORITY_MEDIA = 50
    const val PRIORITY_MESSAGE = 40
    const val PRIORITY_MEETING = 40
    const val PRIORITY_PAYMENT = 35
    const val PRIORITY_WEATHER = 30
    const val PRIORITY_GENERIC = 20

    fun priorityForKind(kind: String): Int = when (kind.lowercase()) {
        "call" -> PRIORITY_CALL
        "navigation" -> PRIORITY_NAVIGATION
        "timer" -> PRIORITY_TIMER
        "ride" -> PRIORITY_RIDE
        "delivery" -> PRIORITY_DELIVERY
        "auth" -> PRIORITY_AUTH_PROMPT
        "otp" -> PRIORITY_OTP
        "sports" -> PRIORITY_SPORTS
        "media" -> PRIORITY_MEDIA
        "message" -> PRIORITY_MESSAGE
        "meeting" -> PRIORITY_MEETING
        "payment" -> PRIORITY_PAYMENT
        "weather" -> PRIORITY_WEATHER
        else -> PRIORITY_GENERIC
    }
}
