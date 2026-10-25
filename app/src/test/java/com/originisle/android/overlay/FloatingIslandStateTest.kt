package com.originisle.android.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FloatingIslandStateTest {

    @Test
    fun testOverlayCardDataDefaults() {
        val card = OverlayCardData(
            id = 101,
            title = "Google Chrome",
            text = "Downloading file • 19.2 Mbps",
            sourceApp = "Google Play Store",
            progress = 75,
            waveState = 0,
        )

        assertEquals(101, card.id)
        assertEquals("Google Chrome", card.title)
        assertEquals("Downloading file • 19.2 Mbps", card.text)
        assertEquals("Google Play Store", card.sourceApp)
        assertEquals(75, card.progress)
        assertEquals(0, card.waveState)
    }

    @Test
    fun testVisualModes() {
        assertEquals(OverlayVisualMode.HIDDEN, OverlayVisualMode.valueOf("HIDDEN"))
        assertEquals(OverlayVisualMode.COMPACT, OverlayVisualMode.valueOf("COMPACT"))
        assertEquals(OverlayVisualMode.EXPANDED, OverlayVisualMode.valueOf("EXPANDED"))
    }
}
