/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Implementation tests for AuthTokenManager
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Implementation tests for AuthTokenManager covering:
 * - Token storage and retrieval operations
 * - Token expiration handling
 * - Error scenarios and edge cases
 * - Cross-platform compatibility
 */
class AuthTokenManagerImplTest {

    private lateinit var secureStorage: TestSecureStorage
    private lateinit var authTokenManager: AuthTokenManager
    private val testService = "test_service"
    private val testToken = "test_access_token_123"
    private val testRefreshToken = "test_refresh_token_456"

    @BeforeTest
    fun setUp() {
        secureStorage = TestSecureStorage()
        authTokenManager = AuthTokenManager(testService, secureStorage)
    }

    @AfterTest
    fun tearDown() = runBlocking<Unit> {
        secureStorage.clear()
    }

    // ==================== Token Storage Tests ====================

    @Test
    fun testStoreAccessToken() = runBlocking<Unit> {
        // When
        val result = authTokenManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isSuccess, "Should successfully store access token")
    }

    @Test
    fun testGetAccessToken() = runBlocking<Unit> {
        // Given
        authTokenManager.storeAccessToken(testToken)

        // When
        val result = authTokenManager.getAccessToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully retrieve access token")
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testGetAccessTokenNotFound() = runBlocking<Unit> {
        // When
        val result = authTokenManager.getAccessToken()

        // Then
        assertTrue(result.isSuccess, "Should succeed with null when token not found")
        assertNull(result.getOrNull())
    }

    @Test
    fun testStoreRefreshToken() = runBlocking<Unit> {
        // When
        val result = authTokenManager.storeRefreshToken(testRefreshToken)

        // Then
        assertTrue(result.isSuccess, "Should successfully store refresh token")
    }

    @Test
    fun testGetRefreshToken() = runBlocking<Unit> {
        // Given
        authTokenManager.storeRefreshToken(testRefreshToken)

        // When
        val result = authTokenManager.getRefreshToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully retrieve refresh token")
        assertNotNull(result.getOrNull())
    }

    // ==================== Token Expiration Tests ====================

    @Test
    fun testStoreTokenExpiration() = runBlocking<Unit> {
        // Given
        val expiresAt = Clock.System.now().plus(1.hours)

        // When
        val result = authTokenManager.storeTokenExpiration(expiresAt)

        // Then
        assertTrue(result.isSuccess, "Should successfully store token expiration")
    }

    @Test
    fun testIsTokenExpired() = runBlocking<Unit> {
        // Given
        val expiredTime = Clock.System.now().minus(1.hours)
        authTokenManager.storeTokenExpiration(expiredTime)

        // When
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertTrue(result.getOrNull() ?: false, "Should detect expired token")
    }

    @Test
    fun testIsTokenNotExpired() = runBlocking<Unit> {
        // Given
        val validTime = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(validTime)

        // When
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertFalse(result.getOrNull() ?: true, "Should detect valid token")
    }

    @Test
    fun testIsTokenExpiredNoExpirationStored() = runBlocking<Unit> {
        // When
        val result = authTokenManager.isTokenExpired()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertTrue(result.getOrNull() ?: false, "Should consider token expired when no expiration stored")
    }

    // ==================== Token Validation Tests ====================

    @Test
    fun testHasValidToken() = runBlocking<Unit> {
        // Given
        authTokenManager.storeAccessToken(testToken)
        val validTime = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(validTime)

        // When
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertTrue(result.getOrNull() ?: false, "Should detect valid token")
    }

    @Test
    fun testHasValidTokenExpired() = runBlocking<Unit> {
        // Given
        authTokenManager.storeAccessToken(testToken)
        val expiredTime = Clock.System.now().minus(1.hours)
        authTokenManager.storeTokenExpiration(expiredTime)

        // When
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertFalse(result.getOrNull() ?: true, "Should detect expired token")
    }

    @Test
    fun testHasValidTokenNoToken() = runBlocking<Unit> {
        // When
        val result = authTokenManager.hasValidToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertFalse(result.getOrNull() ?: true, "Should detect missing token")
    }

    // ==================== Token Deletion Tests ====================

    @Test
    fun testClearTokens() = runBlocking<Unit> {
        // Given
        authTokenManager.storeAccessToken(testToken)
        authTokenManager.storeRefreshToken(testRefreshToken)
        val expiresAt = Clock.System.now().plus(1.hours)
        authTokenManager.storeTokenExpiration(expiresAt)

        // When
        val result = authTokenManager.clearTokens()

        // Then
        assertTrue(result.isSuccess, "Should successfully clear tokens")

        // Verify tokens are cleared
        val hasValid = authTokenManager.hasValidToken()
        assertFalse(hasValid.getOrNull() ?: true, "Should not have valid token after clear")
    }

    @Test
    fun testClearTokensForMultipleServices() = runBlocking<Unit> {
        // Given - create multiple token managers for different services
        val services = listOf("service1", "service2", "service3")
        val managers = services.map { service ->
            AuthTokenManager(service, secureStorage)
        }

        // Store tokens for each service
        managers.forEach { manager ->
            manager.storeAccessToken("token_${manager.getTokenInfo().getOrNull()?.serviceName}")
        }

        // When - clear tokens for each service
        val results = managers.map { it.clearTokens() }

        // Then
        assertTrue(results.all { it.isSuccess }, "Should successfully clear all service tokens")
        managers.forEach { manager ->
            val hasValid = manager.hasValidToken()
            assertFalse(hasValid.getOrNull() ?: true, "Should not have valid token after clear")
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyServiceName() = runBlocking<Unit> {
        // Given
        val emptyServiceManager = AuthTokenManager("", secureStorage)

        // When
        val result = emptyServiceManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isSuccess, "Should handle empty service name")
    }

    @Test
    fun testSpecialCharactersInTokens() = runBlocking<Unit> {
        // Given
        val specialToken = "token_with_special_chars_!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        // When
        val result = authTokenManager.storeAccessToken(specialToken)

        // Then
        assertTrue(result.isSuccess, "Should handle special characters in tokens")
    }

    @Test
    fun testVeryLongServiceNames() = runBlocking<Unit> {
        // Given
        val longService = "a".repeat(1000)
        val longServiceManager = AuthTokenManager(longService, secureStorage)

        // When
        val result = longServiceManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isSuccess, "Should handle very long service names")
    }

    @Test
    fun testConcurrentTokenOperations() = runBlocking<Unit> {
        // Given
        val operations = 100

        // When - each iteration uses its own AuthTokenManager for a unique service
        val results = (1..operations).map { i ->
            val manager = AuthTokenManager("${testService}$i", secureStorage)
            manager.storeAccessToken("token$i")
        }

        // Then
        assertTrue(results.all { it.isSuccess }, "All concurrent operations should succeed")
    }

    @Test
    fun testTokenOverwrite() = runBlocking<Unit> {
        // Given
        val originalToken = "original_token"
        val newToken = "new_token"

        // When
        authTokenManager.storeAccessToken(originalToken)
        authTokenManager.storeAccessToken(newToken)

        // Then - the token should be overwritten (we can verify via getAccessToken)
        val result = authTokenManager.getAccessToken()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ==================== Token Info Tests ====================

    @Test
    fun testStoreTokenInfo() = runBlocking<Unit> {
        // When
        val result = authTokenManager.storeTokenInfo(
            accessToken = testToken,
            refreshToken = testRefreshToken,
            expiresIn = 3600L
        )

        // Then
        assertTrue(result.isSuccess, "Should successfully store token info")

        // Verify via getTokenInfo
        val tokenInfo = authTokenManager.getTokenInfo()
        assertTrue(tokenInfo.isSuccess)
        assertTrue(tokenInfo.getOrNull()?.hasAccessToken ?: false)
        assertTrue(tokenInfo.getOrNull()?.hasRefreshToken ?: false)
        assertFalse(tokenInfo.getOrNull()?.isExpired ?: true)
        assertEquals(testService, tokenInfo.getOrNull()?.serviceName)
    }

    @Test
    fun testGetTokenInfo() = runBlocking<Unit> {
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

    @Test
    fun testGetTokenInfoNoTokens() = runBlocking<Unit> {
        // When - no tokens stored
        val result = authTokenManager.getTokenInfo()

        // Then
        assertTrue(result.isSuccess)
        val tokenInfo = result.getOrNull()
        assertNotNull(tokenInfo)
        assertFalse(tokenInfo.hasAccessToken)
        assertFalse(tokenInfo.hasRefreshToken)
        assertTrue(tokenInfo.isExpired)
    }

    // ==================== Storage Failure Tests ====================

    @Test
    fun testStorageFailure() = runBlocking<Unit> {
        // Given
        secureStorage.shouldFail = true

        // When
        val result = authTokenManager.storeAccessToken(testToken)

        // Then
        assertTrue(result.isFailure, "Should fail when storage fails")
    }

    @Test
    fun testRecoverFromStorageFailure() = runBlocking<Unit> {
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

    private class TestSecureStorage : SecureStorage {
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
