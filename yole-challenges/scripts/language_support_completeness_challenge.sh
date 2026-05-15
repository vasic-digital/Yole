#!/bin/bash
# language_support_completeness_challenge.sh — iter-58 F2 Phase 9 anti-bluff gate.
#
# Verifies that the 55-language LanguageMetadata manifest is complete
# and that the runtime test suites pass end-to-end. Two layers:
#
#   (a) STATIC: filesystem checks — every language in LanguageMetadata.all
#       must have 3 .scm files under grammars/<lang>/ AND a fixture file
#       under test-fixtures/<lang>/. Counts emitted per language.
#
#   (b) RUNTIME: runs :shared:desktopTest with the 7 iter-58 F2 test
#       filters and asserts >= 30 PASSED lines + 0 FAILED. Threshold
#       is 30 (not 55) because: the 7 test-class filters produce exactly
#       31 individual test PASSED lines today (2026-05-15):
#         - Feature2LanguageSmokeTest:      6 tests
#         - LanguageAffordanceParityTest:   8 tests
#         - LanguageMetadataCompletenessTest: 6 tests
#         - LanguageRegistryTest:           4 tests
#         - HtmlEmbeddedLangTest:           2 tests
#         - MarkdownCodeFencesTest:         2 tests
#         - BonedeGrammarSmokeTest:         3 tests
#       Total: 31 PASSED. We anchor at 30 to absorb 1-test volatility
#       while remaining meaningfully above any trivially-satisfied count.
#
# Exit codes:
#   0 = all checks PASS
#   1 = static layer fail
#   2 = runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted —
# per-language summary for the static layer and PASS count + log path
# for the runtime layer. No metadata-only PASS.
#
# Cross-platform impact (CONST-037): static layer is host-agnostic
# (pure filesystem). Runtime layer runs on the host JVM via
# :shared:desktopTest (no Android SDK required).
#
# Submodule decoupling (CONST-038): no submodule state is read or
# required. The script only drives Gradle and inspects checked-in
# source files.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

GRAMMARS_DIR="shared/src/commonMain/resources/grammars"
FIXTURES_DIR="shared/src/commonTest/resources/test-fixtures"
LANG_METADATA_KT="shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageMetadata.kt"

echo "=== [language_support_completeness_challenge] static layer ==="

# ----------------------------------------------------------------
# Guard: LanguageMetadata.kt must exist and declare >= 50 languages
# in val all: List<LanguageFormat>
# ----------------------------------------------------------------
if [[ ! -f "${LANG_METADATA_KT}" ]]; then
  echo "FAIL [static]: ${LANG_METADATA_KT} not found — LanguageMetadata missing."
  exit 1
fi

# Count val properties that are LanguageFormat(...) — each is one language entry.
# A safe proxy: count lines matching "val <id> = LanguageFormat(" in the file.
lang_props=$(grep -cE "^\s+val [a-z]+ = LanguageFormat\(" "${LANG_METADATA_KT}" 2>/dev/null || true)
if (( lang_props < 50 )); then
  echo "FAIL [static]: ${LANG_METADATA_KT} defines only ${lang_props} LanguageFormat properties; expected >= 50."
  exit 1
fi
echo "OK [static]: LanguageMetadata.kt defines ${lang_props} LanguageFormat properties (>= 50)."

# ----------------------------------------------------------------
# Per-language checks: .scm files + fixture
# ----------------------------------------------------------------
static_failures=()
static_ok_count=0

# Extract the list of language ids from the val all block:
# "    markdown, kotlin," etc. — pick up every lowercase identifier.
# The most reliable source is the val names themselves.
mapfile -t lang_ids < <(
  grep -E "^\s+val [a-z]+ = LanguageFormat\(" "${LANG_METADATA_KT}" \
    | grep -oE "val [a-z]+" \
    | awk '{print $2}'
)

if (( ${#lang_ids[@]} == 0 )); then
  echo "FAIL [static]: could not extract language ids from ${LANG_METADATA_KT}."
  exit 1
fi

echo ""
echo "Checking ${#lang_ids[@]} languages for .scm files + fixtures:"

for lang in "${lang_ids[@]}"; do
  lang_ok=1
  issues=()

  for scm in highlights folds outline; do
    scm_path="${GRAMMARS_DIR}/${lang}/${scm}.scm"
    if [[ ! -f "${scm_path}" ]]; then
      issues+=("  missing: ${scm_path}")
      lang_ok=0
    elif [[ ! -s "${scm_path}" ]]; then
      issues+=("  empty: ${scm_path}")
      lang_ok=0
    fi
  done

  fixture_dir="${FIXTURES_DIR}/${lang}"
  if [[ ! -d "${fixture_dir}" ]] || [[ -z "$(ls -A "${fixture_dir}" 2>/dev/null)" ]]; then
    issues+=("  missing fixture: ${fixture_dir}/ has no files")
    lang_ok=0
  fi

  if (( lang_ok )); then
    echo "  OK   ${lang}: grammars/{highlights,folds,outline}.scm + fixture present"
    (( static_ok_count += 1 )) || true
  else
    echo "  FAIL ${lang}:"
    for issue in "${issues[@]}"; do
      echo "${issue}"
    done
    static_failures+=("${lang}")
  fi
done

echo ""
echo "Static summary: ${static_ok_count}/${#lang_ids[@]} languages fully present."

if (( ${#static_failures[@]} > 0 )); then
  echo "FAIL [static]: missing resources for langs: ${static_failures[*]}"
  exit 1
fi
echo "OK [static]: all ${static_ok_count} languages have .scm files + fixtures."

# ----------------------------------------------------------------
# Runtime layer
# ----------------------------------------------------------------
echo ""
echo "=== [language_support_completeness_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
  --tests "*LanguageRegistryTest*" \
  --tests "*LanguageMetadataCompletenessTest*" \
  --tests "*LanguageAffordanceParityTest*" \
  --tests "*Feature2LanguageSmokeTest*" \
  --tests "*BonedeGrammarSmokeTest*" \
  --tests "*HtmlEmbeddedLangTest*" \
  --tests "*MarkdownCodeFencesTest*" \
  > "${log}" 2>&1; then
  runtime_ok=1
fi

# Count PASSED and FAILED lines regardless of exit code (some sub-tests
# may have been expected failures handled by the test runner).
passed=$(grep -cE " PASSED$" "${log}" 2>/dev/null || true)
failed=$(grep -cE " FAILED$" "${log}" 2>/dev/null || true)

echo "Runtime result: ${passed} PASSED, ${failed} FAILED (evidence: ${log})."

if (( runtime_ok == 0 )); then
  echo "FAIL [runtime]: :shared:desktopTest did not succeed. See ${log}."
  tail -30 "${log}" >&2
  exit 2
fi

if (( passed < 30 )); then
  echo "FAIL [runtime]: only ${passed} PASSED lines — expected >= 30. See ${log}."
  tail -30 "${log}" >&2
  exit 2
fi

if (( failed > 0 )); then
  echo "FAIL [runtime]: ${failed} FAILED test(s) detected. See ${log}."
  grep -E " FAILED$" "${log}" >&2
  exit 2
fi

echo ""
echo "PASS: language_support_completeness_challenge — ${#lang_ids[@]} langs / ${passed} tests PASSED (evidence: ${log})."
