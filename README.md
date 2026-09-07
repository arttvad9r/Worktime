# WorkTime

**WorkTime is a compact, offline Android timesheet for tracking shifts, worked hours and expected income from a calendar.**

No account, no cloud sync, no analytics and no ads. Data stays on the device unless you explicitly export it.

[**Download the latest APK**](https://github.com/arttvad9r/Worktime/releases/latest) · [Changelog](CHANGELOG.md) · [Privacy](docs/PRIVACY.md) · [License](LICENSE) · [Documentation](docs/README.md)

## Screenshots

<p align="center">
  <img src="docs/screenshots/calendar.jpg" width="42%" alt="WorkTime monthly calendar">
  <img src="docs/screenshots/year-summary.svg" width="42%" alt="WorkTime yearly summary">
</p>

<p align="center"><sub>Monthly calendar · Year summary</sub></p>

## What it does

- Calendar-first shift entry: tap a day, enter duration and hourly rate, save.
- Shows worked duration and income directly in calendar cells.
- Monthly summary with work days, total hours, income, bonuses and averages.
- Read-only yearly summary with per-month totals.
- Optional bonus and penalty amounts for individual entries.
- Default hourly rate plus bulk rate changes for a selected period.
- JSON export/import and CSV export through the Android system file picker.
- System, light and dark themes.
- Optional home-screen widget with the current month summary.
- Local-first operation with no account or network requirement.

WorkTime is intentionally ruble-focused: monetary values are displayed in `₽`; currency conversion and multi-currency accounting are outside the current scope.

## Install

WorkTime is distributed directly through **GitHub Releases** as a signed, optimized APK.

1. Open the [latest release](https://github.com/arttvad9r/Worktime/releases/latest).
2. Download `WorkTime-<version>.apk`.
3. If Android asks, allow your browser or file manager to install apps from that source.
4. Install the APK. Future releases signed with the same WorkTime certificate can be installed over the existing app without removing its data.

The project currently targets Android 8.0+ (`minSdk 26`). Every release also includes `SHA256SUMS.txt`; the pinned production signing-certificate fingerprint is stored in [`release/production-signing-cert-sha256.txt`](release/production-signing-cert-sha256.txt).

## Privacy and data

WorkTime is designed to work entirely on-device:

- entries are stored locally with Room;
- preferences are stored locally with DataStore;
- the app does not request the Android `INTERNET` permission;
- there are no analytics or advertising SDKs;
- automatic Android backup/device transfer is disabled;
- data leaves the app only through user-directed JSON/CSV export.

See [`docs/PRIVACY.md`](docs/PRIVACY.md) for the full data-handling description.

## Product rules

- One aggregate entry per date.
- Worked time is limited to `0..24:00`.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only entries are valid but do not count as work days.
- A saved entry keeps its hourly-rate snapshot; changing the default rate does not rewrite history.
- Bulk rate changes affect only entries inside the selected period and leave the default rate unchanged.

<details>
<summary>Calculation model</summary>

```text
ratePayMicros = roundHalfUp(workedMinutes × hourlyRateMicros / 60)
entryTotalMicros = ratePayMicros + bonusMicros - penaltyMicros
monthTotalMicros = sum(entryTotalMicros)
workDays = count(entries where workedMinutes > 0)
```

Amounts use integer micros in domain/data code. Persisted monetary calculations do not use `Float` or `Double`.

</details>

## Stack

- Kotlin 2.4.x
- Jetpack Compose + Material 3
- AGP 9.3.2 / Gradle Wrapper 9.7.1 / Java 17
- Room 2.8.4
- DataStore 1.2.1
- coroutines and `StateFlow`
- JUnit 6 and AndroidX Test

## Build and verify

Use the repository Gradle Wrapper. The preferred local verification command is:

```bash
./scripts/verify.sh
```

It runs the repository static audit, JVM tests, debug/release lint and Android build verification. GitHub Actions also runs managed-device instrumentation tests and exercises the release-signing path with a disposable CI-only key.

Repository modernization/refactoring is complete; the remaining release gates require physical-device verification. The exact audit boundary is recorded in [`docs/MODERNIZATION_STATUS.md`](docs/MODERNIZATION_STATUS.md).

For release signing and the GitHub Releases workflow, see [`docs/RELEASE_SIGNING.md`](docs/RELEASE_SIGNING.md) and [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

## Documentation

- [Product](docs/PRODUCT.md)
- [UX](docs/UX.md)
- [UI system](docs/UI_SYSTEM.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Build and CI](docs/BUILD.md)
- [Testing](docs/TESTING.md)
- [Modernization status](docs/MODERNIZATION_STATUS.md)
- [Android QA](docs/ANDROID_QA.md)
- [Roadmap](docs/ROADMAP.md)
- [Backlog](docs/BACKLOG.md)
- [Privacy](docs/PRIVACY.md)

## License

WorkTime is open-source software released under the [MIT License](LICENSE).

Copyright (c) 2026 arttvad9r.
