#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-85 phase-4 anti-bluff challenge — anti_bluff_cascade_audit.
#
# Codifies the operator's repeated mandate ("This MUST BE part of
# Constitution of our project, its CLAUDE.MD and AGENTS.MD if it is
# not there already, and to be applied to all Submodules's
# Constitution, CLAUDE.MD and AGENTS.MD as well") as a mechanical
# gate that fails loudly if any owned-submodule governance file
# drops the anti-bluff covenant reference.
#
# What it does:
#   For each governance file (CLAUDE.md / AGENTS.md / CONSTITUTION.md
#   / Constitution.md) that exists under the parent project and
#   each owned-submodule root, assert the file contains at least one
#   of the canonical anti-bluff anchors:
#     - verbatim phrase: "most of the features does not work"
#     - rule ID: CONST-035 / CONST-039 / Article XI §11.9 / §11.4
#     - subject: anti-bluff / Anti-Bluff
#
# A missing reference = covenant drift = constitutional violation.
# Per CONST-047 / CONST-052 / CONST-038 the cascade is mandatory.
#
# Scope: parent project + each repo listed in OWNED_SUBMODULES. Add
# new owned submodules to the array as they get adopted. Third-party
# submodules (e.g. anything in tools/opensource/) are out of scope —
# we don't own them and don't dictate their governance.
#
# Exit codes:
#   0 = every governance file in every owned scope contains a covenant anchor
#   1 = at least one governance file is missing the anchor
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

OWNED_SUBMODULES=(
    "HelixConstitution"
    "Challenges"
    "Containers"
    "HelixQA"
    "LLMProvider"
    "Security"
    "Dependencies/HelixDevelopment/DocProcessor"
    "Dependencies/HelixDevelopment/LLMOrchestrator"
    "Dependencies/HelixDevelopment/LLMsVerifier"
    "Dependencies/HelixDevelopment/VisionEngine"
)

GOVERNANCE_FILES=(CLAUDE.md AGENTS.md CONSTITUTION.md Constitution.md)

# Canonical anchors — any ONE present in a file satisfies the requirement.
# Phrased generically per CONST-038 (no consumer-project leakage).
PATTERN='most of the features does not work|CONST-035|CONST-039|Article XI §11\.9|§11\.4|anti-bluff|Anti-Bluff'

echo "=== [anti_bluff_cascade_audit_challenge] running ==="
echo "Pattern: $PATTERN"
echo

failures=0
total=0
audit_one() {
    local label="$1"
    local dir="$2"
    if [[ ! -d "$dir" ]]; then
        echo "  [SKIP] $label: directory missing"
        return
    fi
    for f in "${GOVERNANCE_FILES[@]}"; do
        if [[ -f "$dir/$f" ]]; then
            total=$((total + 1))
            if grep -qiE "$PATTERN" "$dir/$f"; then
                echo "  [OK]   $label / $f"
            else
                echo "  [FAIL] $label / $f — covenant anchor MISSING"
                failures=$((failures + 1))
            fi
        fi
    done
}

# Parent project
audit_one "Yole (parent)" "."

# Owned submodules
for sub in "${OWNED_SUBMODULES[@]}"; do
    audit_one "$sub" "$sub"
done

echo
if [[ "$failures" -gt 0 ]]; then
    echo "FAIL: $failures / $total governance files missing anti-bluff covenant anchor"
    echo
    echo "Per the operator mandate (2026-04-28, re-invoked 2026-05-18):"
    echo '  "This MUST BE part of Constitution of our project, its CLAUDE.MD'
    echo '   and AGENTS.MD if it is not there already, and to be applied to'
    echo '   all Submodules'\''s Constitution, CLAUDE.MD and AGENTS.MD as well!"'
    echo
    echo "Fix by adding an anti-bluff reference to each failed file."
    echo "Canonical anchors: CONST-035, CONST-039, §11.4, Article XI §11.9,"
    echo "or the verbatim quote 'most of the features does not work'."
    exit 1
fi

echo "PASS: $total / $total governance files contain anti-bluff covenant anchor"
echo "      Cascade integrity verified across parent + ${#OWNED_SUBMODULES[@]} owned submodules."
