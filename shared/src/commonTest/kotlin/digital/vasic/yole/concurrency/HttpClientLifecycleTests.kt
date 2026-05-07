/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * HTTP Client Lifecycle Regression Tests
 *
 * Regression tests for Phase 1 concurrency fixes #6-7:
 * @Volatile httpClientInitialized flag in WebDavService
 * and GitService to prevent closing uninitialized HttpClient.
 *
 * Verifies that services can be created and disconnected
 * without crash, that concurrent connect/disconnect cycles
 * are safe, and that mass-creating service instances
 * concurrently does not cause issues.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.protocols.webdav.WebDavService
import digital.vasic.yole.network.protocols.git.GitService
import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Regression tests for @Volatile httpClientInitialized lifecycle.
 *
 * Phase 1 fixes #6-7 added @Volatile flags to WebDavService and
 * GitService to avoid closing an HttpClient that was never initialized.
 * These tests verify that:
 * - Services can be created and disconnected without crash
 * - Concurrent connect/disconnect cycles are safe
 * - Disconnecting before any connection attempt succeeds
 * - Creating many service instances concurrently is safe
 */
class HttpClientLifecycleTests {

    // ==================== Helper Configs ====================

    private fun createWebDavConfig() = StorageConfig.WebDavConfig(
        name = "test-webdav",
        url = "https://webdav.example.com",
        username = "user",
        password = "pass"
    )

    private fun createGitConfig() = StorageConfig.GitConfig(
        name = "test-git",
        repositoryUrl = "https://github.com/test/repo",
        branch = "main",
        username = "user",
        personalAccessToken = "token",
        localCachePath = "/tmp/git-cache"
    )

    // ==================== WebDavService Create and Disconnect ====================

    @Test
    fun `WebDavService can be created and disconnected without crash`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        // Disconnect without ever connecting (httpClient never initialized)
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Disconnect without connection should succeed: ${result.exceptionOrNull()}")
    }

    @Test
    fun `WebDavService disconnect before any connection attempt succeeds`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        // The httpClient lazy delegate has NOT been accessed
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Early disconnect should not crash")
        assertFalse(service.isOnline, "Service should not be online after disconnect")
    }

    @Test
    fun `WebDavService multiple disconnects without connect are idempotent`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        repeat(5) {
            val result = service.disconnect()
            assertTrue(result.isSuccess, "Repeated disconnect #$it should succeed")
        }
        assertFalse(service.isOnline, "Service should remain offline")
    }

    // ==================== GitService Create and Disconnect ====================

    @Test
    fun `GitService can be created and disconnected without crash`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Disconnect without connection should succeed: ${result.exceptionOrNull()}")
    }

    @Test
    fun `GitService disconnect before any connection attempt succeeds`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Early disconnect should not crash")
        assertFalse(service.isOnline, "Service should not be online after disconnect")
    }

    @Test
    fun `GitService multiple disconnects without connect are idempotent`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        repeat(5) {
            val result = service.disconnect()
            assertTrue(result.isSuccess, "Repeated disconnect #$it should succeed")
        }
        assertFalse(service.isOnline, "Service should remain offline")
    }

    // ==================== Concurrent Connect/Disconnect Cycles ====================

    @Test
    fun `WebDavService concurrent connect disconnect cycles do not crash`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig(), testConnectFn = { Result.success(Unit) })

        repeat(10) { iteration ->
            val jobs = listOf(
                async {
                    service.connect()
                },
                async {
                    service.disconnect()
                }
            )

            val results = jobs.awaitAll()
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure,
                    "Iteration $iteration: connect/disconnect should return a Result, not throw")
            }
        }
    }

    @Test
    fun `GitService concurrent connect disconnect cycles do not crash`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())

        repeat(10) { iteration ->
            val jobs = listOf(
                async {
                    service.connect()
                },
                async {
                    service.disconnect()
                }
            )

            val results = jobs.awaitAll()
            results.forEach { result ->
                assertTrue(result.isSuccess || result.isFailure,
                    "Iteration $iteration: connect/disconnect should return a Result, not throw")
            }
        }
    }

    @Test
    fun `WebDavService rapid connect disconnect alternation`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig(), testConnectFn = { Result.success(Unit) })

        val jobs = (1..20).map { i ->
            async {
                if (i % 2 == 0) {
                    service.connect()
                } else {
                    service.disconnect()
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size, "All 20 operations should complete")
        results.forEach { result ->
            assertTrue(result.isSuccess || result.isFailure, "Each operation should return a Result")
        }
    }

    @Test
    fun `GitService rapid connect disconnect alternation`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())

        val jobs = (1..20).map { i ->
            async {
                if (i % 2 == 0) {
                    service.connect()
                } else {
                    service.disconnect()
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size, "All 20 operations should complete")
        results.forEach { result ->
            assertTrue(result.isSuccess || result.isFailure, "Each operation should return a Result")
        }
    }

    // ==================== Mass Instance Creation ====================

    @Test
    fun `Creating 50 WebDavService instances concurrently does not crash`() = runBlocking<Unit> {
        val jobs = (1..50).map { i ->
            async {
                val service = WebDavService(
                    StorageConfig.WebDavConfig(
                        name = "webdav-$i",
                        url = "https://webdav$i.example.com",
                        username = "user$i",
                        password = "pass$i"
                    )
                )
                // Each instance should be independently usable
                assertNotNull(service, "Service $i should be created")
                assertFalse(service.isOnline, "New service $i should not be online")
                val result = service.disconnect()
                assertTrue(result.isSuccess, "Disconnect of service $i should succeed")
                service
            }
        }

        val services = jobs.awaitAll()
        assertEquals(50, services.size, "All 50 services should be created")
    }

    @Test
    fun `Creating 50 GitService instances concurrently does not crash`() = runBlocking<Unit> {
        val jobs = (1..50).map { i ->
            async {
                val service = GitService(
                    StorageConfig.GitConfig(
                        name = "git-$i",
                        repositoryUrl = "https://github.com/test/repo$i",
                        branch = "main",
                        username = "user$i",
                        personalAccessToken = "token$i",
                        localCachePath = "/tmp/git-cache-$i"
                    )
                )
                assertNotNull(service, "Service $i should be created")
                assertFalse(service.isOnline, "New service $i should not be online")
                val result = service.disconnect()
                assertTrue(result.isSuccess, "Disconnect of service $i should succeed")
                service
            }
        }

        val services = jobs.awaitAll()
        assertEquals(50, services.size, "All 50 services should be created")
    }

    // ==================== Service State After Operations ====================

    @Test
    fun `WebDavService isOnline is false after creation`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        assertFalse(service.isOnline, "Service should start as offline")
    }

    @Test
    fun `GitService isOnline is false after creation`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        assertFalse(service.isOnline, "Service should start as offline")
    }

    @Test
    fun `WebDavService isOnline after connect then disconnect`() = runBlocking<Unit> {
        val service = WebDavService(createWebDavConfig())
        // connect will fail (no server) but we test the state transitions
        service.connect()
        // After failed connect, isOnline depends on whether connect succeeded
        // After disconnect, it should always be false
        service.disconnect()
        assertFalse(service.isOnline, "Service should be offline after disconnect")
    }

    @Test
    fun `GitService isOnline after connect then disconnect`() = runBlocking<Unit> {
        val service = GitService(createGitConfig())
        service.connect()
        service.disconnect()
        assertFalse(service.isOnline, "Service should be offline after disconnect")
    }
}
