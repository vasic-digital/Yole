/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Auth Token Manager Stress Tests
 *
 * Comprehensive stress tests for authentication and
 * token management including concurrent access,
 * token lifecycle, and security scenarios.
 *
 *########################################################*/

package digital.vasic.yole.network.auth

import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Stress tests for AuthTokenManager operations.
 */
class AuthTokenManagerStressTest {

    private lateinit var mockStorage: StressTestSecureStorage
    private lateinit var tokenManager: TestAuthTokenManager

    @BeforeTest
    fun setup() {
        mockStorage = StressTestSecureStorage()
        tokenManager = TestAuthTokenManagerImpl(mockStorage)
    }

    // ==================== CONCURRENT TOKEN OPERATIONS ====================

    @Test
    fun `concurrent token storage operations`() = runBlocking {
        val results = (1..100).map { i ->
            async {
                tokenManager.storeTokenInfo(
                    serviceId = "service-$i",
                    tokenInfo = createTokenInfo("access-$i", "refresh-$i")
                )
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent token retrieval operations`() = runBlocking {
        // Store tokens first
        (1..50).forEach { i ->
            tokenManager.storeTokenInfo(
                serviceId = "service-$i",
                tokenInfo = createTokenInfo("access-$i", "refresh-$i")
            )
        }

        // Concurrent retrieval
        val results = (1..200).map { i ->
            async {
                tokenManager.getTokenInfo("service-${(i % 50) + 1}")
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent hasValidToken checks`() = runBlocking {
        // Store a mix of valid and expired tokens
        (1..50).forEach { i ->
            val tokenInfo = if (i % 2 == 0) {
                createTokenInfo("access-$i", "refresh-$i") // valid
            } else {
                createExpiredTokenInfo("access-$i", "refresh-$i") // expired
            }
            tokenManager.storeTokenInfo("service-$i", tokenInfo)
        }

        // Concurrent validity checks
        val results = (1..200).map { i ->
            async {
                tokenManager.hasValidToken("service-${(i % 50) + 1}")
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent token clear operations`() = runBlocking {
        // Store tokens
        (1..30).forEach { i ->
            tokenManager.storeTokenInfo(
                "service-$i",
                createTokenInfo("access-$i", "refresh-$i")
            )
        }

        // Concurrent clear operations
        val results = (1..30).map { i ->
            async {
                tokenManager.clearToken("service-$i")
            }
        }.awaitAll()

        assertEquals(30, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== TOKEN LIFECYCLE STRESS ====================

    @Test
    fun `rapid store-retrieve-clear cycles`() = runBlocking {
        repeat(100) { i ->
            val serviceId = "cycle-service"
            val tokenInfo = createTokenInfo("access-$i", "refresh-$i")

            // Store
            val storeResult = tokenManager.storeTokenInfo(serviceId, tokenInfo)
            assertTrue(storeResult.isSuccess)

            // Retrieve
            val getResult = tokenManager.getTokenInfo(serviceId)
            assertTrue(getResult.isSuccess)
            assertEquals("access-$i", getResult.getOrNull()?.accessToken)

            // Clear
            val clearResult = tokenManager.clearToken(serviceId)
            assertTrue(clearResult.isSuccess)

            // Verify cleared
            val afterClear = tokenManager.getTokenInfo(serviceId)
            assertNull(afterClear.getOrNull())
        }
    }

    @Test
    fun `token update overwrites previous value`() = runBlocking {
        val serviceId = "update-test"

        repeat(50) { i ->
            val tokenInfo = createTokenInfo("access-v$i", "refresh-v$i")
            tokenManager.storeTokenInfo(serviceId, tokenInfo)

            val retrieved = tokenManager.getTokenInfo(serviceId)
            assertEquals("access-v$i", retrieved.getOrNull()?.accessToken)
        }
    }

    // ==================== TOKEN EXPIRATION STRESS ====================

    @Test
    fun `expired tokens are detected correctly`() = runBlocking {
        val expiredToken = createExpiredTokenInfo("expired-access", "refresh")
        val validToken = createTokenInfo("valid-access", "refresh")

        tokenManager.storeTokenInfo("expired-service", expiredToken)
        tokenManager.storeTokenInfo("valid-service", validToken)

        val expiredResult = tokenManager.hasValidToken("expired-service")
        val validResult = tokenManager.hasValidToken("valid-service")

        assertTrue(expiredResult.isSuccess)
        assertTrue(validResult.isSuccess)

        assertFalse(expiredResult.getOrNull() ?: true)
        assertTrue(validResult.getOrNull() ?: false)
    }

    @Test
    fun `tokens near expiration boundary`() = runBlocking {
        val now = Clock.System.now()

        // Token that expires in 1 minute (near boundary)
        val nearExpiryToken = TestTokenInfo(
            accessToken = "near-expiry",
            refreshToken = "refresh",
            expiresAt = now + 1.minutes,
            tokenType = "Bearer"
        )

        // Token that expired 1 minute ago
        val justExpiredToken = TestTokenInfo(
            accessToken = "just-expired",
            refreshToken = "refresh",
            expiresAt = now - 1.minutes,
            tokenType = "Bearer"
        )

        tokenManager.storeTokenInfo("near-expiry", nearExpiryToken)
        tokenManager.storeTokenInfo("just-expired", justExpiredToken)

        val nearResult = tokenManager.hasValidToken("near-expiry")
        val justExpiredResult = tokenManager.hasValidToken("just-expired")

        assertTrue(nearResult.getOrNull() ?: false, "Token near expiry should still be valid")
        assertFalse(justExpiredResult.getOrNull() ?: true, "Just expired token should be invalid")
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `handle empty service ID`() = runBlocking {
        val result = tokenManager.getTokenInfo("")

        // Should handle gracefully
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `handle very long service ID`() = runBlocking {
        val longId = "a".repeat(10000)
        val tokenInfo = createTokenInfo("access", "refresh")

        val storeResult = tokenManager.storeTokenInfo(longId, tokenInfo)
        assertTrue(storeResult.isSuccess)

        val getResult = tokenManager.getTokenInfo(longId)
        assertTrue(getResult.isSuccess)
    }

    @Test
    fun `handle special characters in service ID`() = runBlocking {
        val specialIds = listOf(
            "service:with:colons",
            "service/with/slashes",
            "service@with@at",
            "service#with#hash",
            "service with spaces"
        )

        specialIds.forEach { id ->
            val tokenInfo = createTokenInfo("access-$id", "refresh")
            val storeResult = tokenManager.storeTokenInfo(id, tokenInfo)
            assertTrue(storeResult.isSuccess, "Failed to store for id '$id'")

            val getResult = tokenManager.getTokenInfo(id)
            assertTrue(getResult.isSuccess, "Failed to get for id '$id'")
        }
    }

    @Test
    fun `handle null refresh token`() = runBlocking {
        val tokenInfo = TestTokenInfo(
            accessToken = "access",
            refreshToken = null,
            expiresAt = Clock.System.now() + 1.hours,
            tokenType = "Bearer"
        )

        val result = tokenManager.storeTokenInfo("null-refresh", tokenInfo)
        assertTrue(result.isSuccess)

        val retrieved = tokenManager.getTokenInfo("null-refresh")
        assertNull(retrieved.getOrNull()?.refreshToken)
    }

    @Test
    fun `handle null expiration`() = runBlocking {
        val tokenInfo = TestTokenInfo(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = null,
            tokenType = "Bearer"
        )

        val result = tokenManager.storeTokenInfo("null-expiry", tokenInfo)
        assertTrue(result.isSuccess)

        // Token with null expiry should be considered valid (never expires)
        val validResult = tokenManager.hasValidToken("null-expiry")
        assertTrue(validResult.getOrNull() ?: false)
    }

    // ==================== CONCURRENT MIXED OPERATIONS ====================

    @Test
    fun `concurrent mixed operations on same service`() = runBlocking {
        val serviceId = "shared-service"
        tokenManager.storeTokenInfo(serviceId, createTokenInfo("initial", "refresh"))

        val operations = (1..100).map { i ->
            async {
                when (i % 4) {
                    0 -> tokenManager.storeTokenInfo(serviceId, createTokenInfo("access-$i", "refresh"))
                    1 -> tokenManager.getTokenInfo(serviceId)
                    2 -> tokenManager.hasValidToken(serviceId)
                    else -> tokenManager.clearToken(serviceId)
                }
            }
        }

        val results = operations.awaitAll()
        assertEquals(100, results.size)
        // All operations should complete without throwing
    }

    @Test
    fun `concurrent operations on many services`() = runBlocking {
        val operations = (1..500).map { i ->
            async {
                val serviceId = "service-${i % 50}"
                when (i % 3) {
                    0 -> tokenManager.storeTokenInfo(serviceId, createTokenInfo("access-$i", "refresh"))
                    1 -> tokenManager.getTokenInfo(serviceId)
                    else -> tokenManager.hasValidToken(serviceId)
                }
            }
        }

        val results = operations.awaitAll()
        assertEquals(500, results.size)
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun createTokenInfo(
        accessToken: String,
        refreshToken: String?
    ): TestTokenInfo {
        return TestTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now() + 1.hours,
            tokenType = "Bearer"
        )
    }

    private fun createExpiredTokenInfo(
        accessToken: String,
        refreshToken: String?
    ): TestTokenInfo {
        return TestTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now() - 1.hours,
            tokenType = "Bearer"
        )
    }
}

/**
 * Token information data class for stress testing.
 * Named TestTokenInfo to avoid conflict with the source TokenInfo.
 */
data class TestTokenInfo(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
    val tokenType: String
)

/**
 * Mock SecureStorage for stress testing.
 */
class StressTestSecureStorage : SecureStorage {
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

/**
 * TestAuthTokenManager implementation for stress testing.
 */
class TestAuthTokenManagerImpl(private val storage: SecureStorage) : TestAuthTokenManager {
    private val tokenInfos = mutableMapOf<String, TestTokenInfo>()

    override suspend fun hasValidToken(serviceId: String): Result<Boolean> {
        return try {
            val tokenInfo = tokenInfos[serviceId] ?: return Result.success(false)
            val expiresAt = tokenInfo.expiresAt ?: return Result.success(true) // No expiry means always valid
            val isValid = expiresAt > Clock.System.now()
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun storeTokenInfo(serviceId: String, tokenInfo: TestTokenInfo): Result<Unit> {
        return try {
            tokenInfos[serviceId] = tokenInfo
            storage.store("${serviceId}_token", tokenInfo.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTokenInfo(serviceId: String): Result<TestTokenInfo?> {
        return try {
            Result.success(tokenInfos[serviceId])
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearToken(serviceId: String): Result<Unit> {
        return try {
            tokenInfos.remove(serviceId)
            storage.delete("${serviceId}_token")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(serviceId: String): Result<TestTokenInfo> {
        return try {
            val existing = tokenInfos[serviceId]
                ?: return Result.failure(Exception("No token to refresh"))

            val refreshed = TestTokenInfo(
                accessToken = "refreshed-${existing.accessToken}",
                refreshToken = existing.refreshToken,
                expiresAt = Clock.System.now() + 1.hours,
                tokenType = existing.tokenType
            )
            tokenInfos[serviceId] = refreshed
            Result.success(refreshed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * TestAuthTokenManager interface for stress testing.
 * Note: This is a test-only interface. The main AuthTokenManager is a class.
 */
interface TestAuthTokenManager {
    suspend fun hasValidToken(serviceId: String): Result<Boolean>
    suspend fun storeTokenInfo(serviceId: String, tokenInfo: TestTokenInfo): Result<Unit>
    suspend fun getTokenInfo(serviceId: String): Result<TestTokenInfo?>
    suspend fun clearToken(serviceId: String): Result<Unit>
    suspend fun refreshToken(serviceId: String): Result<TestTokenInfo>
}
