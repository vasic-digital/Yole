#!/usr/bin/env bash
# Phase 5A — full per-package Pitest baseline runner.
#
# Runs Pitest once per top-level production package, copies each result
# into a STABLE per-package XML store (shared/build/pitest-baseline-xml/),
# aggregates the kill rates from that store, and rewrites Section 2 of
# yole-challenges/baselines/bluff-baseline.txt.
#
# The stable store is the key design point: shared/build/reports/pitest/
# run dirs are volatile (a parallel `:shared:desktopTest` or a `clean`
# can wipe them mid-run). Copying each result out immediately, and
# aggregating from the copies, makes the baseline resilient and lets a
# resume run pre-seed already-measured packages.
#
# Resilient: a single package failing (OOM, timeout) does not abort the
# loop. Long-running (hours): invoke in the background.
#
# Kill convention: detected = KILLED + TIMED_OUT + MEMORY_ERROR +
# RUN_ERROR (Pitest's standard "detected" set), over total mutations.
#
# Usage:
#   run-pitest-baseline.sh                 # all 12 top-level packages
#   run-pitest-baseline.sh model lsp util  # only the named packages;
#                                          # aggregation still folds in
#                                          # every XML already in the
#                                          # stable store
set -uo pipefail   # deliberately NOT -e: a failing package must not abort the loop

if (( BASH_VERSINFO[0] < 4 )); then
  echo "FAIL: run-pitest-baseline.sh requires bash 4+; current shell is ${BASH_VERSION}." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_PITEST="scripts/anti-bluff/run-pitest.sh"
BASELINE="yole-challenges/baselines/bluff-baseline.txt"
STABLE="shared/build/pitest-baseline-xml"
LOG="shared/build/reports/pitest-baseline.log"
mkdir -p "${STABLE}" "$(dirname "${LOG}")"

if [[ $# -gt 0 ]]; then
  PKGS=("$@")
else
  mapfile -t PKGS < <(ls -d shared/src/commonMain/kotlin/digital/vasic/yole/*/ \
      shared/src/desktopMain/kotlin/digital/vasic/yole/*/ 2>/dev/null \
      | sed -E 's#.*/digital/vasic/yole/##; s#/##' | sort -u)
fi

echo "[baseline] $(date) — START — packages: ${PKGS[*]}" | tee "${LOG}"

for p in "${PKGS[@]}"; do
  echo "[baseline] $(date) — running ${p}" | tee -a "${LOG}"
  if bash "${RUN_PITEST}" "digital.vasic.yole.${p}.*" >>"${LOG}" 2>&1; then
    xml="shared/build/reports/pitest/latest/mutations.xml"
    if [[ -f "${xml}" ]]; then
      cp "${xml}" "${STABLE}/${p}.xml"
      echo "[baseline] ${p} OK — saved ${STABLE}/${p}.xml" | tee -a "${LOG}"
    else
      echo "[baseline] ${p} OK but no mutations.xml — skipped" | tee -a "${LOG}"
    fi
  else
    echo "[baseline] ${p} FAILED — skipped (see ${LOG})" | tee -a "${LOG}"
  fi
done

# Aggregate every XML in the stable store and rewrite Section 2.
python3 - "${BASELINE}" "${STABLE}" <<'PY'
import sys, glob, collections, xml.etree.ElementTree as ET
baseline, stable = sys.argv[1], sys.argv[2]
DETECTED = {'KILLED', 'TIMED_OUT', 'MEMORY_ERROR', 'RUN_ERROR'}
total = collections.Counter(); killed = collections.Counter()
for xmlf in sorted(glob.glob(f'{stable}/*.xml')):
    try:
        root = ET.parse(xmlf).getroot()
    except Exception:
        continue
    for m in root:
        cls = m.findtext('mutatedClass') or ''
        pkg = '.'.join(cls.split('.')[:-1])  # FQCN minus class name = package
        if not pkg.startswith('digital.vasic.yole'):
            continue
        total[pkg] += 1
        if (m.get('status') or '') in DETECTED:
            killed[pkg] += 1

section2 = [
    '# === SECTION 2: PER-FILE MUTATION KILL RATES ===',
    '# format <package>:<killed>/<total>:<floor-percent>. The mutation_ratchet',
    '# gate ratchets: a package kill rate may not drop below the recorded floor.',
    '# Captured 2026-05-21 by a full per-package Pitest pass (--mutableCodePaths',
    '# restricts mutation to production code; verified 0 test classes mutated).',
    '# killed = KILLED + TIMED_OUT + MEMORY_ERROR + RUN_ERROR; denom = total.',
]
for pkg in sorted(total):
    rate = killed[pkg] * 100 // max(total[pkg], 1)
    section2.append(f'{pkg}:{killed[pkg]}/{total[pkg]}:{rate}')

src = open(baseline).read().splitlines()
out, i = [], 0
while i < len(src):
    if src[i].startswith('# === SECTION 2'):
        out += section2
        while i < len(src) and not src[i].startswith('# === SECTION 3'):
            i += 1
        continue
    out.append(src[i]); i += 1
open(baseline, 'w').write('\n'.join(out) + '\n')

tk, tt = sum(killed.values()), sum(total.values())
print(f'[baseline] aggregated {len(total)} packages from {stable}, '
      f'{tk}/{tt} mutations detected ({tk * 100 // max(tt, 1)}%)')
PY

echo "[baseline] $(date) — DONE — Section 2 written to ${BASELINE}. Log: ${LOG}"
