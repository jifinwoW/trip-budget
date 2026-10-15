# TripBudget — App Overview

Offline personal Android app for tracking trip budgets, member contributions, and expenses. One phone, one local database, no accounts or cloud.

---

## What it does

1. Create a trip and set a planned budget (INR).
2. Add people and how much each should contribute.
3. Record partial payments and see outstanding or excess per person.
4. Log expenses from the **shared fund** or **personal money**.
5. See collected cash, spending, shared cash left, and budget remaining on one dashboard.

It is intentionally a trip cashbox — not a full accounting or settlement product.

---

## Navigation & shell

| Area | Behavior |
| --- | --- |
| **Top trip selector** | Switch active trip; create / edit / delete from the app bar |
| **Bottom nav** | Dashboard · Expenses · Members · Settings |
| **Trip scope** | All trip data is filtered by the selected trip |
| **First launch** | Empty state → create your first trip |

Settings is global (theme). Everything else is trip-scoped.

---

## Screens & functionality

### Dashboard

Primary overview for the active trip.

**Shows**

- Trip name, destination, planned budget
- Progress: budget used %, contributions collected %
- Overspend banner (coral) when over plan
- **Shared cash** hero (contributions collected − shared-fund expenses)
- Chips: Collected · Shared spent
- Metric tiles: Budget left · Collected · Spent · Outstanding
- Recent expenses with category / shared-vs-personal chips
- Actions: **Contribute**, **Expense**

**Outstanding (dashboard)** = sum of **positive** member outstandings only (excess payments are not subtracted here).

### Members

Manage the crew and their money in.

**Per member**

- Name
- Expected contribution
- Paid so far
- Outstanding or excess

**Actions**

- Add / edit / delete member
- Record a payment (partial OK)
- Expand payment history → edit / delete individual contributions

Deleting a member removes their contribution history (cascade).

### Expenses

Chronological list of trip costs, grouped by date.

**UX**

- Search by title / category / notes
- Filter chips: All · Shared · Personal
- Daily section headers with day totals

**Fields**

- Title, amount, category, date
- Payment source: **Shared fund** or **Personal**
- Who paid — required only for **personal** expenses (not for shared fund)
- Optional notes

**Categories:** Food, Fuel, Transport, Accommodation, Activities, Other

**Actions:** Add / edit / delete (with confirm); editors open as bottom sheets

### Settings

- Theme: System / Light / Dark
- About: app name, offline note, version, currency (INR)

### Trips (via app bar)

- Create trip: name, optional destination, planned budget
- Edit trip metadata
- Delete trip (and all related members, contributions, expenses)

---

## Financial rules

| Metric | Formula |
| --- | --- |
| Member outstanding | expected − paid (if negative → show as excess) |
| Contributions collected | sum of recorded payments |
| Total expenses | all expenses (shared + personal) |
| Shared cash remaining | collected − shared-fund expenses |
| Budget remaining | planned budget − total expenses (can be negative) |
| Outstanding (dashboard) | sum of **positive** member outstandings only |

**Important behaviors**

- Personal expenses **do not** reduce shared cash.
- Personal expenses **do** count toward total trip spending / budget used.
- Personal expenses are **not** auto-counted as contributions.
- No auto reimbursement pairs (no second expense created for you).
- Planned budget may differ from the sum of expected member contributions.
- Money is stored as integer **paise** (`Long`) — never Float/Double. Display currency: **INR**.

---

## Design system

### Direction: Coastal Soft (travel finance)

Light-first, soft product UI inspired by modern travel / finance dashboards:

- Misty aqua canvas with soft color orbs
- Large rounded cards (~18–28dp)
- Soft shadows for depth
- Tinted metric tiles with icons
- Coral reserved for warnings / overspend
- Teal as the primary action / brand accent

Avoids heavy “ledger/cashbox” look and generic purple-gradient AI aesthetics.

### Palette (light)

| Token | Hex | Use |
| --- | --- | --- |
| Aqua mist | `#EAF6F4` | Page atmosphere |
| Soft canvas | `#F4F7F8` | Dashboard canvas |
| Teal 600 | `#0D9B8A` | Primary / CTAs |
| Teal 700 | `#0B7A6E` | Strong labels / icons |
| Pale blue | `#D7EAF7` | Budget-left tile |
| Sand | `#F3E8D4` | Collected tile |
| Soft rose | `#F6DCE0` | Spent tile |
| Peach | `#F8E0D0` | Outstanding tile |
| Mint hero | `#C8EDE4` | Soft mint accents |
| Coral 600 | `#E85D4C` | Overspend / negative |
| Charcoal 900 | `#1A2B28` | Primary text |
| Paper white | `#FFFFFF` | Cards / surfaces |

Dark theme exists (night spruce/teal) via Settings; light coastal is the hero look.

### Typography

- **Display:** serif for trip titles and large money figures
- **Body / chrome:** clean sans-serif for labels, buttons, lists
- Soft letter-spacing on labels (friendly, not stamped ledger)

### Shape

| Size | Radius | Typical use |
| --- | --- | --- |
| Small | 12dp | Chips, small controls |
| Medium | 16–18dp | Buttons, list cards |
| Large | 20–24dp | Metric tiles |
| Extra | 24–28dp | Hero card |

### Motion

- `EnterCascade` — staggered fade + light slide-in on dashboard sections
- Soft timings (~360–400ms) so the screen feels alive without noise

### Dashboard visual layout (current)

```
┌─────────────────────────────────────┐
│ 📍 Destination                      │
│ Trip name                           │
│ Planned ₹…                          │
├─────────────────────────────────────┤
│ Progress card (budget / collected)  │
├─────────────────────────────────────┤
│ 🌊 Shared cash HERO (bg + icon)     │
├──────────────────┬──────────────────┤
│ Collected chip   │ Shared spent     │
├──────────────────┼──────────────────┤
│ Budget left tile │ Collected tile   │  ← tinted + bg image + icon
│ Spent tile       │ Outstanding tile │
├─────────────────────────────────────┤
│ [Contribute]        [Expense]       │
├─────────────────────────────────────┤
│ Recent expenses (icon + chips)      │
└─────────────────────────────────────┘
```

### Design assets

Located under `app/src/main/res/drawable/`:

| Asset | Role |
| --- | --- |
| `bg_hero_ocean.jpg` | Shared-cash hero background |
| `bg_tile_sky.jpg` | Budget-left tile wash |
| `bg_tile_sand.jpg` | Collected tile wash |
| `bg_tile_rose.jpg` | Spent tile wash |
| `bg_tile_mint.jpg` | Outstanding tile wash |
| `bg_orb_teal.xml` / `bg_orb_sand.xml` | Soft radial orb shapes |

Icons use Material Icons (Outlined) — wallet, savings, payments, receipt, schedule, place, etc.

### Other screens (visual)

- **Members / Expenses:** soft white cards on coastal mist, rounded FABs
- **Settings:** white preference card + about card
- **Dialogs:** large rounded surfaces, teal primary actions
- **Empty / loading:** coastal mist + branded empty copy

---

## Data model (Room)

| Entity | Key fields |
| --- | --- |
| **Trip** | name, destination?, plannedBudgetPaise, createdAt |
| **Member** | tripId (CASCADE), name, expectedContributionPaise |
| **Contribution** | tripId + memberId (CASCADE), amountPaise, paidAt, note? |
| **Expense** | tripId, paidByMemberId? (nullable for shared), title, amountPaise, category, date, paymentSource, notes? |

Schema migrations are real Room migrations (no destructive wipe on version bump). Example: v1 → v2 made `paidByMemberId` nullable for shared-fund expenses.

---

## Technology

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DB | Room (SQLite) |
| State | ViewModel + StateFlow + Coroutines |
| Nav | Navigation Compose |
| DI | Manual `AppContainer` (no Hilt) |
| Tests | JUnit for money / trip totals |
| Min SDK | 26 · Target / Compile 35 |

No backend, login, analytics, or network dependency for core use.

### Package layout

```
app/src/main/java/com/tripbudget/app/
  data/       entities, DAOs, database, migrations, repository, theme prefs
  domain/     Money, PaymentSource, ExpenseCategory, TripTotals
  ui/         theme, navigation, screens, ViewModels, components
  di/         AppContainer
```

---

## Out of scope (by design)

Accounts / permissions, settlement optimization, receipt photos, cloud backup, push notifications, multi-currency, reimbursement workflow UI, Hilt.

---

## Related docs

- [`plan.md`](plan.md) — greenfield product / engineering plan
- [`../README.md`](../README.md) — setup, build, and test commands
