/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Non-Blocking Guarantee Tests
 *
 * Validates that no operation blocks the caller beyond
 * acceptable timeouts: format parsing, DocumentCache,
 * FormatRegistry, StyleSheets, and concurrent
 * parse+cache operations.
 *
 *########################################################*/

package digital.vasic.yole.integration

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
 * Non-blocking guarantee tests.
 *
 * Every operation exercised here must complete within a generous timeout
 * (typically 5 000 ms). The tests use [withTimeoutOrNull] to prove that
 * parsing, caching, registry access, and stylesheet generation never
 * block the caller indefinitely -- even under concurrent pressure.
 */
class NonBlockingGuaranteeTest {

    private val timeoutMs = 5_000L

    // -- Format parsing within timeout --

    @Test
    fun `Markdown parsing completes within timeout`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val result = withTimeoutOrNull(timeoutMs) {
            parser.parse("# Hello\n\n**bold** and *italic*\n\n- item 1\n- item 2")
        }
        assertNotNull(result, "Markdown parsing should complete within ${timeoutMs}ms")
        assertEquals(FormatRegistry.ID_MARKDOWN, result.format.id)
    }

    @Test
    fun `all 18 format parsers complete within timeout`() = runBlocking<Unit> {
        data class Sample(val parser: TextParser, val content: String, val id: String)

        val samples = listOf(
            Sample(MarkdownParser(), "# Title\n\nText", FormatRegistry.ID_MARKDOWN),
            Sample(PlaintextParser(), "Plain text", FormatRegistry.ID_PLAINTEXT),
            Sample(TodoTxtParser(), "(A) Task @ctx +proj", FormatRegistry.ID_TODOTXT),
            Sample(CsvParser(), "a,b,c\n1,2,3", FormatRegistry.ID_CSV),
            Sample(LatexParser(), "\\documentclass{article}\n\\begin{document}\nHi\n\\end{document}", FormatRegistry.ID_LATEX),
            Sample(OrgModeParser(), "* Head\nBody", FormatRegistry.ID_ORGMODE),
            Sample(AsciidocParser(), "= Title\n\nPara.", FormatRegistry.ID_ASCIIDOC),
            Sample(WikitextParser(), "== Head ==\nText", FormatRegistry.ID_WIKITEXT),
            Sample(CreoleParser(), "= Head\n**bold**", FormatRegistry.ID_CREOLE),
            Sample(TiddlyWikiParser(), "! Title\n''bold''", FormatRegistry.ID_TIDDLYWIKI),
            Sample(RestructuredTextParser(), "Title\n=====\nPara.", FormatRegistry.ID_RESTRUCTUREDTEXT),
            Sample(KeyValueParser(), "k=v\nk2=v2", FormatRegistry.ID_KEYVALUE),
            Sample(TaskpaperParser(), "Proj:\n\t- Task @done", FormatRegistry.ID_TASKPAPER),
            Sample(TextileParser(), "h1. Title\n\n*bold*", FormatRegistry.ID_TEXTILE),
            Sample(JupyterParser(), "{\"nbformat\":4,\"cells\":[]}", FormatRegistry.ID_JUPYTER),
            Sample(RMarkdownParser(), "---\ntitle: Doc\n---\n```{r}\n1\n```", FormatRegistry.ID_RMARKDOWN),
            Sample(BinaryParser(), "\u0000\u0001binary", FormatRegistry.ID_BINARY),
            Sample(JsonParser(), "{\"a\":1,\"b\":[2,3]}", FormatRegistry.ID_JSON)
        )

        samples.forEach { sample ->
            val doc = withTimeoutOrNull(timeoutMs) {
                sample.parser.parse(sample.content)
            }
            assertNotNull(doc, "Parsing ${sample.id} should complete within ${timeoutMs}ms")
            assertEquals(sample.id, doc.format.id)
        }
    }

    @Test
    fun `parsing moderately sized content completes within timeout`() = runBlocking<Unit> {
        val parser = PlaintextParser()
        val largeContent = buildString {
            repeat(5_000) { i ->
                appendLine("Line $i with some representative text content.")
            }
        }

        val result = withTimeoutOrNull(timeoutMs) {
            parser.parse(largeContent)
        }
        assertNotNull(result, "Parsing 5000-line document should complete within ${timeoutMs}ms")
        assertTrue(result.rawContent.contains("Line 0"))
    }

    // -- DocumentCache operations don't block --

    @Test
    fun `DocumentCache put and get do not block`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)!!
        val doc = ParsedDocument(format, "raw", "parsed")

        val putResult = withTimeoutOrNull(timeoutMs) {
            cache.put("key1", doc)
            true
        }
        assertNotNull(putResult, "DocumentCache.put should not block")

        val getResult = withTimeoutOrNull(timeoutMs) {
            cache.get("key1")
        }
        assertNotNull(getResult, "DocumentCache.get should not block and should return the document")
        assertEquals("raw", getResult.rawContent)
    }

    @Test
    fun `DocumentCache operations under concurrent pressure do not block`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 200)
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)!!

        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..100).map { i ->
                async(Dispatchers.Default) {
                    val doc = ParsedDocument(format, "content-$i", "parsed-$i")
                    cache.put("key-$i", doc)
                    val retrieved = cache.get("key-$i")
                    assertNotNull(retrieved, "Should retrieve key-$i")
                    i
                }
            }
            jobs.awaitAll()
        }
        assertNotNull(result, "100 concurrent cache operations should complete within ${timeoutMs}ms")
        assertEquals(100, result.size)
    }

    @Test
    fun `DocumentCache clear and invalidate do not block`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)!!

        repeat(30) { i ->
            cache.put("k$i", ParsedDocument(format, "r$i", "p$i"))
        }

        val invalidateOk = withTimeoutOrNull(timeoutMs) {
            cache.invalidate("k0")
            true
        }
        assertNotNull(invalidateOk, "DocumentCache.invalidate should not block")
        assertFalse(cache.contains("k0"), "Invalidated key should be absent")

        val clearOk = withTimeoutOrNull(timeoutMs) {
            cache.clear()
            true
        }
        assertNotNull(clearOk, "DocumentCache.clear should not block")
        assertEquals(0, cache.size, "Cache should be empty after clear")
    }

    // -- Concurrent parse + cache don't deadlock --

    @Test
    fun `concurrent parse and cache operations do not deadlock`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 100)
        val parser = MarkdownParser()
        val mutex = Mutex()
        var completed = 0

        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..50).map { i ->
                launch(Dispatchers.Default) {
                    val content = "# Heading $i\n\nParagraph $i with **bold**."
                    val doc = parser.parse(content)
                    val key = "${doc.format.id}:${content.hashCode()}"
                    cache.put(key, doc)

                    val cached = cache.get(key)
                    assertNotNull(cached, "Should retrieve parsed doc for iteration $i")
                    assertEquals(doc.rawContent, cached.rawContent)

                    mutex.withLock { completed++ }
                }
            }
            jobs.forEach { it.join() }
            true
        }
        assertNotNull(result, "50 concurrent parse+cache operations should not deadlock")
        assertEquals(50, completed, "All 50 operations should complete")
    }

    @Test
    fun `mixed read-write cache access under contention does not deadlock`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 30)
        val format = FormatRegistry.getById(FormatRegistry.ID_CSV)!!

        repeat(10) { i ->
            cache.put("pre-$i", ParsedDocument(format, "r$i", "p$i"))
        }

        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..80).map { i ->
                launch(Dispatchers.Default) {
                    when (i % 4) {
                        0 -> cache.put("dyn-$i", ParsedDocument(format, "dr$i", "dp$i"))
                        1 -> cache.get("pre-${i % 10}")
                        2 -> cache.contains("pre-${i % 10}")
                        else -> cache.invalidate("dyn-${i - 1}")
                    }
                }
            }
            jobs.forEach { it.join() }
            true
        }
        assertNotNull(result, "80 mixed cache operations should complete without deadlock")
    }

    // -- FormatRegistry access under contention --

    @Test
    fun `FormatRegistry access does not block under contention`() = runBlocking<Unit> {
        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..100).map { i ->
                async(Dispatchers.Default) {
                    when (i % 5) {
                        0 -> FormatRegistry.detectByExtension("md")
                        1 -> FormatRegistry.detectByContent("# Heading")
                        2 -> FormatRegistry.getById(FormatRegistry.ID_LATEX)
                        3 -> FormatRegistry.getFormatsByExtension("txt")
                        else -> FormatRegistry.getAllExtensions()
                    }
                    i
                }
            }
            jobs.awaitAll()
        }
        assertNotNull(result, "100 concurrent FormatRegistry accesses should complete within timeout")
        assertEquals(100, result.size)
    }

    @Test
    fun `FormatRegistry lazy initialization does not block concurrent callers`() = runBlocking<Unit> {
        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..50).map {
                async(Dispatchers.Default) {
                    val formats = FormatRegistry.formats
                    assertTrue(formats.isNotEmpty(), "Formats list should not be empty")
                    formats.size
                }
            }
            jobs.awaitAll()
        }
        assertNotNull(result, "Concurrent FormatRegistry.formats access should not block")
        assertTrue(result.all { it > 0 }, "All accesses should see a non-empty list")
    }

    // -- StyleSheets generation --

    @Test
    fun `StyleSheets generation does not block`() = runBlocking<Unit> {
        StyleSheets.clearCache()

        val formatIds = listOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )

        val result = withTimeoutOrNull(timeoutMs) {
            formatIds.forEach { formatId ->
                val light = StyleSheets.getStyleSheet(formatId, lightMode = true)
                val dark = StyleSheets.getStyleSheet(formatId, lightMode = false)
                assertNotNull(light)
                assertNotNull(dark)
            }
            true
        }
        assertNotNull(result, "StyleSheets generation for all formats should not block")
        assertTrue(StyleSheets.cacheSize > 0, "Cache should be populated after generation")
    }

    @Test
    fun `StyleSheets concurrent access does not block`() = runBlocking<Unit> {
        StyleSheets.clearCache()

        val result = withTimeoutOrNull(timeoutMs) {
            val jobs = (1..60).map { i ->
                async(Dispatchers.Default) {
                    val formatId = when (i % 6) {
                        0 -> TextFormat.ID_MARKDOWN
                        1 -> TextFormat.ID_WIKITEXT
                        2 -> TextFormat.ID_RESTRUCTUREDTEXT
                        3 -> TextFormat.ID_ORGMODE
                        4 -> TextFormat.ID_ASCIIDOC
                        else -> TextFormat.ID_LATEX
                    }
                    val lightMode = i % 2 == 0
                    StyleSheets.getStyleSheet(formatId, lightMode)
                }
            }
            jobs.awaitAll()
        }
        assertNotNull(result, "60 concurrent StyleSheets accesses should complete within timeout")
        assertEquals(60, result.size)
    }
}
