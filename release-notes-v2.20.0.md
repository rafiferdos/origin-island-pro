# Origin Island Pro v2.20.0 — Navigation Card Overhaul

### 🌟 What's New in v2.20.0
1. **Glanceable Status Bar Pill (Zero Punch-Hole Collision):**
   - Implemented `IntelligentTextSanitizer.formatNavigationPill()` which extracts normalized distance and maneuver action into a compact string strictly under 12 characters (e.g. `300m Left`, `1.5km Exit`, `Turn Right`, `U-Turn`, `4km Straight`, `Arrived 🏁`).
   - Completely eliminates status bar text overflow across the front camera punch-hole cutout on OriginOS devices.

2. **Traffic-Aware Dynamic Badge & Edge Glow:**
   - **Clear Route:** Vibrant Electric Navigation Emerald (`#FF00C853`) pill badge and ambient lighting.
   - **Congestion / Delays:** Radiant Amber (`#FFFF9500`) alert badge and ambient lighting when traffic slowdowns are detected in route summaries.

3. **Template 9 Turn & Lane Assist Guidance:**
   - Automatically parses driving lane hints (e.g., `"Use the left 2 lanes"`, `"Toward Airport"`) into `oi_nav_assist` for native OriginOS driving island rendering.

4. **Expanded Navigation App Ecosystem:**
   - Expanded cast detection to support **Baidu Maps** (`com.baidu.BaiduMap`), **HERE WeGo**, **Petal Maps**, **Organic Maps**, **OsmAnd**, and **Sygic** alongside **Google Maps** and **AutoNavi**.

5. **Visual Polish & Fallbacks:**
   - High-fidelity arrow fallback vector icon for clean rendering even when external maps don't supply an icon in notification extras.
