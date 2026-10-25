# Origin Island Pro v2.8.1 🚀

### 💎 Deep Analysis & Critical Fixes for Floating Liquid Glass Overlay

This update addresses the root causes of why the floating island overlay did not trigger or render glass on hardware, fixing SharedPreferences synchronization, Android 14+ foreground service declarations, dynamic view initialization, and ultra-translucent specular glass shaders.

---

### 🛠️ Root Causes Found & Fixed

1. **Preferences Synchronization Fix:**
   - Previous overlay service checked a mismatched preferences file (`"OriginIsle_Prefs"`) instead of the app's central `PREFS_NAME` (`"experimental_prefs"`).
   - As a result, the engine check always returned early thinking it was in native mode. Now correctly unified under `PREFS_NAME`.

2. **Android 14/15 FGS Specification (`AndroidManifest.xml`):**
   - Declared `android:foregroundServiceType="specialUse"` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` for `FloatingIslandService`, preventing OS-level background execution blocks.

3. **WindowManager View Initialization Lifecycle:**
   - Ensured `initOverlayView()` is dynamically invoked upon card arrival if the view hasn't been mounted yet.
   - Parcelable `Icon` extraction for source app icons (Play Store, Chrome, Spotify) to display launcher bitmaps immediately on the floating pill.

4. **True Apple Liquid Glass Shader Refinements:**
   - Reduced dark slab opacity from 85% down to **25%–40% translucent luminous glass** (`0x552A2A44` to `0x35141424`), allowing the wallpaper, apps, and hardware blur (`blurBehindRadius = 45`) beneath the floating card to shine through clearly.
   - Enhanced specular highlight sheen border with 90% white top-flare.

5. **Instant One-Tap Test Button:**
   - Added **"Test Floating Glass Island 🚀"** directly under the Engine & Appearance section in Home settings so users can trigger the floating island immediately.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.8.1.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `f67336f1ea2739c06dc3c1abf8cefd5b929a3c427ca9e6d02e5690fb0b780e40`
