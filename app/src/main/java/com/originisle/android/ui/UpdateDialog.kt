package com.originisle.android.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File

/**
 * Sleek in-app update presentation dialog for Origin Island Pro.
 * Provides real-time download streaming with smooth animations, speed metrics,
 * and one-tap installation via Android package installer without external browser redirects.
 */
@Composable
fun UpdateDialog(
    context: Context,
    info: UpdateChecker.AppUpdateInfo,
    startDownloadImmediately: Boolean = false,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<UpdateChecker.DownloadState>(UpdateChecker.DownloadState.Idle) }

    // Pre-check if APK is already downloaded
    LaunchedEffect(info) {
        val updatesDir = File(context.cacheDir, "updates")
        val localFile = File(updatesDir, info.apkName)
        if (localFile.exists() && info.apkSize > 0 && localFile.length() == info.apkSize) {
            downloadState = UpdateChecker.DownloadState.Completed(localFile)
        } else if (startDownloadImmediately) {
            coroutineScope.launch {
                UpdateChecker.downloadApk(context, info) { state ->
                    downloadState = state
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (downloadState is UpdateChecker.DownloadState.Downloading) {
                UpdateChecker.cancelDownload()
            }
            onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(IsleColors.Surface)
                .border(1.dp, IsleColors.SurfaceHigher, RoundedCornerShape(26.dp))
                .padding(22.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Top Header with animated icon
                HeaderSection(info = info, downloadState = downloadState)

                // Version details chip row
                VersionPillRow(context = context, info = info)

                // Release notes area (collapsible/scrollable)
                ReleaseNotesCard(notes = info.releaseNotes)

                // Interactive download progress and action area
                DownloadActionArea(
                    context = context,
                    info = info,
                    downloadState = downloadState,
                    onStartDownload = {
                        coroutineScope.launch {
                            UpdateChecker.downloadApk(context, info) { state ->
                                downloadState = state
                            }
                        }
                    },
                    onCancelDownload = {
                        UpdateChecker.cancelDownload()
                        downloadState = UpdateChecker.DownloadState.Idle
                    },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    info: UpdateChecker.AppUpdateInfo,
    downloadState: UpdateChecker.DownloadState,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    val iconColor = when (downloadState) {
        is UpdateChecker.DownloadState.Completed -> IsleColors.Good
        is UpdateChecker.DownloadState.Error -> IsleColors.Bad
        is UpdateChecker.DownloadState.Downloading -> IsleColors.Accent
        else -> IsleColors.Accent
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(iconColor.copy(alpha = if (downloadState is UpdateChecker.DownloadState.Downloading) glowAlpha * 0.25f else 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (downloadState) {
                is UpdateChecker.DownloadState.Completed -> IsleIcons.Check
                is UpdateChecker.DownloadState.Downloading -> IsleIcons.Download
                else -> IsleIcons.Sparkles
            },
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
    }

    Text(
        text = when (downloadState) {
            is UpdateChecker.DownloadState.Completed -> "Update Ready to Install"
            is UpdateChecker.DownloadState.Downloading -> "Downloading Update…"
            is UpdateChecker.DownloadState.Error -> "Download Interrupted"
            else -> "New Version Available"
        },
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        color = IsleColors.Text,
    )
}

@Composable
private fun VersionPillRow(
    context: Context,
    info: UpdateChecker.AppUpdateInfo,
) {
    val currentVersion = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrDefault("2.31.0")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(IsleColors.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "v$currentVersion",
            fontFamily = JakartaSans,
            fontSize = 13.sp,
            color = IsleColors.TextSecondary,
        )
        Text(
            text = "→",
            fontFamily = JakartaSans,
            fontSize = 13.sp,
            color = IsleColors.TextTertiary,
        )
        Text(
            text = "v${info.latestVersion}",
            fontFamily = JakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = IsleColors.AccentText,
        )
        if (info.apkSize > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(IsleColors.GoodBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = UpdateChecker.formatBytes(info.apkSize),
                    fontFamily = JakartaSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IsleColors.GoodText,
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesCard(notes: String) {
    val cleanNotes = notes.lines()
        .filter { it.isNotBlank() }
        .joinToString("\n") { line ->
            line.trim()
                .removePrefix("### ")
                .removePrefix("## ")
                .removePrefix("# ")
        }
        .ifBlank { "Performance optimizations and stability improvements for Origin Island Pro." }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(IsleColors.SurfaceHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "What's New",
            fontFamily = JakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = IsleColors.TextSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 130.dp)
                .verticalScroll(scrollState),
        ) {
            Text(
                text = cleanNotes,
                fontFamily = JakartaSans,
                fontSize = 12.5.sp,
                color = IsleColors.TextChip,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun DownloadActionArea(
    context: Context,
    info: UpdateChecker.AppUpdateInfo,
    downloadState: UpdateChecker.DownloadState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedContent(
        targetState = downloadState,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "download_content",
    ) { state ->
        when (state) {
            is UpdateChecker.DownloadState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Download Update Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IsleColors.Accent, Color(0xFF4C82FF))
                                )
                            )
                            .clickable(onClick = onStartDownload)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = IsleIcons.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Download & Install Update",
                                fontFamily = JakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    }

                    // Later button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(IsleColors.SurfaceHigh)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Remind Me Later",
                            fontFamily = JakartaSans,
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                }
            }

            is UpdateChecker.DownloadState.Downloading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(150, easing = LinearEasing),
                    label = "smooth_progress",
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(IsleColors.SurfaceHigher),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IsleColors.Accent, Color(0xFF68B2FF))
                                    )
                                ),
                        )
                    }

                    // Progress indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            fontFamily = JakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = IsleColors.Text,
                        )
                        Text(
                            text = "${UpdateChecker.formatBytes(state.bytesDownloaded)} / ${UpdateChecker.formatBytes(state.totalBytes)}",
                            fontFamily = JakartaSans,
                            fontSize = 12.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IsleColors.Accent.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "⚡ ${UpdateChecker.formatSpeed(state.speedBytesPerSec)}",
                                fontFamily = JakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = IsleColors.AccentText,
                            )
                        }
                    }

                    // Cancel button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IsleColors.SurfaceHigh)
                            .clickable(onClick = onCancelDownload)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel Download",
                            fontFamily = JakartaSans,
                            fontSize = 12.5.sp,
                            color = IsleColors.BadText,
                        )
                    }
                }
            }

            is UpdateChecker.DownloadState.Completed -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF28A745), Color(0xFF34C759))
                                )
                            )
                            .clickable {
                                UpdateChecker.installApk(context, state.apkFile)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = IsleIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Install Update Now",
                                fontFamily = JakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(IsleColors.SurfaceHigh)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Close",
                            fontFamily = JakartaSans,
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                }
            }

            is UpdateChecker.DownloadState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.message,
                        fontFamily = JakartaSans,
                        fontSize = 12.sp,
                        color = IsleColors.BadText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(IsleColors.Accent)
                            .clickable(onClick = onStartDownload)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Retry Download",
                            fontFamily = JakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
