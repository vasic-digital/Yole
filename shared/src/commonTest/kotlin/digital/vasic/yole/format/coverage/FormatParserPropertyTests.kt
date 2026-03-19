/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Property-Based Tests
 *
 * Validates universal properties that must hold for
 * ALL 17 format parsers: non-null output, empty input
 * handling, raw content preservation, HTML generation,
 * and deterministic parsing.
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
 * Property-based tests for all 17 format parsers.
 *
 * Properties verified:
 * 1. Any valid input produces non-null ParsedDocument
 * 2. Empty input produces ParsedDocument (not crash)
 * 3. ParsedDocument.rawContent matches input
 * 4. ParsedDocument.toHtml() returns non-empty string for non-empty input
 * 5. Parsing is deterministic: same input produces same output
 *
 * ~85 tests (5 per format x 17 formats)
 */
class FormatParserPropertyTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    private data class ParserEntry(
        val name: String,
        val parser: TextParser,
        val validInputs: List<String>
    )

    private val parsers: List<ParserEntry> = listOf(
        ParserEntry("Markdown", MarkdownParser(), listOf(
            "# Title", "**bold**", "- item\n- item2", "[link](url)", "> quote"
        )),
        ParserEntry("Plaintext", PlaintextParser(), listOf(
            "Hello world", "Line1\nLine2\nLine3", "Simple text", "    indented"
        )),
        ParserEntry("TodoTxt", TodoTxtParser(), listOf(
            "(A) Buy milk +grocery @store", "x 2025-01-01 Done task",
            "(B) 2025-02-01 Scheduled +project", "Simple task"
        )),
        ParserEntry("CSV", CsvParser(), listOf(
            "a,b,c\n1,2,3", "name,age\nJohn,30", "single", "a,b\nc,d\ne,f"
        )),
        ParserEntry("Wikitext", WikitextParser(), listOf(
            "== Heading ==", "'''bold'''", "[[Link]]", "* bullet"
        )),
        ParserEntry("Creole", CreoleParser(), listOf(
            "= Heading", "**bold**", "[[link]]", "* bullet"
        )),
        ParserEntry("TiddlyWiki", TiddlyWikiParser(), listOf(
            "! Heading", "''bold''", "[[Link]]", "* bullet"
        )),
        ParserEntry("LaTeX", LatexParser(), listOf(
            "\\section{Title}", "\\textbf{bold}", "\\begin{itemize}\\item one\\end{itemize}",
            "\\documentclass{article}"
        )),
        ParserEntry("AsciiDoc", AsciidocParser(), listOf(
            "= Title", "== Section", "*bold*", "* item"
        )),
        ParserEntry("OrgMode", OrgModeParser(), listOf(
            "* Heading", "** Subheading", "- list item", "#+TITLE: My Doc"
        )),
        ParserEntry("RST", RestructuredTextParser(), listOf(
            "Title\n=====", "Section\n-------", ".. directive::", "- bullet"
        )),
        ParserEntry("KeyValue", KeyValueParser(), listOf(
            "key=value", "[section]\nkey=val", "a=1\nb=2\nc=3", "name=test"
        )),
        ParserEntry("TaskPaper", TaskpaperParser(), listOf(
            "Project:\n\t- Task @tag", "- Simple task @done",
            "Category:\n\t- Item 1\n\t- Item 2", "- Task @priority(1)"
        )),
        ParserEntry("Textile", TextileParser(), listOf(
            "h1. Title", "*bold*", "# item", "p. Paragraph"
        )),
        ParserEntry("Jupyter", JupyterParser(), listOf(
            "{\"nbformat\":4,\"cells\":[]}", "{\"cell_type\":\"code\",\"source\":[\"1+1\"]}",
            "plain notebook text", "{}"
        )),
        ParserEntry("RMarkdown", RMarkdownParser(), listOf(
            "# Title\n```{r}\n1+1\n```", "## Section\nText", "```{r setup}\nlibrary(ggplot2)\n```"
        )),
        ParserEntry("Binary", BinaryParser(), listOf(
            "\u0001\u0002\u0003", "binary content", "\u00FF\u00FE", "mixed\u0000content"
        ))
    )

    // ====================================================================
    // 1. Any valid input produces non-null ParsedDocument
    // ====================================================================

    @Test
    fun testNonNullOutputMarkdown() = verifyNonNullOutput(parsers[0])

    @Test
    fun testNonNullOutputPlaintext() = verifyNonNullOutput(parsers[1])

    @Test
    fun testNonNullOutputTodoTxt() = verifyNonNullOutput(parsers[2])

    @Test
    fun testNonNullOutputCsv() = verifyNonNullOutput(parsers[3])

    @Test
    fun testNonNullOutputWikitext() = verifyNonNullOutput(parsers[4])

    @Test
    fun testNonNullOutputCreole() = verifyNonNullOutput(parsers[5])

    @Test
    fun testNonNullOutputTiddlyWiki() = verifyNonNullOutput(parsers[6])

    @Test
    fun testNonNullOutputLatex() = verifyNonNullOutput(parsers[7])

    @Test
    fun testNonNullOutputAsciiDoc() = verifyNonNullOutput(parsers[8])

    @Test
    fun testNonNullOutputOrgMode() = verifyNonNullOutput(parsers[9])

    @Test
    fun testNonNullOutputRst() = verifyNonNullOutput(parsers[10])

    @Test
    fun testNonNullOutputKeyValue() = verifyNonNullOutput(parsers[11])

    @Test
    fun testNonNullOutputTaskPaper() = verifyNonNullOutput(parsers[12])

    @Test
    fun testNonNullOutputTextile() = verifyNonNullOutput(parsers[13])

    @Test
    fun testNonNullOutputJupyter() = verifyNonNullOutput(parsers[14])

    @Test
    fun testNonNullOutputRMarkdown() = verifyNonNullOutput(parsers[15])

    @Test
    fun testNonNullOutputBinary() = verifyNonNullOutput(parsers[16])

    // ====================================================================
    // 2. Empty input produces ParsedDocument (not crash)
    // ====================================================================

    @Test
    fun testEmptyInputMarkdown() = verifyEmptyInput(parsers[0])

    @Test
    fun testEmptyInputPlaintext() = verifyEmptyInput(parsers[1])

    @Test
    fun testEmptyInputTodoTxt() = verifyEmptyInput(parsers[2])

    @Test
    fun testEmptyInputCsv() = verifyEmptyInput(parsers[3])

    @Test
    fun testEmptyInputWikitext() = verifyEmptyInput(parsers[4])

    @Test
    fun testEmptyInputCreole() = verifyEmptyInput(parsers[5])

    @Test
    fun testEmptyInputTiddlyWiki() = verifyEmptyInput(parsers[6])

    @Test
    fun testEmptyInputLatex() = verifyEmptyInput(parsers[7])

    @Test
    fun testEmptyInputAsciiDoc() = verifyEmptyInput(parsers[8])

    @Test
    fun testEmptyInputOrgMode() = verifyEmptyInput(parsers[9])

    @Test
    fun testEmptyInputRst() = verifyEmptyInput(parsers[10])

    @Test
    fun testEmptyInputKeyValue() = verifyEmptyInput(parsers[11])

    @Test
    fun testEmptyInputTaskPaper() = verifyEmptyInput(parsers[12])

    @Test
    fun testEmptyInputTextile() = verifyEmptyInput(parsers[13])

    @Test
    fun testEmptyInputJupyter() = verifyEmptyInput(parsers[14])

    @Test
    fun testEmptyInputRMarkdown() = verifyEmptyInput(parsers[15])

    @Test
    fun testEmptyInputBinary() = verifyEmptyInput(parsers[16])

    // ====================================================================
    // 3. ParsedDocument.rawContent matches input
    // ====================================================================

    @Test
    fun testRawContentPreservationMarkdown() = verifyRawContentPreservation(parsers[0])

    @Test
    fun testRawContentPreservationPlaintext() = verifyRawContentPreservation(parsers[1])

    @Test
    fun testRawContentPreservationTodoTxt() = verifyRawContentPreservation(parsers[2])

    @Test
    fun testRawContentPreservationCsv() = verifyRawContentPreservation(parsers[3])

    @Test
    fun testRawContentPreservationWikitext() = verifyRawContentPreservation(parsers[4])

    @Test
    fun testRawContentPreservationCreole() = verifyRawContentPreservation(parsers[5])

    @Test
    fun testRawContentPreservationTiddlyWiki() = verifyRawContentPreservation(parsers[6])

    @Test
    fun testRawContentPreservationLatex() = verifyRawContentPreservation(parsers[7])

    @Test
    fun testRawContentPreservationAsciiDoc() = verifyRawContentPreservation(parsers[8])

    @Test
    fun testRawContentPreservationOrgMode() = verifyRawContentPreservation(parsers[9])

    @Test
    fun testRawContentPreservationRst() = verifyRawContentPreservation(parsers[10])

    @Test
    fun testRawContentPreservationKeyValue() = verifyRawContentPreservation(parsers[11])

    @Test
    fun testRawContentPreservationTaskPaper() = verifyRawContentPreservation(parsers[12])

    @Test
    fun testRawContentPreservationTextile() = verifyRawContentPreservation(parsers[13])

    @Test
    fun testRawContentPreservationJupyter() = verifyRawContentPreservation(parsers[14])

    @Test
    fun testRawContentPreservationRMarkdown() = verifyRawContentPreservation(parsers[15])

    @Test
    fun testRawContentPreservationBinary() = verifyRawContentPreservation(parsers[16])

    // ====================================================================
    // 4. toHtml() returns non-empty string for non-empty input
    // ====================================================================

    @Test
    fun testHtmlNonEmptyMarkdown() = verifyHtmlNonEmpty(parsers[0])

    @Test
    fun testHtmlNonEmptyPlaintext() = verifyHtmlNonEmpty(parsers[1])

    @Test
    fun testHtmlNonEmptyTodoTxt() = verifyHtmlNonEmpty(parsers[2])

    @Test
    fun testHtmlNonEmptyCsv() = verifyHtmlNonEmpty(parsers[3])

    @Test
    fun testHtmlNonEmptyWikitext() = verifyHtmlNonEmpty(parsers[4])

    @Test
    fun testHtmlNonEmptyCreole() = verifyHtmlNonEmpty(parsers[5])

    @Test
    fun testHtmlNonEmptyTiddlyWiki() = verifyHtmlNonEmpty(parsers[6])

    @Test
    fun testHtmlNonEmptyLatex() = verifyHtmlNonEmpty(parsers[7])

    @Test
    fun testHtmlNonEmptyAsciiDoc() = verifyHtmlNonEmpty(parsers[8])

    @Test
    fun testHtmlNonEmptyOrgMode() = verifyHtmlNonEmpty(parsers[9])

    @Test
    fun testHtmlNonEmptyRst() = verifyHtmlNonEmpty(parsers[10])

    @Test
    fun testHtmlNonEmptyKeyValue() = verifyHtmlNonEmpty(parsers[11])

    @Test
    fun testHtmlNonEmptyTaskPaper() = verifyHtmlNonEmpty(parsers[12])

    @Test
    fun testHtmlNonEmptyTextile() = verifyHtmlNonEmpty(parsers[13])

    @Test
    fun testHtmlNonEmptyJupyter() = verifyHtmlNonEmpty(parsers[14])

    @Test
    fun testHtmlNonEmptyRMarkdown() = verifyHtmlNonEmpty(parsers[15])

    @Test
    fun testHtmlNonEmptyBinary() = verifyHtmlNonEmpty(parsers[16])

    // ====================================================================
    // 5. Deterministic: same input yields same output
    // ====================================================================

    @Test
    fun testDeterministicMarkdown() = verifyDeterministic(parsers[0])

    @Test
    fun testDeterministicPlaintext() = verifyDeterministic(parsers[1])

    @Test
    fun testDeterministicTodoTxt() = verifyDeterministic(parsers[2])

    @Test
    fun testDeterministicCsv() = verifyDeterministic(parsers[3])

    @Test
    fun testDeterministicWikitext() = verifyDeterministic(parsers[4])

    @Test
    fun testDeterministicCreole() = verifyDeterministic(parsers[5])

    @Test
    fun testDeterministicTiddlyWiki() = verifyDeterministic(parsers[6])

    @Test
    fun testDeterministicLatex() = verifyDeterministic(parsers[7])

    @Test
    fun testDeterministicAsciiDoc() = verifyDeterministic(parsers[8])

    @Test
    fun testDeterministicOrgMode() = verifyDeterministic(parsers[9])

    @Test
    fun testDeterministicRst() = verifyDeterministic(parsers[10])

    @Test
    fun testDeterministicKeyValue() = verifyDeterministic(parsers[11])

    @Test
    fun testDeterministicTaskPaper() = verifyDeterministic(parsers[12])

    @Test
    fun testDeterministicTextile() = verifyDeterministic(parsers[13])

    @Test
    fun testDeterministicJupyter() = verifyDeterministic(parsers[14])

    @Test
    fun testDeterministicRMarkdown() = verifyDeterministic(parsers[15])

    @Test
    fun testDeterministicBinary() = verifyDeterministic(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    private fun verifyNonNullOutput(entry: ParserEntry) {
        for (input in entry.validInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse('${input.take(30)}') returned null")
        }
    }

    private fun verifyEmptyInput(entry: ParserEntry) {
        val doc = entry.parser.parse("")
        assertNotNull(doc, "${entry.name}: parse('') returned null")
        assertEquals("", doc.rawContent, "${entry.name}: rawContent should be empty for empty input")
    }

    private fun verifyRawContentPreservation(entry: ParserEntry) {
        for (input in entry.validInputs) {
            val doc = entry.parser.parse(input)
            assertEquals(
                input, doc.rawContent,
                "${entry.name}: rawContent must exactly match input"
            )
        }
    }

    private fun verifyHtmlNonEmpty(entry: ParserEntry) {
        for (input in entry.validInputs) {
            val doc = entry.parser.parse(input)
            val htmlLight = doc.toHtml(lightMode = true)
            val htmlDark = doc.toHtml(lightMode = false)
            assertTrue(
                htmlLight.isNotEmpty(),
                "${entry.name}: toHtml(light) should be non-empty for input: '${input.take(30)}'"
            )
            assertTrue(
                htmlDark.isNotEmpty(),
                "${entry.name}: toHtml(dark) should be non-empty for input: '${input.take(30)}'"
            )
        }
    }

    private fun verifyDeterministic(entry: ParserEntry) {
        for (input in entry.validInputs) {
            val doc1 = entry.parser.parse(input)
            val doc2 = entry.parser.parse(input)
            assertEquals(
                doc1.rawContent, doc2.rawContent,
                "${entry.name}: rawContent differs between runs"
            )
            assertEquals(
                doc1.parsedContent, doc2.parsedContent,
                "${entry.name}: parsedContent differs between runs"
            )
            assertEquals(
                doc1.metadata, doc2.metadata,
                "${entry.name}: metadata differs between runs"
            )
            assertEquals(
                doc1.toHtml(lightMode = true), doc2.toHtml(lightMode = true),
                "${entry.name}: toHtml(light) differs between runs"
            )
        }
    }
}
