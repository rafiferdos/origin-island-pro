# Origin Island Pro — God-Level Audit & Architecture Plan (OriginOS 7 / Native)

> Verified against `rafiferdos/origin-island-pro` @ `5f67e46` (v2.33.0), cloned to
> `C:\Users\rafif\dev\personal\origin-island-pro`.
> Every finding carries `path:line`. Evidence = read in source; Inference = reasoned, untested.
> Build state on this machine: **no JDK, no Android SDK, no Android Studio** — nothing was compiled.

---

## 0. What this repo actually is

| | |
|---|---|
| **Identity** | `applicationId = com.autonavi.minimap` (AMap spoof), namespace `com.originisle.android` — `app/build.gradle.kts:20,31` |
| **Purpose** | Listens to **every** notification on the device (`NotificationListenerService`), parses each into one of ~20 card types, and re-posts it as a **vivo SuperX / OriginIsland** notification (`tag = VIVO_SUPERX_TAG`) |
| **Platform gate** | Hidden `NotificationManager.setSuperXInfosSceneList(List,List,List,List)` by reflection — `OriginIslandBuilder.kt:53-84` |
| **Ship target** | OriginOS 6 (README:66 says "OriginOS 6 only") — but git HEAD says **"OriginOS 7 units v2.33.0"** (`git log -1`) |
| **Size** | 178 files, 15,095 LOC Kotlin, 23 JVM test files, 64 `@Test` methods |
| **Stack** | Kotlin 2.0.21, AGP 8.9.2, Compose BOM 2024.12.01, compileSdk 36, **minSdk 34, targetSdk 36** |

**The single most important sentence in this document:** the app's core mechanic is a
**package-name spoof**. `docs/DEV.md:8-18` states it plainly, and it is load-bearing:
change `applicationId` and the island stops working. Everything else in this plan is downstream
of that decision.

---

## 1. Where the repo is genuinely excellent (say it plainly, because it matters)

These are not nitpicks. The author has clearly run this on hardware and iterated:

1. **Silent-drop failure mode is fully understood.** `docs/PROTOCOL.md:112-135` documents exactly
   which SuperX preconditions vivo enforces (IMPORTANCE_HIGH channel, non-empty title/text,
   `operation=0`, a real `displays` bitmask, a required content intent, `forceShow=true`). That is
   reverse-engineering, not guessing.
2. **Rejection recovery.** When vivo strips the tag and stamps `SUPERX_VALUE=false`, the listener
   cancels its own shade fallback — `NotificationCastListener.kt:1169-1175`. That is a real bug
   found in the wild and fixed.
3. **Parcel-size discipline.** `shrinkForIcon` / `parcelSafeIcon` exist specifically because a
   full-resolution bitmap throws inside Binder and **crash-loops the listener on reconnect**
   (`CardIntents.kt:59-92`).
4. **Live Update extras are read by raw key** so they work against a core lib that predates the
   typed constants — `GenericCard.kt:118-120, 283-290`.
5. **Re-delivery dedup is postTime-gated**, with the exact reasoning in the comment
   (`NotificationCastListener.kt:350-361`). Same for `lastMatchIcons` (badge persistence) at
   `:331-338`.
6. **Battery-aware polling** — 1s ticks only while the screen is interactive, and polling stops
   entirely when nothing live exists (`NotificationCastListener.kt:420-501`).
7. **`docs/PROTOCOL.md`** is a real artifact: a verbatim key dictionary plus a *practitioner's*
   "what makes vivo accept the post" section. This is the repo's most valuable file after the code.
8. **64 focused JVM tests** covering parsing logic (OtpDetector, Delivery ETA, Download speeds,
   QuietHours, semantic button colours, UpdateChecker version compare).

None of the above should be thrown away in a redesign. They are the moat.

---

## 2. The OriginOS 7 reality check (what vivo actually documents)

Read from `dev.vivo.com.cn/documentCenter/doc/896` (the official 原子通知 spec) and cross-checked
against the repo:

### 2.1 What the repo implements vs. what vivo documents

| vivo spec field | Repo | Verdict |
|---|---|---|
| `notification.superx.operation` 0/1/2 | `OriginIslandConstants.kt:17` | ✅ implemented (uses **0**, never **1** — see §4.3) |
| `showNotify` | `:19` | ✅ |
| `template` 1/2/3/4/5 | `:18` | ✅ + extends to 8/9 |
| `baseInfos.{icon,title,content,subInfo,subText,subTextColor}` | `:47-68` | ✅ subInfo 0–6 all handled |
| `infos.*` per template | `:70-111` | ✅ |
| `capsule.{state,icon,content,contentColor,bgColor,showTime,clickResp}` | `:166-174` | ✅ |
| `island.superx.leftTemplate/leftInfo/rightTemplate/rightInfo` | `:122-164` | ✅ |
| `island.superx.{forceShow,showTime,clickType,priority,state}` | `:126-160` | ✅ |
| `island.superx.rightInfo.{waveColor,waveState,progress*,loadingColor}` | `:135-142` | ✅ |
| `lightEffectInfo.{mainColor,mode}` | `:177-182` | ⚠️ written (`OriginIslandBuilder.kt:270-276`) but **never verified on device** |
| `island.superx.{islandNotify,customSuperx,cardBgColor,keepScreenOn,disableInvertColor}` | `:39-43` | ⚠️ unverified vendor extensions |

### 2.2 Discrepancies worth acting on

| Finding | Evidence | Impact |
|---|---|---|
| **`rightTemplate` doc says 1=律动/2=进度/3=加载中/4=文本+图片/5=图片+文本/6=胶囊文本** | vivo spec §2.3 vs `OriginIslandConstants.kt:200-205` | ✅ **matches exactly** — good |
| **`rightInfo.content` is documented for templates 4/5; repo also writes `capsuleContent` for 6** | `OriginIslandBuilder.kt:562-574` | ✅ matches spec |
| **vivo says `leftInfo.content` supports a *timer* text** | spec §2.3: "支持计时器文本" | ❌ repo uses `leftTemplate=3` + `doublelineText` (`OriginIslandBuilder.kt:507,536-538`) — unverified on OS7 |
| **`island.superx.template` (big-card type) exists in vivo's spec** | spec §2.3 row | ❌ repo defines the key (`:129`) but **never writes it** |
| **`island.superx.showBarWhenCard`** | not in vivo spec | repo writes `false` (`:516`) — a vendor extension, presumably discovered by dumpsys |
| **`getSceneStatus(pkg, scene)` and `isSupportCustomFun(pkg, scene)`** | `OriginIslandBuilder.kt:127-132` (comment only) | ❌ **never called** — the app can't tell whether it's actually granted, so it can't adapt |
| **Only `NAVIGATION` is ever used as a scene** | `grep '"oi_scene"'` → 44 hits, all `"NAVIGATION"` | ❌ 15 scenes requested, 1 used |

### 2.3 OriginOS 7-specific facts that change the plan

- **OriginOS 7 = Android 17** (GSMArena, vivo global launch 21 Sep 2026). Repo `targetSdk = 36`.
- **OriginOS 7 opens the island to arbitrary audio apps** — r/Vivo thread *"Origin OS 7 finally
  allows any app that plays audio to integrate with Origin Island… There's no need to interfere
  with Package IDs any longer."* Users report the app must use Android 14+ **Playback Notifications**
  (i.e. `MediaSession`) to appear, and there's a new Settings path:
  **Settings → Origin player → All apps → toggles**.
  **Inference:** this is the strongest signal in the whole research that the AMap spoof may be
  **unnecessary on OS7**. It must be verified on-device before planning around it.
- OS7 ships **temporary notification muting**, **one-tap group clear**, **AirPods battery in Origin
  Island**, **Spatial Audio**, **Apple Watch notifications**. None are represented in the repo.
- Android 16/17 **Live Updates** (`ProgressStyle`, `POST_PROMOTED_NOTIFICATIONS`) are already
  partially handled by the repo (`GenericCard.kt:166-294`) — this is a strength to build on.
- Android 17 **background audio hardening** — the repo has no audio playback, so **not affected**.

---

## 3. Top findings, ranked by user impact

Each is a defect or a design debt, with the location that proves it.

### 🔴 P0-1 — The AMap package spoof is a single point of total failure
`app/build.gradle.kts:31`, `docs/DEV.md:6-18`.
Consequences, all already documented by the author: cannot coexist with real AMap; cannot ship on
Play; breaks the moment vivo's OS7 whitelist opens up. **This is the #1 strategic decision in the
project.** See §5.1.

### 🔴 P0-1b — The APK update pipeline installs with **zero verification**
`UpdateChecker.kt:168-299` (download), `:308-342` (install), `UpdateDialog.kt:473-475`, `:76`.
Three independent defects, each sufficient on its own:
1. **No signature check.** Zero references to `GET_SIGNATURES` / `signatures` / `MessageDigest`
   anywhere in `UpdateChecker.kt` or `UpdateDialog.kt`. `installApk` checks only
   `apkFile.exists()` (`:309`) and `canRequestPackageInstalls()` (`:315`) before firing
   `ACTION_VIEW`. The project *publishes* a SHA-256 per release (`docs/DEV.md:71-73`,
   `README.md:86-90`) — the app never downloads or checks it.
2. **Cached APK trusted on byte length only.** `targetFile.exists() && info.apkSize > 0 &&
   targetFile.length() == info.apkSize` → straight to "Completed" + install notification
   (`:178`; same in `UpdateDialog.kt:76`). A leftover/partial/other APK of identical length is
   trusted.
3. **Manual redirect following, host-agnostic, no TLS pinning.** Any 3xx `Location` header is
   followed with no allowlist (`:189-209`), *and* `instanceFollowRedirects = true` is also set
   (`:192`), so both mechanisms run. `apkUrl` comes from the GitHub API (`:119`). No
   `TrustManager`, `HostnameVerifier`, `CertificatePinner`, or `network_security_config.xml`
   anywhere in the repo.

The install path is reachable from three places (dialog button, download-complete notification,
cached-update replay in `HomeScreen.kt:144-155`) — all three unverified. For an app that reads
every notification on the device, this is the difference between "an update" and a supply-chain
hole. **Fix is §2.5.**

### 🔴 P0-1c — `cast_enabled_apps` is dead: the allow-list branch is unreachable
`NotificationCastListener.kt:602-607`. `isAppEnabled` reads `cast_enabled_apps` (`:605`), but
**nothing in the repo ever writes that key** (`grep 'putStringSet("cast_enabled_apps"'` → no
results). So the function always returns `true` for any non-ignored package, and the doc comment
at `:598-600` ("The Apps tab's per-app switch is a single master kill switch") describes intent
that is only half-implemented — the deny set (`cast_ignored_apps`) works, the allow set does not.

### 🔴 P0-2 — `HomeScreen.kt` is a 1,493-line god composable with 53 state vars
`HomeScreen.kt:90-120` (34 in the first block alone), and a second batch at `:1110-1112`.
`prefs.getBoolean(...)` is read **during composition** and the result is captured into
`remember` — so any change written by another screen, by the listener, or by `onResume` is
**invisible until the composable leaves composition**. There are **55 `prefs.edit()...apply()`**
calls inside the UI layer (`grep -c 'prefs.edit()' ui/` → 55) and **22 `getSharedPreferences`**
reads in main code. This is the "state lives in the wrong place" defect, at scale.

Two amplifying defects:
- **No UI file registers an `OnSharedPreferenceChangeListener`** (grep of `ui/*.kt` → nothing).
  The service does (`NotificationCastListener.kt:539`); the UI never observes it. Writes are
  self-consistent only because the same lambda also sets the local state var.
- **State is destroyed on every navigation.** `MainActivity.kt:90-99` switches pages with
  `when(page)` inside a `Box`, so the non-active branch leaves composition and its `remember`
  state (menu open, `updateStatus`, `activeUpdateInfo`, `castOn`…) is discarded. Only `page` is
  `rememberSaveable` (`:79`). This masks the staleness problem by accident.

### 🔴 P0-3 — The dispatch cascade is a 416-line if/else ladder with no extension point
`NotificationCastListener.kt:645-1060` — **~50 branches, 34 early `return`s** (measured). Order is
load-bearing (OTP before payment before plain-message, `:723-725`/`:831-835`) but exists only as
source position. Adding card #21 means finding the right line in a 415-line method. There is **no
registry, no priority table, no `CardHandler` interface** — 20 card `object`s, each posting an
`Intent` full of stringly-typed `oi_*` extras. `grep '^object' cards/` → 20 hits, `grep 'interface
' cards/` → **nothing**.

The three `post()` shapes are incompatible, so a new card author has to guess which to copy:

| Shape | Cards | Signature |
|---|---|---|
| parses the SBN itself | Generic, Message, Media, Call, Otp, Download, Mail, Navigation, Raiden, Discord | `post(context, sbn, …)` |
| pre-parsed primitives | Timer, Delivery, Ride, Weather, Meeting, Payment, AuthPrompt | `post(context, id, title, …, clickResp, sourcePkg)` |
| domain-specific | Sports, Charging | — |

**Detection ownership is split**: ride/delivery/weather/meeting/timer/payment/nav/message
detection lives in the listener (`:850-852, 884-886, 918-920, 945-948, 1097-1109, 1245-1263,
1086-1094, 1068-1075`) re-using the card's keyword lists, while call/download/mail/discord/raiden
detection lives in the card. Four near-identical `isApp || hasKeyword` blocks are copy-pasted
(`:850-852, 884-886, 918-920, 945-948`).

### 🔴 P0-4 — `buildBundle` has 63 parameters and writes the same value under up to 5 keys
`OriginIslandBuilder.kt:182-253` (signature — 63 named params), and the duplication is visible at
`:277-288` (clickResp written 2×, landingPkg written 4×), `:327-339` (5×), `:481-487` (5×),
`:521-528` (4×), `:592-600` (5×). The same value is written under the canonical key, the bare key,
and the prefixed key, on the theory that vivo reads one of them. That's not a design, it's
survival.

### 🟠 P1-5 — `PriorityArbitrator` is half-dead
`PriorityArbitrator.kt:29-47` defines `priorityForKind(kind)` — **called from nowhere in main
source** (only a unit test, `RaidenCardTest.kt:124-125`). Each card hardcodes its own constant
(`CallCard.kt:323`, `MediaCard.kt:193`, …). Worse, `GenericCard.kt:177` uses a **magic `65`** that
exists nowhere in `PriorityArbitrator`. And `getRaidenPriority` reads `raiden_always_on_top`
(`:54-59`) while `PlaygroundService.kt:414` reads the same pref **independently** — the two will
drift. The declared "Semantic Priority Matrix" (`:6-8`) is not actually the arbitration engine.
The real policy is `p >= PRIORITY_TIMER` in three places (`PlaygroundService.kt:415-417, 435,
576-578`), and it's check-then-act, safe only by main-thread confinement.

### 🟠 P1-6 — The protocol layer is not unit-testable
`OriginIslandBuilder.buildBundle` takes `Context` and calls
`context.resources.getColor(...)` (`:372,376,556`). So the wire format — the single most
valuable, hardest-won part of the project — **cannot be asserted in a JVM test**. The repo's own
`docs/PROTOCOL.md` says the format was recovered by trial and error on hardware; there is nothing
preventing a regression from silently changing it. Compare: `originos-toolkit` solved exactly this
with a sink interface + 13 JVM tests (referenced in its changelog).

### 🟠 P1-7 — The `oi_*` Intent contract is undocumented and untyped
47 distinct `oi_*` string extras (`grep -rhoE '"oi_[a-z_0-9]+"' app/src/main/java | sort -u` → 47). They are produced
by 20 card objects and consumed by `PlaygroundService.readAndPost` (`:208-332`). Nothing enforces
the contract. `oi_priority` is set by 18 cards but only ever compared against
`PRIORITY_TIMER` in one place (`:435`).

### 🟠 P1-8 — `RaidenCard` sits inside the notification id band; request codes collide
`IconCache.castIdFor` yields `[20000, 29999]` (`IconCache.kt:23-24`). But
`RaidenCard.RAIDEN_CAST_ID = 29500` (`RaidenCard.kt:38`) and its `getService` request code
`29501` (`:377`) are **inside** that band — directly contradicting the band comment at
`ChargingCard.kt:25` ("outside the notification (20000-29999)… band"). A real notification hashing
to 29500 fights RaidenCard for the island id; 29501 collides with `OtpCard`'s PendingIntent
request code (`OtpCard.kt:40`). Separately, `pkg.hashCode()` is reused as a request code by both
`CardIntents.kt:54` and `RaidenCard.kt:470` with `FLAG_UPDATE_CURRENT`, so a same-package pair
silently keeps the first PI's base Intent and swaps only extras. Low probability (1/10000 per key),
silent and persistent when it hits.

### 🟠 P1-9 — Text heuristics are English-first; Bengali is an afterthought
91 `contains("…")` string checks across `cards/` + `util/`. `DownloadCard.kt:58` is the **only**
place with Bengali keywords (`"ডাউনলোড","আপডেট","ট্রান্সফার"`). The user is in Dhaka; bKash/Nagad
notifications are often Bangla. `IntelligentTextSanitizer` has a 40-entry English honourific list
(`:29-44`) including South-Asian titles — but the boilerplate prefix/suffix regexes
(`:12-25`) are English-only, so a Bangla "tap to open" suffix survives into the pill.

The sharpest single predicate is **`isRecapOrHighlights`** (`NotificationCastListener.kt:1177-1184`):
`hay.contains("recap") || hay.contains("highlight") || hay.contains("position")`. `"position"`
is a generic English noun — a score-app notification saying "Your position in the queue" or
"league position changed" is **permanently suppressed** from the live-score path. That is a real
false-negative, not a style issue.

Detection is also locale-inconsistent: `Notification.category` / `EXTRA_TEMPLATE` are the
locale-proof signals and are used by MailCard (`:54`), CallCard (`:76`), and
`isMessageNotification` (`:1071`) — but **not** by DeliveryCard, RideCard, WeatherCard, or
MeetingCard, which fall back to English prose matching.

### 🟡 P2-10 — `IconCache` never evicts on its own and `castIdFor` can collide
`IconCache.kt:23-24`: `(key.hashCode() and Int.MAX_VALUE) % 10000 + 20000`. Hash-derived ids mean
two distinct notifications can share a card id, and `IconCache` has **no size bound** (unlike
`CastLog`, which caps at 200 — `CastLog.kt:22`). `MediaCard` registers a `MediaController.Callback`
per post (`MediaCard.kt:46,65-82`) — a callback leak if `forget()` isn't reached.
Eviction is only `clear(id)` on notification removal; match cards (30000-34999), charging (35001)
and samples (40000+) never call it — harmless today only because they don't write to it, and
nothing enforces that.

### 🟡 P2-11 — Reflection is cached as permanently unavailable on first failure
`OriginIslandBuilder.kt:45-65`: `scenesUnavailable = true` is set on **any** `NoSuchMethodException`
and never reset. If the first call happens before the service is ready, or on a device where the
method name differs, the app gives up forever. `ensureEnabled` heals the *component* but not this
flag. **Evidence: the flag is written at `:62` and read at `:54`, never cleared.**

### 🟡 P2-12 — `KeepAliveAccessibilityService` is a real permission-footprint smell
`KeepAliveAccessibilityService.kt:18-32`. It is honest (empty `onAccessibilityEvent`,
`canRetrieveWindowContent=false`), but it exists purely to keep the process alive. PlaygroundService
already carries a comment saying the accessibility anchor was **abandoned** because a swipe-away
kill leaves it CRASHED and never rebound (`PlaygroundService.kt:174-178`) — yet the service is
still declared in the manifest (`AndroidManifest.xml:81-93`). **Evidence: the manifest entry and
the code comment contradict each other.** Technically legitimate for a documented personal
sideload; unshippable on any store.

### 🟡 P2-13 — Accessibility in the Compose UI is minimal
**7** `contentDescription` occurrences and **3** `semantics {}` blocks across **all** of
`main/java`; `HomeScreen.kt` has **zero**. `IconTile` always passes `null` (`Components.kt:169`),
so every settings-row leading icon is unlabelled. Touch targets: `CircleIconButton` **44 dp**
(`Components.kt:85`), `ChoiceChip` **44 dp** default with overrides at **36 dp**
(`HomeScreen.kt:1022`) and **32 dp** (`ActivityScreen.kt:100`), `SegmentedControl` **40 dp**,
`StatusPill` **32 dp** — all below the 48 dp minimum. Whole sections are "disabled" purely via
`Modifier.alpha(0.4f)` (`HomeScreen.kt:333,404,805,817`) with **no `semantics { disabled() }`**,
so TalkBack announces them as enabled. 8 of 18 tile colours fail the 3:1 non-text contrast
requirement for the white glyphs (e.g. white on `TileRide #FFB800` = **1.73:1**, on
`TileTimer #FF9500` = 2.2:1). `strings.xml` has **4** strings; 14 user-facing strings are
hardcoded inline in the UI.

### 🟡 P2-14 — No static analysis, no lint, no CI that builds
`.github/workflows/` contains **only** `verify-release.yml` (checksum check on release, nothing
else). No `ktlint`, no `detekt`, no `.editorconfig`, no `lint { }` block in `build.gradle.kts`.
64 tests exist and would catch regressions — but nothing runs them automatically.

### 🟡 P2-15 — `targetSdk = 36` while the stated target is OriginOS 7 / Android 17
`app/build.gradle.kts:33`. `compileSdk = 36` is justified in a comment (`:22-23`: "matches the
platform installed on this machine"), but **targetSdk 36 means the app opts out of Android 17
behavior changes** — which is backwards for an app whose entire purpose is a vendor feature that
only exists on the newest OS.

### 🟡 P2-16 — Copy-paste proliferation across the card layer
Measured duplication, each with a canonical form that exists but isn't used:
- **7 copies** of "which actions are usable" — canonical `GenericCard.usableActions`
  (`GenericCard.kt:303-307`); inline weaker copies at `TimerCard.kt:48`, `RideCard.kt:62`,
  `MeetingCard.kt:55`, `AuthPromptCard.kt:53`, `CallCard.kt:304`, `DownloadCard.kt:398`. The
  inline copies **dropped** the `remoteInputs`/`isContextual`/reply checks, so DownloadCard will
  render a button that fires an empty RemoteInput reply.
- **6 copies** of the semantic button-colour `when` (`MessageCard.kt:87`, `TimerCard.kt:60`,
  `DownloadCard.kt:413`, `AuthPromptCard.kt:66`, `MailCard.kt:235`, `DiscordCard.kt:308`) — six
  divergent palettes for a concept `OriginIslandBuilder.resolveSemanticButtonBgColor` already
  centralises (and only CallCard uses it, `CallCard.kt:310`).
- **4 copies** of the `shortName`/`shortSender` `TextPaint`+`ellipsize` scaffolding
  (`MessageCard.kt:275`, `MailCard.kt:260`, `DiscordCard.kt:337`, `MediaCard.kt:258`).
- **4 copies** of `\s*\(\d+.*?\)$` / `\s*\[\d+.*?\]$` sender cleanup (`MessageCard.kt:265,237,277`;
  `MailCard.kt:246`; `DiscordCard.kt:331`).
- **9 copies** of `Icon.createWithResource(pkg, …getApplicationInfo(pkg,0).icon)` (5 in cards, 4 in
  the listener at `:747, 858, 893, 954, 1267`).
- **3 copies** of a private `String.clip` extension (`MessageCard.kt:299`, `MailCard.kt:275`,
  `DiscordCard.kt:352`); **2 copies** of `shrinkForIcon` (canonical `CardIntents.kt:66` max 200,
  private `CallCard.kt:410` max 192); **2 copies** of `DURATION_REGEX` (`CallCard.kt:65`,
  `NotificationCastListener.kt:782`); **2 copies** of `formatElapsed` (`CardIntents.kt:95`,
  inline at `NotificationCastListener.kt:771-785`).
- **Dead code:** `MessageCard.shortName` (`:275-297`) and `DiscordCard.shortSender` (`:337-350`)
  are private and never called.
- **Dead prefs:** `cast_enabled_apps` (never written — see P0-1c), `cast_gemini_view_glow`
  (`OriginIslandBuilder.kt:176`, never written), `cached_update_release_url`
  (`UpdateChecker.kt:371` → `HomeScreen.kt:153` → never consumed).
- **`SYSTEM_PKGS` duplicated byte-identically**: `NotificationCastListener.kt:172-178` ≡
  `RaidenCard.kt:63-69`. `com.microsoft.teams` appears in four separate lists.
- **`PREFS_NAME` duplicated 3×** plus one raw literal: `Prefs.kt:4`, `RaidenCard.kt:39`,
  `NotificationCastListener.kt:162`, `PriorityArbitrator.kt:55`. Mode flag inconsistent (`0` in
  ~15 sites vs `Context.MODE_PRIVATE` in `MainActivity.kt:61/76`, `UpdateChecker.kt:348`).

### 🟡 P2-17 — Unverified protocol claims that need hardware, not reading
Two evidence conflicts the code alone cannot resolve — both must go in the Phase 0 device run:
1. **`forceShow`.** `docs/PROTOCOL.md:129-131` calls `island.superx.forceShow = true` mandatory
   ("without it, a card can be accepted… but never actually surfaces"). Only **6 of 19** cards set
   it (`SportsCard.kt:156`, `PaymentCard.kt:152`, `OtpCard.kt:90`, `NavigationCard.kt:115`,
   `ChargingCard.kt:109`, `CallCard.kt:325`). GenericCard — the catch-all — does not, yet
   NavigationCard.kt:32-33 claims hardware-confirmed rendering. Either the doc overstates it for
   this build, or there is a latent surfacing bug. `dumpsys notification_manager` decides it.
2. **Template 2 (ProgressVisual).** Used as the expanded body by `DownloadCard.kt:429`
   (unconditional) and `GenericCard.kt:279` (Live Updates). `docs/PROTOCOL.md:108` says only
   template 9 is confirmed rendering on hardware. A dropped template 2 leaves downloads with no
   expanded progress track and no fallback path.

### 🟡 P2-18 — `RaidenStateReceiver` permission is `normal`, and UID-only sender checks can be bypassed
`AndroidManifest.xml:15-18` declares `com.originisle.android.permission.RAIDEN_STATE` at
`protectionLevel="normal"`. A normal permission is **auto-granted to any app that declares it**, so
the manifest-level "protection" is illusory.

The receiver *does* run a real sender check (`RaidenStateReceiver.kt:27-43`): self, shell (UID
2000), root (UID 0), or the confirmed Muse package UID. That is better than the manifest suggests.
But the check is UID-only, and **the manifest also declares `android:exported="true"`**
(`AndroidManifest.xml:115-116`), so the attack surface is wider than "a confirmed app can poke it":
1. The receiver accepts arbitrary `state`/`task`/`file` strings from any caller that passes the UID
   check (`RaidenStateReceiver.kt:45-53`) — and `state`/`task` are rendered straight onto the
   island card (`RaidenCard.sanitizeTask`, `RaidenCard.kt:192-233`).
2. `scripts/raiden-bridge.sh` sends from ADB (UID 2000) by design, so **shell is permanently
   trusted**; the UID check offers no protection against anything running as shell or root.
3. A `protectionLevel="normal"` permission cannot be relied on to stop a third-party app from
   *declaring* it, so the manifest declaration gives false confidence.

**Fix:** add a per-install random token (generated in `OriginIsleApp.onCreate`, stored in prefs)
that the bridge must include and `onReceive` must verify. That keeps the ADB bridge working while
making a third-party app's broadcast fail. Raise the protection level to `signature` only if the
shell bridge is dropped. Keep `exported="true"` (the bridge needs it) and document the trade-off.

---

## 4. Design debt that matters (the "how should it be done" half)

### 4.1 The card pipeline has no seam
```
NotificationListenerService
        │
        ▼
  onNotificationPosted()  ← 415-line if/else ladder (645-1060)
        │
        ▼
  20 × object XCard { fun post(context, sbn, ...) }
        │  (each builds an Intent with ~90 possible string extras)
        ▼
  PlaygroundService.readAndPost()  ← 125-line extra-parsing switch (208-332)
        │
        ▼
  OriginIslandBuilder.buildBundle()  ← 57 params, 605 lines
        │
        ▼
  NotificationManager.notify(VIVO_SUPERX_TAG, id, n)
```
Every arrow is an untyped string. There is no place to put a test, a metric, or a new card
without touching four files.

### 4.2 The `oi_*` contract should be a type
Today: `intent.getIntExtra("oi_template", 0)` in 48 places. The fix is a single
`@Parcelize data class SuperXRequest(...)` (or a `Bundle` written through one `BundleCodec`), so
that adding a field is a compile error at every call site instead of a silent `0`.

### 4.3 `operation` is always 0 — vivo's spec says update = 1
`OriginIslandBuilder.kt:205` (`operation: Int = 0`), and `docs/PROTOCOL.md:122` says the original
posts with 0. But vivo's own spec is explicit: **0=create, 1=update, 2=end**, and
`docs/PROTOCOL.md:122-123` even warns that "a non-zero *update* operation on a card that doesn't
exist yet is also dropped". Posting `0` on every update means vivo treats every refresh as a
create. It works because `notify()` with the same id replaces — but it forgoes `changeRecord`
ordering semantics (`OriginIslandConstants.kt:31`) that the spec says protects against stale
updates. **Inference: this is latent, not currently broken — but it's the kind of thing that
breaks on an OS update.**

### 4.4 Scene strategy is 1 of 15
`grep '"oi_scene"'` → 44 hits, **all `NAVIGATION`**. `OriginIslandConstants.SUPERX_SCENES` lists
15 (`:245-249`). `docs/PROTOCOL.md:25-30` records the finding that only `NAVIGATION` was reliably
granted on the test device. So the app requests 15, uses 1, and has no runtime way to find out
which were granted — because `getSceneStatus` is never called (`OriginIslandBuilder.kt:127-132`).
**The plan must add a scene-capability probe; it's a 20-line change that unlocks the rest.**

### 4.5 No state store — everything is a process-global `object`
`IconCache`, `CastLog`, `PriorityArbitrator`, `OriginIslandBuilder`, all 20 cards, plus
`PlaygroundService.activeCardIds` / `activeCardPriorities` / `queuedRaidenIntent` as
`@Volatile` statics (`PlaygroundService.kt:57-69`). Process death loses all of it. On a device
that kills the process on every swipe, that is the normal case, not the edge case.

---

## 5. The god-level plan

Ordered so that each phase makes the next one cheaper. Phases 0–2 are the ones that change the
trajectory; 3–6 are craftsmanship.

---

### Phase 0 — Resolve the platform question (blocking, 1–2 days, device required)

**Everything else depends on this.** The AMap spoof determines the app's distribution model, its
package identity, its `queries` block, and whether it can ever be more than a sideload.

| # | Task | Owner | Done when |
|---|---|---|---|
| 0.1 | On the OriginOS 7 device, install the app with `applicationId` changed to `com.originisle.android` (no spoof) and check `logcat` for `setSuperXInfosSceneList unavailable` | You + me | Log line captured, screenshot |
| 0.2 | If 0.1 works: is the island actually rendered? (`dumpsys notification_manager \| grep -i superx`) | You | Card visible / not visible, recorded |
| 0.3 | If 0.1 fails: enumerate which package names OS7 still grants. Try the vivo-documented route — **原子岛接入权限申请** (the Aliyun doc says you must *apply* for access: "已参考原子通知接入指导申请原子岛接入权限") | You | Application submitted or whitelist confirmed impossible |
| 0.4 | Check whether OS7's new **Settings → Origin player → All apps** toggle makes a non-spoofed `MediaSession`-only app appear on the island | You | Toggle path confirmed |
| 0.5 | Decide the distribution identity: stay spoofed (personal sideload, current model) **or** move to a real `applicationId` + official vivo whitelist | You | Written decision |
| 0.6 | **Resolve the `forceShow` conflict** (P2-17): post a card *without* `oi_force_show` and check `dumpsys notification_manager` for whether it surfaced | You | Doc updated with the real answer |
| 0.7 | **Resolve the template-2 conflict** (P2-17): post a DownloadCard (template 2 expanded body) and confirm whether the progress track renders | You | Confirmed / dropped, recorded |
| 0.8 | Walk all 15 `SUPERX_SCENES` with a sample card each and record which vivo actually grants (Phase 1.3's probe will make this a runtime answer, but the baseline matters) | You | 15-row table in `docs/PROTOCOL.md` |
| 0.9 | Confirm the **OS7 `operation` semantics**: does `operation=1` on an existing card update it, or get dropped? (§4.3) | You | `docs/PROTOCOL.md` §2 updated |

**If 0.1 succeeds**, Phases 1–6 below become far more valuable, because the app stops being
hostage to a package name.

---

### Phase 1 — Native-architecture skeleton (1–2 weeks)

Goal: introduce seams **without changing behaviour**. Every step is a pure refactor with the
existing 64 tests as the safety net, plus new tests for anything newly extracted.

**1.1 `CardHandler` registry — kill the 416-line ladder**

```kotlin
interface CardHandler {
    val id: String
    /** Higher wins. Ties broken by [order]. */
    val priority: Int
    val order: Int
    /** Fast, side-effect-free inspection. Return null to decline. */
    fun handle(ctx: CardContext, sbn: StatusBarNotification): CardDecision?
    fun onRemoved(ctx: CardContext, sbn: StatusBarNotification)
}

data class CardDecision(
    val request: SuperXRequest,
    val dedupeKey: String? = null,
    val autoDismissSec: Int = 0,
)
```

- Replace `NotificationCastListener.kt:645-1060` with a sorted registry walk.
- **Keep the current order exactly** as `order = 0..19` — the ordering is the spec, and it's
  currently only encoded as line position. Making it explicit is the point.
- Result: adding card #21 = one file + one `CardHandler` entry. No edit to the listener.
- **Collapse the three incompatible `post()` shapes** into one: each handler either takes the SBN
  or the pre-parsed primitives, and both return a `SuperXRequest`. Unify the detection ownership
  too — either the handler owns detection (preferred) or it exposes a pure
  `matches(sbn): Boolean` the registry calls.
- **Hoist the four copy-pasted `isApp || hasKeyword` blocks** (`:850-852, 884-886, 918-920,
  945-948`) into one shared `AppKeywordMatcher` that the handlers call.

**1.2 `SuperXRequest` value type — kill the 47 `oi_*` strings**

```kotlin
@Parcelize
data class SuperXRequest(
    val id: Int,
    val scene: SuperXScene,
    val template: Int,
    val rightTemplate: Int,
    val title: String, val content: String, val leftContent: String, val rightContent: String,
    val priority: Int,
    val icons: IconSet,
    val actions: List<CardAction>,
    val displays: Int,
    val live: Boolean,
    val extras: Bundle,          // escape hatch, shrinking over time
) : Parcelable
```

- `PlaygroundService.readAndPost` (`:208-332`) becomes a single `when` over a typed enum, or is
  deleted entirely.
- `Intent` extras become one `putParcelable("request", request)`.
- **Every `oi_*` key becomes a compile error at its call site.** That is the win.

**1.3 Scene capability probe — make the 15 scenes usable**

- Implement `getSceneStatus(pkg, scene)` and `isSupportCustomFun(pkg, scene)` reflectively
  alongside `grantScenes` (`OriginIslandBuilder.kt:53-84`).
- Cache the result per scene. **Stop requesting scenes that come back false.**
- Replace the 44 hardcoded `"NAVIGATION"` sites with a `sceneFor(cardType)` function that returns
  the best granted scene (NAVIGATION for nav, TAXI for rides, TAKEOUT/DELIEVERY for delivery,
  METTING for meetings, TIMER for timers, INCALLING/VOIPCALL for calls).
- This is where the app stops being a one-scene app.

**1.4 Fix the `scenesUnavailable` permanent-failure cache**
`OriginIslandBuilder.kt:45-65`. Add a retry-after-N-minutes policy, and re-probe on
`onListenerConnected`. Currently one early failure disables the feature for the process lifetime.

**1.5 Make `buildBundle` testable — split pure from Android**
`OriginIslandBuilder.kt:182-253`.
- `buildBundlePure(request: SuperXRequest, defaults: ColorDefaults): Bundle` — no `Context`.
- `buildBundle(context, request)` resolves `R.color.*` then delegates.
- Add **13–20 JVM tests asserting the exact key set and value types** for each of templates
  1,2,3,4,5,8,9 and right-island 1–6. This is the highest-value test work in the plan: it pins
  the reverse-engineered protocol so a refactor can never silently break it.

---

### Phase 2 — State, lifecycle, correctness (1–2 weeks)

**2.1 Kill the 53 `remember { mutableStateOf(prefs.getBoolean(...)) }` vars**
`HomeScreen.kt:90-120,1110-1112`.
- Introduce `PrefsState` (a single `data class` of all 40+ settings) exposed as `StateFlow` from a
  `SettingsRepository` backed by `SharedPreferences` + `Flow` (`preferencesDataStore` or a simple
  `MutableStateFlow` refreshed on write).
- Screens collect it. Writes go through the repository, so **every** screen sees the change — and
  register the repository's flow in place of the missing
  `OnSharedPreferenceChangeListener` (no UI file has one today).
- Delete the 55 inline `prefs.edit()` calls in `ui/` (`grep -c` → 55).
- Delete the 53 duplicated state vars.
- Move `HomeScreen`'s `LaunchedEffect(Unit) { while(true) { …; delay(1000) } }` polling loop
  (`:176-192`) and `ActivityScreen`'s (`ActivityScreen.kt:50-52`) onto a flow from the listener;
  a 1 Hz recomposition of the whole screen is not how a status pill should work.
- Hoist the `HomeScreen` `LaunchedEffect` that replays update-intent extras (`:139-170`) into the
  update checker / a navigator.

**2.2 Move `HomeScreen.kt` apart**
1,493 lines. `grep -n '^@Composable|^private fun'` shows the shape:
| Lines | Contents | Becomes |
|---|---|---|
| 82-935 | the screen body (852 lines) | `HomeScreen` (thin) |
| 937-967 | `StatusPill`, `MenuItem` | `Components.kt` |
| 970-1042 | `SampleTester` | `SampleTester.kt` |
| 1082-1106 | `StatChip` | `Components.kt` |
| 1108-1251 | `CameraCutoutTuner` (139 lines) | `CameraCutoutTuner.kt` |
| 1253-1420 | `StatusBarPreviewFrame` (168 lines) | `StatusBarPreviewFrame.kt` |
| 1426-1493 | `postStatusBarTest` (68 lines) | `samples/` or service |

The worst single structural item is the **`Cards` group at 402-801: ~400 lines inside one
`LazyColumn` `item {}`**, holding 15 independent settings with nested conditional chip rows. Split
it into at least six composables (`CardToggles`, `QuietHoursSection`, `CastToggleGroup`,
`NotificationSettings`, `UpdateSection`, `CutoutSection`).

Target: 5-8 files, each under 350 lines, no composable over 60 lines.

**2.3 `IconCache` — bound it and make ids collision-safe**
`IconCache.kt:23-24`.
- `castIdFor` → stable id derived from the notification key **with a collision map**, not a bare
  `% 10000`. Or use the notification's own `id` where the source app is unique.
- Add an LRU cap (say 40 entries) with eviction, matching `CastLog`'s discipline (`CastLog.kt:22`).
- `MediaCard`'s callback registry (`MediaCard.kt:46,65-82`) needs the same eviction; today a
  session destroyed without `forget()` leaks a callback.
- **Move reserved ids out of the notification band.** `RaidenCard.RAIDEN_CAST_ID = 29500`
  (`RaidenCard.kt:38`) and its request code 29501 (`:377`) sit inside `[20000,29999]`, which
  `IconCache.castIdFor` produces (`IconCache.kt:24`) — contradicting the band comment at
  `ChargingCard.kt:25`. Put Raiden at 35001+ and fix the request codes.
- Replace `pkg.hashCode()` as a `PendingIntent` request code (`CardIntents.kt:54`,
  `RaidenCard.kt:470`) with a stable per-purpose constant; two same-package PIs with
  `FLAG_UPDATE_CURRENT` silently keep the first one's base Intent.

**2.4 Fix the `PriorityArbitrator` drift**
- Delete `priorityForKind` or **make it the single source** and call it from every card.
- Replace the magic `65` at `GenericCard.kt:177` with a named constant.
- Collapse the duplicate `raiden_always_on_top` read (`PlaygroundService.kt:414` +
  `PriorityArbitrator.kt:56`) into one function.
- Give `getRaidenPriority` a pure sibling (`getRaidenPriority(isLive, alwaysOnTop: Boolean)`) so
  the policy is testable without Robolectric; the existing test covers only `priorityForKind`.
- Replace the check-then-act arbitration in `PlaygroundService.kt:415-417 → 442-452` with a single
  state transition in one owner.

**2.5 UpdateChecker — verify before install (security)**
`UpdateChecker.kt:308-342`. Three fixes, in order:
- **Verify the signing certificate** of the downloaded APK against the published SHA-256
  fingerprint (`docs/DEV.md:94-98`) before `ACTION_VIEW`. Use
  `packageManager.getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` on API 28+, compare the
  SHA-256 of the signing cert to the pinned value. Mismatch → delete the file, show a clear
  error, never install. The fingerprint must be configurable (a resource or a value fetched from
  the repo), not hardcoded, so a key rotation isn't an app update.
- **Verify the release `.sha256` asset** when present, in addition to the signature. The project
  already publishes it (`docs/DEV.md:71-73`); the app should consume it.
- **Stop trusting byte length.** `UpdateChecker.kt:178` and `UpdateDialog.kt:76` treat
  `length() == apkSize` as proof the cached APK is the right one. Require the signature check on
  *every* install path, including cached files and the cached-update replay in
  `HomeScreen.kt:144-155`.
- **Constrain redirects** (`:189-209`): allowlist `github.com`, `objects.githubusercontent.com`,
  `release-assets.githubusercontent.com` and nothing else; drop the dual mechanism (either
  `instanceFollowRedirects = true` *or* manual following, not both).
- Consider TLS pinning via `network_security_config.xml` for the update host.
- Add a `network_security_config.xml` with `cleartextTrafficPermitted="false"` to make the
  HTTPS-only posture explicit.

**2.6 Update path — stop hand-rolling HTTP**
`UpdateChecker.kt:168-299` (132 lines of manual redirect-following + speed smoothing) and
`SportsFeed.kt:280-310`. Both use `HttpURLConnection` with manual redirect loops.
- Replace with OkHttp (or Ktor client) + a tiny `Http` abstraction that both share.
- Gains: connection pooling, TLS pinning, interceptors, cancellation, real `Content-Length`
  handling, and ~150 fewer lines.

---

### Phase 3 — OriginOS 7 native surface (2–3 weeks, the differentiator)

This is where the app stops being "an OriginOS 6 app that happens to run on 7".

**3.1 Bump to the OS7 reality**
- `compileSdk 37`, `targetSdk 37` (Android 17). Resolve the Android 17 behaviour changes:
  background audio hardening (not applicable), self-broadcast priority (not applicable),
  notification changes.
- Add the new `androidx.core` APIs the repo is already half-using by raw key
  (`GenericCard.kt:118,283-290`) once they're in a stable release the project compiles against.

**3.2 Live Updates as a first-class card type**
Android 16+ `ProgressStyle` is already parsed (`GenericCard.kt:166-294`). Extend it:
- `isStyledByProgress()` detection → `TEMPLATE_PROGRESS_VISUAL` with `progressNodeIcons` and
  `indicatorIcon` (already wired at `OriginIslandBuilder.kt:352-367`).
- `shortCriticalText` as the primary chip (already at `GenericCard.kt:120,126`).
- `POST_PROMOTED_NOTIFICATIONS` + `setRequestPromotedOngoing` (`AndroidManifest.xml:5` already
  declares the permission).
- Map Android 16 Live Updates → vivo `template 2` progress ring consistently, not via the
  `ringSaysItAll` heuristic alone (`GenericCard.kt:158`).

**3.3 OS7 island features**
- **AirPods battery / Spatial Audio** in the island → a `MediaCard` extension (route through
  `MediaSession` metadata).
- **Origin player settings integration** — the OS7 toggle path (Settings → Origin player → All
  apps) implies a new contract; document what the app must expose (likely a `MediaBrowserService`
  or a documented `MediaSession` extras contract). **Needs device verification (Phase 0.4).**
- **Apple Watch notification mirroring** — out of scope for a third-party app, but the island
  may need to de-dupe. Flag, don't build.

**3.4 Use all 15 scenes (after 1.3)**
Map card → scene, and let vivo's own renderer pick the right template:
| Card | Scene |
|---|---|
| Navigation | `NAVIGATION` |
| Ride | `TAXI` / `RIDE_GUIDE` |
| Delivery | `TAKEOUT` / `DELIEVERY` |
| Meeting | `METTING` |
| Timer | `TIMER` |
| Call | `INCALLING` / `VOIPCALL` |
| Sports | `CAR_STATE`? → verify; otherwise stay on `NAVIGATION` |
| Media | `MOVIE`? → verify |

**Verification is mandatory per scene.** `docs/PROTOCOL.md:25-30` records that only `NAVIGATION`
was reliably granted on OS6 — so each mapping must be tested on the OS7 device and the result
written back into `docs/PROTOCOL.md`.

**3.5 `operation = 1` for updates**
`OriginIslandBuilder.kt:205`. Use `changeRecord` (`OriginIslandConstants.kt:31`) plus `operation=1`
on refresh, `0` only on first create, `2` on end. Keep the fallback to `0` if the probe shows OS7
rejects it — but measure, don't assume.

---

### Phase 4 — Robustness on a hostile OEM (1 week)

**4.1 Make the keep-alive honest**
`AndroidManifest.xml:81-93` vs `PlaygroundService.kt:174-178` contradict each other. Decide:
- If the accessibility anchor is dead on OS7, remove the service and rely on
  `requestRebind` + `START_STICKY` + `AlarmManager`.
- If it's kept, add a **watchdog**: if `onAccessibilityEvent` never fires, don't assume the
  process is anchored. Re-evaluate every process start.

**4.2 Handle process death properly**
`IconCache`, `CastLog`, `PlaygroundService.activeCardIds` are all process-global. On a device that
kills the process per swipe, the app must:
- persist enough state to know "there were 3 cards up" and either restore or gracefully clear them
  (`endOrigin` for each stale id on startup), and
- re-derive cards from `activeNotifications` on reconnect (partly done at
  `NotificationCastListener.kt:635-643`).

**4.3 Native-threading audit**
- `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)` (`NotificationCastListener.kt:326`)
  is used for `SportsFeed.iconsFor` (`:1208`) — that's a network call on the main dispatcher.
  Move to `Dispatchers.IO`.
- `handleScore` launches a coroutine from the listener's main thread (`:1208`) — fine, but the
  `scope` is never tied to the service lifecycle beyond `onDestroy` (`:1154`). Use a
  `CoroutineScope` bound to `Service` with a `SupervisorJob`, and cancel on
  `onListenerDisconnected` too.
- `mainHandler` postDelayed chains (`PlaygroundService.kt:563,584,623`) are main-thread;
  acceptable, but the 350ms/400ms magic numbers should be named constants.

**4.4 Fix the `minSdk`/`targetSdk` mismatch**
`minSdk 34` (`build.gradle.kts:32`) means the app claims Android 14 support, but SuperX is a
vendor feature that **cannot work below OriginOS 6**. Either:
- raise `minSdk` to the lowest OS6 device, or
- add a hard runtime gate: detect non-vivo and show "this app requires a vivo/iQOO with
  OriginOS 6+" instead of pretending to work.
The current behaviour — posting SuperX extras that silently do nothing on 99% of devices — is the
worst of both.

---

### Phase 5 — Craftsmanship (1 week, cheap, high signal)

| Task | Location | Effort |
|---|---|---|
| Add `ktlint` + `detekt` (or `ktlint` via Gradle) and a CI workflow that runs them + the 64 tests | repo root, `.github/workflows/ci.yml` | 1 day |
| Move the 14 hardcoded UI strings to `strings.xml` (`strings.xml` has **4**) | `ui/*.kt` | 1 day |
| Add `contentDescription` to every `IsleIcons` use (7 exist today; `HomeScreen.kt` has **0**) | `ui/*.kt` | 1 day |
| Add `minimumInteractiveComponentSize` to all `ToggleRow`/`PillButton`/`CircleIconButton` | `Components.kt` | 1 day |
| Fix touch targets: `CircleIconButton` 44 dp, `ChoiceChip` 44/36/32 dp, `SegmentedControl` 40 dp, `StatusPill` 32 dp — all below 48 dp | `Components.kt:85,300`, `HomeScreen.kt:1022`, `ActivityScreen.kt:100` | 1 day |
| Fix non-text contrast on 8 of 18 tile colours (white on `#FFB800` = 1.73:1, `#FF9500` = 2.2:1, `#34C759` = 2.22:1, `#00C2A0` = 2.28:1, `#29B6F6` = 2.3:1, `#10B981` = 2.54:1, `#2AABEE` = 2.57:1) — darken the tile or switch to a dark glyph | `Theme.kt:66-78` | 1 day |
| Add `semantics { disabled() }` where sections are only `alpha(0.4f)` | `HomeScreen.kt:333,404,805,817` | 2 h |
| Extract the duplicated "read title/text/bigText, build a haystack, check keywords" pattern — it appears in `NotificationCastListener` **7 times** (`:846-849, 880-883, 914-917, 941-944, 1112-1117, 1179-1182, 1192-1196`) | `cards/` | 1 day |
| Collapse the 7 copies of the action filter into `GenericCard.usableActions` (and restore the dropped `remoteInputs`/`isContextual`/reply checks — `DownloadCard.kt:398` currently fires an empty RemoteInput) | 6 card files | 1 day |
| Collapse the 6 copies of the semantic button-colour `when` into `OriginIslandBuilder.resolveSemanticButtonBgColor` | 6 card files | 1 day |
| Delete dead code: `MessageCard.shortName`, `DiscordCard.shortSender`, `MessageCard`/`DiscordCard` private `clip`, `CallCard.shrinkForIcon`, `NotificationCastListener`'s second `DURATION_REGEX` | `cards/` | 2 h |
| Delete dead prefs: `cast_gemini_view_glow`, `cached_update_release_url`; implement or delete `cast_enabled_apps` | 3 files | 2 h |
| De-duplicate `SYSTEM_PKGS` (listener ≡ RaidenCard) and `PREFS_NAME` (3× + 1 raw literal) | 4 files | 2 h |
| Replace `"%02d:%02d".format(...)` with `DateFormats`/`java.time` | `IntelligentTextSanitizer.kt:99`, `QuietHours.kt:30`, `CardIntents.kt:99` | 1 day |
| Add `@VisibleForTesting` annotations and make the 20 `object XCard` constants internal where possible | `cards/` | 2 h |
| Add `RaidenStateReceiver` token auth (P2-18) | `RaidenStateReceiver.kt`, `OriginIsleApp.kt`, `scripts/raiden-bridge.sh` | 1 day |

---

### Phase 6 — Test the things that are currently untested (1 week, but plan it now)

Highest-value additions, in order:

1. **SuperX wire format** (13–20 tests) — after 1.5 makes it possible. This is the crown jewel:
   it pins the protocol so no refactor can break it silently.
2. **`CardHandler` registry ordering** — assert the 20 handlers produce the same decision order as
   today's ladder. This is the regression guard for Phase 1.1.
3. **`PrefsState` round-trip** — assert every one of the ~40 prefs keys survives a write/read and
   propagates to collectors.
4. **`UpdateChecker` signature verification** — assert a mismatched cert is refused on every
   install path (fresh download, cached file, cached-update replay).
5. **`buildEndBundle`** — assert `operation=2`, `showNotify=false`, `changedRecord=Int.MAX_VALUE`
   (`OriginIslandBuilder.kt:120-125`).
6. **Id-collision test for `castIdFor`** — two notification keys mapping to the same id, plus an
   assertion that `castIdFor` ∩ {Raiden 29500/29501, charging 35001, matches 30000-34999} = ∅.
7. **`extractDeepLinkIntent` / `fallbackClickIntent` / `shrinkForIcon` / `parcelSafeIcon`** —
   `CardIntents.kt` has exactly **one** test (`formatElapsed`); the bitmap cap at `:66-72` is the
   documented fix for a crash-loop and is completely untested. A regression there ships green.
8. **Per-app parsing** — `MessageCard.parseMail`/`parseDiscord` and the equivalent helpers in
   MailCard/DiscordCard/MediaCard have **no tests**; 5 of 20 cards (Weather, Sports, Navigation,
   Payment, Charging) have no test file at all.
9. **Replace the tests that assert against local copies.** `PaymentDetectionTest.kt:10-14`
   re-declares the `AMOUNT` regex, and `CallCardTest.kt:60/68` re-declares keywords — so those
   tests pass while production diverges. Point them at the real constants.
10. **Instrumentation tests for `NotificationCastListener`** — none exist today
   (`app/src/androidTest` is absent). Add a Robolectric suite for the dispatch ladder.
11. **Compose UI tests** — no `@Preview` anywhere in the UI package, no Robolectric, no
   `ui-test`. Add previews for the shared components and one Robolectric test per screen.

---

## 6. Target architecture (what "done" looks like)

```
┌──────────────────────────────────────────────────────────────┐
│ UI layer (Compose)                                           │
│  HomeScreen · AppsScreen · ActivityScreen · Onboarding       │
│  state: StateFlow<PrefsState> · no SharedPreferences in comp │
└───────────────▲──────────────────────────────┬───────────────┘
                │ events                       │ collect
┌───────────────┴──────────────────────────────▼───────────────┐
│ CardHandler registry (20+ handlers, sorted by priority)      │
│  CallHandler · NavHandler · PaymentHandler · MediaHandler …   │
│  each: handle(ctx, sbn) -> CardDecision?                     │
└───────────────▲──────────────────────────────┬───────────────┘
                │ SuperXRequest (typed)        │ CardDecision
┌───────────────┴──────────────────────────────▼───────────────┐
│ CastEngine (single owner of the cast/skip decision)          │
│  dedupe · quiet hours · app allow/deny · priority arbitration│
└───────────────▲──────────────────────────────┬───────────────┘
                │                              │
┌───────────────┴──────────────┐  ┌────────────▼───────────────┐
│ NotificationCastListener     │  │ MediaSessionRouter          │
│ (framework callbacks only)   │  │ (MediaController callbacks) │
└──────────────────────────────┘  └────────────────────────────┘
                │ SuperXRequest
┌───────────────▼──────────────────────────────────────────────┐
│ SuperXTransport                                             │
│  SceneCapabilityProbe (getSceneStatus / isSupportCustomFun) │
│  BundleCodec (pure, JVM-tested) · grantScenes · end/cancel  │
└───────────────▲──────────────────────────────────────────────┘
                │ one typed request
┌───────────────┴──────────────────────────────────────────────┐
│ OriginIslandBuilder.buildBundlePure(request, defaults)      │
│  NO Context · NO resources · fully unit-testable            │
└──────────────────────────────────────────────────────────────┘
```

Invariants:
- **One typed request** from detection to transport. No `oi_*` strings.
- **One decision owner.** No priority logic in cards.
- **One bundle builder.** No duplicate keys, no magic colours, no `Context`.
- **No `SharedPreferences` read in a composable.**
- **Every prefs key has a name in `PrefsState`** — no stringly-typed settings.
- **Every scene is probed before use.**
- **Every install path verifies the APK signature** — no cached-file shortcut.
- **One card id space.** No handler can pick an id another handler owns.

---

## 7. Explicit non-goals (so the plan doesn't balloon)

- No iOS/Dynamic Island support. Not relevant.
- No Wear OS / watch face complications.
- No rewrite in Rust/Compose Multiplatform. The native Android stack is correct here.
- No Play Store distribution while the AMap spoof is in place (§0 decides this).
- No removal of the SuperX key dictionary — `OriginIslandConstants.kt` is the reference value of
  the repo and must stay complete, including keys the app doesn't use (`:11-13` says so).

---

## 8. Immediate next actions (in order)

| # | Action | Why now |
|---|---|---|
| 1 | **Phase 0 device test** — does a non-spoofed `applicationId` work on OriginOS 7? | Blocks the whole strategy |
| 2 | Fix the APK update pipeline: signature + checksum verification before install (P0-1b) | Self-inflicted supply-chain hole; the app reads every notification |
| 3 | `targetSdk 37` + `compileSdk 37` + Android 17 behaviour-change audit | The app targets the newest OS while opting out of it |
| 4 | Add `ktlint` + a CI workflow running the existing 64 tests | 30 min, instant regression signal |
| 5 | `CardHandler` registry extraction (1.1) | Unblocks everything else; pure refactor |
| 6 | `SuperXRequest` typed contract (1.2) | Makes 1.1 safe to land incrementally |
| 7 | `buildBundlePure` + wire-format tests (1.5) | Protects the hardest-won asset |
| 8 | `PrefsState` + split `HomeScreen.kt` (2.1, 2.2) | The biggest single-file defect |
| 9 | Scene capability probe (1.3) | Unlocks 15 scenes, ~20 lines |
| 10 | `IconCache` bounding + collision-safe ids (2.3) | Memory + correctness |
| 11 | Fix the `cast_enabled_apps` dead allow-list (P0-1c) | The Apps-tab promise is half-implemented |
| 12 | Move `isRecapOrHighlights`'s `"position"` to a real signal (P1-9) | Real false-negatives on live scores |
| 13 | Collapse the 7 action-filter copies and 6 colour `when`s (P2-16) | DownloadCard can fire an empty RemoteInput today |

---

## 9. Open questions only you can answer

1. **Do you have an OriginOS 7 device right now** (X200 Pro / X300 / X500 / iQOO 15)? Phase 0
   needs it, and §3.3/3.4 need it for every scene and template claim.
2. **Is the goal still "personal sideload on one device", or "distributable"?** The AMap spoof
   caps the second; OS7 may remove the cap.
3. **Is the `RaidenCard` / Muse integration something you actually use?** It is 494 lines plus a
   `raiden-bridge.sh` script and 4 prefs keys — the largest single feature in the repo, and the
   only one tied to a third-party app that may not exist on your device. If it's dead weight, it
   is also the source of P1-8 (id-band collision), P2-18 (broadcast exposure) and 7 of the 64
   tests.
4. **Which card types do you actually read on the island?** The repo ships 20; the plan's Phase 3
   effort should concentrate on the 5 you use daily. 5 of the 20 have no test file at all
   (Weather, Sports, Navigation, Payment, Charging) — if those are your daily drivers, that's the
   wrong test distribution.
5. **Do you want the Gemini iridescent glow / ambient light effects kept?** They are
   `OriginIslandBuilder.kt:144-172` and `cast_gemini_view_glow` — a purely aesthetic feature that
   is not in the OS7 design language ("光影美学 / liquid light", not iridescent gradients). The
   pref is also dead (never written), so the feature is on with no way to turn it off.
6. **Is Bangla notification text a real case for you?** If bKash/Nagad/Rocket send Bangla, P1-9 is
   a daily-impact bug, not a nice-to-have — and it's cheap to fix (Bengali boilerplate regexes +
   `Notification.category` instead of prose matching).

---

## 10. How to read this document

- **§1** is the honest credit. Do not let a refactor throw away the reverse-engineering.
- **§2** is the vivo spec cross-check — the source of truth for "what should the island do".
- **§3** is the defect list, ranked by user impact, every item cited.
- **§5 Phase 0** blocks everything. **Phases 1-2** are pure refactors with the existing tests as
  the net. **Phase 3** is where the app becomes an OS7 app. **Phases 4-6** are craftsmanship.
- **§8** is the ordered task list. Start at 1; stop when the app is yours again.

Not one line of this plan has been compiled or run — there is no JDK, no Android SDK and no
Android Studio on the machine that produced it. Every claim is a source citation; the two claims
that need hardware are marked as such in §3 P2-17 and §5 Phase 0.
