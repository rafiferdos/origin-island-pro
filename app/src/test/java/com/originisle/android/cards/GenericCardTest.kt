package com.originisle.android.cards

import org.junit.Assert.assertTrue
import org.junit.Test

class GenericCardTest {

    @Test
    fun testMeetingPackagesRecognized() {
        assertTrue("com.google.android.apps.meetings" in GenericCard.MEETING_PACKAGES)
        assertTrue("com.google.android.apps.tachyon" in GenericCard.MEETING_PACKAGES)
        assertTrue("us.zoom.videomeetings" in GenericCard.MEETING_PACKAGES)
        assertTrue("com.microsoft.teams" in GenericCard.MEETING_PACKAGES)
    }

    @Test
    fun testCallKeywordsRecognized() {
        assertTrue("your video" in GenericCard.CALL_KEYWORDS)
        assertTrue("video call" in GenericCard.CALL_KEYWORDS)
        assertTrue("in a meeting" in GenericCard.CALL_KEYWORDS)
        assertTrue("meeting in progress" in GenericCard.CALL_KEYWORDS)
        assertTrue("ongoing call" in GenericCard.CALL_KEYWORDS)
    }
}
