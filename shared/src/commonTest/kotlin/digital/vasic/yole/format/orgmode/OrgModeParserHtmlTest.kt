/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for OrgModeParser HTML generation and metadata
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrgModeParserHtmlTest {

    private val parser = OrgModeParser()

    // ==================== Headings ====================

    @Test
    fun testLevel1Heading() {
        val doc = parser.parse("* Introduction")
        assertTrue(doc.parsedContent.contains("org-heading-1"))
    }

    @Test
    fun testLevel2Heading() {
        val doc = parser.parse("** Subsection")
        assertTrue(doc.parsedContent.contains("org-heading-2"))
    }

    @Test
    fun testLevel3Heading() {
        val doc = parser.parse("*** Details")
        assertTrue(doc.parsedContent.contains("org-heading-3"))
    }

    @Test
    fun testHeadingCountMetadata() {
        val content = "* One\n** Two\n* Three"
        val doc = parser.parse(content)
        // Without metadata header, counts all headings
        assertEquals("3", doc.metadata["headings"])
    }

    @Test
    fun testMaxLevelMetadata() {
        val content = "* Level 1\n** Level 2\n*** Level 3"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["max_level"])
    }

    // ==================== TODO states ====================

    @Test
    fun testTodoState() {
        val doc = parser.parse("* TODO Buy groceries")
        assertTrue(doc.parsedContent.contains("org-todo-todo"))
    }

    @Test
    fun testDoneState() {
        val doc = parser.parse("* DONE Complete report")
        assertTrue(doc.parsedContent.contains("org-todo-done"))
    }

    @Test
    fun testTodoCountMetadata() {
        val content = "* TODO Task 1\n* DONE Task 2\n* TODO Task 3"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["todos"])
    }

    // ==================== Properties ====================

    @Test
    fun testPropertiesRendered() {
        val content = ":PROPERTIES:\n:CREATED: 2026-01-01\n:END:"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("org-properties"))
    }

    @Test
    fun testPropertyKeyRendered() {
        val content = ":PROPERTIES:\n:STATUS: active\n:END:"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("org-property-key"))
    }

    @Test
    fun testPropertyValueRendered() {
        val content = ":PROPERTIES:\n:STATUS: active\n:END:"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("org-property-value"))
    }

    @Test
    fun testPropertiesCountMetadata() {
        val content = ":PROPERTIES:\n:A: 1\n:B: 2\n:END:"
        val doc = parser.parse(content)
        assertEquals("2", doc.metadata["properties"])
    }

    // ==================== Code blocks ====================

    @Test
    fun testCodeBlockRendered() {
        val content = "#+BEGIN_SRC python\nprint('hi')\n#+END_SRC"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("org-block"))
    }

    @Test
    fun testCodeBlockHeader() {
        val content = "#+BEGIN_SRC python\nprint('hi')\n#+END_SRC"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("org-block-header"))
    }

    // ==================== Inline formatting ====================

    @Test
    fun testBoldFormatting() {
        val doc = parser.parse("This is **bold** text")
        assertTrue(doc.parsedContent.contains("org-bold"))
    }

    @Test
    fun testItalicFormatting() {
        val doc = parser.parse("This is /italic/ text")
        assertTrue(doc.parsedContent.contains("org-italic"))
    }

    @Test
    fun testUnderlineFormatting() {
        val doc = parser.parse("This is _underline_ text")
        assertTrue(doc.parsedContent.contains("org-underline"))
    }

    @Test
    fun testStrikethroughFormatting() {
        val doc = parser.parse("This is +strikethrough+ text")
        assertTrue(doc.parsedContent.contains("org-strikethrough"))
    }

    @Test
    fun testVerbatimFormatting() {
        val doc = parser.parse("This is =\"verbatim=\" text")
        assertTrue(doc.parsedContent.contains("org-verbatim"))
    }

    // ==================== Theme classes ====================

    @Test
    fun testLightModeHasLightClass() {
        val doc = parser.parse("* Heading")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("light"))
    }

    @Test
    fun testDarkModeHasDarkClass() {
        val doc = parser.parse("* Heading")
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("dark"))
    }

    @Test
    fun testLightAndDarkModeDiffer() {
        val doc = parser.parse("* Heading")
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark)
    }

    @Test
    fun testToHtmlIncludesStyleSheet() {
        val doc = parser.parse("* Heading")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    // ==================== Document metadata headers ====================

    @Test
    fun testDocumentWithMetadataCountsOnlyLevel1() {
        val content = "#+TITLE: My Doc\n* Section 1\n** Sub 1\n* Section 2"
        val doc = parser.parse(content)
        assertEquals("2", doc.metadata["headings"]) // Only level-1 headings counted
    }

    @Test
    fun testDocumentWithoutMetadataCountsAll() {
        val content = "* Section 1\n** Sub 1\n* Section 2"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["headings"]) // All headings counted
    }

    // ==================== Edge cases ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("0", doc.metadata["headings"])
        assertEquals("0", doc.metadata["todos"])
        assertEquals("0", doc.metadata["properties"])
        assertEquals("0", doc.metadata["max_level"])
    }

    @Test
    fun testContentWithOnlyText() {
        val doc = parser.parse("Just some plain text here")
        assertEquals("0", doc.metadata["headings"])
    }

    @Test
    fun testCanParse() {
        val format = parser.supportedFormat
        assertTrue(parser.canParse(format))
    }

    @Test
    fun testWrappedInOrgModeDocument() {
        val doc = parser.parse("* Heading")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("org-mode-document"))
    }
}
