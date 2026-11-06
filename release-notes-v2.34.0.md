v2.34.0 — Security & Correctness

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
  45:8C:47:85:FE:6D:CD:F1:29:30:AA:AF:9D:AD:5B:AD:91:25:19:B1:AA:E9:7C:68:CC:F9:ED:84:90:91:9A:E8
