package com.originisle.android.util

import com.originisle.android.island.OriginIslandBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticButtonTest {

    @Test
    fun testSemanticButtonBgColors() {
        val red = 0xFFFF3B30.toInt()
        val green = 0xFF34C759.toInt()
        val amber = 0xFFFF9500.toInt()

        assertEquals(red, OriginIslandBuilder.resolveSemanticButtonBgColor("Decline"))
        assertEquals(red, OriginIslandBuilder.resolveSemanticButtonBgColor("Reject"))
        assertEquals(red, OriginIslandBuilder.resolveSemanticButtonBgColor("Hang up"))
        assertEquals(red, OriginIslandBuilder.resolveSemanticButtonBgColor("Cancel"))
        assertEquals(red, OriginIslandBuilder.resolveSemanticButtonBgColor("Deny"))

        assertEquals(green, OriginIslandBuilder.resolveSemanticButtonBgColor("Answer"))
        assertEquals(green, OriginIslandBuilder.resolveSemanticButtonBgColor("Accept"))
        assertEquals(green, OriginIslandBuilder.resolveSemanticButtonBgColor("Approve"))
        assertEquals(green, OriginIslandBuilder.resolveSemanticButtonBgColor("Join"))

        assertEquals(amber, OriginIslandBuilder.resolveSemanticButtonBgColor("Pause"))
        assertEquals(amber, OriginIslandBuilder.resolveSemanticButtonBgColor("Snooze"))
    }

    @Test
    fun testSemanticButtonTextColors() {
        val white = 0xFFFFFFFF.toInt()
        assertEquals(white, OriginIslandBuilder.resolveSemanticButtonTextColor("Decline"))
        assertEquals(white, OriginIslandBuilder.resolveSemanticButtonTextColor("Answer"))
        assertEquals(white, OriginIslandBuilder.resolveSemanticButtonTextColor("Approve"))
    }
}
