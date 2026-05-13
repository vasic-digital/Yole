/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Parser Initializer Tests
 *
 * Tests for ParserInitializer covering eager and lazy
 * registration, initialization status tracking, parser
 * statistics, idempotent re-registration, and registry
 * state after initialization.
 *
 *########################################################*/

package digital.vasic.yole.format

import kotlin.test.*

/**
 * Comprehensive tests for [ParserInitializer].
 *
 * Verifies both eager and lazy parser registration strategies,
 * initialization status reporting, parser statistics, idempotent
 * re-registration behavior, and registry consistency.
 */
class ParserInitializerTest {

    /**
     * All 18 format IDs that ParserInitializer registers.
     */
    private val allFormatIds = listOf(
        FormatRegistry.ID_PLAINTEXT,
        FormatRegistry.ID_MARKDOWN,
        FormatRegistry.ID_TODOTXT,
        FormatRegistry.ID_CSV,
        FormatRegistry.ID_WIKITEXT,
        FormatRegistry.ID_CREOLE,
        FormatRegistry.ID_TIDDLYWIKI,
        FormatRegistry.ID_LATEX,
        FormatRegistry.ID_ASCIIDOC,
        FormatRegistry.ID_ORGMODE,
        FormatRegistry.ID_RESTRUCTUREDTEXT,
        FormatRegistry.ID_KEYVALUE,
        FormatRegistry.ID_TASKPAPER,
        FormatRegistry.ID_TEXTILE,
        FormatRegistry.ID_JUPYTER,
        FormatRegistry.ID_RMARKDOWN,
        FormatRegistry.ID_JSON,
        FormatRegistry.ID_BINARY
    )

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
    }

    // ==================== registerAllParsers() Tests ====================

    @Test
    fun `registerAllParsers registers all 18 format parsers`() {
        ParserInitializer.registerAllParsers()

        val registeredParsers = ParserRegistry.getAllParsers()
        assertEquals(18, registeredParsers.size, "Should register exactly 18 parsers")
    }

    @Test
    fun `registerAllParsers registers parsers for every expected format ID`() {
        ParserInitializer.registerAllParsers()

        for (formatId in allFormatIds) {
            val format = FormatRegistry.getById(formatId)
            assertNotNull(format, "FormatRegistry should contain format '$formatId'")
            assertTrue(
                ParserRegistry.hasParser(format),
                "ParserRegistry should have parser for '$formatId'"
            )
        }
    }

    @Test
    fun `registerAllParsers creates immediately instantiated parsers`() {
        ParserInitializer.registerAllParsers()

        // Eager registration means all parsers are instantiated, none pending
        assertEquals(18, ParserRegistry.getInstantiatedParserCount(),
            "All 18 parsers should be instantiated immediately")
        assertEquals(0, ParserRegistry.getPendingParserCount(),
            "No parsers should be pending (lazy) after eager registration")
    }

    @Test
    fun `registerAllParsers parsers can parse content`() {
        ParserInitializer.registerAllParsers()

        val parser = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser, "Markdown parser should be retrievable")

        val doc = parser.parse("# Hello")
        assertEquals("Markdown", doc.format.name)
    }

    @Test
    fun `registerAllParsers each parser reports correct supported format`() {
        ParserInitializer.registerAllParsers()

        val parsers = ParserRegistry.getAllParsers()
        val registeredFormatIds = parsers.map { it.supportedFormat.id }.toSet()

        for (formatId in allFormatIds) {
            assertTrue(
                formatId in registeredFormatIds,
                "Parser for format '$formatId' should be in the registered set"
            )
        }
    }

    // ==================== registerAllParsersLazy() Tests ====================

    @Test
    fun `registerAllParsersLazy registers all 18 formats`() {
        ParserInitializer.registerAllParsersLazy()

        for (formatId in allFormatIds) {
            val format = FormatRegistry.getById(formatId)
            assertNotNull(format, "FormatRegistry should contain format '$formatId'")
            assertTrue(
                ParserRegistry.hasParser(format),
                "ParserRegistry should have parser for '$formatId' after lazy registration"
            )
        }
    }

    @Test
    fun `registerAllParsersLazy does not instantiate parsers immediately`() {
        ParserInitializer.registerAllParsersLazy()

        assertEquals(0, ParserRegistry.getInstantiatedParserCount(),
            "No parsers should be instantiated immediately after lazy registration")
        assertEquals(18, ParserRegistry.getPendingParserCount(),
            "All 18 parsers should be pending (lazy)")
    }

    @Test
    fun `registerAllParsersLazy instantiates parser on first access`() {
        ParserInitializer.registerAllParsersLazy()

        val pendingBefore = ParserRegistry.getPendingParserCount()
        val instantiatedBefore = ParserRegistry.getInstantiatedParserCount()

        // Access one parser
        val parser = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser, "Markdown parser should be retrievable after lazy registration")

        assertEquals(pendingBefore - 1, ParserRegistry.getPendingParserCount(),
            "Pending count should decrease by 1 after accessing a parser")
        assertEquals(instantiatedBefore + 1, ParserRegistry.getInstantiatedParserCount(),
            "Instantiated count should increase by 1 after accessing a parser")
    }

    @Test
    fun `registerAllParsersLazy subsequent access returns same instance`() {
        ParserInitializer.registerAllParsersLazy()

        val parser1 = ParserRegistry.getParser(FormatRegistry.ID_CSV)
        val parser2 = ParserRegistry.getParser(FormatRegistry.ID_CSV)

        assertNotNull(parser1)
        assertNotNull(parser2)
        assertSame(parser1, parser2, "Subsequent calls should return the same parser instance")
    }

    @Test
    fun `registerAllParsersLazy instantiates only accessed parsers`() {
        ParserInitializer.registerAllParsersLazy()

        // Access only two parsers
        ParserRegistry.getParser(FormatRegistry.ID_PLAINTEXT)
        ParserRegistry.getParser(FormatRegistry.ID_LATEX)

        assertEquals(2, ParserRegistry.getInstantiatedParserCount(),
            "Only 2 parsers should be instantiated")
        assertEquals(16, ParserRegistry.getPendingParserCount(),
            "16 parsers should still be pending (18 total - 2 accessed)")
    }

    @Test
    fun `registerAllParsersLazy hasParser does not trigger instantiation`() {
        ParserInitializer.registerAllParsersLazy()

        val pendingBefore = ParserRegistry.getPendingParserCount()
        val instantiatedBefore = ParserRegistry.getInstantiatedParserCount()

        // Check hasParser for all formats
        for (formatId in allFormatIds) {
            val format = FormatRegistry.getById(formatId)!!
            assertTrue(ParserRegistry.hasParser(format))
        }

        assertEquals(pendingBefore, ParserRegistry.getPendingParserCount(),
            "hasParser should not change pending count")
        assertEquals(instantiatedBefore, ParserRegistry.getInstantiatedParserCount(),
            "hasParser should not change instantiated count")
    }

    @Test
    fun `registerAllParsersLazy lazily created parsers can parse content`() {
        ParserInitializer.registerAllParsersLazy()

        val parser = ParserRegistry.getParser(FormatRegistry.ID_TODOTXT)
        assertNotNull(parser, "TodoTxt parser should be retrievable")

        val doc = parser.parse("(A) Buy groceries @store +shopping")
        assertEquals("Todo.txt", doc.format.name)
    }

    @Test
    fun `registerAllParsersLazy accessing all parsers transitions all from pending to instantiated`() {
        ParserInitializer.registerAllParsersLazy()

        // Access all parsers
        for (formatId in allFormatIds) {
            val parser = ParserRegistry.getParser(formatId)
            assertNotNull(parser, "Parser for '$formatId' should be retrievable")
        }

        assertEquals(18, ParserRegistry.getInstantiatedParserCount(),
            "All 18 parsers should be instantiated after accessing each one")
        assertEquals(0, ParserRegistry.getPendingParserCount(),
            "No parsers should be pending after accessing all")
    }

    // ==================== getInitializationStatus() Tests ====================

    @Test
    fun `getInitializationStatus returns all false when no parsers registered`() {
        val status = ParserInitializer.getInitializationStatus()

        // FormatRegistry has 23 formats total (18 text + 5 network storage)
        assertTrue(status.isNotEmpty(), "Status map should not be empty")

        // Check that text format parsers show as not registered
        val markdownStatus = status["Markdown"]
        assertNotNull(markdownStatus, "Status should contain 'Markdown' key")
        assertFalse(markdownStatus, "Markdown should be false when no parsers registered")
    }

    @Test
    fun `getInitializationStatus returns true for all formats after eager registration`() {
        ParserInitializer.registerAllParsers()

        val status = ParserInitializer.getInitializationStatus()

        // All 18 parser format names should be true
        val expectedNames = listOf(
            "Plain Text", "Markdown", "Todo.txt", "CSV",
            "WikiText", "Creole", "TiddlyWiki",
            "LaTeX", "AsciiDoc", "Org Mode", "reStructuredText",
            "Key-Value", "TaskPaper", "Textile",
            "Jupyter Notebook", "R Markdown", "Binary"
        )

        for (name in expectedNames) {
            val registered = status[name]
            assertNotNull(registered, "Status should contain '$name'")
            assertTrue(registered, "'$name' should be registered")
        }
    }

    @Test
    fun `getInitializationStatus returns true for all formats after lazy registration`() {
        ParserInitializer.registerAllParsersLazy()

        val status = ParserInitializer.getInitializationStatus()

        // Even lazily registered parsers should show as available
        val markdownStatus = status["Markdown"]
        assertNotNull(markdownStatus)
        assertTrue(markdownStatus,
            "Lazily registered parsers should show as available in initialization status")
    }

    @Test
    fun `getInitializationStatus covers all FormatRegistry formats`() {
        val status = ParserInitializer.getInitializationStatus()

        // The status map should have an entry for every format in FormatRegistry
        for (format in FormatRegistry.formats) {
            assertTrue(
                status.containsKey(format.name),
                "Status should contain entry for '${format.name}'"
            )
        }
        assertEquals(FormatRegistry.formats.size, status.size,
            "Status map size should match FormatRegistry format count")
    }

    @Test
    fun `getInitializationStatus network formats are false without network parser registration`() {
        ParserInitializer.registerAllParsers()

        val status = ParserInitializer.getInitializationStatus()

        // Network storage formats (Dropbox, FTP, Google Drive, OneDrive, SFTP)
        // are NOT registered by ParserInitializer
        val networkFormatNames = listOf("Dropbox", "FTP", "Google Drive", "OneDrive", "SFTP")
        for (name in networkFormatNames) {
            val registered = status[name]
            assertNotNull(registered, "Status should contain '$name'")
            assertFalse(registered,
                "'$name' should not be registered (ParserInitializer does not register network formats)")
        }
    }

    // ==================== getParserStatistics() Tests ====================

    @Test
    fun `getParserStatistics returns zero when no parsers registered`() {
        val stats = ParserInitializer.getParserStatistics()

        assertEquals(0, stats["total_parsers"], "Total parsers should be 0 with no registration")

        @Suppress("UNCHECKED_CAST")
        val supportedFormats = stats["supported_formats"] as List<String>
        assertTrue(supportedFormats.isEmpty(), "Supported formats should be empty")

        @Suppress("UNCHECKED_CAST")
        val missingFormats = stats["missing_formats"] as List<String>
        assertTrue(missingFormats.isNotEmpty(), "Missing formats should not be empty")
    }

    @Test
    fun `getParserStatistics after eager registration shows 18 parsers`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()

        assertEquals(18, stats["total_parsers"],
            "Total parsers should be 18 after eager registration")
    }

    @Test
    fun `getParserStatistics supported_formats lists all registered format names`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()

        @Suppress("UNCHECKED_CAST")
        val supportedFormats = stats["supported_formats"] as List<String>

        assertTrue(supportedFormats.contains("Markdown"), "Should list Markdown")
        assertTrue(supportedFormats.contains("Plain Text"), "Should list Plain Text")
        assertTrue(supportedFormats.contains("LaTeX"), "Should list LaTeX")
        assertTrue(supportedFormats.contains("Binary"), "Should list Binary")
        assertEquals(18, supportedFormats.size, "Should list exactly 18 supported formats")
    }

    @Test
    fun `getParserStatistics missing_formats lists network formats`() {
        ParserInitializer.registerAllParsers()

        val stats = ParserInitializer.getParserStatistics()

        @Suppress("UNCHECKED_CAST")
        val missingFormats = stats["missing_formats"] as List<String>

        // Network storage formats are not registered by ParserInitializer
        assertTrue(missingFormats.contains("Dropbox"), "Dropbox should be missing")
        assertTrue(missingFormats.contains("FTP"), "FTP should be missing")
        assertTrue(missingFormats.contains("Google Drive"), "Google Drive should be missing")
        assertTrue(missingFormats.contains("OneDrive"), "OneDrive should be missing")
        assertTrue(missingFormats.contains("SFTP"), "SFTP should be missing")
    }

    @Test
    fun `getParserStatistics after lazy registration only counts instantiated parsers`() {
        ParserInitializer.registerAllParsersLazy()

        // Before accessing any parser
        val statsBefore = ParserInitializer.getParserStatistics()
        assertEquals(0, statsBefore["total_parsers"],
            "getAllParsers only returns instantiated parsers, so total should be 0")

        // Access one parser
        ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)

        val statsAfter = ParserInitializer.getParserStatistics()
        assertEquals(1, statsAfter["total_parsers"],
            "After accessing one parser, total should be 1")
    }

    @Test
    fun `getParserStatistics contains all required keys`() {
        val stats = ParserInitializer.getParserStatistics()

        assertTrue(stats.containsKey("total_parsers"), "Stats should contain 'total_parsers'")
        assertTrue(stats.containsKey("supported_formats"), "Stats should contain 'supported_formats'")
        assertTrue(stats.containsKey("missing_formats"), "Stats should contain 'missing_formats'")
        assertEquals(3, stats.size, "Stats should contain exactly 3 keys")
    }

    // ==================== Idempotent Re-registration Tests ====================

    @Test
    fun `eager re-registration after clear succeeds`() {
        ParserInitializer.registerAllParsers()
        assertEquals(18, ParserRegistry.getInstantiatedParserCount())

        ParserRegistry.clear()
        assertEquals(0, ParserRegistry.getInstantiatedParserCount())

        // Re-register should work without error
        ParserInitializer.registerAllParsers()
        assertEquals(18, ParserRegistry.getInstantiatedParserCount())
    }

    @Test
    fun `lazy re-registration after clear succeeds`() {
        ParserInitializer.registerAllParsersLazy()
        assertEquals(18, ParserRegistry.getPendingParserCount())

        ParserRegistry.clear()
        assertEquals(0, ParserRegistry.getPendingParserCount())

        // Re-register should work without error
        ParserInitializer.registerAllParsersLazy()
        assertEquals(18, ParserRegistry.getPendingParserCount())
    }

    @Test
    fun `duplicate eager registration silently skips`() {
        ParserInitializer.registerAllParsers()

        // Should not throw — silently skips already-registered parsers
        ParserInitializer.registerAllParsers()

        // Verify parsers still work after duplicate registration
        assertNotNull(ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN))
    }

    @Test
    fun `duplicate lazy registration silently skips`() {
        ParserInitializer.registerAllParsersLazy()

        // Should not throw — silently skips already-registered parsers
        ParserInitializer.registerAllParsersLazy()
    }

    @Test
    fun `lazy then eager registration silently skips`() {
        ParserInitializer.registerAllParsersLazy()

        // Should not throw — silently skips already-registered parsers
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun `eager then lazy registration silently skips`() {
        ParserInitializer.registerAllParsers()

        // Should not throw — silently skips already-registered parsers
        ParserInitializer.registerAllParsersLazy()
    }

    @Test
    fun `clear and re-register preserves parser functionality`() {
        ParserInitializer.registerAllParsers()
        val parser1 = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser1)
        val doc1 = parser1.parse("# Test")

        ParserRegistry.clear()
        ParserInitializer.registerAllParsersLazy()
        val parser2 = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser2)
        val doc2 = parser2.parse("# Test")

        assertEquals(doc1.format.id, doc2.format.id,
            "Parsers should produce documents with same format after re-registration")
        assertEquals(doc1.rawContent, doc2.rawContent,
            "Parsers should produce documents with same raw content after re-registration")
    }

    // ==================== Registry State After Initialization Tests ====================

    @Test
    fun `registry is empty before any initialization`() {
        assertEquals(0, ParserRegistry.getInstantiatedParserCount())
        assertEquals(0, ParserRegistry.getPendingParserCount())
        assertTrue(ParserRegistry.getAllParsers().isEmpty())
    }

    @Test
    fun `registry state after eager initialization is fully populated`() {
        ParserInitializer.registerAllParsers()

        val allParsers = ParserRegistry.getAllParsers()
        assertEquals(18, allParsers.size)
        assertEquals(18, ParserRegistry.getInstantiatedParserCount())
        assertEquals(0, ParserRegistry.getPendingParserCount())
    }

    @Test
    fun `registry state after lazy initialization has only pending parsers`() {
        ParserInitializer.registerAllParsersLazy()

        val allParsers = ParserRegistry.getAllParsers()
        assertEquals(0, allParsers.size, "getAllParsers should return empty for lazy-only parsers")
        assertEquals(0, ParserRegistry.getInstantiatedParserCount())
        assertEquals(18, ParserRegistry.getPendingParserCount())
    }

    @Test
    fun `getParser returns null for unregistered format`() {
        ParserInitializer.registerAllParsers()

        // Network formats are not registered
        val dropboxParser = ParserRegistry.getParser(FormatRegistry.ID_DROPBOX)
        assertNull(dropboxParser, "Should return null for unregistered format (dropbox)")
    }

    @Test
    fun `getParser returns null for unknown format ID`() {
        ParserInitializer.registerAllParsers()

        val parser = ParserRegistry.getParser("nonexistent_format")
        assertNull(parser, "Should return null for unknown format ID")
    }

    @Test
    fun `clear removes all parsers and factories`() {
        ParserInitializer.registerAllParsersLazy()

        // Access a few to create a mix of instantiated and pending
        ParserRegistry.getParser(FormatRegistry.ID_PLAINTEXT)
        ParserRegistry.getParser(FormatRegistry.ID_CSV)
        assertTrue(ParserRegistry.getInstantiatedParserCount() > 0)
        assertTrue(ParserRegistry.getPendingParserCount() > 0)

        ParserRegistry.clear()

        assertEquals(0, ParserRegistry.getInstantiatedParserCount(),
            "Instantiated count should be 0 after clear")
        assertEquals(0, ParserRegistry.getPendingParserCount(),
            "Pending count should be 0 after clear")
        assertTrue(ParserRegistry.getAllParsers().isEmpty(),
            "getAllParsers should be empty after clear")
    }

    @Test
    fun `each registered parser has unique format ID`() {
        ParserInitializer.registerAllParsers()

        val parsers = ParserRegistry.getAllParsers()
        val formatIds = parsers.map { it.supportedFormat.id }
        val uniqueIds = formatIds.toSet()

        assertEquals(formatIds.size, uniqueIds.size,
            "All registered parsers should have unique format IDs")
    }

    @Test
    fun `eager and lazy registration produce equivalent parser coverage`() {
        // Eager registration
        ParserInitializer.registerAllParsers()
        val eagerStatus = ParserInitializer.getInitializationStatus()

        ParserRegistry.clear()

        // Lazy registration
        ParserInitializer.registerAllParsersLazy()
        val lazyStatus = ParserInitializer.getInitializationStatus()

        assertEquals(eagerStatus, lazyStatus,
            "Eager and lazy registration should produce identical initialization status")
    }

    @Test
    fun `all registered parsers can parse empty content without error`() {
        ParserInitializer.registerAllParsers()

        val parsers = ParserRegistry.getAllParsers()
        for (parser in parsers) {
            val doc = parser.parse("")
            assertNotNull(doc, "Parser for '${parser.supportedFormat.id}' should handle empty content")
            assertEquals(parser.supportedFormat.id, doc.format.id,
                "Parsed document format should match parser's supported format")
        }
    }

    @Test
    fun `lazy registration followed by full access matches eager registration parser count`() {
        // Lazy path
        ParserInitializer.registerAllParsersLazy()
        for (formatId in allFormatIds) {
            ParserRegistry.getParser(formatId)
        }
        val lazyCount = ParserRegistry.getInstantiatedParserCount()

        ParserRegistry.clear()

        // Eager path
        ParserInitializer.registerAllParsers()
        val eagerCount = ParserRegistry.getInstantiatedParserCount()

        assertEquals(eagerCount, lazyCount,
            "Lazy registration with full access should produce same instantiated count as eager")
    }
}
