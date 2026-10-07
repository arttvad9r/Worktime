# Contributing

## Scope

Keep WorkTime a small calendar-first personal timesheet. Do not add notes, quick-duration presets, currency, accounts, projects, timers, landscape support or payroll complexity without an explicit product decision.

## Before opening or updating a pull request

1. Keep domain/data calculations in integer micros.
2. Preserve Room data and historical hourly-rate snapshots.
3. Update EN and RU resources together.
4. Update documentation when a UI or product contract changes.
5. Run:

```bash
./scripts/verify.sh
```

6. Execute the relevant physical-device checklist in `docs/ANDROID_QA.md` for UI changes.

Do not report a build, test or device result as passed unless the command/test actually ran to completion. Infrastructure failures must be recorded separately from code failures.

## Physical-device test safety

- Debug builds use `com.worktime.app.debug`; keep this application-id suffix in place so Gradle instrumentation cleanup cannot uninstall the real `com.worktime.app` package and its user data.
- Before running `connectedDebugAndroidTest` on a personal device, verify the tested package is the debug-suffixed package. Never run destructive instrumentation against the production application id.
- Release/benchmark install and update checks are separate release gates. Do not substitute a debug-signed build for a production-signed release candidate.
- Do not use `pm clear`, uninstall the production package or otherwise destroy existing user data as part of routine physical QA.

## Repository hygiene

- Branch from `dev` (`feat/<name>`, `fix/<name>`), merge back into `dev`, and delete the branch and its worktree after it is merged or superseded. `main` receives only releases and hotfixes, each tagged `vX.Y.Z`.
- Keep `main` and the current documentation as the source of truth; historical implementation snapshots belong in Git history.
- Close dependency-update pull requests when the same version is already present through another verified change.
- Keep CI actions pinned to immutable commit SHAs and let Dependabot group routine ecosystem updates instead of accumulating parallel one-package pull requests.
- Do not keep placeholder files in directories that already contain tracked generated/required artifacts.

## UI expectations

- The application is portrait-first, not portrait-locked: rotation and window resizing must stay usable (see `docs/DECISIONS.md`).
- The calendar geometry is fixed and must not depend on monthly data.
- Normal day entry should fit without scrolling when the keyboard is closed.
- Numeric focus changes must not intentionally clear/reopen the IME or animate the sheet height.
- Required actions remain reachable after keyboard dismissal.
- Numeric validation is intentionally outline-only; do not add helper-text rows without a product decision.
- Persistence errors must remain visible without changing modal geometry.
- Labels and values must survive Russian text and increased font scale.
- Calendar/status meaning should not rely solely on color; numeric field error state follows the explicit outline-only product rule and should retain Material error semantics.