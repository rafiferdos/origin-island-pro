# Origin Island Pro v2.17.0 🚀

### Call Island Optimization: Smart Name Trimming + Live Elapsed Call Duration Badge + Direct In-Call Screen Deep-Link

This release brings call notifications (Messenger, WhatsApp, Telegram, Phone dialer, and VoIP) to the same refined standard as messaging, featuring intelligent caller name trimming on the compact pill, a live ticking elapsed call duration badge on the right pill, and safe deep-link routing to open the active in-call screen.

---

### What's New in v2.17.0

#### 1. 👤 Smart Caller Name Trimming (Compact Island Pill Left)
- In the compact pill state, caller names are now intelligently formatted using `IntelligentTextSanitizer.smartFirstName(callerName)`:
  - Skips honorifics and prefixes (`Md.`, `MD`, `Dr.`, `Prof.`, `Engr.`, `Adv.`, `Sheikh`, etc.).
  - `"Md. Rafiul Ferdos"` renders cleanly as **`[App Icon / Avatar] Rafiul`**.
  - Expanding the card reveals the full caller name (`Md. Rafiul Ferdos`).

#### 2. ⏱️ Live Elapsed Call Duration Badge (Compact Island Pill Right)
- When a call is ongoing / connected (Messenger, WhatsApp, Phone, etc.):
  - The right side of the Dynamic Island pill displays a high-visibility Emerald Green (`#34C759`) capsule badge with the live elapsed call time (e.g., `00:45`, `02:15`, `12:34`).
  - Automatically extracts duration from the notification text, start timestamp (`when`), or internal session timer.
  - Ticks live every 1 second in real time while the call remains active.
- For incoming (ringing) calls, the dynamic ringing waveform remains active until answered.

#### 3. 📞 Direct In-Call Screen Tap-to-Open (Card Body)
- **Problem Solved:** Previously, tapping the expanded card could either fail to bring up the call UI or accidentally trigger the Decline/Hang-up intent if fallback resolution picked the first action.
- **Dedicated Call Deep-Link Architecture (`extractCallDeepLinkIntent`):**
  - The **Hang up** button exclusively triggers call termination (`declineAction.actionIntent`).
  - Tapping **anywhere else** on the expanded card (caller name, avatar photo, duration/status line, card background) triggers `clickIntent`, opening the full-screen InCallActivity / active call UI in Messenger, WhatsApp, or Phone dialer.
  - Safe fallback brings the source app's task forward with `FLAG_ACTIVITY_REORDER_TO_FRONT`.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.17.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `3a4f01b1463ed3213374e6b0c11874d8d66d8e6da64603825241446757d0d375`
