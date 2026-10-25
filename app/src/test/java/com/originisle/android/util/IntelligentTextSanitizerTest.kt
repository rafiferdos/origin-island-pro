package com.originisle.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class IntelligentTextSanitizerTest {

    @Test
    fun testCleanTitle() {
        assertEquals("Rafi", IntelligentTextSanitizer.cleanTitle("[Slack] Rafi", "Slack"))
        assertEquals("Tanvir", IntelligentTextSanitizer.cleanTitle("WhatsApp: Tanvir (3)", "WhatsApp"))
        assertEquals("Sprint Planning", IntelligentTextSanitizer.cleanTitle("Google Calendar: Sprint Planning", "Google Calendar"))
        assertEquals("Alert", IntelligentTextSanitizer.cleanTitle("You have a new notification: Alert", "System"))
    }

    @Test
    fun testCleanBody() {
        assertEquals("Let's meet at 5 PM", IntelligentTextSanitizer.cleanBody("Tanvir: Let's meet at 5 PM • Tap to open", "Tanvir", "WhatsApp"))
        assertEquals("Server is healthy", IntelligentTextSanitizer.cleanBody("Server is healthy (tap to view details)", "Alert", "Datadog"))
    }

    @Test
    fun testFormatRightCapsule() {
        assertEquals("In 10 min", IntelligentTextSanitizer.formatRightCapsule("in 10 minutes", 14))
        assertEquals("Due Tmrw", IntelligentTextSanitizer.formatRightCapsule("due tomorrow", 14))
        assertEquals("Done ✓", IntelligentTextSanitizer.formatRightCapsule("delivered", 14))
    }

    @Test
    fun testExtractTimeSnippet() {
        assertEquals("In 10m", IntelligentTextSanitizer.extractTimeSnippet("Team sync starts in 10 minutes"))
        assertEquals("Now", IntelligentTextSanitizer.extractTimeSnippet("Design review starts now"))
        assertEquals("3:30 PM", IntelligentTextSanitizer.extractTimeSnippet("Client call at 3:30 PM today"))
    }

    @Test
    fun testExtractVehiclePlate() {
        val plate = IntelligentTextSanitizer.extractVehiclePlate("Your driver is arriving in Dhaka Metro Ga-12-3456")
        assertNotNull(plate)
        assertEquals("Dhaka Metro Ga-12-3456", plate)
    }
}
