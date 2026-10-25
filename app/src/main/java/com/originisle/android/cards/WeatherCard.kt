package com.originisle.android.cards

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.PriorityArbitrator

/**
 * Casts weather warnings, rain alerts and forecasts as an island card.
 */
object WeatherCard {

    private const val ORANGE = "#FFE87040"

    fun post(
        context: Context,
        id: Int,
        headline: String,
        chip: String,
        clickResp: PendingIntent?,
    ) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_WEATHER)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", headline.take(40))
            putExtra("text", "Weather alert")
            putExtra("oi_left_content", "Weather")
            putExtra("status_chip_text", chip.take(12))
            putExtra("oi_right_content", chip.take(12))
            putExtra("icon_res", R.drawable.ic_alert)
            putExtra("oi_bg_color", ORANGE)
            putExtra("oi_light_color", ORANGE)
            if (clickResp != null) putExtra("source_content_intent", clickResp)
        }
        context.startService(intent)
    }

    /** Known weather providers & apps. */
    val WEATHER_APPS = setOf(
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.weather",
        "com.accuweather.android",
        "com.yahoo.mobile.client.android.weather",
        "cz.meteogroup.meteonews",
        "com.samlamore.weather",
        "net.baw.weatherapp",
        "uk.co.bbc.weather",
        "com.weather.Weather",
        "com.apalon.weather",
        "com.samsung.android.weather",
        "com.miui.weather2",
        "com.huawei.android.totemweather",
        "com.xiaomi.weather.overlay.miui",
        "org.breezyweather",
        "com.pluckar.rain_alarm",
        "com.weatherbug.bb",
        "com.climacell.climacell",
        "com.wunderground.android.weather",
        "co.uk.metoffice.android",
    )

    /** Weather event keywords. */
    val WEATHER_KEYWORDS = listOf(
        "storm", "rain", "thunder", "lightning", "warning", "alert",
        "forecast", "temperature", "weather", "wind", "humidity",
        "sunshine", "cloudy", "sunny", "fog", "snow", "hail",
        "°c", "°f", "feels like", "uv index", "air quality",
    )

    /** Extract a short chip label from weather notification text. */
    fun extractChip(title: String, text: String): String {
        val combined = "$title $text"
        val tempMatch = Regex("""-?\d{1,3}\s*°\s*[CF]""").find(combined)
        if (tempMatch != null) return tempMatch.value.replace(" ", "")

        if (combined.contains("storm", ignoreCase = true) || combined.contains("thunder", ignoreCase = true)) return "⛈ Storm"
        if (combined.contains("rain", ignoreCase = true)) return "🌧 Rain"
        if (combined.contains("snow", ignoreCase = true)) return "❄ Snow"
        if (combined.contains("sunny", ignoreCase = true) || combined.contains("sunshine", ignoreCase = true)) return "☀ Sunny"
        if (combined.contains("cloudy", ignoreCase = true)) return "☁ Cloudy"
        if (combined.contains("fog", ignoreCase = true)) return "🌫 Fog"
        if (combined.contains("wind", ignoreCase = true)) return "💨 Wind"
        return "Alert"
    }
}
