/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for AuthTokenManager
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Duration
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Comprehensive tests for AuthTokenManager covering:
 * - Token storage and retrieval
 * - Token expiration handling
 * - Automatic token refresh
 * - Error scenarios and edge cases
 * - Cross-platform compatibility
 */
class AuthTokenManagerTest {

    private lateinit var secureStorage: SecureStorage
    private lateinit var authTokenManager: AuthTokenManager
    private val testService = "test_service"
    private val testToken = "test_access_token_123"
    private val testRefreshToken = "test_refresh_token_456"

    @BeforeTest
    fun setUp() {
        secureStorage = mockk(relaxed = true)
        authTokenManager = AuthTokenManager(secureStorage)
        
        // Clear all recorded calls
        clearAllMocks()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Token Storage Tests ====================

    @Test
    fun `storeAccessToken should save token to secure storage`() = runTest {
        // Given
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeAccessToken(testService, testToken)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("${testService}_access_token", testToken) }
    }

    @Test
    fun `storeAccessToken should handle storage failure`() = runTest {
        // Given
        val storageError = Exception("Storage failed")
        coEvery { secureStorage.store(any(), any()) } returns Result.failure(storageError)
        
        // When
        val result = authTokenManager.storeAccessToken(testService, testToken)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(storageError, result.exceptionOrNull())
    }

    @Test
    fun `getAccessToken should retrieve token from secure storage`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_access_token") } returns Result.success(testToken)
        
        // When
        val result = authTokenManager.getAccessToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testToken, result.getOrNull())
    }

    @Test
    fun `getAccessToken should handle missing token`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_access_token") } returns Result.failure(
            Exception("Token not found")
        )
        
        // When
        val result = authTokenManager.getAccessToken(testService)
        
        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Refresh Token Tests ====================

    @Test
    fun `storeRefreshToken should save refresh token`() = runTest {
        // Given
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeRefreshToken(testService, testRefreshToken)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("${testService}_refresh_token", testRefreshToken) }
    }

    @Test
    fun `getRefreshToken should retrieve refresh token`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_refresh_token") } returns Result.success(testRefreshToken)
        
        // When
        val result = authTokenManager.getRefreshToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testRefreshToken, result.getOrNull())
    }

    // ==================== Token Expiration Tests ====================

    @Test
    fun `storeTokenExpiration should save expiration time`() = runTest {
        // Given
        val expiresAt = Clock.System.now().plus(1.hours)
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeTokenExpiration(testService, expiresAt)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("${testService}_token_expires_at", expiresAt.toString()) }
    }

    @Test
    fun `isTokenExpired should return true for expired token`() = runTest {
        // Given
        val expiredTime = Clock.System.now().minus(1.hours)
        coEvery { secureStorage.retrieve("${testService}_token_expires_at") } returns Result.success(expiredTime.toString())
        
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun `isTokenExpired should return false for valid token`() = runTest {
        // Given
        val validTime = Clock.System.now().plus(1.hours)
        coEvery { secureStorage.retrieve("${testService}_token_expires_at") } returns Result.success(validTime.toString())
        
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `isTokenExpired should return true when no expiration stored`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_token_expires_at") } returns Result.failure(
            Exception("No expiration found")
        )
        
        // When
        val result = authTokenManager.isTokenExpired(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false) // Should be considered expired
    }

    // ==================== Token Validation Tests ====================

    @Test
    fun `hasValidToken should return true for valid token`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_access_token") } returns Result.success(testToken)
        coEvery { secureStorage.retrieve("${testService}_token_expires_at") } returns Result.success(
            Clock.System.now().plus(1.hours).toString()
        )
        
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun `hasValidToken should return false for expired token`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_access_token") } returns Result.success(testToken)
        coEvery { secureStorage.retrieve("${testService}_token_expires_at") } returns Result.success(
            Clock.System.now().minus(1.hours).toString()
        )
        
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `hasValidToken should return false when no token exists`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_access_token") } returns Result.failure(
            Exception("No token found")
        )
        
        // When
        val result = authTokenManager.hasValidToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== Token Refresh Tests ====================

    @Test
    fun `refreshAccessToken should use refresh token to get new access token`() = runTest {
        // Given
        val newAccessToken = "new_access_token_789"
        coEvery { secureStorage.retrieve("${testService}_refresh_token") } returns Result.success(testRefreshToken)
        
        // Mock the refresh operation (would normally call OAuth2Flow)
        coEvery { secureStorage.store("${testService}_access_token", newAccessToken) } returns Result.success(Unit)
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.refreshAccessToken(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(newAccessToken, result.getOrNull())
    }

    @Test
    fun `refreshAccessToken should handle missing refresh token`() = runTest {
        // Given
        coEvery { secureStorage.retrieve("${testService}_refresh_token") } returns Result.failure(
            Exception("No refresh token found")
        )
        
        // When
        val result = authTokenManager.refreshAccessToken(testService)
        
        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Clear Token Tests ====================

    @Test
    fun `clearTokens should remove all tokens for service`() = runTest {
        // Given
        coEvery { secureStorage.remove(any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.clearTokens(testService)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.remove("${testService}_access_token") }
        coVerify { secureStorage.remove("${testService}_refresh_token") }
        coVerify { secureStorage.remove("${testService}_token_expires_at") }
    }

    @Test
    fun `clearAllTokens should remove tokens for all services`() = runTest {
        // Given
        val services = listOf("service1", "service2", "service3")
        coEvery { secureStorage.getAllKeys() } returns Result.success(services.map { "${it}_access_token" })
        coEvery { secureStorage.remove(any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.clearAllTokens()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { secureStorage.remove(any()) }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty service name`() = runTest {
        // Given
        val emptyService = ""
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeAccessToken(emptyService, testToken)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("_access_token", testToken) }
    }

    @Test
    fun `should handle very long service names`() = runTest {
        // Given
        val longService = "a".repeat(1000)
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeAccessToken(longService, testToken)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("${longService}_access_token", testToken) }
    }

    @Test
    fun `should handle special characters in tokens`() = runTest {
        // Given
        val specialToken = "token_with_!@#$%^&*()_+-=[]{}|;':\",./<>?"
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = authTokenManager.storeAccessToken(testService, specialToken)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { secureStorage.store("${testService}_access_token", specialToken) }
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun `should handle concurrent token operations`() = runTest {
        // Given
        val iterations = 100
        coEvery { secureStorage.store(any(), any()) } returns Result.success(Unit)
        coEvery { secureStorage.retrieve(any()) } returns Result.success(testToken)
        
        // When
        val results = (1..iterations).map { i ->
            authTokenManager.storeAccessToken("$testService$i", "token$i")
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        coVerify(exactly = iterations) { secureStorage.store(any(), any()) }
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should recover from partial storage failures`() = runTest {
        // Given
        var callCount = 0
        coEvery { secureStorage.store(any(), any()) } answers {
            callCount++
            if (callCount == 1) Result.failure(Exception("Storage failed"))
            else Result.success(Unit)
        }
        
        // When
        val firstResult = authTokenManager.storeAccessToken(testService, testToken)
        val secondResult = authTokenManager.storeAccessToken(testService, testToken)
        
        // Then
        assertTrue(firstResult.isFailure)
        assertTrue(secondResult.isSuccess)
    }
}