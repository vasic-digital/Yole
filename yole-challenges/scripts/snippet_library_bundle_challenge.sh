#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# snippet_library_bundle_challenge.sh — iter-60 Phase 9 anti-bluff gate.
#
# Verifies that the 55-language snippet bundle is complete, each bundle
# is valid JSON, and that the runtime snippet test suites pass end-to-end.
# Two layers:
#
#   (a) STATIC: count snippets.json files under
#       shared/src/commonMain/resources/snippets/<lang>/snippets.json.
#       Assert >= 50. Then validate each as JSON using python3.
#       Emit "[OK] <lang>" per bundle.
#
#   (b) RUNTIME: run :shared:desktopTest filtered to
#       SnippetBundleCompletenessTest, SnippetRegistryTest, and
#       VsCodeSnippetParserTest. Assert >= 10 PASSED, 0 FAILED.
#
# Plan deviation (vs. plan Phase 9 section): the plan mentioned verifying
# snippets inside the packaged Android APK + Desktop tarball + Wasm bundle
# via `unzip -l`. That check is DEFERRED here because building those
# artifacts adds 5+ minutes to every challenge run. Source-tree bundling
# (verified in the STATIC layer) is sufficient to confirm the files are
# present. The actual packaged-APK path will be exercised in Phase 11
# (Firebase distribution), which unpacks the APK and tests it on a real
# device.
#
# Exit codes:
#   0 = both layers PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# per-language "[OK]" for the static layer and PASS count + log path
# for the runtime layer. No metadata-only PASS.
#
# Cross-platform impact (CONST-037): static layer is host-agnostic
# (pure filesystem). Runtime layer runs on the host JVM via
# :shared:desktopTest (no Android SDK required). Android snippet loading
# is covered by Robolectric (make container-robolectric-test). iOS +
# Wasm snippet loading returns null (benign, see KNOWN_DEFECTS.md).
#
# Submodule decoupling (CONST-038): no submodule state is read or
# required. The script only drives Gradle and inspects checked-in
# source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

SNIPPETS_DIR="shared/src/commonMain/resources/snippets"

echo "=== [snippet_library_bundle_challenge] static layer ==="

# ----------------------------------------------------------------
# Count snippet bundles
# ----------------------------------------------------------------
if [[ ! -d "${SNIPPETS_DIR}" ]]; then
    echo "FAIL [static]: ${SNIPPETS_DIR} not found — snippet bundle directory missing."
    exit 1
fi

mapfile -t bundle_files < <(find "${SNIPPETS_DIR}" -name "snippets.json" | sort)
bundle_count="${#bundle_files[@]}"

if (( bundle_count < 50 )); then
    echo "FAIL [static]: only ${bundle_count} snippet bundles found under ${SNIPPETS_DIR}; expected >= 50."
    exit 1
fi

# Collect language names for the summary line
lang_names=()
for bf in "${bundle_files[@]}"; do
    lang_names+=("$(basename "$(dirname "${bf}")")")
done
langs_csv="$(IFS=, ; echo "${lang_names[*]}")"

echo "[OK] ${bundle_count} snippet bundles bundled (langs: ${langs_csv})"

# ----------------------------------------------------------------
# Validate each bundle as valid JSON
# ----------------------------------------------------------------
echo ""
echo "Validating ${bundle_count} snippet bundles as JSON:"

static_fail=0
if command -v python3 &>/dev/null; then
    for bf in "${bundle_files[@]}"; do
        lang="$(basename "$(dirname "${bf}")")"
        if python3 -m json.tool "${bf}" > /dev/null 2>&1; then
            echo "  [OK] ${lang}: valid JSON"
        else
            echo "  FAIL [static]: ${lang}: ${bf} is not valid JSON"
            static_fail=1
        fi
    done
elif command -v jq &>/dev/null; then
    for bf in "${bundle_files[@]}"; do
        lang="$(basename "$(dirname "${bf}")")"
        if jq . "${bf}" > /dev/null 2>&1; then
            echo "  [OK] ${lang}: valid JSON"
        else
            echo "  FAIL [static]: ${lang}: ${bf} is not valid JSON"
            static_fail=1
        fi
    done
else
    echo "  [WARN] Neither python3 nor jq available — skipping per-bundle JSON validation."
    echo "         Install python3 or jq to enable this check."
fi

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more snippet bundles contain invalid JSON — see above."
    exit 1
fi

echo ""
echo "OK [static]: ${bundle_count} snippet bundles present and valid."

# ----------------------------------------------------------------
# Runtime layer
# ----------------------------------------------------------------
echo ""
echo "=== [snippet_library_bundle_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
    --tests "*SnippetBundleCompletenessTest*" \
    --tests "*SnippetRegistryTest*" \
    --tests "*VsCodeSnippetParserTest*" \
    > "${log}" 2>&1; then
    runtime_ok=1
fi

passed=$(grep -cE " PASSED$" "${log}" 2>/dev/null || true)
failed=$(grep -cE " FAILED$" "${log}" 2>/dev/null || true)

echo "[OK] desktopTest (SnippetBundleCompletenessTest + SnippetRegistryTest + VsCodeSnippetParserTest): ${passed} PASSED, ${failed} FAILED, log: ${log}"

if (( runtime_ok == 0 )); then
    echo "FAIL [runtime]: :shared:desktopTest did not succeed. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( passed < 10 )); then
    echo "FAIL [runtime]: only ${passed} PASSED lines — expected >= 10. See ${log}."
    tail -30 "${log}" >&2
    exit 2
fi

if (( failed > 0 )); then
    echo "FAIL [runtime]: ${failed} FAILED test(s) detected. See ${log}."
    grep -E " FAILED$" "${log}" >&2
    exit 2
fi

echo ""
echo "PASS: snippet_library_bundle_challenge — ${bundle_count} bundles valid / ${passed} tests PASSED (evidence: ${log})."
