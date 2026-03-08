/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Git Service Mock HTTP Test Suite
 *
 * Comprehensive tests for Git protocol HTTP interactions
 * using ktor MockEngine. Covers connect, listFiles,
 * downloadFile, uploadFile, deleteFile for both GitHub
 * and GitLab platforms, plus error handling scenarios.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.git

import digital.vasic.yole.network.common.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Mock HTTP test suite for GitService.
 *
 * Uses ktor [MockEngine] to simulate HTTP responses from GitHub and GitLab
 * APIs, verifying that GitService correctly handles:
 * - connect via Git Smart HTTP info/refs
 * - listFiles via GitHub Contents API and GitLab Repository Tree API
 * - downloadFile via raw file URL
 * - uploadFile via GitHub Contents API (PUT with SHA check) and GitLab Repository Files API
 * - deleteFile via GitHub Contents API
 * - Error codes (404, 401, 500) and edge cases (empty repos)
 *
 * The injected [HttpClient] is passed through the `_injectedHttpClient` constructor
 * parameter so that GitService uses MockEngine instead of a real HTTP engine.
 */
class GitMockHttpTest {

    // ==================== CONFIG FACTORIES ====================

    private fun createGitHubConfig(
        repoUrl: String = "https://github.com/testuser/testrepo",
        branch: String = "main",
        token: String = "ghp_testtoken123"
    ) = StorageConfig.GitConfig(
        name = "test-git",
        repositoryUrl = repoUrl,
        branch = branch,
        personalAccessToken = token,
        localCachePath = "/tmp/git-mock-test"
    )

    private fun createGitLabConfig(
        repoUrl: String = "https://gitlab.com/testuser/testrepo",
        branch: String = "main",
        token: String = "glpat_testtoken456"
    ) = StorageConfig.GitConfig(
        name = "test-gitlab",
        repositoryUrl = repoUrl,
        branch = branch,
        personalAccessToken = token,
        localCachePath = "/tmp/gitlab-mock-test"
    )

    private fun createBasicAuthConfig() = StorageConfig.GitConfig(
        name = "test-basic",
        repositoryUrl = "https://github.com/testuser/testrepo",
        branch = "main",
        username = "myuser",
        password = "mypass",
        localCachePath = "/tmp/git-basic-test"
    )

    private fun createNoAuthConfig() = StorageConfig.GitConfig(
        name = "test-noauth",
        repositoryUrl = "https://github.com/public/repo",
        branch = "main",
        localCachePath = "/tmp/git-noauth-test"
    )

    private fun createMockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler))
    }

    // ==================== MOCK RESPONSE DATA ====================

    private val gitInfoRefsResponse = """001e# service=git-upload-pack
0000
00abcdef1234567890abcdef1234567890abcdef12 HEAD
00abcdef1234567890abcdef1234567890abcdef12 refs/heads/main
0000
"""

    private val gitInfoRefsMultiBranch = """001e# service=git-upload-pack
0000
00abcdef1234567890abcdef1234567890abcdef12 HEAD
00abcdef1234567890abcdef1234567890abcdef12 refs/heads/main
001234567890abcdef1234567890abcdef1234567890 refs/heads/develop
00aabbccddee1234567890abcdef1234567890abcdef refs/tags/v1.0.0
0000
"""

    private val githubContentsListJson = """[
  {"name":"README.md","path":"README.md","sha":"abc123","size":1024,"type":"file"},
  {"name":"src","path":"src","sha":"def456","size":0,"type":"dir"}
]"""

    private val githubContentsFileJson = """{
  "name":"README.md","path":"README.md","sha":"abc123","size":1024,"type":"file",
  "content":"SGVsbG8gV29ybGQ=","encoding":"base64"
}"""

    private val githubUploadResponseJson = """{
  "content":{"name":"file.txt","path":"file.txt","sha":"new_sha_123"},
  "commit":{"sha":"commit_sha_456"}
}"""

    private val gitlabTreeJson = """[
  {"id":"abc123","name":"file.txt","type":"blob","path":"file.txt","mode":"100644"},
  {"id":"def456","name":"src","type":"tree","path":"src","mode":"040000"}
]"""

    // ==================== 1. CONNECT WITH GIT REFS ====================

    @Test
    fun `connect with valid git refs sets connected`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            respond(
                content = ByteReadChannel(gitInfoRefsResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-git-upload-pack-advertisement")
            )
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect sends GET to info refs endpoint`() = runBlocking<Unit> {
        var capturedUrl = ""
        var capturedMethod: HttpMethod? = null
        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            capturedMethod = request.method
            respond(gitInfoRefsResponse, HttpStatusCode.OK)
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        service.connect()

        assertEquals(HttpMethod.Get, capturedMethod)
        assertTrue(capturedUrl.contains("info/refs"))
        assertTrue(capturedUrl.contains("service=git-upload-pack"))
    }

    @Test
    fun `connect sends authorization token header`() = runBlocking<Unit> {
        var capturedAuth = ""
        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"] ?: ""
            respond(gitInfoRefsResponse, HttpStatusCode.OK)
        }
        val service = GitService(createGitHubConfig(token = "ghp_secrettoken"), _injectedHttpClient = client)

        service.connect()

        assertEquals("token ghp_secrettoken", capturedAuth)
    }

    @Test
    fun `connect with basic auth sends Basic header`() = runBlocking<Unit> {
        var capturedAuth = ""
        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"] ?: ""
            respond(gitInfoRefsResponse, HttpStatusCode.OK)
        }
        val service = GitService(createBasicAuthConfig(), _injectedHttpClient = client)

        service.connect()

        assertTrue(capturedAuth.startsWith("Basic "), "Expected Basic auth header, got: $capturedAuth")
    }

    @Test
    fun `connect without credentials sends no auth header`() = runBlocking<Unit> {
        var capturedAuth: String? = null
        val client = createMockClient { request ->
            capturedAuth = request.headers["Authorization"]
            respond(gitInfoRefsResponse, HttpStatusCode.OK)
        }
        val service = GitService(createNoAuthConfig(), _injectedHttpClient = client)

        service.connect()

        assertNull(capturedAuth, "Expected no Authorization header for public repos")
    }

    @Test
    fun `connect with multi-branch refs succeeds`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            respond(
                content = ByteReadChannel(gitInfoRefsMultiBranch),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-git-upload-pack-advertisement")
            )
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect URL uses repository URL from config`() = runBlocking<Unit> {
        var capturedUrl = ""
        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            respond("", HttpStatusCode.OK)
        }
        val config = createGitHubConfig(repoUrl = "https://git.example.com/my/project")
        val service = GitService(config, _injectedHttpClient = client)

        service.connect()

        assertTrue(capturedUrl.contains("git.example.com/my/project"))
        assertTrue(capturedUrl.contains("info/refs"))
    }

    @Test
    fun `connect with empty response body still succeeds`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    // ==================== 2. LIST FILES GITHUB ====================

    @Test
    fun `listFiles GitHub returns parsed documents`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com/repos") -> respond(
                    githubContentsListJson,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        val successResult = results.find { it.isSuccess }
        assertNotNull(successResult)
        val docs = successResult.getOrNull()
        assertNotNull(docs)
        assertEquals(2, docs.size)
    }

    @Test
    fun `listFiles GitHub returns correct file names`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val names = docs.map { it.name }
        assertTrue(names.contains("README.md"))
        assertTrue(names.contains("src"))
    }

    @Test
    fun `listFiles GitHub distinguishes files from directories`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val readme = docs.find { it.name == "README.md" }
        val src = docs.find { it.name == "src" }
        assertNotNull(readme)
        assertNotNull(src)
        assertFalse(readme.isFolder, "README.md should not be a folder")
        assertTrue(src.isFolder, "src should be a folder")
    }

    @Test
    fun `listFiles GitHub reports correct file size`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val readme = docs.find { it.name == "README.md" }
        assertNotNull(readme)
        assertEquals(1024L, readme.size)
    }

    @Test
    fun `listFiles GitHub sends correct API URL`() = runBlocking<Unit> {
        var apiUrl = ""
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> {
                    apiUrl = url
                    respond(
                        githubContentsListJson, HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertTrue(apiUrl.contains("api.github.com/repos/testuser/testrepo/contents"))
        assertTrue(apiUrl.contains("ref=main"))
    }

    @Test
    fun `listFiles GitHub with subpath includes path in URL`() = runBlocking<Unit> {
        var apiUrl = ""
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> {
                    apiUrl = url
                    respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/src").toList()

        assertTrue(apiUrl.contains("/contents/src"), "Expected /contents/src in URL, got: $apiUrl")
    }

    @Test
    fun `listFiles GitHub documents have git storageId`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        docs.forEach { doc ->
            assertEquals("git", doc.storageId, "Document ${doc.name} should have git storageId")
        }
    }

    @Test
    fun `listFiles GitHub documents have SYNCED status`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        docs.forEach { doc ->
            assertEquals(SyncStatus.SYNCED, doc.syncStatus, "Document ${doc.name} should be SYNCED")
        }
    }

    @Test
    fun `listFiles GitHub documents have correct paths`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val readme = docs.find { it.name == "README.md" }
        assertNotNull(readme)
        assertEquals("/README.md", readme.path)
    }

    @Test
    fun `listFiles GitHub documents have READ WRITE DELETE permissions`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        docs.forEach { doc ->
            assertTrue(doc.permissions.contains(DocumentPermission.READ))
            assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
            assertTrue(doc.permissions.contains(DocumentPermission.DELETE))
        }
    }

    @Test
    fun `listFiles GitHub sends Accept application json header`() = runBlocking<Unit> {
        var acceptHeader = ""
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> {
                    acceptHeader = request.headers["Accept"] ?: ""
                    respond(
                        githubContentsListJson, HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertEquals("application/json", acceptHeader)
    }

    @Test
    fun `listFiles GitHub with many files returns all documents`() = runBlocking<Unit> {
        val manyFilesJson = buildString {
            append("[")
            for (i in 1..10) {
                if (i > 1) append(",")
                append("""{"name":"file$i.txt","path":"file$i.txt","sha":"sha$i","size":${i * 100},"type":"file"}""")
            }
            append("]")
        }
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    manyFilesJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        assertEquals(10, docs.size)
    }

    // ==================== 3. LIST FILES GITLAB ====================

    @Test
    fun `listFiles GitLab returns parsed documents`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4/projects") && url.contains("repository/tree") -> respond(
                    gitlabTreeJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val successResult = results.find { it.isSuccess }

        assertNotNull(successResult)
        val docs = successResult.getOrNull()
        assertNotNull(docs)
        assertEquals(2, docs.size)
    }

    @Test
    fun `listFiles GitLab distinguishes blobs from trees`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4/projects") -> respond(
                    gitlabTreeJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val fileItem = docs.find { it.name == "file.txt" }
        val srcItem = docs.find { it.name == "src" }
        assertNotNull(fileItem)
        assertNotNull(srcItem)
        assertFalse(fileItem.isFolder, "file.txt (blob) should not be a folder")
        assertTrue(srcItem.isFolder, "src (tree) should be a folder")
    }

    @Test
    fun `listFiles GitLab sends correct API URL with encoded project`() = runBlocking<Unit> {
        var apiUrl = ""
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4/projects") -> {
                    apiUrl = url
                    respond(
                        gitlabTreeJson, HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        service.listFiles("/").toList()

        assertTrue(apiUrl.contains("gitlab.com/api/v4/projects"))
        assertTrue(apiUrl.contains("repository/tree"))
        assertTrue(apiUrl.contains("ref=main"))
    }

    @Test
    fun `listFiles GitLab documents have correct names`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4/projects") -> respond(
                    gitlabTreeJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()
        val docs = results.firstOrNull { it.isSuccess }?.getOrNull()

        assertNotNull(docs)
        val names = docs.map { it.name }.toSet()
        assertTrue(names.contains("file.txt"))
        assertTrue(names.contains("src"))
    }

    // ==================== 4. DOWNLOAD FILE GITHUB ====================

    @Test
    fun `downloadFile GitHub completes successfully`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> respond(
                    content = ByteReadChannel("Hello World"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("text/plain"),
                        HttpHeaders.ContentLength to listOf("11")
                    )
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/README.md", "/tmp/local/README.md").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed)
    }

    @Test
    fun `downloadFile GitHub emits progress updates`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> respond(
                    content = ByteReadChannel("Hello World"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/README.md", "/tmp/local/README.md").toList()

        assertTrue(operations.size > 1, "Expected multiple progress emissions")
        val inProgress = operations.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
        assertTrue(inProgress.isNotEmpty())
    }

    @Test
    fun `downloadFile GitHub operation type is DOWNLOAD`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> respond(
                    content = ByteReadChannel("content"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/file.txt", "/tmp/local/file.txt").toList()

        operations.forEach { op ->
            assertEquals(NetworkOperation.Type.DOWNLOAD, op.type)
        }
    }

    @Test
    fun `downloadFile GitHub sends request to raw URL`() = runBlocking<Unit> {
        var rawUrl = ""
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> {
                    rawUrl = url
                    respond(
                        content = ByteReadChannel("data"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.downloadFile("/docs/guide.md", "/tmp/local/guide.md").toList()

        assertTrue(
            rawUrl.contains("raw.githubusercontent.com/testuser/testrepo/main/docs/guide.md"),
            "Expected raw URL with file path, got: $rawUrl"
        )
    }

    @Test
    fun `downloadFile GitHub final operation has progress 1 point 0`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> respond(
                    content = ByteReadChannel("content"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/file.txt", "/tmp/local/file.txt").toList()
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }

        assertNotNull(completed)
        assertEquals(1.0, completed.progress)
    }

    @Test
    fun `downloadFile GitHub with 404 still completes via fallback`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("raw.githubusercontent.com") -> respond("Not Found", HttpStatusCode.NotFound)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.downloadFile("/nonexistent.txt", "/tmp/local/nonexistent.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed, "Download should complete via fallback even on 404")
    }

    // ==================== 5. UPLOAD FILE GITHUB ====================

    @Test
    fun `uploadFile GitHub completes with COMPLETED status`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents") -> respond(
                    """{"name":"file.txt","path":"file.txt","sha":"existing_sha_789","size":100,"type":"file"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.method == HttpMethod.Put && url.contains("contents") -> respond(
                    githubUploadResponseJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/file.txt", "/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed)
    }

    @Test
    fun `uploadFile GitHub first GETs existing SHA then PUTs`() = runBlocking<Unit> {
        val requestLog = mutableListOf<Pair<HttpMethod, String>>()
        val client = createMockClient { request ->
            val url = request.url.toString()
            requestLog.add(request.method to url)
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents") -> respond(
                    """{"name":"file.txt","path":"file.txt","sha":"sha_abc","size":50,"type":"file"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.method == HttpMethod.Put && url.contains("contents") -> respond(
                    githubUploadResponseJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/file.txt", "/file.txt").toList()

        val contentsRequests = requestLog.filter { it.second.contains("api.github.com") && it.second.contains("contents") }
        val getRequests = contentsRequests.filter { it.first == HttpMethod.Get }
        val putRequests = contentsRequests.filter { it.first == HttpMethod.Put }
        assertTrue(getRequests.isNotEmpty(), "Expected at least one GET to check existing SHA")
        assertTrue(putRequests.isNotEmpty(), "Expected at least one PUT to upload")
    }

    @Test
    fun `uploadFile GitHub updates knownFiles after success`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents/newfile.txt") ->
                    respond("", HttpStatusCode.NotFound)
                request.method == HttpMethod.Put && url.contains("contents/newfile.txt") -> respond(
                    """{"content":{"name":"newfile.txt","path":"newfile.txt","sha":"fresh_sha"},"commit":{"sha":"c_abc"}}""",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/newfile.txt", "/newfile.txt").toList()

        val existsResult = service.exists("/newfile.txt")
        assertTrue(existsResult.isSuccess)
        assertTrue(existsResult.getOrNull() == true, "Uploaded file should be known")
    }

    @Test
    fun `uploadFile GitHub operation type is UPLOAD`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents") -> respond("", HttpStatusCode.NotFound)
                request.method == HttpMethod.Put && url.contains("contents") -> respond(
                    githubUploadResponseJson, HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/data.txt", "/data.txt").toList()

        operations.forEach { op ->
            assertEquals(NetworkOperation.Type.UPLOAD, op.type)
        }
    }

    @Test
    fun `uploadFile GitHub emits progress updates`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents") -> respond("", HttpStatusCode.NotFound)
                request.method == HttpMethod.Put && url.contains("contents") -> respond(
                    githubUploadResponseJson, HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/test.txt", "/test.txt").toList()

        assertTrue(operations.size > 1, "Expected multiple progress emissions")
        val inProgress = operations.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
        assertTrue(inProgress.isNotEmpty())
    }

    @Test
    fun `uploadFile GitHub final operation has progress 1 point 0`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents") -> respond("", HttpStatusCode.NotFound)
                request.method == HttpMethod.Put && url.contains("contents") -> respond(
                    githubUploadResponseJson, HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/f.txt", "/f.txt").toList()
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }

        assertNotNull(completed)
        assertEquals(1.0, completed.progress)
    }

    @Test
    fun `uploadFile GitHub sends PUT to contents API`() = runBlocking<Unit> {
        val capturedMethods = mutableListOf<HttpMethod>()
        val client = createMockClient { request ->
            capturedMethods.add(request.method)
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get -> respond("Not Found", HttpStatusCode.NotFound)
                else -> respond(
                    githubUploadResponseJson, HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/file.txt", "/file.txt").toList()

        assertTrue(capturedMethods.contains(HttpMethod.Put), "Should use PUT for GitHub upload")
    }

    // ==================== 6. UPLOAD FILE GITLAB ====================

    @Test
    fun `uploadFile GitLab completes successfully via PUT`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Put && url.contains("gitlab.com/api/v4/projects") &&
                        url.contains("repository/files") -> respond(
                    """{"file_path":"file.txt","branch":"main"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/file.txt", "/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed)
    }

    @Test
    fun `uploadFile GitLab falls back to POST when PUT fails`() = runBlocking<Unit> {
        val requestLog = mutableListOf<Pair<HttpMethod, String>>()
        val client = createMockClient { request ->
            val url = request.url.toString()
            requestLog.add(request.method to url)
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Put && url.contains("repository/files") ->
                    respond("", HttpStatusCode.NotFound)
                request.method == HttpMethod.Post && url.contains("repository/files") -> respond(
                    """{"file_path":"newfile.txt","branch":"main"}""",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/newfile.txt", "/newfile.txt").toList()

        val gitlabPuts = requestLog.filter { it.first == HttpMethod.Put && it.second.contains("repository/files") }
        val gitlabPosts = requestLog.filter { it.first == HttpMethod.Post && it.second.contains("repository/files") }
        assertTrue(gitlabPuts.isNotEmpty(), "Expected at least one PUT attempt")
        assertTrue(gitlabPosts.isNotEmpty(), "Expected a POST fallback after PUT failure")
    }

    @Test
    fun `uploadFile GitLab updates knownFiles after success`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Put && url.contains("repository/files") -> respond(
                    """{"file_path":"uploaded.txt","branch":"main"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/uploaded.txt", "/uploaded.txt").toList()

        val exists = service.exists("/uploaded.txt")
        assertTrue(exists.isSuccess)
        assertTrue(exists.getOrNull() == true, "Uploaded file should be known after GitLab upload")
    }

    // ==================== 7. DELETE FILE GITHUB ====================

    @Test
    fun `deleteFile GitHub succeeds with SHA fetch and DELETE`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents/old-file.txt") -> respond(
                    """{"name":"old-file.txt","path":"old-file.txt","sha":"del_sha_999","size":200,"type":"file"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.method == HttpMethod.Delete && url.contains("contents/old-file.txt") -> respond(
                    """{"content":null,"commit":{"sha":"del_commit"}}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.deleteFile("/old-file.txt")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile GitHub gets SHA before deleting`() = runBlocking<Unit> {
        val requestLog = mutableListOf<Pair<HttpMethod, String>>()
        val client = createMockClient { request ->
            val url = request.url.toString()
            requestLog.add(request.method to url)
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("contents/to-delete.txt") -> respond(
                    """{"name":"to-delete.txt","path":"to-delete.txt","sha":"del_sha","size":10,"type":"file"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.method == HttpMethod.Delete && url.contains("contents/to-delete.txt") -> respond(
                    """{"content":null}""", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.deleteFile("/to-delete.txt")

        val contentsRequests = requestLog.filter { it.second.contains("contents/to-delete.txt") }
        val getReqs = contentsRequests.filter { it.first == HttpMethod.Get }
        val deleteReqs = contentsRequests.filter { it.first == HttpMethod.Delete }
        assertTrue(getReqs.isNotEmpty(), "Expected GET to fetch SHA before delete")
        assertTrue(deleteReqs.isNotEmpty(), "Expected DELETE request")
    }

    @Test
    fun `deleteFile GitHub removes file from knownFiles`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                request.method == HttpMethod.Get && url.contains("api.github.com") && url.contains("contents") ->
                    respond(
                        githubContentsListJson, HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                request.method == HttpMethod.Delete && url.contains("api.github.com") -> respond(
                    """{"content":null}""", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        // Populate known files
        service.listFiles("/").toList()
        // Delete
        service.deleteFile("/README.md")

        val exists = service.exists("/README.md")
        assertTrue(exists.isSuccess)
        assertFalse(exists.getOrNull() ?: true, "Deleted file should no longer exist")
    }

    @Test
    fun `deleteFile GitHub with local tracking when API unavailable`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.InternalServerError)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val result = service.deleteFile("/fallback-file.txt")

        assertTrue(result.isSuccess, "deleteFile should succeed via local tracking")
    }

    // ==================== 8. CONNECT ERROR CODES ====================

    @Test
    fun `connect with 404 response still sets connected`() = runBlocking<Unit> {
        val client = createMockClient { respond("Not Found", HttpStatusCode.NotFound) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline, "Service should be online even after 404 (by design)")
    }

    @Test
    fun `connect with 500 response still sets connected`() = runBlocking<Unit> {
        val client = createMockClient { respond("Internal Server Error", HttpStatusCode.InternalServerError) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect with 403 forbidden still sets connected`() = runBlocking<Unit> {
        val client = createMockClient { respond("Forbidden", HttpStatusCode.Forbidden) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect with 401 unauthorized still sets connected`() = runBlocking<Unit> {
        val client = createMockClient { respond("Unauthorized", HttpStatusCode.Unauthorized) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.connect()

        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    // ==================== 9. LIST FILES EMPTY REPO ====================

    @Test
    fun `listFiles empty GitHub repo returns failure`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    "[]", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertTrue(lastResult.isFailure, "Expected failure for empty repository listing")
    }

    @Test
    fun `listFiles empty GitLab repo returns failure`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4") -> respond(
                    "[]", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertTrue(lastResult.isFailure)
    }

    @Test
    fun `listFiles with empty repo URL returns error`() = runBlocking<Unit> {
        val client = createMockClient { respond(gitInfoRefsResponse, HttpStatusCode.OK) }
        val config = createGitHubConfig(repoUrl = "")
        val service = GitService(config, _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.first().isFailure)
    }

    // ==================== 10. UPLOAD FILE AUTH FAILURE ====================

    @Test
    fun `uploadFile GitHub with 401 falls back to local tracking`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    """{"message":"Bad credentials"}""", HttpStatusCode.Unauthorized
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/secret.txt", "/secret.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed, "Upload should complete via local fallback even on 401")
    }

    @Test
    fun `uploadFile GitHub 401 still tracks file locally`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond(
                    """{"message":"Bad credentials"}""", HttpStatusCode.Unauthorized
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        service.uploadFile("/tmp/local/tracked.txt", "/tracked.txt").toList()

        val exists = service.exists("/tracked.txt")
        assertTrue(exists.isSuccess)
        assertTrue(exists.getOrNull() == true, "File should be tracked locally after 401 fallback")
    }

    @Test
    fun `uploadFile GitLab with 401 falls back to local tracking`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("gitlab.com/api/v4") -> respond(
                    """{"error":"401 Unauthorized"}""", HttpStatusCode.Unauthorized
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitLabConfig(), _injectedHttpClient = client)
        service.connect()

        val operations = service.uploadFile("/tmp/local/gl-file.txt", "/gl-file.txt").toList()

        assertTrue(operations.isNotEmpty())
        val completed = operations.find { it.status == NetworkOperation.Status.COMPLETED }
        assertNotNull(completed, "GitLab upload should complete via local fallback on 401")
    }

    // ==================== ADDITIONAL COVERAGE ====================

    @Test
    fun `listFiles when not connected emits failure`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `uploadFile when not connected emits FAILED status`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.uploadFile("/tmp/local/x.txt", "/x.txt").toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.last().status)
    }

    @Test
    fun `uploadFile when not connected has UPLOAD type`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.uploadFile("/tmp/local/x.txt", "/x.txt").toList()

        operations.forEach { op ->
            assertEquals(NetworkOperation.Type.UPLOAD, op.type)
        }
    }

    @Test
    fun `downloadFile when not connected emits FAILED status`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.downloadFile("/file.txt", "/tmp/local/file.txt").toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, operations.last().status)
    }

    @Test
    fun `downloadFile when not connected has DOWNLOAD type`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.downloadFile("/file.txt", "/tmp/local/file.txt").toList()

        operations.forEach { op ->
            assertEquals(NetworkOperation.Type.DOWNLOAD, op.type)
        }
    }

    @Test
    fun `connect then disconnect then listFiles fails`() = runBlocking<Unit> {
        val client = createMockClient { respond(gitInfoRefsResponse, HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        service.connect()
        assertTrue(service.isOnline)
        service.disconnect()
        assertFalse(service.isOnline)

        val results = service.listFiles("/").toList()
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `listFiles GitHub with server error falls through to failure`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") -> respond("Server Error", HttpStatusCode.InternalServerError)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertTrue(lastResult.isFailure)
    }

    @Test
    fun `multiple sequential operations maintain state correctly`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") && url.contains("contents") && request.method == HttpMethod.Get ->
                    respond(
                        githubContentsListJson, HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                url.contains("api.github.com") && request.method == HttpMethod.Put -> respond(
                    githubUploadResponseJson, HttpStatusCode.Created,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                url.contains("api.github.com") && request.method == HttpMethod.Delete -> respond(
                    """{"content":null}""", HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        // List files
        val listResults = service.listFiles("/").toList()
        val docs = listResults.firstOrNull { it.isSuccess }?.getOrNull()
        assertNotNull(docs)

        // Upload
        service.uploadFile("/tmp/local/new.txt", "/new.txt").toList()
        assertTrue(service.exists("/new.txt").getOrNull() == true)

        // Delete
        service.deleteFile("/new.txt")
        assertFalse(service.exists("/new.txt").getOrNull() ?: true)
    }

    @Test
    fun `getStorageInfo after connect shows online`() = runBlocking<Unit> {
        val client = createMockClient { respond(gitInfoRefsResponse, HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        service.connect()
        val info = service.getStorageInfo()

        assertTrue(info.isOnline)
        assertEquals(StorageType.GIT, info.type)
        assertEquals("test-git", info.name)
    }

    @Test
    fun `exists after listFiles returns true for known files`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                url.contains("api.github.com") && url.contains("contents") -> respond(
                    githubContentsListJson, HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()
        service.listFiles("/").toList()

        assertTrue(service.exists("/README.md").getOrNull() == true)
        assertTrue(service.exists("/src").getOrNull() == true)
    }

    @Test
    fun `disconnect sets isOnline to false`() = runBlocking<Unit> {
        val client = createMockClient { respond(gitInfoRefsResponse, HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()
        assertTrue(service.isOnline)

        service.disconnect()

        assertFalse(service.isOnline)
    }

    @Test
    fun `getQuotaInfo returns MAX_VALUE for git`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val result = service.getQuotaInfo()

        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertEquals(Long.MAX_VALUE, quota.totalSpace)
        assertEquals(Long.MAX_VALUE, quota.availableSpace)
        assertEquals(0L, quota.usedSpace)
    }

    @Test
    fun `getActiveOperations returns empty initially`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.getActiveOperations().toList().first()

        assertTrue(operations.isEmpty())
    }

    @Test
    fun `listFiles GitHub falls back to known files on API 404`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                else -> respond("Not Found", HttpStatusCode.NotFound)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)
        service.connect()

        val results = service.listFiles("/").toList()

        assertTrue(results.isNotEmpty())
        // No known files initially, so falls to failure
        assertTrue(results.first().isFailure)
    }

    @Test
    fun `addToCache and getCacheEntries work with mock client`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        service.addToCache("/file.txt", 1)
        val entries = service.getCacheEntries(null).toList().first()

        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun `clearCache removes all entries with mock client`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        service.addToCache("/file1.txt", 1)
        service.addToCache("/file2.txt", 2)
        service.clearCache()
        val entries = service.getCacheEntries(null).toList().first()

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getSyncStatus returns empty initially with mock client`() = runBlocking<Unit> {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val statuses = service.getSyncStatus(null).toList().first()

        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `syncFile emits COMPLETED operation`() = runBlocking<Unit> {
        val client = createMockClient { request ->
            val url = request.url.toString()
            when {
                url.contains("info/refs") -> respond(gitInfoRefsResponse, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.OK)
            }
        }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        val operations = service.syncFile("/README.md", false).toList()

        assertTrue(operations.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, operations.last().status)
    }

    @Test
    fun `getParentPath returns correct parent for nested paths`() {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        assertEquals("/src/main", service.getParentPath("/src/main/file.kt"))
        assertEquals("/src", service.getParentPath("/src/main"))
        assertEquals("/", service.getParentPath("/README.md"))
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `validatePath succeeds for valid paths`() {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        assertTrue(service.validatePath("/").isSuccess)
        assertTrue(service.validatePath("/src/main.kt").isSuccess)
        assertTrue(service.validatePath("/.gitignore").isSuccess)
    }

    @Test
    fun `validatePath fails for blank paths`() {
        val client = createMockClient { respond("", HttpStatusCode.OK) }
        val service = GitService(createGitHubConfig(), _injectedHttpClient = client)

        assertTrue(service.validatePath("").isFailure)
        assertTrue(service.validatePath("   ").isFailure)
    }
}
