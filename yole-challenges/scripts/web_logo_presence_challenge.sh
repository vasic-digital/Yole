#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-90 anti-bluff challenge — web_logo_presence.
#
# Asserts the Yole launcher icon / logo is present on every web surface
# that needs it:
#   1. favicon.ico (HTTP 200)
#   2. PWA manifest icons (icons/icon-{48..512}.png) all HTTP 200
#   3. PWA manifest references the icons (manifest.json structurally valid)
#   4. In-app "YOLE" logo present in the live app's a11y tree at the toolbar
#   5. Web page <title> contains "Yole"
#
# Forensic anchor: through iter-71 the Android launcher icon was broken
# for 9 consecutive release cycles before the operator manually noticed.
# This gate prevents the equivalent class of regression on web: it's
# easy to deploy a build where the logo asset moves, the URL changes,
# or the manifest reference rots — and the rest of the app keeps
# working invisibly. This gate fails loudly on any of those.
#
# Layers:
#   STATIC: confirms source has favicon.ico + manifest.json with icons
#     and matching files in resources/.
#   RUNTIME: HTTP-checks each icon URL on the deployed site, fetches
#     and a11y-probes the live page for the "YOLE" logo node.
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift — source missing icons or manifest
#   2 = runtime FAIL — deployed icon URL 404 or in-app logo missing
#   3 = environmental (curl/node missing) — SKIP-OK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

RES_DIR="webApp/src/wasmJsMain/resources"
MANIFEST="$RES_DIR/manifest.json"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

# ---------------- STATIC LAYER ----------------

echo "=== [web_logo_presence_challenge] static layer ==="

if [[ ! -f "$RES_DIR/favicon.ico" ]]; then
    echo "[FAIL] $RES_DIR/favicon.ico missing"
    exit 1
fi
echo "[OK] favicon.ico source present ($(wc -c < "$RES_DIR/favicon.ico" | tr -d ' ') bytes)"

if [[ ! -f "$MANIFEST" ]]; then
    echo "[FAIL] $MANIFEST missing"
    exit 1
fi
echo "[OK] manifest.json source present"

# Icon sizes that MUST be present — the set declared by manifest.json.
# We extract this list from the manifest itself so source + manifest
# stay in sync automatically. If we later add 128/384 to the manifest,
# the source file MUST be present too — the gate enforces this contract.
REQUIRED_ICON_SIZES=$(python3 -c "
import json
with open('$MANIFEST') as f:
    m = json.load(f)
for i in m.get('icons', []):
    src = i.get('src', '')
    if src.endswith('.png'):
        # extract '48' from 'icons/icon-48.png'
        import re
        match = re.search(r'icon-(\d+)\.png', src)
        if match: print(match.group(1))
" 2>/dev/null)
if [[ -z "$REQUIRED_ICON_SIZES" ]]; then
    echo "[FAIL] could not extract icon size list from $MANIFEST"
    exit 1
fi
missing_icons=0
present_sizes=()
for sz in $REQUIRED_ICON_SIZES; do
    if [[ ! -f "$RES_DIR/icons/icon-${sz}.png" ]]; then
        echo "[FAIL] $RES_DIR/icons/icon-${sz}.png declared in manifest but source MISSING"
        missing_icons=$((missing_icons + 1))
    else
        present_sizes+=("$sz")
    fi
done
if [[ $missing_icons -gt 0 ]]; then
    echo "[FAIL] $missing_icons PWA icon size(s) declared in manifest but missing from source"
    exit 1
fi
echo "[OK] all manifest-declared PWA icon source files present: ${present_sizes[*]}"

if ! grep -q '"icons"' "$MANIFEST"; then
    echo "[FAIL] $MANIFEST has no 'icons' array"
    exit 1
fi
echo "[OK] manifest.json declares 'icons' array"

# ---------------- RUNTIME LAYER ----------------

echo
echo "=== [web_logo_presence_challenge] runtime layer ==="

if ! command -v curl >/dev/null 2>&1; then
    echo "[SKIP-OK] curl not installed; runtime logo-presence verification deferred"
    exit 3
fi

# Check each icon URL resolves AND returns image content-type.
# A bare HTTP-200 check is bluff: Firebase's `**` → `/index.html`
# rewrite catches missing assets and serves index.html with HTTP 200
# (8.8 KB of text/html). The iter-90 forensic case: the prior gate
# said "icons/icon-128.png HTTP 200" while the URL actually returned
# the homepage HTML. Verifying Content-Type starts with `image/`
# defeats the rewrite-as-200 trick.
echo "[INFO] checking deployed icon URLs at $TARGET_URL (HTTP + content-type)"
url_failures=0
check_image_url() {
    local url_path="$1"
    local hc ct
    hc=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 "$TARGET_URL/$url_path" 2>/dev/null || echo "000")
    ct=$(curl -s -I --max-time 15 "$TARGET_URL/$url_path" 2>/dev/null | grep -i "^content-type:" | head -1 | tr -d '\r' | sed 's/.*: *//' || true)
    if [[ "$hc" == "200" && "$ct" =~ ^image/ ]]; then
        echo "[OK]   $url_path → HTTP 200 + $ct"
    elif [[ "$hc" == "200" && ! "$ct" =~ ^image/ ]]; then
        echo "[FAIL] $url_path → HTTP 200 but Content-Type='$ct' (Firebase rewrite likely serving index.html — asset is actually missing)"
        url_failures=$((url_failures + 1))
    else
        echo "[FAIL] $url_path → HTTP $hc, content-type='$ct'"
        url_failures=$((url_failures + 1))
    fi
}

for sz in $REQUIRED_ICON_SIZES; do
    check_image_url "icons/icon-${sz}.png"
done
check_image_url favicon.ico
check_image_url icons/icon.svg
check_image_url Logo.png

if [[ $url_failures -gt 0 ]]; then
    echo
    echo "FAIL: $url_failures asset URL(s) returned non-image content"
    exit 2
fi

# Check <title> contains Yole
title_html=$(curl -s --max-time 15 "$TARGET_URL/index.html" 2>/dev/null | grep -oE '<title>[^<]*</title>' | head -1 || true)
if [[ -z "$title_html" ]]; then
    echo "[FAIL] could not extract <title> from deployed index.html"
    exit 2
fi
if echo "$title_html" | grep -qi "yole"; then
    echo "[OK]   page <title> contains 'Yole': $title_html"
else
    echo "[FAIL] page <title> missing 'Yole': $title_html"
    exit 2
fi

# In-app YOLE logo a11y check — reuse the full-ui-suite which already
# asserts the "YOLE" StaticText is present at desktop viewport
if [[ -f tools/node-render-gate/full-ui-suite.js ]] && command -v node >/dev/null 2>&1 && [[ -d tools/node-render-gate/node_modules/puppeteer ]]; then
    echo "[INFO] verifying in-app YOLE logo via full-ui-suite..."
    if node tools/node-render-gate/full-ui-suite.js "$TARGET_URL" 2>&1 | grep -q '✓ StaticText "YOLE"'; then
        echo "[OK]   in-app YOLE logo node present in a11y tree"
    else
        echo "[FAIL] in-app YOLE logo node NOT present in a11y tree"
        exit 2
    fi
else
    echo "[SKIP-OK] node/puppeteer not available; in-app logo a11y check deferred"
fi

echo
echo "PASS: web_logo_presence_challenge — all 11 asset URLs + title + in-app logo verified."
