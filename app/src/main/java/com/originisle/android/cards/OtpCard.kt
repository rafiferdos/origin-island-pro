package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.CopyCodeReceiver
import com.originisle.android.service.IconCache
import com.originisle.android.util.PriorityArbitrator

/**
 * Login / verification code card: the code big on the card and in the pill, and tapping either
 * (or the "Copy" capsule) copies it — see [CopyCodeReceiver], which also takes the card down.
 *
 * Shown even in "Live only" mode, where the plain message carrying it would otherwise be filtered
 * out; it's the one kind of message you're always waiting on.
 */
object OtpCard {

    /** Codes go stale fast; don't leave one sitting on the island. */
    private const val HOLD_MS = 120_000L

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    fun post(context: Context, sbn: StatusBarNotification, code: String, appLabel: String) {
        val app = context.applicationContext
        val id = IconCache.castIdFor(sbn)
        val appIcon = runCatching {
            Icon.createWithResource(sbn.packageName, app.packageManager.getApplicationInfo(sbn.packageName, 0).icon)
        }.getOrNull()

        val copy = PendingIntent.getBroadcast(
            app, id,
            Intent(app, CopyCodeReceiver::class.java)
                .putExtra(CopyCodeReceiver.EXTRA_CODE, code)
                .putExtra("cast_id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val copyAction = Notification.Action.Builder(
            Icon.createWithResource(app, R.drawable.ic_key), "Copy Code", copy,
        ).build()

        val otpColor = "#FF007AFF" // Electric Sapphire Blue

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_OTP)
            putExtra("oi_scene", "NAVIGATION")
            // Use Template 8 (Buttons) so the expanded card shows the prominent [ Copy Code ] button
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("title", spaced(code))
            putExtra("text", "Login code · $appLabel")
            putExtra("subtext", "Verification Code")
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)

            // Compact pill
            putExtra("oi_left_content", "Code")
            val keyIcon = Icon.createWithResource(app, R.drawable.ic_key)
            val pillIcon = appIcon ?: keyIcon
            putExtra("oi_left_icon", pillIcon)
            putExtra("oi_large_icon", pillIcon)

            putExtra("oi_right_content", code)
            putExtra("status_chip_text", code)
            putExtra("oi_bg_color", otpColor)
            putExtra("oi_fg_color", "#FFFFFFFF")

            // Ambient Edge Glow around expanded card
            putExtra("oi_light_color", otpColor)
            putExtra("oi_light_mode", 1)

            putExtra("icon_res", R.drawable.ic_key)
            putParcelableArrayListExtra("actions", arrayListOf(copyAction))
            putStringArrayListExtra("oi_button_titles", arrayListOf("Copy Code"))
            putIntegerArrayListExtra("oi_button_bg_colors", arrayListOf(0xFF007AFF.toInt()))
            putIntegerArrayListExtra("oi_button_text_colors", arrayListOf(0xFFFFFFFF.toInt()))

            // The whole card copies: that's the one thing anyone tapping a code wants.
            putExtra("source_content_intent", copy)
            putExtra("oi_force_show", true)
        }
        app.startService(intent)

        handler.postDelayed({ cancel(app, id) }, HOLD_MS)
    }

    fun cancel(context: Context, id: Int) {
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    /** "482913" -> "482 913", easier to read off the card; 4–5 digit codes stay as they are. */
    internal fun spaced(code: String): String =
        if (code.length >= 6) code.chunked((code.length + 1) / 2).joinToString(" ") else code
}
