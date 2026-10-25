package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MailCardTest {

    @Test
    fun testMailPackages() {
        assertTrue(MailCard.MAIL_PACKAGES.contains("com.google.android.gm"))
        assertTrue(MailCard.MAIL_PACKAGES.contains("com.google.android.gm.lite"))
        assertTrue(MailCard.MAIL_PACKAGES.contains("com.microsoft.office.outlook"))
        assertTrue(MailCard.MAIL_PACKAGES.contains("com.yahoo.mobile.client.android.mail"))
        assertTrue(MailCard.MAIL_PACKAGES.contains("ch.protonmail.android"))
    }
}
