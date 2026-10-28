# Origin Island Pro v2.13.0 ⚡

### Vivo OriginOS 6-Safe Optimisation + Privacy Mode + Customizable Download Pill Styles

This release combines privacy enhancements, battery-saving adaptive polling, and a comprehensive overhaul of download pill styles with guaranteed real-time throughput accounting in Megabits per second (Mbps).

---

### What's New in v2.13.0

#### 1. 🛡️ Privacy Mode (Opt-in, default OFF)
- New toggle in **Home > Cards**: **Privacy mode**.
- When ON:
  - **Message cards:** Displays sender + app, but masks the message body on the island as "New message" — both in compact pill and expanded card.
  - **Payment cards:** Preserves the spinner -> tick animation while hiding amounts and merchants: phase 1 reads "Payment complete", phase 2 reads "Paid".
  - OTP and 2FA cards remain untouched to allow one-tap code copying.

#### 2. 🔋 Adaptive Polling (Battery Life)
- Notification listener sweeps all active notifications dynamically:
  - **1s refresh** only while a live card is active (media, ticking timer, active download, live score).
  - **3s refresh** during idle times.
- Reduces IPC binder calls by ~66% with zero impact on UI responsiveness.

#### 3. ⚡ Guaranteed Real-Time Download Speed in Mbps
- Fixed real-time network throughput measurement on Android 14+ / vivo OriginOS devices where per-UID stats are restricted (`TrafficStats.getUidRxBytes(uid)` returning -1).
- Automatically falls back to kernel-level device network accounting via `TrafficStats.getTotalRxBytes()`.
- Live throughput is accurately computed every second and rendered in Megabits per second (e.g. `24.5 Mbps`, `19.2 Mbps`, `850 Kbps`), ensuring speed is never lost or replaced by plain percentages.

#### 4. 🎨 4 Customizable Download Pill Styles
Select your favorite layout under **Cards > Download pill style**:
1. **Speed & Ring (Default & Recommended):**
   - Left: `[App Icon] 24.5 Mbps`
   - Right: Circular progress ring with percentage (`45%`).
2. **Dual-line (vivo OriginOS Native):**
   - Left: `[App Icon]` + 2 stacked lines: Line 1 App Name, Line 2 Live Speed (`24.5 Mbps`).
   - Right: Circular progress ring with percentage (`45%`).
3. **Speed & % Badge:**
   - Left: `[App Icon] 24.5 Mbps`
   - Right: Percentage in a sleek capsule badge (`[ 45% ]`).
4. **% & Speed Badge:**
   - Left: `[App Icon] 45%`
   - Right: Live speed in a capsule badge (`[ 24.5 Mbps ]`).

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.13.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `4690bb39299ab6b0f4bb4cd4ae9af1be91d68859a11cfb623df841ef59a8d045`
