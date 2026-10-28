# Origin Island Pro v2.14.0 🎯

### Universal Deep-Linking into Nested Screens for All Apps (Messenger, WhatsApp, Gmail, Discord, etc.)

This release resolves the long-standing limitation where tapping an expanded card on the Dynamic Island would only open the source app's root/main screen. In v2.14.0, tapping the expanded notification card deep-links directly into the exact nested conversation, thread, email, or order for **all apps**.

---

### What's New in v2.14.0

#### 1. 🚀 Direct Nested Deep-Linking on Tap
- When an incoming notification arrives (e.g. from Facebook Messenger, WhatsApp, Telegram, Gmail, Discord, etc.):
  - Tapping the compact pill expands the rich island card.
  - **Tapping the expanded card now deep-links directly into that specific nested conversation, chat room, email thread, channel, or order** instead of opening the generic app inbox/launcher.

#### 2. 🔍 Root Cause Analysis & Fix
- **SuperX Notification Intent Attachment:** In previous versions, `PlaygroundService.postSuperX` created the underlying Android notification without attaching `.setContentIntent(clickResp)`. As a result, Android / vivo SystemUI saw a `null` `contentIntent` on the posted notification and fell back to `packageManager.getLaunchIntentForPackage(...)`, which only launched the root activity.
- `postSuperX` now explicitly sets `builder.setContentIntent(clickResp)` so the source app's original `PendingIntent` is invoked directly by SystemUI upon touch.

#### 3. 🛡️ Universal Deep-Link Intent Extractor (`extractDeepLinkIntent`)
- Centralized intent extraction in `CardIntents.kt` with multi-tier fallback:
  1. Primary `notification.contentIntent` specified by the source app.
  2. `notification.bubbleMetadata?.intent` (for conversation shortcuts & bubbles in Android 11+ like Messenger & Telegram).
  3. Safe non-RemoteInput action intents.
  4. Safe fallback to application launch intent.
- Integrated across all cards: `MessageCard`, `GenericCard`, `MailCard`, `DiscordCard`, `CallCard`, `DownloadCard`, `NavigationCard`, `RideCard`, `DeliveryCard`, `WeatherCard`, `MeetingCard`, and `PaymentCard`.

#### 4. ⚡ Comprehensive SuperX Click Key Coverage
- Updated `OriginIslandBuilder` to inject `clickResp` across:
  - Root SuperX bundle (`notification.superx.clickResp`).
  - All expanded card templates in `infos` (`notification.superx.infos.imageClickResp`, `notification.superx.infos.clickResp`, and `"clickResp"`).
  - Base info card (`notification.superx.baseInfos`).
  - Short info preview (`notification.superx.shortInfos`).
  - Dynamic Island capsule and bar (`island.superx.clickResp`).

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.14.0.apk`
- **Size:** ~24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `eef828631a6a82cad61cf40140ae56de86bc58a88141d745ef74d82725963046`
