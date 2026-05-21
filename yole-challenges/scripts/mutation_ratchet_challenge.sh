#!/usr/bin/env bash
# CONST-035 / CONST-039 mutation ratchet challenge — Yole main (Kotlin/Pitest).
#
# Runs a REAL Pitest mutation pass and ratchets per-package kill rates
# against bluff-baseline.txt Section 2. A package whose detected-rate
# drops below its recorded floor FAILs the gate. This is the runtime
# evidence the gate carries — it is no longer a stub.
#
# Mode (env MUTATION_MODE, default `changed`):
#   changed  mutate only the packages whose production OR test code
#            changed vs the default branch — fast; the qa-all gate.
#   full     mutate every production package — slow; baseline refresh.
#
# Exit 0 = all measured packages at or above floor (or nothing changed).
# Exit 1 = a real regression, a Pitest failure, or a malformed baseline.
set -euo pipefail

if (( BASH_VERSINFO[0] < 4 )); then
  echo "FAIL: mutation_ratchet_challenge.sh requires bash 4+; current shell is ${BASH_VERSION}." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BASELINE="${ROOT_DIR}/yole-challenges/baselines/bluff-baseline.txt"
RUN_PITEST="${ROOT_DIR}/scripts/anti-bluff/run-pitest.sh"
MODE="${MUTATION_MODE:-changed}"
LOG_DIR="${ROOT_DIR}/qa-results"
LOG="${LOG_DIR}/mutation_ratchet.log"
mkdir -p "${LOG_DIR}"

if [[ ! -f "${BASELINE}" ]]; then
  echo "FAIL: baseline file missing at ${BASELINE}" >&2
  exit 1
fi
# Structural integrity — all three section markers must be present.
for marker in 'SECTION 1' 'SECTION 2' 'SECTION 3'; do
  grep -q "^# === ${marker}" "${BASELINE}" \
    || { echo "FAIL: baseline malformed (missing ${marker} marker)." >&2; exit 1; }
done
# Section 2 must hold real data, not a deferral stub.
if ! grep -qE '^digital\.vasic\.yole\.[a-z_.]+:[0-9]+/[0-9]+:[0-9]+$' "${BASELINE}"; then
  echo "FAIL: baseline Section 2 has no per-package kill-rate data — gate cannot ratchet." >&2
  exit 1
fi

echo "[mutation_ratchet] mode=${MODE} — running Pitest..." | tee "${LOG}"
if ! bash "${RUN_PITEST}" "${MODE}" >>"${LOG}" 2>&1; then
  echo "FAIL: Pitest run failed — see ${LOG}" >&2
  exit 1
fi

# changed-mode with no changed code: run-pitest.sh exits 0 without a report.
if grep -q 'nothing to mutate' "${LOG}"; then
  echo "OK: mutation ratchet — no changed production/test code to mutate."
  exit 0
fi

XML="${ROOT_DIR}/shared/build/reports/pitest/latest/mutations.xml"
if [[ ! -f "${XML}" ]]; then
  echo "FAIL: Pitest produced no mutations.xml — see ${LOG}" >&2
  exit 1
fi

# Ratchet the measured per-package detected-rates against Section 2 floors.
RESULT="$(python3 - "${XML}" "${BASELINE}" <<'PY'
import sys, collections, xml.etree.ElementTree as ET
xml_path, baseline_path = sys.argv[1], sys.argv[2]
DETECTED = {'KILLED', 'TIMED_OUT', 'MEMORY_ERROR', 'RUN_ERROR'}

total = collections.Counter(); killed = collections.Counter()
for m in ET.parse(xml_path).getroot():
    cls = m.findtext('mutatedClass') or ''
    pkg = '.'.join(cls.split('.')[:-1])      # FQCN minus class name = package
    if not pkg.startswith('digital.vasic.yole'):
        continue
    total[pkg] += 1
    if (m.get('status') or '') in DETECTED:
        killed[pkg] += 1

floors = {}
in_s2 = False
for line in open(baseline_path):
    if line.startswith('# === SECTION 2'): in_s2 = True;  continue
    if line.startswith('# === SECTION 3'): in_s2 = False; continue
    if not in_s2 or line.startswith('#') or not line.strip():
        continue
    pkg, _ratio, floor = line.strip().rsplit(':', 2)
    floors[pkg] = int(floor.rstrip('%'))

if not total:
    print('WARN  no production mutations in this run.')
    sys.exit(0)

failed = False
for pkg in sorted(total):
    rate = killed[pkg] * 100 // max(total[pkg], 1)
    floor = floors.get(pkg)
    if floor is None:
        print(f'WARN  {pkg}: {rate}% — no Section-2 floor (new package; '
              f'run `make mutation-full` to record it).')
        continue
    mark = 'PASS' if rate >= floor else 'FAIL'
    if mark == 'FAIL':
        failed = True
    print(f'{mark}  {pkg}: {rate}% (floor {floor}%, {killed[pkg]}/{total[pkg]})')

sys.exit(1 if failed else 0)
PY
)" && RC=0 || RC=$?

echo "${RESULT}" | tee -a "${LOG}"
if [[ "${RC}" -ne 0 ]]; then
  echo "FAIL: a package's mutation kill rate dropped below its baseline floor." >&2
  echo "      A test was weakened or production code lost coverage. Log: ${LOG}" >&2
  exit 1
fi
echo "OK: mutation ratchet — all measured packages at or above baseline floor. Log: ${LOG}"
exit 0
