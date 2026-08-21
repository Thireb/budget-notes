# Budget Notes

Private, local-only budget notes and card vault for Android (Kotlin, Jetpack Compose, Material 3, Room + SQLCipher).

No accounts, no network. Data is gated by **fingerprint / device lock** (app PIN only if the phone has neither), encrypted at rest, and removed on uninstall.

See [docs/SPEC_CARDS.md](docs/SPEC_CARDS.md) and [docs/SPEC_SECURITY.md](docs/SPEC_SECURITY.md).

## Requirements

- JDK 17+ (21 recommended)
- Android SDK (see [docs/TOOLCHAIN.md](docs/TOOLCHAIN.md))

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew test assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
(Package id: `com.budgetnotes.app.debug`)

## Privacy / security

- SQLCipher encryption for notes and cards databases (Keystore-wrapped key; PIN fallback when needed)
- Card photos encrypted (AES-GCM) in app-private storage; in-app camera (fields entered manually — no OCR)
- Screenshots blocked (`FLAG_SECURE`); copied card fields clear from clipboard after ~45s
- Android Backup disabled for vault data

## Play Store

See [docs/PLAY_STORE.md](docs/PLAY_STORE.md) for upload keystore setup and `bundleRelease`.

## License

Add a license before publishing if desired.
