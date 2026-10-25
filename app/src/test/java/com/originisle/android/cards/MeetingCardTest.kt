package com.originisle.android.cards

import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingCardTest {

    @Test
    fun testMeetingApps() {
        assertTrue(MeetingCard.MEETING_APPS.contains("com.google.android.calendar"))
        assertTrue(MeetingCard.MEETING_APPS.contains("com.google.android.apps.meetings"))
        assertTrue(MeetingCard.MEETING_APPS.contains("us.zoom.videomeetings"))
        assertTrue(MeetingCard.MEETING_APPS.contains("com.microsoft.teams"))
        assertTrue(MeetingCard.MEETING_APPS.contains("com.microsoft.office.outlook"))
    }

    @Test
    fun testMeetingKeywords() {
        val keywords = MeetingCard.MEETING_KEYWORDS
        assertTrue(keywords.contains("meeting"))
        assertTrue(keywords.contains("starts in"))
        assertTrue(keywords.contains("zoom meeting"))
        assertTrue(keywords.contains("google meet"))
    }
}
