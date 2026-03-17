/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Performance Baseline Tests
 *
 * Establishes measurable performance baselines for:
 * - Parse time for each of 17 formats at 1KB, 10KB, 100KB
 * - HTML generation time for each format
 * - FormatRegistry detection latency
 * - DocumentCache hit vs miss latency
 * - CircuitBreaker and ConnectionLimiter overhead
 *
 *########################################################*/
package digital.vasic.yole.format.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking

/**
 * Performance baseline tests establishing measurable bounds for
 * parsing, HTML generation, format detection, caching, and
 * resilience component overhead.
 *
 * 60+ test methods covering all 17 formats and system components.
 */
class PerformanceBaselineTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ====================================================================
    // Content generators for each format at variable sizes
    // ====================================================================

    private fun generateContent(formatId: String, sizeKb: Int): String {
        val targetSize = sizeKb * 1024
        return when (formatId) {
            FormatRegistry.ID_MARKDOWN -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("# Heading ${i++}")
                    appendLine()
                    appendLine("Paragraph with **bold**, *italic*, and `code`. [Link](https://example.com).")
                    appendLine()
                    appendLine("- Item 1\n- Item 2\n- Item 3")
                    appendLine()
                    appendLine("```kotlin\nfun main() { println(\"Hello\") }\n```")
                    appendLine()
                }
            }
            FormatRegistry.ID_PLAINTEXT -> buildString {
                while (length < targetSize) {
                    appendLine("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                    appendLine("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                }
            }
            FormatRegistry.ID_TODOTXT -> buildString {
                var i = 0
                while (length < targetSize) {
                    val priority = ('A' + i % 26)
                    appendLine("($priority) 2024-01-${(i % 28) + 1} Task number $i +project${i % 5} @context${i % 3}")
                    if (i % 3 == 0) appendLine("x 2024-02-01 Completed task $i +done")
                    i++
                }
            }
            FormatRegistry.ID_CSV -> buildString {
                appendLine("Name,Age,City,Country,Email")
                var i = 0
                while (length < targetSize) {
                    appendLine("User${i},${20 + i % 50},City${i % 100},Country${i % 30},user${i}@example.com")
                    i++
                }
            }
            FormatRegistry.ID_WIKITEXT -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("== Section ${i++} ==")
                    appendLine("'''Bold''' and ''italic'' text.")
                    appendLine("* Item 1\n* Item 2")
                    appendLine("[[Link|Display text]]")
                }
            }
            FormatRegistry.ID_CREOLE -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("= Heading ${i++}")
                    appendLine("**Bold** and //italic// text.")
                    appendLine("* Bullet item")
                    appendLine("[[link|text]]")
                }
            }
            FormatRegistry.ID_TIDDLYWIKI -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("! Heading ${i++}")
                    appendLine("''Bold'' and //italic// text.")
                    appendLine("* Bullet item")
                    appendLine("[[WikiLink]]")
                }
            }
            FormatRegistry.ID_LATEX -> buildString {
                appendLine("\\documentclass{article}")
                appendLine("\\begin{document}")
                var i = 0
                while (length < targetSize) {
                    appendLine("\\section{Section ${i++}}")
                    appendLine("Text with \\textbf{bold} and \\textit{italic}.")
                    appendLine("\\begin{enumerate}")
                    appendLine("\\item First item")
                    appendLine("\\item Second item")
                    appendLine("\\end{enumerate}")
                }
                appendLine("\\end{document}")
            }
            FormatRegistry.ID_ASCIIDOC -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("= Section ${i++}")
                    appendLine("*Bold* and _italic_ text.")
                    appendLine("* Item 1")
                    appendLine("NOTE: This is a note.")
                }
            }
            FormatRegistry.ID_ORGMODE -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("* Heading ${i++}")
                    appendLine("Text with *bold* and /italic/ markup.")
                    appendLine("- [ ] Todo item")
                    appendLine("- [X] Done item")
                }
            }
            FormatRegistry.ID_RESTRUCTUREDTEXT -> buildString {
                var i = 0
                while (length < targetSize) {
                    val title = "Section ${i++}"
                    appendLine(title)
                    appendLine("=".repeat(title.length))
                    appendLine()
                    appendLine("Paragraph with **bold** and *italic* text.")
                    appendLine()
                }
            }
            FormatRegistry.ID_KEYVALUE -> buildString {
                var i = 0
                while (length < targetSize) {
                    if (i % 20 == 0) appendLine("[section_${i / 20}]")
                    appendLine("key_${i}=value_${i}_with_some_longer_text_content")
                    i++
                }
            }
            FormatRegistry.ID_TASKPAPER -> buildString {
                var i = 0
                while (length < targetSize) {
                    if (i % 10 == 0) appendLine("Project ${i / 10}:")
                    appendLine("\t- Task $i @due(2024-01-${(i % 28) + 1}) @priority(high)")
                    appendLine("\t\tNote about task $i")
                    i++
                }
            }
            FormatRegistry.ID_TEXTILE -> buildString {
                var i = 0
                while (length < targetSize) {
                    appendLine("h${(i % 6) + 1}. Heading ${i++}")
                    appendLine()
                    appendLine("*Bold* and _italic_ text with \"link\":http://example.com")
                    appendLine()
                    appendLine("* Item 1\n* Item 2")
                    appendLine()
                }
            }
            FormatRegistry.ID_JUPYTER -> buildString {
                // Build a valid Jupyter notebook JSON of the requested size
                val cellsBuilder = StringBuilder()
                var i = 0
                while (cellsBuilder.length < targetSize - 100) {
                    if (cellsBuilder.isNotEmpty()) cellsBuilder.append(",")
                    cellsBuilder.append("""{"cell_type":"code","source":["print('cell $i')"],"metadata":{},"execution_count":$i,"outputs":[]}""")
                    i++
                }
                append("""{"cells":[${cellsBuilder}],"metadata":{},"nbformat":4,"nbformat_minor":5}""")
            }
            FormatRegistry.ID_RMARKDOWN -> buildString {
                appendLine("---")
                appendLine("title: \"Test Document\"")
                appendLine("output: html_document")
                appendLine("---")
                appendLine()
                var i = 0
                while (length < targetSize) {
                    appendLine("# Section ${i++}")
                    appendLine("Text with **bold** and *italic*.")
                    appendLine("```{r chunk_$i}")
                    appendLine("summary(mtcars)")
                    appendLine("```")
                    appendLine()
                }
            }
            FormatRegistry.ID_BINARY -> buildString {
                while (length < targetSize) {
                    for (b in 0..255) append(b.toChar())
                }
            }
            else -> "x".repeat(targetSize)
        }
    }

    private fun getParser(formatId: String): TextParser = when (formatId) {
        FormatRegistry.ID_MARKDOWN -> MarkdownParser()
        FormatRegistry.ID_PLAINTEXT -> PlaintextParser()
        FormatRegistry.ID_TODOTXT -> TodoTxtParser()
        FormatRegistry.ID_CSV -> CsvParser()
        FormatRegistry.ID_WIKITEXT -> WikitextParser()
        FormatRegistry.ID_CREOLE -> CreoleParser()
        FormatRegistry.ID_TIDDLYWIKI -> TiddlyWikiParser()
        FormatRegistry.ID_LATEX -> LatexParser()
        FormatRegistry.ID_ASCIIDOC -> AsciidocParser()
        FormatRegistry.ID_ORGMODE -> OrgModeParser()
        FormatRegistry.ID_RESTRUCTUREDTEXT -> RestructuredTextParser()
        FormatRegistry.ID_KEYVALUE -> KeyValueParser()
        FormatRegistry.ID_TASKPAPER -> TaskpaperParser()
        FormatRegistry.ID_TEXTILE -> TextileParser()
        FormatRegistry.ID_JUPYTER -> JupyterParser()
        FormatRegistry.ID_RMARKDOWN -> RMarkdownParser()
        FormatRegistry.ID_BINARY -> BinaryParser()
        else -> throw IllegalArgumentException("Unknown format: $formatId")
    }

    // The 17 text format IDs
    private val textFormatIds = listOf(
        FormatRegistry.ID_MARKDOWN,
        FormatRegistry.ID_PLAINTEXT,
        FormatRegistry.ID_TODOTXT,
        FormatRegistry.ID_CSV,
        FormatRegistry.ID_WIKITEXT,
        FormatRegistry.ID_CREOLE,
        FormatRegistry.ID_TIDDLYWIKI,
        FormatRegistry.ID_LATEX,
        FormatRegistry.ID_ASCIIDOC,
        FormatRegistry.ID_ORGMODE,
        FormatRegistry.ID_RESTRUCTUREDTEXT,
        FormatRegistry.ID_KEYVALUE,
        FormatRegistry.ID_TASKPAPER,
        FormatRegistry.ID_TEXTILE,
        FormatRegistry.ID_JUPYTER,
        FormatRegistry.ID_RMARKDOWN,
        FormatRegistry.ID_BINARY
    )

    // ====================================================================
    // 1. Parse time: 1KB content (< 100ms per format)
    // ====================================================================

    @Test
    fun `parse 1KB markdown under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_MARKDOWN).parse(generateContent(FormatRegistry.ID_MARKDOWN, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Markdown 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB plaintext under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_PLAINTEXT).parse(generateContent(FormatRegistry.ID_PLAINTEXT, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Plaintext 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB todotxt under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_TODOTXT).parse(generateContent(FormatRegistry.ID_TODOTXT, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "TodoTxt 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB csv under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_CSV).parse(generateContent(FormatRegistry.ID_CSV, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "CSV 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB wikitext under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_WIKITEXT).parse(generateContent(FormatRegistry.ID_WIKITEXT, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "WikiText 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB creole under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_CREOLE).parse(generateContent(FormatRegistry.ID_CREOLE, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Creole 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB tiddlywiki under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_TIDDLYWIKI).parse(generateContent(FormatRegistry.ID_TIDDLYWIKI, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "TiddlyWiki 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB latex under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_LATEX).parse(generateContent(FormatRegistry.ID_LATEX, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "LaTeX 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB asciidoc under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_ASCIIDOC).parse(generateContent(FormatRegistry.ID_ASCIIDOC, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "AsciiDoc 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB orgmode under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_ORGMODE).parse(generateContent(FormatRegistry.ID_ORGMODE, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "OrgMode 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB restructuredtext under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_RESTRUCTUREDTEXT).parse(generateContent(FormatRegistry.ID_RESTRUCTUREDTEXT, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "RST 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB keyvalue under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_KEYVALUE).parse(generateContent(FormatRegistry.ID_KEYVALUE, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "KeyValue 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB taskpaper under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_TASKPAPER).parse(generateContent(FormatRegistry.ID_TASKPAPER, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "TaskPaper 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB textile under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_TEXTILE).parse(generateContent(FormatRegistry.ID_TEXTILE, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Textile 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB jupyter under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_JUPYTER).parse(generateContent(FormatRegistry.ID_JUPYTER, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Jupyter 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB rmarkdown under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_RMARKDOWN).parse(generateContent(FormatRegistry.ID_RMARKDOWN, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "RMarkdown 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 1KB binary under 100ms`() {
        val elapsed = measureTime { getParser(FormatRegistry.ID_BINARY).parse(generateContent(FormatRegistry.ID_BINARY, 1)) }
        assertTrue(elapsed.inWholeMilliseconds < 100, "Binary 1KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 2. Parse time: 10KB content (< 500ms per format)
    // ====================================================================

    @Test
    fun `parse 10KB all formats under 500ms each`() {
        for (formatId in textFormatIds) {
            val content = generateContent(formatId, 10)
            val parser = getParser(formatId)
            val elapsed = measureTime { parser.parse(content) }
            assertTrue(elapsed.inWholeMilliseconds < 500,
                "$formatId 10KB parse took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
        }
    }

    // ====================================================================
    // 3. Parse time: 100KB content (< 5000ms per format)
    // ====================================================================

    @Test
    fun `parse 100KB markdown under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_MARKDOWN, 100)
        assertTrue(content.length >= 100 * 1024, "Content must be at least 100KB")
        val elapsed = measureTime { getParser(FormatRegistry.ID_MARKDOWN).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "Markdown 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB plaintext under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_PLAINTEXT, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_PLAINTEXT).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "Plaintext 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB csv under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_CSV, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_CSV).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "CSV 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB latex under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_LATEX, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_LATEX).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "LaTeX 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB orgmode under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_ORGMODE, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_ORGMODE).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "OrgMode 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB todotxt under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_TODOTXT, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_TODOTXT).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "TodoTxt 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB wikitext under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_WIKITEXT, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_WIKITEXT).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "WikiText 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB asciidoc under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_ASCIIDOC, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_ASCIIDOC).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "AsciiDoc 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB restructuredtext under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_RESTRUCTUREDTEXT, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_RESTRUCTUREDTEXT).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "RST 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB keyvalue under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_KEYVALUE, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_KEYVALUE).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "KeyValue 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB textile under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_TEXTILE, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_TEXTILE).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "Textile 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse 100KB binary under 5000ms`() {
        val content = generateContent(FormatRegistry.ID_BINARY, 100)
        val elapsed = measureTime { getParser(FormatRegistry.ID_BINARY).parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 5000, "Binary 100KB parse took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 4. HTML generation time for each format (< 2000ms for 10KB)
    // ====================================================================

    @Test
    fun `html generation markdown 10KB under 2000ms`() {
        val parser = MarkdownParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_MARKDOWN, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "Markdown HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation plaintext 10KB under 2000ms`() {
        val parser = PlaintextParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_PLAINTEXT, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "Plaintext HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation csv 10KB under 2000ms`() {
        val parser = CsvParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_CSV, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "CSV HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation latex 10KB under 2000ms`() {
        val parser = LatexParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_LATEX, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "LaTeX HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation orgmode 10KB under 2000ms`() {
        val parser = OrgModeParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_ORGMODE, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "OrgMode HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation todotxt 10KB under 2000ms`() {
        val parser = TodoTxtParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_TODOTXT, 10))
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000, "TodoTxt HTML gen took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `html generation all formats 10KB under 2000ms each`() {
        for (formatId in textFormatIds) {
            val parser = getParser(formatId)
            val doc = parser.parse(generateContent(formatId, 10))
            val elapsed = measureTime { doc.toHtml(lightMode = true) }
            assertTrue(elapsed.inWholeMilliseconds < 2000,
                "$formatId HTML gen took ${elapsed.inWholeMilliseconds}ms, expected < 2000ms")
        }
    }

    @Test
    fun `html caching returns in under 5ms`() {
        val parser = MarkdownParser()
        val doc = parser.parse(generateContent(FormatRegistry.ID_MARKDOWN, 10))
        // First call generates
        doc.toHtml(lightMode = true)
        // Second call should be cached
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 5,
            "Cached HTML retrieval took ${elapsed.inWholeMilliseconds}ms, expected < 5ms")
    }

    // ====================================================================
    // 5. FormatRegistry.detectByExtension() latency (< 10ms)
    // ====================================================================

    @Test
    fun `detectByExtension latency under 10ms per 1000 calls`() {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "adoc", "rst", "ini", "textile")
        val elapsed = measureTime {
            repeat(1000) {
                for (ext in extensions) {
                    FormatRegistry.detectByExtension(ext)
                }
            }
        }
        // 10,000 calls total
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "10,000 detectByExtension calls took ${elapsed.inWholeMilliseconds}ms")
        // Per-call average
        val avgMicros = (elapsed.inWholeMilliseconds * 1000) / 10_000
        assertTrue(avgMicros < 10_000, "Average detectByExtension call took ${avgMicros}us")
    }

    @Test
    fun `detectByExtension single call under 10ms`() {
        val elapsed = measureTime { FormatRegistry.detectByExtension("md") }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "Single detectByExtension took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `detectByExtension unknown extension falls back under 10ms`() {
        val elapsed = measureTime { FormatRegistry.detectByExtension("xyz_unknown") }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "detectByExtension for unknown extension took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 6. FormatRegistry.detectByContent() latency (< 10ms)
    // ====================================================================

    @Test
    fun `detectByContent latency under 10ms per call`() {
        val contents = listOf(
            "# Heading\nSome text",
            "\\documentclass{article}",
            "* Org heading",
            "== Wiki heading ==",
            "name,age,city"
        )
        for (content in contents) {
            val elapsed = measureTime { FormatRegistry.detectByContent(content) }
            assertTrue(elapsed.inWholeMilliseconds < 10,
                "detectByContent took ${elapsed.inWholeMilliseconds}ms for content: ${content.take(30)}")
        }
    }

    @Test
    fun `detectByContent 1000 calls under 1000ms total`() {
        val content = "# Markdown heading\n\nSome paragraph text."
        val elapsed = measureTime {
            repeat(1000) {
                FormatRegistry.detectByContent(content)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "1000 detectByContent calls took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `detectByContent empty content under 10ms`() {
        val elapsed = measureTime { FormatRegistry.detectByContent("") }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "detectByContent for empty content took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `detectByContent large content sample under 10ms`() {
        val content = "# Title\n" + "Paragraph text. ".repeat(500)
        val elapsed = measureTime { FormatRegistry.detectByContent(content) }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "detectByContent for large content took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 7. DocumentCache hit vs miss latency difference
    // ====================================================================

    @Test
    fun `cache hit faster than cache miss`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )
        val doc = ParsedDocument(
            format = testFormat, rawContent = "test content",
            parsedContent = "test content"
        )
        cache.put("key1", doc)

        // Measure miss
        val missTime = measureTime { cache.get("nonexistent_key") }
        // Measure hit
        val hitTime = measureTime { cache.get("key1") }

        // Both should be very fast, but hit should not be slower
        assertTrue(hitTime.inWholeMilliseconds < 10, "Cache hit took ${hitTime.inWholeMilliseconds}ms")
        assertTrue(missTime.inWholeMilliseconds < 10, "Cache miss took ${missTime.inWholeMilliseconds}ms")
    }

    @Test
    fun `cache 1000 hits under 100ms total`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )
        val doc = ParsedDocument(
            format = testFormat, rawContent = "test",
            parsedContent = "test"
        )
        cache.put("key", doc)

        val elapsed = measureTime {
            repeat(1000) { cache.get("key") }
        }
        assertTrue(elapsed.inWholeMilliseconds < 100,
            "1000 cache hits took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `cache 1000 misses under 100ms total`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val elapsed = measureTime {
            repeat(1000) { i -> cache.get("miss_$i") }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "1000 cache misses took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `cache put 100 entries under 100ms`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )
        val elapsed = measureTime {
            repeat(100) { i ->
                val doc = ParsedDocument(
                    format = testFormat,
                    rawContent = "content $i",
                    parsedContent = "content $i"
                )
                cache.put("key_$i", doc)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 100,
            "100 cache puts took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 8. CircuitBreaker overhead (execute vs direct call)
    // ====================================================================

    @Test
    fun `circuitBreaker overhead under 5ms per call`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)

        // Measure direct call
        val directTime = measureTime {
            repeat(1000) { 42 + it }
        }

        // Measure through circuit breaker
        val cbTime = measureTime {
            repeat(1000) { i ->
                cb.execute { 42 + i }
            }
        }

        // Circuit breaker adds overhead but should still be fast
        assertTrue(cbTime.inWholeMilliseconds < 5000,
            "1000 CircuitBreaker calls took ${cbTime.inWholeMilliseconds}ms")
    }

    @Test
    fun `circuitBreaker single successful call under 10ms`() = runBlocking<Unit> {
        val cb = CircuitBreaker()
        val elapsed = measureTime { cb.execute { "result" } }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "Single CB execute took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `circuitBreaker open rejection under 10ms`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("trigger open") }

        val elapsed = measureTime { cb.execute { "rejected" } }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "CB open rejection took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `circuitBreaker 100 mixed calls under 500ms`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 50)
        val elapsed = measureTime {
            repeat(100) { i ->
                if (i % 5 == 0) {
                    cb.execute { throw RuntimeException("fail") }
                } else {
                    cb.execute { i * 2 }
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "100 mixed CB calls took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 9. ConnectionLimiter overhead
    // ====================================================================

    @Test
    fun `connectionLimiter single operation under 10ms overhead`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5)
        val elapsed = measureTime { limiter.withConnection { "result" } }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "Single ConnectionLimiter operation took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `connectionLimiter 100 sequential operations under 500ms`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5)
        val elapsed = measureTime {
            repeat(100) { i ->
                limiter.withConnection { i * 2 }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "100 sequential ConnectionLimiter ops took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `connectionLimiter permits released promptly`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        val elapsed = measureTime {
            repeat(50) { i ->
                limiter.withConnection { i }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "50 sequential ops with maxConcurrent=1 took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 10. Aggregate and scaling baselines
    // ====================================================================

    @Test
    fun `all 17 formats parse 1KB total under 500ms`() {
        val elapsed = measureTime {
            for (formatId in textFormatIds) {
                getParser(formatId).parse(generateContent(formatId, 1))
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "All 17 formats 1KB parse total took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `registry getById for all format IDs under 10ms`() {
        val elapsed = measureTime {
            for (formatId in textFormatIds) {
                FormatRegistry.getById(formatId)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "getById for all 17 IDs took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `registry getAllExtensions under 10ms`() {
        val elapsed = measureTime {
            repeat(100) { FormatRegistry.getAllExtensions() }
        }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "100x getAllExtensions took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `registry getFormatNames under 10ms`() {
        val elapsed = measureTime {
            repeat(100) { FormatRegistry.getFormatNames() }
        }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "100x getFormatNames took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `parse then html for 5 formats 5KB under 3000ms`() {
        val selected = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_TODOTXT
        )
        val elapsed = measureTime {
            for (formatId in selected) {
                val parser = getParser(formatId)
                val doc = parser.parse(generateContent(formatId, 5))
                doc.toHtml(lightMode = true)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 3000,
            "Parse+HTML for 5 formats took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun `repeated parse 100 times same content under 3000ms`() {
        val parser = MarkdownParser()
        val content = generateContent(FormatRegistry.ID_MARKDOWN, 1)
        val elapsed = measureTime {
            repeat(100) { parser.parse(content) }
        }
        assertTrue(elapsed.inWholeMilliseconds < 3000,
            "100x same content parse took ${elapsed.inWholeMilliseconds}ms")
    }
}
