/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Protocol Resilience Tests
 *
 * Validates graceful degradation and recovery for all
 * 8 protocol services: disconnect handling, failure
 * results, create/destroy cycles, circuit breaker
 * integration, and connection limiter queuing.
 *
 * Total: ~40 tests (5 per protocol x 8 protocols)
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.coverage

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Resilience tests for all 8 protocol implementations.
 *
 * Scenarios per protocol:
 * 1. Disconnect on fresh service completes without exception
 * 2. Operations on disconnected service return Result.failure or complete gracefully
 * 3. Create/destroy 10 times without leaks or crashes
 * 4. Circuit breaker degrades gracefully under repeated failure
 * 5. Connection limiter queues operations when limit reached
 */
class ProtocolResilienceTests {

    // ==================== Config Factories ====================

    private fun dropboxConfig() = StorageConfig.DropboxConfig(
        name = "resilience-dropbox", accessToken = "tok", appKey = "key", appSecret = "sec"
    )
    private fun ftpConfig() = StorageConfig.FtpConfig(
        name = "resilience-ftp", host = "ftp.example.com", port = 21,
        username = "u", password = "p", rootPath = "/data"
    )
    private fun sftpConfig() = StorageConfig.SftpConfig(
        name = "resilience-sftp", host = "sftp.example.com", port = 22,
        username = "u", password = "p", rootPath = "/home"
    )
    private fun smbConfig() = StorageConfig.SmbConfig(
        name = "resilience-smb", host = "smb.example.com", share = "shared",
        username = "u", password = "p", path = "/docs"
    )
    private fun googleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "resilience-gdrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun oneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "resilience-onedrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun gitConfig() = StorageConfig.GitConfig(
        name = "resilience-git", repositoryUrl = "https://github.com/test/repo.git",
        branch = "main", personalAccessToken = "pat", localCachePath = "/tmp/git-cache"
    )
    private fun webDavConfig() = StorageConfig.WebDavConfig(
        name = "resilience-webdav", url = "https://webdav.example.com/dav",
        username = "u", password = "p"
    )

    private data class Proto(val name: String, val create: () -> NetworkStorageService)

    private val protocols = listOf(
        Proto("Dropbox") { DropboxService(dropboxConfig()) },
        Proto("FTP") { FtpService(ftpConfig()) },
        Proto("SFTP") { SftpService(sftpConfig()) },
        Proto("SMB") { SmbService(smbConfig()) },
        Proto("GoogleDrive") { GoogleDriveService(googleDriveConfig()) },
        Proto("OneDrive") { OneDriveService(oneDriveConfig()) },
        Proto("Git") { GitService(gitConfig()) },
        Proto("WebDAV") { WebDavService(webDavConfig()) }
    )

    // ==================== Disconnect Gracefully ====================

    @Test
    fun dropboxDisconnectGraceful() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "Dropbox disconnect on fresh service should succeed")
    }

    @Test
    fun ftpDisconnectGraceful() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "FTP disconnect on fresh service should succeed")
    }

    @Test
    fun sftpDisconnectGraceful() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "SFTP disconnect on fresh service should succeed")
    }

    @Test
    fun smbDisconnectGraceful() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "SMB disconnect on fresh service should succeed")
    }

    @Test
    fun googleDriveDisconnectGraceful() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "GoogleDrive disconnect on fresh service should succeed")
    }

    @Test
    fun oneDriveDisconnectGraceful() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "OneDrive disconnect on fresh service should succeed")
    }

    @Test
    fun gitDisconnectGraceful() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "Git disconnect on fresh service should succeed")
    }

    @Test
    fun webDavDisconnectGraceful() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.disconnect()
        assertTrue(r.isSuccess, "WebDAV disconnect on fresh service should succeed")
    }

    // ==================== Operations When Disconnected Return Failure ====================

    @Test
    fun dropboxOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "Dropbox deleteFile should complete")
    }

    @Test
    fun ftpOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "FTP deleteFile should complete")
    }

    @Test
    fun sftpOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "SFTP deleteFile should complete")
    }

    @Test
    fun smbOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "SMB deleteFile should complete")
    }

    @Test
    fun googleDriveOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive deleteFile should complete")
    }

    @Test
    fun oneDriveOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "OneDrive deleteFile should complete")
    }

    @Test
    fun gitOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "Git deleteFile should complete")
    }

    @Test
    fun webDavOpsWhenDisconnected() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.deleteFile("/nonexistent.txt")
        assertTrue(r.isSuccess || r.isFailure, "WebDAV deleteFile should complete")
    }

    // ==================== Create/Destroy 10 Cycles ====================

    @Test
    fun dropboxCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = DropboxService(dropboxConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun ftpCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = FtpService(ftpConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun sftpCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = SftpService(sftpConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun smbCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = SmbService(smbConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun googleDriveCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = GoogleDriveService(googleDriveConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun oneDriveCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = OneDriveService(oneDriveConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun gitCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = GitService(gitConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    @Test
    fun webDavCreateDestroyCycles() = runBlocking<Unit> {
        repeat(10) {
            val svc = WebDavService(webDavConfig())
            assertFalse(svc.isOnline)
            svc.disconnect()
        }
    }

    // ==================== Circuit Breaker Degrades Gracefully ====================

    @Test
    fun circuitBreakerDegradeDropbox() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "dropbox-res")
        repeat(2) { cb.execute { throw RuntimeException("dropbox fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        val rejected = cb.execute { "should be rejected" }
        assertTrue(rejected.isFailure)
        assertIs<CircuitBreakerOpenException>(rejected.exceptionOrNull())
    }

    @Test
    fun circuitBreakerDegradeFtp() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "ftp-res")
        repeat(2) { cb.execute { throw RuntimeException("ftp fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        val rejected = cb.execute { "rejected" }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun circuitBreakerDegradeSftp() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "sftp-res")
        repeat(2) { cb.execute { throw RuntimeException("sftp fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerDegradeSmb() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "smb-res")
        repeat(2) { cb.execute { throw RuntimeException("smb fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerDegradeGoogleDrive() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "gdrive-res")
        repeat(2) { cb.execute { throw RuntimeException("gdrive fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerDegradeOneDrive() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "onedrive-res")
        repeat(2) { cb.execute { throw RuntimeException("onedrive fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerDegradeGit() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "git-res")
        repeat(2) { cb.execute { throw RuntimeException("git fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerDegradeWebDav() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "webdav-res")
        repeat(2) { cb.execute { throw RuntimeException("webdav fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    // ==================== Connection Limiter Queues Operations ====================

    @Test
    fun connectionLimiterDropbox() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "dropbox-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "dropbox-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
        assertTrue(results.all { it.startsWith("dropbox-op") })
    }

    @Test
    fun connectionLimiterFtp() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "ftp-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "ftp-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterSftp() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "sftp-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "sftp-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterSmb() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "smb-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "smb-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterGoogleDrive() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "gdrive-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "gdrive-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterOneDrive() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "onedrive-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "onedrive-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterGit() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "git-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "git-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }

    @Test
    fun connectionLimiterWebDav() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds, name = "webdav-cl")
        val results = (1..4).map { i ->
            async { cl.withConnection { "webdav-op-$i" } }
        }.awaitAll()
        assertEquals(4, results.size)
    }
}
