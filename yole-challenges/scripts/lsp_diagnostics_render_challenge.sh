#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_diagnostics_render_challenge.sh — iter-62 Phase 9 anti-bluff gate.
#
# Verifies that the LSP diagnostics rendering pipeline is complete and that
# the runtime test suites pass end-to-end. Two layers:
#
#   (a) STATIC: filesystem checks — 3 render composables exist in
#       androidApp/ui/editor/diagnostics/, DiagnosticsPalette.kt exists,
#       DiagnosticsCache is referenced in both LspServerHost.desktop.kt and
#       LspServerHost.android.kt via publishDiagnostics wires.
#       Emits "[OK] <file>" per check.
#
#   (b) RUNTIME (two sub-layers):
#       1. Runs :shared:desktopTest filtered to Diagnostic* + DiagnosticsCache*
#          and asserts >= 8 PASSED, 0 FAILED.
#       2. Runs :androidApp:testDebugUnitTest filtered to *Diagnostics*
#          and asserts >= 3 PASSED, 0 FAILED.
#       Emits "[OK] desktopTest: <N> PASSED" and "[OK] androidUnitTest: <M> PASSED".
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
# the host JVM (no Android SDK required for desktop sub-layer) and
# :androidApp:testDebugUnitTest with -PincludeRobolectric=true
# for the Android sub-layer. iOS and Wasm targets return empty diagnostics
# lists — benign degradation per the Phase 5 design (no-op actuals).
#
# Submodule decoupling (CONST-038): no submodule state is read or required.
# The script only drives Gradle and inspects checked-in source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

ANDROID_DIAG_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics"
LSP_DESKTOP_HOST="shared/src/desktopMain/kotlin/digital/vasic/yole/lsp/LspServerHost.desktop.kt"
LSP_ANDROID_HOST="shared/src/androidMain/kotlin/digital/vasic/yole/lsp/LspServerHost.android.kt"

echo "=== [lsp_diagnostics_render_challenge] static layer ==="

static_fail=0

# ----------------------------------------------------------------
# Assert 3 render composable files exist
# ----------------------------------------------------------------
echo ""
echo "Checking 3 diagnostics render composables:"
declare -a RENDER_FILES=(
    "${ANDROID_DIAG_DIR}/DiagnosticsGutter.kt"
    "${ANDROID_DIAG_DIR}/DiagnosticsInlineUnderline.kt"
    "${ANDROID_DIAG_DIR}/DiagnosticsProblemsPanel.kt"
)
for f in "${RENDER_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: render file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# ----------------------------------------------------------------
# Assert DiagnosticsPalette.kt exists
# ----------------------------------------------------------------
echo ""
echo "Checking DiagnosticsPalette.kt:"
PALETTE_FILE="${ANDROID_DIAG_DIR}/DiagnosticsPalette.kt"
if [[ ! -f "${PALETTE_FILE}" ]]; then
    echo "FAIL [static]: DiagnosticsPalette.kt missing: ${PALETTE_FILE}"
    static_fail=1
else
    echo "[OK] ${PALETTE_FILE}"
fi

# ----------------------------------------------------------------
# Assert DiagnosticsCache referenced in LspServerHost.desktop.kt
# ----------------------------------------------------------------
echo ""
echo "Checking DiagnosticsCache wire in LspServerHost.desktop.kt:"
if [[ ! -f "${LSP_DESKTOP_HOST}" ]]; then
    echo "FAIL [static]: file not found: ${LSP_DESKTOP_HOST}"
    static_fail=1
elif ! grep -q "DiagnosticsCache" "${LSP_DESKTOP_HOST}" 2>/dev/null; then
    echo "FAIL [static]: DiagnosticsCache not referenced in ${LSP_DESKTOP_HOST}"
    static_fail=1
else
    echo "[OK] DiagnosticsCache referenced in ${LSP_DESKTOP_HOST}"
fi

# ----------------------------------------------------------------
# Assert DiagnosticsCache referenced in LspServerHost.android.kt
# ----------------------------------------------------------------
echo ""
echo "Checking DiagnosticsCache wire in LspServerHost.android.kt:"
if [[ ! -f "${LSP_ANDROID_HOST}" ]]; then
    echo "FAIL [static]: file not found: ${LSP_ANDROID_HOST}"
    static_fail=1
elif ! grep -q "DiagnosticsCache" "${LSP_ANDROID_HOST}" 2>/dev/null; then
    echo "FAIL [static]: DiagnosticsCache not referenced in ${LSP_ANDROID_HOST}"
    static_fail=1
else
    echo "[OK] DiagnosticsCache referenced in ${LSP_ANDROID_HOST}"
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: 3 render composables, DiagnosticsPalette, and DiagnosticsCache wires all present."

# ----------------------------------------------------------------
# Runtime layer — desktop sub-layer
# ----------------------------------------------------------------
echo ""
echo "=== [lsp_diagnostics_render_challenge] runtime layer — desktopTest ==="

log_desktop="$(mktemp)"
echo "Desktop runtime log: ${log_desktop}"

desktop_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "digital.vasic.yole.lsp.Diagnostic*" \
    --tests "digital.vasic.yole.lsp.DiagnosticsCache*" \
    > "${log_desktop}" 2>&1; then
    desktop_ok=1
fi

desktop_passed=$(grep -cE " PASSED$" "${log_desktop}" 2>/dev/null || true)
desktop_failed=$(grep -cE " FAILED$" "${log_desktop}" 2>/dev/null || true)
desktop_skipped=$(grep -cE " SKIPPED$" "${log_desktop}" 2>/dev/null || true)

if (( desktop_ok == 0 )); then
    echo "FAIL [runtime/desktop]: :shared:desktopTest did not succeed. See ${log_desktop}."
    tail -30 "${log_desktop}" >&2
    exit 2
fi

if (( desktop_passed < 8 )); then
    echo "FAIL [runtime/desktop]: only ${desktop_passed} PASSED — expected >= 8. See ${log_desktop}."
    tail -30 "${log_desktop}" >&2
    exit 2
fi

if (( desktop_failed > 0 )); then
    echo "FAIL [runtime/desktop]: ${desktop_failed} FAILED test(s) detected. See ${log_desktop}."
    grep -E " FAILED$" "${log_desktop}" >&2
    exit 2
fi

echo "[OK] desktopTest: ${desktop_passed} PASSED, ${desktop_skipped} SKIPPED, ${desktop_failed} FAILED — log: ${log_desktop}"

# ----------------------------------------------------------------
# Runtime layer — Android unit test sub-layer
# ----------------------------------------------------------------
echo ""
echo "=== [lsp_diagnostics_render_challenge] runtime layer — androidUnitTest ==="

log_android="$(mktemp)"
echo "Android runtime log: ${log_android}"

android_ok=0
if ./gradlew :androidApp:testDebugUnitTest \
    -PincludeRobolectric=true \
    --tests "*Diagnostics*" \
    --rerun-tasks \
    > "${log_android}" 2>&1; then
    android_ok=1
fi

android_passed=$(grep -cE " PASSED$" "${log_android}" 2>/dev/null || true)
android_failed=$(grep -cE " FAILED$" "${log_android}" 2>/dev/null || true)
android_skipped=$(grep -cE " SKIPPED$" "${log_android}" 2>/dev/null || true)

if (( android_ok == 0 )); then
    echo "FAIL [runtime/android]: :androidApp:testFlavorDefaultDebugUnitTest did not succeed. See ${log_android}."
    tail -30 "${log_android}" >&2
    exit 2
fi

if (( android_passed < 3 )); then
    echo "FAIL [runtime/android]: only ${android_passed} PASSED — expected >= 3. See ${log_android}."
    tail -30 "${log_android}" >&2
    exit 2
fi

if (( android_failed > 0 )); then
    echo "FAIL [runtime/android]: ${android_failed} FAILED test(s) detected. See ${log_android}."
    grep -E " FAILED$" "${log_android}" >&2
    exit 2
fi

echo "[OK] androidUnitTest: ${android_passed} PASSED, ${android_skipped} SKIPPED, ${android_failed} FAILED — log: ${log_android}"

echo ""
echo "PASS: lsp_diagnostics_render_challenge — 4 render/palette files + DiagnosticsCache wires + ${desktop_passed} desktop tests + ${android_passed} Android tests PASSED (evidence: ${log_desktop}, ${log_android})."
