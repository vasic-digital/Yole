/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for ParserRegistry
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Unit tests for ParserRegistry object.
 *
 * Tests cover:
 * - Parser registration
 * - Parser lookup (by format and by ID)
 * - Parser existence checks
 * - Duplicate registration handling
 * - Registry clearing
 * - Edge cases
 */
class ParserRegistryTest {

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
                parsedContent = content
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

    // ==================== Registration Tests ====================

    @Test
    fun `should register parser successfully`() {
        val parser = TestParser()

        ParserRegistry.register(parser)

        assertTrue(ParserRegistry.hasParser(parser.supportedFormat))
    }

    @Test
    fun `should register multiple parsers`() {
        val parser1 = TestParser("format1", "Format 1")
        val parser2 = TestParser("format2", "Format 2")
        val parser3 = TestParser("format3", "Format 3")

        ParserRegistry.register(parser1)
        ParserRegistry.register(parser2)
        ParserRegistry.register(parser3)

        assertEquals(3, ParserRegistry.getAllParsers().size)
        assertTrue(ParserRegistry.hasParser(parser1.supportedFormat))
        assertTrue(ParserRegistry.hasParser(parser2.supportedFormat))
        assertTrue(ParserRegistry.hasParser(parser3.supportedFormat))
    }

    @Test
    fun `should throw error when registering duplicate format`() {
        val parser1 = TestParser("duplicate", "Duplicate Format")
        val parser2 = TestParser("duplicate", "Duplicate Format")

        ParserRegistry.register(parser1)

        val exception = assertFails {
            ParserRegistry.register(parser2)
        }

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception.message!!.contains("already registered"))
        assertTrue(exception.message!!.contains("duplicate"))
    }

    // ==================== Lookup Tests ====================

    @Test
    fun `should get parser by format`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val retrieved = ParserRegistry.getParser(parser.supportedFormat)

        assertNotNull(retrieved)
        assertEquals(parser.supportedFormat, retrieved.supportedFormat)
    }

    @Test
    fun `should get parser by format ID`() {
        val parser = TestParser("markdown", "Markdown")
        ParserRegistry.register(parser)

        val retrieved = ParserRegistry.getParser("markdown")

        assertNotNull(retrieved)
        assertEquals("markdown", retrieved?.supportedFormat?.id)
    }

    @Test
    fun `should return null for nonexistent format`() {
        val format = TextFormat(
            id = "nonexistent",
            name = "Nonexistent",
            defaultExtension = ".none",
            extensions = listOf(".none")
        )

        val parser = ParserRegistry.getParser(format)

        assertNull(parser)
    }

    @Test
    fun `should return null for nonexistent format ID`() {
        val parser = ParserRegistry.getParser("nonexistent")

        assertNull(parser)
    }

    @Test
    fun `should return null when format not in FormatRegistry`() {
        // Register a parser for a format
        val parser = TestParser("custom", "Custom")
        ParserRegistry.register(parser)

        // Try to get parser by ID for format not in FormatRegistry
        val retrieved = ParserRegistry.getParser("unknown_id")

        assertNull(retrieved)
    }

    // ==================== Existence Check Tests ====================

    @Test
    fun `should correctly check parser existence`() {
        val parser = TestParser()
        val format = parser.supportedFormat

        assertFalse(ParserRegistry.hasParser(format))

        ParserRegistry.register(parser)

        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should return false for nonexistent parser`() {
        val format = TextFormat(
            id = "missing",
            name = "Missing",
            defaultExtension = ".miss",
            extensions = listOf(".miss")
        )

        assertFalse(ParserRegistry.hasParser(format))
    }

    // ==================== Get All Parsers Tests ====================

    @Test
    fun `should return empty list when no parsers registered`() {
        val parsers = ParserRegistry.getAllParsers()

        assertTrue(parsers.isEmpty())
    }

    @Test
    fun `should return all registered parsers`() {
        val parser1 = TestParser("format1", "Format 1")
        val parser2 = TestParser("format2", "Format 2")
        val parser3 = TestParser("format3", "Format 3")

        ParserRegistry.register(parser1)
        ParserRegistry.register(parser2)
        ParserRegistry.register(parser3)

        val parsers = ParserRegistry.getAllParsers()

        assertEquals(3, parsers.size)
        assertTrue(parsers.any { it.supportedFormat.id == "format1" })
        assertTrue(parsers.any { it.supportedFormat.id == "format2" })
        assertTrue(parsers.any { it.supportedFormat.id == "format3" })
    }

    @Test
    fun `should return immutable copy of parsers list`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        val parsers1 = ParserRegistry.getAllParsers()
        val parsers2 = ParserRegistry.getAllParsers()

        // Should be equal but not the same instance
        assertEquals(parsers1, parsers2)
        assertNotSame(parsers1, parsers2)
    }

    // ==================== Clear Tests ====================

    @Test
    fun `should clear all registered parsers`() {
        val parser1 = TestParser("format1", "Format 1")
        val parser2 = TestParser("format2", "Format 2")

        ParserRegistry.register(parser1)
        ParserRegistry.register(parser2)

        assertEquals(2, ParserRegistry.getAllParsers().size)

        ParserRegistry.clear()

        assertEquals(0, ParserRegistry.getAllParsers().size)
        assertFalse(ParserRegistry.hasParser(parser1.supportedFormat))
        assertFalse(ParserRegistry.hasParser(parser2.supportedFormat))
    }

    @Test
    fun `should allow re-registration after clear`() {
        val parser = TestParser()

        ParserRegistry.register(parser)
        ParserRegistry.clear()
        ParserRegistry.register(parser) // Should not throw

        assertTrue(ParserRegistry.hasParser(parser.supportedFormat))
    }

    @Test
    fun `should handle clear on empty registry`() {
        ParserRegistry.clear() // Should not throw

        assertEquals(0, ParserRegistry.getAllParsers().size)
    }

    // ==================== Parser Interface Tests ====================

    @Test
    fun `should use parser canParse method for lookup`() {
        val customParser = object : TextParser {
            override val supportedFormat = TextFormat(
                id = "custom",
                name = "Custom",
                defaultExtension = ".custom",
                extensions = listOf(".custom")
            )

            override fun canParse(format: TextFormat): Boolean {
                // Custom logic: can parse both "custom" and "custom2"
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

        ParserRegistry.register(customParser)

        val format1 = TextFormat("custom", "Custom", ".custom", listOf(".custom"))
        val format2 = TextFormat("custom2", "Custom2", ".custom2", listOf(".custom2"))
        val format3 = TextFormat("other", "Other", ".other", listOf(".other"))

        assertNotNull(ParserRegistry.getParser(format1))
        assertNotNull(ParserRegistry.getParser(format2))
        assertNull(ParserRegistry.getParser(format3))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle parser with empty extensions`() {
        val parser = object : TextParser {
            override val supportedFormat = TextFormat(
                id = "noext",
                name = "No Extensions",
                defaultExtension = "",
                extensions = emptyList()
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

        assertNotNull(ParserRegistry.getParser(parser.supportedFormat))
    }

    @Test
    fun `should handle parser registration order`() {
        val parser1 = TestParser("first", "First")
        val parser2 = TestParser("second", "Second")
        val parser3 = TestParser("third", "Third")

        ParserRegistry.register(parser1)
        ParserRegistry.register(parser2)
        ParserRegistry.register(parser3)

        val parsers = ParserRegistry.getAllParsers()

        // Parsers should be retrievable regardless of order
        assertTrue(parsers.any { it.supportedFormat.id == "first" })
        assertTrue(parsers.any { it.supportedFormat.id == "second" })
        assertTrue(parsers.any { it.supportedFormat.id == "third" })
    }

    @Test
    fun `should handle multiple lookups efficiently`() {
        val parser = TestParser()
        ParserRegistry.register(parser)

        // Perform multiple lookups by format only (not by ID, since test format isn't in FormatRegistry)
        repeat(100) {
            assertNotNull(ParserRegistry.getParser(parser.supportedFormat))
            assertTrue(ParserRegistry.hasParser(parser.supportedFormat))
        }
    }
}
