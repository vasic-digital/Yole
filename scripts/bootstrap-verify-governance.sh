#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# bootstrap-verify-governance.sh
#
# Verify that every repository in the project ecosystem has the
# mandatory governance document trio: CONSTITUTION.md, CLAUDE.md,
# AGENTS.md. This is required by CONST-022 (Submodule Governance
# Propagation) and CONST-035 (Anti-Bluff).
#
# The governance trio ensures that every CLI agent and LLM model
# operating on any repo receives the same anti-bluff covenant,
# host-power-management hard ban, and continuation mandate.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FAILURES=0

GOVERNANCE_FILES=("CONSTITUTION.md" "CLAUDE.md" "AGENTS.md")
REPOS=("." "Challenges" "Containers" "HelixQA")

for repo in "${REPOS[@]}"; do
    for file in "${GOVERNANCE_FILES[@]}"; do
        if [ ! -f "$ROOT_DIR/$repo/$file" ]; then
            echo "[governance-verify] ERROR: $repo/$file is MISSING"
            FAILURES=$((FAILURES + 1))
        fi
    done
done

# Verify anti-bluff covenant (CONST-035) is present in each constitution
ANTI_BLUFF_MARKER="CONST-035"
for repo in "${REPOS[@]}"; do
    if ! grep -q "$ANTI_BLUFF_MARKER" "$ROOT_DIR/$repo/CONSTITUTION.md" 2>/dev/null; then
        echo "[governance-verify] ERROR: $repo/CONSTITUTION.md does not contain CONST-035"
        FAILURES=$((FAILURES + 1))
    fi
done

# Verify host power management hard ban (CONST-033) is present
PM_MARKER="CONST-033"
for repo in "${REPOS[@]}"; do
    if ! grep -q "$PM_MARKER" "$ROOT_DIR/$repo/CONSTITUTION.md" 2>/dev/null; then
        echo "[governance-verify] ERROR: $repo/CONSTITUTION.md does not contain CONST-033"
        FAILURES=$((FAILURES + 1))
    fi
done

# Verify continuation mandate (CONST-036, CONST-044, or §6.S) is present
# in either CONSTITUTION.md or CLAUDE.md
CONT_MARKERS=("CONST-036" "CONST-044")
CLAUSE_MARKER="§6.S"
for repo in "${REPOS[@]}"; do
    FOUND=0
    for marker in "${CONT_MARKERS[@]}"; do
        if grep -q "$marker" "$ROOT_DIR/$repo/CONSTITUTION.md" 2>/dev/null; then
            FOUND=1
            break
        fi
    done
    if [ "$FOUND" -eq 0 ] && grep -q "$CLAUSE_MARKER" "$ROOT_DIR/$repo/CLAUDE.md" 2>/dev/null; then
        FOUND=1
    fi
    if [ "$FOUND" -eq 0 ]; then
        echo "[governance-verify] ERROR: $repo does not have continuation mandate (CONST-036, CONST-044 in CONSTITUTION.md, or §6.S in CLAUDE.md)"
        FAILURES=$((FAILURES + 1))
    fi
done

if [ "$FAILURES" -gt 0 ]; then
    echo "[governance-verify] FAIL: $FAILURES governance violation(s) found."
    echo "[governance-verify] All repos MUST have CONSTITUTION.md, CLAUDE.md, AGENTS.md with CONST-033, CONST-035, and continuation mandate."
    exit 1
else
    echo "[governance-verify] PASS: all governance docs present with required constitutional anchors."
    exit 0
fi
