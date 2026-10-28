# Origin Island Pro v2.22.0 — Expanded Card UI/UX Overhaul

### 🌟 What's New in v2.22.0

1. **OTP & 2FA Verification Card (`OtpCard.kt` & `AuthPromptCard.kt`):**
   - **Template 8 Interactive Action Layout:** Switched from Template 4 (Base) to Template 8 (Buttons).
   - **Prominent `[ 📋 Copy Code ]` Action Button:** Expanded card now renders a large, high-density Electric Sapphire Blue (`#FF007AFF`) filled button. One tap copies the verification code to clipboard, displays a toast, and smoothly dismisses the card.
   - **Sapphire Ambient Edge Glow:** Glowing border matching the Sapphire Blue verification theme (`oi_light_color = "#FF007AFF"`, `oi_light_mode = 1`).
   - **Auth Prompts:** "Approve" (Emerald Green) and "Deny" (Crimson Red) semantic buttons with sapphire ambient glow.

2. **Download Card Dual Layout & Action Buttons (`DownloadCard.kt`):**
   - **Interactive Action Buttons in Progress Layout:** Injected `oi_button_titles`, `oi_button_bg_colors`, and `oi_button_text_colors` so downloads from Google Chrome, Play Store, and browsers show interactive **Pause** (Radiant Amber) and **Cancel/Stop** (Crimson Red) buttons right under the progress bar.
   - **Electric Download Blue Ambient Glow:** Dynamic edge lighting (`#FF007AFF`) around the expanded download card.

3. **Timer Card Buttons & Controls (`TimerCard.kt`):**
   - Expanded card features semantic interactive control buttons with customized palettes (Pause = Amber, Resume = Emerald, Reset = Crimson Red, +1:00 = Slate Blue).
   - Dynamic Tangerine Amber ambient glow (`#FFFF9500`) around the expanded card.

4. **Universal Ambient Edge Glow Across ALL Cards:**
   - Every single expanded card in Origin Island Pro now emits a bespoke, theme-matching ambient edge glow (`oi_light_color` & `oi_light_mode = 1`):
     - **Incoming & Ongoing Calls (`CallCard.kt`):** Emerald Green (`#FF34C759`)
     - **Downloads & Transfers (`DownloadCard.kt`):** Electric Blue (`#FF007AFF`)
     - **Payments & MFS (`PaymentCard.kt`):** Emerald Green (`#FF34C759`) / bKash Pink (`#FFE2136E`)
     - **Deliveries (`DeliveryCard.kt`):** Amber / Cyan (`#FF00D4FF`)
     - **Rides (`RideCard.kt`):** Radiant Amber (`#FFFF9500`)
     - **Meetings (`MeetingCard.kt`):** Royal Blue (`#FF007AFF`)
     - **Live Sports (`SportsCard.kt`):** Pitch Emerald (`#FF34C759`)
     - **Emails (`MailCard.kt`):** Gmail Coral Red (`#FFEA4335`) / Outlook Blue (`#FF007AFF`)
     - **Generic Cards (`GenericCard.kt`):** Royal Blue (`#FF007AFF`)

5. **New Interactive Sample:**
   - Added an interactive **"OTP / Login Code"** sample to the in-app Playground to let users preview the prominent `[ Copy Code ]` button and Sapphire glow.
