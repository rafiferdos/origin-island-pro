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
    fun `pinned certificate matches the published v2_33_0 APK, not docs_DEV_md`() {
        // The pin was originally copied from docs/DEV.md and was WRONG: it disagreed with both the
        // README and the actual signed artifact. Verified against the real v2.33.0 release with
        // `apksigner verify --print-certs`, which reports the lowercase digest
        // 458c4785fe6dcdf12930aaaf9dad5bad912519b1aae97c68ccf9ed8490919ae8.
        // If this test ever fails after a key rotation, update the pin AND docs/DEV.md together.
        assertEquals(
            "458C4785FE6DCDF12930AAAF9DAD5BAD912519B1AAE97C68CCF9ED8490919AE8",
            ApkVerifier.EXPECTED_CERT_SHA256,
        )
        assertEquals(64, ApkVerifier.EXPECTED_CERT_SHA256.length)
    }

    @Test
    fun `pinned certificate agrees with the README fingerprint`() {
        // README.md publishes the same value colon-separated. This guards the two published copies
        // drifting apart again.
        val colonSeparated = ApkVerifier.EXPECTED_CERT_SHA256.chunked(2).joinToString(":")
        assertEquals(
            "45:8C:47:85:FE:6D:CD:F1:29:30:AA:AF:9D:AD:5B:AD:91:25:19:B1:AA:E9:7C:68:CC:F9:ED:84:90:91:9A:E8",
            colonSeparated,
        )
    }
}
