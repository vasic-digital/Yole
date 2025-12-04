/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for parser lifecycle and state management
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.csv.CsvParser
import kotlin.test.*

/**
 * Integration tests for parser lifecycle, state management, and registry interactions.
 *
 * Tests cover:
 * - Parser registration/unregistration cycles
 * - Registry state management
 * - Multiple parser instances
 * - Parser cleanup and isolation
 * - Registry state after errors
 */
class ParserLifecycleIntegrationTest {

    @BeforeTest
    fun setup() {
        ParserRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Registration Lifecycle Tests ====================

    @Test
    fun `should handle multiple register clear cycles`() {
        val parser = MarkdownParser()
        val format = FormatRegistry.getById("markdown")!!

        // Cycle 1: Register and verify
        ParserRegistry.register(parser)
        assertTrue(ParserRegistry.hasParser(format))
        assertEquals(parser, ParserRegistry.getParser(format))

        // Cycle 2: Clear and verify
        ParserRegistry.clear()
        assertFalse(ParserRegistry.hasParser(format))
        assertNull(ParserRegistry.getParser(format))

        // Cycle 3: Re-register and verify
        ParserRegistry.register(parser)
        assertTrue(ParserRegistry.hasParser(format))

        // Cycle 4: Clear again
        ParserRegistry.clear()
        assertFalse(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should maintain registry state across multiple parsers`() {
        val markdownParser = MarkdownParser()
        val plaintextParser = PlaintextParser()
        val csvParser = CsvParser()

        // Register all
        ParserRegistry.register(markdownParser)
        ParserRegistry.register(plaintextParser)
        ParserRegistry.register(csvParser)

        // Verify all present
        assertEquals(3, ParserRegistry.getAllParsers().size)
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("plaintext")!!))
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("csv")!!))

        // Clear and re-register selectively
        ParserRegistry.clear()
        ParserRegistry.register(markdownParser)
        ParserRegistry.register(csvParser)

        // Verify correct state
        assertEquals(2, ParserRegistry.getAllParsers().size)
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertFalse(ParserRegistry.hasParser(FormatRegistry.getById("plaintext")!!))
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("csv")!!))
    }

    @Test
    fun `should handle parser replacement`() {
        val format = FormatRegistry.getById("markdown")!!
        val parser1 = MarkdownParser()
        val parser2 = MarkdownParser()

        // Register first parser
        ParserRegistry.register(parser1)
        assertEquals(parser1, ParserRegistry.getParser(format))

        // Attempt to register second parser (should fail with duplicate error)
        assertFails {
            ParserRegistry.register(parser2)
        }

        // Original parser should still be registered
        assertEquals(parser1, ParserRegistry.getParser(format))
    }

    @Test
    fun `should isolate parser instances`() {
        val parser1 = MarkdownParser()
        val parser2 = MarkdownParser()

        ParserRegistry.register(parser1)

        // Parse with first instance
        val content1 = "# Header 1"
        val doc1 = parser1.parse(content1)
        assertTrue(doc1.parsedContent.contains("Header 1"))

        // Parse different content with second instance (not registered)
        val content2 = "# Header 2"
        val doc2 = parser2.parse(content2)
        assertTrue(doc2.parsedContent.contains("Header 2"))

        // Verify both parsers produced different results
        assertNotEquals(doc1.parsedContent, doc2.parsedContent)
        assertTrue(doc1.parsedContent.contains("Header 1"))
        assertTrue(doc2.parsedContent.contains("Header 2"))
    }

    @Test
    fun `should maintain registry state after parse errors`() {
        val parser = MarkdownParser()
        ParserRegistry.register(parser)

        // Parse malformed content (should not crash)
        val malformedContent = "[Unclosed bracket"
        val doc = parser.parse(malformedContent)

        // Registry should still be intact
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertEquals(parser, ParserRegistry.getParser(FormatRegistry.getById("markdown")!!))

        // Should still be able to parse valid content
        val validContent = "# Valid header"
        val validDoc = parser.parse(validContent)
        assertTrue(validDoc.parsedContent.contains("<h1>"))
    }

    // ==================== Multiple Parser Instances Tests ====================

    @Test
    fun `should support multiple active parsers simultaneously`() {
        val markdownParser = MarkdownParser()
        val csvParser = CsvParser()
        val plaintextParser = PlaintextParser()

        ParserRegistry.register(markdownParser)
        ParserRegistry.register(csvParser)
        ParserRegistry.register(plaintextParser)

        // Parse with all three simultaneously
        val mdDoc = markdownParser.parse("# Markdown")
        val csvDoc = csvParser.parse("col1,col2\nval1,val2")
        val txtDoc = plaintextParser.parse("Plain text")

        // Verify all produced results
        assertNotNull(mdDoc.parsedContent)
        assertNotNull(csvDoc.parsedContent)
        assertNotNull(txtDoc.parsedContent)

        // Verify registry still has all three
        assertEquals(3, ParserRegistry.getAllParsers().size)
    }

    @Test
    fun `should handle parser switching within session`() {
        val markdownParser = MarkdownParser()
        val plaintextParser = PlaintextParser()

        ParserRegistry.register(markdownParser)
        ParserRegistry.register(plaintextParser)

        // Parse markdown
        val mdContent = "# Header"
        val mdDoc = markdownParser.parse(mdContent)
        assertTrue(mdDoc.parsedContent.contains("<h1>"))

        // Switch to plaintext
        val txtContent = "Just text"
        val txtDoc = plaintextParser.parse(txtContent)
        assertTrue(txtDoc.parsedContent.contains("Just text"))

        // Switch back to markdown
        val mdContent2 = "## Subheader"
        val mdDoc2 = markdownParser.parse(mdContent2)
        assertTrue(mdDoc2.parsedContent.contains("<h2>"))

        // Verify both parsers still registered
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("plaintext")!!))
    }

    @Test
    fun `should maintain parser state across multiple parse calls`() {
        val parser = MarkdownParser()
        ParserRegistry.register(parser)

        // Parse multiple documents
        val docs = (1..10).map { i ->
            parser.parse("# Header $i")
        }

        // Verify all documents parsed correctly
        assertEquals(10, docs.size)
        docs.forEachIndexed { index, doc ->
            assertTrue(doc.parsedContent.contains("Header ${index + 1}"))
        }

        // Verify parser still registered
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
    }

    // ==================== Registry Cleanup Tests ====================

    @Test
    fun `should clear all parsers at once`() {
        val markdownParser = MarkdownParser()
        val csvParser = CsvParser()
        val plaintextParser = PlaintextParser()

        ParserRegistry.register(markdownParser)
        ParserRegistry.register(csvParser)
        ParserRegistry.register(plaintextParser)

        assertEquals(3, ParserRegistry.getAllParsers().size)

        // Clear all
        ParserRegistry.clear()

        // Verify all gone
        assertEquals(0, ParserRegistry.getAllParsers().size)
        assertFalse(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertFalse(ParserRegistry.hasParser(FormatRegistry.getById("csv")!!))
        assertFalse(ParserRegistry.hasParser(FormatRegistry.getById("plaintext")!!))
    }

    @Test
    fun `should handle clear on empty registry`() {
        assertEquals(0, ParserRegistry.getAllParsers().size)

        // Should not throw
        ParserRegistry.clear()

        assertEquals(0, ParserRegistry.getAllParsers().size)
    }

    @Test
    fun `should allow re-registration after clear`() {
        val parser = MarkdownParser()

        // Register, clear, re-register
        ParserRegistry.register(parser)
        ParserRegistry.clear()
        ParserRegistry.register(parser)

        // Verify re-registered
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
        assertEquals(parser, ParserRegistry.getParser(FormatRegistry.getById("markdown")!!))
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should maintain registry integrity after registration errors`() {
        val parser1 = MarkdownParser()
        val parser2 = MarkdownParser()

        // Register first successfully
        ParserRegistry.register(parser1)
        assertEquals(1, ParserRegistry.getAllParsers().size)

        // Attempt duplicate registration (should fail)
        assertFails {
            ParserRegistry.register(parser2)
        }

        // Registry should still have only the first parser
        assertEquals(1, ParserRegistry.getAllParsers().size)
        assertEquals(parser1, ParserRegistry.getParser(FormatRegistry.getById("markdown")!!))
    }

    @Test
    fun `should handle getParser for non-existent format`() {
        val parser = MarkdownParser()
        ParserRegistry.register(parser)

        // Try to get non-existent format
        val fakeFormat = TextFormat(
            id = "fake",
            name = "Fake",
            defaultExtension = ".fake",
            extensions = listOf(".fake")
        )

        // Should return null
        assertNull(ParserRegistry.getParser(fakeFormat))

        // Original parser should still be there
        assertTrue(ParserRegistry.hasParser(FormatRegistry.getById("markdown")!!))
    }

    // ==================== Concurrent-like Access Tests ====================

    @Test
    fun `should handle rapid registration and lookup`() {
        val parser = MarkdownParser()
        val format = FormatRegistry.getById("markdown")!!

        // Rapid register/lookup sequence
        repeat(100) {
            if (it == 0) {
                ParserRegistry.register(parser)
            }
            val retrieved = ParserRegistry.getParser(format)
            assertNotNull(retrieved)
            assertEquals(parser, retrieved)
        }
    }

    @Test
    fun `should handle rapid parse calls`() {
        val parser = MarkdownParser()
        ParserRegistry.register(parser)

        // Rapid parsing
        val docs = (1..100).map {
            parser.parse("# Header $it")
        }

        assertEquals(100, docs.size)
        docs.forEachIndexed { index, doc ->
            assertTrue(doc.rawContent.contains("Header ${index + 1}"))
        }
    }

    // ==================== Parser State Tests ====================

    @Test
    fun `should not share state between parser instances`() {
        val parser1 = MarkdownParser()
        val parser2 = MarkdownParser()

        val doc1 = parser1.parse("# Content 1")
        val doc2 = parser2.parse("# Content 2")

        // Each parser should have produced independent results
        assertTrue(doc1.rawContent.contains("Content 1"))
        assertTrue(doc2.rawContent.contains("Content 2"))
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun `should preserve parser options across calls`() {
        val parser = MarkdownParser()
        ParserRegistry.register(parser)

        val options = mapOf("filename" to "test.md")

        val doc1 = parser.parse("# First", options)
        val doc2 = parser.parse("# Second", options)

        // Both should have the filename in metadata
        assertEquals(".md", doc1.metadata["extension"])
        assertEquals(".md", doc2.metadata["extension"])
    }
}
