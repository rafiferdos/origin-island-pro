package com.originisle.android.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Per-app allow list: every launchable app, grouped A–Z, each with a switch. */
@Composable
fun AppsScreen(context: Context, prefs: SharedPreferences, onBack: () -> Unit) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    // 0 = all, 1 = on only, 2 = off only.
    var filter by remember { mutableIntStateOf(0) }
    val ignored = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty()) }
    }
    val enabled = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("cast_enabled_apps", emptySet()).orEmpty()) }
    }

    /**
     * Whether the per-app switch is an opt-out or an opt-in list.
     *
     * `cast_enabled_apps` used to be read by the listener but never written anywhere, so the
     * allow-list branch was dead and the switch silently behaved as opt-out only. This makes the
     * opt-in mode real: when on, the switch writes the ALLOW set (packages that may cast) and every
     * app not in it is blocked; when off, it writes the IGNORE set (the historical behaviour).
     *
     * The mode is persisted so a user who opted into the stricter mode keeps it across restarts.
     */
    var optInMode by remember { mutableStateOf(prefs.getBoolean("cast_app_opt_in_mode", false)) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
        loading = false
    }

    /** Persist both sets. The mode decides which one is authoritative in the listener. */
    fun persist(newIgnored: Set<String>, newEnabled: Set<String>) {
        prefs.edit()
            .putStringSet("cast_ignored_apps", newIgnored)
            .putStringSet("cast_enabled_apps", newEnabled)
            .apply()
    }

    val filtered = apps.filter { app ->
        (query.isBlank() || app.label.contains(query, ignoreCase = true)) &&
            when (filter) {
                1 -> app.pkg !in ignored
                2 -> app.pkg in ignored
                else -> true
            }
    }
    val groups = filtered.groupBy { it.label.firstOrNull()?.takeIf(Char::isLetter)?.uppercaseChar() ?: '#' }
    // Acts on whatever's currently shown — with no search or filter that's every app, but it also
    // doubles as "enable/disable all matching X" once you've typed a query.
    val anyOff = filtered.any { it.pkg in ignored }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                ScreenHeader("Apps", onBack) {
                    if (!loading && filtered.isNotEmpty()) {
                        PillButton(
                            if (anyOff) "All on" else "All off",
                            onClick = {
                                if (anyOff) ignored.removeAll(filtered.map { it.pkg }.toSet())
                                else filtered.forEach { if (it.pkg !in ignored) ignored.add(it.pkg) }
                                prefs.edit().putStringSet("cast_ignored_apps", ignored.toSet()).apply()
                            },
                            content = IsleColors.AccentText,
                            height = 44.dp,
                        )
                    }
                }
                SearchField(query, onChange = { query = it })
                SegmentedControl(
                    listOf("All", "On", "Off"), filter, { filter = it },
                    track = IsleColors.Surface, height = 40.dp,
                )
                Text(
                    if (optInMode) {
                        "Opt-in mode: ONLY the apps switched ON below may cast. Everything else is blocked."
                    } else {
                        "Turn OFF the apps you don't want cast. Plain chat texts are already filtered out, " +
                            "so a messenger's calls still show even while its messages don't."
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IsleColors.TextSecondary,
                )
                ToggleRow(
                    IsleIcons.Shield,
                    IsleColors.TileAuth,
                    "Opt-in mode",
                    checked = optInMode,
                    subtitle = "Only allow the apps you switch ON (blocks everything else)",
                ) { on ->
                    optInMode = on
                    prefs.edit().putBoolean("cast_app_opt_in_mode", on).apply()
                    // Enabling opt-in changes which set is authoritative, so the allow set has to
                    // exist for it to mean anything. Seed it with the apps currently NOT ignored —
                    // otherwise switching the mode on would block every app at once.
                    if (on && enabled.isEmpty()) {
                        val seed = apps.filter { it.pkg !in ignored }.map { it.pkg }.toSet()
                        enabled.clear()
                        enabled.addAll(seed)
                    }
                    persist(ignored.toSet(), enabled.toSet())
                }
            }
        }

        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IsleColors.Accent)
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyMessage(if (query.isNotBlank()) "No apps match \"$query\"." else "No apps")
            }
        }

        groups.forEach { (letter, items) ->
            item(key = "letter-$letter") {
                SectionLabel(letter.toString(), Modifier.padding(top = 12.dp, bottom = 8.dp))
            }
            itemsIndexed(items, key = { _, app -> app.pkg }) { i, app ->
                // In opt-in mode "on" means "in the allow set"; in opt-out mode it means "not in the
                // ignore set". Same switch, two persisted sets.
                val on = if (optInMode) app.pkg in enabled else app.pkg !in ignored
                Column(Modifier.clip(groupItemShape(i, items.size)).background(IsleColors.Surface)) {
                    if (i > 0) GroupDivider(inset = 66.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = on, role = Role.Switch) { allowed ->
                                if (optInMode) {
                                    if (allowed) enabled.add(app.pkg) else enabled.remove(app.pkg)
                                    persist(ignored.toSet(), enabled.toSet())
                                } else {
                                    if (allowed) {
                                        ignored.remove(app.pkg)
                                    } else if (app.pkg !in ignored) {
                                        ignored.add(app.pkg)
                                    }
                                    persist(ignored.toSet(), enabled.toSet())
                                }
                            }
                            .heightIn(min = 60.dp)
                            .padding(start = 16.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))) {
                            app.icon?.let { Image(it, null, Modifier.size(36.dp)) }
                        }
                        Text(
                            app.label,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) IsleColors.Text else IsleColors.TextSecondary,
                        )
                        IsleSwitch(on, onCheckedChange = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .padding(start = 16.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(IsleIcons.Search, null, Modifier.size(18.dp), tint = IsleColors.TextSecondary)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search apps", fontSize = 16.sp, color = IsleColors.TextSecondary)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = JakartaSans, fontSize = 16.sp, color = IsleColors.Text),
                cursorBrush = SolidColor(IsleColors.Accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search apps" },
            )
        }
        if (query.isNotEmpty()) {
            CircleIconButton(
                IsleIcons.Close, "Clear search", onClick = { onChange("") },
                background = IsleColors.Surface, tint = IsleColors.TextSecondary, iconSize = 18.dp,
            )
        }
    }
}

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

private fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    return pm.getInstalledApplications(0)
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
        .map { info ->
            AppEntry(
                pkg = info.packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = runCatching { pm.getApplicationIcon(info).toBitmap(72, 72).asImageBitmap() }.getOrNull(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
