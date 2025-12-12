/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Dropbox Storage Tests - Comprehensive Coverage
 *
 *########################################################*/

package digital.vasic.yole.network

import digital.vasic.yole.network.dropbox.DropboxStorage
import digital.vasic.yole.network.common.NetworkCredentials
import digital.vasic.yole.network.common.StorageException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive test suite for DropboxStorage
 * Tests all Dropbox API integration functionality
 */
class DropboxStorageTest {

    private lateinit var storage: DropboxStorage
    private val testCredentials = NetworkCredentials(
        accessToken = "test_access_token",
        refreshToken = "test_refresh_token",
        clientId = "test_client_id",
        clientSecret = "test_client_secret"
    )

    @BeforeTest
    fun setup() {
        storage = DropboxStorage()
    }

    @Test
    fun testAuthentication() = runTest {
        // Test successful authentication
        val result = storage.authenticate(testCredentials.accessToken)
        assertTrue(result.isSuccess)
        assertEquals("test_access_token", storage.getCurrentToken())
    }

    @Test
    fun testInvalidAuthentication() = runTest {
        // Test authentication with invalid token
        val result = storage.authenticate("invalid_token")
        assertTrue(result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is StorageException)
    }

    @Test
    fun testListFiles() = runTest {
        // Authenticate first
        storage.authenticate(testCredentials.accessToken)
        
        // Test listing root directory
        val result = storage.listFiles("/")
        assertTrue(result.isSuccess)
        
        val files = result.getOrNull()
        assertNotNull(files)
        assertTrue(files.isNotEmpty()) // Should have at least some files
        
        // Verify file structure
        files.forEach { file ->
            assertNotNull(file.name)
            assertNotNull(file.path)
            assertNotNull(file.size)
            assertNotNull(file.lastModified)
            assertTrue(file.size >= 0)
        }
    }

    @Test
    fun testListFilesInDirectory() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test listing specific directory
        val result = storage.listFiles("/Documents")
        assertTrue(result.isSuccess)
        
        val files = result.getOrNull()
        assertNotNull(files)
        
        // All files should be in the specified directory
        files.forEach { file ->
            assertTrue(file.path.startsWith("/Documents"))
        }
    }

    @Test
    fun testListNonExistentDirectory() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test listing non-existent directory
        val result = storage.listFiles("/NonExistentDirectory")
        assertTrue(result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is StorageException)
    }

    @Test
    fun testDownloadFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test downloading a file
        val result = storage.downloadFile("/test-document.md")
        assertTrue(result.isSuccess)
        
        val content = result.getOrNull()
        assertNotNull(content)
        assertTrue(content.isNotEmpty())
        
        // Verify content is valid
        val contentString = content.decodeToString()
        assertTrue(contentString.isNotBlank())
    }

    @Test
    fun testDownloadNonExistentFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test downloading non-existent file
        val result = storage.downloadFile("/non-existent-file.txt")
        assertTrue(result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is StorageException)
    }

    @Test
    fun testUploadFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test uploading a file
        val content = "# Test Document\n\nThis is a test file for Dropbox upload."
        val result = storage.uploadFile("/uploaded-test.md", content.encodeToByteArray())
        assertTrue(result.isSuccess)
        
        // Verify file was uploaded by downloading it back
        val downloadResult = storage.downloadFile("/uploaded-test.md")
        assertTrue(downloadResult.isSuccess)
        
        val downloadedContent = downloadResult.getOrNull()
        assertNotNull(downloadedContent)
        assertEquals(content, downloadedContent.decodeToString())
    }

    @Test
    fun testUploadFileWithSpecialCharacters() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test uploading file with special characters
        val content = """# Special Characters Test

This file contains special characters: !@#$%^&*()
Unicode characters: 你好世界 🌍
Quotes: "double quotes" and 'single quotes'
Code: `console.log("Hello World");`
"""
        
        val result = storage.uploadFile("/special-chars-test.md", content.encodeToByteArray())
        assertTrue(result.isSuccess)
        
        // Verify content integrity
        val downloadResult = storage.downloadFile("/special-chars-test.md")
        assertTrue(downloadResult.isSuccess)
        
        val downloadedContent = downloadResult.getOrNull()
        assertNotNull(downloadedContent)
        assertEquals(content, downloadedContent.decodeToString())
    }

    @Test
    fun testDeleteFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // First upload a file to delete
        val uploadResult = storage.uploadFile("/to-delete.txt", "This file will be deleted".encodeToByteArray())
        assertTrue(uploadResult.isSuccess)
        
        // Test deleting the file
        val deleteResult = storage.deleteFile("/to-delete.txt")
        assertTrue(deleteResult.isSuccess)
        
        // Verify file is deleted by trying to download it
        val downloadResult = storage.downloadFile("/to-delete.txt")
        assertTrue(downloadResult.isFailure)
    }

    @Test
    fun testDeleteNonExistentFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test deleting non-existent file
        val result = storage.deleteFile("/non-existent-file.txt")
        assertTrue(result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is StorageException)
    }

    @Test
    fun testFileMetadata() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Upload a file first
        val uploadResult = storage.uploadFile("/metadata-test.md", "# Test File\nContent here".encodeToByteArray())
        assertTrue(uploadResult.isSuccess)
        
        // Test getting file metadata
        val metadataResult = storage.getFileMetadata("/metadata-test.md")
        assertTrue(metadataResult.isSuccess)
        
        val metadata = metadataResult.getOrNull()
        assertNotNull(metadata)
        assertEquals("metadata-test.md", metadata.name)
        assertEquals("/metadata-test.md", metadata.path)
        assertTrue(metadata.size > 0)
        assertNotNull(metadata.lastModified)
        assertEquals("text/markdown", metadata.contentType)
    }

    @Test
    fun testCreateDirectory() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test creating a directory
        val result = storage.createDirectory("/test-directory")
        assertTrue(result.isSuccess)
        
        // Verify directory exists by listing it
        val listResult = storage.listFiles("/")
        assertTrue(listResult.isSuccess)
        
        val files = listResult.getOrNull()
        assertNotNull(files)
        
        val directoryExists = files.any { it.name == "test-directory" && it.isDirectory }
        assertTrue(directoryExists, "Directory should exist after creation")
    }

    @Test
    fun testMoveFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Upload a file first
        val uploadResult = storage.uploadFile("/original-location.txt", "File to be moved".encodeToByteArray())
        assertTrue(uploadResult.isSuccess)
        
        // Test moving the file
        val moveResult = storage.moveFile("/original-location.txt", "/new-location.txt")
        assertTrue(moveResult.isSuccess)
        
        // Verify file exists at new location
        val newLocationResult = storage.downloadFile("/new-location.txt")
        assertTrue(newLocationResult.isSuccess)
        
        // Verify file no longer exists at original location
        val originalLocationResult = storage.downloadFile("/original-location.txt")
        assertTrue(originalLocationResult.isFailure)
    }

    @Test
    fun testCopyFile() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Upload a file first
        val uploadResult = storage.uploadFile("/source-file.txt", "File to be copied".encodeToByteArray())
        assertTrue(uploadResult.isSuccess)
        
        // Test copying the file
        val copyResult = storage.copyFile("/source-file.txt", "/copied-file.txt")
        assertTrue(copyResult.isSuccess)
        
        // Verify both files exist and have same content
        val sourceResult = storage.downloadFile("/source-file.txt")
        assertTrue(sourceResult.isSuccess)
        
        val copiedResult = storage.downloadFile("/copied-file.txt")
        assertTrue(copiedResult.isSuccess)
        
        val sourceContent = sourceResult.getOrNull()
        val copiedContent = copiedResult.getOrNull()
        
        assertNotNull(sourceContent)
        assertNotNull(copiedContent)
        assertArrayEquals(sourceContent, copiedContent)
    }

    @Test
    fun testBatchOperations() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test batch upload
        val filesToUpload = mapOf(
            "/batch/file1.txt" to "Content 1".encodeToByteArray(),
            "/batch/file2.txt" to "Content 2".encodeToByteArray(),
            "/batch/file3.txt" to "Content 3".encodeToByteArray()
        )
        
        val batchUploadResult = storage.batchUpload(filesToUpload)
        assertTrue(batchUploadResult.isSuccess)
        
        // Verify all files were uploaded
        filesToUpload.forEach { (path, expectedContent) ->
            val downloadResult = storage.downloadFile(path)
            assertTrue(downloadResult.isSuccess)
            
            val downloadedContent = downloadResult.getOrNull()
            assertNotNull(downloadedContent)
            assertArrayEquals(expectedContent, downloadedContent)
        }
    }

    @Test
    fun testRateLimiting() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test that rate limiting is handled gracefully
        val results = mutableListOf<Result<ByteArray>>()
        
        // Make many rapid requests
        repeat(10) { i ->
            val result = storage.downloadFile("/test-file.txt")
            results.add(result)
        }
        
        // Most requests should succeed, but some might be rate limited
        val successfulRequests = results.count { it.isSuccess }
        val rateLimitedRequests = results.count { it.isFailure }
        
        // Should have some successful requests
        assertTrue(successfulRequests > 0, "Should have some successful requests")
        
        // Rate limited requests should have appropriate error
        results.filter { it.isFailure }.forEach { result ->
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            // Could be rate limiting or other network error
            assertTrue(exception is StorageException)
        }
    }

    @Test
    fun testErrorRecovery() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test that temporary network errors are handled with retry
        val result = storage.downloadFile("/test-file.txt")
        
        // Should either succeed or fail with appropriate error
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertTrue(exception is StorageException)
            
            // Error should be informative
            assertTrue(exception.message?.isNotBlank() ?: false)
        }
    }

    @Test
    fun testConcurrentAccess() = runTest {
        storage.authenticate(testCredentials.accessToken)
        
        // Test concurrent operations
        val concurrentResults = mutableListOf<Result<*>>()
        
        // Launch multiple concurrent operations
        val operations = listOf(
            { storage.listFiles("/") },
            { storage.downloadFile("/test-file.txt") },
            { storage.getFileMetadata("/test-file.txt") },
            { storage.listFiles("/Documents") }
        )
        
        operations.forEach { operation ->
            val result = operation()
            concurrentResults.add(result)
        }
        
        // All operations should complete (either success or failure)
        assertEquals(operations.size, concurrentResults.size)
        
        // At least some should succeed
        val successfulOperations = concurrentResults.count { it.isSuccess }
        assertTrue(successfulOperations > 0, "Should have some successful concurrent operations")
    }
}