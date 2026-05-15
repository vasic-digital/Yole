/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 6 + Phase 7: anti-bluff end-to-end smoke test.
 *
 * Scope and honesty disclosure (CONST-035):
 *
 *   F2 Phase 6 shipped LanguageMetadata + .scm queries + fixtures
 *   for 55 languages. Only the `markdown` Tree-Sitter grammar was
 *   bundled at that time (iter-57 Phase 5), so the test ran a real
 *   end-to-end pipeline for markdown and an "input-smoke" check
 *   (fixture + .scm coherence) for the other 54.
 *
 *   F2 Phase 7 closes the bundling gap for 47 of 55 langs on
 *   Desktop (macOS-arm64 / macOS-x64 / Linux-x64 / Linux-aarch64 /
 *   Windows-x64) via 47 io.github.bonede:tree-sitter-<lang> Gradle
 *   dependencies. The 8 langs NOT bundled today are:
 *     - 7 with no bonede artifact: jsx, xml, vim, less, crystal,
 *       groovy, bibtex
 *     - 1 with a broken bonede artifact (segfaults on parse): nim
 *   See docs/KNOWN_DEFECTS.md#f2-phase-7-no-bonede-artifact and
 *       docs/KNOWN_DEFECTS.md#f2-phase-7-nim-grammar-broken.
 *
 *   This test now does THREE things:
 *
 *   (a) Real end-to-end smoke through the engine for `markdown`
 *       (the iter-57 path). Exercises:
 *       TokenizerEngine -> ScmQueryLoader -> FoldQueryRunner +
 *       OutlineExtractor.
 *
 *   (b) Real engine tokenization for the 46 other bonede-bundled
 *       langs (everything in BonedeGrammarRegistry.supportedLangs
 *       minus markdown). Asserts >= 1 token per lang. Doesn't run
 *       OutlineExtractor / FoldQueryRunner against them yet —
 *       those depend on .scm query compatibility with the specific
 *       bonede ABI version, which is a Phase 8 concern. The
 *       BonedeGrammarSmokeTest covers the parse-only path directly
 *       and exhaustively.
 *
 *   (c) For the remaining 8 langs (the gap set), input-smoke
 *       coherence check (unchanged from Phase 6): fixture
 *       loadable + .scm files structurally valid. The end-user
 *       feature gap is explicitly disclosed in the test report.
 *
 * Anti-bluff anchors:
 *   - Mutation: stub OutlineExtractor.outlineFor to return
 *     emptyList() -> `markdownEndToEndProducesOutlineItems` FAILS.
 *   - Mutation: stub FoldQueryRunner.foldRangesFor to return
 *     emptyList() -> `markdownEndToEndProducesFoldRange` FAILS.
 *   - Mutation: stub TokenizerEngine.tokenize to return emptyList()
 *     -> both `realTokenizationForAllBundledLangs` and
 *     `markdownEndToEndProducesTokens` FAIL.
 *   - Mutation: delete any lang's fixture or .scm file -> the
 *     `inputSmokeCheckForAllLanguages` test FAILS with a
 *     descriptive per-lang message.
 *
 *########################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.FoldQueryRunner
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.BonedeGrammarRegistry
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
        // Phase 7: open the gate to every bonede-bundled lang so the new
        // realTokenizationForAllBundledLangs test below can exercise the
        // real engine. Markdown-only tests below still pass because that
        // ID is in the set.
        val open = BonedeGrammarRegistry.supportedLangs + "markdown"
        EnabledFormatGate.setEnabled(open)
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
    // PART A2: F2 Phase 7 — real engine tokenization for every lang
    // whose bonede grammar is bundled today (47 langs incl. markdown).
    // Uses the per-lang fixture as the input. Asserts >= 1 token per
    // lang. The bonede grammar instantiation crash modes were proved
    // out in BonedeGrammarSmokeTest with hand-crafted snippets; this
    // test additionally proves the fixtures we ship are themselves
    // parseable by the bundled grammars (the cross-cut Phase 6 ×
    // Phase 7 anti-bluff anchor).
    //
    // Anti-bluff: when ANY bundled lang fails to tokenize its own
    // fixture, this test fails with the lang name in the message —
    // the user-visible feature is regressed.
    // ================================================================

    @Test
    fun realTokenizationForAllBundledLangs() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()

        val failures = mutableListOf<String>()
        val successes = mutableListOf<String>()

        for (lang in BonedeGrammarRegistry.supportedLangs.sorted()) {
            val fixture = loadFixtureForLang(lang)
            if (fixture == null) {
                failures += "$lang: no fixture file"
                continue
            }
            try {
                engine.loadGrammar(lang)
                val tokens = engine.tokenize(fixture, lang)
                if (tokens.isEmpty()) {
                    failures += "$lang: tokenize returned 0 tokens for fixture"
                } else {
                    successes += "$lang(${tokens.size})"
                }
            } catch (t: Throwable) {
                failures += "$lang: ${t.javaClass.simpleName}: ${t.message?.take(120)}"
            }
        }

        val expected = BonedeGrammarRegistry.supportedLangs.size
        val report = buildString {
            appendLine(
                "F2 Phase 7 real-engine smoke over fixtures: " +
                    "${successes.size}/$expected langs produced >= 1 token.",
            )
            appendLine("  successes: ${successes.joinToString(", ")}")
            if (failures.isNotEmpty()) {
                appendLine("  failures:")
                failures.forEach { appendLine("    - $it") }
            }
            appendLine(
                "  gap-set (8 langs not bundled): " +
                    BonedeGrammarRegistry.unsupportedLangs.sorted().joinToString(", "),
            )
        }
        println(report)
        if (failures.isNotEmpty()) {
            fail(report)
        }
        assertTrue(
            "expected ALL $expected bundled langs to tokenize their own fixtures; " +
                "got ${successes.size}",
            successes.size == expected,
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
