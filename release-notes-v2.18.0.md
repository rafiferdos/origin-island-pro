# Origin Island Pro v2.18.0 Release Notes

### 🚀 Highlights
- **Gesture Swipe-Up Return Animation Fix**:
  - Swiping up from an active call app (e.g. Messenger, WhatsApp, Phone dialer) to the home screen now smoothly returns and docks the app window directly into the active Dynamic Island pill.
  - Disabled `capsuleLandingAutoHide` which previously hid the status bar capsule while the foreground app was active, leaving Launcher without an on-screen island coordinate to dock into.
  - Pinned island visibility using `island.superx.permanent = true` and populated redundant landing protocol keys across all bundle layers (`landingPkg`, `landingInfo`, `capsule`, `baseInfos`, and root).
- **Expanded Call Card Tap Routing to Active Call Screen**:
  - Tapping outside the Hang Up button or tapping the new Emerald Green **`[ 📞 Call Screen ]`** button immediately opens the active call screen in Messenger, WhatsApp, or Phone dialer without getting dropped by Android 14/15 background activity launch restrictions.
  - Tapping **`[ 📴 Hang Up ]`** exclusively terminates the call.
- **Enhanced In-Call Detection**:
  - Automatically identifies in-call notifications through action button titles (`hang up`, `end call`, `decline`, `answer`, `return to call`) and ongoing call status phrases.

### 📦 Checksum & Binary
- **APK**: `origin-island-pro-2.18.0.apk`
- **SHA-256**: `fccddd75a51bf279574b5bf550bccba0516b98ac38ea3172b7c89a17a20ee4c7`
