package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordCardTest {

    @Test
    fun testDiscordPackages() {
        assertTrue(DiscordCard.DISCORD_PACKAGES.contains("com.discord"))
        assertTrue(DiscordCard.DISCORD_PACKAGES.contains("com.discord.canary"))
        assertTrue(DiscordCard.DISCORD_PACKAGES.contains("com.discord.ptb"))
    }
}
