/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test for KeyValue parser
 *
 *########################################################*/
package digital.vasic.yole.format.keyvalue

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleKeyValueTest {

    @Test
    fun `test basic KeyValue parsing`() {
        val parser = KeyValueParser()
        val content = """
            name=John Doe
            age=30
            city=New York
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("3", result.metadata["entries"])
        assertEquals("3", result.metadata["lines"])
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun `test format detection`() {
        val format = FormatRegistry.getByExtension(".ini")
        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
        assertEquals("Key-Value", format.name)
    }

    @Test
    fun `test INI format with sections`() {
        val parser = KeyValueParser()
        val content = """
            [Database]
            host=localhost
            port=5432
            
            [Server]
            host=0.0.0.0
            port=8080
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "config.ini"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("ini", result.metadata["type"])
        assertEquals(".ini", result.metadata["extension"])
        assertEquals("4", result.metadata["entries"])
        assertEquals("2", result.metadata["sections"])
    }

    @Test
    fun `test Java properties format`() {
        val parser = KeyValueParser()
        val content = """
            app.name=My Application
            app.version=1.0.0
            app.author=John Doe
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "app.properties"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("properties", result.metadata["type"])
        assertEquals(".properties", result.metadata["extension"])
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `test HTML conversion`() {
        val parser = KeyValueParser()
        val content = """
            name=John Doe
            age=30
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("<pre"))
        assertTrue(html.contains("font-weight: bold")) // Key highlighting
    }

    @Test
    fun `test validation`() {
        val parser = KeyValueParser()
        val content = """
            key1=value1
            key2=value2
        """.trimIndent()

        val errors = parser.validate(content)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `test validation with errors`() {
        val parser = KeyValueParser()
        val content = """
            key1=value1
            invalid_line_without_separator
            key2=value2
        """.trimIndent()

        val errors = parser.validate(content)
        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("No key-value separator found"))
    }
}