/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: SnippetRegistry integration test.
 *
 * Verifies:
 *   1. forLanguage("markdown") loads the bundled snippets.json and
 *      returns ≥ 1 snippet whose prefix is exactly "table".
 *   2. forLanguage("nonexistent-lang-xyz") returns an empty list and
 *      does NOT throw.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated SnippetRegistry.forLanguage to always return emptyList().
 *   - Re-ran: markdown_snippets_loadTableAnchor FAILED (emptyList).
 *   - Reverted mutation; both tests GREEN.
 *
 * This test must run in desktopTest (not commonTest) because
 * readSnippetResource relies on the JVM classloader actual, which is
 * only wired in Desktop + Android source sets.
 *#######################################################*/
package digital.vasic.yole.completion

import digital.vasic.yole.completion.snippet.SnippetRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnippetRegistryTest {

    @Before
    fun setUp() {
        SnippetRegistry.clear()
    }

    @After
    fun tearDown() {
        SnippetRegistry.clear()
    }

    @Test
    fun markdown_snippets_loadTableAnchor() {
        val snippets = SnippetRegistry.forLanguage("markdown")
        assertTrue(
            "Expected ≥ 1 markdown snippet, got ${snippets.size}",
            snippets.isNotEmpty(),
        )
        val tableSnippet = snippets.firstOrNull { it.prefix == "table" }
        assertTrue(
            "Expected a snippet with prefix 'table' in the bundled markdown snippets.json, " +
                "but found only: ${snippets.map { it.prefix }}",
            tableSnippet != null,
        )
        // Also verify the body is non-trivial (not just a stub).
        assertTrue(
            "Table snippet body should contain '|' (markdown table syntax), " +
                "got: ${tableSnippet!!.body}",
            tableSnippet.body.contains("|"),
        )
    }

    @Test
    fun nonexistent_language_returns_emptyList_no_throw() {
        val snippets = SnippetRegistry.forLanguage("nonexistent-lang-xyz")
        assertEquals(
            "forLanguage for unknown lang should return empty list",
            emptyList<Any>(),
            snippets,
        )
    }
}
