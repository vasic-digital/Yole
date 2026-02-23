/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test for Textile parser
 *
 *########################################################*/
package digital.vasic.yole.format.textile

import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple unit tests for Textile format parser to verify basic functionality.
 */
class TextileParserSimpleTest {

    private val parser = TextileParser()

    @Test
    fun `should parse basic Textile document`() {
        val content = """
            h1. Main Heading
            
            This is a paragraph in the document.
            
            h2. Subsection
            
            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("", result.metadata["extension"])
        assertEquals("7", result.metadata["lines"])
    }

    @Test
    fun `should parse Textile headings`() {
        val content = """
            h1. Level 1 Heading
            h2. Level 2 Heading
            h3. Level 3 Heading
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals("3", result.metadata["lines"])
    }

    @Test
    fun `should parse bold text`() {
        val content = "This is *bold* text."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse italic text`() {
        val content = "This is _italic_ text."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse lists`() {
        val content = """
            * First item
            * Second item
            * Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should convert to HTML`() {
        val content = "h1. Test Document"
        
        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='textile'>"))
        assertTrue(html.contains("<h1>Test Document</h1>"))
    }

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["lines"])
    }
}