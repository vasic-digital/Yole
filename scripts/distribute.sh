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
if [ -f "${ROOT_DIR}/.env" ]; then
    set -a
    source "${ROOT_DIR}/.env"
    set +a
else
    echo "ERROR: .env file not found at ${ROOT_DIR}/.env"
    echo "Copy .env.example to .env and fill in your Firebase credentials."
    exit 1
fi

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
FIREBASE_TOKEN="${FIREBASE_CLI_TOKEN:-}"
PROJECT_ID="${FIREBASE_PROJECT_ID:-yole-app}"
ANDROID_APP_ID="${FIREBASE_ANDROID_APP_ID:-1:578988389676:android:d61715a0a84a42c65d2889}"
TESTER_GROUP="${FIREBASE_TESTER_GROUP:-}"
BUILD_TYPE="release"
RELEASE_NOTES=""

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --debug)   BUILD_TYPE="debug"; shift ;;
        --release) BUILD_TYPE="release"; shift ;;
        --notes)   RELEASE_NOTES="$2"; shift 2 ;;
        --notes=*) RELEASE_NOTES="${1#*=}"; shift ;;
        --help|-h)
            echo "usage: distribute.sh [--debug|--release] [--notes <text>]"
            echo ""
            echo "Builds and distributes the Yole Android app via Firebase."
            echo ""
            echo "Options:"
            echo "  --debug      Build debug variant"
            echo "  --release    Build release variant (default)"
            echo "  --notes TEXT Custom release notes for testers"
            echo ""
            echo "Environment (loaded from .env):"
            echo "  FIREBASE_CLI_TOKEN       Firebase CI token (required)"
            echo "  FIREBASE_PROJECT_ID      Firebase project ID"
            echo "  FIREBASE_ANDROID_APP_ID  Firebase Android app ID"
            echo "  FIREBASE_TESTER_GROUP    Comma-separated tester emails"
            exit 0
            ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
if [ -z "$FIREBASE_TOKEN" ]; then
    echo "ERROR: FIREBASE_CLI_TOKEN is not set in .env"
    echo "Generate one with: firebase login:ci"
    exit 1
fi

if [ -z "$TESTER_GROUP" ]; then
    echo "ERROR: FIREBASE_TESTER_GROUP is not set in .env"
    exit 1
fi

echo "============================================="
echo " Yole — Firebase Distribution"
echo "============================================="
echo " Project:    $PROJECT_ID"
echo " App ID:     $ANDROID_APP_ID"
echo " Build:      $BUILD_TYPE"
echo " Testers:    $TESTER_GROUP"
echo "============================================="

# ---------------------------------------------------------------------------
# Build APK
# ---------------------------------------------------------------------------
echo ""
echo "[1/3] Building Android APK ($BUILD_TYPE)..."

cd "$ROOT_DIR"

if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew :androidApp:assembleRelease --no-daemon 2>&1 | tail -5
    APK_DIR="androidApp/build/outputs/apk/release"
    APK_PATTERN="*.apk"
else
    ./gradlew :androidApp:assembleDebug --no-daemon 2>&1 | tail -5
    APK_DIR="androidApp/build/outputs/apk/debug"
    APK_PATTERN="*.apk"
fi

if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi

APK_FILE=$(find "$APK_DIR" -name "$APK_PATTERN" -not -name "*unsigned*" 2>/dev/null | head -1)
if [ -z "$APK_FILE" ]; then
    echo "ERROR: No APK found in $APK_DIR"
    exit 1
fi

echo "  APK: $APK_FILE ($(du -h "$APK_FILE" | cut -f1))"

# ---------------------------------------------------------------------------
# Generate release notes
# ---------------------------------------------------------------------------
echo ""
echo "[2/3] Preparing release notes..."

if [ -z "$RELEASE_NOTES" ]; then
    RELEASE_NOTES=$(cat <<EOF
Yole Android ${BUILD_TYPE} build
Build date: $(date -u +"%Y-%m-%d %H:%M UTC")
Version: $(grep "versionName" "$ROOT_DIR/androidApp/build.gradle.kts" | grep -oP '"\K[^"]+')
Version code: $(grep "versionCode" "$ROOT_DIR/androidApp/build.gradle.kts" | grep -oP '=\s*\K\d+')
EOF
)
fi

echo "$RELEASE_NOTES" > /tmp/yole-release-notes.txt
echo "  Notes: $(head -1 /tmp/yole-release-notes.txt)"

# ---------------------------------------------------------------------------
# Distribute via Firebase
# ---------------------------------------------------------------------------
echo ""
echo "[3/3] Distributing via Firebase App Distribution..."

firebase appdistribution:distribute "$APK_FILE" \
    --app "$ANDROID_APP_ID" \
    --project "$PROJECT_ID" \
    --token "$FIREBASE_TOKEN" \
    --release-notes-file /tmp/yole-release-notes.txt \
    --testers "$TESTER_GROUP" 2>&1 || {
        echo "ERROR: Distribution failed"
        rm -f /tmp/yole-release-notes.txt
        exit 1
    }

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
rm -f /tmp/yole-release-notes.txt

echo ""
echo "============================================="
echo " Distribution complete!"
echo " Testers will receive an email invitation."
echo "============================================="
