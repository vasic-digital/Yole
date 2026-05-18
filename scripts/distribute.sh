#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole — Firebase Distribution Script
# ====================================
# Builds the Android APK and distributes it via Firebase App Distribution
# to the tester group configured in .env (FIREBASE_TESTER_GROUP).
#
# Prerequisites:
#   - Firebase CLI installed and authenticated (see .env.example)
#   - .env file with FIREBASE_CLI_TOKEN, FIREBASE_PROJECT_ID,
#     FIREBASE_ANDROID_APP_ID, FIREBASE_TESTER_GROUP
#
# Usage:
#   bash scripts/distribute.sh              # Build + distribute Android APK
#   bash scripts/distribute.sh --debug      # Debug build
#   bash scripts/distribute.sh --release    # Release build (default)
#   bash scripts/distribute.sh --notes "Fixed critical crash"  # Custom release notes
#
# Environment (loaded from .env):
#   FIREBASE_CLI_TOKEN       Firebase CI token (required)
#   FIREBASE_PROJECT_ID      Firebase project ID (default: yole-app)
#   FIREBASE_ANDROID_APP_ID  Firebase Android app ID
#   FIREBASE_TESTER_GROUP    Comma-separated tester emails
#   FIREBASE_WEB_APP_ID      Firebase Web app ID (for future web distribution)
#   FIREBASE_IOS_APP_ID      Firebase iOS app ID (for future iOS distribution)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ---------------------------------------------------------------------------
# Load .env
# ---------------------------------------------------------------------------
# Token + tester list may come from either .env or pre-exported env vars.
# Env vars take precedence so that callers (CI, shell rc) can supply the
# CI token without writing it to disk.
if [ -f "${ROOT_DIR}/.env" ]; then
    # Don't clobber already-exported env vars — only set values .env defines.
    set -a
    # shellcheck disable=SC1091
    source "${ROOT_DIR}/.env"
    set +a
fi

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
# Allow FIREBASE_CLI_TOKEN OR FIREBASE_TOKEN (firebase CLI's own canonical name).
FIREBASE_TOKEN="${FIREBASE_CLI_TOKEN:-${FIREBASE_TOKEN:-}}"
PROJECT_ID="${FIREBASE_PROJECT_ID:-yole-app}"
# Per-variant Firebase app IDs. The DEV variant has applicationIdSuffix ".dev"
# in androidApp/build.gradle.kts, which makes its package name
# `digital.vasic.yole.android.dev` — a DIFFERENT Firebase Android app from
# the production package `digital.vasic.yole.android`. Distributing the
# wrong APK to the wrong app fails with "APK package name does not match"
# (iter-87 forensic case, 2026-05-18).
#
# Defaults are the prod IDs in this project. Override via env vars.
ANDROID_APP_ID_RELEASE="${FIREBASE_ANDROID_APP_ID_RELEASE:-${FIREBASE_ANDROID_APP_ID:-1:578988389676:android:d61715a0a84a42c65d2889}}"
ANDROID_APP_ID_DEBUG="${FIREBASE_ANDROID_APP_ID_DEBUG:-1:578988389676:android:5a3d47a9fb23b6465d2889}"
# Backwards-compat: scripts that still read $ANDROID_APP_ID get the release one.
ANDROID_APP_ID="$ANDROID_APP_ID_RELEASE"
TESTER_GROUP="${FIREBASE_TESTER_GROUP:-}"
BUILD_TYPE="release"
BUILD_BOTH=false
RELEASE_NOTES=""

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --debug)   BUILD_TYPE="debug"; BUILD_BOTH=false; shift ;;
        --release) BUILD_TYPE="release"; BUILD_BOTH=false; shift ;;
        --both)    BUILD_BOTH=true; shift ;;
        --notes)   RELEASE_NOTES="$2"; shift 2 ;;
        --notes=*) RELEASE_NOTES="${1#*=}"; shift ;;
        --help|-h)
            cat <<EOF
usage: distribute.sh [--debug|--release|--both] [--notes <text>]

Builds and distributes the Yole Android app via Firebase App Distribution.

Options:
  --debug      Build debug variant only.
  --release    Build release variant only (default).
  --both       Build AND distribute BOTH debug + release in one run.
  --notes TXT  Custom release notes for testers (per-variant if --both).

Token sources (highest priority first):
  1. FIREBASE_CLI_TOKEN env var (export it; never commit to a file)
  2. FIREBASE_TOKEN env var (firebase CLI's canonical name)
  3. FIREBASE_CLI_TOKEN line in .env (gitignored — safer than shell rc)

Env (loaded from .env if present; env-var wins on conflict):
  FIREBASE_PROJECT_ID      Firebase project ID (default: yole-app)
  FIREBASE_ANDROID_APP_ID  Firebase Android app ID
  FIREBASE_TESTER_GROUP    Comma-separated tester emails

Examples:
  bash scripts/distribute.sh                       # release only
  bash scripts/distribute.sh --both                # both variants
  bash scripts/distribute.sh --debug --notes "WIP" # debug with notes
  FIREBASE_CLI_TOKEN=\$TOK bash scripts/distribute.sh --both
EOF
            exit 0
            ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
if [ -z "$FIREBASE_TOKEN" ]; then
    echo "ERROR: no Firebase CI token available."
    echo "Either export FIREBASE_CLI_TOKEN / FIREBASE_TOKEN, or set FIREBASE_CLI_TOKEN in .env."
    echo "Generate one with: firebase login:ci"
    exit 1
fi

if [ -z "$TESTER_GROUP" ]; then
    echo "ERROR: FIREBASE_TESTER_GROUP not set (neither env nor .env)."
    exit 1
fi

VERSION_NAME=$(grep "versionName" "$ROOT_DIR/androidApp/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
VERSION_CODE=$(grep -E "versionCode\s*=\s*[0-9]+" "$ROOT_DIR/androidApp/build.gradle.kts" | head -1 | sed -E 's/.*=[[:space:]]*([0-9]+).*/\1/')

# Resolve which variants we'll distribute this run.
if [ "$BUILD_BOTH" = true ]; then
    VARIANTS=("debug" "release")
else
    VARIANTS=("$BUILD_TYPE")
fi

echo "============================================="
echo " Yole — Firebase Distribution"
echo "============================================="
echo " Project:    $PROJECT_ID"
echo " App ID:     $ANDROID_APP_ID"
echo " Variants:   ${VARIANTS[*]}"
echo " Version:    ${VERSION_NAME} (${VERSION_CODE})"
echo " Testers:    $TESTER_GROUP"
echo "============================================="

NOTES_FILE="$(mktemp -t yole-release-notes.XXXXXX)"
trap 'rm -f "$NOTES_FILE"' EXIT

# Distribute each requested variant in turn. A failure of one variant
# aborts the run (zero-bluff: a single FAIL must not be masked by a
# parallel PASS).
for variant in "${VARIANTS[@]}"; do
    echo ""
    echo "============================================="
    echo " Variant: $variant"
    echo "============================================="

    # ---- Build ----------------------------------------------------------
    echo "[1/3] Building Android APK ($variant)..."
    cd "$ROOT_DIR"
    if [ "$variant" = "release" ]; then
        ./gradlew :androidApp:assembleRelease --no-daemon 2>&1 | tail -5
        APK_DIR="androidApp/build/outputs/apk/release"
    else
        ./gradlew :androidApp:assembleDebug --no-daemon 2>&1 | tail -5
        APK_DIR="androidApp/build/outputs/apk/debug"
    fi

    APK_FILE=$(find "$APK_DIR" -name "*.apk" -not -name "*unsigned*" 2>/dev/null | head -1)
    if [ -z "$APK_FILE" ]; then
        echo "ERROR: No APK produced for variant '$variant' in $APK_DIR"
        exit 1
    fi
    echo "  APK: $APK_FILE ($(du -h "$APK_FILE" | cut -f1))"

    # ---- Notes ----------------------------------------------------------
    echo "[2/3] Preparing release notes..."
    if [ -n "$RELEASE_NOTES" ]; then
        printf '%s\n' "$RELEASE_NOTES" > "$NOTES_FILE"
    else
        cat > "$NOTES_FILE" <<EOF
Yole Android — $variant build
Build date: $(date -u +"%Y-%m-%d %H:%M UTC")
Version: ${VERSION_NAME} (${VERSION_CODE})
EOF
    fi
    echo "  Notes: $(head -1 "$NOTES_FILE")"

    # ---- Distribute -----------------------------------------------------
    # Pick the right Firebase app ID per variant — DEV variant has its own
    # Firebase app because its applicationIdSuffix ".dev" makes its package
    # name distinct from the release package.
    case "$variant" in
        release) APP_ID="$ANDROID_APP_ID_RELEASE" ;;
        debug)   APP_ID="$ANDROID_APP_ID_DEBUG" ;;
        *)       APP_ID="$ANDROID_APP_ID_RELEASE" ;;
    esac
    echo "[3/3] Distributing via Firebase App Distribution (app: $APP_ID)..."
    firebase appdistribution:distribute "$APK_FILE" \
        --app "$APP_ID" \
        --project "$PROJECT_ID" \
        --token "$FIREBASE_TOKEN" \
        --release-notes-file "$NOTES_FILE" \
        --testers "$TESTER_GROUP" 2>&1 || {
            echo "ERROR: Distribution of '$variant' failed."
            exit 1
        }
done

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
# Notes file removed by EXIT trap.

echo ""
echo "============================================="
echo " Distribution complete: ${VARIANTS[*]}"
echo " Testers will receive an email invitation."
echo "============================================="
