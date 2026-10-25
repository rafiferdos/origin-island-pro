package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCardTest {

    @Test
    fun testCleanTrackTitle() {
        assertEquals("Midnight City", MediaCard.cleanTrackTitle("Midnight City [Official Audio]"))
        assertEquals("Starboy", MediaCard.cleanTrackTitle("Starboy (Official Music Video)"))
        assertEquals("Yellow", MediaCard.cleanTrackTitle("Yellow (Lyrics)"))
        assertEquals("Hotel California", MediaCard.cleanTrackTitle("Hotel California [Remastered 2013]"))
        assertEquals("Bohemian Rhapsody", MediaCard.cleanTrackTitle("Bohemian Rhapsody - Official Video"))
        assertEquals("Blinding Lights", MediaCard.cleanTrackTitle("Blinding Lights"))
    }
}
