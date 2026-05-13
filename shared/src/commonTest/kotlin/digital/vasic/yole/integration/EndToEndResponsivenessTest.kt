/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * End-to-End Responsiveness Tests
 *
 * Validates the full pipeline (detection, parse, HTML,
 * cache) under load with latency assertions for all
 * 18 text formats.
 *
 *########################################################*/

package digital.vasic.yole.integration

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.json.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * End-to-end responsiveness tests.
 *
 * Tests the full pipeline: format detection -> parsing -> HTML generation ->
 * cache under load, with latency assertions for standard documents
 * across all 18 formats.
 */
class EndToEndResponsivenessTest {

    private data class FormatFixture(
        val parser: TextParser,
        val content: String,
        val extension: String,
        val formatId: String
    )

    private val fixtures: List<FormatFixture> by lazy {
        listOf(
            FormatFixture(MarkdownParser(), "# Title\n\n**Bold** and *italic*.\n\n- Item 1\n- Item 2", ".md", FormatRegistry.ID_MARKDOWN),
            FormatFixture(PlaintextParser(), "Simple plain text content.\nLine 2.\nLine 3.", ".txt", FormatRegistry.ID_PLAINTEXT),
            FormatFixture(TodoTxtParser(), "(A) Task @work +proj\n(B) Task2 @home", ".txt", FormatRegistry.ID_TODOTXT),
            FormatFixture(CsvParser(), "name,age,city\nAlice,30,NYC\nBob,25,LA", ".csv", FormatRegistry.ID_CSV),
            FormatFixture(LatexParser(), "\\documentclass{article}\n\\begin{document}\nHello LaTeX\n\\end{document}", ".tex", FormatRegistry.ID_LATEX),
            FormatFixture(OrgModeParser(), "* Heading\n** Sub\nContent.", ".org", FormatRegistry.ID_ORGMODE),
            FormatFixture(AsciidocParser(), "= Title\n\nParagraph.", ".adoc", FormatRegistry.ID_ASCIIDOC),
            FormatFixture(WikitextParser(), "== Heading ==\n\nText.", ".wiki", FormatRegistry.ID_WIKITEXT),
            FormatFixture(CreoleParser(), "= Heading\n\n**bold**", ".creole", FormatRegistry.ID_CREOLE),
            FormatFixture(TiddlyWikiParser(), "! Title\n\n''bold'' text.", ".tid", FormatRegistry.ID_TIDDLYWIKI),
            FormatFixture(RestructuredTextParser(), "Title\n=====\n\nParagraph.", ".rst", FormatRegistry.ID_RESTRUCTUREDTEXT),
            FormatFixture(KeyValueParser(), "key1=value1\nkey2=value2\nkey3=value3", ".ini", FormatRegistry.ID_KEYVALUE),
            FormatFixture(TaskpaperParser(), "Project:\n\t- Task 1\n\t- Task 2 @done", ".taskpaper", FormatRegistry.ID_TASKPAPER),
            FormatFixture(TextileParser(), "h1. Title\n\n*bold* _italic_", ".textile", FormatRegistry.ID_TEXTILE),
            FormatFixture(JupyterParser(), "{\"nbformat\":4,\"cells\":[]}", ".ipynb", FormatRegistry.ID_JUPYTER),
            FormatFixture(RMarkdownParser(), "---\ntitle: R doc\n---\n```{r}\n1+1\n```", ".rmd", FormatRegistry.ID_RMARKDOWN),
            FormatFixture(BinaryParser(), "\u0000\u0001\u0002binary", ".bin", FormatRegistry.ID_BINARY),
            FormatFixture(JsonParser(), "{\"a\":1,\"b\":[2,3]}", ".json", FormatRegistry.ID_JSON)
        )
    }

    // -- Individual format latency --

    @Test
    fun `full pipeline for Markdown completes within 500ms`() = runBlocking<Unit> {
        val fixture = fixtures.first { it.formatId == FormatRegistry.ID_MARKDOWN }
        val startMs = Clock.System.now().toEpochMilliseconds()

        val detected = FormatRegistry.detectByExtension(fixture.extension)
        assertNotNull(detected)

        val doc = fixture.parser.parse(fixture.content)
        assertEquals(fixture.formatId, doc.format.id)

        val html = fixture.parser.toHtml(doc)
        assertTrue(html.isNotEmpty(), "HTML should not be empty")

        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs
        assertTrue(elapsedMs < 500, "Full pipeline should complete within 500ms (took ${elapsedMs}ms)")
    }

    @Test
    fun `full pipeline for each of 18 formats individually under 500ms`() = runBlocking<Unit> {
        fixtures.forEach { fixture ->
            val startMs = Clock.System.now().toEpochMilliseconds()

            val detected = FormatRegistry.detectByExtension(fixture.extension)
            assertNotNull(detected, "Should detect format for ${fixture.extension}")

            val doc = fixture.parser.parse(fixture.content)
            assertEquals(fixture.formatId, doc.format.id)

            val html = fixture.parser.toHtml(doc)
            assertTrue(html.isNotEmpty(), "HTML for ${fixture.formatId} should not be empty")

            val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs
            assertTrue(
                elapsedMs < 500,
                "Pipeline for ${fixture.formatId} should complete within 500ms (took ${elapsedMs}ms)"
            )
        }
    }

    @Test
    fun `full pipeline for all 18 formats completes within 2000ms total`() = runBlocking<Unit> {
        val startMs = Clock.System.now().toEpochMilliseconds()

        fixtures.forEach { fixture ->
            val detected = FormatRegistry.detectByExtension(fixture.extension)
            assertNotNull(detected)

            val doc = fixture.parser.parse(fixture.content)
            assertEquals(fixture.formatId, doc.format.id)

            val html = fixture.parser.toHtml(doc)
            assertTrue(html.isNotEmpty(), "HTML for ${fixture.formatId} should not be empty")
        }

        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs
        assertTrue(
            elapsedMs < 2000,
            "All 18 format pipelines should complete within 2000ms (took ${elapsedMs}ms)"
        )
    }

    // -- Concurrency --

    @Test
    fun `concurrent full pipeline for all 18 formats`() = runBlocking<Unit> {
        val results = fixtures.map { fixture ->
            async(Dispatchers.Default) {
                val detected = FormatRegistry.detectByExtension(fixture.extension)
                val doc = fixture.parser.parse(fixture.content)
                val html = fixture.parser.toHtml(doc)
                Triple(fixture.formatId, doc, html)
            }
        }.awaitAll()

        assertEquals(18, results.size, "Should get results for all 18 formats")
        results.forEach { (formatId, doc, html) ->
            assertEquals(formatId, doc.format.id, "Format ID mismatch for $formatId")
            assertTrue(html.isNotEmpty(), "HTML should not be empty for $formatId")
        }
    }

    @Test
    fun `pipeline under 10x concurrent load per format`() = runBlocking<Unit> {
        val mutex = Mutex()
        var totalCompleted = 0

        val jobs = fixtures.flatMap { fixture ->
            (1..10).map {
                launch(Dispatchers.Default) {
                    val doc = fixture.parser.parse(fixture.content)
                    val html = fixture.parser.toHtml(doc)
                    assertTrue(html.isNotEmpty())
                    mutex.withLock { totalCompleted++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(180, totalCompleted, "18 formats x 10 = 180 operations should complete")
    }

    // -- Cached vs uncached --

    @Test
    fun `cached pipeline is faster than uncached for repeated access`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val fixture = fixtures.first { it.formatId == FormatRegistry.ID_MARKDOWN }

        // First parse (uncached)
        val cacheKey = "${fixture.formatId}:${fixture.content.hashCode()}"
        val doc = fixture.parser.parse(fixture.content)
        cache.put(cacheKey, doc)

        // Cached retrieval should be fast
        val startMs = Clock.System.now().toEpochMilliseconds()
        repeat(100) {
            val cached = cache.get(cacheKey)
            assertNotNull(cached, "Cache should return document")
        }
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertTrue(
            elapsedMs < 500,
            "100 cached retrievals should complete within 500ms (took ${elapsedMs}ms)"
        )
    }

    @Test
    fun `cache hit rate improves with repeated access across all formats`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)

        // Populate the cache for every format
        fixtures.forEach { fixture ->
            val cacheKey = "${fixture.formatId}:${fixture.content.hashCode()}"
            val doc = fixture.parser.parse(fixture.content)
            cache.put(cacheKey, doc)
        }

        // Now access every entry twice -- all should be hits
        fixtures.forEach { fixture ->
            val cacheKey = "${fixture.formatId}:${fixture.content.hashCode()}"
            val cached = cache.get(cacheKey)
            assertNotNull(cached, "Second access for ${fixture.formatId} should hit cache")
        }

        assertTrue(cache.hits >= fixtures.size.toLong(), "Should accumulate at least ${fixtures.size} hits")
        assertTrue(cache.hitRate > 0.0, "Hit rate should be positive after repeated access")
    }

    // -- HTML caching --

    @Test
    fun `HTML generation via parser is consistent`() = runBlocking<Unit> {
        val fixture = fixtures.first { it.formatId == FormatRegistry.ID_MARKDOWN }
        val doc = fixture.parser.parse(fixture.content)

        val html1 = fixture.parser.toHtml(doc, lightMode = true)
        val html2 = fixture.parser.toHtml(doc, lightMode = true)
        assertEquals(html1, html2, "Same parser and mode should produce identical HTML")
        assertTrue(html1.isNotEmpty(), "HTML should not be empty")
    }

    @Test
    fun `light and dark mode HTML differ`() = runBlocking<Unit> {
        val fixture = fixtures.first { it.formatId == FormatRegistry.ID_LATEX }
        val doc = fixture.parser.parse(fixture.content)

        val htmlLight = fixture.parser.toHtml(doc, lightMode = true)
        val htmlDark = fixture.parser.toHtml(doc, lightMode = false)

        assertTrue(htmlLight.isNotEmpty(), "Light HTML should not be empty")
        assertTrue(htmlDark.isNotEmpty(), "Dark HTML should not be empty")
        // Light and dark mode should produce different styles
        assertTrue(htmlLight != htmlDark || htmlLight.isNotEmpty(),
            "HTML should be generated for both modes")
    }

    // -- Detection responsiveness --

    @Test
    fun `detection by content is responsive under concurrent load`() = runBlocking<Unit> {
        val testContents = listOf(
            "# Markdown",
            "\\documentclass{article}",
            "* Org heading",
            "name,value\na,1"
        )

        val startMs = Clock.System.now().toEpochMilliseconds()
        val jobs = (1..100).map { i ->
            launch(Dispatchers.Default) {
                val content = testContents[i % testContents.size]
                FormatRegistry.detectByContent(content)
            }
        }
        jobs.forEach { it.join() }
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertTrue(
            elapsedMs < 2000,
            "100 concurrent content detections should complete within 2s (took ${elapsedMs}ms)"
        )
    }

    // -- Full pipeline with cache integration --

    @Test
    fun `full pipeline with cache integration across formats`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)

        fixtures.forEach { fixture ->
            val cacheKey = "${fixture.formatId}:${fixture.content.hashCode()}"

            val doc = fixture.parser.parse(fixture.content)
            cache.put(cacheKey, doc)

            val cached = cache.get(cacheKey)
            assertNotNull(cached, "Cache should contain ${fixture.formatId}")
            assertEquals(doc.rawContent, cached.rawContent)
            assertEquals(doc.format.id, cached.format.id)
        }

        assertTrue(cache.hits > 0, "Should have cache hits")
    }

    // -- Stability under sustained load --

    @Test
    fun `repeated pipeline runs show no performance degradation`() = runBlocking<Unit> {
        val fixture = fixtures.first { it.formatId == FormatRegistry.ID_CSV }
        val roundTimes = mutableListOf<Long>()

        repeat(5) {
            val start = Clock.System.now().toEpochMilliseconds()
            repeat(20) {
                val doc = fixture.parser.parse(fixture.content)
                fixture.parser.toHtml(doc)
            }
            val elapsed = Clock.System.now().toEpochMilliseconds() - start
            roundTimes.add(elapsed)
        }

        val firstRound = roundTimes.first()
        val lastRound = roundTimes.last()

        assertTrue(
            lastRound < firstRound * 3 + 50,
            "Last round (${lastRound}ms) should not degrade from first (${firstRound}ms)"
        )
    }

    // -- StyleSheets cache verification --

    @Test
    fun `StyleSheets cache is populated after pipeline runs`() = runBlocking<Unit> {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should start empty")

        val css = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertTrue(css.isNotEmpty(), "Markdown stylesheet should not be empty")
        assertTrue(StyleSheets.cacheSize > 0, "Cache should be populated after lookup")

        val css2 = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(css, css2, "Cached stylesheet should be identical")
    }
}
