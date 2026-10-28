package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCardTest {

    @Test
    fun testClockAppsList() {
        assertTrue(TimerCard.CLOCK_APPS.contains("com.google.android.deskclock"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.vivo.clock"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.bbk.clock"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.sec.android.app.clockpackage"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.coloros.alarmclock"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.xiaomi.clock"))
        assertTrue(TimerCard.CLOCK_APPS.contains("com.huawei.deskclock"))
    }

    @Test
    fun testTimerColors() {
        assertEquals("#FFFF9500", TimerCard.TIMER_COLOR_HEX)
        assertEquals("#FFFFFFFF", TimerCard.TIMER_TEXT_COLOR_HEX)
    }
}
