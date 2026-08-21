# Install & update with Obtainium

Budget Notes is distributed via **GitHub Releases**. [Obtainium](https://github.com/ImranR98/Obtainium) can install the app and notify you when a new tagged release ships.

IzzyOnDroid inclusion was **declined** (App Inclusion AI Policy). GitHub + Obtainium is the supported FOSS update path for now.

## For users

### 1. Install Obtainium

- GitHub: https://github.com/ImranR98/Obtainium/releases  
- Or F-Droid / IzzyOnDroid listing for Obtainium itself

### 2. Add Budget Notes

1. Open Obtainium → **Add App**
2. **App source URL:**

   `https://github.com/Thireb/budget-notes`

3. Recommended options (if shown):
   - Leave **Include pre-releases** off
   - **APK filter** (optional): `BudgetNotes` — matches `BudgetNotes-*.apk`
4. Tap **Add**, then install when prompted (allow Obtainium to install apps)

Latest APK: https://github.com/Thireb/budget-notes/releases/latest

### 3. Verify the signing certificate (optional but recommended)

Release builds are signed with this certificate. Compare in Obtainium / [AppVerifier](https://github.com/soupslurpr/AppVerifier) after install:

| Digest | Value |
|--------|--------|
| **SHA-256** | `6760d8e3e55ab260d99f94c277a5b2eea88c53cded995973c1e38f52417ff2ad` |
| Package | `com.budgetnotes.app` |

If the SHA-256 differs, do not trust that APK.

### Manual install (without Obtainium)

1. Download `BudgetNotes-*.apk` from [Releases](https://github.com/Thireb/budget-notes/releases)
2. Open the file on your phone and install (allow install from that source once)
3. For updates, repeat or switch to Obtainium

## For maintainers

Each update users receive through Obtainium requires:

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`
2. `./gradlew clean assembleRelease` (with `keystore.properties` present)
3. Tag + GitHub Release (e.g. `v1.0.1`) and attach **`BudgetNotes-<version>.apk`** (same signing key as v1.0.0)
4. Prefer one primary `.apk` asset per release so Obtainium does not need a filter

Do **not** rotate the upload/release keystore; Obtainium / Android treat a new signer as a different app.
