/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for ParsedDocument, ParserRegistry, ParseOptions, escapeHtml
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParsedDocumentTest {

    private val testFormat = TextFormat(id = "test", name = "Test", defaultExtension = ".tst")

    @Test
    fun testParsedDocumentCreation() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "raw",
            parsedContent = "parsed",
            metadata = mapOf("key" to "value"),
            errors = listOf("warning")
        )
        assertEquals(testFormat, doc.format)
        assertEquals("raw", doc.rawContent)
        assertEquals("parsed", doc.parsedContent)
        assertEquals("value", doc.metadata["key"])
        assertEquals(1, doc.errors.size)
    }

    @Test
    fun testDefaultMetadataAndErrors() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertTrue(doc.metadata.isEmpty())
        assertTrue(doc.errors.isEmpty())
    }

    @Test
    fun testHtmlCacheStartsEmpty() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun testClearHtmlCache() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        // No crash when clearing empty cache
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
    }

    @Test
    fun testParsedDocumentEquality() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "a", parsedContent = "b")
        val d2 = ParsedDocument(format = testFormat, rawContent = "a", parsedContent = "b")
        assertEquals(d1, d2)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun testParsedDocumentCopy() {
        val original = ParsedDocument(
            format = testFormat,
            rawContent = "raw",
            parsedContent = "parsed",
            metadata = mapOf("k" to "v")
        )
        val copy = original.copy(rawContent = "new raw")
        assertEquals("new raw", copy.rawContent)
        assertEquals("parsed", copy.parsedContent)
        // Copy should start with empty cache
        assertFalse(copy.hasHtmlCached(lightMode = true))
    }

    @Test
    fun testToStringTruncatesContent() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "x".repeat(100),
            parsedContent = "p"
        )
        val str = doc.toString()
        assertTrue(str.contains("..."))
        // rawContent should be truncated to 50 chars
        assertFalse(str.contains("x".repeat(100)))
    }
}

class EscapeHtmlTest {

    @Test
    fun testEscapeHtmlBasicCharacters() {
        assertEquals("&amp;", "&".escapeHtml())
        assertEquals("&lt;", "<".escapeHtml())
        assertEquals("&gt;", ">".escapeHtml())
        assertEquals("&quot;", "\"".escapeHtml())
        assertEquals("&#39;", "'".escapeHtml())
    }

    @Test
    fun testEscapeHtmlScriptTag() {
        val input = "<script>alert('xss')</script>"
        val escaped = input.escapeHtml()
        assertFalse(escaped.contains("<script>"))
        assertTrue(escaped.contains("&lt;script&gt;"))
    }

    @Test
    fun testEscapeHtmlNoChange() {
        val safe = "Hello World 123"
        assertEquals(safe, safe.escapeHtml())
    }

    @Test
    fun testEscapeHtmlEmptyString() {
        assertEquals("", "".escapeHtml())
    }
}

class ParseOptionsTest {

    @Test
    fun testCreateAndBuildEmpty() {
        val options = ParseOptions.create().build()
        assertTrue(options.isEmpty())
    }

    @Test
    fun testEnableLineNumbers() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .build()
        assertEquals(true, options["lineNumbers"])
    }

    @Test
    fun testEnableHighlighting() {
        val options = ParseOptions.create()
            .enableHighlighting(true)
            .build()
        assertEquals(true, options["highlighting"])
    }

    @Test
    fun testSetBaseUrl() {
        val options = ParseOptions.create()
            .setBaseUrl("https://example.com")
            .build()
        assertEquals("https://example.com", options["baseUrl"])
    }

    @Test
    fun testSetCustomOption() {
        val options = ParseOptions.create()
            .set("custom", 42)
            .build()
        assertEquals(42, options["custom"])
    }

    @Test
    fun testFluentChaining() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .enableHighlighting(false)
            .setBaseUrl("http://test.com")
            .set("extra", "val")
            .build()
        assertEquals(4, options.size)
    }
}

class ParserRegistryTest {

    private val testFormat = TextFormat(id = "parsertest", name = "ParserTest", defaultExtension = ".pt")

    private class StubParser(override val supportedFormat: TextFormat) : TextParser {
        override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
            return ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content
            )
        }
    }

    @Test
    fun testRegisterAndGetParser() {
        ParserRegistry.clear()
        val parser = StubParser(testFormat)
        ParserRegistry.register(parser)
        val retrieved = ParserRegistry.getParser(testFormat)
        assertNotNull(retrieved)
        assertEquals(testFormat.id, retrieved.supportedFormat.id)
        ParserRegistry.clear()
    }

    @Test
    fun testRegisterLazyParser() {
        ParserRegistry.clear()
        var instantiated = false
        ParserRegistry.registerLazy(testFormat.id) {
            instantiated = true
            StubParser(testFormat)
        }
        assertFalse(instantiated)
        assertEquals(1, ParserRegistry.getPendingParserCount())

        val parser = ParserRegistry.getParser(testFormat)
        assertNotNull(parser)
        assertTrue(instantiated)
        assertEquals(0, ParserRegistry.getPendingParserCount())
        ParserRegistry.clear()
    }

    @Test
    fun testHasParser() {
        ParserRegistry.clear()
        assertFalse(ParserRegistry.hasParser(testFormat))
        ParserRegistry.register(StubParser(testFormat))
        assertTrue(ParserRegistry.hasParser(testFormat))
        ParserRegistry.clear()
    }

    @Test
    fun testGetParserReturnsNullForUnknown() {
        ParserRegistry.clear()
        val unknown = TextFormat(id = "nonexistent", name = "None", defaultExtension = ".none")
        assertNull(ParserRegistry.getParser(unknown))
        ParserRegistry.clear()
    }

    @Test
    fun testDuplicateRegistrationSilentlySkips() {
        ParserRegistry.clear()
        ParserRegistry.register(StubParser(testFormat))
        // Should not throw — silently skips already-registered format
        ParserRegistry.register(StubParser(testFormat))
        // Verify parser still works
        assertNotNull(ParserRegistry.getParser(testFormat))
        ParserRegistry.clear()
    }

    @Test
    fun testClearRemovesAllParsers() {
        ParserRegistry.clear()
        ParserRegistry.register(StubParser(testFormat))
        assertEquals(1, ParserRegistry.getInstantiatedParserCount())
        ParserRegistry.clear()
        assertEquals(0, ParserRegistry.getInstantiatedParserCount())
        assertEquals(0, ParserRegistry.getPendingParserCount())
    }

    @Test
    fun testGetAllParsers() {
        ParserRegistry.clear()
        val f1 = TextFormat(id = "pt1", name = "PT1", defaultExtension = ".pt1")
        val f2 = TextFormat(id = "pt2", name = "PT2", defaultExtension = ".pt2")
        ParserRegistry.register(StubParser(f1))
        ParserRegistry.register(StubParser(f2))
        assertEquals(2, ParserRegistry.getAllParsers().size)
        ParserRegistry.clear()
    }

    @Test
    fun testDefaultToHtml() {
        val parser = StubParser(testFormat)
        val doc = parser.parse("<b>bold</b>")
        val html = parser.toHtml(doc)
        assertTrue(html.contains("&lt;b&gt;bold&lt;/b&gt;"))
        assertTrue(html.startsWith("<pre>"))
    }

    @Test
    fun testDefaultValidate() {
        val parser = StubParser(testFormat)
        assertTrue(parser.validate("any content").isEmpty())
    }

    @Test
    fun testCanParse() {
        val parser = StubParser(testFormat)
        assertTrue(parser.canParse(testFormat))
        val other = TextFormat(id = "other", name = "Other", defaultExtension = ".oth")
        assertFalse(parser.canParse(other))
    }
}
