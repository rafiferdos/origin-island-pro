package com.originisle.android.util

import android.os.Build

/**
 * Whether this device can actually show Origin Island cards.
 *
 * SuperX / OriginIsland is a vivo vendor feature reached through a hidden `NotificationManager`
 * method. It does not exist on stock Android or other OEM skins, and it cannot work below
 * OriginOS 6. The app nevertheless declared `minSdk 34` and would install on anything, then
 * silently post SuperX extras that nothing rendered — the worst kind of failure, because the user
 * has no way to tell it apart from a permission they forgot to grant.
 *
 * This object makes that visible: [isVivoPlatform] is checked on entry and the UI says plainly
 * that the device is unsupported instead of pretending to work.
 */
object DeviceCompat {
    /** Brands that ship OriginOS / Funtouch OS with the SuperX island. */
    private val SUPPORTED_MANUFACTURERS = setOf("vivo", "iqoo", "bbk", "bbk electronics")

    /** The device's reported manufacturer, for the banner's wording. */
    val manufacturer: String = Build.MANUFACTURER?.trim().orEmpty().ifBlank { "this device" }

    val model: String = Build.MODEL?.trim().orEmpty()

    /** True when the device is made by vivo or its iQOO sub-brand. */
    val isVivoPlatform: Boolean
        get() = isVivoPlatform(Build.MANUFACTURER, Build.BRAND)

    /**
     * Pure brand check, split out so it can be unit-tested without a device.
     *
     * Comparison is case-insensitive and ignores surrounding whitespace; both the manufacturer and
     * the brand are accepted because some iQOO units report the manufacturer as "vivo" and others
     * as "iQOO", and a few report "BBK".
     */
    fun isVivoPlatform(
        manufacturer: String?,
        brand: String?,
    ): Boolean {
        val m = manufacturer?.trim()?.lowercase().orEmpty()
        val b = brand?.trim()?.lowercase().orEmpty()
        return m in SUPPORTED_MANUFACTURERS || b in SUPPORTED_MANUFACTURERS
    }

    /** The message shown on unsupported hardware. */
    fun unsupportedMessage(): String {
        val device = model.ifBlank { manufacturer }
        return "Origin Island needs a vivo or iQOO device running OriginOS 6 or newer. " +
            "The island will not appear on this $device."
    }
}
