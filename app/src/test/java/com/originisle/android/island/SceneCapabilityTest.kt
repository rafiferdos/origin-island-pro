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
    fun `bestScene prefers the first candidate`() {
        assertEquals("TAXI", SceneCapability.bestScene(context, "TAXI", "NAVIGATION"))
        assertEquals("MOVIE", SceneCapability.bestScene(context, "MOVIE", "NAVIGATION"))
    }

    @Test
    fun `bestScene always returns a scene`() {
        assertEquals("NAVIGATION", SceneCapability.bestScene(context))
        assertEquals("ONLY", SceneCapability.bestScene(context, "ONLY"))
    }

    @Test
    fun `sceneFor maps each card type to its preferred scene`() {
        assertEquals("NAVIGATION", SceneCapability.sceneFor("NAVIGATION", context))
        assertEquals("TAXI", SceneCapability.sceneFor("RIDE", context))
        assertEquals("TAKEOUT", SceneCapability.sceneFor("DELIVERY", context))
        assertEquals("METTING", SceneCapability.sceneFor("MEETING", context))
        assertEquals("TIMER", SceneCapability.sceneFor("TIMER", context))
        assertEquals("INCALLING", SceneCapability.sceneFor("CALL", context))
        assertEquals("MOVIE", SceneCapability.sceneFor("MEDIA", context))
    }

    @Test
    fun `sceneFor is case-insensitive and falls back for unknown types`() {
        assertEquals("TAXI", SceneCapability.sceneFor("ride", context))
        assertEquals("NAVIGATION", SceneCapability.sceneFor("SOMETHING_ELSE", context))
        assertEquals("NAVIGATION", SceneCapability.sceneFor("", context))
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
