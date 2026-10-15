# TripBudget Android App — Complete Coastal Soft UI Redesign

## ROLE

Act as a senior Android UI/UX designer and Kotlin Jetpack Compose engineer with strong expertise in pixel-accurate interface implementation, design systems, Material 3, accessibility, animation, and production-quality Android applications.

Your task is to redesign my existing Android application, **TripBudget**, to closely reproduce the attached Coastal Soft UI reference image.

This is not a request to create a new application, build a prototype, or merely suggest design improvements. You must inspect the existing codebase and implement the redesigned UI in the actual project.

The attached reference image is the primary visual reference. Reproduce its visual language, proportions, spacing, color relationships, typography, card treatments, iconography, and overall composition as closely as possible, adapting them appropriately to Android screen sizes.

**The objective is a faithful, polished implementation of the reference — not a loosely inspired alternative.**

Do not stop after redesigning the dashboard. The entire application must have a coherent visual identity, including every screen, form, dialog, navigation element, empty state, and interactive component.

---

# 1. NON-NEGOTIABLE CONSTRAINTS

Before changing anything, understand these requirements.

### Preserve the existing application

- Keep the existing application name: TripBudget.
- Keep Kotlin and Jetpack Compose.
- Keep Material 3.
- Keep Room and the existing SQLite database.
- Keep ViewModel, StateFlow, and Coroutines.
- Keep Navigation Compose.
- Keep the existing manual AppContainer dependency injection.
- Keep the minimum SDK at 26 and the existing target/compile SDK configuration unless a change is genuinely necessary.
- Keep all existing repositories, DAOs, entities, migrations, domain models, and financial calculations.
- Keep the current offline-first architecture.
- Do not introduce a backend, account system, authentication, cloud database, analytics, or network dependency.
- Do not introduce Hilt or another dependency injection framework.
- Do not replace working architecture merely to make the UI easier to implement.

### This is a UI redesign, not a feature redesign

Do not add, remove, or alter business functionality.

Specifically, do not introduce:

- AI travel planning.
- Weather integration.
- Booking management.
- Receipt photography.
- Cloud backup.
- Multi-currency support.
- Expense settlement optimization.
- Reimbursement workflows.
- Additional financial calculations.
- New database entities or schema migrations just for the redesign.

You may improve the visual presentation and interaction feedback of existing features, but their underlying behavior must remain unchanged.

**If a visual improvement conflicts with an existing business rule, preserve the business rule.**

---

# 2. FIRST: AUDIT THE EXISTING PROJECT

Before editing files, inspect the actual repository.

Identify:

1. All Compose screens and their current layouts.
2. Navigation destinations and bottom navigation implementation.
3. Existing reusable components.
4. Theme definitions, colors, typography, shapes, and Material 3 configuration.
5. Existing drawable assets, including background images and vector drawables.
6. ViewModels, UI state models, repositories, and event handlers.
7. Room entities, DAO queries, database version, and migration history.
8. Existing dialogs and bottom sheets.
9. Theme preference persistence.
10. Existing tests and build configuration.

Use the existing code as the source of truth for functionality.

Determine which design components can be reused and which need to be refactored.

Do not delete working functionality or replace existing implementations with hardcoded mock screens.

After the audit, proceed with the implementation. Do not stop at an audit report or ask me to approve every small design decision.

---

# 3. VISUAL DIRECTION: COASTAL SOFT

The entire application must follow the Coastal Soft design system shown in the attached reference.

The aesthetic combines:

- Coastal travel imagery.
- Misty aqua backgrounds.
- Soft white surfaces.
- Teal accents.
- Pastel financial metric cards.
- Elegant serif display typography.
- Clean sans-serif body typography.
- Rounded corners.
- Restrained shadows.
- Subtle visual depth.
- Carefully controlled motion.

The application should feel like a premium personal travel companion, not a generic accounting application.

Avoid the following:

- Generic purple-gradient AI aesthetics.
- Heavy spreadsheet or ledger styling.
- Excessive borders.
- Excessive gradients.
- Strong drop shadows.
- Neon colors.
- Excessive glassmorphism.
- Inconsistent corner radii.
- Oversized icons.
- Random decorative illustrations.
- Excessive animations.
- Background imagery on every component.
- Different visual styles between screens.

The design should remain calm, refined, readable, and practical.

---

# 4. EXACT LIGHT-THEME DESIGN TOKENS

Use the following palette as the foundation. Define reusable Compose color tokens rather than scattering hex values throughout the application.

| Token | Hex | Intended usage |
|---|---|---|
| Aqua mist | `#EAF6F4` | Atmospheric background |
| Soft canvas | `#F4F7F8` | Main screen background |
| Teal 600 | `#0D9B8A` | Primary actions and progress |
| Teal 700 | `#0B7A6E` | Strong teal text and icons |
| Pale blue | `#D7EAF7` | Budget-left metric tile |
| Sand | `#F3E8D4` | Contributions-collected tile |
| Soft rose | `#F6DCE0` | Total-spent tile |
| Peach | `#F8E0D0` | Outstanding tile |
| Mint hero | `#C8EDE4` | Shared-cash hero accents |
| Coral 600 | `#E85D4C` | Overspending and negative states |
| Charcoal 900 | `#1A2B28` | Primary text |
| Paper white | `#FFFFFF` | Cards, dialogs, and surfaces |

Use appropriate, harmonious secondary text colors, dividers, disabled states, and icon tints.

The reference image takes precedence where a particular surface requires a slight tonal adjustment. Maintain the overall palette and contrast relationships.

### Background atmosphere

Use the aqua mist and soft canvas colors to create a subtle coastal atmosphere.

If the existing project supports the necessary drawable assets, use the existing soft radial orbs:

- `bg_orb_teal.xml`
- `bg_orb_sand.xml`

Place them subtly behind the screen content. They must never interfere with readability.

Decorative elements must remain behind the content layer and must not intercept touches.

### Card surfaces

- Primary cards should generally use white surfaces.
- Use soft shadows and tonal separation rather than heavy borders.
- Keep card spacing consistent.
- Use the same surface treatment for equivalent components across screens.
- Reserve stronger color for the shared-cash hero, metric tiles, primary actions, and important statuses.

---

# 5. TYPOGRAPHY SYSTEM

Typography is a critical part of the reference and must not be treated as an afterthought.

### Display typography

Use an elegant, readable serif typeface for:

- Trip names.
- Large money figures.
- Important dashboard headings.
- Major section titles where appropriate.

Prefer a refined editorial serif similar in character to Lora or DM Serif Display, provided the chosen font renders correctly on Android.

Use bundled font resources where appropriate so the appearance is consistent across devices.

### Body typography

Use a clean sans-serif typeface for:

- Body text.
- Labels.
- Buttons.
- Input fields.
- Navigation labels.
- Supporting descriptions.
- Dates.
- Category names.
- Payment statuses.

Prefer a typeface such as Inter or an equivalent clean sans-serif.

### Hierarchy

Establish consistent styles for:

- Display / large balance.
- Screen title.
- Section heading.
- Card title.
- Primary value.
- Body text.
- Secondary text.
- Compact metadata.
- Small uppercase labels.

Use restrained letter spacing for small labels such as SHARED CASH, COLLECTED, and BUDGET USED.

Do not use uppercase text everywhere.

Avoid inconsistent font weights, arbitrary text sizes, and excessive use of bold.

Large financial values should be prominent without forcing users to scroll horizontally or clipping on smaller screens.

Use appropriate font scaling and accessibility-aware sizing.

---

# 6. SHAPES, SPACING, AND DEPTH

Establish reusable shape tokens.

| Component | Target corner radius |
|---|---:|
| Small chips | 12dp |
| Small controls | 12dp |
| Buttons | 16–18dp |
| Standard cards | 16–18dp |
| Metric tiles | 20–24dp |
| Large hero card | 24–28dp |
| Dialogs and sheets | 24–28dp |

These values are design targets. Use consistent shapes and adapt them when necessary to preserve the reference's proportions.

### Spacing

Use a consistent spacing system based on 4dp increments.

Suggested tokens:

- 4dp: tight internal spacing.
- 8dp: icon-to-label spacing.
- 12dp: compact component spacing.
- 16dp: standard padding and screen gutters.
- 20dp: separation between related sections.
- 24dp: separation between major sections.
- 32dp: occasional spacious section breaks.

Avoid arbitrary spacing values unless a specific layout requires them.

Cards should feel comfortably padded, never cramped.

### Shadows

Use subtle elevation and soft shadows.

Do not apply a heavy shadow to every card. The background, tonal surfaces, and rounded shapes should provide most of the visual depth.

Keep elevation consistent for comparable components.

---

# 7. DASHBOARD — PRIMARY REDESIGN

The dashboard is the most important screen. Implement it to closely match the attached Coastal Soft reference.

Preserve all existing data and functionality.

## 7.1 Trip header

At the top of the dashboard, show:

- Destination, when available.
- Trip name.
- Planned budget.
- Existing trip selector and trip management actions.

The header should feel editorial and travel-oriented.

Use a clean hierarchy, with the trip name as the main visual anchor.

Where destination imagery is appropriate, use a scenic landscape photograph or an existing local drawable asset.

Do not use remote images. The app must remain completely usable offline.

The header must adapt gracefully when a trip has no destination.

The trip selector must remain functional. Do not replace it with a decorative control.

## 7.2 Progress card

Create a rounded white card with two separate progress indicators:

1. Budget used percentage.
2. Contributions collected percentage.

Each indicator must include a clear label, a percentage, and a horizontal progress bar.

Use teal for progress and a soft neutral track.

Progress indicators must be derived from the existing calculated values.

Preserve the existing rules for zero budgets, empty contributions, and percentages outside the normal range.

Do not divide by zero or introduce misleading progress values.

## 7.3 Shared-cash hero

This is the defining visual component of TripBudget.

Make it the most visually prominent card on the dashboard.

Design requirements:

- Large rounded corners.
- Soft mint or coastal teal appearance.
- Optional ocean or coastal background image.
- Clear SHARED CASH label.
- Large serif balance.
- Small supporting description.
- Supporting COLLECTED and SHARED SPENT values.
- Restrained wallet or savings icon.
- Sufficient contrast between text and background.

If using a background photograph, apply a suitable overlay or tint to preserve text readability.

The background must not overpower the amount.

Use the existing local asset `bg_hero_ocean.jpg` when it fits the reference. Inspect the asset first rather than assuming its contents.

If it does not fit, improve or replace it with a locally bundled alternative.

### Financial rule

Shared cash remaining must always equal:

`Contributions collected - Shared-fund expenses`

Personal expenses must never reduce shared cash.

A negative shared balance must remain visible and understandable. Do not silently clamp the financial value to zero.

## 7.4 Supporting chips

Place two compact supporting indicators beneath or within the hero card:

- Collected.
- Shared spent.

Use distinct but harmonious tinted surfaces.

Keep the amounts easy to scan.

Avoid making these chips compete visually with the main shared-cash balance.

## 7.5 Four metric tiles

Use a balanced two-column grid of rounded metric cards.

The four metrics are:

1. Budget left.
2. Collected.
3. Total spent.
4. Outstanding.

Use these color assignments:

- Budget left: pale blue.
- Collected: sand.
- Total spent: soft rose.
- Outstanding: peach.

Each tile should contain:

- A recognizable Material icon.
- A concise label.
- A prominent amount.
- Consistent internal padding.
- A restrained tinted background or wash.
- Clear text contrast.

Use existing local background assets where appropriate:

- `bg_tile_sky.jpg`
- `bg_tile_sand.jpg`
- `bg_tile_rose.jpg`
- `bg_tile_mint.jpg`

Inspect the actual assets and use them only where they improve the design.

If the images are too strong, apply subtle tonal overlays or use clean tinted surfaces instead.

Keep the icon size, alignment, label position, and amount hierarchy consistent across all four tiles.

Do not assign random colors to the tiles.

### Financial rules

Budget remaining:

`Planned budget - Total expenses`

Total expenses include shared and personal expenses.

Budget remaining may be negative.

Contributions collected equals the sum of recorded contribution payments.

Outstanding on the dashboard is the sum of positive member outstandings only. Excess payments must not be subtracted from the dashboard outstanding amount.

Never change these calculations during the redesign.

## 7.6 Primary actions

Display two prominent actions:

- Contribute.
- Expense.

Use teal as the primary action color, with a visually lighter secondary treatment for the other action where appropriate.

Both actions must remain functional and navigate to the existing forms or flows.

Use consistent button heights, corner radii, icon alignment, typography, and touch targets.

Avoid excessive gradients or decorative effects.

## 7.7 Recent expenses

Display the recent expense list below the primary actions.

Each row should include:

- Category icon.
- Expense title.
- Category label.
- Payment-source chip.
- Amount.
- Date or other existing useful metadata.

Shared and Personal expenses must be visually distinguishable without requiring users to read the entire row.

Use the established category palette and iconography consistently.

Use compact, well-aligned rows separated by whitespace or subtle dividers.

Provide a View All action if consistent with the existing navigation.

Do not change which expenses appear, how they are ordered, or how their amounts are calculated.

## 7.8 Overspending state

When the existing calculated budget remaining is negative, show a clear overspend banner or warning treatment.

Use Coral 600, `#E85D4C`, for the warning accent.

The warning should be noticeable without overwhelming the entire dashboard.

Ensure it does not create an excessive stack of alerts or push the main information unnecessarily far down the screen.

When the budget is not exceeded, do not display an overspending warning.

---

# 8. BOTTOM NAVIGATION AND APP SHELL

The existing bottom navigation must remain:

1. Dashboard.
2. Expenses.
3. Members.
4. Settings.

Redesign it to match the Coastal Soft aesthetic.

Requirements:

- Clean background.
- Consistent Material icons.
- Clear selected and unselected states.
- Teal emphasis for the active destination.
- Muted secondary colors for inactive destinations.
- Readable labels.
- Proper system-bar and navigation-bar insets.
- Consistent appearance across every screen.

Use a restrained active indicator or subtle tonal treatment.

Do not introduce extra navigation destinations.

The active destination must reflect the actual navigation state.

The top trip selector must continue to control the selected trip. All trip-scoped screens must continue to show data belonging to that trip.

Settings remains global.

Preserve all existing trip creation, editing, and deletion entry points.

---

# 9. EXPENSES SCREEN

Redesign the Expenses screen using the same visual language.

Do not change any expense functionality.

### Screen structure

Create a clear screen title, trip context, and existing expense-creation action.

If useful and compatible with the current implementation, include a compact search or filter area, but only if it does not introduce new product behavior.

Use the existing filtering and sorting capabilities where available. Do not invent additional filters that require new functionality.

### Expense rows

Each expense should be presented as a polished card or compact list row containing:

- Category icon.
- Expense title.
- Amount.
- Category.
- Date.
- Payment source.
- Optional notes when appropriate.

Use restrained category tints.

Use clear, consistent treatment for:

- Food.
- Fuel.
- Transport.
- Accommodation.
- Activities.
- Other.

Use the existing category values and enums.

Use separate visual chips for Shared fund and Personal.

Amounts should be visually prominent but should not dominate the entire row.

Ensure long expense titles, notes, and large amounts are handled gracefully.

### Expense interactions

Preserve:

- Add expense.
- Edit expense.
- Delete expense with confirmation.
- Existing chronological ordering.
- Existing trip scoping.

Use a clean floating action button or existing primary action placement.

Do not introduce receipt uploads or new expense fields.

---

# 10. MEMBERS SCREEN

The Members screen must make contribution status understandable at a glance.

### Screen summary

If the current screen already provides contribution totals, present them in a compact summary card using the Coastal Soft style.

Where existing data permits, show:

- Contributions collected.
- Expected contributions.
- Collection progress.

Do not invent new financial metrics or change existing calculations.

### Member cards

Each member card should display:

- Member name.
- Expected contribution.
- Amount paid.
- Outstanding amount or excess payment.
- Existing payment-history expansion control.

Use a restrained circular avatar with an initial derived from the member's name, unless the current implementation already provides another appropriate identity treatment.

Use consistent alignment and typography.

Where appropriate, display a thin contribution progress bar.

Handle the following states clearly:

- No payments.
- Partial payment.
- Fully paid.
- Outstanding contribution.
- Excess payment.

Use calm success styling and restrained coral or peach for outstanding amounts.

Excess payments must be distinguishable from outstanding amounts. Never show a negative outstanding value without clearly identifying it as an excess payment.

### Payment history

Keep payment history visually connected to the relevant member.

Expanded history should be easy to read and should not look like a separate, unrelated screen.

Preserve individual payment edit and delete actions.

Preserve all existing payment amounts, timestamps, notes, and contribution relationships.

### Member interactions

Preserve:

- Add member.
- Edit member.
- Delete member.
- Record partial contributions.
- Expand and collapse payment history.
- Edit individual contributions.
- Delete individual contributions.

Deleting a member must continue to follow the existing database cascade behavior.

Do not add member settlement calculations or reimbursement workflows.

---

# 11. SETTINGS SCREEN

Settings should be simpler than the financial screens.

Use a soft canvas background and well-spaced white preference cards.

### Appearance section

Present the existing theme preference:

- System.
- Light.
- Dark.

Use clear selected states, accessible controls, and consistent spacing.

The theme selection must continue to use the existing persistence mechanism.

Changing the theme must update the UI correctly across the application.

### About section

Preserve the existing information:

- App name.
- Version.
- Offline-first description.
- Currency: INR.

Use a restrained app identity treatment, such as a simple wallet or travel-inspired icon.

Do not introduce login, account information, cloud status, or settings for features that do not exist.

The screen should feel finished but intentionally minimal.

---

# 12. CREATE AND EDIT TRIP FORMS

The existing trip creation and editing flows must receive the same design treatment.

Use the existing fields:

- Trip name.
- Optional destination.
- Planned budget in INR.

### Form design

- Clear title and supporting description.
- Rounded input fields.
- Appropriate labels.
- Visible focus state.
- Consistent vertical spacing.
- Teal primary action.
- Neutral secondary or cancel action.
- Clear validation errors.
- Appropriate keyboard types.
- Correct IME actions.
- Proper keyboard and system-inset handling.

If the existing implementation uses dialogs, redesign those dialogs rather than changing the flow without a reason.

If a form is already a full screen, preserve that navigation behavior.

Do not add dates, bookings, trip participants, or additional metadata fields.

Editing must preserve all existing trip-related data.

Deleting a trip must continue to require confirmation and remove the trip's related data according to the existing implementation.

---

# 13. ADD AND EDIT EXPENSE FORMS

The expense form is a critical part of the application.

Redesign it carefully while preserving every existing field and validation rule.

### Fields

- Title.
- Amount.
- Category.
- Date.
- Payment source.
- Who paid, when required.
- Optional notes.

### Payment source selection

Provide a clear, attractive segmented control for:

- Shared fund.
- Personal.

The selection must use the existing payment-source domain model.

When Shared fund is selected, the payer field must follow the existing rule: no individual payer is required.

When Personal is selected, the existing required payer field must be shown and validated.

Do not create a separate expense or automatically create a reimbursement entry.

### Category selection

Use the existing categories:

- Food.
- Fuel.
- Transport.
- Accommodation.
- Activities.
- Other.

Present the category selector consistently with the design system.

### Amount entry

- Use an appropriate numeric keyboard.
- Display INR clearly.
- Avoid unnecessary decimal complexity.
- Preserve exact paise-based money handling.
- Do not use Float or Double for monetary values.
- Avoid floating-point conversion in formatting or calculations.

### Validation and interactions

Preserve all existing validation and save/edit behavior.

Make validation messages readable and specific.

Do not silently change a selected payment source, payer, date, amount, or category.

Do not discard form values unexpectedly.

---

# 14. RECORD CONTRIBUTION FORM

Redesign the contribution recording form or dialog.

Preserve the existing fields and behavior:

- Selected member.
- Payment amount.
- Payment date.
- Optional note.

Use a compact, focused layout.

Show relevant existing member information when appropriate, such as expected contribution and amount already paid.

Do not add an additional settlement or payment-allocation workflow.

Partial payments must continue to work.

The form must use the existing contribution persistence and validation logic.

---

# 15. DELETE CONFIRMATIONS AND OTHER DIALOGS

All dialogs must use the same visual language.

This includes:

- Delete trip.
- Delete member.
- Delete contribution.
- Delete expense.
- Any existing confirmation or selection dialog.

### Dialog styling

- Rounded corners.
- Comfortable padding.
- Clear title.
- Short explanation of the consequence.
- Primary and secondary actions.
- Consistent button placement.
- Appropriate touch targets.

Destructive actions must be visually distinguishable from ordinary actions without making every dialog aggressive.

Use Coral 600 or another restrained destructive treatment where appropriate.

Do not remove confirmation steps or change destructive-action behavior.

Ensure dialogs remain readable on smaller screens.

---

# 16. EMPTY STATES, LOADING STATES, AND EDGE CASES

Every screen must look intentional even when no data exists.

### First launch

The app should show a polished empty state prompting the user to create their first trip.

Use:

- A subtle travel-inspired icon or locally bundled illustration.
- A concise heading.
- Friendly supporting text.
- A clear Create Trip action.

Do not show a dashboard filled with fake financial data.

### Empty trip states

Handle:

- Trip with no members.
- Trip with no contributions.
- Trip with no expenses.
- Trip with no destination.
- Trip with a zero planned budget.
- Member with no payment history.

Use appropriate contextual empty states without adding new product features.

### Edge cases

Handle:

- Very long trip names.
- Long member names.
- Long expense titles.
- Large currency values.
- Zero-value amounts where allowed.
- Negative budget remaining.
- Negative shared cash.
- Excess payments.
- Partially completed forms.
- Keyboard-open layouts.
- Small phone screens.
- System font scaling.

Never allow text clipping, overlapping cards, horizontally overflowing layouts, or inaccessible actions.

---

# 17. DARK THEME

The application already supports System, Light, and Dark themes.

Preserve all three options.

The light Coastal Soft theme is the primary visual reference and must receive the most attention.

Create a matching dark theme using a night-spruce and teal palette.

Dark mode should feel like the same product at night, not a completely different design.

Requirements:

- Appropriate dark backgrounds and elevated surfaces.
- Readable text and supporting labels.
- Muted teal accents.
- Distinct category colors.
- Consistent status indicators.
- Proper dialog and input colors.
- Correct selected navigation states.
- Accessible contrast.
- No bright white cards that visually clash with the dark theme.

The pastel tile concept may be adapted into darker, subdued tinted surfaces.

Do not change the existing theme preference behavior.

---

# 18. ICONOGRAPHY AND ILLUSTRATIONS

Use Material Icons Outlined or the existing compatible Material icon set.

Choose semantically appropriate icons for:

- Wallet.
- Savings.
- Contributions.
- Receipts.
- Food.
- Fuel.
- Transport.
- Accommodation.
- Activities.
- Other expenses.
- Members.
- Schedule.
- Location.
- Settings.
- Editing.
- Deletion.

Use a consistent icon size, stroke appearance, and alignment.

Avoid mixing unrelated icon families.

Do not introduce emojis as substitutes for the application's primary icons.

Decorative artwork should be subtle and purposeful.

All required assets must be bundled locally.

Do not load images or fonts from the internet at runtime.

Inspect existing assets before replacing them. Preserve assets that already match the design.

If an asset is unsuitable, create or source an appropriate locally bundled alternative that can be used legally.

---

# 19. MOTION AND MICRO-INTERACTIONS

Use subtle motion to make the application feel polished.

Implement or refine the existing `EnterCascade` behavior for dashboard sections.

### Dashboard entrance

Use a staggered fade-in combined with a small vertical slide.

Target an overall duration of approximately 360–400ms, with restrained staggering between sections.

The animation should feel smooth rather than theatrical.

### Other interactions

Where appropriate, use subtle transitions for:

- Selected navigation states.
- Expanding member payment history.
- Dialog appearance.
- Progress indicators.
- Theme transitions.
- State changes after adding or editing an expense.

Do not animate every number or card unnecessarily.

Avoid excessive bounce, spring overshoot, and repeated attention-grabbing animations.

Respect reduced-motion preferences where practical.

Animations must not delay important actions or interfere with data loading, navigation, or accessibility.

Do not add animation dependencies without a real need.

---

# 20. RESPONSIVE LAYOUT AND ANDROID DETAILS

The application must be implemented for real Android devices, not just designed to resemble a static mockup.

Ensure:

- Correct status-bar insets.
- Correct navigation-bar insets.
- Proper edge-to-edge behavior consistent with the existing project.
- Scrollable content where required.
- No bottom navigation overlap.
- No content hidden behind system bars.
- No keyboard overlap on forms.
- No horizontal overflow.
- Appropriate touch targets, ideally around 48dp.
- Consistent spacing across screen sizes.
- Appropriate behavior with Android font scaling.
- Correct light and dark system-bar icon appearances.
- Stable layouts during configuration changes.
- Appropriate accessibility descriptions for meaningful icons.

Use `LazyColumn` or other appropriate lazy layouts for potentially long expense and member lists.

Use Compose components and layout primitives rather than hardcoding a screenshot-like arrangement that breaks on different devices.

The result must be a functional Android application.

---

# 21. DESIGN SYSTEM IMPLEMENTATION

Build a cohesive and reusable component system.

Refactor existing components where useful and create reusable components where they genuinely improve consistency.

Potential components include:

- CoastalScaffold.
- TripHeader.
- TripSelector.
- BudgetProgressCard.
- SharedCashHero.
- SupportingMetricChip.
- FinancialMetricTile.
- PrimaryActionButton.
- SecondaryActionButton.
- CategoryIcon.
- ExpenseSourceChip.
- ExpenseListItem.
- MemberContributionCard.
- ContributionProgressIndicator.
- EmptyState.
- CoastalTextField.
- PaymentSourceSelector.
- CategorySelector.
- CoastalDialog.
- SectionHeader.

These names are suggestions, not mandatory class names.

Prefer a small, maintainable design system over dozens of trivial composables.

### Theme architecture

Organize reusable definitions for:

- Colors.
- Typography.
- Shapes.
- Spacing.
- Elevation.
- Component styling.
- Light and dark themes.

Use the existing package structure and architecture wherever possible.

Avoid creating duplicate theme systems or unnecessary abstractions.

Ensure every redesigned screen consumes the same design tokens.

---

# 22. DATABASE AND FINANCIAL INTEGRITY

This section is critical.

The redesign must not alter existing financial behavior.

The application is an offline trip cashbox, not a complete accounting or settlement product.

### Existing entities

Trip:

- Name.
- Optional destination.
- Planned budget in paise.
- Creation timestamp.

Member:

- Trip ID.
- Name.
- Expected contribution in paise.

Contribution:

- Trip ID.
- Member ID.
- Amount in paise.
- Payment date.
- Optional note.

Expense:

- Trip ID.
- Optional payer member ID.
- Title.
- Amount in paise.
- Category.
- Date.
- Payment source.
- Optional notes.

### Mandatory financial rules

1. Member outstanding = expected contribution minus payments recorded.
2. Negative member outstanding must be presented as an excess payment.
3. Contributions collected = sum of recorded contribution payments.
4. Total expenses = shared-fund expenses plus personal expenses.
5. Shared cash remaining = contributions collected minus shared-fund expenses.
6. Budget remaining = planned budget minus total expenses.
7. Dashboard outstanding = sum of positive member outstandings only.
8. Personal expenses do not reduce shared cash.
9. Personal expenses do count toward total trip spending and budget usage.
10. Personal expenses are not automatically counted as contributions.
11. No automatic reimbursement entries are created.
12. Planned budget and total expected member contributions are independent values.
13. Money remains stored as integer paise using `Long`.
14. Currency remains INR.

Do not modify the existing formulas, repository behavior, or database schema to make the new UI work.

Do not introduce destructive Room migrations.

Do not replace existing database data with sample data.

Do not change foreign-key relationships or cascade behavior.

Use the existing domain calculations and ViewModels to supply UI state.

If an existing financial edge case needs to be displayed differently, change only the presentation.

---

# 23. ACCESSIBILITY AND USABILITY

A visually beautiful application that is difficult to use is not finished.

Ensure:

- Readable text contrast.
- Sufficient touch-target sizes.
- Meaningful content descriptions.
- Proper screen-reader labels.
- Clear input labels and validation.
- No reliance on color alone to communicate outstanding, excess, or overspending.
- Appropriate accessibility semantics for clickable cards and icon buttons.
- Stable keyboard navigation where applicable.
- No text clipping at larger font sizes.
- Correct handling of system back navigation.
- Clear destructive-action confirmations.

Keep the interface visually calm without sacrificing usability.

---

# 24. IMPLEMENTATION WORKFLOW

Follow this workflow from start to finish.

## Phase 1 — Audit

Inspect the repository and understand the current implementation.

Identify the existing screens, shared components, theme, assets, navigation, ViewModels, and database architecture.

## Phase 2 — Design system

Implement the reusable Coastal Soft color tokens, typography, shapes, spacing, component styles, and dark-theme equivalents.

Do not create a disconnected theme that existing screens do not use.

## Phase 3 — Dashboard

Redesign the dashboard first because it establishes the visual language for the rest of the app.

Implement the trip header, progress card, shared-cash hero, supporting chips, metric grid, primary actions, recent expenses, and overspend state.

Verify that every displayed value comes from the existing application state.

## Phase 4 — Expenses

Redesign the expense list and add/edit expense flows.

Verify Shared fund versus Personal behavior and payer validation.

## Phase 5 — Members

Redesign member cards, contribution summaries, payment history, and contribution forms.

Verify outstanding and excess payment presentation.

## Phase 6 — Settings

Redesign theme preferences and the About section.

Verify that theme changes persist and apply correctly.

## Phase 7 — Trip management and dialogs

Redesign trip creation/editing, deletion confirmations, and the remaining existing dialogs.

## Phase 8 — Polish

Review all screens for spacing, alignment, typography, colors, icons, animation, responsiveness, accessibility, and empty states.

Remove inconsistent styling and duplicate components.

## Phase 9 — Validation

Run the available project checks:

- Gradle build.
- Existing unit tests.
- Money and trip-total tests.
- Relevant UI tests if configured.
- Migration and persistence checks where available.

Fix regressions caused by the redesign.

Do not claim a build or test passed unless it actually ran successfully.

---

# 25. QUALITY BAR AND VISUAL ACCEPTANCE CRITERIA

The redesign is not complete until all of the following are satisfied.

### Visual consistency

- All screens use the same Coastal Soft design system.
- Colors match the specified palette.
- Typography follows a consistent hierarchy.
- Cards and dialogs use consistent corner radii.
- Spacing is systematic.
- Icons use a consistent family and sizing.
- The light theme closely follows the attached reference.
- The dark theme remains visually coherent.

### Functional correctness

- All existing features still work.
- All four bottom-navigation destinations work.
- Trip selection and trip management work.
- Existing data remains intact.
- Contribution recording and payment history work.
- Expense creation, editing, and deletion work.
- Personal and shared expense rules remain correct.
- Financial values use existing domain calculations.
- Settings and theme persistence work.
- Offline functionality remains intact.

### Layout quality

- No clipped text.
- No overlapping components.
- No horizontal overflow.
- No broken layouts on smaller screens.
- No buttons obscured by system bars or the keyboard.
- No fake values displayed in production UI.
- No unfinished placeholder components.

### Code quality

- No unnecessary architectural rewrites.
- No duplicated financial calculations in composables.
- No unnecessary dependencies.
- No destructive database changes.
- No dead code introduced by the redesign.
- No unnecessary hardcoded colors.
- No regressions in existing tests.

---

# 26. HOW TO HANDLE AMBIGUITY

When the reference image does not specify an implementation detail:

1. Prefer the existing application's behavior.
2. Follow the Coastal Soft design tokens.
3. Reuse existing assets when appropriate.
4. Use standard Android and Material 3 interaction conventions.
5. Choose the simplest implementation that maintains visual consistency.
6. Avoid inventing new functionality.

Do not stop to ask me about minor styling choices. Make reasonable design decisions and continue.

If a requested visual detail cannot be reproduced exactly because of Android constraints or the available assets, implement the closest practical equivalent without compromising functionality.

Do not replace actual implementation with a design description.

---

# 27. FINAL DELIVERABLE

Implement the redesigned UI directly in the existing repository.

At completion, provide:

1. A concise summary of the redesign.
2. A list of the files created or modified.
3. A summary of reusable design components and theme changes.
4. Confirmation of which existing functionality was preserved.
5. Actual build and test results.
6. Any remaining issues or visual differences that could not be resolved.

Do not finish after generating a plan or modifying only the theme file.

Do not use placeholder screens to represent unfinished work.

Do not change business logic simply to make the UI easier to implement.

**Final instruction: Study the attached Coastal Soft reference carefully, inspect the actual project, and then implement a complete, production-quality visual redesign of the existing TripBudget application. Every screen should look like it belongs to the same thoughtfully designed product. Preserve the application's functionality, financial rules, offline-first architecture, and existing data exactly. Visual consistency and functional correctness are equally important.**