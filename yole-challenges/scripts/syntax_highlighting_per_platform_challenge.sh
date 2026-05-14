#!/bin/bash
# syntax_highlighting_per_platform_challenge.sh — iter-57 CONST-037 gate.
#
# Asserts that every TokenizerEngine `expect` declaration in commonMain
# has a corresponding `actual` in EACH of the 4 platform source sets:
#   - androidMain, desktopMain, iosMain, wasmJsMain
#
# Positive evidence on PASS: per-platform actual file paths + the
# count of `actual` keyword occurrences.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

COMMON_FILE="shared/src/commonMain/kotlin/digital/vasic/yole/syntax/TokenizerEngine.kt"

if [[ ! -f "${COMMON_FILE}" ]]; then
  echo "FAIL: ${COMMON_FILE} missing — TokenizerEngine expect declaration not found."
  exit 1
fi

if ! grep -q "expect class TokenizerEngine" "${COMMON_FILE}"; then
  echo "FAIL: ${COMMON_FILE} does not declare `expect class TokenizerEngine`."
  exit 1
fi

failed=0
for platform in android desktop ios wasmJs; do
  actual="shared/src/${platform}Main/kotlin/digital/vasic/yole/syntax/TokenizerEngine.${platform}.kt"
  if [[ ! -f "${actual}" ]]; then
    echo "FAIL: missing ${platform} actual at ${actual}"
    failed=1
    continue
  fi
  if ! grep -q "actual class TokenizerEngine" "${actual}"; then
    echo "FAIL: ${actual} does not declare 'actual class TokenizerEngine'."
    failed=1
    continue
  fi
  echo "OK:   ${platform} actual present: ${actual}"
done

if (( failed )); then
  echo "FAIL: syntax_highlighting_per_platform_challenge — one or more platform actuals missing."
  exit 1
fi

echo "PASS: syntax_highlighting_per_platform_challenge — all 4 TokenizerEngine actuals present."
