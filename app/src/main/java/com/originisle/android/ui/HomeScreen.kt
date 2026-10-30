package com.originisle.android.ui

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon as AndroidIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.originisle.android.cards.RaidenCard
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.cards.ChargingCard
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.service.QuietHours
import com.originisle.android.ui.samples.IslandPreview
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The main screen: listener status, the sample-card tester, casting settings, and links to the Apps
 * and Activity screens. Setup/permissions and the update check live in the ⋯ menu.
 */
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", false)) }
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", false)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var messagesOn by remember { mutableStateOf(prefs.getBoolean("cast_messages", true)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 0)) }
    var otpOn by remember { mutableStateOf(prefs.getBoolean("cast_otp", true)) }
    var authOn by remember { mutableStateOf(prefs.getBoolean("cast_auth", true)) }
    var meetingsOn by remember { mutableStateOf(prefs.getBoolean("cast_meetings", true)) }
    var ridesOn by remember { mutableStateOf(prefs.getBoolean("cast_rides", true)) }
    var timerOn by remember { mutableStateOf(prefs.getBoolean("cast_timer", true)) }
    var sportsOn by remember { mutableStateOf(prefs.getBoolean("cast_sports", true)) }
    var paymentsOn by remember { mutableStateOf(prefs.getBoolean("cast_payments", true)) }
    var deliveryOn by remember { mutableStateOf(prefs.getBoolean("cast_delivery", true)) }
    var weatherOn by remember { mutableStateOf(prefs.getBoolean("cast_weather", true)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }
    var raidenOn by remember { mutableStateOf(prefs.getBoolean("cast_raiden", true)) }
    var raidenLiveOn by remember { mutableStateOf(prefs.getBoolean("cast_raiden_live", true)) }
    var raidenMessagesOn by remember { mutableStateOf(prefs.getBoolean("cast_raiden_messages", true)) }
    var raidenDoneOn by remember { mutableStateOf(prefs.getBoolean("cast_raiden_done", true)) }
    var raidenAlwaysOnTop by remember { mutableStateOf(prefs.getBoolean("raiden_always_on_top", false)) }
    var raidenMusePkg by remember { mutableStateOf(prefs.getString("raiden_muse_pkg", "") ?: "") }
    var privacyOn by remember { mutableStateOf(prefs.getBoolean("cast_privacy_mode", false)) }
    var chargingOn by remember { mutableStateOf(prefs.getBoolean("cast_charging", true)) }
    var chargingKeep by remember { mutableStateOf(prefs.getBoolean("cast_charging_keep", false)) }
    var messagePreviewMode by remember { mutableStateOf(prefs.getString("cast_message_preview_mode", "smart") ?: "smart") }
    var landingAnimOn by remember { mutableStateOf(prefs.getBoolean("cast_landing_animation", true)) }
    var downloadPillStyle by remember { mutableStateOf(prefs.getString("download_pill_style", "speed_ring") ?: "speed_ring") }
    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }
    val appsOff = remember { prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty().size }
    val tick = rememberResumeTick()
    // Setup only needs attention again if something actually broke (OriginOS revoking notification
    // access or re-restricting battery is a known failure mode) — then Home shows a one-line banner;
    // otherwise the permissions stay behind the ⋯ menu so they're not in the way on every open.
    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var activeUpdateInfo by remember { mutableStateOf<UpdateChecker.AppUpdateInfo?>(null) }
    var startDownloadOnOpen by remember { mutableStateOf(false) }
    var autoUpdateCheck by remember { mutableStateOf(prefs.getBoolean("auto_check_updates_daily", true)) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        val actIntent = activity?.intent
        if (actIntent?.getBooleanExtra(UpdateChecker.EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            val startDl = actIntent.getBooleanExtra(UpdateChecker.EXTRA_START_DOWNLOAD, false)
            val cachedVersion = prefs.getString("cached_update_version", null)
            if (cachedVersion != null) {
                activeUpdateInfo = UpdateChecker.AppUpdateInfo(
                    latestVersion = cachedVersion,
                    releaseTitle = prefs.getString("cached_update_title", "Origin Island Pro v$cachedVersion") ?: "",
                    releaseNotes = prefs.getString("cached_update_notes", "") ?: "",
                    apkUrl = prefs.getString("cached_update_apk_url", "") ?: "",
                    apkSize = prefs.getLong("cached_update_apk_size", 0L),
                    apkName = prefs.getString("cached_update_apk_name", "origin-island-pro-$cachedVersion.apk") ?: "",
                    releaseUrl = prefs.getString("cached_update_release_url", "") ?: "",
                )
                startDownloadOnOpen = startDl
            } else {
                coroutineScope.launch {
                    when (val res = UpdateChecker.check(context)) {
                        is UpdateChecker.Result.UpdateAvailable -> {
                            activeUpdateInfo = res.info
                            startDownloadOnOpen = startDl
                        }
                        else -> Unit
                    }
                }
            }
            actIntent.removeExtra(UpdateChecker.EXTRA_OPEN_UPDATE_DIALOG)
            actIntent.removeExtra(UpdateChecker.EXTRA_START_DOWNLOAD)
        }
    }

    // Poll the listener once a second so the status pill stays live.
    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    var todayCasts by remember { mutableIntStateOf(0) }
    var todaySkips by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val dayStart = run {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        while (true) {
            connected = NotificationCastListener.instance != null
            val todayEntries = CastLog.entries.filter { it.time >= dayStart }
            todayCasts = todayEntries.count { it.cast }
            todaySkips = todayEntries.count { !it.cast }
            delay(1000)
        }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Origin Island Pro",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.7).sp,
                        lineHeight = 38.sp,
                        color = IsleColors.Text,
                    )
                    StatusPill(connected, onClick = onOpenActivity)
                }
                Box {
                    CircleIconButton(IsleIcons.More, "More", onClick = { menuOpen = true })
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        offset = DpOffset(0.dp, 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = IsleColors.Menu,
                    ) {
                        MenuItem("Check for updates") {
                            menuOpen = false
                            coroutineScope.launch {
                                updateStatus = "Checking for updates…"
                                when (val result = UpdateChecker.check(context)) {
                                    is UpdateChecker.Result.UpToDate -> {
                                        updateStatus = "You're on the latest version (v${result.current})."
                                    }
                                    is UpdateChecker.Result.UpdateAvailable -> {
                                        updateStatus = null
                                        activeUpdateInfo = result.info
                                        startDownloadOnOpen = false
                                    }
                                    is UpdateChecker.Result.Error -> {
                                        updateStatus = result.message
                                    }
                                }
                            }
                        }
                        MenuItem("Permissions") {
                            menuOpen = false
                            permissionsOpen = true
                        }
                    }
                }
            }
            updateStatus?.let {
                Text(
                    it,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    fontSize = 13.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        if (!listenerOk || !batteryOk) {
            item { SetupAttentionBanner(onClick = { permissionsOpen = true }) }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Cast today",
                    value = todayCasts.toString(),
                    color = IsleColors.GoodText,
                    bg = IsleColors.GoodBg,
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Skipped",
                    value = todaySkips.toString(),
                    color = IsleColors.TextSecondary,
                    bg = IsleColors.SurfaceHigh,
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Active cards",
                    value = PlaygroundService.activeCardIds.size.toString(),
                    color = IsleColors.AccentText,
                    bg = IsleColors.CastTagBg,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SampleTester(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        "Recast all", onClick = { recastAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Refresh,
                        enabled = castOn || mediaOn,
                    )
                    PillButton(
                        "Stop all", onClick = { stopAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Stop, iconSize = 16.dp,
                        content = IsleColors.Danger,
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Send to island")
                GroupCard {
                    ToggleRow(IsleIcons.Bell, IsleColors.TileBlue, "Notifications", castOn) {
                        castOn = it; prefs.edit().putBoolean("cast_notifications", it).apply()
                        if (it) PlaygroundService.keepAlive(context)
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Music, IsleColors.TileMedia, "Media", mediaOn,
                        subtitle = "Mimic OriginPlayer for media apps",
                    ) {
                        mediaOn = it; prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Notifications")
                GroupCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedControl(
                            listOf("Live only", "Everything"),
                            selected = if (includeMessages) 1 else 0,
                            onSelect = {
                                includeMessages = it == 1
                                prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                            },
                        )
                        Text(
                            if (includeMessages) "Every notification, plain messages included."
                            else "Only live cards — score, navigation, payment, etc.",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                    if (messagesOn || includeMessages) {
                        GroupDivider()
                        SettingRow(IsleIcons.Clock, IsleColors.TileTeal, "Hide after", subtitle = "Dismiss messages & one-off alerts")
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(0 to "Never", 5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (sec, label) ->
                                ChoiceChip(
                                    label,
                                    selected = autoDismissSec == sec,
                                    onClick = {
                                        autoDismissSec = sec
                                        prefs.edit().putInt("cast_auto_dismiss_seconds", sec).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Skip silent", ignoreSilent,
                        subtitle = "Ignore silenced notifications",
                    ) {
                        ignoreSilent = it; prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Lock, IsleColors.TileGreen, "Lock screen", lockscreenLiveCard,
                        subtitle = "Live card on lockscreen",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.TrendUp, IsleColors.TileCyan, "Island return animation", landingAnimOn,
                        subtitle = "Dock closing app window directly into the active island",
                    ) {
                        landingAnimOn = it
                        prefs.edit().putBoolean("cast_landing_animation", it).apply()
                    }
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Cards")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Chat, IsleColors.TileChat, "Messages", messagesOn,
                        subtitle = "WhatsApp, Messenger, Telegram, IMO, SMS…",
                    ) {
                        messagesOn = it; prefs.edit().putBoolean("cast_messages", it).apply()
                    }
                    if (messagesOn) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                "smart" to "Smart",
                                "always_badge" to "Expand only",
                                "full_text" to "Full text",
                            ).forEach { (mode, label) ->
                                ChoiceChip(
                                    label,
                                    selected = messagePreviewMode == mode,
                                    onClick = {
                                        messagePreviewMode = mode
                                        prefs.edit().putString("cast_message_preview_mode", mode).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Calendar, IsleColors.TileCalendar, "Meetings & Events", meetingsOn,
                        subtitle = "Countdown to events (Google Calendar, Meet, Zoom…)",
                    ) {
                        meetingsOn = it; prefs.edit().putBoolean("cast_meetings", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Car, IsleColors.TileRide, "Rides & Taxis", ridesOn,
                        subtitle = "Driver arrival & plate (Uber, Pathao Rides…)",
                    ) {
                        ridesOn = it; prefs.edit().putBoolean("cast_rides", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Clock, IsleColors.TileTimer, "Clock Timers", timerOn,
                        subtitle = "Live ticking timer countdown on island",
                    ) {
                        timerOn = it; prefs.edit().putBoolean("cast_timer", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Key, IsleColors.TileAmber, "Login codes", otpOn,
                        subtitle = "Show the code, tap to copy",
                    ) {
                        otpOn = it; prefs.edit().putBoolean("cast_otp", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Shield, IsleColors.TileAuth, "2FA & Sign-in", authOn,
                        subtitle = "Approve login requests with one tap",
                    ) {
                        authOn = it; prefs.edit().putBoolean("cast_auth", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Sparkles, IsleColors.TileRaiden, "Raiden Assistant", raidenOn,
                        subtitle = "AI companion activity & live working state (Muse app)",
                    ) {
                        raidenOn = it; prefs.edit().putBoolean("cast_raiden", it).apply()
                    }
                    if (raidenOn) {
                        Column(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChoiceChip(
                                    "Live state",
                                    selected = raidenLiveOn,
                                    onClick = {
                                        raidenLiveOn = !raidenLiveOn
                                        prefs.edit().putBoolean("cast_raiden_live", raidenLiveOn).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceChip(
                                    "Messages",
                                    selected = raidenMessagesOn,
                                    onClick = {
                                        raidenMessagesOn = !raidenMessagesOn
                                        prefs.edit().putBoolean("cast_raiden_messages", raidenMessagesOn).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceChip(
                                    "Done notice",
                                    selected = raidenDoneOn,
                                    onClick = {
                                        raidenDoneOn = !raidenDoneOn
                                        prefs.edit().putBoolean("cast_raiden_done", raidenDoneOn).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            ToggleRow(
                                IsleIcons.Bolt, IsleColors.TileAmber, "Always on top", raidenAlwaysOnTop,
                                subtitle = if (raidenAlwaysOnTop)
                                    "⚠️ Priority 100: Can displace incoming calls!"
                                else
                                    "Keep below calls, navigation, and timers (Priority 85)",
                            ) {
                                raidenAlwaysOnTop = it
                                prefs.edit().putBoolean("raiden_always_on_top", it).apply()
                            }
                            // Package Configuration for Muse / Raiden
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IsleColors.SurfaceHigh)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Muse App Package",
                                        fontFamily = JakartaSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = IsleColors.Text,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (raidenMusePkg.isNotBlank()) {
                                            Text(
                                                text = "Clear",
                                                fontFamily = JakartaSans,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = IsleColors.BadText,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(IsleColors.SurfaceHigher)
                                                    .clickable {
                                                        raidenMusePkg = ""
                                                        prefs.edit().remove("raiden_muse_pkg").apply()
                                                        RaidenCard.resetPackageCache()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                        Text(
                                            text = "Auto-detect",
                                            fontFamily = JakartaSans,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = IsleColors.AccentText,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(IsleColors.Accent.copy(alpha = 0.15f))
                                                .clickable {
                                                    val detected = RaidenCard.confirmMusePackage(context)
                                                    if (detected != null) {
                                                        raidenMusePkg = detected
                                                        prefs.edit().putString("raiden_muse_pkg", detected).apply()
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IsleColors.Surface)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (raidenMusePkg.isEmpty()) {
                                        Text(
                                            "e.g. ai.muse or com.muse.app",
                                            fontFamily = JakartaSans,
                                            fontSize = 13.sp,
                                            color = IsleColors.TextSecondary,
                                        )
                                    }
                                    BasicTextField(
                                        value = raidenMusePkg,
                                        onValueChange = { newVal ->
                                            val trimmed = newVal.trim()
                                            raidenMusePkg = trimmed
                                            if (trimmed.isEmpty()) {
                                                prefs.edit().remove("raiden_muse_pkg").apply()
                                            } else {
                                                prefs.edit().putString("raiden_muse_pkg", trimmed).apply()
                                            }
                                            RaidenCard.resetPackageCache()
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = JakartaSans,
                                            fontSize = 13.sp,
                                            color = IsleColors.Text,
                                        ),
                                        cursorBrush = SolidColor(IsleColors.Accent),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                val confirmedInfo = remember(raidenMusePkg) {
                                    RaidenCard.confirmMusePackage(context)
                                }
                                Text(
                                    text = if (raidenMusePkg.isNotBlank()) {
                                        "Exact package configured: $raidenMusePkg"
                                    } else if (confirmedInfo != null) {
                                        "Auto-discovered package: $confirmedInfo"
                                    } else {
                                        "Set exact package name of Muse app. Substring matching is disabled."
                                    },
                                    fontFamily = JakartaSans,
                                    fontSize = 11.sp,
                                    color = if (raidenMusePkg.isNotBlank() || confirmedInfo != null) IsleColors.GoodSubtle else IsleColors.TextTertiary,
                                )
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Ball, IsleColors.TileSports, "Football scores", sportsOn,
                        subtitle = "Live match cards from score apps",
                    ) {
                        sportsOn = it; prefs.edit().putBoolean("cast_sports", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Wallet, IsleColors.TilePayment, "Payments", paymentsOn,
                        subtitle = "Apple-style payment animation",
                    ) {
                        paymentsOn = it; prefs.edit().putBoolean("cast_payments", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Truck, IsleColors.TileDelivery, "Delivery tracking", deliveryOn,
                        subtitle = "Parcel & shipping notifications (Amazon, DHL, FedEx…)",
                    ) {
                        deliveryOn = it; prefs.edit().putBoolean("cast_delivery", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.CloudSun, IsleColors.TileWeather, "Weather alerts", weatherOn,
                        subtitle = "Storm warnings and forecast updates",
                    ) {
                        weatherOn = it; prefs.edit().putBoolean("cast_weather", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Buttons, IsleColors.TileDownload, "Action buttons", actionsOn,
                        subtitle = "The app's own buttons on the card",
                    ) {
                        actionsOn = it; prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Shield, IsleColors.TileAmber, "Privacy mode", privacyOn,
                        subtitle = "Hide message bodies & payment amounts on the island",
                    ) {
                        privacyOn = it; prefs.edit().putBoolean("cast_privacy_mode", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Bolt, IsleColors.TileGreen, "Charging", chargingOn,
                        subtitle = "Battery card when you plug in",
                    ) {
                        chargingOn = it; prefs.edit().putBoolean("cast_charging", it).apply()
                        ChargingCard.onSettingsChanged(context)
                    }
                    if (chargingOn) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(false to "Briefly", true to "While charging").forEach { (keep, label) ->
                                ChoiceChip(
                                    label,
                                    selected = chargingKeep == keep,
                                    onClick = {
                                        chargingKeep = keep
                                        prefs.edit().putBoolean("cast_charging_keep", keep).apply()
                                        ChargingCard.onSettingsChanged(context)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    GroupDivider()
                    SettingRow(
                        IsleIcons.Download, IsleColors.TileDownload, "Download pill style",
                        subtitle = "Choose how active downloads look on the island",
                    )
                    Column(
                        Modifier.padding(start = 62.dp, end = 16.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                "Speed & Ring",
                                selected = downloadPillStyle == "speed_ring",
                                onClick = {
                                    downloadPillStyle = "speed_ring"
                                    prefs.edit().putString("download_pill_style", "speed_ring").apply()
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "Dual-line",
                                selected = downloadPillStyle == "doubleline",
                                onClick = {
                                    downloadPillStyle = "doubleline"
                                    prefs.edit().putString("download_pill_style", "doubleline").apply()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                "Speed & %",
                                selected = downloadPillStyle == "speed_capsule",
                                onClick = {
                                    downloadPillStyle = "speed_capsule"
                                    prefs.edit().putString("download_pill_style", "speed_capsule").apply()
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "% & Speed",
                                selected = downloadPillStyle == "percent_speed" || downloadPillStyle == "split",
                                onClick = {
                                    downloadPillStyle = "percent_speed"
                                    prefs.edit().putString("download_pill_style", "percent_speed").apply()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        when (downloadPillStyle) {
                            "doubleline" -> "Dual-line: Left shows [App Icon] + Name & Speed (2 lines), right shows circular progress ring."
                            "speed_capsule" -> "Speed & % Badge: Left shows [App Icon] + live speed in Mbps, right shows % in capsule badge."
                            "percent_speed", "split" -> "% & Speed Badge: Left shows [App Icon] + % progress, right shows live speed (Mbps) badge."
                            else -> "Speed & Ring: Left shows [App Icon] + live speed in Mbps, right shows circular progress ring."
                        },
                        modifier = Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                        fontSize = 12.sp,
                        color = IsleColors.TextSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Punch-hole & Camera Cutout")
                GroupCard {
                    CameraCutoutTuner(context = context, prefs = prefs)
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Quiet hours")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Follow Do Not Disturb", quietDnd,
                        subtitle = "Quiet while DND is on",
                    ) {
                        quietDnd = it; prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Clock, IsleColors.TileGray, "Schedule", quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} every day",
                    ) {
                        quietSchedule = it; prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }
                    if (quietSchedule) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ChoiceChip(
                                "From ${QuietHours.format(quietStart)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietStart) {
                                        quietStart = it; prefs.edit().putInt("cast_quiet_start", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "To ${QuietHours.format(quietEnd)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietEnd) {
                                        quietEnd = it; prefs.edit().putInt("cast_quiet_end", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Text(
                    "While quiet, only calls, navigation, timers, login codes, media and charging show.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        item {
            GroupCard {
                NavRow(
                    IsleIcons.Grid, IsleColors.TileAmber, "Apps",
                    value = if (appsOff == 0) "All on" else "$appsOff off",
                    onClick = onOpenApps,
                )
                GroupDivider()
                NavRow(IsleIcons.Pulse, IsleColors.TileGray, "Activity", onClick = onOpenActivity)
            }
        }

        item {
            SectionLabel("Updates & Maintenance")
            GroupCard {
                ToggleRow(
                    IsleIcons.Download, IsleColors.TileBlue, "Daily update check", autoUpdateCheck,
                    subtitle = "Check daily in background & notify when an update is available",
                ) {
                    autoUpdateCheck = it
                    prefs.edit().putBoolean("auto_check_updates_daily", it).apply()
                    if (it) UpdateChecker.scheduleDailyCheck(context)
                }
                GroupDivider()
                NavRow(
                    IsleIcons.Sparkles, IsleColors.TilePurple, "Check for updates",
                    value = if (activeUpdateInfo != null) "Update available!" else "Check now",
                    onClick = {
                        coroutineScope.launch {
                            updateStatus = "Checking for updates…"
                            when (val result = UpdateChecker.check(context)) {
                                is UpdateChecker.Result.UpToDate -> {
                                    updateStatus = "You're on the latest version (v${result.current})."
                                }
                                is UpdateChecker.Result.UpdateAvailable -> {
                                    updateStatus = null
                                    activeUpdateInfo = result.info
                                    startDownloadOnOpen = false
                                }
                                is UpdateChecker.Result.Error -> {
                                    updateStatus = result.message
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    activeUpdateInfo?.let { info ->
        UpdateDialog(
            context = context,
            info = info,
            startDownloadImmediately = startDownloadOnOpen,
            onDismiss = {
                activeUpdateInfo = null
                startDownloadOnOpen = false
            },
        )
    }
}

/** Green "Listening" / red "Not listening" pill under the title; opens the Activity screen. */
@Composable
private fun StatusPill(connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) "Listening" else "Not listening",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IsleColors.Text) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 200.dp).heightIn(min = 48.dp),
    )
}

/** Island preview + sample picker + send button: fires any [IslandSamples] card on demand. */
@Composable
private fun SampleTester(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all[selected]
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) { delay(1600); sentAt = 0L }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(IsleColors.Surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(IsleColors.PreviewBg)
                .border(1.dp, IsleColors.PreviewBorder, RoundedCornerShape(20.dp))
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // What the compact island will look like once this sample is sent.
            IslandPreviewView(sample.preview)
            Text(
                sample.summary,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.PreviewLabel,
                textAlign = TextAlign.Center,
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Text,
                    selectedContent = IsleColors.Background,
                    shape = RoundedCornerShape(18.dp),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }
        val haptic = LocalHapticFeedback.current
        PillButton(
            if (sentAt != 0L) "Sent" else "Send to island",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                sample.post(context)
                sentAt = System.currentTimeMillis()
            },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            icon = IsleIcons.Send,
            container = IsleColors.Accent,
            content = Color.White,
            height = 48.dp,
        )
    }
}

/** The platform time picker, in minutes since midnight, honouring the device's 12/24h setting. */
private fun pickTime(context: Context, initial: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initial / 60, initial % 60,
        android.text.format.DateFormat.is24HourFormat(context),
    ).show()
}

private fun stopAll(context: Context) {
    context.startService(Intent(context, PlaygroundService::class.java).setAction(PlaygroundService.ACTION_STOP))
}

/**
 * Ask the bound notification listener to replay every current notification onto the island. If the
 * listener isn't connected (access not granted, or the binding went stale after a kill), keep it
 * alive and request a rebind so the sweep works on the next tap.
 */
private fun recastAll(context: Context) {
    PlaygroundService.keepAlive(context)
    val listener = NotificationCastListener.instance
    if (listener == null) {
        val message = when (NotificationCastListener.forceRebind(context)) {
            NotificationCastListener.RebindResult.NO_ACCESS ->
                "Notification access isn't granted — turn it on in Settings, then tap again."
            else ->
                "Listener not connected — reconnecting now. Give it a few seconds, then tap again."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        return
    }
    val count = listener.recastAll()
    android.widget.Toast.makeText(
        context, "Recast swept $count notifications.", android.widget.Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.75f))
    }
}

/**
 * Real-time Punch-Hole Camera Cutout & Island Margin Tuner.
 * Provides live visual feedback, position choices, gap & safe-margin sliders,
 * and a one-tap live status bar test trigger.
 */
@Composable
private fun CameraCutoutTuner(context: Context, prefs: SharedPreferences) {
    val haptic = LocalHapticFeedback.current
    var cutoutPos by remember { mutableStateOf(prefs.getString("camera_cutout_position", "center") ?: "center") }
    var cutoutGap by remember { mutableFloatStateOf(prefs.getFloat("camera_cutout_gap", 34f)) }
    var safeMargin by remember { mutableFloatStateOf(prefs.getFloat("camera_cutout_margin", 8f)) }
    var testActive by remember { mutableStateOf(PlaygroundService.activeCardIds.contains(40099)) }

    fun updateCutoutIfActive(pos: String, gap: Float, margin: Float) {
        if (testActive) {
            postStatusBarTest(context, pos, gap, margin)
        }
    }

    SettingRow(
        icon = IsleIcons.Camera,
        tileColor = IsleColors.TileAmber,
        title = "Punch-hole camera alignment",
        subtitle = "Align island pill with your front camera cutout",
    )

    // Interactive Real-Time Live Status Bar Preview Box
    StatusBarPreviewFrame(
        cutoutPos = cutoutPos,
        cutoutGap = cutoutGap,
        safeMargin = safeMargin,
        isTestPinnedOnDevice = testActive,
    )

    // Cutout Position Chips (Center vs Corner)
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Camera Position", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                "Center (vivo X / iQOO)",
                selected = cutoutPos == "center",
                onClick = {
                    cutoutPos = "center"
                    prefs.edit().putString("camera_cutout_position", "center").apply()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    updateCutoutIfActive("center", cutoutGap, safeMargin)
                },
                modifier = Modifier.weight(1.2f),
            )
            ChoiceChip(
                "Left Corner",
                selected = cutoutPos == "left",
                onClick = {
                    cutoutPos = "left"
                    prefs.edit().putString("camera_cutout_position", "left").apply()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    updateCutoutIfActive("left", cutoutGap, safeMargin)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Slider for Camera Cutout Gap (22dp to 50dp)
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Camera Gap Width", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
            Text("${cutoutGap.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IsleColors.TileAmber)
        }
        Slider(
            value = cutoutGap,
            onValueChange = {
                cutoutGap = it
                prefs.edit().putFloat("camera_cutout_gap", it).apply()
                updateCutoutIfActive(cutoutPos, it, safeMargin)
            },
            valueRange = 22f..50f,
            steps = 27,
            colors = SliderDefaults.colors(
                thumbColor = IsleColors.TileAmber,
                activeTrackColor = IsleColors.TileAmber,
                inactiveTrackColor = IsleColors.PreviewBorder,
            ),
        )
    }

    // Slider for Safe Text Margin (0dp to 16dp)
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Punch-Hole Safe Margin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
            Text("${safeMargin.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IsleColors.TileGreen)
        }
        Slider(
            value = safeMargin,
            onValueChange = {
                safeMargin = it
                prefs.edit().putFloat("camera_cutout_margin", it).apply()
                updateCutoutIfActive(cutoutPos, cutoutGap, it)
            },
            valueRange = 0f..16f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = IsleColors.TileGreen,
                activeTrackColor = IsleColors.TileGreen,
                inactiveTrackColor = IsleColors.PreviewBorder,
            ),
        )
    }

    // Test on Actual Status Bar Button
    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
        PillButton(
            text = if (testActive) "Dismiss Status Bar Test" else "Test on Status Bar 🚀",
            icon = if (testActive) IsleIcons.Close else IsleIcons.Send,
            container = if (testActive) IsleColors.TileGray else IsleColors.TileAmber,
            content = Color.Black,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (testActive) {
                    context.startService(
                        Intent(context, PlaygroundService::class.java)
                            .setAction(PlaygroundService.ACTION_CANCEL)
                            .putExtra("id", 40099)
                            .putExtra("oi_scene", "NAVIGATION"),
                    )
                    testActive = false
                    android.widget.Toast.makeText(context, "Status bar test dismissed.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    postStatusBarTest(context, cutoutPos, cutoutGap, safeMargin)
                    testActive = true
                    android.widget.Toast.makeText(context, "Test card cast to status bar!", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

/**
 * High-contrast, realistic phone status bar simulation frame.
 * Displays top phone speaker, clock, 5G/battery system status, and the Island pill.
 */
@Composable
private fun StatusBarPreviewFrame(
    cutoutPos: String,
    cutoutGap: Float,
    safeMargin: Float,
    isTestPinnedOnDevice: Boolean,
) {
    val isLeft = cutoutPos == "left"
    val testPreview = remember(cutoutPos, cutoutGap, safeMargin) {
        IslandPreview(
            icon = R.drawable.ic_navigation,
            left = if (isLeft) "◄ Left-Fit" else "300m Left",
            right = IslandPreview.Right.Capsule("${cutoutGap.toInt()} dp"),
        )
    }

    Column(
        Modifier
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E212A), Color(0xFF13151A))
                )
            )
            .border(1.2.dp, Color(0xFF333A48), RoundedCornerShape(22.dp))
            .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top phone speaker slit simulation
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(3.5.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2E3B))
        )

        // Mock Phone Status Bar Surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0C0E12))
                .border(1.dp, Color(0xFF252A36), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left Status: Time clock (or Corner Cutout + Time)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isLeft) {
                    CameraLensHole(size = 12.dp)
                }
                Text(
                    "09:41",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }

            // Center: The dynamic island pill preview
            Box(
                modifier = Modifier.padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                IslandPreviewView(
                    preview = testPreview,
                    cameraGap = cutoutGap.dp,
                    showCameraHole = true,
                    cutoutPosition = cutoutPos,
                    safeMargin = safeMargin.dp,
                )
            }

            // Right Status: Icons (5G, Wi-Fi, Battery)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    "5G",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Icon(
                    IsleIcons.Pulse,
                    null,
                    modifier = Modifier.size(13.dp),
                    tint = Color.White.copy(alpha = 0.85f),
                )
                // Battery capsule with 85% green charge
                Row(
                    modifier = Modifier
                        .width(22.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                        .padding(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.85f)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFF34C759))
                    )
                    Spacer(Modifier.weight(0.15f))
                }
            }
        }

        // Live Telemetry Info Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isLeft) "Punch-Hole: Left Corner" else "Punch-Hole: Center Cutout",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = IsleColors.TextSecondary,
            )
            Text(
                "Gap: ${cutoutGap.toInt()}dp • Margin: ${safeMargin.toInt()}dp",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.TileAmber,
            )
        }

        if (isTestPinnedOnDevice) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F291E))
                    .border(1.dp, Color(0xFF1E5E43), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Text(
                    "LIVE ON ORIGIN STATUS BAR • REAL-TIME SYNC",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

/**
 * Posts an ongoing, high-priority test card directly onto the Vivo OriginOS Status Bar.
 * Sets is_ongoing, click_resp, oi_force_show, and interactive dismissal action.
 */
private fun postStatusBarTest(
    context: Context,
    cutoutPos: String,
    cutoutGap: Float,
    safeMargin: Float,
) {
    val openIntent = PendingIntent.getActivity(
        context,
        40099,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val dismissIntent = PendingIntent.getService(
        context,
        40099,
        Intent(context, PlaygroundService::class.java)
            .setAction(PlaygroundService.ACTION_CANCEL)
            .putExtra("id", 40099)
            .putExtra("oi_scene", "NAVIGATION"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val dismissAction = Notification.Action.Builder(
        AndroidIcon.createWithResource(context, R.drawable.ic_delete),
        "Dismiss",
        dismissIntent,
    ).build()

    val navIcon = AndroidIcon.createWithResource(context, R.drawable.ic_navigation)
    val isLeft = cutoutPos == "left"
    val leftContent = if (isLeft) "◄ Left-Fit" else "◄ L-Fit"
    val rightContent = "${cutoutGap.toInt()}dp ►"

    val testIntent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("id", 40099)
        putExtra("is_ongoing", true)
        putExtra("oi_force_show", true)
        putExtra("oi_priority", 99)
        putExtra("oi_scene", "NAVIGATION")
        putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
        putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
        putExtra("title", "Camera Cutout Aligned ✓")
        putExtra("text", "Pos: ${if (isLeft) "Left Corner" else "Center"} • Gap: ${cutoutGap.toInt()}dp • Margin: ${safeMargin.toInt()}dp")
        putExtra("subtext", "Origin Island Pro Tuner")
        putExtra("source_app", "Island Tuner")
        putExtra("oi_left_content", leftContent)
        putExtra("oi_right_content", rightContent)
        putExtra("status_chip_text", rightContent)
        putExtra("oi_left_icon", navIcon)
        putExtra("oi_large_icon", navIcon)
        putExtra("icon_res", R.drawable.ic_navigation)
        putExtra("oi_bg_color", "#FF00C853")
        putExtra("oi_fg_color", "#FFFFFFFF")
        putExtra("oi_light_color", "#FF00C853")
        putExtra("oi_light_mode", 1)
        putExtra("source_pkg", context.packageName)
        putExtra("oi_landing_pkg", context.packageName)
        putExtra("oi_landing_info", context.packageName)
        putExtra("source_content_intent", openIntent)
        putExtra("click_resp", openIntent)
        putExtra("oi_displays", OriginIslandConstants.DISPLAY_STATUSBAR or OriginIslandConstants.DISPLAY_AOD)
        putParcelableArrayListExtra("actions", arrayListOf(dismissAction))
        putStringArrayListExtra("oi_button_titles", arrayListOf("Dismiss Test"))
        putIntegerArrayListExtra("oi_button_bg_colors", arrayListOf(0xFFFF3B30.toInt()))
        putIntegerArrayListExtra("oi_button_text_colors", arrayListOf(0xFFFFFFFF.toInt()))
    }
    context.startService(testIntent)
}
