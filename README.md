# TripBudget

Offline personal Android app for tracking trip budgets, member contributions, and expenses. Built with Kotlin, Jetpack Compose, Material 3, and Room.

## Features

- Create and switch trips from a top trip selector
- Dashboard: budget, collected cash, expenses, shared cash remaining, outstanding contributions, overspend warning
- Members: expected vs paid, partial payments, payment history
- Expenses: shared fund or personal money, categories, edit/delete
- Settings: light / dark / system theme
- All data stored locally in SQLite (Room); no network required

Money is stored as integer **paise** (`Long`). Currency is **INR**.

## Requirements

- Android Studio Ladybug (2024.2+) or newer recommended
- JDK 17+
- Android SDK 35 (installed via Android Studio SDK Manager)
- Min device API 26

## Setup

1. Open this folder in Android Studio (**File → Open**).
2. Copy [`local.properties.example`](local.properties.example) to `local.properties` and set `sdk.dir` to your Android SDK path (Android Studio usually creates this automatically).
3. Let Gradle sync finish.
4. Run on an emulator or device (**Run → Run 'app'**).

### Command line

```bash
# Set JDK if needed
export JAVA_HOME=/path/to/jdk-17-or-21

# Build debug APK
./gradlew :app:assembleDebug

# Unit tests (calculation rules)
./gradlew :app:testDebugUnitTest
```

If `./gradlew` cannot download dependencies, open the project once in Android Studio so the SDK and Google Maven repos resolve.

### Domain smoke test (no Android SDK)

```bash
javac scripts/DomainSmokeTest.java -d scripts
java -cp scripts DomainSmokeTest
```

## Project layout

```
app/src/main/java/com/tripbudget/app/
  data/       Room entities, DAOs, database, repository, theme prefs
  domain/     Money helpers, payment source, categories, trip totals
  ui/         Theme, navigation, screens, ViewModels
  di/         Simple AppContainer (no Hilt)
```

## Financial rules (summary)

| Metric | Formula |
| --- | --- |
| Member outstanding | expected − paid (show excess if negative) |
| Contributions collected | sum of recorded payments |
| Total expenses | all expenses (shared + personal) |
| Shared cash remaining | collected − shared-fund expenses |
| Budget remaining | planned budget − total expenses |
| Outstanding (dashboard) | sum of **positive** member outstandings only |

Personal expenses count toward total spend but do not reduce shared cash and are not auto-counted as contributions.

## License

Personal use.
