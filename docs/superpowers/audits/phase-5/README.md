<!-- SPDX-License-Identifier: CC0-1.0 -->
# Phase 5 Anti-Bluff Audit — Batch Index

Module-by-module mutation-verification audit of every Yole-owned test file
and challenge script (CONST-035 / CONST-039). Each batch was audited by one
report-only subagent. Exact file list per batch: `batch-manifest.txt`.

## Status — all 15 batches audited

| Batch | Scope | Files | Bluff | Suspect | Clean |
|-------|-------|------:|------:|--------:|------:|
| B01 | `commonTest` format/ part 1 | 32 | 4 | 7 | 21 |
| B02 | `commonTest` format/ part 2 | 32 | 2 | 13 | 17 |
| B03 | `commonTest` format/ part 3 | 32 | 1 | 9 | 22 |
| B04 | `commonTest` network/ part 1 | 32 | 1 | 4 | 27 |
| B05 | `commonTest` network/ part 2 | 32 | 3 | 19 | 10 |
| B06 | `commonTest` network/ part 3 | 32 | 8 | 5 | 19 |
| B07 | `commonTest` completion/concurrency/api/challenges | 28 | 4 | 4 | 20 |
| B08 | `commonTest` import_/integration/language/lsp/model | 28 | 0 | 8 | 20 |
| B09 | `commonTest` monitoring/perf/safety/security/syntax/ui/util/e2e/db | 26 | 5 | 6 | 15 |
| B10 | `desktopTest` part 1 | 36 | 3 | 2 | 31 |
| B11 | `desktopTest` part 2 | 35 | 6 | 6 | 23 |
| B12 | `androidApp` tests part 1 | 28 | 4 | 5 | 19 |
| B13 | `androidApp` tests part 2 | 28 | 0 | 3 | 25 |
| B14 | `desktopApp`+`webApp`+`wasmJsTest`+`iosTest` | 26 | 5 | 11 | 10 |
| B15 | `yole-challenges/scripts/*.sh` | 39 | 5 | 5 | 29 |
| **Total** | | **466** | **~51** | **~104** | **~308** |

(Per-batch counts are the auditors' tallies; `CONSOLIDATED.md` re-derives
exact figures by parsing the findings files and triages every SUSPECT.)

## Audit provenance note

B12, B14, and B15 were re-audited with fresh agents after the first
attempt at those batches produced shallow/fabricated findings (verdicts
for files not actually read). The re-audits are the authoritative
versions on disk. B01–B11 and B13 are first-pass audits, spot-checked
against cited source.

## Audit technique

For each test method, the CONST-039 mutation-verification thought
experiment: *"If every line of the unit under test were replaced with a
trivial stub, would this test still pass?"* — yes ⇒ **BLUFF**. For
challenge scripts: *"Does a PASS carry positive runtime evidence the
feature works for the end user?"* — no ⇒ **BLUFF**.

## Dominant systemic patterns

1. **`assertTrue(result.isSuccess || result.isFailure)`** — a `Result<T>`
   tautology equivalent to `assertTrue(true)`; ~135 occurrences across
   network/ tests.
2. **Performance-only assertions** — tests asserting only `elapsed < Xms`;
   pass with an instant stub.
3. **`assertNotNull`-only "coverage" tests** — assert the call returned
   non-null, never inspect the value.
4. **Self-verifying test doubles** — LSP requester tests, WebApp UI tests
   that assert a flag the test itself set; production code never runs.
5. **Silently-skippable challenges** — web gates `exit 3` (SKIP-OK) when
   Node/Puppeteer is absent, producing no runtime evidence.

## Next

`CONSOLIDATED.md` (Phase 5C Task 12) numbers every BLUFF/SUSPECT as
`P5-FIX-NNN`; the operator checkpoint (Phase 5D) sets the fix-vs-defer
split before the Phase 5E TDD fix sweep begins.

## Out of scope (CONST-038)

`HelixQA/tools/opensource/leakcanary/**` — third-party upstream.
Owned-submodule test suites are audited under each submodule's own
governance, not this Yole-repo phase.
