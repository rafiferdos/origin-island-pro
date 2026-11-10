# Origin Island Pro: execution plan for the vivo X200 (China) on OriginOS 7

> **Status:** plan only, nothing in here has been implemented yet.
> **Written against:** branch `claude/vibrant-noether-14sen6`, HEAD `c6fc51d` (v2.34.0).
> **Single target device:** vivo X200 series, **Chinese ROM**, **OriginOS 7**. Global/Funtouch
> builds, other OEMs and OriginOS 6 are **out of scope**. Don't spend effort on them, but don't
> break the build for them either.
> **Supersedes** `docs/GOD-LEVEL-PLAN.md` for prioritisation. That file stays as background
> reading. Where the two disagree, this file wins.

---

## 0. How to use this document (read first, especially if you are a different model)

This plan is written to be executed step by step by any coding model (Opus, Sonnet, …) with
no memory of the conversation that produced it. Follow these rules:

1. **Work top to bottom by task ID** (`T0.1`, `T1.1`, …). Each task lists *files*, *steps*,
   *acceptance criteria* and whether it **needs the device** (🔌) or is **code-only** (💻).
2. **🔌 tasks cannot be done by the model.** For those, the model's job is to prepare the
   code or the `adb` commands, then **stop and ask the user** to run them on the X200 and paste
   the output. Record each answer in §12 *Device findings log* at the bottom of this file and
   commit it. Never invent device results.
3. **Never change `applicationId`** in `app/build.gradle.kts` outside task `T0.3`. It is the
   AMap spoof (`com.autonavi.minimap`) and it is what grants island access today.
4. **Line numbers drift.** Every reference is `path:line` as of `c6fc51d`. If a line no longer
   matches, search for the quoted symbol instead. Don't edit blindly by number.
5. **Keep every existing test green.** There are 156 `@Test` methods under `app/src/test`. Run
   them after each phase (see §1). New logic gets a JVM test in the matching test folder.
6. **One task per commit**, message format `type(scope): summary (Txx)`, e.g.
   `fix(listener): skip notifications that are already native SuperX islands (T1.1)`.
   Push to the working branch. Don't open a PR unless asked.
7. **Native-only rule (the user's hard requirement):** the island must look like vivo's own
   OriginOS 7 islands. Only use bundle fields vivo documents (`docs/PROTOCOL.md`, vivo spec
   原子通知 at `dev.vivo.com.cn/documentCenter/doc/896`) and use them the way vivo's own apps
   do. No invented visual effects. When unsure whether something is native, leave it out.
8. If a task turns out to be wrong once you read the code (e.g. already fixed), mark it
   `~~struck~~ (already done in <sha>)` in this file and move on.

---

## 1. Build & verify commands

The cloud container has no Android SDK by default. CI (`.github/workflows/ci.yml`) installs
API 36 + build-tools 35 and runs the tests. To run locally in a fresh container, copy those CI
steps. Minimal version:

```bash
export ANDROID_HOME=$HOME/android-sdk && mkdir -p $ANDROID_HOME/cmdline-tools && cd $ANDROID_HOME/cmdline-tools \
 && curl -sL -o t.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q -o t.zip && mv cmdline-tools latest && cd - \
 && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null \
 && $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0" \
 && echo "sdk.dir=$ANDROID_HOME" > local.properties
```

```bash
./gradlew --no-daemon :app:testDebugUnitTest      # all JVM tests (Robolectric)
./gradlew --no-daemon :app:ktlintCheck            # style; baseline in app/config/ktlint/baseline.xml
./gradlew --no-daemon :app:assembleDebug          # APK at app/build/outputs/apk/debug/
```

If the network blocks `dl.google.com`, don't fight it: push and let CI run, then read the CI
result. Device-side commands the user runs (Windows/macOS with adb):

```bash
adb install -r app-debug.apk
adb logcat -s SceneCapability PlaygroundService NotificationCastListener CastLog
adb shell dumpsys notification --noredact | grep -i -A3 superx
adb shell getprop ro.build.version.sdk          # Android API level of OriginOS 7 CN
adb shell getprop ro.vivo.os.version            # OriginOS version string
adb shell pm list packages | grep -iE "autonavi|tencent|alipay|sankuai|didi|bbk|incallui|meituan|ele|cainiao|taobao|jingdong|12306|netease|kugou"
```

---

## 2. Current state (facts the executor needs)

| Area | Fact | Where |
|---|---|---|
| Identity | `applicationId = "com.autonavi.minimap"` (AMap spoof), `namespace = com.originisle.android`, `minSdk 34`, `targetSdk 36`, `compileSdk 36`, v2.34.0 / code 55 | `app/build.gradle.kts:44-56` |
| Island entry point | Cards build an `Intent` full of `oi_*` extras → `PlaygroundService.readAndPost()` → `SuperXRequest` → `BundleCodec.buildBundlePure()` → `notify(tag="VIVO_SUPERX_TAG")` | `island/PlaygroundService.kt:208`, `island/BundleCodec.kt`, `island/SuperXRequest.kt` |
| Scene grant | `SceneCapability.grant()` reflects `setSuperXInfosSceneList`, probes `getSceneStatus` / `isSupportCustomFun`, has `sceneFor(cardType)`, **but no card calls it** | `island/SceneCapability.kt:163-174` |
| Scene in practice | 25 card sites hard-code `putExtra("oi_scene", "NAVIGATION")` (list in T2.1) | `cards/*.kt` |
| Dispatcher | `NotificationCastListener.onNotificationPosted` is one ~420-line if/else chain | `service/NotificationCastListener.kt:665-1085` |
| Native-island blind spot | The listener only inspects SuperX tags on **its own** posts (`cleanUpRejectedSuperX`). It re-casts other apps' native islands, causing duplicates | `service/NotificationCastListener.kt:666-668, 1192` |
| Quiet hours | Checked at `:826`, **after** call / media / Raiden / OTP / 2FA / timer branches | `service/NotificationCastListener.kt:823-830` |
| Call detection | Keyword fallback (`"video call"`, `"ringing"`, `"in call"` …) on any app's text | `cards/CallCard.kt:73-110` |
| Non-native visuals | `oi_light_color` (edge glow) set by ~20 cards; per-card button colours (6+ copies of a colour `when`); `buildGeminiTextSpan` / `GEMINI_PALETTE` / `isGeminiGlowEnabled` are dead code with one test | grep `oi_light_color`, `oi_button_bg_colors`, `OriginIslandBuilder.kt:85-117`, `test/.../OriginIslandGeminiViewTest.kt` |
| App UI theme | Dark-only `darkColorScheme` with hard-coded "obsidian" palette, bundled Plus Jakarta Sans font, `Theme.Material3.Dark.NoActionBar` | `ui/Theme.kt`, `res/values/themes.xml`, `res/font/plus_jakarta_sans.ttf` |
| God files | `ui/HomeScreen.kt` 1,512 lines; `service/NotificationCastListener.kt` 1,361 lines | |
| China app coverage | Messaging has WeChat/QQ/TIM/DingTalk/Feishu; NAV has Baidu Maps; **missing** Alipay, Meituan, Ele.me, Didi, Taobao, JD, Pinduoduo, Cainiao, 12306, Tencent Maps, NetEase Cloud Music, QQ Music, Kugou, Tencent Meeting, vivo system apps | `service/NotificationCastListener.kt:198-300`, `cards/*_APPS` |
| Chinese text | `OtpDetector` already knows `验证码`. Everything else (calls, delivery, ride, payment, timer, sanitizer) is English/Bangla keyword-based | `otp/OtpDetector.kt:18` |
| Docs drift | README says "OriginOS 6 only"; `DeviceCompat.unsupportedMessage()` says "OriginOS 6 or newer" | `README.md`, `util/DeviceCompat.kt:47-51` |
| Updates | Update checker talks to `api.github.com` and GitHub release assets; signature + SHA-256 now verified | `ui/UpdateChecker.kt`, `security/ApkVerifier.kt` |

### Why the Chinese ROM changes priorities

1. **Real AMap (高德地图) is near-universal in China.** The spoof makes the two impossible to
   install together, so this is a real problem on the target device (T0.3).
2. **Big Chinese apps already post native vivo islands** (vivo partners: food delivery,
   ride-hailing, payments, train tickets, music). Re-casting them gives two pills for one event,
   which is the most visible "non-native" bug on a CN ROM (T1.1).
3. **Notification text is Chinese.** Keyword heuristics that only know English silently fall
   back to the generic card (T4.x).
4. **vivo's own system apps on the CN ROM use different package names from global builds**
   (e.g. clock / dialer / music). Verify on device with `pm list packages` (T0.2).
5. **GitHub may be slow or blocked** on Chinese networks, so the in-app updater must fail
   gracefully (T6.3).

---

## 3. Phase 0: Device baseline (🔌, blocks the strategy, do first)

### T0.1 🔌 Capture the platform facts
- **Model:** prepare the commands from §1 and ask the user to run them on the X200.
- **Record in §12:** `ro.build.version.sdk`, `ro.vivo.os.version`, `ro.product.model`, and
  whether `adb logcat -s SceneCapability` shows `granted … N/15 scenes` after opening the app.
- **Accept:** §12 has all four values.

### T0.2 🔌 Package census of the user's real apps
- Ask the user for `adb shell pm list packages -3` **and** `adb shell pm list packages -s | grep -iE "vivo|bbk|android\.(incallui|dialer|deskclock)"`.
- From that, fill the table in §12.2: for each category (dialer, clock, music, maps,
  delivery, ride, payment, train/flight, meetings, messaging) record the exact package names
  present. **All later package-list edits must use these real names**, not guesses.
- **Accept:** §12.2 filled. Every package name used in Phase 4 appears there.

### T0.3 🔌 Decide the package-identity question
Goal: find out whether the AMap spoof is still required on OriginOS 7 CN and whether the user
also needs the real AMap installed.

1. 💻 Add a product-flavor dimension `identity` in `app/build.gradle.kts`:
   - `spoof` → `applicationId = "com.autonavi.minimap"` (default, unchanged behaviour)
   - `native` → `applicationId = "com.originisle.android"`
   - Make sure the `FileProvider` authority `${applicationId}.fileprovider` still resolves
     (it does automatically), and that CI still runs `testSpoofDebugUnitTest` (update
     `.github/workflows/ci.yml` task name: `:app:testSpoofDebugUnitTest`).
   - Keep `scripts/release.sh` building `assembleSpoofRelease`.
2. 🔌 User installs `native` debug **alongside** the spoof build and opens it. Record:
   - logcat line from `SceneCapability` (granted vs `unavailable` vs `call failed`)
   - whether the in-app sample card (Cast tab) appears on the island
   - `dumpsys notification --noredact | grep -i superx` output for that post
3. 🔌 Check whether OriginOS 7 CN has a per-app island permission page (Settings → 通知与状态栏 /
   原子岛 / Origin player → 所有应用). If the `native` build is listed there, toggle it on and
   repeat step 2.
4. **Decision (write it in §12.3):**
   - **A. `native` works** → make `native` the default flavor, keep `spoof` as fallback, update
     README/DEV.md. The real AMap can now coexist.
   - **B. Only `spoof` works** → stay spoofed. Add a README note that real AMap must be
     uninstalled, and a first-run check (T6.2) that detects a conflicting install attempt.
- **Accept:** §12.3 records A or B with the evidence.

### T0.4 🔌 Protocol truth table (one sample per question)
Use the Cast-tab sample tester (`ui/samples/IslandSamples.kt`). Add samples where missing, ask
the user to run each, record pass/fail in §12.4:

| # | Question | How |
|---|---|---|
| a | Which of the 15 scenes render? | One sample per scene in `OriginIslandConstants.SUPERX_SCENES`, same template-4 body, scene varied |
| b | Is `forceShow=true` required on OS7? | Same card with `oi_force_show` true vs false |
| c | Does `operation=1` update an existing card, or is it dropped? | Post op 0, then op 1 with new text on same id |
| d | Does the left-island **timer text** work (spec says `leftInfo.content` supports a timer) | Timer sample using left content timer vs current capsule-text approach |
| e | Does `island.superx.template` (big-card type) change anything? | Sample with and without it |
| f | Does `lightEffectInfo` render at all on OS7 CN? | Sample with and without `oi_light_color` |
| g | Do two simultaneous cards on **different** scenes stack (two pills / one pill + count)? | Ride + delivery samples at once |
| h | Template 2 (progress body) and template 5 (simple nav) render? | Existing samples |

- **Accept:** §12.4 has 8 rows filled. `docs/PROTOCOL.md` updated with any new facts (add
  "Verified on X200 CN OriginOS 7" notes).

---

## 4. Phase 1: Correctness fixes in the listener (💻, no device needed to code)

All in `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt` unless
stated. Add tests in `app/src/test/java/com/originisle/android/service/`.

### T1.1 💻 Skip notifications that are already native vivo islands  ⭐ highest impact on CN ROM
- **Why:** Meituan/Ele.me/Didi/Alipay/12306/system apps post their own SuperX islands. Today
  we re-cast them → duplicate pill.
- **Steps:**
  1. Create `service/NativeIslandFilter.kt` with a **pure** function
     `fun isNativeIsland(tag: String?, extrasKeys: Set<String>, pkg: String, ownPkg: String): Boolean`
     returning true when `pkg != ownPkg` **and** (`tag == OriginIslandConstants.SUPERX_TAG`
     (value `"VIVO_SUPERX_TAG"`) **or** any key
     in `extrasKeys` starts with `"notification.superx."` or `"island.superx."`).
  2. In `onNotificationPosted`, right after the `SYSTEM_PKGS` check (`:671`), call it with
     `sbn.tag`, `sbn.notification.extras.keySet()`, and `log(sbn, "skipped — app already shows a native island", false)`.
  3. Add a pref `skip_native_islands` (default **true**) and a toggle in the Cards/Settings UI
     next to existing card switches (search `cast_charging` in `ui/HomeScreen.kt` for the pattern).
  4. Also handle `onNotificationRemoved`: nothing to do, since we never cast it.
- **Tests:** `NativeIslandFilterTest`: own package → false; foreign + tag → true; foreign + superx key → true; foreign plain → false.
- **🔌 follow-up:** ask the user to trigger a Meituan/Didi/Alipay notification and confirm one pill, not two. Record in §12.5.

### T1.2 💻 Built-in "vivo already handles this" package list
- Some system features show on the island without a SuperX-tagged notification visible to us
  (calls, timers, charging, music via Origin player). Add a set `SYSTEM_ISLAND_OWNERS` in
  `NativeIslandFilter.kt`, filled **only from §12.2 real package names** (expected candidates
  to verify: the CN dialer/in-call UI, the CN clock app, vivo's music app).
- Gate: when `skip_native_islands` is on, calls from these packages → skip `CallCard`; timers
  → skip `TimerCard`; `ChargingCard` → off by default (`cast_charging` default false; search
  `getBoolean("cast_charging", true)`, 3 sites in listener + any in UI, and change the defaults
  consistently).
- **Accept:** a vivo dialer incoming call produces exactly one island (🔌 confirm, §12.5).

### T1.3 💻 Tighten call detection
- File: `cards/CallCard.kt:73-110`.
- Change the keyword fallback (step 2, "callKeywords") so it **only applies when**
  `sbn.isOngoing` **or** the notification has a full-screen intent (`n.fullScreenIntent != null`)
  **or** `pkg in CALL_PACKAGES`. A plain message saying "missed video call" must not become
  a call card.
- Remove `"in call"` (substring of too many words) and `"ringing"` unless ongoing.
- Add Chinese: `来电`, `正在通话`, `语音通话`, `视频通话`, `邀请你进行`, under the same ongoing/fullscreen gate.
- **Tests:** extend `CallCardTest`: WhatsApp non-ongoing "Missed video call" → false; WeChat ongoing `语音通话中` → true; vivo dialer ongoing → true.

### T1.4 💻 Quiet-hours ordering
- Move the quiet-hours check (`:823-830`) to **before** the Raiden, 2FA and timer
  branches, keeping these exemptions explicit in one place: calls, navigation, timers/alarms,
  OTP codes (current documented intent). 2FA prompts and Raiden should **respect** quiet hours.
- Extract to a pure function `QuietHours.allows(kind: CastKind, active: Boolean): Boolean` in
  `service/QuietHours.kt` with an enum `CastKind { CALL, NAVIGATION, TIMER, OTP, AUTH, RAIDEN, MEDIA, OTHER }`.
- **Tests:** extend `QuietHoursTest` with one assertion per kind.

### T1.5 💻 Uniform duplicate-delivery guard
- Today only some branches check `lastCastPostTime.put(sbn.key, sbn.postTime) == sbn.postTime`
  (OTP, auth). Move that single check to the top of `onNotificationPosted`, just after the
  per-app filter, **excluding** ongoing live cards that legitimately re-post with the same
  postTime but new progress (media, call, download, navigation, timer). Use
  `sbn.isOngoing` as the exclusion test, and delete the per-branch copies.
- **Tests:** a pure helper `shouldSkipRedelivery(prevPostTime: Long?, postTime: Long, ongoing: Boolean)`, tested.

### T1.6 💻 Delete dead "Gemini" code
- Remove `GEMINI_PALETTE`, `buildGeminiTextSpan`, `isGeminiGlowEnabled` from
  `island/OriginIslandBuilder.kt:85-117`, unused imports, and
  `test/.../island/OriginIslandGeminiViewTest.kt`. Remove any `cast_gemini_view_glow` pref writes.
- **Accept:** `grep -ri gemini app/src` returns nothing; tests green.

### T1.7 💻 Fix platform messaging
- `util/DeviceCompat.kt:47-51` → "…needs a vivo or iQOO device running OriginOS 7…".
- `README.md`: replace "vivo/iQOO + OriginOS 6 only" with "Built and tested for vivo X200 (China ROM) on OriginOS 7".
  Keep install/verify sections.
- Update `DeviceCompatTest` if it asserts the message.

---

## 5. Phase 2: Native island design (💻 code, 🔌 visual check)

Goal: every card looks like a first-party OriginOS 7 island. Uses findings from T0.4.

### T2.1 💻 Route every card through `SceneCapability.sceneFor`
- Replace each hard-coded `putExtra("oi_scene", "NAVIGATION")` with
  `putExtra("oi_scene", OriginIslandBuilder.sceneFor("<TYPE>", context))`.
  Sites (as of `c6fc51d`): `MediaCard.kt:214`(MEDIA) `CallCard.kt:324`(CALL)
  `MessageCard.kt:103`(MESSAGE) `AuthPromptCard.kt:81`(AUTH) `TimerCard.kt:77`(TIMER)
  `PaymentCard.kt:104,128`(PAYMENT, **both posts must use the same scene**) `GenericCard.kt:181`(GENERIC)
  `DiscordCard.kt:91`(MESSAGE) `MeetingCard.kt:65`(MEETING) `OtpCard.kt:56,102`(OTP)
  `DownloadCard.kt:429`(DOWNLOAD) `RaidenCard.kt:387,407,491`(RAIDEN) `SportsCard.kt:113,247`(SPORTS)
  `NavigationCard.kt:96`(NAVIGATION) `MailCard.kt:95`(MAIL) `WeatherCard.kt:31`(WEATHER)
  `ChargingCard.kt:96,129`(CHARGING) `RideCard.kt:72`(RIDE) `DeliveryCard.kt:38`(DELIVERY).
- **Critical:** the end/cancel path must use the **same** scene the card was posted with.
  `PlaygroundService` stores it in `originScenes[id]` (`PlaygroundService.kt:560`). Verify every
  card's cancel path goes through that map; if a card ends itself with a literal scene, fix it.
- Update `SceneCapability.sceneFor` mapping with the T0.4a results. Unlisted types fall back to `NAVIGATION`.
- **Tests:** `SceneCapabilityTest`: unknown probe → `NAVIGATION` for every type (behaviour
  unchanged on devices where the probe fails).
- 🔌 Confirm a ride + a delivery show together (T0.4g).

### T2.2 💻 Central "native style" policy
- Create `island/NativeStyle.kt` with the **only** allowed colour/effect decisions:
  - `buttonColors(semantic: ButtonSemantic): Pair<Int, Int>?`: return `null` (system default)
    for everything **except** `DESTRUCTIVE` (decline / end call / reject), which returns vivo
    red `0xFFFF3B30` on white text. Reuse/replace `OriginIslandBuilder.resolveSemanticButtonBgColor`
    if it exists (grep it).
  - `edgeLight(kind): Int?`: non-null **only** for PAYMENT success and CHARGING full
    (and only if T0.4f shows edge light renders); null everywhere else.
  - `capsuleColors(kind)`: keep the capsule colour only where vivo's own islands colour it
    (timer = orange, call = green, recording/critical = red). Everything else → null (system default).
- Delete the per-card colour `when` blocks (TimerCard.kt:60-69 and its siblings in Call,
  Message, AuthPrompt, Discord, Otp, Download, Raiden, Mail, Media; find them via
  `grep -n "oi_button_bg_colors"`) and call `NativeStyle` instead. When `NativeStyle` returns
  null, **don't put the extra at all**.
- Remove every `putExtra("oi_light_color", …)` / `oi_light_mode` except via `NativeStyle.edgeLight`.
- **Tests:** `NativeStyleTest`: only DESTRUCTIVE returns colours; edge light only for the two kinds.
  Update `SemanticButtonTest` accordingly.

### T2.3 💻 Plain-text labels
- Remove emoji/symbols from island text: `✓`, `✗`, `✦`, `✨`, `⚡`, `🔐` etc. in card titles,
  capsule text and button titles. Use `oi_icon_status_type` (0 success / 1 fail / 2 error) for
  status ticks, as `PaymentCard` already does.
- `grep -nP "[\x{2600}-\x{27BF}\x{1F300}-\x{1FAFF}]" -r app/src/main/java/com/originisle/android/cards`
  (use `rg` if grep -P fails) to find them.
- Update affected card tests' expected strings.

### T2.4 💻 Timer uses the native left-island timer (only if T0.4d passed)
- In `TimerCard.kt`, put the countdown in left content (timer text per spec) and drop the
  right capsule text. If T0.4d failed, skip and strike this task.

### T2.5 💻 Set the big-card type explicitly (only if T0.4e showed an effect)
- In `BundleCodec.buildBundlePure`, write `island.superx.template` from a new `SuperXRequest`
  field `islandTemplate: Int?` (null → not written). Update `BundleCodecWireFormatTest`.

### T2.6 💻 `operation` semantics (only if T0.4c says op 1 updates)
- Add `isUpdate: Boolean` to `SuperXRequest`. `PlaygroundService` sets it when `id` is already
  in its live-card map. `BundleCodec` writes `operation = if (isUpdate) 1 else 0`.
- Wire-format test for both.

### T2.7 🔌 Visual sign-off
- User screenshots each card type (Cast tab samples) next to a native island (e.g. vivo's own
  timer or a Meituan delivery) and confirms they look like one family. Record in §12.6.

---

## 6. Phase 3: App UI that looks like OriginOS 7 Settings (💻)

The app's own screens should look like a vivo system settings page, not a custom "obsidian"
skin. vivo publishes no public Compose library, so: **use stock Material 3 components with
system colours and the system font, laid out like OriginOS Settings**. Check spacing and corner
radius against vivo's design guidelines on dev.vivo.com.cn (设计规范) and record what you used.

### T3.1 💻 Follow system light/dark + wallpaper colours
- `ui/Theme.kt`: replace the fixed `darkColorScheme` with
  `if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)` (minSdk 34 ≥ 31, so
  always available), `dark = isSystemInDarkTheme()`.
- `IsleColors`: keep **only** semantic status colours (good/bad/warn) and make them read from
  `MaterialTheme.colorScheme` where an equivalent exists. Remove "luxury obsidian" comments.
  Every composable that reads `IsleColors.Background/Surface/Text…` should switch to
  `MaterialTheme.colorScheme.*`. Do it file by file: `grep -n "IsleColors\." app/src/main/java/com/originisle/android/ui`.
- Status/nav bar icon contrast: `isAppearanceLightStatusBars = !dark`.
- `res/values/themes.xml`: parent → `Theme.Material3.DayNight.NoActionBar`; add
  `res/values-night/colors.xml` for `window_background`.
- **Accept:** app follows the system dark-mode toggle; no hard-coded background hex in `ui/`.

### T3.2 💻 System font
- Remove `IsleTypography`'s Plus Jakarta overrides; use `Typography()` defaults (which use the
  system font, vivo Sans on OriginOS). Delete `res/font/plus_jakarta_sans.ttf` and
  `assets/licenses/OFL-PlusJakartaSans.txt` once nothing references them.

### T3.3 💻 Settings-style layout primitives
- In `ui/Components.kt`, define and use everywhere:
  - `SettingsScaffold(title)`: `LargeTopAppBar` that collapses on scroll (`exitUntilCollapsedScrollBehavior`).
  - `SettingsGroup(header?) { … }`: rounded grouped card (corner radius per vivo guideline,
    start with 16dp), surfaceContainer colour, no shadows.
  - `SettingsSwitchRow(title, summary?, checked, onChange)` with stock `Switch`.
  - `SettingsNavRow(title, summary?, onClick)` with a trailing chevron.
- Replace the custom `ToggleRow`/`PillButton`/`CircleIconButton`/`ChoiceChip`/`SegmentedControl`
  usages where a stock M3 equivalent exists (`Switch`, `FilledTonalButton`, `IconButton`,
  `FilterChip`, `SingleChoiceSegmentedButtonRow`). Keep 48dp touch targets.
- Remove decorative animation: the glow in `UpdateDialog.kt:156-177`, any pulsing/aurora
  backgrounds in `HomeScreen.kt` / `IslandPreviewView.kt` (grep `infiniteTransition`).

### T3.4 💻 Split `HomeScreen.kt` (1,512 lines)
Move, unchanged in behaviour, into `ui/home/`:
| From (`HomeScreen.kt`) | To |
|---|---|
| `HomeScreen` body (`:84-955`) | `ui/home/HomeScreen.kt`, further split by section into `StatusSection.kt`, `CardsSection.kt`, `QuietHoursSection.kt` as the code naturally divides |
| `StatusPill`, `MenuItem`, `StatChip` | `ui/Components.kt` |
| `SampleTester`, `pickTime`, `stopAll`, `recastAll` | `ui/home/SampleTester.kt` |
| `CameraCutoutTuner`, `StatusBarPreviewFrame`, `postStatusBarTest` | `ui/tuner/` (3 files) |
- Introduce `ui/PrefsState.kt`: one class exposing each pref as `MutableState`, backed by
  `SharedPreferences` + an `OnSharedPreferenceChangeListener`, replacing the ~50 ad-hoc
  `remember { mutableStateOf(prefs.get…) }` vars. Pref keys as `const val` in `ui/Prefs.kt`.
- **Accept:** no file in `ui/` over 500 lines; app behaves the same (🔌 smoke test by user).

### T3.5 💻 Strings & Chinese UI
- Move all user-visible UI strings to `res/values/strings.xml` (today 4 strings).
- Add `res/values-zh-rCN/strings.xml` with Simplified Chinese translations (the X200 CN UI
  language). Keep English as default.
- Island text produced by cards (e.g. `"$timeFormatted remaining"` in `TimerCard.kt`) also
  goes through resources: pass `context.getString(R.string.timer_remaining, timeFormatted)`.
- **Accept:** switching phone language to 中文 shows Chinese UI and Chinese card text.

### T3.6 💻 Diagnostics screen
- New screen reachable from the overflow menu: shows `SceneCapability.grantedScenes()`,
  probe availability (grant method found? status method found?), current identity flavor,
  OS version (`ro.vivo.os.version` via `SystemProperties` reflection, fallback "unknown"),
  notification-listener connected, battery-optimisation state, last 20 `CastLog` entries.
  Add a "Copy report" button (plain text to clipboard) the user can paste back to the model,
  which makes every future 🔌 task cheaper.

---

## 7. Phase 4: Chinese app coverage & text parsing (💻 + 🔌)

All package names come from §12.2. **Do not add a package that is not in §12.2** unless the
user confirms it. For each card: add packages, add Chinese keywords, add tests with real-looking
Chinese notification text (ask the user for 2–3 real examples per category via screenshot or
`dumpsys notification --noredact` and store them as test fixtures in
`app/src/test/resources/fixtures/cn/<category>.txt`).

Before T4.x, T1.1 must be done: many of these apps already post native islands and will be
skipped automatically; only cast the ones that don't.

| Task | Card / list | Chinese signals to parse | Candidate packages (verify in §12.2) |
|---|---|---|---|
| T4.1 | `DeliveryCard.DELIVERY_APPS` | `骑手`, `已取餐`, `预计送达`, `分钟`, `取件码`, `快递`, `派送中`, `已签收` | Meituan, Ele.me, Cainiao, JD, Taobao, Pinduoduo, SF Express |
| T4.2 | `RideCard.RIDE_APPS` | `司机`, `已接单`, `距您`, `公里`, `分钟`, `车牌`, plate regex `[京沪粤…][A-Z][A-Z0-9]{5,6}` | Didi, Gaode taxi (only if real AMap installed), Caocao, T3 |
| T4.3 | `PAYMENT_APPS` + `PAYMENT_WORDS` | `支付成功`, `付款`, `收款`, `到账`, `元`, `¥`/`￥`, amount regex `[¥￥]\s?\d+(\.\d{1,2})?` and `\d+(\.\d{1,2})?元` | Alipay, WeChat (payments only: detect `微信支付` title, not chats), UnionPay, bank apps |
| T4.4 | `NAV_APPS` | `米后`, `左转`, `右转`, `直行`, `掉头`, `进入`, `到达` | Baidu Maps (present), Tencent Maps, AMap (if decision A) |
| T4.5 | `CallCard.CALL_PACKAGES` | see T1.3 | CN dialer/in-call UI from §12.2, WeChat VoIP (ongoing only) |
| T4.6 | `TimerCard.CLOCK_APPS` | `计时器`, `倒计时`, `闹钟`, `秒表`, `剩余` | CN vivo clock package from §12.2 |
| T4.7 | `MeetingCard.MEETING_APPS` | `会议`, `即将开始`, `入会`, `会议号` | Tencent Meeting, DingTalk, Feishu |
| T4.8 | Train/flight → `GenericCard` with ongoing template | `检票口`, `车次`, `座位`, `登机口`, `航班` | 12306, Ctrip, Fliggy, Umetrip |
| T4.9 | Media | — | NetEase Cloud Music, QQ Music, Kugou: **only** if not already shown by Origin player (T5.1) |
| T4.10 | `IntelligentTextSanitizer` | strip `[n条]`, `(n条新消息)`, `【App名】` prefixes, `[图片]`/`[语音]` → keep as-is but short | — |
| T4.11 | `OtpDetector` | add `校验码`, `动态码`, `动态密码`, `确认码`; check the candidate regex picks the digits after `验证码：` / `验证码是` | — |

For each row: 💻 implement + JVM test from fixtures; 🔌 user confirms one real event.

---

## 8. Phase 5: OriginOS 7 native features (💻 + 🔌)

### T5.1 Media hand-off to Origin player
- OriginOS 7 can show any audio app on the island natively (Settings → Origin player → all apps).
- 🔌 Ask the user to enable that for their music apps, then play music with our media casting on
  and off. Record in §12.7 whether we create a duplicate.
- 💻 If native media covers the user's apps: default `cast_media_sessions` to **false**, and in
  `MediaCard` path skip packages where the native player handles it (when T1.1 already catches
  it via superx extras, nothing extra is needed). Keep the toggle for apps vivo doesn't cover.

### T5.2 Android 16+ Live Updates as primary progress source
- `GenericCard.kt:118-120, 166-294` already reads `ProgressStyle`/promoted-notification extras
  by raw key. Make that path win over keyword heuristics in `DownloadCard`, `DeliveryCard`,
  `RideCard`: if `Notification.EXTRA_PROGRESS`/`ProgressStyle` data exists, use it.
- 💻 tests with synthetic extras.

### T5.3 Temporary per-app mute
- Long-press (or menu) on an entry in the Activity/Log screen → "Mute this app for 1 h / until
  tomorrow". Store `mute_until_<pkg>` epoch ms; check it in `isAppEnabled`
  (`NotificationCastListener.kt:614`). Pure helper + test.

### T5.4 Target the OS's real API level
- Using T0.1's `ro.build.version.sdk`: if it is 37, bump `compileSdk`/`targetSdk` to 37, AGP to
  a version that supports 37, update `API_LEVEL` in CI, read the Android 17 behaviour-changes
  page via the Context7 connector or web, and fix what applies (foreground-service types,
  `PendingIntent` mutability, background activity starts from `CardIntents`, broadcast
  receivers' `exported` flags). If it is 36, strike this task.

---

## 9. Phase 6: Robustness on the CN ROM (💻 + 🔌)

### T6.1 Keep-alive guidance specific to OriginOS 7 CN
- Onboarding (`ui/OnboardingScreen.kt`, `ui/PermissionsSheet.kt`): deep-link to the CN
  settings pages (i管家 / 后台耗电管理 → 允许后台高耗电; 自启动; 关联启动). Use
  `Intent` component names verified on the device (🔌 ask user to run
  `adb shell dumpsys activity activities | grep -i mResumed` while on each page and record in §12.8).
- Fallback to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` if a component is missing.

### T6.2 Real-AMap conflict check (only if T0.3 decision = B)
- Can't detect the real AMap while we *are* that package. Instead: README + onboarding note, and
  the update checker refuses to suggest updates if `packageManager.getInstallerPackageName`
  shows a store installer (meaning a real AMap took the slot).

### T6.3 Updater on Chinese networks
- `ui/UpdateChecker.kt`: connect/read timeouts ≤ 10 s, single retry, a clear "Can't reach
  GitHub (network)" state instead of a spinner. Optional mirror base-URL pref (empty by default).
  Signature + SHA-256 verification (`security/ApkVerifier.kt`) must still run for any source.
- Daily check (`UpdateCheckReceiver`) must never post more than one failure notification a day.

### T6.4 Remaining items carried over from `GOD-LEVEL-PLAN.md`
Do after Phases 1–3: `CardHandler` registry replacing the if/else chain
(`GOD-LEVEL-PLAN.md` §4.1/Phase 1.1), `RaidenStateReceiver` token auth (P2-18), Raiden id-band
collision (P1-8), copy-paste collapse (P2-16). Follow that file's instructions; they still apply.

---

## 10. Order of execution (summary)

| Order | Tasks | Needs device? |
|---|---|---|
| 1 | T0.1, T0.2 | 🔌 |
| 2 | T0.3 code part (flavors) → 🔌 test → decision | 💻 + 🔌 |
| 3 | T0.4 (samples coded 💻, run 🔌) | 💻 + 🔌 |
| 4 | T1.1 → T1.7 | 💻 (T1.1/T1.2 confirm 🔌) |
| 5 | T2.1 → T2.6, then T2.7 | 💻 then 🔌 |
| 6 | T3.6 diagnostics first (it speeds up all later 🔌 work), then T3.1 → T3.5 | 💻 |
| 7 | T4.x per category, after collecting fixtures | 💻 + 🔌 |
| 8 | T5.x | 💻 + 🔌 |
| 9 | T6.x | 💻 + 🔌 |

Release after each of steps 4, 5, 6 and 7: bump `versionCode`/`versionName` in
`app/build.gradle.kts`, add `release-notes-vX.Y.Z.md`, the user runs `scripts/release.sh`
(it needs the local keystore, so the model can't run it).

---

## 11. Explicit non-goals

- Supporting global/Funtouch ROMs, OriginOS 6, non-vivo devices beyond the existing "unsupported" banner.
- Template 7 (custom RemoteViews) or any custom-drawn island content.
- Play Store distribution.
- Replacing Compose/Material 3 with a vivo SDK (none is public for this).
- New "effects" (glows, gradients, animated spans) anywhere on the island.

---

## 12. Device findings log (fill in as 🔌 tasks complete; commit each update)

### 12.1 Platform (T0.1)
| Key | Value |
|---|---|
| ro.product.model | _pending_ |
| ro.build.version.sdk | _pending_ |
| ro.vivo.os.version | _pending_ |
| SceneCapability grant log line | _pending_ |

### 12.2 Package census (T0.2)
| Category | Package(s) on the user's X200 |
|---|---|
| Dialer / in-call UI | _pending_ |
| Clock | _pending_ |
| Music (system) | _pending_ |
| Maps | _pending_ |
| Food / parcel delivery | _pending_ |
| Ride-hailing | _pending_ |
| Payments / banks | _pending_ |
| Train / flight | _pending_ |
| Meetings | _pending_ |
| Messaging | _pending_ |
| Music (3rd-party) | _pending_ |

### 12.3 Identity decision (T0.3)
_pending: A (native works) or B (spoof required), with evidence_

### 12.4 Protocol truth table (T0.4)
| # | Result |
|---|---|
| a scenes rendered | _pending_ |
| b forceShow required | _pending_ |
| c operation=1 | _pending_ |
| d left timer text | _pending_ |
| e island.superx.template | _pending_ |
| f lightEffectInfo | _pending_ |
| g two scenes stack | _pending_ |
| h templates 2 / 5 | _pending_ |

### 12.5 Native-duplicate checks (T1.1, T1.2)
_pending_

### 12.6 Visual sign-off (T2.7)
_pending_

### 12.7 Origin player media (T5.1)
_pending_

### 12.8 CN settings components (T6.1)
_pending_
