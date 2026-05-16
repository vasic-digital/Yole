#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# import_from_completeness_challenge.sh — iter-64 Phase 13 anti-bluff gate.
#
# Verifies that the "Import from" feature (iter-64) is complete and that
# the runtime test suites pass end-to-end. Three layers:
#
#   (a) STATIC: filesystem checks —
#       6 importer files (DocxImporter.kt, HtmlImporter.kt, RtfImporter.kt,
#       OdtImporter.kt, PdfImporter.kt, EpubImporter.kt) in commonMain;
#       5 conversion helpers (HeadingDetector.kt, CodeBlockDetector.kt,
#       TableConverter.kt, ImageExtractor.kt, LinkPreserver.kt);
#       ImporterRegistry.kt, ImportedDocument.kt, DocumentImporter.kt.
#       Emits "[OK] <file>" per check.
#
#   (b) RUNTIME (desktop): runs :shared:desktopTest filtered to the
#       digital.vasic.yole.import_.* package and asserts >= 30 PASSED,
#       0 FAILED.  Emits "[OK] desktopTest: <N> PASSED".
#
#   (c) RUNTIME (Robolectric): runs :androidApp:testDebugUnitTest
#       -PincludeRobolectric=true filtered to *Import* and asserts
#       >= 8 PASSED, 0 FAILED.
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
#   - iOS/Wasm: stub actuals for all 6 importers; no regression risk.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.
# The script only drives Gradle and inspects checked-in source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

COMMON_IMPORT="shared/src/commonMain/kotlin/digital/vasic/yole/import_"
CONVERSION_DIR="${COMMON_IMPORT}/conversion"

echo "=== [import_from_completeness_challenge] static layer ==="

static_fail=0

# -----------------------------------------------------------------------
# Assert 3 foundation files exist
# -----------------------------------------------------------------------
echo ""
echo "Checking 3 foundation files:"
declare -a FOUNDATION_FILES=(
    "${COMMON_IMPORT}/DocumentImporter.kt"
    "${COMMON_IMPORT}/ImportedDocument.kt"
    "${COMMON_IMPORT}/ImporterRegistry.kt"
)
for f in "${FOUNDATION_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: foundation file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# -----------------------------------------------------------------------
# Assert 6 importer files exist in commonMain
# -----------------------------------------------------------------------
echo ""
echo "Checking 6 importer files in commonMain:"
declare -a IMPORTER_FILES=(
    "${COMMON_IMPORT}/DocxImporter.kt"
    "${COMMON_IMPORT}/HtmlImporter.kt"
    "${COMMON_IMPORT}/RtfImporter.kt"
    "${COMMON_IMPORT}/OdtImporter.kt"
    "${COMMON_IMPORT}/PdfImporter.kt"
    "${COMMON_IMPORT}/EpubImporter.kt"
)
for f in "${IMPORTER_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: importer file missing: ${f}"
        static_fail=1
    else
        echo "[OK] ${f}"
    fi
done

# -----------------------------------------------------------------------
# Assert 5 conversion helper files exist
# -----------------------------------------------------------------------
echo ""
echo "Checking 5 conversion helper files:"
declare -a HELPER_FILES=(
    "${CONVERSION_DIR}/HeadingDetector.kt"
    "${CONVERSION_DIR}/CodeBlockDetector.kt"
    "${CONVERSION_DIR}/TableConverter.kt"
    "${CONVERSION_DIR}/ImageExtractor.kt"
    "${CONVERSION_DIR}/LinkPreserver.kt"
)
for f in "${HELPER_FILES[@]}"; do
    if [[ ! -f "${f}" ]]; then
        echo "FAIL [static]: conversion helper missing: ${f}"
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
echo "OK [static]: all 14 required files present (3 foundation + 6 importers + 5 conversion helpers)."

# -----------------------------------------------------------------------
# Desktop runtime layer
# -----------------------------------------------------------------------
echo ""
echo "=== [import_from_completeness_challenge] desktop runtime layer ==="

desktop_log="$(mktemp)"
echo "Desktop test log: ${desktop_log}"

desktop_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "digital.vasic.yole.import_.*" \
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

if (( desktop_passed < 30 )); then
    echo "FAIL [desktop-runtime]: only ${desktop_passed} PASSED — expected >= 30. See ${desktop_log}."
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
echo "=== [import_from_completeness_challenge] Robolectric layer ==="

robo_log="$(mktemp)"
echo "Robolectric test log: ${robo_log}"

robo_ok=0
if ./gradlew :androidApp:testDebugUnitTest \
    -PincludeRobolectric=true \
    --rerun-tasks \
    --tests "*Import*" \
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

if (( robo_passed < 8 )); then
    echo "FAIL [robolectric]: only ${robo_passed} PASSED — expected >= 8. See ${robo_log}."
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
echo "PASS: import_from_completeness_challenge — 14 files present + ${desktop_passed} desktop tests + ${robo_passed} Robolectric tests all PASSED (evidence: ${desktop_log}, ${robo_log})."
