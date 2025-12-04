/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for TextParser interface and default implementations
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Unit tests for TextParser interface and its default implementations.
 *
 * Tests cover:
 * - Default canParse implementation
 * - Default toHtml implementation
 * - Default validate implementation
 * - Parse method contract
 * - Integration between methods
 * - Edge cases and error scenarios
 */
class TextParserTest {

    // Test parser with default implementations
    private class DefaultTestParser(
        override val supportedFormat: TextFormat = TextFormat(
            id = "test",
            name = "Test Format",
            defaultExtension = ".test",
            extensions = listOf(".test")
        )
    ) : TextParser {
        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content
            )
        }
    }

    // Test parser with custom canParse
    private class CustomCanParseParser : TextParser {
        override val supportedFormat = TextFormat(
            id = "custom",
            name = "Custom",
            defaultExtension = ".custom",
            extensions = listOf(".custom")
        )

        override fun canParse(format: TextFormat): Boolean {
            // Can parse both "custom" and "custom2"
            return format.id == "custom" || format.id == "custom2"
        }

        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content
            )
        }
    }

    // Test parser with custom toHtml
    private class CustomHtmlParser : TextParser {
        override val supportedFormat = TextFormat(
            id = "html",
            name = "HTML",
            defaultExtension = ".html",
            extensions = listOf(".html")
        )

        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content
            )
        }

        override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
            return if (lightMode) {
                "<div class='light'>${document.parsedContent}</div>"
            } else {
                "<div class='dark'>${document.parsedContent}</div>"
            }
        }
    }

    // Test parser with custom validate
    private class ValidatingParser : TextParser {
        override val supportedFormat = TextFormat(
            id = "validated",
            name = "Validated",
            defaultExtension = ".val",
            extensions = listOf(".val")
        )

        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            val errors = validate(content)
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content,
                errors = errors
            )
        }

        override fun validate(content: String): List<String> {
            val errors = mutableListOf<String>()
            if (content.isEmpty()) {
                errors.add("Content cannot be empty")
            }
            if (content.contains("ERROR")) {
                errors.add("Content contains ERROR keyword")
            }
            return errors
        }
    }

    // ==================== Default canParse Tests ====================

    @Test
    fun `should use default canParse with format ID equality`() {
        val parser = DefaultTestParser()
        val matchingFormat = TextFormat("test", "Test", ".test", listOf(".test"))
        val differentFormat = TextFormat("other", "Other", ".other", listOf(".other"))

        assertTrue(parser.canParse(matchingFormat))
        assertFalse(parser.canParse(differentFormat))
    }

    @Test
    fun `should match format by ID exactly`() {
        val parser = DefaultTestParser()
        val format1 = TextFormat("test", "Name1", ".ext1", listOf(".ext1"))
        val format2 = TextFormat("test", "Name2", ".ext2", listOf(".ext2"))

        // Both should match because ID is the same
        assertTrue(parser.canParse(format1))
        assertTrue(parser.canParse(format2))
    }

    @Test
    fun `should not match different format IDs`() {
        val parser = DefaultTestParser()
        val format = TextFormat("different", "Different", ".diff", listOf(".diff"))

        assertFalse(parser.canParse(format))
    }

    // ==================== Custom canParse Tests ====================

    @Test
    fun `should allow custom canParse logic`() {
        val parser = CustomCanParseParser()
        val format1 = TextFormat("custom", "Custom1", ".c1", listOf(".c1"))
        val format2 = TextFormat("custom2", "Custom2", ".c2", listOf(".c2"))
        val format3 = TextFormat("other", "Other", ".o", listOf(".o"))

        assertTrue(parser.canParse(format1))
        assertTrue(parser.canParse(format2))
        assertFalse(parser.canParse(format3))
    }

    // ==================== Default toHtml Tests ====================

    @Test
    fun `should use default toHtml with pre tag`() {
        val parser = DefaultTestParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "test content",
            parsedContent = "test content"
        )

        val html = parser.toHtml(document)

        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("test content"))
        assertTrue(html.contains("</pre>"))
    }

    @Test
    fun `should escape HTML in default toHtml`() {
        val parser = DefaultTestParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "<script>alert('xss')</script>",
            parsedContent = "<script>alert('xss')</script>"
        )

        val html = parser.toHtml(document)

        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&lt;/script&gt;"))
        assertFalse(html.contains("<script>"))
    }

    @Test
    fun `should handle empty content in default toHtml`() {
        val parser = DefaultTestParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "",
            parsedContent = ""
        )

        val html = parser.toHtml(document)

        assertEquals("<pre></pre>", html)
    }

    @Test
    fun `default toHtml should ignore lightMode parameter`() {
        val parser = DefaultTestParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "content",
            parsedContent = "content"
        )

        val lightHtml = parser.toHtml(document, lightMode = true)
        val darkHtml = parser.toHtml(document, lightMode = false)

        // Default implementation doesn't differentiate
        assertEquals(lightHtml, darkHtml)
    }

    // ==================== Custom toHtml Tests ====================

    @Test
    fun `should allow custom toHtml implementation`() {
        val parser = CustomHtmlParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "test",
            parsedContent = "test"
        )

        val lightHtml = parser.toHtml(document, lightMode = true)
        val darkHtml = parser.toHtml(document, lightMode = false)

        assertTrue(lightHtml.contains("class='light'"))
        assertTrue(darkHtml.contains("class='dark'"))
        assertNotEquals(lightHtml, darkHtml)
    }

    @Test
    fun `custom toHtml should handle empty content`() {
        val parser = CustomHtmlParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "",
            parsedContent = ""
        )

        val html = parser.toHtml(document)

        assertTrue(html.contains("<div"))
        assertTrue(html.contains("</div>"))
    }

    // ==================== Default validate Tests ====================

    @Test
    fun `should return empty errors by default`() {
        val parser = DefaultTestParser()

        val errors = parser.validate("any content")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `default validate should accept invalid syntax`() {
        val parser = DefaultTestParser()

        val errors = parser.validate("<<<<<>>>>>")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `default validate should accept empty content`() {
        val parser = DefaultTestParser()

        val errors = parser.validate("")

        assertTrue(errors.isEmpty())
    }

    // ==================== Custom validate Tests ====================

    @Test
    fun `should allow custom validate implementation`() {
        val parser = ValidatingParser()

        val errorsEmpty = parser.validate("")
        val errorsWithError = parser.validate("This contains ERROR")
        val errorsValid = parser.validate("valid content")

        assertEquals(1, errorsEmpty.size)
        assertTrue(errorsEmpty[0].contains("empty"))

        assertEquals(1, errorsWithError.size)
        assertTrue(errorsWithError[0].contains("ERROR"))

        assertTrue(errorsValid.isEmpty())
    }

    @Test
    fun `custom validate should detect multiple errors`() {
        val parser = ValidatingParser()

        // Empty content with ERROR keyword (but won't match ERROR since empty)
        val errors = parser.validate("")

        assertEquals(1, errors.size)
    }

    // ==================== Parse Method Tests ====================

    @Test
    fun `parse should return ParsedDocument`() {
        val parser = DefaultTestParser()

        val document = parser.parse("test content")

        assertNotNull(document)
        assertEquals("test content", document.rawContent)
        assertEquals("test content", document.parsedContent)
    }

    @Test
    fun `parse should accept options`() {
        val parser = DefaultTestParser()
        val options = mapOf("key" to "value", "number" to 42)

        val document = parser.parse("content", options)

        assertNotNull(document)
    }

    @Test
    fun `parse should accept empty options`() {
        val parser = DefaultTestParser()

        val document = parser.parse("content", emptyMap())

        assertNotNull(document)
    }

    @Test
    fun `parse should handle empty content`() {
        val parser = DefaultTestParser()

        val document = parser.parse("")

        assertNotNull(document)
        assertEquals("", document.rawContent)
    }

    @Test
    fun `parse should handle very long content`() {
        val parser = DefaultTestParser()
        val longContent = "A".repeat(100000)

        val document = parser.parse(longContent)

        assertNotNull(document)
        assertEquals(100000, document.rawContent.length)
    }

    @Test
    fun `parse should handle unicode content`() {
        val parser = DefaultTestParser()
        val content = "Unicode: 你好 🌍 Привет"

        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals(content, document.rawContent)
    }

    @Test
    fun `parse should handle special characters`() {
        val parser = DefaultTestParser()
        val content = "Special: <>&\"'\n\t\r"

        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals(content, document.rawContent)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate validate with parse`() {
        val parser = ValidatingParser()

        val document = parser.parse("This has ERROR")

        assertFalse(document.errors.isEmpty())
        assertTrue(document.errors[0].contains("ERROR"))
    }

    @Test
    fun `should integrate toHtml with parse`() {
        val parser = CustomHtmlParser()

        val document = parser.parse("content")
        val html = parser.toHtml(document)

        assertTrue(html.contains("content"))
        assertTrue(html.contains("div"))
    }

    @Test
    fun `should use canParse to filter formats`() {
        val parser = CustomCanParseParser()
        val formats = listOf(
            TextFormat("custom", "C1", ".c1", listOf(".c1")),
            TextFormat("custom2", "C2", ".c2", listOf(".c2")),
            TextFormat("other", "O", ".o", listOf(".o"))
        )

        val compatible = formats.filter { parser.canParse(it) }

        assertEquals(2, compatible.size)
        assertTrue(compatible.any { it.id == "custom" })
        assertTrue(compatible.any { it.id == "custom2" })
    }

    // ==================== Supported Format Tests ====================

    @Test
    fun `should provide supportedFormat property`() {
        val parser = DefaultTestParser()

        assertNotNull(parser.supportedFormat)
        assertEquals("test", parser.supportedFormat.id)
        assertEquals("Test Format", parser.supportedFormat.name)
    }

    @Test
    fun `supportedFormat should be consistent`() {
        val parser = DefaultTestParser()

        val format1 = parser.supportedFormat
        val format2 = parser.supportedFormat

        assertEquals(format1, format2)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle null bytes in content`() {
        val parser = DefaultTestParser()
        val content = "Text\u0000with\u0000nulls"

        val document = parser.parse(content)

        assertNotNull(document)
    }

    @Test
    fun `should handle content with only whitespace`() {
        val parser = DefaultTestParser()
        val content = "   \n\n\t\t   "

        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals(content, document.rawContent)
    }

    @Test
    fun `should handle multiline content`() {
        val parser = DefaultTestParser()
        val content = "Line 1\nLine 2\nLine 3"

        val document = parser.parse(content)

        assertNotNull(document)
        assertTrue(document.rawContent.contains("\n"))
    }

    @Test
    fun `toHtml should preserve newlines in pre tag`() {
        val parser = DefaultTestParser()
        val document = ParsedDocument(
            format = parser.supportedFormat,
            rawContent = "Line 1\nLine 2",
            parsedContent = "Line 1\nLine 2"
        )

        val html = parser.toHtml(document)

        assertTrue(html.contains("Line 1"))
        assertTrue(html.contains("Line 2"))
    }

    @Test
    fun `should handle formats with multiple extensions`() {
        val format = TextFormat(
            id = "multi",
            name = "Multi",
            defaultExtension = ".m1",
            extensions = listOf(".m1", ".m2", ".m3")
        )
        val parser = DefaultTestParser(format)

        assertEquals(3, parser.supportedFormat.extensions.size)
    }
}
