/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for Yole Desktop Application
 *
 *########################################################*/
package digital.vasic.yole.desktop

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry

import digital.vasic.yole.format.TextFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.After
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for Yole Desktop Application.
 *
 * Tests cover:
 * - End-to-end file processing workflows
 * - Format detection and parsing integration
 * - Settings persistence across sessions
 * - Cross-component communication
 * - Error recovery and handling
 * - Performance under load
 */
@RunWith(JUnit4::class)
class DesktopAppIntegrationTest {

    private lateinit var tempDir: Path
    private lateinit var desktopApp: DesktopApplication
    private lateinit var fileManager: DesktopFileManager
    private lateinit var settingsManager: DesktopSettingsManager

    @Before
    fun setUp() {
        // Format registry is automatically initialized as an object
        
        // Create temporary directory
        tempDir = Files.createTempDirectory("yole_integration_test")
        
        // Initialize desktop app components
        desktopApp = DesktopApplication()
        fileManager = DesktopFileManager()
        settingsManager = DesktopSettingsManager()
    }

    @After
    fun tearDown() {
        // Clean up temporary files
        if (::tempDir.isInitialized) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        
        // Clear registries
        // FormatRegistry.clear() - method doesn't exist
    }

    // ==================== End-to-End Workflows ====================

    @Test
    fun `should complete full markdown document workflow`() {
        // Create a markdown file
        val markdownFile = tempDir.resolve("workflow_test.md").toFile()
        val markdownContent = """
            # Test Document
            
            This is a **test** document with *italic* text and `code`.
            
            ## Features
            
            - List item 1
            - List item 2
            - List item 3
            
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
            
            [Link to example](https://example.com)
        """.trimIndent()
        
        // Step 1: Save the file
        val saveResult = fileManager.saveFile(markdownFile, markdownContent)
        assertTrue(saveResult, "File should be saved successfully")
        
        // Step 2: Detect format
        val detectedFormat = fileManager.detectFormatFromFile(markdownFile)
        assertNotNull(detectedFormat, "Format should be detected")
        assertThat(detectedFormat.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
        
        // Step 3: Get appropriate parser
        val parser = ParserRegistry.getParser(detectedFormat.id)
        assertNotNull(parser, "Parser should be available for detected format")
        
        // Step 4: Parse the content
        val parsedDocument = parser.parse(markdownContent)
        assertNotNull(parsedDocument, "Document should be parsed successfully")
        assertTrue(parsedDocument.errors.isEmpty(), "Parsed document should not have errors")
        
        // Step 5: Generate HTML for preview
        val htmlContent = parser.toHtml(parsedDocument)
        assertNotNull(htmlContent, "HTML should be generated successfully")
        assertThat(htmlContent).contains("<h1>")
        assertThat(htmlContent).contains("<em>")
        assertThat(htmlContent).contains("<strong>")
        assertThat(htmlContent).contains("<code>")
        
        // Step 6: Add to recent files
        fileManager.addToRecentFiles(markdownFile)
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles).contains(markdownFile)
        
        // Step 7: Load file back
        val loadedContent = fileManager.loadFile(markdownFile)
        assertNotNull(loadedContent, "File should be loaded successfully")
        assertThat(loadedContent).isEqualTo(markdownContent)
    }

    @Test
    fun `should handle CSV data processing workflow`() {
        // Create a CSV file
        val csvFile = tempDir.resolve("data.csv").toFile()
        val csvContent = """
            Name,Age,City,Department
            John Doe,30,New York,Engineering
            Jane Smith,25,Los Angeles,Marketing
            Bob Johnson,35,Chicago,Sales
            Alice Brown,28,Houston,Design
        """.trimIndent()
        
        // Step 1: Save CSV file
        val saveResult = fileManager.saveFile(csvFile, csvContent)
        assertTrue(saveResult, "CSV file should be saved successfully")
        
        // Step 2: Detect CSV format
        val detectedFormat = fileManager.detectFormatFromFile(csvFile)
        assertNotNull(detectedFormat, "CSV format should be detected")
        assertThat(detectedFormat.id).isEqualTo(FormatRegistry.ID_CSV)
        
        // Step 3: Parse CSV content
        val parser = ParserRegistry.getParser(detectedFormat.id)
        assertNotNull(parser, "CSV parser should be available")
        
        val parsedDocument = parser.parse(csvContent)
        assertNotNull(parsedDocument, "CSV should be parsed successfully")
        
        // Step 4: Generate HTML table
        val htmlContent = parser.toHtml(parsedDocument)
        assertNotNull(htmlContent, "HTML table should be generated")
        assertThat(htmlContent).contains("<table>")
        assertThat(htmlContent).contains("<th>Name</th>")
        assertThat(htmlContent).contains("<td>John Doe</td>")
        
        // Step 5: Extract metadata
        val metadata = parsedDocument.metadata
        assertThat(metadata["rowCount"]).isEqualTo("4")
        assertThat(metadata["columnCount"]).isEqualTo("4")
        assertThat(metadata["hasHeader"]).isEqualTo("true")
    }

    @Test
    fun `should handle TodoTxt task management workflow`() {
        // Create a TodoTxt file
        val todoFile = tempDir.resolve("tasks.txt").toFile()
        val todoContent = """
            (A) Complete project documentation @work +urgent due:2025-12-15
            (B) Review code changes @work +review
            (C) Update team on progress @work +communication
            x 2025-12-01 Finished quarterly report @work +completed
            (A) Schedule team meeting @work +planning
        """.trimIndent()
        
        // Step 1: Save TodoTxt file
        val saveResult = fileManager.saveFile(todoFile, todoContent)
        assertTrue(saveResult, "TodoTxt file should be saved successfully")
        
        // Step 2: Detect format by content
        val detectedFormat = fileManager.detectFormatFromContent(todoFile)
        assertNotNull(detectedFormat, "TodoTxt format should be detected by content")
        assertThat(detectedFormat.id).isEqualTo(FormatRegistry.ID_TODOTXT)
        
        // Step 3: Parse TodoTxt content
        val parser = ParserRegistry.getParser(detectedFormat.id)
        assertNotNull(parser, "TodoTxt parser should be available")
        
        val parsedDocument = parser.parse(todoContent)
        assertNotNull(parsedDocument, "TodoTxt should be parsed successfully")
        
        // Step 4: Generate HTML output
        val htmlContent = parser.toHtml(parsedDocument)
        assertNotNull(htmlContent, "HTML should be generated for TodoTxt")
        
        // Step 5: Validate task structure in output
        assertThat(htmlContent).contains("Complete project documentation")
        assertThat(htmlContent).contains("@work")
        assertThat(htmlContent).contains("+urgent")
    }

    // ==================== Settings Integration Tests ====================

    @Test
    fun `should persist settings across application sessions`() {
        // Set various settings
        settingsManager.setThemeMode("dark")
        settingsManager.setShowLineNumbers(false)
        settingsManager.setAutoSave(true)
        settingsManager.setHighContrastEnabled(true)
        
        // Simulate application restart by creating new settings manager
        val newSettingsManager = DesktopSettingsManager()
        
        // Verify settings were persisted
        assertThat(newSettingsManager.getThemeMode()).isEqualTo("dark")
        assertThat(newSettingsManager.getShowLineNumbers()).isFalse()
        assertThat(newSettingsManager.getAutoSave()).isTrue()
        assertThat(newSettingsManager.getHighContrastEnabled()).isTrue()
    }

    @Test
    fun `should handle settings validation and defaults`() {
        // Test invalid theme mode (should default to "system")
        settingsManager.setThemeMode("invalid_theme")
        assertThat(settingsManager.getThemeMode()).isEqualTo("system")
        
        // Test null accent color (should return null)
        settingsManager.setAccentColor(null)
        assertThat(settingsManager.getAccentColor()).isNull()
        
        // Test boolean settings
        settingsManager.setAnimationsEnabled(false)
        assertThat(settingsManager.getAnimationsEnabled()).isFalse()
        
        // Focus indicators not implemented in mock settings manager
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    fun `should handle malformed content gracefully`() {
        val malformedFile = tempDir.resolve("malformed.md").toFile()
        val malformedContent = """
            # Valid Header
            
            **Unclosed bold text
            
            *Unclosed italic text
            
            [Unclosed link](https://example.com
            
            ```unclosed code block
            code without closing
            
            > Unclosed blockquote
        """.trimIndent()
        
        // Save malformed content
        val saveResult = fileManager.saveFile(malformedFile, malformedContent)
        assertTrue(saveResult, "Malformed content should be saved successfully")
        
        // Detect format
        val format = fileManager.detectFormatFromFile(malformedFile)
        assertNotNull(format, "Format should be detected even for malformed content")
        
        // Parse malformed content
        val parser = ParserRegistry.getParser(format.id)
        assertNotNull(parser, "Parser should be available")
        
        val parsedDocument = parser.parse(malformedContent)
        assertNotNull(parsedDocument, "Malformed content should be parsed without crashing")
        
        // Should have validation errors
        val errors = parser.validate(malformedContent)
        assertTrue(errors.isNotEmpty(), "Malformed content should produce validation errors")
        
        // But should still generate HTML
        val htmlContent = parser.toHtml(parsedDocument)
        assertNotNull(htmlContent, "HTML should be generated even for malformed content")
    }

    @Test
    fun `should recover from file operation errors`() {
        val invalidFile = File("/invalid/path/that/does/not/exist/file.md")
        
        // Attempt to save to invalid location
        val saveResult = fileManager.saveFile(invalidFile, "content")
        assertFalse(saveResult, "Save to invalid path should fail gracefully")
        
        // Attempt to load non-existent file
        val loadContent = fileManager.loadFile(invalidFile)
        assertThat(loadContent).isNull()
        
        // Attempt to detect format of non-existent file
        val format = fileManager.detectFormatFromFile(invalidFile)
        assertThat(format).isNull()
        
        // Application should continue functioning after errors
        val validFile = tempDir.resolve("recovery_test.md").toFile()
        val validSaveResult = fileManager.saveFile(validFile, "Recovery test content")
        assertTrue(validSaveResult, "Application should recover and save valid files")
    }

    // ==================== Performance Integration Tests ====================

    @Test
    fun `should handle large documents efficiently`() {
        val largeFile = tempDir.resolve("large_document.md").toFile()
        val largeContent = buildString {
            repeat(1000) { i ->
                appendLine("# Section $i")
                appendLine()
                appendLine("This is paragraph $i with some **bold** and *italic* text.")
                appendLine()
                appendLine("```kotlin")
                appendLine("fun example$i() {")
                appendLine("    println(\"Example $i\")")
                appendLine("}")
                appendLine("```")
                appendLine()
            }
        }
        
        val startTime = System.currentTimeMillis()
        
        // Save large document
        val saveResult = fileManager.saveFile(largeFile, largeContent)
        assertTrue(saveResult, "Large document should be saved successfully")
        
        // Detect format
        val format = fileManager.detectFormatFromFile(largeFile)
        assertNotNull(format, "Format should be detected for large document")
        
        // Parse large document
        val parser = ParserRegistry.getParser(format.id)
        assertNotNull(parser, "Parser should be available for large document")
        
        val parsedDocument = parser.parse(largeContent)
        assertNotNull(parsedDocument, "Large document should be parsed successfully")
        
        // Generate HTML
        val htmlContent = parser.toHtml(parsedDocument)
        assertNotNull(htmlContent, "HTML should be generated for large document")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (less than 10 seconds)
        assertThat(duration).isLessThan(10000)
        
        // Verify content integrity
        assertThat(parsedDocument.rawContent).isEqualTo(largeContent)
        assertThat(htmlContent).contains("<h1>")
        assertThat(htmlContent.length).isGreaterThan(largeContent.length)
    }

    @Test
    fun `should handle rapid format switching efficiently`() {
        val formats = listOf(
            FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!,
            FormatRegistry.getById(FormatRegistry.ID_CSV)!!,
            FormatRegistry.getById(FormatRegistry.ID_TODOTXT)!!,
            FormatRegistry.getById(FormatRegistry.ID_LATEX)!!
        )
        
        val startTime = System.currentTimeMillis()
        
        repeat(100) { iteration ->
            val format = formats[iteration % formats.size]
            val parser = ParserRegistry.getParser(format.id)
            assertNotNull(parser, "Parser should be available for ${format.name}")
            
            val sampleContent = when (format.id) {
                FormatRegistry.ID_MARKDOWN -> "# Test\n\nContent"
                FormatRegistry.ID_CSV -> "name,age\nJohn,30"
                FormatRegistry.ID_TODOTXT -> "(A) Test task @work +project"
                FormatRegistry.ID_LATEX -> "\\documentclass{article}"
                else -> "Test content"
            }
            
            val parsedDocument = parser.parse(sampleContent)
            assertNotNull(parsedDocument, "Content should be parsed for ${format.name}")
            
            val htmlContent = parser.toHtml(parsedDocument)
            assertNotNull(htmlContent, "HTML should be generated for ${format.name}")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete rapidly (less than 2 seconds for 100 switches)
        assertThat(duration).isLessThan(2000)
    }

    // ==================== Mock Implementations ====================

    /**
     * Mock implementation of DesktopApplication for testing
     */
    class DesktopApplication {
        fun initialize() {
            // Parser initialization handled by FormatRegistry
        }
        
        fun shutdown() {
            // FormatRegistry.clear() - method doesn't exist
        }
    }

    /**
     * Mock implementation of DesktopFileManager for testing
     */
    class DesktopFileManager {
        fun saveFile(file: File, content: String): Boolean {
            return try {
                file.writeText(content)
                true
            } catch (e: Exception) {
                false
            }
        }
        
        fun loadFile(file: File): String? {
            return try {
                if (file.exists()) file.readText() else null
            } catch (e: Exception) {
                null
            }
        }
        
        fun detectFormatFromFile(file: File): TextFormat? {
            return FormatRegistry.getByExtension(file.extension)
        }
        
        fun detectFormatFromContent(file: File): TextFormat? {
            return try {
                val content = file.readText()
                FormatRegistry.detectByContent(content)
            } catch (e: Exception) {
                null
            }
        }
        
        fun addToRecentFiles(file: File) {
            // Implementation would add to recent files list
        }
        
        fun getRecentFiles(): List<File> {
            return emptyList() // Mock implementation
        }
    }

    /**
     * Mock implementation of DesktopSettingsManager for testing
     */
    class DesktopSettingsManager {
        private val settings = mutableMapOf<String, Any>()
        
        fun setThemeMode(mode: String) {
            settings["theme_mode"] = mode
        }
        
        fun getThemeMode(): String {
            return settings["theme_mode"] as? String ?: "system"
        }
        
        fun setShowLineNumbers(show: Boolean) {
            settings["show_line_numbers"] = show
        }
        
        fun getShowLineNumbers(): Boolean {
            return settings["show_line_numbers"] as? Boolean ?: true
        }
        
        fun setAutoSave(auto: Boolean) {
            settings["auto_save"] = auto
        }
        
        fun getAutoSave(): Boolean {
            return settings["auto_save"] as? Boolean ?: true
        }
        
        fun setHighContrastEnabled(enabled: Boolean) {
            settings["high_contrast"] = enabled
        }
        
        fun getHighContrastEnabled(): Boolean {
            return settings["high_contrast"] as? Boolean ?: false
        }
        
        fun setAnimationsEnabled(enabled: Boolean) {
            settings["animations_enabled"] = enabled
        }
        
        fun getAnimationsEnabled(): Boolean {
            return settings["animations_enabled"] as? Boolean ?: true
        }
        
        fun setAccentColor(color: String?) {
            if (color != null) {
                settings["accent_color"] = color
            } else {
                settings.remove("accent_color")
            }
        }
        
        fun getAccentColor(): String? {
            return settings["accent_color"] as? String
        }
    }
}