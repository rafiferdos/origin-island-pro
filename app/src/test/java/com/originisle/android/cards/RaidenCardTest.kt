package com.originisle.android.cards

import com.originisle.android.util.PriorityArbitrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaidenCardTest {

    @Test
    fun testCandidateAndPackageMatching() {
        // Known candidate packages
        assertTrue("ai.muse" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertTrue("com.muse.app" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertTrue("com.hermes.muse" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertTrue("ai.raiden" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertTrue("com.raiden.app" in RaidenCard.CANDIDATE_MUSE_APPS)

        // Strict non-substring matching: random apps containing "muse" or "raiden" MUST NOT be in candidate set
        assertFalse("org.musescore.player" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertFalse("com.example.muse.client" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertFalse("org.raiden.agent" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertFalse("com.musemusic" in RaidenCard.CANDIDATE_MUSE_APPS)

        // System packages are not in candidate set and unconditionally stripped
        assertFalse("android" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertFalse("com.android.systemui" in RaidenCard.CANDIDATE_MUSE_APPS)
        assertFalse("com.vivo.upslide" in RaidenCard.CANDIDATE_MUSE_APPS)
    }

    @Test
    fun testStateParsing() {
        assertEquals(RaidenCard.RaidenState.WORKING, RaidenCard.parseState("Muse", "Working on your request"))
        assertEquals(RaidenCard.RaidenState.TYPING, RaidenCard.parseState("Raiden", "Typing response..."))
        assertEquals(RaidenCard.RaidenState.CODING, RaidenCard.parseState("Muse", "Coding in NotificationCastListener.kt"))
        assertEquals(RaidenCard.RaidenState.READING_GMAIL, RaidenCard.parseState("Raiden", "Reading Gmail inbox"))
        assertEquals(RaidenCard.RaidenState.SEARCHING, RaidenCard.parseState("Muse", "Searching the web for documentation"))
        assertEquals(RaidenCard.RaidenState.WAITING, RaidenCard.parseState("Raiden", "Waiting for your confirmation"))
        assertEquals(RaidenCard.RaidenState.DONE, RaidenCard.parseState("Muse", "Task completed successfully"))
        assertEquals(RaidenCard.RaidenState.ERROR, RaidenCard.parseState("Raiden", "Needs attention: API rate limit"))
        assertEquals(RaidenCard.RaidenState.MESSAGE, RaidenCard.parseState("Raiden", "Here is your summary"))
    }

    @Test
    fun testCapsuleAndShortTaskFormatting() {
        // Working
        val (shortWork, fullWork) = RaidenCard.sanitizeTask("processing prompt", RaidenCard.RaidenState.WORKING)
        assertEquals("processing prompt", shortWork)
        assertEquals("Raiden is processing prompt", fullWork)
        assertEquals("Raiden Working", RaidenCard.RaidenState.WORKING.capsuleText)

        // Typing
        val (shortType, fullType) = RaidenCard.sanitizeTask("hello there", RaidenCard.RaidenState.TYPING)
        assertEquals("Typing", shortType)
        assertEquals("Raiden: hello there", fullType)
        assertEquals("Raiden Typing", RaidenCard.RaidenState.TYPING.capsuleText)

        // Coding: file or task name in expanded mode only!
        val (shortCode, fullCode) = RaidenCard.sanitizeTask("Refactoring services", RaidenCard.RaidenState.CODING, "RaidenCard.kt")
        assertEquals("Coding", shortCode) // Short mode does not leak long file or task name
        assertEquals("Raiden is coding in RaidenCard.kt", fullCode) // Expanded mode shows file name
        assertEquals("Raiden Coding", RaidenCard.RaidenState.CODING.capsuleText)

        // Reading Gmail: strict privacy, no raw private body in capsule
        val (shortMail, fullMail) = RaidenCard.sanitizeTask("Confidential email body with secret numbers 12345", RaidenCard.RaidenState.READING_GMAIL)
        assertEquals("Reading Gmail", shortMail)
        assertEquals("Raiden is reading Gmail", fullMail)
        assertEquals("Raiden Reading Gmail", RaidenCard.RaidenState.READING_GMAIL.capsuleText)

        // Searching
        val (shortSearch, fullSearch) = RaidenCard.sanitizeTask("Jetpack Compose docs", RaidenCard.RaidenState.SEARCHING)
        assertEquals("Jetpack Compose docs", shortSearch)
        assertEquals("Raiden is searching for Jetpack Compose docs", fullSearch)
        assertEquals("Raiden Searching", RaidenCard.RaidenState.SEARCHING.capsuleText)

        // Waiting
        val (shortWait, fullWait) = RaidenCard.sanitizeTask("confirm overwrite", RaidenCard.RaidenState.WAITING)
        assertEquals("Waiting", shortWait)
        assertEquals("Raiden is waiting: confirm overwrite", fullWait)
        assertEquals("Raiden Waiting", RaidenCard.RaidenState.WAITING.capsuleText)

        // Done
        val (shortDone, fullDone) = RaidenCard.sanitizeTask("all tests passed", RaidenCard.RaidenState.DONE)
        assertEquals("Done", shortDone)
        assertEquals("Raiden completed: all tests passed", fullDone)
        assertEquals("Raiden Done", RaidenCard.RaidenState.DONE.capsuleText)

        // Error
        val (shortErr, fullErr) = RaidenCard.sanitizeTask("connection lost", RaidenCard.RaidenState.ERROR)
        assertEquals("Needs Attention", shortErr)
        assertEquals("Raiden needs attention: connection lost", fullErr)
        assertEquals("Raiden Needs Attention", RaidenCard.RaidenState.ERROR.capsuleText)
    }

    @Test
    fun testTextSanitizationRedaction() {
        val dirtyText = "Working with sk-1234567890abcdef1234 and Bearer my-secret-auth-token-xyz token=supersecret"
        val (_, full) = RaidenCard.sanitizeTask(dirtyText, RaidenCard.RaidenState.WORKING)
        assertFalse(full.contains("sk-1234567890abcdef1234"))
        assertFalse(full.contains("my-secret-auth-token-xyz"))
        assertFalse(full.contains("supersecret"))
        assertTrue(full.contains("[REDACTED_KEY]"))
        assertTrue(full.contains("[REDACTED_TOKEN]"))
        assertTrue(full.contains("token=[REDACTED]"))
    }

    @Test
    fun testPriorityRankingAndCallProtection() {
        // Priority Arbitrator constants check
        assertEquals(100, PriorityArbitrator.PRIORITY_CALL)
        assertEquals(95, PriorityArbitrator.PRIORITY_NAVIGATION)
        assertEquals(90, PriorityArbitrator.PRIORITY_TIMER)
        assertEquals(85, PriorityArbitrator.PRIORITY_RAIDEN_LIVE)
        assertEquals(40, PriorityArbitrator.PRIORITY_RAIDEN_MESSAGE)

        // Raiden live (85) sits strictly below Call (100), Navigation (95), and Timer (90)
        assertTrue(PriorityArbitrator.PRIORITY_RAIDEN_LIVE < PriorityArbitrator.PRIORITY_TIMER)
        assertTrue(PriorityArbitrator.PRIORITY_RAIDEN_LIVE < PriorityArbitrator.PRIORITY_NAVIGATION)
        assertTrue(PriorityArbitrator.PRIORITY_RAIDEN_LIVE < PriorityArbitrator.PRIORITY_CALL)

        // Raiden message (40) sits at Message level
        assertEquals(PriorityArbitrator.PRIORITY_MESSAGE, PriorityArbitrator.PRIORITY_RAIDEN_MESSAGE)
        assertEquals(PriorityArbitrator.PRIORITY_RAIDEN_LIVE, PriorityArbitrator.priorityForKind("raiden_live"))
        assertEquals(PriorityArbitrator.PRIORITY_RAIDEN_MESSAGE, PriorityArbitrator.priorityForKind("raiden_message"))
    }

    @Test
    fun testLiveVsTransientStates() {
        // Live ongoing states
        assertTrue(RaidenCard.RaidenState.WORKING.isLive)
        assertTrue(RaidenCard.RaidenState.TYPING.isLive)
        assertTrue(RaidenCard.RaidenState.CODING.isLive)
        assertTrue(RaidenCard.RaidenState.READING_GMAIL.isLive)
        assertTrue(RaidenCard.RaidenState.SEARCHING.isLive)
        assertTrue(RaidenCard.RaidenState.WAITING.isLive)

        // Non-ongoing transient states
        assertFalse(RaidenCard.RaidenState.DONE.isLive)
        assertFalse(RaidenCard.RaidenState.ERROR.isLive)
        assertFalse(RaidenCard.RaidenState.MESSAGE.isLive)
    }

    @Test
    fun testPriorityQueueingDecisionLogic() {
        // Active priorities map simulation
        val activePriorities = mutableMapOf<Int, Int>()

        // 1. When no higher card is active, Raiden should post immediately
        val hasHigherInitial = activePriorities.any { (id, p) ->
            id != RaidenCard.RAIDEN_CAST_ID && p >= PriorityArbitrator.PRIORITY_TIMER
        }
        assertFalse(hasHigherInitial)

        // 2. When a Call (100) is active, Raiden should queue
        activePriorities[20000] = PriorityArbitrator.PRIORITY_CALL
        val hasHigherWithCall = activePriorities.any { (id, p) ->
            id != RaidenCard.RAIDEN_CAST_ID && p >= PriorityArbitrator.PRIORITY_TIMER
        }
        assertTrue(hasHigherWithCall)

        // 3. When a Navigation (95) is active, Raiden should queue
        activePriorities.clear()
        activePriorities[20001] = PriorityArbitrator.PRIORITY_NAVIGATION
        val hasHigherWithNav = activePriorities.any { (id, p) ->
            id != RaidenCard.RAIDEN_CAST_ID && p >= PriorityArbitrator.PRIORITY_TIMER
        }
        assertTrue(hasHigherWithNav)

        // 4. When a Timer (90) is active, Raiden should queue
        activePriorities.clear()
        activePriorities[20002] = PriorityArbitrator.PRIORITY_TIMER
        val hasHigherWithTimer = activePriorities.any { (id, p) ->
            id != RaidenCard.RAIDEN_CAST_ID && p >= PriorityArbitrator.PRIORITY_TIMER
        }
        assertTrue(hasHigherWithTimer)

        // 5. When Media (50) or Message (40) is active, Raiden (85) should NOT queue
        activePriorities.clear()
        activePriorities[20003] = PriorityArbitrator.PRIORITY_MEDIA
        activePriorities[20004] = PriorityArbitrator.PRIORITY_MESSAGE
        val hasHigherWithMedia = activePriorities.any { (id, p) ->
            id != RaidenCard.RAIDEN_CAST_ID && p >= PriorityArbitrator.PRIORITY_TIMER
        }
        assertFalse(hasHigherWithMedia)
    }
}
