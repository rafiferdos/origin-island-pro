# Origin Island Pro v2.13.0

### Vivo OriginOS 6-safe optimisation + Privacy mode

No new SuperX scene, template, or bundle field is introduced in this release.
Everything below rides on paths OriginOS 6 already grants: the single granted
`NAVIGATION` carrier scene, templates 3/4/8, and the existing `oi_priority`
values. No Play Store distribution is intended — this remains a personal
sideload, as before.

### What's new

#### 1. Privacy mode (opt-in, default OFF)
- New toggle in Home > Cards: **Privacy mode**.
- When ON:
  - Message cards show the sender + app, but the message body on the island
    becomes "New message" — pill and expanded card alike.
  - Payment cards keep the same spinner -> tick animation, but the amount
    and merchant are hidden: phase 1 reads "Payment complete", phase 2
    reads "Paid".
- OTP and 2FA cards are deliberately untouched: hiding a code you are
  waiting to copy would break their purpose.

#### 2. Adaptive polling (battery)
- The notification listener previously swept all active notifications
  every 1 second, even with nothing live.
- Now: 1s refresh only while a live card is active (media, ticking timer,
  download, live score — so download Mbps and timers stay smooth);
  otherwise 3s. This cuts the idle sweep + binder calls to roughly a third,
  with no change to what vivo renders.

### No behaviour change otherwise
- Same scenes, templates, priorities, and card layouts.
- Privacy mode defaults to OFF, so existing installs behave exactly as before
  until the user turns it on.

### Release Asset Verification
- File: `origin-island-pro-2.13.0.apk`
- Verify with the published `.sha256` file and the maintainer's signing
  certificate before installing, as documented in the README.
  (NOTE: do not publish this release from a machine without the maintainer
  keystore — a differently-signed APK cannot update existing installs.)
