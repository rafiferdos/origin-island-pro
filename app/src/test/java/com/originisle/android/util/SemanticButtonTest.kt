package com.originisle.android.util

import com.originisle.android.island.OriginIslandBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticButtonTest {
    private val red = OriginIslandBuilder.NATIVE_RED
    private val green = OriginIslandBuilder.NATIVE_GREEN
    private val neutral = OriginIslandBuilder.NATIVE_NEUTRAL

    private fun bg(title: String) = OriginIslandBuilder.resolveSemanticButtonBgColor(title)

    @Test
    fun `only call-style decline and accept are coloured`() {
        listOf("Decline", "Reject", "Hang up", "End call", "Deny", "Delete", "挂断", "拒接").forEach { assertEquals(it, red, bg(it)) }
        listOf("Answer", "Accept", "Approve", "接听", "同意").forEach { assertEquals(it, green, bg(it)) }
    }

    @Test
    fun `every other action gets the native neutral pill`() {
        listOf("Pause", "Snooze", "Join", "Resume", "Cancel", "Reply", "Mark as read").forEach { assertEquals(it, neutral, bg(it)) }
    }

    @Test
    fun `send and extend are not mistaken for end`() {
        assertEquals(neutral, bg("Send"))
        assertEquals(neutral, bg("Extend"))
    }

    @Test
    fun `brand colours passed by a card are ignored`() {
        assertEquals(neutral, OriginIslandBuilder.resolveSemanticButtonBgColor("Pause", 0xFF1C2B4E.toInt()))
    }

    @Test
    fun `button text is white`() {
        val white = 0xFFFFFFFF.toInt()
        assertEquals(white, OriginIslandBuilder.resolveSemanticButtonTextColor("Decline"))
        assertEquals(white, OriginIslandBuilder.resolveSemanticButtonTextColor("Answer", 0xFF000000.toInt()))
    }
}
