package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpCardTest {

    @Test
    fun testOtpSpacedCode() {
        assertEquals("482 913", OtpCard.spaced("482913"))
        assertEquals("12345", OtpCard.spaced("12345"))
        assertEquals("1234", OtpCard.spaced("1234"))
        assertEquals("987 654", OtpCard.spaced("987654"))
        assertEquals("1234 5678", OtpCard.spaced("12345678"))
    }
}
