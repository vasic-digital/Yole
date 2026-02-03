/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge Case Stress Tests
 *
 * Comprehensive edge case and boundary condition tests
 * for format parsers and document processing.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

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
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Edge case and boundary condition stress tests.
 */
class EdgeCaseStressTest {

    // ==================== EMPTY INPUT HANDLING ====================

    @Test
    fun `all parsers handle empty string`() = runTest {
        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse("")
            assertNotNull(result, "${parser::class.simpleName} should handle empty string")
        }
    }

    @Test
    fun `all parsers handle whitespace only`() = runTest {
        val parsers = getAllParsers()
        val whitespaceInputs = listOf(" ", "  ", "\t", "\n", "\r\n", "   \t\n  ")

        parsers.forEach { parser ->
            whitespaceInputs.forEach { input ->
                val result = parser.parse(input)
                assertNotNull(result, "${parser::class.simpleName} should handle whitespace: '${input.replace("\n", "\\n")}'")
            }
        }
    }

    @Test
    fun `all parsers handle null bytes`() = runTest {
        val parsers = getAllParsers()
        val nullByteInput = "test\u0000content"

        parsers.forEach { parser ->
            val result = parser.parse(nullByteInput)
            assertNotNull(result, "${parser::class.simpleName} should handle null bytes")
        }
    }

    // ==================== VERY LONG INPUT HANDLING ====================

    @Test
    fun `parsers handle very long single line`() = runTest {
        val longLine = "a".repeat(100000)

        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse(longLine)
            assertNotNull(result, "${parser::class.simpleName} should handle very long line")
        }
    }

    @Test
    fun `parsers handle many short lines`() = runTest {
        val manyLines = (1..10000).joinToString("\n") { "line $it" }

        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse(manyLines)
            assertNotNull(result, "${parser::class.simpleName} should handle many lines")
        }
    }

    @Test
    fun `parsers handle very long words`() = runTest {
        val longWord = "supercalifragilisticexpialidocious".repeat(100)

        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse(longWord)
            assertNotNull(result, "${parser::class.simpleName} should handle very long word")
        }
    }

    // ==================== UNICODE EDGE CASES ====================

    @Test
    fun `parsers handle emoji`() = runTest {
        val emojiContent = "\uD83D\uDE00 Hello \uD83D\uDC4B World \uD83C\uDF0E"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(emojiContent)
            assertNotNull(result, "${parser::class.simpleName} should handle emoji")
        }
    }

    @Test
    fun `parsers handle right-to-left text`() = runTest {
        val rtlContent = "مرحبا بالعالم"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(rtlContent)
            assertNotNull(result, "${parser::class.simpleName} should handle RTL text")
        }
    }

    @Test
    fun `parsers handle mixed scripts`() = runTest {
        val mixedContent = "Hello 世界 مرحبا Привет こんにちは"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(mixedContent)
            assertNotNull(result, "${parser::class.simpleName} should handle mixed scripts")
        }
    }

    @Test
    fun `parsers handle combining characters`() = runTest {
        val combiningChars = "e\u0301" // é as e + combining accent

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(combiningChars)
            assertNotNull(result, "${parser::class.simpleName} should handle combining characters")
        }
    }

    @Test
    fun `parsers handle zero-width characters`() = runTest {
        val zeroWidthContent = "hello\u200Bworld" // zero-width space

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(zeroWidthContent)
            assertNotNull(result, "${parser::class.simpleName} should handle zero-width characters")
        }
    }

    // ==================== SPECIAL CHARACTER HANDLING ====================

    @Test
    fun `parsers handle control characters`() = runTest {
        val controlChars = "line1\u0001\u0002\u0003line2"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(controlChars)
            assertNotNull(result, "${parser::class.simpleName} should handle control characters")
        }
    }

    @Test
    fun `parsers handle backslash sequences`() = runTest {
        val backslashContent = "path\\to\\file\\name"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(backslashContent)
            assertNotNull(result, "${parser::class.simpleName} should handle backslash")
        }
    }

    @Test
    fun `parsers handle angle brackets`() = runTest {
        val angleContent = "<test> & <another>"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(angleContent)
            assertNotNull(result, "${parser::class.simpleName} should handle angle brackets")
        }
    }

    // ==================== LINE ENDING VARIATIONS ====================

    @Test
    fun `parsers handle Unix line endings`() = runTest {
        val unixContent = "line1\nline2\nline3"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(unixContent)
            assertNotNull(result, "${parser::class.simpleName} should handle Unix line endings")
        }
    }

    @Test
    fun `parsers handle Windows line endings`() = runTest {
        val windowsContent = "line1\r\nline2\r\nline3"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(windowsContent)
            assertNotNull(result, "${parser::class.simpleName} should handle Windows line endings")
        }
    }

    @Test
    fun `parsers handle old Mac line endings`() = runTest {
        val macContent = "line1\rline2\rline3"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(macContent)
            assertNotNull(result, "${parser::class.simpleName} should handle old Mac line endings")
        }
    }

    @Test
    fun `parsers handle mixed line endings`() = runTest {
        val mixedContent = "line1\nline2\r\nline3\rline4"

        val parsers = getAllParsers()

        parsers.forEach { parser ->
            val result = parser.parse(mixedContent)
            assertNotNull(result, "${parser::class.simpleName} should handle mixed line endings")
        }
    }

    // ==================== FORMAT-SPECIFIC EDGE CASES ====================

    @Test
    fun `markdown handles unclosed code blocks`() = runTest {
        val parser = MarkdownParser()

        val unclosedCode = "```kotlin\nfun test() {"

        val result = parser.parse(unclosedCode)
        assertNotNull(result)
    }

    @Test
    fun `markdown handles deeply nested lists`() = runTest {
        val parser = MarkdownParser()

        val deepList = (1..20).joinToString("\n") { "  ".repeat(it) + "- item $it" }

        val result = parser.parse(deepList)
        assertNotNull(result)
    }

    @Test
    fun `markdown handles broken links`() = runTest {
        val parser = MarkdownParser()

        val brokenLinks = "[link](broken\n[another](incomplete"

        val result = parser.parse(brokenLinks)
        assertNotNull(result)
    }

    @Test
    fun `csv handles quoted fields with special characters`() = runTest {
        val parser = CsvParser()

        val specialCsv = """
            "field,with,commas","field""with""quotes","field
            with
            newlines"
        """.trimIndent()

        val result = parser.parse(specialCsv)
        assertNotNull(result)
    }

    @Test
    fun `csv handles inconsistent column counts`() = runTest {
        val parser = CsvParser()

        val inconsistentCsv = """
            a,b,c
            1,2
            x,y,z,w
        """.trimIndent()

        val result = parser.parse(inconsistentCsv)
        assertNotNull(result)
    }

    @Test
    fun `todotxt handles malformed priorities`() = runTest {
        val parser = TodoTxtParser()

        val malformedContent = """
            (A) Valid task
            (Z) Another valid task
            (invalid) Should not match
            () Empty priority
            (A Missing close paren
        """.trimIndent()

        val result = parser.parse(malformedContent)
        assertNotNull(result)
    }

    @Test
    fun `todotxt handles dates in various formats`() = runTest {
        val parser = TodoTxtParser()

        val dateContent = """
            2024-01-15 Task with creation date
            x 2024-01-15 2024-01-10 Completed task
            (A) 2024-01-15 Priority with date
        """.trimIndent()

        val result = parser.parse(dateContent)
        assertNotNull(result)
    }

    @Test
    fun `latex handles unmatched braces`() = runTest {
        val parser = LatexParser()

        val unmatchedBraces = """
            \begin{document}
            \textbf{unclosed
            \end{document}
        """.trimIndent()

        val result = parser.parse(unmatchedBraces)
        assertNotNull(result)
    }

    @Test
    fun `keyvalue handles various formats`() = runTest {
        val parser = KeyValueParser()

        val mixedContent = """
            key1=value1
            key2: value2
            key3 = value3
            key4:value4
            # comment
            key5=value with spaces
        """.trimIndent()

        val result = parser.parse(mixedContent)
        assertNotNull(result)
    }

    @Test
    fun `orgmode handles complex hierarchies`() = runTest {
        val parser = OrgModeParser()

        val complexOrg = """
            * Level 1
            ** Level 2
            *** Level 3
            **** Level 4
            ***** Level 5
            * Back to 1
            ** Another 2
        """.trimIndent()

        val result = parser.parse(complexOrg)
        assertNotNull(result)
    }

    @Test
    fun `wikitext handles nested templates`() = runTest {
        val parser = WikitextParser()

        val nestedTemplates = """
            {{template1|{{template2|value}}}}
            [[Link|{{display}}]]
        """.trimIndent()

        val result = parser.parse(nestedTemplates)
        assertNotNull(result)
    }

    // ==================== BINARY DETECTION EDGE CASES ====================

    @Test
    fun `binary parser detects various signatures`() = runTest {
        val parser = BinaryParser()

        // PNG signature
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val pngString = pngBytes.decodeToString()
        val pngResult = parser.parse(pngString)
        assertNotNull(pngResult)

        // JPEG signature
        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val jpegString = jpegBytes.decodeToString()
        val jpegResult = parser.parse(jpegString)
        assertNotNull(jpegResult)
    }

    @Test
    fun `binary parser handles text files`() = runTest {
        val parser = BinaryParser()

        val textContent = "This is plain text content without binary signatures"
        val result = parser.parse(textContent)
        assertNotNull(result)
    }

    // ==================== CONCURRENT PARSING EDGE CASES ====================

    @Test
    fun `same parser instance used concurrently`() = runTest {
        val parser = MarkdownParser()

        val contents = (1..100).map { "# Heading $it\n\nContent for document $it" }

        val results = kotlinx.coroutines.async {
            contents.map { content ->
                kotlinx.coroutines.async {
                    parser.parse(content)
                }
            }.map { it.await() }
        }.await()

        assertEquals(100, results.size)
        assertTrue(results.all { it != null })
    }

    @Test
    fun `different parser types used concurrently`() = runTest {
        val parsers = getAllParsers()
        val content = "Test content for all parsers"

        val results = parsers.map { parser ->
            kotlinx.coroutines.async {
                parser.parse(content)
            }
        }.map { it.await() }

        assertEquals(parsers.size, results.size)
        assertTrue(results.all { it != null })
    }

    // ==================== BOUNDARY CONDITIONS ====================

    @Test
    fun `parsers handle max int length content`() = runTest {
        // Test with content near typical buffer sizes
        val bufferSizes = listOf(1024, 4096, 8192, 16384)

        val parser = PlaintextParser()

        bufferSizes.forEach { size ->
            val content = "x".repeat(size)
            val result = parser.parse(content)
            assertNotNull(result, "Should handle content of size $size")
        }
    }

    @Test
    fun `parsers handle content with many matches`() = runTest {
        val parser = MarkdownParser()

        // Many bold markers
        val manyBold = (1..1000).joinToString(" ") { "**bold$it**" }
        val result = parser.parse(manyBold)
        assertNotNull(result)
    }

    @Test
    fun `parsers handle alternating patterns`() = runTest {
        val parser = MarkdownParser()

        // Alternating emphasis
        val alternating = (1..500).joinToString(" ") { if (it % 2 == 0) "*italic*" else "**bold**" }
        val result = parser.parse(alternating)
        assertNotNull(result)
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun getAllParsers(): List<TextParser> = listOf(
        MarkdownParser(),
        PlaintextParser(),
        TodoTxtParser(),
        CsvParser(),
        LatexParser(),
        AsciidocParser(),
        OrgModeParser(),
        WikitextParser(),
        RestructuredTextParser(),
        TaskpaperParser(),
        TextileParser(),
        CreoleParser(),
        TiddlyWikiParser(),
        JupyterParser(),
        RMarkdownParser(),
        KeyValueParser(),
        BinaryParser()
    )
}
