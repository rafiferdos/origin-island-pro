package com.originisle.android.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Verifies a downloaded APK before it is handed to the package installer.
 *
 * Origin Island Pro reads every notification on the device, so a tampered update is not a
 * hypothetical: it is the single highest-impact compromise available. The release pipeline already
 * publishes a SHA-256 checksum per APK (`docs/DEV.md`) and a signing-certificate fingerprint, but
 * nothing on the device ever checked either. This object closes that gap.
 *
 * Two independent checks, both required:
 *
 * 1. **Signing certificate.** The APK's signing cert must hash to [EXPECTED_CERT_SHA256]. A
 *    tampered APK signed with a different key fails here. (A tampered APK signed with the *same*
 *    key cannot happen without the maintainer's keystore — at which point the checksum is the
 *    secondary control.)
 * 2. **SHA-256 of the file.** Must equal the `.sha256` asset published on the release, when the
 *    caller has it. This is what catches a corrupted or truncated download even when the signature
 *    happens to verify (e.g. a re-signed build from a leaked key).
 *
 * Both checks are pure JVM + android.content.pm, so [verify] is unit-testable on the JVM for
 * everything except the certificate comparison.
 */
object ApkVerifier {
    /**
     * SHA-256 of the release signing certificate, hex, upper-case, no separators.
     *
     * VERIFIED against the real published v2.33.0 APK via
     * `apksigner verify --print-certs`: `458c4785fe6dcdf12930aaaf9dad5bad912519b1aae97c68ccf9ed8490919ae8`
     * (Signer #1 certificate DN: `CN=Origin Island Pro, OU=Personal, O=rafiferdos, C=DE`).
     *
     * Note the discrepancy this corrects: `docs/DEV.md` publishes a DIFFERENT fingerprint
     * (`5D2DFA7E…`) than the README and the actual APK. The README and the signed artifact agree
     * with each other, so they are authoritative and docs/DEV.md is stale — see the note in
     * docs/DEV.md. Do not "fix" this back to the docs value.
     *
     * It is a *pin*, not a secret — safe to ship in the APK, and it is what makes a download from
     * anywhere other than the maintainer's key un-installable.
     */
    const val EXPECTED_CERT_SHA256 =
        "458C4785FE6DCDF12930AAAF9DAD5BAD912519B1AAE97C68CCF9ED8490919AE8"

    /** Hosts a GitHub release APK download is allowed to redirect through. */
    private val ALLOWED_REDIRECT_HOSTS =
        setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "api.github.com",
        )

    /** Lower-case hex SHA-256 of [file], or null if it can't be read. */
    fun sha256Of(file: File): String? =
        runCatching {
            file.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    digest.update(buffer, 0, n)
                }
                digest.digest().joinToString("") { "%02X".format(it) }
            }
        }.getOrNull()

    /**
     * The SHA-256 of the APK's signing certificate, upper-case hex, or null if it can't be read
     * (no signature, unsupported API, malformed archive).
     */
    fun signingCertificateSha256(
        context: Context,
        apkFile: File,
    ): String? =
        runCatching {
            val pm = context.packageManager
            val info: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pm.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.GET_SIGNING_CERTIFICATES,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                } ?: return null

            val signatures: Array<Signature> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.signingInfo?.apkContentsSigners ?: return null
                } else {
                    @Suppress("DEPRECATION")
                    info.signatures ?: return null
                }
            if (signatures.isEmpty()) return null

            // android.content.pm.Signature.toByteArray() is the DER-encoded X.509 certificate, which
            // is exactly what `apksigner verify --print-certs` hashes — so this is directly comparable
            // to the fingerprint published in docs/DEV.md.
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(signatures[0].toByteArray())
            digest.digest().joinToString("") { "%02X".format(it) }
        }.getOrNull()

    /**
     * Result of [verify], so callers can tell the user *why* an install was refused instead of
     * just refusing it.
     */
    sealed class Result {
        /** Safe to install. [file] is the verified APK. */
        data class Ok(val file: File, val detail: String) : Result()

        /** Refused. [reason] is user-presentable. */
        data class Rejected(val reason: String) : Result()
    }

    /**
     * Verify [apkFile] is the genuine maintainer build.
     *
     * @param expectedSha256 the release's published `.sha256` value, if known. Null skips the
     *   checksum comparison (the signature check still runs) — the signature is the stronger
     *   control, and it is the one the release pipeline guarantees.
     * @param allowUnpinnedCert set true only for debug builds, where the APK is signed with the
     *   debug keystore and cannot match [EXPECTED_CERT_SHA256]. Never true for a release build.
     */
    fun verify(
        context: Context,
        apkFile: File,
        expectedSha256: String? = null,
        allowUnpinnedCert: Boolean = false,
    ): Result {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return Result.Rejected("Downloaded file is missing or empty.")
        }

        val certSha =
            signingCertificateSha256(context, apkFile)
                ?: return Result.Rejected(
                    "The download has no readable signing certificate — it is not a valid Origin Island Pro build.",
                )

        if (!certSha.equals(EXPECTED_CERT_SHA256, ignoreCase = true) && !allowUnpinnedCert) {
            return Result.Rejected(
                "Signature mismatch.\n" +
                    "Expected: $EXPECTED_CERT_SHA256\n" +
                    "Got:      $certSha\n\n" +
                    "This APK was not signed by the maintainer's key. Do not install it.",
            )
        }

        if (expectedSha256 != null) {
            val actual = sha256Of(apkFile)
            if (actual == null) {
                return Result.Rejected("Could not read the downloaded file to verify its checksum.")
            }
            if (!actual.equals(expectedSha256.trim().substringBefore(' ').uppercase(), ignoreCase = true)) {
                return Result.Rejected(
                    "Checksum mismatch — the download is corrupt or incomplete.\n" +
                        "Expected: $expectedSha256\n" +
                        "Got:      $actual",
                )
            }
            return Result.Ok(apkFile, "signature + checksum verified")
        }

        return Result.Ok(apkFile, "signature verified")
    }

    /** Whether [url] is on a host a release download is allowed to redirect to. */
    fun isAllowedRedirectHost(url: String): Boolean =
        runCatching {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            host in ALLOWED_REDIRECT_HOSTS
        }.getOrDefault(false)
}
