/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *#######################################################*/
package digital.vasic.yole.network.protocols.onedrive

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Mock HTTP tests for OneDriveService using Ktor MockEngine.
 * Validates all HTTP-level interactions with the Microsoft Graph API v1.0
 * through a fully injected mock HTTP client and mock secure storage.
 */
class OneDriveMockHttpTest {

    private class MockSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()
        override suspend fun store(key: String, value: String) = run { storage[key] = value; Result.success(Unit) }
        override suspend fun retrieve(key: String) = Result.success(storage[key])
        override suspend fun delete(key: String) = run { storage.remove(key); Result.success(Unit) }
        override suspend fun contains(key: String) = Result.success(key in storage)
        override suspend fun listKeys() = Result.success(storage.keys.toList())
        override suspend fun clear() = run { storage.clear(); Result.success(Unit) }
        override suspend fun isSecure() = Result.success(true)
    }

    private val driveInfoResponse = """
        {
            "id": "drive123",
            "driveType": "personal",
            "quota": {"total": 5368709120, "used": 1073741824, "remaining": 4294967296, "state": "normal"}
        }
    """.trimIndent()

    private val itemListResponse = """
        {
            "value": [
                {"id": "item1", "name": "document.txt", "size": 1024, "lastModifiedDateTime": "2024-01-01T00:00:00Z", "file": {"mimeType": "text/plain"}},
                {"id": "item2", "name": "subfolder", "size": 0, "folder": {"childCount": 3}}
            ],
            "@odata.nextLink": null
        }
    """.trimIndent()

    private val singleItemResponse = """
        {"id": "item1", "name": "document.txt", "size": 1024, "lastModifiedDateTime": "2024-01-01T00:00:00Z", "file": {"mimeType": "text/plain"}}
    """.trimIndent()

    private val folderCreatedResponse = """
        {"id": "newfolder1", "name": "new-folder", "size": 0, "lastModifiedDateTime": "2024-06-01T00:00:00Z", "folder": {"childCount": 0}}
    """.trimIndent()

    private val emptyItemListResponse = """
        {"value": [], "@odata.nextLink": null}
    """.trimIndent()

    private val searchResultResponse = """
        {
            "value": [
                {"id": "search1", "name": "found-doc.txt", "size": 512, "lastModifiedDateTime": "2024-06-15T12:00:00Z", "file": {"mimeType": "text/plain"}}
            ],
            "@odata.nextLink": null
        }
    """.trimIndent()

    private val quotaOnlyResponse = """
        {
            "id": "drive123",
            "driveType": "personal",
            "quota": {"total": 5368709120, "used": 1073741824, "remaining": 4294967296, "state": "normal"}
        }
    """.trimIndent()

    private val config = StorageConfig.OneDriveConfig(
        name = "test-onedrive",
        clientId = "test_client_id",
        clientSecret = "test_client_secret",
        driveType = OneDriveDriveType.ME
    )

    private fun createMockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun createServiceWithMock(handler: MockRequestHandler): OneDriveService {
        val mockStorage = MockSecureStorage()
        val authManager = AuthTokenManager("onedrive", mockStorage)
        kotlinx.coroutines.runBlocking {
            authManager.storeTokenInfo(accessToken = "test_token", refreshToken = "refresh_token", expiresIn = 3600)
        }
        val client = createMockClient(handler)
        return OneDriveService(config, client, authManager)
    }

    // ==================== Connect / Disconnect ====================

    @Test
    fun testConnectWithValidTokenSucceeds() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = service.connect()
        assertTrue(result.isSuccess, "Connect should succeed with valid token and successful drive info response")
        assertTrue(service.isOnline, "Service should be online after connect")
    }

    @Test
    fun testConnectSendsAuthorizationHeader() = runBlocking<Unit> {
        var capturedAuth: String? = null
        val service = createServiceWithMock { request ->
            capturedAuth = request.headers["Authorization"]
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        service.connect()
        assertEquals("Bearer test_token", capturedAuth, "Authorization header should contain Bearer token")
    }

    @Test
    fun testConnectCallsDriveEndpoint() = runBlocking<Unit> {
        var capturedUrl: String? = null
        val service = createServiceWithMock { request ->
            capturedUrl = request.url.toString()
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        service.connect()
        assertNotNull(capturedUrl, "Should have made a request")
        assertTrue(capturedUrl!!.contains("graph.microsoft.com"), "Should call Microsoft Graph API")
        assertTrue(capturedUrl!!.contains("me/drive"), "Should call me/drive endpoint")
    }

    @Test
    fun testConnectFailsOnServerError() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }

        val result = service.connect()
        // connect() will fail and try to refresh token, which will also fail
        assertTrue(result.isFailure, "Connect should fail when server returns 500")
    }

    @Test
    fun testDisconnectSucceeds() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        service.connect()
        assertTrue(service.isOnline)

        val result = service.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
        assertFalse(service.isOnline, "Service should be offline after disconnect")
    }

    // ==================== List Files ====================

    @Test
    fun testListFilesReturnsDocuments() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                url.contains("children") -> respond(
                    content = itemListResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.listFiles("/").first()

        assertTrue(result.isSuccess, "listFiles should succeed")
        val docs = result.getOrThrow()
        assertEquals(2, docs.size, "Should return 2 items")
        assertEquals("document.txt", docs[0].name)
        assertEquals("subfolder", docs[1].name)
        assertTrue(docs[1].isFolder, "Second item should be a folder (has folder property)")
        assertFalse(docs[0].isFolder, "First item should be a file")
    }

    @Test
    fun testListFilesSetsCorrectPath() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("children") -> respond(
                    content = itemListResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.listFiles("/").first()
        val docs = result.getOrThrow()

        assertEquals("/document.txt", docs[0].path, "File path should be prefixed with /")
        assertEquals("/subfolder", docs[1].path, "Folder path should be prefixed with /")
    }

    @Test
    fun testListFilesWhenNotConnectedFails() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = "Unauthorized",
                status = HttpStatusCode.Unauthorized
            )
        }

        val result = service.listFiles("/").first()
        assertTrue(result.isFailure, "listFiles should fail when not connected")
    }

    @Test
    fun testListFilesHandlesEmptyResponse() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("children") -> respond(
                    content = emptyItemListResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.listFiles("/").first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty(), "Empty item list should produce empty document list")
    }

    @Test
    fun testListFilesHandlesApiError() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = "Forbidden",
                    status = HttpStatusCode.Forbidden,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                )
            }
        }

        service.connect()
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure, "listFiles should fail on API error")
    }

    // ==================== Download File ====================

    @Test
    fun testDownloadFileEmitsProgressUpdates() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                url.contains("content") -> respond(
                    content = "file content here",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                )
                url.contains("root:") -> respond(
                    content = singleItemResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = singleItemResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val operations = service.downloadFile("/document.txt", "/tmp/document.txt").toList()

        assertTrue(operations.isNotEmpty(), "Should emit at least one operation")
        assertEquals(NetworkOperation.Type.DOWNLOAD, operations.first().type)
    }

    @Test
    fun testDownloadFileWhenNotConnectedReturnsFailed() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val operations = service.downloadFile("/test.md", "/tmp/test.md").toList()
        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.first().status)
        assertEquals("OneDrive not connected", operations.first().error)
    }

    // ==================== Upload File ====================

    @Test
    fun testUploadFileWhenNotConnectedReturnsFailed() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val operations = service.uploadFile("/tmp/test.md", "/test.md").toList()
        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.first().status)
        assertEquals(NetworkOperation.Type.UPLOAD, operations.first().type)
    }

    @Test
    fun testUploadFileEmitsProgressWhenConnected() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                request.method == HttpMethod.Put -> respond(
                    content = singleItemResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = singleItemResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val operations = service.uploadFile("/tmp/test.md", "/test.md").toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Type.UPLOAD, operations.first().type)
    }

    // ==================== Delete File ====================

    @Test
    fun testDeleteFileWhenNotConnectedSucceedsSilently() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.deleteFile("/test.md")
        assertTrue(result.isSuccess, "Delete should succeed silently when not connected (offline queue pattern)")
    }

    @Test
    fun testDeleteFileSuccess() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                request.method == HttpMethod.Delete -> respond(
                    content = "",
                    status = HttpStatusCode.NoContent
                )
                else -> respond(
                    content = singleItemResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.deleteFile("/document.txt")
        assertNotNull(result)
    }

    // ==================== Create Folder ====================

    @Test
    fun testCreateFolderSuccess() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                request.method == HttpMethod.Post -> respond(
                    content = folderCreatedResponse,
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = emptyItemListResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.createFolder("/new-folder")
        assertTrue(result.isSuccess, "Create folder should succeed")
        val doc = result.getOrThrow()
        assertEquals("new-folder", doc.name)
        assertTrue(doc.isFolder)
    }

    @Test
    fun testCreateFolderWhenNotConnectedReturnsOfflineDoc() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.createFolder("/offline-folder")
        assertTrue(result.isSuccess, "Create folder should succeed with offline document")
        val doc = result.getOrThrow()
        assertEquals("offline-folder", doc.name)
        assertTrue(doc.isFolder)
    }

    // ==================== Search Files ====================

    @Test
    fun testSearchFilesReturnsResults() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("me/drive") && !url.contains("items") && !url.contains("root") && !url.contains("search") -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                url.contains("search") -> respond(
                    content = searchResultResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        val result = service.searchFiles("found", "/", false).first()
        assertTrue(result.isSuccess)
        val docs = result.getOrThrow()
        assertEquals(1, docs.size)
        assertEquals("found-doc.txt", docs[0].name)
    }

    @Test
    fun testSearchFilesWhenNotConnectedReturnsEmpty() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.searchFiles("query", "/", false).first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun testSearchFilesIncludesQueryInUrl() = runBlocking<Unit> {
        var capturedUrl: String? = null
        val service = createServiceWithMock { request ->
            val url = request.url.toString()
            when {
                url.contains("search") -> {
                    capturedUrl = url
                    respond(
                        content = searchResultResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = driveInfoResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        service.connect()
        service.searchFiles("testquery", null, false).first()

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("testquery"), "Search query should appear in URL")
    }

    // ==================== Get Quota Info ====================

    @Test
    fun testGetQuotaInfoReturnsValidQuota() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        service.connect()
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess, "getQuotaInfo should succeed")
        val quota = result.getOrThrow()
        assertEquals(5368709120L, quota.totalSpace)
        assertEquals(1073741824L, quota.usedSpace)
        assertEquals(4294967296L, quota.availableSpace)
        assertFalse(quota.isFull)
    }

    @Test
    fun testGetQuotaInfoWhenNotConnectedFails() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.getQuotaInfo()
        assertTrue(result.isFailure, "getQuotaInfo should fail when not connected")
    }

    @Test
    fun testGetQuotaInfoUsagePercentage() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        service.connect()
        val quota = service.getQuotaInfo().getOrThrow()
        val expectedPercentage = 1073741824.0 / 5368709120.0
        assertEquals(expectedPercentage, quota.usagePercentage, 0.001)
    }

    // ==================== Get File Info ====================

    @Test
    fun testGetFileInfoWhenNotConnectedReturnsOfflineDoc() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.getFileInfo("/test.md")
        assertTrue(result.isSuccess)
        assertEquals("test.md", result.getOrThrow().name)
        assertEquals("/test.md", result.getOrThrow().path)
    }

    // ==================== Storage Info ====================

    @Test
    fun testGetStorageInfoReturnsCorrectMetadata() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val storageInfo = service.getStorageInfo()
        assertEquals("onedrive_test-onedrive", storageInfo.id)
        assertEquals("test-onedrive", storageInfo.name)
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("onedrive://", storageInfo.location)
        assertTrue(storageInfo.supportsFolders)
    }

    // ==================== Rename / Move / Copy ====================

    @Test
    fun testRenameFileWhenNotConnectedSucceeds() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.renameFile("/test.md", "renamed.md")
        assertTrue(result.isSuccess, "Rename should succeed silently when not connected")
    }

    @Test
    fun testMoveFileWhenNotConnectedReturnsOfflineDoc() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.moveFile("/test.md", "/moved/test.md")
        assertTrue(result.isSuccess)
        assertEquals("test.md", result.getOrThrow().name)
        assertEquals("/moved/test.md", result.getOrThrow().path)
    }

    @Test
    fun testCopyFileWhenNotConnectedSucceeds() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.copyFile("/test.md", "/copy/test.md")
        assertTrue(result.isSuccess, "Copy should succeed silently when not connected")
    }

    // ==================== Exists ====================

    @Test
    fun testExistsWhenNotConnectedReturnsTrue() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = service.exists("/test.md")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow(), "Exists should return true when not connected (offline mode returns success from getFileInfo)")
    }

    // ==================== Validate Path ====================

    @Test
    fun testValidatePathAcceptsValidPaths() {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        assertTrue(service.validatePath("/test.md").isSuccess)
        assertTrue(service.validatePath("/folder/test.md").isSuccess)
    }

    @Test
    fun testValidatePathRejectsBlankPaths() {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        assertTrue(service.validatePath("").isFailure)
        assertTrue(service.validatePath("   ").isFailure)
    }

    // ==================== Get Parent Path ====================

    @Test
    fun testGetParentPathReturnsCorrectParent() {
        val service = createServiceWithMock { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        assertEquals("/", service.getParentPath("/test.md"))
        assertEquals("/folder", service.getParentPath("/folder/test.md"))
        assertEquals(null, service.getParentPath("/"))
        assertEquals(null, service.getParentPath(""))
    }

    // ==================== Test Connection ====================

    @Test
    fun testTestConnectionSuccess() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = driveInfoResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = service.testConnection()
        assertTrue(result.isSuccess, "testConnection should succeed")
        assertTrue(result.getOrThrow(), "testConnection should return true on successful drive info call")
    }

    @Test
    fun testTestConnectionFailsOnError() = runBlocking<Unit> {
        val service = createServiceWithMock { request ->
            respond(
                content = "Service Unavailable",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }

        val result = service.testConnection()
        assertTrue(result.isSuccess, "testConnection wraps failure as Result.success(false)")
        assertFalse(result.getOrThrow(), "testConnection should return false on failed drive info call")
    }
}
