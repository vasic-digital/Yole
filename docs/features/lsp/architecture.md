<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LSP Integration — Architecture

> **Audience:** Yole contributors and Feature 4b/4c plug-in authors.
> Read `docs/features/auto-complete/architecture.md` first — this document builds on it.

iter-61 wires `LspCompletionProvider` into the iter-60 `CompletionEngine` as the fourth provider. The rest of the completion pipeline (trigger, ranker, popup) is unchanged.

---

## 1. Pipeline overview

```
User keystrokes (debounced 80 ms) or Ctrl+Space / toolbar button
    │
    ▼
CompletionContext  (.../completion/CompletionContext.kt)
  Fields added in iter-61:
    documentUri: String?     (file URI fed to LSP textDocument/didOpen)
    workspaceRoot: String?   (resolved by LspWorkspaceResolver)
    │
    ▼
CompletionEngine.complete(ctx): Flow<List<CompletionItem>>
  Fans out to ALL 4 providers concurrently (Dispatchers.Default)
  Per-provider timeout: 500 ms
    │
    ├──▶ TokenFrequencyProvider   (iter-60 — words in doc)
    ├──▶ SnippetProvider          (iter-60 — VS Code snippets)
    ├──▶ IdentifierProvider       (iter-60 — outline symbols)
    └──▶ LspCompletionProvider    (iter-61 — type-aware completions)
              │
              ▼
         LspServerHost  (.../lsp/LspServerHost.<platform>.kt)
           Desktop/Android actual: ProcessBuilder spawn
             ├── LspWorkspaceResolver  (walk-up project marker finder)
             ├── LspServerInstaller    (per-platform binary path resolver)
             └── LSP4J 1.0.0 JSON-RPC over stdin/stdout
                   ├── initialize / initialized handshake
                   ├── textDocument/didOpen (on first call for URI)
                   └── textDocument/completion → CompletionList
              │
              ▼
         LspCompletionLine / LspCompletionResult
           (mapped to CompletionItem by LspCompletionProvider)
              │
              ▼
    CompletionRanker.merge(accumulated, scope)
      ScopeAwareRanker.boost() applied (same rules as iter-60)
      Deduplication by label, sort descending by score
              │
              ▼
    Popup (Android: CompletionPopup LazyColumn)
```

The LSP path adds latency (server cold-start + JSON-RPC round-trip) but does not block the other three providers. Token and snippet results appear within the first 500 ms debounce window; LSP results appear in a subsequent emission once the server responds.

---

## 2. Provider interface contract

`LspCompletionProvider` implements the same `CompletionProvider` interface shipped in iter-60:

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionProvider.kt
interface CompletionProvider {
    val id: String
    suspend fun complete(ctx: CompletionContext): List<CompletionItem>
}
```

`LspCompletionProvider` is an `expect` class with platform-specific `actual` implementations:

| Source set | Behaviour |
|---|---|
| `desktopMain` | Full implementation via `LspServerHost.desktop.kt` |
| `androidMain` | Full implementation via `LspServerHost.android.kt` (returns empty until binaries land) |
| `iosMain` | Honest stub — always returns `emptyList()` |
| `wasmJsMain` | Honest stub — always returns `emptyList()` |

`id = "lsp"`. The engine fans it out in parallel with the other three providers; a 500 ms `withTimeout` wraps the call so a slow server never blocks the popup.

---

## 3. LspServerHost internals (Desktop/Android JVM)

`LspServerHost` owns the lifecycle of one server process per language ID. It is an `expect` class; the JVM `actual` uses:

1. **`LspServerInstaller`** — resolves the server binary path from staged resources (`lsp-bundles/<langId>/<exe>` in the Desktop classpath). Returns `InstallerResult.NotInstalled` on Android until binaries land.
2. **`LspWorkspaceResolver`** — walks up from the open file's directory looking for project markers listed in `server.json`. Returns the first matching parent (max 20 levels). Falls back to the file's directory.
3. **`ProcessBuilder`** — spawns the server executable with args from `LspServerSpec.args`. stdin/stdout piped.
4. **LSP4J `Launcher.createLauncher`** — wraps the process streams with JSON-RPC. Provides a typed `LanguageServer` proxy.
5. **`initialize` / `initialized` handshake** — sent once on first use with a 30 s timeout. Failure transitions the server to `ServerState.Error`.
6. **`textDocument/didOpen`** — sent the first time a given URI is seen in this server session.
7. **`textDocument/completion`** — the actual completion request. 500 ms timeout per call.
8. **Idle shutdown ticker** — 60 s tick; if no requests arrive within 5 minutes, the server process is killed. The next request restarts it transparently.
9. **`Mutex` serialisation** — all server interactions are serialised to prevent concurrent lifecycle transitions from racing.
10. **`CancellationException` rethrow** — always rethrown in catch blocks (CONST-035 coroutine safety rule).

`ServerState` sealed class:

```kotlin
sealed class ServerState {
    object Idle : ServerState()
    object Starting : ServerState()
    object Ready : ServerState()
    data class Error(val cause: Throwable) : ServerState()
}
```

---

## 4. CompletionEngine wiring

`CompletionEngine.default()` was extended in iter-61 to accept an optional `lspHost` parameter:

```kotlin
fun default(
    extractor: OutlineExtractor,
    engine: TokenizerEngine,
    lspHost: LspServerHost? = null,
): CompletionEngine = CompletionEngine(
    providers = listOfNotNull(
        TokenFrequencyProvider(),
        SnippetProvider(),
        IdentifierProvider(extractor, engine),
        lspHost?.let { LspCompletionProvider(it) },
    ),
)
```

`CompletionEngineParityTest` was bumped to require ≥ 4 providers. Any future provider addition MUST update this test to avoid an accidental silent-drop.

---

## 5. How to add a new LSP server

Follow these four steps when contributing a new language server (iter-62/63 additions):

1. **Add `langId` to `LspServerRegistry.BUNDLED_LANG_IDS`** in `shared/src/commonMain/kotlin/digital/vasic/yole/lsp/LspServerRegistry.kt`.

2. **Author `shared/src/commonMain/resources/lsp-servers/<langId>/server.json`**:

   ```json
   {
     "langIds": ["<langId>"],
     "executable": "<server-binary-name>",
     "args": ["--stdio"],
     "projectMarkers": ["<marker-file>"],
     "initOptions": {}
   }
   ```

   `langIds` may contain multiple IDs (e.g., `["c", "cpp"]` for `clangd`). `projectMarkers` supports glob patterns (e.g., `"*.cabal"`).

3. **Update `scripts/acquire-lsp-binaries.sh`** — add download URL and extraction steps for the new binary for each supported ABI (`macos-arm64`, `linux-x64`, `windows-x64`, `android-arm64`). Mirror the existing `rust-analyzer` or `marksman` stanzas.

4. **Bump the iter-61 Phase 7 challenge thresholds** in `yole-challenges/scripts/lsp_binary_acquisition_challenge.sh` — increment `MIN_LANGS` by 1. The challenge will FAIL until the binary is staged.

---

## 6. How to add a new LSP capability (e.g., textDocument/hover for iter-62)

LSP capabilities beyond completion are not wired in v1. To add a new one:

1. **Add a `suspend fun` to `LspServerHost`** (both `expect` and JVM `actual`):

   ```kotlin
   // In LspServerHost.desktop.kt
   suspend fun hover(uri: String, line: Int, character: Int): String? {
       val result = server?.textDocumentService?.hover(...) ?: return null
       return result.get()?.contents?.toString()
   }
   ```

2. **The consumer (UI or provider) calls the new method directly.** No changes to `CompletionProvider` or `CompletionEngine` are required — hover is a separate concern from completion.

3. **LSP4J's `LanguageServer.textDocumentService`** exposes typed methods for every standard LSP request (`hover`, `definition`, `references`, `formatting`, `signatureHelp`, etc.). Each method returns a `CompletableFuture<T>` which must be bridged to a Kotlin coroutine via `.await()` (from `kotlinx-coroutines-jdk8`).

4. **Add tests in `desktopTest`** mirroring `LspServerHostTest` degradation tests for the no-spec path, and a `RealServerSmokeTest` stanza for marksman or another fast-starting server.

---

## 7. Per-platform notes

### Desktop macOS-arm64

Full implementation. `lspBundleStage` Gradle task copies binaries from `.lsp-binary-cache/<langId>/macos-arm64/` into `processedResources/desktop/main/lsp-bundles/<langId>/` at build time. `LspServerInstaller.desktop.kt` resolves the classpath path at runtime.

### Desktop Linux / Windows

`LspServerHost.desktop.kt` is platform-agnostic JVM code. The binary acquisition and classpath staging are identical in structure; the ABIs differ. Distribution is gated on `#crossbuild-linux-windows-infra`.

### Android

`LspServerHost.android.kt` is the same JVM code as Desktop. `LspServerInstaller.android.kt` currently returns `NotInstalled` for all servers — no binaries are staged for Android in v1. The `LspSettingsScreen` (Settings → Language Servers) renders a "Not available on Android (v1)" status row for each of the 15 servers. JVM-bundle servers (jdtls, kotlin-language-server) will use a shared `lsp-jvm-runtime` DFM (~50 MB JRE); Node-bundle servers (pyright, typescript-language-server, bash-language-server, yaml-language-server) will use a Node runtime DFM. Both DFMs are planned for Phase 8b.

### iOS

`LspServerHost.ios.kt` and `LspCompletionProvider.ios.kt` return no-ops / empty lists. This is the only correct behaviour given App Store Review Guideline 2.5.2. No further iOS LSP work is planned unless Apple changes the guideline.

### Web (Wasm)

`LspServerHost.wasmJs.kt` and `LspCompletionProvider.wasmJs.kt` return no-ops / empty lists. Running native binaries in a browser sandbox is not possible without WASI / Wasm workers, which are deferred.

---

## 8. Anti-bluff invariants

Two challenges gate the feature in `make qa-all` (via `qa-iter-61-gates`):

### `yole-challenges/scripts/lsp_binary_acquisition_challenge.sh`

- **Static layer:** asserts `server.json` files exist for ≥ 15 language IDs; asserts `.lsp-binary-cache/<langId>/macos-arm64/` directories exist for ≥ 8 languages.
- **Runtime layer:** runs `LspServerRegistryTest` and `LspWorkspaceResolverTest` and asserts ≥ 10 PASSED.

### `yole-challenges/scripts/lsp_completion_provider_challenge.sh`

- **Static layer:** asserts `LspCompletionProvider.kt` (expect) exists; asserts all 4 platform actuals exist; asserts `CompletionEngineParityTest.kt` references provider count ≥ 4.
- **Runtime layer:** runs `LspCompletionProviderTest` + `CompletionEngineParityTest` and asserts ≥ 8 PASSED, 0 FAILED.

### `LspCompletionProvider` parity assertion

`CompletionEngineParityTest` contains a minimum provider count assertion (`≥ 4`). Removing or failing to wire a provider causes this test to FAIL — the intended structural anti-bluff gate inherited from iter-60.

---

## 9. Package layout

```
shared/src/commonMain/kotlin/digital/vasic/yole/
├── lsp/
│   ├── LspServerSpec.kt            (data class: langIds, executable, args, projectMarkers, initOptions)
│   ├── LspServerRegistry.kt        (BUNDLED_LANG_IDS + registry of LspServerSpec per langId)
│   ├── LspWorkspaceResolver.kt     (walk-up project-marker finder, okio-based, MAX_LEVELS=20)
│   └── LspServerHost.kt            (expect class: ServerState, LspCompletionResult, LspCompletionLine)
│
└── completion/
    ├── CompletionContext.kt         (+ documentUri?, workspaceRoot? added in iter-61)
    └── providers/
        └── LspCompletionProvider.kt (expect class)

shared/src/desktopMain/kotlin/digital/vasic/yole/
├── lsp/
│   ├── LspServerHost.desktop.kt    (ProcessBuilder + LSP4J + idle ticker + Mutex)
│   └── LspServerInstaller.desktop.kt (classpath binary resolver)
└── completion/providers/
    └── LspCompletionProvider.desktop.kt (delegates to LspServerHost, maps LspCompletionLine → CompletionItem)

shared/src/androidMain/kotlin/digital/vasic/yole/
├── lsp/
│   ├── LspServerHost.android.kt    (same JVM body; installer returns NotInstalled)
│   └── LspServerInstaller.android.kt (returns NotInstalled in v1)
└── completion/providers/
    └── LspCompletionProvider.android.kt (delegates; returns empty until binaries land)

shared/src/iosMain/kotlin/digital/vasic/yole/
├── lsp/LspServerHost.ios.kt        (honest stub)
└── completion/providers/LspCompletionProvider.ios.kt (emptyList)

shared/src/wasmJsMain/kotlin/digital/vasic/yole/
├── lsp/LspServerHost.wasmJs.kt     (honest stub)
└── completion/providers/LspCompletionProvider.wasmJs.kt (emptyList)

shared/src/commonMain/resources/lsp-servers/
└── <langId>/server.json            (15 files: bash, c, cpp, elixir, go, haskell,
                                      java, kotlin, lua, markdown, python, rust,
                                      typescript, yaml, zig)

.lsp-binary-cache/                  (gitignored; staged by scripts/acquire-lsp-binaries.sh)
└── <langId>/macos-arm64/<exe>      (8 langs staged in Phase 7)

androidApp/src/main/java/digital/vasic/yole/android/ui/settings/
└── LspSettingsScreen.kt            (Compose screen: 15 server rows, v1 disclaimer)
```

---

## 10. Cross-platform impact (CONST-037)

Every change to this feature MUST be evaluated against all four targets before coding:

- **Android:** LSP infrastructure present; Settings UX ships the v1 disclaimer. Completions return empty until binaries + runtime DFMs land.
- **Desktop macOS-arm64:** full pipeline shipped end-to-end; 8 servers bundled.
- **Desktop Linux / Windows:** code identical to macOS; binary distribution gated.
- **iOS:** honest `emptyList()` stubs; hard-blocked by App Store §2.5.2.
- **Web (Wasm):** honest `emptyList()` stubs; native subprocess not possible in browser sandbox.

Changes to `commonMain` LSP code (`LspServerSpec`, `LspServerRegistry`, `LspWorkspaceResolver`, `LspServerHost` expect, `LspCompletionProvider` expect, `CompletionContext` fields) affect all four platform compilations. Changes must include a "Cross-platform impact" block in the commit body per CONST-037.
