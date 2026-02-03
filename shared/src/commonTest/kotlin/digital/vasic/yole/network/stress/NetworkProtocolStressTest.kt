/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Network Protocol Stress Tests
 *
 * Comprehensive stress tests for network protocol services
 * covering concurrent access, rapid operations, and edge cases.
 *
 *########################################################*/

package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Stress tests for network protocol services.
 * Tests concurrent access, rapid operations, and edge cases.
 */
class NetworkProtocolStressTest {

    // ==================== FTP STRESS TESTS ====================

    @Test
    fun `FTP concurrent connect disconnect cycles`() = runTest {
        val config = StorageConfig.FtpConfig(
            name = "stress-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = FtpService(config)

        // Run multiple concurrent connect/disconnect cycles
        val jobs = (1..10).map { i ->
            async {
                repeat(5) {
                    service.connect()
                    delay(10)
                    service.disconnect()
                }
                i
            }
        }

        // All jobs should complete without exceptions
        val results = jobs.awaitAll()
        assertEquals(10, results.size)
    }

    @Test
    fun `FTP rapid file info requests`() = runTest {
        val config = StorageConfig.FtpConfig(
            name = "stress-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = FtpService(config)
        service.connect()

        // Rapid file info requests
        val results = (1..100).map { i ->
            async {
                service.getFileInfo("/path/to/file$i.txt")
            }
        }.awaitAll()

        // All requests should succeed
        assertTrue(results.all { it.isSuccess })
        service.disconnect()
    }

    @Test
    fun `FTP concurrent cache operations`() = runTest {
        val config = StorageConfig.FtpConfig(
            name = "stress-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = FtpService(config)

        // Concurrent cache add/remove operations
        val jobs = (1..50).map { i ->
            async {
                service.addToCache("/file$i.txt", i % 10)
                delay(5)
                service.removeFromCache("/file$i.txt")
            }
        }

        jobs.awaitAll()
        // Should complete without exceptions
        assertTrue(service.clearCache().isSuccess)
    }

    // ==================== SMB STRESS TESTS ====================

    @Test
    fun `SMB concurrent connect disconnect cycles`() = runTest {
        val config = StorageConfig.SmbConfig(
            name = "stress-smb",
            host = "192.168.1.100",
            share = "documents",
            domain = "WORKGROUP",
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = SmbService(config)

        // Run multiple concurrent connect/disconnect cycles
        val jobs = (1..10).map { i ->
            async {
                repeat(5) {
                    service.connect()
                    delay(10)
                    service.disconnect()
                }
                i
            }
        }

        val results = jobs.awaitAll()
        assertEquals(10, results.size)
    }

    @Test
    fun `SMB rapid folder operations`() = runTest {
        val config = StorageConfig.SmbConfig(
            name = "stress-smb",
            host = "192.168.1.100",
            share = "documents",
            domain = "WORKGROUP",
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = SmbService(config)
        service.connect()

        // Rapid folder creation
        val results = (1..50).map { i ->
            async {
                service.createFolder("/folder$i")
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
        service.disconnect()
    }

    @Test
    fun `SMB concurrent file move operations`() = runTest {
        val config = StorageConfig.SmbConfig(
            name = "stress-smb",
            host = "192.168.1.100",
            share = "documents",
            domain = "WORKGROUP",
            username = "user",
            password = "pass",
            path = "/"
        )
        val service = SmbService(config)
        service.connect()

        // Concurrent move operations
        val results = (1..30).map { i ->
            async {
                service.moveFile("/source$i.txt", "/dest$i.txt")
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
        service.disconnect()
    }

    // ==================== WEBDAV STRESS TESTS ====================

    @Test
    fun `WebDAV concurrent connect disconnect cycles`() = runTest {
        val config = StorageConfig.WebDavConfig(
            name = "stress-webdav",
            url = "https://webdav.example.com/dav",
            username = "user",
            password = "pass"
        )
        val service = WebDavService(config)

        val jobs = (1..10).map { i ->
            async {
                repeat(5) {
                    service.connect()
                    delay(10)
                    service.disconnect()
                }
                i
            }
        }

        val results = jobs.awaitAll()
        assertEquals(10, results.size)
    }

    @Test
    fun `WebDAV rapid sync operations`() = runTest {
        val config = StorageConfig.WebDavConfig(
            name = "stress-webdav",
            url = "https://webdav.example.com/dav",
            username = "user",
            password = "pass"
        )
        val service = WebDavService(config)

        // Rapid sync file operations
        val results = (1..50).map { i ->
            async {
                service.syncFile("/file$i.txt", forceSync = i % 2 == 0).toList()
            }
        }.awaitAll()

        // All syncs should complete
        assertTrue(results.all { it.isNotEmpty() })
    }

    @Test
    fun `WebDAV concurrent quota checks`() = runTest {
        val config = StorageConfig.WebDavConfig(
            name = "stress-webdav",
            url = "https://webdav.example.com/dav",
            username = "user",
            password = "pass"
        )
        val service = WebDavService(config)

        // Concurrent quota info requests
        val results = (1..30).map {
            async {
                service.getQuotaInfo()
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
    }

    // ==================== GIT STRESS TESTS ====================

    @Test
    fun `Git rapid file operations`() = runTest {
        val config = StorageConfig.GitConfig(
            name = "stress-git",
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "user",
            personalAccessToken = "token"
        )
        val service = GitService(config)

        // Rapid file info requests
        val results = (1..100).map { i ->
            async {
                service.getFileInfo("/src/file$i.kt")
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `Git concurrent folder creation`() = runTest {
        val config = StorageConfig.GitConfig(
            name = "stress-git",
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "user",
            personalAccessToken = "token"
        )
        val service = GitService(config)

        // Concurrent folder creation
        val results = (1..50).map { i ->
            async {
                service.createFolder("/src/package$i")
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `Git path validation stress`() = runTest {
        val config = StorageConfig.GitConfig(
            name = "stress-git",
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "user",
            personalAccessToken = "token"
        )
        val service = GitService(config)

        // Rapid path validations
        val paths = (1..200).map { "/src/main/kotlin/package$it/File$it.kt" }
        val results = paths.map { path ->
            async {
                service.validatePath(path)
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
    }

    // ==================== CROSS-PROTOCOL STRESS TESTS ====================

    @Test
    fun `Multiple protocols concurrent operations`() = runTest {
        val ftpConfig = StorageConfig.FtpConfig(
            name = "ftp", host = "ftp.example.com", port = 21,
            username = "user", password = "pass", path = "/"
        )
        val smbConfig = StorageConfig.SmbConfig(
            name = "smb", host = "192.168.1.100", share = "docs",
            domain = "WORKGROUP", username = "user", password = "pass", path = "/"
        )
        val webdavConfig = StorageConfig.WebDavConfig(
            name = "webdav", url = "https://webdav.example.com",
            username = "user", password = "pass"
        )
        val gitConfig = StorageConfig.GitConfig(
            name = "git", repositoryUrl = "https://github.com/example/repo.git",
            branch = "main", username = "user", personalAccessToken = "token"
        )

        val ftpService = FtpService(ftpConfig)
        val smbService = SmbService(smbConfig)
        val webdavService = WebDavService(webdavConfig)
        val gitService = GitService(gitConfig)

        // Concurrent operations across all protocols
        val jobs = listOf(
            async { ftpService.connect(); ftpService.getStorageInfo() },
            async { smbService.connect(); smbService.getStorageInfo() },
            async { webdavService.connect(); webdavService.getStorageInfo() },
            async { gitService.getStorageInfo() }
        )

        val results = jobs.awaitAll()
        assertEquals(4, results.size)

        // Cleanup
        ftpService.disconnect()
        smbService.disconnect()
        webdavService.disconnect()
        gitService.disconnect()
    }

    @Test
    fun `Rapid protocol switching`() = runTest {
        val ftpConfig = StorageConfig.FtpConfig(
            name = "ftp", host = "ftp.example.com", port = 21,
            username = "user", password = "pass", path = "/"
        )
        val smbConfig = StorageConfig.SmbConfig(
            name = "smb", host = "192.168.1.100", share = "docs",
            domain = "WORKGROUP", username = "user", password = "pass", path = "/"
        )

        val ftpService = FtpService(ftpConfig)
        val smbService = SmbService(smbConfig)

        // Rapid switching between protocols
        repeat(20) { i ->
            if (i % 2 == 0) {
                ftpService.connect()
                ftpService.getFileInfo("/file.txt")
                ftpService.disconnect()
            } else {
                smbService.connect()
                smbService.getFileInfo("/file.txt")
                smbService.disconnect()
            }
        }

        // Both services should be disconnected
        assertFalse(ftpService.isOnline)
        assertFalse(smbService.isOnline)
    }

    // ==================== EDGE CASE STRESS TESTS ====================

    @Test
    fun `Stress test with empty paths`() = runTest {
        val config = StorageConfig.FtpConfig(
            name = "ftp", host = "ftp.example.com", port = 21,
            username = "user", password = "pass", path = "/"
        )
        val service = FtpService(config)

        // Multiple operations with edge case paths
        val results = (1..50).map {
            async {
                listOf(
                    service.validatePath("/"),
                    service.getParentPath("/"),
                    service.exists("/")
                )
            }
        }.awaitAll()

        assertEquals(50, results.size)
    }

    @Test
    fun `Stress test with special characters in paths`() = runTest {
        val config = StorageConfig.WebDavConfig(
            name = "webdav", url = "https://webdav.example.com",
            username = "user", password = "pass"
        )
        val service = WebDavService(config)
        service.connect()

        val specialPaths = listOf(
            "/path with spaces/file.txt",
            "/path-with-dashes/file.txt",
            "/path_with_underscores/file.txt",
            "/path.with.dots/file.txt",
            "/unicode/文档/file.txt",
            "/unicode/файлы/file.txt"
        )

        // Rapid operations with special characters
        val results = (1..20).flatMap { _ ->
            specialPaths.map { path ->
                async { service.getFileInfo(path) }
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
        service.disconnect()
    }

    @Test
    fun `Stress test connection state consistency`() = runTest {
        val config = StorageConfig.SmbConfig(
            name = "smb", host = "192.168.1.100", share = "docs",
            domain = "WORKGROUP", username = "user", password = "pass", path = "/"
        )
        val service = SmbService(config)

        // Rapid state checks during connect/disconnect
        val stateChecks = mutableListOf<Boolean>()

        repeat(50) {
            service.connect()
            stateChecks.add(service.isOnline)
            service.disconnect()
            stateChecks.add(!service.isOnline)
        }

        // All state checks should be true (online after connect, offline after disconnect)
        assertTrue(stateChecks.all { it })
    }
}
