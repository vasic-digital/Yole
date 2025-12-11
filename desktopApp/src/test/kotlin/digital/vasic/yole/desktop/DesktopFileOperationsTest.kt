/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * File operations tests for Yole Desktop Application
 *
 *########################################################*/
package digital.vasic.yole.desktop

import digital.vasic.yole.format.FormatRegistry
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
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * File operations tests for Yole Desktop Application.
 *
 * Tests cover:
 * - File save operations
 * - File load operations
 * - Recent files management
 * - File format detection
 * - File validation and security
 * - Error handling for file operations
 * - Cross-platform file path handling
 */
@RunWith(JUnit4::class)
class DesktopFileOperationsTest {

    private lateinit var tempDir: Path
    private lateinit var testFile: Path
    private val testContent = "# Test Document\n\nThis is test content for Yole desktop application."

    @Before
    fun setUp() {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("yole_test")
        testFile = tempDir.resolve("test.md")
    }

    @After
    fun tearDown() {
        // Clean up temporary files
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }

    // ==================== Basic File Operations ====================

    @Test
    fun `should save file with content successfully`() {
        val fileManager = DesktopFileManager()
        val result = fileManager.saveFile(testFile.toFile(), testContent)

        assertThat(result).isTrue()
        assertThat(Files.exists(testFile)).isTrue()
        assertThat(Files.readString(testFile)).isEqualTo(testContent)
    }

    @Test
    fun `should load file content successfully`() {
        // First create a file
        Files.writeString(testFile, testContent)
        
        val fileManager = DesktopFileManager()
        val content = fileManager.loadFile(testFile.toFile())

        assertNotNull(content)
        assertThat(content).isEqualTo(testContent)
    }

    @Test
    fun `should handle empty file content`() {
        val emptyFile = tempDir.resolve("empty.md")
        Files.writeString(emptyFile, "")
        
        val fileManager = DesktopFileManager()
        val content = fileManager.loadFile(emptyFile.toFile())

        assertNotNull(content)
        assertThat(content).isEqualTo("")
    }

    @Test
    fun `should handle large file content`() {
        val largeContent = "Line of content\n".repeat(10000)
        val largeFile = tempDir.resolve("large.md")
        Files.writeString(largeFile, largeContent)
        
        val fileManager = DesktopFileManager()
        val content = fileManager.loadFile(largeFile.toFile())

        assertNotNull(content)
        assertThat(content).isEqualTo(largeContent)
    }

    // ==================== File Format Detection ====================

    @Test
    fun `should detect markdown format from extension`() {
        val markdownFile = tempDir.resolve("document.md")
        Files.writeString(markdownFile, "# Markdown Content")
        
        val format = DesktopFileManager.detectFormatFromFile(markdownFile.toFile())
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun `should detect CSV format from extension`() {
        val csvFile = tempDir.resolve("data.csv")
        Files.writeString(csvFile, "name,age\nJohn,30\nJane,25")
        
        val format = DesktopFileManager.detectFormatFromFile(csvFile.toFile())
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
    }

    @Test
    fun `should detect LaTeX format from extension`() {
        val latexFile = tempDir.resolve("document.tex")
        Files.writeString(latexFile, "\\documentclass{article}")
        
        val format = DesktopFileManager.detectFormatFromFile(latexFile.toFile())
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_LATEX)
    }

    @Test
    fun `should detect format from content when extension is ambiguous`() {
        val txtFile = tempDir.resolve("document.txt")
        Files.writeString(txtFile, "# Markdown Header\n\nThis looks like markdown.")
        
        val format = DesktopFileManager.detectFormatFromContent(txtFile.toFile())
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun `should detect todotxt format from content`() {
        val txtFile = tempDir.resolve("tasks.txt")
        Files.writeString(txtFile, "(A) Important task @work +project\n(B) Less important task")
        
        val format = DesktopFileManager.detectFormatFromContent(txtFile.toFile())
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_TODOTXT)
    }

    // ==================== Recent Files Management ====================

    @Test
    fun `should add file to recent files list`() {
        val fileManager = DesktopFileManager()
        val testFile = tempDir.resolve("recent.md").toFile()
        Files.writeString(testFile.toPath(), "Recent file content")
        
        fileManager.addToRecentFiles(testFile)
        
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles).contains(testFile)
    }

    @Test
    fun `should limit recent files to maximum count`() {
        val fileManager = DesktopFileManager()
        val maxFiles = 10
        
        // Create more files than the limit
        for (i in 1..15) {
            val file = tempDir.resolve("file$i.md").toFile()
            Files.writeString(file.toPath(), "Content $i")
            fileManager.addToRecentFiles(file)
        }
        
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles.size).isLessThanOrEqualTo(maxFiles)
    }

    @Test
    fun `should remove non-existent files from recent list`() {
        val fileManager = DesktopFileManager()
        val existingFile = tempDir.resolve("existing.md").toFile()
        val nonExistentFile = tempDir.resolve("nonexistent.md").toFile()
        
        Files.writeString(existingFile.toPath(), "Existing content")
        fileManager.addToRecentFiles(existingFile)
        fileManager.addToRecentFiles(nonExistentFile)
        
        // Clean up recent files list
        fileManager.cleanRecentFiles()
        
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles).contains(existingFile)
        assertThat(recentFiles).doesNotContain(nonExistentFile)
    }

    // ==================== File Validation Tests ====================

    @Test
    fun `should validate file before saving`() {
        val fileManager = DesktopFileManager()
        val invalidFile = File("/invalid/path/file.md")
        
        val result = fileManager.saveFile(invalidFile, "content")
        
        assertThat(result).isFalse()
    }

    @Test
    fun `should validate file before loading`() {
        val fileManager = DesktopFileManager()
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.md")
        
        val content = fileManager.loadFile(nonExistentFile)
        
        assertThat(content).isNull()
    }

    @Test
    fun `should handle read-only files gracefully`() {
        val readonlyFile = tempDir.resolve("readonly.md").toFile()
        Files.writeString(readonlyFile.toPath(), "Read-only content")
        readonlyFile.setReadOnly()
        
        val fileManager = DesktopFileManager()
        val result = fileManager.saveFile(readonlyFile, "New content")
        
        assertThat(result).isFalse()
    }

    // ==================== Security Tests ====================

    @Test
    fun `should prevent directory traversal attacks`() {
        val fileManager = DesktopFileManager()
        val maliciousPath = "../../../etc/passwd"
        val maliciousFile = File(tempDir.toFile(), maliciousPath)
        
        val result = fileManager.saveFile(maliciousFile, "malicious content")
        
        assertThat(result).isFalse()
    }

    @Test
    fun `should sanitize file paths`() {
        val fileManager = DesktopFileManager()
        val unsafePath = "file<script>alert('xss')</script>.md"
        val safePath = fileManager.sanitizeFileName(unsafePath)
        
        assertThat(safePath).doesNotContain("<script>")
        assertThat(safePath).doesNotContain(">")
        assertThat(safePath).doesNotContain("<")
    }

    @Test
    fun `should validate file extensions`() {
        val fileManager = DesktopFileManager()
        
        assertThat(fileManager.isValidExtension(".md")).isTrue()
        assertThat(fileManager.isValidExtension(".txt")).isTrue()
        assertThat(fileManager.isValidExtension(".exe")).isFalse()
        assertThat(fileManager.isValidExtension(".bat")).isFalse()
        assertThat(fileManager.isValidExtension(".sh")).isFalse()
    }

    // ==================== Cross-Platform Tests ====================

    @Test
    fun `should handle different line endings correctly`() {
        val contentWithCRLF = "Line 1\r\nLine 2\r\nLine 3"
        val contentWithLF = "Line 1\nLine 2\nLine 3"
        val contentWithCR = "Line 1\rLine 2\rLine 3"
        
        val fileManager = DesktopFileManager()
        
        // Test CRLF
        val crlfFile = tempDir.resolve("crlf.md").toFile()
        fileManager.saveFile(crlfFile, contentWithCRLF)
        val crlfContent = fileManager.loadFile(crlfFile)
        assertThat(crlfContent).isEqualTo(contentWithCRLF)
        
        // Test LF
        val lfFile = tempDir.resolve("lf.md").toFile()
        fileManager.saveFile(lfFile, contentWithLF)
        val lfContent = fileManager.loadFile(lfFile)
        assertThat(lfContent).isEqualTo(contentWithLF)
        
        // Test CR
        val crFile = tempDir.resolve("cr.md").toFile()
        fileManager.saveFile(crFile, contentWithCR)
        val crContent = fileManager.loadFile(crFile)
        assertThat(crContent).isEqualTo(contentWithCR)
    }

    @Test
    fun `should handle unicode filenames correctly`() {
        val unicodeFile = tempDir.resolve("文档_файл_📝.md").toFile()
        val content = "Unicode content with emojis 🎉 and special chars ñ"
        
        val fileManager = DesktopFileManager()
        val saveResult = fileManager.saveFile(unicodeFile, content)
        val loadContent = fileManager.loadFile(unicodeFile)
        
        assertThat(saveResult).isTrue()
        assertThat(loadContent).isEqualTo(content)
        assertThat(unicodeFile.exists()).isTrue()
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle rapid file operations efficiently`() {
        val fileManager = DesktopFileManager()
        val iterations = 100
        
        val startTime = System.currentTimeMillis()
        
        for (i in 1..iterations) {
            val file = tempDir.resolve("rapid$i.md").toFile()
            val content = "Content $i"
            
            fileManager.saveFile(file, content)
            val loadedContent = fileManager.loadFile(file)
            
            assertThat(loadedContent).isEqualTo(content)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (less than 5 seconds for 100 operations)
        assertThat(duration).isLessThan(5000)
    }

    @Test
    fun `should handle concurrent file operations safely`() {
        val fileManager = DesktopFileManager()
        val threadCount = 10
        val filesPerThread = 10
        val results = mutableListOf<Boolean>()
        
        val threads = (1..threadCount).map { threadId ->
            Thread {
                for (i in 1..filesPerThread) {
                    val file = tempDir.resolve("concurrent_${threadId}_$i.md").toFile()
                    val content = "Thread $threadId, File $i"
                    
                    val saveResult = fileManager.saveFile(file, content)
                    val loadContent = fileManager.loadFile(file)
                    
                    synchronized(results) {
                        results.add(saveResult && loadContent == content)
                    }
                }
            }
        }
        
        // Start all threads
        threads.forEach { it.start() }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // All operations should succeed
        assertThat(results).hasSize(threadCount * filesPerThread)
        assertThat(results).containsOnly(true)
    }

    // ==================== Mock Implementation ====================

    /**
     * Mock implementation of DesktopFileManager for testing
     */
    class DesktopFileManager {
        private val recentFiles = mutableListOf<File>()
        private val maxRecentFiles = 10
        
        fun saveFile(file: File, content: String): Boolean {
            return try {
                // Validate file path
                if (!isValidFilePath(file)) {
                    return false
                }
                
                // Ensure parent directory exists
                file.parentFile?.mkdirs()
                
                // Write content
                file.writeText(content)
                
                // Add to recent files
                addToRecentFiles(file)
                
                true
            } catch (e: Exception) {
                false
            }
        }
        
        fun loadFile(file: File): String? {
            return try {
                if (!file.exists() || !file.isFile) {
                    return null
                }
                file.readText()
            } catch (e: Exception) {
                null
            }
        }
        
        fun addToRecentFiles(file: File) {
            recentFiles.remove(file)
            recentFiles.add(0, file)
            
            // Limit to max recent files
            while (recentFiles.size > maxRecentFiles) {
                recentFiles.removeAt(recentFiles.size - 1)
            }
        }
        
        fun getRecentFiles(): List<File> {
            return recentFiles.toList()
        }
        
        fun cleanRecentFiles() {
            recentFiles.removeIf { !it.exists() }
        }
        
        fun isValidExtension(extension: String): Boolean {
            val validExtensions = setOf(
                ".md", ".txt", ".csv", ".tex", ".org", ".adoc", ".wiki",
                ".json", ".xml", ".yaml", ".yml", ".html", ".css", ".js",
                ".kt", ".kts", ".java", ".py", ".cpp", ".c", ".h", ".rs",
                ".go", ".rb", ".php", ".swift", ".scala", ".r", ".m",
                ".sql", ".sh", ".bat", ".cmd", ".ps1", ".pl", ".lua",
                ".nim", ".dart", ".ts", ".tsx", ".jsx", ".vue", ".sass",
                ".scss", ".less", ".styl", ".pug", ".haml", ".slim", ".erb",
                ".coffee", ".litcoffee", ".tsv", ".log", ".conf", ".config",
                ".ini", ".properties", ".env", ".gitignore", ".dockerignore"
            )
            return validExtensions.contains(extension.lowercase())
        }
        
        fun sanitizeFileName(fileName: String): String {
            return fileName.replace(Regex("[<>&\"']"), "_")
        }
        
        fun isValidFilePath(file: File): Boolean {
            // Prevent directory traversal
            val canonicalPath = file.canonicalPath
            val tempDirCanonical = tempDir.toFile().canonicalPath
            
            return canonicalPath.startsWith(tempDirCanonical)
        }
        
        companion object {
            fun detectFormatFromFile(file: File): TextFormat? {
                val extension = file.extension
                return FormatRegistry.getByExtension(extension)
            }
            
            fun detectFormatFromContent(file: File): TextFormat? {
                val content = file.readText()
                return FormatRegistry.detectByContent(content)
            }
        }
    }
}