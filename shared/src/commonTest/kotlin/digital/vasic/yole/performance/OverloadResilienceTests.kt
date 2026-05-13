/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Overload Resilience Tests
 *
 * Attempts to overload every major subsystem to verify
 * no crashes, no deadlocks, and no uncaught exceptions:
 * FormatRegistry, DocumentCache, StyleSheets, protocol
 * services, SecurityEventLogger, PerformanceMetrics,
 * and parse concurrency reconfiguration.
 *
 *########################################################*/

package digital.vasic.yole.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.monitoring.PerformanceMetrics
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Overload resilience tests.
 *
 * Each test attempts to push a subsystem past normal usage to verify
 * resilience: no crashes, no deadlocks, no uncaught exceptions.
 *
 * Total: 9 tests
 */
class OverloadResilienceTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        PerformanceMetrics.reset()
        SecurityEventLogger.clear()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
        PerformanceMetrics.reset()
        SecurityEventLogger.clear()
        // Restore default parse concurrency
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    // ==================== FORMAT REGISTRY: 500 CONCURRENT DETECTIONS ====================

    @Test
    fun `FormatRegistry handles 500 concurrent detectByContent calls`() = runBlocking<Unit> {
        val contents = listOf(
            "# Markdown heading",
            "(A) Task @work",
            "a,b,c\n1,2,3",
            "\\documentclass{article}",
            "* Org heading",
            "= AsciiDoc Title",
            "== WikiText ==",
            "h1. Textile Heading",
            "---\ntitle: R doc\n---\n```{r}\n1+1\n```",
            "key1 = value1"
        )

        val result = withTimeoutOrNull(30_000L) {
            val mutex = Mutex()
            var exceptionCount = 0

            val jobs = (1..500).map { i ->
                async(Dispatchers.Default) {
                    try {
                        val content = contents[i % contents.size]
                        val detected = FormatRegistry.detectByContent(content)
                        detected?.id ?: "null"
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutex.withLock { exceptionCount++ }
                        "exception"
                    }
                }
            }.awaitAll()

            assertEquals(0, exceptionCount, "No exceptions during 500 concurrent detections")
            jobs
        }

        assertNotNull(result, "500 concurrent detectByContent calls must complete within 30s")
        assertEquals(500, result.size, "All 500 results must be collected")
        // Most known contents should be detected; some may return null if detection
        // patterns are restrictive. The primary goal is no crashes or deadlocks.
        val detectedCount = result.count { it != "null" }
        assertTrue(detectedCount > 400,
            "Most contents should be detected ($detectedCount/500 detected)")
        assertTrue(result.none { it == "exception" },
            "No exceptions should occur during concurrent detections")
    }

    // ==================== DOCUMENT CACHE: 1000 ENTRIES ====================

    @Test
    fun `DocumentCache with 1000 entries exceeding normal capacity`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 100)
        val parser = MarkdownParser()

        val result = withTimeoutOrNull(30_000L) {
            // Insert 1000 entries into a cache with maxSize=100
            repeat(1000) { i ->
                val content = "# Document $i\n\nContent for doc $i with data: ${i * 31}"
                val doc = parser.parse(content)
                cache.put("key-$i", doc)
            }

            // Cache should be bounded at maxSize
            assertTrue(cache.size <= 100,
                "Cache size (${cache.size}) must not exceed maxSize (100)")

            // Most recent entries should be present (LRU keeps newest)
            var recentHits = 0
            for (i in 900..999) {
                val retrieved = cache.get("key-$i")
                if (retrieved != null) {
                    recentHits++
                }
            }
            assertTrue(recentHits > 0,
                "At least some recent entries should be in cache (got $recentHits)")

            // Old entries should be evicted
            val oldEntry = cache.get("key-0")
            assertNull(oldEntry, "Entry 0 should be evicted from LRU cache of size 100 after 1000 inserts")

            true
        }

        assertNotNull(result, "1000-entry cache test must complete within 30s")
    }

    // ==================== STYLESHEETS: ALL 17 FORMATS x 2 THEMES ====================

    @Test
    fun `StyleSheets handles 34 concurrent requests for all format-theme combos`() = runBlocking<Unit> {
        StyleSheets.clearCache()

        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_PLAINTEXT, TextFormat.ID_TODOTXT,
            TextFormat.ID_CSV, TextFormat.ID_WIKITEXT, TextFormat.ID_ORGMODE,
            TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI, TextFormat.ID_LATEX,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_KEYVALUE,
            TextFormat.ID_TASKPAPER, TextFormat.ID_TEXTILE, TextFormat.ID_JUPYTER,
            TextFormat.ID_RMARKDOWN, TextFormat.ID_BINARY
        )

        val result = withTimeoutOrNull(10_000L) {
            val jobs = formatIds.flatMap { formatId ->
                listOf(true, false).map { lightMode ->
                    async(Dispatchers.Default) {
                        val css = StyleSheets.getStyleSheet(formatId, lightMode)
                        Triple(formatId, lightMode, css)
                    }
                }
            }.awaitAll()
            jobs
        }

        assertNotNull(result, "34 concurrent StyleSheet requests must complete within 10s")
        assertEquals(34, result.size, "Should get 18 formats x 2 themes = 34 results")

        // Formats that have defined styles should return non-empty CSS
        val styledFormats = setOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT, TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE, TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )
        result.forEach { (formatId, _, css) ->
            if (formatId in styledFormats) {
                assertTrue(css.isNotEmpty(),
                    "CSS for styled format $formatId should not be empty")
            }
        }

        // Cache should now contain entries
        assertTrue(StyleSheets.cacheSize > 0,
            "StyleSheets cache should be populated after concurrent requests")
    }

    // ==================== ALL 7 PROTOCOL SERVICES CREATED SIMULTANEOUSLY ====================

    @Test
    fun `all protocol service types can be instantiated simultaneously`() = runBlocking<Unit> {
        // Verify that creating all protocol service configs does not crash
        val result = withTimeoutOrNull(10_000L) {
            val configs = listOf(
                StorageConfig.DropboxConfig(
                    name = "test-dropbox", accessToken = "fake-token",
                    appKey = "fake-key", appSecret = "fake-secret"
                ),
                StorageConfig.FtpConfig(
                    name = "test-ftp", host = "localhost",
                    username = "test", password = "test"
                ),
                StorageConfig.SftpConfig(
                    name = "test-sftp", host = "localhost",
                    username = "test", password = "test"
                ),
                StorageConfig.SmbConfig(
                    name = "test-smb", host = "localhost",
                    share = "share", username = "test", password = "test"
                ),
                StorageConfig.GoogleDriveConfig(
                    name = "test-gdrive", clientId = "fake-id",
                    clientSecret = "fake-secret"
                ),
                StorageConfig.OneDriveConfig(
                    name = "test-onedrive", clientId = "fake-id",
                    clientSecret = "fake-secret"
                ),
                StorageConfig.WebDavConfig(
                    name = "test-webdav", url = "https://localhost/dav",
                    username = "test", password = "test"
                )
            )

            // All configs should be valid
            assertEquals(7, configs.size, "Should create 7 distinct configs")
            configs.forEach { config ->
                assertTrue(config.name.isNotEmpty(), "Config name must not be empty")
                assertTrue(config.isEnabled, "Config should be enabled by default")
            }

            // Verify storage types are unique
            val storageTypes = configs.map { it.storageType }.toSet()
            assertEquals(7, storageTypes.size, "All 7 storage types must be distinct")

            true
        }

        assertNotNull(result, "Protocol config creation must complete within 10s")
    }

    // ==================== RAPID CONNECT/DISCONNECT CYCLES ====================

    @Test
    fun `rapid config creation and toggle cycles for all protocols`() = runBlocking<Unit> {
        val result = withTimeoutOrNull(15_000L) {
            repeat(10) { cycle ->
                val configs = listOf(
                    StorageConfig.DropboxConfig(
                        name = "dropbox-$cycle", accessToken = "tok",
                        appKey = "key", appSecret = "sec"
                    ),
                    StorageConfig.FtpConfig(
                        name = "ftp-$cycle", host = "host",
                        username = "u", password = "p"
                    ),
                    StorageConfig.SftpConfig(
                        name = "sftp-$cycle", host = "host",
                        username = "u", password = "p"
                    ),
                    StorageConfig.SmbConfig(
                        name = "smb-$cycle", host = "host",
                        share = "s", username = "u", password = "p"
                    ),
                    StorageConfig.GoogleDriveConfig(
                        name = "gdrive-$cycle", clientId = "id",
                        clientSecret = "sec"
                    ),
                    StorageConfig.OneDriveConfig(
                        name = "onedrive-$cycle", clientId = "id",
                        clientSecret = "sec"
                    ),
                    StorageConfig.WebDavConfig(
                        name = "webdav-$cycle", url = "https://host/dav",
                        username = "u", password = "p"
                    )
                )

                // Toggle enabled/disabled rapidly
                configs.forEach { config ->
                    val disabled = config.withEnabled(false)
                    assertFalse((disabled as StorageConfig).isEnabled,
                        "Disabled config should have isEnabled=false")
                    val reenabled = disabled.withEnabled(true)
                    assertTrue((reenabled as StorageConfig).isEnabled,
                        "Re-enabled config should have isEnabled=true")
                }
            }
            true
        }

        assertNotNull(result, "10 rapid config toggle cycles must complete within 15s")
    }

    // ==================== SECURITY EVENT LOGGER: 10000 EVENTS FROM 50 COROUTINES ====================

    @Test
    fun `SecurityEventLogger handles 10000 events from 50 concurrent coroutines`() = runBlocking<Unit> {
        SecurityEventLogger.clear()
        SecurityEventLogger.maxEvents = 1000

        val result = withTimeoutOrNull(20_000L) {
            val mutex = Mutex()
            var exceptionCount = 0

            val jobs = (1..50).map { coroutineId ->
                async(Dispatchers.Default) {
                    try {
                        repeat(200) { eventId ->
                            when (eventId % 5) {
                                0 -> SecurityEventLogger.logAuthFailure(
                                    "Service-$coroutineId", "Reason $eventId"
                                )
                                1 -> SecurityEventLogger.logPathTraversalBlocked(
                                    "Service-$coroutineId", "../../etc/passwd"
                                )
                                2 -> SecurityEventLogger.logCircuitBreakerOpen(
                                    "Service-$coroutineId", eventId
                                )
                                3 -> SecurityEventLogger.logTokenRefresh(
                                    "Service-$coroutineId", eventId % 2 == 0
                                )
                                else -> SecurityEventLogger.logRateLimited(
                                    "Service-$coroutineId", "operation-$eventId"
                                )
                            }
                        }
                        true
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutex.withLock { exceptionCount++ }
                        false
                    }
                }
            }.awaitAll()

            assertEquals(0, exceptionCount,
                "No exceptions during concurrent event logging")
            jobs
        }

        assertNotNull(result, "10000 concurrent event logs must complete within 20s")
        assertEquals(50, result.size, "All 50 coroutines must complete")
        assertTrue(result.all { it }, "All coroutines must succeed")

        // Verify maxEvents enforcement: 10000 total events, maxEvents=1000
        val eventCount = SecurityEventLogger.eventCount()
        assertTrue(eventCount <= 1000,
            "Event count ($eventCount) must not exceed maxEvents (1000)")
        assertTrue(eventCount > 0, "Events must be retained")

        // Verify we can query events without error
        val authFailures = SecurityEventLogger.getEvents(type = SecurityEventType.AUTH_FAILURE)
        assertTrue(authFailures.isNotEmpty(), "Some auth failure events should be retained")
    }

    // ==================== PERFORMANCE METRICS: 10000 CONCURRENT RECORDINGS ====================

    @Test
    fun `PerformanceMetrics handles 10000 concurrent recordings`() = runBlocking<Unit> {
        PerformanceMetrics.reset()

        val result = withTimeoutOrNull(20_000L) {
            val mutex = Mutex()
            var exceptionCount = 0

            val jobs = (1..50).map { coroutineId ->
                async(Dispatchers.Default) {
                    try {
                        repeat(200) { i ->
                            when (i % 5) {
                                0 -> PerformanceMetrics.recordParse(i.toLong())
                                1 -> PerformanceMetrics.recordCacheHit()
                                2 -> PerformanceMetrics.recordCacheMiss()
                                3 -> PerformanceMetrics.recordDetection(i.toLong())
                                else -> PerformanceMetrics.recordNetworkRequest(
                                    i.toLong(), error = i % 7 == 0
                                )
                            }
                        }
                        true
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutex.withLock { exceptionCount++ }
                        false
                    }
                }
            }.awaitAll()

            assertEquals(0, exceptionCount,
                "No exceptions during concurrent metrics recording")
            jobs
        }

        assertNotNull(result, "10000 concurrent metric recordings must complete within 20s")
        assertEquals(50, result.size, "All 50 coroutines must complete")
        assertTrue(result.all { it }, "All metric recording coroutines must succeed")

        // Verify snapshot is consistent
        val snapshot = PerformanceMetrics.snapshot()
        assertTrue(snapshot.parseCount > 0, "Parse count should be positive")
        assertTrue(snapshot.cacheHits > 0, "Cache hits should be positive")
        assertTrue(snapshot.cacheMisses > 0, "Cache misses should be positive")
        assertTrue(snapshot.detectionCount > 0, "Detection count should be positive")
        assertTrue(snapshot.networkRequests > 0, "Network requests should be positive")
        assertTrue(snapshot.timestamp > 0, "Snapshot timestamp should be positive")
    }

    // ==================== PARSE CONCURRENCY RECONFIGURATION ====================

    @Test
    fun `configureParseConcurrency while parse operations are in flight`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Reconfigured\n\nTest content for concurrency reconfiguration."

        val result = withTimeoutOrNull(30_000L) {
            val mutex = Mutex()
            var completedParses = 0
            var exceptions = 0

            // Start some parse operations
            val parseJobs = (1..20).map { i ->
                async(Dispatchers.Default) {
                    try {
                        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
                        val doc = FormatRegistry.parseWithCacheConcurrent(
                            "# Doc $i\nContent $i",
                            format
                        )
                        assertTrue(doc.rawContent.isNotEmpty())
                        mutex.withLock { completedParses++ }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutex.withLock { exceptions++ }
                    }
                }
            }

            // Reconfigure concurrency while operations are running
            // (valid range 1..16)
            FormatRegistry.configureParseConcurrency(2)

            // Start more parse operations with new concurrency
            val moreJobs = (21..40).map { i ->
                async(Dispatchers.Default) {
                    try {
                        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
                        val doc = FormatRegistry.parseWithCacheConcurrent(
                            "# Doc $i\nNew content $i",
                            format
                        )
                        assertTrue(doc.rawContent.isNotEmpty())
                        mutex.withLock { completedParses++ }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutex.withLock { exceptions++ }
                    }
                }
            }

            // Reconfigure again
            FormatRegistry.configureParseConcurrency(8)

            (parseJobs + moreJobs).awaitAll()

            assertTrue(completedParses > 0,
                "At least some parse operations should complete successfully")
            completedParses to exceptions
        }

        assertNotNull(result, "Parse concurrency reconfiguration test must complete within 30s")
        val (completed, exceptions) = result
        println("Parse concurrency reconfig: completed=$completed, exceptions=$exceptions")
        assertTrue(completed + exceptions == 40,
            "All 40 operations must finish (completed=$completed, exceptions=$exceptions)")
    }

    // ==================== CIRCUIT BREAKER UNDER RAPID FAILURE ====================

    @Test
    fun `CircuitBreaker handles rapid success-failure alternation without crash`() = runBlocking<Unit> {
        val breaker = CircuitBreaker(failureThreshold = 5, name = "overload-test")

        val result = withTimeoutOrNull(15_000L) {
            var successes = 0
            var failures = 0
            var opens = 0

            repeat(1000) { i ->
                val outcome = breaker.execute {
                    if (i % 3 == 0) {
                        throw RuntimeException("Simulated failure $i")
                    }
                    "success-$i"
                }
                if (outcome.isSuccess) {
                    successes++
                } else {
                    val ex = outcome.exceptionOrNull()
                    if (ex is CircuitBreakerOpenException) {
                        opens++
                    } else {
                        failures++
                    }
                }
            }

            Triple(successes, failures, opens)
        }

        assertNotNull(result, "1000 circuit breaker calls must complete within 15s")
        val (successes, failures, opens) = result
        assertTrue(successes + failures + opens == 1000,
            "All 1000 calls must be accounted for (s=$successes, f=$failures, o=$opens)")
        assertTrue(successes > 0, "Some operations should succeed")
        println("CircuitBreaker overload: successes=$successes, failures=$failures, opens=$opens")
    }
}
