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
import com.originisle.android.R
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator

/**
 * Dedicated Next-Level Incoming & Ongoing Call Card for Origin Island Pro.
 * Intercepts phone, WhatsApp, Messenger, Telegram, and VoIP calls.
 * Displays high-contrast ergonomic action buttons directly on the expanded island:
 * - Decline: Refined Apple/OriginOS Crimson Red (#FF3B30) with [ic_call_end]
 * - Answer: Vibrant Emerald Green (#34C759) with [ic_call]
 * And renders a dynamic ringing waveform or caller avatar on the compact pill.
 */
object CallCard {

    const val COLOR_DECLINE = 0xFFFF3B30.toInt()
    const val COLOR_ANSWER = 0xFF34C759.toInt()

    val CALL_PACKAGES = setOf(
        "com.google.android.dialer",
        "com.samsung.android.incallui",
        "com.android.dialer",
        "com.android.incallui",
        "com.whatsapp",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.apps.tachyon", // Google Meet
        "com.viber.voip",
        "com.skype.raider",
        "com.truecaller",
    )

    fun isCall(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val template = n.extras.getString(NotificationCompat.EXTRA_TEMPLATE).orEmpty()
        val isCategoryCall = n.category == NotificationCompat.CATEGORY_CALL ||
            template.contains("CallStyle", ignoreCase = true)
        if (isCategoryCall) return true

        val pkg = sbn.packageName
        if (pkg in CALL_PACKAGES) {
            val text = "${n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)} ${n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)}".lowercase()
            if (listOf("incoming call", "calling...", "voice call", "video call", "ringing", "call in progress").any { text.contains(it) }) {
                return true
            }
        }
        return false
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
        val details = when {
            rawText.isNotBlank() && rawText != callerName -> rawText
            bigText.isNotBlank() && bigText != callerName -> bigText
            subText.isNotBlank() -> subText
            else -> "Incoming call"
        }

        // Cache icons
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        val avatarIcon = extractCallerAvatar(context, sbn)
        avatarIcon?.let { IconCache.activeLargeIcons[id] = it }

        // Find Decline and Answer actions from notification
        val actions = n.actions.orEmpty()
        val declineAction = actions.firstOrNull { isDeclineAction(it) }
        val answerAction = actions.firstOrNull { isAnswerAction(it) }

        val buttonTriples = mutableListOf<Notification.Action>()
        val buttonBgColors = ArrayList<Int>()
        val buttonTextColors = ArrayList<Int>()

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

        // If specific decline/answer actions weren't resolved, use all actionable buttons
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

        val hasButtons = buttonTriples.isNotEmpty()
        val clickIntent = extractDeepLinkIntent(context, sbn)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_CALL)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", if (hasButtons) OriginIslandConstants.TEMPLATE_BUTTONS else OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
            putExtra("oi_wave_state", 1) // Active ringing wave
            putExtra("title", callerName)
            putExtra("text", "$details • $appLabel")
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            // Pill left: Caller Name (crisp, adaptive)
            putExtra("oi_left_content", callerName.take(14))
            avatarIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            // Pill right: Ringing wave or short chip
            putExtra("oi_right_content", "Incoming")
            putExtra("status_chip_text", "Ringing")
            putExtra("icon_res", R.drawable.ic_call)
            putExtra("source_content_intent", clickIntent)

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
        return listOf("decline", "reject", "hang up", "end", "dismiss", "cancel", "drop").any { t.contains(it) }
    }

    private fun isAnswerAction(action: Notification.Action): Boolean {
        val t = action.title?.toString()?.lowercase().orEmpty()
        return listOf("answer", "accept", "pickup", "join").any { t.contains(it) }
    }

    private fun extractCallerAvatar(context: Context, sbn: StatusBarNotification): Icon? {
        when (val large = sbn.notification.extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> return large
            is Bitmap -> return Icon.createWithBitmap(shrinkForIcon(large))
        }
        return runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull()
    }

    private fun shrinkForIcon(src: Bitmap): Bitmap {
        val max = 192
        if (src.width <= max && src.height <= max) return src
        val scale = max.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }
}
