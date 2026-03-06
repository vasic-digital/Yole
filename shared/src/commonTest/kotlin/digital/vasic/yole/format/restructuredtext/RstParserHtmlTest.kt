/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for RestructuredTextParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.restructuredtext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RstParserHtmlTest {

    private val parser = RestructuredTextParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInRstDocumentDiv() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-document"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    // ==================== Sections ====================

    @Test
    fun testLevel1Section() {
        val doc = parser.parse("Title\n=====")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-section-1"))
    }

    @Test
    fun testLevel2Section() {
        val doc = parser.parse("Section\n-------")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-section-2"))
    }

    @Test
    fun testLevel3Section() {
        val doc = parser.parse("Subsection\n~~~~~~~~~~")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-section-3"))
    }

    @Test
    fun testSectionTitleContent() {
        val doc = parser.parse("My Title\n========")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("My Title"))
    }

    // ==================== Directives ====================

    @Test
    fun testDirectiveRendered() {
        val doc = parser.parse(".. note::\n   This is a note")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-directive"))
    }

    @Test
    fun testDirectiveHeader() {
        val doc = parser.parse(".. warning::\n   Be careful")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-directive-header"))
    }

    @Test
    fun testDirectiveContent() {
        val doc = parser.parse(".. note::\n   Content here")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("rst-directive-content"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataSections() {
        val doc = parser.parse("Title\n=====\nSubtitle\n--------")
        assertEquals("2", doc.metadata["sections"])
    }

    @Test
    fun testMetadataDirectives() {
        val doc = parser.parse(".. note::\n   A note\n\n.. warning::\n   A warning")
        assertEquals("2", doc.metadata["directives"])
    }

    @Test
    fun testMetadataMaxLevel() {
        val doc = parser.parse("Title\n=====\nSub\n---\nDetail\n~~~~~~")
        assertEquals("3", doc.metadata["max_level"])
    }

    // ==================== Paragraphs ====================

    @Test
    fun testParagraphGeneration() {
        val doc = parser.parse("Just some text")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<p>"))
    }

    // ==================== Light/dark mode ====================

    @Test
    fun testLightModeClass() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("light"))
    }

    @Test
    fun testDarkModeClass() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("dark"))
    }

    @Test
    fun testLightAndDarkModeDiffer() {
        val doc = parser.parse("Hello")
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark)
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("Title\n=====\nContent")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testShortUnderline() {
        val errors = parser.validate("Long Title\n===")
        assertTrue(errors.any { it.contains("too short") })
    }

    // ==================== HTML escaping ====================

    @Test
    fun testHtmlEscaping() {
        val doc = parser.parse("Use <tag> here")
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("&lt;tag&gt;"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("0", doc.metadata["sections"])
    }

    // ==================== canParse ====================

    @Test
    fun testCanParse() {
        assertTrue(parser.canParse(parser.supportedFormat))
    }
}
