# Origin Island Pro
<div align="center">
  
[![Release](https://img.shields.io/github/v/release/rafiferdos/origin-island-pro)](https://github.com/rafiferdos/origin-island-pro/releases)
[![Downloads](https://img.shields.io/github/downloads/rafiferdos/origin-island-pro/total)](https://github.com/rafiferdos/origin-island-pro/releases)

Brings the OriginOS **island** to *every* app: downloads, navigation, calls, payments, media and
live football scores, including from European and international apps.

**Credits** · OriginIsland api by [@theVakhovskeIsTaken](https://github.com/theVakhovskeIsTaken)
· Navigation card by [@Martimborgss](https://github.com/Martimborgss)
· Original project by [@fvhde](https://github.com/fvhde/origin-isle)

</div>

<table align="center">
  <tr>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/island-navigation.jpg" width="250"><br>
      <sub><b>Navigation</b></sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/island-payment.jpg" width="250"><br>
      <sub><b>Payment</b></sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/island-call.jpg" width="250"><br>
      <sub><b>Call</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <img src="docs/screenshots/media.jpg" width="250"><br>
      <sub><b>Media</b></sub>
    </td>
    <td align="center" valign="middle" colspan="2">
      <img src="docs/screenshots/island-football.jpg" width="520"><br>
      <sub><b>Live football</b></sub>
    </td>
  </tr>
</table>

## Features

- 🏝️ **Any notification** → island, with an allow/deny list per app
- 💬 **Smart Messaging Cards** — WhatsApp, Messenger, Telegram, IMO, Viber, Discord, Instagram DM, Signal, SMS:
  - **Compact Island Pill**: Shows app icon on the left + clean sender name (smart handling for prefixes like `Md.`, `Dr.`), with message preview on the right.
  - **Expanded Island Card**: Shows sender name, full message body, sender avatar photo, source app label, and quick action buttons.
- 👆 **Tap a card** to open the original notification
- ⚽ **Live football scores** with team crests (SofaScore, FotMob, FlashScore, 365Scores…)
- ৳ **Mobile Financial Services (MFS) & Payments** — Apple Pay–style success animation for bKash, Nagad, Rocket, Upay, Cellfin, Citytouch, Astha, EBL, NexusPay (`৳`, `Tk`, `BDT`), Google Wallet, Revolut, PayPal, Wise, Binance…
- 🛵 **Food & Ride Delivery tracking** — Pathao, Foodpanda, Daraz, Chaldal, Shohoz, Uber Eats, Amazon, DHL, FedEx, UPS…
- 🌦️ **Weather alerts** — storm warnings, rainfall updates and temperatures
- 🎛️ **Every card type has its own switch** — messages, payments, delivery, weather, football, login codes, charging…
- 📊 **Real-time activity stats** — live cast/skip dashboard on Home and multi-category log filtering
- 🎵 **Media controls** wired to the real app with waveform visualization
- 📋 **Log tab** showing what was cast or skipped, and why

> [!WARNING]
> **vivo/iQOO + OriginOS 6 only.** Enable **OriginOS Navigation** first, and uninstall the real
> **AMap (高德地图)** app if you have it ([why?](docs/DEV.md)).

<details>
<summary>Where to enable OriginOS Navigation</summary>
<br>
<img src="docs/screenshots/pre-req.jpg" width="300">
</details>

## Install

1. Download the APK from **[Releases](https://github.com/rafiferdos/origin-island-pro/releases)** and install it.
2. Open Origin Island Pro and grant **notification access** (required).
3. Test with a sample card in the **Cast** tab.

> [!TIP]
> For reliable background running, also enable:
> **Battery → Unrestricted** · **Auto-start → Allow** · **Accessibility → Origin Island Pro keep-alive**

<details>
<summary><b>Verify your download</b></summary>

```sh
shasum -a 256 origin-island-pro-<version>.apk   # must match the .sha256 file
apksigner verify --print-certs origin-island-pro-<version>.apk
```
Expected certificate:
```
45:8C:47:85:FE:6D:CD:F1:29:30:AA:AF:9D:AD:5B:AD:91:25:19:B1:AA:E9:7C:68:CC:F9:ED:84:90:91:9A:E8
```
Doesn't match? **Don't install.**
</details>

<details>
<summary><b>App screenshots</b></summary>
<br>

| Cast | Apps | Log |
|:-:|:-:|:-:|
| <img src="docs/screenshots/cast.jpg" width="240"> | <img src="docs/screenshots/apps.jpg" width="240"> | <img src="docs/screenshots/log-tab.jpg" width="240"> |
</details>

<details>
<summary><b>Developers</b></summary>

Building, the AMap package spoof, and signing: [docs/DEV.md](docs/DEV.md)

> Switching between a self-built and an official APK requires uninstalling first (different
> signatures), so you'll need to re-grant permissions afterward.
</details>

---

**License** · Source-available, no redistribution. See [LICENSE](LICENSE)
