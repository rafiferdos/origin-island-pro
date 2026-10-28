# Origin Island Pro v2.15.0 🚀

### Smart First-Name Extraction + Live Video Call Timer/Wave + Adaptive Message "View" Badge

This release introduces intelligent name reasoning for messaging notifications, automatic detection and enrichment of blank right-side island pills (featuring live Google Meet / video call timers and waveforms), and customizable message preview styling with an adaptive `[ View ]` badge.

---

### What's New in v2.15.0

#### 1. 👤 Smart First-Name Extraction (Compact Pill)
- **Problem Solved:** Long or title-heavy names (e.g. "Md. Rafiul Ferdos", "Dr. Gregory House") were previously truncated awkwardly into "Md. Ra…" in the limited width of the compact Dynamic Island pill.
- **Smart Reasoning Engine:** Added `IntelligentTextSanitizer.smartFirstName(rawName)`:
  - Intelligently detects and skips honorifics and prefixes: `Md`, `Md.`, `MD`, `Dr.`, `Prof.`, `Engr.`, `Adv.`, `Sheikh`, `Syed`, `Mst.`, `Mr.`, `Mrs.`, etc.
  - Handles prefixes even without spaces (e.g. `md.tanvir` -> `Tanvir`).
  - Strips emojis and unread message counters (`(3)`, `[2]`), and properly capitalizes.
  - Non-expanded pill shows: **`[App Icon] Rafiul`**.
  - Expanded card continues to show: **`Md. Rafiul Ferdos`** (full name + photo avatar).

#### 2. 📹 Live Video Call & Meeting Island Pill (Google Meet, Zoom, Teams, Calls)
- **Problem Solved:** When in Google Meet, Gmail video call, or Zoom, the Dynamic Island left side showed "Your video" while the right side sat completely blank.
- **Automatic Right-Side Detection & Enrichment:**
  - Automatically identifies video calls, meetings, and ongoing call states across Google Meet, Gmail embedded Meet, Zoom, Teams, and WhatsApp.
  - **Live Elapsed Timer:** When a call start timestamp is present, calculates and displays live elapsed duration (e.g. `[ 03:45 ]`) in a capsule badge that ticks every second in real time.
  - **Animated Waveform:** If no timestamp is present, displays vivo SuperX's live animated sound/video wave (`TEMPLATE_RIGHT_ISLAND_WAVE`), eliminating empty space completely.
  - Extends `NotificationCastListener`'s poll loop so meeting timers tick smoothly every second.

#### 3. 💬 Adaptive Message Preview & `[ View ]` Badge
- **Short Messages (≤ 16 chars):** Short phrases (e.g. "Hi", "Hello", "Where are you?", "Ok") fit statically on the right side of the pill without horizontal marquee scrolling.
- **Long Messages (> 16 chars):** Instead of cutting off or auto-scrolling, displays a sleek `[ View ]` capsule badge.
- **In-App Selector (Home > Cards > Messages):**
  - **Smart (Default):** Short messages display text; long messages display `[ View ]` badge.
  - **Expand only:** Pill always displays `[ View ]` badge; full text only shown when expanding.
  - **Full text:** Always displays message text snippet on the pill.
- Applied across both standard messaging apps (`MessageCard`) and Discord (`DiscordCard`).

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.15.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `d8a5188be712e764013b4bde005ea113a30ef5258676bfbad721c10ef22194f5`
