# Spec: IzzyOnDroid launch

## Objective

Publish Budget Notes on **IzzyOnDroid** so users can install/update a signed FOSS release without relying on Play or long-lived GitHub APK hosting.

## Requirements (from Izzy policy)

| Requirement | Our status |
|-------------|------------|
| OSI/FSF license in repo | Missing — must add |
| Public source (GitHub/Codeberg/…) | Remote exists (`Thireb/budget-notes`) — must be **public** |
| No proprietary / trackers | OCR/ML Kit removed — good |
| No ads/analytics | Good |
| Fastlane metadata (short + full desc, icon, screenshots) | Missing |
| Tagged GitHub/Codeberg **release** with signed `.apk` | Missing |
| Release APK not debuggable | Use `assembleRelease` |
| APK size ~≤ **30 MB** (rule of thumb) | Debug ~62 MB — **must shrink release** |
| Unique package id | `com.budgetnotes.app` |

## Assumptions

1. License: **Apache-2.0** unless you choose GPL-3.0.
2. Tip/support link: **out of APK for v1** (Zindigi 7‑day links); mention support only in README/store text later.
3. Same package id for Izzy as Play later.
4. First Izzy version = `1.0.0` / `versionCode 1`.
5. Prefer **arm64-v8a + armeabi-v7a** only (drop x86) to cut native lib size (SQLCipher).

## Launch checklist

1. [x] Add `LICENSE` (Apache-2.0) + README license line
2. [ ] Make GitHub repo **public** (or Codeberg)
3. [ ] Create release keystore (`docs/PLAY_STORE.md` / keystore.properties)
4. [x] Shrink release APK (minify + phone ABI filters) — unsigned ~24 MB before ABI cut
5. [x] Add Fastlane under `fastlane/metadata/android/en-US/`
6. [x] Add 2–4 phone screenshots into Fastlane (captured via wireless adb debug build)
7. [ ] Tag `v1.0.0`, attach **signed** `app-release.apk` to GitHub Release
8. [ ] File IzzyOnDroid inclusion request: https://gitlab.com/IzzyOnDroid/repo/-/issues

## Success criteria

- [x] Licensed in repo (Apache-2.0)
- [x] Fastlane text + icon path present
- [x] Screenshots present
- [ ] Signed release APK on a git tag, size ≤ ~30 MB
- [ ] Inclusion issue filed
- [ ] Repo is public
