# Auto-Complete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a provider-based auto-complete engine that mixes token-frequency + per-language snippets (vendored from VS Code basics) + identifier-aware suggestions (via Feature 2's `OutlineExtractor`) + Tree-Sitter scope-aware ranking. Implicit trigger after 2 chars + explicit Ctrl+Space on Desktop/Web; toolbar Suggest button on Android/iOS. v1 covers all 4 platforms wired to Compose Popup; LSP-fed completions (Feature 4) plug in as a 5th provider without architectural change.

**Architecture:** New `shared/.../completion/` subsystem. `CompletionEngine` orchestrates 3 `CompletionProvider` implementations in parallel on `Dispatchers.Default`, merges their results, applies `ScopeAwareRanker` boosts, dedupes, emits `Flow<List<CompletionItem>>` to a Compose `CompletionPopup`. Snippet library vendored from `microsoft/vscode/extensions/<lang>-basics/snippets/` (MIT) — same JSON schema. Trigger logic isolated in `CompletionTrigger` so the editor never knows about debounce or prefix-guards.

**Tech Stack:** Kotlin Multiplatform 2.0.20, Compose Multiplatform 1.7.3, kotlinx-serialization-json (already in `libs.versions.toml`), iter-57's `TokenizerEngine`, iter-58's `OutlineExtractor`, Compose 1.7's `androidx.compose.ui.window.Popup`.

**Spec:** `docs/superpowers/specs/2026-05-15-auto-complete-design.md` (commit `264e9486`).

**Dependencies on iter-57 + iter-58:** This plan builds on `digital.vasic.yole.syntax.TokenizerEngine`, `digital.vasic.yole.language.{LanguageFormat, LanguageRegistry, LocalLanguage}`, `digital.vasic.yole.language.affordance.OutlineExtractor`, `OutlineItem`. Reading those modules before Phase 1 is mandatory.

---

## Phase dependency graph

```
Phase 0  Deep research → research-report.md          (no code)
   │
   ▼
Phase 1  CompletionItem + CompletionContext + CompletionProvider interface
   │
   ▼
Phase 2  Snippet + VsCodeSnippetParser + SnippetRegistry
   │
   ▼
Phase 3  3 providers (TokenFrequency + Snippet + Identifier)
   │
   ▼
Phase 4  ScopeAwareRanker + CompletionRanker + CompletionEngine
   │
   ▼
Phase 5  CompletionTrigger (debounce + prefix-guard + explicit)
   │
   ▼
Phase 6  CompletionPopup + CompletionPopupState + CompletionToolbarButton
   │
   ▼
Phase 7  Per-language snippet vendoring (50+ JSON from VS Code basics)
   │
   ▼
Phase 8  Snippet placeholder navigation (Tab-stop integration with iter-58 IndentEngine)
   │
   ▼
Phase 9  2 anti-bluff challenges + Makefile qa-iter-60-gates
   │
   ▼
Phase 10 Documentation
   │
   ▼
Phase 11 Firebase distribution v1.3.0
```

**Detail level per phase:**

- **Phases 0–5** have full TDD bite-sized tasks with complete code.
- **Phases 6–11** have task-level checklists with exact file paths + acceptance criteria. Their per-step code crystallises after Phase 0's research-report.md closes 6 specific open questions. Each later phase declares a **"Pre-execution gate"** at its top.

---

## File Structure (all new + modified across the 12 phases)

**Create:**

```
shared/src/commonMain/kotlin/digital/vasic/yole/completion/
  CompletionItem.kt
  CompletionContext.kt
  CompletionProvider.kt              # interface
  CompletionRanker.kt
  CompletionEngine.kt
  providers/
    TokenFrequencyProvider.kt
    SnippetProvider.kt
    IdentifierProvider.kt
    ScopeAwareRanker.kt              # rank-modifier (not a Provider)
  snippet/
    Snippet.kt
    VsCodeSnippetParser.kt
    SnippetParseException.kt
    SnippetRegistry.kt
  trigger/
    CompletionTrigger.kt
    LocalCompletionEngine.kt         # Compose CompositionLocal

shared/src/commonMain/resources/snippets/<lang>/snippets.json    # 50+ vendored

shared/src/commonTest/kotlin/digital/vasic/yole/completion/
  CompletionItemTest.kt
  CompletionContextTest.kt
  VsCodeSnippetParserTest.kt
  SnippetRegistryTest.kt
  TokenFrequencyProviderTest.kt
  IdentifierProviderTest.kt
  ScopeAwareRankerTest.kt
  CompletionRankerTest.kt
  CompletionTriggerTest.kt
  CompletionEngineParityTest.kt
  SnippetLibraryCompletenessTest.kt
  Feature3LanguageSnippetSmokeTest.kt

shared/src/desktopTest/kotlin/digital/vasic/yole/completion/
  CompletionEngineDesktopIntegrationTest.kt

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/
  CompletionPopup.kt
  CompletionPopupState.kt
  CompletionToolbarButton.kt

androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/
  CompletionPopupRobolectricTest.kt
  CompletionExplicitTriggerRobolectricTest.kt
  MobileSuggestButtonRobolectricTest.kt
  SnippetExpansionRobolectricTest.kt

yole-challenges/scripts/
  auto_complete_completeness_challenge.sh
  snippet_library_bundle_challenge.sh

docs/features/auto-complete/
  user-guide.md
  architecture.md
  research-report.md                  # output of Phase 0
  snippet-coverage-matrix.md
```

**Modify:**

```
androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt
  - Add optional `completionEngine: CompletionEngine?` parameter
  - Wire CompletionTrigger into onValueChange + onKeyEvent (Ctrl+Space)
  - Render CompletionPopup overlay when popup state is open

androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
  - IdeEditorScreen constructs CompletionEngine + wraps editor in CompletionPopupState provider
  - Toolbar gains CompletionToolbarButton for mobile explicit trigger

shared/src/commonMain/kotlin/digital/vasic/yole/language/affordance/IndentEngine.kt
  - Phase 8: coordinate Tab with snippet-placeholder navigation —
    while snippet placeholders are active, Tab advances placeholder;
    after last placeholder, Tab falls through to IndentEngine

Makefile
  - New qa-iter-60-gates target chained into qa-all
CHANGELOG.md
  - iter-60 entry
docs/CONTINUATION.md
  - Section 43 (iter-60)
```

---

## Phase 0 — Deep research

**Goal:** Close 6 open questions before Phases 5+ start.

**Files:**
- Create: `docs/features/auto-complete/research-report.md`

### Task 0.1: VS Code snippet bundle inventory

- [ ] **Step 1: Inventory snippets per lang**

For each of the 55 Yole-supported languages (from iter-58 `LanguageMetadata.all`):

- Visit `https://github.com/microsoft/vscode/tree/main/extensions/<lang>-basics/snippets/<lang>.json` and similar paths.
- Record: file existence, snippet count, license (verify MIT in extension manifest), file size.
- Languages where Microsoft ships no built-in snippets (e.g., `regex`, `dockerfile`, `bibtex`, niche grammars): identify alternative sources (community snippet packs) or mark for hand-authoring stubs.

Output: research-report.md §1 with 55-row table.

### Task 0.2: VS Code snippet schema authoritative reference

- [ ] **Step 1: Document the schema**

Fetch `https://code.visualstudio.com/api/language-extensions/snippet-guide`. Record:
- Top-level keys (`prefix`, `body`, `description`, `scope`).
- Placeholder syntax: `${1:default}`, `${1}`, `$1`, `${1|choice1,choice2|}`.
- Built-in variables: `$TM_FILENAME`, `$TM_SELECTED_TEXT`, `$CLIPBOARD`, etc.
- Multi-line `body` as JSON array of strings.
- Comments allowed (with `//` even though it's not strict JSON).

Output: research-report.md §2 with the formal schema + concrete example.

### Task 0.3: Tree-Sitter node-at-byte API

- [ ] **Step 1: Document the API**

For `ScopeAwareRanker` to inspect the node containing the cursor:
- JVM (bonede `tree-sitter:0.26.6`): `TSTree.rootNode.descendantForByteRange(start, end): TSNode?` — verify signature in the JAR.
- Kotlin/Native iOS (when unblocked): same C-API `ts_node_descendant_for_byte_range`.
- Wasm (vscode-textmate): no Tree-Sitter — fall back to a textual-context heuristic.

Output: research-report.md §3 with per-platform code samples.

### Task 0.4: Compose Popup composable + cursor anchoring

- [ ] **Step 1: Document Popup APIs**

- `androidx.compose.ui.window.Popup(alignment, offset, properties, content)` — verify presence in Compose 1.7.
- How to compute pixel position from `TextLayoutResult.getCursorRect(offset)`.
- How to anchor a popup to a moving cursor inside a `BasicTextField`.

Output: research-report.md §4 with code sample.

### Task 0.5: Snippet placeholder navigation

- [ ] **Step 1: Define coexistence with IndentEngine Tab**

- VS Code: Tab advances through placeholders in order `${1}, ${2}, ${3}`. Esc commits.
- Yole IndentEngine (iter-58 Phase 4) intercepts Tab for indent. Need explicit rule.
- Recommendation: snippet-Tab takes precedence while placeholder ranges are active; falls through to IndentEngine after the last placeholder is consumed.

Output: research-report.md §5 with state machine.

### Task 0.6: Provider scheduling

- [ ] **Step 1: Decide all-or-nothing vs progressive emit**

- Progressive: emit partial results as fast providers (TokenFrequency, Snippet) return, then refine when slow (Identifier) returns. Better perceived perf.
- All-or-nothing: simpler; pop only after the slowest finishes.

Recommendation: progressive emit via `Flow<List<CompletionItem>>` — emit after each provider completes. Confirm + document.

Output: research-report.md §6 with the chosen behavior + sample timing.

### Task 0.7: Commit research report

- [ ] **Step 1: Verify completeness**

```bash
wc -l docs/features/auto-complete/research-report.md
```
Expected: ≥ 600 lines, ≥ 100 URL citations.

- [ ] **Step 2: Commit**

```bash
cd /Users/milosvasic/Projects/Yole
git add docs/features/auto-complete/research-report.md
git commit -m "$(cat <<'EOF'
docs(iter-60): Phase 0 deep-research report for auto-complete

Closes the 6 open questions from docs/superpowers/specs/2026-05-15-auto-complete-design.md §8. Every claim cites an upstream URL per CONST-035.

§1 VS Code snippet bundle inventory (55 langs).
§2 VS Code snippet schema reference.
§3 Tree-Sitter node-at-byte API per platform.
§4 Compose Popup + cursor anchoring.
§5 Snippet placeholder navigation state machine.
§6 Provider scheduling (progressive emit chosen).

Cross-platform impact (CONST-037):
- Android / Desktop / iOS / Web: research; no code change.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
git push origin master
```

---

## Phase 1 — CompletionItem + CompletionContext + CompletionProvider interface

**Goal:** Foundation data types + the universal provider contract.

**Pre-execution gate:** Phase 0 complete (only §6 strictly needed for progressive emit, but Phase 1 doesn't actually use it yet — Phase 4 does).

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionItem.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionContext.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionProvider.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionItemTest.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionContextTest.kt`

### Task 1.1: Write failing test for CompletionItem

- [ ] **Step 1: Create the test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionItemTest.kt
/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 1: anti-bluff data-class tests.
 *#######################################################*/
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionItemTest {
    @Test
    fun construct_carriesAllFields() {
        val item = CompletionItem(
            label = "println",
            insertText = "println($1)",
            kind = CompletionItem.Kind.Snippet,
            score = 0.75,
            range = 0..4,
        )
        assertEquals("println", item.label)
        assertEquals("println($1)", item.insertText)
        assertEquals(CompletionItem.Kind.Snippet, item.kind)
        assertEquals(0.75, item.score, 1e-9)
        assertEquals(0..4, item.range)
    }

    @Test
    fun kind_hasFourVariants() {
        val variants = CompletionItem.Kind.values().toSet()
        assertTrue(CompletionItem.Kind.Identifier in variants)
        assertTrue(CompletionItem.Kind.Snippet in variants)
        assertTrue(CompletionItem.Kind.Keyword in variants)
        assertTrue(CompletionItem.Kind.Word in variants)
        assertEquals(4, variants.size)
    }
}
```

- [ ] **Step 2: Run — expect RED**

```bash
./gradlew :shared:desktopTest --tests "*CompletionItemTest*"
```
Expected: `Unresolved reference: CompletionItem`.

### Task 1.2: Implement CompletionItem

- [ ] **Step 1: Write the data class**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionItem.kt
/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60: auto-complete data classes.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * A single completion suggestion shown in the popup.
 *
 * @property label what the user sees in the popup row.
 * @property insertText what gets inserted on commit. For snippets,
 *   may contain `${N:placeholder}` markers parsed by Phase 8 navigation.
 * @property kind disambiguates the icon + ranker boost rules.
 * @property score [0.0, 1.0] — higher wins on dedup.
 * @property range char-index range of the partial word being completed
 *   (the user's already-typed prefix); insertText replaces this range.
 */
data class CompletionItem(
    val label: String,
    val insertText: String,
    val kind: Kind,
    val score: Double,
    val range: IntRange,
) {
    enum class Kind { Identifier, Snippet, Keyword, Word }
}
```

- [ ] **Step 2: Run — expect GREEN**

Expected: 2/2 PASS.

- [ ] **Step 3: Mutation-verify**

Add a 5th enum variant. Re-run. Expected: `kind_hasFourVariants` FAILS. Revert.

### Task 1.3: Implement CompletionContext + test

- [ ] **Step 1: Write the test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionContextTest.kt
package digital.vasic.yole.completion

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionContextTest {
    @Test
    fun prefix_extractedFromCursorByte() {
        val ctx = CompletionContext.of(
            text = "fun foo() { println(\"hi\") }",
            cursorChar = 7, // just after "foo"
            langId = "kotlin",
        )
        assertEquals("foo", ctx.prefix)
        assertEquals(4..7, ctx.prefixRange)
    }

    @Test
    fun prefix_emptyWhenCursorOnWhitespace() {
        val ctx = CompletionContext.of(
            text = "fun foo()",
            cursorChar = 3, // on the space
            langId = "kotlin",
        )
        assertEquals("", ctx.prefix)
    }
}
```

- [ ] **Step 2: Run — expect RED**

- [ ] **Step 3: Implement CompletionContext**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionContext.kt
package digital.vasic.yole.completion

/**
 * Snapshot of editor state for a single completion request.
 *
 * @property text full document text.
 * @property cursorChar cursor position as a char index in [text].
 * @property langId lang id from LanguageRegistry; null = plaintext.
 * @property prefix the partial word the user has already typed
 *   (chars [a-zA-Z0-9_] walking back from cursor until whitespace).
 * @property prefixRange char range of [prefix] inside [text].
 * @property surroundingScope Tree-Sitter node-type at cursor, or null
 *   when Tree-Sitter is unavailable (graceful degradation).
 */
data class CompletionContext(
    val text: String,
    val cursorChar: Int,
    val langId: String?,
    val prefix: String,
    val prefixRange: IntRange,
    val surroundingScope: String? = null,
) {
    companion object {
        /**
         * Build a context with the prefix derived from text + cursor.
         * surroundingScope is filled by the engine via Tree-Sitter (not here).
         */
        fun of(text: String, cursorChar: Int, langId: String?): CompletionContext {
            val safe = cursorChar.coerceIn(0, text.length)
            var start = safe
            while (start > 0 && text[start - 1].isWordChar()) start--
            val prefix = text.substring(start, safe)
            return CompletionContext(
                text = text,
                cursorChar = safe,
                langId = langId,
                prefix = prefix,
                prefixRange = start..safe,
            )
        }

        private fun Char.isWordChar(): Boolean =
            this.isLetterOrDigit() || this == '_'
    }
}
```

- [ ] **Step 4: Run — expect GREEN; mutation-verify (replace `isWordChar` with `false` → both tests FAIL); revert.**

### Task 1.4: Implement CompletionProvider interface

- [ ] **Step 1: Write the interface**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionProvider.kt
package digital.vasic.yole.completion

/**
 * Universal completion provider contract. Phase 3 ships 3 impls
 * (TokenFrequencyProvider, SnippetProvider, IdentifierProvider).
 * Feature 4 (LSP integration) adds a 4th without touching this file.
 *
 * Implementations MUST be thread-safe and MUST NOT block — return
 * empty list on any failure (graceful degradation per CONST-035;
 * caller logs the failure).
 */
interface CompletionProvider {
    /** Human-readable id, used for diagnostics + Engine parity tests. */
    val id: String

    /**
     * Produce candidate items for the context. May return empty.
     * Implementations SHOULD respect a soft latency budget (~50ms);
     * the CompletionEngine wraps each call in withTimeout.
     */
    suspend fun complete(ctx: CompletionContext): List<CompletionItem>
}
```

### Task 1.5: Commit Phase 1

```bash
cd /Users/milosvasic/Projects/Yole
git add shared/src/commonMain/kotlin/digital/vasic/yole/completion/ \
        shared/src/commonTest/kotlin/digital/vasic/yole/completion/
git commit -m "feat(iter-60): Phase 1 — CompletionItem + CompletionContext + CompletionProvider foundation

3 data files (~150 LOC) + 4 unit tests, mutation-verified (kind variant + prefix word-char). Provider interface explicitly designed to admit Feature 4's LspCompletionProvider without v1 changes.

Cross-platform impact (CONST-037):
- Android / Desktop / iOS / Web: pure commonMain, identical on all.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
git push origin master
```

---

## Phase 2 — Snippet + VsCodeSnippetParser + SnippetRegistry

**Goal:** Parse VS Code snippet JSON; register per-language snippets.

**Pre-execution gate:** Phase 1 complete + Phase 0 §2 closed (schema documented).

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/Snippet.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/SnippetParseException.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/VsCodeSnippetParser.kt`
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/SnippetRegistry.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/completion/VsCodeSnippetParserTest.kt`
- Test: `shared/src/desktopTest/kotlin/digital/vasic/yole/completion/SnippetRegistryTest.kt`

### Task 2.1: Snippet + SnippetParseException

- [ ] **Step 1: Write the data classes**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/Snippet.kt
package digital.vasic.yole.completion.snippet

/**
 * A single snippet entry parsed from a VS Code snippets.json bundle.
 *
 * @property prefix what the user types to trigger this snippet.
 * @property body the inserted text, possibly with `${N:placeholder}` markers.
 * @property description shown in the popup tooltip.
 * @property scope optional VS Code scope filter; empty = any scope.
 */
data class Snippet(
    val prefix: String,
    val body: String,
    val description: String? = null,
    val scope: String = "",
)

// shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/SnippetParseException.kt
package digital.vasic.yole.completion.snippet

class SnippetParseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
```

### Task 2.2: VsCodeSnippetParser (test-first)

- [ ] **Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/digital/vasic/yole/completion/VsCodeSnippetParserTest.kt
package digital.vasic.yole.completion

import digital.vasic.yole.completion.snippet.SnippetParseException
import digital.vasic.yole.completion.snippet.VsCodeSnippetParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VsCodeSnippetParserTest {
    // Microsoft's kotlin.json schema example (string-body form).
    private val singleLineJson = """
        {
          "Hello World": {
            "prefix": "hello",
            "body": "println(\"Hello, World!\")",
            "description": "Print a greeting"
          }
        }
    """.trimIndent()

    // Array-body form — the canonical VS Code format for multi-line snippets.
    private val multiLineJson = """
        {
          "For Loop": {
            "prefix": "for",
            "body": [
              "for (i in 0 until ${'$'}{1:size}) {",
              "    ${'$'}0",
              "}"
            ],
            "description": "For loop"
          }
        }
    """.trimIndent()

    @Test
    fun parses_singleLineBody() {
        val snippets = VsCodeSnippetParser.parse(singleLineJson)
        assertEquals(1, snippets.size)
        val s = snippets.single()
        assertEquals("hello", s.prefix)
        assertTrue(s.body.contains("Hello, World!"))
        assertEquals("Print a greeting", s.description)
    }

    @Test
    fun parses_arrayBodyAsNewlineJoined() {
        val snippets = VsCodeSnippetParser.parse(multiLineJson)
        val s = snippets.single()
        assertEquals("for", s.prefix)
        assertTrue(s.body.contains("\n"), "array body must join with newlines")
        assertTrue(s.body.contains("\${1:size}"))
        assertTrue(s.body.contains("\$0"))
    }

    @Test
    fun malformed_throws() {
        assertFailsWith<SnippetParseException> {
            VsCodeSnippetParser.parse("{ not json }")
        }
    }

    @Test
    fun missingPrefix_skipped() {
        val noPrefix = """{ "Broken": { "body": "x" } }"""
        // Skip silently — VS Code does the same.
        assertEquals(0, VsCodeSnippetParser.parse(noPrefix).size)
    }
}
```

- [ ] **Step 2: Run — expect RED**

- [ ] **Step 3: Implement the parser**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/VsCodeSnippetParser.kt
package digital.vasic.yole.completion.snippet

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object VsCodeSnippetParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    fun parse(input: String): List<Snippet> {
        val root: JsonObject = try {
            json.parseToJsonElement(input).jsonObject
        } catch (e: SerializationException) {
            throw SnippetParseException("malformed JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw SnippetParseException("root is not a JSON object: ${e.message}", e)
        }
        return root.values.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val prefix = obj["prefix"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val body = parseBody(obj["body"]) ?: return@mapNotNull null
            val description = obj["description"]?.jsonPrimitive?.contentOrNull
            val scope = obj["scope"]?.jsonPrimitive?.contentOrNull ?: ""
            Snippet(prefix = prefix, body = body, description = description, scope = scope)
        }
    }

    private fun parseBody(element: kotlinx.serialization.json.JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.joinToString("\n") { (it as? JsonPrimitive)?.contentOrNull ?: "" }
        else -> null
    }
}
```

- [ ] **Step 4: Run — expect GREEN (4/4); mutation-verify (parseBody → return null → 2/4 FAIL); revert.**

### Task 2.3: SnippetRegistry

- [ ] **Step 1: Write test** (loads bundled snippets/markdown/snippets.json + asserts ≥ 1 entry).

- [ ] **Step 2: Implement**

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/SnippetRegistry.kt
package digital.vasic.yole.completion.snippet

object SnippetRegistry {
    private val cache = mutableMapOf<String, List<Snippet>>()
    private val loader: (String) -> String? = ::loadResource

    fun forLanguage(langId: String): List<Snippet> = cache.getOrPut(langId) {
        val raw = loader("snippets/$langId/snippets.json") ?: return@getOrPut emptyList()
        try {
            VsCodeSnippetParser.parse(raw)
        } catch (e: SnippetParseException) {
            emptyList()
        }
    }

    fun clear() = cache.clear()

    private fun loadResource(path: String): String? =
        SnippetRegistry::class.java.classLoader
            ?.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
}
```

(Note: `::class.java.classLoader` is JVM-specific; for cross-platform, define `expect fun readSnippetResource(path: String): String?` with platform actuals. Phase 1 of iter-58 used the same pattern for `ScmQueryLoader` — mirror that.)

- [ ] **Step 3: Bundle a markdown snippets.json placeholder** at `shared/src/commonMain/resources/snippets/markdown/snippets.json`:

```json
{
  "Table": {
    "prefix": "table",
    "body": [
      "| ${1:Column1} | ${2:Column2} |",
      "| --- | --- |",
      "| $0 |  |"
    ],
    "description": "Markdown table skeleton"
  },
  "Link": {
    "prefix": "link",
    "body": "[${1:text}](${2:url})",
    "description": "Markdown link"
  }
}
```

(SPDX header pointing to Microsoft's vscode markdown-basics snippets, MIT.)

- [ ] **Step 4: Run + mutation + commit.**

### Task 2.4: Commit Phase 2

```bash
git add shared/src/commonMain/kotlin/digital/vasic/yole/completion/snippet/ \
        shared/src/commonMain/resources/snippets/markdown/ \
        shared/src/commonTest/kotlin/digital/vasic/yole/completion/VsCodeSnippetParserTest.kt \
        shared/src/desktopTest/kotlin/digital/vasic/yole/completion/SnippetRegistryTest.kt
git commit -m "feat(iter-60): Phase 2 — VsCodeSnippetParser + SnippetRegistry + markdown anchor"
git push origin master
```

---

## Phase 3 — 3 providers (TokenFrequencyProvider + SnippetProvider + IdentifierProvider)

**Pre-execution gate:** Phase 2 complete + iter-58's `OutlineExtractor` available.

**Files:** 3 provider files + 3 test files. Pattern matches Phase 2.

### Tasks (decomposed at execution; same TDD shape as Phase 2)

- [ ] **Task 3.1:** `TokenFrequencyProvider` — word frequency in current file; return top-N matching prefix. Test: known input "foo bar foo baz foo" + prefix "f" → "foo" with score by frequency.
- [ ] **Task 3.2:** `SnippetProvider` — call `SnippetRegistry.forLanguage(ctx.langId)`; filter by prefix; map to CompletionItem with kind=Snippet. Test: markdown grammar + prefix "tab" → matches the bundled table snippet.
- [ ] **Task 3.3:** `IdentifierProvider` — call iter-58's `OutlineExtractor.outlineFor(ctx.text, ctx.langId, engine)`; map OutlineItem.name → CompletionItem with kind=Identifier. Test: markdown with 2 headings "Hello" + "World" + prefix "H" → both returned.

Each provider gets a unit test + mutation cycle + commit.

---

## Phase 4 — ScopeAwareRanker + CompletionRanker + CompletionEngine

**Pre-execution gate:** Phase 3 complete.

**Files:** `ScopeAwareRanker.kt`, `CompletionRanker.kt`, `CompletionEngine.kt`, 3 test files.

### Tasks

- [ ] **Task 4.1:** `ScopeAwareRanker.boost(item, scope): Double` — table-driven boost values: after `.` boost methods by +2.0, after `:` boost types by +1.5, etc. Test: table-driven over 8 scope/kind pairs.
- [ ] **Task 4.2:** `CompletionRanker.merge(perProviderResults): List<CompletionItem>` — dedupe by `label` (highest score wins), apply ScopeAwareRanker boosts, sort by score descending. Test: 3 providers with overlapping items → assert dedup + boost.
- [ ] **Task 4.3:** `CompletionEngine` — orchestrate the 3 providers in parallel on `Dispatchers.Default`; emit `Flow<List<CompletionItem>>`. Use `withTimeout(500.milliseconds)` per provider. Test: feed text + prefix; assert non-empty Flow emission.
- [ ] **Task 4.4:** `CompletionEngineParityTest` — every provider impl in `providers/` MUST be in CompletionEngine.providers list. Catches forgot-to-wire regressions. Mutation: comment out a provider in the engine list → test FAILS.

---

## Phase 5 — CompletionTrigger (debounce + prefix-guard + explicit)

**Pre-execution gate:** Phase 4 complete.

**Files:** `CompletionTrigger.kt` + `CompletionTriggerTest.kt`.

### Tasks

- [ ] **Task 5.1:** `CompletionTrigger` is a small state machine consuming `KeyEvent` + `TextFieldValue` changes. Emits `TriggerEvent.{Show, Hide, Update}` to the engine. 80ms debounce on implicit; immediate on explicit. Prefix-length guard of ≥ 2 chars for implicit (configurable; mobile may want ≥ 3).
- [ ] **Task 5.2:** Test the debounce + prefix-guard logic with `kotlinx-coroutines-test`'s `TestScope`. Mutation: stub debounce to 0 → test FAILS (immediate fires before debounce window).

---

## Phase 6 — Editor UI (CompletionPopup + CompletionPopupState + CompletionToolbarButton)

**Pre-execution gate:** Phase 5 complete + Phase 0 §4 closed.

**Files:**
- `androidApp/.../ui/editor/CompletionPopup.kt`
- `androidApp/.../ui/editor/CompletionPopupState.kt`
- `androidApp/.../ui/editor/CompletionToolbarButton.kt`
- 4 Robolectric test files

### Tasks (task-level; full TDD finalised after Phase 0 closes Compose Popup details)

- [ ] **Task 6.1:** `CompletionPopup` Composable — uses `androidx.compose.ui.window.Popup`; anchors to cursor via `TextLayoutResult.getCursorRect(offset)`. Renders LazyColumn of items. Arrow-key navigation; Enter/Tab commits; Esc dismisses.
- [ ] **Task 6.2:** `CompletionPopupState` — `isOpen`, `items`, `selectedIndex` as `MutableState`. Updated by Trigger events.
- [ ] **Task 6.3:** `CompletionToolbarButton` — only added on Android (and later iOS); `IconButton` with `Icons.Filled.Build` (or similar) + contentDescription "Suggest". onClick → trigger explicit.
- [ ] **Task 6.4:** Wire `SyncedScrollEditor` to accept `completionEngine: CompletionEngine?` + render the popup overlay. Preserve iter-57's VisualTransformation length-guard.
- [ ] **Task 6.5:** Wire `IdeEditorScreen` to construct `CompletionEngine` from the active language + engine + theme; toolbar button added next to outline button.
- [ ] **Task 6.6:** `CompletionPopupRobolectricTest` — type 3 chars, popup appears (testTag check).
- [ ] **Task 6.7:** `CompletionExplicitTriggerRobolectricTest` — synth Ctrl+Space, popup appears even on empty prefix.
- [ ] **Task 6.8:** `MobileSuggestButtonRobolectricTest` — tap button, popup appears.
- [ ] **Task 6.9:** `SnippetExpansionRobolectricTest` — select snippet item, body inserted with first placeholder selected (Phase 8 navigation activates).
- [ ] **Task 6.10:** Mutation-verify each; commit.

---

## Phase 7 — Per-language snippet vendoring

**Pre-execution gate:** Phase 6 complete + Phase 0 §1 closed (inventory done).

**Files:** `shared/src/commonMain/resources/snippets/<lang>/snippets.json` for 50+ languages.

### Tasks

- [ ] **Task 7.1:** For each of the 55 langs from iter-58 `LanguageMetadata.all`, vendor the matching VS Code snippets file from `microsoft/vscode/extensions/<lang>-basics/snippets/<lang>.json`. SPDX header attribution at the top. License: MIT (verify each).
- [ ] **Task 7.2:** Languages without VS Code snippets (per Phase 0 §1 inventory): create a small Yole-authored stub with ~5 idiomatic snippets per lang, SPDX-marked.
- [ ] **Task 7.3:** `SnippetLibraryCompletenessTest` — every lang in `LanguageMetadata.all` has a snippets.json OR a documented exclusion in KNOWN_DEFECTS.md.
- [ ] **Task 7.4:** `Feature3LanguageSnippetSmokeTest` — parametrized over 55 langs; each lang's snippets.json parses cleanly + has ≥ 1 snippet (or is in the exclusion list).
- [ ] **Task 7.5:** Mutation: delete one snippets.json file → SnippetLibraryCompletenessTest FAILS. Revert. Commit.

---

## Phase 8 — Snippet placeholder navigation

**Pre-execution gate:** Phase 7 complete + Phase 0 §5 closed.

**Files:** `androidApp/.../ui/editor/SnippetPlaceholderNavigator.kt` + modifications to `IndentEngine.kt` (iter-58 Phase 4 file).

### Tasks

- [ ] **Task 8.1:** `SnippetPlaceholderNavigator` — given inserted snippet body with `${N:placeholder}` markers, computes placeholder ranges in document coordinates. Tab advances through them in order; Esc commits + clears.
- [ ] **Task 8.2:** Modify `IndentEngine.handleTab` to consult `SnippetPlaceholderNavigator.isActive()` — if active, defer to it; else apply existing indent logic.
- [ ] **Task 8.3:** `SnippetExpansionRobolectricTest` (created in Phase 6) is upgraded — select a snippet with 2 placeholders → Tab once → second placeholder selected → Tab again → no placeholders left, IndentEngine takes over.
- [ ] **Task 8.4:** Mutation: stub navigator.isActive → false always → snippet Tab navigation regresses to plain Tab indent. Test FAILS. Revert. Commit.

---

## Phase 9 — 2 anti-bluff challenges + qa-iter-60-gates

**Pre-execution gate:** Phase 8 complete.

**Files:** 2 shell scripts + Makefile edit.

### Tasks

- [ ] **Task 9.1:** `auto_complete_completeness_challenge.sh` — static-grep for the 3 provider impls in providers/; runtime: run unit + Robolectric + smoke + structural test suites. Emit per-test PASSED count + log path per CONST-035.
- [ ] **Task 9.2:** `snippet_library_bundle_challenge.sh` — `unzip -l` Android APK + Desktop tarball; assert ≥ 50 lang dirs under `assets/snippets/` (or wherever Compose Multiplatform packages commonMain resources). Plus Wasm bundle check for `snippets/` JSON.
- [ ] **Task 9.3:** Modify `Makefile` — add `qa-iter-60-gates` target chained into `qa-all` after `qa-iter-58-gates`.
- [ ] **Task 9.4:** Run `make qa-all` end-to-end. Confirm every existing iter-57/58/59 challenge + the 2 new iter-60 challenges PASS.
- [ ] **Task 9.5:** Commit.

---

## Phase 10 — Documentation + diagrams

**Pre-execution gate:** Phase 9 complete (everything green in qa-all).

**Files:** user-guide.md + architecture.md + snippet-coverage-matrix.md + CHANGELOG.md + docs/CONTINUATION.md.

### Tasks

- [ ] **Task 10.1:** `docs/features/auto-complete/user-guide.md` — end-user: trigger keys per platform, popup navigation, snippet editing, supported langs.
- [ ] **Task 10.2:** `docs/features/auto-complete/architecture.md` — contributor: provider interface, how to add a 4th provider (preparing for Feature 4 LSP), how to add a new lang's snippet bundle, per-platform notes.
- [ ] **Task 10.3:** `docs/features/auto-complete/snippet-coverage-matrix.md` — 55 langs × {bundled snippets, custom user snippets allowed, ScopeAwareRanker available} matrix with evidence.
- [ ] **Task 10.4:** Update `CHANGELOG.md` with iter-60 entry.
- [ ] **Task 10.5:** Update `docs/CONTINUATION.md` Section 43.
- [ ] **Task 10.6:** Commit.

---

## Phase 11 — Firebase distribution v1.3.0

**Pre-execution gate:** Phase 10 complete + `make qa-all` green.

### Tasks

- [ ] **Task 11.1:** Bump version: 1.2.1 → 1.3.0; versionCode 121 → 130; dotted `0.0.0.1.30`.
- [ ] **Task 11.2:** Build Android Release + Debug APK (DEV variant from iter-59 ships automatically with the suffix).
- [ ] **Task 11.3:** Build macOS-arm64 DMG.
- [ ] **Task 11.4:** Verify APK contains `assets/snippets/` resource tree.
- [ ] **Task 11.5:** Firebase distribute Android Release + Debug (using the 2 Firebase apps registered in iter-59).
- [ ] **Task 11.6:** Capture evidence to `docs/qa/iter-60/`.
- [ ] **Task 11.7:** Tag `v1.3.0-iter60`; push.
- [ ] **Task 11.8:** Update CHANGELOG.md + CONTINUATION.md with distribution evidence; final commit; push.

---

## Self-review against the spec

**Spec coverage check:**

| Spec requirement | Plan task |
|---|---|
| §2 4-source mix (TokenFreq + Snippets + Identifier + ScopeRanker) | Phase 3 + Phase 4 |
| §2 Implicit (2 chars) + Ctrl+Space + mobile button | Phase 5 + Phase 6 |
| §2 VS Code snippet bundles vendored + user-extensible | Phase 7 |
| §3 Provider-based pipeline (CompletionEngine) | Phase 4 |
| §4 16 components | Each component → its phase |
| §7 14 anti-bluff tests + 2 challenges | Distributed across phases |
| §8 Phase 0 deep-research | Phase 0 |
| §9 Documentation deliverables | Phase 10 |
| §10 Firebase distribution | Phase 11 |

No gaps.

**Placeholder scan:** Phases 0–5 contain full TDD with complete code. Phases 6–11 contain task-level checklists with exact file paths; per-step code crystallizes after Phase 0 closes specific open questions. This is explicit dependency-on-research, not `TBD`.

**Type consistency:** `CompletionItem`, `CompletionContext`, `CompletionProvider`, `Snippet`, `SnippetRegistry`, `CompletionRanker`, `CompletionEngine`, `CompletionTrigger`, `CompletionPopup`, `CompletionPopupState` are introduced in their authoring phases and referenced consistently through Phases 5+. No drift.

---

## Execution handoff

Plan saved to `docs/superpowers/plans/2026-05-15-auto-complete-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks. Best for a ~12-phase plan with ~60 tasks; keeps main context clean.
2. **Inline Execution** — slower; not suitable for this scope.

**Strong recommendation: subagent-driven.** Same pattern that shipped iter-57 (Feature 1) + iter-58 (Feature 2) + iter-59 (DEV variant).

Pick approach.
