#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_workspace_edit_applier_challenge.sh — iter-63 Phase 11 anti-bluff gate.
#
# Verifies that WorkspaceEditApplier.kt is present and structurally correct,
# and that its unit tests pass end-to-end. Two layers:
#
#   (a) STATIC: assert WorkspaceEditApplier.kt exists in commonMain/lsp/ and
#       that the ApplyConflict inner class is declared within it.
#       Emits "[OK] <file>" and "[OK] ApplyConflict declared" respectively.
#
#   (b) RUNTIME: runs :shared:desktopTest filtered to *WorkspaceEdit* and
#       *TextEdit* test classes and asserts >= 8 PASSED, 0 FAILED.
#       Emits "[OK] desktopTest: <N> PASSED".
#
# Exit codes:
#   0 = both layers PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# structural "[OK]" for the static layer (file existence + class declaration)
# and PASS count + log path for the runtime layer. No metadata-only PASS.
#
# Mutation guards (CONST-035):
#   1. Delete WorkspaceEditApplier.kt → static layer FAILS immediately.
#   2. Remove 'class ApplyConflict' declaration → static layer FAILS.
#   3. Stub apply() to return sources unchanged → runtime layer FAILS
#      because WorkspaceEditApplierTest.singleFile_singleEdit,
#      multiFile_editsApplied, and conflict_throwsApplyConflict all detect
#      the stub (only nonExistentUri_skippedGracefully passes the stub).
#   4. Revert all stubs → both layers PASS.
#
# Cross-platform impact (CONST-037):
#   - commonMain: WorkspaceEditApplier is pure Kotlin; runs on all targets.
#   - Desktop (desktopTest): all tests live here (JVM, no Android SDK needed).
#   - Android/iOS/Wasm: no per-platform divergence; applier is universal.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.
# The script only drives Gradle and inspects checked-in source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

LSP_SRC="shared/src/commonMain/kotlin/digital/vasic/yole/lsp"
APPLIER_FILE="${LSP_SRC}/WorkspaceEditApplier.kt"

echo "=== [lsp_workspace_edit_applier_challenge] static layer ==="

static_fail=0

# -----------------------------------------------------------------------
# Assert WorkspaceEditApplier.kt exists
# -----------------------------------------------------------------------
echo ""
echo "Checking WorkspaceEditApplier.kt in commonMain/lsp/:"
if [[ ! -f "${APPLIER_FILE}" ]]; then
    echo "FAIL [static]: file missing: ${APPLIER_FILE}"
    static_fail=1
else
    echo "[OK] ${APPLIER_FILE}"
fi

# -----------------------------------------------------------------------
# Assert ApplyConflict class is declared inside WorkspaceEditApplier.kt
# -----------------------------------------------------------------------
echo ""
echo "Checking ApplyConflict class declaration in WorkspaceEditApplier.kt:"
if [[ ! -f "${APPLIER_FILE}" ]]; then
    echo "FAIL [static]: cannot check ApplyConflict — file missing"
    static_fail=1
elif ! grep -q "class ApplyConflict" "${APPLIER_FILE}" 2>/dev/null; then
    echo "FAIL [static]: 'class ApplyConflict' not declared in ${APPLIER_FILE}"
    static_fail=1
else
    echo "[OK] ApplyConflict declared in ${APPLIER_FILE}"
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: WorkspaceEditApplier.kt present and ApplyConflict declared."

# -----------------------------------------------------------------------
# Runtime layer
# -----------------------------------------------------------------------
echo ""
echo "=== [lsp_workspace_edit_applier_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "*WorkspaceEdit*" \
    --tests "*TextEdit*" \
    > "${log}" 2>&1; then
    runtime_ok=1
fi

passed=$(grep -cE " PASSED$" "${log}" 2>/dev/null || true)
failed=$(grep -cE " FAILED$" "${log}" 2>/dev/null || true)
skipped=$(grep -cE " SKIPPED$" "${log}" 2>/dev/null || true)

if (( runtime_ok == 0 )); then
    echo "FAIL [runtime]: :shared:desktopTest did not succeed. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( passed < 8 )); then
    echo "FAIL [runtime]: only ${passed} PASSED — expected >= 8. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( failed > 0 )); then
    echo "FAIL [runtime]: ${failed} FAILED test(s) detected. See ${log}."
    grep -E " FAILED$" "${log}" >&2
    exit 2
fi

echo "[OK] desktopTest: ${passed} PASSED, ${skipped} SKIPPED, ${failed} FAILED — log: ${log}"

echo ""
echo "PASS: lsp_workspace_edit_applier_challenge — WorkspaceEditApplier.kt + ApplyConflict declared + ${passed} tests PASSED (evidence: ${log})."
