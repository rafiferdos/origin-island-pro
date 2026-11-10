# Developer guide

This doc covers building Origin Isle from source, the package-name spoof it relies on, and how to
cut a signed release. For what the app does and how to install it, see the [README](../README.md).

## ⚠️ Read this first: the package name is load-bearing

`applicationId = "com.autonavi.minimap"` in [`app/build.gradle.kts`](../app/build.gradle.kts) is
**deliberate, not a mistake**. OriginOS only grants the hidden `setSuperXInfosSceneList` API to a
short whitelist of packages, and this app's `applicationId` spoofs AMap's (高德地图 / AutoNavi) real
package id to land on that whitelist. **If you change it, the island will stop working** —
`grantScenes()` will start logging `setSuperXInfosSceneList unavailable`.

Two consequences:

1. **It cannot be installed alongside the real AMap app** (same applicationId → package conflict).
2. **This can never ship on the Play Store** or be distributed as if it were AMap. It's a personal
   sideload — install it on your own device, understand what you're installing.

The Kotlin package / `R` namespace is a separate, harmless identifier: `com.originisle.android`. It's
just where the code lives; it isn't checked by OriginOS.

## Build & run

Requires Android Studio (AGP 8.9+) and the `android-36` SDK platform.

1. **Open the folder in Android Studio** and let it sync.
2. **Run** onto a vivo/iQOO OriginOS device. (It will build and install fine on any Android 14+
   device, but the island itself only renders on OriginOS.)
3. On first launch, the onboarding screen walks you through the permissions. Notification access is
   the only mandatory one to continue; grant the rest for the best experience (see the README).
4. Try the sample cards in the **Cast** tab to confirm the island is working before relying on real
   notifications.

### Recommended device setup (all optional, but each fixes a real OriginOS quirk)

- **Battery → unrestricted**, so OriginOS doesn't kill the background caster to save power.
- **Accessibility → "Origin Isle keep-alive"**, so the app runs with no status-bar icon.
- **OriginOS auto-start allow-list** (Settings → Battery → Auto-start), or the caster gets killed
  when the screen turns off.

## Signed release build

The debug build works fine for personal use (SuperX doesn't care about the signing key). To build a
release APK you can update over time:

```
keytool -genkeypair -v -keystore originisle-release.jks -alias originisle -keyalg RSA -keysize 2048 -validity 10000
```

Create `keystore.properties` at the repo root (gitignored — never commit it):

```
storeFile=../originisle-release.jks
storePassword=...
keyAlias=originisle
keyPassword=...
```

A `keystore.properties.template` is committed as a reference — copy it to `keystore.properties` and
fill in the real values on the machine that holds your key. `keystore.properties`, `*.jks` and
`*.keystore` are gitignored — never commit them, and keep a safe backup: losing the keystore or its
passwords means you can never publish a signature-compatible update again.

Then run the release helper from the repo root:

```
./scripts/release.sh
```

It builds `:app:assembleSpoofRelease`, verifies the signature, and writes the signed APK plus its SHA-256
checksum to `dist/`, printing the signing certificate's fingerprint. Upload **both** the APK and the
`.sha256` file to the GitHub Release. (Plain `./gradlew :app:assembleSpoofRelease` still works; without
`keystore.properties` it builds *unsigned*, which is why the script refuses to run without it.)

> Switching between a debug-signed and release-signed install requires uninstalling first (Android
> rejects a signature mismatch), which also drops notification access and the keep-alive accessibility
> grant — re-grant them after.

## Verify a release is genuine

This app reads your notifications, so only ever install a build you can prove came from the
maintainer. Every official APK is signed with one private key and published on this repo's Releases
page with its SHA-256 checksum. To check a downloaded APK:

```
# 1. checksum matches the one published on the release
shasum -a 256 origin-isle-<version>.apk

# 2. it was signed by the maintainer's key — compare against the fingerprint below
apksigner verify --print-certs origin-isle-<version>.apk
```

Official signing certificate SHA-256 (current, v2.34.0 and later):

```
D8:52:11:80:FE:AA:EE:30:37:21:DD:1F:7E:E2:88:39:60:35:42:66:0C:07:A7:45:39:9C:84:3A:99:20:14:5F
```

> **Correction (this page).** This page previously listed `5D:2D:FA:7E…`, which does not match the
> README or any published APK. It was verified against the real v2.33.0 release with
> `apksigner verify --print-certs`, which reported
> `458c4785fe6dcdf12930aaaf9dad5bad912519b1aae97c68ccf9ed8490919ae8`
> (Signer #1 certificate DN: `CN=Origin Island Pro, OU=Personal, O=rafiferdos, C=DE`).
>
> **Key rotation (v2.34.0).** That v2.33.0 key is no longer available on the maintainer's machine,
> so a new release key was generated and every artifact from v2.34.0 on is signed with it. The
> fingerprint above is verified against the signed v2.34.0 release APK; the DN is unchanged
> (`CN=Origin Island Pro, OU=Personal, O=rafiferdos, C=DE`), so only the key changed.
>
> Consequence: a v2.34.0 APK cannot install over an existing v2.33.0 install — Android rejects a
> signature change — so users must uninstall first. The pin in
> `app/src/main/java/com/originisle/android/security/ApkVerifier.kt` matches this value, and
> `ApkVerifierTest` fails if the pin, this page, or the README ever disagree.

A build whose checksum or certificate fingerprint doesn't match is **not** an official build — do not
install it. Never install an "Origin Isle" APK from anywhere other than this repo's Releases page.
