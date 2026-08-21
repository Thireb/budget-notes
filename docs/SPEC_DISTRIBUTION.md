# Spec: Play + FOSS distribution & tip jar

## Objective

Ship Budget Notes on **Google Play** (optional) and **FOSS via GitHub Releases + Obtainium** (IzzyOnDroid declined — AI policy; F-Droid later if desired) as the **same local vault** (photos yes, **OCR removed**), with an optional external **support / tip** link — no ads, no AdMob, no Play Billing required for v1.

## Assumptions (confirm or correct)

1. **Single codebase / build** for Play and FOSS regarding features — **no ML Kit / OCR** in any build. Product flavors remain optional later only if store packaging differs.
2. **Same `applicationId`** `com.budgetnotes.app` (GitHub/Obtainium APK and Play AAB share the id — users should pick **one** install source).
3. **Tip = external support link** (not Buy Me a Coffee / Stripe — creator is in Pakistan where those don’t pay out). Exact provider TBD. Opens via `ACTION_VIEW`. Optional; never required.
4. **Tip entry point** — About / Settings: “Support the developer” (or similar).
5. **No `INTERNET` permission** — vault stays local-only in the manifest.
6. **FOSS license** — Apache-2.0 or GPL-3.0 before public release.
7. **Ads** — out of scope.
8. **Auto-lock timeout** — out of scope.

## User-facing behavior

| | Play | GitHub + Obtainium |
|---|---|---|
| Notes + encrypted cards | Yes | Yes |
| Camera / gallery photos on cards | Yes | Yes |
| OCR / auto-fill from photo | No | No |
| Support tip link | Yes (when URL set) | Yes (when URL set) |

## Success criteria

- [x] No ML Kit / GMS OCR deps in the app
- [ ] Support link wired when URL chosen
- [ ] Manifest still has no `INTERNET` permission
- [ ] LICENSE file in repo
- [x] README documents Obtainium / Releases install
- [ ] Tip URL documented when chosen

## Open questions

1. Support URL (Zindigi Pay Link / other PK-friendly link / skip for now)
2. License: Apache-2.0 or GPL-3.0?
3. Same applicationId vs `.foss` suffix?
