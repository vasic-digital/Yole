# Source-Code File Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship first-class source-code editing for 50+ programming languages on top of iter-57's syntax-highlighting foundation — every language gets its own `TextFormat` plus the 5 IDE-ish affordances (comment toggle, auto-indent, outline view, bracket-pair auto-complete, fold regions). All grammars + query files + native binaries bundled in the base APK/IPA/tarball.

**Architecture:** New `shared/.../language/` subsystem. 95% of per-language behavior is data-driven from `LanguageMetadata` rows + bundled Tree-Sitter `.scm` query files (vendored from `nvim-treesitter`). 5% special-case code: HTML embedded sub-languages, markdown nested code fences. Editor affordances (`CommentToggleAction`, `BracketAutoCompleter`, `IndentEngine`, `OutlineDrawer`, `FoldGutter`) all read `LocalLanguage.current` via Compose CompositionLocal, parallel to how iter-57's `LocalTheme` works.

**Tech Stack:** Kotlin Multiplatform 2.0.20, Compose Multiplatform 1.7.3, Tree-Sitter (existing iter-57 engines), `nvim-treesitter` query-file vendoring (MIT), bundled per-language `.so`/`.dylib`/`.dll` on JVM platforms, vscode-textmate JSON grammars on Wasm.

**Spec:** `docs/superpowers/specs/2026-05-15-source-code-file-support-design.md` (commit `f4444bc1`).

**Dependencies on iter-57:** This plan builds on `digital.vasic.yole.syntax.{TokenizerEngine, SyntaxHighlighter, EnabledFormatGate, GrammarRegistry, Theme}` shipped in iter-57. Reading those modules before starting Phase 1 is mandatory.

---

## Phase dependency graph

```
Phase 0  Deep research → research-report.md          (no code)
   │
   ▼
Phase 1  LanguageFormat + LanguageMetadata foundation
   │
   ▼
Phase 2  CommentSyntax + IndentRules + BracketPairs   (data layer)
   │
   ▼
Phase 3  FoldQueryRunner + OutlineExtractor + ScmQueryLoader  (generic runners)
   │
   ▼
Phase 4  Editor affordances: CommentToggleAction + BracketAutoCompleter + IndentEngine
   │
   ▼
Phase 5  Editor UI: OutlineDrawer + FoldGutter
   │
   ▼
Phase 6  Per-language data: 50 LanguageMetadata rows + 200 .scm query files (vendored)
   │
   ▼
Phase 7  Native binary acquisition: 50 × 3 ABI Android .so + Desktop natives + iOS .a
   │
   ▼
Phase 8  Special cases: HtmlEmbeddedLang + MarkdownCodeFences upgrade
   │
   ▼
Phase 9  2 new anti-bluff challenges + Makefile qa-iter-58-gates
   │
   ▼
Phase 10 Documentation + diagrams
   │
   ▼
Phase 11 Firebase distribution
```

**Detail level per phase:**

- **Phases 0–4** have full TDD bite-sized tasks with complete code.
- **Phases 5–11** have task-level checklists with exact file paths + acceptance criteria. Their step-by-step code crystallizes after Phase 0's research-report.md closes specific open questions (native-binary sourcing strategy, `nvim-treesitter` query-file licensing, per-language indent unit conventions). Each later phase declares a **"Pre-execution gate"** at its top.

---

## File Structure (new + modified across all phases)

**Create:**

```
shared/src/commonMain/kotlin/digital/vasic/yole/language/
  LanguageFormat.kt
  LanguageRegistry.kt
  LanguageMetadata.kt
  LocalLanguage.kt                   # Compose CompositionLocal
  affordance/
    CommentSyntax.kt
    IndentRules.kt
    BracketPairs.kt
    FoldQueryRunner.kt
    OutlineExtractor.kt
    OutlineItem.kt
    FoldRange.kt
  special/
    HtmlEmbeddedLang.kt
    MarkdownCodeFences.kt            # supersedes iter-57 Phase 10 logic
  ScmQueryLoader.kt
  ScmQuery.kt                        # parsed query data class

shared/src/commonMain/resources/grammars/<lang>/
  highlights.scm                     # iter-57 Phase 10 reuses; Feature 2 vendors all 50
  folds.scm                          # NEW per Feature 2
  outline.scm                        # NEW per Feature 2
  grammar.tmLanguage.json            # for Wasm

shared/src/androidMain/jniLibs/<abi>/libtree-sitter-<lang>.so   (~150 binaries)
shared/src/desktopMain/native/<os-arch>/libtree-sitter-<lang>.{so,dylib,dll}
shared/src/iosMain/native/lib<lang>.a                            (~150 static libs)

shared/src/commonTest/kotlin/digital/vasic/yole/language/
  LanguageRegistryTest.kt
  CommentSyntaxTest.kt
  IndentRulesTest.kt
  BracketPairsTest.kt
  OutlineExtractorTest.kt
  FoldQueryRunnerTest.kt
  ScmQueryLoaderTest.kt
  LanguageMetadataCompletenessTest.kt
  LanguageAffordanceParityTest.kt
  Feature2LanguageSmokeTest.kt

shared/src/commonTest/resources/test-fixtures/<lang>/example.txt   (50 fixtures)

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/
  CommentToggleAction.kt
  BracketAutoCompleter.kt
  IndentEngine.kt
  OutlineDrawer.kt
  FoldGutter.kt

androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/
  CommentToggleActionRobolectricTest.kt
  BracketAutoCompleterRobolectricTest.kt
  IndentEngineRobolectricTest.kt
  OutlineDrawerRobolectricTest.kt
  FoldGutterRobolectricTest.kt

yole-challenges/scripts/
  language_support_completeness_challenge.sh
  language_grammar_bundle_challenge.sh

docs/features/source-code-file-support/
  user-guide.md
  architecture.md
  research-report.md                 # output of Phase 0
  language-coverage-matrix.md
```

**Modify:**

```
shared/src/commonMain/kotlin/digital/vasic/yole/syntax/grammar/GrammarRegistry.kt
  - Hand grammar IDs back consistent with LanguageMetadata.all
shared/src/commonMain/kotlin/digital/vasic/yole/syntax/render/PreviewCodeBlockHighlighter.kt
  - Delegate to MarkdownCodeFences for nested grammar handling
androidApp/.../ui/editor/SyncedScrollEditor.kt
  - Wire CommentToggleAction + BracketAutoCompleter + IndentEngine
androidApp/.../ui/YoleApp.kt::IdeEditorScreen
  - Wrap editor in LanguageProvider (parallel to ThemeProvider)
desktopApp/.../ui/{EnhancedYoleApp,YoleApp}.kt
  - Same wiring as Android
iosApp/src/iosMain/.../Main.kt
  - Same wiring as Android
webApp/src/wasmJsMain/.../Main.kt
  - Same wiring
Makefile
  - Add qa-iter-58-gates target chained into qa-all
CHANGELOG.md
  - iter-58 entry
docs/CONTINUATION.md
  - Section 42 (iter-58)
```

---

## Phase 0 — Deep research

**Goal:** Produce `docs/features/source-code-file-support/research-report.md` that closes 6 specific questions. Phases 5+ won't start until this report exists.

**Files:**
- Create: `docs/features/source-code-file-support/research-report.md`

### Task 0.1: `nvim-treesitter` query-file inventory + licensing

- [ ] **Step 1: Inventory the 50 languages' query files**

For each of: kotlin, java, python, javascript, typescript, tsx, jsx, go, rust, c, cpp, csharp, ruby, php, swift, scala, dart, html, css, scss, less, sql, yaml, toml, json, xml, bash, lua, perl, haskell, ocaml, julia, r, elixir, erlang, fortran, vim, dockerfile, makefile, terraform, regex, vue, graphql, markdown, plus 5+ to reach 50:

Visit `https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries/<lang>/`. Record:
- Whether `highlights.scm` exists (Feature 1 already needs these; verify alignment).
- Whether `folds.scm` exists.
- Whether `locals.scm` exists (useful for outline extraction).
- License: nvim-treesitter ships under Apache-2.0 — verify in their LICENSE.
- File size estimate.

Output: table in research-report.md §1 with one row per language.

### Task 0.2: Outline-query strategy

- [ ] **Step 1: Determine the canonical pattern**

`nvim-treesitter` doesn't ship `outline.scm` directly — outline-style queries are typically embedded in `locals.scm` (definitions) or constructed by post-processing the highlight-query captures (`@function`, `@class`, etc.). Survey:
- `helix-editor/helix`'s `tags.scm` queries — they DO ship outline-equivalent queries.
- `nvim-treesitter-textobjects`'s `textobjects.scm` queries.

Choose ONE source. Output: research-report.md §2 with the chosen source's URL + per-language query-file URLs.

### Task 0.3: Native binary acquisition strategy

- [ ] **Step 1: Survey Tree-Sitter Android NDK builds**

iter-57 used `io.github.bonede:tree-sitter:0.22.6` + `io.github.bonede:tree-sitter-markdown:0.7.1a` with operator-built Android NDK `.so` per-ABI (commit `91c137fd` in iter-57). For Feature 2's 50+ languages, the same NDK build path must be applied per language. Inventory:
- For each of the 50 languages, the canonical `tree-sitter-<lang>` GitHub repo URL.
- Whether the repo includes a `Cargo.toml` or `package.json` enabling crate-based / npm-based generic build, OR a hand-written `src/parser.c`.
- License (most are MIT or Apache-2.0; verify each).
- Estimated `.so` size per ABI.

For Desktop natives: same `tree-sitter-<lang>` repo; cross-compile via the host's standard C toolchain. Document the per-OS build command.

For iOS static libs: same repo; build via Xcode `xcodebuild` or `clang` cross-compile for `arm64-apple-ios` + `arm64-apple-ios-simulator` + `arm64-apple-darwin`.

Output: research-report.md §3 with per-language source URL + per-platform build command + estimated size matrix.

### Task 0.4: Per-language indent + comment conventions

- [ ] **Step 1: Source the canonical conventions**

For each of the 50 languages, record:
- Line-comment prefix (`//`, `#`, `--`, `<!-- -->`, etc.).
- Block-comment delimiters (`/* */`, `<!-- -->`, `{- -}`, etc., or `null` if not supported).
- Conventional indent unit (`4 spaces`, `tab`, `2 spaces`).
- Open-tokens that increase indent (`{`, `(`, `[`, `do`, `then`, etc.).
- Close-tokens that decrease indent.

Cross-reference each against editorconfig.org's defaults and the official language style guides.

Output: research-report.md §4 as a 50-row table.

### Task 0.5: Special-case languages

- [ ] **Step 1: Document the edge cases**

- HTML: `<style>` blocks are CSS; `<script>` blocks are JavaScript. Tree-Sitter HTML grammar emits `style_element` / `script_element` nodes — recurse into those with the right sub-grammar.
- Markdown: code fences specify the language tag; the fence body parsed by that language's grammar. This is the iter-57 Phase 10 work; Feature 2 upgrades it from "syntax highlighting only" to "full affordance support inside the fence".
- Vue / Svelte / Astro / MDX: similar embedded-sub-language pattern. Recommendation: defer to Feature 2.1 (v1 ships HTML + Markdown only).
- TSX / JSX: lang-specific Tree-Sitter grammars (`tree-sitter-tsx`, `tree-sitter-typescript`'s `tsx` dialect) already handle embedded XML-like syntax.

Output: research-report.md §5 with concrete handling for HTML + Markdown; deferral note for Vue/Svelte/Astro/MDX.

### Task 0.6: Tree-Sitter query runtime API

- [ ] **Step 1: Document the query-execution API surface**

Tree-Sitter exposes `TSQuery`, `TSQueryCursor`, `TSQueryMatch`. The bonede Java wrapper exposes these as `Query`, `QueryCursor`, `QueryMatch`. The Wasm `vscode-textmate` ecosystem doesn't use `.scm` queries directly (TextMate uses pattern arrays). Feature 2 needs:
- JVM (Android+Desktop): `Query`, `QueryCursor` API for executing `.scm` queries against the parse tree.
- iOS K/N: same via cinterop.
- Wasm: hand-rolled query runner over TextMate token captures (or vendor `web-tree-sitter` JS lib if licensing fits).

Output: research-report.md §6 with API examples per platform.

### Task 0.7: Commit research report

- [ ] **Step 1: Verify completeness**

Run: `wc -l docs/features/source-code-file-support/research-report.md`
Expected: ≥ 600 lines covering all 6 sections.

Each section closes with a concrete decision, no `TBD`.

- [ ] **Step 2: Commit**

```bash
cd /Users/milosvasic/Projects/Yole
git add docs/features/source-code-file-support/research-report.md
git commit -m "$(cat <<'EOF'
docs(iter-58): deep-research report for source-code file support

Closes the 6 open questions left by docs/superpowers/specs/2026-05-15-source-code-file-support-design.md §8. Sections:
  §1 nvim-treesitter query-file inventory (50 langs × 3 query files).
  §2 Outline-query strategy (chosen: helix-editor tags.scm, MIT).
  §3 Native binary acquisition (per-lang repo + build command matrix).
  §4 Indent + comment conventions (50-row table).
  §5 Special-case languages (HTML + Markdown in v1; Vue/Svelte/Astro/MDX deferred).
  §6 Tree-Sitter query runtime API per platform.

Each section produces a concrete decision; no TBDs.

Cross-platform impact (CONST-037):
- Android / Desktop / iOS / Web: research; no code change.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
git push origin master
```

---

## Phase 1 — LanguageFormat + LanguageMetadata + LocalLanguage

**Goal:** Foundation data model. New `LanguageFormat` extends iter-57's `TextFormat`/`Grammar` to carry affordance metadata; `LanguageMetadata` static manifest; `LocalLanguage` CompositionLocal mirrors `LocalTheme` pattern.

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageFormat.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageMetadata.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageRegistry.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/LocalLanguage.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/LanguageRegistryTest.kt`

### Task 1.1: Write the failing test

- [ ] **Step 1: Create LanguageRegistryTest**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/language/LanguageRegistryTest.kt
/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: anti-bluff registry tests.
 *#######################################################*/
package digital.vasic.yole.language

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LanguageRegistryTest {
    @Test
    fun markdownIsRegistered() = runBlocking<Unit> {
        val lf = LanguageRegistry.get("markdown")
        assertNotNull(lf, "markdown LanguageFormat must be present")
        assertEquals("Markdown", lf.displayName)
        assertEquals(true, lf.extensions.contains(".md"))
    }

    @Test
    fun detectByFilename_handlesKotlin() = runBlocking<Unit> {
        val lf = LanguageRegistry.detectByFilename("HelloWorld.kt")
        assertNotNull(lf)
        assertEquals("kotlin", lf.id)
    }

    @Test
    fun detectByFilename_returnsNullOnUnknownExtension() = runBlocking<Unit> {
        assertNull(LanguageRegistry.detectByFilename("strange.xyznotreal"))
    }

    @Test
    fun all_listsAtLeastMarkdown() = runBlocking<Unit> {
        val ids = LanguageRegistry.all().map { it.id }
        assertEquals(true, "markdown" in ids,
            "LanguageMetadata.all must contain markdown (the v1 anchor)")
    }
}
```

- [ ] **Step 2: Run test — expect RED**

```bash
./gradlew :shared:desktopTest --tests "*LanguageRegistryTest*"
```
Expected: `Unresolved reference: LanguageRegistry`.

### Task 1.2: Implement LanguageFormat

- [ ] **Step 1: Write the class**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageFormat.kt
/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58: first-class language metadata. Extends iter-57's
 * Grammar abstraction with affordance data (filled in Phase 2).
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules

/**
 * First-class language metadata. Carries the iter-57 grammar id PLUS the
 * Feature 2 affordance data needed for comment-toggle, auto-indent,
 * bracket-pair, outline, and fold.
 */
data class LanguageFormat(
    val id: String,                 // matches iter-57 Grammar.id
    val displayName: String,
    val extensions: List<String>,
    val mimeTypes: List<String>,
    val commentSyntax: CommentSyntax,
    val indentRules: IndentRules,
    val bracketPairs: BracketPairs,
    val indentUnit: String = "    ", // 4 spaces default; per-lang overrides
)
```

### Task 1.3: Stub affordance data classes (Phase 2 fills them in)

- [ ] **Step 1: Create the stubs so Phase 1 compiles**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/CommentSyntax.kt
package digital.vasic.yole.language.affordance
data class CommentSyntax(
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
)

// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/IndentRules.kt
package digital.vasic.yole.language.affordance
data class IndentRules(
    val indentTokens: Set<String> = setOf("{", "(", "["),
    val dedentTokens: Set<String> = setOf("}", ")", "]"),
)

// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/BracketPairs.kt
package digital.vasic.yole.language.affordance
data class BracketPairs(
    val pairs: List<Pair<Char, Char>> = listOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\''),
)
```

### Task 1.4: Implement LanguageMetadata

- [ ] **Step 1: Phase 1 ships markdown only; Phase 6 fills the other 49**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageMetadata.kt
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules

/**
 * Static manifest of every Yole-supported language.
 * Phase 1 ships markdown only — the architectural anchor.
 * Phase 6 fills the other 49+ from Phase 0 research-report.md §4.
 */
object LanguageMetadata {
    val markdown = LanguageFormat(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
        mimeTypes = listOf("text/markdown", "text/x-markdown"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — markdown's de-facto convention
    )

    val all: List<LanguageFormat> = listOf(markdown)
}
```

### Task 1.5: Implement LanguageRegistry

- [ ] **Step 1: Write the class**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageRegistry.kt
package digital.vasic.yole.language

object LanguageRegistry {
    fun get(id: String): LanguageFormat? = LanguageMetadata.all.firstOrNull { it.id == id }

    fun detectByFilename(name: String): LanguageFormat? {
        val lowered = name.lowercase()
        for (lf in LanguageMetadata.all) {
            if (lf.extensions.any { lowered.endsWith(it.lowercase()) }) return lf
        }
        return null
    }

    fun all(): List<LanguageFormat> = LanguageMetadata.all
}
```

### Task 1.6: Implement LocalLanguage (Compose CompositionLocal)

- [ ] **Step 1: Write the class**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/LocalLanguage.kt
package digital.vasic.yole.language

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal carrying the active LanguageFormat for the current
 * editor surface. Mirrors iter-57's LocalTheme pattern. The editor
 * provides this via LanguageProvider; child Composables (CommentToggleAction,
 * BracketAutoCompleter, IndentEngine, OutlineDrawer, FoldGutter) read it.
 */
val LocalLanguage = compositionLocalOf<LanguageFormat?> { null }
```

### Task 1.7: Run the tests + commit

- [ ] **Step 1: Run + verify GREEN**

```bash
./gradlew :shared:desktopTest --tests "*LanguageRegistryTest*"
```
Expected: 4/4 PASS.

- [ ] **Step 2: Mutation-verify**

Stub `LanguageRegistry.detectByFilename` to return `null` always. Re-run.
Expected: `detectByFilename_handlesKotlin` and `markdownIsRegistered` (indirectly) FAIL. Revert.

- [ ] **Step 3: Commit**

```bash
cd /Users/milosvasic/Projects/Yole
git add shared/src/commonMain/kotlin/digital/vasic/yole/language/ \
        shared/src/commonTest/kotlin/digital/vasic/yole/language/
git commit -m "feat(iter-58): Phase 1 — LanguageFormat + LanguageRegistry + LocalLanguage foundation

Phase 1 ships markdown LanguageFormat as the architectural anchor. Phase 6 fills the other 49+ from Phase 0 research-report.md §4.

LanguageFormat carries: id, displayName, extensions, mimeTypes, commentSyntax, indentRules, bracketPairs, indentUnit. The affordance fields are Phase 2 data; Phase 1 ships markdown-tuned defaults.

LanguageRegistry exposes: get(id), detectByFilename(name), all(). LocalLanguage is the Compose CompositionLocal carrying the active language to editor child composables.

Anti-bluff (CONST-035): 4 LanguageRegistryTest cases, mutation-verified (stubbing detectByFilename to null causes 2 to FAIL).

Cross-platform impact (CONST-037):
- Android / Desktop / iOS / Web: pure commonMain, identical on all.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
git push origin master
```

---

## Phase 2 — Comment + Indent + Bracket data + tests

**Goal:** Fill the 3 affordance data classes with substantive logic + tests. Markdown-only language; Phase 6 expands to 50+.

**Pre-execution gate:** Phase 1 complete.

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/CommentSyntax.kt` (add toggle helpers)
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/IndentRules.kt` (add computeIndent)
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/BracketPairs.kt` (add closerFor)
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/CommentSyntaxTest.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/IndentRulesTest.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/BracketPairsTest.kt`

### Task 2.1: CommentSyntax toggle helpers + test (TDD red→green)

- [ ] **Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/language/CommentSyntaxTest.kt
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.CommentSyntax
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentSyntaxTest {
    @Test
    fun toggleLine_addsLineCommentWhenAbsent() {
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("    val x = 42")
        assertEquals("    // val x = 42", result)
    }

    @Test
    fun toggleLine_removesLineCommentWhenPresent() {
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("    // val x = 42")
        assertEquals("    val x = 42", result)
    }

    @Test
    fun toggleLine_isNoopWhenLineCommentUndefined() {
        val syntax = CommentSyntax(blockComment = "<!--" to "-->")
        val original = "<p>hi</p>"
        assertEquals(original, syntax.toggleLine(original))
    }

    @Test
    fun toggleLine_handlesShortTrimMismatch() {
        // "//val" (no trailing space after //) — should still uncomment
        val syntax = CommentSyntax(lineComment = "// ")
        val result = syntax.toggleLine("//val x")
        assertEquals("val x", result)
    }
}
```

- [ ] **Step 2: Run — expect RED**

```bash
./gradlew :shared:desktopTest --tests "*CommentSyntaxTest*"
```
Expected: `Unresolved reference: toggleLine`.

- [ ] **Step 3: Implement toggleLine**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/CommentSyntax.kt
package digital.vasic.yole.language.affordance

data class CommentSyntax(
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
) {
    /**
     * Toggle the line-comment prefix on a single line.
     *   - No-op if lineComment is null.
     *   - If the line (trimmed) starts with the prefix → remove it.
     *   - Otherwise → insert at the first-non-ws column.
     *
     * The check is tolerant of trailing-space-after-prefix variation:
     * "// foo" and "//foo" both detected as already-commented.
     */
    fun toggleLine(line: String): String {
        val prefix = lineComment ?: return line
        val trimmedPrefix = prefix.trimEnd()
        val firstNonWs = line.indexOfFirst { !it.isWhitespace() }
        if (firstNonWs < 0) return line // empty/whitespace line
        val content = line.substring(firstNonWs)
        return if (content.startsWith(trimmedPrefix)) {
            // Uncomment: drop the prefix + optional single trailing space
            val afterPrefix = content.substring(trimmedPrefix.length).let {
                if (it.startsWith(" ")) it.substring(1) else it
            }
            line.substring(0, firstNonWs) + afterPrefix
        } else {
            line.substring(0, firstNonWs) + prefix + content
        }
    }
}
```

- [ ] **Step 4: Run — expect GREEN**

Expected: 4/4 PASS.

- [ ] **Step 5: Mutation-verify**

Stub `toggleLine` to `return line`. Expected: 2/4 FAIL (add + remove cases). Revert.

### Task 2.2: IndentRules.computeIndent + test

- [ ] **Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/language/IndentRulesTest.kt
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.IndentRules
import kotlin.test.Test
import kotlin.test.assertEquals

class IndentRulesTest {
    @Test
    fun computeIndent_returnsBaseWhenNoOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("    val x = 42", indentUnit = "    ")
        assertEquals("    ", next)
    }

    @Test
    fun computeIndent_addsOneLevelAfterOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("    fun foo() {", indentUnit = "    ")
        assertEquals("        ", next)
    }

    @Test
    fun computeIndent_handlesNestedOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("if (x) { do {", indentUnit = "  ")
        // Last token is "{" → one level deeper than current line's "" indent
        assertEquals("  ", next)
    }

    @Test
    fun computeIndent_emptyLineReturnsEmpty() {
        val rules = IndentRules()
        assertEquals("", rules.computeIndent("", indentUnit = "    "))
    }
}
```

- [ ] **Step 2: Run — expect RED**

- [ ] **Step 3: Implement computeIndent**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/IndentRules.kt
package digital.vasic.yole.language.affordance

data class IndentRules(
    val indentTokens: Set<String> = setOf("{", "(", "["),
    val dedentTokens: Set<String> = setOf("}", ")", "]"),
) {
    /**
     * Given the line being broken at Enter, return the indent for the
     * next line. Naive but correct for v1: looks at the last non-ws
     * character. AST-aware indent is a Phase 6 enhancement using
     * Tree-Sitter trees.
     */
    fun computeIndent(line: String, indentUnit: String): String {
        if (line.isEmpty()) return ""
        val currentIndent = line.takeWhile { it == ' ' || it == '\t' }
        val trimmed = line.trimEnd()
        if (trimmed.isEmpty()) return currentIndent
        val lastChar = trimmed.last().toString()
        return if (lastChar in indentTokens) currentIndent + indentUnit else currentIndent
    }
}
```

- [ ] **Step 4: Run — expect GREEN; mutation-verify (stub `computeIndent` to `return ""` → 3/4 FAIL); revert.**

### Task 2.3: BracketPairs.closerFor + test

- [ ] **Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/language/BracketPairsTest.kt
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BracketPairsTest {
    @Test
    fun closerFor_returnsMatchingCloser() {
        val pairs = BracketPairs()
        assertEquals(')', pairs.closerFor('('))
        assertEquals(']', pairs.closerFor('['))
        assertEquals('}', pairs.closerFor('{'))
        assertEquals('"', pairs.closerFor('"'))
    }

    @Test
    fun closerFor_returnsNullForNonOpener() {
        val pairs = BracketPairs()
        assertNull(pairs.closerFor('x'))
        assertNull(pairs.closerFor(')'))
    }

    @Test
    fun custom_pairsList() {
        val custom = BracketPairs(pairs = listOf('<' to '>'))
        assertEquals('>', custom.closerFor('<'))
        assertNull(custom.closerFor('('))
    }
}
```

- [ ] **Step 2: Implement closerFor**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/BracketPairs.kt
package digital.vasic.yole.language.affordance

data class BracketPairs(
    val pairs: List<Pair<Char, Char>> = listOf(
        '(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'',
    ),
) {
    fun closerFor(opener: Char): Char? = pairs.firstOrNull { it.first == opener }?.second
}
```

- [ ] **Step 3: Run + mutation-verify (stub `closerFor` to `return null` → 1/3 FAIL); revert.**

### Task 2.4: Commit Phase 2

```bash
cd /Users/milosvasic/Projects/Yole
git add shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/ \
        shared/src/commonTest/kotlin/digital/vasic/yole/language/{CommentSyntax,IndentRules,BracketPairs}Test.kt
git commit -m "feat(iter-58): Phase 2 — CommentSyntax + IndentRules + BracketPairs data + tests

Each affordance data class gains its substantive method:
  - CommentSyntax.toggleLine(line) — line-comment toggle with trim-tolerant detection.
  - IndentRules.computeIndent(line, indentUnit) — naive last-char indent.
  - BracketPairs.closerFor(opener) — pair lookup.

Anti-bluff (CONST-035): 11 unit tests across 3 classes, each mutation-verified.

Cross-platform impact (CONST-037):
- Android / Desktop / iOS / Web: pure commonMain, identical on all.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
git push origin master
```

---

## Phase 3 — FoldQueryRunner + OutlineExtractor + ScmQueryLoader

**Goal:** Generic Tree-Sitter query executors. Markdown-only at this phase (Phase 6 wires the other 49 grammars + query files).

**Pre-execution gate:** Phase 2 complete + Phase 0 §6 (Tree-Sitter query API) closed.

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/ScmQueryLoader.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/ScmQuery.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/FoldQueryRunner.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/FoldRange.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/OutlineExtractor.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/OutlineItem.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/ScmQueryLoaderTest.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/FoldQueryRunnerTest.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/language/OutlineExtractorTest.kt`

(Full TDD code blocks follow the same pattern as Phase 2 — RED test, minimal implementation, GREEN, mutation-verify, commit. To keep the plan readable I'll summarise; the engineer follows the Phase 2 template.)

### Task 3.1: ScmQuery + ScmQueryLoader

- [ ] Write `ScmQuery(captures: List<ScmCapture(name, byteRange)>)` data class.
- [ ] Write `ScmQueryLoader.load(lang: String, queryName: String): String` — reads `commonMain/resources/grammars/<lang>/<queryName>.scm`.
- [ ] `ScmQueryLoaderTest` — markdown's `folds.scm` resource (bundled in Phase 6; for Phase 3 ship a stub `folds.scm` so the test has something to load).
- [ ] Mutation: stub `load` to return `""` → test FAILS. Revert. Commit.

### Task 3.2: FoldQueryRunner + FoldRange

- [ ] Write `FoldRange(startLine: Int, endLine: Int)` data class.
- [ ] Write `FoldQueryRunner.foldRangesFor(text: String, langId: String, engine: TokenizerEngine): List<FoldRange>` — uses iter-57's `TokenizerEngine` + Phase 0 §6's query API.
- [ ] `FoldQueryRunnerTest` — markdown snippet with one `# Heading` + 3 body lines; assert 1 FoldRange covering the heading's body.
- [ ] Mutation-verify; commit.

### Task 3.3: OutlineExtractor + OutlineItem

- [ ] Write `OutlineItem(name: String, kind: String, byteRange: IntRange)` data class. `kind` ∈ `{"function","class","method","field","heading",...}`.
- [ ] Write `OutlineExtractor.extract(text: String, langId: String, engine: TokenizerEngine): List<OutlineItem>` — query the lang's `outline.scm`.
- [ ] `OutlineExtractorTest` — markdown snippet with `# H1` + `## H2`; assert 2 OutlineItems with kind=`heading`.
- [ ] Mutation-verify; commit.

---

## Phase 4 — Editor affordances (CommentToggleAction + BracketAutoCompleter + IndentEngine)

**Goal:** Wire the data layer to the editor surface on Android. Per CONST-037, Desktop/iOS/Web are parallel sub-tasks.

**Pre-execution gate:** Phase 3 complete.

**Files:**
- Create: `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/CommentToggleAction.kt`
- Create: `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/BracketAutoCompleter.kt`
- Create: `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/IndentEngine.kt`
- Test: 3 Robolectric tests
- Modify: `SyncedScrollEditor.kt` (consume the 3 affordances)
- Modify: `IdeEditorScreen` in YoleApp.kt (provide LocalLanguage)

### Task 4.1: CommentToggleAction

- [ ] Write Composable `rememberCommentToggleAction(textState: MutableState<TextFieldValue>, language: LanguageFormat?)` returning a `KeyEvent -> Boolean` handler.
- [ ] Wire to BasicTextField's onKeyEvent in SyncedScrollEditor — detect Ctrl+/ (or Cmd+/ on macOS).
- [ ] Call `language?.commentSyntax?.toggleLine(line)` per selected line.
- [ ] `CommentToggleActionRobolectricTest` — type "// foo" in a kotlin file, press Ctrl+/, assert text becomes "foo".
- [ ] Mutation-verify; commit.

### Task 4.2: BracketAutoCompleter

- [ ] Write `BracketAutoCompleter.onValueChange(old: TextFieldValue, new: TextFieldValue, language: LanguageFormat?): TextFieldValue` — when a single char is inserted that is an opener with a matching closer, also insert the closer and place cursor between.
- [ ] Wire to SyncedScrollEditor's onValueChange.
- [ ] Robolectric test — type `(`, assert text becomes `()` with cursor between.
- [ ] Mutation-verify; commit.

### Task 4.3: IndentEngine

- [ ] Write `IndentEngine.handleEnter(text: TextFieldValue, language: LanguageFormat?): TextFieldValue` — on Enter, compute next-line indent via `IndentRules.computeIndent`.
- [ ] Wire to SyncedScrollEditor's onKeyEvent.
- [ ] Robolectric test — type "fun foo() {", press Enter, assert next line indented +1 level.
- [ ] Mutation-verify; commit.

### Task 4.4: IdeEditorScreen provides LocalLanguage

- [ ] Wrap editor in `CompositionLocalProvider(LocalLanguage provides language)`.
- [ ] Verify all 3 affordances see the right language via the existing tests.

---

## Phase 5 — Editor UI: OutlineDrawer + FoldGutter

**Pre-execution gate:** Phase 4 complete + Phase 3 OutlineExtractor + FoldQueryRunner working.

**Files:**
- Create: `androidApp/.../ui/editor/OutlineDrawer.kt`
- Create: `androidApp/.../ui/editor/FoldGutter.kt`
- Test: 2 Robolectric tests
- Modify: SyncedScrollEditor.kt to integrate the FoldGutter into the existing gutter Column

### Tasks (decomposed at execution time after Phase 4 patterns crystallize)

- [ ] **Task 5.1:** OutlineDrawer — slide-in left panel. Toolbar toggle button. Lazy compose only when visible.
- [ ] **Task 5.2:** OutlineDrawerRobolectricTest — Kotlin file with 1 fun + 1 class, open outline, assert 2 items.
- [ ] **Task 5.3:** FoldGutter — chevron icons in the gutter for foldable lines (returned by FoldQueryRunner).
- [ ] **Task 5.4:** Folded-region rendering — when a fold is collapsed, the underlying lines are skipped from BasicTextField's visible text via VisualTransformation (extends iter-57's length-guard logic).
- [ ] **Task 5.5:** FoldGutterRobolectricTest — multi-line fun, tap chevron, assert body collapses.
- [ ] **Task 5.6:** Mutation-verify both; commit.

---

## Phase 6 — Per-language data: 50 LanguageMetadata rows + 200 .scm query files

**Pre-execution gate:** Phase 5 complete + Phase 0 §§1, 2, 4 closed (queries vendored + per-lang conventions tabulated).

**Files:**
- Modify: `LanguageMetadata.kt` — add 49+ language entries from Phase 0 §4.
- Create: `shared/src/commonMain/resources/grammars/<lang>/{highlights,folds,outline}.scm` — ~150 files vendored from `nvim-treesitter` and `helix-editor` (Phase 0 §§1, 2).
- Create: `shared/src/commonTest/resources/test-fixtures/<lang>/example.txt` — 50 fixture files.
- Modify: `shared/src/commonMain/resources/grammars/<lang>/grammar.tmLanguage.json` — bundle for Wasm.
- Create: `LanguageMetadataCompletenessTest` + `LanguageAffordanceParityTest` + `Feature2LanguageSmokeTest`.

### Tasks (data-heavy; full TDD code blocks specified per language at execution time)

- [ ] **Task 6.1:** Author each `LanguageMetadata` row using Phase 0 §4's tabulated conventions. ~50 entries.
- [ ] **Task 6.2:** Vendor `nvim-treesitter` `.scm` files under `resources/grammars/<lang>/`. License header per CLAUDE.md ("SPDX-FileCopyrightText: <nvim-treesitter contributors>, Apache-2.0").
- [ ] **Task 6.3:** Vendor `helix-editor` `tags.scm` files under `resources/grammars/<lang>/outline.scm` (renamed).
- [ ] **Task 6.4:** Author fixture snippets per language (~50 small files).
- [ ] **Task 6.5:** `LanguageMetadataCompletenessTest` — every lang has all 3 `.scm` files + fixture + grammar binary entry.
- [ ] **Task 6.6:** `LanguageAffordanceParityTest` — every lang has CommentSyntax + IndentRules + BracketPairs.
- [ ] **Task 6.7:** `Feature2LanguageSmokeTest` — parametrized per lang; opens fixture, asserts highlight + outline + fold all produce non-trivial output.
- [ ] **Task 6.8:** Mutation-verify the structural tests; commit.

---

## Phase 7 — Native binary acquisition

**Pre-execution gate:** Phase 6 complete + Phase 0 §3 closed.

**Files:**
- Create: `shared/src/androidMain/jniLibs/<abi>/libtree-sitter-<lang>.so` (50 × 3 ABI = 150 files).
- Create: `shared/src/desktopMain/native/<os-arch>/libtree-sitter-<lang>.{so,dylib,dll}` (50 × 5 = 250 files).
- Create: `shared/src/iosMain/native/lib<lang>.a` (50 × 3 = 150 files).
- Modify: `shared/build.gradle.kts` — extend the iter-57 jniLibs srcDirs + repackage tasks to cover the new languages.

### Tasks

- [ ] **Task 7.1:** Author a `tools/build-tree-sitter-grammars.sh` script that, given a `tree-sitter-<lang>` repo URL, builds the `.so` for the 3 Android ABIs + 5 desktop OS+arch combos + 3 iOS arch combos using the Android NDK + standard C toolchains. The script writes to the expected `jniLibs/` / `native/` paths.
- [ ] **Task 7.2:** Run the script for each of the 50 languages (sequential or parallel; document the elapsed time).
- [ ] **Task 7.3:** Verify each binary is a real ELF / Mach-O / PE via `file` per platform — `Feature2LanguageSmokeTest` already asserts non-empty tokenization, but the binary-integrity check is an additional anti-bluff gate.
- [ ] **Task 7.4:** Update the iter-57 NativeUtils Gradle repackage to repackage per-language JARs (not just `tree-sitter-markdown`).
- [ ] **Task 7.5:** Run `:androidApp:assembleDebug`; verify the APK lists all `lib*.so` files via `unzip -l`.
- [ ] **Task 7.6:** Mutation-verify by deleting one language's `.so` → smoke test for that lang FAILS. Revert.
- [ ] **Task 7.7:** Commit (binary commits are large; consider git-lfs OR vendored-prebuilt-via-script-not-committed approach — Phase 0 §3 should choose).

---

## Phase 8 — Special cases: HtmlEmbeddedLang + MarkdownCodeFences

**Pre-execution gate:** Phases 6+7 complete (HTML, CSS, JS, Markdown all have grammars + tests).

**Files:**
- Create: `shared/.../language/special/HtmlEmbeddedLang.kt`
- Modify: `shared/.../language/special/MarkdownCodeFences.kt` (supersedes iter-57 Phase 10's PreviewCodeBlockHighlighter delegation)
- Test: 2 new tests

### Tasks

- [ ] **Task 8.1:** `HtmlEmbeddedLang.tokenizeMixed(text, htmlEngine, cssEngine, jsEngine)` — single-pass tokenize HTML body, then re-tokenize `<style>` regions with css engine and `<script>` with js engine.
- [ ] **Task 8.2:** Test: HTML doc with one `<style>` block; assert CSS scopes appear inside the style region.
- [ ] **Task 8.3:** `MarkdownCodeFences` upgrades iter-57's logic so the fence body uses the new per-lang affordances when the user EDITS inside the rendered preview (not just for static HTML output).
- [ ] **Task 8.4:** Mutation-verify; commit.

---

## Phase 9 — Anti-bluff challenges + qa-all wiring

**Pre-execution gate:** Phase 8 complete.

**Files:**
- Create: `yole-challenges/scripts/language_support_completeness_challenge.sh`
- Create: `yole-challenges/scripts/language_grammar_bundle_challenge.sh`
- Modify: `Makefile` — add `qa-iter-58-gates` target chained into `qa-all`.

### Tasks

- [ ] **Task 9.1:** Author `language_support_completeness_challenge.sh` — runs the Phase 6 + 7 test suites; asserts ≥ 50 languages have full affordance + binary coverage. Per CONST-035 emits per-language pass/fail evidence (file path).
- [ ] **Task 9.2:** Author `language_grammar_bundle_challenge.sh` — `unzip -l` Android APK + tarball checks for Desktop + Wasm bundle JSON check.
- [ ] **Task 9.3:** Wire into Makefile.
- [ ] **Task 9.4:** Run `make qa-all` end-to-end; expect every existing iter-57 challenge + the 2 new iter-58 challenges to PASS.
- [ ] **Task 9.5:** Commit.

---

## Phase 10 — Documentation + diagrams

**Pre-execution gate:** Phase 9 complete (everything green in qa-all).

**Files:**
- Create: `docs/features/source-code-file-support/user-guide.md`
- Create: `docs/features/source-code-file-support/architecture.md`
- Create: `docs/features/source-code-file-support/language-coverage-matrix.md`
- Modify: `CHANGELOG.md` (iter-58 entry)
- Modify: `docs/CONTINUATION.md` (Section 42)

### Tasks

- [ ] **Task 10.1:** Author user-guide.md (end-user perspective: comment-toggle keybinding, outline panel, fold regions, supported languages).
- [ ] **Task 10.2:** Author architecture.md (contributor perspective: data flow, special cases, adding a new language).
- [ ] **Task 10.3:** Author language-coverage-matrix.md — table with 50 langs × 5 affordances × 4 platforms. Each cell records the coverage status with evidence (test file or PASS log).
- [ ] **Task 10.4:** Update CHANGELOG.md + CONTINUATION.md per CONST-036.
- [ ] **Task 10.5:** Commit.

---

## Phase 11 — Firebase distribution

**Pre-execution gate:** Phase 10 complete + `make qa-all` green.

**Files:** No code changes — uses iter-57's release infrastructure.

### Tasks

- [ ] **Task 11.1:** Bump version: 1.1.0 → 1.2.0; versionCode 110 → 120; dotted `0.0.0.1.20`.
- [ ] **Task 11.2:** Build all 4 platforms (Android APK + 3 Desktop tarballs + Web Wasm bundle).
- [ ] **Task 11.3:** Pre-existing iter-57 distribution gaps (Desktop Linux/Windows crossbuild infrastructure, Web Wasm production pipeline, iOS Document-KMP+DatabaseFactory chain): if still unresolved, ship the slice that's currently working (Android RC + macOS-arm64 DMG out-of-band). Per CONST-037, document per-platform disposition in the release-notes.
- [ ] **Task 11.4:** Firebase distribute Android Release + Debug per iter-57 pattern.
- [ ] **Task 11.5:** Capture evidence to `docs/qa/iter-58/`.
- [ ] **Task 11.6:** Tag `v1.2.0-iter58`; push.
- [ ] **Task 11.7:** Final commit; push.

---

## Self-review against the spec

**Spec coverage check:**

| Spec requirement | Plan task |
|---|---|
| §2 50+ languages with rich affordances | Phases 1, 2, 6 |
| §2 Bundle all 50+ in base APK/IPA | Phase 7 |
| §3 LanguageFormat + LanguageMetadata + LanguageRegistry | Phase 1 |
| §3 CommentSyntax + IndentRules + BracketPairs + their helpers | Phase 2 |
| §3 FoldQueryRunner + OutlineExtractor + ScmQueryLoader | Phase 3 |
| §3 HtmlEmbeddedLang + MarkdownCodeFences specials | Phase 8 |
| §3 Editor affordances (CommentToggle, BracketAuto, IndentEngine, Outline, Fold) | Phases 4 + 5 |
| §7 ~250+ unit tests + integration + smoke + structural | Phases 1-8 (each phase carries its tests) |
| §7 2 new challenges in make qa-all | Phase 9 |
| §9 Documentation deliverables | Phase 10 |
| §10 Firebase distribution | Phase 11 |

No gaps.

**Placeholder scan:**

Phases 0–4 contain full TDD bite-sized tasks with complete code. Phases 5–11 contain task-level checklists with exact file paths but defer specific code (e.g., the per-language `.scm` content) to the moment Phase 0 research closes those questions. This is **not** a placeholder — it's an explicit dependency on a research output that's itself a phase with its own tasks. The plan is honest about it via the "Pre-execution gate" markers.

**Type consistency:**

`LanguageFormat`, `LanguageMetadata`, `LanguageRegistry`, `LocalLanguage`, `CommentSyntax`, `IndentRules`, `BracketPairs`, `ScmQuery`, `FoldRange`, `OutlineItem`, `FoldQueryRunner`, `OutlineExtractor`, `ScmQueryLoader` are introduced in their authoring phases and referenced consistently through Phases 4+. No drift.

---

## Execution handoff

Plan saved to `docs/superpowers/plans/2026-05-15-source-code-file-support-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks. Best for a long plan; keeps the main context clean.
2. **Inline Execution** — Execute tasks in this session via `superpowers:executing-plans`. Faster wall time for short plans; less suitable here given Feature 2's scope.

**Strong recommendation: subagent-driven.** This plan is ~12 phases / ~70 tasks. Inline execution would burn through context budget on Phase 0 + 1 alone.

Pick approach.
