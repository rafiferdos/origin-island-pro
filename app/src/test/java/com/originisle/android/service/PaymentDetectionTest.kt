package com.originisle.android.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentDetectionTest {

    private val amountRegex = Regex(
        """(?:[৳€$£¥₹₽₺₩₪₫₴]|\b(?:Tk\.?|BDT|EUR|USD|GBP|CHF|SEK|NOK|DKK|PLN|CZK|INR|TRY|RUB|KRW|THB|PHP)\b)\s?\d[\d.,]*""" +
            """|\d[\d.,]*\s?(?:[৳€$£¥₹₽₺₩₪₫₴]|\b(?:Tk\.?|BDT|EUR|USD|GBP|CHF|SEK|NOK|DKK|PLN|CZK|INR|TRY|RUB|KRW|THB|PHP)\b)""",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun testBangladeshCurrencyMatching() {
        val bdt1 = amountRegex.find("You have received ৳1,500.00 from Tanvir")?.value?.trim()
        assertEquals("৳1,500.00", bdt1)

        val bdt2 = amountRegex.find("Cash In Tk 500 from Agent")?.value?.trim()
        assertEquals("Tk 500", bdt2)

        val bdt3 = amountRegex.find("Payment to Daraz: 2500 Tk. TrxID: 9X3KL")?.value?.trim()
        assertEquals("2500 Tk", bdt3)

        val bdt4 = amountRegex.find("Mobile recharge BDT 100 successful")?.value?.trim()
        assertEquals("BDT 100", bdt4)
    }

    @Test
    fun testBangladeshPaymentApps() {
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("com.bKash.customerapp"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("bd.gov.nagad"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("com.konasl.dbbl.mb"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("bd.com.upay.customer"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("com.ibbl.cellfin"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("co.bracbank.astha"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("com.thecitybank.citytouch"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("ebl.skybanking"))
        assertTrue(NotificationCastListener.PAYMENT_APPS.contains("com.konasl.nexuspay"))
    }

    @Test
    fun testMessagingApps() {
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.whatsapp"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.whatsapp.w4b"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.facebook.orca"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.facebook.mlite"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("org.telegram.messenger"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.imo.android.imoim"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.imo.android.imoimhd"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.viber.voip"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.discord"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.instagram.android"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.google.android.apps.messaging"))
        assertTrue(NotificationCastListener.MESSAGING_APPS.contains("com.truecaller"))
    }
}
