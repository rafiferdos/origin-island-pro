package com.originisle.android.cards

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.util.PriorityArbitrator

/**
 * Casts a parcel or delivery-tracking notification as a dedicated island card.
 *
 * Delivery apps (Amazon, DHL, FedEx, UPS, DoorDash, Uber Eats, etc.) post updates like
 * "Your parcel is out for delivery · Arriving by 4 PM". This card puts the courier and status
 * on the left and a neat capsule ETA on the right.
 */
object DeliveryCard {

    private const val CYAN = "#FF0EA5C8"

    fun post(
        context: Context,
        id: Int,
        title: String,
        eta: String,
        carrier: String,
        appIcon: Icon?,
        clickResp: PendingIntent?,
        sourcePkg: String? = null,
    ) {
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            sourcePkg?.let { putExtra("source_pkg", it) }
            putExtra("oi_priority", PriorityArbitrator.PRIORITY_DELIVERY)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("title", title.take(40))
            putExtra("text", carrier.take(30))
            putExtra("oi_left_content", carrier.take(16))
            putExtra("status_chip_text", eta.take(12))
            putExtra("oi_right_content", eta.take(12))
            putExtra("icon_res", R.drawable.ic_alert)
            putExtra("oi_bg_color", CYAN)
            putExtra("oi_fg_color", "#FFFFFFFF")
            putExtra("oi_light_color", CYAN)
            putExtra("oi_light_mode", 1)
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            if (clickResp != null) putExtra("source_content_intent", clickResp)
        }
        context.startService(intent)
    }

    /** Known logistics, courier, shopping and food delivery apps. */
    val DELIVERY_APPS = setOf(
        // Bangladesh & Regional Top Delivery / Rides / E-Commerce
        "com.pathao.user",
        "com.pathao.drive",
        "com.global.foodpanda.android",
        "com.daraz.android",
        "com.chaldal.poached",
        "com.shohoz.rides",
        "com.hungrynaki.user",
        "com.ubercab",
        "com.ubercab.eats",

        // Global Logistics & Courier
        "com.amazon.mShop.android.shopping",
        "com.amazon.dee.app",
        "com.dhl.parcel",
        "com.dhlandroid",
        "de.dhl.paket",
        "com.fedex.mobile",
        "com.ups.mobile",
        "com.shein.shopping",
        "com.zalando.mobile",
        "com.zalando.lounge",
        "com.shopify.arrive",
        "com.ebay.mobile",
        "com.aliexpress.buyer",
        "com.trendyol.shopapp",
        "com.lazada.android",
        "com.shopee.ph",
        "vn.tiki.app.tikiandroid",
        "com.deliveroo.orderapp",
        "com.doordash.driverapp",
        "com.doordash.consumer",
        "com.glovo.app",
        "com.instacart.client",
        "com.grubhub.android",
        "com.postmates.android",
        "com.myhermes.app",
        "com.dpd.dpdapp",
        "uk.co.royalmail.deliveryapp",
        "com.usps",
    )

    /** Keywords indicating delivery tracking. */
    val DELIVERY_KEYWORDS = listOf(
        "out for delivery", "on its way", "arriving today", "arriving tomorrow",
        "delivery today", "delivered", "package delivered", "parcel delivered",
        "your order has shipped", "order shipped", "shipment update",
        "estimated delivery", "expected delivery", "delivery attempt",
        "tracking update", "in transit", "order is out", "will be delivered",
        "picked up by courier", "with courier", "left the warehouse",
        "order dispatched", "dispatched", "on the way",
        "rider is on the way", "rider arriving", "food is being prepared",
        "picked up your order", "order delivered", "parcel is on the way",
        "delivery hero", "delivering your order", "rider assigned", "order confirmed",
    )

    /** Extract ETA or short status from notification text. */
    fun extractEta(text: String): String {
        // 1. Minute countdown: "15 min", "in 20 mins", "15m", "arriving in 12 mins"
        val minPattern = Regex("""(?:\bin\s+)?(\d{1,2})\s*(?:mins?|minutes?|m\b)""", RegexOption.IGNORE_CASE)
        val minMatch = minPattern.find(text)
        if (minMatch != null) {
            val num = minMatch.groupValues[1]
            return "$num min"
        }

        // 2. Specific clock time: "by 4:30 PM", "before 5 PM", "12:45 pm"
        val timePattern = Regex("""(?:by |before |between )?(\d{1,2}(?::\d{2})?\s*(?:am|pm|AM|PM)(?:\s*[-–]\s*\d{1,2}(?::\d{2})?\s*(?:am|pm|AM|PM))?)""")
        val timeMatch = timePattern.find(text)
        if (timeMatch != null) return timeMatch.value.trim().take(12)

        if (text.contains("delivered", ignoreCase = true)) return "Delivered ✓"
        if (text.contains("today", ignoreCase = true)) return "Today"
        if (text.contains("tomorrow", ignoreCase = true)) return "Tomorrow"
        if (text.contains("out for delivery", ignoreCase = true) || text.contains("on the way", ignoreCase = true)) return "On the way"
        if (text.contains("in transit", ignoreCase = true)) return "In transit"
        if (text.contains("prepared", ignoreCase = true) || text.contains("cooking", ignoreCase = true)) return "Preparing"
        return "Tracking"
    }
}
