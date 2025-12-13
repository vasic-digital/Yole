/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test for PlainText parser
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimplePlainTextTest {

    @Test
    fun `test basic PlainText parsing`() {
        val parser = PlaintextParser()
        val content = """
            This is a simple plain text document.
            It contains multiple lines of text.
            Each line represents content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("plain", result.metadata["type"])
        assertEquals("3", result.metadata["lines"])
    }

    @Test
    fun `test format detection`() {
        val format = FormatRegistry.getByExtension(".txt")
        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
        assertEquals("Plain Text", format.name)
    }

    @Test
    fun `test PlainText with different extensions`() {
        val parser = PlaintextParser()
        val content = "Application Log File"

        val result = parser.parse(content, mapOf("filename" to "app.log"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("plain", result.metadata["type"])
        assertEquals(".log", result.metadata["extension"])
        assertEquals("1", result.metadata["lines"])
    }

    @Test
    fun `test HTML conversion`() {
        val parser = PlaintextParser()
        val content = "Simple plain text content"

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"))
        assertTrue(html.contains("<pre"))
        assertTrue(html.contains("font-family: monospace"))
    }
}