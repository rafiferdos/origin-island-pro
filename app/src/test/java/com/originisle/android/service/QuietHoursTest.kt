package com.originisle.android.service

import com.originisle.android.service.QuietHours.CastKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    @Test fun overnightInside() {
        assertTrue(QuietHours.inWindow(m(23), m(22), m(7)))
        assertTrue(QuietHours.inWindow(m(3), m(22), m(7)))
        assertTrue(QuietHours.inWindow(m(22), m(22), m(7)))
    }

    @Test fun overnightOutside() {
        assertFalse(QuietHours.inWindow(m(7), m(22), m(7)))
        assertFalse(QuietHours.inWindow(m(12), m(22), m(7)))
        assertFalse(QuietHours.inWindow(m(21, 59), m(22), m(7)))
    }

    @Test fun sameDay() {
        assertTrue(QuietHours.inWindow(m(13), m(12), m(14)))
        assertFalse(QuietHours.inWindow(m(14), m(12), m(14)))
        assertFalse(QuietHours.inWindow(m(11), m(12), m(14)))
    }

    @Test fun equalIsOff() = assertFalse(QuietHours.inWindow(m(10), m(10), m(10)))

    @Test
    fun `quiet hours let only waited-on kinds through`() {
        CastKind.values().forEach { assertTrue(QuietHours.allows(it, active = false)) }
        listOf(CastKind.CALL, CastKind.NAVIGATION, CastKind.TIMER, CastKind.OTP, CastKind.MEDIA)
            .forEach { assertTrue(QuietHours.allows(it, active = true)) }
        listOf(CastKind.AUTH, CastKind.RAIDEN, CastKind.OTHER)
            .forEach { assertFalse(QuietHours.allows(it, active = true)) }
    }
}
