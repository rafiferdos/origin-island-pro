package com.originisle.android.ui.samples

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.cards.AuthPromptCard
import com.originisle.android.cards.DeliveryCard
import com.originisle.android.cards.DownloadCard
import com.originisle.android.cards.MeetingCard
import com.originisle.android.cards.PaymentCard
import com.originisle.android.cards.RideCard
import com.originisle.android.cards.SportsCard
import com.originisle.android.cards.WeatherCard
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.ui.PREFS_NAME

/**
 * The in-app tester catalog: one ready-made card per supported OriginIsland type, plus the new
 * live football score. Each entry fires an `oi_*` intent at [PlaygroundService] exactly the way a
 * real cast would, so it exercises the same path end-to-end.
 */
object IslandSamples {

    val all: List<OriginSample> = listOf(
        OriginSample(
            "Football score", "Scoreboard · ARS 1 – 1 MAN · 65' (generic badges)",
            IslandPreview(R.drawable.ic_soccer, "", IslandPreview.Right.Score(1, 1)),
        ) { ctx ->
            // Sample uses a generic ball on both sides instead of fetching real crests.
            val ball = Icon.createWithResource(ctx, R.drawable.ic_soccer)
            SportsCard.post(
                ctx, home = "ARS", homeScore = 1, away = "MAN", awayScore = 1, clock = "65'",
                homeLogo = ball, awayLogo = ball,
            )
        },
        OriginSample(
            "App download", "Play Store download · 75% · 19.2 Mbps",
            IslandPreview(R.drawable.ic_download, "75%", IslandPreview.Right.Capsule("19.2 Mbps")),
        ) { ctx ->
            val playStoreIcon = try {
                val appInfo = ctx.packageManager.getApplicationInfo("com.android.vending", 0)
                Icon.createWithResource("com.android.vending", appInfo.icon)
            } catch (e: Exception) {
                Icon.createWithResource(ctx, R.drawable.ic_download)
            }
            val style = ctx.getSharedPreferences(PREFS_NAME, 0)
                .getString("download_pill_style", "split") ?: "split"
            val config = DownloadCard.resolvePillConfig(
                style = style,
                displayTitle = "Google Chrome",
                percent = 75,
                indeterminate = false,
                progressMax = 100,
                liveMbpsSpeed = "19.2 Mbps",
            )
            start(ctx, 40001) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 75)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", config.rightTemplate)
                putExtra("oi_right_content", config.rightContent)
                putExtra("status_chip_text", config.statusChip)
                putExtra("title", "Google Chrome")
                putExtra("text", "75% • 18 MB / 24 MB • 19.2 Mbps")
                putExtra("subtext", "Google Play Store")
                putExtra("source_app", "Google Play Store")
                putExtra("oi_left_content", config.leftContent)
                if (config.leftDoubleLine != null) {
                    putStringArrayListExtra("oi_left_doubleline", config.leftDoubleLine)
                }
                putExtra("oi_left_icon", playStoreIcon)
                putExtra("oi_large_icon", playStoreIcon)
                if (config.showProgress) {
                    putExtra("show_progress", true)
                    putExtra("progress", 75)
                    putExtra("progress_max", 100)
                }
                putExtra("icon_res", R.drawable.ic_download)
            }
        },
        OriginSample(
            "Game download", "Genshin Impact · 24.5 Mbps · 45% • 4.5 GB / 12.0 GB",
            IslandPreview(
                R.drawable.ic_download,
                "Genshin",
                IslandPreview.Right.Progress(0.45f),
                leftDoubleLine = listOf("Genshin", "24.5 Mbps"),
            ),
        ) { ctx ->
            val style = ctx.getSharedPreferences(PREFS_NAME, 0)
                .getString("download_pill_style", "doubleline") ?: "doubleline"
            val config = DownloadCard.resolvePillConfig(
                style = style,
                displayTitle = "Genshin Impact",
                percent = 45,
                indeterminate = false,
                progressMax = 100,
                liveMbpsSpeed = "24.5 Mbps",
            )
            start(ctx, 40022) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 76)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", config.rightTemplate)
                putExtra("oi_right_content", config.rightContent)
                putExtra("status_chip_text", config.statusChip)
                putExtra("title", "Genshin Impact")
                putExtra("text", "45% • 4.5 GB / 12.0 GB • 24.5 Mbps")
                putExtra("subtext", "Genshin Impact • Game Asset Download")
                putExtra("source_app", "Genshin Impact")
                putExtra("oi_left_content", config.leftContent)
                if (config.leftDoubleLine != null) {
                    putStringArrayListExtra("oi_left_doubleline", config.leftDoubleLine)
                }
                putExtra("oi_left_icon", Icon.createWithResource(ctx, R.drawable.ic_download))
                putExtra("oi_large_icon", Icon.createWithResource(ctx, R.drawable.ic_download))
                if (config.showProgress) {
                    putExtra("show_progress", true)
                    putExtra("progress", 45)
                    putExtra("progress_max", 100)
                }
                putExtra("icon_res", R.drawable.ic_download)
                putExtra("click_resp", openApp(ctx))
            }
        },
        OriginSample(
            "bKash payment", "bKash MFS · Received ৳1,500 from Tanvir ✓",
            IslandPreview(R.drawable.ic_payment, "৳1,500", IslandPreview.Right.Success),
        ) { ctx ->
            PaymentCard.post(
                context = ctx, id = 40002, appIcon = null,
                amount = "৳1,500", merchant = "Tanvir Ahmed", appLabel = "bKash",
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "Food delivery", "Foodpanda / Pathao · Rider arriving · 15 min",
            IslandPreview(R.drawable.ic_delivery, "Foodpanda", IslandPreview.Right.Capsule("15 min")),
        ) { ctx ->
            DeliveryCard.post(
                context = ctx, id = 40003,
                title = "Rider is on the way",
                eta = "15 min",
                carrier = "Foodpanda",
                appIcon = null,
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "Ride hailing", "Uber / Pathao · Driver Tanvir (Dhaka Metro Ga-12-3456) · 3 min",
            IslandPreview(R.drawable.ic_car, "Pathao", IslandPreview.Right.Capsule("3 min")),
        ) { ctx ->
            RideCard.post(
                context = ctx, id = 40004,
                title = "Driver Tanvir",
                details = "Arriving in Toyota Aqua · Dhaka Metro Ga-12-3456 in 3 min",
                appLabel = "Pathao",
                appIcon = null,
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "Meeting & Event", "Google Meet · Sprint Review · In 10m",
            IslandPreview(R.drawable.ic_calendar, "Meet", IslandPreview.Right.Capsule("In 10m")),
        ) { ctx ->
            MeetingCard.post(
                context = ctx, id = 40015,
                title = "Sprint Review with Team",
                timeSnippet = "In 10m",
                details = "3:00 PM – 4:00 PM · meet.google.com/abc-defg-hij",
                appLabel = "Google Calendar",
                appIcon = null,
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "2FA Sign-in", "Google · Approve sign-in from Windows PC? · Approve / Deny",
            IslandPreview(R.drawable.ic_shield, "Sign-in", IslandPreview.Right.Capsule("Approve?")),
        ) { ctx ->
            start(ctx, 40016) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 72)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Is that you signing in?")
                putExtra("text", "Chrome on Windows • Dhaka, Bangladesh")
                putExtra("subtext", "Google Play Services")
                putExtra("oi_left_content", "Sign-in")
                putExtra("oi_right_content", "Approve?")
                putExtra("status_chip_text", "Approve?")
                putExtra("icon_res", R.drawable.ic_shield)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Deny", "Approve"))
                putIntegerArrayListExtra("oi_button_bg_colors", arrayListOf(0xFFFF3B30.toInt(), 0xFF34C759.toInt()))
                putIntegerArrayListExtra("oi_button_text_colors", arrayListOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()))
                putExtra("click_resp", openApp(ctx))
            }
        },
        OriginSample(
            "Flight boarding", "FLIGHT · symmetric route card",
            IslandPreview(R.drawable.ic_flight, "CDG → FCO", IslandPreview.Right.Capsule("Gate K21")),
        ) { ctx ->
            start(ctx, 40005) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "AF1980")
                putExtra("text", "Boarding")
                putExtra("oi_extra1", "CDG"); putExtra("oi_extra2", "12:40")
                putExtra("oi_extra3", "FCO"); putExtra("oi_extra4", "14:55")
                putExtra("oi_left_content", "CDG → FCO")
                putExtra("status_chip_text", "Gate K21")
                putExtra("icon_res", R.drawable.ic_flight)
            }
        },
        OriginSample(
            "Countdown timer", "TIMER · progress 40%",
            IslandPreview(R.drawable.ic_timer, "Timer", IslandPreview.Right.Progress(0.4f)),
        ) { ctx ->
            start(ctx, 40006) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
                putExtra("title", "Timer")
                putExtra("text", "6:00 remaining")
                putExtra("oi_left_content", "Timer")
                putExtra("show_progress", true)
                putExtra("progress", 40)
                putExtra("progress_max", 100)
                putExtra("status_chip_text", "6:00")
                putExtra("icon_res", R.drawable.ic_timer)
            }
        },
        OriginSample(
            "Loading", "Loading-dots right island",
            IslandPreview(R.drawable.ic_alert, "Working", IslandPreview.Right.Loading),
        ) { ctx ->
            start(ctx, 40007) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING)
                putExtra("title", "Working…")
                putExtra("text", "Please wait")
                putExtra("oi_left_content", "Working")
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Music player", "Wave island · luxury controls, ambient glow & soundwave",
            IslandPreview(R.drawable.ic_media_play, "Midnight City", IslandPreview.Right.Wave),
        ) { ctx ->
            start(ctx, 40008) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("title", "Midnight City")
                putExtra("text", "M83")
                putExtra("subtext", "Hurry Up, We're Dreaming • Spotify")
                putExtra("oi_left_content", "Midnight City")
                putExtra("oi_wave_state", 1)
                putStringArrayListExtra("oi_wave_color_list", arrayListOf("#FF3A82F6", "#FF8B5CF6", "#FFEC4899"))
                putExtra("oi_light_color", "#FF8B5CF6")
                putExtra("oi_light_mode", 1)
                putExtra("status_chip_text", "Playing")
                putExtra("icon_res", R.drawable.ic_media_play)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Prev", "Pause", "Next"))
                putIntegerArrayListExtra("oi_button_bg_colors", arrayListOf(0xFF24242C.toInt(), 0xFF2D6BF5.toInt(), 0xFF24242C.toInt()))
                putIntegerArrayListExtra("oi_button_text_colors", arrayListOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()))
                putExtra("click_resp", openApp(ctx))
            }
        },
        OriginSample(
            "Gmail email", "Gmail · sender + subject on pill · Archive/Reply/Delete",
            IslandPreview(R.drawable.ic_mail, "GitHub", IslandPreview.Right.Text("Security alert")),
        ) { ctx ->
            start(ctx, 40020) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 70)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("title", "GitHub Security")
                putExtra("text", "New sign-in from Chrome on Windows\nWe noticed a new login to your account from Dhaka, Bangladesh.")
                putExtra("subtext", "Gmail • user@gmail.com")
                putExtra("source_app", "Gmail")
                putExtra("oi_left_content", "GitHub")
                putExtra("oi_right_content", "Security alert: New login")
                putExtra("status_chip_text", "Security alert: New login")
                putExtra("icon_res", R.drawable.ic_mail)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Archive", "Reply", "Delete"))
                putIntegerArrayListExtra(
                    "oi_button_bg_colors",
                    arrayListOf(0xFF2C344E.toInt(), 0xFF1E3A2F.toInt(), 0xFF3A1E22.toInt()),
                )
                putIntegerArrayListExtra(
                    "oi_button_text_colors",
                    arrayListOf(0xFFE0E6FF.toInt(), 0xFF34C759.toInt(), 0xFFFF453A.toInt()),
                )
                putExtra("click_resp", openApp(ctx))
            }
        },
        OriginSample(
            "Discord chat", "Discord · #general / @mention · Reply/Mark Read",
            IslandPreview(R.drawable.ic_discord, "#general", IslandPreview.Right.Text("Check out the new release! 🚀")),
        ) { ctx ->
            start(ctx, 40021) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 71)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("title", "Alex • #general")
                putExtra("text", "@everyone Check out the new Origin Island Pro v2.9.0 release! 🚀")
                putExtra("subtext", "Discord • OriginOS Community")
                putExtra("source_app", "Discord")
                putExtra("oi_left_content", "#general")
                putExtra("oi_right_content", "🔔 Check out the new release!")
                putExtra("status_chip_text", "🔔 Check out the new release!")
                putExtra("icon_res", R.drawable.ic_discord)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Reply", "Mark Read", "Mute"))
                putIntegerArrayListExtra(
                    "oi_button_bg_colors",
                    arrayListOf(0xFF5865F2.toInt(), 0xFF1E2D52.toInt(), 0xFF24242C.toInt()),
                )
                putIntegerArrayListExtra(
                    "oi_button_text_colors",
                    arrayListOf(0xFFFFFFFF.toInt(), 0xFF4D88FF.toInt(), 0xFFE0E0E0.toInt()),
                )
                putExtra("click_resp", openApp(ctx))
            }
        },
        OriginSample(
            "Message", "Chat · app icon + sender left, message right",
            IslandPreview(R.drawable.ic_chat, "Tanvir", IslandPreview.Right.Text("Kire koi tui? Free achos? 💬")),
        ) { ctx ->
            // The same extras MessageCard sends for a real chat notification:
            // Short pill has app icon on left + sender name, expanded card has contact avatar + text.
            start(ctx, 40013) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("title", "Tanvir Ahmed")
                putExtra("text", "Kire koi tui? Free achos? 💬")
                putExtra("subtext", "WhatsApp")
                putExtra("source_app", "WhatsApp")
                putExtra("oi_left_content", "Tanvir")
                putExtra("oi_left_icon", Icon.createWithResource(ctx, R.drawable.ic_chat))
                putExtra("oi_large_icon", Icon.createWithResource(ctx, R.drawable.ic_avatar))
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", false)
                putExtra("oi_right_content", "Kire koi tui? Free achos? 💬")
                putExtra("status_chip_text", "Kire koi tui? Free achos? 💬")
                putExtra("icon_res", R.drawable.ic_chat)
            }
        },
        OriginSample(
            "Incoming call", "VOIPCALL · Red Decline & Green Answer",
            IslandPreview(R.drawable.ic_call, "John", IslandPreview.Right.Wave),
        ) { ctx ->
            start(ctx, 40009) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_priority", 100)
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE)
                putExtra("oi_wave_state", 1)
                putExtra("title", "John Doe")
                putExtra("text", "WhatsApp Audio Call")
                putExtra("subtext", "WhatsApp")
                putExtra("oi_left_content", "John")
                putExtra("oi_right_content", "Incoming")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Decline", "Answer"))
                putIntegerArrayListExtra("oi_button_bg_colors", arrayListOf(0xFFFF3B30.toInt(), 0xFF34C759.toInt()))
                putIntegerArrayListExtra("oi_button_text_colors", arrayListOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_call)
            }
        },
        OriginSample(
            "Driving navigation", "Driving-navi template · maneuver + street + ETA",
            IslandPreview(R.drawable.ic_navigation, "350 m", IslandPreview.Right.Capsule("12 min")),
        ) { ctx ->
            // The template the real NavigationCard posts. Unproven on hardware: if nothing appears,
            // OriginOS rejected template 9 and the Cast tab's navigation toggle should stay off.
            start(ctx, 40011) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_DRIVING_NAVI)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "350 m")
                putExtra("text", "Rue de Rivoli")
                putExtra("oi_nav_msg", "12 min · 4,5 km · 14:32")
                putExtra("oi_nav_assist", "Keep left")
                putExtra("oi_left_content", "350 m")
                putExtra("status_chip_text", "12 min")
                putExtra("icon_res", R.drawable.ic_navigation)
            }
        },
        OriginSample(
            "Navigation message", "Navigation template · nav icon + single message line",
            IslandPreview(R.drawable.ic_navigation, "Turn right", IslandPreview.Right.Capsule("2 min")),
        ) { ctx ->
            start(ctx, 40012) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_NAVIGATION)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
                putExtra("title", "Turn right")
                putExtra("text", "onto Quai des Tuileries")
                putExtra("oi_nav_msg", "Turn right onto Quai des Tuileries · 2 min")
                putExtra("oi_left_content", "Turn right")
                putExtra("status_chip_text", "2 min")
                putExtra("icon_res", R.drawable.ic_navigation)
            }
        },
        OriginSample(
            "Two buttons", "Buttons template · Deny / Receive",
            IslandPreview(R.drawable.ic_alert, "Share", IslandPreview.Right.None),
        ) { ctx ->
            start(ctx, 40010) {
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
                putExtra("title", "Nearby share")
                putExtra("text", "Pixel 9 wants to share a file")
                putExtra("oi_left_content", "Share")
                putStringArrayListExtra("oi_button_titles", arrayListOf("Deny", "Receive"))
                putExtra("click_resp", openApp(ctx))
                putExtra("icon_res", R.drawable.ic_alert)
            }
        },
        OriginSample(
            "Delivery", "Amazon · Out for delivery · by 4 PM",
            IslandPreview(R.drawable.ic_alert, "Amazon", IslandPreview.Right.Capsule("by 4 PM")),
        ) { ctx ->
            DeliveryCard.post(
                context = ctx, id = 40011,
                title = "Out for delivery",
                eta = "by 4 PM",
                carrier = "Amazon Logistics",
                appIcon = null,
                clickResp = openApp(ctx),
            )
        },
        OriginSample(
            "Weather alert", "Severe storm warning · ⛈ Storm",
            IslandPreview(R.drawable.ic_alert, "Weather", IslandPreview.Right.Capsule("⛈ Storm")),
        ) { ctx ->
            WeatherCard.post(
                context = ctx, id = 40012,
                headline = "Severe Storm Warning",
                chip = "⛈ Storm",
                clickResp = openApp(ctx),
            )
        },
    )

    private fun start(context: Context, id: Int, configure: Intent.() -> Unit) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            // vivo rejects a SuperX post with no content intent, so give every sample one.
            // A sample's own configure() may overwrite this with a more specific intent.
            putExtra("click_resp", openApp(context))
            // Force the island to actually take over the pill (football sets this; without it the
            // card is accepted but not surfaced, so the sample looks like it "does nothing").
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            configure()
        }
        context.startService(intent)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
