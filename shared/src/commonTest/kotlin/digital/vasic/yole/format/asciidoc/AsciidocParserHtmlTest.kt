/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for AsciidocParser HTML generation and validation
 *
 *########################################################*/
package digital.vasic.yole.format.asciidoc

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AsciidocParserHtmlTest {

    private val parser = AsciidocParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInAsciidocDiv() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("class='asciidoc'"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    // ==================== Headings ====================

    @Test
    fun testH1Heading() {
        val doc = parser.parse("= Title")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("Title"))
    }

    @Test
    fun testH2Heading() {
        val doc = parser.parse("== Section")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("Section"))
    }

    @Test
    fun testH3Heading() {
        val doc = parser.parse("=== Subsection")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<h3>"))
    }

    // ==================== Admonitions ====================

    @Test
    fun testNoteAdmonition() {
        val doc = parser.parse("NOTE: Remember this")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("admonition"))
        assertTrue(html.contains("admonition-note"))
    }

    @Test
    fun testWarningAdmonition() {
        val doc = parser.parse("WARNING: Be careful")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("admonition-warning"))
    }

    @Test
    fun testTipAdmonition() {
        val doc = parser.parse("TIP: Try this")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("admonition-tip"))
    }

    @Test
    fun testImportantAdmonition() {
        val doc = parser.parse("IMPORTANT: Read this")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("admonition-important"))
    }

    @Test
    fun testCautionAdmonition() {
        val doc = parser.parse("CAUTION: Watch out")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("admonition-caution"))
    }

    // ==================== Lists ====================

    @Test
    fun testUnorderedList() {
        val doc = parser.parse("* Item one")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>"))
    }

    @Test
    fun testOrderedList() {
        val doc = parser.parse(". First item")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>"))
    }

    // ==================== Code blocks ====================

    @Test
    fun testCodeBlockWithDashes() {
        val doc = parser.parse("----\ncode here\n----")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("<code>"))
    }

    @Test
    fun testCodeBlockWithDots() {
        val doc = parser.parse("....\nliteral block\n....")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<pre>"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val doc = parser.parse("----\n<script>\n----")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(!html.contains("<script>") || html.contains("&lt;script&gt;"))
    }

    // ==================== Comment blocks ====================

    @Test
    fun testCommentBlockHidden() {
        val doc = parser.parse("////\nThis is hidden\n////")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(!html.contains("This is hidden"))
    }

    // ==================== Paragraphs ====================

    @Test
    fun testParagraphGeneration() {
        val doc = parser.parse("Just some text here")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<p>"))
    }

    // ==================== Metadata ====================

    @Test
    fun testTitleMetadata() {
        val doc = parser.parse("= My Document")
        assertEquals("My Document", doc.metadata["title"])
    }

    @Test
    fun testAttributeMetadata() {
        val doc = parser.parse(":author: John Doe")
        assertEquals("John Doe", doc.metadata["author"])
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val doc = parser.parse("= Title\nSome text")
        assertTrue(doc.errors.isEmpty())
    }

    @Test
    fun testUnclosedCodeBlock() {
        val doc = parser.parse("----\ncode here")
        assertTrue(doc.errors.any { it.contains("Unclosed code block") })
    }

    @Test
    fun testUnclosedCommentBlock() {
        val doc = parser.parse("////\nhidden")
        assertTrue(doc.errors.any { it.contains("Unclosed comment block") })
    }

    @Test
    fun testMalformedInclude() {
        val doc = parser.parse("include::file.adoc")
        assertTrue(doc.errors.any { it.contains("Malformed include") })
    }

    // ==================== Light/dark mode ====================

    @Test
    fun testLightAndDarkModeDiffer() {
        val doc = parser.parse("= Title")
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark)
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertTrue(doc.errors.isEmpty())
    }

    // ==================== HTML escaping ====================

    @Test
    fun testParagraphEscapesHtml() {
        val doc = parser.parse("Use <tag> here")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;tag&gt;"))
    }
}
