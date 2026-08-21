# Spec: IzzyOnDroid launch (closed)

## Outcome

**Declined** — [repodata#482](https://codeberg.org/IzzyOnDroid/repodata/issues/482) (2026-08-21).  
Reason: LLM assistance exceeded IzzyOnDroid’s [App Inclusion AI Policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/). Do not resubmit this codebase.

**Current FOSS path:** GitHub Releases + [Obtainium](OBTAINIUM.md).

## Objective (original)

Publish Budget Notes on **IzzyOnDroid** so users can install/update a signed FOSS release without relying on Play or long-lived GitHub APK hosting.

## What we completed before the decision

1. [x] `LICENSE` (Apache-2.0) + README license line
2. [x] Public GitHub repo (`Thireb/budget-notes`)
3. [x] Release keystore (`keystore/` + `keystore.properties`, gitignored)
4. [x] Shrink release APK (minify + arm ABI filters) — ~12 MB signed
5. [x] Fastlane under `fastlane/metadata/android/en-US/`
6. [x] Phone screenshots in Fastlane
7. [x] Tag `v1.0.0` with signed `BudgetNotes-1.0.0.apk`
8. [x] Inclusion request filed (Codeberg) — **rejected on AI policy**

## Success criteria (revised)

- [x] Licensed FOSS release on GitHub
- [x] Signed APK ≤ ~30 MB on a git tag
- [x] User update path documented ([OBTAINIUM.md](OBTAINIUM.md))
- [x] Izzy path closed; no further inclusion attempts for this app
