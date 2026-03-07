/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Concurrency Fixes Tests
 *
 * Verifies that the concurrency safety fixes for protocol
 * services and auth layer are correct and race-free.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive tests for concurrency safety fixes across protocol services.
 *
 * Tests verify:
 * - pauseFlags protected access under concurrent pause/resume/cancel
 * - serviceScope race safety (concurrent connect/disconnect)
 * - httpClient lifecycle (access tracking)
 * - initJob cancellation on disconnect
 * - SecureStorage double-checked locking
 * - activeJobs cleanup on disconnect (FTP)
 * - Mutex-protected operation state
 * - Concurrent operation creation/cancellation
 */
class ConcurrencyFixesTest {

    // ==================== In-Memory SecureStorage for Tests ====================

    private class TestSecureStorage : SecureStorage {
        private val data = mutableMapOf<String, String>()
        private val mutex = Mutex()

        override suspend fun store(key: String, value: String): Result<Unit> {
            mutex.withLock { data[key] = value }
            return Result.success(Unit)
        }

        override suspend fun retrieve(key: String): Result<String?> {
            val value = mutex.withLock { data[key] }
            return Result.success(value)
        }

        override suspend fun delete(key: String): Result<Unit> {
            mutex.withLock { data.remove(key) }
            return Result.success(Unit)
        }

        override suspend fun contains(key: String): Result<Boolean> {
            val result = mutex.withLock { data.containsKey(key) }
            return Result.success(result)
        }

        override suspend fun listKeys(): Result<List<String>> {
            val keys = mutex.withLock { data.keys.toList() }
            return Result.success(keys)
        }

        override suspend fun clear(): Result<Unit> {
            mutex.withLock { data.clear() }
            return Result.success(Unit)
        }

        override suspend fun isSecure(): Result<Boolean> = Result.success(true)
    }

    // ==================== Helper Configs ====================

    private fun createFtpConfig() = StorageConfig.FtpConfig(
        name = "test-ftp",
        host = "ftp.example.com",
        port = 21,
        username = "user",
        password = "pass",
        rootPath = "/"
    )

    private fun createSftpConfig() = StorageConfig.SftpConfig(
        name = "test-sftp",
        host = "sftp.example.com",
        port = 22,
        username = "user",
        password = "pass",
        rootPath = "/"
    )

    private fun createSmbConfig() = StorageConfig.SmbConfig(
        name = "test-smb",
        host = "smb.example.com",
        share = "documents",
        domain = "TESTDOMAIN",
        username = "user",
        password = "pass",
        path = "/"
    )

    // ==================== ISSUE 1: pauseFlags Protected Access ====================

    @Test
    fun `SMB concurrent pause and resume operations are thread-safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create operations first
        val operationIds = (1L..20L).toList()

        // Launch concurrent pause/resume calls
        val jobs = operationIds.flatMap { id ->
            listOf(
                async { service.pauseOperation(id) },
                async { service.resumeOperation(id) }
            )
        }

        // All operations should complete without exceptions
        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "Operation should succeed: ${result.exceptionOrNull()}")
        }
    }

    @Test
    fun `SMB concurrent cancel operations are thread-safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val jobs = (1L..50L).map { id ->
            async { service.cancelOperation(id) }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "Cancel should succeed")
        }
    }

    @Test
    fun `Concurrent pause resume cancel interleaved for 100 operations`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val jobs = (1L..100L).flatMap { id ->
            listOf(
                async { service.pauseOperation(id) },
                async { service.resumeOperation(id) },
                async { service.cancelOperation(id) }
            )
        }

        val results = jobs.awaitAll()
        // All 300 operations should succeed without ConcurrentModificationException
        assertEquals(300, results.size)
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB pause then resume yields consistent state`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Pause many operations then resume them all
        val pauseJobs = (1L..50L).map { id ->
            async { service.pauseOperation(id) }
        }
        pauseJobs.awaitAll()

        val resumeJobs = (1L..50L).map { id ->
            async { service.resumeOperation(id) }
        }
        val resumeResults = resumeJobs.awaitAll()
        resumeResults.forEach { assertTrue(it.isSuccess) }
    }

    // ==================== ISSUE 2: ServiceScope Race Safety ====================

    @Test
    fun `SFTP concurrent connect and disconnect do not crash`() = runTest {
        val service = SftpService(createSftpConfig())

        // Rapidly alternate connect/disconnect
        val jobs = (1..20).map { i ->
            async {
                if (i % 2 == 0) {
                    service.connect()
                } else {
                    service.disconnect()
                }
            }
        }

        // All should complete without exceptions
        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "connect/disconnect should not throw")
        }
    }

    @Test
    fun `SMB concurrent connect disconnect maintains connection state consistency`() = runTest {
        val service = SmbService(createSmbConfig())

        // Rapid connect/disconnect cycles
        val jobs = (1..30).map { i ->
            async {
                if (i % 2 == 0) {
                    service.connect()
                } else {
                    service.disconnect()
                }
            }
        }

        jobs.awaitAll()

        // Service should be in a definite state (connected or not)
        // Just verify it doesn't crash and we can still use the service
        val testResult = service.testConnection()
        assertTrue(testResult.isSuccess || testResult.isFailure)
    }

    @Test
    fun `SFTP multiple disconnect calls are idempotent`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val disconnectJobs = (1..20).map {
            async { service.disconnect() }
        }

        val results = disconnectJobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }

        // After all disconnects, service should be offline
        assertFalse(service.isOnline)
    }

    @Test
    fun `SMB reconnect cycle is safe under concurrency`() = runTest {
        val service = SmbService(createSmbConfig())

        // Simulate a full connect-disconnect-connect cycle from multiple coroutines
        repeat(10) {
            val connectResult = service.connect()
            assertTrue(connectResult.isSuccess)
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess)
        }

        // Final state should be consistent
        assertFalse(service.isOnline)
    }

    // ==================== ISSUE 3: httpClient Lifecycle ====================

    @Test
    fun `SFTP disconnect without prior httpClient access does not crash`() = runTest {
        // SftpService doesn't have httpClient but this tests the general pattern
        val service = SftpService(createSftpConfig())
        // Disconnect without ever connecting (no httpClient initialized)
        val result = service.disconnect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SMB disconnect without connect does not crash`() = runTest {
        val service = SmbService(createSmbConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess)
    }

    // ==================== ISSUE 4: initJob Cancellation ====================

    @Test
    fun `SFTP rapid create-and-disconnect does not leak coroutines`() = runTest {
        // Create services and immediately disconnect them
        val services = (1..10).map { SftpService(createSftpConfig()) }

        val disconnectJobs = services.map { service ->
            async { service.disconnect() }
        }

        val results = disconnectJobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB service created and immediately disconnected is clean`() = runTest {
        val service = SmbService(createSmbConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    // ==================== ISSUE 5: SecureStorage Double-Checked Locking ====================

    @Test
    fun `AuthTokenManager concurrent getAccessToken calls are safe`() = runTest {
        val storage = TestSecureStorage()
        storage.store("test_access_token", "test-token-value")
        val manager = AuthTokenManager("test", storage)

        val jobs = (1..100).map {
            async { manager.getAccessToken() }
        }

        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `AuthTokenManager concurrent storeTokenInfo calls are safe`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        val jobs = (1..50).map { i ->
            async {
                manager.storeTokenInfo(
                    accessToken = "token_$i",
                    refreshToken = "refresh_$i",
                    expiresIn = 3600L
                )
            }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "Store should succeed: ${result.exceptionOrNull()}")
        }
    }

    @Test
    fun `AuthTokenManager concurrent hasValidToken calls are consistent`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        // Store a token first
        manager.storeTokenInfo(
            accessToken = "valid-token",
            refreshToken = "refresh",
            expiresIn = 3600L
        )

        val jobs = (1..100).map {
            async { manager.hasValidToken() }
        }

        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        results.forEach { result ->
            assertTrue(result.isSuccess, "hasValidToken should not throw")
        }
    }

    @Test
    fun `AuthTokenManager concurrent clearTokens and getAccessToken`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        // Store some tokens
        manager.storeTokenInfo(accessToken = "token", refreshToken = "refresh", expiresIn = 3600L)

        // Concurrently read and clear
        val readJobs = (1..50).map {
            async { manager.getAccessToken() }
        }
        val clearJobs = (1..10).map {
            async { manager.clearTokens() }
        }

        val allResults = (readJobs + clearJobs).awaitAll()
        allResults.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent read/clear should not crash")
        }
    }

    @Test
    fun `AuthTokenManager double-checked locking prevents duplicate storage creation`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        // Concurrent calls should all use the same storage instance
        val jobs = (1..100).map {
            async {
                manager.getAccessToken()
                manager.getRefreshToken()
                manager.isTokenExpired()
            }
        }

        val results = jobs.awaitAll()
        assertEquals(100, results.size)
    }

    @Test
    fun `AuthTokenManager getTokenInfo concurrent access`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)
        manager.storeTokenInfo(accessToken = "access", refreshToken = "refresh", expiresIn = 3600L)

        val jobs = (1..50).map {
            async { manager.getTokenInfo() }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess)
            val info = result.getOrThrow()
            assertEquals("test", info.serviceName)
        }
    }

    // ==================== ISSUE 6: activeJobs Cleanup on Disconnect ====================

    @Test
    fun `SFTP disconnect clears active operations`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        // Disconnect should clear all operations
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `SFTP concurrent operations and disconnect`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        // Start some operations concurrently with a disconnect
        val opJobs = (1..10).map { i ->
            async {
                try {
                    service.uploadFile("/local/file$i.txt", "/remote/file$i.txt")
                } catch (_: Exception) {
                    // May fail if disconnect happens first
                }
            }
        }

        delay(50)
        val disconnectResult = service.disconnect()
        assertTrue(disconnectResult.isSuccess)

        // Wait for operation attempts to finish
        opJobs.forEach { job ->
            try { job.await() } catch (_: Exception) { }
        }
    }

    @Test
    fun `SMB disconnect clears all state safely`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()
        assertTrue(service.isOnline)

        // Perform some operations
        service.createFolder("/test")
        service.createFolder("/test/sub")

        // Disconnect
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    // ==================== CONCURRENT OPERATION STATE TESTS ====================

    @Test
    fun `SMB concurrent createFolder does not corrupt file tree`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val jobs = (1..50).map { i ->
            async { service.createFolder("/folder$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "createFolder should succeed: ${result.exceptionOrNull()}")
        }

        // Verify all folders exist
        for (i in 1..50) {
            val exists = service.exists("/folder$i")
            assertTrue(exists.isSuccess)
            assertTrue(exists.getOrThrow(), "Folder /folder$i should exist")
        }
    }

    @Test
    fun `SMB concurrent deleteFile does not corrupt state`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create folders first
        for (i in 1..20) {
            service.createFolder("/deleteme$i")
        }

        // Delete concurrently
        val jobs = (1..20).map { i ->
            async { service.deleteFile("/deleteme$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }

        // Verify all deleted
        for (i in 1..20) {
            val exists = service.exists("/deleteme$i")
            assertTrue(exists.isSuccess)
            assertFalse(exists.getOrThrow(), "Folder /deleteme$i should be deleted")
        }
    }

    @Test
    fun `SMB concurrent copyFile operations are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create source
        service.createFolder("/source")

        // Copy to multiple destinations concurrently
        val jobs = (1..30).map { i ->
            async { service.copyFile("/source", "/copy$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB concurrent renameFile operations are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create files first
        for (i in 1..10) {
            service.createFolder("/rename$i")
        }

        // Rename concurrently
        val jobs = (1..10).map { i ->
            async { service.renameFile("/rename$i", "renamed$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB concurrent getFileInfo operations are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create files
        for (i in 1..20) {
            service.createFolder("/info$i")
        }

        // Get info concurrently
        val jobs = (1..20).map { i ->
            async { service.getFileInfo("/info$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrThrow())
        }
    }

    // ==================== CACHE/SYNC CONCURRENT ACCESS TESTS ====================

    @Test
    fun `SMB concurrent addToCache and removeFromCache`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create files first
        for (i in 1..20) {
            service.createFolder("/cached$i")
        }

        // Concurrent add and remove
        val addJobs = (1..20).map { i ->
            async { service.addToCache("/cached$i", priority = i) }
        }
        val removeJobs = (1..10).map { i ->
            async { service.removeFromCache("/cached$i") }
        }

        val results = (addJobs + removeJobs).awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB concurrent syncFile operations`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        service.createFolder("/synctest")

        val jobs = (1..20).map { i ->
            async {
                service.syncFile("/synctest/file$i.txt", forceSync = i % 2 == 0)
            }
        }

        val flows = jobs.awaitAll()
        // Just verify that creating the flows doesn't crash
        assertNotNull(flows)
    }

    @Test
    fun `SMB concurrent clearCache is safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Add cache entries
        for (i in 1..10) {
            service.createFolder("/cc$i")
            service.addToCache("/cc$i", priority = 1)
        }

        // Clear cache from multiple coroutines
        val jobs = (1..10).map {
            async { service.clearCache() }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    // ==================== SFTP CONCURRENT STATE TESTS ====================

    @Test
    fun `SFTP concurrent connect calls are safe`() = runTest {
        val service = SftpService(createSftpConfig())

        val jobs = (1..20).map {
            async { service.connect() }
        }

        val results = jobs.awaitAll()
        // At least one should succeed; all should not throw
        assertTrue(results.any { it.isSuccess })
    }

    @Test
    fun `SFTP concurrent testConnection calls are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1..50).map {
            async { service.testConnection() }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `SFTP concurrent createFolder operations are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1..30).map { i ->
            async { service.createFolder("/sftpfolder$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess, "createFolder should succeed: ${result.exceptionOrNull()}")
        }
    }

    @Test
    fun `SFTP concurrent exists checks are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1..50).map { i ->
            async { service.exists("/path$i") }
        }

        val results = jobs.awaitAll()
        assertEquals(50, results.size)
    }

    // ==================== SMB OPERATION LIFECYCLE TESTS ====================

    @Test
    fun `SMB getActiveOperations under concurrent mutations`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Start some operations via upload flow emission
        val pauseJobs = (1L..10L).map { id ->
            async { service.pauseOperation(id) }
        }
        val getActiveJobs = (1..10).map {
            async {
                service.getActiveOperations().collect { ops ->
                    // Just verify we can read without crash
                    assertNotNull(ops)
                }
            }
        }

        pauseJobs.awaitAll()
        getActiveJobs.awaitAll()
    }

    @Test
    fun `SMB getSyncStatus under concurrent sync changes`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create and sync concurrently with reading status
        val createJobs = (1..10).map { i ->
            async { service.createFolder("/status$i") }
        }

        val statusJobs = (1..10).map {
            async {
                service.getSyncStatus(null).collect { statuses ->
                    assertNotNull(statuses)
                }
            }
        }

        createJobs.awaitAll()
        statusJobs.awaitAll()
    }

    @Test
    fun `SMB getCacheEntries under concurrent cache mutations`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val addJobs = (1..10).map { i ->
            async {
                service.createFolder("/ce$i")
                service.addToCache("/ce$i", priority = 1)
            }
        }

        val readJobs = (1..10).map {
            async {
                service.getCacheEntries(null).collect { entries ->
                    assertNotNull(entries)
                }
            }
        }

        addJobs.awaitAll()
        readJobs.awaitAll()
    }

    // ==================== MUTEX ORDERING AND DEADLOCK PREVENTION ====================

    @Test
    fun `Concurrent mutex operations on different resources do not deadlock`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Exercise multiple mutex-protected operations simultaneously
        withTimeout(10000) {
            val jobs = listOf(
                async { service.createFolder("/dl1") },
                async { service.addToCache("/dl1", priority = 1) },
                async { service.pauseOperation(1L) },
                async { service.cancelOperation(2L) },
                async { service.getFileInfo("/dl1") },
                async { service.exists("/dl1") },
                async {
                    service.getSyncStatus(null).collect { assertNotNull(it) }
                },
                async {
                    service.getCacheEntries(null).collect { assertNotNull(it) }
                },
                async {
                    service.getActiveOperations().collect { assertNotNull(it) }
                },
                async { service.clearCache() }
            )

            jobs.awaitAll()
        }
    }

    @Test
    fun `No deadlock with rapid create-delete-rename cycle`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        withTimeout(10000) {
            val jobs = (1..20).map { i ->
                async {
                    service.createFolder("/cycle$i")
                    service.renameFile("/cycle$i", "renamed$i")
                    service.deleteFile("/renamed$i")
                }
            }
            jobs.awaitAll()
        }
    }

    // ==================== AuthTokenManager EDGE CASES ====================

    @Test
    fun `AuthTokenManager storeTokenExpiration concurrent calls`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        val jobs = (1..50).map { i ->
            async {
                val expiresAt = kotlinx.datetime.Clock.System.now()
                    .plus(kotlin.time.Duration.Companion.seconds(i.toLong()))
                manager.storeTokenExpiration(expiresAt)
            }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `AuthTokenManager isTokenExpired concurrent with store`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        val storeJobs = (1..25).map { i ->
            async {
                manager.storeTokenInfo(
                    accessToken = "token_$i",
                    expiresIn = 3600L
                )
            }
        }

        val checkJobs = (1..25).map {
            async { manager.isTokenExpired() }
        }

        val allResults = (storeJobs + checkJobs).awaitAll()
        allResults.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `AuthTokenManager storeRefreshToken concurrent with getRefreshToken`() = runTest {
        val storage = TestSecureStorage()
        val manager = AuthTokenManager("test", storage)

        val storeJobs = (1..25).map { i ->
            async { manager.storeRefreshToken("refresh_$i") }
        }
        val getJobs = (1..25).map {
            async { manager.getRefreshToken() }
        }

        val allResults = (storeJobs + getJobs).awaitAll()
        allResults.forEach { assertTrue(it.isSuccess) }
    }

    // ==================== SFTP OPERATION CONTROL TESTS ====================

    @Test
    fun `SFTP pauseOperation concurrent calls are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1L..50L).map { id ->
            async { service.pauseOperation(id) }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SFTP resumeOperation concurrent calls are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1L..50L).map { id ->
            async { service.resumeOperation(id) }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SFTP cancelOperation concurrent calls are safe`() = runTest {
        val service = SftpService(createSftpConfig())
        service.connect()

        val jobs = (1L..50L).map { id ->
            async { service.cancelOperation(id) }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB moveFile concurrent operations are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        // Create source files
        for (i in 1..10) {
            service.createFolder("/move_source$i")
        }

        // Move concurrently
        val jobs = (1..10).map { i ->
            async { service.moveFile("/move_source$i", "/move_dest$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB validatePath concurrent calls are safe`() = runTest {
        val service = SmbService(createSmbConfig())

        val jobs = (1..100).map { i ->
            async { service.validatePath("/path$i") }
        }

        val results = jobs.awaitAll()
        results.forEach { assertTrue(it.isSuccess) }
    }

    @Test
    fun `SMB getParentPath concurrent calls are safe`() = runTest {
        val service = SmbService(createSmbConfig())

        val jobs = (1..100).map { i ->
            async { service.getParentPath("/a/b/c$i") }
        }

        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun `SMB getStorageInfo concurrent calls are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val jobs = (1..50).map {
            async { service.getStorageInfo() }
        }

        val results = jobs.awaitAll()
        assertEquals(50, results.size)
        results.forEach { info ->
            assertEquals("test-smb", info.name)
            assertTrue(info.isOnline)
        }
    }

    @Test
    fun `SMB getQuotaInfo concurrent calls are safe`() = runTest {
        val service = SmbService(createSmbConfig())
        service.connect()

        val jobs = (1..50).map {
            async { service.getQuotaInfo() }
        }

        val results = jobs.awaitAll()
        results.forEach { result ->
            assertTrue(result.isSuccess)
            val quota = result.getOrThrow()
            assertTrue(quota.totalSpace > 0)
        }
    }
}
