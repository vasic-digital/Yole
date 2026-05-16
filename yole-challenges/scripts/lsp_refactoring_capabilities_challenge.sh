#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_refactoring_capabilities_challenge.sh — iter-63 Phase 11 anti-bluff gate.
#
# Verifies that the LSP refactoring capabilities foundation (rename,
# code actions, signature help, formatting, find-references) is complete
# and that the runtime test suites pass end-to-end. Three layers:
#
#   (a) STATIC: filesystem checks — core commonMain files (WorkspaceEdit.kt,
#       TextEdit.kt, WorkspaceEditApplier.kt, CodeAction.kt, SignatureHelp.kt,
#       ReferenceLocation.kt), 5 requester interfaces
#       (LspRenameRequester, LspCodeActionRequester, LspSignatureHelpRequester,
#       LspFormattingRequester, LspReferencesRequester), FormattingTrigger.kt,
#       SignatureHelpTrigger.kt, FindReferencesAction.kt. Plus 4 Android UI
#       files: RenamePreviewPanel.kt, CodeActionLightbulb.kt, SignatureHelpPill.kt,
#       ReferencesPanel.kt. Emits "[OK] <file>" per check.
#
#   (b) RUNTIME (desktop): runs :shared:desktopTest filtered to the lsp.*
#       package and asserts >= 50 PASSED, 0 FAILED.
#       Emits "[OK] desktopTest: <N> PASSED".
#
#   (c) RUNTIME (Robolectric): runs :androidApp:testDebugUnitTest
#       -PincludeRobolectric=true filtered to *Rename*RobolectricTest*,
#       *CodeAction*RobolectricTest*, *SignatureHelp*RobolectricTest*,
#       *References*RobolectricTest*, *FormattingSettings*RobolectricTest*
#       and asserts >= 14 PASSED, 0 FAILED.
#       Emits "[OK] Robolectric: <N> PASSED".
#
# Exit codes:
#   0 = all three layers PASS
#   1 = static layer fail
#   2 = desktop runtime layer fail
#   3 = Robolectric layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# per-file "[OK]" for the static layer and PASS count + log path for
# the runtime layers. No metadata-only PASS.
#
# Cross-platform impact (CONST-037):
#   - Static layer: host-agnostic (pure filesystem checks).
#   - Desktop runtime: :shared:desktopTest on host JVM (no Android SDK required).
#   - Robolectric: :androidApp:testDebugUnitTest (Android SDK required on host).
#   - iOS/Wasm: stub actuals for all 5 capabilities; no regression risk.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.
# The script only drives Gradle and inspects checked-in source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

LSP_SRC="shared/src/commonMain/kotlin/digital/vasic/yole/lsp"
ANDROID_RENAME_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/rename"
ANDROID_CODEACTION_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/codeaction"
ANDROID_SIGHELP_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/signaturehelp"
ANDROID_REFS_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/references"

echo "=== [lsp_refactoring_capabilities_challenge] static layer ==="

static_fail=0

# -----------------------------------------------------------------------
# Assert 13 core commonMain files exist
# -----------------------------------------------------------------------
echo ""
echo "Checking 13 core commonMain lsp/ files:"
declare -a CORE_FILES=(
    "${LSP_SRC}/WorkspaceEdit.kt"
    "${LSP_SRC}/TextEdit.kt"
    "${LSP_SRC}/WorkspaceEditApplier.kt"
    "${LSP_SRC}/CodeAction.kt"
    "${LSP_SRC}/SignatureHelp.kt"
    "${LSP_SRC}/ReferenceLocation.kt"
    "${LSP_SRC}/LspRenameRequester.kt"
    "${LSP_SRC}/LspCodeActionRequester.kt"
    "${LSP_SRC}/LspSignatureHelpRequester.kt"
    "${LSP_SRC}/LspFormattingRequester.kt"
    "${LSP_SRC}/LspReferencesRequester.kt"
    "${LSP_SRC}/FormattingTrigger.kt"
    "${LSP_SRC}/SignatureHelpTrigger.kt"
    "${LSP_SRC}/FindReferencesAction.kt"
)
for f in "${CORE_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: core file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# -----------------------------------------------------------------------
# Assert 4 Android UI files exist
# -----------------------------------------------------------------------
echo ""
echo "Checking 4 Android UI files:"
declare -a UI_FILES=(
    "${ANDROID_RENAME_DIR}/RenamePreviewPanel.kt"
    "${ANDROID_CODEACTION_DIR}/CodeActionLightbulb.kt"
    "${ANDROID_SIGHELP_DIR}/SignatureHelpPill.kt"
    "${ANDROID_REFS_DIR}/ReferencesPanel.kt"
)
for f in "${UI_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: Android UI file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: all 14 required files present (10 commonMain + 4 Android UI)."

# -----------------------------------------------------------------------
# Desktop runtime layer
# -----------------------------------------------------------------------
echo ""
echo "=== [lsp_refactoring_capabilities_challenge] desktop runtime layer ==="

desktop_log="$(mktemp)"
echo "Desktop test log: ${desktop_log}"

desktop_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "digital.vasic.yole.lsp.*" \
    > "${desktop_log}" 2>&1; then
    desktop_ok=1
fi

desktop_passed=$(grep -cE " PASSED$" "${desktop_log}" 2>/dev/null || true)
desktop_failed=$(grep -cE " FAILED$" "${desktop_log}" 2>/dev/null || true)
desktop_skipped=$(grep -cE " SKIPPED$" "${desktop_log}" 2>/dev/null || true)

if (( desktop_ok == 0 )); then
    echo "FAIL [desktop-runtime]: :shared:desktopTest did not succeed. See ${desktop_log}."
    tail -30 "${desktop_log}" >&2
    exit 2
fi

if (( desktop_passed < 50 )); then
    echo "FAIL [desktop-runtime]: only ${desktop_passed} PASSED — expected >= 50. See ${desktop_log}."
    tail -30 "${desktop_log}" >&2
    exit 2
fi

if (( desktop_failed > 0 )); then
    echo "FAIL [desktop-runtime]: ${desktop_failed} FAILED test(s) detected. See ${desktop_log}."
    grep -E " FAILED$" "${desktop_log}" >&2
    exit 2
fi

echo "[OK] desktopTest: ${desktop_passed} PASSED, ${desktop_skipped} SKIPPED, ${desktop_failed} FAILED — log: ${desktop_log}"

# -----------------------------------------------------------------------
# Robolectric runtime layer
# -----------------------------------------------------------------------
echo ""
echo "=== [lsp_refactoring_capabilities_challenge] Robolectric layer ==="

robo_log="$(mktemp)"
echo "Robolectric test log: ${robo_log}"

robo_ok=0
if ./gradlew :androidApp:testDebugUnitTest \
    -PincludeRobolectric=true \
    --rerun-tasks \
    --tests "*Rename*RobolectricTest*" \
    --tests "*CodeAction*RobolectricTest*" \
    --tests "*SignatureHelp*RobolectricTest*" \
    --tests "*References*RobolectricTest*" \
    --tests "*FormattingSettings*RobolectricTest*" \
    > "${robo_log}" 2>&1; then
    robo_ok=1
fi

robo_passed=$(grep -cE " PASSED$" "${robo_log}" 2>/dev/null || true)
robo_failed=$(grep -cE " FAILED$" "${robo_log}" 2>/dev/null || true)
robo_skipped=$(grep -cE " SKIPPED$" "${robo_log}" 2>/dev/null || true)

if (( robo_ok == 0 )); then
    echo "FAIL [robolectric]: :androidApp:testDebugUnitTest did not succeed. See ${robo_log}."
    tail -30 "${robo_log}" >&2
    exit 3
fi

if (( robo_passed < 14 )); then
    echo "FAIL [robolectric]: only ${robo_passed} PASSED — expected >= 14. See ${robo_log}."
    tail -30 "${robo_log}" >&2
    exit 3
fi

if (( robo_failed > 0 )); then
    echo "FAIL [robolectric]: ${robo_failed} FAILED test(s) detected. See ${robo_log}."
    grep -E " FAILED$" "${robo_log}" >&2
    exit 3
fi

echo "[OK] Robolectric: ${robo_passed} PASSED, ${robo_skipped} SKIPPED, ${robo_failed} FAILED — log: ${robo_log}"

echo ""
echo "PASS: lsp_refactoring_capabilities_challenge — 14 files present + ${desktop_passed} desktop tests + ${robo_passed} Robolectric tests all PASSED (evidence: ${desktop_log}, ${robo_log})."
