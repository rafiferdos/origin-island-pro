package com.originisle.android.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the cast-id allocation policy.
 *
 * The old formula `(key.hashCode() and MAX_VALUE) % 10000 + 20000` handed the same id to any two
 * notifications whose keys hashed to the same slot. Those cards then shared icons and, worse, one
 * card's dismissal cleared the other's cached state. These tests assert the two properties that
 * matter: an id is stable for a key, and distinct live keys never share one.
 */
class IconCacheTest {

    private fun bandSlot(key: String): Int =
        (key.hashCode() and Int.MAX_VALUE) % (IconCache.NOTIFICATION_ID_BAND_END - IconCache.NOTIFICATION_ID_BAND_START + 1)

    /** Finds two distinct keys that collide in the notification band. */
    private fun collidingKeys(): Pair<String, String> {
        val seen = HashMap<Int, String>()
        for (i in 0 until 200_000) {
            val key = "collision-probe-$i"
            val slot = bandSlot(key)
            val previous = seen[slot]
            if (previous != null) return previous to key
            seen[slot] = key
        }
        throw AssertionError("no hash collision found in 200k keys — the band math is wrong")
    }

    @Test
    fun `id is stable for the same key`() {
        val key = "stable-key"
        val first = IconCache.castIdForKey(key)
        val second = IconCache.castIdForKey(key)
        assertEquals(first, second)
        assertEquals(first, IconCache.idForKey(key))
    }

    @Test
    fun `ids stay inside the notification band`() {
        for (i in 0 until 200) {
            val id = IconCache.castIdForKey("band-key-$i")
            assertTrue("id $id outside band", IconCache.isNotificationBandId(id))
        }
    }

    @Test
    fun `colliding keys get distinct ids`() {
        val (a, b) = collidingKeys()
        // Both keys really do collide in the raw formula — otherwise the test proves nothing.
        assertEquals(bandSlot(a), bandSlot(b))
        assertNotEquals(IconCache.castIdForKey(a), IconCache.castIdForKey(b))
    }

    @Test
    fun `many keys never share an id`() {
        val ids = (0 until 300).map { IconCache.castIdForKey("unique-key-$it") }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `tracked keys are bounded`() {
        for (i in 0 until IconCache.MAX_TRACKED_CARDS + 5) {
            IconCache.castIdForKey("bound-key-$i")
        }
        assertTrue(
            "tracked ${IconCache.trackedKeyCount()} keys",
            IconCache.trackedKeyCount() <= IconCache.MAX_TRACKED_CARDS,
        )
    }

    @Test
    fun `forgetKey releases the mapping`() {
        val key = "forget-key"
        IconCache.castIdForKey(key)
        IconCache.forgetKey(key)
        assertNull(IconCache.idForKey(key))
    }

    @Test
    fun `raiden and charging ids are outside the notification band`() {
        // If a reserved id drifts back into the band, a notification can steal a fixed card's slot.
        assertTrue(!IconCache.isNotificationBandId(com.originisle.android.cards.RaidenCard.RAIDEN_CAST_ID))
        assertTrue(!IconCache.isNotificationBandId(com.originisle.android.cards.RaidenCard.RAIDEN_REQUEST_CODE))
        assertTrue(!IconCache.isNotificationBandId(com.originisle.android.cards.ChargingCard.ID))
    }
}
