# Origin Island Pro v2.16.0 🚀

### Native OriginOS Atomic Island Landing & App-to-Island Return Animation (原子通知回弹/收敛动画)

This release implements native OriginOS SuperX landing integration (`island.superx.landingPkg`, `island.superx.landingInfo`, `notification.superx.capsule.landingAutoHide`). When an ongoing app (Spotify, YouTube Music, Google Meet, WhatsApp, Navigation, Downloads, etc.) has an active Dynamic Island pill, exiting or minimizing the app via swipe-up or back gesture now plays vivo's signature **Atomic Island Landing / Window-to-Pill Return Animation** instead of the standard launcher app-grid zoom exit animation!

---

### What's New in v2.16.0

#### 1. 🧲 Native OriginOS Island Landing & Return Animation
- **Problem Solved:** When an app was active on the island (e.g. playing music in Spotify/YouTube Music, ongoing call, navigation), tapping the island expanded the card, and tapping the card opened the app. However, upon exiting the app (swipe up to go home or back gesture), vivo OriginOS Launcher played the generic app-to-desktop-grid-icon zoom down animation because it did not recognize that the active SuperX card was associated with that foreground package.
- **Root Cause & Solution:**
  - OriginOS Launcher (`com.bbk.launcher2` / `QuickstepTransitionManager`) monitors active SuperX cards for `island.superx.landingPkg`.
  - When the foreground package matches `landingPkg`, Launcher intercepts the exit window transition and morphs/shrinks the app window smoothly upwards into the active Dynamic Island pill at the top of the display!
  - `OriginIslandBuilder` now writes `island.superx.landingPkg` and `island.superx.landingInfo` into both top-level extras and the `island` bundle, and enables `notification.superx.capsule.landingAutoHide` in the `capsule` bundle.
  - `PlaygroundService` propagates the source app's package name (`source_pkg`) as `landingPkg`.
  - Propagated across all card posters: `MediaCard`, `MessageCard`, `DiscordCard`, `MailCard`, `CallCard`, `DownloadCard`, `NavigationCard`, `SportsCard`, `PaymentCard`, `DeliveryCard`, `RideCard`, `MeetingCard`, `TimerCard`, `WeatherCard`, `AuthPromptCard`, and `GenericCard`.

#### 2. ⚙️ In-App Control Toggle
- Added a dedicated switch in **Settings > System / Cast**:
  - **Island return animation**: "Dock closing app window directly into the active island" (enabled by default).
  - Allows users who prefer standard home screen icon exit animation to toggle it off at any time.

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.16.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `f0438813bc782eb991a004e19c882eb35ea8e185f31c953f0af830e17c77bc97`
