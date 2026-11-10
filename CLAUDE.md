# CLAUDE.md

**Target:** vivo X200 (China ROM) on OriginOS 7 only.

**Active plan:** `docs/X200-CN-OS7-PLAN.md`. Read its §0 rules before doing anything, then continue
from the first task that is not done. Record device results in its §12 log; never invent them.

- Never change `applicationId` (`com.autonavi.minimap`, the AMap spoof) outside plan task T0.3.
- Island visuals must be native OriginOS 7: no invented glows, gradients, emoji or custom colours.
- Protocol reference: `docs/PROTOCOL.md`. Background audit: `docs/GOD-LEVEL-PLAN.md` (older; the plan above wins on priorities).
- Tests: `./gradlew --no-daemon :app:testDebugUnitTest`; style: `:app:ktlintCheck`. CI runs both.
- One task per commit: `type(scope): summary (Txx)`.
