#!/bin/bash
# scroll_sync_challenge.sh — iter-55 anti-bluff gate for the
# Android editor gutter/text scroll synchronisation fix.
#
# Two layers of evidence are produced on PASS:
#   (a) STATIC — the SyncedScrollEditor source declares exactly one
#       rememberScrollState() (non-comment) and both gutter +
#       BasicTextField verticalScroll() calls reference the same
#       variable.
#   (b) RUNTIME — the EditorScrollSyncRobolectricTest passes under
#       :androidApp:testDebugUnitTest -PincludeRobolectric=true.
#
# Per CONST-035, layers (a) and (b) together carry positive evidence
# that the bug fixed in iter-55 (line numbers desyncing from text on
# vertical scroll) is not regressed.
#
# Exit:
#   0 = both layers PASS
#   1 = static layer FAIL (source-level invariant broken)
#   2 = runtime layer FAIL (Robolectric test broken)
#   3 = SyncedScrollEditor.kt missing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

EDITOR_SRC="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
if [[ ! -f "${EDITOR_SRC}" ]]; then
  echo "FAIL: ${EDITOR_SRC} missing — iter-55 introduced SyncedScrollEditor as the canonical editor surface."
  exit 3
fi

# ---------- Layer (a): static structural invariant ----------
# Strip block comments and line comments, then count rememberScrollState() calls.
code_only=$(perl -0pe 's|/\*.*?\*/||sg' "${EDITOR_SRC}" | sed 's://.*::')
remember_count=$(echo "${code_only}" | grep -c "rememberScrollState()" || true)
if [[ "${remember_count}" != "1" ]]; then
  echo "FAIL [static]: SyncedScrollEditor.kt declares ${remember_count} rememberScrollState() calls in non-comment code; iter-55 requires exactly 1 (shared between gutter and editor)."
  exit 1
fi

# Both verticalScroll() arguments must reference the same variable.
mapfile -t scroll_args < <(echo "${code_only}" \
  | grep -oE "\.verticalScroll\([a-zA-Z_][a-zA-Z0-9_]*\)" \
  | sed 's/^\.verticalScroll(//;s/)$//')
if (( ${#scroll_args[@]} < 2 )); then
  echo "FAIL [static]: SyncedScrollEditor.kt must contain at least 2 verticalScroll(variable) calls (gutter + editor); found ${#scroll_args[@]}."
  exit 1
fi
distinct=$(printf '%s\n' "${scroll_args[@]}" | sort -u | wc -l | tr -d ' ')
if [[ "${distinct}" != "1" ]]; then
  echo "FAIL [static]: gutter and editor verticalScroll() calls reference DIFFERENT variables: ${scroll_args[*]}"
  exit 1
fi

echo "OK [static]: SyncedScrollEditor.kt declares 1 rememberScrollState() and shares it across both verticalScroll() calls."

# ---------- Layer (b): runtime Robolectric verification ----------
log="$(mktemp)"
echo "Running EditorScrollSyncRobolectricTest (log: ${log})..."
if ! ./gradlew :androidApp:testDebugUnitTest \
      --tests "digital.vasic.yole.android.robolectric.EditorScrollSyncRobolectricTest" \
      -PincludeRobolectric=true --rerun-tasks > "${log}" 2>&1; then
  echo "FAIL [runtime]: EditorScrollSyncRobolectricTest did not pass. See ${log}."
  tail -30 "${log}" >&2
  exit 2
fi

# Positive evidence: at least one PASSED line referring to the test class.
if ! grep -q "EditorScrollSyncRobolectricTest .* PASSED" "${log}"; then
  echo "FAIL [runtime]: test runner produced no PASSED evidence for EditorScrollSyncRobolectricTest. See ${log}."
  exit 2
fi

passed_count=$(grep -c "EditorScrollSyncRobolectricTest > .* PASSED" "${log}" || true)
echo "OK [runtime]: ${passed_count} EditorScrollSyncRobolectricTest case(s) PASSED (evidence: ${log})."

echo "PASS: scroll_sync_challenge — both static and runtime layers green."
