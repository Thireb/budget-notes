# Budget Notes App — Build Plan

## 1. Concept Summary

A private, local-only Android note-taking app focused on **quick budget/tally calculations**, styled after [EasyNotes](https://f-droid.org/packages/com.kin.easynotes/) (minimalist Material Design, card-grid home screen, Jetpack Compose).

Instead of freeform text notes, every note is a **Budget Note**:
- Home screen: grid of note "cards" (like Google Keep) — title, running total, and a preview of a few items.
- Tapping a card expands it to a full-screen editor.
- Full-screen editor layout, top to bottom:
  1. **Header** — editable note title.
  2. **Total** — large, prominent, auto-updating number: `sum(Add items) - sum(Deduct items)`. Can go negative.
  3. **Grey divider line** ("–––––––").
  4. **Add section** — list of items you're adding money for.
  5. **Grey divider line** ("–––––––").
  6. **Deduct section** — list of items you're subtracting money for.
- Each line item: `checkbox` + `amount : description`.
  - Checking an item strikes it through (visual only — see Open Questions on whether it affects the total).
  - Unchecking restores it.

No cloud sync, no accounts, no permissions beyond local storage. Sync is explicitly out of scope for v1 (future work).

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard for modern native Android |
| UI | Jetpack Compose | Matches EasyNotes' stack, modern declarative UI, easy Material 3 theming |
| Architecture | MVVM (ViewModel + UiState) | Standard, testable, plays well with Compose |
| Local persistence | **Room** (SQLite) | Structured relational data (notes → items) with live Flow queries; better than raw JSON/key-value for a list-of-items-per-note model that will grow (search, sort, future sync) |
| DI | Hilt (optional, recommended) | Keeps ViewModel/Repository wiring clean; can skip for a very small v1 if you want to move fast |
| Navigation | Navigation Compose | Home grid ↔ Note detail screen |
| Reactive data | Kotlin Flow / StateFlow | Room DAOs return Flow; totals recompute reactively as items change |
| Theming | Material 3, dynamic color + manual dark/light | Matches EasyNotes' minimalist Material feel |
| Min SDK | API 26 (Android 8.0) | Matches EasyNotes' stated minimum |

---

## 3. Data Model

### `BudgetNote` (Room Entity)
| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK, autogenerate) | |
| `title` | String | Editable header |
| `createdAt` | Long (epoch millis) | For sort order on home screen |
| `updatedAt` | Long (epoch millis) | Updated on any edit |
| `colorTag` | Int? (optional) | If you want Keep-style card colors later |

### `BudgetItem` (Room Entity)
| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK, autogenerate) | |
| `noteId` | Long (FK → BudgetNote.id) | |
| `amount` | Double or Long (minor units, e.g. cents) | **Recommend storing as integer minor units** (e.g. paisa/cents) to avoid float rounding errors in a money app |
| `description` | String | |
| `type` | Enum: `ADD` / `DEDUCT` | Which section it belongs to |
| `isChecked` | Boolean | Struck-through state |
| `position` | Int | Manual ordering within its section (for drag-to-reorder later) |
| `createdAt` | Long | |

### Derived (not stored, computed in ViewModel)
- `addTotal = sum(amount where type == ADD)`
- `deductTotal = sum(amount where type == DEDUCT)`
- `total = addTotal - deductTotal`

---

## 4. Screens

### 4.1 Home Screen (Note Grid)
- `LazyVerticalGrid` (2 columns, Keep-style staggered/adaptive card layout).
- Each card shows:
  - Note title (bold, top)
  - Total (prominent, colored green if positive / red if negative / grey if zero)
  - Small preview of first 2–3 items (amount : description, dimmed if checked)
- Floating Action Button (`+`) → creates a new blank Budget Note and opens it directly in the editor.
- Long-press on a card → selection mode → delete / (future: pin, color, duplicate).
- Empty state: friendly illustration/text "No budget notes yet — tap + to start one."
- Search bar (optional v1, easy add later) filtering by title.

### 4.2 Note Editor (Expanded Full Screen)
Top to bottom:
1. **Top app bar**: back button, editable title as a `TextField` styled like a header (large font, no visible border until focused), overflow menu (delete note, share/export as text — optional).
2. **Total display**: large centered/left-aligned number, live-updating, formatted with the device's currency/locale or a plain number with a currency prefix set in Settings.
3. **Grey horizontal divider**.
4. **Add section**:
   - Section label (small, muted, e.g. "ADD" or a `+` icon) — optional, can be implicit.
   - List of `BudgetItem` rows (type = ADD).
   - Each row: `Checkbox` — `amount` — `:` — `description` — (swipe-to-delete or trailing delete icon).
   - "Add item" row/button at the bottom of the section — tapping opens an inline input (amount field + description field + confirm) or a small bottom sheet.
5. **Grey horizontal divider**.
6. **Deduct section**: same structure as Add, but type = DEDUCT.
7. Keyboard-aware layout so adding an item doesn't jump the whole screen awkwardly (`imePadding()`).

### 4.3 Add/Edit Item Input
- Minimal inline row: numeric amount field (decimal keyboard) + description text field + confirm (checkmark) + cancel.
- Validation: amount must be a valid positive number (sign is implied by section, not typed by the user — see Open Questions).
- Tapping an existing item's text (not the checkbox) opens it for editing in the same inline style.

---

## 5. Calculation Logic
- `Total = Σ(Add items' amounts) − Σ(Deduct items' amounts)`.
- Recomputed reactively via Flow whenever an item is added, edited, deleted, or (pending your answer below) checked/unchecked.
- Total can be negative — render in a distinct color (e.g. red) when negative, matching common budgeting-app conventions.
- Store amounts as integer minor units internally; format for display using `NumberFormat` / locale-aware currency formatting at the UI layer only.

---

## 6. Interaction Details
- **Strike-through**: `TextDecoration.LineThrough` + reduced alpha on the row when `isChecked == true`.
- **Swipe-to-delete**: `SwipeToDismiss` (Compose Material) on each item row, with an undo snackbar.
- **Reordering**: not in v1 scope unless you want it — items sort by `createdAt` or `position` ascending.
- **Autosave**: every edit (title, item amount/description, check state) writes immediately to Room — no explicit "Save" button, matching the Keep/EasyNotes autosave feel.
- **Dark mode**: full support, following system theme by default, matching EasyNotes.

---

## 7. Project Structure (suggested)

```
app/
 └─ src/main/java/com/<you>/budgetnotes/
     ├─ data/
     │   ├─ BudgetNote.kt          (entity)
     │   ├─ BudgetItem.kt          (entity)
     │   ├─ BudgetItemType.kt      (enum: ADD, DEDUCT)
     │   ├─ BudgetNoteDao.kt
     │   ├─ BudgetItemDao.kt
     │   └─ AppDatabase.kt
     ├─ repository/
     │   └─ BudgetNoteRepository.kt
     ├─ ui/
     │   ├─ home/
     │   │   ├─ HomeScreen.kt
     │   │   ├─ HomeViewModel.kt
     │   │   └─ NoteCard.kt
     │   ├─ editor/
     │   │   ├─ NoteEditorScreen.kt
     │   │   ├─ NoteEditorViewModel.kt
     │   │   ├─ BudgetItemRow.kt
     │   │   └─ AddItemInput.kt
     │   ├─ theme/
     │   │   ├─ Color.kt
     │   │   ├─ Theme.kt
     │   │   └─ Type.kt
     │   └─ components/
     │       └─ SectionDivider.kt
     ├─ navigation/
     │   └─ NavGraph.kt
     └─ MainActivity.kt
```

---

## 8. Suggested Build Order (milestones for Cursor)

1. **Scaffold**: new Compose project, min SDK 26, Material 3 theme, empty home + editor screens wired via Navigation Compose.
2. **Data layer**: Room entities, DAOs, database, repository. Verify with simple unit tests/logs.
3. **Home screen**: grid of hardcoded note cards → wire to real Room data via ViewModel/Flow.
4. **Note editor — read/display**: title, total, add/deduct sections rendering existing items (no editing yet).
5. **Note editor — add item**: inline add-item input for both sections, writes to Room, total recomputes live.
6. **Checkbox + strike-through**: toggle `isChecked`, visual strike, decide/implement whether it affects total (see below).
7. **Edit/delete item**: tap to edit inline, swipe-to-delete with undo.
8. **New note creation + delete note**: FAB creates note, long-press-to-delete on home grid.
9. **Polish**: dark mode, empty states, currency formatting, animations (Compose's default AnimatedVisibility for section changes).
10. **(Future, not v1)**: cloud sync, backup/export, note colors, search, widgets.

---

## 9. Locked decisions (v1)

1. **Checked items**: visual strike-through only — amounts **still count** toward the total.
2. **Sign entry**: positive-only input; Add vs Deduct section determines the sign.
3. **Currency**: locale `NumberFormat` display only — no Settings screen / currency picker in v1.
4. **Decimal precision**: 2 decimal places; stored as integer minor units (`Long`).
5. **Note deletion**: immediate hard delete (home long-press confirm + editor overflow).
6. **Item reordering**: not in v1; order by `position` / creation order.


---

**Reference for visual style**: [EasyNotes on F-Droid](https://f-droid.org/packages/com.kin.easynotes/) — minimalist Material design, card grid, zero permissions, full dark mode support.
