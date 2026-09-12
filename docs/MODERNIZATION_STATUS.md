# 2026 rewrite status

This file describes the new implementation on `rewrite/worktime-2026`. It intentionally does not inherit completion claims from the previous WorkTime codebase.

## Implemented

- single-activity Compose application;
- Material 3 light/dark/system themes;
- fixed 7 × 6 monthly calendar with today/selected/saved/adjacent-month states;
- localized calendar accessibility descriptions;
- day add/edit/delete with worked minutes, rate, bonus, fine, signed other adjustment and note;
- exact integer money calculations and checked arithmetic;
- compact/expanded monthly summary;
- monthly and yearly reports with 12-month income overview;
- default rate, month rate, bounded range rate and open-ended rate-from-date;
- explicit historical rate snapshots and transactional bulk changes;
- Room-backed work days, rate periods and settings;
- versioned JSON export/import with preview and transactional restore;
- Navigation 3 back stack with system/predictive Back integration;
- edge-to-edge/safe-drawing handling and bounded large-screen content width;
- moderate haptic feedback for destructive day deletion;
- Russian and default resources for the modern UI;
- debug application-ID isolation from the production package;
- optimized/R8 release build and signing verification path.

## Automated gates

The branch CI is designed to require all of the following on every head before it is considered validated:

- rewrite static audit;
- JVM/unit tests;
- debug and release lint;
- debug, androidTest and optimized release compilation;
- committed Room-schema drift check;
- API 30 managed-device instrumented suite, including repository/rate/backup/migration coverage and modern UI smoke;
- API 37 modern startup/navigation/accessibility smoke;
- disposable-key release-signing smoke;
- APK and R8 mapping artifacts.

A check is evidence only when it has actually completed successfully for the exact commit. Do not infer a future commit is green from an earlier run.

## Deliberately removed legacy evidence

The rewrite branch no longer compiles or ships the old:

- `data`, `domain` and `ui` runtime trees;
- `AppContainer` / `WorkTimeApplication` bootstrap;
- legacy Room schema;
- widget implementation/resources;
- screenshot baselines;
- Baseline Profile / Macrobenchmark modules and workflows;
- tests that exercised the rejected runtime rather than the rewrite.

Historical files remain recoverable from Git history. Their previous passing tests are not evidence for this implementation.

## Release gates still open

The rewrite must not be described as a completed public replacement until these are resolved:

1. **Old-data policy.** The rewrite uses `worktime-modern.db`. Decide whether the first rewrite release is intentionally clean-state or implement/test deterministic migration/import from the published app.
2. **Versioning.** Increment `versionCode` and choose the release `versionName` before distribution as an update.
3. **Physical-device accessibility.** Walk the main flows with TalkBack and large font scales on hardware.
4. **Keyboard/OEM behavior.** Verify repeated numeric editing with a real IME and bottom-sheet resize behavior.
5. **Haptics.** Confirm the deliberately small haptic set feels appropriate on hardware.
6. **Large/resizable configuration.** Validate at least one tablet/foldable/freeform-resize case; Android may ignore the phone portrait request on large screens.
7. **Visual QA.** Capture and review current rewrite screens. Old screenshots were removed because they represented the rejected design.
8. **Exact release update.** Build with the production signing identity and install that exact APK over the previous public release according to the selected data policy.
9. **Performance measurement.** Measure the modern app first. Add a new Baseline Profile only if measurements justify it; never reuse the old profile.

## Definition of repository-level completion

Repository-level rewrite work is complete only when the exact candidate commit has green automated gates, the active source tree contains only the modern runtime, documentation describes the actual implementation, and known release limitations are stated rather than hidden.
