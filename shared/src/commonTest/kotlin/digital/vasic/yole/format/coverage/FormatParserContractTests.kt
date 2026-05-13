/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Contract Tests
 *
 * Validates the TextParser interface contract for ALL
 * 18 format parsers: correct format ID, empty/null
 * input safety, valid metadata, toHtml idempotency,
 * and error-free valid input parsing.
 *
 *########################################################*/
package digital.vasic.yole.format.coverage

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
import kotlin.test.*

/**
 * Interface contract tests for all 18 format parsers.
 *
 * Contracts verified:
 * 1. Each parser produces ParsedDocument with correct formatId
 * 2. Each parser handles empty input without throwing
 * 3. Each ParsedDocument has valid metadata (non-null, map type)
 * 4. toHtml() is idempotent (call twice, same result)
 * 5. ParsedDocument.errors is empty for valid input
 *
 * ~85 tests (5 per format x 18 formats)
 */
class FormatParserContractTests {

    private data class ParserEntry(
        val name: String,
        val parser: TextParser,
        val expectedFormatId: String,
        val validContent: String
    )

    private val parsers: List<ParserEntry> = listOf(
        ParserEntry("Markdown", MarkdownParser(), TextFormat.ID_MARKDOWN, "# Hello\n\nParagraph"),
        ParserEntry("Plaintext", PlaintextParser(), TextFormat.ID_PLAINTEXT, "Hello world"),
        ParserEntry("TodoTxt", TodoTxtParser(), TextFormat.ID_TODOTXT, "(A) Buy milk +grocery @store"),
        ParserEntry("CSV", CsvParser(), TextFormat.ID_CSV, "name,age\nJohn,30\nJane,25"),
        ParserEntry("Wikitext", WikitextParser(), TextFormat.ID_WIKITEXT, "== Heading ==\nContent"),
        ParserEntry("Creole", CreoleParser(), TextFormat.ID_CREOLE, "= Heading\nContent"),
        ParserEntry("TiddlyWiki", TiddlyWikiParser(), TextFormat.ID_TIDDLYWIKI, "! Heading\nContent"),
        ParserEntry("LaTeX", LatexParser(), TextFormat.ID_LATEX, "\\section{Title}\nContent"),
        ParserEntry("AsciiDoc", AsciidocParser(), TextFormat.ID_ASCIIDOC, "= Title\n\nParagraph"),
        ParserEntry("OrgMode", OrgModeParser(), TextFormat.ID_ORGMODE, "* Heading\nContent"),
        ParserEntry("RST", RestructuredTextParser(), TextFormat.ID_RESTRUCTUREDTEXT, "Title\n=====\nContent"),
        ParserEntry("KeyValue", KeyValueParser(), TextFormat.ID_KEYVALUE, "key1=value1\nkey2=value2"),
        ParserEntry("TaskPaper", TaskpaperParser(), TextFormat.ID_TASKPAPER, "Project:\n\t- Task @tag"),
        ParserEntry("Textile", TextileParser(), TextFormat.ID_TEXTILE, "h1. Title\n\np. Paragraph"),
        ParserEntry("Jupyter", JupyterParser(), TextFormat.ID_JUPYTER, "{\"nbformat\":4,\"cells\":[]}"),
        ParserEntry("RMarkdown", RMarkdownParser(), TextFormat.ID_RMARKDOWN, "# Title\n```{r}\n1+1\n```"),
        ParserEntry("Binary", BinaryParser(), TextFormat.ID_BINARY, "\u0001\u0002\u0003\u0004")
    )

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
    // 1. Correct format ID in ParsedDocument
    // ====================================================================

    @Test
    fun testFormatIdMarkdown() = verifyFormatId(parsers[0])

    @Test
    fun testFormatIdPlaintext() = verifyFormatId(parsers[1])

    @Test
    fun testFormatIdTodoTxt() = verifyFormatId(parsers[2])

    @Test
    fun testFormatIdCsv() = verifyFormatId(parsers[3])

    @Test
    fun testFormatIdWikitext() = verifyFormatId(parsers[4])

    @Test
    fun testFormatIdCreole() = verifyFormatId(parsers[5])

    @Test
    fun testFormatIdTiddlyWiki() = verifyFormatId(parsers[6])

    @Test
    fun testFormatIdLatex() = verifyFormatId(parsers[7])

    @Test
    fun testFormatIdAsciiDoc() = verifyFormatId(parsers[8])

    @Test
    fun testFormatIdOrgMode() = verifyFormatId(parsers[9])

    @Test
    fun testFormatIdRst() = verifyFormatId(parsers[10])

    @Test
    fun testFormatIdKeyValue() = verifyFormatId(parsers[11])

    @Test
    fun testFormatIdTaskPaper() = verifyFormatId(parsers[12])

    @Test
    fun testFormatIdTextile() = verifyFormatId(parsers[13])

    @Test
    fun testFormatIdJupyter() = verifyFormatId(parsers[14])

    @Test
    fun testFormatIdRMarkdown() = verifyFormatId(parsers[15])

    @Test
    fun testFormatIdBinary() = verifyFormatId(parsers[16])

    // ====================================================================
    // 2. Empty input handled without throwing
    // ====================================================================

    @Test
    fun testEmptyInputSafeMarkdown() = verifyEmptyInputSafe(parsers[0])

    @Test
    fun testEmptyInputSafePlaintext() = verifyEmptyInputSafe(parsers[1])

    @Test
    fun testEmptyInputSafeTodoTxt() = verifyEmptyInputSafe(parsers[2])

    @Test
    fun testEmptyInputSafeCsv() = verifyEmptyInputSafe(parsers[3])

    @Test
    fun testEmptyInputSafeWikitext() = verifyEmptyInputSafe(parsers[4])

    @Test
    fun testEmptyInputSafeCreole() = verifyEmptyInputSafe(parsers[5])

    @Test
    fun testEmptyInputSafeTiddlyWiki() = verifyEmptyInputSafe(parsers[6])

    @Test
    fun testEmptyInputSafeLatex() = verifyEmptyInputSafe(parsers[7])

    @Test
    fun testEmptyInputSafeAsciiDoc() = verifyEmptyInputSafe(parsers[8])

    @Test
    fun testEmptyInputSafeOrgMode() = verifyEmptyInputSafe(parsers[9])

    @Test
    fun testEmptyInputSafeRst() = verifyEmptyInputSafe(parsers[10])

    @Test
    fun testEmptyInputSafeKeyValue() = verifyEmptyInputSafe(parsers[11])

    @Test
    fun testEmptyInputSafeTaskPaper() = verifyEmptyInputSafe(parsers[12])

    @Test
    fun testEmptyInputSafeTextile() = verifyEmptyInputSafe(parsers[13])

    @Test
    fun testEmptyInputSafeJupyter() = verifyEmptyInputSafe(parsers[14])

    @Test
    fun testEmptyInputSafeRMarkdown() = verifyEmptyInputSafe(parsers[15])

    @Test
    fun testEmptyInputSafeBinary() = verifyEmptyInputSafe(parsers[16])

    // ====================================================================
    // 3. Valid metadata in ParsedDocument
    // ====================================================================

    @Test
    fun testValidMetadataMarkdown() = verifyValidMetadata(parsers[0])

    @Test
    fun testValidMetadataPlaintext() = verifyValidMetadata(parsers[1])

    @Test
    fun testValidMetadataTodoTxt() = verifyValidMetadata(parsers[2])

    @Test
    fun testValidMetadataCsv() = verifyValidMetadata(parsers[3])

    @Test
    fun testValidMetadataWikitext() = verifyValidMetadata(parsers[4])

    @Test
    fun testValidMetadataCreole() = verifyValidMetadata(parsers[5])

    @Test
    fun testValidMetadataTiddlyWiki() = verifyValidMetadata(parsers[6])

    @Test
    fun testValidMetadataLatex() = verifyValidMetadata(parsers[7])

    @Test
    fun testValidMetadataAsciiDoc() = verifyValidMetadata(parsers[8])

    @Test
    fun testValidMetadataOrgMode() = verifyValidMetadata(parsers[9])

    @Test
    fun testValidMetadataRst() = verifyValidMetadata(parsers[10])

    @Test
    fun testValidMetadataKeyValue() = verifyValidMetadata(parsers[11])

    @Test
    fun testValidMetadataTaskPaper() = verifyValidMetadata(parsers[12])

    @Test
    fun testValidMetadataTextile() = verifyValidMetadata(parsers[13])

    @Test
    fun testValidMetadataJupyter() = verifyValidMetadata(parsers[14])

    @Test
    fun testValidMetadataRMarkdown() = verifyValidMetadata(parsers[15])

    @Test
    fun testValidMetadataBinary() = verifyValidMetadata(parsers[16])

    // ====================================================================
    // 4. toHtml() is idempotent (call twice, same result)
    // ====================================================================

    @Test
    fun testHtmlIdempotentMarkdown() = verifyHtmlIdempotent(parsers[0])

    @Test
    fun testHtmlIdempotentPlaintext() = verifyHtmlIdempotent(parsers[1])

    @Test
    fun testHtmlIdempotentTodoTxt() = verifyHtmlIdempotent(parsers[2])

    @Test
    fun testHtmlIdempotentCsv() = verifyHtmlIdempotent(parsers[3])

    @Test
    fun testHtmlIdempotentWikitext() = verifyHtmlIdempotent(parsers[4])

    @Test
    fun testHtmlIdempotentCreole() = verifyHtmlIdempotent(parsers[5])

    @Test
    fun testHtmlIdempotentTiddlyWiki() = verifyHtmlIdempotent(parsers[6])

    @Test
    fun testHtmlIdempotentLatex() = verifyHtmlIdempotent(parsers[7])

    @Test
    fun testHtmlIdempotentAsciiDoc() = verifyHtmlIdempotent(parsers[8])

    @Test
    fun testHtmlIdempotentOrgMode() = verifyHtmlIdempotent(parsers[9])

    @Test
    fun testHtmlIdempotentRst() = verifyHtmlIdempotent(parsers[10])

    @Test
    fun testHtmlIdempotentKeyValue() = verifyHtmlIdempotent(parsers[11])

    @Test
    fun testHtmlIdempotentTaskPaper() = verifyHtmlIdempotent(parsers[12])

    @Test
    fun testHtmlIdempotentTextile() = verifyHtmlIdempotent(parsers[13])

    @Test
    fun testHtmlIdempotentJupyter() = verifyHtmlIdempotent(parsers[14])

    @Test
    fun testHtmlIdempotentRMarkdown() = verifyHtmlIdempotent(parsers[15])

    @Test
    fun testHtmlIdempotentBinary() = verifyHtmlIdempotent(parsers[16])

    // ====================================================================
    // 5. Errors empty for valid input
    // ====================================================================

    @Test
    fun testNoErrorsMarkdown() = verifyNoErrors(parsers[0])

    @Test
    fun testNoErrorsPlaintext() = verifyNoErrors(parsers[1])

    @Test
    fun testNoErrorsTodoTxt() = verifyNoErrors(parsers[2])

    @Test
    fun testNoErrorsCsv() = verifyNoErrors(parsers[3])

    @Test
    fun testNoErrorsWikitext() = verifyNoErrors(parsers[4])

    @Test
    fun testNoErrorsCreole() = verifyNoErrors(parsers[5])

    @Test
    fun testNoErrorsTiddlyWiki() = verifyNoErrors(parsers[6])

    @Test
    fun testNoErrorsLatex() = verifyNoErrors(parsers[7])

    @Test
    fun testNoErrorsAsciiDoc() = verifyNoErrors(parsers[8])

    @Test
    fun testNoErrorsOrgMode() = verifyNoErrors(parsers[9])

    @Test
    fun testNoErrorsRst() = verifyNoErrors(parsers[10])

    @Test
    fun testNoErrorsKeyValue() = verifyNoErrors(parsers[11])

    @Test
    fun testNoErrorsTaskPaper() = verifyNoErrors(parsers[12])

    @Test
    fun testNoErrorsTextile() = verifyNoErrors(parsers[13])

    @Test
    fun testNoErrorsJupyter() = verifyNoErrors(parsers[14])

    @Test
    fun testNoErrorsRMarkdown() = verifyNoErrors(parsers[15])

    @Test
    fun testNoErrorsBinary() = verifyNoErrors(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    private fun verifyFormatId(entry: ParserEntry) {
        val doc = entry.parser.parse(entry.validContent)
        assertEquals(
            entry.expectedFormatId, doc.format.id,
            "${entry.name}: ParsedDocument.format.id must match expected format ID"
        )
        // Also verify canParse returns true for its own format
        assertTrue(
            entry.parser.canParse(doc.format),
            "${entry.name}: parser.canParse(own format) must return true"
        )
    }

    private fun verifyEmptyInputSafe(entry: ParserEntry) {
        // Must not throw for empty string
        val doc = entry.parser.parse("")
        assertNotNull(doc, "${entry.name}: parse('') must not return null")
        // toHtml must also work on empty parsed document
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html, "${entry.name}: toHtml on empty doc must not return null")
    }

    private fun verifyValidMetadata(entry: ParserEntry) {
        val doc = entry.parser.parse(entry.validContent)
        assertNotNull(doc.metadata, "${entry.name}: metadata must not be null")
        // Metadata is a Map<String, String> - verify all entries are valid
        for ((key, value) in doc.metadata) {
            assertTrue(key.isNotEmpty(), "${entry.name}: metadata key must not be empty")
            assertNotNull(value, "${entry.name}: metadata value for '$key' must not be null")
        }
    }

    private fun verifyHtmlIdempotent(entry: ParserEntry) {
        val doc = entry.parser.parse(entry.validContent)
        val html1 = doc.toHtml(lightMode = true)
        val html2 = doc.toHtml(lightMode = true)
        assertEquals(
            html1, html2,
            "${entry.name}: toHtml(light) must return identical results on consecutive calls"
        )

        val htmlDark1 = doc.toHtml(lightMode = false)
        val htmlDark2 = doc.toHtml(lightMode = false)
        assertEquals(
            htmlDark1, htmlDark2,
            "${entry.name}: toHtml(dark) must return identical results on consecutive calls"
        )
    }

    private fun verifyNoErrors(entry: ParserEntry) {
        val doc = entry.parser.parse(entry.validContent)
        assertTrue(
            doc.errors.isEmpty(),
            "${entry.name}: errors should be empty for valid input, got: ${doc.errors}"
        )
    }
}
