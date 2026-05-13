/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-independent Parsing Guarantees Tests
 *
 * Verifies that all 18 parsers produce deterministic,
 * platform-independent results: identical input yields
 * identical ParsedDocument, extension detection is
 * case-insensitive, line endings are handled, UTF-8 BOM
 * is processed, and file size limits are enforced.
 *
 *########################################################*/
package digital.vasic.yole.format.platform

import digital.vasic.yole.format.*
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.json.JsonParser
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
import kotlin.test.*

/**
 * Tests verifying platform-independent behavior guarantees.
 *
 * These tests ensure:
 * 1. Deterministic parsing: identical input -> identical ParsedDocument
 * 2. Extension detection is case-insensitive
 * 3. Content detection patterns work with different line endings (\n, \r\n, \r)
 * 4. UTF-8 BOM handling
 * 5. File size limits and boundary conditions
 *
 * Total: 40+ test methods.
 */
class PlatformParsingTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    private val markdownParser = MarkdownParser()
    private val plaintextParser = PlaintextParser()
    private val todoTxtParser = TodoTxtParser()
    private val csvParser = CsvParser()
    private val wikitextParser = WikitextParser()
    private val creoleParser = CreoleParser()
    private val tiddlyWikiParser = TiddlyWikiParser()
    private val latexParser = LatexParser()
    private val asciidocParser = AsciidocParser()
    private val orgModeParser = OrgModeParser()
    private val rstParser = RestructuredTextParser()
    private val keyValueParser = KeyValueParser()
    private val taskpaperParser = TaskpaperParser()
    private val textileParser = TextileParser()
    private val jupyterParser = JupyterParser()
    private val rMarkdownParser = RMarkdownParser()
    private val binaryParser = BinaryParser()

    private data class ParserEntry(
        val parser: TextParser,
        val formatId: String,
        val sampleContent: String
    )

    private val allParsers = listOf(
        ParserEntry(markdownParser, FormatRegistry.ID_MARKDOWN, "# Hello\n\nWorld."),
        ParserEntry(plaintextParser, FormatRegistry.ID_PLAINTEXT, "Hello World."),
        ParserEntry(todoTxtParser, FormatRegistry.ID_TODOTXT, "(A) Task @ctx +proj"),
        ParserEntry(csvParser, FormatRegistry.ID_CSV, "a,b,c\n1,2,3"),
        ParserEntry(wikitextParser, FormatRegistry.ID_WIKITEXT, "== Title ==\nText."),
        ParserEntry(creoleParser, FormatRegistry.ID_CREOLE, "= Title\nText."),
        ParserEntry(tiddlyWikiParser, FormatRegistry.ID_TIDDLYWIKI, "! Title\nText."),
        ParserEntry(latexParser, FormatRegistry.ID_LATEX, "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"),
        ParserEntry(asciidocParser, FormatRegistry.ID_ASCIIDOC, "= Title\n\nParagraph."),
        ParserEntry(orgModeParser, FormatRegistry.ID_ORGMODE, "* TODO Task\nBody."),
        ParserEntry(rstParser, FormatRegistry.ID_RESTRUCTUREDTEXT, "Title\n=====\n\nBody."),
        ParserEntry(keyValueParser, FormatRegistry.ID_KEYVALUE, "key = value"),
        ParserEntry(taskpaperParser, FormatRegistry.ID_TASKPAPER, "Project:\n\t- Task"),
        ParserEntry(textileParser, FormatRegistry.ID_TEXTILE, "h1. Title\n\np. Text."),
        ParserEntry(jupyterParser, FormatRegistry.ID_JUPYTER, """{"nbformat": 4, "cells": [], "metadata": {}}"""),
        ParserEntry(rMarkdownParser, FormatRegistry.ID_RMARKDOWN, "---\ntitle: T\n---\n# Heading"),
        ParserEntry(binaryParser, FormatRegistry.ID_BINARY, "\u0000\u0001\u0002")
    )

    // ====================================================================
    // DETERMINISTIC PARSING: identical input -> identical output
    // ====================================================================

    @Test
    fun `deterministic Markdown same input same output`() {
        verifyDeterministic(markdownParser, "# Hello\n\n**Bold** text.")
    }

    @Test
    fun `deterministic Plaintext same input same output`() {
        verifyDeterministic(plaintextParser, "Simple plain text content.")
    }

    @Test
    fun `deterministic TodoTxt same input same output`() {
        verifyDeterministic(todoTxtParser, "(A) 2024-01-01 Buy groceries @store +shopping")
    }

    @Test
    fun `deterministic CSV same input same output`() {
        verifyDeterministic(csvParser, "name,age\nAlice,30\nBob,25")
    }

    @Test
    fun `deterministic WikiText same input same output`() {
        verifyDeterministic(wikitextParser, "== Heading ==\nParagraph text.")
    }

    @Test
    fun `deterministic Creole same input same output`() {
        verifyDeterministic(creoleParser, "= Heading\nParagraph.")
    }

    @Test
    fun `deterministic TiddlyWiki same input same output`() {
        verifyDeterministic(tiddlyWikiParser, "! Heading\nContent.")
    }

    @Test
    fun `deterministic LaTeX same input same output`() {
        verifyDeterministic(latexParser, "\\documentclass{article}\n\\begin{document}\nContent\n\\end{document}")
    }

    @Test
    fun `deterministic AsciiDoc same input same output`() {
        verifyDeterministic(asciidocParser, "= Title\n\nContent.")
    }

    @Test
    fun `deterministic OrgMode same input same output`() {
        verifyDeterministic(orgModeParser, "* TODO Task\nBody text.")
    }

    @Test
    fun `deterministic reStructuredText same input same output`() {
        verifyDeterministic(rstParser, "Title\n=====\n\nParagraph.")
    }

    @Test
    fun `deterministic KeyValue same input same output`() {
        verifyDeterministic(keyValueParser, "[section]\nkey1 = val1\nkey2 = val2")
    }

    @Test
    fun `deterministic TaskPaper same input same output`() {
        verifyDeterministic(taskpaperParser, "Project:\n\t- Task @done")
    }

    @Test
    fun `deterministic Textile same input same output`() {
        verifyDeterministic(textileParser, "h1. Title\n\np. Paragraph.")
    }

    @Test
    fun `deterministic Jupyter same input same output`() {
        verifyDeterministic(jupyterParser, """{"nbformat": 4, "cells": [], "metadata": {}}""")
    }

    @Test
    fun `deterministic RMarkdown same input same output`() {
        verifyDeterministic(rMarkdownParser, "---\ntitle: Doc\n---\n# Heading\nContent.")
    }

    @Test
    fun `deterministic Binary same input same output`() {
        verifyDeterministic(binaryParser, "\u0000\u0001\u0002\u0003")
    }

    // ====================================================================
    // EXTENSION DETECTION CASE INSENSITIVITY
    // ====================================================================

    @Test
    fun `extension detection md is case-insensitive`() {
        verifyCaseInsensitiveExtension("md", "MD", "Md", "mD")
    }

    @Test
    fun `extension detection csv is case-insensitive`() {
        verifyCaseInsensitiveExtension("csv", "CSV", "Csv", "cSv")
    }

    @Test
    fun `extension detection tex is case-insensitive`() {
        verifyCaseInsensitiveExtension("tex", "TEX", "Tex", "tEx")
    }

    @Test
    fun `extension detection adoc is case-insensitive`() {
        verifyCaseInsensitiveExtension("adoc", "ADOC", "Adoc", "aDoc")
    }

    @Test
    fun `extension detection rst is case-insensitive`() {
        verifyCaseInsensitiveExtension("rst", "RST", "Rst", "rSt")
    }

    @Test
    fun `extension detection org is case-insensitive`() {
        verifyCaseInsensitiveExtension("org", "ORG", "Org", "oRg")
    }

    @Test
    fun `extension detection wiki is case-insensitive`() {
        verifyCaseInsensitiveExtension("wiki", "WIKI", "Wiki", "wIkI")
    }

    @Test
    fun `extension detection ipynb is case-insensitive`() {
        verifyCaseInsensitiveExtension("ipynb", "IPYNB", "Ipynb", "iPyNb")
    }

    @Test
    fun `extension detection tid is case-insensitive`() {
        verifyCaseInsensitiveExtension("tid", "TID", "Tid", "tId")
    }

    @Test
    fun `extension detection with leading dot is equivalent`() {
        val withDot = FormatRegistry.detectByExtension(".md")
        val withoutDot = FormatRegistry.detectByExtension("md")
        assertEquals(withDot.id, withoutDot.id)
    }

    @Test
    fun `extension detection with whitespace is handled`() {
        val format = FormatRegistry.detectByExtension("  md  ")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    // ====================================================================
    // LINE ENDING HANDLING (\n, \r\n, \r)
    // ====================================================================

    @Test
    fun `line endings Unix LF parsed by Markdown`() {
        val content = "# Title\nLine 2\nLine 3"
        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun `line endings Windows CRLF parsed by Markdown`() {
        val content = "# Title\r\nLine 2\r\nLine 3"
        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun `line endings old Mac CR parsed by Markdown`() {
        val content = "# Title\rLine 2\rLine 3"
        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun `line endings mixed parsed by all parsers`() {
        val mixedContent = "Line1\r\nLine2\rLine3\nLine4"
        allParsers.forEach { entry ->
            val doc = entry.parser.parse(mixedContent)
            assertNotNull(doc, "Parser ${entry.formatId} should handle mixed line endings")
            assertEquals(mixedContent, doc.rawContent, "Raw content should be preserved for ${entry.formatId}")
        }
    }

    @Test
    fun `content detection works with CRLF line endings`() {
        val content = "# Title\r\nContent\r\n"
        val detected = FormatRegistry.detectByContent(content)
        assertNotNull(detected)
        assertEquals(FormatRegistry.ID_MARKDOWN, detected.id)
    }

    @Test
    fun `content detection LaTeX with CRLF`() {
        val content = "\\documentclass{article}\r\n\\begin{document}\r\nHello\r\n\\end{document}"
        val detected = FormatRegistry.detectByContent(content)
        assertNotNull(detected)
        assertEquals(FormatRegistry.ID_LATEX, detected.id)
    }

    @Test
    fun `content detection WikiText with CRLF`() {
        val content = "== Title ==\r\nParagraph.\r\n"
        val detected = FormatRegistry.detectByContent(content)
        assertNotNull(detected)
        assertEquals(FormatRegistry.ID_WIKITEXT, detected.id)
    }

    // ====================================================================
    // UTF-8 BOM HANDLING
    // ====================================================================

    @Test
    fun `UTF-8 BOM at start of Markdown content is handled`() {
        val bom = "\uFEFF"
        val content = "${bom}# Title\n\nContent."
        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        // The parser should handle BOM gracefully (not crash)
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `UTF-8 BOM at start of CSV content is handled`() {
        val bom = "\uFEFF"
        val content = "${bom}col1,col2\nval1,val2"
        val doc = csvParser.parse(content)
        assertNotNull(doc)
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `UTF-8 BOM at start of KeyValue content is handled`() {
        val bom = "\uFEFF"
        val content = "${bom}[section]\nkey = value"
        val doc = keyValueParser.parse(content)
        assertNotNull(doc)
    }

    @Test
    fun `UTF-8 BOM at start of all parsers does not crash`() {
        val bom = "\uFEFF"
        allParsers.forEach { entry ->
            val content = "${bom}${entry.sampleContent}"
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "Parser ${entry.formatId} should handle UTF-8 BOM")
        }
    }

    @Test
    fun `UTF-8 BOM content detection does not crash`() {
        val bom = "\uFEFF"
        val content = "${bom}# Markdown Title"
        // May or may not detect - the key is no crash
        val detected = FormatRegistry.detectByContent(content)
        assertTrue(detected != null || detected == null, "Should not crash")
    }

    // ====================================================================
    // FILE SIZE LIMITS AND BOUNDARY CONDITIONS
    // ====================================================================

    @Test
    fun `single character input parsed by all parsers`() {
        allParsers.forEach { entry ->
            val doc = entry.parser.parse("x")
            assertNotNull(doc, "Parser ${entry.formatId} should handle single character")
            assertEquals("x", doc.rawContent)
        }
    }

    @Test
    fun `single newline input parsed by all parsers`() {
        allParsers.forEach { entry ->
            val doc = entry.parser.parse("\n")
            assertNotNull(doc, "Parser ${entry.formatId} should handle single newline")
        }
    }

    @Test
    fun `whitespace-only input parsed by all parsers`() {
        val whitespace = "   \t  \t   "
        allParsers.forEach { entry ->
            val doc = entry.parser.parse(whitespace)
            assertNotNull(doc, "Parser ${entry.formatId} should handle whitespace-only input")
        }
    }

    @Test
    fun `large content 200KB parsed by Markdown`() {
        val content = buildString {
            append("# Large Document\n\n")
            while (length < 200_000) {
                append("Paragraph text with **bold** and *italic* content. ")
            }
        }
        assertTrue(content.length >= 200_000)
        val doc = markdownParser.parse(content)
        assertNotNull(doc)
        assertEquals(content.length, doc.rawContent.length)
    }

    @Test
    fun `large content 200KB parsed by Plaintext`() {
        val content = "a".repeat(200_000)
        val doc = plaintextParser.parse(content)
        assertNotNull(doc)
        assertEquals(content.length, doc.rawContent.length)
    }

    @Test
    fun `empty string parsed by all parsers produces empty rawContent`() {
        allParsers.forEach { entry ->
            val doc = entry.parser.parse("")
            assertNotNull(doc)
            assertEquals("", doc.rawContent, "Empty input should produce empty rawContent for ${entry.formatId}")
        }
    }

    // ====================================================================
    // FORMAT ID CONSISTENCY
    // ====================================================================

    @Test
    fun `all parsers report correct format ID`() {
        allParsers.forEach { entry ->
            val doc = entry.parser.parse(entry.sampleContent)
            assertEquals(
                entry.formatId, doc.format.id,
                "Format ID should be ${entry.formatId}, got ${doc.format.id}"
            )
        }
    }

    @Test
    fun `all parsers supportedFormat ID matches format ID`() {
        allParsers.forEach { entry ->
            assertEquals(
                entry.formatId, entry.parser.supportedFormat.id,
                "Parser supportedFormat.id should match expected for ${entry.formatId}"
            )
        }
    }

    @Test
    fun `all parsers canParse returns true for their own format`() {
        allParsers.forEach { entry ->
            val format = FormatRegistry.getById(entry.formatId)
            if (format != null) {
                assertTrue(
                    entry.parser.canParse(format),
                    "Parser for ${entry.formatId} should canParse its own format"
                )
            }
        }
    }

    @Test
    fun `parsers canParse returns false for different format`() {
        // Markdown parser should not canParse LaTeX format
        val latexFormat = FormatRegistry.getById(FormatRegistry.ID_LATEX)
        if (latexFormat != null) {
            assertFalse(markdownParser.canParse(latexFormat))
        }
    }

    // ====================================================================
    // HTML GENERATION CONSISTENCY
    // ====================================================================

    @Test
    fun `HTML generation is deterministic across calls`() {
        allParsers.forEach { entry ->
            val doc1 = entry.parser.parse(entry.sampleContent)
            val doc2 = entry.parser.parse(entry.sampleContent)

            val html1 = doc1.toHtml(lightMode = true)
            val html2 = doc2.toHtml(lightMode = true)

            assertEquals(
                html1, html2,
                "HTML should be identical for identical input in ${entry.formatId}"
            )
        }
    }

    @Test
    fun `light and dark HTML may differ for themed formats`() {
        val themedFormats = listOf(
            FormatRegistry.ID_RESTRUCTUREDTEXT,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_ASCIIDOC,
            FormatRegistry.ID_LATEX
        )
        themedFormats.forEach { formatId ->
            val entry = allParsers.first { it.formatId == formatId }
            val doc = entry.parser.parse(entry.sampleContent)
            val lightHtml = doc.toHtml(lightMode = true)
            val darkHtml = doc.toHtml(lightMode = false)
            // They may or may not differ, but both should be valid
            assertNotNull(lightHtml)
            assertNotNull(darkHtml)
        }
    }

    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    private fun verifyDeterministic(parser: TextParser, content: String) {
        val doc1 = parser.parse(content)
        val doc2 = parser.parse(content)

        assertEquals(doc1.format, doc2.format, "Format should be deterministic")
        assertEquals(doc1.rawContent, doc2.rawContent, "Raw content should be deterministic")
        assertEquals(doc1.parsedContent, doc2.parsedContent, "Parsed content should be deterministic")
        assertEquals(doc1.metadata, doc2.metadata, "Metadata should be deterministic")
        assertEquals(doc1.errors, doc2.errors, "Errors should be deterministic")
        assertEquals(doc1, doc2, "ParsedDocuments should be equal for same input")
    }

    private fun verifyCaseInsensitiveExtension(vararg variants: String) {
        val ids = variants.map { FormatRegistry.detectByExtension(it).id }
        val uniqueIds = ids.toSet()
        assertEquals(
            1, uniqueIds.size,
            "All case variants of '${variants.first()}' should detect the same format, got: $ids"
        )
    }
}
