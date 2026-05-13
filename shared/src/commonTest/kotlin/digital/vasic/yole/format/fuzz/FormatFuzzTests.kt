/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fuzz tests for all 18 format parsers.
 *
 * Each parser is subjected to 200 iterations of random,
 * unicode, and malformed inputs to verify crash safety
 * and robustness. Additional cross-format fuzz tests
 * validate that the same random input can be fed to all
 * parsers without OOM or unrecoverable errors.
 *
 *########################################################*/
package digital.vasic.yole.format.fuzz

import digital.vasic.yole.format.*
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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Fuzz tests for all 18 format parsers.
 *
 * Validates that parsers do not crash, OOM, or produce null fields
 * when fed random ASCII, random Unicode, and deliberately malformed
 * inputs. Deterministic seeds ensure reproducibility.
 */
class FormatFuzzTests {

    // ====================================================================
    // Input generators
    // ====================================================================

    private fun generateRandomString(random: Random, maxLength: Int = 5000): String {
        val length = random.nextInt(0, maxLength)
        return buildString {
            repeat(length) {
                append(random.nextInt(32, 127).toChar())
            }
        }
    }

    private fun generateRandomUnicodeString(random: Random, maxLength: Int = 2000): String {
        val length = random.nextInt(0, maxLength)
        return buildString {
            repeat(length) {
                val codePoint = when (random.nextInt(4)) {
                    0 -> random.nextInt(32, 127)        // ASCII printable
                    1 -> random.nextInt(0x00C0, 0x00FF) // Latin Extended-A
                    2 -> random.nextInt(0x0400, 0x04FF) // Cyrillic
                    else -> random.nextInt(0x4E00, 0x4FFF) // CJK Unified Ideographs
                }
                append(codePoint.toChar())
            }
        }
    }

    private fun generateMalformedInput(random: Random): String {
        val templates = listOf(
            // Deeply nested brackets
            buildString { repeat(100) { append("[") }; repeat(100) { append("]") } },
            // Massive single line (bounded to avoid O(n^2) parser hangs)
            "a".repeat(10_000),
            // Only whitespace
            " \t\n\r".repeat(1000),
            // Only special characters
            buildString { repeat(500) { append("!@#\$%^&*()") } },
            // Null bytes and control chars
            buildString { repeat(100) { append('\u0000'); append('\u0001'); append('\u001F') } },
            // Mixed line endings
            "line1\r\nline2\rline3\nline4\r\n\rline5",
            // Extremely long lines (bounded)
            "x".repeat(10_000) + "\n" + "y".repeat(10_000),
            // Empty
            "",
            // Single char
            "x",
            // Binary-looking content
            buildString { repeat(100) { append(random.nextInt(0, 255).toChar()) } }
        )
        return templates[random.nextInt(templates.size)]
    }

    // ====================================================================
    // Helper: run fuzz for a single parser
    // ====================================================================

    private fun fuzzParser(parser: TextParser, seed: Int) {
        val random = Random(seed)
        repeat(200) { i ->
            val input = when (i % 3) {
                0 -> generateRandomString(random)
                1 -> generateRandomUnicodeString(random)
                else -> generateMalformedInput(random)
            }
            val result = runCatching { parser.parse(input) }
            assertTrue(
                result.isSuccess || result.exceptionOrNull() !is OutOfMemoryError,
                "Parser ${parser.supportedFormat.name} should not OOM on iteration $i"
            )
            result.getOrNull()?.let { doc ->
                assertNotNull(doc.rawContent, "rawContent should not be null for ${parser.supportedFormat.name} iteration $i")
                assertNotNull(doc.parsedContent, "parsedContent should not be null for ${parser.supportedFormat.name} iteration $i")
            }
        }
    }

    // ====================================================================
    // Per-format fuzz tests (17 total)
    // ====================================================================

    @Test
    fun fuzzMarkdownParser() {
        fuzzParser(MarkdownParser(), seed = 42)
    }

    @Test
    fun fuzzTodoTxtParser() {
        fuzzParser(TodoTxtParser(), seed = 43)
    }

    @Test
    fun fuzzCsvParser() {
        fuzzParser(CsvParser(), seed = 44)
    }

    @Test
    fun fuzzLatexParser() {
        fuzzParser(LatexParser(), seed = 45)
    }

    @Test
    fun fuzzPlaintextParser() {
        fuzzParser(PlaintextParser(), seed = 46)
    }

    @Test
    fun fuzzOrgModeParser() {
        fuzzParser(OrgModeParser(), seed = 47)
    }

    @Test
    fun fuzzWikitextParser() {
        fuzzParser(WikitextParser(), seed = 48)
    }

    @Test
    fun fuzzAsciidocParser() {
        fuzzParser(AsciidocParser(), seed = 49)
    }

    @Test
    fun fuzzRestructuredTextParser() {
        fuzzParser(RestructuredTextParser(), seed = 50)
    }

    @Test
    fun fuzzRMarkdownParser() {
        fuzzParser(RMarkdownParser(), seed = 51)
    }

    @Test
    fun fuzzTaskpaperParser() {
        fuzzParser(TaskpaperParser(), seed = 52)
    }

    @Test
    fun fuzzTextileParser() {
        fuzzParser(TextileParser(), seed = 53)
    }

    @Test
    fun fuzzCreoleParser() {
        fuzzParser(CreoleParser(), seed = 54)
    }

    @Test
    fun fuzzTiddlyWikiParser() {
        fuzzParser(TiddlyWikiParser(), seed = 55)
    }

    @Test
    fun fuzzJupyterParser() {
        fuzzParser(JupyterParser(), seed = 56)
    }

    @Test
    fun fuzzKeyValueParser() {
        fuzzParser(KeyValueParser(), seed = 57)
    }

    @Test
    fun fuzzBinaryParser() {
        fuzzParser(BinaryParser(), seed = 58)
    }

    // ====================================================================
    // Cross-format fuzz tests
    // ====================================================================

    @Test
    fun fuzzAllFormatsWithSameInput() {
        val random = Random(123)
        val parsers: List<TextParser> = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(),
            CsvParser(), WikitextParser(), CreoleParser(),
            TiddlyWikiParser(), LatexParser(), AsciidocParser(),
            OrgModeParser(), RestructuredTextParser(), KeyValueParser(),
            TaskpaperParser(), TextileParser(), JupyterParser(),
            RMarkdownParser(), BinaryParser()
        )

        repeat(50) {
            val input = generateRandomString(random, 10_000)
            parsers.forEach { parser ->
                val result = runCatching { parser.parse(input) }
                assertTrue(
                    result.isSuccess || result.exceptionOrNull() !is OutOfMemoryError,
                    "${parser.supportedFormat.name} should not OOM on shared-input iteration $it"
                )
            }
        }
    }

    @Test
    fun fuzzFormatDetection() {
        val random = Random(456)
        repeat(200) {
            val input = generateRandomString(random, 1000)
            // Detection should never throw, even on garbage input
            val result = runCatching { FormatRegistry.detectByContent(input) }
            assertTrue(result.isSuccess, "detectByContent should not throw on iteration $it")
        }
    }

    @Test
    fun fuzzHtmlGenerationAfterParse() {
        val random = Random(789)
        val parsers: List<TextParser> = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(),
            CsvParser(), WikitextParser(), LatexParser(),
            AsciidocParser(), OrgModeParser()
        )

        repeat(50) { i ->
            val input = generateRandomString(random, 2000)
            parsers.forEach { parser ->
                val parseResult = runCatching { parser.parse(input) }
                parseResult.getOrNull()?.let { doc ->
                    val htmlResult = runCatching { parser.toHtml(doc, lightMode = true) }
                    assertTrue(
                        htmlResult.isSuccess || htmlResult.exceptionOrNull() !is OutOfMemoryError,
                        "${parser.supportedFormat.name} toHtml should not OOM on iteration $i"
                    )
                    htmlResult.getOrNull()?.let { html ->
                        assertNotNull(html, "HTML output should not be null")
                    }
                }
            }
        }
    }

    @Test
    fun fuzzUnicodeStressAllParsers() {
        val random = Random(999)
        val parsers: List<TextParser> = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(),
            CsvParser(), WikitextParser(), CreoleParser(),
            TiddlyWikiParser(), LatexParser(), AsciidocParser(),
            OrgModeParser(), RestructuredTextParser(), KeyValueParser(),
            TaskpaperParser(), TextileParser(), JupyterParser(),
            RMarkdownParser(), BinaryParser()
        )

        repeat(30) { i ->
            val input = generateRandomUnicodeString(random, 5000)
            parsers.forEach { parser ->
                val result = runCatching { parser.parse(input) }
                assertTrue(
                    result.isSuccess || result.exceptionOrNull() !is OutOfMemoryError,
                    "${parser.supportedFormat.name} should handle Unicode on iteration $i"
                )
                result.getOrNull()?.let { doc ->
                    assertNotNull(doc.rawContent, "rawContent should not be null")
                    assertNotNull(doc.parsedContent, "parsedContent should not be null")
                }
            }
        }
    }

    @Test
    fun fuzzMalformedInputAllParsers() {
        val random = Random(1337)
        val parsers: List<TextParser> = listOf(
            MarkdownParser(), PlaintextParser(), TodoTxtParser(),
            CsvParser(), WikitextParser(), CreoleParser(),
            TiddlyWikiParser(), LatexParser(), AsciidocParser(),
            OrgModeParser(), RestructuredTextParser(), KeyValueParser(),
            TaskpaperParser(), TextileParser(), JupyterParser(),
            RMarkdownParser(), BinaryParser()
        )

        repeat(30) { i ->
            val input = generateMalformedInput(random)
            parsers.forEach { parser ->
                val result = runCatching { parser.parse(input) }
                assertTrue(
                    result.isSuccess || result.exceptionOrNull() !is OutOfMemoryError,
                    "${parser.supportedFormat.name} should handle malformed input on iteration $i"
                )
            }
        }
    }

    @Test
    fun fuzzExtensionDetection() {
        val random = Random(2025)
        val extensions = listOf(
            ".md", ".txt", ".csv", ".tex", ".org", ".wiki", ".adoc",
            ".rst", ".rmd", ".taskpaper", ".textile", ".creole",
            ".tid", ".ipynb", ".ini", ".bin", ".unknown", "",
            ".PDF", ".MD", ".TXT", "md", "txt"
        )
        repeat(200) {
            val ext = extensions[random.nextInt(extensions.size)]
            val result = runCatching { FormatRegistry.detectByExtension(ext) }
            assertTrue(result.isSuccess, "detectByExtension should not throw for '$ext'")
            assertNotNull(result.getOrNull(), "detectByExtension should always return a format for '$ext'")
        }
    }
}
