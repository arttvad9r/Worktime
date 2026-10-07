# Release signing

WorkTime is distributed as a directly installable APK through GitHub Releases. There is no store-managed app-signing layer: the private key used here is the **actual app-signing key** trusted by Android for every installed release.

That key is a long-lived release identity. Every future APK intended to update an existing WorkTime installation must be signed with the same certificate. Losing the private key means existing installations cannot receive a normally installable update signed by a replacement key.

The keystore and passwords must never be committed to this repository, uploaded to GitHub Actions, or pasted into issue/PR logs. The project reads signing inputs from Gradle properties or `RELEASE_*` environment variables and never falls back to debug signing.

## Where the key lives

Everything secret sits in the git-ignored `keystore/` directory at the repository root:

```text
keystore/worktime-release.jks   # production app-signing keystore
keystore/worktime-release.env   # RELEASE_STORE_FILE / _ALIAS / _STORE_PASSWORD / _KEY_PASSWORD
```

On the maintainer machine the env file makes a local build one command (run from the repository root):

```bash
set -a; . keystore/worktime-release.env; set +a
./scripts/build_release_candidate.sh
```

`keystore/` is copied to the encrypted offline backup by `keys-backup`. A new `git worktree` does not contain it; copy the directory in when a release is built from a worktree.

## Production signing identity

The public SHA-256 fingerprint of the production signing certificate is pinned in:

```text
release/production-signing-cert-sha256.txt
```

The fingerprint is public information and is safe to keep in the repository. The private key and passwords are not.

`build_release_candidate.sh` compares every normal release candidate against that pinned fingerprint and refuses a candidate signed by another certificate. `create_github_release.sh` also verifies that the candidate metadata names the pinned production signer before it can create a draft release.

CI uses a disposable certificate only in the explicitly isolated `WORKTIME_SIGNING_SMOKE=1` path. That bypass is accepted only when `CI=true`, and the resulting APK is never a release artifact.

## One-time app-signing key creation

Create the keystore locally on a trusted machine. The command prompts for secrets instead of putting passwords in shell history:

```bash
mkdir -p keystore && chmod 700 keystore
keytool -genkeypair -v \
  -keystore "keystore/worktime-release.jks" \
  -storetype JKS \
  -alias worktime-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Use a unique strong password and keep it in a password manager. Keep at least two encrypted backups of `worktime-release.jks` in separate locations. Do not keep the only copy on the development machine.

Export and archive the public certificate and its SHA-256 fingerprint. The certificate is safe to publish; the private key is not:

```bash
keytool -export -rfc \
  -keystore "keystore/worktime-release.jks" \
  -alias worktime-release \
  -file "keystore/worktime-release-certificate.pem"

keytool -list -v \
  -keystore "keystore/worktime-release.jks" \
  -alias worktime-release
```

Compare the displayed SHA-256 value with `release/production-signing-cert-sha256.txt` before the first real release and after restoring the key from backup.

## Build a signed release candidate locally

Use the exact clean commit that passed CI. Set the path and alias normally; enter passwords interactively:

```bash
export RELEASE_STORE_FILE="$PWD/keystore/worktime-release.jks"
export RELEASE_KEY_ALIAS="worktime-release"

read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script refuses a dirty working tree, runs the static audit and release lint, builds the optimized release APK, verifies it with Android `apksigner`, checks the signer against the pinned production certificate, and records:

- exact Git commit;
- `versionCode` / `versionName`;
- APK SHA-256;
- signer certificate SHA-256;
- matching R8 mapping.

Distribution outputs are copied to:

```text
app/build/outputs/release-candidate/WorkTime-<version>.apk
app/build/outputs/release-candidate/SHA256SUMS.txt
app/build/outputs/release-candidate/metadata.txt
app/build/outputs/release-candidate/WorkTime-<version>-mapping.txt
```

The keystore and passwords are never part of the release output.

## Create a draft GitHub Release

The release helper uses the already-built local candidate. It does not rebuild or resign anything and therefore does not need the signing key.

Prerequisites:

- GitHub CLI (`gh`) is installed and authenticated for this repository;
- the current clean `HEAD` is the tested release commit;
- a tag named `v<versionName>` points to the same commit and has been pushed to `origin`;
- `build_release_candidate.sh` has already produced the candidate files above.

Example:

```bash
git tag -a v0.1.0 -m "WorkTime 0.1.0"
git push origin v0.1.0

./scripts/create_github_release.sh
```

`create_github_release.sh` verifies the candidate metadata, pinned signer and SHA-256, verifies the local and remote tag, refuses to replace an existing release, then creates a **draft GitHub Release** with:

- `WorkTime-<version>.apk`;
- `SHA256SUMS.txt`;
- `metadata.txt`;
- `WorkTime-<version>-mapping.txt`.

The release remains draft intentionally. Download that exact APK from GitHub, install/update it on the target phone, complete the physical-device checklist, verify its checksum and signer fingerprint, then publish the same draft. Do not rebuild a different APK after QA.

## Why the permanent key stays out of GitHub

Normal CI proves the signing plumbing with a disposable key. The real WorkTime key is more sensitive because direct-distribution Android updates depend permanently on its certificate and there is no store-side key reset layer. Keeping the private key offline reduces the number of systems that can expose the release identity.

GitHub hosts only the final signed APK and non-secret verification files. A signed APK and public certificate reveal the public signing identity, not the private signing key.

## Update continuity

For every later release:

- increment `versionCode`;
- set the intended `versionName`;
- sign with the same WorkTime app-signing certificate;
- verify installation over the previous public APK without uninstalling;
- confirm Room/DataStore data and the home-screen widget survive the update.

Changing package name or signing certificate creates a different Android installation identity and is not a normal update path.

## CI signing smoke test

Normal PR/main CI intentionally uses a disposable signing key in `signing-smoke`. It exercises the same release signing configuration, `assembleRelease`, `apksigner`, metadata and checksum path without using the real WorkTime key. The disposable APK is never uploaded or distributed.
