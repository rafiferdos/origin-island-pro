package com.originisle.android.cards

import android.app.Notification
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
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.util.PriorityArbitrator

/**
 * Dedicated Next-Level Discord Optimization Card for Origin Island Pro.
 * Handles Server channels, Direct Messages (DMs), and @mentions with bespoke layout,
 * clean markdown stripping, and semantic Discord actions (Reply, Mark Read, Mute).
 */
object DiscordCard {

    val DISCORD_PACKAGES = setOf(
        "com.discord",
        "com.discord.canary",
        "com.discord.ptb",
    )

    data class ParsedDiscord(
        val sender: String,
        val channel: String?,
        val server: String?,
        val message: String,
        val isDm: Boolean,
        val isMention: Boolean,
        val avatar: Icon?,
    )

    fun isDiscord(sbn: StatusBarNotification): Boolean {
        return sbn.packageName in DISCORD_PACKAGES
    }

    fun post(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ) {
        val n = sbn.notification
        val id = IconCache.castIdFor(sbn)
        val discord = parseDiscord(context, sbn, appLabel)

        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull() ?: Icon.createWithResource(context, R.drawable.ic_discord)

        val largeAvatar = parcelSafeIcon(context, discord.avatar) ?: appIcon
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }

        val showActions = context.getSharedPreferences(PREFS_NAME, 0)
            .getBoolean("cast_show_actions", true)
        val actions = if (showActions) buildDiscordActions(context, n) else emptyList()

        val cardTitle = when {
            discord.channel != null -> "${discord.sender} • ${discord.channel}"
            discord.isDm -> "${discord.sender} (DM)"
            else -> discord.sender
        }

        val subtext = when {
            discord.server != null -> "Discord • ${discord.server}"
            discord.isDm -> "Discord • Direct Message"
            else -> "Discord"
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", false)
            putExtra(
                "oi_priority",
                if (discord.isMention) PriorityArbitrator.PRIORITY_AUTH_PROMPT
                else PriorityArbitrator.PRIORITY_MESSAGE,
            )
            putExtra("oi_scene", "NAVIGATION")
            putExtra(
                "oi_template",
                if (actions.size >= 2) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("title", cardTitle)
            putExtra("text", discord.message)
            putExtra("subtext", subtext)
            putExtra("source_app", "Discord")
            putExtra("source_pkg", sbn.packageName)

            // Short/Compact Pill presentation:
            // Left: Discord icon + Channel name (#general) or @Sender
            val leftText = when {
                discord.channel != null -> discord.channel
                discord.sender.startsWith("@") -> discord.sender
                else -> "@${shortSender(context, discord.sender)}"
            }
            putExtra("oi_left_content", leftText)
            putExtra("oi_left_icon", appIcon)

            // Right: Message content snippet, or mention alert
            val rightSnippet = if (discord.isMention) "🔔 ${discord.message}" else discord.message
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_right_content", rightSnippet.clip(45))
            putExtra("status_chip_text", rightSnippet.clip(45))

            // Expanded card presentation:
            putExtra("oi_large_icon", largeAvatar)
            putExtra("icon_res", R.drawable.ic_discord)
            putExtra("when", n.`when`)
            putExtra(
                "source_content_intent",
                n.contentIntent ?: fallbackClickIntent(context, sbn.packageName),
            )

            if (actions.isNotEmpty()) {
                putParcelableArrayListExtra("actions", ArrayList(actions))
                val btnTitles = ArrayList<String>()
                val btnBgColors = ArrayList<Int>()
                val btnTxtColors = ArrayList<Int>()
                actions.take(3).forEach { a ->
                    btnTitles.add(a.title?.toString().orEmpty())
                    val colors = semanticActionColor(a.title?.toString().orEmpty())
                    btnBgColors.add(colors.first)
                    btnTxtColors.add(colors.second)
                }
                putStringArrayListExtra("oi_button_titles", btnTitles)
                putIntegerArrayListExtra("oi_button_bg_colors", btnBgColors)
                putIntegerArrayListExtra("oi_button_text_colors", btnTxtColors)
            }
        }
        context.startService(intent)
    }

    internal fun parseDiscord(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ): ParsedDiscord {
        val n = sbn.notification
        val extras = n.extras

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val convTitle = extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()

        val largeIcon = when (val large = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> large
            is Bitmap -> Icon.createWithBitmap(shrinkForIcon(large))
            else -> null
        }

        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val lastMsg = style?.messages?.lastOrNull()

        var sender: String
        var channel: String? = null
        var server: String? = subText.takeIf { it.isNotBlank() }
        var message: String
        var isDm = false

        if (style != null && lastMsg != null) {
            sender = lastMsg.person?.name?.toString()?.trim().orEmpty()
                .ifBlank { style.user.name?.toString()?.trim().orEmpty() }
                .ifBlank { rawTitle }

            message = lastMsg.text?.toString()?.trim().orEmpty().ifBlank { rawText }
            val conv = style.conversationTitle?.toString()?.trim()

            if (conv != null) {
                if (conv.startsWith("#")) {
                    channel = conv
                } else if (conv.contains(" > #")) {
                    val parts = conv.split(" > #", limit = 2)
                    server = parts[0].trim()
                    channel = "#${parts[1].trim()}"
                } else {
                    channel = "#$conv"
                }
            }
            isDm = !style.isGroupConversation && channel == null
        } else {
            // Title formats:
            // "Sender (#channel)" or "Server > #channel" or "Sender"
            var parsedSender = rawTitle
            var parsedMsg = rawText.ifBlank { bigText }

            if (parsedSender.contains(" > #")) {
                val parts = parsedSender.split(" > #", limit = 2)
                server = parts[0].trim()
                channel = "#${parts[1].trim()}"
                if (parsedMsg.contains(": ")) {
                    val msgParts = parsedMsg.split(": ", limit = 2)
                    parsedSender = msgParts[0].trim()
                    parsedMsg = msgParts[1].trim()
                }
            } else if (parsedSender.contains("(#")) {
                val matcher = Regex("""^(.*?)\s*\(#(.*?)\)""").find(parsedSender)
                if (matcher != null) {
                    parsedSender = matcher.groupValues[1].trim()
                    channel = "#${matcher.groupValues[2].trim()}"
                }
            } else if (convTitle != null) {
                channel = if (convTitle.startsWith("#")) convTitle else "#$convTitle"
            }

            if (parsedMsg.startsWith("$parsedSender: ")) {
                parsedMsg = parsedMsg.removePrefix("$parsedSender: ").trim()
            }

            sender = parsedSender.ifBlank { "Discord" }
            message = parsedMsg
            isDm = channel == null
        }

        // Clean Discord raw formatting
        val cleanMsg = cleanDiscordMarkdown(message)
        val cleanSender = cleanSenderName(sender)
        val isMention = cleanMsg.contains("@everyone") ||
            cleanMsg.contains("@here") ||
            cleanMsg.contains(Regex("""@\w+""")) ||
            rawTitle.contains("@")

        return ParsedDiscord(
            sender = cleanSender.ifBlank { "Discord" },
            channel = channel,
            server = server,
            message = cleanMsg.ifBlank { "New message" },
            isDm = isDm,
            isMention = isMention,
            avatar = largeIcon,
        )
    }

    private fun buildDiscordActions(
        context: Context,
        n: Notification,
    ): List<Notification.Action> {
        val rawActions = n.actions.orEmpty()
        if (rawActions.isEmpty()) return emptyList()

        val results = mutableListOf<Notification.Action>()
        for (a in rawActions) {
            val title = a.title?.toString()?.trim().orEmpty()
            if (title.isBlank()) continue
            val lower = title.lowercase()

            val iconRes = when {
                lower.contains("reply") -> R.drawable.ic_reply
                lower.contains("read") -> R.drawable.ic_mail
                lower.contains("mute") -> R.drawable.ic_alert
                else -> R.drawable.ic_discord
            }

            val icon = Icon.createWithResource(context, iconRes)
            results.add(Notification.Action.Builder(icon, title, a.actionIntent).build())
        }
        return results
    }

    private fun semanticActionColor(title: String): Pair<Int, Int> {
        val lower = title.lowercase()
        return when {
            lower.contains("reply") -> 0xFF5865F2.toInt() to 0xFFFFFFFF.toInt() // Discord Blurple
            lower.contains("read") -> 0xFF1E2D52.toInt() to 0xFF4D88FF.toInt()  // Sapphire Blue
            lower.contains("mute") -> 0xFF24242C.toInt() to 0xFFE0E0E0.toInt()  // Dark Slate
            else -> 0xFF24242C.toInt() to 0xFFFFFFFF.toInt()
        }
    }

    private fun cleanDiscordMarkdown(text: String): String {
        return text
            .replace(Regex("""<@!?\d+>"""), "@mention")
            .replace(Regex("""<#\d+>"""), "#channel")
            .replace(Regex("""<a?:(\w+):\d+>"""), ":$1:")
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""\*(.*?)\*"""), "$1")
            .replace(Regex("""~~(.*?)~~"""), "$1")
            .replace(Regex("""`(.*?)`"""), "$1")
            .replace('\n', ' ')
            .trim()
    }

    private fun cleanSenderName(raw: String): String {
        return raw.replace(Regex("""\s*\(\d+.*?\)$"""), "")
            .replace(Regex("""\s*\[\d+.*?\]$"""), "")
            .trim()
    }

    private fun shortSender(context: Context, name: String): String {
        val clean = name.replace('\n', ' ').trim()
        if (clean.isBlank()) return "Discord"
        val parts = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val candidate = parts.firstOrNull() ?: clean

        val density = context.resources.displayMetrics.density
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val room = 72f * density
        return TextUtils.ellipsize(candidate, paint, room, TextUtils.TruncateAt.END).toString()
    }

    private fun String.clip(max: Int): String =
        replace('\n', ' ').let { if (it.length > max) it.take(max - 1) + "…" else it }
}
