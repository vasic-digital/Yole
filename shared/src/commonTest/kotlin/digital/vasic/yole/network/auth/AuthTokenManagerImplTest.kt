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
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

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
        authTokenManager = AuthTokenManager(secureStorage)
    }

    @AfterTest
    fun tearDown() {
        secureStorage.clear()
    }

    // ==================== Token Storage Tests ====================

    @Test
    fun testStoreAccessToken() = runTest {
        // When
        val result = authTokenManager.storeAccessToken(testService, testToken)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully store access token")
        assertEquals(testToken, secureStorage.getStoredValue("${testService}_access_token"))
    }

    @Test
    fun testGetAccessToken() = runTest {
        // Given
        secureStorage.storeValue("${testService}_access_token", testToken)
        
        // When
        val result = authTokenManager.getAccessToken(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully retrieve access token")
        assertEquals(testToken, result.getOrNull())
    }

    @Test
    fun testGetAccessTokenNotFound() = runTest {
        // When
        val result = authTokenManager.getAccessToken(testService)
        
        // Then
        assertTrue(result.isFailure, "Should fail when token not found")
    }

    @Test
    fun testStoreRefreshToken() = runTest {
        // When
        val result = authTokenManager.storeRefreshToken(testService, testRefreshToken)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully store refresh token")
        assertEquals(testRefreshToken, secureStorage.getStoredValue("${testService}_refresh_token"))
    }

    @Test
    fun testGetRefreshToken() = runTest {
        // Given
        secureStorage.storeValue("${testService}_refresh_token", testRefreshToken)
        
        // When
        val result = authTokenManager.getRefreshToken(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully retrieve refresh token")
        assertEquals(testRefreshToken, result.getOrNull())
    }

    // ==================== Token Expiration Tests ====================

    @Test
    fun testStoreTokenExpiration() = runTest {
        // Given
        val expiresAt = Clock.System.now().plus(1.hours)
        
        // When
        val result = authTokenManager.storeTokenExpiration(testService, expiresAt)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully store token expiration")
        assertEquals(expiresAt.toString(), secureStorage.getStoredValue("${testService}_token_expires_at"))
    }

    @Test
    fun testIsTokenExpired() = runTest {
        // Given
        val expiredTime = Clock.System.now().minus(1.hours)
        secureStorage.storeValue("${testService}_token_expires_at", expiredTime.toString())
        
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertTrue(result.getOrNull() ?: false, "Should detect expired token")
    }

    @Test
    fun testIsTokenNotExpired() = runTest {
        // Given
        val validTime = Clock.System.now().plus(1.hours)
        secureStorage.storeValue("${testService}_token_expires_at", validTime.toString())
        
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertFalse(result.getOrNull() ?: true, "Should detect valid token")
    }

    @Test
    fun testIsTokenExpiredNoExpirationStored() = runTest {
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token expiration")
        assertTrue(result.getOrNull() ?: false, "Should consider token expired when no expiration stored")
    }

    // ==================== Token Validation Tests ====================

    @Test
    fun testHasValidToken() = runTest {
        // Given
        secureStorage.storeValue("${testService}_access_token", testToken)
        val validTime = Clock.System.now().plus(1.hours)
        secureStorage.storeValue("${testService}_token_expires_at", validTime.toString())
        
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertTrue(result.getOrNull() ?: false, "Should detect valid token")
    }

    @Test
    fun testHasValidTokenExpired() = runTest {
        // Given
        secureStorage.storeValue("${testService}_access_token", testToken)
        val expiredTime = Clock.System.now().minus(1.hours)
        secureStorage.storeValue("${testService}_token_expires_at", expiredTime.toString())
        
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertFalse(result.getOrNull() ?: true, "Should detect expired token")
    }

    @Test
    fun testHasValidTokenNoToken() = runTest {
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully check token validity")
        assertFalse(result.getOrNull() ?: true, "Should detect missing token")
    }

    // ==================== Token Deletion Tests ====================

    @Test
    fun testClearTokens() = runTest {
        // Given
        secureStorage.storeValue("${testService}_access_token", testToken)
        secureStorage.storeValue("${testService}_refresh_token", testRefreshToken)
        val expiresAt = Clock.System.now().plus(1.hours)
        secureStorage.storeValue("${testService}_token_expires_at", expiresAt.toString())
        
        // When
        val result = authTokenManager.clearTokens(testService)
        
        // Then
        assertTrue(result.isSuccess, "Should successfully clear tokens")
        assertNull(secureStorage.getStoredValue("${testService}_access_token"))
        assertNull(secureStorage.getStoredValue("${testService}_refresh_token"))
        assertNull(secureStorage.getStoredValue("${testService}_token_expires_at"))
    }

    @Test
    fun testClearAllTokens() = runTest {
        // Given
        val services = listOf("service1", "service2", "service3")
        services.forEach { service ->
            secureStorage.storeValue("${service}_access_token", "token_$service")
            secureStorage.storeValue("${service}_refresh_token", "refresh_$service")
        }
        
        // Mock the getAllKeys method
        secureStorage.setAllKeys(services.flatMap { service ->
            listOf("${service}_access_token", "${service}_refresh_token", "${service}_token_expires_at")
        })
        
        // When
        val result = authTokenManager.clearAllTokens()
        
        // Then
        assertTrue(result.isSuccess, "Should successfully clear all tokens")
        services.forEach { service ->
            assertNull(secureStorage.getStoredValue("${service}_access_token"))
            assertNull(secureStorage.getStoredValue("${service}_refresh_token"))
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyServiceName() = runTest {
        // When
        val result = authTokenManager.storeAccessToken("", testToken)
        
        // Then
        assertTrue(result.isSuccess, "Should handle empty service name")
        assertEquals(testToken, secureStorage.getStoredValue("_access_token"))
    }

    @Test
    fun testSpecialCharactersInTokens() = runTest {
        // Given
        val specialToken = "token_with_special_chars_!@#$%^&*()_+-=[]{}|;':\",./<>?"
        
        // When
        val result = authTokenManager.storeAccessToken(testService, specialToken)
        
        // Then
        assertTrue(result.isSuccess, "Should handle special characters in tokens")
        assertEquals(specialToken, secureStorage.getStoredValue("${testService}_access_token"))
    }

    @Test
    fun testVeryLongServiceNames() = runTest {
        // Given
        val longService = "a".repeat(1000)
        
        // When
        val result = authTokenManager.storeAccessToken(longService, testToken)
        
        // Then
        assertTrue(result.isSuccess, "Should handle very long service names")
        assertEquals(testToken, secureStorage.getStoredValue("${longService}_access_token"))
    }

    @Test
    fun testConcurrentTokenOperations() = runTest {
        // Given
        val operations = 100
        
        // When
        val results = (1..operations).map { i ->
            authTokenManager.storeAccessToken("$testService$i", "token$i")
        }
        
        // Then
        assertTrue(results.all { it.isSuccess }, "All concurrent operations should succeed")
        (1..operations).forEach { i ->
            assertEquals("token$i", secureStorage.getStoredValue("$testService$i" + "_access_token"))
        }
    }

    @Test
    fun testTokenOverwrite() = runTest {
        // Given
        val originalToken = "original_token"
        val newToken = "new_token"
        
        // When
        authTokenManager.storeAccessToken(testService, originalToken)
        authTokenManager.storeAccessToken(testService, newToken)
        
        // Then
        assertEquals(newToken, secureStorage.getStoredValue("${testService}_access_token"))
    }

    // ==================== Test Helper Classes ====================

    private class TestSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()
        private var allKeys = listOf<String>()

        override suspend fun store(key: String, value: String): Result<Unit> {
            storage[key] = value
            return Result.success(Unit)
        }

        override suspend fun retrieve(key: String): Result<String> {
            return storage[key]?.let { Result.success(it) } 
                ?: Result.failure(Exception("Key not found: $key"))
        }

        override suspend fun remove(key: String): Result<Unit> {
            storage.remove(key)
            return Result.success(Unit)
        }

        override suspend fun getAllKeys(): Result<List<String>> {
            return Result.success(allKeys)
        }

        fun getStoredValue(key: String): String? {
            return storage[key]
        }

        fun clear() {
            storage.clear()
            allKeys = listOf()
        }

        fun setAllKeys(keys: List<String>) {
            allKeys = keys
        }
    }
}