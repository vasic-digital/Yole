/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop File Manager Tests
 * Comprehensive tests for desktop file operations
 *
 *########################################################*/
package digital.vasic.yole.desktop

import digital.vasic.yole.desktop.file.DesktopFileManager
import digital.vasic.yole.format.FormatRegistry
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
 * Comprehensive tests for DesktopFileManager.
 */
@RunWith(JUnit4::class)
class DesktopFileManagerTest {

    private lateinit var tempDir: Path
    private lateinit var fileManager: DesktopFileManager
    private val testContent = "# Test Document\n\nThis is test content for Yole desktop application."

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("yole_test")
        fileManager = DesktopFileManager()
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
        val testFile = tempDir.resolve("test.md").toFile()
        
        val result = fileManager.saveFile(testFile, testContent)
        
        assertThat(result).isTrue()
        assertThat(testFile.exists()).isTrue()
        assertThat(testFile.readText()).isEqualTo(testContent)
    }

    @Test
    fun `should load file content successfully`() {
        val testFile = tempDir.resolve("test.md").toFile()
        testFile.writeText(testContent)
        
        val content = fileManager.loadFile(testFile)
        
        assertNotNull(content)
        assertThat(content).isEqualTo(testContent)
    }

    @Test
    fun `should handle empty file content`() {
        val emptyFile = tempDir.resolve("empty.md").toFile()
        emptyFile.writeText("")
        
        val content = fileManager.loadFile(emptyFile)
        
        assertNotNull(content)
        assertThat(content).isEqualTo("")
    }

    @Test
    fun `should handle large file content`() {
        val largeContent = "Line of content\n".repeat(10000)
        val largeFile = tempDir.resolve("large.md").toFile()
        largeFile.writeText(largeContent)
        
        val content = fileManager.loadFile(largeFile)
        
        assertNotNull(content)
        assertThat(content).isEqualTo(largeContent)
    }

    // ==================== File Format Detection ====================

    @Test
    fun `should detect markdown format from extension`() {
        val markdownFile = tempDir.resolve("document.md").toFile()
        markdownFile.writeText("# Markdown Content")
        
        val format = fileManager.detectFormatFromFile(markdownFile)
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun `should detect CSV format from extension`() {
        val csvFile = tempDir.resolve("data.csv").toFile()
        csvFile.writeText("name,age\nJohn,30\nJane,25")
        
        val format = fileManager.detectFormatFromFile(csvFile)
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
    }

    @Test
    fun `should detect format from content when extension is ambiguous`() {
        val txtFile = tempDir.resolve("document.txt").toFile()
        txtFile.writeText("# Markdown Header\n\nThis looks like markdown.")
        
        val format = fileManager.detectFormatFromContent(txtFile)
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun `should detect todotxt format from content`() {
        val txtFile = tempDir.resolve("tasks.txt").toFile()
        txtFile.writeText("(A) Important task @work +project\n(B) Less important task")
        
        val format = fileManager.detectFormatFromContent(txtFile)
        
        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_TODOTXT)
    }

    // ==================== Recent Files Management ====================

    @Test
    fun `should add file to recent files list`() {
        val testFile = tempDir.resolve("recent.md").toFile()
        testFile.writeText("Recent file content")
        
        fileManager.addToRecentFiles(testFile)
        
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles).contains(testFile)
    }

    @Test
    fun `should limit recent files to maximum count`() {
        val maxFiles = 20
        
        // Create more files than the limit
        for (i in 1..25) {
            val file = tempDir.resolve("file$i.md").toFile()
            file.writeText("Content $i")
            fileManager.addToRecentFiles(file)
        }
        
        val recentFiles = fileManager.getRecentFiles()
        assertThat(recentFiles.size).isLessThanOrEqualTo(maxFiles)
    }

    @Test
    fun `should remove non-existent files from recent list`() {
        val existingFile = tempDir.resolve("existing.md").toFile()
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.md")
        
        existingFile.writeText("Existing content")
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
        val invalidFile = File("/invalid/path/file.md")
        
        val result = fileManager.saveFile(invalidFile, "content")
        
        assertThat(result).isFalse()
    }

    @Test
    fun `should validate file before loading`() {
        val nonExistentFile = File(tempDir.toFile(), "nonexistent.md")
        
        val content = fileManager.loadFile(nonExistentFile)
        
        assertThat(content).isNull()
    }

    @Test
    fun `should handle read-only files gracefully`() {
        val readonlyFile = tempDir.resolve("readonly.md").toFile()
        readonlyFile.writeText("Read-only content")
        readonlyFile.setReadOnly()
        
        val result = fileManager.saveFile(readonlyFile, "New content")
        
        assertThat(result).isFalse()
    }

    // ==================== Security Tests ====================

    @Test
    fun `should prevent directory traversal attacks`() {
        val maliciousFile = File(tempDir.toFile(), "../../../etc/passwd")
        
        val result = fileManager.saveFile(maliciousFile, "malicious content")
        
        assertThat(result).isFalse()
    }

    @Test
    fun `should sanitize file names`() {
        val unsafeName = "file<script>alert('xss')</script>.md"
        val safeName = fileManager.sanitizeFileName(unsafeName)
        
        assertThat(safeName).doesNotContain("<script>")
        assertThat(safeName).doesNotContain(">")
        assertThat(safeName).doesNotContain("<")
    }

    @Test
    fun `should validate file extensions`() {
        assertThat(fileManager.isValidExtension("md")).isTrue()
        assertThat(fileManager.isValidExtension("txt")).isTrue()
        assertThat(fileManager.isValidExtension("exe")).isFalse()
        assertThat(fileManager.isValidExtension("bat")).isFalse()
        assertThat(fileManager.isValidExtension("sh")).isFalse()
    }

    // ==================== Cross-Platform Tests ====================

    @Test
    fun `should handle different line endings correctly`() {
        val contentWithCRLF = "Line 1\r\nLine 2\r\nLine 3"
        val contentWithLF = "Line 1\nLine 2\nLine 3"
        val contentWithCR = "Line 1\rLine 2\rLine 3"
        
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
        
        val saveResult = fileManager.saveFile(unicodeFile, content)
        val loadContent = fileManager.loadFile(unicodeFile)
        
        assertThat(saveResult).isTrue()
        assertThat(loadContent).isEqualTo(content)
        assertThat(unicodeFile.exists()).isTrue()
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle rapid file operations efficiently`() {
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

    // ==================== Document Management Tests ====================

    @Test
    fun `should track open documents correctly`() {
        val testFile = tempDir.resolve("document.md").toFile()
        testFile.writeText("Document content")
        
        fileManager.loadFile(testFile)
        
        val openDocuments = fileManager.getOpenDocuments()
        assertThat(openDocuments).hasSize(1)
        assertThat(openDocuments[0].file).isEqualTo(testFile)
        assertThat(openDocuments[0].content).isEqualTo("Document content")
    }

    @Test
    fun `should detect unsaved changes correctly`() {
        val testFile = tempDir.resolve("unsaved.md").toFile()
        testFile.writeText("Original content")
        
        fileManager.loadFile(testFile)
        
        // Modify the file directly
        testFile.writeText("Modified content")
        
        val hasUnsavedChanges = fileManager.hasUnsavedChanges(testFile)
        assertThat(hasUnsavedChanges).isTrue()
    }

    @Test
    fun `should create backup files correctly`() {
        val testFile = tempDir.resolve("backup.md").toFile()
        testFile.writeText("Content to backup")
        
        val backupResult = fileManager.createBackup(testFile)
        
        assertThat(backupResult).isTrue()
        
        val backupFile = File("${testFile.absolutePath}.bak")
        assertThat(backupFile.exists()).isTrue()
        assertThat(backupFile.readText()).isEqualTo("Content to backup")
    }

    // ==================== File Information Tests ====================

    @Test
    fun `should get file information correctly`() {
        val testFile = tempDir.resolve("info.md").toFile()
        testFile.writeText("File information test content")
        
        val fileInfo = fileManager.getFileInfo(testFile)
        
        assertNotNull(fileInfo)
        assertThat(fileInfo.file).isEqualTo(testFile)
        assertThat(fileInfo.size).isEqualTo(testFile.length())
        assertThat(fileInfo.isReadable).isTrue()
        assertThat(fileInfo.isWritable).isTrue()
        assertThat(fileInfo.format?.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    @Test
    fun `should handle non-existent file information`() {
        val nonExistentFile = tempDir.resolve("nonexistent.md").toFile()
        
        val fileInfo = fileManager.getFileInfo(nonExistentFile)
        
        assertThat(fileInfo).isNull()
    }
}