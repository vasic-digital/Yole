/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: anti-bluff FoldQueryRunner test (Desktop JVM).
 *
 * Verifies the JVM actual:
 *   1. Drives the real bonede TSQuery + TSQueryCursor pipeline,
 *   2. Produces at least one FoldRange for a multi-line markdown
 *      heading section (the `(section)` node is the dominant @fold
 *      capture in nvim-treesitter's markdown/folds.scm),
 *   3. Each FoldRange has consistent line + byte coordinates
 *      (startByte <= endByte, startLine <= endLine, all >= 0).
 *
 * Anti-bluff anchor (CONST-035): stubbing the actual body to
 * `return emptyList()` causes this test to FAIL on the first
 * `assertTrue(folds.isNotEmpty())` assertion. Verified pre-commit.
 *
 *########################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.FoldQueryRunner
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class FoldQueryRunnerTest {

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
    fun markdownHeadingProducesFoldRange() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val runner = FoldQueryRunner()
        // A markdown section with a heading and >=2 body lines. The
        // tree-sitter-markdown grammar parses this into a (document)
        // -> (section) node which the bundled folds.scm captures as
        // @fold. The trailing newline ensures the section is properly
        // terminated.
        val input = "# Heading\n\nLine 1\nLine 2\n"
        val folds = runner.foldRangesFor(input, "markdown", engine)

        assertNotNull("foldRangesFor must not return null", folds)
        assertTrue(
            "expected at least 1 fold range for `$input`, got: $folds",
            folds.isNotEmpty(),
        )

        // Every emitted fold must have sane coordinates.
        for (f in folds) {
            assertTrue(
                "fold startByte should be >=0 (got $f)",
                f.startByte >= 0,
            )
            assertTrue(
                "fold endByte should be >= startByte (got $f)",
                f.endByte >= f.startByte,
            )
            assertTrue(
                "fold startLine should be >=0 (got $f)",
                f.startLine >= 0,
            )
            assertTrue(
                "fold endLine should be >= startLine (got $f)",
                f.endLine >= f.startLine,
            )
            assertTrue(
                "fold endByte ${f.endByte} should not exceed input length ${input.length}",
                f.endByte <= input.encodeToByteArray().size,
            )
        }

        // The bundled folds.scm (Yole-authored for tree-sitter-markdown
        // 0.7.1) captures `(paragraph)` -- which covers the body lines
        // beneath the heading. The captured paragraph node spans
        // multiple lines, so at least one fold MUST be multi-line
        // (endLine > startLine). This is the property a stubbed
        // implementation would fail to produce.
        val multiLineFold = folds.any { it.endLine > it.startLine }
        assertTrue(
            "expected at least one multi-line fold (paragraph body), got $folds",
            multiLineFold,
        )
    }

    @Test
    fun emptyInputProducesNoFolds() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val runner = FoldQueryRunner()
        val folds = runner.foldRangesFor("", "markdown", engine)
        assertEquals(
            "empty markdown should produce no folds, got $folds",
            0,
            folds.size,
        )
    }

    @Test
    fun fencedCodeBlockIsCaptured() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val runner = FoldQueryRunner()
        // A fenced code block is a top-level fold capture per
        // nvim-treesitter's folds.scm (the `(fenced_code_block)`
        // node type is part of the @fold alternation).
        val input = "```\nint x = 1;\nint y = 2;\n```\n"
        val folds = runner.foldRangesFor(input, "markdown", engine)
        assertTrue(
            "fenced code block should produce at least 1 fold, got $folds",
            folds.isNotEmpty(),
        )
    }
}
