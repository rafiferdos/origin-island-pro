package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadCardTest {

    @Test
    fun testSpeedExtraction() {
        val text1 = "Downloading update.zip • 45 MB / 120 MB • 2.4 MB/s • 30s left"
        assertEquals("2.4MB/s", DownloadCard.extractSpeed(text1))
        assertEquals("2.4M/s", DownloadCard.extractCompactSpeed(text1))

        val text2 = "origin-island.apk: 850 KB/s - 15 seconds remaining"
        assertEquals("850KB/s", DownloadCard.extractSpeed(text2))
        assertEquals("850K/s", DownloadCard.extractCompactSpeed(text2))

        val textNoSpeed = "Downloading file without speed info"
        assertNull(DownloadCard.extractSpeed(textNoSpeed))
        assertNull(DownloadCard.extractCompactSpeed(textNoSpeed))
    }

    @Test
    fun testDownloadAppRecognition() {
        val chrome = "com.android.chrome"
        val telegram = "org.telegram.messenger"
        val adm = "com.dv.adm"
        val idm = "idm.internet.download.manager"

        assertEquals(true, chrome in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, telegram in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, adm in DownloadCard.DOWNLOAD_APPS)
        assertEquals(true, idm in DownloadCard.DOWNLOAD_APPS)
    }
}
