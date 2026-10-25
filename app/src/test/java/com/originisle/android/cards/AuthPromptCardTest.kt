package com.originisle.android.cards

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPromptCardTest {

    @Test
    fun testAuthPromptDetection() {
        assertTrue(AuthPromptCard.isAuthPrompt("com.google.android.gms", "Trying to sign in from Windows PC? Tap yes."))
        assertTrue(AuthPromptCard.isAuthPrompt("com.azure.authenticator", "Approve sign-in request: Number 42"))
        assertTrue(AuthPromptCard.isAuthPrompt("com.okta.android.auth", "Did you just sign in? Approve or Deny"))
        assertFalse(AuthPromptCard.isAuthPrompt("com.whatsapp", "Hey let's meet at 5 PM"))
    }
}
