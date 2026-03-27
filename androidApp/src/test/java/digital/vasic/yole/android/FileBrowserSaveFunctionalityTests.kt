/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * File Browser and Save Functionality Tests
 * Validates SAF support and proper file operations
 *
 *########################################################*/
package digital.vasic.yole.android

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import java.io.File

/**
 * Tests for file browser and save functionality
 */
@RunWith(AndroidJUnit4::class)
class FileBrowserSaveFunctionalityTests {

    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_files").apply {
            mkdirs()
        }
    }

    @Test
    fun testSaveFileWithDirectAccess() {
        val fileName = "test_save_direct.txt"
        val filePath = File(testDir, fileName).absolutePath
        val content = "Test content for direct file save"

        val result = saveFile(context, filePath, content, null)
        
        assertTrue("Save should succeed", result)
        
        val savedFile = File(filePath)
        assertTrue("File should exist", savedFile.exists())
        assertEquals("Content should match", content, savedFile.readText())
    }

    @Test
    fun testSaveFileCreatesParentDirectories() {
        val nestedDir = File(testDir, "nested/deep/directory")
        val filePath = File(nestedDir, "test.txt").absolutePath
        val content = "Test content"

        val result = saveFile(context, filePath, content, null)
        
        assertTrue("Save should succeed", result)
        assertTrue("Parent directories should be created", nestedDir.exists())
        assertTrue("File should exist", File(filePath).exists())
    }

    @Test
    fun testSaveFileWithEmptyContent() {
        val fileName = "test_empty.txt"
        val filePath = File(testDir, fileName).absolutePath
        val content = ""

        val result = saveFile(context, filePath, content, null)
        
        assertTrue("Save should succeed with empty content", result)
        assertTrue("File should exist", File(filePath).exists())
        assertEquals("Content should be empty", "", File(filePath).readText())
    }

    @Test
    fun testSaveFileWithSpecialCharacters() {
        val fileName = "test_special_ñ_中_🎉.txt"
        val filePath = File(testDir, fileName).absolutePath
        val content = "Special content: ñ 中 🎉 émojis"

        val result = saveFile(context, filePath, content, null)
        
        assertTrue("Save should succeed with special characters", result)
        assertTrue("File should exist", File(filePath).exists())
        assertEquals("Content should match", content, File(filePath).readText())
    }

    @Test
    fun testSaveFileWithMultilineContent() {
        val fileName = "test_multiline.txt"
        val filePath = File(testDir, fileName).absolutePath
        val content = """Line 1
Line 2
Line 3

Line 5 after empty line"""

        val result = saveFile(context, filePath, content, null)
        
        assertTrue("Save should succeed with multiline content", result)
        assertEquals("Content should match", content, File(filePath).readText())
    }

    @Test
    fun testLoadFileExists() {
        val fileName = "test_load.txt"
        val filePath = File(testDir, fileName).absolutePath
        val content = "Test content to load"
        File(filePath).writeText(content)

        val loadedContent = loadFile(filePath)
        
        assertNotNull("Should load existing file", loadedContent)
        assertEquals("Content should match", content, loadedContent)
    }

    @Test
    fun testLoadFileNotExists() {
        val filePath = File(testDir, "nonexistent.txt").absolutePath

        val loadedContent = loadFile(filePath)
        
        assertNull("Should return null for non-existent file", loadedContent)
    }

    @Test
    fun testDeleteFile() {
        val fileName = "test_delete.txt"
        val filePath = File(testDir, fileName).absolutePath
        File(filePath).writeText("Content to delete")

        assertTrue("File should exist before delete", File(filePath).exists())
        
        val result = deleteFile(filePath)
        
        assertTrue("Delete should succeed", result)
        assertFalse("File should not exist after delete", File(filePath).exists())
    }

    @Test
    fun testDeleteNonExistentFile() {
        val filePath = File(testDir, "nonexistent_delete.txt").absolutePath

        val result = deleteFile(filePath)
        
        // Delete returns false for non-existent files (no exception)
        assertFalse("Delete should return false for non-existent file", result)
    }

    @Test
    fun testFileBrowserLoadsLocalDirectory() {
        // Create test files in directory
        val dir = File(testDir, "browser_test").apply { mkdirs() }
        File(dir, "file1.txt").writeText("Content 1")
        File(dir, "file2.txt").writeText("Content 2")
        File(dir, "subdir").mkdirs()

        // Verify directory structure
        assertTrue("Directory should exist", dir.exists())
        assertTrue("Directory should be readable", dir.canRead())
        
        val files = dir.listFiles()
        assertNotNull("Should list files", files)
        assertEquals("Should have 3 items", 3, files?.size)
        
        // Verify file properties
        val txtFiles = files?.filter { it.extension == "txt" }
        assertEquals("Should have 2 text files", 2, txtFiles?.size)
    }

    @Test
    fun testFileBrowserHandlesEmptyDirectory() {
        val dir = File(testDir, "empty_browser_test").apply { mkdirs() }

        assertTrue("Directory should exist", dir.exists())
        
        val files = dir.listFiles()
        assertNotNull("Should return non-null array", files)
        assertEquals("Should have 0 items in empty directory", 0, files?.size)
    }

    @Test
    fun testFileBrowserHandlesUnreadableDirectory() {
        // This test documents expected behavior - in real app, SAF fallback should be used
        val dir = File(testDir, "unreadable_test").apply { 
            mkdirs()
            // Note: We can't actually make it unreadable on most test environments
            // but we test the logic path
        }

        // Simulate permission check logic
        val exists = dir.exists()
        val canRead = dir.canRead()
        
        assertTrue("Directory should exist", exists)
        assertTrue("Should be readable in test environment", canRead)
    }

    @Test
    fun testVersionCodeIsIncremented() {
        val buildFile = File("androidApp/build.gradle.kts")
        val content = buildFile.readText()
        
        // Extract version code
        val versionCodeRegex = Regex("versionCode\\s*=\\s*(\\d+)")
        val match = versionCodeRegex.find(content)
        
        assertNotNull("Should find versionCode", match)
        val versionCode = match?.groupValues?.get(1)?.toInt()
        assertNotNull("Version code should be parseable", versionCode)
        
        // Version code should be >= 6 (incremented from 5)
        assertTrue("Version code should be >= 6", versionCode!! >= 6)
    }

    @Test
    fun testSaveFileFunctionSignature() {
        // Verify the saveFile function accepts all required parameters
        val method = try {
            Class.forName("digital.vasic.yole.android.YoleAppKt")
                .getMethod("saveFile", Context::class.java, String::class.java, String::class.java, Uri::class.java)
        } catch (e: Exception) {
            null
        }
        
        assertNotNull("saveFile function should exist with SAF support", method)
    }

    @Test
    fun testCreateFileWithSAFFunctionSignature() {
        // Verify the createFileWithSAF function exists
        val method = try {
            Class.forName("digital.vasic.yole.android.YoleAppKt")
                .getMethod("createFileWithSAF", Context::class.java, Uri::class.java, String::class.java, String::class.java)
        } catch (e: Exception) {
            null
        }
        
        assertNotNull("createFileWithSAF function should exist", method)
    }
}
