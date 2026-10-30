package com.originisle.android.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.originisle.android.R

/** The redesign's palette: luxury obsidian carbon, grouped frosted cards, refined dynamic accents. */
object IsleColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    /** Dividers, idle chips, segmented-control track. */
    val SurfaceHigh = Color(0xFF222228)
    /** Selected segment. */
    val SurfaceHigher = Color(0xFF32323A)
    val Menu = Color(0xFF202026)

    val Text = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9898A0)
    val TextTertiary = Color(0xFFA8A8B2)
    val TextChip = Color(0xFFD6D6DC)
    val Chevron = Color(0xFF70707A)

    // Sleek Apple / OriginOS Electric Sapphire (replacing harsh raw blue)
    val Accent = Color(0xFF2D6BF5)
    val AccentText = Color(0xFF90B4FE)
    val SwitchOff = Color(0xFF34343C)
    val Danger = Color(0xFFFF6961)

    val Good = Color(0xFF34C759)
    val GoodText = Color(0xFF5CD985)
    val GoodBg = Color(0xFF132519)
    val GoodButton = Color(0xFF1B3825)
    val GoodSubtle = Color(0xFF9ECCAF)
    val GoodIconBg = Color(0xFF163822)

    val Bad = Color(0xFFFF453A)
    val BadText = Color(0xFFFF7B72)
    val BadBg = Color(0xFF2B1615)
    val BadButton = Color(0xFF3D1E1B)
    val BadSubtle = Color(0xFFE2A199)

    val Warn = Color(0xFFFF9F0A)
    val WarnBg = Color(0xFF2C2013)

    // Deep frosted obsidian carbon container with crisp OLED contrast
    val CastTagBg = Color(0xFF1C1E26)
    val CastTagFg = Color(0xFFB0C4DE)
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

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val JakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun TextStyle.jakarta() = copy(fontFamily = JakartaSans)

private val IsleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.jakarta(), displayMedium = displayMedium.jakarta(),
        displaySmall = displaySmall.jakarta(), headlineLarge = headlineLarge.jakarta(),
        headlineMedium = headlineMedium.jakarta(), headlineSmall = headlineSmall.jakarta(),
        titleLarge = titleLarge.jakarta(), titleMedium = titleMedium.jakarta(),
        titleSmall = titleSmall.jakarta(), bodyLarge = bodyLarge.jakarta(),
        bodyMedium = bodyMedium.jakarta(), bodySmall = bodySmall.jakarta(),
        labelLarge = labelLarge.jakarta(), labelMedium = labelMedium.jakarta(),
        labelSmall = labelSmall.jakarta(),
    )
}

/**
 * Origin Isle is dark-mode
 */
private val OriginIsleColorScheme = darkColorScheme(
    primary = IsleColors.Accent,
    onPrimary = Color.White,
    secondary = IsleColors.Good,
    onSecondary = Color.White,
    background = IsleColors.Background,
    onBackground = IsleColors.Text,
    surface = IsleColors.Surface,
    onSurface = IsleColors.Text,
    surfaceVariant = IsleColors.SurfaceHigh,
    onSurfaceVariant = IsleColors.TextTertiary,
    surfaceContainer = IsleColors.Menu,
    outline = IsleColors.SwitchOff,
)

@Composable
fun OriginIsleTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = OriginIsleColorScheme, typography = IsleTypography, content = content)
}
