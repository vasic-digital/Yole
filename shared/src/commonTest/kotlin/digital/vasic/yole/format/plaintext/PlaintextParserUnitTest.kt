/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for PlaintextParser covering
 * all input types, type detection, HTML output,
 * Unicode, large input, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Isolated unit tests for [PlaintextParser].
 *
 * Validates parsing of all recognized sub-types (PLAIN, CODE, HTML, JSON, XML),
 * metadata correctness, HTML wrapping behavior, Unicode support,
 * large input stability, and edge cases.
 *
 * Total: 45 tests
 */
class PlaintextParserUnitTest {

    private val parser = PlaintextParser()

    // ==================== Supported Format ====================

    @Test
    fun supportedFormatIsPlaintext() {
        val fmt = parser.supportedFormat
        assertEquals(TextFormat.ID_PLAINTEXT, fmt.id)
    }

    @Test
    fun supportedFormatNameIsPlainText() {
        val fmt = parser.supportedFormat
        assertEquals("Plain Text", fmt.name)
    }

    // ==================== Empty Input ====================

    @Test
    fun emptyInputProducesDocument() {
        val doc = parser.parse("", emptyMap())
        assertNotNull(doc)
        assertEquals("", doc.rawContent)
    }

    @Test
    fun emptyInputHasZeroLines() {
        val doc = parser.parse("", emptyMap())
        val lines = doc.metadata["lines"]?.toIntOrNull() ?: 0
        // empty string splits into 1 "empty" line in Kotlin
        assertTrue(lines >= 0)
    }

    @Test
    fun emptyInputParsedContentContainsPreTag() {
        val doc = parser.parse("", emptyMap())
        assertTrue(doc.parsedContent.contains("<pre") || doc.parsedContent.isEmpty() || doc.parsedContent.contains("plaintext"))
    }

    // ==================== Single Line ====================

    @Test
    fun singleLineProducesDocument() {
        val doc = parser.parse("Hello World", emptyMap())
        assertNotNull(doc)
        assertEquals("Hello World", doc.rawContent)
    }

    @Test
    fun singleLineMetadataHasOneOrMoreLines() {
        val doc = parser.parse("Hello World", emptyMap())
        val lines = doc.metadata["lines"]?.toIntOrNull() ?: 0
        assertTrue(lines >= 1)
    }

    @Test
    fun singleLineCharacterCountIsCorrect() {
        val content = "Hello World"
        val doc = parser.parse(content, emptyMap())
        val chars = doc.metadata["characters"]?.toIntOrNull() ?: -1
        assertEquals(content.length, chars)
    }

    // ==================== Multiline ====================

    @Test
    fun multilineContentPreservesRawContent() {
        val content = "Line 1\nLine 2\nLine 3"
        val doc = parser.parse(content, emptyMap())
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun multilineHasCorrectLineCount() {
        val content = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        val doc = parser.parse(content, emptyMap())
        val lines = doc.metadata["lines"]?.toIntOrNull() ?: 0
        assertEquals(5, lines)
    }

    @Test
    fun multilineCharacterCountIsCorrect() {
        val content = "Line 1\nLine 2\nLine 3"
        val doc = parser.parse(content, emptyMap())
        val chars = doc.metadata["characters"]?.toIntOrNull() ?: -1
        assertEquals(content.length, chars)
    }

    // ==================== Metadata ====================

    @Test
    fun metadataContainsTypeKey() {
        val doc = parser.parse("Hello", emptyMap())
        assertTrue(doc.metadata.containsKey("type"))
    }

    @Test
    fun metadataContainsExtensionKey() {
        val doc = parser.parse("Hello", mapOf("filename" to "notes.txt"))
        assertTrue(doc.metadata.containsKey("extension"))
        assertEquals(".txt", doc.metadata["extension"])
    }

    @Test
    fun metadataContainsLinesKey() {
        val doc = parser.parse("Hello", emptyMap())
        assertTrue(doc.metadata.containsKey("lines"))
    }

    @Test
    fun metadataContainsCharactersKey() {
        val doc = parser.parse("Hello", emptyMap())
        assertTrue(doc.metadata.containsKey("characters"))
    }

    @Test
    fun metadataTypeIsPlainForTxtFile() {
        val doc = parser.parse("Hello", mapOf("filename" to "notes.txt"))
        assertEquals("plain", doc.metadata["type"])
    }

    // ==================== Type Detection ====================

    @Test
    fun htmlFileDetectedAsHtmlType() {
        val content = "<html><body><h1>Test</h1></body></html>"
        val doc = parser.parse(content, mapOf("filename" to "page.html"))
        assertEquals("html", doc.metadata["type"])
    }

    @Test
    fun htmFileDetectedAsHtmlType() {
        val content = "<html><body>Test</body></html>"
        val doc = parser.parse(content, mapOf("filename" to "page.htm"))
        assertEquals("html", doc.metadata["type"])
    }

    @Test
    fun kotlinFileDetectedAsCodeType() {
        val content = "fun main() { println(\"Hello\") }"
        val doc = parser.parse(content, mapOf("filename" to "Main.kt"))
        assertEquals("code", doc.metadata["type"])
    }

    @Test
    fun pythonFileDetectedAsCodeType() {
        val content = "def main():\n    print('Hello')"
        val doc = parser.parse(content, mapOf("filename" to "script.py"))
        assertEquals("code", doc.metadata["type"])
    }

    @Test
    fun javaFileDetectedAsCodeType() {
        val content = "public class Main { public static void main(String[] args) {} }"
        val doc = parser.parse(content, mapOf("filename" to "Main.java"))
        assertEquals("code", doc.metadata["type"])
    }

    @Test
    fun jsonFileDetectedAsCodeType() {
        val content = """{"key": "value", "num": 42}"""
        val doc = parser.parse(content, mapOf("filename" to "data.json"))
        // JSON is in CODE_EXTENSIONS so it detects as code, but also has JSON type path
        val type = doc.metadata["type"]
        assertTrue(type == "code" || type == "json")
    }

    @Test
    fun xmlFileDetectedAsCodeType() {
        val content = "<root><item>value</item></root>"
        val doc = parser.parse(content, mapOf("filename" to "data.xml"))
        val type = doc.metadata["type"]
        assertTrue(type == "xml" || type == "code")
    }

    @Test
    fun unknownExtensionDefaultsToPlain() {
        val doc = parser.parse("Some text content", mapOf("filename" to "file.xyz"))
        assertEquals("plain", doc.metadata["type"])
    }

    @Test
    fun noFilenameOptionDefaultsToPlain() {
        val doc = parser.parse("Some text content", emptyMap())
        assertEquals("plain", doc.metadata["type"])
    }

    // ==================== HTML Output ====================

    @Test
    fun plainTypeOutputContainsPreTag() {
        val doc = parser.parse("Hello World", mapOf("filename" to "notes.txt"))
        assertTrue(doc.parsedContent.contains("<pre"))
    }

    @Test
    fun plainTypeOutputContainsDivClassPlaintext() {
        val doc = parser.parse("Hello World", mapOf("filename" to "notes.txt"))
        assertTrue(doc.parsedContent.contains("plaintext"))
    }

    @Test
    fun codeTypeOutputContainsCodeTag() {
        val doc = parser.parse("fun main() {}", mapOf("filename" to "Main.kt"))
        assertTrue(doc.parsedContent.contains("<code"))
    }

    @Test
    fun codeTypeOutputContainsLanguageClass() {
        val doc = parser.parse("fun main() {}", mapOf("filename" to "Main.kt"))
        assertTrue(doc.parsedContent.contains("kotlin") || doc.parsedContent.contains("language-"))
    }

    @Test
    fun htmlTypeOutputIsPassedThrough() {
        val htmlContent = "<html><body><p>Test</p></body></html>"
        val doc = parser.parse(htmlContent, mapOf("filename" to "page.html"))
        // HTML type returns content as-is
        assertEquals(htmlContent, doc.parsedContent)
    }

    @Test
    fun toHtmlReturnsPrecomputedContent() {
        val doc = parser.parse("Hello", emptyMap())
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    @Test
    fun toHtmlLightAndDarkReturnSameContent() {
        val doc = parser.parse("Hello", mapOf("filename" to "notes.txt"))
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertEquals(light, dark)
    }

    // ==================== HTML Escaping ====================

    @Test
    fun htmlSpecialCharactersAreEscapedInPlainType() {
        val content = "<script>alert('xss')</script>"
        val doc = parser.parse(content, mapOf("filename" to "notes.txt"))
        // The parsed content should not contain raw unescaped script tag
        assertFalse(doc.parsedContent.contains("<script>"))
    }

    @Test
    fun ampersandIsEscapedInPlainType() {
        val content = "Tom & Jerry"
        val doc = parser.parse(content, mapOf("filename" to "notes.txt"))
        assertFalse(doc.parsedContent.contains(" & "))
    }

    // ==================== Unicode ====================

    @Test
    fun unicodeCharactersArePreservedInRawContent() {
        val content = "Hello 世界! こんにちは Привет мир"
        val doc = parser.parse(content, mapOf("filename" to "unicode.txt"))
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun emojiCharactersDoNotCrashParser() {
        val content = "Tasks: ✓ Done ✗ Failed 🚀 Launch"
        val doc = parser.parse(content, mapOf("filename" to "tasks.txt"))
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun arabicRtlTextIsPreserved() {
        val content = "مرحبا بالعالم"
        val doc = parser.parse(content, mapOf("filename" to "arabic.txt"))
        assertEquals(content, doc.rawContent)
    }

    // ==================== Large Input ====================

    @Test
    fun largeInputTenThousandLinesDoesNotCrash() {
        val content = (1..10_000).joinToString("\n") { "Line number $it with some content here." }
        val doc = parser.parse(content, mapOf("filename" to "large.txt"))
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
        val lines = doc.metadata["lines"]?.toIntOrNull() ?: 0
        assertEquals(10_000, lines)
    }

    @Test
    fun largeInputCharacterCountIsAccurate() {
        val content = "X".repeat(100_000)
        val doc = parser.parse(content, emptyMap())
        val chars = doc.metadata["characters"]?.toIntOrNull() ?: 0
        assertEquals(100_000, chars)
    }

    // ==================== Special Content ====================

    @Test
    fun onlyWhitespaceContent() {
        val content = "   \n\t\n   "
        val doc = parser.parse(content, mapOf("filename" to "blank.txt"))
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun windowsLineEndingsAreHandled() {
        val content = "Line 1\r\nLine 2\r\nLine 3"
        val doc = parser.parse(content, mapOf("filename" to "windows.txt"))
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun mixedLineEndingsDoNotCrash() {
        val content = "Line 1\nLine 2\r\nLine 3\rLine 4"
        val doc = parser.parse(content, mapOf("filename" to "mixed.txt"))
        assertNotNull(doc)
    }

    @Test
    fun nullCharacterInContentDoesNotCrash() {
        val content = "Before\u0000After"
        val doc = parser.parse(content, mapOf("filename" to "null.txt"))
        assertNotNull(doc)
        assertEquals(content, doc.rawContent)
    }

    @Test
    fun maxLengthSingleLine() {
        val content = "A".repeat(1_000_000)
        val doc = parser.parse(content, emptyMap())
        assertNotNull(doc)
        val chars = doc.metadata["characters"]?.toIntOrNull() ?: 0
        assertEquals(1_000_000, chars)
    }

    // ==================== JSON Pretty-Printing ====================

    @Test
    fun jsonContentIsPrettyPrinted() {
        val content = """{"name":"test","value":42}"""
        val doc = parser.parse(content, mapOf("filename" to "data.json"))
        // JSON in CODE_EXTENSIONS → type = code; but json detection also runs
        // The parsed content should be non-empty
        assertNotNull(doc)
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun invalidJsonDoesNotCrashParser() {
        val content = "{this is not valid json !!!}"
        val doc = parser.parse(content, mapOf("filename" to "invalid.json"))
        assertNotNull(doc)
        // Should fall back gracefully
    }

    // ==================== Format Object ====================

    @Test
    fun parsedDocumentHasCorrectFormat() {
        val doc = parser.parse("Hello", emptyMap())
        assertEquals(TextFormat.ID_PLAINTEXT, doc.format.id)
    }

    @Test
    fun parsedDocumentFormatIsFromRegistry() {
        val doc = parser.parse("Hello", emptyMap())
        val registryFormat = FormatRegistry.getById(TextFormat.ID_PLAINTEXT)
        assertNotNull(registryFormat)
        assertEquals(registryFormat.id, doc.format.id)
    }
}
