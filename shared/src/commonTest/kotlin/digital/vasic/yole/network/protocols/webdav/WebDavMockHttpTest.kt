/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WebDAV Mock HTTP Test Suite
 *
 * Comprehensive tests for WebDavService using ktor
 * MockEngine to simulate real HTTP responses. Covers
 * all WebDAV operations: PROPFIND, GET, PUT, DELETE,
 * MKCOL, COPY, MOVE, HEAD, and OPTIONS with mocked
 * server responses and error scenarios.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.webdav

import digital.vasic.yole.network.common.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*

/**
 * Mock HTTP test suite for WebDavService.
 *
 * Uses ktor [MockEngine] to intercept and simulate HTTP responses for all
 * WebDAV operations. Each test injects a configured mock [HttpClient] into
 * WebDavService via the `_injectedHttpClient` constructor parameter and
 * verifies that the service correctly handles request methods, headers,
 * response bodies, and error conditions.
 */
class WebDavMockHttpTest {

    private val baseUrl = "https://webdav.example.com/remote.php/dav/files/testuser"

    private fun createConfig(
        url: String = baseUrl,
        username: String = "testuser",
        password: String = "testpass",
        authType: WebDavAuthenticationType = WebDavAuthenticationType.BASIC
    ) = StorageConfig.WebDavConfig(
        name = "test-webdav",
        url = url,
        username = username,
        password = password,
        authenticationType = authType
    )

    private fun createMockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler))
    }

    // ==================== Shared XML Responses ====================

    private val multistatusXml = """<?xml version="1.0" encoding="utf-8"?>
<d:multistatus xmlns:d="DAV:">
  <d:response>
    <d:href>/remote.php/dav/files/testuser/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>testuser</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
  <d:response>
    <d:href>/remote.php/dav/files/testuser/document.txt</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>document.txt</d:displayname>
        <d:getcontentlength>1024</d:getcontentlength>
        <d:getcontenttype>text/plain</d:getcontenttype>
        <d:resourcetype/>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
  <d:response>
    <d:href>/remote.php/dav/files/testuser/photos/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>photos</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
  <d:response>
    <d:href>/remote.php/dav/files/testuser/image.png</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>image.png</d:displayname>
        <d:getcontentlength>204800</d:getcontentlength>
        <d:getcontenttype>image/png</d:getcontenttype>
        <d:resourcetype/>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
</d:multistatus>"""

    private val emptyDirXml = """<?xml version="1.0" encoding="utf-8"?>
<d:multistatus xmlns:d="DAV:">
  <d:response>
    <d:href>/remote.php/dav/files/testuser/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>testuser</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
      </d:prop>
    </d:propstat>
  </d:response>
</d:multistatus>"""

    private val singleFileXml = """<?xml version="1.0" encoding="utf-8"?>
<d:multistatus xmlns:d="DAV:">
  <d:response>
    <d:href>/remote.php/dav/files/testuser/readme.md</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>readme.md</d:displayname>
        <d:getcontentlength>2048</d:getcontentlength>
        <d:getcontenttype>text/markdown</d:getcontenttype>
        <d:resourcetype/>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
</d:multistatus>"""

    private val quotaXml = """<?xml version="1.0" encoding="utf-8"?>
<d:multistatus xmlns:d="DAV:">
  <d:response>
    <d:href>/remote.php/dav/files/testuser/</d:href>
    <d:propstat>
      <d:prop>
        <d:quota-available-bytes>5368709120</d:quota-available-bytes>
        <d:quota-used-bytes>1073741824</d:quota-used-bytes>
      </d:prop>
    </d:propstat>
  </d:response>
</d:multistatus>"""

    // ==================== 1. connect + testConnection ====================

    @Test
    fun `connect succeeds with 200 OPTIONS response`() = runTest {
        val client = createMockClient { request ->
            respond("", HttpStatusCode.OK, headersOf("DAV", "1, 2, 3"))
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect sends OPTIONS method`() = runTest {
        var capturedMethod: HttpMethod? = null
        val client = createMockClient { request ->
            capturedMethod = request.method
            respond("", HttpStatusCode.OK)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        service.connect()

        assertEquals(HttpMethod("OPTIONS"), capturedMethod)
    }

    @Test
    fun `connect succeeds even with 401 response`() = runTest {
        val client = createMockClient { request ->
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect succeeds even with 500 response`() = runTest {
        val client = createMockClient { request ->
            respond("Server Error", HttpStatusCode.InternalServerError)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect succeeds even with network error`() = runTest {
        val client = createMockClient { request ->
            throw Exception("Network unreachable")
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `testConnection returns true when already connected`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.testConnection()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `testConnection performs connect-disconnect when not connected`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.testConnection()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    // ==================== 2. disconnect ====================

    @Test
    fun `disconnect sets isOnline to false`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()
        assertTrue(service.isOnline)

        service.disconnect()

        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect returns success`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.disconnect()

        assertTrue(result.isSuccess)
    }

    // ==================== 3. listFiles (PROPFIND) ====================

    @Test
    fun `listFiles parses multistatus XML with files and folders`() = runTest {
        val client = createMockClient { request ->
            respond(
                multistatusXml,
                HttpStatusCode.MultiStatus,
                headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isSuccess)
        val docs = results.first().getOrThrow()
        // Should exclude the parent directory (first response), leaving 3 entries
        assertEquals(3, docs.size)
    }

    @Test
    fun `listFiles parses file name correctly`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val file = docs.find { it.name == "document.txt" }
        assertNotNull(file)
        assertEquals("document.txt", file.name)
    }

    @Test
    fun `listFiles parses file size correctly`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val file = docs.find { it.name == "document.txt" }
        assertNotNull(file)
        assertEquals(1024L, file.size)
    }

    @Test
    fun `listFiles parses content type correctly`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val file = docs.find { it.name == "document.txt" }
        assertNotNull(file)
        assertEquals("text/plain", file.contentType)
    }

    @Test
    fun `listFiles identifies folders correctly`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val folder = docs.find { it.name == "photos" }
        assertNotNull(folder)
        assertTrue(folder.isFolder)
    }

    @Test
    fun `listFiles identifies files as non-folders`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val file = docs.find { it.name == "image.png" }
        assertNotNull(file)
        assertFalse(file.isFolder)
        assertEquals(204800L, file.size)
    }

    @Test
    fun `listFiles sends PROPFIND method`() = runTest {
        var capturedMethod: HttpMethod? = null

        val client = createMockClient { request ->
            capturedMethod = request.method
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertEquals(HttpMethod("PROPFIND"), capturedMethod)
    }

    @Test
    fun `listFiles sends Depth 1 header`() = runTest {
        var capturedDepth: String? = null

        val client = createMockClient { request ->
            capturedDepth = request.headers["Depth"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertEquals("1", capturedDepth)
    }

    @Test
    fun `listFiles sends Content-Type XML header`() = runTest {
        var capturedContentType: ContentType? = null

        val client = createMockClient { request ->
            capturedContentType = request.body.contentType
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertNotNull(capturedContentType)
        assertTrue(capturedContentType.toString().contains("application/xml"))
    }

    @Test
    fun `listFiles sends authorization header`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Basic "))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `listFiles sends correct Basic auth credentials`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        val expectedEncoded = Base64.encode("testuser:testpass".encodeToByteArray())
        assertEquals("Basic $expectedEncoded", capturedAuth)
    }

    @Test
    fun `listFiles with OAuth sends Bearer token`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val config = createConfig(
            password = "oauth_token_123",
            authType = WebDavAuthenticationType.OAUTH
        )
        val service = WebDavService(config, _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertEquals("Bearer oauth_token_123", capturedAuth)
    }

    @Test
    fun `listFiles with NONE auth sends no authorization`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val config = createConfig(authType = WebDavAuthenticationType.NONE)
        val service = WebDavService(config, _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertNull(capturedAuth)
    }

    @Test
    fun `listFiles skips parent directory entry`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val parentEntries = docs.filter { it.name == "testuser" }
        assertEquals(0, parentEntries.size)
    }

    @Test
    fun `listFiles parses empty multistatus directory`() = runTest {
        val client = createMockClient { respond(emptyDirXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isSuccess)
        assertEquals(0, results.first().getOrThrow().size)
    }

    @Test
    fun `listFiles builds correct URL for subpath`() = runTest {
        var capturedUrl: String? = null

        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("Documents/work").toList()

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("Documents/work"))
    }

    @Test
    fun `listFiles builds correct URL from config base URL`() = runTest {
        var capturedUrl: String? = null

        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val config = createConfig(url = "https://cloud.example.com/dav/")
        val service = WebDavService(config, _injectedHttpClient = client)
        service.connect()

        service.listFiles("photos").toList()

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("cloud.example.com/dav/photos"))
    }

    @Test
    fun `listFiles handles 200 OK response as success`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.first().isSuccess)
    }

    @Test
    fun `listFiles handles multistatus with many files`() = runTest {
        val manyFilesXml = buildString {
            append("""<?xml version="1.0" encoding="utf-8"?><d:multistatus xmlns:d="DAV:">""")
            append("""<d:response><d:href>/remote.php/dav/files/testuser/</d:href><d:propstat><d:prop><d:displayname>testuser</d:displayname><d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat></d:response>""")
            for (i in 1..10) {
                append("""<d:response><d:href>/remote.php/dav/files/testuser/file$i.txt</d:href><d:propstat><d:prop><d:displayname>file$i.txt</d:displayname><d:getcontentlength>${i * 100}</d:getcontentlength><d:resourcetype/></d:prop></d:propstat></d:response>""")
            }
            append("</d:multistatus>")
        }

        val client = createMockClient { respond(manyFilesXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.first().isSuccess)
        assertEquals(10, results.first().getOrThrow().size)
    }

    @Test
    fun `listFiles sets storageId to webdav`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        docs.forEach { doc ->
            assertEquals("webdav", doc.storageId)
        }
    }

    @Test
    fun `listFiles sets syncStatus to SYNCED`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        docs.forEach { doc ->
            assertEquals(SyncStatus.SYNCED, doc.syncStatus)
        }
    }

    @Test
    fun `listFiles sets permissions for files`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val file = docs.first { !it.isFolder }
        assertTrue(file.permissions.contains(DocumentPermission.READ))
        assertTrue(file.permissions.contains(DocumentPermission.WRITE))
        assertTrue(file.permissions.contains(DocumentPermission.DELETE))
    }

    @Test
    fun `listFiles sets permissions for folders with EXECUTE`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val docs = service.listFiles("/").first().getOrThrow()
        val folder = docs.first { it.isFolder }
        assertTrue(folder.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ==================== 4. listFiles error handling ====================

    @Test
    fun `listFiles returns error on 403 response`() = runTest {
        val client = createMockClient { respond("Forbidden", HttpStatusCode.Forbidden) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `listFiles returns error on 500 response`() = runTest {
        val client = createMockClient {
            respond("Internal Server Error", HttpStatusCode.InternalServerError)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `listFiles returns error on 401 response`() = runTest {
        val client = createMockClient {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `listFiles when not connected returns error`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        // Don't connect

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFailure)
        assertEquals("WebDAV not connected", results.first().exceptionOrNull()?.message)
    }

    @Test
    fun `listFiles error on network exception`() = runTest {
        var isFirst = true
        val client = createMockClient { _ ->
            if (isFirst) {
                isFirst = false
                respond("", HttpStatusCode.OK) // OPTIONS for connect
            } else {
                throw Exception("Connection reset")
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFailure)
    }

    // ==================== 5. downloadFile ====================

    @Test
    fun `downloadFile emits progress and COMPLETED on success`() = runTest {
        val fileContent = "Hello, WebDAV world!".encodeToByteArray()
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond(
                    fileContent,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentLength, fileContent.size.toString())
                )
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/document.txt", "/local/document.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
        assertEquals(1.0, completed.progress)
    }

    @Test
    fun `downloadFile starts with IN_PROGRESS at 0`() = runTest {
        val fileContent = ByteArray(10240) { 42 }
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond(
                    fileContent,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentLength, fileContent.size.toString())
                )
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/big.bin", "/local/big.bin").toList()

        assertTrue(operations.size >= 3, "Expected at least 3 operations but got ${operations.size}")
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operations.first().status)
        assertEquals(0.0, operations.first().progress)
    }

    @Test
    fun `downloadFile has DOWNLOAD type`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("data", HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "4"))
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val firstOp = service.downloadFile("/file.bin", "/tmp/file.bin").first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOp.type)
    }

    @Test
    fun `downloadFile emits FAILED on HTTP error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Not Found", HttpStatusCode.NotFound)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/missing.txt", "/local/missing.txt").toList()

        assertTrue(operations.isNotEmpty())
        val last = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, last.status)
    }

    @Test
    fun `downloadFile emits FAILED on 500 error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Server Error", HttpStatusCode.InternalServerError)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/error.txt", "/local/error.txt").toList()

        val last = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, last.status)
    }

    @Test
    fun `downloadFile when not connected emits error`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val operations = service.downloadFile("/file.txt", "/local/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.first().status)
        assertTrue(operations.first().error?.contains("not connected") == true)
    }

    @Test
    fun `downloadFile sends GET request with auth`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedAuth: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                capturedAuth = request.headers["Authorization"]
                respond("data", HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "4"))
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.downloadFile("/file.txt", "/local/file.txt").toList()

        assertEquals(HttpMethod.Get, capturedMethod)
        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Basic "))
    }

    @Test
    fun `downloadFile builds correct URL for remote path`() = runTest {
        var capturedUrl: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedUrl = request.url.toString()
                respond("data", HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "4"))
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.downloadFile("/documents/readme.md", "/local/readme.md").toList()

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("documents/readme.md"))
    }

    // ==================== 6. uploadFile ====================

    @Test
    fun `uploadFile emits COMPLETED on 201 Created`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
        assertEquals(1.0, completed.progress)
    }

    @Test
    fun `uploadFile emits COMPLETED on 204 NoContent`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        val completed = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
    }

    @Test
    fun `uploadFile emits COMPLETED on 200 OK`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.OK)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        val completed = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
    }

    @Test
    fun `uploadFile has UPLOAD type`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val firstOp = service.uploadFile("/local/file.txt", "/remote/file.txt").first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOp.type)
    }

    @Test
    fun `uploadFile emits progress steps`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        assertTrue(operations.size >= 2, "Should emit multiple progress updates")
        val inProgressOps = operations.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
        assertTrue(inProgressOps.isNotEmpty())
    }

    @Test
    fun `uploadFile sends PUT with auth header`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedAuth: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                capturedAuth = request.headers["Authorization"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        assertEquals(HttpMethod.Put, capturedMethod)
        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Basic "))
    }

    @Test
    fun `uploadFile when not connected emits error`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.first().status)
        assertTrue(operations.first().error?.contains("not connected") == true)
    }

    // ==================== 7. uploadFile error handling ====================

    @Test
    fun `uploadFile emits FAILED on 507 Insufficient Storage`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Insufficient Storage", HttpStatusCode.InsufficientStorage)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        val last = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, last.status)
    }

    @Test
    fun `uploadFile emits FAILED on 413 Payload Too Large`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Payload Too Large", HttpStatusCode.PayloadTooLarge)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/big.bin", "/remote/big.bin").toList()

        val last = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, last.status)
    }

    @Test
    fun `uploadFile emits FAILED on 403 Forbidden`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Forbidden", HttpStatusCode.Forbidden)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        val last = operations.last()
        assertEquals(NetworkOperation.Status.FAILED, last.status)
    }

    // ==================== 8. deleteFile ====================

    @Test
    fun `deleteFile succeeds with 204 NoContent`() = runTest {
        var capturedMethod: HttpMethod? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.deleteFile("/document.txt")

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod.Delete, capturedMethod)
    }

    @Test
    fun `deleteFile builds correct URL`() = runTest {
        var capturedUrl: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedUrl = request.url.toString()
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.deleteFile("/path/to/file.txt")

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("path/to/file.txt"))
    }

    @Test
    fun `deleteFile sends auth header`() = runTest {
        var capturedAuth: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedAuth = request.headers["Authorization"]
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.deleteFile("/file.txt")

        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Basic "))
    }

    @Test
    fun `deleteFile succeeds even on network error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                throw Exception("Network error")
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.deleteFile("/document.txt")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile succeeds even on 500 error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Server Error", HttpStatusCode.InternalServerError)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.deleteFile("/file.txt")

        // The implementation catches errors and returns success for offline tracking
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile when not connected still succeeds`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.deleteFile("/file.txt")

        assertTrue(result.isSuccess)
    }

    // ==================== 9. copyFile ====================

    @Test
    fun `copyFile sends COPY with Destination header`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedDest: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                capturedDest = request.headers["Destination"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.copyFile("/source.txt", "/dest.txt")

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod("COPY"), capturedMethod)
        assertNotNull(capturedDest)
        assertTrue(capturedDest!!.contains("dest.txt"))
    }

    @Test
    fun `copyFile sends Overwrite T header`() = runTest {
        var capturedOverwrite: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedOverwrite = request.headers["Overwrite"]
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.copyFile("/source.txt", "/dest.txt")

        assertEquals("T", capturedOverwrite)
    }

    @Test
    fun `copyFile succeeds on 204 NoContent`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.NoContent)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.copyFile("/a.txt", "/b.txt")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `copyFile when not connected still succeeds`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.copyFile("/source.txt", "/dest.txt")

        assertTrue(result.isSuccess)
    }

    // ==================== 10. moveFile ====================

    @Test
    fun `moveFile sends MOVE with Destination header`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedDest: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                capturedDest = request.headers["Destination"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.moveFile("/source/file.txt", "/dest/file.txt")

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod("MOVE"), capturedMethod)
        assertNotNull(capturedDest)
        assertTrue(capturedDest!!.contains("dest/file.txt"))
    }

    @Test
    fun `moveFile sends Overwrite T header`() = runTest {
        var capturedOverwrite: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedOverwrite = request.headers["Overwrite"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.moveFile("/a.txt", "/b.txt")

        assertEquals("T", capturedOverwrite)
    }

    @Test
    fun `moveFile returns NetworkDocument with correct name`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.moveFile("/old/file.txt", "/new/file.txt")

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("file.txt", doc.name)
        assertEquals("/new/file.txt", doc.path)
    }

    @Test
    fun `moveFile when not connected still succeeds`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.moveFile("/source.txt", "/dest.txt")

        assertTrue(result.isSuccess)
    }

    // ==================== 11. renameFile ====================

    @Test
    fun `renameFile sends MOVE with correct destination`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedDest: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                capturedDest = request.headers["Destination"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.renameFile("/docs/old.txt", "new.txt")

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod("MOVE"), capturedMethod)
        assertNotNull(capturedDest)
        assertTrue(capturedDest!!.contains("new.txt"))
    }

    @Test
    fun `renameFile sends Overwrite T header`() = runTest {
        var capturedOverwrite: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedOverwrite = request.headers["Overwrite"]
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.renameFile("/old.txt", "new.txt")

        assertEquals("T", capturedOverwrite)
    }

    // ==================== 12. createFolder ====================

    @Test
    fun `createFolder sends MKCOL request`() = runTest {
        var capturedMethod: HttpMethod? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.createFolder("/new-folder")

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod("MKCOL"), capturedMethod)
    }

    @Test
    fun `createFolder returns folder document`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.createFolder("/new-folder")

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertTrue(doc.isFolder)
        assertEquals("new-folder", doc.name)
    }

    @Test
    fun `createFolder sets SYNCED status`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val doc = service.createFolder("/synced-folder").getOrThrow()

        assertEquals(SyncStatus.SYNCED, doc.syncStatus)
    }

    @Test
    fun `createFolder sets EXECUTE permission`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val doc = service.createFolder("/folder").getOrThrow()

        assertTrue(doc.permissions.contains(DocumentPermission.READ))
        assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
        assertTrue(doc.permissions.contains(DocumentPermission.DELETE))
        assertTrue(doc.permissions.contains(DocumentPermission.EXECUTE))
    }

    @Test
    fun `createFolder builds correct URL for nested path`() = runTest {
        var capturedUrl: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedUrl = request.url.toString()
                respond("", HttpStatusCode.Created)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.createFolder("/deep/nested/folder")

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("deep/nested/folder"))
    }

    @Test
    fun `createFolder succeeds even on network error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                throw Exception("Network error")
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.createFolder("/new-folder")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFolder)
    }

    @Test
    fun `createFolder when not connected still succeeds`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.createFolder("/offline-folder")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFolder)
    }

    // ==================== 13. getFileInfo ====================

    @Test
    fun `getFileInfo sends PROPFIND with Depth 0`() = runTest {
        var capturedDepth: String? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedDepth = request.headers["Depth"]
                respond(singleFileXml, HttpStatusCode.MultiStatus)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.getFileInfo("/readme.md")

        assertEquals("0", capturedDepth)
    }

    @Test
    fun `getFileInfo parses file metadata from response`() = runTest {
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond(singleFileXml, HttpStatusCode.MultiStatus)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.getFileInfo("/readme.md")

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("readme.md", doc.name)
        assertEquals(2048L, doc.size)
    }

    @Test
    fun `getFileInfo returns fallback on PROPFIND failure`() = runTest {
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Not Found", HttpStatusCode.NotFound)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.getFileInfo("/fallback.txt")

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("fallback.txt", doc.name)
    }

    // ==================== 14. exists ====================

    @Test
    fun `exists returns true for 200 HEAD response`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.OK)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.exists("/document.txt")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `exists returns false for 404 HEAD response`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.exists("/missing.txt")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `exists uses HEAD method`() = runTest {
        var capturedMethod: HttpMethod? = null
        var requestCount = 0

        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                capturedMethod = request.method
                respond("", HttpStatusCode.OK)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.exists("/check.txt")

        assertEquals(HttpMethod.Head, capturedMethod)
    }

    @Test
    fun `exists returns false when not connected`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.exists("/some-file.txt")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `exists returns false on network error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                throw Exception("Connection refused")
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.exists("/unreachable.txt")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    // ==================== 15. getQuotaInfo ====================

    @Test
    fun `getQuotaInfo parses quota from PROPFIND response`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond(quotaXml, HttpStatusCode.MultiStatus)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.getQuotaInfo()

        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertEquals(1073741824L, quota.usedSpace)
        assertEquals(5368709120L, quota.availableSpace)
        assertEquals(1073741824L + 5368709120L, quota.totalSpace)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo returns default on server error`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK)
            } else {
                respond("Server Error", HttpStatusCode.InternalServerError)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.getQuotaInfo()

        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertEquals(1000000000L, quota.totalSpace)
    }

    // ==================== 16. searchFiles ====================

    @Test
    fun `searchFiles sends PROPFIND with Depth infinity`() = runTest {
        var capturedDepth: String? = null

        val client = createMockClient { request ->
            capturedDepth = request.headers["Depth"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        service.searchFiles("document").first()

        assertEquals("infinity", capturedDepth)
    }

    @Test
    fun `searchFiles filters results by query`() = runTest {
        val client = createMockClient { respond(multistatusXml, HttpStatusCode.MultiStatus) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.searchFiles("document").first()

        assertTrue(result.isSuccess)
        val docs = result.getOrThrow()
        assertTrue(docs.all { it.name.contains("document", ignoreCase = true) || it.path.contains("document", ignoreCase = true) })
    }

    @Test
    fun `searchFiles when not connected returns error`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val result = service.searchFiles("test").first()

        assertTrue(result.isFailure)
    }

    // ==================== 17. syncFile and syncAll ====================

    @Test
    fun `syncFile emits COMPLETED operation`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK) // OPTIONS
            } else {
                respond("", HttpStatusCode.OK) // HEAD
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.syncFile("/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        val last = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, last.status)
        assertEquals(1.0, last.progress)
    }

    @Test
    fun `syncAll emits COMPLETED with multistatus response`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            if (requestCount <= 1) {
                respond("", HttpStatusCode.OK) // OPTIONS
            } else {
                respond(multistatusXml, HttpStatusCode.MultiStatus) // PROPFIND
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.syncAll().toList()

        assertTrue(operations.isNotEmpty())
        val last = operations.last()
        assertEquals(NetworkOperation.Status.COMPLETED, last.status)
    }

    @Test
    fun `syncAll when not connected emits FAILED`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val operations = service.syncAll().toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.last().status)
    }

    // ==================== 18. Authentication variants ====================

    @Test
    fun `Digest auth falls back to Basic auth`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val config = createConfig(authType = WebDavAuthenticationType.DIGEST)
        val service = WebDavService(config, _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertNotNull(capturedAuth)
        assertTrue(capturedAuth!!.startsWith("Basic "))
    }

    @Test
    fun `blank username sends no auth header for BASIC`() = runTest {
        var capturedAuth: String? = null

        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(multistatusXml, HttpStatusCode.MultiStatus)
        }
        val config = createConfig(username = "", password = "")
        val service = WebDavService(config, _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertNull(capturedAuth)
    }

    // ==================== 19. getStorageInfo ====================

    @Test
    fun `getStorageInfo returns correct storage type and name`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val info = service.getStorageInfo()

        assertEquals(StorageType.WEBDAV, info.type)
        assertEquals("test-webdav", info.name)
        assertEquals("webdav_test-webdav", info.id)
        assertEquals(baseUrl, info.location)
        assertTrue(info.isOnline)
    }

    @Test
    fun `getStorageInfo reflects connection state`() = runTest {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)

        val infoBefore = service.getStorageInfo()
        assertFalse(infoBefore.isOnline)

        service.connect()
        val infoAfter = service.getStorageInfo()
        assertTrue(infoAfter.isOnline)
    }

    // ==================== 20. Path and config utilities ====================

    @Test
    fun `getParentPath returns parent directory`() {
        val service = WebDavService(createConfig())

        assertEquals("/folder", service.getParentPath("/folder/file.txt"))
        assertEquals("/", service.getParentPath("/file.txt"))
        assertNull(service.getParentPath("/"))
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `validatePath succeeds for valid paths`() {
        val service = WebDavService(createConfig())

        assertTrue(service.validatePath("/valid/path").isSuccess)
        assertTrue(service.validatePath("/").isSuccess)
        assertTrue(service.validatePath("some-file.txt").isSuccess)
    }

    @Test
    fun `validatePath fails for blank paths`() {
        val service = WebDavService(createConfig())

        assertTrue(service.validatePath("").isFailure)
        assertTrue(service.validatePath("  ").isFailure)
    }

    @Test
    fun `rootPath is forward slash`() {
        val service = WebDavService(createConfig())
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `isOnline initially false`() {
        val service = WebDavService(createConfig())
        assertFalse(service.isOnline)
    }

    @Test
    fun `config properties accessible`() {
        val service = WebDavService(createConfig())

        assertEquals("test-webdav", service.config.name)
        assertEquals(baseUrl, service.config.url)
        assertEquals("testuser", service.config.username)
        assertEquals("testpass", service.config.password)
        assertEquals(WebDavAuthenticationType.BASIC, service.config.authenticationType)
        assertEquals(StorageType.WEBDAV, service.config.storageType)
    }

    // ==================== 21. Multiple sequential operations ====================

    @Test
    fun `sequential create upload delete operations`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            when {
                requestCount <= 1 -> respond("", HttpStatusCode.OK) // OPTIONS
                request.method.value == "MKCOL" -> respond("", HttpStatusCode.Created)
                request.method == HttpMethod.Put -> respond("", HttpStatusCode.Created)
                request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                else -> respond("", HttpStatusCode.OK)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        // Create folder
        val folderResult = service.createFolder("/test-folder")
        assertTrue(folderResult.isSuccess)

        // Upload file
        val uploadOps = service.uploadFile("/tmp/test.txt", "/test-folder/test.txt").toList()
        assertEquals(NetworkOperation.Status.COMPLETED, uploadOps.last().status)

        // Delete file
        val deleteResult = service.deleteFile("/test-folder/test.txt")
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun `copy then delete operations`() = runTest {
        var requestCount = 0
        val client = createMockClient { request ->
            requestCount++
            when {
                requestCount <= 1 -> respond("", HttpStatusCode.OK) // OPTIONS
                request.method.value == "COPY" -> respond("", HttpStatusCode.Created)
                request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                else -> respond("", HttpStatusCode.OK)
            }
        }
        val service = WebDavService(createConfig(), _injectedHttpClient = client)
        service.connect()

        val copyResult = service.copyFile("/source.txt", "/backup.txt")
        assertTrue(copyResult.isSuccess)

        val deleteResult = service.deleteFile("/source.txt")
        assertTrue(deleteResult.isSuccess)
    }
}
