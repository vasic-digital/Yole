<!--
SPDX-License-Identifier: CC0-1.0
-->
# Phase 5 — Full Anti-Bluff Re-Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove every existing Yole test and Challenge genuinely exercises the user-visible behavior it claims to verify (CONST-035 / CONST-039), and make the `mutation_ratchet` gate a real Pitest-backed regression guard instead of a stub.

**Architecture:** Two natures of work. (A) **Machinery** — wire real Pitest mutation testing for the `:shared` desktop (JVM) KMP target, then rewrite `mutation_ratchet_challenge.sh` to consume real `mutations.xml`. This gives an objective, re-runnable bluff signal for the JVM-runnable unit tests. (B) **Manual sweep** — subagent-driven mutation-verification audit of every test category Pitest cannot reach (Compose UI, Robolectric, `wasmJsTest`, `iosTest`, structural source-grep tests) plus all 39 challenge scripts. Audit tasks are *report-only* and produce a fixed-schema findings file; confirmed bluffs become appended fix tasks, each TDD'd and reviewed.

**Tech Stack:** Pitest 1.15.0 (`pitest-command-line`, JUnit4 test plugin), Kotlin Multiplatform / Gradle 8.13, bash 4+ challenge scripts, the existing `scripts/anti-bluff/` scanner harness.

**Epic context:** This is Phase 5 of 7 in `docs/superpowers/specs/2026-05-20-codegraph-integration-and-anti-bluff-epic-design.md`. Phases 0–4 are COMPLETE. Phase 5 ends at an operator checkpoint before Phase 6 (governance propagation).

---

## Operator decisions locked (2026-05-20)

- **Audit method:** Hybrid — real Pitest for JVM-runnable unit tests + manual subagent sweep for the rest.
- **`mutation_ratchet` gate:** Make it real this phase (replace the stub with a real Pitest invocation).

## Scope

**In scope** — Yole-owned tests and challenges:

| Bucket | Count | Pitest-reachable? |
|---|---|---|
| `shared/src/commonTest` | 273 files | Partly (runs on desktop JVM; structural-grep tests are not) |
| `shared/src/desktopTest` | 71 files | Yes (JVM) |
| `shared/src/wasmJsTest` | 5 files | No (WASM) — manual |
| `shared/src/iosTest` | 1 file | No (Native) — manual |
| `androidApp` tests | 56 files | No (Robolectric/Compose) — manual |
| `desktopApp` tests | 15 files | Partly — manual |
| `webApp` tests | 5 files | No (WASM) — manual |
| `yole-challenges/scripts/*.sh` | 39 scripts | Manual (CONST-039 evidence audit) |

**Out of scope (CONST-038):** `HelixQA/tools/opensource/leakcanary/**` — third-party upstream. Owned-submodule test suites (Challenges, Containers, HelixQA, LLMProvider, Security, HelixDevelopment/*) — their governance is Phase 6; their own anti-bluff audits are tracked by each submodule, not by this Yole-repo phase.

## Cross-platform impact (CONST-037)

This phase changes **test and gate infrastructure only** — no `shared/`, `*App/`, UI, navigation, or public-API source changes are made by the machinery tasks. Audit-discovered fixes to *production* source (if any bluff turns out to be a real product defect, per the operator covenant) MUST each carry their own per-platform reasoning in their fix-task commit body. The machinery commit body states: `Android/Desktop/iOS/Web: unaffected — test-infrastructure only`.

## File Structure

**Created:**

- `scripts/anti-bluff/run-pitest.sh` — resolves the `:shared` desktop-test classpath and invokes Pitest in `full` or `changed` mode. One responsibility: produce `shared/build/reports/pitest/.../mutations.xml`.
- `docs/superpowers/audits/phase-5/README.md` — index of audit batches + their status.
- `docs/superpowers/audits/phase-5/<batch>.md` — one findings file per audit batch (fixed schema, see Task 10).
- `scripts/anti-bluff/tests/fixtures/SurvivingMutantBluff.kt` — self-test fixture: a unit-under-test + a test that does NOT kill an obvious mutant. Proves the rewritten gate FAILs on real bluff.

**Modified:**

- `gradle/libs.versions.toml` — add `pitest` version + `pitest-command-line` library coord.
- `shared/build.gradle.kts` — add a `pitestTool` configuration and two classpath-dump tasks.
- `yole-challenges/scripts/mutation_ratchet_challenge.sh` — replace the stub body with a real `mutations.xml` parser + Section-2 ratchet.
- `yole-challenges/baselines/bluff-baseline.txt` — populate Section 2 with the real per-package kill rates from the first full run.
- `Makefile` — `anti-bluff-mutation` runs the gate in `changed` mode; new `mutation-full` target runs the baseline-refresh full run.
- `docs/CONTINUATION.md` — incremental progress per CONST-036.
- `docs/KNOWN_DEFECTS.md` — register any discovered-but-deferred bluff with a tracker id.

---

# Phase 5A — Pitest mutation infrastructure

### Task 1: Add Pitest dependency coordinates

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add the version pin**

In `gradle/libs.versions.toml`, under `[versions]`, add (keep alphabetical near `pdfbox`/existing entries):

```toml
pitest = "1.15.0"
```

- [ ] **Step 2: Add the library coord**

Under `[libraries]`, add:

```toml
pitest-command-line = { module = "org.pitest:pitest-command-line", version.ref = "pitest" }
```

- [ ] **Step 3: Verify the catalog still resolves**

Run: `./gradlew :shared:help -q`
Expected: no `Invalid TOML catalog` error; task completes.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build(anti-bluff): add Pitest 1.15.0 dependency coordinate

Phase 5A — mutation-testing infrastructure for the CONST-039 re-audit.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure dependency only."
```

### Task 2: Add the `pitestTool` configuration + classpath-dump tasks

**Files:**
- Modify: `shared/build.gradle.kts` (append a new block near the existing custom-task region around line 505)

- [ ] **Step 1: Add the configuration and tasks**

Append to `shared/build.gradle.kts` (after the existing benchmark-compilation block, top level of the file — NOT inside `kotlin { }`):

```kotlin
// === Phase 5A — Pitest mutation-testing wiring ===
// Pitest is a JVM tool; it runs against the compiled `desktop` KMP JVM target.
// We do not use the info.solidsoft.pitest Gradle plugin: it auto-wires to the
// `java` plugin's source sets, which a KMP module does not have. Instead we
// dump two classpaths to files and drive the pitest-command-line jar from
// scripts/anti-bluff/run-pitest.sh.
val pitestTool: Configuration by configurations.creating

dependencies {
    pitestTool(libs.pitest.command.line)
}

tasks.register("pitestToolClasspath") {
    description = "Writes the resolved Pitest tool classpath to a file."
    group = "verification"
    val outFile = layout.buildDirectory.file("pitest-tool-classpath.txt")
    val cfg = configurations["pitestTool"]
    inputs.files(cfg)
    outputs.file(outFile)
    doLast {
        outFile.get().asFile.writeText(cfg.files.joinToString(":") { it.absolutePath })
    }
}

tasks.register("pitestClasspath") {
    description = "Writes the :shared desktop test runtime classpath to a file."
    group = "verification"
    val outFile = layout.buildDirectory.file("pitest-classpath.txt")
    val testCompilation = kotlin.targets.getByName("desktop")
        .compilations.getByName("test")
    inputs.files(testCompilation.runtimeDependencyFiles ?: files())
    outputs.file(outFile)
    doLast {
        val files = testCompilation.runtimeDependencyFiles ?: files()
        outFile.get().asFile.writeText(files.joinToString(":") { it.absolutePath })
    }
}
```

- [ ] **Step 2: Run the tool-classpath task**

Run: `./gradlew :shared:pitestToolClasspath -q && cat shared/build/pitest-tool-classpath.txt | tr ':' '\n' | grep -c pitest`
Expected: a count ≥ 3 (pitest-command-line pulls `pitest`, `pitest-entry`, `pitest-command-line`).

- [ ] **Step 3: Run the test-classpath task**

Run: `./gradlew :shared:pitestClasspath -q && wc -c < shared/build/pitest-classpath.txt`
Expected: a byte count > 1000 (the runtime classpath is many jars).
If the task fails with an unresolved `runtimeDependencyFiles` symbol, the KMP/Gradle API differs in this version — fall back to `testCompilation.runtimeDependencyFiles` → `(testCompilation as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationToRunnableFiles<*>).runtimeDependencyFiles` or resolve `configurations["desktopTestRuntimeClasspath"]` directly. Adjust, re-run, expect the same byte count.

- [ ] **Step 4: Commit**

```bash
git add shared/build.gradle.kts
git commit -m "build(anti-bluff): add Pitest classpath-dump tasks for :shared desktop target

Phase 5A — pitestToolClasspath + pitestClasspath dump the tool and
desktop-test runtime classpaths so run-pitest.sh can drive the
pitest-command-line jar without the JVM-only Gradle plugin.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure only."
```

### Task 3: Create `run-pitest.sh`

**Files:**
- Create: `scripts/anti-bluff/run-pitest.sh`

- [ ] **Step 1: Write the script**

```bash
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
CLASSES_MAIN="shared/build/classes/kotlin/desktop/main"
CLASSES_TEST="shared/build/classes/kotlin/desktop/test"
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
    # Map changed shared commonMain/desktopMain .kt files to package globs.
    mapfile -t CHANGED < <(
      { git diff --name-only "${DEFAULT_BRANCH}"...HEAD; git diff --name-only --cached; } \
      | grep -E '^shared/src/(commonMain|desktopMain)/kotlin/.*\.kt$' || true
    )
    if [[ ${#CHANGED[@]} -eq 0 ]]; then
      echo "OK: no changed shared production .kt files vs ${DEFAULT_BRANCH} — nothing to mutate."
      exit 0
    fi
    PKGS=()
    for f in "${CHANGED[@]}"; do
      pkg="$(dirname "${f}" \
        | sed -E 's#^shared/src/(commonMain|desktopMain)/kotlin/##; s#/#.#g')"
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
# --classPath takes a COMMA-separated list (Pitest --help); translate
# the colon-separated Java classpath for this arg only. --skipFailingTests
# tolerates the ~21 timing-sensitive tests that fail in Pitest's forked
# minion JVM but pass under Gradle — yields an honest floor from the
# Pitest-runnable subset.
java -cp "${PITEST_CP}" org.pitest.mutationtest.commandline.MutationCoverageReport \
  --reportDir "${RUN_DIR}" \
  --targetClasses "${TARGET_CLASSES}" \
  --targetTests "${TARGET_TESTS}" \
  --sourceDirs "shared/src/commonMain/kotlin,shared/src/desktopMain/kotlin" \
  --classPath "${CLASSES_MAIN},${CLASSES_TEST},${DEP_CP//:/,}" \
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
```

- [ ] **Step 2: Make it executable**

Run: `chmod +x scripts/anti-bluff/run-pitest.sh`

- [ ] **Step 3: Commit**

```bash
git add scripts/anti-bluff/run-pitest.sh
git commit -m "build(anti-bluff): add run-pitest.sh — full/changed/package mutation runner

Phase 5A. Drives pitest-command-line against the :shared desktop JVM
target. changed-mode maps git-diffed commonMain/desktopMain .kt files
to package globs so the routine gate stays fast.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure only."
```

### Task 4: Smoke-run Pitest on one package

**Files:** none (verification task)

- [ ] **Step 1: Pick a small, pure package**

Run: `ls shared/src/commonMain/kotlin/digital/vasic/yole/format/csv/`
Expected: a handful of `.kt` files (CSV parser — pure logic, no platform deps).

- [ ] **Step 2: Run Pitest scoped to that package**

Run: `bash scripts/anti-bluff/run-pitest.sh digital.vasic.yole.format.csv.*`
Expected: ends with `OK: mutations.xml at shared/build/reports/pitest/<ts>/mutations.xml`.

- [ ] **Step 3: Inspect the report for real signal**

Run: `grep -oE 'status="[A-Z_]+"' shared/build/reports/pitest/latest/mutations.xml | sort | uniq -c`
Expected: a mix — at least some `status="KILLED"` AND the run found mutations (total > 0). A run with `0` mutations means the classpath or `--targetClasses` glob is wrong — fix `run-pitest.sh` / Task 2 classpath resolution and re-run.

- [ ] **Step 4: Record the smoke result** (no commit — `shared/build/` is git-ignored)

Note the KILLED/SURVIVED/NO_COVERAGE counts in the Task 5 baseline.

### Task 5: Establish Section 2 of the baseline

**Files:**
- Modify: `yole-challenges/baselines/bluff-baseline.txt`

- [ ] **Step 1: Run the full mutation pass**

Run: `bash scripts/anti-bluff/run-pitest.sh full 2>&1 | tee shared/build/reports/pitest-full.log`
Expected: completes (may take 30–60+ min) with `OK: mutations.xml`. If it OOMs or times out, re-run per top-level package and concatenate — Pitest appends are not supported, so instead run each `digital.vasic.yole.<pkg>.*` separately and record each package's rate.

- [ ] **Step 2: Compute per-package kill rates**

Run:
```bash
python3 - <<'PY'
import xml.etree.ElementTree as ET, collections, glob
xml = sorted(glob.glob('shared/build/reports/pitest/*/mutations.xml'))[-1]
pkg = collections.Counter(); killed = collections.Counter()
for m in ET.parse(xml).getroot():
    cls = m.findtext('mutatedClass') or ''
    # Package = the FQCN minus its last segment (the class name). This
    # keeps per-format granularity (digital.vasic.yole.format.csv), so a
    # weak CSV suite cannot hide behind a strong Markdown suite.
    p = '.'.join(cls.split('.')[:-1])
    pkg[p] += 1
    if m.get('status') == 'KILLED': killed[p] += 1
for p in sorted(pkg):
    print(f"{p}:{killed[p]}/{pkg[p]}:{killed[p]*100//max(pkg[p],1)}")
PY
```
Expected: a per-package list like `digital.vasic.yole.format.csv:142/151:94`.

- [ ] **Step 3: Write Section 2**

Replace the Section 2 placeholder block in `yole-challenges/baselines/bluff-baseline.txt` (the lines between `# === SECTION 2:` and `# === SECTION 3:`) with the real data. Format, one line per package:

```
# === SECTION 2: PER-FILE MUTATION KILL RATES ===
# format <package>:<killed>/<total>:<floor-percent>. The gate ratchets:
# a package's kill rate may not drop below the recorded floor.
digital.vasic.yole.format.csv:142/151:94
digital.vasic.yole.format.markdown:...:...
# ... (one line per package from the full run)
```

Use the *measured* rate as each package's floor (the integer percent, no `%` sign — the gate parser accepts either). Do not round up — the floor is the honest current state; later tasks raise it as bluffs are fixed.

- [ ] **Step 4: Commit**

```bash
git add yole-challenges/baselines/bluff-baseline.txt
git commit -m "test(anti-bluff): populate bluff-baseline Section 2 with real Pitest kill rates

Phase 5A — first full mutation pass over :shared. Section 2 records the
honest per-package kill-rate floor; the mutation_ratchet gate now
ratchets against it.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure only."
```

---

# Phase 5B — Make `mutation_ratchet` a real gate

### Task 6: Add the surviving-mutant self-test fixture

**Files:**
- Create: `scripts/anti-bluff/tests/fixtures/SurvivingMutantBluff.kt`

This fixture is the anti-bluff proof for the gate itself: the rewritten challenge MUST be demonstrably able to FAIL.

- [ ] **Step 1: Write the fixture**

```kotlin
// SPDX-License-Identifier: CC0-1.0
// Anti-bluff self-test fixture (Phase 5B). NOT compiled into the build —
// scanner fixtures dir is excluded. Documents the shape of a test that
// leaves an obvious arithmetic mutant alive: `add` could be mutated to
// subtraction and `testAdd` would still pass because 0 + 0 == 0 - 0.
package fixtures

object SurvivingMutantBluff {
    fun add(a: Int, b: Int): Int = a + b
}

class SurvivingMutantBluffTest {
    // BLUFF: this test never distinguishes + from -, *, or a constant.
    fun testAdd() {
        check(SurvivingMutantBluff.add(0, 0) == 0)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scripts/anti-bluff/tests/fixtures/SurvivingMutantBluff.kt
git commit -m "test(anti-bluff): add surviving-mutant fixture for the ratchet self-test

Phase 5B — documents a test that leaves an arithmetic mutant alive,
used to prove mutation_ratchet_challenge.sh can genuinely FAIL.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure only."
```

### Task 7: Rewrite `mutation_ratchet_challenge.sh`

**Files:**
- Modify: `yole-challenges/scripts/mutation_ratchet_challenge.sh` (full rewrite)

- [ ] **Step 1: Replace the stub body with the real challenge**

```bash
#!/usr/bin/env bash
# CONST-035 / CONST-039 mutation ratchet challenge — Yole main (Kotlin/Pitest).
#
# Runs a real Pitest mutation pass and ratchets per-package kill rates
# against bluff-baseline.txt Section 2. A package whose kill rate drops
# below its recorded floor FAILs the gate.
#
# Mode (env MUTATION_MODE, default `changed`):
#   changed  mutate only packages touched vs the default branch (fast gate).
#   full     mutate everything (baseline refresh — `make mutation-full`).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BASELINE="${ROOT_DIR}/yole-challenges/baselines/bluff-baseline.txt"
MODE="${MUTATION_MODE:-changed}"
LOG="${ROOT_DIR}/qa-results/mutation_ratchet.log"
mkdir -p "$(dirname "${LOG}")"

if [[ ! -f "${BASELINE}" ]]; then
  echo "FAIL: baseline file missing at ${BASELINE}" >&2
  exit 1
fi
# Structural integrity check retained from the stub.
for marker in 'SECTION 1' 'SECTION 2' 'SECTION 3'; do
  grep -q "^# === ${marker}" "${BASELINE}" \
    || { echo "FAIL: baseline malformed (missing ${marker})." >&2; exit 1; }
done

echo "[mutation_ratchet] mode=${MODE} — running Pitest..." | tee "${LOG}"
if ! bash "${ROOT_DIR}/scripts/anti-bluff/run-pitest.sh" "${MODE}" >>"${LOG}" 2>&1; then
  echo "FAIL: Pitest run failed — see ${LOG}" >&2
  exit 1
fi

# changed-mode with no changed files: run-pitest.sh exits 0 with no report.
XML="${ROOT_DIR}/shared/build/reports/pitest/latest/mutations.xml"
if [[ ! -f "${XML}" ]]; then
  if grep -q 'nothing to mutate' "${LOG}"; then
    echo "OK: mutation ratchet — no changed production code to mutate."
    exit 0
  fi
  echo "FAIL: no mutations.xml produced — see ${LOG}" >&2
  exit 1
fi

# Parse current per-package rates and ratchet against Section 2 floors.
RESULT="$(python3 - "${XML}" "${BASELINE}" <<'PY'
import sys, collections, xml.etree.ElementTree as ET
xml_path, baseline_path = sys.argv[1], sys.argv[2]

total = collections.Counter(); killed = collections.Counter()
for m in ET.parse(xml_path).getroot():
    cls = m.findtext('mutatedClass') or ''
    pkg = '.'.join(cls.split('.')[:-1])  # FQCN minus class name = package
    total[pkg] += 1
    if m.get('status') == 'KILLED':
        killed[pkg] += 1

floors = {}
in_s2 = False
for line in open(baseline_path):
    if line.startswith('# === SECTION 2'): in_s2 = True; continue
    if line.startswith('# === SECTION 3'): in_s2 = False; continue
    if not in_s2 or line.startswith('#') or not line.strip(): continue
    pkg, _ratio, floor = line.strip().rsplit(':', 2)
    floors[pkg] = int(floor.rstrip('%'))

failed = False
for pkg in sorted(total):
    rate = killed[pkg] * 100 // max(total[pkg], 1)
    floor = floors.get(pkg)
    if floor is None:
        print(f"WARN  {pkg}: {rate}% — no Section-2 floor (new package; add it).")
        continue
    mark = 'PASS' if rate >= floor else 'FAIL'
    if mark == 'FAIL': failed = True
    print(f"{mark}  {pkg}: {rate}% (floor {floor}%, {killed[pkg]}/{total[pkg]})")

sys.exit(1 if failed else 0)
PY
)" && RC=0 || RC=$?

echo "${RESULT}" | tee -a "${LOG}"
if [[ "${RC}" -ne 0 ]]; then
  echo "FAIL: a package kill rate dropped below its baseline floor. Log: ${LOG}" >&2
  exit 1
fi
echo "OK: mutation ratchet — all packages at or above baseline floor. Log: ${LOG}"
exit 0
```

- [ ] **Step 2: Verify it PASSes clean on master**

Run: `MUTATION_MODE=changed bash yole-challenges/scripts/mutation_ratchet_challenge.sh`
Expected (on a clean master with no working changes): `OK: mutation ratchet — no changed production code to mutate.`

- [ ] **Step 3: Verify it can FAIL — inject a real surviving mutant**

Pick a small tested function and weaken its test, OR temporarily lower a Section-2 floor's measured package by editing a production file so a test stops killing a mutant. Concretely: in `shared/src/commonMain/kotlin/digital/vasic/yole/format/csv/` change one production method to a constant return, then:

Run: `MUTATION_MODE=full bash scripts/anti-bluff/run-pitest.sh digital.vasic.yole.format.csv.* >/dev/null 2>&1; MUTATION_MODE=changed bash yole-challenges/scripts/mutation_ratchet_challenge.sh`
Expected: `FAIL: a package kill rate dropped below its baseline floor.`
Then `git checkout -- shared/...` to revert the injected mutant and re-run Step 2 → expect OK again.

- [ ] **Step 4: Commit**

```bash
git add yole-challenges/scripts/mutation_ratchet_challenge.sh
git commit -m "test(anti-bluff): make mutation_ratchet a real Pitest-backed gate

Phase 5B — replaces the sub-project-1 stub. The challenge now runs a
real Pitest pass and ratchets per-package kill rates against
bluff-baseline.txt Section 2. Verified: FAILs on an injected surviving
mutant, PASSes clean. Closes the CONST-039 defect that the gate itself
was a bluff.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — test-infrastructure only."
```

### Task 8: Wire gate modes into the Makefile

**Files:**
- Modify: `Makefile`

- [ ] **Step 1: Update the mutation targets**

Replace the `anti-bluff-mutation` and `anti-bluff-mutation-changed` recipes:

```makefile
anti-bluff-mutation:
	@MUTATION_MODE=changed bash yole-challenges/scripts/mutation_ratchet_challenge.sh

mutation-full:
	@MUTATION_MODE=full bash yole-challenges/scripts/mutation_ratchet_challenge.sh
```

Remove `anti-bluff-mutation-changed` (now redundant — `anti-bluff-mutation` is changed-mode) and drop it from the `.PHONY` line; add `mutation-full` to `.PHONY`.

- [ ] **Step 2: Verify qa-all still references the gate**

Run: `grep -n 'anti-bluff:' Makefile`
Expected: `anti-bluff: anti-bluff-scan anti-bluff-anchors anti-bluff-mutation` — unchanged; `qa-all` reaches the real gate transitively.

- [ ] **Step 3: Run the anti-bluff aggregate target**

Run: `make anti-bluff`
Expected: all three sub-gates print `OK:`.

- [ ] **Step 4: Commit**

```bash
git add Makefile
git commit -m "build(anti-bluff): changed-mode mutation gate + mutation-full refresh target

Phase 5B. anti-bluff-mutation now runs the real gate in fast changed
mode; mutation-full does the baseline-refresh full pass.

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — build-automation only."
```

---

# Phase 5C — Manual subagent audit sweep (report-only)

Each task below dispatches **one report-only subagent** for a batch of test files. Batches are sized ~15–30 files. Subagents in different batches are independent — dispatch several in parallel per `superpowers:dispatching-parallel-agents`. **Audit subagents do not fix anything** — they produce a findings file. Confirmed bluffs become Phase 5E fix tasks.

### Task 9: Create the audit index

**Files:**
- Create: `docs/superpowers/audits/phase-5/README.md`

- [ ] **Step 1: Write the index**

```markdown
<!-- SPDX-License-Identifier: CC0-1.0 -->
# Phase 5 Anti-Bluff Audit — Batch Index

Each batch has a findings file in this directory. Status: `pending` →
`audited` → `fixes-filed` → `closed`.

| Batch | Scope | Files | Status |
|---|---|---|---|
| B01 | shared format/ core (FormatRegistry, TextParser, DocumentCache, StyleSheets, ParserRegistry) + format/markdown | ~25 | pending |
| B02 | shared format/ — csv, todotxt, json, yaml, xml, html, ini, properties, toml | ~25 | pending |
| B03 | shared format/ — remaining format dirs + integration/ + supremacy/ + stress/ | ~25 | pending |
| B04 | shared network/protocols/ — 8 protocol services | ~25 | pending |
| B05 | shared network/ — auth, common, platform, protocol | ~25 | pending |
| B06 | shared concurrency/ + util/ + performance/ + monitoring/ | ~23 | pending |
| B07 | shared ui/ + syntax/ + completion/ + lsp/ + language/ | ~31 | pending |
| B08 | shared security/ + safety/ + import_/ + model/ + database/ + e2e/ + api/ + challenges/ | ~26 | pending |
| B09 | shared desktopTest/ — batch A (first ~36) | ~36 | pending |
| B10 | shared desktopTest/ — batch B (remaining ~35) | ~35 | pending |
| B11 | shared wasmJsTest/ + iosTest/ | 6 | pending |
| B12 | androidApp tests — batch A (Robolectric/Compose, first ~28) | ~28 | pending |
| B13 | androidApp tests — batch B (remaining ~28) | ~28 | pending |
| B14 | desktopApp + webApp tests | 20 | pending |
| B15 | yole-challenges/scripts/ — all 39 challenge scripts (Task 11) | 39 | pending |

A batch is `audited` when its findings file exists and lists every file
in scope with a verdict. Bluff verdicts roll up into Phase 5E.
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/audits/phase-5/README.md
git commit -m "docs(anti-bluff): Phase 5 audit batch index

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — documentation only."
```

### Task 10: Audit batches B01–B14 (one subagent each, parallelizable)

**Files (per batch):**
- Create: `docs/superpowers/audits/phase-5/B<NN>-<slug>.md`
- Modify: `docs/superpowers/audits/phase-5/README.md` (flip the batch row to `audited`)

For each batch B01–B14, dispatch a `general-purpose` subagent with **exactly** this prompt (substitute `<BATCH ID>`, `<SCOPE DESCRIPTION>`, and the resolved file list):

```
You are a CONST-035 / CONST-039 anti-bluff auditor for the Yole project
(/Users/milosvasic/Projects/Yole). You audit ONLY — you do not edit any
file except your findings report. Do not run the build.

Scope — batch <BATCH ID>, <SCOPE DESCRIPTION>. Audit exactly these test
files:
<NEWLINE-SEPARATED ABSOLUTE FILE LIST>

For EACH test file, and for EACH test method in it, apply the mutation-
verification thought experiment from CONST-039:

  "If every line of the unit under test were replaced with a trivial
   stub (return null / return 0 / return emptyList / no-op), would this
   test still pass?"

If yes → the test is BLUFF. Also flag these specific patterns:
  - the unit under test is mocked (test verifies the mock, not the code)
  - assertions only on metadata/config/constants, never on behavior
  - "absence of error" only — runs code in try/catch and asserts nothing
  - structural source-grep tests using regex that can false-PASS (see
    memory: regex with [^X]* over Kotlin types false-PASSes on nested
    parens) — for each, state whether a mutation of the matched source
    would be caught
  - empty or no-op runBlocking { } bodies
  - permanent skips / @Ignore without a `// SKIP-OK: #<ticket>` marker

For each test file, also confirm the file actually compiles into a real
test source set (not dead/orphaned).

Write your findings to:
  /Users/milosvasic/Projects/Yole/docs/superpowers/audits/phase-5/<BATCH ID>-<slug>.md

Use EXACTLY this schema:

  # Phase 5 Audit — <BATCH ID> <SCOPE DESCRIPTION>
  Audited: <date>  |  Files: <n>  |  Bluff: <n>  |  Suspect: <n>  |  Clean: <n>

  ## Findings
  ### <relative/path/to/TestFile.kt>
  - **Verdict:** CLEAN | SUSPECT | BLUFF
  - **Methods reviewed:** <n>
  - **Evidence:** <one or two sentences — for BLUFF/SUSPECT, the exact
    method name(s) + why the stub-substitution test passes, with line
    numbers. For CLEAN, the strongest real behavioral assertion you saw.>
  - **Recommended fix:** <for BLUFF/SUSPECT only — concrete: what real
    input + real assertion would kill the mutant. For CLEAN: "none".>

  (one ### block per file — every file in scope MUST appear)

Be skeptical and concrete. A vague "looks fine" is not an audit. Cite
line numbers. Do not mark BLUFF without naming the surviving mutant.
```

- [ ] **Step 1 (B01):** Resolve the file list — `find shared/src/commonTest/kotlin/digital/vasic/yole/format -maxdepth 2 -name '*Test*.kt'` filtered to the B01 scope. Dispatch the subagent with the prompt above.
- [ ] **Step 2 (B01):** Two-stage review the findings file (per subagent-driven-development): (a) does it cover every file in scope? (b) spot-check 3 random CLEAN verdicts by reading the cited evidence yourself — confirm the auditor was not itself bluffing.
- [ ] **Step 3 (B01):** Flip the B01 row to `audited` in the index. Commit:

```bash
git add docs/superpowers/audits/phase-5/
git commit -m "docs(anti-bluff): Phase 5 audit findings — B01 format core + markdown

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — audit documentation only."
```

- [ ] **Steps 4–N:** Repeat Steps 1–3 for B02 … B14. Dispatch up to 3–4 batch subagents concurrently; review and commit each as it returns. Update `docs/CONTINUATION.md` after every 3 committed batches with the running bluff count.

### Task 11: Audit batch B15 — the 39 challenge scripts

**Files:**
- Create: `docs/superpowers/audits/phase-5/B15-challenge-scripts.md`
- Modify: `docs/superpowers/audits/phase-5/README.md`

- [ ] **Step 1: Dispatch the challenge-script auditor subagent**

Use this prompt:

```
You are a CONST-039 auditor for the Yole project
(/Users/milosvasic/Projects/Yole). Audit ONLY — do not edit anything
except your findings report. Do not run the build.

Audit every script in yole-challenges/scripts/*.sh (39 scripts). For
each, determine whether a PASS carries POSITIVE RUNTIME EVIDENCE that
the feature works for the end user, or whether PASS is achievable
without the feature working. CONST-039 forbids:
  - metadata-only PASS
  - configuration-only PASS (e.g. greps a config file, never runs it)
  - "absence-of-error" PASS
  - grep-based PASS with no runtime execution
  - any challenge that PASSes on a non-functional feature

Already known: mutation_ratchet_challenge.sh was a stub — by the time
you audit, Phase 5B has made it real; verify that.

Write findings to
docs/superpowers/audits/phase-5/B15-challenge-scripts.md with this
schema:

  # Phase 5 Audit — B15 Challenge Scripts
  Audited: <date>  |  Scripts: 39  |  Bluff: <n>  |  Suspect: <n>  |  Clean: <n>

  ## Findings
  ### <script-name>.sh
  - **Verdict:** CLEAN | SUSPECT | BLUFF
  - **PASS mechanism:** <what exactly makes it print OK/exit 0>
  - **Runtime evidence:** <what real execution + artifact it captures,
    or "NONE — <why>">
  - **Recommended fix:** <for BLUFF/SUSPECT: concrete runtime evidence
    to add. For CLEAN: "none">

Cite line numbers. A challenge that only greps source without executing
it is BLUFF unless the grep target IS the deliverable (e.g. a manifest).
```

- [ ] **Step 2:** Two-stage review the findings; spot-check 3 CLEAN verdicts against the script source yourself.
- [ ] **Step 3:** Flip B15 to `audited`; commit as in Task 10 Step 3.

### Task 12: Consolidate findings

**Files:**
- Create: `docs/superpowers/audits/phase-5/CONSOLIDATED.md`

- [ ] **Step 1: Aggregate**

Collate every `BLUFF` and `SUSPECT` verdict from B01–B15 into one table: `id | file | method/script | verdict | recommended fix | fix-task`. Number each row `P5-FIX-NNN`.

- [ ] **Step 2: Triage SUSPECT rows**

For each `SUSPECT`, read the cited test yourself and resolve it to `BLUFF` or `CLEAN`. Auditors err toward SUSPECT when unsure; you make the final call.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/audits/phase-5/CONSOLIDATED.md
git commit -m "docs(anti-bluff): Phase 5 consolidated bluff findings + fix backlog

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — audit documentation only."
```

---

# Phase 5D — Operator checkpoint (mid-phase)

### Task 13: Present the consolidated findings

**Files:** none

- [ ] **Step 1:** Present `CONSOLIDATED.md` to the operator: total bluff count, the most severe (a bluff hiding a real broken feature ranks highest per the operator covenant), and the estimated fix effort.
- [ ] **Step 2:** Confirm with the operator whether all `P5-FIX-*` rows are fixed in this phase, or whether low-severity ones are deferred to `docs/KNOWN_DEFECTS.md` with tracker ids. **Do not guess — ask.**

---

# Phase 5E — Fix sweep (tasks appended after Task 12)

> **Operator decision (2026-05-20, Phase 5D checkpoint):** scope = **all 158
> `P5-FIX` items** (55 BLUFF + 103 SUSPECT). Nothing deferred. Full anti-bluff
> covenant compliance. Backlog + 12 systemic patterns: `docs/superpowers/audits/phase-5/CONSOLIDATED.md`.
> Estimated 4–5 engineer-weeks — Phase 5E spans many sessions; progress tracked
> incrementally in `docs/CONTINUATION.md` per CONST-036.

**Execution structure — pattern campaigns, not 158 ad-hoc tasks.** The fixes
are organised as campaigns, run in CONSOLIDATED.md Section 5 priority order:

1. **CRITICAL items first** (the 10 in CONSOLIDATED.md Section 4) — each may hide
   a genuinely broken feature. Every CRITICAL fix task MUST first *verify the
   real feature works*; if it does not, fixing the **product** is the task.
2. **Mechanical pattern sweeps** (PAT-01, 02, 03, 09, 10, 11, 12) — one uniform
   technique applied across all occurrences of a pattern; one campaign per pattern.
3. **Independent-investigation patterns** (PAT-04, 05, 06, 08) — per-entry work.

**Concurrency rule:** fix subagents EDIT code — unlike the report-only audit
subagents they may NOT run in parallel unless partitioned to provably disjoint
file sets. Default to sequential implementer → spec review → quality review.

This section is intentionally not pre-enumerated as code: the fixes are
audit-discovered. **Append one fix task per `P5-FIX-*` cluster / pattern campaign**, each following strict TDD:

1. **Write/strengthen the test first** so it FAILs against the current (bluff-permitting) code — i.e. it now genuinely kills the mutant the auditor named.
2. **Run it, confirm it FAILs** for the stated reason.
3. **Fix the root cause.** Per the operator covenant: if the bluff was hiding a genuinely broken feature, fixing the *product* is the task — not just the test. If the feature works and only the test was weak, strengthening the test is the fix.
4. **Run it, confirm it PASSes.**
5. **Re-run `bash scripts/anti-bluff/run-pitest.sh <that package>`** — confirm the previously-surviving mutant is now KILLED.
6. **Raise that package's Section-2 floor** in `bluff-baseline.txt` to the new measured rate.
7. **Commit** with a Conventional Commit; if production source changed, include the full Cross-platform impact block (CONST-037).

Each fix task names its `P5-FIX-NNN` id, the file, the surviving mutant, and the per-platform reasoning. A bluff in a Compose/Robolectric test that source-grep cannot verify MUST be rewritten to use real user gestures + `testTag` matching (CONST-039 component-test rule), not regex.

**Template for each appended fix task:**

```markdown
### Task <N>: Fix P5-FIX-<NNN> — <file>::<method>

**Files:**
- Test: `<path>`
- Modify: `<production path if the feature itself is broken>`

- [ ] Step 1: Strengthen the test so it FAILs (kills the named mutant). Show the test code.
- [ ] Step 2: Run it — `./gradlew :shared:desktopTest --tests "<FQN>"` — expect FAIL.
- [ ] Step 3: Fix the root cause. Show the code.
- [ ] Step 4: Run it — expect PASS.
- [ ] Step 5: `bash scripts/anti-bluff/run-pitest.sh <package>` — mutant now KILLED.
- [ ] Step 6: Raise the Section-2 floor for that package.
- [ ] Step 7: Commit (Conventional Commit + Cross-platform impact block).
```

---

# Phase 5F — Close-out

### Task 14: Full gate sweep

**Files:** none

- [ ] **Step 1:** Run `bash scripts/anti-bluff/bluff-scanner.sh --mode all` — expect `OK: scanner clean`.
- [ ] **Step 2:** Run `make anti-bluff` — expect all three sub-gates `OK:` (mutation gate now real).
- [ ] **Step 3:** Run `bash yole-challenges/scripts/anchor_manifest_challenge.sh` and `anti_bluff_cascade_audit_challenge.sh` — expect PASS.
- [ ] **Step 4:** Run `make mutation-full` once more — expect every package at or above its (now raised) floor.
- [ ] **Step 5:** Run `./gradlew :shared:desktopTest` — expect the full suite green (any K2-stub exemptions still documented in `KNOWN_DEFECTS.md`).

### Task 15: Update governance + continuation

**Files:**
- Modify: `docs/CONTINUATION.md`, `docs/KNOWN_DEFECTS.md`

- [ ] **Step 1:** Update `docs/CONTINUATION.md` — add a Phase 5 section: machinery shipped, total files audited, bluffs found, bluffs fixed, any deferred trackers. Update the top-of-file status line.
- [ ] **Step 2:** Register any deferred `P5-FIX-*` rows in `docs/KNOWN_DEFECTS.md` with `#phase-5-*` tracker ids.
- [ ] **Step 3:** Commit:

```bash
git add docs/CONTINUATION.md docs/KNOWN_DEFECTS.md
git commit -m "docs(anti-bluff): Phase 5 re-audit close-out — continuation + defect register

Cross-platform impact:
- Android/Desktop/iOS/Web: unaffected — documentation only."
```

### Task 16: Phase 5 operator checkpoint

**Files:** none

- [ ] **Step 1:** Present to the operator: the real `mutation_ratchet` gate, the audit coverage (15 batches), the bluff tally, the fixes, and the gate sweep output. Per the epic spec, the operator reviews before Phase 6 begins.

---

## Definition of Done (Phase 5)

1. ✅ All machinery + fix commits committed.
2. ✅ `./gradlew :shared:desktopTest` green on a clean clone.
3. ✅ All `yole-challenges/scripts/` challenges pass on the host — including the now-real `mutation_ratchet`.
4. ✅ Governance docs coherent.
5. ✅ `docs/CONTINUATION.md` updated (CONST-036).
6. ✅ Cross-platform impact reasoned + in every commit body (CONST-037).
7. ✅ No submodule decoupling touched — Phase 5 is Yole-repo-local (CONST-038).
8. ✅ Every PASS carries positive runtime evidence — the mutation gate runs real Pitest; every audit verdict cites concrete evidence (CONST-039).

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Pitest + KMP classpath wiring fails | Task 2 Step 3 + Task 4 are explicit verification gates with fallback API paths; smoke run on one package before the full pass. |
| Full mutation run OOMs / too slow | Task 5 Step 1 falls back to per-top-level-package runs; gate itself runs `changed` mode only. |
| Audit subagents themselves bluff (vague verdicts) | Task 10 Step 2 / Task 11 Step 2 two-stage review spot-checks CLEAN verdicts against cited source. |
| Phase 5E unbounded | Task 13 operator checkpoint sets the fix/defer split before fixing starts. |
| A bluff hides a real broken feature | Phase 5E Step 3 explicitly fixes the *product*, not just the test — per the operator covenant. |
