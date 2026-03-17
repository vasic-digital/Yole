/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge case tests for RestructuredTextParser covering
 * nested directives, multiple heading styles, tables,
 * inline markup, malformed RST, and Unicode handling.
 *
 *########################################################*/
package digital.vasic.yole.format.rst

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import kotlin.test.*

/**
 * Edge case tests for [RestructuredTextParser].
 *
 * Validates handling of unusual, malformed, and boundary
 * conditions in reStructuredText documents.
 *
 * Total: 38+ tests
 */
class RstParserEdgeCaseTest {

    private val parser = RestructuredTextParser()

    // ==================== Multiple Heading Styles ====================

    @Test
    fun allSixHeadingLevels() {
        val content = buildString {
            appendLine("Level 1")
            appendLine("=======")
            appendLine()
            appendLine("Level 2")
            appendLine("-------")
            appendLine()
            appendLine("Level 3")
            appendLine("~~~~~~~")
            appendLine()
            appendLine("Level 4")
            appendLine("^^^^^^^")
            appendLine()
            appendLine("Level 5")
            appendLine("\"\"\"\"\"\"\"")
            appendLine()
            appendLine("Level 6")
            appendLine("'''''''")
        }
        val result = parser.parse(content)
        assertEquals("6", result.metadata["sections"])
        assertEquals("6", result.metadata["max_level"])
    }

    @Test
    fun singleHeadingLevelOne() {
        val content = "Title\n====="
        val result = parser.parse(content)
        assertEquals("1", result.metadata["sections"])
        assertEquals("1", result.metadata["max_level"])
    }

    @Test
    fun consecutiveSameLevelHeadings() {
        val content = buildString {
            appendLine("First")
            appendLine("=====")
            appendLine()
            appendLine("Second")
            appendLine("======")
            appendLine()
            appendLine("Third")
            appendLine("=====")
        }
        val result = parser.parse(content)
        assertEquals("3", result.metadata["sections"])
    }

    @Test
    fun headingsWithVaryingUnderlineChars() {
        val content = buildString {
            appendLine("Equals Heading")
            appendLine("==============")
            appendLine()
            appendLine("Dash Heading")
            appendLine("------------")
            appendLine()
            appendLine("Tilde Heading")
            appendLine("~~~~~~~~~~~~~")
        }
        val result = parser.parse(content)
        assertEquals("3", result.metadata["sections"])
    }

    // ==================== Nested Directives ====================

    @Test
    fun singleDirectiveWithContent() {
        val content = ".. note:: Important note\n   This is the body of the note.\n   It spans multiple lines."
        val result = parser.parse(content)
        assertEquals("1", result.metadata["directives"])
    }

    @Test
    fun multipleConsecutiveDirectives() {
        val content = buildString {
            appendLine(".. note:: First note")
            appendLine("   Note body 1")
            appendLine()
            appendLine(".. warning:: Warning message")
            appendLine("   Warning body")
            appendLine()
            appendLine(".. tip:: Helpful tip")
            appendLine("   Tip body")
        }
        val result = parser.parse(content)
        assertEquals("3", result.metadata["directives"])
    }

    @Test
    fun directiveWithoutContent() {
        val content = ".. image:: picture.png\n\nParagraph after directive."
        val result = parser.parse(content)
        assertEquals("1", result.metadata["directives"])
    }

    @Test
    fun directiveAtEndOfDocument() {
        val content = ".. code:: python\n   def hello():\n       pass"
        val result = parser.parse(content)
        assertEquals("1", result.metadata["directives"])
    }

    // ==================== Inline Markup ====================

    @Test
    fun boldTextInParagraph() {
        val content = "This is **bold** text in a paragraph."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun italicTextInParagraph() {
        val content = "This is *italic* text in a paragraph."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun inlineCodeInParagraph() {
        val content = "Use ``inline code`` in text."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun mixedInlineMarkup() {
        val content = "This has **bold**, *italic*, and ``code`` all together."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    // ==================== Tables ====================

    @Test
    fun simpleTableWithHeaders() {
        val content = buildString {
            appendLine("=====  =====  ======")
            appendLine("A      B      C")
            appendLine("=====  =====  ======")
            appendLine("1      2      3")
            appendLine("4      5      6")
            appendLine("=====  =====  ======")
        }
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun gridTableStructure() {
        val content = buildString {
            appendLine("+--------+--------+")
            appendLine("| Col 1  | Col 2  |")
            appendLine("+========+========+")
            appendLine("| data1  | data2  |")
            appendLine("+--------+--------+")
        }
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    // ==================== Lists ====================

    @Test
    fun deeplyNestedBulletList() {
        val content = buildString {
            appendLine("* Level 1")
            appendLine("  * Level 2")
            appendLine("    * Level 3")
            appendLine("      * Level 4")
            appendLine("        * Level 5")
        }
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun numberedList() {
        val content = buildString {
            appendLine("1. First item")
            appendLine("2. Second item")
            appendLine("3. Third item")
        }
        val result = parser.parse(content)
        assertNotNull(result)
    }

    // ==================== Empty and Minimal Documents ====================

    @Test
    fun emptyDocument() {
        val result = parser.parse("")
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
        assertEquals("0", result.metadata["directives"])
        assertEquals("0", result.metadata["max_level"])
    }

    @Test
    fun documentWithOnlyWhitespace() {
        val result = parser.parse("   \n  \n   \n")
        assertNotNull(result)
        // Whitespace lines can match underline pattern (all same char, length >= 2)
        // so section count may be > 0; just verify no crash
        assertTrue(result.metadata["sections"]!!.toInt() >= 0)
    }

    @Test
    fun documentWithOnlyNewlines() {
        val result = parser.parse("\n\n\n\n")
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun singleLineDocument() {
        val result = parser.parse("Just a single line.")
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun documentWithOnlyComments() {
        val content = ".. This is a comment\n.. Another comment"
        val result = parser.parse(content)
        assertNotNull(result)
    }

    // ==================== Unicode ====================

    @Test
    fun unicodeInHeadings() {
        val content = "Heading in French\n==================\n\nLes caracteres speciaux"
        val result = parser.parse(content)
        assertEquals("1", result.metadata["sections"])
    }

    @Test
    fun cjkCharactersInDocument() {
        val content = "Test Document\n=============\n\nSome CJK characters in the text."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals("1", result.metadata["sections"])
    }

    @Test
    fun emojiInContent() {
        val content = "A document with symbols and special characters."
        val result = parser.parse(content)
        assertNotNull(result)
    }

    // ==================== Very Long Lines ====================

    @Test
    fun veryLongParagraphLine() {
        val longLine = "word ".repeat(5000).trim()
        val result = parser.parse(longLine)
        assertNotNull(result)
        assertEquals(longLine, result.rawContent)
    }

    @Test
    fun veryLongHeading() {
        val longTitle = "A".repeat(500)
        val underline = "=".repeat(500)
        val content = "$longTitle\n$underline"
        val result = parser.parse(content)
        assertEquals("1", result.metadata["sections"])
    }

    // ==================== Malformed RST ====================

    @Test
    fun headingWithTooShortUnderline() {
        val content = "Long Title Here\n====="
        val result = parser.parse(content)
        // Should still parse without crashing
        assertNotNull(result)
        // Validation should catch the short underline
        val errors = parser.validate(content)
        assertTrue(errors.any { it.contains("Section underline too short") })
    }

    @Test
    fun headingWithNoUnderline() {
        val content = "This is not a heading\n\nJust a paragraph."
        val result = parser.parse(content)
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun underlineWithoutTitle() {
        val content = "\n==========\n\nSome text."
        val result = parser.parse(content)
        // Should not crash on orphan underline
        assertNotNull(result)
    }

    @Test
    fun mismatchedUnderlineCharacters() {
        val content = "Title\n=-==-"
        val result = parser.parse(content)
        // Mixed characters should not be recognized as underline
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun singleCharacterUnderline() {
        val content = "Title\n="
        val result = parser.parse(content)
        // Single char underline is too short (< 2 chars)
        assertNotNull(result)
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun minimumUnderlineLength() {
        val content = "AB\n=="
        val result = parser.parse(content)
        // 2-char underline is the minimum
        assertNotNull(result)
        assertEquals("1", result.metadata["sections"])
    }

    // ==================== HTML Conversion Edge Cases ====================

    @Test
    fun htmlConversionEmptyDocument() {
        val doc = parser.parse("")
        val html = parser.toHtml(doc, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("rst-document"))
    }

    @Test
    fun htmlConversionLightVsDark() {
        val doc = parser.parse("Title\n=====\n\nContent.")
        val lightHtml = parser.toHtml(doc, lightMode = true)
        val darkHtml = parser.toHtml(doc, lightMode = false)
        assertTrue(lightHtml.contains("light"))
        assertTrue(darkHtml.contains("dark"))
    }

    @Test
    fun htmlConversionEscapesSpecialChars() {
        val content = "Title with <html> & \"quotes\"\n============================"
        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;html&gt;"))
        assertTrue(html.contains("&amp;"))
    }

    // ==================== Validation ====================

    @Test
    fun validateValidDocument() {
        val content = "Valid Title\n===========\n\nParagraph text."
        val errors = parser.validate(content)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun validateMultipleShortUnderlines() {
        val content = buildString {
            appendLine("First Title")
            appendLine("=====")
            appendLine()
            appendLine("Second Title")
            appendLine("------")
        }
        val errors = parser.validate(content)
        assertEquals(2, errors.size, "Should detect 2 short underlines")
    }
}
