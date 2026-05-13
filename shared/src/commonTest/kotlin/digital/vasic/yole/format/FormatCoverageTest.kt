/*#######################################################
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Coverage-focused tests for format system uncovered branches
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import kotlin.test.*

class FormatCoverageTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ===== ParsedDocument edge cases =====

    @Test
    fun testParsedDocumentToStringTruncation() {
        val parser = MarkdownParser()
        val longContent = "a".repeat(200)
        val doc = parser.parse(longContent)
        val str = doc.toString()
        assertTrue(str.contains("..."))
    }

    @Test
    fun testParsedDocumentEqualsSameObject() {
        val parser = MarkdownParser()
        val doc = parser.parse("# test")
        assertTrue(doc.equals(doc))
    }

    @Test
    fun testParsedDocumentEqualsNull() {
        val parser = MarkdownParser()
        val doc = parser.parse("# test")
        assertFalse(doc.equals(null))
    }

    @Test
    fun testParsedDocumentEqualsDifferentClass() {
        val parser = MarkdownParser()
        val doc = parser.parse("# test")
        assertFalse(doc.equals("not a ParsedDocument"))
    }

    @Test
    fun testParsedDocumentCopy() {
        val parser = MarkdownParser()
        val doc = parser.parse("# test")
        val copy = doc.copy(rawContent = "# modified")
        assertEquals("# modified", copy.rawContent)
        assertEquals(doc.format, copy.format)
    }

    @Test
    fun testParsedDocumentCopyDoesNotCopyHtmlCache() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Hello")
        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))

        val copy = doc.copy()
        assertFalse(copy.hasHtmlCached(lightMode = true))
    }

    @Test
    fun testParsedDocumentClearHtmlCache() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Hello")
        doc.toHtml(lightMode = true)
        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(true))
        assertTrue(doc.hasHtmlCached(false))

        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false))
    }

    @Test
    fun testParsedDocumentToHtmlDarkMode() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Hello")
        val darkHtml = doc.toHtml(lightMode = false)
        assertTrue(darkHtml.isNotEmpty())
        assertTrue(doc.hasHtmlCached(false))
    }

    @Test
    fun testParsedDocumentToHtmlCaching() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Hello")
        val html1 = doc.toHtml(lightMode = true)
        val html2 = doc.toHtml(lightMode = true)
        assertSame(html1, html2) // Same reference = cached
    }

    @Test
    fun testParsedDocumentHashCode() {
        val parser = MarkdownParser()
        val doc1 = parser.parse("# Hello")
        val doc2 = parser.parse("# Hello")
        assertEquals(doc1.hashCode(), doc2.hashCode())
    }

    @Test
    fun testParsedDocumentInequality() {
        val parser = MarkdownParser()
        val doc1 = parser.parse("# Hello")
        val doc2 = parser.parse("# World")
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testParsedDocumentDifferentMetadata() {
        val format = FormatRegistry.detectByFilename("test.md")
        val doc1 = ParsedDocument(format, "a", "b", mapOf("k" to "v1"))
        val doc2 = ParsedDocument(format, "a", "b", mapOf("k" to "v2"))
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testParsedDocumentDifferentErrors() {
        val format = FormatRegistry.detectByFilename("test.md")
        val doc1 = ParsedDocument(format, "a", "b", errors = listOf("err1"))
        val doc2 = ParsedDocument(format, "a", "b", errors = listOf("err2"))
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testParsedDocumentDifferentFormat() {
        val md = FormatRegistry.detectByFilename("test.md")
        val txt = FormatRegistry.detectByFilename("test.txt")
        val doc1 = ParsedDocument(md, "a", "b")
        val doc2 = ParsedDocument(txt, "a", "b")
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testParsedDocumentDifferentParsedContent() {
        val format = FormatRegistry.detectByFilename("test.md")
        val doc1 = ParsedDocument(format, "a", "parsed1")
        val doc2 = ParsedDocument(format, "a", "parsed2")
        assertNotEquals(doc1, doc2)
    }

    // ===== Markdown parser edge cases =====

    @Test
    fun testMarkdownInlineLink() {
        val parser = MarkdownParser()
        val doc = parser.parse("Click [here](https://example.com) for more.")
        assertTrue(doc.parsedContent.contains("here"))
    }

    @Test
    fun testMarkdownImageLink() {
        val parser = MarkdownParser()
        val doc = parser.parse("![alt text](image.png)")
        assertTrue(doc.parsedContent.contains("image.png") || doc.parsedContent.contains("alt text"))
    }

    @Test
    fun testMarkdownBoldItalic() {
        val parser = MarkdownParser()
        val doc = parser.parse("This is ***bold italic*** text")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownOrderedListEnd() {
        val parser = MarkdownParser()
        val doc = parser.parse("1. item1\n2. item2\n\nParagraph after list")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownNestedLinkInBold() {
        val parser = MarkdownParser()
        val doc = parser.parse("**[bold link](url)**")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownBrokenLink() {
        val parser = MarkdownParser()
        val doc = parser.parse("[broken link(missing bracket")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownLinkMissingClosingParen() {
        val parser = MarkdownParser()
        val doc = parser.parse("[text](url-no-close")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownEmptyLink() {
        val parser = MarkdownParser()
        val doc = parser.parse("[]()")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownBoldUnclosed() {
        val parser = MarkdownParser()
        val doc = parser.parse("**unclosed bold")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownItalicUnclosed() {
        val parser = MarkdownParser()
        val doc = parser.parse("*unclosed italic")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownStrikethroughUnclosed() {
        val parser = MarkdownParser()
        val doc = parser.parse("~~unclosed strikethrough")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownOrderedListFollowedByHeading() {
        val parser = MarkdownParser()
        val doc = parser.parse("1. first\n2. second\n# Heading")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownMultipleLinksSameLine() {
        val parser = MarkdownParser()
        val doc = parser.parse("[link1](url1) and [link2](url2)")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownImageWithTitle() {
        val parser = MarkdownParser()
        val doc = parser.parse("![alt](image.png \"title\")")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Creole parser edge cases =====

    @Test
    fun testCreoleNestedFormatting() {
        val parser = CreoleParser()
        val doc = parser.parse("**bold //and italic//**")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testCreoleUnclosedBold() {
        val parser = CreoleParser()
        val doc = parser.parse("**unclosed bold text")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testCreoleTableWithEmptyCells() {
        val parser = CreoleParser()
        val doc = parser.parse("|cell1||cell3|\n|a|b|c|")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Jupyter parser edge cases =====

    @Test
    fun testJupyterEmptyNotebook() {
        val parser = JupyterParser()
        val doc = parser.parse("{}")
        assertTrue(doc.parsedContent.isNotEmpty() || doc.errors.isNotEmpty())
    }

    @Test
    fun testJupyterInvalidJson() {
        val parser = JupyterParser()
        val doc = parser.parse("not json at all")
        assertTrue(doc.parsedContent.isNotEmpty() || doc.errors.isNotEmpty())
    }

    @Test
    fun testJupyterMinimalNotebook() {
        val parser = JupyterParser()
        val doc = parser.parse("""{"cells":[],"metadata":{},"nbformat":4,"nbformat_minor":5}""")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testJupyterCodeCell() {
        val parser = JupyterParser()
        val doc = parser.parse("""{"cells":[{"cell_type":"code","source":["print('hello')"],"metadata":{},"execution_count":1,"outputs":[]}],"metadata":{},"nbformat":4,"nbformat_minor":5}""")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testJupyterMarkdownCell() {
        val parser = JupyterParser()
        val doc = parser.parse("""{"cells":[{"cell_type":"markdown","source":["# Title"],"metadata":{}}],"metadata":{},"nbformat":4,"nbformat_minor":5}""")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testJupyterRawCell() {
        val parser = JupyterParser()
        val doc = parser.parse("""{"cells":[{"cell_type":"raw","source":["raw text"],"metadata":{}}],"metadata":{},"nbformat":4,"nbformat_minor":5}""")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Binary parser edge cases =====

    @Test
    fun testBinaryParserNonBinaryContent() {
        val parser = BinaryParser()
        val doc = parser.parse("This is plain text, not binary")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testBinaryParserBinarySignatures() {
        val parser = BinaryParser()
        // PDF magic bytes
        val doc = parser.parse("%PDF-1.4 binary content here")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testBinaryParserEmptyInput() {
        val parser = BinaryParser()
        val doc = parser.parse("")
        assertTrue(doc.parsedContent.isNotEmpty() || doc.rawContent.isEmpty())
    }

    // ===== OrgMode parser edge cases =====

    @Test
    fun testOrgModePropertyDrawer() {
        val parser = OrgModeParser()
        val doc = parser.parse(":PROPERTIES:\n:CUSTOM_ID: my-heading\n:END:\n* Heading")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testOrgModeCheckboxList() {
        val parser = OrgModeParser()
        val doc = parser.parse("- [ ] unchecked\n- [X] checked\n- [-] partial")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== TiddlyWiki parser edge cases =====

    @Test
    fun testTiddlyWikiDefinitionList() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(";Term\n:Definition")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testTiddlyWikiHorizontalRule() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse("Before\n---\nAfter")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Asciidoc parser edge cases =====

    @Test
    fun testAsciidocAdmonition() {
        val parser = AsciidocParser()
        val doc = parser.parse("NOTE: This is a note")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testAsciidocPassthroughBlock() {
        val parser = AsciidocParser()
        val doc = parser.parse("++++\n<p>raw html</p>\n++++")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Latex parser edge cases =====

    @Test
    fun testLatexNestedEnvironments() {
        val parser = LatexParser()
        val doc = parser.parse("\\begin{document}\n\\begin{enumerate}\n\\item First\n\\end{enumerate}\n\\end{document}")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testLatexMathMode() {
        val parser = LatexParser()
        val doc = parser.parse("Inline math: \$x^2\$ and display: \\[y = mx + b\\]")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== KeyValue parser edge cases =====

    @Test
    fun testKeyValueSectionHeaders() {
        val parser = KeyValueParser()
        val doc = parser.parse("[section]\nkey=value\n\n[another]\nk2=v2")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testKeyValueMultilineValues() {
        val parser = KeyValueParser()
        val doc = parser.parse("key=value\\\ncontinued on next line")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Plaintext parser edge cases =====

    @Test
    fun testPlaintextVeryLongLine() {
        val parser = PlaintextParser()
        val longLine = "x".repeat(10000)
        val doc = parser.parse(longLine)
        assertEquals(longLine, doc.rawContent)
    }

    @Test
    fun testPlaintextOnlyWhitespace() {
        val parser = PlaintextParser()
        val doc = parser.parse("   \n\t\n   ")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Textile parser edge cases =====

    @Test
    fun testTextileFootnotes() {
        val parser = TextileParser()
        val doc = parser.parse("Text with footnote[1].\n\nfn1. This is the footnote.")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testTextileDefinitionList() {
        val parser = TextileParser()
        val doc = parser.parse("- Term := Definition")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Wikitext parser edge cases =====

    @Test
    fun testWikitextTemplate() {
        val parser = WikitextParser()
        val doc = parser.parse("{{template|param=value}}")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testWikitextCategory() {
        val parser = WikitextParser()
        val doc = parser.parse("[[Category:Test]]")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== RMarkdown parser edge cases =====

    @Test
    fun testRMarkdownCodeChunkWithOptions() {
        val parser = RMarkdownParser()
        val doc = parser.parse("```{r setup, include=FALSE}\nlibrary(ggplot2)\n```")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testRMarkdownInlineR() {
        val parser = RMarkdownParser()
        val doc = parser.parse("The value is `r mean(x)`.")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Taskpaper parser edge cases =====

    @Test
    fun testTaskpaperTagsInTask() {
        val parser = TaskpaperParser()
        val doc = parser.parse("- Task @due(2024-01-01) @priority(high)")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testTaskpaperProjectWithNotes() {
        val parser = TaskpaperParser()
        val doc = parser.parse("Project:\n\tNote about project\n\t- Task 1")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== Csv parser edge cases =====

    @Test
    fun testCsvQuotedFieldsWithCommas() {
        val parser = CsvParser()
        val doc = parser.parse("\"Name, First\",Age\nDoe,30")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testCsvEmptyFields() {
        val parser = CsvParser()
        val doc = parser.parse("a,,c\n1,,3")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== RestructuredText parser edge cases =====

    @Test
    fun testRstDirective() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(".. note::\n\n   This is a note.")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testRstFieldList() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(":Author: John Doe\n:Version: 1.0")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== TodoTxt parser edge cases =====

    @Test
    fun testTodoTxtWithAllComponents() {
        val parser = TodoTxtParser()
        val doc = parser.parse("(A) 2024-01-01 Task +project @context due:2024-02-01")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testTodoTxtCompletedTask() {
        val parser = TodoTxtParser()
        val doc = parser.parse("x 2024-01-15 2024-01-01 Completed task")
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    // ===== ParseOptions / Format detection edge cases =====

    @Test
    fun testParseOptionsDefault() {
        val parser = MarkdownParser()
        val doc = parser.parse("test", emptyMap())
        assertNotNull(doc)
    }

    @Test
    fun testParseOptionsWithFilename() {
        val parser = MarkdownParser()
        val doc = parser.parse("test", mapOf("filename" to "test.md"))
        assertNotNull(doc)
    }

    @Test
    fun testFormatRegistryDetectByContentMarkdown() {
        val format = FormatRegistry.detectByContent("# Heading\n\nSome text with **bold**")
        assertNotNull(format)
    }

    @Test
    fun testFormatRegistryDetectByContentLatex() {
        val format = FormatRegistry.detectByContent("\\documentclass{article}")
        assertNotNull(format)
    }

    @Test
    fun testFormatRegistryDetectByContentUnknown() {
        val format = FormatRegistry.detectByContent("just plain text nothing special")
        // May return plaintext or null
        if (format != null) {
            assertTrue(format.id.isNotEmpty())
        }
    }

    @Test
    fun testFormatRegistryAllFormatsHaveIds() {
        val formats = FormatRegistry.formats
        for (f in formats) {
            assertTrue(f.id.isNotEmpty(), "Format ${f.name} has empty id")
        }
    }

    @Test
    fun testFormatRegistryAllFormatsHaveNames() {
        val formats = FormatRegistry.formats
        for (f in formats) {
            assertTrue(f.name.isNotEmpty(), "Format with id ${f.id} has empty name")
        }
    }

    @Test
    fun testTextParserCanParse() {
        val parser = MarkdownParser()
        assertTrue(parser.canParse(parser.supportedFormat))
    }

    @Test
    fun testTextParserCannotParseOther() {
        val mdParser = MarkdownParser()
        val csvParser = CsvParser()
        assertFalse(mdParser.canParse(csvParser.supportedFormat))
    }

    // ===== ParserRegistry edge cases =====

    @Test
    fun testParserRegistryGetAllParsers() {
        val parsers = ParserRegistry.getAllParsers()
        assertTrue(parsers.isNotEmpty())
        assertTrue(parsers.size >= 18) // At least 18 formats
    }

    @Test
    fun testParserRegistryGetParserByFormat() {
        val format = FormatRegistry.detectByFilename("test.md")
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
    }

    @Test
    fun testParserRegistryGetParserForUnknownFormat() {
        val unknownFormat = TextFormat(
            id = "nonexistent_format_xyz",
            name = "Unknown",
            defaultExtension = ".xyz123",
            extensions = listOf(".xyz123")
        )
        val parser = ParserRegistry.getParser(unknownFormat)
        // Should return null for unknown format
        assertNull(parser)
    }

    // ===== Validate all parsers produce valid ParsedDocuments =====

    @Test
    fun testAllParsersProduceValidDocuments() {
        val testContent = "# Hello World\n\nSome content here."
        val parsers = ParserRegistry.getAllParsers()
        for (parser in parsers) {
            val doc = parser.parse(testContent)
            assertNotNull(doc, "Parser ${parser.supportedFormat.name} returned null")
            assertEquals(testContent, doc.rawContent)
            assertNotNull(doc.format)
            assertNotNull(doc.metadata)
            assertNotNull(doc.errors)
        }
    }

    @Test
    fun testAllParsersHandleEmptyContent() {
        val parsers = ParserRegistry.getAllParsers()
        for (parser in parsers) {
            val doc = parser.parse("")
            assertNotNull(doc, "Parser ${parser.supportedFormat.name} returned null for empty content")
        }
    }
}
