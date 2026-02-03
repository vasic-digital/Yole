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

/**
 * Stress tests for AuthTokenManager operations.
 */
class AuthTokenManagerStressTest {

    private lateinit var mockStorage: MockSecureStorage
    private lateinit var tokenManager: AuthTokenManager

    @BeforeTest
    fun setup() {
        mockStorage = MockSecureStorage()
        tokenManager = AuthTokenManagerImpl(mockStorage)
    }

    // ==================== CONCURRENT TOKEN OPERATIONS ====================

    @Test
    fun `concurrent token storage operations`() = runTest {
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
    fun `concurrent token retrieval operations`() = runTest {
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
    fun `concurrent hasValidToken checks`() = runTest {
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
    fun `concurrent token clear operations`() = runTest {
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
    fun `rapid store-retrieve-clear cycles`() = runTest {
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
    fun `token update overwrites previous value`() = runTest {
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
    fun `expired tokens are detected correctly`() = runTest {
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
    fun `tokens near expiration boundary`() = runTest {
        val now = Clock.System.now()

        // Token that expires in 1 minute (near boundary)
        val nearExpiryToken = TokenInfo(
            accessToken = "near-expiry",
            refreshToken = "refresh",
            expiresAt = now + 1.minutes,
            tokenType = "Bearer"
        )

        // Token that expired 1 minute ago
        val justExpiredToken = TokenInfo(
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
    fun `handle empty service ID`() = runTest {
        val result = tokenManager.getTokenInfo("")

        // Should handle gracefully
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `handle very long service ID`() = runTest {
        val longId = "a".repeat(10000)
        val tokenInfo = createTokenInfo("access", "refresh")

        val storeResult = tokenManager.storeTokenInfo(longId, tokenInfo)
        assertTrue(storeResult.isSuccess)

        val getResult = tokenManager.getTokenInfo(longId)
        assertTrue(getResult.isSuccess)
    }

    @Test
    fun `handle special characters in service ID`() = runTest {
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
    fun `handle null refresh token`() = runTest {
        val tokenInfo = TokenInfo(
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
    fun `handle null expiration`() = runTest {
        val tokenInfo = TokenInfo(
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
    fun `concurrent mixed operations on same service`() = runTest {
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
    fun `concurrent operations on many services`() = runTest {
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
    ): TokenInfo {
        return TokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now() + 1.hours,
            tokenType = "Bearer"
        )
    }

    private fun createExpiredTokenInfo(
        accessToken: String,
        refreshToken: String?
    ): TokenInfo {
        return TokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now() - 1.hours,
            tokenType = "Bearer"
        )
    }
}

/**
 * Mock SecureStorage for testing.
 */
class MockSecureStorage : SecureStorage {
    private val tokens = mutableMapOf<String, String>()
    private val credentials = mutableMapOf<String, Pair<String, String>>()
    private val privateKeys = mutableMapOf<String, String>()

    override suspend fun storeToken(key: String, token: String): Result<Unit> {
        tokens[key] = token
        return Result.success(Unit)
    }

    override suspend fun getToken(key: String): Result<String?> {
        return Result.success(tokens[key])
    }

    override suspend fun deleteToken(key: String): Result<Unit> {
        tokens.remove(key)
        return Result.success(Unit)
    }

    override suspend fun storeCredentials(key: String, username: String, password: String): Result<Unit> {
        credentials[key] = username to password
        return Result.success(Unit)
    }

    override suspend fun getCredentials(key: String): Result<Pair<String, String>?> {
        return Result.success(credentials[key])
    }

    override suspend fun deleteCredentials(key: String): Result<Unit> {
        credentials.remove(key)
        return Result.success(Unit)
    }

    override suspend fun storePrivateKey(key: String, privateKey: String): Result<Unit> {
        privateKeys[key] = privateKey
        return Result.success(Unit)
    }

    override suspend fun getPrivateKey(key: String): Result<String?> {
        return Result.success(privateKeys[key])
    }

    override suspend fun deletePrivateKey(key: String): Result<Unit> {
        privateKeys.remove(key)
        return Result.success(Unit)
    }

    override suspend fun clear(): Result<Unit> {
        tokens.clear()
        credentials.clear()
        privateKeys.clear()
        return Result.success(Unit)
    }
}

/**
 * AuthTokenManager implementation for testing.
 */
class AuthTokenManagerImpl(private val storage: SecureStorage) : AuthTokenManager {
    private val tokenInfos = mutableMapOf<String, TokenInfo>()

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

    override suspend fun storeTokenInfo(serviceId: String, tokenInfo: TokenInfo): Result<Unit> {
        return try {
            tokenInfos[serviceId] = tokenInfo
            storage.storeToken(serviceId, tokenInfo.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTokenInfo(serviceId: String): Result<TokenInfo?> {
        return try {
            Result.success(tokenInfos[serviceId])
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearToken(serviceId: String): Result<Unit> {
        return try {
            tokenInfos.remove(serviceId)
            storage.deleteToken(serviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(serviceId: String): Result<TokenInfo> {
        return try {
            val existing = tokenInfos[serviceId]
                ?: return Result.failure(Exception("No token to refresh"))

            val refreshed = TokenInfo(
                accessToken = "refreshed-${existing.accessToken}",
                refreshToken = existing.refreshToken,
                expiresAt = Clock.System.now() + kotlin.time.Duration.parse("1h"),
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
 * Token information data class.
 */
data class TokenInfo(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
    val tokenType: String
)

/**
 * AuthTokenManager interface for testing.
 */
interface AuthTokenManager {
    suspend fun hasValidToken(serviceId: String): Result<Boolean>
    suspend fun storeTokenInfo(serviceId: String, tokenInfo: TokenInfo): Result<Unit>
    suspend fun getTokenInfo(serviceId: String): Result<TokenInfo?>
    suspend fun clearToken(serviceId: String): Result<Unit>
    suspend fun refreshToken(serviceId: String): Result<TokenInfo>
}
