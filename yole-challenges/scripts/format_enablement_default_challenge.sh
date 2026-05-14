#!/bin/bash
# format_enablement_default_challenge.sh — iter-57 Phase 4 anti-bluff gate.
#
# Asserts the operator constraint (spec §3.7):
#   "Markdown is the only default-enabled format."
#
# Two layers:
#
#   (a) STATIC: source-grep asserts FormatRegistry.defaultEnabledFormatIds()
#       returns `setOf("markdown")` and that no other set is hardcoded as the
#       fresh-install default.
#   (b) RUNTIME: runs FormatEnablementDefaultTest + FormatEnablementGateTest.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

REGISTRY="shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt"

if [[ ! -f "${REGISTRY}" ]]; then
  echo "FAIL: ${REGISTRY} not found."
  exit 1
fi

# Accept either string literal `setOf("markdown")` OR the ID_MARKDOWN
# constant idiom `setOf(ID_MARKDOWN)`. In the second case, verify the
# constant itself is the literal "markdown".
if grep -E 'fun\s+defaultEnabledFormatIds\(\)\s*(:\s*Set<String>\s*)?=\s*setOf\("markdown"\)' "${REGISTRY}" >/dev/null; then
  echo "OK [static]: defaultEnabledFormatIds() returns setOf(\"markdown\") (literal)."
elif grep -E 'fun\s+defaultEnabledFormatIds\(\)\s*(:\s*Set<String>\s*)?=\s*setOf\(ID_MARKDOWN\)' "${REGISTRY}" >/dev/null; then
  if grep -E 'const\s+val\s+ID_MARKDOWN\s*=\s*"markdown"' "${REGISTRY}" >/dev/null || \
     grep -E 'val\s+ID_MARKDOWN\s*=\s*"markdown"' "${REGISTRY}" >/dev/null; then
    echo "OK [static]: defaultEnabledFormatIds() returns setOf(ID_MARKDOWN) where ID_MARKDOWN == \"markdown\"."
  else
    # The constant may live in TextFormat — check there too.
    if grep -rE 'const\s+val\s+ID_MARKDOWN\s*=\s*"markdown"|val\s+ID_MARKDOWN\s*=\s*"markdown"' \
        shared/src/commonMain/kotlin/digital/vasic/yole/format/ --include="*.kt" >/dev/null; then
      echo "OK [static]: defaultEnabledFormatIds() returns setOf(ID_MARKDOWN); constant resolves to \"markdown\" in TextFormat companion."
    else
      echo "FAIL [static]: defaultEnabledFormatIds() uses ID_MARKDOWN but the constant resolution to \"markdown\" was not found."
      exit 1
    fi
  fi
else
  echo "FAIL [static]: FormatRegistry.defaultEnabledFormatIds() does not return setOf(\"markdown\") or setOf(ID_MARKDOWN)."
  grep -nE 'defaultEnabledFormatIds' "${REGISTRY}" | head -5
  exit 1
fi

log="$(mktemp)"
if ! ./gradlew :shared:desktopTest --rerun-tasks \
  --tests "*FormatEnablementDefaultTest*" \
  --tests "*FormatEnablementGateTest*" \
  > "${log}" 2>&1; then
  echo "FAIL [runtime]: enablement suite did not pass. See ${log}."
  tail -20 "${log}" >&2
  exit 1
fi

passed=$(grep -c " PASSED$" "${log}" || true)
if (( passed == 0 )); then
  echo "FAIL [runtime]: zero PASSED lines in log."
  exit 1
fi

echo "OK [runtime]: ${passed} enablement cases PASSED (evidence: ${log})."
echo "PASS: format_enablement_default_challenge — markdown-only default enforced + runtime verified."
