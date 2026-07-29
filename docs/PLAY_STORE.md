# Play Store release guide

Budget Notes is a **local-only** app (Room/SQLite, no network permissions). Play Console uploads must be an **Android App Bundle (`.aab`)**, not a debug APK.

## One-time: create an upload keystore

```bash
cd /mnt/fast-storage/budget-notes   # or your clone path
mkdir -p keystore
keytool -genkeypair -v \
  -keystore keystore/budget-notes-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias budgetnotes
```

Copy the example properties file and fill in real passwords (never commit this file):

```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties
```

`keystore/`, `*.jks`, and `keystore.properties` are gitignored.

Prefer **Play App Signing**: upload the AAB; let Google manage the app-signing key. Keep your upload keystore backed up offline.

## Build the Play Store bundle

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew clean bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

Upload that `.aab` in Play Console → Production (or Internal testing first).

## Versioning

In [`app/build.gradle.kts`](../app/build.gradle.kts):

- `versionCode` — integer, must increase for every Play upload
- `versionName` — user-visible string (currently `1.0.0`)

Bump both before each store release.

## Debug vs release package names

- Release: `com.budgetnotes.app` (Play Store listing)
- Debug: `com.budgetnotes.app.debug` (sideload without clobbering the store build)

## Play Console checklist (Data safety / content)

- No internet permission; no accounts; data stays on device (Room DB)
- Optional device backup via Android Backup / cloud-backup rules (declare in Data safety if you leave `allowBackup` enabled)
- Target API: 35 (`targetSdk = 35`)
- Privacy policy URL: only required if you collect user data; for a fully on-device notes app you typically declare **no data collected / no data shared**, and may still want a short privacy policy page stating that

## What not to commit

- `keystore.properties`, `keystore/*.jks`
- `local.properties`, `.gradle-home/`, `*.aab`, `*.apk`
