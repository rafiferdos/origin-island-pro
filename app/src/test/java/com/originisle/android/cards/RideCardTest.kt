package com.originisle.android.cards

import org.junit.Assert.assertTrue
import org.junit.Test

class RideCardTest {

    @Test
    fun testRideApps() {
        assertTrue(RideCard.RIDE_APPS.contains("com.ubercab"))
        assertTrue(RideCard.RIDE_APPS.contains("com.pathao.user"))
        assertTrue(RideCard.RIDE_APPS.contains("com.shohoz.rides"))
        assertTrue(RideCard.RIDE_APPS.contains("com.grabtaxi.passenger"))
        assertTrue(RideCard.RIDE_APPS.contains("com.bolt.android"))
    }

    @Test
    fun testRideKeywords() {
        val keywords = RideCard.RIDE_KEYWORDS
        assertTrue(keywords.contains("driver arriving"))
        assertTrue(keywords.contains("driver is on the way"))
        assertTrue(keywords.contains("ride confirmed"))
        assertTrue(keywords.contains("captain is on the way"))
    }
}
