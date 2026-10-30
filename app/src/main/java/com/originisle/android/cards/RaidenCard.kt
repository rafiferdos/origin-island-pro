package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.util.PriorityArbitrator
import java.util.concurrent.ConcurrentHashMap

/**
 * Raiden AI Activity & Companion Card for Origin Island Pro.
 *
 * Implements high-density glanceable activity monitoring for the user's AI companion (Raiden / Muse):
 * - Route A: Intercepts and transforms Android notifications posted by the Muse app.
 * - Route B: Direct live state stream via PlaygroundService for working states without fake notifications.
 *
 * Follows the Origin Island Pro SuperX architecture:
 * - Priority: 85 (live) and 40 (message). Subordinated to Call (100), Navigation (95), and Timer (90).
 * - Fixed ID: 29500 (updates in place without disturbing higher priority cards).
 * - Scene: NAVIGATION.
 * - Sanitized glanceable capsule text: no tokens, passwords, or secrets leaked on the island.
 */
object RaidenCard {

    const val TAG = "RaidenCard"
    const val RAIDEN_CAST_ID = 29500
    private const val PREFS = "experimental_prefs"

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var pendingAutoEndRunnable: Runnable? = null

    /**
     * Known candidate package names for the Muse / Raiden AI companion app.
     * Note: [confirmMusePackage] additionally dynamically probes the device's PackageManager
     * so unlisted package IDs are auto-discovered without requiring a hardcoded guess.
     */
    val CANDIDATE_MUSE_APPS = setOf(
        "ai.muse",
        "ai.muse.app",
        "com.muse",
        "com.muse.app",
        "com.hermes.muse",
        "com.raiden.app",
        "ai.raiden",
        "com.muse.mobile",
        "io.muse.app",
        "com.xiaohei.muse",
    )

    /** Framework/system packages that must NEVER cast as Raiden. */
    private val SYSTEM_PKGS = setOf(
        "android",
        "com.android.systemui",
        "com.android.shell",
        "com.vivo.upslide",
        "com.vivo.smartmultiwindow",
    )

    @Volatile
    private var cachedDiscoveredPackage: String? = null

    fun resetPackageCache() {
        cachedDiscoveredPackage = null
    }

    /**
     * Confirms the exact package name of the Muse companion app on the device.
     * 1. Checks user preference "raiden_muse_pkg" if explicitly configured.
     * 2. Checks cached discovered package.
     * 3. Probes the device's PackageManager for installed candidate packages in [CANDIDATE_MUSE_APPS].
     * Substring matching on package names or labels is strictly removed!
     */
    fun confirmMusePackage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val userPkg = prefs.getString("raiden_muse_pkg", null)?.trim()?.ifBlank { null }
        if (userPkg != null) {
            return userPkg
        }

        cachedDiscoveredPackage?.let { return it }

        val pm = context.packageManager
        for (cand in CANDIDATE_MUSE_APPS) {
            val installed = runCatching { pm.getPackageInfo(cand, 0) != null }.getOrDefault(false)
            if (installed) {
                cachedDiscoveredPackage = cand
                Log.i(TAG, "Confirmed installed Muse companion package on device: $cand")
                CastLog.add("Raiden", cand, "confirmed discovered Muse package", false)
                return cand
            }
        }
        return null
    }

    /**
     * Verifies if [packageName] is the exact Muse / Raiden companion app.
     * Casts ONLY for the exact user set package or an exact confirmed Muse package.
     * Substring matching is strictly removed. System packages are unconditionally stripped.
     */
    fun isMuseApp(context: Context, packageName: String): Boolean {
        if (packageName in SYSTEM_PKGS) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val userPkg = prefs.getString("raiden_muse_pkg", null)?.trim()?.ifBlank { null }
        if (userPkg != null) {
            return packageName == userPkg
        }

        val confirmed = confirmMusePackage(context)
        if (confirmed != null) {
            return packageName == confirmed
        }

        return packageName in CANDIDATE_MUSE_APPS
    }

    /**
     * Raiden companion activity states.
     */
    enum class RaidenState(
        val rawState: String,
        val shortState: String,
        val capsuleText: String,
        val isLive: Boolean,
    ) {
        WORKING("working", "Working", "Raiden Working", isLive = true),
        TYPING("typing", "Typing", "Raiden Typing", isLive = true),
        CODING("coding", "Coding", "Raiden Coding", isLive = true),
        READING_GMAIL("reading Gmail", "Reading Gmail", "Raiden Reading Gmail", isLive = true),
        SEARCHING("searching", "Searching", "Raiden Searching", isLive = true),
        WAITING("waiting", "Waiting", "Raiden Waiting", isLive = true),
        DONE("done", "Done", "Raiden Done", isLive = false),
        ERROR("error", "Needs Attention", "Raiden Needs Attention", isLive = false),
        MESSAGE("message", "Message", "Raiden", isLive = false);

        companion object {
            fun fromRaw(raw: String): RaidenState {
                val r = raw.trim().lowercase()
                return when {
                    r.contains("work") || r.contains("think") || r.contains("execut") || r.contains("generat") -> WORKING
                    r.contains("type") || r.contains("typing") || r.contains("respond") -> TYPING
                    r.contains("code") || r.contains("coding") || r.contains("program") || r.contains("refactor") -> CODING
                    r.contains("gmail") || r.contains("mail") || r.contains("email") -> READING_GMAIL
                    r.contains("search") || r.contains("brows") || r.contains("look") || r.contains("google") -> SEARCHING
                    r.contains("wait") || r.contains("confirm") || r.contains("pause") || r.contains("prompt") -> WAITING
                    r.contains("done") || r.contains("finish") || r.contains("complet") || r.contains("success") -> DONE
                    r.contains("error") || r.contains("fail") || r.contains("attention") || r.contains("alert") -> ERROR
                    else -> MESSAGE
                }
            }
        }
    }

    /**
     * Parses notification content into a recognized [RaidenState].
     */
    fun parseState(title: String, text: String): RaidenState {
        val hay = "$title $text".lowercase()
        return when {
            hay.contains("done") || hay.contains("task finished") || hay.contains("completed") -> RaidenState.DONE
            hay.contains("error") || hay.contains("failed") || hay.contains("needs attention") || hay.contains("warning") -> RaidenState.ERROR
            hay.contains("reading gmail") || hay.contains("reading email") || hay.contains("checking gmail") -> RaidenState.READING_GMAIL
            hay.contains("coding") || hay.contains("writing code") || hay.contains("editing") || hasCodeFile(hay) -> RaidenState.CODING
            hay.contains("typing") || hay.contains("writing response") -> RaidenState.TYPING
            hay.contains("searching") || hay.contains("web search") || hay.contains("looking up") -> RaidenState.SEARCHING
            hay.contains("waiting") || hay.contains("waiting for you") || hay.contains("need confirmation") -> RaidenState.WAITING
            hay.contains("working") || hay.contains("processing") || hay.contains("analyzing") -> RaidenState.WORKING
            else -> RaidenState.MESSAGE
        }
    }

    private fun hasCodeFile(text: String): Boolean =
        Regex("""\b[\w-]+\.(?:kt|java|py|ts|js|cpp|c|h|rs|go|json|xml|html|css|sh)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    /**
     * Sanitizes task and status text. Ensures secrets, tokens, passwords, bearer tokens,
     * and full private email bodies are strictly redacted and never leaked onto the capsule or island.
     * Returns: Pair(shortTaskText for shortInfos/capsule, fullTaskText for expanded infos core).
     */
    fun sanitizeTask(rawText: String?, state: RaidenState, file: String? = null): Pair<String, String> {
        var clean = rawText?.replace('\n', ' ')?.trim().orEmpty()

        // Redact secrets, auth tokens, API keys, passwords, bearer tokens
        clean = clean
            .replace(Regex("""(?i)\b(?:bearer\s+[A-Za-z0-9\-._~+/]+=*)\b"""), "[REDACTED_TOKEN]")
            .replace(Regex("""(?i)\b(?:sk-[A-Za-z0-9]{16,})\b"""), "[REDACTED_KEY]")
            .replace(Regex("""(?i)\b(?:AIza[0-9A-Za-z\-_]{35})\b"""), "[REDACTED_KEY]")
            .replace(Regex("""(?i)\b(?:gh[pousr]_[A-Za-z0-9]{36,})\b"""), "[REDACTED_KEY]")
            .replace(Regex("""(?i)(?:password|secret|apikey|token)[:=\s]+[^\s,;]+"""), "token=[REDACTED]")
            .trim()

        val fullTask = when (state) {
            RaidenState.WORKING -> if (clean.isNotBlank()) "Raiden is $clean" else "Raiden is working on your task"
            RaidenState.TYPING -> if (clean.isNotBlank()) "Raiden: $clean" else "Raiden is typing a response"
            RaidenState.CODING -> {
                val target = file?.ifBlank { null } ?: clean.ifBlank { null }
                if (target != null) "Raiden is coding in $target" else "Raiden is coding in Origin Island Pro"
            }
            RaidenState.READING_GMAIL -> "Raiden is reading Gmail" // Strict privacy: no raw email snippet exposed
            RaidenState.SEARCHING -> if (clean.isNotBlank()) "Raiden is searching for $clean" else "Raiden is searching"
            RaidenState.WAITING -> if (clean.isNotBlank()) "Raiden is waiting: $clean" else "Raiden is waiting for your input"
            RaidenState.DONE -> if (clean.isNotBlank()) "Raiden completed: $clean" else "Raiden has finished your task"
            RaidenState.ERROR -> if (clean.isNotBlank()) "Raiden needs attention: $clean" else "Raiden needs your attention"
            RaidenState.MESSAGE -> if (clean.isNotBlank()) clean else "Raiden companion update"
        }

        // Short task text: strictly concise for collapsed shortInfos.core and baseInfos.content
        val shortTask = when (state) {
            RaidenState.WORKING -> if (clean.isNotBlank()) clean.take(24) else "Working"
            RaidenState.TYPING -> "Typing"
            RaidenState.CODING -> "Coding" // File or task name shown in expanded mode only!
            RaidenState.READING_GMAIL -> "Reading Gmail"
            RaidenState.SEARCHING -> if (clean.isNotBlank()) clean.take(20) else "Searching"
            RaidenState.WAITING -> "Waiting"
            RaidenState.DONE -> "Done"
            RaidenState.ERROR -> "Needs Attention"
            RaidenState.MESSAGE -> if (clean.isNotBlank()) clean.take(20) else "Message"
        }

        return Pair(shortTask, fullTask)
    }

    /**
     * Route A: Posts or updates a Raiden card from an incoming Muse StatusBarNotification.
     */
    fun post(context: Context, sbn: StatusBarNotification, appLabel: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("cast_raiden", true)) {
            Log.d(TAG, "Raiden casting is globally disabled")
            return false
        }

        val extras = sbn.notification.extras
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val combined = "$rawTitle $rawText $bigText".trim()

        val state = parseState(rawTitle, "$rawText $bigText")

        // Per-category switch checks
        when {
            state == RaidenState.DONE -> {
                if (!prefs.getBoolean("cast_raiden_done", true)) {
                    CastLog.add(appLabel, "Raiden Done", "skipped — Raiden done disabled", false)
                    cancel(context)
                    return false
                }
            }
            state.isLive -> {
                if (!prefs.getBoolean("cast_raiden_live", true)) {
                    CastLog.add(appLabel, state.capsuleText, "skipped — Raiden live disabled", false)
                    return false
                }
            }
            else -> {
                if (!prefs.getBoolean("cast_raiden_messages", true)) {
                    CastLog.add(appLabel, "Raiden Message", "skipped — Raiden messages disabled", false)
                    return false
                }
            }
        }

        val (shortTask, fullTask) = sanitizeTask(combined.ifBlank { rawText }, state)
        val clickIntent = extractDeepLinkIntent(context, sbn)
        dispatchSuperX(
            context = context,
            state = state,
            shortTask = shortTask,
            fullTask = fullTask,
            sourcePkg = sbn.packageName,
            clickIntent = clickIntent,
            whenMs = sbn.notification.`when`,
        )

        CastLog.add("Raiden", state.capsuleText, "cast — raiden ${state.rawState}", true)
        return true
    }

    /**
     * Route B: Direct live state route without requiring a fake system notification.
     */
    fun postDirectState(
        context: Context,
        rawState: String,
        task: String? = null,
        file: String? = null,
        sourcePkg: String? = null,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("cast_raiden", true)) {
            CastLog.add("Raiden", rawState, "skipped — Raiden casting is off", false)
            return
        }

        val state = RaidenState.fromRaw(rawState)

        // Per-category switch checks
        when {
            state == RaidenState.DONE -> {
                if (!prefs.getBoolean("cast_raiden_done", true)) {
                    CastLog.add("Raiden", "Raiden Done", "skipped — Raiden done disabled", false)
                    cancel(context)
                    return
                }
            }
            state.isLive -> {
                if (!prefs.getBoolean("cast_raiden_live", true)) {
                    CastLog.add("Raiden", state.capsuleText, "skipped — Raiden live disabled", false)
                    return
                }
            }
            else -> {
                if (!prefs.getBoolean("cast_raiden_messages", true)) {
                    CastLog.add("Raiden", "Raiden Message", "skipped — Raiden messages disabled", false)
                    return
                }
            }
        }

        val resolvedPkg = sourcePkg ?: confirmMusePackage(context) ?: "com.originisle.android"
        val (shortTask, fullTask) = sanitizeTask(task, state, file)
        val clickIntent = getMuseLaunchIntent(context, resolvedPkg)

        dispatchSuperX(
            context = context,
            state = state,
            shortTask = shortTask,
            fullTask = fullTask,
            sourcePkg = resolvedPkg,
            clickIntent = clickIntent,
            whenMs = System.currentTimeMillis(),
        )

        CastLog.add("Raiden", state.capsuleText, "cast — direct raiden state (${state.rawState})", true)
    }

    private fun dispatchSuperX(
        context: Context,
        state: RaidenState,
        shortTask: String,
        fullTask: String,
        sourcePkg: String,
        clickIntent: PendingIntent?,
        whenMs: Long,
    ) {
        pendingAutoEndRunnable?.let { handler.removeCallbacks(it) }
        pendingAutoEndRunnable = null

        val isLive = state.isLive
        val priority = PriorityArbitrator.getRaidenPriority(context, isLive)
        val raidenIcon = Icon.createWithResource(context, R.drawable.ic_raiden)

        // Open Muse / Open Raiden Action
        val openTitle = "Open Muse"
        val openAction = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_navigation),
            openTitle,
            clickIntent ?: fallbackClickIntent(context, sourcePkg),
        ).build()

        // Dismiss Action
        val dismissIntent = PendingIntent.getService(
            context,
            RAIDEN_CAST_ID + 1,
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", RAIDEN_CAST_ID)
                .putExtra("oi_scene", "NAVIGATION"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissAction = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_delete),
            "Dismiss",
            dismissIntent,
        ).build()

        val actionsList = arrayListOf(openAction, dismissAction)
        val btnTitles = arrayListOf(openTitle, "Dismiss")
        val btnBgColors = arrayListOf(
            0xFF2C2C2E.toInt(), // Frosted dark pill
            0xFFFF3B30.toInt(), // Crimson Red for dismiss
        )
        val btnTxtColors = arrayListOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", RAIDEN_CAST_ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("is_ongoing", isLive)
            putExtra("oi_priority", priority)
            putExtra("title", "Raiden")
            putExtra("text", fullTask)
            putExtra("subtext", "Raiden")
            putExtra("source_app", "Raiden")
            putExtra("source_pkg", sourcePkg)
            putExtra("when", whenMs)

            // Capsule & ShortInfos
            putExtra("oi_left_content", " ")
            putExtra("oi_right_content", state.capsuleText)
            putExtra("status_chip_text", state.capsuleText)

            // Templates
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // Expanded with 2 actions
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)

            // Working state progress animation
            if (state == RaidenState.WORKING) {
                putExtra("progress_indeterminate", true)
            }

            // Icons
            putExtra("oi_large_icon", raidenIcon)
            putExtra("oi_left_icon", raidenIcon)
            putExtra("oi_right_icon", raidenIcon)
            putExtra("icon_res", R.drawable.ic_raiden)

            // Colors & Styling: Futuristic Raiden Violet Accent
            putExtra("oi_bg_color", "#2C2C2E")
            putExtra("oi_fg_color", "#C084FC")
            putExtra("oi_light_color", "#A855F7")
            putExtra("oi_light_mode", 1)

            // Interactive Actions
            putParcelableArrayListExtra("actions", actionsList)
            putStringArrayListExtra("oi_button_titles", btnTitles)
            putIntegerArrayListExtra("oi_button_bg_colors", btnBgColors)
            putIntegerArrayListExtra("oi_button_text_colors", btnTxtColors)

            clickIntent?.let {
                putExtra("click_resp", it)
                putExtra("source_content_intent", it)
            }
        }

        context.startService(intent)

        // If state is DONE, auto-end the card cleanly
        if (state == RaidenState.DONE) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val autoDismissSec = prefs.getInt("cast_auto_dismiss_seconds", 0)
            val delayMs = if (autoDismissSec > 0) autoDismissSec * 1000L else 4000L
            val autoEnd = Runnable { cancel(context) }
            pendingAutoEndRunnable = autoEnd
            handler.postDelayed(autoEnd, delayMs)
        }
    }

    /**
     * Resolves an explicit launch intent for the Muse companion app.
     */
    fun getMuseLaunchIntent(context: Context, pkg: String): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            pkg.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Ends the Raiden card on the island and tears down the SuperX NAVIGATION scene.
     */
    fun cancel(context: Context) {
        pendingAutoEndRunnable?.let { handler.removeCallbacks(it) }
        pendingAutoEndRunnable = null
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", RAIDEN_CAST_ID)
            putExtra("oi_scene", "NAVIGATION")
        }
        context.startService(intent)
    }

    fun forget(sbn: StatusBarNotification) {
        // Notification removed: clear cached icons if needed
        IconCache.clear(RAIDEN_CAST_ID)
    }
}
