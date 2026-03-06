/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TextileParser HTML generation and validation
 *
 *########################################################*/
package digital.vasic.yole.format.textile

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class TextileParserHtmlTest {

    private val parser = TextileParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInTextileDiv() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("class='textile'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("<style>"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("h1. Title")
        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Title"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("h2. Section")
        assertTrue(doc.parsedContent.contains("<h2>"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("h3. Subsection")
        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    // ==================== Inline formatting ====================

    @Test
    fun testBoldFormatting() {
        val doc = parser.parse("This is *bold* text")
        assertTrue(doc.parsedContent.contains("<b>"))
    }

    @Test
    fun testStrongEmphasis() {
        val doc = parser.parse("This is **strong** text")
        assertTrue(doc.parsedContent.contains("<strong>"))
    }

    @Test
    fun testItalicFormatting() {
        val doc = parser.parse("This is _italic_ text")
        assertTrue(doc.parsedContent.contains("<em>"))
    }

    @Test
    fun testStrikethrough() {
        val doc = parser.parse("This is -deleted- text")
        assertTrue(doc.parsedContent.contains("<s>"))
    }

    @Test
    fun testSuperscript() {
        val doc = parser.parse("x^2^")
        assertTrue(doc.parsedContent.contains("<sup>"))
    }

    @Test
    fun testSubscript() {
        val doc = parser.parse("H~2~O")
        assertTrue(doc.parsedContent.contains("<sub>"))
    }

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use @code@ here")
        assertTrue(doc.parsedContent.contains("<code>"))
    }

    // ==================== Blockquotes ====================

    @Test
    fun testBlockquote() {
        val doc = parser.parse("bq. A famous quote")
        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("A famous quote"))
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

    // ==================== Pre-formatted blocks ====================

    @Test
    fun testPreBlock() {
        val doc = parser.parse("pre. Some preformatted text\nmore text\n")
        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    // ==================== Links ====================

    @Test
    fun testLink() {
        val doc = parser.parse("Visit \"Google\":https://google.com today")
        assertTrue(doc.parsedContent.contains("<a"))
        assertTrue(doc.parsedContent.contains("Google"))
    }

    // ==================== Images ====================

    @Test
    fun testImage() {
        val doc = parser.parse("!photo.jpg!")
        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("photo.jpg"))
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

    @Test
    fun testMetadataEmptyContent() {
        val doc = parser.parse("")
        assertEquals("0", doc.metadata["lines"])
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("h1. Title\nSome text")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testInvalidHeadingLevel() {
        val errors = parser.validate("h8. Bad heading")
        assertTrue(errors.any { it.contains("Invalid heading level") })
    }

    @Test
    fun testUnclosedInlineCode() {
        val errors = parser.validate("Use @code here")
        assertTrue(errors.any { it.contains("Unclosed inline code") })
    }

    @Test
    fun testUnclosedImage() {
        val errors = parser.validate("See !image.jpg here")
        assertTrue(errors.any { it.contains("Unclosed image") })
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }
}
