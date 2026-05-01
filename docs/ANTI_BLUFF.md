# Anti-Bluff Discipline (CONST-035) — Runbook (Yole main)

This document is the runbook for working with the anti-bluff gates in
the Yole main repository. The rule itself lives in `CONSTITUTION.md`
(CONST-035). For per-submodule context, see each submodule's own
`docs/ANTI_BLUFF.md`.

## What "bluff" means here

A test or Challenge is **bluff** if it can pass without exercising the
user-visible behavior it claims to verify. The Yole project is mixed:

- **App layer** (Android, Desktop, iOS, Web): user-visible means a real
  human user can observe the result. Anchor signal: UI screenshot diff,
  file written to disk and re-read, network request observed on a real
  socket, intent/activity dispatched and asserted.
- **KMP module layer** (10 modules: RateLimiter, Concurrency, Auth,
  Security, Document, Config, Database, Storage, Formatters,
  UI-Components): user-visible means a developer consuming the public
  API can observe the documented contract. Anchor signal: integration
  test calling only the public API, asserting the KDoc contract.
- **Go submodule layer** (Challenges, Containers, HelixQA): each has its
  own runbook; anchor is CLI invocation against a real target.

## Three gates

1. **Static scanner** (`scripts/anti-bluff/bluff-scanner.sh`) —
   pattern matcher detecting forbidden constructs in Kotlin and Go
   sources. Runs on every commit (pre-commit hook) and in `make qa-all`.
2. **Mutation testing** (`Pitest` for Kotlin/JVM, `go-mutesting` for
   submodules). Threshold: 90% on changed code, 80% project-wide
   ratchet. Runs in `make qa-all` (slow; not in pre-commit).
3. **Behavior-anchor manifest** (`docs/behavior-anchors.md`) — every
   user-facing capability has at least one anchor test that proves it
   works end-to-end. Populated during the audit phase (sub-project 3).

## "I got a bluff hit, what now?"

The scanner output names the file, line, BLUFF-K-NNN or BLUFF-G-NNN ID,
and a one-line context. Look up the ID:

### Kotlin patterns

| ID | Pattern | Fix |
|----|---------|-----|
| BLUFF-K-001 | `mockk<X>()` of the SUT | Stop mocking the SUT; use real type or restructure. (AST-aware; deferred to follow-up scanner.) |
| BLUFF-K-002 | `assertTrue(true)`, `assertEquals(x, x)`, lone `assertNotNull` | Replace with a real assertion that exercises the SUT's behavior. |
| BLUFF-K-003 | `@Ignore` without `// SKIP-OK: #<ticket>` | Remove the @Ignore and fix the test, or add a SKIP-OK comment with a tracking ticket. |
| BLUFF-K-004 | `assumeTrue(false)` / unconditional skip | Same as BLUFF-K-003. |
| BLUFF-K-005 | `runBlocking { }` body with no awaits / no asserts | Add real coroutine work and assertions. (Deferred — needs AST awareness.) |
| BLUFF-K-006 | Empty `@Test` body | Fill or delete. |
| BLUFF-K-007 | Test class imports SUT but only references via mock | Restructure: use the real type. (Deferred — needs AST awareness.) |
| BLUFF-K-008 | `@Suppress("BLUFF")` without justification | Add justification or fix underlying issue. |

### Go patterns (submodules)

See submodule-specific runbooks; same general taxonomy with
BLUFF-G-NNN identifiers.

## "Mutation gate failed on my change"

Pitest (or `go-mutesting`) printed mutants that survived (the test
suite did not detect them). Each surviving mutant is a place where the
SUT's behavior could change without any test noticing. Either:

- Add a test that would notice (preferred), or
- Add an in-line `// SKIP-OK: #equiv-mutant — <reason>` if the mutant
  is genuinely equivalent (extremely rare).

The challenge enforces 90% kill rate on changed files. Equivalent
mutants count toward the 10% slack.

## "Anchor manifest check failed"

`anchor_manifest_challenge.sh` validates `docs/behavior-anchors.md`.
Most failures: the `anchor_test_path` you wrote does not resolve to an
existing test method. Path format:
- Kotlin: `<relative path>.kt::ClassName::methodName`
- Go: `<relative path>.go::TestName`

## Reducing the baseline

`challenges/baselines/bluff-baseline.txt` is expected to shrink during
sub-project 4. Removing a line is a **ratchet improvement**: do it in
the same commit that fixes the underlying bluff. The scanner exits
with code 2 if it sees a baselined hit that is no longer present —
this is the signal that the baseline file is stale.

## Verification commands

Run all three before declaring work done:

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS.

## What's deferred

- **Section 2 mutation baseline (Kotlin/Pitest full-tree pass)**: the
  initial Pitest run is slow (~30–60 min on `:shared:jvm`; cumulatively
  multiple hours across the 10 KMP modules). At sub-project 1 close
  the baseline Section 2 is empty for Yole main; populated incrementally
  in sub-project 4 (per-module on a slow cadence).
- **Anchor manifest population**: the manifest ships at sub-project 1
  close with zero `active` rows. Rows are populated during the audit
  phase (sub-project 3).
- **AST-aware scanner patterns** (BLUFF-K-001 mock-self, BLUFF-K-005
  runBlocking-no-op, BLUFF-K-007 SUT-via-mock-only) require a Kotlin
  parser. Deferred to a follow-up sub-project.
