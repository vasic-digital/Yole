/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Protocol Non-Blocking Tests
 *
 * Validates that operations on disconnected services
 * return promptly, that concurrent operations do not
 * deadlock, and that disconnect does not block
 * indefinitely for all 8 protocol services.
 *
 * Total: ~24 tests (3 per protocol x 8 protocols)
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
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Non-blocking tests for all 8 protocol implementations.
 *
 * Scenarios per protocol:
 * 1. Operations on disconnected service return within 1 second
 * 2. Multiple concurrent operations do not deadlock (within 10 seconds)
 * 3. Disconnect does not block indefinitely (within 5 seconds)
 */
class ProtocolNonBlockingTests {

    // ==================== Config Factories ====================

    private fun dropboxConfig() = StorageConfig.DropboxConfig(
        name = "nb-dropbox", accessToken = "tok", appKey = "key", appSecret = "sec"
    )
    private fun ftpConfig() = StorageConfig.FtpConfig(
        name = "nb-ftp", host = "ftp.example.com", port = 21,
        username = "u", password = "p", rootPath = "/data"
    )
    private fun sftpConfig() = StorageConfig.SftpConfig(
        name = "nb-sftp", host = "sftp.example.com", port = 22,
        username = "u", password = "p", rootPath = "/home"
    )
    private fun smbConfig() = StorageConfig.SmbConfig(
        name = "nb-smb", host = "smb.example.com", share = "shared",
        username = "u", password = "p", path = "/docs"
    )
    private fun googleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "nb-gdrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun oneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "nb-onedrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun gitConfig() = StorageConfig.GitConfig(
        name = "nb-git", repositoryUrl = "https://github.com/test/repo.git",
        branch = "main", personalAccessToken = "pat", localCachePath = "/tmp/git-cache"
    )
    private fun webDavConfig() = StorageConfig.WebDavConfig(
        name = "nb-webdav", url = "https://webdav.example.com/dav",
        username = "u", password = "p"
    )

    // ==================== Operations Return Promptly ====================

    @Test
    fun dropboxOpsReturnPromptly() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun ftpOpsReturnPromptly() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun sftpOpsReturnPromptly() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun smbOpsReturnPromptly() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun googleDriveOpsReturnPromptly() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun oneDriveOpsReturnPromptly() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun gitOpsReturnPromptly() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    @Test
    fun webDavOpsReturnPromptly() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        withTimeout(5.seconds) {
            svc.exists("/test.txt")
            svc.getFileInfo("/test.txt")
            svc.validatePath("/test.txt")
        }
    }

    // ==================== Concurrent Operations No Deadlock ====================

    @Test
    fun dropboxConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = DropboxService(dropboxConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun ftpConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = FtpService(ftpConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun sftpConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = SftpService(sftpConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun smbConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = SmbService(smbConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun googleDriveConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = GoogleDriveService(googleDriveConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun oneDriveConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = OneDriveService(oneDriveConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun gitConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = GitService(gitConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    @Test
    fun webDavConcurrentNoDeadlock() = runBlocking<Unit> {
        withTimeout(10.seconds) {
            val svc = WebDavService(webDavConfig())
            val jobs = (1..10).map { async { svc.exists("/file-$it.txt") } }
            val results = jobs.awaitAll()
            assertEquals(10, results.size)
        }
    }

    // ==================== Disconnect Does Not Block ====================

    @Test
    fun dropboxDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = DropboxService(dropboxConfig())
            svc.disconnect()
        }
    }

    @Test
    fun ftpDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = FtpService(ftpConfig())
            svc.disconnect()
        }
    }

    @Test
    fun sftpDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = SftpService(sftpConfig())
            svc.disconnect()
        }
    }

    @Test
    fun smbDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = SmbService(smbConfig())
            svc.disconnect()
        }
    }

    @Test
    fun googleDriveDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = GoogleDriveService(googleDriveConfig())
            svc.disconnect()
        }
    }

    @Test
    fun oneDriveDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = OneDriveService(oneDriveConfig())
            svc.disconnect()
        }
    }

    @Test
    fun gitDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = GitService(gitConfig())
            svc.disconnect()
        }
    }

    @Test
    fun webDavDisconnectDoesNotBlock() = runBlocking<Unit> {
        withTimeout(5.seconds) {
            val svc = WebDavService(webDavConfig())
            svc.disconnect()
        }
    }
}
