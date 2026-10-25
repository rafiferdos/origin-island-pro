# Origin Island Pro v2.10.0 🚀

### 💎 Next-Level App Optimizations: Gmail, Discord & Origin Ghost-Player Music

This major release delivers dedicated, bespoke notification engines for **Gmail** and **Discord**, along with a complete overhaul of the **Music Player** inspired directly by the architecture of [origin-ghost-player](https://github.com/fvhde/origin-ghost-player).

---

### ✨ What's New in v2.10.0

#### 1. 📧 Bespoke Gmail & Email Optimization (`MailCard`)
- **Compact Island Pill (Short State):**
  - **Left side:** Authentic Gmail icon + clean sender name (e.g. `Google`, `GitHub`, `Tanvir`).
  - **Right side:** Concise email subject snippet (e.g. `Security alert: New login...`) or unread counter.
- **Expanded SuperX Card:**
  - **Header:** Clean sender name and high-res avatar / Gmail icon.
  - **Body:** Bold subject line followed by a concise preview of the email body text.
  - **Subtext:** `Gmail • account@gmail.com` (or mailbox account).
  - **Semantic Action Buttons:** Dedicated vector icons with luxury color-coding:
    - **Archive** (`ic_archive` in Slate Indigo `#2C344E`)
    - **Reply** (`ic_reply` in Vibrant Emerald `#1E3A2F` / `#34C759`)
    - **Delete / Trash** (`ic_delete` in Crimson `#3A1E22` / `#FF453A`)
    - **Mark as Read** (`ic_mail` in Sapphire `#1E2D52`)

#### 2. 🎮 Bespoke Discord Engine (`DiscordCard`)
- **Context-Aware Routing:**
  - Distinctly handles **Server Channels** (e.g. `#general`), **Direct Messages (DMs)**, and **@mentions/pings**.
- **Compact Island Pill (Short State):**
  - **Left side:** Discord icon + `#channel` or `@Sender`.
  - **Right side:** Clean message preview snippet (or `🔔 @mention` alert when tagged).
- **Expanded SuperX Card:**
  - **Header:** `Sender • #channel` or `Sender (DM)`.
  - **Body:** Clean formatted message text (strips raw Discord snowflake IDs `<@12345>`, channel tags `<#12345>`, and custom emoji codes).
  - **Subtext:** `Discord • Server Name` or `Discord • Direct Message`.
  - **Action Buttons:** Reply (Discord Blurple `#5865F2`), Mark as Read (Sapphire `#1E2D52`), Mute (Dark Slate `#24242C`).

#### 3. 🎵 Ghost-Player Audiophile Music Architecture (`MediaCard`)
- **Real-Time Reactive Callbacks:**
  - Integrated `MediaSessionManager.OnActiveSessionsChangedListener` and `MediaController.Callback` (inspired by `origin-ghost-player`), eliminating polling latency and instantly updating track changes, play/pause states, and metadata with 0ms delay.
- **Compact Island Pill:**
  - Left side now displays **[Album Artwork] [Song Title]** together (no longer blank text!).
  - Right side renders a live multi-hue animated soundwave (`TEMPLATE_RIGHT_ISLAND_WAVE`) when playing, or sleek pause indicator when paused.
- **Expanded SuperX Card:**
  - Song title, artist name, and album subtitle (`Album • Spotify`).
  - High-res square-rounded album artwork with native ambient edge lighting (`oi_light_color` + `oi_light_mode = 1`) dynamically extracted from the album palette.
  - Luxury transport buttons: **Prev** (`ic_media_prev`), **Play / Pause** highlighted in Electric Sapphire (`#2D6BF5`), and **Next** (`ic_media_next`).

---

### 📦 Release Asset Verification
- **File:** `origin-island-pro-2.10.0.apk`
- **Size:** 24 MB
- **Signature:** v2 scheme verified (`apksigner verify --verbose`)
- **SHA-256:** `8a63c792bd54654d8df4943a92eb25d5fe398f3004822eb09b1397d1d3c26699`
