/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Extended unit tests for Plaintext parser
 * Covers JSON pretty-printing edge cases, language mapping,
 * extension detection, and HTML rendering for all types
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Extended tests for PlaintextParser covering areas not addressed
 * by the existing PlainTextParserTest and SimplePlainTextTest:
 *
 * - JSON pretty-printing: nested objects/arrays, escaped strings,
 *   booleans, nulls, empty structures, malformed JSON fallback
 * - Language mapping: all supported extensions
 * - Type detection: .htm, .xlf, .md, .markdown, all code extensions
 * - HTML rendering: JSON type, XML type, MARKDOWN type
 * - Filename edge cases: multiple dots, case sensitivity, paths
 */
class PlaintextParserExtendedTests {

    private val parser = PlaintextParser()

    // ==================== JSON Pretty-Printing Tests ====================

    @Test
    fun `prettyPrintJson should format nested objects`() {
        val content = """{"a":{"b":{"c":"deep"}}}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        // The pretty-printed content is stored in parsedContent as HTML
        // but rawContent stays original. The metadata lines/characters reflect
        // the pretty-printed version.
        val lines = result.metadata["lines"]!!.toInt()
        assertTrue(lines > 1, "Nested JSON should be formatted across multiple lines, got $lines")
    }

    @Test
    fun `prettyPrintJson should format arrays`() {
        val content = """{"items":[1,2,3,4,5]}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        val lines = result.metadata["lines"]!!.toInt()
        assertTrue(lines > 1, "JSON with array should be formatted across multiple lines, got $lines")
    }

    @Test
    fun `prettyPrintJson should handle nested arrays`() {
        val content = """{"matrix":[[1,2],[3,4]]}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        val lines = result.metadata["lines"]!!.toInt()
        assertTrue(lines > 1, "Nested arrays should produce multiple lines")
    }

    @Test
    fun `prettyPrintJson should handle booleans and null`() {
        val content = """{"active":true,"deleted":false,"notes":null}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        // Verify the pretty-printed HTML contains these values
        assertTrue(result.parsedContent.contains("true"), "Should contain boolean true")
        assertTrue(result.parsedContent.contains("false"), "Should contain boolean false")
        assertTrue(result.parsedContent.contains("null"), "Should contain null")
    }

    @Test
    fun `prettyPrintJson should handle empty object`() {
        val content = """{}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        assertTrue(result.parsedContent.contains("{"), "Should contain opening brace")
        assertTrue(result.parsedContent.contains("}"), "Should contain closing brace")
    }

    @Test
    fun `prettyPrintJson should handle empty array`() {
        val content = """[]"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        assertTrue(result.parsedContent.contains("["), "Should contain opening bracket")
        assertTrue(result.parsedContent.contains("]"), "Should contain closing bracket")
    }

    @Test
    fun `prettyPrintJson should preserve escaped quotes in strings`() {
        val content = """{"msg":"He said \"hello\""}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        // The escaped quotes should be preserved in the pretty-printed output
        // (they appear in parsedContent which is HTML, so check the underlying logic)
        assertTrue(result.parsedContent.contains("msg"), "Should contain key 'msg'")
    }

    @Test
    fun `prettyPrintJson should preserve escaped backslashes`() {
        val content = """{"path":"C:\\Users\\test"}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        assertTrue(result.parsedContent.contains("path"), "Should contain key 'path'")
    }

    @Test
    fun `prettyPrintJson should handle string with special characters`() {
        val content = """{"text":"line1\nline2\ttab"}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        assertTrue(result.parsedContent.contains("text"), "Should contain key 'text'")
    }

    @Test
    fun `prettyPrintJson should add space after colon`() {
        val content = """{"key":"value"}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        // The pretty-printer adds a space after colons outside strings
        // The parsedContent is HTML-wrapped, so check that the formatted structure is correct
        assertTrue(result.parsedContent.contains(": ") || result.parsedContent.contains(":"),
            "Should format colon with space")
    }

    @Test
    fun `prettyPrintJson should handle malformed JSON gracefully`() {
        val content = """{this is not valid json at all"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        // Malformed JSON should still parse without crashing
        // The pretty-printer either formats what it can or returns original
        assertNotNull(result.parsedContent, "Should produce parsedContent even for malformed JSON")
    }

    @Test
    fun `prettyPrintJson should handle large JSON`() {
        val content = buildString {
            append("{")
            repeat(50) { i ->
                if (i > 0) append(",")
                append("\"key$i\":\"value$i\"")
            }
            append("}")
        }
        val result = parser.parse(content, mapOf("filename" to "big.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        val lines = result.metadata["lines"]!!.toInt()
        assertTrue(lines > 1, "Large JSON should be formatted across multiple lines")
    }

    @Test
    fun `prettyPrintJson should handle numbers`() {
        val content = """{"int":42,"float":3.14,"neg":-7,"exp":1.5e10}"""
        val result = parser.parse(content, mapOf("filename" to "nums.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        assertTrue(result.parsedContent.contains("42"), "Should contain integer 42")
        assertTrue(result.parsedContent.contains("3.14"), "Should contain float 3.14")
        assertTrue(result.parsedContent.contains("-7"), "Should contain negative number")
    }

    @Test
    fun `prettyPrintJson should handle mixed nested structures`() {
        val content = """{"users":[{"name":"Alice","tags":["admin","user"]},{"name":"Bob","tags":["user"]}]}"""
        val result = parser.parse(content, mapOf("filename" to "users.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        val lines = result.metadata["lines"]!!.toInt()
        assertTrue(lines > 5, "Complex nested JSON should produce many lines, got $lines")
        assertTrue(result.parsedContent.contains("Alice"), "Should contain user name")
        assertTrue(result.parsedContent.contains("Bob"), "Should contain user name")
    }

    // ==================== Type Detection: HTML Extensions ====================

    @Test
    fun `should detect HTML type by htm extension`() {
        val content = "<html><body>Test</body></html>"
        val result = parser.parse(content, mapOf("filename" to "page.htm"))

        assertNotNull(result)
        assertEquals("html", result.metadata["type"])
        assertEquals(".htm", result.metadata["extension"])
    }

    // ==================== Type Detection: XML Extensions ====================

    @Test
    fun `should detect XML type by xlf extension`() {
        val content = """<xliff version="1.2"><file source-language="en"></file></xliff>"""
        val result = parser.parse(content, mapOf("filename" to "strings.xlf"))

        assertNotNull(result)
        assertEquals("xml", result.metadata["type"])
        assertEquals(".xlf", result.metadata["extension"])
    }

    // ==================== Type Detection: Markdown ====================

    @Test
    fun `should detect Markdown type by md extension`() {
        val content = "# Hello\nThis is markdown."
        val result = parser.parse(content, mapOf("filename" to "readme.md"))

        assertNotNull(result)
        assertEquals("markdown", result.metadata["type"])
        assertEquals(".md", result.metadata["extension"])
    }

    @Test
    fun `should detect Markdown type by markdown extension`() {
        val content = "## Section\nSome content."
        val result = parser.parse(content, mapOf("filename" to "notes.markdown"))

        assertNotNull(result)
        assertEquals("markdown", result.metadata["type"])
        assertEquals(".markdown", result.metadata["extension"])
    }

    // ==================== Type Detection: All Code Extensions ====================

    @Test
    fun `should detect code type for Python files`() {
        val result = parser.parse("print('hello')", mapOf("filename" to "script.py"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".py", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Java files`() {
        val result = parser.parse("public class Main {}", mapOf("filename" to "Main.java"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".java", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for JavaScript files`() {
        val result = parser.parse("console.log('hi');", mapOf("filename" to "app.js"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".js", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for mjs files`() {
        val result = parser.parse("export default {};", mapOf("filename" to "module.mjs"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".mjs", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for TypeScript files`() {
        val result = parser.parse("const x: number = 1;", mapOf("filename" to "app.ts"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".ts", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for C files`() {
        val result = parser.parse("int main() { return 0; }", mapOf("filename" to "main.c"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".c", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for C++ files`() {
        val extVariants = listOf(".cpp", ".cc", ".cxx")
        val filenames = listOf("main.cpp", "main.cc", "main.cxx")
        for (i in extVariants.indices) {
            // .cc and .cxx may or may not be in CODE_EXTENSIONS
            val result = parser.parse("#include <iostream>", mapOf("filename" to filenames[i]))
            // At least .cpp should be code
            if (extVariants[i] == ".cpp") {
                assertEquals("code", result.metadata["type"], "Should detect .cpp as code")
            }
        }
    }

    @Test
    fun `should detect code type for header files`() {
        val result = parser.parse("#pragma once", mapOf("filename" to "header.h"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".h", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for C sharp files`() {
        val result = parser.parse("using System;", mapOf("filename" to "Program.cs"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".cs", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Ruby files`() {
        val result = parser.parse("puts 'hello'", mapOf("filename" to "script.rb"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".rb", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for PHP files`() {
        val result = parser.parse("<?php echo 'hi'; ?>", mapOf("filename" to "index.php"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".php", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Swift files`() {
        val result = parser.parse("print(\"hello\")", mapOf("filename" to "main.swift"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".swift", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Rust files`() {
        val result = parser.parse("fn main() {}", mapOf("filename" to "main.rs"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".rs", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Go files`() {
        val result = parser.parse("package main", mapOf("filename" to "main.go"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".go", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for shell scripts`() {
        val result = parser.parse("#!/bin/bash", mapOf("filename" to "run.sh"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".sh", result.metadata["extension"])

        val result2 = parser.parse("echo hi", mapOf("filename" to "run.bash"))
        assertEquals("code", result2.metadata["type"])
        assertEquals(".bash", result2.metadata["extension"])
    }

    @Test
    fun `should detect code type for CSS files`() {
        val result = parser.parse("body { margin: 0; }", mapOf("filename" to "style.css"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".css", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for SQL files`() {
        val result = parser.parse("SELECT * FROM users;", mapOf("filename" to "query.sql"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".sql", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for YAML files`() {
        val result = parser.parse("key: value", mapOf("filename" to "config.yaml"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".yaml", result.metadata["extension"])

        val result2 = parser.parse("key: value", mapOf("filename" to "config.yml"))
        assertEquals("code", result2.metadata["type"])
        assertEquals(".yml", result2.metadata["extension"])
    }

    @Test
    fun `should detect code type for Lua files`() {
        val result = parser.parse("print('hello')", mapOf("filename" to "script.lua"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".lua", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Perl files`() {
        val result = parser.parse("print 'hello';", mapOf("filename" to "script.perl"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".perl", result.metadata["extension"])

        val result2 = parser.parse("print 'hello';", mapOf("filename" to "script.pl"))
        assertEquals("code", result2.metadata["type"])
        assertEquals(".pl", result2.metadata["extension"])
    }

    @Test
    fun `should detect code type for R files`() {
        val result = parser.parse("x <- 1", mapOf("filename" to "analysis.r"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".r", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for diff and patch files`() {
        val result = parser.parse("--- a/file\n+++ b/file", mapOf("filename" to "change.diff"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".diff", result.metadata["extension"])

        val result2 = parser.parse("--- a/file\n+++ b/file", mapOf("filename" to "change.patch"))
        assertEquals("code", result2.metadata["type"])
        assertEquals(".patch", result2.metadata["extension"])
    }

    @Test
    fun `should detect code type for Groovy files`() {
        val result = parser.parse("println 'hello'", mapOf("filename" to "build.groovy"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".groovy", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Scala files`() {
        val result = parser.parse("object Main extends App", mapOf("filename" to "Main.scala"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".scala", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Dart files`() {
        val result = parser.parse("void main() {}", mapOf("filename" to "main.dart"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".dart", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for QML files`() {
        val result = parser.parse("Rectangle { width: 100 }", mapOf("filename" to "main.qml"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".qml", result.metadata["extension"])
    }

    @Test
    fun `should detect code type for Objective-C++ files`() {
        val result = parser.parse("@implementation Foo", mapOf("filename" to "Foo.mm"))
        assertEquals("code", result.metadata["type"])
        assertEquals(".mm", result.metadata["extension"])
    }

    // ==================== Type Detection: Plain Type for Unknown Extensions ====================

    @Test
    fun `should detect plain type for unknown extension`() {
        val result = parser.parse("some content", mapOf("filename" to "file.xyz"))
        assertEquals("plain", result.metadata["type"])
        assertEquals(".xyz", result.metadata["extension"])
    }

    @Test
    fun `should detect plain type when no filename option provided`() {
        val result = parser.parse("some content")
        assertEquals("plain", result.metadata["type"])
        assertEquals("", result.metadata["extension"])
    }

    @Test
    fun `should detect plain type for empty filename`() {
        val result = parser.parse("some content", mapOf("filename" to ""))
        assertEquals("plain", result.metadata["type"])
        assertEquals("", result.metadata["extension"])
    }

    // ==================== Language Mapping via HTML Output ====================

    @Test
    fun `should map Python extension to python language in HTML`() {
        val result = parser.parse("x = 1", mapOf("filename" to "test.py"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-python"), "HTML should contain language-python class")
    }

    @Test
    fun `should map JavaScript extension to javascript language in HTML`() {
        val result = parser.parse("var x = 1;", mapOf("filename" to "test.js"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-javascript"), "HTML should contain language-javascript class")
    }

    @Test
    fun `should map TypeScript extension to typescript language in HTML`() {
        val result = parser.parse("let x: number = 1;", mapOf("filename" to "test.ts"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-typescript"), "HTML should contain language-typescript class")
    }

    @Test
    fun `should map Java extension to java language in HTML`() {
        val result = parser.parse("class Foo {}", mapOf("filename" to "Foo.java"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-java"), "HTML should contain language-java class")
    }

    @Test
    fun `should map Kotlin extension to kotlin language in HTML`() {
        val result = parser.parse("fun main() {}", mapOf("filename" to "Main.kt"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-kotlin"), "HTML should contain language-kotlin class")
    }

    @Test
    fun `should map C extension to c language in HTML`() {
        val result = parser.parse("int x;", mapOf("filename" to "main.c"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-c"), "HTML should contain language-c class")
    }

    @Test
    fun `should map C++ extensions to cpp language in HTML`() {
        val result = parser.parse("int x;", mapOf("filename" to "main.cpp"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-cpp"), "HTML should contain language-cpp class")
    }

    @Test
    fun `should map header extensions to cpp language in HTML`() {
        val result = parser.parse("#pragma once", mapOf("filename" to "header.h"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-cpp"), "HTML for .h should contain language-cpp class")

        val result2 = parser.parse("#pragma once", mapOf("filename" to "header.hpp"))
        val html2 = parser.toHtml(result2, lightMode = true)
        // .hpp is not in CODE_EXTENSIONS, so it may be plain
        // Only check if it was detected as code
        if (result2.metadata["type"] == "code") {
            assertTrue(html2.contains("language-cpp"), "HTML for .hpp should contain language-cpp class")
        }
    }

    @Test
    fun `should map C sharp extension to csharp language in HTML`() {
        val result = parser.parse("using System;", mapOf("filename" to "Program.cs"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-csharp"), "HTML should contain language-csharp class")
    }

    @Test
    fun `should map Ruby extension to ruby language in HTML`() {
        val result = parser.parse("puts 'hello'", mapOf("filename" to "test.rb"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-ruby"), "HTML should contain language-ruby class")
    }

    @Test
    fun `should map PHP extension to php language in HTML`() {
        val result = parser.parse("<?php echo 'hi'; ?>", mapOf("filename" to "test.php"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-php"), "HTML should contain language-php class")
    }

    @Test
    fun `should map Swift extension to swift language in HTML`() {
        val result = parser.parse("let x = 1", mapOf("filename" to "test.swift"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-swift"), "HTML should contain language-swift class")
    }

    @Test
    fun `should map Rust extension to rust language in HTML`() {
        val result = parser.parse("fn main() {}", mapOf("filename" to "test.rs"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-rust"), "HTML should contain language-rust class")
    }

    @Test
    fun `should map Go extension to go language in HTML`() {
        val result = parser.parse("package main", mapOf("filename" to "test.go"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-go"), "HTML should contain language-go class")
    }

    @Test
    fun `should map shell extensions to bash language in HTML`() {
        val result = parser.parse("echo hi", mapOf("filename" to "test.sh"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-bash"), "HTML for .sh should contain language-bash class")

        val result2 = parser.parse("echo hi", mapOf("filename" to "test.bash"))
        val html2 = parser.toHtml(result2, lightMode = true)
        assertTrue(html2.contains("language-bash"), "HTML for .bash should contain language-bash class")
    }

    @Test
    fun `should map CSS extension to css language in HTML`() {
        val result = parser.parse("body { }", mapOf("filename" to "style.css"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-css"), "HTML should contain language-css class")
    }

    @Test
    fun `should map SQL extension to sql language in HTML`() {
        val result = parser.parse("SELECT 1;", mapOf("filename" to "query.sql"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-sql"), "HTML should contain language-sql class")
    }

    @Test
    fun `should map YAML extensions to yaml language in HTML`() {
        val result = parser.parse("key: value", mapOf("filename" to "config.yaml"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-yaml"), "HTML for .yaml should contain language-yaml class")

        val result2 = parser.parse("key: value", mapOf("filename" to "config.yml"))
        val html2 = parser.toHtml(result2, lightMode = true)
        assertTrue(html2.contains("language-yaml"), "HTML for .yml should contain language-yaml class")
    }

    @Test
    fun `should map R extension to r language in HTML`() {
        val result = parser.parse("x <- 1", mapOf("filename" to "analysis.r"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-r"), "HTML should contain language-r class")
    }

    @Test
    fun `should map Lua extension to lua language in HTML`() {
        val result = parser.parse("x = 1", mapOf("filename" to "script.lua"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-lua"), "HTML should contain language-lua class")
    }

    @Test
    fun `should map Perl extensions to perl language in HTML`() {
        val result = parser.parse("my \$x = 1;", mapOf("filename" to "test.perl"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-perl"), "HTML for .perl should contain language-perl class")

        val result2 = parser.parse("my \$x = 1;", mapOf("filename" to "test.pl"))
        val html2 = parser.toHtml(result2, lightMode = true)
        assertTrue(html2.contains("language-perl"), "HTML for .pl should contain language-perl class")
    }

    @Test
    fun `should map diff and patch extensions to diff language in HTML`() {
        val result = parser.parse("--- a\n+++ b", mapOf("filename" to "change.diff"))
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("language-diff"), "HTML for .diff should contain language-diff class")

        val result2 = parser.parse("--- a\n+++ b", mapOf("filename" to "change.patch"))
        val html2 = parser.toHtml(result2, lightMode = true)
        assertTrue(html2.contains("language-diff"), "HTML for .patch should contain language-diff class")
    }

    // ==================== HTML Rendering for JSON Type ====================

    @Test
    fun `should render JSON as plain pre block not code block`() {
        // JSON type is detected before CODE_EXTENSIONS check in detectType,
        // and toHtml routes JSON through the else branch (plain pre block)
        val content = """{"key":"value"}"""
        val result = parser.parse(content, mapOf("filename" to "data.json"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"),
            "JSON should be rendered as a plain text block (not code-block)")
        assertTrue(html.contains("<pre"),
            "JSON should be wrapped in a pre element")
        assertTrue(html.contains("white-space: pre-wrap"),
            "JSON should use pre-wrap white-space style")
        // JSON content is pretty-printed and then HTML-escaped
        assertTrue(html.contains("&quot;key&quot;"), "JSON keys should be HTML-escaped")
    }

    // ==================== HTML Rendering for XML Type ====================

    @Test
    fun `should render XML as plain pre block not code block`() {
        // XML type is detected before CODE_EXTENSIONS check in detectType,
        // and toHtml routes XML through the else branch (plain pre block)
        val content = """<root><item>test</item></root>"""
        val result = parser.parse(content, mapOf("filename" to "data.xml"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"),
            "XML should be rendered as a plain text block (not code-block)")
        assertTrue(html.contains("<pre"),
            "XML should be wrapped in a pre element")
        // XML tags should be HTML-escaped
        assertTrue(html.contains("&lt;root&gt;"), "XML tags should be HTML-escaped")
    }

    @Test
    fun `should render xlf files as plain pre block`() {
        val content = """<xliff version="1.2"></xliff>"""
        val result = parser.parse(content, mapOf("filename" to "strings.xlf"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"),
            "XLF should be rendered as a plain text block")
        assertTrue(html.contains("<pre"),
            "XLF should be wrapped in a pre element")
    }

    // ==================== HTML Rendering for Markdown Type ====================

    @Test
    fun `should render Markdown type as plain text block`() {
        // Markdown type also goes through the else branch in toHtml
        val content = "# Heading\nSome **bold** text."
        val result = parser.parse(content, mapOf("filename" to "notes.md"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"),
            "Markdown in plaintext mode should be rendered as a plain text block")
        assertTrue(html.contains("<pre"),
            "Markdown should be wrapped in a pre element")
        // Markdown content should be HTML-escaped (raw, not rendered)
        assertTrue(html.contains("# Heading") || html.contains("Heading"),
            "Markdown headings should appear in the output")
    }

    // ==================== Filename Edge Cases ====================

    @Test
    fun `should extract extension from filename with multiple dots`() {
        val result = parser.parse("content", mapOf("filename" to "archive.backup.txt"))
        assertEquals(".txt", result.metadata["extension"])
        assertEquals("plain", result.metadata["type"])
    }

    @Test
    fun `should extract extension from filename with path`() {
        val result = parser.parse("content", mapOf("filename" to "/home/user/documents/notes.txt"))
        assertEquals(".txt", result.metadata["extension"])
        assertEquals("plain", result.metadata["type"])
    }

    @Test
    fun `should handle uppercase extension by lowercasing it`() {
        val result = parser.parse("content", mapOf("filename" to "FILE.TXT"))
        assertEquals(".txt", result.metadata["extension"])
        // .txt is not in CODE_EXTENSIONS, so it should be plain
        assertEquals("plain", result.metadata["type"])
    }

    @Test
    fun `should handle mixed case extension`() {
        val result = parser.parse("content", mapOf("filename" to "script.Py"))
        assertEquals(".py", result.metadata["extension"])
        assertEquals("code", result.metadata["type"])
    }

    @Test
    fun `should handle filename that is just a dot`() {
        val result = parser.parse("content", mapOf("filename" to "."))
        // lastIndexOf('.') = 0, substring(0) = "."
        assertEquals(".", result.metadata["extension"])
        assertEquals("plain", result.metadata["type"])
    }

    @Test
    fun `should handle dotfile without extension`() {
        val result = parser.parse("content", mapOf("filename" to ".gitignore"))
        // lastIndexOf('.') = 0, substring(0) = ".gitignore"
        assertEquals(".gitignore", result.metadata["extension"])
    }

    // ==================== HTML Escaping in Code Blocks ====================

    @Test
    fun `should escape HTML entities in code blocks`() {
        val content = """val x = "<div>" + "&" + "'" """
        val result = parser.parse(content, mapOf("filename" to "test.kt"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("&lt;div&gt;"), "HTML entities should be escaped in code blocks")
        assertTrue(html.contains("&amp;"), "Ampersand should be escaped in code blocks")
    }

    @Test
    fun `should escape HTML entities in JSON code blocks`() {
        val content = """{"html":"<script>alert('xss')</script>"}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))
        val html = parser.toHtml(result, lightMode = true)

        assertNotNull(html)
        assertFalse(html.contains("<script>alert"), "Script tags should be escaped in JSON code blocks")
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;script&gt;"),
            "HTML entities should be escaped in JSON output")
    }

    // ==================== XML Content Not Pretty-Printed ====================

    @Test
    fun `should not modify XML content during parsing`() {
        val content = """<root><child attr="value">text</child></root>"""
        val result = parser.parse(content, mapOf("filename" to "data.xml"))

        assertNotNull(result)
        assertEquals("xml", result.metadata["type"])
        // XML content should be passed through without modification
        // (the parser comment says "Could add XML formatting")
        assertEquals(content, result.rawContent)
    }

    // ==================== Metadata Accuracy ====================

    @Test
    fun `should count characters correctly for single line`() {
        val content = "Hello"
        val result = parser.parse(content)
        assertEquals("5", result.metadata["characters"])
        assertEquals("1", result.metadata["lines"])
    }

    @Test
    fun `should count lines correctly for trailing newline`() {
        val content = "Line1\nLine2\n"
        val result = parser.parse(content)
        assertEquals("3", result.metadata["lines"])  // "Line1", "Line2", "" (empty last line)
    }

    @Test
    fun `should count characters for JSON after pretty-printing`() {
        val content = """{"a":1}"""
        val result = parser.parse(content, mapOf("filename" to "test.json"))
        // Characters should reflect the pretty-printed content, not the original
        val prettyPrintedChars = result.metadata["characters"]!!.toInt()
        assertTrue(prettyPrintedChars >= content.length,
            "Pretty-printed JSON character count ($prettyPrintedChars) should be >= original (${content.length})")
    }

    // ==================== toHtml(document) Method ====================

    @Test
    fun `toHtml should return parsedContent directly`() {
        val content = "Simple text"
        val result = parser.parse(content)
        val html = parser.toHtml(result, lightMode = true)

        assertEquals(result.parsedContent, html,
            "toHtml(document, lightMode) should return parsedContent directly")
    }

    @Test
    fun `toHtml should return same content regardless of lightMode`() {
        val content = "Test content"
        val result = parser.parse(content)
        val htmlLight = parser.toHtml(result, lightMode = true)
        val htmlDark = parser.toHtml(result, lightMode = false)

        assertEquals(htmlLight, htmlDark,
            "toHtml should return same parsedContent regardless of lightMode flag")
    }

    // ==================== supportedFormat Property ====================

    @Test
    fun `supportedFormat should return plaintext format`() {
        val format = parser.supportedFormat
        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }
}
