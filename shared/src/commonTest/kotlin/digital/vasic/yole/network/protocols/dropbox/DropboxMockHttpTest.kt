/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive mock HTTP tests for DropboxService using
 * Ktor MockEngine to verify real Dropbox API v2 HTTP calls.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Comprehensive mock HTTP test suite for DropboxService.
 *
 * Uses Ktor [MockEngine] to intercept and verify all HTTP calls that
 * [DropboxService] makes against Dropbox API v2 endpoints. A
 * [MockSecureStorage] backs the [AuthTokenManager] with pre-populated
 * OAuth2 tokens so every test starts in a fully-authenticated state.
 *
 * Covers: connect, listFiles, downloadFile, uploadFile, deleteFile,
 * createFolder, moveFile, copyFile, renameFile, getFileInfo, searchFiles,
 * getQuotaInfo, exists, disconnect, testConnection, error responses,
 * and edge cases.
 */
class DropboxMockHttpTest {

    // ----- mock secure storage -----

    private class MockSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()
        override suspend fun store(key: String, value: String): Result<Unit> {
            storage[key] = value; return Result.success(Unit)
        }
        override suspend fun retrieve(key: String): Result<String?> = Result.success(storage[key])
        override suspend fun delete(key: String): Result<Unit> {
            storage.remove(key); return Result.success(Unit)
        }
        override suspend fun contains(key: String): Result<Boolean> = Result.success(key in storage)
        override suspend fun listKeys(): Result<List<String>> = Result.success(storage.keys.toList())
        override suspend fun clear(): Result<Unit> { storage.clear(); return Result.success(Unit) }
        override suspend fun isSecure(): Result<Boolean> = Result.success(true)
    }

    // ----- JSON response fixtures -----

    private val accountInfoJson = """
        {"account_id":"dbid:test123","name":{"given_name":"Test","surname":"User","familiar_name":"Test","display_name":"Test User","abbreviated_name":"TU"},"email":"test@example.com","email_verified":true,"disabled":false}
    """.trimIndent()

    private val listFolderJson = """
        {"entries":[{"tag":"file","name":"document.txt","id":"id:file1","pathLower":"/document.txt","pathDisplay":"/document.txt","size":1024,"serverModified":"2024-01-01T00:00:00Z","rev":"rev1"},{"tag":"folder","name":"subfolder","id":"id:folder1","pathLower":"/subfolder","pathDisplay":"/subfolder"}],"cursor":"cursor_abc","has_more":false}
    """.trimIndent()

    private val listFolderEmptyJson = """
        {"entries":[],"cursor":"cursor_empty","has_more":false}
    """.trimIndent()

    private val listFolderMultiJson = """
        {"entries":[{"tag":"file","name":"readme.md","id":"id:f1","pathLower":"/readme.md","pathDisplay":"/readme.md","size":512,"serverModified":"2024-02-01T12:00:00Z","rev":"r1"},{"tag":"file","name":"notes.txt","id":"id:f2","pathLower":"/notes.txt","pathDisplay":"/notes.txt","size":256,"serverModified":"2024-03-15T08:30:00Z","rev":"r2"},{"tag":"folder","name":"images","id":"id:d1","pathLower":"/images","pathDisplay":"/images"},{"tag":"file","name":"report.pdf","id":"id:f3","pathLower":"/report.pdf","pathDisplay":"/report.pdf","size":204800,"serverModified":"2024-06-20T16:45:00Z","rev":"r3"},{"tag":"folder","name":"archive","id":"id:d2","pathLower":"/archive","pathDisplay":"/archive"}],"cursor":"cursor_multi","has_more":false}
    """.trimIndent()

    private val metadataFileJson = """
        {".tag":"file","name":"document.txt","id":"id:file1","path_display":"/document.txt","size":2048,"server_modified":"2024-06-15T10:30:00Z"}
    """.trimIndent()

    private val metadataFolderJson = """
        {".tag":"folder","name":"subfolder","id":"id:folder1","path_display":"/subfolder"}
    """.trimIndent()

    private val createFolderJson = """
        {"metadata":{".tag":"folder","name":"new-folder","id":"id:newfolder","path_display":"/new-folder"}}
    """.trimIndent()

    private val moveJson = """
        {"metadata":{".tag":"file","name":"moved.txt","id":"id:moved","path_display":"/dest/moved.txt","size":1024,"server_modified":"2024-01-02T00:00:00Z"}}
    """.trimIndent()

    private val spaceUsageJson = """
        {"used":5368709120,"allocation":{"allocated":10737418240}}
    """.trimIndent()

    private val searchJson = """
        {"matches":[{"metadata":{"metadata":{".tag":"file","name":"found.txt","id":"id:found1","path_display":"/found.txt","size":512}}},{"metadata":{"metadata":{".tag":"file","name":"found2.txt","id":"id:found2","path_display":"/deep/found2.txt","size":1024}}}],"has_more":false}
    """.trimIndent()

    private val uploadJson = """
        {"name":"uploaded.txt","id":"id:uploaded","path_lower":"/uploaded.txt","path_display":"/uploaded.txt","size":42,"server_modified":"2024-07-01T10:00:00Z","rev":"revUp1"}
    """.trimIndent()

    // ----- factory helpers -----

    private fun testConfig() = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "cfg_token",
        appKey = "test_app_key",
        appSecret = "test_app_secret",
        rootPath = ""
    )

    private suspend fun preparedAuthManager(): AuthTokenManager {
        val storage = MockSecureStorage()
        val manager = AuthTokenManager("dropbox", storage)
        manager.storeTokenInfo(
            accessToken = "test_access_token_123",
            refreshToken = "test_refresh_token_456",
            expiresIn = 3600L
        )
        return manager
    }

    private fun mockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private suspend fun connectedService(handler: MockRequestHandler): DropboxService {
        val auth = preparedAuthManager()
        val client = mockClient(handler)
        return DropboxService(testConfig(), client, auth)
    }

    private suspend fun offlineService(): DropboxService {
        val emptyStorage = MockSecureStorage()
        val auth = AuthTokenManager("dropbox", emptyStorage)
        val client = mockClient { respond("", HttpStatusCode.OK) }
        return DropboxService(testConfig(), client, auth)
    }

    /** Standard routing: routes get_current_account to account info, everything else to [other]. */
    private fun routeWithAccount(other: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): MockRequestHandler = { request ->
        if (request.url.encodedPath.contains("get_current_account")) {
            respond(accountInfoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        } else {
            other(request)
        }
    }

    // ==================== 1-5: Connect ====================

    @Test
    fun `01 connect succeeds with valid token and account info`() = runTest {
        val service = connectedService(routeWithAccount {
            respond(accountInfoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val result = service.connect()
        assertTrue(result.isSuccess, "connect should succeed: ${result.exceptionOrNull()}")
        assertTrue(service.isOnline)
    }

    @Test
    fun `02 connect sends POST to get_current_account`() = runTest {
        var capturedUrl: String? = null
        var capturedMethod: HttpMethod? = null

        val service = connectedService { request ->
            capturedUrl = request.url.toString()
            capturedMethod = request.method
            respond(accountInfoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        service.connect()

        assertEquals(HttpMethod.Post, capturedMethod)
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("api.dropboxapi.com"))
        assertTrue(capturedUrl!!.contains("2/users/get_current_account"))
    }

    @Test
    fun `03 connect sends Bearer authorization header`() = runTest {
        var capturedAuth: String? = null
        val service = connectedService { request ->
            capturedAuth = request.headers["Authorization"]
            respond(accountInfoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        service.connect()
        assertNotNull(capturedAuth)
        assertEquals("Bearer test_access_token_123", capturedAuth)
    }

    @Test
    fun `04 connect fails without valid token`() = runTest {
        val service = offlineService()
        val result = service.connect()
        assertTrue(result.isFailure)
        assertFalse(service.isOnline)
    }

    @Test
    fun `05 connect handles server error on account info`() = runTest {
        val service = connectedService { request ->
            when {
                request.url.encodedPath.contains("get_current_account") ->
                    respond("Internal Server Error", HttpStatusCode.InternalServerError)
                else -> respond("", HttpStatusCode.BadRequest)
            }
        }
        val result = service.connect()
        assertTrue(result.isFailure, "connect should fail on server error")
    }

    // ==================== 6-14: List Files ====================

    @Test
    fun `06 listFiles returns files and folders`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder"))
                respond(listFolderJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        assertEquals(2, docs.size)
        val file = docs.first { !it.isFolder }
        assertEquals("document.txt", file.name)
        assertEquals(1024L, file.size)
        assertEquals("id:file1", file.id)
        val folder = docs.first { it.isFolder }
        assertEquals("subfolder", folder.name)
    }

    @Test
    fun `07 listFiles returns empty list for empty folder`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder"))
                respond(listFolderEmptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        assertEquals(0, docs.size)
    }

    @Test
    fun `08 listFiles returns multiple files and folders`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder"))
                respond(listFolderMultiJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        assertEquals(5, docs.size)
        assertEquals(3, docs.count { !it.isFolder })
        assertEquals(2, docs.count { it.isFolder })
        assertTrue(docs.any { it.name == "readme.md" })
        assertTrue(docs.any { it.name == "report.pdf" })
        assertTrue(docs.any { it.name == "images" })
    }

    @Test
    fun `09 listFiles fails when not connected`() = runTest {
        val service = offlineService()
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `10 listFiles sends correct content type`() = runTest {
        var capturedContentType: ContentType? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder")) {
                capturedContentType = request.body.contentType
                respond(listFolderEmptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.listFiles("/").first()
        assertNotNull(capturedContentType)
        assertEquals(ContentType.Application.Json, capturedContentType)
    }

    @Test
    fun `11 listFiles sends POST to list_folder endpoint`() = runTest {
        var capturedUrl: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder")) {
                capturedUrl = request.url.toString()
                respond(listFolderEmptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.listFiles("/").first()
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("2/files/list_folder"))
    }

    @Test
    fun `12 listFiles sends authorization header`() = runTest {
        var capturedAuth: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder")) {
                capturedAuth = request.headers["Authorization"]
                respond(listFolderEmptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.listFiles("/").first()
        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Bearer "))
    }

    @Test
    fun `13 listFiles handles API error response`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder"))
                respond("""{"error":"path/not_found"}""", HttpStatusCode.Conflict)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val result = service.listFiles("/nonexistent").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `14 listFiles sends path in request body`() = runTest {
        var capturedBody: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("list_folder")) {
                capturedBody = request.body.toByteArray().decodeToString()
                respond(listFolderEmptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.listFiles("/test-path").first()
        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("\"path\""))
    }

    // ==================== 15-19: Download File ====================

    @Test
    fun `15 downloadFile emits progress and COMPLETED`() = runTest {
        val fileBytes = "Hello Dropbox!".encodeToByteArray()
        val service = connectedService(routeWithAccount { request ->
            when {
                request.url.encodedPath.contains("get_metadata") ->
                    respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.host == "content.dropboxapi.com" ->
                    respond(fileBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/octet-stream"))
                else -> respond("", HttpStatusCode.OK)
            }
        })
        service.connect()

        val ops = service.downloadFile("/document.txt", "/local/document.txt").toList()
        assertTrue(ops.isNotEmpty())
        val completed = ops.last { it.status == NetworkOperation.Status.COMPLETED }
        assertEquals(1.0, completed.progress)
        assertEquals(fileBytes.size.toLong(), completed.bytesTransferred)
    }

    @Test
    fun `16 downloadFile emits FAILED when not connected`() = runTest {
        val service = offlineService()
        val ops = service.downloadFile("/doc.txt", "/local/doc.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, ops.last().status)
    }

    @Test
    fun `17 downloadFile uses content endpoint`() = runTest {
        var capturedHost: String? = null
        val service = connectedService(routeWithAccount { request ->
            when {
                request.url.encodedPath.contains("get_metadata") ->
                    respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("2/files/download") -> {
                    capturedHost = request.url.host
                    respond("data".encodeToByteArray(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/octet-stream"))
                }
                else -> respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        service.downloadFile("/test.txt", "/local/test.txt").toList()
        assertNotNull(capturedHost)
        assertEquals("content.dropboxapi.com", capturedHost)
    }

    @Test
    fun `18 downloadFile sends Dropbox-API-Arg header`() = runTest {
        var capturedApiArg: String? = null
        val service = connectedService(routeWithAccount { request ->
            when {
                request.url.encodedPath.contains("get_metadata") ->
                    respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("2/files/download") -> {
                    capturedApiArg = request.headers["Dropbox-API-Arg"]
                    respond("data".encodeToByteArray(), HttpStatusCode.OK)
                }
                else -> respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        service.downloadFile("/my-file.txt", "/local/my-file.txt").toList()
        assertNotNull(capturedApiArg)
        assertTrue(capturedApiArg!!.contains("path"))
    }

    @Test
    fun `19 downloadFile handles server error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            when {
                request.url.encodedPath.contains("get_metadata") ->
                    respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("2/files/download") ->
                    respond("", HttpStatusCode.InternalServerError)
                else -> respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        val ops = service.downloadFile("/test.txt", "/local/test.txt").toList()
        assertEquals(NetworkOperation.Status.FAILED, ops.last().status)
    }

    // ==================== 20-24: Upload File ====================

    @Test
    fun `20 uploadFile emits progress and COMPLETED`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("2/files/upload"))
                respond(uploadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond("", HttpStatusCode.OK)
        })
        service.connect()

        val ops = service.uploadFile("/local/file.txt", "/file.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, ops.last().status)
        assertEquals(1.0, ops.last().progress)
    }

    @Test
    fun `21 uploadFile emits FAILED when not connected`() = runTest {
        val service = offlineService()
        val ops = service.uploadFile("/local/file.txt", "/file.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, ops.last().status)
    }

    @Test
    fun `22 uploadFile uses content endpoint`() = runTest {
        var capturedHost: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("2/files/upload")) {
                capturedHost = request.url.host
                respond(uploadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        service.uploadFile("/local/file.txt", "/file.txt").toList()
        assertNotNull(capturedHost)
        assertEquals("content.dropboxapi.com", capturedHost)
    }

    @Test
    fun `23 uploadFile sends Dropbox-API-Arg header with mode`() = runTest {
        var capturedApiArg: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("2/files/upload")) {
                capturedApiArg = request.headers["Dropbox-API-Arg"]
                respond(uploadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        service.uploadFile("/local/up.txt", "/up.txt").toList()
        assertNotNull(capturedApiArg)
        assertTrue(capturedApiArg!!.contains("path"))
        assertTrue(capturedApiArg!!.contains("overwrite"))
    }

    @Test
    fun `24 uploadFile handles server error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("2/files/upload"))
                respond("", HttpStatusCode.InsufficientStorage)
            else
                respond("", HttpStatusCode.OK)
        })
        service.connect()
        val ops = service.uploadFile("/local/file.txt", "/file.txt").toList()
        assertEquals(NetworkOperation.Status.FAILED, ops.last().status)
    }

    // ==================== 25-28: Delete File ====================

    @Test
    fun `25 deleteFile succeeds with OK response`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("delete_v2"))
                respond("""{"metadata":{".tag":"file","name":"file.txt"}}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.deleteFile("/file.txt").isSuccess)
    }

    @Test
    fun `26 deleteFile succeeds silently when offline`() = runTest {
        val service = offlineService()
        assertTrue(service.deleteFile("/file.txt").isSuccess)
    }

    @Test
    fun `27 deleteFile posts to delete_v2 endpoint`() = runTest {
        var capturedUrl: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("delete_v2")) {
                capturedUrl = request.url.toString()
                respond("""{"metadata":{".tag":"file","name":"file.txt"}}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.deleteFile("/test.txt")
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("2/files/delete_v2"))
    }

    @Test
    fun `28 deleteFile handles server error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("delete_v2"))
                respond("", HttpStatusCode.InternalServerError)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.deleteFile("/file.txt").isFailure)
    }

    // ==================== 29-31: Create Folder ====================

    @Test
    fun `29 createFolder succeeds and returns folder doc`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("create_folder_v2"))
                respond(createFolderJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()

        val result = service.createFolder("/new-folder")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertTrue(doc.isFolder)
        assertEquals("new-folder", doc.name)
    }

    @Test
    fun `30 createFolder returns offline stub when not connected`() = runTest {
        val service = offlineService()
        val result = service.createFolder("/offline-folder")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFolder)
    }

    @Test
    fun `31 createFolder handles conflict error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("create_folder_v2"))
                respond("""{"error":"path/conflict"}""", HttpStatusCode.Conflict)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.createFolder("/existing").isFailure)
    }

    // ==================== 32-34: Move File ====================

    @Test
    fun `32 moveFile succeeds and returns doc`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("move_v2"))
                respond(moveJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val result = service.moveFile("/src.txt", "/dest/moved.txt")
        assertTrue(result.isSuccess)
        assertEquals("moved.txt", result.getOrThrow().name)
    }

    @Test
    fun `33 moveFile returns offline stub when not connected`() = runTest {
        val service = offlineService()
        val result = service.moveFile("/src.txt", "/dst.txt")
        assertTrue(result.isSuccess)
        assertEquals("dst.txt", result.getOrThrow().name)
    }

    @Test
    fun `34 moveFile handles server error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("move_v2"))
                respond("", HttpStatusCode.InternalServerError)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.moveFile("/src.txt", "/dst.txt").isFailure)
    }

    // ==================== 35-37: Copy File ====================

    @Test
    fun `35 copyFile succeeds with OK response`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("copy_v2"))
                respond("""{"metadata":{".tag":"file","name":"copy.txt"}}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.copyFile("/src.txt", "/copy.txt").isSuccess)
    }

    @Test
    fun `36 copyFile succeeds silently when offline`() = runTest {
        val service = offlineService()
        assertTrue(service.copyFile("/src.txt", "/copy.txt").isSuccess)
    }

    @Test
    fun `37 copyFile handles forbidden error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("copy_v2"))
                respond("", HttpStatusCode.Forbidden)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.copyFile("/src.txt", "/dst.txt").isFailure)
    }

    // ==================== 38-40: Rename File ====================

    @Test
    fun `38 renameFile sends POST to move_v2`() = runTest {
        var capturedUrl: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("move_v2")) {
                capturedUrl = request.url.toString()
                respond(moveJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        assertTrue(service.renameFile("/old.txt", "new.txt").isSuccess)
        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("2/files/move_v2"))
    }

    @Test
    fun `39 renameFile succeeds silently when offline`() = runTest {
        val service = offlineService()
        assertTrue(service.renameFile("/old.txt", "new.txt").isSuccess)
    }

    @Test
    fun `40 renameFile sends from_path and to_path`() = runTest {
        var capturedBody: String? = null
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("move_v2")) {
                capturedBody = request.body.toByteArray().decodeToString()
                respond(moveJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        service.connect()
        service.renameFile("/dir/old.txt", "new.txt")
        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("from_path"))
        assertTrue(capturedBody!!.contains("to_path"))
    }

    // ==================== 41-43: Get File Info ====================

    @Test
    fun `41 getFileInfo returns file metadata`() = runTest {
        val service = connectedService(routeWithAccount {
            respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val result = service.getFileInfo("/document.txt")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("document.txt", doc.name)
        assertEquals(2048L, doc.size)
        assertFalse(doc.isFolder)
    }

    @Test
    fun `42 getFileInfo returns folder metadata`() = runTest {
        val service = connectedService(routeWithAccount {
            respond(metadataFolderJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val doc = service.getFileInfo("/subfolder").getOrThrow()
        assertTrue(doc.isFolder)
        assertEquals("subfolder", doc.name)
    }

    @Test
    fun `43 getFileInfo handles not found`() = runTest {
        val service = connectedService(routeWithAccount {
            respond("""{"error":"path/not_found"}""", HttpStatusCode.Conflict)
        })
        service.connect()
        assertTrue(service.getFileInfo("/nonexistent.txt").isFailure)
    }

    // ==================== 44-46: Search Files ====================

    @Test
    fun `44 searchFiles returns matching results`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("search_v2"))
                respond(searchJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val docs = service.searchFiles("found").first().getOrThrow()
        assertEquals(2, docs.size)
        assertTrue(docs.any { it.name == "found.txt" })
        assertTrue(docs.any { it.name == "found2.txt" })
    }

    @Test
    fun `45 searchFiles returns empty list when not connected`() = runTest {
        val service = offlineService()
        val docs = service.searchFiles("anything").first().getOrThrow()
        assertEquals(0, docs.size)
    }

    @Test
    fun `46 searchFiles handles rate limit error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("search_v2"))
                respond("", HttpStatusCode.TooManyRequests)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.searchFiles("test").first().isFailure)
    }

    // ==================== 47-49: Quota Info ====================

    @Test
    fun `47 getQuotaInfo returns storage quota`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("get_space_usage"))
                respond(spaceUsageJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        val quota = service.getQuotaInfo().getOrThrow()
        assertEquals(10737418240L, quota.totalSpace)
        assertEquals(5368709120L, quota.usedSpace)
        assertEquals(5368709120L, quota.availableSpace)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertTrue(quota.usagePercentage in 0.49..0.51)
    }

    @Test
    fun `48 getQuotaInfo fails when not connected`() = runTest {
        val service = offlineService()
        assertTrue(service.getQuotaInfo().isFailure)
    }

    @Test
    fun `49 getQuotaInfo handles server error`() = runTest {
        val service = connectedService(routeWithAccount { request ->
            if (request.url.encodedPath.contains("get_space_usage"))
                respond("", HttpStatusCode.InternalServerError)
            else
                respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.getQuotaInfo().isFailure)
    }

    // ==================== 50-51: Exists ====================

    @Test
    fun `50 exists returns true when file found`() = runTest {
        val service = connectedService(routeWithAccount {
            respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.exists("/document.txt").getOrThrow())
    }

    @Test
    fun `51 exists returns false when file not found`() = runTest {
        val service = connectedService(routeWithAccount {
            respond("", HttpStatusCode.Conflict)
        })
        service.connect()
        assertFalse(service.exists("/missing.txt").getOrThrow())
    }

    // ==================== 52-53: Disconnect ====================

    @Test
    fun `52 disconnect sets service offline`() = runTest {
        val service = connectedService(routeWithAccount {
            respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        service.connect()
        assertTrue(service.isOnline)
        assertTrue(service.disconnect().isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `53 disconnect succeeds even when already offline`() = runTest {
        val service = offlineService()
        assertFalse(service.isOnline)
        assertTrue(service.disconnect().isSuccess)
    }

    // ==================== 54-55: Test Connection ====================

    @Test
    fun `54 testConnection returns true on success`() = runTest {
        val service = connectedService { request ->
            respond(accountInfoJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        assertTrue(service.testConnection().getOrThrow())
    }

    @Test
    fun `55 testConnection returns false on server error`() = runTest {
        val service = connectedService { request ->
            respond("", HttpStatusCode.InternalServerError)
        }
        assertFalse(service.testConnection().getOrThrow())
    }

    // ==================== 56-58: All Calls Use POST ====================

    @Test
    fun `56 all API calls use POST method`() = runTest {
        val capturedMethods = mutableListOf<HttpMethod>()
        val service = connectedService(routeWithAccount { request ->
            capturedMethods.add(request.method)
            when {
                request.url.encodedPath.contains("list_folder") ->
                    respond(listFolderJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("delete_v2") ->
                    respond("""{"metadata":{}}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("get_metadata") ->
                    respond(metadataFileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("get_space_usage") ->
                    respond(spaceUsageJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.OK)
            }
        })
        service.connect()
        service.listFiles("/").first()
        service.deleteFile("/x.txt")
        service.getFileInfo("/x.txt")
        service.getQuotaInfo()
        capturedMethods.forEach { assertEquals(HttpMethod.Post, it) }
    }

    // ==================== 57-58: Helpers ====================

    @Test
    fun `57 validatePath accepts any path`() = runTest {
        val service = offlineService()
        assertTrue(service.validatePath("/any/path").isSuccess)
        assertTrue(service.validatePath("").isSuccess)
        assertTrue(service.validatePath("/").isSuccess)
    }

    @Test
    fun `58 getParentPath returns correct parent`() = runTest {
        val service = offlineService()
        assertEquals("/parent", service.getParentPath("/parent/child"))
        assertEquals("/", service.getParentPath("/file.txt"))
        assertEquals("/", service.getParentPath("/"))
    }

    // ==================== 59-60: Storage Info ====================

    @Test
    fun `59 getStorageInfo returns DROPBOX type`() = runTest {
        val service = offlineService()
        val info = service.getStorageInfo()
        assertEquals(StorageType.DROPBOX, info.type)
        assertEquals("test-dropbox", info.name)
        assertTrue(info.supportsFolders)
        assertTrue(info.supportsMetadata)
    }

    @Test
    fun `60 rootPath is always slash`() = runTest {
        val service = offlineService()
        assertEquals("/", service.rootPath)
    }
}
