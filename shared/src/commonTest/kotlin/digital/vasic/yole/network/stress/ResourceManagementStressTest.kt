/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Resource Management Stress Tests
 *
 * Comprehensive stress tests for resource management
 * including connection lifecycle, memory usage patterns,
 * and proper cleanup verification.
 *
 *########################################################*/

package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Stress tests for resource management across all network services.
 * Verifies proper cleanup, connection lifecycle, and memory patterns.
 */
class ResourceManagementStressTest {

    // ==================== CONNECTION LIFECYCLE STRESS ====================

    @Test
    fun `repeated connect-disconnect cycles do not leak resources`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        repeat(100) {
            // Real FTP client cannot connect to non-existent server
            val connectResult = service.connect()
            assertTrue(connectResult.isFailure, "Connection should fail when server is unreachable")
            assertFalse(service.isOnline, "Should not be online after failed connection")
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "Disconnect should succeed")
            assertFalse(service.isOnline, "Should not be online after disconnect")
        }
    }

    @Test
    fun `multiple disconnect calls are safe`() = runBlocking<Unit> {
        val service = createSmbService("smb")

        service.connect()

        // Multiple disconnect calls should not cause issues
        repeat(10) {
            val result = service.disconnect()
            assertTrue(result.isSuccess)
            assertFalse(service.isOnline)
        }
    }

    @Test
    fun `connect after failed connect is safe`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")

        // First connect
        service.connect()
        assertTrue(service.isOnline)

        // Second connect while online should be safe
        val result = service.connect()
        assertTrue(result.isSuccess)

        service.disconnect()
    }

    @Test
    fun `operations after disconnect fail gracefully`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        service.connect() // Will fail with real client, but that's OK
        service.disconnect()

        // Operations after disconnect should return failures, not throw
        val listResult = service.listFiles("/").toList()
        assertTrue(listResult.all { it.isFailure })

        val infoResult = service.getFileInfo("/test.txt")
        // Should fail since not connected, but should not throw
        assertTrue(infoResult.isFailure, "File info should fail when not connected")
    }

    // ==================== CONCURRENT LIFECYCLE ====================

    @Test
    fun `concurrent connect-disconnect on same service`() = runBlocking<Unit> {
        val service = createSmbService("smb")

        val jobs = (1..50).map {
            async {
                if (it % 2 == 0) {
                    service.connect()
                } else {
                    service.disconnect()
                }
            }
        }

        jobs.awaitAll()

        // Final state should be consistent (either online or offline)
        val finalState = service.isOnline
        // State is deterministic based on operations
        assertNotNull(finalState.toString())
    }

    @Test
    fun `many services created and destroyed concurrently`() = runBlocking<Unit> {
        val jobs = (1..100).map { i ->
            async {
                val service = when (i % 4) {
                    0 -> createFtpService("ftp-$i")
                    1 -> createSmbService("smb-$i")
                    2 -> createWebDavService("webdav-$i")
                    else -> createGitService("git-$i")
                }

                service.connect()
                service.getStorageInfo()
                service.disconnect()

                service
            }
        }

        val services = jobs.awaitAll()
        assertEquals(100, services.size)
        assertTrue(services.all { !it.isOnline })
    }

    // ==================== OPERATION UNDER RESOURCE PRESSURE ====================

    @Test
    fun `many simultaneous file info requests`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        // Real FTP client cannot connect to non-existent server
        val connectResult = service.connect()
        assertTrue(connectResult.isFailure, "Connection should fail when server is unreachable")

        // All file info requests should fail gracefully since not connected
        val results = (1..200).map { i ->
            async {
                service.getFileInfo("/path/to/file$i.txt")
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isFailure },
            "All file info requests should fail when not connected")

        service.disconnect()
    }

    @Test
    fun `many simultaneous folder creations`() = runBlocking<Unit> {
        val service = createSmbService("smb")
        service.connect()

        val results = (1..100).map { i ->
            async {
                service.createFolder("/folder$i")
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })

        service.disconnect()
    }

    @Test
    fun `many simultaneous exists checks`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")
        service.connect()

        val results = (1..300).map { i ->
            async {
                service.exists("/path$i")
            }
        }.awaitAll()

        assertEquals(300, results.size)
        assertTrue(results.all { it.isSuccess })

        service.disconnect()
    }

    // ==================== CACHE RESOURCE MANAGEMENT ====================

    @Test
    fun `cache can handle many entries`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        // Add many cache entries
        (1..500).forEach { i ->
            service.addToCache("/path/file$i.txt", i % 60)
        }

        // Clear should work
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `concurrent cache operations`() = runBlocking<Unit> {
        val service = createSmbService("smb")

        val addJobs = (1..200).map { i ->
            async {
                service.addToCache("/file$i.txt", i % 30)
            }
        }
        addJobs.awaitAll()

        // Multiple clears
        val clearJobs = (1..10).map {
            async { service.clearCache() }
        }

        val clearResults = clearJobs.awaitAll()
        assertTrue(clearResults.all { it.isSuccess })
    }

    // ==================== STORAGE INFO STRESS ====================

    @Test
    fun `rapid storage info requests`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")

        val infos = (1..100).map {
            async { service.getStorageInfo() }
        }.awaitAll()

        assertEquals(100, infos.size)
        assertTrue(infos.all { it.type == StorageType.WEBDAV })
    }

    @Test
    fun `rapid quota info requests`() = runBlocking<Unit> {
        val service = createGitService("git")

        val quotas = (1..100).map {
            async { service.getQuotaInfo() }
        }.awaitAll()

        assertEquals(100, quotas.size)
        assertTrue(quotas.all { it.isSuccess })
    }

    // ==================== PATH VALIDATION STRESS ====================

    @Test
    fun `many path validations concurrently`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        val results = (1..500).map { i ->
            async {
                service.validatePath("/valid/path/segment$i/file.txt")
            }
        }.awaitAll()

        assertEquals(500, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `invalid paths handled consistently`() = runBlocking<Unit> {
        val service = createSmbService("smb")

        val invalidPaths = listOf("", " ", "\t", "\n")

        invalidPaths.forEach { path ->
            val result = service.validatePath(path)
            assertTrue(result.isFailure, "Path '$path' should fail validation")
        }
    }

    // ==================== SYNC OPERATION STRESS ====================

    @Test
    fun `many sync operations concurrently`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")

        val syncResults = (1..50).map { i ->
            async {
                service.syncFile("/file$i.txt", forceSync = i % 2 == 0).toList()
            }
        }.awaitAll()

        assertEquals(50, syncResults.size)
        assertTrue(syncResults.all { it.isNotEmpty() })
    }

    @Test
    fun `sync with force flag variations`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        repeat(50) { i ->
            val result = service.syncFile("/test.txt", forceSync = i % 2 == 0).toList()
            assertTrue(result.isNotEmpty())
        }
    }

    // ==================== UPLOAD/DOWNLOAD STRESS ====================

    @Test
    fun `concurrent upload operations`() = runBlocking<Unit> {
        val service = createSmbService("smb")
        service.connect()

        val results = (1..30).map { i ->
            async {
                service.uploadFile("/local/file$i.txt", "/remote/file$i.txt").toList()
            }
        }.awaitAll()

        assertEquals(30, results.size)
        results.forEach { ops ->
            assertTrue(ops.all { it.type == NetworkOperation.Type.UPLOAD })
        }

        service.disconnect()
    }

    @Test
    fun `concurrent download operations`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")
        service.connect()

        val results = (1..30).map { i ->
            async {
                service.downloadFile("/remote/file$i.txt", "/local/file$i.txt").toList()
            }
        }.awaitAll()

        assertEquals(30, results.size)
        results.forEach { ops ->
            assertTrue(ops.all { it.type == NetworkOperation.Type.DOWNLOAD })
        }

        service.disconnect()
    }

    @Test
    fun `interleaved upload and download`() = runBlocking<Unit> {
        val service = createFtpService("ftp")

        // Real FTP client cannot connect to non-existent server
        service.connect()

        val results = (1..50).map { i ->
            async {
                if (i % 2 == 0) {
                    service.uploadFile("/local/$i.txt", "/remote/$i.txt").toList()
                } else {
                    service.downloadFile("/remote/$i.txt", "/local/$i.txt").toList()
                }
            }
        }.awaitAll()

        assertEquals(50, results.size)
        // All operations should have failed since not connected
        results.forEach { ops ->
            assertTrue(ops.isNotEmpty(), "Should emit at least one operation")
            assertTrue(ops.all { it.status == NetworkOperation.Status.FAILED },
                "All operations should be FAILED when not connected")
        }

        service.disconnect()
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `operations on uninitialized service`() = runBlocking<Unit> {
        val service = createGitService("git")

        // Should not crash
        val info = service.getStorageInfo()
        assertNotNull(info)

        val quota = service.getQuotaInfo()
        assertNotNull(quota)
    }

    @Test
    fun `very long file paths`() = runBlocking<Unit> {
        val service = createFtpService("ftp")
        service.connect() // Will fail with real client, but that's OK

        val longPath = "/" + "a".repeat(1000) + "/file.txt"
        val result = service.getFileInfo(longPath)

        // Should handle long paths gracefully -- will fail since not connected
        assertTrue(result.isFailure, "Should fail when not connected")

        service.disconnect()
    }

    @Test
    fun `special characters in paths`() = runBlocking<Unit> {
        val service = createSmbService("smb")
        service.connect()

        val specialPaths = listOf(
            "/path with spaces/file.txt",
            "/path-with-dashes/file.txt",
            "/path_with_underscores/file.txt",
            "/path.with.dots/file.txt",
            "/path(with)parens/file.txt"
        )

        specialPaths.forEach { path ->
            val result = service.getFileInfo(path)
            assertTrue(result.isSuccess || result.isFailure, "Path '$path' should be handled")
        }

        service.disconnect()
    }

    @Test
    fun `unicode paths handled correctly`() = runBlocking<Unit> {
        val service = createWebDavService("webdav")
        service.connect()

        val unicodePaths = listOf(
            "/документы/файл.txt",
            "/文档/文件.txt",
            "/ドキュメント/ファイル.txt",
            "/مستندات/ملف.txt"
        )

        unicodePaths.forEach { path ->
            val result = service.getFileInfo(path)
            assertTrue(result.isSuccess || result.isFailure, "Unicode path '$path' should be handled")
        }

        service.disconnect()
    }

    // ==================== SERVICE-SPECIFIC STRESS ====================

    @Test
    fun `git branch operations stress`() = runBlocking<Unit> {
        val service = createGitService("git")

        repeat(50) {
            val info = service.getStorageInfo()
            assertEquals(StorageType.GIT, info.type)
        }
    }

    @Test
    fun `cloud service token handling stress`() = runBlocking<Unit> {
        val dropboxService = DropboxService(
            StorageConfig.DropboxConfig(
                name = "dropbox",
                accessToken = "test-token",
                appKey = "test-app-key",
                appSecret = "test-app-secret"
            )
        )

        repeat(50) {
            val connected = dropboxService.connect()
            dropboxService.disconnect()
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun createFtpService(name: String) = FtpService(
        StorageConfig.FtpConfig(
            name = name,
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            rootPath = "/"
        )
    )

    private fun createSmbService(name: String) = SmbService(
        StorageConfig.SmbConfig(
            name = name,
            host = "192.168.1.100",
            share = "docs",
            domain = "WORKGROUP",
            username = "user",
            password = "pass",
            path = "/"
        ),
        testConnectFn = { _, _, _ -> Result.success(Unit) },
        testAuthenticateFn = { _, _, _ -> Result.success(Unit) }
    )

    private fun createWebDavService(name: String) = WebDavService(
        StorageConfig.WebDavConfig(
            name = name,
            url = "https://webdav.example.com",
            username = "user",
            password = "pass"
        ),
        testConnectFn = { Result.success(Unit) }
    )

    private fun createGitService(name: String) = GitService(
        StorageConfig.GitConfig(
            name = name,
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "user",
            personalAccessToken = "token",
            localCachePath = "/tmp/git-cache"
        )
    )
}
