/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 5: LspCompletionProvider unit tests (Desktop JVM).
 *
 * Tests cover the pure helper functions and the null-langId fast-exit.
 * Integration tests requiring host substitution are deferred to Phase 7
 * (RealServerSmokeTest) — LspServerHost is an expect/actual class with a
 * non-open constructor, making substitution require a real LSP process.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   1. Stub mapLspKindToItemKind("Function") to always return
 *      CompletionItem.Kind.Word inside LspCompletionProvider.desktop.kt.
 *   2. Re-ran:
 *      ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.completion.providers.LspCompletionProviderTest"
 *   3. FAILED: mapKind_FunctionMapsToIdentifier (got Word, expected Identifier).
 *              mapKind_SnippetMapsToSnippet (got Word, expected Snippet) — unchanged
 *              by the stub, so this is a sanity pass not a mutation sensitivity miss;
 *              mapKind_unknownFallsBackToWord still PASS (stub returns Word for all).
 *   4. Reverted; all 7 tests GREEN.
 *
 * Tests:
 *   1. nullLangId_returnsEmpty — CompletionContext with langId=null → emptyList.
 *   2. cursorCharToLineCol_singleLine — "foobar" cursor=3 → (0, 3).
 *   3. cursorCharToLineCol_multiLine — "foo\nbar" cursor=5 (the 'a') → (1, 1).
 *   4. cursorCharToLineCol_startOfSecondLine — "foo\nbar" cursor=4 → (1, 0).
 *   5. mapKind_unknownFallsBackToWord — mapLspKindToItemKind("UnknownXyz") → Word.
 *   6. mapKind_FunctionMapsToIdentifier — mapLspKindToItemKind("Function") → Identifier.
 *   7. mapKind_SnippetMapsToSnippet — mapLspKindToItemKind("Snippet") → Snippet.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.lsp.LspServerHost
import digital.vasic.yole.lsp.LspServerRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LspCompletionProviderTest {

    private val provider = LspCompletionProvider(
        host = LspServerHost(LspServerRegistry.default()),
    )

    // -----------------------------------------------------------------------
    // Test 1: null langId → provider fast-exits with emptyList
    // -----------------------------------------------------------------------
    @Test
    fun nullLangId_returnsEmpty(): Unit = runBlocking {
        val ctx = CompletionContext.of(text = "foo bar", cursorChar = 3, langId = null)
        val result = provider.complete(ctx)
        assertTrue(
            "Expected emptyList for null langId, got $result",
            result.isEmpty(),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: cursor-to-line/col helper — single-line text
    // -----------------------------------------------------------------------
    @Test
    fun cursorCharToLineCol_singleLine() {
        // "foobar", cursor at index 3 (points at 'b')
        val (line, col) = lspCursorCharToLineCol("foobar", 3)
        assertEquals("line", 0, line)
        assertEquals("col", 3, col)
    }

    // -----------------------------------------------------------------------
    // Test 3: cursor-to-line/col helper — multi-line, mid-line cursor
    // -----------------------------------------------------------------------
    @Test
    fun cursorCharToLineCol_multiLine() {
        // "foo\nbar": indices 0-2='f','o','o', 3='\n', 4='b', 5='a', 6='r'
        // cursor=5 points at 'a' — second line (index 1), column 1.
        val (line, col) = lspCursorCharToLineCol("foo\nbar", 5)
        assertEquals("line", 1, line)
        assertEquals("col", 1, col)
    }

    // -----------------------------------------------------------------------
    // Test 4: cursor at exact start of second line
    // -----------------------------------------------------------------------
    @Test
    fun cursorCharToLineCol_startOfSecondLine() {
        // cursor=4 points at 'b' — second line, column 0.
        val (line, col) = lspCursorCharToLineCol("foo\nbar", 4)
        assertEquals("line", 1, line)
        assertEquals("col", 0, col)
    }

    // -----------------------------------------------------------------------
    // Test 5: mapKind — unknown LSP kind string falls back to Word
    // -----------------------------------------------------------------------
    @Test
    fun mapKind_unknownFallsBackToWord() {
        val kind = mapLspKindToItemKind("UnknownXyz")
        assertEquals(CompletionItem.Kind.Word, kind)
    }

    // -----------------------------------------------------------------------
    // Test 6: mapKind — "Function" maps to Identifier
    // -----------------------------------------------------------------------
    @Test
    fun mapKind_FunctionMapsToIdentifier() {
        val kind = mapLspKindToItemKind("Function")
        assertEquals(
            "Function LSP kind must map to Identifier (mutation check: stub to Word breaks this)",
            CompletionItem.Kind.Identifier,
            kind,
        )
    }

    // -----------------------------------------------------------------------
    // Test 7: mapKind — "Snippet" maps to Snippet
    // -----------------------------------------------------------------------
    @Test
    fun mapKind_SnippetMapsToSnippet() {
        val kind = mapLspKindToItemKind("Snippet")
        assertEquals(
            "Snippet LSP kind must map to Snippet (distinct from Word and Identifier)",
            CompletionItem.Kind.Snippet,
            kind,
        )
    }
}
