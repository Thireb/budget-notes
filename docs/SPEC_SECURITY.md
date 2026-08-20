# Spec: App Lock & Encryption (Phase 2)

## Objective

Keep budget notes and card vault data **encrypted at rest** on device, gated by **fingerprint / device lock** (app PIN only when the device has neither), with screenshots blocked and sensitive clipboard contents cleared automatically.

## Success criteria

- First launch: if the device has fingerprint/face or a screen lock, show the system biometric prompt immediately and create a Keystore-wrapped vault key (no app PIN required)
- Devices without biometric/screen lock: fall back to a 6-digit app PIN
- Subsequent launches: same biometric/device-credential prompt
- `budget_notes_secure.db` and `cards_secure.db` encrypted with SQLCipher
- Card front/back images encrypted with AES-GCM
- `FLAG_SECURE` on the activity
- PAN/CVV clipboard auto-clears after 45 seconds
- Android Backup disabled
- Uninstall wipes all data
- Existing plaintext DBs/images migrated once on first unlock

## Tech

| Piece | Choice |
|---|---|
| DB encryption | SQLCipher Community (`net.zetetic:sqlcipher-android`) + Room `SupportOpenHelperFactory` |
| Key derivation | PBKDF2-HMAC-SHA256, 310k iterations, 32-byte key, random 16-byte salt |
| Images | AES-256-GCM, IV prepended, key = SHA-256(dbKey \|\| "card-images-v1") |
| Biometric | AndroidX BiometricPrompt unlocking a Keystore-wrapped copy of the DB key |
| Lock UI | Compose PIN pad + optional biometric button |

## Boundaries

- Always: never log PIN/keys; clear key arrays after use where possible; no network
- Ask first: changing PIN (re-encrypt) UX, cloud recovery
- Never: store raw PIN; upload vault data

## Out of scope (later)

- PIN change / rekey flow UI polish
- Auto-lock timeout settings screen
- Stealth / decoy PIN
