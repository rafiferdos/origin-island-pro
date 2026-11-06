#!/usr/bin/env bash
#
# Publish a release to GitHub, end to end, with the certificate pinned.
#
#   ./scripts/publish-release.sh
#
# Unlike release.sh (which only builds and stages), this one:
#   1. builds the signed release APK,
#   2. verifies the signing certificate matches the published fingerprint,
#   3. writes the .sha256 sidecar,
#   4. creates the GitHub Release with both assets.
#
# It REFUSES to publish if any check fails. A release that installs over an existing
# install must be signed by the same key; publishing anything else would break updates for
# every existing user.
#
# Prerequisites:
#   - keystore.properties at the repo root (copy from keystore.properties.template)
#   - the referenced .jks present
#   - gh CLI authenticated: `gh auth login`
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

export JAVA_HOME="${JAVA_HOME:-C:\\dev\\tools\\jdk17\\jdk}"
export ANDROID_HOME="${ANDROID_HOME:-C:/dev/tools/android-sdk}"

# The fingerprint published in README.md and pinned in ApkVerifier.kt.
EXPECTED_CERT="458C4785FE6DCDF12930AAAF9DAD5BAD912519B1AAE97C68CCF9ED8490919AE8"

fail() { echo "ERROR: $*" >&2; exit 1; }

[[ -f keystore.properties ]] || fail "keystore.properties not found. Copy keystore.properties.template and fill it in. Refusing to publish an UNSIGNED apk — it would not install over an existing install."
command -v gh >/dev/null 2>&1 || fail "gh CLI not found. Install it and run 'gh auth login'."
gh auth status >/dev/null 2>&1 || fail "gh is not authenticated. Run 'gh auth login' first."

VERSION="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n1)"
VERSION="${VERSION:-unversioned}"
TAG="v${VERSION}"
ARTIFACT="origin-island-pro-${VERSION}"
NOTES_FILE="release-notes-${TAG}.md"

echo "==> Version: ${VERSION} (tag ${TAG})"

# --- already published? -------------------------------------------------------------------
if gh release view "${TAG}" --repo rafiferdos/origin-island-pro >/dev/null 2>&1; then
    fail "Release ${TAG} already exists. Bump versionCode/versionName first."
fi

# --- build --------------------------------------------------------------------------------
echo "==> Building signed release ..."
./gradlew --no-daemon --console=plain :app:assembleRelease

APK_IN="app/build/outputs/apk/release/app-release.apk"
[[ -f "$APK_IN" ]] || fail "expected APK not found at ${APK_IN}"

# --- verify the certificate BEFORE publishing ---------------------------------------------
APKSIGNER="$(ls -1 "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n1 || true)"
[[ -n "$APKSIGNER" ]] || fail "apksigner not found in ${ANDROID_HOME}/build-tools"

echo "==> Verifying signing certificate ..."
CERT_OUT="$("$APKSIGNER" verify --print-certs "$APK_IN" 2>&1)"
echo "$CERT_OUT" | grep -E "Signer #1 certificate (DN|SHA-256)"

ACTUAL_CERT="$(echo "$CERT_OUT" | grep -oE '[0-9a-f]{64}' | head -n1 | tr '[:lower:]' '[:upper:]')"
[[ -n "$ACTUAL_CERT" ]] || fail "could not read the signing certificate fingerprint."

if [[ "$ACTUAL_CERT" != "$EXPECTED_CERT" ]]; then
    echo >&2
    echo "ERROR: signing certificate does NOT match the published fingerprint." >&2
    echo "  expected: ${EXPECTED_CERT}" >&2
    echo "  actual:   ${ACTUAL_CERT}" >&2
    echo >&2
    echo "This keystore is not the one the published releases were signed with. Publishing would" >&2
    echo "break updates for every existing install (Android rejects a signature change)." >&2
    echo "Use the original originisle-release.jks, or accept that all users must uninstall first." >&2
    exit 1
fi
echo "    ✓ certificate matches"

# --- also confirm our own verifier agrees --------------------------------------------------
echo "==> Confirming ApkVerifier's pin agrees ..."
grep -q "$EXPECTED_CERT" app/src/main/java/com/originisle/android/security/ApkVerifier.kt \
    || fail "ApkVerifier.kt does not contain the expected fingerprint. Update the pin and docs together."

# --- stage --------------------------------------------------------------------------------
mkdir -p dist
cp -f "$APK_IN" "dist/${ARTIFACT}.apk"
( cd dist && sha256sum "${ARTIFACT}.apk" > "${ARTIFACT}.apk.sha256" )
echo "==> Checksum: $(cat dist/${ARTIFACT}.apk.sha256)"

# --- publish ------------------------------------------------------------------------------
echo "==> Creating GitHub Release ${TAG} ..."
ARGS=(--repo rafiferdos/origin-island-pro --title "Origin Island Pro ${TAG}")
if [[ -f "$NOTES_FILE" ]]; then
    ARGS+=(--notes-file "$NOTES_FILE")
fi
gh release create "${TAG}" "dist/${ARTIFACT}.apk" "dist/${ARTIFACT}.apk.sha256" "${ARGS[@]}"

echo
echo "==> Published ${TAG}."
echo "    APK      : dist/${ARTIFACT}.apk"
echo "    Checksum : dist/${ARTIFACT}.apk.sha256"
echo "    URL      : https://github.com/rafiferdos/origin-island-pro/releases/tag/${TAG}"
echo
echo "    The in-app updater will now verify this APK against the pinned certificate before"
echo "    offering it for install."
