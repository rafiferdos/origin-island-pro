# Origin Island Pro v2.9.0 ⚡

### 🌟 Return to Pure Native vivo OriginOS SuperX Architecture

Based on real-world testing on vivo OriginOS hardware, this release completely purges artificial window overlays and returns 100% to vivo's native hardware-accelerated **SuperX Island** engine. This ensures buttery smooth 120Hz native transitions, zero touch latency, seamless Lockscreen & Always-On Display (AOD) integration, and minimal battery consumption.

---

### ✨ What's in This Release

1. **Purged Artificial Floating Window & Glass Experiments:**
   - Completely removed all window overlay code (`FloatingIslandService`, `WindowManager` layers, and `SYSTEM_ALERT_WINDOW` permission).
   - Removed artificial glass dropdowns and clutter from the app settings, restoring the clean, high-contrast, minimalist **Obsidian UI**.
   - Zero background overhead — 100% aligned with vivo's native OriginOS SuperX notification framework.

2. **Clean & Modern Download / Timer Progress Line (Template 2):**
   - Eliminated repetitive and clunky icons overlapping the progress bar (previously showing 4 redundant icons across the track).
   - The expanded island now features an uninterrupted, sleek, high-precision progress line.

3. **Dynamic Source App Launcher Icon:**
   - In compact/unexpanded island state, active downloads display the source application's authentic launcher icon (e.g. Google Play Store, Google Chrome, Telegram) instead of legacy download glyphs.

4. **Real-time Download Throughput in Megabits per Second (Mbps):**
   - Download speeds are now calculated per-second in **Mbps** (e.g., `19.2 Mbps` / `19.2M`) rather than raw bytes, matching standard ISP speedtests and Google Play Store metrics.

5. **Ghost-Player Audiophile Enhancements:**
   - Automatic song title cleanup (strips junk tags like `[Official Video]`, `(Lyrics)`, `feat.`).
   - Native ambient edge lighting (`oi_light_color` + `oi_light_mode = 1`) dynamically keyed to current album artwork.
   - Multi-hue soundwave visualization (`oi_wave_color_list`).

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.9.0.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `819b9a4b01dd2c68978b764b39a1daf1f232531a639cdbd3ed9fee9d8935ab19`
