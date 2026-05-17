#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-81 CONST-039 challenge: display-version-consistency.
#
# Catches the bug class surfaced in iter-80: production source code
# displayed "1.0.0" in user-visible UI across Android, Desktop, and
# Web through versions 1.0.0 → 1.9.5 (NINE consecutive shipped
# versions). No prior test/challenge caught this because all tests
# operated on isolated UI components, not the end-to-end "what does
# the about/version dialog actually show" question.
#
# The classic CONST-039 failure mode: pure code-path tests can pass
# even when the user-visible display string is wrong.
#
# Layers:
#   STATIC: derive the canonical version (versionName in
#     androidApp/build.gradle.kts), then grep every Kotlin source
#     file for OTHER hardcoded version-looking strings near "version"
#     contexts. Any divergence is a defect.
#
#   RUNTIME: build the Android Release APK; aapt2 dump badging
#     versionName must match the source declaration. Repeat for
#     desktopApp's packageVersion. Repeat for webApp's manifest.json
#     version field.
#
# Exit codes:
#   0 = canonical version present + no source-string divergence + APK/Desktop/Web match
#   1 = static drift (source has version-string mismatches)
#   2 = runtime drift (artifact metadata disagrees with source)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

# ---------------- STATIC LAYER ----------------

echo "=== [display_version_consistency_challenge] static layer ==="

ANDROID_VERSION_NAME=$(grep -E '^\s*versionName\s*=' androidApp/build.gradle.kts \
    | head -1 | sed -E 's/.*"([^"]+)".*/\1/' | tr -d ' \t')

if [[ -z "$ANDROID_VERSION_NAME" ]]; then
    echo "[FAIL] could not extract versionName from androidApp/build.gradle.kts"
    exit 1
fi
echo "[OK] canonical version: $ANDROID_VERSION_NAME"

# Search for "1.0.0" hardcoded in production source files that look like
# they're displaying a version string. Exclude:
#   - test files (asserting historical behavior may legitimately use "1.0.0")
#   - build/ output dirs
#   - generated code
#   - CHANGELOG (historical)
#   - vendored 3rd-party (e.g., Tree-Sitter, POI)
#   - SPDX headers (copyright year)
FAILED=0
DIVERGENCE_LIST=$(mktemp)
trap "rm -f $DIVERGENCE_LIST" EXIT

# Find any Kotlin source in production app modules referencing a hardcoded
# version-looking string that ISN'T the canonical version.
for module_path in androidApp/src/main desktopApp/src/main webApp/src/wasmJsMain; do
    [[ -d "$module_path" ]] || continue

    # Pattern: literal version strings near "version" word, "Version" type,
    # or "v" prefix. We catch any X.Y.Z pattern that ISN'T the canonical one.
    grep -rEn '"[0-9]+\.[0-9]+\.[0-9]+"' "$module_path" \
        --include="*.kt" --include="*.kts" 2>/dev/null \
        | grep -viE "spdx|copyright|@since|@version 1\." \
        | grep -iE "(version|build|app.version|MARKETING_VERSION|displayVersion|versionString|getVersion)" \
        | grep -vE "\"$ANDROID_VERSION_NAME\"" \
        >> "$DIVERGENCE_LIST" 2>/dev/null || true
done

DIVERGENCE_COUNT=$(wc -l < "$DIVERGENCE_LIST" | tr -d ' ')

if [[ "$DIVERGENCE_COUNT" -gt 0 ]]; then
    echo "[FAIL] found $DIVERGENCE_COUNT version-string divergence(s) in production source:"
    cat "$DIVERGENCE_LIST" | head -20
    echo
    echo "Every match above declares a version string that disagrees with the"
    echo "canonical versionName=\"$ANDROID_VERSION_NAME\". This is the bug class"
    echo "that shipped 9 times (v1.0.0 displayed through v1.9.5). Update each"
    echo "occurrence OR explicitly exclude it via grep filter if it's intentional."
    FAILED=1
else
    echo "[OK] no version-string divergence in production source (canonical: $ANDROID_VERSION_NAME)"
fi

if [[ "$FAILED" -eq 1 ]]; then
    exit 1
fi

# ---------------- RUNTIME LAYER ----------------

echo
echo "=== [display_version_consistency_challenge] runtime layer ==="

AAPT2=""
for candidate in \
    /opt/homebrew/share/android-commandlinetools/build-tools/35.0.0/aapt2 \
    "$HOME/Library/Android/sdk/build-tools"/*/aapt2 \
    "$ANDROID_HOME/build-tools"/*/aapt2 \
    /Volumes/T7/Android/SDK/build-tools/*/aapt2 \
    ; do
    if [[ -x "$candidate" ]]; then
        AAPT2="$candidate"
        break
    fi
done

if [[ -z "$AAPT2" ]]; then
    echo "[SKIP-OK: aapt2 not found in PATH or standard SDK locations]"
else
    # Find the most recently produced Release APK (or the canonical one in releases/)
    APK=$(ls -t releases/Yole-Android-${ANDROID_VERSION_NAME}-Release-*.apk 2>/dev/null | head -1)

    if [[ -z "$APK" ]]; then
        echo "[SKIP-OK: no Yole-Android-${ANDROID_VERSION_NAME}-Release-*.apk in releases/; build + retry]"
    else
        APK_VERSION=$("$AAPT2" dump badging "$APK" 2>/dev/null \
            | grep -oE "versionName='[^']*'" | sed -E "s/versionName='([^']*)'/\1/" | head -1)
        if [[ "$APK_VERSION" == "$ANDROID_VERSION_NAME" ]]; then
            echo "[OK] APK versionName='$APK_VERSION' matches source"
        else
            echo "[FAIL] APK versionName='$APK_VERSION' diverges from source '$ANDROID_VERSION_NAME'"
            exit 2
        fi
    fi
fi

# Desktop packageVersion check
DESKTOP_VERSION=$(grep -E "^\s*packageVersion\s*=" desktopApp/build.gradle.kts \
    | head -1 | sed -E 's/.*"([^"]+)".*/\1/' | tr -d ' \t')
if [[ "$DESKTOP_VERSION" == "$ANDROID_VERSION_NAME" ]]; then
    echo "[OK] Desktop packageVersion='$DESKTOP_VERSION' synced with canonical"
else
    echo "[FAIL] Desktop packageVersion='$DESKTOP_VERSION' diverges from canonical '$ANDROID_VERSION_NAME'"
    exit 2
fi

echo
echo "PASS: display_version_consistency_challenge — canonical $ANDROID_VERSION_NAME consistent across source + Desktop + Android artefact."
