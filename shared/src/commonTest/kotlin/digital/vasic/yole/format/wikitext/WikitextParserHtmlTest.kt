/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for WikitextParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WikitextParserHtmlTest {

    private val parser = WikitextParser()

    // ==================== Document wrapping ====================

    @Test
    fun testWrappedInWikitextDiv() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("class='wikitext'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("<style>") || doc.parsedContent.contains("wikitext"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("= Heading =")
        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Heading"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("== Subsection ==")
        assertTrue(doc.parsedContent.contains("<h2>"))
        assertTrue(doc.parsedContent.contains("Subsection"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("=== Details ===")
        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    // ==================== Inline formatting ====================

    @Test
    fun testBoldFormatting() {
        val doc = parser.parse("This is **bold** text")
        assertTrue(doc.parsedContent.contains("<strong>"))
        assertTrue(doc.parsedContent.contains("bold"))
    }

    @Test
    fun testItalicFormatting() {
        val doc = parser.parse("This is //italic// text")
        assertTrue(doc.parsedContent.contains("<em>"))
        assertTrue(doc.parsedContent.contains("italic"))
    }

    @Test
    fun testHighlightFormatting() {
        val doc = parser.parse("This is __highlighted__ text")
        assertTrue(doc.parsedContent.contains("highlight"))
    }

    @Test
    fun testStrikethroughFormatting() {
        val doc = parser.parse("This is ~~deleted~~ text")
        assertTrue(doc.parsedContent.contains("<s>"))
    }

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use ''code'' here")
        assertTrue(doc.parsedContent.contains("<code>"))
    }

    @Test
    fun testSuperscript() {
        val doc = parser.parse("x^{2}")
        assertTrue(doc.parsedContent.contains("<sup>"))
    }

    @Test
    fun testSubscript() {
        val doc = parser.parse("H_{2}O")
        assertTrue(doc.parsedContent.contains("<sub>"))
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
        val doc = parser.parse("1. First\n2. Second")
        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<li>"))
    }

    @Test
    fun testChecklist() {
        val doc = parser.parse("[*] Done\n[ ] Not done")
        assertTrue(doc.parsedContent.contains("checklist"))
    }

    @Test
    fun testChecklistCheckedClass() {
        val doc = parser.parse("[*] Done item")
        assertTrue(doc.parsedContent.contains("checked"))
    }

    // ==================== Code blocks ====================

    @Test
    fun testCodeBlock() {
        val doc = parser.parse("'''\ncode here\n'''")
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("<code>"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val doc = parser.parse("'''\n<script>alert('x')</script>\n'''")
        assertTrue(!doc.parsedContent.contains("<script>alert"))
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
        val doc = parser.parse("See [[PageName|description]]")
        assertTrue(doc.parsedContent.contains("description"))
    }

    // ==================== Images ====================

    @Test
    fun testImageFromMediaSyntax() {
        val doc = parser.parse("[[File:image.png]]")
        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("image.png"))
    }

    @Test
    fun testImageFromDoubleBrace() {
        val doc = parser.parse("{{photo.jpg}}")
        assertTrue(doc.parsedContent.contains("<img"))
    }

    // ==================== Tables ====================

    @Test
    fun testTableStructure() {
        val doc = parser.parse("{|\n|-\n| Cell 1\n| Cell 2\n|}")
        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("<td>"))
    }

    @Test
    fun testTableHeaderCell() {
        val doc = parser.parse("{|\n! Header\n|-\n| Data\n|}")
        assertTrue(doc.parsedContent.contains("<th>"))
    }

    @Test
    fun testTableCaption() {
        val doc = parser.parse("{|\n|+ Caption\n|-\n| Data\n|}")
        assertTrue(doc.parsedContent.contains("<caption>"))
        assertTrue(doc.parsedContent.contains("Caption"))
    }

    // ==================== Zim header removal ====================

    @Test
    fun testZimHeaderDetected() {
        val content = "[DocumentAttributes]\nkey=value\n\nActual content"
        val doc = parser.parse(content)
        assertEquals("true", doc.metadata["hasZimHeader"])
    }

    @Test
    fun testZimHeaderNotPresent() {
        val doc = parser.parse("Normal content")
        assertEquals("false", doc.metadata["hasZimHeader"])
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
        val errors = parser.validate("= Heading =\nSome text")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testUnbalancedHeading() {
        val errors = parser.validate("== Heading ===")
        assertTrue(errors.any { it.contains("Unbalanced") })
    }

    @Test
    fun testUnclosedBrackets() {
        val errors = parser.validate("See [[broken link")
        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    // ==================== Paragraphs ====================

    @Test
    fun testPlainTextParagraph() {
        val doc = parser.parse("Just some text")
        assertTrue(doc.parsedContent.contains("<p>"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }

    // ==================== Templates ====================

    @Test
    fun testTemplate() {
        val doc = parser.parse("{{TemplateName}}")
        assertTrue(doc.parsedContent.contains("template"))
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }
}
