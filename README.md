# Budget Notes

Private, local-only budget note-taking and card vault for Android (Kotlin, Jetpack Compose, Material 3, Room).

No accounts, no network — budget notes track Add / Deduct line items and a live total. The **Cards** tab stores payment and ID cards (front/back photos, on-device OCR for payment fields, custom ID fields, copy-to-clipboard for online checkout).

See [docs/SPEC_CARDS.md](docs/SPEC_CARDS.md) for the cards vault specification. Encryption / app lock is planned as a follow-up.

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

## Privacy notes

- All data stays in app-private storage and is removed when the app is uninstalled.
- `cards.db` and card photos are excluded from Android cloud backup (PAN/CVV are sensitive; full encryption is Phase 2).

## Play Store

See [docs/PLAY_STORE.md](docs/PLAY_STORE.md) for upload keystore setup and `bundleRelease`.

## License

Add a license before publishing if desired.
