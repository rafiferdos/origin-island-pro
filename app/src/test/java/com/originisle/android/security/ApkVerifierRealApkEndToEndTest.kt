package com.originisle.android.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory

/**
 * End-to-end proof that [ApkVerifier]'s certificate logic can extract the signing certificate
 * from a GENUINE release APK.
 *
 * [ApkVerifier.signingCertificateSha256] needs android.content.pm, which is not on the JVM, so this
 * test reproduces the same computation with the JDK only: it pulls the signing certificate out of
 * the APK's v2/v3 signing block and hashes the DER encoding. That is the same value
 * `apksigner verify --print-certs` prints, and the same value the verifier compares against.
 *
 * Expected artifact: `official-2.33.0.apk` in the repo root (the downloaded GitHub release).
 * The test skips when it is absent, so CI is green without the artifact.
 *
 * Note the artifact is signed with the RETIRED v2.33.0 key, not the current pin — the key was
 * rotated in v2.34.0 and the old `.jks` is no longer available. The test therefore asserts the
 * artifact's known fingerprint (proving the extraction works on a real APK with a known answer)
 * and separately asserts that the live pin is a *different* key, so the rotation cannot silently
 * revert.
 */
class ApkVerifierRealApkEndToEndTest {
    /** Fingerprint of the retired v2.33.0 release key, verified with `apksigner verify --print-certs`. */
    private val v2330Cert = "458C4785FE6DCDF12930AAAF9DAD5BAD912519B1AAE97C68CCF9ED8490919AE8"

    private fun findReleaseApk(): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            if (dir == null) return@repeat
            val candidate = File(dir, "official-2.33.0.apk")
            if (candidate.exists()) return candidate
            dir = dir?.parentFile
        }
        return null
    }

    @Test
    fun `genuine v2_33_0 release APK certificate extracts to its known fingerprint`() {
        val apk = findReleaseApk()
        if (apk == null) {
            println("SKIP: official-2.33.0.apk not found in the repo root; nothing to verify.")
            return
        }

        val certDer = extractSigningCertificate(apk)
            ?: throw AssertionError("Could not extract a certificate from ${apk.name}")

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(certDer)
            .joinToString("") { "%02X".format(it) }

        println("APK:         ${apk.name}")
        println("cert SHA256: $digest")
        println("known v2.33: $v2330Cert")
        println("live pin:    ${ApkVerifier.EXPECTED_CERT_SHA256}")

        assertEquals(v2330Cert, digest)
    }

    @Test
    fun `live pin is not the retired v2_33_0 key`() {
        // Guards the key rotation: the pin must be the new key, not the old one, and must not
        // silently revert to it.
        assertTrue(ApkVerifier.EXPECTED_CERT_SHA256.isNotBlank())
        assertTrue(
            "the pin is the retired v2.33.0 key — the rotation was reverted",
            !ApkVerifier.EXPECTED_CERT_SHA256.equals(v2330Cert, ignoreCase = true),
        )
    }

    // --- APK signature extraction (JDK only) ----------------------------------------------

    /**
     * Returns the DER encoding of the APK's first signing certificate.
     *
     * Modern release APKs are v2/v3 signed and carry no v1 (META-INF) signature, so this parses the
     * APK Signing Block that sits just before the central directory: magic "APK Sig Block 42",
     * length-prefixed id/value pairs, id 0x7109871a = signed data, whose value holds the signer
     * certificate chain.
     *
     * Only the length-prefix walking and the certificate scan are needed; SignedData is not fully
     * parsed, because the certificate is found by DER shape.
     */
    private fun extractSigningCertificate(apk: File): ByteArray? {
        val bytes = apk.readBytes()

        val magic = "APK Sig Block 42".toByteArray()
        var block: ByteArray? = null
        val idx = indexOf(bytes, magic)
        if (idx >= 0) {
            // Layout: [u64 size][pairs...][u64 size]["APK Sig Block 42"], immediately before the
            // zip central directory. The size sits 8 bytes before the magic; the block is
            // [cd_off - size, cd_off).
            val trailingSizeOff = idx - 8
            if (trailingSizeOff >= 0) {
                val size = readU64Le(bytes, trailingSizeOff)
                val blockStart = trailingSizeOff - size.toInt()
                if (size >= 24L && size <= bytes.size.toLong() && blockStart >= 0) {
                    block = bytes.copyOfRange(blockStart, blockStart + size.toInt())
                }
            }
        }

        val candidates = mutableListOf<ByteArray>()
        block?.let { candidates.add(it) }
        extractV1Signature(bytes)?.let { candidates.add(it) }

        for (blob in candidates) {
            val cert = scanForDerCertificate(blob)
            if (cert != null) return cert
        }
        return null
    }

    /** Reads the first v1 signature file from the zip, if present. */
    private fun extractV1Signature(bytes: ByteArray): ByteArray? = runCatching {
        val tmp = File.createTempFile("oi-v1", ".apk")
        tmp.writeBytes(bytes)
        tmp.deleteOnExit()
        java.util.zip.ZipFile(tmp).use { zip ->
            val entry = zip.entries().asSequence().firstOrNull { e ->
                val n = e.name.uppercase()
                n.startsWith("META-INF/") &&
                    (n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")) &&
                    !n.endsWith(".SF")
            } ?: return null
            zip.getInputStream(entry).use { it.readBytes() }
        }
    }.getOrNull()

    /**
     * Finds the first DER-encoded X.509 certificate in [blob] by shape: a SEQUENCE whose length
     * CertificateFactory can actually parse.
     */
    private fun scanForDerCertificate(blob: ByteArray): ByteArray? {
        val cf = CertificateFactory.getInstance("X.509")
        var i = 0
        while (i < blob.size - 8) {
            if (blob[i] == 0x30.toByte() && blob[i + 1] == 0x82.toByte()) {
                val len = ((blob[i + 2].toInt() and 0xFF) shl 8) or (blob[i + 3].toInt() and 0xFF)
                if (len in 200..65536 && i + 4 + len <= blob.size) {
                    val candidate = blob.copyOfRange(i, i + 4 + len)
                    val parsed = runCatching {
                        cf.generateCertificate(candidate.inputStream()).encoded
                    }.getOrNull()
                    if (parsed != null) return parsed
                }
            }
            i++
        }
        return null
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun readU64Le(b: ByteArray, off: Int): Long {
        if (off < 0 || off + 8 > b.size) return -1
        var v = 0L
        for (i in 7 downTo 0) {
            v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        }
        return v
    }
}
