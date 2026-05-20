<!-- SPDX-License-Identifier: CC0-1.0 -->
# Phase 5 Anti-Bluff Audit — Batch Index

Module-by-module mutation-verification audit of every Yole-owned test file
and challenge script (CONST-035 / CONST-039). Each batch is audited by one
report-only subagent that writes a fixed-schema findings file here. The
exact file list per batch is in `batch-manifest.txt`.

**Status flow:** `pending` → `audited` → (rolls into `CONSOLIDATED.md`).

| Batch | Scope | Files | Findings file | Status |
|-------|-------|------:|---------------|--------|
| B01 | `shared/commonTest` format/ — part 1 (asciidoc → coverage) | 32 | `B01-format-1.md` | audited — 4 bluff / 7 suspect |
| B02 | `shared/commonTest` format/ — part 2 (creole → performance) | 32 | `B02-format-2.md` | audited — 2 bluff / 13 suspect |
| B03 | `shared/commonTest` format/ — part 3 (plaintext → wikitext) | 32 | `B03-format-3.md` | audited — 1 bluff / 9 suspect |
| B04 | `shared/commonTest` network/ — part 1 (auth, common) | 32 | `B04-network-1.md` | audited — 1 bluff / 4 suspect |
| B05 | `shared/commonTest` network/ — part 2 (common, platform, protocol) | 32 | `B05-network-2.md` | audited — 3 bluff / 19 suspect |
| B06 | `shared/commonTest` network/ — part 3 (protocols, stress) | 32 | `B06-network-3.md` | pending |
| B07 | `shared/commonTest` — completion, concurrency, api, challenges | 28 | `B07-common-1.md` | pending |
| B08 | `shared/commonTest` — import_, integration, language, lsp, model | 28 | `B08-common-2.md` | pending |
| B09 | `shared/commonTest` — monitoring, performance, safety, security, syntax, ui, util, e2e, database | 26 | `B09-common-3.md` | pending |
| B10 | `shared/desktopTest` — part 1 | 36 | `B10-desktop-1.md` | pending |
| B11 | `shared/desktopTest` — part 2 | 35 | `B11-desktop-2.md` | pending |
| B12 | `androidApp` tests — part 1 (Robolectric / Compose UI) | 28 | `B12-android-1.md` | pending |
| B13 | `androidApp` tests — part 2 (Robolectric / Compose UI) | 28 | `B13-android-2.md` | pending |
| B14 | `desktopApp` + `webApp` + `shared/wasmJsTest` + `shared/iosTest` | 26 | `B14-misc-platforms.md` | pending |
| B15 | `yole-challenges/scripts/*.sh` — all 39 challenge scripts | 39 | `B15-challenge-scripts.md` | pending |

**Total:** 427 test files + 39 challenge scripts = 466 audited units.

## Audit technique

For each test method, the CONST-039 mutation-verification thought experiment:
*"If every line of the unit under test were replaced with a trivial stub,
would this test still pass?"* — yes ⇒ **BLUFF**. For challenge scripts:
*"Does a PASS carry positive runtime evidence the feature works for the end
user?"* — no ⇒ **BLUFF**.

## Findings schema

Each `B##-*.md` file lists every in-scope file with a verdict
(`CLEAN` / `SUSPECT` / `BLUFF`), evidence with line numbers, and a concrete
recommended fix. `SUSPECT` and `BLUFF` rows roll up into `CONSOLIDATED.md`
(Phase 5C Task 12), get a `P5-FIX-NNN` id, and become Phase 5E fix tasks.

## Out of scope (CONST-038)

`HelixQA/tools/opensource/leakcanary/**` — third-party upstream. Owned-
submodule test suites are audited under each submodule's own governance,
not this Yole-repo phase.
