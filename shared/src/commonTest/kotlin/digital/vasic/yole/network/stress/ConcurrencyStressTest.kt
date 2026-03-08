package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.smb.SmbService
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Stress tests for concurrent network service operations.
 * Validates thread-safety of stateMutex-protected state in network services.
 */
class ConcurrencyStressTest {

    private val smbConfig = StorageConfig.SmbConfig(
        name = "stress-test",
        host = "localhost",
        share = "test",
        domain = "WORKGROUP",
        username = "user",
        password = "pass",
        port = 445,
        useSsl = false,
        connectionTimeout = 5000
    )

    @Test
    fun testConcurrentConnectDisconnect() = runBlocking {
        val service = SmbService(smbConfig)

        // Rapid connect/disconnect cycles from multiple coroutines
        val jobs = (1..100).map {
            async {
                service.connect()
                service.disconnect()
            }
        }
        jobs.awaitAll()

        // After all concurrent ops, service should be in a consistent state
        // (not crashed, no deadlocks)
        val storageInfo = service.getStorageInfo()
        assertEquals("smb_stress-test", storageInfo.id)
    }

    @Test
    fun testConcurrentStateAccess() = runBlocking {
        val service = SmbService(smbConfig)
        service.connect()

        // Multiple coroutines reading and writing state simultaneously
        val results = (1..50).map { i ->
            async {
                if (i % 2 == 0) {
                    service.connect()
                    service.isOnline // Read
                } else {
                    service.disconnect()
                    service.isOnline // Read
                }
            }
        }.awaitAll()

        // All operations completed without deadlock
        assertEquals(50, results.size)
    }

    @Test
    fun testConcurrentFileOperations() = runBlocking {
        val service = SmbService(smbConfig)
        service.connect()

        // Multiple coroutines performing file operations
        val results = (1..20).map { i ->
            async {
                val createResult = service.createFolder("/test-folder-$i")
                assertTrue(createResult.isSuccess, "Folder creation $i should succeed")

                val existsResult = service.exists("/test-folder-$i")
                assertTrue(existsResult.isSuccess, "Exists check $i should succeed")

                val deleteResult = service.deleteFile("/test-folder-$i")
                assertTrue(deleteResult.isSuccess, "Deletion $i should succeed")
            }
        }
        jobs@ for (job in results) {
            job.await()
        }
    }

    @Test
    fun testNoDeadlockUnderContention() = runBlocking {
        val service = SmbService(smbConfig)

        // High contention: many coroutines competing for the same resource
        withTimeout(10_000) { // Must complete within 10 seconds
            val jobs = (1..200).map {
                async {
                    service.connect()
                    service.testConnection()
                    service.getStorageInfo()
                    service.disconnect()
                }
            }
            jobs.awaitAll()
        }
        // If we get here, no deadlock occurred
    }

    @Test
    fun testConnectDisconnectLeavesConsistentState() = runBlocking {
        val service = SmbService(smbConfig)

        // Connect then disconnect many times sequentially
        repeat(100) {
            service.connect()
            assertTrue(service.isOnline, "Should be online after connect iteration $it")
            service.disconnect()
        }

        // Final state should be disconnected
        assertTrue(!service.isOnline, "Should be offline after final disconnect")
    }
}
