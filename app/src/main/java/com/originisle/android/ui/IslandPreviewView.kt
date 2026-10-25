package com.originisle.android.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
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
fun IslandPreviewView(preview: IslandPreview, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(1.dp, IslandRim, CircleShape)
            .padding(horizontal = 6.dp),
        content = { IslandHalves(preview) },
    ) { measurables, constraints ->
        // Each half gets the same width (the wider of the two, within bounds), leaving the camera
        // gap exactly in the middle; the left half hugs the left end, the right half the right end.
        val gap = CameraGap.roundToPx()
        val maxHalf = ((constraints.maxWidth - gap) / 2).coerceAtMost(MaxHalf.roundToPx())
        val loose = Constraints(maxWidth = maxHalf, maxHeight = constraints.maxHeight)
        val (left, rightHalf) = measurables.map { it.measure(loose) }
        val half = maxOf(left.width, rightHalf.width, MinHalf.roundToPx())
        val width = 2 * half + gap
        val height = constraints.maxHeight
        layout(width, height) {
            left.place(0, (height - left.height) / 2)
            rightHalf.place(width - rightHalf.width, (height - rightHalf.height) / 2)
        }
    }
}

private val CameraGap = 34.dp
private val MinHalf = 52.dp
private val MaxHalf = 104.dp

@Composable
private fun IslandHalves(preview: IslandPreview) {
    val right = preview.right
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (right is IslandPreview.Right.Score) {
            Crest()
            IslandText(right.home.toString(), Modifier.padding(start = 10.dp), size = 16)
        } else {
            LeftIcon(preview.icon)
            if (preview.left.isNotEmpty()) IslandText(preview.left, Modifier.padding(start = 8.dp))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (right) {
            is IslandPreview.Right.Score -> {
                IslandText(right.away.toString(), Modifier.padding(end = 10.dp), size = 16)
                Crest()
            }
            is IslandPreview.Right.Capsule -> Box(
                Modifier.height(26.dp).clip(CircleShape).background(CapsuleGrey).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { IslandText(right.text, size = 13, weight = FontWeight.Normal) }
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

private val IslandRim = Color(0xFF2E2E32)
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
