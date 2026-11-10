package com.originisle.android.island

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the card-type -> scene mapping.
 *
 * On a device that does not expose `getSceneStatus` (including the Robolectric sandbox), every
 * probe answers "unknown", which [SceneCapability] treats as available — so the first candidate
 * wins. That is the behaviour asserted here: the mapping itself, independent of what vivo grants.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SceneCapabilityTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `without a probe answer bestScene stays on the safe carrier scene`() {
        assertEquals("NAVIGATION", SceneCapability.bestScene(context, "TAXI", "NAVIGATION"))
        assertEquals("NAVIGATION", SceneCapability.bestScene(context, "NAVIGATION", "RIDE_GUIDE"))
        assertEquals("NAVIGATION", SceneCapability.bestScene(context))
        assertEquals("ONLY", SceneCapability.bestScene(context, "ONLY"))
    }

    @Test
    fun `without a probe answer every card type uses the safe carrier scene`() {
        listOf("NAVIGATION", "RIDE", "DELIVERY", "MEETING", "TIMER", "CALL", "MEDIA", "ride", "SOMETHING_ELSE", "")
            .forEach { assertEquals(it, "NAVIGATION", SceneCapability.sceneFor(it, context)) }
    }

    @Test
    fun `an unknown scene is not confirmed`() {
        assertEquals(false, SceneCapability.isConfirmed(context, "TAXI"))
    }

    @Test
    fun `an unavailable scene is never reported as available by default`() {
        // With no probe API the answer must be "available" (true), never false: treating
        // "unknown" as "no" would leave a device without the probe with zero scenes.
        assertEquals(true, SceneCapability.isAvailable(context, "NAVIGATION"))
        assertEquals(false, SceneCapability.supportsCustomTemplate(context, "NAVIGATION"))
    }

    @Test
    fun `reset drops the cached answers`() {
        SceneCapability.isAvailable(context, "TAXI")
        SceneCapability.reset()
        assertEquals(true, SceneCapability.isAvailable(context, "TAXI"))
    }
}
