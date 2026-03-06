/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TiddlyWikiParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.tiddlywiki

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TiddlyWikiParserHtmlTest {

    private val parser = TiddlyWikiParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInTiddlywikiDiv() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("class='tiddlywiki'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        assertTrue(doc.parsedContent.contains("<style>"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("! Title")
        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Title"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("!! Section")
        assertTrue(doc.parsedContent.contains("<h2>"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("!!! Subsection")
        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    // ==================== Inline formatting ====================

    @Test
    fun testBoldFormatting() {
        val doc = parser.parse("This is ''bold'' text")
        assertTrue(doc.parsedContent.contains("<strong>"))
    }

    @Test
    fun testItalicFormatting() {
        val doc = parser.parse("This is //italic// text")
        assertTrue(doc.parsedContent.contains("<em>"))
    }

    @Test
    fun testUnderlineFormatting() {
        val doc = parser.parse("This is __underlined__ text")
        assertTrue(doc.parsedContent.contains("<u>"))
    }

    @Test
    fun testStrikethroughFormatting() {
        val doc = parser.parse("This is ~~deleted~~ text")
        assertTrue(doc.parsedContent.contains("<s>"))
    }

    @Test
    fun testSuperscript() {
        val doc = parser.parse("x^^2^^")
        assertTrue(doc.parsedContent.contains("<sup>"))
    }

    @Test
    fun testSubscript() {
        val doc = parser.parse("H,,2,,O")
        assertTrue(doc.parsedContent.contains("<sub>"))
    }

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use `code` here")
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

    // ==================== Code blocks ====================

    @Test
    fun testCodeBlock() {
        val doc = parser.parse("```\ncode here\n```")
        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val doc = parser.parse("```\n<script>\n```")
        assertTrue(!doc.parsedContent.contains("<script>") || doc.parsedContent.contains("&lt;script&gt;"))
    }

    // ==================== Block quotes ====================

    @Test
    fun testBlockQuoteTriple() {
        val doc = parser.parse("<<<\nQuoted text\n<<<")
        assertTrue(doc.parsedContent.contains("<blockquote>"))
    }

    @Test
    fun testBlockQuoteSingleLine() {
        val doc = parser.parse("> A quote")
        assertTrue(doc.parsedContent.contains("<blockquote>"))
    }

    // ==================== Horizontal rule ====================

    @Test
    fun testHorizontalRule() {
        val doc = parser.parse("---")
        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    // ==================== Links ====================

    @Test
    fun testInternalLink() {
        val doc = parser.parse("See [[PageName]]")
        assertTrue(doc.parsedContent.contains("<a"))
        assertTrue(doc.parsedContent.contains("PageName"))
    }

    @Test
    fun testLinkWithDescription() {
        val doc = parser.parse("See [[Page|Description]]")
        assertTrue(doc.parsedContent.contains("Description"))
    }

    @Test
    fun testExternalLink() {
        val doc = parser.parse("[ext[https://example.com]]")
        assertTrue(doc.parsedContent.contains("target='_blank'"))
    }

    // ==================== Images ====================

    @Test
    fun testImage() {
        val doc = parser.parse("[img[photo.jpg]]")
        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("photo.jpg"))
    }

    // ==================== Metadata ====================

    @Test
    fun testTitleMetadata() {
        val doc = parser.parse("title: My Tiddler\n\nContent here")
        assertEquals("My Tiddler", doc.metadata["title"])
    }

    @Test
    fun testTagsMetadata() {
        val doc = parser.parse("tags: tag1 tag2\n\nContent")
        assertTrue(doc.metadata["tags"]?.contains("tag1") == true)
    }

    @Test
    fun testMetadataDisplay() {
        val doc = parser.parse("title: My Tiddler\ntags: important\n\nContent")
        assertTrue(doc.parsedContent.contains("metadata"))
    }

    // ==================== Paragraphs ====================

    @Test
    fun testParagraph() {
        val doc = parser.parse("Just some text")
        assertTrue(doc.parsedContent.contains("<p>"))
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("! Heading\nSome text")
        assertTrue(errors.isEmpty())
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
