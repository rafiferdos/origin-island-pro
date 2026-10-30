package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated Next-Level Incoming & Ongoing Call Card for Origin Island Pro.
 * Intercepts phone, WhatsApp, Messenger, Telegram, and VoIP calls.
 * Displays high-contrast ergonomic action buttons directly on the expanded island:
 * - Decline: Refined Apple/OriginOS Crimson Red (#FF3B30) with [ic_call_end]
 * - Answer: Vibrant Emerald Green (#34C759) with [ic_call]
 * And renders:
 * - Left compact pill: Cleanly trimmed smart first name (skipping Md./Dr./etc.) + app/caller avatar
 * - Right compact pill: Live ticking call duration badge for ongoing calls or ringing wave for incoming
 * - Expanded card: Full caller name, duration/status, and Hang up button
 * - Deep linking: Tapping anywhere on the expanded card (outside Hang up) directly opens the active call screen!
 */
object CallCard {

    const val COLOR_DECLINE = 0xFFFF3B30.toInt()
    const val COLOR_ANSWER = 0xFF34C759.toInt()

    val CALL_PACKAGES = setOf(
        "com.google.android.dialer",
        "com.samsung.android.incallui",
        "com.android.dialer",
        "com.android.incallui",
        "com.vivo.upslide",
        "com.bbk.launcher2",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.orca",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.chopin",
        "com.google.android.apps.tachyon", // Google Meet
        "com.google.android.apps.meetings",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.viber.voip",
        "com.skype.raider",
        "com.truecaller",
        "org.thoughtcrime.securesms", // Signal
        "com.imo.android.imoim",
        "com.discord",
    )

    private val DURATION_REGEX = Regex("""\b(\d{1,2}:\d{2}(?::\d{2})?)\b""")

    private val callStartTimes = ConcurrentHashMap<Int, Long>()

    fun forget(id: Int) {
        callStartTimes.remove(id)
    }

    fun isCall(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val template = n.extras.getString(NotificationCompat.EXTRA_TEMPLATE).orEmpty()
        val isCategoryCall = n.category == NotificationCompat.CATEGORY_CALL ||
            template.contains("CallStyle", ignoreCase = true)
        if (isCategoryCall) return true

        val extras = n.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = "$title $text $subText $bigText".lowercase()

        // 1. Action button examination
        val actionTitles = n.actions.orEmpty().mapNotNull { it.title?.toString()?.lowercase() }
        val hasCallAction = actionTitles.any { act ->
            listOf("hang up", "hangup", "end call", "decline", "answer", "accept call", "return to call").any { act.contains(it) }
        }
        if (hasCallAction && (sbn.packageName in CALL_PACKAGES || sbn.isOngoing)) {
            return true
        }

        // 2. Keyword detection
        val callKeywords = listOf(
            "incoming call", "calling...", "voice call", "video call", "ringing",
            "call in progress", "ongoing call", "tap to return to call", "touch to return to call",
            "in call", "active call", "return to call",
        )
        if (callKeywords.any { combined.contains(it) }) {
            return true
        }

        val pkg = sbn.packageName
        if (pkg in CALL_PACKAGES && sbn.isOngoing) {
            val lowerPkg = pkg.lowercase()
            if (lowerPkg.contains("dialer") || lowerPkg.contains("incallui")) {
                return true
            }
        }
        return false
    }

    fun isOngoingCall(
        sbn: StatusBarNotification,
        answerAction: Notification.Action?,
        rawText: String,
        bigText: String,
        subText: String,
    ): Boolean {
        // If an Answer/Accept button is present, it's still incoming/ringing
        if (answerAction != null) return false

        // Ongoing notification flag
        if (sbn.isOngoing) return true

        val lower = "$rawText $bigText $subText".lowercase()
        return listOf(
            "ongoing", "in call", "call in progress", "active call",
            "connected", "call duration", "tap to return", "return to call",
        ).any { lower.contains(it) }
    }

    fun resolveCallDuration(
        id: Int,
        sbn: StatusBarNotification,
        rawText: String,
        bigText: String,
        subText: String,
    ): String {
        val n = sbn.notification
        val textHaystack = "$rawText $bigText $subText"

        // 1. Text snippet containing formatted timer (e.g. "02:45" or "12:34")
        val match = DURATION_REGEX.find(textHaystack)
        if (match != null) {
            return match.value
        }

        // 2. Notification timestamp (when)
        if (n.`when` > 0L) {
            val diffMs = System.currentTimeMillis() - n.`when`
            if (diffMs in 0..86_400_000L) {
                return formatElapsed(diffMs / 1000L)
            }
        }

        // 3. Dynamic elapsed timer tracked from start
        val startTime = callStartTimes.getOrPut(id) { System.currentTimeMillis() }
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000L
        return formatElapsed(elapsedSec)
    }

    fun extractCallDeepLinkIntent(
        context: Context,
        sbn: StatusBarNotification,
        declineAction: Notification.Action? = null,
    ): PendingIntent {
        val n = sbn.notification
        val declinePi = declineAction?.actionIntent

        // 1. fullScreenIntent - The primary Android standard for direct active call activity
        if (n.fullScreenIntent != null && n.fullScreenIntent != declinePi) {
            return n.fullScreenIntent
        }

        // 2. contentIntent - The primary notification tap intent which opens the active call screen
        if (n.contentIntent != null && n.contentIntent != declinePi) {
            return n.contentIntent
        }

        // 3. Conversation / bubble intent
        n.bubbleMetadata?.intent?.let { return it }

        // 4. Return to call / open call action intent if exposed
        n.actions?.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase().orEmpty()
            val isDestructive = isDeclineAction(action)
            val isCallOpen = listOf("return", "open", "show", "view", "call", "back", "screen").any { title.contains(it) }
            action.actionIntent != null && isCallOpen && !isDestructive && action.actionIntent != declinePi
        }?.actionIntent?.let { return it }

        // 5. Fallback: Launch the app's task with REORDER_TO_FRONT to bring the call activity directly to the foreground
        val launch = context.packageManager.getLaunchIntentForPackage(sbn.packageName)?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        } ?: Intent(context, MainActivity::class.java)

        return PendingIntent.getActivity(
            context,
            sbn.packageName.hashCode() xor 0xCA11,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun post(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ) {
        val n = sbn.notification
        val extras = n.extras
        val id = IconCache.castIdFor(sbn)

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        val callerName = IntelligentTextSanitizer.cleanTitle(rawTitle.ifBlank { rawText }.ifBlank { appLabel }, appLabel)

        // Cache icons
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        val avatarIcon = extractCallerAvatar(context, sbn)
        avatarIcon?.let { IconCache.activeLargeIcons[id] = it }

        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull()
        val pillIcon = avatarIcon ?: appIcon

        // Find Decline and Answer actions from notification
        val actions = n.actions.orEmpty()
        val declineAction = actions.firstOrNull { isDeclineAction(it) }
        val answerAction = actions.firstOrNull { isAnswerAction(it) }

        val isOngoing = isOngoingCall(sbn, answerAction, rawText, bigText, subText)
        val durationStr = if (isOngoing) resolveCallDuration(id, sbn, rawText, bigText, subText) else ""

        val details = when {
            isOngoing && durationStr.isNotBlank() -> durationStr
            rawText.isNotBlank() && rawText != callerName -> rawText
            bigText.isNotBlank() && bigText != callerName -> bigText
            subText.isNotBlank() -> subText
            else -> if (isOngoing) "Ongoing call" else "Incoming call"
        }

        val clickIntent = extractCallDeepLinkIntent(context, sbn, declineAction)

        val buttonTriples = mutableListOf<Notification.Action>()
        val buttonBgColors = ArrayList<Int>()
        val buttonTextColors = ArrayList<Int>()

        if (isOngoing) {
            // Ongoing calls:
            // STRICTLY single-button UI: only the clean Red "End Call" button is shown.
            // DO NOT show "Call Screen" or "Back to call screen" button.
            // Tapping anywhere else on the expanded card (avatar, caller name, duration, card body)
            // directly launches the active call screen via clickIntent!
            val hangUpAction = if (declineAction != null) {
                Notification.Action.Builder(
                    declineAction.getIcon() ?: Icon.createWithResource(context, R.drawable.ic_call_end),
                    "End Call",
                    declineAction.actionIntent,
                ).build()
            } else {
                val candidate = actions.firstOrNull { isDeclineAction(it) }
                    ?: actions.firstOrNull { !isAnswerAction(it) }
                if (candidate != null) {
                    Notification.Action.Builder(
                        candidate.getIcon() ?: Icon.createWithResource(context, R.drawable.ic_call_end),
                        "End Call",
                        candidate.actionIntent,
                    ).build()
                } else null
            }
            if (hangUpAction != null) {
                buttonTriples.add(hangUpAction)
                buttonBgColors.add(COLOR_DECLINE)
                buttonTextColors.add(Color.WHITE)
            }
        } else {
            // For incoming ringing calls, provide Decline (red) and Answer (green)
            if (declineAction != null) {
                buttonTriples.add(declineAction)
                buttonBgColors.add(COLOR_DECLINE)
                buttonTextColors.add(Color.WHITE)
            }
            if (answerAction != null) {
                buttonTriples.add(answerAction)
                buttonBgColors.add(COLOR_ANSWER)
                buttonTextColors.add(Color.WHITE)
            }

            if (buttonTriples.isEmpty()) {
                val usable = actions.filter { a ->
                    a.actionIntent != null && !a.title.isNullOrBlank()
                }.take(2)
                buttonTriples.addAll(usable)
                usable.forEach { a ->
                    val title = a.title.toString()
                    buttonBgColors.add(OriginIslandBuilder.resolveSemanticButtonBgColor(title))
                    buttonTextColors.add(OriginIslandBuilder.resolveSemanticButtonTextColor(title))
                }
            }
        }

        val hasButtons = buttonTriples.isNotEmpty()
        val smartCaller = IntelligentTextSanitizer.smartFirstName(callerName)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_CALL)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_landing_pkg", sbn.packageName)
            putExtra("oi_landing_info", sbn.packageName)
            putExtra("oi_template", if (hasButtons) OriginIslandConstants.TEMPLATE_BUTTONS else OriginIslandConstants.TEMPLATE_BASE)

            // Pill left: for ongoing calls, show ONLY the caller avatar / app icon (no text!)
            // For incoming ringing calls, show the smart caller name
            val pillLeft = if (isOngoing) "" else smartCaller.take(12)
            putExtra("oi_left_content", pillLeft)
            pillIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }

            // Expanded card info
            putExtra("title", callerName) // Full name when expanded
            putExtra("text", if (isOngoing) "$durationStr • Ongoing call • $appLabel" else "$details • $appLabel")
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)
            putExtra("icon_res", R.drawable.ic_call)

            // Crucial: Tapping anywhere on the expanded card (outside Hang up button) opens the call screen!
            putExtra("source_content_intent", clickIntent)
            putExtra("click_resp", clickIntent)

            // Ambient Edge Glow around expanded call card (Emerald Green)
            putExtra("oi_light_color", "#FF34C759")
            putExtra("oi_light_mode", 1)

            if (isOngoing) {
                // Pill right: Live ticking duration badge (e.g. "02:45")
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("oi_right_content", durationStr)
                putExtra("status_chip_text", durationStr)
                putExtra("oi_bg_color", "#FF34C759") // Call Emerald Green badge
                putExtra("oi_fg_color", "#FFFFFFFF")
            } else {
                // Pill right: Ringing waveform for incoming call
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("oi_wave_state", 1) // Active ringing wave
                putExtra("oi_right_content", "Incoming")
                putExtra("status_chip_text", "Ringing")
                putExtra("oi_bg_color", "#FF16161A")
                putExtra("oi_fg_color", "#FFFFFFFF")
            }

            if (hasButtons) {
                putParcelableArrayListExtra("actions", ArrayList(buttonTriples))
                putIntegerArrayListExtra("oi_button_bg_colors", buttonBgColors)
                putIntegerArrayListExtra("oi_button_text_colors", buttonTextColors)
            }
        }
        context.startService(intent)
    }

    private fun isDeclineAction(action: Notification.Action): Boolean {
        val t = action.title?.toString()?.lowercase().orEmpty()
        return listOf(
            "decline", "reject", "hang up", "hangup", "end call", "end",
            "dismiss", "cancel", "drop", "disconnect", "finish", "leave", "stop call",
        ).any { t.contains(it) }
    }

    private fun isAnswerAction(action: Notification.Action): Boolean {
        val t = action.title?.toString()?.lowercase().orEmpty()
        return listOf("answer", "accept", "pickup", "join", "return to call", "call screen", "return", "back to call").any { t.contains(it) }
    }

    private fun extractCallerAvatar(context: Context, sbn: StatusBarNotification): Icon? {
        val extras = sbn.notification.extras
        when (val large = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> return large
            is Bitmap -> return Icon.createWithBitmap(shrinkForIcon(large))
        }
        when (val largeBig = extras.get(NotificationCompat.EXTRA_LARGE_ICON_BIG)) {
            is Icon -> return largeBig
            is Bitmap -> return Icon.createWithBitmap(shrinkForIcon(largeBig))
        }
        sbn.notification.getLargeIcon()?.let { return it }
        return null
    }

    private fun shrinkForIcon(src: Bitmap): Bitmap {
        val max = 192
        if (src.width <= max && src.height <= max) return src
        val scale = max.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }
}
