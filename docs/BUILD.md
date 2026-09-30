# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.7.1
- Android Gradle Plugin 9.3.2
- Kotlin 2.4.10
- compile/target SDK 37
- Compose BOM 2026.08.00

Compose libraries, including Material 3, are resolved from the stable BOM. Do not add a Material 3 alpha override solely to work around editor IME behavior; the attempted alpha override did not eliminate keyboard rebuilding on the tested physical device and has been removed.

## Release optimization

The `release` build uses the AGP 9.3 optimization DSL:

```kotlin
optimization {
    enable = true
}
```

This enables R8 code optimization and optimized resource shrinking together. Project-specific keep rules remain in `app/proguard-rules.pro` and should be added only for demonstrated runtime/reflection requirements.

Release signing is opt-in through `releaseStoreFile`, `releaseStorePassword`, `releaseKeyAlias` and `releaseKeyPassword` Gradle properties or their `RELEASE_*` environment-variable equivalents. Without all four values the project intentionally produces an unsigned optimized release APK for verification; it never falls back to debug signing.

The permanent app-signing-key procedure is documented in `docs/RELEASE_SIGNING.md`. Keep the keystore and passwords outside source control and GitHub Actions. `scripts/build_release_candidate.sh` builds and verifies the exact signed APK candidate without writing signing secrets to the project.

## Local verification

Use a compatible Android SDK/JDK installation and the checked-in wrapper:

```bash
./scripts/verify.sh
```

Equivalent tasks:

```bash
python3 scripts/static_audit.py
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:lintRelease
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:assembleRelease
```

Typical verification outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
app/build/outputs/mapping/release/
```

The release APK produced without signing inputs is verification-only and must not be distributed.

For an actual signed candidate, configure the `RELEASE_*` inputs locally and run:

```bash
./scripts/build_release_candidate.sh
```

That helper requires a clean Git tree, verifies the APK with `apksigner`, computes SHA-256, records the signer certificate fingerprint and writes distribution files under `app/build/outputs/release-candidate/`.

Do not substitute an arbitrary system Gradle version when recording verification evidence; the wrapper defines the project Gradle version.

## Host environment

Primary development is on Arch Linux with a system JDK 17 and Android SDK. The project has no distribution-specific build layer: any compatible Linux/macOS/Windows Android development environment is valid when the Java/Android SDK requirements are met.

Use the checked-in Gradle Wrapper for project builds and keep one compatible `adb` installation active during physical-device testing.

## CI

`.github/workflows/android.yml` runs for pull requests and pushes to `main`. The `verify` job runs the static audit, JVM tests, debug/release lint, debug APK/test APK assembly and optimized unsigned release APK build. It uploads verification reports, the debug APK, unsigned release APK and R8 mapping as short-lived artifacts.

After `verify` succeeds, two independent jobs run:

- `signing-smoke` creates a disposable CI-only keystore, builds the optimized release APK through the normal `RELEASE_*` signing inputs and verifies it with Android `apksigner`. The disposable signed APK is not uploaded or distributed.
- `instrumented-tests` executes the instrumentation suite on a Pixel 2 API 30 AOSP ATD Gradle Managed Device. KVM access and Android SDK licenses are configured explicitly on the hosted Linux runner.

The permanent WorkTime signing key is never used by GitHub Actions.

## GitHub release creation

Release publication is intentionally local so the permanent signing key remains offline.

After the exact `main` commit is green and `build_release_candidate.sh` has produced the signed candidate:

1. create and push the tag `v<versionName>` on that same commit;
2. run `./scripts/create_github_release.sh` from the same clean checkout;
3. the helper verifies the metadata/checksum and local/remote tag, then creates a **draft GitHub Release** with the existing candidate files;
4. download the APK back from GitHub and complete physical-device QA;
5. publish the same draft release without rebuilding or replacing the APK.

The draft contains:

- `WorkTime-<version>.apk`;
- `SHA256SUMS.txt`;
- release metadata including commit/version/signer fingerprint;
- matching R8 mapping.

`create_github_release.sh` uses an authenticated local GitHub CLI session. It never reads the private signing key because signing is already complete before upload.

Third-party Actions used by CI are pinned to immutable commit SHAs and use Node 24-native releases.

A red CI run must be classified from its actual logs rather than assumed to be infrastructure. Test synchronization should wait for observable operation completion instead of relying on `advanceUntilIdle()` when production work runs in `viewModelScope` outside the coroutine-test scheduler.

## Device verification

Automated verification does not replace physical-device QA for the interaction paths that depend on OEM behavior. Recheck at minimum:

- day-editor focus/IME chain (duration → rate → bonus → penalty);
- haptic feedback on the intentionally limited interaction set;
- modal-sheet tap/drag/insets;
- calendar swipe/arrows and the `+` in today's empty cell;
- system document picker import/export;
- home-screen widget refresh/tap-through and compact layout;
- launcher icon presentation after install/update;
- fresh install and update-over-previous-release using the exact APK downloaded from the draft GitHub Release.

Record exact device model, Android version, tested commit, APK SHA-256 and signer fingerprint for a release candidate.
