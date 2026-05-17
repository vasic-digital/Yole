#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# helixqa_evidence_size_portable_challenge.sh — iter-76 stat-portability regression gate.
#
# Regression test for the macOS "stat -c%s" host bug fixed in iter-76.
# On macOS (BSD stat), "stat -c%s file" fails silently and emits "0" via the
# "|| echo 0" fallback in the old code — causing every screenshot/recording
# to report 0 bytes, so the evidence validator could PASS on empty files.
#
# This challenge verifies the portable get_file_size() helper in
# automation/helixqa-validate.sh correctly reports a known file size (1234 B)
# on the running host, regardless of OS.
#
# Emits "[OK] ..." on each PASS or "[FAIL] ..." on each failure.
# Exits 0 (PASS) or 1 (FAIL).

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT="$PROJECT_ROOT/automation/helixqa-validate.sh"

PASS_COUNT=0
FAIL_COUNT=0

ok()   { echo "  [OK]   $1"; ((PASS_COUNT++)); }
fail() { echo "  [FAIL] $1"; ((FAIL_COUNT++)); }

echo "============================================================"
echo " helixqa_evidence_size_portable_challenge (iter-76)"
echo "============================================================"

# ── (1) helixqa-validate.sh must exist ──────────────────────────────────────
if [[ ! -f "$SCRIPT" ]]; then
    fail "helixqa-validate.sh not found at: $SCRIPT"
    echo ""
    echo "RESULT: FAIL"
    exit 1
fi
ok "helixqa-validate.sh present at $SCRIPT"

# ── (2) get_file_size function must be defined in the script ─────────────────
if ! grep -q "get_file_size()" "$SCRIPT"; then
    fail "get_file_size() function not found in helixqa-validate.sh (old GNU stat code still present?)"
    ((FAIL_COUNT++))
else
    ok "get_file_size() function declared in helixqa-validate.sh"
fi

# ── (3) No bare 'stat -c%s' in helixqa-validate.sh ──────────────────────────
if grep -qE 'stat -c%s|stat -c %s' "$SCRIPT"; then
    fail "Bare 'stat -c%s' (GNU-only) still present in helixqa-validate.sh"
    ((FAIL_COUNT++))
else
    ok "No bare GNU stat -c%s in helixqa-validate.sh"
fi

# ── (4) Runtime: create 1234-byte file; verify get_file_size reports 1234 ────
TMP_FILE=$(mktemp)
TMP_DIR=$(dirname "$TMP_FILE")
trap 'rm -f "$TMP_FILE"' EXIT

dd if=/dev/zero of="$TMP_FILE" bs=1 count=1234 2>/dev/null
EXPECTED=1234

# Load only the get_file_size function from helixqa-validate.sh without
# executing the rest of the script (which has side effects).
# Extract the function body, write to a sourced helper, then call it.
FUNC_HELPER=$(mktemp /tmp/get_file_size_helper.XXXXXX.sh)
trap 'rm -f "$TMP_FILE" "$FUNC_HELPER"' EXIT

# Extract the function block using awk (portable; no GNU extensions)
awk '/^get_file_size\(\)/{found=1} found{print} /^}$/{if(found) exit}' \
    "$SCRIPT" > "$FUNC_HELPER" 2>/dev/null

if [[ ! -s "$FUNC_HELPER" ]]; then
    fail "Could not extract get_file_size() from helixqa-validate.sh via awk"
    ((FAIL_COUNT++))
else
    ok "Extracted get_file_size() helper block ($(wc -l < "$FUNC_HELPER") lines)"

    # Source the function and call it
    # shellcheck source=/dev/null
    source "$FUNC_HELPER"
    REPORTED=$(get_file_size "$TMP_FILE")

    if [[ "$REPORTED" == "$EXPECTED" ]]; then
        ok "get_file_size reported $REPORTED bytes (expected $EXPECTED) — portable on $(uname -s)"
    else
        fail "get_file_size reported '$REPORTED' bytes, expected '$EXPECTED' — portability fix broken on $(uname -s)"
    fi
fi

# ── (5) Verify the old GNU-only one-liner would have returned 0 on macOS ─────
# (documentation only — we don't execute "stat -c%s" here because on macOS it
# emits an error. We simply confirm the old pattern is absent as a static check.)
echo ""
echo "  Note: 'stat -c%s' returns 0 on macOS (BSD) silently."
echo "  This challenge confirms the portable helper was adopted instead."
echo ""

# ── Summary ───────────────────────────────────────────────────────────────────
echo "============================================================"
echo " Results: $PASS_COUNT passed, $FAIL_COUNT failed"
echo "============================================================"

if [[ "$FAIL_COUNT" -gt 0 ]]; then
    echo ""
    echo "RESULT: FAIL"
    exit 1
else
    echo ""
    echo "RESULT: PASS"
    exit 0
fi
