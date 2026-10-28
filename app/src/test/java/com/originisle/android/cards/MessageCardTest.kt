package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageCardTest {

    @Test
    fun testResolveAppColor() {
        assertEquals("#FF2AABEE", MessageCard.resolveAppColor("org.telegram.messenger"))
        assertEquals("#FF2AABEE", MessageCard.resolveAppColor("org.thunderdog.challegram"))
        assertEquals("#FF25D366", MessageCard.resolveAppColor("com.whatsapp"))
        assertEquals("#FF25D366", MessageCard.resolveAppColor("com.whatsapp.w4b"))
        assertEquals("#FF0084FF", MessageCard.resolveAppColor("com.facebook.orca"))
        assertEquals("#FFE1306C", MessageCard.resolveAppColor("com.instagram.android"))
        assertEquals("#FF07C160", MessageCard.resolveAppColor("com.tencent.mm"))
        assertEquals("#FF12B7F5", MessageCard.resolveAppColor("com.tencent.mobileqq"))
        assertEquals("#FF2C6BED", MessageCard.resolveAppColor("org.thoughtcrime.securesms"))
        assertEquals("#FF5865F2", MessageCard.resolveAppColor("com.discord"))
        assertEquals("#FF007AFF", MessageCard.resolveAppColor("com.unknown.chat"))
    }
}
