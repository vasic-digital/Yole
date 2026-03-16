/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Metrics Collection Tests
 *
 * Validates parsing throughput, memory usage bounds,
 * and latency percentiles across all text formats.
 *
 *########################################################*/
package digital.vasic.yole.format.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.binary.BinaryParser
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Tests that measure parsing throughput (documents/second),
 * parsed document size bounds, and latency percentiles (p50, p95, p99).
 */
class MetricsCollectionTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // -- helpers --

    private fun generateMarkdown(sizeKb: Int): String = buildString {
        var i = 0
        while (length < sizeKb * 1024) {
            appendLine("# Heading ${i++}")
            appendLine("Paragraph with **bold**, *italic*, and `code`.")
            appendLine("- Item 1\n- Item 2\n- Item 3")
            appendLine()
        }
    }

    private fun generateCsv(rows: Int): String = buildString {
        appendLine("Name,Age,City,Country,Email")
        repeat(rows) { i ->
            appendLine("User$i,${20 + i % 50},City${i % 100},Country${i % 30},user$i@example.com")
        }
    }

    private fun generatePlaintext(sizeKb: Int): String = buildString {
        while (length < sizeKb * 1024) {
            appendLine("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
        }
    }

    private fun generateLatex(sizeKb: Int): String = buildString {
        appendLine("\\documentclass{article}")
        appendLine("\\begin{document}")
        var i = 0
        while (length < sizeKb * 1024) {
            appendLine("\\section{Section ${i++}}")
            appendLine("Text with \\textbf{bold} and \\textit{italic}.")
        }
        appendLine("\\end{document}")
    }

    private fun generateOrgMode(sizeKb: Int): String = buildString {
        var i = 0
        while (length < sizeKb * 1024) {
            appendLine("* Heading ${i++}")
            appendLine("Text with *bold* and /italic/ markup.")
            appendLine("- [ ] Todo item\n- [X] Done item")
        }
    }

    private fun collectLatencies(iterations: Int, block: () -> Unit): List<Long> {
        val latencies = mutableListOf<Long>()
        repeat(iterations) {
            val elapsed = measureTime { block() }
            latencies.add(elapsed.inWholeMilliseconds)
        }
        return latencies.sorted()
    }

    private fun percentile(sorted: List<Long>, p: Double): Long {
        val index = ((p / 100.0) * sorted.size).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    // ====================================================================
    // 1. Parsing throughput (documents per second)
    // ====================================================================

    @Test
    fun `markdown throughput exceeds 10 docs per second for 1KB`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(1)
        var count = 0
        val elapsed = measureTime {
            while (count < 50) {
                parser.parse(content)
                count++
            }
        }
        val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds / 1000.0)
        assertTrue(docsPerSecond > 10, "Markdown throughput: $docsPerSecond docs/s, expected > 10")
    }

    @Test
    fun `csv throughput exceeds 10 docs per second for 100 rows`() {
        val parser = CsvParser()
        val content = generateCsv(100)
        var count = 0
        val elapsed = measureTime {
            while (count < 50) {
                parser.parse(content)
                count++
            }
        }
        val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds / 1000.0)
        assertTrue(docsPerSecond > 10, "CSV throughput: $docsPerSecond docs/s, expected > 10")
    }

    @Test
    fun `plaintext throughput exceeds 50 docs per second for 1KB`() {
        val parser = PlaintextParser()
        val content = generatePlaintext(1)
        var count = 0
        val elapsed = measureTime {
            while (count < 100) {
                parser.parse(content)
                count++
            }
        }
        val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds / 1000.0)
        assertTrue(docsPerSecond > 50, "Plaintext throughput: $docsPerSecond docs/s, expected > 50")
    }

    @Test
    fun `latex throughput exceeds 10 docs per second for 1KB`() {
        val parser = LatexParser()
        val content = generateLatex(1)
        var count = 0
        val elapsed = measureTime {
            while (count < 50) {
                parser.parse(content)
                count++
            }
        }
        val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds / 1000.0)
        assertTrue(docsPerSecond > 10, "LaTeX throughput: $docsPerSecond docs/s, expected > 10")
    }

    @Test
    fun `orgmode throughput exceeds 10 docs per second for 1KB`() {
        val parser = OrgModeParser()
        val content = generateOrgMode(1)
        var count = 0
        val elapsed = measureTime {
            while (count < 50) {
                parser.parse(content)
                count++
            }
        }
        val docsPerSecond = count.toDouble() / (elapsed.inWholeMilliseconds / 1000.0)
        assertTrue(docsPerSecond > 10, "OrgMode throughput: $docsPerSecond docs/s, expected > 10")
    }

    // ====================================================================
    // 2. Parsed document size bounds
    // ====================================================================

    @Test
    fun `parsed document parsedContent not excessively larger than rawContent for markdown`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(5)
        val doc = parser.parse(content)
        val ratio = doc.parsedContent.length.toDouble() / doc.rawContent.length
        assertTrue(ratio < 20.0, "Markdown parsed/raw ratio: $ratio, expected < 20x")
    }

    @Test
    fun `parsed document parsedContent not excessively larger than rawContent for csv`() {
        val parser = CsvParser()
        val content = generateCsv(200)
        val doc = parser.parse(content)
        val ratio = doc.parsedContent.length.toDouble() / doc.rawContent.length
        assertTrue(ratio < 20.0, "CSV parsed/raw ratio: $ratio, expected < 20x")
    }

    @Test
    fun `parsed document parsedContent not excessively larger than rawContent for plaintext`() {
        val parser = PlaintextParser()
        val content = generatePlaintext(5)
        val doc = parser.parse(content)
        val ratio = doc.parsedContent.length.toDouble() / doc.rawContent.length
        assertTrue(ratio < 10.0, "Plaintext parsed/raw ratio: $ratio, expected < 10x")
    }

    @Test
    fun `parsed document metadata size is bounded`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(10)
        val doc = parser.parse(content)
        // Metadata should not be excessively large
        val metadataSize = doc.metadata.entries.sumOf { it.key.length + it.value.length }
        assertTrue(metadataSize < 10_000, "Metadata size $metadataSize chars, expected < 10,000")
    }

    // ====================================================================
    // 3. Latency percentiles (p50, p95, p99)
    // ====================================================================

    @Test
    fun `markdown parse p50 under 50ms for 1KB`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(1)
        // Warmup
        repeat(3) { parser.parse(content) }
        val latencies = collectLatencies(20) { parser.parse(content) }
        val p50 = percentile(latencies, 50.0)
        assertTrue(p50 < 50, "Markdown p50: ${p50}ms, expected < 50ms")
    }

    @Test
    fun `markdown parse p95 under 100ms for 1KB`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(1)
        repeat(3) { parser.parse(content) }
        val latencies = collectLatencies(20) { parser.parse(content) }
        val p95 = percentile(latencies, 95.0)
        assertTrue(p95 < 100, "Markdown p95: ${p95}ms, expected < 100ms")
    }

    @Test
    fun `markdown parse p99 under 200ms for 1KB`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(1)
        repeat(3) { parser.parse(content) }
        val latencies = collectLatencies(50) { parser.parse(content) }
        val p99 = percentile(latencies, 99.0)
        assertTrue(p99 < 200, "Markdown p99: ${p99}ms, expected < 200ms")
    }

    @Test
    fun `csv parse p50 under 50ms for 100 rows`() {
        val parser = CsvParser()
        val content = generateCsv(100)
        repeat(3) { parser.parse(content) }
        val latencies = collectLatencies(20) { parser.parse(content) }
        val p50 = percentile(latencies, 50.0)
        assertTrue(p50 < 50, "CSV p50: ${p50}ms, expected < 50ms")
    }

    @Test
    fun `csv parse p95 under 100ms for 100 rows`() {
        val parser = CsvParser()
        val content = generateCsv(100)
        repeat(3) { parser.parse(content) }
        val latencies = collectLatencies(20) { parser.parse(content) }
        val p95 = percentile(latencies, 95.0)
        assertTrue(p95 < 100, "CSV p95: ${p95}ms, expected < 100ms")
    }

    @Test
    fun `format detection p50 under 5ms`() {
        val content = "# Heading\n\nParagraph text"
        repeat(3) { FormatRegistry.detectByContent(content) }
        val latencies = collectLatencies(20) { FormatRegistry.detectByContent(content) }
        val p50 = percentile(latencies, 50.0)
        assertTrue(p50 < 5, "detectByContent p50: ${p50}ms, expected < 5ms")
    }

    @Test
    fun `format detection p99 under 20ms`() {
        val content = "# Heading\n\nParagraph text"
        repeat(3) { FormatRegistry.detectByContent(content) }
        val latencies = collectLatencies(50) { FormatRegistry.detectByContent(content) }
        val p99 = percentile(latencies, 99.0)
        assertTrue(p99 < 20, "detectByContent p99: ${p99}ms, expected < 20ms")
    }

    @Test
    fun `html generation p50 under 100ms for 5KB markdown`() {
        val parser = MarkdownParser()
        val content = generateMarkdown(5)
        val doc = parser.parse(content)
        // Each call needs a fresh doc since toHtml caches
        repeat(3) { parser.parse(content).toHtml(lightMode = true) }
        val latencies = collectLatencies(20) {
            parser.parse(content).toHtml(lightMode = true)
        }
        val p50 = percentile(latencies, 50.0)
        assertTrue(p50 < 100, "HTML gen p50: ${p50}ms, expected < 100ms")
    }
}
