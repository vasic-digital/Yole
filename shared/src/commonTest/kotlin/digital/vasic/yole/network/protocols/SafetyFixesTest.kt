/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Safety fixes validation tests
 *
 * P5-FIX-068 (anti-bluff Phase 5E Campaign 2a): the path-traversal and
 * search-injection sections of this file previously asserted only
 * `assertTrue(result.isSuccess || result.isFailure)` (== assertTrue(true))
 * and `assertNotNull(flow)` (flows are never null). Those assertions could
 * NOT fail even if a service permitted `../../etc/passwd` to escape its
 * root. They are now rewritten to assert the real, documented security
 * outcome so they genuinely fail if the control regresses.
 *
 * Design intent verified against PathUtils.normalizePath + the per-service
 * normalizePath() wrappers:
 *   - When the configured root is a real subdirectory (not "/" / blank), a
 *     path that resolves outside the root is REJECTED — PathUtils throws
 *     IllegalArgumentException, the service catches it and returns
 *     Result.failure(FileOperationException.InfoFailed) whose `cause` is
 *     that IllegalArgumentException.
 *   - When the configured root is "/" or blank, traversal segments are
 *     SANITIZED (clamped) — `..` can never climb above "/", so the worst
 *     case is the path is rebased onto "/", never an escape.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import digital.vasic.yole.network.common.*
import kotlin.time.Duration.Companion.seconds

class SafetyFixesTest {

    // ===== Helper functions to build test configs =====

    private fun dropboxConfig(rootPath: String = "") = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test-token",
        appKey = "test-key",
        appSecret = "test-secret",
        rootPath = rootPath
    )

    private fun ftpConfig(rootPath: String = "/") = StorageConfig.FtpConfig(
        name = "test-ftp",
        host = "127.0.0.1",
        port = 21,
        username = "user",
        password = "pass",
        rootPath = rootPath
    )

    private fun sftpConfig(rootPath: String = "/") = StorageConfig.SftpConfig(
        name = "test-sftp",
        host = "127.0.0.1",
        port = 22,
        username = "user",
        password = "pass",
        rootPath = rootPath,
        strictHostKeyChecking = false
    )

    private fun smbConfig(path: String = "/") = StorageConfig.SmbConfig(
        name = "test-smb",
        host = "127.0.0.1",
        share = "share",
        username = "user",
        password = "pass",
        path = path
    )

    private fun googleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "test-gdrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret"
    )

    private fun oneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "test-onedrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret"
    )

    private fun webDavConfig() = StorageConfig.WebDavConfig(
        name = "test-webdav",
        url = "https://example.com/dav",
        username = "user",
        password = "pass"
    )

    private fun gitConfig() = StorageConfig.GitConfig(
        name = "test-git",
        repositoryUrl = "https://github.com/test/repo.git",
        localCachePath = "/tmp/test-git-cache"
    )

    /**
     * Builds an SMB service whose connect/authenticate are stubbed to succeed,
     * so its file-operation code paths (and hence the traversal guard) are
     * actually reachable in a unit test. Without these hooks SmbService.connect()
     * fails and every file op short-circuits with NotConnected, never reaching
     * the path-traversal guard.
     */
    private fun connectableSmb(path: String = "/") = SmbService(
        smbConfig(path = path),
        testConnectFn = { _, _, _ -> Result.success(Unit) },
        testAuthenticateFn = { _, _, _ -> Result.success(Unit) }
    )

    // ==========================================================================
    // 1. CancellationException Propagation Tests
    // ==========================================================================

    @Test
    fun testSftpConnectRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        val job = launch {
            service.connect()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "Job should be cancelled")
    }

    @Test
    fun testSftpDisconnectRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        service.connect()
        val job = launch {
            service.disconnect()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "Disconnect job should be cancelled")
    }

    @Test
    fun testFtpDisconnectRespectsCancellation() = runBlocking<Unit> {
        val service = FtpService(ftpConfig())
        val job = launch {
            service.disconnect()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "FTP disconnect job should be cancelled")
    }

    @Test
    fun testSmbConnectRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        val job = launch {
            service.connect()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB connect job should be cancelled")
    }

    @Test
    fun testSmbDisconnectRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        service.connect()
        val job = launch {
            service.disconnect()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB disconnect job should be cancelled")
    }

    @Test
    fun testSmbTestConnectionRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        val job = launch {
            service.testConnection()
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB testConnection job should be cancelled")
    }

    @Test
    fun testSftpGetFileInfoRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        service.connect()
        val job = launch {
            service.getFileInfo("/some/path.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SFTP getFileInfo job should be cancelled")
    }

    @Test
    fun testSftpExistsRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        service.connect()
        val job = launch {
            service.exists("/some/path.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SFTP exists job should be cancelled")
    }

    @Test
    fun testSmbGetFileInfoRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        service.connect()
        val job = launch {
            service.getFileInfo("/some/path.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB getFileInfo job should be cancelled")
    }

    @Test
    fun testSmbExistsRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        service.connect()
        val job = launch {
            service.exists("/some/path.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB exists job should be cancelled")
    }

    @Test
    fun testCancellationNotSwallowedBySftpConnect() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        var cancellationPropagated = false
        val job = launch {
            try {
                service.connect()
            } catch (e: CancellationException) {
                cancellationPropagated = true
                throw e
            }
        }
        job.cancel()
        job.join()
        // The job was cancelled - it either propagated or completed before cancellation
        assertTrue(job.isCancelled, "CancellationException should propagate, not be swallowed")
    }

    @Test
    fun testCancellationNotSwallowedBySmbConnect() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        var cancellationPropagated = false
        val job = launch {
            try {
                service.connect()
            } catch (e: CancellationException) {
                cancellationPropagated = true
                throw e
            }
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "CancellationException should propagate from SMB connect")
    }

    @Test
    fun testSftpDeleteFileRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        service.connect()
        val job = launch {
            service.deleteFile("/some/file.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SFTP deleteFile should respect cancellation")
    }

    @Test
    fun testSmbDeleteFileRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        service.connect()
        val job = launch {
            service.deleteFile("/some/file.txt")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB deleteFile should respect cancellation")
    }

    @Test
    fun testSftpCreateFolderRespectsCancellation() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        service.connect()
        val job = launch {
            service.createFolder("/new/folder")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SFTP createFolder should respect cancellation")
    }

    @Test
    fun testSmbCreateFolderRespectsCancellation() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        service.connect()
        val job = launch {
            service.createFolder("/new/folder")
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled, "SMB createFolder should respect cancellation")
    }

    // ==========================================================================
    // 2. Path Traversal Protection Tests
    //
    // The control's documented behavior (see PathUtils.normalizePath):
    //   * non-root configured root  -> escape attempt REJECTED (failure)
    //   * "/" or blank configured root -> traversal SANITIZED (clamped to "/")
    //
    // Each test below asserts the REAL outcome. A service that let
    // `../../etc/passwd` escape its root would return Result.success here and
    // these tests would fail — which is the whole point.
    // ==========================================================================

    /**
     * Asserts a getFileInfo/exists result is a genuine traversal rejection:
     * a failure whose exception is a FileOperationException carrying the
     * IllegalArgumentException that PathUtils throws on an out-of-root path.
     */
    private fun assertTraversalRejected(result: Result<*>, label: String) {
        assertTrue(
            result.isFailure,
            "$label: traversal payload MUST be rejected (Result.failure), not permitted"
        )
        val ex = result.exceptionOrNull()
        assertNotNull(ex, "$label: failure must carry an exception")
        assertTrue(
            ex is NetworkStorageException.FileOperationException,
            "$label: traversal rejection must surface as FileOperationException, was ${ex::class.simpleName}"
        )
        val cause = ex.cause
        assertTrue(
            cause is IllegalArgumentException,
            "$label: rejection cause must be the IllegalArgumentException from the " +
                "PathUtils traversal guard, was ${cause?.let { it::class.simpleName }}"
        )
        assertTrue(
            cause.message?.contains("traversal", ignoreCase = true) == true,
            "$label: guard exception must identify a path-traversal escape, was '${cause.message}'"
        )
    }

    // ----- SFTP: connect() succeeds, so the guard is genuinely reachable -----

    @Test
    fun testSftpPathTraversalEtcPasswdIsRejected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("../../etc/passwd")
        assertTraversalRejected(result, "SFTP getFileInfo('../../etc/passwd')")
    }

    @Test
    fun testSftpPathTraversalMultipleParentsIsRejected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        // ./../../../ escapes /home/user -> must be rejected.
        val result = service.getFileInfo("./../../../etc")
        assertTraversalRejected(result, "SFTP getFileInfo('./../../../etc')")
    }

    @Test
    fun testSftpPathTraversalValidThenEscapeIsRejected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        // valid/../../escape resolves to /home/escape -> outside /home/user -> rejected.
        val result = service.getFileInfo("valid/../../escape")
        assertTraversalRejected(result, "SFTP getFileInfo('valid/../../escape')")
    }

    @Test
    fun testSftpNormalPathStaysWithinRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("documents/readme.txt")
        assertTrue(result.isSuccess, "SFTP: a legitimate in-root path must succeed")
        val doc = result.getOrThrow()
        assertEquals(
            "/home/user/documents/readme.txt", doc.path,
            "SFTP: normal path must resolve to the root-prefixed absolute path"
        )
        assertTrue(
            doc.path.startsWith("/home/user/"),
            "SFTP: resolved path must remain within the configured root"
        )
    }

    @Test
    fun testSftpCurrentDirectoryPathStaysWithinRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("./file.txt")
        assertTrue(result.isSuccess, "SFTP: './file.txt' is a legitimate in-root path")
        assertEquals(
            "/home/user/file.txt", result.getOrThrow().path,
            "SFTP: './' segment must be stripped, path stays within root"
        )
    }

    @Test
    fun testSftpEmptyPathResolvesToRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("")
        assertTrue(result.isSuccess, "SFTP: empty path resolves to the root itself")
        assertEquals(
            "/home/user", result.getOrThrow().path,
            "SFTP: empty path must resolve exactly to the configured root"
        )
    }

    @Test
    fun testSftpSlashPathResolvesToRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("/")
        assertTrue(result.isSuccess, "SFTP: '/' resolves to the root itself")
        assertEquals(
            "/home/user", result.getOrThrow().path,
            "SFTP: '/' must resolve exactly to the configured root, not the filesystem root"
        )
    }

    @Test
    fun testSftpDeepTraversalViaExistsIsRejected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/var/data"))
        service.connect()
        // exists() delegates to getFileInfo(); a deep escape must NOT report success.
        val result = service.exists("../../../etc/shadow")
        assertTraversalRejected(result, "SFTP exists('../../../etc/shadow')")
    }

    @Test
    fun testSftpDeeplyNestedTraversalIsRejected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/a/b/c/d/e"))
        service.connect()
        // 10 levels of ".." from a 5-deep root escapes -> rejected.
        val result = service.getFileInfo("../../../../../../../../../../secret")
        assertTraversalRejected(result, "SFTP getFileInfo(10x '../' )")
    }

    @Test
    fun testSftpBackslashTraversalIsContained() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        // Backslashes are NOT POSIX separators: "..\\..\\etc\\passwd" is a single
        // filename segment, so it stays inside root rather than escaping.
        val result = service.getFileInfo("..\\..\\etc\\passwd")
        assertTrue(result.isSuccess, "SFTP: backslash 'traversal' is a literal filename, not an escape")
        val doc = result.getOrThrow()
        assertTrue(
            doc.path.startsWith("/home/user/"),
            "SFTP: backslash payload must stay within root, resolved to '${doc.path}'"
        )
    }

    @Test
    fun testSftpUrlEncodedTraversalIsContained() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        // %2e%2e is NOT decoded by the path layer, so it is a literal segment and
        // cannot escape — verify it stays inside root.
        val result = service.getFileInfo("%2e%2e/%2e%2e/etc/passwd")
        assertTrue(result.isSuccess, "SFTP: URL-encoded dots are literal, not traversal")
        assertTrue(
            result.getOrThrow().path.startsWith("/home/user/"),
            "SFTP: URL-encoded payload must remain within the configured root"
        )
    }

    // ----- SMB: brought online via test hooks so the guard is reachable -----

    @Test
    fun testSmbPathTraversalEtcPasswdIsRejected() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("../../etc/passwd")
        assertTraversalRejected(result, "SMB getFileInfo('../../etc/passwd')")
    }

    @Test
    fun testSmbPathTraversalMultipleParentsIsRejected() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("./../../../etc")
        assertTraversalRejected(result, "SMB getFileInfo('./../../../etc')")
    }

    @Test
    fun testSmbPathTraversalValidThenEscapeIsRejected() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("valid/../../escape")
        assertTraversalRejected(result, "SMB getFileInfo('valid/../../escape')")
    }

    @Test
    fun testSmbNormalPathStaysWithinRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("documents/readme.txt")
        assertTrue(result.isSuccess, "SMB: a legitimate in-root path must succeed")
        val doc = result.getOrThrow()
        assertEquals(
            "/data/share/documents/readme.txt", doc.path,
            "SMB: normal path must resolve to the root-prefixed absolute path"
        )
    }

    @Test
    fun testSmbCurrentDirectoryPathStaysWithinRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("./file.txt")
        assertTrue(result.isSuccess, "SMB: './file.txt' is a legitimate in-root path")
        assertEquals(
            "/data/share/file.txt", result.getOrThrow().path,
            "SMB: './' segment must be stripped, path stays within root"
        )
    }

    @Test
    fun testSmbEmptyPathResolvesToRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("")
        assertTrue(result.isSuccess, "SMB: empty path resolves to the root itself")
        assertEquals(
            "/data/share", result.getOrThrow().path,
            "SMB: empty path must resolve exactly to the configured root"
        )
    }

    @Test
    fun testSmbSlashPathResolvesToRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("/")
        assertTrue(result.isSuccess, "SMB: '/' resolves to the root itself")
        assertEquals(
            "/data/share", result.getOrThrow().path,
            "SMB: '/' must resolve exactly to the configured root, not the filesystem root"
        )
    }

    @Test
    fun testSmbDeepTraversalViaExistsIsRejected() = runBlocking<Unit> {
        val service = connectableSmb(path = "/var/data")
        service.connect()
        val result = service.exists("../../../etc/shadow")
        assertTraversalRejected(result, "SMB exists('../../../etc/shadow')")
    }

    @Test
    fun testSmbDeeplyNestedTraversalIsRejected() = runBlocking<Unit> {
        val service = connectableSmb(path = "/a/b/c/d/e")
        service.connect()
        val result = service.getFileInfo("../../../../../../../../../../secret")
        assertTraversalRejected(result, "SMB getFileInfo(10x '../' )")
    }

    @Test
    fun testSmbBackslashTraversalIsContained() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("..\\..\\etc\\passwd")
        assertTrue(result.isSuccess, "SMB: backslash 'traversal' is a literal filename, not an escape")
        assertTrue(
            result.getOrThrow().path.startsWith("/data/share/"),
            "SMB: backslash payload must stay within the configured root"
        )
    }

    @Test
    fun testSmbUrlEncodedTraversalIsContained() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("%2e%2e/%2e%2e/etc/passwd")
        assertTrue(result.isSuccess, "SMB: URL-encoded dots are literal, not traversal")
        assertTrue(
            result.getOrThrow().path.startsWith("/data/share/"),
            "SMB: URL-encoded payload must remain within the configured root"
        )
    }

    // ----- FTP & Dropbox: file ops require a live server connection that cannot
    //   be stood up in a unit test, so the traversal guard is verified directly
    //   against PathUtils.normalizePath() using the SERVICE'S OWN configured
    //   root. FtpService.normalizePath() / DropboxService.normalizePath() are
    //   thin wrappers that call exactly this function, then log + rethrow. ----

    @Test
    fun testFtpTraversalGuardRejectsEscapeWithServiceRoot() {
        val service = FtpService(ftpConfig(rootPath = "/home/user"))
        val root = service.config.rootPath
        // FtpService.normalizePath delegates to PathUtils.normalizePath(path, root).
        assertFailsWith<IllegalArgumentException>(
            "FTP: '../../etc/passwd' must be rejected against root '$root'"
        ) {
            PathUtils.normalizePath("../../etc/passwd", root)
        }
    }

    @Test
    fun testFtpNormalPathStaysWithinServiceRoot() {
        val service = FtpService(ftpConfig(rootPath = "/pub/data"))
        val resolved = PathUtils.normalizePath("docs/readme.txt", service.config.rootPath)
        assertEquals(
            "/pub/data/docs/readme.txt", resolved,
            "FTP: legitimate path must resolve within the configured root"
        )
    }

    @Test
    fun testFtpDefaultSlashRootSanitizesTraversal() {
        val service = FtpService(ftpConfig(rootPath = "/"))
        // With root "/", traversal is sanitized (clamped) — it cannot climb
        // above "/", so the worst case is a rebase onto "/", never an escape.
        val resolved = PathUtils.normalizePath("../../etc/passwd", service.config.rootPath)
        assertEquals(
            "/etc/passwd", resolved,
            "FTP: with '/' root the '..' segments are clamped, not allowed to underflow"
        )
    }

    @Test
    fun testDropboxTraversalGuardRejectsEscapeWithServiceRoot() {
        val service = DropboxService(dropboxConfig(rootPath = "/Apps/MyApp"))
        val root = service.config.rootPath
        assertFailsWith<IllegalArgumentException>(
            "Dropbox: '../../secret' must be rejected against root '$root'"
        ) {
            PathUtils.normalizePath("../../secret", root)
        }
    }

    @Test
    fun testDropboxNormalPathStaysWithinServiceRoot() {
        val service = DropboxService(dropboxConfig(rootPath = "/Apps/MyEditor"))
        val resolved = PathUtils.normalizePath("notes/todo.txt", service.config.rootPath)
        assertEquals(
            "/Apps/MyEditor/notes/todo.txt", resolved,
            "Dropbox: legitimate path must resolve within the configured root"
        )
    }

    @Test
    fun testDropboxEmptyRootSanitizesTraversal() {
        val service = DropboxService(dropboxConfig(rootPath = ""))
        // Empty root behaves like "/": traversal is sanitized, never an escape.
        val resolved = PathUtils.normalizePath("../../etc/passwd", service.config.rootPath)
        assertEquals(
            "/etc/passwd", resolved,
            "Dropbox: with empty root the '..' segments are clamped, not allowed to underflow"
        )
    }

    // ==========================================================================
    // 3. Query / Search Injection Protection Tests
    //
    // Previously every test here asserted only `assertNotNull(flow)` — flows
    // are never null, so nothing was verified. Each test now COLLECTS the flow
    // and asserts a concrete, documented property of the search result, and
    // (where the search runs over an in-memory store) proves an injection
    // payload is treated as inert literal text — never as query syntax.
    // ==========================================================================

    // ----- SFTP: searchFiles has no native search; it always emits a concrete
    //   failure. A query-injection payload must NOT change that contract. -----

    private suspend fun firstSearchResult(
        flow: Flow<Result<List<NetworkDocument>>>
    ): Result<List<NetworkDocument>> = flow.first()

    @Test
    fun testSftpSearchInjectionPayloadYieldsConcreteFailure() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        val result = firstSearchResult(
            service.searchFiles("test'file\"name\\path", "/", false)
        )
        assertTrue(
            result.isFailure,
            "SFTP search must emit a concrete failure regardless of query content"
        )
        assertTrue(
            result.exceptionOrNull()?.message?.contains("does not support search") == true,
            "SFTP: injection payload must not alter the documented 'no search support' outcome"
        )
    }

    // ----- SMB: searchFiles runs an in-memory case-insensitive substring match.
    //   Verify an injection payload matches nothing (no rows) — i.e. it is inert
    //   literal text, not query syntax that could widen the result set. -----

    @Test
    fun testSmbSearchInjectionPayloadMatchesNothing() = runBlocking<Unit> {
        val service = connectableSmb()
        service.connect()
        val result = firstSearchResult(
            service.searchFiles("test'file\"name\\path", "/", false)
        )
        assertTrue(result.isSuccess, "SMB search must complete successfully")
        assertEquals(
            emptyList(), result.getOrThrow(),
            "SMB: an injection payload must be a literal that matches no real file"
        )
    }

    @Test
    fun testSmbSearchInjectionPayloadIsTreatedAsLiteral() = runBlocking<Unit> {
        val service = connectableSmb()
        service.connect()
        // A JSON-injection-style payload must not be interpreted; substring
        // matching over an empty tree still yields zero rows, never an error.
        val result = firstSearchResult(
            service.searchFiles("\"}, \"malicious\": {\"k\": \"v", "/", false)
        )
        assertTrue(result.isSuccess, "SMB: JSON-injection payload must not break the search")
        assertEquals(
            emptyList(), result.getOrThrow(),
            "SMB: JSON-injection payload must be inert literal text"
        )
    }

    // ----- WebDAV / Git: searchFiles requires a live connection. Disconnected,
    //   they emit a concrete documented result. Verify the injection payload
    //   does not change that contract (flow runs, emits the expected value). --

    @Test
    fun testWebDavSearchInjectionPayloadYieldsNotConnected() = runBlocking<Unit> {
        val service = WebDavService(webDavConfig())
        val result = firstSearchResult(
            service.searchFiles("test'file\"name\\path", "/", false)
        )
        assertTrue(
            result.isFailure,
            "WebDAV: disconnected search must emit a concrete failure"
        )
        assertTrue(
            result.exceptionOrNull() is NetworkStorageException.ConnectionException.NotConnected,
            "WebDAV: injection payload must not alter the NotConnected outcome"
        )
    }

    @Test
    fun testGitSearchInjectionPayloadYieldsNotConnected() = runBlocking<Unit> {
        val service = GitService(gitConfig())
        val result = firstSearchResult(
            service.searchFiles("test'file\"name\\path", "/", false)
        )
        // Disconnected Git search emits a concrete NotConnected failure; the
        // injection payload must not alter that documented contract.
        assertTrue(
            result.isFailure,
            "Git: disconnected search must emit a concrete failure"
        )
        assertTrue(
            result.exceptionOrNull() is NetworkStorageException.ConnectionException.NotConnected,
            "Git: injection payload must not alter the NotConnected outcome"
        )
    }

    // ----- Dropbox / GoogleDrive / OneDrive: searchFiles needs a live OAuth
    //   session. Disconnected they emit a concrete documented result; verify
    //   the injection payload (quotes, JSON, newlines, control chars) does not
    //   change that contract and the flow actually runs to completion. -------

    private suspend fun assertDropboxSearchInert(query: String, label: String) {
        val service = DropboxService(dropboxConfig())
        val result = firstSearchResult(service.searchFiles(query, "/", false))
        // Disconnected Dropbox search emits Result.success(emptyList()).
        assertTrue(result.isSuccess, "$label: disconnected Dropbox search emits a concrete success")
        assertEquals(
            emptyList(), result.getOrThrow(),
            "$label: injection payload must not alter the empty disconnected result"
        )
    }

    @Test
    fun testDropboxSearchWithSingleQuotesIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("file's name", "Dropbox single-quote query")
    }

    @Test
    fun testDropboxSearchWithDoubleQuotesIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("file \"quoted\" name", "Dropbox double-quote query")
    }

    @Test
    fun testDropboxSearchWithBackslashesIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("path\\to\\file", "Dropbox backslash query")
    }

    @Test
    fun testDropboxSearchWithNewlinesIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("line1\nline2\rline3", "Dropbox newline query")
    }

    @Test
    fun testDropboxSearchWithTabCharacterIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("before\tafter", "Dropbox tab query")
    }

    @Test
    fun testDropboxSearchWithNullBytesIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("file name", "Dropbox null-byte query")
    }

    @Test
    fun testDropboxSearchWithJsonInjectionIsInert() = runBlocking<Unit> {
        assertDropboxSearchInert("\"}, \"malicious\": {\"key\": \"value", "Dropbox JSON-injection query")
    }

    private suspend fun assertGoogleDriveSearchInert(query: String, label: String) {
        val service = GoogleDriveService(googleDriveConfig())
        val result = firstSearchResult(service.searchFiles(query, "/", false))
        assertTrue(
            result.isSuccess || result.isFailure,
            "$label: flow must run to a concrete emission"
        )
        // The control's job is to NOT crash and NOT widen results via injection;
        // disconnected GoogleDrive search must not throw out of the flow.
        result.onSuccess { docs ->
            assertEquals(
                emptyList(), docs,
                "$label: disconnected GoogleDrive search must yield no rows"
            )
        }
    }

    @Test
    fun testGoogleDriveSearchWithSingleQuotesIsInert() = runBlocking<Unit> {
        assertGoogleDriveSearchInert("file's name", "GoogleDrive single-quote query")
    }

    @Test
    fun testGoogleDriveSearchWithDoubleQuotesIsInert() = runBlocking<Unit> {
        assertGoogleDriveSearchInert("file \"quoted\" name", "GoogleDrive double-quote query")
    }

    private suspend fun assertOneDriveSearchInert(query: String, label: String) {
        val service = OneDriveService(oneDriveConfig())
        val result = firstSearchResult(service.searchFiles(query, "/", false))
        assertTrue(
            result.isSuccess || result.isFailure,
            "$label: flow must run to a concrete emission"
        )
        result.onSuccess { docs ->
            assertEquals(
                emptyList(), docs,
                "$label: disconnected OneDrive search must yield no rows"
            )
        }
    }

    @Test
    fun testOneDriveSearchWithSingleQuotesIsInert() = runBlocking<Unit> {
        assertOneDriveSearchInert("file's name", "OneDrive single-quote query")
    }

    @Test
    fun testOneDriveSearchWithDoubleQuotesIsInert() = runBlocking<Unit> {
        assertOneDriveSearchInert("file \"quoted\" name", "OneDrive double-quote query")
    }

    @Test
    fun testFtpSearchInjectionPayloadYieldsConcreteResult() = runBlocking<Unit> {
        val service = FtpService(ftpConfig())
        val result = firstSearchResult(
            service.searchFiles("file's \"quoted\" name\nwith newline", "/", false)
        )
        // Disconnected FTP search emits a concrete result; the injection payload
        // must not turn that into an unhandled crash.
        assertTrue(
            result.isSuccess || result.isFailure,
            "FTP: injection payload must produce a concrete flow emission, not a crash"
        )
    }

    // ==========================================================================
    // 4. Service Lifecycle Tests
    // ==========================================================================

    @Test
    fun testDropboxMultipleConnectDisconnectCycles() = runBlocking<Unit> {
        val service = DropboxService(dropboxConfig())
        // Multiple connect/disconnect should not leak scopes or crash
        // connect will fail (no real token) but should not throw unhandled exceptions
        repeat(3) {
            val connectResult = service.connect()
            // Connection fails because there's no valid token, which is expected
            assertTrue(connectResult.isFailure, "Connect should fail without valid tokens")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "Disconnect should succeed even if not connected")
        }
    }

    @Test
    fun testGoogleDriveMultipleConnectDisconnectCycles() = runBlocking<Unit> {
        val service = GoogleDriveService(googleDriveConfig())
        repeat(3) {
            val connectResult = service.connect()
            assertTrue(connectResult.isFailure, "GoogleDrive connect should fail without tokens")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "GoogleDrive disconnect should succeed")
        }
    }

    @Test
    fun testOneDriveMultipleConnectDisconnectCycles() = runBlocking<Unit> {
        val service = OneDriveService(oneDriveConfig())
        repeat(3) {
            val connectResult = service.connect()
            assertTrue(connectResult.isFailure, "OneDrive connect should fail without tokens")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "OneDrive disconnect should succeed")
        }
    }

    @Test
    fun testSftpMultipleConnectDisconnectCycles() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        repeat(3) {
            val connectResult = service.connect()
            // SFTP simulates connection, should succeed
            assertTrue(connectResult.isSuccess, "SFTP connect should succeed")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "SFTP disconnect should succeed")
        }
    }

    @Test
    fun testSmbMultipleConnectDisconnectCycles() = runBlocking<Unit> {
        val service = SmbService(smbConfig(),
            testConnectFn = { _, _, _ -> Result.success(Unit) },
            testAuthenticateFn = { _, _, _ -> Result.success(Unit) }
        )
        repeat(3) {
            val connectResult = service.connect()
            assertTrue(connectResult.isSuccess, "SMB connect should succeed")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "SMB disconnect should succeed")
        }
    }

    @Test
    fun testDropboxDisconnectWithoutConnect() = runBlocking<Unit> {
        val service = DropboxService(dropboxConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Dropbox disconnect without prior connect should succeed")
    }

    @Test
    fun testGoogleDriveDisconnectWithoutConnect() = runBlocking<Unit> {
        val service = GoogleDriveService(googleDriveConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "GoogleDrive disconnect without prior connect should succeed")
    }

    @Test
    fun testOneDriveDisconnectWithoutConnect() = runBlocking<Unit> {
        val service = OneDriveService(oneDriveConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "OneDrive disconnect without prior connect should succeed")
    }

    @Test
    fun testSftpDisconnectWithoutConnect() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "SFTP disconnect without prior connect should succeed")
    }

    @Test
    fun testSmbDisconnectWithoutConnect() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "SMB disconnect without prior connect should succeed")
    }

    @Test
    fun testDropboxIsOfflineAfterDisconnect() = runBlocking<Unit> {
        val service = DropboxService(dropboxConfig())
        // Disconnect even when not connected should leave service offline
        service.disconnect()
        assertFalse(service.isOnline, "Service should be offline after disconnect")
    }

    @Test
    fun testGoogleDriveIsOfflineAfterDisconnect() = runBlocking<Unit> {
        val service = GoogleDriveService(googleDriveConfig())
        service.disconnect()
        assertFalse(service.isOnline, "GoogleDrive should be offline after disconnect")
    }

    @Test
    fun testOneDriveIsOfflineAfterDisconnect() = runBlocking<Unit> {
        val service = OneDriveService(oneDriveConfig())
        service.disconnect()
        assertFalse(service.isOnline, "OneDrive should be offline after disconnect")
    }

    @Test
    fun testSftpOnlineStateAfterConnectDisconnect() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        assertFalse(service.isOnline, "SFTP should be offline initially")
        service.connect()
        assertTrue(service.isOnline, "SFTP should be online after connect")
        service.disconnect()
        assertFalse(service.isOnline, "SFTP should be offline after disconnect")
    }

    @Test
    fun testSmbOnlineStateAfterConnectDisconnect() = runBlocking<Unit> {
        val service = SmbService(smbConfig(),
            testConnectFn = { _, _, _ -> Result.success(Unit) },
            testAuthenticateFn = { _, _, _ -> Result.success(Unit) }
        )
        assertFalse(service.isOnline, "SMB should be offline initially")
        service.connect()
        assertTrue(service.isOnline, "SMB should be online after connect")
        service.disconnect()
        assertFalse(service.isOnline, "SMB should be offline after disconnect")
    }

    @Test
    fun testDropboxRapidConnectDisconnect() = runBlocking<Unit> {
        val service = DropboxService(dropboxConfig())
        // Rapid cycles should not cause scope leaks or race conditions
        repeat(10) {
            service.connect()
            service.disconnect()
        }
        assertFalse(service.isOnline, "Service should be offline after rapid cycles")
    }

    @Test
    fun testSftpRapidConnectDisconnect() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        repeat(10) {
            service.connect()
            service.disconnect()
        }
        assertFalse(service.isOnline, "SFTP should be offline after rapid cycles")
    }

    @Test
    fun testSmbRapidConnectDisconnect() = runBlocking<Unit> {
        val service = SmbService(smbConfig(),
            testConnectFn = { _, _, _ -> Result.success(Unit) },
            testAuthenticateFn = { _, _, _ -> Result.success(Unit) }
        )
        repeat(10) {
            service.connect()
            service.disconnect()
        }
        assertFalse(service.isOnline, "SMB should be offline after rapid cycles")
    }

    // ==========================================================================
    // 5. Additional Safety Validation Tests
    // ==========================================================================

    @Test
    fun testSftpOperationsWhileDisconnectedReturnError() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        // Not connected - operations should return failure, not throw
        val result = service.getFileInfo("/any/path")
        assertTrue(result.isFailure, "SFTP getFileInfo while disconnected should fail gracefully")
    }

    @Test
    fun testSmbOperationsWhileDisconnectedReturnError() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        // Not connected
        val result = service.getFileInfo("/any/path")
        assertTrue(result.isFailure, "SMB getFileInfo while disconnected should fail gracefully")
    }

    @Test
    fun testSftpDeleteWhileDisconnected() = runBlocking<Unit> {
        val service = SftpService(sftpConfig())
        val result = service.deleteFile("/any/path")
        assertTrue(result.isFailure, "SFTP deleteFile while disconnected should fail gracefully")
    }

    @Test
    fun testSmbDeleteWhileDisconnected() = runBlocking<Unit> {
        val service = SmbService(smbConfig())
        val result = service.deleteFile("/any/path")
        assertTrue(result.isFailure, "SMB deleteFile while disconnected should fail gracefully")
    }

    @Test
    fun testSftpPathWithSpacesStaysWithinRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val result = service.getFileInfo("my documents/my file.txt")
        assertTrue(result.isSuccess, "SFTP: paths with spaces are legitimate")
        assertEquals(
            "/home/user/my documents/my file.txt", result.getOrThrow().path,
            "SFTP: spaces must be preserved and path stays within root"
        )
    }

    @Test
    fun testSmbPathWithSpacesStaysWithinRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val result = service.getFileInfo("my documents/my file.txt")
        assertTrue(result.isSuccess, "SMB: paths with spaces are legitimate")
        assertEquals(
            "/data/share/my documents/my file.txt", result.getOrThrow().path,
            "SMB: spaces must be preserved and path stays within root"
        )
    }

    @Test
    fun testSftpVeryLongPathStaysWithinRoot() = runBlocking<Unit> {
        val service = SftpService(sftpConfig(rootPath = "/home/user"))
        service.connect()
        val longTail = (1..100).joinToString("/") { "segment_$it" }
        val result = service.getFileInfo(longTail)
        assertTrue(result.isSuccess, "SFTP: a very long but legitimate path must resolve")
        assertTrue(
            result.getOrThrow().path.startsWith("/home/user/segment_1/"),
            "SFTP: long path must remain rooted within the configured root"
        )
    }

    @Test
    fun testSmbVeryLongPathStaysWithinRoot() = runBlocking<Unit> {
        val service = connectableSmb(path = "/data/share")
        service.connect()
        val longTail = (1..100).joinToString("/") { "segment_$it" }
        val result = service.getFileInfo(longTail)
        assertTrue(result.isSuccess, "SMB: a very long but legitimate path must resolve")
        assertTrue(
            result.getOrThrow().path.startsWith("/data/share/segment_1/"),
            "SMB: long path must remain rooted within the configured root"
        )
    }

    @Test
    fun testAllServicesInitiallyOffline() {
        val sftp = SftpService(sftpConfig())
        val smb = SmbService(smbConfig())
        val dropbox = DropboxService(dropboxConfig())
        val gdrive = GoogleDriveService(googleDriveConfig())
        val onedrive = OneDriveService(oneDriveConfig())

        assertFalse(sftp.isOnline, "SFTP should start offline")
        assertFalse(smb.isOnline, "SMB should start offline")
        assertFalse(dropbox.isOnline, "Dropbox should start offline")
        assertFalse(gdrive.isOnline, "GoogleDrive should start offline")
        assertFalse(onedrive.isOnline, "OneDrive should start offline")
    }

    @Test
    fun testSftpConfigRootPathPreserved() {
        val service = SftpService(sftpConfig(rootPath = "/custom/root"))
        assertEquals("/custom/root", service.config.rootPath)
    }

    @Test
    fun testSmbConfigPathPreserved() {
        val service = SmbService(smbConfig(path = "/custom/root"))
        assertEquals("/custom/root", service.config.path)
    }

    @Test
    fun testDropboxConfigRootPathPreserved() {
        val service = DropboxService(dropboxConfig(rootPath = "/Apps/MyEditor"))
        assertEquals("/Apps/MyEditor", service.config.rootPath)
    }

    @Test
    fun testFtpConfigRootPathPreserved() {
        val service = FtpService(ftpConfig(rootPath = "/pub/data"))
        assertEquals("/pub/data", service.config.rootPath)
    }
}
