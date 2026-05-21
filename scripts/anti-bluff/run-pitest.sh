#!/usr/bin/env bash
# Phase 5A — real Pitest mutation run for the :shared desktop (JVM) KMP target.
#
# Modes:
#   full      mutate all of digital.vasic.yole.* (slow — baseline refresh).
#   changed   mutate only packages touched vs the default branch (gate mode).
#   <pkg>     mutate one explicit package glob, e.g. digital.vasic.yole.format.csv.*
#
# Output: shared/build/reports/pitest/<timestamp>/mutations.xml  (+ HTML).
set -euo pipefail

# Uses `mapfile`, a bash 4+ builtin. macOS default is bash 3.2 — install
# bash 4+ (e.g., `brew install bash`) and invoke via `/opt/homebrew/bin/bash`.
if (( BASH_VERSINFO[0] < 4 )); then
  echo "FAIL: run-pitest.sh requires bash 4+; current shell is ${BASH_VERSION}." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MODE="${1:-full}"
GRADLE="./gradlew"
# Absolute paths: Pitest matches --mutableCodePaths entries against
# --classPath entries; mixing relative and absolute forms makes the
# filter match nothing (0 mutation units). Keep both absolute.
CLASSES_MAIN="${ROOT_DIR}/shared/build/classes/kotlin/desktop/main"
CLASSES_TEST="${ROOT_DIR}/shared/build/classes/kotlin/desktop/test"
REPORT_DIR="shared/build/reports/pitest"

# 1. Compile desktop main + test classes and dump the two classpaths.
"${GRADLE}" :shared:desktopTestClasses :shared:pitestClasspath :shared:pitestToolClasspath -q

[[ -d "${CLASSES_MAIN}" ]] || { echo "FAIL: ${CLASSES_MAIN} missing — desktop main not compiled." >&2; exit 1; }
[[ -d "${CLASSES_TEST}" ]] || { echo "FAIL: ${CLASSES_TEST} missing — desktop test not compiled." >&2; exit 1; }

DEP_CP="$(cat shared/build/pitest-classpath.txt)"
PITEST_CP="$(cat shared/build/pitest-tool-classpath.txt)"
[[ -n "${DEP_CP}" ]]   || { echo "FAIL: empty Pitest dependency classpath." >&2; exit 1; }
[[ -n "${PITEST_CP}" ]] || { echo "FAIL: empty Pitest tool classpath." >&2; exit 1; }

# 2. Resolve target classes.
TARGET_TESTS="digital.vasic.yole.*"
case "${MODE}" in
  full)
    TARGET_CLASSES="digital.vasic.yole.*"
    ;;
  changed)
    DEFAULT_BRANCH="master"
    git rev-parse --verify main >/dev/null 2>&1 && DEFAULT_BRANCH="main"
    # Map changed .kt files to production package globs. BOTH production
    # source sets (commonMain/desktopMain) AND test source sets
    # (commonTest/desktopTest) are included: a weakened test is the
    # primary bluff vector, and a test package mirrors its production
    # package, so a changed test file points at the production package
    # whose kill rate must be re-checked.
    mapfile -t CHANGED < <(
      { git diff --name-only "${DEFAULT_BRANCH}"...HEAD; git diff --name-only --cached; } \
      | grep -E '^shared/src/(commonMain|desktopMain|commonTest|desktopTest)/kotlin/.*\.kt$' || true
    )
    if [[ ${#CHANGED[@]} -eq 0 ]]; then
      echo "OK: no changed shared production/test .kt files vs ${DEFAULT_BRANCH} — nothing to mutate."
      exit 0
    fi
    PKGS=()
    for f in "${CHANGED[@]}"; do
      pkg="$(dirname "${f}" \
        | sed -E 's#^shared/src/(commonMain|desktopMain|commonTest|desktopTest)/kotlin/##; s#/#.#g')"
      PKGS+=("${pkg}.*")
    done
    TARGET_CLASSES="$(printf '%s\n' "${PKGS[@]}" | sort -u | paste -sd',' -)"
    echo "Changed-mode target packages: ${TARGET_CLASSES}"
    ;;
  digital.vasic.yole.*)
    TARGET_CLASSES="${MODE}"
    ;;
  *)
    echo "FAIL: unknown mode '${MODE}' (use: full | changed | <package glob>)." >&2
    exit 1
    ;;
esac

# 3. Invoke Pitest.
RUN_DIR="${REPORT_DIR}/$(date +%Y%m%d-%H%M%S)"
mkdir -p "${RUN_DIR}"

set +e
# Pitest's --classPath takes a COMMA-separated list (its --help: "coma
# separated list of additional classpath elements"). DEP_CP is a normal
# colon-separated Java classpath, so translate ':' -> ',' for this arg
# only. The `java -cp` launch classpath above stays colon-separated.
# --skipFailingTests: ~21 timing/environment-sensitive tests pass under
# Gradle's JVM but fail in Pitest's forked minion JVM; skipping them
# (rather than aborting the whole run) yields an honest kill-rate floor
# from the Pitest-runnable subset. Skipped tests are listed in the run log.
# --mutableCodePaths: production and test classes share the
# digital.vasic.yole.* namespace, so --targetClasses alone would also
# mutate test classes — and a test mutating its own body trivially
# "kills" itself, producing meaningless inflated data. Restricting
# mutable code to the production main classes dir mutates ONLY
# production code; the test classes dir stays on --classPath so tests
# can still run.
java -cp "${PITEST_CP}" org.pitest.mutationtest.commandline.MutationCoverageReport \
  --reportDir "${RUN_DIR}" \
  --targetClasses "${TARGET_CLASSES}" \
  --targetTests "${TARGET_TESTS}" \
  --sourceDirs "shared/src/commonMain/kotlin,shared/src/desktopMain/kotlin" \
  --classPath "${CLASSES_MAIN},${CLASSES_TEST},${DEP_CP//:/,}" \
  --mutableCodePaths "${CLASSES_MAIN}" \
  --outputFormats XML,HTML \
  --testPlugin junit \
  --skipFailingTests true \
  --timeoutConst 12000 \
  --threads 4 \
  --verbose false
PITEST_RC=$?
set -e

XML="$(find "${RUN_DIR}" -name mutations.xml -print -quit)"
if [[ -z "${XML}" ]]; then
  echo "FAIL: Pitest produced no mutations.xml (rc=${PITEST_RC})." >&2
  exit 1
fi

# Stable symlink for the challenge to consume.
ln -sfn "$(cd "$(dirname "${XML}")" && pwd)" "${REPORT_DIR}/latest"
echo "OK: mutations.xml at ${XML}"
echo "${XML}"
exit 0
