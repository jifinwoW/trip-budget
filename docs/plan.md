# TripBudget — Greenfield Personal Android App

Offline-only personal trip budget tracker for a single Android phone.

## Goal

1. Create a trip and set a budget.
2. Add people and define how much each person should contribute.
3. Record how much each person has paid and how much remains.
4. Record trip expenses.
5. See how much money has been collected, spent, and remains.

Do not build an accounting platform. Keep the implementation simple and maintainable.

## Technology

- Kotlin
- Jetpack Compose + Material 3
- Room (SQLite)
- ViewModel + StateFlow + Coroutines
- Navigation Compose
- Manual DI via `AppContainer` (no Hilt)
- JUnit for core calculation tests

No backend, login, analytics, or internet dependency.

## Navigation

- **Top trip selector:** switch the active trip; create / edit / delete trip from the app bar.
- **Bottom nav (trip-scoped except Settings):** Dashboard · Expenses · Members · Settings
- All trip data is filtered by the selected trip. There is no global cross-trip expense list.
- First launch: empty state → create trip.

## Screens

### Dashboard

Planned budget, contributions collected, total expenses, shared cash remaining, outstanding contributions, budget remaining / overspend warning, recent expenses. Actions: Add Contribution, Add Expense.

### Members

Per member: name, expected, paid, outstanding or excess. Add/edit members, record partial payments, view/edit/delete payment history.

### Expenses

Title, amount, category, date, who paid, payment source (shared fund | personal money), optional notes. Chronological list with edit/delete confirmation.

Categories: Food, Fuel, Transport, Accommodation, Activities, Other.

### Settings

Theme (system / light / dark), app information.

## Financial rules

1. Planned budget is the expected trip cost/collection target (may differ from sum of expected contributions).
2. Contributions collected = sum of recorded payments.
3. Total expenses include shared and personal expenses.
4. Shared cash remaining = contributions collected − shared-fund expenses.
5. Budget remaining = planned budget − total expenses (allow negative + warning).
6. Outstanding contributions (dashboard) = sum of **positive** member outstandings only.
7. Personal expenses do not reduce shared cash.
8. Personal expenses still count toward total trip spending.
9. Personal expenses are not auto-counted as contributions.
10. Do not auto-create reimbursement pairs (no second expense).
11. Store money as `Long` paise; never Float/Double. Currency: INR.

## Data model (Room)

- `Trip` — name, destination?, plannedBudgetPaise, createdAt
- `Member` — tripId FK CASCADE, name, expectedContributionPaise
- `Contribution` — tripId + memberId FK CASCADE, amountPaise, paidAt, note?
- `Expense` — tripId + paidByMemberId FK CASCADE, title, amountPaise, category, date, paymentSource, notes?

## Out of scope

Accounts/permissions, settlement optimization, receipt images, cloud backup, notifications, multi-currency, reimbursement workflow UI, Hilt.
