package com.originisle.android.util

/**
 * Universal Intelligent Text Sanitizer for Origin Island Pro.
 *
 * Intercepted notifications often contain redundant app labels, boilerplate status phrases,
 * unread counters in brackets, or verbose timestamps. This engine strips the noise and distills
 * the text into concise, high-density glanceable snippets for the vivo dynamic island pill.
 */
object IntelligentTextSanitizer {

    private val BOILERPLATE_PREFIXES = listOf(
        Regex("""^(?:you have (?:a )?new notification(?: from)?[:\s]*)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:new message(?: from)?[:\s]*)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:incoming message[:\s]*)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:notification[:\s]*)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:alert[:\s]*)""", RegexOption.IGNORE_CASE),
        Regex("""^(?:update[:\s]*)""", RegexOption.IGNORE_CASE),
    )

    private val BOILERPLATE_SUFFIXES = listOf(
        Regex("""\s*[•·|\-—]\s*(?:tap|click) to (?:open|view|see)(?: details)?.*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(tap to (?:open|view|see)(?: details)?\)$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[-–]\s*tap to reply$""", RegexOption.IGNORE_CASE),
    )

    private val UNREAD_COUNT_REGEX = Regex("""\s*[\(\[]\d+(?:\s*(?:new|unread)?\s*(?:msgs?|messages?))?[\)\]]$""", RegexOption.IGNORE_CASE)

    private val TITLE_PREFIXES = setOf(
        // Religious & South Asian Honorifics (with & without dot, common abbreviations)
        "md", "md.", "m.d.", "m.d", "mohd", "mohd.", "moh'd", "mohammad", "mohammed", "muhammad", "muhammed",
        "mst", "mst.", "most", "most.", "musammat", "mosammat",
        "sheikh", "shaikh", "syed", "sayed", "kazi", "qazi", "al", "el", "haji", "haji.",
        "mir", "chowdhury",
        // Professional titles
        "dr", "dr.", "doc", "doctor",
        "prof", "prof.", "professor",
        "engr", "engr.", "engineer",
        "adv", "adv.", "advocate",
        "barrister",
        "capt", "capt.", "captain", "col", "col.", "colonel", "maj", "maj.", "major", "gen", "gen.", "general",
        // Social titles
        "mr", "mr.", "mrs", "mrs.", "ms", "ms.", "miss", "sir", "madam"
    )

    /**
     * Extracts a clean, single first name for the compact island pill.
     * Intelligently strips honorifics and titles (e.g. "Md.", "MD", "Dr.", "Engr.", "Sheikh"),
     * handles prefixes without space (e.g. "md.tanvir"), strips unread badges and emojis,
     * and capitalizes properly, ensuring only the genuine first name is shown when collapsed.
     */
    fun smartFirstName(rawName: String): String {
        var text = rawName.replace('\n', ' ')
            .replace(Regex("""\s*\(\d+.*?\)$"""), "") // Remove "(2)"
            .replace(Regex("""\s*\[\d+.*?\]$"""), "") // Remove "[3]"
            .trim()

        if (text.isBlank()) return "Message"

        // If the name starts with prefix without space (e.g. "md.rafiul" or "dr.john"), split the prefix
        text = text.replace(Regex("""^(?i)(md|dr|prof|engr|adv|mr|mrs|ms)\.([a-zA-Z])"""), "$1. $2")

        // Split into tokens, stripping any leading/trailing symbols/emojis from each token
        val tokens = text.split(Regex("""\s+"""))
            .map { it.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "").trim() }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return "Message"

        // Skip leading honorifics / title prefixes
        var index = 0
        while (index < tokens.size) {
            val tokenLower = tokens[index].lowercase()
            if (tokenLower in TITLE_PREFIXES) {
                index++
            } else {
                break
            }
        }

        val chosen = if (index < tokens.size) tokens[index] else tokens[0]

        // Clean capitalization: e.g. "rafiul" -> "Rafiul"
        return chosen.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private val ABBREVIATIONS = mapOf(
        "tomorrow" to "Tmrw",
        "yesterday" to "Yest",
        "minutes" to "min",
        "minute" to "min",
        "seconds" to "s",
        "second" to "s",
        "hours" to "h",
        "hour" to "h",
        "kilometers" to "km",
        "kilometer" to "km",
        "approved" to "Appr ✓",
        "completed" to "Done ✓",
        "delivered" to "Done ✓",
        "downloading" to "Downloading",
        "verification" to "Verify",
        "meeting" to "Mtg",
    )

    /**
     * Clean redundant prefixes, unread counters, and brackets from a notification title.
     */
    fun cleanTitle(title: String, appLabel: String = ""): String {
        var clean = title.trim()
        if (clean.isEmpty()) return appLabel

        // Remove square bracket tags at start (e.g. "[Slack] Rafi" -> "Rafi", "[Team] Discussion" -> "Discussion")
        clean = clean.replace(Regex("""^\[[^\]]+\]\s*"""), "")

        // Remove unread count badges at end: (3), [2 new messages]
        clean = UNREAD_COUNT_REGEX.replace(clean, "").trim()

        // Remove AppLabel prefix (e.g. "WhatsApp: Tanvir" -> "Tanvir")
        if (appLabel.isNotBlank()) {
            if (clean.startsWith("$appLabel: ", ignoreCase = true)) {
                clean = clean.substring(appLabel.length + 2).trim()
            } else if (clean.startsWith("$appLabel - ", ignoreCase = true)) {
                clean = clean.substring(appLabel.length + 3).trim()
            }
        }

        // Clean boilerplate prefixes only if there is actual remaining text after it
        for (pattern in BOILERPLATE_PREFIXES) {
            val stripped = pattern.replace(clean, "").trim()
            if (stripped.isNotEmpty() && stripped != clean) {
                clean = stripped
                break
            }
        }

        return clean.ifBlank { appLabel }
    }

    /**
     * Clean redundant title/app echoes and boilerplate instructions from a notification body.
     */
    fun cleanBody(body: String, title: String = "", appLabel: String = ""): String {
        var clean = body.trim()
        if (clean.isEmpty()) return ""

        // Strip repeating title if body starts with "Title: Message"
        if (title.isNotBlank() && clean.startsWith("$title: ", ignoreCase = true)) {
            clean = clean.substring(title.length + 2).trim()
        }
        if (appLabel.isNotBlank() && clean.startsWith("$appLabel: ", ignoreCase = true)) {
            clean = clean.substring(appLabel.length + 2).trim()
        }

        // Strip boilerplate suffixes like "Tap to open", "Tap to view details"
        for (pattern in BOILERPLATE_SUFFIXES) {
            clean = pattern.replace(clean, "").trim()
        }

        // Strip unneeded trailing unread badges
        clean = UNREAD_COUNT_REGEX.replace(clean, "").trim()

        return clean
    }

    /**
     * Formats text into a high-density, concise capsule string for the right island pill.
     * Avoids cutting in the middle of words and applies intelligent abbreviations.
     */
    fun formatRightCapsule(raw: String, maxChars: Int = 16): String {
        var text = raw.trim()
        if (text.isEmpty()) return ""

        // Apply known abbreviations
        val words = text.split(Regex("""\s+""")).toMutableList()
        for (i in words.indices) {
            val lower = words[i].lowercase()
            ABBREVIATIONS[lower]?.let { words[i] = it }
        }
        text = words.joinToString(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        if (text.length <= maxChars) return text

        // Clip at word boundary when possible
        val sub = text.take(maxChars - 1)
        val lastSpace = sub.lastIndexOf(' ')
        return if (lastSpace >= maxChars / 2) {
            "${sub.substring(0, lastSpace)}…"
        } else {
            "$sub…"
        }
    }

    /**
     * Extracts dynamic countdowns or meeting times (e.g. "starts in 10 mins" -> "In 10m", "at 3:00 PM" -> "3:00 PM").
     */
    fun extractTimeSnippet(text: String): String? {
        // 1. "Starts in 10 mins" / "in 5 minutes" / "in 15m"
        val minPattern = Regex("""(?:\b(?:starts?\s+|begins?\s+|arriving\s+)?in\s+)(\d{1,2})\s*(?:mins?|minutes?|m\b)""", RegexOption.IGNORE_CASE)
        val minMatch = minPattern.find(text)
        if (minMatch != null) {
            return "In ${minMatch.groupValues[1]}m"
        }

        // 2. "Starts now" / "Happening now"
        if (text.contains("starts now", ignoreCase = true) || text.contains("happening now", ignoreCase = true)) {
            return "Now"
        }

        // 3. Exact clock time: "3:30 PM", "12:00 pm", "at 4 PM"
        val timePattern = Regex("""(?:\bat\s+)?(\d{1,2}(?::\d{2})?\s*(?:am|pm|AM|PM))""")
        val timeMatch = timePattern.find(text)
        if (timeMatch != null) {
            return timeMatch.groupValues[1].trim()
        }

        return null
    }

    /**
     * Extract vehicle license plate from ride-hailing text (e.g. "Dhaka Metro Ga-12-3456", "DM GA 1234", "KA-01-AB-1234").
     */
    fun extractVehiclePlate(text: String): String? {
        val platePattern = Regex("""\b(?:[A-Z]{2,}\s*[–-]?\s*[A-Z0-9]{1,4}\s*[–-]?\s*\d{2,4}|[A-Za-z]+(?:\s+Metro)?\s+[A-Za-z]{1,2}[–-]?\d{2}[–-]?\d{4})\b""", RegexOption.IGNORE_CASE)
        val match = platePattern.find(text)
        return match?.value?.trim()
    }
}
