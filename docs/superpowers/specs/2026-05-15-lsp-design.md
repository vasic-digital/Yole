<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Integration — Design Spec (Feature 4a of 5)

> **Status:** Brainstorm complete; awaiting operator review of written spec before invoking writing-plans.
> **Author:** brainstormed 2026-05-15 with operator.
> **Sequence:** Feature 4 of 5 in the editor-capability initiative. Features 1 (syntax highlighting, iter-57 v1.1.0), 2 (source-code support, iter-58 v1.2.0), and 3 (auto-complete, iter-60 v1.3.0) shipped. **Feature 4 is decomposed into 4a / 4b / 4c**:
> - **4a (iter-61, this spec):** LSP hosting infrastructure + completion-only integration.
> - **4b (iter-62, future spec):** Diagnostics + hover + go-to-definition.
> - **4c (iter-63, future spec):** Rename + code actions + formatting + signature help.

---

## 1. Goal

Add LSP-fed completions to Yole's editor for 15 curated languages, plugging into the existing iter-60 `CompletionProvider` interface as a 4th provider (after TokenFrequency, Snippet, Identifier). Build the LSP hosting infrastructure once in 4a so 4b + 4c plug in without re-architecting.

LSP servers run as bundled native binaries on Android (per-language Dynamic Feature Modules) and Desktop (extracted from app bundle to a user-writable dir). iOS + Web ship honest stubs returning `emptyList()` per CONST-035 because subprocess spawn is blocked on those platforms.

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Iteration decomposition | 4a (hosting + completion) → 4b (diagnostics/hover/go-to-def) → 4c (rename/actions/format/sig-help). Three iterations, each ships independently. |
| Server distribution | **Bundle 15 curated LSP servers in-APK** via Android AAB Dynamic Feature Modules (per-language download). Desktop ships all in the app bundle. iOS + Web skip. |
| Bundled language list | Maximalist 15: Rust, Go, C+C++ (clangd shared), Markdown, Lua, Zig, Python, Elixir, Haskell, TypeScript, Bash, YAML, Java (jdtls), Kotlin. Drops Crystal/Nim/OCaml from the native-12 to make room for JVM heavies. |
| LSP client library | **Eclipse LSP4J** (JVM-only). iOS + Wasm get stub provider returning `emptyList()`. |
| Process model | Lazy spawn per langId, 5-minute idle shutdown. Restart-on-crash with exponential backoff (1/min, max 5 retries). |
| Workspace root | Walk up parent directories looking for per-language project markers (Cargo.toml, go.mod, etc.). Fall back to file's parent dir. Max 20 walk-up levels. |
| Diagnostics in 4a | Cached in `DiagnosticsCache` (LSP servers emit them unsolicited), but **not rendered** until 4b. Avoids dropping on the floor. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific content leaks into sibling submodules.

---

## 3. Architecture

```
shared/src/commonMain/kotlin/digital/vasic/yole/lsp/
├── LspCompletionProvider.kt           # expect class — implements CompletionProvider
├── LspServerSpec.kt                   # data: langIds, executable, args, project markers, init options
└── LspServerRegistry.kt               # 15 supported server specs

shared/src/{androidMain,desktopMain}/kotlin/digital/vasic/yole/lsp/   (JVM actuals)
├── LspCompletionProvider.{android,desktop}.kt   # uses LSP4J
├── LspServerHost.kt                   # process lifecycle: spawn, idle shutdown, restart-on-crash
├── LspWorkspaceResolver.kt            # walk-up project marker detection
└── LspServerInstaller.kt              # extract bundled binaries to writable dir + chmod

shared/src/{iosMain,wasmJsMain}/kotlin/digital/vasic/yole/lsp/        (stubs)
└── LspCompletionProvider.{ios,wasmJs}.kt        # returns emptyList per CONST-035

shared/src/commonMain/resources/lsp-servers/<langId>/server.json
desktopApp/.../lsp/BundledLspExtractor.kt        # extract on first launch
androidApp/dynamicFeatures/lsp-<langId>/         # 15 DFMs (one per server bundle)
```

**Invariants:**

- `LspCompletionProvider` implements iter-60's `CompletionProvider` interface; no other shape needed.
- `LspServerHost` is the single owner of LSP processes. No other code spawns LSP processes.
- The iter-60 `CompletionEngineParityTest` is extended to assert `LspCompletionProvider` is in `CompletionEngine.default()`. Catches forgot-to-wire regressions.
- `expect`/`actual` pattern preserves cross-platform compile parity; iOS + Wasm cannot accidentally regress to a non-stub impl without explicit code.

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `LspServerSpec` | `langIds: List<String>`, `executable: String`, `args: List<String>`, `projectMarkers: List<String>`, `initOptions: JsonObject` | 15 instances |
| `LspServerRegistry` | Map langId → LspServerSpec; lookup by file extension via existing LanguageMetadata | 1 |
| `LspWorkspaceResolver` | Given a file path + project markers, walk up dirs (max 20 levels) and return the workspace root or fallback | 1 |
| `LspServerHost` | Owns the `Map<langId, RunningServer>` process pool; lazy spawn; 5-min idle shutdown; restart-on-crash with backoff; per-langId Mutex serializes spawn | 1 (JVM); stub on iOS/Wasm |
| `LspServerInstaller` | `ensureInstalled(langId): Result<Path>` — Android: verifies DFM installed, returns bundled path; Desktop: extracts to `~/.yole/lsp-servers/<lang>/` (or `~/.local/share/yole/lsp-servers/` on Linux), `chmod +x` | 1 (per-platform actuals) |
| `LspCompletionProvider` | Implements `CompletionProvider`; calls `textDocument/completion`; maps LSP `CompletionItem` → Yole `CompletionItem` (kind, label, insertText, score derived from sortText) | 1 (JVM); stub (iOS/Wasm) |
| `BundledLspExtractor` (desktopApp) | First-launch extraction from app-bundle resources to user-writable dir. Idempotent. | 1 |
| `DiagnosticsCache` | `Map<URI, List<Diagnostic>>` populated by `textDocument/publishDiagnostics`. Not rendered in 4a; consumed by 4b. | 1 |

## 5. Data flow

### 5.1 Cold start (first .rs open)

```
User opens hello.rs
  → IdeEditorScreen detects langId="rust" via existing GrammarRegistry
  → CompletionEngine.default(...) reconstructed with langId="rust"
  → LspCompletionProvider.complete(ctx) first call:
      ├ LspServerHost.acquire("rust"):
      │   ├ If process already running: return existing handle
      │   └ Else (cold start):
      │       ├ LspServerSpec lookup → rust-analyzer spec
      │       ├ LspServerInstaller.ensureInstalled("rust"):
      │       │   ├ Android: verify DFM "lsp-rust" is installed; if not → Result.failure → provider returns empty + emits "Tap to install" UX
      │       │   └ Desktop: BundledLspExtractor copies to ~/.yole/lsp-servers/rust/ if absent; chmod +x; returns Path
      │       ├ LspWorkspaceResolver.resolve(hello.rs, ["Cargo.toml"]) → walks up; if Cargo.toml found → that dir; else file.parent
      │       ├ ProcessBuilder(path, args).start()
      │       ├ Launcher.createIoLauncher(LanguageClient, server.in/out) → LanguageServer proxy
      │       ├ Send initialize {processId, rootUri, capabilities: {textDocument: {completion: {snippetSupport: true}}}}
      │       ├ Send initialized
      │       └ Cache (process, languageServer, lastActivity=now) keyed by "rust"
      ├ Send textDocument/didOpen for hello.rs (full content, version=0)
      ├ Send textDocument/completion {textDocument, position, context: TriggerKind.Invoked}
      ├ Wait up to 500 ms (iter-60 Phase 4's withTimeout)
      └ Map LSP CompletionItem list → Yole CompletionItem list
  → Engine merges with TokenFreq + Snippet + Identifier → ranker → popup
```

Cold start is slow (rust-analyzer 5–30 s indexing; jdtls 20+ s). v1 UX: a "Loading <lang> support…" Toast on first invoke per langId. Popup shows non-LSP completions in the meantime.

### 5.2 Steady-state path

After cold start, the server process stays alive. Each `textDocument/completion` round-trip is typically 20–200 ms. The iter-60 Phase 5 trigger's 80 ms debounce already pre-batches keystrokes, so LSP completions arrive within one debounce window in steady state.

### 5.3 Idle shutdown

`LspServerHost` runs an `IdleShutdownTicker` coroutine: every 60 s, for each langId where `now − lastActivity > 5 min` AND no document of that langId is currently open, send `shutdown` + `exit` to the server and remove the cache entry. Next file-open spawns afresh.

### 5.4 Document sync (minimal v1)

LSP completion requires the server to know current document content. Yole sends:

- `textDocument/didOpen` on first edit of a file.
- `textDocument/didChange` (incremental — `TextDocumentContentChangeEvent` with range + newText) on each keystroke.
- `textDocument/didClose` on close.

Applied uniformly to all 15 servers.

### 5.5 Diagnostics shelf

LSP servers emit `textDocument/publishDiagnostics` notifications unsolicited as the document is synced. v1 stores them in `DiagnosticsCache` keyed by URI; UI does not render. Feature 4b consumes the cache.

## 6. Error handling

| Failure | Detection | Recovery |
|---|---|---|
| Bundled LSP binary missing on device | `LspServerInstaller.ensureInstalled` returns Result.failure | Provider returns `emptyList()`; UI emits Toast "Rust language support not installed — tap to download" → DFM install (Android) or Settings link (Desktop). |
| Process spawn fails (permissions, missing exec bit, ABI mismatch) | `ProcessBuilder.start()` throws IOException | Catch, log structured error, mark langId BACKING_OFF for 60 s, provider returns empty. Auto-retry on next completion request after 60 s. |
| Server crashes mid-session (segfault, OOM) | LSP4J `onError`/`onClose` callback | Mark dead, remove from cache, Toast. Auto-restart on next request with exponential backoff: 1 retry/min, max 5 retries before giving up. |
| LSP `initialize` handshake times out | `withTimeout(30 s)` around handshake | `Process.destroyForcibly()`, mark BACKING_OFF. 30 s is generous because jdtls cold-start is ~20 s. |
| `textDocument/completion` hangs | iter-60 Phase 4 existing `withTimeout(500 ms)` per provider | Provider contributes no items this round. Other providers still emit. Anti-bluff: hang doesn't fake-PASS as "no completions"; popup shows non-LSP completions. |
| Malformed JSON-RPC from server | LSP4J handles framing; malformed messages logged + dropped | Server stays alive; we don't get a response for that request. 500 ms timeout catches it. |
| Workspace marker walk-up traverses to `/` | `LspWorkspaceResolver` walks max 20 levels then stops | Falls back to file's parent dir. Prevents pathological deep traversals. |
| DFM partial install (network drop) | Android DFM API reports `INSTALL_FAILED` | DFM install is idempotent — re-trigger from Settings. Provider returns empty until DFM is INSTALLED. |
| jdtls downloads plugins at runtime (offline-first violation) | We intercept jdtls plugin-download HTTP requests | **Track as known defect** `#iter-61-jdtls-runtime-plugin-download` — operate online or fall back to frozen snapshot. Document in user-guide. |
| Two completions arrive simultaneously (race) | `LspServerHost` serializes per-langId via Mutex | LSP4J handles request id correlation; we just don't double-spawn. |

`CancellationException` must always be rethrown in catch blocks (CONST-035 + Detekt `SwallowedException`). LSP request cancellation is supported via `$/cancelRequest`; on timeout, send it to free the server.

## 7. Per-platform feasibility matrix

| Platform | LSP server execution | Status |
|---|---|---|
| **Android** | LSP server executables bundled as **AAB assets inside per-language Dynamic Feature Modules** (one DFM per langId, with per-ABI variants — arm64-v8a + armeabi-v7a + x86_64). On first use, `LspServerInstaller` extracts the executable from the DFM asset path to the app's writable storage (e.g., `context.filesDir/lsp-servers/<lang>/`), `chmod +x`'s it, then spawns via `ProcessBuilder`. **Not** placed in `jniLibs/` — those are reserved for `System.loadLibrary`-style `.so` libraries (LSP servers are standalone executables, not libraries). Battery cost (50–200 MB RAM per active server) mitigated by 5-min idle shutdown. | Full support. |
| **Desktop macOS-arm64** | Binaries shipped in DMG bundle resources. `BundledLspExtractor` copies to `~/.yole/lsp-servers/<lang>/` on first launch. Process spawn via `ProcessBuilder`. | Full support. |
| **Desktop Linux-x64** | Same pattern + tarball + `~/.local/share/yole/lsp-servers/<lang>/`. Blocked on `#crossbuild-linux-windows-infra` until that unblocks. | Deferred at distribution time, code-ready. |
| **Desktop Windows-x64** | Same as Linux + `.exe` extensions. Same blocker. | Deferred at distribution time, code-ready. |
| **iOS** | Subprocess spawn forbidden by App Store policy. `LspCompletionProvider.ios.kt` returns `emptyList()`. | Hard-blocked indefinitely. Document in user-guide + KNOWN_DEFECTS.md. |
| **Web Wasm** | Native LSP binaries can't run in browser. WASM-LSP ecosystem too immature (2026) to make primary strategy. Stub returns `emptyList()`. | Deferred. Re-evaluate in a future iteration. |

## 8. Deep-research checklist

Phase 0 equivalent. Output: `docs/features/lsp/research-report.md`.

1. **LSP4J Android compatibility** — does the latest LSP4J compile and run cleanly under Android's restricted JVM? Spike: minimal LSP4J client on Android Robolectric harness.
2. **jdtls offline-first feasibility** — `--offline` mode? Frozen-snapshot strategy? Document honestly if jdtls is online-only.
3. **Android DFM size limits + UX** — Play Store AAB constraint: 200 MB base + up to 1 GB total DFMs. Per-DFM install UX flow.
4. **Cold-start time measurements** — rust-analyzer + gopls on macOS-arm64 + Android Pixel (if available). Inform user-guide cold-start prose.
5. **clangd C+C++ shared binary** — confirm one DFM serves both langIds without per-language config divergence.
6. **TypeScript + Node bundling** — Node single-binary size per ABI. Whether 3 Node-based servers (typescript-language-server, bash-language-server, yaml-language-server) can share one Node DFM.
7. **iOS skip rationale documentation** — authoritative current App Store policy reference for subprocess prohibition.
8. **LSP4J version pinning** — current latest + LTS branch.

Output requirements: ≥ 600 lines, ≥ 100 URL citations, all gaps marked OPEN with explicit spike output expectation.

## 9. Testing strategy (anti-bluff)

### 9.1 Unit tests (desktopTest — LSP4J is JVM-only)

1. `LspServerRegistryTest` — every spec parseable + 15 entries + per-langId lookup. Mutation: drop entry → FAIL.
2. `LspWorkspaceResolverTest` — table-driven over 6 markers; synthesized tmp dir trees. Mutation: stub resolve → FAIL.
3. `LspServerSpecTest` — JSON round-trip per spec. Mutation: serializer swallows fields → FAIL.
4. `LspServerHostTest` — fake LSP server (tiny Kotlin coroutine echoing minimal LSP responses). Cold spawn → alive; 5-min idle shutdown (fake time source); restart-after-crash. Mutation: skip spawn → FAIL.
5. `LspCompletionProviderTest` — feeds `CompletionContext` + stubbed `LspServerHost` returning canned LSP items; asserts mapping correct. Mutation: provider always emptyList → FAIL.
6. `LspCompletionProviderStubTest` (iosMain/wasmJsMain compilation parity — verified via `:shared:compile*` succeeding) — asserts stub returns `emptyList()` + does NOT throw. Mutation: stub throws → FAIL.

### 9.2 Integration tests (desktopTest, against real bundled binaries)

7. `RealServerSmokeTest` — for each of 3 representative servers (gopls, rust-analyzer, marksman): spawn real binary, init handshake, send `textDocument/completion` against known file, assert ≥ 1 completion. **Constraint:** only runs if binaries present in `~/.yole/lsp-servers/<lang>/`; otherwise skipped via JUnit `assumeTrue` with explicit `// SKIP-OK: requires bundled LSP binaries`.
8. `LspCompletionEngineParityTest` — extends iter-60 Phase 4 parity test; asserts `LspCompletionProvider` in `CompletionEngine.default()`. Mutation: remove → FAIL.

### 9.3 Robolectric (androidApp)

9. `LspCompletionPopupRobolectricTest` — wires fake `LspServerHost`; opens .rs file, types 3 chars; asserts popup shows ≥ 1 LSP-kind item via testTag. Mutation: stub host empty → FAIL.
10. `LspMissingDfmRobolectricTest` — simulate DFM-not-installed; asserts "Tap to install" UX appears.

### 9.4 Structural anti-bluff

11. `LspServerBundleCompletenessTest` — every `LspServerRegistry` entry has corresponding `lsp-servers/<langId>/server.json` resource. Mutation: drop server.json → FAIL.

### 9.5 Challenges (Phase 9, wired into `make qa-all`)

12. `lsp_hosting_completeness_challenge.sh` — STATIC: 15 server.json + 11 test classes exist + parity test PASS. RUNTIME: `:shared:desktopTest --tests "digital.vasic.yole.lsp.*"` ≥ 25 PASSED.
13. `lsp_binary_bundle_challenge.sh` — for each shipped ABI, extract APK/DMG and `file <binary>` to confirm ABI. Assert ≥ 1 full ABI complete (likely macos-arm64 in v1, mirroring iter-58 Phase 7 pivot).

Every test mutation-verified per CONST-035; mutation procedure captured in each test class's KDoc.

## 10. Phase breakdown

| Phase | Scope | TDD depth |
|---|---|---|
| 0 | Deep research → `docs/features/lsp/research-report.md` | Research only |
| 1 | `LspServerSpec` + `LspServerRegistry` + 15 server.json | Full TDD |
| 2 | `LspWorkspaceResolver` | Full TDD |
| 3 | `LspServerInstaller` + per-platform actuals | Full TDD |
| 4 | `LspServerHost` (LSP4J wiring + lazy spawn + idle shutdown + restart) | Full TDD with fake LSP server |
| 5 | `LspCompletionProvider` (commonMain interface + JVM actual + iOS/Wasm stubs) | Full TDD |
| 6 | Wire into `CompletionEngine.default(...)` + parity test extension | Task-level |
| 7 | Acquire/build 15 LSP binaries × per-ABI matrix (pivot to Maven Central / GitHub releases where possible) | Task-level + smoke |
| 8 | Android DFM packaging + install-on-demand UX in Settings → LSP screen | Task-level + Robolectric |
| 9 | 2 anti-bluff challenges + `qa-iter-61-gates` chained into `qa-all` | Full TDD on shell scripts |
| 10 | Documentation | Authoring only |
| 11 | Firebase distribution v1.4.0 | Mechanical |

## 11. Documentation deliverables

- `docs/features/lsp/user-guide.md` — per-platform trigger story, 15 supported languages, DFM install flow on Android, jdtls online-only caveat, deferral status for Linux/Windows/iOS/Web.
- `docs/features/lsp/architecture.md` — components, provider plug-in pattern (mirroring iter-60's prep for 4b/4c).
- `docs/features/lsp/lsp-coverage-matrix.md` — 15 langs × {bundled, runtime requirement, project markers, init options}.
- `docs/features/lsp/research-report.md` — Phase 0 output.
- CHANGELOG.md iter-61 entry + version 1.3.0 → 1.4.0 bump.
- docs/CONTINUATION.md per CONST-036.

## 12. Firebase distribution

Same pattern as iter-60 v1.3.0:
- Android Release + Debug + DEV: AAB with DFMs distributed via Firebase App Distribution.
- Desktop macOS-arm64: DMG staged in `releases/`.
- Linux/Windows: pending `#crossbuild-linux-windows-infra`.
- Web Wasm: pending `#wasmjs-production-distribution-gap` (and LSP fundamentally not available on Web).
- iOS: hard-blocked.

Version target: **1.4.0 / versionCode 140 / dotted `0.0.0.1.40`**.

## 13. Out of scope for 4a

- Diagnostics rendering UI → 4b.
- Hover popup → 4b.
- Go-to-definition navigation → 4b.
- Rename, code actions, signature help, formatting → 4c.
- User-supplied LSP server config / PATH discovery → defer.
- Workspace folder selection UI beyond auto walk-up → 4b/4c.
- LLM-driven completions → future feature, not part of LSP arc.

## 14. Open questions for the implementation plan

- LSP4J on Android: any reflection/desugaring issues? Phase 0 spike resolves.
- jdtls offline: fully bundlable, or document as online-only? Phase 0 §2 resolves.
- Per-DFM size: confirm each language's bundle fits under Play Store DFM size constraints. Phase 0 §3.
- Node-bundle sharing across 3 Node-based servers: Phase 0 §6 confirms.

## 15. Forensic anchor

Brainstorm session 2026-05-15 with operator. Operator chose at every round:

1. Iteration decomposition = "4a / 4b / 4c".
2. Server distribution = "Bundle everything for a curated set" (heaviest option).
3. Bundled language list = "Maximalist 15 including JVM heavies".
4. LSP client library = "Eclipse LSP4J".
5. Process model = "Lazy spawn + idle shutdown after 5 min".
6. Workspace root = "Walk up looking for project markers".

No defaults selected; every choice explicit.

---

**Next steps after operator review:**

1. Operator reviews this spec + requests any changes.
2. Invoke `superpowers:writing-plans` to produce `docs/superpowers/plans/2026-05-15-lsp-plan.md`.
3. Plan begins with Phase 0 deep-research (§8) before any code lands.
4. Implementation phases follow with bite-sized TDD, mutation-verified anti-bluff tests, CONST-037 cross-platform-impact tracking, and Firebase distribution at the end.
