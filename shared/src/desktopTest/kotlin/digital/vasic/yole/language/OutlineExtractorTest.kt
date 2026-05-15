/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: anti-bluff OutlineExtractor test (Desktop JVM).
 *
 * Verifies the JVM actual:
 *   1. Drives the real bonede TSQuery + TSQueryCursor pipeline,
 *   2. Produces an OutlineItem with kind=`section` per markdown ATX
 *      heading (helix's tags.scm captures `(atx_heading)` as
 *      `@definition.section`),
 *   3. Each OutlineItem has consistent byte coordinates and a non-
 *      blank name extracted from the captured node.
 *
 * Anti-bluff anchor (CONST-035): stubbing the actual body to
 * `return emptyList()` causes this test to FAIL on the
 * `assertEquals(2, items.size)` assertion (the input has two ATX
 * headings, so the real query must produce two items).
 *
 *########################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class OutlineExtractorTest {

    @Before
    fun setUp() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        ScmQueryLoader.clearCacheForTest()
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun markdownHeadingsProduceOutlineItems() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val extractor = OutlineExtractor()
        // Two ATX headings (different levels). Helix's tags.scm
        // captures every (atx_heading) node as @definition.section.
        val input = "# H1\n\n## H2\n"
        val items = extractor.outlineFor(input, "markdown", engine)

        assertNotNull("outlineFor must not return null", items)
        assertEquals(
            "expected exactly 2 outline items for two ATX headings, got: $items",
            2,
            items.size,
        )

        // All items should be of kind `section` (the suffix after the
        // `definition.` prefix in the capture name).
        for (item in items) {
            assertEquals(
                "outline kind should be `section`, got `${item.kind}` for $item",
                "section",
                item.kind,
            )
            assertTrue(
                "outline name should be non-blank, got `${item.name}` for $item",
                item.name.isNotBlank(),
            )
            assertTrue(
                "outline startByte should be >=0 (got $item)",
                item.startByte >= 0,
            )
            assertTrue(
                "outline endByte should be > startByte (got $item)",
                item.endByte > item.startByte,
            )
        }

        // The first heading is `H1`; the name should resolve to that.
        // The vendored outline.scm captures the full atx_heading node
        // (markers + content) so the extractor's name-cleanup
        // (trimStart('#') + trim) should produce a clean `H1`.
        val firstName = items[0].name
        assertTrue(
            "first outline item name should contain `H1`, got `$firstName`",
            firstName.contains("H1"),
        )
        val secondName = items[1].name
        assertTrue(
            "second outline item name should contain `H2`, got `$secondName`",
            secondName.contains("H2"),
        )
    }

    @Test
    fun emptyInputProducesNoOutlineItems() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val extractor = OutlineExtractor()
        val items = extractor.outlineFor("", "markdown", engine)
        assertEquals(
            "empty markdown should produce no outline items, got $items",
            0,
            items.size,
        )
    }
}
