# WorkTime — 2026 rewrite

This branch contains the new Android implementation of WorkTime. The previous implementations remain available through Git history as behavioral/reference material, but they are not compiled into this application and their UI, architecture, tests and performance profiles are not treated as proof for the rewrite.

The latest published GitHub release predates this rewrite. Do not use that release as a visual or technical reference for the code on `rewrite/worktime-2026`.

## Current product

WorkTime is an offline, calendar-first work-hours and earnings tracker:

- a fixed **7 × 6** monthly calendar, so the grid never changes height between months;
- date cells show worked duration and calculated income;
- fast per-day editing of hours/minutes, hourly rate, bonus, fine, signed other adjustment and optional note;
- a compact monthly summary that expands in place;
- flow: **calendar → expanded monthly summary → monthly report → yearly report**;
- yearly report with all 12 months and a lightweight income bar for each month;
- default hourly rate plus explicit bulk changes for a selected month, a bounded date range or an open-ended range starting on a chosen date;
- saved work days keep a rate snapshot; changing the default rate does not silently rewrite history;
- JSON backup/export and validated, confirmed, transactional restore through Android's system document picker;
- system, light and dark themes;
- selectable RUB, USD and EUR display currency;
- localized dates, durations, numbers and accessibility descriptions.

There is no account, cloud sync, analytics or advertising SDK. The manifest does not request `INTERNET` or broad storage permission.

## Calculation model

Persisted monetary values use `Long` **minor currency units**; worked duration uses integer minutes. Business/data calculations do not use binary floating point.

```text
base = roundHalfUp(hourlyRateMinor × workedMinutes / 60)
total = base + bonusMinor - penaltyMinor + otherMinor
periodTotal = sum(day totals)
shiftCount = count(days where workedMinutes > 0)
```

`otherMinor` is signed; bonus and fine are stored separately. Arithmetic uses checked integer operations.

## Architecture

The rewrite deliberately stays small:

```text
MainActivity
  → ModernWorkTimeApp / Navigation 3
  → ModernViewModel (StateFlow)
  → ModernRepository
  → Room
```

Room is the single persisted source of truth for work days, rate periods and settings. The app uses manual constructor wiring rather than a DI framework. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Android baseline

- Kotlin 2.4.10
- Jetpack Compose + Material 3
- Navigation 3
- AGP 9.3.2 / Java 17
- `minSdk 26`, `compileSdk 37`, `targetSdk 37`
- Room 2.8.4
- coroutines / `StateFlow`
- edge-to-edge with safe-drawing insets
- phone UX requests portrait orientation; large/resizable configurations are kept usable with bounded content width rather than orientation hacks

## Build and verification

Use the repository Gradle Wrapper:

```bash
./scripts/verify.sh
```

The verification path runs the rewrite static audit, unit tests, debug/release lint, debug/androidTest/release builds and a Room schema-drift check. GitHub Actions additionally runs:

- the full managed-device instrumented suite on API 30;
- the modern startup/navigation/accessibility smoke on API 37;
- release-signing plumbing with a disposable CI-only key;
- R8 release output generation.

The generated Room v2 schema is committed under `app/schemas/com.worktime.app.modern.data.ModernDatabase/` and CI fails if code generation changes it unexpectedly.

## Release status

The rewrite is **not yet a public release**. Before replacing the existing `v0.1.0` APK, at minimum:

1. increment `versionCode` / choose the rewrite `versionName`;
2. decide and test the old-data policy — the rewrite intentionally uses `worktime-modern.db`, so the old application's database is not silently consumed;
3. perform physical-device TalkBack, keyboard, haptic, font-scale and resize checks;
4. install the exact production-signed candidate over the previous public APK and verify update/data behavior;
5. measure the modern build before deciding whether a new Baseline Profile is justified.

See [`docs/MODERNIZATION_STATUS.md`](docs/MODERNIZATION_STATUS.md) and [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

## Current documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/MODERNIZATION_STATUS.md`](docs/MODERNIZATION_STATUS.md)
- [`docs/PRIVACY.md`](docs/PRIVACY.md)
- [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md)
- [`docs/RELEASE_SIGNING.md`](docs/RELEASE_SIGNING.md)

Historical documentation and screenshots from the rejected implementation were removed from the rewrite branch; they remain recoverable from Git history.

## License

MIT. See [`LICENSE`](LICENSE).
