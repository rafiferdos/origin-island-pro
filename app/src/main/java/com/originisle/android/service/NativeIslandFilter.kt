package com.originisle.android.service

import com.originisle.android.island.OriginIslandConstants

/**
 * Detects notifications that another app already shows as a native vivo island.
 *
 * On the China ROM, vivo's partner apps (food delivery, ride-hailing, payments, train tickets) and
 * vivo's own system apps post their own SuperX islands. Re-casting those produces two pills for
 * one event, so the listener skips them. A native island is recognisable from outside: it is
 * posted under [OriginIslandConstants.SUPERX_TAG] and/or carries `notification.superx.*` /
 * `island.superx.*` extras.
 *
 * Pure, so it is JVM-testable without a StatusBarNotification.
 */
object NativeIslandFilter {
    const val PREF_SKIP_NATIVE = "skip_native_islands"

    private val SUPERX_PREFIXES = listOf("notification.superx.", "island.superx.")

    fun isNativeIsland(
        tag: String?,
        extrasKeys: Set<String>,
        pkg: String,
        ownPkg: String,
    ): Boolean {
        if (pkg == ownPkg) return false
        if (tag == OriginIslandConstants.SUPERX_TAG) return true
        return extrasKeys.any { key -> SUPERX_PREFIXES.any { key.startsWith(it) } }
    }
}
