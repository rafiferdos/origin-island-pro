package com.originisle.android.island

import org.junit.Assert.assertEquals
import org.junit.Test

class OriginIslandLandingTest {

    @Test
    fun testLandingProtocolConstants() {
        assertEquals("island.superx.landingPkg", OriginIslandConstants.BUNDLE_KEY_ISLAND_LANDING_PKG)
        assertEquals("island.superx.landingInfo", OriginIslandConstants.BUNDLE_KEY_ISLAND_LANDING_INFO)
        assertEquals("notification.superx.capsule.landingAutoHide", OriginIslandConstants.BUNDLE_KEY_CAPSULE_LANDING_AUTO_HIDE)
        assertEquals(1, OriginIslandConstants.ISLAND_CLICK_LANDING)
    }
}
