# Origin Island Pro v2.21.0 — Timer & Message / Discord Overhaul

### 🌟 What's New in v2.21.0

1. **Timer Card (`TimerCard.kt`):**
   - **Vibrant Tangerine Amber Capsule (`#FFFF9500`):** Capsule badge now renders with a striking orange background and bold white text (`#FFFFFFFF`), eliminating the drab system default chip.
   - **Interactive Semantic Buttons:** Dedicated color themes for timer controls:
     - **Pause / Snooze:** Radiant Amber (`#FFFF9500`) with white text.
     - **Resume / Start:** Emerald Green (`#FF34C759`) with white text.
     - **Reset / Stop / Cancel / Dismiss / Delete:** Crimson Red (`#FFFF3B30`) with white text.
     - **+1:00 Add Time:** Deep Blue Accent (`#FF1C2B4E`) with cyan-blue text.
   - **Ambient Edge Glow:** Expanded timer card emits a warm Tangerine Amber glow (`oi_light_color = "#FFFF9500"`).
   - **Punch-Hole Cutout Safe:** Left pill text limited to 10 characters to prevent status bar overflow.
   - **Broad Vendor & Chinese ROM Support:** Expanded detection across `com.vivo.clock`, `com.bbk.clock`, `com.google.android.deskclock`, `com.sec.android.app.clockpackage`, `com.coloros.alarmclock`, `com.xiaomi.clock`, `com.huawei.deskclock`, etc., with enhanced countdown calculation for past and future notification timestamps.

2. **Discord Card (`DiscordCard.kt`):**
   - **High-Contrast Capsule Badge:** Capsule renders with signature **Discord Blurple** (`#FF5865F2`) for DMs/server channels or **Warning Amber** (`#FFFF9500`) for `@mentions`.
   - **Ambient Edge Glow:** Expanded card illuminates with signature Discord Blurple or Mention Amber glow.
   - **Punch-Hole Cutout Safe:** Left channel name or sender handle capped strictly at 11 characters.

3. **Message Card (`MessageCard.kt`):**
   - **Dynamic Brand Colors & Badges:** Automatically applies signature brand colors to status bar capsule badges and ambient glows:
     - **Telegram:** Telegram Sky Blue (`#FF2AABEE`)
     - **WhatsApp:** WhatsApp Emerald (`#FF25D366`)
     - **Facebook Messenger:** Messenger Blue (`#FF0084FF`)
     - **Instagram:** Neon Berry (`#FFE1306C`)
     - **WeChat (Chinese ROM):** WeChat Green (`#FF07C160`)
     - **QQ (Chinese ROM):** QQ Blue (`#FF12B7F5`)
     - **DingTalk / Feishu:** Enterprise Blue (`#FF007AFF`)
     - **Signal:** Signal Blue (`#FF2C6BED`)
     - **IMO:** IMO Cyan (`#FF00B2FF`)
     - **Viber:** Viber Purple (`#FF7360F2`)
     - **Default SMS:** Royal Blue (`#FF007AFF`)
   - **Semantic Chat Action Buttons:** Quick "Reply" dynamically takes the app's brand color, "Mark as Read" renders in sleek Sapphire Slate, and "Delete/Mute" renders in Warning Slate.
   - **Chinese ROM OriginOS 6 App Ingestion:** Added native support for WeChat (`com.tencent.mm`), QQ (`com.tencent.mobileqq`), TIM, DingTalk, and Feishu.
