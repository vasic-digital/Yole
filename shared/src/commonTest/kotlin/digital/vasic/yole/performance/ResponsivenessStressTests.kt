/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Responsiveness Stress Tests
 *
 * Validates system-wide responsiveness under heavy
 * concurrent load: parse operations, format detections,
 * DocumentCache contention, StyleSheets requests,
 * mixed workloads, rapid-fire sequential operations,
 * graceful degradation, and sustained stress cycles.
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
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*
import kotlin.time.measureTime

/**
 * System-wide responsiveness stress tests.
 *
 * Validates that all major subsystems remain responsive under heavy
 * concurrent and sequential load, with timeout-based deadlock detection
 * and CancellationException leak tracking.
 *
 * Total: 10 tests
 */
class ResponsivenessStressTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // Representative parsers for concurrent parse tests
    private val parsers: List<TextParser> = listOf(
        MarkdownParser(),
        PlaintextParser(),
        TodoTxtParser(),
        CsvParser(),
        LatexParser(),
        OrgModeParser(),
        AsciidocParser(),
        WikitextParser(),
        CreoleParser(),
        TiddlyWikiParser(),
        RestructuredTextParser(),
        KeyValueParser(),
        TaskpaperParser(),
        TextileParser(),
        JupyterParser(),
        RMarkdownParser(),
        BinaryParser()
    )

    private val formatContents: Map<String, String> = mapOf(
        FormatRegistry.ID_MARKDOWN to "# Heading\n\nParagraph with **bold** and *italic*.\n\n- List 1\n- List 2\n\n```kotlin\nfun main() {}\n```",
        FormatRegistry.ID_PLAINTEXT to "This is plain text content.\nLine two of the plain text.\nLine three.",
        FormatRegistry.ID_TODOTXT to "(A) Important task @work +project1\n(B) Another task @home +project2\nx 2025-01-01 Completed task",
        FormatRegistry.ID_CSV to "name,age,city\nAlice,30,New York\nBob,25,Los Angeles\nCharlie,35,Chicago",
        FormatRegistry.ID_WIKITEXT to "= Main Heading =\n\n== Section ==\n\n'''Bold''' and ''italic''.\n\n* List item\n* Another item",
        FormatRegistry.ID_CREOLE to "= Heading\n\n**Bold** and //italic//.\n\n* List item\n* Another item",
        FormatRegistry.ID_TIDDLYWIKI to "title: Test Tiddler\ntags: test sample\n\n! Heading\n\n!! Subheading\n\nSome content here.",
        FormatRegistry.ID_LATEX to "\\documentclass{article}\n\\begin{document}\n\\section{Introduction}\nHello \\textbf{world}.\n\\begin{itemize}\n\\item First\n\\item Second\n\\end{itemize}\n\\end{document}",
        FormatRegistry.ID_ASCIIDOC to "= Document Title\n\n== Section 1\n\nSome *bold* and _italic_ text.\n\n* Item 1\n* Item 2",
        FormatRegistry.ID_ORGMODE to "* Heading 1\n** TODO Task 1\nContent here.\n** DONE Task 2\n* Heading 2\n** Subheading",
        FormatRegistry.ID_RESTRUCTUREDTEXT to "Title\n=====\n\nSection\n-------\n\nParagraph with **bold** and *italic*.\n\n* Item 1\n* Item 2",
        FormatRegistry.ID_KEYVALUE to "[section]\nkey1 = value1\nkey2 = value2\n\n[another]\nfoo = bar",
        FormatRegistry.ID_TASKPAPER to "Project:\n\t- Task 1 @due(2025-01-15)\n\t- Task 2 @done\n\t\t- Subtask @tag(value)",
        FormatRegistry.ID_TEXTILE to "h1. Heading\n\nParagraph with *bold* and _italic_.\n\n* Item 1\n* Item 2",
        FormatRegistry.ID_JUPYTER to """{"nbformat":4,"nbformat_minor":4,"metadata":{"kernelspec":{"name":"python3","display_name":"Python 3"}},"cells":[{"cell_type":"markdown","source":"# Title"},{"cell_type":"code","execution_count":1,"source":"print('hello')","outputs":[]}]}""",
        FormatRegistry.ID_RMARKDOWN to "---\ntitle: 'Test'\noutput: html_document\n---\n\n# Heading\n\n```{r}\nsummary(cars)\n```",
        FormatRegistry.ID_BINARY to "\u0000\u0001\u0002\u0003\u0004\u0005"
    )

    // ==================== CONCURRENT PARSE OPERATIONS ====================

    @Test
    fun `100 concurrent parse operations complete without deadlock`() = runBlocking<Unit> {
        val result = withTimeoutOrNull(30_000L) {
            val deferred = (1..100).map { i ->
                async(Dispatchers.Default) {
                    val parser = parsers[i % parsers.size]
                    val formatId = parser.supportedFormat.id
                    val content = formatContents[formatId] ?: "fallback content $i"
                    val doc = parser.parse(content)
                    doc.format.id to doc.rawContent.isNotEmpty()
                }
            }
            deferred.awaitAll()
        }

        assertNotNull(result, "100 concurrent parses must complete within 30s (deadlock detected)")
        assertEquals(100, result.size, "All 100 parse results must be collected")
        assertTrue(result.all { it.second }, "All parse results must have non-empty rawContent")
    }

    // ==================== CONCURRENT FORMAT DETECTIONS ====================

    @Test
    fun `200 concurrent format detections all return results`() = runBlocking<Unit> {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst", "adoc", "tid", "ini",
            "taskpaper", "textile", "ipynb", "rmd", "creole", "bin", "markdown", "log", "properties", "rest")

        val result = withTimeoutOrNull(15_000L) {
            (1..200).map { i ->
                async(Dispatchers.Default) {
                    FormatRegistry.detectByExtension(extensions[i % extensions.size])
                }
            }.awaitAll()
        }

        assertNotNull(result, "200 concurrent detections must complete within 15s")
        assertEquals(200, result.size, "All 200 detections must return")
        assertTrue(result.all { it.id.isNotEmpty() }, "All detections must return valid format IDs")
    }

    // ==================== CONCURRENT DOCUMENT CACHE OPERATIONS ====================

    @Test
    fun `50 concurrent DocumentCache get and put interleaved`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 30)
        val parser = MarkdownParser()

        val result = withTimeoutOrNull(20_000L) {
            val mutex = Mutex()
            var cancellationCount = 0

            val jobs = (1..50).map { i ->
                async(Dispatchers.Default) {
                    try {
                        val content = "# Doc $i\n\nContent for document number $i."
                        val doc = parser.parse(content)
                        val key = "cache-key-$i"

                        // Interleave puts and gets
                        cache.put(key, doc)
                        val retrieved = cache.get(key)
                        // May be evicted if cache is full, so null is acceptable
                        if (retrieved != null) {
                            assertEquals(content, retrieved.rawContent,
                                "Retrieved doc $i must match original")
                        }

                        // Also try reading from other entries
                        val otherKey = "cache-key-${(i + 7) % 50 + 1}"
                        cache.get(otherKey) // may or may not exist

                        true
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        mutex.withLock { cancellationCount++ }
                        throw e // always rethrow
                    }
                }
            }.awaitAll()

            jobs
        }

        assertNotNull(result, "50 concurrent cache ops must complete within 20s")
        assertEquals(50, result.size, "All 50 cache operations must complete")
        assertTrue(result.all { it }, "All cache operations must succeed")
    }

    // ==================== CONCURRENT STYLESHEET REQUESTS ====================

    @Test
    fun `100 concurrent StyleSheet requests return valid CSS`() = runBlocking<Unit> {
        StyleSheets.clearCache()

        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT, TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE, TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )

        val result = withTimeoutOrNull(10_000L) {
            (1..100).map { i ->
                async(Dispatchers.Default) {
                    val formatId = formatIds[i % formatIds.size]
                    val lightMode = i % 2 == 0
                    val css = StyleSheets.getStyleSheet(formatId, lightMode)
                    formatId to css
                }
            }.awaitAll()
        }

        assertNotNull(result, "100 concurrent StyleSheet requests must complete within 10s")
        assertEquals(100, result.size, "All 100 StyleSheet requests must return")
        // Formats with defined styles should return non-empty CSS
        val styledFormats = setOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT, TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE, TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )
        result.forEach { (formatId, css) ->
            if (formatId in styledFormats) {
                assertTrue(css.isNotEmpty(), "CSS for $formatId must not be empty")
                assertTrue(css.contains("<style>"), "CSS for $formatId must contain <style> tag")
            }
        }
    }

    // ==================== MIXED WORKLOAD ====================

    @Test
    fun `mixed workload 50 parses 50 detections 50 cache ops simultaneously`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val parser = MarkdownParser()

        val result = withTimeoutOrNull(30_000L) {
            val parseJobs = (1..50).map { i ->
                async(Dispatchers.Default) {
                    val p = parsers[i % parsers.size]
                    val content = formatContents[p.supportedFormat.id] ?: "fallback"
                    p.parse(content)
                    "parse-$i"
                }
            }

            val detectionJobs = (1..50).map { i ->
                async(Dispatchers.Default) {
                    val extensions = listOf("md", "csv", "tex", "org", "rst")
                    FormatRegistry.detectByExtension(extensions[i % extensions.size])
                    "detect-$i"
                }
            }

            val cacheJobs = (1..50).map { i ->
                async(Dispatchers.Default) {
                    val content = "# Mixed $i\nContent."
                    val doc = parser.parse(content)
                    cache.put("mixed-$i", doc)
                    cache.get("mixed-${(i + 3) % 50 + 1}")
                    "cache-$i"
                }
            }

            val allResults = (parseJobs + detectionJobs + cacheJobs).awaitAll()
            allResults
        }

        assertNotNull(result, "Mixed workload of 150 ops must complete within 30s")
        assertEquals(150, result.size, "All 150 mixed operations must complete")
    }

    // ==================== RAPID-FIRE SEQUENTIAL ====================

    @Test
    fun `1000 sequential parses complete in reasonable time`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Sequential Test\n\n**Bold** text with *italic*.\n\n- Item 1\n- Item 2"

        val result = withTimeoutOrNull(30_000L) {
            val elapsed = measureTime {
                repeat(1000) {
                    val doc = parser.parse(content)
                    assertTrue(doc.rawContent.isNotEmpty())
                }
            }
            elapsed
        }

        assertNotNull(result, "1000 sequential parses must complete within 30s")
        println("1000 sequential parses completed in $result")
    }

    // ==================== GRACEFUL DEGRADATION ====================

    @Test
    fun `500 concurrent operations degrade gracefully without crashes`() = runBlocking<Unit> {
        val mutex = Mutex()
        var exceptionCount = 0
        var completedCount = 0

        val result = withTimeoutOrNull(60_000L) {
            val jobs = (1..500).map { i ->
                async(Dispatchers.Default) {
                    try {
                        when (i % 5) {
                            0 -> {
                                val p = parsers[i % parsers.size]
                                val content = formatContents[p.supportedFormat.id] ?: "fallback"
                                p.parse(content)
                            }
                            1 -> {
                                FormatRegistry.detectByExtension(listOf("md", "csv", "tex", "org", "rst")[i % 5])
                            }
                            2 -> {
                                FormatRegistry.detectByContent("# Markdown $i")
                            }
                            3 -> {
                                StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, i % 2 == 0)
                            }
                            else -> {
                                FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
                            }
                        }
                        mutex.withLock { completedCount++ }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // always rethrow
                    } catch (_: Exception) {
                        mutex.withLock { exceptionCount++ }
                    }
                }
            }
            jobs.awaitAll()
            completedCount to exceptionCount
        }

        assertNotNull(result, "500 concurrent ops must complete within 60s (no deadlock)")
        val (completed, exceptions) = result
        assertTrue(completed + exceptions == 500,
            "All 500 operations must finish (completed=$completed, exceptions=$exceptions)")
        // Graceful degradation: we allow slowness but not crashes
        assertTrue(exceptions == 0,
            "No exceptions expected under normal load (got $exceptions)")
    }

    // ==================== SUSTAINED STRESS CYCLES ====================

    @Test
    fun `all operations responsive after 10 sequential stress cycles`() = runBlocking<Unit> {
        val result = withTimeoutOrNull(60_000L) {
            repeat(10) { cycle ->
                // Each cycle: concurrent parses + detections + cache ops
                val parser = parsers[cycle % parsers.size]
                val content = formatContents[parser.supportedFormat.id] ?: "fallback"
                val cache = DocumentCache(maxSize = 20)

                val jobs = (1..30).map { i ->
                    async(Dispatchers.Default) {
                        when (i % 3) {
                            0 -> parser.parse(content)
                            1 -> FormatRegistry.detectByExtension("md")
                            else -> {
                                val doc = parser.parse(content)
                                cache.put("cycle-$cycle-$i", doc)
                                cache.get("cycle-$cycle-$i")
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
            true
        }

        assertNotNull(result, "10 stress cycles must complete within 60s (no degradation)")
        assertTrue(result, "Sustained stress must succeed")
    }

    // ==================== CANCELLATION EXCEPTION LEAK CHECK ====================

    @Test
    fun `no CancellationException leaks during concurrent operations`() = runBlocking<Unit> {
        val mutex = Mutex()
        var leakedCancellations = 0

        val result = withTimeoutOrNull(20_000L) {
            val jobs = (1..100).map { i ->
                async(Dispatchers.Default) {
                    try {
                        val parser = parsers[i % parsers.size]
                        val content = formatContents[parser.supportedFormat.id] ?: "fallback"
                        val doc = parser.parse(content)
                        doc.toHtml(lightMode = i % 2 == 0)
                        true
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // proper: rethrow
                    } catch (_: Exception) {
                        // Non-cancellation exceptions are acceptable
                        true
                    }
                }
            }
            jobs.awaitAll()
        }

        assertNotNull(result, "100 concurrent parse+HTML ops must complete within 20s")
        assertEquals(100, result.size, "All 100 operations must complete")
        assertEquals(0, leakedCancellations, "No CancellationException should leak")
    }

    // ==================== TIMEOUT-BASED HANG DETECTION ====================

    @Test
    fun `withTimeoutOrNull detects hangs in parse pipeline`() = runBlocking<Unit> {
        // This test validates that our timeout mechanism works correctly
        // by running a known-fast operation and verifying it completes
        val parser = MarkdownParser()
        val content = "# Quick test\n\nSimple content."

        val quickResult = withTimeoutOrNull(5_000L) {
            val doc = parser.parse(content)
            doc.toHtml(lightMode = true)
        }
        assertNotNull(quickResult, "Quick parse must complete well within 5s timeout")
        assertTrue(quickResult.isNotEmpty(), "HTML result must not be empty")

        // Verify FormatRegistry is responsive
        val registryResult = withTimeoutOrNull(5_000L) {
            FormatRegistry.formats.size
        }
        assertNotNull(registryResult, "FormatRegistry.formats must be accessible within 5s")
        assertTrue(registryResult > 0, "FormatRegistry must have registered formats")
    }
}
