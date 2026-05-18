#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-85 phase-2 anti-bluff challenge — web_full_ui_suite.
#
# Complement to webapp_render_validation_challenge.sh (iter-84):
# that challenge checks RENDERING quality (canvas dimensions, pixel
# variance); this one checks FUNCTIONAL quality (every expected UI
# element is present + reachable via accessibility tree, and no
# forbidden internal content has leaked into the user-visible
# surface).
#
# Forensic anchor: iter-85 surfaced two distinct anti-bluff failures
# in the v2.0.1 web bundle that the iter-84 gate alone could not have
# caught:
#   (a) #yoleCanvas CSS selector mismatch — Compose mounted in a
#       1280x154 strip, leaving 80% of viewport blank. iter-84 gate's
#       byte-count heuristic falsely PASSed.
#   (b) Markdown preview pane rendered the literal CSS stylesheet
#       (`.markdown { font-family: ... }`) instead of the rendered
#       markdown. iter-84 gate had no visibility into preview content.
#
# This challenge runs Puppeteer against a container'd or deployed
# Yole web URL, walks the Compose-generated accessibility tree, and
# asserts BOTH:
#   - every entry in EXPECTED (19 buttons/labels/textboxes) is present
#   - every entry in FORBIDDEN_TEXT (CSS leak patterns) is absent
#
# Layers:
#   STATIC: confirms the Node gate script + Puppeteer deps are present.
#   RUNTIME: invokes the gate against YOLE_WEB_URL (default
#            https://yole-app.web.app, overridable for local
#            container testing via YOLE_WEB_URL=http://localhost:18080).
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift (missing script / deps)
#   2 = runtime FAIL (gate reports missing element or leaked content)
#   3 = environmental (Node / Puppeteer missing) — SKIP-OK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

GATE_SCRIPT="tools/node-render-gate/full-ui-suite.js"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

# ---------------- STATIC LAYER ----------------

echo "=== [web_full_ui_suite_challenge] static layer ==="

if [[ ! -f "$GATE_SCRIPT" ]]; then
    echo "[FAIL] $GATE_SCRIPT missing — full-UI suite cannot run"
    exit 1
fi
echo "[OK] $GATE_SCRIPT present"

# ---------------- RUNTIME LAYER ----------------

echo
echo "=== [web_full_ui_suite_challenge] runtime layer ==="

if ! command -v node >/dev/null 2>&1; then
    echo "[SKIP-OK] node not installed; runtime full-UI verification deferred"
    exit 3
fi

if [[ ! -d "tools/node-render-gate/node_modules/puppeteer" ]]; then
    echo "[SKIP-OK] puppeteer not installed under tools/node-render-gate/node_modules"
    echo "          run: (cd tools/node-render-gate && npm install) to enable runtime layer"
    exit 3
fi

echo "[INFO] target URL: $TARGET_URL"
echo "[INFO] running Puppeteer full-UI suite..."

if node "$GATE_SCRIPT" "$TARGET_URL"; then
    echo
    echo "PASS: web_full_ui_suite_challenge — all expected UI elements present, no forbidden leaks."
    echo "      Evidence: qa-results/iter-85/full-ui-suite/{a11y-tree.json,full-ui.png,report.json}"
    exit 0
else
    rc=$?
    echo
    echo "FAIL: full-UI suite exited $rc against $TARGET_URL"
    echo "      See qa-results/iter-85/full-ui-suite/report.json for details"
    exit 2
fi
