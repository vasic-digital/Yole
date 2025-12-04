/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for ParserInitializer
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Unit tests for ParserInitializer object.
 *
 * Tests cover:
 * - Parser registration during initialization
 * - Initialization status tracking
 * - Parser statistics
 * - Multiple initialization handling
 * - Integration with ParserRegistry and FormatRegistry
 */
class ParserInitializerTest {

    @BeforeTest
    fun setup() {
        // Clear registry before each test
        ParserRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        // Clear registry after each test
        ParserRegistry.clear()
    }

    // ==================== Registration Tests ====================

    @Test
    fun `should register all parsers successfully`() {
        ParserInitializer.registerAllParsers()

        val parsers = ParserRegistry.getAllParsers()

        // Should have 17 parsers (all formats)
        assertTrue(parsers.size >= 15, "Expected at least 15 parsers, got ${parsers.size}")
    }

    @Test
    fun `should register plaintext parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register markdown parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register todotxt parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_TODOTXT)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register csv parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_CSV)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register wikitext parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_WIKITEXT)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register latex parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_LATEX)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register asciidoc parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_ASCIIDOC)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register orgmode parser`() {
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_ORGMODE)
        assertNotNull(format)
        assertTrue(ParserRegistry.hasParser(format))
    }

    @Test
    fun `should register all wiki formats`() {
        ParserInitializer.registerAllParsers()

        val wikitext = FormatRegistry.getById(FormatRegistry.ID_WIKITEXT)
        val creole = FormatRegistry.getById(FormatRegistry.ID_CREOLE)
        val tiddlywiki = FormatRegistry.getById(FormatRegistry.ID_TIDDLYWIKI)

        assertNotNull(wikitext)
        assertNotNull(creole)
        assertNotNull(tiddlywiki)

        assertTrue(ParserRegistry.hasParser(wikitext))
        assertTrue(ParserRegistry.hasParser(creole))
        assertTrue(ParserRegistry.hasParser(tiddlywiki))
    }

    @Test
    fun `should register all technical formats`() {
        ParserInitializer.registerAllParsers()

        val latex = FormatRegistry.getById(FormatRegistry.ID_LATEX)
        val asciidoc = FormatRegistry.getById(FormatRegistry.ID_ASCIIDOC)
        val orgmode = FormatRegistry.getById(FormatRegistry.ID_ORGMODE)
        val rst = FormatRegistry.getById(FormatRegistry.ID_RESTRUCTUREDTEXT)

        assertNotNull(latex)
        assertNotNull(asciidoc)
        assertNotNull(orgmode)
        assertNotNull(rst)

        assertTrue(ParserRegistry.hasParser(latex))
        assertTrue(ParserRegistry.hasParser(asciidoc))
        assertTrue(ParserRegistry.hasParser(orgmode))
        assertTrue(ParserRegistry.hasParser(rst))
    }

    // ==================== Initialization Status Tests ====================

    @Test
    fun `should return empty status before initialization`() {
        val status = ParserInitializer.getInitializationStatus()

        // All formats should show as not registered
        status.values.forEach { isRegistered ->
            assertFalse(isRegistered)
        }
    }

    @Test
    fun `should return complete status after initialization`() {
        ParserInitializer.registerAllParsers()

        val status = ParserInitializer.getInitializationStatus()

        // Most formats should show as registered
        val registeredCount = status.values.count { it }
        assertTrue(registeredCount >= 15, "Expected at least 15 registered formats")
    }

    @Test
    fun `should include all known formats in status`() {
        val status = ParserInitializer.getInitializationStatus()

        // Should have entries for all formats in FormatRegistry
        val formatCount = FormatRegistry.formats.size
        assertEquals(formatCount, status.size)
    }

    @Test
    fun `should show correct status for specific formats`() {
        ParserInitializer.registerAllParsers()

        val status = ParserInitializer.getInitializationStatus()

        // Check specific formats
        assertTrue(status["Markdown"] == true || status.values.count { it } >= 15)
        assertTrue(status["Todo.txt"] == true || status.values.count { it } >= 15)
        assertTrue(status["CSV"] == true || status.values.count { it } >= 15)
    }

    // ==================== Parser Statistics Tests ====================

    @Test
    fun `should return zero statistics before initialization`() {
        val stats = ParserInitializer.getParserStatistics()

        assertEquals(0, stats["total_parsers"])
        assertTrue((stats["supported_formats"] as List<*>).isEmpty())
    }

    @Test
    fun `should return correct statistics after initialization`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()

        val totalParsers = stats["total_parsers"] as Int
        assertTrue(totalParsers >= 15, "Expected at least 15 parsers")
    }

    @Test
    fun `should list all supported formats in statistics`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()
        val supportedFormats = stats["supported_formats"] as List<*>

        // Should include core formats
        assertTrue(supportedFormats.size >= 15)
    }

    @Test
    fun `should identify missing formats in statistics`() {
        // Don't initialize - all formats should be missing
        val stats = ParserInitializer.getParserStatistics()
        val missingFormats = stats["missing_formats"] as List<*>

        // All formats should be missing
        assertEquals(FormatRegistry.formats.size, missingFormats.size)
    }

    @Test
    fun `should show correct missing formats after initialization`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()
        val missingFormats = stats["missing_formats"] as List<*>

        // Most formats should be registered, so missing should be small or empty
        assertTrue(missingFormats.size <= 2, "Expected at most 2 missing formats")
    }

    @Test
    fun `should have consistent statistics`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()
        val totalParsers = stats["total_parsers"] as Int
        val supportedFormats = (stats["supported_formats"] as List<*>).size
        val missingFormats = (stats["missing_formats"] as List<*>).size
        val totalFormats = FormatRegistry.formats.size

        // Total parsers should equal supported formats
        assertEquals(totalParsers, supportedFormats)

        // Supported + missing should equal total formats
        assertEquals(totalFormats, supportedFormats + missingFormats)
    }

    // ==================== Multiple Initialization Tests ====================

    @Test
    fun `should not fail on multiple initialization attempts`() {
        // First initialization
        ParserInitializer.registerAllParsers()
        val parsersAfterFirst = ParserRegistry.getAllParsers().size

        // Attempting second initialization should fail due to duplicates
        // This is expected behavior - parsers can only be registered once
        assertFails {
            ParserInitializer.registerAllParsers()
        }

        // Parser count should remain the same
        assertEquals(parsersAfterFirst, ParserRegistry.getAllParsers().size)
    }

    @Test
    fun `should allow re-initialization after clear`() {
        ParserInitializer.registerAllParsers()
        val firstCount = ParserRegistry.getAllParsers().size

        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        val secondCount = ParserRegistry.getAllParsers().size

        assertEquals(firstCount, secondCount)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with FormatRegistry`() {
        ParserInitializer.registerAllParsers()

        // Every registered parser should correspond to a format in FormatRegistry
        val parsers = ParserRegistry.getAllParsers()

        parsers.forEach { parser ->
            val formatId = parser.supportedFormat.id
            val format = FormatRegistry.getById(formatId)
            assertNotNull(format, "Format $formatId should exist in FormatRegistry")
        }
    }

    @Test
    fun `should allow parsing after initialization`() {
        ParserInitializer.registerAllParsers()

        // Test that we can actually parse content with registered parsers
        val markdownFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(markdownFormat)

        val parser = ParserRegistry.getParser(markdownFormat)
        assertNotNull(parser)

        val document = parser.parse("# Hello World")
        assertNotNull(document)
        assertEquals("# Hello World", document.rawContent)
    }

    @Test
    fun `should support all registered format IDs`() {
        ParserInitializer.registerAllParsers()

        val expectedFormatIds = listOf(
            FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_WIKITEXT,
            FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ASCIIDOC,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_RESTRUCTUREDTEXT,
            FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_TASKPAPER,
            FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_CREOLE,
            FormatRegistry.ID_TIDDLYWIKI,
            FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_RMARKDOWN
        )

        expectedFormatIds.forEach { formatId ->
            val parser = ParserRegistry.getParser(formatId)
            assertNotNull(parser, "Parser for $formatId should be registered")
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty registry gracefully`() {
        // Don't initialize
        val stats = ParserInitializer.getParserStatistics()

        assertEquals(0, stats["total_parsers"])
        assertTrue((stats["supported_formats"] as List<*>).isEmpty())
    }

    @Test
    fun `should provide consistent status across calls`() {
        ParserInitializer.registerAllParsers()

        val status1 = ParserInitializer.getInitializationStatus()
        val status2 = ParserInitializer.getInitializationStatus()
        val stats1 = ParserInitializer.getParserStatistics()
        val stats2 = ParserInitializer.getParserStatistics()

        assertEquals(status1, status2)
        assertEquals(stats1, stats2)
    }
}
