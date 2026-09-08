# Release checklist

Run this checklist against the exact commit and APK that will be published. Prior verification from another commit is supporting evidence, not a substitute for the final candidate gate.

## Automated build gate

- [ ] `python3 scripts/static_audit.py` passes on the exact release candidate.
- [ ] `:app:testDebugUnitTest` passes with no failed or skipped regression tests.
- [ ] `:app:validateDebugScreenshotTest` passes against the reviewed committed golden references.
- [ ] `:app:lintDebug` and `:app:lintRelease` pass; remaining hints are reviewed and understood.
- [ ] `:app:analyzeReleaseR8Config` passes and its report is retained for review.
- [ ] `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:assembleRelease` pass through the checked-in Gradle Wrapper.
- [ ] `:app:assembleBenchmark`, `:macrobenchmark:assembleBenchmark` and `:baselineprofile:assemble` pass through the same local/CI verification gate.
- [ ] Release optimization remains enabled through AGP `optimization { enable = true }`.
- [ ] The checked-in Baseline Profile is source-level, while `nonMinifiedRelease` keeps capture unobfuscated and the production `release` variant remains optimized.
- [ ] The optimized release APK contains `assets/dexopt/baseline.prof`; the release is rejected if the compiled Baseline Profile is missing.
- [ ] The matching GitHub Actions run is green, including `signing-smoke`, API 30 managed-device instrumentation and the API 37 target-platform smoke.
- [ ] CI retains the unsigned optimized release APK, R8 mapping and verification reports for inspection; the unsigned APK is never distributed.
- [ ] Normal PR/main CI uses only its disposable signing key.
- [ ] Release signing never falls back to debug signing.

## App-signing key

For the first actual RuStore production release, WorkTime `0.2.0` establishes the replacement production signing identity documented in `docs/RELEASE_SIGNING.md`. The key used for the early direct `0.1.0` GitHub build is no longer available, and there is no public installed user base that must be migrated for this launch.

- [ ] A dedicated WorkTime `0.2.0+` release keystore exists outside the repository.
- [ ] Keystore and key passwords are stored in a password manager, not source files or shell scripts.
- [ ] At least two encrypted backups of the release keystore exist in separate locations.
- [ ] One backup has been opened and verified with `keytool`.
- [ ] The public signing certificate has been exported and its SHA-256 fingerprint recorded in `release/production-signing-cert-sha256.txt`.
- [ ] The permanent private signing key is not stored in GitHub Actions or release assets.
- [ ] The new private release key is treated as permanent Android update identity from `0.2.0` onward and is not replaced between routine releases.

## Version and candidate

- [ ] `versionCode` is greater than every previously published build using the active production identity.
- [ ] Final `versionName` matches the intended release and store metadata.
- [ ] The candidate commit is contained in `main` and its full Android CI run is green.
- [ ] `./scripts/build_release_candidate.sh` is run locally with the permanent WorkTime `0.2.0+` signing key.
- [ ] The script produces a signed optimized APK, checksum file, metadata and R8 mapping.
- [ ] The script runs the R8 Configuration Analyzer and confirms the compiled Baseline Profile is packaged in the APK.
- [ ] `apksigner` verification passes and the recorded signer SHA-256 matches the pinned WorkTime production certificate.
- [ ] APK SHA-256 in `SHA256SUMS.txt` matches the candidate file.
- [ ] No code, resources, signing inputs or version metadata change after candidate creation without producing a new candidate.

## RuStore release

- [ ] The exact locally verified signed APK is uploaded to RuStore; no CI unsigned APK is ever uploaded.
- [ ] RuStore package name is `com.worktime.app` and version metadata matches the candidate.
- [ ] Store listing, screenshots/cards, permissions declaration and user-data declaration match the exact APK.
- [ ] Public privacy-policy and user-agreement URLs are complete and accessible without authentication.
- [ ] The developer identity, contact and legal details in RuStore match the public legal documents.
- [ ] Manual publication is selected for the first release so the approved public card can be reviewed before rollout.
- [ ] The APK is not rebuilt, replaced or re-signed after final physical-device QA without restarting the release gate.

## Optional direct GitHub release

If the same build is also distributed directly, use `./scripts/create_github_release.sh` only after the candidate has passed the same release gate.

- [ ] The helper validates the candidate metadata/checksum and matching local/remote tag.
- [ ] The draft contains `WorkTime-<version>.apk`, `SHA256SUMS.txt`, release metadata and the matching R8 mapping.
- [ ] The APK downloaded back from the draft has the same SHA-256 and signer certificate as the verified candidate.
- [ ] The existing draft is published after QA; the APK is not rebuilt or replaced.

## Functional QA

- [ ] Fresh installation of the exact release APK succeeds on a supported physical phone.
- [ ] For releases after `0.2.0`, update installation over the previous production APK succeeds without uninstalling.
- [ ] Existing Room/DataStore data survives a same-certificate update.
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
- [ ] The subdued Settings privacy footer remains readable and exposes at least a 48 dp touch target.
- [ ] The full-screen Privacy page is scrollable, readable at large font scale and returns correctly to Settings.
- [ ] Home-screen widget theme, layout, body tap, `+` action and refresh after data changes are verified on the target launcher.
- [ ] Widget refresh is verified across date/time/time-zone changes and process restart.
- [ ] Final launcher icon and RuStore presentation are approved.

## Privacy/security

- [ ] Final dependency graph and merged manifest contain no unexpected network, analytics, advertising, crash-reporting or privacy-sensitive permission changes.
- [ ] Backup/device-transfer behavior matches `docs/PRIVACY.md` on the release build.
- [ ] In-app `Privacy` disclosure still matches actual behavior.
- [ ] RuStore `Безопасность данных пользователя` declaration matches the exact app behavior, including any user-entered financial data category required by the current form.
- [ ] Public privacy policy separately describes developer-side support-contact data and does not imply local work records are uploaded.
- [ ] The actual support mailbox/provider has been reviewed for data-location, retention and transfer implications before publication.
- [ ] The developer has checked whether notification to Roskomnadzor is required before developer-side personal-data processing and completed it when applicable.
- [ ] No private signing material, passwords, user data or sensitive logs are present in release assets or CI output.

The release is not considered device-verified until the exact signed APK intended for distribution has been tested and its device model, Android version, commit, checksum and signer fingerprint have been recorded.
