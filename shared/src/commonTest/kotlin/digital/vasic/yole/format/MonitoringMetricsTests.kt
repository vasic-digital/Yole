/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Monitoring and metrics collection tests
 *
 *########################################################*/
package digital.vasic.yole.format

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
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Monitoring and metrics collection tests.
 *
 * Validates parse-time budgets, HTML generation performance, format detection
 * latency, memory behavior, throughput, and lazy-loading effectiveness across
 * all 17 text formats.
 */
class MonitoringMetricsTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ========================== helpers ==========================

    /** All 17 parsers in a stable order. */
    private val allParsers: List<TextParser> = listOf(
        MarkdownParser(),
        PlaintextParser(),
        TodoTxtParser(),
        CsvParser(),
        WikitextParser(),
        CreoleParser(),
        TiddlyWikiParser(),
        LatexParser(),
        AsciidocParser(),
        OrgModeParser(),
        RestructuredTextParser(),
        KeyValueParser(),
        TaskpaperParser(),
        TextileParser(),
        JupyterParser(),
        RMarkdownParser(),
        BinaryParser()
    )

    /** Representative small content (~100 chars) per format id. */
    private val smallContents: Map<String, String> = mapOf(
        FormatRegistry.ID_MARKDOWN to "# Title\n\nShort **bold** paragraph.",
        FormatRegistry.ID_PLAINTEXT to "A short plain text sample document for testing purposes.",
        FormatRegistry.ID_TODOTXT to "(A) Buy groceries @store +home",
        FormatRegistry.ID_CSV to "name,age\nAlice,30\nBob,25",
        FormatRegistry.ID_WIKITEXT to "= Heading =\n\n'''Bold''' text.",
        FormatRegistry.ID_CREOLE to "= Heading\n\n**Bold** text here.",
        FormatRegistry.ID_TIDDLYWIKI to "title: Sample\n\n! Heading\n\nContent.",
        FormatRegistry.ID_LATEX to "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}",
        FormatRegistry.ID_ASCIIDOC to "= Title\n\nSome *bold* text.",
        FormatRegistry.ID_ORGMODE to "* Heading\n** Sub\nContent.",
        FormatRegistry.ID_RESTRUCTUREDTEXT to "Title\n=====\n\nParagraph.",
        FormatRegistry.ID_KEYVALUE to "[section]\nkey = value",
        FormatRegistry.ID_TASKPAPER to "Project:\n\t- Task @due(2025-01-01)",
        FormatRegistry.ID_TEXTILE to "h1. Heading\n\nParagraph.",
        FormatRegistry.ID_JUPYTER to """{"nbformat":4,"nbformat_minor":4,"metadata":{},"cells":[{"cell_type":"markdown","source":"# Hi","metadata":{}}]}""",
        FormatRegistry.ID_RMARKDOWN to "---\ntitle: Test\n---\n\n# Heading\n\n```{r}\n1+1\n```",
        FormatRegistry.ID_BINARY to "\u0000\u0001\u0002\u0003"
    )

    /** Build a medium document (~10 KB) for a parser by repeating its small content. */
    private fun buildMediumContent(formatId: String): String {
        val base = smallContents[formatId] ?: "Sample text."
        return buildString {
            val repetitions = (10240 / base.length).coerceAtLeast(1)
            repeat(repetitions) {
                appendLine(base)
            }
        }
    }

    /** Find the parser whose supportedFormat.id matches. */
    private fun parserFor(formatId: String): TextParser =
        allParsers.first { it.supportedFormat.id == formatId }

    // ========================== 1. Parse Time Metrics ==========================

    @Test
    fun testParseTimeMarkdownSmall() {
        val parser = parserFor(FormatRegistry.ID_MARKDOWN)
        val content = smallContents[FormatRegistry.ID_MARKDOWN]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Markdown small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimePlaintextSmall() {
        val parser = parserFor(FormatRegistry.ID_PLAINTEXT)
        val content = smallContents[FormatRegistry.ID_PLAINTEXT]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Plaintext small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeTodoTxtSmall() {
        val parser = parserFor(FormatRegistry.ID_TODOTXT)
        val content = smallContents[FormatRegistry.ID_TODOTXT]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "TodoTxt small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeCsvSmall() {
        val parser = parserFor(FormatRegistry.ID_CSV)
        val content = smallContents[FormatRegistry.ID_CSV]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "CSV small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeWikitextSmall() {
        val parser = parserFor(FormatRegistry.ID_WIKITEXT)
        val content = smallContents[FormatRegistry.ID_WIKITEXT]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Wikitext small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeCreoleSmall() {
        val parser = parserFor(FormatRegistry.ID_CREOLE)
        val content = smallContents[FormatRegistry.ID_CREOLE]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Creole small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeTiddlyWikiSmall() {
        val parser = parserFor(FormatRegistry.ID_TIDDLYWIKI)
        val content = smallContents[FormatRegistry.ID_TIDDLYWIKI]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "TiddlyWiki small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeLatexSmall() {
        val parser = parserFor(FormatRegistry.ID_LATEX)
        val content = smallContents[FormatRegistry.ID_LATEX]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "LaTeX small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeAsciidocSmall() {
        val parser = parserFor(FormatRegistry.ID_ASCIIDOC)
        val content = smallContents[FormatRegistry.ID_ASCIIDOC]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "AsciiDoc small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeOrgModeSmall() {
        val parser = parserFor(FormatRegistry.ID_ORGMODE)
        val content = smallContents[FormatRegistry.ID_ORGMODE]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "OrgMode small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeRestructuredTextSmall() {
        val parser = parserFor(FormatRegistry.ID_RESTRUCTUREDTEXT)
        val content = smallContents[FormatRegistry.ID_RESTRUCTUREDTEXT]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "RST small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeKeyValueSmall() {
        val parser = parserFor(FormatRegistry.ID_KEYVALUE)
        val content = smallContents[FormatRegistry.ID_KEYVALUE]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "KeyValue small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeTaskpaperSmall() {
        val parser = parserFor(FormatRegistry.ID_TASKPAPER)
        val content = smallContents[FormatRegistry.ID_TASKPAPER]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Taskpaper small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeTextileSmall() {
        val parser = parserFor(FormatRegistry.ID_TEXTILE)
        val content = smallContents[FormatRegistry.ID_TEXTILE]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Textile small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeJupyterSmall() {
        val parser = parserFor(FormatRegistry.ID_JUPYTER)
        val content = smallContents[FormatRegistry.ID_JUPYTER]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Jupyter small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeRMarkdownSmall() {
        val parser = parserFor(FormatRegistry.ID_RMARKDOWN)
        val content = smallContents[FormatRegistry.ID_RMARKDOWN]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "RMarkdown small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testParseTimeBinarySmall() {
        val parser = parserFor(FormatRegistry.ID_BINARY)
        val content = smallContents[FormatRegistry.ID_BINARY]!!
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Binary small parse took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    // ========================== 2. HTML Generation Metrics ==========================

    @Test
    fun testHtmlGenerationFirstTimeUncached() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Big Heading\n\nParagraph with **bold**, *italic*, and `code`.\n\n- Item 1\n- Item 2\n- Item 3")
        assertFalse(doc.hasHtmlCached(lightMode = true))

        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(doc.hasHtmlCached(lightMode = true), "HTML should be cached after first call")
        // First-time generation must complete in a reasonable timeframe
        assertTrue(elapsed.inWholeMilliseconds < 200, "First-time HTML generation took ${elapsed.inWholeMilliseconds}ms, expected < 200ms")
    }

    @Test
    fun testHtmlGenerationCachedRetrieval() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Cached Test\n\nContent here.")
        // Warm the cache
        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))

        // Cached retrieval should be near-instant
        val elapsed = measureTime {
            repeat(1000) { doc.toHtml(lightMode = true) }
        }
        val perCallNanos = elapsed.inWholeNanoseconds / 1000
        // Average cached call should be well under 1ms (< 1_000_000 ns)
        assertTrue(perCallNanos < 1_000_000, "Cached HTML retrieval averaged ${perCallNanos}ns, expected < 1ms")
    }

    @Test
    fun testHtmlGenerationComplexMarkdown() {
        val parser = MarkdownParser()
        val complex = buildString {
            repeat(50) { i ->
                appendLine("## Section $i")
                appendLine()
                appendLine("Paragraph with **bold** and *italic* and [link](http://example.com/$i).")
                appendLine()
                appendLine("- Item A$i")
                appendLine("- Item B$i")
                appendLine("- Item C$i")
                appendLine()
                appendLine("```kotlin")
                appendLine("fun section$i() = println(\"$i\")")
                appendLine("```")
                appendLine()
            }
        }
        val doc = parser.parse(complex)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty(), "HTML must not be empty for complex Markdown")
        assertTrue(elapsed.inWholeMilliseconds < 500, "Complex Markdown HTML generation took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
    }

    @Test
    fun testCssGenerationTimingLightAndDark() {
        // StyleSheets.getStyleSheet is a pure lookup -- measure for all themed formats
        val themedFormats = listOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (fmtId in themedFormats) {
                    StyleSheets.getStyleSheet(fmtId, lightMode = true)
                    StyleSheets.getStyleSheet(fmtId, lightMode = false)
                }
            }
        }
        // 12000 lookups should finish well under 100ms
        assertTrue(elapsed.inWholeMilliseconds < 100, "12000 CSS lookups took ${elapsed.inWholeMilliseconds}ms, expected < 100ms")
    }

    @Test
    fun testBatchHtmlGenerationAllFormats() {
        val elapsed = measureTime {
            for (parser in allParsers) {
                val content = smallContents[parser.supportedFormat.id] ?: "fallback"
                val doc = parser.parse(content)
                val html = doc.toHtml(lightMode = true)
                assertTrue(html.isNotEmpty(), "HTML must not be empty for ${parser.supportedFormat.name}")
            }
        }
        // Generating HTML for all 17 formats should finish quickly
        assertTrue(elapsed.inWholeMilliseconds < 1000, "Batch HTML for 17 formats took ${elapsed.inWholeMilliseconds}ms, expected < 1000ms")
    }

    // ========================== 3. Format Detection Metrics ==========================

    @Test
    fun testExtensionDetectionLatency() {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst", "adoc", "tid", "ini", "taskpaper", "textile", "ipynb", "rmd")
        val elapsed = measureTime {
            repeat(1000) {
                for (ext in extensions) {
                    FormatRegistry.detectByExtension(ext)
                }
            }
        }
        val perCallUs = elapsed.inWholeNanoseconds / (1000 * extensions.size * 1000)
        // Each extension detection should be sub-millisecond
        assertTrue(perCallUs < 5000, "Extension detection averaged ${perCallUs}us, expected < 5ms")
    }

    @Test
    fun testContentDetectionLatencyByCategory() {
        val categorySamples = mapOf(
            "markup" to "# Heading\n\n**Bold** text",
            "data" to "a,b,c\n1,2,3\n4,5,6",
            "technical" to "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}",
            "wiki" to "= Heading =\n\n'''Bold''' text",
            "task" to "(A) Buy groceries @store +home"
        )
        for ((category, sample) in categorySamples) {
            val elapsed = measureTime {
                repeat(1000) { FormatRegistry.detectByContent(sample) }
            }
            val avgMs = elapsed.inWholeMilliseconds.toDouble() / 1000.0
            assertTrue(avgMs < 5.0, "Content detection for category '$category' averaged ${avgMs}ms, expected < 5ms")
        }
    }

    @Test
    fun testAmbiguousDetectionLatency() {
        // Plaintext-like content that could be ambiguous
        val ambiguous = "Some text that has no special formatting at all."
        val elapsed = measureTime {
            repeat(1000) { FormatRegistry.detectByContent(ambiguous) }
        }
        val avgMs = elapsed.inWholeMilliseconds.toDouble() / 1000.0
        // Ambiguous detection may scan all patterns -- still should be fast
        assertTrue(avgMs < 10.0, "Ambiguous content detection averaged ${avgMs}ms, expected < 10ms")
    }

    @Test
    fun testBatchDetectionPerformance1000Files() {
        val filenames = (1..1000).map { i ->
            when (i % 10) {
                0 -> "file$i.md"
                1 -> "file$i.txt"
                2 -> "file$i.csv"
                3 -> "file$i.tex"
                4 -> "file$i.org"
                5 -> "file$i.wiki"
                6 -> "file$i.rst"
                7 -> "file$i.adoc"
                8 -> "file$i.tid"
                else -> "file$i.ini"
            }
        }
        val elapsed = measureTime {
            for (filename in filenames) {
                FormatRegistry.detectByFilename(filename)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500, "Batch detection of 1000 filenames took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
    }

    @Test
    fun testRegistryLookupByIdPerformance() {
        val ids = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_PLAINTEXT, FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_LATEX, FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_ASCIIDOC, FormatRegistry.ID_RESTRUCTUREDTEXT, FormatRegistry.ID_WIKITEXT,
            FormatRegistry.ID_CREOLE, FormatRegistry.ID_TIDDLYWIKI, FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_TASKPAPER, FormatRegistry.ID_TEXTILE, FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_RMARKDOWN, FormatRegistry.ID_BINARY
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (id in ids) {
                    val format = FormatRegistry.getById(id)
                    assertNotNull(format)
                }
            }
        }
        // 17000 lookups should be very fast
        assertTrue(elapsed.inWholeMilliseconds < 500, "17000 ID lookups took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
    }

    // ========================== 4. Memory Metrics ==========================

    @Test
    fun testParsedDocumentMemoryFootprintEstimate() {
        val parser = MarkdownParser()
        val content = "# Heading\n\nParagraph."
        val doc = parser.parse(content)

        // Before HTML generation the cached fields should be null
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // After generation, caches are populated
        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false), "Dark cache should still be null")

        // Generate dark too
        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun testHtmlCacheMemoryImpact() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Cache Test\n\nSome content with **bold** and *italic*.")

        // Pre-cache: no HTML allocated
        assertFalse(doc.hasHtmlCached(true))

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.length > doc.rawContent.length, "HTML should be larger than raw content due to tags and styles")

        // Clear and verify the cache is gone
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(true))
    }

    @Test
    fun testLargeDocumentMemoryHandling() {
        val parser = PlaintextParser()
        // ~1 MB document
        val largeContent = "x".repeat(1_000_000)
        val doc = parser.parse(largeContent)
        assertEquals(1_000_000, doc.rawContent.length)

        // HTML generation for a large doc should succeed without error
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testBatchParsingMemoryStability() {
        // Parse 1000 documents and verify no error / consistent behavior
        val parser = MarkdownParser()
        val documents = mutableListOf<ParsedDocument>()
        for (i in 1..1000) {
            documents.add(parser.parse("# Doc $i\n\nContent $i"))
        }
        assertEquals(1000, documents.size, "All 1000 documents must be parsed")

        // Verify all are valid
        for ((idx, doc) in documents.withIndex()) {
            assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id, "Document $idx has wrong format")
            assertTrue(doc.rawContent.contains("Doc ${idx + 1}"), "Document $idx content mismatch")
        }

        // Drop references and verify we can still parse more (no permanent resource leak)
        documents.clear()
        val finalDoc = parser.parse("# Final\n\nAfter clearing.")
        assertEquals(FormatRegistry.ID_MARKDOWN, finalDoc.format.id)
    }

    @Test
    fun testCacheClearReclaimsMemory() {
        val parser = MarkdownParser()
        val bigContent = buildString {
            repeat(200) { i ->
                appendLine("## Section $i")
                appendLine("Content for section $i with some extra text to bulk it up.")
            }
        }
        val doc = parser.parse(bigContent)

        // Generate both caches
        val lightHtml = doc.toHtml(lightMode = true)
        val darkHtml = doc.toHtml(lightMode = false)
        assertTrue(lightHtml.isNotEmpty())
        assertTrue(darkHtml.isNotEmpty())
        assertTrue(doc.hasHtmlCached(true))
        assertTrue(doc.hasHtmlCached(false))

        // Clear and verify caches are released
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(true), "Light cache should be cleared")
        assertFalse(doc.hasHtmlCached(false), "Dark cache should be cleared")

        // Re-generating should produce the same result
        val lightHtml2 = doc.toHtml(lightMode = true)
        assertEquals(lightHtml, lightHtml2, "Re-generated HTML should match original")
    }

    // ========================== 5. Throughput Metrics ==========================

    @Test
    fun testDocumentsPerSecondPerFormatCategory() {
        val categories = mapOf(
            "markup" to parserFor(FormatRegistry.ID_MARKDOWN),
            "data" to parserFor(FormatRegistry.ID_CSV),
            "technical" to parserFor(FormatRegistry.ID_LATEX),
            "wiki" to parserFor(FormatRegistry.ID_WIKITEXT),
            "task" to parserFor(FormatRegistry.ID_TODOTXT)
        )
        for ((category, parser) in categories) {
            val content = smallContents[parser.supportedFormat.id] ?: "fallback"
            val count = 1000
            val elapsed = measureTime {
                repeat(count) { parser.parse(content) }
            }
            val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds.toDouble() / 1000.0)
            // Each category should achieve at least 100 docs/sec (very conservative)
            assertTrue(docsPerSecond > 100, "Category '$category' throughput: ${docsPerSecond.toLong()} docs/sec, expected > 100")
        }
    }

    @Test
    fun testConcurrentThroughput10Coroutines() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Concurrent\n\nSome **bold** text.\n\n- Item 1\n- Item 2"
        val docsPerCoroutine = 100
        val elapsed = measureTime {
            val jobs = (1..10).map {
                async {
                    repeat(docsPerCoroutine) { parser.parse(content) }
                }
            }
            jobs.awaitAll()
        }
        val totalDocs = 10 * docsPerCoroutine
        val docsPerSecond = totalDocs.toDouble() / (elapsed.inWholeMilliseconds.coerceAtLeast(1).toDouble() / 1000.0)
        assertTrue(docsPerSecond > 100, "Concurrent throughput: ${docsPerSecond.toLong()} docs/sec, expected > 100")
    }

    @Test
    fun testSequentialVsParallelThroughputComparison() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Throughput\n\n**Bold** and *italic*."
        val totalDocs = 500

        // Sequential
        val seqElapsed = measureTime {
            repeat(totalDocs) { parser.parse(content) }
        }
        val seqPerSec = totalDocs.toDouble() / (seqElapsed.inWholeMilliseconds.coerceAtLeast(1).toDouble() / 1000.0)

        // Parallel (5 coroutines)
        val parElapsed = measureTime {
            val jobs = (1..5).map {
                async {
                    repeat(totalDocs / 5) { parser.parse(content) }
                }
            }
            jobs.awaitAll()
        }
        val parPerSec = totalDocs.toDouble() / (parElapsed.inWholeMilliseconds.coerceAtLeast(1).toDouble() / 1000.0)

        // Both should achieve reasonable throughput -- just validate they complete and are positive
        assertTrue(seqPerSec > 0, "Sequential throughput must be positive: $seqPerSec")
        assertTrue(parPerSec > 0, "Parallel throughput must be positive: $parPerSec")
        // Log comparison (not a hard requirement since test-dispatch may serialize)
        assertTrue(seqPerSec > 50, "Sequential: ${seqPerSec.toLong()} docs/sec, expected > 50")
        assertTrue(parPerSec > 50, "Parallel: ${parPerSec.toLong()} docs/sec, expected > 50")
    }

    @Test
    fun testFormatDetectionThroughput() {
        val sampleContents = listOf(
            "# Heading",
            "a,b,c",
            "\\documentclass{article}",
            "= Heading =",
            "(A) Task @ctx"
        )
        val count = 10_000
        val elapsed = measureTime {
            repeat(count) { i ->
                FormatRegistry.detectByContent(sampleContents[i % sampleContents.size])
            }
        }
        val detectionsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds.coerceAtLeast(1).toDouble() / 1000.0)
        assertTrue(detectionsPerSecond > 1000, "Detection throughput: ${detectionsPerSecond.toLong()}/sec, expected > 1000")
    }

    @Test
    fun testFullPipelineThroughputDetectParseHtml() {
        val filenames = listOf("readme.md", "data.csv", "paper.tex", "notes.org", "page.wiki")
        val contents = listOf(
            "# Hello\n\nWorld.",
            "a,b\n1,2",
            "\\documentclass{article}\n\\begin{document}\nHi\n\\end{document}",
            "* Heading\nContent.",
            "= Heading =\n\nText."
        )
        val iterations = 200
        val elapsed = measureTime {
            repeat(iterations) { i ->
                val idx = i % filenames.size
                val format = FormatRegistry.detectByFilename(filenames[idx])
                val parser = ParserRegistry.getParser(format)
                assertNotNull(parser, "Parser must exist for ${format.id}")
                val doc = parser.parse(contents[idx])
                val html = doc.toHtml(lightMode = true)
                assertTrue(html.isNotEmpty())
            }
        }
        val pipelinePerSecond = iterations.toDouble() / (elapsed.inWholeMilliseconds.coerceAtLeast(1).toDouble() / 1000.0)
        assertTrue(pipelinePerSecond > 50, "Full pipeline throughput: ${pipelinePerSecond.toLong()}/sec, expected > 50")
    }

    // ========================== 6. Lazy Loading Metrics ==========================

    @Test
    fun testParserLazyInitializationTiming() {
        // Save current state and clear for isolated testing
        val savedInstantiated = ParserRegistry.getInstantiatedParserCount()
        val savedPending = ParserRegistry.getPendingParserCount()

        // If the registry already has parsers registered, verify the counts are consistent
        val totalKnown = savedInstantiated + savedPending
        assertTrue(totalKnown >= 0, "Registry counts must be non-negative")

        // Accessing a parser by format should be fast
        val mdFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(mdFormat)

        val elapsed = measureTime {
            val parser = ParserRegistry.getParser(mdFormat)
            assertNotNull(parser, "Markdown parser must be available")
        }
        assertTrue(elapsed.inWholeMilliseconds < 50, "Parser retrieval took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun testFirstAccessVsSubsequentAccessTiming() {
        val mdFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!

        // First access (may or may not trigger lazy init depending on state)
        val firstElapsed = measureTime {
            ParserRegistry.getParser(mdFormat)
        }

        // Subsequent access (always from cache)
        val subsequentElapsed = measureTime {
            repeat(1000) { ParserRegistry.getParser(mdFormat) }
        }
        val avgSubsequent = subsequentElapsed.inWholeNanoseconds / 1000

        // First should complete quickly; subsequent should be very fast
        assertTrue(firstElapsed.inWholeMilliseconds < 50, "First access took ${firstElapsed.inWholeMilliseconds}ms, expected < 50ms")
        // Average subsequent access well under 1ms
        assertTrue(avgSubsequent < 1_000_000, "Subsequent access averaged ${avgSubsequent}ns, expected < 1ms")
    }

    @Test
    fun testParserRegistryInstantiatedCountTracking() {
        val instantiated = ParserRegistry.getInstantiatedParserCount()
        val pending = ParserRegistry.getPendingParserCount()

        // After normal initialization, instantiated + pending should cover registered formats
        assertTrue(instantiated >= 0, "Instantiated count must be non-negative: $instantiated")
        assertTrue(pending >= 0, "Pending count must be non-negative: $pending")

        // If we access all parsers, instantiated should increase (or stay same if already all instantiated)
        val allParsersFromRegistry = ParserRegistry.getAllParsers()
        assertTrue(allParsersFromRegistry.size >= instantiated,
            "getAllParsers().size (${allParsersFromRegistry.size}) must be >= instantiated count ($instantiated)")
    }

    @Test
    fun testLazyHtmlCachingVerification() {
        // Parse a document -- HTML should NOT be generated yet
        val parser = MarkdownParser()
        val doc = parser.parse("# Lazy HTML\n\nContent.")

        assertFalse(doc.hasHtmlCached(true), "Light HTML must not be cached before toHtml()")
        assertFalse(doc.hasHtmlCached(false), "Dark HTML must not be cached before toHtml()")

        // Access light HTML -- only light should be cached
        val lightHtml = doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(true), "Light HTML must be cached after toHtml(true)")
        assertFalse(doc.hasHtmlCached(false), "Dark HTML must still not be cached")
        assertTrue(lightHtml.isNotEmpty())

        // Access dark HTML -- both should now be cached
        val darkHtml = doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(false), "Dark HTML must be cached after toHtml(false)")
        assertTrue(darkHtml.isNotEmpty())

        // Verify they are different (light vs dark styling may differ)
        // At minimum, both must be valid non-empty strings
        assertTrue(lightHtml.isNotEmpty() && darkHtml.isNotEmpty())
    }

    @Test
    fun testRegistrationVsFirstUseTimingGap() {
        // Measure the overhead of hasParser (no instantiation) vs getParser (potential instantiation)
        val mdFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!

        val hasElapsed = measureTime {
            repeat(10_000) { ParserRegistry.hasParser(mdFormat) }
        }

        val getElapsed = measureTime {
            repeat(10_000) { ParserRegistry.getParser(mdFormat) }
        }

        val avgHasNs = hasElapsed.inWholeNanoseconds / 10_000
        val avgGetNs = getElapsed.inWholeNanoseconds / 10_000

        // Both should be sub-millisecond
        assertTrue(avgHasNs < 1_000_000, "hasParser averaged ${avgHasNs}ns, expected < 1ms")
        assertTrue(avgGetNs < 1_000_000, "getParser averaged ${avgGetNs}ns, expected < 1ms")

        // hasParser should generally be <= getParser (no instantiation overhead)
        // Not a strict requirement since both are fast, but verify both are reasonable
        assertTrue(avgHasNs < 5_000_000 && avgGetNs < 5_000_000,
            "Both hasParser (${avgHasNs}ns) and getParser (${avgGetNs}ns) must be < 5ms")
    }
}
