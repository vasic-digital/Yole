#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole — Release keystore generator (idempotent)
# ==============================================
#
# Generates the project's Android release keystore at
# `docker/keys/yole.keystore` if it does not exist. The keystore is
# git-ignored (line 13 of .gitignore: `docker/keys/`).
#
# IMPORTANT — signing-identity continuity:
# Each freshly-generated keystore creates a NEW signing identity. An
# APK signed with keystore-A cannot be installed over an APK signed
# with keystore-B (Android rejects with INSTALL_FAILED_UPDATE_INCOMPATIBLE).
# Consequences:
#   - If the Linux dev host already has `docker/keys/yole.keystore`,
#     copy that file to this host instead of running this script. That
#     preserves the signing identity for the existing tester / Play
#     Store user base.
#   - If you run this script on a fresh host, you are starting a NEW
#     signing identity. Users with an APK signed by any earlier keystore
#     must uninstall before installing builds signed with this new key.
#   - To move TO Play Store (Google Play App Signing), keep the FIRST
#     production keystore safe forever — its public-key fingerprint is
#     registered with Google and is not changeable.
#
# Passwords use the project's existing convention (env-var-first with
# `yole123` as the documented fallback in androidApp/build.gradle.kts).
# To use a non-default password, export YOLE_KEYSTORE_PASSWORD /
# YOLE_KEY_PASSWORD / YOLE_KEY_ALIAS before running, AND set the same
# values in your shell rc / .env file so subsequent builds match.
#
# Usage:
#   bash scripts/generate-keystore.sh                # idempotent — skips if exists
#   bash scripts/generate-keystore.sh --force        # OVERWRITE existing keystore
#                                                    # (creates NEW signing identity)
#   YOLE_KEYSTORE_PASSWORD=hunter2 bash scripts/generate-keystore.sh
#
# After generation:
#   - Verify the SHA-256 fingerprint matches your expectation (printed at end).
#   - The keystore file is at docker/keys/yole.keystore (gitignored).
#   - Subsequent `./gradlew :androidApp:assembleRelease` will sign with it.
#   - `scripts/distribute.sh --release` distributes the properly-signed APK
#     via Firebase App Distribution.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
KEYSTORE_DIR="${ROOT_DIR}/docker/keys"
KEYSTORE_FILE="${KEYSTORE_DIR}/yole.keystore"

FORCE=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        --force) FORCE=true; shift ;;
        --help|-h)
            sed -n '4,40p' "${BASH_SOURCE[0]}"
            exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [[ -f "${KEYSTORE_FILE}" ]] && [[ "${FORCE}" != "true" ]]; then
    echo "OK: keystore already exists at ${KEYSTORE_FILE}"
    echo "    Use --force to regenerate (creates a NEW signing identity)."
    echo ""
    echo "    Current SHA-256 fingerprint:"
    keytool -list -keystore "${KEYSTORE_FILE}" \
        -storepass "${YOLE_KEYSTORE_PASSWORD:-yole123}" \
        -alias "${YOLE_KEY_ALIAS:-yole}" 2>/dev/null \
        | grep -E "SHA(1|256):" || echo "    (could not read — check passwords)"
    exit 0
fi

mkdir -p "${KEYSTORE_DIR}"

# Confirm keytool present (ships with JDK).
if ! command -v keytool >/dev/null 2>&1; then
    echo "FAIL: keytool not on PATH. Install a JDK first (e.g. brew install openjdk@17)." >&2
    exit 1
fi

STORE_PASS="${YOLE_KEYSTORE_PASSWORD:-yole123}"
KEY_PASS="${YOLE_KEY_PASSWORD:-yole123}"
KEY_ALIAS="${YOLE_KEY_ALIAS:-yole}"

echo "Generating new release keystore at ${KEYSTORE_FILE}"
echo "  alias: ${KEY_ALIAS}"
echo "  validity: 25000 days"
echo "  algorithm: RSA-2048"
echo ""

# Note: -dname is non-interactive (avoids keytool prompting). C=RS is the
# ISO-3166-1 alpha-2 code for Serbia matching the project author location.
keytool -genkeypair \
    -keystore "${KEYSTORE_FILE}" \
    -alias "${KEY_ALIAS}" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 25000 \
    -storepass "${STORE_PASS}" \
    -keypass "${KEY_PASS}" \
    -dname "CN=Yole, OU=Vasic Digital, O=Vasic Digital, L=Belgrade, ST=Belgrade, C=RS" \
    -storetype JKS

chmod 600 "${KEYSTORE_FILE}"

echo ""
echo "DONE. Fingerprints:"
keytool -list -keystore "${KEYSTORE_FILE}" \
    -storepass "${STORE_PASS}" \
    -alias "${KEY_ALIAS}" | grep -E "SHA(1|256):"
echo ""
echo "Reminder: this is a NEW signing identity. APKs signed with this"
echo "keystore CANNOT install over APKs signed with a different keystore"
echo "without uninstall-first."
