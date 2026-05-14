#!/bin/bash
# syntax_highlighting_challenge.sh — iter-57 Phase 12 anti-bluff gate.
#
# Runs the syntax-highlighting test suites and emits positive PASSED-case
# evidence per CONST-035 (count + log path). No metadata-only PASS.
#
# Exit:
#   0 = all suites green; positive evidence emitted
#   1 = at least one suite failed
#   2 = test runner did not execute (compile failure, etc.)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

log_shared="$(mktemp)"
log_android="$(mktemp)"

# Shared (KMP) suites: parser, registry, scope mapper, theme parity,
# format gate, badge tinter, preview highlighter, syntax highlighter,
# source invariants.
if ! ./gradlew :shared:desktopTest --rerun-tasks \
  --tests "*VsCodeThemeParserTest*" \
  --tests "*ThemeRegistryTest*" \
  --tests "*ScopeMapperTest*" \
  --tests "*GrammarRegistryTest*" \
  --tests "*LegacyThemeParityTest*" \
  --tests "*ThemeUiCoverageTest*" \
  --tests "*FormatEnablementDefaultTest*" \
  --tests "*FormatEnablementGateTest*" \
  --tests "*BadgeTinterTest*" \
  --tests "*PreviewCodeBlockHighlighterTest*" \
  --tests "*SyntaxHighlighterTest*" \
  --tests "*SyntaxHighlightingSourceInvariantsTest*" \
  --tests "*TokenizerEngineJvmTest*" \
  > "${log_shared}" 2>&1; then
  echo "FAIL: shared:desktopTest suite did not pass. See ${log_shared}."
  tail -30 "${log_shared}" >&2
  exit 1
fi

shared_passed=$(grep -c " PASSED$" "${log_shared}" || true)

# Android Robolectric suites: editor highlighting, file editing,
# editor scroll sync, filename badges, formats settings, migration.
if ! ./gradlew :androidApp:testDebugUnitTest -PincludeRobolectric=true --rerun-tasks \
  --tests "*EditorScrollSync*" \
  --tests "*EditorHighlighting*" \
  --tests "*FilenameBadges*" \
  --tests "*FormatsSettingsScreen*" \
  --tests "*FormatMigrationDialog*" \
  --tests "*FileEditing*" \
  --tests "*FormatDetection*" \
  > "${log_android}" 2>&1; then
  echo "FAIL: androidApp:testDebugUnitTest suite did not pass. See ${log_android}."
  tail -30 "${log_android}" >&2
  exit 1
fi

android_passed=$(grep -c " PASSED$" "${log_android}" || true)

if (( shared_passed == 0 || android_passed == 0 )); then
  echo "FAIL: zero PASSED lines in one of the logs (suites=${shared_passed} shared / ${android_passed} android). Possible runner-skipped scenario."
  exit 1
fi

echo "OK: shared suites: ${shared_passed} PASSED (evidence: ${log_shared})."
echo "OK: android suites: ${android_passed} PASSED (evidence: ${log_android})."
echo "PASS: syntax_highlighting_challenge — total $((shared_passed + android_passed)) PASSED cases across shared + android."
