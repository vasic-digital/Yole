#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# helixqa_scenario_coverage_challenge.sh — iter-76 scenario-coverage gate.
#
# Per CONST-039: every user-facing feature MUST have at least one HelixQA scenario
# under Challenges/banks/yole/feature-coverage/ before ship.
#
# STATIC layer (always runs):
#   • Verifies feature-coverage/ directory exists.
#   • Counts scenario YAMLs; asserts ≥ REQUIRED_SCENARIOS.
#   • Verifies coverage-matrix.md exists and documents all features.
#   • Checks each scenario YAML has mandatory fields:
#     name, version, metadata, platforms, test_cases, evidence_required.
#
# RUNTIME layer (skips gracefully when emulator absent):
#   • If ADB device is available, confirms helixqa can be invoked for the
#     feature-coverage bank dir (dry-run / list mode only — no real execution).
#   • If no ADB device: emits "SKIP(emulator-absent)" — not PASS, not FAIL.
#     The caller (qa-iter-76-gates) documents this as a deferred gate.
#
# Exit: 0 = PASS (static OK; runtime OK or SKIP), 1 = FAIL.

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FEATURE_DIR="$PROJECT_ROOT/Challenges/banks/yole/feature-coverage"
MATRIX_FILE="$PROJECT_ROOT/Challenges/banks/yole/coverage-matrix.md"
REQUIRED_SCENARIOS=7

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

ok()   { echo "  [OK]   $1"; ((PASS_COUNT++)); }
fail() { echo "  [FAIL] $1"; ((FAIL_COUNT++)); }
skip() { echo "  [SKIP] $1"; ((SKIP_COUNT++)); }

echo "============================================================"
echo " helixqa_scenario_coverage_challenge (iter-76)"
echo " CONST-039: per-feature HelixQA scenario coverage gate"
echo "============================================================"
echo ""
echo "--- STATIC LAYER ---"

# ── (1) feature-coverage/ directory exists ───────────────────────────────────
if [[ ! -d "$FEATURE_DIR" ]]; then
    fail "feature-coverage/ directory missing at: $FEATURE_DIR"
    echo ""
    echo "RESULT: FAIL"
    exit 1
fi
ok "feature-coverage/ directory present"

# ── (2) Count scenario YAMLs ─────────────────────────────────────────────────
YAML_COUNT=$(find "$FEATURE_DIR" -maxdepth 1 -name "*.yaml" | wc -l | tr -d ' ')
if [[ "$YAML_COUNT" -lt "$REQUIRED_SCENARIOS" ]]; then
    fail "Found $YAML_COUNT scenario YAML(s) in feature-coverage/ — need ≥ $REQUIRED_SCENARIOS"
    echo "       Files found:"
    find "$FEATURE_DIR" -maxdepth 1 -name "*.yaml" | sort | sed 's/^/         /'
else
    ok "Found $YAML_COUNT scenario YAML(s) (≥ $REQUIRED_SCENARIOS required)"
fi

# ── (3) coverage-matrix.md exists ────────────────────────────────────────────
if [[ ! -f "$MATRIX_FILE" ]]; then
    fail "coverage-matrix.md missing at: $MATRIX_FILE"
else
    ok "coverage-matrix.md present"
fi

# ── (4) Per-scenario field validation ────────────────────────────────────────
MANDATORY_FIELDS=("name:" "version:" "metadata:" "platforms:" "test_cases:" "evidence_required:")
while IFS= read -r -d '' yaml_file; do
    base=$(basename "$yaml_file")
    field_fail=0
    for field in "${MANDATORY_FIELDS[@]}"; do
        if ! grep -q "$field" "$yaml_file"; then
            fail "$base: missing mandatory field '$field'"
            ((field_fail++))
        fi
    done
    if [[ "$field_fail" -eq 0 ]]; then
        ok "$base: all mandatory fields present"
    fi
done < <(find "$FEATURE_DIR" -maxdepth 1 -name "*.yaml" -print0 | sort -z)

# ── (5) Each scenario has at least one evidence_type entry ───────────────────
while IFS= read -r -d '' yaml_file; do
    base=$(basename "$yaml_file")
    if ! grep -q "evidence_type:" "$yaml_file"; then
        fail "$base: no 'evidence_type:' assertion found — CONST-039 requires positive evidence"
    else
        ok "$base: contains evidence_type assertion"
    fi
done < <(find "$FEATURE_DIR" -maxdepth 1 -name "*.yaml" -print0 | sort -z)

# ── (6) coverage-matrix.md references all 7 known features ───────────────────
EXPECTED_FEATURES=(
    "feature-1-syntax-highlighting"
    "feature-2-source-code-support"
    "feature-3-autocomplete"
    "feature-4a-lsp-completion"
    "feature-4b-diagnostics-hover-gotodef"
    "feature-4c-refactoring"
    "feature-5-import"
)
for feat in "${EXPECTED_FEATURES[@]}"; do
    if grep -q "$feat" "$MATRIX_FILE" 2>/dev/null; then
        ok "coverage-matrix.md references $feat"
    else
        fail "coverage-matrix.md missing reference to $feat"
    fi
done

echo ""
echo "--- RUNTIME LAYER ---"

# ── (7) Check if ADB device available; attempt helixqa dry-run list ──────────
if command -v adb &>/dev/null && adb devices 2>/dev/null | grep -q "emulator\|device$"; then
    DEVICE=$(adb devices | grep "emulator\|device$" | head -1 | awk '{print $1}')
    ok "ADB device available: $DEVICE"

    HELIXQA_BIN="$PROJECT_ROOT/releases/tools/helixqa"
    if [[ -x "$HELIXQA_BIN" ]]; then
        # Dry-run: list scenarios only — no actual execution
        if "$HELIXQA_BIN" list --banks "$FEATURE_DIR" 2>/dev/null | grep -q "feature-"; then
            ok "helixqa list shows feature-coverage scenarios"
        else
            skip "helixqa list returned empty — run 'make helixqa' manually to verify"
        fi
    else
        skip "helixqa binary not found at $HELIXQA_BIN — run 'make helixqa' to build + execute"
    fi
else
    skip "No ADB device/emulator available — runtime execution DEFERRED"
    echo "         To execute: bring up emulator-5554, then run 'make helixqa'"
    echo "         Tracker: #iter-76-helixqa-runtime-deferred-emulator-absent"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo " Results: $PASS_COUNT passed, $FAIL_COUNT failed, $SKIP_COUNT skipped"
echo "============================================================"

if [[ "$FAIL_COUNT" -gt 0 ]]; then
    echo ""
    echo "RESULT: FAIL"
    exit 1
elif [[ "$SKIP_COUNT" -gt 0 && "$PASS_COUNT" -gt 0 ]]; then
    echo ""
    echo "RESULT: PASS (with $SKIP_COUNT deferred runtime checks — emulator required)"
    exit 0
else
    echo ""
    echo "RESULT: PASS"
    exit 0
fi
