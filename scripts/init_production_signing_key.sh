#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

for tool in keytool sed tr; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required tool is missing: $tool" >&2
    exit 2
  fi
done

KEYSTORE="${1:-$HOME/.android/keys/worktime-release-v2.jks}"
ALIAS="${2:-worktime-release}"
KEY_DIR="$(dirname -- "$KEYSTORE")"
BASE="$(basename -- "$KEYSTORE")"
STEM="${BASE%.*}"
CERT="$KEY_DIR/${STEM}-certificate.pem"
FINGERPRINT_FILE="$ROOT/release/production-signing-cert-sha256.txt"

mkdir -p "$KEY_DIR"

if [ -e "$KEYSTORE" ]; then
  echo "Refusing to overwrite existing keystore: $KEYSTORE" >&2
  exit 2
fi
if [ -e "$CERT" ]; then
  echo "Refusing to overwrite existing certificate: $CERT" >&2
  exit 2
fi

echo "Creating the new WorkTime production signing identity."
echo "Keystore: $KEYSTORE"
echo "Alias:    $ALIAS"
echo
echo "keytool will prompt for the keystore password, owner fields and key password."
echo "Use unique strong passwords and store them in a password manager."
echo

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 36500

echo
keytool -exportcert -rfc \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" \
  -file "$CERT"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT HUP INT TERM

LC_ALL=C keytool \
  -J-Duser.language=en \
  -J-Duser.country=US \
  -list -v \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" > "$TMP"

fingerprint="$(sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' "$TMP" | head -n 1)"
if [ -z "$fingerprint" ]; then
  echo "Could not read SHA-256 fingerprint from keytool output." >&2
  exit 1
fi

normalized="$(printf '%s' "$fingerprint" | tr -d '[:space:]:' | tr '[:lower:]' '[:upper:]')"
case "$normalized" in
  ''|*[!0-9A-F]*)
    echo "Parsed fingerprint is not a valid SHA-256 digest: $fingerprint" >&2
    exit 1
    ;;
esac
if [ "${#normalized}" -ne 64 ]; then
  echo "Parsed fingerprint must contain 64 hex characters: $fingerprint" >&2
  exit 1
fi

pretty="$(printf '%s' "$normalized" | sed 's/../&:/g; s/:$//')"
printf '%s\n' "$pretty" > "$FINGERPRINT_FILE"

echo
echo "New signing identity created."
echo "Private keystore:   $KEYSTORE"
echo "Public certificate: $CERT"
echo "SHA-256:            $pretty"
echo "Pinned fingerprint: $FINGERPRINT_FILE"
echo
echo "Next steps:"
echo "  1. Make at least two encrypted backups of the .jks file in separate locations."
echo "  2. Verify one backup with keytool before deleting anything."
echo "  3. Commit ONLY release/production-signing-cert-sha256.txt."
echo "  4. Never commit or upload the .jks file or passwords."
