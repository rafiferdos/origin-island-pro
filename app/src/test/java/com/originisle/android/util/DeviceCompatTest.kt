package com.originisle.android.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the platform check that decides whether the unsupported-device banner is shown. */
class DeviceCompatTest {
    @Test
    fun `vivo and iqoo are supported`() {
        assertTrue(DeviceCompat.isVivoPlatform("vivo", "vivo"))
        assertTrue(DeviceCompat.isVivoPlatform("iQOO", "iQOO"))
        assertTrue(DeviceCompat.isVivoPlatform("BBK", "vivo"))
    }

    @Test
    fun `brand alone is enough when the manufacturer differs`() {
        assertTrue(DeviceCompat.isVivoPlatform("vivo", "iQOO"))
    }

    @Test
    fun `other manufacturers are not supported`() {
        assertFalse(DeviceCompat.isVivoPlatform("Samsung", "samsung"))
        assertFalse(DeviceCompat.isVivoPlatform("Xiaomi", "redmi"))
        assertFalse(DeviceCompat.isVivoPlatform("Google", "pixel"))
    }

    @Test
    fun `comparison is case-insensitive and tolerant of whitespace`() {
        assertTrue(DeviceCompat.isVivoPlatform(" VIVO ", " Vivo "))
    }

    @Test
    fun `null and blank values are not supported`() {
        assertFalse(DeviceCompat.isVivoPlatform(null, null))
        assertFalse(DeviceCompat.isVivoPlatform("", ""))
        assertFalse(DeviceCompat.isVivoPlatform("   ", "   "))
    }

    @Test
    fun `the unsupported message names the requirement`() {
        val message = DeviceCompat.unsupportedMessage()
        assertTrue(message.contains("vivo"))
        assertTrue(message.contains("OriginOS 7"))
    }
}
