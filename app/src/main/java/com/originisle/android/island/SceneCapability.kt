package com.originisle.android.island

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import android.util.Log

/**
 * Probes and caches which SuperX scenes vivo will actually grant this package.
 *
 * `OriginIslandConstants.SUPERX_SCENES` lists fifteen scenes, and the app used to request all of
 * them unconditionally. On a device that grants only a subset, the ungranted ones are simply
 * ignored by the system — so nothing was obviously broken, but the app had no idea which scenes
 * it could really use, and every card was hardwired to `NAVIGATION`.
 *
 * NotificationManager exposes two more hidden probes alongside `setSuperXInfosSceneList`:
 * `getSceneStatus(String pkg, String scene)` and `isSupportCustomFun(String pkg, String scene)`.
 * They are reflected here, defensively: if the method is absent (non-vivo build, or a future OS
 * that renames it) every probe reports "unknown" and the caller behaves exactly as it did before,
 * requesting everything. A probe that answers is cached per scene for the process lifetime.
 *
 * The grant itself has a retry policy: a missing method used to be cached as permanently
 * unavailable for the whole process, so one early failure (before the listener was fully
 * connected, before the user granted notification access) silently killed island access until the
 * app was restarted. It now retries after [RETRY_AFTER_MS].
 */
object SceneCapability {

    private const val TAG = "SceneCapability"

    /** How long a failed reflective lookup is trusted before it is retried. */
    private const val RETRY_AFTER_MS = 15 * 60 * 1000L

    @Volatile
    private var grantMethod: java.lang.reflect.Method? = null

    @Volatile
    private var grantFailedAt = 0L

    @Volatile
    private var statusMethod: java.lang.reflect.Method? = null

    @Volatile
    private var statusMethodProbed = false

    @Volatile
    private var customFunMethod: java.lang.reflect.Method? = null

    @Volatile
    private var customFunMethodProbed = false

    /** scene -> granted. Only scenes the API actually answered are present. */
    private val sceneStatus = LinkedHashMap<String, Boolean>()

    /** scene -> template-7 (custom RemoteViews) support. */
    private val customFunStatus = LinkedHashMap<String, Boolean>()

    /** Drops every cached answer, so the next grant re-probes from scratch. */
    fun reset() {
        grantMethod = null
        grantFailedAt = 0L
        statusMethod = null
        statusMethodProbed = false
        customFunMethod = null
        customFunMethodProbed = false
        synchronized(sceneStatus) { sceneStatus.clear() }
        synchronized(customFunStatus) { customFunStatus.clear() }
    }

    /**
     * Asks vivo to whitelist this package for every scene that reports as available.
     *
     * Falls back to requesting the full [OriginIslandConstants.SUPERX_SCENES] list when the probe
     * API is unavailable, which is the historical behaviour.
     */
    fun grant(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // A previously failed lookup is retried, but not hammered: one attempt per retry window.
        if (grantMethod == null && grantFailedAt != 0L &&
            SystemClock.elapsedRealtime() - grantFailedAt < RETRY_AFTER_MS
        ) {
            return
        }

        val method = grantMethod ?: runCatching {
            NotificationManager::class.java.getMethod(
                "setSuperXInfosSceneList",
                List::class.java, List::class.java, List::class.java, List::class.java,
            )
        }.getOrElse {
            grantFailedAt = SystemClock.elapsedRealtime()
            Log.w(TAG, "setSuperXInfosSceneList unavailable: ${it.message}")
            return
        }
        grantMethod = method
        grantFailedAt = 0L

        try {
            val all = OriginIslandConstants.SUPERX_SCENES
            val scenes = all.filter { isAvailable(context, it) }
            // If the probe API is unavailable every scene reports "available", so `scenes` is the
            // full list — identical to the old behaviour. Only a real answer prunes it.
            val granted = if (scenes.isEmpty()) all else scenes
            val enabled = ArrayList<String>()
            val pkgs = ArrayList<String>()
            val allowed = ArrayList<String>()
            repeat(granted.size) {
                enabled.add("true")
                pkgs.add(context.packageName)
                allowed.add("true")
            }
            method.invoke(nm, ArrayList(granted), enabled, pkgs, allowed)
            Log.d(TAG, "setSuperXInfosSceneList granted for ${context.packageName}: ${granted.size}/${all.size} scenes")
        } catch (e: Exception) {
            // The method exists but the call was refused (not vivo, permission revoked, …). Don't
            // cache this as permanent — a refusal can be transient, unlike a missing method.
            Log.w(TAG, "setSuperXInfosSceneList call failed: ${itOrNull(e)}")
        }
    }

    /**
     * Whether [scene] is currently granted to this package.
     *
     * @return the probe's answer, or `true` when the probe API is unavailable — "unknown" must
     *   never be treated as "no", or a device without the probe would get no scenes at all.
     */
    fun isAvailable(context: Context, scene: String): Boolean {
        synchronized(sceneStatus) { sceneStatus[scene]?.let { return it } }
        val answer = probeSceneStatus(context, scene)
        if (answer != null) {
            synchronized(sceneStatus) { sceneStatus[scene] = answer }
        }
        return answer ?: true
    }

    /**
     * Whether template 7 (a custom `RemoteViews`) is supported for [scene]. Unknown answers are
     * reported as `false`, which is the safe direction: the app simply never uses template 7.
     */
    fun supportsCustomTemplate(context: Context, scene: String): Boolean {
        synchronized(customFunStatus) { customFunStatus[scene]?.let { return it } }
        val answer = probeCustomFun(context, scene) ?: false
        synchronized(customFunStatus) { customFunStatus[scene] = answer }
        return answer
    }

    /** The carrier scene that was reliably granted on every tested device (docs/PROTOCOL.md §1). */
    const val SAFE_SCENE = "NAVIGATION"

    /**
     * Whether the probe positively confirmed [scene] as granted. Unlike [isAvailable], "unknown"
     * is false here: posting under a scene vivo never granted is silently dropped, so a card may
     * only leave the safe carrier scene on a real "yes".
     */
    fun isConfirmed(
        context: Context,
        scene: String,
    ): Boolean {
        synchronized(sceneStatus) { sceneStatus[scene]?.let { return it } }
        val answer = probeSceneStatus(context, scene) ?: return false
        synchronized(sceneStatus) { sceneStatus[scene] = answer }
        return answer
    }

    /**
     * Picks the first candidate scene the probe confirmed, so a card can prefer the most specific
     * scene. Without a confirmation it falls back to [SAFE_SCENE] when that is a candidate, else
     * to the last candidate. Always returns a scene.
     */
    fun bestScene(context: Context, vararg candidates: String): String {
        if (candidates.isEmpty()) return SAFE_SCENE
        candidates.forEach { if (isConfirmed(context, it)) return it }
        return if (SAFE_SCENE in candidates) SAFE_SCENE else candidates.last()
    }

    /**
     * The scene a given card type should use. Replaces the 44 hardcoded `"NAVIGATION"` sites.
     *
     * Order matters: the first probe-confirmed candidate wins, so a card lands on the most specific
     * surface vivo will give it.
     */
    fun sceneFor(cardType: String, context: Context): String = when (cardType.uppercase()) {
        "NAVIGATION" -> bestScene(context, "NAVIGATION", "RIDE_GUIDE")
        "RIDE" -> bestScene(context, "TAXI", "RIDE_GUIDE", "NAVIGATION")
        "DELIVERY" -> bestScene(context, "TAKEOUT", "DELIEVERY", "NAVIGATION")
        "MEETING" -> bestScene(context, "METTING", "NAVIGATION")
        "TIMER" -> bestScene(context, "TIMER", "NAVIGATION")
        "CALL" -> bestScene(context, "INCALLING", "VOIPCALL", "NAVIGATION")
        "MEDIA" -> bestScene(context, "MOVIE", "NAVIGATION")
        else -> bestScene(context, "NAVIGATION")
    }

    /** Scenes the probe has actually confirmed, for logging and diagnostics. */
    fun grantedScenes(): List<String> = synchronized(sceneStatus) { sceneStatus.filterValues { it }.keys.toList() }

    private fun probeSceneStatus(context: Context, scene: String): Boolean? {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        if (!statusMethodProbed) {
            statusMethodProbed = true
            statusMethod = runCatching {
                NotificationManager::class.java.getMethod(
                    "getSceneStatus",
                    String::class.java, String::class.java,
                )
            }.getOrNull()
        }
        val m = statusMethod ?: return null
        return runCatching { m.invoke(nm, context.packageName, scene) as? Boolean }.getOrNull()
    }

    private fun probeCustomFun(context: Context, scene: String): Boolean? {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        if (!customFunMethodProbed) {
            customFunMethodProbed = true
            customFunMethod = runCatching {
                NotificationManager::class.java.getMethod(
                    "isSupportCustomFun",
                    String::class.java, String::class.java,
                )
            }.getOrNull()
        }
        val m = customFunMethod ?: return null
        return runCatching { m.invoke(nm, context.packageName, scene) as? Boolean }.getOrNull()
    }

    private fun itOrNull(e: Exception): String = e.message ?: e.javaClass.simpleName
}
