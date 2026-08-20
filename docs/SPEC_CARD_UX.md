# Spec: Card expiry warnings & one-tap checkout copy

## Objective

Help users notice cards that are expiring or expired, and speed up online checkout by copying payment fields in order with one control.

## Assumptions (confirm or correct)

1. **No auto-lock timeout** — out of scope; leave session lifetime as today.
2. **Warning window** — surface a warning when expiry is within **30 days**, and a stronger **Expired** state after the end of the expiry month/day.
3. **Payment expiry** — use `expiryMonth` + `expiryYear` (end of that calendar month).
4. **ID expiry** — parse `expiryDate` best-effort (`YYYY-MM`, `YYYY-MM-DD`, `MM/YYYY`, `MM/YY`). Unparseable dates show **no** warning (avoid false alarms).
5. **Where warnings appear** — Cards home tile subtitle/chip; optional banner at top of card editor.
6. **Sort** — expired / expiring soon cards sort **above** healthy ones on Cards home (stable within each bucket by `updatedAt` desc).
7. **One-tap copy (payment only)** — button on payment card editor: copy **PAN → wait → MM/YY → wait → CVV**, with short snackbar prompts (“Copied number — paste now”, then expiry, then CVV). ID cards keep per-field copy only.
8. **Timing between checkout steps** — **8 seconds** between automatic clipboard advances (user can paste); each step still uses the existing ~45s clear timer reset on each copy.
9. **Missing fields** — skip blank steps; if nothing to copy, snackbar “Nothing to copy”.
10. **Reveal secrets** — one-tap does **not** require reveal; it copies real values regardless of mask UI.
11. **Warning chrome** — small status chip **plus** colored tile border (amber = expiring soon, red = expired). Decided.

## Decisions (locked)

- Copy for checkout: **auto-advance** (8s between steps)
- Tile look: **chip + border** (option B)
- No auto-lock timeout

## Tech stack

Existing: Kotlin, Compose, Room, `SecureClipboard`. No new dependencies.

## Commands

```bash
./gradlew test assembleDebug
```

## Project structure

```
docs/SPEC_CARD_UX.md          → this spec
app/.../util/CardExpiry.kt    → parse + status (ExpiringSoon / Expired / Ok / Unknown)
app/.../ui/cards/*            → tile chip, editor banner, checkout copy control
app/src/test/.../CardExpiryTest.kt
```

## Code style

- Pure functions for expiry parsing/status (unit-tested).
- Match existing Material 3 snackbars / TextButton patterns in `CardEditorScreen`.
- No logging of PAN/CVV.

## Testing strategy

- Unit tests for `CardExpiry` parsers and day boundaries (soon / expired / ok / unparseable).
- Manual: tile shows chip; one-tap advances clipboard with snackbars.

## Boundaries

- **Always:** never log secrets; reuse `SecureClipboard`; payment one-tap only.
- **Ask first:** changing the 30-day window or inter-step delay; adding ID one-tap sequences.
- **Never:** network sync; storing clipboard history.

## Success criteria

- [x] Payment and ID cards with parseable expiry show **Expiring soon** (≤30 days) or **Expired** on the Cards home tile.
- [x] Cards home lists expired/expiring before others.
- [x] Payment editor has **Copy for checkout** that sequences number → MM/YY → CVV with snackbars.
- [x] Blank fields skipped; all-blank → “Nothing to copy”.
- [x] `./gradlew test` covers expiry parsing edge cases.
- [x] Tile uses **chip + colored border** (amber / red).

## Open questions

_(Resolved — see Decisions.)_
