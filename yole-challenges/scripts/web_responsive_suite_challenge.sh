#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-90 anti-bluff challenge — web_responsive_suite.
#
# Re-runs the inventory + structure assertions at 6 viewport sizes
# spanning real device classes (320 → 1280 px wide), asserting both:
#   - critical UI elements (YOLE logo + 3 toolbar buttons + Code editor)
#     remain reachable at every size
#   - the responsive-layout breakpoint contract is honored (sidebar /
#     preview visible at the expected widths only)
#
# Forensic anchor: operator probe 2026-05-18 reported the mobile
# experience was "the three-column desktop layout crammed into 320 px"
# while every other web gate was PASSing — they all ran at 1280x800.
# This gate makes the gate-coverage match the user-coverage.
#
# Layers:
#   STATIC: confirms the Node suite + Puppeteer are present.
#   RUNTIME: invokes responsive-suite.js against YOLE_WEB_URL.
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift (missing script)
#   2 = runtime FAIL (some viewport's assertions failed)
#   3 = environmental (Node / Puppeteer missing) — SKIP-OK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

GATE_SCRIPT="tools/node-render-gate/responsive-suite.js"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

echo "=== [web_responsive_suite_challenge] static layer ==="
if [[ ! -f "$GATE_SCRIPT" ]]; then
    echo "[FAIL] $GATE_SCRIPT missing"
    exit 1
fi
echo "[OK] $GATE_SCRIPT present"

echo
echo "=== [web_responsive_suite_challenge] runtime layer ==="
if ! command -v node >/dev/null 2>&1; then
    echo "[SKIP-OK] node not installed"
    exit 3
fi
if [[ ! -d "tools/node-render-gate/node_modules/puppeteer" ]]; then
    echo "[SKIP-OK] puppeteer not installed"
    exit 3
fi

echo "[INFO] target URL: $TARGET_URL"
if node "$GATE_SCRIPT" "$TARGET_URL"; then
    echo
    echo "PASS: web_responsive_suite_challenge — every viewport's a11y + breakpoint contract verified."
    exit 0
else
    rc=$?
    echo
    echo "FAIL: responsive suite exited $rc against $TARGET_URL"
    exit 2
fi
