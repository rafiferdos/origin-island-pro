package com.originisle.android.cards

import com.originisle.android.island.OriginIslandConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadCardTest {

    @Test
    fun testSpeedExtraction() {
        val text1 = "Downloading update.zip • 45 MB / 120 MB • 2.4 MB/s • 30s left"
        assertEquals("19.2 Mbps", DownloadCard.convertToMbps(text1))
        assertEquals("19.2M", DownloadCard.extractCompactMbps(text1))
        assertEquals("2.4 MB/s", DownloadCard.extractRawSpeedText(text1))

        val text2 = "origin-island.apk: 850 KB/s - 15 seconds remaining"
        assertEquals("6.8 Mbps", DownloadCard.convertToMbps(text2))
        assertEquals("6.8M", DownloadCard.extractCompactMbps(text2))
        assertEquals("850 KB/s", DownloadCard.extractRawSpeedText(text2))

        val text3 = "Streaming asset at 15 Mbps"
        assertEquals("15.0 Mbps", DownloadCard.convertToMbps(text3))
        assertEquals("15.0M", DownloadCard.extractCompactMbps(text3))
        assertEquals("15 Mbps", DownloadCard.extractRawSpeedText(text3))

        val textNoSpeed = "Downloading file without speed info"
        assertNull(DownloadCard.convertToMbps(textNoSpeed))
        assertNull(DownloadCard.extractCompactMbps(textNoSpeed))
        assertNull(DownloadCard.extractRawSpeedText(textNoSpeed))
    }

    @Test
    fun testZeroSpeedRejectionForAntiFlicker() {
        // Dummy 0 B/s or 0.00 KB/s text shouldn't be extracted as active text speed
        val zero1 = "Connecting... 0 B/s"
        assertNull(DownloadCard.convertToMbps(zero1))
        assertNull(DownloadCard.extractRawSpeedText(zero1))

        val zero2 = "Buffering: 0.00 KB/s"
        assertNull(DownloadCard.convertToMbps(zero2))
        assertNull(DownloadCard.extractRawSpeedText(zero2))
    }

    @Test
    fun testDownloadAppRecognition() {
        val chrome = "com.android.chrome"
        val telegram = "org.telegram.messenger"
        val adm = "com.dv.adm"
        val idm = "idm.internet.download.manager"
        val genshin = "com.miHoYo.GenshinImpact"

        assertEquals(true, chrome in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, telegram in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, adm in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, idm in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, genshin in DownloadCard.DOWNLOAD_APPS)
    }

    @Test
    fun testFormatting() {
        assertEquals("24.5 Mbps", DownloadCard.formatMbps(24.5))
        assertEquals("24.5M", DownloadCard.formatCompactMbps(24.5))
        assertEquals("120 Mbps", DownloadCard.formatMbps(120.0))
        assertEquals("120M", DownloadCard.formatCompactMbps(120.0))
        assertEquals("500 Kbps", DownloadCard.formatMbps(0.5))
        assertEquals("500K", DownloadCard.formatCompactMbps(0.5))
    }

    @Test
    fun testOriginOSFormatSpeed() {
        // OriginOS MB/s and KB/s unit formatting
        val speedMbs1 = DownloadCard.formatSpeed(24.0, "mbs") // 24 Mbps = 3.0 MB/s
        assertEquals("3.0 MB/s", speedMbs1)

        val speedMbs2 = DownloadCard.formatSpeed(0.8, "mbs") // 0.8 Mbps = 100 KB/s
        assertEquals("100 KB/s", speedMbs2)

        // Standard Mbps unit formatting
        val speedMbps = DownloadCard.formatSpeed(24.5, "mbps")
        assertEquals("24.5 Mbps", speedMbps)
    }

    @Test
    fun testResolvePillConfigSpeedRing() {
        // Option 1 (Speed & Progress Ring)
        val config = DownloadCard.resolvePillConfig(
            style = "speed_ring",
            displayTitle = "Telegram",
            percent = 30,
            indeterminate = false,
            progressMax = 100,
            liveMbpsSpeed = "12.0 Mbps",
        )
        assertEquals("12.0 Mbps", config.leftContent)
        assertNull(config.leftDoubleLine)
        assertEquals(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS, config.rightTemplate)
        assertEquals("30%", config.rightContent)
        assertEquals("30%", config.statusChip)
        assertEquals(true, config.showProgress)
        assertEquals(30, config.progressVal)
    }

    @Test
    fun testResolvePillConfigDoubleline() {
        // Option 2 (vivo OriginOS Native Dual-Line)
        val config = DownloadCard.resolvePillConfig(
            style = "doubleline",
            displayTitle = "Genshin Impact",
            percent = 45,
            indeterminate = false,
            progressMax = 100,
            liveMbpsSpeed = "24.5 Mbps",
        )
        assertEquals("Genshin Impa", config.leftContent)
        assertNotNull(config.leftDoubleLine)
        assertEquals(listOf("Genshin Impa", "24.5 Mbps"), config.leftDoubleLine)
        assertEquals(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS, config.rightTemplate)
        assertEquals("45%", config.rightContent)
        assertEquals("45%", config.statusChip)
        assertEquals(true, config.showProgress)
        assertEquals(45, config.progressVal)
    }

    @Test
    fun testResolvePillConfigSpeedCapsule() {
        // Option 3 (Speed & % Badge)
        val config = DownloadCard.resolvePillConfig(
            style = "speed_capsule",
            displayTitle = "Google Chrome",
            percent = 60,
            indeterminate = false,
            progressMax = 100,
            liveMbpsSpeed = "18.5 Mbps",
        )
        assertEquals("18.5 Mbps", config.leftContent)
        assertNull(config.leftDoubleLine)
        assertEquals(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, config.rightTemplate)
        assertEquals("60%", config.rightContent)
        assertEquals("60%", config.statusChip)
        assertEquals(true, config.showProgress)
        assertEquals(60, config.progressVal)
    }

    @Test
    fun testResolvePillConfigPercentSpeed() {
        // Option 4 (% & Speed Badge)
        val config = DownloadCard.resolvePillConfig(
            style = "percent_speed",
            displayTitle = "Google Chrome",
            percent = 75,
            indeterminate = false,
            progressMax = 100,
            liveMbpsSpeed = "19.2 Mbps",
        )
        assertEquals("75%", config.leftContent)
        assertNull(config.leftDoubleLine)
        assertEquals(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, config.rightTemplate)
        assertEquals("19.2 Mbps", config.rightContent)
        assertEquals("19.2 Mbps", config.statusChip)
        assertEquals(true, config.showProgress)
        assertEquals(75, config.progressVal)
    }

    @Test
    fun testResolvePillConfigIndeterminate() {
        val config = DownloadCard.resolvePillConfig(
            style = "doubleline",
            displayTitle = "Asset Fetch",
            percent = 0,
            indeterminate = true,
            progressMax = 0,
            liveMbpsSpeed = "0.0 Mbps",
        )
        assertEquals("Asset Fetch", config.leftContent)
        assertEquals(listOf("Asset Fetch", "0.0 Mbps"), config.leftDoubleLine)
        assertEquals(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING, config.rightTemplate)
        assertEquals("...", config.statusChip)
        assertEquals(false, config.showProgress)
    }
}
