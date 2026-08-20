# Spec: Saved Cards Vault

## Objective

Add a **Cards** vault next to budget notes so the user can store payment and ID card data locally, attach front/back photos, auto-fill payment fields from a scan when possible, and **copy fields (including CVV)** when paying online without finding the physical card.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3 (existing)
- Room DB `cards.db` (separate from budget notes for backup exclusion)
- App-private images: `files/cards/{cardId}/front.jpg`, `back.jpg`
- System camera + Photo Picker for capture
- ML Kit Text Recognition (on-device, no INTERNET)
- Navigation Compose with bottom tabs

## Commands

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew test assembleDebug
```

## Project Structure

```
data/          SavedCard, CardCustomField, CardType, CardsDatabase, DAOs
repository/    CardRepository
ocr/           CardOcrHelper, PaymentCardParser
ui/cards/      home, editor, components
ui/root/       RootScaffold with Notes | Cards tabs
navigation/    Nested note + card graphs
```

## Code Style

Match existing Room entities, ViewModel factories, Compose Material 3 patterns (Scaffold, TopAppBar, FAB, LazyVerticalGrid).

## Testing Strategy

- Unit: `PaymentCardParser` with sample OCR text
- Existing money/totals tests unchanged
- Manual: create payment/ID, OCR, copy, uninstall wipe

## Boundaries

- **Always:** local-only; mask PAN/CVV by default; exclude `cards.db` and `files/cards` from backup
- **Ask first:** app rename for wallet; network; encryption Phase 2
- **Never:** upload card data; log CVV/PAN; store secrets in git

## Success Criteria

- Bottom tabs Notes | Cards; notes unchanged
- Payment + ID cards with front/back images
- OCR fills empty payment fields; ID supports custom fields
- Copy-to-clipboard for online pay
- Data wiped on uninstall; card backup excluded

## Locked decisions

- Layout: bottom tabs (Option 1)
- Photos: keep front and back
- CVV: stored for personal online copy/paste
- OCR: on-device ML Kit
- Encryption: Phase 2 (SQLCipher, EncryptedFile, biometric lock)
