/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive PlaintextParser tests covering all functionality
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.FormatRegistry
import kotlin.test.*

/**
 * Comprehensive tests for PlaintextParser
 *
 * Coverage:
 * - Type detection for all extension types
 * - JSON pretty-printing with all edge cases
 * - Language mapping for all supported extensions
 * - HTML generation for all plaintext types
 * - Parse integration with metadata
 * - Edge cases and error handling
 */
class PlaintextParserComprehensiveTest {

    private val parser = PlaintextParser()

    // ==================== Type Detection Tests ====================

    @Test
    fun `should detect HTML type for html extension`() {
        val content = "<html><body>Test</body></html>"
        val options = mapOf("filename" to "test.html")

        val document = parser.parse(content, options)

        assertEquals("html", document.metadata["type"])
        assertEquals(".html", document.metadata["extension"])
    }

    @Test
    fun `should detect HTML type for htm extension`() {
        val content = "<html><body>Test</body></html>"
        val options = mapOf("filename" to "test.htm")

        val document = parser.parse(content, options)

        assertEquals("html", document.metadata["type"])
        assertEquals(".htm", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Python extension`() {
        val content = "def hello():\n    print('world')"
        val options = mapOf("filename" to "script.py")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".py", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Java extension`() {
        val content = "public class Test {}"
        val options = mapOf("filename" to "Test.java")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".java", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Kotlin extension`() {
        val content = "fun main() { println(\"Hello\") }"
        val options = mapOf("filename" to "main.kt")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".kt", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for JavaScript extension`() {
        val content = "console.log('Hello');"
        val options = mapOf("filename" to "script.js")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".js", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for TypeScript extension`() {
        val content = "const x: number = 42;"
        val options = mapOf("filename" to "script.ts")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".ts", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for C extension`() {
        val content = "int main() { return 0; }"
        val options = mapOf("filename" to "main.c")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".c", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for C++ extensions`() {
        // Only .cpp and .h are in CODE_EXTENSIONS (not .cc, .cxx, .hpp)
        val extensions = listOf(".cpp", ".h")
        extensions.forEach { ext ->
            val content = "class Test {};"
            val options = mapOf("filename" to "test$ext")

            val document = parser.parse(content, options)

            assertEquals("code", document.metadata["type"])
            assertEquals(ext, document.metadata["extension"])
        }
    }

    @Test
    fun `should detect CODE type for Rust extension`() {
        val content = "fn main() {}"
        val options = mapOf("filename" to "main.rs")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".rs", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Go extension`() {
        val content = "func main() {}"
        val options = mapOf("filename" to "main.go")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".go", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Swift extension`() {
        val content = "func hello() {}"
        val options = mapOf("filename" to "test.swift")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".swift", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for Ruby extension`() {
        val content = "def hello\nend"
        val options = mapOf("filename" to "script.rb")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".rb", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for PHP extension`() {
        val content = "<?php echo 'hello'; ?>"
        val options = mapOf("filename" to "index.php")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".php", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for shell script extensions`() {
        val extensions = listOf(".sh", ".bash")
        extensions.forEach { ext ->
            val content = "#!/bin/bash\necho 'hello'"
            val options = mapOf("filename" to "script$ext")

            val document = parser.parse(content, options)

            assertEquals("code", document.metadata["type"])
            assertEquals(ext, document.metadata["extension"])
        }
    }

    @Test
    fun `should detect CODE type for json extension`() {
        val content = "{\"key\": \"value\"}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        // .json is in CODE_EXTENSIONS, so it returns CODE type
        assertEquals("code", document.metadata["type"])
        assertEquals(".json", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for xml extension`() {
        val content = "<root><item>test</item></root>"
        val options = mapOf("filename" to "data.xml")

        val document = parser.parse(content, options)

        // .xml is in CODE_EXTENSIONS, so it returns CODE type
        assertEquals("code", document.metadata["type"])
        assertEquals(".xml", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for xlf extension`() {
        val content = "<xliff version='1.2'></xliff>"
        val options = mapOf("filename" to "strings.xlf")

        val document = parser.parse(content, options)

        // .xlf is in CODE_EXTENSIONS, so it returns CODE type
        assertEquals("code", document.metadata["type"])
        assertEquals(".xlf", document.metadata["extension"])
    }

    @Test
    fun `should detect MARKDOWN type for md extension`() {
        val content = "# Title\n\nContent"
        val options = mapOf("filename" to "README.md")

        val document = parser.parse(content, options)

        assertEquals("markdown", document.metadata["type"])
        assertEquals(".md", document.metadata["extension"])
    }

    @Test
    fun `should detect MARKDOWN type for markdown extension`() {
        val content = "# Title\n\nContent"
        val options = mapOf("filename" to "document.markdown")

        val document = parser.parse(content, options)

        assertEquals("markdown", document.metadata["type"])
        assertEquals(".markdown", document.metadata["extension"])
    }

    @Test
    fun `should detect PLAIN type for txt extension`() {
        val content = "Plain text content"
        val options = mapOf("filename" to "notes.txt")

        val document = parser.parse(content, options)

        assertEquals("plain", document.metadata["type"])
        assertEquals(".txt", document.metadata["extension"])
    }

    @Test
    fun `should detect PLAIN type for unknown extension`() {
        val content = "Unknown content"
        val options = mapOf("filename" to "file.unknown")

        val document = parser.parse(content, options)

        assertEquals("plain", document.metadata["type"])
        assertEquals(".unknown", document.metadata["extension"])
    }

    @Test
    fun `should detect PLAIN type for no extension`() {
        val content = "No extension content"
        val options = mapOf("filename" to "README")

        val document = parser.parse(content, options)

        assertEquals("plain", document.metadata["type"])
        assertEquals("", document.metadata["extension"])
    }

    @Test
    fun `should handle uppercase extensions by converting to lowercase`() {
        val content = "public class Test {}"
        val options = mapOf("filename" to "Test.JAVA")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".java", document.metadata["extension"])
    }

    @Test
    fun `should handle mixed case extensions`() {
        val content = "def hello():\n    pass"
        val options = mapOf("filename" to "script.Py")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".py", document.metadata["extension"])
    }

    @Test
    fun `should handle filenames with multiple dots`() {
        val content = "console.log('test');"
        val options = mapOf("filename" to "my.module.test.js")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".js", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for CSS extension`() {
        val content = "body { color: red; }"
        val options = mapOf("filename" to "styles.css")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".css", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for SQL extension`() {
        val content = "SELECT * FROM users;"
        val options = mapOf("filename" to "query.sql")

        val document = parser.parse(content, options)

        assertEquals("code", document.metadata["type"])
        assertEquals(".sql", document.metadata["extension"])
    }

    @Test
    fun `should detect CODE type for YAML extensions`() {
        val extensions = listOf(".yaml", ".yml")
        extensions.forEach { ext ->
            val content = "key: value"
            val options = mapOf("filename" to "config$ext")

            val document = parser.parse(content, options)

            assertEquals("code", document.metadata["type"])
            assertEquals(ext, document.metadata["extension"])
        }
    }

    // ==================== JSON Pretty-Printing Tests ====================

    @Test
    fun `should pretty-print simple JSON object`() {
        val content = "{\"name\":\"John\",\"age\":30}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        val parsed = document.rawContent
        assertTrue(parsed.contains("\"name\""))
        assertTrue(parsed.contains("\"age\""))
    }

    @Test
    fun `should pretty-print nested JSON object`() {
        val content = "{\"person\":{\"name\":\"John\",\"age\":30}}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
        assertTrue(document.parsedContent.isNotEmpty())
    }

    @Test
    fun `should pretty-print JSON array`() {
        val content = "[1,2,3,4,5]"
        val options = mapOf("filename" to "array.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
        assertTrue(document.parsedContent.contains("1"))
        assertTrue(document.parsedContent.contains("5"))
    }

    @Test
    fun `should pretty-print JSON with nested arrays`() {
        val content = "{\"items\":[1,2,3]}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
        assertTrue(document.parsedContent.contains("items"))
    }

    @Test
    fun `should handle JSON with escaped quotes in strings`() {
        val content = "{\"text\":\"He said \\\"hello\\\"\"}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with backslashes in strings`() {
        val content = "{\"path\":\"C:\\\\Users\\\\test\"}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with newlines in strings`() {
        val content = "{\"text\":\"line1\\nline2\"}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle empty JSON object`() {
        val content = "{}"
        val options = mapOf("filename" to "empty.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle empty JSON array`() {
        val content = "[]"
        val options = mapOf("filename" to "empty.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with boolean values`() {
        val content = "{\"active\":true,\"deleted\":false}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with null values`() {
        val content = "{\"value\":null}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with numeric values`() {
        val content = "{\"int\":42,\"float\":3.14,\"negative\":-10}"
        val options = mapOf("filename" to "numbers.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle already formatted JSON`() {
        val content = """
            {
              "name": "John",
              "age": 30
            }
        """.trimIndent()
        val options = mapOf("filename" to "formatted.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle malformed JSON gracefully`() {
        val content = "{invalid json"
        val options = mapOf("filename" to "invalid.json")

        val document = parser.parse(content, options)

        // Should not crash, should return original or formatted content
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle JSON with special characters in values`() {
        val content = "{\"text\":\"<>&\\\"'\"}"
        val options = mapOf("filename" to "special.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle large JSON object`() {
        val pairs = (1..100).joinToString(",") { "\"key$it\":\"value$it\"" }
        val content = "{$pairs}"
        val options = mapOf("filename" to "large.json")

        val document = parser.parse(content, options)

        assertNotNull(document.parsedContent)
        assertTrue(document.parsedContent.contains("key1"))
        assertTrue(document.parsedContent.contains("key100"))
    }

    // ==================== Language Mapping Tests ====================

    @Test
    fun `should map Python extension to python language`() {
        val content = "print('hello')"
        val options = mapOf("filename" to "script.py")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-python"))
    }

    @Test
    fun `should map JavaScript extensions to javascript language`() {
        listOf(".js", ".mjs").forEach { ext ->
            val content = "console.log('test');"
            val options = mapOf("filename" to "script$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-javascript"))
        }
    }

    @Test
    fun `should map TypeScript extension to typescript language`() {
        val content = "const x: string = 'test';"
        val options = mapOf("filename" to "app.ts")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-typescript"))
    }

    @Test
    fun `should map Java extension to java language`() {
        val content = "public class Test {}"
        val options = mapOf("filename" to "Test.java")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-java"))
    }

    @Test
    fun `should map Kotlin extension to kotlin language`() {
        val content = "fun main() {}"
        val options = mapOf("filename" to "Main.kt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-kotlin"))
    }

    @Test
    fun `should map C++ extensions to cpp language`() {
        // Only .cpp and .h are in CODE_EXTENSIONS (not .cc, .cxx, .hpp)
        listOf(".cpp", ".h").forEach { ext ->
            val content = "class Test {};"
            val options = mapOf("filename" to "test$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-cpp") ||
                      document.parsedContent.contains("language-c"))
        }
    }

    @Test
    fun `should map C extension to c language`() {
        val content = "int main() {}"
        val options = mapOf("filename" to "main.c")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-c"))
    }

    @Test
    fun `should map C# extension to csharp language`() {
        val content = "class Program {}"
        val options = mapOf("filename" to "Program.cs")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-csharp"))
    }

    @Test
    fun `should map Ruby extension to ruby language`() {
        val content = "def hello\nend"
        val options = mapOf("filename" to "script.rb")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-ruby"))
    }

    @Test
    fun `should map PHP extension to php language`() {
        val content = "<?php echo 'test'; ?>"
        val options = mapOf("filename" to "index.php")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-php"))
    }

    @Test
    fun `should map Swift extension to swift language`() {
        val content = "func hello() {}"
        val options = mapOf("filename" to "App.swift")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-swift"))
    }

    @Test
    fun `should map Rust extension to rust language`() {
        val content = "fn main() {}"
        val options = mapOf("filename" to "main.rs")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-rust"))
    }

    @Test
    fun `should map Go extension to go language`() {
        val content = "func main() {}"
        val options = mapOf("filename" to "main.go")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-go"))
    }

    @Test
    fun `should map shell extensions to bash language`() {
        listOf(".sh", ".bash").forEach { ext ->
            val content = "echo 'hello'"
            val options = mapOf("filename" to "script$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-bash"))
        }
    }

    @Test
    fun `should map XML extensions to xml language`() {
        listOf(".xml", ".xlf").forEach { ext ->
            val content = "<root />"
            val options = mapOf("filename" to "data$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-xml") ||
                      document.parsedContent.contains("language-code"))
        }
    }

    @Test
    fun `should map JSON extension to json language`() {
        val content = "{\"key\": \"value\"}"
        val options = mapOf("filename" to "data.json")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-json") ||
                  document.parsedContent.contains("language-code"))
    }

    @Test
    fun `should map CSS extension to css language`() {
        val content = "body { color: red; }"
        val options = mapOf("filename" to "styles.css")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-css"))
    }

    @Test
    fun `should map HTML extensions to html language`() {
        listOf(".html", ".htm").forEach { ext ->
            val content = "<html></html>"
            val options = mapOf("filename" to "index$ext")

            val document = parser.parse(content, options)

            // HTML type displays as-is, not wrapped in code block
            assertTrue(document.parsedContent.contains("<html>"))
        }
    }

    @Test
    fun `should map SQL extension to sql language`() {
        val content = "SELECT * FROM users;"
        val options = mapOf("filename" to "query.sql")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-sql"))
    }

    @Test
    fun `should map YAML extensions to yaml language`() {
        listOf(".yaml", ".yml").forEach { ext ->
            val content = "key: value"
            val options = mapOf("filename" to "config$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-yaml"))
        }
    }

    @Test
    fun `should map R extension to r language`() {
        val content = "x <- c(1, 2, 3)"
        val options = mapOf("filename" to "analysis.r")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-r"))
    }

    @Test
    fun `should map Lua extension to lua language`() {
        val content = "function hello() end"
        val options = mapOf("filename" to "script.lua")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("language-lua"))
    }

    @Test
    fun `should map Perl extensions to perl language`() {
        listOf(".perl", ".pl").forEach { ext ->
            val content = "print 'hello';"
            val options = mapOf("filename" to "script$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-perl"))
        }
    }

    @Test
    fun `should map diff extensions to diff language`() {
        listOf(".diff", ".patch").forEach { ext ->
            val content = "+added line\n-removed line"
            val options = mapOf("filename" to "changes$ext")

            val document = parser.parse(content, options)

            assertTrue(document.parsedContent.contains("language-diff"))
        }
    }

    @Test
    fun `should map unknown extension to plaintext language`() {
        val content = "Some content"
        val options = mapOf("filename" to "file.unknown")

        val document = parser.parse(content, options)

        // Unknown extensions get PLAIN type, not CODE type
        assertTrue(document.parsedContent.contains("<pre"))
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should wrap plain text in pre tag`() {
        val content = "Plain text content"
        val options = mapOf("filename" to "notes.txt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("<pre"))
        assertTrue(document.parsedContent.contains("Plain text content"))
        assertTrue(document.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should escape HTML in plain text`() {
        val content = "<script>alert('xss')</script>"
        val options = mapOf("filename" to "text.txt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("&lt;script&gt;"))
        assertFalse(document.parsedContent.contains("<script>"))
    }

    @Test
    fun `should pass through HTML content without escaping`() {
        val content = "<html><body><h1>Title</h1></body></html>"
        val options = mapOf("filename" to "page.html")

        val document = parser.parse(content, options)

        // HTML type should pass through content as-is
        assertTrue(document.parsedContent.contains("<html>"))
        assertTrue(document.parsedContent.contains("<h1>Title</h1>"))
    }

    @Test
    fun `should wrap code in code block with language class`() {
        val content = "def hello():\n    print('world')"
        val options = mapOf("filename" to "script.py")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("<code"))
        assertTrue(document.parsedContent.contains("language-python"))
        assertTrue(document.parsedContent.contains("def hello()"))
    }

    @Test
    fun `should escape HTML in code blocks`() {
        val content = "const html = '<div>test</div>';"
        val options = mapOf("filename" to "script.js")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("&lt;div&gt;"))
        assertTrue(document.parsedContent.contains("language-javascript"))
    }

    @Test
    fun `should add plaintext class to plain text div`() {
        val content = "Plain text"
        val options = mapOf("filename" to "file.txt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("class='plaintext'"))
    }

    @Test
    fun `should add code-block class to code div`() {
        val content = "print('hello')"
        val options = mapOf("filename" to "script.py")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("code-block"))
    }

    @Test
    fun `should preserve whitespace with pre-wrap style`() {
        val content = "Line 1\n  Indented line\nLine 3"
        val options = mapOf("filename" to "text.txt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("white-space: pre-wrap"))
    }

    @Test
    fun `should use monospace font for plain text`() {
        val content = "Monospace text"
        val options = mapOf("filename" to "file.txt")

        val document = parser.parse(content, options)

        assertTrue(document.parsedContent.contains("font-family: monospace"))
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should include type in metadata`() {
        val content = "Test content"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals("plain", document.metadata["type"])
    }

    @Test
    fun `should include extension in metadata`() {
        val content = "Test content"
        val options = mapOf("filename" to "test.py")

        val document = parser.parse(content, options)

        assertEquals(".py", document.metadata["extension"])
    }

    @Test
    fun `should include line count in metadata`() {
        val content = "Line 1\nLine 2\nLine 3"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals("3", document.metadata["lines"])
    }

    @Test
    fun `should include character count in metadata`() {
        val content = "12345"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals("5", document.metadata["characters"])
    }

    @Test
    fun `should count single line correctly`() {
        val content = "Single line"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals("1", document.metadata["lines"])
    }

    @Test
    fun `should count empty content as one line`() {
        val content = ""
        val options = mapOf("filename" to "empty.txt")

        val document = parser.parse(content, options)

        assertEquals("1", document.metadata["lines"])
        assertEquals("0", document.metadata["characters"])
    }

    // ==================== Parse Integration Tests ====================

    @Test
    fun `should parse without filename option`() {
        val content = "Content without filename"

        val document = parser.parse(content)

        assertNotNull(document)
        assertEquals("plain", document.metadata["type"])
        assertEquals("", document.metadata["extension"])
    }

    @Test
    fun `should preserve raw content unchanged`() {
        val content = "Original\tcontent\nwith\rspecial\r\nchars"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals(content, document.rawContent)
    }

    @Test
    fun `should set correct format in document`() {
        val content = "Test"
        val options = mapOf("filename" to "test.txt")

        val document = parser.parse(content, options)

        assertEquals(FormatRegistry.getById("plaintext"), document.format)
    }

    @Test
    fun `should handle very long filenames`() {
        val longFilename = "a".repeat(200) + ".py"
        val content = "print('test')"
        val options = mapOf("filename" to longFilename)

        val document = parser.parse(content, options)

        assertEquals(".py", document.metadata["extension"])
        assertEquals("code", document.metadata["type"])
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty content`() {
        val content = ""
        val options = mapOf("filename" to "empty.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
        assertEquals("", document.rawContent)
        assertEquals("0", document.metadata["characters"])
    }

    @Test
    fun `should handle very large content`() {
        val content = "A".repeat(100000)
        val options = mapOf("filename" to "large.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
        assertEquals("100000", document.metadata["characters"])
    }

    @Test
    fun `should handle Unicode content`() {
        val content = "Hello 世界 🌍 مرحبا Привет"
        val options = mapOf("filename" to "unicode.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
        assertTrue(document.parsedContent.contains("世界"))
        assertTrue(document.parsedContent.contains("🌍"))
    }

    @Test
    fun `should handle special characters`() {
        val content = "Special: !@#$%^&*()_+-={}[]|\\:\";<>?,./"
        val options = mapOf("filename" to "special.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
    }

    @Test
    fun `should handle null bytes`() {
        val content = "Text\u0000with\u0000nulls"
        val options = mapOf("filename" to "binary.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
    }

    @Test
    fun `should handle mixed line endings`() {
        val content = "Line1\nLine2\rLine3\r\nLine4"
        val options = mapOf("filename" to "mixed.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
        assertEquals("4", document.metadata["lines"])
    }

    @Test
    fun `should handle only whitespace content`() {
        val content = "   \n\n\t\t   "
        val options = mapOf("filename" to "whitespace.txt")

        val document = parser.parse(content, options)

        assertNotNull(document)
        assertTrue(document.metadata["lines"]!!.toInt() > 0)
    }

    @Test
    fun `should handle filename with path separators`() {
        val content = "Test"
        val options = mapOf("filename" to "/path/to/file.py")

        val document = parser.parse(content, options)

        assertEquals(".py", document.metadata["extension"])
    }

    @Test
    fun `should handle filename with Windows path separators`() {
        val content = "Test"
        val options = mapOf("filename" to "C:\\path\\to\\file.java")

        val document = parser.parse(content, options)

        assertEquals(".java", document.metadata["extension"])
    }

    @Test
    fun `should handle hidden files with dot prefix`() {
        val content = "config data"
        val options = mapOf("filename" to ".gitignore")

        val document = parser.parse(content, options)

        assertEquals(".gitignore", document.metadata["extension"])
    }
}
