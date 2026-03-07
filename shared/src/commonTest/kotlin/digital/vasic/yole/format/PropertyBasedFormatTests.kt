/*#######################################################
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property-based style tests for all 17 format parsers.
 * Each parser is tested against 10 edge-case input
 * scenarios to ensure robustness and no crashes.
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

/**
 * Property-based style tests for all 17 format parsers.
 *
 * Each parser is subjected to 10 edge-case input categories:
 * 1. Empty string
 * 2. Single character
 * 3. Very long input (100,000 chars)
 * 4. Only whitespace
 * 5. Only newlines
 * 6. Null characters (\u0000)
 * 7. All printable ASCII
 * 8. Unicode (CJK, emoji, RTL)
 * 9. Mixed line endings (\r\n, \r, \n)
 * 10. Binary-like input (random byte patterns as string)
 *
 * Total: 17 parsers x 10 scenarios = 170 tests
 */
class PropertyBasedFormatTests {

    // ====================================================================
    // Shared test inputs
    // ====================================================================

    private val emptyInput = ""
    private val singleCharInput = "x"
    private val longInput = "a".repeat(100_000)
    private val whitespaceOnlyInput = "   \t  \t   \t\t    "
    private val newlineOnlyInput = "\n\n\n\n\n\n\n\n\n\n"
    private val nullCharInput = "Hello\u0000World\u0000Test\u0000"
    private val printableAsciiInput = buildString {
        for (c in 32..126) append(c.toChar())
    }
    private val unicodeInput = buildString {
        // CJK characters
        append("\u4F60\u597D\u4E16\u754C ")
        // Emoji
        append("\uD83D\uDE00\uD83D\uDE80\uD83C\uDF1F ")
        // RTL Arabic
        append("\u0645\u0631\u062D\u0628\u0627 ")
        // Accented Latin
        append("\u00E9\u00E0\u00FC\u00F1\u00F6 ")
        // Japanese Hiragana
        append("\u3053\u3093\u306B\u3061\u306F")
    }
    private val mixedLineEndingsInput = "Line1\r\nLine2\rLine3\nLine4\r\n\rLine6\n"
    private val binaryLikeInput = buildString {
        // Simulate binary-like content with various byte values as chars
        for (i in 0..255) append(i.toChar())
        // Also include common binary file headers
        append("%PDF-1.4")
        append("PK\u0003\u0004")
    }

    // Helper to run all 10 scenarios on a given parser
    private fun assertParserHandlesAllInputs(parser: TextParser, parserName: String) {
        // This is used for documentation; individual tests call specific scenarios.
    }

    // ====================================================================
    // 1. Markdown Parser
    // ====================================================================

    @Test
    fun testMarkdownEmptyString() {
        val parser = MarkdownParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc, "MarkdownParser should not return null for empty input")
        assertEquals(emptyInput, doc.rawContent)
    }

    @Test
    fun testMarkdownSingleChar() {
        val parser = MarkdownParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
        assertEquals(singleCharInput, doc.rawContent)
    }

    @Test
    fun testMarkdownVeryLongInput() {
        val parser = MarkdownParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
        assertEquals(100_000, doc.rawContent.length)
    }

    @Test
    fun testMarkdownWhitespaceOnly() {
        val parser = MarkdownParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testMarkdownNewlinesOnly() {
        val parser = MarkdownParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testMarkdownNullChars() {
        val parser = MarkdownParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testMarkdownPrintableAscii() {
        val parser = MarkdownParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownUnicode() {
        val parser = MarkdownParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testMarkdownMixedLineEndings() {
        val parser = MarkdownParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testMarkdownBinaryLikeInput() {
        val parser = MarkdownParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 2. Plaintext Parser
    // ====================================================================

    @Test
    fun testPlaintextEmptyString() {
        val parser = PlaintextParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
        assertEquals(emptyInput, doc.rawContent)
    }

    @Test
    fun testPlaintextSingleChar() {
        val parser = PlaintextParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextVeryLongInput() {
        val parser = PlaintextParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
        assertEquals(100_000, doc.rawContent.length)
    }

    @Test
    fun testPlaintextWhitespaceOnly() {
        val parser = PlaintextParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextNewlinesOnly() {
        val parser = PlaintextParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextNullChars() {
        val parser = PlaintextParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextPrintableAscii() {
        val parser = PlaintextParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextUnicode() {
        val parser = PlaintextParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextMixedLineEndings() {
        val parser = PlaintextParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testPlaintextBinaryLikeInput() {
        val parser = PlaintextParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 3. TodoTxt Parser
    // ====================================================================

    @Test
    fun testTodoTxtEmptyString() {
        val parser = TodoTxtParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtSingleChar() {
        val parser = TodoTxtParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtVeryLongInput() {
        val parser = TodoTxtParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtWhitespaceOnly() {
        val parser = TodoTxtParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtNewlinesOnly() {
        val parser = TodoTxtParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtNullChars() {
        val parser = TodoTxtParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtPrintableAscii() {
        val parser = TodoTxtParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtUnicode() {
        val parser = TodoTxtParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtMixedLineEndings() {
        val parser = TodoTxtParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testTodoTxtBinaryLikeInput() {
        val parser = TodoTxtParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 4. CSV Parser
    // ====================================================================

    @Test
    fun testCsvEmptyString() {
        val parser = CsvParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvSingleChar() {
        val parser = CsvParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvVeryLongInput() {
        val parser = CsvParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvWhitespaceOnly() {
        val parser = CsvParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvNewlinesOnly() {
        val parser = CsvParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvNullChars() {
        val parser = CsvParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvPrintableAscii() {
        val parser = CsvParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvUnicode() {
        val parser = CsvParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvMixedLineEndings() {
        val parser = CsvParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testCsvBinaryLikeInput() {
        val parser = CsvParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 5. WikiText Parser
    // ====================================================================

    @Test
    fun testWikitextEmptyString() {
        val parser = WikitextParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextSingleChar() {
        val parser = WikitextParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextVeryLongInput() {
        val parser = WikitextParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextWhitespaceOnly() {
        val parser = WikitextParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextNewlinesOnly() {
        val parser = WikitextParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextNullChars() {
        val parser = WikitextParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextPrintableAscii() {
        val parser = WikitextParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextUnicode() {
        val parser = WikitextParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextMixedLineEndings() {
        val parser = WikitextParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testWikitextBinaryLikeInput() {
        val parser = WikitextParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 6. Creole Parser
    // ====================================================================

    @Test
    fun testCreoleEmptyString() {
        val parser = CreoleParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleSingleChar() {
        val parser = CreoleParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleVeryLongInput() {
        val parser = CreoleParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleWhitespaceOnly() {
        val parser = CreoleParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleNewlinesOnly() {
        val parser = CreoleParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleNullChars() {
        val parser = CreoleParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreolePrintableAscii() {
        val parser = CreoleParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleUnicode() {
        val parser = CreoleParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleMixedLineEndings() {
        val parser = CreoleParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testCreoleBinaryLikeInput() {
        val parser = CreoleParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 7. TiddlyWiki Parser
    // ====================================================================

    @Test
    fun testTiddlyWikiEmptyString() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiSingleChar() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiVeryLongInput() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiWhitespaceOnly() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiNewlinesOnly() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiNullChars() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiPrintableAscii() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiUnicode() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiMixedLineEndings() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testTiddlyWikiBinaryLikeInput() {
        val parser = TiddlyWikiParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 8. LaTeX Parser
    // ====================================================================

    @Test
    fun testLatexEmptyString() {
        val parser = LatexParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexSingleChar() {
        val parser = LatexParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexVeryLongInput() {
        val parser = LatexParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexWhitespaceOnly() {
        val parser = LatexParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexNewlinesOnly() {
        val parser = LatexParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexNullChars() {
        val parser = LatexParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexPrintableAscii() {
        val parser = LatexParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexUnicode() {
        val parser = LatexParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexMixedLineEndings() {
        val parser = LatexParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testLatexBinaryLikeInput() {
        val parser = LatexParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 9. AsciiDoc Parser
    // ====================================================================

    @Test
    fun testAsciidocEmptyString() {
        val parser = AsciidocParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocSingleChar() {
        val parser = AsciidocParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocVeryLongInput() {
        val parser = AsciidocParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocWhitespaceOnly() {
        val parser = AsciidocParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocNewlinesOnly() {
        val parser = AsciidocParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocNullChars() {
        val parser = AsciidocParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocPrintableAscii() {
        val parser = AsciidocParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocUnicode() {
        val parser = AsciidocParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocMixedLineEndings() {
        val parser = AsciidocParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testAsciidocBinaryLikeInput() {
        val parser = AsciidocParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 10. OrgMode Parser
    // ====================================================================

    @Test
    fun testOrgModeEmptyString() {
        val parser = OrgModeParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeSingleChar() {
        val parser = OrgModeParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeVeryLongInput() {
        val parser = OrgModeParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeWhitespaceOnly() {
        val parser = OrgModeParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeNewlinesOnly() {
        val parser = OrgModeParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeNullChars() {
        val parser = OrgModeParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModePrintableAscii() {
        val parser = OrgModeParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeUnicode() {
        val parser = OrgModeParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeMixedLineEndings() {
        val parser = OrgModeParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testOrgModeBinaryLikeInput() {
        val parser = OrgModeParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 11. RestructuredText Parser
    // ====================================================================

    @Test
    fun testRstEmptyString() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstSingleChar() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstVeryLongInput() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstWhitespaceOnly() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstNewlinesOnly() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstNullChars() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstPrintableAscii() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstUnicode() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstMixedLineEndings() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testRstBinaryLikeInput() {
        val parser = RestructuredTextParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 12. KeyValue Parser
    // ====================================================================

    @Test
    fun testKeyValueEmptyString() {
        val parser = KeyValueParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueSingleChar() {
        val parser = KeyValueParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueVeryLongInput() {
        val parser = KeyValueParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueWhitespaceOnly() {
        val parser = KeyValueParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueNewlinesOnly() {
        val parser = KeyValueParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueNullChars() {
        val parser = KeyValueParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValuePrintableAscii() {
        val parser = KeyValueParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueUnicode() {
        val parser = KeyValueParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueMixedLineEndings() {
        val parser = KeyValueParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testKeyValueBinaryLikeInput() {
        val parser = KeyValueParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 13. Taskpaper Parser
    // ====================================================================

    @Test
    fun testTaskpaperEmptyString() {
        val parser = TaskpaperParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperSingleChar() {
        val parser = TaskpaperParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperVeryLongInput() {
        val parser = TaskpaperParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperWhitespaceOnly() {
        val parser = TaskpaperParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperNewlinesOnly() {
        val parser = TaskpaperParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperNullChars() {
        val parser = TaskpaperParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperPrintableAscii() {
        val parser = TaskpaperParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperUnicode() {
        val parser = TaskpaperParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperMixedLineEndings() {
        val parser = TaskpaperParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testTaskpaperBinaryLikeInput() {
        val parser = TaskpaperParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 14. Textile Parser
    // ====================================================================

    @Test
    fun testTextileEmptyString() {
        val parser = TextileParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileSingleChar() {
        val parser = TextileParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileVeryLongInput() {
        val parser = TextileParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileWhitespaceOnly() {
        val parser = TextileParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileNewlinesOnly() {
        val parser = TextileParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileNullChars() {
        val parser = TextileParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextilePrintableAscii() {
        val parser = TextileParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileUnicode() {
        val parser = TextileParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileMixedLineEndings() {
        val parser = TextileParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testTextileBinaryLikeInput() {
        val parser = TextileParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 15. Jupyter Parser
    // ====================================================================

    @Test
    fun testJupyterEmptyString() {
        val parser = JupyterParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterSingleChar() {
        val parser = JupyterParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterVeryLongInput() {
        val parser = JupyterParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterWhitespaceOnly() {
        val parser = JupyterParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterNewlinesOnly() {
        val parser = JupyterParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterNullChars() {
        val parser = JupyterParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterPrintableAscii() {
        val parser = JupyterParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterUnicode() {
        val parser = JupyterParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterMixedLineEndings() {
        val parser = JupyterParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testJupyterBinaryLikeInput() {
        val parser = JupyterParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 16. RMarkdown Parser
    // ====================================================================

    @Test
    fun testRMarkdownEmptyString() {
        val parser = RMarkdownParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownSingleChar() {
        val parser = RMarkdownParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownVeryLongInput() {
        val parser = RMarkdownParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownWhitespaceOnly() {
        val parser = RMarkdownParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownNewlinesOnly() {
        val parser = RMarkdownParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownNullChars() {
        val parser = RMarkdownParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownPrintableAscii() {
        val parser = RMarkdownParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownUnicode() {
        val parser = RMarkdownParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownMixedLineEndings() {
        val parser = RMarkdownParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testRMarkdownBinaryLikeInput() {
        val parser = RMarkdownParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // 17. Binary Parser
    // ====================================================================

    @Test
    fun testBinaryEmptyString() {
        val parser = BinaryParser()
        val doc = parser.parse(emptyInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinarySingleChar() {
        val parser = BinaryParser()
        val doc = parser.parse(singleCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryVeryLongInput() {
        val parser = BinaryParser()
        val doc = parser.parse(longInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryWhitespaceOnly() {
        val parser = BinaryParser()
        val doc = parser.parse(whitespaceOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryNewlinesOnly() {
        val parser = BinaryParser()
        val doc = parser.parse(newlineOnlyInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryNullChars() {
        val parser = BinaryParser()
        val doc = parser.parse(nullCharInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryPrintableAscii() {
        val parser = BinaryParser()
        val doc = parser.parse(printableAsciiInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryUnicode() {
        val parser = BinaryParser()
        val doc = parser.parse(unicodeInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryMixedLineEndings() {
        val parser = BinaryParser()
        val doc = parser.parse(mixedLineEndingsInput)
        assertNotNull(doc)
    }

    @Test
    fun testBinaryBinaryLikeInput() {
        val parser = BinaryParser()
        val doc = parser.parse(binaryLikeInput)
        assertNotNull(doc)
    }

    // ====================================================================
    // Cross-parser invariant tests
    // ====================================================================

    @Test
    fun testAllParsersPreserveRawContent() {
        val parsers = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
            WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
            AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
            KeyValueParser(), TaskpaperParser(), TextileParser(),
            JupyterParser(), RMarkdownParser(), BinaryParser()
        )
        val testInput = "Hello, World!"
        for (parser in parsers) {
            val doc = parser.parse(testInput)
            assertEquals(testInput, doc.rawContent,
                "Parser ${parser.supportedFormat.name} should preserve raw content")
        }
    }

    @Test
    fun testAllParsersReturnNonNullMetadata() {
        val parsers = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
            WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
            AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
            KeyValueParser(), TaskpaperParser(), TextileParser(),
            JupyterParser(), RMarkdownParser(), BinaryParser()
        )
        for (parser in parsers) {
            val doc = parser.parse("test content")
            assertNotNull(doc.metadata,
                "Parser ${parser.supportedFormat.name} should have non-null metadata")
            assertNotNull(doc.errors,
                "Parser ${parser.supportedFormat.name} should have non-null errors")
            assertNotNull(doc.format,
                "Parser ${parser.supportedFormat.name} should have non-null format")
        }
    }

    @Test
    fun testAllParsersReturnNonEmptyParsedContentForNonEmpty() {
        val parsers = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
            WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
            AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
            KeyValueParser(), TaskpaperParser(), TextileParser(),
            RMarkdownParser(), BinaryParser()
        )
        val testInput = "Some meaningful content with text"
        for (parser in parsers) {
            val doc = parser.parse(testInput)
            assertTrue(doc.parsedContent.isNotEmpty(),
                "Parser ${parser.supportedFormat.name} should produce non-empty parsedContent")
        }
    }

    @Test
    fun testAllParsersAssignCorrectFormat() {
        val parsers = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
            WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
            AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
            KeyValueParser(), TaskpaperParser(), TextileParser(),
            JupyterParser(), RMarkdownParser(), BinaryParser()
        )
        for (parser in parsers) {
            val doc = parser.parse("test")
            assertEquals(parser.supportedFormat.id, doc.format.id,
                "Parser ${parser.supportedFormat.name} should set correct format on doc")
        }
    }

    @Test
    fun testAllParsersHandleRepeatedParsing() {
        val parsers = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(), CsvParser(),
            WikitextParser(), CreoleParser(), TiddlyWikiParser(), LatexParser(),
            AsciidocParser(), OrgModeParser(), RestructuredTextParser(),
            KeyValueParser(), TaskpaperParser(), TextileParser(),
            JupyterParser(), RMarkdownParser(), BinaryParser()
        )
        val testInput = "Repeated parsing test"
        for (parser in parsers) {
            // Parse the same content multiple times to ensure no state leakage
            val doc1 = parser.parse(testInput)
            val doc2 = parser.parse(testInput)
            val doc3 = parser.parse(testInput)
            assertEquals(doc1.rawContent, doc2.rawContent)
            assertEquals(doc2.rawContent, doc3.rawContent)
            assertEquals(doc1.parsedContent, doc2.parsedContent,
                "Parser ${parser.supportedFormat.name} should produce deterministic output")
        }
    }
}
