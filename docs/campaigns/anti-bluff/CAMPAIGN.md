# Campaign — Anti-Bluff Discipline (CONST-035)

**Started:** 2026-05-01
**Status:** sub-project 1 DONE (anti-bluff foundation in main + 3 submodules); sub-project 2 next
**Resume protocol:** read this file top to bottom, find the first
checkpoint with status ≠ done, resume there.

## Sub-projects

1. [done 2026-05-01] Anti-Bluff Foundation — see spec
   `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-design.md`,
   adjustments
   `docs/superpowers/specs/2026-05-01-anti-bluff-foundation-ADJUSTMENTS.md`,
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
| Challenges | main | 1.12 merged | n/a (Phase 1 done; Section 2 baseline deferred to sub-project 4) | 13f490b |
| Containers | main | 1.12 merged | n/a (Phase 1 done; Section 2 baseline deferred to sub-project 4) | 002bd39 |
| HelixQA    | main | 1.12 merged | n/a (Phase 1 done; Section 2 captured 86.3% on buildable subset) | fa02c7f |
| Yole       | master | 2.13 (merged) | n/a (sub-project 1 done) | (TBD post-merge) |

### Decisions log (Q1–Q8 from brainstorming)

- Q1 → bluff = passes without exercising user-visible behavior (formal rule).
- Q2 → enforcement = scanner + mutation + anchor manifest, all gating.
- Q3 → user-visible = three layers (app / KMP / Go submodule).
- Q4 → hard block on new code, baseline-grandfathered.
- Q5 → submodules: independent enforcement; rule already in place via
  cascaded CONST-035, only need to add scanner/mutation/anchor infra.
- Q6 → mutation: 90% changed / 80% project ratchet.
- Q7 → push: hybrid (master/main immediate; campaign branch at checkpoints).
- Q8 → anchor manifest: per user-facing capability, ~100 rows expected.

### Discoveries (recorded 2026-05-01 mid-session)

After fetching submodule remotes, found existing infrastructure that
narrowed Phase 1 scope and adjusted naming conventions. See
`docs/superpowers/specs/2026-05-01-anti-bluff-foundation-ADJUSTMENTS.md`.
Key points:

- **CONST-035 (not CONST-034)** is the rule number — already cascaded
  from umbrella ATMOSphere CONSTITUTION.md to all 3 submodules
  (2026-04-28).
- **MANDATORY ANTI-BLUFF COVENANT** is already in all 3 submodules'
  CLAUDE.md and AGENTS.md (verbatim user mandate from 2026-04-28).
- **Skip marker convention** is `// SKIP-OK: #<ticket>` (not the
  originally-proposed `// ANTI-BLUFF-EXEMPT:`).
- **Bluff taxonomy** documented verbatim in
  `Challenges/CONSTITUTION.md` CONST-035: wrapper / contract /
  structural / comment / skip bluff. Reuse, do not reinvent.
- **Enforcement infrastructure does not yet exist** anywhere — no
  scanner, no mutation gate, no anchor manifest. This remains the
  full work.

### Open risks

- Pitest cannot mutate Kotlin Native or WASM — KMP coverage of those
  targets relies entirely on the anchor manifest.
- Equivalent mutants in Pitest may produce false 90% threshold misses;
  exemption mechanism is `// SKIP-OK: #equiv-mutant — <reason>`.
- Submodule pointer churn during sub-project 1: 3 main-repo "update
  submodule pointer" commits batched into checkpoint 12 of main.
- HelixQA has 6 remotes; pushes must use named remotes individually.

### Resume cheatsheet

- Lost: read this file's per-repo state table.
- Branch unclear: every per-repo working branch is `campaign/anti-bluff`.
- Stuck on a checkpoint: each task in
  `docs/superpowers/plans/2026-05-01-anti-bluff-foundation.md` lists
  its concrete output. Don't move on until that output exists.
- Naming: scanner/manifest/baseline names are unchanged from plan;
  rule-number references are CONST-035 (not CONST-034).
- Skip marker: use `// SKIP-OK: #<ticket>` exclusively.

### Per-repo remote inventory (push targets)

- **Yole (main):** `github`, `origin`, `upstream` (all alias
  `git@github.com:vasic-digital/Yole.git`)
- **Challenges:** `github`, `gitlab`, `origin`, `upstream`
- **Containers:** `github`, `gitlab`, `origin`, `upstream`
- **HelixQA:** `github`, `gitlab`, `helixgithub`, `helixgitlab`,
  `origin` (multi-URL fan-out), `upstream`

When pushing, iterate by remote name: `for r in $REMOTES; do git push "$r" <branch>; done`

### Phase 0 — main repo bootstrap (DONE)

- [x] 0.1 Create `campaign/anti-bluff` branch in main Yole repo.
- [x] 0.2 Create `docs/campaigns/anti-bluff/CAMPAIGN.md` (this file).

### Phase 1 — per-submodule rollout (DONE — completed 2026-05-01)

Three parallel implementer agents ran against Challenges/Containers/HelixQA.
HelixQA completed end-to-end including Section 2 mutation baseline.
Challenges and Containers stalled on the full-tree go-mutesting pass and
were finished by the controller; their Section 2 baselines are empty and
will be populated incrementally in sub-project 4.

**Phase 1 deliverables, per submodule (all 3):**

- [x] 1.1 `campaign/anti-bluff` branch created.
- [x] 1.2 `docs/ANTI_BLUFF.md` runbook added.
- [x] ~~1.3 / 1.4 — already in place upstream~~
- [x] 1.5 `scripts/anti-bluff/` (scanner + Go pattern lib + hook installer + fixtures + self-test).
- [x] 1.6 Section 1 baseline captured.
- [x] 1.7 `go-mutesting` configured.
- [PARTIAL] 1.8 Section 2 baseline — captured for HelixQA only; Challenges/Containers deferred to sub-project 4.
- [x] 1.9 `docs/behavior-anchors.md` skeleton.
- [x] 1.10 Three challenge scripts + Makefile/qa-all wiring.
- [x] 1.11 End-to-end + tripwire test passing in all 3.
- [x] 1.12 Merged to `main`, pushed to all remotes.

### Phase 2 — main Yole rollout (DONE — completed 2026-05-01)

After two Phase 1 subagents stalled on the full-tree go-mutesting
pass, the controller chose to execute Phase 2 directly rather than
risk another stalled agent. Pitest config + per-module mutation
baseline scoped to sub-project 4.

- [x] 2.1 Tracker updated with Phase 1 results.
- [x] 2.2 `docs/ANTI_BLUFF.md` runbook (Kotlin-flavored).
- [x] 2.3 CONST-035 added to `CONSTITUTION.md` (verbatim adaptation
  of Challenges/CONSTITUTION.md text + three-layer definition + bluff
  taxonomy).
- [x] 2.4 CONST-035 anti-bluff section added to `CLAUDE.md` and `AGENTS.md`
  with verbatim user mandate forensic anchor.
- [x] 2.5 `scripts/anti-bluff/` (scanner + Kotlin pattern lib +
  Go pattern lib + hook installer + 6-fixture self-test, all 6 pass).
- [x] 2.6 Section 1 baseline captured: 7 unique BLUFF-K-002 hits
  across commonTest/e2e/integration/network-stress/iosTest.
- [PARTIAL] 2.7 Pitest config — DEFERRED to sub-project 4 (would have
  added Pitest to 11 modules and run a multi-hour mutation pass; out of
  scope for foundation phase).
- [PARTIAL] 2.8 Section 2 baseline — DEFERRED to sub-project 4 (no
  Pitest run yet).
- [x] 2.9 `mutation_ratchet_challenge.sh` — ships as a stub that
  validates baseline file structure; sub-project 4 replaces it with a
  real Pitest invocation.
- [x] 2.10 Anchor manifest skeleton + bluff_scanner / anchor_manifest /
  mutation_ratchet challenge scripts + Makefile/qa-all wiring.
- [x] 2.11 Trip test verified: deliberate `assertTrue(true)` detected
  (BLUFF-K-002), scanner exit 1.
- [x] 2.12 Submodule pointers advanced (already done in Phase 1
  completion commit d381075d).
- [x] 2.13 Merge to master + push to github/origin/upstream.
- [x] 2.14 This tracker updated — sub-project 1 marked done.

## Sub-project 1 acceptance summary

Per the spec's acceptance criteria, in each of the 4 repos:

- `grep -l "CONST-035" CONSTITUTION.md CLAUDE.md AGENTS.md` returns
  all three files (verified).
- `scripts/anti-bluff/bluff-scanner.sh` exists and is executable.
- `challenges/baselines/bluff-baseline.txt` exists with 3 sections.
- `docs/behavior-anchors.md` exists.
- `docs/ANTI_BLUFF.md` exists.
- `bash scripts/anti-bluff/bluff-scanner.sh --mode all` exits 0.
- `bash challenges/scripts/anchor_manifest_challenge.sh` exits 0.
- `bash challenges/scripts/mutation_ratchet_challenge.sh` exits 0
  (Yole main: stub; submodules: real go-mutesting in HelixQA, stub
  in Challenges/Containers pending sub-project 4).

## Known gaps for sub-project 4

- **Mutation Section 2 baselines incomplete:** Yole main (empty),
  Challenges (empty), Containers (empty); HelixQA captured 86.3% on
  buildable pkg/nexus subset.
- **Anchor manifest empty:** all 4 repos have schema-only manifests;
  rows populated during sub-project 3 (audit) and ratified by
  sub-project 4 (punch list fixes).
- **AST-aware scanner patterns deferred:** BLUFF-K-001 / BLUFF-K-005 /
  BLUFF-K-007 / BLUFF-G-002 / BLUFF-G-004 require a Kotlin/Go parser
  to detect reliably; current awk-based scanner catches the easy half.
- **Section 1 baseline reduction:** Yole has 7 baselined hits;
  Challenges 1; Containers 4; HelixQA 12. Reducing these is
  sub-project 4 work.
- **Definition-of-Done linkage:** the existing `qa-all` target now
  depends on `anti-bluff` everywhere, but the broader "evidence in
  PR body" / "demo before code" mandates from CLAUDE.md are not yet
  mechanically enforced. Sub-project 6 (operational cadence) addresses
  this.
