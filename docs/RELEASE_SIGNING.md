# Release signing

WorkTime uses a developer-controlled Android app-signing key. The private key is the **actual update identity** trusted by Android for directly distributed APKs and by RuStore for future APK updates.

## Production signing starts at 0.2.0

The private key that signed the early direct GitHub build `0.1.0` has been lost. That key cannot be reconstructed from the APK, certificate, fingerprint or repository history.

We cannot guarantee that no `0.1.0` production APK remains installed outside the developer's current devices. Development now uses an isolated debug package, but any `0.1.0` production install signed by the lost certificate cannot be updated in place. WorkTime `0.2.0` is therefore the first RuStore production identity and establishes a new permanent signing identity for all `0.2.0+` production builds.

Consequences:

- the first RuStore release can start with the newly generated certificate because WorkTime has not previously established a RuStore signing identity;
- WorkTime `0.2.0` and every later RuStore/direct production APK must use the **same new key**;
- any installed `0.1.0` APK cannot be updated in place with the replacement certificate;
- if data from an installed `0.1.0` must be preserved, export JSON first, save it outside the app, uninstall `0.1.0`, install `0.2.0+`, then import the JSON and verify the restored entries/settings;
- the debug build remains isolated through `com.worktime.app.debug` and does not determine production update identity.

The legacy public certificate fingerprint remains in `release/production-signing-cert-sha256.txt` only until the replacement key is generated. Do not build or publish the `0.2.0` production candidate until that file has been replaced with the new public SHA-256 fingerprint and committed.

## Generate the replacement key once

Generate the private key **locally on a trusted machine**, not in ChatGPT, GitHub Actions, a public CI runner or the repository. The repository includes a helper that deliberately leaves the private key outside Git:

```bash
sh scripts/init_production_signing_key.sh
```

By default it creates:

```text
$HOME/.android/keys/worktime-release-v2.jks
$HOME/.android/keys/worktime-release-v2-certificate.pem
```

and updates only this public repository file:

```text
release/production-signing-cert-sha256.txt
```

The helper uses RSA 4096 and a long-lived certificate, prompts for secrets interactively, refuses to overwrite an existing keystore, exports the public certificate, and pins its SHA-256 fingerprint. The fingerprint is public information; the `.jks` file and passwords are not.

Immediately after generation:

1. Save the keystore password and key password in a password manager.
2. Create at least two encrypted backups of `worktime-release-v2.jks` in separate locations.
3. Verify one backup can be opened with `keytool -list -v`.
4. Commit only the changed `release/production-signing-cert-sha256.txt` file.
5. Never upload the private keystore or passwords to GitHub, RuStore descriptions, issue/PR comments or chat.

## Production signing identity

The public SHA-256 fingerprint of the active production signing certificate is pinned in:

```text
release/production-signing-cert-sha256.txt
```

`build_release_candidate.sh` compares every normal release candidate against that fingerprint and refuses an APK signed by another certificate. `create_github_release.sh` also verifies the candidate metadata against the pinned signer.

CI uses a disposable certificate only in the explicitly isolated `WORKTIME_SIGNING_SMOKE=1` path. That bypass is accepted only when `CI=true`, and its APK is never a production release artifact.

## Build a signed release candidate locally

After the replacement fingerprint is committed, build from the exact clean commit that passed CI:

```bash
export RELEASE_STORE_FILE="$HOME/.android/keys/worktime-release-v2.jks"
export RELEASE_KEY_ALIAS="worktime-release"

read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script refuses a dirty working tree and then:

- runs the repository static audit;
- runs `lintRelease`;
- runs the AGP 9.3 R8 Configuration Analyzer;
- builds the optimized release APK with R8/resource optimization enabled;
- refuses the candidate if `assets/dexopt/baseline.prof` is missing from the final APK;
- verifies the APK with Android `apksigner`;
- checks the signer against the pinned production certificate;
- records the exact commit, version, APK SHA-256, signer SHA-256 and matching R8 mapping.

Distribution outputs are copied to:

```text
app/build/outputs/release-candidate/WorkTime-<version>.apk
app/build/outputs/release-candidate/SHA256SUMS.txt
app/build/outputs/release-candidate/metadata.txt
app/build/outputs/release-candidate/WorkTime-<version>-mapping.txt
```

The keystore and passwords are never part of the release output.

## RuStore first publication

For the first RuStore publication, upload the exact APK produced by the release-candidate process above. From `0.2.0` onward, keep the same new signing certificate for every RuStore update and for any direct GitHub APK release.

Using one certificate across both channels preserves normal Android signature continuity for later versions, provided package name and `versionCode` ordering are also compatible.

## Update continuity from 0.2.0 onward

For every later release:

- increment `versionCode`;
- set the intended `versionName`;
- sign with the same WorkTime `0.2.0+` certificate;
- verify installation over the previous production APK without uninstalling;
- confirm Room/DataStore data and the home-screen widget survive the update.

Changing package name or signing certificate again creates another Android installation identity and must not be done as a routine release step.

## Why the permanent key stays out of GitHub

The WorkTime production key is more sensitive than an ordinary repository secret because Android update continuity depends permanently on it. Keeping it offline reduces the number of systems that can expose the release identity.

GitHub may contain the final signed APK, the public certificate/fingerprint and verification metadata. None of those reveal the private key.
