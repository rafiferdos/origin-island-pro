package com.originisle.android.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.R
import com.originisle.android.ui.samples.IslandPreview

/**
 * A static rendering of the compact OriginIsland a sample card produces, drawn to match the real
 * island (see docs/screenshots/island-*.jpg): a black pill with a thin grey rim wrapped around the
 * camera, icon + text pinned to the left end, the right-island template pinned to the right end.
 * Like the real one it sizes to its content, with equal halves so the camera stays centred. System
 * font, as on the device.
 */
@Composable
fun IslandPreviewView(
    preview: IslandPreview,
    modifier: Modifier = Modifier,
    cameraGap: androidx.compose.ui.unit.Dp = CameraGap,
    showCameraHole: Boolean = false,
    cutoutPosition: String = "center",
    safeMargin: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Layout(
        modifier = modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(1.2.dp, IslandRim, CircleShape)
            .padding(horizontal = 6.dp),
        content = {
            IslandLeftHalf(preview)
            IslandRightHalf(preview)
            if (showCameraHole) {
                CameraLensHole()
            }
        },
    ) { measurables, constraints ->
        val isLeftCorner = cutoutPosition == "left"
        val gap = cameraGap.roundToPx()
        val marginPx = safeMargin.roundToPx()

        if (isLeftCorner && showCameraHole && measurables.size > 2) {
            val cameraMeasure = measurables[2].measure(Constraints.fixed(13.dp.roundToPx(), 13.dp.roundToPx()))
            val maxRemaining = ((constraints.maxWidth - cameraMeasure.width - marginPx) / 2)
                .coerceIn(MinHalf.roundToPx(), MaxHalf.roundToPx())
            val loose = Constraints(maxWidth = maxRemaining, maxHeight = constraints.maxHeight)
            val left = measurables[0].measure(loose)
            val rightHalf = measurables[1].measure(loose)

            val width = (cameraMeasure.width + marginPx + left.width + 14.dp.roundToPx() + rightHalf.width)
                .coerceAtMost(constraints.maxWidth)
            val height = constraints.maxHeight

            layout(width, height) {
                // Hole on the left
                cameraMeasure.place(2.dp.roundToPx(), (height - cameraMeasure.height) / 2)
                // Left content starts after the hole + safe margin
                val leftX = 2.dp.roundToPx() + cameraMeasure.width + marginPx
                left.place(leftX, (height - left.height) / 2)
                // Right half hugs the right edge
                rightHalf.place(width - rightHalf.width, (height - rightHalf.height) / 2)
            }
        } else {
            val effectiveGap = if (showCameraHole) maxOf(gap, 13.dp.roundToPx() + 2 * marginPx) else gap
            val maxHalf = ((constraints.maxWidth - effectiveGap) / 2).coerceAtMost(MaxHalf.roundToPx())
            val loose = Constraints(maxWidth = maxHalf, maxHeight = constraints.maxHeight)
            val left = measurables[0].measure(loose)
            val rightHalf = measurables[1].measure(loose)
            val cameraMeasure = if (showCameraHole && measurables.size > 2) {
                measurables[2].measure(Constraints.fixed(13.dp.roundToPx(), 13.dp.roundToPx()))
            } else null

            val half = maxOf(left.width, rightHalf.width, MinHalf.roundToPx())
            val width = (2 * half + effectiveGap).coerceAtMost(constraints.maxWidth)
            val height = constraints.maxHeight

            layout(width, height) {
                left.place(0, (height - left.height) / 2)
                rightHalf.place(width - rightHalf.width, (height - rightHalf.height) / 2)
                cameraMeasure?.place((width - cameraMeasure.width) / 2, (height - cameraMeasure.height) / 2)
            }
        }
    }
}

/**
 * Realistic front camera punch-hole lens with anti-reflective optical sapphire reflection.
 */
@Composable
fun CameraLensHole(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 13.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF141720))
            .border(1.2.dp, Color(0xFF475569), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Inner optical lens aperture
        Box(
            modifier = Modifier
                .size(size - 3.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF030712),
                        )
                    )
                )
        )
        // Sapphire anti-reflective reflection highlight
        Box(
            modifier = Modifier
                .size(3.5.dp)
                .align(Alignment.TopEnd)
                .padding(top = 1.dp, end = 1.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.9f))
        )
    }
}

private val CameraGap = 34.dp
private val MinHalf = 52.dp
private val MaxHalf = 104.dp

@Composable
private fun IslandLeftHalf(preview: IslandPreview) {
    val right = preview.right
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (right is IslandPreview.Right.Score) {
            Crest()
            IslandText(right.home.toString(), Modifier.padding(start = 10.dp), size = 16)
        } else if (preview.leftDoubleLine != null && preview.leftDoubleLine.size == 2) {
            LeftIcon(preview.icon)
            Column(Modifier.padding(start = 6.dp), verticalArrangement = Arrangement.Center) {
                IslandText(preview.leftDoubleLine[0], size = 10, weight = FontWeight.SemiBold)
                IslandText(preview.leftDoubleLine[1], size = 9, weight = FontWeight.Normal, color = IsleColors.TextSecondary)
            }
        } else {
            LeftIcon(preview.icon)
            if (preview.left.isNotEmpty()) IslandText(preview.left, Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun IslandRightHalf(preview: IslandPreview) {
    val right = preview.right
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (right) {
            is IslandPreview.Right.Score -> {
                IslandText(right.away.toString(), Modifier.padding(end = 10.dp), size = 16)
                Crest()
            }
            is IslandPreview.Right.Capsule -> {
                val cleanText = right.text.trim().removePrefix("✦").removePrefix("✨").trim()
                if (cleanText.equals("View", ignoreCase = true)) {
                    GeminiViewBadge("View")
                } else {
                    Box(
                        Modifier.height(26.dp).clip(CircleShape).background(CapsuleGrey).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { IslandText(right.text, size = 13, weight = FontWeight.Normal) }
                }
            }
            is IslandPreview.Right.Progress -> ProgressRing(right.fraction)
            IslandPreview.Right.Loading -> LoadingDots()
            IslandPreview.Right.Wave -> WaveBars()
            is IslandPreview.Right.Text -> IslandText(right.text, Modifier.padding(end = 6.dp))
            IslandPreview.Right.Success -> Box(
                Modifier.size(26.dp).clip(CircleShape).background(SuccessGreen),
                contentAlignment = Alignment.Center,
            ) { Icon(IsleIcons.Check, null, Modifier.size(15.dp), tint = Color.White) }
            IslandPreview.Right.None -> Unit
        }
    }
}

/**
 * Google Gemini signature live animated iridescent chromatic glow badge.
 * Features:
 * - Ambient animated bloom halo breathing behind the capsule
 * - Full badge background filled with live flowing Aurora/Gemini colors (Cyan → Indigo → Purple → Coral Pink)
 * - Animated glowing border that sweeps with the flow
 * - Frosted glass luster glaze with luminous white typography and pulsing ✦ star
 */
@Composable
fun GeminiViewBadge(
    text: String = "View",
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_live")

    // Continuous flow animation: 0f to 1f over 3200ms
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gemini_flow",
    )

    // Breathing pulse for ambient glow halo: 0.35f to 0.75f over 1600ms
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gemini_halo",
    )

    // Cosmic sparkle pulse: 0.85f to 1.25f
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gemini_star",
    )

    // Core Gemini Palette
    val c0 = Color(0xFF00E5FF) // Electric Cyan
    val c1 = Color(0xFF6366F1) // Royal Indigo
    val c2 = Color(0xFFA855F7) // Celestial Purple
    val c3 = Color(0xFFFF2A85) // Sunset Magenta / Neon Pink
    val c4 = Color(0xFFFB7185) // Cosmic Coral

    val palette = listOf(c0, c1, c2, c3, c4, c0, c1)
    val startOffset = flowPhase * 180f

    val flowingBrush = Brush.linearGradient(
        colors = palette,
        start = androidx.compose.ui.geometry.Offset(startOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(startOffset + 240f, 60f),
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            c0,
            c2,
            c3,
            Color.White.copy(alpha = 0.85f),
        ),
        start = androidx.compose.ui.geometry.Offset(startOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(startOffset + 200f, 50f),
    )

    Box(
        modifier = modifier.height(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 1. Ambient Bloom Halo behind the capsule
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.4f
                    alpha = haloPulse
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            c2.copy(alpha = 0.7f),
                            c0.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    )
                )
        )

        // 2. Main Glowing Capsule (The ENTIRE background glows!)
        Box(
            modifier = Modifier
                .height(26.dp)
                .clip(CircleShape)
                // Flowing Gemini animated background
                .background(flowingBrush)
                // Flowing luminous border
                .border(1.2.dp, borderBrush, CircleShape)
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 3. Frosted Glass Luster Highlight overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f),
                            )
                        )
                    )
            )

            // 4. Content: Pulsing ✦ Sparkle Star + Luminous High-Contrast Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.5.dp),
            ) {
                Text(
                    text = "✦",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    },
                )
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                )
            }
        }
    }
}

private val IslandRim = Color(0xFF383C48)
private val CapsuleGrey = Color(0xFF2C2C2E)
private val IconTileGrey = Color(0xFF1C1C1E)
private val SuccessGreen = Color(0xFF34C759)
/** Refined Electric Sapphire for progress rings (replacing harsh royal blue). */
private val ProgressBlue = Color(0xFF2D6BF5)

@Composable
private fun IslandText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 13,
    weight: FontWeight = FontWeight.SemiBold,
    color: Color = Color.White,
) {
    Text(
        text,
        modifier = modifier,
        fontFamily = FontFamily.Default,
        fontSize = size.sp,
        fontWeight = weight,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LeftIcon(res: Int) {
    // A contact avatar is a full-colour round picture, like on the real island.
    if (res == R.drawable.ic_avatar) {
        Icon(painterResource(res), null, Modifier.size(24.dp).clip(CircleShape), tint = Color.Unspecified)
        return
    }
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(IconTileGrey),
        contentAlignment = Alignment.Center,
    ) {
        val tint = when (res) {
            R.drawable.ic_mail -> Color(0xFFEA4335)
            R.drawable.ic_discord -> Color(0xFF5865F2)
            R.drawable.ic_payment -> Color.Unspecified
            R.drawable.ic_call -> IsleColors.TileCall
            R.drawable.ic_call_end -> IsleColors.Bad
            R.drawable.ic_download -> IsleColors.TileDownload
            R.drawable.ic_charging -> IsleColors.Good
            R.drawable.ic_car -> IsleColors.TileRide
            R.drawable.ic_calendar -> IsleColors.TileCalendar
            R.drawable.ic_timer -> IsleColors.TileTimer
            else -> Color.White
        }
        Icon(painterResource(res), null, Modifier.size(15.dp), tint = tint)
    }
}

/** The sample's generic badge (a ball), where a real match shows the club crest. */
@Composable
private fun Crest() {
    Icon(painterResource(R.drawable.ic_soccer), null, Modifier.size(24.dp), tint = Color.White)
}

@Composable
private fun ProgressRing(fraction: Float) {
    val animatedFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "progress_fraction",
    )
    Canvas(Modifier.padding(end = 3.dp).size(22.dp)) {
        val stroke = 2.8.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(CapsuleGrey, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(ProgressBlue, -90f, 360f * animatedFraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun LoadingDots() {
    val t by rememberInfiniteTransition(label = "dots").animateFloat(
        0f, 3f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "dots",
    )
    Row(Modifier.padding(end = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val on = t.toInt() == i
            Box(Modifier.size(6.dp).alpha(if (on) 1f else 0.35f).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun WaveBars() {
    val t by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "wave",
    )
    Row(
        Modifier.padding(end = 6.dp).height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(0.45f, 0.9f, 0.6f, 1f, 0.5f).forEachIndexed { i, base ->
            val h = if (i % 2 == 0) base * (0.5f + 0.5f * t) else base * (1f - 0.5f * t)
            Box(Modifier.width(3.dp).fillMaxHeight(h.coerceIn(0.2f, 1f)).clip(CircleShape).background(Color.White))
        }
    }
}
