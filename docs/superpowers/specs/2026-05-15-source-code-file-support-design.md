# Source-Code File Support — Design Spec (Feature 2 of 5)

> **Status:** Brainstorm complete; awaiting operator review of this file before invoking writing-plans.
> **Author:** brainstormed 2026-05-15 with operator.
> **Sequence:** Feature 2 of 5 in the comprehensive editor capability initiative. Feature 1 (syntax highlighting) shipped as iter-57 v1.1.0. Feature 2 follows in dependency order.

---

## 1. Goal

Make Yole a first-class editor for source code across 50+ programming languages. Beyond Feature 1's syntax highlighting, Feature 2 lights up:

- **Format-level integration:** each language gets a proper `TextFormat` entry with display name, file-extension mapping, MIME types, lang-specific icon. The full 50+ language set appears in Settings → Formats with individual toggles.
- **Rich language affordances** (the per-language IDE-ish behaviors):
  - **Comment toggle** (Ctrl+/) using each language's correct comment syntax.
  - **Auto-indent** on Enter using per-language indent rules (Tree-Sitter-aware where the grammar supports it).
  - **Bracket pair auto-complete** when typing opening brackets and quotes.
  - **Outline view** (left-panel tree of functions/classes/methods extracted via Tree-Sitter `outline.scm` queries).
  - **Fold regions** in the gutter (Tree-Sitter `folds.scm` queries → toggleable fold markers).

The result is that opening a `.kt` file in Yole behaves like opening it in a lightweight IDE — colored tokens, working Ctrl+/, smart Enter, paired brackets, an outline, and foldable regions — across all four platforms (Android, Desktop, iOS, Web).

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Feature scope beyond Feature 1 | Comprehensive — first-class TextFormat entries + rich affordances (comment toggle, auto-indent, outline, bracket-pair, fold). |
| Language set | All 50+ from `docs/features/syntax-highlighting/research-report.md` §5. |
| Grammar acquisition | Bundle ALL 50+ grammar binaries + query files in the base APK / IPA / Desktop tarball / Web Wasm bundle. ~30-40 MB additional install size. No CDN lazy-fetch in Feature 2. |
| Phasing | Big bang — all 50+ languages with all 5 affordances in one ship cycle. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific governance leaks into sibling submodules.

---

## 3. Architecture (Approach C — hybrid: data-driven + per-language code where needed)

```
shared/src/commonMain/kotlin/digital/vasic/yole/language/      # new subsystem
├── LanguageFormat.kt              # extends TextFormat with affordance metadata
├── LanguageRegistry.kt             # detect, list-all, get-by-id
├── LanguageMetadata.kt             # 50+ language manifests (id, ext, MIME, icon)
├── affordance/
│   ├── CommentSyntax.kt           # data class: line + block prefix/suffix per lang
│   ├── IndentRules.kt             # data class: indent-on / dedent-on tokens per lang
│   ├── BracketPairs.kt            # data class: matching brackets + quote pairs
│   ├── FoldQueryRunner.kt         # generic Tree-Sitter fold-query executor
│   └── OutlineExtractor.kt        # generic Tree-Sitter outline-query executor
├── special/
│   ├── HtmlEmbeddedLang.kt        # HTML's embedded CSS/JS handling (special case)
│   └── MarkdownCodeFences.kt      # delegate to nested grammar per fence
└── ScmQueryLoader.kt              # loads per-language .scm query files (cached)

shared/src/commonMain/resources/
├── grammars/<lang>/grammar.tmLanguage.json    # for Wasm (TextMate)
├── grammars/<lang>/highlights.scm             # Tree-Sitter highlight queries
├── grammars/<lang>/folds.scm                  # Tree-Sitter fold queries
└── grammars/<lang>/outline.scm                # Tree-Sitter outline queries

shared/src/androidMain/jniLibs/<abi>/libtree-sitter-<lang>.so    # ~150 .so (50 × 3 ABI)
shared/src/desktopMain/native/<os-arch>/libtree-sitter-<lang>.{so,dylib,dll}
shared/src/iosMain/native/lib<lang>.a                            # ~150 static libs

editor surface (androidApp, desktopApp, iosApp, webApp):
├── CommentToggleAction.kt          # Ctrl+/ keybinding
├── BracketAutoCompleter.kt         # auto-insert closing brackets/quotes
├── IndentEngine.kt                 # apply per-lang IndentRules on Enter
├── OutlineDrawer.kt                # left-panel outline view
└── FoldGutter.kt                   # gutter fold-toggle icons
```

**Invariants:**

- **95% data-driven.** Per-language affordances come from `LanguageMetadata` rows + bundled `.scm` query files. Only 2-3 special-case languages (HTML embedded sub-languages, markdown nested code fences) need per-language code.
- **`expect/actual` only at the engine boundary** (`TokenizerEngine` from iter-57). Affordance logic runs in pure `commonMain`.
- **All 50+ grammars bundled in the base release artifact** per the operator's decision. No CDN lazy-fetch path. APK/IPA/tarball install size grows by ~30-40 MB.

---

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `LanguageFormat` | Extends `TextFormat` with `commentSyntax`, `indentRules`, `bracketPairs`, plus grammar reference. | 50+ instances |
| `LanguageMetadata` | Static manifest of every supported language. | 1 object |
| `CommentSyntax` | `lineComment: String?`, `blockComment: Pair<String,String>?`. | 50+ data values |
| `IndentRules` | `indentTokens`, `dedentTokens`, optional Tree-Sitter query for context-aware indent. | 50+ data values |
| `BracketPairs` | List of opener/closer `Pair<Char,Char>` + quote pairs. | 50+ data values (mostly identical) |
| `FoldQueryRunner` | Generic: Tree-Sitter tree + per-lang `folds.scm` → fold ranges. | 1 |
| `OutlineExtractor` | Generic: Tree-Sitter tree + per-lang `outline.scm` → outline items. | 1 |
| `ScmQueryLoader` | Loads `.scm` query files from bundled resources; per-session cache. | 1 |
| `HtmlEmbeddedLang` | Runs CSS sub-tokenizer for `<style>`; JS sub-tokenizer for `<script>`. | 1 |
| `MarkdownCodeFences` | Updates iter-57 Phase 10 highlighter to delegate to per-fence nested grammar. | 1 (updated) |
| Editor: `CommentToggleAction` | Composable handler for Ctrl+/. | 1 |
| Editor: `BracketAutoCompleter` | onValueChange intercept; inserts matching closer. | 1 |
| Editor: `IndentEngine` | onValueChange intercept; computes next-line indent on Enter. | 1 |
| Editor: `OutlineDrawer` | Slide-in left panel; calls `OutlineExtractor.extract`. | 1 |
| Editor: `FoldGutter` | Adds chevron icons in the gutter; renders folded regions as `…`. | 1 |

---

## 5. Data flow

### 5.1 File open → first-class language identification

```
User taps file in FILES tab
  → GrammarRegistry.detectByFilename(name)   (iter-57 surface)
  → LanguageRegistry.get(grammarId)          (new: returns LanguageFormat)
  → IdeEditorScreen receives langId + LanguageFormat
  → LocalLanguage.current = LanguageFormat
  → SyncedScrollEditor renders highlighting (iter-57)
  → CommentToggleAction / BracketAutoCompleter / IndentEngine
    all read LocalLanguage.current
```

### 5.2 Comment toggle (Ctrl+/)

```
User selects line(s) + Ctrl+/
  → CommentToggleAction.invoke():
      get LocalLanguage.current.commentSyntax
      for each selected line:
        if line starts with lineComment prefix → uncomment
        else → comment (insert prefix at first non-ws column)
      apply as single onValueChange
```

### 5.3 Auto-indent on Enter

```
User presses Enter at end of "fun foo() {"
  → BasicTextField onKeyEvent intercepts Enter
  → IndentEngine.computeIndent(text, cursor, indentRules):
      tokenize the line being broken
      if last non-ws token ∈ indentTokens
        → newIndent = currentLineIndent + indentUnit
      else → newIndent = currentLineIndent
  → insert "\n" + newIndent
```

### 5.4 Outline view

```
User opens outline panel (toolbar toggle)
  → OutlineDrawer composable opens
  → OutlineExtractor.extract(text, langId):
      tree = TokenizerEngine.tokenize(text, langId)
      query = ScmQueryLoader.outlineQuery(langId)
      run query against tree → captures matching nodes
      return List<OutlineItem(name, kind, byteRange)>
  → drawer renders tree
  → tapping an item scrolls editor to byteRange.start
```

### 5.5 Fold regions

```
On each text change (debounced 80 ms):
  → FoldQueryRunner.foldRangesFor(text, langId)
      tree = TokenizerEngine.tokenize
      query = ScmQueryLoader.foldQuery(langId)
      return List<FoldRange(startLine, endLine)>
  → FoldGutter renders chevrons at foldable lines
  → tapping a chevron toggles fold; folded region collapses to "…" line
  → cursor navigation respects fold boundaries
```

---

## 6. Error handling

| Error | Detection | Recovery |
|---|---|---|
| Grammar `.so` / `.scm` query file missing for a language | runtime check at `LanguageRegistry.get()` | Fall back to highlighting-only mode. Affordances disabled. Banner: "Limited language support — outline + fold unavailable for $lang". |
| `.scm` query file malformed | `ScmQueryLoader.parse()` throws | Skip that affordance; log warning. Other affordances for the lang still work. |
| Outline query times out on huge file (> 5 MB) | `withTimeout(2s)` wrap | Show truncated outline (first 50 entries). Status bar notice. |
| Comment-toggle on a line with mixed syntax (inline string containing `//`) | text-based heuristic | Conservative: only first-column comment prefix counted; tracked as known edge-case. |
| Auto-indent surprises on multi-bracket lines | IndentEngine consults Tree-Sitter tree | AST-based indent for TS-supported langs; brace-based fallback for non-TS. |
| Bracket auto-completer interferes with paste | onValueChange differentiates single vs multi-char input | Skip auto-completion when input length > 1. |
| HTML embedded sub-language fails to tokenize | per-region fallback | Outer HTML still highlighted; inner region renders plain. |
| Markdown code fence with unknown language tag | `GrammarRegistry` returns null | Fence renders plain — same as iter-57 Phase 10. |

All error paths covered by anti-bluff tests per §7.

---

## 7. Testing strategy (anti-bluff, mutation-verified)

### 7.1 Unit tests (commonTest)

1. **`LanguageRegistryTest`** — for each of 50+ languages, assert `detect/get` returns the right manifest.
2. **`CommentSyntaxTest`** — for each lang, assert commentSyntax matches a known canonical example.
3. **`IndentRulesTest`** — drive `IndentEngine.computeIndent()` on per-lang snippets; assert results.
4. **`BracketPairsTest`** — table-driven per-lang bracket set.
5. **`OutlineExtractorTest`** — per-lang snippet with known functions/classes; assert extracted items.
6. **`FoldQueryRunnerTest`** — per-lang foldable snippet; assert fold ranges.

≈ 50 languages × 5 test classes = 250+ test cases. Each mutation-verified.

### 7.2 Platform integration (Robolectric Android, desktopTest JVM)

7. **`CommentToggleActionRobolectricTest`** — Kotlin file, select 3 lines, Ctrl+/, assert commented; again, assert uncommented.
8. **`BracketAutoCompleterRobolectricTest`** — type `(`, assert cursor at `(|)`.
9. **`IndentEngineRobolectricTest`** — type `if (x) {`, Enter, assert indented one level deeper.
10. **`OutlineDrawerRobolectricTest`** — Kotlin file with one `fun foo()` + one `class Bar`, open outline, assert 2 items.
11. **`FoldGutterRobolectricTest`** — multi-line function, tap chevron, assert body collapses.

### 7.3 Per-language smoke test

12. **`Feature2LanguageSmokeTest`** — parametrized over 50 langs. For each: open `test-fixtures/<lang>/example.txt`; assert highlight produces tokens, outline produces ≥ 1 item, fold produces ≥ 1 range. Catches forgot-to-bundle-X regressions.

### 7.4 Anti-bluff structural

13. **`LanguageMetadataCompletenessTest`** — for each lang in `LanguageMetadata.all`: `.scm` files exist, grammar binary exists per platform, fixture exists.
14. **`LanguageAffordanceParityTest`** — every lang has matching CommentSyntax / IndentRules / BracketPairs entries.

### 7.5 Challenges

15. **`language_support_completeness_challenge.sh`** — runs the 14 test suites, asserts ≥ 50 langs have full affordance coverage. Per CONST-035 emits per-language pass/fail evidence.
16. **`language_grammar_bundle_challenge.sh`** — verifies each platform's release artifact contains the bundled grammars (APK `unzip -l` check for `.so`; Desktop tarball check for `.dylib`/`.dll`/`.so`; Wasm bundle check for grammar JSONs).

Every test in this feature is mutation-verified per iter-57 pattern. The mutation procedure is captured in each test class's KDoc and re-executed before commit.

---

## 8. Deep-research checklist

Phase 0-equivalent task before writing per-language code:

- **Tree-Sitter `outline.scm` query format** — survey upstream Tree-Sitter community grammars for how each language's outline query is conventionally written (e.g., `nvim-treesitter`'s queries directory is an excellent reference).
- **Tree-Sitter `folds.scm` query format** — same survey.
- **Per-language indent conventions** — Python uses 4-space; Go uses tabs; CSS uses 2-space; what's the canonical default per lang?
- **Per-language comment syntax** — line and block, with quirks (Haskell uses `--` for line, `{- -}` for block; SQL uses `--` for line, `/* */` for block; etc.).
- **Special cases:** HTML embedded CSS+JS; markdown nested code fences; Vue single-file components; Svelte; Astro; MDX (markdown + JSX); etc.
- **Grammar binary acquisition for 50 × 3 Android ABIs = 150 NDK builds.** Either pre-built artifact source OR vendored NDK build pipeline.

This research feeds the implementation plan; assumptions about query file shape are validated against actual upstream grammar repositories before any code is written. Output: `docs/features/source-code-file-support/research-report.md`.

---

## 9. Documentation deliverables

- `docs/features/source-code-file-support/user-guide.md` — end-user docs (comment toggle, outline panel, fold regions, supported languages).
- `docs/features/source-code-file-support/architecture.md` — contributor reference; per-language data model; how to add a new language.
- `docs/features/source-code-file-support/research-report.md` — output of §8.
- `docs/features/source-code-file-support/language-coverage-matrix.md` — table of 50+ langs with per-affordance coverage status (helpful for future contributors).
- Updated CHANGELOG.md + docs/CONTINUATION.md per CONST-036.

---

## 10. Firebase distribution

Per the operator's iter-57 lessons:
- **Android:** APK with bundled grammars (~30-40 MB additional). Distributed via Firebase App Distribution (works today).
- **Desktop:** 3 tarballs; macOS-arm64 ships, Linux+Windows depend on `#crossbuild-linux-windows-infra` resolution.
- **iOS:** ships once `#shared-iosmain-databasefactory-broken` is resolved.
- **Web Wasm:** ships once `#wasmjs-production-distribution-gap` is resolved.

Phase 14-equivalent for Feature 2 includes evidence capture at `docs/qa/iter-58/` or whichever iteration ships Feature 2.

---

## 11. Out of scope for v1

- Auto-complete (Feature 3).
- LSP-driven semantic analysis (Feature 4).
- Diagnostics / error squigglies (Feature 4).
- Per-language formatter integrations (gofmt, black, prettier) — operator-decision territory; deferred.
- Workspace-level features (multi-file project view, go-to-definition across files) — Feature 4 territory.

---

## 12. Open questions for the implementation plan

- Final per-language `outline.scm` and `folds.scm` sourcing strategy: vendor from `nvim-treesitter` (MIT-licensed, well-maintained, ~150 langs) vs. hand-author? Recommendation: vendor.
- Per-language indent unit (tabs vs N spaces) — fixed default per lang in `LanguageMetadata` vs user-overridable in Settings? Recommendation: fixed default per lang, user can override globally in Settings.
- Outline panel: slide-in drawer vs persistent split-view? Recommendation: slide-in drawer (matches mobile-first Yole UX).
- Fold gutter: render in the existing gutter or a separate column? Recommendation: existing gutter (saves screen real estate).

---

## 13. Forensic anchor

Brainstorm session 2026-05-15 with operator. Operator chose at every round:

1. Feature 2 scope = "TextFormat + rich language affordances (Comprehensive)".
2. Language set = "All 50+ from research-report.md §5".
3. Grammar acquisition = "Bundle all 50+ in the base APK/IPA".
4. Phasing = "Big bang: ship all 50+ in one cycle".

No defaults selected; every choice was explicit. Verbatim approvals captured in the AskUserQuestion answers.

---

**Next steps after operator review of this spec:**

1. Operator reviews + requests any changes.
2. Invoke `superpowers:writing-plans` skill to produce `docs/superpowers/plans/2026-05-15-source-code-file-support-plan.md`.
3. Plan begins with Phase 0 deep-research task (§8) before any per-language code lands.
4. Implementation phases follow with bite-sized TDD, mutation-verified anti-bluff tests, CONST-037 cross-platform-impact tracking, and a Firebase-distribution gate at the end.
