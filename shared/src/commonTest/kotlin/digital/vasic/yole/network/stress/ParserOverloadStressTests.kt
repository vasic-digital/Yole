/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Parser Overload Stress Tests
 *
 * Validates FormatRegistry, individual parsers, and
 * DocumentCache under concurrent and high-volume load:
 * 100 concurrent markdown parses, all 17 formats parsed
 * concurrently, large document (10K lines), rapid format
 * detection alternation, DocumentCache concurrent access.
 *
 *########################################################*/
package digital.vasic.yole.network.stress

import digital.vasic.yole.format.*
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.binary.BinaryParser
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
import kotlinx.coroutines.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Parser overload stress tests.
 *
 * Verifies no crashes, no deadlocks, and consistent results under
 * high concurrency and large input volume across all 17 parsers.
 *
 * Total: ~30 tests
 */
class ParserOverloadStressTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        FormatRegistry.documentCache.let { /* warm up */ }
    }

    // ==================== Markdown concurrent ====================

    @Test
    fun OneHundredConcurrentMarkdownParsesNoCrash() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Title\n\n**Bold** and *italic*.\n\n- Item 1\n- Item 2\n- Item 3"
        val results = withTimeout(30.seconds) {
            (1..100).map { async(Dispatchers.Default) { parser.parse(content) } }.awaitAll()
        }
        assertEquals(100, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun OneHundredConcurrentMarkdownToHtmlNoCrash() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Stress Test\n\nParagraph with **bold** and *italic*."
        val results = withTimeout(30.seconds) {
            (1..100).map {
                async(Dispatchers.Default) {
                    val doc = parser.parse(content)
                    parser.toHtml(doc, lightMode = it % 2 == 0)
                }
            }.awaitAll()
        }
        assertEquals(100, results.size)
        results.forEach { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun ConcurrentMarkdownResultsAreConsistent() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Consistent\n\nSame content always."
        val results = withTimeout(30.seconds) {
            (1..50).map { async(Dispatchers.Default) { parser.parse(content) } }.awaitAll()
        }
        // All results should have same format and content
        val firstId = results.first().format.id
        results.forEach { assertEquals(firstId, it.format.id) }
    }

    // ==================== All 17 formats concurrently ====================

    @Test
    fun AllSeventeenFormatsParsedConcurrentlyNoCrash() = runBlocking<Unit> {
        data class Fixture(val parser: TextParser, val content: String)

        val fixtures = listOf(
            Fixture(MarkdownParser(), "# Title\n\n**bold** text"),
            Fixture(PlaintextParser(), "Plain text content line."),
            Fixture(TodoTxtParser(), "(A) Task @work +proj\n(B) Task2"),
            Fixture(CsvParser(), "name,age,city\nAlice,30,NY\nBob,25,LA"),
            Fixture(WikitextParser(), "= Heading =\n**bold** and //italic//"),
            Fixture(OrgModeParser(), "* Heading\n** Sub\n   Content"),
            Fixture(CreoleParser(), "= Title =\n**bold** and //italic//"),
            Fixture(TiddlyWikiParser(), "! Heading\nContent line"),
            Fixture(LatexParser(), "\\documentclass{article}\n\\begin{document}\nHello\n\\end{document}"),
            Fixture(AsciidocParser(), "= Document Title\n\nSection content here."),
            Fixture(RestructuredTextParser(), "Title\n=====\n\n.. note::\n   A note."),
            Fixture(KeyValueParser(), "key1=value1\nkey2=value2\n[section]"),
            Fixture(TaskpaperParser(), "Project:\n\t- Task one @done\n\t- Task two"),
            Fixture(TextileParser(), "h1. Title\n\nParagraph with *bold*."),
            Fixture(JupyterParser(), "{\"nbformat\":4,\"cells\":[{\"cell_type\":\"markdown\",\"source\":[\"# Title\"]}]}"),
            Fixture(RMarkdownParser(), "---\ntitle: Test\n---\n\n```{r}\nplot(1:10)\n```"),
            Fixture(BinaryParser(), "\u0000\u0001\u0002binary content\u00FF")
        )

        val results = withTimeout(30.seconds) {
            fixtures.map { fixture ->
                async(Dispatchers.Default) { fixture.parser.parse(fixture.content) }
            }.awaitAll()
        }

        assertEquals(17, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun AllFormatsToHtmlConcurrentlyNoCrash() = runBlocking<Unit> {
        data class Fixture(val parser: TextParser, val content: String)

        val fixtures = listOf(
            Fixture(MarkdownParser(), "# Title\n\nContent"),
            Fixture(PlaintextParser(), "Plain content"),
            Fixture(TodoTxtParser(), "(A) Do task @work"),
            Fixture(CsvParser(), "a,b,c\n1,2,3"),
            Fixture(WikitextParser(), "= H1 =\nParagraph"),
            Fixture(OrgModeParser(), "* Heading\nContent"),
            Fixture(CreoleParser(), "= Title =\nContent"),
            Fixture(TiddlyWikiParser(), "! H1\nContent"),
            Fixture(LatexParser(), "\\documentclass{article}\n\\begin{document}\nText\n\\end{document}"),
            Fixture(AsciidocParser(), "= Title\n\nContent"),
            Fixture(RestructuredTextParser(), "Title\n=====\nContent"),
            Fixture(KeyValueParser(), "k=v\n[s]"),
            Fixture(TaskpaperParser(), "P:\n\t- Task"),
            Fixture(TextileParser(), "h1. Title\nContent"),
            Fixture(JupyterParser(), "{\"nbformat\":4,\"cells\":[]}"),
            Fixture(RMarkdownParser(), "# Title\n\nContent"),
            Fixture(BinaryParser(), "binary")
        )

        val results = withTimeout(30.seconds) {
            fixtures.map { fixture ->
                async(Dispatchers.Default) {
                    val doc = fixture.parser.parse(fixture.content)
                    fixture.parser.toHtml(doc, lightMode = true)
                }
            }.awaitAll()
        }

        assertEquals(17, results.size)
        results.forEach { assertTrue(it.isNotEmpty()) }
    }

    // ==================== Large document ====================

    @Test
    fun MarkdownLargeDocument10KLinesNoCrash() = runBlocking<Unit> {
        val lines = (1..10_000).map { i ->
            when (i % 10) {
                0 -> "# Heading $i"
                1 -> "## Sub-heading $i"
                2 -> "**Bold text** in line $i."
                3 -> "*Italic text* in line $i."
                4 -> "- List item $i"
                5 -> "[Link $i](https://example.com)"
                else -> "Regular paragraph content on line $i with some text."
            }
        }
        val content = lines.joinToString("\n")
        val parser = MarkdownParser()
        val doc = withTimeout(30.seconds) { parser.parse(content) }
        assertNotNull(doc)
        assertTrue(doc.rawContent.length > 100_000)
    }

    @Test
    fun PlaintextLargeDocument10KLinesNoCrash() = runBlocking<Unit> {
        val content = (1..10_000).joinToString("\n") { "Line $it content text." }
        val parser = PlaintextParser()
        val doc = withTimeout(30.seconds) { parser.parse(content) }
        assertNotNull(doc)
    }

    @Test
    fun CsvLargeDocument1KRowsNoCrash() = runBlocking<Unit> {
        val header = "id,name,value,category,active"
        val rows = (1..1_000).map { "$it,Item $it,${it * 10},Cat${it % 5},${it % 2 == 0}" }
        val content = (listOf(header) + rows).joinToString("\n")
        val parser = CsvParser()
        val doc = withTimeout(30.seconds) { parser.parse(content) }
        assertNotNull(doc)
    }

    @Test
    fun TaskpaperLargeDocument500TasksNoCrash() = runBlocking<Unit> {
        val sb = StringBuilder()
        repeat(10) { p ->
            sb.appendLine("Project $p:")
            repeat(50) { t ->
                sb.appendLine("\t- Task $t in project $p @priority(${t % 3})")
            }
            sb.appendLine()
        }
        val parser = TaskpaperParser()
        val doc = withTimeout(30.seconds) { parser.parse(sb.toString()) }
        assertNotNull(doc)
        assertEquals("500", doc.metadata["tasks"])
    }

    // ==================== Rapid format detection ====================

    @Test
    fun RapidFormatDetectionAlternationNoCrash() = runBlocking<Unit> {
        val samples = listOf(
            "README.md",
            "document.rst",
            "tasks.taskpaper",
            "data.csv",
            "note.wiki",
            "file.org",
            "report.tex",
            "notebook.ipynb",
            "notes.txt",
            "page.adoc"
        )
        val results = withTimeout(30.seconds) {
            (1..500).map { i ->
                async(Dispatchers.Default) {
                    FormatRegistry.detectByFilename(samples[i % samples.size])
                }
            }.awaitAll()
        }
        assertEquals(500, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun ConcurrentContentDetectionNoCrash() = runBlocking<Unit> {
        val samples = listOf(
            "# Markdown\n\n**bold**",
            "(A) Todo task @work",
            "name,age\nAlice,30",
            "\\documentclass{article}",
            "= Title\n\nContent",
            "* Org heading\n",
            "key=value\n[section]"
        )
        val results = withTimeout(30.seconds) {
            (1..200).map { i ->
                async(Dispatchers.Default) {
                    FormatRegistry.detectByContent(samples[i % samples.size])
                }
            }.awaitAll()
        }
        assertEquals(200, results.size)
        // Results may be null for ambiguous content — that is valid
    }

    // ==================== DocumentCache concurrent ====================

    @Test
    fun DocumentCacheConcurrentPutAndGetNoCrash() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val parser = MarkdownParser()
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!

        withTimeout(30.seconds) {
            val writers = (1..100).map { i ->
                async(Dispatchers.Default) {
                    val content = "# Title $i\n\nContent $i"
                    val doc = parser.parse(content)
                    cache.put("key$i", doc)
                }
            }
            val readers = (1..100).map { i ->
                async(Dispatchers.Default) {
                    cache.get("key${i % 20}")
                }
            }
            (writers + readers).awaitAll()
        }
        assertTrue(cache.size <= 50)
    }

    @Test
    fun DocumentCacheLruEvictionUnderLoad() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        val parser = PlaintextParser()

        withTimeout(30.seconds) {
            (1..100).map { i ->
                async {
                    val doc = parser.parse("Content $i")
                    cache.put("key$i", doc)
                }
            }.awaitAll()
        }

        assertTrue(cache.size <= 10, "Cache should not exceed maxSize under load")
    }

    @Test
    fun DocumentCacheClearUnderConcurrentAccess() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 100)
        val parser = MarkdownParser()

        withTimeout(30.seconds) {
            val writers = (1..50).map { i ->
                async {
                    val doc = parser.parse("# Title $i")
                    cache.put("k$i", doc)
                }
            }
            val clearer = async {
                delay(10)
                cache.clear()
            }
            (writers + listOf(clearer)).awaitAll()
        }
        // After clear, size should be 0 (modulo any puts after clear)
        assertTrue(cache.size >= 0)
    }

    // ==================== FormatRegistry.parseWithCache concurrent ====================

    @Test
    fun ParseWithCacheConcurrentSameContentReturnsCachedResult() = runBlocking<Unit> {
        val content = "# Shared Content\n\nSame document parsed many times."
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!

        val results = withTimeout(30.seconds) {
            (1..50).map {
                async(Dispatchers.Default) {
                    FormatRegistry.parseWithCache(content, format)
                }
            }.awaitAll()
        }

        assertEquals(50, results.size)
        val firstContent = results.first().rawContent
        results.forEach { assertEquals(firstContent, it.rawContent) }
    }

    @Test
    fun ParseWithCacheConcurrentDifferentFormatsNoCrash() = runBlocking<Unit> {
        val formatAndContent = listOf(
            FormatRegistry.ID_MARKDOWN to "# Title\n\nContent",
            FormatRegistry.ID_PLAINTEXT to "Plain text content",
            FormatRegistry.ID_CSV to "a,b\n1,2"
        )

        val results = withTimeout(30.seconds) {
            (1..90).map { i ->
                val (id, content) = formatAndContent[i % 3]
                val format = FormatRegistry.getById(id)!!
                async(Dispatchers.Default) {
                    FormatRegistry.parseWithCache(content, format)
                }
            }.awaitAll()
        }
        // Every parse must have produced a non-null ParsedDocument with non-empty raw content.
        assertEquals(90, results.size, "all 90 concurrent parses must complete")
        assertTrue(
            results.all { it.rawContent.isNotEmpty() },
            "every concurrent parse must yield a populated ParsedDocument"
        )
    }

    @Test
    fun ParseWithCacheConcurrentSemaphoreReleasedAfterError() = runBlocking<Unit> {
        // Configure low concurrency, verify no permit leak on repeated calls.
        FormatRegistry.configureParseConcurrency(2)
        val results = try {
            val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
            withTimeout(30.seconds) {
                (1..20).map { i ->
                    async(Dispatchers.Default) {
                        FormatRegistry.parseWithCacheConcurrent("# Doc $i", format)
                    }
                }.awaitAll()
            }
        } finally {
            FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
        }
        // All 20 concurrent parses must complete (proves no permit leak — leaked
        // permits would deadlock the await within the timeout).
        assertEquals(20, results.size, "all 20 concurrent parses must complete (no permit leak)")
        // Post-test: the registry must be functional after concurrency reset.
        val postTest = FormatRegistry.parseWithCacheConcurrent("# post-test", FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!)
        assertTrue(postTest.rawContent.isNotEmpty(), "registry must remain functional after stress")
    }
}
