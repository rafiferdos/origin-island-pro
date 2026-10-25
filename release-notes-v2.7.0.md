# Origin Island Pro v2.7.0 🚀

### 💎 Apple Liquid Glass, Ghost Player Ambient Music & Mbps Download Engine

This release brings an ultra-premium visual redesign to Origin Island Pro for vivo OriginOS, fixing progress line clutter, introducing real-time Mbps speed metering, source app branding, ambient music edge glow, and a hardware-accelerated Apple Liquid Glass engine.

---

### ✨ Highlights & What's New

#### 1. ⚡ Mbps Download Engine & Source App Branding
- **Per-Second Megabits Throughput:** Network speeds are now converted and displayed in **Megabits per second (Mbps)** (`* 8.0`) rather than raw bytes (e.g. `2.4 MB/s` -> `19.2 Mbps` / `19.2M`).
- **Source App Icon on Compact Island:** When downloading via Google Play Store, Chrome, Telegram, or ADM, the compact pill displays the **actual launcher icon of the downloading app** rather than a generic download icon.
- **Normalized Dimensions:** Island pill maintains stable, pixel-perfect dimensions with no awkward width stretching.

#### 2. 🪄 Redesigned Progress Line (Zero Clutter)
- **Eliminated Repeating 4-Icon Clutter:** Fixed the template renderer where 4 repeating download/timer icons were rendered along the progress track.
- **Continuous Sleek Track:** Both active downloads and countdown timers now feature a continuous, clean, modern progress track.

#### 3. 🎵 Ghost Player Music Integration
- **Clean Track Metadata:** Redundant video and streaming tags (e.g., `[Official Music Video]`, `(Lyrics)`, `[Audio]`, `Remastered 2013`) are stripped automatically from the island title.
- **Ambient Artwork Edge Glow:** Dynamically extracts the dominant color palette from album art and projects an ambient glow (`oi_light_color` + `oi_light_mode = 1`) around the expanded island card.
- **Multi-Hue Dynamic Soundwave:** Passes dynamic album art colors (`oi_wave_color_list`) into vivo SuperX wave visualizer.
- **High-Contrast Transport Controls:** Styled prev/play/pause/next control buttons with tactile dark surfaces.

#### 4. 🪟 Apple Liquid Glass & Frosted Glass Compositor
- **In-App Glass Selector:** Added a 3-way appearance switcher under Settings:
  - **Solid Obsidian:** Classic opaque matte dark card with high contrast.
  - **Frosted Glass:** Translucent frosted dark glass with subtle background blur.
  - **Apple Liquid Glass ✨:** Fluid translucent glass with specular highlight sheen, electric sapphire edge glow, and hardware compositor blur.
- **Live Compose Preview:** Real-time preview on the home screen updates immediately with specular highlight borders and gradient depth when changing styles.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.7.0.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `fcdb27368460c2aa181053272c030d595f0d8e5f631743f5880b2e5f72d0f7eb`
