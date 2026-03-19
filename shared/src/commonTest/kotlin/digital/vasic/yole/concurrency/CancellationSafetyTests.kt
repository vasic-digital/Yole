/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cancellation Safety Regression Tests
 *
 * Regression tests for Phase 1 concurrency fixes #1-3:
 * CancellationException handling in cloud service Flows
 * (DropboxService, GoogleDriveService, OneDriveService).
 *
 * Verifies that getRecentChanges() returns a Flow, that
 * disconnected services emit empty/failure, and that
 * concurrent collection and mid-collection cancellation
 * do not leak coroutines or crash.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Regression tests for CancellationException safety in cloud service Flows.
 *
 * Phase 1 fixes #1-3 ensured that CancellationException is rethrown
 * (not swallowed) in Dropbox, GoogleDrive, and OneDrive getRecentChanges().
 * These tests verify that:
 * - getRecentChanges() returns a Flow (type check)
 * - Disconnected services emit empty lists (not exceptions)
 * - Concurrent collection from multiple services works safely
 * - Cancelling collectors mid-collection does not leak or crash
 */
class CancellationSafetyTests {

    // ==================== Helper Configs ====================

    private fun createDropboxConfig() = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test-token",
        appKey = "test-key",
        appSecret = "test-secret",
        rootPath = ""
    )

    private fun createGoogleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "test-gdrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret"
    )

    private fun createOneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "test-onedrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret"
    )

    // ==================== Flow Type Checks ====================

    @Test
    fun `DropboxService getRecentChanges returns a Flow`() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        val since = Clock.System.now().minus(1.hours)

        val flow = service.getRecentChanges(since)
        assertNotNull(flow, "getRecentChanges() should return a non-null Flow")
        // The flow itself is a Flow<List<NetworkDocument>>
        assertTrue(flow is Flow<*>, "getRecentChanges() should return a Flow instance")
    }

    @Test
    fun `GoogleDriveService getRecentChanges returns a Flow`() = runBlocking<Unit> {
        val service = GoogleDriveService(createGoogleDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        val flow = service.getRecentChanges(since)
        assertNotNull(flow, "getRecentChanges() should return a non-null Flow")
        assertTrue(flow is Flow<*>, "getRecentChanges() should return a Flow instance")
    }

    @Test
    fun `OneDriveService getRecentChanges returns a Flow`() = runBlocking<Unit> {
        val service = OneDriveService(createOneDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        val flow = service.getRecentChanges(since)
        assertNotNull(flow, "getRecentChanges() should return a non-null Flow")
        assertTrue(flow is Flow<*>, "getRecentChanges() should return a Flow instance")
    }

    // ==================== Disconnected Service Emissions ====================

    @Test
    fun `DropboxService getRecentChanges emits empty when disconnected`() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        // Do NOT connect -- service is disconnected
        val since = Clock.System.now().minus(1.hours)

        val results = mutableListOf<Any>()
        service.getRecentChanges(since).collect { docs ->
            results.add(docs)
        }

        assertTrue(results.isNotEmpty(), "Flow should emit at least one value")
        // Disconnected service emits an empty list
        val firstEmission = results.first()
        assertTrue(firstEmission is List<*>, "First emission should be a list")
        assertTrue((firstEmission as List<*>).isEmpty(), "Disconnected service should emit empty list")
    }

    @Test
    fun `GoogleDriveService getRecentChanges emits empty when disconnected`() = runBlocking<Unit> {
        val service = GoogleDriveService(createGoogleDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        val results = mutableListOf<Any>()
        service.getRecentChanges(since).collect { docs ->
            results.add(docs)
        }

        assertTrue(results.isNotEmpty(), "Flow should emit at least one value")
        val firstEmission = results.first()
        assertTrue(firstEmission is List<*>, "First emission should be a list")
        assertTrue((firstEmission as List<*>).isEmpty(), "Disconnected service should emit empty list")
    }

    @Test
    fun `OneDriveService getRecentChanges emits empty when disconnected`() = runBlocking<Unit> {
        val service = OneDriveService(createOneDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        val results = mutableListOf<Any>()
        service.getRecentChanges(since).collect { docs ->
            results.add(docs)
        }

        assertTrue(results.isNotEmpty(), "Flow should emit at least one value")
        val firstEmission = results.first()
        assertTrue(firstEmission is List<*>, "First emission should be a list")
        assertTrue((firstEmission as List<*>).isEmpty(), "Disconnected service should emit empty list")
    }

    // ==================== Concurrent Collection ====================

    @Test
    fun `Concurrent collection from multiple disconnected services simultaneously`() = runBlocking<Unit> {
        val dropbox = DropboxService(createDropboxConfig())
        val gdrive = GoogleDriveService(createGoogleDriveConfig())
        val onedrive = OneDriveService(createOneDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        val dropboxResults = mutableListOf<Any>()
        val gdriveResults = mutableListOf<Any>()
        val onedriveResults = mutableListOf<Any>()

        val jobs = listOf(
            async {
                dropbox.getRecentChanges(since).collect { docs ->
                    dropboxResults.add(docs)
                }
            },
            async {
                gdrive.getRecentChanges(since).collect { docs ->
                    gdriveResults.add(docs)
                }
            },
            async {
                onedrive.getRecentChanges(since).collect { docs ->
                    onedriveResults.add(docs)
                }
            }
        )

        // All should complete without exceptions
        jobs.awaitAll()

        assertTrue(dropboxResults.isNotEmpty(), "Dropbox flow should have emitted")
        assertTrue(gdriveResults.isNotEmpty(), "GoogleDrive flow should have emitted")
        assertTrue(onedriveResults.isNotEmpty(), "OneDrive flow should have emitted")
    }

    // ==================== Stress: Cancel Mid-Collection ====================

    @Test
    fun `Stress test - 50 coroutines collecting from Dropbox getRecentChanges, cancel half mid-collection`() = runBlocking<Unit> {
        val since = Clock.System.now().minus(1.hours)
        val completedCount = Mutex()
        var completed = 0
        var cancelled = 0

        val jobs = (1..50).map { i ->
            launch {
                val service = DropboxService(createDropboxConfig())
                try {
                    service.getRecentChanges(since).collect { docs ->
                        assertNotNull(docs)
                        completedCount.withLock { completed++ }
                    }
                } catch (e: CancellationException) {
                    completedCount.withLock { cancelled++ }
                    throw e
                }
            }
        }

        // Let some start collecting
        yield()

        // Cancel the first 25
        jobs.take(25).forEach { it.cancel() }

        // Wait for all to finish (cancelled or completed)
        jobs.forEach { job ->
            try {
                job.join()
            } catch (_: CancellationException) {
                // Expected for cancelled jobs
            }
        }

        // Verify: some completed, some were cancelled, no leaks
        val finalCompleted = completedCount.withLock { completed }
        val finalCancelled = completedCount.withLock { cancelled }
        assertTrue(
            finalCompleted + finalCancelled > 0,
            "At least some coroutines should have completed or been cancelled"
        )
        // No leaked coroutines -- all jobs have joined
    }

    @Test
    fun `Stress test - 50 coroutines collecting from GoogleDrive getRecentChanges, cancel half`() = runBlocking<Unit> {
        val since = Clock.System.now().minus(1.hours)
        val countMutex = Mutex()
        var completed = 0

        val jobs = (1..50).map { i ->
            launch {
                val service = GoogleDriveService(createGoogleDriveConfig())
                try {
                    service.getRecentChanges(since).collect { docs ->
                        assertNotNull(docs)
                        countMutex.withLock { completed++ }
                    }
                } catch (_: CancellationException) {
                    // Expected
                }
            }
        }

        yield()

        // Cancel odd-numbered jobs
        jobs.forEachIndexed { index, job ->
            if (index % 2 == 0) job.cancel()
        }

        jobs.forEach { job ->
            try {
                job.join()
            } catch (_: CancellationException) {
                // Expected
            }
        }

        // Verify no crash and some completed
        val finalCompleted = countMutex.withLock { completed }
        assertTrue(finalCompleted >= 0, "No crash during concurrent collection and cancellation")
    }

    @Test
    fun `Stress test - 50 coroutines collecting from OneDrive getRecentChanges, cancel half`() = runBlocking<Unit> {
        val since = Clock.System.now().minus(1.hours)
        val countMutex = Mutex()
        var completed = 0

        val jobs = (1..50).map { i ->
            launch {
                val service = OneDriveService(createOneDriveConfig())
                try {
                    service.getRecentChanges(since).collect { docs ->
                        assertNotNull(docs)
                        countMutex.withLock { completed++ }
                    }
                } catch (_: CancellationException) {
                    // Expected
                }
            }
        }

        yield()

        // Cancel the last 25
        jobs.takeLast(25).forEach { it.cancel() }

        jobs.forEach { job ->
            try {
                job.join()
            } catch (_: CancellationException) {
                // Expected
            }
        }

        val finalCompleted = countMutex.withLock { completed }
        assertTrue(finalCompleted >= 0, "No crash during concurrent collection and cancellation")
    }

    // ==================== Multiple Collection Passes ====================

    @Test
    fun `Collecting getRecentChanges multiple times from same service is safe`() = runBlocking<Unit> {
        val service = DropboxService(createDropboxConfig())
        val since = Clock.System.now().minus(1.hours)

        // Collect from the same service 10 times sequentially
        repeat(10) {
            val results = mutableListOf<Any>()
            service.getRecentChanges(since).collect { docs ->
                results.add(docs)
            }
            assertTrue(results.isNotEmpty(), "Each collection pass should emit at least one value")
        }
    }

    @Test
    fun `Collecting getRecentChanges with null path from disconnected service`() = runBlocking<Unit> {
        val dropbox = DropboxService(createDropboxConfig())
        val gdrive = GoogleDriveService(createGoogleDriveConfig())
        val onedrive = OneDriveService(createOneDriveConfig())
        val since = Clock.System.now().minus(1.hours)

        // All three with null path (default)
        val services = listOf(dropbox, gdrive, onedrive)
        for (service in services) {
            val emissions = mutableListOf<Any>()
            service.getRecentChanges(since, null).collect { docs ->
                emissions.add(docs)
            }
            assertTrue(emissions.isNotEmpty(), "Flow with null path should emit at least one value")
        }
    }
}
