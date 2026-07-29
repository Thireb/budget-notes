# Toolchain inventory (what was installed for Budget Notes)

This documents **system / user-local changes made while building this project**, and how to remove them.

## Summary

| Item | Location | Approx. size | Installed via |
|---|---|---|---|
| Android SDK (cmdline-tools + packages) | `/home/ahmad/Android/Sdk` | ~596 MB | Google zip + `sdkmanager` (not pacman) |
| Gradle user home for this project | `/mnt/fast-storage/budget-notes/.gradle-home` | ~1.7 GB | Gradle Wrapper downloads |
| Temporary cmdline-tools download | `/tmp/cmdline-tools.zip`, `/tmp/cmdline-tools-extract` | ~154 MB | `curl` / `unzip` |

**No new Arch/pacman packages were installed** for this project. These were already on the machine and left unchanged:

- `jdk21-openjdk`
- `android-tools` (provides `adb`)
- `android-udev`
- `gradle` (system Gradle; the app builds with the **project Gradle Wrapper**, not this package)

---

## 1. Android SDK — `/home/ahmad/Android/Sdk`

Created under your home directory (`ANDROID_HOME` / `sdk.dir` in `local.properties`).

### Contents installed

Command-line tools unpack layout:

- `cmdline-tools/latest/` — Android SDK Command-line Tools
- `licenses/` — accepted SDK licenses

Packages reported by `sdkmanager --list_installed`:

| SDK package ID | Description |
|---|---|
| `cmdline-tools` (under `cmdline-tools/latest`) | SDK manager / avdmanager, etc. |
| `platform-tools` | Android platform-tools (SDK copy; system also has `adb` via pacman) |
| `platforms;android-35` | Android SDK Platform 35 |
| `build-tools;35.0.0` | Build-Tools 35.0.0 (requested at setup) |
| `build-tools;34.0.0` | Build-Tools 34.0.0 (pulled later, typically by Android Gradle Plugin) |

Pointed at by project file:

```properties
# local.properties (gitignored)
sdk.dir=/home/ahmad/Android/Sdk
```

### How it was installed

```bash
# Downloaded Google commandlinetools-linux zip → unpacked to:
#   ~/Android/Sdk/cmdline-tools/latest
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=~/Android/Sdk --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=~/Android/Sdk \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### How to remove

**Remove the entire SDK (recommended if you only added it for this app):**

```bash
rm -rf /home/ahmad/Android/Sdk
# optional: remove empty parent
rmdir /home/ahmad/Android 2>/dev/null || true
```

**Or uninstall individual SDK packages** (keeps cmdline-tools):

```bash
export ANDROID_HOME=/home/ahmad/Android/Sdk
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" --uninstall \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "build-tools;34.0.0"
```

After removing the SDK, builds will fail until you install another SDK and update `local.properties`.

---

## 2. Project Gradle cache — `.gradle-home/`

Gradle Wrapper was configured to use a **project-local** user home (not `~/.gradle`) so caches stay with the repo workspace:

```text
/mnt/fast-storage/budget-notes/.gradle-home/
```

Contains: Gradle 8.9 distribution, dependency caches, Android build transforms, daemons metadata, etc.

### How to remove

```bash
cd /mnt/fast-storage/budget-notes
rm -rf .gradle-home .wrapper-gen .gradle app/build build
```

Next `./gradlew` run will re-download Gradle and dependencies (needs network).

---

## 3. Temporary files in `/tmp`

Left over from the SDK cmdline-tools download:

```bash
rm -f /tmp/cmdline-tools.zip
rm -rf /tmp/cmdline-tools-extract
```

Safe to delete anytime; not required for builds.

---

## 4. What was *not* installed

- No `paru` / AUR packages (`android-sdk`, Android Studio, etc.)
- No Android emulator / system images / KVM packages
- No changes to pacman-managed JDK, `adb`, or system `gradle`
- No GitHub / CI setup

---

## Rebuild after a clean toolchain

If you wiped the SDK and Gradle caches:

1. Reinstall SDK under `~/Android/Sdk` (or another path) as in section 1.
2. Set `sdk.dir=...` in `local.properties`.
3. From the project root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/home/ahmad/Android/Sdk
export GRADLE_USER_HOME=/mnt/fast-storage/budget-notes/.gradle-home
./gradlew test assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk dist/BudgetNotes-debug.apk
```
