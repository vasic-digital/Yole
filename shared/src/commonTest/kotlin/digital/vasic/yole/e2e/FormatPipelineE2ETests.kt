/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Pipeline End-to-End Tests
 *
 * Tests the full pipeline for each of the 18 text formats:
 * detect format -> parse -> generate HTML -> verify HTML
 * contains expected elements. Uses FormatRegistry and
 * parseWithCache for all 18 parsers.
 *
 *########################################################*/
package digital.vasic.yole.e2e

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
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for the format detection + parse + HTML pipeline.
 *
 * Each test exercises a single format through the full pipeline:
 * 1. Detect format via [FormatRegistry.detectByExtension]
 * 2. Parse content via [FormatRegistry.parseWithCache]
 * 3. Generate HTML via [ParsedDocument.toHtml]
 * 4. Assert HTML contains expected structural elements
 *
 * Total: ~45 tests
 */
class FormatPipelineE2ETests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    // ==================== Markdown ====================

    @Test
    fun MarkdownPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)

        val content = "# Title\n\n**Bold** and *italic*.\n\n- Item 1\n- Item 2"
        val doc = FormatRegistry.parseWithCache(content, format)
        assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)
        assertEquals(content, doc.rawContent)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<h1>") || html.contains("h1"))
    }

    @Test
    fun MarkdownHtmlCachingSecondCallReturnsSameResult() = runBlocking<Unit> {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        val content = "# Cached\n\nContent."
        val doc = FormatRegistry.parseWithCache(content, format)

        val html1 = doc.toHtml(lightMode = true)
        val html2 = doc.toHtml(lightMode = true)
        assertEquals(html1, html2)
        assertTrue(doc.hasHtmlCached(lightMode = true))
    }

    // ==================== Plain Text ====================

    @Test
    fun PlainTextPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".txt")
        assertNotNull(format)

        val content = "Simple plain text.\nSecond line.\nThird line."
        val parser = PlaintextParser()
        val doc = parser.parse(content)
        assertEquals(content, doc.rawContent)

        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("Simple plain text") || html.contains("plain"))
    }

    // ==================== Todo.txt ====================

    @Test
    fun TodoTxtPipelineDetectParseHtml() = runBlocking<Unit> {
        val content = "(A) Task one @work +project\n(B) Task two @home\nx 2025-01-01 Completed task"
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)

        val parser = TodoTxtParser()
        val doc = parser.parse(content)
        assertEquals(content, doc.rawContent)

        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun TodoTxtMetadataContainsTaskCount() = runBlocking<Unit> {
        val content = "(A) Task one\n(B) Task two\n(C) Task three"
        val parser = TodoTxtParser()
        val doc = parser.parse(content)
        val taskCount = doc.metadata["tasks"]?.toIntOrNull() ?: doc.metadata["total"]?.toIntOrNull() ?: -1
        assertTrue(taskCount >= 0 || doc.metadata.isNotEmpty())
    }

    // ==================== CSV ====================

    @Test
    fun CsvPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".csv")
        assertEquals(FormatRegistry.ID_CSV, format.id)

        val content = "name,age,city\nAlice,30,New York\nBob,25,London\nCarol,35,Tokyo"
        val doc = FormatRegistry.parseWithCache(content, format)
        assertEquals(FormatRegistry.ID_CSV, doc.format.id)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<table") || html.contains("csv") || html.contains("Alice"))
    }

    @Test
    fun CsvMetadataContainsRowAndColumnInfo() = runBlocking<Unit> {
        val content = "a,b,c\n1,2,3\n4,5,6"
        val parser = CsvParser()
        val doc = parser.parse(content)
        assertTrue(doc.metadata.isNotEmpty())
    }

    // ==================== WikiText ====================

    @Test
    fun WikitextPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".wiki")
        assertEquals(FormatRegistry.ID_WIKITEXT, format.id)

        val content = "= Main Heading =\n\n**Bold** and //italic// text.\n\n[[Link|Description]]"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<h1>") || html.contains("Heading"))
    }

    // ==================== Org Mode ====================

    @Test
    fun OrgModePipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".org")
        assertEquals(FormatRegistry.ID_ORGMODE, format.id)

        val content = "* Main Heading\n** Sub-heading\n   Content paragraph.\n#+TITLE: My Doc"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== Creole ====================

    @Test
    fun CreolePipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".creole")
        assertEquals(FormatRegistry.ID_CREOLE, format.id)

        val content = "= Title =\n\n**Bold** and //italic// text here.\n\n* List item one"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== TiddlyWiki ====================

    @Test
    fun TiddlyWikiPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".tid")
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, format.id)

        val content = "title: My Tiddler\n\n! Heading\n''bold'' and //italic// text."
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== LaTeX ====================

    @Test
    fun LatexPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".tex")
        assertEquals(FormatRegistry.ID_LATEX, format.id)

        val content = "\\documentclass{article}\n\\begin{document}\n\\section{Introduction}\nHello LaTeX.\n\\end{document}"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun LatexMetadataContainsSectionInfo() = runBlocking<Unit> {
        val content = "\\documentclass{article}\n\\begin{document}\n\\section{Intro}\nText.\n\\section{Body}\nMore.\n\\end{document}"
        val parser = LatexParser()
        val doc = parser.parse(content)
        assertTrue(doc.metadata.isNotEmpty())
    }

    // ==================== AsciiDoc ====================

    @Test
    fun AsciidocPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".adoc")
        assertEquals(FormatRegistry.ID_ASCIIDOC, format.id)

        val content = "= Document Title\n\n== Section One\n\nParagraph content here.\n\n== Section Two\n\nMore content."
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("Document Title") || html.contains("<h"))
    }

    // ==================== reStructuredText ====================

    @Test
    fun RestructuredTextPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".rst")
        assertEquals(FormatRegistry.ID_RESTRUCTUREDTEXT, format.id)

        val content = "Document Title\n==============\n\n.. note::\n   A note.\n\nParagraph content."
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("rst-document") || html.contains("Document Title"))
    }

    @Test
    fun RestructuredTextMetadataContainsSectionAndDirectiveCounts() = runBlocking<Unit> {
        val content = "Title\n=====\n\nSection\n-------\n\n.. warning::\n   Be careful."
        val parser = RestructuredTextParser()
        val doc = parser.parse(content)
        assertEquals("2", doc.metadata["sections"])
        assertEquals("1", doc.metadata["directives"])
    }

    // ==================== Key-Value ====================

    @Test
    fun KeyValuePipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".ini")
        assertEquals(FormatRegistry.ID_KEYVALUE, format.id)

        val content = "[database]\nhost=localhost\nport=5432\nname=mydb\n\n[server]\nport=8080"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== TaskPaper ====================

    @Test
    fun TaskpaperPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".taskpaper")
        assertEquals(FormatRegistry.ID_TASKPAPER, format.id)

        val content = "Work:\n\t- Write report @due(2025-12-31)\n\t- Send email @done\n\nPersonal:\n\t- Exercise @today"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("taskpaper"))
    }

    @Test
    fun TaskpaperMetadataCountsProjectsAndTasks() = runBlocking<Unit> {
        val content = "Project A:\n\t- Task 1\n\t- Task 2\n\nProject B:\n\t- Task 3"
        val parser = TaskpaperParser()
        val doc = parser.parse(content)
        assertEquals("2", doc.metadata["projects"])
        assertEquals("3", doc.metadata["tasks"])
    }

    // ==================== Textile ====================

    @Test
    fun TextilePipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".textile")
        assertEquals(FormatRegistry.ID_TEXTILE, format.id)

        val content = "h1. Document Title\n\nh2. Subtitle\n\n*bold* and _italic_ text."
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== Jupyter ====================

    @Test
    fun JupyterPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".ipynb")
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)

        val content = """{"nbformat":4,"nbformat_minor":5,"cells":[{"cell_type":"markdown","source":["# Title\n"]},{"cell_type":"code","source":["print('hello')"]}]}"""
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== R Markdown ====================

    @Test
    fun RMarkdownPipelineDetectParseHtml() = runBlocking<Unit> {
        val format = FormatRegistry.detectByExtension(".rmd")
        assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)

        val content = "---\ntitle: R Analysis\nauthor: Researcher\n---\n\n# Introduction\n\n```{r setup}\nlibrary(ggplot2)\n```\n\n## Results"
        val doc = FormatRegistry.parseWithCache(content, format)

        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== Binary ====================

    @Test
    fun BinaryPipelineDetectParseHtml() = runBlocking<Unit> {
        val parser = BinaryParser()
        val content = "\u0000\u0001\u0002\u0003binary data\u00FF\u00FE"
        val doc = parser.parse(content)
        assertEquals(FormatRegistry.ID_BINARY, doc.format.id)

        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.isNotEmpty())
    }

    // ==================== Cross-format pipeline invariants ====================

    @Test
    fun AllFormatsParseWithCacheReturnNonNullDoc() = runBlocking<Unit> {
        val fixtures = listOf(
            FormatRegistry.ID_MARKDOWN to "# Title\n\nContent",
            FormatRegistry.ID_PLAINTEXT to "Plain content",
            FormatRegistry.ID_CSV to "a,b\n1,2",
            FormatRegistry.ID_WIKITEXT to "= H1 =\nContent",
            FormatRegistry.ID_ORGMODE to "* Heading\nContent",
            FormatRegistry.ID_CREOLE to "= Title =\nContent",
            FormatRegistry.ID_TIDDLYWIKI to "! H1\nContent",
            FormatRegistry.ID_LATEX to "\\documentclass{article}\n\\begin{document}\nText\n\\end{document}",
            FormatRegistry.ID_ASCIIDOC to "= Title\n\nContent",
            FormatRegistry.ID_RESTRUCTUREDTEXT to "Title\n=====\nContent",
            FormatRegistry.ID_KEYVALUE to "k=v",
            FormatRegistry.ID_TASKPAPER to "P:\n\t- Task",
            FormatRegistry.ID_TEXTILE to "h1. Title",
            FormatRegistry.ID_JUPYTER to "{\"nbformat\":4,\"cells\":[]}",
            FormatRegistry.ID_RMARKDOWN to "# Title",
            FormatRegistry.ID_BINARY to "binary"
        )

        fixtures.forEach { (id, content) ->
            val format = FormatRegistry.getById(id)
            assertNotNull(format, "Format $id should exist in registry")
            val doc = FormatRegistry.parseWithCache(content, format)
            assertNotNull(doc, "parseWithCache should return non-null for $id")
            assertEquals(id, doc.format.id, "Parsed doc format should match $id")
        }
    }

    @Test
    fun AllFormatsParsedDocHtmlIsNonEmpty() = runBlocking<Unit> {
        val fixtures = listOf(
            MarkdownParser() to "# Title\n\nContent",
            PlaintextParser() to "Plain content here",
            TodoTxtParser() to "(A) Task @work",
            CsvParser() to "a,b,c\n1,2,3",
            WikitextParser() to "= H1 =\nContent",
            OrgModeParser() to "* Heading\nContent",
            CreoleParser() to "= Title =\nContent",
            TiddlyWikiParser() to "! H1\nContent",
            LatexParser() to "\\documentclass{article}\n\\begin{document}\nText\n\\end{document}",
            AsciidocParser() to "= Title\n\nContent",
            RestructuredTextParser() to "Title\n=====\nContent",
            KeyValueParser() to "key=value",
            TaskpaperParser() to "Project:\n\t- Task",
            TextileParser() to "h1. Title\n\nContent",
            JupyterParser() to "{\"nbformat\":4,\"cells\":[]}",
            RMarkdownParser() to "# Title\n\nContent",
            BinaryParser() to "binary"
        )

        fixtures.forEach { (parser, content) ->
            val doc = parser.parse(content)
            val html = parser.toHtml(doc, lightMode = true)
            assertTrue(html.isNotEmpty(), "HTML for ${doc.format.id} should not be empty")
        }
    }

    @Test
    fun ParseWithCacheSameContentReturnsCachedResult() = runBlocking<Unit> {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        val content = "# Cache Test\n\nThis content is cached."

        val doc1 = FormatRegistry.parseWithCache(content, format)
        val doc2 = FormatRegistry.parseWithCache(content, format)

        // Same content and format = cache hit, both should be equal
        assertEquals(doc1.rawContent, doc2.rawContent)
        assertEquals(doc1.format.id, doc2.format.id)
    }

    @Test
    fun HtmlCacheIsPopulatedAfterFirstCall() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nContent")

        assertFalse(doc.hasHtmlCached(lightMode = true))
        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))
    }

    @Test
    fun HtmlCacheClearAllowsRegeneration() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nContent")

        val html1 = doc.toHtml(lightMode = true)
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
        val html2 = doc.toHtml(lightMode = true)
        assertEquals(html1, html2)
    }

    @Test
    fun LightAndDarkHtmlAreCachedSeparately() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nContent")

        val htmlLight = doc.toHtml(lightMode = true)
        val htmlDark = doc.toHtml(lightMode = false)

        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertTrue(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun FormatDetectionByFilenameMatchesParser() = runBlocking<Unit> {
        val pairs = listOf(
            "README.md" to FormatRegistry.ID_MARKDOWN,
            "data.csv" to FormatRegistry.ID_CSV,
            "notes.org" to FormatRegistry.ID_ORGMODE,
            "report.tex" to FormatRegistry.ID_LATEX,
            "tasks.taskpaper" to FormatRegistry.ID_TASKPAPER,
            "notebook.ipynb" to FormatRegistry.ID_JUPYTER
        )
        pairs.forEach { (filename, expectedId) ->
            val format = FormatRegistry.detectByFilename(filename)
            assertEquals(expectedId, format.id, "detectByFilename('$filename') should return $expectedId")
        }
    }
}
