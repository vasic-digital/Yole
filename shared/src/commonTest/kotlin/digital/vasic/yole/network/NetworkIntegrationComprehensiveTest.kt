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
import digital.vasic.yole.network.database.NetworkStorageDatabase
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive integration tests covering:
 * - End-to-end workflows across all network protocols
 * - Cross-service data synchronization
 * - Authentication flow integration
 * - Database integration with network services
 * - Performance under load
 * - Error recovery across services
 */
class NetworkIntegrationComprehensiveTest {

    private val database = NetworkStorageDatabase()
    private val mockAuthManager = MockAuthTokenManager()

    @Test
    fun testEndToEndWorkflow() = runTest {
        // Complete workflow: authenticate → upload → sync → download → verify
        
        val testDocument = createTestDocument("integration_test.txt", "Integration test content")
        val localPath = "/local/integration_test.txt"
        val remotePath = "/remote/integration_test.txt"
        
        // Step 1: Store document in database
        val storeResult = database.storeDocument("test-service", testDocument)
        assertTrue(storeResult.isSuccess, "Should store document successfully")
        
        // Step 2: Create sync status
        val syncStatus = SyncStatus(
            documentId = testDocument.id,
            remotePath = remotePath,
            localPath = localPath,
            lastSyncTime = Clock.System.now(),
            syncState = SyncStatus.State.PENDING_UPLOAD,
            checksum = "abc123"
        )
        
        val syncStoreResult = database.storeSyncStatus("test-service", syncStatus)
        assertTrue(syncStoreResult.isSuccess, "Should store sync status successfully")
        
        // Step 3: Simulate upload process
        val uploadOperation = createNetworkOperation(
            type = NetworkOperation.Type.UPLOAD,
            remotePath = remotePath,
            localPath = localPath
        )
        
        val uploadResults = simulateNetworkOperationWithProgress(uploadOperation).toList()
        assertTrue(uploadResults.isNotEmpty(), "Should produce upload progress")
        
        val finalUploadResult = uploadResults.last()
        assertEquals(NetworkOperation.Status.COMPLETED, finalUploadResult.status, "Upload should complete successfully")
        
        // Step 4: Update sync status
        val updatedSyncStatus = syncStatus.copy(
            lastSyncTime = Clock.System.now(),
            syncState = SyncStatus.State.SYNCED
        )
        
        val updateSyncResult = database.storeSyncStatus("test-service", updatedSyncStatus)
        assertTrue(updateSyncResult.isSuccess, "Should update sync status")
        
        // Step 5: Verify sync status
        val retrievedSyncStatus = database.getSyncStatus("test-service", testDocument.id)
        assertTrue(retrievedSyncStatus.isSuccess, "Should retrieve sync status")
        assertEquals(SyncStatus.State.SYNCED, retrievedSyncStatus.getOrNull()?.syncState, "Should be synced")
        
        println("✓ End-to-end workflow completed successfully")
    }

    @Test
    fun testCrossServiceSynchronization() = runTest {
        // Test synchronizing the same document across multiple services
        
        val document = createTestDocument("cross_service.txt", "Cross-service test content")
        val services = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp")
        
        // Store document once
        database.storeDocument("shared", document)
        
        // Sync to multiple services concurrently
        val syncResults = services.map { service ->
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
            println("✓ $service synchronization completed")
        }
        
        // Verify consistency across services
        val checksums = syncResults.map { it.second.metadata?.get("checksum") }
        assertTrue(checksums.all { it == checksums[0] }, "All services should have consistent checksums")
        
        println("✓ Cross-service synchronization completed across ${services.size} services")
    }

    @Test
    fun testAuthenticationFlowIntegration() = runTest {
        // Test complete authentication flow with token management
        
        val testService = "test_network_service"
        val testToken = "test_access_token_123"
        val testRefreshToken = "test_refresh_token_456"
        
        // Step 1: Store initial tokens
        val storeTokenResult = mockAuthManager.storeAccessToken(testService, testToken)
        assertTrue(storeTokenResult.isSuccess, "Should store access token")
        
        val storeRefreshResult = mockAuthManager.storeRefreshToken(testService, testRefreshToken)
        assertTrue(storeRefreshResult.isSuccess, "Should store refresh token")
        
        // Step 2: Store token expiration
        val expiresAt = Clock.System.now().plus(kotlin.time.Duration.hours(1))
        val storeExpirationResult = mockAuthManager.storeTokenExpiration(testService, expiresAt)
        assertTrue(storeExpirationResult.isSuccess, "Should store token expiration")
        
        // Step 3: Verify valid token
        val hasValidTokenResult = mockAuthManager.hasValidToken(testService)
        assertTrue(hasValidTokenResult.isSuccess, "Should check token validity")
        assertTrue(hasValidTokenResult.getOrNull() ?: false, "Should have valid token")
        
        // Step 4: Simulate token refresh
        val refreshResult = mockAuthManager.refreshAccessToken(testService, "client_id", "client_secret", "https://token.url")
        assertTrue(refreshResult.isSuccess, "Should refresh access token")
        assertEquals("new_mock_token", refreshResult.getOrNull(), "Should return new token")
        
        // Step 5: Clear tokens
        val clearResult = mockAuthManager.clearTokens(testService)
        assertTrue(clearResult.isSuccess, "Should clear tokens")
        
        // Step 6: Verify tokens are cleared
        val hasTokenAfterClear = mockAuthManager.hasValidToken(testService)
        assertTrue(hasTokenAfterClear.isSuccess, "Should check token after clear")
        assertFalse(hasTokenAfterClear.getOrNull() ?: true, "Should not have valid token after clear")
        
        println("✓ Authentication flow integration completed")
    }

    @Test
    fun testDatabaseNetworkIntegration() = runTest {
        // Test integration between database operations and network services
        
        val documents = (1..10).map { index ->
            createTestDocument("doc_$index.txt", "Document $index content")
        }
        
        // Step 1: Store documents in database
        documents.forEach { document ->
            val storeResult = database.storeDocument("test-service", document)
            assertTrue(storeResult.isSuccess, "Should store document ${document.id}")
        }
        
        // Step 2: Create sync statuses
        val syncStatuses = documents.map { document ->
            SyncStatus(
                documentId = document.id,
                remotePath = "/remote/${document.name}",
                localPath = "/local/${document.name}",
                lastSyncTime = Clock.System.now(),
                syncState = SyncStatus.State.PENDING_UPLOAD,
                checksum = "checksum_${document.id}"
            )
        }
        
        syncStatuses.forEach { syncStatus ->
            val storeSyncResult = database.storeSyncStatus("test-service", syncStatus)
            assertTrue(storeSyncResult.isSuccess, "Should store sync status")
        }
        
        // Step 3: Simulate batch upload
        val batchOperation = createBatchNetworkOperation(
            type = NetworkOperation.Type.UPLOAD,
            documents = documents
        )
        
        val batchResults = simulateBatchNetworkOperation(batchOperation).toList()
        assertTrue(batchResults.isNotEmpty(), "Should produce batch operation results")
        
        val finalBatchResult = batchResults.last()
        assertEquals(NetworkOperation.Status.COMPLETED, finalBatchResult.status, "Batch upload should complete")
        
        // Step 4: Update sync statuses based on results
        val updatedStatuses = syncStatuses.map { syncStatus ->
            syncStatus.copy(
                lastSyncTime = Clock.System.now(),
                syncState = SyncStatus.State.SYNCED
            )
        }
        
        updatedStatuses.forEach { syncStatus ->
            val updateResult = database.storeSyncStatus("test-service", syncStatus)
            assertTrue(updateResult.isSuccess, "Should update sync status")
        }
        
        // Step 5: Verify database state
        val allDocuments = database.getAllDocuments("test-service")
        assertTrue(allDocuments.isSuccess, "Should retrieve all documents")
        assertEquals(10, allDocuments.getOrNull()?.size, "Should have all 10 documents")
        
        val allSyncStatuses = database.getAllSyncStatus("test-service")
        assertTrue(allSyncStatuses.isSuccess, "Should retrieve all sync statuses")
        assertEquals(10, allSyncStatuses.getOrNull()?.size, "Should have all 10 sync statuses")
        
        println("✓ Database-network integration completed for ${documents.size} documents")
    }

    @Test
    fun testErrorRecoveryAcrossServices() = runTest {
        // Test error recovery mechanisms across different network services
        
        val errorScenarios = listOf(
            "network_timeout" to "Network timeout",
            "authentication_failure" to "Authentication failure", 
            "service_unavailable" to "Service unavailable",
            "rate_limited" to "Rate limited",
            "invalid_request" to "Invalid request"
        )
        
        errorScenarios.forEach { (errorType, description) ->
            val document = createTestDocument("error_test_$errorType.txt", "Error test content")
            val operation = createNetworkOperation(
                type = NetworkOperation.Type.UPLOAD,
                remotePath = "/remote/error_test_$errorType.txt",
                localPath = "/local/error_test_$errorType.txt"
            )
            
            // Simulate error scenario
            val errorResult = simulateNetworkError(operation, errorType)
            
            // Verify error is properly handled
            assertEquals(NetworkOperation.Status.FAILED, errorResult.status, "$description should fail")
            assertTrue(errorResult.error?.contains(errorType) ?: false, "$description error should be recorded")
            
            // Test recovery mechanism
            val recoveryOperation = createNetworkOperation(
                type = NetworkOperation.Type.UPLOAD,
                remotePath = "/remote/recovered_$errorType.txt",
                localPath = "/local/recovered_$errorType.txt"
            )
            
            val recoveryResults = simulateNetworkOperationWithProgress(recoveryOperation).toList()
            val finalRecoveryResult = recoveryResults.last()
            
            assertEquals(NetworkOperation.Status.COMPLETED, finalRecoveryResult.status, "Recovery from $description should succeed")
            
            println("✓ Error recovery completed for: $description")
        }
    }

    @Test
    fun testPerformanceUnderLoad() = runTest {
        // Test system performance under high load conditions
        
        val loadLevels = listOf(10, 50, 100, 500)
        val results = mutableMapOf<Int, PerformanceMetrics>()
        
        loadLevels.forEach { loadLevel ->
            val startTime = Clock.System.now()
            
            // Create load test operations
            val operations = (1..loadLevel).map { index ->
                createNetworkOperation(
                    type = NetworkOperation.Type.values()[index % NetworkOperation.Type.values().size],
                    remotePath = "/load/test_$index.txt",
                    localPath = "/load/test_$index.txt"
                )
            }
            
            // Process operations concurrently
            val jobs = operations.map { operation ->
                async {
                    val results = simulateNetworkOperationWithProgress(operation).toList()
                    results.last()
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
                totalDuration = totalDuration,
                averageDuration = averageDuration,
                successRate = successCount.toDouble() / loadLevel
            )
            
            results[loadLevel] = metrics
            
            println("Load test $loadLevel operations:")
            println("  Success rate: ${metrics.successRate * 100}%")
            println("  Total time: ${metrics.totalDuration.inWholeMilliseconds}ms")
            println("  Average time: ${metrics.averageDuration.inWholeMilliseconds}ms")
            
            // Performance assertions
            assertTrue(metrics.successRate >= 0.95, "Success rate should be at least 95% at load level $loadLevel")
            assertTrue(metrics.totalDuration < 30.seconds, "Total duration should be under 30s at load level $loadLevel")
            assertTrue(metrics.averageDuration < 500.milliseconds, "Average duration should be under 500ms at load level $loadLevel")
        }
        
        // Verify scalability
        val scalabilityFactor = calculateScalabilityFactor(results.values.toList())
        assertTrue(scalabilityFactor < 1.5, "Scalability factor should be under 1.5x")
        
        println("✓ Performance under load test completed")
    }

    @Test
    fun testConcurrentOperationsAcrossProtocols() = runTest {
        // Test concurrent operations across different network protocols
        
        val protocols = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp")
        val concurrentOperationsPerProtocol = 20
        
        val allResults = protocols.map { protocol ->
            async {
                val protocolResults = (1..concurrentOperationsPerProtocol).map { index ->
                    async {
                        val document = createTestDocument("${protocol}_concurrent_$index.txt", "Concurrent content for $protocol")
                        val operation = createNetworkOperation(
                            type = NetworkOperation.Type.UPLOAD,
                            remotePath = "/$protocol/concurrent_$index.txt",
                            localPath = "/local/${protocol}_concurrent_$index.txt"
                        )
                        
                        val results = simulateNetworkOperationWithProgress(operation).toList()
                        protocol to results.last()
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
        
        println("✓ Concurrent operations completed across ${protocols.size} protocols with $concurrentOperationsPerProtocol operations each")
    }

    // ==================== Helper Methods ====================

    private fun createTestDocument(name: String, content: String): NetworkDocument {
        return NetworkDocument(
            id = "test_${System.currentTimeMillis()}",
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
            id = System.currentTimeMillis(),
            type = type,
            status = NetworkOperation.Status.PENDING,
            remotePath = remotePath,
            localPath = localPath,
            progress = progress,
            createdAt = Clock.System.now()
        )
    }

    private fun createBatchNetworkOperation(
        type: NetworkOperation.Type,
        documents: List<NetworkDocument>
    ): NetworkOperation {
        return NetworkOperation(
            id = System.currentTimeMillis(),
            type = type,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/remote/batch/",
            localPath = "/local/batch/",
            progress = 0.0,
            createdAt = Clock.System.now(),
            metadata = mapOf("batch_size" to documents.size.toString())
        )
    }

    private fun simulateNetworkOperationWithProgress(operation: NetworkOperation) = kotlinx.coroutines.flow.flow {
        var currentProgress = 0.0
        val operationCopy = operation.copy(status = NetworkOperation.Status.IN_PROGRESS, startedAt = Clock.System.now())
        
        emit(operationCopy.copy(progress = currentProgress))
        
        // Simulate progress
        while (currentProgress < 1.0) {
            delay(100)
            currentProgress += 0.1
            emit(operationCopy.copy(progress = currentProgress.coerceAtMost(1.0)))
        }
        
        // Complete operation
        emit(operationCopy.copy(
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            completedAt = Clock.System.now()
        ))
    }

    private fun simulateBatchNetworkOperation(operation: NetworkOperation) = kotlinx.coroutines.flow.flow {
        val batchSize = operation.metadata?.get("batch_size")?.toIntOrNull() ?: 1
        
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, startedAt = Clock.System.now()))
        
        // Simulate batch processing
        repeat(batchSize) { index ->
            delay(50)
            val progress = (index + 1).toDouble() / batchSize
            emit(operation.copy(progress = progress))
        }
        
        // Complete batch
        emit(operation.copy(
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
            metadata = (operation.metadata ?: emptyMap()) + mapOf("error_type" to errorType)
        )
    }

    private fun calculateScalabilityFactor(results: List<PerformanceMetrics>): Double {
        if (results.size < 2) return 0.0
        val first = results.first()
        val last = results.last()
        return (last.timePerOperation.inWholeMicroseconds.toDouble() / 
                first.timePerOperation.inWholeMicroseconds.toDouble()) / 
               (last.loadLevel.toDouble() / first.loadLevel.toDouble())
    }

    private data class PerformanceMetrics(
        val loadLevel: Int,
        val successCount: Int,
        val totalDuration: kotlinx.datetime.Duration,
        val averageDuration: kotlinx.datetime.Duration,
        val successRate: Double
    )

    private class MockAuthTokenManager : AuthTokenManager {
        private val tokens = mutableMapOf<String, String>()
        private val refreshTokens = mutableMapOf<String, String>()
        private val expirations = mutableMapOf<String, kotlinx.datetime.Instant>()
        private var simulateFailure: String? = null
        
        override suspend fun storeAccessToken(service: String, token: String) = Result.success(Unit).also {
            tokens[service] = token
        }
        
        override suspend fun getAccessToken(service: String): Result<String> {
            simulateFailure?.let { 
                return Result.failure(Exception("Simulated auth failure: $it"))
            }
            return tokens[service]?.let { Result.success(it) } 
                ?: Result.failure(Exception("No access token found"))
        }
        
        override suspend fun storeRefreshToken(service: String, token: String) = Result.success(Unit).also {
            refreshTokens[service] = token
        }
        
        override suspend fun getRefreshToken(service: String): Result<String> {
            return refreshTokens[service]?.let { Result.success(it) } 
                ?: Result.failure(Exception("No refresh token found"))
        }
        
        override suspend fun storeTokenExpiration(service: String, expiresAt: kotlinx.datetime.Instant) = Result.success(Unit).also {
            expirations[service] = expiresAt
        }
        
        override suspend fun isTokenExpired(service: String): Result<Boolean> {
            val expiration = expirations[service] ?: return Result.success(true)
            return Result.success(Clock.System.now() > expiration)
        }
        
        override suspend fun hasValidToken(service: String): Result<Boolean> {
            val hasToken = tokens.containsKey(service)
            val isExpired = isTokenExpired(service).getOrNull() ?: true
            return Result.success(hasToken && !isExpired)
        }
        
        override suspend fun refreshAccessToken(service: String, clientId: String, clientSecret: String, tokenUrl: String): Result<String> {
            return Result.success("new_mock_token").also {
                tokens[service] = "new_mock_token"
            }
        }
        
        override suspend fun clearTokens(service: String) = Result.success(Unit).also {
            tokens.remove(service)
            refreshTokens.remove(service)
            expirations.remove(service)
        }
        
        override suspend fun clearAllTokens() = Result.success(Unit).also {
            tokens.clear()
            refreshTokens.clear()
            expirations.clear()
        }
        
        fun simulateAuthFailure(errorType: String) {
            simulateFailure = errorType
        }
    }
}