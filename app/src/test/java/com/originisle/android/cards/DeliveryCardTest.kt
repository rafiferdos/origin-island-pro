package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryCardTest {

    @Test
    fun testBangladeshDeliveryPackages() {
        assertTrue(DeliveryCard.DELIVERY_APPS.contains("com.pathao.user"))
        assertTrue(DeliveryCard.DELIVERY_APPS.contains("com.global.foodpanda.android"))
        assertTrue(DeliveryCard.DELIVERY_APPS.contains("com.daraz.android"))
        assertTrue(DeliveryCard.DELIVERY_APPS.contains("com.chaldal.poached"))
        assertTrue(DeliveryCard.DELIVERY_APPS.contains("com.shohoz.rides"))
    }

    @Test
    fun testEtaExtractionMinutes() {
        assertEquals("15 min", DeliveryCard.extractEta("Your Foodpanda rider is arriving in 15 mins"))
        assertEquals("20 min", DeliveryCard.extractEta("Pathao delivery: ETA 20m"))
        assertEquals("10 min", DeliveryCard.extractEta("Rider arriving in 10 min"))
    }

    @Test
    fun testEtaExtractionStatus() {
        assertEquals("Preparing", DeliveryCard.extractEta("Your food is being prepared at the restaurant"))
        assertEquals("On the way", DeliveryCard.extractEta("Rider is on the way to your location"))
        assertEquals("Delivered ✓", DeliveryCard.extractEta("Your order has been delivered! Enjoy your meal"))
    }

    @Test
    fun testEtaExtractionTime() {
        assertEquals("by 4:30 PM", DeliveryCard.extractEta("Your package will be delivered by 4:30 PM"))
    }
}
