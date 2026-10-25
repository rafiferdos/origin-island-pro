# Origin Island Pro v2.8.0 🌟

### 🪟 Real Floating Apple Liquid Glass Overlay & Dual-Engine Architecture

This release introduces an optional **Floating Liquid Glass Overlay Engine** alongside the existing **Native vivo OriginOS SuperX Engine**, allowing users to experience true hardware background blur (`FLAG_BLUR_BEHIND`), specular highlight reflections, spring physics, and Apple-style Liquid Glass rendered directly over their phone's camera cutout!

---

### ✨ Highlights & What's New

#### 1. 🎛️ Dual-Engine Architecture (Switchable Anytime)
- **Native vivo SuperX Engine (Default):**
  - Communicates directly with vivo's native `SystemUI` framework via `NotificationManager`.
  - Zero overlay permissions needed.
  - Native integration with vivo's Lockscreen, Always-On Display (AOD), and system camera cutout animations.
  - Ambient edge glow lighting (`oi_light_color` + `oi_light_mode = 1`) and dynamic soundwave.
- **Floating Liquid Glass Overlay Engine ✨ (New & Experimental):**
  - Draws directly on top of the screen around the camera cutout via an interactive Android WindowManager overlay service (`SYSTEM_ALERT_WINDOW`).
  - **Real Hardware Backdrop Blur:** Uses Android 12+ `FLAG_BLUR_BEHIND` and `blurBehindRadius = 45` to blur the actual wallpaper and underlying apps when expanded!
  - **Apple Liquid Glass Specular Sheen:** Multi-layered gradient, specular white highlight border (`Color.White.copy(alpha = 0.65f)`), and electric sapphire ambient glow.
  - **Interactive Spring Animations:** Tapping the compact cutout pill smoothly expands it into the full liquid glass card. Swiping up collapses it back. Swiping sideways dismisses the active island.

#### 2. 📐 Camera Cutout Vertical Offset Adjustment
- Under Settings > Island Engine & Glass Appearance:
  - Added `- 2dp` and `+ 2dp` alignment buttons (ranging from 4dp to 48dp, default 14dp) so the compact floating pill aligns with any front camera punch-hole cutout (vivo X200 Pro, iQOO 13, and others).

#### 3. 🎨 3-Way Expanded Appearance Selector
- **Solid Obsidian:** Matte dark card with maximum readability and contrast.
- **Frosted Glass:** Translucent dark glass with subtle background blur.
- **Apple Liquid Glass ✨:** Specular sheen border, fluid translucent glass, and hardware backdrop blur.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.8.0.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `cef02b4ed8b369986a502035ba75985338a1dd020d5eee38b848d2219b07f947`
