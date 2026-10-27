# Origin Island Pro v2.12.0 ⚡

### 🎨 Customizable Download Pill Styles: Balanced Split (Option 2) & Native Dual-Line (Option 3)

This release solves visual imbalance on the dynamic island during active downloads. Users can now choose their preferred compact layout template directly from the app settings, featuring the new **Balanced Split** (Option 2) and vivo OriginOS **Native Dual-Line** (Option 3).

---

### ✨ What's New in v2.12.0

#### 1. ⚖️ Option 2: Balanced Info Split (`split` — Default)
- **Left Side:** `[Source App Icon] 45%` (clean percentage progress next to the game/app launcher icon).
- **Right Side:** `[ 24.5 Mbps ]` (live speed badge rendered in a sleek grey capsule via vivo SuperX `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT`).
- **Visual Balance:** Eliminates left-heavy text clutter by evenly balancing percentage metrics on the left and live speed on the right.

#### 2. 📱 Option 3: vivo OriginOS Native Dual-Line (`doubleline`)
- **Left Side:** `[Source App Icon]` + Two compact stacked lines (vivo SuperX Left Template 3 via `oi_left_doubleline`):
  - **Line 1:** Clean game / app title (e.g. `Genshin` or `Chrome`).
  - **Line 2:** Live speed (e.g. `24.5 Mbps`).
- **Right Side:** Circular progress ring (`TEMPLATE_RIGHT_ISLAND_PROGRESS`) displaying progress percentage.
- **Aesthetic:** High-density, authentic vivo OriginOS design matching native system service cards.

#### 3. ⏱️ Option 1: Classic Speed & Ring (`speed_ring`)
- **Left Side:** `[Source App Icon] 24.5 Mbps` (live throughput speed).
- **Right Side:** Circular progress ring with percentage badge.

#### 4. ⚙️ In-App Download Pill Style Selector
- Added a dedicated setting under **Cards** in `HomeScreen.kt`:
  - Choice chips: **Split**, **Dual-line**, and **Speed ring**.
  - Dynamic explanation text explaining the layout behavior of each option.
  - Changes take effect immediately and persist in preferences (`download_pill_style`).

#### 5. 🧪 Interactive Sample Tester Updated
- Sample catalog updated with live previews for both Split (`Capsule` badge) and Dual-line layouts in `IslandPreviewView`.
- Triggering sample cards respects the user's active style preference.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.12.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `8cf62734444c15485a69a8352088dfdb240af391740635a9781622c761cc4bb6`
