/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for PlaintextParser HTML generation paths
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlaintextParserHtmlTest {

    private val parser = PlaintextParser()

    // ==================== Plain text HTML generation ====================

    @Test
    fun testPlainTextWrapsInPreBlock() {
        val doc = parser.parse("Hello world")
        assertTrue(doc.parsedContent.contains("<pre"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    @Test
    fun testPlainTextHasPlaintextClass() {
        val doc = parser.parse("Hello world")
        assertTrue(doc.parsedContent.contains("class='plaintext'"))
    }

    @Test
    fun testPlainTextEscapesHtml() {
        val doc = parser.parse("<script>alert('xss')</script>")
        assertFalse(doc.parsedContent.contains("<script>"))
        assertTrue(doc.parsedContent.contains("&lt;script&gt;"))
    }

    @Test
    fun testPlainTextPreservesWhitespace() {
        val doc = parser.parse("Hello world")
        assertTrue(doc.parsedContent.contains("white-space: pre-wrap"))
    }

    @Test
    fun testPlainTextUsesMonospaceFont() {
        val doc = parser.parse("code-like text")
        assertTrue(doc.parsedContent.contains("font-family: monospace"))
    }

    // ==================== HTML file type detection ====================

    @Test
    fun testHtmlFileRendersAsIs() {
        val htmlContent = "<div><p>Hello</p></div>"
        val doc = parser.parse(htmlContent, mapOf("filename" to "page.html"))
        assertEquals(htmlContent, doc.parsedContent)
    }

    @Test
    fun testHtmExtensionDetectedAsHtml() {
        val htmlContent = "<p>Hello</p>"
        val doc = parser.parse(htmlContent, mapOf("filename" to "page.htm"))
        assertEquals(htmlContent, doc.parsedContent)
    }

    // ==================== Code file HTML generation ====================

    @Test
    fun testCodeFileWrapsInCodeBlock() {
        val doc = parser.parse("def hello():\n    print('hi')", mapOf("filename" to "test.py"))
        assertTrue(doc.parsedContent.contains("<code"))
        assertTrue(doc.parsedContent.contains("</code>"))
    }

    @Test
    fun testCodeFileHasLanguageClass() {
        val doc = parser.parse("fn main() {}", mapOf("filename" to "main.rs"))
        assertTrue(doc.parsedContent.contains("language-rust"))
    }

    @Test
    fun testPythonLanguageMapping() {
        val doc = parser.parse("print('hi')", mapOf("filename" to "test.py"))
        assertTrue(doc.parsedContent.contains("language-python"))
    }

    @Test
    fun testJavaScriptLanguageMapping() {
        val doc = parser.parse("console.log('hi')", mapOf("filename" to "app.js"))
        assertTrue(doc.parsedContent.contains("language-javascript"))
    }

    @Test
    fun testTypeScriptLanguageMapping() {
        val doc = parser.parse("const x: number = 1", mapOf("filename" to "app.ts"))
        assertTrue(doc.parsedContent.contains("language-typescript"))
    }

    @Test
    fun testKotlinLanguageMapping() {
        val doc = parser.parse("fun main() {}", mapOf("filename" to "Main.kt"))
        assertTrue(doc.parsedContent.contains("language-kotlin"))
    }

    @Test
    fun testJavaLanguageMapping() {
        val doc = parser.parse("class Main {}", mapOf("filename" to "Main.java"))
        assertTrue(doc.parsedContent.contains("language-java"))
    }

    @Test
    fun testCppLanguageMapping() {
        val doc = parser.parse("#include <iostream>", mapOf("filename" to "main.cpp"))
        assertTrue(doc.parsedContent.contains("language-cpp"))
    }

    @Test
    fun testCLanguageMapping() {
        val doc = parser.parse("#include <stdio.h>", mapOf("filename" to "main.c"))
        assertTrue(doc.parsedContent.contains("language-c"))
    }

    @Test
    fun testGoLanguageMapping() {
        val doc = parser.parse("package main", mapOf("filename" to "main.go"))
        assertTrue(doc.parsedContent.contains("language-go"))
    }

    @Test
    fun testRubyLanguageMapping() {
        val doc = parser.parse("puts 'hello'", mapOf("filename" to "app.rb"))
        assertTrue(doc.parsedContent.contains("language-ruby"))
    }

    @Test
    fun testBashLanguageMapping() {
        val doc = parser.parse("echo hello", mapOf("filename" to "script.sh"))
        assertTrue(doc.parsedContent.contains("language-bash"))
    }

    @Test
    fun testSqlLanguageMapping() {
        val doc = parser.parse("SELECT * FROM table", mapOf("filename" to "query.sql"))
        assertTrue(doc.parsedContent.contains("language-sql"))
    }

    @Test
    fun testSwiftLanguageMapping() {
        val doc = parser.parse("print(\"hi\")", mapOf("filename" to "main.swift"))
        assertTrue(doc.parsedContent.contains("language-swift"))
    }

    @Test
    fun testCssLanguageMapping() {
        val doc = parser.parse("body { color: red; }", mapOf("filename" to "style.css"))
        assertTrue(doc.parsedContent.contains("language-css"))
    }

    @Test
    fun testYamlLanguageMapping() {
        val doc = parser.parse("key: value", mapOf("filename" to "config.yaml"))
        assertTrue(doc.parsedContent.contains("language-yaml"))
    }

    @Test
    fun testPhpLanguageMapping() {
        val doc = parser.parse("<?php echo 'hi'; ?>", mapOf("filename" to "index.php"))
        assertTrue(doc.parsedContent.contains("language-php"))
    }

    @Test
    fun testCodeBlockEscapesHtml() {
        val doc = parser.parse("x < y && z > w", mapOf("filename" to "test.py"))
        assertTrue(doc.parsedContent.contains("&lt;"))
        assertTrue(doc.parsedContent.contains("&gt;"))
        assertTrue(doc.parsedContent.contains("&amp;"))
    }

    @Test
    fun testCodeBlockHasPreWrapper() {
        val doc = parser.parse("x = 1", mapOf("filename" to "test.py"))
        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun testCodeBlockHasCodeBlockClass() {
        val doc = parser.parse("x = 1", mapOf("filename" to "test.py"))
        assertTrue(doc.parsedContent.contains("code-block"))
    }

    // ==================== JSON pretty printing ====================

    @Test
    fun testJsonTypeDetection() {
        val doc = parser.parse("{\"key\": \"value\"}", mapOf("filename" to "data.json"))
        val metadata = doc.metadata
        assertEquals("json", metadata["type"])
    }

    @Test
    fun testJsonPrettyPrinting() {
        val json = "{\"name\":\"Alice\",\"age\":30}"
        val doc = parser.parse(json, mapOf("filename" to "data.json"))
        // Pretty-printed JSON should have newlines
        assertTrue(doc.parsedContent.contains("\n"))
    }

    @Test
    fun testJsonPrettyPrintingWithNested() {
        val json = "{\"user\":{\"name\":\"Bob\",\"items\":[1,2,3]}}"
        val doc = parser.parse(json, mapOf("filename" to "data.json"))
        assertTrue(doc.parsedContent.contains("\n"))
    }

    @Test
    fun testJsonPrettyPrintingPreservesStrings() {
        val json = "{\"message\":\"Hello, world!\"}"
        val doc = parser.parse(json, mapOf("filename" to "data.json"))
        assertTrue(doc.parsedContent.contains("Hello, world!"))
    }

    @Test
    fun testJsonPrettyPrintInvalidJsonReturnsOriginal() {
        val notJson = "this is not json"
        val doc = parser.parse(notJson, mapOf("filename" to "data.json"))
        // Even if not valid JSON, parser should not crash
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun testJsonPrettyPrintEscapedStrings() {
        val json = "{\"path\":\"C:\\\\Users\\\\test\"}"
        val doc = parser.parse(json, mapOf("filename" to "data.json"))
        assertNotNull(doc.parsedContent)
    }

    // ==================== XML type detection ====================

    @Test
    fun testXmlTypeDetection() {
        val doc = parser.parse("<root/>", mapOf("filename" to "data.xml"))
        assertEquals("xml", doc.metadata["type"])
    }

    @Test
    fun testXlfExtensionDetectedAsXml() {
        val doc = parser.parse("<xliff/>", mapOf("filename" to "strings.xlf"))
        assertEquals("xml", doc.metadata["type"])
    }

    // ==================== Metadata generation ====================

    @Test
    fun testMetadataContainsLineCount() {
        val content = "line1\nline2\nline3"
        val doc = parser.parse(content)
        assertEquals("3", doc.metadata["lines"])
    }

    @Test
    fun testMetadataContainsCharacterCount() {
        val content = "Hello"
        val doc = parser.parse(content)
        assertEquals("5", doc.metadata["characters"])
    }

    @Test
    fun testMetadataContainsTypeForPlain() {
        val doc = parser.parse("plain text")
        assertEquals("plain", doc.metadata["type"])
    }

    @Test
    fun testMetadataContainsExtension() {
        val doc = parser.parse("code", mapOf("filename" to "test.py"))
        assertEquals(".py", doc.metadata["extension"])
    }

    @Test
    fun testMetadataExtensionEmptyWhenNoFilename() {
        val doc = parser.parse("text")
        assertEquals("", doc.metadata["extension"])
    }

    // ==================== Extension extraction ====================

    @Test
    fun testNoExtensionReturnsEmpty() {
        val doc = parser.parse("content", mapOf("filename" to "Makefile"))
        assertEquals("", doc.metadata["extension"])
    }

    @Test
    fun testMultipleDotExtension() {
        val doc = parser.parse("content", mapOf("filename" to "archive.tar.gz"))
        assertEquals(".gz", doc.metadata["extension"])
    }

    @Test
    fun testUppercaseExtensionNormalized() {
        val doc = parser.parse("content", mapOf("filename" to "README.TXT"))
        // Extension should be lowercased
        assertEquals(".txt", doc.metadata["extension"])
    }

    // ==================== toHtml(document) pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("Hello")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    @Test
    fun testToHtmlLightAndDarkSame() {
        val doc = parser.parse("Hello")
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertEquals(light, dark)
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContentProducesValidHtml() {
        val doc = parser.parse("")
        assertNotNull(doc.parsedContent)
        assertTrue(doc.parsedContent.isNotEmpty())
    }

    @Test
    fun testEmptyContentMetadata() {
        val doc = parser.parse("")
        assertEquals("1", doc.metadata["lines"]) // empty string still has 1 "line"
        assertEquals("0", doc.metadata["characters"])
    }

    // ==================== PlaintextType enum ====================

    @Test
    fun testAllPlaintextTypes() {
        assertEquals(6, PlaintextType.entries.size)
        PlaintextType.PLAIN
        PlaintextType.HTML
        PlaintextType.CODE
        PlaintextType.JSON
        PlaintextType.XML
        PlaintextType.MARKDOWN
    }

    // ==================== Markdown in plaintext mode ====================

    @Test
    fun testMarkdownExtensionDetected() {
        val doc = parser.parse("# Heading", mapOf("filename" to "README.md"))
        assertEquals("markdown", doc.metadata["type"])
    }
}
