#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_hosting_completeness_challenge.sh — iter-61 Phase 9 anti-bluff gate.
#
# Verifies that the LSP hosting foundation is complete and that the runtime
# test suites pass end-to-end. Two layers:
#
#   (a) STATIC: filesystem checks — 15 server.json resources exist under
#       lsp-servers/<langId>/server.json, the 6 LSP foundation files exist in
#       commonMain, and LspCompletionProvider is referenced in
#       CompletionEngine.kt's default(...) factory. Emits "[OK] <file>" per check.
#
#   (b) RUNTIME: runs :shared:desktopTest filtered to
#       digital.vasic.yole.lsp.* and asserts >= 25 PASSED lines + 0 FAILED.
#       Threshold is 25 (actual Phase 1–8 count: ~34 PASSED across
#       LspServerSpec, LspServerRegistry, LspWorkspaceResolver, LspServerInstaller,
#       LspServerHost, LspCompletionProvider, RealServerSmokeTest). We anchor at 25
#       to absorb SKIP volatility when LSP binaries are absent on CI.
#
# Exit codes:
#   0 = both layers PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# per-file "[OK]" for the static layer and PASS count + log path
# for the runtime layer. No metadata-only PASS.
#
# Cross-platform impact (CONST-037): static layer is host-agnostic
# (pure filesystem). Runtime layer runs on the host JVM via
# :shared:desktopTest (no Android SDK required). Android LSP stub is
# a no-op (LspCompletionProvider.android.kt returns empty list). iOS
# and Wasm actuals return empty list similarly — benign degradation
# per the Phase 5 design.
#
# Submodule decoupling (CONST-038): no submodule state is read or
# required. The script only drives Gradle and inspects checked-in
# source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

LSP_RESOURCES_DIR="shared/src/commonMain/resources/lsp-servers"
LSP_SRC_DIR="shared/src/commonMain/kotlin/digital/vasic/yole/lsp"
COMPLETION_DIR="shared/src/commonMain/kotlin/digital/vasic/yole/completion"
ENGINE_FILE="${COMPLETION_DIR}/CompletionEngine.kt"

echo "=== [lsp_hosting_completeness_challenge] static layer ==="

static_fail=0

# ----------------------------------------------------------------
# Guard: CompletionEngine.kt must exist
# ----------------------------------------------------------------
if [[ ! -f "${ENGINE_FILE}" ]]; then
    echo "FAIL [static]: ${ENGINE_FILE} not found — CompletionEngine missing."
    exit 1
fi
echo "[OK] CompletionEngine.kt exists: ${ENGINE_FILE}"

# ----------------------------------------------------------------
# 15 server.json resources
# ----------------------------------------------------------------
echo ""
echo "Checking 15 server.json resources:"
declare -a LANG_IDS=(rust go c cpp markdown lua zig python elixir haskell typescript bash yaml java kotlin)
for langId in "${LANG_IDS[@]}"; do
    file="${LSP_RESOURCES_DIR}/${langId}/server.json"
    if [[ ! -f "${file}" ]]; then
        echo "FAIL [static]: missing: ${file}"
        static_fail=1
    else
        echo "[OK] ${file}"
    fi
done

# ----------------------------------------------------------------
# 6 foundation source files in commonMain/lsp/
# ----------------------------------------------------------------
echo ""
echo "Checking 6 LSP foundation source files:"
declare -a FOUNDATION_FILES=(
    "${LSP_SRC_DIR}/LspServerSpec.kt"
    "${LSP_SRC_DIR}/LspServerRegistry.kt"
    "${LSP_SRC_DIR}/LspWorkspaceResolver.kt"
    "${LSP_SRC_DIR}/LspServerInstaller.kt"
    "${LSP_SRC_DIR}/LspServerHost.kt"
    "${COMPLETION_DIR}/providers/LspCompletionProvider.kt"
)
for f in "${FOUNDATION_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: foundation file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# ----------------------------------------------------------------
# LspCompletionProvider referenced in CompletionEngine.default()
# ----------------------------------------------------------------
echo ""
if ! grep -q "LspCompletionProvider(" "${ENGINE_FILE}" 2>/dev/null; then
    echo "FAIL [static]: LspCompletionProvider is not referenced in ${ENGINE_FILE} default() factory."
    static_fail=1
else
    echo "[OK] LspCompletionProvider( referenced in CompletionEngine.default()"
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: all 15 server.json resources, 6 foundation files, and engine reference present."

# ----------------------------------------------------------------
# Runtime layer
# ----------------------------------------------------------------
echo ""
echo "=== [lsp_hosting_completeness_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "digital.vasic.yole.lsp.*" \
    > "${log}" 2>&1; then
    runtime_ok=1
fi

passed=$(grep -cE " PASSED$" "${log}" 2>/dev/null || true)
skipped=$(grep -cE " SKIPPED$" "${log}" 2>/dev/null || true)
failed=$(grep -cE " FAILED$" "${log}" 2>/dev/null || true)

echo "[OK] desktopTest filtered to digital.vasic.yole.lsp.*: ${passed} PASSED, ${skipped} SKIPPED, ${failed} FAILED — log: ${log}"

if (( runtime_ok == 0 )); then
    echo "FAIL [runtime]: :shared:desktopTest did not succeed. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( passed < 25 )); then
    echo "FAIL [runtime]: only ${passed} PASSED lines — expected >= 25. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( failed > 0 )); then
    echo "FAIL [runtime]: ${failed} FAILED test(s) detected. See ${log}."
    grep -E " FAILED$" "${log}" >&2
    exit 2
fi

echo ""
echo "PASS: lsp_hosting_completeness_challenge — 15 server.json + 6 foundation files + ${passed} tests PASSED (evidence: ${log})."
