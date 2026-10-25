package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallCardTest {

    @Test
    fun testCallPackageRecognition() {
        assertTrue("com.whatsapp" in CallCard.CALL_PACKAGES)
        assertTrue("com.google.android.dialer" in CallCard.CALL_PACKAGES)
        assertTrue("com.facebook.orca" in CallCard.CALL_PACKAGES)
        assertTrue("org.telegram.messenger" in CallCard.CALL_PACKAGES)
    }

    @Test
    fun testCallColors() {
        // Red Decline (#FF3B30) and Green Answer (#34C759)
        assertEquals(0xFFFF3B30.toInt(), CallCard.COLOR_DECLINE)
        assertEquals(0xFF34C759.toInt(), CallCard.COLOR_ANSWER)
    }
}
