/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 6: anti-bluff end-to-end smoke test.
 *
 * Scope and honesty disclosure (CONST-035):
 *
 *   The original Phase 6 plan called for parametrized per-lang
 *   smoke tests: load fixture -> tokenize -> outline -> fold for
 *   each of the 55 languages, asserting >= 1 of each. BUT the
 *   desktop TokenizerEngine.desktop.kt currently only bundles the
 *   `markdown` Tree-Sitter grammar (per Phase 5 of Feature 1's plan
 *   -- the 55-grammar bundling matrix is tracked separately, not in
 *   F2 Phase 6 scope). Loading any other grammar throws
 *   `IllegalArgumentException` per the engine's own contract.
 *
 *   Per CONST-035 ("never write a test that can pass without
 *   exercising the user-visible behavior it claims to verify"), we
 *   do NOT fake the smoke test by mocking the engine or stubbing
 *   out the affordance runners. Instead we:
 *
 *   (a) Run a REAL end-to-end smoke test for `markdown` (the one
 *       grammar actually bundled). This exercises the full pipeline:
 *       TokenizerEngine -> ScmQueryLoader -> FoldQueryRunner +
 *       OutlineExtractor. If any of those layers regresses or is
 *       stubbed, this test FAILS for markdown.
 *
 *   (b) For the other 54 languages, run a deterministic INPUT
 *       smoke check: the lang's fixture is loadable, contains some
 *       text, and its `.scm` files are real upstream content
 *       (with @-captures OR `inherits:` directives OR upstream
 *       placeholder markers) OR an explicit Yole-authored stub.
 *       This is the most honest assertion possible without the
 *       grammar bundled. When the grammar matrix lands (tracked
 *       as a follow-up to #f2-phase-3-bonede-query-api-gap), this
 *       file is extended to run the real engine pipeline for all 55.
 *
 *   The DOCUMENTED gap is anti-bluff compliant: the test never
 *   asserts an end-user-visible outcome it cannot honestly verify.
 *
 * Anti-bluff anchors:
 *   - Mutation: stub OutlineExtractor.outlineFor to return
 *     emptyList() -> the `markdownEndToEndProducesOutlineItems`
 *     test below FAILS (asserts >= 1 outline item from headings).
 *   - Mutation: stub FoldQueryRunner.foldRangesFor to return
 *     emptyList() -> the `markdownEndToEndProducesFoldRange` test
 *     FAILS (asserts >= 1 fold range).
 *   - Mutation: delete any lang's fixture or .scm file -> the
 *     per-lang `inputSmokeCheckForLanguage_<id>` parametrized hits
 *     FAIL with a descriptive message.
 *
 *########################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.FoldQueryRunner
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.io.File

class Feature2LanguageSmokeTest {

    @Before
    fun setUp() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        ScmQueryLoader.clearCacheForTest()
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    // ================================================================
    // PART A: real end-to-end smoke through engine for the markdown
    // grammar (the only Tree-Sitter grammar currently bundled).
    // ================================================================

    @Test
    fun markdownEndToEndProducesTokens() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val input = loadFixtureForLang("markdown")
        assertNotNull("markdown fixture must be available", input)

        val tokens = engine.tokenize(input!!, "markdown")
        assertTrue(
            "tokenize must produce >= 1 token for markdown fixture, got ${tokens.size}",
            tokens.isNotEmpty(),
        )
    }

    @Test
    fun markdownEndToEndProducesOutlineItems() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val input = loadFixtureForLang("markdown")
        assertNotNull("markdown fixture must be available", input)

        val outliner = OutlineExtractor()
        val items = outliner.outlineFor(input!!, "markdown", engine)
        assertTrue(
            "outline must produce >= 1 item for markdown fixture " +
                "(the fixture contains two H-headings), got ${items.size}",
            items.isNotEmpty(),
        )
    }

    @Test
    fun markdownEndToEndProducesFoldRange() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")

        val input = loadFixtureForLang("markdown")
        assertNotNull("markdown fixture must be available", input)

        val folder = FoldQueryRunner()
        val folds = folder.foldRangesFor(input!!, "markdown", engine)
        assertTrue(
            "fold must produce >= 1 range for markdown fixture, got ${folds.size}",
            folds.isNotEmpty(),
        )
    }

    // ================================================================
    // PART B: input-smoke check for all 55 languages.
    //
    // For each language asserts the affordance pipeline INPUTS are
    // coherent: the fixture is loadable + non-empty + its .scm files
    // exist + are non-blank + have non-trivial structure (contain
    // either a `@`-capture or are a documented stub with explicit
    // SPDX header).
    //
    // This is the most honest assertion possible without the 55-
    // grammar bundling matrix landing. When that bundling lands
    // (follow-up to #f2-phase-3-bonede-query-api-gap), extend this
    // method to also run the real engine pipeline.
    // ================================================================

    @Test
    fun inputSmokeCheckForAllLanguages() {
        val failures = mutableListOf<String>()

        for (lf in LanguageMetadata.all) {
            // 1. fixture is loadable + non-empty
            val fixture = loadFixtureForLang(lf.id)
            if (fixture == null) {
                failures += "${lf.id}: no fixture file"
                continue
            }
            if (fixture.length < 20) {
                failures += "${lf.id}: fixture suspiciously short (${fixture.length} chars)"
            }

            // 2. .scm files exist and are non-blank + structurally valid
            for (query in listOf("highlights", "folds", "outline")) {
                val content = loadScmForLang(lf.id, query)
                if (content == null) {
                    failures += "${lf.id}/$query.scm: missing"
                    continue
                }
                if (content.isBlank()) {
                    failures += "${lf.id}/$query.scm: blank"
                    continue
                }
                // Must be one of:
                //   (a) an upstream query with `@`-captures (the
                //       common case for langs with real grammar),
                //   (b) an upstream "inherits" directive that
                //       delegates to a parent (e.g. tsx -> typescript,
                //       jsx -> ecma, scss -> css — common in
                //       nvim-treesitter/helix language families),
                //   (c) an explicit Yole-authored stub (honest, with
                //       SPDX header naming the gap),
                //   (d) an upstream placeholder file (e.g.
                //       clojure/locals.scm's "; placeholder file to
                //       get incremental selection to work" marker)
                //       which is real upstream content even if empty.
                // A file missing all four is a regression.
                val hasCapture = content.contains("@")
                val hasInherits = content.contains("inherits:")
                val isStub = content.contains("Yole-authored stub")
                val isUpstreamPlaceholder = content.contains("placeholder file")
                if (!hasCapture && !hasInherits && !isStub && !isUpstreamPlaceholder) {
                    failures += "${lf.id}/$query.scm: no @-capture, no inherits:, " +
                        "not a documented stub, not an upstream placeholder"
                }
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "input-smoke failures (${failures.size}):\n  " +
                    failures.joinToString("\n  "),
            )
        }
    }

    @Test
    fun every_NonMarkdown_LangHasFixtureThatExercisesItsCommentSyntax() {
        // Sanity check on fixtures: each one should contain its own
        // language's comment marker so a human reader can confirm the
        // file is in the right language. Documented exceptions:
        // - JSON (no comments allowed by RFC 8259)
        // - regex (no comment syntax)
        // - HTML / Markdown / XML / Vue / BibTeX (mixed/embedded —
        //   fixtures contain SPDX headers in alternate comment style)
        val skip = setOf("json", "regex")

        val failures = mutableListOf<String>()
        for (lf in LanguageMetadata.all) {
            if (lf.id in skip) continue
            val fixture = loadFixtureForLang(lf.id) ?: continue
            val cs = lf.commentSyntax
            val lineMark = cs.lineComment?.trim()
            val blockOpen = cs.blockComment?.first
            // Either a line-comment or block-comment marker must appear.
            val present = (lineMark != null && fixture.contains(lineMark)) ||
                (blockOpen != null && fixture.contains(blockOpen))
            if (!present) {
                failures += "${lf.id}: fixture does not contain its own comment marker " +
                    "(line=$lineMark, block=$blockOpen)"
            }
        }
        if (failures.isNotEmpty()) {
            fail(
                "fixture comment-marker check failed:\n  " +
                    failures.joinToString("\n  "),
            )
        }
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private fun loadFixtureForLang(langId: String): String? {
        val resourceDirUrl =
            javaClass.classLoader.getResource("test-fixtures/$langId") ?: return null
        val dir = File(resourceDirUrl.toURI())
        if (!dir.isDirectory) return null
        val file = dir.listFiles()?.firstOrNull { it.isFile } ?: return null
        return file.readText(Charsets.UTF_8)
    }

    private fun loadScmForLang(langId: String, queryName: String): String? {
        val url = javaClass.classLoader.getResource("grammars/$langId/$queryName.scm")
            ?: return null
        return url.openStream().use { it.bufferedReader().readText() }
    }
}
