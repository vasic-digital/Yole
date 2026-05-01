# Campaign — Anti-Bluff Discipline (CONST-035)

**Started:** 2026-05-01
**Status:** sub-project 1 in progress (Phase 0 done; Phase 1 next)
**Resume protocol:** read this file top to bottom, find the first
checkpoint with status ≠ done, resume there.

## Sub-projects

1. [in progress] Anti-Bluff Foundation — see spec
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
| Yole       | campaign/anti-bluff | 0.2 (tracker stub) | 2.1 (Phase 2 main repo work) | a3a390fb |

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

### Phase 2 — main Yole rollout (pending; sequential after Phase 1)

For each of `Challenges`, `Containers`, `HelixQA`:

- [ ] 1.1 Create `campaign/anti-bluff` branch.
- [ ] 1.2 Add `docs/ANTI_BLUFF.md` runbook.
- [ ] ~~1.3 Create CONSTITUTION.md~~ — SKIP, already exists with CONST-035.
- [ ] ~~1.4 Anti-bluff section in CLAUDE.md/AGENTS.md~~ — SKIP, already
  exists as MANDATORY ANTI-BLUFF COVENANT.
- [ ] 1.5 Drop in `scripts/anti-bluff/` (scanner + lib + fixtures + hook installer).
- [ ] 1.6 Run scanner full tree → capture baseline Section 1.
- [ ] 1.7 Configure `go-mutesting`.
- [ ] 1.8 Run full mutation pass → capture baseline Section 2.
- [ ] 1.9 Add `docs/behavior-anchors.md` skeleton.
- [ ] 1.10 Add 3 challenge scripts + Makefile/`runChallenges` wiring.
- [ ] 1.11 End-to-end verification + deliberate-bluff trip test.
- [ ] 1.12 Merge to `main`, push to all remotes.

### Phase 2 — main Yole rollout (pending; sequential after Phase 1)

- [ ] 2.1 Update tracker with Phase 1 results.
- [ ] 2.2 Add `docs/ANTI_BLUFF.md` (Kotlin-flavored).
- [ ] 2.3 Add CONST-035 to `CONSTITUTION.md` (verbatim from
  `Challenges/CONSTITUTION.md`).
- [ ] 2.4 Add anti-bluff section to `CLAUDE.md` and `AGENTS.md`.
- [ ] 2.5 Drop in static scanner (Kotlin-aware lib).
- [ ] 2.6 Capture scanner baseline (Section 1).
- [ ] 2.7 Configure Pitest for `shared/` and 10 KMP modules.
- [ ] 2.8 Full Pitest pass → capture baseline (Section 2).
- [ ] 2.9 Add Kotlin variant of mutation ratchet challenge.
- [ ] 2.10 Anchor manifest skeleton + scanner challenge + manifest challenge + Makefile/runChallenges wiring.
- [ ] 2.11 End-to-end verification + deliberate-bluff trip test.
- [ ] 2.12 Advance submodule pointers.
- [ ] 2.13 Merge to `master`, push to all remotes.
- [ ] 2.14 Mark sub-project 1 done in this tracker.
