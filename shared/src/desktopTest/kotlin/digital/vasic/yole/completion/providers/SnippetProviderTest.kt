/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.2: anti-bluff SnippetProvider tests (Desktop JVM).
 *
 * Must run in desktopTest because SnippetRegistry.forLanguage() uses
 * the JVM ClassLoader actual (readSnippetResource) to load bundled
 * snippets.json resources — only wired in Desktop + Android source sets.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated SnippetProvider.complete to always return emptyList().
 *   - Re-ran: markdown_prefixTab_returnsTableSnippet FAILED (empty).
 *   - Reverted mutation; all tests GREEN.
 *
 * Tests:
 *   1. markdown_prefixTab_returnsTableSnippet — langId="markdown",
 *      prefix="tab" → "table" snippet returned (prefix startsWith "tab").
 *   2. nullLangId_returnsEmpty — langId=null → empty list (no lang = no snippets).
 *   3. prefixNoMatch_returnsEmpty — prefix="zzz" → no match → empty list.
 *   4. emptyPrefix_returnsAllSnippets — prefix="" → all markdown snippets
 *      returned (every snippet.prefix startsWith "").
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.snippet.SnippetRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnippetProviderTest {

    private val provider = SnippetProvider()

    @Before
    fun setUp() {
        SnippetRegistry.clear()
    }

    @After
    fun tearDown() {
        SnippetRegistry.clear()
    }

    @Test
    fun markdown_prefixTab_returnsTableSnippet() = runBlocking<Unit> {
        val ctx = CompletionContext(
            text = "tab",
            cursorChar = 3,
            langId = "markdown",
            prefix = "tab",
            prefixRange = 0..3,
        )

        val items = provider.complete(ctx)

        assertTrue(
            "Expected ≥ 1 snippet for prefix 'tab' in markdown, got: ${items.map { it.label }}",
            items.isNotEmpty(),
        )
        val tableItem = items.firstOrNull { it.label == "table" }
        assertTrue(
            "Expected a snippet with label 'table' (prefix starts with 'tab'), " +
                "got: ${items.map { it.label }}",
            tableItem != null,
        )
        assertEquals(
            "Snippet kind must be Snippet",
            CompletionItem.Kind.Snippet,
            tableItem!!.kind,
        )
        assertTrue(
            "Snippet body should contain '|' (markdown table syntax), got: ${tableItem.insertText}",
            tableItem.insertText.contains("|"),
        )
        assertEquals(
            "Snippet score should be 1.0",
            1.0,
            tableItem.score,
            1e-9,
        )
    }

    @Test
    fun nullLangId_returnsEmpty() = runBlocking<Unit> {
        val ctx = CompletionContext(
            text = "table",
            cursorChar = 5,
            langId = null,
            prefix = "tab",
            prefixRange = 0..3,
        )

        val items = provider.complete(ctx)
        assertTrue("null langId must return empty list, got: $items", items.isEmpty())
    }

    @Test
    fun prefixNoMatch_returnsEmpty() = runBlocking<Unit> {
        val ctx = CompletionContext(
            text = "zzz",
            cursorChar = 3,
            langId = "markdown",
            prefix = "zzz",
            prefixRange = 0..3,
        )

        val items = provider.complete(ctx)
        assertTrue(
            "Prefix 'zzz' should match no snippets, got: ${items.map { it.label }}",
            items.isEmpty(),
        )
    }

    @Test
    fun emptyPrefix_returnsAllMarkdownSnippets() = runBlocking<Unit> {
        val ctx = CompletionContext(
            text = "",
            cursorChar = 0,
            langId = "markdown",
            prefix = "",
            prefixRange = 0..0,
        )

        val items = provider.complete(ctx)
        val allSnippets = SnippetRegistry.forLanguage("markdown")
        assertEquals(
            "Empty prefix should return all ${allSnippets.size} markdown snippets, " +
                "got ${items.size}",
            allSnippets.size,
            items.size,
        )
    }
}
