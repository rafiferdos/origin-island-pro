v2.34.0 — Security & Correctness

⚠️ SIGNING KEY ROTATED — READ THIS BEFORE INSTALLING
- The original release key is no longer available, so v2.34.0 is signed with a new one.
- Android refuses to install an APK signed by a different key over an existing install.
- **Uninstall v2.33.0 first.** This clears the app's data and its notification-access grant,
  so you will need to re-enable notification access and re-set your preferences.
- Nothing else about the app changes; only the signature does.
- The old key was `45:8C:47:85…`. The new key is below.

Update pipeline (the important one)
- An update can no longer be installed without verification. Origin Island Pro reads every
  notification on your device, so a tampered update was the worst available compromise.
- The release signing certificate is now checked before every install, against the
  fingerprint published in the README. A mismatched signature is refused and the file deleted.
- A downloaded APK is also checked against the release's published SHA-256 checksum when one
  is available.
- A previously-downloaded APK is no longer trusted just because its file size matches — it must
  verify first. A file that fails verification is deleted, not offered for install.
- Download redirects are now restricted to GitHub's asset hosts; previously any redirect
  target was followed.

Live scores
- Fixed a false negative: any score notification containing the word "position"
  (a table-position change, "your position in the queue") was permanently suppressed from the
  live-score card. Removed from the recap filter; recaps are still detected.

Apps
- The per-app switch now has a real opt-in mode. Previously the allow-list was read by the
  listener but never written by anything, so it did nothing. Opt-in mode blocks every app
  except the ones you switch on.

Tooling (no user-visible change)
- Added CI: unit tests, Kotlin lint, and a debug build run on every push. Previously only a
  release-checksum checker existed, so the 95 unit tests never ran automatically.
- Added ktlint with every rule enabled; pre-existing style violations are recorded in a
  baseline so new ones still fail.
- Fixed the signing-certificate fingerprint in docs/DEV.md, which disagreed with the README
  and the actual published APK.

Verify a download
  shasum -a 256 origin-island-pro-2.34.0.apk
  apksigner verify --print-certs origin-island-pro-2.34.0.apk

Expected certificate SHA-256:
  D8:52:11:80:FE:AA:EE:30:37:21:DD:1F:7E:E2:88:39:60:35:42:66:0C:07:A7:45:39:9C:84:3A:99:20:14:5F
