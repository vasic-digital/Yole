/*#######################################################
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Performance baseline tests for format parsers and
 * FormatRegistry operations. Each test verifies that
 * operations complete within acceptable time bounds.
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
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Performance baseline tests for the Yole format system.
 *
 * These tests establish performance baselines by verifying that core
 * operations complete within generous time bounds. The time limits are
 * intentionally high (1-5 seconds) to avoid flaky tests on slow CI
 * machines while still catching catastrophic performance regressions.
 *
 * Categories:
 * 1. Parser performance: Each format parser parses 10KB in under 1s
 * 2. Registry performance: Format detection operations are fast
 * 3. HTML generation: toHtml() completes in reasonable time
 * 4. Batch operations: Multiple operations in sequence
 *
 * Total: ~30 tests
 */
class PerformanceMetricsTests {

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
    // Test data generators
    // ====================================================================

    /** Generate ~10KB of format-appropriate content */
    private fun generateMarkdownContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        var i = 0
        while (length < targetSize) {
            appendLine("# Heading ${i++}")
            appendLine()
            appendLine("This is a paragraph with **bold**, *italic*, and `code` formatting.")
            appendLine("Here is a [link](https://example.com) and an ![image](img.png).")
            appendLine()
            appendLine("- Item 1")
            appendLine("- Item 2")
            appendLine("- Item 3")
            appendLine()
            appendLine("```kotlin")
            appendLine("fun main() { println(\"Hello\") }")
            appendLine("```")
            appendLine()
        }
    }

    private fun generatePlaintextContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        while (length < targetSize) {
            appendLine("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
            appendLine("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
        }
    }

    private fun generateCsvContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        appendLine("Name,Age,City,Country,Email")
        var i = 0
        while (length < targetSize) {
            appendLine("User${i},${20 + i % 50},City${i % 100},Country${i % 30},user${i}@example.com")
            i++
        }
    }

    private fun generateLatexContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        appendLine("\\documentclass{article}")
        appendLine("\\begin{document}")
        var i = 0
        while (length < targetSize) {
            appendLine("\\section{Section ${i++}}")
            appendLine("This is paragraph text with \\textbf{bold} and \\textit{italic}.")
            appendLine("\\begin{enumerate}")
            appendLine("\\item First item")
            appendLine("\\item Second item")
            appendLine("\\end{enumerate}")
        }
        appendLine("\\end{document}")
    }

    private fun generateOrgModeContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        var i = 0
        while (length < targetSize) {
            appendLine("* Heading ${i++}")
            appendLine("Some text with *bold* and /italic/ markup.")
            appendLine("- [ ] Todo item")
            appendLine("- [X] Done item")
            appendLine("#+BEGIN_SRC python")
            appendLine("print('hello')")
            appendLine("#+END_SRC")
        }
    }

    private fun generateTodoTxtContent(sizeKb: Int = 10): String = buildString {
        val targetSize = sizeKb * 1024
        var i = 0
        while (length < targetSize) {
            val priority = ('A' + i % 26)
            appendLine("($priority) 2024-01-${(i % 28) + 1} Task number $i +project${i % 5} @context${i % 3}")
            if (i % 3 == 0) appendLine("x 2024-02-01 2024-01-15 Completed task $i +done")
            i++
        }
    }

    // ====================================================================
    // 1. Parser performance: 10KB document under 1 second
    // ====================================================================

    @Test
    fun testMarkdownParserPerformance10Kb() {
        val content = generateMarkdownContent(10)
        val parser = MarkdownParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Markdown parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testPlaintextParserPerformance10Kb() {
        val content = generatePlaintextContent(10)
        val parser = PlaintextParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Plaintext parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testTodoTxtParserPerformance10Kb() {
        val content = generateTodoTxtContent(10)
        val parser = TodoTxtParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "TodoTxt parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testCsvParserPerformance10Kb() {
        val content = generateCsvContent(10)
        val parser = CsvParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "CSV parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testWikitextParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                appendLine("== Section ${i++} ==")
                appendLine("'''Bold''' and ''italic'' text.")
                appendLine("* Item 1\n* Item 2")
                appendLine("[[Link|Display text]]")
            }
        }
        val parser = WikitextParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "WikiText parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testCreoleParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                appendLine("= Heading ${i++}")
                appendLine("**Bold** and //italic// text.")
                appendLine("* Bullet item")
                appendLine("[[link|text]]")
            }
        }
        val parser = CreoleParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Creole parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testTiddlyWikiParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                appendLine("! Heading ${i++}")
                appendLine("''Bold'' and //italic// text.")
                appendLine("* Bullet item")
                appendLine("[[WikiLink]]")
            }
        }
        val parser = TiddlyWikiParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "TiddlyWiki parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testLatexParserPerformance10Kb() {
        val content = generateLatexContent(10)
        val parser = LatexParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "LaTeX parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testAsciidocParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                appendLine("= Section ${i++}")
                appendLine("*Bold* and _italic_ text.")
                appendLine("* Item 1")
                appendLine("NOTE: This is a note")
            }
        }
        val parser = AsciidocParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "AsciiDoc parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testOrgModeParserPerformance10Kb() {
        val content = generateOrgModeContent(10)
        val parser = OrgModeParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "OrgMode parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testRstParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                val title = "Section ${i++}"
                appendLine(title)
                appendLine("=".repeat(title.length))
                appendLine()
                appendLine("Paragraph with **bold** and *italic* text.")
                appendLine()
                appendLine(".. note::")
                appendLine("   This is a note.")
                appendLine()
            }
        }
        val parser = RestructuredTextParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "RST parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testKeyValueParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                if (i % 20 == 0) appendLine("[section_${i / 20}]")
                appendLine("key_${i}=value_${i}_with_some_longer_text_content")
                i++
            }
        }
        val parser = KeyValueParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "KeyValue parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testTaskpaperParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                if (i % 10 == 0) appendLine("Project ${i / 10}:")
                appendLine("\t- Task $i @due(2024-01-${(i % 28) + 1}) @priority(high)")
                appendLine("\t\tNote about task $i")
                i++
            }
        }
        val parser = TaskpaperParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "TaskPaper parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testTextileParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            var i = 0
            while (length < targetSize) {
                appendLine("h${(i % 6) + 1}. Heading ${i++}")
                appendLine()
                appendLine("*Bold* and _italic_ text with \"link\":http://example.com")
                appendLine()
                appendLine("* Item 1")
                appendLine("* Item 2")
                appendLine()
            }
        }
        val parser = TextileParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Textile parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testRMarkdownParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
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
        val parser = RMarkdownParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "RMarkdown parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testBinaryParserPerformance10Kb() {
        val content = buildString {
            val targetSize = 10 * 1024
            while (length < targetSize) {
                for (b in 0..255) append(b.toChar())
            }
        }
        val parser = BinaryParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Binary parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testJupyterParserPerformance10Kb() {
        // Generate a valid Jupyter notebook JSON of ~10KB
        val cells = buildString {
            val targetSize = 9 * 1024 // Leave room for wrapper
            var i = 0
            while (length < targetSize) {
                if (length > 0) append(",")
                append("""{"cell_type":"code","source":["print('cell $i')"],"metadata":{},"execution_count":$i,"outputs":[]}""")
                i++
            }
        }
        val content = """{"cells":[$cells],"metadata":{},"nbformat":4,"nbformat_minor":5}"""
        val parser = JupyterParser()
        val elapsed = measureTime { parser.parse(content) }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "Jupyter parsing 10KB should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 2. FormatRegistry detection performance
    // ====================================================================

    @Test
    fun testDetectByExtensionPerformance() {
        val extensions = listOf(
            "test.md", "test.txt", "test.csv", "test.tex", "test.org",
            "test.wiki", "test.adoc", "test.rst", "test.ini", "test.taskpaper",
            "test.textile", "test.creole", "test.tid", "test.ipynb", "test.rmd"
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (ext in extensions) {
                    FormatRegistry.detectByExtension(ext)
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "15,000 detectByExtension calls should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testDetectByContentPerformance() {
        val contents = listOf(
            "# Heading\nSome text",
            "\\documentclass{article}",
            "* Org heading",
            "== Wiki heading ==",
            "= AsciiDoc heading",
            "key=value",
            "name,age,city"
        )
        val elapsed = measureTime {
            repeat(100) {
                for (content in contents) {
                    FormatRegistry.detectByContent(content)
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "700 detectByContent calls should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testDetectByFilenamePerformance() {
        val filenames = listOf(
            "README.md", "todo.txt", "data.csv", "paper.tex",
            "notes.org", "page.wiki", "manual.adoc", "guide.rst"
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (name in filenames) {
                    FormatRegistry.detectByFilename(name)
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "8,000 detectByFilename calls should complete in <1s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testGetByIdPerformance() {
        val ids = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE, FormatRegistry.ID_WIKITEXT
        )
        val elapsed = measureTime {
            repeat(1000) {
                for (id in ids) {
                    FormatRegistry.getById(id)
                }
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "6,000 getById calls should complete in <500ms, took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 3. HTML generation performance
    // ====================================================================

    @Test
    fun testMarkdownHtmlGenerationPerformance() {
        val parser = MarkdownParser()
        val content = generateMarkdownContent(10)
        val doc = parser.parse(content)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "Markdown HTML generation for 10KB should be <2s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testMarkdownHtmlCachingPerformance() {
        val parser = MarkdownParser()
        val content = generateMarkdownContent(10)
        val doc = parser.parse(content)
        // First call generates HTML
        doc.toHtml(lightMode = true)
        // Second call should be nearly instant (cached)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 10,
            "Cached HTML retrieval should be <10ms, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testPlaintextHtmlGenerationPerformance() {
        val parser = PlaintextParser()
        val content = generatePlaintextContent(10)
        val doc = parser.parse(content)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "Plaintext HTML generation for 10KB should be <2s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testLatexHtmlGenerationPerformance() {
        val parser = LatexParser()
        val content = generateLatexContent(10)
        val doc = parser.parse(content)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "LaTeX HTML generation for 10KB should be <2s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testCsvHtmlGenerationPerformance() {
        val parser = CsvParser()
        val content = generateCsvContent(10)
        val doc = parser.parse(content)
        val elapsed = measureTime { doc.toHtml(lightMode = true) }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "CSV HTML generation for 10KB should be <2s, took ${elapsed.inWholeMilliseconds}ms")
    }

    // ====================================================================
    // 4. Batch operation performance
    // ====================================================================

    @Test
    fun testBatchParsingMultipleFormats() {
        val parsersAndContent = listOf(
            MarkdownParser() to generateMarkdownContent(5),
            PlaintextParser() to generatePlaintextContent(5),
            CsvParser() to generateCsvContent(5),
            LatexParser() to generateLatexContent(5),
            TodoTxtParser() to generateTodoTxtContent(5)
        )
        val elapsed = measureTime {
            for ((parser, content) in parsersAndContent) {
                val doc = parser.parse(content)
                doc.toHtml(lightMode = true)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 5000,
            "Parsing + HTML for 5 formats (5KB each) should be <5s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testRepeatedParsingStability() {
        val parser = MarkdownParser()
        val content = generateMarkdownContent(1) // 1KB
        val elapsed = measureTime {
            repeat(100) {
                parser.parse(content)
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 5000,
            "100x parsing of 1KB Markdown should be <5s, took ${elapsed.inWholeMilliseconds}ms")
    }

    @Test
    fun testFormatRegistryAllOperationsBatch() {
        val elapsed = measureTime {
            repeat(100) {
                FormatRegistry.formats
                FormatRegistry.getFormatNames()
                FormatRegistry.getAllExtensions()
                FormatRegistry.isSupported(FormatRegistry.ID_MARKDOWN)
                FormatRegistry.isExtensionSupported(".md")
                FormatRegistry.getFormatsByExtension(".txt")
                FormatRegistry.detectByExtension("test.md")
                FormatRegistry.detectByFilename("README.md")
                FormatRegistry.detectByContent("# Heading")
                FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
                FormatRegistry.getByExtension("md")
            }
        }
        assertTrue(elapsed.inWholeMilliseconds < 2000,
            "1,100 registry operations should be <2s, took ${elapsed.inWholeMilliseconds}ms")
    }
}
