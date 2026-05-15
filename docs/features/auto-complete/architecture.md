<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Auto-Complete — Architecture

> **Audience:** future Yole contributors. Read after `user-guide.md`.
> Special note for the **Feature 4 (LSP)** author: §3 is written for you.

This document describes the iter-60 auto-complete pipeline: package layout, provider contract, engine flow, ranker boost rules, per-platform disposition, and the anti-bluff invariants that protect each claim.

---

## 1. Pipeline overview

```
User keystrokes (debounced 80 ms) or Ctrl+Space / toolbar button
    │
    ▼
CompletionTrigger          (shared/src/commonMain/.../completion/trigger/)
  - PrefixGuard: prefix ≥ 2 chars (implicit) OR explicit signal
  - Debounces keystrokes; skips inside string literals when scope known
    │
    ▼
CompletionContext           (.../completion/CompletionContext.kt)
  - text: String            (full document text)
  - cursor: Int             (byte offset)
  - prefix: String          (word fragment under cursor)
  - langId: String?         (null for plaintext)
  - surroundingScope: String? (Tree-Sitter node type; null when unavailable)
    │
    ▼
CompletionEngine.complete(ctx): Flow<List<CompletionItem>>
  - Fans out to ALL providers concurrently (Dispatchers.Default)
  - Per-provider timeout: 500 ms (TimeoutCancellationException caught silently)
  - Emits progressive results as each provider completes
    │
    ├──▶ TokenFrequencyProvider  → words already in doc, ranked by frequency
    ├──▶ SnippetProvider         → bundled VS Code snippets for langId
    └──▶ IdentifierProvider      → outline symbols (functions, classes, vars)
              │
              ▼
         CompletionRanker.merge(accumulated, scope)
           - ScopeAwareRanker.boost(item, scope) applied per item
           - Deduplicate by label; sort descending by final score
              │
              ▼
         Popup / caller
           - Android: CompletionPopup (LazyColumn, max 8 rows)
           - Desktop/iOS/Web: engine available; popup UI deferred (see §6)
```

---

## 2. Provider interface

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionProvider.kt

interface CompletionProvider {
    val id: String
    suspend fun complete(ctx: CompletionContext): List<CompletionItem>
}
```

Implementations MUST be thread-safe and MUST NOT block. Return an empty list on any failure — the engine degrades gracefully without surfacing provider errors to the user (CONST-035).

### Three v1 providers

| Provider | `id` | What it does |
|---|---|---|
| `TokenFrequencyProvider` | `token-frequency` | Splits the document on `[A-Za-z_][A-Za-z0-9_]*`, counts occurrences, filters by prefix, returns descending by frequency. Excludes the cursor word (the partial word being typed). |
| `SnippetProvider` | `snippet` | Calls `SnippetRegistry.forLanguage(langId)`, filters snippets whose prefix starts with `ctx.prefix`. Returns empty when `langId` is null. |
| `IdentifierProvider` | `identifier` | Calls `OutlineExtractor.extract(text, langId)` to get function/class/variable symbols, then filters by prefix. Requires a `TokenizerEngine` instance. |

---

## 3. How to add a new provider (Feature 4 prep)

1. **Implement `CompletionProvider`** in `shared/src/commonMain/kotlin/digital/vasic/yole/completion/providers/`:
   ```kotlin
   class LspProvider(private val lspClient: LspClient) : CompletionProvider {
       override val id = "lsp"
       override suspend fun complete(ctx: CompletionContext): List<CompletionItem> { … }
   }
   ```

2. **Wire it into `CompletionEngine.default(…)`** in `CompletionEngine.kt`:
   ```kotlin
   fun default(extractor: OutlineExtractor, engine: TokenizerEngine, lsp: LspClient?): CompletionEngine =
       CompletionEngine(
           providers = listOfNotNull(
               TokenFrequencyProvider(),
               SnippetProvider(),
               IdentifierProvider(extractor, engine),
               lsp?.let { LspProvider(it) },
           ),
       )
   ```

3. **`CompletionEngineParityTest` will FAIL until you add the provider.**
   This is the structural anti-bluff gate — it verifies the production engine's provider list matches a known set. Update the expected set in `CompletionEngineParityTest` when you add yours.

4. **Add a unit test in `commonTest`** mirroring the 3 existing provider tests:
   ```
   shared/src/commonTest/kotlin/digital/vasic/yole/completion/
   ├── TokenFrequencyProviderTest.kt
   ├── SnippetProviderTest.kt
   └── IdentifierProviderTest.kt
   ```
   Your test must include mutation evidence (stub the provider body → relevant test FAILS).

5. The LSP provider in Feature 4 will live here. The engine's channelFlow fan-out means LSP latency does not block the token-frequency or snippet results; users see fast partial results while LSP is in-flight.

---

## 4. How to add a new language's snippet bundle

1. Create `shared/src/commonMain/resources/snippets/<langId>/snippets.json`.
2. Use the VS Code snippet schema:
   ```json
   {
     "Snippet Name": {
       "prefix": "fn",
       "body": ["fun ${1:name}(${2:params}): ${3:Unit} {", "    $0", "}"],
       "description": "Function declaration"
     }
   }
   ```
3. **v1 limitations** (do not use or these fields will silently be ignored):
   - `$VARIABLE` tokens (e.g., `$TM_FILENAME`) — rendered as literal text.
   - `${N|a,b,c|}` choice-list syntax — parsed but the UI shows a plain text field.
4. The `snippet_library_bundle_challenge.sh` will immediately validate the new JSON and report it in the bundle count.
5. Add the language to the snippet-coverage-matrix.md (regenerate from the source tree — see §7).

---

## 5. Ranker boost rules

`ScopeAwareRanker` applies a Double boost to each `CompletionItem.score` based on the Tree-Sitter `surroundingScope` at the cursor and the item's `Kind`. The final score is `item.score + boost`.

| `surroundingScope` | `CompletionItem.Kind` | Boost |
|---|---|---|
| `member_access` | `Identifier` | +2.0 |
| `member_access` | anything else | 0.0 |
| `type_annotation` | `Identifier` | +1.5 |
| `type_annotation` | anything else | 0.0 |
| `string_literal` | any | −3.0 |
| `null` (Tree-Sitter unavailable) | any | 0.0 |
| any other scope | any | 0.0 |

The ranker is stateless (`object ScopeAwareRanker`). Adding new scope rules is a single `when` branch in `ScopeAwareRanker.boost`.

---

## 6. Per-platform notes

### Android

Full implementation. `CompletionPopup`, `CompletionPopupState`, `CompletionToolbarButton`, and the `SyncedScrollEditor` wiring all live in `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/`. Robolectric tests for the popup UI are in `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/`.

### Desktop

The completion engine, all three providers, the snippet bundles, and `SnippetPlaceholderNavigator` compile and run on Desktop via `commonMain`. The `CompletionTrigger` is also `commonMain`. Only the `CompletionPopup` composable is Android-only for v1 — it is in `androidApp/` and not yet ported to `desktopApp/`. The engine works today: `CompletionEngineTest` and all provider tests pass under `:shared:desktopTest`. Desktop popup UI is a follow-up.

### iOS

The engine, providers, snippet registry, and trigger compile for iOS (`commonMain`). The `SnippetRegistry.ios.kt` actual returns `null` for resource reads (NSBundle wiring not yet implemented — `#shared-iosmain-databasefactory-broken` from iter-57 / iter-58 blocks this). The popup UI is deferred for the same reason. Snippet bundles are bundled in the resource directory and will be accessible once the NSBundle actual is wired.

### Web (Wasm)

Same as iOS: engine + providers compile for Wasm. `SnippetRegistry.wasmJs.kt` returns `null` (fetch-based access not yet wired — `#wasmjs-production-distribution-gap` from iter-57). Popup UI deferred.

---

## 7. Anti-bluff invariants

Two challenges gate the feature in `make qa-all` (via `qa-iter-60-gates`):

### `yole-challenges/scripts/auto_complete_completeness_challenge.sh`

- **Static layer:** asserts that the 12 foundation source files exist (`CompletionItem.kt`, `CompletionContext.kt`, `CompletionProvider.kt`, `CompletionEngine.kt`, `CompletionRanker.kt`, `ScopeAwareRanker.kt`, `CompletionTrigger.kt`, `Snippet.kt`, `VsCodeSnippetParser.kt`, `SnippetRegistry.kt`, `SnippetPlaceholderNavigator.kt`, and one more), and that the engine file references all 3 provider class names.
- **Runtime layer:** runs `:shared:desktopTest --tests "digital.vasic.yole.completion.*"` and asserts ≥50 PASSED, 0 FAILED.

### `yole-challenges/scripts/snippet_library_bundle_challenge.sh`

- **Static layer:** finds all `shared/src/commonMain/resources/snippets/*/snippets.json` files, validates each with `python3 -m json.tool`, and asserts ≥50 bundles.
- **Runtime layer:** runs `SnippetBundleCompletenessTest`, `SnippetRegistryTest`, and `VsCodeSnippetParserTest` and asserts ≥10 PASSED.

If you add a new language bundle, the snippet challenge will include it automatically in the static count. If you add a new provider, `CompletionEngineParityTest` will FAIL until the expected set is updated — this is the intended gate.

---

## 8. Cross-platform impact (CONST-037)

Every change to this feature MUST be evaluated against all four targets before coding:

- **Android:** full pipeline + popup UI shipped.
- **Desktop:** engine + snippets available; popup UI follow-up.
- **iOS:** engine + snippets compile; NSBundle resource wiring + popup UI deferred until `#shared-iosmain-databasefactory-broken` clears.
- **Web (Wasm):** engine + snippets compile; fetch-based resource wiring + popup UI deferred until `#wasmjs-production-distribution-gap` resolves.

Changes to `commonMain` (engine, providers, ranker, trigger, snippet schema) affect all four targets. Changes to `androidApp/` affect Android only. Any cross-platform divergence must be documented in the commit body per CONST-037.

---

## Package layout

```
shared/src/commonMain/kotlin/digital/vasic/yole/completion/
├── CompletionContext.kt
├── CompletionEngine.kt
├── CompletionItem.kt
├── CompletionProvider.kt
├── CompletionRanker.kt
├── ScopeAwareRanker.kt
├── providers/
│   ├── TokenFrequencyProvider.kt
│   ├── SnippetProvider.kt
│   └── IdentifierProvider.kt
├── snippet/
│   ├── Snippet.kt
│   ├── SnippetParseException.kt
│   ├── SnippetPlaceholderNavigator.kt
│   ├── SnippetRegistry.kt        (expect)
│   └── VsCodeSnippetParser.kt
└── trigger/
    └── CompletionTrigger.kt

shared/src/{android,desktop,ios,wasmJs}Main/kotlin/digital/vasic/yole/completion/snippet/
└── SnippetRegistry.<platform>.kt  (actual)

shared/src/commonMain/resources/snippets/
└── <langId>/snippets.json         (55 bundles)

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/
├── CompletionPopup.kt
├── CompletionPopupState.kt
└── CompletionToolbarButton.kt
```
