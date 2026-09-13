# Contributing to the 2026 rewrite

## Scope

Treat `rewrite/worktime-2026` as a new implementation. Historical WorkTime code is behavioral/reference material only; do not restore old runtime trees, UI components, screenshot baselines, benchmark modules or Baseline Profiles as shortcuts.

Keep the product focused on fast calendar-based work tracking. Current supported concepts include worked duration, hourly-rate snapshots, bonus, fine, signed other adjustment, optional note, RUB/USD/EUR display currency, monthly/yearly reports and JSON backup/restore.

## Engineering invariants

- Persist money as `Long` minor currency units and duration as integer minutes.
- Do not use `Float`/`Double` in business/data money calculations.
- Preserve saved hourly-rate snapshots unless the user explicitly applies a bulk rate change to their date range.
- Keep Room as the persisted source of truth unless a concrete requirement justifies another store.
- Keep the architecture small; do not add DI/use-case/service abstractions without measurable value.
- Keep Navigation 3 as the application navigation layer.
- Preserve the fixed 42-cell calendar contract.
- Update default and Russian resources together for user-visible text.
- Do not add broad storage, network, analytics or advertising permissions/dependencies implicitly.

## Validation

Before calling a change complete, run:

```bash
./scripts/verify.sh
```

For a PR/head intended as a candidate, also require the GitHub Actions jobs for API 30 instrumentation, API 37 modern smoke and signing smoke to finish successfully. A check that was not run is not a pass.

Database changes must update the committed Room schema and include migration coverage. User-visible UI changes require current visual/device review; screenshots from the rejected implementation are not valid baselines.

## Device safety

Debug builds use `com.worktime.app.debug`; keep the suffix so instrumentation cleanup cannot uninstall the production package. Do not use `pm clear` or uninstall the user's production app as routine QA.

A production update must be tested with the real application ID and production signing identity. The rewrite currently uses `worktime-modern.db`, so update installation and old-data continuity are separate release questions.

## UI/accessibility expectations

- Phone UX requests portrait, but large/resizable configurations must remain usable when the platform ignores that request.
- Calendar height must remain structurally stable at six rows.
- Meaning must not rely only on color.
- Maintain localized TalkBack descriptions and meaningful selected-state semantics.
- Critical controls must remain reachable with increased font/display scale and with the IME shown/dismissed.
- Motion should be short and functional; do not reintroduce the legacy animation system.
- Haptics should be sparse and attached to meaningful actions rather than every tap.
