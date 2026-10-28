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

    @Test
    fun testSmartFirstName() {
        assertEquals("Rafiul", IntelligentTextSanitizer.smartFirstName("Md. Rafiul Ferdos"))
        assertEquals("Hasan", IntelligentTextSanitizer.smartFirstName("MD Hasan Mahmud"))
        assertEquals("Sabbir", IntelligentTextSanitizer.smartFirstName("md sabbir"))
        assertEquals("Tanvir", IntelligentTextSanitizer.smartFirstName("md.tanvir"))
        assertEquals("Gregory", IntelligentTextSanitizer.smartFirstName("Dr. Gregory House"))
        assertEquals("Albus", IntelligentTextSanitizer.smartFirstName("Prof. Albus Dumbledore"))
        assertEquals("Hasib", IntelligentTextSanitizer.smartFirstName("Engr. Sheikh Hasib"))
        assertEquals("Nazrul", IntelligentTextSanitizer.smartFirstName("Syed Nazrul Islam"))
        assertEquals("Fatema", IntelligentTextSanitizer.smartFirstName("Mst. Fatema Akter"))
        assertEquals("John", IntelligentTextSanitizer.smartFirstName("John Doe"))
        assertEquals("Sarah", IntelligentTextSanitizer.smartFirstName("Sarah Connor"))
        assertEquals("Sarah", IntelligentTextSanitizer.smartFirstName("✨ Sarah ✨"))
        assertEquals("Rafiul", IntelligentTextSanitizer.smartFirstName("Rafiul (3)"))
        assertEquals("Md", IntelligentTextSanitizer.smartFirstName("Md"))
        assertEquals("Message", IntelligentTextSanitizer.smartFirstName(""))
    }

    @Test
    fun testFormatNavigationPill() {
        assertEquals("300m Left", IntelligentTextSanitizer.formatNavigationPill("In 300 m, use the left 2 lanes to turn left onto Gulshan Ave"))
        assertEquals("1.5km Exit", IntelligentTextSanitizer.formatNavigationPill("In 1.5 km, take exit 4 toward Airport"))
        assertEquals("Turn Right", IntelligentTextSanitizer.formatNavigationPill("Turn right on Airport Rd"))
        assertEquals("U-Turn", IntelligentTextSanitizer.formatNavigationPill("Make a U-turn"))
        assertEquals("4km Straight", IntelligentTextSanitizer.formatNavigationPill("Continue straight for 4 km"))
        assertEquals("350m", IntelligentTextSanitizer.formatNavigationPill("350 m"))
        assertEquals("Arrived 🏁", IntelligentTextSanitizer.formatNavigationPill("You have arrived at your destination"))
    }

    @Test
    fun testExtractNavigationAssist() {
        assertEquals("Use the left 2 lanes", IntelligentTextSanitizer.extractNavigationAssist("In 300 m, use the left 2 lanes to turn left onto Gulshan Ave"))
        assertEquals("Toward Airport", IntelligentTextSanitizer.extractNavigationAssist("In 1.5 km, take exit 4 toward Airport"))
    }
}
