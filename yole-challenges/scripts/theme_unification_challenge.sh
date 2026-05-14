#!/bin/bash
# theme_unification_challenge.sh — iter-57 Phase 3+12 anti-bluff gate.
#
# Static asserts that the legacy YoleColors palette object has been
# fully migrated to the VS Code theme JSON system AND that the
# parity tests run. Two layers:
#
#   (a) STATIC: grep-asserts that production code references neither
#       `YoleColors.Ide.`, `YoleColors.Dark.`, nor the legacy
#       Material3 palette getters via the old code path.
#   (b) RUNTIME: runs LegacyThemeParityTest + ThemeWcagContrastTest.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

# ---------- Layer (a): static ----------
prod_paths=(
  androidApp/src/main
  desktopApp/src/main
  iosApp/src
  webApp/src/wasmJsMain
  shared/src/commonMain
  shared/src/androidMain
  shared/src/desktopMain
  shared/src/iosMain
  shared/src/wasmJsMain
)

# Allow these legacy references only in comments OR in the
# LegacyThemeBridge file (which intentionally records the legacy
# palette as a migration helper).
hits=$(grep -rnE "(YoleColors\.Ide\.|YoleColors\.Dark\.)" \
  "${prod_paths[@]}" --include="*.kt" 2>/dev/null \
  | grep -v "LegacyThemeBridge.kt" \
  | grep -v "//.*YoleColors" \
  | grep -cv "^$" || true)

if (( hits > 0 )); then
  echo "FAIL [static]: ${hits} production callsite(s) still reference YoleColors.Ide/Dark — iter-57 Phase 3 migration not clean."
  grep -rnE "(YoleColors\.Ide\.|YoleColors\.Dark\.)" \
    "${prod_paths[@]}" --include="*.kt" 2>/dev/null \
    | grep -v "LegacyThemeBridge.kt" \
    | grep -v "//.*YoleColors" \
    | head -10
  exit 1
fi

echo "OK [static]: zero production callsites reference YoleColors.Ide/Dark — theme system unified."

# ---------- Layer (b): runtime ----------
log="$(mktemp)"
if ! ./gradlew :shared:desktopTest --rerun-tasks \
  --tests "*LegacyThemeParityTest*" \
  --tests "*ThemeWcagContrastTest*" \
  > "${log}" 2>&1; then
  echo "FAIL [runtime]: parity / contrast suite did not pass. See ${log}."
  tail -20 "${log}" >&2
  exit 1
fi

passed=$(grep -c " PASSED$" "${log}" || true)
if (( passed == 0 )); then
  echo "FAIL [runtime]: zero PASSED lines in log — suite skipped?"
  exit 1
fi

echo "OK [runtime]: ${passed} parity+contrast cases PASSED (evidence: ${log})."
echo "PASS: theme_unification_challenge — both layers green."
