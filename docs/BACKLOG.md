# Prioritized backlog

**Repository modernization is complete.** The audited application/code baseline is PR #95 (`49d5bfa71e3d49a713377548c5bcf0378796d9ca`) with green post-merge Android CI. The items below are physical release/device verification or future product work, not unfinished refactoring.

## P0 — physical release verification

1. Build and install the exact `main` candidate on the primary compact phone; record device model, Android version and commit SHA.
2. Create/edit/delete entries, relaunch the app and verify Room/DataStore persistence.
3. Move through duration/rate/bonus/penalty with the numeric IME open; confirm the keyboard does not open with the sheet, focus order and that Done on penalty saves.
4. Swipe between months and use the arrows; confirm no lag and that the header matches the visible page.
5. Verify calendar selected/today/populated states and bonus/penalty dots in light and dark themes.
6. Confirm cell, monthly card and yearly totals agree for entries with fractional-cent pay.
7. Run `Change rate for period`; verify the affected-record count, blocked invalid ranges, unchanged default rate and Undo.
8. Export JSON and CSV; import the JSON backup, confirm restore and the Undo snackbar; confirm malformed import writes nothing.
9. Open Year summary from the monthly card, switch years, verify bars and the collapsed empty-month line.
10. Add the home-screen widget, change an entry and confirm refresh, theme behavior and tap-through.
11. Check Russian and English locales, rotation/window resize, increased font scale and TalkBack.
12. Complete the remaining items in `RELEASE_CHECKLIST.md`.

## P1 — release packaging and hardening

- Measure the checked-in Baseline Profile with Macrobenchmark on a representative physical device before treating it as a demonstrated performance improvement.
- Capture final release screenshots; update committed screenshot goldens only after intentional visual changes have been reviewed.
- Review signing, launcher assets and GitHub Release presentation before public distribution.
- Keep the exact release candidate green through `./scripts/verify.sh` and GitHub Actions before tagging or distributing it.

## P2 — future decisions

- rate periods table (per-day rate history) with its own backup version;
- multiple work profiles/jobs;
- overtime/pay-period configuration.

Do not reintroduce currency selection, notes, validation helper text, cloud accounts or timers without an explicit product decision. Do not add a separate landscape-only product mode; adaptive rotation/window resizing remains part of the Android quality contract.
