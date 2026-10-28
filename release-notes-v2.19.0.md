# Origin Island Pro v2.19.0 Release Notes

### 🚀 Highlights
- **Native OriginOS 6 (Android 16) Gesture Swipe-Up Island Return**:
  - Pinned island state with `island.superx.state = 1` and `permanent = true`.
  - Added full hierarchical landing resolution across `island.superx`, `notification.superx.capsule`, and `baseInfos`.
  - Disabled `capsuleLandingAutoHide` so the capsule stays visible on the status bar while the foreground app is running, enabling OriginOS 6 Launcher to calculate exact docking coordinates upon home swipe-up gesture.
- **Pure Native Call Card UX**:
  - Expanded ongoing call cards now natively present **ONLY the single Crimson Red [ 📴 Hang Up ] button** by default, mirroring native OriginOS call UI.
  - Tapping **anywhere else** on the expanded card (avatar, caller name, call duration, card background) immediately brings the active call screen to the front without getting blocked.
  - Added an optional settings switch ("Call screen action button") under System/Cast for users who also prefer a secondary green action button.

### 📦 Checksum & Binary
- **APK**: `origin-island-pro-2.19.0.apk`
- **SHA-256**: `9559f8bdc1285d15cf9d0f2d2c6aabf61333240606a4113c44417fd425d8bb37`
