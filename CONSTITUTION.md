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
4. `yole-challenges/scripts/host_no_auto_suspend_challenge.sh` — asserts
   the running host's state matches layer-1 masking.
5. `yole-challenges/scripts/no_suspend_calls_challenge.sh` — wraps the
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
2. `yole-challenges/baselines/bluff-baseline.txt` — captured pre-existing
   bluff hits and per-file mutation kill rates. Ratchet enforces the
   baseline never worsens.
3. `docs/behavior-anchors.md` — anchor manifest. Every user-facing
   capability has at least one row pointing at one anchor test that
   proves the capability end-to-end.
4. `yole-challenges/scripts/bluff_scanner_challenge.sh` — wraps the scanner
   as a challenge.
5. `yole-challenges/scripts/mutation_ratchet_challenge.sh` — runs Pitest
   (Kotlin/JVM) or `go-mutesting` (Go), enforces 90% kill on changed
   code and 80% project-wide ratchet.
6. `yole-challenges/scripts/anchor_manifest_challenge.sh` — verifies every
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

<!-- BEGIN cross-platform-impact addendum (CONST-037) -->

### CONST-037 — Cross-Platform Impact MUST Be Reasoned About

**Status:** Mandatory. Non-negotiable. Applies to every change in this
repository and its submodules.

**Rule:** Yole ships to four user-visible platforms — Android, Desktop
(Linux x64, Windows x64, macOS arm64), iOS, and Web (Wasm PWA). Every
change that touches shared code, UI, a screen, a navigation entry, a
data model, or a public API MUST explicitly answer all four questions
below BEFORE the change is considered complete:

1. **Does this compile on every target?** KMP common code,
   `expect/actual` surfaces, and platform-specific code paths each have
   their own compilation matrix. "Builds on my machine" is not a
   platform-coverage answer.
2. **Does this behave identically — or by-design differently — on
   every target?** A bottom navigation bar that makes sense on Android
   may be wrong on Desktop. A file picker that uses Android SAF needs
   an `expect/actual` counterpart on Desktop / iOS / Web.
3. **Is the change covered by a test on every affected target?** A fix
   that lands only in `androidMain` but not in `desktopMain` /
   `iosMain` / `wasmJsMain` is incomplete by default.
4. **Are platform-specific resources (AndroidManifest.xml, Info.plist,
   web `manifest.json`, container packaging) updated coherently?**

**How to apply:**
- Before editing shared code, list which `*Main` / `*Test` source sets
  the change touches. If only one is touched but more are affected,
  the change is incomplete.
- Every PR description / commit body for a multi-platform change MUST
  contain a "Cross-platform impact" block enumerating each platform
  and its disposition (changed / unchanged / N/A with reason).
- Per-platform divergence is allowed when justified, but MUST be
  documented in the commit body and (for permanent divergences) in
  `docs/ARCHITECTURE.md`.

**Why:** End users experience Yole on whichever platform they install.
A regression that ships only to iOS still ships. We have shipped fixes
that worked on the host platform but silently broke others; mandatory
up-front consideration is the only mitigation.

**Enforcement:** `yole-challenges/scripts/cross_platform_parity_challenge.sh`
runs in `make qa-all` and scans for divergent surfaces (e.g. a screen
present in one platform's nav but absent in another's without a
documented reason). Detekt rules and per-target test suites enforce
compile-time parity.

**See also:** `CLAUDE.md` and `AGENTS.md` "Cross-Platform Impact"
sections; `docs/ARCHITECTURE.md` for the per-platform module map.

<!-- END cross-platform-impact addendum (CONST-037) -->

<!-- BEGIN submodule-decoupling addendum (CONST-038) -->

### CONST-038 — Submodules Must Remain Fully Decoupled and Reusable

**Status:** Mandatory. Non-negotiable. Applies to every submodule
consumed by this repository AND, recursively, to every submodule
consumed by those submodules.

**Rule:** Every submodule referenced from this repository's
`.gitmodules` (and every submodule referenced from any of those
submodules' `.gitmodules`, transitively) is **shared infrastructure**
consumed by multiple independent consumer projects. Each submodule
exists for one specialized responsibility (e.g. Challenges runs
challenge suites; Containers manages container runtimes; HelixQA runs
autonomous QA; LLMProvider abstracts model providers; Security holds
shared security primitives). The value of these submodules is their
**reusability across consumers** — and that value is destroyed the
moment a submodule starts assuming it is only consumed by Yole, or
only by any one consumer project.

**Prohibited inside a submodule:**

1. Hardcoded consumer-project names, paths, platform lists, version
   strings, release-naming conventions, or feature names.
2. `import` / dependency on a consumer-project namespace, package, or
   build coordinate.
3. Embedding consumer-project-specific governance, branding, copy,
   or rule numbering.
4. Assuming the submodule is consumed by a particular CLI, build
   system, language toolchain version, or target architecture beyond
   what its public interface explicitly documents.

**Required inside a submodule:**

1. All public surfaces (APIs, CLIs, configuration files, environment
   variables, scripts) MUST be expressed in terms of the submodule's
   own domain — not any consumer's.
2. Submodule governance (`CONSTITUTION.md` / `CLAUDE.md` / `AGENTS.md`)
   MUST describe responsibilities and contract from the **submodule's**
   perspective. Consumer projects appear as illustrative examples at
   most, never as load-bearing requirements.
3. Cross-project rules adopted by submodules (such as the
   cross-platform impact mandate in CONST-037) MUST be phrased
   generically — "every consuming project's full platform matrix" —
   and never hardcode any single consumer's matrix.
4. Each submodule's specialized responsibility is documented in the
   submodule's own README and Constitution; the parent project's
   knowledge of that responsibility is **derived**, not authored, from
   the submodule.

**Why:** This project has shipped changes in the past where one
consumer's platform list, feature names, or rule numbering leaked
into shared-submodule governance — and then collided at merge time
with another consumer's parallel work, leaving the submodule
unmergeable until manual conflict resolution stripped the
consumer-specific text back out. The cost of preventing the leak at
authoring time is far lower than the cost of unwinding it after the
fact. **Decoupling is the only mechanism that preserves a submodule's
value as shared infrastructure.**

**Enforcement:**

1. Any submodule change that introduces consumer-coupling MUST be
   reverted on its remote.
2. Reviewers MUST treat consumer-project-specific text in a submodule
   as a regression of the same severity as breaking the submodule's
   public API.
3. When a submodule's governance must reflect a cross-project rule,
   the parent project carries the consumer-specific specifics; the
   submodule carries only the project-agnostic abstraction.

**Recursive scope:** this rule MUST be mirrored in every owned
submodule's own governance (`CONSTITUTION.md` / `CLAUDE.md` /
`AGENTS.md`). Submodules that are upstream third-party projects
(e.g. open-source tools vendored under `tools/opensource/`) are
explicitly out of scope — we are not their owners and have no right
to amend their governance.

**See also:** [[const-037]] cross-platform impact — the canonical
example of a rule that MUST be expressed generically inside a
submodule.

<!-- END submodule-decoupling addendum (CONST-038) -->

## Definition of Done

A change is done when:

1. The code change is committed.
2. All project-level tests pass on a clean clone.
3. All challenges in `yole-challenges/scripts/` pass on the running host.
4. Governance docs (`CONSTITUTION.md`, `AGENTS.md`, `CLAUDE.md`) are
   coherent with the change.
5. `docs/CONTINUATION.md` is updated to reflect current state per CONST-036.
6. The change has been reasoned about across all four user-visible
   platforms per CONST-037, and any per-platform divergence is
   documented in the commit body.
7. Any submodule touched by the change preserves its decoupling +
   reusability per CONST-038. Consumer-project specifics never leak
   into shared-submodule code or governance.

## See also

- `README.md` — project overview, quickstart.
- `AGENTS.md` — guidance for AI coding agents (Codex, Cursor, etc.).
- `CLAUDE.md` — guidance specifically for Claude Code.
- `docs/HOST_POWER_MANAGEMENT.md` — CONST-033 background and runbook.

<!-- BEGIN iter-52 anti-bluff covenant propagation (CONST-035) -->
### MANDATORY ANTI-BLUFF COVENANT — END-USER QUALITY GUARANTEE (User mandate, 2026-04-28)

**Forensic anchor — direct user mandate (verbatim):**

> "We had been in position that all tests do execute with success
> and all Challenges as well, but in reality the most of the
> features does not work and can't be used! This MUST NOT be the
> case and execution of tests and Challenges MUST guarantee the
> quality, the completion and full usability by end users of the
> product!"

**Operative rule:** the bar for shipping is **not** "tests pass"
but **"users can use the feature."** Every PASS in this codebase
MUST carry positive evidence captured during execution that the
feature works for the end user. Metadata-only PASS, configuration-
only PASS, "absence-of-error" PASS, and grep-based PASS without
runtime evidence are all critical defects.

**Tests AND Challenges (HelixQA) are bound equally** — a Challenge
that scores PASS on a non-functional feature is the same class of
defect as a unit test that does.

### Verification commands

Run before claiming a fix is complete:

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash yole-challenges/scripts/anchor_manifest_challenge.sh
bash yole-challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS. Pre-existing bluff hits are tracked in
`yole-challenges/baselines/bluff-baseline.txt`; do not extend the baseline
without an explicit justification comment.

**Skip-marker convention:** `// SKIP-OK: #<ticket>` (canonical),
`// ANTI-BLUFF-EXEMPT: <reason>` (synonym).

<!-- END iter-52 anti-bluff covenant propagation (CONST-035) -->
