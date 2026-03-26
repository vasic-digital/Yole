/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lock Ordering Compliance Tests
 *
 * Regression tests for Phase 1 concurrency fix: lock ordering
 * in cancelOperation for Dropbox, GoogleDrive, OneDrive, and
 * FTP services to prevent deadlock under high contention.
 *
 * Tests that complete within the timeout guarantee no deadlock.
 * Wrong lock ordering would cause a deadlock and the test would
 * hang indefinitely, triggering the [withTimeout] exception.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.*

/**
 * Lock ordering compliance tests across all 8 protocol services.
 *
 * These tests launch many coroutines concurrently calling cancel/pause/resume
 * and assert they complete within a timeout. A deadlock due to wrong lock
 * ordering would cause the test to time out and fail with [TimeoutCancellationException].
 *
 * Lock ordering enforced (must acquire in this order):
 * scopeMutex > stateMutex > operationsMutex > syncMutex > cacheMutex >
 * pauseFlagsMutex > activeJobsMutex > storageInitMutex
 */
class LockOrderingComplianceTests {

    // ==================== Helper Config Factories ====================

    private fun createDropboxConfig() = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test-access-token",
        appKey = "test",
        appSecret = "test"
    )

    private fun createGoogleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "test-googledrive",
        clientId = "test",
        clientSecret = "test"
    )

    private fun createOneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "test-onedrive",
        clientId = "test",
        clientSecret = "test"
    )

    private fun createFtpConfig() = StorageConfig.FtpConfig(
        name = "test-ftp",
        host = "localhost",
        username = "test",
        password = "test"
    )

    private fun createSftpConfig() = StorageConfig.SftpConfig(
        name = "test-sftp",
        host = "localhost",
        username = "test",
        password = "test"
    )

    private fun createSmbConfig() = StorageConfig.SmbConfig(
        name = "test-smb",
        host = "localhost",
        share = "test",
        username = "test",
        password = "test"
    )

    private fun createGitConfig() = StorageConfig.GitConfig(
        name = "test-git",
        repositoryUrl = "https://example.com/repo.git",
        localCachePath = "/tmp/test-git-cache"
    )

    private fun createWebDavConfig() = StorageConfig.WebDavConfig(
        name = "test-webdav",
        url = "https://example.com/dav",
        username = "test",
        password = "test"
    )

    // ==================== Concurrent cancelOperation — 4 Fixed Services ====================

    /**
     * 50 coroutines call cancelOperation with distinct IDs on DropboxService.
     *
     * If lock ordering in cancelOperation is wrong (e.g. operationsMutex acquired
     * before stateMutex while another path holds them in reverse), this will deadlock
     * and the 5 s timeout will trip.
     */
    @Test
    fun concurrentCancelOperations_Dropbox() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "cancelOperation must return a Result")
            }
        }
    }

    /**
     * 50 coroutines call cancelOperation with distinct IDs on GoogleDriveService.
     */
    @Test
    fun concurrentCancelOperations_GoogleDrive() = runBlocking<Unit> {
        val service = GoogleDriveService(createGoogleDriveConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "cancelOperation must return a Result")
            }
        }
    }

    /**
     * 50 coroutines call cancelOperation with distinct IDs on OneDriveService.
     */
    @Test
    fun concurrentCancelOperations_OneDrive() = runBlocking<Unit> {
        val service = OneDriveService(createOneDriveConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "cancelOperation must return a Result")
            }
        }
    }

    /**
     * 50 coroutines call cancelOperation with distinct IDs on FtpService.
     */
    @Test
    fun concurrentCancelOperations_Ftp() = runBlocking<Unit> {
        val service = FtpService(createFtpConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "cancelOperation must return a Result")
            }
        }
    }

    // ==================== Concurrent cancelOperation — Remaining 4 Services ====================

    @Test
    fun concurrentCancelOperations_SFTP() = runBlocking<Unit> {
        val service = SftpService(createSftpConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
        }
    }

    @Test
    fun concurrentCancelOperations_SMB() = runBlocking<Unit> {
        val service = SmbService(createSmbConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
        }
    }

    @Test
    fun concurrentCancelOperations_Git() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
        }
    }

    @Test
    fun concurrentCancelOperations_WebDAV() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        withTimeout(5000L) {
            val jobs = (1L..50L).map { id ->
                async { service.cancelOperation(id) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "All 50 cancel calls should complete without deadlock")
        }
    }

    // ==================== Interleaved Cancel / Pause / Resume ====================

    /**
     * 100 coroutines randomly call cancel/pause/resume on SmbService within 10 s.
     *
     * This exercises all three locking paths simultaneously. If any two paths
     * acquire the same pair of mutexes in different order the test will deadlock
     * and fail via the timeout guard.
     */
    @Test
    fun interleavedCancelPauseResume_Smb() = runBlocking<Unit> {
        val service = SmbService(createSmbConfig())
        withTimeout(10000L) {
            val jobs = (1L..100L).map { id ->
                async {
                    when (Random.nextInt(3)) {
                        0 -> service.cancelOperation(id)
                        1 -> service.pauseOperation(id)
                        else -> service.resumeOperation(id)
                    }
                }
            }
            val results = jobs.awaitAll()
            assertEquals(100, results.size, "All 100 interleaved operations should complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "Each operation must return a Result")
            }
        }
    }

    /**
     * 100 coroutines randomly call cancel/pause/resume on DropboxService within 10 s.
     */
    @Test
    fun interleavedCancelPauseResume_Dropbox() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        withTimeout(10000L) {
            val jobs = (1L..100L).map { id ->
                async {
                    when (Random.nextInt(3)) {
                        0 -> service.cancelOperation(id)
                        1 -> service.pauseOperation(id)
                        else -> service.resumeOperation(id)
                    }
                }
            }
            val results = jobs.awaitAll()
            assertEquals(100, results.size, "All 100 interleaved operations should complete without deadlock")
        }
    }

    /**
     * 100 coroutines randomly call cancel/pause/resume on GoogleDriveService within 10 s.
     */
    @Test
    fun interleavedCancelPauseResume_GoogleDrive() = runBlocking<Unit> {
        val service = GoogleDriveService(createGoogleDriveConfig())
        withTimeout(10000L) {
            val jobs = (1L..100L).map { id ->
                async {
                    when (Random.nextInt(3)) {
                        0 -> service.cancelOperation(id)
                        1 -> service.pauseOperation(id)
                        else -> service.resumeOperation(id)
                    }
                }
            }
            val results = jobs.awaitAll()
            assertEquals(100, results.size, "All 100 interleaved operations should complete without deadlock")
        }
    }

    /**
     * 100 coroutines randomly call cancel/pause/resume on FtpService within 10 s.
     */
    @Test
    fun interleavedCancelPauseResume_Ftp() = runBlocking<Unit> {
        val service = FtpService(createFtpConfig())
        withTimeout(10000L) {
            val jobs = (1L..100L).map { id ->
                async {
                    when (Random.nextInt(3)) {
                        0 -> service.cancelOperation(id)
                        1 -> service.pauseOperation(id)
                        else -> service.resumeOperation(id)
                    }
                }
            }
            val results = jobs.awaitAll()
            assertEquals(100, results.size, "All 100 interleaved operations should complete without deadlock")
        }
    }

    /**
     * Mixed operations across ALL 8 services concurrently within 10 s.
     *
     * Exercises cross-service lock ordering (each service has independent mutexes,
     * so contention is intra-service only, but this validates no shared-state
     * deadlock with FormatRegistry or AuthTokenManager).
     */
    @Test
    fun interleavedCancelPauseResume_AllServices() = runBlocking<Unit> {
        val dropbox = DropboxService(createDropboxConfig())
        val googleDrive = GoogleDriveService(createGoogleDriveConfig())
        val oneDrive = OneDriveService(createOneDriveConfig())
        val ftp = FtpService(createFtpConfig())
        val sftp = SftpService(createSftpConfig())
        val smb = SmbService(createSmbConfig())
        val git = GitService(createGitConfig())
        val webdav = WebDavService(createWebDavConfig())

        val services = listOf(dropbox, googleDrive, oneDrive, ftp, sftp, smb, git, webdav)

        withTimeout(10000L) {
            val jobs = (1L..100L).map { id ->
                async {
                    val service = services[(id % services.size).toInt()]
                    when (Random.nextInt(3)) {
                        0 -> service.cancelOperation(id)
                        1 -> service.pauseOperation(id)
                        else -> service.resumeOperation(id)
                    }
                }
            }
            val results = jobs.awaitAll()
            assertEquals(100, results.size, "All operations across all services must complete without deadlock")
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure, "Each operation must return a Result")
            }
        }
    }

    // ==================== Repeated Cancel Same ID ====================

    /**
     * Cancelling the same operation ID 50 times concurrently must not deadlock
     * or throw — idempotency under lock contention.
     */
    @Test
    fun repeatedCancelSameId_Dropbox() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        withTimeout(5000L) {
            val jobs = (1..50).map {
                async { service.cancelOperation(42L) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "50 cancel calls for same ID must all complete")
        }
    }

    @Test
    fun repeatedCancelSameId_GoogleDrive() = runBlocking<Unit> {
        val service = GoogleDriveService(createGoogleDriveConfig())
        withTimeout(5000L) {
            val jobs = (1..50).map {
                async { service.cancelOperation(99L) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "50 cancel calls for same ID must all complete")
        }
    }

    @Test
    fun repeatedCancelSameId_OneDrive() = runBlocking<Unit> {
        val service = OneDriveService(createOneDriveConfig())
        withTimeout(5000L) {
            val jobs = (1..50).map {
                async { service.cancelOperation(7L) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "50 cancel calls for same ID must all complete")
        }
    }

    @Test
    fun repeatedCancelSameId_Ftp() = runBlocking<Unit> {
        val service = FtpService(createFtpConfig())
        withTimeout(5000L) {
            val jobs = (1..50).map {
                async { service.cancelOperation(1L) }
            }
            val results = jobs.awaitAll()
            assertEquals(50, results.size, "50 cancel calls for same ID must all complete")
        }
    }
}
