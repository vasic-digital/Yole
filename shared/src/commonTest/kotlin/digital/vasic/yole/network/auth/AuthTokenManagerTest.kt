/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for AuthTokenManager
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Comprehensive tests for AuthTokenManager covering:
 * - Token storage and retrieval
 * - Token expiration handling
 * - Token info management
 * - Error scenarios and edge cases
 * - Cross-platform compatibility
 */
class AuthTokenManagerTest {

    private lateinit var secureStorage: InMemorySecureStorage
    private lateinit var authTokenManager: AuthTokenManager
    private val testService = "test_service"
    private val testToken = "test_access_token_123"
    private val testRefreshToken = "test_refresh_token_456"

    @BeforeTest
    fun setUp() {
        secureStorage = InMemorySecureStorage()
        authTokenManager = AuthTokenManager(testService, secureStorage)
    }

    @AfterTest
    fun tearDown() = runTest {
        secureStorage.clear()
    }

    // ==================== Token Storage Tests ====================

    @Test
    fun `storeAccessToken should save token to secure storage`() = runTest {
        // When
        val result = authTokenManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `storeAccessToken should handle storage failure`() = runTest {
        // Given
        secureStorage.shouldFail = true

        // When
        val result = authTokenManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAccessToken should retrieve token from secure storage`() = runTest {
        // Given
        authTokenManager.storeAccessToken(testToken)

        // When
        val result = authTokenManager.getAccessToken()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getAccessToken should handle missing token`() = runTest {
        // When - no token stored
        val result = authTokenManager.getAccessToken()

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ==================== Refresh Token Tests ====================

    @Test
    fun `storeRefreshToken should save refresh token`() = runTest {
        // When
        val result = authTokenManager.storeRefreshToken(testRefreshToken)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getRefreshToken should retrieve refresh token`() = runTest {
        // Given
        authTokenManager.storeRefreshToken(testRefreshToken)

        // When
        val result = authTokenManager.getRefreshToken()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ==================== Token Expiration Tests ====================

    @Test
    fun `storeTokenExpiration should save expiration time`() = runTest {
        // Given
        val expiresAt = Clock.System.now().plus(1.hours)

        // When
        val result = authTokenManager.storeTokenExpiration(expiresAt)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `isTokenExpired should return true for expired token`() = runTest {
        // Given
        val expiredTime = Clock.System.now().minus(1.hours)
        authTokenManager.storeTokenExpiration(expiredTime)

        // When
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun `isTokenExpired should return false for valid token`() = runTest {
        // Given
        val validTime = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(validTime)

        // When
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `isTokenExpired should return true when no expiration stored`() = runTest {
        // When - no expiration stored
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false) // Should be considered expired
    }

    // ==================== Token Validation Tests ====================

    @Test
    fun `hasValidToken should return true for valid token`() = runTest {
        // Given
        authTokenManager.storeAccessToken(testToken)
        val validTime = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(validTime)

        // When
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun `hasValidToken should return false for expired token`() = runTest {
        // Given
        authTokenManager.storeAccessToken(testToken)
        val expiredTime = Clock.System.now().minus(1.hours)
        authTokenManager.storeTokenExpiration(expiredTime)

        // When
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `hasValidToken should return false when no token exists`() = runTest {
        // When - no token stored
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== Clear Token Tests ====================

    @Test
    fun `clearTokens should remove all tokens for service`() = runTest {
        // Given
        authTokenManager.storeAccessToken(testToken)
        authTokenManager.storeRefreshToken(testRefreshToken)
        val expiresAt = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(expiresAt)

        // When
        val result = authTokenManager.clearTokens()

        // Then
        assertTrue(result.isSuccess)

        // Verify tokens are cleared
        val hasValid = authTokenManager.hasValidToken()
        assertFalse(hasValid.getOrNull() ?: true)
    }

    // ==================== Token Info Tests ====================

    @Test
    fun `storeTokenInfo should store complete token information`() = runTest {
        // When
        val result = authTokenManager.storeTokenInfo(
            accessToken = testToken,
            refreshToken = testRefreshToken,
            expiresIn = 3600L
        )

        // Then
        assertTrue(result.isSuccess)

        // Verify token info
        val tokenInfo = authTokenManager.getTokenInfo()
        assertTrue(tokenInfo.isSuccess)
        assertTrue(tokenInfo.getOrNull()?.hasAccessToken ?: false)
        assertTrue(tokenInfo.getOrNull()?.hasRefreshToken ?: false)
        assertFalse(tokenInfo.getOrNull()?.isExpired ?: true)
        assertEquals(testService, tokenInfo.getOrNull()?.serviceName)
    }

    @Test
    fun `getTokenInfo should return token info`() = runTest {
        // Given
        authTokenManager.storeTokenInfo(
            accessToken = testToken,
            refreshToken = testRefreshToken,
            expiresIn = 3600L
        )

        // When
        val result = authTokenManager.getTokenInfo()

        // Then
        assertTrue(result.isSuccess)
        val tokenInfo = result.getOrNull()
        assertNotNull(tokenInfo)
        assertTrue(tokenInfo.hasAccessToken)
        assertTrue(tokenInfo.hasRefreshToken)
        assertEquals(testService, tokenInfo.serviceName)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle special characters in tokens`() = runTest {
        // Given
        val specialToken = "token_with_special_chars_+-=[]{}|;':,./<>?"

        // When
        val result = authTokenManager.storeAccessToken(specialToken)

        // Then
        assertTrue(result.isSuccess)
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun `should handle sequential token operations`() = runTest {
        // When
        val results = (1..100).map { i ->
            val manager = AuthTokenManager("${testService}$i", secureStorage)
            manager.storeAccessToken("token$i")
        }

        // Then
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should recover from partial storage failures`() = runTest {
        // Given - make storage fail
        secureStorage.shouldFail = true
        val firstResult = authTokenManager.storeAccessToken(testToken)
        assertTrue(firstResult.isFailure)

        // Now make storage work again
        secureStorage.shouldFail = false
        val secondResult = authTokenManager.storeAccessToken(testToken)

        // Then
        assertTrue(secondResult.isSuccess)
    }

    // ==================== Test Helper Classes ====================

    private class InMemorySecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()
        var shouldFail = false

        override suspend fun store(key: String, value: String): Result<Unit> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            storage[key] = value
            return Result.success(Unit)
        }

        override suspend fun retrieve(key: String): Result<String?> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            return Result.success(storage[key])
        }

        override suspend fun delete(key: String): Result<Unit> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            storage.remove(key)
            return Result.success(Unit)
        }

        override suspend fun contains(key: String): Result<Boolean> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            return Result.success(storage.containsKey(key))
        }

        override suspend fun listKeys(): Result<List<String>> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
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
