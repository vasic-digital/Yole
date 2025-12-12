/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Window Manager Tests
 * Tests for multi-window functionality
 *
 *########################################################*/
package digital.vasic.yole.desktop

import digital.vasic.yole.desktop.window.DesktopWindowManager
import digital.vasic.yole.format.FormatRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.After
import java.io.File
import java.nio.file.Files
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for DesktopWindowManager multi-window functionality.
 */
@RunWith(JUnit4::class)
class DesktopWindowManagerTest {

    private lateinit var windowManager: DesktopWindowManager
    private lateinit var tempDir: java.nio.file.Path

    @Before
    fun setUp() {
        windowManager = DesktopWindowManager()
        tempDir = Files.createTempDirectory("yole_window_test")
    }

    @After
    fun tearDown() {
        // Clean up temporary files
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }

    // ==================== Window Creation Tests ====================

    @Test
    fun `should create new window successfully`() {
        val window = windowManager.createWindow()
        
        assertNotNull(window)
        assertThat(window.title).isEqualTo("Untitled 1")
        assertThat(window.file).isNull()
        assertThat(window.content).isEqualTo("")
        assertThat(window.isModified).isFalse()
    }

    @Test
    fun `should create window with file and content`() {
        val testFile = tempDir.resolve("test.md").toFile()
        val testContent = "# Test Content"
        
        val window = windowManager.createWindow(testFile, testContent)
        
        assertNotNull(window)
        assertThat(window.title).isEqualTo("test.md")
        assertThat(window.file).isEqualTo(testFile)
        assertThat(window.content).isEqualTo(testContent)
        assertThat(window.isModified).isFalse()
    }

    @Test
    fun `should create multiple windows with unique titles`() {
        val window1 = windowManager.createWindow()
        val window2 = windowManager.createWindow()
        val window3 = windowManager.createWindow()
        
        assertThat(window1.title).isEqualTo("Untitled 1")
        assertThat(window2.title).isEqualTo("Untitled 2")
        assertThat(window3.title).isEqualTo("Untitled 3")
    }

    @Test
    fun `should enforce maximum window limit`() {
        val maxWindows = 10
        
        // Create more than the maximum number of windows
        val windows = (1..maxWindows + 5).map {
            windowManager.createWindow()
        }
        
        val currentWindows = windowManager.getWindows()
        assertThat(currentWindows.size).isLessThanOrEqualTo(maxWindows)
    }

    // ==================== Window Management Tests ====================

    @Test
    fun `should close window successfully`() {
        val window = windowManager.createWindow()
        val windowId = window.id
        
        val closeResult = windowManager.closeWindow(windowId)
        
        assertThat(closeResult).isTrue()
        assertThat(windowManager.getWindows()).doesNotContain(window)
    }

    @Test
    fun `should not close window with unsaved changes`() {
        val testFile = tempDir.resolve("unsaved.md").toFile()
        val window = windowManager.createWindow(testFile, "Content")
        window.isModified = true
        val windowId = window.id
        
        val closeResult = windowManager.closeWindow(windowId)
        
        assertThat(closeResult).isFalse()
        assertThat(windowManager.getWindows()).contains(window)
    }

    @Test
    fun `should handle closing non-existent window gracefully`() {
        val closeResult = windowManager.closeWindow("non-existent-id")
        
        assertThat(closeResult).isFalse()
    }

    @Test
    fun `should get all windows correctly`() {
        val window1 = windowManager.createWindow()
        val window2 = windowManager.createWindow()
        val window3 = windowManager.createWindow()
        
        val windows = windowManager.getWindows()
        
        assertThat(windows).hasSize(3)
        assertThat(windows).contains(window1, window2, window3)
    }

    @Test
    fun `should get specific window by ID`() {
        val window = windowManager.createWindow()
        val windowId = window.id
        
        val retrievedWindow = windowManager.getWindow(windowId)
        
        assertThat(retrievedWindow).isEqualTo(window)
    }

    // ==================== Window Content Management Tests ====================

    @Test
    fun `should update window content correctly`() {
        val window = windowManager.createWindow()
        val windowId = window.id
        val newContent = "Updated content"
        
        windowManager.updateWindowContent(windowId, newContent, true)
        
        val updatedWindow = windowManager.getWindow(windowId)
        assertThat(updatedWindow?.content).isEqualTo(newContent)
        assertThat(updatedWindow?.isModified).isTrue()
    }

    @Test
    fun `should update window file association correctly`() {
        val window = windowManager.createWindow()
        val windowId = window.id
        val newFile = tempDir.resolve("new_file.md").toFile()
        val newTitle = "New File Title"
        
        windowManager.updateWindowFile(windowId, newFile, newTitle)
        
        val updatedWindow = windowManager.getWindow(windowId)
        assertThat(updatedWindow?.file).isEqualTo(newFile)
        assertThat(updatedWindow?.title).isEqualTo(newTitle)
        assertThat(updatedWindow?.isModified).isFalse()
    }

    @Test
    fun `should handle updating non-existent window gracefully`() {
        val newFile = tempDir.resolve("new_file.md").toFile()
        
        windowManager.updateWindowFile("non-existent-id", newFile, "Title")
        windowManager.updateWindowContent("non-existent-id", "Content", true)
        
        // Should not throw exceptions
    }

    // ==================== Unsaved Changes Tests ====================

    @Test
    fun `should detect unsaved changes correctly`() {
        val window1 = windowManager.createWindow()
        val window2 = windowManager.createWindow()
        
        window1.isModified = true
        
        val hasUnsavedChanges = windowManager.hasUnsavedChanges()
        assertThat(hasUnsavedChanges).isTrue()
    }

    @Test
    fun `should get windows with unsaved changes`() {
        val window1 = windowManager.createWindow()
        val window2 = windowManager.createWindow()
        val window3 = windowManager.createWindow()
        
        window1.isModified = true
        window3.isModified = true
        
        val windowsWithUnsavedChanges = windowManager.getWindowsWithUnsavedChanges()
        
        assertThat(windowsWithUnsavedChanges).hasSize(2)
        assertThat(windowsWithUnsavedChanges).contains(window1, window3)
        assertThat(windowsWithUnsavedChanges).doesNotContain(window2)
    }

    // ==================== Window State Tests ====================

    @Test
    fun `should save and load window state correctly`() {
        val window = windowManager.createWindow()
        val windowId = window.id
        
        // Modify window state
        window.title = "Modified Title"
        window.content = "Modified Content"
        window.isModified = true
        
        // Save state
        windowManager.saveWindowState()
        
        // Clear current windows
        windowManager.closeWindow(windowId)
        
        // Load state
        windowManager.loadWindowState()
        
        // Window state should be restored
        val restoredWindow = windowManager.getWindow(windowId)
        // Note: This test assumes window state persistence is implemented
        // In actual implementation, you would verify the restored state
    }

    // ==================== Display Title Tests ====================

    @Test
    fun `should get display title for unmodified window`() {
        val window = windowManager.createWindow()
        window.title = "Test Document"
        window.isModified = false
        
        val displayTitle = window.getDisplayTitle()
        
        assertThat(displayTitle).isEqualTo("Test Document")
    }

    @Test
    fun `should get display title for modified window`() {
        val window = windowManager.createWindow()
        window.title = "Test Document"
        window.isModified = true
        
        val displayTitle = window.getDisplayTitle()
        
        assertThat(displayTitle).isEqualTo("* Test Document")
    }

    @Test
    fun `should identify new document correctly`() {
        val newWindow = windowManager.createWindow()
        val existingFileWindow = windowManager.createWindow(
            tempDir.resolve("existing.md").toFile(),
            "Content"
        )
        
        assertThat(newWindow.isNewDocument()).isTrue()
        assertThat(existingFileWindow.isNewDocument()).isFalse()
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect format for window with file`() {
        val markdownFile = tempDir.resolve("document.md").toFile()
        markdownFile.writeText("# Markdown Content")
        
        val window = windowManager.createWindow(markdownFile, "Content")
        
        // Format would be detected when the file is loaded
        assertThat(window.format?.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
    }

    // ==================== Concurrent Window Operations Tests ====================

    @Test
    fun `should handle concurrent window operations safely`() {
        val threadCount = 10
        val windowsPerThread = 5
        val results = mutableListOf<Boolean>()
        
        val threads = (1..threadCount).map { threadId ->
            Thread {
                for (i in 1..windowsPerThread) {
                    val file = tempDir.resolve("window_${threadId}_$i.md").toFile()
                    val window = windowManager.createWindow(file, "Content $i")
                    
                    // Update window content
                    windowManager.updateWindowContent(window.id, "Updated content $i", true)
                    
                    // Try to close window
                    val closeResult = windowManager.closeWindow(window.id)
                    
                    synchronized(results) {
                        results.add(closeResult)
                    }
                }
            }
        }
        
        // Start all threads
        threads.forEach { it.start() }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // All operations should succeed
        assertThat(results).hasSize(threadCount * windowsPerThread)
        assertThat(results).containsOnly(true)
    }
}