# Origin Island Pro v2.11.0 ⚡

### 🌐 Universal Download Engine: Auto-Detection for ANY App, Live Mbps TrafficStats & Source App Icons

This release introduces an intelligent, universal download detection and real-time network throughput engine. You no longer need hardcoded app whitelists — Origin Island now automatically detects in-game resource downloads (e.g. **Genshin Impact**, PUBG, Call of Duty), browsers, file managers, and cloud transfers from **any app in the world**.

---

### ✨ What's New in v2.11.0

#### 1. 🎮 Universal Auto-Detection for ANY Download
- **Works with any app worldwide:** Automatically intercepts downloads, game patch updates, resource packs, and file transfers without needing the app to be in a predefined list.
- **Multi-signal detection:** Analyzes progress metrics (`progressMax`, indeterminate flags), category metadata (`CATEGORY_PROGRESS`), size patterns (`4.5 GB / 12.0 GB`), speed indicators, and multi-lingual keywords (`downloading`, `resources`, `assets`, `obb`, `ডাউনলোড`, etc.).

#### 2. 📱 Authentic Source App Icon on Left Pill
- When a download is active, the compact pill displays the **actual launcher icon of the downloading app** (e.g., Genshin Impact's official app icon, Google Chrome, Play Store, Telegram, etc.) on the left side.

#### 3. ⚡ Real-Time Live Speed in Megabits per Second (Mbps)
- **Kernel-level Traffic Accounting (`TrafficStats`):** Measures actual real-time network RX throughput per second directly via Android's `TrafficStats.getUidRxBytes(uid)`.
- Even if a game or app does not print speed in its notification text (e.g. Genshin Impact just saying "Downloading game resources..."), Origin Island calculates and displays the live speed in **Mbps** (e.g., `19.2 Mbps`, `24.5 Mbps`, `850 Kbps`).
- Continually refreshed every second in background through `pollRunnable`.

#### 4. 🔄 Sleek Circular Progress Ring on Right Pill
- Pinned to the right side of the pill: an animated circular progress ring (`TEMPLATE_RIGHT_ISLAND_PROGRESS`) reflecting exact percentage (e.g. `45%`), or animated loading dots (`TEMPLATE_RIGHT_ISLAND_LOADING`) for indeterminate downloads.

#### 5. 📋 Meaningful Expanded Card Text
- Expanding the island reveals comprehensive, structured details:
  - **Title:** The game/file name (e.g. `Genshin Impact` or `update-v5.2.zip`).
  - **Body:** `45% • 4.5 GB / 12.0 GB • 24.5 Mbps • ~5 mins left`.
  - **Subtext:** `Genshin Impact • Game Asset Download`.
  - **Progress Bar:** High-precision, uninterrupted progress track (Template 2).
  - **Interactive Actions:** Pause / Cancel buttons if supported by the source app.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.11.0.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `edf0e14f716e7cdc337413263575514f6b22c4a10e9be49978148a41b8416aa8`
