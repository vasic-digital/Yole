# Anti-Bluff Discipline (CONST-035) — Milestone 2026-05-01

**Date:** 2026-05-01
**Iterations:** sub-project 1 (initial setup) + iters 6–19 (active work)
**Scope:** main Yole repo + 3 Go submodules (Challenges, Containers, HelixQA)

This is the executive-summary view of the campaign. The detailed
iter-by-iter log lives in `CAMPAIGN.md` alongside this file. Read this
for "what's done"; read `CAMPAIGN.md` for "what each iter changed".

## Starting state (sub-project 1 close)

- Anti-bluff covenant text was in submodules' `CONSTITUTION.md` /
  `CLAUDE.md` / `AGENTS.md` (cascaded from umbrella ATMOSphere
  constitution, dated 2026-04-28). **No mechanical enforcement.**
- Yole main repo had no anti-bluff rule.
- 24 pre-existing bluff hits sat unaddressed across all 4 repos.
- Anchor manifests existed as schema-only skeletons (zero rows).
- Scanner self-tests existed but were not wired into `make qa-all`.
- 3 unresolved entries on the CLAUDE.md "Known Issues" list.

## End state (after iter 19)

| Dimension | Value |
|-----------|-------|
| **CONST-035 in CONSTITUTION/CLAUDE/AGENTS** | All 4 repos × 3 docs = 12 governance docs |
| **Scanner enforcing CONST-035** | All 4 repos via `make qa-all` |
| **Pre-existing bluff hits in baselines** | **0** (was 24) |
| **Anchor manifest rows** | **123** active across 4 repos (was 0) |
| **Self-test fixtures** | **13** covering all 8 detected BLUFF-{G,K}-NNN patterns (was 6 covering 4) |
| **Scanner self-test wired into challenge** | All 4 repos (was 0) |
| **Scanner precision improvements** | 2 (Go + Kotlin string-literal stripping) |
| **CLAUDE.md "Known Issues" cleared** | 3 of 3 (Go flaky tests, AGP mismatch, container OOM) |
| **Bootstrap target for fresh-clone setup** | `make bootstrap` |
| **Cumulative commits across iters 6–19** | 35+ |
| **Cumulative pushes** | All branches (`main` + `master`) on every reachable remote per repo |

## Per-repo anchor breakdown

| Repo | Anchor rows | Coverage areas |
|------|-------------|----------------|
| Yole (main) | 49 (48 active + 1 pending) | Format×16 + Protocol×8 + Document/Monitoring/UI×8 + Android×2 + Desktop×2 + iOS×1 + Web-Wasm×9 + cross-format×3 |
| Challenges | 24 | Runner+registry+plugin+monitor+infra+userflow+anti-bluff metatest+CLI |
| Containers | 23 | Runtime+orchestration+distribution+remote/SSH+lifecycle+failover+integration |
| HelixQA | 27 | Orchestrator+detector+validator+evidence+ticket+autonomous-pipeline+LLM+vision+CLI |
| **Total** | **123** | |

## What CONST-035 enforces today

1. **Static scanner** detects 8 bluff patterns across Go and Kotlin
   sources. Runs on every `git commit` (pre-commit hook) and on every
   `make qa-all`. False-positive-resistant: string literals, raw
   strings, and comments are stripped before pattern matching.
2. **Scanner self-test** runs as phase 1 of `bluff_scanner_challenge.sh`
   in every repo. 13 handcrafted fixtures with known verdicts; if any
   awk pattern silently regresses, `make qa-all` fails before the
   tree scan runs.
3. **Anchor manifest** documents 123 user-facing capabilities across 4
   repos with their proof-of-life test paths. Build-time check verifies
   every active row's anchor test resolves.
4. **Mutation gate** (Go submodules): `go-mutesting` configured;
   HelixQA captured 86.3% kill rate on buildable pkg/nexus subset.
   Yole main + Challenges + Containers Section 2 baselines are deferred.
5. **Hard block on regression**: pre-commit hook + qa-all gate prevent
   new bluff. Baseline-grandfather mechanism allowed reducing the 24
   pre-existing hits to 0 via the ratchet pattern (no force-push, no
   silenced suppression).

## What CONST-035 does NOT yet enforce

- **AST-aware scanner patterns** (BLUFF-K-001 mock-self, BLUFF-K-005
  runBlocking-no-op, BLUFF-K-007 SUT-via-mock-only, BLUFF-G-002
  testing.Short short-circuit, BLUFF-G-004 gomock-of-same-package-SUT).
  These need a real Kotlin/Go parser; awk-based scanner can only catch
  the easy half.
- **Pitest mutation gate** for Yole main (Kotlin `:shared:jvm` + 10 KMP
  modules). The full mutation pass takes hours; deferred from
  sub-project 1 close. Mutation-gate stub script exists and validates
  baseline format.
- **Definition-of-Done linkage** beyond `qa-all` — e.g.,
  PR-body-evidence-block enforcement. Sub-project 6 work.
- **Recursive submodule bootstrap verification** — the `make bootstrap`
  target initialises submodules but doesn't yet verify nested submodule
  state (Challenges has its own Containers and Panoptic submodules).

## Resume protocol

To pick up the campaign:

1. Read `docs/campaigns/anti-bluff/CAMPAIGN.md` — full iter log + per-
   repo state table. The "Iter log" section captures every step from
   iter 6 onward in the same format used here.
2. Read this `MILESTONE-2026-05-01.md` for the high-level state.
3. Pick the next leverage point from the "What CONST-035 does NOT yet
   enforce" list above. The natural next iteration depends on
   priorities:
   - **Coverage extension:** more anchor rows per submodule
     (e.g., per-format protocol-specific tests, per-LLM-provider
     vision tests).
   - **Precision:** AST-aware scanner replacement (would close the
     5 BLUFF-* identifiers currently impossible to detect with awk).
   - **Mutation gate:** wire Pitest into `:shared:jvm` (multi-hour
     first run; consider running in background or per-module on a
     slow cadence).
   - **Operational cadence (sub-project 6):** make `bootstrap`
     idempotent + verifiable; add a per-PR `make verify` target that
     enforces the demo-before-code mandate.

## References

- `CONSTITUTION.md` § CONST-035 — the rule (all 4 repos)
- `CLAUDE.md` / `AGENTS.md` — agent-facing summary + verification
  commands (all 4 repos)
- `docs/ANTI_BLUFF.md` — runbook (all 4 repos)
- `docs/behavior-anchors.md` — the manifest (all 4 repos)
- `challenges/baselines/bluff-baseline.txt` — the baseline (all 4
  repos; Section 1 empty in all)
- `scripts/anti-bluff/bluff-scanner.sh` — the scanner (all 4 repos)
- `scripts/anti-bluff/lib/{go,kotlin}.sh` — pattern libraries
- `scripts/anti-bluff/tests/run-fixtures.sh` — self-test runner
- `challenges/scripts/bluff_scanner_challenge.sh` — qa-all wrapper
  (self-test + tree scan)
- `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md`
  — the original spec (sub-project 1)
- `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-ADJUSTMENTS.md`
  — discoveries during execution
- `docs/superpowers/plans/2026-05-01-anti-bluff-foundation.md`
  — the implementation plan (sub-project 1)
- `docs/campaigns/anti-bluff/CAMPAIGN.md` — iter-by-iter log

## Push state

All 4 repos pushed on both `main` and `master` to every reachable
remote:

| Repo | `master` | `main` | Remotes |
|------|----------|--------|---------|
| Yole | 80ded562 (post-iter-19) | 80ded562 | github, origin, upstream |
| Challenges | f330765 (post-iter-13) | f330765 | github, gitlab, origin, upstream |
| Containers | 4c49780 (post-iter-13) | 4c49780 | github, gitlab, origin, upstream |
| HelixQA | 0c3ec21 (post-iter-13) | 0c3ec21 | github, gitlab, helixgithub†, helixgitlab†, origin, upstream |

† HelixQA's `helixgithub` and `helixgitlab` remotes return SSH-auth
errors and were skipped — separate config issue, not a campaign issue.

The user can resume from any of these SHAs. The campaign is
deterministically reproducible from a fresh clone:

```bash
git clone git@github.com:vasic-digital/Yole.git
cd Yole
make bootstrap     # initialises submodules + installs anti-bluff hooks
make qa-all        # all gates pass on baseline (~ 10–30 min depending on submodule mutation cadence)
```
