package com.originisle.android.ui

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.SceneCapability
import com.originisle.android.log.CastLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Plain-text device report (plan task T3.6), copied from the overflow menu, so a device check is
 * one paste: platform, identity, island API availability, per-scene grant answers, permissions and
 * the last cast decisions.
 */
object Diagnostics {
    fun report(context: Context): String =
        buildString {
            val pm = context.packageManager
            val version = runCatching { pm.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            appendLine("Origin Island Pro diagnostics")
            appendLine("app: ${context.packageName} v$version")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            appendLine("os: ${systemProperty("ro.vivo.os.version")} / ${systemProperty("ro.vivo.os.build.display.id")}")
            appendLine("setSuperXInfosSceneList: ${hasMethod("setSuperXInfosSceneList", 4)}")
            appendLine("getSceneStatus: ${hasMethod("getSceneStatus", 2)}")
            appendLine("listener enabled: ${isListenerEnabled(context)}")
            appendLine("keep-alive accessibility: ${isAccessibilityEnabled(context)}")
            appendLine("battery unrestricted: ${isBatteryUnrestricted(context)}")
            appendLine("scenes (probe answer: true / false / unknown):")
            OriginIslandConstants.SUPERX_SCENES.forEach { scene ->
                appendLine("  $scene = ${SceneCapability.probe(context, scene) ?: "unknown"}")
            }
            appendLine("last decisions:")
            val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
            CastLog.entries.take(20).forEach { e ->
                appendLine("  ${clock.format(Date(e.time))} ${e.pkg.ifEmpty { e.app }} | ${e.outcome}")
            }
        }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Origin Island diagnostics", report(context)))
    }

    private fun hasMethod(
        name: String,
        params: Int,
    ): Boolean = NotificationManager::class.java.methods.any { it.name == name && it.parameterCount == params }

    private fun systemProperty(key: String): String =
        runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as String
        }.getOrNull()?.ifBlank { null } ?: "unknown"
}
