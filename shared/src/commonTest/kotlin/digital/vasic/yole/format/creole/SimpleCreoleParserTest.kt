/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test to verify Creole parser functionality
 *
 *########################################################*/
package digital.vasic.yole.format.creole

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleCreoleParserTest {

    private val parser = CreoleParser()

    @Test
    fun `test basic Creole parsing`() {
        val content = """
            = Main Heading
            
            This is a paragraph with **bold** and //italic// text.
            
            * List item 1
            * List item 2
            ** Nested item
            
            [[http://example.com|Example Link]]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Test HTML conversion
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assert(html.contains("<h1>Main Heading</h1>"))
        assert(html.contains("<strong>bold</strong>"))
        assert(html.contains("<em>italic</em>"))
        assert(html.contains("<ul>"))
        assert(html.contains("<a href='http://example.com'>Example Link</a>"))
    }

    @Test
    fun `test format detection`() {
        val format = FormatRegistry.getByExtension(".creole")
        assertNotNull(format)
        assertEquals(TextFormat.ID_CREOLE, format.id)
        assertEquals("Creole", format.name)
    }
}