# Auto-Complete — Design Spec (Feature 3 of 5)

> **Status:** Brainstorm complete; awaiting operator review before invoking writing-plans.
> **Author:** brainstormed 2026-05-15 with operator.
> **Sequence:** Feature 3 of 5 in the editor-capability initiative. Feature 1 (syntax highlighting) shipped iter-57 v1.1.0. Feature 2 (source-code file support) shipped iter-58 v1.2.0. iter-59 added DEV variant. Feature 3 follows in dependency order; LSP-fed completion (Feature 4) will plug into this scaffold without re-architecting.

---

## 1. Goal

Add auto-complete to Yole's editor. When the user types in any of the 55+ supported languages, a dropdown of relevant completions appears — mixing four sources: file-local words, per-language snippet libraries, identifiers extracted from the file (via Feature 2's `OutlineExtractor`), and Tree-Sitter context-aware ranking.

The result is a lightweight-IDE feel without depending on a Language Server. Feature 4 (LSP integration) will later add a 5th provider for semantic completions; the v1 architecture is provider-based specifically to make that addition a drop-in.

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Completion sources | **Comprehensive** — token-frequency + snippets + identifier-aware + scope-aware ranking. |
| Trigger model | **Implicit (after 2 chars) + explicit (Ctrl+Space) on Desktop/Web**; toolbar "Suggest" button on Android/iOS. |
| Snippet library | **Vendored from VS Code's `microsoft/vscode/extensions/<lang>-basics/snippets/` MIT-licensed bundles + user-extensible JSON**. ~50+ languages covered out of the box. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific content leaks into sibling submodules.

---

## 3. Architecture: provider-based completion pipeline

```
shared/src/commonMain/kotlin/digital/vasic/yole/completion/
├── CompletionEngine.kt           # top-level API; orchestrates providers in parallel
├── CompletionItem.kt             # data class: label, insertText, kind, score, range
├── CompletionContext.kt          # data class: text, cursorByte, langId, prefix, scope
├── CompletionProvider.kt         # interface; all providers implement
├── CompletionRanker.kt           # merges + scores results from providers
├── providers/
│   ├── TokenFrequencyProvider.kt
│   ├── SnippetProvider.kt
│   ├── IdentifierProvider.kt     # consumes Feature 2 OutlineExtractor
│   └── ScopeAwareRanker.kt       # rank-modifier (not a provider per se)
├── snippet/
│   ├── Snippet.kt
│   ├── VsCodeSnippetParser.kt
│   ├── SnippetParseException.kt
│   └── SnippetRegistry.kt
└── trigger/
    └── CompletionTrigger.kt      # debounce + explicit-vs-implicit logic

shared/src/commonMain/resources/snippets/<lang>/snippets.json    # 50+ files

editor surface (androidApp, desktopApp, iosApp, webApp):
├── CompletionPopup.kt
├── CompletionPopupState.kt
└── CompletionToolbarButton.kt    # mobile only
```

**Invariants:**

- `CompletionEngine.complete(context): Flow<List<CompletionItem>>` — single entry point.
- Providers run in parallel; results merge + emit as they arrive (fast providers show before slow).
- Provider interface is universal: Feature 4's `LspCompletionProvider` plugs in without architectural change.
- Trigger logic isolated in `CompletionTrigger`; the editor doesn't know about debounce or prefix-guards.

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `CompletionItem` | `label`, `insertText`, `kind` (Identifier/Snippet/Keyword/Word), `score: Double`, `range: IntRange`. | many per popup |
| `CompletionContext` | Snapshot: text, cursorByte, langId, prefix, surroundingScope (Tree-Sitter node-at-cursor). | 1 per request |
| `CompletionProvider` (interface) | `suspend fun complete(ctx): List<CompletionItem>`. | 3 impls in v1 |
| `TokenFrequencyProvider` | Word frequency in current file (excluding cursor word). Boost rare-but-recent. | 1 |
| `SnippetProvider` | Looks up bundled + user snippets matching prefix for `ctx.langId`. | 1 |
| `IdentifierProvider` | Calls `OutlineExtractor.outlineFor(text, langId, engine)` → outline items become candidates. | 1 |
| `ScopeAwareRanker` | Tree-Sitter node-at-cursor inspection → boosts items by kind (after `.` → method preference; after `:` → type preference). Not a provider — a rank-modifier. | 1 |
| `CompletionRanker` | Merges results from all providers, applies `ScopeAwareRanker` boosts, dedupes, sorts by score. | 1 |
| `Snippet` | `prefix`, `body` (with `${N:placeholder}` markers), `description?`. | 50+ langs × ~15 = ~750 |
| `VsCodeSnippetParser` | Parses VS Code `snippets.json` schema (kotlinx-serialization). | 1 |
| `SnippetRegistry` | Lazy per-lang snippet load; cache; scans bundled + user directory. | 1 |
| `CompletionTrigger` | Debounce 80ms, char-count guard (≥ 2 or explicit), explicit-trigger handling. | 1 |
| `CompletionEngine` | Top-level: builds `CompletionContext`, runs providers in parallel, ranks, emits `Flow<List<CompletionItem>>`. | 1 |
| `CompletionPopup` (Composable) | Dropdown anchored to cursor; arrow keys + Enter/Tab on Desktop; tap-to-select on mobile. | 1 per platform |
| `CompletionPopupState` | `isOpen`, `items`, `selectedIndex`. | 1 |
| `CompletionToolbarButton` (Android/iOS) | Manual trigger button in the editor toolbar. | 1 per mobile platform |

## 5. Data flow

### 5.1 Implicit completion (Desktop + Web)

```
User types a char
  → BasicTextField onValueChange fires
  → CompletionTrigger receives keystroke event
  → debounce 80 ms; if user keeps typing the timer resets
  → after quiescence: check prefix (partial word at cursor)
      if prefix.length < 2: ignore (no popup)
      else: proceed
  → build CompletionContext { text, cursorByte, langId, prefix, surroundingScope }
  → CompletionEngine.complete(ctx):
      ├ launches 3 providers in parallel on Dispatchers.Default:
      │   TokenFrequencyProvider — instant
      │   SnippetProvider        — fast (registry cache)
      │   IdentifierProvider     — slower (Tree-Sitter parse)
      └ as each provider emits, ranker merges + scores
          ├ ScopeAwareRanker boosts items by kind based on surroundingScope
          └ emits Flow<List<CompletionItem>> sorted by score
  → CompletionPopupState.update(items)
  → CompletionPopup renders dropdown anchored at cursor
```

### 5.2 Explicit completion (Ctrl+Space)

Same as 5.1, but skips the prefix-length guard. Fires even on zero-char prefix.

### 5.3 Mobile explicit completion (toolbar tap)

Toolbar `Suggest` button → same as Ctrl+Space.

### 5.4 Snippet expansion

```
User selects a CompletionItem of kind=Snippet
  → Enter (or Tab)
  → CompletionPopup.onCommit(item)
  → if item.kind == Snippet:
      parse item.insertText for ${N:placeholder} markers
      → insert body at cursor; replace partial word with first placeholder
      → mark placeholder ranges for sequential Tab navigation
  → otherwise (Identifier/Keyword/Word): just insert item.insertText replacing the partial word
  → popup closes
```

## 6. Error handling

| Error | Detection | Recovery |
|---|---|---|
| Provider throws during `complete()` | try-catch in CompletionEngine per-provider | Other providers continue; that provider contributes no items this round. Log dev warning. |
| OutlineExtractor times out on huge file | `withTimeout(500 ms)` | IdentifierProvider returns empty; popup shows what other providers gathered. |
| Snippet JSON malformed | `VsCodeSnippetParser.parse()` throws `SnippetParseException` | Skip that snippet file; log + Toast (dev only). Other langs unaffected. |
| Snippet has malformed `${N:placeholder}` syntax | Parser detects, returns Snippet with no placeholders | Item still inserts body text verbatim. Honest degradation. |
| User-supplied snippet directory missing | Filesystem check | Skip user dir; bundled snippets still load. |
| Tree-Sitter unavailable for langId (e.g., Android NDK gap from iter-58 #f2-phase-7-android-ndk-bulk-build-pending) | `engine.tokenize` throws | `ScopeAwareRanker` can't boost; ranker falls back to per-provider scores. Honest. |
| Popup tries to anchor at off-screen cursor | Compose measure pass | Reposition to nearest visible edge OR skip rendering that frame. |
| User dismisses popup (Esc / tap-outside) | Trigger sets `isOpen=false` | Future keystrokes re-evaluate; debounce + prefix gate prevent immediate re-pop. |
| Two snippets share the same `prefix` | Registry detects on load | Both load; ranker treats as competing items; user picks. No silent override. |

## 7. Testing strategy (anti-bluff)

### 7.1 Unit tests (commonTest)

1. `VsCodeSnippetParserTest` — parses Microsoft's `kotlin.json` snippet bundle; asserts known prefixes (`fun`, `class`, `if`) produce expected bodies. Plus malformed-JSON throw case.
2. `SnippetRegistryTest` — loads all bundled snippet files; asserts ≥ 50 langs have ≥ 1 snippet each.
3. `TokenFrequencyProviderTest` — input "foo bar foo baz foo"; prefix "f" → assert "foo" wins on frequency score.
4. `IdentifierProviderTest` — markdown text with 2 headings; prefix "H" → assert both heading names returned via OutlineExtractor.
5. `ScopeAwareRankerTest` — table-driven: cursor after ".", surrounding node `member_access` → method-kind items get +2.0 boost.
6. `CompletionRankerTest` — feed 3 providers' results with overlapping items; assert dedup + score-merge.
7. `CompletionTriggerTest` — debounce + prefix-guard + explicit-trigger logic.

### 7.2 Platform integration (Android Robolectric, desktopTest)

8. `CompletionPopupRobolectricTest` — type 3 chars in editor with Kotlin grammar; assert popup auto-shows with ≥ 1 item.
9. `CompletionExplicitTriggerRobolectricTest` — empty editor, synth Ctrl+Space KeyEvent; popup opens.
10. `MobileSuggestButtonRobolectricTest` — tap the toolbar Suggest button; popup opens.
11. `SnippetExpansionRobolectricTest` — select a snippet item with placeholders; body inserted + cursor at first placeholder.

### 7.3 Per-language smoke

12. `Feature3LanguageSnippetSmokeTest` — parametrized over 50 langs; for each: snippet file parses cleanly + has ≥ 5 snippets. Catches forgot-to-vendor-X.

### 7.4 Anti-bluff structural

13. `CompletionEngineParityTest` — every Provider impl in `providers/` MUST be wired in CompletionEngine.complete. Catches drift.
14. `SnippetLibraryCompletenessTest` — every lang in `LanguageMetadata.all` (from Feature 2) MUST have a `snippets.json` entry OR be in an explicit exclusion list in `KNOWN_DEFECTS.md`.

### 7.5 Challenges (wired into `make qa-all`)

15. `auto_complete_completeness_challenge.sh` — runs 7 unit + 4 Robolectric + smoke + structural; emits per-test PASSED-count + log path per CONST-035.
16. `snippet_library_bundle_challenge.sh` — `unzip -l` Android APK + Desktop tarball + Wasm bundle for the `snippets/` resource tree; asserts ≥ 50 lang dirs present.

Every test mutation-verified per the iter-57/58 pattern (mutation procedure captured in each test class's KDoc and re-executed before commit).

## 8. Deep-research checklist

Phase 0 equivalent for Feature 3. Output: `docs/features/auto-complete/research-report.md`.

- **VS Code snippet bundle inventory.** Survey `microsoft/vscode/tree/main/extensions/<lang>-basics/snippets/<lang>.json` for the 55 languages. Record presence + license confirmation (MIT) + bundle size estimate per lang. Identify languages where Microsoft ships no built-in snippets (e.g., bibtex, regex, dockerfile may lack).
- **VS Code snippet schema authoritative reference.** `https://code.visualstudio.com/api/language-extensions/snippet-guide`. Document the `${N:placeholder}` and `${VARIABLE:default}` syntaxes; whether Yole's v1 supports `$TM_FILENAME` style built-in variables.
- **Tree-Sitter "node at byte" API.** For `ScopeAwareRanker` to inspect the node containing the cursor, the bonede `TSTree`/`TSNode` API needs `descendantForByteRange`. Document the JVM call signature.
- **Compose Popup composable.** Modern Compose 1.7 ships `androidx.compose.ui.window.Popup` with anchor + offset. Document the API + how to compute pixel position from a `TextLayoutResult.getCursorRect()`.
- **Snippet placeholder navigation.** How VS Code handles sequential `${1}`, `${2}`, `${3}` via Tab. Yole's IndentEngine (Feature 2 Phase 4) intercepts Enter — coexistence rules with snippet-Tab need explicit definition.
- **Provider scheduling.** Should `CompletionEngine.complete` block until all 3 providers return, or progressive-emit? Compose `Flow<List<CompletionItem>>` favors progressive — document the chosen behavior + UX rationale.

Output requirements: ≥ 600 lines, ≥ 100 URL citations, all gaps marked OPEN with explicit spike output expectation.

## 9. Documentation deliverables

- `docs/features/auto-complete/user-guide.md` — end-user: trigger keys per platform, popup navigation, snippet placeholder editing, supported languages.
- `docs/features/auto-complete/architecture.md` — contributor: provider interface, how to add a new provider (preparing for Feature 4 LSP), how to add a new language's snippet bundle.
- `docs/features/auto-complete/research-report.md` — output of §8.
- `docs/features/auto-complete/snippet-coverage-matrix.md` — 55-language × {bundled snippets, custom user snippets, Tree-Sitter ranker available} matrix with evidence.
- Updated CHANGELOG.md + docs/CONTINUATION.md per CONST-036.

## 10. Firebase distribution

Same per-platform reality as iter-58 v1.2.0:
- **Android Release + Debug:** ship via Firebase App Distribution (DEV variant from iter-59 picks up Feature 3 automatically).
- **Desktop macOS-arm64:** DMG built out-of-band.
- **Desktop Linux/Windows:** pending `#crossbuild-linux-windows-infra`.
- **Web Wasm:** pending `#wasmjs-production-distribution-gap`.
- **iOS:** pending `#shared-iosmain-databasefactory-broken` + iOS Tree-Sitter cinterop work.

Version target: **1.3.0 / versionCode 130 / dotted `0.0.0.1.30`**.

## 11. Out of scope for v1

- LSP-driven semantic completions (Feature 4).
- Multi-cursor completion.
- AI/LLM-driven suggestions.
- Snippet placeholder choice lists (`${1|a,b,c|}` VS Code syntax — v1 supports positional placeholders only, not choice lists).
- Snippet body variables beyond text — VS Code's `$TM_FILENAME`/`$CLIPBOARD`/etc. land in v2.
- Workspace-wide identifier scan (only current file in v1).

## 12. Open questions for the implementation plan

- Provider scheduling: progressive-emit vs all-or-nothing? Recommendation: progressive (better UX); confirm in plan after Phase 0 §6 research.
- Snippet Tab navigation vs IndentEngine Tab handling: which wins when both are armed? Recommendation: snippet-Tab takes precedence while a snippet's placeholder ranges are active; falls through to IndentEngine after the last placeholder.
- Mobile suggest button placement: top toolbar (next to existing Outline button) vs floating action button? Recommendation: top toolbar — consistent with Outline.
- Popup max-height: 8 items vs 10 vs 15? Recommendation: 8 on mobile (tighter screens), 10 on Desktop/Web.

## 13. Forensic anchor

Brainstorm session 2026-05-15 with operator. Operator chose at every round:

1. Completion sources = "Comprehensive: token-frequency + snippets + identifier-aware + scope-aware ranking".
2. Trigger model = "Implicit (after 2 chars) + explicit (Ctrl+Space) on Desktop; toolbar button on mobile".
3. Snippet library = "Vendored from VS Code's basics snippet bundles + user-extensible JSON".

No defaults selected; every choice explicit.

---

**Next steps after operator review:**

1. Operator reviews + requests any changes.
2. Invoke `superpowers:writing-plans` skill to produce `docs/superpowers/plans/2026-05-15-auto-complete-plan.md`.
3. Plan begins with Phase 0 deep-research (§8) before any code lands.
4. Implementation phases follow with bite-sized TDD, mutation-verified anti-bluff tests, CONST-037 cross-platform-impact tracking, and Firebase distribution at the end.
