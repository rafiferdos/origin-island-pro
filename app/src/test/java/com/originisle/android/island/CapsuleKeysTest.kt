package com.originisle.android.island

import org.junit.Assert.assertEquals
import org.junit.Test

class CapsuleKeysTest {
    @Test
    fun capsuleKeysMatchTheProtocol() {
        assertEquals("notification.superx.capsule.content", OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT)
        assertEquals("notification.superx.capsule.contentColor", OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT_COLOR)
        assertEquals("notification.superx.capsule.bgColor", OriginIslandConstants.BUNDLE_KEY_CAPSULE_BG_COLOR)
        assertEquals("island.superx.rightInfo.capsuleContent", OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT)
        assertEquals("island.superx.rightInfo.capsuleBgColor", OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR)
    }
}
