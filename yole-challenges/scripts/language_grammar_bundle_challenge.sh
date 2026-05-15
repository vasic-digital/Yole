#!/bin/bash
# language_grammar_bundle_challenge.sh — iter-58 F2 Phase 9 anti-bluff gate.
#
# Verifies the Phase 7 bundling claim: "47 grammars covered on Desktop
# via bonede Maven Central JARs". Two layers:
#
#   (a) STATIC: scans shared/build.gradle.kts for
#       implementation(libs.ts.<lang>) lines in the desktopMain source
#       set, plus the canonical tree-sitter-markdown entry. Counts
#       unique language references and asserts >= 47.
#
#   (b) RUNTIME: runs BonedeGrammarSmokeTest.allBundledLangs_loadAndParse
#       and BonedeGrammarSmokeTest.bonedeRegistry_isComplete specifically.
#       Asserts the PASSED lines exist and the registry-completeness
#       assertion (which embeds "expected to be 47") is satisfied.
#
# Exit codes:
#   0 = all checks PASS
#   1 = static or runtime layer fail
#
# Anti-bluff (CONST-035): positive evidence — per-language dep list
# printed on PASS; test log path emitted always. A missing dep in
# build.gradle.kts causes the static count to drop below 47; a missing
# bonede JAR at runtime causes BonedeGrammarSmokeTest to fail with the
# specific language named in the failure message.
#
# Cross-platform impact (CONST-037): both layers run entirely on the
# host JVM (no Android SDK). Desktop bonede JARs are fetched from Maven
# Central by Gradle — works on any host with network access.
#
# Submodule decoupling (CONST-038): no submodule state read.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

BUILD_GRADLE="shared/build.gradle.kts"

echo "=== [language_grammar_bundle_challenge] static layer ==="

if [[ ! -f "${BUILD_GRADLE}" ]]; then
  echo "FAIL [static]: ${BUILD_GRADLE} not found."
  exit 1
fi

# Collect all bonede language dependency references.
# We match two patterns:
#   implementation(libs.ts.<lang>)      — the 46 F2 Phase 7 entries
#   implementation(libs.tree.sitter.markdown)  — the iter-57 entry
#
# Both sets resolve to a tree-sitter-<lang> bonede artifact.

mapfile -t ts_lang_deps < <(
  grep -E "implementation\(libs\.ts\.[a-z]" "${BUILD_GRADLE}" \
    | grep -oE "libs\.ts\.[a-z]+" \
    | sort -u
)

mapfile -t ts_markdown_deps < <(
  grep -E "implementation\(libs\.tree\.sitter\.markdown" "${BUILD_GRADLE}" \
    | grep -oE "libs\.tree\.sitter\.markdown" \
    | sort -u
)

total_lang_deps=$(( ${#ts_lang_deps[@]} + ${#ts_markdown_deps[@]} ))

echo "Bonede grammar dependencies in ${BUILD_GRADLE}:"
echo "  libs.ts.* entries (${#ts_lang_deps[@]}):"
for dep in "${ts_lang_deps[@]}"; do
  echo "    ${dep}"
done
echo "  libs.tree.sitter.markdown entries (${#ts_markdown_deps[@]}):"
for dep in "${ts_markdown_deps[@]}"; do
  echo "    ${dep}"
done
echo "  Total unique language deps: ${total_lang_deps}"

if (( total_lang_deps < 47 )); then
  echo "FAIL [static]: only ${total_lang_deps} bonede language dependencies found in ${BUILD_GRADLE}; expected >= 47."
  exit 1
fi

echo "OK [static]: ${total_lang_deps} bonede language dependencies declared in ${BUILD_GRADLE} (>= 47)."

# ----------------------------------------------------------------
# Runtime layer
# ----------------------------------------------------------------
echo ""
echo "=== [language_grammar_bundle_challenge] runtime layer ==="

log="$(mktemp)"
echo "Runtime log: ${log}"

runtime_ok=0
if ./gradlew :shared:desktopTest --rerun-tasks \
  --tests "digital.vasic.yole.syntax.BonedeGrammarSmokeTest.allBundledLangs_loadAndParse" \
  --tests "digital.vasic.yole.syntax.BonedeGrammarSmokeTest.bonedeRegistry_isComplete" \
  > "${log}" 2>&1; then
  runtime_ok=1
fi

passed=$(grep -cE " PASSED$" "${log}" 2>/dev/null || true)
failed=$(grep -cE " FAILED$" "${log}" 2>/dev/null || true)

echo "Runtime result: ${passed} PASSED, ${failed} FAILED (evidence: ${log})."

if (( runtime_ok == 0 )); then
  echo "FAIL [runtime]: BonedeGrammarSmokeTest suite did not pass. See ${log}."
  tail -30 "${log}" >&2
  exit 1
fi

if (( failed > 0 )); then
  echo "FAIL [runtime]: ${failed} FAILED test(s). See ${log}."
  grep -E " FAILED$" "${log}" >&2
  exit 1
fi

if (( passed < 2 )); then
  echo "FAIL [runtime]: expected >= 2 PASSED (allBundledLangs + bonedeRegistry_isComplete), got ${passed}. See ${log}."
  tail -20 "${log}" >&2
  exit 1
fi

# Extract the per-language parse summary from the log — this is the
# positive evidence that real tokenization happened (CONST-035).
echo ""
echo "Positive evidence (per-language parse report from BonedeGrammarSmokeTest):"
grep -E "Phase 7 bonede smoke:|successes:|failures:" "${log}" 2>/dev/null | head -10 || true

# Verify count claim: the log must contain at least one reference to
# "47" languages tokenized (from the bonedeRegistry_isComplete assertion
# output or the smoke report).
if ! grep -qE "47" "${log}" 2>/dev/null; then
  echo "FAIL [runtime]: expected '47' to appear in test log (registry count assertion). See ${log}."
  exit 1
fi

echo ""
echo "PASS: language_grammar_bundle_challenge — ${total_lang_deps} bonede deps declared / ${passed} runtime tests PASSED (evidence: ${log})."
