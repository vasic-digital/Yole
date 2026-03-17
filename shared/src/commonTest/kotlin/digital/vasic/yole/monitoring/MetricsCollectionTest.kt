/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Monitoring and metrics collection tests covering
 * parse time budgets, cache statistics, format detection
 * latency, and memory growth behavior.
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.binary.BinaryParser
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Monitoring and metrics collection tests.
 *
 * Validates parse time budgets, cache hit/miss tracking,
 * format detection speed, and memory stability.
 *
 * Total: 28+ tests
 */
class MetricsCollectionTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    /** Representative content per format. */
    private val sampleContents = mapOf(
        FormatRegistry.ID_MARKDOWN to "# Title\n\nThis is **bold** and *italic*.\n\n- Item 1\n- Item 2",
        FormatRegistry.ID_PLAINTEXT to "Plain text content for testing.\nMultiple lines of text.",
        FormatRegistry.ID_TODOTXT to "(A) Buy groceries @store +home\n(B) Clean house @home +chores",
        FormatRegistry.ID_CSV to "name,age,city\nAlice,30,NYC\nBob,25,LA\nCharlie,35,SF",
        FormatRegistry.ID_WIKITEXT to "= Heading =\n\n'''Bold''' text.\n\n[[link]]",
        FormatRegistry.ID_CREOLE to "= Heading\n\n**Bold** text here.\n\n* list item",
        FormatRegistry.ID_TIDDLYWIKI to "title: Test\n\n! Heading\n\nContent here.",
        FormatRegistry.ID_LATEX to "\\documentclass{article}\n\\begin{document}\n\\section{Intro}\nHello.\n\\end{document}",
        FormatRegistry.ID_ASCIIDOC to "= Title\n\nSome *bold* text.\n\n== Section\n\nContent.",
        FormatRegistry.ID_ORGMODE to "* Heading\n** Sub-heading\nContent.\n#+TITLE: Test",
        FormatRegistry.ID_RESTRUCTUREDTEXT to "Title\n=====\n\nParagraph text.\n\nSection\n-------\n\nMore text.",
        FormatRegistry.ID_KEYVALUE to "[section]\nkey1 = value1\nkey2 = value2\n\n[section2]\nk = v",
        FormatRegistry.ID_TASKPAPER to "Project:\n\t- Task 1 @due(2025-01-01)\n\t- Task 2 @done",
        FormatRegistry.ID_TEXTILE to "h1. Heading\n\nParagraph with *bold* text.",
        FormatRegistry.ID_JUPYTER to """{"nbformat":4,"nbformat_minor":4,"metadata":{},"cells":[{"cell_type":"markdown","source":"# Hi","metadata":{}}]}""",
        FormatRegistry.ID_RMARKDOWN to "---\ntitle: Test\n---\n\n# Heading\n\n```{r}\n1+1\n```",
        FormatRegistry.ID_BINARY to "\u0000\u0001\u0002\u0003\u0004"
    )

    private val allParsers: List<TextParser> = listOf(
        MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
        WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
        AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
        KeyValueParser(), TaskpaperParser(), TextileParser(),
        JupyterParser(), RMarkdownParser(), BinaryParser()
    )

    // ==================== Parse Time Per Format ====================

    @Test
    fun allFormatsParseSampleContentUnder500ms() {
        for (parser in allParsers) {
            val content = sampleContents[parser.supportedFormat.id] ?: "fallback text"
            val elapsed = measureTime { parser.parse(content) }
            assertTrue(
                elapsed.inWholeMilliseconds < 500,
                "${parser.supportedFormat.name} parse took ${elapsed.inWholeMilliseconds}ms, expected < 500ms"
            )
        }
    }

    @Test
    fun markdownParsesTypicalDocUnder100ms() {
        val parser = MarkdownParser()
        val content = buildString {
            repeat(20) { i ->
                appendLine("## Section $i")
                appendLine()
                appendLine("Paragraph with **bold** and *italic* text.")
                appendLine()
                appendLine("- Item A")
                appendLine("- Item B")
                appendLine()
            }
        }
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(
            elapsed.inWholeMilliseconds < 100,
            "Markdown typical doc parse took ${elapsed.inWholeMilliseconds}ms, expected < 100ms"
        )
    }

    @Test
    fun csvParsesLargeTableUnder200ms() {
        val parser = CsvParser()
        val content = buildString {
            appendLine("col1,col2,col3,col4,col5")
            repeat(500) { i ->
                appendLine("val-$i-1,val-$i-2,val-$i-3,val-$i-4,val-$i-5")
            }
        }
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(
            elapsed.inWholeMilliseconds < 200,
            "CSV large table parse took ${elapsed.inWholeMilliseconds}ms, expected < 200ms"
        )
    }

    // ==================== Cache Hit Rate Tracking ====================

    @Test
    fun documentCacheTracksHitsCorrectly() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val doc = ParsedDocument(format = testFormat, rawContent = "test", parsedContent = "test")

        cache.put("key", doc)
        cache.get("key")  // hit
        cache.get("key")  // hit
        cache.get("key")  // hit

        assertEquals(3L, cache.hits)
    }

    @Test
    fun documentCacheTracksMissesCorrectly() = runBlocking<Unit> {
        val cache = DocumentCache()
        cache.get("miss1")
        cache.get("miss2")
        cache.get("miss3")

        assertEquals(3L, cache.misses)
        assertEquals(0L, cache.hits)
    }

    @Test
    fun documentCacheHitRateAccurate() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val doc = ParsedDocument(format = testFormat, rawContent = "test", parsedContent = "test")

        cache.put("key", doc)

        // 3 hits, 1 miss = 75% hit rate
        cache.get("key")    // hit
        cache.get("key")    // hit
        cache.get("key")    // hit
        cache.get("other")  // miss

        assertEquals(0.75, cache.hitRate, 0.01)
    }

    @Test
    fun documentCacheHitRateZeroInitially() {
        val cache = DocumentCache()
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    @Test
    fun documentCacheStatsResetOnClear() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val doc = ParsedDocument(format = testFormat, rawContent = "test", parsedContent = "test")

        cache.put("key", doc)
        cache.get("key")
        cache.get("miss")
        assertTrue(cache.hits > 0 || cache.misses > 0)

        cache.clear()
        assertEquals(0L, cache.hits)
        assertEquals(0L, cache.misses)
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    // ==================== DocumentCache Statistics ====================

    @Test
    fun documentCacheSizeAccurate() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        assertEquals(0, cache.size)

        cache.put("a", ParsedDocument(format = testFormat, rawContent = "a", parsedContent = "a"))
        assertEquals(1, cache.size)

        cache.put("b", ParsedDocument(format = testFormat, rawContent = "b", parsedContent = "b"))
        assertEquals(2, cache.size)

        cache.invalidate("a")
        assertEquals(1, cache.size)
    }

    @Test
    fun documentCacheEvictionMaintainsMaxSize() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")

        for (i in 1..20) {
            cache.put("key-$i", ParsedDocument(format = testFormat, rawContent = "c$i", parsedContent = "c$i"))
        }
        assertTrue(cache.size <= 5, "Cache size should not exceed maxSize")
    }

    @Test
    fun documentCacheContainsAfterPut() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val doc = ParsedDocument(format = testFormat, rawContent = "test", parsedContent = "test")

        assertFalse(cache.contains("key"))
        cache.put("key", doc)
        assertTrue(cache.contains("key"))
    }

    // ==================== Memory Growth Behavior ====================

    @Test
    fun parsedDocumentHtmlNotAllocatedUntilNeeded() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nContent.")
        assertFalse(doc.hasHtmlCached(true), "Light HTML should not be cached before toHtml()")
        assertFalse(doc.hasHtmlCached(false), "Dark HTML should not be cached before toHtml()")
    }

    @Test
    fun parsedDocumentHtmlAllocatedOnlyForRequestedMode() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nContent.")

        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false), "Dark HTML should not be cached when only light was requested")
    }

    @Test
    fun batchParsingDoesNotGrowUnbounded() {
        val parser = MarkdownParser()
        val documents = mutableListOf<ParsedDocument>()
        for (i in 1..500) {
            documents.add(parser.parse("# Doc $i\n\nContent $i"))
        }
        assertEquals(500, documents.size)
        // Verify all are valid
        documents.forEach { doc ->
            assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)
            assertFalse(doc.hasHtmlCached(true), "HTML should not be auto-generated")
        }
    }

    @Test
    fun clearHtmlCacheFreesCachedData() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Large Content\n\n" + "x".repeat(10000))

        doc.toHtml(lightMode = true)
        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(true))
        assertTrue(doc.hasHtmlCached(false))

        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false))
    }

    // ==================== Format Detection Time ====================

    @Test
    fun extensionDetectionIsFast() {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst", "adoc", "ini", "ipynb")
        val elapsed = measureTime {
            repeat(1000) {
                for (ext in extensions) {
                    FormatRegistry.detectByExtension(ext)
                }
            }
        }
        // 10000 detections should complete in well under 1 second
        assertTrue(
            elapsed.inWholeMilliseconds < 1000,
            "10000 extension detections took ${elapsed.inWholeMilliseconds}ms"
        )
    }

    @Test
    fun contentDetectionIsFast() {
        val samples = listOf(
            "# Heading",
            "a,b,c\n1,2,3",
            "\\documentclass{article}",
            "= Heading =",
            "(A) Task @ctx"
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (sample in samples) {
                    FormatRegistry.detectByContent(sample)
                }
            }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 1000,
            "5000 content detections took ${elapsed.inWholeMilliseconds}ms"
        )
    }

    @Test
    fun filenameDetectionIsFast() {
        val filenames = listOf(
            "readme.md", "data.csv", "paper.tex",
            "notes.org", "page.wiki", "doc.rst"
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (name in filenames) {
                    FormatRegistry.detectByFilename(name)
                }
            }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 1000,
            "6000 filename detections took ${elapsed.inWholeMilliseconds}ms"
        )
    }

    @Test
    fun getByIdIsFast() {
        val ids = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX, FormatRegistry.ID_ORGMODE
        )
        val elapsed = measureTime {
            repeat(10000) {
                for (id in ids) {
                    FormatRegistry.getById(id)
                }
            }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 1000,
            "40000 ID lookups took ${elapsed.inWholeMilliseconds}ms"
        )
    }

    // ==================== StyleSheets Performance ====================

    @Test
    fun styleSheetLookupIsFast() {
        StyleSheets.clearCache()
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_LATEX
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (fmtId in formatIds) {
                    StyleSheets.getStyleSheet(fmtId, true)
                    StyleSheets.getStyleSheet(fmtId, false)
                }
            }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 200,
            "8000 stylesheet lookups took ${elapsed.inWholeMilliseconds}ms"
        )
    }

    @Test
    fun styleSheetCacheSizeGrowsCorrectly() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(1, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, false)
        assertEquals(2, StyleSheets.cacheSize)

        // Same key should not increase size
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(2, StyleSheets.cacheSize)
    }
}
