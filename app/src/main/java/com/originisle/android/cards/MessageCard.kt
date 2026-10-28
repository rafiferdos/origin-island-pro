package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.util.PriorityArbitrator
import com.originisle.android.ui.PREFS_NAME

/**
 * A one-off message (WhatsApp, Instagram, X, Messenger, Snapchat, SMS…, and any other plain
 * notification) as a chat bubble on the island:
 *
 *  - **Pill**: split around the camera the way Apple's Dynamic Island and OPPO's Fluid Cloud do —
 *    who on the left (avatar + name), what they said on the right (the message, as wide as the
 *    right half allows; vivo ellipsizes past that and scrolls it once when a new one lands).
 *  - **Card**: sender (or group) as the title, the full latest message, the avatar, and
 *    "WhatsApp · 3 new messages" underneath. Tapping opens the conversation.
 *
 * MessagingStyle notifications give the real sender, avatar and group; anything else falls back to
 * title/text, which is how X, Snapchat and Instagram's activity notifications are built.
 */
object MessageCard {

    /** Long enough for the island to ellipsize itself; short enough to keep the parcel small. */
    private const val LINE_MAX_CHARS = 60
    private const val NAME_WIDTH_DP = 72f
    private const val NAME_TEXT_DP = 12f

    internal data class Message(
        val sender: String,
        val text: String,
        val group: String?,
        val avatar: Icon?,
        val count: Int,
    )

    fun post(context: Context, sbn: StatusBarNotification, appLabel: String) {
        val n = sbn.notification
        val id = IconCache.castIdFor(sbn)
        val msg = read(context, sbn, appLabel)
        // Privacy mode (opt-in, default off): vivo-safe — only the text we send in
        // the existing template changes. Sender + app stay visible so the user still
        // knows who it is from; the message body itself is never put on the island.
        val privacyOn = context.getSharedPreferences(PREFS_NAME, 0)
            .getBoolean("cast_privacy_mode", false)
        val displayText = if (privacyOn) "New message" else msg.text

        val appIcon = runCatching {
            Icon.createWithResource(sbn.packageName, context.packageManager.getApplicationInfo(sbn.packageName, 0).icon)
        }.getOrNull()
        val contactAvatar = parcelSafeIcon(context, msg.avatar)
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }

        val top = msg.sender
        val subtext = if (privacyOn) appLabel else if (msg.count > 1) "$appLabel · ${msg.count} new messages" else appLabel

        val buttons = if (context.getSharedPreferences(PREFS_NAME, 0).getBoolean("cast_show_actions", true)) {
            GenericCard.usableActions(n)
        } else {
            emptyList()
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", false)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_MESSAGE)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template",
                if (buttons.size >= 2) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("title", if (msg.group != null) "${msg.sender} · ${msg.group}" else msg.sender)
            putExtra("text", displayText)
            putExtra("subtext", subtext)
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            // Short/compact pill: App icon on left + clean sender name
            putExtra("oi_left_content", shortName(context, top))
            (appIcon ?: contactAvatar)?.let {
                putExtra("oi_left_icon", it)
            }
            // Expanded card: contact photo avatar
            (contactAvatar ?: appIcon)?.let {
                putExtra("oi_large_icon", it)
            }

            // Right side of compact pill: message preview
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_right_content", displayText.clip(LINE_MAX_CHARS))
            putExtra("status_chip_text", displayText.clip(LINE_MAX_CHARS))
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("when", n.`when`)
            putExtra("source_content_intent", extractDeepLinkIntent(context, sbn))
            if (buttons.isNotEmpty()) putParcelableArrayListExtra("actions", ArrayList(buttons))
        }
        context.startService(intent)
    }

    /** Sender, text, group and avatar — handles MessagingStyle as well as title/text apps. */
    internal fun read(context: Context, sbn: StatusBarNotification, appLabel: String): Message {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body = (extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty())
            .ifBlank { extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty() }
            .ifBlank { extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty() }
        val largeIcon = when (val large = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> large
            is Bitmap -> Icon.createWithBitmap(shrinkForIcon(large))
            else -> null
        }

        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val last = style?.messages?.lastOrNull()
        if (style != null && last != null) {
            val rawSender = last.person?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: style.user.name?.toString()?.trim()?.takeIf { last.person == null && it.isNotEmpty() }
                ?: title.ifBlank { appLabel }
            val sender = cleanSender(rawSender, appLabel)
            val group = style.conversationTitle?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() && style.isGroupConversation && it != appLabel }
            val avatar = last.person?.icon?.let { runCatching { it.toIcon(context) }.getOrNull() } ?: largeIcon
            val text = last.text?.toString()?.trim().orEmpty().ifBlank { body }
            return Message(sender, text.ifBlank { "New message" }, group, avatar, style.messages.size)
        }

        // Non-MessagingStyle chat / social apps (IMO, Viber, Instagram DM, SMS, Discord, etc.)
        var cleanTitle = title
            .replace(Regex("""\s*\(\d+.*?\)$"""), "")
            .replace(Regex("""\s*\[\d+.*?\]$"""), "")
            .trim()
        var cleanBody = body.trim()

        // Handle "Sender: Message" format inside title or body
        if (cleanTitle.contains(": ")) {
            val split = cleanTitle.split(": ", limit = 2)
            if (split[0].isNotBlank() && split[1].isNotBlank()) {
                cleanTitle = split[0].trim()
                if (cleanBody.isBlank() || cleanBody == title) {
                    cleanBody = split[1].trim()
                }
            }
        } else if (cleanBody.startsWith("$cleanTitle: ")) {
            cleanBody = cleanBody.removePrefix("$cleanTitle: ").trim()
        }

        return Message(
            sender = cleanSender(cleanTitle, appLabel),
            text = cleanBody.ifBlank { cleanTitle }.ifBlank { "New message" },
            group = null,
            avatar = largeIcon,
            count = 1,
        )
    }

    /** Clean unneeded counts or prefixes from sender names. */
    private fun cleanSender(raw: String, fallback: String): String =
        raw.replace(Regex("""\s*\(\d+.*?\)$"""), "")
            .replace(Regex("""\s*\[\d+.*?\]$"""), "")
            .trim()
            .ifBlank { fallback }

    /**
     * Clean and format sender's short name for the island pill.
     * Handles common prefixes ("Md.", "Dr.", "Prof.") and prevents aggressive letter truncation.
     */
    private fun shortName(context: Context, name: String): String {
        val clean = name.replace('\n', ' ')
            .replace(Regex("""\s*\(\d+.*?\)$"""), "")
            .replace(Regex("""\s*\[\d+.*?\]$"""), "")
            .trim()
        if (clean.isBlank()) return "Message"

        val parts = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val titlePrefixes = setOf("md.", "md", "dr.", "dr", "prof.", "prof", "engr.", "engr", "adv.", "adv")
        val candidate = when {
            parts.size >= 2 && parts[0].lowercase() in titlePrefixes -> "${parts[0]} ${parts[1]}"
            parts.isNotEmpty() -> parts[0]
            else -> clean
        }

        val density = context.resources.displayMetrics.density
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = NAME_TEXT_DP * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val room = NAME_WIDTH_DP * density
        return TextUtils.ellipsize(candidate, paint, room, TextUtils.TruncateAt.END).toString()
    }

    private fun String.clip(max: Int): String =
        replace('\n', ' ').let { if (it.length > max) it.take(max - 1) + "…" else it }
}
