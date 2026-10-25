package com.originisle.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The redesign's line icons, from the same 24×24 SVG paths as the design mockups. Drawn in black;
 * `Icon`'s tint recolors them.
 */
object IsleIcons {
    val Bell = stroke("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val Chat = stroke("chat", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z")
    val Call = stroke("call", "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z")
    val CallEnd = stroke("call-end", "M12 9c-3.15 0-6.07 1.01-8.49 2.73l1.83 1.83c1.92-1.3 4.21-2.06 6.66-2.06s4.74.76 6.66 2.06l1.83-1.83C18.07 10.01 15.15 9 12 9z", "M4.05 17.27l2.84-.81a1 1 0 0 0 .78-.9l.42-3.03a12.8 12.8 0 0 0-3.03-1.85l-1.92 1.92a1 1 0 0 0-.2 1.09l1.11 3.58z", "M19.95 17.27l1.11-3.58a1 1 0 0 0-.2-1.09l-1.92-1.92a12.8 12.8 0 0 0-3.03 1.85l.42 3.03a1 1 0 0 0 .78.9l2.84.81z")
    val Calendar = stroke("calendar", roundRect(3f, 4f, 18f, 17f, 2f), "M16 2v4", "M8 2v4", "M3 9h18")
    val Car = stroke("car", "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9L16 10l-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9L2 11c-.7.4-1 1-1 2v3c0 .6.4 1 1 1h2", circle(6.5f, 17f, 2.5f), circle(16.5f, 17f, 2.5f))
    val Music = stroke("music", "M9 18V5l12-2v13", circle(6f, 18f, 3f), circle(18f, 16f, 3f))
    val Clock = stroke("clock", circle(12f, 12f, 9f), "M12 7v5l3 2")
    val Moon = stroke("moon", "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Lock = stroke("lock", roundRect(5f, 11f, 14f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")
    val Grid = stroke(
        "grid",
        roundRect(4f, 4f, 6.5f, 6.5f, 1.6f), roundRect(13.5f, 4f, 6.5f, 6.5f, 1.6f),
        roundRect(4f, 13.5f, 6.5f, 6.5f, 1.6f), roundRect(13.5f, 13.5f, 6.5f, 6.5f, 1.6f),
    )
    val Pulse = stroke("pulse", "M22 12h-4l-3 9L9 3l-3 9H2")
    val ChevronRight = stroke("chevron-right", "m9 18 6-6-6-6", width = 2f)
    val Back = stroke("back", "m15 18-6-6 6-6", width = 2.2f)
    val Send = stroke("send", "M22 2 11 13", "M22 2 15 22l-4-9-9-4 20-7z")
    val Refresh = stroke(
        "refresh",
        "M3 12a9 9 0 0 1 15-6.7L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-15 6.7L3 16", "M3 21v-5h5",
    )
    val Trash = stroke("trash", "M3 6h18", "M8 6V4h8v2", "M6 6l1 14h10l1-14")
    val Search = stroke("search", circle(11f, 11f, 7f), "m21 21-4.3-4.3")
    val Close = stroke("close", "M18 6 6 18", "m6 6 12 12")
    val Check = stroke("check", "M20 6 9 17l-5-5", width = 2.6f)
    val Dash = stroke("dash", "M6 12h12", width = 2.6f)
    val Shield = stroke("shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z")
    val Battery = stroke("battery", roundRect(2f, 7f, 17f, 10f, 2f), "M22 11v2", "M6 10v4")
    val Heart = stroke("heart", "M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.8 0-3 .5-4.5 2-1.5-1.5-2.7-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7Z")
    val Power = stroke("power", "M12 2v10", "M18.4 6.6a9 9 0 1 1-12.8 0")
    val Download = stroke("download", "M12 3v12", "m7 10 5 5 5-5", "M5 21h14")
    val Key = stroke("key", circle(7.5f, 15.5f, 5.5f), "m21 2-9.6 9.6", "m15.5 7.5 3 3L22 7l-3-3")
    val Bolt = stroke("bolt", "M13 2 3 14h9l-1 8 10-12h-9l1-8z")
    val Buttons = stroke("buttons", roundRect(2f, 8f, 9f, 8f, 2.5f), roundRect(13f, 8f, 9f, 8f, 2.5f))
    val Wallet = stroke("wallet", roundRect(2f, 5f, 20f, 14f, 3f), "M2 10h20")
    val Ball = stroke(
        "ball",
        circle(12f, 12f, 9f),
        "M12 8.6l3.9 2.8-1.5 4.6h-4.8L8.1 11.4z",
        "M12 8.6V3", "M15.9 11.4l4.4-1.8", "M14.4 16l2.6 3.3",
        "M9.6 16l-2.6 3.3", "M8.1 11.4L3.7 9.6",
    )
    val Truck = stroke(
        "truck",
        roundRect(1f, 3f, 15f, 13f, 2f),
        "M16 8h4l3 3v5h-7V8z",
        "M5.5 21a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
        "M18.5 21a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z",
    )
    val CloudSun = stroke(
        "cloudsun",
        "M12 2v2", "M12 20v2", "M4.93 4.93l1.41 1.41", "M17.66 17.66l1.41 1.41",
        "M2 12h2", "M20 12h2", "M6.34 17.66l-1.41 1.41", "M19.07 4.93l-1.41 1.41",
        "M10.5 8.5a4 4 0 1 0 4 4",
        "M16 5c0 2.5-2 4-4 7H8c0-3-2-4.5-2-7a5 5 0 1 1 10 0z",
    )
    val Package = stroke(
        "package",
        "m7.5 4.27 9 5.15", roundRect(2f, 3f, 20f, 14f, 2f),
        "M12 3v10", "m2 8.27 10 5.73 10-5.73",
    )
    val TrendUp = stroke("trendup", "M22 7 13.5 15.5l-5-5L2 17", "M16 7h6v6")
    val Star = stroke("star", "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
    val History = stroke("history", circle(12f, 12f, 9f), "M12 7v5l3 3")
    val Mail = stroke("mail", "M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z", "M22 6l-10 7L2 6")
    val Discord = stroke("discord", "M19.27 5.33C17.94 4.71 16.5 4.26 15 4c-.18.33-.39.76-.53 1.09a16.09 16.09 0 0 0-4.8 0c-.14-.34-.35-.76-.54-1.09-1.5.26-2.93.71-4.27 1.33C2.15 9.37 1.41 13.31 1.77 17.2a15.8 15.8 0 0 0 5.06 2.58c.4-.55.77-1.13 1.08-1.74a10.9 10.9 0 0 1-1.59-.76c.11-.08.22-.16.32-.25 3.48 1.59 7.24 1.59 10.68 0 .11.08.21.17.32.25a11.25 11.25 0 0 1-1.6.76c.32.61.69 1.19 1.09 1.74a15.8 15.8 0 0 0 5.07-2.58c.44-4.46-.76-8.38-3.15-11.93z")

    val More = filled("more", circle(5f, 12f, 2f), circle(12f, 12f, 2f), circle(19f, 12f, 2f))
    val Stop = filled("stop", roundRect(5f, 5f, 14f, 14f, 3f))
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}Z"

private fun stroke(name: String, vararg paths: String, width: Float = 2f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun filled(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
    }.build()
