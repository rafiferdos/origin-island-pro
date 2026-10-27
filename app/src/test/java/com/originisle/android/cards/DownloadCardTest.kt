package com.originisle.android.cards

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

        val text2 = "origin-island.apk: 850 KB/s - 15 seconds remaining"
        assertEquals("6.8 Mbps", DownloadCard.convertToMbps(text2))
        assertEquals("6.8M", DownloadCard.extractCompactMbps(text2))

        val text3 = "Streaming asset at 15 Mbps"
        assertEquals("15.0 Mbps", DownloadCard.convertToMbps(text3))
        assertEquals("15.0M", DownloadCard.extractCompactMbps(text3))

        val textNoSpeed = "Downloading file without speed info"
        assertNull(DownloadCard.convertToMbps(textNoSpeed))
        assertNull(DownloadCard.extractCompactMbps(textNoSpeed))
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
}
