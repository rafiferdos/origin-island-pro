package com.originisle.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMuteTest {
    @Test
    fun `muted until the deadline, then not`() {
        val until = AppMute.until(now = 1_000L, hours = 1)
        assertEquals(3_601_000L, until)
        assertTrue(AppMute.isMuted(until, now = 2_000L))
        assertFalse(AppMute.isMuted(until, now = until))
        assertFalse(AppMute.isMuted(0L, now = 2_000L))
    }

    @Test
    fun `key is per package`() {
        assertEquals("mute_until_com.tencent.mm", AppMute.key("com.tencent.mm"))
    }
}
