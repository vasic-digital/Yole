/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Supremacy / Edge Case Tests
 *
 * Validates extreme edge cases across ALL 17 format
 * parsers: empty string, single character, newlines
 * only, Unicode-only, very long single line, and
 * binary-looking content.
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
 * Supremacy / edge case tests for all 17 format parsers.
 *
 * Edge cases:
 * 1. Empty string input
 * 2. Single character input
 * 3. Input of only newlines
 * 4. Input of only Unicode (CJK, Arabic, emoji)
 * 5. Very long single line (10KB on one line)
 * 6. Binary-looking content (random byte chars cast to string)
 *
 * ~102 tests (6 per format x 17 formats)
 */
class FormatParserSupremacyTests {

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
        val parser: TextParser
    )

    private val parsers: List<ParserEntry> = listOf(
        ParserEntry("Markdown", MarkdownParser()),
        ParserEntry("Plaintext", PlaintextParser()),
        ParserEntry("TodoTxt", TodoTxtParser()),
        ParserEntry("CSV", CsvParser()),
        ParserEntry("Wikitext", WikitextParser()),
        ParserEntry("Creole", CreoleParser()),
        ParserEntry("TiddlyWiki", TiddlyWikiParser()),
        ParserEntry("LaTeX", LatexParser()),
        ParserEntry("AsciiDoc", AsciidocParser()),
        ParserEntry("OrgMode", OrgModeParser()),
        ParserEntry("RST", RestructuredTextParser()),
        ParserEntry("KeyValue", KeyValueParser()),
        ParserEntry("TaskPaper", TaskpaperParser()),
        ParserEntry("Textile", TextileParser()),
        ParserEntry("Jupyter", JupyterParser()),
        ParserEntry("RMarkdown", RMarkdownParser()),
        ParserEntry("Binary", BinaryParser())
    )

    // ====================================================================
    // 1. Empty string input
    // ====================================================================

    @Test
    fun testEmptyStringMarkdown() = verifyEmptyString(parsers[0])

    @Test
    fun testEmptyStringPlaintext() = verifyEmptyString(parsers[1])

    @Test
    fun testEmptyStringTodoTxt() = verifyEmptyString(parsers[2])

    @Test
    fun testEmptyStringCsv() = verifyEmptyString(parsers[3])

    @Test
    fun testEmptyStringWikitext() = verifyEmptyString(parsers[4])

    @Test
    fun testEmptyStringCreole() = verifyEmptyString(parsers[5])

    @Test
    fun testEmptyStringTiddlyWiki() = verifyEmptyString(parsers[6])

    @Test
    fun testEmptyStringLatex() = verifyEmptyString(parsers[7])

    @Test
    fun testEmptyStringAsciiDoc() = verifyEmptyString(parsers[8])

    @Test
    fun testEmptyStringOrgMode() = verifyEmptyString(parsers[9])

    @Test
    fun testEmptyStringRst() = verifyEmptyString(parsers[10])

    @Test
    fun testEmptyStringKeyValue() = verifyEmptyString(parsers[11])

    @Test
    fun testEmptyStringTaskPaper() = verifyEmptyString(parsers[12])

    @Test
    fun testEmptyStringTextile() = verifyEmptyString(parsers[13])

    @Test
    fun testEmptyStringJupyter() = verifyEmptyString(parsers[14])

    @Test
    fun testEmptyStringRMarkdown() = verifyEmptyString(parsers[15])

    @Test
    fun testEmptyStringBinary() = verifyEmptyString(parsers[16])

    // ====================================================================
    // 2. Single character input
    // ====================================================================

    @Test
    fun testSingleCharMarkdown() = verifySingleChar(parsers[0])

    @Test
    fun testSingleCharPlaintext() = verifySingleChar(parsers[1])

    @Test
    fun testSingleCharTodoTxt() = verifySingleChar(parsers[2])

    @Test
    fun testSingleCharCsv() = verifySingleChar(parsers[3])

    @Test
    fun testSingleCharWikitext() = verifySingleChar(parsers[4])

    @Test
    fun testSingleCharCreole() = verifySingleChar(parsers[5])

    @Test
    fun testSingleCharTiddlyWiki() = verifySingleChar(parsers[6])

    @Test
    fun testSingleCharLatex() = verifySingleChar(parsers[7])

    @Test
    fun testSingleCharAsciiDoc() = verifySingleChar(parsers[8])

    @Test
    fun testSingleCharOrgMode() = verifySingleChar(parsers[9])

    @Test
    fun testSingleCharRst() = verifySingleChar(parsers[10])

    @Test
    fun testSingleCharKeyValue() = verifySingleChar(parsers[11])

    @Test
    fun testSingleCharTaskPaper() = verifySingleChar(parsers[12])

    @Test
    fun testSingleCharTextile() = verifySingleChar(parsers[13])

    @Test
    fun testSingleCharJupyter() = verifySingleChar(parsers[14])

    @Test
    fun testSingleCharRMarkdown() = verifySingleChar(parsers[15])

    @Test
    fun testSingleCharBinary() = verifySingleChar(parsers[16])

    // ====================================================================
    // 3. Input of only newlines
    // ====================================================================

    @Test
    fun testNewlinesOnlyMarkdown() = verifyNewlinesOnly(parsers[0])

    @Test
    fun testNewlinesOnlyPlaintext() = verifyNewlinesOnly(parsers[1])

    @Test
    fun testNewlinesOnlyTodoTxt() = verifyNewlinesOnly(parsers[2])

    @Test
    fun testNewlinesOnlyCsv() = verifyNewlinesOnly(parsers[3])

    @Test
    fun testNewlinesOnlyWikitext() = verifyNewlinesOnly(parsers[4])

    @Test
    fun testNewlinesOnlyCreole() = verifyNewlinesOnly(parsers[5])

    @Test
    fun testNewlinesOnlyTiddlyWiki() = verifyNewlinesOnly(parsers[6])

    @Test
    fun testNewlinesOnlyLatex() = verifyNewlinesOnly(parsers[7])

    @Test
    fun testNewlinesOnlyAsciiDoc() = verifyNewlinesOnly(parsers[8])

    @Test
    fun testNewlinesOnlyOrgMode() = verifyNewlinesOnly(parsers[9])

    @Test
    fun testNewlinesOnlyRst() = verifyNewlinesOnly(parsers[10])

    @Test
    fun testNewlinesOnlyKeyValue() = verifyNewlinesOnly(parsers[11])

    @Test
    fun testNewlinesOnlyTaskPaper() = verifyNewlinesOnly(parsers[12])

    @Test
    fun testNewlinesOnlyTextile() = verifyNewlinesOnly(parsers[13])

    @Test
    fun testNewlinesOnlyJupyter() = verifyNewlinesOnly(parsers[14])

    @Test
    fun testNewlinesOnlyRMarkdown() = verifyNewlinesOnly(parsers[15])

    @Test
    fun testNewlinesOnlyBinary() = verifyNewlinesOnly(parsers[16])

    // ====================================================================
    // 4. Unicode-only input (CJK, Arabic, emoji representation)
    // ====================================================================

    @Test
    fun testUnicodeOnlyMarkdown() = verifyUnicodeOnly(parsers[0])

    @Test
    fun testUnicodeOnlyPlaintext() = verifyUnicodeOnly(parsers[1])

    @Test
    fun testUnicodeOnlyTodoTxt() = verifyUnicodeOnly(parsers[2])

    @Test
    fun testUnicodeOnlyCsv() = verifyUnicodeOnly(parsers[3])

    @Test
    fun testUnicodeOnlyWikitext() = verifyUnicodeOnly(parsers[4])

    @Test
    fun testUnicodeOnlyCreole() = verifyUnicodeOnly(parsers[5])

    @Test
    fun testUnicodeOnlyTiddlyWiki() = verifyUnicodeOnly(parsers[6])

    @Test
    fun testUnicodeOnlyLatex() = verifyUnicodeOnly(parsers[7])

    @Test
    fun testUnicodeOnlyAsciiDoc() = verifyUnicodeOnly(parsers[8])

    @Test
    fun testUnicodeOnlyOrgMode() = verifyUnicodeOnly(parsers[9])

    @Test
    fun testUnicodeOnlyRst() = verifyUnicodeOnly(parsers[10])

    @Test
    fun testUnicodeOnlyKeyValue() = verifyUnicodeOnly(parsers[11])

    @Test
    fun testUnicodeOnlyTaskPaper() = verifyUnicodeOnly(parsers[12])

    @Test
    fun testUnicodeOnlyTextile() = verifyUnicodeOnly(parsers[13])

    @Test
    fun testUnicodeOnlyJupyter() = verifyUnicodeOnly(parsers[14])

    @Test
    fun testUnicodeOnlyRMarkdown() = verifyUnicodeOnly(parsers[15])

    @Test
    fun testUnicodeOnlyBinary() = verifyUnicodeOnly(parsers[16])

    // ====================================================================
    // 5. Very long single line (10KB on one line)
    // ====================================================================

    @Test
    fun testLongSingleLineMarkdown() = verifyLongSingleLine(parsers[0])

    @Test
    fun testLongSingleLinePlaintext() = verifyLongSingleLine(parsers[1])

    @Test
    fun testLongSingleLineTodoTxt() = verifyLongSingleLine(parsers[2])

    @Test
    fun testLongSingleLineCsv() = verifyLongSingleLine(parsers[3])

    @Test
    fun testLongSingleLineWikitext() = verifyLongSingleLine(parsers[4])

    @Test
    fun testLongSingleLineCreole() = verifyLongSingleLine(parsers[5])

    @Test
    fun testLongSingleLineTiddlyWiki() = verifyLongSingleLine(parsers[6])

    @Test
    fun testLongSingleLineLatex() = verifyLongSingleLine(parsers[7])

    @Test
    fun testLongSingleLineAsciiDoc() = verifyLongSingleLine(parsers[8])

    @Test
    fun testLongSingleLineOrgMode() = verifyLongSingleLine(parsers[9])

    @Test
    fun testLongSingleLineRst() = verifyLongSingleLine(parsers[10])

    @Test
    fun testLongSingleLineKeyValue() = verifyLongSingleLine(parsers[11])

    @Test
    fun testLongSingleLineTaskPaper() = verifyLongSingleLine(parsers[12])

    @Test
    fun testLongSingleLineTextile() = verifyLongSingleLine(parsers[13])

    @Test
    fun testLongSingleLineJupyter() = verifyLongSingleLine(parsers[14])

    @Test
    fun testLongSingleLineRMarkdown() = verifyLongSingleLine(parsers[15])

    @Test
    fun testLongSingleLineBinary() = verifyLongSingleLine(parsers[16])

    // ====================================================================
    // 6. Binary-looking content (high byte chars)
    // ====================================================================

    @Test
    fun testBinaryContentMarkdown() = verifyBinaryContent(parsers[0])

    @Test
    fun testBinaryContentPlaintext() = verifyBinaryContent(parsers[1])

    @Test
    fun testBinaryContentTodoTxt() = verifyBinaryContent(parsers[2])

    @Test
    fun testBinaryContentCsv() = verifyBinaryContent(parsers[3])

    @Test
    fun testBinaryContentWikitext() = verifyBinaryContent(parsers[4])

    @Test
    fun testBinaryContentCreole() = verifyBinaryContent(parsers[5])

    @Test
    fun testBinaryContentTiddlyWiki() = verifyBinaryContent(parsers[6])

    @Test
    fun testBinaryContentLatex() = verifyBinaryContent(parsers[7])

    @Test
    fun testBinaryContentAsciiDoc() = verifyBinaryContent(parsers[8])

    @Test
    fun testBinaryContentOrgMode() = verifyBinaryContent(parsers[9])

    @Test
    fun testBinaryContentRst() = verifyBinaryContent(parsers[10])

    @Test
    fun testBinaryContentKeyValue() = verifyBinaryContent(parsers[11])

    @Test
    fun testBinaryContentTaskPaper() = verifyBinaryContent(parsers[12])

    @Test
    fun testBinaryContentTextile() = verifyBinaryContent(parsers[13])

    @Test
    fun testBinaryContentJupyter() = verifyBinaryContent(parsers[14])

    @Test
    fun testBinaryContentRMarkdown() = verifyBinaryContent(parsers[15])

    @Test
    fun testBinaryContentBinary() = verifyBinaryContent(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    private fun verifyEmptyString(entry: ParserEntry) {
        val doc = entry.parser.parse("")
        assertNotNull(doc, "${entry.name}: parse('') must not return null")
        assertEquals("", doc.rawContent, "${entry.name}: rawContent should be empty")
        // toHtml must not crash on empty document
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html, "${entry.name}: toHtml on empty doc must not return null")
        // validate must not crash on empty
        val errors = entry.parser.validate("")
        assertNotNull(errors, "${entry.name}: validate('') must not return null")
    }

    private fun verifySingleChar(entry: ParserEntry) {
        val singleChars = listOf("a", "1", "#", "*", "=", "!", "-", "\\", "{", " ")
        for (ch in singleChars) {
            val doc = entry.parser.parse(ch)
            assertNotNull(doc, "${entry.name}: parse('$ch') must not return null")
            assertEquals(ch, doc.rawContent, "${entry.name}: rawContent must match single char '$ch'")
            // toHtml should work
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml on single char '$ch' must not return null")
        }
    }

    private fun verifyNewlinesOnly(entry: ParserEntry) {
        val newlineInputs = listOf(
            "\n",
            "\n\n\n\n\n",
            "\r\n\r\n\r\n",
            "\n".repeat(100),
            "\r\n".repeat(50)
        )
        for (input in newlineInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse of newlines-only must not return null")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml on newlines-only must not return null")
        }
    }

    private val unicodeInputs = listOf(
        // CJK characters
        "\u4e16\u754c\u4f60\u597d\u5927\u5bb6",
        // Arabic
        "\u0645\u0631\u062d\u0628\u0627 \u0628\u0627\u0644\u0639\u0627\u0644\u0645",
        // Cyrillic
        "\u041f\u0440\u0438\u0432\u0435\u0442 \u043c\u0438\u0440",
        // Korean
        "\uc548\ub155\ud558\uc138\uc694 \uc138\uacc4",
        // Thai
        "\u0e2a\u0e27\u0e31\u0e2a\u0e14\u0e35\u0e0a\u0e32\u0e27\u0e42\u0e25\u0e01",
        // Mixed script: CJK + Latin + Cyrillic
        "\u4e16\u754c Hello \u041c\u0438\u0440",
        // Various symbols and punctuation
        "\u2603\u2764\u2605\u266B\u263A\u2622\u2623"
    )

    private fun verifyUnicodeOnly(entry: ParserEntry) {
        for (input in unicodeInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse of Unicode-only must not return null")
            assertEquals(input, doc.rawContent,
                "${entry.name}: rawContent must preserve Unicode content")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml on Unicode must not return null")
            assertTrue(html.isNotEmpty(),
                "${entry.name}: toHtml on Unicode must not be empty")
        }
    }

    private fun verifyLongSingleLine(entry: ParserEntry) {
        // 10KB of text on a single line (no newline characters)
        val longLine = "x".repeat(10_240)
        assertTrue(longLine.length >= 10_000, "Long line should be at least 10KB")
        assertFalse(longLine.contains("\n"), "Long line must not contain newlines")

        val doc = entry.parser.parse(longLine)
        assertNotNull(doc, "${entry.name}: parse of 10KB single line must not return null")
        assertEquals(longLine, doc.rawContent,
            "${entry.name}: rawContent must preserve entire 10KB line")
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html, "${entry.name}: toHtml on 10KB line must not return null")
        assertTrue(html.isNotEmpty(),
            "${entry.name}: toHtml on 10KB line must not be empty")
    }

    private fun verifyBinaryContent(entry: ParserEntry) {
        // Content that looks like binary data: control characters, high bytes
        val binaryInputs = listOf(
            // Control characters
            buildString {
                for (i in 0..31) {
                    if (i != 10 && i != 13) { // skip \n and \r to test pure binary
                        append(i.toChar())
                    }
                }
            },
            // High byte Latin-1 supplement chars
            buildString {
                for (i in 128..255) {
                    append(i.toChar())
                }
            },
            // Mix of null, BEL, ESC, DEL and normal text
            "\u0000\u0007\u001B\u007FHello\u0000World\u0007",
            // Repeated high bytes
            "\u00FF".repeat(100)
        )

        for (input in binaryInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse of binary content must not return null")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml on binary content must not return null")
        }
    }
}
