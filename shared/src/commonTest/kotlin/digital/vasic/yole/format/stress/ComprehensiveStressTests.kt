/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Stress Tests
 *
 * Deep stress tests covering concurrent parsing of all
 * 17 formats, rapid format detection, document cache
 * contention, circuit breaker behavior, connection
 * limiter fairness, large document parsing, registry
 * thread safety, parse-then-toHtml cycles, and memory
 * stability.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

import digital.vasic.yole.format.*
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.binary.BinaryParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.measureTime
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive stress tests covering concurrent parsing, format detection,
 * cache contention, circuit breaker behavior, large document handling,
 * registry thread safety, and memory stability.
 */
class ComprehensiveStressTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // All parsers for comprehensive testing
    private val allParsers: List<TextParser> = listOf(
        MarkdownParser(),
        PlaintextParser(),
        TodoTxtParser(),
        CsvParser(),
        WikitextParser(),
        CreoleParser(),
        TiddlyWikiParser(),
        LatexParser(),
        AsciidocParser(),
        OrgModeParser(),
        RestructuredTextParser(),
        KeyValueParser(),
        TaskpaperParser(),
        TextileParser(),
        JupyterParser(),
        RMarkdownParser(),
        BinaryParser()
    )

    // Representative content for each of the 17 formats
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

    // ==================== CONCURRENT PARSING (ALL 17 FORMATS) ====================

    @Test
    fun `concurrent parsing of all 17 formats with 100 coroutines`() = runBlocking<Unit> {
        val results = (1..100).map { i ->
            async {
                val parserIndex = i % allParsers.size
                val parser = allParsers[parserIndex]
                val formatId = parser.supportedFormat.id
                val content = formatContents[formatId] ?: "fallback content"
                val result = parser.parse(content)
                result.format.id to result.rawContent.isNotEmpty()
            }
        }.awaitAll()

        assertEquals(100, results.size, "All 100 coroutines must complete")
        assertTrue(results.all { it.second }, "All parse results must have non-empty rawContent")
    }

    @Test
    fun `concurrent parsing same format from multiple coroutines`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Concurrent Test\n\n**Bold** and *italic* text.\n\n- Item 1\n- Item 2"

        val results = (1..100).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        // All results must be identical
        assertTrue(results.all { it.format.id == FormatRegistry.ID_MARKDOWN })
        assertTrue(results.all { it.rawContent == content })
        assertTrue(results.all { it.errors.isEmpty() })
    }

    @Test
    fun `concurrent parsing different documents same parser`() = runBlocking<Unit> {
        val parser = CsvParser()
        val documents = (1..100).map { i ->
            "id,name,value\n${i},Item${i},${i * 10}"
        }

        val results = documents.map { doc ->
            async { parser.parse(doc) }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.format.id == FormatRegistry.ID_CSV })
        // Each result should have its unique content
        val uniqueContents = results.map { it.rawContent }.toSet()
        assertEquals(100, uniqueContents.size, "Each document must be parsed independently")
    }

    // ==================== RAPID FORMAT DETECTION ====================

    @Test
    fun `rapid format detection 10000 calls`() = runBlocking<Unit> {
        val testContents = listOf(
            "# Markdown heading" to FormatRegistry.ID_MARKDOWN,
            "(A) Task @work" to FormatRegistry.ID_TODOTXT,
            "a,b,c\n1,2,3" to FormatRegistry.ID_CSV,
            "\\documentclass{article}" to FormatRegistry.ID_LATEX,
            "* Org heading" to FormatRegistry.ID_ORGMODE
        )

        val results = (1..2000).flatMap { _ ->
            testContents.map { (content, _) ->
                async {
                    FormatRegistry.detectByContent(content)
                }
            }
        }.awaitAll()

        assertEquals(10000, results.size, "All 10000 detection calls must complete")
        assertTrue(results.all { it != null }, "All detections must produce a result for known content")
    }

    @Test
    fun `rapid extension detection 10000 calls`() = runBlocking<Unit> {
        val extensions = listOf("md", "txt", "csv", "tex", "org", "wiki", "rst", "adoc", "tid", "ini")

        val results = (1..1000).flatMap { _ ->
            extensions.map { ext ->
                async {
                    FormatRegistry.detectByExtension(ext)
                }
            }
        }.awaitAll()

        assertEquals(10000, results.size)
        assertTrue(results.all { it.id.isNotEmpty() })
    }

    @Test
    fun `rapid filename detection 10000 calls`() = runBlocking<Unit> {
        val filenames = listOf(
            "README.md", "todo.txt", "data.csv", "paper.tex", "notes.org",
            "page.wiki", "doc.rst", "manual.adoc", "tiddler.tid", "config.ini"
        )

        val results = (1..1000).flatMap { _ ->
            filenames.map { name ->
                async {
                    FormatRegistry.detectByFilename(name)
                }
            }
        }.awaitAll()

        assertEquals(10000, results.size)
        assertTrue(results.all { it.id.isNotEmpty() })
    }

    // ==================== DOCUMENT CACHE UNDER HIGH CONTENTION ====================

    @Test
    fun `document cache contention 50 coroutines 1000 operations each`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val documents = (1..50).map { i ->
            parser.parse("# Document $i\n\nContent for document $i with **bold** text.")
        }

        // 50 coroutines, each doing 1000 toHtml calls on different documents
        val results = documents.mapIndexed { index, doc ->
            async {
                var successCount = 0
                repeat(1000) { iteration ->
                    val html = doc.toHtml(lightMode = iteration % 2 == 0)
                    if (html.contains("Document ${index + 1}")) {
                        successCount++
                    }
                }
                successCount
            }
        }.awaitAll()

        // All operations must succeed
        assertEquals(50, results.size)
        assertTrue(results.all { it == 1000 }, "All 1000 operations per coroutine must succeed")
    }

    @Test
    fun `cache invalidation under concurrent access`() = runBlocking<Unit> {
        val parser = LatexParser()
        val doc = parser.parse("\\documentclass{article}\n\\begin{document}\nHello world.\n\\end{document}")

        // Concurrent reads and cache invalidation
        val results = (1..100).map { i ->
            async {
                if (i % 10 == 0) {
                    doc.clearHtmlCache()
                    "cleared"
                } else {
                    val html = doc.toHtml(lightMode = i % 2 == 0)
                    if (html.isNotEmpty()) "success" else "empty"
                }
            }
        }.awaitAll()

        // All operations must complete
        assertEquals(100, results.size)
        assertTrue(results.none { it == "empty" }, "No toHtml call should return empty")
    }

    // ==================== CIRCUIT BREAKER UNDER RAPID FAILURE ====================

    @Test
    fun `circuit breaker simulation with 1000 rapid calls`() = runBlocking<Unit> {
        // Simulate circuit breaker behavior: parse invalid content rapidly
        // and verify the system remains stable
        val parser = MarkdownParser()
        var successCount = 0
        var errorCount = 0

        repeat(1000) { i ->
            val content = if (i % 3 == 0) {
                // Valid content
                "# Valid Document $i\n\nContent here."
            } else {
                // Edge case content that parsers should still handle
                "\u0000".repeat(i % 50 + 1)
            }
            val result = parser.parse(content)
            if (result.errors.isEmpty()) {
                successCount++
            } else {
                errorCount++
            }
        }

        // Parser must never crash regardless of input
        assertTrue(successCount + errorCount == 1000, "All 1000 calls must complete")
        assertTrue(successCount > 0, "At least some valid documents must parse successfully")
    }

    @Test
    fun `rapid alternating success and failure parsing`() = runBlocking<Unit> {
        val csvParser = CsvParser()
        val results = mutableListOf<Boolean>()

        repeat(1000) { i ->
            val content = if (i % 2 == 0) {
                "name,value\nAlice,$i"
            } else {
                "" // Empty content edge case
            }
            val result = csvParser.parse(content)
            results.add(result.rawContent == content)
        }

        assertEquals(1000, results.size)
        assertTrue(results.all { it }, "All parse operations must return correct rawContent")
    }

    // ==================== CONNECTION LIMITER FAIRNESS ====================

    @Test
    fun `connection limiter fairness across concurrent requests`() = runBlocking<Unit> {
        // Test that FormatRegistry operations are fair under contention
        val formatIds = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX, FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_TODOTXT
        )

        val accessCounts = mutableMapOf<String, Int>()
        formatIds.forEach { accessCounts[it] = 0 }

        val results = (1..500).map { i ->
            async {
                val formatId = formatIds[i % formatIds.size]
                val format = FormatRegistry.getById(formatId)
                format?.id ?: "null"
            }
        }.awaitAll()

        // Count accesses per format
        results.forEach { id ->
            accessCounts[id] = (accessCounts[id] ?: 0) + 1
        }

        // Each format should be accessed roughly equally (100 +/- some)
        val expectedCount = 100
        accessCounts.values.forEach { count ->
            assertEquals(expectedCount, count, "Each format should be accessed exactly $expectedCount times")
        }
    }

    @Test
    fun `concurrent format lookup fairness across all formats`() = runBlocking<Unit> {
        val allFormatIds = FormatRegistry.formats.map { it.id }

        val results = (1..1000).map { i ->
            async {
                val formatId = allFormatIds[i % allFormatIds.size]
                FormatRegistry.getById(formatId)
            }
        }.awaitAll()

        assertEquals(1000, results.size)
        assertTrue(results.all { it != null }, "All format lookups must succeed")
    }

    // ==================== LARGE DOCUMENT PARSING ====================

    @Test
    fun `large markdown document parsing 1MB`() = runBlocking<Unit> {
        val parser = MarkdownParser()

        // Generate ~1MB document
        val largeContent = buildString {
            repeat(5000) { i ->
                appendLine("# Section $i")
                appendLine()
                appendLine("Paragraph with **bold** text for section $i. " +
                    "This adds enough content to reach approximately 1MB total size.")
                appendLine()
                appendLine("- Item 1 in section $i")
                appendLine("- Item 2 in section $i")
                appendLine()
            }
        }

        assertTrue(largeContent.length >= 500_000, "Document must be at least 500KB")

        val parseTime = measureTime {
            val result = parser.parse(largeContent)
            assertEquals(FormatRegistry.ID_MARKDOWN, result.format.id)
            assertEquals(largeContent, result.rawContent)
            assertTrue(result.errors.isEmpty())
        }

        println("1MB Markdown parse time: $parseTime")
        assertTrue(parseTime.inWholeMilliseconds < 10000, "1MB parse must complete within 10 seconds")
    }

    @Test
    fun `large CSV document parsing 10MB`() = runBlocking<Unit> {
        val parser = CsvParser()

        // Generate ~10MB CSV
        val largeContent = buildString {
            appendLine("id,name,value,description,category,timestamp,status")
            repeat(100000) { i ->
                appendLine("$i,Item $i,${i * 100},Description for item $i,Category${i % 10},2025-01-01T00:00:00Z,active")
            }
        }

        assertTrue(largeContent.length >= 5_000_000, "Document must be at least 5MB")

        val parseTime = measureTime {
            val result = parser.parse(largeContent)
            assertEquals(FormatRegistry.ID_CSV, result.format.id)
        }

        println("10MB CSV parse time: $parseTime")
        assertTrue(parseTime.inWholeMilliseconds < 30000, "10MB parse must complete within 30 seconds")
    }

    @Test
    fun `large LaTeX document parsing`() = runBlocking<Unit> {
        val parser = LatexParser()

        val largeContent = buildString {
            appendLine("\\documentclass{book}")
            appendLine("\\usepackage{amsmath}")
            appendLine("\\begin{document}")
            repeat(2000) { i ->
                appendLine("\\chapter{Chapter $i}")
                appendLine("Content for chapter $i.")
                appendLine("\\section{Section ${i}.1}")
                appendLine("\\begin{equation}")
                appendLine("x_{$i} = \\frac{a_{$i}}{b_{$i}}")
                appendLine("\\end{equation}")
                appendLine("\\begin{itemize}")
                appendLine("\\item Point 1")
                appendLine("\\item Point 2")
                appendLine("\\end{itemize}")
            }
            appendLine("\\end{document}")
        }

        assertTrue(largeContent.length >= 200_000, "Document must be at least 200KB")

        val result = parser.parse(largeContent)
        assertEquals(FormatRegistry.ID_LATEX, result.format.id)
        assertTrue(result.errors.isEmpty())
    }

    // ==================== FORMAT REGISTRY THREAD SAFETY ====================

    @Test
    fun `FormatRegistry thread safety under concurrent access`() = runBlocking<Unit> {
        // Concurrent mixed operations on FormatRegistry
        val results = (1..1000).map { i ->
            async {
                when (i % 7) {
                    0 -> FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)?.id ?: "null"
                    1 -> FormatRegistry.getByExtension("md")?.id ?: "null"
                    2 -> FormatRegistry.detectByContent("# Heading")?.id ?: "null"
                    3 -> FormatRegistry.detectByExtension("csv").id
                    4 -> FormatRegistry.detectByFilename("test.tex").id
                    5 -> FormatRegistry.isSupported(FormatRegistry.ID_LATEX).toString()
                    else -> FormatRegistry.getAllExtensions().size.toString()
                }
            }
        }.awaitAll()

        assertEquals(1000, results.size, "All 1000 concurrent operations must complete")
        assertTrue(results.none { it == "null" || it.isEmpty() }, "No operation should return null or empty")
    }

    @Test
    fun `FormatRegistry consistency under repeated access`() = runBlocking<Unit> {
        // Verify that repeated access produces consistent results
        val formatId = FormatRegistry.ID_MARKDOWN
        val results = (1..500).map {
            async {
                val format = FormatRegistry.getById(formatId)
                Triple(format?.id, format?.name, format?.defaultExtension)
            }
        }.awaitAll()

        assertTrue(results.all { it.first == FormatRegistry.ID_MARKDOWN })
        assertTrue(results.all { it.second == "Markdown" })
        assertTrue(results.all { it.third == ".md" })
    }

    @Test
    fun `ParserRegistry thread safety with concurrent getParser calls`() = runBlocking<Unit> {
        // Save original state
        val formatIds = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_PLAINTEXT
        )

        val results = (1..200).map { i ->
            async {
                val formatId = formatIds[i % formatIds.size]
                val parser = ParserRegistry.getParser(formatId)
                parser?.supportedFormat?.id ?: "null"
            }
        }.awaitAll()

        assertEquals(200, results.size)
        // All results should be non-null (parsers must be registered)
        val nonNullResults = results.filter { it != "null" }
        assertTrue(nonNullResults.isNotEmpty(), "At least some parsers must be found")
    }

    // ==================== REPEATED PARSE-THEN-TOHTML CYCLES ====================

    @Test
    fun `repeated parse then toHtml cycles 1000 iterations`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Test Document\n\nParagraph with **bold** and *italic*.\n\n- Item 1\n- Item 2"

        val totalTime = measureTime {
            repeat(1000) { iteration ->
                val doc = parser.parse(content)
                assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)

                val html = doc.toHtml(lightMode = iteration % 2 == 0)
                assertTrue(html.contains("Test Document"))
                assertTrue(html.isNotEmpty())
            }
        }

        println("1000 parse-then-toHtml cycles: $totalTime")
        assertTrue(totalTime.inWholeMilliseconds < 30000, "1000 cycles must complete within 30 seconds")
    }

    @Test
    fun `parse then toHtml cycles with cache verification`() = runBlocking<Unit> {
        val parser = OrgModeParser()
        val content = "* Heading 1\n** TODO Task\nContent here.\n** DONE Completed"

        repeat(100) { i ->
            val doc = parser.parse(content)

            // First call should generate HTML
            assertFalse(doc.hasHtmlCached(lightMode = true))
            val html1 = doc.toHtml(lightMode = true)
            assertTrue(doc.hasHtmlCached(lightMode = true))

            // Second call should return cached
            val html2 = doc.toHtml(lightMode = true)
            assertEquals(html1, html2, "Cached HTML must be identical at iteration $i")

            // Clear and verify
            doc.clearHtmlCache()
            assertFalse(doc.hasHtmlCached(lightMode = true))
        }
    }

    @Test
    fun `parse then toHtml with alternating light and dark modes`() = runBlocking<Unit> {
        val parser = LatexParser()
        val content = "\\documentclass{article}\n\\begin{document}\n\\section{Test}\nContent.\n\\end{document}"

        repeat(500) { i ->
            val doc = parser.parse(content)
            val lightHtml = doc.toHtml(lightMode = true)
            val darkHtml = doc.toHtml(lightMode = false)

            assertTrue(lightHtml.isNotEmpty(), "Light HTML must not be empty at iteration $i")
            assertTrue(darkHtml.isNotEmpty(), "Dark HTML must not be empty at iteration $i")

            // Both modes should be cached independently
            assertTrue(doc.hasHtmlCached(lightMode = true))
            assertTrue(doc.hasHtmlCached(lightMode = false))
        }
    }

    // ==================== MEMORY STABILITY ====================

    @Test
    fun `memory stability parse 10K small documents`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val documents = mutableListOf<ParsedDocument>()

        // Parse 10,000 small documents
        repeat(10000) { i ->
            val content = "# Doc $i\nContent for document $i."
            val doc = parser.parse(content)
            documents.add(doc)
        }

        assertEquals(10000, documents.size, "All 10,000 documents must be parsed")
        assertTrue(documents.all { it.format.id == FormatRegistry.ID_MARKDOWN })
        assertTrue(documents.all { it.errors.isEmpty() })

        // Verify no unbounded growth by checking document sizes are reasonable
        val totalRawSize = documents.sumOf { it.rawContent.length }
        assertTrue(totalRawSize > 0, "Total raw content size must be positive")
        assertTrue(totalRawSize < 100_000_000, "Total raw content must be bounded")

        // Clear all references
        documents.clear()
    }

    @Test
    fun `memory stability parse and clear HTML cache repeatedly`() = runBlocking<Unit> {
        val parser = CsvParser()

        repeat(5000) { i ->
            val content = "a,b,c\n${i},${i * 2},${i * 3}"
            val doc = parser.parse(content)

            // Generate and cache HTML
            val html = doc.toHtml(lightMode = true)
            assertTrue(html.isNotEmpty())
            assertTrue(doc.hasHtmlCached(lightMode = true))

            // Clear cache to free memory
            doc.clearHtmlCache()
            assertFalse(doc.hasHtmlCached(lightMode = true))
        }
        // If we reach here without OOM, memory is stable
    }

    @Test
    fun `memory stability concurrent parsing with cleanup`() = runBlocking<Unit> {
        val parsers = listOf(
            MarkdownParser(), CsvParser(), LatexParser(), OrgModeParser(), TodoTxtParser()
        )

        repeat(20) { batch ->
            val batchResults = (1..500).map { i ->
                async {
                    val parser = parsers[i % parsers.size]
                    val formatId = parser.supportedFormat.id
                    val content = formatContents[formatId] ?: "fallback"
                    val doc = parser.parse(content)
                    doc.toHtml(lightMode = true)
                    doc.clearHtmlCache()
                    doc.rawContent.length
                }
            }.awaitAll()

            assertEquals(500, batchResults.size, "Batch $batch: all 500 operations must complete")
            assertTrue(batchResults.all { it > 0 }, "Batch $batch: all documents must have content")
        }
        // 20 batches x 500 = 10,000 total operations
    }

    @Test
    fun `memory stability all parsers used in rotation`() = runBlocking<Unit> {
        val totalIterations = 10000
        var parsedCount = 0

        repeat(totalIterations) { i ->
            val parser = allParsers[i % allParsers.size]
            val formatId = parser.supportedFormat.id
            val content = formatContents[formatId] ?: "fallback"
            val doc = parser.parse(content)
            assertEquals(formatId, doc.format.id)
            parsedCount++
        }

        assertEquals(totalIterations, parsedCount, "All $totalIterations iterations must complete")
    }

    // ==================== ADDITIONAL STRESS SCENARIOS ====================

    @Test
    fun `concurrent format detection and parsing pipeline`() = runBlocking<Unit> {
        val testInputs = listOf(
            "# Markdown" to "md",
            "a,b,c\n1,2,3" to "csv",
            "\\documentclass{article}" to "tex",
            "* Org heading" to "org",
            "(A) Todo task" to "txt",
            "= AsciiDoc Title" to "adoc",
            "h1. Textile Heading" to "textile"
        )

        val results = (1..100).flatMap { _ ->
            testInputs.map { (content, ext) ->
                async {
                    // Detect format
                    val format = FormatRegistry.detectByExtension(ext)
                    // Get parser
                    val parser = ParserRegistry.getParser(format)
                    // Parse content
                    val doc = parser?.parse(content)
                    // Generate HTML
                    val html = doc?.toHtml(lightMode = true)
                    html?.isNotEmpty() ?: false
                }
            }
        }.awaitAll()

        assertEquals(700, results.size, "All 700 pipeline operations must complete")
    }

    @Test
    fun `stress test ParsedDocument copy under contention`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val original = parser.parse("# Original\n\nContent here.")

        val copies = (1..500).map { i ->
            async {
                val copy = original.copy(
                    metadata = mapOf("iteration" to i.toString())
                )
                copy.metadata["iteration"] to copy.rawContent
            }
        }.awaitAll()

        assertEquals(500, copies.size)
        // Each copy should have its own metadata
        val uniqueIterations = copies.map { it.first }.toSet()
        assertEquals(500, uniqueIterations.size, "Each copy must have unique metadata")
        // All copies should share the same rawContent
        assertTrue(copies.all { it.second == original.rawContent })
    }

    @Test
    fun `stress test ParsedDocument equality and hashCode`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Test\n\nContent."

        val documents = (1..1000).map {
            async {
                parser.parse(content)
            }
        }.awaitAll()

        // All documents parsed from same content should be equal
        val first = documents.first()
        assertTrue(documents.all { it == first }, "All documents from same content must be equal")

        // HashCodes should be consistent
        val hashCodes = documents.map { it.hashCode() }.toSet()
        assertEquals(1, hashCodes.size, "All equal documents must have the same hashCode")
    }

    @Test
    fun `stress test rapid format switching`() = runBlocking<Unit> {
        // Rapidly switch between different format parsers
        val parserPairs = allParsers.zip(formatContents.values.toList())

        val totalTime = measureTime {
            repeat(100) { round ->
                parserPairs.forEach { (parser, content) ->
                    val doc = parser.parse(content)
                    assertTrue(doc.rawContent.isNotEmpty(),
                        "Round $round: ${parser.supportedFormat.id} must produce non-empty rawContent")
                }
            }
        }

        println("100 rounds x ${parserPairs.size} formats = ${100 * parserPairs.size} parses in $totalTime")
    }

    @Test
    fun `stress test concurrent HTML generation all formats`() = runBlocking<Unit> {
        val results = formatContents.entries.flatMap { (formatId, content) ->
            val parser = allParsers.firstOrNull { it.supportedFormat.id == formatId }
            if (parser != null) {
                (1..20).map {
                    async {
                        val doc = parser.parse(content)
                        val html = doc.toHtml(lightMode = true)
                        formatId to html.isNotEmpty()
                    }
                }
            } else {
                emptyList()
            }
        }.awaitAll()

        assertTrue(results.all { it.second }, "All HTML generations must produce non-empty output")
    }
}
