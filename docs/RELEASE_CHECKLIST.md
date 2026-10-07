# Release checklist

Run this checklist against the exact commit and APK that will be published. Prior verification from another commit is supporting evidence, not a substitute for the final candidate gate.

## Automated build gate

- [ ] `python3 scripts/static_audit.py` passes on the exact release candidate.
- [ ] `:app:testDebugUnitTest` passes with no failed or skipped regression tests.
- [ ] `:app:validateDebugScreenshotTest` passes against the reviewed committed golden references.
- [ ] `:app:lintDebug` and `:app:lintRelease` pass; remaining hints are reviewed and understood.
- [ ] `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:assembleRelease` pass through the checked-in Gradle Wrapper.
- [ ] `:app:assembleBenchmark`, `:macrobenchmark:assembleBenchmark` and `:baselineprofile:assemble` pass through the same local/CI verification gate.
- [ ] Release optimization remains enabled through AGP `optimization { enable = true }`.
- [ ] The checked-in Baseline Profile is source-level, while `nonMinifiedRelease` keeps capture unobfuscated and the production `release` variant remains optimized.
- [ ] The matching GitHub Actions run is green, including `signing-smoke`, API 30 managed-device instrumentation and the API 37 target-platform smoke.
- [ ] CI retains the unsigned optimized release APK and R8 mapping for inspection; the unsigned APK is never distributed.
- [ ] Normal PR/main CI uses only its disposable signing key.
- [ ] Release signing never falls back to debug signing.

## App-signing key

- [ ] A dedicated WorkTime release keystore exists in the git-ignored `keystore/` directory, never in Git.
- [ ] Keystore and key passwords are stored in a password manager, not source files or shell scripts.
- [ ] At least two encrypted backups of the release keystore exist in separate locations.
- [ ] The public signing certificate has been exported and its SHA-256 fingerprint recorded.
- [ ] The permanent private signing key is not stored in GitHub Actions or release assets.
- [ ] The private release key is treated as permanent Android update identity; it is not replaced between releases.

## Version and candidate

- [ ] `versionCode` is greater than the previous public release.
- [ ] Final `versionName` matches the intended Git tag `v<versionName>`.
- [ ] The candidate commit is contained in `main` and its full Android CI run is green.
- [ ] `./scripts/build_release_candidate.sh` is run locally with the permanent WorkTime signing key.
- [ ] The script produces a signed optimized APK, checksum file, metadata and R8 mapping.
- [ ] `apksigner` verification passes and the recorded signer SHA-256 matches the permanent WorkTime certificate.
- [ ] APK SHA-256 in `SHA256SUMS.txt` matches the candidate file.
- [ ] The release tag points exactly to that tested commit and is pushed to `origin`.
- [ ] No code, resources, signing inputs or version metadata change after candidate creation without producing a new candidate and tag.

## GitHub Release

- [ ] `./scripts/create_github_release.sh` validates the candidate metadata/checksum and the local/remote `v<versionName>` tag.
- [ ] The helper creates a **draft** GitHub Release without rebuilding or resigning the APK.
- [ ] The draft contains `WorkTime-<version>.apk`, `SHA256SUMS.txt`, release metadata and the matching R8 mapping.
- [ ] The APK downloaded back from the draft has the same SHA-256 recorded by the build.
- [ ] The downloaded APK has the expected permanent signer certificate.
- [ ] Release notes are reviewed for user-visible changes and known limitations.
- [ ] The release remains draft until physical-device QA of that exact downloaded APK is complete.
- [ ] After QA, the existing draft is published; the APK is not rebuilt or replaced.

## Functional QA

- [ ] Fresh installation of the exact release APK succeeds on a supported physical phone.
- [ ] Update installation over the previous public APK succeeds without uninstalling.
- [ ] Existing Room/DataStore data survives the update.
- [ ] Create, edit, delete and relaunch work normally.
- [ ] Process death/relaunch preserves Room/DataStore state and returns to a valid UI state.
- [ ] Monthly calculation is manually reconciled.
- [ ] Historical rate snapshots are verified.
- [ ] Empty, populated, bonus and penalty months are verified.
- [ ] Worked time with zero rate is rejected; adjustment-only zero-rate entries remain valid.
- [ ] Report sheet opens by both tap and drag in compact layout.
- [ ] Repeated report open/collapse cycles keep the same peek height and anchors.
- [ ] Holding the report handle never shows Material's drag-handle tooltip.
- [ ] Wider adaptive layout shows the monthly report in the supporting pane without duplicating the compact report sheet.
- [ ] Persistence failure path retains the draft/settings surface and shows transient feedback.
- [ ] JSON export/import is verified through the system document picker.
- [ ] JSON import rollback preserves the previous Room/DataStore state when preference restore is forced to fail.

## UI/accessibility

- [ ] Compact portrait phone remains the primary layout and does not regress.
- [ ] Rotation/window resizing preserves a usable UI and state; on a large-screen/API 37 environment, verify compact/short/supporting-pane modes behave according to available window space rather than an orientation lock.
- [ ] Calendar grid remains spatially stable for a given window size; horizontal month swipes track the finger and settle on the expected month.
- [ ] Russian labels fit, including `Системная` and `Отработано часов`.
- [ ] Invalid numeric input uses red outline only; no validation helper text appears.
- [ ] Numeric IME stays visible while moving between duration/rate/bonus/penalty.
- [ ] Expanding bonus/penalty does not close/reopen the keyboard or move the modal sheet.
- [ ] Intended haptics occur only on the documented interaction set; ordinary navigation remains silent.
- [ ] Settings initial `0` is selected on focus without changing sheet height.
- [ ] Save is reachable after keyboard dismissal.
- [ ] Narrow compact screen and 200% font-scale checks pass.
- [ ] Light/dark contrast and TalkBack checks pass.
- [ ] Fixed `₽` and `₽/h` labels are readable in supported locales.
- [ ] Home-screen widget theme, layout, body tap, `+` action and refresh after data changes are verified on the target launcher.
- [ ] Widget refresh is verified across date/time/time-zone changes and process restart.
- [ ] Final launcher icon and GitHub release presentation are approved.

## Privacy/security

- [ ] Final dependency graph and merged manifest contain no unexpected network, analytics, advertising or privacy-sensitive permission changes.
- [ ] Backup/device-transfer behavior matches `docs/PRIVACY.md` on the release build.
- [ ] In-app Privacy & data disclosure still matches actual behavior.
- [ ] No private signing material, passwords, user data or sensitive logs are present in release assets or CI output.

The release is not considered device-verified until the exact APK downloaded from the draft GitHub Release has been tested and its device model, Android version, commit, checksum and signer fingerprint have been recorded.
