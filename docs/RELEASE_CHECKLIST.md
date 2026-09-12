# Rewrite release checklist

This checklist applies to the new implementation on `rewrite/worktime-2026`. Passing repository CI alone is not permission to publish.

## Repository state

- [ ] Exact release commit is identified and the working tree is clean.
- [ ] Static audit passes.
- [ ] Unit tests and debug/release lint pass.
- [ ] Debug, androidTest and release APKs build.
- [ ] Room schema drift check passes and migration tests are green.
- [ ] API 30 instrumented suite passes.
- [ ] API 37 modern smoke passes.
- [ ] Signing smoke passes.
- [ ] No legacy runtime, old screenshot baseline, old Baseline Profile or benchmark module has been reintroduced.

## Product verification

- [ ] Fixed six-week calendar is stable across short/long months and year boundaries.
- [ ] Add/edit/delete a work day and verify persistence after process/app restart.
- [ ] Verify zero-work adjustment-only dates.
- [ ] Verify bonus, fine and signed other adjustment totals.
- [ ] Verify default-rate changes do not rewrite existing rate snapshots.
- [ ] Verify month, bounded-range and open-ended rate changes, including affected-record preview.
- [ ] Verify monthly report and all 12 months in yearly report.
- [ ] Export JSON, inspect preview, restore it transactionally and confirm data/settings/rate periods.
- [ ] Verify light, dark and system theme.
- [ ] Verify RUB, USD and EUR display formats.

## Device/accessibility verification

- [ ] TalkBack traversal and calendar descriptions on a physical phone.
- [ ] Large font/display scale without clipped critical controls.
- [ ] Real numeric keyboard/IME editing and bottom-sheet resize behavior.
- [ ] Gesture navigation, status/navigation bars, cutout/insets.
- [ ] Haptic feedback on intended destructive action only.
- [ ] At least one large/resizable/tablet/foldable configuration remains usable when portrait is ignored.

## Update/data policy

- [ ] Increment `versionCode` and choose the rewrite `versionName`.
- [ ] Decide: clean-state rewrite or deterministic legacy migration/import.
- [ ] If migration/import is supported, add regression tests from real legacy-format fixtures before release.
- [ ] Install the exact production-signed candidate over the latest public WorkTime APK without uninstalling and verify the chosen data behavior.

## Release artifact

- [ ] Build with `./scripts/build_release_candidate.sh` using the production signing identity.
- [ ] `apksigner` verification succeeds and signer SHA-256 matches `release/production-signing-cert-sha256.txt`.
- [ ] Retain APK SHA-256, exact commit and R8 mapping.
- [ ] Do not rebuild after final physical QA; publish the exact tested artifact.

## Performance

- [ ] Check startup and month-navigation smoothness on representative hardware.
- [ ] Investigate measurable regressions before adding optimization machinery.
- [ ] Generate a new Baseline Profile only if modern-app measurements justify it; never restore the legacy profile as a shortcut.
