# Anti-Bluff Foundation Implementation Plan (Sub-project 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land CONST-034 (anti-bluff test discipline) and its enforcement gates (scanner + mutation testing + behavior-anchor manifest) across main repo + 3 Go submodules, with a baseline of pre-existing bluff hits captured (not fixed). The historical failure was tests passing while features were unusable; this sub-project makes that detectable and blocks its recurrence.

**Architecture:** Four-repo rollout. Submodules first in parallel (3 background agents), main repo last (it advances submodule pointers to already-pushed SHAs). Each repo gets verbatim CONST-034 + per-language scanner + per-language mutation gate + manifest skeleton. Hard-block on new code; pre-existing bluff is grandfathered into a baseline file. Push at every checkpoint on the campaign branch; immediately on master.

**Tech Stack:** Bash 4+ (scanners and challenge wrappers), Pitest 1.17.x via `pitest-gradle-plugin` 1.15.x (Kotlin/JVM mutation), `go-mutesting` (Go mutation), Markdown (manifest, baseline, governance docs). No new languages.

**Spec reference:** `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md`. The spec is authoritative for design decisions; this plan is authoritative for execution steps.

**Estimated effort:** 3–5 days focused work. The two slowest individual steps are the first full-tree mutation runs (~30–60 min on Kotlin/JVM, ~5–15 min per Go submodule); they can run in the background while drafting docs.

---

## Phase overview

| Phase | Repo(s) | Purpose | Parallelizable |
|-------|---------|---------|----------------|
| **0** | `Yole/` (main) | Bootstrap: create campaign branch + CAMPAIGN.md tracker stub. Must precede all other phases. | No |
| **1** | `Challenges/`, `Containers/`, `HelixQA/` | Per-submodule rollout (Go flavor). Identical structure across the 3 submodules; differs only in path substitution and per-repo remote names. | Yes — 3 parallel agents |
| **2** | `Yole/` (main) | Main repo rollout (Kotlin/JVM flavor). Includes advancing submodule pointers to the SHAs landed in Phase 1. | No (depends on Phase 1 completion) |

---

## Phase 0 — Main repo bootstrap

The single source of truth for cross-repo campaign state lives in main repo. It must exist before any submodule work begins so submodule agents can write back to it (or report status that the main session writes back).

### Task 0.1: Create campaign branch in main repo

**Files:**
- Modify branch state in `/run/media/milosvasic/DATA4TB/Projects/Yole`

- [ ] **Step 1: Verify clean working tree**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git status -s
```

Expected: shows only the two submodule "modified content" markers (`m Challenges`, `m Containers`) and nothing else uncommitted. If anything else appears, stop and resolve before proceeding.

- [ ] **Step 2: Verify on master and up to date**

```bash
git rev-parse --abbrev-ref HEAD
git fetch --all --prune
git log @{u}..HEAD --oneline | wc -l
```

Expected: branch `master`; last command output `0` (no unpushed commits).

- [ ] **Step 3: Create campaign branch**

```bash
git checkout -b campaign/anti-bluff
```

Expected: `Switched to a new branch 'campaign/anti-bluff'`.

### Task 0.2: Create campaign tracker stub

**Files:**
- Create: `docs/campaigns/anti-bluff/CAMPAIGN.md`

- [ ] **Step 1: Create the tracker file**

Write `docs/campaigns/anti-bluff/CAMPAIGN.md` with this exact content:

```markdown
# Campaign — Anti-Bluff Discipline (CONST-034)

**Started:** 2026-05-01
**Status:** sub-project 1 in progress
**Resume protocol:** read this file top to bottom, find the first
checkpoint with status ≠ done, resume there.

## Sub-projects

1. [in progress] Anti-Bluff Foundation — see spec
   `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md`
   and plan `docs/superpowers/plans/2026-05-01-anti-bluff-foundation.md`.
2. [pending] Resumable Campaign Tracker formalisation
3. [pending] Bluff audit of existing tests/Challenges
4. [pending] Punch list fixes (round 1)
5. [pending] Coverage push (component-by-component)
6. [pending] Operational cadence formalisation

## Sub-project 1 — Anti-Bluff Foundation

### Per-repo state

| Repo | Branch | Last checkpoint | Next checkpoint | Last push |
|------|--------|------------------|-------------------|-----------|
| Challenges | campaign/anti-bluff | — | 1 | — |
| Containers | campaign/anti-bluff | — | 1 | — |
| HelixQA    | campaign/anti-bluff | — | 1 | — |
| Yole       | campaign/anti-bluff | 0.1 (branch created) | 0.2 (this commit) | — |

### Decisions log

- Q1 → bluff = passes without exercising user-visible behavior (formal rule).
- Q2 → enforcement = scanner + mutation + anchor manifest, all gating.
- Q3 → user-visible = three layers (app / KMP / Go submodule).
- Q4 → hard block on new code, baseline-grandfathered.
- Q5 → submodules: verbatim copy, independent enforcement.
- Q6 → mutation: 90% changed / 80% project ratchet.
- Q7 → push: hybrid (master immediate / campaign branch at checkpoints).
- Q8 → anchor manifest: per user-facing capability, ~100 rows expected.

### Open risks

- Pitest cannot mutate Kotlin Native or WASM — KMP coverage of those
  targets relies entirely on the anchor manifest. Track gap explicitly.
- Equivalent mutants in Pitest may produce false 90%-threshold misses;
  exemption mechanism is `// ANTI-BLUFF-EXEMPT: TRIVIAL-CORRECT — <reason>`
  inline. Watch for over-use in sub-project 4.
- Submodule pointer churn during sub-project 1: 3 main-repo "update
  submodule pointer" commits batched into checkpoint 12 of main; single
  push, not three.
- HelixQA has 6 remotes; `git push origin master` pushes to all 4 of
  origin's destinations because of multi-URL push config. The plan
  treats `git push <remote-name> master` per remote name as the unit.

### Resume cheatsheet

- Lost: read this file's per-repo state table.
- Branch unclear: every per-repo working branch is `campaign/anti-bluff`.
- Stuck on a checkpoint: each task in the plan lists its concrete output.
  Don't move on until that output exists.

### Per-repo remote inventory (push targets)

- **Yole (main):** `github`, `origin`, `upstream` (all alias `git@github.com:vasic-digital/Yole.git`)
- **Challenges:** `github`, `gitlab`, `origin`, `upstream`
- **Containers:** `github`, `gitlab`, `origin`, `upstream`
- **HelixQA:** `github`, `gitlab`, `helixgithub`, `helixgitlab`, `origin` (multi-URL fan-out), `upstream`
```

- [ ] **Step 2: Commit and push the tracker stub**

```bash
git add docs/campaigns/anti-bluff/CAMPAIGN.md
git commit -m "$(cat <<'EOF'
docs(campaign): bootstrap anti-bluff campaign tracker

Single source of truth for cross-repo CONST-034 rollout. Lists 6
sub-projects, per-repo state table, decisions log Q1-Q8, open risks,
resume protocol.

Refs: docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
git push -u github campaign/anti-bluff
git push origin campaign/anti-bluff
git push upstream campaign/anti-bluff
```

Expected: commit hash output, then 3 successful push lines.

- [ ] **Step 3: Verify**

```bash
git log -1 --format="%h %s"
git log @{u}..HEAD --oneline
```

Expected: latest commit subject is `docs(campaign): bootstrap anti-bluff campaign tracker`. Second command empty.

---

## Phase 1 — Per-submodule rollout

**Run 3 parallel agents**, one per submodule. Each agent receives a `SUBMODULE` parameter naming one of: `Challenges`, `Containers`, `HelixQA`. The agent works inside `/run/media/milosvasic/DATA4TB/Projects/Yole/${SUBMODULE}/`. All paths below are relative to that directory unless explicitly absolute.

**Per-submodule push remote list:**

| `${SUBMODULE}` | `${PUSH_REMOTES}` |
|----------------|-------------------|
| `Challenges`   | `github gitlab origin upstream` |
| `Containers`   | `github gitlab origin upstream` |
| `HelixQA`      | `github gitlab helixgithub helixgitlab origin upstream` |

When the plan says "push to all remotes," the agent runs `for r in ${PUSH_REMOTES}; do git push "$r" <branch>; done`.

### Task 1.1: Create campaign branch

**Files:**
- Modify branch state in submodule

- [ ] **Step 1: Verify submodule is on a clean tracked state**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/${SUBMODULE}
git status -s
git rev-parse --abbrev-ref HEAD
```

Expected: empty status (or only files we'll explicitly handle); branch is `master` or `main`. If on a detached HEAD (because of submodule checkout), run `git checkout master`.

- [ ] **Step 2: Fetch and verify up to date**

```bash
git fetch --all --prune
git log @{u}..HEAD --oneline | wc -l
```

Expected: `0`.

- [ ] **Step 3: Create campaign branch**

```bash
git checkout -b campaign/anti-bluff
```

Expected: `Switched to a new branch 'campaign/anti-bluff'`.

### Task 1.2: Create `docs/ANTI_BLUFF.md` (runbook)

**Files:**
- Create: `docs/ANTI_BLUFF.md`

- [ ] **Step 1: Ensure docs/ exists**

```bash
mkdir -p docs
```

- [ ] **Step 2: Write the runbook**

Write `docs/ANTI_BLUFF.md` with this exact content (this is per-submodule context for Go; the Kotlin variant in Phase 2 differs):

```markdown
# Anti-Bluff Discipline (CONST-034) — Runbook

This document is the runbook for working with the anti-bluff gates in
this repository. The rule itself lives in `CONSTITUTION.md` (CONST-034).

## What "bluff" means here

A test is **bluff** if it can pass without exercising the user-visible
behavior it claims to verify. In this Go submodule, "user-visible"
means an operator invoking the CLI/binary can observe the result — the
anchor signal is a CLI invocation against a real target producing an
observable artifact (file on disk, exit code, log line) that the test
asserts on.

## Three gates

1. **Static scanner** (`scripts/anti-bluff/bluff-scanner.sh`) — pattern
   matcher that flags forbidden constructs. Runs on every commit
   (pre-commit hook) and in `make qa-all`.
2. **Mutation testing** (`go-mutesting`) — kills generated mutants.
   Threshold: 90% on changed code, 80% project-wide ratchet. Runs in
   `make qa-all` (slow; not in pre-commit).
3. **Behavior-anchor manifest** (`docs/behavior-anchors.md`) — every
   user-facing capability has at least one anchor test that proves it
   works end-to-end.

## "I got a bluff hit, what now?"

The scanner output names the file, line, BLUFF-G-NNN ID, and a
one-line context. Look up the BLUFF-G-NNN in the table below:

| ID | Pattern | Fix |
|----|---------|-----|
| BLUFF-G-001 | `t.Skip()` without exempt comment | Add `// ANTI-BLUFF-EXEMPT: <ticket>` on the line above, or remove the skip and fix the underlying issue. |
| BLUFF-G-002 | `if testing.Short()` early return without long-path coverage elsewhere | Add a sub-test that runs the long path; or add the exempt comment if short is correct here. |
| BLUFF-G-003 | Test body has only `t.Log`, no assertions | Add `t.Fatal`/`t.Error`/`testify` assertions. |
| BLUFF-G-004 | `gomock` mocking a type from the same package | Stop mocking the SUT; use the real type or move the mocked type to a different package. |
| BLUFF-G-005 | `t.Run("", func)` empty subtest body | Fill the subtest body with assertions or remove. |
| BLUFF-G-006 | Empty test function body | Fill or delete. |
| BLUFF-G-007 | `assert.True(t, true)` / `assert.NotNil(t, x)` as sole assertion | Add a real assertion that exercises the SUT's behavior. |

## "Mutation gate failed on my change"

`go-mutesting` printed mutants that survived (the test suite did not
detect them). Each surviving mutant is a place where the SUT's
behavior could change without any test noticing. Either:

- Add a test that would notice (preferred), or
- Add an in-line `// ANTI-BLUFF-EXEMPT: TRIVIAL-CORRECT — <reason>`
  if the mutant is genuinely equivalent (the mutated code is
  semantically identical to the original — extremely rare).

The challenge enforces 90% kill rate on changed files. Equivalent
mutants count toward the 10% slack; you should not need exemptions
unless your change happens to hit a mathematical identity.

## "Anchor manifest check failed"

`anchor_manifest_challenge.sh` validates `docs/behavior-anchors.md`.
Most failures are: the `anchor_test_path` you wrote does not resolve
to an existing test method. Re-check the path; the format is
`<relative path>.go::TestName` for Go.

## Reducing the baseline

The baseline file `challenges/baselines/bluff-baseline.txt` is
expected to shrink during sub-project 4. Removing a line is a
**ratchet improvement**: do it in the same commit that fixes the
underlying bluff. The scanner exits with code 2 if it sees a
baselined hit that is no longer present — this is the signal that the
baseline file is stale.

## Verification commands

Run all three before declaring work done:

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh
```

All three must pass.
```

- [ ] **Step 3: Commit**

```bash
git add docs/ANTI_BLUFF.md
git commit -m "$(cat <<'EOF'
docs: add ANTI_BLUFF.md runbook for CONST-034

Runbook for the three anti-bluff gates (scanner, mutation, manifest)
adapted to this Go submodule. Includes BLUFF-G-NNN reference table and
"I got a hit, what now?" troubleshooting.

Refs: CONST-034 (added in next commit).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: Push to all remotes for durability**

```bash
for r in ${PUSH_REMOTES}; do git push -u "$r" campaign/anti-bluff; done
```

Expected: one successful push per remote.

### Task 1.3: Create `CONSTITUTION.md` with CONST-034

**Files:**
- Create: `CONSTITUTION.md`

This submodule does not currently have a CONSTITUTION.md. Create one
with minimal scaffolding plus CONST-034 verbatim.

- [ ] **Step 1: Write `CONSTITUTION.md`**

```markdown
# ${SUBMODULE} — Constitution

> **Status:** Active. This document is the project's authoritative
> rule set. When a rule here conflicts with `CLAUDE.md`, `AGENTS.md`,
> or any guide, the Constitution wins.

## Mission

See `README.md` for the project's purpose and scope.

## Mandatory Standards

1. **Reproducibility:** every change is reproducible from a clean
   clone (`git clone <repo> && <project bootstrap>`); no hidden steps.
2. **Tests track behavior, not code:** test what the user-visible
   behavior is, not what the implementation looks like.
3. **No silent skips, no silent mocks above unit tests.**
4. **Conventional Commits** for all commits.
5. **SSH-only for git operations** (`git@…`); HTTPS prohibited.

## Numbered Rules

### CONST-034 — Anti-Bluff Test Discipline

**Status:** Mandatory. Non-negotiable. Applies to every test, challenge,
script, and verification artifact in this repository and its submodules.

**Rule:** A test or challenge is **bluff** if it can pass without
exercising the user-visible behavior it claims to verify. Bluff tests
are forbidden. Every test that lands must, on failure of the behavior
it claims to verify, fail. Every test that passes must constitute
evidence that the claimed behavior works for an end consumer.

**"User-visible behavior" — three-layer definition:**

- **App layer** (Android, Desktop, iOS, Web): user-visible means a real
  human user can observe the result. Anchor signal: UI screenshot diff,
  file written to disk and re-read, network request observed on a real
  socket, intent/activity dispatched and asserted.
- **KMP module layer** (RateLimiter, Concurrency, UI-Components, Auth,
  Security, Document, Config, Database, Storage, Formatters):
  user-visible means a developer consuming the public API can observe
  the documented contract. Anchor signal: integration test calling only
  the public API, using real coroutines / real clock / real I/O where
  applicable, asserting the contract in `KDoc` for that API.
- **Go submodule layer** (Challenges, Containers, HelixQA): user-visible
  means an operator invoking the CLI/binary can observe the result.
  Anchor signal: CLI invocation against a real target (real container,
  real test bank, real device emulator) producing an observable artifact
  (file on disk, exit code, log line), asserted by the test.

**Forbidden patterns** (non-exhaustive — see scanner for full list):
1. Mocking the unit under test.
2. Asserting only implementation details (private fields, internal
   state) and not user-observable output.
3. Skipping real I/O at integration boundaries when the test claims
   to test that boundary.
4. Trivial assertions (`assertTrue(true)`, lone `assertNotNull`,
   `t.Log` without `t.Fatal`/`t.Error`) as the sole assertion.
5. Always-skipped under common conditions (`@Ignore`, `assumeTrue(false)`,
   `t.Skip()` without a documented in-source ticket reference).
6. `runBlocking { }` / `t.Run("", func(t *testing.T) { })` blocks
   that perform no awaits / no sub-assertions.
7. Any test whose body would still pass if every line of the unit
   under test were replaced with `throw NotImplementedError()`.

**Defence in depth (mandatory artifacts in every project):**
1. `scripts/anti-bluff/bluff-scanner.sh` — static scanner. Exits non-zero
   on any forbidden pattern outside the baseline.
2. `challenges/baselines/bluff-baseline.txt` — captured set of pre-existing
   bluff hits and per-file mutation kill rates. Ratchet enforces the
   baseline never worsens.
3. `docs/behavior-anchors.md` — anchor manifest. Every user-facing
   capability has at least one row pointing at one anchor test that
   proves the capability end-to-end.
4. `challenges/scripts/bluff_scanner_challenge.sh` — wraps the scanner
   as a challenge.
5. `challenges/scripts/mutation_ratchet_challenge.sh` — runs Pitest
   (Kotlin/JVM) or `go-mutesting` (Go), enforces 90% kill on changed
   code and 80% project-wide ratchet.
6. `challenges/scripts/anchor_manifest_challenge.sh` — verifies every
   anchor row points at a callable test that exists.

**Enforcement:** All three challenges MUST run in `make qa-all` /
`runChallenges`. A violation in any channel blocks merge. Adding files
to scanner `EXCLUDE_PATHS` or expanding the baseline requires an
explicit justification comment naming the non-bluff context.

**Hard block scope:** new code (any file modified in the working tree)
must produce zero net new scanner hits AND must not lower the project
mutation kill rate. Pre-existing bluff hits are recorded in the
baseline; baseline reduction is the work of campaign sub-projects 4–5.

**Why:** the project has a documented history of feature regressions
that test suites failed to catch — tests passed while features were
unusable. CONST-034 makes that class of failure detectable and blocks
its recurrence.

**See also:** `docs/ANTI_BLUFF.md` (background and runbook),
`docs/behavior-anchors.md` (manifest), `../docs/campaigns/anti-bluff/CAMPAIGN.md`
(active campaign tracker, in main Yole repo).

## Definition of Done

A change is done when:

1. The code change is committed.
2. All project-level tests pass on a clean clone.
3. All challenges in `challenges/scripts/` pass on the running host.
4. Governance docs (`CONSTITUTION.md`, `AGENTS.md`, `CLAUDE.md`) are
   coherent with the change.

## See also

- `README.md` — project overview.
- `AGENTS.md` — guidance for AI coding agents.
- `CLAUDE.md` — guidance specifically for Claude Code.
- `docs/ANTI_BLUFF.md` — CONST-034 background and runbook.
```

Replace `${SUBMODULE}` literally with `Challenges`, `Containers`, or `HelixQA` per the running agent's parameter.

- [ ] **Step 2: Commit**

```bash
git add CONSTITUTION.md
git commit -m "$(cat <<'EOF'
chore(governance): add CONSTITUTION.md with CONST-034 anti-bluff rule

This submodule lacked a CONSTITUTION.md. Adds the standard 5 mandatory
standards inherited from the main Yole repo plus CONST-034 verbatim
(anti-bluff test discipline). Three-layer user-visible definition
(app / KMP / Go submodule) is built into the rule itself.

Refs: ../docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Push to all remotes**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.4: Add anti-bluff section to `CLAUDE.md` and `AGENTS.md`

**Files:**
- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Append the anti-bluff section to `CLAUDE.md`**

Read the existing `CLAUDE.md`, then append (at the end of file, before any trailing whitespace) the following block:

```markdown

## ⚠️ Anti-Bluff Test Discipline — CONST-034

**STRICTLY FORBIDDEN: never write a test that can pass without
exercising the user-visible behavior it claims to verify.** This
includes mocking the unit under test, trivial-assertion-only tests,
permanent skips without an exempt ticket reference, no-op
`t.Run("", func)` bodies, and any test whose body would still pass if
every line of the unit under test were replaced with a trivial stub.
See CONST-034 in `CONSTITUTION.md` for the full rule.

**Verification commands** (run before claiming a fix is complete):

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS. Pre-existing bluff hits are tracked in
`challenges/baselines/bluff-baseline.txt`; do not extend the baseline
without an explicit justification comment.
```

- [ ] **Step 2: Append the same section to `AGENTS.md`**

Same content as Step 1, appended to `AGENTS.md`.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md AGENTS.md
git commit -m "$(cat <<'EOF'
docs(governance): add CONST-034 anti-bluff section to CLAUDE.md and AGENTS.md

Cross-references CONST-034 from the agent-facing governance docs.
Verification command block matches the existing CONST-033
host-power-management style.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.5: Drop in the static scanner

**Files:**
- Create: `scripts/anti-bluff/bluff-scanner.sh`
- Create: `scripts/anti-bluff/lib/go.sh`
- Create: `scripts/anti-bluff/install-hooks.sh`
- Create: `scripts/anti-bluff/pre-commit-hook.sh`
- Create: `scripts/anti-bluff/tests/fixtures/bluff_g_001_skip.go`
- Create: `scripts/anti-bluff/tests/fixtures/bluff_g_007_trivial.go`
- Create: `scripts/anti-bluff/tests/fixtures/clean_test.go`
- Create: `scripts/anti-bluff/tests/run-fixtures.sh`

- [ ] **Step 1: Create directory layout**

```bash
mkdir -p scripts/anti-bluff/lib scripts/anti-bluff/tests/fixtures
```

- [ ] **Step 2: Write `scripts/anti-bluff/bluff-scanner.sh`**

```bash
#!/usr/bin/env bash
# CONST-034 static scanner — entry point.
# Walks tracked source files, dispatches per-language matchers, applies
# baseline filter, prints new hits. Exit codes:
#   0 clean
#   1 new bluff outside baseline (gate failure)
#   2 baseline drift (a baselined hit is gone — baseline is stale)
#   3 invocation error
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MODE="all"
BASELINE="${ROOT_DIR}/challenges/baselines/bluff-baseline.txt"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --baseline) BASELINE="$2"; shift 2 ;;
    -h|--help)
      echo "usage: bluff-scanner.sh [--mode all|changed] [--baseline <path>]"
      exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 3 ;;
  esac
done

# Source language helpers (presence depends on which repo we're in)
[[ -f "${SCRIPT_DIR}/lib/kotlin.sh" ]] && source "${SCRIPT_DIR}/lib/kotlin.sh"
[[ -f "${SCRIPT_DIR}/lib/go.sh"     ]] && source "${SCRIPT_DIR}/lib/go.sh"

# Determine file list
if [[ "$MODE" == "changed" ]]; then
  # In a git checkout, "changed" = files that differ from master.
  if git -C "${ROOT_DIR}" rev-parse master >/dev/null 2>&1; then
    mapfile -t FILES < <(git -C "${ROOT_DIR}" diff --name-only master...HEAD)
  else
    mapfile -t FILES < <(git -C "${ROOT_DIR}" diff --name-only --cached)
  fi
elif [[ "$MODE" == "all" ]]; then
  mapfile -t FILES < <(git -C "${ROOT_DIR}" ls-files)
else
  echo "invalid --mode: ${MODE}" >&2; exit 3
fi

HITS_FILE="$(mktemp -t bluff-scanner.XXXXXX)"
trap 'rm -f "${HITS_FILE}"' EXIT

for f in "${FILES[@]}"; do
  [[ -z "$f" ]] && continue
  fpath="${ROOT_DIR}/${f}"
  [[ ! -f "$fpath" ]] && continue

  case "$f" in
    *.kt|*.kts)
      if declare -F scan_kotlin >/dev/null; then
        scan_kotlin "$f" "$fpath" >>"${HITS_FILE}" || true
      fi
      ;;
    *.go)
      if declare -F scan_go >/dev/null; then
        scan_go "$f" "$fpath" >>"${HITS_FILE}" || true
      fi
      ;;
  esac
done

# Build baseline key set (Section 1 only).
BASELINE_KEYS_FILE="$(mktemp -t bluff-baseline.XXXXXX)"
trap 'rm -f "${HITS_FILE}" "${BASELINE_KEYS_FILE}"' EXIT
if [[ -f "${BASELINE}" ]]; then
  awk '
    /^# === SECTION 2/ { exit }
    /^[^#[:space:]]/ && NF > 0 { print }
  ' "${BASELINE}" > "${BASELINE_KEYS_FILE}"
else
  : > "${BASELINE_KEYS_FILE}"
fi

# Filter: a hit line is "path:line:BLUFF-ID:context"; key is "path:BLUFF-ID".
NEW_HITS=0
SEEN_BASELINE_KEYS_FILE="$(mktemp -t bluff-seen.XXXXXX)"
trap 'rm -f "${HITS_FILE}" "${BASELINE_KEYS_FILE}" "${SEEN_BASELINE_KEYS_FILE}"' EXIT
: > "${SEEN_BASELINE_KEYS_FILE}"

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  key=$(awk -F: '{print $1 ":" $3}' <<<"$line")
  if grep -qxF "${key}" "${BASELINE_KEYS_FILE}"; then
    echo "${key}" >> "${SEEN_BASELINE_KEYS_FILE}"
  else
    echo "$line"
    NEW_HITS=$((NEW_HITS+1))
  fi
done < "${HITS_FILE}"

# Drift detection: baseline keys not seen this run = stale baseline.
DRIFT=0
sort -u "${SEEN_BASELINE_KEYS_FILE}" > "${SEEN_BASELINE_KEYS_FILE}.sorted"
sort -u "${BASELINE_KEYS_FILE}"       > "${BASELINE_KEYS_FILE}.sorted"
mapfile -t STALE < <(comm -23 "${BASELINE_KEYS_FILE}.sorted" "${SEEN_BASELINE_KEYS_FILE}.sorted")
if (( ${#STALE[@]} > 0 )); then
  echo "" >&2
  echo "WARN: ${#STALE[@]} baseline entries are no longer present; baseline is stale." >&2
  printf '  %s\n' "${STALE[@]}" >&2
  DRIFT=1
fi

if (( NEW_HITS > 0 )); then
  echo "" >&2
  echo "FAIL: ${NEW_HITS} new bluff hit(s) outside baseline. Fix or add an exempt comment." >&2
  exit 1
fi

if (( DRIFT > 0 )); then
  echo "" >&2
  echo "FAIL: baseline is stale (${#STALE[@]} entries). Run 'make update-baseline' to refresh." >&2
  exit 2
fi

echo "OK: scanner clean (mode=${MODE})." >&2
exit 0
```

```bash
chmod +x scripts/anti-bluff/bluff-scanner.sh
```

- [ ] **Step 3: Write `scripts/anti-bluff/lib/go.sh`**

```bash
#!/usr/bin/env bash
# Go-flavored bluff patterns. Sourced by bluff-scanner.sh.
# Each pattern emits "<relative path>:<line>:BLUFF-G-NNN:<context>"

scan_go() {
  local relpath="$1" fpath="$2"

  # BLUFF-G-001: t.Skip() / t.Skipf() without ANTI-BLUFF-EXEMPT on prev line.
  awk -v rel="${relpath}" '
    /ANTI-BLUFF-EXEMPT/ { exempt[NR+1] = 1 }
    /[[:space:]]t\.Skip[f]?\(/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-G-001:t.Skip without exempt comment"
    }
  ' "$fpath"

  # BLUFF-G-005: t.Run("", func(t *testing.T) { }) — empty named subtest.
  awk -v rel="${relpath}" '
    /t\.Run\("",[[:space:]]*func\(t \*testing\.T\)[[:space:]]*\{[[:space:]]*\}\)/ {
      print rel ":" NR ":BLUFF-G-005:empty t.Run subtest"
    }
  ' "$fpath"

  # BLUFF-G-006: empty TestXxx body on a single line.
  awk -v rel="${relpath}" '
    /^func Test[A-Z][A-Za-z0-9_]*\(t \*testing\.T\)[[:space:]]*\{[[:space:]]*\}[[:space:]]*$/ {
      print rel ":" NR ":BLUFF-G-006:empty test body"
    }
  ' "$fpath"

  # BLUFF-G-007: assert.True(t, true) / assert.NotNil(t, x) as a literal line.
  awk -v rel="${relpath}" '
    /^[[:space:]]*assert\.True\(t,[[:space:]]*true\)/ {
      print rel ":" NR ":BLUFF-G-007:assert.True(t, true) trivial"
    }
  ' "$fpath"

  # BLUFF-G-003: test function whose body has only t.Log calls (no t.Fatal/Error/Errorf, no assert.).
  # Conservative single-pass: extract each TestXxx body, count assertion-like calls.
  awk -v rel="${relpath}" '
    function flush(start_line) {
      if (start_line > 0 && asserts == 0 && logs > 0) {
        print rel ":" start_line ":BLUFF-G-003:no-assert test (only t.Log)"
      }
    }
    /^func Test[A-Z][A-Za-z0-9_]*\(t \*testing\.T\)[[:space:]]*\{/ {
      flush(start_line)
      start_line = NR; brace = 1; asserts = 0; logs = 0
      next
    }
    start_line > 0 {
      n = gsub(/\{/, "&"); brace += n
      n = gsub(/\}/, "&"); brace -= n
      if ($0 ~ /t\.(Fatal|Fatalf|Error|Errorf)\(|assert\./)  asserts++
      if ($0 ~ /t\.Log[f]?\(/) logs++
      if (brace == 0) { flush(start_line); start_line = 0 }
    }
    END { flush(start_line) }
  ' "$fpath"
}
```

- [ ] **Step 4: Write `scripts/anti-bluff/install-hooks.sh`**

```bash
#!/usr/bin/env bash
# Installs the anti-bluff pre-commit hook into .git/hooks/pre-commit.
# Idempotent.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

HOOK_TARGET="${ROOT_DIR}/.git/hooks/pre-commit"
HOOK_SOURCE="${SCRIPT_DIR}/pre-commit-hook.sh"

if [[ -e "${HOOK_TARGET}" && ! -L "${HOOK_TARGET}" ]]; then
  echo "Existing non-symlink pre-commit hook at ${HOOK_TARGET}; refusing to overwrite." >&2
  echo "Move it aside, then re-run." >&2
  exit 1
fi

ln -sf "${HOOK_SOURCE}" "${HOOK_TARGET}"
chmod +x "${HOOK_SOURCE}"
echo "Installed ${HOOK_TARGET} -> ${HOOK_SOURCE}"
```

```bash
chmod +x scripts/anti-bluff/install-hooks.sh
```

- [ ] **Step 5: Write `scripts/anti-bluff/pre-commit-hook.sh`**

```bash
#!/usr/bin/env bash
# Pre-commit hook — runs scanner + manifest check on staged files.
# Mutation gate is excluded (too slow for pre-commit).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Run scanner in changed-mode against staged files only.
"${SCRIPT_DIR}/bluff-scanner.sh" --mode changed

# Run anchor manifest check (cheap, < 1s).
if [[ -f "${ROOT_DIR}/challenges/scripts/anchor_manifest_challenge.sh" ]]; then
  bash "${ROOT_DIR}/challenges/scripts/anchor_manifest_challenge.sh"
fi
```

```bash
chmod +x scripts/anti-bluff/pre-commit-hook.sh
```

- [ ] **Step 6: Write fixtures**

`scripts/anti-bluff/tests/fixtures/bluff_g_001_skip.go`:

```go
package fixtures
import "testing"
func TestBluffG001Skip(t *testing.T) {
	t.Skip()
}
```

`scripts/anti-bluff/tests/fixtures/bluff_g_007_trivial.go`:

```go
package fixtures
import (
	"testing"
	"github.com/stretchr/testify/assert"
)
func TestBluffG007Trivial(t *testing.T) {
	assert.True(t, true)
}
```

`scripts/anti-bluff/tests/fixtures/clean_test.go`:

```go
package fixtures
import "testing"
func TestClean(t *testing.T) {
	got := 1 + 1
	want := 2
	if got != want {
		t.Fatalf("got %d, want %d", got, want)
	}
}
```

- [ ] **Step 7: Write `scripts/anti-bluff/tests/run-fixtures.sh`**

```bash
#!/usr/bin/env bash
# Self-test for the scanner: each fixture must produce its expected verdict.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_GO="${SCRIPT_DIR}/../lib/go.sh"
source "${LIB_GO}"

run_fixture() {
  local name="$1" expected_id="$2" file="$3"
  local out
  out="$(scan_go "${name}" "${file}" || true)"
  if [[ -z "$expected_id" ]]; then
    if [[ -n "$out" ]]; then
      echo "FAIL ${name}: expected no hits, got: ${out}"
      return 1
    fi
  else
    if ! grep -q "${expected_id}" <<< "${out}"; then
      echo "FAIL ${name}: expected ${expected_id} hit, got: ${out}"
      return 1
    fi
  fi
  echo "OK ${name}: ${expected_id:-clean}"
}

failed=0
run_fixture "bluff_g_001_skip.go"    "BLUFF-G-001" "${SCRIPT_DIR}/fixtures/bluff_g_001_skip.go"    || failed=1
run_fixture "bluff_g_007_trivial.go" "BLUFF-G-007" "${SCRIPT_DIR}/fixtures/bluff_g_007_trivial.go" || failed=1
run_fixture "clean_test.go"          ""            "${SCRIPT_DIR}/fixtures/clean_test.go"          || failed=1

if (( failed )); then
  echo "Scanner self-test FAILED"
  exit 1
fi
echo "Scanner self-test PASSED"
```

```bash
chmod +x scripts/anti-bluff/tests/run-fixtures.sh
```

- [ ] **Step 8: Run scanner self-test**

```bash
bash scripts/anti-bluff/tests/run-fixtures.sh
```

Expected: three `OK` lines and `Scanner self-test PASSED`. If any line says FAIL, fix the corresponding awk pattern in `lib/go.sh` until it passes; do not proceed until all three pass.

- [ ] **Step 9: Commit**

```bash
git add scripts/anti-bluff
git commit -m "$(cat <<'EOF'
feat(anti-bluff): add static scanner and hook installer

Adds CONST-034 static scanner (bluff-scanner.sh) with Go pattern
library (lib/go.sh) detecting BLUFF-G-001/003/005/006/007. Pre-commit
hook installer (install-hooks.sh) and hook (pre-commit-hook.sh).
Self-test fixtures with run-fixtures.sh confirm scanner verdicts on
known-good and known-bluff Go snippets.

Patterns BLUFF-G-002 (testing.Short short-circuit) and BLUFF-G-004
(gomock of same-package SUT) require AST awareness and are deferred
to a follow-up.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 10: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.6: Capture scanner baseline (Section 1)

**Files:**
- Create: `challenges/baselines/bluff-baseline.txt`

- [ ] **Step 1: Run scanner full tree without baseline**

```bash
mkdir -p challenges/baselines
# Run scanner with /dev/null as baseline so every hit is "new"; capture them.
bash scripts/anti-bluff/bluff-scanner.sh --mode all --baseline /dev/null > /tmp/bluff-hits.txt 2>&1 || true
```

The scanner exits 1 because everything is "new"; that's expected here.

- [ ] **Step 2: Build the baseline file**

```bash
cat > challenges/baselines/bluff-baseline.txt <<'BASELINE_HEADER'
# CONST-034 baseline file.
# Captured: 2026-05-01 (sub-project 1).
# Reducing this file is the work of campaign sub-projects 4-5.
# Section 1: static scanner hits, format <path>:<BLUFF-ID>.
# Section 2: per-file mutation kill rates, format <path>:<rate>:<mutants>.
# Section 3: anchor-manifest gaps, format <capability-id>:MISSING_ANCHOR.
# === SECTION 1: STATIC SCANNER HITS ===
BASELINE_HEADER

# Extract path:BLUFF-ID keys, dedupe and sort.
awk -F: '/^[^#]/ && NF > 1 { print $1 ":" $3 }' /tmp/bluff-hits.txt \
  | grep -E ':BLUFF-[KG]-[0-9]{3}$' \
  | sort -u \
  >> challenges/baselines/bluff-baseline.txt

cat >> challenges/baselines/bluff-baseline.txt <<'BASELINE_MID'
# === SECTION 2: PER-FILE MUTATION KILL RATES ===
BASELINE_MID

cat >> challenges/baselines/bluff-baseline.txt <<'BASELINE_TAIL'
# === SECTION 3: ANCHOR-MANIFEST GAPS ===
BASELINE_TAIL
```

- [ ] **Step 3: Verify scanner now passes against captured baseline**

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
echo "scanner exit code: $?"
```

Expected: `OK: scanner clean (mode=all).` and exit code `0`.

- [ ] **Step 4: Commit**

```bash
git add challenges/baselines/bluff-baseline.txt
git commit -m "$(cat <<'EOF'
chore(anti-bluff): capture scanner baseline (Section 1)

Snapshot of pre-existing scanner hits as of CONST-034 landing.
Section 2 (mutation kill rates) and Section 3 (anchor-manifest gaps)
populated in subsequent commits in this sub-project.

Reducing this file is the work of campaign sub-projects 4-5; it is
read-only for sub-project 1.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 5: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.7: Configure `go-mutesting`

**Files:**
- Create: `.go-mutesting.yml`
- Modify: `go.mod` (toolchain dep)

- [ ] **Step 1: Install go-mutesting toolchain**

```bash
go install github.com/avito-tech/go-mutesting/cmd/go-mutesting@latest
```

Expected: tool installed under `$GOPATH/bin` or `$HOME/go/bin`. Verify with:

```bash
which go-mutesting || ls -la "$(go env GOPATH)/bin/go-mutesting"
```

- [ ] **Step 2: Write `.go-mutesting.yml`**

```yaml
# CONST-034 mutation testing config (go-mutesting).
# Threshold rules are enforced by challenges/scripts/mutation_ratchet_challenge.sh,
# not by go-mutesting itself.
mutators:
  - branch/case
  - branch/if
  - expression/remove
  - statement/remove
  - numbers/incrementer
  - numbers/decrementer
exec:
  timeout: 60   # seconds per mutant
match: ".*"
exclude:
  - vendor/.*
  - .*\.pb\.go$
  - .*_mock\.go$
  - scripts/anti-bluff/.*
```

- [ ] **Step 3: Smoke test on one package**

Pick the smallest Go package in this submodule (find with `find . -name "*.go" -not -path "./vendor/*" | xargs -I{} dirname {} | sort -u | head -5`); run `go-mutesting` on it:

```bash
PKG=$(find . -name "*.go" -not -path "./vendor/*" | xargs -I{} dirname {} | sort -u | head -1)
go-mutesting --config=.go-mutesting.yml "${PKG}/..."
```

Expected: tool runs, prints mutants and their kill/survive status. If it errors, debug; do not proceed until a clean run produces output.

- [ ] **Step 4: Commit**

```bash
git add .go-mutesting.yml
git commit -m "$(cat <<'EOF'
feat(anti-bluff): configure go-mutesting for CONST-034 mutation gate

Standard mutator set (branch, expression-remove, statement-remove,
numbers). 60s per-mutant timeout. Excludes generated and vendored code.
Threshold enforcement (90% changed / 80% project ratchet) lives in
challenges/scripts/mutation_ratchet_challenge.sh, not in this config.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 5: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.8: Run full mutation pass and capture baseline (Section 2)

This is the slow step (~5–15 min on a Go submodule).

**Files:**
- Modify: `challenges/baselines/bluff-baseline.txt`

- [ ] **Step 1: Run go-mutesting across all packages**

```bash
go-mutesting --config=.go-mutesting.yml ./... > /tmp/mutation-output.txt 2>&1 || true
```

This may take a while. The exit code is non-zero if mutants survived; that's fine — we're capturing the current state, not gating on it.

- [ ] **Step 2: Parse output into baseline format**

go-mutesting prints per-mutation lines and a summary. We need per-file kill rate.

```bash
python3 - <<'PYEOF'
import re, collections, sys
killed = collections.Counter()
total = collections.Counter()
with open("/tmp/mutation-output.txt") as f:
    for line in f:
        m = re.match(r"PASS\s+\".*\.go.original\.go\".*at file (.+\.go), line", line)
        if m:
            killed[m.group(1)] += 1
            total[m.group(1)] += 1
            continue
        m = re.match(r"FAIL\s+\".*\.go.original\.go\".*at file (.+\.go), line", line)
        if m:
            total[m.group(1)] += 1
            continue
files = sorted(total.keys())
for f in files:
    rate = round(100 * killed[f] / total[f]) if total[f] else 0
    print(f"{f}:{rate}:{total[f]}")
PYEOF
```

(go-mutesting's exact output format may differ between versions; adjust the regex above if the awk-equivalent does not match. The implementing agent should grep a sample of `/tmp/mutation-output.txt` first to confirm format. If go-mutesting emits JSON, prefer parsing that.)

Pipe the script's output to a temp file, then splice into the baseline:

```bash
python3 - <<'PYEOF' > /tmp/mutation-section2.txt
# (script body from previous step)
PYEOF
```

- [ ] **Step 3: Splice Section 2 into baseline**

Replace the empty Section 2 in `challenges/baselines/bluff-baseline.txt` with the captured rates. Use a small awk script:

```bash
awk -v new_section2="$(cat /tmp/mutation-section2.txt)" '
  /^# === SECTION 2:/ { print; print new_section2; in_section2=1; next }
  /^# === SECTION 3:/ { in_section2=0; print; next }
  !in_section2 { print }
' challenges/baselines/bluff-baseline.txt > challenges/baselines/bluff-baseline.txt.new
mv challenges/baselines/bluff-baseline.txt.new challenges/baselines/bluff-baseline.txt
```

- [ ] **Step 4: Sanity check baseline file**

```bash
grep -c '^[^#]' challenges/baselines/bluff-baseline.txt
head -50 challenges/baselines/bluff-baseline.txt
```

Expected: a positive number of non-comment lines, three sections clearly demarcated.

- [ ] **Step 5: Commit**

```bash
git add challenges/baselines/bluff-baseline.txt
git commit -m "$(cat <<'EOF'
chore(anti-bluff): capture mutation kill rate baseline (Section 2)

Per-file mutation kill rates from initial go-mutesting full-tree run.
Baseline format: <path>:<rate-percent>:<mutants-tested>.

Project-wide ratchet (80% floor) and changed-file gate (90% floor) are
enforced by challenges/scripts/mutation_ratchet_challenge.sh against
this snapshot.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 6: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.9: Add anchor-manifest skeleton

**Files:**
- Create: `docs/behavior-anchors.md`

- [ ] **Step 1: Write the manifest skeleton**

```markdown
---
schema_version: 1
constitution_rule: CONST-034
last_audit: 2026-05-01
---

# Behavior Anchor Manifest — ${SUBMODULE}

Every row is a user-facing capability and the single anchor test that
proves it works end-to-end. See CONST-034 in `CONSTITUTION.md`.

## Status legend

- `active` — anchor exists and is callable; capability is verified.
- `pending-anchor` — capability declared, anchor test does not yet
  exist. Listed in `challenges/baselines/bluff-baseline.txt` Section 3.
  Reducing this state is the work of campaign sub-project 4.
- `retired` — capability removed; row kept for history.

## Capabilities

| id | layer | capability | anchor_test_path | verifies | status |
|----|-------|------------|------------------|----------|--------|

(Manifest is empty at sub-project 1 close. Rows populated during the
audit phase, sub-project 3.)
```

- [ ] **Step 2: Commit**

```bash
git add docs/behavior-anchors.md
git commit -m "$(cat <<'EOF'
docs(anti-bluff): add behavior-anchors.md skeleton

Manifest file for tracking user-facing capability anchors per
CONST-034. Schema-only at sub-project 1 close (zero rows).

Rows populated during sub-project 3 (audit). Rows with
status=pending-anchor will be tracked in
challenges/baselines/bluff-baseline.txt Section 3.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.10: Add challenge scripts and Makefile wiring

**Files:**
- Create: `challenges/scripts/bluff_scanner_challenge.sh`
- Create: `challenges/scripts/anchor_manifest_challenge.sh`
- Create: `challenges/scripts/mutation_ratchet_challenge.sh`
- Modify: `Makefile`

- [ ] **Step 1: Write `challenges/scripts/bluff_scanner_challenge.sh`**

```bash
#!/usr/bin/env bash
# Wraps bluff-scanner.sh as a challenge.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
exec bash "${ROOT_DIR}/scripts/anti-bluff/bluff-scanner.sh" --mode all
```

```bash
chmod +x challenges/scripts/bluff_scanner_challenge.sh
```

- [ ] **Step 2: Write `challenges/scripts/anchor_manifest_challenge.sh`**

```bash
#!/usr/bin/env bash
# CONST-034 anchor-manifest challenge.
# Validates docs/behavior-anchors.md:
#   1. File exists and parses.
#   2. Every active row's anchor_test_path resolves to an existing
#      file containing the named test symbol.
#   3. Every pending-anchor row appears in baseline Section 3.
#   4. Cross-check against docs/CAPABILITIES.md if present (no-op if absent).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MANIFEST="${ROOT_DIR}/docs/behavior-anchors.md"
BASELINE="${ROOT_DIR}/challenges/baselines/bluff-baseline.txt"
CAPABILITIES="${ROOT_DIR}/docs/CAPABILITIES.md"

if [[ ! -f "${MANIFEST}" ]]; then
  echo "FAIL: ${MANIFEST} missing." >&2
  exit 1
fi

failed=0
# Extract data rows (Markdown table after header line containing "anchor_test_path").
mapfile -t ROWS < <(awk '
  /^\| *id *\|/ { in_table=1; next }
  in_table && /^\|[ ]*-/ { next }
  in_table && /^\|/ { print }
' "${MANIFEST}")

for row in "${ROWS[@]}"; do
  IFS='|' read -ra cols <<< "${row}"
  # cols[0] is empty (leading |), real columns are cols[1..6]
  id="$(echo "${cols[1]}"   | xargs)"
  layer="$(echo "${cols[2]}" | xargs)"
  capability="$(echo "${cols[3]}" | xargs)"
  anchor="$(echo "${cols[4]}" | xargs)"
  verifies="$(echo "${cols[5]}" | xargs)"
  status="$(echo "${cols[6]}" | xargs)"

  if [[ -z "${id}" ]]; then continue; fi

  case "${status}" in
    active)
      # anchor format: "path/to/file.go::TestName" or "path/to/file.kt::Class::method"
      file_part="${anchor%%::*}"
      sym_part="${anchor#*::}"
      if [[ ! -f "${ROOT_DIR}/${file_part}" ]]; then
        echo "FAIL: ${id}: anchor file ${file_part} not found." >&2
        failed=1
        continue
      fi
      # Crude symbol check: grep the file for the test name.
      first_sym="${sym_part%%::*}"
      if ! grep -qE "(func[[:space:]]+|fun[[:space:]]+)${first_sym}\b" "${ROOT_DIR}/${file_part}"; then
        echo "FAIL: ${id}: symbol ${first_sym} not found in ${file_part}." >&2
        failed=1
      fi
      ;;
    pending-anchor)
      if [[ -f "${BASELINE}" ]]; then
        if ! grep -qxF "${id}:MISSING_ANCHOR" "${BASELINE}"; then
          echo "FAIL: ${id}: pending-anchor row not in baseline Section 3." >&2
          failed=1
        fi
      fi
      ;;
    retired)
      ;;
    *)
      echo "FAIL: ${id}: unknown status '${status}'." >&2
      failed=1
      ;;
  esac
done

# Cross-check (no-op if CAPABILITIES.md absent).
if [[ -f "${CAPABILITIES}" ]]; then
  mapfile -t ACTIVE_IDS < <(awk -F'|' '
    /^\| *id *\|/ { in_table=1; next }
    in_table && /^\|[ ]*-/ { next }
    in_table && /^\|/ { gsub(/^[ ]+|[ ]+$/, "", $7); if ($7 == "active") { gsub(/^[ ]+|[ ]+$/, "", $2); print $2 } }
  ' "${MANIFEST}")
  mapfile -t DECLARED_IDS < <(grep -oE 'CAP-[0-9]{3}' "${CAPABILITIES}" | sort -u)
  for cid in "${DECLARED_IDS[@]}"; do
    if ! printf '%s\n' "${ACTIVE_IDS[@]}" | grep -qxF "${cid}"; then
      echo "FAIL: ${cid} declared in CAPABILITIES.md but no active anchor." >&2
      failed=1
    fi
  done
fi

if (( failed )); then
  echo "FAIL: anchor manifest challenge." >&2
  exit 1
fi
echo "OK: anchor manifest valid."
exit 0
```

```bash
chmod +x challenges/scripts/anchor_manifest_challenge.sh
```

- [ ] **Step 3: Write `challenges/scripts/mutation_ratchet_challenge.sh`**

```bash
#!/usr/bin/env bash
# CONST-034 mutation ratchet challenge (Go).
# Modes:
#   default (no --mode): run on changed files vs master.
#   --mode all: run full project (slow).
# Compares against challenges/baselines/bluff-baseline.txt Section 2.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MODE="changed"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 3 ;;
  esac
done

BASELINE="${ROOT_DIR}/challenges/baselines/bluff-baseline.txt"
CONFIG="${ROOT_DIR}/.go-mutesting.yml"

if ! command -v go-mutesting >/dev/null; then
  GOPATH_BIN="$(go env GOPATH)/bin"
  export PATH="${GOPATH_BIN}:${PATH}"
fi

if ! command -v go-mutesting >/dev/null; then
  echo "FAIL: go-mutesting not installed (run: go install github.com/avito-tech/go-mutesting/cmd/go-mutesting@latest)" >&2
  exit 1
fi

OUT="$(mktemp)"
trap 'rm -f "$OUT"' EXIT

if [[ "$MODE" == "changed" ]]; then
  mapfile -t CHANGED < <(git -C "${ROOT_DIR}" diff --name-only master...HEAD -- '*.go')
  if (( ${#CHANGED[@]} == 0 )); then
    echo "OK: no Go changes vs master."
    exit 0
  fi
  PKGS=()
  for f in "${CHANGED[@]}"; do PKGS+=("./$(dirname "$f")"); done
  PKGS=($(printf '%s\n' "${PKGS[@]}" | sort -u))
  go-mutesting --config="${CONFIG}" "${PKGS[@]}" > "${OUT}" 2>&1 || true
else
  go-mutesting --config="${CONFIG}" ./... > "${OUT}" 2>&1 || true
fi

# Parse per-file kill rates (regex must match installed go-mutesting version).
python3 - "${OUT}" "${BASELINE}" "${MODE}" <<'PYEOF'
import collections, re, sys
out_path, baseline_path, mode = sys.argv[1], sys.argv[2], sys.argv[3]
killed = collections.Counter(); total = collections.Counter()
with open(out_path) as f:
    for line in f:
        m = re.search(r"at file (\S+\.go), line", line)
        if not m: continue
        fn = m.group(1)
        total[fn] += 1
        if line.startswith("PASS"):  killed[fn] += 1

baseline = {}
section = None
with open(baseline_path) as f:
    for line in f:
        line = line.rstrip("\n")
        if line.startswith("# === SECTION 2"): section = 2; continue
        if line.startswith("# === SECTION 3"): section = 3; continue
        if section == 2 and line and not line.startswith("#"):
            try:
                p, r, n = line.split(":")
                baseline[p] = int(r)
            except ValueError: continue

failed = False
for fn in sorted(total.keys()):
    rate = 100 * killed[fn] // total[fn] if total[fn] else 0
    if mode == "changed" and rate < 90:
        print(f"FAIL: {fn} kill rate {rate}% < 90% (changed-code threshold)")
        failed = True
    if fn in baseline and rate < baseline[fn]:
        print(f"FAIL: {fn} kill rate {rate}% < baseline {baseline[fn]}% (ratchet)")
        failed = True

if mode == "all":
    overall_killed = sum(killed.values()); overall_total = sum(total.values())
    overall_rate = 100 * overall_killed // overall_total if overall_total else 0
    if overall_rate < 80:
        print(f"FAIL: project-wide kill rate {overall_rate}% < 80%")
        failed = True

sys.exit(1 if failed else 0)
PYEOF

echo "OK: mutation ratchet (mode=${MODE})."
```

```bash
chmod +x challenges/scripts/mutation_ratchet_challenge.sh
```

- [ ] **Step 4: Update `Makefile`**

Read the existing `Makefile`. Append (and merge into existing `qa-all` if present) the following:

```makefile

# === CONST-034 anti-bluff gates ===
.PHONY: anti-bluff anti-bluff-scan anti-bluff-anchors anti-bluff-mutation anti-bluff-mutation-changed update-baseline

anti-bluff-scan:
	@bash scripts/anti-bluff/bluff-scanner.sh --mode all

anti-bluff-anchors:
	@bash challenges/scripts/anchor_manifest_challenge.sh

anti-bluff-mutation:
	@bash challenges/scripts/mutation_ratchet_challenge.sh --mode all

anti-bluff-mutation-changed:
	@bash challenges/scripts/mutation_ratchet_challenge.sh

anti-bluff: anti-bluff-scan anti-bluff-anchors anti-bluff-mutation-changed

# Append anti-bluff to existing qa-all target if it exists; otherwise define.
ifndef HAS_QA_ALL
qa-all: anti-bluff
endif

update-baseline:
	@echo "Manual baseline update — see docs/ANTI_BLUFF.md"
	@echo "1. Run scanner: bash scripts/anti-bluff/bluff-scanner.sh --mode all"
	@echo "2. Run mutation: bash challenges/scripts/mutation_ratchet_challenge.sh --mode all"
	@echo "3. Edit challenges/baselines/bluff-baseline.txt to reflect new state."
```

If the existing Makefile already has a `qa-all` target, modify it instead to add `anti-bluff` to its dependency list.

- [ ] **Step 5: Smoke test new targets**

```bash
make anti-bluff-scan
make anti-bluff-anchors
```

Expected: both print `OK:` lines and exit 0.

```bash
make anti-bluff-mutation-changed
```

Expected: `OK: no Go changes vs master.` (since the campaign-branch's only changes so far are non-Go files).

- [ ] **Step 6: Install pre-commit hook**

```bash
bash scripts/anti-bluff/install-hooks.sh
ls -la .git/hooks/pre-commit
```

Expected: symlink to `scripts/anti-bluff/pre-commit-hook.sh`.

- [ ] **Step 7: Commit**

```bash
git add challenges/scripts Makefile
git commit -m "$(cat <<'EOF'
feat(anti-bluff): wire CONST-034 challenges into Makefile

Adds three new challenge scripts under challenges/scripts/:
- bluff_scanner_challenge.sh — full-tree scanner pass.
- anchor_manifest_challenge.sh — manifest schema and resolvability check.
- mutation_ratchet_challenge.sh — go-mutesting + ratchet enforcement.

Makefile gains anti-bluff (default), anti-bluff-scan, anti-bluff-anchors,
anti-bluff-mutation, anti-bluff-mutation-changed, update-baseline. qa-all
now includes anti-bluff.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 8: Push**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" campaign/anti-bluff; done
```

### Task 1.11: End-to-end verification (must-pass + deliberate-bluff trip test)

**Files:** none (verification only). Final tripwire test creates and immediately deletes a fixture.

- [ ] **Step 1: Run `make qa-all`**

```bash
make qa-all
```

Expected: all challenges (existing + new anti-bluff) pass. If any pre-existing challenge fails (i.e., not from CONST-034 changes), stop and investigate before proceeding — sub-project 1 should not regress existing functionality.

If anti-bluff challenges fail, debug. Likely causes:
- baseline file format off (Section 2 lines don't match parser regex)
- manifest table parsing miscount of `|` columns
- go-mutesting output regex doesn't match installed version

- [ ] **Step 2: Deliberate-bluff trip test — scanner must fail**

```bash
cat > /tmp/bluff_trip_test.go <<'GOEOF'
package main
import "testing"
func TestTripwire(t *testing.T) {
	t.Skip()
}
GOEOF
mkdir -p tmp_tripwire
cp /tmp/bluff_trip_test.go tmp_tripwire/
git add tmp_tripwire/
bash scripts/anti-bluff/bluff-scanner.sh --mode all
echo "scanner exit code: $?"
```

Expected: scanner prints `FAIL: 1 new bluff hit(s) outside baseline.`, exit code `1`. The hit is `tmp_tripwire/bluff_trip_test.go:4:BLUFF-G-001:t.Skip without exempt comment`.

If the trip test does NOT trip, the scanner is bluff itself — fix the matcher in `lib/go.sh` and re-run the fixture self-test plus this trip test.

- [ ] **Step 3: Remove the trip test**

```bash
git rm -r tmp_tripwire/
rm -f /tmp/bluff_trip_test.go
```

Expected: clean working tree (other than expected campaign-branch commits ahead of master).

- [ ] **Step 4: Final verification before merge**

```bash
make qa-all
git log @{u}..HEAD --oneline
```

Expected: `make qa-all` passes. Last command empty (all checkpoint commits already pushed).

### Task 1.12: Merge to master and push

**Files:** branch state.

- [ ] **Step 1: Switch to master and update**

```bash
git checkout master
git fetch --all --prune
git pull --ff-only
```

Expected: clean fast-forward to upstream master.

- [ ] **Step 2: Merge campaign branch (no fast-forward, to preserve checkpoint history)**

```bash
git merge --no-ff campaign/anti-bluff -m "$(cat <<'EOF'
Merge branch 'campaign/anti-bluff' — sub-project 1 (anti-bluff foundation)

Lands CONST-034 anti-bluff test discipline:
- CONSTITUTION.md (new file with rule)
- CLAUDE.md and AGENTS.md governance updates
- scripts/anti-bluff/ (scanner + lib + fixtures + hook installer)
- .go-mutesting.yml (mutation testing config)
- challenges/scripts/{bluff_scanner,anchor_manifest,mutation_ratchet}_challenge.sh
- challenges/baselines/bluff-baseline.txt (Sections 1, 2, empty 3)
- docs/behavior-anchors.md (manifest skeleton)
- docs/ANTI_BLUFF.md (runbook)
- Makefile anti-bluff targets, qa-all wired

Refs: ../docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Final qa-all on master**

```bash
make qa-all
```

Expected: pass.

- [ ] **Step 4: Push master to all remotes**

```bash
for r in ${PUSH_REMOTES}; do git push "$r" master; done
```

Expected: one successful push line per remote.

- [ ] **Step 5: Delete campaign branch (local + remote)**

```bash
git branch -d campaign/anti-bluff
for r in ${PUSH_REMOTES}; do git push "$r" --delete campaign/anti-bluff || true; done
```

(`|| true` because if a remote already cleaned up branch refs, deletion errors are non-fatal.)

- [ ] **Step 6: Capture submodule master SHA for main-repo pointer update**

```bash
SHA=$(git rev-parse HEAD)
echo "Submodule ${SUBMODULE} master SHA: ${SHA}"
```

Report this SHA back to the main session so it can be recorded in `docs/campaigns/anti-bluff/CAMPAIGN.md` per-repo state table and used in Phase 2 Task 2.13 (submodule pointer advance).

---

## Phase 2 — Main repo full rollout

After Phase 1 has completed for all 3 submodules and their master SHAs are captured, run the main repo phase.

Working directory: `/run/media/milosvasic/DATA4TB/Projects/Yole`. Branch: `campaign/anti-bluff` (already created in Phase 0).

Push remotes for main repo: `github`, `origin`, `upstream`.

### Task 2.1: Update tracker with Phase 1 results

**Files:**
- Modify: `docs/campaigns/anti-bluff/CAMPAIGN.md`

- [ ] **Step 1: Update per-repo state table**

Edit `docs/campaigns/anti-bluff/CAMPAIGN.md`. Replace the per-repo state table with the actual SHAs and checkpoint progress reported by Phase 1 agents:

```markdown
| Repo | Branch | Last checkpoint | Next checkpoint | Last push |
|------|--------|------------------|-------------------|-----------|
| Challenges | master | 1.12 (merged) | n/a (sub-project 1 done) | <Challenges SHA> |
| Containers | master | 1.12 (merged) | n/a (sub-project 1 done) | <Containers SHA> |
| HelixQA    | master | 1.12 (merged) | n/a (sub-project 1 done) | <HelixQA SHA> |
| Yole       | campaign/anti-bluff | 0.2 (tracker stub) | 2.2 (this commit) | <last push of campaign/anti-bluff> |
```

- [ ] **Step 2: Commit and push**

```bash
git add docs/campaigns/anti-bluff/CAMPAIGN.md
git commit -m "docs(campaign): record Phase 1 (submodule rollout) completion"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.2: Add `docs/ANTI_BLUFF.md` (Kotlin-flavored)

**Files:**
- Create: `docs/ANTI_BLUFF.md`

- [ ] **Step 1: Write the Kotlin-flavored runbook**

Same body as the Phase 1 version (Task 1.2 Step 2), with these substitutions for the Kotlin context:

- Replace the BLUFF-G-NNN reference table with the Kotlin BLUFF-K-NNN table:

```markdown
| ID | Pattern | Fix |
|----|---------|-----|
| BLUFF-K-001 | `mockk<X>()` / `mock<X>()` of the SUT | Stop mocking the SUT; use the real type or restructure the test. |
| BLUFF-K-002 | `assertTrue(true)` / `assertEquals(x, x)` / lone `assertNotNull` as sole assertion | Add a real assertion that exercises the SUT's behavior. |
| BLUFF-K-003 | `@Ignore` without `// ANTI-BLUFF-EXEMPT: <ticket>` | Remove the @Ignore and fix the test, or add an exempt comment with a tracking ticket. |
| BLUFF-K-004 | `assumeTrue(false)` / unconditional skip | Same as BLUFF-K-003. |
| BLUFF-K-005 | `runBlocking { }` body with no awaits / no asserts | Add real coroutine work and assertions. |
| BLUFF-K-006 | Empty `@Test` body | Fill or delete. |
| BLUFF-K-007 | Test class imports SUT but only references it via mock | Restructure: use the real type. |
| BLUFF-K-008 | `@Suppress("BLUFF")` without justification | Add justification comment or fix underlying issue. |
```

- Replace the Go anchor-test path format `<path>.go::TestName` with the Kotlin format `<path>.kt::ClassName::methodName`.
- Replace `go-mutesting` references with `Pitest` (and reference `:shared:pitest` Gradle task).

- [ ] **Step 2: Commit and push**

```bash
git add docs/ANTI_BLUFF.md
git commit -m "docs: add ANTI_BLUFF.md runbook for CONST-034 (Kotlin/JVM)"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.3: Add CONST-034 to `CONSTITUTION.md`

**Files:**
- Modify: `CONSTITUTION.md`

- [ ] **Step 1: Append CONST-034 to existing CONSTITUTION.md**

Read the current `CONSTITUTION.md`. After the existing `CONST-033` block (which ends with `<!-- END host-power-management addendum (CONST-033) -->`), insert a blank line then the CONST-034 block (verbatim from `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md` § "CONST-034 — full text").

- [ ] **Step 2: Commit and push**

```bash
git add CONSTITUTION.md
git commit -m "$(cat <<'EOF'
chore(governance): add CONST-034 anti-bluff rule to CONSTITUTION.md

Rule, three-layer user-visible definition (app / KMP / Go submodule),
forbidden-patterns list, defence-in-depth artifacts, hard-block
scope. Verbatim wording matches the same rule already landed in
Challenges/, Containers/, HelixQA/ submodules during sub-project 1
Phase 1.

Refs: docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.4: Add anti-bluff section to `CLAUDE.md` and `AGENTS.md`

**Files:**
- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Append the anti-bluff section to both files**

Same content as Phase 1 Task 1.4 Step 1, with verification commands appropriate for the main repo:

```markdown

## ⚠️ Anti-Bluff Test Discipline — CONST-034

**STRICTLY FORBIDDEN: never write a test that can pass without
exercising the user-visible behavior it claims to verify.** This
includes mocking the unit under test, trivial-assertion-only tests,
permanent skips without an exempt ticket reference, no-op
`runBlocking { }` bodies, and any test whose body would still pass if
every line of the unit under test were replaced with a trivial stub.
See CONST-034 in `CONSTITUTION.md` for the full rule.

**Verification commands** (run before claiming a fix is complete):

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS. Pre-existing bluff hits are tracked in
`challenges/baselines/bluff-baseline.txt`; do not extend the baseline
without an explicit justification comment.
```

- [ ] **Step 2: Commit and push**

```bash
git add CLAUDE.md AGENTS.md
git commit -m "docs(governance): add CONST-034 anti-bluff section to CLAUDE.md and AGENTS.md"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.5: Drop in static scanner (Kotlin-aware)

**Files:**
- Create: `scripts/anti-bluff/bluff-scanner.sh` (same as Phase 1 Task 1.5 Step 2)
- Create: `scripts/anti-bluff/lib/kotlin.sh`
- Create: `scripts/anti-bluff/install-hooks.sh` (same as Phase 1)
- Create: `scripts/anti-bluff/pre-commit-hook.sh` (same as Phase 1)
- Create: `scripts/anti-bluff/tests/fixtures/BluffK002Trivial.kt`
- Create: `scripts/anti-bluff/tests/fixtures/BluffK003Ignore.kt`
- Create: `scripts/anti-bluff/tests/fixtures/Clean.kt`
- Create: `scripts/anti-bluff/tests/run-fixtures.sh`

- [ ] **Step 1: Reuse Phase 1 entry-point and hook scripts**

Copy verbatim:

```bash
mkdir -p scripts/anti-bluff/lib scripts/anti-bluff/tests/fixtures
# Copy from spec or from any Phase 1 submodule's scripts/anti-bluff/.
# bluff-scanner.sh, install-hooks.sh, pre-commit-hook.sh are repo-agnostic.
```

(See Phase 1 Task 1.5 Steps 2, 4, 5 for full content.)

- [ ] **Step 2: Write `scripts/anti-bluff/lib/kotlin.sh`**

```bash
#!/usr/bin/env bash
# Kotlin-flavored bluff patterns. Sourced by bluff-scanner.sh.
# Each pattern emits "<relative path>:<line>:BLUFF-K-NNN:<context>"

scan_kotlin() {
  local relpath="$1" fpath="$2"

  # BLUFF-K-002: trivial assertion patterns on a line.
  awk -v rel="${relpath}" '
    /assertTrue\(true\)/ {
      print rel ":" NR ":BLUFF-K-002:assertTrue(true)"
    }
    /assertEquals\([[:space:]]*([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*,[[:space:]]*\1[[:space:]]*\)/ {
      print rel ":" NR ":BLUFF-K-002:assertEquals(x, x)"
    }
  ' "$fpath"

  # BLUFF-K-003: @Ignore without ANTI-BLUFF-EXEMPT on prev line.
  awk -v rel="${relpath}" '
    /ANTI-BLUFF-EXEMPT/ { exempt[NR+1] = 1 }
    /^[[:space:]]*@Ignore([(].*[)])?[[:space:]]*$/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-003:@Ignore without exempt comment"
    }
  ' "$fpath"

  # BLUFF-K-004: assumeTrue(false).
  awk -v rel="${relpath}" '
    /assumeTrue\(false\)/ {
      print rel ":" NR ":BLUFF-K-004:assumeTrue(false)"
    }
  ' "$fpath"

  # BLUFF-K-006: @Test annotation directly followed by an empty body on the
  # same logical method declaration (single-line empty body case).
  awk -v rel="${relpath}" '
    /^[[:space:]]*@Test[[:space:]]*$/ { test_line = NR; next }
    test_line > 0 && /^[[:space:]]*fun [A-Za-z_][A-Za-z0-9_]*\([^)]*\)([[:space:]]*:[[:space:]]*[A-Za-z_][A-Za-z0-9_]*)?[[:space:]]*\{[[:space:]]*\}[[:space:]]*$/ {
      print rel ":" test_line ":BLUFF-K-006:empty @Test method body"
      test_line = 0; next
    }
    test_line > 0 && /^[[:space:]]*fun / { test_line = 0; next }
    test_line > 0 && /^[[:space:]]*$/ { next }
    { test_line = 0 }
  ' "$fpath"

  # BLUFF-K-008: @Suppress("BLUFF...") without explicit justification on prev line.
  awk -v rel="${relpath}" '
    /ANTI-BLUFF-EXEMPT/ { exempt[NR+1] = 1 }
    /@Suppress\([^)]*"BLUFF[^"]*"[^)]*\)/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-008:@Suppress(\"BLUFF...\") without justification"
    }
  ' "$fpath"
}
```

- [ ] **Step 3: Write fixtures**

`scripts/anti-bluff/tests/fixtures/BluffK002Trivial.kt`:

```kotlin
package fixtures
import kotlin.test.Test
import kotlin.test.assertTrue
class BluffK002Trivial {
    @Test fun trivial() { assertTrue(true) }
}
```

`scripts/anti-bluff/tests/fixtures/BluffK003Ignore.kt`:

```kotlin
package fixtures
import kotlin.test.Test
import org.junit.Ignore
class BluffK003Ignore {
    @Test
    @Ignore
    fun skipped() { }
}
```

`scripts/anti-bluff/tests/fixtures/Clean.kt`:

```kotlin
package fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
class Clean {
    @Test fun adds() {
        val got = 1 + 1
        assertEquals(2, got, "1+1 should be 2")
    }
}
```

- [ ] **Step 4: Write `scripts/anti-bluff/tests/run-fixtures.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../lib/kotlin.sh"

run_fixture() {
  local name="$1" expected_id="$2" file="$3"
  local out
  out="$(scan_kotlin "${name}" "${file}" || true)"
  if [[ -z "$expected_id" ]]; then
    if [[ -n "$out" ]]; then echo "FAIL ${name}: expected clean, got: ${out}"; return 1; fi
  else
    if ! grep -q "${expected_id}" <<< "${out}"; then echo "FAIL ${name}: expected ${expected_id}, got: ${out}"; return 1; fi
  fi
  echo "OK ${name}: ${expected_id:-clean}"
}

failed=0
run_fixture "BluffK002Trivial.kt" "BLUFF-K-002" "${SCRIPT_DIR}/fixtures/BluffK002Trivial.kt" || failed=1
run_fixture "BluffK003Ignore.kt"  "BLUFF-K-003" "${SCRIPT_DIR}/fixtures/BluffK003Ignore.kt"  || failed=1
run_fixture "Clean.kt"            ""             "${SCRIPT_DIR}/fixtures/Clean.kt"            || failed=1

if (( failed )); then echo "Scanner self-test FAILED"; exit 1; fi
echo "Scanner self-test PASSED"
```

```bash
chmod +x scripts/anti-bluff/tests/run-fixtures.sh
```

- [ ] **Step 5: Run self-test, fix until passes**

```bash
bash scripts/anti-bluff/tests/run-fixtures.sh
```

Iterate awk patterns in `lib/kotlin.sh` until all three fixture lines say `OK`.

- [ ] **Step 6: Commit and push**

```bash
git add scripts/anti-bluff
git commit -m "feat(anti-bluff): add Kotlin static scanner and self-tests"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.6: Capture scanner baseline (Section 1, main repo)

Same as Phase 1 Task 1.6, scoped to the Yole tree.

- [ ] **Step 1–5:** identical to Phase 1 Task 1.6 Steps 1–5, run from the main repo working directory. Expect a much larger Section 1 list (Yole has ~9,400 tests with significant pre-existing bluff).

- [ ] **Step 6: Push**

```bash
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.7: Configure Pitest for `shared/` and the 10 KMP modules

**Files:**
- Modify: `shared/build.gradle.kts`
- Modify: each of the 10 sibling `../<Module>-KMP/build.gradle.kts` files (or `composite-build.gradle.kts` includes if Pitest can be applied centrally)
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add Pitest version pin**

Read `gradle/libs.versions.toml`. Add under the `[versions]` section:

```toml
pitest = "1.17.0"
pitest-gradle-plugin = "1.15.0"
```

Add under `[plugins]`:

```toml
pitest = { id = "info.solidsoft.pitest", version.ref = "pitest-gradle-plugin" }
```

- [ ] **Step 2: Apply Pitest to `shared/build.gradle.kts`**

Read `shared/build.gradle.kts`. Add under the `plugins { }` block:

```kotlin
alias(libs.plugins.pitest)
```

After the `kotlin { ... }` block, add:

```kotlin
configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    pitestVersion = libs.versions.pitest.get()
    junit5PluginVersion = "1.2.1"
    targetClasses.set(listOf("digital.vasic.yole.*"))
    threads.set(2)
    timeoutFactor.set(1.5.toBigDecimal())
    timeoutConstInMillis.set(5000)
    outputFormats.set(setOf("XML", "HTML"))
    failWhenNoMutations.set(false)
    avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlinx.coroutines"))
}
```

This applies Pitest only to `:shared:jvm` (the JVM target). KMP Native and WASM targets are explicitly out of scope per spec § Risks.

- [ ] **Step 3: Apply Pitest to each KMP module**

For each of the 10 sibling KMP modules (`../RateLimiter-KMP`, `../Concurrency-KMP`, `../UI-Components-KMP`, `../Auth-KMP`, `../Security-KMP`, `../Document-KMP`, `../Config-KMP`, `../Database-KMP`, `../Storage-KMP`, `../Formatters-KMP`), open the module's root `build.gradle.kts` and add the same `pitest` plugin alias and `configure<...>` block, with `targetClasses.set(listOf("digital.vasic.<modulename>.*"))` substituted appropriately.

If a module's repo doesn't share `gradle/libs.versions.toml` (composite builds may have their own), inline the version literally: `id("info.solidsoft.pitest") version "1.15.0"`.

- [ ] **Step 4: Smoke test Pitest on `:shared:jvm`**

```bash
./gradlew :shared:pitest --no-daemon
```

Expected: Pitest runs, prints mutation report. **Slow: 30–60 minutes.** Run in the background; the next steps in the plan (writing challenge wrappers) can proceed in parallel.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts ../*/build.gradle.kts
git commit -m "$(cat <<'EOF'
feat(anti-bluff): wire Pitest mutation testing for shared and KMP modules

Pitest 1.17.0 via pitest-gradle-plugin 1.15.0. Applied to :shared:jvm
and each of the 10 sibling KMP modules. Targets digital.vasic.<module>.*
classes; avoids stdlib/coroutines internals.

Native and WASM targets are out of scope (Pitest is JVM-only). Their
coverage is enforced by the anchor manifest, see CONST-034 § risks.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 6: Push**

```bash
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.8: Run full mutation pass and capture baseline (Section 2, Kotlin)

This is the slowest step in Phase 2. Plan ~30–60 minutes per module × 11 modules. Run sequentially in a long-running shell, or distribute across machines.

- [ ] **Step 1: Run Pitest across all modules**

```bash
./gradlew :shared:pitest pitest --no-daemon | tee /tmp/pitest-output.txt
```

The `pitest` task without a project prefix runs Pitest on every module that applies the plugin.

- [ ] **Step 2: Parse Pitest reports into baseline format**

Pitest writes an XML report per module at `<module>/build/reports/pitest/<run>/mutations.xml`. Parse with:

```bash
python3 - <<'PYEOF' > /tmp/pitest-section2.txt
import os, glob
import xml.etree.ElementTree as ET

reports = glob.glob("**/build/reports/pitest/*/mutations.xml", recursive=True)
totals = {}; killed = {}
for rpt in reports:
    tree = ET.parse(rpt); root = tree.getroot()
    for m in root.findall("mutation"):
        src = m.findtext("sourceFile") or ""
        path = m.findtext("mutatedClass") or ""
        # Use sourceFile + class to construct an approximate file path.
        key = path.replace(".", "/") + ".kt"  # heuristic
        totals[key] = totals.get(key, 0) + 1
        if m.get("status") in ("KILLED", "TIMED_OUT"):
            killed[key] = killed.get(key, 0) + 1

for key in sorted(totals):
    rate = round(100 * killed.get(key, 0) / totals[key])
    print(f"{key}:{rate}:{totals[key]}")
PYEOF
```

(The `key = path.replace(".", "/") + ".kt"` heuristic is approximate. The implementing agent should refine it by consulting actual report XML and matching to real source paths via `git ls-files`. If Pitest provides `sourceFile` attribute, prefer that.)

- [ ] **Step 3: Splice into baseline file**

Same approach as Phase 1 Task 1.8 Step 3.

- [ ] **Step 4: Verify baseline parses correctly**

```bash
bash challenges/scripts/mutation_ratchet_challenge.sh --mode changed
```

Expected: `OK: no Kotlin changes vs master.` (because the challenge for Kotlin should be configured to limit to `*.kt` changes only at this point — adapt the Phase 1 Go challenge to a Kotlin variant).

- [ ] **Step 5: Commit and push**

```bash
git add challenges/baselines/bluff-baseline.txt
git commit -m "chore(anti-bluff): capture Kotlin mutation kill rate baseline (Section 2)"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.9: Add `mutation_ratchet_challenge.sh` (Kotlin variant)

The Phase 1 challenge script targets `go-mutesting`. The main repo needs a Kotlin/Pitest variant.

**Files:**
- Create: `challenges/scripts/mutation_ratchet_challenge.sh`

- [ ] **Step 1: Write the Kotlin variant**

```bash
#!/usr/bin/env bash
# CONST-034 mutation ratchet challenge (Kotlin/Pitest).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MODE="changed"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 3 ;;
  esac
done

BASELINE="${ROOT_DIR}/challenges/baselines/bluff-baseline.txt"

if [[ "$MODE" == "changed" ]]; then
  mapfile -t CHANGED < <(git -C "${ROOT_DIR}" diff --name-only master...HEAD -- '*.kt' '*.kts')
  if (( ${#CHANGED[@]} == 0 )); then
    echo "OK: no Kotlin changes vs master."
    exit 0
  fi
fi

# Run Pitest scoped to changed (--targetClasses arg) or all classes.
if [[ "$MODE" == "all" ]]; then
  cd "${ROOT_DIR}" && ./gradlew :shared:pitest pitest --no-daemon
else
  # Translate changed file paths to FQ classnames for Pitest --targetClasses.
  TARGETS=$(for f in "${CHANGED[@]}"; do
    pkg=$(awk '/^package / { print $2; exit }' "${ROOT_DIR}/${f}")
    cls=$(basename "$f" .kt)
    [[ -n "$pkg" ]] && echo "${pkg}.${cls}"
  done | sort -u | tr '\n' ',' | sed 's/,$//')
  cd "${ROOT_DIR}" && ./gradlew :shared:pitest -Ppitest.targetClasses="${TARGETS}" --no-daemon
fi

# Parse XML reports, compare to baseline. (Identical Python logic to Phase 1 Task 1.8 Step 2.)
python3 - "${BASELINE}" "${MODE}" <<'PYEOF'
import sys, glob, os, collections
import xml.etree.ElementTree as ET
baseline_path, mode = sys.argv[1], sys.argv[2]

reports = glob.glob("**/build/reports/pitest/*/mutations.xml", recursive=True)
totals = collections.Counter(); killed = collections.Counter()
for rpt in reports:
    tree = ET.parse(rpt)
    for m in tree.getroot().findall("mutation"):
        path = m.findtext("mutatedClass") or ""
        key = path.replace(".", "/") + ".kt"
        totals[key] += 1
        if m.get("status") in ("KILLED", "TIMED_OUT"):
            killed[key] += 1

baseline = {}
section = None
with open(baseline_path) as f:
    for line in f:
        line = line.rstrip("\n")
        if line.startswith("# === SECTION 2"): section = 2; continue
        if line.startswith("# === SECTION 3"): section = 3; continue
        if section == 2 and line and not line.startswith("#"):
            try:
                p, r, n = line.split(":")
                baseline[p] = int(r)
            except ValueError: continue

failed = False
for fn in sorted(totals):
    rate = 100 * killed[fn] // totals[fn] if totals[fn] else 0
    if mode == "changed" and rate < 90:
        print(f"FAIL: {fn} kill rate {rate}% < 90%"); failed = True
    if fn in baseline and rate < baseline[fn]:
        print(f"FAIL: {fn} kill rate {rate}% < baseline {baseline[fn]}%"); failed = True

if mode == "all":
    overall = 100 * sum(killed.values()) // max(1, sum(totals.values()))
    if overall < 80:
        print(f"FAIL: project-wide kill rate {overall}% < 80%"); failed = True

sys.exit(1 if failed else 0)
PYEOF

echo "OK: mutation ratchet (mode=${MODE})."
```

```bash
chmod +x challenges/scripts/mutation_ratchet_challenge.sh
```

- [ ] **Step 2: Commit and push**

```bash
git add challenges/scripts/mutation_ratchet_challenge.sh
git commit -m "feat(anti-bluff): add Kotlin/Pitest mutation ratchet challenge"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.10: Add anchor manifest skeleton + scanner challenge + manifest challenge

**Files:**
- Create: `docs/behavior-anchors.md` (same skeleton format as Phase 1 Task 1.9)
- Create: `challenges/scripts/bluff_scanner_challenge.sh` (same as Phase 1 Task 1.10 Step 1)
- Create: `challenges/scripts/anchor_manifest_challenge.sh` (same as Phase 1 Task 1.10 Step 2; Kotlin grep also looks for `fun ${first_sym}`)

- [ ] **Step 1–3:** Write all three files using Phase 1 templates.

- [ ] **Step 4: Update `Makefile` to include anti-bluff in qa-all**

The main repo's `Makefile` already has `qa-all` defined. Add `anti-bluff` to its dependency list:

```makefile
# Find existing line:
# qa-all: test-shared challenge helixqa-test
# Replace with:
qa-all: test-shared challenge helixqa-test anti-bluff
```

Plus the new `anti-bluff*` targets and `update-baseline` (Phase 1 Task 1.10 Step 4).

- [ ] **Step 5: Wire `runChallenges` Gradle task**

Read `build.gradle.kts` (root). Find the `runChallenges` task definition. Add the three new challenge scripts to its `commandLine` list or its `dependsOn` chain (depending on how it's currently structured — may be a series of `Exec` tasks).

- [ ] **Step 6: Install pre-commit hook**

```bash
bash scripts/anti-bluff/install-hooks.sh
```

- [ ] **Step 7: Smoke-test all anti-bluff targets**

```bash
make anti-bluff-scan
make anti-bluff-anchors
make anti-bluff-mutation-changed
```

Expected: each prints `OK:` and exits 0.

- [ ] **Step 8: Commit and push**

```bash
git add docs/behavior-anchors.md challenges/scripts Makefile build.gradle.kts
git commit -m "feat(anti-bluff): wire challenges + manifest into main repo"
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.11: End-to-end verification

Same shape as Phase 1 Task 1.11.

- [ ] **Step 1: Run `make qa-all`**

```bash
make qa-all
```

Expected: pass. The first run includes a full mutation pass (slow). Set `MUTATION_FULL=1` env var if applicable to your config.

- [ ] **Step 2: Deliberate-bluff trip test**

```bash
cat > /tmp/BluffTripwireTest.kt <<'KTEOF'
package digital.vasic.yole.tripwire
import kotlin.test.Test
import kotlin.test.assertTrue
class BluffTripwireTest {
    @Test fun trip() { assertTrue(true) }
}
KTEOF
mkdir -p shared/src/commonTest/kotlin/digital/vasic/yole/tripwire
cp /tmp/BluffTripwireTest.kt shared/src/commonTest/kotlin/digital/vasic/yole/tripwire/
git add shared/src/commonTest/kotlin/digital/vasic/yole/tripwire
bash scripts/anti-bluff/bluff-scanner.sh --mode all
echo "scanner exit code: $?"
```

Expected: scanner reports `BLUFF-K-002:assertTrue(true)`, exits 1.

- [ ] **Step 3: Remove tripwire**

```bash
git rm -r shared/src/commonTest/kotlin/digital/vasic/yole/tripwire/
rm -f /tmp/BluffTripwireTest.kt
```

- [ ] **Step 4: Final verification**

```bash
make qa-all
```

Expected: pass.

### Task 2.12: Advance submodule pointers

**Files:**
- Modify: submodule pointer entries (effectively `Challenges`, `Containers`, `HelixQA` in main repo's worktree)

- [ ] **Step 1: Update each submodule pointer to its post-Phase-1 master SHA**

```bash
for sub in Challenges Containers HelixQA; do
  cd "/run/media/milosvasic/DATA4TB/Projects/Yole/${sub}"
  git checkout master
  git pull --ff-only
done
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add Challenges Containers HelixQA
git status -s
```

Expected: `git status -s` shows `M Challenges`, `M Containers`, `M HelixQA` (modified submodule pointers).

- [ ] **Step 2: Commit**

```bash
git commit -m "$(cat <<'EOF'
chore: advance submodule pointers to post-anti-bluff masters

Submodule masters now contain CONST-034 + scanner + mutation gate +
manifest skeleton (sub-project 1 Phase 1).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Push**

```bash
for r in github origin upstream; do git push "$r" campaign/anti-bluff; done
```

### Task 2.13: Merge to master and push

- [ ] **Step 1: Switch to master**

```bash
git checkout master
git fetch --all --prune
git pull --ff-only
```

- [ ] **Step 2: Merge campaign branch (no fast-forward)**

```bash
git merge --no-ff campaign/anti-bluff -m "$(cat <<'EOF'
Merge branch 'campaign/anti-bluff' — sub-project 1 (anti-bluff foundation, main)

Lands CONST-034 in main Yole repo with Kotlin/JVM enforcement:
- CONSTITUTION.md: CONST-034 added.
- CLAUDE.md, AGENTS.md: anti-bluff section.
- scripts/anti-bluff/: scanner + Kotlin pattern lib + hook installer + fixtures.
- gradle/libs.versions.toml: Pitest pin.
- shared/build.gradle.kts and KMP modules: Pitest config.
- challenges/scripts/{bluff_scanner,anchor_manifest,mutation_ratchet}_challenge.sh.
- challenges/baselines/bluff-baseline.txt: full baseline (S1 scanner, S2 mutation rates).
- docs/behavior-anchors.md: manifest skeleton.
- docs/ANTI_BLUFF.md: runbook.
- Makefile + runChallenges: anti-bluff targets wired into qa-all.
- Submodule pointers advanced to post-anti-bluff masters.

Refs: docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Final qa-all on master**

```bash
make qa-all
```

Expected: pass.

- [ ] **Step 4: Push master to all main remotes**

```bash
for r in github origin upstream; do git push "$r" master; done
```

- [ ] **Step 5: Delete campaign branch**

```bash
git branch -d campaign/anti-bluff
for r in github origin upstream; do git push "$r" --delete campaign/anti-bluff || true; done
```

### Task 2.14: Update tracker — sub-project 1 done

**Files:**
- Modify: `docs/campaigns/anti-bluff/CAMPAIGN.md`

- [ ] **Step 1: Mark sub-project 1 done**

Edit the tracker:

```markdown
## Sub-projects

1. [done] Anti-Bluff Foundation — completed 2026-05-XX
2. [pending] Resumable Campaign Tracker formalisation
...

### Per-repo state

| Repo | Branch | Last checkpoint | Next checkpoint | Last push |
|------|--------|------------------|-------------------|-----------|
| Challenges | master | 1.12 (merged) | n/a (sub-project 1 done) | <SHA> |
| Containers | master | 1.12 (merged) | n/a (sub-project 1 done) | <SHA> |
| HelixQA    | master | 1.12 (merged) | n/a (sub-project 1 done) | <SHA> |
| Yole       | master | 2.13 (merged)   | n/a (sub-project 1 done) | <SHA> |
```

- [ ] **Step 2: Commit and push**

```bash
git add docs/campaigns/anti-bluff/CAMPAIGN.md
git commit -m "docs(campaign): mark sub-project 1 (anti-bluff foundation) done"
for r in github origin upstream; do git push "$r" master; done
```

---

## Acceptance test (run after all phases complete)

In each of the four repos, run:

```bash
# Verify governance docs reference CONST-034.
grep -l "CONST-034" CONSTITUTION.md CLAUDE.md AGENTS.md
# All three filenames must appear in output.

# Verify all required artifacts exist.
test -x scripts/anti-bluff/bluff-scanner.sh
test -f challenges/baselines/bluff-baseline.txt
test -f docs/behavior-anchors.md
test -f docs/ANTI_BLUFF.md

# Verify gates pass.
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh

# Verify no unpushed commits.
test "$(git log '@{u}..HEAD' --oneline | wc -l)" -eq 0

# Verify last commit references CONST-034 / anti-bluff campaign.
git log master -1 --format=%s | grep -E 'anti-bluff|CONST-034'
```

In main repo only:

```bash
test -f docs/campaigns/anti-bluff/CAMPAIGN.md
grep -q '\[done\] Anti-Bluff Foundation' docs/campaigns/anti-bluff/CAMPAIGN.md
```

When all of these pass in all four repos, sub-project 1 is complete and sub-project 2 (Resumable Campaign Tracker formalisation) can begin via a fresh brainstorm cycle.

---

## Self-review notes (from plan author)

- **Spec coverage:** every section of the spec is mapped to one or more tasks. CONST-034 wording → 1.3 / 2.3. Three-layer definition → embedded in CONST-034 itself, not a separate task. Scanner design → 1.5 / 2.5. Mutation gate → 1.7 / 2.7. Baseline → 1.6+1.8 / 2.6+2.8. Anchor manifest → 1.9 / 2.10. Wiring → 1.10 / 2.10. Sequencing & push points → enforced via per-task pushes. Master tracker stub → 0.2 / 2.1 / 2.14. Acceptance criteria → above.

- **Known plan limits:**
  - The awk-based scanner patterns only cover the easy half of the BLUFF-N-NNN list. BLUFF-K-001 (mock-self), BLUFF-K-005 (runBlocking no-op), BLUFF-K-007 (SUT-via-mock-only), BLUFF-G-002 (testing.Short short-circuit), BLUFF-G-004 (gomock same-package SUT) require AST awareness and are deferred to a follow-up sub-project. Plan documents this in commit messages.
  - The Pitest XML → file-path heuristic in Task 2.8 is approximate. Implementing agent should refine on first real run.
  - The go-mutesting output regex in Task 1.8 must be confirmed against the installed version; format changes have happened across versions.

- **Type / signature consistency check:**
  - `scan_kotlin` and `scan_go` both take `(relpath, fpath)` — consistent.
  - Baseline file format declared once (Section 1: `path:BLUFF-ID`, Section 2: `path:rate:total`, Section 3: `id:MISSING_ANCHOR`) and referenced consistently throughout.
  - Anchor test path format declared once (`<path>::<symbol>` Go, `<path>::<class>::<method>` Kotlin) — consistent.

- **Push triggers:** every task that creates or modifies tracked files ends with a push step. Per Q7 (campaign branches push at checkpoint boundaries).

- **No placeholders:** no "TBD" / "implement later" / "similar to Task N" entries. Every script body and doc body is shown in full.
