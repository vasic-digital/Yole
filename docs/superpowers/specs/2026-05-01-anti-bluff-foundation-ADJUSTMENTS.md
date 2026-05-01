# Anti-Bluff Foundation — Adjustments to Spec/Plan (2026-05-01, late afternoon)

After committing the original spec and plan, fetching all submodule
remotes revealed substantial pre-existing anti-bluff infrastructure that
the original docs were unaware of. This file records the targeted
adjustments. Original spec and plan stay in place as the design record;
this file is the authoritative delta.

## Findings

1. **All 3 submodules already have `CONSTITUTION.md`** with `CONST-033`
   (host-power) and `CONST-035` (anti-bluff cascaded from umbrella).
   Containers and HelixQA also have `CONST-036` (user-session
   termination, 2026-04-28). Original spec assumed Containers and
   HelixQA had no `CONSTITUTION.md`; they do.

2. **Submodule `CLAUDE.md` and `AGENTS.md` already contain a
   "MANDATORY ANTI-BLUFF COVENANT — END-USER QUALITY GUARANTEE
   (User mandate, 2026-04-28)" section**, plus the cascaded `CONST-035`
   addendum. Same mandate the user is asking us to enforce, dated days
   before this session.

3. **Existing skip-marker convention is `// SKIP-OK: #<ticket>`**, not
   `// ANTI-BLUFF-EXEMPT:`. Multiple test files in Containers and
   Challenges already use this marker.

4. **Umbrella ATMOSphere `CONSTITUTION.md`** lives at
   `/run/media/milosvasic/DATA4TB/Projects/Android_15/docs/guides/ATMOSPHERE_CONSTITUTION.md`
   and uses `§N.N` section numbering (e.g., §8.1, §11, §11.4, §12.6).
   `CONST-035` in submodules is a cascade wrapper around §11.

5. **Bluff taxonomy already documented** in `Challenges/CONSTITUTION.md`
   §`CONST-035` "End-User Usability Mandate (2026-04-29 strengthening)":
   wrapper bluff / contract bluff / structural bluff / comment bluff /
   skip bluff. Reuse verbatim — do not reinvent.

6. **Anti-bluff enforcement infrastructure does NOT yet exist
   anywhere.** No `bluff_scanner_challenge.sh`, no
   `mutation_ratchet_challenge.sh`, no `anchor_manifest_challenge.sh`,
   no `docs/behavior-anchors.md`, no Pitest/go-mutesting config.
   Submodule `challenges/scripts/` only contains CONST-033 host-power
   challenges plus the existing per-submodule challenge scripts
   (`challenges_compile_challenge.sh` etc. in Challenges).

7. **HelixQA vision docs already specify** `go-mutesting` at "90% kill
   rate" as the target (`HelixQA/docs/vision/RESEARCH_SUMMARY.md`).
   Aligning with that target ratifies prior design intent.

## Adjustments

### Numbering: CONST-035 (not CONST-034)

Adopt `CONST-035` for the anti-bluff rule in main Yole `CONSTITUTION.md`
to match the existing cross-project numbering. `CONST-034` is left
unused in main Yole so the slot can be reserved for the
user-session-termination rule (which is `CONST-036` elsewhere; main
Yole may eventually adopt it as `CONST-034` to keep its own numbering
contiguous, or skip directly to `CONST-036` to align — that decision
is deferred).

### Rule text: reuse Challenges/CONSTITUTION.md CONST-035 verbatim

For main Yole's `CONSTITUTION.md`, the `CONST-035` section is the same
text already in `Challenges/CONSTITUTION.md` lines 100–192 (cascaded
addendum + bluff taxonomy + forensic anchor). Adapt only the path
references (e.g., "this submodule" → "this project").

### Skip marker: SKIP-OK

The scanner accepts `// SKIP-OK: #<ticket>` as a valid skip exemption
(Go) and `// SKIP-OK: #<ticket>` (Kotlin, comment style preserved).
Drop the `ANTI-BLUFF-EXEMPT:` marker entirely.

For mutation-equivalent exemptions where the trivial assertion is
correct by design, scanner accepts `// SKIP-OK: #equiv-mutant — <reason>`
or `// ANTI-BLUFF-EXEMPT: TRIVIAL-CORRECT — <reason>`. Both forms
recognized; the second is a synonym.

### Submodule Phase 1 scope shrinks

The original plan's submodule Phase 1 had 12 checkpoints. With the
governance docs already in place, the actual work per submodule is:

- ~~Task 1.2: docs/ANTI_BLUFF.md~~ — STILL NEEDED. Per-submodule runbook
  doesn't exist yet.
- ~~Task 1.3: Create CONSTITUTION.md~~ — SKIP (already exists with
  CONST-035 cascaded).
- ~~Task 1.4: Anti-bluff section in CLAUDE.md/AGENTS.md~~ — SKIP
  (already exists as the COVENANT section).
- Tasks 1.1, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12 — STILL NEEDED.
  Branch creation, scanner, scanner baseline, go-mutesting config,
  mutation baseline, anchor manifest skeleton, challenges wiring,
  trip test, merge.

Per-submodule task count reduces from 12 to 9.

### Main Yole Phase 2 scope unchanged

Main Yole has no anti-bluff rule, no scanner, no mutation gate, no
manifest. All of Phase 2's 14 tasks remain in scope, with one
adjustment: `CONST-035` (not `CONST-034`) and the rule text adapted
verbatim from `Challenges/CONSTITUTION.md`.

### Acceptance criteria addition

Add to spec § "Acceptance criteria for sub-project 1":

- In each submodule, `grep -c '^### CONST-035\|^## CONST-035' CONSTITUTION.md` returns ≥ 1.
- In main Yole, `grep -c '^### CONST-035\|^## CONST-035' CONSTITUTION.md` returns ≥ 1.
- Skip marker convention test: a deliberate `t.Skip("test")` (no marker)
  trips the scanner; `t.Skip("test")  // SKIP-OK: #placeholder` does not.

### What stays unchanged

- Three-layer "user-visible" definition (app / KMP / Go submodule).
- Hard block on new code, baseline grandfather.
- Mutation thresholds 90% changed / 80% project ratchet.
- Hybrid push cadence.
- Anchor manifest at ~100 rows, populated in sub-project 3.
- Per-repo independent enforcement.

## Resume protocol

When resuming, read this file alongside the original spec and plan.
The original docs describe the design intent; this file describes how
to actually execute it against the real codebase state.
