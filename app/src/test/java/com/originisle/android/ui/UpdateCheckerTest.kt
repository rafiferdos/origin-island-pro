package com.originisle.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        // Newer minor/patch
        assertTrue(UpdateChecker.isNewer("2.31.0", "2.30.0"))
        assertTrue(UpdateChecker.isNewer("2.30.1", "2.30.0"))
        assertTrue(UpdateChecker.isNewer("2.30.10", "2.30.9"))
        assertTrue(UpdateChecker.isNewer("3.0.0", "2.31.0"))

        // Equal versions
        assertFalse(UpdateChecker.isNewer("2.31.0", "2.31.0"))
        assertFalse(UpdateChecker.isNewer("2.30.0", "2.30.0"))

        // Older versions
        assertFalse(UpdateChecker.isNewer("2.29.0", "2.30.0"))
        assertFalse(UpdateChecker.isNewer("2.9.0", "2.10.0"))
        assertFalse(UpdateChecker.isNewer("1.99.0", "2.0.0"))
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 MB", UpdateChecker.formatBytes(0L))
        assertEquals("0 MB", UpdateChecker.formatBytes(-50L))
        assertEquals("1.0 MB", UpdateChecker.formatBytes(1024L * 1024L))
        assertEquals("24.3 MB", UpdateChecker.formatBytes((24.3 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatSpeed() {
        assertEquals("0 KB/s", UpdateChecker.formatSpeed(0L))
        assertEquals("500 KB/s", UpdateChecker.formatSpeed(500L * 1024L))
        assertEquals("1.0 MB/s", UpdateChecker.formatSpeed(1024L * 1024L))
        assertEquals("3.5 MB/s", UpdateChecker.formatSpeed((3.5 * 1024 * 1024).toLong()))
    }
}
