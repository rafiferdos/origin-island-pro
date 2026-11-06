package com.originisle.android.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * JVM-testable parts of [ApkVerifier]. The certificate comparison itself needs an Android
 * [android.content.pm.Signature], so it is exercised on-device (see docs/DEV.md for the manual
 * `apksigner verify --print-certs` check); everything that can be asserted here is.
 */
class ApkVerifierTest {
    // --- redirect allowlist ---------------------------------------------------------------

    @Test
    fun `github hosts are allowed redirect targets`() {
        assertTrue(ApkVerifier.isAllowedRedirectHost("https://github.com/rafiferdos/origin-island-pro/releases"))
        assertTrue(
            ApkVerifier.isAllowedRedirectHost(
                "https://objects.githubusercontent.com/release-asset-1?token=ABC",
            ),
        )
        assertTrue(
            ApkVerifier.isAllowedRedirectHost(
                "https://release-assets.githubusercontent.com/abc/origin-island-pro.apk",
            ),
        )
        assertTrue(ApkVerifier.isAllowedRedirectHost("https://api.github.com/repos/rafiferdos/origin-island-pro"))
    }

    @Test
    fun `off-host redirects are refused`() {
        // This is the regression guard for the bug where any 3xx Location was followed.
        assertFalse(ApkVerifier.isAllowedRedirectHost("https://evil.example.com/fake.apk"))
        assertFalse(ApkVerifier.isAllowedRedirectHost("http://github.com.evil.com/x"))
        assertFalse(ApkVerifier.isAllowedRedirectHost("https://sub.githubusercontent.com.evil.com/x"))
        assertFalse(ApkVerifier.isAllowedRedirectHost("not a url"))
        assertFalse(ApkVerifier.isAllowedRedirectHost(""))
    }

    @Test
    fun `lookalike hosts do not slip through`() {
        // Substring matching would let these pass; host equality must not.
        assertFalse(ApkVerifier.isAllowedRedirectHost("https://github.com@evil.example.com/x"))
        assertFalse(ApkVerifier.isAllowedRedirectHost("https://evilgithub.com/x"))
        assertFalse(ApkVerifier.isAllowedRedirectHost("https://github.com.evil.example.com/x"))
    }

    // --- checksum --------------------------------------------------------------------------

    @Test
    fun `sha256Of matches the JDK digest`() {
        val f =
            File.createTempFile("oi-test", ".bin").apply {
                writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
                deleteOnExit()
            }
        val expected =
            MessageDigest.getInstance("SHA-256")
                .digest(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
                .joinToString("") { "%02X".format(it) }
        assertEquals(expected, ApkVerifier.sha256Of(f))
    }

    @Test
    fun `sha256Of returns null for a missing file`() {
        assertNull(ApkVerifier.sha256Of(File(System.getProperty("java.io.tmpdir"), "definitely-not-here-12345.apk")))
    }

    @Test
    fun `sha256Of is case stable and 64 chars`() {
        val f =
            File.createTempFile("oi-test2", ".bin").apply {
                writeText("origin island pro")
                deleteOnExit()
            }
        val digest = ApkVerifier.sha256Of(f)
        assertNotNull(digest)
        assertEquals(64, digest!!.length)
        assertEquals(digest, digest.uppercase())
    }

    // --- pinned certificate ---------------------------------------------------------------

    @Test
    fun `pinned certificate is the documented fingerprint`() {
        // Keeps the pin in this file and the one in docs/DEV.md from drifting apart silently.
        assertEquals(
            "5D2DFA7EF6A96B954AC24361A330BA9B2CEA4518C5B86358C9F4FFB11D79E600",
            ApkVerifier.EXPECTED_CERT_SHA256,
        )
        assertEquals(64, ApkVerifier.EXPECTED_CERT_SHA256.length)
    }
}
