package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.service.notification.StatusBarNotification
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.originisle.android.R
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import com.originisle.android.service.MediaControlReceiver
import com.originisle.android.util.PriorityArbitrator
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Dedicated Next-Level Music Player Card for Origin Island Pro, inspired by origin-ghost-player.
 * Features real-time MediaController callbacks, clean song titles, album subtitles,
 * live dynamic soundwave with harmonic palette extraction, ambient edge lighting,
 * and high-contrast luxury transport buttons (Prev, Play/Pause, Next).
 */
object MediaCard {

    private data class PostedState(
        val track: String,
        val artist: String,
        val playing: Boolean,
        val hasArt: Boolean,
        val chipBucket: Long,
    )

    private val lastPosted = ConcurrentHashMap<Int, PostedState>()
    private val activeCallbacks = ConcurrentHashMap<Int, Pair<MediaController, MediaController.Callback>>()

    /** Drop dedupe state and unregister callback for [id]. */
    fun forget(id: Int) {
        lastPosted.remove(id)
        activeCallbacks.remove(id)?.let { (ctrl, cb) ->
            runCatching { ctrl.unregisterCallback(cb) }
        }
    }

    fun post(context: Context, sbn: StatusBarNotification, controller: MediaController) {
        val id = IconCache.castIdFor(sbn)
        val n = sbn.notification
        IconCache.activeControllers[id] = controller

        // Register real-time callback so state/metadata updates trigger instant island refresh (origin-ghost-player standard)
        val existing = activeCallbacks[id]
        if (existing == null || existing.first.sessionToken != controller.sessionToken) {
            existing?.let { runCatching { it.first.unregisterCallback(it.second) } }
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    post(context, sbn, controller)
                }
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    post(context, sbn, controller)
                }
                override fun onSessionDestroyed() {
                    forget(id)
                }
            }
            runCatching { controller.registerCallback(callback) }
            activeCallbacks[id] = Pair(controller, callback)
        }

        val md = controller.metadata
        val rawTrack = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
            .ifBlank { extrasTitle(sbn) }
            .ifBlank { appLabel(context, sbn.packageName) }
        val track = cleanTrackTitle(rawTrack)

        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
            .ifBlank { "Now playing" }
        val album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
        val sourceApp = appLabel(context, sbn.packageName)
        val subtext = if (album.isNotBlank() && album != track) "$album • $sourceApp" else sourceApp

        val playbackStateValue = controller.playbackState?.state
        val playing = when (playbackStateValue) {
            PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_CONNECTING,
            -> true
            null -> sbn.isOngoing
            else -> false
        }

        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it }
        val rawLargeIcon = n.extras.get(NotificationCompat.EXTRA_LARGE_ICON)
        val albumArt: Bitmap? = (md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: (rawLargeIcon as? Bitmap)
            ?: (rawLargeIcon as? Icon)?.let { runCatching { it.loadDrawable(context)?.toBitmap() }.getOrNull() }
        )?.let(::shrinkForIcon)
        albumArt?.let { IconCache.activeLargeBitmaps[id] = it }
        val waveColors = waveColorsFrom(albumArt)

        val showChrono = n.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER, false)
        val chip = if (showChrono && n.`when` > 0) {
            formatElapsed(abs(System.currentTimeMillis() - n.`when`) / 1000)
        } else if (playing) {
            "Playing"
        } else {
            " "
        }

        // Dedupe gate: skip if unchanged and already active
        val chipBucket =
            if (showChrono && n.`when` > 0) abs(System.currentTimeMillis() - n.`when`) / 10_000L else 0L
        val state = PostedState(track, artist, playing, albumArt != null, chipBucket)
        if (id in PlaygroundService.activeCardIds && lastPosted[id] == state) return
        lastPosted[id] = state

        fun mediaAction(action: String) = PendingIntent.getBroadcast(
            context, (id.toString() + action).hashCode(),
            Intent(action).setPackage(context.packageName)
                .putExtra("cast_id", id)
                .putExtra(MediaControlReceiver.EXTRA_TOKEN, controller.sessionToken),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun findSourceAction(titleKeywords: List<String>): PendingIntent? =
            n.actions?.firstOrNull { a ->
                val title = a.title?.toString()?.lowercase().orEmpty()
                titleKeywords.any { title.contains(it) }
            }?.actionIntent

        val prevSourceAction = findSourceAction(listOf("previous", "skip back", "rewind"))
        val nextSourceAction = findSourceAction(listOf("next", "skip forward", "fast forward"))
        val sessionActions = controller.playbackState?.actions ?: 0L
        val hasPrev = prevSourceAction != null || (sessionActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
        val hasNext = nextSourceAction != null || (sessionActions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L

        val actions = arrayListOf<Notification.Action>()
        val btnTitles = arrayListOf<String>()
        val btnBgColors = arrayListOf<Int>()
        val btnTxtColors = arrayListOf<Int>()

        if (hasPrev) {
            actions.add(makeAction(context, R.drawable.ic_media_prev, "Prev", prevSourceAction ?: mediaAction(MediaControlReceiver.ACTION_PREV)))
            btnTitles.add("Prev")
            btnBgColors.add(0xFF24242C.toInt())
            btnTxtColors.add(0xFFFFFFFF.toInt())
        }

        val playPauseTitle = if (playing) "Pause" else "Play"
        actions.add(
            makeAction(
                context,
                if (playing) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                playPauseTitle,
                mediaAction(MediaControlReceiver.ACTION_PLAY_PAUSE),
            ),
        )
        btnTitles.add(playPauseTitle)
        btnBgColors.add(0xFF2D6BF5.toInt()) // Electric Sapphire highlight for active control
        btnTxtColors.add(0xFFFFFFFF.toInt())

        if (hasNext) {
            actions.add(makeAction(context, R.drawable.ic_media_next, "Next", nextSourceAction ?: mediaAction(MediaControlReceiver.ACTION_NEXT)))
            btnTitles.add("Next")
            btnBgColors.add(0xFF24242C.toInt())
            btnTxtColors.add(0xFFFFFFFF.toInt())
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_MEDIA)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", track)
            putExtra("text", artist)
            putExtra("subtext", subtext)
            putExtra("source_pkg", sbn.packageName)

            // Short/compact pill: album art + clean track title
            val shortTrack = shortTrackTitle(context, track)
            putExtra("oi_left_content", shortTrack)
            albumArt?.let {
                putExtra("oi_left_icon", Icon.createWithBitmap(it))
                putExtra("oi_large_icon", Icon.createWithBitmap(it))
            }

            putExtra("status_chip_text", chip)
            putExtra("icon_res", R.drawable.ic_media_play)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_bg_color", "#FF16161A")
            putExtra("oi_fg_color", "#FFFFFFFF")

            // Ambient glow around expanded card matching album art
            val dominantColor = waveColors.firstOrNull()?.let {
                OriginIslandBuilder.parseColor(it, 0)
            } ?: 0xFF2D6BF5.toInt()
            putExtra("oi_light_color", String.format("#%08X", dominantColor))
            putExtra("oi_light_mode", 1)

            if (btnBgColors.isNotEmpty()) {
                putStringArrayListExtra("oi_button_titles", btnTitles)
                putIntegerArrayListExtra("oi_button_bg_colors", btnBgColors)
                putIntegerArrayListExtra("oi_button_text_colors", btnTxtColors)
            }

            if (playing) {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("oi_wave_state", 1)
                if (waveColors.isNotEmpty()) {
                    putStringArrayListExtra("oi_wave_color_list", ArrayList(waveColors))
                }
            } else {
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_right_icon", Icon.createWithResource(context, R.drawable.ic_media_pause))
                putExtra("oi_show_right_icon", true)
                putExtra("oi_right_content", "Paused")
            }

            putParcelableArrayListExtra("actions", actions)
            putExtra(
                "source_content_intent",
                n.contentIntent ?: fallbackClickIntent(context, sbn.packageName),
            )
        }
        context.startService(intent)
    }

    fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\s*[\(\[](?:official\s*(?:video|audio|music\s*video|lyric\s*video)?|lyrics?|audio|visualizer|remastered(?:\s*\d+)?|live|explicit|hd|4k)[\)\]]"""), "")
            .replace(Regex("""(?i)\s*-\s*official\s*(?:music\s*)?video"""), "")
            .replace(Regex("""(?i)\s*\(feat\..*?\)"""), "")
            .replace(Regex("""(?i)\s*\[feat\..*?\]"""), "")
            .trim()
    }

    private fun shortTrackTitle(context: Context, track: String): String {
        val clean = track.replace('\n', ' ').trim()
        val density = context.resources.displayMetrics.density
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val room = 84f * density
        return TextUtils.ellipsize(clean, paint, room, TextUtils.TruncateAt.END).toString()
    }

    private fun extrasTitle(sbn: StatusBarNotification): String =
        sbn.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim().orEmpty()

    private fun appLabel(context: Context, pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun makeAction(context: Context, iconRes: Int, title: String, pi: PendingIntent): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(context, iconRes), title, pi).build()

    private fun waveColorsFrom(bitmap: Bitmap?): List<String> {
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return emptyList()
        if (bitmap.config == Bitmap.Config.HARDWARE) return emptyList()
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            listOf(0.25f to 0.5f, 0.5f to 0.3f, 0.75f to 0.5f).map { (fx, fy) ->
                val x = (fx * (w - 1)).toInt().coerceIn(0, w - 1)
                val y = (fy * (h - 1)).toInt().coerceIn(0, h - 1)
                String.format("#%06X", 0xFFFFFF and bitmap.getPixel(x, y))
            }
        }.getOrDefault(emptyList())
    }
}
