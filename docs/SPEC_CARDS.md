# Spec: Saved Cards Vault

## Objective

Add a **Cards** vault next to budget notes so the user can store payment and ID card data locally, attach front/back photos, enter fields manually, and **copy fields (including CVV)** when paying online without finding the physical card.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3 (existing)
- Room DB `cards.db` (separate from budget notes for backup exclusion)
- App-private encrypted images under `files/cards/{cardId}/`
- In-app camera + Photo Picker (copy into app storage only)
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
util/          PaymentCardFormat, CardExpiry
ui/cards/      home, editor, components
ui/root/       RootScaffold with Notes | Cards tabs
```

## Code Style

Match existing Room entities, ViewModel factories, Compose Material 3 patterns (Scaffold, TopAppBar, FAB, LazyVerticalGrid).

## Testing Strategy

- Unit: `PaymentCardFormat`, `CardExpiry`
- Existing money/totals tests unchanged
- Manual: create payment/ID, photos, copy, uninstall wipe

## Boundaries

- **Always:** local-only; mask PAN/CVV by default; exclude `cards.db` and `files/cards` from backup
- **Ask first:** app rename for wallet; network; monetization links
- **Never:** upload card data; log CVV/PAN; store secrets in git; OCR / ML Kit

## Success Criteria

- Bottom tabs Notes | Cards; notes unchanged
- Payment + ID cards with front/back images
- Manual entry for payment fields; ID supports custom fields
- Copy-to-clipboard for online pay
- Data wiped on uninstall; card backup excluded

## Locked decisions

- Layout: bottom tabs (Option 1)
- Photos: keep front and back (no OCR — user enters fields)
- CVV: stored for personal online copy/paste
- Encryption: SQLCipher + encrypted images + biometric/PIN lock
