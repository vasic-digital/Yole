/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge case tests for ParsedDocument, escapeHtml,
 * ParseOptions, and ParserRegistry
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParsedDocumentEdgeCaseTest {

    private val testFormat = TextFormat(id = "edgetest", name = "EdgeTest", defaultExtension = ".et")
    private val otherFormat = TextFormat(id = "other", name = "Other", defaultExtension = ".oth")

    // ==================== ParsedDocument equality edge cases ====================

    @Test
    fun testEqualsSameReference() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertEquals(doc, doc)
    }

    @Test
    fun testEqualsNull() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertFalse(doc.equals(null))
    }

    @Test
    fun testEqualsDifferentClass() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertFalse(doc.equals("string"))
    }

    @Test
    fun testNotEqualDifferentFormat() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        val d2 = ParsedDocument(format = otherFormat, rawContent = "r", parsedContent = "p")
        assertNotEquals(d1, d2)
    }

    @Test
    fun testNotEqualDifferentRawContent() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "a", parsedContent = "p")
        val d2 = ParsedDocument(format = testFormat, rawContent = "b", parsedContent = "p")
        assertNotEquals(d1, d2)
    }

    @Test
    fun testNotEqualDifferentParsedContent() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "a")
        val d2 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "b")
        assertNotEquals(d1, d2)
    }

    @Test
    fun testNotEqualDifferentMetadata() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            metadata = mapOf("k" to "v1"))
        val d2 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            metadata = mapOf("k" to "v2"))
        assertNotEquals(d1, d2)
    }

    @Test
    fun testNotEqualDifferentErrors() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            errors = listOf("error1"))
        val d2 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            errors = listOf("error2"))
        assertNotEquals(d1, d2)
    }

    @Test
    fun testEqualWithEmptyMetadataAndNoMetadata() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            metadata = emptyMap())
        val d2 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertEquals(d1, d2)
    }

    @Test
    fun testHashCodeConsistency() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        val hash1 = doc.hashCode()
        val hash2 = doc.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun testHashCodeEqualObjects() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        val d2 = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun testHashCodeDifferentObjects() {
        val d1 = ParsedDocument(format = testFormat, rawContent = "a", parsedContent = "p")
        val d2 = ParsedDocument(format = testFormat, rawContent = "b", parsedContent = "p")
        assertNotEquals(d1.hashCode(), d2.hashCode())
    }

    // ==================== ParsedDocument toString edge cases ====================

    @Test
    fun testToStringShortContent() {
        val doc = ParsedDocument(format = testFormat, rawContent = "short", parsedContent = "p")
        val str = doc.toString()
        assertTrue(str.contains("short"))
        assertTrue(str.contains("..."))
    }

    @Test
    fun testToStringEmptyContent() {
        val doc = ParsedDocument(format = testFormat, rawContent = "", parsedContent = "")
        val str = doc.toString()
        assertTrue(str.contains("..."))
        assertTrue(str.contains("format="))
    }

    @Test
    fun testToStringExactly50Chars() {
        val content = "x".repeat(50)
        val doc = ParsedDocument(format = testFormat, rawContent = content, parsedContent = "")
        val str = doc.toString()
        assertTrue(str.contains(content))
    }

    @Test
    fun testToString51Chars() {
        val content = "x".repeat(51)
        val doc = ParsedDocument(format = testFormat, rawContent = content, parsedContent = "")
        val str = doc.toString()
        // Should truncate to 50 chars
        assertFalse(str.contains(content))
    }

    @Test
    fun testToStringIncludesMetadata() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            metadata = mapOf("key" to "val"))
        val str = doc.toString()
        assertTrue(str.contains("metadata="))
    }

    @Test
    fun testToStringIncludesErrors() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            errors = listOf("err1"))
        val str = doc.toString()
        assertTrue(str.contains("errors="))
    }

    // ==================== ParsedDocument copy edge cases ====================

    @Test
    fun testCopyAllFields() {
        val original = ParsedDocument(
            format = testFormat,
            rawContent = "raw",
            parsedContent = "parsed",
            metadata = mapOf("k" to "v"),
            errors = listOf("e1")
        )
        val copy = original.copy(
            format = otherFormat,
            rawContent = "new raw",
            parsedContent = "new parsed",
            metadata = mapOf("k2" to "v2"),
            errors = listOf("e2", "e3")
        )
        assertEquals(otherFormat, copy.format)
        assertEquals("new raw", copy.rawContent)
        assertEquals("new parsed", copy.parsedContent)
        assertEquals(mapOf("k2" to "v2"), copy.metadata)
        assertEquals(listOf("e2", "e3"), copy.errors)
    }

    @Test
    fun testCopyDefaultsPreserveOriginal() {
        val original = ParsedDocument(
            format = testFormat,
            rawContent = "raw",
            parsedContent = "parsed",
            metadata = mapOf("k" to "v"),
            errors = listOf("e1")
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun testCopyDoesNotShareHtmlCache() {
        val original = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        // Original has no cache
        assertFalse(original.hasHtmlCached(true))
        val copy = original.copy()
        assertFalse(copy.hasHtmlCached(true))
    }

    // ==================== ParsedDocument HTML cache edge cases ====================

    @Test
    fun testHasHtmlCachedLightAndDarkIndependent() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun testClearHtmlCacheClearsBoth() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun testClearHtmlCacheIdempotent() {
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p")
        doc.clearHtmlCache()
        doc.clearHtmlCache()
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    // ==================== ParsedDocument with metadata ====================

    @Test
    fun testLargeMetadata() {
        val metadata = (1..100).associate { "key$it" to "value$it" }
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            metadata = metadata)
        assertEquals(100, doc.metadata.size)
        assertEquals("value50", doc.metadata["key50"])
    }

    @Test
    fun testLargeErrors() {
        val errors = (1..50).map { "Error #$it" }
        val doc = ParsedDocument(format = testFormat, rawContent = "r", parsedContent = "p",
            errors = errors)
        assertEquals(50, doc.errors.size)
    }

    @Test
    fun testUnicodeContent() {
        val doc = ParsedDocument(
            format = testFormat,
            rawContent = "Hello 世界 🌍 مرحبا",
            parsedContent = "Parsed 世界"
        )
        assertTrue(doc.rawContent.contains("世界"))
        assertTrue(doc.parsedContent.contains("世界"))
    }

    // ==================== escapeHtml edge cases ====================

    @Test
    fun testEscapeHtmlAllSpecialChars() {
        val input = "<div class=\"test\" data-value='5'>A & B</div>"
        val escaped = input.escapeHtml()
        assertFalse(escaped.contains("<"))
        assertFalse(escaped.contains(">"))
        assertFalse(escaped.contains("\""))
        assertFalse(escaped.contains("'"))
        assertTrue(escaped.contains("&amp;"))
        assertTrue(escaped.contains("&lt;"))
        assertTrue(escaped.contains("&gt;"))
        assertTrue(escaped.contains("&quot;"))
        assertTrue(escaped.contains("&#39;"))
    }

    @Test
    fun testEscapeHtmlPreservesNonSpecialChars() {
        val input = "Hello World 123 !@#\$%^*()"
        val escaped = input.escapeHtml()
        assertTrue(escaped.contains("Hello World 123"))
    }

    @Test
    fun testEscapeHtmlDoubleEscaping() {
        val input = "&amp;"
        val escaped = input.escapeHtml()
        assertEquals("&amp;amp;", escaped)
    }

    @Test
    fun testEscapeHtmlUnicode() {
        val input = "日本語 <tag>"
        val escaped = input.escapeHtml()
        assertTrue(escaped.contains("日本語"))
        assertTrue(escaped.contains("&lt;tag&gt;"))
    }

    @Test
    fun testEscapeHtmlNewlines() {
        val input = "line1\nline2\rline3"
        val escaped = input.escapeHtml()
        assertTrue(escaped.contains("\n"))
        assertTrue(escaped.contains("\r"))
    }

    // ==================== ParseOptions edge cases ====================

    @Test
    fun testParseOptionsOverwriteKey() {
        val options = ParseOptions.create()
            .set("key", "first")
            .set("key", "second")
            .build()
        assertEquals("second", options["key"])
    }

    @Test
    fun testParseOptionsDisableLineNumbers() {
        val options = ParseOptions.create()
            .enableLineNumbers(false)
            .build()
        assertEquals(false, options["lineNumbers"])
    }

    @Test
    fun testParseOptionsDisableHighlighting() {
        val options = ParseOptions.create()
            .enableHighlighting(false)
            .build()
        assertEquals(false, options["highlighting"])
    }

    @Test
    fun testParseOptionsBuildIsImmutable() {
        val builder = ParseOptions.create()
            .set("key1", "val1")
        val options1 = builder.build()
        builder.set("key2", "val2")
        val options2 = builder.build()
        // First build should not contain key2
        assertFalse(options1.containsKey("key2"))
        assertTrue(options2.containsKey("key2"))
    }

    @Test
    fun testParseOptionsVariousValueTypes() {
        val options = ParseOptions.create()
            .set("string", "hello")
            .set("int", 42)
            .set("boolean", true)
            .set("double", 3.14)
            .set("list", listOf(1, 2, 3))
            .build()
        assertEquals("hello", options["string"])
        assertEquals(42, options["int"])
        assertEquals(true, options["boolean"])
        assertEquals(3.14, options["double"])
        assertEquals(listOf(1, 2, 3), options["list"])
    }

    // ==================== ParserRegistry edge cases ====================

    @Test
    fun testParserRegistryGetParserByStringKnownFormat() {
        ParserRegistry.clear()
        val format = TextFormat(id = "strtest", name = "StrTest", defaultExtension = ".str")
        val parser = object : TextParser {
            override val supportedFormat = format
            override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
            }
        }
        // Register it with the format registry won't work since FormatRegistry is immutable,
        // but we can test getParser(String) for real format IDs after clearing and re-registering
        ParserRegistry.register(parser)
        // getParser(String) delegates to FormatRegistry.getById() which doesn't know our test format
        val result = ParserRegistry.getParser("strtest")
        // Should return null since "strtest" is not in FormatRegistry
        assertNull(result)
        ParserRegistry.clear()
    }

    @Test
    fun testParserRegistryGetParserByStringNonexistent() {
        ParserRegistry.clear()
        val result = ParserRegistry.getParser("nonexistent_format_xyz")
        assertNull(result)
        ParserRegistry.clear()
    }

    @Test
    fun testParserRegistryDuplicateLazyRegistrationSilentlySkips() {
        ParserRegistry.clear()
        val format = TextFormat(id = "duptest", name = "DupTest", defaultExtension = ".dup")
        ParserRegistry.registerLazy(format.id) {
            object : TextParser {
                override val supportedFormat = format
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
                }
            }
        }
        // Should not throw — silently skips already-registered format
        ParserRegistry.registerLazy(format.id) {
            object : TextParser {
                override val supportedFormat = format
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
                }
            }
        }
        // Verify the original parser still works
        assertNotNull(ParserRegistry.getParser(format))
        ParserRegistry.clear()
    }

    @Test
    fun testParserRegistryMixEagerAndLazySilentlySkips() {
        ParserRegistry.clear()
        val format = TextFormat(id = "mixtest", name = "MixTest", defaultExtension = ".mix")
        val parser = object : TextParser {
            override val supportedFormat = format
            override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
            }
        }
        ParserRegistry.register(parser)
        // Should not throw — silently skips already-registered format
        ParserRegistry.registerLazy(format.id) { parser }
        // Verify the original eager parser still works
        assertNotNull(ParserRegistry.getParser(format))
        ParserRegistry.clear()
    }

    @Test
    fun testParserRegistryHasParserChecksBothMaps() {
        ParserRegistry.clear()
        val eagerFormat = TextFormat(id = "eager1", name = "Eager1", defaultExtension = ".e1")
        val lazyFormat = TextFormat(id = "lazy1", name = "Lazy1", defaultExtension = ".l1")
        val unknownFormat = TextFormat(id = "unknown1", name = "Unknown1", defaultExtension = ".u1")

        val eagerParser = object : TextParser {
            override val supportedFormat = eagerFormat
            override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
            }
        }

        ParserRegistry.register(eagerParser)
        ParserRegistry.registerLazy(lazyFormat.id) {
            object : TextParser {
                override val supportedFormat = lazyFormat
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
                }
            }
        }

        assertTrue(ParserRegistry.hasParser(eagerFormat))
        assertTrue(ParserRegistry.hasParser(lazyFormat))
        assertFalse(ParserRegistry.hasParser(unknownFormat))

        ParserRegistry.clear()
    }

    @Test
    fun testParserRegistryGetAllParsersExcludesLazy() {
        ParserRegistry.clear()
        val format1 = TextFormat(id = "gall1", name = "All1", defaultExtension = ".a1")
        val format2 = TextFormat(id = "gall2", name = "All2", defaultExtension = ".a2")

        ParserRegistry.register(object : TextParser {
            override val supportedFormat = format1
            override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
            }
        })
        ParserRegistry.registerLazy(format2.id) {
            object : TextParser {
                override val supportedFormat = format2
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    return ParsedDocument(format = supportedFormat, rawContent = content, parsedContent = content)
                }
            }
        }

        val allParsers = ParserRegistry.getAllParsers()
        assertEquals(1, allParsers.size)
        assertEquals("gall1", allParsers[0].supportedFormat.id)

        assertEquals(1, ParserRegistry.getInstantiatedParserCount())
        assertEquals(1, ParserRegistry.getPendingParserCount())

        ParserRegistry.clear()
    }
}
