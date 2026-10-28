package com.originisle.android.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginIslandGeminiViewTest {

    @Test
    fun testGeminiConstants() {
        assertEquals("notification.superx.capsule.content", OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT)
        assertEquals("notification.superx.capsule.contentColor", OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT_COLOR)
        assertEquals("notification.superx.capsule.bgColor", OriginIslandConstants.BUNDLE_KEY_CAPSULE_BG_COLOR)
        assertEquals("island.superx.rightInfo.capsuleContent", OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT)
        assertEquals("island.superx.rightInfo.capsuleBgColor", OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR)
    }

    @Test
    fun testGeminiPaletteValues() {
        val palette = OriginIslandBuilder.GEMINI_PALETTE
        assertNotNull(palette)
        assertTrue(palette.isNotEmpty())
        assertEquals(4, palette.size)
        // Verify key colors: bright cyan, indigo, purple, rose
        assertEquals(0xFF38BDF8.toInt(), palette[0])
        assertEquals(0xFF818CF8.toInt(), palette[1])
        assertEquals(0xFFC084FC.toInt(), palette[2])
        assertEquals(0xFFF472B6.toInt(), palette[3])
    }
}
