#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_hover_definition_challenge.sh — iter-62 Phase 9 anti-bluff gate.
#
# Verifies that the LSP hover + go-to-definition foundation is complete and
# that the runtime test suites pass end-to-end. Two layers:
#
#   (a) STATIC: filesystem checks — 7 foundation files exist in
#       shared/src/commonMain/kotlin/digital/vasic/yole/lsp/, 3 Android UI
#       files exist, and the expect class LspServerHost.kt declares both
#       `hover` and `definition` methods. Emits "[OK] <file>" per check.
#
#   (b) RUNTIME: runs :shared:desktopTest filtered to Hover*, Definition*,
#       EditorNavigationStack*, GoToDefinition*, LspRangeMapping* and asserts
#       >= 10 PASSED, 0 FAILED. Emits "[OK] desktopTest: <N> PASSED".
#
# Exit codes:
#   0 = both layers PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# per-file "[OK]" for the static layer and PASS count + log path for
# the runtime layer. No metadata-only PASS.
#
# Cross-platform impact (CONST-037): static layer is host-agnostic
# (pure filesystem checks). Runtime layer uses :shared:desktopTest on
# the host JVM (no Android SDK required). Android hover UI is provided
# by HoverPopup.kt and wired via IdeEditorScreen. iOS and Wasm targets
# use no-op actuals for hover/definition — benign degradation per Phase 6-7
# design.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.
# The script only drives Gradle and inspects checked-in source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

LSP_SRC_DIR="shared/src/commonMain/kotlin/digital/vasic/yole/lsp"
ANDROID_HOVER_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/hover"
ANDROID_NAV_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/navigation"
LSP_EXPECT_HOST="${LSP_SRC_DIR}/LspServerHost.kt"

echo "=== [lsp_hover_definition_challenge] static layer ==="

static_fail=0

# ----------------------------------------------------------------
# Assert 7 foundation files exist in commonMain/lsp/
# ----------------------------------------------------------------
echo ""
echo "Checking 7 hover/definition foundation files in commonMain:"
declare -a FOUNDATION_FILES=(
    "${LSP_SRC_DIR}/HoverInfo.kt"
    "${LSP_SRC_DIR}/HoverBlock.kt"
    "${LSP_SRC_DIR}/HoverMarkdownRenderer.kt"
    "${LSP_SRC_DIR}/HoverTriggerDetector.kt"
    "${LSP_SRC_DIR}/DefinitionLocation.kt"
    "${LSP_SRC_DIR}/EditorNavigationStack.kt"
    "${LSP_SRC_DIR}/GoToDefinitionAction.kt"
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
# Assert 2 Android hover UI files exist
# ----------------------------------------------------------------
echo ""
echo "Checking 2 Android hover UI files:"
declare -a HOVER_FILES=(
    "${ANDROID_HOVER_DIR}/HoverPopup.kt"
    "${ANDROID_HOVER_DIR}/HoverShortcut.kt"
)
for f in "${HOVER_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: Android hover file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# ----------------------------------------------------------------
# Assert Android navigation file exists
# ----------------------------------------------------------------
echo ""
echo "Checking Android navigation UI file:"
DEF_CHOOSER="${ANDROID_NAV_DIR}/DefinitionLocationChooser.kt"
if [[ ! -f "${DEF_CHOOSER}" ]]; then
    echo "FAIL [static]: Android navigation file missing: ${DEF_CHOOSER}"
    static_fail=1
else
    echo "[OK] ${DEF_CHOOSER}"
fi

# ----------------------------------------------------------------
# Assert LspServerHost.kt (expect class) declares hover + definition methods
# ----------------------------------------------------------------
echo ""
echo "Checking LspServerHost.kt (expect) for hover + definition declarations:"
if [[ ! -f "${LSP_EXPECT_HOST}" ]]; then
    echo "FAIL [static]: expect file not found: ${LSP_EXPECT_HOST}"
    static_fail=1
else
    if ! grep -q "fun hover" "${LSP_EXPECT_HOST}" 2>/dev/null; then
        echo "FAIL [static]: 'fun hover' not declared in ${LSP_EXPECT_HOST}"
        static_fail=1
    else
        echo "[OK] 'fun hover' declared in ${LSP_EXPECT_HOST}"
    fi

    if ! grep -q "fun definition" "${LSP_EXPECT_HOST}" 2>/dev/null; then
        echo "FAIL [static]: 'fun definition' not declared in ${LSP_EXPECT_HOST}"
        static_fail=1
    else
        echo "[OK] 'fun definition' declared in ${LSP_EXPECT_HOST}"
    fi
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: 7 foundation files, 3 Android UI files, and hover+definition method declarations all present."

# ----------------------------------------------------------------
# Runtime layer
# ----------------------------------------------------------------
echo ""
echo "=== [lsp_hover_definition_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "digital.vasic.yole.lsp.Hover*" \
    --tests "digital.vasic.yole.lsp.Definition*" \
    --tests "digital.vasic.yole.lsp.EditorNavigationStack*" \
    --tests "digital.vasic.yole.lsp.GoToDefinition*" \
    --tests "digital.vasic.yole.lsp.LspRangeMapping*" \
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

if (( passed < 10 )); then
    echo "FAIL [runtime]: only ${passed} PASSED — expected >= 10. See ${log}."
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
echo "PASS: lsp_hover_definition_challenge — 10 foundation/UI files + hover+definition methods declared + ${passed} tests PASSED (evidence: ${log})."
