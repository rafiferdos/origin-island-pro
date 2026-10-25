package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.IntelligentTextSanitizer
import com.originisle.android.util.PriorityArbitrator

/**
 * Ride-Hailing & Taxi Live Card for Origin Island Pro.
 * Intercepts notifications from Uber, Pathao Rides, Shohoz, Grab, Bolt, etc.
 * Surfaces driver ETA, vehicle license plate, and car model on the island pill.
 */
object RideCard {

    val RIDE_APPS = setOf(
        "com.ubercab",
        "com.pathao.user",
        "com.pathao.drive",
        "com.shohoz.rides",
        "com.grabtaxi.passenger",
        "com.olacabs.customer",
        "ru.yandex.taxi",
        "com.careem.acma",
        "com.bolt.android",
    )

    val RIDE_KEYWORDS = listOf(
        "driver is on the way", "driver arriving", "captain is on the way",
        "captain arriving", "ride confirmed", "driver accepted", "driver has arrived",
        "arriving in", "your ride", "vehicle", "license plate",
    )

    fun post(
        context: Context,
        id: Int,
        title: String,
        details: String,
        appLabel: String,
        appIcon: Icon?,
        clickResp: PendingIntent?,
        actions: List<Notification.Action> = emptyList(),
    ) {
        val hay = "$title $details"
        val eta = DeliveryCard.extractEta(hay).ifBlank { "Ride" }
        val plate = IntelligentTextSanitizer.extractVehiclePlate(hay)
        val cleanTitle = IntelligentTextSanitizer.cleanTitle(title, appLabel)
        val cleanDetails = IntelligentTextSanitizer.cleanBody(details, cleanTitle, appLabel)

        val expandedTitle = if (plate != null && !cleanTitle.contains(plate)) {
            "$cleanTitle · $plate"
        } else {
            cleanTitle
        }

        val usable = actions.filter { a ->
            a.actionIntent != null && !a.title.isNullOrBlank()
        }.take(2)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true) // Live ongoing ride tracking
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_RIDE)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template",
                if (usable.isNotEmpty()) OriginIslandConstants.TEMPLATE_BUTTONS
                else OriginIslandConstants.TEMPLATE_BASE,
            )
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", expandedTitle)
            putExtra("text", cleanDetails.ifBlank { "Driver is arriving" })
            putExtra("subtext", appLabel)
            putExtra("source_app", appLabel)

            // Short compact pill
            putExtra("oi_left_content", "Ride")
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            putExtra("oi_right_content", eta)
            putExtra("status_chip_text", eta)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("source_content_intent", clickResp)
            if (usable.isNotEmpty()) putParcelableArrayListExtra("actions", ArrayList(usable))
        }
        context.startService(intent)
    }
}
