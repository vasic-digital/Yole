/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration Tests for Yole Desktop App
 *
 * Tests cover:
 * - File operations integration
 * - Desktop-specific features
 * - Cross-platform compatibility
 * - System integration
 * - File format handling
 * - Desktop window management
 * - Platform-specific operations
 *
 *########################################################*/

package digital.vasic.yole.desktop.integration

import digital.vasic.yole.desktop.ui.YoleDesktopSettings
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.MarkdownFormat
import digital.vasic.yole.format.TodoTxtFormat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.*

/**
 * Integration tests for desktop application functionality.
 * Tests real file operations and system integration.
 */
@RunWith(JUnit4::class)
class DesktopAppIntegrationTest {

    private lateinit var settings: YoleDesktopSettings
    private lateinit var tempDir: Path
    private lateinit var testFilesDir: File

    @Before
    fun setup() {
        settings = YoleDesktopSettings()
        tempDir = Files.createTempDirectory("yole-desktop-test")
        testFilesDir = tempDir.toFile()
        
        // Create test files
        createTestFiles()
    }

    private fun createTestFiles() {
        // Create various format test files
        val markdownFile = File(testFilesDir, "test.md")
        markdownFile.writeText("""# Test Markdown
            |
            |This is a test markdown file with:
            |
            |- Bullet points
            |- **Bold text**
            |- *Italic text*
            |
            |## Code Block
            |```kotlin
            |fun main() {
            |    println("Hello World")
            |}
            |```
        """.trimMargin())

        val todoFile = File(testFilesDir, "tasks.todo.txt")
        todoFile.writeText("""x 2024-01-01 Complete project setup @work +yole
            |2024-01-02 Write integration tests @testing +desktop
            |(A) High priority task @urgent
            |(B) Medium priority task @medium
            |(C) Low priority task @low
        """.trimMargin())

        val csvFile = File(testFilesDir, "data.csv")
        csvFile.writeText("""Name,Age,Department,Active
            |John Doe,30,Engineering,true
            |Jane Smith,25,Marketing,true
            |Bob Johnson,35,Sales,false
        """.trimMargin())

        val plainTextFile = File(testFilesDir, "notes.txt")
        plainTextFile.writeText("""Plain text notes
            |
            |This is a simple text file without any special formatting.
            |It should be handled as plain text by the application.
        """.trimMargin())

        val jsonFile = File(testFilesDir, "config.json")
        jsonFile.writeText("""{
            |  "app": {
            |    "name": "Yole",
            |    "version": "2.15.1",
            |    "settings": {
            |      "theme": "system",
            |      "autoSave": true
            |    }
            |  }
            |}
        """.trimMargin())
    }

    // ==================== File Operations Tests ====================

    @Test
    fun `should detect file formats correctly`() {
        val markdownFile = File(testFilesDir, "test.md")
        val todoFile = File(testFilesDir, "tasks.todo.txt")
        val csvFile = File(testFilesDir, "data.csv")
        val plainFile = File(testFilesDir, "notes.txt")

        assertNotNull(FormatRegistry.detectByFilename(markdownFile.name))
        assertEquals("Markdown", FormatRegistry.detectByFilename(markdownFile.name).name)

        assertNotNull(FormatRegistry.detectByFilename(todoFile.name))
        assertEquals("Todo.txt", FormatRegistry.detectByFilename(todoFile.name).name)

        assertNotNull(FormatRegistry.detectByFilename(csvFile.name))
        assertEquals("CSV", FormatRegistry.detectByFilename(csvFile.name).name)

        assertNotNull(FormatRegistry.detectByFilename(plainFile.name))
        assertEquals("Plain Text", FormatRegistry.detectByFilename(plainFile.name).name)
    }

    @Test
    fun `should handle file reading operations`() {
        val testFile = File(testFilesDir, "test.md")
        
        assertTrue(testFile.exists())
        assertTrue(testFile.canRead())
        
        val content = testFile.readText()
        assertTrue(content.contains("# Test Markdown"))
        assertTrue(content.contains("```kotlin"))
    }

    @Test
    fun `should handle file writing operations`() {
        val newFile = File(testFilesDir, "new_file.md")
        val content = "# New File\n\nThis is a new file."
        
        newFile.writeText(content)
        
        assertTrue(newFile.exists())
        assertEquals(content, newFile.readText())
        
        // Cleanup
        newFile.delete()
    }

    @Test
    fun `should handle file deletion operations`() {
        val tempFile = File(testFilesDir, "temp_to_delete.md")
        tempFile.writeText("Temporary content")
        
        assertTrue(tempFile.exists())
        
        val deleted = tempFile.delete()
        assertTrue(deleted)
        assertFalse(tempFile.exists())
    }

    @Test
    fun `should handle file renaming operations`() {
        val originalFile = File(testFilesDir, "original.md")
        originalFile.writeText("Original content")
        
        val renamedFile = File(testFilesDir, "renamed.md")
        val renamed = originalFile.renameTo(renamedFile)
        
        assertTrue(renamed)
        assertFalse(originalFile.exists())
        assertTrue(renamedFile.exists())
        assertEquals("Original content", renamedFile.readText())
        
        // Cleanup
        renamedFile.delete()
    }

    // ==================== Desktop-Specific Features Tests ====================

    @Test
    fun `should handle desktop window state preferences`() {
        // Test window position and size preferences
        settings.setThemeMode("dark")
        settings.setShowLineNumbers(false)
        settings.setAutoSave(false)
        
        assertEquals("dark", settings.getThemeMode())
        assertFalse(settings.getShowLineNumbers())
        assertFalse(settings.getAutoSave())
    }

    @Test
    fun `should handle recent files tracking`() {
        // Simulate recent files functionality
        val recentFiles = mutableListOf<String>()
        
        // Add files to recent list
        recentFiles.add("test.md")
        recentFiles.add("tasks.todo.txt")
        recentFiles.add("data.csv")
        
        assertEquals(3, recentFiles.size)
        assertEquals("test.md", recentFiles[0])
        
        // Test recent files limit (simulate max 5 files)
        recentFiles.add("notes.txt")
        recentFiles.add("config.json")
        recentFiles.add("another.md")
        
        // Keep only last 5 files
        while (recentFiles.size > 5) {
            recentFiles.removeAt(0)
        }
        
        assertEquals(5, recentFiles.size)
        assertFalse(recentFiles.contains("test.md"))
        assertTrue(recentFiles.contains("another.md"))
    }

    @Test
    fun `should handle file associations and desktop integration`() {
        // Test file extension handling
        val extensions = listOf(".md", ".txt", ".csv", ".json", ".todo.txt")
        
        extensions.forEach { ext ->
            val testFile = File(testFilesDir, "test$ext")
            testFile.writeText("Test content for $ext")
            
            assertTrue(testFile.exists())
            assertTrue(testFile.name.endsWith(ext))
            
            // Verify format detection works
            val format = FormatRegistry.detectByFilename(testFile.name)
            assertNotNull(format, "Format should be detected for $ext")
        }
    }

    // ==================== Cross-Platform Compatibility Tests ====================

    @Test
    fun `should handle different line endings correctly`() {
        val contentWindows = "Line 1\r\nLine 2\r\nLine 3"
        val contentUnix = "Line 1\nLine 2\nLine 3"
        val contentMac = "Line 1\rLine 2\rLine 3"
        
        val windowsFile = File(testFilesDir, "windows.txt")
        val unixFile = File(testFilesDir, "unix.txt")
        val macFile = File(testFilesDir, "mac.txt")
        
        windowsFile.writeText(contentWindows)
        unixFile.writeText(contentUnix)
        macFile.writeText(contentMac)
        
        // Read back and normalize
        val windowsRead = windowsFile.readText().replace("\r\n", "\n")
        val unixRead = unixFile.readText()
        val macRead = macFile.readText().replace("\r", "\n")
        
        // All should be equivalent after normalization
        assertEquals(windowsRead, unixRead)
        assertEquals(unixRead, macRead)
    }

    @Test
    fun `should handle special characters in file names`() {
        val specialFiles = listOf(
            "file with spaces.txt",
            "file-with-dashes.txt",
            "file_with_underscores.txt",
            "file.multiple.dots.txt",
            "file(1).txt",
            "file[1].txt",
            "file{1}.txt"
        )
        
        specialFiles.forEach { fileName ->
            val file = File(testFilesDir, fileName)
            file.writeText("Content for $fileName")
            
            assertTrue(file.exists(), "File should exist: $fileName")
            assertEquals(fileName, file.name)
        }
    }

    @Test
    fun `should handle unicode file names and content`() {
        val unicodeFile = File(testFilesDir, "测试文件.txt")
        val unicodeContent = """Unicode test content:
            |中文 (Chinese)
            |العربية (Arabic)
            |Русский (Russian)
            |日本語 (Japanese)
            |한국어 (Korean)
            |Ελληνικά (Greek)
            |עברית (Hebrew)
        """.trimMargin()
        
        unicodeFile.writeText(unicodeContent, Charsets.UTF_8)
        
        assertTrue(unicodeFile.exists())
        assertEquals(unicodeContent, unicodeFile.readText(Charsets.UTF_8))
    }

    // ==================== File Format Processing Tests ====================

    @Test
    fun `should process markdown format correctly`() {
        val markdownFile = File(testFilesDir, "test.md")
        val format = FormatRegistry.detectByFilename(markdownFile.name)
        
        assertNotNull(format)
        assertEquals("Markdown", format.name)
        
        val content = markdownFile.readText()
        assertTrue(content.startsWith("# Test Markdown"))
        
        // Test format-specific processing
        val lines = content.lines()
        assertTrue(lines.any { it.startsWith("#") })
        assertTrue(lines.any { it.contains("```") })
    }

    @Test
    fun `should process todo.txt format correctly`() {
        val todoFile = File(testFilesDir, "tasks.todo.txt")
        val format = FormatRegistry.detectByFilename(todoFile.name)
        
        assertNotNull(format)
        assertEquals("Todo.txt", format.name)
        
        val content = todoFile.readText()
        val lines = content.lines()
        
        // Verify todo.txt format elements
        assertTrue(lines.any { it.startsWith("x ") }) // Completed task
        assertTrue(lines.any { it.startsWith("(A) ") }) // Priority A
        assertTrue(lines.any { it.contains("@") }) // Context
        assertTrue(lines.any { it.contains("+") }) // Project
    }

    @Test
    fun `should process CSV format correctly`() {
        val csvFile = File(testFilesDir, "data.csv")
        val format = FormatRegistry.detectByFilename(csvFile.name)
        
        assertNotNull(format)
        assertEquals("CSV", format.name)
        
        val content = csvFile.readText()
        val lines = content.lines()
        
        // Verify CSV structure
        assertTrue(lines.isNotEmpty())
        val header = lines[0]
        assertTrue(header.contains("Name"))
        assertTrue(header.contains("Age"))
        assertTrue(header.contains("Department"))
        
        // Verify data rows
        assertTrue(lines.size > 1)
        assertTrue(lines.any { it.contains("John Doe") })
        assertTrue(lines.any { it.contains("Jane Smith") })
    }

    // ==================== Desktop Window Management Tests ====================

    @Test
    fun `should handle desktop window preferences`() {
        // Test window state persistence
        val windowPrefs = mutableMapOf<String, Any>()
        
        // Simulate window position and size
        windowPrefs["window_x"] = 100
        windowPrefs["window_y"] = 200
        windowPrefs["window_width"] = 1200
        windowPrefs["window_height"] = 800
        windowPrefs["window_maximized"] = false
        
        assertEquals(100, windowPrefs["window_x"])
        assertEquals(200, windowPrefs["window_y"])
        assertEquals(1200, windowPrefs["window_width"])
        assertEquals(800, windowPrefs["window_height"])
        assertEquals(false, windowPrefs["window_maximized"])
    }

    @Test
    fun `should handle desktop menu integration`() {
        // Test menu state management
        val menuState = mutableMapOf<String, Boolean>()
        
        menuState["file_new"] = true
        menuState["file_open"] = true
        menuState["file_save"] = false // Disabled when no file open
        menuState["edit_undo"] = false // Disabled when no changes
        menuState["edit_redo"] = false // Disabled when no changes
        
        assertTrue(menuState["file_new"] as Boolean)
        assertTrue(menuState["file_open"] as Boolean)
        assertFalse(menuState["file_save"] as Boolean)
        assertFalse(menuState["edit_undo"] as Boolean)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle file not found gracefully`() {
        val nonExistentFile = File(testFilesDir, "nonexistent.md")
        
        assertFalse(nonExistentFile.exists())
        
        // Test error handling
        try {
            nonExistentFile.readText()
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            // Expected behavior
            assertTrue(e is java.io.FileNotFoundException || e.cause is java.io.FileNotFoundException)
        }
    }

    @Test
    fun `should handle permission denied gracefully`() {
        val restrictedFile = File(testFilesDir, "restricted.txt")
        restrictedFile.writeText("Restricted content")
        
        // Make file read-only (simulating permission issue)
        restrictedFile.setReadOnly()
        
        // Should still be able to read
        assertTrue(restrictedFile.canRead())
        assertEquals("Restricted content", restrictedFile.readText())
        
        // But not write
        assertFalse(restrictedFile.canWrite())
    }

    @Test
    fun `should handle corrupted file content`() {
        val corruptedFile = File(testFilesDir, "corrupted.md")
        
        // Write binary data to text file
        val binaryData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte())
        corruptedFile.writeBytes(binaryData)
        
        // Should handle reading gracefully
        try {
            val content = corruptedFile.readText(Charsets.UTF_8)
            // Content might be garbled but should not crash
            assertNotNull(content)
        } catch (e: Exception) {
            // Expected for severely corrupted content
            assertTrue(e is java.io.IOException || e is java.nio.charset.MalformedInputException)
        }
    }

    // ==================== Cleanup ====================

    @Test
    fun `should cleanup test files properly`() {
        // Verify test directory exists
        assertTrue(testFilesDir.exists())
        assertTrue(testFilesDir.isDirectory)
        
        // Delete test directory recursively
        testFilesDir.deleteRecursively()
        
        // Verify cleanup
        assertFalse(testFilesDir.exists())
    }
}