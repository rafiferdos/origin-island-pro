package com.originisle.android.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.originisle.android.MainActivity
import com.originisle.android.R
import com.originisle.android.service.UpdateCheckReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Intelligent In-App Update Engine for Origin Island Pro.
 * Features:
 * - Daily automated background update checks via AlarmManager and app lifecycle.
 * - High-priority system notifications when a new version is released.
 * - Direct in-app APK download with real-time speed, bytes, and progress animation.
 * - Zero external browser redirects to GitHub for downloading: downloads and installs smoothly inside the app.
 * - Seamless installation via Android FileProvider and PackageInstaller.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/rafiferdos/origin-island-pro/releases/latest"

    const val UPDATE_CHANNEL_ID = "originisle_app_updates"
    const val NOTIF_UPDATE_AVAILABLE_ID = 9901
    const val NOTIF_DOWNLOAD_PROGRESS_ID = 9902
    const val NOTIF_DOWNLOAD_COMPLETE_ID = 9903
    const val ALARM_REQUEST_CODE = 9905

    const val EXTRA_OPEN_UPDATE_DIALOG = "extra_open_update_dialog"
    const val EXTRA_START_DOWNLOAD = "extra_start_download"

    @Volatile
    private var isDownloadCancelled = false

    data class AppUpdateInfo(
        val latestVersion: String,
        val releaseTitle: String,
        val releaseNotes: String,
        val apkUrl: String,
        val apkSize: Long,
        val apkName: String,
        val releaseUrl: String,
    )

    sealed class Result {
        data class UpToDate(val current: String) : Result()
        data class UpdateAvailable(val info: AppUpdateInfo) : Result()
        data class Error(val message: String) : Result()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val progress: Float,
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val speedBytesPerSec: Long,
        ) : DownloadState()
        data class Completed(val apkFile: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    /**
     * Checks GitHub's Releases API for a newer version.
     */
    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        val current = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: return@withContext Result.Error("Unknown installed version")

        try {
            val conn = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "OriginIslandPro-Updater")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode != 200) {
                return@withContext Result.Error("Server returned code ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.getString("tag_name").removePrefix("v").trim()
            val title = json.optString("name", "Origin Island Pro v$tag")
            val notes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var apkSize = 0L
            var apkName = "origin-island-pro-$tag.apk"

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        apkName = name
                        break
                    }
                }
            }

            if (apkUrl.isBlank()) {
                apkUrl = "https://github.com/rafiferdos/origin-island-pro/releases/download/v$tag/origin-island-pro-$tag.apk"
            }

            val info = AppUpdateInfo(
                latestVersion = tag,
                releaseTitle = title,
                releaseNotes = notes,
                apkUrl = apkUrl,
                apkSize = apkSize,
                apkName = apkName,
                releaseUrl = htmlUrl,
            )

            if (isNewer(tag, current)) {
                Result.UpdateAvailable(info)
            } else {
                Result.UpToDate(current)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            Result.Error("Couldn't check for updates")
        }
    }

    /** "2.31.0" > "2.30.0" — compares numeric dot-separated parts. */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /**
     * Downloads the APK file directly in the app, streaming data with real-time progress callbacks.
     * Follows HTTP 302/301 redirects from GitHub to Azure Blob storage.
     */
    suspend fun downloadApk(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (DownloadState) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        isDownloadCancelled = false
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, info.apkName)

        // Check if an identical completed APK already exists
        if (targetFile.exists() && info.apkSize > 0 && targetFile.length() == info.apkSize) {
            onProgress(DownloadState.Completed(targetFile))
            postDownloadCompleteNotification(context, targetFile, info.latestVersion)
            return@withContext targetFile
        }

        try {
            var currentUrl = info.apkUrl
            var conn: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 5) {
                val u = URL(currentUrl)
                conn = (u.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 25000
                    setRequestProperty("User-Agent", "OriginIslandPro-Updater")
                    setRequestProperty("Accept", "application/octet-stream")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc != null) {
                        currentUrl = loc
                        redirects++
                        continue
                    }
                }
                break
            }

            if (conn == null || conn.responseCode !in 200..299) {
                val err = "Download failed: HTTP ${conn?.responseCode ?: -1}"
                Log.e(TAG, err)
                onProgress(DownloadState.Error(err))
                return@withContext null
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(16 * 1024)
            var bytesReadTotal = 0L
            var lastProgressTime = System.currentTimeMillis()
            var bytesAtLastSample = 0L
            var speedBytesPerSec = 0L

            createNotificationChannel(context)

            inputStream.use { input ->
                outputStream.use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        if (isDownloadCancelled) {
                            targetFile.delete()
                            cancelNotification(context, NOTIF_DOWNLOAD_PROGRESS_ID)
                            onProgress(DownloadState.Idle)
                            return@withContext null
                        }
                        output.write(buffer, 0, n)
                        bytesReadTotal += n

                        val now = System.currentTimeMillis()
                        val delta = now - lastProgressTime
                        if (delta >= 150) {
                            speedBytesPerSec = ((bytesReadTotal - bytesAtLastSample) * 1000L) / delta.coerceAtLeast(1)
                            bytesAtLastSample = bytesReadTotal
                            lastProgressTime = now

                            val progress = if (totalBytes > 0) {
                                (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            onProgress(
                                DownloadState.Downloading(
                                    progress = progress,
                                    bytesDownloaded = bytesReadTotal,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = speedBytesPerSec,
                                )
                            )
                            postDownloadProgressNotification(context, (progress * 100).toInt(), bytesReadTotal, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            cancelNotification(context, NOTIF_DOWNLOAD_PROGRESS_ID)
            onProgress(DownloadState.Completed(targetFile))
            postDownloadCompleteNotification(context, targetFile, info.latestVersion)
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            targetFile.delete()
            cancelNotification(context, NOTIF_DOWNLOAD_PROGRESS_ID)
            onProgress(DownloadState.Error(e.message ?: "Network error during download"))
            null
        }
    }

    fun cancelDownload() {
        isDownloadCancelled = true
    }

    /**
     * Launches the native Android Package Installer for the downloaded APK file.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK file not found on disk", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please allow permission to install app updates", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Checks daily in background. If an update is available, triggers a system notification.
     */
    suspend fun checkDailyIfNeeded(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDailyEnabled = prefs.getBoolean("auto_check_updates_daily", true)
        if (!isDailyEnabled && !force) return

        val lastCheck = prefs.getLong("last_update_check_time", 0L)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        if (!force && now - lastCheck < oneDayMs) return

        prefs.edit().putLong("last_update_check_time", now).apply()

        when (val res = check(context)) {
            is Result.UpdateAvailable -> {
                Log.i(TAG, "Daily update check: v${res.info.latestVersion} available!")
                // Save cached update info in prefs
                prefs.edit()
                    .putString("cached_update_version", res.info.latestVersion)
                    .putString("cached_update_title", res.info.releaseTitle)
                    .putString("cached_update_notes", res.info.releaseNotes)
                    .putString("cached_update_apk_url", res.info.apkUrl)
                    .putLong("cached_update_apk_size", res.info.apkSize)
                    .putString("cached_update_apk_name", res.info.apkName)
                    .putString("cached_update_release_url", res.info.releaseUrl)
                    .apply()

                postUpdateAvailableNotification(context, res.info)
            }
            is Result.UpToDate -> {
                prefs.edit().remove("cached_update_version").apply()
            }
            else -> Unit
        }
    }

    /**
     * Schedules a recurring 24-hour background alarm for update checking.
     */
    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, UpdateCheckReceiver::class.java).apply {
            action = UpdateCheckReceiver.ACTION_DAILY_UPDATE_CHECK
        }
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextTrigger = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            nextTrigger,
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(UPDATE_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for Origin Island Pro app updates"
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun postUpdateAvailableNotification(context: Context, info: AppUpdateInfo) {
        createNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPending = PendingIntent.getActivity(
            context,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val downloadIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            putExtra(EXTRA_START_DOWNLOAD, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val downloadPending = PendingIntent.getActivity(
            context,
            1002,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notesSummary = info.releaseNotes.lines().take(4).joinToString("\n").ifBlank {
            "A newer, faster, and more refined version of Origin Island Pro is available."
        }

        val notif = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Origin Island Pro v${info.latestVersion} Available")
            .setContentText("Tap to download and install update in-app.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("New release: ${info.releaseTitle}\n\n$notesSummary")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_download, "Download Update", downloadPending)
            .build()

        nm.notify(NOTIF_UPDATE_AVAILABLE_ID, notif)
    }

    private fun postDownloadProgressNotification(context: Context, percent: Int, downloaded: Long, total: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPending = PendingIntent.getActivity(
            context,
            1003,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading Origin Island Pro…")
            .setContentText("${formatBytes(downloaded)} / ${formatBytes(total)} (${percent}%)")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .build()

        nm.notify(NOTIF_DOWNLOAD_PROGRESS_ID, notif)
    }

    private fun postDownloadCompleteNotification(context: Context, apkFile: File, version: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentUri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        }.getOrNull()

        val installIntent = if (contentUri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            }
        }

        val pending = PendingIntent.getActivity(
            context,
            1004,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download Complete: v$version")
            .setContentText("Tap to install update immediately.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .addAction(R.drawable.ic_download, "Install Now", pending)
            .build()

        nm.notify(NOTIF_DOWNLOAD_COMPLETE_ID, notif)
    }

    private fun cancelNotification(context: Context, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        val mb = bytesPerSec.toDouble() / (1024 * 1024)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB/s", mb)
        } else {
            val kb = bytesPerSec.toDouble() / 1024
            String.format(Locale.US, "%.0f KB/s", kb)
        }
    }
}
