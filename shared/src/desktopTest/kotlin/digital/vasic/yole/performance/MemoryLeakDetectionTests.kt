/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Memory Leak Detection Tests (JVM Desktop Only)
 *
 * Uses JVM Runtime to detect memory leaks by measuring
 * heap usage before and after operations with forced GC.
 * Validates bounded growth for document parsing,
 * DocumentCache eviction, StyleSheets caching, protocol
 * service lifecycle, and SecurityEventLogger capacity.
 *
 *########################################################*/

package digital.vasic.yole.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Memory leak detection tests using JVM Runtime.
 *
 * These tests are JVM-only (desktopTest) because they rely on
 * [Runtime.getRuntime] for heap measurement and [System.gc] for
 * garbage collection triggering.
 *
 * Strategy:
 * 1. Force GC and measure baseline heap
 * 2. Run the operation under test
 * 3. Clear references
 * 4. Force GC and measure final heap
 * 5. Assert heap growth is within 20% tolerance
 *
 * Total: 5 tests
 */
class MemoryLeakDetectionTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    /**
     * Force garbage collection and return approximate used heap in bytes.
     * Calls System.gc() multiple times to maximize collection.
     */
    private fun measureUsedHeap(): Long {
        val runtime = Runtime.getRuntime()
        // Multiple GC passes for thoroughness
        repeat(3) {
            System.gc()
            Thread.sleep(50)
        }
        System.gc()
        Thread.sleep(100)
        return runtime.totalMemory() - runtime.freeMemory()
    }

    // ==================== DOCUMENT PARSING: 1000 DOCUMENTS ====================

    @Test
    fun `parsing 1000 documents does not cause unbounded heap growth`() = runBlocking<Unit> {
        val parsers = listOf(
            MarkdownParser(), CsvParser(), LatexParser(),
            OrgModeParser(), TodoTxtParser(), AsciidocParser(), PlaintextParser()
        )
        val contents = listOf(
            "# Heading ${'$'}{it}\n\nParagraph with **bold** text and *italic*.\n\n- Item 1\n- Item 2",
            "name,age,city\nAlice,30,NYC\nBob,25,LA",
            "\\documentclass{article}\n\\begin{document}\nHello \\textbf{world}.\n\\end{document}",
            "* Heading\n** TODO Task\nContent.",
            "(A) Important task @work +proj",
            "= Title\n\nParagraph.",
            "Simple plain text content."
        )

        val baselineHeap = measureUsedHeap()

        // Parse 1000 documents, discard references as we go
        repeat(1000) { i ->
            val parser = parsers[i % parsers.size]
            val content = contents[i % contents.size]
            val doc = parser.parse(content)
            // Access fields to ensure they are fully realized
            doc.rawContent
            doc.format.id
            // Reference goes out of scope at end of each iteration
        }

        val afterParseHeap = measureUsedHeap()
        val growth = afterParseHeap - baselineHeap
        val growthPercent = if (baselineHeap > 0) (growth.toDouble() / baselineHeap) * 100 else 0.0

        println("Parsing 1000 docs: baseline=${baselineHeap / 1024}KB, after=${afterParseHeap / 1024}KB, " +
            "growth=${growth / 1024}KB (${growthPercent.toInt()}%)")

        // Allow 20% growth tolerance after GC
        // Note: JVM heap measurement is inherently approximate, so we use
        // a generous tolerance. The key assertion is no unbounded linear growth.
        assertTrue(growthPercent < 20.0 || growth < 10 * 1024 * 1024,
            "Heap growth should be bounded after parsing 1000 docs " +
                "(grew by ${growth / 1024}KB = ${growthPercent.toInt()}%)")
    }

    // ==================== DOCUMENT CACHE: PUT/EVICT CYCLES ====================

    @Test
    fun `DocumentCache with repeated put and evict cycles stays bounded`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val parser = MarkdownParser()

        val baselineHeap = measureUsedHeap()

        // Run 20 cycles of filling and partially clearing the cache
        repeat(20) { cycle ->
            // Fill cache beyond capacity
            repeat(100) { i ->
                val content = "# Cycle $cycle Doc $i\n\nContent with enough text to be meaningful."
                val doc = parser.parse(content)
                cache.put("cycle-$cycle-doc-$i", doc)
            }

            // Cache should auto-evict to stay at maxSize
            assertTrue(cache.size <= 50,
                "Cache size (${cache.size}) must not exceed maxSize (50) at cycle $cycle")

            // Invalidate some entries
            repeat(10) { i ->
                cache.invalidate("cycle-$cycle-doc-$i")
            }
        }

        // Clear the cache entirely
        cache.clear()
        assertEquals(0, cache.size, "Cache must be empty after clear()")

        val afterHeap = measureUsedHeap()
        val growth = afterHeap - baselineHeap
        val growthPercent = if (baselineHeap > 0) (growth.toDouble() / baselineHeap) * 100 else 0.0

        println("DocumentCache cycles: baseline=${baselineHeap / 1024}KB, after=${afterHeap / 1024}KB, " +
            "growth=${growth / 1024}KB (${growthPercent.toInt()}%)")

        assertTrue(growthPercent < 20.0 || growth < 10 * 1024 * 1024,
            "Heap growth should be bounded after cache put/evict cycles " +
                "(grew by ${growth / 1024}KB = ${growthPercent.toInt()}%)")
    }

    // ==================== STYLESHEETS: CACHE GROWTH IS BOUNDED ====================

    @Test
    fun `StyleSheets cache growth is bounded and clearCache resets`() = runBlocking<Unit> {
        StyleSheets.clearCache()
        val baselineHeap = measureUsedHeap()

        // Populate cache for all known format+theme combos multiple times
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT, TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE, TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX,
            TextFormat.ID_PLAINTEXT, TextFormat.ID_CSV, TextFormat.ID_TODOTXT,
            TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI, TextFormat.ID_KEYVALUE,
            TextFormat.ID_TASKPAPER, TextFormat.ID_TEXTILE, TextFormat.ID_JUPYTER,
            TextFormat.ID_RMARKDOWN, TextFormat.ID_BINARY
        )

        // Access each format+theme combo 100 times
        repeat(100) {
            formatIds.forEach { formatId ->
                StyleSheets.getStyleSheet(formatId, true)
                StyleSheets.getStyleSheet(formatId, false)
            }
        }

        val cacheSizeAfterPopulation = StyleSheets.cacheSize
        // Max possible unique keys = 18 formats * 2 themes = 34
        assertTrue(cacheSizeAfterPopulation <= 34,
            "StyleSheets cache should have at most 34 entries (got $cacheSizeAfterPopulation)")

        val afterPopulationHeap = measureUsedHeap()

        // Clear and verify
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache must be empty after clearCache()")

        val afterClearHeap = measureUsedHeap()
        val totalGrowth = afterClearHeap - baselineHeap
        val growthPercent = if (baselineHeap > 0) (totalGrowth.toDouble() / baselineHeap) * 100 else 0.0

        println("StyleSheets cache: baseline=${baselineHeap / 1024}KB, " +
            "populated=${afterPopulationHeap / 1024}KB (size=$cacheSizeAfterPopulation), " +
            "cleared=${afterClearHeap / 1024}KB, " +
            "net growth=${totalGrowth / 1024}KB (${growthPercent.toInt()}%)")

        assertTrue(growthPercent < 20.0 || totalGrowth < 5 * 1024 * 1024,
            "Heap should be bounded after StyleSheets populate+clear " +
                "(grew by ${totalGrowth / 1024}KB = ${growthPercent.toInt()}%)")
    }

    // ==================== PROTOCOL SERVICE CONFIGS: CREATE AND GC ====================

    @Test
    fun `creating and GC-ing 100 protocol service config instances`() = runBlocking<Unit> {
        val baselineHeap = measureUsedHeap()

        // Create 100 config instances of each type, then discard references
        repeat(100) { i ->
            val configs = listOf(
                StorageConfig.DropboxConfig(
                    name = "dropbox-$i", accessToken = "tok-$i",
                    appKey = "key-$i", appSecret = "sec-$i"
                ),
                StorageConfig.FtpConfig(
                    name = "ftp-$i", host = "host-$i",
                    username = "user-$i", password = "pass-$i"
                ),
                StorageConfig.SftpConfig(
                    name = "sftp-$i", host = "host-$i",
                    username = "user-$i", password = "pass-$i"
                ),
                StorageConfig.SmbConfig(
                    name = "smb-$i", host = "host-$i",
                    share = "share-$i", username = "user-$i", password = "pass-$i"
                ),
                StorageConfig.GoogleDriveConfig(
                    name = "gdrive-$i", clientId = "id-$i",
                    clientSecret = "sec-$i"
                ),
                StorageConfig.OneDriveConfig(
                    name = "onedrive-$i", clientId = "id-$i",
                    clientSecret = "sec-$i"
                ),
                StorageConfig.WebDavConfig(
                    name = "webdav-$i", url = "https://host-$i/dav",
                    username = "user-$i", password = "pass-$i"
                )
            )
            // Access fields to prevent compiler optimization
            configs.forEach { config ->
                config.name
                config.storageType
                config.isEnabled
            }
            // References go out of scope
        }

        val afterHeap = measureUsedHeap()
        val growth = afterHeap - baselineHeap
        val growthPercent = if (baselineHeap > 0) (growth.toDouble() / baselineHeap) * 100 else 0.0

        println("Protocol configs: baseline=${baselineHeap / 1024}KB, after=${afterHeap / 1024}KB, " +
            "growth=${growth / 1024}KB (${growthPercent.toInt()}%)")

        // 700 config objects should all be GC'd
        assertTrue(growthPercent < 20.0 || growth < 10 * 1024 * 1024,
            "Heap growth should be bounded after creating/GC-ing 700 config instances " +
                "(grew by ${growth / 1024}KB = ${growthPercent.toInt()}%)")
    }

    // ==================== SECURITY EVENT LOGGER: MAX EVENTS BOUNDED ====================

    @Test
    fun `SecurityEventLogger with maxEvents 100 does not grow beyond limit`() = runBlocking<Unit> {
        SecurityEventLogger.clear()
        SecurityEventLogger.maxEvents = 100

        val baselineHeap = measureUsedHeap()

        // Log 10000 events into a logger with maxEvents=100
        repeat(10000) { i ->
            when (i % 4) {
                0 -> SecurityEventLogger.logAuthFailure("TestService", "Failure $i")
                1 -> SecurityEventLogger.logPathTraversalBlocked("TestService", "../../path/$i")
                2 -> SecurityEventLogger.logCircuitBreakerOpen("TestService", i)
                else -> SecurityEventLogger.logTokenRefresh("TestService", i % 2 == 0)
            }
        }

        // Verify count is capped
        val eventCount = SecurityEventLogger.eventCount()
        assertTrue(eventCount <= 100,
            "Event count ($eventCount) must not exceed maxEvents (100)")
        assertTrue(eventCount == 100,
            "Event count should be exactly maxEvents after exceeding it (got $eventCount)")

        // Query events to verify they are accessible
        val allEvents = SecurityEventLogger.getEvents()
        assertEquals(100, allEvents.size, "getEvents() should return exactly 100 events")

        val afterHeap = measureUsedHeap()
        val growth = afterHeap - baselineHeap
        val growthPercent = if (baselineHeap > 0) (growth.toDouble() / baselineHeap) * 100 else 0.0

        println("SecurityEventLogger: baseline=${baselineHeap / 1024}KB, after=${afterHeap / 1024}KB, " +
            "growth=${growth / 1024}KB (${growthPercent.toInt()}%), events=$eventCount")

        // Clear and verify
        SecurityEventLogger.clear()
        assertEquals(0, SecurityEventLogger.eventCount(), "Logger must be empty after clear()")

        val afterClearHeap = measureUsedHeap()
        val clearGrowth = afterClearHeap - baselineHeap
        val clearGrowthPercent = if (baselineHeap > 0) (clearGrowth.toDouble() / baselineHeap) * 100 else 0.0

        assertTrue(clearGrowthPercent < 20.0 || clearGrowth < 5 * 1024 * 1024,
            "Heap should be bounded after SecurityEventLogger populate+clear " +
                "(grew by ${clearGrowth / 1024}KB = ${clearGrowthPercent.toInt()}%)")

        // Restore default
        SecurityEventLogger.maxEvents = 1000
    }
}
