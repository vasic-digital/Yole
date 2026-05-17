#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-84 anti-bluff challenge — webapp_render_validation.
#
# Closes the recurring CONST-039 failure mode where asset-presence
# gates pass but rendered UI is silently broken.
#
# Hit FOUR times before this gate existed:
#   - iter-71: Android launcher icon shipped broken v1.4.0 → v1.9.0
#   - iter-78: iOS UI placeholder shipped as "feature parity"
#   - iter-80: production source showed "1.0.0" through v1.9.5
#   - iter-83: Web Wasm shipped splash-then-blank at v2.0.0
#
# This challenge actually loads the deployed Yole URL in a headless
# Chromium browser via Puppeteer and verifies:
#   - the Wasm bundle loads
#   - Compose injects a canvas into #yoleCanvas
#   - the canvas has non-zero dimensions
#   - the splash screen hides (proves the app reached steady state)
#   - sampled canvas pixels are ≥ 5% non-blank (proves something rendered)
#   - captures a screenshot to qa-results/iter-84/render-gate.png as positive evidence
#
# Layers:
#   STATIC: source index.html container ID matches Kotlin `viewportContainerId`;
#           index.html includes <script src="yole-web.js"> tag.
#   RUNTIME: render-gate.js Puppeteer script PASSes against the deployed URL
#            (defaulting to https://yole-app.web.app; overridable via
#            $YOLE_WEB_URL for local-bundle / local-dev-server validation).
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift (HTML / Kotlin mismatch)
#   2 = runtime FAIL (render-gate.js returned 2)
#   3 = environmental (Node / Puppeteer missing) — SKIP-OK per CONST-035
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

INDEX_HTML="webApp/src/wasmJsMain/resources/index.html"
MAIN_KT="webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/Main.kt"
RENDER_GATE="tools/node-render-gate/render-gate.js"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

# ---------------- STATIC LAYER ----------------

echo "=== [webapp_render_validation_challenge] static layer ==="

if [[ ! -f "$INDEX_HTML" ]]; then
    echo "[FAIL] $INDEX_HTML missing"
    exit 1
fi

if [[ ! -f "$MAIN_KT" ]]; then
    echo "[FAIL] $MAIN_KT missing"
    exit 1
fi

# Extract container ID from Main.kt (viewportContainerId = "...")
KOTLIN_CONTAINER_ID=$(grep -oE 'viewportContainerId\s*=\s*"[^"]+"' "$MAIN_KT" \
    | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
if [[ -z "$KOTLIN_CONTAINER_ID" ]]; then
    echo "[FAIL] could not extract viewportContainerId from $MAIN_KT (Kotlin code may have changed)"
    exit 1
fi
echo "[OK] Kotlin viewportContainerId = \"$KOTLIN_CONTAINER_ID\""

# Verify index.html has a <div id="..."> matching the Kotlin container ID
if ! grep -qE "<div[^>]+id=\"${KOTLIN_CONTAINER_ID}\"" "$INDEX_HTML"; then
    echo "[FAIL] $INDEX_HTML does NOT contain <div id=\"${KOTLIN_CONTAINER_ID}\">"
    echo "       Kotlin ComposeViewport mounts to that ID; without it the page renders blank."
    echo "       This is the iter-83 v2.0.0 defect class — fix the HTML to match the Kotlin code."
    exit 1
fi
echo "[OK] $INDEX_HTML has <div id=\"${KOTLIN_CONTAINER_ID}\">"

# Verify index.html includes the Wasm bundle script tag
# (KMP Wasm webpack does NOT auto-inject when index.html is in src/wasmJsMain/resources/)
if ! grep -qE "<script[^>]+src=\"yole-web\.js\"" "$INDEX_HTML"; then
    echo "[FAIL] $INDEX_HTML does NOT include <script src=\"yole-web.js\">"
    echo "       Without this tag the Kotlin/Wasm bundle never loads — Compose never mounts — page stays blank."
    echo "       This is the iter-83 v2.0.0 defect class — add the script tag before </body>."
    exit 1
fi
echo "[OK] $INDEX_HTML loads yole-web.js"

# ---------------- RUNTIME LAYER ----------------

echo
echo "=== [webapp_render_validation_challenge] runtime layer ==="

if ! command -v node >/dev/null 2>&1; then
    echo "[SKIP-OK] node not installed; runtime browser-render verification deferred"
    exit 3
fi

if [[ ! -f "$RENDER_GATE" ]]; then
    echo "[SKIP-OK] $RENDER_GATE missing; runtime browser-render verification deferred"
    exit 3
fi

if [[ ! -d "tools/node-render-gate/node_modules/puppeteer" ]]; then
    echo "[SKIP-OK] puppeteer not installed under tools/node-render-gate/node_modules"
    echo "          run: (cd tools/node-render-gate && npm install) to enable runtime layer"
    exit 3
fi

echo "[INFO] target URL: $TARGET_URL"
echo "[INFO] running Puppeteer render gate..."

if node "$RENDER_GATE" "$TARGET_URL"; then
    echo
    echo "PASS: webapp_render_validation_challenge — static + runtime layers both verified."
    echo "      Screenshot evidence: qa-results/iter-84/render-gate.png"
    exit 0
else
    rc=$?
    echo
    echo "FAIL: render gate exited $rc against $TARGET_URL"
    exit $rc
fi
