# Release signing

WorkTime is distributed as a directly installable APK. Android update continuity therefore depends on the long-lived production signing certificate.

The private keystore and passwords must never be committed. The application reads release signing inputs from Gradle properties or these environment variables:

```text
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

The public expected signer fingerprint is pinned in `release/production-signing-cert-sha256.txt`.

## Build the candidate

Use the exact clean commit that passed all intended CI gates:

```bash
export RELEASE_STORE_FILE="$HOME/.android/keys/worktime-release.jks"
export RELEASE_KEY_ALIAS="worktime-release"
read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script runs the rewrite static audit and release lint/build, verifies the signed APK with `apksigner`, checks the certificate fingerprint, and records the commit, version, APK SHA-256 and R8 mapping under `app/build/outputs/release-candidate/`.

Normal CI never uses the production private key. Its `signing-smoke` job creates a disposable key and enables the isolated `WORKTIME_SIGNING_SMOKE=1` path only with `CI=true`; that APK is proof of signing plumbing, not a distributable release.

## Update continuity

A rewrite release intended to replace the existing public WorkTime installation must:

- keep `applicationId = com.worktime.app`;
- increment `versionCode`;
- use the same production signing certificate;
- explicitly resolve and test the data policy, because the rewrite currently uses the separate `worktime-modern.db` database;
- install the exact signed candidate over the latest public APK during final QA.

Do not claim data continuity merely because package name and certificate match: update installation identity and database migration are separate concerns.

## Publication

`./scripts/create_github_release.sh` may be used only after the candidate has passed final device QA. Tag the exact tested commit with the matching `v<versionName>`, create a draft release, verify downloaded checksum/signer again, then publish that same artifact. Do not rebuild a different APK after QA.
