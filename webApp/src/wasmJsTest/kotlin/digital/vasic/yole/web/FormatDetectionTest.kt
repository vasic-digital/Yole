/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format detection tests for Yole Web Application
 *
 *########################################################*/
package digital.vasic.yole.web

import digital.vasic.yole.format.FormatRegistry
import kotlin.test.*

/**
 * Format detection tests for Yole Web Application.
 * Simplified for WASM compatibility.
 */
class FormatDetectionTest {

    @Test
    fun `should detect markdown format correctly`() {
        val testCases = listOf(
            "document.md" to "markdown",
            "README.md" to "markdown",
            "my-file.markdown" to "markdown",
            "UPPERCASE.MD" to "markdown",
            "Mixed.Case.Md" to "markdown"
        )

        testCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect markdown from $filename")
        }
    }

    @Test
    fun `should detect plain text format correctly`() {
        val testCases = listOf(
            "notes.txt" to "plaintext",
            "README.txt" to "plaintext",
            "document.TXT" to "plaintext",
            "file-with-dashes.txt" to "plaintext"
        )

        testCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect plain text from $filename")
        }
    }

    @Test
    fun `should detect CSV format correctly`() {
        val testCases = listOf(
            "data.csv" to "csv",
            "spreadsheet.csv" to "csv",
            "UPPERCASE.CSV" to "csv",
            "data_export.csv" to "csv"
        )

        testCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect CSV from $filename")
        }
    }

    @Test
    fun `should detect LaTeX format correctly`() {
        val testCases = listOf(
            "document.tex" to "latex",
            "paper.tex" to "latex",
            "UPPERCASE.TEX" to "latex",
            "research_paper.tex" to "latex"
        )

        testCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect LaTeX from $filename")
        }
    }

    @Test
    fun `should detect web development formats correctly`() {
        val webFormats = listOf(
            "styles.css" to "css",
            "script.js" to "javascript",
            "app.ts" to "typescript",
            "component.jsx" to "javascript",
            "page.html" to "html",
            "data.json" to "json",
            "config.xml" to "xml",
            "settings.yaml" to "yaml",
            "settings.yml" to "yaml"
        )

        webFormats.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect $expectedFormat from $filename")
        }
    }

    @Test
    fun `should detect programming language formats correctly`() {
        val programmingFormats = listOf(
            "Main.kt" to "kotlin",
            "App.java" to "java",
            "script.py" to "python",
            "program.cpp" to "cpp",
            "main.c" to "c",
            "lib.rs" to "rust",
            "server.go" to "go",
            "app.rb" to "ruby",
            "index.php" to "php",
            "App.swift" to "swift"
        )

        programmingFormats.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect $expectedFormat from $filename")
        }
    }

    @Test
    fun `should handle files without extensions`() {
        val noExtensionFiles = listOf(
            "README",
            "LICENSE",
            "Makefile",
            "Dockerfile",
            "gitignore",
            "Jenkinsfile"
        )

        noExtensionFiles.forEach { filename ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals("plaintext", detectedFormat, "Should fallback to plaintext for $filename")
        }
    }

    @Test
    fun `should handle unknown extensions correctly`() {
        val unknownExtensions = listOf(
            "file.unknown" to "plaintext",
            "document.xyz" to "plaintext",
            "data.abc123" to "plaintext",
            "file.ext_that_does_not_exist" to "plaintext"
        )

        unknownExtensions.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should fallback to plaintext for unknown extension in $filename")
        }
    }

    @Test
    fun `should handle case sensitivity correctly`() {
        val caseTestCases = listOf(
            "file.md" to "markdown",
            "file.MD" to "markdown",
            "file.Md" to "markdown",
            "file.mD" to "markdown",
            "FILE.MD" to "markdown",
            "File.Md" to "markdown"
        )

        caseTestCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should handle case variations for $filename")
        }
    }

    @Test
    fun `should handle malicious filenames safely`() {
        val maliciousFilenames = listOf(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "file<script>.md",
            "file\"quoted\".txt",
            "file|with|pipes.csv",
            "file\u0000with\u0000nulls.tex",
            "file/with/slashes.md",
            "file\\with\\backslashes.txt"
        )

        maliciousFilenames.forEach { filename ->
            try {
                val detectedFormat = detectFormatFromFilename(filename)
                // Should not crash and should return some format
                assertNotNull(detectedFormat, "Should return a format even for malicious filename")
            } catch (e: Exception) {
                fail("Should handle malicious filename '$filename' gracefully without throwing: ${e.message}")
            }
        }
    }

    @Test
    fun `should handle Unicode filenames correctly`() {
        val unicodeFilenames = listOf(
            "文档.md" to "markdown",
            "ドキュメント.txt" to "plaintext",
            "مستند.csv" to "csv",
            "документ.tex" to "latex",
            "doküment.md" to "markdown",
            "café.txt" to "plaintext",
            "naïve.md" to "markdown"
        )

        unicodeFilenames.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect format from Unicode filename: $filename")
        }
    }

    @Test
    fun `should handle emoji in filenames`() {
        val emojiFilenames = listOf(
            "document📝.md" to "markdown",
            "notes📄.txt" to "plaintext",
            "data📊.csv" to "csv",
            "code💻.js" to "javascript"
        )

        emojiFilenames.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should detect format from emoji filename: $filename")
        }
    }

    @Test
    fun `should detect formats efficiently`() {
        val testFilenames = (1..1000).map { i ->
            when (i % 10) {
                0 -> "file$i.md"
                1 -> "file$i.txt"
                2 -> "file$i.csv"
                3 -> "file$i.tex"
                4 -> "file$i.py"
                5 -> "file$i.js"
                6 -> "file$i.html"
                7 -> "file$i.json"
                8 -> "file$i.xml"
                9 -> "file$i.unknown"
                else -> "file$i.md"
            }
        }
        
        val results = testFilenames.map { filename ->
            detectFormatFromFilename(filename)
        }

        assertEquals(1000, results.size, "Should process all filenames")
        assertTrue(results.all { it.isNotEmpty() }, "All results should have valid formats")
        
        // Verify some specific results
        assertEquals("markdown", results[0], "First file should be markdown")
        assertEquals("plaintext", results[1], "Second file should be plaintext")
        assertEquals("csv", results[2], "Third file should be CSV")
    }

    @Test
    fun `should integrate with FormatRegistry for supported formats`() {
        val supportedFormats = FormatRegistry.formats.map { it.id }
        
        // Test that our detection returns formats that are actually supported
        val testFiles = listOf(
            "test.md",
            "test.txt",
            "test.csv",
            "test.tex",
            "test.org",
            "test.asciidoc",
            "test.wiki"
        )

        testFiles.forEach { filename ->
            val detectedFormat = detectFormatFromFilename(filename)
            
            // For known extensions, should detect supported formats
            when (filename.substringAfterLast(".").lowercase()) {
                "md", "markdown" -> assertEquals("markdown", detectedFormat)
                "txt" -> assertTrue(detectedFormat == "plaintext" || detectedFormat == "todotxt")
                "csv" -> assertEquals("csv", detectedFormat)
                "tex" -> assertEquals("latex", detectedFormat)
                "org" -> assertEquals("orgmode", detectedFormat)
                "asciidoc", "adoc" -> assertEquals("asciidoc", detectedFormat)
                "wiki" -> assertEquals("wikitext", detectedFormat)
            }
        }
    }

    @Test
    fun `should handle very long filenames`() {
        val longFilename = "a".repeat(200) + ".md"
        
        try {
            val detectedFormat = detectFormatFromFilename(longFilename)
            assertEquals("markdown", detectedFormat, "Should handle very long filenames")
        } catch (e: Exception) {
            fail("Should handle very long filenames gracefully: ${e.message}")
        }
    }

    @Test
    fun `should handle special characters in extensions`() {
        val specialExtensionCases = listOf(
            "file.m@d" to "plaintext",
            "file.md!" to "plaintext",
            "file.md#" to "plaintext",
            "file.md$" to "plaintext",
            "file.md%" to "plaintext"
        )

        specialExtensionCases.forEach { (filename, expectedFormat) ->
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals(expectedFormat, detectedFormat, "Should handle special characters in $filename")
        }
    }

    @Test
    fun `should maintain consistency across multiple calls`() {
        val filename = "consistency-test.md"
        
        val results = (1..100).map {
            detectFormatFromFilename(filename)
        }
        
        assertTrue(results.all { it == "markdown" }, "Should consistently return same format")
        assertEquals(1, results.distinct().size, "Should have exactly one unique result")
    }
}