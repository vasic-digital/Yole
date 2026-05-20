#!/usr/bin/env bash
# Phase 5A — full per-package Pitest baseline runner.
#
# Runs Pitest once per top-level production package, aggregates the
# per-package kill rates across ALL report dirs under
# shared/build/reports/pitest/, and rewrites Section 2 of
# yole-challenges/baselines/bluff-baseline.txt.
#
# Resilient: a single package failing (OOM, crash) does not abort the
# loop — its run is skipped and the rest proceed. Long-running (hours):
# invoke in the background. Idempotent on the aggregation step.
#
# Usage:
#   run-pitest-baseline.sh                 # all 12 top-level packages
#   run-pitest-baseline.sh model lsp util  # only the named packages
#                                          # (aggregation still folds in
#                                          #  every existing report dir)
set -uo pipefail   # deliberately NOT -e: a failing package must not abort the loop

# Uses `mapfile`, a bash 4+ builtin. macOS default is bash 3.2.
if (( BASH_VERSINFO[0] < 4 )); then
  echo "FAIL: run-pitest-baseline.sh requires bash 4+; current shell is ${BASH_VERSION}." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_PITEST="scripts/anti-bluff/run-pitest.sh"
BASELINE="yole-challenges/baselines/bluff-baseline.txt"
LOG="shared/build/reports/pitest-baseline.log"
mkdir -p "$(dirname "${LOG}")"

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
    echo "[baseline] ${p} OK" | tee -a "${LOG}"
  else
    echo "[baseline] ${p} FAILED — skipped (see ${LOG})" | tee -a "${LOG}"
  fi
done

# Aggregate every report dir on disk and rewrite Section 2.
python3 - "${BASELINE}" <<'PY'
import sys, glob, collections, xml.etree.ElementTree as ET
baseline = sys.argv[1]
total = collections.Counter(); killed = collections.Counter()
for xml in glob.glob('shared/build/reports/pitest/*/mutations.xml'):
    try:
        root = ET.parse(xml).getroot()
    except Exception:
        continue
    for m in root:
        cls = m.findtext('mutatedClass') or ''
        pkg = '.'.join(cls.split('.')[:-1])  # FQCN minus class name = package
        if not pkg.startswith('digital.vasic.yole'):
            continue
        total[pkg] += 1
        if m.get('status') == 'KILLED':
            killed[pkg] += 1

section2 = [
    '# === SECTION 2: PER-FILE MUTATION KILL RATES ===',
    '# format <package>:<killed>/<total>:<floor-percent>. The mutation_ratchet',
    '# gate ratchets: a package kill rate may not drop below the recorded floor.',
    '# Captured 2026-05-21 by a full per-package Pitest pass',
    '# (--mutableCodePaths restricts mutation to production code).',
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
print(f'[baseline] aggregated {len(total)} packages, '
      f'{tk}/{tt} mutations killed ({tk * 100 // max(tt, 1)}%)')
PY

echo "[baseline] $(date) — DONE — Section 2 written to ${BASELINE}. Log: ${LOG}"
