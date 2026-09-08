#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

: "${RELEASE_STORE_FILE:?Set RELEASE_STORE_FILE to the app-signing keystore path}"
: "${RELEASE_STORE_PASSWORD:?Set RELEASE_STORE_PASSWORD}"
: "${RELEASE_KEY_ALIAS:?Set RELEASE_KEY_ALIAS}"
: "${RELEASE_KEY_PASSWORD:?Set RELEASE_KEY_PASSWORD}"

if [ ! -f "$RELEASE_STORE_FILE" ]; then
  echo "Release keystore not found: $RELEASE_STORE_FILE" >&2
  exit 2
fi

for tool in git python3 keytool unzip; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required tool is missing: $tool" >&2
    exit 2
  fi
done

if [ ! -x ./gradlew ]; then
  echo "Gradle Wrapper is missing or not executable." >&2
  exit 2
fi

if [ -n "$(git status --porcelain --untracked-files=normal)" ]; then
  echo "Refusing to build a release candidate from a dirty working tree." >&2
  exit 2
fi

find_apksigner() {
  if command -v apksigner >/dev/null 2>&1; then
    command -v apksigner
    return
  fi

  for sdk_root in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
    [ -n "$sdk_root" ] || continue
    candidate="$sdk_root/build-tools/37.0.0/apksigner"
    if [ -x "$candidate" ]; then
      printf '%s\n' "$candidate"
      return
    fi
  done

  return 1
}

normalize_sha256() {
  printf '%s' "$1" \
    | tr -d '[:space:]:' \
    | tr '[:upper:]' '[:lower:]'
}

APKSIGNER="$(find_apksigner || true)"
if [ -z "$APKSIGNER" ]; then
  echo "apksigner was not found. Install Android Build Tools 37.0.0 or add apksigner to PATH." >&2
  exit 2
fi

SIGNER_FINGERPRINT_FILE="release/production-signing-cert-sha256.txt"
if [ ! -s "$SIGNER_FINGERPRINT_FILE" ]; then
  echo "Pinned production signing fingerprint is missing: $SIGNER_FINGERPRINT_FILE" >&2
  exit 2
fi
expected_signer_sha256="$(normalize_sha256 "$(cat "$SIGNER_FINGERPRINT_FILE")")"
case "$expected_signer_sha256" in
  ''|*[!0-9a-f]*)
    echo "Pinned production signing fingerprint is not a valid SHA-256 digest." >&2
    exit 2
    ;;
esac
if [ "${#expected_signer_sha256}" -ne 64 ]; then
  echo "Pinned production signing fingerprint must contain exactly 64 hex characters." >&2
  exit 2
fi

python3 scripts/static_audit.py

./gradlew --no-daemon \
  :app:lintRelease \
  :app:analyzeReleaseR8Config \
  :app:assembleRelease \
  --stacktrace

APK="app/build/outputs/apk/release/app-release.apk"
MAPPING="app/build/outputs/mapping/release/mapping.txt"
METADATA_DIR="app/build/outputs/release-candidate"
METADATA="$METADATA_DIR/metadata.txt"
CHECKSUMS="$METADATA_DIR/SHA256SUMS.txt"

if [ ! -s "$APK" ]; then
  echo "Signed release APK was not produced: $APK" >&2
  exit 1
fi
if [ ! -s "$MAPPING" ]; then
  echo "R8 mapping was not produced: $MAPPING" >&2
  exit 1
fi

if ! unzip -l "$APK" | grep -F 'assets/dexopt/baseline.prof' >/dev/null; then
  echo "Release APK does not contain assets/dexopt/baseline.prof." >&2
  echo "The production Baseline Profile is not packaged; refusing release candidate." >&2
  exit 1
fi

if ! "$APKSIGNER" verify --verbose --print-certs "$APK"; then
  echo "Release APK signature verification failed." >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  apk_sha256="$(sha256sum "$APK" | awk '{print $1}')"
elif command -v shasum >/dev/null 2>&1; then
  apk_sha256="$(shasum -a 256 "$APK" | awk '{print $1}')"
else
  echo "Neither sha256sum nor shasum is available." >&2
  exit 2
fi

signer_sha256="$(
  LC_ALL=C "$APKSIGNER" verify --print-certs "$APK" 2>/dev/null \
    | sed -n 's/.*certificate SHA-256 digest: *//p' \
    | head -n 1
)"
if [ -z "$signer_sha256" ]; then
  echo "Could not read the signer SHA-256 fingerprint from the release APK." >&2
  exit 1
fi
signer_sha256="$(normalize_sha256 "$signer_sha256")"

if [ "${WORKTIME_SIGNING_SMOKE:-0}" = "1" ]; then
  if [ "${CI:-}" != "true" ]; then
    echo "WORKTIME_SIGNING_SMOKE is reserved for CI signing smoke tests." >&2
    exit 2
  fi
else
  if [ "$signer_sha256" != "$expected_signer_sha256" ]; then
    echo "Release APK is signed by the wrong certificate." >&2
    echo "Expected SHA-256: $expected_signer_sha256" >&2
    echo "Actual SHA-256:   $signer_sha256" >&2
    exit 1
  fi
fi

commit="$(git rev-parse HEAD)"
version_code="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
version_name="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"

if [ -z "$version_code" ] || [ -z "$version_name" ]; then
  echo "Could not read versionCode/versionName from app/build.gradle.kts." >&2
  exit 1
fi

mkdir -p "$METADATA_DIR"
DIST_APK="$METADATA_DIR/WorkTime-$version_name.apk"
DIST_MAPPING="$METADATA_DIR/WorkTime-$version_name-mapping.txt"
cp "$APK" "$DIST_APK"
cp "$MAPPING" "$DIST_MAPPING"

cat > "$CHECKSUMS" <<EOF
$apk_sha256  WorkTime-$version_name.apk
EOF

cat > "$METADATA" <<EOF
commit=$commit
versionCode=$version_code
versionName=$version_name
apk=$DIST_APK
apkSha256=$apk_sha256
signerSha256=$signer_sha256
mapping=$DIST_MAPPING
baselineProfile=assets/dexopt/baseline.prof
EOF

printf 'Release candidate built and verified.\n'
printf 'Commit: %s\n' "$commit"
printf 'Version: %s (%s)\n' "$version_name" "$version_code"
printf 'APK: %s\n' "$DIST_APK"
printf 'APK SHA-256: %s\n' "$apk_sha256"
printf 'Signer SHA-256: %s\n' "$signer_sha256"
printf 'Baseline Profile: packaged\n'
printf 'R8 mapping: %s\n' "$DIST_MAPPING"
printf 'Checksums: %s\n' "$CHECKSUMS"
printf 'Metadata: %s\n' "$METADATA"
