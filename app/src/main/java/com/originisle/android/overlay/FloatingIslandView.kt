package com.originisle.android.overlay

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.R
import kotlin.math.roundToInt

@Composable
fun FloatingIslandRoot(
    card: OverlayCardData?,
    mode: OverlayVisualMode,
    glassStyle: String = "liquid",
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (card == null || mode == OverlayVisualMode.HIDDEN) return

    val isExpanded = mode == OverlayVisualMode.EXPANDED

    // Dismiss drag gestures
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(isExpanded) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -80f) {
                            if (isExpanded) onCollapse() else onDismiss()
                        } else if (kotlin.math.abs(offsetX) > 120f) {
                            onDismiss()
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        if (!isExpanded) {
            FloatingCompactPill(
                card = card,
                glassStyle = glassStyle,
                onClick = onExpand,
            )
        } else {
            FloatingExpandedCard(
                card = card,
                glassStyle = glassStyle,
                onCollapse = onCollapse,
            )
        }
    }
}

/** Compact cutout pill */
@Composable
fun FloatingCompactPill(
    card: OverlayCardData,
    glassStyle: String,
    onClick: () -> Unit,
) {
    val pillModifier = when (glassStyle) {
        "frosted" -> Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0x88181824)) // 50% translucent frosted dark glass
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f))), RoundedCornerShape(percent = 50))
        "liquid" -> Modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(percent = 50), ambientColor = Color(0xFF2D6BF5), spotColor = Color(0xFF2D6BF5))
            .clip(RoundedCornerShape(percent = 50))
            .background(Brush.verticalGradient(listOf(Color(0x882A2A42), Color(0x66141422)))) // Highly translucent liquid glass
            .border(1.4.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color(0x882D6BF5), Color.White.copy(alpha = 0.18f))), RoundedCornerShape(percent = 50))
        else -> Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF2A2A32), RoundedCornerShape(percent = 50))
    }

    Row(
        modifier = pillModifier
            .height(36.dp)
            .widthIn(min = 140.dp, max = 240.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (card.iconBitmap != null) {
                Image(
                    bitmap = card.iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(if (card.iconRes != 0) card.iconRes else R.mipmap.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            }

            // Left text snippet
            val leftLabel = card.leftContent.ifBlank { card.title }
            Text(
                text = leftLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 100.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Right indicator
        if (card.waveState > 0) {
            FloatingSoundwave(colors = card.waveColors)
        } else if (card.progress >= 0) {
            Text(
                text = "${card.progress}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
            )
        } else if (card.statusChip.isNotBlank() || card.rightContent.isNotBlank()) {
            val chipText = card.statusChip.ifBlank { card.rightContent }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = chipText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Full Expanded Card with Apple Liquid Glass styling */
@Composable
fun FloatingExpandedCard(
    card: OverlayCardData,
    glassStyle: String,
    onCollapse: () -> Unit,
) {
    val cardModifier = when (glassStyle) {
        "frosted" -> Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0x66181826)) // Genuine 40% translucent frosted dark glass
            .border(1.2.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.12f))), RoundedCornerShape(26.dp))
            .padding(16.dp)
        "liquid" -> Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(if (card.lightColor != 0) card.lightColor else 0xFF2D6BF5.toInt()),
                spotColor = Color(if (card.lightColor != 0) card.lightColor else 0xFF2D6BF5.toInt())
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x552A2A44), // Translucent liquid glass layer (allows wallpaper/app beneath to shine through)
                        Color(0x35141424)
                    )
                )
            )
            .border(
                1.6.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.90f), // High-intensity specular highlight glass sheen
                        Color(0x882D6BF5),               // Electric sapphire refraction
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .padding(16.dp)
        else -> Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16161A))
            .border(1.dp, Color(0xFF282830), RoundedCornerShape(26.dp))
            .padding(16.dp)
    }

    Column(
        modifier = cardModifier.animateContentSize(spring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: App identity + header + collapse chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (card.iconBitmap != null) {
                    Image(
                        bitmap = card.iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(if (card.iconRes != 0) card.iconRes else R.mipmap.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Column {
                    Text(
                        text = card.title.ifBlank { "Notification" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.sourceApp.ifBlank { card.subtext.ifBlank { "Origin Island Pro" } },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
            }

            // Collapse pill / chip
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onCollapse() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close ✕",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Body message
        if (card.text.isNotBlank()) {
            Text(
                text = card.text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Progress bar (for downloads and timers)
        if (card.progress >= 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { card.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(percent = 50)),
                    color = Color(0xFF2D6BF5),
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.leftContent.ifBlank { "Progress" },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    Text(
                        text = "${card.progress}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Soundwave (for music)
        if (card.waveState > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Now Playing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                FloatingSoundwave(colors = card.waveColors, barCount = 7)
            }
        }

        // Action Buttons (Calls, OTPs, Actions)
        if (card.buttonTitles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card.buttonTitles.forEachIndexed { index, title ->
                    val bgColorInt = card.buttonColors.getOrNull(index) ?: 0xFF2A2A36.toInt()
                    val fgColorInt = card.buttonTextColors.getOrNull(index) ?: 0xFFFFFFFF.toInt()
                    val pi = card.buttonIntents.getOrNull(index)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(bgColorInt))
                            .clickable {
                                runCatching { pi?.send() }
                                onCollapse()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(fgColorInt)
                        )
                    }
                }
            }
        }
    }
}

/** Animated multi-color soundwave bars */
@Composable
fun FloatingSoundwave(
    colors: List<String>,
    barCount: Int = 4,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    val heights = (0 until barCount).map { idx ->
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400 + idx * 120, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$idx"
        )
    }

    val defaultPalette = listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF22C55E))

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp)
    ) {
        heights.forEachIndexed { idx, animValue ->
            val color = if (colors.isNotEmpty()) {
                val hex = colors[idx % colors.size]
                try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    defaultPalette[idx % defaultPalette.size]
                }
            } else {
                defaultPalette[idx % defaultPalette.size]
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((animValue.value * 16).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color)
            )
        }
    }
}
