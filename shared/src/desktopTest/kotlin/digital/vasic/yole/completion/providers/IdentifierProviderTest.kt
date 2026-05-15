/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 3.3: anti-bluff IdentifierProvider tests (Desktop JVM).
 *
 * Uses the real JVM OutlineExtractor + TokenizerEngine, mirroring the
 * setup pattern of OutlineExtractorTest.kt (iter-58 Phase 3).
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated IdentifierProvider.complete to always return emptyList().
 *   - Re-ran: headingPrefixH_returnsHello FAILED (emptyList returned).
 *   - emptyPrefix_returnsBothHeadings also FAILED.
 *   - Reverted mutation; all tests GREEN.
 *
 * Plan deviation (CONST-035): The plan spec §7.1 test #4 states
 * "prefix 'H' → assert both heading names returned" for markdown with
 * headings "# Hello\n## World\n". This is incorrect — "World" does NOT
 * start with 'H', so it cannot match prefix 'H'. The honest tests use:
 *   - prefix "H" → only "Hello"
 *   - prefix ""  → both "Hello" and "World"
 * This deviation is documented in the Phase 3.3 commit body.
 *
 * Tests:
 *   1. headingPrefixH_returnsHello — markdown "# Hello\n## World\n";
 *      prefix "H" → "Hello" returned; "World" not returned.
 *   2. emptyPrefix_returnsBothHeadings — same input; prefix "" → both
 *      "Hello" and "World" returned.
 *   3. prefixNoMatch_returnsEmpty — prefix "Z" → empty list.
 *   4. nullLangId_returnsEmpty — langId null → empty list immediately.
 *   5. deduplication_sameNameOnce — markdown with two identical headings
 *      "# Hello\n# Hello\n"; prefix "H" → only one "Hello" returned.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.language.ScmQueryLoader
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentifierProviderTest {

    private lateinit var engine: TokenizerEngine
    private lateinit var extractor: OutlineExtractor
    private lateinit var provider: IdentifierProvider

    @Before
    fun setUp() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        ScmQueryLoader.clearCacheForTest()
        engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")
        extractor = OutlineExtractor()
        provider = IdentifierProvider(extractor, engine)
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun headingPrefixH_returnsHello() = runBlocking<Unit> {
        val text = "# Hello\n## World\n"
        val ctx = CompletionContext(
            text = text,
            cursorChar = 1,
            langId = "markdown",
            prefix = "H",
            prefixRange = 0..1,
        )

        val items = provider.complete(ctx)

        assertTrue(
            "Expected 'Hello' in identifiers for prefix 'H', got: ${items.map { it.label }}",
            items.any { it.label == "Hello" },
        )
        assertTrue(
            "Expected 'World' NOT in identifiers for prefix 'H' (World doesn't start with H), " +
                "got: ${items.map { it.label }}",
            items.none { it.label == "World" },
        )
        val helloItem = items.first { it.label == "Hello" }
        assertEquals(
            "Identifier kind must be Identifier",
            CompletionItem.Kind.Identifier,
            helloItem.kind,
        )
        assertEquals("Identifier score must be 1.0", 1.0, helloItem.score, 1e-9)
    }

    @Test
    fun emptyPrefix_returnsBothHeadings() = runBlocking<Unit> {
        val text = "# Hello\n## World\n"
        val ctx = CompletionContext(
            text = text,
            cursorChar = 0,
            langId = "markdown",
            prefix = "",
            prefixRange = 0..0,
        )

        val items = provider.complete(ctx)

        assertTrue(
            "Expected 'Hello' in identifiers for empty prefix, got: ${items.map { it.label }}",
            items.any { it.label == "Hello" },
        )
        assertTrue(
            "Expected 'World' in identifiers for empty prefix, got: ${items.map { it.label }}",
            items.any { it.label == "World" },
        )
        assertEquals(
            "Expected exactly 2 identifier items (one per heading), got: ${items.map { it.label }}",
            2,
            items.size,
        )
    }

    @Test
    fun prefixNoMatch_returnsEmpty() = runBlocking<Unit> {
        val text = "# Hello\n## World\n"
        val ctx = CompletionContext(
            text = text,
            cursorChar = 0,
            langId = "markdown",
            prefix = "Z",
            prefixRange = 0..1,
        )

        val items = provider.complete(ctx)
        assertTrue(
            "Prefix 'Z' matches no heading, expected empty, got: ${items.map { it.label }}",
            items.isEmpty(),
        )
    }

    @Test
    fun nullLangId_returnsEmpty() = runBlocking<Unit> {
        val text = "# Hello\n## World\n"
        val ctx = CompletionContext(
            text = text,
            cursorChar = 0,
            langId = null,
            prefix = "H",
            prefixRange = 0..1,
        )

        val items = provider.complete(ctx)
        assertTrue("null langId must return empty list, got: $items", items.isEmpty())
    }

    @Test
    fun deduplication_sameNameOnce() = runBlocking<Unit> {
        // Two identical headings — dedupe should produce only one item.
        val text = "# Hello\n# Hello\n"
        val ctx = CompletionContext(
            text = text,
            cursorChar = 0,
            langId = "markdown",
            prefix = "H",
            prefixRange = 0..1,
        )

        val items = provider.complete(ctx)

        val helloItems = items.filter { it.label == "Hello" }
        assertEquals(
            "Duplicate heading 'Hello' must appear only once after dedupe, " +
                "got: ${items.map { it.label }}",
            1,
            helloItems.size,
        )
    }
}
