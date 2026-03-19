/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Resilience Tests
 *
 * Validates graceful handling of hostile input across
 * ALL 17 format parsers: corrupt/random bytes, very
 * long input, mixed encodings, whitespace-only input,
 * and special-character-only input.
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
import kotlin.random.Random
import kotlin.test.*

/**
 * Resilience tests for all 17 format parsers.
 *
 * Resilience scenarios:
 * 1. Corrupt/random bytes as input do not crash parser
 * 2. Very long input (100KB) parses without timeout
 * 3. Input with mixed encodings does not crash
 * 4. Input with only whitespace parses successfully
 * 5. Input with only special characters parses
 *
 * ~85 tests (5 per format x 17 formats)
 */
class FormatParserResilienceTests {

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
    // 1. Corrupt/random bytes do not crash parser
    // ====================================================================

    @Test
    fun testRandomBytesMarkdown() = verifyRandomBytes(parsers[0])

    @Test
    fun testRandomBytesPlaintext() = verifyRandomBytes(parsers[1])

    @Test
    fun testRandomBytesTodoTxt() = verifyRandomBytes(parsers[2])

    @Test
    fun testRandomBytesCsv() = verifyRandomBytes(parsers[3])

    @Test
    fun testRandomBytesWikitext() = verifyRandomBytes(parsers[4])

    @Test
    fun testRandomBytesCreole() = verifyRandomBytes(parsers[5])

    @Test
    fun testRandomBytesTiddlyWiki() = verifyRandomBytes(parsers[6])

    @Test
    fun testRandomBytesLatex() = verifyRandomBytes(parsers[7])

    @Test
    fun testRandomBytesAsciiDoc() = verifyRandomBytes(parsers[8])

    @Test
    fun testRandomBytesOrgMode() = verifyRandomBytes(parsers[9])

    @Test
    fun testRandomBytesRst() = verifyRandomBytes(parsers[10])

    @Test
    fun testRandomBytesKeyValue() = verifyRandomBytes(parsers[11])

    @Test
    fun testRandomBytesTaskPaper() = verifyRandomBytes(parsers[12])

    @Test
    fun testRandomBytesTextile() = verifyRandomBytes(parsers[13])

    @Test
    fun testRandomBytesJupyter() = verifyRandomBytes(parsers[14])

    @Test
    fun testRandomBytesRMarkdown() = verifyRandomBytes(parsers[15])

    @Test
    fun testRandomBytesBinary() = verifyRandomBytes(parsers[16])

    // ====================================================================
    // 2. Very long input (100KB) parses without error
    // ====================================================================

    @Test
    fun testLongInputMarkdown() = verifyLongInput(parsers[0])

    @Test
    fun testLongInputPlaintext() = verifyLongInput(parsers[1])

    @Test
    fun testLongInputTodoTxt() = verifyLongInput(parsers[2])

    @Test
    fun testLongInputCsv() = verifyLongInput(parsers[3])

    @Test
    fun testLongInputWikitext() = verifyLongInput(parsers[4])

    @Test
    fun testLongInputCreole() = verifyLongInput(parsers[5])

    @Test
    fun testLongInputTiddlyWiki() = verifyLongInput(parsers[6])

    @Test
    fun testLongInputLatex() = verifyLongInput(parsers[7])

    @Test
    fun testLongInputAsciiDoc() = verifyLongInput(parsers[8])

    @Test
    fun testLongInputOrgMode() = verifyLongInput(parsers[9])

    @Test
    fun testLongInputRst() = verifyLongInput(parsers[10])

    @Test
    fun testLongInputKeyValue() = verifyLongInput(parsers[11])

    @Test
    fun testLongInputTaskPaper() = verifyLongInput(parsers[12])

    @Test
    fun testLongInputTextile() = verifyLongInput(parsers[13])

    @Test
    fun testLongInputJupyter() = verifyLongInput(parsers[14])

    @Test
    fun testLongInputRMarkdown() = verifyLongInput(parsers[15])

    @Test
    fun testLongInputBinary() = verifyLongInput(parsers[16])

    // ====================================================================
    // 3. Mixed encoding characters do not crash
    // ====================================================================

    @Test
    fun testMixedEncodingMarkdown() = verifyMixedEncoding(parsers[0])

    @Test
    fun testMixedEncodingPlaintext() = verifyMixedEncoding(parsers[1])

    @Test
    fun testMixedEncodingTodoTxt() = verifyMixedEncoding(parsers[2])

    @Test
    fun testMixedEncodingCsv() = verifyMixedEncoding(parsers[3])

    @Test
    fun testMixedEncodingWikitext() = verifyMixedEncoding(parsers[4])

    @Test
    fun testMixedEncodingCreole() = verifyMixedEncoding(parsers[5])

    @Test
    fun testMixedEncodingTiddlyWiki() = verifyMixedEncoding(parsers[6])

    @Test
    fun testMixedEncodingLatex() = verifyMixedEncoding(parsers[7])

    @Test
    fun testMixedEncodingAsciiDoc() = verifyMixedEncoding(parsers[8])

    @Test
    fun testMixedEncodingOrgMode() = verifyMixedEncoding(parsers[9])

    @Test
    fun testMixedEncodingRst() = verifyMixedEncoding(parsers[10])

    @Test
    fun testMixedEncodingKeyValue() = verifyMixedEncoding(parsers[11])

    @Test
    fun testMixedEncodingTaskPaper() = verifyMixedEncoding(parsers[12])

    @Test
    fun testMixedEncodingTextile() = verifyMixedEncoding(parsers[13])

    @Test
    fun testMixedEncodingJupyter() = verifyMixedEncoding(parsers[14])

    @Test
    fun testMixedEncodingRMarkdown() = verifyMixedEncoding(parsers[15])

    @Test
    fun testMixedEncodingBinary() = verifyMixedEncoding(parsers[16])

    // ====================================================================
    // 4. Whitespace-only input parses successfully
    // ====================================================================

    @Test
    fun testWhitespaceOnlyMarkdown() = verifyWhitespaceOnly(parsers[0])

    @Test
    fun testWhitespaceOnlyPlaintext() = verifyWhitespaceOnly(parsers[1])

    @Test
    fun testWhitespaceOnlyTodoTxt() = verifyWhitespaceOnly(parsers[2])

    @Test
    fun testWhitespaceOnlyCsv() = verifyWhitespaceOnly(parsers[3])

    @Test
    fun testWhitespaceOnlyWikitext() = verifyWhitespaceOnly(parsers[4])

    @Test
    fun testWhitespaceOnlyCreole() = verifyWhitespaceOnly(parsers[5])

    @Test
    fun testWhitespaceOnlyTiddlyWiki() = verifyWhitespaceOnly(parsers[6])

    @Test
    fun testWhitespaceOnlyLatex() = verifyWhitespaceOnly(parsers[7])

    @Test
    fun testWhitespaceOnlyAsciiDoc() = verifyWhitespaceOnly(parsers[8])

    @Test
    fun testWhitespaceOnlyOrgMode() = verifyWhitespaceOnly(parsers[9])

    @Test
    fun testWhitespaceOnlyRst() = verifyWhitespaceOnly(parsers[10])

    @Test
    fun testWhitespaceOnlyKeyValue() = verifyWhitespaceOnly(parsers[11])

    @Test
    fun testWhitespaceOnlyTaskPaper() = verifyWhitespaceOnly(parsers[12])

    @Test
    fun testWhitespaceOnlyTextile() = verifyWhitespaceOnly(parsers[13])

    @Test
    fun testWhitespaceOnlyJupyter() = verifyWhitespaceOnly(parsers[14])

    @Test
    fun testWhitespaceOnlyRMarkdown() = verifyWhitespaceOnly(parsers[15])

    @Test
    fun testWhitespaceOnlyBinary() = verifyWhitespaceOnly(parsers[16])

    // ====================================================================
    // 5. Special-character-only input parses
    // ====================================================================

    @Test
    fun testSpecialCharsMarkdown() = verifySpecialChars(parsers[0])

    @Test
    fun testSpecialCharsPlaintext() = verifySpecialChars(parsers[1])

    @Test
    fun testSpecialCharsTodoTxt() = verifySpecialChars(parsers[2])

    @Test
    fun testSpecialCharsCsv() = verifySpecialChars(parsers[3])

    @Test
    fun testSpecialCharsWikitext() = verifySpecialChars(parsers[4])

    @Test
    fun testSpecialCharsCreole() = verifySpecialChars(parsers[5])

    @Test
    fun testSpecialCharsTiddlyWiki() = verifySpecialChars(parsers[6])

    @Test
    fun testSpecialCharsLatex() = verifySpecialChars(parsers[7])

    @Test
    fun testSpecialCharsAsciiDoc() = verifySpecialChars(parsers[8])

    @Test
    fun testSpecialCharsOrgMode() = verifySpecialChars(parsers[9])

    @Test
    fun testSpecialCharsRst() = verifySpecialChars(parsers[10])

    @Test
    fun testSpecialCharsKeyValue() = verifySpecialChars(parsers[11])

    @Test
    fun testSpecialCharsTaskPaper() = verifySpecialChars(parsers[12])

    @Test
    fun testSpecialCharsTextile() = verifySpecialChars(parsers[13])

    @Test
    fun testSpecialCharsJupyter() = verifySpecialChars(parsers[14])

    @Test
    fun testSpecialCharsRMarkdown() = verifySpecialChars(parsers[15])

    @Test
    fun testSpecialCharsBinary() = verifySpecialChars(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    /**
     * Generate pseudo-random character content using a seeded Random for
     * reproducibility. Uses printable + control chars to simulate corrupt data.
     */
    private fun generateRandomContent(size: Int, seed: Int = 42): String {
        val rng = Random(seed)
        return buildString {
            repeat(size) {
                // Mix of printable ASCII, control chars, and high Unicode
                val ch = when (rng.nextInt(4)) {
                    0 -> rng.nextInt(32, 127).toChar()  // printable ASCII
                    1 -> rng.nextInt(0, 32).toChar()     // control chars
                    2 -> rng.nextInt(128, 256).toChar()  // extended ASCII
                    else -> rng.nextInt(0x4E00, 0x4F00).toChar() // CJK
                }
                append(ch)
            }
        }
    }

    private fun verifyRandomBytes(entry: ParserEntry) {
        // Test with several seeds to get different random content
        for (seed in listOf(1, 42, 99, 255, 1000)) {
            val content = generateRandomContent(512, seed)
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "${entry.name}: parse returned null for random bytes (seed=$seed)")
            // toHtml must also not crash
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for random bytes (seed=$seed)")
        }
    }

    private fun verifyLongInput(entry: ParserEntry) {
        // Generate ~100KB of content as lines of text
        val lineCount = 2000
        val content = buildString {
            repeat(lineCount) { i ->
                appendLine("Line $i: " + "x".repeat(50))
            }
        }
        assertTrue(content.length >= 100_000,
            "Generated content should be at least 100KB, was ${content.length}")

        val doc = entry.parser.parse(content)
        assertNotNull(doc, "${entry.name}: parse returned null for 100KB input")
        assertTrue(
            doc.rawContent.length >= 100_000,
            "${entry.name}: rawContent should preserve full content"
        )
    }

    private val mixedEncodingInputs = listOf(
        // Latin + CJK + Arabic + Cyrillic + emoji representation
        "Hello \u4e16\u754c \u0645\u0631\u062d\u0628\u0627 \u041f\u0440\u0438\u0432\u0435\u0442",
        // Right-to-left + left-to-right mixing
        "\u202eReversed\u202c Normal",
        // Zero-width characters
        "Zero\u200Bwidth\u200Bspace\u200Bhere",
        // BOM markers
        "\uFEFFContent after BOM",
        // Combining characters
        "e\u0301 n\u0303 a\u030A"  // accented chars via combining marks
    )

    private fun verifyMixedEncoding(entry: ParserEntry) {
        for (input in mixedEncodingInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse returned null for mixed encoding input")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for mixed encoding input")
        }
    }

    private val whitespaceInputs = listOf(
        " ",
        "   ",
        "\t",
        "\t\t\t",
        "\n",
        "\n\n\n",
        "\r\n",
        "\r\n\r\n",
        " \t \n \r\n ",
        "    \t\t\n\n\r\n   "
    )

    private fun verifyWhitespaceOnly(entry: ParserEntry) {
        for (input in whitespaceInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse returned null for whitespace-only input")
        }
    }

    private val specialCharInputs = listOf(
        "!@#\$%^&*()",
        "{}[]|\\:;\"'<>?,./",
        "~`\u00A0\u00A1\u00BF\u00D7\u00F7",
        "\u2603\u2764\u2605\u2606\u2660\u2663\u2665\u2666", // symbols
        "\u2190\u2191\u2192\u2193\u2194\u2195",               // arrows
        "+-*/=<>!?@#%&|^~",
        "(((())))[[[[]]]]{{{{}}}}",
        "......------======++++++",
        "\"\"\"\'\'\'```````",
        "\\\\\\\\/////::::"
    )

    private fun verifySpecialChars(entry: ParserEntry) {
        for (input in specialCharInputs) {
            val doc = entry.parser.parse(input)
            assertNotNull(doc, "${entry.name}: parse returned null for special chars: '${input.take(20)}'")
            // toHtml must also handle special chars
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for special chars")
        }
    }
}
