package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.text.Html
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
 * Dedicated Next-Level Gmail & Email Card for Origin Island Pro.
 * Provides bespoke parsing, concise short-pill display, and rich expanded cards with
 * semantic actions (Archive, Reply, Delete, Mark Read).
 */
object MailCard {

    val MAIL_PACKAGES = setOf(
        "com.google.android.gm",
        "com.google.android.gm.lite",
        "com.microsoft.office.outlook",
        "com.samsung.android.email.provider",
        "com.yahoo.mobile.client.android.mail",
        "ch.protonmail.android",
        "me.bluemail.mail",
        "com.trtf.blue",
        "ru.yandex.mail",
        "com.fsck.k9",
    )

    data class ParsedMail(
        val sender: String,
        val subject: String,
        val bodySnippet: String,
        val account: String,
        val count: Int,
        val avatar: Icon?,
    )

    fun isMail(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in MAIL_PACKAGES) return true
        if (sbn.notification.category == NotificationCompat.CATEGORY_EMAIL) return true
        return false
    }

    fun post(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ) {
        val n = sbn.notification
        val id = IconCache.castIdFor(sbn)
        val mail = parseMail(context, sbn, appLabel)

        val appIcon = runCatching {
            val ai = context.packageManager.getApplicationInfo(sbn.packageName, 0)
            Icon.createWithResource(sbn.packageName, ai.icon)
        }.getOrNull() ?: Icon.createWithResource(context, R.drawable.ic_mail)

        val largeAvatar = parcelSafeIcon(context, mail.avatar) ?: appIcon
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }

        val showActions = context.getSharedPreferences(PREFS_NAME, 0)
            .getBoolean("cast_show_actions", true)
        val actions = if (showActions) buildMailActions(context, n, sbn.packageName) else emptyList()

        val subtext = if (mail.account.isNotBlank()) "$appLabel • ${mail.account}" else appLabel

        // Combined body text for the expanded card: Subject + Body snippet
        val expandedText = when {
            mail.subject.isNotBlank() && mail.bodySnippet.isNotBlank() ->
                "${mail.subject}\n${mail.bodySnippet}"
            mail.subject.isNotBlank() -> mail.subject
            mail.bodySnippet.isNotBlank() -> mail.bodySnippet
            else -> "New message received"
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", false)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_MESSAGE)
            putExtra("oi_scene", "NAVIGATION")
            putExtra(
                "oi_template",
                if (actions.size >= 2) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("title", mail.sender)
            putExtra("text", expandedText)
            putExtra("subtext", subtext)
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            // Short/Compact Pill presentation:
            // Left: Gmail icon + concise sender name
            putExtra("oi_left_content", shortSender(context, mail.sender))
            putExtra("oi_left_icon", appIcon)

            // Right: Concise Subject snippet (or "New Email")
            val rightSubject = mail.subject.ifBlank { mail.bodySnippet }.ifBlank { "New Email" }
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_right_content", rightSubject.clip(45))
            putExtra("status_chip_text", rightSubject.clip(45))

            // Expanded card presentation:
            val mailColor = if (sbn.packageName.contains("gmail", ignoreCase = true)) "#FFEA4335" else "#FF007AFF"
            putExtra("oi_light_color", mailColor)
            putExtra("oi_light_mode", 1)
            putExtra("oi_large_icon", largeAvatar)
            putExtra("icon_res", R.drawable.ic_mail)
            putExtra("when", n.`when`)
            putExtra(
                "source_content_intent",
                extractDeepLinkIntent(context, sbn),
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

    internal fun parseMail(
        context: Context,
        sbn: StatusBarNotification,
        appLabel: String,
    ): ParsedMail {
        val n = sbn.notification
        val extras = n.extras

        val rawTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        val largeIcon = when (val large = extras.get(NotificationCompat.EXTRA_LARGE_ICON)) {
            is Icon -> large
            is Bitmap -> Icon.createWithBitmap(shrinkForIcon(large))
            else -> null
        }

        // Multi-message lines in InboxStyle
        val textLines = extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES)
        val count = textLines?.size ?: 1

        var sender = cleanSenderName(rawTitle.ifBlank { appLabel })
        var subject = cleanText(rawText)
        var body = cleanText(bigText)

        // Gmail multi-line summary handling (e.g. "2 new messages")
        if (rawTitle.contains(Regex("""\d+\s+new\s+messages?""", RegexOption.IGNORE_CASE))) {
            val firstLine = textLines?.firstOrNull()?.toString().orEmpty()
            if (firstLine.contains(": ")) {
                val split = firstLine.split(": ", limit = 2)
                sender = cleanSenderName(split[0])
                subject = cleanText(split[1])
            }
        } else if (rawTitle.contains("<") && rawTitle.contains(">")) {
            // "Name <email@domain.com>" format
            val matcher = Regex("""^([^<]+)<([^>]+)>""").find(rawTitle)
            if (matcher != null) {
                sender = matcher.groupValues[1].trim()
            }
        }

        // If body is empty or identical to subject, check if bigText has more details
        if (body.isBlank() || body == subject) {
            body = ""
        }

        return ParsedMail(
            sender = sender.ifBlank { appLabel },
            subject = subject,
            bodySnippet = body,
            account = cleanText(subText),
            count = count,
            avatar = largeIcon,
        )
    }

    private fun buildMailActions(
        context: Context,
        n: Notification,
        pkg: String,
    ): List<Notification.Action> {
        val rawActions = n.actions.orEmpty()
        if (rawActions.isEmpty()) return emptyList()

        val results = mutableListOf<Notification.Action>()
        for (a in rawActions) {
            val title = a.title?.toString()?.trim().orEmpty()
            if (title.isBlank()) continue
            val lower = title.lowercase()

            val iconRes = when {
                lower.contains("archive") -> R.drawable.ic_archive
                lower.contains("reply") -> R.drawable.ic_reply
                lower.contains("delete") || lower.contains("trash") -> R.drawable.ic_delete
                else -> R.drawable.ic_mail
            }

            val icon = Icon.createWithResource(context, iconRes)
            results.add(Notification.Action.Builder(icon, title, a.actionIntent).build())
        }
        return results
    }

    private fun semanticActionColor(title: String): Pair<Int, Int> {
        val lower = title.lowercase()
        return when {
            lower.contains("archive") -> 0xFF2C344E.toInt() to 0xFFE0E6FF.toInt() // Slate Indigo
            lower.contains("reply") -> 0xFF1E3A2F.toInt() to 0xFF34C759.toInt()   // Vibrant Emerald
            lower.contains("delete") || lower.contains("trash") -> 0xFF3A1E22.toInt() to 0xFFFF453A.toInt() // Crimson
            lower.contains("read") -> 0xFF1E2D52.toInt() to 0xFF4D88FF.toInt()    // Sapphire
            else -> 0xFF24242C.toInt() to 0xFFFFFFFF.toInt()                      // High-contrast Obsidian
        }
    }

    private fun cleanSenderName(raw: String): String {
        return raw.replace(Regex("""\s*<.*?>"""), "")
            .replace(Regex("""\s*\(\d+.*?\)$"""), "")
            .replace(Regex("""\s*\[\d+.*?\]$"""), "")
            .trim()
    }

    private fun cleanText(raw: String): String {
        if (raw.isBlank()) return ""
        val unescaped = runCatching { Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString() }
            .getOrDefault(raw)
        return unescaped.replace('\n', ' ').trim()
    }

    private fun shortSender(context: Context, name: String): String {
        val clean = name.replace('\n', ' ').trim()
        if (clean.isBlank()) return "Gmail"
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
