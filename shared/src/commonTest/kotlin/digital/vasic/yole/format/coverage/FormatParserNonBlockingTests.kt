/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Non-Blocking Tests
 *
 * Validates that parsing and HTML generation for ALL
 * 17 format parsers complete within time bounds, handle
 * concurrent parsing, and that format detection does
 * not block.
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Non-blocking tests for all 17 format parsers.
 *
 * Scenarios:
 * 1. Parsing completes within 5 seconds
 * 2. 10 concurrent parses of same format complete
 * 3. toHtml() generation completes within 5 seconds
 * 4. Format detection does not block for more than 1 second
 *
 * ~68 tests (4 per format x 17 formats)
 */
class FormatParserNonBlockingTests {

    private data class ParserEntry(
        val name: String,
        val parser: TextParser,
        val content: String
    )

    private val parsers: List<ParserEntry> = listOf(
        ParserEntry("Markdown", MarkdownParser(), "# Title\n\n**Bold** and *italic*\n\n- Item 1\n- Item 2"),
        ParserEntry("Plaintext", PlaintextParser(), "Hello world\nLine 2\nLine 3"),
        ParserEntry("TodoTxt", TodoTxtParser(), "(A) Buy milk +grocery @store\n(B) Call dentist +health"),
        ParserEntry("CSV", CsvParser(), "name,age,city\nJohn,30,NYC\nJane,25,LA"),
        ParserEntry("Wikitext", WikitextParser(), "== Heading ==\n'''bold'''\n* bullet"),
        ParserEntry("Creole", CreoleParser(), "= Heading\n**bold**\n* bullet"),
        ParserEntry("TiddlyWiki", TiddlyWikiParser(), "! Heading\n''bold''\n* bullet"),
        ParserEntry("LaTeX", LatexParser(), "\\section{Title}\n\\textbf{bold}\n\\begin{itemize}\n\\item one\n\\end{itemize}"),
        ParserEntry("AsciiDoc", AsciidocParser(), "= Title\n\n*bold*\n\n* Item"),
        ParserEntry("OrgMode", OrgModeParser(), "* Heading\n** Subheading\n- list item"),
        ParserEntry("RST", RestructuredTextParser(), "Title\n=====\n\nParagraph"),
        ParserEntry("KeyValue", KeyValueParser(), "key1=value1\nkey2=value2\nkey3=value3"),
        ParserEntry("TaskPaper", TaskpaperParser(), "Project:\n\t- Task 1 @tag\n\t- Task 2 @done"),
        ParserEntry("Textile", TextileParser(), "h1. Title\n\np. Paragraph text"),
        ParserEntry("Jupyter", JupyterParser(), "{\"nbformat\":4,\"cells\":[]}"),
        ParserEntry("RMarkdown", RMarkdownParser(), "# Title\n```{r}\n1+1\n```\n## Section"),
        ParserEntry("Binary", BinaryParser(), "\u0001\u0002\u0003\u0004\u0005")
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
    // 1. Parsing completes within 5 seconds
    // ====================================================================

    @Test
    fun testParseTimeBoundMarkdown() = verifyParseBound(parsers[0])

    @Test
    fun testParseTimeBoundPlaintext() = verifyParseBound(parsers[1])

    @Test
    fun testParseTimeBoundTodoTxt() = verifyParseBound(parsers[2])

    @Test
    fun testParseTimeBoundCsv() = verifyParseBound(parsers[3])

    @Test
    fun testParseTimeBoundWikitext() = verifyParseBound(parsers[4])

    @Test
    fun testParseTimeBoundCreole() = verifyParseBound(parsers[5])

    @Test
    fun testParseTimeBoundTiddlyWiki() = verifyParseBound(parsers[6])

    @Test
    fun testParseTimeBoundLatex() = verifyParseBound(parsers[7])

    @Test
    fun testParseTimeBoundAsciiDoc() = verifyParseBound(parsers[8])

    @Test
    fun testParseTimeBoundOrgMode() = verifyParseBound(parsers[9])

    @Test
    fun testParseTimeBoundRst() = verifyParseBound(parsers[10])

    @Test
    fun testParseTimeBoundKeyValue() = verifyParseBound(parsers[11])

    @Test
    fun testParseTimeBoundTaskPaper() = verifyParseBound(parsers[12])

    @Test
    fun testParseTimeBoundTextile() = verifyParseBound(parsers[13])

    @Test
    fun testParseTimeBoundJupyter() = verifyParseBound(parsers[14])

    @Test
    fun testParseTimeBoundRMarkdown() = verifyParseBound(parsers[15])

    @Test
    fun testParseTimeBoundBinary() = verifyParseBound(parsers[16])

    // ====================================================================
    // 2. 10 concurrent parses complete
    // ====================================================================

    @Test
    fun testConcurrentParseMarkdown() = verifyConcurrentParse(parsers[0])

    @Test
    fun testConcurrentParsePlaintext() = verifyConcurrentParse(parsers[1])

    @Test
    fun testConcurrentParseTodoTxt() = verifyConcurrentParse(parsers[2])

    @Test
    fun testConcurrentParseCsv() = verifyConcurrentParse(parsers[3])

    @Test
    fun testConcurrentParseWikitext() = verifyConcurrentParse(parsers[4])

    @Test
    fun testConcurrentParseCreole() = verifyConcurrentParse(parsers[5])

    @Test
    fun testConcurrentParseTiddlyWiki() = verifyConcurrentParse(parsers[6])

    @Test
    fun testConcurrentParseLatex() = verifyConcurrentParse(parsers[7])

    @Test
    fun testConcurrentParseAsciiDoc() = verifyConcurrentParse(parsers[8])

    @Test
    fun testConcurrentParseOrgMode() = verifyConcurrentParse(parsers[9])

    @Test
    fun testConcurrentParseRst() = verifyConcurrentParse(parsers[10])

    @Test
    fun testConcurrentParseKeyValue() = verifyConcurrentParse(parsers[11])

    @Test
    fun testConcurrentParseTaskPaper() = verifyConcurrentParse(parsers[12])

    @Test
    fun testConcurrentParseTextile() = verifyConcurrentParse(parsers[13])

    @Test
    fun testConcurrentParseJupyter() = verifyConcurrentParse(parsers[14])

    @Test
    fun testConcurrentParseRMarkdown() = verifyConcurrentParse(parsers[15])

    @Test
    fun testConcurrentParseBinary() = verifyConcurrentParse(parsers[16])

    // ====================================================================
    // 3. toHtml() completes within 5 seconds
    // ====================================================================

    @Test
    fun testHtmlTimeBoundMarkdown() = verifyHtmlBound(parsers[0])

    @Test
    fun testHtmlTimeBoundPlaintext() = verifyHtmlBound(parsers[1])

    @Test
    fun testHtmlTimeBoundTodoTxt() = verifyHtmlBound(parsers[2])

    @Test
    fun testHtmlTimeBoundCsv() = verifyHtmlBound(parsers[3])

    @Test
    fun testHtmlTimeBoundWikitext() = verifyHtmlBound(parsers[4])

    @Test
    fun testHtmlTimeBoundCreole() = verifyHtmlBound(parsers[5])

    @Test
    fun testHtmlTimeBoundTiddlyWiki() = verifyHtmlBound(parsers[6])

    @Test
    fun testHtmlTimeBoundLatex() = verifyHtmlBound(parsers[7])

    @Test
    fun testHtmlTimeBoundAsciiDoc() = verifyHtmlBound(parsers[8])

    @Test
    fun testHtmlTimeBoundOrgMode() = verifyHtmlBound(parsers[9])

    @Test
    fun testHtmlTimeBoundRst() = verifyHtmlBound(parsers[10])

    @Test
    fun testHtmlTimeBoundKeyValue() = verifyHtmlBound(parsers[11])

    @Test
    fun testHtmlTimeBoundTaskPaper() = verifyHtmlBound(parsers[12])

    @Test
    fun testHtmlTimeBoundTextile() = verifyHtmlBound(parsers[13])

    @Test
    fun testHtmlTimeBoundJupyter() = verifyHtmlBound(parsers[14])

    @Test
    fun testHtmlTimeBoundRMarkdown() = verifyHtmlBound(parsers[15])

    @Test
    fun testHtmlTimeBoundBinary() = verifyHtmlBound(parsers[16])

    // ====================================================================
    // 4. Format detection does not block for more than 1 second
    // ====================================================================

    @Test
    fun testDetectionTimeBoundMarkdown() = verifyDetectionBound(parsers[0])

    @Test
    fun testDetectionTimeBoundPlaintext() = verifyDetectionBound(parsers[1])

    @Test
    fun testDetectionTimeBoundTodoTxt() = verifyDetectionBound(parsers[2])

    @Test
    fun testDetectionTimeBoundCsv() = verifyDetectionBound(parsers[3])

    @Test
    fun testDetectionTimeBoundWikitext() = verifyDetectionBound(parsers[4])

    @Test
    fun testDetectionTimeBoundCreole() = verifyDetectionBound(parsers[5])

    @Test
    fun testDetectionTimeBoundTiddlyWiki() = verifyDetectionBound(parsers[6])

    @Test
    fun testDetectionTimeBoundLatex() = verifyDetectionBound(parsers[7])

    @Test
    fun testDetectionTimeBoundAsciiDoc() = verifyDetectionBound(parsers[8])

    @Test
    fun testDetectionTimeBoundOrgMode() = verifyDetectionBound(parsers[9])

    @Test
    fun testDetectionTimeBoundRst() = verifyDetectionBound(parsers[10])

    @Test
    fun testDetectionTimeBoundKeyValue() = verifyDetectionBound(parsers[11])

    @Test
    fun testDetectionTimeBoundTaskPaper() = verifyDetectionBound(parsers[12])

    @Test
    fun testDetectionTimeBoundTextile() = verifyDetectionBound(parsers[13])

    @Test
    fun testDetectionTimeBoundJupyter() = verifyDetectionBound(parsers[14])

    @Test
    fun testDetectionTimeBoundRMarkdown() = verifyDetectionBound(parsers[15])

    @Test
    fun testDetectionTimeBoundBinary() = verifyDetectionBound(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    private fun verifyParseBound(entry: ParserEntry) = runBlocking<Unit> {
        val result = withTimeoutOrNull(5.seconds) {
            entry.parser.parse(entry.content)
        }
        assertNotNull(result, "${entry.name}: parsing did not complete within 5 seconds")
    }

    private fun verifyConcurrentParse(entry: ParserEntry) = runBlocking<Unit> {
        val results = withTimeoutOrNull(10.seconds) {
            (1..10).map { i ->
                async {
                    entry.parser.parse("${entry.content}\nConcurrent run $i")
                }
            }.awaitAll()
        }
        assertNotNull(results, "${entry.name}: 10 concurrent parses did not complete within 10s")
        assertEquals(10, results.size, "${entry.name}: expected 10 results from concurrent parses")
        for ((i, doc) in results.withIndex()) {
            assertNotNull(doc, "${entry.name}: concurrent parse $i returned null")
        }
    }

    private fun verifyHtmlBound(entry: ParserEntry) = runBlocking<Unit> {
        val doc = entry.parser.parse(entry.content)
        val htmlLight = withTimeoutOrNull(5.seconds) {
            doc.toHtml(lightMode = true)
        }
        assertNotNull(htmlLight, "${entry.name}: toHtml(light) did not complete within 5 seconds")

        // Clear cache and test dark mode
        doc.clearHtmlCache()
        val htmlDark = withTimeoutOrNull(5.seconds) {
            doc.toHtml(lightMode = false)
        }
        assertNotNull(htmlDark, "${entry.name}: toHtml(dark) did not complete within 5 seconds")
    }

    private fun verifyDetectionBound(entry: ParserEntry) {
        val elapsed = measureTime {
            // Detect by content
            FormatRegistry.detectByContent(entry.content)
            // Detect by extension
            FormatRegistry.detectByExtension(entry.parser.supportedFormat.defaultExtension)
        }
        assertTrue(
            elapsed < 1.seconds,
            "${entry.name}: format detection took ${elapsed}, expected < 1 second"
        )
    }
}
