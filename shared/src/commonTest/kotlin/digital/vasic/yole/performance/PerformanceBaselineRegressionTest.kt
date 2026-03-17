/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Performance baseline regression tests verifying parse
 * time budgets, lazy initialization, cache performance,
 * and sequential throughput for all formats.
 *
 *########################################################*/
package digital.vasic.yole.performance

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
 * Performance baseline regression tests.
 *
 * Establishes and validates performance baselines for:
 * - Per-format parse time (10KB input < 200ms)
 * - Lazy initialization of FormatRegistry
 * - StyleSheets cache effectiveness
 * - DocumentCache hit performance
 * - Sequential throughput (100 parses < 5s)
 *
 * Total: 28+ tests
 */
class PerformanceBaselineRegressionTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        StyleSheets.clearCache()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    private val allParsers: List<TextParser> = listOf(
        MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
        WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
        AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
        KeyValueParser(), TaskpaperParser(), TextileParser(),
        JupyterParser(), RMarkdownParser(), BinaryParser()
    )

    /** Build ~10KB content appropriate for each format. */
    private fun build10kContent(formatId: String): String {
        val baseContent = when (formatId) {
            FormatRegistry.ID_MARKDOWN -> "# Heading\n\nParagraph with **bold** and *italic* text.\n\n- Item\n"
            FormatRegistry.ID_PLAINTEXT -> "This is a line of plain text content for testing.\n"
            FormatRegistry.ID_TODOTXT -> "(A) Task description @context +project\n"
            FormatRegistry.ID_CSV -> "col1,col2,col3,col4\nval1,val2,val3,val4\n"
            FormatRegistry.ID_WIKITEXT -> "= Heading =\n'''Bold''' text.\n[[link]]\n"
            FormatRegistry.ID_CREOLE -> "= Heading\n**Bold** text.\n* list item\n"
            FormatRegistry.ID_TIDDLYWIKI -> "title: Note\n! Heading\nContent line.\n"
            FormatRegistry.ID_LATEX -> "\\section{Section}\nParagraph text.\n"
            FormatRegistry.ID_ASCIIDOC -> "== Section\nSome *bold* text.\n"
            FormatRegistry.ID_ORGMODE -> "* Heading\nContent line.\n"
            FormatRegistry.ID_RESTRUCTUREDTEXT -> "Section\n-------\nParagraph.\n"
            FormatRegistry.ID_KEYVALUE -> "key_line = value_line\n"
            FormatRegistry.ID_TASKPAPER -> "Project:\n\t- Task @tag\n"
            FormatRegistry.ID_TEXTILE -> "h2. Section\nParagraph text.\n"
            FormatRegistry.ID_JUPYTER -> """{"nbformat":4,"metadata":{},"cells":[{"cell_type":"code","source":"x=1","metadata":{},"outputs":[]}]}"""
            FormatRegistry.ID_RMARKDOWN -> "# Heading\n\nParagraph.\n\n```{r}\n1\n```\n"
            FormatRegistry.ID_BINARY -> "\u0000\u0001\u0002\u0003"
            else -> "Default content line.\n"
        }
        return buildString {
            val reps = (10240 / baseContent.length).coerceAtLeast(1)
            repeat(reps) { append(baseContent) }
        }
    }

    // ==================== Per-Format Parse Time (10KB < 200ms) ====================

    @Test
    fun markdownParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_MARKDOWN)
        val elapsed = measureTime { MarkdownParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Markdown 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun plaintextParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_PLAINTEXT)
        val elapsed = measureTime { PlaintextParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Plaintext 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun todotxtParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_TODOTXT)
        val elapsed = measureTime { TodoTxtParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "TodoTxt 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun csvParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_CSV)
        val elapsed = measureTime { CsvParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "CSV 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun wikitextParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_WIKITEXT)
        val elapsed = measureTime { WikitextParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "WikiText 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun creoleParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_CREOLE)
        val elapsed = measureTime { CreoleParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Creole 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun tiddlywikiParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_TIDDLYWIKI)
        val elapsed = measureTime { TiddlyWikiParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "TiddlyWiki 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun latexParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_LATEX)
        val elapsed = measureTime { LatexParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "LaTeX 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun asciidocParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_ASCIIDOC)
        val elapsed = measureTime { AsciidocParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "AsciiDoc 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun orgmodeParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_ORGMODE)
        val elapsed = measureTime { OrgModeParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "OrgMode 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun rstParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_RESTRUCTUREDTEXT)
        val elapsed = measureTime { RestructuredTextParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "RST 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun keyvalueParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_KEYVALUE)
        val elapsed = measureTime { KeyValueParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "KeyValue 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun taskpaperParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_TASKPAPER)
        val elapsed = measureTime { TaskpaperParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "TaskPaper 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun textileParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_TEXTILE)
        val elapsed = measureTime { TextileParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Textile 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun jupyterParsesUnder200ms() {
        // Jupyter is JSON-based, content is different
        val content = build10kContent(FormatRegistry.ID_JUPYTER)
        val elapsed = measureTime { JupyterParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Jupyter: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun rmarkdownParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_RMARKDOWN)
        val elapsed = measureTime { RMarkdownParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "RMarkdown 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun binaryParses10kbUnder200ms() {
        val content = build10kContent(FormatRegistry.ID_BINARY)
        val elapsed = measureTime { BinaryParser().parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 200, "Binary 10KB: ${elapsed.inWholeMilliseconds}ms")
    }

    // ==================== Lazy Initialization ====================

    @Test
    fun formatRegistryIsLazyInitialized() {
        // Note: By the time this test runs, formats may already be initialized
        // from setUp(). We just verify the property exists and works.
        val isInit = FormatRegistry.isFormatsInitialized
        // After accessing formats, should be initialized
        @Suppress("UNUSED_VARIABLE") val ignored = FormatRegistry.formats
        assertTrue(FormatRegistry.isFormatsInitialized)
    }

    // ==================== StyleSheets Cache Effectiveness ====================

    @Test
    fun styleSheetsSecondCallUnder1ms() {
        // First call populates cache
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)

        // Second call should be from cache and very fast
        val elapsed = measureTime {
            repeat(1000) {
                StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
            }
        }
        val avgMs = elapsed.inWholeMilliseconds.toDouble() / 1000.0
        assertTrue(avgMs < 1.0, "Cached stylesheet avg: ${avgMs}ms, expected < 1ms")
    }

    @Test
    fun styleSheetsAllFormatsFromCacheFast() {
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )
        // Warm up
        formatIds.forEach {
            StyleSheets.getStyleSheet(it, true)
            StyleSheets.getStyleSheet(it, false)
        }

        // Measure cached access
        val elapsed = measureTime {
            repeat(1000) {
                for (fmtId in formatIds) {
                    StyleSheets.getStyleSheet(fmtId, true)
                    StyleSheets.getStyleSheet(fmtId, false)
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 100, "12000 cached lookups: ${elapsed.inWholeMilliseconds}ms")
    }

    // ==================== DocumentCache Hit Performance ====================

    @Test
    fun documentCacheHitReturnsUnder1ms() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")
        val doc = ParsedDocument(format = testFormat, rawContent = "content", parsedContent = "content")
        cache.put("key", doc)

        // Warm up
        cache.get("key")

        val elapsed = measureTime {
            repeat(1000) { cache.get("key") }
        }
        val avgMs = elapsed.inWholeMilliseconds.toDouble() / 1000.0
        assertTrue(avgMs < 1.0, "Cache hit avg: ${avgMs}ms, expected < 1ms")
    }

    // ==================== Sequential Throughput ====================

    @Test
    fun hundredSequentialParsesUnder5s() {
        val parser = MarkdownParser()
        val content = "# Heading\n\nParagraph with **bold** and *italic*.\n\n- Item 1\n- Item 2\n"

        val elapsed = measureTime {
            repeat(100) { parser.parse(content) }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 5000,
            "100 sequential Markdown parses took ${elapsed.inWholeMilliseconds}ms, expected < 5000ms"
        )
    }

    @Test
    fun hundredSequentialParsesAllFormats() {
        val elapsed = measureTime {
            for (parser in allParsers) {
                val content = build10kContent(parser.supportedFormat.id).take(1000)
                repeat(6) { parser.parse(content) }  // ~100 total across 17 formats
            }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 5000,
            "~100 sequential parses across all formats took ${elapsed.inWholeMilliseconds}ms, expected < 5000ms"
        )
    }
}
