/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for CreoleParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.creole

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreoleParserHtmlTest {

    private val parser = CreoleParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInCreoleDiv() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("class='creole'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("<style>"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("= Title")
        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Title"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("== Section")
        assertTrue(doc.parsedContent.contains("<h2>"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("=== Subsection")
        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    // ==================== Inline formatting ====================

    @Test
    fun testBoldFormatting() {
        val doc = parser.parse("This is **bold** text")
        assertTrue(doc.parsedContent.contains("<strong>"))
    }

    @Test
    fun testItalicFormatting() {
        val doc = parser.parse("This is //italic// text")
        assertTrue(doc.parsedContent.contains("<em>"))
    }

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use {{{code}}} here")
        assertTrue(doc.parsedContent.contains("<code>"))
    }

    // ==================== Lists ====================

    @Test
    fun testUnorderedList() {
        val doc = parser.parse("* Item one\n* Item two")
        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<li>"))
    }

    @Test
    fun testOrderedList() {
        val doc = parser.parse("# First\n# Second")
        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<li>"))
    }

    @Test
    fun testNestedUnorderedList() {
        val doc = parser.parse("* Item\n** Nested")
        // Should have nested <ul> tags
        val ulCount = "<ul>".toRegex().findAll(doc.parsedContent).count()
        assertTrue(ulCount >= 2)
    }

    // ==================== Tables ====================

    @Test
    fun testTableStructure() {
        val doc = parser.parse("|Cell 1|Cell 2|")
        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("<td>"))
    }

    @Test
    fun testTableHeaderCell() {
        val doc = parser.parse("|=Header 1|=Header 2|")
        assertTrue(doc.parsedContent.contains("<th>"))
    }

    // ==================== Code blocks ====================

    @Test
    fun testCodeBlock() {
        val doc = parser.parse("{{{\ncode here\n}}}")
        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val doc = parser.parse("{{{\n<script>\n}}}")
        assertTrue(!doc.parsedContent.contains("<script>") || doc.parsedContent.contains("&lt;script&gt;"))
    }

    // ==================== Horizontal rule ====================

    @Test
    fun testHorizontalRule() {
        val doc = parser.parse("----")
        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    // ==================== Links ====================

    @Test
    fun testSimpleLink() {
        val doc = parser.parse("See [[PageName]]")
        assertTrue(doc.parsedContent.contains("<a"))
        assertTrue(doc.parsedContent.contains("PageName"))
    }

    @Test
    fun testLinkWithDescription() {
        val doc = parser.parse("See [[http://example.com|Example]]")
        assertTrue(doc.parsedContent.contains("Example"))
    }

    // ==================== Images ====================

    @Test
    fun testImage() {
        val doc = parser.parse("{{photo.jpg}}")
        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("photo.jpg"))
    }

    @Test
    fun testImageWithAlt() {
        val doc = parser.parse("{{photo.jpg|My Photo}}")
        assertTrue(doc.parsedContent.contains("My Photo"))
    }

    // ==================== Line breaks ====================

    @Test
    fun testLineBreak() {
        val doc = parser.parse("Line one\\\\Line two")
        assertTrue(doc.parsedContent.contains("<br>"))
    }

    // ==================== Paragraphs ====================

    @Test
    fun testParagraph() {
        val doc = parser.parse("Just some text")
        assertTrue(doc.parsedContent.contains("<p>"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataLineCount() {
        val doc = parser.parse("Line 1\nLine 2\nLine 3")
        assertEquals("3", doc.metadata["lines"])
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("= Heading\nSome text")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testMalformedTableRow() {
        val errors = parser.validate("|Cell without closing")
        assertTrue(errors.any { it.contains("Malformed table row") })
    }

    @Test
    fun testUnclosedCodeBlock() {
        val errors = parser.validate("{{{\ncode")
        assertTrue(errors.any { it.contains("Unclosed") })
    }

    @Test
    fun testUnclosedBrackets() {
        val errors = parser.validate("[[broken link")
        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }
}
