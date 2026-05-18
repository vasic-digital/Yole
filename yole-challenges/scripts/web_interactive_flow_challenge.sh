#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-86 phase-2 anti-bluff challenge — web_interactive_flow.
#
# Complements iter-84 (render-gate: rendering quality) and iter-85
# (full-ui-suite: initial-state a11y inventory) with FUNCTIONAL CLICKS
# proving each toolbar button actually does something visible. Catches
# "dead buttons" — UI elements that render in the a11y tree (so the
# inventory suite PASSes) but whose onClick handlers are no-op or
# silently broken.
#
# Forensic anchor: this gate exists because the operator repeatedly
# invoked the mandate "execution of tests and Challenges MUST guarantee
# the quality, the completion and full usability by end users of the
# product". An inventory-only check is satisfied by a button that
# RENDERS but does NOTHING when clicked. This gate clicks each one.
#
# Layers:
#   STATIC: confirms the Node suite + Puppeteer are present.
#   RUNTIME: invokes interactive-flow-suite.js against YOLE_WEB_URL
#            (default https://yole-app.web.app; override with
#            YOLE_WEB_URL=http://localhost:18080 for container testing).
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift
#   2 = runtime FAIL (dead button OR no state change on Toggle theme)
#   3 = environmental (Node / Puppeteer missing) — SKIP-OK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

GATE_SCRIPT="tools/node-render-gate/interactive-flow-suite.js"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

echo "=== [web_interactive_flow_challenge] static layer ==="

if [[ ! -f "$GATE_SCRIPT" ]]; then
    echo "[FAIL] $GATE_SCRIPT missing — interactive-flow suite cannot run"
    exit 1
fi
echo "[OK] $GATE_SCRIPT present"

echo
echo "=== [web_interactive_flow_challenge] runtime layer ==="

if ! command -v node >/dev/null 2>&1; then
    echo "[SKIP-OK] node not installed; runtime interactive-flow verification deferred"
    exit 3
fi

if [[ ! -d "tools/node-render-gate/node_modules/puppeteer" ]]; then
    echo "[SKIP-OK] puppeteer not installed under tools/node-render-gate/node_modules"
    exit 3
fi

echo "[INFO] target URL: $TARGET_URL"
echo "[INFO] running Puppeteer interactive-flow suite..."

if node "$GATE_SCRIPT" "$TARGET_URL"; then
    echo
    echo "PASS: web_interactive_flow_challenge — every probed button produced the expected UI delta."
    echo "      Evidence: qa-results/iter-86/interactive-flow/report.json"
    exit 0
else
    rc=$?
    echo
    echo "FAIL: interactive-flow suite exited $rc against $TARGET_URL"
    echo "      See qa-results/iter-86/interactive-flow/report.json for which button(s) failed."
    exit 2
fi
