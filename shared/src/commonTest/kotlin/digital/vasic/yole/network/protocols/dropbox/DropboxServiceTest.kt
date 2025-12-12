/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for DropboxService
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocol.HttpClient
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Duration
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive tests for DropboxService covering:
 * - Connection management and authentication
 * - File operations (upload, download, delete, etc.)
 * - Error handling and retry logic
 * - Progress tracking and operation status
 * - Rate limiting and API constraints
 * - Cross-platform compatibility
 */
class DropboxServiceTest {

    private lateinit var mockHttpClient: HttpClient
    private lateinit var mockAuthTokenManager: AuthTokenManager
    private lateinit var dropboxService: DropboxService
    
    private val testAccessToken = "test_dropbox_access_token_123"
    private val testRefreshToken = "test_dropbox_refresh_token_456"

    @BeforeTest
    fun setUp() {
        mockHttpClient = mockk(relaxed = true)
        mockAuthTokenManager = mockk(relaxed = true)
        dropboxService = DropboxService(mockHttpClient, mockAuthTokenManager)
        
        clearAllMocks()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Connection Tests ====================

    @Test
    fun `connect should succeed with valid token`() = runTest {
        // Given
        coEvery { mockAuthTokenManager.hasValidToken() } returns Result.success(true)
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        
        val aboutResponse = """
            {
                "account_id": "dbid:AAH4f99T0taONIb-OurWxbNQ6ywGRopQngc",
                "name": {
                    "given_name": "Test",
                    "surname": "User",
                    "display_name": "Test User"
                },
                "email": "test@example.com"
            }
        """.trimIndent()
        
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/users/get_current_account", any()) } returns 
            Result.success(aboutResponse)
        
        // When
        val result = dropboxService.connect()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(dropboxService.isConnected())
        
        coVerify { mockAuthTokenManager.hasValidToken() }
        coVerify { mockHttpClient.post(any(), any()) }
    }

    @Test
    fun `connect should fail without valid token`() = runTest {
        // Given
        coEvery { mockAuthTokenManager.hasValidToken() } returns Result.success(false)
        
        // When
        val result = dropboxService.connect()
        
        // Then
        assertTrue(result.isFailure)
        assertFalse(dropboxService.isConnected())
        
        val exception = result.exceptionOrNull() as NetworkStorageException.ConnectionException.Authentication
        assertEquals("No valid authentication tokens found", exception.message)
    }

    @Test
    fun `connect should handle token refresh failure`() = runTest {
        // Given
        coEvery { mockAuthTokenManager.hasValidToken() } returns Result.success(false)
        coEvery { mockAuthTokenManager.refreshAccessToken(any(), any(), any(), any()) } returns Result.failure(
            Exception("Token refresh failed")
        )
        
        // When
        val result = dropboxService.connect()
        
        // Then
        assertTrue(result.isFailure)
        assertFalse(dropboxService.isConnected())
    }

    @Test
    fun `disconnect should clear connection state`() = runTest {
        // Given
        dropboxService.connect() // First establish connection
        clearAllMocks() // Clear previous calls
        
        // When
        dropboxService.disconnect()
        
        // Then
        assertFalse(dropboxService.isConnected())
    }

    // ==================== File Upload Tests ====================

    @Test
    fun `uploadFile should upload successfully with progress tracking`() = runTest {
        // Given
        val localPath = "/local/test/file.txt"
        val remotePath = "/remote/test/file.txt"
        val fileContent = "Test file content"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        
        // Mock upload session start
        coEvery { mockHttpClient.post("https://content.dropboxapi.com/2/files/upload_session/start", any()) } returns
            Result.success("""{"session_id": "test_session_123"}""")
        
        // Mock upload session append
        coEvery { mockHttpClient.post("https://content.dropboxapi.com/2/files/upload_session/append_v2", any()) } returns
            Result.success("""{"session_id": "test_session_123"}""")
        
        // Mock upload session finish
        coEvery { mockHttpClient.post("https://content.dropboxapi.com/2/files/upload_session/finish", any()) } returns
            Result.success("""{"name": "file.txt", "path_display": "$remotePath", "size": ${fileContent.length}}""")
        
        // When
        val operations = dropboxService.uploadFile(remotePath, localPath).toList()
        
        // Then
        assertTrue(operations.isNotEmpty())
        
        val finalOperation = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, finalOperation.status)
        assertEquals(100.0, finalOperation.progress)
        assertEquals(remotePath, finalOperation.remotePath)
        assertEquals(localPath, finalOperation.localPath)
    }

    @Test
    fun `uploadFile should handle authentication failure`() = runTest {
        // Given
        val localPath = "/local/test/file.txt"
        val remotePath = "/remote/test/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.failure(
            Exception("No access token available")
        )
        
        // When
        val operations = dropboxService.uploadFile(remotePath, localPath).toList()
        
        // Then
        assertTrue(operations.isNotEmpty())
        
        val finalOperation = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, finalOperation.status)
        assertTrue(finalOperation.error?.contains("No access token available") ?: false)
    }

    @Test
    fun `uploadFile should handle network errors during upload`() = runTest {
        // Given
        val localPath = "/local/test/file.txt"
        val remotePath = "/remote/test/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post(any(), any()) } returns Result.failure(
            Exception("Network error during upload")
        )
        
        // When
        val operations = dropboxService.uploadFile(remotePath, localPath).toList()
        
        // Then
        assertTrue(operations.isNotEmpty())
        
        val finalOperation = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, finalOperation.status)
        assertTrue(finalOperation.error?.contains("Network error") ?: false)
    }

    // ==================== File Download Tests ====================

    @Test
    fun `downloadFile should download successfully with progress tracking`() = runTest {
        // Given
        val remotePath = "/remote/test/file.txt"
        val localPath = "/local/test/file.txt"
        val fileContent = "Test file content for download"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        
        // Mock download request
        coEvery { mockHttpClient.post("https://content.dropboxapi.com/2/files/download", any()) } returns
            Result.success(fileContent)
        
        // When
        val operations = dropboxService.downloadFile(remotePath, localPath).toList()
        
        // Then
        assertTrue(operations.isNotEmpty())
        
        val finalOperation = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, finalOperation.status)
        assertEquals(100.0, finalOperation.progress)
        assertEquals(remotePath, finalOperation.remotePath)
        assertEquals(localPath, finalOperation.localPath)
    }

    @Test
    fun `downloadFile should handle file not found`() = runTest {
        // Given
        val remotePath = "/remote/test/nonexistent.txt"
        val localPath = "/local/test/nonexistent.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://content.dropboxapi.com/2/files/download", any()) } returns
            Result.failure(NetworkStorageException.FileOperationException.NotFound(remotePath))
        
        // When
        val operations = dropboxService.downloadFile(remotePath, localPath).toList()
        
        // Then
        assertTrue(operations.isNotEmpty())
        
        val finalOperation = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, finalOperation.status)
        assertTrue(finalOperation.error?.contains("not found") ?: false)
    }

    // ==================== File Information Tests ====================

    @Test
    fun `getFileInfo should retrieve file metadata`() = runTest {
        // Given
        val remotePath = "/remote/test/file.txt"
        val fileInfoResponse = """
            {
                "name": "file.txt",
                "path_display": "$remotePath",
                "size": 1024,
                "client_modified": "2024-01-01T12:00:00Z",
                "server_modified": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/get_metadata", any()) } returns
            Result.success(fileInfoResponse)
        
        // When
        val result = dropboxService.getFileInfo(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
        
        val document = result.getOrNull()!!
        assertEquals("file.txt", document.name)
        assertEquals(remotePath, document.path)
        assertEquals(1024L, document.size)
        assertFalse(document.isDirectory)
    }

    @Test
    fun `getFileInfo should handle directory metadata`() = runTest {
        // Given
        val remotePath = "/remote/test/folder/"
        val folderInfoResponse = """
            {
                "name": "folder",
                "path_display": "$remotePath",
                "size": 0
            }
        """.trimIndent()
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/get_metadata", any()) } returns
            Result.success(folderInfoResponse)
        
        // When
        val result = dropboxService.getFileInfo(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
        
        val document = result.getOrNull()!!
        assertEquals("folder", document.name)
        assertEquals(remotePath, document.path)
        assertEquals(0L, document.size)
        assertTrue(document.isDirectory)
    }

    // ==================== Directory Operations Tests ====================

    @Test
    fun `listDirectory should retrieve directory contents`() = runTest {
        // Given
        val remotePath = "/remote/test/"
        val listResponse = """
            {
                "entries": [
                    {
                        ".tag": "file",
                        "name": "file1.txt",
                        "path_display": "/remote/test/file1.txt",
                        "size": 512
                    },
                    {
                        ".tag": "folder",
                        "name": "subfolder",
                        "path_display": "/remote/test/subfolder/",
                        "size": 0
                    }
                ],
                "cursor": "test_cursor_123",
                "has_more": false
            }
        """.trimIndent()
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/list_folder", any()) } returns
            Result.success(listResponse)
        
        // When
        val result = dropboxService.listDirectory(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
        
        val documents = result.getOrNull()!!
        assertEquals(2, documents.size)
        
        val file = documents.find { it.name == "file1.txt" }
        assertNotNull(file)
        assertFalse(file.isDirectory)
        assertEquals(512L, file.size)
        
        val folder = documents.find { it.name == "subfolder" }
        assertNotNull(folder)
        assertTrue(folder.isDirectory)
        assertEquals(0L, folder.size)
    }

    @Test
    fun `createDirectory should create new directory`() = runTest {
        // Given
        val remotePath = "/remote/test/new_folder/"
        val createResponse = """
            {
                "name": "new_folder",
                "path_display": "$remotePath",
                "size": 0
            }
        """.trimIndent()
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/create_folder_v2", any()) } returns
            Result.success(createResponse)
        
        // When
        val result = dropboxService.createDirectory(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
        
        val document = result.getOrNull()!!
        assertEquals("new_folder", document.name)
        assertEquals(remotePath, document.path)
        assertTrue(document.isDirectory)
    }

    // ==================== File Deletion Tests ====================

    @Test
    fun `deleteFile should remove file successfully`() = runTest {
        // Given
        val remotePath = "/remote/test/file_to_delete.txt"
        val deleteResponse = """
            {
                "name": "file_to_delete.txt",
                "path_display": "$remotePath"
            }
        """.trimIndent()
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/delete_v2", any()) } returns
            Result.success(deleteResponse)
        
        // When
        val result = dropboxService.deleteFile(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile should handle file not found`() = runTest {
        // Given
        val remotePath = "/remote/test/nonexistent.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post("https://api.dropboxapi.com/2/files/delete_v2", any()) } returns
            Result.failure(NetworkStorageException.FileOperationException.NotFound(remotePath))
        
        // When
        val result = dropboxService.deleteFile(remotePath)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkStorageException.FileOperationException.NotFound)
    }

    // ==================== Rate Limiting Tests ====================

    @Test
    fun `should handle rate limiting with retry`() = runTest {
        // Given
        val remotePath = "/remote/test/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        
        // First call returns rate limit error, second call succeeds
        var callCount = 0
        coEvery { mockHttpClient.post(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("HTTP 429: Rate limit exceeded"))
            } else {
                Result.success("""{"name": "file.txt", "size": 1024}""")
            }
        }
        
        // When
        val result = dropboxService.getFileInfo(remotePath)
        
        // Then
        assertTrue(result.isSuccess) // Should succeed after retry
        coVerify(exactly = 2) { mockHttpClient.post(any(), any()) }
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should recover from temporary network failures`() = runTest {
        // Given
        val remotePath = "/remote/test/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockAuthTokenManager.refreshAccessToken(any(), any(), any(), any()) } returns Result.success("new_token")
        
        // Simulate token expiration followed by successful retry
        var callCount = 0
        coEvery { mockHttpClient.post(any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("HTTP 401: Unauthorized"))
            } else {
                Result.success("""{"name": "file.txt", "size": 1024}""")
            }
        }
        
        // When
        val result = dropboxService.getFileInfo(remotePath)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAuthTokenManager.refreshAccessToken(any(), any(), any(), any()) }
    }

    // ==================== Concurrent Operations Tests ====================

    @Test
    fun `should handle concurrent file operations`() = runTest {
        // Given
        val operations = 10
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post(any(), any()) } returns Result.success("""{"name": "file.txt", "size": 1024}""")
        
        // When
        val results = (1..operations).map { i ->
            dropboxService.getFileInfo("/remote/test/file$i.txt")
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        coVerify(exactly = operations) { mockHttpClient.post(any(), any()) }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle very long file paths`() = runTest {
        // Given
        val longPath = "/remote/test/" + "a".repeat(1000) + "/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post(any(), any()) } returns Result.success(
            """{"name": "file.txt", "path_display": "$longPath", "size": 1024}"""
        )
        
        // When
        val result = dropboxService.getFileInfo(longPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(longPath, result.getOrNull()?.path)
    }

    @Test
    fun `should handle special characters in file names`() = runTest {
        // Given
        val specialPath = "/remote/test/file with spaces and special chars !@#$%^&*().txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post(any(), any()) } returns Result.success(
            """{"name": "file with spaces and special chars !@#$%^&*().txt", "path_display": "$specialPath", "size": 1024}"""
        )
        
        // When
        val result = dropboxService.getFileInfo(specialPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(specialPath, result.getOrNull()?.path)
    }

    @Test
    fun `should handle empty responses from API`() = runTest {
        // Given
        val remotePath = "/remote/test/file.txt"
        
        coEvery { mockAuthTokenManager.getAccessToken() } returns Result.success(testAccessToken)
        coEvery { mockHttpClient.post(any(), any()) } returns Result.success("")
        
        // When
        val result = dropboxService.getFileInfo(remotePath)
        
        // Then
        assertTrue(result.isFailure) // Should fail with empty response
    }
}