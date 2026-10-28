package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class CardIntentsTest {

    @Test
    fun testFormatElapsed() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:45", formatElapsed(45))
        assertEquals("02:05", formatElapsed(125))
        assertEquals("01:02:05", formatElapsed(3725))
    }
}
