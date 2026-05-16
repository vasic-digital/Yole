#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# import_from_fixture_bundle_challenge.sh — iter-64 Phase 13 anti-bluff gate.
#
# Verifies that:
#   (a) STATIC: 7 UI files exist (ImportButton.kt, ImportMenuItem.kt,
#       ImportProgressDialog.kt, ImportPreview.kt, ImportWarningsPanel.kt —
#       all in androidApp; ImportShareIntentHandler.kt in androidApp;
#       DesktopImportDragDrop.kt in desktopApp).
#       Emits "[OK] <file>" per check.
#
#   (b) RUNTIME: per-importer synthesis-then-import roundtrip tests for all
#       6 importer test classes (DocxImporterTest, HtmlImporterTest,
#       RtfImporterTest, OdtImporterTest, PdfImporterTest, EpubImporterTest)
#       pass — each class synthesises its format at runtime and feeds it
#       through the real importer (no bundled fixture files needed).
#       Asserts >= 6 test classes all produced >= 1 PASSED result each, and
#       0 FAILED overall.
#       Emits "[OK] <ClassName>: <N> PASSED".
#
# Exit codes:
#   0 = both layers PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence — per-file "[OK]" for the
# static layer; per-class PASS count for the runtime layer. No
# metadata-only PASS.
#
# Cross-platform impact (CONST-037):
#   - Static layer: host-agnostic (pure filesystem checks).
#   - Runtime layer: :shared:desktopTest on host JVM.
#   - iOS/Wasm: stub actuals; no regression risk.
#   - Android: UI files verified by static layer; importer logic
#     verified by desktop runtime (shared commonMain code path).
#
# Submodule decoupling (CONST-038): no submodule state is read or required.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

ANDROID_UI_DIR="androidApp/src/main/java/digital/vasic/yole/android/ui/import_"
DESKTOP_UI_DIR="desktopApp/src/main/kotlin/digital/vasic/yole/desktop/ui/import_"

echo "=== [import_from_fixture_bundle_challenge] static layer ==="

static_fail=0

# -----------------------------------------------------------------------
# Assert 6 Android UI files exist
# -----------------------------------------------------------------------
echo ""
echo "Checking 6 Android UI files:"
declare -a ANDROID_UI_FILES=(
    "${ANDROID_UI_DIR}/ImportButton.kt"
    "${ANDROID_UI_DIR}/ImportMenuItem.kt"
    "${ANDROID_UI_DIR}/ImportProgressDialog.kt"
    "${ANDROID_UI_DIR}/ImportPreview.kt"
    "${ANDROID_UI_DIR}/ImportWarningsPanel.kt"
    "${ANDROID_UI_DIR}/ImportShareIntentHandler.kt"
)
for f in "${ANDROID_UI_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: Android UI file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# -----------------------------------------------------------------------
# Assert 1 Desktop UI file exists
# -----------------------------------------------------------------------
echo ""
echo "Checking 1 Desktop UI file:"
DESKTOP_DRAGDROP="${DESKTOP_UI_DIR}/DesktopImportDragDrop.kt"
if [[ ! -f "${DESKTOP_DRAGDROP}" ]]; then
    echo "FAIL [static]: Desktop UI file missing: ${DESKTOP_DRAGDROP}"
    static_fail=1
else
    echo "[OK] ${DESKTOP_DRAGDROP}"
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more static checks failed — see above."
    exit 1
fi

echo ""
echo "OK [static]: all 7 required UI files present (6 Android + 1 Desktop)."

# -----------------------------------------------------------------------
# Runtime layer — per-importer synthesis-then-import roundtrip tests
# -----------------------------------------------------------------------
echo ""
echo "=== [import_from_fixture_bundle_challenge] runtime layer ==="

runtime_log="$(mktemp)"
echo "Runtime test log: ${runtime_log}"

declare -a IMPORTER_CLASSES=(
    "digital.vasic.yole.import_.DocxImporterTest"
    "digital.vasic.yole.import_.HtmlImporterTest"
    "digital.vasic.yole.import_.RtfImporterTest"
    "digital.vasic.yole.import_.OdtImporterTest"
    "digital.vasic.yole.import_.PdfImporterTest"
    "digital.vasic.yole.import_.EpubImporterTest"
)

# Build --tests args
TESTS_ARGS=()
for cls in "${IMPORTER_CLASSES[@]}"; do
    TESTS_ARGS+=("--tests" "${cls}")
done

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    "${TESTS_ARGS[@]}" \
    > "${runtime_log}" 2>&1; then
    runtime_ok=1
fi

total_passed=$(grep -cE " PASSED$" "${runtime_log}" 2>/dev/null || true)
total_failed=$(grep -cE " FAILED$" "${runtime_log}" 2>/dev/null || true)
total_skipped=$(grep -cE " SKIPPED$" "${runtime_log}" 2>/dev/null || true)

if (( runtime_ok == 0 )); then
    echo "FAIL [runtime]: :shared:desktopTest did not succeed. See ${runtime_log}."
    tail -30 "${runtime_log}" >&2
    exit 2
fi

if (( total_failed > 0 )); then
    echo "FAIL [runtime]: ${total_failed} FAILED test(s) detected. See ${runtime_log}."
    grep -E " FAILED$" "${runtime_log}" >&2
    exit 2
fi

runtime_class_fail=0
for cls in "${IMPORTER_CLASSES[@]}"; do
    # Short class name for grep (e.g. DocxImporterTest)
    short_name="${cls##*.}"
    class_passed=$(grep -cE "^${short_name}\[desktop\].*PASSED$" "${runtime_log}" 2>/dev/null || true)
    if (( class_passed < 1 )); then
        echo "FAIL [runtime]: ${short_name} produced 0 PASSED — expected >= 1."
        runtime_class_fail=1
    else
        echo "[OK] ${short_name}: ${class_passed} PASSED"
    fi
done

if (( runtime_class_fail )); then
    echo ""
    echo "FAIL [runtime]: one or more importer test classes had 0 PASSED results. See ${runtime_log}."
    exit 2
fi

echo ""
echo "PASS: import_from_fixture_bundle_challenge — 7 UI files present + 6 importer classes all PASSED (total ${total_passed} PASSED, ${total_failed} FAILED — evidence: ${runtime_log})."
