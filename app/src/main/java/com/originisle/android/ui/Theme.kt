package com.originisle.android.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Whether the app is currently drawn dark (follows the system setting). */
val LocalIsleDark = staticCompositionLocalOf { true }

/**
 * The app palette, OriginOS 7 Settings style (plan task T3.1).
 *
 * Neutral tokens are read from the system's wallpaper-based colour scheme, so the app follows
 * light/dark mode and the user's theme colour like vivo's own Settings. Status colours have a
 * light and a dark variant. The names are unchanged from the old fixed dark palette, so call
 * sites did not have to change; they must be read inside a composable.
 */
object IsleColors {
    private val dark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsleDark.current

    /** Page background: light grey behind white groups (light), darkest surface (dark). */
    val Background: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    /** Grouped-card fill. */
    val Surface: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest

    /** Dividers, idle chips, segmented-control track. */
    val SurfaceHigh: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /** Selected segment. */
    val SurfaceHigher: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant

    val Menu: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val Text: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val TextSecondary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val TextTertiary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val TextChip: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val Chevron: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outline

    val Accent: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val AccentText: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val SwitchOff: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val Danger: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val Good = Color(0xFF34C759)
    val Bad = Color(0xFFFF453A)
    val Warn = Color(0xFFFF9F0A)

    val GoodText: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF5CD985) else Color(0xFF1E7B34)

    val GoodBg: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF132519) else Color(0xFFE6F4EA)

    val GoodButton: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF1B3825) else Color(0xFFCEEAD6)

    val GoodSubtle: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF9ECCAF) else Color(0xFF3C7A50)

    val GoodIconBg: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF163822) else Color(0xFFCEEAD6)

    val BadText: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFFFF7B72) else Color(0xFFC5221F)

    val BadBg: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF2B1615) else Color(0xFFFCE8E6)

    val BadButton: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF3D1E1B) else Color(0xFFFAD2CF)

    val BadSubtle: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFFE2A199) else Color(0xFFA5443B)

    val WarnBg: Color
        @Composable @ReadOnlyComposable
        get() = if (dark) Color(0xFF2C2013) else Color(0xFFFEF3E0)

    val CastTagBg: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondaryContainer

    val CastTagFg: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSecondaryContainer

    // The island itself is always black on the device, so its preview stays dark in both themes.
    val PreviewBg = Color(0xFF121215)
    val PreviewBorder = Color(0xFF24242C)
    val PreviewLabel = Color(0xFF8E8E98)

    // Vibrant, category-specific modern pastel-neon tile accents:
    val TileCall = Color(0xFF34C759)      // Vibrant Emerald for calls
    val TileChat = Color(0xFF2AABEE)      // Telegram Azure for chats
    val TileCalendar = Color(0xFFFF5A79)  // Coral Rose for meetings
    val TileDownload = Color(0xFF00C2A0)  // Modern Aqua for downloads
    val TileRide = Color(0xFFFFB800)      // Amber Gold for rides
    val TileDelivery = Color(0xFFFF2B85)  // Magenta for deliveries
    val TileTimer = Color(0xFFFF9500)     // Radiant Orange for timers
    val TileAuth = Color(0xFF8B5CF6)      // Violet for 2FA / Login
    val TileWeather = Color(0xFF29B6F6)   // Sky Blue for weather
    val TileSports = Color(0xFF10B981)    // Emerald Green for sports
    val TileMedia = Color(0xFFFA2D48)     // Apple Music Red for media
    val TilePayment = Color(0xFFE2136E)   // bKash Magenta / Payment
    val TileRaiden = Color(0xFFA855F7)    // Electric Violet for Raiden companion

    // Legacy aliases preserved for backward compatibility
    val TileBlue = Color(0xFF2D6BF5)
    val TileOrange = Color(0xFFF2683C)
    val TileTeal = Color(0xFF00C2A0)
    val TilePurple = Color(0xFF8B5CF6)
    val TileGreen = Color(0xFF34C759)
    val TileAmber = Color(0xFFFFB800)
    val TileGray = Color(0xFF4A4A55)
    val TileRose = Color(0xFFFF5A79)
    val TileIndigo = Color(0xFF5C6CFF)
    val TileCyan = Color(0xFF00C2FF)
    val TileOrange2 = Color(0xFFE87040)
}

/**
 * OriginOS 7 Settings look: system font (vivo Sans on device), the wallpaper-based system colour
 * scheme, and the system light/dark setting.
 */
@Composable
fun OriginIsleTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalIsleDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
