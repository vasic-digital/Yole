/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive error path tests for parser infrastructure
 * Focuses on exception handling, error messages, and edge cases
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.*

/**
 * Comprehensive error path tests for parser infrastructure.
 *
 * Tests cover:
 * - Exception handling for missing parsers
 * - Duplicate registration errors (eager and lazy)
 * - Validation error messages
 * - HTML caching edge cases
 * - ParsedDocument error scenarios
 * - Format registry edge cases
 */
class ParserErrorPathTest {

    // Test parser implementation
    private class TestParser(
        private val formatId: String = "test",
        private val formatName: String = "Test Format"
    ) : TextParser {
        override val supportedFormat = TextFormat(
            id = formatId,
            name = formatName,
            defaultExtension = ".test",
            extensions = listOf(".test")
        )

        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = "<p>$content</p>"
            )
        }
    }

    @BeforeTest
    fun setup() {
        // Clear registry before each test to ensure isolation
        ParserRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        // Clear registry after each test
        ParserRegistry.clear()
    }

    // ==================== Parser Not Found Error Tests ====================

    @Test
    fun `should throw IllegalStateException when parser not found for toHtml in light mode`() {
        // Create a format that has no registered parser
        val format = TextFormat(
            id = "unregistered",
            name = "Unregistered Format",
            defaultExtension = ".unreg",
            extensions = listOf(".unreg")
        )

        // Create a document with this format
        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed"
        )

        // Attempting to call toHtml should throw IllegalStateException
        val exception = assertFails {
            document.toHtml(lightMode = true)
        }

        assertTrue(exception is IllegalStateException, "Expected IllegalStateException but got ${exception::class.simpleName}")
        assertTrue(
            exception.message?.contains("No parser found for format") == true,
            "Exception message should mention missing parser"
        )
        assertTrue(
            exception.message?.contains(format.id) == true,
            "Exception message should include format ID '${format.id}'"
        )
    }

    @Test
    fun `should throw IllegalStateException when parser not found for toHtml in dark mode`() {
        // Create a format that has no registered parser
        val format = TextFormat(
            id = "unregistered-dark",
            name = "Unregistered Dark Format",
            defaultExtension = ".unreg",
            extensions = listOf(".unreg")
        )

        // Create a document with this format
        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed"
        )

        // Attempting to call toHtml in dark mode should throw IllegalStateException
        val exception = assertFails {
            document.toHtml(lightMode = false)
        }

        assertTrue(exception is IllegalStateException, "Expected IllegalStateException but got ${exception::class.simpleName}")
        assertTrue(
            exception.message?.contains("No parser found for format") == true,
            "Exception message should mention missing parser"
        )
        assertTrue(
            exception.message?.contains(format.id) == true,
            "Exception message should include format ID '${format.id}'"
        )
    }

    @Test
    fun `should throw IllegalStateException after parser is unregistered`() {
        val parser = TestParser("temp-format", "Temporary Format")
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        // First call should work
        val html1 = document.toHtml()
        assertNotNull(html1)

        // Clear registry (simulating parser unregistration)
        ParserRegistry.clear()

        // Clear cache to force regeneration
        document.clearHtmlCache()

        // Second call should throw
        val exception = assertFails {
            document.toHtml()
        }

        assertTrue(exception is IllegalStateException)
        assertTrue(exception.message?.contains("No parser found") == true)
    }

    // ==================== Duplicate Registration Error Tests ====================

    @Test
    fun `should throw IllegalArgumentException for duplicate eager registration`() {
        val parser1 = TestParser("duplicate-eager", "Duplicate Eager Format")
        val parser2 = TestParser("duplicate-eager", "Duplicate Eager Format")

        ParserRegistry.register(parser1)

        val exception = assertFails {
            ParserRegistry.register(parser2)
        }

        assertTrue(exception is IllegalArgumentException, "Expected IllegalArgumentException but got ${exception::class.simpleName}")
        assertTrue(
            exception.message?.contains("already registered") == true,
            "Exception message should mention 'already registered'"
        )
        assertTrue(
            exception.message?.contains("duplicate-eager") == true,
            "Exception message should include format ID 'duplicate-eager'"
        )
    }

    @Test
    fun `should throw IllegalArgumentException for duplicate lazy registration`() {
        ParserRegistry.registerLazy("duplicate-lazy") { TestParser("duplicate-lazy", "Duplicate Lazy 1") }

        val exception = assertFails {
            ParserRegistry.registerLazy("duplicate-lazy") { TestParser("duplicate-lazy", "Duplicate Lazy 2") }
        }

        assertTrue(exception is IllegalArgumentException, "Expected IllegalArgumentException but got ${exception::class.simpleName}")
        assertTrue(
            exception.message?.contains("already registered") == true,
            "Exception message should mention 'already registered'"
        )
        assertTrue(
            exception.message?.contains("duplicate-lazy") == true,
            "Exception message should include format ID 'duplicate-lazy'"
        )
    }

    @Test
    fun `should throw IllegalArgumentException when lazy follows eager registration`() {
        val parser = TestParser("eager-then-lazy", "Eager Then Lazy")
        ParserRegistry.register(parser)

        val exception = assertFails {
            ParserRegistry.registerLazy("eager-then-lazy") { TestParser("eager-then-lazy", "Second") }
        }

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message?.contains("already registered") == true)
    }

    @Test
    fun `should throw IllegalArgumentException when eager follows lazy registration`() {
        ParserRegistry.registerLazy("lazy-then-eager") { TestParser("lazy-then-eager", "Lazy First") }

        val parser = TestParser("lazy-then-eager", "Eager Second")
        val exception = assertFails {
            ParserRegistry.register(parser)
        }

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message?.contains("already registered") == true)
    }

    // ==================== Validation Error Message Tests ====================

    @Test
    fun `should return correct validation errors for malformed markdown`() {
        val parser = MarkdownParser()
        val malformed = """
            [Link without closing paren](url
            Text with [unclosed bracket
            Another line with (unclosed paren
        """.trimIndent()

        val errors = parser.validate(malformed)

        assertNotNull(errors, "Validation errors should not be null")
        assertTrue(errors.isNotEmpty(), "Should detect validation errors in malformed markdown")

        // Check that error messages mention line numbers
        assertTrue(
            errors.any { it.contains("Line") },
            "Error messages should include line numbers"
        )
    }

    @Test
    fun `should return empty list for valid markdown`() {
        val parser = MarkdownParser()
        val valid = """
            # Header

            This is [a valid link](https://example.com).

            **Bold** and *italic* text.
        """.trimIndent()

        val errors = parser.validate(valid)

        assertTrue(errors.isEmpty(), "Valid markdown should have no validation errors")
    }

    @Test
    fun `should detect unclosed brackets in markdown`() {
        val parser = MarkdownParser()
        val unclosed = "[Link without closing bracket"

        val errors = parser.validate(unclosed)

        assertTrue(errors.isNotEmpty(), "Should detect unclosed brackets")
        assertTrue(
            errors.any { it.contains("bracket") || it.contains("Unclosed") },
            "Error message should mention brackets or unclosed elements"
        )
    }

    @Test
    fun `should detect unclosed parentheses in markdown`() {
        val parser = MarkdownParser()
        val unclosed = "Text with (unclosed parenthesis"

        val errors = parser.validate(unclosed)

        assertTrue(errors.isNotEmpty(), "Should detect unclosed parentheses")
        assertTrue(
            errors.any { it.contains("paren") || it.contains("Unclosed") },
            "Error message should mention parentheses or unclosed elements"
        )
    }

    // ==================== HTML Caching Edge Cases ====================

    @Test
    fun `should cache HTML separately for light and dark modes`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        // Generate HTML for both modes
        val lightHtml = document.toHtml(lightMode = true)
        val darkHtml = document.toHtml(lightMode = false)

        // Both should be non-null
        assertNotNull(lightHtml)
        assertNotNull(darkHtml)

        // Both should be cached
        assertTrue(document.hasHtmlCached(lightMode = true))
        assertTrue(document.hasHtmlCached(lightMode = false))
    }

    @Test
    fun `should clear both light and dark HTML caches`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        // Generate and cache both modes
        document.toHtml(lightMode = true)
        document.toHtml(lightMode = false)

        assertTrue(document.hasHtmlCached(lightMode = true))
        assertTrue(document.hasHtmlCached(lightMode = false))

        // Clear cache
        document.clearHtmlCache()

        // Both should be uncached
        assertFalse(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))
    }

    @Test
    fun `should not cache HTML until toHtml is called`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        // Cache should be empty initially
        assertFalse(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))

        // Call toHtml for light mode
        document.toHtml(lightMode = true)

        // Only light mode should be cached
        assertTrue(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))
    }

    @Test
    fun `should regenerate HTML after cache clear`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        val html1 = document.toHtml()
        document.clearHtmlCache()
        val html2 = document.toHtml()

        // Both should be valid HTML
        assertNotNull(html1)
        assertNotNull(html2)

        // Content should be the same (regenerated)
        assertEquals(html1, html2)
    }

    // ==================== ParsedDocument Edge Cases ====================

    @Test
    fun `should handle ParsedDocument with empty rawContent`() {
        val parser = PlaintextParser()
        ParserRegistry.register(parser)

        val document = parser.parse("")

        assertEquals("", document.rawContent)
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle ParsedDocument with empty parsedContent`() {
        val format = TextFormat(
            id = "empty-parsed",
            name = "Empty Parsed",
            defaultExtension = ".empty",
            extensions = listOf(".empty")
        )

        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = ""
        )

        assertEquals("content", document.rawContent)
        assertEquals("", document.parsedContent)
    }

    @Test
    fun `should handle ParsedDocument with errors list`() {
        val format = TextFormat(
            id = "with-errors",
            name = "With Errors",
            defaultExtension = ".err",
            extensions = listOf(".err")
        )

        val errors = listOf("Error 1", "Error 2", "Error 3")
        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed",
            errors = errors
        )

        assertEquals(3, document.errors.size)
        assertTrue(document.errors.contains("Error 1"))
        assertTrue(document.errors.contains("Error 2"))
        assertTrue(document.errors.contains("Error 3"))
    }

    @Test
    fun `should handle ParsedDocument with metadata`() {
        val format = TextFormat(
            id = "with-metadata",
            name = "With Metadata",
            defaultExtension = ".meta",
            extensions = listOf(".meta")
        )

        val metadata = mapOf(
            "lines" to "100",
            "words" to "500",
            "characters" to "2500"
        )

        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed",
            metadata = metadata
        )

        assertEquals("100", document.metadata["lines"])
        assertEquals("500", document.metadata["words"])
        assertEquals("2500", document.metadata["characters"])
    }

    // ==================== Format Registry Edge Cases ====================

    @Test
    fun `should handle getParser with null return gracefully`() {
        val nonExistent = TextFormat(
            id = "nonexistent",
            name = "Non-Existent",
            defaultExtension = ".none",
            extensions = listOf(".none")
        )

        val parser = ParserRegistry.getParser(nonExistent)

        assertNull(parser, "Should return null for non-existent format")
    }

    @Test
    fun `should handle hasParser with unregistered format`() {
        val nonExistent = TextFormat(
            id = "unregistered",
            name = "Unregistered",
            defaultExtension = ".unreg",
            extensions = listOf(".unreg")
        )

        assertFalse(ParserRegistry.hasParser(nonExistent))
    }

    @Test
    fun `should handle getAllParsers on empty registry`() {
        ParserRegistry.clear()

        val parsers = ParserRegistry.getAllParsers()

        assertTrue(parsers.isEmpty(), "Should return empty list when no parsers registered")
    }

    // ==================== Parser Options Error Tests ====================

    @Test
    fun `should handle invalid option values gracefully`() {
        val parser = PlaintextParser()
        ParserRegistry.register(parser)

        val invalidOptions = mapOf<String, Any>(
            "unknown_option" to "value",
            "invalid_number" to "not a number",
            "null_value" to "null"
        )

        // Should not throw, just ignore invalid options
        val document = parser.parse("content", invalidOptions)

        assertNotNull(document)
        assertEquals("content", document.rawContent)
    }

    @Test
    fun `should handle extremely large options map`() {
        val parser = PlaintextParser()
        ParserRegistry.register(parser)

        val largeOptions = (1..10000).associate { "key$it" to "value$it" }

        val document = parser.parse("content", largeOptions)

        assertNotNull(document)
    }

    // ==================== Concurrent Error Scenarios ====================

    @Test
    fun `should handle rapid successive toHtml calls`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        // Rapid successive calls should all work
        repeat(100) {
            val html = document.toHtml(lightMode = it % 2 == 0)
            assertNotNull(html)
        }
    }

    @Test
    fun `should handle cache clear between toHtml calls`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val document = parser.parse("content")

        repeat(50) {
            document.toHtml()
            document.clearHtmlCache()
        }

        // Should still work after many clear cycles
        val html = document.toHtml()
        assertNotNull(html)
    }

    // ==================== Empty and Null Edge Cases ====================

    @Test
    fun `should handle parser with empty format ID`() {
        val parser = object : TextParser {
            override val supportedFormat = TextFormat(
                id = "",
                name = "Empty ID",
                defaultExtension = ".empty",
                extensions = listOf(".empty")
            )

            override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                return ParsedDocument(
                    format = supportedFormat,
                    rawContent = content,
                    parsedContent = content
                )
            }
        }

        ParserRegistry.register(parser)

        // Should be able to retrieve parser with empty ID
        val retrieved = ParserRegistry.getParser(parser.supportedFormat)
        assertNotNull(retrieved)
    }

    @Test
    fun `should handle format with empty extensions list`() {
        val format = TextFormat(
            id = "no-extensions",
            name = "No Extensions",
            defaultExtension = "",
            extensions = emptyList()
        )

        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed"
        )

        assertNotNull(document)
        assertTrue(document.format.extensions.isEmpty())
    }

    @Test
    fun `should handle ParsedDocument with empty metadata and errors`() {
        val format = TextFormat(
            id = "minimal",
            name = "Minimal",
            defaultExtension = ".min",
            extensions = listOf(".min")
        )

        val document = ParsedDocument(
            format = format,
            rawContent = "content",
            parsedContent = "parsed",
            metadata = emptyMap(),
            errors = emptyList()
        )

        assertTrue(document.metadata.isEmpty())
        assertTrue(document.errors.isEmpty())
    }
}
