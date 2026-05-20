<!--
SPDX-License-Identifier: CC0-1.0
-->
# Design Spec — CodeGraph Integration & Anti-Bluff Hardening Epic

- **Date:** 2026-05-20
- **Status:** Approved (user approved decomposition + Phase 0 plan on 2026-05-20)
- **Author:** Claude Code (Opus 4.7), driven by operator Milos Vasic
- **Execution mode:** Subagent-driven development, phased with checkpoints
- **Tracking doc:** `docs/CONTINUATION.md` (kept in sync per CONST-036)

## 1. Context & Motivation

The operator requested four interlocking outcomes:

1. **Precondition** — finish, commit, and push all pending work (4 dirty shared
   submodules + main repo) to all upstreams, losing nothing.
2. **CodeGraph integration** — install `@colbymchenry/codegraph` (a local
   SQLite code-knowledge-graph + MCP server) and wire it into **5 CLI agents**:
   Claude Code (primary), OpenCode, Kimi CLI, Crush, Qwen Code.
3. **Anti-bluff verification** — every existing test AND Challenge must prove the
   feature it covers genuinely works for end users (CONST-035 / CONST-039).
4. **Governance propagation** — the anti-bluff covenant must live in the root
   `HelixConstitution` submodule and inherit into every owned submodule's
   `Constitution.md` / `CLAUDE.md` / `AGENTS.md` / `QWEN.md`.

### 1.1 Investigation findings (precondition)

The 4 dirty submodules each carry a substantial uncommitted rewrite. All 4 are
genuine work that MUST be preserved. Verification against live code found that
**3 of the 4 `ARCHITECTURE.md` rewrites are factually wrong** — a documentation
bluff:

| Submodule | Confirmed defects in the working-tree rewrite |
|---|---|
| DocProcessor | `LLMAgent.Extract(ctx,prompt)` (real: `ExtractFeatures(ctx,text)`); invents `HTMLParser`/`AsciiDocParser`/`RSTParser` (real loaders: `markdown.go`, `yaml_parser.go` only); claims 5 doc formats (real: md+yaml). |
| LLMOrchestrator | drops `MultiProviderPool` (real: `pkg/agent/multi_pool.go`); drops `OpenCodeHeadlessAdapter` (real: `pkg/adapter/opencode_headless.go`); says 5 adapters (real: 6). |
| VisionEngine | deletes the entire "Remote Deployment" section (real: `pkg/remote/{deployer,distributed,remote,ssh}.go`, actively developed); lists 4 vision providers (real: 8 — `ollama`,`kimi`,`astica`,`stepgui` dropped); OpenCV table cites non-existent `opencv_stub.go`/`opencv_real.go` (real: `stub.go` + `*_vision.go`). |
| LLMsVerifier | `website/js/main.js` — clean self-consistent rewrite; no code-vs-doc drift; safe. |

## 2. Epic Decomposition (7 phases)

Each phase is a sub-project with its own implementation plan, executed
subagent-driven, ending at a checkpoint where the operator reviews before the
next phase begins.

| Phase | Title | Scope |
|---|---|---|
| 0 | Submodule doc merge (precondition) | Fix 3 docs to match code, add doc-accuracy tests, commit+push 4 submodules, bump pointers. |
| 1 | CodeGraph install + 5-agent wiring | Install codegraph; MCP config for claude/opencode/kimi/crush/qwen. |
| 2 | CodeGraph init + scan | `.codegraph/config.json` tuned for Kotlin/KMP; full index of Yole. |
| 3 | CodeGraph documentation | `docs/CODEGRAPH.md`, Makefile targets, `CONTINUATION.md`. |
| 4 | CodeGraph anti-bluff Challenge | Runtime-evidence Challenge proving codegraph works for all 5 agents. |
| 5 | Full anti-bluff re-audit | Manual mutation-verified audit of ~215 test files + ~25 challenges. |
| 6 | Governance propagation | Anti-bluff covenant + `QWEN.md` across HelixConstitution + 9 owned submodules. |

## 3. Phase 0 — Submodule Doc Merge (detailed)

**Principle:** base = current HEAD of each submodule; integrate the working-tree
work on top; fix every inaccuracy at root cause; cover every fix with a
verification test; lose nothing; push to upstreams.

### 3.1 Tasks

- **T0.1 — DocProcessor:** rewrite `docs/ARCHITECTURE.md` keeping all style gains
  (tables, mermaid, structure) but corrected against the real `pkg/` tree.
- **T0.2 — LLMOrchestrator:** same, restoring `MultiProviderPool` +
  `OpenCodeHeadlessAdapter`.
- **T0.3 — VisionEngine:** same, restoring the Remote Deployment section + all 8
  vision providers + correct OpenCV filenames.
- **T0.4 — Doc-accuracy verification test (per submodule):** a test that parses
  `ARCHITECTURE.md`, extracts every referenced package path / exported type /
  source-file name, and asserts each exists in the code. Fails on hallucination
  or on a dropped-but-still-present package. This is the anti-bluff guard.
- **T0.5 — LLMsVerifier:** commit `website/js/main.js`; add `node --check` syntax
  gate; add a jsdom smoke test if the submodule has JS test infrastructure
  (decide during execution).
- **T0.6 — Verify each submodule's existing suite:** `go test ./... -race
  -count=1` green, and confirmed genuinely exercising behavior (spot-check for
  bluff per [[feedback_anti_bluff_test_invariants]]).
- **T0.7 — Commit + push:** Conventional Commit per submodule with an honest body
  documenting the corrections; push to each upstream; bump the 4 submodule
  pointers in the Yole repo; commit the pointer bump.

### 3.2 Doc-accuracy test contract (T0.4)

Given `docs/ARCHITECTURE.md`, the test MUST:
1. Extract tokens that look like package paths (`pkg/<name>`), Go source files
   (`*.go`), and exported identifiers in fenced `go` code blocks.
2. For each package path: assert the directory exists.
3. For each `.go` filename: assert the file exists in the repo.
4. For each exported type/interface named in a `go` code block: assert a
   matching `type <Name> (struct|interface)` declaration exists.
5. Exit non-zero with a per-item report on any miss. Anti-bluff: the test must
   fail loudly if pointed at the *current* (wrong) docs.

### 3.3 Phase 0 checkpoint

Operator reviews the 3 corrected docs + the test output + push results before
Phase 1 begins.

## 4. Phases 1–6 — Design Summary

### Phase 1 — CodeGraph install + 5-agent wiring
- Install: `npm install -g @colbymchenry/codegraph` (Node 25.9.0 present ✓).
- Per-agent MCP registration (each agent's own config file):
  - **Claude Code** — project-scoped `.mcp.json` in the Yole repo (shareable,
    not global) → `codegraph serve --mcp`.
  - **OpenCode** — `opencode.json` `mcp` section.
  - **Qwen Code** — `.qwen/settings.json` `mcpServers`.
  - **Crush** — `crush.json` `mcp` section.
  - **Kimi CLI** — Kimi config `mcpServers` (exact path verified at execution).
- Decision: project-scoped where supported (index is per-project); the codegraph
  binary itself is global. No CI/CD config introduced (MANDATORY rule #1).

### Phase 2 — CodeGraph init + scan
- `.codegraph/config.json`: `languages` includes `kotlin`, `java`, `swift`,
  `javascript`, `typescript`; `exclude` build/output dirs (`build/**`,
  `.gradle/**`, `*/build/**`, `releases/**`, `node_modules/**`).
- `codegraph init` + `codegraph index`; verify `status` shows real symbol counts.
- Commit `.codegraph/config.json`; gitignore `.codegraph/codegraph.db` + caches.

### Phase 3 — CodeGraph documentation
- `docs/CODEGRAPH.md`: what it is, per-agent setup, init/scan/sync, MCP tools,
  troubleshooting. Makefile targets (`codegraph-index`, `codegraph-status`,
  `codegraph-sync`). Update `CONTINUATION.md`, `KNOWN_DEFECTS.md` if needed.

### Phase 4 — CodeGraph anti-bluff Challenge
- `yole-challenges/scripts/codegraph_integration_challenge.sh` — RUNTIME layer,
  PASS/FAIL per check + log artifact:
  - `.codegraph/codegraph.db` exists and has a symbol count above a real
    threshold (proves a genuine scan, not an empty DB).
  - `codegraph query` for a known Yole symbol (e.g. `FormatRegistry`) returns the
    real `file:line`.
  - For **each of the 5 agents**: the MCP server actually starts and a
    `tools/list` handshake returns the codegraph tools — NOT a config grep.
- Wire into `make qa-all`.

### Phase 5 — Full anti-bluff re-audit
- Systematic, module-by-module manual audit of ~215 test files + ~25 challenge
  scripts. Technique: for each test, apply mutation verification — if the unit
  under test were stubbed, would the test still pass? If yes → bluff → fix.
- Run existing gates (`bluff-scanner`, `mutation_ratchet`, `anchor_manifest`,
  `anti_bluff_cascade_audit`) and fix every real failure.
- Largest phase; honest incremental progress tracking in `CONTINUATION.md`.

### Phase 6 — Governance propagation
- Verify the anti-bluff covenant (generically phrased) in `HelixConstitution`'s
  `Constitution.md` / `CLAUDE.md` / `AGENTS.md` / `QWEN.md`; fill gaps.
- Propagate into every **owned** submodule (Challenges, Containers, HelixQA,
  LLMProvider, Security, DocProcessor, LLMOrchestrator, LLMsVerifier,
  VisionEngine); create missing `QWEN.md`. Third-party `HelixQA/tools/opensource/*`
  are out of scope (CONST-038).
- No Yole-specific platform list/feature name leaks into shared submodules.

## 5. Definition of Done (per phase)

Per `CLAUDE.md` / `CONSTITUTION.md`:
1. Code change committed.
2. Project tests pass on a clean clone.
3. All `yole-challenges/scripts/` challenges pass on the host.
4. Governance docs remain coherent.
5. `docs/CONTINUATION.md` updated (CONST-036).
6. Cross-platform impact reasoned + documented in the commit body (CONST-037).
7. Touched submodules preserve decoupling + reusability (CONST-038).
8. Every PASS carries positive runtime evidence (CONST-039).

## 6. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Pushing wrong docs to shared upstreams | Phase 0 fixes docs first; doc-accuracy test gates. |
| Losing in-progress submodule work | Never `revert`; base on HEAD, integrate on top. |
| codegraph unsupported for Kimi/Crush/Qwen | All 3 support MCP; manual config + Phase 4 runtime proof. |
| Phase 5 unbounded scope | Incremental, module-by-module, tracked in CONTINUATION. |
| Governance leak into shared submodules | Generic phrasing only; CONST-038 review per file. |

## 7. Anti-Bluff Applied To This Epic Itself

Every deliverable in this epic carries positive runtime evidence:
doc fixes are gated by an executable doc-accuracy test; the codegraph
integration is gated by a Challenge that starts each agent's MCP server;
governance changes are gated by the existing inheritance meta-tests.
No PASS without proof.
