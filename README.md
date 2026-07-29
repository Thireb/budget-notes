# Budget Notes

Private, local-only budget note-taking for Android (Kotlin, Jetpack Compose, Material 3, Room).

No accounts, no network — each note tracks Add / Deduct line items and a live total.

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

## Play Store

See [docs/PLAY_STORE.md](docs/PLAY_STORE.md) for upload keystore setup and `bundleRelease`.

## License

Add a license before publishing if desired.
