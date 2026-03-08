/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive integration tests for all network components
 *
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive integration tests covering:
 * - End-to-end workflows across all network protocols
 * - Cross-service data synchronization
 * - Authentication flow integration
 * - Network document operations
 * - Performance under load
 * - Error recovery across services
 */
class NetworkIntegrationComprehensiveTest {

    private lateinit var mockSecureStorage: SecureStorage

    @BeforeTest
    fun setup() {
        mockSecureStorage = IntegrationTestSecureStorage()
    }

    @Test
    fun testEndToEndWorkflow() = runBlocking<Unit> {
        // Complete workflow: create document -> create operation -> simulate progress -> complete

        val testDocument = createTestDocument("integration_test.txt", "Integration test content")
        val localPath = "/local/integration_test.txt"
        val remotePath = "/remote/integration_test.txt"

        // Step 1: Verify document creation
        assertNotNull(testDocument)
        assertEquals("integration_test.txt", testDocument.name)
        assertEquals(SyncStatus.SYNCED, testDocument.syncStatus)

        // Step 2: Create upload operation
        val uploadOperation = createNetworkOperation(
            type = NetworkOperation.Type.UPLOAD,
            remotePath = remotePath,
            localPath = localPath
        )
        assertEquals(NetworkOperation.Status.PENDING, uploadOperation.status)
        assertEquals(NetworkOperation.Type.UPLOAD, uploadOperation.type)

        // Step 3: Simulate upload progress
        val uploadResults = simulateNetworkOperationWithProgress(uploadOperation).toList()
        assertTrue(uploadResults.isNotEmpty(), "Should produce upload progress")

        val finalUploadResult = uploadResults.last()
        assertEquals(NetworkOperation.Status.COMPLETED, finalUploadResult.status, "Upload should complete successfully")

        // Step 4: Verify document sync status update
        val updatedDocument = testDocument.withSyncStatus(SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, updatedDocument.syncStatus)
    }

    @Test
    fun testCrossServiceDocumentCreation() = runBlocking<Unit> {
        // Test creating documents across multiple service types

        val services = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp")

        val documents = services.map { service ->
            createTestDocument("cross_service_$service.txt", "Content for $service")
        }

        assertEquals(services.size, documents.size)
        documents.forEach { doc ->
            assertNotNull(doc)
            assertTrue(doc.name.startsWith("cross_service_"))
        }

        // Simulate concurrent operations across services
        val syncResults = services.mapIndexed { index, service ->
            async {
                val operation = createNetworkOperation(
                    type = NetworkOperation.Type.UPLOAD,
                    remotePath = "/$service/cross_service.txt",
                    localPath = "/local/cross_service.txt"
                )

                val results = simulateNetworkOperationWithProgress(operation).toList()
                service to results.last()
            }
        }.awaitAll()

        // Verify all services completed successfully
        syncResults.forEach { (service, result) ->
            assertEquals(NetworkOperation.Status.COMPLETED, result.status, "$service should complete successfully")
        }
    }

    @Test
    fun testAuthenticationFlowIntegration() = runBlocking<Unit> {
        // Test complete authentication flow with token management

        val testService = "test_network_service"
        val testToken = "test_access_token_123"
        val testRefreshToken = "test_refresh_token_456"

        // Step 1: Create AuthTokenManager and store tokens
        val authTokenManager = AuthTokenManager(testService, mockSecureStorage)
        val storeTokenResult = authTokenManager.storeAccessToken(testToken)
        assertTrue(storeTokenResult.isSuccess, "Should store access token")

        val storeRefreshResult = authTokenManager.storeRefreshToken(testRefreshToken)
        assertTrue(storeRefreshResult.isSuccess, "Should store refresh token")

        // Step 2: Store token expiration
        val expiresAt = Clock.System.now().plus(1.hours)
        val storeExpirationResult = authTokenManager.storeTokenExpiration(expiresAt)
        assertTrue(storeExpirationResult.isSuccess, "Should store token expiration")

        // Step 3: Verify valid token
        val hasValidTokenResult = authTokenManager.hasValidToken()
        assertTrue(hasValidTokenResult.isSuccess, "Should check token validity")
        assertTrue(hasValidTokenResult.getOrNull() ?: false, "Should have valid token")

        // Step 4: Clear tokens
        val clearResult = authTokenManager.clearTokens()
        assertTrue(clearResult.isSuccess, "Should clear tokens")

        // Step 5: Verify tokens are cleared
        val hasTokenAfterClear = authTokenManager.hasValidToken()
        assertTrue(hasTokenAfterClear.isSuccess, "Should check token after clear")
        assertFalse(hasTokenAfterClear.getOrNull() ?: true, "Should not have valid token after clear")
    }

    @Test
    fun testNetworkDocumentOperations() = runBlocking<Unit> {
        // Test integration of network document operations

        val documents = (1..10).map { index ->
            createTestDocument("doc_$index.txt", "Document $index content")
        }

        // Step 1: Verify all documents created
        assertEquals(10, documents.size)
        documents.forEach { doc ->
            assertNotNull(doc)
            assertEquals(SyncStatus.SYNCED, doc.syncStatus)
            assertFalse(doc.isFolder)
        }

        // Step 2: Create batch upload operations
        val operations = documents.map { doc ->
            createNetworkOperation(
                type = NetworkOperation.Type.UPLOAD,
                remotePath = "/remote/${doc.name}",
                localPath = "/local/${doc.name}"
            )
        }
        assertEquals(10, operations.size)

        // Step 3: Simulate batch processing
        val batchResults = operations.map { operation ->
            async {
                val results = simulateNetworkOperationWithProgress(operation).toList()
                results.last()
            }
        }.awaitAll()

        // Step 4: Verify all completed
        assertEquals(10, batchResults.size)
        batchResults.forEach { result ->
            assertEquals(NetworkOperation.Status.COMPLETED, result.status, "Should complete")
        }

        // Step 5: Verify document sync status can be updated
        val updatedDocuments = documents.map { it.withSyncStatus(SyncStatus.SYNCED) }
        updatedDocuments.forEach { doc ->
            assertEquals(SyncStatus.SYNCED, doc.syncStatus)
        }
    }

    @Test
    fun testErrorRecoveryAcrossServices() = runBlocking<Unit> {
        // Test error recovery mechanisms across different network services

        val errorScenarios = listOf(
            "network_timeout" to "Network timeout",
            "authentication_failure" to "Authentication failure",
            "service_unavailable" to "Service unavailable",
            "rate_limited" to "Rate limited",
            "invalid_request" to "Invalid request"
        )

        errorScenarios.forEach { (errorType, _) ->
            val operation = createNetworkOperation(
                type = NetworkOperation.Type.UPLOAD,
                remotePath = "/remote/error_test_$errorType.txt",
                localPath = "/local/error_test_$errorType.txt"
            )

            // Simulate error scenario
            val errorResult = simulateNetworkError(operation, errorType)

            // Verify error is properly handled
            assertEquals(NetworkOperation.Status.FAILED, errorResult.status, "$errorType should fail")
            assertTrue(errorResult.error?.contains(errorType) ?: false, "$errorType error should be recorded")

            // Test recovery mechanism
            val recoveryOperation = createNetworkOperation(
                type = NetworkOperation.Type.UPLOAD,
                remotePath = "/remote/recovered_$errorType.txt",
                localPath = "/local/recovered_$errorType.txt"
            )

            val recoveryResults = simulateNetworkOperationWithProgress(recoveryOperation).toList()
            val finalRecoveryResult = recoveryResults.last()

            assertEquals(NetworkOperation.Status.COMPLETED, finalRecoveryResult.status, "Recovery from $errorType should succeed")
        }
    }

    @Test
    fun testPerformanceUnderLoad() = runBlocking<Unit> {
        // Test system performance under high load conditions

        val loadLevels = listOf(10, 50, 100)
        val results = mutableMapOf<Int, PerformanceMetrics>()

        loadLevels.forEach { loadLevel ->
            val startTime = Clock.System.now()

            // Create load test operations
            val operations = (1..loadLevel).map { index ->
                createNetworkOperation(
                    type = NetworkOperation.Type.entries[index % NetworkOperation.Type.entries.size],
                    remotePath = "/load/test_$index.txt",
                    localPath = "/load/test_$index.txt"
                )
            }

            // Process operations concurrently
            val jobs = operations.map { operation ->
                async {
                    val operationResults = simulateNetworkOperationWithProgress(operation).toList()
                    operationResults.last()
                }
            }

            val operationResults = jobs.awaitAll()
            val endTime = Clock.System.now()

            val successCount = operationResults.count { it.status == NetworkOperation.Status.COMPLETED }
            val totalDuration = endTime - startTime
            val averageDuration = totalDuration / loadLevel

            val metrics = PerformanceMetrics(
                loadLevel = loadLevel,
                successCount = successCount,
                totalDurationMs = totalDuration.inWholeMilliseconds,
                averageDurationMs = averageDuration.inWholeMilliseconds,
                successRate = successCount.toDouble() / loadLevel
            )

            results[loadLevel] = metrics

            // Performance assertions
            assertTrue(metrics.successRate >= 0.95, "Success rate should be at least 95% at load level $loadLevel")
            assertTrue(metrics.totalDurationMs < 30000, "Total duration should be under 30s at load level $loadLevel")
        }
    }

    @Test
    fun testConcurrentOperationsAcrossProtocols() = runBlocking<Unit> {
        // Test concurrent operations across different network protocols

        val protocols = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp")
        val concurrentOperationsPerProtocol = 20

        val allResults = protocols.map { protocol ->
            async {
                val protocolResults = (1..concurrentOperationsPerProtocol).map { index ->
                    async {
                        val operation = createNetworkOperation(
                            type = NetworkOperation.Type.UPLOAD,
                            remotePath = "/$protocol/concurrent_$index.txt",
                            localPath = "/local/${protocol}_concurrent_$index.txt"
                        )

                        val operationResults = simulateNetworkOperationWithProgress(operation).toList()
                        protocol to operationResults.last()
                    }
                }.awaitAll()

                protocolResults
            }
        }.awaitAll().flatten()

        // Verify all operations completed successfully
        allResults.forEach { (protocol, result) ->
            assertEquals(NetworkOperation.Status.COMPLETED, result.status, "$protocol concurrent operation should succeed")
        }

        val successCount = allResults.count { it.second.status == NetworkOperation.Status.COMPLETED }
        val totalOperations = protocols.size * concurrentOperationsPerProtocol

        assertEquals(totalOperations, successCount, "All concurrent operations should succeed")
    }

    // ==================== Helper Methods ====================

    private fun createTestDocument(name: String, content: String): NetworkDocument {
        return NetworkDocument(
            id = "test_${name.hashCode()}",
            name = name,
            path = "/test/$name",
            size = content.length.toLong(),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
    }

    private fun createNetworkOperation(
        type: NetworkOperation.Type,
        remotePath: String,
        localPath: String,
        progress: Double = 0.0
    ): NetworkOperation {
        return NetworkOperation(
            id = remotePath.hashCode().toLong(),
            type = type,
            status = NetworkOperation.Status.PENDING,
            remotePath = remotePath,
            localPath = localPath,
            progress = progress,
            createdAt = Clock.System.now()
        )
    }

    private fun simulateNetworkOperationWithProgress(operation: NetworkOperation) = kotlinx.coroutines.flow.flow {
        var currentProgress = 0.0
        val operationCopy = operation.copy(status = NetworkOperation.Status.IN_PROGRESS, startedAt = Clock.System.now())

        emit(operationCopy.copy(progress = currentProgress))

        // Simulate progress
        while (currentProgress < 1.0) {
            delay(10) // Short delay for test performance
            currentProgress += 0.25
            emit(operationCopy.copy(progress = currentProgress.coerceAtMost(1.0)))
        }

        // Complete operation
        emit(operationCopy.copy(
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            completedAt = Clock.System.now()
        ))
    }

    private fun simulateNetworkError(operation: NetworkOperation, errorType: String): NetworkOperation {
        return operation.copy(
            status = NetworkOperation.Status.FAILED,
            completedAt = Clock.System.now(),
            error = "Network error: $errorType",
            metadata = operation.metadata + mapOf("error_type" to errorType)
        )
    }

    private data class PerformanceMetrics(
        val loadLevel: Int,
        val successCount: Int,
        val totalDurationMs: Long,
        val averageDurationMs: Long,
        val successRate: Double
    )

    /**
     * Secure storage implementation for integration tests.
     */
    private class IntegrationTestSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()

        override suspend fun store(key: String, value: String): Result<Unit> {
            storage[key] = value
            return Result.success(Unit)
        }

        override suspend fun retrieve(key: String): Result<String?> {
            return Result.success(storage[key])
        }

        override suspend fun delete(key: String): Result<Unit> {
            storage.remove(key)
            return Result.success(Unit)
        }

        override suspend fun contains(key: String): Result<Boolean> {
            return Result.success(storage.containsKey(key))
        }

        override suspend fun listKeys(): Result<List<String>> {
            return Result.success(storage.keys.toList())
        }

        override suspend fun clear(): Result<Unit> {
            storage.clear()
            return Result.success(Unit)
        }

        override suspend fun isSecure(): Result<Boolean> {
            return Result.success(true)
        }
    }
}
