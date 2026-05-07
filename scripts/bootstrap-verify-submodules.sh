#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# bootstrap-verify-submodules.sh
#
# Verify that each top-level git submodule is checked out at the
# SHA recorded in the superproject's index. This ensures that a
# fresh clone + bootstrap produces the expected submodule state.
#
# Part of CONST-036 (Continuation Document) and CONST-035 (Anti-Bluff)
# enforcement: a bootstrap that silently leaves submodules detached
# or at wrong SHAs produces a non-reproducible workspace.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FAILURES=0

verify_submodule() {
    local name="$1"
    local expected_commit="$2"

    if [ ! -d "$ROOT_DIR/$name/.git" ] && [ ! -f "$ROOT_DIR/$name/.git" ]; then
        echo "[bootstrap-verify] ERROR: submodule '$name' directory missing or not initialised"
        FAILURES=$((FAILURES + 1))
        return
    fi

    local actual_commit
    actual_commit=$(cd "$ROOT_DIR/$name" && git rev-parse HEAD 2>/dev/null || echo "MISSING")

    if [ "$actual_commit" != "$expected_commit" ]; then
        echo "[bootstrap-verify] ERROR: submodule '$name' is at $actual_commit, expected $expected_commit"
        FAILURES=$((FAILURES + 1))
    else
        echo "[bootstrap-verify] OK: submodule '$name' at expected commit $expected_commit"
    fi
}

# Read expected commits from .gitmodules index
CHALLENGES_SHA=$(cd "$ROOT_DIR" && git ls-tree HEAD Challenges | awk '{print $3}')
CONTAINERS_SHA=$(cd "$ROOT_DIR" && git ls-tree HEAD Containers | awk '{print $3}')
HELIXQA_SHA=$(cd "$ROOT_DIR" && git ls-tree HEAD HelixQA | awk '{print $3}')

verify_submodule "Challenges" "$CHALLENGES_SHA"
verify_submodule "Containers" "$CONTAINERS_SHA"
verify_submodule "HelixQA" "$HELIXQA_SHA"

if [ "$FAILURES" -gt 0 ]; then
    echo "[bootstrap-verify] FAIL: $FAILURES submodule(s) not at expected commits."
    echo "[bootstrap-verify] Run: git submodule update --init Challenges Containers HelixQA"
    exit 1
else
    echo "[bootstrap-verify] PASS: all submodules at expected commits."
    exit 0
fi
