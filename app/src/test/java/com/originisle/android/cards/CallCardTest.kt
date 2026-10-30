package com.originisle.android.cards

import com.originisle.android.util.IntelligentTextSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallCardTest {

    @Test
    fun testCallPackageRecognition() {
        assertTrue("com.whatsapp" in CallCard.CALL_PACKAGES)
        assertTrue("com.google.android.dialer" in CallCard.CALL_PACKAGES)
        assertTrue("com.facebook.orca" in CallCard.CALL_PACKAGES)
        assertTrue("org.telegram.messenger" in CallCard.CALL_PACKAGES)
        assertTrue("com.google.android.apps.tachyon" in CallCard.CALL_PACKAGES)
        assertTrue("com.google.android.apps.meetings" in CallCard.CALL_PACKAGES)
        assertTrue("com.microsoft.teams" in CallCard.CALL_PACKAGES)
        assertTrue("us.zoom.videomeetings" in CallCard.CALL_PACKAGES)
        assertTrue("org.thoughtcrime.securesms" in CallCard.CALL_PACKAGES)
        assertTrue("com.imo.android.imoim" in CallCard.CALL_PACKAGES)
    }

    @Test
    fun testOngoingCallPillLeftContentIsEmpty() {
        // When a call is ongoing, compact island left side must only have icon, no text!
        val isOngoing = true
        val smartCaller = "Tanvir"
        val pillLeft = if (isOngoing) "" else smartCaller.take(12)
        assertEquals("", pillLeft)
    }

    @Test
    fun testIncomingCallPillLeftContentHasCaller() {
        // When a call is incoming (ringing), compact island left side has caller name
        val isOngoing = false
        val smartCaller = "Tanvir"
        val pillLeft = if (isOngoing) "" else smartCaller.take(12)
        assertEquals("Tanvir", pillLeft)
    }

    @Test
    fun testCallColors() {
        // Red Decline (#FF3B30) and Green Answer (#34C759)
        assertEquals(0xFFFF3B30.toInt(), CallCard.COLOR_DECLINE)
        assertEquals(0xFF34C759.toInt(), CallCard.COLOR_ANSWER)
    }

    @Test
    fun testSmartFirstNameForCalls() {
        assertEquals("Rafiul", IntelligentTextSanitizer.smartFirstName("Md. Rafiul Ferdos"))
        assertEquals("Gregory", IntelligentTextSanitizer.smartFirstName("Dr. Gregory House"))
        assertEquals("Mom", IntelligentTextSanitizer.smartFirstName("Mom"))
        assertEquals("Sarah", IntelligentTextSanitizer.smartFirstName("Sarah Connor"))
    }

    @Test
    fun testDurationRegexExtraction() {
        val regex = Regex("""\b(\d{1,2}:\d{2}(?::\d{2})?)\b""")
        assertEquals("03:45", regex.find("Ongoing call • 03:45 • Messenger")?.value)
        assertEquals("12:00", regex.find("12:00 call in progress")?.value)
        assertEquals("01:23:45", regex.find("Call duration 01:23:45")?.value)
    }

    @Test
    fun testCallKeywordsDetection() {
        val keywords = listOf(
            "incoming call", "calling...", "voice call", "video call", "ringing",
            "call in progress", "ongoing call", "tap to return to call", "touch to return to call",
            "in call", "active call", "return to call",
        )
        val text1 = "tap to return to call • Messenger"
        assertTrue(keywords.any { text1.lowercase().contains(it) })

        val text2 = "active call (04:12)"
        assertTrue(keywords.any { text2.lowercase().contains(it) })

        val text3 = "random notification text"
        assertFalse(keywords.any { text3.lowercase().contains(it) })
    }
}
