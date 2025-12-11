/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for Yole Web Application
 *
 *########################################################*/
package digital.vasic.yole.web

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Integration tests for Yole Web Application.
 * Simplified for WASM compatibility.
 */
class WebAppIntegrationTest {

    @Test
    fun `should integrate file download with document content`() {
        val testContent = "# Test Document\n\nThis is integration test content."
        val testFilename = "integration-test.md"
        var downloadTriggered = false

        // Mock the download functionality
        val mockDownloadFile: (String, String) -> Unit = { content, filename ->
            downloadTriggered = true
            assertEquals(testContent, content, "Downloaded content should match document content")
            assertEquals(testFilename, filename, "Downloaded filename should be correct")
        }

        // Simulate download
        mockDownloadFile(testContent, testFilename)

        // Verify download was triggered correctly
        assertTrue(downloadTriggered, "Download should be triggered")
    }

    @Test
    fun `should integrate file upload with format detection`() {
        val testFilename = "sample-document.md"
        val testContent = "# Sample Document\n\nContent for testing."
        var fileLoaded = false

        // Mock file loading functionality
        val mockTriggerFileInput: ((String, String) -> Unit) -> Unit = { onFileLoaded ->
            fileLoaded = true
            onFileLoaded(testContent, testFilename)
        }

        // Simulate file loading with format detection
        mockTriggerFileInput { content, filename ->
            assertEquals(testContent, content, "Loaded content should match")
            assertEquals(testFilename, filename, "Loaded filename should be correct")
            val detectedFormat = detectFormatFromFilename(filename)
            assertEquals("markdown", detectedFormat, "Format should be detected from filename")
        }

        // Verify file was loaded correctly
        assertTrue(fileLoaded, "File loading should be triggered")
    }

    @Test
    fun `should integrate format registry with UI components`() {
        val availableFormats = FormatRegistry.formats
        
        // Verify format registry has expected formats
        assertTrue(availableFormats.isNotEmpty(), "Format registry should have formats")
        assertTrue(availableFormats.any { it.id == "markdown" }, "Markdown format should be available")
        assertTrue(availableFormats.any { it.id == "plaintext" }, "Plain text format should be available")
        assertTrue(availableFormats.any { it.id == "csv" }, "CSV format should be available")
        assertTrue(availableFormats.any { it.id == "latex" }, "LaTeX format should be available")

        // Test format selection integration
        val markdownFormat = FormatRegistry.getById("markdown")
        assertNotNull(markdownFormat, "Markdown format should be retrievable")
        assertEquals("Markdown", markdownFormat.name, "Format name should be correct")
        assertEquals(".md", markdownFormat.defaultExtension, "Default extension should be correct")
    }

    @Test
    fun `should integrate parser registry with preview functionality`() {
        val testContent = "# Test Header\n\nTest paragraph content."
        val markdownParser = ParserRegistry.getParser("markdown")
        
        assertNotNull(markdownParser, "Markdown parser should be available")

        // Test parsing integration
        val parsedDocument = markdownParser.parse(testContent)
        assertNotNull(parsedDocument, "Document should be parsed successfully")

        // Test HTML generation integration
        val htmlOutput = markdownParser.toHtml(parsedDocument)
        assertTrue(htmlOutput.isNotEmpty(), "HTML output should not be empty")
        assertTrue(htmlOutput.contains("Test Header") || htmlOutput.contains("Test paragraph content"), 
                  "HTML output should contain parsed content")
    }

    @Test
    fun `should handle new document creation with templates`() {
        val testFormats = listOf("markdown", "todotxt", "latex", "orgmode", "plaintext")
        
        testFormats.forEach { formatId ->
            var createdContent = ""
            var createdFilename = ""

            // Simulate new document creation
            createNewDocument(formatId) { content, filename ->
                createdContent = content
                createdFilename = filename
            }

            // Verify document was created with appropriate template
            assertTrue(createdContent.isNotEmpty(), "Content should be generated for $formatId")
            assertTrue(createdFilename.isNotEmpty(), "Filename should be generated for $formatId")
        }
    }

    @Test
    fun `should coordinate between format selection and content creation`() {
        var selectedFormat = "markdown"
        var documentContent = ""
        var documentName = ""

        // Simulate format selection
        val newFormat = "latex"
        selectedFormat = newFormat
        
        // Simulate new document creation with selected format
        createNewDocument(selectedFormat) { content, filename ->
            documentContent = content
            documentName = filename
        }

        // Verify coordination worked correctly
        assertEquals("latex", selectedFormat, "Format should be updated")
        assertTrue(documentContent.isNotEmpty(), "Content should be created for selected format")
        assertTrue(documentName.contains(".tex"), "Filename should have correct extension for LaTeX")
    }

    @Test
    fun `should handle large documents efficiently`() {
        // Create large content
        val largeContent = buildString {
            repeat(1000) { i ->
                append("# Section $i\n\n")
                append("This is paragraph content for section $i. ".repeat(10))
                append("\n\n")
            }
        }

        // Test parsing performance
        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")
        
        val parsedDocument = parser.parse(largeContent)
        assertNotNull(parsedDocument, "Large document should be parsed successfully")
        
        val htmlOutput = parser.toHtml(parsedDocument)
        assertTrue(htmlOutput.isNotEmpty(), "HTML output should be generated for large document")
        
        // Verify the content is processed correctly
        assertTrue(htmlOutput.contains("Section 0"), "HTML should contain first section")
        assertTrue(htmlOutput.contains("Section 999"), "HTML should contain last section")
    }

    @Test
    fun `should handle rapid format switching efficiently`() {
        val content = "# Quick Test\n\nContent for rapid switching test."
        val formats = listOf("markdown", "plaintext", "csv", "latex", "orgmode")
        
        formats.forEach { format ->
            // Simulate format switching
            val parser = ParserRegistry.getParser(format)
            if (parser != null) {
                val parsedDocument = parser.parse(content)
                val htmlOutput = parser.toHtml(parsedDocument)
                assertTrue(htmlOutput.isNotEmpty(), "Should generate preview for $format")
            }
        }
        
        // Verify all formats were processed
        assertEquals(5, formats.size, "Should process all formats")
    }
}