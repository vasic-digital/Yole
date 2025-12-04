/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for ParsedDocument data class
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Unit tests for ParsedDocument data class.
 *
 * Tests cover:
 * - Construction with all parameters
 * - Construction with defaults
 * - Data class features (equals, hashCode, copy)
 * - Property access
 * - Edge cases
 */
class ParsedDocumentTest {

    private val testFormat = TextFormat(
        id = "markdown",
        name = "Markdown",
        defaultExtension = ".md",
        extensions = listOf(".md", ".markdown")
    )

    // ==================== Construction Tests ====================

    @Test
    fun `should create ParsedDocument with all parameters`() {
        val metadata = mapOf("lines" to "10", "words" to "50")
        val errors = listOf("Warning: line 5")

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "# Title",
            parsedContent = "<h1>Title</h1>",
            metadata = metadata,
            errors = errors
        )

        assertEquals(testFormat, doc.format)
        assertEquals("# Title", doc.rawContent)
        assertEquals("<h1>Title</h1>", doc.parsedContent)
        assertEquals(metadata, doc.metadata)
        assertEquals(errors, doc.errors)
    }

    @Test
    fun `should create ParsedDocument with default metadata and errors`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed"
        )

        assertEquals(testFormat, doc.format)
        assertEquals("content", doc.rawContent)
        assertEquals("parsed", doc.parsedContent)
        assertTrue(doc.metadata.isEmpty())
        assertTrue(doc.errors.isEmpty())
    }

    @Test
    fun `should handle empty raw content`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "",
            parsedContent = ""
        )

        assertEquals("", doc.rawContent)
        assertEquals("", doc.parsedContent)
    }

    @Test
    fun `should handle empty parsed content`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "# Title",
            parsedContent = ""
        )

        assertEquals("# Title", doc.rawContent)
        assertEquals("", doc.parsedContent)
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `should support equality for identical documents`() {
        val doc1 = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed"
        )

        val doc2 = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed"
        )

        assertEquals(doc1, doc2)
        assertEquals(doc1.hashCode(), doc2.hashCode())
    }

    @Test
    fun `should support inequality for different documents`() {
        val doc1 = ParsedDocument(
            format = testFormat,
            rawContent = "content1",
            parsedContent = "parsed1"
        )

        val doc2 = ParsedDocument(
            format = testFormat,
            rawContent = "content2",
            parsedContent = "parsed2"
        )

        assertNotEquals(doc1, doc2)
    }

    @Test
    fun `should support copy with modifications`() {
        val original = ParsedDocument(
            format = testFormat,
            rawContent = "original",
            parsedContent = "parsed original"
        )

        val modified = original.copy(rawContent = "modified")

        assertEquals("modified", modified.rawContent)
        assertEquals("parsed original", modified.parsedContent)
        assertEquals(testFormat, modified.format)
    }

    @Test
    fun `should support copy with all fields modified`() {
        val original = ParsedDocument(
            format = testFormat,
            rawContent = "original",
            parsedContent = "parsed"
        )

        val newFormat = TextFormat(
            id = "plaintext",
            name = "Plain Text",
            defaultExtension = ".txt",
            extensions = listOf(".txt")
        )

        val modified = original.copy(
            format = newFormat,
            rawContent = "new raw",
            parsedContent = "new parsed",
            metadata = mapOf("lines" to "5"),
            errors = listOf("error1")
        )

        assertEquals(newFormat, modified.format)
        assertEquals("new raw", modified.rawContent)
        assertEquals("new parsed", modified.parsedContent)
        assertEquals(mapOf("lines" to "5"), modified.metadata)
        assertEquals(listOf("error1"), modified.errors)
    }

    @Test
    fun `should provide access to all properties`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "raw",
            parsedContent = "parsed",
            metadata = mapOf("key" to "value"),
            errors = listOf("error")
        )

        // Verify all properties are accessible
        assertEquals(testFormat, doc.format)
        assertEquals("raw", doc.rawContent)
        assertEquals("parsed", doc.parsedContent)
        assertEquals(mapOf("key" to "value"), doc.metadata)
        assertEquals(listOf("error"), doc.errors)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should handle metadata with multiple entries`() {
        val metadata = mapOf(
            "lines" to "100",
            "words" to "500",
            "characters" to "2500",
            "title" to "Document Title"
        )

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            metadata = metadata
        )

        assertEquals("100", doc.metadata["lines"])
        assertEquals("500", doc.metadata["words"])
        assertEquals("2500", doc.metadata["characters"])
        assertEquals("Document Title", doc.metadata["title"])
    }

    @Test
    fun `should handle empty metadata map`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            metadata = emptyMap()
        )

        assertTrue(doc.metadata.isEmpty())
        assertNull(doc.metadata["nonexistent"])
    }

    // ==================== Error Tests ====================

    @Test
    fun `should handle multiple errors`() {
        val errors = listOf(
            "Line 5: Syntax error",
            "Line 10: Warning",
            "Line 15: Missing closing tag"
        )

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            errors = errors
        )

        assertEquals(3, doc.errors.size)
        assertEquals("Line 5: Syntax error", doc.errors[0])
        assertEquals("Line 10: Warning", doc.errors[1])
        assertEquals("Line 15: Missing closing tag", doc.errors[2])
    }

    @Test
    fun `should handle empty errors list`() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            errors = emptyList()
        )

        assertTrue(doc.errors.isEmpty())
    }

    @Test
    fun `should check if document has errors`() {
        val docWithErrors = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            errors = listOf("Error 1")
        )

        val docWithoutErrors = ParsedDocument(
            format = testFormat,
            rawContent = "content",
            parsedContent = "parsed",
            errors = emptyList()
        )

        assertFalse(docWithErrors.errors.isEmpty())
        assertTrue(docWithoutErrors.errors.isEmpty())
    }

    // ==================== Format Tests ====================

    @Test
    fun `should preserve format information`() {
        val format = TextFormat(
            id = "todotxt",
            name = "Todo.txt",
            defaultExtension = ".txt",
            extensions = listOf(".txt"),
            detectionPatterns = listOf("^\\(.*?\\)")
        )

        val doc = ParsedDocument(
            format = format,
            rawContent = "(A) Task",
            parsedContent = "<div class='task'>Task</div>"
        )

        assertEquals("todotxt", doc.format.id)
        assertEquals("Todo.txt", doc.format.name)
        assertEquals(".txt", doc.format.defaultExtension)
    }

    // ==================== Content Tests ====================

    @Test
    fun `should handle multiline raw content`() {
        val multilineContent = """
            # Title

            ## Subtitle

            Content here.
        """.trimIndent()

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = multilineContent,
            parsedContent = "<h1>Title</h1><h2>Subtitle</h2><p>Content here.</p>"
        )

        assertTrue(doc.rawContent.contains("# Title"))
        assertTrue(doc.rawContent.contains("## Subtitle"))
        assertTrue(doc.parsedContent.contains("<h1>Title</h1>"))
    }

    @Test
    fun `should handle special characters in content`() {
        val content = "Special chars: <>&\"'"

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = content,
            parsedContent = content.escapeHtml()
        )

        assertEquals("Special chars: <>&\"'", doc.rawContent)
        assertTrue(doc.parsedContent.contains("&lt;"))
        assertTrue(doc.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode: 你好世界 🌍 Привет"

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = content,
            parsedContent = content
        )

        assertEquals(content, doc.rawContent)
        assertEquals(content, doc.parsedContent)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle very long content`() {
        val longContent = "A".repeat(10000)

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = longContent,
            parsedContent = longContent
        )

        assertEquals(10000, doc.rawContent.length)
        assertEquals(10000, doc.parsedContent.length)
    }

    @Test
    fun `should handle content with only whitespace`() {
        val whitespace = "   \n\n\t  "

        val doc = ParsedDocument(
            format = testFormat,
            rawContent = whitespace,
            parsedContent = "<pre>   \\n\\n\\t  </pre>"
        )

        assertEquals(whitespace, doc.rawContent)
    }
}
