#!/bin/bash
# tokenizer_android_real_tokens_challenge.sh
# iter-57 #android-tree-sitter-ndk-so-missing — RESOLVED.
#
# On-device anti-bluff verification of TokenizerEngine on Android.
# Closes the historical gap noted in docs/KNOWN_DEFECTS.md by running
# the real JNI binding against the Android NDK-built .so files
# packaged into the APK by shared/build.gradle.kts's
# repackageTreeSitterJarForAndroid task family.
#
# Exit:
#   0 = at least one instrumentation device available, the
#       TokenizerEngineAndroidTest cases ran, and >=3 PASSED lines
#       were emitted by the test runner (one per test method).
#   1 = the test ran but FAILED (anti-bluff regression caught).
#   2 = no adb-visible device → SKIP-OK per CONST-035 (operator
#       must spin up an emulator or plug in a real phone). Treated
#       as "skip" rather than "fail" because absence of hardware is
#       not a defect in the project.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

ADB="${ANDROID_HOME:-}/platform-tools/adb"
if [[ ! -x "${ADB}" ]]; then
  echo "SKIP-OK: adb not found at ${ADB} — no Android SDK install on this host."
  exit 2
fi

devices_csv="$("${ADB}" devices | awk 'NR>1 && $2=="device" {print $1}' | tr '\n' ',')"
if [[ -z "${devices_csv}" || "${devices_csv}" == "," ]]; then
  echo "SKIP-OK: no adb-visible Android device — run 'emulator -avd <name>'"
  echo "         or plug a real phone and re-run this challenge."
  exit 2
fi
echo "OK:   adb devices found: ${devices_csv%,}"

log="$(mktemp)"
if ! ./gradlew :androidApp:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=digital.vasic.yole.android.TokenizerEngineAndroidTest \
    > "${log}" 2>&1; then
  echo "FAIL: connectedDebugAndroidTest for TokenizerEngineAndroidTest did not succeed."
  echo "      See ${log}."
  tail -50 "${log}" >&2
  exit 1
fi

# AGP's instrumentation runner reports per-test results as
# "<class> > <method>[<device>] PASSED" — count them.
passed=$(grep -cE "TokenizerEngineAndroidTest.*PASSED" "${log}" || true)
failed=$(grep -cE "TokenizerEngineAndroidTest.*FAILED" "${log}" || true)

if (( failed > 0 )); then
  echo "FAIL: ${failed} TokenizerEngineAndroidTest cases FAILED on device. See ${log}."
  exit 1
fi
if (( passed < 3 )); then
  echo "FAIL: only ${passed} TokenizerEngineAndroidTest PASSED lines — expected >=3 (initializeSucceedsOnAndroidDevice, tokenizesMarkdownSnippetOnDevice, tokenizesReentrantOnSameEngine)."
  echo "      Log: ${log}"
  exit 1
fi

echo "PASS: tokenizer_android_real_tokens_challenge — ${passed} TokenizerEngineAndroidTest cases PASSED on device."
echo "      Evidence: ${log}"
