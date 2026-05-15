# Source-Code File Support — Architecture

> **Audience:** Yole contributors. Read `user-guide.md` first.

This document describes the implementation contract behind iter-58 Feature 2: package layout, component responsibilities, dataflow per affordance, per-platform notes, anti-bluff invariants, and the recipe for adding a new language.

---

## 1. Package layout

```
shared/src/commonMain/kotlin/digital/vasic/yole/language/
├── LanguageFormat.kt           # data class — per-language manifest
├── LanguageMetadata.kt         # object — 55 LanguageFormat singletons + all list
├── LanguageRegistry.kt         # lookup: get(id), detectByFilename(name), all()
├── LocalLanguage.kt            # Compose CompositionLocal — LocalLanguage: LanguageFormat?
├── ScmQuery.kt                 # data class ScmQuery(langId, queryName, content)
├── ScmQueryLoader.kt           # classpath resource loader + in-memory cache
├── affordance/
│   ├── CommentSyntax.kt        # data class — lineComment / blockComment pair
│   ├── IndentRules.kt          # data class — indentTokens / dedentTokens sets
│   ├── BracketPairs.kt         # data class — open→close char pairs map
│   ├── FoldQueryRunner.kt      # expect class — runs folds.scm → List<FoldRange>
│   ├── FoldRange.kt            # data class — startLine, endLine, label
│   ├── OutlineExtractor.kt     # class — runs outline.scm → List<OutlineItem>
│   └── OutlineItem.kt          # data class — name, kind, lineNumber
└── special/
    ├── HtmlEmbeddedLang.kt     # object — CSS/JS re-tokenization inside <style>/<script>
    └── MarkdownCodeFences.kt   # object — per-fence sub-language tokenization

shared/src/{android,desktop,ios,wasmJs}Main/kotlin/digital/vasic/yole/language/affordance/
└── FoldQueryRunner.<platform>.kt   # actual class

shared/src/commonMain/resources/grammars/
├── <lang-id>/
│   ├── highlights.scm          # vendored from nvim-treesitter / helix (Apache-2.0 / MPL-2.0)
│   ├── folds.scm               # vendored or Yole-authored stub
│   └── outline.scm             # vendored from helix tags.scm or Yole-authored stub
├── THIRD-PARTY.md              # attribution notice (Apache-2.0 attribution §4)
└── MANIFEST.json               # per-file upstream URL + upstream SHA + local SHA-256

shared/src/commonTest/resources/test-fixtures/
└── <lang-id>/                  # at least one fixture file per language

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/
├── SyncedScrollEditor.kt       # wires all 5 affordances into the editor Composable
├── CommentToggleAction.kt      # Ctrl+/ handler
├── IndentEngine.kt             # Enter-key smart-indent handler
├── BracketAutoCompleter.kt     # bracket-pair auto-close key handler
├── OutlineDrawer.kt            # slide-in drawer Composable
└── FoldGutter.kt               # gutter Composable with chevron tap handling
```

The iter-57 `syntax/` subsystem (`TokenizerEngine`, `SyntaxHighlighter`, `GrammarRegistry`, etc.) is consumed unchanged. Feature 2 builds on top of it.

---

## 2. Component table

| Component | Responsibility | Lives in |
|---|---|---|
| `LanguageFormat` | Immutable per-language manifest: id, displayName, extensions, mimeTypes, commentSyntax, indentRules, bracketPairs, indentUnit | `commonMain` |
| `LanguageMetadata` | Object holding all 55 `LanguageFormat` singletons + the `all` list | `commonMain` |
| `LanguageRegistry` | Lookup by id or filename extension; list all | `commonMain` |
| `LocalLanguage` | `CompositionLocal<LanguageFormat?>` — lets any Composable read the current file's language | `commonMain` (Compose) |
| `ScmQueryLoader` | Loads `.scm` files from the classpath resource path `grammars/<id>/<query>.scm`, caches in-memory; exposes `clearCacheForTest()` | `commonMain` |
| `CommentSyntax` | Data class: `lineComment: String?`, `blockComment: Pair<String,String>?` | `commonMain/affordance` |
| `IndentRules` | Data class: `indentTokens: Set<String>`, `dedentTokens: Set<String>` | `commonMain/affordance` |
| `BracketPairs` | Data class: `pairs: Map<Char,Char>` (defaults: `()[]{}""''`) | `commonMain/affordance` |
| `FoldQueryRunner` | `expect class` — runs `folds.scm` against the parse tree to produce `List<FoldRange>` | `commonMain` expect; platform actuals |
| `OutlineExtractor` | `class` using `TokenizerEngine` + `outline.scm` to produce `List<OutlineItem>` | `commonMain` |
| `HtmlEmbeddedLang` | Merges CSS/JS sub-engine tokens into the outer HTML token stream | `commonMain/special` |
| `MarkdownCodeFences` | Detects per-fence sub-language; re-tokenizes fences with the appropriate sub-engine | `commonMain/special` |
| `CommentToggleAction` | Kotlin/Android Composable action — reads `LocalLanguage`, applies `CommentSyntax` rule | `androidMain` |
| `IndentEngine` | Kotlin/Android `TextInputFilter`-like actor — reads `LocalLanguage.indentRules` + `indentUnit` | `androidMain` |
| `BracketAutoCompleter` | Kotlin/Android key-event interceptor | `androidMain` |
| `OutlineDrawer` | Kotlin/Android Composable drawer; calls `OutlineExtractor` on file open | `androidMain` |
| `FoldGutter` | Kotlin/Android Composable gutter strip; calls `FoldQueryRunner` on file open | `androidMain` |

---

## 3. Dataflow per affordance

### 3.1 Comment toggle

```
User presses Ctrl+/
  └→ CommentToggleAction.toggle(text, selection, lang):
       ├→ lang.commentSyntax.lineComment != null?
       │    yes → prefix each selected line with lineComment
       │           (or strip prefix if line already starts with it)
       └→ lineComment == null && blockComment != null?
            yes → wrap entire selection with blockComment.first / blockComment.second
                  (or strip if already wrapped)
  └→ editor.textState updated with new comment state
```

### 3.2 Smart auto-indent

```
User presses Enter
  └→ IndentEngine.computeNextIndent(currentLine, lang):
       ├→ Load indents.scm via ScmQueryLoader if present
       │    if indents.scm parse succeeds → use Tree-Sitter indent level
       └→ Fallback: check currentLine.trimEnd() in lang.indentRules.indentTokens
            if match → currentIndent + lang.indentUnit
            else → currentIndent (unchanged)
  └→ editor inserts "\n" + computedIndent at cursor
```

### 3.3 Outline panel

```
File opened in editor
  └→ OutlineDrawer.refreshOutline(content, lang, engine):
       ├→ engine.loadGrammar(lang.id)   // no-op if already loaded
       ├→ ScmQueryLoader.load(lang.id, "outline") → outlineScm: String
       └→ OutlineExtractor.outlineFor(content, lang.id, engine):
              ├→ engine.tokenize(content, lang.id) → tokens
              ├→ filter tokens by captures in outlineScm (@name, @kind, etc.)
              └→ returns List<OutlineItem>(name, kind, lineNumber)
       └→ OutlineDrawer renders the list in a LazyColumn
  User taps an item → editor scrolls to item.lineNumber
```

### 3.4 Bracket-pair auto-close

```
User types an opening char (e.g. '(')
  └→ BracketAutoCompleter.onChar(char, lang):
       ├→ closer = lang.bracketPairs.pairs[char] ?: return  // not a pair opener
       ├→ Insert char + closer at cursor position
       └→ Move cursor to between them (after char, before closer)
```

### 3.5 Fold gutter

```
File opened in editor
  └→ FoldGutter.refreshFolds(content, lang, engine):
       ├→ engine.loadGrammar(lang.id)
       ├→ ScmQueryLoader.load(lang.id, "folds") → foldsScm: String
       └→ FoldQueryRunner.foldRangesFor(content, lang.id, engine):
              ├→ platform actual walks the parse tree using foldsScm
              └→ returns List<FoldRange>(startLine, endLine, label)
       └→ FoldGutter renders a ▼ icon at each startLine
  User taps ▼ → fold applied (lines startLine+1..endLine hidden)
  User taps ▶ → fold expanded
```

### 3.6 Embedded sub-language tokenization

#### HTML (`<style>` + `<script>`)

```
HtmlEmbeddedLang.tokenize(text, htmlEngine, cssEngine?, jsEngine?):
  1. htmlEngine.tokenize(text, "html") → outerTokens
  2. styleRegex.findAll(text) → CSS body ranges (when cssEngine != null)
  3. scriptRegex.findAll(text) → JS body ranges (when jsEngine != null)
  4. For each embedded region:
       subTokens = subEngine.tokenize(body, subLang)
       Adjust byte offsets by region.byteStart
  5. Drop outer tokens overlapping resolved regions
  6. Splice sub-tokens in; sort by startByte
```

#### Markdown code fences

```
MarkdownCodeFences.tokenize(text, markdownEngine, engineFactory):
  1. markdownEngine.tokenize(text, "markdown") → outerTokens
  2. Find ``` lang ... ``` regions via injections.scm parsing
  3. For each fence: langId = detected sub-language
       if EnabledFormatGate.isEnabled(langId):
         subEngine = engineFactory(langId)
         subTokens = subEngine.tokenize(fenceBody, langId)
         Adjust byte offsets; splice into stream
       else: leave as outer markdown tokens
```

---

## 4. Per-platform notes

### Desktop (JVM — all 5 ABIs)

- `FoldQueryRunner.desktop.kt` uses the bonede `TSQuery` / `TSQueryCursor` API directly on the JVM parse tree (the same `TSTree` produced by `TokenizerEngine.tokenize`).
- `BonedeGrammarRegistry` maps 47 Yole language IDs to their fully-qualified `org.treesitter.TreeSitter<Name>` class names. The 8-lang gap set (`jsx`, `xml`, `vim`, `less`, `crystal`, `groovy`, `bibtex`, `nim`) returns null; the engine throws `IllegalArgumentException` rather than faking a grammar (CONST-035).
- All 47 grammars are declared as Gradle dependencies in `shared/build.gradle.kts` `desktopMain` dependencies block. Each JAR ships native `.so` / `.dll` / `.dylib` for `linux-x86_64`, `linux-aarch64`, `windows-x86_64`, `osx-x86_64`, `osx-aarch64`.

### Android

- `FoldQueryRunner.android.kt` is an honest stub: `foldRangesFor` returns `emptyList()` with a log message naming `KNOWN_DEFECTS#f2-phase-7-android-ndk-bulk-build-pending`. No fake fold ranges.
- `TokenizerEngine.android.kt loadGrammar()` only recognizes `"markdown"` (the iter-57 NDK-built `.so`). All other languages throw `IllegalArgumentException` with an explicit reference to the defect ticket.
- Comment toggle, auto-indent, and bracket-pair auto-close work for all 55 languages because they run off `LanguageMetadata` data, not Tree-Sitter grammars.
- Outline is available only for Markdown (same constraint as fold).

### iOS

- `FoldQueryRunner.ios.kt` is a `NotImplementedError` stub. The iOS build host lacks Xcode; no `libtree-sitter-<lang>.a` static libraries can be compiled. Tracked in `KNOWN_DEFECTS#f2-phase-7-ios-xcode-required`.
- All three non-grammar affordances (comment, indent, bracket) work for all 55 languages.

### Web (Wasm)

- `FoldQueryRunner.wasmJs.kt` is a stub returning `emptyList()` — `vscode-textmate`'s API does not expose a parse tree for fold-range extraction. A future upgrade to `web-tree-sitter` would be required.
- `OutlineExtractor` returns `emptyList()` on Wasm for the same reason.
- Comment toggle, auto-indent, and bracket auto-close work for all 55 languages.

---

## 5. Anti-bluff invariants and their tests (CONST-035)

### 5.1 Every language has highlights.scm, folds.scm, outline.scm

**Test:** `LanguageMetadataCompletenessTest.everyLanguageHasHighlightsScm` /  `everyLanguageHasFoldsScm` / `everyLanguageHasOutlineScm`.
**Mutation:** temporarily rename `python/highlights.scm` to `python/_.scm` → test fails with:
`"python: grammars/python/highlights.scm must exist on classpath"`.
**Commits:** `36621a3f`…`8f8b01ef` (Phase 6 batch 1–5 + structural tests).

### 5.2 Every .scm file carries a SPDX header

**Test:** `LanguageMetadataCompletenessTest.everyScmFileHasSpdxHeader`.
**Mutation:** strip the SPDX header from `kotlin/folds.scm` → test fails.

### 5.3 ScmQueryLoader round-trip for all 55 languages

**Test:** `LanguageMetadataCompletenessTest.loaderRoundtripWorksForEveryLanguage`.
**Mutation:** stub `ScmQueryLoader.load` to return blank → all 55 × 3 = 165 assertions fail.

### 5.4 Real end-to-end tokenization for all 47 bundled languages

**Test:** `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs`.
Asserts `>= 1 token` per language using the real bonede grammar against its test fixture.
**Mutation:** stub `TokenizerEngine.tokenize` → 0 tokens → test fails.
**Commit:** `9606ff42` (Phase 7).

### 5.5 8-lang gap set throws honestly (no fake grammars)

**Test:** `Feature2LanguageSmokeTest.unsupportedLangs_throwHonestly` (via `BonedeGrammarSmokeTest`).
**Mutation:** add `"xml"` to `BonedeGrammarRegistry.classNames` with a fabricated class name → test fails with a class-not-found error that the test detects as unexpected success.

### 5.6 Markdown end-to-end (outline + fold)

**Tests:** `Feature2LanguageSmokeTest.markdownEndToEndProducesOutlineItems` and `markdownEndToEndProducesFoldRange`.
**Mutations:** stub `OutlineExtractor.outlineFor` to return `emptyList()` → outline test fails; stub `FoldQueryRunner.foldRangesFor` → fold test fails.

### 5.7 Grammar-bundling challenge (external)

**Challenge:** `yole-challenges/scripts/language_grammar_bundle_challenge.sh` — verifies that 47 bonede JARs are declared in `shared/build.gradle.kts`, that the 8-lang gap set is documented in `KNOWN_DEFECTS.md`, and that a Gradle dependency build succeeds.
**Challenge:** `yole-challenges/scripts/language_support_completeness_challenge.sh` — verifies that all 55 `LanguageMetadata.all` entries have non-empty `commentSyntax` coverage (or documented exemption) and that `LanguageRegistry.detectByFilename` returns non-null for the first extension of each language.
**Commits:** `2982ded0` (Phase 9).

---

## 6. Error handling matrix

| Error condition | Detection | Recovery |
|---|---|---|
| `.scm` resource missing | `ScmQueryLoader.load` throws `IllegalStateException` | Fold/Outline returns `emptyList()`; user sees empty panel. |
| Grammar not bundled (gap set) | `BonedeGrammarRegistry.classNameFor` returns null | `IllegalArgumentException` with defect-ticket reference; editor falls back to plain text. |
| Grammar load fails (reflection) | `ClassNotFoundException` or `InvocationTargetException` | Same plain-text fallback; exception logged. |
| Sub-grammar throws in HtmlEmbeddedLang | `catch (Throwable)` — excluding CancellationException | Outer HTML tokens kept for that region; no fabricated sub-tokens. |
| CancellationException | All coroutine boundary catch blocks rethrow | Standard coroutine cancellation; no data loss. |
| FoldQueryRunner Android stub | Returns `emptyList()` + logs defect reference | Fold gutter empty; no crash. |
| indents.scm not available | `ScmQueryLoader.load` throws | Fall back to simple `indentTokens` set matching. |

---

## 7. How to add a new language (10-step recipe)

1. **Add a `LanguageFormat` singleton** to `LanguageMetadata.kt` with the language's id, displayName, extensions, mimeTypes, commentSyntax, indentRules, bracketPairs, and indentUnit. Add it to the `all` list at the correct position.

2. **Register in `LanguageRegistry`** — no code change needed; `LanguageRegistry.detectByFilename` iterates `LanguageMetadata.all` automatically.

3. **Vendor the Tree-Sitter query files** at `shared/src/commonMain/resources/grammars/<id>/`:
   - `highlights.scm` — from nvim-treesitter `queries/<lang>/highlights.scm` (Apache-2.0).
   - `folds.scm` — from nvim-treesitter `queries/<lang>/folds.scm`, or author a stub.
   - `outline.scm` — from helix `runtime/queries/<lang>/tags.scm` (MPL-2.0 query files), or author a stub.
   Each file must begin with SPDX header lines (see any existing file for the template).

4. **Add the upstream URL + SHA to `MANIFEST.json`** alongside the vendored files.

5. **Add a test fixture** at `shared/src/commonTest/resources/test-fixtures/<id>/sample.<ext>`. The fixture must contain the language's own comment marker (for `Feature2LanguageSmokeTest.every_NonMarkdown_LangHasFixtureThatExercisesItsCommentSyntax`).

6. **Add the bonede Gradle dependency** to `shared/build.gradle.kts` `desktopMain` dependencies block:
   ```kotlin
   implementation("io.github.bonede:tree-sitter-<lang>:<version>")
   ```
   If no bonede artifact exists, add the language id to `BonedeGrammarRegistry.unsupportedLangs` and document the gap in `docs/KNOWN_DEFECTS.md`.

7. **Add the class-name entry to `BonedeGrammarRegistry.classNames`** (when the bonede artifact exists):
   ```kotlin
   "<yole-id>" to "org.treesitter.TreeSitter<TitleCase>"
   ```

8. **Add a ScopeMapper entry** if the new grammar emits Tree-Sitter scope names not already in `ScopeMapper.kt` (the mapping from Tree-Sitter node types to VS Code token scopes).

9. **Write an anti-bluff test** in `Feature2LanguageSmokeTest` or a dedicated `<Language>SmokeTest`:
   - Asserts `>= 1 token` from `TokenizerEngine.tokenize(fixture, langId)`.
   - Asserts `>= 1 item` from `OutlineExtractor.outlineFor(fixture, langId, engine)`.
   - Documents the mutation: stubbing `tokenize` → 0 tokens → test fails.

10. **Update `KNOWN_DEFECTS.md`** if any gap exists (no bonede artifact, Android NDK build pending, etc.) following the existing entry format.

---

## 8. Cross-references

- **Spec:** `docs/superpowers/specs/2026-05-15-source-code-file-support-design.md`.
- **Plan:** `docs/superpowers/plans/2026-05-15-source-code-file-support-plan.md`.
- **Research:** `docs/features/source-code-file-support/research-report.md`.
- **User guide:** `docs/features/source-code-file-support/user-guide.md`.
- **Language coverage matrix:** `docs/features/source-code-file-support/language-coverage-matrix.md`.
- **Syntax highlighting architecture:** `docs/features/syntax-highlighting/architecture.md` (iter-57 TokenizerEngine + SyntaxHighlighter).
- **Lock ordering:** `docs/LOCK_ORDERING.md`.
- **Known defects:** `docs/KNOWN_DEFECTS.md` (active iter-58 tickets: `#f2-phase-7-android-ndk-bulk-build-pending`, `#f2-phase-7-no-bonede-artifact`, `#f2-phase-7-nim-grammar-broken`, `#f2-phase-7-ios-xcode-required`, `#f2-phase-3-bonede-query-api-gap`).
