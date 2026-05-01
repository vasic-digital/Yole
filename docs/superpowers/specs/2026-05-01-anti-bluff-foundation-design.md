# Anti-Bluff Foundation — Design Spec

**Date:** 2026-05-01
**Status:** Approved (brainstorming complete; awaiting implementation plan)
**Constitution rule introduced:** CONST-034
**Sub-project:** 1 of 6 in the Anti-Bluff Discipline campaign
**Owner:** Milos Vasic
**Resume protocol:** sections are self-contained; resume by checking
`docs/campaigns/anti-bluff/CAMPAIGN.md` for last completed checkpoint.

## Background

This project has a documented history where test suites passed while
features were unusable for end users. The cause is **bluff tests**:
tests that pass without exercising the user-visible behavior they claim
to verify. CONST-034 makes that failure mode detectable and blocks its
recurrence.

Sub-project 1 (this spec) lays the foundation: rule, gates, baseline,
manifest skeleton. It writes no new feature tests, fixes no existing
bluff hits, and increases no coverage. Its sole job is to land the
rule and the enforcement so that subsequent sub-projects work against
a measurable baseline.

The campaign as a whole consists of six sub-projects, each with its
own spec and plan:

1. Anti-Bluff Foundation (this spec)
2. Resumable Campaign Tracker formalisation
3. Bluff audit of existing tests/Challenges
4. Punch list fixes (round 1)
5. Coverage push (component-by-component)
6. Operational cadence formalisation

## Decisions log (the eight inputs that drove this design)

| # | Decision | Choice |
|---|----------|--------|
| Q1 | Formal rule | A test is bluff if it can pass without exercising the user-visible behavior it claims to verify |
| Q2 | Enforcement mechanism | Static scanner + mutation testing + behavior-anchor manifest, all gating from day one |
| Q3 | "User-visible behavior" definition | Three layers: app (UI/observable output), KMP module (public-API integration), Go submodule (CLI vs real target) |
| Q4 | Failure mode | Hard block on new code, baseline-grandfathered for existing |
| Q5 | Submodule strategy | Verbatim CONST-034 copy in each submodule, independent enforcement |
| Q6 | Mutation threshold | 90% kill on changed code, 80% project-wide ratchet |
| Q7 | Push cadence | Hybrid — feature branches push at checkpoint boundaries; master pushes immediately. Submodule pointers only follow already-pushed submodule SHAs |
| Q8 | Anchor manifest granularity | Per user-facing capability, ~100 rows expected |

## Scope (sub-project 1 deliverables)

1. **CONST-034** added to all four `CONSTITUTION.md` files (main + 3
   submodules), wording verbatim with the per-repo three-layer
   adaptation built into the rule itself.
2. **Anti-Bluff section** added to all four `CLAUDE.md` and all four
   `AGENTS.md` files (8 governance pointers total).
3. **Static scanner** (`scripts/anti-bluff/bluff-scanner.sh` + language
   helpers) — one copy per repo, four total. Detects 8 Kotlin patterns
   and 7 Go patterns (full list below).
4. **Mutation gate config** — Pitest wired into `shared/` and each of
   the 10 KMP modules; `go-mutesting` config in each Go submodule.
   Threshold 90% changed / 80% project-wide ratchet.
5. **Anchor manifest** at `docs/behavior-anchors.md` in each repo.
   Schema-only at sub-project 1 close (zero `active` rows). Build-time
   check verifies every row's anchor test path resolves.
6. **Bluff baseline** at `challenges/baselines/bluff-baseline.txt` in
   each repo. Three sections: scanner hits, per-file mutation kill
   rates, anchor-manifest gaps.
7. **Wiring**: three new challenge scripts
   (`bluff_scanner_challenge.sh`, `mutation_ratchet_challenge.sh`,
   `anchor_manifest_challenge.sh`) wired into `make qa-all`,
   `make challenge`, `runChallenges`. Pre-commit hook installs
   scanner + manifest check (mutation excluded — too slow).
8. **Master campaign tracker stub** at
   `docs/campaigns/anti-bluff/CAMPAIGN.md` (main repo only; full
   formalisation deferred to sub-project 2).
9. **First-time push** to all upstreams per the hybrid cadence.

## Out of scope for sub-project 1

- Populating anchor manifest with capability rows (sub-project 3).
- Reducing the bluff baseline (sub-projects 4–5).
- Adding new tests for coverage (sub-project 5).
- Fixing pre-existing CLAUDE.md "Known Issues" items (different
  sub-projects).
- Detecting sophisticated semantic bluff beyond mutation (deferred
  indefinitely; mutation + anchors are the strong signal).

## CONST-034 — full text (added verbatim to all four `CONSTITUTION.md`)

```markdown
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
`docs/behavior-anchors.md` (manifest), `docs/campaigns/anti-bluff/CAMPAIGN.md`
(active campaign tracker).
```

## Static scanner design

**One scanner per repo, four total.** Same patterns and exit semantics
across all four; per-language matchers (Kotlin/JVM in main repo, Go in
submodules).

**Location:** `scripts/anti-bluff/bluff-scanner.sh` (entry point),
`scripts/anti-bluff/lib/kotlin.sh`, `scripts/anti-bluff/lib/go.sh`.

**Inputs:**

- `--mode all` (full tree) | `--mode changed` (git diff against `master`).
- `--baseline challenges/baselines/bluff-baseline.txt`.
- `--exclude-paths` — read from inline `# scanner-exclude:` comments in
  source files. Each exclude requires a one-line justification.

**Patterns detected (Kotlin/JVM):**

| ID | Pattern | What it catches |
|----|---------|------|
| BLUFF-K-001 | `mockk<X>()` / `mock<X>()` where `X` is also imported as the SUT in the same test class | Mocking the unit under test |
| BLUFF-K-002 | `assertTrue(true)`, `assertEquals(x, x)`, `assertNotNull(...)` as the sole assertion in a `@Test` body | Trivial assertions |
| BLUFF-K-003 | `@Ignore` without an accompanying `// ANTI-BLUFF-EXEMPT: <ticket-id>` comment | Permanent skip |
| BLUFF-K-004 | `assumeTrue(false)`, `assumeTrue(System.getenv("CI") != null)` patterns | Always-skipped tests |
| BLUFF-K-005 | `runBlocking { }` / `runBlocking<Unit> { }` test bodies with no `await`/`join`/`first()`/`collect`/`assert*` calls | Coroutine no-op tests |
| BLUFF-K-006 | `@Test` method with empty body or only `// TODO`/`// FIXME` comments | Stub tests |
| BLUFF-K-007 | Test class file imports SUT but never instantiates it (only references via mock) | SUT-via-mock-only |
| BLUFF-K-008 | Uses `@Suppress("BLUFF")` annotation without justification line | Suppression abuse |

**Patterns detected (Go):**

| ID | Pattern | What it catches |
|----|---------|------|
| BLUFF-G-001 | `t.Skip()` / `t.Skipf(...)` without `// ANTI-BLUFF-EXEMPT: <ticket-id>` comment | Permanent skip |
| BLUFF-G-002 | `if testing.Short()` returning early without sub-tests covering the long path elsewhere | Short-circuit skip |
| BLUFF-G-003 | Test function body with only `t.Log`, no `t.Fatal`/`t.Error`/`t.Errorf`/assert calls | No-assert test |
| BLUFF-G-004 | `gomock.NewController` mocking a type from the same package as the test's package | Mocking the SUT |
| BLUFF-G-005 | `t.Run("", func(t *testing.T) { })` — empty subtest body | Stub subtest |
| BLUFF-G-006 | `func TestXxx(t *testing.T) {}` — empty test body | Empty test |
| BLUFF-G-007 | `assert.True(t, true)`, `assert.NotNil(t, x)` as sole assertion (testify) | Trivial assertion |

**False-positive policy:**

- One line per hit: `<path>:<line>:<BLUFF-ID>:<one-line-context>`.
- Hits matching a line in `bluff-baseline.txt` (by `<path>:<BLUFF-ID>`
  key, line-number tolerant) are filtered.
- Hits with an inline `# scanner-exclude: BLUFF-K-001 because <reason>`
  comment on the line above are filtered, but the exclude itself is
  logged to `challenges/anti-bluff-excludes.log` for review.
- Equivalent-mutant exemptions use `// ANTI-BLUFF-EXEMPT: TRIVIAL-CORRECT
  — <reason>`. Scanner accepts these without logging.

**Exit codes:** `0` clean, `1` new bluff (blocks commit/qa-all), `2`
baseline drift (a previously-baselined hit is gone — congratulations,
but the baseline file is stale; user must remove the line), `3`
invocation error.

## Mutation gate design

**Tooling per layer:**

- **Kotlin/JVM (main `shared/`, all 10 KMP modules)** — Pitest 1.17.x
  via `pitest-gradle-plugin`. Runs against JVM-target tests only;
  cannot mutate Kotlin Native or WASM. KMP coverage of those targets
  relies on the anchor manifest (explicit known limitation).
- **Go submodules** — `go-mutesting` (current Avito fork). Standard
  mutators: arithmetic, conditional, branch, removal.

**Per-module config** at `<module>/pitest.gradle.kts` (Kotlin) or
`<module>/.go-mutesting.yml` (Go).

**Threshold semantics:**

- **Changed code:** every file in `git diff master...HEAD` must show
  ≥90% mutation kill rate. Pre-existing files modified in the change
  pick up the same gate.
- **Project-wide ratchet:** post-run kill rate per file must be ≥
  baseline value. The baseline file stores per-file kill rates; the
  ratchet check runs file-by-file.
- **New files:** "changed code" — must hit 90% on first commit. No
  grace period.

**Where mutation runs:**

- Pre-commit: NOT mutation (too slow). Pre-commit runs scanner only.
- `make qa-all`: scoped to changed files by default; full ratchet on
  `MUTATION_FULL=1`.
- `runChallenges` Gradle task: same shape.
- Per-submodule: each Go submodule runs `go-mutesting` per package.

## Baseline file format

`challenges/baselines/bluff-baseline.txt` is plain text, one record per
line, sorted, three sections.

```
# === SECTION 1: STATIC SCANNER HITS ===
# Format: <path>:<BLUFF-ID>
shared/src/commonTest/kotlin/.../FooParserTests.kt:BLUFF-K-002
...

# === SECTION 2: PER-FILE MUTATION KILL RATES ===
# Format: <path>:<kill-rate-percent>:<mutants-tested>
shared/src/commonMain/kotlin/.../MarkdownParser.kt:87:142
...

# === SECTION 3: ANCHOR-MANIFEST GAPS ===
# Format: <capability-id>:MISSING_ANCHOR
CAP-001:MISSING_ANCHOR
...
```

**Ratchet logic:**

- Section 1: additive only. New line = gate failure. Removed line = OK
  and required when sub-project 4 fixes a hit.
- Section 2: per-file numeric. `current_rate < baseline_rate` for any
  file = gate failure. Equal or higher = OK; if higher, baseline is
  updated in the same commit via `make update-baseline`.
- Section 3: counter. New rows allowed only when adding a new
  capability with `status: pending-anchor`.

## Anchor manifest design

**Location:** `docs/behavior-anchors.md` in each repo.

**Format** — Markdown table, one row per capability. YAML frontmatter
for metadata.

```markdown
---
schema_version: 1
constitution_rule: CONST-034
last_audit: 2026-05-01
---

# Behavior Anchor Manifest

| id | layer | capability | anchor_test_path | verifies | status |
|----|-------|------------|------------------|----------|--------|
| CAP-001 | app | Open and render a Markdown file with light theme | shared/src/desktopTest/.../MarkdownRenderE2ETest.kt::renderLightTheme | Screenshot diff vs `samples/expected/markdown-light.png` within 1% pixel tolerance | active |
```

**Status values:**

- `active` — anchor exists and is callable.
- `pending-anchor` — capability declared, anchor test does not yet
  exist. Counts toward baseline gap (Section 3).
- `retired` — capability removed; row kept for history.

**Build-time check** (`anchor_manifest_challenge.sh`):

1. Parse the table; fail on schema violations.
2. For every `active` row: verify the anchor test path exists, points
   at a file the test runner can find, references an existing test
   method.
3. For every `pending-anchor` row: ensure it appears in baseline
   Section 3.
4. Cross-check: `active` rows in the manifest must include at least
   one row per capability declared in `docs/CAPABILITIES.md` (populated
   in sub-project 3). If `CAPABILITIES.md` is absent (the case for all
   of sub-project 1), the cross-check is a no-op and the challenge
   does not fail on its absence.

## Wiring

**Pre-commit hook** (`scripts/anti-bluff/pre-commit-hook.sh`, installed
by `scripts/anti-bluff/install-hooks.sh` into `.git/hooks/pre-commit`):

```
1. bluff-scanner.sh --mode changed     # < 1s
2. anchor_manifest_challenge.sh        # < 1s
3. (skipped: mutation gate — too slow for pre-commit)
```

**Makefile additions:**

```makefile
anti-bluff-scan:
	@scripts/anti-bluff/bluff-scanner.sh --mode all

anti-bluff-anchors:
	@bash challenges/scripts/anchor_manifest_challenge.sh

anti-bluff-mutation:
	@bash challenges/scripts/mutation_ratchet_challenge.sh

anti-bluff-mutation-changed:
	@bash challenges/scripts/mutation_ratchet_challenge.sh --mode changed

anti-bluff: anti-bluff-scan anti-bluff-anchors anti-bluff-mutation-changed

qa-all: test-shared challenge helixqa-test anti-bluff
```

**Three new challenge scripts** in `challenges/scripts/`:

- `bluff_scanner_challenge.sh`
- `anchor_manifest_challenge.sh`
- `mutation_ratchet_challenge.sh`

**Gradle `runChallenges` task** updated to depend on the three new
challenge scripts.

**Submodules** get the same wiring — each submodule has its own
`Makefile`, its own `challenges/scripts/`, its own pre-commit installer.
Verbatim copies adapted only for Go (no Pitest, only `go-mutesting`).

**Documentation per repo:**

- New `docs/ANTI_BLUFF.md` — runbook, scanner pattern reference,
  mutation tooling guide, "I got a bluff hit, what now?" troubleshooting.
- `CLAUDE.md` and `AGENTS.md`: short pointer to `ANTI_BLUFF.md` and the
  Verification Commands block (matching existing CONST-033 style).
- `CHANGELOG.md`: one line per repo.

## Bluff-tools-themselves anti-bluff

The scanner and mutation gate are themselves code that could be bluff.
Mitigation:

- Scanner gets a fixture suite under `scripts/anti-bluff/tests/`:
  handcrafted Kotlin/Go snippets, each with an expected scanner verdict.
  Scanner CI runs the fixtures; a missed pattern fails the gate's own
  gate.
- Mutation gate gets a self-mutation test: deliberately-faulty fixture
  with known kill rate, asserting the gate flags it correctly.
- Both fixture suites are listed in the manifest as anchored
  capabilities so they can't be deleted without notice.

## Sequencing

**Pre-checkpoint bootstrap** (main repo only, before any submodule
work begins): create `docs/campaigns/anti-bluff/CAMPAIGN.md` stub with
sub-project list, decisions log, and empty per-repo state table. Commit
to `campaign/anti-bluff` in main, push for durability. This is the
single source of truth all subsequent work writes back to. Counted
as a pre-step, not as one of the 12 numbered checkpoints.

**Repo order** (submodule pointers must only advance to already-pushed
SHAs):

1. `Challenges/` — submodule
2. `Containers/` — submodule
3. `HelixQA/` — submodule
4. `Yole/` — main repo (last; picks up new submodule SHAs)

Submodules can be done in parallel (3 background agents, one per
submodule). Each agent updates its own row in the main-repo
`CAMPAIGN.md` (or reports back so the main session updates it) at every
checkpoint, so the tracker stays current across parallel work.

**Per-repo internal sequence (~12 checkpoints per repo, 48 total):**

| # | Checkpoint | Branch |
|---|-----------|--------|
| 1 | Create `campaign/anti-bluff` branch | campaign |
| 2 | Add `docs/ANTI_BLUFF.md` | campaign |
| 3 | Add CONST-034 to `CONSTITUTION.md` | campaign |
| 4 | Anti-bluff section in `CLAUDE.md` + `AGENTS.md` | campaign |
| 5 | `scripts/anti-bluff/` (scanner + lib + fixtures + install-hooks) | campaign |
| 6 | Run scanner full tree → capture baseline Section 1 | campaign |
| 7 | Configure mutation tool (Pitest / go-mutesting) | campaign |
| 8 | Run full mutation pass → capture baseline Section 2 | campaign (slow — 30–60 min on Kotlin) |
| 9 | Add `docs/behavior-anchors.md` (schema-only, zero rows) | campaign |
| 10 | Three challenge scripts + `Makefile` + `runChallenges` wiring | campaign |
| 11 | `make qa-all` end-to-end — must pass on baseline; deliberate-bluff fixture must fail; remove fixture | campaign |
| 12 | Merge to `master`, delete campaign branch, push to all remotes | master |

## Push points

- After **checkpoint 12 of each submodule**: push that submodule's
  `master` to its remotes. Trigger for main repo's checkpoint 12 to
  advance the submodule pointer.
- After **checkpoint 12 of main**: push `master` to all three remotes
  (`github`, `origin`, `upstream`).
- During campaign-branch work (checkpoints 1–11): push
  `campaign/anti-bluff` after each checkpoint to all remotes for
  durability.

## Master campaign tracker stub

`docs/campaigns/anti-bluff/CAMPAIGN.md` — created in main repo only at
sub-project 1. Contents:

- Sub-project list (1 of 6 in progress; 2–6 pending).
- Per-repo state table (branch, last checkpoint, next checkpoint, last
  push).
- Decisions log (Q1–Q8 from this spec).
- Open risks (Pitest vs Native/WASM, equivalent mutants, submodule
  pointer churn).
- Resume cheatsheet.

Full formalisation deferred to sub-project 2.

## Estimated effort

- ~3 days focused, ~5 days realistic.
- First mutation runs (~30–60 min each, 4 of them) are the longest
  individual steps; can run in the background while drafting docs.

## Risks

- **Pitest cannot mutate Kotlin Native or WASM** — KMP coverage of
  those targets relies entirely on the anchor manifest. Track gap
  explicitly in tracker.
- **Equivalent mutants in Pitest** may produce false 90%-threshold
  misses; exemption mechanism is `// ANTI-BLUFF-EXEMPT:
  TRIVIAL-CORRECT — <reason>` inline. Watch for over-use in
  sub-project 4.
- **Submodule pointer churn** during sub-project 1: 3 main-repo
  "update submodule pointer" commits (one per submodule) batched into
  checkpoint 12 of main. Single push, not three.
- **Scanner heuristic gaps** — language scanners are pattern-based and
  will miss sophisticated bluff. Mutation gate covers semantic bluff;
  anchor manifest covers feature-level bluff. The three layers
  together are the defence.

## Acceptance criteria for sub-project 1

A reviewer can verify sub-project 1 is done by running, in each of the
four repos:

```bash
make qa-all                                # all gates green on baseline
bash scripts/anti-bluff/bluff-scanner.sh --mode all   # zero new hits
bash challenges/scripts/anchor_manifest_challenge.sh  # passes
bash challenges/scripts/mutation_ratchet_challenge.sh # passes
git log -1 --format=%s                     # last commit references CONST-034
git remote -v                              # all remotes reachable
test "$(git log '@{u}..HEAD' --oneline | wc -l)" -eq 0   # no unpushed commits on current branch
```

And by inspecting:

```bash
grep -l "CONST-034" CONSTITUTION.md CLAUDE.md AGENTS.md  # all three contain it
test -f scripts/anti-bluff/bluff-scanner.sh
test -f challenges/baselines/bluff-baseline.txt
test -f docs/behavior-anchors.md
test -f docs/ANTI_BLUFF.md
test -f docs/campaigns/anti-bluff/CAMPAIGN.md  # main repo only
```

When all eight commands pass in all four repos, sub-project 1 is
complete and sub-project 2 (Resumable Campaign Tracker formalisation)
can begin.

## What sub-project 1 does NOT do

- Write new feature tests beyond scanner/mutation self-tests.
- Populate anchor manifest with capability rows (sub-project 3).
- Reduce the bluff baseline (sub-project 4).
- Increase coverage (sub-project 5).
- Fix any pre-existing CLAUDE.md "Known Issues" item.

The sole purpose of sub-project 1 is to land the rule and the gates,
capture the baseline, and prove the gates trip on a deliberate
violation.
