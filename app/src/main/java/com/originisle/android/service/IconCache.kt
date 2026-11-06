package com.originisle.android.service

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaController
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared per-cast-id state, written by the card posters in `cards/` (as they inspect a source
 * notification) and read by [com.originisle.android.island.PlaygroundService] (when it builds the
 * SuperX bundle) and [MediaControlReceiver] (to dispatch transport controls back to the right
 * [MediaController]). Centralising this avoids the card posters and the service depending on each
 * other just to share these maps.
 *
 * ## Id allocation
 *
 * Notification-derived cast ids live in `[NOTIFICATION_ID_BAND_START, NOTIFICATION_ID_BAND_END]`.
 * That band used to be computed as `(key.hashCode() and MAX_VALUE) % 10000 + 20000` with no
 * collision handling, so two notifications whose keys hashed to the same slot shared an id —
 * they would overwrite each other's icons and one card's dismissal would clear the other's state.
 * Allocation now keeps a key→id map and probes linearly for the next free slot, so the id is
 * stable for a key *and* unique among live keys.
 *
 * Reserved ids are outside the band by construction: match cards use 30000-34999, charging uses
 * 35001, Raiden uses 35002/35003, and sample cards use 40000+. `RaidenCard` previously used
 * 29500/29501 — inside the notification band — which is exactly the collision this fixes.
 *
 * ## Bounding
 *
 * Every map here is process-global and was previously unbounded. At most [MAX_TRACKED_CARDS] keys
 * are tracked; beyond that the least-recently-used key is evicted and its card state cleared.
 */
object IconCache {

    const val NOTIFICATION_ID_BAND_START = 20000
    const val NOTIFICATION_ID_BAND_END = 29999

    /** Upper bound on tracked notification keys, matching CastLog's discipline. */
    const val MAX_TRACKED_CARDS = 40

    val activeControllers = ConcurrentHashMap<Int, MediaController>()
    val activeSmallIcons = ConcurrentHashMap<Int, Icon>()
    val activeLargeIcons = ConcurrentHashMap<Int, Icon>()
    val activeLargeBitmaps = ConcurrentHashMap<Int, Bitmap>()

    /** notification key -> allocated cast id. Access-ordered, so the eldest key is the LRU victim. */
    private val keyToId = LinkedHashMap<String, Int>(16, 0.75f, true)

    private val usedIds = HashSet<Int>()

    /** Deterministic, stable, collision-free cast id for [sbn]. */
    @Synchronized
    fun castIdFor(sbn: StatusBarNotification): Int = castIdForKey(sbn.key)

    /**
     * Stable cast id for a notification [key], unique among currently tracked keys.
     *
     * Exposed separately from [castIdFor] so the allocation policy can be unit-tested without
     * constructing a `StatusBarNotification`.
     */
    @Synchronized
    fun castIdForKey(key: String): Int {
        keyToId[key]?.let { return it }
        val id = allocate(key)
        keyToId[key] = id
        usedIds.add(id)
        trim()
        return id
    }

    /** The id currently assigned to [key], or null when it is not tracked. */
    @Synchronized
    fun idForKey(key: String): Int? = keyToId[key]

    /**
     * The slot [key] would naturally occupy, without allocating or tracking it.
     *
     * Used on the removal path: when the key was evicted by the LRU bound, its card may still be up
     * under this id, and the cancel must still reach it. This reproduces the old stateless formula
     * exactly, so behaviour is unchanged for any key that is no longer tracked.
     */
    fun preferredCastId(key: String): Int {
        val span = NOTIFICATION_ID_BAND_END - NOTIFICATION_ID_BAND_START + 1
        return (key.hashCode() and Int.MAX_VALUE) % span + NOTIFICATION_ID_BAND_START
    }

    /** True when [id] is inside the band notification-derived ids occupy. */
    fun isNotificationBandId(id: Int): Boolean =
        id in NOTIFICATION_ID_BAND_START..NOTIFICATION_ID_BAND_END

    /**
     * Forgets the key→id mapping for [key] and clears the card state. Called when the source
     * notification goes away, so a later notification with the same key gets a fresh slot.
     */
    @Synchronized
    fun forgetKey(key: String) {
        val id = keyToId.remove(key) ?: return
        usedIds.remove(id)
        clear(id)
    }

    /** Drop everything cached for a card id once its source notification is gone. */
    fun clear(id: Int) {
        activeControllers.remove(id)
        activeSmallIcons.remove(id)
        activeLargeIcons.remove(id)
        activeLargeBitmaps.remove(id)
    }

    @Synchronized
    fun trackedKeyCount(): Int = keyToId.size

    private fun allocate(key: String): Int {
        val span = NOTIFICATION_ID_BAND_END - NOTIFICATION_ID_BAND_START + 1
        val preferred = (key.hashCode() and Int.MAX_VALUE) % span + NOTIFICATION_ID_BAND_START
        if (preferred !in usedIds) return preferred
        // Linear probe: deterministic for a given key set, and keeps an id near its key's natural
        // slot so related notifications stay clustered.
        for (offset in 1 until span) {
            val candidate = NOTIFICATION_ID_BAND_START +
                ((preferred - NOTIFICATION_ID_BAND_START + offset) % span)
            if (candidate !in usedIds) return candidate
        }
        // Band exhausted. Reuse the preferred slot rather than throw: 10k slots against a
        // 40-key bound makes this unreachable, and a shared id is preferable to crashing a
        // notification listener.
        return preferred
    }

    private fun trim() {
        while (keyToId.size > MAX_TRACKED_CARDS) {
            val eldest = keyToId.keys.iterator().next()
            val id = keyToId.remove(eldest) ?: continue
            usedIds.remove(id)
            clear(id)
        }
    }
}
