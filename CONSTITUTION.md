# Yole — Constitution

> **Status:** Active. This document is the project's authoritative
> rule set. When a rule here conflicts with `CLAUDE.md`, `AGENTS.md`,
> or any guide, the Constitution wins.

## Mission

See README.md.

## Mandatory Standards

1. **Reproducibility:** every change is reproducible from a clean
   clone (`git clone <repo> && <project bootstrap>`); no hidden steps.
2. **Tests track behavior, not code:** test what the user-visible
   behavior is, not what the implementation looks like.
3. **No silent skips, no silent mocks above unit tests.**
4. **Conventional Commits** for all commits.
5. **SSH-only for git operations** (`git@…`); HTTPS prohibited.

## Numbered Rules

<!-- Rules are numbered CONST-NNN. New rules append. Removed rules
     keep their number with a "**Retired:** …" line. -->

<!-- BEGIN host-power-management addendum (CONST-033) -->

### CONST-033 — Host Power Management is Forbidden

**Status:** Mandatory. Non-negotiable. Applies to every project,
submodule, container entry point, build script, test, challenge, and
systemd unit shipped from this repository.

**Rule:** No code in this repository may invoke a host-level power-
state transition (suspend, hibernate, hybrid-sleep, suspend-then-
hibernate, poweroff, halt, reboot, kexec) on the host machine. This
includes — but is not limited to:

- `systemctl {suspend,hibernate,hybrid-sleep,suspend-then-hibernate,poweroff,halt,reboot,kexec}`
- `loginctl {suspend,hibernate,hybrid-sleep,suspend-then-hibernate,poweroff,halt,reboot}`
- `pm-{suspend,hibernate,suspend-hybrid}`
- `shutdown {-h,-r,-P,-H,now,--halt,--poweroff,--reboot}`
- DBus calls to `org.freedesktop.login1.Manager.{Suspend,Hibernate,HybridSleep,SuspendThenHibernate,PowerOff,Reboot}`
- DBus calls to `org.freedesktop.UPower.{Suspend,Hibernate,HybridSleep}`
- `gsettings set ... sleep-inactive-{ac,battery}-type` to any value other than `'nothing'` or `'blank'`

**Why:** The host runs mission-critical parallel CLI-agent and
container workloads. On 2026-04-26 18:23:43 the host was auto-
suspended by the GDM greeter's idle policy mid-session, killing
HelixAgent and 41 dependent services. Recurring memory-pressure
SIGKILLs of `user@1000.service` (perceived as "logged out") have the
same outcome. Auto-suspend, hibernate, and any power-state transition
are unsafe for this host.

**Defence in depth (mandatory artifacts in every project):**
1. `scripts/host-power-management/install-host-suspend-guard.sh` —
   privileged installer, manual prereq, run once per host with sudo.
   Masks `sleep.target`, `suspend.target`, `hibernate.target`,
   `hybrid-sleep.target`; writes `AllowSuspend=no` drop-in; sets
   logind `IdleAction=ignore` and `HandleLidSwitch=ignore`.
2. `scripts/host-power-management/user_session_no_suspend_bootstrap.sh` —
   per-user, no-sudo defensive layer. Idempotent. Safe to source from
   `start.sh` / `setup.sh` / `bootstrap.sh`.
3. `scripts/host-power-management/check-no-suspend-calls.sh` —
   static scanner. Exits non-zero on any forbidden invocation.
4. `challenges/scripts/host_no_auto_suspend_challenge.sh` — asserts
   the running host's state matches layer-1 masking.
5. `challenges/scripts/no_suspend_calls_challenge.sh` — wraps the
   scanner as a challenge that runs in CI / `run_all_challenges.sh`.

**Enforcement:** Every project's CI / `run_all_challenges.sh`
equivalent MUST run both challenges (host state + source tree). A
violation in either channel blocks merge. Adding files to the
scanner's `EXCLUDE_PATHS` requires an explicit justification comment
identifying the non-host context.

**See also:** `docs/HOST_POWER_MANAGEMENT.md` for full background and
runbook.

<!-- END host-power-management addendum (CONST-033) -->

<!-- BEGIN anti-bluff addendum (CONST-035) -->

### CONST-035 — Anti-Bluff Tests & Challenges

**Status:** Mandatory. Non-negotiable. Applies to every test, challenge,
script, and verification artifact in this repository and its submodules.

Tests and Challenges in this project MUST verify the product, not the
LLM's mental model of the product. A test that passes when the feature
is broken is worse than a missing test — it gives false confidence and
lets defects ship to users. Functional probes at the protocol layer are
mandatory:

- TCP-open is the FLOOR, not the ceiling. Postgres → execute `SELECT 1`.
  Redis → `PING` returns `PONG`. ChromaDB → `GET /api/v1/heartbeat`
  returns 200. MCP server → TCP connect + valid JSON-RPC handshake.
  HTTP gateway → real request, real response, non-empty body.
- Container `Up` is NOT application healthy. A `docker/podman ps` `Up`
  status only means PID 1 is running; the application may be
  crash-looping internally.
- No mocks/fakes outside unit tests. CONST-035 raises the cost of a
  mock-driven false pass to the same severity as a regression.
- Re-verify after every change. Don't assume a previously-passing
  test still verifies the same scope after a refactor.
- Verification of CONST-035 itself: deliberately break the feature
  (e.g. mutate a parser, swap a config). The test MUST fail. If it
  still passes, the test is non-conformant and MUST be tightened.

#### Three-layer "user-visible behaviour" definition

This project is mixed: a Kotlin Multiplatform app + 10 KMP library modules + 3 Go testing-framework submodules. CONST-035 has a per-context definition of "user-visible":

- **App layer** (Android, Desktop, iOS, Web): user-visible means a real
  human user can observe the result. Anchor signal: UI screenshot diff,
  file written to disk and re-read, network request observed on a real
  socket, intent/activity dispatched and asserted.
- **KMP module layer** (RateLimiter, Concurrency, UI-Components, Auth,
  Security, Document, Config, Database, Storage, Formatters):
  user-visible means a developer consuming the public API can observe
  the documented contract. Anchor signal: integration test calling only
  the public API, using real coroutines / real clock / real I/O where
  applicable, asserting the contract in the API's KDoc.
- **Go submodule layer** (Challenges, Containers, HelixQA): user-visible
  means an operator invoking the CLI/binary can observe the result.
  Anchor signal: CLI invocation against a real target producing an
  observable artifact (file on disk, exit code, log line), asserted by
  the test.

#### Bluff taxonomy (each pattern observed and now forbidden)

- **Wrapper bluff** — assertions PASS but the wrapper's exit-code logic
  is buggy, marking the run FAILED (or the inverse: assertions FAIL but
  the wrapper swallows them). Every aggregating wrapper MUST use a
  robust counter (`! grep -qs "|FAILED|" "$LOG"` style).
- **Contract bluff** — the system advertises a capability but rejects
  it in dispatch. Every advertised capability MUST be exercised by a
  test or Challenge that actually invokes it.
- **Structural bluff** — `check_file_exists "foo_test.kt"` passes if
  the file is present but doesn't run the test or assert anything
  about its content. File-existence checks MUST be paired with at
  least one functional assertion.
- **Comment bluff** — a code comment promises a behavior the code
  doesn't actually have. Documentation MUST be re-verified against the
  code on every change touching the documented function.
- **Skip bluff** — `t.Skip("not running yet")` / `@Ignore` without a
  `SKIP-OK: #<ticket>` marker silently passes. Every skip needs the
  marker; gates fail on bare skips.

#### Defence in depth (mandatory artifacts)

1. `scripts/anti-bluff/bluff-scanner.sh` — static scanner. Exits non-zero
   on any forbidden pattern outside the baseline.
2. `challenges/baselines/bluff-baseline.txt` — captured pre-existing
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

**Enforcement.** All three challenges MUST run in `runChallenges` /
`make qa-all`. A violation in any channel blocks merge. Adding files
to scanner `EXCLUDE_PATHS` or expanding the baseline requires an
explicit justification comment naming the non-bluff context.

**Hard block scope.** New code (any file modified in the working tree)
must produce zero net new scanner hits AND must not lower the project
mutation kill rate. Pre-existing bluff hits are recorded in the
baseline; baseline reduction is the work of campaign sub-projects 4–5.

**Skip-marker convention.** `// SKIP-OK: #<ticket>` (Go and Kotlin) is
the canonical exempt marker. `// ANTI-BLUFF-EXEMPT: <reason>` is
accepted as a synonym for forward compat.

**Why.** The project has a documented history of feature regressions
that test suites failed to catch — tests passed while features were
unusable. CONST-035 makes that class of failure detectable and blocks
its recurrence. See the verbatim user-mandate forensic anchor in
`CLAUDE.md` and `AGENTS.md`.

**See also:** `docs/ANTI_BLUFF.md` (background and runbook),
`docs/behavior-anchors.md` (manifest), `docs/campaigns/anti-bluff/CAMPAIGN.md`
(active campaign tracker), and the cascaded copies of CONST-035 in each
submodule's `CONSTITUTION.md`.

<!-- END anti-bluff addendum (CONST-035) -->

<!-- BEGIN continuation-document addendum (CONST-036) -->

### CONST-036 — Continuation Document MUST Be Maintained

**Status:** Mandatory. Non-negotiable. Applies to every work session in this
repository and its submodules.

**Rule:** `docs/CONTINUATION.md` is a living document that tracks ALL
unfinished work, active tasks, known defects, implementation phases, and
current repo state. During ANY work — Phases implementation, debugging,
fixing, refactoring, testing, documentation — the Continuation document
MUST be maintained and MUST NOT be out of sync with current work.

If work stops for any reason (session loss, context overflow, agent
switch, model change, human interruption), the next CLI agent or LLM
model MUST be able to continue exactly where work left off from the
Continuation document alone.

**Mandatory update points:**
1. After completing ANY task or subtask — update task status immediately.
2. When creating new files (untracked) — add to Section 3 (Uncommitted Files).
3. When committing — update Section 6 (Repo State) with new commit SHAs.
4. When discovering a new bug or defect — add to Section 4 (Known Defects).
5. When starting a new feature stream or phase — add to Section 7.
6. When the "How to Resume" prompt becomes stale — refresh Section 1.
7. Before any `git commit` — verify Continuation document reflects reality.

**Enforcement:** Before claiming work is done or before any commit, the
agent MUST verify `docs/CONTINUATION.md` accurately reflects:
- All active tasks and their completion status
- All uncommitted files in the working tree
- All known defects
- Current branch and commit state

A stale or inaccurate Continuation document is a CONST-036 violation and
MUST be corrected before proceeding.

**Why.** Session loss and agent/model switches are normal operational
reality for AI-assisted development. Without a maintained Continuation
document, work context is lost and must be reconstructed from scratch,
wasting time and risking incomplete or duplicated work.

**See also:** `docs/CONTINUATION.md` — the living document itself.

<!-- END continuation-document addendum (CONST-036) -->

## Definition of Done

A change is done when:

1. The code change is committed.
2. All project-level tests pass on a clean clone.
3. All challenges in `challenges/scripts/` pass on the running host.
4. Governance docs (`CONSTITUTION.md`, `AGENTS.md`, `CLAUDE.md`) are
   coherent with the change.
5. `docs/CONTINUATION.md` is updated to reflect current state per CONST-036.

## See also

- `README.md` — project overview, quickstart.
- `AGENTS.md` — guidance for AI coding agents (Codex, Cursor, etc.).
- `CLAUDE.md` — guidance specifically for Claude Code.
- `docs/HOST_POWER_MANAGEMENT.md` — CONST-033 background and runbook.
