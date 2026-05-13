/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Concurrent Format Parsing Stress Tests
 *
 * Validates that all 18 format parsers operate correctly
 * under simultaneous concurrent load with no data corruption,
 * no cross-contamination, and valid ParsedDocument output.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

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
import digital.vasic.yole.format.json.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*

/**
 * Stress tests for concurrent format parsing across all 18 formats.
 *
 * Validates that 100+ concurrent parse operations produce valid,
 * uncorrupted ParsedDocuments with no cross-contamination between
 * formats, sustained over multiple rounds.
 */
class ConcurrentFormatParsingStressTest {

    private data class FormatTestCase(
        val parser: TextParser,
        val content: String,
        val expectedFormatId: String
    )

    private val testCases: List<FormatTestCase> by lazy {
        listOf(
            FormatTestCase(MarkdownParser(), "# Heading\n\n**Bold** and *italic*.", FormatRegistry.ID_MARKDOWN),
            FormatTestCase(PlaintextParser(), "Simple plain text content.", FormatRegistry.ID_PLAINTEXT),
            FormatTestCase(TodoTxtParser(), "(A) Important task @work +project1\nx 2025-01-15 Done", FormatRegistry.ID_TODOTXT),
            FormatTestCase(CsvParser(), "name,age,city\nAlice,30,NY\nBob,25,LA", FormatRegistry.ID_CSV),
            FormatTestCase(LatexParser(), "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}", FormatRegistry.ID_LATEX),
            FormatTestCase(OrgModeParser(), "* Heading 1\n** Sub\nContent here.", FormatRegistry.ID_ORGMODE),
            FormatTestCase(AsciidocParser(), "= Title\n\nParagraph text.", FormatRegistry.ID_ASCIIDOC),
            FormatTestCase(WikitextParser(), "== Heading ==\n\n[[Link]]", FormatRegistry.ID_WIKITEXT),
            FormatTestCase(CreoleParser(), "= Heading\n\n**bold** //italic//", FormatRegistry.ID_CREOLE),
            FormatTestCase(TiddlyWikiParser(), "! Heading\n\nSome ''bold'' text.", FormatRegistry.ID_TIDDLYWIKI),
            FormatTestCase(RestructuredTextParser(), "Title\n=====\n\nParagraph.", FormatRegistry.ID_RESTRUCTUREDTEXT),
            FormatTestCase(KeyValueParser(), "key1=value1\nkey2=value2", FormatRegistry.ID_KEYVALUE),
            FormatTestCase(TaskpaperParser(), "Project:\n\t- Task 1\n\t- Task 2 @done", FormatRegistry.ID_TASKPAPER),
            FormatTestCase(TextileParser(), "h1. Title\n\n*bold* _italic_", FormatRegistry.ID_TEXTILE),
            FormatTestCase(JupyterParser(), "{\"nbformat\":4,\"cells\":[]}", FormatRegistry.ID_JUPYTER),
            FormatTestCase(RMarkdownParser(), "---\ntitle: Test\n---\n```{r}\nplot(1)\n```", FormatRegistry.ID_RMARKDOWN),
            FormatTestCase(BinaryParser(), "\u0000\u0001\u0002binary data", FormatRegistry.ID_BINARY),
            FormatTestCase(JsonParser(), "{\"a\":1,\"b\":[2,3]}", FormatRegistry.ID_JSON)
        )
    }

    @Test
    fun `100 concurrent parses across all 18 formats simultaneously`() = runBlocking<Unit> {
        val results = testCases.flatMap { tc ->
            (1..6).map {
                async(Dispatchers.Default) {
                    tc.parser.parse(tc.content) to tc.expectedFormatId
                }
            }
        }.awaitAll()

        // 18 formats x 6 = 108 concurrent operations
        assertTrue(results.size >= 100, "Should have at least 100 results (got ${results.size})")
        results.forEach { (doc, expectedId) ->
            assertEquals(expectedId, doc.format.id, "Format mismatch: expected $expectedId got ${doc.format.id}")
        }
    }

    @Test
    fun `no corruption between concurrent parses of different formats`() = runBlocking<Unit> {
        val mutex = Mutex()
        val resultsMap = mutableMapOf<String, MutableList<ParsedDocument>>()

        val jobs = testCases.flatMap { tc ->
            (1..10).map {
                launch(Dispatchers.Default) {
                    val doc = tc.parser.parse(tc.content)
                    mutex.withLock {
                        resultsMap.getOrPut(tc.expectedFormatId) { mutableListOf() }.add(doc)
                    }
                }
            }
        }
        jobs.forEach { it.join() }

        // Each format should have exactly 10 results
        testCases.forEach { tc ->
            val docs = resultsMap[tc.expectedFormatId] ?: emptyList()
            assertEquals(10, docs.size, "Format ${tc.expectedFormatId} should have 10 results")
            docs.forEach { doc ->
                assertEquals(tc.content, doc.rawContent, "Content corrupted for ${tc.expectedFormatId}")
                assertEquals(tc.expectedFormatId, doc.format.id, "Format ID corrupted for ${tc.expectedFormatId}")
            }
        }
    }

    @Test
    fun `all results are valid ParsedDocuments with non-empty parsedContent`() = runBlocking<Unit> {
        val results = testCases.map { tc ->
            async(Dispatchers.Default) {
                tc.parser.parse(tc.content)
            }
        }.awaitAll()

        results.forEach { doc ->
            assertNotNull(doc.format, "Format must not be null")
            assertTrue(doc.format.id.isNotEmpty(), "Format ID must not be empty")
            assertTrue(doc.rawContent.isNotEmpty(), "Raw content must not be empty")
            assertTrue(doc.parsedContent.isNotEmpty(), "Parsed content must not be empty for ${doc.format.id}")
        }
    }

    @Test
    fun `sustained load over 10 rounds produces consistent results`() = runBlocking<Unit> {
        val mutex = Mutex()
        var totalParsed = 0

        repeat(10) { round ->
            val roundResults = testCases.map { tc ->
                async(Dispatchers.Default) {
                    tc.parser.parse(tc.content) to tc.expectedFormatId
                }
            }.awaitAll()

            roundResults.forEach { (doc, expectedId) ->
                assertEquals(expectedId, doc.format.id, "Round $round: format mismatch")
            }
            mutex.withLock { totalParsed += roundResults.size }
        }

        assertEquals(180, totalParsed, "Should have parsed 18 formats x 10 rounds = 180")
    }

    @Test
    fun `concurrent same-parser same-content yields identical results`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Title\n\nParagraph with **bold** and *italic*.\n\n- Item 1\n- Item 2"

        val results = (1..100).map {
            async(Dispatchers.Default) {
                parser.parse(content)
            }
        }.awaitAll()

        val reference = results.first()
        results.forEach { doc ->
            assertEquals(reference.rawContent, doc.rawContent, "Raw content mismatch")
            assertEquals(reference.parsedContent, doc.parsedContent, "Parsed content mismatch")
            assertEquals(reference.format.id, doc.format.id, "Format ID mismatch")
        }
    }

    @Test
    fun `concurrent parsing with varying content sizes`() = runBlocking<Unit> {
        val parser = CsvParser()
        val mutex = Mutex()
        var successCount = 0

        val jobs = (1..50).map { size ->
            launch(Dispatchers.Default) {
                val content = buildString {
                    appendLine("id,name,value")
                    repeat(size * 10) { i ->
                        appendLine("$i,item-$i,${i * 100}")
                    }
                }
                val doc = parser.parse(content)
                assertEquals(FormatRegistry.ID_CSV, doc.format.id)
                assertTrue(doc.rawContent.isNotEmpty())
                mutex.withLock { successCount++ }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(50, successCount, "All 50 operations should succeed")
    }

    @Test
    fun `concurrent parsing does not leak metadata between parses`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val contents = (1..50).map { i ->
            "# Document $i\n\nUnique content for document number $i."
        }

        val results = contents.map { content ->
            async(Dispatchers.Default) {
                parser.parse(content, mapOf("filename" to "doc.md"))
            }
        }.awaitAll()

        results.forEachIndexed { index, doc ->
            val expected = "# Document ${index + 1}\n\nUnique content for document number ${index + 1}."
            assertEquals(expected, doc.rawContent, "Document $index has wrong rawContent")
        }
    }

    @Test
    fun `concurrent HTML generation produces valid output`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Hello **World**\n\nA paragraph with `code`."

        val htmlResults = (1..100).map {
            async(Dispatchers.Default) {
                val doc = parser.parse(content)
                parser.toHtml(doc)
            }
        }.awaitAll()

        val reference = htmlResults.first()
        assertTrue(reference.contains("Hello"), "HTML should contain 'Hello'")
        assertTrue(reference.contains("World"), "HTML should contain 'World'")
        htmlResults.forEach { html ->
            assertEquals(reference, html, "All HTML outputs should be identical")
        }
    }

    @Test
    fun `concurrent parsing of empty content across all formats`() = runBlocking<Unit> {
        val results = testCases.flatMap { tc ->
            (1..5).map {
                async(Dispatchers.Default) {
                    tc.parser.parse("") to tc.expectedFormatId
                }
            }
        }.awaitAll()

        assertEquals(90, results.size, "18 formats x 5 = 90 results")
        results.forEach { (doc, expectedId) ->
            assertEquals(expectedId, doc.format.id, "Empty parse format mismatch")
            assertEquals("", doc.rawContent, "Raw content should be empty")
        }
    }

    @Test
    fun `interleaved format parsing under contention`() = runBlocking<Unit> {
        val mutex = Mutex()
        val formatCounts = mutableMapOf<String, Int>()

        // Launch all parsers interleaved: round-robin across formats
        val jobs = (1..200).map { i ->
            val tc = testCases[i % testCases.size]
            launch(Dispatchers.Default) {
                val doc = tc.parser.parse(tc.content)
                assertEquals(tc.expectedFormatId, doc.format.id)
                mutex.withLock {
                    formatCounts[tc.expectedFormatId] = (formatCounts[tc.expectedFormatId] ?: 0) + 1
                }
            }
        }
        jobs.forEach { it.join() }

        val totalOps = formatCounts.values.sum()
        assertEquals(200, totalOps, "All 200 interleaved operations should complete")
        // Each format should have been parsed at least once
        testCases.forEach { tc ->
            assertTrue(
                (formatCounts[tc.expectedFormatId] ?: 0) > 0,
                "Format ${tc.expectedFormatId} should have been parsed at least once"
            )
        }
    }

    @Test
    fun `sustained concurrent load with validation per round`() = runBlocking<Unit> {
        val parser = OrgModeParser()
        val content = "* Section 1\n** Sub 1.1\nContent.\n* Section 2\n** Sub 2.1\nMore content."

        repeat(10) { round ->
            val results = (1..20).map {
                async(Dispatchers.Default) {
                    parser.parse(content)
                }
            }.awaitAll()

            results.forEach { doc ->
                assertEquals(FormatRegistry.ID_ORGMODE, doc.format.id, "Round $round: wrong format")
                assertEquals(content, doc.rawContent, "Round $round: content corrupted")
                assertTrue(doc.parsedContent.isNotEmpty(), "Round $round: parsed content empty")
            }
        }
    }
}
