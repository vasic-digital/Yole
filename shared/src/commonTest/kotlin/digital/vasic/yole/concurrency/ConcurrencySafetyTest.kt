/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Concurrency Safety Tests
 *
 * Tests to verify thread safety of shared components
 * and detect potential race conditions or deadlocks.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.random.Random

/**
 * Concurrency safety tests for shared components.
 * Verifies thread safety and detects race conditions.
 */
class ConcurrencySafetyTest {

    // ==================== FORMAT REGISTRY CONCURRENCY ====================

    @Test
    fun `FormatRegistry concurrent access is thread-safe`() = runTest {
        // Concurrent reads from FormatRegistry
        val jobs = (1..100).map {
            async {
                listOf(
                    FormatRegistry.getById(FormatRegistry.ID_MARKDOWN),
                    FormatRegistry.getById(FormatRegistry.ID_CSV),
                    FormatRegistry.getById(FormatRegistry.ID_TODOTXT),
                    FormatRegistry.getByExtension(".md"),
                    FormatRegistry.getByExtension(".csv"),
                    FormatRegistry.formats
                )
            }
        }

        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        // All results should be consistent
        results.forEach { list ->
            assertNotNull(list[0])
            assertNotNull(list[1])
            assertNotNull(list[2])
        }
    }

    @Test
    fun `FormatRegistry detection is consistent under concurrent access`() = runTest {
        val content = "# Markdown Header"

        val results = (1..100).map {
            async {
                FormatRegistry.detectByContent(content)
            }
        }.awaitAll()

        // All detections should return the same result
        val firstResult = results.first()
        assertTrue(results.all { it == firstResult })
    }

    // ==================== NETWORK OPERATION CONCURRENCY ====================

    @Test
    fun `NetworkOperation creation is thread-safe`() = runTest {
        val results = (1..100).map { i ->
            async {
                NetworkOperation.createUpload(
                    id = "upload_$i",
                    remotePath = "/remote/$i.txt",
                    localPath = "/local/$i.txt"
                )
            }
        }.awaitAll()

        assertEquals(100, results.size)
        // All operations should have unique IDs
        val ids = results.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    @Test
    fun `NetworkOperation copy is thread-safe`() = runTest {
        val original = NetworkOperation.createDownload(
            id = "test",
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt"
        )

        val copies = (1..100).map { i ->
            async {
                original.copy(
                    progress = i / 100.0,
                    status = if (i == 100) NetworkOperation.Status.COMPLETED else NetworkOperation.Status.IN_PROGRESS
                )
            }
        }.awaitAll()

        assertEquals(100, copies.size)
        // Progress should be correctly set for each copy
        copies.forEachIndexed { index, op ->
            assertEquals((index + 1) / 100.0, op.progress, 0.001)
        }
    }

    // ==================== STORAGE CONFIG CONCURRENCY ====================

    @Test
    fun `StorageConfig access is thread-safe`() = runTest {
        val configs = listOf(
            StorageConfig.FtpConfig(
                name = "ftp", host = "ftp.example.com", port = 21,
                username = "user", password = "pass", rootPath = "/"
            ),
            StorageConfig.SmbConfig(
                name = "smb", host = "smb.example.com", share = "docs",
                domain = "DOMAIN", username = "user", password = "pass", path = "/"
            ),
            StorageConfig.WebDavConfig(
                name = "webdav", url = "https://webdav.example.com",
                username = "user", password = "pass"
            ),
            StorageConfig.GitConfig(
                name = "git", repositoryUrl = "https://github.com/test/repo",
                branch = "main", username = "user", personalAccessToken = "token",
                localCachePath = "/tmp/git-cache"
            )
        )

        // Concurrent reads of config properties
        val results = (1..100).map {
            async {
                configs.map { config ->
                    when (config) {
                        is StorageConfig.FtpConfig -> config.host
                        is StorageConfig.SmbConfig -> config.host
                        is StorageConfig.WebDavConfig -> config.url
                        is StorageConfig.GitConfig -> config.repositoryUrl
                        else -> ""
                    }
                }
            }
        }.awaitAll()

        assertEquals(100, results.size)
    }

    // ==================== NETWORK DOCUMENT CONCURRENCY ====================

    @Test
    fun `NetworkDocument creation is thread-safe`() = runTest {
        val results = (1..100).map { i ->
            async {
                NetworkDocument(
                    id = "doc_$i",
                    name = "file$i.txt",
                    path = "/path/file$i.txt",
                    isFolder = i % 2 == 0,
                    size = i * 1000L,
                    lastModified = kotlinx.datetime.Clock.System.now(),
                    storageId = "storage",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    syncStatus = SyncStatus.SYNCED
                )
            }
        }.awaitAll()

        assertEquals(100, results.size)
        // Verify all documents are unique
        val ids = results.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    // ==================== MUTEX PATTERN TESTS ====================

    @Test
    fun `Mutex protects shared state correctly`() = runTest {
        val mutex = Mutex()
        var counter = 0

        val jobs = (1..1000).map {
            async {
                mutex.withLock {
                    val current = counter
                    delay(1) // Simulate some work
                    counter = current + 1
                }
            }
        }

        jobs.awaitAll()
        assertEquals(1000, counter)
    }

    @Test
    fun `Nested mutex does not cause deadlock when avoided`() = runTest {
        val outerMutex = Mutex()
        val innerMutex = Mutex()
        var result = 0

        // Proper pattern: acquire locks in consistent order
        val jobs = (1..100).map {
            async {
                outerMutex.withLock {
                    innerMutex.withLock {
                        result += 1
                    }
                }
            }
        }

        // Should complete without deadlock
        withTimeout(5000) {
            jobs.awaitAll()
        }

        assertEquals(100, result)
    }

    // ==================== RACE CONDITION DETECTION ====================

    @Test
    fun `Detect potential race condition in non-atomic operations`() = runTest {
        // This test demonstrates why synchronization is needed
        var unsafeCounter = 0

        val jobs = (1..100).map {
            async {
                repeat(10) {
                    // Non-atomic read-modify-write
                    val current = unsafeCounter
                    yield() // Allow other coroutines to run
                    unsafeCounter = current + 1
                }
            }
        }

        jobs.awaitAll()
        // Without synchronization, the counter will likely be less than 1000
        // This test documents the expected behavior, not a requirement
        assertTrue(unsafeCounter <= 1000)
    }

    @Test
    fun `Atomic operations prevent race conditions`() = runTest {
        val mutex = Mutex()
        var safeCounter = 0

        val jobs = (1..100).map {
            async {
                repeat(10) {
                    mutex.withLock {
                        safeCounter += 1
                    }
                }
            }
        }

        jobs.awaitAll()
        // With mutex protection, counter should be exactly 1000
        assertEquals(1000, safeCounter)
    }

    // ==================== COLLECTION SAFETY TESTS ====================

    @Test
    fun `Concurrent list access with synchronization`() = runTest {
        val list = mutableListOf<Int>()
        val listLock = Mutex()

        val jobs = (1..100).map { i ->
            async {
                listLock.withLock {
                    list.add(i)
                }
            }
        }

        jobs.awaitAll()
        assertEquals(100, list.size)
    }

    @Test
    fun `Concurrent map access with synchronization`() = runTest {
        val map = mutableMapOf<String, Int>()
        val mapLock = Mutex()

        val jobs = (1..100).map { i ->
            async {
                mapLock.withLock {
                    map["key$i"] = i
                }
            }
        }

        jobs.awaitAll()
        assertEquals(100, map.size)
    }

    // ==================== TIMEOUT AND CANCELLATION TESTS ====================

    @Test
    fun `Operations can be cancelled without leaving inconsistent state`() = runTest {
        val mutex = Mutex()
        var value = 0

        val job = launch {
            try {
                mutex.withLock {
                    value = 1
                    delay(Long.MAX_VALUE) // Simulate long operation
                    value = 2 // This should not execute
                }
            } catch (e: CancellationException) {
                // Expected
            }
        }

        delay(100)
        job.cancel()
        job.join()

        // Value should be 1, not 0 or 2
        assertEquals(1, value)
    }

    @Test
    fun `Mutex is properly released on cancellation`() = runTest {
        val mutex = Mutex()

        val job = launch {
            mutex.withLock {
                delay(Long.MAX_VALUE)
            }
        }

        delay(100)
        job.cancel()
        job.join()

        // Mutex should be released and available
        assertFalse(mutex.isLocked)

        // Should be able to acquire mutex
        assertTrue(mutex.tryLock())
        mutex.unlock()
    }

    // ==================== HIGH CONTENTION TESTS ====================

    @Test
    fun `High contention mutex performs correctly`() = runTest {
        val mutex = Mutex()
        val results = mutableListOf<Int>()

        // Many coroutines competing for the same mutex
        val jobs = (1..500).map { i ->
            async {
                mutex.withLock {
                    results.add(i)
                }
                i
            }
        }

        val completed = jobs.awaitAll()
        assertEquals(500, completed.size)
        assertEquals(500, results.size)
    }

    @Test
    fun `Random delays do not cause consistency issues`() = runTest {
        val mutex = Mutex()
        var sum = 0

        val jobs = (1..100).map { i ->
            async {
                delay(Random.nextLong(0, 50))
                mutex.withLock {
                    sum += i
                }
            }
        }

        jobs.awaitAll()
        // Sum of 1 to 100 is 5050
        assertEquals(5050, sum)
    }
}
